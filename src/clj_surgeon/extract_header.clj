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
  :minimal keeps only statically proved direct libspecs. :copy-all changes
  only the namespace name and preserves the complete source header exactly."
  [{:keys [source-ns-form target-ns form-sources require-policy]
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
                  renamed-ns (replace-namespace-name source-ns-form target-ns)]
              {:ok true
               :require-policy :minimal
               :ns-form (install-require-form renamed-ns retained)
               :target-requires (mapv (comp str :namespace) retained)
               :omitted-target-requires
               (mapv (comp str :namespace) omitted)})))))

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
