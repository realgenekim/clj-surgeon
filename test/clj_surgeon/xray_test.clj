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
           ["(form 'data)" :xray-terminal-required]
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
        (is (some #{"(xray path pure-function)"} (:allowed-forms error)))
        (is (str/includes? (:remedy error) ":xray"))))))

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
                :input-count (count expected-input)}
               (:xray result)))
        (is (= "test expression" (:expression result)))
        (is (= (lens/source-hash source) (:source-hash result)))))))

(deftest spans-and-partitions-arrive-as-vectors-of-clojure-values
  (let [span-result
        (dsl/evaluate-xray
         source
         {:file "bench/xray.clj"
          :expression "span"
          :xray (spec [[:form 'choose] [:find :a] [:span 2]] first)})
        partition-result
        (dsl/evaluate-xray
         source
         {:file "bench/xray.clj"
          :expression "partitions"
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
                         (assoc base :expr "(slurp \"secret\")")))))))

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
                        "(xray #(apply max (first %))))")]
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
        (is (= "[100 250 500]" (get-in result [:matches 0 :source])))
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
    (is (= #{:file :expr} (set (keys (get-in core/ops-registry [:xray :args])))))
    (is (every? :required (vals (get-in core/ops-registry [:xray :args]))))
    (doseq [[surface text] surfaces]
      (testing surface
        (is (str/includes? text ":xray"))
        (is (str/includes? text "(xray"))
        (is (str/includes? (str/lower-case text) "pure clojure"))
        (is (str/includes? text ":value"))
        (is (str/includes? (str/lower-case text) "never write"))))
    (is (<= (count (str/split-lines
                    (get surfaces "canonical skill")))
            240))))

(deftest parse-args-preserves-xray-expression-verbatim
  (let [expression "(-> (form 'data) (xray #(mapv count %)))"]
    (is (= expression
           (:expr (core/parse-args [":op" ":xray"
                                    ":file" "src/data.clj"
                                    ":expr" expression]))))))
