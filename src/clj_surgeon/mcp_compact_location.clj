(ns clj-surgeon.mcp-compact-location
  "Pure, source-aware lowering of compact edits to explicit generic owners."
  (:require
   [clj-surgeon.outline :as outline]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def ^:private namespace-clause-kinds
  #{:refer-clojure :require :require-macros :use :use-macros
    :import :load :gen-class})

(defn- meaningful-children
  [form-node]
  (->> (node/children form-node)
       (remove node/whitespace?)
       vec))

(defn- parse-one-node
  [source]
  (when (string? source)
    (try
      (let [forms (-> (parser/parse-string-all source)
                      meaningful-children)]
        (when (and (= 1 (count forms))
                   (not (node/comment? (first forms))))
          (first forms)))
      (catch Exception _ nil))))

(defn- lossless-fingerprint
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn- direct-form-nodes
  [source]
  (try
    (->> (parser/parse-string-all source)
         meaningful-children
         (remove node/comment?)
         vec)
    (catch Exception _ [])))

(defn- list-head
  [form-node]
  (when (= :list (node/tag form-node))
    (try
      (first (node/sexpr form-node))
      (catch Exception _ nil))))

(defn- namespace-evidence
  [file source]
  (let [records (outline/top-level-form-records file source)
        walked-namespaces (filterv #(= 'ns (:type %)) records)
        direct-nodes (direct-form-nodes source)
        direct-namespaces (filterv #(= 'ns (list-head %)) direct-nodes)
        direct-namespace (first direct-namespaces)
        namespace-name
        (when (and (= 1 (count walked-namespaces))
                   (= 1 (count direct-namespaces)))
          (try
            (let [candidate (second (node/sexpr direct-namespace))]
              (when (symbol? candidate) candidate))
            (catch Exception _ nil)))]
    {:records records
     :namespace-node direct-namespace
     :namespace-name namespace-name
     :unique-direct-namespace?
     (and (= 1 (count walked-namespaces))
          (= 1 (count direct-namespaces))
          (symbol? namespace-name))}))

(defn- namespace-clause-kind
  [source]
  (when-let [form-node (parse-one-node source)]
    (when (= :list (node/tag form-node))
      (let [head (list-head form-node)]
        (when (contains? namespace-clause-kinds head)
          head)))))

(defn- descendant-nodes
  [form-node]
  (tree-seq node/inner? node/children form-node))

(defn- fingerprint-count
  [root target]
  (let [target-fingerprint (lossless-fingerprint target)]
    (->> (descendant-nodes root)
         (filter #(= target-fingerprint (lossless-fingerprint %)))
         count)))

(defn- direct-fingerprint-count
  [root target]
  (let [target-fingerprint (lossless-fingerprint target)]
    (->> (meaningful-children root)
         (filter #(= target-fingerprint (lossless-fingerprint %)))
         count)))

(defn- source-fingerprint-count
  [source target]
  (try
    (fingerprint-count (parser/parse-string-all source) target)
    (catch Exception _ 0)))

(defn- replacement-source
  [change]
  (let [[operation value] (:do change)]
    (when (= :replace operation) value)))

(defn- named-owner-evidence
  [file source]
  (when-let [form-node (parse-one-node source)]
    (let [records (outline/top-level-form-records file source)]
      (when (and (= 1 (count records))
                 (= 1 (count (direct-form-nodes source))))
        (let [record (first records)]
          (when (and (:name record)
                     (= (list-head form-node) (:type record)))
            {:node form-node
             :kind (:type record)
             :name (:name record)}))))))

(defn- evidence
  [change index relation requested emitted]
  {:edit_id (some-> (:id change) name)
   :edit_index index
   :relation relation
   :requested_location requested
   :emitted_location emitted})

(defn- lower-namespace-name-in-form
  [source change index]
  (when (= 1 (count (:forms change)))
    (let [requested (first (:forms change))
          file (first (:in change))
          {:keys [records namespace-name unique-direct-namespace?]}
          (namespace-evidence file source)
          competing (filterv #(= requested (:name %)) records)]
      (when (and unique-direct-namespace?
                 (= requested namespace-name)
                 (empty? competing))
        {:change (-> change
                     (dissoc :forms)
                     (update :expect dissoc :each-form)
                     (assoc :owner {:kind :namespace :name requested}))
         :evidence
         (evidence change index "namespace-name-in-form"
                   {:form (str requested)}
                   {:namespace (str requested)})}))))

(defn- lower-namespace-clause
  [source change index]
  (let [file (first (:in change))
        from-node (parse-one-node (:find change))
        from-kind (namespace-clause-kind (:find change))
        to-kind (namespace-clause-kind (replacement-source change))
        matches (get-in change [:expect :matches])]
    (when (and from-node
               from-kind
               (= from-kind to-kind)
               (integer? matches)
               (pos? matches))
      (let [{:keys [namespace-node unique-direct-namespace?]}
            (namespace-evidence file source)
            direct-count (when unique-direct-namespace?
                           (direct-fingerprint-count namespace-node from-node))
            namespace-count (when unique-direct-namespace?
                              (fingerprint-count namespace-node from-node))
            source-count (source-fingerprint-count source from-node)]
        (when (and unique-direct-namespace?
                   (= matches direct-count)
                   (= direct-count namespace-count)
                   (= namespace-count source-count))
          {:change (assoc change :owner {:kind :namespace})
           :evidence
           (evidence change index "namespace-clause" "omitted"
                     {:namespace true})})))))

(defn- lower-complete-named-owner
  [source change index]
  (let [file (first (:in change))
        before (named-owner-evidence file (:find change))
        after (named-owner-evidence file (replacement-source change))
        matches (get-in change [:expect :matches])]
    (when (and before
               after
               (= (select-keys before [:kind :name])
                  (select-keys after [:kind :name]))
               (= 1 matches))
      (let [target (lossless-fingerprint (:node before))
            owner-count
            (->> (outline/top-level-form-records file source)
                 (filter #(and (= (:kind before) (:type %))
                               (= (:name before) (:name %))))
                 count)
            occurrences
            (->> (direct-form-nodes source)
                 (filter #(= target (lossless-fingerprint %)))
                 count)]
        (when (and (= 1 owner-count) (= 1 occurrences))
          {:change (assoc change :forms [(:name before)])
           :evidence
           (evidence change index "complete-named-owner" "omitted"
                     {:form (str (:name before))})})))))

(defn- unresolved
  [change index]
  {:ok false
   :error "Compact edit location could not be proven from the frozen source"
   :error-type :compact-location-unresolved
   :change-index index
   :change-id (:id change)
   :source-unchanged true
   :mutation-attempted false
   :write-authority false})

(defn normalize-spec
  "Lower eligible compact edits to explicit generic owners using frozen source.

   The relation is all-or-nothing. It never reads, writes, or grants authority
   from similarity. Generic direct changes do not call this function."
  [sources spec {:keys [change-indexes]}]
  ;; @spec MCP-OP-EDIT-012
  ;; @spec MCP-OP-EDIT-013
  ;; @spec MCP-OP-EDIT-014
  ;; @spec MCP-OP-EDIT-015
  ;; @spec MCP-OP-EDIT-016
  (loop [remaining change-indexes
         changes (:changes spec)
         normalized []]
    (if-let [index (first remaining)]
      (let [change (get changes index)
            source (get sources (first (:in change)))
            already-explicit? (or (:owner change) (:forms change))
            lowered
            (cond
              (:owner change) nil
              (:forms change)
              (lower-namespace-name-in-form source change index)
              :else
              (or (lower-namespace-clause source change index)
                  (lower-complete-named-owner source change index)))]
        (cond
          lowered
          (recur (next remaining)
                 (assoc changes index (:change lowered))
                 (conj normalized (:evidence lowered)))

          already-explicit?
          (recur (next remaining) changes normalized)

          :else
          (unresolved change index)))
      (cond-> {:ok true
               :spec (assoc spec :changes changes)}
        (seq normalized)
        (assoc :location-normalization
               {:count (count normalized)
                :edits normalized})))))
