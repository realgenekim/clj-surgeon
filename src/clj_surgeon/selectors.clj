(ns clj-surgeon.selectors
  "Selector DSL for extracting named fields from custom defining-form macros
   declared in `.clj-surgeon.edn`.

   A selector is an EDN vector of the form `[op & args]`. Selectors compose:
   nested operator forms run as sub-selectors. See docs/field-extraction-dsl.md
   for the design and rationale.

   Public:
   - `resolve-fields` — given a form zloc and a `:fields` map, return a map
     of field-key -> extracted-value.
   - `field-order` — given a `:fields` map, return the topo-sorted field-key
     sequence (so anchored selectors can reference already-resolved fields).
   - `validate-spec` — check a field-spec at config-load; throws on bad ops
     or unknown field refs."
  (:require [clojure.set :as set]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]))

;; ============================================================
;; Meta-unwrap: every terminal selector strips :meta wrappers
;; so author-level type hints (^String [a]) don't leak through.
;; ============================================================

(defn- unwrap-meta
  "If zloc is wrapped in :meta, descend to its rightmost child. Otherwise
   return zloc unchanged. nil-safe."
  [zloc]
  (if (and zloc (= :meta (some-> zloc z/node n/tag)))
    (some-> zloc z/down z/rightmost)
    zloc))

;; ============================================================
;; Type predicates — match selector :type keywords
;; ============================================================

(defn- vector-node? [zloc]
  (and zloc (= :vector (some-> zloc z/node n/tag))))

(defn- map-node? [zloc]
  (and zloc (= :map (some-> zloc z/node n/tag))))

(defn- list-node? [zloc]
  (and zloc (= :list (some-> zloc z/node n/tag))))

(defn- string-literal? [zloc]
  (when zloc
    (let [tag (some-> zloc z/node n/tag)]
      (or (= :multi-line tag)
          (and (= :token tag)
               (let [s (z/string zloc)]
                 (and (.startsWith s "\"")
                      (.endsWith s "\""))))))))

(defn- read-token [zloc]
  (when zloc
    (try (read-string (z/string zloc))
         (catch Exception _ nil))))

(defn- symbol-node? [zloc]
  (symbol? (read-token zloc)))

(defn- keyword-node? [zloc]
  (keyword? (read-token zloc)))

(def ^:private type-pred
  {:vector  vector-node?
   :map     map-node?
   :list    list-node?
   :string  string-literal?
   :symbol  symbol-node?
   :keyword keyword-node?
   :any     (fn [z] (some? z))})

(defn- check-type
  "If selector :type matches zloc, return zloc; else nil."
  [zloc type-kw]
  (when-let [pred (type-pred type-kw)]
    (when (pred zloc) zloc)))

;; ============================================================
;; Selector ops — each takes (form-zloc op-args resolved) -> zloc-or-value
;; resolved is a map field-key -> {:zloc z :value v} for prior fields.
;; ============================================================

(defn- op-nth
  "Direct child at index n (0 = the macro symbol itself)."
  [form-zloc [n] _resolved]
  (loop [child (some-> form-zloc z/down)
         i 0]
    (when child
      (if (= i n)
        (unwrap-meta child)
        (recur (z/right child) (inc i))))))

(defn- op-find-first
  "Scan direct children left-to-right, return first one matching type."
  [form-zloc [type-kw] _resolved]
  (loop [child (some-> form-zloc z/down)]
    (when child
      (let [u (unwrap-meta child)]
        (if (check-type u type-kw)
          u
          (recur (z/right child)))))))

(defn- op-right-of
  "Return the next sibling immediately after a previously-resolved field's
   zloc. Skips any :meta wrapping on the result."
  [_form-zloc [field-key] resolved]
  (when-let [{:keys [zloc]} (get resolved field-key)]
    (unwrap-meta (z/right zloc))))

(defn- op-left-of
  "Mirror of :right-of."
  [_form-zloc [field-key] resolved]
  (when-let [{:keys [zloc]} (get resolved field-key)]
    (unwrap-meta (z/left zloc))))

(defn- op-find-first-after
  "First child matching `type` that appears AFTER a previously-resolved
   field's zloc. If the anchor didn't resolve, fall back to scanning from
   the start (same as :find-first)."
  [form-zloc [field-key type-kw] resolved]
  (let [anchor (some-> resolved (get field-key) :zloc)
        start (if anchor (z/right anchor) (some-> form-zloc z/down))]
    (loop [child start]
      (when child
        (let [u (unwrap-meta child)]
          (if (check-type u type-kw)
            u
            (recur (z/right child))))))))

(declare run-selector)

(defn- op-when-type
  "Run inner selector, return result only if its type matches."
  [form-zloc [type-kw inner] resolved]
  (let [z (run-selector form-zloc inner resolved)]
    (check-type z type-kw)))

(defn- op-literal
  "Constant string value. Returns a synthetic 'value-only' marker — no zloc."
  [_form-zloc [v] _resolved]
  {:value (str v) :no-zloc true})

(defn- op-join
  "String-join previously-resolved field values with sep. Each ref is a
   field-key. Missing refs render as empty string."
  [_form-zloc [sep & refs] resolved]
  (let [parts (map (fn [k]
                     (or (some-> (get resolved k) :value)
                         ""))
                   refs)]
    {:value (clojure.string/join sep parts) :no-zloc true}))

(def ^:private op-table
  {:nth              op-nth
   :find-first       op-find-first
   :find-first-after op-find-first-after
   :right-of         op-right-of
   :left-of          op-left-of
   :when-type        op-when-type
   :literal          op-literal
   :join             op-join})

;; ============================================================
;; Dispatch + value extraction
;; ============================================================

(defn- run-selector
  "Run a selector expression against a form zloc. Returns either:
   - a zloc (selector resolved to an AST node)
   - a map {:value s :no-zloc true} for synthetic ops (:literal, :join)
   - nil (selector did not resolve)"
  [form-zloc sel resolved]
  (cond
    (nil? sel) nil
    (not (vector? sel)) (throw (ex-info "Selector must be a vector [op & args]"
                                        {:got sel}))
    :else
    (let [[op & args] sel
          f (op-table op)]
      (when-not f
        (throw (ex-info (str "Unknown selector op: " op)
                        {:op op :valid-ops (keys op-table)})))
      (f form-zloc args resolved))))

(defn- result->record
  "Normalize a run-selector result into {:zloc z :value v}.
   For synthetic ops (no-zloc), :zloc is nil."
  [result]
  (cond
    (nil? result) nil
    (:no-zloc result) {:zloc nil :value (:value result)}
    :else {:zloc result :value (z/string result)}))

;; ============================================================
;; Field-spec analysis: figure out resolution order
;; ============================================================

(defn- selector-refs
  "Field-keys this selector depends on. Walks nested selectors recursively."
  [sel]
  (cond
    (not (vector? sel)) #{}
    :else
    (let [[op & args] sel]
      (case op
        :right-of         #{(first args)}
        :left-of          #{(first args)}
        :find-first-after #{(first args)}
        :join             (set (rest args))
        :when-type        (selector-refs (second args))
        #{}))))

(defn- spec->selector [spec]
  (if (vector? spec) spec (:select spec)))

(defn field-order
  "Topo-sort fields by :right-of / :join refs. Throw on cycle / dangling ref."
  [fields]
  (let [keys (set (clojure.core/keys fields))
        deps (into {} (for [[k v] fields]
                        [k (selector-refs (spec->selector v))]))]
    (doseq [[k refs] deps
            r refs]
      (when-not (contains? keys r)
        (throw (ex-info (str "Field " k " references unknown field " r)
                        {:field k :missing r :known keys}))))
    (loop [remaining keys
           resolved []
           resolved-set #{}]
      (if (empty? remaining)
        resolved
        (let [ready (filter (fn [k]
                              (set/subset? (deps k) resolved-set))
                            remaining)]
          (when (empty? ready)
            (throw (ex-info "Cyclic field dependencies"
                            {:cycle remaining :deps deps})))
          (let [next-batch (vec ready)]
            (recur (clojure.set/difference remaining (set next-batch))
                   (into resolved next-batch)
                   (clojure.set/union resolved-set (set next-batch)))))))))

;; ============================================================
;; Public entry point
;; ============================================================

(defn resolve-fields
  "Given a form zloc and a fields map (field-key -> selector or spec map),
   resolve every field in topo order. Returns a map field-key -> string
   value, omitting fields that didn't resolve unless :optional? false."
  [form-zloc fields]
  (let [order (field-order fields)]
    (loop [remaining order
           resolved {}]
      (if (empty? remaining)
        ;; Build output: field-key -> :value (strings)
        (into {} (for [[k {:keys [value]}] resolved
                       :when value]
                   [k value]))
        (let [k (first remaining)
              spec (get fields k)
              sel (spec->selector spec)
              optional? (and (map? spec) (:optional? spec true))
              raw (run-selector form-zloc sel resolved)
              record (result->record raw)]
          (recur (rest remaining)
                 (cond
                   record (assoc resolved k record)
                   optional? resolved
                   :else (throw (ex-info (str "Required field " k " did not resolve")
                                         {:field k :selector sel})))))))))

(defn validate-spec
  "Sanity-check a field-spec at config-load time. Throws on:
   - unknown op
   - bad arg shapes
   - unknown :right-of / :left-of / :join refs (caught at field-order time)
   Returns fields unchanged."
  [fields]
  (doseq [[k v] fields]
    (let [sel (spec->selector v)]
      (when-not (vector? sel)
        (throw (ex-info (str "Field " k " has no :select selector")
                        {:field k :spec v})))
      ;; force field-order to compute deps; will throw on dangling/cyclic
      (field-order fields)))
  fields)
