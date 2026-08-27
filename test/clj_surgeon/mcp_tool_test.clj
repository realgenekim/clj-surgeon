(ns clj-surgeon.mcp-tool-test
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [nrepl.server :as nrepl-server])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def fixture-root "bench/fixtures/edit_portfolio/decision-batch-edit")

(deftest hot-transaction-law
  (is (= :live :live)))

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
        (is (:ok result) (pr-str result))
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

(deftest editor-gesture-is-exact-guarded-and-undoable
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/demo.clj")
        before (str "(ns demo)\n\n"
                    "(defn route-event [event]\n"
                    "  (case event\n"
                    "    :pending :wait\n"
                    "    :done))\n\n")
        after (str "(ns demo)\n\n"
                   "(defn route-event [event]\n"
                   "  (case event\n"
                   "    :pending :wait\n"
                   "    :complete))\n\n")
        request
        {"edits"
         [{"file" "src/demo.clj"
           "within" {"form" "route-event"}
           "from" ":done"
           "to" ":complete"}]
         "expect" {"changes" 0 "edits" 1 "files" 1}
         "verify" "fast"}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)
                      :verification-profiles {"fast" {:commands []}}
                      :verify! (fn [_root profile _profiles _files]
                                 {:ok true :profile profile :checks []})}
                     request)]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (= 1 (:changes result)))
        (is (= 1 (:edits result)))
        (is (= 1 (:files result)))
        (is (= {:ignored ["expect"]
                :reason "editor counts are derived"}
               (:input_normalization result)))
        (is (= after (slurp source-file))
            "every unrelated byte, including the extra EOF newline, survives")
        (let [stale (mcp-tool/execute-request!
                      {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)
                       :verification-profiles {"fast" {:commands []}}
                       :verify! (fn [_root profile _profiles _files]
                                  {:ok true :profile profile :checks []})}
                      request)]
          (is (false? (:ok stale)))
          (is (= "expect-count-mismatch" (:error_type stale)))
          (is (:source_unchanged stale))
          (is (= after (slurp source-file))))
        (let [undo (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})]
          (is (:ok undo))
          (is (= before (slurp source-file)))))
      (finally
        (delete-tree! workspace)))))

(deftest grouped-root-edn-edit-is-atomic-lossless-and-undoable
  ;; @spec MCP-OP-EDIT-001
  ;; @spec MCP-OP-EDIT-003
  ;; @spec MCP-OP-EDIT-004
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        first-file (io/file workspace "bench/first.edn")
        second-file (io/file workspace "bench/second.edn")
        old-verification "{:exact-bytes true :parse-clojure true}"
        new-verification
        (str "{:exact-bytes-secondary true\n"
             " :meaning-preserved true\n"
             " :parse-clojure true}")
        first-before
        (str "{:id :first\n"
             " :verification " old-verification "\n"
             " ;; operational comment survives exactly\n"
             " :tagged #demo/value {:x 1}}\n")
        second-before
        (str "{:id :second\n"
             " :verification " old-verification "\n"
             " :meta ^:keep [:same]}\n")
        first-after (str/replace first-before old-verification new-verification)
        second-after (str/replace second-before old-verification new-verification)
        request
        {"edits"
         [{"files" ["bench/first.edn" "bench/second.edn"]
           "within" {"root" true}
           "from" old-verification
           "to" new-verification}]}
        config {:project-root (.getPath workspace)
                :receipt-dir (.getPath receipt-dir)}]
    (try
      (.mkdirs (.getParentFile first-file))
      (spit first-file first-before)
      (spit second-file second-before)
      (let [result (mcp-tool/execute-request! config request)]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (= 1 (:changes result)))
        (is (= 2 (:edits result)))
        (is (= 2 (:files result)))
        (is (= first-after (slurp first-file)))
        (is (= second-after (slurp second-file)))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= first-before (slurp first-file)))
        (is (= second-before (slurp second-file))))
      (let [second-mismatch (str/replace second-before
                                         ":exact-bytes true"
                                         ":exact-bytes false")]
        (spit second-file second-mismatch)
        (let [refused (mcp-tool/execute-request! config request)]
          (is (false? (:ok refused)))
          (is (= "expect-count-mismatch" (:error_type refused)))
          (is (:source_unchanged refused))
          (is (= first-before (slurp first-file)))
          (is (= second-mismatch (slurp second-file)))))
      (finally
        (delete-tree! workspace)))))

(deftest hybrid-editor-batch-commits-one-historical-two-file-decision
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        request
        {"edits"
         [{"file" "src/bench/app_shell.clj"
           "within" {"form" "ide-shell"}
           "from" ":body"
           "to" ":body.ide-shell-page"}
          {"file" "src/bench/source_reader.clj"
           "within" {"form" "source-reader-shell"}
           "from" "[project-id projects artifact current-location reader-region show-all?]"
           "to" "[project-id projects artifact document-title current-location reader-region show-all?]"}
          {"file" "src/bench/source_reader.clj"
           "within" {"form" "source-reader-shell"}
           "from" ":body"
           "to" ":body.ide-shell-page"}
          {"file" "src/bench/source_reader.clj"
           "within" {"form" "source-reader-shell"}
           "from" "[:span.tab-label artifact]"
           "to" "[:span.tab-label {:title artifact} document-title]"}]
         "programs"
         [{"file" "src/bench/app_shell.clj"
           "expression" "(-> (form 'ide-shell) (match \"/app.css\") (transform (constantly \"/command-center.css\")))"
           "expect" {"matches" 1 "max_changed_characters" 21}}
          {"file" "src/bench/source_reader.clj"
           "expression" "(-> (form 'source-reader-shell) (match '[:title \"Workbench\"]) (transform (constantly '[:title (str document-title \" — Workbench\")])))"
           "expect" {"matches" 1 "max_changed_characters" 48}}]}]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [refused
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              (assoc-in request ["programs" 0 "expect" "matches"] 2))]
        (is (false? (:ok refused)))
        (is (= "expected-count-mismatch" (:error_type refused)))
        (is (:source_unchanged refused))
        (doseq [relative ["src/bench/app_shell.clj"
                          "src/bench/source_reader.clj"]]
          (is (= (slurp (io/file fixture-root "before" relative))
                 (slurp (io/file workspace relative))))))
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)}
                     request)]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (= 6 (:edits result)))
        (is (= 2 (:files result)))
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

(deftest editor-gesture-skips-whole-file-formatting
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/demo.clj")
        formatter-calls (atom 0)
        before (str "(ns demo)\n\n"
                    "(defn status [] :old)\n\n")
        after (str "(ns demo)\n\n"
                   "(defn status [] :new)\n\n")
        request
        {"edits"
         [{"file" "src/demo.clj"
           "within" {"form" "status"}
           "from" ":old"
           "to" ":new"}]}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates!
               (fn [_project-root _command future-sources]
                 (swap! formatter-calls inc)
                 {:ok true
                  :status :complete
                  :file-count 1
                  :changed-file-count 1
                  :elapsed_ms 0.1
                  :future-sources
                  (update-vals future-sources
                               #(str/replace % #"\n\n$" "\n"))})}
              request)]
        (is (:ok result) (pr-str result))
        (is (zero? @formatter-calls)
            "a surgical gesture must not normalize unrelated file bytes")
        (is (= after (slurp source-file)))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= before (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest compact-owner-deletion-is-exact-atomic-and-undoable
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/demo.clj")
        formatter-calls (atom 0)
        before (str "(ns demo)\n\n"
                    ";; attached obsolete explanation\n"
                    "(defn obsolete-a [] :a)\n\n"
                    "(defn keep-me [] :kept)\n\n"
                    "(defn obsolete-b [] :b)\n")
        after (str "(ns demo)\n\n"
                   "(defn keep-me [] :kept)\n")
        request
        {"delete_owners"
         [{"file" "src/demo.clj"
           "forms" ["obsolete-a" "obsolete-b"]}]}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates!
               (fn [_project-root _command future-sources]
                 (swap! formatter-calls inc)
                 {:ok true :future-sources future-sources})}
              request)]
        (is (:ok result) (pr-str result))
        (is (= 1 (:changes result)))
        (is (= 2 (:edits result)))
        (is (= 1 (:files result)))
        (is (zero? @formatter-calls)
            "compact deletion preserves unrelated source spelling")
        (is (= after (slurp source-file)))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= before (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest compact-namespace-edit-and-owner-deletion-share-one-transaction
  ;; @spec MCP-OP-EDIT-005
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/demo.clj")
        before (str "(ns demo\n"
                    "  (:require\n"
                    "   [sample.old :as old]))\n\n"
                    ";; moved to sample.new\n"
                    "(defn obsolete-handler [] :old)\n\n"
                    "(def routes [old/handle])\n")
        after (str "(ns demo\n"
                   "  (:require\n"
                   "   [sample.new :as new]\n"
                   "   [sample.old :as old]))\n\n"
                   "(def routes [new/handle])\n")
        request
        {"edits"
         [{"file" "src/demo.clj"
           "within" {"namespace" true}
           "from" "(:require [sample.old :as old])"
           "to" "(:require\n   [sample.new :as new]\n   [sample.old :as old])"}
          {"file" "src/demo.clj"
           "within" {"form" "routes"}
           "from" "old/handle"
           "to" "new/handle"}]
         "delete_owners"
         [{"file" "src/demo.clj"
           "forms" ["obsolete-handler"]}]}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              request)]
        (is (:ok result) (pr-str result))
        (is (= 3 (:changes result)))
        (is (= 3 (:edits result)))
        (is (= 1 (:files result)))
        (is (= after (slurp source-file)))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= before (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest editor-gesture-replaces-exact-known-multiplicity
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/demo.clj")
        before (str "(ns demo)\n\n"
                    "(defn repeated-status [] [:old :old])\n")
        after (str "(ns demo)\n\n"
                   "(defn repeated-status [] [:new :new])\n")
        request
        {"edits"
         [{"file" "src/demo.clj"
           "within" {"form" "repeated-status"}
           "from" ":old"
           "to" ":new"
           "matches" 2}]}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)}
                     request)]
        (is (:ok result) (pr-str result))
        (is (= 1 (:changes result)))
        (is (= 2 (:edits result)))
        (is (= after (slurp source-file)))
        (let [stale (mcp-tool/execute-request!
                      {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)}
                      request)]
          (is (false? (:ok stale)))
          (is (= "expect-count-mismatch" (:error_type stale)))
          (is (:source_unchanged stale))
          (is (= after (slurp source-file))))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= before (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest same-file-namespace-and-form-edits-format-commit-and-undo-once
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/sample.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns sample.core)\n"
                      "(defn alpha [] :old-a)\n"
                      "(defn beta [] :old-b)\n")
        formatted (str "(ns sample.next)\n\n"
                       "(defn alpha\n  []\n  :new-a)\n\n"
                       "(defn beta\n  []\n  :new-b)\n")
        request
        {"changes"
         [{"id" "namespace"
           "files" ["src/sample.clj"]
           "owner" {"kind" "namespace" "name" "sample.core"}
           "find" "sample.core"
           "replace" "sample.next"
           "expect" {"matches" 1 "each_file" 1}}
          {"id" "alpha"
           "files" ["src/sample.clj"]
           "forms" ["alpha"]
           "find" ":old-a"
           "replace" ":new-a"
           "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
          {"id" "beta"
           "files" ["src/sample.clj"]
           "forms" ["beta"]
           "find" ":old-b"
           "replace" ":new-b"
           "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
         "expect" {"changes" 3 "edits" 3 "files" 1}}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :formatter ["fixture-formatter" "{files}"]
               :format-candidates!
               (fn [_project-root _command future-sources]
                 {:ok true
                  :status :complete
                  :file-count 1
                  :changed-file-count 1
                  :elapsed_ms 0.1
                  :future-sources
                  (into {} (map (fn [[file _]] [file formatted]))
                        future-sources)})}
              request)]
        (is (:ok result) (pr-str result))
        (is (= 3 (:changes result)))
        (is (= 3 (:edits result)))
        (is (= 1 (:files result)))
        (is (:verification_complete result))
        (is (= formatted (slurp source-file)))
        (when (:ok result)
          (let [receipt (edn/read-string (slurp (:undo_receipt result)))]
            (is (= 3 (:match-count receipt)))
            (is (= 1 (:inverse-edit-count receipt)))
            (is (= 1 (count (get-in receipt [:files 0 :inverse-edits])))))
          (is (:ok (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})))
          (is (= original (slurp source-file)))))
      (finally
        (delete-tree! workspace)))))

(deftest compiles-one-extraction-through-the-public-mcp-boundary
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/sample/core.clj")
        target-file (io/file workspace "src/sample/moved.clj")
        caller-file (io/file workspace "src/sample/user.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns sample.core\n"
                      "  (:require [clojure.string :as str]))\n\n"
                      "(defn helper [x] (str/upper-case x))\n\n"
                      "(defn retained [] :ok)\n")
        caller-original (str "(ns sample.user)\n\n"
                             "(defn use-helper [x]\n"
                             "  (sample.core/helper x))\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (spit caller-file caller-original)
      (let [plan
            (extraction-plan/plan!
              {:project-root (.getPath workspace)}
              {:mode "plan-extraction"
               :file "src/sample/core.clj"
               :to "src/sample/moved.clj"
               :forms ["helper"]
               :require_policy "copy-all"})
            result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {:extraction
               {:file "src/sample/core.clj"
                :to "src/sample/moved.clj"
                :forms ["helper"]
                :require_policy "copy-all"
                :source_hash (:source_hash plan)
                :caller_changes
                [{:id "redirect-helper"
                  :files ["src/sample/user.clj"]
                  :forms ["use-helper"]
                  :find "sample.core/helper"
                  :replace "sample.moved/helper"
                  :expect {:matches 1 :each_form 1 :each_file 1}}]
                :ignored_caller_files []}})]
        (is (:ok plan) (pr-str plan))
        (is (= ["src/sample/user.clj"]
               (get-in plan [:plan :callers-to-review])))
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (string? (:receipt_hash result)))
        (is (= "structural-candidates-only"
               (get-in result [:caller_proof :level])))
        (is (false? (get-in result
                            [:caller_proof :zero_callers_authoritative])))
        (is (str/includes? (mcp-tool/concise-summary
                             (assoc result :elapsed_ms 0.1))
                           "not semantic completeness"))
        (is (= 1 (:changes result)))
        (is (= 2 (:edits result)))
        (is (= 3 (:files result)))
        (is (.exists target-file))
        (is (= "(ns sample.core\n  (:require [clojure.string :as str]))\n\n(defn retained [] :ok)\n"
               (slurp source-file)))
        (is (= (str "(ns sample.user)\n\n"
                    "(defn use-helper [x]\n"
                    "  (sample.moved/helper x))\n")
               (slurp caller-file)))
        (let [_receipt-is-edn (edn/read-string (slurp (:undo_receipt result)))
              undo (extract/undo! {:receipt (:undo_receipt result)})]
          (is (:ok undo) (pr-str undo))
          (is (= original (slurp source-file)))
          (is (= caller-original (slurp caller-file)))
          (is (not (.exists target-file)))))
      (finally
        (delete-tree! workspace)))))

(deftest extraction-creates-and-undoes-a-missing-target-parent
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/sample/core.clj")
        target-file (io/file workspace "src/sample/nested/moved.clj")
        target-parent (.getParentFile target-file)
        receipt-dir (io/file workspace "receipts")
        formatter-files (atom nil)
        original (str "(ns sample.core)\n\n"
                      "(defn helper [] :ok)\n\n"
                      "(defn retained [] :ok)\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (let [plan
            (extraction-plan/plan!
              {:project-root (.getPath workspace)}
              {:mode "plan-extraction"
               :file "src/sample/core.clj"
               :to "src/sample/nested/moved.clj"
               :forms ["helper"]
               :require_policy "copy-all"})]
        (is (:ok plan) (pr-str plan))
        (is (not (.exists target-parent)))
        (let [result
              (mcp-tool/execute-request!
                {:project-root (.getPath workspace)
                 :receipt-dir (.getPath receipt-dir)
                 :formatter ["format" "{files}"]
                 :format-candidates!
                 (fn [_ _ future-sources]
                   (reset! formatter-files (vec (keys future-sources)))
                   {:ok true
                    :status :complete
                    :file-count (count future-sources)
                    :changed-file-count 0
                    :elapsed_ms 0.1
                    :future-sources future-sources})}
                {:extraction
                 {:file "src/sample/core.clj"
                  :to "src/sample/nested/moved.clj"
                  :forms ["helper"]
                  :require_policy "copy-all"
                  :source_hash (:source_hash plan)
                  :caller_changes []
                  :ignored_caller_files []}})]
          (is (:ok result) (pr-str result))
          (is (= [(.getCanonicalPath target-file)]
                 @formatter-files))
          (is (.exists target-file))
          (let [receipt (edn/read-string (slurp (:undo_receipt result)))]
            (is (= [(.getCanonicalPath target-parent)]
                   (:created-directories receipt))))
          (let [undo (extract/undo! {:receipt (:undo_receipt result)})]
            (is (:ok undo) (pr-str undo))
            (is (= original (slurp source-file)))
            (is (not (.exists target-file)))
            (is (not (.exists target-parent))))
          (let [failed
                (mcp-tool/execute-request!
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath receipt-dir)
                   :verification-profiles {"fast" {:commands ["ignored"]}}
                   :verify! (fn [_ _ _ _]
                              {:ok false :profile "fast"
                               :checks [{:ok false :exit 1}]})}
                  {:extraction
                   {:file "src/sample/core.clj"
                    :to "src/sample/nested/moved.clj"
                    :forms ["helper"]
                    :require_policy "copy-all"
                    :source_hash (:source_hash plan)
                    :caller_changes []
                    :ignored_caller_files []}
                   :verify "fast"})]
            (is (false? (:ok failed)))
            (is (= "verification-failed" (:error_type failed)))
            (is (true? (:rolled_back failed)))
            (is (= original (slurp source-file)))
            (is (not (.exists target-file)))
            (is (not (.exists target-parent))))))
      (finally
        (delete-tree! workspace)))))

(deftest direct-extraction-publicizes-a-required-private-form-in-the-same-transaction
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/sample/core.clj")
        target-file (io/file workspace "src/sample/moved.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns sample.core)\n\n"
                      "(defn- helper [x] (inc x))\n\n"
                      "(defn retained [] (helper 1))\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {:extraction
               {:file "src/sample/core.clj"
                :to "src/sample/moved.clj"
                :forms ["helper"]
                :require_policy "minimal"}})]
        (is (:ok result) (pr-str result))
        (is (:verification_complete result))
        (is (.contains ^String (slurp target-file) "(defn helper "))
        (is (.contains ^String (slurp source-file) ":refer [helper]"))
        (is (not (.contains ^String (slurp target-file) "(defn- helper "))))
      (finally
        (delete-tree! workspace)))))

;; This is the smallest genuine-unknown case from the frozen Sessionize
;; extraction: the natural destination alias is already occupied, so one exact
;; candidate still does not authorize the kernel to choose the caller rewrite.
;; @spec MCP-OP-PLAN-009
(deftest omitted-caller-decisions-refuse-before-any-extraction-side-effect
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/cfp_scheduler_killer/views.clj")
        target-file (io/file workspace "src/cfp_scheduler_killer/format.clj")
        caller-file (io/file workspace "src/cfp_scheduler_killer/report.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns cfp-scheduler-killer.views)\n\n"
                      "(defn- not-blank [x] (when (seq x) x))\n\n"
                      "(defn fmt-date [x] (not-blank x))\n\n"
                      "(defn retained [] (not-blank \"x\"))\n")
        caller-original
        (str "(ns cfp-scheduler-killer.report\n"
             "  (:require [clojure.string :as format]\n"
             "            [cfp-scheduler-killer.views :as views]))\n\n"
             "(defn report-date [x]\n"
             "  (format/trim (views/fmt-date x)))\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (spit caller-file caller-original)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :formatter ["must-not-run"]
               :format-candidates!
               (fn [& _] (throw (ex-info "formatter ran" {})))
               :verify!
               (fn [& _] (throw (ex-info "verifier ran" {})))}
              {:extraction
               {:file "src/cfp_scheduler_killer/views.clj"
                :to "src/cfp_scheduler_killer/format.clj"
                :forms ["not-blank" "fmt-date"]
                :require_policy "minimal"}})]
        (is (false? (:ok result)))
        (is (= "extraction-decisions-required" (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result)))
        (is (false? (:write_authority result)))
        (is (= ["src/cfp_scheduler_killer/report.clj"]
               (mapv :file (:genuine_unknowns result))))
        (let [source-hash
              (get-in result [:genuine_unknowns 0 :source_hash])]
          (is (and (string? source-hash)
                   (re-matches #"[0-9a-f]{64}" source-hash))))
        (is (= ["not-blank"]
               (get-in result [:completed_plan :required_public_forms])))
        (is (= ["not-blank"]
               (get-in result [:next_call :extraction :public_forms])))
        (is (= "fill_caller_decisions_then_apply_once"
               (:next_action result)))
        (is (= original (slurp source-file)))
        (is (= caller-original (slurp caller-file)))
        (is (not (.exists target-file)))
        (is (not (.exists receipt-dir))))
      (finally
        (delete-tree! workspace)))))

(deftest direct-change-runs-the-declared-verification-profile
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        calls (atom [])]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)
                      :verification-profiles {"fast" {:commands ["ignored"]}}
                      :verify! (fn [root profile profiles files]
                                 (swap! calls conj
                                        {:root root :profile profile
                                         :profiles profiles :files files})
                                 {:ok true :profile profile :checks []})}
                     (assoc decision-request "verify" "fast"))]
        (is (:ok result))
        (is (= "fast" (get-in result [:verification :profile])))
        (is (= 1 (count @calls)))
        (is (= "fast" (:profile (first @calls))))
        (is (= 2 (count (:files (first @calls)))))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)}))))
      (finally
        (delete-tree! workspace)))))

(deftest exact-terminal-response-is-a-total-evidence-projection
  ;; @spec MCP-OP-RELAY-001
  ;; @spec MCP-OP-RELAY-002
  ;; @spec MCP-OP-RELAY-003
  (let [response "Done — changes committed and exact verification completed."
        sha-a (apply str (repeat 64 "a"))
        sha-b (apply str (repeat 64 "b"))
        sha-c (apply str (repeat 64 "c"))
        eligible {:ok true
                  :operation "apply_clojure_changes"
                  :committed true
                  :verification_complete true
                  :next_action "none"
                  :read_back_hashes {"src/sample/core.clj" sha-a}
                  :undo_receipt "/tmp/receipt.edn"
                  :receipt_hash sha-b
                  :verification {:ok true
                                 :profile "exact"
                                 :profile-source :project
                                 :profile-sha256 sha-c
                                 :acceptance :exact-exit
                                 :process-outcome :pass
                                 :exit 0
                                 :cwd "/tmp/workspace"
                                 :argv ["/usr/bin/clj-kondo" "--lint" "src"]
                                 :elapsed_ms 1.25
                                 :output-bytes 0
                                 :output-sha256 sha-a
                                 :output-truncated false}}]
    (is (= response (mcp-tool/exact-terminal-response eligible)))
    (doseq [[label result]
            [[:non-map nil]
             [:refusal {:ok false :error_type "verification-failed"}]
             [:non-exact (assoc-in eligible [:verification :profile] "fast")]
             [:pending (assoc eligible :verification_complete false)]
             [:next-action (assoc eligible :next_action "inspect_verification_job")]
             [:rolled-back (assoc eligible :rolled_back true)]
             [:hyphenated-rollback (assoc eligible :rolled-back true)]
             [:wrong-operation (assoc eligible :operation "edit_clojure")]
             [:contradictory-error (assoc eligible :error_type "verification-failed")]
             [:contradictory-remedy (assoc eligible :remedy "Retry the request.")]
             [:recovery-required (assoc eligible :recovery_required true)]
             [:next-call (assoc eligible :next_call {:operation "inspect_clojure"})]
             [:missing-read-back (dissoc eligible :read_back_hashes)]
             [:malformed-read-back
              (assoc eligible :read_back_hashes {"src/sample/core.clj" "source-sha"})]
             [:missing-receipt (dissoc eligible :undo_receipt)]
             [:malformed-receipt-hash (assoc eligible :receipt_hash "receipt-sha")]
             [:missing-profile-hash (update eligible :verification dissoc :profile-sha256)]
             [:missing-argv (update eligible :verification dissoc :argv)]
             [:missing-output-hash (update eligible :verification dissoc :output-sha256)]
             [:unverified (assoc-in eligible [:verification :process-outcome]
                                    :launch-failure)]
             [:nonzero (assoc-in eligible [:verification :exit] 3)]]]
      (testing (name label)
        (is (nil? (mcp-tool/exact-terminal-response result)))))
    (let [summary (mcp-tool/concise-summary
                    (assoc eligible
                           :operation "apply_clojure_changes"
                           :edits 15
                           :files 2
                           :elapsed_ms 2.25
                           :terminal_response response))]
      (is (= 1 (count (re-seq (re-pattern
                                (java.util.regex.Pattern/quote response))
                              summary))))
      (is (str/includes? summary "2.25 ms"))
      (is (str/includes?
            summary
            "If this mutation completes all remaining work, return exactly:"))
      (is (str/includes? summary "If work remains, continue.")))
    (is (= {:type "string"}
           (get-in mcp-tool/clj-change-output-schema
                   [:properties "terminal_response"])))
    (is (not (some #{"terminal_response"}
                   (:required mcp-tool/clj-change-output-schema))))))

(deftest exact-terminal-response-is-published-on-both-mcp-surfaces
  ;; @spec MCP-OP-RELAY-001
  ;; @spec MCP-OP-RELAY-003
  (let [response "Done — changes committed and exact verification completed."
        sha-a (apply str (repeat 64 "a"))
        sha-b (apply str (repeat 64 "b"))
        sha-c (apply str (repeat 64 "c"))
        calls (atom [])
        callback (fn [content error? structured]
                   (swap! calls conj {:content (first content)
                                      :payload structured
                                      :error? error?}))
        exact-pass {:ok true
                    :committed true
                    :verification_complete true
                    :next_action "none"
                    :edits 1
                    :files 1
                    :read_back_hashes {"src/sample/core.clj" sha-a}
                    :undo_receipt "/tmp/receipt.edn"
                    :receipt_hash sha-b
                    :verification {:ok true
                                   :profile "exact"
                                   :profile-source :project
                                   :profile-sha256 sha-c
                                   :acceptance :exact-exit
                                   :process-outcome :pass
                                   :exit 0
                                   :cwd "/tmp/workspace"
                                   :argv ["/usr/bin/clj-kondo" "--lint" "src"]
                                   :elapsed_ms 1.25
                                   :output-bytes 0
                                   :output-sha256 sha-a
                                   :output-truncated false}}]
    (try
      (mcp-tool/init! {:project-root "."})
      (with-redefs [mcp-tool/execute-request! (fn [_ _] exact-pass)]
        (mcp-tool/handle-clj-change nil {"changes" []} callback))
      (is (= 1 (count @calls)))
      (is (false? (get-in @calls [0 :error?])))
      (is (= response (get-in @calls [0 :payload :terminal_response])))
      (is (= 1 (count (re-seq (re-pattern
                                (java.util.regex.Pattern/quote response))
                              (get-in @calls [0 :content])))))
      (is (number? (get-in @calls [0 :payload :elapsed_ms])))
      (is (re-find #"When terminal_response is present"
                   mcp-tool/tool-description))
      (finally
        (mcp-tool/init! nil)))))

(deftest exact-verifier-sees-staged-extraction-and-rolls-back-every-nonpass
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-005
  ;; @spec MCP-OP-VERIFY-006
  ;; @spec MCP-OP-VERIFY-007
  ;; @spec MCP-OP-VERIFY-008
  ;; @spec MCP-OP-VERIFY-009
  ;; @spec MCP-OP-VERIFY-010
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/sample/core.clj")
        target-file (io/file workspace "src/sample/moved.clj")
        receipt-dir (io/file workspace "receipts")
        original (str "(ns sample.core)\n\n"
                      "(defn helper [] :ok)\n\n"
                      "(defn retained [] :ok)\n")
        request (fn [source-hash]
                  {:extraction
                   {:file "src/sample/core.clj"
                    :to "src/sample/moved.clj"
                    :forms ["helper"]
                    :require_policy "copy-all"
                    :source_hash source-hash
                    :caller_changes []
                    :ignored_caller_files []}
                   :verify "exact"})
        config (fn [command]
                 {:project-root (.getPath workspace)
                  :receipt-dir (.getPath receipt-dir)
                  :verification-profile-source :project
                  :verification-profiles
                  {"exact" {:acceptance :exact-exit
                            :timeout-ms 120000
                            :commands [command]}}})]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file original)
      (let [plan (extraction-plan/plan!
                   {:project-root (.getPath workspace)}
                   {:mode "plan-extraction"
                    :file "src/sample/core.clj"
                    :to "src/sample/moved.clj"
                    :forms ["helper"]
                    :require_policy "copy-all"})
            passed (mcp-tool/execute-request!
                     (config ["/bin/test" "-s"
                              "src/sample/moved.clj"])
                     (request (:source_hash plan)))]
        (is (:ok passed) (pr-str passed))
        (is (= :exact-exit (get-in passed [:verification :acceptance])))
        (is (= 64 (count (get-in passed
                                 [:verification :profile-sha256]))))
        (is (.exists target-file)
            "the exact verifier passed only if staged target bytes existed")
        (is (:ok (extract/undo! {:receipt (:undo_receipt passed)})))
        (is (.delete (io/file (:undo_receipt passed))))
        (is (= original (slurp source-file)))
        (is (not (.exists target-file)))
        (let [failed (mcp-tool/execute-request!
                       (config ["/usr/bin/false"])
                       (request (:source_hash plan)))]
          (is (false? (:ok failed)))
          (is (= "verification-failed" (:error_type failed)))
          (is (:source_unchanged failed))
          (is (:rolled_back failed))
          (is (= original (slurp source-file)))
          (is (not (.exists target-file)))
          (is (not (re-find #"(?i)retry" (:remedy failed)))))
        (let [unverified (mcp-tool/execute-request!
                           (config ["/definitely/missing-verifier"])
                           (request (:source_hash plan)))]
          (is (false? (:ok unverified)))
          (is (= "verification-unverified" (:error_type unverified)))
          (is (= :launch-failure
                 (get-in unverified [:verification :process-outcome])))
          (is (:source_unchanged unverified))
          (is (:rolled_back unverified))
          (is (= original (slurp source-file)))
          (is (not (.exists target-file)))
          (is (not (re-find #"(?i)retry" (:remedy unverified))))
          (is (empty? (or (seq (.listFiles receipt-dir)) []))
              "a non-pass cannot leave a usable receipt")))
      (finally
        (delete-tree! workspace)))))

(deftest direct-change-returns-after-hot-proof-and-publishes-a-cold-job
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")]
    (try
      (cold-verify/clear-jobs!)
      (copy-tree! (str fixture-root "/before") workspace)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)
                      :verification-profiles
                      {"full" {:cold {:command ["/bin/sh" "-c"
                                                "sleep 0.05; printf cold-proof"]
                                      :timeout-ms 1000}}}}
                     (assoc decision-request "verify" "full"))
            job (get-in result [:next_call :verification_job])
            cold-result
            (loop [attempt 0]
              (let [status (cold-verify/status (.getPath workspace) job)]
                (if (or (:verification_complete status) (>= attempt 100))
                  status
                  (do (Thread/sleep 10) (recur (inc attempt))))))]
        (is (:ok result))
        (is (:committed result))
        (is (false? (:verification_complete result)))
        (is (= "inspect_verification_job" (:next_action result)))
        (is (string? job))
        (is (:ok cold-result))
        (is (:passed cold-result))
        (is (= "cold-proof" (:output cold-result)))
        (is (= (:undo_receipt result) (:undo_receipt cold-result)))
        (is (= (:receipt_hash result) (:receipt_hash cold-result)))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)}))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! workspace)))))

(deftest direct-change-addresses-one-defmethod-dispatch-and-undoes
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/app/render.clj")
        formatter-calls (atom [])
        before (str "(ns app.render)\n"
                    "(defmulti render :kind)\n"
                    "(defmethod render :card [x] [:card :old x])\n"
                    "(defmethod render :panel [x] [:panel :old x])\n")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file before)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :format-candidates!
               (fn [root command future-sources]
                 (swap! formatter-calls conj
                        {:root root :command command
                         :files (vec (keys future-sources))})
                 {:ok true
                  :status :complete
                  :file-count 1
                  :changed-file-count 0
                  :elapsed_ms 0.5
                  :future-sources future-sources})
               :verification-profiles-fn
               (fn [] {"fast" {:commands ["ignored"]}})
               :verify! (fn [_ _ _ _]
                          {:ok true :profile "fast" :checks []})}
              {"changes"
               [{"id" "card-method"
                 "files" ["src/app/render.clj"]
                 "forms" [{"kind" "defmethod"
                           "name" "render"
                           "dispatch" ":card"}]
                 "find" ":old"
                 "replace" ":new"
                 "expect" {"matches" 1 "each_form" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}
               "verify" "fast"})]
        (is (:ok result))
        (is (= (str "(ns app.render)\n"
                    "(defmulti render :kind)\n"
                    "(defmethod render :card [x] [:card :new x])\n"
                    "(defmethod render :panel [x] [:panel :old x])\n")
               (slurp source-file)))
        (is (true? (:verification_complete result)))
        (is (= 1 (count @formatter-calls)))
        (is (= ["npx" "@chrisoakman/standard-clojure-style" "fix" "{files}"]
               (:command (first @formatter-calls))))
        (is (= :complete (get-in result [:format :status])))
        (is (= 0 (get-in result [:format :changed-file-count])))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)})))
        (is (= before (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest direct-change-proves-focused-laws-in-the-configured-application-nrepl
  (let [project-root (System/getProperty "user.dir")
        workspace (io/file project-root (str ".hot-transaction-" (random-uuid)))
        relative-root (.getName workspace)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/app/render.clj")
        port-file (io/file workspace ".app-nrepl-port")
        server (nrepl-server/start-server :bind "127.0.0.1" :port 0)]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file
            (str "(ns app.render)\n"
                 "(defn render [] :old)\n"))
      (spit port-file (:port server))
      (let [result
            (mcp-tool/execute-request!
              {:project-root project-root
               :receipt-dir (.getPath receipt-dir)
               :formatter ["format" "{files}"]
               :format-candidates!
               (fn [_ _ future-sources]
                 {:ok true :status :complete :file-count 1
                  :changed-file-count 0 :elapsed_ms 0.1
                  :future-sources future-sources})
               :verification-profiles
               {"fast"
                {:commands [["/usr/bin/true"]]
                 :hot {:port-file (str relative-root "/.app-nrepl-port")
                       :reload []
                       :tests ["clj-surgeon.mcp-tool-test/hot-transaction-law"]
                       :timeout-ms 5000}}}}
              {"changes"
               [{"id" "render"
                 "files" [(str relative-root "/src/app/render.clj")]
                 "forms" ["render"]
                 "find" ":old"
                 "replace" ":new"
                 "expect" {"matches" 1 "each_form" 1}}]
               "expect" {"changes" 1 "edits" 1 "files" 1}
               "verify" "fast"})]
        (is (:ok result))
        (is (= :complete
               (get-in result [:verification :hot-verification :status])))
        (is (= 1 (get-in result
                         [:verification :hot-verification :summary :test])))
        (is (= "application"
               (get-in result [:verification :hot-verification :jvm])))
        (is (:ok (transaction/execute-undo!
                   {:receipt (:undo_receipt result)}))))
      (finally
        (nrepl-server/stop-server server)
        (delete-tree! workspace)))))

(deftest direct-change-refuses-before-write-when-the-baseline-is-unavailable
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        verification-called? (atom false)]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [before (into {}
                         (for [relative ["src/bench/app_shell.clj"
                                         "src/bench/source_reader.clj"]]
                           [relative (slurp (io/file workspace relative))]))
            result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)
                      :verification-profiles {"fast" {:commands ["ignored"]}}
                      :capture-verification-baseline!
                      (fn [_ _ _ _]
                        {:ok false
                         :error-type :invalid-diagnostic-output})
                      :verify! (fn [_ _ _ _]
                                 (reset! verification-called? true)
                                 {:ok true})}
                     (assoc decision-request "verify" "fast"))]
        (is (false? (:ok result)))
        (is (= "verification-baseline-failed" (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (false? @verification-called?))
        (doseq [[relative source] before]
          (is (= source (slurp (io/file workspace relative)))))
        (is (or (not (.exists receipt-dir))
                (empty? (seq (.listFiles receipt-dir))))))
      (finally
        (delete-tree! workspace)))))

(deftest direct-change-rolls-back-when-declared-verification-fails
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (let [result (mcp-tool/execute-request!
                     {:project-root (.getPath workspace)
                      :receipt-dir (.getPath receipt-dir)
                      :verification-profiles {"fast" {:commands ["ignored"]}}
                      :verify! (fn [_ _ _ _]
                                 {:ok false :profile "fast"
                                  :checks [{:ok false :exit 1}]})}
                     (assoc decision-request "verify" "fast"))]
        (is (false? (:ok result)))
        (is (= "verification-failed" (:error_type result)))
        (is (true? (:rolled_back result)))
        (doseq [relative ["src/bench/app_shell.clj"
                          "src/bench/source_reader.clj"]]
          (is (= (slurp (io/file fixture-root "before" relative))
                 (slurp (io/file workspace relative))))))
      (finally
        (delete-tree! workspace)))))

(deftest direct-change-accepts-only-the-diagnostic-delta
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source (io/file workspace "src/sample/core.clj")
        original (str "(ns sample.core)\n"
                      "(defn target [] {:status :old :value missing})\n")
        profiles {"fast" {:commands [["clj-kondo" "--lint" "{files}"]]}}
        request (fn [replacement]
                  {"changes"
                   [{"id" "status"
                     "files" ["src/sample/core.clj"]
                     "forms" ["target"]
                     "find" ":old"
                     "replace" replacement
                     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                   "expect" {"changes" 1 "edits" 1 "files" 1}
                   "verify" "fast"})]
    (try
      (.mkdirs (.getParentFile source))
      (spit source original)
      (testing "a retained pre-existing diagnostic does not block the change"
        (let [result (mcp-tool/execute-request!
                       {:project-root (.getPath workspace)
                        :receipt-dir (.getPath receipt-dir)
                        :verification-profiles profiles}
                       (request ":new"))]
          (is (:ok result))
          (is (= 1 (get-in result
                           [:verification :checks 0 :diagnostic-delta
                            :unchanged-count])))
          (is (zero? (get-in result
                             [:verification :checks 0 :diagnostic-delta
                              :blocking-introduced-count])))
          (is (:ok (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})))
          (is (= original (slurp source)))))

      (testing "one new diagnostic rolls the complete change back"
        (let [result (mcp-tool/execute-request!
                       {:project-root (.getPath workspace)
                        :receipt-dir (.getPath receipt-dir)
                        :verification-profiles profiles}
                       (request "another-missing"))]
          (is (false? (:ok result)))
          (is (= "verification-failed" (:error_type result)))
          (is (true? (:rolled_back result)))
          (is (= 1 (get-in result
                           [:verification :checks 0 :diagnostic-delta
                            :blocking-introduced-count])))
          (is (= original (slurp source)))))
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
  (let [decision-schema (get-in mcp-schema/basis-change-schema
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
      (is (= "edit_clojure"
             (mcp-tool/request-operation {"delete_owners" []})))
      (is (= "edit_clojure"
             (mcp-tool/request-operation {:programs []})))
      (is (= "apply_clojure_changes"
             (mcp-tool/request-operation {"changes" []})))
      (copy-tree! (str fixture-root "/before") workspace)
      (mcp-tool/init! {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)
                       :verification-profiles {"fast" {:commands ["ignored"]}}
                       :verify! (fn [_ profile _ _]
                                  {:ok true :profile profile :checks []})})
      (mcp-tool/handle-clj-change nil decision-request callback)
      (testing "the direct route steers named forms away from namespace owner syntax"
        (is (re-find #"For named top-level def or defn owners, use forms: \[name\]"
                     mcp-tool/tool-description))
        (is (re-find #"never pass owner as a string"
                     mcp-tool/tool-description)))
      (is (= false (:error? (first @calls))))
      (is (= true (get-in @calls [0 :payload :verification_complete])))
      (let [elapsed (get-in @calls [0 :payload :elapsed_ms])]
        (is (number? elapsed))
        (when (number? elapsed)
          (is (<= 0 elapsed))
          (is (= (str "apply_clojure_changes\n"
                      (format "  6 edits · 2 files · %.2f ms\n\n" elapsed)
                      "✓ atomic commit complete\n"
                      "✓ written bytes read back and verified\n"
                      "✓ terminal evidence · verification_complete=true · next action none")
                 (get-in @calls [0 :content])))))
      (mcp-tool/handle-clj-change nil
                                  (assoc decision-request "unexpected" true)
                                  callback)
      (is (= true (:error? (second @calls))))
      (is (= "invalid-mcp-request"
             (get-in @calls [1 :payload :error_type])))
      (is (number? (get-in @calls [1 :payload :elapsed_ms])))
      (when-let [elapsed (get-in @calls [1 :payload :elapsed_ms])]
        (is (str/includes?
              (get-in @calls [1 :content])
              (format "%.2f ms" elapsed))))
      (is (re-find #"refused · unknown-fields at \[\]"
                   (get-in @calls [1 :content])))
      (is (not (re-find #"\{\"ok\""
                        (get-in @calls [1 :content]))))
      (testing "the handler accepts and executes the same verify field it advertises"
        (is (:ok (transaction/execute-undo!
                   {:receipt (get-in @calls [0 :payload :undo_receipt])})))
        (mcp-tool/handle-clj-change nil
                                    (assoc decision-request "verify" "fast")
                                    callback)
        (is (= false (:error? (nth @calls 2))))
        (is (= true (get-in @calls [2 :payload :verification_complete])))
        (is (= "fast" (get-in @calls [2 :payload :verification :profile]))))
      (testing "refusal summaries preserve the actionable diagnostic"
        (is (str/starts-with?
              (mcp-tool/concise-summary
                {:ok true
                 :operation "edit_clojure"
                 :edits 2
                 :files 1
                 :elapsed_ms 1.25
                 :verification_complete true})
              "edit_clojure\n"))
        (testing "keyword refusal types and rollback state remain truthful"
          (is (re-find #"refused · verification-failed"
                       (mcp-tool/concise-summary
                         {:ok false
                          :error-type :verification-failed
                          :elapsed_ms 1.25
                          :rolled-back true})))
          (is (re-find #"source state requires structured receipt review"
                       (mcp-tool/concise-summary
                         {:ok false
                          :error-type :verification-failed
                          :elapsed_ms 1.25
                          :rolled-back false}))))
        (testing "the exact failed change and field stay visible"
          (is (= (str "apply_clojure_changes\n"
                      "  refused · invalid-intent-form · 2.50 ms\n"
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
                    :elapsed_ms 2.5
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
