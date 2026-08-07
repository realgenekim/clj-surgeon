(ns clj-surgeon.mcp-tool-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]])
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

(deftest callback-uses-mcp-success-and-error-channels
  (let [workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        calls (atom [])
        callback (fn [content error?]
                   (swap! calls conj {:payload (json/parse-string (first content))
                                      :error? error?}))]
    (try
      (copy-tree! (str fixture-root "/before") workspace)
      (mcp-tool/init! {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)})
      (mcp-tool/handle-clj-change nil decision-request callback)
      (is (= false (:error? (first @calls))))
      (is (= true (get-in @calls [0 :payload "verification_complete"])))
      (mcp-tool/handle-clj-change nil
                                  (assoc decision-request "unexpected" true)
                                  callback)
      (is (= true (:error? (second @calls))))
      (is (= "invalid-mcp-request"
             (get-in @calls [1 :payload "error_type"])))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))
