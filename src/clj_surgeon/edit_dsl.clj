(ns clj-surgeon.edit-dsl
  "Pure Clojure builders for clj-surgeon's existing structural query data."
  (:refer-clojure :exclude [partition-all replace])
  (:require
   [clojure.string :as str]
   [sci.core :as sci]))

(def ^:private terminal-steps
  #{:replace :replace-span})

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
  '[form match where right left up down span partition-all replace replace-span])

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
   "(right path)" "(left path)" "(up path)" "(down path)"
   "(span path n)" "(partition-all path n)"
   "(replace path form)" "(replace-span path & forms)"])

(def ^:private max-expression-characters 32768)

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

(def ^:private sci-bindings
  {'form form
   'match match
   'where where
   'right right
   'left left
   'up up
   'down down
   'span span
   'partition-all partition-all
   'replace replace
   'replace-span replace-span})

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

(defn compile-query
  "Compile one capability-limited Clojure expression into query-vector data."
  [expression]
  (when-not (string? expression)
    (invalid-expression! expression :expression-must-be-string))
  (when (> (count expression) max-expression-characters)
    (invalid-expression! expression :expression-too-large))
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
          (invalid-expression! expression :expected-one-form))
        (let [query (:val (sci/eval-string+ context expression {:ns user-ns}))]
          (when-not (vector? query)
            (invalid-expression! expression :query-must-be-vector))
          query))
      (catch Exception exception
        (if (= :invalid-edit-expression
               (:error-type (ex-data exception)))
          (throw exception)
          (invalid-expression! expression
                               (if (or (str/includes? (.getMessage exception)
                                                      "is not allowed")
                                       (str/includes? (.getMessage exception)
                                                      "Unable to resolve symbol"))
                                 :disallowed-symbol
                                 :evaluation-failed)
                               exception))))))

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
