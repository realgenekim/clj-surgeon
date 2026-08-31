(ns clj-surgeon.experiments.embedded-elaborator-d1
  "One-turn, exact-comparator D1 dogfood through the embedded edit branch."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-elaborator-intent :as intent]
   [clj-surgeon.mcp-elaborator-supervisor :as supervisor]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.outline :as outline]
   [clojure.string :as str])
  (:import
   (java.nio.file LinkOption Paths)
   (java.time Instant)))

(def ^:private target-file
  "src/clj_surgeon/mcp_elaborator_supervisor.clj")

(def ^:private target-owner "close-session!")

(def ^:private binding-pattern
  #"(?<![A-Za-z0-9_?!*+./<>=$%&-])valid(?![A-Za-z0-9_?!*+./<>=$%&-])")

(defn- wait-for-boot
  [spark-supervisor]
  (let [deadline (+ (System/currentTimeMillis) 15000)]
    (loop []
      (let [state (supervisor/state spark-supervisor)]
        (if (or (not= :starting (:status state))
                (>= (System/currentTimeMillis) deadline))
          state
          (do (Thread/sleep 25) (recur)))))))

(defn- elapsed-ms
  [started]
  (/ (double (- (System/nanoTime) started)) 1000000.0))

(defn- emit!
  [value]
  (println (json/generate-string value))
  (flush))

(defn- canonical-root
  []
  (str (.toRealPath (Paths/get (System/getProperty "user.dir")
                               (make-array String 0))
                    (make-array LinkOption 0))))

(defn- captured-owner
  [root]
  (let [path (.resolve (Paths/get root (make-array String 0)) target-file)
        source (slurp (str path))
        owners (filter #(= target-owner (str (:name %)))
                       (outline/top-level-form-records target-file source))]
    (when-not (= 1 (count owners))
      (throw (ex-info "D1 target owner is not exact" {:count (count owners)})))
    {:path path :file-source source :owner-source (:source (first owners))}))

(defn- dogfood!
  [spark-supervisor root]
  (let [independent-started (System/nanoTime)
        {:keys [path file-source owner-source]} (captured-owner root)
        rename-count (count (re-seq binding-pattern owner-source))
        _ (when-not (= 3 rename-count)
            (throw (ex-info "D1 independent rename comparator is not exact"
                            {:rename_count rename-count})))
        expected (str/replace owner-source binding-pattern "ownership")
        expected-sha256 (intent/sha256 expected)
        independent-preparation-ms (elapsed-ms independent-started)
        callback-evidence (atom nil)
        request {:workspace_root root
                 :edits [{:file target-file
                          :within {:form target-owner}
                          :from owner-source
                          :to nil
                          :matches 1}]
                 :elaborate
                 {:decision
                  (str "In this Clojure form, rename only the local binding "
                       "`valid` to `ownership` and update only its references. "
                       "Preserve every other byte exactly, including whitespace "
                       "and literals. Return the complete resulting form.")}}
        started (System/nanoTime)
        result
        (supervisor/execute-edit!
          spark-supervisor
          {:project-root root}
          request
          (fn [completed]
            (let [candidate (get-in completed [:edits 0 :to])
                  exact? (= expected candidate)
                  compared-at (elapsed-ms started)]
              (reset! callback-evidence
                      {:candidate_sha256 (when candidate (intent/sha256 candidate))
                       :expected_sha256 expected-sha256
                       :exact_comparator exact?
                       :comparator_complete_ms compared-at})
              (if exact?
                (let [ordinary-started (System/nanoTime)
                      ordinary (mcp-tool/execute-request!
                                 {:project-root root
                                  :public-operation "edit_clojure"}
                                 completed)]
                  (swap! callback-evidence assoc
                         :ordinary_ms (elapsed-ms ordinary-started))
                  ordinary)
                {:ok false
                 :error_type "d1-independent-comparator-mismatch"
                 :source_unchanged true}))))
        after-source (slurp (str path))]
    {:event "d1-dogfood"
     :timestamp (str (Instant/now))
     :target {:file target-file
              :owner target-owner
              :old_file_sha256 (intent/sha256 file-source)
              :new_file_sha256 (intent/sha256 after-source)
              :old_owner_sha256 (intent/sha256 owner-source)
              :expected_owner_sha256 expected-sha256
              :rename_count rename-count}
     :comparison @callback-evidence
     :independent_preparation_ms independent-preparation-ms
     :result {:ok (:ok result)
              :error_type (:error_type result)
              :verification_complete (:verification_complete result)
              :receipt_hash (:receipt_hash result)
              :elaboration (:elaboration result)}
     :pipeline_ms (elapsed-ms started)
     :source_changed (not= file-source after-source)}))

(defn -main
  [& _]
  (let [root (canonical-root)
        spark-supervisor (supervisor/start-background!)
        boot-started (System/nanoTime)
        boot (wait-for-boot spark-supervisor)]
    (try
      (emit! {:event "d1-boot"
              :timestamp (str (Instant/now))
              :boot_ms (elapsed-ms boot-started)
              :state boot})
      (if (= :available (:status boot))
        (let [result (dogfood! spark-supervisor root)]
          (emit! result)
          (when-not (and (get-in result [:result :ok])
                         (get-in result [:comparison :exact_comparator])
                         (:source_changed result))
            (throw (ex-info "D1 dogfood failed" result))))
        (throw (ex-info "D1 supervisor unavailable" {:state boot})))
      (finally
        (emit! {:event "d1-shutdown"
                :timestamp (str (Instant/now))
                :cleanup (supervisor/stop! spark-supervisor)})))))
