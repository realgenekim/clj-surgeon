(ns clj-surgeon.mcp-compact-edit-fields)

;; @spec MCP-OP-EDIT-017
;; @spec MCP-OP-EDIT-018
(def ^:private value-pairs
  [{:relation "from-to"
    :source-field "from"
    :target-field "to"}
   {:relation "old-new"
    :source-field "old"
    :target-field "new"}
   {:relation "before-after"
    :source-field "before"
    :target-field "after"}])

(def ^:private value-field-names
  (set (mapcat (juxt :source-field :target-field) value-pairs)))

(defn- field-name
  [field]
  (cond
    (keyword? field) (name field)
    (string? field) field
    :else (str field)))

(defn- field-key
  [edit requested-name]
  (some (fn [field]
          (when (= requested-name (field-name field)) field))
        (keys edit)))

(defn- remove-value-fields
  [edit]
  (reduce-kv
    (fn [result field value]
      (if (contains? value-field-names (field-name field))
        result
        (assoc result field value)))
    (empty edit)
    edit))

(defn normalize-edit
  "Lower one exact compact edit value-pair spelling to canonical from/to.

  This function is source-blind. It refuses any partial, mixed, or repeated
  spelling before location or source compilation can begin."
  [edit index]
  (if-not (map? edit)
    {:ok true :edit edit}
    (let [supplied-fields
          (->> (keys edit)
               (map field-name)
               (filter value-field-names)
               distinct
               sort
               vec)
          supplied-set (set supplied-fields)
          pair
          (some (fn [{:keys [source-field target-field] :as candidate}]
                  (when (= supplied-set #{source-field target-field})
                    candidate))
                value-pairs)]
      (if-not pair
        {:ok false
         :reason :invalid-editor-field-pair
         :path ["edits" index]
         :supplied-fields supplied-fields
         :source-unchanged true
         :mutation-attempted false
         :write-authority false
         :remedy
         (str "Provide exactly one complete pair: old/new, before/after, or "
              "from/to. old maps to from, new maps to to, before maps "
              "to from, and after maps to to. Correct this edit_clojure "
              "request once; no source was changed.")}
        (let [{:keys [relation source-field target-field]} pair]
          (if (= relation "from-to")
            {:ok true :edit edit}
            {:ok true
             :edit (assoc (remove-value-fields edit)
                          "from" (get edit (field-key edit source-field))
                          "to" (get edit (field-key edit target-field)))
             :evidence
             {:edit_index index
              :relation relation
              :requested_fields [source-field target-field]
              :emitted_fields ["from" "to"]}}))))))

(defn normalize-edits
  "Normalize one compact edit batch or return the first fail-closed refusal."
  [edits]
  (reduce
    (fn [{:keys [edits evidence] :as result} [index edit]]
      (if-not (:ok result)
        (reduced result)
        (let [normalized (normalize-edit edit index)]
          (if-not (:ok normalized)
            (reduced normalized)
            {:ok true
             :edits (conj edits (:edit normalized))
             :evidence (cond-> evidence
                         (:evidence normalized)
                         (conj (:evidence normalized)))}))))
    {:ok true :edits [] :evidence []}
    (map-indexed vector edits)))
