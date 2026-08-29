(ns request-shape-compression-screen
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]))

(defn- json-bytes [value]
  (alength (.getBytes (json/generate-string value) "UTF-8")))

(defn- canonical [value]
  (cond
    (map? value) (into (sorted-map) (map (fn [[k v]] [k (canonical v)])) value)
    (vector? value) (mapv canonical value)
    (sequential? value) (mapv canonical value)
    :else value))

(defn- edit-multiset [edits]
  (frequencies (map #(json/generate-string (canonical %)) edits)))

(defn- distinct-in-order [values]
  (:values
    (reduce
      (fn [{:keys [seen] :as result} value]
        (if (contains? seen value)
          result
          (-> result
              (update :seen conj value)
              (update :values conj value))))
      {:seen #{} :values []}
      values)))

(defn- without-default-match [edit]
  (cond-> edit
    (= 1 (:matches edit)) (dissoc :matches)))

(defn- restore-default-match [edit]
  (if (contains? edit :matches) edit (assoc edit :matches 1)))

(defn- top-level [request additions]
  (cond-> additions
    (:workspace_root request) (assoc :workspace_root (:workspace_root request))
    (:delete_owners request) (assoc :delete_owners (:delete_owners request))))

(defn- omitted-defaults [request]
  (update request :edits #(mapv without-default-match %)))

(defn- file-index-shape [request]
  (let [files (distinct-in-order (map :file (:edits request)))
        file-index (zipmap files (range))]
    (top-level
      request
      {:files files
       :edits
       (mapv
         (fn [edit]
           (-> edit
               (assoc :file_index (file-index (:file edit)))
               (dissoc :file)
               without-default-match))
         (:edits request))})))

(defn- expand-file-index [shape]
  (mapv
    (fn [edit]
      (-> edit
          (assoc :file (get (:files shape) (:file_index edit)))
          (dissoc :file_index)
          restore-default-match))
    (:edits shape)))

(defn- file-group-shape [request]
  (let [files (distinct-in-order (map :file (:edits request)))]
    (top-level
      request
      {:edit_groups
       (mapv
         (fn [file]
           {:file file
            :edits
            (->> (:edits request)
                 (filter #(= file (:file %)))
                 (mapv #(-> % (dissoc :file) without-default-match)))})
         files)})))

(defn- expand-file-groups [shape]
  (mapv
    (fn [[file edit]]
      (-> edit (assoc :file file) restore-default-match))
    (mapcat
      (fn [{:keys [file edits]}]
        (map #(vector file %) edits))
      (:edit_groups shape))))

(defn- replacement-group-key [edit]
  (when (string? (get-in edit [:within :form]))
    [(:from edit) (:to edit) (:matches edit)]))

(defn- replacement-group-shape [request]
  (let [frequencies (frequencies (keep replacement-group-key (:edits request)))
        grouped? #(> (get frequencies (replacement-group-key %) 0) 1)
        grouped (filterv grouped? (:edits request))
        retained (remove grouped? (:edits request))
        keys (distinct-in-order (map replacement-group-key grouped))]
    (top-level
      request
      {:replacement_groups
       (mapv
         (fn [[from to matches :as key]]
           (let [edits (filterv #(= key (replacement-group-key %)) grouped)
                 files (distinct-in-order (map :file edits))]
             (cond-> {:from from
                      :to to
                      :sites
                      (mapv
                        (fn [file]
                          {:file file
                           :forms
                           (mapv #(get-in % [:within :form])
                                 (filter #(= file (:file %)) edits))})
                        files)}
               (not= 1 matches) (assoc :matches matches))))
         keys)
       :edits (mapv without-default-match retained)})))

(defn- expand-replacement-groups [shape]
  (into
    (mapv restore-default-match (:edits shape))
    (for [{:keys [from to matches sites]} (:replacement_groups shape)
          {:keys [file forms]} sites
          form forms]
      {:file file
       :within {:form form}
       :from from
       :to to
       :matches (or matches 1)})))

(defn- value-prefix [value]
  (let [index (str/last-index-of value "/")]
    (if index (subs value 0 (inc index)) "")))

(defn- value-name [value]
  (let [index (str/last-index-of value "/")]
    (if index (subs value (inc index)) value)))

(defn- symbol-edit? [edit]
  (and (string? (get-in edit [:within :form]))
       (re-matches #"[^\s\[\](){}]+" (:from edit))
       (re-matches #"[^\s\[\](){}]+" (:to edit))
       (= (value-name (:from edit)) (value-name (:to edit)))))

(defn- grouped-symbol-edits [edits]
  (let [files (distinct-in-order (map :file edits))]
    (mapv
      (fn [file]
        (let [file-edits (filterv #(= file (:file %)) edits)
              from-prefixes (distinct (map (comp value-prefix :from) file-edits))
              to-prefixes (distinct (map (comp value-prefix :to) file-edits))
              _ (assert (= 1 (count from-prefixes)))
              _ (assert (= 1 (count to-prefixes)))
              forms (distinct-in-order (map #(get-in % [:within :form]) file-edits))]
          {:file file
           :from_prefix (first from-prefixes)
           :to_prefix (first to-prefixes)
           :owners
           (mapv
             (fn [form]
               {:form form
                :symbols
                (mapv
                  (fn [edit]
                    (cond-> {:name (value-name (:from edit))}
                      (not= 1 (:matches edit))
                      (assoc :matches (:matches edit))))
                  (filter #(= form (get-in % [:within :form])) file-edits))})
             forms)}))
      files)))

(defn- relation-shape [request]
  (let [namespace-edits
        (filterv #(= {:namespace true} (:within %)) (:edits request))
        symbol-edits (filterv symbol-edit? (:edits request))
        retained (remove (set (concat namespace-edits symbol-edits)) (:edits request))]
    (assert (= 9 (count namespace-edits)))
    (assert (= 23 (count symbol-edits)))
    (assert (= 1 (count retained)))
    (top-level
      request
      {:namespace_edits
       (mapv #(-> % (dissoc :within) without-default-match) namespace-edits)
       :symbol_rewrites (grouped-symbol-edits symbol-edits)
       :edits (mapv without-default-match retained)})))

(defn- require-entries [clause]
  (mapv (fn [[_ lib alias]] {:lib lib :as alias})
        (re-seq #"\[([^\s\]]+)\s+:as\s+([^\s\]]+)\]" clause)))

(defn- require-delta [target edit]
  (let [before (set (require-entries (:from edit)))
        after (set (require-entries (:to edit)))
        added (set (remove before after))
        removed (set (remove after before))]
    (assert (= #{target} added))
    (assert (<= (count removed) 1))
    (cond-> {:file (:file edit)}
      (seq removed) (assoc :remove (first removed)))))

(defn- relation-with-require-delta-shape [request]
  (let [target {:lib "sample.views.submission-row" :as "submission-row"}
        namespace-edits
        (filterv #(= {:namespace true} (:within %)) (:edits request))
        symbol-edits (filterv symbol-edit? (:edits request))
        retained (remove (set (concat namespace-edits symbol-edits)) (:edits request))]
    (assert (= 9 (count namespace-edits)))
    (assert (= 23 (count symbol-edits)))
    (assert (= 1 (count retained)))
    (top-level
      request
      {:require_change
       {:add target
        :files (mapv #(require-delta target %) namespace-edits)}
       :symbol_rewrites (grouped-symbol-edits symbol-edits)
       :edits (mapv without-default-match retained)})))

(defn- exact-occurrences [text fragment]
  (count (re-seq (re-pattern (java.util.regex.Pattern/quote fragment)) text)))

(declare expand-relation)

(defn- compile-require-change [shape frozen-clauses]
  (let [{:keys [add files]} (:require_change shape)
        add-line (str "\n   [" (:lib add) " :as " (:as add) "]")]
    (mapv
      (fn [{:keys [file remove]}]
        (let [from (get frozen-clauses file)
              _ (assert (string? from))
              _ (assert (str/ends-with? from ")"))
              _ (assert (zero? (exact-occurrences from add-line)))
              remove-line (when remove
                            (str "\n   [" (:lib remove) " :as " (:as remove) "]"))
              _ (when remove-line
                  (assert (= 1 (exact-occurrences from remove-line))))
              without-remove (if remove-line
                               (str/replace from remove-line "")
                               from)
              to (str (subs without-remove 0 (dec (count without-remove)))
                      add-line
                      ")")]
          {:file file
           :within {:namespace true}
           :from from
           :to to
           :matches 1}))
      files)))

(defn- expand-relation-with-require-delta [shape frozen-clauses]
  (let [compiled-requires (compile-require-change shape frozen-clauses)
        relation (-> shape
                     (dissoc :require_change)
                     (assoc :namespace_edits []))]
    (into compiled-requires (expand-relation relation))))

(defn- expand-relation [shape]
  (let [namespace-edits
        (mapv #(-> %
                   (assoc :within {:namespace true})
                   restore-default-match)
              (:namespace_edits shape))
        symbol-edits
        (mapv
          (fn [{:keys [file from_prefix to_prefix form name matches]}]
            {:file file
             :within {:form form}
             :from (str from_prefix name)
             :to (str to_prefix name)
             :matches (or matches 1)})
          (for [{:keys [file from_prefix to_prefix owners]}
                (:symbol_rewrites shape)
                {:keys [form symbols]} owners
                {:keys [name matches]} symbols]
            {:file file
             :from_prefix from_prefix
             :to_prefix to_prefix
             :form form
             :name name
             :matches matches}))
        retained (mapv restore-default-match (:edits shape))]
    (into [] (concat namespace-edits symbol-edits retained))))

(defn- scope-tuple [edit]
  (let [[kind owner]
        (cond
          (= true (get-in edit [:within :namespace])) ["namespace" nil]
          (get-in edit [:within :form]) ["form" (get-in edit [:within :form])]
          (= true (get-in edit [:within :root])) ["root" nil]
          :else ["omitted" nil])
        row [(:file edit) kind owner (:from edit) (:to edit)]]
    (cond-> row
      (not= 1 (:matches edit)) (conj (:matches edit)))))

(defn- tuple-shape [request]
  (top-level request {:edit_rows (mapv scope-tuple (:edits request))}))

(defn- expand-tuples [shape]
  (mapv
    (fn [[file kind owner from to matches]]
      {:file file
       :within (case kind
                 "namespace" {:namespace true}
                 "form" {:form owner}
                 "root" {:root true}
                 "omitted" nil)
       :from from
       :to to
       :matches (or matches 1)})
    (:edit_rows shape)))

(defn- metric [baseline value]
  (let [size (json-bytes value)]
    {:bytes size
     :saved-bytes (- baseline size)
     :reduction (double (/ (- baseline size) baseline))}))

(defn -main [& [request-file]]
  (assert request-file "Provide one compact JSON request file")
  (let [request (json/parse-string (slurp request-file) true)
        baseline (json-bytes request)
        original-multiset (edit-multiset (:edits request))
        default-shape (omitted-defaults request)
        index-shape (file-index-shape request)
        group-shape (file-group-shape request)
        replacement-groups (replacement-group-shape request)
        relation (relation-shape request)
        relation-with-require-delta (relation-with-require-delta-shape request)
        tuples (tuple-shape request)
        namespace-edits (filterv #(= {:namespace true} (:within %)) (:edits request))
        frozen-clauses (into {} (map (juxt :file :from) namespace-edits))
        equivalence
        {:omitted-defaults
         (= original-multiset
            (edit-multiset (mapv restore-default-match (:edits default-shape))))
         :file-index (= original-multiset
                        (edit-multiset (expand-file-index index-shape)))
         :file-groups (= original-multiset
                         (edit-multiset (expand-file-groups group-shape)))
         :replacement-groups
         (= original-multiset
            (edit-multiset (expand-replacement-groups replacement-groups)))
         :closed-relations (= original-multiset
                              (edit-multiset (expand-relation relation)))
         :closed-relations-with-require-delta
         (= original-multiset
            (edit-multiset
              (expand-relation-with-require-delta
                relation-with-require-delta frozen-clauses)))
         :positional-tuples (= original-multiset
                               (edit-multiset (expand-tuples tuples)))}]
    (assert (every? true? (vals equivalence)))
    (prn
      {:schema :clj-surgeon.request-shape-compression-screen/v1
       :source {:file request-file
                :bytes baseline
                :edits (count (:edits request))
                :edit-matches (reduce + (map :matches (:edits request)))
                :default-match-rows (count (filter #(= 1 (:matches %)) (:edits request)))
                :files (count (distinct (map :file (:edits request))))
                :deletion-groups (count (:delete_owners request))
                :deleted-owners (reduce + (map #(count (:forms %)) (:delete_owners request)))}
       :baseline-components
       {:namespace-edit-bytes (json-bytes namespace-edits)
        :symbol-edit-bytes (json-bytes (filterv symbol-edit? (:edits request)))
        :retained-edit-bytes
        (json-bytes
          (remove (set (concat namespace-edits
                               (filterv symbol-edit? (:edits request))))
                  (:edits request)))
        :delete-owner-bytes (json-bytes (:delete_owners request))}
       :decision-geometry
       {:baseline {:file-occurrences (count (:edits request))
                   :scope-occurrences (count (:edits request))
                   :literal-pairs (count (:edits request))
                   :explicit-default-counts
                   (count (filter #(= 1 (:matches %)) (:edits request)))}
        :file-groups {:file-groups (count (:edit_groups group-shape))
                      :local-edit-rows
                      (reduce + (map #(count (:edits %)) (:edit_groups group-shape)))
                      :literal-pairs (count (:edits request))}
        :replacement-groups
        {:groups (count (:replacement_groups replacement-groups))
         :sites (reduce +
                        (for [group (:replacement_groups replacement-groups)
                              site (:sites group)]
                          (count (:forms site))))
         :retained-edits (count (:edits replacement-groups))}
        :closed-relations-with-require-delta
        {:require-targets 1
         :require-files (count (get-in relation-with-require-delta
                                       [:require_change :files]))
         :require-removals
         (count (filter :remove (get-in relation-with-require-delta
                                        [:require_change :files])))
         :symbol-file-groups (count (:symbol_rewrites relation-with-require-delta))
         :symbol-owner-groups
         (reduce + (map #(count (:owners %))
                        (:symbol_rewrites relation-with-require-delta)))
         :symbol-names
         (reduce + (for [group (:symbol_rewrites relation-with-require-delta)
                         owner (:owners group)]
                     (count (:symbols owner))))
         :retained-literal-pairs (count (:edits relation-with-require-delta))}}
       :equivalence equivalence
       :metrics
       {:omit-default-matches (metric baseline default-shape)
        :file-index (metric baseline index-shape)
        :file-groups (metric baseline group-shape)
        :replacement-groups (metric baseline replacement-groups)
        :closed-relations (metric baseline relation)
        :closed-relations-with-require-delta
        (metric baseline relation-with-require-delta)
        :positional-tuples (metric baseline tuples)}})))
