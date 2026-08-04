(ns clj-surgeon.edit-dsl
  "Pure Clojure builders for clj-surgeon's existing structural query data."
  (:refer-clojure :exclude [partition-all replace]))

(def ^:private terminal-steps
  #{:replace :replace-span})

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
