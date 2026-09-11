(ns clj-surgeon.rename-alias
  "Closed-snapshot shell over the shared insertion and transaction entrances."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-plan :as p]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.rename-alias-plan :as planner]
   [clj-surgeon.txn-journal :as journal]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   (java.nio.file Files LinkOption)
   (java.util UUID)))

(def plan planner/plan)
(def receipt-text insert/receipt-text)
(defn read-request [text]
  (let [r (insert/read-request text)] (if (:error-type r) (assoc r :operation "rename_alias" :version 1) r)))
(defn failure [attempted kind message]
  (assoc (insert/failed (if attempted "recovery-required" "failed") attempted (when-not attempted true)
           (if attempted :commit-outcome-unknown kind) message {})
         :operation "rename_alias" :version 1 :at []
         :remedy (if attempted "Inspect the durable journal and fresh file hashes; never replay or overwrite external bytes."
                     "Repair the I/O failure and reconsider the guarded request.")))
(defn target! [r f] (insert/target! {:workspace_root (:workspace_root r) :file f}))
(defn scope-paths! [r]
  (let [scope (:scope r)]
    (vec (sort
           (cond
             (:file scope) [(:file scope)]
             (:paths scope) (:paths scope)
             :else
             (let [root (.toPath (io/file (:workspace_root r)))]
               (when-not (and (.isAbsolute root) (Files/isDirectory root insert/nofollow)
                              (= (str root) (str (.toRealPath root (make-array LinkOption 0)))))
                 (p/refuse! :invalid-path [:workspace_root] "Existing canonical absolute root required."))
               (letfn [(visit [^java.io.File dir]
                         (mapcat (fn [^java.io.File f]
                                   (let [path (.toPath f) relative (str (.relativize root path))]
                                     (cond
                                       (= ".git" (.getName f)) []
                                       (Files/isSymbolicLink path) (p/refuse! :invalid-path [:scope] "Repository scope contains a symlink." {:file relative})
                                       (.isDirectory f) (visit f)
                                       (re-find #"\.clj[sc]?$" (.getName f)) [relative]
                                       :else [])))
                                 (or (seq (.listFiles dir)) [])))]
                 (let [paths (vec (take 1001 (visit (.toFile root))))]
                   (when (> (count paths) 1000) (p/refuse! :limit-exceeded [:scope] "At most 1000 inspected files."))
                   paths))))))))
(defn observations [r sources candidates]
  (into (sorted-map)
        (for [[f original] sources]
          [f (try
               (let [hash (journal/sha256-file (target! r f))]
                 {:state (cond (= hash (some-> (candidates f) p/sha)) "candidate"
                               (= hash (p/sha original)) "original" :else "external")
                  :observed_sha256 hash})
               (catch Exception _ {:state "unknown"}))])))
(defn transaction-status [r sources candidates replaced restored]
  (let [states (observations r sources candidates)
        candidate-paths (filter #(= "candidate" (get-in states [% :state])) (keys candidates))
        unknown? (some #(= "unknown" (:state %)) (vals states))]
    {:partial_write (when-not unknown? (< 0 (count candidate-paths) (count candidates)))
     :replaced_files (vec replaced) :restored_files (vec restored)
     :pending_files (vec (remove (set replaced) (keys candidates)))
     :unresolved_files (vec (filter #(#{"unknown" "external"} (get-in states [% :state])) (keys states)))
     :file_states states}))
(defn detail-sections [detail]
  (let [present (set (mapcat keys (filter map? (tree-seq coll? seq detail))))]
    (mapv name (filter present [:per_file :sites :preservation :inverse_splices]))))

;; @spec RENAME-ALIAS-013
;; INTENT: RENAME-ALIAS-013
(defn receipt-projector
  "EVERY BOOLEAN IN A RECEIPT MUST HAVE A WITNESS IN WHICH IT IS FALSE.
   Project read-back facts from observed disk text and declared change ordinals;
   candidate hashes are expectations, never observations."
  [sources result disk]
  (let [per-file
        (mapv (fn [entry]
                (let [f (:file entry) observed (get disk f)
                      indices (set (map :form_index (:sites entry)))
                      proof (try (planner/form-evidence (p/tree (sources f) :source)
                                                        (p/tree observed :candidate) indices)
                                 (catch Exception _
                                   {:preservation {:other_forms_checked (get-in entry [:preservation :other_forms_checked])
                                                   :other_forms_unchanged false :gaps_unchanged false :discards_unchanged false}}))]
                  (merge entry proof {:read_back_hash (when observed (p/sha observed))})))
              (get-in result [:detail :per_file]))
        hashes (into {} (map (juxt :file :read_back_hash)) per-file)]
    {:per_file per-file
     :facts {:read_back_hashes (select-keys hashes (keys (:candidates result)))
             :other_forms_checked (reduce + (map #(get-in % [:preservation :other_forms_checked]) per-file))
             :other_forms_unchanged (every? #(get-in % [:preservation :other_forms_unchanged]) per-file)
             :write_verified (every? #(= (:result_hash %) (:read_back_hash %)) per-file)}}))

(defn write-detail! [path detail]
  (let [text (str (pr-str detail) "\n")]
    (io/make-parents path) (file-ops/atomic-write! path text)
    (when-not (= (p/sha text) (journal/sha256-file path))
      (throw (java.io.IOException. "Durable receipt read-back differs.")))
    {:receipt_details_path path :receipt_hash (p/sha text)
     :details_contains (detail-sections (edn/read-string (slurp path :encoding "UTF-8")))}))
(defn trim-summary [r]
  (let [r (if-let [e (:write_refusal_evidence r)]
            (assoc r :write_refusal_evidence
                   (loop [items (vec (take 10 (:items e)))]
                     (let [e (assoc e :items items :returned_count (count items)
                                    :omitted_count (- (:available_count e) (count items))
                                    :truncated (< (count items) (:available_count e)))]
                       (if (and (> (alength (p/bytes (pr-str e))) 1800) (seq items)) (recur (pop items)) e)))) r)
        r (dissoc r :bindings :sites)]
    (if (< (alength (p/bytes (pr-str r))) 3800) r
        (let [sections [:paths :per_file_counts :expected_count :actual_count :mismatched_files :read_back_hashes :transaction]
              r (reduce (fn [r k] (if (and (> (alength (p/bytes (pr-str r))) 3500) (coll? (get r k)))
                                    (-> r (dissoc k) (update :details_contains (fnil conj []) (name k))) r)) r sections)]
          (if (> (alength (p/bytes (pr-str r))) 3800)
            (-> r (dissoc :write_refusal_evidence) (update :details_contains (fnil conj []) "write_refusal_evidence")) r)))))
(defn persist-refusal! [r result]
  (let [path (artifacts/target "rename-alias" (:workspace_root r) (str (UUID/randomUUID) ".edn"))
        detail {:request r :receipt result}]
    (trim-summary (merge result (write-detail! path detail)))))
(defn recheck! [r paths sources identities]
  (let [current (scope-paths! r)]
    (when-not (= paths current)
      (p/refuse! :scope-changed-before-commit [:scope] "Repository membership changed before publication."
                 {:expected paths :actual current :next_action "refresh-source"})))
  (doseq [[f source] sources]
    (let [target (target! r f) actual (journal/sha256-file target)]
      (when-not (and (= (identities f) (journal/path-identity target)) (= (p/sha source) actual))
        (p/refuse! :source-changed-before-commit [:guards f] "Source identity or bytes changed before publication."
                   {:file f :expected (p/sha source) :actual actual})))))

;; @spec RENAME-ALIAS-009
;; INTENT: RENAME-ALIAS-009
(defn commit-plan! [r sources result identities hooks]
  (let [candidates (into (sorted-map) (:candidates result)) paths (vec (keys sources))
        path (artifacts/target "rename-alias" (:workspace_root r) (str (UUID/randomUUID) ".edn"))
        artifact (atom {}) attempted (atom false) stages (atom {}) seeds (atom [])
        replaced (atom []) restored (atom []) read-hook-fired (atom false)
        hook! (fn [k] (when-let [f (hooks k)] (f)))
        status #(transaction-status r sources candidates @replaced @restored)
        detail (atom (assoc (:detail result) :receipt (assoc (:receipt result) :state "planned" :committed false
                                                        :mutation_attempted false :source_unchanged true :next_action "await-publication")))
        persist! (fn [receipt]
                   (swap! detail assoc :receipt receipt :transaction (status))
                   (reset! artifact (write-detail! path @detail)))
        compiled {:ok true :original-sources sources :future-sources (merge sources candidates)
                  :files (mapv (fn [[f candidate]] {:file f :source-hash (p/sha (sources f)) :result-hash (p/sha candidate) :match-count 1}) candidates)}]
    (try
      (persist! (:receipt @detail))
      (doseq [[f candidate] candidates]
        (hook! :stage)
        (let [seed (io/file (str path "." (count @seeds) ".candidate"))]
          (swap! seeds conj seed)
          (spit seed candidate :encoding "UTF-8")
          (let [stage (file-ops/prepare-publish! (target! r f) seed)]
            (swap! stages assoc f stage)
            (when-not (= (p/sha candidate) (journal/sha256-file stage))
              (throw (java.io.IOException. "Staged candidate differs."))))))
      (swap! detail assoc :staged_files (into {} (map (fn [[f stage]] [f (str stage)]) @stages)))
      (persist! (:receipt @detail))
      (hook! :before-recheck)
      (recheck! r paths sources identities)
      (let [read-source (fn [f] (insert/read-bounded (target! r f) :candidate))
            outcome
            (transaction/commit-compiled!
              compiled
              {:read-source read-source
               :write-source!
               (fn [f text]
                 (if (= text (sources f))
                   (do
                     (hook! :restore)
                     (when-not (= (p/sha (candidates f)) (journal/sha256-file (target! r f)))
                       (throw (java.io.IOException. "Recovery target changed.")))
                     (file-ops/atomic-write! (target! r f) text)
                     (when-not (= (p/sha text) (journal/sha256-file (target! r f)))
                       (throw (java.io.IOException. "Restoration read-back differs.")))
                     (swap! restored conj f)
                     (persist! (assoc (:receipt @detail) :state "rolling-back" :mutation_attempted true)))
                   (let [target (target! r f)]
                     (when-not (and (= (identities f) (journal/path-identity target)) (= (p/sha (sources f)) (journal/sha256-file target)))
                       (p/refuse! :source-changed-before-commit [:guards f] "Final target identity or digest differs."))
                     (when (= 1 (count @replaced)) (hook! :second-replacement))
                     (persist! (assoc (:receipt @detail) :state "publishing" :mutation_attempted true :source_unchanged nil :replacement_attempt f))
                     (reset! attempted true)
                     (file-ops/publish-prepared! target (@stages f))
                     (hook! :external-after-write)
                     (when (compare-and-set! read-hook-fired false true) (hook! :read-back))
                     (when-not (= (p/sha text) (journal/sha256-file (target! r f)))
                       (throw (java.io.IOException. "Replacement read-back differs.")))
                     (swap! replaced conj f)
                     (persist! (assoc (:receipt @detail) :state "publishing" :mutation_attempted true :source_unchanged false))
                     (when (= (count @replaced) (count candidates))
                       (recheck! r paths (merge sources candidates)
                                 (merge identities (into {} (map (fn [p] [p (journal/path-identity (target! r p))]) (keys candidates)))))))))})
            disk (into {} (for [f paths] [f (try (read-source f) (catch Exception _ nil))]))
            projected (receipt-projector sources result disk)
            _ (swap! detail assoc :per_file (:per_file projected))
            snapshot (status)
            complete? (every? (fn [[f state]] (= (:state state) (if (contains? candidates f) "candidate" "original"))) (:file_states snapshot))
            receipt (cond
                      (and (:ok outcome) complete? (get-in projected [:facts :write_verified]))
                      (merge {:state "committed" :committed true :mutation_attempted true :source_unchanged false :ok true}
                             (:receipt result) (:facts projected))
                      (not @attempted)
                      (planner/refuse (ex-info (:error outcome) {:error-type :source-changed-before-commit :at [:guards]}))
                      (:rolled-back outcome)
                      (assoc (failure true :io-error (:error outcome)) :state "rolled-back" :committed false :source_unchanged true
                             :error-type :io-error :next_action "retry-after-repair")
                      :else (failure true :commit-outcome-unknown (or (:error outcome) "Final snapshot changed.")))
            receipt (merge receipt (:facts projected) {:transaction snapshot})]
        (hook! :receipt-finalize)
        (persist! receipt)
        (trim-summary (merge receipt @artifact)))
      (catch Exception e
        (let [receipt (merge (if (and (not @attempted) (:error-type (ex-data e))) (planner/refuse e)
                               (failure @attempted :io-error (.getMessage e)))
                             {:transaction (status)} @artifact)]
          (try (persist! receipt) (merge receipt @artifact)
               (catch Exception _ (assoc receipt :receipt_persistence_failed true)))))
      (finally
        (doseq [f (concat (vals @stages) @seeds)] (Files/deleteIfExists (.toPath ^java.io.File f)))))))

(defn recovery-status
  "Read-only recovery classification. First establish publisher has stopped. Hash every inspected
   path freshly; candidates must never be replayed, originals require fresh guards, external or
   unreadable files require manual reconciliation. Retain the journal; never undo external bytes."
  [detail hashes]
  (into {} (for [{:keys [file source_hash result_hash]} (:per_file detail)]
             [file (insert/recovery-status {:receipt {:source_hash source_hash :result_hash result_hash}} (get hashes file))])))
(defn execute!
  ([request] (execute! request {}))
  ([request hooks]
   (let [start (System/nanoTime)
         completed (atom nil)
         result (try
                  (planner/validate! request)
                  (file-ops/with-publish-lock*
                    (journal/transactions-dir (:workspace_root request))
                    (fn []
                      (let [paths (scope-paths! request)]
                        (planner/paths! paths request)
                        (let [sources (loop [remaining paths total 0 captured (sorted-map)]
                                        (if-let [f (first remaining)]
                                          (let [target (target! request f)
                                                size (Files/size (.toPath target))]
                                            (when (> (+ total size) 67108864)
                                              (p/refuse! :limit-exceeded [:source] "Aggregate source exceeds 64 MiB."))
                                            (let [text (insert/read-bounded target :source)
                                                  total (+ total (alength (p/bytes text)))]
                                              (when (> total 67108864)
                                                (p/refuse! :limit-exceeded [:source] "Aggregate source grew beyond 64 MiB."))
                                              (recur (next remaining) total (assoc captured f text)))) captured))
                              identities (into {} (map (fn [f] [f (journal/path-identity (target! request f))]) paths))
                              planned (plan sources request)]
                          (reset! completed
                            (if (:ok planned) (commit-plan! request sources planned identities hooks)
                                (persist-refusal! request planned)))))))
                  (catch Exception e
                    (if (:mutation_attempted @completed)
                      (merge (failure true :commit-outcome-unknown (.getMessage e))
                             (select-keys @completed [:transaction :receipt_details_path :receipt_hash]))
                      (if (:error-type (ex-data e)) (planner/refuse e) (failure false :io-error (.getMessage e))))))]
     (trim-summary (assoc result :elapsed_ms (/ (- (System/nanoTime) start) 1e6))))))

;; @spec RENAME-ALIAS-011
;; INTENT: RENAME-ALIAS-011
(defn cli! [opts]
  (let [start (System/nanoTime)]
    (try
      (p/closed! opts [:op :request-file] [] [] :invalid-request)
      (when-not (string? (:request-file opts)) (p/refuse! :invalid-request [:request-file] "Request file required."))
      (let [r (read-request (insert/read-bounded (:request-file opts) :request))]
        (if (:error-type r) (assoc r :elapsed_ms (/ (- (System/nanoTime) start) 1e6)) (execute! r)))
      (catch Exception e
        (assoc (if (:error-type (ex-data e)) (planner/refuse e) (failure false :io-error (.getMessage e)))
               :elapsed_ms (/ (- (System/nanoTime) start) 1e6))))))
