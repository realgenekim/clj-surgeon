(ns request-shape-composition-screen
  (:require
   [cheshire.core :as json]
   [clojure.walk :as walk]
   [request-shape-compression-screen :as screen]))

(def candidates
  [:omit-default-matches
   :file-index
   :file-groups
   :replacement-groups
   :closed-relations
   :closed-relations-with-require-delta
   :positional-tuples])

(def descriptive-emission-seconds-per-character
  (/ 21.417 3638.0))

(defn- invoke-screen [owner & args]
  (apply (var-get (ns-resolve 'request-shape-compression-screen owner)) args))

(defn- json-bytes [value]
  (invoke-screen 'json-bytes value))

(defn- edit-multiset [edits]
  (invoke-screen 'edit-multiset edits))

(defn- replacement-residual [shape]
  (let [candidate
        (invoke-screen 'replacement-group-shape {:edits (:edits shape)})
        groups (:replacement_groups candidate)]
    (cond-> (assoc (dissoc shape :edits) :edits (:edits candidate))
      (seq groups) (assoc :replacement_groups groups))))

(defn- file-group-residual [shape]
  (let [candidate (invoke-screen 'file-group-shape {:edits (:edits shape)})]
    (-> shape
        (dissoc :edits)
        (assoc :edit_groups (:edit_groups candidate)))))

(defn- tuple-residual [shape]
  (let [shape
        (if (contains? shape :edits)
          (-> shape
              (assoc :edit_rows
                     (mapv #(invoke-screen 'scope-tuple %) (:edits shape)))
              (dissoc :edits))
          shape)]
    (if (contains? shape :edit_groups)
      (update
        shape
        :edit_groups
        (fn [groups]
          (mapv
            (fn [group]
              (-> group
                  (assoc :edit_rows
                         (mapv
                           #(subvec (invoke-screen 'scope-tuple %) 1)
                           (:edits group)))
                  (dissoc :edits)))
            groups)))
      shape)))

(defn- all-maps [value]
  (filter map? (tree-seq coll? seq value)))

(defn- distinct-in-order [values]
  (invoke-screen 'distinct-in-order values))

(defn- index-files [shape]
  (let [delete-owners (:delete_owners shape)
        shape (dissoc shape :delete_owners)
        map-files (keep :file (all-maps shape))
        row-files (keep first (:edit_rows shape))
        files (distinct-in-order (concat map-files row-files))
        indexes (zipmap files (range))
        indexed
        (walk/postwalk
          (fn [value]
            (if (and (map? value) (string? (:file value)))
              (-> value
                  (assoc :file_index (indexes (:file value)))
                  (dissoc :file))
              value))
          shape)
        indexed
        (if (contains? indexed :edit_rows)
          (update indexed :edit_rows
                  #(mapv (fn [row] (assoc row 0 (indexes (first row)))) %))
          indexed)]
    (cond-> (assoc indexed :files files)
      delete-owners (assoc :delete_owners delete-owners))))

(defn- primary-shape [request selected]
  (cond
    (contains? selected :closed-relations-with-require-delta)
    (invoke-screen 'relation-with-require-delta-shape request)

    (contains? selected :closed-relations)
    (invoke-screen 'relation-shape request)

    (contains? selected :replacement-groups)
    (invoke-screen 'replacement-group-shape request)

    (contains? selected :file-groups)
    (invoke-screen 'file-group-shape request)

    (contains? selected :positional-tuples)
    (invoke-screen 'tuple-shape request)

    (contains? selected :file-index)
    (invoke-screen 'file-index-shape request)

    (contains? selected :omit-default-matches)
    (invoke-screen 'omitted-defaults request)

    :else request))

(defn composed-shape [request selected]
  (let [shape (primary-shape request selected)
        relation?
        (or (contains? selected :closed-relations)
            (contains? selected :closed-relations-with-require-delta))
        shape
        (if (and (contains? selected :replacement-groups)
                 relation?)
          (replacement-residual shape)
          shape)
        shape
        (if (and (contains? selected :file-groups)
                 (or relation?
                     (contains? selected :replacement-groups)))
          (file-group-residual shape)
          shape)
        shape
        (if (and (contains? selected :positional-tuples)
                 (or relation?
                     (contains? selected :replacement-groups)
                     (contains? selected :file-groups)))
          (tuple-residual shape)
          shape)]
    (if (and (contains? selected :file-index)
             (or relation?
                 (contains? selected :replacement-groups)
                 (contains? selected :file-groups)
                 (contains? selected :positional-tuples)))
      (index-files shape)
      shape)))

(defn- unindex-files [shape]
  (let [files (:files shape)
        delete-owners (:delete_owners shape)
        shape (dissoc shape :delete_owners)
        shape
        (if (contains? shape :edit_rows)
          (update shape :edit_rows
                  #(mapv (fn [row] (assoc row 0 (get files (first row)))) %))
          shape)]
    (cond->
      (->>
        shape
        (walk/postwalk
          (fn [value]
            (if (and (map? value) (contains? value :file_index))
              (-> value
                  (assoc :file (get files (:file_index value)))
                  (dissoc :file_index))
              value)))
        (#(dissoc % :files)))
      delete-owners (assoc :delete_owners delete-owners))))

(defn- expand-local-row [file row]
  (first
    (invoke-screen
      'expand-tuples
      {:edit_rows [(into [file] row)]})))

(defn- untuple-residual [shape]
  (let [shape
        (if (contains? shape :edit_rows)
          (-> shape
              (assoc :edits (invoke-screen 'expand-tuples shape))
              (dissoc :edit_rows))
          shape)]
    (if (contains? shape :edit_groups)
      (update
        shape
        :edit_groups
        (fn [groups]
          (mapv
            (fn [{:keys [file edit_rows] :as group}]
              (-> group
                  (assoc :edits
                         (mapv #(dissoc (expand-local-row file %) :file)
                               edit_rows))
                  (dissoc :edit_rows)))
            groups)))
      shape)))

(defn- unfile-group-residual [shape]
  (-> shape
      (assoc :edits (invoke-screen 'expand-file-groups shape))
      (dissoc :edit_groups)))

(defn- unreplacement-residual [shape]
  (-> shape
      (assoc :edits (invoke-screen 'expand-replacement-groups shape))
      (dissoc :replacement_groups)))

(defn expanded-edits [shape selected frozen-clauses]
  (let [shape (if (contains? selected :file-index)
                (unindex-files shape)
                shape)
        shape (if (contains? selected :positional-tuples)
                (untuple-residual shape)
                shape)
        shape (if (contains? selected :file-groups)
                (unfile-group-residual shape)
                shape)
        shape (if (contains? selected :replacement-groups)
                (unreplacement-residual shape)
                shape)]
    (cond
      (contains? selected :closed-relations-with-require-delta)
      (invoke-screen 'expand-relation-with-require-delta shape frozen-clauses)

      (contains? selected :closed-relations)
      (invoke-screen 'expand-relation shape)

      :else
      (mapv #(invoke-screen 'restore-default-match %) (:edits shape)))))

(defn- frozen-clauses [request]
  (into {}
        (map (juxt :file :from)
             (filterv #(= {:namespace true} (:within %)) (:edits request)))))

(defn- exact-round-trip? [request selected shape]
  (let [metadata-shape (if (contains? selected :file-index)
                         (unindex-files shape)
                         shape)]
    (and (= (edit-multiset (:edits request))
            (edit-multiset
              (expanded-edits shape selected (frozen-clauses request))))
         (= (:delete_owners request) (:delete_owners metadata-shape))
         (= (:workspace_root request) (:workspace_root metadata-shape)))))

(defn- metric [baseline shape]
  (let [bytes (json-bytes shape)]
    {:bytes bytes
     :saved-bytes (- baseline bytes)
     :reduction (/ (- baseline bytes) (double baseline))
     :projected-emission-saving-seconds
     (* descriptive-emission-seconds-per-character (- baseline bytes))}))

(defn- classification [single-savings combined-savings]
  (let [best-single (apply max single-savings)
        naive (reduce + single-savings)
        overlap (- naive combined-savings)]
    {:increment-over-best (- combined-savings best-single)
     :overlap-bytes overlap
     :composition
     (cond
       (< combined-savings best-single) :interfering
       (zero? overlap) :additive
       (pos? overlap) :sub-additive
       :else :super-additive)}))

(defn report [request]
  (let [baseline (json-bytes request)
        singles
        (into {}
              (for [candidate candidates
                    :let [selected #{candidate}
                          shape (composed-shape request selected)]]
                [candidate
                 (assoc (metric baseline shape)
                        :round-trip (exact-round-trip? request selected shape))]))
        ordered-pairs
        (vec
          (for [left candidates
                right candidates
                :when (not= left right)
                :let [selected #{left right}
                      shape (composed-shape request selected)
                      combined (metric baseline shape)
                      composition
                      (classification
                        [(get-in singles [left :saved-bytes])
                         (get-in singles [right :saved-bytes])]
                        (:saved-bytes combined))]]
            (merge {:order [left right]
                    :round-trip (exact-round-trip? request selected shape)}
                   combined
                   composition)))
        triples
        (vec
          (for [a candidates
                b candidates
                c candidates
                :when (< (.indexOf candidates a)
                         (.indexOf candidates b)
                         (.indexOf candidates c))
                :let [selected #{a b c}
                      shape (composed-shape request selected)
                      combined (metric baseline shape)]]
            (merge {:members [a b c]
                    :round-trip (exact-round-trip? request selected shape)}
                   combined
                   (classification
                     (mapv #(get-in singles [% :saved-bytes]) selected)
                     (:saved-bytes combined)))))
        best-triple (apply max-key :saved-bytes triples)
        relation-candidates
        #{:closed-relations :closed-relations-with-require-delta}
        best-new-triple
        (apply max-key
               :saved-bytes
               (remove #(some relation-candidates (:members %)) triples))]
    (assert (every? :round-trip (vals singles)))
    (assert (every? :round-trip ordered-pairs))
    (assert (every? :round-trip triples))
    {:schema :clj-surgeon.request-shape-composition-screen/v1
     :baseline-bytes baseline
     :singles singles
     :ordered-pairs ordered-pairs
     :best-triple best-triple
     :best-new-triple best-new-triple
     :clears-20-percent
     (filterv #(>= (:reduction %) 0.20) ordered-pairs)
     :misses-20-percent
     (filterv #(< (:reduction %) 0.20) ordered-pairs)}))

(defn -main [& [request-file]]
  (assert request-file "Provide one compact JSON request file")
  (-> request-file
      slurp
      (json/parse-string true)
      report
      prn))
