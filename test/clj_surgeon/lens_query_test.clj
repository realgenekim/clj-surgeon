(ns clj-surgeon.lens-query-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.structural-lens :as lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
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
                                     [:find #{:match :with :contains :query}]])]
    (is (= 1 (:match-count result)))
    (is (= "#{:match :with :contains :query}" (-> result :matches first :source)))
    (is (= "parse-args" (-> result :matches first :inside)))))

(deftest sibling-spans-address-peer-shaped-and-flattened-syntax
  (let [cases [{:label "case pair with internal comment"
                :source target-source
                :query [[:form 'transition] [:find :finish] [:span 2]]
                :forms [":finish" "(assoc state :status :done)"]
                :source-fragment ":finish\n    ;; Preserve this comment between the pair.\n    (assoc state :status :done)"}
               {:label "cond pair"
                :source target-source
                :query [[:form 'classify] [:find '(eligible? user)] [:span 2]]
                :forms ["(eligible? user)" ":accepted"]}
               {:label "map pair"
                :source target-source
                :query [[:form 'settings] [:find :timeout-ms] [:span 2]]
                :forms [":timeout-ms" "1000"]}
               {:label "binding pair"
                :source target-source
                :query [[:form 'fetch] [:find 'cache-key]
                        [:where {:parent-tag :vector}] [:span 2]]
                :forms ["cache-key" "(key-for source)"]}
               {:label "anonymous function body siblings"
                :source "(ns flat.fn)\n(def f #(select-keys % [:type :line :end-line]))\n"
                :query [[:form 'f] [:find 'select-keys]
                        [:where {:parent-tag :fn}] [:span 3]]
                :forms ["select-keys" "%" "[:type :line :end-line]"]}]]
    (doseq [{:keys [label source query forms source-fragment]} cases]
      (testing label
        (let [result (lens/evaluate-query source query)
              match (first (:matches result))]
          (is (= 1 (:match-count result)))
          (is (= :span (:tag match)))
          (is (= forms (:forms match)))
          (is (= (count forms) (:count match)))
          (is (= (count forms) (count (get-in match [:address :preorders]))))
          (when source-fragment
            (is (= source-fragment (:source match)))))))))

(deftest span-updater-plans-one-slice-and-preserves-all-internal-trivia
  (let [query [[:form 'transition] [:find :finish] [:span 2]
               [:replace-span :finish (list 'assoc 'state :status :complete)]]
        plan (lens/evaluate-lens target-source {:file "state.clj" :query query})
        applied (lens/apply-plan target-source plan)]
    (is (= :replace-span (:operation plan)))
    (is (= 1 (:match-count plan)))
    (is (= [":finish" "(assoc state :status :done)"]
           (-> plan :edits first :before-forms)))
    (is (= [":finish" "(assoc state :status :complete)"]
           (-> plan :edits first :after-forms)))
    (is (str/includes? (:diff plan) ";; Preserve this comment between the pair."))
    (is (:ok applied))
    (is (str/includes? (:source applied)
                       ":finish\n    ;; Preserve this comment between the pair.\n    (assoc state :status :complete)"))
    (is (str/includes? (:source applied) ":start (assoc state :status :running)"))))

(deftest span-reads-report-overlap-and-boundaries-while-plans-refuse-ambiguity
  (let [source "(ns spans)\n(defn f [] [:a :a :a])\n"
        query [[:form 'f] [:find :a] [:span 2]]
        read (lens/evaluate-query source query)
        plan (lens/evaluate-lens source
                                 {:file "spans.clj"
                                  :query (conj query [:replace-span :a :b])})
        boundary (lens/evaluate-query source
                                      [[:form 'f] [:find :a] [:span 4]])]
    (is (= 2 (:match-count read)) "two overlapping windows are evidence")
    (is (= :ambiguous-match (:error-type plan)))
    (is (= 0 (:match-count boundary)) "a span never crosses its parent")))

(deftest invalid-span-grammar-and-replacement-arity-refuse-before-planning
  (doseq [{:keys [query error-type]}
          [{:query [[:span 0]] :error-type :invalid-query}
           {:query [[:span -1]] :error-type :invalid-query}
           {:query [[:span "2"]] :error-type :invalid-query}
           {:query [[:form 'transition] [:span 2] :right]
            :error-type :invalid-query}
           {:query [[:form 'transition] [:replace-span :x]]
            :error-type :invalid-query}
           {:query [[:form 'transition] [:find :finish] [:span 2]
                    [:replace-span :finish]]
            :error-type :span-arity-mismatch}]]
    (let [result (lens/evaluate-lens target-source
                                     {:file "state.clj" :query query})]
      (is (= error-type (:error-type result)) (pr-str query)))))

(deftest agent-facing-surfaces-teach-one-jq-like-getter-updater-route
  (let [surfaces {"README" (slurp "README.md")
                  "repository instructions" (slurp "CLAUDE.md")
                  "vision" (slurp "docs/vision.md")
                  "installed skill" (slurp "skills/clj-surgeon/SKILL.md")
                  "legacy skill" (slurp "skill.md")
                  "changelog" (slurp "CHANGELOG.md")}]
    (doseq [[surface body] surfaces]
      (testing surface
        (is (str/includes? body ":q"))
        (is (str/includes? body "[:form transition]"))
        (is (str/includes? body "[:find :finish]"))
        (is (str/includes? body ":right"))
        (is (str/includes? body "[:span 2]"))
        (is (str/includes? body "[:partition-all 2]"))
        (is (str/includes? body "[:replace-span"))
        (is (str/includes? body "[:replace"))
        (is (str/includes? body ":replace-subform!"))))))

(def ^:private project-root
  (.getCanonicalPath (io/file ".")))

(defn- run-cli [& args]
  @(proc/process
     (into ["bb" "-m" "clj-surgeon.core"] args)
     {:dir project-root :err :string :out :string}))

(deftest cli-lens-and-q-read-plan-and-apply-the-documented-expression
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon q "})
        source-file (fs/path tmp-dir "state fixture.clj")
        plan-file (fs/path tmp-dir "lens plan.edn")
        source (str target-source
                    "\n(defn unrelated-finish [state]\n"
                    "  (assoc state :status :done))\n")
        read-query "[[:form transition] [:find :finish] :right]"
        edit-query (str "[[:form transition] [:find :finish] :right "
                        "[:replace (assoc state :status :complete)]]")]
    (try
      (spit (str source-file) source)
      (testing "canonical and short operations return identical read evidence"
        (let [canonical (run-cli ":op" ":lens" ":file" (str source-file)
                                 ":query" read-query)
              short (run-cli ":op" ":q" ":file" (str source-file)
                             ":query" read-query)
              canonical-result (edn/read-string (:out canonical))
              short-result (edn/read-string (:out short))]
          (is (zero? (:exit canonical)) (:err canonical))
          (is (zero? (:exit short)) (:err short))
          (is (= canonical-result short-result))
          (is (= :lens (:operation canonical-result)))
          (is (= "(assoc state :status :done)"
                 (-> canonical-result :matches first :source)))))
      (testing "the updater emits a saved plan, then the existing applier emits proof"
        (let [planned (run-cli ":op" ":q" ":file" (str source-file)
                               ":query" edit-query
                               ":plan-out" (str plan-file))
              plan (edn/read-string (:out planned))
              saved (edn/read-string (slurp (str plan-file)))
              applied (run-cli ":op" ":replace-subform!"
                               ":plan" (str plan-file))
              receipt (edn/read-string (:out applied))]
          (is (zero? (:exit planned)) (:err planned))
          (is (= (dissoc plan :plan-out) saved))
          (is (= :replace-subform (:operation plan)))
          (is (= (edn/read-string edit-query) (get-in plan [:selector :query])))
          (is (zero? (:exit applied)) (:err applied))
          (is (= (:result-hash plan) (get-in receipt [:verified :read-back-hash])))
          (is (= 1 (count (re-seq #"status :complete"
                                  (slurp (str source-file))))))
          (is (= 1 (count (re-seq #"status :done"
                                  (slurp (str source-file))))))))
      (testing "invalid pipelines are nonzero structured EDN"
        (let [result (run-cli ":op" ":q" ":file" (str source-file)
                              ":query" "[:sideways]")
              refusal (edn/read-string (:out result))]
          (is (pos? (:exit result)))
          (is (= :invalid-query (:error-type refusal)))
          (is (= 0 (:step-index refusal)))
          (is (str/blank? (:err result)))))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest cli-q-span-plan-applies-through-the-existing-verified-executor
  (let [tmp-dir (fs/create-temp-dir {:prefix "clj surgeon span "})
        source-file (fs/path tmp-dir "span fixture.clj")
        plan-file (fs/path tmp-dir "span plan.edn")
        query (str "[[:form transition] [:find :finish] [:span 2] "
                   "[:replace-span :finish (assoc state :status :complete)]]")]
    (try
      (spit (str source-file) target-source)
      (let [planned (run-cli ":op" ":q" ":file" (str source-file)
                             ":query" query ":plan-out" (str plan-file))
            plan (edn/read-string (:out planned))
            applied (run-cli ":op" ":replace-subform!"
                             ":plan" (str plan-file))
            receipt (edn/read-string (:out applied))]
        (is (zero? (:exit planned)) (:err planned))
        (is (= :replace-span (:operation plan)))
        (is (= [18 19] (-> plan :edits first :addresses)))
        (is (zero? (:exit applied)) (:err applied))
        (is (= :replace-span (:planned-operation receipt)))
        (is (= (:result-hash plan) (get-in receipt [:verified :read-back-hash])))
        (is (str/includes? (slurp (str source-file))
                           ";; Preserve this comment between the pair.")))
      (finally
        (fs/delete-tree tmp-dir)))))
