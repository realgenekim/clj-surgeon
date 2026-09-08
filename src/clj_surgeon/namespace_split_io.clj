(ns clj-surgeon.namespace-split-io
  "Confined snapshot, baseline-relative lint, shared extraction publish/proof/inverse."
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-operation :as operation]
   [clj-surgeon.mcp-paths :as paths]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.namespace-split :as split]
   [clj-surgeon.namespace-split-warm :as warm]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.split-proof-gate :as gate]
   [clj-surgeon.structural-lens :as lens]
   [clj-surgeon.synchronous-verification :as proof]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [rewrite-clj.parser :as parser])
  (:import
   (java.nio.file FileVisitOption Files LinkOption)
   (java.util UUID)))

(def max-files 4000)
(def max-bytes (* 16 1024 1024))
(def max-analysis-bytes (* 64 1024 1024))
(def max-file-bytes (* 2 1024 1024))
(def string-schema {:type "string" :minLength 1})
(def strings-schema {:type "array" :items string-schema :uniqueItems true})
(defn object-schema [properties required]
  {:type "object" :additionalProperties false :properties properties :required required})

;; @spec NS-SPLIT-013
(def schema
  (object-schema
    {"workspace_root" string-schema
     "source" (object-schema {"file" string-schema "lib" string-schema
                              "retain" {:type "boolean"}
                              "alias_policy" (assoc strings-schema :minItems 1)
                              "comment_policy" {:type "string" :enum ["remove-moved-invocations"]}} ["file" "lib"])
     "destinations" {:type "array" :minItems 1 :maxItems 1000
                     :items (object-schema {"lib" string-schema "file" string-schema
                                            "doc" string-schema
                                            "forms" strings-schema
                                            "alias_policy" (assoc strings-schema :minItems 1)}
                                           ["lib" "file" "forms" "alias_policy"])}
     "promotion_policy" {:oneOf [{:type "string" :enum ["promote-required"]} strings-schema]}
     "source_retirement" {:type "string" :enum ["delete" "retain-empty"]}
     "roots" (assoc strings-schema :minItems 1 :maxItems 32)
     "constraints" (object-schema {"forbidden_edges" {:type "array" :items {:type "array" :items string-schema :minItems 2 :maxItems 2}}} [])
     "verification" (object-schema {"profile" string-schema "profile-file" string-schema} ["profile"])
     "expect" (object-schema (into {} (for [k ["files" "forms" "destinations" "caller_files" "caller_sites"]]
                                        [k {:type "integer" :minimum 0}])) [])
     "snapshot_hash" string-schema
     "plan_only" {:oneOf [{:type "boolean"} {:type "string" :enum ["facts"]}]}}
    ["workspace_root" "source" "destinations" "promotion_policy" "roots" "verification"]))

(defn- schema-errors [s x path]
  (if-let [alternatives (:oneOf s)]
    (if (some #(empty? (schema-errors % x path)) alternatives) [] [{:path path :reason :no-matching-shape}])
    (let [type-ok? (case (:type s)
                     "object" (map? x) "array" (vector? x) "string" (string? x)
                     "integer" (integer? x) "boolean" (boolean? x) true)]
      (if-not type-ok? [{:path path :reason :wrong-type :expected (:type s)}]
        (vec (concat
               (when (and (:enum s) (not (some #{x} (:enum s)))) [{:path path :reason :invalid-value}])
               (case (:type s)
                 "object" (concat (for [k (:required s) :when (not (contains? x (keyword k)))] {:path (conj path k) :reason :missing-field})
                                  (for [k (keys x) :when (not (contains? (:properties s) (name k)))] {:path (conj path (name k)) :reason :unknown-field})
                                  (mapcat (fn [[k v]] (when-let [child (get (:properties s) (name k))] (schema-errors child v (conj path (name k))))) x))
                 "array" (concat (when (or (and (:minItems s) (< (count x) (:minItems s)))
                                           (and (:maxItems s) (> (count x) (:maxItems s)))) [{:path path :reason :array-bound}])
                                 (when (and (:uniqueItems s) (not= (count x) (count (distinct x)))) [{:path path :reason :duplicate-item}])
                                 (mapcat (fn [[i v]] (schema-errors (:items s) v (conj path i))) (map-indexed vector x)))
                 "string" (when (str/blank? x) [{:path path :reason :blank-string}])
                 "integer" (when (and (:minimum s) (< x (:minimum s))) [{:path path :reason :below-minimum}]) nil)))))))

;; @spec NS-SPLIT-041
;; INTENT: NS-SPLIT-041
(defn validate-request [request]
  (let [errors (schema-errors schema request [])
        bad-aliases (for [d (cons (:source request) (:destinations request)) a (:alias_policy d)
                          :when (not (and (string? a) (re-matches #"[A-Za-z][A-Za-z0-9_-]*" a)))]
                      {:path ["destinations" (:lib d) "alias_policy"] :reason :invalid-alias})
        bad-libs (for [lib (cons (get-in request [:source :lib]) (map :lib (:destinations request)))
                       :when (not (and (string? lib) (re-matches #"[A-Za-z_][A-Za-z0-9_!?*+\-]*(?:\.[A-Za-z_][A-Za-z0-9_!?*+\-]*)*" lib)))]
                   {:path ["lib"] :reason :invalid-library :lib lib})]
    (vec (concat errors bad-aliases bad-libs
                 (when (if (true? (get-in request [:source :retain]))
                         (contains? request :source_retirement)
                         (not (contains? request :source_retirement)))
                   [{:path ["source_retirement"] :reason :retention-policy-conflict}])
                 (when (and (not (true? (get-in request [:source :retain])))
                            (get-in request [:source :comment_policy]))
                   [{:path ["source" "comment_policy"] :reason :requires-retained-source}])))))

(defn- refuse! [type message data]
  (throw (ex-info message (assoc data :error-type type))))

(defn- delete-tree! [root]
  (doseq [f (reverse (file-seq (io/file root)))] (Files/deleteIfExists (.toPath f))))

;; @spec NS-SPLIT-015
(defn capture!
  "Capture bounded, non-symlink source roots. No discovery outside this universe."
  [root roots]
  (let [paths (mapv (fn [relative]
                      (when-not (and (string? relative) (not (str/starts-with? relative "/"))
                                     (every? #(not (#{"" "." ".."} %)) (str/split relative #"/")))
                        (refuse! :invalid-root "Roots must be bounded relative directory paths" {:root relative}))
                      (let [p (.resolve root relative)]
                        (when-not (and (Files/isDirectory p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                                       (.startsWith (.toRealPath p (make-array LinkOption 0)) root)
                                       (= p (.toRealPath p (make-array LinkOption 0))))
                          (refuse! :unconfined-root "Source root is absent or traverses a symlink" {:root relative})) p)) roots)
        files (vec (distinct
                     (mapcat (fn [p]
                               (with-open [stream (Files/walk p (make-array FileVisitOption 0))]
                                 (let [entries (vec (take 50001 (iterator-seq (.iterator stream))))]
                                   (when (> (count entries) 50000)
                                     (refuse! :entry-bound "Discovery entry bound exceeded" {:max_entries 50000}))
                                   (->> entries
                                        (filter #(re-find #"\.clj[sc]?$" (str %)))
                                        (take (inc max-files)) vec)))) paths)))]
    (when (> (count files) max-files) (refuse! :file-bound "Source file bound exceeded" {:max_files max-files}))
    (doseq [p files]
      (when-not (and (Files/isRegularFile p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                     (.startsWith (.toRealPath p (make-array LinkOption 0)) root))
        (refuse! :unconfined-source "Source file must be a confined regular file" {:file (str p)})))
    (when (some #(> (Files/size %) max-file-bytes) files)
      (refuse! :file-byte-bound "One source exceeds the per-file byte bound" {:max_file_bytes max-file-bytes}))
    (when (> (reduce + 0 (map #(Files/size %) files)) max-bytes)
      (refuse! :byte-bound "Source byte bound exceeded" {:max_bytes max-bytes}))
    (into (sorted-map) (map (fn [p] [(str (.relativize root p)) (slurp (str p))])) files)))

;; @spec NS-SPLIT-015
;; @spec NS-SPLIT-044
;; INTENT: NS-SPLIT-044
(defn analyze!
  "One serialized clj-kondo run over exact captured bytes in an isolated mirror."
  [sources]
  (let [started (System/nanoTime)
        temp (Files/createTempDirectory "namespace-split-analysis-" (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (doseq [[file source] sources]
        (let [f (io/file (str temp) file)] (.mkdirs (.getParentFile f)) (spit f source)))
      (let [result (process/run-bounded!
                     {:command [(str (System/getProperty "user.home") "/bin/clj-kondo") "--lint" (str temp)
                                "--cache" "false" "--config"
                                (pr-str {:output {:format :edn :analysis {:var-definitions true :var-usages true
                                                                          :namespace-usages true :java-class-usages true}}})]
                      :cwd (str temp) :timeout-ms 120000 :visible-byte-limit max-analysis-bytes})
            data (when (and (:finished? result) (not (:out-truncated result)))
                   (edn/read-string (:out result)))]
        (when-not (and (= :admitted (get-in result [:admission :status]))
                       (#{0 2 3} (:exit result)) (map? (:analysis data))
                       (vector? (:findings data)))
          (refuse! :analysis-unavailable "The captured-snapshot analyzer did not complete"
                   {:exit (:exit result) :finished (:finished? result) :truncated (:out-truncated result)}))
        {:analysis (walk/postwalk (fn [x] (if (and (map? x) (:filename x))
                                            (update x :filename #(str (.relativize temp (.toPath (io/file %))))) x))
                     (:analysis data))
         :findings (:findings data)
         :check {:name "captured-reference-analysis" :exit (:exit result) :duration_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
                 :status "baseline" :baseline (select-keys (:summary data) [:error :warning :info])
                 :note "Non-zero exit is pre-mutation baseline lint, not split damage; candidate delta is checked separately."}})
      (finally (delete-tree! (str temp))))))

(defn- finding-counts [findings]
  (merge {:error 0 :warning 0 :info 0} (frequencies (map :level findings))))

;; INTENT: NS-SPLIT-016
(defn lint-comparison
  "Compare error type/message multisets, ignoring coordinates that move during a
  split. Removing one error never cancels a different introduced error."
  [baseline candidate]
  (let [before (finding-counts (:findings baseline))
        after (finding-counts (:findings candidate))
        errors #(frequencies (map (juxt :type :message) (filter (fn [f] (= :error (:level f))) (:findings %))))
        old (errors baseline)
        introduced (reduce-kv (fn [total k v] (+ total (max 0 (- v (get old k 0))))) 0 (errors candidate))]
    {:name "candidate-lint-delta" :exit (get-in candidate [:check :exit])
     :duration_ms (get-in candidate [:check :duration_ms])
     :status (if (pos? introduced) "failed" "passed")
     :baseline before :post after :delta (merge-with - after before)
     :introduced_errors introduced
     :note (if (pos? introduced)
             "New error findings relative to baseline lint block publication."
             "Non-zero exit is baseline lint, not split damage; no new error findings.")}))

;; INTENT: NS-SPLIT-020
(defn proof-check [profile check]
  (let [[executable argument] (:command check)
        executable (.getName (io/file executable))
        label (cond
                (= "kaocha" executable) (str executable (when argument (str " " argument)))
                (and argument (re-find #"\.(py|sh|clj)$" argument)) (.getName (io/file argument))
                :else executable)]
    {:name label :profile profile :command (:command check)
     :exit (:exit check) :duration_ms (:elapsed_ms check)
     :status (if (and (:finished? check) (zero? (or (:exit check) 1))) "passed" "failed")}))

(defn- canonical-compiled [root compiled]
  (let [canonical #(str (.resolve root %))
        file-map #(into (sorted-map) (map (fn [[f s]] [(canonical f) s])) %)
        targets (mapv #(paths/resolve-new-source-path root %) (:created-files compiled))
        bad (first (remove :ok targets))]
    (when bad (refuse! :invalid-destination "Destination path is not available" bad))
    (-> compiled
        (update :original-sources file-map) (update :future-sources file-map) (update :guard-sources file-map)
        (update :created-files #(mapv canonical %)) (update :deleted-files #(mapv canonical %))
        (assoc :created-directories (vec (distinct (mapcat #(map str (:missing-parent-directories %)) targets)))))))

(defn- save! [dir name data]
  (.mkdirs (io/file dir))
  (let [file (str (io/file dir name))] (file-ops/atomic-write! file (pr-str data)) file))

;; @spec NS-SPLIT-030
;; INTENT: NS-SPLIT-030
(defn- anchored-profiles [root profiles]
  (into {} (for [[profile spec] profiles]
             [profile (if-let [capability (proof/profile-capability spec)]
                        (cond-> {:commands (mapv (fn [argv]
                                                   (let [exe (first argv)]
                                                     (if (and (str/includes? exe "/") (not (.isAbsolute (io/file exe))))
                                                       (assoc argv 0 (str (.resolve root exe))) argv)))
                                             (:commands capability))}
                          (:timeout-ms capability) (assoc :timeout-ms (:timeout-ms capability))
                          (contains? spec :proof) (assoc :proof (:proof spec))
                          (contains? spec :gate) (assoc :gate (:gate spec)))
                        spec)])))

(def max-profile-bytes (* 1024 1024))

;; @spec NS-SPLIT-047
;; INTENT: NS-SPLIT-047
(defn external-profiles!
  "Read operator-owned profile configuration without touching the workspace."
  [root filename]
  (try
    (let [file (io/file filename)
          path (.toPath file)
          real (.toRealPath path (make-array LinkOption 0))]
      (when-not (and (.isAbsolute file) (not (.startsWith real root))
                     (Files/isRegularFile real (make-array LinkOption 0))
                     (<= (Files/size real) max-profile-bytes))
        (refuse! :invalid-profile-file "Profile file must be an absolute external regular EDN file of at most 1 MiB" {}))
      ;; Bound the actual read as well as the stat, in case the file grows.
      (let [bytes (with-open [in (Files/newInputStream real (make-array java.nio.file.OpenOption 0))]
                    (.readNBytes in (inc max-profile-bytes)))
            _ (when (> (alength bytes) max-profile-bytes)
                (refuse! :invalid-profile-file "Profile file exceeds 1 MiB" {}))
            config (edn/read-string (String. bytes java.nio.charset.StandardCharsets/UTF_8))]
        (when-not (and (map? config) (map? (:verification-profiles config))
                       (every? string? (keys (:verification-profiles config))))
          (refuse! :invalid-profile-file "Profile file must contain :verification-profiles with string names" {}))
        (:verification-profiles config)))
    (catch Exception e
      (refuse! :invalid-profile-file "Cannot read external verification profile file"
               {:profile_file filename :reason (.getMessage e)}))))

;; @spec NS-SPLIT-048
;; INTENT: NS-SPLIT-048
(defn proof-completion
  "Completion needs executed substantive cold evidence, not merely cold mode.
  Operator-configured commands are trusted; true itself proves no cold gate."
  [capability verification]
  (let [warm? (= :warm (:proof capability))
        substantive? (some (fn [check]
                             (and (:finished? check) (= 0 (:exit check))
                                  (seq (:command check))
                                  (not= "true" (.getName (io/file (first (:command check)))))))
                           (:process_evidence verification))
        pending (if warm?
                  (mapv #(str/join " " %) (:pending-commands capability))
                  (if (and (:ok verification) substantive?) [] ["cold-suite"]))
        pending (if (and warm? (empty? pending)) ["cold-suite"] pending)]
    {:verification_complete (and (not warm?) (true? (:ok verification)) (empty? pending))
     :proof_pending pending}))

(defn- result-snapshot-current? [root compiled]
  (try
    (= (into (sorted-map) (remove (comp nil? val))
             (merge (:guard-sources compiled) (:future-sources compiled)))
       ;; This is a verification guard, not another compiler snapshot or analysis.
       ;; Comparing the inventory also detects a new caller created during proof.
       (capture! root (get-in compiled [:projection :coverage :roots])))
    (catch Exception _ false)))

(defn- request-key [request]
  ;; Read flags, verification transport and reviewed-snapshot guards do not
  ;; change the operation identity. Mapping and policy changes do.
  (lens/source-hash (pr-str (walk/postwalk #(if (map? %) (into (sorted-map) %) %)
                              (select-keys request [:workspace_root :source :destinations
                                                    :promotion_policy :source_retirement
                                                    :roots :constraints :expect])))))

(defn- index-path [root request]
  (artifacts/target "namespace-split" (str root) (str (request-key request) "-committed.edn")))

(defn- source-index-path [root request]
  (artifacts/target "namespace-split" (str root)
                    (str (lens/source-hash (get-in request [:source :file])) "-source-committed.edn")))

(defn- persist-index! [root request result]
  (let [entry {:receipt_path (:receipt_path result) :request_key (request-key request)
               :destinations (mapv :file (:destinations request))
               :original_hash (lens/source-hash (gate/read-text! (:receipt_path result) gate/max-receipt-bytes))}]
    (doseq [path [(index-path root request) (source-index-path root request)]]
      (file-ops/atomic-write! path (pr-str entry))))
  result)

;; @spec NS-SPLIT-053
;; INTENT: NS-SPLIT-053
(defn committed-facts!
  "Resolve a known committed intent without re-compiling a deleted source."
  [root request]
  (let [exact (index-path root request)
        fallback (source-index-path root request)
        entry (cond (.isFile (io/file exact)) (gate/read-edn! exact gate/max-receipt-bytes)
                    (.isFile (io/file fallback))
                    (let [entry (gate/read-edn! fallback gate/max-receipt-bytes)]
                      (when (or (not (.isFile (io/file (str root) (get-in request [:source :file]))))
                                (some (set (:destinations entry)) (map :file (:destinations request)))) entry)))]
    (when entry
      (let [receipt (gate/read-edn! (:receipt_path entry) gate/max-receipt-bytes)
            sources (try (capture! root (:roots request)) (catch Exception _ nil))
            current (when sources (gate/snapshot-hash sources))
            snapshot (when sources (split/snapshot-hash sources))
            same-request? (or (= (:request_key entry) (request-key request))
                              (and (nil? (:request_key entry)) (.isFile (io/file exact))))
            same? (and same-request?
                       (= (:original_hash entry) (lens/source-hash (gate/read-text! (:receipt_path entry) gate/max-receipt-bytes)))
                       (= (:candidate_hash receipt) current)
                       (or (nil? (:snapshot_hash request))
                           (contains? (set [(:snapshot_hash receipt) current snapshot]) (:snapshot_hash request))))]
        (if same?
          (assoc (select-keys receipt [:receipt_id :receipt_path :closure_receipt :candidate_hash
                                       :facts :counts :map_hash :verification_complete :proof_pending :checks])
                 :ok true :state "committed-facts" :operation "namespace_split"
                 :snapshot_hash snapshot :input_snapshot_hash (:snapshot_hash receipt)
                 :facts_basis "committed-transaction" :verification_basis "original-receipt"
                 :read_complete true :source_unchanged true :committed true :mutation_attempted false)
          {:ok false :state "refused" :operation "namespace_split"
           :error_type (if same-request? "committed-facts-stale" "committed-facts-request-mismatch")
           :error (if same-request?
                    "Committed facts no longer match this source inventory; inspect the named closure receipt."
                    "This request differs from the committed split; inspect the named original and closure receipts.")
           :closure_receipt (:closure_receipt receipt) :receipt_path (:receipt_path receipt)
           :verification_complete false :mutation_attempted false :source_unchanged true})))))

;; @spec NS-SPLIT-010
;; @spec NS-SPLIT-012
;; INTENT: NS-SPLIT-021
(defn publish!
  "Shared kernel publication and shared synchronous profile; failures use its inverse."
  [root compiled profile-name capability receipt-dir checks]
  (when-not (= (:guard-sources compiled) (capture! root (get-in compiled [:projection :coverage :roots])))
    (refuse! :snapshot-drift "The captured source inventory changed before publication" {}))
  (let [base (split/publication-receipt compiled checks)
        base-size (gate/receipt-size base)
        _ (when (> base-size (- gate/max-receipt-bytes 16384))
            (refuse! :receipt-size-bound "Split review facts exceed the receipt budget" {:receipt_bytes base-size}))
        candidate (canonical-compiled root compiled)
        committed (kernel/commit! candidate)
        retired? #(boolean (and (seq (:deleted-files candidate))
                                (every? (fn [file] (not (.exists (io/file file)))) (:deleted-files candidate))))]
    (if-not (:ok committed)
      (merge base {:ok false :state (cond (:rolled-back committed) "rolled-back"
                                      (:recovery committed) "recovery-required" :else "refused")
                   :error_type (some-> (:error-type committed) name) :error (:error committed)
                   :mutation_attempted (boolean (:recovery committed)) :source_retired (retired?) :kernel committed})
      (let [id (str (UUID/randomUUID))]
        (try
          (let [inverse (save! receipt-dir (str id "-undo.edn") (:receipt committed))
                warm-check (when-let [live (:warm-live capability)]
                             (warm/probe! live (:warm-selection compiled)))
                probe-only? (= :warm (:proof capability))
                verification (cond
                               (and warm-check (not (:ok warm-check))) {:ok false}
                               probe-only? {:ok (true? (:ok warm-check))}
                               :else (proof/run-proof! (str root) profile-name capability))
                guard-start (System/nanoTime)
                snapshot-current? (result-snapshot-current? root compiled)
                checks (cond-> checks warm-check (conj warm-check))
                checks (into checks (map (partial proof-check profile-name) (:process_evidence verification)))
                checks (conj checks {:name "verified-snapshot-guard" :exit (if snapshot-current? 0 1)
                                     :duration_ms (/ (double (- (System/nanoTime) guard-start)) 1000000.0)
                                     :status (if snapshot-current? "passed" "failed")})
                details (save! receipt-dir (str id "-details.edn")
                               {:projection (:projection compiled) :verification verification
                                :read_back (:verified committed)})]
            (if (and (:ok verification) snapshot-current?)
              (let [result (assoc (merge base (artifacts/workspace-evidence (str root) (concat (keys (:future-sources compiled)) (:deleted-files compiled)))) :ok true :committed true :mutation_attempted true
                             :source_retired (boolean (seq (:deleted-files compiled)))
                             :verification_complete (:verification_complete (proof-completion capability verification))
                             :state (if probe-only? "committed-probe-only" "committed")
                             :proof_pending (:proof_pending (proof-completion capability verification))
                             :checks checks :undo_receipt inverse :details_path details
                             :undo_command ["clj-surgeon" ":op" ":undo-extract!" ":receipt" inverse]
                             :receipt_hash (:receipt-hash committed))
                    result (assoc-in result [:facts :unexpected_paths]
                                     (mapv operation/encode-caller-text (get-in result [:workspace_status :unexpected_paths])))
                    result (if (nil? (get-in result [:workspace_status :unexpected_paths]))
                             (assoc-in result [:facts :unexpected_paths] nil) result)
                    result (assoc result :receipt_id id :workspace_root (str root)
                                  :receipt_path (str (io/file receipt-dir (str id "-receipt.edn")))
                                  :closure_receipt (str (io/file receipt-dir (str id "-closure.edn")))
                                  :candidate_hash (gate/snapshot-hash
                                                    (into (sorted-map) (remove (comp nil? val))
                                                          (merge (:guard-sources compiled) (:future-sources compiled)))))]
                (persist-index! root (:request compiled)
                  (if (= :background (:gate capability))
                    (gate/launch! result receipt-dir (assoc capability :profile profile-name))
                    (do (save! receipt-dir (str id "-receipt.edn") (gate/bounded-receipt! result)) result))))
              (let [undo (kernel/undo! (:receipt committed))]
                (assoc base :ok false :state (if (:ok undo) "rolled-back" "recovery-required")
                       :error (if snapshot-current? "Required verification failed" "The verified snapshot changed during proof")
                       :error_type (if snapshot-current? "verification-failed" "snapshot-drift")
                       :mutation_attempted true :restored (:ok undo) :checks checks
                       :source_retired (retired?)
                       :undo_receipt inverse :details_path details :recovery undo))))
          (catch Throwable error
            (let [undo (kernel/undo! (:receipt committed))]
              (assoc base :ok false :state (if (:ok undo) "rolled-back" "recovery-required")
                     :error (.getMessage error) :error_type "publication-failed"
                     :source_retired (retired?)
                     :mutation_attempted true :restored (:ok undo) :recovery undo))))))))

;; @spec NS-SPLIT-011
;; @spec NS-SPLIT-012
;; @spec NS-SPLIT-054
;; @spec NS-SPLIT-058
;; INTENT: NS-SPLIT-054
;; INTENT: NS-SPLIT-058
(defn execute!
  ([request] (execute! {} request))
  ([config request]
   (let [started (System/nanoTime)
         elapsed #(/ (double (- (System/nanoTime) started)) 1000000.0)]
     (try
       (let [request (walk/keywordize-keys request)
             invalid (validate-request request)]
         (when (seq invalid) (refuse! :invalid-request "Invalid namespace_split request" {:blockers invalid}))
         (let [root (paths/real-root (:workspace_root request))
               request (assoc request :workspace_root (str root))
               committed-facts (when (= "facts" (:plan_only request)) (committed-facts! root request))]
           (if committed-facts
             (assoc committed-facts :elapsed_ms (elapsed))
             (let [config-file (io/file (str root) ".clj-surgeon.edn")
                   raw-profiles (if-let [file (get-in request [:verification :profile-file])]
                                  (external-profiles! root file)
                                  (or (:verification-profiles config)
                                      (when (.isFile config-file)
                                        (:verification-profiles (edn/read-string (slurp config-file))))))
                   profiles (anchored-profiles root raw-profiles)
                   profile-name (get-in request [:verification :profile])
                   mode (get-in profiles [profile-name :proof] :cold)
                   gate-mode (get-in profiles [profile-name :gate])
                   _ (when (and gate-mode (not (and (= :background gate-mode) (= :warm mode))))
                       (refuse! :invalid-gate-mode "Background gate requires :proof :warm and :gate :background" {}))
                   _ (when-not (#{:cold :warm} mode)
                       (refuse! :invalid-proof-mode "Profile :proof must be :cold or :warm" {}))
                   live (when-not (:plan_only request) (warm/discover! root))
                   _ (when (and (not (:plan_only request)) (= :warm mode) (nil? live))
                       (refuse! :warm-probe-unavailable "Warm proof requires a live workspace nREPL" {}))
                   runner (when (and (= :background gate-mode) (not (:plan_only request)))
                            (gate/runner-commands!))
                   preflight (when-not (:plan_only request) (proof/verification-preflight profiles profile-name true))]
               (when preflight
                 (refuse! (if (= "helper-extraction-verification-empty-profile" (:error_type preflight))
                            :verification-empty-profile :verification-unavailable)
                          (:error preflight) (cond-> {:preflight preflight}
                                               (:proof_pending preflight) (assoc :proof_pending (:proof_pending preflight)))))
               (let [sources (capture! root (:roots request))
                     _ (doseq [d (:destinations request)]
                         (when-not (some #(.startsWith (.normalize (.toPath (io/file (:file d))))
                                                       (.toPath (io/file %))) (:roots request))
                           (refuse! :destination-outside-roots "Destination must be inside an authorized root" {:file (:file d)})))
                     _ (when-not (contains? sources (get-in request [:source :file]))
                         (refuse! :missing-source "The source file is not within the captured roots" {:file (get-in request [:source :file])}))
                     analyzed (analyze! sources)
                     compiled (split/compile-split request {:sources sources :analysis (:analysis analyzed)
                                                            :source-paths (extract/workspace-source-paths root)})
                     expected-errors (for [[k v] (:expect request) :when (not= v (get-in compiled [:projection :counts k]))]
                                       {:type :expect-mismatch :field k :expected v :actual (get-in compiled [:projection :counts k])})
                     compiled (if (seq expected-errors) (-> compiled (assoc :ok false) (update :blockers into expected-errors)) compiled)
                     checks [(:check analyzed)]
                     result (cond
                              (:plan_only request) (cond-> (assoc (split/receipt compiled checks)
                                                             :read_complete true :source_unchanged true)
                                                     (= "facts" (:plan_only request)) (assoc :facts (get-in compiled [:projection :facts])
                                                                                        :manifest (assoc request :plan_only true
                                                                                                         :snapshot_hash (get-in compiled [:projection :snapshot_hash])))
                                                     (not= "facts" (:plan_only request)) (assoc :analysis (split/analysis-projection compiled))
                                                     (not (:ok compiled)) (assoc :error "Split analysis contains blockers" :error_type "split-refused"))
                              (not (:ok compiled)) (assoc (split/receipt compiled checks) :error "Split decisions or static proof are incomplete"
                                                     :error_type "split-refused" :source_unchanged true
                                                     :next_call (assoc request :plan_only true))
                              :else
                              (let [parse-start (System/nanoTime)
                                    _ (doseq [[_ s] (:future-sources compiled) :when s] (parser/parse-string-all s))
                                    parse-ms (/ (double (- (System/nanoTime) parse-start)) 1000000.0)
                                    candidate-sources (into (sorted-map) (remove (comp nil? val))
                                                            (merge sources (:future-sources compiled)))
                                    candidate-analysis (analyze! candidate-sources)
                                    delta (lint-comparison analyzed candidate-analysis)
                                    checks (conj checks {:name "future-source-parse" :exit 0 :duration_ms parse-ms :status "passed"} delta)]
                                (if (= "failed" (:status delta))
                                  (assoc (split/receipt compiled checks) :ok false :state "refused"
                                         :error "Candidate introduces error findings relative to baseline lint"
                                         :error_type "lint-regression" :source_unchanged true
                                         :blockers [{:type :lint-regression :introduced_errors (:introduced_errors delta)}])
                                  (publish! root compiled profile-name
                                            (assoc (proof/profile-capability (get profiles profile-name))
                                                   :proof mode :gate gate-mode :runner runner :warm-live live
                                                   :pending-commands (:commands (proof/profile-capability (get profiles profile-name))))
                                            (artifacts/directory "namespace-split" (str root))
                                            checks))))]
                 (assoc result :elapsed_ms (elapsed)))))))
       ;; @spec NS-SPLIT-059
       ;; INTENT: NS-SPLIT-059
       ;; A typed refusal that names no repair is a dead end: preserve the
       ;; refusing boundary's own next_call all the way to the public receipt.
       (catch Throwable error
         {:ok false :operation "namespace_split" :state "refused" :committed false
          :mutation_attempted false :source_unchanged true :verification_complete false
          :error_type (name (or (:error-type (ex-data error)) :split-failed))
          :error (.getMessage error) :evidence (dissoc (ex-data error) :error-type :next_call)
          :next_call (:next_call (ex-data error))
          :proof_pending (or (:proof_pending (ex-data error)) []) :elapsed_ms (elapsed)})))))

;; @spec NS-SPLIT-014
(defn cli! [opts]
  ;; Babashka does not read JAVA_TOOL_OPTIONS. Its process-owned CLI entrance
  ;; explicitly adopts TMPDIR before any shared subprocess runner allocates.
  (when (System/getProperty "babashka.version")
    (when-let [tmpdir (System/getenv "TMPDIR")]
      (System/setProperty "java.io.tmpdir" tmpdir)))
  (let [request (or (:request opts)
                    (when-let [file (:request-file opts)] (edn/read-string (slurp file)))
                    (dissoc opts :op :plan-only :facts-only :profile-file))
        request (cond-> request (:plan-only opts) (assoc :plan_only true)
                  (:facts-only opts) (assoc :plan_only "facts")
                  (:profile-file opts) (assoc-in [:verification :profile-file] (:profile-file opts)))]
    ;; EDN remains machine-readable while its first field answers what happened.
    (into (sorted-map-by (fn [a b] (compare [(if (= :state a) 0 1) (name a)]
                                     [(if (= :state b) 0 1) (name b)])))
          (execute! request))))
