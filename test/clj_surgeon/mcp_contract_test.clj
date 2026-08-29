(ns clj-surgeon.mcp-contract-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clojure.string :as str]
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

(def gesture-request
  {"edits"
   [{"file" "src/bench/pair_view.clj"
     "within" {"form" "route-event"}
     "from" ":done"
     "to" ":complete"}]
   "verify" "fast"})

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

(deftest validates-and-compiles-one-editor-gesture
  (let [validated (contract/validate-tool-params gesture-request)]
    (is (:ok validated))
    (is (= "fast" (get-in validated [:params :verify])))
    (is (= {:changes
            [{:id :edit-1
              :in ["src/bench/pair_view.clj"]
              :forms ['route-event]
              :find ":done"
              :do [:replace ":complete"]
              :expect {:matches 1 :each-form 1 :each-file 1}}]
            :expect {:changes 1 :edits 1 :files 1}}
           (contract/tool-params->transaction (:params validated))))))

(deftest editor-gesture-normalizes-injective-value-field-pairs
  ;; @spec MCP-OP-EDIT-017
  ;; @spec MCP-OP-EDIT-019
  (doseq [[source-field target-field relation]
          [["old" "new" "old-new"]
           ["before" "after" "before-after"]]]
    (testing relation
      (let [edit (-> (get-in gesture-request ["edits" 0])
                     (dissoc "from" "to")
                     (assoc source-field ":done"
                            target-field ":complete"))
            result (contract/validate-tool-params
                     (assoc-in gesture-request ["edits" 0] edit))]
        (is (:ok result))
        (is (= ":done" (get-in result [:params :changes 0 "find"])))
        (is (= ":complete" (get-in result [:params :changes 0 "replace"])))
        (is (= [{:edit_index 0
                 :relation relation
                 :requested_fields [source-field target-field]
                 :emitted_fields ["from" "to"]}]
               (:compact-field-normalization result)))))))

(deftest editor-gesture-refuses-ambiguous-value-field-pairs
  ;; @spec MCP-OP-EDIT-018
  ;; @spec MCP-OP-EDIT-019
  (doseq [[label edit supplied]
          [[:partial-old
            (-> (get-in gesture-request ["edits" 0])
                (dissoc "from" "to")
                (assoc "old" ":done"))
            ["old"]]
           [:cross-pair
            (-> (get-in gesture-request ["edits" 0])
                (dissoc "from" "to")
                (assoc "old" ":done" "after" ":complete"))
            ["after" "old"]]
           [:canonical-plus-alias
            (assoc (get-in gesture-request ["edits" 0])
                   "old" ":done" "new" ":complete")
            ["from" "new" "old" "to"]]
           [:two-alias-pairs
            (-> (get-in gesture-request ["edits" 0])
                (dissoc "from" "to")
                (assoc "old" ":done" "new" ":complete"
                       "before" ":done" "after" ":complete"))
            ["after" "before" "new" "old"]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params
                     (assoc-in gesture-request ["edits" 0] edit))]
        (is (false? (:ok result)))
        (is (= :invalid-editor-field-pair (:reason result)))
        (is (= ["edits" 0] (:path result)))
        (is (= supplied (:supplied-fields result)))
        (is (:source-unchanged result))
        (is (false? (:mutation-attempted result)))
        (is (false? (:write-authority result)))
        (is (re-find #"old/new.*before/after.*from/to" (:remedy result)))
        (is (re-find #"edit_clojure" (:remedy result)))
        (is (not (re-find #"apply_clojure_changes" (:remedy result))))))))

(deftest editor-gesture-compiles-a-namespace-location
  (let [request (assoc-in gesture-request ["edits" 0 "within"]
                          {"namespace" "sample.server"})
        validated (contract/validate-tool-params request)]
    (is (:ok validated))
    (is (= {:kind :namespace :name 'sample.server}
           (get-in validated [:params :changes 0 :owner])))
    (is (= {:kind :namespace :name 'sample.server}
           (get-in (contract/tool-params->transaction (:params validated))
                   [:changes 0 :owner])))))

(deftest editor-gesture-infers-the-unique-namespace-location
  ;; @spec MCP-OP-EDIT-005
  (let [request (assoc-in gesture-request ["edits" 0 "within"]
                          {"namespace" true})
        validated (contract/validate-tool-params request)]
    (is (:ok validated) (pr-str validated))
    (is (= {:kind :namespace}
           (get-in validated [:params :changes 0 :owner])))
    (is (= {:kind :namespace}
           (get-in (contract/tool-params->transaction (:params validated))
                   [:changes 0 :owner])))))

(deftest editor-gesture-tolerates-redundant-aggregate-expect
  (let [validated
        (contract/validate-tool-params
          (assoc gesture-request "expect"
                 {"changes" 0 "edits" 1 "files" 1}))]
    (is (:ok validated) (pr-str validated))
    (is (= {:ignored ["expect"]
            :reason "editor counts are derived"}
           (:input-normalization validated)))
    (is (= {:changes 1 :edits 1 :files 1}
           (get-in validated [:params :expect])))))

(deftest editor-gesture-derives-aggregate-counts
  (let [request
        {"edits"
         [{"file" "src/a.clj" "within" {"form" "a"}
           "from" ":old-a" "to" ":new-a" "matches" 2}
          {"file" "src/a.clj" "within" {"form" "b"}
           "from" ":old-b" "to" ":new-b"}
          {"file" "src/b.clj" "within" {"form" "c"}
           "from" ":old-c" "to" ":new-c"}]}
        validated (contract/validate-tool-params request)]
    (is (:ok validated))
    (is (= {:changes 3 :edits 4 :files 2}
           (get-in validated [:params :expect])))
    (is (= {:matches 2 :each-form 2 :each-file 2}
           (get-in validated [:params :changes 0 :expect])))
    (is (= ["edit-1" "edit-2" "edit-3"]
           (mapv :id (get-in validated [:params :changes]))))))

(deftest grouped-root-edn-edit-derives-per-file-guards
  ;; @spec MCP-OP-EDIT-001
  ;; @spec MCP-OP-EDIT-002
  (let [files ["bench/a.edn" "bench/b.edn"]
        request
        {"edits"
         [{"files" files
           "within" {"root" true}
           "from" "{:exact-bytes true :parse-clojure true}"
           "to" "{:exact-bytes-secondary true :meaning-preserved true :parse-clojure true}"}]}
        validated (contract/validate-tool-params request)
        change (get-in validated [:params :changes 0])]
    (is (:ok validated) (pr-str validated))
    (is (= files (:files change)))
    (is (= {:matches 2 :each-file 1} (:expect change)))
    (is (= {:changes 1 :edits 2 :files 2}
           (get-in validated [:params :expect])))))

(deftest grouped-root-edn-edit-refuses-ambiguous-scope
  ;; @spec MCP-OP-EDIT-002
  ;; @spec MCP-OP-EDIT-003
  (let [root-edit {"files" ["bench/a.edn" "bench/b.edn"]
                   "within" {"root" true}
                   "from" ":old"
                   "to" ":new"}]
    (doseq [[label edit reason path]
            [[:both-file-fields
              (assoc root-edit "file" "bench/a.edn")
              :ambiguous-editor-files ["edits" 0]]
             [:missing-file-fields
              (dissoc root-edit "files")
              :ambiguous-editor-files ["edits" 0]]
             [:duplicate-files
              (assoc root-edit "files" ["bench/a.edn" "bench/a.edn"])
              :duplicate-file ["edits" 0 "files"]]
             [:grouped-named-owner
              (assoc root-edit "within" {"form" "settings"})
              :invalid-grouped-editor-scope ["edits" 0 "within"]]
             [:edn-named-owner
              (-> root-edit
                  (dissoc "files")
                  (assoc "file" "bench/a.edn"
                         "within" {"form" "settings"}))
              :invalid-edn-editor-scope ["edits" 0 "within"]]
             [:false-root
              (assoc root-edit "within" {"root" false})
              :invalid-root-scope ["edits" 0 "within" "root"]]]]
      (testing (name label)
        (let [result (contract/validate-tool-params {"edits" [edit]})]
          (is (false? (:ok result)))
          (is (= reason (:reason result)))
          (is (= path (:path result)))
          (is (:source-unchanged result)))))
    (testing "EDN owner deletion"
      (let [result
            (contract/validate-tool-params
              {"delete_owners" [{"file" "bench/a.edn"
                                 "forms" ["settings"]}]})]
        (is (false? (:ok result)))
        (is (= :invalid-edn-editor-scope (:reason result)))
        (is (= ["delete_owners" 0 "file"] (:path result)))
        (is (:source-unchanged result))))))

(deftest editor-gesture-compiles-grouped-exact-owner-deletion
  (let [request
        {"delete_owners"
         [{"file" "src/a.clj" "forms" ["alpha" "beta"]}
          {"file" "src/b.clj" "forms" ["gamma"]}]}
        validated (contract/validate-tool-params request)
        transaction (contract/tool-params->transaction (:params validated))]
    (is (:ok validated))
    (is (= {:changes 2 :edits 3 :files 2}
           (get-in validated [:params :expect])))
    (is (= [{:id :delete-owners-1
             :in ["src/a.clj"]
             :forms ['alpha 'beta]
             :do [:delete true]
             :expect {:matches 2 :each-form 1}}
            {:id :delete-owners-2
             :in ["src/b.clj"]
             :forms ['gamma]
             :do [:delete true]
             :expect {:matches 1 :each-form 1}}]
           (:changes transaction)))
    (doseq [[label bad-request reason path]
            [[:empty-request {} :missing-fields []]
             [:empty-groups {"delete_owners" []}
              :non-empty-array ["delete_owners"]]
             [:empty-forms {"delete_owners"
                            [{"file" "src/a.clj" "forms" []}]}
              :non-empty-array ["delete_owners" 0 "forms"]]
             [:duplicate-form {"delete_owners"
                               [{"file" "src/a.clj"
                                 "forms" ["alpha" "alpha"]}]}
              :duplicate-form ["delete_owners" 0 "forms"]]
             [:blank-form {"delete_owners"
                           [{"file" "src/a.clj" "forms" [" "]}]}
              :non-blank-string ["delete_owners" 0 "forms" 0]]
             [:unknown-field {"delete_owners"
                              [{"file" "src/a.clj" "forms" ["alpha"]
                                "force" true}]}
              :unknown-fields ["delete_owners" 0]]]]
      (testing (name label)
        (let [result (contract/validate-tool-params bad-request)]
          (is (false? (:ok result)))
          (is (= reason (:reason result)))
          (is (= path (:path result)))
          (is (:source-unchanged result)))))))

(deftest editor-gesture-refuses-invalid-or-mixed-locations
  (doseq [[label request reason path]
          [[:empty-edits (assoc gesture-request "edits" [])
            :non-empty-array ["edits"]]
           [:unknown-top (assoc gesture-request "changes" [])
            :unknown-fields []]
           [:unknown-edit (assoc-in gesture-request ["edits" 0 "occurrence"]
                                    1)
            :unknown-fields ["edits" 0]]
           [:zero-matches (assoc-in gesture-request ["edits" 0 "matches"] 0)
            :positive-integer ["edits" 0 "matches"]]
           [:absolute-file (assoc-in gesture-request ["edits" 0 "file"]
                                     "/tmp/a.clj")
            :invalid-relative-source-path ["edits" 0 "file"]]
           [:blank-form (assoc-in gesture-request ["edits" 0 "within" "form"]
                                  " ")
            :non-blank-string ["edits" 0 "within" "form"]]
           [:missing-location (update-in gesture-request ["edits" 0 "within"]
                                         dissoc "form")
            :ambiguous-editor-location ["edits" 0 "within"]]
           [:mixed-location (assoc-in gesture-request
                                      ["edits" 0 "within" "namespace"]
                                      "sample.server")
            :ambiguous-editor-location ["edits" 0 "within"]]
           [:unknown-location (assoc-in gesture-request ["edits" 0 "within" "line"]
                                        12)
            :unknown-fields ["edits" 0 "within"]]
           [:blank-from (assoc-in gesture-request ["edits" 0 "from"] "")
            :non-blank-string ["edits" 0 "from"]]
           [:blank-to (assoc-in gesture-request ["edits" 0 "to"] "\n")
            :non-blank-string ["edits" 0 "to"]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params request)]
        (is (false? (:ok result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))
        (is (:source-unchanged result))))))

(deftest direct-change-accepts-only-closed-verification-profiles
  ;; @spec MCP-OP-VERIFY-001
  (let [validated (contract/validate-tool-params
                    (assoc valid-request "verify" "fast"))
        exact (contract/validate-tool-params
                (assoc valid-request "verify" "exact"))]
    (is (:ok validated))
    (is (:ok exact))
    (is (= "fast" (get-in validated [:params :verify])))
    (is (= "exact" (get-in exact [:params :verify])))
    (is (= :invalid-enum
           (:reason (contract/validate-tool-params
                      (assoc valid-request "verify" "eventually")))))))

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

(deftest normalizes-a-running-cold-proof-without-pretending-it-is-terminal
  (let [root "/work/project"
        next-call {:tool "inspect_clojure"
                   :workspace_root root
                   :verification_job "verify/123"
                   :view "verification"}
        result {:ok true
                :committed true
                :change-count 1
                :match-count 2
                :changed-file-count 1
                :receipt-file "/work/project/.receipts/undo.edn"
                :receipt-hash "receipt-hash"
                :verified {:whole-files true
                           :read-back-hashes
                           {"/work/project/src/a.clj" "a-hash"}}
                :verification
                {:ok true
                 :cold-verification
                 {:ok true :status :running
                  :verification_complete false
                  :next_call next-call}}}
        receipt (contract/normalize-success-receipt root result)]
    (is (:ok receipt))
    (is (:committed receipt))
    (is (false? (:verification_complete receipt)))
    (is (= "inspect_verification_job" (:next_action receipt)))
    (is (= next-call (:next_call receipt)))
    (is (= "/work/project/.receipts/undo.edn" (:undo_receipt receipt)))))

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
    (is (= (str "Correct :find for change 2 (gallery-renderer). "
                "Complete-input parser: :find must contain exactly one complete form "
                "with no detached comments Submit exactly one complete Clojure form.")
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
    (is (:source_unchanged result)))
  (let [result (contract/normalize-refusal
                 {:error-type :overlapping-intents
                  :error "Changes overlap in src/app.clj"
                  :change-ids [:namespace :render]
                  :intent-indexes [0 2]
                  :source-unchanged true})]
    (is (= ["namespace" "render"] (:change_ids result)))
    (is (= [0 2] (:change_indexes result)))
    (is (str/includes? (:remedy result) "namespace and render"))
    (is (:source_unchanged result))))

(deftest verification-refusal-names-the-failed-check-and-bounds-its-output
  (let [long-output (apply str (repeat 3000 "x"))
        result (contract/normalize-refusal
                 {:ok false
                  :error-type :verification-failed
                  :error "Verification failed; rolled back"
                  :rolled-back true
                  :verification
                  {:ok false
                   :profile "fast"
                   :checks [{:ok false :command "clj-kondo" :exit 2
                             :output long-output}]}})]
    (is (= "verification-failed" (:error_type result)))
    (is (true? (:source_unchanged result)))
    (is (true? (:rolled_back result)))
    (is (str/includes? (:remedy result) "fast verification check `clj-kondo`"))
    (is (str/includes? (:remedy result) "exit 2"))
    (is (str/includes? (:remedy result) "retry the same request once"))
    (is (= 2000 (count (get-in result [:verification :checks 0 :output]))))))

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

(deftest defmethod-owner-crosses-the-json-boundary-as-closed-data
  (let [request (assoc-in valid-request ["changes" 0 "forms"]
                          [{"kind" "defmethod"
                            "name" "render"
                            "dispatch" ":card"}])
        validated (contract/validate-tool-params request)
        transaction (contract/tool-params->transaction (:params validated))]
    (is (:ok validated))
    (is (= [{:kind :defmethod :name 'render :dispatch ":card"}]
           (get-in transaction [:changes 0 :forms])))
    (doseq [[label form-owner expected]
            [["unsupported kind"
              {"kind" "defn" "name" "render" "dispatch" ":card"}
              "invalid-form-owner-kind"]
             ["blank name"
              {"kind" "defmethod" "name" "" "dispatch" ":card"}
              "non-blank-string"]
             ["blank dispatch"
              {"kind" "defmethod" "name" "render" "dispatch" ""}
              "non-blank-string"]
             ["unknown field"
              {"kind" "defmethod" "name" "render" "dispatch" ":card"
               "line" 42}
              "unknown-fields"]]]
      (testing label
        (let [result (contract/validate-tool-params
                       (assoc-in valid-request ["changes" 0 "forms"]
                                 [form-owner]))]
          (is (false? (:ok result)))
          (is (= expected (some-> (:reason result) name))))))))

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

(deftest normalizes-only-provable-editor-bookkeeping
  ;; @spec MCP-OP-EDIT-006
  ;; @spec MCP-OP-EDIT-007
  ;; @spec MCP-OP-EDIT-010
  (let [packed
        (str "(deftest renders-a-button (is true))\n"
             "(deftest renders-a-link (is true))")
        change (-> (get-in valid-request ["changes" 0])
                   (dissoc "replace")
                   (assoc "insert_after" [packed]))
        request {"changes" [change]
                 "expect" {"changes" 91 "edits" 92 "files" 93}}
        validated (contract/validate-tool-params request)
        without-aggregate
        (contract/validate-tool-params (dissoc request "expect"))
        transaction (some-> validated :params
                            contract/tool-params->transaction)]
    (is (:ok validated) (pr-str validated))
    (is (= {:ignored ["expect"]
            :reason "aggregate counts are derived from exact change guards"}
           (:input-normalization validated)))
    (is (= {:changes 1 :edits 1 :files 1}
           (get-in validated [:params :expect])))
    (is (:ok without-aggregate) (pr-str without-aggregate))
    (is (= {:changes 1 :edits 1 :files 1}
           (get-in without-aggregate [:params :expect])))
    (is (not (contains? without-aggregate :input-normalization)))
    (is (= [:insert-right
            ["(deftest renders-a-button (is true))"
             "(deftest renders-a-link (is true))"]]
           (get-in transaction [:changes 0 :do]))))
  (let [malformed
        (str "(deftest renders-a-button (is true))\n"
             "(deftest renders-a-link (is true))))")
        request
        {"changes"
         [(-> (get-in valid-request ["changes" 0])
              (dissoc "replace")
              (assoc "insert_after" [malformed]))]
         "expect" {"changes" 1 "edits" 1 "files" 1}}
        path ["changes" 0 "insert_after" 0]
        result (contract/validate-tool-params request)
        template (:retry-template result)]
    (is (false? (:ok result)))
    (is (= :invalid-intent-form (:error-type result)))
    (is (= :invalid-intent-form (:reason result)))
    (is (= path (:path result)))
    (is (re-find #"Unmatched delimiter" (:error result)))
    (is (= "apply_clojure_changes" (:operation template)))
    (is (false? (:executable template)))
    (is (false? (:selector-authority template)))
    (is (false? (:write-authority template)))
    (is (= [{:path path
             :kind :clojure-forms
             :authority false}]
           (:holes template)))
    (is (= (assoc-in request path nil)
           (:arguments template)))
    (is (not (contains? result :next-call)))))

(deftest validates-top-level-insertion-without-repeating-owner-source
  (let [change (-> (get-in valid-request ["changes" 0])
                   (dissoc "find" "replace")
                   (assoc "insert_after" ["(defn helper [] :ready)"]))
        request {"changes" [change]
                 "expect" {"changes" 1 "edits" 1 "files" 1}}
        validated (contract/validate-tool-params request)
        transaction (some-> validated :params contract/tool-params->transaction)]
    (is (:ok validated))
    (is (not (contains? (get-in transaction [:changes 0]) :find)))
    (is (= [:insert-right ["(defn helper [] :ready)"]]
           (get-in transaction [:changes 0 :do]))))
  (doseq [[label change path]
          [[:several-owners
            (-> (get-in valid-request ["changes" 0])
                (dissoc "find" "replace")
                (assoc "forms" ["ide-shell" "source-reader-shell"]
                       "insert_after" ["(defn helper [] :ready)"]))
            ["changes" 0 "forms"]]
           [:namespace-owner
            (-> (get-in valid-request ["changes" 0])
                (dissoc "find" "replace" "forms")
                (assoc "owner" {"kind" "namespace"
                                "name" "bench.app-shell"}
                       "insert_after" ["(defn helper [] :ready)"]))
            ["changes" 0 "owner"]]]]
    (testing (name label)
      (let [result (contract/validate-tool-params
                     {"changes" [change]
                      "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (false? (:ok result)))
        (is (= :invalid-top-level-insertion-owner (:reason result)))
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

(deftest validates-and-compiles-whole-owner-deletion-without-find
  (let [request
        {"changes"
         [{"id" "delete-obsolete"
           "files" ["src/demo.clj"]
           "forms" ["alpha" "beta"]
           "delete" true
           "expect" {"matches" 2 "each_form" 1}}]
         "expect" {"changes" 1 "edits" 2 "files" 1}}
        validated (contract/validate-tool-params request)
        transaction (contract/tool-params->transaction (:params validated))]
    (is (:ok validated))
    (is (= [:delete true] (get-in transaction [:changes 0 :do])))
    (is (not (contains? (get-in transaction [:changes 0]) :find)))
    (is (= {:matches 2 :each-form 1}
           (get-in transaction [:changes 0 :expect])))
    (is (= :invalid-delete-action
           (:reason
             (contract/validate-tool-params
               (assoc-in request ["changes" 0 "delete"] false)))))))
