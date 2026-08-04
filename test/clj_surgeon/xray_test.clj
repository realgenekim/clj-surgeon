(ns clj-surgeon.xray-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.edit-dsl :as dsl]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private source
  (str "(ns bench.xray)\n"
       "\n"
       "(def data\n"
       "  ^:private\n"
       "  {:xs [1 2]\n"
       "   ;; Keep the second vector distinct.\n"
       "   :ys [3 4]})\n"
       "\n"
       "(defn choose [x]\n"
       "  (case x\n"
       "    :a 1\n"
       "    :b 2))\n"))

(def ^:private vectors-query
  [[:form 'data] [:find '_] [:where {:tag :vector}]])

(defn- spec [query analyzer]
  (dsl/xray query analyzer))

(defn- one-spec [query analyzer]
  (dsl/xray-one query analyzer))

(deftest xray-builder-is-an-ordinary-threadable-terminal
  (let [program (-> (dsl/form 'data)
                    (dsl/match '_)
                    (dsl/where {:tag :vector})
                    (dsl/xray #(mapv count %)))]
    (is (= :xray (:kind program)))
    (is (= vectors-query (:query program)))
    (is (= [2 2] ((:analyzer program) [[1 2] [3 4]]))))
  (doseq [[label path analyzer error-type]
          [["path must be a vector" :not-a-path identity :invalid-xray-path]
           ["write terminal is invalid"
            [[:form 'data] [:replace :changed]] identity :invalid-xray-path]
           ["transform terminal is invalid"
            [[:form 'data] [:transform identity]] identity :invalid-xray-path]
           ["analyzer must be a function" [[:form 'data]] :not-a-function
            :invalid-xray-analyzer]]]
    (testing label
      (let [error (try
                    (dsl/xray path analyzer)
                    nil
                    (catch Exception exception (ex-data exception)))]
        (is (= error-type (:error-type error)))))))

(deftest xray-one-is-threadable-and-receives-one-value-directly
  (let [program (-> (dsl/form 'data)
                    (dsl/match :xs)
                    dsl/right
                    (dsl/xray-one #(reduce + %)))]
    (is (= :xray (:kind program)))
    (is (= :one (:cardinality program)))
    (is (= :selected-value (:input-shape program)))
    (is (= 3 ((:analyzer program) [1 2]))))
  (is (= [[:form 'load-starred-post :cljs]]
         (dsl/form 'load-starred-post :cljs))))

(deftest one-xray-algebra-covers-literal-stable-selection-and-count-refinement
  (let [literal (dsl/compile-xray "(-> (form 'data) (match :xs) right)")
        initializer (dsl/compile-xray "(-> (form 'data) initializer)")
        traversal (dsl/compile-xray
                    "(-> (form 'data) (analyze #(count (filter (fn [x] (and (map? x) (= true (:required x)))) (tree-seq coll? seq %)))))")
        computed (dsl/compile-xray
                   "(-> (form 'data) (match :xs) right (expect-count 1) (analyze (fn [[xs]] (count xs))))")
        aggregated (dsl/compile-xray
                     "(-> (form 'data) (match '_) (where {:tag :vector}) (analyze #(mapv count %)))")]
    (is (= :literal (:kind literal)))
    (is (= [[:form 'data] [:find :xs] :right] (:query literal)))
    (is (= [[:form 'data] :initializer] (:query initializer)))
    (is (= [1 1]
           (mapv #((:analyzer traversal) [%])
                 [{:args {:file {:required true}
                          :root {:required false}}}
                  {:args [{:required true}
                          {:required false}]}])))
    (is (= 1 (:expected-count computed)))
    (is (= :selected-values (:input-shape computed)))
    (is (= 2 ((:analyzer computed) [[1 2]])))
    (is (nil? (:cardinality aggregated)))
    (is (= [2 2] ((:analyzer aggregated) [[1 2] [3 4]])))
    (is (= :one (:cardinality (dsl/compute (dsl/form 'data) identity))))
    (is (nil? (:cardinality (dsl/aggregate (dsl/form 'data) identity))))
    (let [error (try
                  (dsl/inspect (dsl/form 'data) :maybe identity)
                  nil
                  (catch Exception exception (ex-data exception)))]
      (is (= :invalid-xray-cardinality (:error-type error)))
      (is (= [:one :all] (:allowed error))))))

(deftest sci-compiles-one-capability-limited-xray-program
  (let [expression (str "(-> (form 'data) (match '_) "
                        "(where {:tag :vector}) "
                        "(xray #(mapv (fn [xs] (reduce + xs)) %)))")
        compiled (dsl/compile-xray expression)]
    (is (= :xray (:kind compiled)))
    (is (= vectors-query (:query compiled)))
    (is (= [3 7] ((:analyzer compiled) [[1 2] [3 4]])))
    (is (= expression (:expression compiled)))
    (is (not-any? fn? (vals (dissoc compiled :analyzer)))))
  (doseq [[expression reason]
          [["" :expected-one-form]
           ["(form 'data) (form 'choose)" :expected-one-form]
           ["(-> (form 'data) (replace :changed))" :xray-terminal-required]
           ["(-> (form 'data) (replace :changed) (xray identity))"
            :invalid-xray-path]
           ["(-> (form 'data) (xray :callable-keyword))"
            :invalid-xray-analyzer]
           ["(spit \"/tmp/xray-must-not-write\" \"bad\")" :disallowed-symbol]]]
    (testing expression
      (let [error (try
                    (dsl/compile-xray expression)
                    nil
                    (catch Exception exception (ex-data exception)))]
        (is (= :invalid-xray-expression (:error-type error)))
        (is (= reason (:reason error)))
        (is (= expression (:expression error)))
        (is (some #{"(analyze path pure-function)"} (:allowed-forms error)))
        (is (str/includes? (:usage error) ":xray"))))))

(deftest sci-supports-idiomatic-pure-map-comprehension
  ;; Clean-context candidate-v9 agents first wrote these ordinary Clojure core
  ;; forms, then lost calls because the X-ray sandbox omitted for/key/val.
  (let [compiled
        (dsl/compile-xray
          "(-> (form 'data) (analyze (fn [[registry]] {:keys (vec (sort (map key registry))) :categories (vec (sort (map (comp :category val) registry))) :paired (vec (sort (for [[op spec] registry :when (contains? spec :pair)] op)))})))")
        registry (array-map :read {:category :read}
                            :plan {:category :write :pair :plan!}
                            :plan! {:category :write :pair :plan})]
    (is (= {:keys [:plan :plan! :read]
            :categories [:read :write :write]
            :paired [:plan :plan!]}
           ((:analyzer compiled) [registry])))))

(deftest literal-xray-returns-full-structural-evidence
  (let [expression "(-> (form 'data) (match :ys) right)"
        program (dsl/compile-xray expression)
        expected (lens/evaluate-query source (:query program))
        result (dsl/evaluate-xray source
                                  {:expression expression :xray program})]
    (is (= :xray (:operation result)))
    (is (= :literal (:mode result)))
    (is (= expression (:expression result)))
    (is (= (select-keys expected
                        [:query :trace :match-count :matches :source-hash])
           (select-keys result
                        [:query :trace :match-count :matches :source-hash])))
    (is (= "[3 4]" (get-in result [:matches 0 :source])))
    (is (nil? (:value result)))))

(deftest xray-evaluates-zero-one-and-many-selected-values
  (doseq [{:keys [label query analyzer expected-input expected-value]}
          [{:label "zero"
            :query [[:form 'data] [:find :missing]]
            :analyzer (fn [values] {:empty? (empty? values)})
            :expected-input []
            :expected-value {:empty? true}}
           {:label "one"
            :query [[:form 'data] [:find :xs] :right]
            :analyzer first
            :expected-input [[1 2]]
            :expected-value [1 2]}
           {:label "many"
            :query vectors-query
            :analyzer #(mapv (partial reduce +) %)
            :expected-input [[1 2] [3 4]]
            :expected-value [3 7]}]]
    (testing label
      (let [seen (atom nil)
            wrapped (fn [values]
                      (reset! seen values)
                      (analyzer values))
            result (dsl/evaluate-xray source
                                      {:file "bench/xray.clj"
                                       :expression "test expression"
                                       :xray (spec query wrapped)})]
        (is (= expected-input @seen))
        (is (= expected-value (:value result)))
        (is (= :xray (:operation result)))
        (is (= (count expected-input) (:match-count result)))
        (is (= {:input-shape :selected-values
                :input-count (count expected-input)
                :data-view :canonical-collections
                :cardinality :any
                :evidence :compact}
               (:xray result)))
        (is (= "test expression" (:expression result)))
        (is (= (lens/source-hash source) (:source-hash result)))))))

(deftest xray-one-refuses-zero-and-many-before-calling-the-analyzer
  (doseq [[label query expected-count]
          [["zero" [[:form 'missing]] 0]
           ["many" vectors-query 2]]]
    (testing label
      (let [calls (atom 0)
            result (dsl/evaluate-xray
                     source
                     {:expression label
                      :xray (one-spec query (fn [_] (swap! calls inc)))})]
        (is (= :xray-cardinality-mismatch (:error-type result)))
        (is (= 1 (:expected-match-count result)))
        (is (= expected-count (:actual-match-count result)))
        (is (zero? @calls))
        (is (nil? (:value result))))))
  (let [seen (atom nil)
        result (dsl/evaluate-xray
                 source
                 {:expression "one"
                  :xray (one-spec [[:form 'data] [:find :xs] :right]
                                  #(do (reset! seen %) (reduce + %)))})]
    (is (= [1 2] @seen))
    (is (= 3 (:value result)))
    (is (= :selected-value (get-in result [:xray :input-shape])))))

(deftest expect-count-refines-cardinality-without-changing-analyzer-input
  (let [seen (atom nil)
        expression (str "(-> (form 'data) (match :xs) right "
                        "(expect-count 1) (analyze identity))")
        program (dsl/compile-xray expression)
        result (dsl/evaluate-xray
                 source
                 {:expression expression
                  :xray (assoc program :analyzer #(do (reset! seen %) %))})]
    (is (= [[1 2]] @seen))
    (is (= [[1 2]] (:value result)))
    (is (= [:exactly 1] (get-in result [:xray :cardinality])))
    (is (= :selected-values (get-in result [:xray :input-shape]))))
  (let [calls (atom 0)
        program (-> (dsl/form 'data)
                    (dsl/match '_)
                    (dsl/where {:tag :vector})
                    (dsl/expect-count 1)
                    (dsl/analyze #(swap! calls inc)))
        result (dsl/evaluate-xray source {:expression "refusal"
                                          :xray program})]
    (is (= :xray-cardinality-mismatch (:error-type result)))
    (is (= 1 (:expected-match-count result)))
    (is (= 2 (:actual-match-count result)))
    (is (zero? @calls)))
  (let [error (try
                (dsl/expect-count (dsl/form 'data) -1)
                nil
                (catch Exception exception (ex-data exception)))]
    (is (= :invalid-xray-cardinality (:error-type error)))))

(deftest computed-xray-canonicalizes-map-shaped-syntax-without-evaluation
  (let [map-source (str "(ns bench.maps)\n"
                        "(def literal {:a 1 :b 2})\n"
                        "(def hashed (hash-map :a 1 :b 2))\n"
                        "(def arrayed (array-map :a 1 :b 2))\n"
                        "(def unsupported (merge {:a 1} {:b 2}))\n"
                        "(def malformed (hash-map :a))\n")
        evaluate (fn [name]
                   (let [expression (str "(-> (form '" name ") initializer "
                                         "(expect-count 1) (analyze identity))")
                         program (dsl/compile-xray expression)]
                     (dsl/evaluate-xray map-source
                                        {:expression expression
                                         :xray program})))]
    (doseq [name ['literal 'hashed 'arrayed]]
      (testing (str name)
        (let [result (evaluate name)]
          (is (= [{:a 1 :b 2}] (:value result)))
          (is (= :canonical-collections
                 (get-in result [:xray :data-view]))))))
    (testing "unsupported calls remain syntax"
      (is (= ['(merge {:a 1} {:b 2})]
             (:value (evaluate 'unsupported)))))
    (testing "literal read remains exact source"
      (let [expression "(-> (form 'hashed) initializer)"
            program (dsl/compile-xray expression)
            result (dsl/evaluate-xray map-source
                                      {:expression expression
                                       :xray program})]
        (is (= "(hash-map :a 1 :b 2)"
               (get-in result [:matches 0 :source])))
        (is (nil? (:value result)))))
    (testing "odd constructor arguments refuse before analysis"
      (let [result (evaluate 'malformed)]
        (is (= :xray-input-invalid (:error-type result)))
        (is (str/includes? (:error result) "key/value pairs"))
        (is (nil? (:value result)))))))

(deftest compact-evidence-preserves-provenance-without-repeating-source
  (let [query [[:form 'data] [:find :ys] :right]
        compact (dsl/evaluate-xray
                  source
                  {:expression "compact"
                   :xray (one-spec query identity)})
        full (dsl/evaluate-xray
               source
               {:expression "full"
                :evidence :full
                :xray (one-spec query identity)})
        compact-match (get-in compact [:matches 0])]
    (is (= [3 4] (:value compact) (:value full)))
    (is (= :compact (get-in compact [:xray :evidence])))
    (is (= :full (get-in full [:xray :evidence])))
    (is (nil? (:source compact-match)))
    (is (= "[3 4]" (get-in full [:matches 0 :source])))
    (is (re-matches #"[0-9a-f]{64}" (:source-hash compact-match)))
    (is (re-matches #"[0-9a-f]{64}" (:selection-hash compact)))
    (is (= (:address compact-match) (get-in full [:matches 0 :address])))
    (is (= (:trace compact) (:trace full))))
  (let [large-source (str "(ns large.evidence)\n(def data "
                          (pr-str (vec (range 1000))) ")\n")
        query [[:form 'data]]
        compact (dsl/evaluate-xray
                  large-source
                  {:expression "compact"
                   :xray (one-spec query count)})
        full (dsl/evaluate-xray
               large-source
               {:expression "full"
                :evidence :full
                :xray (one-spec query count)})]
    (is (< (count (pr-str compact))
           (/ (count (pr-str full)) 2))))
  (let [first-result (dsl/evaluate-xray
                       source
                       {:expression "stable"
                        :xray (one-spec [[:form 'data]] identity)})
        changed-result (dsl/evaluate-xray
                         (str/replace source "[3 4]" "[3 5]")
                         {:expression "stable"
                          :xray (one-spec [[:form 'data]] identity)})]
    (is (not= (:selection-hash first-result)
              (:selection-hash changed-result)))
    (is (not= (get-in first-result [:matches 0 :source-hash])
              (get-in changed-result [:matches 0 :source-hash])))))

(deftest spans-and-partitions-arrive-as-vectors-of-clojure-values
  (let [span-result
        (dsl/evaluate-xray
          source
          {:file "bench/xray.clj"
           :expression "span"
           :evidence :full
           :xray (spec [[:form 'choose] [:find :a] [:span 2]] first)})
        partition-result
        (dsl/evaluate-xray
          source
          {:file "bench/xray.clj"
           :expression "partitions"
           :evidence :full
           :xray (spec [[:form 'choose] [:find 'case] :up :down :right :right
                        [:partition-all 2]]
                       #(mapv vec %))})]
    (is (= [:a 1] (:value span-result)))
    (is (= [[:a 1] [:b 2]] (:value partition-result)))
    (is (= [":a" "1"] (get-in span-result [:matches 0 :forms])))
    (is (= [":b" "2"] (get-in partition-result [:matches 1 :forms])))))

(deftest xray-preserves-exact-query-evidence-beside-the-computed-value
  (let [query [[:form 'data] [:find :ys] :right]
        expected (lens/evaluate-query source query)
        actual (dsl/evaluate-xray
                 source
                 {:file "bench/xray.clj"
                  :expression "evidence"
                  :evidence :full
                  :xray (spec query first)})]
    (is (= (select-keys expected
                        [:query :trace :match-count :matches :source-hash])
           (select-keys actual
                        [:query :trace :match-count :matches :source-hash])))
    (is (= [3 4] (:value actual)))
    (is (not-any? fn? (tree-seq coll? seq actual)))))

(deftest xray-accepts-only-concrete-bounded-edn-results
  (doseq [[label value]
          [["nil" nil]
           ["boolean" true]
           ["character" \x]
           ["string" "x"]
           ["symbol" 'x]
           ["keyword" :x]
           ["integer" 1]
           ["ratio" 1/2]
           ["list" '(1 2)]
           ["vector" [1 2]]
           ["map" {:x [1]}]
           ["set" #{1 2}]]]
    (testing label
      (let [result (dsl/evaluate-xray
                     source
                     {:file "bench/xray.clj"
                      :expression label
                      :xray (spec [[:form 'data] [:find :missing]]
                                  (constantly value))})]
        (is (= value (:value result)))
        (is (nil? (:error result))))))
  (doseq [[label analyzer expected]
          [["lazy sequence" #(map identity %) :invalid-xray-result]
           ["function" (constantly identity) :invalid-xray-result]
           ["host object" (constantly (Object.)) :invalid-xray-result]
           ["oversized" (constantly (apply str (repeat 65537 "x")))
            :xray-result-too-large]
           ["throws" (fn [_] (throw (ex-info "boom" {})))
            :xray-analysis-failed]]]
    (testing label
      (let [result (dsl/evaluate-xray
                     source
                     {:file "bench/xray.clj"
                      :expression label
                      :xray (spec [[:form 'data] [:find :missing]] analyzer)})]
        (is (= expected (:error-type result)))
        (is (nil? (:value result)))
        (is (not (str/includes? (str result) "#object")))))))

(deftest xray-analysis-refusal-stays-compact-and-actionable
  (let [shaped-source (str "(ns bench.shape)\n"
                           "(def registry\n"
                           "  \"Operation registry.\"\n"
                           "  (hash-map :read {:category :read}\n"
                           "            :write {:category :write}))\n")
        result (dsl/evaluate-xray
                 shaped-source
                 {:expression "shape repair"
                  :xray (one-spec [[:form 'registry]]
                                  #(throw (ex-info "not a map" {})))})
        serialized (pr-str result)]
    (is (= :xray-analysis-failed (:error-type result)))
    (is (str/includes? (:remedy result) "parsed syntax"))
    (is (str/includes? (:remedy result) "without its terminal"))
    (is (nil? (:input-summary result)))
    (is (not (str/includes? serialized "Operation registry.")))
    (is (not (str/includes? serialized "hash-map :read")))
    (is (< (count serialized) 1500))))

(deftest xray-refuses-to-compute-from-truncated-evidence
  (let [forms (str/join " " (map #(keyword (str "k" %)) (range 101)))
        many-source (str "(ns bench.many)\n(def many [" forms "])\n")
        calls (atom 0)
        result (dsl/evaluate-xray
                 many-source
                 {:file "bench/many.clj"
                  :expression "bounded"
                  :xray (spec [[:form 'many] [:find '_]
                               [:where {:tag :token :parent-tag :vector}]]
                              (fn [values]
                                (swap! calls inc)
                                values))})]
    (is (= :xray-input-truncated (:error-type result)))
    (is (= 101 (:match-count result)))
    (is (= 100 (count (:matches result))))
    (is (zero? @calls))
    (is (nil? (:value result)))))

(deftest real-nested-cond-xray-composes-with-outermost-and-partitions
  (let [real-source (slurp "bench/fixtures/bench/pair_view.clj")
        expression (str "(-> (form 'classify-request) (match 'cond) up "
                        "outermost down right (partition-all 2) "
                        "(xray #(mapv first %)))")
        compiled (dsl/compile-xray expression)
        result (dsl/evaluate-xray
                 real-source
                 {:file "bench/fixtures/bench/pair_view.clj"
                  :expression expression
                  :evidence :full
                  :xray compiled})]
    (is (= [[:form 'classify-request] [:find 'cond] :up :outermost
            :down :right [:partition-all 2]]
           (:query compiled)))
    (is (= 7 (:match-count result)))
    (is (= '(nil? actor) (first (:value result))))
    (is (= :else (last (:value result))))
    (is (string? (get-in result [:matches 5 :forms 1])))
    (is (str/starts-with? (get-in result [:matches 5 :source])
                          "(seq (:delegations actor))"))))

(deftest prepare-xray-validates-before-source-io
  (let [base {:op :xray :file "/missing/source.clj"}
        valid "(-> (form 'data) (xray count))"]
    (is (= :xray
           (:operation (dsl/prepare-xray-options (assoc base :expr valid)))))
    (is (= :missing-xray-input
           (:error-type (dsl/prepare-xray-options base))))
    (is (= :unsupported-arguments
           (:error-type (dsl/prepare-xray-options
                          (assoc base :expr valid :query [])))))
    (is (= :invalid-xray-expression
           (:error-type (dsl/prepare-xray-options
                          (assoc base :expr "(slurp \"secret\")")))))
    (is (= :invalid-evidence-mode
           (:error-type (dsl/prepare-xray-options
                          (assoc base :expr valid :evidence :brief)))))
    (is (= :compact
           (:evidence (dsl/prepare-xray-options (assoc base :expr valid)))))
    (is (= :full
           (:evidence (dsl/prepare-xray-options
                        (assoc base :expr valid :evidence :full)))))))

(def ^:private project-root
  (str (fs/normalize (fs/path (System/getProperty "user.dir")))))

(defn- run-cli [& args]
  @(proc/process
     (into ["bb" "-m" "clj-surgeon.core"] args)
     {:dir project-root :err :string :out :string}))

(deftest cli-xray-computes-one-read-only-edn-result
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon xray "})
        source-file (fs/path tmp-dir "source.clj")
        original "(ns bench.retry)\n(def retry-policy {:delays [100 250 500]})\n"
        expression (str "(-> (form 'retry-policy) (match :delays) right "
                        "(xray-one #(apply max %)))")]
    (try
      (spit (str source-file) original)
      (let [run (run-cli ":op" ":xray"
                         ":file" (str source-file)
                         ":expr" expression)
            result (edn/read-string (:out run))]
        (is (zero? (:exit run)) (:err run))
        (is (= :xray (:operation result)))
        (is (= 500 (:value result)))
        (is (= 1 (:match-count result)))
        (is (nil? (get-in result [:matches 0 :source])))
        (is (re-matches #"[0-9a-f]{64}"
                        (get-in result [:matches 0 :source-hash])))
        (is (re-matches #"[0-9a-f]{64}" (:selection-hash result)))
        (is (= original (slurp (str source-file))))
        (is (not (str/includes? (:out run) "#object"))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-xray-refuses-unsafe-input-before-file-io
  (doseq [[label args expected]
          [["missing expression" [] :missing-arguments]
           ["unsafe expression" [":expr" "(slurp \"secret\")"]
            :invalid-xray-expression]
           ["unknown argument"
            [":expr" "(-> (form 'data) (xray count))" ":query" "[]"]
            :unsupported-arguments]]]
    (testing label
      (let [run (apply run-cli ":op" ":xray"
                       ":file" "/definitely/missing/source.clj"
                       args)
            result (edn/read-string (:out run))]
        (is (pos? (:exit run)) (:out run))
        (is (= expected (:error-type result)))
        (is (str/includes? (:usage result) "clj-surgeon :op :xray"))
        (is (< (count (:out run)) 1024))
        (is (not (str/includes? (:err run) "Exception")))))))

(deftest xray-help-and-agent-surfaces-teach-the-computed-read-boundary
  (let [global (core/format-global-help core/ops-registry)
        help (core/format-op-help :xray (get core/ops-registry :xray))
        surfaces {"README" (slurp "README.md")
                  "canonical skill" (slurp "skills/clj-surgeon/SKILL.md")
                  "legacy skill" (slurp "skill.md")
                  "repository instructions" (slurp "CLAUDE.md")
                  "vision" (slurp "docs/vision.md")
                  "changelog" (slurp "CHANGELOG.md")
                  "help" help}]
    (is (contains? core/ops-registry :xray))
    (is (str/includes? global "clj-surgeon :op :xray"))
    (is (= #{:file :expr}
           (set (keys (get-in core/ops-registry [:xray :args])))))
    (is (every? :required
                (map #(get-in core/ops-registry [:xray :args %])
                     [:file :expr])))
    (doseq [[surface text] surfaces]
      (testing surface
        (is (str/includes? text ":xray"))
        (is (str/includes? text "(form"))
        (is (str/includes? (str/lower-case text) "pure clojure"))
        (is (str/includes? text ":value"))
        (is (str/includes? (str/lower-case text) "never write"))))
    (doseq [surface ["README" "canonical skill" "legacy skill" "help"]]
      (testing (str surface " teaches computed aggregation and exact-one input")
        (let [text (get surfaces surface)]
          (is (str/includes? text "frequencies"))
          (is (str/includes? text "analyze") surface)
          (is (str/includes? text "expect-count") surface)
          (is (str/includes? text "initializer") surface)
          (is (str/includes? text "tree-seq") surface)
          (is (str/includes? text "(for") surface))))
    (is (<= (count (str/split-lines
                     (get surfaces "canonical skill")))
            240))))

(deftest parse-args-preserves-xray-expression-verbatim
  (let [expression "(-> (form 'data) (xray #(mapv count %)))"]
    (is (= expression
           (:expr (core/parse-args [":op" ":xray"
                                    ":file" "src/data.clj"
                                    ":expr" expression]))))))
