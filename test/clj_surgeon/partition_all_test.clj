(ns clj-surgeon.partition-all-test
  (:require
   [babashka.process :as proc]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private shape-source
  (str "(ns partition.shapes)\n"
       "\n"
       "(defn route [state event]\n"
       "  (case event\n"
       "    :start  (assoc state :status :running)\n"
       "    :finish\n"
       "    ;; This comment is inside the second pair.\n"
       "    (assoc state :status :done)\n"
       "    ;; This comment is between the second pair and the tail.\n"
       "    state))\n"
       "\n"
       "(defn bindings [x]\n"
       "  (let [a (inc x)\n"
       "        b (* a 2)]\n"
       "    {:a a :b b}))\n"
       "\n"
       "(def settings {:timeout 1000 :mode :safe})\n"
       "(def normalize #(select-keys % [:type :line :end-line]))\n"
       "(def platform #?(:clj [:a 1 :b 2] :cljs [:a 3]))\n"
       "(def discarded [:a #_obsolete 1 :b 2])\n"
       "(def ^:private annotated [:a 1 :b 2])\n"))

(defn- partitions [source query]
  (:matches (lens/evaluate-query source query)))

(def ^:private project-root
  (.getCanonicalPath (io/file ".")))

(defn- run-cli [& args]
  @(proc/process
     (into ["bb" "-m" "clj-surgeon.core"] args)
     {:dir project-root :err :string :out :string}))

(deftest partition-all-groups-a-semantic-sibling-suffix-with-an-explicit-tail
  (let [query [[:form 'route] [:find 'case] :up :down :right :right
               [:partition-all 2]]
        result (lens/evaluate-query shape-source query)
        [start finish tail] (:matches result)]
    (is (nil? (:error result)))
    (is (= 3 (:match-count result)))
    (is (= [2 2 1] (mapv :count (:matches result))))
    (is (= [[":start" "(assoc state :status :running)"]
            [":finish" "(assoc state :status :done)"]
            ["state"]]
           (mapv :forms (:matches result))))
    (is (= [{:size 2 :index 0 :complete? true}
            {:size 2 :index 1 :complete? true}
            {:size 2 :index 2 :complete? false}]
           (mapv :partition (:matches result))))
    (is (every? #(= :span (:tag %)) (:matches result)))
    (is (= 2 (count (get-in start [:address :preorders]))))
    (is (= 1 (count (:gaps finish))))
    (is (str/includes? (:source finish)
                       ";; This comment is inside the second pair."))
    (is (not (str/includes? (:source finish)
                            ";; This comment is between the second pair")))
    (is (= "state" (:source tail)))
    (is (= {:partition-all {:size 2 :index 2 :count 1}}
           (last (:path tail))))
    (is (= 3 (:output-count (last (:trace result)))))))

(deftest partition-all-is-macro-agnostic-across-concrete-syntax-shapes
  (let [cases
        [{:label "binding vector"
          :query [[:form 'bindings] [:find 'a]
                  [:where {:parent-tag :vector}] [:partition-all 2]]
          :forms [["a" "(inc x)"] ["b" "(* a 2)"]]}
         {:label "map entries"
          :query [[:form 'settings] [:find :timeout] [:partition-all 2]]
          :forms [[":timeout" "1000"] [":mode" ":safe"]]}
         {:label "flattened anonymous function body"
          :query [[:form 'normalize] [:find 'select-keys]
                  [:where {:parent-tag :fn}] [:partition-all 2]]
          :forms [["select-keys" "%"] ["[:type :line :end-line]"]]}
         {:label "reader-conditional branch vector"
          :query [[:form 'platform] [:find :clj] :right :down
                  [:partition-all 2]]
          :forms [[":a" "1"] [":b" "2"]]}
         {:label "discard forms remain concrete semantic siblings"
          :query [[:form 'discarded] [:find :a] [:partition-all 2]]
          :forms [[":a" "#_obsolete"] ["1" ":b"] ["2"]]}
         {:label "metadata wrapper does not erase the selected vector"
          :query [[:form 'annotated] [:find :a] [:partition-all 2]]
          :forms [[":a" "1"] [":b" "2"]]}]]
    (doseq [{:keys [label query forms]} cases]
      (testing label
        (let [result (lens/evaluate-query shape-source query)]
          (is (nil? (:error result)))
          (is (= forms (mapv :forms (:matches result))))
          (is (= (count forms) (:match-count result)))
          (is (= (range (count forms))
                 (map #(get-in % [:partition :index]) (:matches result)))))))))

(deftest real-program-derived-inventories-become-one-structural-read-each
  (let [source (slurp "bench/fixtures/bench/pair_view.clj")
        case-result
        (lens/evaluate-query
          source
          [[:form 'route-event] [:find 'case] :up :down :right :right
           [:partition-all 2]])
        cond-result
        (lens/evaluate-query
          source
          [[:form 'classify-request] [:find '(nil? actor)]
           [:partition-all 2]])
        binding-result
        (lens/evaluate-query
          source
          [[:form 'prepare-request] [:find 'request-id]
           [:where {:parent-tag :vector}] [:partition-all 2]])]
    (testing "case pairs plus explicit optional-default remainder"
      (is (= 9 (:match-count case-result)))
      (is (= [:start :pause :resume :finish :cancel :retry
              :archive :restore]
             (mapv (comp read-string first :forms)
                   (butlast (:matches case-result)))))
      (is (= ["(assoc state :last-unknown-event event-type)"]
             (-> case-result :matches last :forms)))
      (is (false? (get-in case-result
                          [:matches 8 :partition :complete?]))))
    (testing "outer cond pairs keep the nested cond as one result subtree"
      (is (= 7 (:match-count cond-result)))
      (is (every? #(= 2 (:count %)) (:matches cond-result)))
      (is (str/starts-with? (get-in cond-result [:matches 5 :forms 1])
                            "(cond\n"))
      (is (= [":else" "{:decision :deny :reason :no-policy}"]
             (-> cond-result :matches last :forms))))
    (testing "binding pairs stop at the vector and ignore later symbol uses"
      (is (= 8 (:match-count binding-result)))
      (is (= ["request-id" "actor-id" "resource-id" "cache-key"
              "cached-decision" "timeout-ms" "retry-limit"
              "audit-context"]
             (mapv (comp first :forms) (:matches binding-result)))))))

(deftest cli-q-returns-the-complete-case-inventory-in-one-invocation
  (let [source-file (java.io.File/createTempFile "partition-all-cli" ".clj")]
    (try
      (spit source-file shape-source)
      (let [process (run-cli
                      ":op" ":q"
                      ":file" (.getPath source-file)
                      ":query" (str "[[:form route] [:find case] :up :down "
                                    ":right :right [:partition-all 2]]"))
            result (edn/read-string (:out process))]
        (is (zero? (:exit process)) (:err process))
        (is (= 3 (:match-count result)))
        (is (= [[":start" "(assoc state :status :running)"]
                [":finish" "(assoc state :status :done)"]
                ["state"]]
               (mapv :forms (:matches result))))
        (is (= [true true false]
               (mapv #(get-in % [:partition :complete?])
                     (:matches result)))))
      (finally
        (.delete source-file)))))

(deftest empty-singleton-even-and-odd-streams-have-total-results
  (let [source "(ns partition.edges)\n(defn f [] [:a :b :c :d])\n"
        none (lens/evaluate-query source
                                  [[:form 'f] [:find :missing]
                                   [:partition-all 2]])
        singleton (partitions source
                              [[:form 'f] [:find :d] [:partition-all 2]])
        even (partitions source
                         [[:form 'f] [:find :a] [:partition-all 2]])
        odd (partitions source
                        [[:form 'f] [:find :b] [:partition-all 2]])]
    (is (= 0 (:match-count none)))
    (is (= [] (:matches none)))
    (is (= [[":d"]] (mapv :forms singleton)))
    (is (= [[":a" ":b"] [":c" ":d"]] (mapv :forms even)))
    (is (= [[":b" ":c"] [":d"]] (mapv :forms odd)))))

(deftest multiple-inputs-retain-distinct-overlapping-partitions
  (let [source "(ns partition.overlap)\n(defn f [] [:a :a :a])\n"
        result (lens/evaluate-query source
                                    [[:form 'f] [:find :a]
                                     [:partition-all 2]])]
    (is (= 3 (:match-count result)))
    (is (= [[":a" ":a"] [":a"] [":a" ":a"]]
           (mapv :forms (:matches result))))
    (is (= 3 (count (set (map #(get-in % [:address :preorders])
                              (:matches result))))))))

(deftest partition-results-use-the-existing-bounded-evidence-contract
  (let [source (str "(ns partition.many)\n(def values ["
                    (str/join " " (map #(str ":v" %) (range 205)))
                    "])\n")
        result (lens/evaluate-query source
                                    [[:form 'values] [:find :v0]
                                     [:partition-all 1]])]
    (is (= 205 (:match-count result)))
    (is (= 100 (count (:matches result))))
    (is (true? (:matches-truncated? result)))
    (is (= 205 (:output-count (last (:trace result)))))))

(deftest invalid-partition-all-grammar-refuses-with-local-evidence
  (doseq [{:keys [label query]}
          [{:label "missing size" :query [[:partition-all]]}
           {:label "zero size" :query [[:partition-all 0]]}
           {:label "negative size" :query [[:partition-all -2]]}
           {:label "string size" :query [[:partition-all "2"]]}
           {:label "surplus argument" :query [[:partition-all 2 :extra]]}
           {:label "nonterminal read" :query [[:find :start]
                                                [:partition-all 2] :right]}
           {:label "ordinary replace" :query [[:find :start]
                                                [:partition-all 2]
                                                [:replace :begin]]}
           {:label "partition after span" :query [[:find :start] [:span 2]
                                                    [:partition-all 2]]}]]
    (testing label
      (let [result (lens/evaluate-lens shape-source
                                       {:file "shapes.clj" :query query})]
        (is (= :invalid-query (:error-type result)) (pr-str result))
        (is (integer? (:step-index result)))
        (is (vector? (:supported-query-steps result)))))))

(deftest partition-all-reuses-the-guarded-equal-arity-span-updater
  (let [source "(ns partition.edit)\n(defn f [] [:a\n  ;; keep me\n  1])\n"
        selection [[:form 'f] [:find :a] [:partition-all 2]]
        query (conj selection [:replace-span :b 2])
        plan (lens/evaluate-lens source {:file "edit.clj" :query query})
        applied (lens/apply-plan source plan)]
    (is (= :replace-span (:operation plan)))
    (is (= [":a" "1"] (-> plan :edits first :before-forms)))
    (is (= [":b" "2"] (-> plan :edits first :after-forms)))
    (is (= selection (get-in plan [:selector :selection-query])))
    (is (str/includes? (:diff plan) ";; keep me"))
    (is (:ok applied))
    (is (str/includes? (:source applied) ":b\n  ;; keep me\n  2"))
    (testing "the plan is replayed by addresses and fenced by the source hash"
      (let [stale (lens/apply-plan (str source "\n") plan)]
        (is (= :source-hash-mismatch (:error-type stale)))))))

(deftest partition-updater-refuses-zero-many-and-unequal-arity
  (let [source "(ns partition.refuse)\n(defn f [] [:a 1 :b 2 :tail])\n"
        no-match (lens/evaluate-lens
                   source
                   {:file "refuse.clj"
                    :query [[:find :missing] [:partition-all 2]
                            [:replace-span :x :y]]})
        many (lens/evaluate-lens
               source
               {:file "refuse.clj"
                :query [[:find :a] [:partition-all 2]
                        [:replace-span :x :y]]})
        mismatch (lens/evaluate-lens
                   source
                   {:file "refuse.clj"
                    :query [[:find :tail] [:partition-all 2]
                            [:replace-span :new-tail :extra]]})
        singleton (lens/evaluate-lens
                    source
                    {:file "refuse.clj"
                     :query [[:find :tail] [:partition-all 2]
                             [:replace-span :new-tail]]})]
    (is (= :no-match (:error-type no-match)))
    (is (= :ambiguous-match (:error-type many)))
    (is (= :span-arity-mismatch (:error-type mismatch)))
    (is (= 1 (:span-count mismatch)))
    (is (= 2 (:replacement-count mismatch)))
    (is (= :replace-span (:operation singleton)))
    (is (= [":tail"] (-> singleton :edits first :before-forms)))))
