#!/usr/bin/env bb

(ns score-format-extraction
  (:require
   [clj-surgeon.outline :as outline]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests testing]]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def score-schema
  "clj-surgeon.format-extraction-score/v1")

(def presentation-tags
  #{:comma :newline :whitespace})

(def sessionize-contract
  {:source-file "src/cfp_scheduler_killer/views.clj"
   :source-ns 'cfp-scheduler-killer.views
   :destination-file "src/cfp_scheduler_killer/views/format.clj"
   :destination-ns 'cfp-scheduler-killer.views.format
   :moved-forms
   '[date-fmt datetime-fmt ->local-date fmt-date fmt-date-range fmt-instant
     ->instant when-fmt relative-when fmt-when fmt-cfp-window iso-date-fmt
     fmt-close-date cfp-public-url not-blank]
   :publicized-forms '#{not-blank}
   :destination-libs
   '#{cfp-scheduler-killer.events clojure.string}
   :destination-aliases
   '{events cfp-scheduler-killer.events
     str clojure.string}
   :destination-imports
   '#{java.time.LocalDate java.time.ZoneId
      java.time.format.DateTimeFormatter}})

(defn parse-records
  [file source]
  (try
    {:ok true
     :records (outline/top-level-form-records file source)}
    (catch Exception exception
      {:ok false :message (ex-message exception)})))

(defn attached-source
  [source {:keys [line end-line comment-start] :as record}]
  (if (and line end-line comment-start (< comment-start line))
    (let [lines (vec (str/split-lines source))]
      (str/join "\n" (subvec lines (dec comment-start) end-line)))
    (:source record)))

(defn meaning-shape
  [form-node]
  (when-not (presentation-tags (node/tag form-node))
    [(node/tag form-node)
     (if (node/inner? form-node)
       (->> (node/children form-node)
            (keep meaning-shape)
            vec)
       (node/string form-node))]))

(defn source-shape
  [source]
  (meaning-shape (parser/parse-string-all source)))

(defn record-view
  [source record]
  {:type (:type record)
   :name (:name record)
   :shape (source-shape (attached-source source record))})

(defn owner-index
  [records]
  (group-by (comp str :name) (filter :name records)))

(defn comment-frequencies
  [source]
  (->> (tree-seq node/inner?
                 node/children
                 (parser/parse-string-all source))
       (filter #(= :comment (node/tag %)))
       (map node/string)
       frequencies))

(defn form-symbols
  [source]
  (->> (tree-seq node/inner?
                 node/children
                 (parser/parse-string source))
       (filter #(= :token (node/tag %)))
       (keep (fn [token]
               (try
                 (let [value (node/sexpr token)]
                   (when (symbol? value) value))
                 (catch Exception _ nil))))))

(defn ns-form
  [records]
  (some #(when (= 'ns (:type %)) %) records))

(defn- option-map
  [values]
  (loop [remaining values result {}]
    (if (empty? remaining)
      result
      (let [[key value & more] remaining]
        (recur more (assoc result key value))))))

(defn- libspec-contract
  [spec]
  (let [values (if (vector? spec) spec [spec])
        lib (first values)
        options (option-map (rest values))]
    {:lib lib
     :alias (:as options)
     :refer (let [referred (:refer options)]
              (cond
                (= :all referred) :all
                (sequential? referred) (set referred)
                :else #{}))}))

(defn ns-contract
  [records]
  (let [form (ns-form records)
        sexpr (some-> form :source parser/parse-string node/sexpr)
        clauses (filter seq? (drop 2 sexpr))
        require-clause (some #(when (= :require (first %)) %) clauses)
        import-clause (some #(when (= :import (first %)) %) clauses)
        libspecs (map libspec-contract (rest require-clause))
        aliases (into {} (keep (fn [{:keys [alias lib]}]
                                 (when alias [alias lib]))) libspecs)
        refers (into {} (map (juxt :lib :refer)) libspecs)
        imports
        (->> (rest import-clause)
             (mapcat
              (fn [spec]
                (if (seq? spec)
                  (let [package (first spec)]
                    (map #(symbol (str package "." %)) (rest spec)))
                  [spec])))
             set)]
    {:name (second sexpr)
     :libs (set (map :lib libspecs))
     :aliases aliases
     :refers refers
     :imports imports}))

(defn publicize-source
  [source]
  (str/replace-first source #"\(defn-\s+" "(defn "))

(defn remaining-record-views
  [source records moved]
  (->> records
       (remove #(or (= 'ns (:type %))
                    (contains? moved (:name %))))
       (mapv #(record-view source %))))

(defn moved-form-errors
  [contract before-source before-records destination-source destination-records]
  (let [before-by-owner (owner-index before-records)
        destination-by-owner (owner-index destination-records)
        publicized (:publicized-forms contract)]
    (->> (:moved-forms contract)
         (keep
          (fn [owner]
            (let [before-matches (get before-by-owner (str owner))
                  destination-matches (get destination-by-owner (str owner))
                  expected (some->> (first before-matches)
                                    (attached-source before-source)
                                    (#(if (contains? publicized owner)
                                        (publicize-source %)
                                        %))
                                    source-shape)
                  actual (some->> (first destination-matches)
                                  (attached-source destination-source)
                                  source-shape)]
              (cond
                (not= 1 (count before-matches))
                {:error-type :invalid-before-owner-cardinality
                 :owner owner :count (count before-matches)}

                (not= 1 (count destination-matches))
                {:error-type :destination-owner-cardinality
                 :owner owner :count (count destination-matches)}

                (not= expected actual)
                {:error-type :moved-owner-source-mismatch :owner owner}))))
         vec)))

(defn moved-symbol-occurrences
  [records moved]
  (->> records
       (remove #(= 'ns (:type %)))
       (mapcat (comp form-symbols :source))
       (filter #(contains? moved (symbol (name %))))
       vec))

(defn resolved-from-destination?
  [destination-ns {:keys [aliases refers]} occurrence]
  (let [owner (symbol (name occurrence))
        qualifier (some-> occurrence namespace symbol)]
    (if qualifier
      (or (= qualifier destination-ns)
          (= destination-ns (get aliases qualifier)))
      (let [referred (get refers destination-ns #{})]
        (or (= :all referred) (contains? referred owner))))))

(defn score-extraction
  [contract before-source candidate-source candidate-destination]
  (let [before-parse (parse-records (:source-file contract) before-source)
        source-parse (parse-records (:source-file contract) candidate-source)
        destination-parse
        (parse-records (:destination-file contract) candidate-destination)
        parseable (every? :ok [before-parse source-parse destination-parse])]
    (if-not parseable
      {:schema score-schema
       :parseable false
       :meaning-preserved false
       :correct false
       :errors
       (vec
        (keep-indexed
         (fn [index result]
           (when-not (:ok result)
             {:error-type :parse-failed
              :input (nth [:before :source :destination] index)
              :message (:message result)}))
         [before-parse source-parse destination-parse]))}
      (let [before-records (:records before-parse)
            source-records (:records source-parse)
            destination-records (:records destination-parse)
            moved (set (:moved-forms contract))
            source-owners (owner-index source-records)
            destination-owner-names
            (set (map :name (filter :name destination-records)))
            expected-source-views
            (remaining-record-views before-source before-records moved)
            candidate-source-views
            (remaining-record-views candidate-source source-records moved)
            before-ns (ns-contract before-records)
            source-ns (ns-contract source-records)
            destination-ns (ns-contract destination-records)
            allowed-source-libs
            (conj (:libs before-ns) (:destination-ns contract))
            required-source-libs
            (set/difference (:libs before-ns) (:destination-libs contract))
            required-source-imports
            (set/difference (:imports before-ns)
                            (:destination-imports contract))
            required-source-aliases
            (into {}
                  (filter (fn [[_ lib]]
                            (contains? required-source-libs lib)))
                  (:aliases before-ns))
            caller-symbols
            (moved-symbol-occurrences source-records moved)
            unresolved-callers
            (->> caller-symbols
                 (remove #(resolved-from-destination?
                           (:destination-ns contract) source-ns %))
                 (map str)
                 distinct
                 sort
                 vec)
            before-comments (comment-frequencies before-source)
            candidate-comments
            (merge-with +
                        (comment-frequencies candidate-source)
                        (comment-frequencies candidate-destination))
            errors
            (cond-> []
              (not= (:source-ns contract) (:name source-ns))
              (conj {:error-type :wrong-source-namespace
                     :expected (:source-ns contract)
                     :actual (:name source-ns)})

              (not= (:destination-ns contract) (:name destination-ns))
              (conj {:error-type :wrong-destination-namespace
                     :expected (:destination-ns contract)
                     :actual (:name destination-ns)})

              (some #(seq (get source-owners (str %))) moved)
              (conj {:error-type :moved-owner-remains-in-source
                     :owners (vec (sort (filter #(seq (get source-owners (str %)))
                                                moved)))})

              (not= moved destination-owner-names)
              (conj {:error-type :destination-owner-set-mismatch
                     :expected moved
                     :actual destination-owner-names})

              (seq (moved-form-errors contract before-source before-records
                                      candidate-destination destination-records))
              (into (moved-form-errors contract before-source before-records
                                       candidate-destination destination-records))

              (not= expected-source-views candidate-source-views)
              (conj {:error-type :unrelated-source-form-changed})

              (not= before-comments candidate-comments)
              (conj {:error-type :comment-multiset-changed
                     :expected before-comments
                     :actual candidate-comments})

              (not= (:destination-libs contract) (:libs destination-ns))
              (conj {:error-type :destination-libraries-not-minimal
                     :expected (:destination-libs contract)
                     :actual (:libs destination-ns)})

              (not= (:destination-aliases contract)
                    (:aliases destination-ns))
              (conj {:error-type :destination-aliases-mismatch
                     :expected (:destination-aliases contract)
                     :actual (:aliases destination-ns)})

              (not= (:destination-imports contract)
                    (:imports destination-ns))
              (conj {:error-type :destination-imports-mismatch
                     :expected (:destination-imports contract)
                     :actual (:imports destination-ns)})

              (not (set/subset? required-source-libs (:libs source-ns)))
              (conj {:error-type :required-source-library-removed
                     :missing (set/difference required-source-libs
                                              (:libs source-ns))})

              (not (set/subset? (:libs source-ns) allowed-source-libs))
              (conj {:error-type :unexpected-source-library
                     :unexpected (set/difference (:libs source-ns)
                                                 allowed-source-libs)})

              (not (contains? (:libs source-ns)
                              (:destination-ns contract)))
              (conj {:error-type :destination-not-required-by-source})

              (not (set/subset? required-source-imports
                                (:imports source-ns)))
              (conj {:error-type :required-source-import-removed
                     :missing (set/difference required-source-imports
                                              (:imports source-ns))})

              (not (set/subset? (:imports source-ns) (:imports before-ns)))
              (conj {:error-type :unexpected-source-import
                     :unexpected (set/difference (:imports source-ns)
                                                 (:imports before-ns))})

              (not= required-source-aliases
                    (select-keys (:aliases source-ns)
                                 (keys required-source-aliases)))
              (conj {:error-type :required-source-alias-changed})

              (seq unresolved-callers)
              (conj {:error-type :unresolved-moved-callers
                     :symbols unresolved-callers}))]
        {:schema score-schema
         :parseable true
         :meaning-preserved (empty? errors)
         :correct (empty? errors)
         :moved-owner-count (count moved)
         :remaining-caller-occurrence-count (count caller-symbols)
         :errors (vec errors)}))))

(def synthetic-contract
  {:source-file "src/sample/core.clj"
   :source-ns 'sample.core
   :destination-file "src/sample/format.clj"
   :destination-ns 'sample.format
   :moved-forms '[fmt not-blank]
   :publicized-forms '#{not-blank}
   :destination-libs '#{sample.events clojure.string}
   :destination-aliases '{events sample.events str clojure.string}
   :destination-imports '#{java.time.LocalDate}})

(def synthetic-before
  "(ns sample.core\n  (:require [sample.events :as events]\n            [clojure.string :as str]\n            [sample.keep :as keep])\n  (:import (java.time LocalDate)))\n\n(defn keep-owner [] (keep/x))\n\n;; Formatting helpers\n\n(defn- not-blank [s] (when-not (str/blank? s) s))\n\n;; Keep fmt attached.\n(defn fmt [x] (events/name (not-blank x) (LocalDate/now)))\n\n(defn caller [x] (fmt x))\n")

(def synthetic-source
  "(ns sample.core\n  (:require [sample.format :refer [fmt]]\n            [sample.keep :as keep]))\n\n(defn keep-owner [] (keep/x))\n\n;; Formatting helpers\n\n(defn caller [x] (fmt x))\n")

(def synthetic-destination
  "(ns sample.format\n  (:require [clojure.string :as str]\n            [sample.events :as events])\n  (:import (java.time LocalDate)))\n\n(defn-ignored-placeholder [])\n")

(defn synthetic-valid-destination
  []
  (str/replace
   synthetic-destination
   "(defn-ignored-placeholder [])"
   (str ";; Keep fmt attached.\n"
        "(defn fmt [x] (events/name (not-blank x) (LocalDate/now)))\n\n"
        "(defn not-blank [s] (when-not (str/blank? s) s))")))

(deftest extraction-invariant-contract
  (let [valid-destination (synthetic-valid-destination)
        valid (score-extraction synthetic-contract synthetic-before
                                synthetic-source valid-destination)]
    (is (:correct valid) valid)
    (doseq [[label source destination expected-error]
            [[:missing-owner synthetic-source
              (str/replace valid-destination
                           "(defn fmt [x] (events/name (not-blank x) (LocalDate/now)))\n\n"
                           "")
              :destination-owner-set-mismatch]
             [:private-not-blank synthetic-source
              (str/replace valid-destination "(defn not-blank" "(defn- not-blank")
              :moved-owner-source-mismatch]
             [:unbound-caller
              (str/replace synthetic-source ":refer [fmt]" ":as format")
              valid-destination :unresolved-moved-callers]
             [:unrelated-form-change
              (str/replace synthetic-source "(keep/x)" "(keep/y)")
              valid-destination :unrelated-source-form-changed]
             [:comment-loss
              (str/replace synthetic-source ";; Formatting helpers\n\n" "")
              valid-destination :comment-multiset-changed]
             [:extra-destination-dependency synthetic-source
              (str/replace valid-destination
                           "[sample.events :as events]"
                           "[sample.events :as events]\n            [sample.keep :as keep]")
              :destination-libraries-not-minimal]]]
      (testing label
        (let [score (score-extraction synthetic-contract synthetic-before
                                      source destination)]
          (is (false? (:correct score)))
          (is (some #(= expected-error (:error-type %)) (:errors score))
              score))))))

(defn -main
  [& args]
  (cond
    (= ["--self-test"] args)
    (let [{:keys [fail error]} (run-tests 'score-format-extraction)]
      (when (pos? (+ fail error))
        (System/exit 1)))

    (= 3 (count args))
    (let [[before-path candidate-source-path candidate-destination-path] args
          score (score-extraction sessionize-contract
                                  (slurp before-path)
                                  (slurp candidate-source-path)
                                  (slurp candidate-destination-path))]
      (prn score)
      (when-not (:correct score)
        (System/exit 1)))

    :else
    (throw
     (ex-info
      (str "Usage: score_format_extraction.clj --self-test | "
           "BEFORE-SOURCE CANDIDATE-SOURCE CANDIDATE-DESTINATION")
      {}))))

(when (some? *command-line-args*)
  (apply -main *command-line-args*))
