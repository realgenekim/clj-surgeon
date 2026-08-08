(ns clj-surgeon.mcp-tool-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def fixture-root "bench/fixtures/edit_portfolio/decision-batch-edit")

(def decision-request
  {"changes"
   [{"id" "app-body"
     "files" ["src/bench/app_shell.clj"]
     "forms" ["ide-shell"]
     "find" ":body"
     "replace" ":body.ide-shell-page"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
    {"id" "stylesheet"
     "files" ["src/bench/app_shell.clj"]
     "forms" ["ide-shell"]
     "find" "\"/app.css\""
     "replace" "\"/command-center.css\""
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
    {"id" "reader-arguments"
     "files" ["src/bench/source_reader.clj"]
     "forms" ["source-reader-shell"]
     "find" "[project-id projects artifact current-location reader-region show-all?]"
     "replace" "[project-id projects artifact document-title current-location reader-region show-all?]"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
    {"id" "document-title"
     "files" ["src/bench/source_reader.clj"]
     "forms" ["source-reader-shell"]
     "find" "[:title \"Workbench\"]"
     "replace" "[:title (str document-title \" — Workbench\")]"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
    {"id" "reader-body"
     "files" ["src/bench/source_reader.clj"]
     "forms" ["source-reader-shell"]
     "find" ":body"
     "replace" ":body.ide-shell-page"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
    {"id" "tab-title"
     "files" ["src/bench/source_reader.clj"]
     "forms" ["source-reader-shell"]
     "find" "[:span.tab-label artifact]"
     "replace" "[:span.tab-label {:title artifact} document-title]"
     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
   "expect" {"changes" 6 "edits" 6 "files" 2}})

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-mcp-tool-test-"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- copy-tree!
  [from to]
  (doseq [source (file-seq (io/file from))]
    (let [relative (.relativize (.toPath (io/file from)) (.toPath source))
          target (.toFile (.resolve (.toPath (io/file to)) relative))]
      (if (.isDirectory source)
        (.mkdirs target)
        (do
          (.mkdirs (.getParentFile target))
          (io/copy source target))))))

(deftest commits-the-real-six-edit-fixture-and-undoes-it
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)}
                     decision-request)]
        (is (:ok result))
        (is (:verification_complete result))
        (is (= 6 (:changes result)))
        (is (= 6 (:edits result)))
        (is (= 2 (:files result)))
        (is (= #{"src/bench/app_shell.clj" "src/bench/source_reader.clj"}
               (set (keys (:read_back_hashes result)))))
        (doseq [relative ["src/bench/app_shell.clj"
                          "src/bench/source_reader.clj"]]
          (is (= (slurp (io/file fixture-root "after" relative))
                 (slurp (io/file workspace relative)))))
        (let [undo (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})]
          (is (:ok undo))
          (doseq [relative ["src/bench/app_shell.clj"
                            "src/bench/source_reader.clj"]]
            (is (= (slurp (io/file fixture-root "before" relative))
                   (slurp (io/file workspace relative)))))))
      (finally
        (delete-tree! workspace)))))

(deftest refuses-before-write-and-publishes-no-receipt
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [before (slurp (io/file workspace "src/bench/app_shell.clj"))
            request (assoc-in decision-request ["changes" 0 "expect" "matches"] 2)
            result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)}
                     request)]
        (is (false? (:ok result)))
        (is (= true (:source_unchanged result)))
        (is (= before (slurp (io/file workspace "src/bench/app_shell.clj"))))
        (is (not (.exists receipt-dir))))
      (finally
        (delete-tree! workspace)))))

(deftest comment-bearing-insertion-refusal-proves-source-unchanged
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/demo.clj")
        receipt-dir (io/file workspace "receipts")
        source (str "(ns demo)\n"
                    "(def metrics [:wall-ms\n"
                    "              ;; belongs to input tokens\n"
                    "              :input-tokens])\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {"changes"
               [{"id" "ambiguous-gap"
                 "files" ["src/demo.clj"]
                 "forms" ["metrics"]
                 "find" ":input-tokens"
                 "insert_before" [":cached-input-tokens"]
                 "expect" {"matches" 1 "each_file" 1 "each_form" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (= false (:ok result)))
        (is (= "ambiguous-insertion-gap" (:error_type result)))
        (is (= "compile" (:kernel_phase result)))
        (is (= true (:source_unchanged result)))
        (is (= source (slurp source-file)))
        (is (not (.exists receipt-dir))))
      (finally
        (delete-tree! workspace)))))

(deftest basis-route-refuses-mixed-fields-before-basis-lookup
  (let [loads (atom 0)
        config {:project-root "."
                :verification-profiles-fn
                (fn []
                  (swap! loads inc)
                  {"fast" ["true"]})}
        result (mcp-tool/execute-request!
                 config
                 {"basis" "cb-does-not-exist"
                  "decisions" []
                  "changes" []})]
    (is (false? (:ok result)))
    (is (= :invalid-mcp-request (:error-type result)))
    (is (= ["changes"] (:unknown-fields result)))
    (is (:source-unchanged result))
    (is (= :unknown-or-expired-basis
           (:error-type
             (mcp-tool/execute-request!
               config
               {"basis" "cb-does-not-exist" "decisions" []}))))
    (is (= 2 @loads) "the workspace verification profile is read for every basis request")))

(deftest basis-schema-exposes-whole-site-delete-as-a-decision
  (let [decision-schema (get-in mcp-tool/basis-change-schema
                                [:properties "decisions" :items])]
    (is (= {:type "boolean" :const true
            :description
            "Delete the prepared owner and its contiguous leading comment block."}
           (get-in decision-schema [:properties "delete"])))
    (is (some #(= {:required ["delete"]} %)
              (:oneOf decision-schema)))))

(deftest confines-real-targets-to-the-project-root
  (let [workspace (temp-dir)
        outside (temp-dir)
        link (io/file workspace "linked.clj")]
    (try
      (spit (io/file outside "outside.clj") "(ns outside)\n(def x :old)\n")
      (Files/createSymbolicLink
        (.toPath link)
        (.toPath (io/file outside "outside.clj"))
        (make-array FileAttribute 0))
      (let [request
            {"changes"
             [{"id" "escape" "files" ["linked.clj"] "forms" ["x"]
               "find" ":old" "replace" ":new"
               "expect" {"matches" 1 "each_form" 1}}]
             "expect" {"changes" 1 "edits" 1 "files" 1}}
            result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath (io/file workspace "receipts"))}
                     request)]
        (is (false? (:ok result)))
        (is (= "path-outside-project" (:error_type result)))
        (is (= true (:source_unchanged result)))
        (is (= "(ns outside)\n(def x :old)\n"
               (slurp (io/file outside "outside.clj")))))
      (finally
        (delete-tree! workspace)
        (delete-tree! outside)))))

(deftest routes-one-shared-writer-without-cross-workspace-mutation
  (let [default-root (temp-dir)
        requested-root (temp-dir)
        default-file (io/file default-root "src/demo.clj")
        requested-file (io/file requested-root "src/demo.clj")]
    (try
      (.mkdirs (.getParentFile default-file))
      (.mkdirs (.getParentFile requested-file))
      (spit default-file "(ns demo)\n(def marker :default)\n")
      (spit requested-file "(ns demo)\n(def marker :requested)\n")
      (let [request
            {"workspace_root" (.getPath requested-root)
             "changes"
             [{"id" "marker" "files" ["src/demo.clj"]
               "forms" ["marker"] "find" ":requested" "replace" ":changed"
               "expect" {"matches" 1 "each_form" 1}}]
             "expect" {"changes" 1 "edits" 1 "files" 1}}
            routed-receipts (io/file requested-root "routed-receipts")
            result
            (with-redefs [workspace/receipt-dir
                          (fn [workspace-root]
                            (is (= (.getCanonicalPath requested-root)
                                   (.toString
                                     (mcp-paths/real-root workspace-root))))
                            (.getPath routed-receipts))]
              (mcp-tool/execute-request!
                {:project-root (.getPath default-root)}
                request))]
        (is (:ok result))
        (is (= (.getPath (.getCanonicalFile requested-root))
               (:workspace_root result)))
        (is (= (.getCanonicalPath routed-receipts)
               (.getCanonicalPath (.getParentFile (io/file (:undo_receipt result))))))
        (is (= "(ns demo)\n(def marker :default)\n" (slurp default-file)))
        (is (= "(ns demo)\n(def marker :changed)\n" (slurp requested-file))))
      (finally
        (delete-tree! default-root)
        (delete-tree! requested-root)))))

(deftest namespace-owner-transaction-crosses-the-complete-writer-boundary
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/demo.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns demo\n"
                      "  (:require [legacy.api :as legacy]))\n"
                      "(defn use-api [] [legacy.api :as legacy])\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {"workspace_root" (.getPath workspace)
               "changes"
               [{"id" "namespace-require"
                 "files" ["src/demo.clj"]
                 "owner" {"kind" "namespace" "name" "demo"}
                 "find" "[legacy.api :as legacy]"
                 "replace" "[current.api :as current]"
                 "expect" {"matches" 1 "each_file" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}})]
        (is (:ok result))
        (is (= (.getPath (.getCanonicalFile workspace))
               (:workspace_root result)))
        (is (= (str "(ns demo\n"
                    "  (:require [current.api :as current]))\n"
                    "(defn use-api [] [legacy.api :as legacy])\n")
               (slurp source-file)))
        (is (.isFile (io/file (:undo_receipt result)))))
      (finally
        (delete-tree! workspace)))))

(deftest callback-uses-mcp-success-and-error-channels
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        calls (atom [])
        callback (fn [content error? structured]
                   (swap! calls conj {:content (first content)
                                      :payload structured
                                      :error? error?}))]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (mcp-tool/init! {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)})
      (mcp-tool/handle-clj-change nil decision-request callback)
      (testing "the direct route steers named forms away from namespace owner syntax"
        (is (re-find #"For named top-level def or defn owners, use forms: \[name\]"
                     mcp-tool/tool-description))
        (is (re-find #"never pass owner as a string"
                     mcp-tool/tool-description)))
      (is (= false (:error? (first @calls))))
      (is (= true (get-in @calls [0 :payload :verification_complete])))
      (is (= (str "apply_clojure_changes\n"
                  "  6 edits · 2 files\n\n"
                  "✓ atomic commit complete\n"
                  "✓ written bytes read back and verified\n"
                  "✓ terminal evidence · verification_complete=true · next action none")
             (get-in @calls [0 :content])))
      (mcp-tool/handle-clj-change nil
                                  (assoc decision-request "unexpected" true)
                                  callback)
      (is (= true (:error? (second @calls))))
      (is (= "invalid-mcp-request"
             (get-in @calls [1 :payload :error_type])))
      (is (re-find #"refused · unknown-fields at \[\]"
                   (get-in @calls [1 :content])))
      (is (not (re-find #"\{\"ok\""
                        (get-in @calls [1 :content]))))
      (testing "the handler enforces the same basis-only verify boundary"
        (mcp-tool/handle-clj-change nil
                                    (assoc decision-request "verify" "fast")
                                    callback)
        (is (= true (:error? (nth @calls 2))))
        (is (= "invalid-mcp-request"
               (get-in @calls [2 :payload :error_type])))
        (is (= "unknown-fields"
               (get-in @calls [2 :payload :reason])))
        (is (= [] (get-in @calls [2 :payload :path])))
        (is (= true (get-in @calls [2 :payload :source_unchanged]))))
      (testing "refusal summaries preserve the actionable diagnostic"
        (testing "keyword refusal types and rollback state remain truthful"
          (is (re-find #"refused · verification-failed"
                       (mcp-tool/concise-summary
                         {:ok false
                          :error-type :verification-failed
                          :rolled-back true})))
          (is (re-find #"source state requires structured receipt review"
                       (mcp-tool/concise-summary
                         {:ok false
                          :error-type :verification-failed
                          :rolled-back false}))))
        (testing "the exact failed change and field stay visible"
          (is (= (str "apply_clojure_changes\n"
                      "  refused · invalid-intent-form\n"
                      "  change 0 · gallery-resolver · field :find\n\n"
                      "✓ source unchanged\n"
                      "→ Pass exactly one complete parseable Clojure form in :find for change 0 (gallery-resolver).")
                 (mcp-tool/concise-summary
                   {:ok false
                    :error_type "invalid-intent-form"
                    :reason "invalid-intent-form"
                    :phase "kernel"
                    :change_index 0
                    :change_id "gallery-resolver"
                    :field ":find"
                    :source_unchanged true
                    :remedy "Pass exactly one complete parseable Clojure form in :find for change 0 (gallery-resolver)."})))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

(deftest binding-aware-rename-is-one-verified-and-undoable-mcp-transaction
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/demo.clj")
        receipt-dir (io/file workspace "receipts")
        source (str "(ns demo)\n"
                    "(defn feed [{:keys [sort-by] :or {sort-by :score}}] [sort-by :sort-by clojure.core/sort-by])\n"
                    "(defn table [{:keys [sort-by]}] (name sort-by))\n")
        analysis
        {:locals
         [{:row 2 :col 21 :end-row 2 :end-col 28 :name 'sort-by :id 1}
          {:row 3 :col 22 :end-row 3 :end-col 29 :name 'sort-by :id 2}]
         :local-usages
         [{:row 2 :col 35 :end-row 2 :end-col 42 :name 'sort-by :id 1}
          {:row 2 :col 54 :end-row 2 :end-col 61 :name 'sort-by :id 1}
          {:row 3 :col 39 :end-row 3 :end-col 46 :name 'sort-by :id 2}]}
        request
        {"workspace_root" (.getPath workspace)
         "changes"
         [{"id" "rename-sort-binding"
           "files" ["src/demo.clj"]
           "forms" ["feed" "table"]
           "rename_binding"
           {"from" "sort-by"
            "to" "sort-field"
            "preserve_external_key" true}
           "expect" {"matches" 5 "each_form" 1}}]
         "expect" {"changes" 1 "edits" 5 "files" 1}}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (let [result
            (binding [transaction/*binding-analyzer* (fn [_ _] analysis)]
              (mcp-tool/execute-request!
                {:project-root (.getPath workspace)
                 :receipt-dir (.getPath receipt-dir)}
                request))]
        (is (:ok result))
        (is (:verification_complete result))
        (is (= 5 (:edits result)))
        (is (= 1 (:files result)))
        (is (= (str "(ns demo)\n"
                    "(defn feed [{:keys [] :or {sort-field :score} sort-field :sort-by}] [sort-field :sort-by clojure.core/sort-by])\n"
                    "(defn table [{:keys [] sort-field :sort-by}] (name sort-field))\n")
               (slurp source-file)))
        (let [undo (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})]
          (is (:ok undo))
          (is (= source (slurp source-file)))))
      (finally
        (delete-tree! workspace)))))
