(ns clj-surgeon.mcp-elaborator-supervisor
  "Boot-owned H-S app-server supervisor. Raw JSONL remains private to this namespace."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-elaborator-adapter :as adapter]
   [clj-surgeon.mcp-elaborator-intent :as intent]
   [clj-surgeon.mcp-elaborator-policy :as policy]
   [clj-surgeon.mcp-elaborator-receipt :as receipt]
   [clj-surgeon.outline :as outline]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter)
   (java.nio.charset StandardCharsets)
   (java.nio.file CopyOption Files LinkOption Path Paths StandardCopyOption)
   (java.nio.file.attribute PosixFilePermissions)
   (java.security MessageDigest)
   (java.time Instant)
   (java.util.concurrent TimeUnit)))

(def ^:private outbound-methods
  #{"initialize" "account/read" "account/rateLimits/read" "account/usage/read"
    "model/list" "thread/start" "turn/start" "turn/interrupt"})

(def ^:private minimal-path
  "/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin")

(def ^:private max-jsonl-line-chars 65536)

(defonce ^:private active-supervisor (atom nil))

(declare turn-token-counts)

(defn configured-from-environment
  "Read only explicit operator configuration. Never search a home directory."
  []
  {:enabled? (= "true" (some-> (System/getenv "CLJ_SURGEON_SPARK_ENABLED")
                               str/lower-case))
   :codex-path (System/getenv "CLJ_SURGEON_SPARK_CODEX_PATH")
   :auth-file (System/getenv "CLJ_SURGEON_SPARK_AUTH_FILE")
   :schema-file (System/getenv "CLJ_SURGEON_SPARK_SCHEMA_FILE")
   :expected-cli-sha256 (System/getenv "CLJ_SURGEON_SPARK_CLI_SHA256")
   :expected-schema-sha256 (System/getenv "CLJ_SURGEON_SPARK_SCHEMA_SHA256")
   :auth-identity-sha256 (System/getenv "CLJ_SURGEON_SPARK_AUTH_IDENTITY_SHA256")
   :ledger-file (System/getenv "CLJ_SURGEON_SPARK_LEDGER_FILE")
   :rolling-24h-call-budget
   (some-> (System/getenv "CLJ_SURGEON_SPARK_24H_CALL_BUDGET") parse-long)})

(defn- posix-mode!
  [^Path path mode]
  (Files/setPosixFilePermissions path (PosixFilePermissions/fromString mode)))

(defn- sha256-file
  [file]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [stream (io/input-stream file)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [read-count (.read stream buffer)]
            (when (pos? read-count)
              (.update digest buffer 0 read-count)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(defn- auth-identity-sha256
  [auth-file]
  (with-open [reader (io/reader auth-file)]
    (let [account-id (get-in (json/parse-stream reader true)
                             [:tokens :account_id])]
      (when (or (not (string? account-id)) (str/blank? account-id))
        (throw (ex-info "Managed auth omits a stable account identity" {})))
      (intent/sha256 account-id))))

(defn- safe-service-config?
  [config]
  (and (:enabled? config)
       (every? #(and (string? %) (not (str/blank? %)))
               ((juxt :codex-path :auth-file :schema-file
                      :expected-cli-sha256 :expected-schema-sha256
                      :auth-identity-sha256 :ledger-file)
                config))
       (every? #(.isAbsolute (io/file %))
               ((juxt :codex-path :auth-file :schema-file :ledger-file) config))
       (pos-int? (:rolling-24h-call-budget config))
       (every? #(re-matches #"[0-9a-f]{64}" %)
               ((juxt :expected-cli-sha256 :expected-schema-sha256
                      :auth-identity-sha256)
                config))))

(defn- delete-tree!
  [root]
  (when root
    (let [file (io/file (str root))]
      (when (.exists file)
        (doseq [child (reverse (file-seq file))]
          (io/delete-file child true))))))

(defn- make-service-root!
  [{:keys [auth-file]}]
  (let [root (Files/createTempDirectory "clj-surgeon-spark-"
                                        (make-array java.nio.file.attribute.FileAttribute 0))
        codex-home (.resolve root "codex-home")
        workspace (.resolve root "workspace")
        child-tmp (.resolve root "tmp")]
    (posix-mode! root "rwx------")
    (doseq [path [codex-home workspace child-tmp]]
      (Files/createDirectory path
                             (make-array java.nio.file.attribute.FileAttribute 0))
      (posix-mode! path "rwx------"))
    (let [auth-target (.resolve codex-home "auth.json")
          config-target (.resolve codex-home "config.toml")]
      (Files/copy (.toPath (io/file auth-file)) auth-target
                  (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))
      (posix-mode! auth-target "rw-------")
      (spit (str config-target) policy/hardening-config)
      (posix-mode! config-target "rw-------"))
    (posix-mode! workspace "r-x------")
    {:root root :codex-home codex-home :workspace workspace :child-tmp child-tmp}))

(defn- child-environment!
  [^ProcessBuilder builder {:keys [codex-home child-tmp]}]
  (let [environment (.environment builder)]
    (.clear environment)
    (doseq [[key value]
            {"CODEX_HOME" (str codex-home)
             "HOME" (str child-tmp)
             "LANG" "C.UTF-8"
             "NO_COLOR" "1"
             "PATH" minimal-path
             "SHELL" "/bin/zsh"
             "TERM" "dumb"
             "TMPDIR" (str child-tmp)
             "USER" "codex"}]
      (.put environment key value)))
  builder)

(defn- launch-child!
  [{:keys [codex-path] :as _config} service-root]
  (let [setsid-code (str "import os,sys; os.setsid(); "
                         "os.execv(sys.argv[1], sys.argv[1:])")
        command ["/usr/bin/python3" "-c" setsid-code codex-path
                 "app-server" "--strict-config" "--listen" "stdio://"]
        builder (doto (ProcessBuilder. ^java.util.List command)
                  (.directory (.toFile ^Path (:workspace service-root))))]
    (child-environment! builder service-root)
    (.start builder)))

(defn- process-group
  [pid]
  (let [process (.start (ProcessBuilder.
                          ^java.util.List ["/bin/ps" "-o" "pgid=" "-p" (str pid)]))]
    (when (.waitFor process 1000 TimeUnit/MILLISECONDS)
      (some-> (slurp (.getInputStream process)) str/trim parse-long))))

(defn- process-group-pids
  [pgid]
  (let [process (.start (ProcessBuilder.
                          ^java.util.List
                          ["/bin/ps" "-axo" "pid=,pgid="]))]
    (if-not (.waitFor process 1000 TimeUnit/MILLISECONDS)
      #{}
      (->> (str/split-lines (slurp (.getInputStream process)))
           (keep (fn [line]
                   (when-let [[_ pid group]
                              (re-matches #"\s*(\d+)\s+(\d+)\s*" line)]
                     (when (= pgid (parse-long group))
                       (parse-long pid)))))
           set))))

(defn- signal-group!
  [pgid signal]
  (when (and (integer? pgid) (> pgid 1))
    (let [process (.start (ProcessBuilder.
                            ^java.util.List ["/bin/kill" signal (str "-" pgid)]))]
      (.waitFor process 1000 TimeUnit/MILLISECONDS))))

(defn- write-message!
  [session message]
  (locking (:writer session)
    (.write ^BufferedWriter (:writer session) (json/generate-string message))
    (.newLine ^BufferedWriter (:writer session))
    (.flush ^BufferedWriter (:writer session))))

(defn- send-notification!
  [session method params]
  (write-message! session (cond-> {:method method} params (assoc :params params))))

(defn- rpc!
  ([session method params]
   (rpc! session method params 15000))
  ([session method params timeout-ms]
   (when-not (contains? outbound-methods method)
     (throw (ex-info "Refusing non-whitelisted app-server method"
                     {:method method})))
   (let [id (swap! (:next-id session) inc)
         result (promise)]
     (swap! (:pending session) assoc id result)
     (write-message! session (cond-> {:id id :method method}
                               (some? params) (assoc :params params)))
     (let [value (deref result timeout-ms ::timeout)]
       (swap! (:pending session) dissoc id)
       (cond
         (= ::timeout value)
         (throw (ex-info "app-server request timeout" {:method method}))
         (:error value)
         (throw (ex-info "app-server request failed"
                         {:method method :error (:error value)}))
         :else (:result value))))))

(defn- interrupt-once!
  [session thread-id turn-id]
  (when (compare-and-set! (:interrupted? session) false true)
    (let [id (swap! (:next-id session) inc)]
      (write-message! session {:id id :method "turn/interrupt"
                               :params {:threadId thread-id :turnId turn-id}}))))

(defn- action-item?
  [item]
  (not (contains? #{"userMessage" "reasoning" "agentMessage"} (:type item))))

(defn- handle-message!
  [session message]
  (cond
    (and (:id message) (contains? @(:pending session) (:id message)))
    (deliver (get @(:pending session) (:id message))
             (select-keys message [:result :error]))

    (and (:id message) (:method message))
    (do
      (swap! (:violations session) conj {:type :server-request
                                         :method (:method message)})
      (write-message! session {:id (:id message) :result {:decision "decline"}}))

    :else
    (let [method (:method message)
          params (:params message)
          turn-id (or (:turnId params) (get-in params [:turn :id]))
          item (:item params)]
      (cond
        (= method "model/rerouted")
        (swap! (:violations session) conj {:type :reroute})

        (and (= method "item/started") (action-item? item))
        (do
          (swap! (:violations session) conj {:type :non-text-item
                                             :item-type (:type item)})
          (when-let [thread-id (or (:threadId params)
                                   (get @(:turn-threads session) turn-id))]
            (interrupt-once! session thread-id turn-id)))

        (= method "item/completed")
        (if (action-item? item)
          (do
            (swap! (:violations session) conj {:type :non-text-item
                                               :item-type (:type item)})
            (when-let [thread-id (or (:threadId params)
                                     (get @(:turn-threads session) turn-id))]
              (interrupt-once! session thread-id turn-id)))
          (swap! (:turn-events session) update turn-id (fnil conj []) item))

        (= method "item/agentMessage/delta")
        (let [total (swap! (:output-bytes session) update turn-id (fnil + 0)
                           (intent/utf8-bytes (or (:delta params) "")))]
          (when (> (get total turn-id) adapter/output-byte-ceiling)
            (swap! (:overflow-at-ms session)
                   #(if (contains? % turn-id) %
                        (assoc % turn-id (System/currentTimeMillis))))
            (swap! (:violations session) conj {:type :oversized-output})
            (when-let [thread-id (or (:threadId params)
                                     (get @(:turn-threads session) turn-id))]
              (interrupt-once! session thread-id turn-id))))

        (= method "thread/tokenUsage/updated")
        (swap! (:token-usage session) assoc turn-id (:tokenUsage params))

        (= method "turn/completed")
        (deliver (or (get @(:turn-waiters session) turn-id)
                     (let [result (promise)]
                       (swap! (:turn-waiters session) assoc turn-id result)
                       result))
                 (:turn params))))))

(defn- read-bounded-line
  [^BufferedReader reader]
  (let [line (StringBuilder.)]
    (loop []
      (let [character (.read reader)]
        (cond
          (= -1 character) (when (pos? (.length line)) (str line))
          (= (int \newline) character) (str line)
          (> (.length line) max-jsonl-line-chars)
          (throw (ex-info "app-server JSONL line exceeded bound"
                          {:maximum max-jsonl-line-chars}))
          :else (do (.append line (char character)) (recur)))))))

(defn- start-reader!
  [session]
  (let [thread
        (Thread.
          (fn []
            (try
              (loop []
                (when-let [line (read-bounded-line (:reader session))]
                  (try
                    (handle-message! session (json/parse-string line true))
                    (catch Exception error
                      (swap! (:violations session) conj
                             {:type :protocol-desync :class (.getName (class error))})))
                  (recur)))
              (catch Exception error
                (swap! (:violations session) conj
                       {:type :protocol-desync :class (.getName (class error))}))
              (finally
                (reset! (:eof? session) true)
                (doseq [[_ result] @(:pending session)]
                  (deliver result {:error {:message "app-server EOF"}}))
                (doseq [[_ result] @(:turn-waiters session)]
                  (deliver result {:status "eof"}))))))
        _ (.setName thread "clj-surgeon-spark-jsonl-reader")
        _ (.setDaemon thread true)]
    (.start thread)
    thread))

(defn- session!
  [process service-root pgid]
  (let [session {:process process
                 :pgid pgid
                 :service-root service-root
                 :reader (BufferedReader.
                           (InputStreamReader. (.getInputStream process)
                                               StandardCharsets/UTF_8))
                 :writer (BufferedWriter.
                           (OutputStreamWriter. (.getOutputStream process)
                                                StandardCharsets/UTF_8))
                 :next-id (atom 0)
                 :pending (atom {})
                 :turn-waiters (atom {})
                 :turn-events (atom {})
                 :turn-threads (atom {})
                 :token-usage (atom {})
                 :output-bytes (atom {})
                 :overflow-at-ms (atom {})
                 :active-turn (atom nil)
                 :eof? (atom false)
                 :violations (atom [])
                 :interrupted? (atom false)
                 :close-lock (Object.)
                 :cleanup (atom nil)}]
    (assoc session :reader-thread (start-reader! session))))

(defn- wait-turn!
  [session turn-id timeout-ms]
  (let [result (or (get @(:turn-waiters session) turn-id)
                   (let [created (promise)]
                     (get (swap! (:turn-waiters session)
                                 #(if (contains? % turn-id) %
                                      (assoc % turn-id created)))
                          turn-id)))
        deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [now (System/currentTimeMillis)
            overflow-at (get @(:overflow-at-ms session) turn-id)]
        (cond
          (realized? result) @result

          @(:eof? session) {:status "eof"}

          (and overflow-at (>= (- now overflow-at) 1000))
          (do
            (signal-group! (:pgid session) "-KILL")
            (throw (ex-info "Oversized Spark output did not settle after interrupt"
                            {:turn-id turn-id :settle_timeout_ms 1000})))

          (>= now deadline)
          (do
            (when-let [thread-id (get @(:turn-threads session) turn-id)]
              (interrupt-once! session thread-id turn-id))
            (throw (ex-info "Spark turn timeout" {:turn-id turn-id})))

          :else
          (do
            (deref result (min 25 (- deadline now)) ::waiting)
            (recur)))))))

(defn- model-rows
  [models]
  (or (:data models) (:models models) []))

(defn- exact-model-present?
  [models]
  (boolean
    (some #(and (map? %)
                (or (and (= policy/model-slug (:slug %))
                         (false? (:supported_in_api %)))
                    (and (= policy/model-slug (:model %))
                         (= policy/model-slug (:id %)))))
          (model-rows models))))

(defn- thread-id
  [response]
  (get-in response [:thread :id]))

(defn- start-thread!
  [session workspace]
  (let [response (rpc! session "thread/start" (adapter/thread-params workspace))]
    (when-not (and (= policy/model-slug (:model response))
                   (= "openai" (:modelProvider response))
                   (thread-id response))
      (throw (ex-info "Spark thread model pin failed" {:response response})))
    (thread-id response)))

(defn- run-turn!
  [session thread-id prompt]
  (reset! (:interrupted? session) false)
  (reset! (:violations session) [])
  (let [started-ns (System/nanoTime)]
    (reset! (:active-turn session) {:started true :thread-id thread-id})
    (try
      (let [response (rpc! session "turn/start" (adapter/turn-params thread-id prompt))
            turn-id (get-in response [:turn :id])]
        (when-not turn-id
          (throw (ex-info "turn/start omitted turn identity" {:response response})))
        (swap! (:active-turn session) assoc :turn-id turn-id)
        (swap! (:turn-threads session) assoc turn-id thread-id)
        (let [completed (wait-turn! session turn-id adapter/turn-timeout-ms)
              events (get @(:turn-events session) turn-id [])
              consumed (adapter/consume-turn events)
              violations @(:violations session)]
          (cond
            (seq violations)
            (assoc (adapter/failure-decision (:type (first violations)))
                   :violations violations
                   :tool_item_observed (boolean (some #(= :non-text-item (:type %))
                                                      violations))
                   :reroute_observed (boolean (some #(= :reroute (:type %))
                                                    violations))
                   :turn_id turn-id)
            (not= "completed" (:status completed))
            (assoc (adapter/failure-decision :interrupted)
                   :tool_item_observed false :reroute_observed false
                   :turn_id turn-id)
            (not (:ok consumed))
            (assoc consumed :tool_item_observed false :reroute_observed false
                   :turn_id turn-id)
            :else
            (assoc consumed
                   :turn_id turn-id
                   :latency_ms (/ (- (System/nanoTime) started-ns) 1000000.0)
                   :tool_item_observed false
                   :reroute_observed false
                   :token_usage (get @(:token-usage session) turn-id)))))
      (catch Exception error
        (merge (adapter/failure-decision :adapter-failure)
               {:turn_id (:turn-id @(:active-turn session))
                :latency_ms (/ (- (System/nanoTime) started-ns) 1000000.0)
                :tool_item_observed false
                :reroute_observed false
                :error_class (.getName (class error))
                :error_data (select-keys (ex-data error)
                                         [:settle_timeout_ms])})))))

(defn- run-observed-turn!
  [session baseline-pids workspace prompt]
  (let [observer-running? (atom true)
        descendants (atom #{})
        descendant-killed? (atom false)
        observer (doto
                   (Thread.
                     (fn []
                       (while @observer-running?
                         (let [new-descendants
                               (seq (remove baseline-pids
                                            (process-group-pids (:pgid session))))]
                           (when new-descendants
                             (swap! descendants into new-descendants)
                             (when (compare-and-set! descendant-killed? false true)
                               (swap! (:violations session) conj
                                      {:type :descendant-process
                                       :pids (sort new-descendants)})
                               (signal-group! (:pgid session) "-KILL"))))
                         (Thread/sleep 25)))
                     "clj-surgeon-spark-process-observer")
                   (.setDaemon true))]
    (.start observer)
    (try
      (let [thread-id (start-thread! session workspace)
            turn (run-turn! session thread-id prompt)]
        (if (seq @descendants)
          (merge (adapter/failure-decision :descendant-process)
                 {:descendant_pids (sort @descendants)
                  :turn_id (:turn_id turn)
                  :latency_ms (:latency_ms turn)
                  :token_usage (:token_usage turn)
                  :tool_item_observed (:tool_item_observed turn)
                  :reroute_observed (:reroute_observed turn)})
          turn))
      (finally
        (reset! observer-running? false)
        (.join observer 200)))))

(defn- meter-evidence
  [rate-limits]
  (let [window (policy/meter-window rate-limits)
        primary (:primary window)
        secondary (:secondary window)
        used (or (:usedPercent primary) (:used_percent primary))
        valid-rate-window?
        (fn [value]
          (let [percent (or (:usedPercent value) (:used_percent value))
                duration (or (:windowDurationMins value)
                             (:window_duration_mins value))
                reset-at (or (:resetsAt value) (:resets_at value))]
            (and (number? percent) (<= 0 percent 100)
                 (pos-int? duration) (integer? reset-at))))]
    {:meter_present (map? window)
     :meter_consistent (and (= "codex_bengalfox" (:limitId window))
                            (= "GPT-5.3-Codex-Spark" (:limitName window))
                            (valid-rate-window? primary)
                            (valid-rate-window? secondary))
     :service_used_percent used
     :window window}))

(defn- rate-window-consistent?
  [before after]
  (let [before-percent (or (:usedPercent before) (:used_percent before))
        after-percent (or (:usedPercent after) (:used_percent after))
        before-duration (or (:windowDurationMins before)
                            (:window_duration_mins before))
        after-duration (or (:windowDurationMins after)
                           (:window_duration_mins after))
        before-reset (or (:resetsAt before) (:resets_at before))
        after-reset (or (:resetsAt after) (:resets_at after))]
    (and (= before-duration after-duration)
         (or (and (= before-reset after-reset)
                  (<= before-percent after-percent))
             (> after-reset before-reset)))))

(defn- meter-snapshots-consistent?
  [before after]
  (let [before-window (policy/meter-window before)
        after-window (policy/meter-window after)]
    (and (= (select-keys before-window [:limitId :limitName])
            (select-keys after-window [:limitId :limitName]))
         (rate-window-consistent? (:primary before-window)
                                  (:primary after-window))
         (rate-window-consistent? (:secondary before-window)
                                  (:secondary after-window)))))

(defn- rolling-percent
  [calls budget now-ms]
  (let [cutoff (- now-ms (* 24 60 60 1000))
        retained (filterv #(>= % cutoff) calls)]
    {:calls retained
     :percent (* 100.0 (/ (count retained) budget))}))

(defn- quota-state!
  ([supervisor rate-limits]
   (quota-state! supervisor nil rate-limits))
  ([supervisor before rate-limits]
   (let [now-ms (System/currentTimeMillis)
         rolling (rolling-percent @(:calls supervisor)
                                  (get-in supervisor [:config :rolling-24h-call-budget])
                                  now-ms)
         _ (reset! (:calls supervisor) (:calls rolling))
         meter (meter-evidence rate-limits)
         snapshots-consistent? (or (nil? before)
                                   (meter-snapshots-consistent? before rate-limits))
         decision (receipt/quota-decision
                    {:rolling_24h_used_percent (:percent rolling)
                     :meter_present (:meter_present meter)
                     :meter_consistent (and (:meter_consistent meter)
                                            snapshots-consistent?)})]
     (merge meter decision
            {:meter_consistent (and (:meter_consistent meter)
                                    snapshots-consistent?)
             :rolling_24h_used_percent (:percent rolling)}))))

(defn- append-ledger!
  [supervisor row]
  (let [file (io/file (get-in supervisor [:config :ledger-file]))]
    (when-let [parent (.getParentFile file)] (.mkdirs parent))
    (spit file (str (json/generate-string row) "\n") :append true)))

(defn- load-call-history
  [config]
  (let [file (io/file (:ledger-file config))]
    (if-not (.exists file)
      []
      (->> (str/split-lines (slurp file))
           (remove str/blank?)
           (map #(json/parse-string % true))
           (filter #(and (= "clj-surgeon.embedded-elaborator-ledger.v1"
                            (:schema %))
                         (:turn_id %)))
           (mapv #(-> ^String (:timestamp %) Instant/parse .toEpochMilli))))))

(defn- emit-quota-alarm!
  [supervisor quota]
  (when (and (:alarm quota)
             (compare-and-set! (:alarm-emitted? supervisor) false true))
    (append-ledger!
      supervisor
      {:schema "clj-surgeon.embedded-elaborator-operator-event.v1"
       :timestamp (str (Instant/now))
       :event "quota-alarm"
       :auth_identity_sha256
       (get-in supervisor [:config :auth-identity-sha256])
       :rolling_24h_used_percent (:rolling_24h_used_percent quota)
       :alarm_threshold_percent policy/alarm-threshold-percent
       :circuit_threshold_percent policy/circuit-threshold-percent
       :circuit_open (:circuit_open quota)
       :reason (:reason quota)})))

(defn- runtime-evidence
  [supervisor]
  {:model policy/model-slug
   :reported_model policy/model-slug
   :provider "openai"
   :account_type "chatgpt"
   :plan_type "pro"
   :cli_version policy/cli-version
   :cli_sha256 (get-in supervisor [:config :expected-cli-sha256])
   :schema_sha256 (get-in supervisor [:config :expected-schema-sha256])
   :auth_identity_sha256 (get-in supervisor [:config :auth-identity-sha256])
   :allow_provider_model_fallback false
   :reroutes []})

(defn- close-session!
  [session]
  (locking (:close-lock session)
    (or @(:cleanup session)
        (let [process ^Process (:process session)
              pid (.pid process)
              parent-pid (some-> (.parent (.toHandle process)) (.orElse nil) .pid)
              cwd (str (get-in session [:service-root :workspace]))
              pgid (:pgid session)
              ownership (adapter/validate-owned-process
                          {:pid pid :pgid pgid :actual_pgid pgid})]
          (try (.close ^BufferedWriter (:writer session)) (catch Exception _))
          (.waitFor process 1000 TimeUnit/MILLISECONDS)
          (when (and (:signal_allowed ownership) (seq (process-group-pids pgid)))
            (signal-group! pgid "-TERM")
            (let [deadline (+ (System/currentTimeMillis) 1000)]
              (loop []
                (when (and (< (System/currentTimeMillis) deadline)
                           (seq (process-group-pids pgid)))
                  (Thread/sleep 25)
                  (recur))))
            (when (seq (process-group-pids pgid))
              (signal-group! pgid "-KILL")
              (let [deadline (+ (System/currentTimeMillis) 1000)]
                (loop []
                  (when (and (< (System/currentTimeMillis) deadline)
                             (seq (process-group-pids pgid)))
                    (Thread/sleep 25)
                    (recur))))))
          (let [remaining (if pgid (process-group-pids pgid) #{})
                cleanup {:pid pid :pgid pgid :remaining remaining
                         :cleanup_ok (empty? remaining)
                         :signal_allowed (:signal_allowed ownership)
                         :cwd cwd
                         :parent_pid parent-pid
                         :actions [:close-stdin :wait-1000ms :sigterm-owned-pgid
                                   :wait-1000ms :sigkill-owned-pgid-if-required]
                         :exit_value (when-not (.isAlive process)
                                       (.exitValue process))}]
            (when (empty? remaining)
              (delete-tree! (get-in session [:service-root :root])))
            (reset! (:cleanup session) cleanup)
            cleanup)))))

(defn- disable-and-close!
  [supervisor reason]
  (let [cleanup (when-let [session @(:session supervisor)]
                  (close-session! session))]
    (when cleanup (reset! (:last-cleanup supervisor) cleanup))
    (reset! (:session supervisor) nil)
    (reset! (:state supervisor)
            {:status :unavailable :reason reason :cleanup cleanup})
    cleanup))

(defn- verify-files!
  [config]
  (doseq [key [:codex-path :auth-file :schema-file]]
    (when-not (.isFile (io/file (get config key)))
      (throw (ex-info "Configured elaborator file is absent" {:field key}))))
  (when-not (= (:expected-cli-sha256 config) (sha256-file (:codex-path config)))
    (throw (ex-info "Pinned Codex CLI hash mismatch" {})))
  (when-not (= (:expected-schema-sha256 config) (sha256-file (:schema-file config)))
    (throw (ex-info "Pinned app-server schema hash mismatch" {})))
  (when-not (= (:auth-identity-sha256 config)
               (auth-identity-sha256 (:auth-file config)))
    (throw (ex-info "Managed ChatGPT auth identity hash mismatch" {}))))

(defn- verify-cli-version!
  [config service-root]
  (let [builder (doto (ProcessBuilder.
                        ^java.util.List [(:codex-path config) "--version"])
                  (.redirectErrorStream true)
                  (.directory (.toFile ^Path (:workspace service-root))))
        _ (child-environment! builder service-root)
        process (.start builder)]
    (when-not (.waitFor process 3000 TimeUnit/MILLISECONDS)
      (.destroyForcibly process)
      (throw (ex-info "Pinned Codex CLI version check timed out" {})))
    (let [reported (str/trim (slurp (.getInputStream process)))]
      (when-not (and (zero? (.exitValue process)) (= policy/cli-version reported))
        (throw (ex-info "Pinned Codex CLI version mismatch"
                        {:expected policy/cli-version :reported reported}))))))

(defn- prepare-child!
  [config]
  (let [service-root (make-service-root! config)]
    (try
      (verify-cli-version! config service-root)
      {:service-root service-root
       :process (launch-child! config service-root)}
      (catch Exception error
        (delete-tree! (:root service-root))
        (throw error)))))

(defn- warmup-ledger-row
  [runtime warmup rate-limits rate-limits-after]
  (let [tokens (turn-token-counts warmup)]
    (receipt/ledger-row
      (merge warmup tokens
             {:timestamp (str (Instant/now))
              :intent_sha256 (intent/sha256 "fixed-no-effect-warmup-v1")
              :elaboration_sha256 (when (:ok warmup) (intent/sha256 "ready"))
              :runtime runtime
              :rate_limits_before rate-limits
              :rate_limits_after rate-limits-after}))))

(defn- initialize-session!
  [supervisor]
  (verify-files! (:config supervisor))
  (when @(:stopping? supervisor)
    (throw (ex-info "Elaborator stopped during boot" {:stopped true})))
  (let [{:keys [service-root process]} (prepare-child! (:config supervisor))
        pgid (loop [attempt 0]
               (let [observed (process-group (.pid process))]
                 (if (or (= (.pid process) observed) (>= attempt 20))
                   observed
                   (do
                     (Thread/sleep 10)
                     (recur (inc attempt))))))]
    (when-not (= (.pid process) pgid)
      (.destroyForcibly process)
      (delete-tree! (:root service-root))
      (throw (ex-info "Dedicated process-group admission failed"
                      {:pid (.pid process) :pgid pgid})))
    (let [session (session! process service-root pgid)
          admitted-to-boot?
          (locking (:lifecycle-lock supervisor)
            (when-not @(:stopping? supervisor)
              (reset! (:boot-session supervisor) session)
              true))]
      (when-not admitted-to-boot?
        (let [cleanup (close-session! session)]
          (reset! (:last-cleanup supervisor) cleanup)
          (throw (ex-info "Elaborator stopped during boot"
                          {:stopped true :cleanup cleanup}))))
      (try
        (let [initialize (rpc! session "initialize"
                               {:clientInfo {:name "clj_surgeon_embedded_elaborator"
                                             :title "clj-surgeon embedded elaborator"
                                             :version "1.0.0"}
                                :capabilities
                                {:experimentalApi true
                                 :requestAttestation false
                                 :optOutNotificationMethods
                                 ["item/reasoning/summaryTextDelta"
                                  "item/reasoning/summaryPartAdded"
                                  "item/reasoning/textDelta"]}}
                               adapter/initialize-timeout-ms)
              _ (send-notification! session "initialized" nil)
              account (rpc! session "account/read" {:refreshToken false})
              models (rpc! session "model/list" {})
              rate-limits (rpc! session "account/rateLimits/read" {})
              runtime (runtime-evidence supervisor)
              admission (policy/admission-decision
                          (assoc runtime
                                 :account_type (get-in account [:account :type])
                                 :plan_type (get-in account [:account :planType])))]
          (when-not (:ok admission)
            (throw (ex-info "Runtime identity admission failed" admission)))
          (when-not (exact-model-present? models)
            (throw (ex-info "Pinned Spark model absent from app-server catalog" {})))
          (when (:circuit_open (quota-state! supervisor rate-limits))
            (throw (ex-info "Spark meter unavailable, inconsistent, or over circuit" {})))
          (let [baseline-pids (process-group-pids pgid)
                warmup (run-observed-turn!
                         session baseline-pids (str (:workspace service-root))
                         "Return exactly {\"replacement\":\"ready\"}.")
                rate-limits-after (try
                                    (rpc! session "account/rateLimits/read" {} 1000)
                                    (catch Exception _ nil))
                observed-pids (process-group-pids pgid)
                descendants (seq (remove baseline-pids observed-pids))]
            (swap! (:calls supervisor) conj (System/currentTimeMillis))
            (let [quota-after (quota-state! supervisor rate-limits rate-limits-after)
                  warmup-result (merge (if descendants
                                         (merge
                                           (adapter/failure-decision
                                             :descendant-process)
                                           {:descendant_pids (sort descendants)
                                            :turn_id (:turn_id warmup)})
                                         warmup)
                                       {:runtime runtime
                                        :result_class (if (:ok warmup)
                                                        "warmup" "refused")
                                        :alarm (:alarm quota-after)
                                        :circuit_open (:circuit_open quota-after)
                                        :quota_reason (:reason quota-after)})]
              (emit-quota-alarm! supervisor quota-after)
              (append-ledger! supervisor
                              (warmup-ledger-row runtime warmup-result
                                                 rate-limits rate-limits-after))
              (when-not (and (:ok warmup-result)
                             (= "ready" (:replacement warmup-result)))
                (throw (ex-info "Fixed no-effect warm-up failed" warmup-result)))
              (when (:circuit_open quota-after)
                (throw (ex-info "Spark quota circuit opened during warm-up" {})))
              (assoc session :initialize initialize :runtime runtime
                     :baseline-pids observed-pids))))
        (catch Exception error
          (let [cleanup (close-session! session)]
            (reset! (:last-cleanup supervisor) cleanup)
            (throw (ex-info (.getMessage error)
                            (assoc (or (ex-data error) {}) :cleanup cleanup)
                            error))))))))

(defn- boot!
  [supervisor]
  (try
    (if-not (safe-service-config? (:config supervisor))
      (reset! (:state supervisor)
              {:status :unavailable :reason :operator-config-absent})
      (let [_ (reset! (:calls supervisor)
                      (load-call-history (:config supervisor)))
            session (initialize-session! supervisor)
            published?
            (locking (:lifecycle-lock supervisor)
              (when-not @(:stopping? supervisor)
                (reset! (:boot-session supervisor) nil)
                (reset! (:session supervisor) session)
                (reset! (:state supervisor)
                        {:status :available
                         :model policy/model-slug
                         :isolation_policy_version policy/isolation-policy-version
                         :boot_ready_at (str (Instant/now))})
                true))]
        (when-not published?
          (let [cleanup (close-session! session)]
            (reset! (:last-cleanup supervisor) cleanup)))))
    (catch Exception error
      (when-let [session @(:boot-session supervisor)]
        (reset! (:last-cleanup supervisor) (close-session! session)))
      (locking (:lifecycle-lock supervisor)
        (reset! (:boot-session supervisor) nil)
        (reset! (:session supervisor) nil)
        (when-not @(:stopping? supervisor)
          (reset! (:state supervisor)
                  {:status :unavailable
                   :reason :boot-admission-failed
                   :error_class (.getName (class error))
                   :cleanup (or (:cleanup (ex-data error))
                                @(:last-cleanup supervisor))}))))))

;; @spec MCP-OP-ELAB-006
(declare stop!)

(defn start-background!
  "Start admission on one daemon boot thread and return immediately."
  ([] (start-background! (configured-from-environment)))
  ([config]
   (let [supervisor {:config config
                     :state (atom {:status :starting})
                     :session (atom nil)
                     :boot-session (atom nil)
                     :busy? (atom false)
                     :calls (atom [])
                     :alarm-emitted? (atom false)
                     :stopping? (atom false)
                     :last-cleanup (atom nil)
                     :lifecycle-lock (Object.)}
         thread (doto (Thread. #(boot! supervisor)
                               "clj-surgeon-spark-boot")
                  (.setDaemon true))
         supervisor (assoc supervisor :boot-thread thread)]
     (reset! active-supervisor supervisor)
     (.addShutdownHook (Runtime/getRuntime)
                       (Thread. #(try (stop! supervisor)
                                      (catch Exception _))))
     (.start thread)
     supervisor)))

(defn state
  ([] (some-> @active-supervisor state))
  ([supervisor] @(:state supervisor)))

(defn- turn-token-counts
  [turn]
  (let [usage (:token_usage turn)
        last-usage (or (:last usage) usage)]
    {:input_tokens (or (:inputTokens last-usage) 0)
     :output_tokens (or (:outputTokens last-usage) 0)}))

;; @spec MCP-OP-ELAB-004
;; @spec MCP-OP-ELAB-007
;; @spec MCP-OP-ELAB-008
;; @spec MCP-OP-ELAB-010
;; @spec MCP-OP-ELAB-011
(defn elaborate!
  "Run one fresh-thread intent only when boot admission is already complete."
  [supervisor {:keys [model_input intent_sha256]}]
  (cond
    (not= :available (:status (state supervisor)))
    (merge (adapter/failure-decision :elaborator-unavailable)
           {:turn_count 0
            :runtime (runtime-evidence supervisor)
            :result_class "refused"
            :tool_item_observed false
            :reroute_observed false})

    (not (compare-and-set! (:busy? supervisor) false true))
    (merge (adapter/failure-decision :elaborator-busy)
           {:turn_count 0
            :runtime (runtime-evidence supervisor)
            :result_class "refused"
            :tool_item_observed false
            :reroute_observed false})

    :else
    (try
      (let [session @(:session supervisor)
            before (rpc! session "account/rateLimits/read" {})
            quota-before (quota-state! supervisor before)]
        (emit-quota-alarm! supervisor quota-before)
        (if (:circuit_open quota-before)
          (do
            (disable-and-close! supervisor :quota-circuit)
            (merge (adapter/failure-decision :quota-stop)
                   {:turn_count 0
                    :runtime (runtime-evidence supervisor)
                    :result_class "refused"
                    :alarm (:alarm quota-before)
                    :circuit_open true
                    :quota_reason (:reason quota-before)}))
          (let [turn (run-observed-turn!
                       session (:baseline-pids session)
                       (str (get-in session [:service-root :workspace]))
                       (adapter/model-prompt model_input))
                _ (swap! (:calls supervisor) conj (System/currentTimeMillis))
                after (try
                        (rpc! session "account/rateLimits/read" {} 1000)
                        (catch Exception _ nil))
                quota-after (quota-state! supervisor before after)
                runtime (runtime-evidence supervisor)
                tokens (turn-token-counts turn)
                accepted? (and (:ok turn) (not (:circuit_open quota-after)))
                fatal-turn? (contains? #{"adapter-failure" "descendant-process"
                                         "non-text-item" "reroute"}
                                       (:error_type turn))
                result (merge turn tokens
                              {:intent_sha256 intent_sha256
                               :runtime runtime
                               :rate_limits_before before
                               :rate_limits_after after
                               :alarm (:alarm quota-after)
                               :circuit_open (:circuit_open quota-after)
                               :quota_reason (:reason quota-after)
                               :result_class (if accepted? "accepted" "refused")})
                row (receipt/ledger-row
                      (merge result
                             {:timestamp (str (Instant/now))
                              :elaboration_sha256
                              (when accepted? (intent/sha256 (:replacement turn)))}))]
            (emit-quota-alarm! supervisor quota-after)
            (append-ledger! supervisor row)
            (when (or fatal-turn? (:circuit_open quota-after))
              (disable-and-close! supervisor
                                  (if (:circuit_open quota-after)
                                    :quota-circuit :isolation-violation)))
            (cond
              accepted? result

              (:circuit_open quota-after)
              (if (:ok turn)
                (merge result (adapter/failure-decision :quota-stop)
                       {:runtime runtime
                        :result_class "refused"
                        :alarm (:alarm quota-after)
                        :circuit_open true
                        :quota_reason (:reason quota-after)})
                result)

              :else result))))
      (catch Exception error
        (let [cleanup (when-let [session @(:session supervisor)]
                        (close-session! session))]
          (reset! (:session supervisor) nil)
          (reset! (:state supervisor)
                  {:status :unavailable :reason :runtime-failure
                   :cleanup cleanup})
          (merge (adapter/failure-decision :adapter-failure)
                 {:runtime (runtime-evidence supervisor)
                  :result_class "refused"
                  :tool_item_observed false
                  :reroute_observed false
                  :error_class (.getName (class error))})))
      (finally
        (reset! (:busy? supervisor) false)))))

;; @spec MCP-OP-ELAB-003
;; @spec MCP-OP-ELAB-005
(defn execute-edit!
  "Capture caller authority, elaborate one body, then invoke the ordinary writer.

  The supplied ordinary function is the only effect path and must recapture the
  source after the model turn."
  [supervisor {:keys [project-root] :as config} request ordinary!]
  (let [validated (intent/validate-request request)]
    (if-not (:ok validated)
      validated
      (try
        (let [root (.toRealPath (Paths/get (str project-root)
                                           (make-array String 0))
                                (make-array LinkOption 0))
              edit (first (:edits request))
              target (.normalize (.resolve root ^String (:file edit)))
              owner-name (get-in edit [:within :form])]
          (cond
            (not= (str root) (:workspace_root request))
            {:ok false :error_type "canonical-workspace-root-required"
             :source_unchanged true :ordinary_path_available true}

            (not (.startsWith target root))
            {:ok false :error_type "path-outside-workspace"
             :source_unchanged true :ordinary_path_available true}

            (not (Files/isRegularFile target (make-array LinkOption 0)))
            {:ok false :error_type "existing-file-required"
             :source_unchanged true :ordinary_path_available true}

            :else
            (let [canonical-target (.toRealPath target (make-array LinkOption 0))
                  _ (when-not (.startsWith canonical-target root)
                      (throw (ex-info "Resolved target escapes workspace"
                                      {:error_type "path-outside-workspace"})))
                  source (if-let [read-source (:read-source config)]
                           (read-source canonical-target)
                           (slurp (str canonical-target)))
                  owners (->> (outline/top-level-form-records (:file edit) source)
                              (filter #(= owner-name (str (:name %)))))
                  capture {:workspace_root (str root)
                           :file (:file edit)
                           :owner owner-name
                           :source_sha256 (intent/sha256 source)
                           :owner_source (:source (first owners))}
                  captured (if (= 1 (count owners))
                             (intent/capture-intent request capture)
                             {:ok false :error_type "named-owner-not-exact"
                              :source_unchanged true
                              :ordinary_path_available true})]
              (if-not (:ok captured)
                captured
                (let [generated (elaborate!
                                  supervisor
                                  {:model_input (:model_input captured)
                                   :intent_sha256 (:intent_sha256 captured)})]
                  (if-not (:ok generated)
                    (assoc generated
                           :intent_sha256 (:intent_sha256 captured)
                           :elaboration_receipt
                           (receipt/project-receipt
                             (merge generated
                                    {:intent_sha256 (:intent_sha256 captured)})))
                    (let [completed (intent/complete-request
                                      request (:replacement generated))
                          ordinary (ordinary! (:request completed))
                          ordinary-evidence
                          {:operation "edit_clojure"
                           :receipt_hash (:receipt_hash ordinary)
                           :verification_complete
                           (:verification_complete ordinary)
                           :verification_receipt
                           (receipt/verification-receipt
                             (:verification ordinary))}
                          elaboration-receipt
                          (receipt/project-receipt
                            (assoc generated :ordinary ordinary-evidence))]
                      (assoc ordinary
                             :elaboration elaboration-receipt))))))))
        (catch Exception error
          {:ok false
           :error_type "elaboration-capture-failure"
           :error_class (.getName (class error))
           :source_unchanged true
           :ordinary_path_available true})))))

;; @spec MCP-OP-ELAB-009
(defn stop!
  ([] (when-let [supervisor @active-supervisor] (stop! supervisor)))
  ([supervisor]
   (let [sessions
         (locking (:lifecycle-lock supervisor)
           (reset! (:stopping? supervisor) true)
           (let [sessions (distinct (remove nil? [@(:boot-session supervisor)
                                                  @(:session supervisor)]))]
             (reset! (:boot-session supervisor) nil)
             (reset! (:session supervisor) nil)
             (reset! (:state supervisor) {:status :stopping})
             sessions))]
     (doseq [session sessions]
       (reset! (:last-cleanup supervisor) (close-session! session)))
     (when-let [thread (:boot-thread supervisor)]
       (when-not (= thread (Thread/currentThread))
         (.join ^Thread thread 4000)))
     (let [cleanup @(:last-cleanup supervisor)]
       (reset! (:state supervisor) {:status :stopped :cleanup cleanup})
       cleanup))))
