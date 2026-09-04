(ns ^{:lane :fast} clj-surgeon.mcp-write-refusal-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(defn- optional-call
  [symbol & args]
  (try
    (if-let [f (requiring-resolve symbol)]
      (apply f args)
      {})
    (catch Exception _
      {})))

(defn- source-hashes
  [sources]
  (into {} (map (fn [[file source]]
                  [file (structural-lens/source-hash source)]))
        sources))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (io/delete-file child true))))

(defn- fresh-workspace
  []
  (.toFile (java.nio.file.Files/createTempDirectory
             "clj-surgeon-write-refusal"
             (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- raw-form-facts
  []
  (let [sources
        {"src/a.clj"
         (str "(ns app.a)\n"
              "(defn one [] [:old :old])\n"
              "(defn two [] :old)\n")}
        items
        [{:file "src/a.clj" :owner 'one :line 2 :end-line 2
          :address {:preorder 8}}
         {:file "src/a.clj" :owner 'one :line 2 :end-line 2
          :address {:preorder 9}}
         {:file "src/a.clj" :owner 'two :line 3 :end-line 3
          :address {:preorder 14}}]]
    {:operation "edit_clojure"
     :change-index 0
     :change-id :count-old
     :files ["src/a.clj"]
     :scope {:kind :form :forms ['one 'two]}
     :matcher {:from ":old"}
     :expectation {:matches 4}
     :expected-count 4
     :actual-count 3
     :per-file-counts {"src/a.clj" 3}
     :per-form-counts {"src/a.clj" {'one 2 'two 1}}
     :items items
     :snapshot-guards (source-hashes sources)}))

(deftest generic-count-mismatch-projects-complete-form-evidence
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [evidence
        (optional-call
          'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
          (raw-form-facts))]
    (is (= 1 (:version evidence)))
    (is (= :generic-count-mismatch (:family evidence)))
    (is (= :intent-compilation (:failed-stage evidence)))
    (is (= {:change-index 0
            :change-id :count-old}
           (select-keys (:subject evidence) [:change-index :change-id])))
    (is (let [selector (get-in evidence [:subject :selector-sha256])]
          (and (string? selector)
               (re-matches #"[0-9a-f]{64}" selector))))
    (is (= {:expected-count 4
            :actual-count 3
            :per-file-counts {"src/a.clj" 3}
            :per-form-counts {"src/a.clj" {'one 2 'two 1}}}
           (select-keys evidence
                        [:expected-count :actual-count
                         :per-file-counts :per-form-counts])))
    (is (= [{:file "src/a.clj" :scope-kind :form
             :owner-kind :form :owner-name 'one
             :line 2 :end-line 2 :address {:preorder 8}}
            {:file "src/a.clj" :scope-kind :form
             :owner-kind :form :owner-name 'one
             :line 2 :end-line 2 :address {:preorder 9}}
            {:file "src/a.clj" :scope-kind :form
             :owner-kind :form :owner-name 'two
             :line 3 :end-line 3 :address {:preorder 14}}]
           (:items evidence)))
    (is (= 3 (:available-count evidence)))
    (is (= false (:authority evidence)))
    (is (= false (:write-authority evidence)))
    (is (= (:snapshot-guards (raw-form-facts))
           (:snapshot-guards evidence)))))

(deftest closed-scope-identities-never-invent-an-owner
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [root
        (optional-call
          'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
          (assoc (raw-form-facts)
                 :scope {:kind :root}
                 :per-form-counts nil
                 :items [{:file "src/a.clj" :owner nil :line 2 :end-line 2
                          :address {:preorder 8}}]))
        namespace
        (optional-call
          'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
          (assoc (raw-form-facts)
                 :scope {:kind :namespace :name 'app.a}
                 :per-form-counts nil
                 :items [{:file "src/a.clj" :owner 'app.a :line 1 :end-line 1
                          :address {:preorder 0}}]))]
    (is (= :root (get-in root [:items 0 :scope-kind])))
    (is (not (contains? (first (:items root)) :owner-kind)))
    (is (not (contains? (first (:items root)) :owner-name)))
    (is (not (contains? root :per-form-counts)))
    (is (= {:scope-kind :namespace
            :owner-kind :namespace
            :owner-name 'app.a}
           (select-keys (first (:items namespace))
                        [:scope-kind :owner-kind :owner-name])))))

(deftest bounded-public-refusal-is-complete-or-honestly-truncated
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [items (mapv (fn [index]
                      {:file "src/a.clj" :scope-kind :root
                       :line 2 :end-line 2 :address {:preorder index}})
                    (range 129))
        evidence (assoc
                   (optional-call
                     'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
                     (assoc (raw-form-facts)
                            :scope {:kind :root}
                            :per-form-counts nil
                            :actual-count 129
                            :per-file-counts {"src/a.clj" 129}
                            :items items))
                   :items items
                   :available-count 129)
        result {:ok false
                :operation "edit_clojure"
                :error_type "expect-count-mismatch"
                :source_unchanged true
                :write_refusal_evidence evidence}
        projected
        (optional-call
          'clj-surgeon.mcp-write-refusal/bound-public-refusal
          result
          (constantly "bounded refusal"))
        continuation (get-in projected
                             [:write_refusal_evidence :candidate_continuation])]
    (is (= 128 (get-in projected [:write_refusal_evidence :returned_count])))
    (is (= 1 (get-in projected [:write_refusal_evidence :omitted_count])))
    (is (= true (get-in projected [:write_refusal_evidence :truncated])))
    (is (= 128 (count (get-in projected [:write_refusal_evidence :items]))))
    (is (= false (:executable continuation)))
    (is (= false (:authority continuation)))
    (is (= false (:write_authority continuation)))
    (is (= 128 (:next_offset continuation)))
    (is (= 1 (:remaining_count continuation)))
    (is (let [query (:candidate_query_sha256 continuation)]
          (and (string? query)
               (re-matches #"[0-9a-f]{64}" query))))))

(deftest output-budget-fails-empty-without-dynamic-authority
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [evidence
        (optional-call
          'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
          (assoc (raw-form-facts)
                 :change-id (apply str (repeat 40000 "x"))))
        projected
        (optional-call
          'clj-surgeon.mcp-write-refusal/bound-public-refusal
          {:ok false
           :operation "edit_clojure"
           :error_type "expect-count-mismatch"
           :error (apply str (repeat 40000 "x"))
           :source_unchanged true
           :write_refusal_evidence evidence}
          (constantly "bounded refusal"))
        encoded (when (map? projected)
                  (count (.getBytes (json/generate-string projected) "UTF-8")))]
    (is (= "output-budget" (:write_refusal_evidence_omitted projected)))
    (is (= :intent-compilation (:failed_stage projected)))
    (is (= false (:write_authority projected)))
    (is (= false (:mutation_attempted projected)))
    (is (not (contains? projected :write_refusal_evidence)))
    (is (not (contains? projected :error)))
    (is (<= encoded 32640))))

(deftest public-count-refusal-carries-inert-complete-evidence
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [workspace (fresh-workspace)
        source-file (io/file workspace "src/a.clj")
        receipt-dir (io/file workspace "receipts")
        source
        (str "(ns app.a)\n"
             "(defn one [] [:old :old])\n"
             "(defn two [] :old)\n")]
    (try
      (io/make-parents source-file)
      (spit source-file source)
      (let [result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              {"changes"
               [{"id" "count-old"
                 "files" ["src/a.clj"]
                 "forms" ["one" "two"]
                 "find" ":old"
                 "replace" ":new"
                 "expect" {"matches" 4}}]})
            evidence (:write_refusal_evidence result)]
        (is (= false (:ok result)))
        (is (= "expect-count-mismatch" (:error_type result)))
        (is (= true (:source_unchanged result)))
        (is (= 4 (:expected_count result)))
        (is (= 3 (:actual_count result)))
        (is (= {(.getCanonicalPath source-file) 3} (:per_file_counts result)))
        (is (= {"src/a.clj" {"one" 2 "two" 1}}
               (:per_form_counts result)))
        (is (= 3 (:available_count evidence)))
        (is (= 3 (:returned_count evidence)))
        (is (= 0 (:omitted_count evidence)))
        (is (= false (:truncated evidence)))
        (is (= false (:authority evidence)))
        (is (= false (:write_authority evidence)))
        (is (not (contains? result :next_call)))
        (is (not (contains? result :prepared_request)))
        (is (= source (slurp source-file))))
      (finally
        (delete-tree! workspace)))))

(deftest zero-match-evidence-is-a-complete-empty-investigation
  ;; @spec MCP-OP-WRITE-REFUSAL-001
  (let [evidence
        (optional-call
          'clj-surgeon.mcp-write-refusal/generic-count-mismatch-evidence
          (assoc (raw-form-facts)
                 :actual-count 0
                 :per-file-counts {"src/a.clj" 0}
                 :per-form-counts {"src/a.clj" {'one 0 'two 0}}
                 :items []))]
    (is (= 0 (:available-count evidence)))
    (is (= [] (:items evidence)))
    (is (= false (:truncated evidence)))
    (is (= false (:authority evidence)))
    (is (= false (:write-authority evidence)))))
