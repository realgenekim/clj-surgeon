(ns clj-surgeon.mcp-compact-relations
  "Pure lowering for the closed compact symbol_migration + require_change pair."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

;; @spec MCP-OP-EDIT-037
(def relation-schema
  "The single source of truth for the closed compact relation shape.

  Both admission (`validate-migration!`, `validate-require!`,
  `validate-lib-alias!`, and the request-field checks in
  `compile-source-blind`) and the published `expected_shape` skeleton read
  their allowed field names, required field names, column vector, and
  `target_rule` constant from this one value, so a refusal can never teach a
  shape the validator does not accept."
  {:request {:allowed #{"workspace_root" "symbol_migration" "require_change"
                        "edits" "delete_owners" "verify"}
             :relations #{"symbol_migration" "require_change"}}
   :migration {:field "symbol_migration"
               :allowed #{"target_alias" "target_rule" "columns" "files"}
               :required #{"target_alias" "target_rule" "columns" "files"}
               :columns ["owner" "from" "matches"]
               :target-rule "preserve-name"}
   :require {:field "require_change"
             :allowed #{"add" "files"}
             :required #{"add" "files"}
             :file {:allowed #{"file" "remove"}
                    :required #{"file"}}}
   :lib-alias {:allowed #{"lib" "as"}
               :required #{"lib" "as"}}})

(def relation-fields (get-in relation-schema [:request :relations]))

(def allowed-request-fields (get-in relation-schema [:request :allowed]))

(defn- object-skeleton
  "Render one closed object skeleton for exactly the schema's allowed fields."
  [allowed field->value]
  (into {} (map (fn [field] [field (get field->value field)])) allowed))

(defn- lib-alias-skeleton []
  (object-skeleton (get-in relation-schema [:lib-alias :allowed])
                   {"lib" "<lib>" "as" "<alias>"}))

(defn- migration-skeleton []
  (let [columns (get-in relation-schema [:migration :columns])]
    (object-skeleton
      (get-in relation-schema [:migration :allowed])
      {"target_alias" "<alias>"
       "target_rule" (get-in relation-schema [:migration :target-rule])
       "columns" columns
       "files" [["<file>"
                 [(mapv {"owner" "<owner>"
                         "from" "<from-symbol>"
                         "matches" 1}
                        columns)]]]})))

(defn- require-skeleton []
  (object-skeleton
    (get-in relation-schema [:require :allowed])
    {"add" (lib-alias-skeleton)
     "files" [(object-skeleton
                (get-in relation-schema [:require :file :allowed])
                {"file" "<file>" "remove" (lib-alias-skeleton)})]}))

;; @spec MCP-OP-EDIT-037
(def relation-expected-shape
  "The literal accepted skeleton of the closed relation pair, derived from
  `relation-schema` so one shape refusal replaces the whole field-at-a-time
  ladder."
  (object-skeleton (get-in relation-schema [:request :relations])
                   {"symbol_migration" (migration-skeleton)
                    "require_change" (require-skeleton)}))

(defn- string-keys [value]
  (cond
    (map? value) (into {} (map (fn [[key child]]
                                 [(if (keyword? key) (name key) (str key))
                                  (string-keys child)])) value)
    (vector? value) (mapv string-keys value)
    (sequential? value) (mapv string-keys value)
    :else value))

(defn relation-request? [request]
  (let [request (string-keys request)]
    (boolean (some #(contains? request %) relation-fields))))

(defn- fail! [message path]
  (throw (ex-info message {:path path})))

(defn- closed-map! [value allowed required path]
  (when-not (map? value)
    (fail! "Expected one closed object" path))
  (when-let [missing (first (remove #(contains? value %) required))]
    (fail! (str "Missing required field " missing) (conj path missing)))
  (when-let [unexpected (first (sort (set/difference (set (keys value)) allowed)))]
    (fail! (str "Unknown field " unexpected) (conj path unexpected)))
  value)

(def simple-symbol-pattern #"^[A-Za-z*+!_?<>=$%&.-][A-Za-z0-9*+!_?<>=$%&.-]*$")

(def reserved-reader-tokens #{"nil" "true" "false"})

;; @spec MCP-OP-EDIT-026
;; @spec MCP-OP-EDIT-027
(defn- simple-symbol-token? [value]
  (and (string? value)
       (not (contains? reserved-reader-tokens value))
       (re-matches simple-symbol-pattern value)))

(defn- simple-symbol! [value path]
  (when-not (simple-symbol-token? value)
    (fail! "Expected one nonblank unqualified Clojure symbol" path))
  value)

(defn- namespace-symbol! [value path]
  (when-not (simple-symbol-token? value)
    (fail! "Expected one exact namespace symbol" path))
  value)

(defn- migration-symbol! [value path]
  (let [parts (when (string? value) (str/split value #"/" -1))]
    (when-not (and (#{1 2} (count parts))
                   (every? simple-symbol-token? parts))
      (fail! "Expected one exact Clojure symbol" path))
    (symbol value)))

(defn- nonblank! [value path]
  (when-not (and (string? value) (not (str/blank? value)))
    (fail! "Expected one nonblank string" path))
  value)

(defn- positive-integer! [value path]
  (when-not (and (integer? value) (pos? value))
    (fail! "Expected one positive integer" path))
  value)

(defn- relation-refusal [error-type failed-stage message path]
  (cond-> {:ok false
           :error message
           :error-type error-type
           :failed-stage failed-stage
           :mutation-attempted false
           :write-authority false
           :next-action "correct_request"
           :source-unchanged true}
    path (assoc :path path)))

(defn- request-files [request]
  (let [literal (mapcat #(if-let [file (get % "file")]
                           [file]
                           (or (get % "files") []))
                        (or (get request "edits") []))
        deletions (mapv #(get % "file") (or (get request "delete_owners") []))]
    (vec (distinct (concat literal deletions)))))

(defn- request-file-origins [request migration-files]
  (vec
    (concat
      (map-indexed (fn [index {:keys [file]}]
                     {:raw file
                      :path ["symbol_migration" "files" index 0]})
                   migration-files)
      (mapcat
        (fn [index edit]
          (if-let [file (get edit "file")]
            [{:raw file :path ["edits" index "file"]}]
            (map-indexed
              (fn [file-index file]
                {:raw file :path ["edits" index "files" file-index]})
              (or (get edit "files") []))))
        (range) (or (get request "edits") []))
      (map-indexed
        (fn [index deletion]
          {:raw (get deletion "file")
           :path ["delete_owners" index "file"]})
        (or (get request "delete_owners") [])))))

(defn- deletion-count [request]
  (reduce + 0 (map #(count (get % "forms"))
                   (or (get request "delete_owners") []))))

(defn- literal-match-count [request]
  (reduce + 0
          (map (fn [edit]
                 (* (or (get edit "matches") 1)
                    (max 1 (count (get edit "files")))))
               (or (get request "edits") []))))

(defn- validate-migration! [migration]
  (closed-map! migration
               (get-in relation-schema [:migration :allowed])
               (get-in relation-schema [:migration :required])
               ["symbol_migration"])
  (let [target-alias (simple-symbol! (get migration "target_alias")
                                     ["symbol_migration" "target_alias"])
        _ (when-not (= (get-in relation-schema [:migration :target-rule])
                       (get migration "target_rule"))
            (fail! "target_rule must be preserve-name"
                   ["symbol_migration" "target_rule"]))
        _ (when-not (= (get-in relation-schema [:migration :columns])
                       (get migration "columns"))
            (fail! "columns must be [owner, from, matches]"
                   ["symbol_migration" "columns"]))
        files (get migration "files")]
    (when-not (and (vector? files) (seq files))
      (fail! "files must be one nonempty array" ["symbol_migration" "files"]))
    (loop [file-index 0
           remaining files
           seen-files #{}
           seen-rows #{}
           parsed []]
      (if-let [entry (first remaining)]
        (do
          (when-not (and (vector? entry) (= 2 (count entry)))
            (fail! "Each migration file must be [file, rows]"
                   ["symbol_migration" "files" file-index]))
          (let [[file rows] entry
                file (nonblank! file ["symbol_migration" "files" file-index 0])]
            (when (contains? seen-files file)
              (fail! "Duplicate migration file"
                     ["symbol_migration" "files" file-index 0]))
            (when-not (and (vector? rows) (seq rows))
              (fail! "Migration rows must be one nonempty array"
                     ["symbol_migration" "files" file-index 1]))
            (let [[parsed-rows next-seen]
                  (loop [row-index 0
                         remaining-rows rows
                         seen seen-rows
                         result []]
                    (if-let [row (first remaining-rows)]
                      (do
                        (when-not (and (vector? row) (= 3 (count row)))
                          (fail! "Each migration row must be [owner, from, matches]"
                                 ["symbol_migration" "files" file-index 1 row-index]))
                        (let [[owner from matches] row
                              owner (nonblank! owner
                                               ["symbol_migration" "files"
                                                file-index 1 row-index 0])
                              parsed-from (migration-symbol!
                                            from
                                            ["symbol_migration" "files"
                                             file-index 1 row-index 1])
                              _ (positive-integer!
                                  matches
                                  ["symbol_migration" "files"
                                   file-index 1 row-index 2])
                              identity [file owner from]]
                          (when (= target-alias (namespace parsed-from))
                            (fail! "Migration target would be a no-op"
                                   ["symbol_migration" "files"
                                    file-index 1 row-index 1]))
                          (when (contains? seen identity)
                            (fail! "Duplicate migration row"
                                   ["symbol_migration" "files"
                                    file-index 1 row-index]))
                          (recur (inc row-index) (next remaining-rows)
                                 (conj seen identity)
                                 (conj result
                                       {:file file :owner owner :from from
                                        :from-symbol parsed-from
                                        :matches matches}))))
                      [result seen]))]
              (recur (inc file-index) (next remaining)
                     (conj seen-files file) next-seen
                     (conj parsed {:file file :rows parsed-rows})))))
        {:target-alias target-alias :files parsed}))))

(defn- validate-lib-alias! [value path]
  (closed-map! value
               (get-in relation-schema [:lib-alias :allowed])
               (get-in relation-schema [:lib-alias :required])
               path)
  {:lib (namespace-symbol! (get value "lib") (conj path "lib"))
   :as (simple-symbol! (get value "as") (conj path "as"))})

(defn- validate-require! [require-change]
  (closed-map! require-change
               (get-in relation-schema [:require :allowed])
               (get-in relation-schema [:require :required])
               ["require_change"])
  (let [add (validate-lib-alias! (get require-change "add")
                                 ["require_change" "add"])
        files (get require-change "files")]
    (when-not (and (vector? files) (seq files))
      (fail! "files must be one nonempty array" ["require_change" "files"]))
    {:add add
     :files
     (loop [index 0 remaining files seen #{} parsed []]
       (if-let [entry (first remaining)]
         (do
           (closed-map! entry
                        (get-in relation-schema [:require :file :allowed])
                        (get-in relation-schema [:require :file :required])
                        ["require_change" "files" index])
           (let [file (nonblank! (get entry "file")
                                 ["require_change" "files" index "file"])
                 remove (when (contains? entry "remove")
                          (validate-lib-alias!
                            (get entry "remove")
                            ["require_change" "files" index "remove"]))]
             (when (contains? seen file)
               (fail! "Duplicate require file"
                      ["require_change" "files" index "file"]))
             (when (and remove (= (:as add) (:as remove)))
               (fail! "A removal cannot use the target alias"
                      ["require_change" "files" index "remove" "as"]))
             (when (= add remove)
               (fail! "Add and removal pairs must differ"
                      ["require_change" "files" index "remove"]))
             (recur (inc index) (next remaining) (conj seen file)
                    (conj parsed {:file file :remove remove}))))
         parsed))}))

(defn- generated-symbol-edits [target-alias migration-files]
  (->> migration-files
       (mapcat (fn [{:keys [file rows]}]
                 (map (fn [{:keys [owner from from-symbol matches]}]
                        {"file" file
                         "within" {"form" owner}
                         "from" from
                         "to" (str target-alias "/" (name from-symbol))
                         "matches" matches})
                      rows)))
       vec))

(defn- obvious-overlap? [literal generated]
  (let [address (fn [edit]
                  [(get edit "file")
                   (get-in edit ["within" "form"])
                   (get edit "from")])]
    (seq (set/intersection (set (map address literal))
                           (set (map address generated))))))

;; @spec MCP-OP-EDIT-025
(defn compile-source-blind
  "Validate and lower the paired relation without reading or resolving files."
  [raw-request]
  ;; @spec MCP-OP-EDIT-020
  ;; @spec MCP-OP-EDIT-021
  (let [request (string-keys raw-request)]
    (try
      (when-let [unexpected
                 (first (sort (set/difference (set (keys request))
                                              allowed-request-fields)))]
        (fail! (str "Field is not allowed with closed compact relations: " unexpected)
               [unexpected]))
      (doseq [field relation-fields]
        (when-not (contains? request field)
          (fail! (str "Closed compact relations require " field) [field])))
      (let [migration (validate-migration! (get request "symbol_migration"))
            requires (validate-require! (get request "require_change"))
            relation-file-vector (mapv :file (:files migration))
            require-file-vector (mapv :file (:files requires))
            _ (when-not (= relation-file-vector require-file-vector)
                (fail! "The ordered relation file vectors must match"
                       ["require_change" "files"]))
            _ (when-not (= (:target-alias migration) (get-in requires [:add :as]))
                (fail! "target_alias must equal require_change.add.as"
                       ["require_change" "add" "as"]))
            generated (generated-symbol-edits (:target-alias migration)
                                              (:files migration))
            literal (vec (or (get request "edits") []))
            _ (when (obvious-overlap? literal generated)
                (throw (ex-info "A literal edit overlaps a generated symbol edit"
                                {:relation-error-type :compact-relation-overlap
                                 :failed-stage :relation-composition})))
            ordinary-request (dissoc request "symbol_migration" "require_change")
            origins (request-file-origins request (:files migration))
            declared-files (vec (distinct (map :raw origins)))
            migration-rows (count generated)
            symbol-matches (reduce + 0 (map #(get % "matches") generated))]
        {:ok true
         :request ordinary-request
         :pending-require (get request "require_change")
         :parsed-require requires
         :generated-symbol-edits generated
         :relation-files relation-file-vector
         :declared-files declared-files
         :file-origins origins
         :relation-normalization
         {:version 1
          :relations ["symbol_migration" "require_change"]
          :target_rule "preserve-name"
          :files relation-file-vector
          :migration_rows migration-rows
          :require_files (count require-file-vector)
          :literal_edits (count literal)
          :deleted_owners (deletion-count ordinary-request)
          :declared_matches symbol-matches}})
      (catch clojure.lang.ExceptionInfo error
        ;; @spec MCP-OP-EDIT-037
        (let [data (ex-data error)
              error-type (or (:relation-error-type data)
                             :invalid-compact-relation)]
          (cond-> (relation-refusal
                    error-type
                    (or (:failed-stage data) :relation-admission)
                    (.getMessage error) (:path data))
            ;; Only shape refusals teach the shape ; path-conflict,
            ;; require-change-unprovable, and overlap refusals stay closed.
            (= :invalid-compact-relation error-type)
            (assoc :expected-shape relation-expected-shape)))))))

(defn validate-path-resolution
  "Prove that one existing resolver result gives each raw relation path one canonical identity."
  [source-blind resolution]
  ;; @spec MCP-OP-EDIT-022
  (if-not (:ok resolution)
    (let [raw (:raw-path resolution)
          path (some #(when (= raw (:raw %)) (:path %))
                     (:file-origins source-blind))]
      (relation-refusal :compact-relation-path-conflict :path-resolution
                        (or (:error resolution)
                            "A relation path is outside the workspace")
                        path))
    (let [facts (:path-facts resolution)
          declared (set (map :raw (:file-origins source-blind)))
          observed (set (map :raw facts))
          bad-path (some #(when (or (nil? (:path %))
                                    (str/blank? (str (:path %))))
                            %)
                         facts)
          raw-collision
          (some (fn [[raw grouped]]
                  (let [paths (vec (distinct (map :path grouped)))]
                    (when (< 1 (count paths))
                      {:raw raw :paths paths})))
                (group-by :raw facts))
          canonical-collision
          (some (fn [[canonical grouped]]
                  (let [raws (vec (distinct (map :raw grouped)))]
                    (when (< 1 (count raws))
                      {:canonical canonical :raws raws})))
                (group-by :path facts))
          problem (cond
                    (not= declared observed)
                    {:raw (or (first (set/difference declared observed))
                              (first (set/difference observed declared)))}
                    bad-path {:raw (:raw bad-path)}
                    raw-collision raw-collision
                    canonical-collision {:raw (second (:raws canonical-collision))})]
      (if problem
        (let [raw (:raw problem)
              path (some #(when (= raw (:raw %)) (:path %))
                         (:file-origins source-blind))]
          (relation-refusal :compact-relation-path-conflict :path-resolution
                            "Relation path evidence is incomplete or non-injective"
                            path))
        (assoc resolution
               :relation-path-map
               (into {} (map (juxt :raw :path)) facts))))))

(defn- node-contains-comment? [node]
  (boolean (some #(= :comment (n/tag %))
                 (tree-seq n/inner? n/children node))))

(defn- top-level-ns-locs [source]
  (->> (z/of-string source)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= "ns" (some-> % z/down z/string))))
       vec))

(defn- require-locs [ns-loc]
  (->> (-> ns-loc z/down z/right z/right)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= ":require" (some-> % z/down z/string))))
       vec))

(defn- meaningful-node? [node]
  (not (contains? #{:whitespace :newline :comma} (n/tag node))))

;; @spec MCP-OP-EDIT-038
(defn- lenient-entry-facts
  "Read `{:lib, :as, :direct?}` from any vector libspec node.

  `:as` is nil when the entry binds no alias, and `:direct?` is true only for
  the exact three-element `[lib :as alias]` shape. Entries the request never
  names are read leniently so that an untouched `[lib :refer [...]]` cannot
  refuse a provable require change, while still proving that the target lib or
  alias is not already bound."
  [node]
  (when (= :vector (n/tag node))
    (let [value (try (n/sexpr node) (catch Exception _ nil))]
      (when (and (vector? value) (seq value) (symbol? (first value)))
        (let [alias (second (drop-while #(not= :as %) value))]
          {:lib (str (first value))
           :as (when (symbol? alias) (str alias))
           :direct? (and (= 3 (count value))
                         (= :as (nth value 1))
                         (symbol? (nth value 2)))})))))

(defn- entry-facts [node]
  (let [facts (lenient-entry-facts node)]
    (when (:direct? facts)
      (select-keys facts [:lib :as]))))

(defn- target-node [{:keys [lib as]}]
  (n/vector-node
    [(n/token-node (symbol lib))
     (n/spaces 1)
     (n/keyword-node :as)
     (n/spaces 1)
     (n/token-node (symbol as))]))

(defn- remove-range [values start end-inclusive]
  (vec (concat (subvec values 0 start)
               (subvec values (inc end-inclusive)))))

(defn- insert-after [values index inserted]
  (vec (concat (subvec values 0 (inc index)) inserted
               (subvec values (inc index)))))

(defn- compile-require-edit [source file-index file-spec add]
  (let [ns-locs (top-level-ns-locs source)]
    (when-not (= 1 (count ns-locs))
      (fail! "Expected exactly one direct namespace form"
             ["require_change" "files" file-index]))
    (let [require-locs (require-locs (first ns-locs))]
      (when-not (= 1 (count require-locs))
        (fail! "Expected exactly one direct require clause"
               ["require_change" "files" file-index]))
      (let [require-loc (first require-locs)
            require-node (z/node require-loc)
            _ (when (node-contains-comment? require-node)
                (fail! "Comment-bearing require clauses are unsupported"
                       ["require_change" "files" file-index]))
            children (vec (n/children require-node))
            meaningful-indexes (keep-indexed
                                 (fn [index node]
                                   (when (meaningful-node? node) index))
                                 children)
            entry-indexes (vec (rest meaningful-indexes))
            ;; @spec MCP-OP-EDIT-038
            ;; Positional, aligned with entry-indexes ; nil for any meaningful
            ;; child that is not a vector libspec.
            entries (mapv #(lenient-entry-facts (get children %)) entry-indexes)
            libspecs (vec (keep identity entries))
            _ (when (empty? libspecs)
                (fail! "Require entries must be direct [lib :as alias] vectors"
                       ["require_change" "files" file-index]))
            remove (:remove file-spec)
            remove-positions
            (when remove
              (vec (keep-indexed
                     (fn [position facts]
                       (when (and facts
                                  (= remove (select-keys facts [:lib :as])))
                         position))
                     entries)))
            _ (when (and remove (not= 1 (count remove-positions)))
                (fail! "Declared removal must match exactly one direct libspec"
                       ["require_change" "files" file-index "remove"]))
            ;; @spec MCP-OP-EDIT-038
            ;; The duplicate checks run against what will SURVIVE the change.
            ;; An entry the call declares it is removing is not a duplicate of
            ;; the entry replacing it, so a re-alias -- remove [lib :as a], add
            ;; [lib :as b] -- must lower rather than refuse "already required".
            survivors (if (seq remove-positions)
                        (vec (keep identity
                                   (assoc entries (first remove-positions) nil)))
                        libspecs)
            _ (when (some #(= (:lib add) (:lib %)) survivors)
                (fail! "Target namespace is already required"
                       ["require_change" "files" file-index]))
            _ (when (some #(= (:as add) (:as %)) survivors)
                (fail! "Target alias is already bound"
                       ["require_change" "files" file-index]))
            ;; @spec MCP-OP-EDIT-038
            ;; The entry a removal NAMES is an entry the call names, so it
            ;; remains provable-or-refuse -- and here the refusal is load
            ;; bearing, not ceremony: deleting [lib :as a :refer [x y]] when the
            ;; caller declared [lib :as a] would silently drop referred names
            ;; the caller never mentioned. Say exactly that, so the refusal is
            ;; recoverable instead of merely correct.
            _ (when (and remove
                         (not (entry-facts
                                (get children
                                     (get entry-indexes
                                          (first remove-positions))))))
                (fail! (str "The declared removal names a libspec that carries "
                            "more than [lib :as alias]; removing it would drop "
                            "options the removal did not declare: "
                            (z/string (z/of-node
                                        (get children
                                             (get entry-indexes
                                                  (first remove-positions))))))
                       ["require_change" "files" file-index "remove"]))
            only-removed? (and remove (= 1 (count libspecs)))
            target (target-node add)
            changed-children
            (if only-removed?
              (assoc children
                     (get entry-indexes (first remove-positions))
                     target)
              (let [without-removal
                    (if-not remove
                      children
                      (let [position (first remove-positions)
                            entry-index (get entry-indexes position)
                            [start end]
                            (if (zero? position)
                              [entry-index (dec (get entry-indexes 1))]
                              [(inc (get entry-indexes (dec position))) entry-index])]
                        (remove-range children start end)))
                    ;; @spec MCP-OP-EDIT-038
                    ;; The insertion point is the last vector libspec of any
                    ;; shape ; reading it strictly would address the wrong
                    ;; child whenever an untouched entry is not direct.
                    remaining-entry-indexes
                    (vec (keep-indexed
                           (fn [index node]
                             (when (lenient-entry-facts node) index))
                           without-removal))
                    last-index (last remaining-entry-indexes)
                    previous-index
                    (or (last (keep-indexed
                                (fn [index node]
                                  (when (and (< index last-index)
                                             (meaningful-node? node))
                                    index))
                                without-removal))
                        0)
                    separator (subvec without-removal
                                      (inc previous-index) last-index)]
                (when (empty? separator)
                  (fail! "A unique whitespace separator is required"
                         ["require_change" "files" file-index]))
                (insert-after without-removal last-index
                              (conj (vec separator) target))))
            from (z/string require-loc)
            to (n/string (n/list-node changed-children))]
        {"file" (:file file-spec)
         "within" {"namespace" true}
         "from" from
         "to" to
         "matches" 1}))))

(defn compile-frozen
  "Lower pending require decisions using only the supplied frozen source map."
  [sources source-blind]
  ;; @spec MCP-OP-EDIT-022
  ;; @spec MCP-OP-EDIT-023
  (if-not (:ok source-blind)
    source-blind
    (try
      (let [parsed (:parsed-require source-blind)
            generated-require
            (mapv (fn [index file-spec]
                    (let [file (:file file-spec)
                          source (get sources file)]
                      (when-not (string? source)
                        (fail! "Frozen source is missing for a relation file"
                               ["require_change" "files" index]))
                      (compile-require-edit source index file-spec (:add parsed))))
                  (range) (:files parsed))
            literal (vec (or (get-in source-blind [:request "edits"]) []))
            generated-symbol (:generated-symbol-edits source-blind)
            request (-> (:request source-blind)
                        (assoc "edits" (vec (concat generated-require
                                                    generated-symbol literal))))
            normalization
            (-> (:relation-normalization source-blind)
                (assoc :require_files (count generated-require)
                       :declared_matches
                       (+ (count generated-require)
                          (reduce + 0 (map #(get % "matches") generated-symbol))
                          (literal-match-count (:request source-blind))
                          (deletion-count (:request source-blind)))
                       :expanded_edits
                       (+ (count generated-require) (count generated-symbol)
                          (count literal) (deletion-count (:request source-blind)))
                       :edit_ids
                       (vec (concat
                              (map #(str "relation/require/" %)
                                   (range (count generated-require)))
                              (map #(str "relation/symbol/" %)
                                   (range (count generated-symbol)))))))]
        {:ok true
         :request request
         :generated-require-edits generated-require
         :generated-symbol-edits generated-symbol
         :relation-normalization normalization})
      (catch clojure.lang.ExceptionInfo error
        (relation-refusal :require-change-unprovable :require-lowering
                          (.getMessage error) (:path (ex-data error)))))))
