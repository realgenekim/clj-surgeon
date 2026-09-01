(ns clj-surgeon.mcp-create-files-test
  "Characterization contract for the create_files transaction verb.

  create_files creates absent Clojure or EDN files inside the same frozen
  edit_clojure transaction that carries edits, programs, and delete_owners.
  Every guard refuses the whole transaction and names the offending path."
  ;; @spec MCP-OP-EDIT-031
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-create-files-test-"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- config
  [workspace]
  {:project-root (.getPath workspace)
   :receipt-dir (.getPath (io/file workspace "receipts"))})

(defn- write-source!
  [workspace relative source]
  (let [target (io/file workspace relative)]
    (.mkdirs (.getParentFile target))
    (spit target source)
    target))

;; ---------------------------------------------------------------------------
;; Create-only transactions
;; ---------------------------------------------------------------------------

(deftest create-only-transaction-commits-one-clojure-file
  (let [workspace (temp-dir)
        content "(ns demo.fresh)\n\n(defn greet [] :hello)\n"]
    (try
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"create_files"
                      [{"file" "src/demo/fresh.clj" "content" content}]})]
        (is (:ok result) (pr-str result))
        (is (= 1 (:created result)))
        (is (contains? (:read_back_hashes result) "src/demo/fresh.clj"))
        (is (= (structural-lens/source-hash content)
               (get (:read_back_hashes result) "src/demo/fresh.clj")))
        (is (= content (slurp (io/file workspace "src/demo/fresh.clj")))
            "created content is written verbatim")
        (is (string? (:undo_receipt result))))
      (finally
        (delete-tree! workspace)))))

(deftest create-only-transaction-commits-one-edn-file
  (let [workspace (temp-dir)
        content "{:paths [\"src\"]}\n"]
    (try
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"create_files"
                      [{"file" "resources/config.edn" "content" content}]})]
        (is (:ok result) (pr-str result))
        (is (= 1 (:created result)))
        (is (= content (slurp (io/file workspace "resources/config.edn")))))
      (finally
        (delete-tree! workspace)))))

(deftest create-only-transaction-creates-missing-parent-directories
  (let [workspace (temp-dir)
        content "(ns demo.deep.nested)\n"]
    (try
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"create_files"
                      [{"file" "src/demo/deep/nested.clj" "content" content}]})]
        (is (:ok result) (pr-str result))
        (is (= content (slurp (io/file workspace "src/demo/deep/nested.clj"))))
        (let [undo (transaction/execute-undo!
                     {:receipt (:undo_receipt result)})]
          (is (:ok undo) (pr-str undo))
          (is (not (.exists (io/file workspace "src/demo/deep/nested.clj"))))
          (is (not (.exists (io/file workspace "src/demo/deep")))
              "undo removes the directories the transaction created")))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; Mixed create + edit atomicity
;; ---------------------------------------------------------------------------

(deftest create-and-edit-commit-in-one-transaction
  (let [workspace (temp-dir)
        before "(ns demo)\n\n(defn route [] :done)\n"
        after "(ns demo)\n\n(defn route [] :complete)\n"
        created "(ns demo.helper)\n\n(defn help [] :ok)\n"]
    (try
      (write-source! workspace "src/demo.clj" before)
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"edits" [{"file" "src/demo.clj"
                                "within" {"form" "route"}
                                "from" ":done"
                                "to" ":complete"}]
                      "create_files"
                      [{"file" "src/demo/helper.clj" "content" created}]})]
        (is (:ok result) (pr-str result))
        (is (= 1 (:edits result)))
        (is (= 1 (:created result)))
        (is (= after (slurp (io/file workspace "src/demo.clj"))))
        (is (= created (slurp (io/file workspace "src/demo/helper.clj"))))
        (is (= #{"src/demo.clj" "src/demo/helper.clj"}
               (set (keys (:read_back_hashes result))))
            "read_back_hashes covers edited and created files alike")
        (testing "undo restores the edit and deletes the creation"
          (let [undo (transaction/execute-undo!
                       {:receipt (:undo_receipt result)})]
            (is (:ok undo) (pr-str undo))
            (is (= before (slurp (io/file workspace "src/demo.clj"))))
            (is (not (.exists (io/file workspace "src/demo/helper.clj")))))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; Guards — each refuses the whole transaction and names the path
;; ---------------------------------------------------------------------------

(deftest existing-target-refuses-the-whole-transaction
  (let [workspace (temp-dir)
        before "(ns demo)\n\n(defn route [] :done)\n"
        occupied "(ns demo.helper)\n"]
    (try
      (write-source! workspace "src/demo.clj" before)
      (write-source! workspace "src/demo/helper.clj" occupied)
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"edits" [{"file" "src/demo.clj"
                                "within" {"form" "route"}
                                "from" ":done"
                                "to" ":complete"}]
                      "create_files"
                      [{"file" "src/demo/helper.clj"
                        "content" "(ns demo.helper)\n(defn help [] :ok)\n"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (= "target-already-exists" (:error_type result)))
        (is (= "src/demo/helper.clj" (:path result))
            "the refusal names the offending path")
        (is (:source_unchanged result))
        (is (= before (slurp (io/file workspace "src/demo.clj")))
            "the sibling edit was not applied")
        (is (= occupied (slurp (io/file workspace "src/demo/helper.clj")))))
      (finally
        (delete-tree! workspace)))))

(deftest unparsable-content-refuses-with-nothing-written
  (let [workspace (temp-dir)
        before "(ns demo)\n\n(defn route [] :done)\n"]
    (try
      (write-source! workspace "src/demo.clj" before)
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"edits" [{"file" "src/demo.clj"
                                "within" {"form" "route"}
                                "from" ":done"
                                "to" ":complete"}]
                      "create_files"
                      [{"file" "src/demo/broken.clj"
                        "content" "(ns demo.broken\n(defn oops [] :x)\n"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (= "invalid-created-source" (:error_type result)))
        (is (= "src/demo/broken.clj" (:path result)))
        (is (:source_unchanged result))
        (is (not (.exists (io/file workspace "src/demo/broken.clj"))))
        (is (= before (slurp (io/file workspace "src/demo.clj")))))
      (finally
        (delete-tree! workspace)))))

(deftest sibling-edit-guard-failure-leaves-no-created-file
  (let [workspace (temp-dir)
        before "(ns demo)\n\n(defn route [] :done)\n"]
    (try
      (write-source! workspace "src/demo.clj" before)
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"edits" [{"file" "src/demo.clj"
                                "within" {"form" "route"}
                                "from" ":absent-literal"
                                "to" ":complete"}]
                      "create_files"
                      [{"file" "src/demo/helper.clj"
                        "content" "(ns demo.helper)\n"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (:source_unchanged result))
        (is (= before (slurp (io/file workspace "src/demo.clj"))))
        (is (not (.exists (io/file workspace "src/demo/helper.clj")))
            "a failed sibling edit leaves no created file behind")
        (is (not (.exists (io/file workspace "src/demo")))
            "and leaves no directory it would have created"))
      (finally
        (delete-tree! workspace)))))

(deftest absolute-and-traversal-paths-refuse
  (let [workspace (temp-dir)]
    (try
      (doseq [path ["/etc/evil.clj" "../escape.clj" "src/../../escape.clj"]]
        (let [result (mcp-tool/execute-request!
                       (config workspace)
                       {"create_files"
                        [{"file" path "content" "(ns evil)\n"}]})]
          (is (false? (:ok result)) (str path " -> " (pr-str result)))
          (is (= "invalid-relative-source-path" (:error_type result))
              (str path " -> " (pr-str result)))
          (is (= path (:path result)) (pr-str result))))
      (finally
        (delete-tree! workspace)))))

(deftest unsupported-extension-refuses
  (let [workspace (temp-dir)]
    (try
      (doseq [path ["src/notes.txt" "README.md" "src/demo"]]
        (let [result (mcp-tool/execute-request!
                       (config workspace)
                       {"create_files"
                        [{"file" path "content" "(ns demo)\n"}]})]
          (is (false? (:ok result)) (str path " -> " (pr-str result)))
          (is (= "invalid-relative-source-path" (:error_type result))
              (str path " -> " (pr-str result)))))
      (finally
        (delete-tree! workspace)))))

(deftest duplicate-create-paths-refuse
  (let [workspace (temp-dir)]
    (try
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"create_files"
                      [{"file" "src/demo.clj" "content" "(ns demo)\n"}
                       {"file" "src/demo.clj" "content" "(ns demo)\n"}]})]
        (is (false? (:ok result)) (pr-str result))
        (is (:source_unchanged result))
        (is (not (.exists (io/file workspace "src/demo.clj")))))
      (finally
        (delete-tree! workspace)))))

(deftest empty-transaction-refuses
  (let [workspace (temp-dir)]
    (try
      (let [result (mcp-tool/execute-request! (config workspace) {})]
        (is (false? (:ok result)) (pr-str result)))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; Receipt evidence
;; ---------------------------------------------------------------------------

(deftest receipt-records-every-creation-for-undo
  (let [workspace (temp-dir)
        content "(ns demo.recorded)\n"]
    (try
      (let [result (mcp-tool/execute-request!
                     (config workspace)
                     {"create_files"
                      [{"file" "src/demo/recorded.clj" "content" content}]})
            _ (is (:ok result) (pr-str result))
            receipt (edn/read-string (slurp (:undo_receipt result)))
            created (:created-files receipt)]
        (is (vector? created))
        (is (= 1 (count created)))
        (is (= (structural-lens/source-hash content)
               (:result-hash (first created))))
        (is (.endsWith ^String (:file (first created))
                       "src/demo/recorded.clj"))
        (is (= (:receipt_hash result) (:receipt-hash receipt))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; Kernel-level rollback: a write failure on a created file restores everything
;; ---------------------------------------------------------------------------

(deftest commit-rollback-deletes-created-files-when-a-write-fails
  (let [workspace (temp-dir)
        source-file (io/file workspace "src/demo.clj")
        created-file (io/file workspace "src/demo/helper.clj")
        before "(ns demo)\n\n(defn route [] :done)\n"
        after "(ns demo)\n\n(defn route [] :complete)\n"
        created "(ns demo.helper)\n"]
    (try
      (write-source! workspace "src/demo.clj" before)
      (.mkdirs (io/file workspace "src/demo"))
      (let [compiled
            (transaction/compile-transaction
              {(.getPath source-file) before}
              {:changes [{:id :route
                          :in [(.getPath source-file)]
                          :find ":done"
                          :do [:replace ":complete"]
                          :expect {:matches 1}}]
               :expect {:changes 1 :edits 1 :files 1}
               :create-files [{:file (.getPath created-file)
                               :content created}]})
            _ (is (:ok compiled) (pr-str compiled))
            commit (transaction/commit-compiled!
                     compiled
                     {:read-source slurp
                      :write-source!
                      (fn [file source]
                        (if (= file (.getPath created-file))
                          (throw (ex-info "injected creation failure" {}))
                          (spit file source)))})]
        (is (:error commit) (pr-str commit))
        (is (true? (:rolled-back commit)) (pr-str commit))
        (is (= before (slurp source-file))
            "the sibling edit was rolled back")
        (is (not (.exists created-file))
            "a partially committed creation is removed")
        (is (not= after (slurp source-file))))
      (finally
        (delete-tree! workspace)))))
(deftest create-bearing-receipts-suppress-edit-only-canonical-identity
  ;; @spec MCP-OP-EDIT-033
  (let [root-a (temp-dir)
        root-b (temp-dir)]
    (try
      (doseq [root [root-a root-b]]
        (let [source-file (io/file root "src/demo.clj")]
          (io/make-parents source-file)
          (spit source-file "(ns demo)\n(def value :old)\n")))
      (let [request-a {"edits" [{"file" "src/demo.clj"
                                 "within" {"form" "value"}
                                 "from" ":old"
                                 "to" ":new"}]
                       "create_files" [{"file" "src/created_a.clj"
                                        "content" "(ns created-a)\n"}]}
            request-b {"edits" [{"file" "src/demo.clj"
                                 "within" {"form" "value"}
                                 "from" ":old"
                                 "to" ":new"}]
                       "create_files" [{"file" "src/created_b.clj"
                                        "content" "(ns created-b)\n(def different true)\n"}]}
            result-a (mcp-tool/execute-request! (config root-a) request-a)
            result-b (mcp-tool/execute-request! (config root-b) request-b)]
        (is (:ok result-a))
        (is (:ok result-b))
        (is (nil? (:canonical_effect_identity result-a)))
        (is (nil? (:canonical_effect_identity result-b)))
        (is (= "create-files-present"
               (:canonical_effect_identity_suppressed_reason result-a)))
        (is (= "create-files-present"
               (:canonical_effect_identity_suppressed_reason result-b))))
      (finally
        (delete-tree! root-a)
        (delete-tree! root-b)))))

(deftest receipt-publication-failure-rolls-back-create-only-and-mixed-effects
  ;; @spec MCP-OP-EDIT-034
  (let [publish-var (ns-resolve 'clj-surgeon.intent-transaction
                                'publish-staged-receipt!)]
    (doseq [mode [:create-only :mixed]]
      (testing (name mode)
        (let [root (temp-dir)
              source-file (io/file root "src/demo.clj")
              source-dir (.getParentFile source-file)
              created-file (io/file root "src/generated/helper.clj")
              created-dir (.getParentFile created-file)
              receipt-file (io/file root "receipt.edn")
              original "(ns demo)\n(def value :old)\n"
              intents (if (= mode :mixed)
                        [{:files [(.getPath source-file)]
                          :from ":old"
                          :to ":new"
                          :expect-count 1}]
                        [])
              change-spec {:intents intents
                           :expect {:intent-count (count intents)
                                    :edit-count (count intents)
                                    :changed-file-count (count intents)}
                           :create-files [{:file (.getPath created-file)
                                           :content "(ns generated.helper)\n"
                                           :directories [(.getPath source-dir)
                                                         (.getPath created-dir)]}]}]
          (try
            (when (= mode :mixed)
              (io/make-parents source-file)
              (spit source-file original))
            (let [result (with-redefs-fn
                           {publish-var
                            (fn [& _]
                              (throw (ex-info "forced receipt publication failure" {})))}
                           #(transaction/execute-change!
                              {:spec change-spec
                               :receipt-out (.getPath receipt-file)}))]
              (is (= :receipt-write-failed (:error-type result)))
              (is (true? (:rolled-back result)))
              (is (true? (get-in result [:recovery :ok])))
              (is (not= :transaction-recovery-required (:error-type result)))
              (is (not (.exists created-file)))
              (is (not (.exists created-dir)))
              (when (= mode :mixed)
                (is (= original (slurp source-file)))))
            (finally
              (delete-tree! root))))))))

(deftest atomic-create-refuses-a-file-that-appears-at-publication
  ;; @spec MCP-OP-EDIT-035
  (let [root (temp-dir)
        target (io/file root "src/foreign.clj")
        atomic-create-var (or (ns-resolve 'clj-surgeon.file-ops 'atomic-create!)
                              (ns-resolve 'clj-surgeon.file-ops 'atomic-write!))
        atomic-create @atomic-create-var
        foreign-bytes "foreign-owner-bytes\n"]
    (try
      (let [result (with-redefs-fn
                     {atomic-create-var
                      (fn [file source]
                        (io/make-parents file)
                        (spit file foreign-bytes)
                        (atomic-create file source))}
                     #(mcp-tool/execute-request!
                        (config root)
                        {"create_files" [{"file" "src/foreign.clj"
                                          "content" "(ns should-not-win)\n"}]}))]
        (is (false? (:ok result)))
        (is (= foreign-bytes (slurp target))))
      (finally
        (delete-tree! root)))))

(deftest commit-rejects-a-planned-parent-replaced-by-a-symlink
  ;; @spec MCP-OP-EDIT-036
  (let [root (temp-dir)
        outside-root (temp-dir)
        source-file (io/file root "src/demo.clj")
        planned-parent (io/file root "src/late")
        outside-file (io/file outside-root "escaped.clj")
        original "(ns demo)\n(def value :old)\n"
        attrs (make-array FileAttribute 0)]
    (try
      (io/make-parents source-file)
      (spit source-file original)
      (let [result (mcp-tool/execute-request!
                     (assoc (config root)
                            :prepare-compiled!
                            (fn [_ compiled]
                              (Files/createSymbolicLink
                                (.toPath planned-parent)
                                (.toPath outside-root)
                                attrs)
                              compiled))
                     {"edits" [{"file" "src/demo.clj"
                                "within" {"form" "value"}
                                "from" ":old"
                                "to" ":new"}]
                      "create_files" [{"file" "src/late/escaped.clj"
                                       "content" "(ns escaped)\n"}]})]
        (is (false? (:ok result)))
        (is (= original (slurp source-file)))
        (is (not (.exists outside-file))))
      (finally
        (Files/deleteIfExists (.toPath planned-parent))
        (delete-tree! root)
        (delete-tree! outside-root)))))
