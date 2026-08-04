(ns clj-surgeon.outermost-test
  (:require
   [clj-surgeon.structural-lens :as lens]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private nested-source
  (str "(ns outermost.shapes)\n"
       "\n"
       "(defn nested []\n"
       "  [(outer (middle (inner :deep)))\n"
       "   (second-root (child :other))])\n"
       "\n"
       "(defn siblings [] [(a) (b) (c)])\n"
       "\n"
       "(defn duplicate-parent [] [:x :x])\n"
       "\n"
       "(def platform\n"
       "  ^:private\n"
       "  #?(:clj (node\n"
       "            ;; Retain this comment in the outer node.\n"
       "            (node :clj))\n"
       "     :cljs (node (node :cljs))))\n"))

(deftest outermost-keeps-maximal-current-subtrees-in-stable-order
  (let [result (lens/evaluate-query
                 nested-source
                 [[:form 'nested]
                  :down :right :right :right
                  [:find '_]
                  [:where {:tag :list}]
                  :outermost])]
    (is (nil? (:error result)))
    (is (= 2 (:match-count result)))
    (is (= ["(outer (middle (inner :deep)))"
            "(second-root (child :other))"]
           (mapv :source (:matches result))))
    (is (= 5 (:input-count (last (:trace result)))))
    (is (= 2 (:output-count (last (:trace result)))))
    (is (= :outermost (:step (last (:trace result)))))))

(deftest outermost-preserves-zero-singleton-sibling-and-deduplicated-streams
  (testing "zero remains zero"
    (let [result (lens/evaluate-query nested-source
                                      [[:find :missing] :outermost])]
      (is (= 0 (:match-count result)))
      (is (= [] (:matches result)))))
  (testing "one remains one"
    (let [result (lens/evaluate-query nested-source
                                      [[:form 'nested]
                                       [:find '(inner :deep)]
                                       :outermost])]
      (is (= ["(inner :deep)"] (mapv :source (:matches result))))))
  (testing "siblings do not contain one another"
    (let [result (lens/evaluate-query
                   nested-source
                   [[:form 'siblings]
                    [:find '_]
                    [:where {:tag :list :parent-tag :vector}]
                    :outermost])]
      (is (= ["(a)" "(b)" "(c)"] (mapv :source (:matches result))))))
  (testing "equal locations deduplicate before filtering"
    (let [result (lens/evaluate-query nested-source
                                      [[:form 'duplicate-parent]
                                       [:find :x] :up :outermost])]
      (is (= 1 (:match-count result)))
      (is (= "[:x :x]" (-> result :matches first :source))))))

(deftest containment-is-structural-across-different-node-tags
  (let [result (lens/evaluate-query
                 nested-source
                 [[:form 'nested]
                  :down :right :right :right
                  [:find '_]
                  :outermost])]
    (is (= 1 (:match-count result)))
    (is (= :vector (-> result :matches first :tag)))
    (is (str/starts-with? (-> result :matches first :source)
                          "[(outer"))))

(deftest reader-conditionals-metadata-and-comments-do-not-change-ancestry
  (let [result (lens/evaluate-query nested-source
                                    [[:form 'platform]
                                     [:find 'node] :up :outermost])]
    (is (= 2 (:match-count result)))
    (is (= ["(node\n            ;; Retain this comment in the outer node.\n            (node :clj))"
            "(node (node :cljs))"]
           (mapv :source (:matches result))))
    (is (every? #(= :list (:tag %)) (:matches result)))))

(deftest nested-cond-inventory-is-one-exact-query-without-a-known-first-guard
  (let [source (slurp "bench/fixtures/bench/pair_view.clj")
        result (lens/evaluate-query
                 source
                 [[:form 'classify-request]
                  [:find 'cond] :up :outermost :down :right
                  [:partition-all 2]])]
    (is (= 7 (:match-count result)))
    (is (every? #(= 2 (:count %)) (:matches result)))
    (is (= ["(nil? actor)" "{:decision :deny :reason :missing-actor}"]
           (-> result :matches first :forms)))
    (is (str/starts-with? (get-in result [:matches 5 :forms 1])
                          "(cond\n"))
    (is (= [":else" "{:decision :deny :reason :no-policy}"]
           (-> result :matches last :forms)))))

(deftest nested-case-inventory-retains-two-disjoint-outer-owners
  (let [source (slurp "bench/fixtures/bench/pair_view.clj")
        result (lens/evaluate-query
                 source
                 [[:form 'route-two-dimensions]
                  [:find 'case] :up :outermost :down :right :right
                  [:partition-all 2]])]
    (is (= 4 (:match-count result)))
    (is (= [[":online"
             "(case event\n       :start :run\n       :stop :halt)"]
            [":offline" ":queue"]
            [":start"
             "(case mode\n       :online :immediate\n       :offline :deferred)"]
            [":stop" ":halt"]]
           (mapv :forms (:matches result))))
    (is (every? #(true? (get-in % [:partition :complete?]))
                (:matches result)))))

(deftest outermost-composes-with-one-guarded-update
  (let [source "(ns outermost.edit)\n(defn f [x] (case x :a 1))\n"
        query [[:form 'f] [:find 'case] :up :outermost
               :down :right [:replace 'event]]
        plan (lens/evaluate-lens source {:file "edit.clj" :query query})
        applied (lens/apply-plan source plan)]
    (is (= :replace-subform (:operation plan)))
    (is (= "x" (-> plan :edits first :before)))
    (is (= "event" (-> plan :edits first :after)))
    (is (:ok applied))
    (is (str/includes? (:source applied) "(case event :a 1)"))))

(deftest disjoint-outermost-owners-still-refuse-an-ambiguous-update
  (let [source (slurp "bench/fixtures/bench/pair_view.clj")
        result (lens/evaluate-lens
                 source
                 {:file "pair_view.clj"
                  :query [[:form 'route-two-dimensions]
                          [:find 'case] :up :outermost
                          :down :right [:replace 'dispatch]]})]
    (is (= :ambiguous-match (:error-type result)))
    (is (= 2 (:match-count result)))
    (is (= ["mode" "event"] (mapv :source (:matches result))))))

(deftest invalid-outermost-grammar-and-terminal-placement-refuse
  (doseq [{:keys [label query]}
          [{:label "vector spelling" :query [[:outermost]]}
           {:label "vector spelling with argument"
            :query [[:outermost :extra]]}
           {:label "after span"
            :query [[:find :x] [:span 2] :outermost]}
           {:label "after partition"
            :query [[:find :x] [:partition-all 2] :outermost]}]]
    (testing label
      (let [result (lens/evaluate-query nested-source query)]
        (is (= :invalid-query (:error-type result)))
        (is (integer? (:step-index result)))
        (is (vector? (:supported-query-steps result)))))))

(deftest operational-surfaces-preserve-the-placement-contract
  (doseq [[surface path]
          {"README" "README.md"
           "canonical skill" "skills/clj-surgeon/SKILL.md"
           "legacy skill" "skill.md"
           "repository instructions" "CLAUDE.md"
           "vision" "docs/vision.md"
           "changelog" "CHANGELOG.md"}]
    (testing surface
      (let [text (slurp path)]
        (is (str/includes? text ":outermost"))
        (is (str/includes? text ":up :outermost"))
        (is (str/includes? text ":outermost :up"))))))
