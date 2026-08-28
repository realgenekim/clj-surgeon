(ns clj-surgeon.experiments.mcp-candidate-catalog-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-catalog :as candidate]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests testing]]))

(defn- tool-by-id
  [tools id]
  (some #(when (= id (:id %)) %) tools))

(defn- tool-by-name
  [tools name]
  (some #(when (= name (:name %)) %) tools))

(defn- canonical-handler [tool]
  (get (meta (:tool-fn tool))
       :clj-surgeon.experiments.mcp-candidate-response/canonical-handler))

(deftest catalog-name-validation
  (is (= :A (candidate/normalize-catalog :A)))
  (is (= :L (candidate/normalize-catalog "L")))
  (is (= [:A :L :C :M :N :O :P :Q :R :S :T :U :V :W :X]
         candidate/supported-catalogs))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Unsupported MCP candidate catalog"
                        (candidate/normalize-catalog :unknown))))

(deftest every-catalog-defines-one-complete-role-lexicon
  (doseq [catalog candidate/supported-catalogs]
    (testing (name catalog)
      (let [lexicon (candidate/catalog-lexicon catalog)]
        (is (= (set candidate/public-role-keys) (set (keys lexicon))))
        (is (every? #(and (string? %) (not-empty %)) (vals lexicon))))))
  (is (= {:inspect "inspect_clojure"
          :edit "edit_clojure"
          :extract "extract_clojure"
          :plan "apply_clojure_plan"
          :transform-preview "transform_clojure_with_clojure"
          :transform-commit "transform_clojure_with_clojure"}
         (candidate/catalog-lexicon :N)))
  (is (= {:inspect "inspect_clojure"
          :edit "write_clojure_edits"
          :extract "move_clojure_owners"
          :plan "apply_clojure_plan"
          :transform-preview "preview_clojure_transform"
          :transform-commit "apply_clojure_transform"}
         (candidate/catalog-lexicon :T))))

(deftest catalog-role-receipt-is-one-transport-neutral-scorer-contract
  (is (= {:catalog "T"
          :roles {:edit "write_clojure_edits"
                  :extract "move_clojure_owners"
                  :inspect "inspect_clojure"
                  :plan "apply_clojure_plan"
                  :transform-commit "apply_clojure_transform"
                  :transform-preview "preview_clojure_transform"}}
         (candidate/catalog-role-receipt :T))))

(deftest catalog-runtime-receipt-retains-the-exact-advertised-surface
  (let [receipt (candidate/catalog-runtime-receipt :V)]
    (is (= (candidate/catalog-role-receipt :V)
           (select-keys receipt [:catalog :roles])))
    (is (= (mapv :name (candidate/catalog-tools :V))
           (:tool-order receipt)
           (mapv :name (:tools receipt))))
    (is (= (candidate/catalog-instructions :V) (:instructions receipt)))
    (is (every? #(and (string? (:description %))
                      (map? (:input-schema %))
                      (map? (:output-schema %))
                      (or (nil? (:annotations %))
                          (map? (:annotations %))))
                (:tools receipt)))))

(deftest alternate-catalogs-expose-one-self-consistent-pre-call-universe
  (doseq [catalog candidate/supported-catalogs]
    (testing (name catalog)
      (let [{:keys [initialize-instructions tools]}
            (candidate/caller-visible-surface catalog)]
        (is (string? initialize-instructions))
        (is (= (set (vals (candidate/catalog-lexicon catalog)))
               (set (map :name tools))))
        (is (empty? (candidate/unavailable-public-name-leaks catalog))))))
  (testing "N may name edit_clojure but cannot leak either unavailable legacy mutation"
    (let [leaks (set (map :name
                          (candidate/unavailable-public-name-leaks :N)))
          visible (candidate/caller-visible-strings :N)]
      (is (some #{"edit_clojure"} visible))
      (is (not (contains? leaks "apply_clojure_changes")))
      (is (not (contains? leaks "transform_clojure")))
      (is (not-any? #(re-find #"(?<![A-Za-z0-9_.!_-])apply_clojure_changes(?![A-Za-z0-9_.!_-])" %)
                    visible))
      (is (not-any? #(re-find #"(?<![A-Za-z0-9_.!_-])transform_clojure(?![A-Za-z0-9_.!_-])" %)
                    visible))))
  (testing "T exposes none of the three legacy mutation names"
    (let [visible (candidate/caller-visible-strings :T)]
      (doseq [legacy ["apply_clojure_changes" "edit_clojure"
                      "transform_clojure"]]
        (is (not-any? #(.contains ^String % legacy) visible)
            {:legacy legacy})))))

(deftest leak-detector-catches-the-exact-n-and-t-regressions
  (let [real-catalog-tools candidate/catalog-tools
        base-tools (mcp-server/public-tool-registry)
        inject-description
        (fn [catalog leaked-names]
          (update-in (real-catalog-tools catalog base-tools) [0 :description]
                     str " " (str/join " " leaked-names)))]
    (with-redefs [candidate/catalog-tools
                  (fn
                    ([catalog]
                     (inject-description catalog
                                         ["apply_clojure_changes"
                                          "transform_clojure"]))
                    ([catalog _base-tools]
                     (inject-description catalog
                                         ["apply_clojure_changes"
                                          "transform_clojure"])))]
      (is (= #{"apply_clojure_changes" "transform_clojure"}
             (set (map :name
                       (candidate/unavailable-public-name-leaks :N))))))
    (with-redefs [candidate/catalog-tools
                  (fn
                    ([catalog]
                     (inject-description catalog
                                         ["apply_clojure_changes"
                                          "edit_clojure"
                                          "transform_clojure"]))
                    ([catalog _base-tools]
                     (inject-description catalog
                                         ["apply_clojure_changes"
                                          "edit_clojure"
                                          "transform_clojure"])))]
      (is (= #{"apply_clojure_changes" "edit_clojure"
               "transform_clojure"}
             (set (map :name
                       (candidate/unavailable-public-name-leaks :T))))))))

(deftest catalog-projection-does-not-mutate-canonical-registration
  (let [instructions mcp-server/server-instructions
        contracts (mcp-server/tool-contracts
                    (mcp-server/public-tool-registry))]
    (doseq [catalog candidate/supported-catalogs]
      (candidate/caller-visible-surface catalog))
    (is (= instructions mcp-server/server-instructions))
    (is (= contracts
           (mcp-server/tool-contracts
             (mcp-server/public-tool-registry))))))

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
          (is (identical? (:tool-fn base-edit) (canonical-handler edit)))
          (is (identical? (:tool-fn base-change) (canonical-handler change)))
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
      (is (identical? (:tool-fn base-change) (canonical-handler tool)))
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
    (is (re-find #"one standalone computed relation"
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
                           (map canonical-handler split)
                           (map canonical-handler aliased))))
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
                           (map canonical-handler split)
                           (map canonical-handler aliased))))))

(deftest name-only-extraction-family-preserves-every-other-public-byte
  (let [catalogs [:U :V :W :X]
        tools-by-catalog (into {} (map (juxt identity candidate/catalog-tools)
                                       catalogs))
        extraction-names (set (map #(get-in (candidate/catalog-lexicon %)
                                            [:extract])
                                   catalogs))
        normalize-extraction-name
        (fn [tools]
          (mapv (fn [tool]
                  (cond-> (assoc tool :tool-fn (canonical-handler tool))
                    (contains? extraction-names (:name tool))
                    (assoc :name "EXTRACTION_CANDIDATE")))
                tools))]
    (is (= #{"apply_clojure_changes"
             "apply_clojure_extraction"
             "extract_clojure"
             "move_clojure_forms"}
           extraction-names))
    (is (apply = (map (comp normalize-extraction-name tools-by-catalog)
                      catalogs)))
    (doseq [catalog catalogs]
      (let [tools (tools-by-catalog catalog)]
        (is (= ["inspect_clojure"
                "edit_clojure"
                (get-in (candidate/catalog-lexicon catalog) [:extract])
                "continue_clojure_plan"
                "transform_clojure"]
               (mapv :name tools)))))))

(deftest effect-catalogs-preserve-handlers-and-separate-preview-from-commit
  (let [base (mcp-server/public-tool-registry)
        base-transform (tool-by-id base :transform-clojure)
        expected-names
        {:P ["inspect_clojure" "commit_clojure_edits"
             "commit_clojure_extraction" "apply_clojure_plan"
             "preview_clojure_transform" "commit_clojure_transform"]
         :Q ["inspect_clojure" "clojure.edit.commit"
             "clojure.extract.commit" "clojure.plan.apply"
             "clojure.transform.preview" "clojure.transform.commit"]
         :R ["inspect_clojure" "edit_clojure_commit"
             "extract_clojure_commit" "apply_clojure_plan"
             "transform_clojure_preview" "transform_clojure_commit"]
         :S ["inspect_clojure" "edit_clojure_bang"
             "extract_clojure_bang" "apply_clojure_plan_bang"
             "transform_clojure_with_clojure"
             "transform_clojure_with_clojure_bang"]
         :T ["inspect_clojure" "write_clojure_edits"
             "move_clojure_owners" "apply_clojure_plan"
             "preview_clojure_transform" "apply_clojure_transform"]}]
    (doseq [[catalog names] expected-names]
      (testing (name catalog)
        (let [tools (candidate/catalog-tools catalog base)
              preview (nth tools 4)
              commit (nth tools 5)]
          (is (= names (mapv :name tools)))
          (is (every? #(re-matches #"[A-Za-z0-9_.-]+" (:name %)) tools))
          (is (identical? (:tool-fn base-transform) (canonical-handler preview)))
          (is (identical? (:tool-fn base-transform) (canonical-handler commit)))
          (is (= (:output-schema base-transform) (:output-schema preview)))
          (is (= (:output-schema base-transform) (:output-schema commit)))
          (is (nil? (get-in preview [:schema :properties "commit"])))
          (is (= true (get-in commit [:schema :properties "commit" :const])))
          (is (some #{"commit"} (:required (:schema commit)))))))))

(deftest effect-catalogs-publish-conservative-mcp-annotations
  (doseq [catalog [:P :Q :R :S :T]]
    (testing (name catalog)
      (let [tools (candidate/catalog-tools catalog)
            inspect (first tools)
            mutations (subvec tools 1 4)
            preview (nth tools 4)
            transform-commit (nth tools 5)]
        (is (= true (get-in inspect [:annotations :read-only])))
        (doseq [tool (conj mutations transform-commit)]
          (is (= candidate/mutation-annotations
                 (dissoc (:annotations tool) :title))))
        (is (= candidate/read-only-annotations
               (dissoc (:annotations preview) :title))))))
  (testing "titles can contain Clojure bang syntax without changing MCP names"
    (let [commit-tools (candidate/catalog-tools :R)
          bang-tools (candidate/catalog-tools :S)
          action-tools (candidate/catalog-tools :T)]
      (is (= "edit_clojure_commit!"
             (get-in commit-tools [1 :annotations :title])))
      (is (= "edit_clojure_bang!"
             (get-in bang-tools [1 :annotations :title])))
      (is (= "write_clojure_edits!"
             (get-in action-tools [1 :annotations :title])))
      (is (nil? (get-in (candidate/catalog-tools :P)
                        [1 :annotations :title])))
      (is (nil? (get-in (candidate/catalog-tools :Q)
                        [1 :annotations :title]))))))

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

(deftest post-call-response-projection-remains-an-explicit-route-gate
  (is (= :deferred
         (get-in (candidate/catalog-report :N)
                 [:route-gates :post-call-response-projection :status])))
  (is (re-find #"Legacy operation fields"
               (get-in (candidate/catalog-report :T)
                       [:route-gates :post-call-response-projection :reason]))))

(deftest isolated-start-projects-the-selected-catalog
  (let [observed (atom nil)
        written (atom nil)]
    (with-redefs [http-server/start
                  (fn [opts]
                    (reset! observed {:opts opts
                                      :tools (mcp-tool/all-tools)
                                      :instructions mcp-server/server-instructions})
                    :stopped)
                  spit
                  (fn [path contents]
                    (reset! written {:path path :contents contents}))]
      (is (= :stopped
             (candidate/start {:catalog :L
                               :catalog-receipt-file "/tmp/catalog-role-receipt.json"
                               :project-dir "/tmp/example"
                               :port 0})))
      (is (= "/tmp/example" (get-in @observed [:opts :project-dir])))
      (is (not (contains? (:opts @observed) :catalog)))
      (is (not (contains? (:opts @observed) :catalog-receipt-file)))
      (is (= ["inspect_clojure" "apply_clojure_plan" "edit_clojure"
              "transform_clojure"]
             (mapv :name (:tools @observed))))
      (is (= (candidate/catalog-instructions :L)
             (:instructions @observed)))
      (is (= "/tmp/catalog-role-receipt.json" (:path @written)))
      (is (= (json/parse-string
               (json/generate-string (candidate/catalog-runtime-receipt :L))
               true)
             (json/parse-string (:contents @written) true)))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"require the full tool profile"
                        (candidate/start {:catalog :A
                                          :tool-profile :edit}))))

(defn -main
  [& _]
  (let [{:keys [fail error]} (run-tests 'clj-surgeon.experiments.mcp-candidate-catalog-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
