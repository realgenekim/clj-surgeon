(ns clj-surgeon.fields
  "Standard library of field-extractor functions used in `.clj-surgeon.edn`.

   Each extractor is a function `(zloc) -> value-or-nil`. The `zloc` points
   at the top-level form (a list whose head is the macro symbol). Extractors
   return either:
   - a Clojure value (symbol, keyword, string, vector, map) for structured
     output — preferred when possible
   - a string (from `z/string`) for opaque source — used for arglists where
     we want to preserve the literal token sequence

   All extractors are pure: same zloc + same source = same output. Meta
   wrappers (`^Tag [x]`) are unwrapped wherever a value is returned.

   Compose with normal Clojure: `(fn [z] (-> z ->defn-name ...))` or call
   directly from user `.clj-surgeon.edn` `:fields` map:

       {\"defenterprise\"
        {:fields {:name      ->defn-name
                  :docstring ->defn-docstring
                  :arglist   ->defn-arg-list}}}"
  (:require [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]
            [clojure.string :as str]))

;; ============================================================
;; Internal helpers
;; ============================================================

(defn unwrap-meta
  "If `zloc` is wrapped in `:meta`, descend to its rightmost child (the
   underlying value). Otherwise return `zloc` unchanged. nil-safe."
  [zloc]
  (if (and zloc (= :meta (some-> zloc z/node n/tag)))
    (some-> zloc z/down z/rightmost)
    zloc))

(defn- collapse-ws [s]
  (when s (str/replace s #"\s+" " ")))

(defn- safe-sexpr [zloc]
  (when zloc
    (try (z/sexpr zloc) (catch Exception _ nil))))

(defn- string-literal? [zloc]
  (when zloc
    (let [tag (some-> zloc z/node n/tag)]
      (or (= :multi-line tag)
          (and (= :token tag)
               (let [s (z/string zloc)]
                 (and (.startsWith s "\"")
                      (.endsWith s "\""))))))))

(defn- vector-node? [zloc]
  (and zloc (= :vector (some-> zloc z/node n/tag))))

(defn- symbol-value? [zloc]
  (when zloc (symbol? (safe-sexpr zloc))))

(defn- keyword-value? [zloc]
  (when zloc (keyword? (safe-sexpr zloc))))

(defn- string-value? [zloc]
  (when (string-literal? zloc)
    (try (string? (z/sexpr zloc)) (catch Exception _ false))))

;; ============================================================
;; Positional accessors
;; ============================================================

(defn ->nth-child
  "Return a zloc-extractor for the nth direct child of the form.
   0 = the macro symbol itself; 1 = name slot; 2 = next slot; …
   Meta-unwraps. Returns nil if no such child."
  [n]
  (fn [zloc]
    (loop [child (some-> zloc z/down)
           i 0]
      (when child
        (if (= i n)
          (unwrap-meta child)
          (recur (z/right child) (inc i)))))))

;; ============================================================
;; defn-shape extractors — what Metabase / Malli / Guardrails want
;; ============================================================

(defn ->defn-name
  "Returns the form's name as a symbol (e.g. `'enable-custom-viz?`).
   For `(defn foo …)` returns `'foo`. Meta-unwraps so `^:private foo`
   resolves to `foo`."
  [zloc]
  (some-> zloc z/down z/right unwrap-meta safe-sexpr))

(defn ->defn-arg-list
  "Returns the first vector child as a source string (e.g. `\"[a b]\"`).
   Meta-tagged arglists (`^String [a]`) unwrap to the inner vector.
   Returns nil if no vector child. Whitespace inside the arglist is
   collapsed (newlines → spaces) for one-line :ls output."
  [zloc]
  (loop [child (some-> zloc z/down)]
    (when child
      (let [unwrapped (unwrap-meta child)]
        (if (vector-node? unwrapped)
          (collapse-ws (z/string unwrapped))
          (recur (z/right child)))))))

(defn ->defn-docstring
  "Returns the form's docstring if the 3rd child is a string literal,
   else nil. Skips metadata on the name slot."
  [zloc]
  (let [c (some-> zloc z/down z/right z/right)]
    (when (string-value? (unwrap-meta c))
      (safe-sexpr (unwrap-meta c)))))

;; ============================================================
;; Type-predicate finders
;; ============================================================

(defn ->first-keyword
  "First direct child that's a keyword token, after meta-unwrap. Returns
   the keyword value (e.g. `:get`), or nil."
  [zloc]
  (loop [child (some-> zloc z/down)]
    (when child
      (let [u (unwrap-meta child)]
        (if (keyword-value? u)
          (safe-sexpr u)
          (recur (z/right child)))))))

(defn ->first-string
  "First direct child that's a string literal. Returns the unquoted string
   value (e.g. `\"/:id\"`), or nil."
  [zloc]
  (loop [child (some-> zloc z/down)]
    (when child
      (let [u (unwrap-meta child)]
        (if (string-value? u)
          (safe-sexpr u)
          (recur (z/right child)))))))

(defn ->first-symbol
  "First direct child that's a symbol, after meta-unwrap. Returns the
   symbol value, or nil. Skips the macro head (position 0)."
  [zloc]
  (loop [child (some-> zloc z/down z/right)] ;; start at position 1
    (when child
      (let [u (unwrap-meta child)]
        (if (symbol-value? u)
          (safe-sexpr u)
          (recur (z/right child)))))))

(defn ->first-vector
  "First direct child that's a vector, after meta-unwrap. Returns the
   vector's source string (newlines collapsed), or nil."
  [zloc]
  (loop [child (some-> zloc z/down)]
    (when child
      (let [u (unwrap-meta child)]
        (if (vector-node? u)
          (collapse-ws (z/string u))
          (recur (z/right child)))))))

;; ============================================================
;; Public set of names exposed to .clj-surgeon.edn via SCI
;; ============================================================

(def public
  "Map of symbol -> fn for SCI binding. Each entry is callable from
   a `.clj-surgeon.edn` `:fields` value as a bare symbol."
  {'->nth-child       ->nth-child
   '->defn-name       ->defn-name
   '->defn-arg-list   ->defn-arg-list
   '->defn-docstring  ->defn-docstring
   '->first-keyword   ->first-keyword
   '->first-string    ->first-string
   '->first-symbol    ->first-symbol
   '->first-vector    ->first-vector
   ;; Aliases for brevity
   '->name            ->defn-name
   '->arg-list        ->defn-arg-list
   '->docstring       ->defn-docstring
   ;; Pass-through helpers users might want in inline fns
   'unwrap-meta       unwrap-meta})
