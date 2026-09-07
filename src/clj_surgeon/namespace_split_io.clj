(ns clj-surgeon.namespace-split-io
  "Confined snapshot, baseline-relative lint, shared extraction publish/proof/inverse."
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-paths :as paths]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.namespace-split :as split]
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
(def max-file-bytes (* 2 1024 1024))
(def string-schema {:type "string" :minLength 1})
(def strings-schema {:type "array" :items string-schema :uniqueItems true})
(defn object-schema [properties required]
  {:type "object" :additionalProperties false :properties properties :required required})

;; @spec NS-SPLIT-013
(def schema
  (object-schema
    {"workspace_root" string-schema
     "source" (object-schema {"file" string-schema "lib" string-schema} ["file" "lib"])
     "destinations" {:type "array" :minItems 1 :maxItems 1000
                     :items (object-schema {"lib" string-schema "file" string-schema
                                            "forms" strings-schema
                                            "alias_policy" (assoc strings-schema :minItems 1)}
                                           ["lib" "file" "forms" "alias_policy"])}
     "promotion_policy" {:oneOf [{:type "string" :enum ["promote-required"]} strings-schema]}
     "source_retirement" {:type "string" :enum ["delete" "retain-empty"]}
     "roots" (assoc strings-schema :minItems 1 :maxItems 32)
     "constraints" (object-schema {"forbidden_edges" {:type "array" :items {:type "array" :items string-schema :minItems 2 :maxItems 2}}} [])
     "verification" (object-schema {"profile" string-schema} ["profile"])
     "expect" (object-schema (into {} (for [k ["files" "forms" "destinations" "caller_files" "caller_sites"]]
                                        [k {:type "integer" :minimum 0}])) [])
     "snapshot_hash" string-schema
     "plan_only" {:type "boolean"}}
    ["workspace_root" "source" "destinations" "promotion_policy" "source_retirement" "roots" "verification"]))

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

(defn validate-request [request]
  (let [errors (schema-errors schema request [])
        bad-aliases (for [d (:destinations request) a (:alias_policy d)
                          :when (not (and (string? a) (re-matches #"[A-Za-z][A-Za-z0-9_-]*" a)))]
                      {:path ["destinations" (:lib d) "alias_policy"] :reason :invalid-alias})
        bad-libs (for [lib (cons (get-in request [:source :lib]) (map :lib (:destinations request)))
                       :when (not (and (string? lib) (re-matches #"[A-Za-z_][A-Za-z0-9_!?*+\-]*(?:\.[A-Za-z_][A-Za-z0-9_!?*+\-]*)*" lib)))]
                   {:path ["lib"] :reason :invalid-library :lib lib})]
    (vec (concat errors bad-aliases bad-libs))))

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
                                                                          :namespace-usages true :java-class-usages true :keywords true}}})]
                      :cwd (str temp) :timeout-ms 120000 :visible-byte-limit max-bytes})
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

(defn- anchored-profiles [root profiles]
  (into {} (for [[profile spec] profiles]
             [profile (if-let [capability (proof/profile-capability spec)]
                        (cond-> {:commands (mapv (fn [argv]
                                                   (let [exe (first argv)]
                                                     (if (and (str/includes? exe "/") (not (.isAbsolute (io/file exe))))
                                                       (assoc argv 0 (str (.resolve root exe))) argv)))
                                             (:commands capability))}
                          (:timeout-ms capability) (assoc :timeout-ms (:timeout-ms capability)))
                        spec)])))

(defn- result-snapshot-current? [root compiled]
  (try
    (= (into (sorted-map) (remove (comp nil? val))
             (merge (:guard-sources compiled) (:future-sources compiled)))
       ;; This is a verification guard, not another compiler snapshot or analysis.
       ;; Comparing the inventory also detects a new caller created during proof.
       (capture! root (get-in compiled [:projection :coverage :roots])))
    (catch Exception _ false)))

;; @spec NS-SPLIT-010
;; @spec NS-SPLIT-012
;; INTENT: NS-SPLIT-021
(defn publish!
  "Shared kernel publication and shared synchronous profile; failures use its inverse."
  [root compiled profile-name capability receipt-dir checks]
  (when-not (= (:guard-sources compiled) (capture! root (get-in compiled [:projection :coverage :roots])))
    (refuse! :snapshot-drift "The captured source inventory changed before publication" {}))
  (let [candidate (canonical-compiled root compiled)
        committed (kernel/commit! candidate)
        base (split/receipt compiled checks)
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
                verification (proof/run-proof! (str root) profile-name capability)
                guard-start (System/nanoTime)
                snapshot-current? (result-snapshot-current? root compiled)
                checks (into checks (map (partial proof-check profile-name) (:process_evidence verification)))
                checks (conj checks {:name "verified-snapshot-guard" :exit (if snapshot-current? 0 1)
                                     :duration_ms (/ (double (- (System/nanoTime) guard-start)) 1000000.0)
                                     :status (if snapshot-current? "passed" "failed")})
                details (save! receipt-dir (str id "-details.edn")
                               {:projection (:projection compiled) :verification verification
                                :read_back (:verified committed)})]
            (if (and (:ok verification) snapshot-current?)
              (assoc base :ok true :state "committed" :committed true :mutation_attempted true
                     :source_retired (boolean (seq (:deleted-files compiled)))
                     :verification_complete true :checks checks :undo_receipt inverse :details_path details
                     :undo_command ["clj-surgeon" ":op" ":undo-extract!" ":receipt" inverse]
                     :receipt_hash (:receipt-hash committed)
                     :graph (-> (:graph base) (dissoc :edges) (assoc :edge_count (count (get-in base [:graph :edges])))))
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
               config-file (io/file (str root) ".clj-surgeon.edn")
               project-config (when (.isFile config-file) (edn/read-string (slurp config-file)))
               profiles (anchored-profiles root (or (:verification-profiles config) (:verification-profiles project-config)))
               profile-name (get-in request [:verification :profile])
               preflight (when-not (:plan_only request) (proof/verification-preflight profiles profile-name true))]
           (when preflight (refuse! :verification-unavailable "Verification profile cannot run synchronously" {:preflight preflight}))
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
                          (:plan_only request) (cond-> (assoc (split/receipt compiled checks) :analysis (split/analysis-projection compiled)
                                                         :read_complete true :source_unchanged true)
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
                              (publish! root compiled profile-name (proof/profile-capability (get profiles profile-name))
                                        (or (:receipt-dir config)
                                            (str (System/getProperty "java.io.tmpdir") "/namespace-split-receipts"))
                                        checks))))]
             (assoc result :elapsed_ms (elapsed)))))
       (catch Throwable error
         {:ok false :operation "namespace_split" :state "refused" :committed false
          :mutation_attempted false :source_unchanged true :verification_complete false
          :error_type (name (or (:error-type (ex-data error)) :split-failed))
          :error (.getMessage error) :evidence (dissoc (ex-data error) :error-type)
          :next_call nil :elapsed_ms (elapsed)})))))

;; @spec NS-SPLIT-014
(defn cli! [opts]
  ;; Babashka does not read JAVA_TOOL_OPTIONS. Its process-owned CLI entrance
  ;; explicitly adopts TMPDIR before any shared subprocess runner allocates.
  (when (System/getProperty "babashka.version")
    (when-let [tmpdir (System/getenv "TMPDIR")]
      (System/setProperty "java.io.tmpdir" tmpdir)))
  (let [request (or (:request opts)
                    (when-let [file (:request-file opts)] (edn/read-string (slurp file)))
                    (dissoc opts :op :plan-only))
        request (cond-> request (:plan-only opts) (assoc :plan_only true))]
    ;; EDN remains machine-readable while its first field answers what happened.
    (into (sorted-map-by (fn [a b] (compare [(if (= :state a) 0 1) (name a)]
                                     [(if (= :state b) 0 1) (name b)])))
          (execute! request))))
