(ns clj-surgeon.structural-lens
  "Exact, fail-closed structural search and replacement below a named form."
  (:require
   [clj-surgeon.cljc.walk :as cwalk]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.forms :as forms]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z])
  (:import
   (java.security MessageDigest)))

(def plan-version 1)
(def tool-version "0.1.0")
(def max-query-steps 32)
(def query-result-limit 100)
(def supported-platform-file-extensions [".clj" ".cljs" ".cljc"])
(def supported-query-steps
  [[:form 'NAME]
   [:find 'PATTERN]
   [:where {:tag :TAG}]
   [:where {:parent-tag :TAG}]
   :right :left :up :down :outermost :initializer
   [:span 'POSITIVE-COUNT]
   [:partition-all 'POSITIVE-COUNT]
   [:replace 'FORM]
   [:replace-span 'FORM 'FORM]])

(defn source-hash [source]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes source "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- one-complete-form [value error-type label]
  (when (nil? value)
    (throw (ex-info (str label " is required") {:error-type error-type})))
  (let [source (if (string? value) value (pr-str value))]
    (try
      (let [root (z/of-string source {:track-position? true})
            forms (->> (iterate z/right root) (take-while some?) vec)]
        (when-not (= 1 (count forms))
          (throw (ex-info (str label " must contain exactly one complete form")
                          {:error-type error-type})))
        {:sexpr (z/sexpr (first forms))
         :source (z/string (first forms))})
      (catch clojure.lang.ExceptionInfo e
        (if (= error-type (:error-type (ex-data e)))
          (throw e)
          (throw (ex-info (str "Invalid " (str/lower-case label) ": " (.getMessage e))
                          {:error-type error-type}))))
      (catch Exception e
        (throw (ex-info (str "Invalid " (str/lower-case label) ": " (.getMessage e))
                        {:error-type error-type}))))))

(defn- wildcard-match? [pattern candidate]
  (cond
    (= '_ pattern) true
    (and (map? pattern) (map? candidate))
    (and (= (set (keys pattern)) (set (keys candidate)))
         (every? (fn [[k v]] (wildcard-match? v (get candidate k))) pattern))
    (and (sequential? pattern) (sequential? candidate))
    (and (= (count pattern) (count candidate))
         (every? true? (map wildcard-match? pattern candidate)))
    (and (set? pattern) (set? candidate)) (= pattern candidate)
    :else (= pattern candidate)))

(defn- zipper-locations [zloc]
  (->> (iterate z/next zloc) (take-while #(and % (not (z/end? %))))))

(defn- top-level-locations [zloc]
  (->> (iterate z/right zloc) (take-while some?)))

(defn- defining-form-name [zloc]
  (when (z/list? zloc)
    (let [head (some-> zloc z/down z/sexpr str)]
      (when (forms/defining-form? head)
        (some-> zloc z/down z/right z/sexpr str)))))

(defn- inside-range [zloc inside]
  (some (fn [candidate]
          (when (= (str inside) (defining-form-name candidate))
            (let [{:keys [row end-row]} (meta (z/node candidate))]
              {:row row :end-row end-row})))
        (top-level-locations zloc)))

(defn- within-range? [{:keys [row end-row]} zloc]
  (let [{node-row :row node-end-row :end-row} (meta (z/node zloc))]
    (and node-row node-end-row (<= row node-row) (<= node-end-row end-row))))

(defn- enclosing-form-name [top-levels candidate]
  (some (fn [top-level]
          (let [{:keys [row end-row]} (meta (z/node top-level))]
            (when (within-range? {:row row :end-row end-row} candidate)
              (defining-form-name top-level))))
        top-levels))

(defn- node-head [zloc]
  (when-let [child (z/down zloc)]
    (try (z/sexpr child) (catch Exception _ nil))))

(defn- semantic-path [zloc inside]
  (loop [current zloc child nil path '()]
    (if-not current
      (vec path)
      (let [head (node-head current)
            descriptor
            (cond
              (z/list? current)
              (if (and inside (= (str inside) (defining-form-name current)))
                {:form (symbol (str inside))}
                (when head {:call head}))
              (z/vector? current)
              (if (keyword? head)
                {:vector-tag head}
                (let [left (some-> child z/left)
                      binding (when left (try (z/sexpr left) (catch Exception _ nil)))]
                  (if (symbol? binding) {:binding binding} {:vector true})))
              (z/map? current)
              (let [left (some-> child z/left)
                    key (when left (try (z/sexpr left) (catch Exception _ nil)))]
                (if (keyword? key) {:attr key} {:map true}))
              :else nil)]
        (recur (z/up current) current (cond-> path descriptor (conj descriptor)))))))

(def ^:private navigation-steps #{:right :left :up :down})
(def ^:private where-keys #{:tag :parent-tag})

(defn- invalid-query!
  ([message]
   (invalid-query! message {}))
  ([message data]
   (throw (ex-info message
                   (merge {:error-type :invalid-query
                           :supported-query-steps supported-query-steps}
                          data)))))

(defn- parse-query [query allow-transform?]
  (let [parsed (cond
                 (vector? query) query
                 (string? query)
                 (if (str/blank? query)
                   (invalid-query! "Query must be a nonempty EDN vector")
                   (:sexpr (one-complete-form query :invalid-query "Query")))
                 :else (invalid-query! "Query must be a nonempty EDN vector"))]
    (when-not (vector? parsed)
      (invalid-query! "Query must be an EDN vector"))
    (when (empty? parsed)
      (invalid-query! "Query must contain at least one step"))
    (when (> (count parsed) max-query-steps)
      (invalid-query! (str "Query exceeds the " max-query-steps " step limit")
                      {:step-count (count parsed)
                       :max-query-steps max-query-steps}))
    (let [transform-indexes (keep-indexed
                              (fn [index step]
                                (when (and (vector? step)
                                           (#{:replace :replace-span} (first step)))
                                  index))
                              parsed)]
      (when (and (seq transform-indexes) (not allow-transform?))
        (invalid-query! "Read queries cannot contain a transformation"
                        {:step-index (first transform-indexes)
                         :step (nth parsed (first transform-indexes))}))
      (when (> (count transform-indexes) 1)
        (invalid-query! "A query may contain only one transformation"
                        {:step-index (second transform-indexes)
                         :step (nth parsed (second transform-indexes))}))
      (when (and (seq transform-indexes)
                 (not= (first transform-indexes) (dec (count parsed))))
        (invalid-query! "A transformation must be the final query step"
                        {:step-index (first transform-indexes)
                         :step (nth parsed (first transform-indexes))})))
    (doseq [[index step] (map-indexed vector parsed)]
      (cond
        (navigation-steps step) nil

        (#{:outermost :initializer} step) nil

        (not (vector? step))
        (invalid-query! (str "Unsupported query step: " (pr-str step))
                        {:step-index index :step step})

        (= :form (first step))
        (when-not (and (zero? index)
                       (#{2 3} (count step))
                       (or (symbol? (second step))
                           (and (string? (second step))
                                (not (str/blank? (second step)))))
                       (or (= 2 (count step))
                           (keyword? (nth step 2))))
          (invalid-query! "[:form NAME] or [:form NAME PLATFORM] must be first; NAME is a symbol or nonblank string and PLATFORM is a keyword"
                          {:step-index index :step step}))

        (= :find (first step))
        (when-not (= 2 (count step))
          (invalid-query! "[:find PATTERN] requires exactly one pattern"
                          {:step-index index :step step}))

        (= :where (first step))
        (let [predicates (second step)]
          (when-not (and (= 2 (count step))
                         (map? predicates)
                         (seq predicates)
                         (every? where-keys (keys predicates))
                         (every? keyword? (vals predicates)))
            (invalid-query! "[:where PREDICATES] supports nonempty :tag and :parent-tag keyword predicates"
                            {:step-index index :step step})))

        (= :span (first step))
        (let [terminal-index (if (and allow-transform?
                                      (vector? (last parsed))
                                      (= :replace-span (first (last parsed))))
                               (- (count parsed) 2)
                               (dec (count parsed)))]
          (when-not (and (= 2 (count step))
                         (integer? (second step))
                         (pos? (second step))
                         (= index terminal-index))
            (invalid-query! "[:span N] requires a positive integer and must end a read or immediately precede [:replace-span ...]"
                            {:step-index index :step step})))

        (= :partition-all (first step))
        (let [terminal-index (if (and allow-transform?
                                      (vector? (last parsed))
                                      (= :replace-span (first (last parsed))))
                               (- (count parsed) 2)
                               (dec (count parsed)))]
          (when-not (and (= 2 (count step))
                         (integer? (second step))
                         (pos? (second step))
                         (= index terminal-index))
            (invalid-query! "[:partition-all N] requires a positive integer and must end a read or immediately precede [:replace-span ...]"
                            {:step-index index :step step})))

        (= :replace (first step))
        (let [previous (when (pos? index) (nth parsed (dec index)))]
          (when-not (and (= 2 (count step))
                         (not (and (vector? previous)
                                   (#{:span :partition-all}
                                    (first previous)))))
            (invalid-query! "[:replace FORM] requires exactly one node selection and one replacement form"
                            {:step-index index :step step})))

        (= :replace-span (first step))
        (let [selection-step (when (pos? index) (nth parsed (dec index)))
              selection-op (when (vector? selection-step)
                             (first selection-step))
              span-count (when (= :span selection-op)
                           (second selection-step))
              replacement-count (dec (count step))]
          (when-not (and (#{:span :partition-all} selection-op)
                         (pos? replacement-count))
            (invalid-query! "[:replace-span FORM ...] must immediately follow [:span N] or [:partition-all N] and contain replacement forms"
                            {:step-index index :step step}))
          (when (and span-count (not= span-count replacement-count))
            (throw (ex-info (str "Span contains " span-count
                                 " forms but replacement contains " replacement-count)
                            {:error-type :span-arity-mismatch
                             :span-count span-count
                             :replacement-count replacement-count
                             :step-index index
                             :step step}))))

        :else
        (invalid-query! (str "Unsupported query step: " (pr-str step))
                        {:step-index index :step step})))
    parsed))

(defn- location-key [zloc]
  (let [{:keys [row col end-row end-col]} (meta (z/node zloc))]
    [row col end-row end-col (z/tag zloc)]))

(defn- unique-items [items]
  (second
    (reduce (fn [[seen result] item]
              (let [address (if (= :span (:kind item))
                              [:span (:addresses item)]
                              (:address item))]
                (if (contains? seen address)
                  [seen result]
                  [(conj seen address) (conj result item)])))
            [#{} []]
            items)))

(defn- outermost-items [items]
  (let [items (unique-items items)
        current-locations (set (map (comp location-key :zloc) items))]
    (filterv (fn [{:keys [zloc]}]
               (not-any? #(contains? current-locations (location-key %))
                         (take-while some? (iterate z/up (z/up zloc)))))
             items)))

(defn- semantic-span [{:keys [by-location]} {:keys [zloc]} span-count]
  (let [zlocs (->> (iterate z/right zloc)
                   (take span-count)
                   (take-while some?)
                   vec)]
    (when (= span-count (count zlocs))
      (let [items (mapv #(get by-location (location-key %)) zlocs)]
        (when (every? some? items)
          {:kind :span
           :zloc (first zlocs)
           :zlocs zlocs
           :address (:address (first items))
           :addresses (mapv :address items)})))))

(defn- semantic-partitions [{:keys [by-location]} {:keys [zloc]} size]
  (->> (iterate z/right zloc)
       (take-while some?)
       (partition-all size)
       (map-indexed
         (fn [index zlocs]
           (let [zlocs (vec zlocs)
                 items (mapv #(get by-location (location-key %)) zlocs)]
             (when (every? some? items)
               {:kind :span
                :zloc (first zlocs)
                :zlocs zlocs
                :address (:address (first items))
                :addresses (mapv :address items)
                :partition {:size size
                            :index index
                            :complete? (= size (count zlocs))}}))))
       (keep identity)))

(defn- raw-span-source [zlocs]
  (let [end-key (location-key (last zlocs))]
    (loop [current (first zlocs) result ""]
      (let [result (str result (z/string current))]
        (if (= end-key (location-key current))
          result
          (recur (z/right* current) result))))))

(defn- raw-between [left right]
  (let [end-key (location-key right)]
    (loop [current (z/right* left) result ""]
      (if (= end-key (location-key current))
        result
        (recur (z/right* current) (str result (z/string current)))))))

(defn- span-gaps [zlocs]
  (mapv (fn [[left right]] (raw-between left right))
        (partition 2 1 zlocs)))

(defn platform-context-for-file
  "Return the platforms valid for ordinary forms in a known Clojure source
   file. Nil means the file context is absent or unsupported. Pure."
  [file]
  (when file
    (case (some->> (re-find #"(?i)\.([^.\\/]+)$" (str file))
                   second
                   str/lower-case)
      "clj" #{:clj}
      "cljs" #{:cljs}
      "cljc" #{:clj :cljs}
      nil)))

(defn- source-index [root default-platforms]
  (let [locations (vec (zipper-locations root))
        entries (mapv (fn [address zloc]
                        {:address address :zloc zloc})
                      (range)
                      locations)
        walked (->> (cwalk/top-level-forms-from-zloc root default-platforms)
                    (mapv #(update % :platforms set/intersection
                                   default-platforms)))
        walked-by-location (into {}
                                 (map (fn [{:keys [zloc platforms]}]
                                        [(location-key zloc) platforms]))
                                 walked)
        entries (mapv (fn [item]
                        (if-let [platforms (get walked-by-location
                                                (location-key (:zloc item)))]
                          (assoc item :platforms platforms)
                          item))
                      entries)
        by-location (into {} (map (fn [item]
                                    [(location-key (:zloc item)) item])
                                  entries))
        ordinary-top-levels (top-level-locations root)
        walked-top-levels (map :zloc walked)]
    {:entries entries
     :by-location by-location
     :top-levels (vec walked-top-levels)
     :initial (unique-items
                (into []
                      (keep #(get by-location (location-key %)))
                      (concat ordinary-top-levels walked-top-levels)))}))

(defn- subtree-items [entries {:keys [address zloc]}]
  (let [subtree-size (count (zipper-locations (z/subzip zloc)))
        end (min (count entries) (+ address subtree-size))]
    (subvec entries address end)))

(defn- matching-descendants [entries item pattern]
  (keep (fn [candidate]
          (when (try
                  (wildcard-match? pattern (z/sexpr (:zloc candidate)))
                  (catch Exception _ false))
            candidate))
        (subtree-items entries item)))

(defn- where-match? [zloc predicates]
  (and (if-let [tag (:tag predicates)]
         (= tag (z/tag zloc))
         true)
       (if-let [parent-tag (:parent-tag predicates)]
         (= parent-tag (some-> zloc z/up z/tag))
         true)))

(defn- navigate [zloc step]
  (case step
    :right (z/right zloc)
    :left (z/left zloc)
    :up (z/up zloc)
    :down (z/down zloc)))

(defn- def-initializer
  [by-location {:keys [zloc]}]
  (let [children (loop [child (z/down zloc) result []]
                   (if child
                     (recur (z/right child) (conj result child))
                     result))
        head (some-> children first z/sexpr)]
    (when (and (= 'def head) (<= 3 (count children)))
      (get by-location (location-key (peek children))))))

(defn- apply-query-step [{:keys [entries by-location]} items step]
  (unique-items
    (cond
      (navigation-steps step)
      (keep (fn [item]
              (some->> (navigate (:zloc item) step)
                       location-key
                       (get by-location)))
            items)

      (= :outermost step)
      (outermost-items items)

      (= :initializer step)
      (keep #(def-initializer by-location %) items)

      (= :form (first step))
      (let [[_ name platform] step]
        (filter #(and (= (str name) (defining-form-name (:zloc %)))
                      (or (nil? platform)
                          (contains? (:platforms %) platform)))
                items))

      (= :find (first step))
      (mapcat #(matching-descendants entries % (second step)) items)

      (= :where (first step))
      (filter #(where-match? (:zloc %) (second step)) items)

      (= :span (first step))
      (keep #(semantic-span {:by-location by-location} % (second step)) items)

      (= :partition-all (first step))
      (mapcat #(semantic-partitions {:by-location by-location}
                                    % (second step))
              items))))

(defn- query-match
  [top-levels {:keys [address addresses kind partition zloc zlocs]}]
  (let [{:keys [row]} (meta (z/node zloc))
        {end-row :end-row} (meta (z/node (or (last zlocs) zloc)))
        owner (enclosing-form-name top-levels zloc)]
    (cond-> {:path (cond-> (semantic-path zloc owner)
                     (= :span kind)
                     (conj (if partition
                             {:partition-all
                              {:size (:size partition)
                               :index (:index partition)
                               :count (count zlocs)}}
                             {:span {:count (count zlocs)}})))
             :tag (if (= :span kind) :span (z/tag zloc))
             :address (if (= :span kind)
                        {:preorders addresses}
                        {:preorder address})
             :line row
             :end-line end-row
             :source (if (= :span kind) (raw-span-source zlocs) (z/string zloc))}
      (= :span kind) (assoc :count (count zlocs)
                            :forms (mapv z/string zlocs)
                            :gaps (span-gaps zlocs))
      partition (assoc :partition partition)
      owner (assoc :inside owner))))

(defn- query-error-result [source query exception]
  (merge {:operation :lens
          :query query
          :error (.getMessage exception)
          :error-type (or (:error-type (ex-data exception)) :invalid-source)
          :match-count 0
          :matches []
          :source-hash (source-hash source)}
         (select-keys (ex-data exception)
                      [:step-index :step :step-count :max-query-steps
                       :supported-query-steps :span-count
                       :replacement-count :file :platform
                       :supported-file-extensions])))

(defn evaluate-query
  "Evaluate a read-only EDN zipper pipeline against source. Pure: source and
   query data/string and optional file context in; bounded match records and a
   per-step trace out. Platform-qualified form selection requires a known
   .clj, .cljs, or .cljc file context."
  ([source query]
   (evaluate-query source query nil))
  ([source query {:keys [file]}]
   (try
     (let [query (parse-query query false)
           platform (when (and (vector? (first query))
                               (= :form (ffirst query))
                               (= 3 (count (first query))))
                      (nth (first query) 2))
           file-platforms (platform-context-for-file file)]
       (when (and platform (nil? file-platforms))
         (throw (ex-info
                  "Platform-qualified form selection requires a .clj, .cljs, or .cljc file"
                  {:error-type :platform-context-required
                   :file file
                   :platform platform
                   :supported-file-extensions supported-platform-file-extensions})))
       (let [default-platforms (or file-platforms #{:clj :cljs})
             root (z/of-string source {:track-position? true})
             {:keys [initial top-levels] :as index}
             (source-index root default-platforms)
             {:keys [items trace]}
             (reduce (fn [{:keys [items trace]} step]
                       (let [next-items (apply-query-step index items step)]
                         {:items next-items
                          :trace (conj trace {:step step
                                              :input-count (count items)
                                              :output-count (count next-items)})}))
                     {:items initial :trace []}
                     query)
             matches (mapv #(query-match top-levels %) items)
             match-count (count matches)]
         (cond-> {:operation :lens
                  :query query
                  :trace trace
                  :match-count match-count
                  :matches (vec (take query-result-limit matches))
                  :source-hash (source-hash source)}
           (> match-count query-result-limit)
           (assoc :matches-truncated? true))))
     (catch Exception e
       (query-error-result source query e)))))

(defn find-subforms [source {:keys [inside match]}]
  (try
    (let [{pattern :sexpr match-source :source}
          (one-complete-form match :invalid-match "Match")
          root (z/of-string source {:track-position? true})
          top-levels (vec (top-level-locations root))
          range (when inside (inside-range root inside))]
      (if (and inside (nil? range))
        {:error (str "Enclosing form not found: " inside)
         :error-type :inside-not-found
         :inside (str inside) :match match-source :match-count 0 :matches []
         :source-hash (source-hash source)}
        (let [matches (->> (zipper-locations root)
                           (map-indexed vector)
                           (keep (fn [[index candidate]]
                                   (when (and (or (nil? range) (within-range? range candidate))
                                              (try (wildcard-match? pattern (z/sexpr candidate))
                                                   (catch Exception _ false)))
                                     (let [{:keys [row end-row]} (meta (z/node candidate))
                                           owner (enclosing-form-name top-levels candidate)]
                                       (cond-> {:path (semantic-path candidate inside)
                                                :address {:preorder index}
                                                :line row :end-line end-row
                                                :source (z/string candidate)}
                                         owner (assoc :inside owner))))))
                           vec)]
          {:inside (when inside (str inside)) :match match-source
           :match-count (count matches) :matches matches
           :source-hash (source-hash source)})))
    (catch Exception e
      {:error (.getMessage e)
       :error-type (or (:error-type (ex-data e)) :invalid-source)
       :match-count 0 :matches [] :source-hash (source-hash source)})))

(defn find-file [{:keys [file] :as opts}]
  (if-not file
    {:error ":file is required" :error-type :missing-arguments}
    (assoc (find-subforms (slurp file) opts) :file file)))

(defn- prefixed-lines [prefix source]
  (str/join "\n" (map #(str prefix %) (str/split source #"\n" -1))))

(defn- edit-diff [file {:keys [line before after]}]
  (let [before-count (count (str/split before #"\n" -1))
        after-count (count (str/split after #"\n" -1))
        absolute? (.isAbsolute (io/file file))
        before-file (if absolute? file (str "a/" file))
        after-file (if absolute? file (str "b/" file))]
    (str "--- " before-file "\n+++ " after-file "\n"
         "@@ -" line "," before-count " +" line "," after-count " @@\n"
         (prefixed-lines "-" before) "\n" (prefixed-lines "+" after) "\n")))

(defn- replacement-node [source]
  (z/node (z/of-string source)))

(defn- replace-at-address [source address before after]
  (let [root (z/of-string source {:track-position? true})
        target (nth (zipper-locations root) address nil)]
    (when-not target
      (throw (ex-info (str "Planned address no longer exists: " address)
                      {:error-type :stale-path})))
    (when-not (= before (z/string target))
      (throw (ex-info "Source at planned address does not match edit"
                      {:error-type :stale-subform})))
    (let [{replacement-source :source}
          (one-complete-form after :invalid-result-source "Replacement result")]
      (-> target (z/replace (replacement-node replacement-source)) z/root-string))))

(defn- apply-span-edit [source {:keys [addresses before-forms after-forms]}]
  (when-not (and (= (count addresses) (count before-forms))
                 (= (count addresses) (count after-forms))
                 (pos? (count addresses)))
    (throw (ex-info "Span edit addresses and forms must have equal positive counts"
                    {:error-type :invalid-plan})))
  (reduce (fn [current [address before after]]
            (replace-at-address current address before after))
          source
          (sort-by first > (map vector addresses before-forms after-forms))))

(defn apply-plan [source {:keys [source-hash edits result-hash operation] :as plan}]
  (cond
    (not= plan-version (:plan-version plan))
    {:error (str "Unsupported plan version: " (pr-str (:plan-version plan)))
     :error-type :unsupported-plan-version :supported-plan-version plan-version}
    (not (#{:replace-subform :replace-span} operation))
    {:error (str "Unsupported plan operation: " (pr-str operation))
     :error-type :unsupported-plan-operation}
    (not= source-hash (clj-surgeon.structural-lens/source-hash source))
    {:error "Source hash does not match plan" :error-type :source-hash-mismatch}
    (not= 1 (count edits))
    {:error (str "Expected exactly one planned edit, found " (count edits))
     :error-type :invalid-plan}
    :else
    (try
      (let [{:keys [address before after] :as edit} (first edits)
            updated (if (= :replace-span operation)
                      (apply-span-edit source edit)
                      (replace-at-address source (:preorder address) before after))
            ;; Validate the whole future file, not only replacement nodes.
            _ (z/of-string updated {:track-position? true})
            actual-result-hash (clj-surgeon.structural-lens/source-hash updated)]
        (if (and result-hash (not= result-hash actual-result-hash))
          {:error "Result hash does not match plan" :error-type :result-hash-mismatch}
          {:ok true :source updated :result-hash actual-result-hash}))
      (catch Exception e
        {:error (.getMessage e)
         :error-type (or (:error-type (ex-data e)) :invalid-result-source)}))))

(defn- build-replacement-plan
  [source found selector replacement-source file extra-fields]
  (let [matched (first (:matches found))
        edit {:path (:path matched) :address (:address matched)
              :line (:line matched) :before (:source matched)
              :after replacement-source}
        provisional (merge {:plan-version plan-version
                            :operation :replace-subform
                            :file file
                            :selector selector
                            :source-hash (:source-hash found)
                            :match-count 1
                            :edits [edit]
                            :diff (edit-diff (or file "source.clj") edit)}
                           extra-fields)
        applied (apply-plan source provisional)]
    (if (:error applied)
      applied
      (let [result-hash (:result-hash applied)]
        (assoc provisional
               :result-hash result-hash
               :provenance {:tool "clj-surgeon"
                            :tool-version tool-version
                            :operation :replace-subform
                            :selector selector
                            :source-hash (:source-hash found)
                            :result-hash result-hash})))))

(defn- span-source-from-parts [forms gaps]
  (apply str
         (mapcat (fn [index form]
                   (cond-> [form]
                     (< index (count gaps)) (conj (nth gaps index))))
                 (range)
                 forms)))

(defn- build-span-plan [source found selector replacement-sources file]
  (let [matched (first (:matches found))
        edit {:path (:path matched)
              :address (:address matched)
              :addresses (get-in matched [:address :preorders])
              :line (:line matched)
              :before-forms (:forms matched)
              :after-forms replacement-sources
              :before (:source matched)
              :after (span-source-from-parts replacement-sources (:gaps matched))}
        provisional {:plan-version plan-version
                     :operation :replace-span
                     :file file
                     :selector selector
                     :source-hash (:source-hash found)
                     :match-count 1
                     :query-trace (:trace found)
                     :edits [edit]
                     :diff (edit-diff (or file "source.clj") edit)}
        applied (apply-plan source provisional)]
    (if (:error applied)
      applied
      (let [result-hash (:result-hash applied)]
        (assoc provisional
               :result-hash result-hash
               :provenance {:tool "clj-surgeon"
                            :tool-version tool-version
                            :operation :replace-span
                            :selector selector
                            :source-hash (:source-hash found)
                            :result-hash result-hash})))))

(defn plan-replacement [source {:keys [inside match with file]}]
  (try
    (let [{replacement-source :source}
          (one-complete-form with :invalid-replacement "Replacement")
          found (find-subforms source {:inside inside :match match})
          match-count (:match-count found)]
      (cond
        (:error found) found
        (not= 1 match-count)
        (assoc found :error (str "Expected exactly one match, found " match-count)
               :error-type (if (zero? match-count) :no-match :ambiguous-match))
        :else
        (let [selector {:inside (:inside found) :match (:match found)
                        :expected-match-count 1}]
          (build-replacement-plan source found selector replacement-source file {}))))
    (catch Exception e
      {:error (.getMessage e)
       :error-type (or (:error-type (ex-data e)) :invalid-replacement)})))

(defn evaluate-lens
  "Evaluate a getter/updater lens expression. Navigation-only queries return
   read evidence; a terminal replacement returns a guarded plan. Pure."
  [source {:keys [query file]}]
  (try
    (let [query (parse-query query true)
          transform (when (and (vector? (last query))
                               (#{:replace :replace-span}
                                (first (last query))))
                      (last query))]
      (if-not transform
        (cond-> (evaluate-query source query {:file file})
          file (assoc :file file))
        (let [selection-query (pop query)
              replacement-sources
              (mapv (fn [replacement]
                      (:source (one-complete-form replacement
                                                  :invalid-replacement
                                                  "Replacement")))
                    (rest transform))
              found (evaluate-query source selection-query {:file file})
              match-count (:match-count found)]
          (cond
            (:error found) found
            (not= 1 match-count)
            (assoc found
                   :error (str "Expected exactly one match, found " match-count)
                   :error-type (if (zero? match-count)
                                 :no-match
                                 :ambiguous-match))
            :else
            (let [selector {:query query
                            :selection-query selection-query
                            :expected-match-count 1}]
              (if (= :replace-span (first transform))
                (let [span-count (get-in found [:matches 0 :count])
                      replacement-count (count replacement-sources)]
                  (if (not= span-count replacement-count)
                    (assoc found
                           :error (str "Span contains " span-count
                                       " forms but replacement contains "
                                       replacement-count)
                           :error-type :span-arity-mismatch
                           :span-count span-count
                           :replacement-count replacement-count)
                    (build-span-plan source found selector
                                     replacement-sources file)))
                (build-replacement-plan source found selector
                                        (first replacement-sources) file
                                        {:query-trace (:trace found)})))))))
    (catch Exception e
      (query-error-result source query e))))

(def edit-allowed-arguments
  "CLI arguments accepted by the plan-only :edit facade."
  #{:op :file :query :plan-out :expect})

(defn evaluate-edit
  "Pure plan-only facade over evaluate-lens. Successful getter-only queries
   refuse because :edit must include an existing terminal lens transform."
  [source {:keys [file query] :as opts}]
  (let [unsupported (->> (keys opts)
                         (remove edit-allowed-arguments)
                         sort
                         vec)]
    (if (seq unsupported)
      {:operation :edit
       :file file
       :query query
       :error (str "Unsupported :edit arguments: "
                   (str/join ", " unsupported))
       :error-type :unsupported-arguments
       :unsupported unsupported
       :supported (->> edit-allowed-arguments
                       (remove #{:op})
                       sort
                       vec)}
      (let [result (evaluate-lens source opts)]
        (if (and (= :lens (:operation result))
                 (nil? (:error result)))
          {:operation :edit
           :file file
           :query query
           :source-hash (:source-hash result)
           :error ":edit requires a terminal [:replace FORM] or [:replace-span FORM ...] step; use :q for read-only queries"
           :error-type :edit-requires-transform
           :remedy {:read-operation :q
                    :terminal-steps [[:replace 'form]
                                     [:replace-span 'form '...]]}}
          result)))))

(defn- read-form-nodes
  "Every complete form node in source, with whitespace and comment nodes
   dropped. One reader for both sides of an :expect comparison."
  [source]
  (->> (node/children (parser/parse-string-all source))
       (remove node/whitespace-or-comment?)
       vec))

(defn- read-all-forms
  "Every complete form in source, as Clojure data. Whitespace and comments
   are not represented, so this is the whitespace-insensitive reading."
  [source]
  (mapv node/sexpr (read-form-nodes source)))

(defn- lossless-node-fingerprint
  "A rewrite-clj node fingerprint that ignores whitespace but retains comments,
   metadata, reader macros, token spelling, and tree position."
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-node-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn- lossless-source-fingerprint [source]
  (->> (node/children (parser/parse-string-all source))
       (keep lossless-node-fingerprint)
       vec))

(defn parse-expect
  "Parse the caller's declared before-state into exactly one Clojure form.
   Pure: a source string (or an already-read form) in, data out. Returns
   {:expect FORM :expect-source SOURCE} or a refusal carrying
   :error-type :invalid-expect for zero forms, several forms, or a reader
   error."
  [expect]
  (let [source (if (string? expect) expect (pr-str expect))]
    (try
      (let [nodes (read-form-nodes source)]
        (if (= 1 (count nodes))
          {:expect (node/sexpr (first nodes))
           :expect-source (node/string (first nodes))}
          {:error (str ":expect must contain exactly one complete form; found "
                       (count nodes))
           :error-type :invalid-expect
           :form-count (count nodes)}))
      (catch Exception exception
        {:error (str "Invalid :expect: " (.getMessage exception))
         :error-type :invalid-expect}))))

(defn expect-comparison
  "Compare parsed :expect data and syntax with a plan edit's selected source.
   Whitespace is ignored. Comments, metadata, reader macros, and token spelling
   remain part of the guard. A selection that is not exactly one form (a
   :replace-span selection) never matches a single :expect."
  [{:keys [expect expect-source]} before-source]
  (let [forms (try (read-all-forms before-source)
                   (catch Exception _ nil))
        one? (= 1 (count forms))
        actual (if one? (first forms) (vec forms))
        data-match? (boolean (and one? (= expect actual)))
        syntax-match? (boolean
                        (and data-match?
                             (= (lossless-source-fingerprint expect-source)
                                (lossless-source-fingerprint before-source))))
        reason (cond
                 (not one?) :selection-is-not-one-form
                 (not data-match?) :form-mismatch
                 (not syntax-match?) :source-syntax-mismatch)]
    (cond-> {:match? syntax-match?
             :expected expect
             :actual actual
             :actual-source before-source}
      reason (assoc :reason reason)
      (not one?) (assoc :actual-form-count (count forms)))))

(defn expect-mismatch-result
  "Structured refusal for a plan whose selection differs from :expect.
   Pure: plan and comparison in, refusal map out. No plan is saved and no
   source byte changes, so the caller can re-declare :expect and retry."
  [plan comparison]
  (let [edit (first (:edits plan))]
    (cond-> {:operation :edit
             :file (:file plan)
             :mode :expect-guarded
             :error ":expect does not match the selected form; no plan was saved and no bytes changed"
             :error-type :expect-mismatch
             :expected (:expected comparison)
             :actual (:actual comparison)
             :actual-source (:actual-source comparison)
             :selector (:selector plan)
             :source-hash (:source-hash plan)}
      (:line edit) (assoc :line (:line edit))
      (:reason comparison) (assoc :reason (:reason comparison))
      (:actual-form-count comparison)
      (assoc :actual-form-count (:actual-form-count comparison)))))

(declare execute-plan!)

(defn- edn-plan-path? [plan-out]
  (and (string? plan-out)
       (str/ends-with? (str/lower-case plan-out) ".edn")))

(defn- write-plan-file [plan plan-out]
  (if-not (edn-plan-path? plan-out)
    {:operation (:operation plan)
     :file (:file plan)
     :plan-out plan-out
     :error ":plan-out must name an .edn audit artifact"
     :error-type :invalid-plan-out}
    (try
      (file-ops/atomic-write! plan-out (with-out-str (pprint/pprint plan)))
      (assoc plan :plan-out plan-out)
      (catch Exception e
        {:error (str "Could not write plan: " (.getMessage e))
         :error-type :plan-write-failed
         :plan-out plan-out}))))

(defn- canonical-path [file]
  (.getCanonicalPath (io/file file)))

(defn- guarded-plan-source [saved-plan]
  (with-out-str
    (pprint/pprint (dissoc saved-plan :plan-out))))

(defn- inspect-plan-artifact [saved-plan]
  (let [expected-source (guarded-plan-source saved-plan)
        expected-hash (source-hash expected-source)]
    (try
      (let [actual-hash (source-hash (slurp (:plan-out saved-plan)))]
        {:verified (= expected-hash actual-hash)
         :expected-hash expected-hash
         :actual-hash actual-hash})
      (catch Exception exception
        {:verified false
         :expected-hash expected-hash
         :error (.getMessage exception)}))))

(defn- repair-plan-artifact [saved-plan]
  (try
    (file-ops/atomic-write! (:plan-out saved-plan)
                            (guarded-plan-source saved-plan))
    (assoc (inspect-plan-artifact saved-plan) :repaired true)
    (catch Exception exception
      {:verified false
       :repaired false
       :expected-hash (source-hash (guarded-plan-source saved-plan))
       :error (.getMessage exception)})))

(defn- apply-guarded-plan
  "Apply an already-saved plan through the shared :replace-subform! executor
   and merge the plan evidence with its receipt. Verify that the saved audit
   artifact still names this exact plan before and after source mutation."
  [saved-plan]
  (let [before (inspect-plan-artifact saved-plan)]
    (if-not (:verified before)
      {:operation :edit
       :mode :expect-guarded
       :file (:file saved-plan)
       :plan-out (:plan-out saved-plan)
       :error "The saved plan artifact changed before guarded apply; no source bytes changed"
       :error-type :plan-artifact-changed
       :source-unchanged true
       :plan-artifact before}
      (let [receipt (execute-plan! {:plan saved-plan})]
        (if (:error receipt)
          (assoc receipt
                 :mode :expect-guarded
                 :plan-out (:plan-out saved-plan)
                 :plan-artifact (inspect-plan-artifact saved-plan))
          (let [after (inspect-plan-artifact saved-plan)
                artifact (if (:verified after)
                           (assoc after :repaired false)
                           (repair-plan-artifact saved-plan))
                result (assoc (merge saved-plan receipt)
                              :mode :expect-guarded
                              :plan-artifact artifact)]
            (if (:verified artifact)
              result
              (assoc result
                     :ok false
                     :source-applied true
                     :error "Source was applied, but the audit plan artifact could not be restored"
                     :error-type :plan-artifact-repair-failed))))))))

(defn- guarded-edit
  "Save and apply one plan whose selection equals the declared :expect form.
   A mismatch refuses before the plan artifact is written."
  [plan parsed-expect plan-out]
  (let [comparison (expect-comparison parsed-expect
                                      (:before (first (:edits plan))))]
    (if-not (:match? comparison)
      (assoc (expect-mismatch-result plan comparison)
             :plan-out plan-out
             :expect-source (:expect-source parsed-expect))
      (let [saved (write-plan-file plan plan-out)]
        (if (:error saved)
          saved
          (apply-guarded-plan saved))))))

(defn- transform-query? [query]
  (let [terminal (when (vector? query) (peek query))]
    (and (vector? terminal) (= :transform (first terminal)))))

(defn edit-file-with-evaluator
  "Plan one edit with evaluator after path checks and the single source read.
   With :expect, the plan is also applied in the same invocation once the
   selection structurally equals the declared form."
  [{:keys [file plan-out expect] :as opts} evaluator]
  (let [unsupported (seq (remove edit-allowed-arguments (keys opts)))
        parsed-expect (when (contains? opts :expect) (parse-expect expect))]
    (cond
      unsupported
      (evaluate-edit "" opts)

      (= (canonical-path file) (canonical-path plan-out))
      {:operation :edit
       :file file
       :plan-out plan-out
       :error ":plan-out must not resolve to the source file"
       :error-type :plan-overwrites-source}

      (not (edn-plan-path? plan-out))
      {:operation :edit
       :file file
       :plan-out plan-out
       :error ":plan-out must name an .edn audit artifact"
       :error-type :invalid-plan-out}

      (:error parsed-expect)
      (assoc parsed-expect
             :operation :edit
             :file file
             :plan-out plan-out
             :expect expect)

      (and parsed-expect (transform-query? (:query opts)))
      {:operation :edit
       :file file
       :plan-out plan-out
       :mode :expect-guarded
       :error ":expect requires a literal replacement; a computed transform must be planned and reviewed before apply"
       :error-type :expect-requires-literal-replacement
       :remedy {:mode :plan-and-review
                :instruction "Remove :expect, review the concrete diff, then apply the saved plan with :replace-subform!"}}

      :else
      (try
        (let [plan (evaluator (slurp file) opts)]
          (if (and (#{:replace-subform :replace-span} (:operation plan))
                   (nil? (:error plan)))
            (if parsed-expect
              (guarded-edit plan parsed-expect plan-out)
              (write-plan-file plan plan-out))
            plan))
        (catch Exception e
          {:operation :edit
           :file file
           :plan-out plan-out
           :error (str "Cannot plan edit for source file: " file
                       " (" (.getMessage e) ")")
           :error-type :file-read-failed})))))

(defn edit-file
  "Plan exactly one existing lens transformation and atomically save its plan.
   Source bytes are never changed."
  [opts]
  (edit-file-with-evaluator opts evaluate-edit))

(defn lens-file
  "Read a file and evaluate one getter/updater lens. A terminal replacement
   may write a plan artifact; this function never changes source bytes."
  [{:keys [file plan-out] :as opts}]
  (if-not file
    {:operation :lens
     :error ":file is required"
     :error-type :missing-arguments}
    (try
      (let [result (evaluate-lens (slurp file) opts)
            result (cond-> result
                     (= :lens (:operation result)) (assoc :file file))]
        (if (and (#{:replace-subform :replace-span} (:operation result))
                 (nil? (:error result))
                 plan-out)
          (write-plan-file result plan-out)
          result))
      (catch Exception e
        {:operation :lens
         :file file
         :error (str "Cannot read source file: " file " (" (.getMessage e) ")")
         :error-type :file-read-failed}))))

(defn plan-file-replacement [{:keys [file plan-out] :as opts}]
  (if-not file
    {:error ":file is required" :error-type :missing-arguments}
    (let [plan (plan-replacement (slurp file) opts)]
      (if (or (:error plan) (nil? plan-out))
        plan
        (write-plan-file plan plan-out)))))

(defn- read-plan [plan]
  (cond (map? plan) plan
        (string? plan) (edn/read-string (slurp plan))
        :else nil))

(defn verified-apply-receipt
  "Build a machine-readable receipt from a plan and the exact bytes read back
   after its atomic write. Pure: plan and source string in; data out."
  [plan read-back-source]
  (let [read-back-hash (source-hash read-back-source)
        expected-result-hash (:result-hash plan)]
    (if (not= expected-result-hash read-back-hash)
      {:error "Read-back hash does not match the planned result"
       :error-type :read-back-hash-mismatch
       :file (:file plan)
       :expected-result-hash expected-result-hash
       :read-back-hash read-back-hash}
      (try
        (z/of-string read-back-source {:track-position? true})
        {:ok true
         :operation :replace-subform!
         :planned-operation (:operation plan)
         :file (:file plan)
         :source-hash (:source-hash plan)
         :result-hash expected-result-hash
         :applied-edit (first (:edits plan))
         :verified {:whole-file-parsed true
                    :atomic-write true
                    :read-back-hash read-back-hash}}
        (catch Exception e
          {:error (str "Written result could not be reparsed: " (.getMessage e))
           :error-type :read-back-invalid-source
           :file (:file plan)
           :read-back-hash read-back-hash})))))

(defn execute-plan! [{:keys [plan]}]
  (try
    (if-let [plan (read-plan plan)]
      (if-let [file (:file plan)]
        (let [source (slurp file)
              result (apply-plan source plan)]
          (if (:error result)
            result
            (if-let [write-error
                     (try
                       (file-ops/atomic-write! file (:source result))
                       nil
                       (catch Exception e
                         {:error (str "Atomic replacement failed; target was not replaced: "
                                      (.getMessage e))
                          :error-type :atomic-write-failed :file file}))]
              write-error
              (try
                (verified-apply-receipt plan (slurp file))
                (catch Exception e
                  {:error (str "Could not read back the replaced target: "
                               (.getMessage e))
                   :error-type :read-back-failed :file file})))))
        {:error "Plan does not contain :file" :error-type :invalid-plan})
      {:error ":plan must be an EDN plan map or file path" :error-type :invalid-plan})
    (catch Exception e
      {:error (.getMessage e) :error-type :apply-failed})))
