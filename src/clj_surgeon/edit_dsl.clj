(ns clj-surgeon.edit-dsl
  "Pure Clojure builders for clj-surgeon's existing structural query data."
  (:refer-clojure :exclude [partition-all replace])
  (:require [sci.core :as sci]))

(def ^:private terminal-steps
  #{:replace :replace-span})

(def ^:private allowed-symbols
  '[-> quote form match where right left up down span partition-all replace
    replace-span])

(def ^:private allowed-call-symbols
  (set (remove #{'quote} allowed-symbols)))

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
                    :allowed-forms expression-reference
                    :remedy "Use one thread-first expression made only from the allowed forms."}
                   cause))))

(declare validate-expression-form!)

(defn- validate-sequential-values!
  [expression values]
  (doseq [value values]
    (validate-expression-form! expression value)))

(defn- validate-expression-form!
  [expression form]
  (cond
    (seq? form)
    (let [operator (first form)]
      (cond
        (= 'quote operator)
        (when-not (= 2 (count form))
          (invalid-expression! expression :unsupported-form))

        (contains? allowed-call-symbols operator)
        (validate-sequential-values! expression (rest form))

        :else
        (invalid-expression! expression :unsupported-form)))

    (symbol? form)
    (when-not (contains? allowed-call-symbols form)
      (invalid-expression! expression :unsupported-symbol))

    (map? form)
    (validate-sequential-values! expression (mapcat identity form))

    (coll? form)
    (validate-sequential-values! expression form))
  form)

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
        reader (sci/reader expression)]
    (try
      (let [form (sci/parse-next context reader)
            trailing (sci/parse-next context reader)]
        (when (or (= :sci.core/eof form)
                  (not= :sci.core/eof trailing))
          (invalid-expression! expression :expected-one-form))
        (validate-expression-form! expression form)
        (let [query (sci/eval-form context form)]
          (when-not (vector? query)
            (invalid-expression! expression :query-must-be-vector))
          query))
      (catch Exception exception
        (if (= :invalid-edit-expression
               (:error-type (ex-data exception)))
          (throw exception)
          (invalid-expression! expression :evaluation-failed exception))))))
