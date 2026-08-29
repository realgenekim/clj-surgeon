(ns three-arm-request-shape-screen
  "Pure F/A/B request expanders and capture-only screen scorer.

  Every treatment lowers to the existing canonical edit request. This
  experiment has no write authority and does not define a second transaction
  compiler."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [namespace-tolerance-replay :as replay]
   [owner-aware-call-construction-screen :as owner-screen]
   [owner-aware-symbol-migration :as migration]
   [rewrite-clj.node :as node]
   [rewrite-clj.zip :as z]))

(def arms [:flat :file-groups :closed-relations])
(def cohort-order [:flat :file-groups :closed-relations
                   :closed-relations :file-groups :flat])

(defn- refusal [error-type error data]
  (merge {:ok false
          :error-type error-type
          :error error
          :source-unchanged true
          :write-authority false}
         data))

(defn json-bytes [value]
  (alength (.getBytes (json/generate-string value) "UTF-8")))

(defn- canonical [value]
  (cond
    (map? value) (into (sorted-map)
                       (map (fn [[key child]] [key (canonical child)]))
                       value)
    (sequential? value) (mapv canonical value)
    :else value))

(defn- row-key [row]
  (json/generate-string (canonical row)))

(defn- duplicate-row [rows]
  (some (fn [[row count]] (when (> count 1) row))
        (frequencies (map row-key rows))))

(defn- without-default-match [edit]
  (if (= 1 (get edit "matches")) (dissoc edit "matches") edit))

(defn- restore-default-match [edit]
  (if (contains? edit "matches") edit (assoc edit "matches" 1)))

(defn- public-json [value]
  (json/parse-string (json/generate-string value)))

(def local-edit-schema
  (let [ordinary (get-in mcp-schema/editor-tool-schema
                         [:properties "edits" :items])]
    (-> ordinary
        (update :properties dissoc "file" "files")
        ;; Preserve only the closed field-pair relation. File ownership is
        ;; supplied by the enclosing group, so the ordinary file/files oneOf
        ;; would make every local edit unsatisfiable.
        (assoc :allOf [(first (:allOf ordinary))]))))

(def file-groups-field-schema
  {:type "array"
   :minItems 1
   :maxItems 128
   :description
   (str "Group exact edits by file. Each local edit uses the ordinary edit "
        "fields but must omit file and files. Omitted matches means one.")
   :items
   {:type "object"
    :additionalProperties false
    :properties
    {"file" {:type "string" :minLength 1}
     "edits" {:type "array"
              :minItems 1
              :maxItems 128
              :items local-edit-schema}}
    :required ["file" "edits"]}})

(def require-change-field-schema
  {:type "object"
   :additionalProperties false
   :description
   (str "One exact require target plus every exact file and optional exact "
        "lib/alias removal. Frozen source supplies only repeated clause text.")
   :properties
   {"add" {:type "object"
           :additionalProperties false
           :properties {"lib" {:type "string" :minLength 1}
                        "as" {:type "string" :minLength 1}}
           :required ["lib" "as"]}
    "files" {:type "array"
             :minItems 1
             :maxItems 128
             :items
             {:type "object"
              :additionalProperties false
              :properties
              {"file" {:type "string" :minLength 1}
               "remove" {:type "object"
                         :additionalProperties false
                         :properties
                         {"lib" {:type "string" :minLength 1}
                          "as" {:type "string" :minLength 1}}
                         :required ["lib" "as"]}}
              :required ["file"]}}}
   :required ["add" "files"]})

(def file-groups-description
  (str mcp-tool/tool-description
       " When all exact edits are known and several share a file, use one "
       "file_groups array. State each file once and put its complete local "
       "edits below it. Local edits must omit file and files."))

(def closed-relations-description
  (str mcp-tool/tool-description
       owner-screen/candidate-description-suffix
       " For the associated namespace updates, use one require_change with "
       "the exact added lib/alias and every exact file; declare each removed "
       "lib/alias explicitly. Frozen source supplies clause text only."))

(defn tool-surface [arm]
  (case arm
    :flat
    {:name "edit_clojure"
     :description mcp-tool/tool-description
     :schema mcp-schema/editor-tool-schema}

    :file-groups
    {:name "edit_clojure"
     :description file-groups-description
     :schema (-> mcp-schema/editor-tool-schema
                 (assoc-in [:properties "file_groups"]
                           file-groups-field-schema)
                 (update :anyOf conj {:required ["file_groups"]}))}

    :closed-relations
    {:name "edit_clojure"
     :description closed-relations-description
     :schema (-> mcp-schema/editor-tool-schema
                 (assoc-in [:properties owner-screen/candidate-field-name]
                           owner-screen/candidate-field-schema)
                 (assoc-in [:properties "require_change"]
                           require-change-field-schema)
                 (update :anyOf conj
                         {:required ["symbol_migration" "require_change"]}))}

    (throw (ex-info "Unknown three-arm screen arm" {:arm arm}))))

(defn flat-request []
  (dissoc migration/oracle-request "workspace_root"))

(defn file-groups-request []
  (let [request (flat-request)
        edits (get request "edits")
        files (distinct (map #(get % "file") edits))]
    {"file_groups"
     (mapv (fn [file]
             {"file" file
              "edits" (->> edits
                           (filter #(= file (get % "file")))
                           (mapv #(-> %
                                      (dissoc "file")
                                      without-default-match)))})
           files)
     "delete_owners" (get request "delete_owners")}))

(defn- require-entry-facts [clause]
  (mapv (fn [[_ lib alias]] {"lib" lib "as" alias})
        (re-seq #"\[([^\s\]]+)\s+:as\s+([^\s\]]+)\]" clause)))

(def require-target
  {"lib" "sample.views.submission-row" "as" "submission-row"})

(defn- require-removal [edit]
  (let [before (set (require-entry-facts (get edit "from")))
        after (set (require-entry-facts (get edit "to")))
        added (set/difference after before)
        removed (set/difference before after)]
    (assert (= #{require-target} added))
    (assert (<= (count removed) 1))
    (cond-> {"file" (get edit "file")}
      (seq removed) (assoc "remove" (first removed)))))

(defn closed-relations-request []
  {"require_change"
   {"add" require-target
    "files" (mapv require-removal migration/namespace-edits)}
   "symbol_migration" migration/symbol-migration-table
   "edits" [(without-default-match migration/bespoke-edit)]
   "delete_owners" [migration/moved-owner-deletion]})

(defn- expand-file-groups [request]
  (let [groups (get request "file_groups")
        flat-edits (get request "edits")]
    (cond
      (not (and (vector? groups) (seq groups)))
      (refusal :invalid-file-groups
               "file_groups must be a non-empty array"
               {})

      (seq flat-edits)
      (refusal :ambiguous-edit-shape
               "file_groups and flat edits cannot both be populated"
               {})

      :else
      (let [bad-group
            (some (fn [group]
                    (when-not (and (map? group)
                                   (string? (get group "file"))
                                   (seq (get group "file"))
                                   (vector? (get group "edits"))
                                   (seq (get group "edits")))
                      group))
                  groups)
            files (mapv #(get % "file") groups)
            local-with-file
            (some (fn [edit]
                    (when (or (contains? edit "file")
                              (contains? edit "files"))
                      edit))
                  (mapcat #(get % "edits") groups))]
        (cond
          bad-group
          (refusal :invalid-file-group
                   "Every file group needs one file and non-empty edits"
                   {:group bad-group})

          (not= (count files) (count (distinct files)))
          (refusal :duplicate-file-group
                   "Each grouped file must appear exactly once"
                   {:files files})

          local-with-file
          (refusal :ambiguous-local-file
                   "A local grouped edit cannot contain file or files"
                   {:edit local-with-file})

          :else
          (let [edits
                (mapv (fn [[file edit]]
                        (-> edit
                            (assoc "file" file)
                            restore-default-match))
                      (mapcat (fn [group]
                                (map #(vector (get group "file") %)
                                     (get group "edits")))
                              groups))]
            (if-let [duplicate (duplicate-row edits)]
              (refusal :duplicate-expanded-edit
                       "Two grouped rows lower to the same canonical edit"
                       {:row duplicate})
              {:ok true
               :request (-> request
                            (dissoc "file_groups")
                            (assoc "edits" edits))
               :derived {:file-occurrences (- (count edits) (count groups))
                         :default-matches
                         (count (remove #(contains? % "matches")
                                        (mapcat #(get % "edits") groups)))}})))))))

(defn- meaningful-child? [loc]
  (not (contains? #{:whitespace :newline :comma}
                  (node/tag (z/node loc)))))

(defn- node-contains-tag? [candidate tag]
  (boolean (some #(= tag (node/tag %))
                 (tree-seq node/inner? node/children candidate))))

(defn- direct-list-children [loc]
  (->> (some-> loc z/down)
       (iterate z/right)
       (take-while some?)
       (filter meaningful-child?)
       vec))

(defn- top-level-ns-locations [source]
  (->> (z/of-string source)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= "ns" (some-> % z/down z/string))))
       vec))

(defn- require-locations [ns-loc]
  (->> (direct-list-children ns-loc)
       (filter #(and (z/list? %)
                     (= ":require" (some-> % z/down z/string))))
       vec))

(defn- libspec-fact [loc]
  (let [entry (z/sexpr loc)
        entry (when (vector? entry) entry)
        as-index (when entry (.indexOf entry :as))]
    (when (and entry
               (symbol? (first entry))
               (integer? as-index)
               (pos? as-index)
               (< as-index (dec (count entry))))
      {:lib (str (first entry))
       :as (str (nth entry (inc as-index)))
       :source (z/string loc)})))

(defn- compile-one-require-change [source add file-change]
  (let [ns-locs (top-level-ns-locations source)]
    (cond
      (not= 1 (count ns-locs))
      (refusal :ambiguous-namespace
               "Frozen source must contain exactly one namespace form"
               {:namespace-count (count ns-locs)})

      :else
      (let [require-locs (require-locations (first ns-locs))]
        (cond
          (not= 1 (count require-locs))
          (refusal :ambiguous-require-clause
                   "Namespace must contain exactly one direct require clause"
                   {:require-count (count require-locs)})

          (node-contains-tag? (z/node (first require-locs)) :comment)
          (refusal :comment-bearing-require-clause
                   "Require clause comments make exact delta placement ambiguous"
                   {})

          :else
          (let [require-loc (first require-locs)
                children (->> (some-> require-loc z/down z/right)
                              (iterate z/right)
                              (take-while some?)
                              (filter meaningful-child?)
                              vec)
                non-vector (some #(when-not (z/vector? %) %) children)
                facts (mapv libspec-fact children)
                invalid (some nil? facts)
                add-fact {:lib (get add "lib") :as (get add "as")}
                remove (get file-change "remove")
                remove-fact (when remove
                              {:lib (get remove "lib")
                               :as (get remove "as")})
                add-count (count (filter #(= add-fact (select-keys % [:lib :as]))
                                         facts))
                alias-collisions
                (filter #(and (= (:as %) (:as add-fact))
                              (not= (:lib %) (:lib add-fact)))
                        facts)
                remove-matches
                (filter #(= remove-fact (select-keys % [:lib :as])) facts)]
            (cond
              non-vector
              (refusal :platform-conditional-require
                       "Require entries must be direct vectors"
                       {:entry (z/string non-vector)})

              invalid
              (refusal :unsupported-require-entry
                       "Every require entry must have an exact lib and alias"
                       {})

              (pos? add-count)
              (refusal :require-target-already-present
                       "Declared require target is already present"
                       {:target add-fact})

              (seq alias-collisions)
              (refusal :require-alias-collision
                       "Declared target alias is already bound"
                       {:target add-fact})

              (and remove-fact (not= 1 (count remove-matches)))
              (refusal :require-removal-not-unique
                       "Declared require removal must resolve exactly once"
                       {:remove remove-fact
                        :matches (count remove-matches)})

              :else
              (let [from (z/string require-loc)
                    remove-source (:source (first remove-matches))
                    remove-line (when remove-source (str "\n   " remove-source))
                    without-remove
                    (if remove-line (str/replace-first from remove-line "") from)
                    add-line (str "\n   [" (:lib add-fact)
                                  " :as " (:as add-fact) "]")
                    to (str (subs without-remove 0 (dec (count without-remove)))
                            add-line ")")]
                {:ok true
                 :edit {"file" (get file-change "file")
                        "within" {"namespace" true}
                        "from" from
                        "to" to
                        "matches" 1}}))))))))

(defn- compile-require-change [sources request]
  (let [change (get request "require_change")
        add (get change "add")
        files (get change "files")
        paths (mapv #(get % "file") files)]
    (cond
      (not (and (map? change)
                (map? add)
                (string? (get add "lib"))
                (seq (get add "lib"))
                (string? (get add "as"))
                (seq (get add "as"))
                (vector? files)
                (seq files)))
      (refusal :invalid-require-change
               "require_change needs one add target and non-empty files"
               {})

      (not= (count paths) (count (distinct paths)))
      (refusal :duplicate-require-file
               "Each require_change file must appear exactly once"
               {:files paths})

      (some #(not (string? (get sources %))) paths)
      (refusal :missing-frozen-source
               "Every require_change file must exist in the frozen source map"
               {:missing (vec (remove #(string? (get sources %)) paths))})

      :else
      (let [compiled
            (mapv #(compile-one-require-change
                     (get sources (get % "file")) add %)
                  files)
            failed (some #(when-not (:ok %) %) compiled)]
        (if failed
          failed
          {:ok true :edits (mapv :edit compiled)})))))

(defn- expand-closed-relations [sources request]
  (let [require-result (compile-require-change sources request)]
    (if-not (:ok require-result)
      require-result
      (let [relation-request (dissoc request "require_change")
            migration-result (migration/compile-manifest relation-request)]
        (if-not (:ok migration-result)
          migration-result
          (let [generated (get-in migration-result [:request "edits"])
                edits (into (:edits require-result) generated)]
            (if-let [duplicate (duplicate-row edits)]
              (refusal :duplicate-expanded-edit
                       "Two closed relations lower to the same canonical edit"
                       {:row duplicate})
              {:ok true
               :request (assoc (:request migration-result) "edits" edits)
               :owner-row-count (:owner-row-count migration-result)
               :declared-match-count (:declared-match-count migration-result)
               :derived {:require-clause-strings (count (:edits require-result))
                         :symbol-target-strings
                         (:owner-row-count migration-result)
                         :default-matches
                         (count (filter #(= 1 (get % "matches")) edits))}})))))))

(defn expand-request [sources arm arguments]
  (let [request (public-json arguments)]
    (case arm
      :flat {:ok true :request request :derived {}}
      :file-groups (expand-file-groups request)
      :closed-relations (expand-closed-relations sources request)
      (throw (ex-info "Unknown three-arm screen arm" {:arm arm})))))

(defn- symbol-site [edit]
  [(get edit "file")
   (get-in edit ["within" "form"])
   (get edit "from")
   (get edit "matches")])

(defn- symbol-value-name [value]
  (last (str/split value #"/")))

(defn- symbol-rewrite-edit? [edit]
  (and (string? (get-in edit ["within" "form"]))
       (string? (get edit "from"))
       (string? (get edit "to"))
       (re-matches #"[^\s\[\](){}]+" (get edit "from"))
       (re-matches #"[^\s\[\](){}]+" (get edit "to"))
       (= (symbol-value-name (get edit "from"))
          (symbol-value-name (get edit "to")))))

(defn- require-decisions [namespace-edits]
  (let [removals
        (mapcat (fn [edit]
                  (let [before (set (require-entry-facts (get edit "from")))
                        after (set (require-entry-facts (get edit "to")))]
                    (map #(vector (get edit "file")
                                  (get % "lib") (get % "as"))
                         (set/difference before after))))
                namespace-edits)]
    {:add [(get require-target "lib") (get require-target "as")]
     :files (vec (sort (map #(get % "file") namespace-edits)))
     :removals (vec (sort removals))}))

(def expected-decision-coverage
  {:files
   (vec (sort (distinct
                (concat (map #(get % "file")
                             (get migration/oracle-request "edits"))
                        (map #(get % "file")
                             (get migration/oracle-request "delete_owners"))))))
   :symbol-sites (mapv symbol-site migration/oracle-rewrite-edits)
   :non-default-counts
   (vec (keep (fn [edit]
                (when (not= 1 (get edit "matches"))
                  (symbol-site edit)))
              migration/oracle-rewrite-edits))
   :require (require-decisions migration/namespace-edits)
   :bespoke (row-key migration/bespoke-edit)
   :deleted
   (vec (sort (for [group (get migration/oracle-request "delete_owners")
                    owner (get group "forms")]
                [(get group "file") owner])))})

(defn- coverage-from-flat-request [request]
  (let [edits (mapv restore-default-match (get request "edits"))
        namespace-edits
        (filterv #(= true (get-in % ["within" "namespace"])) edits)
        symbol-edits (filterv symbol-rewrite-edit? edits)
        bespoke-edits
        (remove (set (concat namespace-edits symbol-edits)) edits)]
    {:files
     (vec (sort (distinct
                  (concat (map #(get % "file") edits)
                          (map #(get % "file")
                               (get request "delete_owners"))))))
     :symbol-sites (mapv symbol-site symbol-edits)
     :non-default-counts
     (vec (filter #(not= 1 (last %)) (map symbol-site symbol-edits)))
     :require (require-decisions namespace-edits)
     :bespoke (when (= 1 (count bespoke-edits))
                (row-key (first bespoke-edits)))
     :deleted
     (vec (sort (for [group (get request "delete_owners")
                      owner (get group "forms")]
                  [(get group "file") owner])))}))

(defn- explicit-symbol-sites [migration-table]
  (vec
    (for [[file sites] (get migration-table "files")
          [owner from matches] sites]
      [file owner from matches])))

(defn decision-coverage [arm arguments expanded]
  (let [arguments (public-json arguments)
        explicit
        (case arm
          :flat (coverage-from-flat-request (:request expanded))
          :file-groups (coverage-from-flat-request (:request expanded))
          :closed-relations
          (let [require-change (get arguments "require_change")]
            {:files
             (vec
               (sort
                 (distinct
                   (concat
                     (map #(get % "file") (get require-change "files"))
                     (map first
                          (get-in arguments ["symbol_migration" "files"]))
                     (map #(get % "file") (get arguments "edits"))
                     (map #(get % "file") (get arguments "delete_owners"))))))
             :symbol-sites
             (explicit-symbol-sites (get arguments "symbol_migration"))
             :non-default-counts
             (vec (filter #(not= 1 (last %))
                          (explicit-symbol-sites
                            (get arguments "symbol_migration"))))
             :require
             {:add [(get-in require-change ["add" "lib"])
                    (get-in require-change ["add" "as"])]
              :files (vec (sort (map #(get % "file")
                                     (get require-change "files"))))
              :removals
              (vec (sort (keep (fn [file-change]
                                 (when-let [remove (get file-change "remove")]
                                   [(get file-change "file")
                                    (get remove "lib")
                                    (get remove "as")]))
                               (get require-change "files"))))}
             :bespoke (some-> (first (get arguments "edits"))
                              restore-default-match
                              row-key)
             :deleted
             (vec (sort (for [group (get arguments "delete_owners")
                              owner (get group "forms")]
                          [(get group "file") owner])))}))]
    {:complete (= expected-decision-coverage explicit)
     :expected expected-decision-coverage
     :explicit explicit
     :derived (:derived expanded)
     :opaque-plan-ids 0
     :prior-results 0
     :model-turns 1}))

(defn treatment-adherent? [arm arguments]
  (let [arguments (public-json arguments)]
    (case arm
      :flat
      (and (= 33 (count (get arguments "edits")))
           (not (contains? arguments "file_groups"))
           (not (contains? arguments "require_change"))
           (not (contains? arguments "symbol_migration")))

      :file-groups
      (and (= 9 (count (get arguments "file_groups")))
           (= 33 (reduce + (map #(count (get % "edits"))
                                (get arguments "file_groups"))))
           (not (seq (get arguments "edits"))))

      :closed-relations
      (let [require-change (get arguments "require_change")
            migration-table (get arguments "symbol_migration")]
        (and (= require-target (get require-change "add"))
             (= 9 (count (get require-change "files")))
             (= 3 (count (filter #(get % "remove")
                                 (get require-change "files"))))
             (= 9 (count (get migration-table "files")))
             (= migration/symbol-migration-table migration-table)
             (= 1 (count (get arguments "edits")))
             (= 14 (count (get-in arguments ["delete_owners" 0 "forms"])))))

      false)))

(defn- exact-product? [product expected-after-hashes]
  (let [compiled (:compiled product)]
    (and (:ok product)
         (:ok compiled)
         (= 51 (:match-count compiled))
         (= 9 (:changed-file-count compiled))
         (= expected-after-hashes (migration/file-hashes compiled)))))

(defn- canonical-transaction [transaction]
  ;; Edit ids and independent row order are transport bookkeeping. The
  ;; authority-bearing change maps and aggregate expectation must be exact.
  {:changes (->> (:changes transaction)
                 (map #(dissoc % :id))
                 (sort-by pr-str)
                 vec)
   :expect (:expect transaction)})

(defn score-call [sources expected-after-hashes arm arguments geometry]
  (let [arguments (public-json arguments)
        expanded (expand-request sources arm arguments)
        product (when (:ok expanded)
                  (migration/compile-request sources (:request expanded)))
        oracle (migration/compile-request sources (flat-request))
        coverage (when (:ok expanded)
                   (decision-coverage arm arguments expanded))
        exact? (and (:ok expanded)
                    (exact-product? product expected-after-hashes))
        transaction-equal?
        (= (canonical-transaction (:transaction oracle))
           (canonical-transaction (:transaction product)))
        adherence? (treatment-adherent? arm arguments)
        one-action? (and (= 1 (:mcp-call-count geometry))
                         (zero? (:refusal-count geometry))
                         (zero? (:recovery-count geometry))
                         (zero? (:shell-call-count geometry))
                         (zero? (:file-change-count geometry)))
        payload-bytes (json-bytes arguments)
        payload-ok?
        (case arm
          :flat true
          :file-groups (<= payload-bytes (* 0.82 (json-bytes (flat-request))))
          :closed-relations
          (<= payload-bytes (* 0.60 (json-bytes (flat-request)))))]
    {:schema :clj-surgeon.three-arm-request-shape-run/v1
     :arm arm
     :correct (and exact? transaction-equal? adherence? one-action?
                   (:complete coverage) payload-ok?)
     :semantic-exact exact?
     :treatment-adherent adherence?
     :one-action one-action?
     :payload {:bytes payload-bytes :within-budget payload-ok?}
     :decision-coverage coverage
     :compiler {:canonical-transaction-equal transaction-equal?
                :future-hashes-equal exact?
                :match-count (get-in product [:compiled :match-count])
                :changed-file-count (get-in product [:compiled
                                                     :changed-file-count])
                :error-type (or (:error-type expanded)
                                (:error-type product)
                                (get-in product [:compiled :error-type]))}
     :geometry geometry}))

(defn- surface-metrics [arm]
  (let [surface (tool-surface arm)]
    {:description-bytes (alength (.getBytes (:description surface) "UTF-8"))
     :schema-bytes (json-bytes (:schema surface))
     :surface-bytes (json-bytes surface)}))

(defn- falsifier-result [result error-type]
  (and (false? (:ok result))
       (= error-type (:error-type result))
       (:source-unchanged result)
       (false? (:write-authority result))))

(defn falsifier-report []
  (let [{:keys [sources]} (migration/load-fixture)
        a (file-groups-request)
        b (closed-relations-request)
        first-group (get-in a ["file_groups" 0])
        first-change (get-in b ["require_change" "files" 0])
        alias-file (get first-change "file")
        permuted-b
        (update-in b ["symbol_migration" "files" 0 1]
                   #(vec (reverse %)))
        permuted-expansion (expand-closed-relations sources permuted-b)]
    {:a-mixed-flat
     (falsifier-result
       (expand-file-groups (assoc a "edits" [migration/bespoke-edit]))
       :ambiguous-edit-shape)
     :a-local-file
     (falsifier-result
       (expand-file-groups
         (assoc-in a ["file_groups" 0 "edits" 0 "file"] "src/other.clj"))
       :ambiguous-local-file)
     :a-duplicate-group
     (falsifier-result
       (expand-file-groups
         (update a "file_groups" conj first-group))
       :duplicate-file-group)
     :b-duplicate-require-file
     (falsifier-result
       (expand-closed-relations
         sources
         (update-in b ["require_change" "files"] conj first-change))
       :duplicate-require-file)
     :b-missing-source
     (falsifier-result
       (expand-closed-relations (dissoc sources alias-file) b)
       :missing-frozen-source)
     :b-platform-conditional
     (falsifier-result
       (expand-closed-relations
         (update sources alias-file
                 str/replace
                 "(:require"
                 "(:require #?(:clj [sample.conditional :as conditional])")
         b)
       :platform-conditional-require)
     :b-alias-collision
     (falsifier-result
       (expand-closed-relations
         (update sources alias-file
                 str/replace
                 "(:require"
                 "(:require [sample.other :as submission-row]")
         b)
       :require-alias-collision)
     :b-duplicate-symbol-row
     (let [site (get-in b ["symbol_migration" "files" 0 1 0])]
       (falsifier-result
         (expand-closed-relations
           sources
           (update-in b ["symbol_migration" "files" 0 1] conj site))
         :duplicate-expanded-edit))
     :b-row-permutation
     (and (:ok permuted-expansion)
          (false? (:complete
                    (decision-coverage :closed-relations
                                       permuted-b
                                       permuted-expansion)))
          (false? (treatment-adherent? :closed-relations permuted-b)))}))

(defn prerequisite-report []
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        requests {:flat (flat-request)
                  :file-groups (file-groups-request)
                  :closed-relations (closed-relations-request)}
        geometry {:mcp-call-count 1
                  :refusal-count 0
                  :recovery-count 0
                  :shell-call-count 0
                  :file-change-count 0
                  :prompt-to-call-ms 0.0}
        scores (into {}
                     (map (fn [[arm request]]
                            [arm (score-call sources expected-after-hashes
                                             arm request geometry)]))
                     requests)
        falsifiers (falsifier-report)]
    {:schema :clj-surgeon.three-arm-request-shape-prerequisites/v1
     :model-calls 0
     :mutation-actions 0
     :order cohort-order
     :arms
     (into {}
           (map (fn [[arm score]]
                  [arm {:correct (:correct score)
                        :semantic-exact (:semantic-exact score)
                        :treatment-adherent (:treatment-adherent score)
                        :decision-coverage-complete
                        (get-in score [:decision-coverage :complete])
                        :canonical-transaction-equal
                        (get-in score [:compiler :canonical-transaction-equal])
                        :argument-bytes (get-in score [:payload :bytes])
                        :surface (surface-metrics arm)}]))
           scores)
     :falsifiers falsifiers
     :all-prerequisites-green
     (and (= (set arms) (set (keys scores)))
          (every? :correct (vals scores))
          (every? true? (vals falsifiers)))}))

(defn cohort-report [runs]
  (let [by-arm (group-by :arm runs)
        arm-report
        (into {}
              (map (fn [arm]
                     [arm {:runs (count (get by-arm arm))
                           :correct (count (filter :correct (get by-arm arm)))
                           :treatment-adherent
                           (count (filter :treatment-adherent (get by-arm arm)))}]))
              arms)]
    {:schema :clj-surgeon.three-arm-request-shape-cohort/v1
     :arms arm-report
     :gate {:two-per-arm
            (every? #(= 2 (get-in arm-report [% :runs])) arms)
            :all-correct
            (every? #(= 2 (get-in arm-report [% :correct])) arms)
            :all-treatment-adherent
            (every? #(= 2 (get-in arm-report [% :treatment-adherent])) arms)}}))

(defn- parse-pairs [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {} (map (fn [[key value]] [(keyword (subs key 2)) value]))
        (partition 2 args)))

(defn- read-json [path]
  (json/parse-string (slurp (io/file path))))

(defn -main [& args]
  (case (first args)
    "score"
    (let [{:keys [arm capture timing mcp-calls shell-calls file-changes]}
          (parse-pairs (rest args))
          capture (read-json capture)
          timing (edn/read-string (slurp (io/file timing)))
          calls (get capture "calls")
          geometry {:mcp-call-count (parse-long mcp-calls)
                    :refusal-count 0
                    :recovery-count (max 0 (dec (parse-long mcp-calls)))
                    :shell-call-count (parse-long shell-calls)
                    :file-change-count (parse-long file-changes)
                    :prompt-to-call-ms
                    (owner-screen/prompt-to-first-call-ms timing)}
          {:keys [sources expected-after-hashes]} (migration/load-fixture)]
      (prn (score-call sources expected-after-hashes (keyword arm)
                       (get (first calls) "params") geometry)))

    "cohort"
    (let [runs (mapv #(edn/read-string (slurp (io/file %))) (rest args))]
      (prn (cohort-report runs)))

    "prerequisites"
    (let [result (prerequisite-report)]
      (prn result)
      (when-not (:all-prerequisites-green result)
        (System/exit 1)))

    (throw (ex-info "Usage: score|cohort|prerequisites" {:args args}))))
