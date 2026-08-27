(ns clj-surgeon.binding-rename
  "Compile a local binding rename from clj-kondo binding identities.

  This namespace does not write source. It returns exact, lossless replacement
  targets for the transaction kernel."
  (:require
   [clj-surgeon.mcp-process :as process-env]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(defn- refuse!
  [error-type message & [data]]
  (throw (ex-info message (merge {:error-type error-type} data))))

(defn- source-language
  [file]
  (or (some->> (re-find #"\.([^.]+)$" file) second)
      "clj"))

(defn analyze-source
  "Return clj-kondo local-binding analysis for one exact source snapshot."
  [file source]
  (let [working-directory (or (some-> file io/file .getAbsoluteFile .getParent)
                              ".")
        config (pr-str {:output {:format :edn
                                 :analysis {:locals true
                                            :keywords true}}})
        command ["clj-kondo" "--lint" "-"
                 "--filename" file
                 "--lang" (source-language file)
                 "--config" config]
        {:keys [finished? exit out err admission]}
        (try
          (process-env/run-bounded!
            {:command command
             :cwd working-directory
             :timeout-ms 120000
             :stdin-text source
             :visible-byte-limit (* 1024 1024)})
          (catch Exception error
            (refuse! :analyzer-authority-unverified
                     "clj-kondo binding-analysis authority is unavailable"
                     {:file file
                      :cause-error-type (:error-type (ex-data error))})))
        result (try
                 (edn/read-string out)
                 (catch Exception _ nil))
        analysis (:analysis result)]
    (when-not (= :admitted (:status admission))
      (refuse! :analyzer-authority-unverified
               "clj-kondo binding-analysis authority is unverified"
               {:file file :admission admission}))
    (when-not (and finished? (zero? exit))
      (refuse! :binding-analysis-failed
               "clj-kondo binding analysis did not complete successfully"
               {:file file :exit exit :diagnostic (str/trim (or err ""))}))
    (when-not (map? analysis)
      (refuse! :binding-analysis-failed
               "clj-kondo did not return local binding analysis"
               {:file file :exit exit :diagnostic (str/trim (or err ""))}))
    analysis))

(defn- line-offsets
  [source]
  (loop [offsets [0]
         index 0]
    (if-let [newline (str/index-of source "\n" index)]
      (recur (conj offsets (inc newline)) (inc newline))
      offsets)))

(defn- absolute-span
  [offsets {:keys [row col end-row end-col]}]
  [(+ (nth offsets (dec row)) (dec col))
   (+ (nth offsets (dec end-row)) (dec end-col))])

(defn- all-locations
  [source]
  (let [first-form (z/of-string source {:track-position? true})]
    (take-while (complement z/end?) (iterate z/next first-form))))

(defn- exact-location
  [locations {:keys [row col]}]
  (first
    (filter
      (fn [location]
        (let [{node-row :row node-col :col} (meta (z/node location))]
          (and (= row node-row) (= col node-col))))
      locations)))

(defn- ancestor-with-tag
  [location tag]
  (first
    (filter #(= tag (some-> % z/node n/tag))
            (take-while some? (iterate z/up location)))))

(defn- position-inside?
  [{outer-row :row outer-col :col outer-end-row :end-row
    outer-end-col :end-col}
   {inner-row :row inner-col :col inner-end-row :end-row
    inner-end-col :end-col}]
  (and (or (< outer-row inner-row)
           (and (= outer-row inner-row) (<= outer-col inner-col)))
       (or (> outer-end-row inner-end-row)
           (and (= outer-end-row inner-end-row)
                (>= outer-end-col inner-end-col)))))

(defn- inside-owner?
  [{:keys [line end-line]} {:keys [row end-row]}]
  (and (<= line row) (<= end-row end-line)))

(defn- binding-directive
  [binding-location]
  (let [vector-location (ancestor-with-tag binding-location :vector)
        map-location (some-> vector-location z/up)]
    (when (= :map (some-> map-location z/node n/tag))
      {:directive (some-> vector-location z/left z/sexpr)
       :vector-location vector-location
       :map-location map-location})))

(defn- comment-sensitive?
  [vector-location]
  (let [source (z/string vector-location)]
    (or (str/includes? source ";")
        (str/includes? source "#_"))))

(defn- replace-spans
  [source replacements]
  (reduce
    (fn [result {:keys [start end replacement]}]
      (str (subs result 0 start) replacement (subs result end)))
    source
    (sort-by :start > replacements)))

(defn- destructuring-target
  [source offsets binder usages directive-info from to]
  (let [{:keys [directive vector-location map-location]} directive-info]
    (when-not (= :keys directive)
      (refuse! :unsupported-binding-destructuring
               "Binding rename supports :keys destructuring, not :strs or :syms"
               {:binding from :directive directive}))
    (when (comment-sensitive? vector-location)
      (refuse! :comment-sensitive-binding
               "The selected :keys vector contains a comment whose attachment could move"
               {:binding from}))
    (let [map-meta (meta (z/node map-location))
          map-source (z/string map-location)
          [map-start map-end] (absolute-span offsets map-meta)
          inside-usages (filter #(position-inside? map-meta %) usages)
          positions (cons binder inside-usages)
          replacements
          (mapv
            (fn [position]
              (let [[start end] (absolute-span offsets position)]
                {:start (- start map-start)
                 :end (- end map-start)
                 :replacement (if (= position binder) "" (str to))}))
            positions)
          renamed (replace-spans map-source replacements)
          closing (str/last-index-of renamed "}")
          binder-source (subs source
                              (first (absolute-span offsets binder))
                              (second (absolute-span offsets binder)))
          explicit-pair (str " " to " :" binder-source)
          after (str (subs renamed 0 closing)
                     explicit-pair
                     (subs renamed closing))]
      {:row (:row map-meta)
       :col (:col map-meta)
       :before map-source
       :after after
       :occurrence-count (count positions)
       :covered-usage-ids (set (map (juxt :row :col) inside-usages))
       :map-end map-end})))

(defn- symbol-target
  [source offsets position replacement]
  (let [[start end] (absolute-span offsets position)]
    {:row (:row position)
     :col (:col position)
     :before (subs source start end)
     :after (str replacement)
     :occurrence-count 1}))

(defn compile-targets
  "Compile exact replacement targets for one binding rename.

  `owners` are exact top-level form records. `analysis` is clj-kondo analysis
  for the same source snapshot. The result contains no writes."
  [{:keys [file source owners analysis from to preserve-external-key]}]
  (when-not (and (symbol? from) (nil? (namespace from))
                 (symbol? to) (nil? (namespace to))
                 (not= from to))
    (refuse! :invalid-binding-rename
             "Binding names must be distinct unqualified symbols"
             {:from from :to to}))
  (when-not (= true preserve-external-key)
    (refuse! :unsafe-binding-rename
             "Binding rename requires preserve_external_key=true"))
  (let [locations (all-locations source)
        offsets (line-offsets source)
        locals (vec (:locals analysis))
        usages (vec (:local-usages analysis))
        binders-by-owner
        (into {}
              (map
                (fn [owner]
                  [(:name owner)
                   (filterv #(and (= from (:name %))
                                  (inside-owner? owner %))
                            locals)]))
              owners)
        target-collisions
        (into {}
              (keep
                (fn [owner]
                  (let [matches (filterv #(and (= to (:name %))
                                               (inside-owner? owner %))
                                         locals)]
                    (when (seq matches) [(:name owner) matches]))))
              owners)]
    (doseq [[owner binders] binders-by-owner]
      (when-not (= 1 (count binders))
        (refuse! :binding-identity-ambiguous
                 "Each owner must contain exactly one selected local binding"
                 {:file file :owner owner :binding from
                  :actual-count (count binders)})))
    (when (seq target-collisions)
      (refuse! :binding-capture-risk
               "The destination local name already exists in a selected owner"
               {:file file :binding to
                :owners (vec (keys target-collisions))}))
    (let [compiled
          (mapv
            (fn [owner]
              (let [binder (first (get binders-by-owner (:name owner)))
                    binder-id (:id binder)
                    binding-usages (filterv #(= binder-id (:id %)) usages)
                    binding-location (exact-location locations binder)]
                (when-not binding-location
                  (refuse! :binding-source-drift
                           "Binding analysis does not identify an exact source node"
                           {:file file :owner (:name owner) :binding from
                            :row (:row binder) :col (:col binder)}))
                (let [directive-info (binding-directive binding-location)
                      destructuring
                      (when directive-info
                        (destructuring-target source offsets binder binding-usages
                                              directive-info from to))
                      covered (or (:covered-usage-ids destructuring) #{})
                      remaining-usages
                      (remove #(contains? covered [(:row %) (:col %)])
                              binding-usages)
                      targets
                      (vec
                        (concat
                          (if destructuring
                            [(dissoc destructuring :covered-usage-ids :map-end)]
                            [(symbol-target source offsets binder to)])
                          (map #(symbol-target source offsets % to)
                               remaining-usages)))]
                  {:owner (:name owner)
                   :binding-id binder-id
                   :occurrence-count (inc (count binding-usages))
                   :targets targets})))
            owners)]
      {:ok true
       :binding-count (count compiled)
       :occurrence-count (reduce + (map :occurrence-count compiled))
       :per-form-counts (into {} (map (juxt :owner :occurrence-count) compiled))
       :targets
       (vec
         (mapcat
           (fn [{:keys [owner targets]}]
             (map #(assoc % :owner owner) targets))
           compiled))})))
