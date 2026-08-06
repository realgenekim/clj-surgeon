(ns clj-surgeon.intent-transaction
  (:require
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z]))

(def transaction-version 1)

(def ^:private supported-extensions [".clj" ".cljs" ".cljc"])
(def ^:private spec-keys #{:intents :expect})
(def ^:private intent-keys #{:files :from :to :expect-count})
(def ^:private expectation-keys
  #{:intent-count :edit-count :changed-file-count})

(defn- refuse!
  [error-type message & [data]]
  (throw (ex-info message (merge {:error-type error-type} data))))

(defn- lossless-node-fingerprint
  "Return a syntax fingerprint that ignores whitespace but retains comments,
   metadata, reader macros, token spelling, and tree position."
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-node-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn- parse-one-form
  [source label]
  (when-not (string? source)
    (refuse! :invalid-intent-form
             (str label " must be a source string")
             {:field label :actual source}))
  (try
    (let [root (parser/parse-string-all source)
          forms (->> (node/children root)
                     (remove node/whitespace-or-comment?)
                     vec)]
      (when-not (= 1 (count forms))
        (refuse! :invalid-intent-form
                 (str label " must contain exactly one complete form")
                 {:field label :form-count (count forms)}))
      (let [form-node (first forms)]
        {:node form-node
         :source (node/string form-node)
         :fingerprint (lossless-node-fingerprint form-node)}))
    (catch clojure.lang.ExceptionInfo e
      (if (:error-type (ex-data e))
        (throw e)
        (refuse! :invalid-intent-form
                 (str "Invalid " label ": " (.getMessage e))
                 {:field label})))
    (catch Exception e
      (refuse! :invalid-intent-form
               (str "Invalid " label ": " (.getMessage e))
               {:field label}))))

(defn- validate-complete-source!
  [file source error-type]
  (when-not (string? source)
    (refuse! error-type
             (str "Source is missing for " file)
             {:file file}))
  (try
    (parser/parse-string-all source)
    source
    (catch Exception e
      (refuse! error-type
               (str "Invalid source in " file ": " (.getMessage e))
               {:file file}))))

(defn- zipper-locations
  [zloc]
  (take-while (complement z/end?) (iterate z/next zloc)))

(defn- node-size
  [form-node]
  (if (node/whitespace-or-comment? form-node)
    0
    (inc (if (node/inner? form-node)
           (reduce + 0 (map node-size (node/children form-node)))
           0))))

(defn- supported-file?
  [file]
  (and (string? file)
       (some #(str/ends-with? file %) supported-extensions)))

(defn- normalized-path
  [file]
  (str (.normalize (java.nio.file.Paths/get file (make-array String 0)))))

(defn- validate-files!
  [files intent-index]
  (when-not (and (vector? files) (seq files))
    (refuse! :invalid-files
             "Intent :files must be a non-empty vector"
             {:intent-index intent-index :files files}))
  (doseq [file files]
    (when-not (supported-file? file)
      (refuse! :unsupported-file
               (str "Unsupported Clojure source file: " (pr-str file))
               {:intent-index intent-index :file file})))
  (let [normalized (mapv normalized-path files)]
    (when-not (= (count normalized) (count (distinct normalized)))
      (refuse! :duplicate-file
               "Intent contains duplicate or aliased file paths"
               {:intent-index intent-index :files files})))
  files)

(defn- validate-expect-count!
  [expected intent-index]
  (when-not (and (integer? expected) (pos? expected))
    (refuse! :invalid-expect-count
             "Intent :expect-count must be a positive integer"
             {:intent-index intent-index :expected-count expected}))
  expected)

(defn- validate-intent!
  [intent intent-index]
  (when-not (map? intent)
    (refuse! :invalid-intents
             "Every intent must be a map"
             {:intent-index intent-index :actual intent}))
  (let [unknown (vec (sort (remove intent-keys (keys intent))))]
    (when (seq unknown)
      (refuse! :unknown-intent-arguments
               (str "Unknown intent arguments: " (str/join ", " unknown))
               {:intent-index intent-index :unknown unknown})))
  (let [files (validate-files! (:files intent) intent-index)
        from (parse-one-form (:from intent) ":from")
        to (parse-one-form (:to intent) ":to")
        expected-count (validate-expect-count! (:expect-count intent)
                                               intent-index)]
    (when (= (:fingerprint from) (:fingerprint to))
      (refuse! :no-op-intent
               "Intent :from and :to are losslessly equal"
               {:intent-index intent-index}))
    {:intent-index intent-index
     :files files
     :from from
     :to to
     :expect-count expected-count}))

(defn- validate-aggregate-expectation!
  [expectation]
  (when-not (map? expectation)
    (refuse! :invalid-transaction-expectation
             "Spec :expect must be a map"
             {:expected-fields expectation-keys}))
  (let [unknown (vec (sort (remove expectation-keys (keys expectation))))
        missing (vec (sort (remove #(contains? expectation %) expectation-keys)))]
    (when (seq unknown)
      (refuse! :invalid-transaction-expectation
               (str "Unknown transaction expectation fields: "
                    (str/join ", " unknown))
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :invalid-transaction-expectation
               (str "Missing transaction expectation fields: "
                    (str/join ", " missing))
               {:missing missing})))
  (doseq [[field value] expectation]
    (when-not (and (integer? value) (pos? value))
      (refuse! :invalid-transaction-expectation
               (str field " must be a positive integer")
               {:field field :actual value})))
  expectation)

(defn- validate-spec!
  [spec]
  (when-not (map? spec)
    (refuse! :invalid-transaction-spec "Transaction spec must be a map"))
  (let [unknown (vec (sort (remove spec-keys (keys spec))))]
    (when (seq unknown)
      (refuse! :unknown-transaction-arguments
               (str "Unknown transaction arguments: "
                    (str/join ", " unknown))
               {:unknown unknown})))
  (when-not (and (vector? (:intents spec)) (seq (:intents spec)))
    (refuse! :invalid-intents "Spec :intents must be a non-empty vector"))
  {:intents (mapv validate-intent! (:intents spec) (range))
   :expect (validate-aggregate-expectation! (:expect spec))})

(defn- ordered-scoped-files
  [intents]
  (reduce (fn [result file]
            (if (some #{file} result) result (conj result file)))
          []
          (mapcat :files intents)))

(defn- matching-edits
  [source file {:keys [intent-index from to]}]
  (let [root (z/of-string source {:track-position? true})]
    (->> (zipper-locations root)
         (map-indexed vector)
         (keep (fn [[preorder candidate]]
                 (let [candidate-node (z/node candidate)]
                   (when (= (:fingerprint from)
                            (lossless-node-fingerprint candidate-node))
                     (let [{:keys [row end-row]} (meta candidate-node)
                           size (node-size candidate-node)]
                       {:intent-index intent-index
                        :file file
                        :address {:preorder preorder}
                        :end-preorder (+ preorder size -1)
                        :line row
                        :end-line end-row
                        :before (z/string candidate)
                        :after (:source to)})))))
         vec)))

(defn- compile-intent-edits
  [sources intent]
  (let [by-file (mapv (fn [file]
                        [file (matching-edits (get sources file) file intent)])
                      (:files intent))
        edits (vec (mapcat second by-file))
        actual-count (count edits)]
    (when-not (= (:expect-count intent) actual-count)
      (refuse! :expect-count-mismatch
               (str "Intent " (:intent-index intent) " expected "
                    (:expect-count intent) " matches, found " actual-count)
               {:intent-index (:intent-index intent)
                :expected-count (:expect-count intent)
                :actual-count actual-count
                :per-file-counts (into {} (map (fn [[file matches]]
                                                 [file (count matches)]))
                                       by-file)}))
    {:intent-index (:intent-index intent)
     :files (:files intent)
     :from (get-in intent [:from :source])
     :to (get-in intent [:to :source])
     :expected-count (:expect-count intent)
     :match-count actual-count
     :per-file-counts (into {} (map (fn [[file matches]]
                                      [file (count matches)]))
                            by-file)
     :edits edits}))

(defn- overlap?
  [left right]
  (<= (:address-preorder right) (:end-preorder left)))

(defn- assert-disjoint-edits!
  [file edits]
  (let [ordered (->> edits
                     (map #(assoc % :address-preorder
                                  (get-in % [:address :preorder])))
                     (sort-by :address-preorder)
                     vec)]
    (doseq [[left right] (partition 2 1 ordered)]
      (when (overlap? left right)
        (refuse! :overlapping-intents
                 (str "Intents overlap in " file)
                 {:file file
                  :intent-indexes [(:intent-index left)
                                   (:intent-index right)]
                  :edits [(dissoc left :address-preorder)
                          (dissoc right :address-preorder)]})))
    (mapv #(dissoc % :address-preorder) ordered)))

(defn- replacement-node
  [source]
  (:node (parse-one-form source ":to")))

(defn- replace-at-address
  [source {:keys [address before after]}]
  (let [root (z/of-string source {:track-position? true})
        target (nth (zipper-locations root) (:preorder address) nil)]
    (when-not target
      (refuse! :stale-path
               (str "Planned address no longer exists: " (:preorder address))))
    (when-not (= before (z/string target))
      (refuse! :stale-subform
               "Source at planned address does not match edit"))
    (-> target
        (z/replace (replacement-node after))
        z/root-string)))

(defn- apply-edits
  [source edits]
  (reduce replace-at-address
          source
          (sort-by #(get-in % [:address :preorder]) > edits)))

(defn- prefixed-lines
  [prefix source]
  (->> (str/split source #"\n" -1)
       (map #(str prefix %))
       (str/join "\n")))

(defn- edit-diff
  [{:keys [line before after intent-index]}]
  (str "@@ intent " intent-index " line " line " @@\n"
       (prefixed-lines "-" before) "\n"
       (prefixed-lines "+" after) "\n"))

(defn- file-diff
  [file edits]
  (str "--- a/" file "\n"
       "+++ b/" file "\n"
       (apply str (map edit-diff edits))))

(defn- compile-file
  [file source edits]
  (let [ordered-edits (assert-disjoint-edits! file edits)
        result-source (if (seq ordered-edits)
                        (apply-edits source ordered-edits)
                        source)]
    (validate-complete-source! file result-source :invalid-result-source)
    {:file file
     :match-count (count ordered-edits)
     :source-hash (structural-lens/source-hash source)
     :result-hash (structural-lens/source-hash result-source)
     :edits ordered-edits
     :diff (when (seq ordered-edits) (file-diff file ordered-edits))
     :result-source result-source}))

(defn- compile-transaction*
  [sources spec]
  (let [{:keys [intents expect]} (validate-spec! spec)
        files (ordered-scoped-files intents)
        _ (doseq [file files]
            (validate-complete-source! file (get sources file) :invalid-source))
        compiled-intents (mapv #(compile-intent-edits sources %) intents)
        edits-by-file (group-by :file (mapcat :edits compiled-intents))
        compiled-files (mapv #(compile-file % (get sources %) (get edits-by-file % []))
                             files)
        actual {:intent-count (count compiled-intents)
                :edit-count (reduce + (map :match-count compiled-intents))
                :changed-file-count (count (filter (comp pos? :match-count)
                                                   compiled-files))}]
    (when-not (= expect actual)
      (refuse! :transaction-expectation-mismatch
               "Compiled transaction does not match aggregate expectations"
               {:expected expect :actual actual}))
    {:ok true
     :operation :change
     :transaction-version transaction-version
     :intent-count (:intent-count actual)
     :match-count (:edit-count actual)
     :changed-file-count (:changed-file-count actual)
     :intents (mapv #(dissoc % :edits) compiled-intents)
     :files (mapv #(dissoc % :result-source :diff) compiled-files)
     :diff (apply str (keep :diff compiled-files))
     :future-sources (into {} (map (juxt :file :result-source) compiled-files))
     :validated {:whole-files-parsed true
                 :file-count (count compiled-files)}}))

(defn compile-transaction
  "Compile explicit exact structural intents against an in-memory file map.
   Returns a complete future state or one structured refusal. Performs no I/O."
  [sources spec]
  (try
    (when-not (map? sources)
      (refuse! :invalid-sources "Sources must be a map of file path to source"))
    (compile-transaction* sources spec)
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))

(defn- spec-files
  [spec]
  (->> (:intents spec)
       (mapcat :files)
       distinct
       vec))

(defn- read-sources
  [files]
  (reduce (fn [sources file]
            (try
              (assoc sources file (slurp file))
              (catch Exception e
                (refuse! :invalid-source
                         (str "Cannot read source " file ": " (.getMessage e))
                         {:file file}))))
          {}
          files))

(defn- public-plan
  [compiled]
  (dissoc compiled :future-sources))

(defn plan-change
  "Read the explicit files in :spec and compile one non-mutating transaction
   plan. The public result retains concrete edits and hashes but omits complete
   future-file source."
  [{:keys [spec] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    (let [sources (read-sources (spec-files spec))]
      (public-plan (compile-transaction sources spec)))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))
