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
   (java.util UUID)))

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

(defn- analyzer-authority-error-type
  [result]
  (let [status (get-in result [:admission :status])
        error-type (or (get-in result [:admission :error-type])
                       (:error-type result))]
    (cond
      (= :admission-timeout status) :clj-kondo-admission-timeout
      (= :pressure-deferred status) :clj-kondo-pressure-deferred
      (= :delegated status) :clj-kondo-admission-unverified
      (#{:clj-kondo-admission-unavailable
         :clj-kondo-executable-unavailable
         :clj-kondo-exec-failed
         :clj-kondo-pressure-deferred
         :process-interrupted} error-type) error-type
      :else nil)))

(defn- run-job!
  [id project-root command timeout-ms]
  (let [started (System/nanoTime)]
    (try
      (let [process (process-env/run-bounded!
                      {:command command
                       :cwd project-root
                       :timeout-ms timeout-ms
                       :merge-error? true
                       :visible-byte-limit max-output-characters
                       :on-start #(swap! job-store update id assoc :pid %)})
            authority-error (analyzer-authority-error-type process)
            ok? (and (not authority-error)
                     (:finished? process)
                     (zero? (:exit process)))
            result {:ok true
                    :passed ok?
                    :status (cond
                              authority-error :unverified
                              ok? :passed
                              (:finished? process) :failed
                              (:termination-confirmed process) :timed-out
                              :else :termination-failed)
                    ;; @spec MCP-OP-ALIAS-059
                    ;; forwarded-refusal-kind: the kind analyzer-authority-
                    ;; error-type mints in this file travels verbatim
                    :error-type authority-error
                    :exit (:exit process)
                    :termination_confirmed (:termination-confirmed process)
                    :elapsed_ms (:elapsed_ms process)
                    :output (:output process)
                    :output_bytes (:output-bytes process)
                    :output_sha256 (:output-sha256 process)
                    :output_truncated (:output-truncated process)
                    :admission (:admission process)
                    :verification_complete true
                    :next_action (cond
                                   authority-error
                                   "restore_analyzer_authority_before_retry"
                                   ok? "none"
                                   :else "review_failure_and_use_undo_receipt")}]
        (finish-job!
          id result))
      (catch Exception error
        (let [data (ex-data error)
              authority-error (or (:error-type data)
                                  (get-in data [:admission :error-type]))
              authority-unverified? (#{:clj-kondo-admission-unavailable
                                       :clj-kondo-executable-unavailable
                                       :clj-kondo-exec-failed
                                       :clj-kondo-pressure-deferred
                                       :process-interrupted}
                                     authority-error)]
          (finish-job!
            id
            {:ok true
             :passed false
             :status (if authority-unverified? :unverified :failed)
             :error-type (if authority-unverified?
                           authority-error
                           :cold-verification-exception)
             :error (or (.getMessage error) (.getName (class error)))
             :admission (:admission data)
             :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
             :verification_complete true
             :next_action (if authority-unverified?
                            "restore_analyzer_authority_before_retry"
                            "review_failure_and_use_undo_receipt")}))))))

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
