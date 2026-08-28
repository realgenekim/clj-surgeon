(ns clj-surgeon.experiments.mcp-candidate-catalog-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-catalog :as candidate]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.test :refer [deftest is run-tests testing]]))

(defn- tool-by-id
  [tools id]
  (some #(when (= id (:id %)) %) tools))

(defn- tool-by-name
  [tools name]
  (some #(when (= name (:name %)) %) tools))

(deftest catalog-name-validation
  (is (= :A (candidate/normalize-catalog :A)))
  (is (= :L (candidate/normalize-catalog "L")))
  (is (= [:A :L :C :M :N :O] candidate/supported-catalogs))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Unsupported MCP candidate catalog"
                        (candidate/normalize-catalog :unknown))))

(deftest rename-only-catalogs-preserve-mechanics
  (let [base (mcp-server/public-tool-registry)
        base-edit (tool-by-id base :edit-clojure)
        base-change (tool-by-id base :clj-change)]
    (doseq [[catalog expected-change-name]
            [[:A "apply_clojure_changes"]
             [:L "apply_clojure_plan"]
             [:C "refactor_clojure"]]]
      (testing (name catalog)
        (let [tools (candidate/catalog-tools catalog base)
              edit (tool-by-id tools :edit-clojure)
              change (tool-by-id tools :clj-change)]
          (is (= (count base) (count tools)))
          (is (= "edit_clojure" (:name edit)))
          (is (= expected-change-name (:name change)))
          (is (identical? (:tool-fn base-edit) (:tool-fn edit)))
          (is (identical? (:tool-fn base-change) (:tool-fn change)))
          (is (= (:schema base-edit) (:schema edit)))
          (is (= (:schema base-change) (:schema change)))
          (is (= (:output-schema base-edit) (:output-schema edit)))
          (is (= (:output-schema base-change) (:output-schema change))))))))

(deftest split-catalog-preserves-handler-and-output-semantics
  (let [base (mcp-server/public-tool-registry)
        base-edit (tool-by-id base :edit-clojure)
        base-change (tool-by-id base :clj-change)
        tools (candidate/catalog-tools :M base)
        edit (tool-by-name tools "edit_clojure")
        extraction (tool-by-name tools "extract_clojure")
        plan (tool-by-name tools "apply_clojure_plan")]
    (is (= ["inspect_clojure" "edit_clojure" "extract_clojure"
            "apply_clojure_plan" "transform_clojure"]
           (mapv :name tools)))
    (doseq [tool [edit extraction plan]]
      (is (identical? (:tool-fn base-change) (:tool-fn tool)))
      (is (= (:output-schema base-change) (:output-schema tool))))
    (is (= (:outcome-classes base-edit) (:outcome-classes edit)))
    (is (= (:outcome-classes base-change) (:outcome-classes extraction)))
    (is (= (:outcome-classes base-change) (:outcome-classes plan)))
    (is (= #{"workspace_root" "edits" "programs" "delete_owners"
             "changes" "expect"}
           (set (keys (get-in edit [:schema :properties])))))
    (is (= #{"workspace_root" "extraction" "verify"}
           (set (keys (get-in extraction [:schema :properties])))))
    (is (= #{"workspace_root" "basis" "decisions" "verify"}
           (set (keys (get-in plan [:schema :properties])))))
    (is (= 2 (count (get-in edit [:schema :oneOf]))))
    (is (= 1 (count (get-in extraction [:schema :oneOf]))))
    (is (= 1 (count (get-in plan [:schema :oneOf]))))
    (is (re-find #"compiles, commits, and verifies the extraction in this call"
                 (:description extraction)))
    (is (re-find #"from the inspect_clojure next call"
                 (:description plan)))
    (is (re-find #"standalone computed preview or commit"
                 (:description edit)))
    (is (= :not-implemented
           (get-in (candidate/catalog-report :M)
                   [:deferred-factorials 0 :status])))))

(deftest split-catalog-does-not-increase-total-schema-characters
  (let [base (candidate/catalog-tools :A)
        split (candidate/catalog-tools :M)
        base-size (get-in (candidate/schema-characters base) [:mutation-total])
        split-size (get-in (candidate/schema-characters split) [:mutation-total])]
    (is (pos? base-size))
    (is (pos? split-size))
    (is (<= split-size base-size)
        {:base base-size :split split-size})))

(deftest atomic-chord-aliases-preserve-split-catalog
  (let [split (candidate/catalog-tools :M)
        aliased (candidate/catalog-tools :N)]
    (is (= ["inspect_clojure" "edit_clojure" "extract_clojure"
            "apply_clojure_plan" "transform_clojure_with_clojure"]
           (mapv :name aliased)))
    (is (= (mapv :schema split) (mapv :schema aliased)))
    (is (every? true? (map identical?
                           (map :tool-fn split)
                           (map :tool-fn aliased))))
    (is (= (get-in (candidate/schema-characters split) [:total])
           (get-in (candidate/schema-characters aliased) [:total])))))

(deftest data-versus-program-aliases-preserve-split-catalog
  (let [split (candidate/catalog-tools :M)
        aliased (candidate/catalog-tools :O)]
    (is (= ["inspect_clojure" "apply_clojure_edits" "extract_clojure"
            "apply_clojure_plan" "run_clojure_transform"]
           (mapv :name aliased)))
    (is (= (mapv :schema split) (mapv :schema aliased)))
    (is (every? true? (map identical?
                           (map :tool-fn split)
                           (map :tool-fn aliased))))))

(deftest option-m-classifies-every-pair-and-falsifier
  (let [report (candidate/catalog-report :M)
        {:keys [controls overlap-matrix falsifier-cards]}
        (:orthogonality report)
        pairs (map :pair overlap-matrix)]
    (is (= 5 (count controls)))
    (is (= 10 (count pairs)))
    (is (= 10 (count (set (map set pairs)))))
    (is (every? :classification overlap-matrix))
    (is (= :defect
           (:classification
             (some #(when (= [:edit :transform] (:pair %)) %)
                   overlap-matrix))))
    (is (= #{:standalone-program :mixed-computed-chord :complete-extraction
             :ambiguous-extraction :complete-edit-with-exact-verification
             :retained-basis :invented-plan}
           (set (map :id falsifier-cards))))))

(deftest isolated-start-projects-the-selected-catalog
  (let [observed (atom nil)]
    (with-redefs [http-server/start
                  (fn [opts]
                    (reset! observed {:opts opts
                                      :tools (mcp-tool/all-tools)})
                    :stopped)]
      (is (= :stopped
             (candidate/start {:catalog :L
                               :project-dir "/tmp/example"
                               :port 0})))
      (is (= "/tmp/example" (get-in @observed [:opts :project-dir])))
      (is (not (contains? (:opts @observed) :catalog)))
      (is (= ["inspect_clojure" "apply_clojure_plan" "edit_clojure"
              "transform_clojure"]
             (mapv :name (:tools @observed))))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"require the full tool profile"
                        (candidate/start {:catalog :A
                                          :tool-profile :edit}))))

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'clj-surgeon.experiments.mcp-candidate-catalog-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
