(ns clj-surgeon.mcp-inspect-contract-test
  (:require
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def forms-request
  {"requests"
   [{"id" "selected"
     "operation" "forms"
     "file" "src/example.clj"
     "forms" ["alpha" "beta"]
     "expect" {"forms" 2}}]
   "expect" {"requests" 1 "files" 1}})

(defn- java-json-containers
  [value]
  (if (System/getProperty "babashka.version")
    value
    ((requiring-resolve 'clj-surgeon.java-json-containers/convert) value)))

(defn- snapshot
  [file source]
  {:file file
   :source source
   :hash (structural-lens/source-hash source)})

(deftest validates-all-four-discriminated-request-variants
  (let [request
        {"requests"
         [{"id" "forms" "operation" "forms" "file" "src/a.clj"
           "forms" ["a"] "expect" {"forms" 1}}
          {"id" "outline" "operation" "outline" "file" "src/a.clj"}
          {"id" "match" "operation" "match" "file" "src/b.cljs"
           "match" "(send! _)" "inside" "dispatch"
           "expect" {"matches" 0}}
          {"id" "xray" "operation" "xray" "file" "src/c.cljc"
           "expression" "(-> (form 'settings) initializer)"}]
         "expect" {"requests" 4 "files" 3}}
        result (inspect/validate-inspect-params request)]
    (is (:ok result))
    (is (= ["forms" "outline" "match" "xray"]
           (mapv :operation (get-in result [:params :requests]))))
    (is (= 0 (get-in result [:params :requests 2 :expect :matches])))
    (is (= {:requests 4 :files 3} (get-in result [:params :expect])))))

(deftest accepts-real-java-sdk-containers
  (is (= (inspect/validate-inspect-params forms-request)
         (inspect/validate-inspect-params
           (java-json-containers forms-request)))))

(deftest validates-metadata-only-form-projection
  (let [request (assoc-in forms-request
                          ["requests" 0 "include_source"] false)
        result (inspect/validate-inspect-params request)]
    (is (:ok result))
    (is (false? (get-in result [:params :requests 0 :include-source]))))
  (let [request (assoc-in forms-request
                          ["requests" 0 "include_source"] "false")
        result (inspect/validate-inspect-params request)]
    (is (false? (:ok result)))
    (is (= :boolean (:reason result)))
    (is (= ["requests" 0 "include_source"] (:path result)))))

(deftest validates-optional-string-symbol-outline-projection
  (let [request {"requests" [{"id" "outline" "operation" "outline"
                              "file" "src/example.clj"
                              "include_string_symbols" true}]
                 "expect" {"requests" 1 "files" 1}}
        result (inspect/validate-inspect-params request)]
    (is (:ok result))
    (is (true? (get-in result
                       [:params :requests 0 :include-string-symbols]))))
  (let [request {"requests" [{"id" "outline" "operation" "outline"
                              "file" "src/example.clj"
                              "include_string_symbols" "true"}]
                 "expect" {"requests" 1 "files" 1}}
        result (inspect/validate-inspect-params request)]
    (is (false? (:ok result)))
    (is (= :boolean (:reason result)))
    (is (= ["requests" 0 "include_string_symbols"] (:path result)))))

(deftest outline-string-symbols-are-gated-and-default-output-is-identical
  (let [source (str "(ns example)\n"
                    "(defn- page [] \"function onsetReady(e){}\")\n")
        base-request {"requests" [{"id" "outline" "operation" "outline"
                                   "file" "src/example.clj"}]
                      "expect" {"requests" 1 "files" 1}}
        evaluate (fn [raw]
                   (inspect/evaluate-snapshots
                     (get-in (inspect/validate-inspect-params raw) [:params])
                     {"src/example.clj"
                      (snapshot "src/example.clj" source)}))
        absent (evaluate base-request)
        explicit-off (evaluate
                       (assoc-in base-request
                                 ["requests" 0 "include_string_symbols"] false))
        requested (evaluate
                    (assoc-in base-request
                              ["requests" 0 "include_string_symbols"] true))]
    (is (= absent explicit-off)
        "absent and explicit false produce identical structured bytes")
    (is (= [{:name "onsetReady" :kind "function" :line 2 :owner "page"}]
           (get-in requested
                   [:results 0 :outline :forms 0 :string_symbols])))
    (is (str/includes? (inspect/concise-summary
                         (assoc requested :elapsed_ms 0))
                       "1 string symbol"))))

(deftest rejects-schema-and-cardinality-errors-with-exact-paths
  (doseq [[label request reason path]
          [[:top-not-map [] :expected-object []]
           [:unknown-top (assoc forms-request "extra" true)
            :unknown-fields []]
           [:snapshot-guards-not-map
            (assoc forms-request "snapshot_guards" [])
            :expected-object ["snapshot_guards"]]
           [:empty-snapshot-guards
            (assoc forms-request "snapshot_guards" {})
            :empty-snapshot-guards ["snapshot_guards"]]
           [:invalid-snapshot-hash
            (assoc forms-request "snapshot_guards"
                   {"src/example.clj" "ABC"})
            :invalid-snapshot-hash ["snapshot_guards" "src/example.clj"]]
           [:non-string-snapshot-hash
            (assoc forms-request "snapshot_guards"
                   {"src/example.clj"
                    1111111111111111111111111111111111111111111111111111111111111111N})
            :invalid-snapshot-hash ["snapshot_guards" "src/example.clj"]]
           [:missing-requested-snapshot-guard
            (assoc forms-request "snapshot_guards"
                   {"src/other.clj" (apply str (repeat 64 "a"))})
            :missing-snapshot-guards ["snapshot_guards"]]
           [:missing-top (dissoc forms-request "requests")
            :missing-fields []]
           [:empty-requests (assoc forms-request "requests" [])
            :non-empty-array ["requests"]]
           [:request-not-map (assoc forms-request "requests" [42])
            :expected-object ["requests" 0]]
           [:blank-id (assoc-in forms-request ["requests" 0 "id"] " ")
            :non-blank-string ["requests" 0 "id"]]
           [:duplicate-id
            (-> forms-request
                (assoc "requests" [(get-in forms-request ["requests" 0])
                                   (assoc (get-in forms-request ["requests" 0])
                                          "file" "src/b.clj")])
                (assoc "expect" {"requests" 2 "files" 2}))
            :duplicate-id ["requests" 1 "id"]]
           [:unknown-operation
            (assoc-in forms-request ["requests" 0 "operation"] "grep")
            :unknown-operation ["requests" 0 "operation"]]
           [:illegal-forms-field-on-outline
            (-> forms-request
                (assoc-in ["requests" 0 "operation"] "outline")
                (update-in ["requests" 0] dissoc "expect"))
            :unknown-fields ["requests" 0]]
           [:illegal-match-field-on-xray
            (-> forms-request
                (assoc "requests" [{"id" "x" "operation" "xray"
                                    "file" "src/example.clj"
                                    "expression" "(form 'a)"
                                    "match" "a"}]))
            :unknown-fields ["requests" 0]]
           [:empty-forms (assoc-in forms-request ["requests" 0 "forms"] [])
            :non-empty-array ["requests" 0 "forms"]]
           [:duplicate-form
            (assoc-in forms-request ["requests" 0 "forms"] ["alpha" "alpha"])
            :duplicate-form ["requests" 0 "forms" 1]]
           [:forms-expect-mismatch
            (assoc-in forms-request ["requests" 0 "expect" "forms"] 1)
            :request-expectation-mismatch ["requests" 0 "expect" "forms"]]
           [:aggregate-request-mismatch
            (assoc-in forms-request ["expect" "requests"] 2)
            :aggregate-expectation-mismatch ["expect" "requests"]]
           [:aggregate-file-mismatch
            (assoc-in forms-request ["expect" "files"] 2)
            :aggregate-expectation-mismatch ["expect" "files"]]
           [:match-expect-negative
            {"requests" [{"id" "m" "operation" "match"
                          "file" "src/a.clj" "match" "_"
                          "expect" {"matches" -1}}]
             "expect" {"requests" 1 "files" 1}}
            :non-negative-integer ["requests" 0 "expect" "matches"]]
           [:absolute-path
            (assoc-in forms-request ["requests" 0 "file"] "/tmp/a.clj")
            :invalid-relative-source-path ["requests" 0 "file"]]
           [:parent-path
            (assoc-in forms-request ["requests" 0 "file"] "../a.clj")
            :invalid-relative-source-path ["requests" 0 "file"]]
           [:unsupported-extension
            (assoc-in forms-request ["requests" 0 "file"] "src/a.txt")
            :invalid-relative-source-path ["requests" 0 "file"]]]]
    (testing (name label)
      (let [result (inspect/validate-inspect-params request)]
        (is (false? (:ok result)))
        (is (= "inspect_clojure" (:operation result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))))))

(deftest evaluates-ordered-exact-forms-with-source-fidelity
  (let [source (str "(ns example)\n\n"
                    "(defn alpha\n"
                    "  [x]\n"
                    "  ^{:doc \"λ\"} #(inc %, x))\n\n"
                    "(def beta\n"
                    "  ;; café\n"
                    "  [:a, :b])\n")
        params (get-in (inspect/validate-inspect-params forms-request)
                       [:params])
        result (inspect/evaluate-snapshots
                 params {"src/example.clj"
                         (snapshot "src/example.clj" source)})]
    (is (:ok result))
    (is (= ["alpha" "beta"]
           (mapv :name (get-in result [:results 0 :forms]))))
    (is (= (str "(defn alpha\n"
                "  [x]\n"
                "  ^{:doc \"λ\"} #(inc %, x))")
           (get-in result [:results 0 :forms 0 :source])))
    (is (= "(def beta\n  ;; café\n  [:a, :b])"
           (get-in result [:results 0 :forms 1 :source])))
    (is (= (structural-lens/source-hash source)
           (get-in result [:file_hashes "src/example.clj"])))
    (is (= 2 (get-in result [:results 0 :form_count])))
    (is (= "none" (:next_action result)))))

(deftest evaluates-outline-match-and-xray-in-request-order
  (let [source (str "(ns example)\n"
                    "(def settings {:alpha 1 :beta 2})\n"
                    "(defn dispatch []\n"
                    "  \"textual decoy: (send! _)\"\n"
                    "  (send! :real))\n")
        raw {"requests"
             [{"id" "outline" "operation" "outline"
               "file" "src/example.clj"}
              {"id" "match" "operation" "match"
               "file" "src/example.clj" "match" "(send! _)"
               "expect" {"matches" 1}}
              {"id" "xray" "operation" "xray"
               "file" "src/example.clj"
               "expression"
               "(-> (form 'settings) initializer (expect-count 1) (analyze (fn [[m]] (count m))))"}]
             "expect" {"requests" 3 "files" 1}}
        params (get-in (inspect/validate-inspect-params raw) [:params])
        result (inspect/evaluate-snapshots
                 params {"src/example.clj"
                         (snapshot "src/example.clj" source)})]
    (is (:ok result))
    (is (= ["outline" "match" "xray"] (mapv :id (:results result))))
    (is (= 2 (get-in result [:results 0 :outline :form_count])))
    (is (= 1 (get-in result [:results 1 :match_count])))
    (is (= "(send! :real)"
           (get-in result [:results 1 :matches 0 :source])))
    (is (= "dispatch" (get-in result [:results 1 :matches 0 :inside])))
    (is (= 2 (get-in result [:results 2 :value])))
    ;; @spec MCP-OP-STUDY-041
    ;; The text block carries the ROWS, not a description of them: O2 round 2
    ;; reversed the "source-free companion" rule for every mode, because a
    ;; text-only client that is handed a count has been handed nothing.
    (let [summary (inspect/concise-summary (assoc result :elapsed_ms 1.0))]
      (is (str/includes? summary "outline: 5 lines · 2 forms"))
      (is (str/includes? summary "· 2-2 def settings"))
      (is (str/includes? summary "· 3-5 defn dispatch []"))
      (is (str/includes? summary "match: 1 match"))
      (is (str/includes? summary "· dispatch@5-5"))
      (is (str/includes? summary "(send! :real)"))
      (is (str/includes? summary "· value 2")))))

(deftest refuses-the-complete-batch-on-form-or-match-failure
  (let [source "(ns example)\n(defn duplicate [] 1)\n(defn duplicate [] 2)\n"
        base-snapshot {"src/example.clj"
                       (snapshot "src/example.clj" source)}]
    (doseq [[label request expected]
            [[:ambiguous
              forms-request
              "batch-form-selection-failed"]
             [:missing
              (assoc-in forms-request ["requests" 0 "forms"]
                        ["missing" "beta"])
              "batch-form-selection-failed"]
             [:match-count
              {"requests" [{"id" "m" "operation" "match"
                            "file" "src/example.clj"
                            "match" "(defn _ _ _)"
                            "expect" {"matches" 1}}]
               "expect" {"requests" 1 "files" 1}}
              "inspect-cardinality-mismatch"]]]
      (testing (name label)
        (let [params (get-in (inspect/validate-inspect-params request) [:params])
              result (inspect/evaluate-snapshots params base-snapshot)]
          (is (false? (:ok result)))
          (is (= expected (:error_type result)))
          (is (nil? (:results result)))
          (when (= :missing label)
            (is (= 1 (:available_form_count result)))
            (is (= ["duplicate"] (:form_candidates result)))
            (is (not (contains? result :source)))
            (is (not (contains? result :results)))))))))

(deftest selector-local-failure-preserves-only-complete-sibling-requests
  ;; @spec MCP-OP-READ-CONT-001 MCP-OP-READ-CONT-002
  (let [source "(ns example)\n(def alpha 1)\n(def beta 2)\n"
        source-snapshot (snapshot "src/example.clj" source)
        requests [{:id "before" :operation "forms" :file "src/example.clj"
                   :forms ["beta"] :expect {:forms 1}}
                  {:id "mistyped" :operation "forms" :file "src/example.clj"
                   :forms ["answr"] :expect {:forms 1}}
                  {:id "later" :operation "outline" :file "src/example.clj"}]
        result (inspect/evaluate-snapshots
                 {:requests requests :expect {:requests 3 :files 1}}
                 {"src/example.clj" source-snapshot})
        continuation (:continuation result)]
    (is (false? (:ok result)))
    (is (false? (:read_complete result)))
    (is (not (contains? result :results)))
    (is (not (contains? result :next_call)))
    (is (= "selector" (:failed_stage result)))
    (is (= true (:snapshot_bound continuation)))
    (is (= false (:write_authority continuation)))
    (is (= 1 (:completed_request_count continuation)))
    (is (= ["before"] (:completed_request_ids continuation)))
    (is (= 2 (:pending_request_count continuation)))
    (is (= ["mistyped" "later"] (:pending_request_ids continuation)))
    (is (= false (:selector_authority continuation)))
    (is (= {"src/example.clj" (:hash source-snapshot)}
           (:snapshot_guards continuation)))
    (is (= ["before"] (mapv :id (:completed_results continuation))))
    (is (= "(def beta 2)"
           (get-in continuation [:completed_results 0 :forms 0 :source])))
    (is (= false (get-in continuation [:retry_template :executable])))
    (is (= false (get-in continuation
                         [:retry_template :selector_authority])))
    (is (= {:requests 2 :files 1}
           (get-in continuation [:retry_template :arguments :expect])))
    (is (= [nil]
           (get-in continuation
                   [:retry_template :arguments :requests 0 :forms])))
    (is (= "later"
           (get-in continuation
                   [:retry_template :arguments :requests 1 :id])))
    (is (= [{:path ["requests" 0 "forms" 0]
             :request_id "mistyped"
             :kind "exact-top-level-owner"
             :rejected_value "answr"
             :must_replace true
             :authority false}]
           (get-in continuation [:retry_template :holes])))))

(deftest non-selector-and-over-budget-failures-publish-no-continuation
  ;; @spec MCP-OP-READ-CONT-002
  (let [source "(ns example)\n(def alpha 1)\n(def beta 2)\n"
        source-snapshot (snapshot "src/example.clj" source)
        good {:id "before" :operation "forms" :file "src/example.clj"
              :forms ["beta"] :expect {:forms 1}}
        cardinality {:id "count" :operation "match" :file "src/example.clj"
                     :match "(def _ _)" :expect {:matches 1}}
        selector {:id "mistyped" :operation "forms" :file "src/example.clj"
                  :forms ["answr"] :expect {:forms 1}}
        evaluate (fn [requests limits]
                   (inspect/evaluate-snapshots
                     {:requests requests
                      :expect {:requests (count requests) :files 1}}
                     {"src/example.clj" source-snapshot}
                     limits))
        non-selector (evaluate [good cardinality]
                               inspect/default-output-limits)
        over-budget (evaluate [good selector]
                              (assoc inspect/default-output-limits
                                     :per-request-source 1))]
    (is (= "inspect-cardinality-mismatch" (:error_type non-selector)))
    (is (not (contains? non-selector :continuation)))
    (is (= "inspect-output-limit" (:error_type over-budget)))
    (is (not (contains? over-budget :continuation)))))

(deftest selector-refusal-reports-every-failed-owner-without-choosing
  ;; @spec MCP-OP-READ-DIAG-001 MCP-OP-READ-DIAG-003 MCP-OP-READ-HYP-001
  (let [source (str "(ns example)\n"
                    "(def alpha 1)\n"
                    "(def beta 2)\n"
                    "(def beta 3)\n")
        requests [{:id "owners" :operation "forms" :file "src/example.clj"
                   :forms ["betta" "beta"] :expect {:forms 2}}]
        result (inspect/evaluate-snapshots
                 {:requests requests :expect {:requests 1 :files 1}}
                 {"src/example.clj" (snapshot "src/example.clj" source)})]
    (is (false? (:ok result)))
    (is (false? (:read_complete result)))
    (is (= "selector" (:failed_stage result)))
    (is (string? (:file_hash result)))
    (is (= {:id "owners" :operation "forms" :file "src/example.clj"
            :requested_forms ["betta" "beta"]}
           (:failed_request result)))
    (is (= 2 (:failure_count result)))
    (is (= 2 (:available_owner_count result)))
    (is (= ["alpha" "beta"] (:available_owners result)))
    (is (= 2 (:available_owners_returned result)))
    (is (= 0 (:available_owners_omitted result)))
    (is (false? (:available_owners_truncated result)))
    (is (= [{:form "betta" :error_type "form-not-found" :match_count 0}
            {:form "beta" :error_type "ambiguous-form" :match_count 2
             :candidate_limit 10 :matches_truncated? false
             :matches [{:type "def" :name "beta" :line 3 :end_line 3
                        :platforms ["clj"]}
                       {:type "def" :name "beta" :line 4 :end_line 4
                        :platforms ["clj"]}]}]
           (:failures result)))
    (is (= "betta" (get-in result [:selection_failures 0 :requested_owner])))
    (is (= "beta" (get-in result [:selection_failures 0 :hypotheses 0 :owner])))
    (is (= 1 (get-in result [:selection_failures 0 :hypotheses 0 :rank])))
    (is (= "normalized-levenshtein"
           (get-in result [:selection_failures 0 :hypotheses 0 :ranking_basis])))
    (is (false? (get-in result [:selection_failures 0 :hypotheses 0 :authority])))
    (is (= [] (get-in result [:selection_failures 1 :hypotheses])))
    (is (not (contains? result :results)))
    (is (not (contains? result :next_call)))
    (is (not (contains? result :selected_form)))))

(deftest selector-hypotheses-disclose-the-bounded-candidate-window
  ;; @spec MCP-OP-READ-HYP-002
  (let [source (str "(ns example)\n"
                    (apply str (map #(format "(def owner-%02d %d)\n" % %)
                                    (range 10))))
        requests [{:id "missing" :operation "forms" :file "src/example.clj"
                   :forms ["owner-xx"] :expect {:forms 1}}]
        result (inspect/evaluate-snapshots
                 {:requests requests :expect {:requests 1 :files 1}}
                 {"src/example.clj" (snapshot "src/example.clj" source)})]
    (is (= 1 (:failure_count result)))
    (is (= 10 (:available_form_count result)))
    (is (= 10 (:candidate_limit result)))
    (is (= 10 (count (:form_candidates result))))
    (is (false? (:candidates_truncated result)))
    (is (= 10 (get-in result [:selection_failures 0 :hypotheses_returned])))
    (is (= 0 (get-in result [:selection_failures 0 :hypotheses_omitted])))
    (is (false? (get-in result [:selection_failures 0 :hypotheses_truncated])))))

(deftest selector-owner-vocabulary-truncates-with-exact-counts
  ;; @spec MCP-OP-READ-DIAG-003
  (let [source (apply str
                      (map #(format "(def owner-%04d-with-a-deliberately-long-name %d)\n" % %)
                           (range 900)))
        requests [{:id "missing" :operation "forms" :file "src/example.clj"
                   :forms ["not-real"] :expect {:forms 1}}]
        result (inspect/evaluate-snapshots
                 {:requests requests :expect {:requests 1 :files 1}}
                 {"src/example.clj" (snapshot "src/example.clj" source)})]
    (is (= 900 (:available_owner_count result)))
    (is (< (:available_owners_returned result) 900))
    (is (= 900 (+ (:available_owners_returned result)
                  (:available_owners_omitted result))))
    (is (:available_owners_truncated result))
    (is (= (:available_owners_returned result)
           (count (:available_owners result))))))

(deftest output-budget-boundaries-are-inclusive-and-fail-closed
  ;; @spec MCP-OP-STUDY-020
  ;; The source bound is charged against the source the result RETURNS, so the
  ;; boundary fixture carries real returned source rather than a declared
  ;; `source_character_count` (which reports what was READ and is not a budget).
  (doseq [[label size limit ok?]
          [[:below 9 10 true]
           [:equal 10 10 true]
           [:above 11 10 false]]]
    (testing (name label)
      (let [result (inspect/enforce-output-budget
                     [{:id "x" :operation "forms"
                       :source_character_count size
                       :forms [{:source (apply str (repeat size "x"))}]}]
                     {:per-request-source limit
                      :per-request-result 1000
                      :aggregate-result 1000})]
        (is (= ok? (:ok result)))
        (when-not ok?
          (is (= "inspect-output-limit" (:error_type result)))))))
  (let [one {:id "a" :operation "outline" :source_character_count 0}
        per-request-size (inspect/json-character-count one)
        encoded-size (inspect/json-character-count [one])]
    (doseq [[label limit ok?]
            [[:per-request-below (inc per-request-size) true]
             [:per-request-equal per-request-size true]
             [:per-request-above (dec per-request-size) false]]]
      (testing (name label)
        (let [result (inspect/enforce-output-budget
                       [one] {:per-request-source 1000
                              :per-request-result limit
                              :aggregate-result 1000})]
          (is (= ok? (:ok result)))
          (when-not ok?
            (is (= "inspect-output-limit" (:error_type result)))))))
    (is (:ok (inspect/enforce-output-budget
               [one] {:per-request-source 1000
                      :per-request-result 1000
                      :aggregate-result encoded-size})))
    (is (= "inspect-output-limit"
           (:error_type
             (inspect/enforce-output-budget
               [one] {:per-request-source 1000
                      :per-request-result 1000
                      :aggregate-result (dec encoded-size)}))))))

(deftest summaries-are-stable-and-concise-for-a-result-carrying-no-rows
  ;; @spec MCP-OP-STUDY-041
  ;; Renamed by O2 round 2: "source-free" was the old contract. A result that
  ;; carries no rows still renders exactly this envelope and nothing else.
  (let [result {:ok true :operation "inspect_clojure"
                :request_count 2 :file_count 1
                :results [{:operation "forms" :form_count 3}
                          {:operation "match" :match_count 2}]
                :source_character_count 1234 :elapsed_ms 12.5}
        summary (inspect/concise-summary result)]
    (is (= (str "inspect_clojure\n"
                "  2 requests · 1 file · 3 forms · 2 matches\n\n"
                "✓ all requests resolved\n"
                "✓ ordered snapshot\n"
                "✓ hashes attached\n"
                "✓ terminal evidence · read_complete=true · next action none\n"
                "  1,234 source characters · 12.50 ms")
           summary))
    (is (not (.contains summary "(defn")))))
