(ns clj-surgeon.analyze
  "Homoiconicity-powered analysis: because Clojure code IS data,
   we can walk the AST to discover structure that would take 10,000 lines
   in a non-homoiconic language.

   ALL FUNCTIONS ARE PURE. They take a zipper or parsed forms and return data.
   No file I/O, no side effects."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]))

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

(defn- top-level-forms
  "Walk a zipper and collect all top-level list forms with metadata."
  [zloc]
  (loop [zloc zloc, forms []]
    (if (nil? zloc)
      forms
      (recur (z/right zloc)
             (if (z/list? zloc)
               (conj forms {:zloc zloc
                            :node (z/node zloc)
                            :meta (meta (z/node zloc))
                            :type-str (some-> zloc z/down z/string)
                            :name-str (let [second (some-> zloc z/down z/right)]
                                        (when second
                                          ;; Skip metadata nodes (^:private, ^:dynamic, etc.)
                                          (if (= :meta (some-> second z/node n/tag))
                                            (some-> second z/down z/rightmost z/string)
                                            (z/string second))))})
               forms)))))

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

(defn parse-ns-aliases
  "Parse the (ns ...) form and extract {:alias namespace} map.
   E.g., (:require [clojure.string :as str]) → {\"str\" clojure.string}"
  [ns-zloc]
  (let [require-form (->> (z/down ns-zloc)
                          (iterate z/right)
                          (take-while some?)
                          (filter #(and (z/list? %)
                                        (= ":require" (some-> % z/down z/string))))
                          first)]
    (when require-form
      (->> (z/down require-form)
           (iterate z/right)
           (take-while some?)
           (filter z/vector?)
           (map (fn [v]
                  ;; [clojure.string :as str] → {"str" "clojure.string"}
                  (let [children (->> (z/down v)
                                      (iterate z/right)
                                      (take-while some?)
                                      (map z/string)
                                      vec)
                        as-idx (.indexOf children ":as")]
                    (when (pos? as-idx)
                      [(nth children (inc as-idx)) (first children)]))))
           (filter some?)
           (into {})))))

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
                 (let [syms (symbols-in-form (:zloc f))
                       deps (disj (clojure.set/intersection syms all-names)
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
                            set)
        private-types #{"defn-" ">defn-"}]
    (->> deps
         (filter (fn [d]
                   (and (not (contains? all-referenced (:name d)))
                        ;; Only flag private forms — public might be used externally
                        (or (contains? private-types (:type d))
                            (str/starts-with? (or (:type d) "") "defn-")))))
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
                                                   (or (= "defn-" (:type dep-form))
                                                       (every? closure' callers)))))))
                                vec)]
              (recur (into (vec rest-q) new-deps)
                     closure'
                     visited'))))))))

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
