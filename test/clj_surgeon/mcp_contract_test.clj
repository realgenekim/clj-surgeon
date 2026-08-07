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
            :kernel-refusal]
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

(deftest normalizes-kernel-refusal-with-stable-remedy
  (let [result
        (contract/normalize-refusal
          {:error "Compiled transaction does not match aggregate expectations"
           :error-type :transaction-expectation-mismatch
           :expected {:intent-count 1 :edit-count 2 :changed-file-count 1}
           :actual {:intent-count 1 :edit-count 1 :changed-file-count 1}
           :remedies {:count "Set matches to 1"}})]
    (is (= false (:ok result)))
    (is (= "transaction-expectation-mismatch" (:error_type result)))
    (is (= true (:source_unchanged result)))
    (is (= {:intent-count 1 :edit-count 2 :changed-file-count 1}
           (:expected result)))
    (is (= {:count "Set matches to 1"} (:remedies result)))
    (is (= "Correct the declared scope or count and call apply_clojure_changes once."
           (:remedy result)))))
