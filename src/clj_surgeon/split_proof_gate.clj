(ns clj-surgeon.split-proof-gate
  "Detached proof over an immutable namespace-split receipt. No late rollback."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.spawn-ledger :as spawn]
   [clj-surgeon.structural-lens :as lens]
   [clj-surgeon.synchronous-verification :as proof]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file LinkOption)
   (java.time Instant)))

(def max-receipt-bytes 65536)
(def max-job-bytes (* 4 1024 1024))

(defn read-text!
  "Bound artifact bytes before reading or hashing, including growth after stat."
  [path limit]
  (with-open [in (io/input-stream path)]
    (let [bytes (.readNBytes in (inc limit))]
      (when (> (alength bytes) limit)
        (throw (ex-info "Proof artifact exceeds its byte bound" {:error-type :proof-artifact-bound})))
      (String. bytes "UTF-8"))))

(defn read-edn! [path limit]
  (edn/read-string (read-text! path limit)))

(defn receipt-size [receipt]
  (max (alength (.getBytes (pr-str receipt) "UTF-8"))
       (alength (.getBytes (json/generate-string receipt {:escape-non-ascii true}) "UTF-8"))))

;; @spec NS-SPLIT-057
;; INTENT: NS-SPLIT-057
(defn bounded-receipt! [receipt]
  (let [size (receipt-size receipt)]
    ;; The transport adds elapsed_ms and a fixed summary prefix after saving.
    (when (> size (- max-receipt-bytes 256))
      (throw (ex-info "Split receipt exceeds 65536 bytes" {:error-type :receipt-size-bound :receipt_bytes size}))))
  receipt)

(defn snapshot-hash [sources]
  (lens/source-hash (pr-str (into (sorted-map) (map (fn [[path source]] [path (lens/source-hash source)])) sources))))

(defn current-hash [root roots]
  (try
    (snapshot-hash ((requiring-resolve 'clj-surgeon.namespace-split-io/capture!)
                    (.toRealPath (.toPath (io/file root)) (make-array LinkOption 0)) roots))
    (catch Exception _ nil)))

;; @spec NS-SPLIT-056
;; INTENT: NS-SPLIT-056
(defn safe-tmpdir?
  "True only for an existing java.io.tmpdir rooted below /var/tmp."
  [path]
  (try
    (and (string? path)
         (.isAbsolute (io/file path))
         (let [candidate (.toRealPath (.toPath (io/file path)) (make-array LinkOption 0))
               allowed (.toRealPath (.toPath (io/file "/var/tmp")) (make-array LinkOption 0))]
           (and (.startsWith candidate allowed) (not= candidate allowed))))
    (catch Exception _ false)))

(defn- admitted-tmpdir! []
  (let [tmpdir (System/getProperty "java.io.tmpdir")]
    (when-not (safe-tmpdir? tmpdir)
      (throw (ex-info "Background proof requires java.io.tmpdir below /var/tmp"
                      {:error-type :background-gate-unsafe-tmpdir :java_tmpdir tmpdir})))
    (str (.toRealPath (.toPath (io/file tmpdir)) (make-array LinkOption 0)))))

;; @spec NS-SPLIT-051
;; @spec NS-SPLIT-055
;; INTENT: NS-SPLIT-051
;; INTENT: NS-SPLIT-055
(defn closure-status
  "Join evidence, never trust a completion label without its executed checks."
  [original original-hash closure current]
  (let [expected (:candidate_hash original)
        worker (get original :background_gate)
        commands (:commands worker)
        checks (:checks closure)
        stale? (or (not= expected current)
                   (= "stale" (:state closure))
                   (and (:before_hash closure) (not= expected (:before_hash closure)))
                   (and (:after_hash closure) (not= expected (:after_hash closure)))
                   (some #(or (not= expected (:before_hash %))
                              (not= expected (:after_hash %))) checks))
        invalid? (and closure
                      (or (not (pos-int? (:pid worker)))
                          (not (seq (:argv worker)))
                          (not (string? (:worker_started worker)))
                          (not= (:receipt_id original) (:receipt_id closure))
                          (not= (:pid worker) (:pid closure))
                          (not= (:argv worker) (:worker_argv closure))
                          (not= (:worker_started worker) (:worker_started closure))
                          (not= original-hash (:original_hash closure))
                          (not= expected (:candidate_hash closure))))
        failed (first (filter #(not (and (:finished? %) (= 0 (:exit %)))) checks))
        all? (and (seq commands) (= commands (mapv :command checks)))
        substantive? (some #(not= "true" (.getName (io/file (first %)))) commands)
        cold-complete? (and (nil? (:background_gate original))
                            (= "committed" (:state original))
                            (true? (:verification_complete original))
                            (empty? (:proof_pending original))
                            (seq (:checks original))
                            (every? #(and (not= "failed" (:status %))
                                       (or (= 0 (:exit %))
                                           (and (map? (:baseline %))
                                                (contains? #{"captured-reference-analysis" "candidate-lint-delta"} (:name %)))))
                                    (:checks original)))
        state (cond invalid? "failed" stale? "stale" cold-complete? "complete" (nil? closure) "pending"
                    (or failed (not all?) (not (contains? #{"complete" "pending"} (:state closure)))) "failed"
                    (not substantive?) "pending" :else "complete")]
    (cond-> {:ok (contains? #{"complete" "pending"} state) :state state
             :verification_complete (= "complete" state)
             :receipt_id (:receipt_id original) :receipt_path (:receipt_path original)
             :closure_receipt (:closure_receipt original) :candidate_hash expected
             :proof_pending (if (= "complete" state) [] (:proof_pending original))}
      failed (assoc :failed_command (:command failed) :exit (:exit failed))
      invalid? (assoc :error_type "proof-closure-identity-mismatch" :error "Closure identity does not match the original receipt.")
      (= "stale" state) (assoc :error_type "proof-snapshot-stale" :error "The committed source snapshot moved; closure is refused.")
      (and (= "failed" state) (not invalid?)) (assoc :error_type "proof-gate-failed" :error "Background proof failed; inspect the named command and closure receipt."))))

(defn status! [original-path]
  (try
    (let [bytes (read-text! original-path max-receipt-bytes)
          original (edn/read-string bytes)
          _ (when-not (and (true? (:committed original))
                        (string? (:receipt_id original))
                        (string? (:candidate_hash original))
                        (string? (:workspace_root original))
                        (seq (get-in original [:coverage :roots])))
              (throw (ex-info "Expected an original committed split receipt" {:error-type :invalid-proof-original})))
          _ (when-let [worker (:background_gate original)]
              (when-not (and (pos-int? (:pid worker)) (seq (:argv worker))
                             (string? (:worker_started worker)) (seq (:commands worker)))
                (throw (ex-info "Original receipt has an incomplete worker identity"
                                {:error-type :invalid-proof-original}))))
          closure-path (:closure_receipt original)
          pid (get-in original [:background_gate :pid])
          worker-started (get-in original [:background_gate :worker_started])
          alive? (when pid
                   (let [handle (java.lang.ProcessHandle/of pid)]
                     (and (.isPresent handle)
                          (.isAlive (.get handle))
                          (= worker-started
                             (some-> (.startInstant (.info (.get handle))) (.orElse nil) str)))))
          closure (when (and closure-path (.isFile (io/file closure-path)))
                    (read-edn! closure-path max-job-bytes))
          result (closure-status original (lens/source-hash bytes) closure
                                 (current-hash (:workspace_root original) (get-in original [:coverage :roots])))]
      (if (and (= "pending" (:state result)) (nil? closure) pid (not alive?))
        (assoc result :ok false :state "failed" :error_type "proof-worker-exited" :pid pid
               :error "The detached worker exited without a closure receipt.")
        result))
    (catch Exception e
      {:ok false :state "failed" :verification_complete false
       :error_type "proof-artifact-unavailable" :error (.getMessage e)})))

(defn- now [] (str (Instant/now)))

(defn runner-commands!
  "Resolve the two fixed worker executables before publication."
  []
  (admitted-tmpdir!)
  (mapv (fn [program]
          (or (some (fn [directory]
                      (let [file (io/file directory program)]
                        (when (and (.isFile file) (.canExecute file))
                          (.getCanonicalPath file))))
                    (str/split (or (System/getenv "PATH") "")
                               (re-pattern java.io.File/pathSeparator)))
              (throw (ex-info "Detached proof runner is unavailable"
                              {:error-type :background-gate-unavailable :program program}))))
        ["setsid" "bb"]))

(defn- write-once! [path data]
  (when (.exists (io/file path))
    (throw (ex-info "Immutable proof artifact already exists" {:error-type :proof-artifact-exists})))
  (file-ops/atomic-write! path (pr-str data)))

;; @spec NS-SPLIT-050
;; INTENT: NS-SPLIT-050
(defn launch!
  "Launch an owned detached process, then seal original and job exactly once.
  The worker waits for its sealed job; command streams never inherit the caller."
  [receipt dir capability]
  (let [tmpdir (admitted-tmpdir!)
        id (:receipt_id receipt)
        job-path (str (io/file dir (str id "-gate-job.edn")))
        config-path (str (io/file dir (str id "-bb.edn")))
        _ (write-once! config-path {})
        log-path (str (io/file dir (str id "-gate.log")))
        ;; Anchor relative classpath entries before the child changes cwd.
        classpath (str/join java.io.File/pathSeparator
                            (map #(str (.getAbsoluteFile (io/file %)))
                                 (str/split (System/getProperty "java.class.path")
                                            (re-pattern java.io.File/pathSeparator))))
        argv (into (or (:runner capability) (runner-commands!))
                   ["--config" config-path "--classpath" classpath "-m" "clj-surgeon.split-proof-gate" job-path])
        builder (doto (ProcessBuilder. ^java.util.List argv)
                  (.directory (io/file (:workspace_root receipt)))
                  (.redirectInput (io/file "/dev/null"))
                  (.redirectOutput (io/file log-path))
                  (.redirectErrorStream true))
        worker (.start builder)
        worker-started (some-> (.startInstant (.info (.toHandle worker))) (.orElse nil) str)
        _ (when-not worker-started
            (.destroyForcibly worker)
            (throw (ex-info "Detached worker start identity is unavailable"
                            {:error-type :background-gate-unavailable})))
        ;; @spec TEST-ISO-002 -- retain the launch even after a short worker exits.
        pid (spawn/record! (.pid worker) argv)
        receipt (assoc receipt :background_gate {:pid pid :argv argv :worker_started worker-started
                                                 :commands (:commands capability)
                                                 :log_path log-path :started (now)})]
    (try
      (write-once! (:receipt_path receipt) (bounded-receipt! receipt))
      (write-once! job-path {:receipt_id id :pid pid :original_path (:receipt_path receipt)
                             :worker_argv argv :worker_started worker-started
                             :original_hash (lens/source-hash (read-text! (:receipt_path receipt) max-receipt-bytes))
                             :closure_path (:closure_receipt receipt)
                             :workspace_root (:workspace_root receipt) :roots (get-in receipt [:coverage :roots])
                             :candidate_hash (:candidate_hash receipt) :profile (:profile capability)
                             :commands (:commands capability) :timeout-ms (:timeout-ms capability)
                             :java_tmpdir tmpdir})
      receipt
      (catch Throwable e
        (.destroyForcibly worker)
        (throw e)))))

(defn run-job! [job]
  (let [started (now)
        start (System/nanoTime)
        expected (:candidate_hash job)
        hash! #(current-hash (:workspace_root job) (:roots job))
        initial (hash!)
        outcomes (if (not= expected initial) []
                     (reduce
                       (fn [checks command]
                         (let [before (hash!)
                               began (now)
                               tick (System/nanoTime)
                               result (when (= expected before)
                                        (proof/run-proof! (:workspace_root job) (:profile job)
                                                          {:commands [command] :timeout-ms (:timeout-ms job)}))
                               check (first (:process_evidence result))
                               after (hash!)
                               output-path (str (:closure_path job) "-command-" (count checks) ".log")
                               _ (file-ops/atomic-write! output-path (or (:output check) ""))
                               row (merge (select-keys check [:exit :finished? :output-sha256 :output-truncated :launch-error])
                                          {:command command :argv (:command check) :started began :finished (now)
                                           :output_path output-path
                                           :wall_ms (/ (double (- (System/nanoTime) tick)) 1e6)
                                           :before_hash before :after_hash after})
                               rows (conj checks row)]
                           (if (and (= expected before after) (:ok result)) rows (reduced rows))))
                       [] (:commands job)))
        final (hash!)
        stale? (or (not= expected initial final)
                   (some #(not= expected (:before_hash %) (:after_hash %)) outcomes))
        substantive? (some #(not= "true" (.getName (io/file (first %)))) (:commands job))
        passed? (and (seq outcomes) (= (count outcomes) (count (:commands job)))
                     (every? #(and (:finished? %) (= 0 (:exit %))) outcomes))]
    {:receipt_id (:receipt_id job) :pid (:pid job) :worker_argv (:worker_argv job)
     :worker_started (:worker_started job) :original_hash (:original_hash job)
     :candidate_hash expected :before_hash initial :after_hash final
     :state (cond stale? "stale" (and passed? substantive?) "complete" passed? "pending" :else "failed")
     :verification_complete (and (not stale?) passed? (boolean substantive?))
     :java_tmpdir (System/getProperty "java.io.tmpdir")
     :started started :finished (now) :wall_ms (/ (double (- (System/nanoTime) start)) 1e6)
     :checks outcomes}))

(defn -main [& [job-path]]
  ;; Parent records the pid before making the job visible. A dead parent leaves
  ;; no runnable job; this bounded rendezvous cannot execute partial authority.
  (let [deadline (+ (System/currentTimeMillis) 30000)]
    (loop []
      (when-not (.isFile (io/file job-path))
        (when (> (System/currentTimeMillis) deadline)
          (throw (ex-info "Parent did not seal background proof job" {:error-type :proof-job-unsealed})))
        (Thread/sleep 20)
        (recur))))
  (let [job (read-edn! job-path max-job-bytes)]
    (System/setProperty "java.io.tmpdir" (:java_tmpdir job))
    (try
      (let [current (java.lang.ProcessHandle/current)
            actual-pid (.pid current)
            actual-started (some-> (.startInstant (.info current)) (.orElse nil) str)]
        (when-not (and (= (:pid job) actual-pid)
                       (= (:worker_started job) actual-started))
          (throw (ex-info "Worker identity differs from sealed job" {:error-type :proof-worker-mismatch}))))
      (when-not (= (:worker_argv job)
                   (get-in (read-edn! (:original_path job) max-receipt-bytes) [:background_gate :argv]))
        (throw (ex-info "Worker argv differs from sealed job" {:error-type :proof-worker-mismatch})))
      (when-not (= (:original_hash job) (lens/source-hash (read-text! (:original_path job) max-receipt-bytes)))
        (throw (ex-info "Original receipt changed before gate" {:error-type :proof-original-changed})))
      (write-once! (:closure_path job) (run-job! job))
      (catch Throwable e
        (let [current (java.lang.ProcessHandle/current)]
          (write-once! (:closure_path job)
                       (merge (select-keys job [:receipt_id :original_hash :candidate_hash])
                              {:pid (.pid current)
                               :worker_argv (:worker_argv job)
                               :worker_started (some-> (.startInstant (.info current)) (.orElse nil) str)}
                              {:state "failed" :verification_complete false :error (.getMessage e)
                               :checks [] :finished (now)})))))))
