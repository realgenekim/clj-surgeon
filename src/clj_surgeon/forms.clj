(ns clj-surgeon.forms
  "Single source of truth for defining-form classification.

   Every operation — :ls, :deps, :mv, :extract, :fix-declares, :topo —
   uses these predicates to decide what counts as a definition, what has
   arglists, and what is private.

   To add support for a custom macro (e.g. mu/defn, defendpoint):
   - If the local name after / matches a core form, it works automatically.
     (mu/defn -> 'defn' -> :defn)
   - Otherwise, add one entry to `explicit-aliases`."
  (:require [clojure.string :as str]))

;; ============================================================
;; Core defining forms — these map to themselves
;; ============================================================

(def core-forms
  "Standard Clojure defining forms, keyed by their string name."
  {"def"         :def
   "defn"        :defn
   "defn-"       :defn-
   "defonce"     :defonce
   "defmacro"    :defmacro
   "defmethod"   :defmethod
   "defmulti"    :defmulti
   "defprotocol" :defprotocol
   "defrecord"   :defrecord
   "deftype"     :deftype
   "declare"     :declare
   "deftest"     :deftest})

;; ============================================================
;; Explicit aliases — non-standard names that can't be auto-detected
;; from the local part after /
;; ============================================================

(def explicit-aliases
  "Custom defining forms whose local name doesn't match a core form.
   Add project-specific macros here (one line each)."
  {">defn"  :defn
   ">defn-" :defn-})

;; ============================================================
;; Classification: the one function everything else calls
;; ============================================================

(defn classify
  "Classify a form's type-str into a canonical kind (:defn, :defn-, :def, etc.)
   or nil if it's not a defining form.

   Resolution order:
   1. Exact match against core Clojure forms
   2. Exact match against explicit aliases (>defn, defendpoint, etc.)
   3. Namespace-qualified: split on / and match local part against core forms
      (mu/defn -> 'defn' -> :defn, works for any alias)"
  [type-str]
  (when type-str
    (or (core-forms type-str)
        (explicit-aliases type-str)
        (when-let [idx (str/index-of type-str "/")]
          (core-forms (subs type-str (inc idx)))))))

;; ============================================================
;; Derived predicates — used across outline, analyze, extract
;; ============================================================

(defn defining-form?
  "Is this type-str a recognized defining form?"
  [type-str]
  (some? (classify type-str)))

(defn private-form?
  "Is this a private defining form (defn-, >defn-, mu/defn-, etc.)?"
  [type-str]
  (= :defn- (classify type-str)))

(defn has-arglists?
  "Does this form type have an arglist vector? (defn, defn-, >defn, mu/defn, etc.)"
  [type-str]
  (#{:defn :defn-} (classify type-str)))
