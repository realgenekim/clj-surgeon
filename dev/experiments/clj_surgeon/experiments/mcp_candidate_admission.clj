(ns clj-surgeon.experiments.mcp-candidate-admission)

(defn- public-key [value]
  (if (keyword? value) (name value) (str value)))

(defn- object-value? [value]
  (or (map? value) (instance? java.util.Map value)))

(defn- array-value? [value]
  (or (sequential? value) (instance? java.util.List value)))

(defn- public-map [value]
  (when (object-value? value)
    (into {} (map (fn [[key child]] [(public-key key) child])) value)))

(declare valid?)

(defn- type-valid? [expected value]
  (case expected
    "object" (object-value? value)
    "array" (array-value? value)
    "string" (string? value)
    "integer" (integer? value)
    "number" (number? value)
    "boolean" (instance? Boolean value)
    false))

(def supported-schema-keywords
  #{:type :description :title :additionalProperties :properties :required
    :items :prefixItems :minItems :maxItems :uniqueItems :minLength :pattern :minimum
    :maximum :const :enum :allOf :anyOf :oneOf :not :default
    :minProperties :maxProperties})

(defn- unsupported-schema-keywords [schema]
  (let [local (remove supported-schema-keywords (keys schema))
        property-children (vals (:properties schema))
        direct-children (remove nil? [(:items schema) (:not schema)
                                      (when (map? (:additionalProperties schema))
                                        (:additionalProperties schema))])
        prefix-children (or (:prefixItems schema) [])
        branch-children (mapcat #(or (% schema) []) [:allOf :anyOf :oneOf])]
    (into (set local)
          (mapcat unsupported-schema-keywords)
          (concat property-children direct-children prefix-children branch-children))))

(defn- object-valid? [schema value]
  (if-not (object-value? value)
    true
    (let [value (public-map value)
          properties (public-map (:properties schema))
          required (map public-key (:required schema))
          unexpected (remove (set (keys properties)) (keys value))
          additional-schema (when (map? (:additionalProperties schema))
                              (:additionalProperties schema))]
      (and
        (every? #(contains? value %) required)
        (or (not (:minProperties schema))
            (<= (:minProperties schema) (count value)))
        (or (not (:maxProperties schema))
            (<= (count value) (:maxProperties schema)))
        (or (not= false (:additionalProperties schema))
            (empty? unexpected))
        (or (not additional-schema)
            (every? #(valid? additional-schema (get value %)) unexpected))
        (every? (fn [[key child-schema]]
                  (or (not (contains? value key))
                      (valid? child-schema (get value key))))
                properties)))))

(defn- array-valid? [schema value]
  (if-not (array-value? value)
    true
    (let [value (vec value)
          prefix-items (vec (or (:prefixItems schema) []))
          remaining (if (seq prefix-items)
                      (subvec value (min (count value) (count prefix-items)))
                      value)]
      (and
        (or (not (:minItems schema))
            (<= (:minItems schema) (count value)))
        (or (not (:maxItems schema))
            (<= (count value) (:maxItems schema)))
        (or (not (:uniqueItems schema))
            (= (count value) (count (distinct value))))
        (every? true?
                (map valid? prefix-items value))
        (or (not (:items schema))
            (every? #(valid? (:items schema) %) remaining))))))

(defn- scalar-valid? [schema value]
  (and
    (or (not (contains? schema :const)) (= (:const schema) value))
    (or (not (:enum schema)) (some #(= value %) (:enum schema)))
    (or (not (:minLength schema))
        (and (string? value) (<= (:minLength schema) (count value))))
    (or (not (:pattern schema))
        (and (string? value)
             (boolean (re-find (re-pattern (:pattern schema)) value))))
    (or (not (:minimum schema))
        (and (number? value) (<= (:minimum schema) value)))
    (or (not (:maximum schema))
        (and (number? value) (<= value (:maximum schema))))))

(defn valid?
  "Validate the JSON Schema subset published by clj-surgeon MCP tools."
  [schema value]
  (and
    (or (not (:type schema)) (type-valid? (:type schema) value))
    (object-valid? schema value)
    (array-valid? schema value)
    (scalar-valid? schema value)
    (or (not (:allOf schema)) (every? #(valid? % value) (:allOf schema)))
    (or (not (:anyOf schema)) (some #(valid? % value) (:anyOf schema)))
    (or (not (:oneOf schema))
        (= 1 (count (filter #(valid? % value) (:oneOf schema)))))
    (or (not (:not schema)) (not (valid? (:not schema) value)))))

(defn authorize
  "Enforce one advertised public schema before its shared handler runs."
  [schema params]
  (let [params (or params {})
        unsupported (vec (sort (unsupported-schema-keywords schema)))
        param-keys (set (map public-key (keys params)))
        property-keys (set (map public-key (keys (:properties schema))))
        unexpected (vec (sort (remove property-keys param-keys)))
        branches (or (:oneOf schema) [schema])
        matching-branches (count (filter #(valid? % params) branches))]
    (if (and (empty? unsupported) (valid? schema params))
      {:ok true}
      {:ok false
       :error "The invoked public tool does not authorize this request"
       :error-type :public-schema-denied
       :unsupported-schema-keywords unsupported
       :unexpected-fields unexpected
       :matching-branches matching-branches
       :mutation-attempted false
       :write-authority false})))
