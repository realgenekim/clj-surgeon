(ns edit-field-alias-algebra
  "Pure experiment for a closed, fail-closed compact-edit field vocabulary.

   This namespace does not alter the product schema or compiler. It models the
   smallest relation that can lower two observed field pairs to the canonical
   from/to pair without accepting mixtures, partial pairs, or duplicated
   semantic authority."
  (:require
   [clojure.set :as set]))

(def canonical-pair ["from" "to"])

(def accepted-pairs
  [{:name :canonical :fields canonical-pair}
   {:name :old-new :fields ["old" "new"]}
   {:name :before-after :fields ["before" "after"]}])

(def alias-fields
  (set (mapcat :fields accepted-pairs)))

(defn present-alias-fields
  [edit]
  (set/intersection alias-fields (set (keys edit))))

(defn complete-pairs
  [present]
  (filterv #(set/subset? (set (:fields %)) present) accepted-pairs))

(defn refusal-reason
  [present complete]
  (cond
    (empty? present) :missing-edit-field-pair
    (> (count complete) 1) :multiple-edit-field-pairs
    (= 1 (count complete)) :mixed-edit-field-pairs
    (= 1 (count (keep (fn [{:keys [name fields]}]
                        (when (seq (set/intersection present (set fields))) name))
                      accepted-pairs)))
    :partial-edit-field-pair
    :else :mixed-edit-field-pairs))

(defn lower-edit
  "Return one canonical edit only for exactly one complete accepted pair.

   The relation deliberately refuses canonical+alias duplication even when
   values agree. Duplicate authority is ambiguous input, not a compatibility
   opportunity. Fields unrelated to the pair are preserved byte-for-value."
  [edit]
  (let [present (present-alias-fields edit)
        complete (complete-pairs present)]
    (if (and (= 1 (count complete))
             (= present (set (:fields (first complete)))))
      (let [{:keys [name fields]} (first complete)
            [source-field replacement-field] fields
            lowered (-> edit
                        (dissoc source-field replacement-field)
                        (assoc "from" (get edit source-field)
                               "to" (get edit replacement-field)))]
        {:ok true
         :edit lowered
         :normalization
         {:relation "exact-edit-field-pair"
          :requested_pair fields
          :emitted_pair canonical-pair
          :canonical (= :canonical name)}})
      {:ok false
       :error_type :invalid-mcp-request
       :reason (refusal-reason present complete)
       :source_unchanged true
       :write_authority false
       :present_fields (vec (sort present))
       :accepted_pairs (mapv :fields accepted-pairs)
       :remedy (str "Provide exactly one complete edit pair: from/to, old/new, "
                    "or before/after; then call edit_clojure once. "
                    "No source was changed.")})))

(defn lower-edits
  "Lower a batch atomically. One refusal returns no lowered siblings."
  [edits]
  (loop [index 0
         remaining edits
         lowered []
         evidence []]
    (if-let [edit (first remaining)]
      (let [result (lower-edit edit)]
        (if (:ok result)
          (recur (inc index)
                 (rest remaining)
                 (conj lowered (:edit result))
                 (conj evidence (assoc (:normalization result) :edit_index index)))
          (assoc result :path ["edits" index])))
      {:ok true
       :edits lowered
       :normalizations evidence})))
