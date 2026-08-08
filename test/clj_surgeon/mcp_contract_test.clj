(ns clj-surgeon.mcp-contract-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clojure.test :refer [deftest is testing]]))

(def valid-request
  {"changes"
   [{"id" "body-class"
     "files" ["src/bench/app_shell.clj"]
     "forms" ["ide-shell"]
     "find" ":body"
     "replace" ":body.ide-shell-page"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
   "expect" {"changes" 1 "edits" 1 "files" 1}})

(defn- java-json-containers
  [value]
  (cond
    (map? value)
    ;; Babashka exposes HashMap but not LinkedHashMap to SCI. The live JVM HTTP
    ;; test exercises the MCP SDK's actual LinkedHashMap boundary.
    (let [result (java.util.HashMap.)]
      (doseq [[key child] value]
        (.put result key (java-json-containers child)))
      result)

    (vector? value)
    (java.util.ArrayList. (map java-json-containers value))

    :else value))

(deftest validates-and-compiles-one-typed-request
  (let [validated (contract/validate-tool-params valid-request)]
    (is (:ok validated))
    (is (= {:changes
            [{:id :body-class
              :in ["src/bench/app_shell.clj"]
              :forms ['ide-shell]
              :find ":body"
              :do [:replace ":body.ide-shell-page"]
              :expect {:matches 1 :each-form 1 :each-file 1}}]
            :expect {:changes 1 :edits 1 :files 1}}
           (contract/tool-params->transaction (:params validated))))))

(deftest accepts-java-json-containers-from-the-mcp-sdk
  (let [clojure-result (contract/validate-tool-params valid-request)
        java-result (contract/validate-tool-params
                      (java-json-containers valid-request))]
    (is (:ok java-result))
    (is (= clojure-result java-result))))

(deftest preserves-source-spelling-and-owner-punctuation
  (let [request
        {"changes"
         [{"id" "literal-source"
           "files" ["src/example.cljc"]
           "forms" ["render!" "api/->wire"]
           "find" "^:private #(views/static %)"
           "replace" "^:private #(views/static\n  ;; café λ\n  %)"
           "expect" {"matches" 2 "each_form" 1}}]
         "expect" {"changes" 1 "edits" 2 "files" 1}}
        result (contract/validate-tool-params request)
        transaction (contract/tool-params->transaction (:params result))]
    (is (:ok result))
    (is (= ['render! 'api/->wire]
           (get-in transaction [:changes 0 :forms])))
    (is (= "^:private #(views/static %)"
           (get-in transaction [:changes 0 :find])))
    (is (= [:replace "^:private #(views/static\n  ;; café λ\n  %)"]
           (get-in transaction [:changes 0 :do])))))

(deftest validates-six-change-multi-file-request
  (let [changes
        [{"id" "one" "files" ["src/a.clj"] "forms" ["a"]
          "find" ":old-a" "replace" ":new-a" "expect" {"matches" 1}}
         {"id" "two" "files" ["src/a.clj" "src/b.cljs"] "forms" ["a" "b"]
          "find" ":old-b" "replace" ":new-b"
          "expect" {"matches" 2 "each_form" 1}}
         {"id" "three" "files" ["src/a.clj"] "forms" ["a"]
          "find" ":old-c" "replace" ":new-c" "expect" {"matches" 1}}
         {"id" "four" "files" ["src/b.cljs"] "forms" ["b"]
          "find" ":old-d" "replace" ":new-d" "expect" {"matches" 1}}
         {"id" "five" "files" ["src/b.cljs"] "forms" ["b"]
          "find" ":old-e" "replace" ":new-e" "expect" {"matches" 1}}
         {"id" "six" "files" ["src/b.cljs"] "forms" ["b"]
          "find" ":old-f" "replace" ":new-f" "expect" {"matches" 1}}]
        result (contract/validate-tool-params
                 {"changes" changes
                  "expect" {"changes" 6 "edits" 7 "files" 2}})]
    (is (:ok result))
    (is (= 6 (count (get-in result [:params :changes]))))))

(deftest rejects-invalid-typed-requests-exhaustively
  (doseq [[label request reason path]
          [[:top-not-map [] :expected-object []]
           [:unknown-top (assoc valid-request "surprise" true)
            :unknown-fields []]
           [:missing-changes (dissoc valid-request "changes")
            :missing-fields []]
           [:empty-changes (assoc valid-request "changes" [])
            :non-empty-array ["changes"]]
           [:change-not-map (assoc valid-request "changes" [42])
            :expected-object ["changes" 0]]
           [:unknown-change (assoc-in valid-request ["changes" 0 "surprise"] true)
            :unknown-fields ["changes" 0]]
           [:missing-id (update-in valid-request ["changes" 0] dissoc "id")
            :missing-fields ["changes" 0]]
           [:blank-id (assoc-in valid-request ["changes" 0 "id"] "  ")
            :non-blank-string ["changes" 0 "id"]]
           [:duplicate-id
            (assoc valid-request "changes"
                   [(get-in valid-request ["changes" 0])
                    (get-in valid-request ["changes" 0])])
            :duplicate-id ["changes" 1 "id"]]
           [:files-not-vector (assoc-in valid-request ["changes" 0 "files"] "src/a.clj")
            :non-empty-array ["changes" 0 "files"]]
           [:empty-files (assoc-in valid-request ["changes" 0 "files"] [])
            :non-empty-array ["changes" 0 "files"]]
           [:blank-file (assoc-in valid-request ["changes" 0 "files"] [""])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:absolute-file (assoc-in valid-request ["changes" 0 "files"] ["/tmp/a.clj"])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:parent-file (assoc-in valid-request ["changes" 0 "files"] ["../a.clj"])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:normalized-escape (assoc-in valid-request ["changes" 0 "files"] ["src/../../a.clj"])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:windows-parent (assoc-in valid-request ["changes" 0 "files"] ["..\\a.clj"])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:wrong-extension (assoc-in valid-request ["changes" 0 "files"] ["src/a.txt"])
            :invalid-relative-source-path ["changes" 0 "files" 0]]
           [:empty-forms (assoc-in valid-request ["changes" 0 "forms"] [])
            :non-empty-array ["changes" 0 "forms"]]
           [:blank-form (assoc-in valid-request ["changes" 0 "forms"] [" "])
            :non-blank-string ["changes" 0 "forms" 0]]
           [:blank-find (assoc-in valid-request ["changes" 0 "find"] "\n")
            :non-blank-string ["changes" 0 "find"]]
           [:blank-replace (assoc-in valid-request ["changes" 0 "replace"] "")
            :non-blank-string ["changes" 0 "replace"]]
           [:expect-not-map (assoc-in valid-request ["changes" 0 "expect"] [])
            :expected-object ["changes" 0 "expect"]]
           [:unknown-change-expect
            (assoc-in valid-request ["changes" 0 "expect" "every_form"] 1)
            :unknown-fields ["changes" 0 "expect"]]
           [:missing-matches
            (update-in valid-request ["changes" 0 "expect"] dissoc "matches")
            :missing-fields ["changes" 0 "expect"]]
           [:zero-matches (assoc-in valid-request ["changes" 0 "expect" "matches"] 0)
            :positive-integer ["changes" 0 "expect" "matches"]]
           [:negative-each-form (assoc-in valid-request ["changes" 0 "expect" "each_form"] -1)
            :positive-integer ["changes" 0 "expect" "each_form"]]
           [:boolean-each-file (assoc-in valid-request ["changes" 0 "expect" "each_file"] true)
            :positive-integer ["changes" 0 "expect" "each_file"]]
           [:top-expect-not-map (assoc valid-request "expect" [])
            :expected-object ["expect"]]
           [:unknown-top-expect (assoc-in valid-request ["expect" "matches"] 1)
            :unknown-fields ["expect"]]
           [:missing-top-count (update valid-request "expect" dissoc "files")
            :missing-fields ["expect"]]
           [:non-integer-top-count (assoc-in valid-request ["expect" "edits"] 1.5)
            :positive-integer ["expect" "edits"]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params request)]
        (is (false? (:ok result)))
        (is (= :invalid-mcp-request (:error-type result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))
        (is (string? (:remedy result)))))))

(deftest normalizes-complete-success-to-terminal-evidence
  (let [root "/work/project"
        result
        {:ok true
         :operation :change!
         :committed true
         :change-count 6
         :match-count 6
         :changed-file-count 2
         :receipt-file "/work/project/.receipts/undo.edn"
         :receipt-hash "receipt-hash"
         :verified
         {:whole-files true
          :file-count 2
          :read-back-hashes
          {"/work/project/src/a.clj" "a-hash"
           "/work/project/src/b.clj" "b-hash"}}}]
    (is (= {:ok true
            :operation "apply_clojure_changes"
            :committed true
            :changes 6
            :edits 6
            :files 2
            :verification_complete true
            :read_back_hashes {"src/a.clj" "a-hash"
                               "src/b.clj" "b-hash"}
            :undo_receipt "/work/project/.receipts/undo.edn"
            :receipt_hash "receipt-hash"
            :next_action "none"}
           (contract/normalize-success-receipt root result)))))

(deftest refuses-incomplete-or-unsafe-success-results
  (doseq [[label result reason]
          [[:kernel-error {:error "no" :error-type :scope-mismatch}
            "scope-mismatch"]
           [:not-committed {:ok true :committed false :verified {}}
            :incomplete-verification]
           [:no-whole-file-proof {:ok true :committed true
                                  :receipt-file "/work/project/r.edn"
                                  :verified {:read-back-hashes {}}}
            :incomplete-verification]
           [:no-receipt {:ok true :committed true
                         :verified {:whole-files true
                                    :read-back-hashes {}}}
            :incomplete-verification]
           [:outside-hash {:ok true :committed true
                           :receipt-file "/work/project/r.edn"
                           :verified {:whole-files true
                                      :read-back-hashes
                                      {"/outside/a.clj" "hash"}}}
            :path-outside-project]]]
    (testing (name label)
      (let [normalized (contract/classify-kernel-result "/work/project" result)]
        (is (false? (:ok normalized)))
        (is (= reason (:reason normalized)))))))

(deftest classifies-kernel-refusal-with-the-exact-actionable-diagnostic
  (let [refusal {:error ":find must contain exactly one complete form with no detached comments"
                 :error-type :invalid-intent-form
                 :reason :invalid-intent-form
                 :field ":find"
                 :form-count 0
                 :intent-index 2
                 :change-id :gallery-renderer}
        result (contract/classify-kernel-result "/work/project" refusal)
        custom-result
        (contract/classify-kernel-result
          "/work/project"
          (assoc refusal :remedy "Pass one complete parseable Clojure form in :find."))]
    (is (= false (:ok result)))
    (is (= "invalid-intent-form" (:error_type result)))
    (is (= "invalid-intent-form" (:reason result)))
    (is (= "kernel" (:phase result)))
    (is (= 2 (:change_index result)))
    (is (= "gallery-renderer" (:change_id result)))
    (is (= ":find" (:field result)))
    (is (= 0 (:form_count result)))
    (is (:source_unchanged result))
    (is (= "Pass exactly one complete parseable Clojure form in :find for change 2 (gallery-renderer)."
           (:remedy result)))
    (is (= "Pass one complete parseable Clojure form in :find."
           (:remedy custom-result)))))

(deftest normalized-refusal-preserves-an-actionable-compiler-diagnostic
  (let [result (contract/normalize-refusal
                 {:ok false
                  :error-type :invalid-mcp-request
                  :reason :unknown-fields
                  :path ["changes" 3]
                  :unknown ["owner"]
                  :allowed ["expect" "files" "find" "forms" "id" "replace"]
                  :error "Request contains unknown fields"})]
    (is (= "unknown-fields" (:reason result)))
    (is (= ["changes" 3] (:path result)))
    (is (= ["owner"] (:unknown result)))
    (is (= ["expect" "files" "find" "forms" "id" "replace"]
           (:allowed result)))
    (is (:source_unchanged result))))

(deftest namespace-owner-crosses-the-json-boundary-as-closed-data
  (let [change (-> (get valid-request "changes") first
                   (dissoc "forms")
                   (assoc "owner" {"kind" "namespace"
                                   "name" "bench.app-shell"}))
        request (assoc valid-request "changes" [change])
        validated (contract/validate-tool-params request)
        transaction (contract/tool-params->transaction (:params validated))]
    (is (:ok validated))
    (is (= {:kind :namespace :name 'bench.app-shell}
           (get-in transaction [:changes 0 :owner])))
    (doseq [[label owner expected]
            [["unsupported kind" {"kind" "file" "name" "bench.app-shell"}
              "invalid-owner-kind"]
             ["blank name" {"kind" "namespace" "name" ""}
              "non-blank-string"]
             ["unknown field" {"kind" "namespace" "name" "bench.app-shell"
                               "index" 0}
              "unknown-fields"]]]
      (testing label
        (let [result (contract/validate-tool-params
                       (assoc valid-request "changes"
                              [(assoc change "owner" owner)]))]
          (is (false? (:ok result)))
          (is (= expected (some-> (:reason result) name))))))
    (let [result (contract/validate-tool-params
                   (assoc valid-request "changes"
                          [(assoc change "forms" ["ide-shell"])]))]
      (is (false? (:ok result)))
      (is (= "ambiguous-change-owner" (some-> (:reason result) name))))))

(deftest validates-and-compiles-guarded-sibling-insertion
  (doseq [[field operator]
          [["insert_before" :insert-left]
           ["insert_after" :insert-right]]]
    (testing field
      (let [request
            {"changes"
             [(-> (get-in valid-request ["changes" 0])
                  (dissoc "replace")
                  (assoc field [":mcp-calls" ":mcp-successes"]))]
             "expect" {"changes" 1 "edits" 1 "files" 1}}
            validated (contract/validate-tool-params request)]
        (is (:ok validated))
        (is (= [operator [":mcp-calls" ":mcp-successes"]]
               (get-in (contract/tool-params->transaction (:params validated))
                       [:changes 0 :do]))))))
  (doseq [[label request reason path]
          [[:mixed-actions
            (assoc-in valid-request ["changes" 0 "insert_after"] [":extra"])
            :ambiguous-change-action ["changes" 0]]
           [:missing-action
            (update-in valid-request ["changes" 0] dissoc "replace")
            :ambiguous-change-action ["changes" 0]]
           [:empty-insert
            (-> valid-request
                (update-in ["changes" 0] dissoc "replace")
                (assoc-in ["changes" 0 "insert_before"] []))
            :non-empty-array ["changes" 0 "insert_before"]]
           [:blank-insert
            (-> valid-request
                (update-in ["changes" 0] dissoc "replace")
                (assoc-in ["changes" 0 "insert_after"] [" "]))
            :non-blank-string ["changes" 0 "insert_after" 0]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params request)]
        (is (false? (:ok result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))))))

(deftest validates-and-compiles-binding-aware-local-rename
  (let [request
        {"changes"
         [{"id" "rename-sort-binding"
           "files" ["src/demo.clj"]
           "forms" ["feed" "table"]
           "rename_binding"
           {"from" "sort-by"
            "to" "sort-field"
            "preserve_external_key" true}
           "expect" {"matches" 5 "each_form" 1}}]
         "expect" {"changes" 1 "edits" 5 "files" 1}}
        validated (contract/validate-tool-params request)]
    (is (:ok validated))
    (is (= {:changes
            [{:id :rename-sort-binding
              :in ["src/demo.clj"]
              :forms ['feed 'table]
              :do [:rename-binding
                   {:from 'sort-by
                    :to 'sort-field
                    :preserve-external-key true}]
              :expect {:matches 5 :each-form 1}}]
            :expect {:changes 1 :edits 5 :files 1}}
           (contract/tool-params->transaction (:params validated)))))
  (doseq [[label request reason path]
          [[:missing-forms
            {"changes"
             [{"id" "rename" "files" ["src/demo.clj"]
               "owner" {"kind" "namespace" "name" "demo"}
               "rename_binding"
               {"from" "sort-by" "to" "sort-field"
                "preserve_external_key" true}
               "expect" {"matches" 2}}]
             "expect" {"changes" 1 "edits" 2 "files" 1}}
            :invalid-binding-rename-owner ["changes" 0]]
           [:unsafe-key-change
            {"changes"
             [{"id" "rename" "files" ["src/demo.clj"] "forms" ["feed"]
               "rename_binding"
               {"from" "sort-by" "to" "sort-field"
                "preserve_external_key" false}
               "expect" {"matches" 2}}]
             "expect" {"changes" 1 "edits" 2 "files" 1}}
            :unsafe-binding-rename
            ["changes" 0 "rename_binding" "preserve_external_key"]]
           [:mixed-find
            {"changes"
             [{"id" "rename" "files" ["src/demo.clj"] "forms" ["feed"]
               "find" "sort-by"
               "rename_binding"
               {"from" "sort-by" "to" "sort-field"
                "preserve_external_key" true}
               "expect" {"matches" 2}}]
             "expect" {"changes" 1 "edits" 2 "files" 1}}
            :unexpected-binding-rename-find ["changes" 0]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params request)]
        (is (false? (:ok result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))))))

(deftest validates-and-compiles-comment-preserving-map-entry-insertion
  (let [request
        {"changes"
         [{"id" "add-status"
           "files" ["test/demo_test.clj"]
           "forms" ["contract-test"]
           "find" "{:a 1 :b 2}"
           "inside" "(is (= {:a 1 :b 2} actual))"
           "assoc_entry" {"key" ":status" "value" ":ready"}
           "expect" {"matches" 2}}]
         "expect" {"changes" 1 "edits" 2 "files" 1}}
        validated (contract/validate-tool-params request)]
    (is (:ok validated))
    (is (= [:assoc-entry {:key ":status" :value ":ready"}]
           (get-in (contract/tool-params->transaction (:params validated))
                   [:changes 0 :do])))
    (is (= "(is (= {:a 1 :b 2} actual))"
           (get-in (contract/tool-params->transaction (:params validated))
                   [:changes 0 :inside])))))
