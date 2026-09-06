(ns clj-surgeon.mcp-hot-verify
  "Reload namespaces and run exact focused test Vars in one configured app JVM."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [nrepl.core :as nrepl]
   [nrepl.transport :as transport])
  (:import
   (java.net SocketException)))

(def ^:private qualified-name-pattern
  #"^[A-Za-z0-9._!?*+<>=%$-]+(?:/[A-Za-z0-9._!?*+<>=%$-]+)?$")

(defn valid-profile?
  [profile]
  (and (map? profile)
       (= (set (keys profile))
          (cond-> #{:port-file :reload :tests}
            (:timeout-ms profile) (conj :timeout-ms)))
       (string? (:port-file profile))
       (not (str/blank? (:port-file profile)))
       (not (.isAbsolute (io/file (:port-file profile))))
       (not (str/includes? (:port-file profile) ".."))
       (vector? (:reload profile))
       (every? #(and (string? %) (re-matches qualified-name-pattern %))
               (:reload profile))
       (vector? (:tests profile))
       (every? #(and (string? %)
                     (str/includes? % "/")
                     (re-matches qualified-name-pattern %))
               (:tests profile))
       (or (nil? (:timeout-ms profile))
           (and (int? (:timeout-ms profile))
                (<= 100 (:timeout-ms profile) 60000)))))

(defn- eval-code
  [{:keys [reload tests]}]
  (str
    "(do "
    (apply str (map #(str "(require '" % " :reload) ") reload))
    "(require '[clojure.test :as t]) "
    "(let [summary "
    (if (seq tests)
      (str "(binding [t/*report-counters* (ref t/*initial-report-counters*)] "
           "(t/test-vars ["
           (str/join " " (map #(str "#'" %) tests))
           "]) @t/*report-counters*)")
      "t/*initial-report-counters*")
    "] {:cwd (System/getProperty \"user.dir\") "
    ":pid (.pid (java.lang.ProcessHandle/current)) "
    ":summary summary}))"))

(def ^:private failure-statuses
  "Statuses that mean this evaluation did not verify anything. `interrupted` is
   terminal AND a failure: an interrupted evaluation can have already emitted a
   value with a matching cwd and zero counters, and that must never read as a
   pass."
  #{"eval-error" "error" "timeout" "interrupted"})

(defn- bounded-output
  [responses limit]
  (let [text (str/join "" (keep #(or (:err %) (:out %)) responses))]
    (when (seq text)
      (subs text 0 (min limit (count text))))))

(def ^:private terminal-statuses
  "nREPL statuses that end one message's response stream. A reader that waits
   only for \"done\" hangs to its ceiling when the server ends the stream with
   an error status instead."
  #{"done" "error" "eval-error" "interrupted"})

(defn- response-statuses
  [response]
  (let [status (:status response)]
    (set (map name (if (string? status) [status] status)))))

;; @spec MCP-OP-HOTVER-001
;; @spec MCP-OP-HOTVER-002
(defn- read-until-terminal
  "Consume responses for one message id until a terminal status arrives, the
   transport closes, or the deadline passes. The deadline is a true ceiling:
   every blocking read is bounded by the time remaining against ONE deadline
   taken when the read starts, so a stream of non-terminal responses cannot
   push it out. Responses read before a closure are kept, because they are the
   only diagnostic a caller gets for a read that never terminated."
  [connection id timeout-ms]
  (let [deadline (+ (System/nanoTime) (* 1000000 (long timeout-ms)))
        ;; rounded UP: truncating the remaining nanos to whole milliseconds
        ;; lets the last blocking read expire just BEFORE the deadline, and a
        ;; ceiling that can be undershot is not a ceiling. Measured: a 300 ms
        ;; profile returned at 299.67 ms.
        remaining #(long (Math/ceil (/ (- deadline (System/nanoTime)) 1000000.0)))
        collected (volatile! [])]
    (try
      (loop []
        (let [left (remaining)]
          (if-not (pos? left)
            {:responses @collected :outcome :timeout}
            (if-let [response (transport/recv connection left)]
              (if (= id (:id response))
                (do
                  (vswap! collected conj response)
                  (if (seq (set/intersection (response-statuses response)
                                             terminal-statuses))
                    {:responses @collected :outcome :terminal}
                    (recur)))
                (recur))
              {:responses @collected :outcome :timeout}))))
      (catch SocketException error
        {:responses @collected :outcome :closed :error (.getMessage error)}))))

(defn verify!
  "Run one closed hot profile against the configured application nREPL."
  [project-root profile]
  (let [started (System/nanoTime)]
    (if-not (valid-profile? profile)
      {:ok false :status :refused
       :error-type :invalid-hot-verification-profile
       :error "Hot verification profile is not closed data"}
      (let [root (.getCanonicalFile (io/file project-root))
            port-file (.getCanonicalFile (io/file root (:port-file profile)))]
        (if-not (str/starts-with? (.getPath port-file)
                                  (str (.getPath root) java.io.File/separator))
          {:ok false :status :refused
           :error-type :hot-verification-path-escape
           :error "Hot verification port file is outside the project root"}
          (try
            (let [port (parse-long (str/trim (slurp port-file)))
                  timeout (or (:timeout-ms profile) 10000)]
              (when-not (and port (<= 1 port 65535))
                (throw (ex-info "Invalid application nREPL port"
                                {:error-type :invalid-hot-verification-port})))
              (with-open [connection (nrepl/connect :host "127.0.0.1" :port port)]
                (let [id (str (random-uuid))
                      _ (transport/send connection {:op "eval"
                                                    :id id
                                                    :code (eval-code profile)})
                      {:keys [responses outcome] :as read-result}
                      (read-until-terminal connection id timeout)
                      ;; hoisted rather than spelled inside the `:error-type`
                      ;; value: the entrance's refusal enumeration READS that
                      ;; value structurally, and a comparison operand sitting
                      ;; inside it is minted as a refusal kind that does not
                      ;; exist (the alias-migration enumeration read `closed`).
                      closed? (= :closed outcome)]
                  (if (not= :terminal outcome)
                    {:ok false
                     :status :failed
                     :jvm "application"
                     :reload-count (count (:reload profile))
                     :law-count (count (:tests profile))
                     :error-type (if closed?
                                   :hot-verification-transport-closed
                                   :hot-verification-timeout)
                     :error (if closed?
                              (str "Application nREPL transport closed before "
                                   "any terminal status: " (:error read-result))
                              (str "Application nREPL sent no terminal status "
                                   "within " timeout "ms"))
                     :output (bounded-output responses 2000)
                     :elapsed_ms (/ (double (- (System/nanoTime) started))
                                    1000000.0)}
                    (let [statuses (into #{} (mapcat response-statuses) responses)
                          value-source (last (keep :value responses))
                          value (when value-source (edn/read-string value-source))
                          summary (:summary value)
                          cwd (some-> (:cwd value) io/file .getCanonicalPath)
                          ok (boolean
                               (and value
                                    (= (.getPath root) cwd)
                                    (contains? statuses "done")
                                    (empty? (set/intersection
                                              statuses failure-statuses))
                                    (zero? (long (or (:fail summary) 0)))
                                    (zero? (long (or (:error summary) 0)))))]
                      {:ok ok
                       :status (if ok :complete :failed)
                       :jvm "application"
                       :pid (:pid value)
                       :cwd cwd
                       :reload-count (count (:reload profile))
                       :law-count (count (:tests profile))
                       :summary summary
                       :elapsed_ms (/ (double (- (System/nanoTime) started))
                                      1000000.0)
                       :error-type (when-not ok :hot-verification-failed)
                       :output (when-not ok
                                 (bounded-output responses 4000))})))))
            (catch Exception error
              {:ok false
               :status :failed
               :error-type (or (:error-type (ex-data error))
                               :hot-verification-connection-failed)
               :error (.getMessage error)
               :elapsed_ms (/ (double (- (System/nanoTime) started))
                              1000000.0)})))))))
