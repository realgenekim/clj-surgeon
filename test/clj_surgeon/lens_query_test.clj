(ns clj-surgeon.lens-query-test
  (:require
   [clj-surgeon.structural-lens :as lens]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(def target-source
  "(ns field.lens)\n\n(defn transition [state event]\n  (case event\n    :start (assoc state :status :running)\n    :finish\n    ;; Preserve this comment between the pair.\n    (assoc state :status :done)\n    state))\n\n(defn classify [user]\n  (cond\n    (eligible? user) :accepted\n    :else :rejected))\n\n(defn settings []\n  {:timeout-ms 1000\n   :mode :safe})\n\n(defn fetch [cache source]\n  (let [cache-key (key-for source)\n        value (get cache cache-key)]\n    value))\n")

(def target-cases
  [{:label "case clause value"
    :query [[:form 'transition] [:find :finish] :right]
    :expected "(assoc state :status :done)"
    :inside "transition"
    :tag :list}
   {:label "cond branch value"
    :query [[:form 'classify] [:find '(eligible? user)] :right]
    :expected ":accepted"
    :inside "classify"
    :tag :token}
   {:label "map entry value"
    :query [[:form 'settings] [:find :timeout-ms] :right]
    :expected "1000"
    :inside "settings"
    :tag :token}
   {:label "binding value filters out later symbol uses"
    :query [[:form 'fetch]
            [:find 'cache-key]
            [:where {:parent-tag :vector}]
            :right]
    :expected "(key-for source)"
    :inside "fetch"
    :tag :list}])

(deftest query-pipeline-addresses-general-adjacent-syntax
  (doseq [{:keys [label query expected inside tag]} target-cases]
    (testing label
      (let [result (lens/evaluate-query target-source query)
            match (first (:matches result))]
        (is (nil? (:error result)))
        (is (= :lens (:operation result)))
        (is (= query (:query result)))
        (is (= 1 (:match-count result)))
        (is (= expected (:source match)))
        (is (= inside (:inside match)))
        (is (= tag (:tag match)))
        (is (integer? (get-in match [:address :preorder])))
        (is (pos-int? (:line match)))
        (is (= (count query) (count (:trace result))))
        (is (= 1 (:output-count (last (:trace result)))))
        (is (re-matches #"[0-9a-f]{64}" (:source-hash result)))))))

(deftest navigation-is-semantic-composable-and-auditable
  (testing "right skips whitespace and comments, while left returns the label"
    (let [right-query [[:form 'transition] [:find :finish] :right]
          round-trip (lens/evaluate-query target-source
                                          (conj right-query :left))]
      (is (= [":finish"] (mapv :source (:matches round-trip))))))
  (testing "up and down compose over the concrete syntax tree"
    (let [case-query [[:form 'transition] [:find :finish] :up]
          parent (first (:matches (lens/evaluate-query target-source case-query)))
          head (first (:matches
                        (lens/evaluate-query target-source
                                             (conj case-query :down))))]
      (is (= :list (:tag parent)))
      (is (str/starts-with? (:source parent) "(case event"))
      (is (= "case" (:source head)))))
  (testing "where can filter the current node tag"
    (let [result (lens/evaluate-query target-source
                                      [[:form 'settings]
                                       [:find '_]
                                       [:where {:tag :map}]])]
      (is (= ["{:timeout-ms 1000\n   :mode :safe}"]
             (mapv :source (:matches result)))))))

(deftest duplicate-same-line-values-keep-distinct-complete-file-addresses
  (let [source "(ns same.line)\n(defn f [x] (case x :a (inc 1) :b (inc 2)))\n"
        result (lens/evaluate-query source
                                    [[:form 'f]
                                     [:find '_]
                                     [:where {:tag :token :parent-tag :list}]])
        inc-arguments (filter #(#{"1" "2"} (:source %)) (:matches result))]
    (is (= 2 (count inc-arguments)))
    (is (= 1 (count (set (map :line inc-arguments)))))
    (is (= 2 (count (set (map #(get-in % [:address :preorder])
                              inc-arguments)))))))

(deftest reads-preserve-zero-and-many-as-bounded-evidence
  (testing "zero results are successful evidence"
    (let [result (lens/evaluate-query target-source
                                      [[:form 'transition] [:find :absent]])]
      (is (nil? (:error result)))
      (is (zero? (:match-count result)))
      (is (= [] (:matches result)))
      (is (= 0 (:output-count (last (:trace result)))))))
  (testing "large streams report the total and bound emitted evidence"
    (let [source (str "(ns many.nodes)\n"
                      (str/join "\n" (for [i (range 120)]
                                       (str "(def value-" i " " i ")")))
                      "\n")
          result (lens/evaluate-query source [[:find '_]])]
      (is (> (:match-count result) 100))
      (is (= 100 (count (:matches result))))
      (is (true? (:matches-truncated? result))))))

(deftest invalid-query-matrix-refuses-with-stable-local-evidence
  (let [too-long (vec (repeat 33 :down))
        cases [{:label "nil" :query nil}
               {:label "empty string" :query ""}
               {:label "non-vector EDN" :query "{:form transition}"}
               {:label "empty vector" :query []}
               {:label "too many steps" :query too-long}
               {:label "unknown navigation" :query [:sideways]
                :step-index 0}
               {:label "form is not first"
                :query [[:find :finish] [:form 'transition]]
                :step-index 1}
               {:label "malformed form" :query [[:form]] :step-index 0}
               {:label "malformed find" :query [[:find]] :step-index 0}
               {:label "where needs one map"
                :query [[:where :vector]] :step-index 0}
               {:label "unsupported where key"
                :query [[:where {:owner "f"}]] :step-index 0}
               {:label "where tag must be a keyword"
                :query [[:where {:tag "list"}]] :step-index 0}
               {:label "replace cannot be evaluated as a read"
                :query [[:find :finish] [:replace :done]] :step-index 1}]]
    (doseq [{:keys [label query step-index]} cases]
      (testing label
        (let [result (lens/evaluate-query target-source query)]
          (is (= :invalid-query (:error-type result)))
          (is (string? (:error result)))
          (is (= 0 (:match-count result)))
          (is (= [] (:matches result)))
          (is (vector? (:supported-query-steps result)))
          (when (some? step-index)
            (is (= step-index (:step-index result)))))))))

(deftest lens-expression-is-a-getter-and-a-planned-setter
  ;; Derived from the clean Codex case-edit benchmark fixture. The identical
  ;; expression in unrelated-finish is the condition that made the old route
  ;; require owner recovery; do not remove it while minimizing this regression.
  (let [source (str target-source
                    "\n(defn unrelated-finish [state]\n"
                    "  (assoc state :status :done))\n")
        selection [[:form 'transition] [:find :finish] :right]
        query (conj selection [:replace '(assoc state :status :complete)])
        plan (lens/evaluate-lens source {:file "src/field/lens.clj"
                                         :query query})]
    (testing "the terminal transformation produces a normal guarded plan"
      (is (nil? (:error plan)))
      (is (= :replace-subform (:operation plan)))
      (is (= query (get-in plan [:selector :query])))
      (is (= selection (get-in plan [:selector :selection-query])))
      (is (= 1 (:match-count plan)))
      (is (= "(assoc state :status :done)" (-> plan :edits first :before)))
      (is (= "(assoc state :status :complete)" (-> plan :edits first :after)))
      (is (str/includes? (:diff plan) "-(assoc state :status :done)"))
      (is (str/includes? (:diff plan) "+(assoc state :status :complete)")))
    (testing "application replays the address and preserves comments and peers"
      (let [applied (lens/apply-plan source plan)]
        (is (:ok applied))
        (is (= 1 (count (re-seq #"status :complete" (:source applied)))))
        (is (= 1 (count (re-seq #"status :done" (:source applied)))))
        (is (str/includes? (:source applied)
                           ";; Preserve this comment between the pair."))
        (is (some? (z/of-string (:source applied))))))))

(deftest planned-setter-refuses-zero-many-invalid-and-misplaced-transforms
  (let [cases [{:label "zero selection"
                :query [[:form 'transition]
                        [:find :absent]
                        [:replace :done]]
                :error-type :no-match}
               {:label "ambiguous selection"
                :query [[:find '(assoc state :status :done)]
                        [:replace '(assoc state :status :complete)]]
                :source (str target-source
                             "\n(defn peer [state]"
                             " (assoc state :status :done))\n")
                :error-type :ambiguous-match}
               {:label "invalid replacement"
                :query [[:form 'transition]
                        [:find :finish]
                        :right
                        [:replace "(broken"]]
                :error-type :invalid-replacement}
               {:label "transform must be final"
                :query [[:find :finish] [:replace :done] :right]
                :error-type :invalid-query}
               {:label "only one transform"
                :query [[:find :finish]
                        [:replace :done]
                        [:replace :really-done]]
                :error-type :invalid-query}]]
    (doseq [{:keys [label query source error-type]} cases]
      (testing label
        (let [result (lens/evaluate-lens (or source target-source)
                                         {:file "state.clj" :query query})]
          (is (= error-type (:error-type result)))
          (is (nil? (:result-hash result))))))))

(deftest current-clj-surgeon-is-queryable-by-its-own-algebra
  (let [source (slurp "src/clj_surgeon/core.clj")
        result (lens/evaluate-query source
                                    [[:form 'parse-args]
                                     [:find #{:match :with :contains}]])]
    (is (= 1 (:match-count result)))
    (is (= "#{:match :with :contains}" (-> result :matches first :source)))
    (is (= "parse-args" (-> result :matches first :inside)))))
