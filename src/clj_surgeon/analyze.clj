(ns clj-surgeon.analyze
  "Homoiconicity-powered analysis: because Clojure code IS data,
   we can walk the AST to discover structure that would take 10,000 lines
   in a non-homoiconic language.

   ALL FUNCTIONS ARE PURE. They take a zipper or parsed forms and return data.
   No file I/O, no side effects.

   Reader-conditional aware: top-level walking descends into #?(:clj ...) and
   #?@(:cljs [...]) branches so that defs inside reader conditionals participate
   in dependency analysis, topological sort, and extraction."
  (:require
   [clj-surgeon.forms :as forms]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

;; ============================================================
;; Core: Parse a file into a zipper (the one I/O boundary)
;; ============================================================

(defn file->zloc
  "Read a file into a rewrite-clj zipper. This is the ONLY I/O function."
  [file]
  (z/of-string (slurp file) {:track-position? true}))

(defn string->zloc
  "Parse a string into a zipper. For testing — no I/O."
  [s]
  (z/of-string s {:track-position? true}))

;; ============================================================
;; Walk: Collect all top-level forms from a zipper
;; ============================================================

(defn- reader-cond? [zloc]
  (and (= :reader-macro (some-> zloc z/node n/tag))
       (#{"?" "?@"} (some-> zloc z/down z/string))))

(defn- splicing-rcond? [zloc]
  (= "?@" (some-> zloc z/down z/string)))

(defn- list-zloc->form-map [zloc]
  {:zloc zloc
   :node (z/node zloc)
   :meta (meta (z/node zloc))
   :type-str (some-> zloc z/down z/string)
   :name-str (let [second (some-> zloc z/down z/right)]
               (when second
                 (if (= :meta (some-> second z/node n/tag))
                   (some-> second z/down z/rightmost z/string)
                   (z/string second))))})

(defn- forms-from-rcond
  "Yield list-form maps from inside a reader-conditional zloc, descending into
   each platform branch. For #?(:k FORM) the value-FORM is yielded if it's a
   list. For #?@(:k [a b c]) every list child of the splice vector is yielded."
  [rmacro-zloc]
  (let [splicing (splicing-rcond? rmacro-zloc)
        pair-list (-> rmacro-zloc z/down z/right)
        children (->> (z/down pair-list)
                      (iterate z/right)
                      (take-while some?))
        pairs (partition 2 children)]
    (mapcat (fn [[_k v-zl]]
              (cond
                splicing
                (->> (z/down v-zl)
                     (iterate z/right)
                     (take-while some?)
                     (filter z/list?)
                     (map list-zloc->form-map))

                (z/list? v-zl)
                [(list-zloc->form-map v-zl)]

                :else nil))
            pairs)))

(defn- top-level-forms
  "Walk a zipper and collect all top-level list forms with metadata.
   Descends into reader conditionals so forms inside #?(:clj ...) /
   #?@(:cljs [...]) participate in dependency analysis."
  [zloc]
  (loop [zloc zloc, forms []]
    (cond
      (nil? zloc) forms

      (reader-cond? zloc)
      (recur (z/right zloc) (into forms (forms-from-rcond zloc)))

      (z/list? zloc)
      (recur (z/right zloc) (conj forms (list-zloc->form-map zloc)))

      :else
      (recur (z/right zloc) forms))))

;; ============================================================
;; Symbols: Extract every symbol referenced within a form's subtree
;; ============================================================

(defn symbols-in-form
  "Walk a form's AST and collect every symbol token.
   Returns a set of strings like #{\"str/join\" \"get-state\" \"swap!\"}.
   IMPORTANT: operates on the form's node (not the zipper context)
   so we don't walk into sibling forms."
  [form-zloc]
  ;; Create a fresh zipper rooted at just this form's node
  ;; so z/next + z/end? correctly bound the traversal
  (let [sub-zloc (z/of-string (z/string form-zloc))]
    (loop [loc sub-zloc
           results []]
      (if (z/end? loc)
        (set results)
        (let [node (z/node loc)
              tag (n/tag node)
              results' (if (= :token tag)
                         (let [s (z/string loc)]
                           (if (and (not (str/starts-with? s ":"))
                                    (not (str/starts-with? s "\""))
                                    (not (re-matches #"[0-9].*" s))
                                    (not (#{"true" "false" "nil"} s)))
                             (conj results s)
                             results))
                         results)]
          (recur (z/next loc) results'))))))

(defn- binding-symbols
  "Return symbols introduced by a Clojure binding or destructuring form."
  [binding]
  (cond
    (symbol? binding)
    (if (= '& binding) #{} #{(str binding)})

    (vector? binding)
    (->> binding
         (remove keyword?)
         (mapcat binding-symbols)
         set)

    (map? binding)
    (reduce-kv
      (fn [result k v]
        (let [directive (when (keyword? k) (keyword (name k)))]
          (cond
            (#{:keys :syms :strs} directive)
            (into result (map (comp name symbol str) v))

            (= :as directive)
            (into result (binding-symbols v))

            (= :or directive)
            result

            :else
            (into result (binding-symbols k)))))
      #{} binding)

    :else #{}))

(declare free-symbols*)

(defn- free-symbols-many [forms bound]
  (reduce into #{} (map #(free-symbols* % bound) forms)))

(defn- binding-reference-symbols
  "Return free references evaluated by a binding form itself, notably values
   in map-destructuring :or defaults."
  [binding bound]
  (cond
    (vector? binding)
    (reduce into #{}
            (map #(binding-reference-symbols % bound)
                 (remove keyword? binding)))

    (map? binding)
    (reduce-kv
      (fn [result k v]
        (let [directive (when (keyword? k) (keyword (name k)))]
          (cond
            (= :or directive) (into result (free-symbols-many (vals v) bound))
            (#{:keys :syms :strs :as} directive) result
            :else (into result (binding-reference-symbols k bound)))))
      #{} binding)

    :else #{}))

(defn- analyze-sequential-bindings [bindings bound]
  (loop [remaining (seq bindings)
         current-bound bound
         free #{}]
    (if (empty? remaining)
      {:bound current-bound :free free}
      (let [binding (first remaining)
            init (second remaining)]
        (recur (nnext remaining)
               (into current-bound (binding-symbols binding))
               (into free
                     (concat (free-symbols* init current-bound)
                             (binding-reference-symbols binding
                                                        current-bound))))))))

(defn- fn-body-free-symbols [tail bound]
  (let [tail (loop [remaining tail]
               (cond
                 (empty? remaining) remaining
                 (or (string? (first remaining)) (map? (first remaining)))
                 (recur (rest remaining))
                 (= :- (first remaining))
                 (recur (nnext remaining))
                 :else remaining))]
    (cond
      (vector? (first tail))
      (let [args (first tail)]
        (into (binding-reference-symbols args bound)
              (free-symbols-many (rest tail)
                                 (into bound (binding-symbols args)))))

      :else
      (reduce into #{}
              (for [arity tail
                    :when (and (seq? arity) (vector? (first arity)))]
                (let [args (first arity)]
                  (into (binding-reference-symbols args bound)
                        (free-symbols-many
                          (rest arity)
                          (into bound (binding-symbols args))))))))))

(defn- comprehension-bindings [bindings bound]
  (loop [remaining (seq bindings)
         current-bound bound
         free #{}]
    (if (empty? remaining)
      {:bound current-bound :free free}
      (let [item (first remaining)
            value (second remaining)]
        (cond
          (= :let item)
          (let [{next-bound :bound next-free :free}
                (analyze-sequential-bindings value current-bound)]
            (recur (nnext remaining) next-bound (into free next-free)))

          (#{:when :while} item)
          (recur (nnext remaining) current-bound
                 (into free (free-symbols* value current-bound)))

          :else
          (recur (nnext remaining)
                 (into current-bound (binding-symbols item))
                 (into free (free-symbols* value current-bound))))))))

(defn- free-symbols*
  "Collect free symbol names from an s-expression with common Clojure lexical
   binding forms accounted for. Quoted data contributes no references."
  [form bound]
  (cond
    (symbol? form)
    (if (contains? bound (str form)) #{} #{(str form)})

    (map? form)
    (free-symbols-many (concat (keys form) (vals form)) bound)

    (coll? form)
    (if-not (seq? form)
      (free-symbols-many form bound)
      (let [op (first form)
            args (rest form)
            defining-kind (when (symbol? op) (forms/classify (str op)))]
        (cond
          (#{'quote 'clojure.core/quote} op)
          #{}

          (#{:defn :defn- :defmacro} defining-kind)
          (fn-body-free-symbols (rest args) bound)

          (#{'fn 'fn*} op)
          (let [[fn-name tail] (if (symbol? (first args))
                                 [(first args) (rest args)]
                                 [nil args])
                fn-bound (cond-> bound fn-name (conj (str fn-name)))]
            (fn-body-free-symbols tail fn-bound))

          (#{'let 'let* 'loop 'loop* 'binding 'with-open} op)
          (let [{body-bound :bound binding-free :free}
                (analyze-sequential-bindings (first args) bound)]
            (into binding-free (free-symbols-many (rest args) body-bound)))

          (#{'if-let 'when-let 'if-some 'when-some} op)
          (let [{body-bound :bound binding-free :free}
                (analyze-sequential-bindings (first args) bound)]
            (into binding-free (free-symbols-many (rest args) body-bound)))

          (= 'letfn op)
          (let [bindings (first args)
                names (set (keep #(when (and (seq? %) (symbol? (first %)))
                                    (str (first %)))
                                 bindings))
                body-bound (into bound names)
                binding-free (reduce into #{}
                                     (for [binding bindings
                                           :when (seq? binding)]
                                       (fn-body-free-symbols (rest binding)
                                                             body-bound)))]
            (into binding-free (free-symbols-many (rest args) body-bound)))

          (#{'for 'doseq} op)
          (let [{body-bound :bound binding-free :free}
                (comprehension-bindings (first args) bound)]
            (into binding-free (free-symbols-many (rest args) body-bound)))

          (= 'catch op)
          (let [[_class local & body] args]
            (free-symbols-many body (into bound (binding-symbols local))))

          :else
          (free-symbols-many form bound))))

    :else #{}))

(defn free-symbols-in-form
  "Return free symbol names referenced by a form, excluding locals and quoted
   data. This is the dependency-analysis view; `symbols-in-form` remains the
   raw token inventory used by qualified-symbol analysis."
  [form-zloc]
  (free-symbols* (z/sexpr form-zloc) #{}))

;; ============================================================
;; Qualified symbols: namespace-qualified references in a form
;; ============================================================

(defn qualified-symbols
  "Extract namespace-qualified symbols from a form.
   Returns #{\"str\" \"state\" \"sse\"} — the alias prefixes."
  [form-zloc]
  (->> (symbols-in-form form-zloc)
       (filter #(str/includes? % "/"))
       (map #(first (str/split % #"/")))
       set))

(defn required-aliases
  "Given a form and the ns declaration's alias map,
   return the require entries the form needs.

   alias-map: {\"str\" 'clojure.string, \"state\" 'writer.state, ...}"
  [form-zloc alias-map]
  (let [used (qualified-symbols form-zloc)]
    (->> alias-map
         (filter (fn [[alias _]] (contains? used alias)))
         (into {}))))

;; ============================================================
;; Alias map: Parse a namespace form to get alias → ns mapping
;; ============================================================

(defn- extract-alias-from-vector
  "Extract alias entry from a require vector zloc.
   [clojure.string :as str] → [\"str\" \"clojure.string\"], or nil."
  [v]
  (let [children (->> (z/down v)
                      (iterate z/right)
                      (take-while some?)
                      (map z/string)
                      vec)
        as-idx (.indexOf children ":as")]
    (when (pos? as-idx)
      [(nth children (inc as-idx)) (first children)])))

(defn- aliases-from-rcond-in-require
  "Extract alias entries from reader-conditional nodes inside a :require form.
   Handles both #?(:clj [ns :as a]) and #?@(:cljs [[ns1 :as a] [ns2 :as b]])."
  [rcond-zloc]
  (let [splicing (splicing-rcond? rcond-zloc)
        pair-list (-> rcond-zloc z/down z/right)
        children (->> (z/down pair-list)
                      (iterate z/right)
                      (take-while some?))
        pairs (partition 2 children)]
    (mapcat (fn [[_k v-zl]]
              (cond
                ;; #?@(:clj [[ns1 :as a] [ns2 :as b]]) — splice: v-zl is a vector of vectors
                splicing
                (->> (z/down v-zl)
                     (iterate z/right)
                     (take-while some?)
                     (filter z/vector?)
                     (keep extract-alias-from-vector))

                ;; #?(:clj [ns :as a]) — single vector
                (z/vector? v-zl)
                (when-let [entry (extract-alias-from-vector v-zl)]
                  [entry])

                :else nil))
            pairs)))

(defn parse-ns-aliases
  "Parse the (ns ...) form and extract {:alias namespace} map.
   E.g., (:require [clojure.string :as str]) → {\"str\" clojure.string}
   Reader-conditional-aware: also finds aliases inside #?(:clj [...]) and
   #?@(:cljs [[...]]) within the :require block."
  [ns-zloc]
  (let [require-form (->> (z/down ns-zloc)
                          (iterate z/right)
                          (take-while some?)
                          (filter #(and (z/list? %)
                                        (= ":require" (some-> % z/down z/string))))
                          first)]
    (when require-form
      (let [require-children (->> (z/down require-form)
                                  (iterate z/right)
                                  (take-while some?))
            ;; Direct vector children (shared requires)
            direct-aliases (->> require-children
                                (filter z/vector?)
                                (keep extract-alias-from-vector))
            ;; Reader-conditional children (platform-specific requires)
            rcond-aliases (->> require-children
                               (filter reader-cond?)
                               (mapcat aliases-from-rcond-in-require))]
        (into {} (concat direct-aliases rcond-aliases))))))

;; ============================================================
;; Intra-namespace dependency graph
;; ============================================================

(defn intra-ns-deps
  "For each top-level form, find which OTHER forms in the same namespace
   it references. Returns adjacency list:
   [{:name \"foo\" :depends-on #{\"bar\" \"baz\"}} ...]"
  [zloc]
  (let [forms (->> (top-level-forms zloc)
                   (remove #(#{"ns" "declare"} (:type-str %)))) ;; skip ns + declare forms
        all-names (set (keep :name-str forms))]
    (->> forms
         (filter :name-str)
         (mapv (fn [f]
                 (let [syms (free-symbols-in-form (:zloc f))
                       deps (disj (set/intersection syms all-names)
                                  (:name-str f))] ;; don't count self-reference
                   {:name (:name-str f)
                    :type (:type-str f)
                    :line (:row (:meta f))
                    :depends-on deps}))))))

;; ============================================================
;; Dead code: forms that nothing else in the namespace references
;; ============================================================

(defn unreferenced-forms
  "Find private forms that are never referenced by any other form
   in the namespace. Candidates for deletion."
  [zloc]
  (let [deps (intra-ns-deps zloc)
        all-referenced (->> deps
                            (mapcat (comp seq :depends-on))
                            set)]
    (->> deps
         (filter (fn [d]
                   (and (not (contains? all-referenced (:name d)))
                        ;; Only flag private forms — public might be used externally
                        (forms/private-form? (:type d)))))
         (mapv #(select-keys % [:name :type :line])))))

;; ============================================================
;; Closure: minimal extractable unit
;; ============================================================

(defn extraction-closure
  "Given a form name, find it + all private helpers it exclusively depends on.
   'Exclusively' means the helper is ONLY called by forms in this closure,
   not by anything else in the namespace.

   This is the minimal set of forms you'd need to extract together."
  [zloc target-name]
  (let [deps (intra-ns-deps zloc)
        deps-by-name (into {} (map (juxt :name identity) deps))
        ;; Build reverse deps: who depends on each form?
        rev-deps (reduce (fn [acc {:keys [name depends-on]}]
                           (reduce (fn [a dep]
                                     (update a dep (fnil conj #{}) name))
                                   acc depends-on))
                         {} deps)]
    ;; BFS: start from target, pull in private deps that only this closure uses
    (loop [queue [target-name]
           closure #{}
           visited #{}]
      (if (empty? queue)
        (let [closure-deps (filter #(contains? closure (:name %)) deps)]
          {:target target-name
           :forms (vec (sort-by :line closure-deps))
           :total-lines (when (seq closure-deps)
                          (- (apply max (map #(+ (:line %) 10) closure-deps)) ;; rough estimate
                             (apply min (map :line closure-deps))))})
        (let [current (first queue)
              rest-q (rest queue)]
          (if (visited current)
            (recur rest-q closure visited)
            (let [form (get deps-by-name current)
                  closure' (conj closure current)
                  visited' (conj visited current)
                  ;; Pull in dependencies that are ONLY used by this closure
                  new-deps (->> (:depends-on form)
                                (filter (fn [dep]
                                          (let [callers (get rev-deps dep #{})]
                                            ;; Include if all callers are already in our closure
                                            ;; or if it's private (defn-)
                                            (and (not (visited' dep))
                                                 (let [dep-form (get deps-by-name dep)]
                                                   (or (forms/private-form? (:type dep-form))
                                                       (every? closure' callers)))))))
                                vec)]
              (recur (into (vec rest-q) new-deps)
                     closure'
                     visited'))))))))

;; ============================================================
;; Dependency tree: transitive deps as a tree structure
;; ============================================================

(defn dep-tree
  "Build a transitive dependency tree for a named form.
   Returns a nested map showing the full dep chain with metadata.
   PURE — takes deps list, no I/O."
  ([deps-list target-name] (dep-tree deps-list target-name #{}))
  ([deps-list target-name visited]
   (let [deps-by-name (into {} (map (juxt :name identity) deps-list))
         form (get deps-by-name target-name)]
     (when form
       (let [children (->> (:depends-on form)
                           sort
                           (mapv (fn [dep-name]
                                   (if (contains? visited dep-name)
                                     {:name dep-name :circular? true}
                                     (or (dep-tree deps-list dep-name
                                                   (conj visited target-name))
                                         {:name dep-name :external? true})))))]
         (cond-> {:name (:name form)
                  :type (:type form)
                  :line (:line form)
                  :leaf? (empty? (:depends-on form))}
           (seq children) (assoc :deps children)))))))

(defn flatten-dep-tree
  "Flatten a dep-tree into a set of all form names (transitive closure)."
  [tree]
  (if (nil? tree)
    #{}
    (let [children (:deps tree [])]
      (reduce (fn [acc child]
                (if (or (:circular? child) (:external? child))
                  acc
                  (into acc (flatten-dep-tree child))))
              #{(:name tree)}
              children))))

;; ============================================================
;; Topological sort: reorder to eliminate forward refs
;; ============================================================

(defn topological-sort
  "Topologically sort forms so each form appears AFTER its dependencies.
   This is the order that eliminates forward references.
   Returns {:sorted [...] :cycles [...]}. Cycles need (declare)."
  [zloc]
  (let [deps (intra-ns-deps zloc)
        ;; dep-count: how many intra-ns deps does each form have?
        dep-count (into {} (map (fn [d] [(:name d) (count (:depends-on d))]) deps))
        ;; reverse-adj: form -> list of forms that depend on it
        reverse-adj (reduce (fn [acc {:keys [name depends-on]}]
                              (reduce (fn [a dep]
                                        (update a dep (fnil conj []) name))
                                      acc depends-on))
                            {} deps)
        ;; Start with forms that have ZERO dependencies (they go first)
        start (->> dep-count (filter #(zero? (val %))) (map key) sort vec)]
    (loop [queue start
           sorted []
           dcnt dep-count
           remaining (set (map :name deps))]
      (if (empty? queue)
        {:sorted sorted
         :cycles (vec (sort remaining))
         :has-cycles? (boolean (seq remaining))}
        (let [node (first queue)
              rest-q (vec (rest queue))
              ;; Emit node. Decrement dep-count for forms that depend on node.
              dependents (get reverse-adj node [])
              dcnt' (reduce (fn [d n] (update d n dec)) dcnt dependents)
              remaining' (disj remaining node)
              new-ready (->> dependents
                             (filter #(and (zero? (get dcnt' %))
                                           (contains? remaining' %)))
                             sort vec)]
          (recur (into rest-q new-ready)
                 (conj sorted node)
                 dcnt'
                 remaining'))))))
