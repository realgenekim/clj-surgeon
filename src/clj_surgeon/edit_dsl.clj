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
    inc dec max min mod quot rem zero? pos? neg? even? odd? number? integer?
    int? nat-int? pos-int? neg-int? ratio? rational? float? decimal?
    compare comparator hash
    -> ->> as-> some-> some->> cond-> cond->>
    if if-let if-some when when-not when-let when-some cond condp case
    let let* fn fn* quote do
    identity constantly comp complement partial juxt fnil every-pred some-fn
    apply
    vector vec list list* hash-map array-map sorted-map sorted-map-by
    hash-set sorted-set sorted-set-by set
    conj cons disj pop peek subvec concat into merge merge-with select-keys
    assoc assoc-in dissoc update update-in
    seq first ffirst nfirst second fnext next rest last butlast nth nthnext
    nthrest get get-in find contains? keys vals count empty empty? not-empty
    seq? sequential? associative? coll? counted? indexed? reversible? map?
    vector? set? list? map-entry?
    map mapv mapcat filter filterv remove keep keep-indexed map-indexed
    reduce reduce-kv reductions transduce sequence eduction
    take take-last take-nth take-while drop drop-last drop-while split-at
    split-with partition partition-by interleave interpose flatten distinct
    dedupe sort sort-by group-by frequencies zipmap
    some every? not-every? not-any?
    range
    str subs format name namespace keyword symbol simple-symbol?
    simple-keyword? qualified-symbol? qualified-keyword?
    pr-str print-str println-str
    meta with-meta vary-meta
    re-pattern re-find re-matches re-seq
    clojure.core/partition-all clojure.core/replace])

(def ^:private builder-symbols
  '[form match where right left up down outermost span partition-all replace
    replace-span transform xray])

(def ^:private allowed-symbols
  (vec (distinct (concat pure-core-symbols builder-symbols))))

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
  ["(form name)"
   "(match path pattern)"
   "(where path predicates)"
   "(right path)" "(left path)" "(up path)" "(down path)" "(outermost path)"
   "(span path n)" "(partition-all path n)"
   "(replace path form)" "(replace-span path & forms)"
   "(transform path pure-function)"
   "(xray path pure-function)"])

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
  "Start a query at the named top-level form."
  [name]
  [[:form name]])

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

(def ^:private sci-bindings
  {'form form
   'match match
   'where where
   'right right
   'left left
   'up up
   'down down
   'outermost outermost
   'span span
   'partition-all partition-all
   'replace replace
   'replace-span replace-span
   'transform transform
   'xray xray})

(defn- invalid-expression!
  ([expression reason]
   (invalid-expression! expression reason nil))
  ([expression reason cause]
   (throw (ex-info "Invalid Clojure edit expression"
                   {:error-type :invalid-edit-expression
                    :reason reason
                    :expression expression
                    :allowed-symbols allowed-symbols
                    :allowed-capabilities allowed-capabilities
                    :allowed-forms expression-reference
                    :remedy "Use one pure Clojure expression. A thread-first form and the allowed builders must return one query vector."}
                   cause))))

(defn- invalid-xray-expression!
  ([expression reason]
   (invalid-xray-expression! expression reason nil))
  ([expression reason cause]
   (throw (ex-info "Invalid Clojure xray expression"
                   {:error-type :invalid-xray-expression
                    :reason reason
                    :expression expression
                    :allowed-symbols allowed-symbols
                    :allowed-capabilities allowed-capabilities
                    :allowed-forms expression-reference
                    :remedy "Use one pure Clojure expression ending in (xray path pure-function). Run clj-surgeon :op :xray --help for the complete workflow."}
                   cause))))

(defn- evaluate-expression
  [expression invalid!]
  (when-not (string? expression)
    (invalid! expression :expression-must-be-string))
  (when (> (count expression) max-expression-characters)
    (invalid! expression :expression-too-large))
  (let [context (sci/init {:namespaces {'user sci-bindings}
                           :classes {}
                           :allow allowed-symbols})
        reader (sci/reader expression)
        user-ns (sci/create-ns 'user)]
    (try
      (let [form (sci/parse-next context reader)
            trailing (sci/parse-next context reader)]
        (when (or (= :sci.core/eof form)
                  (not= :sci.core/eof trailing))
          (invalid! expression :expected-one-form))
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

(defn compile-query
  "Compile one capability-limited Clojure expression into query-vector data."
  [expression]
  (let [query (evaluate-expression expression invalid-expression!)]
    (when-not (vector? query)
      (invalid-expression! expression :query-must-be-vector))
    query))

(defn compile-xray
  "Compile one capability-limited Clojure expression into an xray program."
  [expression]
  (let [program (evaluate-expression expression invalid-xray-expression!)]
    (when-not (and (map? program)
                   (= :xray (:kind program))
                   (vector? (:query program))
                   (fn? (:analyzer program)))
      (invalid-xray-expression! expression :xray-terminal-required))
    (assoc program :expression expression)))

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
  #{:op :file :expr :help})

(defn prepare-xray-options
  "Compile :expr before source I/O and reject unsupported arguments."
  [{:keys [file expr] :as opts}]
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
       :allowed (vec (sort xray-allowed-arguments))}

      (not (contains? opts :expr))
      {:operation :xray
       :file file
       :error "Supply :expr"
       :error-type :missing-xray-input
       :missing [:expr]}

      :else
      (try
        (-> opts
            (dissoc :expr)
            (assoc :operation :xray
                   :expression expr
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

(defn evaluate-xray
  "Evaluate one read-only xray program against source. Pure: data in, EDN out."
  [source {:keys [expression xray]}]
  (let [{:keys [query analyzer]} xray
        found (structural-lens/evaluate-query source query)
        evidence (-> found
                     (assoc :operation :xray
                            :expression expression
                            :xray {:input-shape :selected-values
                                   :input-count (:match-count found)}))]
    (cond
      (:error found)
      (assoc evidence :operation :xray :expression expression)

      (:matches-truncated? found)
      (xray-refusal evidence expression :xray-input-truncated
                    (str "X-ray selection has " (:match-count found)
                         " matches; narrow it to at most "
                         structural-lens/query-result-limit))

      :else
      (let [values (try
                     (mapv match-value (:matches found))
                     (catch Exception exception exception))]
        (if (instance? Exception values)
          (xray-refusal evidence expression :xray-input-invalid
                        (str "Selected syntax cannot become Clojure data: "
                             (.getMessage ^Exception values)))
          (let [value (try
                        (analyzer values)
                        (catch Exception exception exception))]
            (cond
              (instance? Exception value)
              (xray-refusal evidence expression :xray-analysis-failed
                            (str "Pure xray analysis failed: "
                                 (.getMessage ^Exception value)))

              (not (concrete-edn? value))
              (xray-refusal evidence expression :invalid-xray-result
                            "Pure xray analysis must return concrete EDN data")

              (> (count (pr-str value)) max-xray-result-characters)
              (xray-refusal evidence expression :xray-result-too-large
                            (str "Pure xray result exceeds "
                                 max-xray-result-characters " characters"))

              :else
              (assoc evidence :value value))))))))

(defn evaluate-edit
  "Materialize a pure transform against one selected form, then build a concrete plan."
  [source {:keys [file query] :as opts}]
  (let [terminal (when (vector? query) (peek query))]
    (if-not (and (vector? terminal) (= :transform (first terminal)))
      (structural-lens/evaluate-edit source opts)
      (let [selection-query (pop query)
            found (structural-lens/evaluate-query source selection-query)
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
