(ns clj-surgeon.mcp-cold-verify
  "Bounded asynchronous verification jobs for checks that do not belong in the
   hot edit transaction."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.java.io :as io])
  (:import
   (java.lang ProcessHandle)
   (java.util UUID)
   (java.util.concurrent TimeUnit)))

(def max-retained-jobs 64)
(def max-running-jobs 4)
(def job-ttl-ms (* 24 60 60 1000))
(def max-output-characters 12000)
(defonce job-store (atom {}))

(defn valid-profile?
  "True when value is one closed, bounded cold-verification profile."
  [value]
  (and (map? value)
       (= #{:command :timeout-ms} (set (keys value)))
       (vector? (:command value))
       (seq (:command value))
       (every? #(and (string? %) (not-empty %)) (:command value))
       (integer? (:timeout-ms value))
       (<= 100 (:timeout-ms value) (* 30 60 1000))))

(defn clear-jobs!
  "Clear retained job metadata. Intended for isolated tests."
  []
  (reset! job-store {}))

(defn- now-ms [] (System/currentTimeMillis))

(defn- job-directory
  [project-root]
  (let [receipt-directory (io/file (workspace/receipt-dir project-root))]
    (io/file (.getParentFile receipt-directory) "verification-jobs")))

(defn- receipt-file
  [project-root job]
  (io/file (job-directory project-root) (str (subs job (count "verify/")) ".edn")))

(defn- public-job
  [job]
  (-> job
      (dissoc :workspace-root :created-at-ms :updated-at-ms :receipt-file :job)
      (assoc :verification_job (:job job))))

(defn- publish!
  [job]
  (let [file (:receipt-file job)]
    (.mkdirs (.getParentFile (io/file file)))
    (file-ops/atomic-write! file (pr-str (public-job job)))))

(defn- prune-jobs!
  []
  (let [cutoff (- (now-ms) job-ttl-ms)]
    (swap! job-store
           (fn [jobs]
             (let [fresh (into {}
                               (remove (fn [[_ job]]
                                         (and (not= :running (:status job))
                                              (< (:updated-at-ms job) cutoff))))
                               jobs)
                   terminal (->> fresh
                                 (remove (fn [[_ job]] (= :running (:status job))))
                                 (sort-by (comp :updated-at-ms val) >))
                   retained-terminal (set (map key (take max-retained-jobs terminal)))]
               (into {}
                     (filter (fn [[id job]]
                               (or (= :running (:status job))
                                   (contains? retained-terminal id))))
                     fresh))))))

(defn- finish-job!
  [id result]
  (when-let [job (get @job-store id)]
    (let [finished (merge job result {:updated-at-ms (now-ms)})]
      (swap! job-store assoc id finished)
      (publish! finished)
      finished)))

(defn- bounded-output
  [file]
  (let [output (slurp file)]
    (subs output 0 (min max-output-characters (count output)))))

(defn- destroy-process-tree!
  [^Process process]
  (with-open [descendants (.descendants (.toHandle process))]
    (let [handles (vec (iterator-seq (.iterator descendants)))]
      (doseq [^ProcessHandle descendant handles]
        (.destroyForcibly descendant))
      (.destroyForcibly process)
      (.waitFor process 5 TimeUnit/SECONDS)
      (doseq [^ProcessHandle descendant handles]
        (when (.isAlive descendant)
          (try
            (.get (.onExit descendant) 5 TimeUnit/SECONDS)
            (catch Exception _))))
      (and (not (.isAlive process))
           (every? #(not (.isAlive ^ProcessHandle %)) handles)))))

(defn- run-job!
  [id project-root command timeout-ms]
  (let [started (System/nanoTime)
        output-file (java.io.File/createTempFile "clj-surgeon-cold-" ".log")]
    (try
      (let [builder (-> (ProcessBuilder. ^java.util.List command)
                        (.directory (io/file project-root))
                        (.redirectErrorStream true)
                        (.redirectOutput output-file))
            environment (.environment builder)
            _ (process-env/configure-environment! environment)
            process (.start builder)
            _ (swap! job-store update id assoc :pid (.pid process))
            finished? (.waitFor process timeout-ms TimeUnit/MILLISECONDS)
            termination-confirmed? (or finished? (destroy-process-tree! process))
            exit (when finished? (.exitValue process))
            elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)
            ok? (and finished? (zero? exit))]
        (finish-job!
          id
          {:ok true
           :passed ok?
           :status (cond
                     ok? :passed
                     finished? :failed
                     termination-confirmed? :timed-out
                     :else :termination-failed)
           :exit exit
           :termination_confirmed termination-confirmed?
           :elapsed_ms elapsed
           :output (bounded-output output-file)
           :verification_complete true
           :next_action (if ok? "none" "review_failure_and_use_undo_receipt")}))
      (catch Exception error
        (finish-job!
          id
          {:ok true
           :passed false
           :status :failed
           :error-type :cold-verification-exception
           :error (or (.getMessage error) (.getName (class error)))
           :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
           :verification_complete true
           :next_action "review_failure_and_use_undo_receipt"}))
      (finally
        (.delete output-file)))))

(defn launch!
  "Launch one bounded cold-verification job and return immediately."
  [project-root profile-name {:keys [command timeout-ms]}]
  (let [canonical-root (.toString (mcp-paths/real-root project-root))
        id (str "verify/" (UUID/randomUUID))
        created (now-ms)
        job {:job id
             :profile profile-name
             :status :running
             :command command
             :timeout_ms timeout-ms
             :workspace-root canonical-root
             :created-at-ms created
             :updated-at-ms created
             :receipt-file (.toString (receipt-file canonical-root id))
             :verification_complete false
             :next_action "inspect_verification_job"}
        reserved
        (locking job-store
          (prune-jobs!)
          (let [running (count (filter #(= :running (:status %))
                                       (vals @job-store)))]
            (if (>= running max-running-jobs)
              {:ok false
               :error-type :cold-verification-capacity-exceeded
               :error "The bounded cold-verification worker pool is full"
               :running-jobs running
               :maximum max-running-jobs}
              (do (swap! job-store assoc id job) job))))]
    (if-not (:ok reserved true)
      reserved
      (do
        (publish! job)
        (future (run-job! id canonical-root command timeout-ms))
        (assoc (public-job job)
               :ok true
               :next_call {:tool "inspect_clojure"
                           :workspace_root canonical-root
                           :verification_job id
                           :view "verification"})))))

(defn attach-undo!
  "Attach the already-published inverse receipt to one workspace-owned job."
  [project-root id undo-receipt receipt-hash]
  (let [canonical-root (.toString (mcp-paths/real-root project-root))]
    (locking job-store
      (if-let [job (get @job-store id)]
        (if (= canonical-root (:workspace-root job))
          (let [updated (assoc job
                               :undo_receipt undo-receipt
                               :receipt_hash receipt-hash
                               :updated-at-ms (now-ms))]
            (swap! job-store assoc id updated)
            (publish! updated)
            {:ok true :verification_job id})
          {:ok false :error-type :verification-job-workspace-mismatch})
        {:ok false :error-type :unknown-or-expired-verification-job}))))

(defn attach-undo-from-verification!
  "Attach an inverse receipt when verification launched an asynchronous cold job."
  [project-root verification undo-receipt receipt-hash]
  (when-let [job-id (get-in verification [:cold-verification :verification_job])]
    (attach-undo! project-root job-id undo-receipt receipt-hash)))

(defn status
  "Return one job only to the canonical workspace that launched it."
  [project-root id]
  (prune-jobs!)
  (let [canonical-root (.toString (mcp-paths/real-root project-root))]
    (if-let [job (get @job-store id)]
      (if (= canonical-root (:workspace-root job))
        (let [result (cond-> (public-job job)
                       (not= :running (:status job))
                       (assoc :job_elapsed_ms (:elapsed_ms job)))]
          (if (= :running (:status result))
            (assoc result
                   :ok true
                   :next_call {:tool "inspect_clojure"
                               :workspace_root canonical-root
                               :verification_job id
                               :view "verification"})
            result))
        {:ok false
         :error-type :verification-job-workspace-mismatch
         :error "The verification job belongs to a different workspace"
         :verification_complete false
         :next_action "use_the_workspace_root_from_the_original_receipt"})
      {:ok false
       :error-type :unknown-or-expired-verification-job
       :error "The verification job is unknown or expired"
       :verification_complete false
       :next_action "rerun_the_verified_change_if_cold_proof_is_still_required"})))
