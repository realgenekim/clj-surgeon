(ns clj-surgeon.edit-dsl
  "Pure Clojure builders for clj-surgeon's existing structural query data."
  (:refer-clojure :exclude [partition-all replace])
  (:require
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [rewrite-clj.zip :as z]
   [sci.core :as sci]))

(def ^:private terminal-steps
  #{:replace :replace-span :transform})

(def pure-core-symbols
  '[* + - / < <= = == > >=
    and or not boolean true? false? nil? some?
    bit-and bit-or bit-xor bit-not bit-shift-left bit-shift-right
    inc dec int max min mod quot rem zero? pos? neg? even? odd? number? integer?
    int? nat-int? pos-int? neg-int? ratio? rational? float? decimal?
    compare comparator hash
    -> ->> as-> some-> some->> cond-> cond->>
    if if-let if-some when when-not when-let when-some cond condp case for
    let let* fn fn* quote do
    identity constantly comp complement partial juxt fnil every-pred some-fn
    apply
    vector vec list list* hash-map array-map sorted-map sorted-map-by
    hash-set sorted-set sorted-set-by set
    conj cons disj pop peek subvec concat into merge merge-with select-keys
    assoc assoc-in dissoc update update-in
    seq first ffirst nfirst second fnext next rest last butlast nth nthnext key val
    nthrest get get-in find contains? keys vals count empty empty? not-empty
    seq? sequential? associative? coll? counted? indexed? reversible? map?
    vector? set? list? map-entry?
    map mapv mapcat filter filterv remove keep keep-indexed map-indexed
    reduce reduce-kv reductions transduce sequence eduction
    take take-last take-nth take-while drop drop-last drop-while split-at
    split-with partition partition-by interleave interpose flatten tree-seq distinct
    dedupe sort sort-by group-by frequencies zipmap
    some every? not-every? not-any?
    range
    str subs format name namespace keyword symbol simple-symbol?
    simple-keyword? qualified-symbol? qualified-keyword?
    pr-str print-str println-str
    meta with-meta vary-meta
    re-pattern re-find re-matches re-seq
    clojure.core/partition-all clojure.core/replace])

(def ^:private macro-expansion-symbols
  '[lazy-seq loop loop* recur unchecked-inc
    chunked-seq? chunk-first chunk-rest chunk-buffer chunk-append chunk chunk-cons])

(def ^:private builder-symbols
  '[form line match where right left up down outermost initializer span partition-all replace
    replace-span transform xray xray-one compute aggregate inspect one all
    expect-count analyze])

(def ^:private allowed-symbols
  (vec (distinct (concat pure-core-symbols builder-symbols))))

(def ^:private sci-allowed-symbols
  (vec (distinct (concat allowed-symbols macro-expansion-symbols))))

(def ^:private forbidden-source-symbols
  (set (map name macro-expansion-symbols)))

(defn- forbidden-source-symbol [form]
  (letfn [(walk [node]
            (cond
              (and (seq? node)
                   (#{'quote 'clojure.core/quote} (first node)))
              nil

              (and (symbol? node)
                   (contains? forbidden-source-symbols (name node)))
              node

              (coll? node)
              (some walk node)

              :else
              nil))]
    (walk form)))

(def ^:private allowed-capabilities
  [:pure-control
   :collection-construction
   :collection-navigation
   :collection-transformation
   :higher-order-functions
   :predicates
   :numbers-and-strings
   :metadata
   :structural-builders])

(def ^:private expression-reference
  ["(form name)" "(form name platform)" "(line positive-line)"
   "(match path pattern)"
   "(where path predicates)"
   "(right path)" "(left path)" "(up path)" "(down path)" "(outermost path)"
   "(initializer path)"
   "(span path n)" "(partition-all path n)"
   "(replace path form)" "(replace-span path & forms)"
   "(transform path pure-function)"
   "(xray path pure-function)"
   "(xray-one path pure-function)"])

(def ^:private xray-expression-reference
  (->> expression-reference
       (remove #(or (str/starts-with? % "(replace")
                    (str/starts-with? % "(transform")
                    (str/starts-with? % "(xray")))
       (into ["A path returns literal evidence."
              "(expect-count path n)"
              "(analyze path pure-function)"])))

(def ^:private max-expression-characters 32768)
(def ^:private max-xray-result-characters 65536)

(defn- terminal-step?
  [step]
  (and (vector? step)
       (contains? terminal-steps (first step))))

(defn- validate-path
  [path]
  (when-not (vector? path)
    (throw (ex-info "Edit path must be a vector"
                    {:error-type :invalid-edit-path
                     :path path})))
  (when (terminal-step? (peek path))
    (throw (ex-info "Cannot append after a terminal edit step"
                    {:error-type :terminal-edit-step
                     :terminal-step (peek path)})))
  path)

(defn- append-step
  [path step]
  (conj (validate-path path) step))

(defn- append-counted-step
  [path step value]
  (validate-path path)
  (when-not (pos-int? value)
    (throw (ex-info "Edit step requires a positive integer"
                    {:error-type :invalid-step-argument
                     :step step
                     :value value})))
  (conj path [step value]))

(defn form
  "Start a query at the named top-level form, optionally on one CLJC platform."
  ([name]
   [[:form name]])
  ([name platform]
   [[:form name platform]]))

(defn line
  "Start a query at the one top-level form containing a physical source line."
  [value]
  (when-not (pos-int? value)
    (throw (ex-info "Line root requires a positive integer"
                    {:error-type :invalid-line-root
                     :line value})))
  [[:line value]])

(defn match
  "Append an exact structural find step."
  [path pattern]
  (append-step path [:find pattern]))

(defn where
  "Append structural predicates that constrain the current match."
  [path predicates]
  (append-step path [:where predicates]))

(defn right
  "Move to the next structural sibling."
  [path]
  (append-step path :right))

(defn left
  "Move to the previous structural sibling."
  [path]
  (append-step path :left))

(defn up
  "Move to the structural parent."
  [path]
  (append-step path :up))

(defn down
  "Move to the first structural child."
  [path]
  (append-step path :down))

(defn outermost
  "Retain current nodes that have no current ancestor."
  [path]
  (append-step path :outermost))

(defn initializer
  "Select the initializer of each selected def without evaluating it."
  [path]
  (append-step path :initializer))

(defn span
  "Select the current form and the next n-1 structural siblings."
  [path n]
  (append-counted-step path :span n))

(defn partition-all
  "Group the current sibling sequence into structural groups of n."
  [path n]
  (append-counted-step path :partition-all n))

(defn replace
  "Append one terminal subtree replacement."
  [path replacement]
  (append-step path [:replace replacement]))

(defn replace-span
  "Append one terminal replacement for the selected structural span."
  [path & replacements]
  (append-step path (into [:replace-span] replacements)))

(defn transform
  "Derive one terminal replacement from the selected form's Clojure data."
  [path transformer]
  (when-not (ifn? transformer)
    (throw (ex-info "Edit transform must be a function"
                    {:error-type :invalid-edit-transform
                     :transform transformer})))
  (append-step path [:transform transformer]))

(defn xray
  "Compute a read-only EDN value from the selected Clojure values."
  [path analyzer]
  (try
    (validate-path path)
    (catch Exception exception
      (throw (ex-info "X-ray path must be a read-only query vector"
                      {:error-type :invalid-xray-path
                       :path path}
                      exception))))
  (when-not (fn? analyzer)
    (throw (ex-info "X-ray analyzer must be a function"
                    {:error-type :invalid-xray-analyzer
                     :analyzer analyzer})))
  {:kind :xray
   :query path
   :analyzer analyzer})

(defn xray-one
  "Compute from exactly one selected Clojure value; refuse zero or many."
  [path analyzer]
  (assoc (xray path analyzer)
         :cardinality :one
         :input-shape :selected-value))

(defn compute
  "Compute from exactly one selected value. Primary X-ray terminal."
  [path analyzer]
  (xray-one path analyzer))

(defn aggregate
  "Compute across zero, one, or many selected values."
  [path analyzer]
  (xray path analyzer))

(defn inspect
  "Inspect exactly one selected value or all selected values."
  [path cardinality analyzer]
  (case cardinality
    :one (xray-one path analyzer)
    :all (xray path analyzer)
    (throw (ex-info "Inspect cardinality must be :one or :all"
                    {:error-type :invalid-xray-cardinality
                     :cardinality cardinality
                     :allowed [:one :all]}))))

(defn one
  "Analyze exactly one selected value; refuse zero or many."
  [path analyzer]
  (xray-one path analyzer))

(defn all
  "Analyze all selected values as a vector in source order."
  [path analyzer]
  (xray path analyzer))

(defn expect-count
  "Refine a selection path with one exact, non-negative match count."
  [path n]
  (validate-path path)
  (when-not (and (integer? n) (not (neg? n)))
    (throw (ex-info "Expected match count must be a non-negative integer"
                    {:error-type :invalid-xray-cardinality
                     :expected-count n})))
  {:kind :selection
   :query path
   :expected-count n})

(defn analyze
  "Analyze a stable vector of selected values, optionally count-refined."
  [selection analyzer]
  (when-not (fn? analyzer)
    (throw (ex-info "X-ray analyzer must be a function"
                    {:error-type :invalid-xray-analyzer})))
  (let [{:keys [query expected-count]}
        (cond
          (vector? selection) {:query (validate-path selection)}
          (and (map? selection)
               (= :selection (:kind selection))
               (vector? (:query selection))) selection
          :else (throw (ex-info "Analyze input must be a structural path"
                                {:error-type :invalid-xray-path})))]
    {:kind :xray
     :query query
     :analyzer analyzer
     :expected-count expected-count
     :input-shape :selected-values}))

(def ^:private sci-bindings
  {'form form
   'line line
   'match match
   'where where
   'right right
   'left left
   'up up
   'down down
   'outermost outermost
   'initializer initializer
   'span span
   'partition-all partition-all
   'replace replace
   'replace-span replace-span
   'transform transform
   'xray xray
   'xray-one xray-one
   'compute compute
   'aggregate aggregate
   'inspect inspect
   'one one
   'all all
   'expect-count expect-count
   'analyze analyze})

(defn- invalid-expression!
  ([expression reason]
   (invalid-expression! expression reason nil))
  ([expression reason cause]
   (let [symbol (:symbol (ex-data cause))]
     (throw (ex-info "Invalid Clojure edit expression"
                     (cond-> {:error-type :invalid-edit-expression
                              :reason reason
                              :expression expression
                              :allowed-symbols allowed-symbols
                              :allowed-capabilities allowed-capabilities
                              :allowed-forms expression-reference
                              :remedy "Use one pure Clojure expression. A thread-first form and the allowed builders must return one query vector."}
                       symbol (assoc :symbol symbol
                                     :remedy (str "Use one pure Clojure expression. A thread-first form and the allowed builders must return one query vector. "
                                                  "Do not execute " symbol
                                                  "; quote it when it is Clojure data, or use a terminating pure collection operation for computation.")))
                     cause)))))

(defn- invalid-xray-expression!
  ([expression reason]
   (invalid-xray-expression! expression reason nil))
  ([expression reason cause]
   (let [symbol (:symbol (ex-data cause))]
     (throw (ex-info "Invalid Clojure xray expression"
                     (cond-> {:error-type :invalid-xray-expression
                              :reason reason
                              :expression expression
                              :allowed-symbol-count (count allowed-symbols)
                              :allowed-capabilities allowed-capabilities
                              :allowed-forms xray-expression-reference
                              :usage "clj-surgeon :op :xray :file FILE :expr EXPR"
                              :remedy "Return a path, or end with analyze; add expect-count for exact cardinality."}
                       symbol (assoc :symbol symbol
                                     :remedy (str "Return a path, or end with analyze; add expect-count for exact cardinality. "
                                                  "Do not execute " symbol
                                                  "; quote it when it is Clojure data, or use a terminating pure collection operation for computation.")))
                     cause)))))

(defn- evaluate-expression
  [expression invalid!]
  (when-not (string? expression)
    (invalid! expression :expression-must-be-string))
  (when (> (count expression) max-expression-characters)
    (invalid! expression :expression-too-large))
  (let [context (sci/init {:namespaces {'user sci-bindings}
                           :classes {}
                           :allow sci-allowed-symbols})
        reader (sci/reader expression)
        user-ns (sci/create-ns 'user)]
    (try
      (let [form (sci/parse-next context reader)
            trailing (sci/parse-next context reader)]
        (when (or (= :sci.core/eof form)
                  (not= :sci.core/eof trailing))
          (invalid! expression :expected-one-form))
        (when-let [symbol (forbidden-source-symbol form)]
          (invalid! expression :disallowed-symbol
                    (ex-info "Macro-expansion-only symbol used as executable source"
                             {:symbol symbol})))
        (:val (sci/eval-string+ context expression {:ns user-ns})))
      (catch Exception exception
        (if (#{:invalid-edit-expression :invalid-xray-expression}
             (:error-type (ex-data exception)))
          (throw exception)
          (invalid! expression
                    (cond
                      (#{:invalid-xray-path :invalid-xray-analyzer}
                       (:error-type (ex-data exception)))
                      (:error-type (ex-data exception))

                      (or (str/includes? (.getMessage exception)
                                         "is not allowed")
                          (str/includes? (.getMessage exception)
                                         "Unable to resolve symbol"))
                      :disallowed-symbol

                      :else :evaluation-failed)
                    exception))))))

(defn- zipper-children
  [zloc]
  (when-let [child (z/down zloc)]
    (->> (iterate z/right child)
         (take-while some?)
         vec)))

(defn- quote-list?
  [zloc]
  (and (z/list? zloc)
       (#{'quote 'clojure.core/quote}
        (some-> zloc z/down z/sexpr))))

(defn- inside-quote?
  [zloc]
  (loop [ancestor (z/up zloc)]
    (cond
      (nil? ancestor) false
      (or (#{:quote :syntax-quote} (z/tag ancestor))
          (quote-list? ancestor)) true
      :else (recur (z/up ancestor)))))

(defn- replacement-call?
  [zloc operation]
  (let [head (when (z/list? zloc)
               (some-> zloc z/down z/sexpr))]
    (and (symbol? head)
         (= (name operation) (name head))
         (not (inside-quote? zloc)))))

(defn- unquoted-source
  [zloc]
  (cond
    (= :quote (z/tag zloc))
    (some-> zloc z/down z/string)

    (quote-list? zloc)
    (let [children (zipper-children zloc)]
      (when (= 2 (count children))
        (z/string (second children))))

    :else
    (z/string zloc)))

(defn- parse-one-sci-form
  [source]
  (try
    (let [context (sci/init {:classes {}})
          reader (sci/reader source)
          form (sci/parse-next context reader)
          trailing (sci/parse-next context reader)]
      (when (and (not= :sci.core/eof form)
                 (= :sci.core/eof trailing))
        form))
    (catch Exception _
      nil)))

(defn- literal-replacement-sources
  [expression query]
  (try
    (let [terminal (when (vector? query) (peek query))
          operation (when (and (vector? terminal)
                               (#{:replace :replace-span} (first terminal)))
                      (first terminal))
          replacements (when operation (vec (rest terminal)))]
      (when (seq replacements)
        (let [root (z/of-string expression)
              call (->> (iterate z/next root)
                        (take-while (complement z/end?))
                        (filter #(replacement-call? % operation))
                        last)
              arguments (some-> call zipper-children rest vec)
              raw-sources (when (<= (count replacements) (count arguments))
                            (->> arguments
                                 (take-last (count replacements))
                                 (mapv unquoted-source)))]
          (when (= (count replacements) (count raw-sources))
            (mapv (fn [replacement source]
                    (when (= replacement (parse-one-sci-form source))
                      source))
                  replacements
                  raw-sources)))))
    (catch Exception _
      nil)))

(defn compile-query
  "Compile one capability-limited Clojure expression into query-vector data."
  [expression]
  (let [query (evaluate-expression expression invalid-expression!)]
    (when-not (vector? query)
      (invalid-expression! expression :query-must-be-vector))
    (if-let [sources (literal-replacement-sources expression query)]
      (vary-meta query assoc
                 ::structural-lens/replacement-sources sources
                 ::structural-lens/replacement-values (vec (rest (peek query))))
      query)))

(defn compile-xray
  "Compile one pure path or terminal computation into an X-ray program."
  [expression]
  (let [program (evaluate-expression expression invalid-xray-expression!)]
    (cond
      (vector? program)
      (do
        (when (terminal-step? (peek program))
          (invalid-xray-expression! expression :xray-terminal-required))
        {:kind :literal
         :query program
         :expression expression})

      (and (map? program)
           (= :xray (:kind program))
           (vector? (:query program))
           (fn? (:analyzer program)))
      (assoc program :expression expression)

      :else
      (invalid-xray-expression!
        expression :xray-expression-must-return-path-or-computation))))

(defn prepare-edit-options
  "Compile :expr into :query or return a structured one-of-input refusal."
  [{:keys [file expr] :as opts}]
  (let [provided (->> [:expr :query]
                      (filter #(contains? opts %))
                      vec)]
    (cond
      (= 2 (count provided))
      {:operation :edit
       :file file
       :error "Supply exactly one of :query and :expr"
       :error-type :edit-input-conflict
       :provided provided
       :required-one-of [:query :expr]}

      (empty? provided)
      {:operation :edit
       :file file
       :error "Supply exactly one of :query and :expr"
       :error-type :missing-edit-input
       :provided provided
       :required-one-of [:query :expr]}

      (= [:expr] provided)
      (try
        (-> opts
            (dissoc :expr)
            (assoc :query (compile-query expr)))
        (catch Exception exception
          (assoc (ex-data exception)
                 :operation :edit
                 :file file
                 :error (.getMessage exception))))

      :else opts)))

(def ^:private xray-allowed-arguments
  #{:op :file :expr :evidence :help})

(defn prepare-xray-options
  "Compile :expr before source I/O and reject unsupported arguments."
  [{:keys [file expr evidence] :as opts}]
  (let [unsupported (->> (keys opts)
                         (remove xray-allowed-arguments)
                         sort
                         vec)]
    (cond
      (seq unsupported)
      {:operation :xray
       :file file
       :error (str "Unsupported arguments for :xray: "
                   (str/join ", " (map #(str ":" (name %)) unsupported)))
       :error-type :unsupported-arguments
       :unsupported unsupported
       :allowed (vec (sort xray-allowed-arguments))
       :usage "clj-surgeon :op :xray :file FILE :expr \"(-> (form 'NAME) (expect-count 1) (analyze pure-function))\""}

      (not (contains? opts :expr))
      {:operation :xray
       :file file
       :error "Supply :expr"
       :error-type :missing-xray-input
       :missing [:expr]
       :usage "clj-surgeon :op :xray :file FILE :expr \"(-> (form 'NAME) (expect-count 1) (analyze pure-function))\""}

      (and (contains? opts :evidence)
           (not (#{:compact :full} evidence)))
      {:operation :xray
       :file file
       :error ":evidence must be :compact or :full"
       :error-type :invalid-evidence-mode
       :evidence evidence
       :allowed [:compact :full]
       :usage "clj-surgeon :op :xray :file FILE :expr EXPR :evidence :full"}

      :else
      (try
        (-> opts
            (dissoc :expr)
            (assoc :operation :xray
                   :expression expr
                   :evidence (or evidence :compact)
                   :xray (compile-xray expr)))
        (catch Exception exception
          (assoc (ex-data exception)
                 :operation :xray
                 :file file
                 :error (.getMessage exception)))))))

(defn- source-value
  [source]
  (z/sexpr (z/of-string source)))

(defn- match-value
  [{:keys [forms source]}]
  (if forms
    (mapv source-value forms)
    (source-value source)))

(def ^:private canonical-map-constructors
  '#{hash-map clojure.core/hash-map array-map clojure.core/array-map})

(defn- canonical-analysis-value
  "Normalize known map-shaped syntax without evaluating any source form."
  [value]
  (if (and (list? value)
           (contains? canonical-map-constructors (first value)))
    (let [arguments (rest value)]
      (when (odd? (count arguments))
        (throw (ex-info "Map constructor syntax requires key/value pairs"
                        {:error-type :invalid-map-constructor-syntax
                         :constructor (first value)
                         :argument-count (count arguments)})))
      (apply array-map arguments))
    value))

(defn- concrete-edn?
  [value]
  (cond
    (or (nil? value)
        (boolean? value)
        (char? value)
        (string? value)
        (symbol? value)
        (keyword? value)
        (number? value)) true
    (vector? value) (every? concrete-edn? value)
    (list? value) (every? concrete-edn? value)
    (set? value) (every? concrete-edn? value)
    (map? value) (every? (fn [[key item]]
                           (and (concrete-edn? key)
                                (concrete-edn? item)))
                         value)
    :else false))

(defn- xray-refusal
  [found expression error-type error]
  (-> found
      (assoc :operation :xray
             :expression expression
             :error-type error-type
             :error error)
      (dissoc :value)))

(defn- compact-match
  [match]
  (-> (select-keys match [:path :tag :address :line :end-line :count
                          :partition :inside])
      (assoc :source-hash (structural-lens/source-hash (:source match)))))

(defn- xray-evidence
  [found mode]
  (if (= :full mode)
    found
    (let [sources (mapv :source (:matches found))]
      (cond-> (update found :matches #(mapv compact-match %))
        (not (:matches-truncated? found))
        (assoc :selection-hash
               (structural-lens/source-hash (pr-str sources)))))))

(defn- evaluate-computed-xray
  "Evaluate one read-only xray program against source. Pure: data in, EDN out."
  [source {:keys [expression file xray evidence]}]
  (let [{:keys [query analyzer cardinality input-shape expected-count]} xray
        found (structural-lens/evaluate-query source query {:file file})
        evidence-mode (or evidence :compact)
        evidence (-> (xray-evidence found evidence-mode)
                     (assoc :operation :xray
                            :expression expression
                            :xray {:input-shape (or input-shape
                                                    :selected-values)
                                   :input-count (:match-count found)
                                   :data-view :canonical-collections
                                   :cardinality (cond
                                                  expected-count [:exactly expected-count]
                                                  cardinality cardinality
                                                  :else :any)
                                   :evidence evidence-mode}))]
    (cond
      (:error found)
      (assoc evidence :operation :xray :expression expression)

      (:matches-truncated? found)
      (xray-refusal evidence expression :xray-input-truncated
                    (str "X-ray selection has " (:match-count found)
                         " matches; narrow it to at most "
                         structural-lens/query-result-limit))

      (and expected-count
           (not= expected-count (:match-count found)))
      (-> (xray-refusal evidence expression :xray-cardinality-mismatch
                        (str "X-ray expected exactly " expected-count
                             " matches, found " (:match-count found)))
          (assoc :expected-match-count expected-count
                 :actual-match-count (:match-count found)))

      (and (= :one cardinality)
           (not= 1 (:match-count found)))
      (-> (xray-refusal evidence expression :xray-cardinality-mismatch
                        (str "X-ray expected exactly one match, found "
                             (:match-count found)))
          (assoc :expected-match-count 1
                 :actual-match-count (:match-count found)))

      :else
      (let [values (try
                     (mapv #(canonical-analysis-value (match-value %))
                           (:matches found))
                     (catch Exception exception exception))]
        (if (instance? Exception values)
          (xray-refusal evidence expression :xray-input-invalid
                        (str "Selected syntax cannot become Clojure data: "
                             (.getMessage ^Exception values)))
          (let [analyzer-input (if (= :one cardinality)
                                 (first values)
                                 values)
                value (try
                        (analyzer analyzer-input)
                        (catch Exception exception exception))]
            (cond
              (instance? Exception value)
              (-> (xray-refusal evidence expression :xray-analysis-failed
                                (str "Pure xray analysis failed: "
                                     (.getMessage ^Exception value)))
                  (assoc :remedy (str "Selected values are parsed syntax, not "
                                      "evaluated program state. Run the path "
                                      "without its terminal to read the source.")))

              (not (concrete-edn? value))
              (-> (xray-refusal evidence expression :invalid-xray-result
                                "Pure xray analysis must return concrete EDN data")
                  (assoc :remedy (str "Return concrete EDN. Realize lazy results "
                                      "with vec or another collection constructor.")))

              (> (count (pr-str value)) max-xray-result-characters)
              (xray-refusal evidence expression :xray-result-too-large
                            (str "Pure xray result exceeds "
                                 max-xray-result-characters " characters"))

              :else
              (assoc evidence :value value))))))))

(defn evaluate-xray
  "Evaluate a literal structural path or a terminal pure computation."
  [source {:keys [expression file xray] :as opts}]
  (if (= :literal (:kind xray))
    (-> (structural-lens/evaluate-query source (:query xray) {:file file})
        (assoc :operation :xray
               :mode :literal
               :expression expression))
    (assoc (evaluate-computed-xray source opts) :mode :computed)))

(defn evaluate-edit
  "Materialize a pure transform against one selected form, then build a concrete plan."
  [source {:keys [file query] :as opts}]
  (let [terminal (when (vector? query) (peek query))]
    (if-not (and (vector? terminal) (= :transform (first terminal)))
      (structural-lens/evaluate-edit source opts)
      (let [selection-query (pop query)
            found (structural-lens/evaluate-query source selection-query
                                                  {:file file})
            match-count (:match-count found)]
        (cond
          (:error found) found

          (not= 1 match-count)
          (assoc found
                 :error (str "Expected exactly one match, found " match-count)
                 :error-type (if (zero? match-count)
                               :no-match
                               :ambiguous-match))

          (not= 2 (count terminal))
          (assoc found
                 :error "Transform requires exactly one pure function"
                 :error-type :invalid-edit-transform)

          (not (ifn? (second terminal)))
          (assoc found
                 :error "Transform requires a pure function"
                 :error-type :invalid-edit-transform)

          :else
          (let [matched (first (:matches found))]
            (try
              (let [before (z/sexpr (z/of-string (:source matched)))
                    after ((second terminal) before)
                    concrete-query (conj selection-query [:replace after])]
                (structural-lens/evaluate-edit source
                                               (assoc opts :query concrete-query)))
              (catch Exception exception
                {:operation :edit
                 :file file
                 :query selection-query
                 :source-hash (:source-hash found)
                 :match-count match-count
                 :error (str "Pure edit transform failed: " (.getMessage exception))
                 :error-type :edit-transform-failed}))))))))
