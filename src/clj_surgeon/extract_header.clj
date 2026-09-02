(ns clj-surgeon.extract-header
  "Compile dependency-minimal namespace headers for extraction."
  (:require
   [clj-surgeon.analyze :as analyze]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z]))

(defn- refusal [entry reason]
  {:ok false
   :error "Cannot prove a dependency-minimal extraction header"
   :error-type :unsupported-require-minimization
   :entry entry
   :reason reason
   :source-unchanged true
   :target-unchanged true})

(defn- require-form-zloc [ns-zloc]
  (->> (some-> ns-zloc z/down)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= ":require" (some-> % z/down z/string))))
       first))

(defn- option-map [entry]
  (loop [parts (seq (rest entry))
         result {}]
    (cond
      (nil? parts) {:ok true :options result}
      (not (keyword? (first parts)))
      (refusal (pr-str entry) :prefix-or-non-keyword-libspec)
      (nil? (next parts))
      (refusal (pr-str entry) :missing-option-value)
      :else
      (recur (nnext parts) (assoc result (first parts) (second parts))))))

(defn- libspec-facts [entry source]
  (if-not (and (vector? entry) (symbol? (first entry)))
    (refusal source :non-vector-or-non-symbol-libspec)
    (let [parsed (option-map entry)]
      (if-not (:ok parsed)
        parsed
        (let [options (:options parsed)
              referred (:refer options)
              renamed (:rename options)]
          (cond
            (and referred
                 (not= :all referred)
                 (not (and (vector? referred) (every? symbol? referred))))
            (refusal source :unsupported-refer-shape)

            (and renamed
                 (not (and (map? renamed)
                           (every? symbol? (keys renamed))
                           (every? symbol? (vals renamed)))))
            (refusal source :unsupported-rename-shape)

            :else
            {:ok true
             :source source
             :namespace (first entry)
             :alias (or (:as options) (:as-alias options))
             :referred referred
             :renamed renamed}))))))

(defn- source-free-symbols [form-sources]
  (reduce
    set/union
    #{}
    (for [source form-sources
          loc (take-while some? (iterate z/right (z/of-string source)))]
      (try
        (analyze/free-symbols-in-form loc)
        (catch Exception _ #{})))))

(defn- source-qualified-keyword-prefixes [form-sources]
  (->> form-sources
       (mapcat (fn [source]
                 (let [root (z/of-string source)]
                   (loop [loc root
                          prefixes []]
                     (if (z/end? loc)
                       prefixes
                       (let [text (when (= :token (n/tag (z/node loc)))
                                    (z/string loc))
                             prefix (some->> text
                                             (re-matches #"::([^/]+)/.+")
                                             second)]
                         (recur (z/next loc) (cond-> prefixes prefix (conj prefix)))))))))
       set))

(defn- qualified-prefixes [symbols]
  (->> symbols
       (filter #(str/includes? % "/"))
       (map #(first (str/split % #"/")))
       set))

(defn- unqualified-symbols [symbols]
  (->> symbols
       (remove #(str/includes? % "/"))
       set))

(defn- libspec-required?
  [{:keys [namespace alias referred renamed]} symbols keyword-prefixes]
  (let [qualified (qualified-prefixes symbols)
        unqualified (unqualified-symbols symbols)
        referred-symbols (if (vector? referred) (set (map str referred)) #{})
        renamed-symbols (set (map str (vals renamed)))]
    (or (contains? qualified (str namespace))
        (and alias (contains? qualified (str alias)))
        (and alias (contains? keyword-prefixes (str alias)))
        (= :all referred)
        (seq (set/intersection unqualified referred-symbols))
        (seq (set/intersection unqualified renamed-symbols)))))

(defn- meaningful-require-child? [loc]
  (not (contains? #{:whitespace :newline :comma} (n/tag (z/node loc)))))

(defn- node-contains-comment? [node]
  (some #(= :comment (n/tag %))
        (tree-seq n/inner? n/children node)))

(defn- direct-libspecs [ns-form]
  (let [ns-zloc (z/of-string ns-form)
        require-zloc (require-form-zloc ns-zloc)]
    (if-not require-zloc
      {:ok true :ns-zloc ns-zloc :require-zloc nil :entries []}
      (let [children (->> (some-> require-zloc z/down z/right)
                          (iterate z/right)
                          (take-while some?)
                          (filter meaningful-require-child?)
                          vec)]
        (cond
          (node-contains-comment? (z/node require-zloc))
          (refusal (z/string require-zloc) :comment-bearing-require-clause)

          (some #(not (z/vector? %)) children)
          (refusal (z/string require-zloc) :reader-conditional-or-non-vector-entry)

          :else
          (let [entries (mapv #(libspec-facts (z/sexpr %) (z/string %)) children)
                failed (some #(when-not (:ok %) %) entries)]
            (if failed
              failed
              {:ok true
               :ns-zloc ns-zloc
               :require-zloc require-zloc
               :entries entries})))))))

(defn- import-form-zloc [ns-zloc]
  (->> (some-> ns-zloc z/down)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= ":import" (some-> % z/down z/string))))
       first))

(defn- import-entry-facts
  "Classify one direct :import entry.
   (pkg C1 C2) -> {:kind :group :package \"pkg\" :classes [\"C1\" \"C2\"]}
   fully.qualified.Class -> {:kind :class :classes [\"Class\"]}"
  [loc]
  (let [source (z/string loc)]
    (cond
      (z/list? loc)
      (let [children (->> (some-> loc z/down)
                          (iterate z/right)
                          (take-while some?)
                          (filter meaningful-require-child?)
                          (mapv z/string))]
        (when (and (seq children) (every? #(re-matches #"[^\s()\[\]{}]+" %) children))
          {:kind :group :source source
           :package (first children) :classes (vec (rest children))}))

      (= :token (n/tag (z/node loc)))
      (let [simple (last (str/split source #"\."))]
        (when (re-matches #"[A-Za-z_$][A-Za-z0-9_$.]*" source)
          {:kind :class :source source :classes [simple]}))

      :else nil)))

(defn- class-referenced?
  "Pure: does any of the given source texts reference this simple class name?"
  [class-name sources]
  (let [pattern (re-pattern (str "(?<![A-Za-z0-9_.$-])"
                                 (java.util.regex.Pattern/quote class-name)
                                 "(?![A-Za-z0-9_$-])"))]
    (boolean (some #(re-find pattern %) sources))))

(defn- all-import-class-names
  "Pure: every simple class name named by one namespace form's :import clause."
  [ns-form]
  (let [import-zloc (import-form-zloc (z/of-string ns-form))]
    (if-not import-zloc
      []
      (->> (some-> import-zloc z/down z/right)
           (iterate z/right)
           (take-while some?)
           (filter meaningful-require-child?)
           (keep import-entry-facts)
           (mapcat :classes)
           vec))))

(defn- import-form-source [entries]
  (str "(:import\n"
       (str/join "\n" (map #(str "   " (:rendered %)) entries))
       ")"))

;; @spec MCP-OP-EXTRACT-003
(defn narrow-import-clause
  "Purely retain exactly the :import entries a set of source texts references.

  Returns {:ok true :ns-form <text> :retained [..] :omitted [..]} or a typed
  refusal for an import shape it cannot prove. Package groups keep their source
  order; a group that loses every class is dropped, and an emptied :import
  clause is removed."
  [ns-form referencing-sources]
  (let [zloc (z/of-string ns-form)
        import-zloc (import-form-zloc zloc)]
    (if-not import-zloc
      {:ok true :ns-form ns-form :retained [] :omitted []}
      (let [children (->> (some-> import-zloc z/down z/right)
                          (iterate z/right)
                          (take-while some?)
                          (filter meaningful-require-child?)
                          vec)]
        (cond
          (node-contains-comment? (z/node import-zloc))
          (refusal (z/string import-zloc) :comment-bearing-import-clause)

          :else
          (let [facts (mapv import-entry-facts children)]
            (if (some nil? facts)
              (refusal (z/string import-zloc) :unsupported-import-entry)
              (let [decided
                    (mapv (fn [{:keys [kind package classes source] :as entry}]
                            (let [kept (filterv #(class-referenced? % referencing-sources)
                                                classes)]
                              (assoc entry
                                     :kept kept
                                     :rendered
                                     (cond
                                       (empty? kept) nil
                                       (= :class kind) source
                                       (= (count kept) (count classes)) source
                                       :else (str "(" package " "
                                                  (str/join " " kept) ")")))))
                          facts)
                    retained (filterv :rendered decided)
                    omitted (remove :rendered decided)]
                {:ok true
                 :ns-form
                 (cond
                   ;; Nothing was pruned: leave the caller's own layout alone.
                   ;; Reformatting a clause we did not change is churn, and this
                   ;; namespace's whole contract is byte preservation.
                   (= (mapv :source decided) (mapv :rendered decided))
                   ns-form

                   (seq retained)
                   (-> import-zloc
                       (z/replace (parser/parse-string (import-form-source retained)))
                       z/root-string)

                   :else (-> import-zloc z/remove z/root-string))
                 :retained (mapv :rendered retained)
                 :omitted (mapv :source omitted)}))))))))

;; @spec MCP-OP-EXTRACT-002
(defn set-ns-docstring
  "Purely install or remove one namespace docstring.

  A nil or blank doc removes any existing docstring; a supplied doc replaces it
  in place, preserving the source's own whitespace between the name and the
  docstring."
  [ns-form doc]
  (let [zloc (z/of-string ns-form)
        name-zloc (some-> zloc z/down z/right)
        existing (some-> name-zloc z/right)
        existing-doc? (and existing
                           (= :token (n/tag (z/node existing)))
                           (string? (try (z/sexpr existing) (catch Exception _ nil))))]
    (cond
      (and existing-doc? (str/blank? (str doc)))
      (-> existing z/remove z/root-string)

      existing-doc?
      (-> existing (z/replace (n/string-node (str/split-lines (str doc))))
          z/root-string)

      (str/blank? (str doc))
      ns-form

      :else
      (-> name-zloc
          (z/insert-right (n/string-node (str/split-lines (str doc))))
          (z/insert-right (n/spaces 2))
          (z/insert-right (n/newlines 1))
          z/root-string))))

(defn- replace-namespace-name [ns-form target-ns]
  (let [zloc (z/of-string ns-form)
        name-zloc (some-> zloc z/down z/right)
        source-ns (some-> name-zloc z/sexpr str)]
    (when-not name-zloc
      (throw (ex-info "Namespace form has no name"
                      {:error-type :invalid-namespace-form})))
    (-> name-zloc
        (z/replace (parser/parse-string
                     (str/replace (z/string name-zloc) source-ns target-ns)))
        z/root-string)))

(defn- require-form-source [entries]
  (str "(:require\n"
       (str/join "\n" (map #(str "   " (:source %)) entries))
       ")"))

(defn- install-require-form [ns-form entries]
  (let [zloc (z/of-string ns-form)
        require-zloc (require-form-zloc zloc)]
    (cond
      (and require-zloc (seq entries))
      (-> require-zloc
          (z/replace (parser/parse-string (require-form-source entries)))
          z/root-string)

      require-zloc
      (-> require-zloc z/remove z/root-string)

      :else ns-form)))

(defn compile-target-header
  "Compile the target namespace header under an explicit dependency policy.
  :minimal keeps only statically proved direct libspecs and imports. :copy-all
  changes only the namespace name and preserves the complete source dependency
  header exactly.

  Under :minimal the target carries a docstring only when the caller supplies
  :doc, because a copied docstring describes the namespace it came from, not
  this one. :copy-all keeps its promise to preserve the complete source header
  exactly, docstring included."
  [{:keys [source-ns-form target-ns form-sources require-policy doc]
    :or {require-policy :minimal}}]
  (case require-policy
    :copy-all
    (let [require-zloc (require-form-zloc (z/of-string source-ns-form))
          copied-count (if require-zloc
                         (->> (some-> require-zloc z/down z/right)
                              (iterate z/right)
                              (take-while some?)
                              (filter meaningful-require-child?)
                              count)
                         0)]
      {:ok true
       :require-policy :copy-all
       :ns-form (replace-namespace-name source-ns-form target-ns)
       :copied-require-count copied-count
       :target-requires :copied-exactly
       :omitted-target-requires []})

    :minimal
    (let [parsed (direct-libspecs source-ns-form)]
      (if-not (:ok parsed)
        parsed
        (let [symbols (source-free-symbols form-sources)
              keyword-prefixes (source-qualified-keyword-prefixes form-sources)
              decisions (mapv (fn [entry]
                                (assoc entry :required?
                                       (libspec-required? entry
                                                          symbols
                                                          keyword-prefixes)))
                              (:entries parsed))
              ambiguous (some (fn [{:keys [required? alias referred renamed]
                                    :as entry}]
                                (when (and (not required?)
                                           (nil? alias)
                                           (nil? referred)
                                           (nil? renamed))
                                  entry))
                              decisions)]
          (if ambiguous
            (refusal (:source ambiguous) :side-effect-only-require)
            (let [retained (filterv :required? decisions)
                  omitted (filterv (complement :required?) decisions)
                  renamed-ns (-> (replace-namespace-name source-ns-form target-ns)
                                 (set-ns-docstring doc))
                  require-narrowed (install-require-form renamed-ns retained)
                  ;; @spec MCP-OP-EXTRACT-003
                  ;; Imports are pruned exactly as requires already are.
                  import-result (narrow-import-clause require-narrowed
                                                      form-sources)]
              (if-not (:ok import-result)
                import-result
                {:ok true
                 :require-policy :minimal
                 :ns-form (:ns-form import-result)
                 :target-requires (mapv (comp str :namespace) retained)
                 :omitted-target-requires
                 (mapv (comp str :namespace) omitted)
                 :target-imports (:retained import-result)
                 :omitted-target-imports (:omitted import-result)}))))))

    {:ok false
     :error-type :unknown-require-policy
     :error (str "Unknown extraction require policy: " require-policy)
     :supported-require-policies [:copy-all :minimal]}))

(defn source-aliases
  "Return direct :as and :as-alias bindings from one namespace form."
  [source-ns-form]
  (let [parsed (direct-libspecs source-ns-form)]
    (if-not (:ok parsed)
      parsed
      {:ok true
       :aliases (into {}
                      (keep (fn [{:keys [alias namespace]}]
                              (when alias [(str alias) (str namespace)])))
                      (:entries parsed))})))

(defn allocate-alias
  "Choose a deterministic alias not bound to a different namespace."
  [target-ns aliases]
  (let [base (last (str/split target-ns #"\."))
        existing-for-target (->> aliases
                                 (keep (fn [[alias namespace]]
                                         (when (= target-ns (str namespace)) alias)))
                                 sort
                                 first)]
    (or existing-for-target
        (first
          (remove #(contains? aliases %)
                  (cons base (map #(str base %) (iterate inc 2))))))))

(defn remaining-source-callers
  "Return remaining top-level owners that depend on moved Vars."
  [source moved-vars]
  (let [moved (set (map str moved-vars))]
    (->> (analyze/intra-ns-deps (analyze/string->zloc source))
         (remove #(contains? moved (:name %)))
         (keep (fn [{:keys [name depends-on]}]
                 (let [used (sort (set/intersection moved depends-on))]
                   (when (seq used)
                     {:owner name :moved-vars (vec used)}))))
         vec)))

(defn source-referred-forms [callers]
  (->> callers (mapcat :moved-vars) distinct sort vec))

;; @spec MCP-OP-EXTRACT-005
(defn narrow-source-ns-header
  "Purely remove from one source namespace header each require and import entry
  that the moved forms referenced and the remaining body no longer references.

  An entry the moved forms never referenced is never touched, so an already-dead
  require stays exactly as the caller left it: this narrows only what the
  extraction itself made dead. Returns
  {:ok true :ns-form <text> :removed-requires [..] :removed-imports [..]}
  or a typed refusal for a header shape it cannot prove."
  [ns-form moved-sources remaining-sources]
  (let [parsed (direct-libspecs ns-form)]
    (if-not (:ok parsed)
      parsed
      (let [moved-symbols (source-free-symbols moved-sources)
            moved-prefixes (source-qualified-keyword-prefixes moved-sources)
            remaining-symbols (source-free-symbols remaining-sources)
            remaining-prefixes (source-qualified-keyword-prefixes remaining-sources)
            decisions
            (mapv (fn [entry]
                    (let [moved? (libspec-required? entry moved-symbols
                                                    moved-prefixes)
                          remaining? (libspec-required? entry remaining-symbols
                                                        remaining-prefixes)]
                      (assoc entry :dead? (and moved? (not remaining?)))))
                  (:entries parsed))
            retained (filterv (complement :dead?) decisions)
            removed (filterv :dead? decisions)
            require-narrowed
            (if (seq removed)
              (install-require-form ns-form retained)
              ns-form)
            ;; An import entry survives when the remaining body still references
            ;; it OR when the moved forms never referenced it at all.
            import-result
            (narrow-import-clause
              require-narrowed
              (conj (vec remaining-sources)
                    ;; a synthetic reference text naming every class the moved
                    ;; forms did NOT use, so untouched imports are never pruned
                    (str/join " "
                              (remove #(class-referenced? % moved-sources)
                                      (all-import-class-names ns-form)))))]
        (if-not (:ok import-result)
          import-result
          {:ok true
           :ns-form (:ns-form import-result)
           :removed-requires (mapv (comp str :namespace) removed)
           :removed-imports (:omitted import-result)})))))

(defn alias-for-namespace
  "Pure: the alias one namespace form binds to a given required namespace, or
  nil when it requires that namespace without an alias or not at all."
  [ns-form namespace]
  (let [result (source-aliases ns-form)]
    (when (:ok result)
      (some (fn [[alias required]] (when (= (str namespace) (str required)) alias))
            (:aliases result)))))
