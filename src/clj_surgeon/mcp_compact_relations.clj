(ns clj-surgeon.mcp-compact-relations
  "Pure lowering for the closed compact symbol_migration + require_change pair."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(def relation-fields #{"symbol_migration" "require_change"})

(def allowed-request-fields
  #{"workspace_root" "symbol_migration" "require_change"
    "edits" "delete_owners" "verify"})

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

(defn- simple-symbol! [value path]
  (when-not (and (string? value)
                 (re-matches simple-symbol-pattern value))
    (fail! "Expected one nonblank unqualified Clojure symbol" path))
  value)

(defn- namespace-symbol! [value path]
  (when-not (and (string? value)
                 (not (str/blank? value))
                 (try
                   (let [parsed (read-string value)]
                     (and (symbol? parsed)
                          (nil? (namespace parsed))
                          (= value (str parsed))))
                   (catch Exception _ false)))
    (fail! "Expected one exact namespace symbol" path))
  value)

(defn- migration-symbol! [value path]
  (let [parsed (when (string? value)
                 (try (read-string value) (catch Exception _ nil)))]
    (when-not (and (symbol? parsed)
                   (= value (str parsed))
                   (not (str/blank? (name parsed))))
      (fail! "Expected one exact Clojure symbol" path))
    parsed))

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
  (let [literal (mapv #(get % "file") (or (get request "edits") []))
        deletions (mapv #(get % "file") (or (get request "delete_owners") []))]
    (vec (distinct (concat literal deletions)))))

(defn- deletion-count [request]
  (reduce + 0 (map #(count (get % "forms"))
                   (or (get request "delete_owners") []))))

(defn- literal-match-count [request]
  (reduce + 0 (map #(or (get % "matches") 1)
                   (or (get request "edits") []))))

(defn- validate-migration! [migration]
  (closed-map! migration
               #{"target_alias" "target_rule" "columns" "files"}
               #{"target_alias" "target_rule" "columns" "files"}
               ["symbol_migration"])
  (let [target-alias (simple-symbol! (get migration "target_alias")
                                     ["symbol_migration" "target_alias"])
        _ (when-not (= "preserve-name" (get migration "target_rule"))
            (fail! "target_rule must be preserve-name"
                   ["symbol_migration" "target_rule"]))
        _ (when-not (= ["owner" "from" "matches"]
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
  (closed-map! value #{"lib" "as"} #{"lib" "as"} path)
  {:lib (namespace-symbol! (get value "lib") (conj path "lib"))
   :as (simple-symbol! (get value "as") (conj path "as"))})

(defn- validate-require! [require-change]
  (closed-map! require-change #{"add" "files"} #{"add" "files"}
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
           (closed-map! entry #{"file" "remove"} #{"file"}
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
            declared-files (vec (distinct (concat relation-file-vector
                                                  (request-files ordinary-request))))
            migration-rows (count generated)
            symbol-matches (reduce + 0 (map #(get % "matches") generated))]
        {:ok true
         :request ordinary-request
         :pending-require (get request "require_change")
         :parsed-require requires
         :generated-symbol-edits generated
         :relation-files relation-file-vector
         :declared-files declared-files
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
        (let [data (ex-data error)]
          (relation-refusal
            (or (:relation-error-type data) :invalid-compact-relation)
            (or (:failed-stage data) :relation-admission)
            (.getMessage error) (:path data)))))))

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

(defn- entry-facts [node]
  (when (= :vector (n/tag node))
    (let [value (try (n/sexpr node) (catch Exception _ nil))]
      (when (and (vector? value) (= 3 (count value))
                 (symbol? (nth value 0)) (= :as (nth value 1))
                 (symbol? (nth value 2)))
        {:lib (str (nth value 0))
         :as (str (nth value 2))}))))

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
            entries (mapv #(entry-facts (get children %)) entry-indexes)
            _ (when (or (empty? entries) (some nil? entries))
                (fail! "Require entries must be direct [lib :as alias] vectors"
                       ["require_change" "files" file-index]))
            _ (when (some #(= (:lib add) (:lib %)) entries)
                (fail! "Target namespace is already required"
                       ["require_change" "files" file-index]))
            _ (when (some #(= (:as add) (:as %)) entries)
                (fail! "Target alias is already bound"
                       ["require_change" "files" file-index]))
            remove (:remove file-spec)
            remove-positions (when remove
                               (keep-indexed #(when (= remove %2) %1) entries))
            _ (when (and remove (not= 1 (count remove-positions)))
                (fail! "Declared removal must match exactly one direct libspec"
                       ["require_change" "files" file-index "remove"]))
            only-removed? (and remove (= 1 (count entries)))
            target (target-node add)
            changed-children
            (if only-removed?
              (assoc children (first entry-indexes) target)
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
                    remaining-entry-indexes
                    (vec (keep-indexed
                           (fn [index node]
                             (when (entry-facts node) index))
                           without-removal))
                    last-index (last remaining-entry-indexes)
                    previous-index (or (last (butlast remaining-entry-indexes)) 0)
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
