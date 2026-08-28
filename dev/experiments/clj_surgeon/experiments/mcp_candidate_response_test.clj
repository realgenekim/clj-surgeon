(ns clj-surgeon.experiments.mcp-candidate-response-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-catalog :as candidate]
   [clj-surgeon.experiments.mcp-candidate-response :as response]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests testing]]))

(def legacy-names
  ["apply_clojure_changes" "edit_clojure" "transform_clojure"])

(defn- protocol-strings [{:keys [content structured]}]
  (concat content
          (keep structured [:operation :error :remedy :decision-rule])
          (keep #(when (string? %) %)
                (tree-seq coll? seq (:remedies structured)))
          [(get-in structured [:next_call :tool])]))

(defn- leaks [callback]
  (for [text (remove nil? (protocol-strings callback))
        legacy legacy-names
        :when (str/includes? text legacy)]
    legacy))

(defn- tool-by-name [tools tool-name]
  (some #(when (= tool-name (:name %)) %) tools))

(defn- contains-token? [text token]
  (boolean
    (re-find
      (re-pattern
        (str "(?<![A-Za-z0-9_.!_-])"
             (java.util.regex.Pattern/quote token)
             "(?![A-Za-z0-9_.!_-])"))
      text)))

(deftest exact-n-success-is-terminal-and-evidence-preserving
  (let [lexicon (candidate/catalog-lexicon :N)
        terminal "Done — extraction and exact verification completed."
        evidence {:source "(defn apply_clojure_changes [])"
                  :diff "- transform_clojure\n+ edit_clojure"
                  :diagnostics ["apply_clojure_changes"]
                  :argv ["tool" "transform_clojure"]
                  :replacement "(edit_clojure)"}
        structured (merge evidence
                          {:ok true
                           :operation "apply_clojure_changes"
                           :terminal_response terminal
                           :next_action "none"
                           :next_call {:extraction {:source "src/a.clj"}}})
        summary (str "apply_clojure_changes\n  15 edits\n\n"
                     "→ If this mutation completes all remaining work, return exactly: "
                     terminal)
        projected (response/project-callback lexicon :extract
                                             [summary] false structured)]
    (is (= "apply_clojure_changes" (get-in projected [:structured :operation])))
    (is (= "extract_clojure" (get-in projected [:structured :invoked_tool])))
    (is (str/starts-with? (first (:content projected)) "extract_clojure\n"))
    (is (= terminal (get-in projected [:structured :terminal_response])))
    (is (= 1 (count (re-seq (re-pattern
                              (java.util.regex.Pattern/quote terminal))
                            (first (:content projected))))))
    (is (= evidence (select-keys (:structured projected) (keys evidence))))
    (is (not (contains? (get-in projected [:structured :next_call]) :tool)))
    (is (empty? (leaks (update projected :structured dissoc
                               :source :diff :diagnostics :argv :replacement
                               :operation :invoked_tool))))))

(deftest exact-t-refuses-extraction-through-the-edit-entrance
  (let [observed (atom nil)
        kernel-calls (atom 0)
        edit-tool (tool-by-name (candidate/catalog-tools :T)
                                "write_clojure_edits")]
    (with-redefs [mcp-tool/handle-clj-change
                  (fn [& _]
                    (swap! kernel-calls inc))]
      ((:tool-fn edit-tool)
       nil
       {"workspace_root" "/tmp/work"
        "extraction" {"file" "src/a.clj"}}
       #(reset! observed {:content %1
                          :error? %2
                          :structured %3})))
    (is (= 0 @kernel-calls))
    (is (true? (:error? @observed)))
    (is (= :public-schema-denied
           (get-in @observed [:structured :error-type])))
    (is (= "write_clojure_edits"
           (get-in @observed [:structured :invoked_tool])))
    (is (nil? (get-in @observed [:structured :operation])))
    (is (false? (get-in @observed [:structured :mutation-attempted])))
    (is (false? (get-in @observed [:structured :write-authority])))))

(deftest exact-t-extraction-entrance-reaches-the-shared-kernel-once
  (let [observed (atom nil)
        kernel-calls (atom 0)
        extraction-name (:extract (candidate/catalog-lexicon :T))
        extraction-tool (tool-by-name (candidate/catalog-tools :T)
                                      extraction-name)]
    (with-redefs [mcp-tool/handle-clj-change
                  (fn [_ _ callback]
                    (swap! kernel-calls inc)
                    (callback ["apply_clojure_changes\n  success"]
                              false
                              {:ok true
                               :operation "apply_clojure_changes"}))]
      ((:tool-fn extraction-tool)
       nil
       {"workspace_root" "/tmp/work"
        "extraction" {"file" "src/a.clj"}}
       #(reset! observed {:content %1
                          :error? %2
                          :structured %3})))
    (is (= 1 @kernel-calls))
    (is (false? (:error? @observed)))
    (is (= "apply_clojure_changes"
           (get-in @observed [:structured :operation])))
    (is (= extraction-name
           (get-in @observed [:structured :invoked_tool])))))

(deftest semantic-operation-survives-public-response-projection
  (let [remedy "Correct the project root or request and call apply_clojure_changes once."
        structured {:ok false
                    :operation "apply_clojure_changes"
                    :error "apply_clojure_changes refused"
                    :remedy remedy
                    :source "apply_clojure_changes"
                    :diagnostics ["edit_clojure"]}
        observed (atom nil)
        extraction-name (:extract (candidate/catalog-lexicon :T))
        extraction-tool (tool-by-name (candidate/catalog-tools :T)
                                      extraction-name)]
    (with-redefs [mcp-tool/handle-clj-change
                  (fn [_ _ callback]
                    (callback
                      [(str "apply_clojure_changes\n  refused\n→ " remedy)]
                      true structured))]
      ((:tool-fn extraction-tool)
       nil {"extraction" {}}
       #(reset! observed {:content %1
                          :error? %2
                          :structured %3})))
    (is (true? (:error? @observed)))
    (is (= "apply_clojure_changes"
           (get-in @observed [:structured :operation])))
    (is (= extraction-name
           (get-in @observed [:structured :invoked_tool])))
    (is (= "apply_clojure_changes" (get-in @observed [:structured :source])))
    (is (= ["edit_clojure"] (get-in @observed [:structured :diagnostics])))))

(deftest every-catalog-projects-extract-success-and-refusal
  (doseq [catalog candidate/supported-catalogs]
    (testing (name catalog)
      (let [lexicon (candidate/catalog-lexicon catalog)
            expected (:extract lexicon)
            remedy "Correct the project root or request and call apply_clojure_changes once."
            success (response/project-callback
                      lexicon :extract ["apply_clojure_changes\n  success"] false
                      {:ok true :operation "apply_clojure_changes"})
            refusal (response/project-callback
                      lexicon :extract
                      [(str "apply_clojure_changes\n  refused\n→ " remedy)] true
                      {:ok false :operation "apply_clojure_changes"
                       :error "apply_clojure_changes refused"
                       :remedy remedy})
            unavailable (remove (set (vals lexicon)) legacy-names)]
        (doseq [projected [success refusal]]
          (is (= "apply_clojure_changes"
                 (get-in projected [:structured :operation])))
          (is (= expected (get-in projected [:structured :invoked_tool])))
          (is (str/starts-with? (first (:content projected)) expected))
          (is (not-any? (fn [legacy]
                          (some #(contains-token? % legacy)
                                (remove nil?
                                        (protocol-strings
                                          (update projected :structured
                                                  dissoc :operation)))))
                        unavailable)))))))

(deftest projection-preserves-arbitrary-prose-and-transform-evidence
  (let [lexicon (candidate/catalog-lexicon :T)
        arbitrary "User source says apply_clojure_changes refused"
        diff "- (transform_clojure old)\n+ (edit_clojure new)"
        result {:ok true :operation :transform! :error arbitrary :diff diff}
        projected (response/project-callback
                    lexicon :transform-commit
                    [(str "transform_clojure\n  transform!\n\n" diff)]
                    false result)]
    (is (str/starts-with? (first (:content projected))
                          "apply_clojure_transform\n"))
    (is (str/ends-with? (first (:content projected)) diff))
    (is (= :transform! (get-in projected [:structured :operation])))
    (is (= arbitrary (get-in projected [:structured :error])))
    (is (= diff (get-in projected [:structured :diff])))))

(deftest inspect-routing-and-canonical-registry-remain-exact
  (let [lexicon (candidate/catalog-lexicon :T)
        extraction {:operation "inspect_clojure"
                    :next_call {:extraction {:file "src/a.clj"}}}
        plan {:operation "inspect_clojure"
              :next_call {:basis "basis-1"}}
        extraction-content
        ["inspect_clojure · plan-extraction\n→ review visibility, fill caller decisions, then call apply_clojure_changes once"]
        plan-content
        ["inspect_clojure · prepare-change\n→ fill next_call decisions, then call apply_clojure_changes once"]
        canonical (mcp-server/public-tool-registry)]
    (is (str/includes?
          (first (:content (response/project-callback
                             lexicon :inspect extraction-content false extraction)))
          "move_clojure_owners once"))
    (is (str/includes?
          (first (:content (response/project-callback
                             lexicon :inspect plan-content false plan)))
          "apply_clojure_plan once"))
    (candidate/catalog-tools :T)
    (is (= canonical (mcp-server/public-tool-registry)))))

(deftest catalog-a-preserves-semantic-output-and-adds-invoked-identity
  (let [tool (tool-by-name (candidate/catalog-tools :A)
                           "apply_clojure_changes")
        cases [{:content ["apply_clojure_changes\n  success"]
                :error? false
                :structured {:ok true :operation "apply_clojure_changes"
                             :terminal_response "Done."}}
               {:content ["apply_clojure_changes\n  refused\n→ correct_request"]
                :error? true
                :structured {:ok false :operation "apply_clojure_changes"
                             :error "arbitrary apply_clojure_changes evidence"
                             :remedy "arbitrary apply_clojure_changes remedy"}}]]
    (doseq [canonical cases]
      (let [observed (atom nil)]
        (with-redefs [mcp-tool/handle-clj-change
                      (fn [_ _ callback]
                        (callback (:content canonical)
                                  (:error? canonical)
                                  (:structured canonical)))]
          ((:tool-fn tool) nil {"changes" [] "expect" {}}
                           #(reset! observed {:content %1 :error? %2 :structured %3})))
        (is (= (:content canonical) (:content @observed)))
        (is (= (:error? canonical) (:error? @observed)))
        (is (= (:structured canonical)
               (dissoc (:structured @observed) :invoked_tool)))
        (is (= "apply_clojure_changes"
               (get-in @observed [:structured :invoked_tool])))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (run-tests 'clj-surgeon.experiments.mcp-candidate-response-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
