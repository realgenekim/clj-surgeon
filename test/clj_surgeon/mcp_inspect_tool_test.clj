(ns clj-surgeon.mcp-inspect-tool-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-inspect-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- write-source!
  [root relative source]
  (let [file (io/file root relative)]
    (.mkdirs (.getParentFile file))
    (spit file source)
    file))

(defn- tree-state
  [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (map (fn [file]
              [(.toString (.relativize (.toPath (io/file root))
                                       (.toPath file)))
               (slurp file)]))
       (into (sorted-map))))

(deftest reads-one-coherent-snapshot-once-for-several-requests
  (let [project (temp-dir)
        source-file (write-source!
                      project "src/demo.clj"
                      "(ns demo)\n(def alpha :old)\n(def beta :stable)\n")
        reads (atom 0)
        request
        {"requests"
         [{"id" "forms" "operation" "forms" "file" "src/demo.clj"
           "forms" ["alpha" "beta"] "expect" {"forms" 2}}
          {"id" "outline" "operation" "outline" "file" "src/demo.clj"}
          {"id" "match" "operation" "match" "file" "src/demo.clj"
           "match" ":old" "expect" {"matches" 1}}]
         "expect" {"requests" 3 "files" 1}}]
    (try
      (let [result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source
               (fn [path]
                 (swap! reads inc)
                 (let [captured (slurp path)]
                   (spit source-file
                         "(ns demo)\n(def alpha :new)\n(def beta :changed)\n")
                   captured))}
              request)]
        (is (:ok result))
        (is (= 1 @reads))
        (is (= 1 (:file_read_count result)))
        (is (= "(def alpha :old)"
               (get-in result [:results 0 :forms 0 :source])))
        (is (= 1 (get-in result [:results 2 :match_count])))
        (is (= ":old" (get-in result [:results 2 :matches 0 :source])))
        (is (= (get-in result [:file_hashes "src/demo.clj"])
               (get-in result [:results 0 :file_hash]))) (is (= {:file "src/demo.clj"
                                                                 :source_sha256 (get-in result [:file_hashes "src/demo.clj"])
                                                                 :owner "alpha"
                                                                 :range {:start {:line 1 :character 0}
                                                                         :end {:line 1 :character 16}}
                                                                 :selection_range {:start {:line 1 :character 5}
                                                                                   :end {:line 1 :character 10}}}
                                                                (get-in result [:results 0 :forms 0 :source_anchor]))))
      (finally
        (delete-tree! project)))))

(deftest routes-one-shared-inspector-to-the-requested-canonical-workspace
  (let [default-root (temp-dir)
        requested-root (temp-dir)
        _ (write-source! default-root "src/demo.clj"
                         "(ns demo)\n(def marker :default)\n")
        _ (write-source! requested-root "src/demo.clj"
                         "(ns demo)\n(def marker :requested)\n")
        request
        {"workspace_root" (.getPath requested-root)
         "requests" [{"id" "marker" "operation" "forms"
                      "file" "src/demo.clj" "forms" ["marker"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}]
    (try
      (let [result (inspect-tool/execute-inspect!
                     {:project-root (.getPath default-root)} request)]
        (is (:ok result))
        (is (= (.getPath (.getCanonicalFile requested-root))
               (:workspace_root result)))
        (is (= "(def marker :requested)"
               (get-in result [:results 0 :forms 0 :source]))))
      (finally
        (delete-tree! default-root)
        (delete-tree! requested-root)))))

(deftest confines-symlinks-and-canonical-file-expectations-before-read
  (let [project (temp-dir)
        outside (temp-dir)
        target (write-source! project "src/target.clj" "(ns target)\n(def x 1)\n")
        inside-link (io/file project "src/alias.clj")
        outside-file (write-source! outside "outside.clj" "(ns outside)\n")
        outside-link (io/file project "src/outside.clj")
        reads (atom 0)]
    (try
      (Files/createSymbolicLink (.toPath inside-link) (.toPath target)
                                (make-array FileAttribute 0))
      (Files/createSymbolicLink (.toPath outside-link) (.toPath outside-file)
                                (make-array FileAttribute 0))
      (let [alias-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"requests"
               [{"id" "one" "operation" "outline" "file" "src/target.clj"}
                {"id" "two" "operation" "outline" "file" "src/alias.clj"}]
               "expect" {"requests" 2 "files" 2}})
            escape-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"requests"
               [{"id" "escape" "operation" "outline"
                 "file" "src/outside.clj"}]
               "expect" {"requests" 1 "files" 1}})]
        (is (= "aggregate-file-expectation-mismatch"
               (:error_type alias-result)))
        (is (= "path-outside-project" (:error_type escape-result)))
        (is (zero? @reads)))
      (finally
        (delete-tree! project)
        (delete-tree! outside)))))

(deftest real-two-file-seven-form-dogfood-is-ordered-and-read-only
  (let [before (tree-state "bench")
        reads (atom [])
        request
        {"requests"
         [{"id" "summary-fields" "operation" "forms"
           "file" "bench/summarize_clean_codex.clj"
           "forms" ["numeric-fields" "boolean-fields" "summarize-group"
                    "markdown" "self-test"]
           "expect" {"forms" 5}}
          {"id" "rescore-fields" "operation" "forms"
           "file" "bench/rescore_clean_codex.clj"
           "forms" ["rescore-row" "emit-table"]
           "expect" {"forms" 2}}]
         "expect" {"requests" 2 "files" 2}}
        result
        (inspect-tool/execute-inspect!
          {:project-root (.getCanonicalPath (io/file "."))
           :read-source (fn [path] (swap! reads conj path) (slurp path))}
          request)]
    (is (:ok result))
    (is (= 2 (:file_read_count result)))
    (is (= 2 (count @reads)))
    (is (= ["summary-fields" "rescore-fields"] (mapv :id (:results result))))
    (is (= ["numeric-fields" "boolean-fields" "summarize-group"
            "markdown" "self-test"]
           (mapv :name (get-in result [:results 0 :forms]))))
    (is (= ["rescore-row" "emit-table"]
           (mapv :name (get-in result [:results 1 :forms]))))
    (is (= 7 (reduce + (map :form_count (:results result)))))
    (is (= before (tree-state "bench")))))

(deftest metadata-only-forms-retain-proof-without-source-bodies
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def alpha {:answer 42})\n")
        request {"requests" [{"id" "alpha" "operation" "forms"
                              "file" "src/demo.clj" "forms" ["alpha"]
                              "include_source" false
                              "expect" {"forms" 1}}]
                 "expect" {"requests" 1 "files" 1}}]
    (try
      (let [result (inspect-tool/execute-inspect!
                     {:project-root (.getPath project)} request)
            form (get-in result [:results 0 :forms 0])]
        (is (:ok result))
        (is (= 24 (get-in result [:results 0 :source_character_count])))
        (is (= "alpha" (:name form)))
        (is (string? (:hash form)))
        (is (= "alpha" (get-in form [:source_anchor :owner])))
        (is (not (contains? form :source))))
      (finally
        (delete-tree! project)))))

(deftest boundary-batch-runs-real-outline-match-and-xray-with-no-artifacts
  (let [project (temp-dir)
        _source (write-source!
                  project "src/demo.clj"
                  (str "(ns demo)\n"
                       "(def settings {:a 1 :b 2 :c 3})\n"
                       "(defn send-batch []\n"
                       "  \"decoy (send! _)\"\n"
                       "  (send! :actual))\n"))
        request
        {"requests"
         [{"id" "outline" "operation" "outline" "file" "src/demo.clj"}
          {"id" "match" "operation" "match" "file" "src/demo.clj"
           "match" "(send! _)" "expect" {"matches" 1}}
          {"id" "xray" "operation" "xray" "file" "src/demo.clj"
           "expression"
           "(-> (form 'settings) initializer (expect-count 1) (analyze (fn [[m]] (reduce + (vals m)))))"}]
         "expect" {"requests" 3 "files" 1}}
        before (tree-state project)]
    (try
      (let [result (inspect-tool/execute-inspect!
                     {:project-root (.getPath project)} request)]
        (is (:ok result))
        (is (= 1 (:file_read_count result)))
        (is (= 1 (get-in result [:results 1 :match_count])))
        (is (= "(send! :actual)"
               (get-in result [:results 1 :matches 0 :source])))
        (is (= 6 (get-in result [:results 2 :value])))
        (is (= before (tree-state project)))
        (is (empty? (filter #(re-find #"(?i)(plan|receipt|manifest)" %)
                            (keys (tree-state project))))))
      (finally
        (delete-tree! project)))))

;; @spec MCP-OP-TIME-004
(deftest callback-separates-concise-content-from-full-structured-evidence
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def answer 42)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "answer" "operation" "forms"
                      "file" "src/demo.clj" "forms" ["answer"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (is (= false (:error? (first @calls))))
      (is (str/starts-with? (first (:content (first @calls)))
                            "inspect_clojure\n"))
      (is (number? (get-in @calls [0 :structured :elapsed_ms])))
      (is (number? (get-in @calls [0 :structured
                                   :inspection_elapsed_ms])))
      (is (str/includes?
            (first (:content (first @calls)))
            (format "%.2f ms" (get-in @calls [0 :structured :elapsed_ms]))))
      (is (not (str/includes? (first (:content (first @calls)))
                              "(def answer")))
      (is (= "(def answer 42)"
             (get-in @calls [0 :structured :results 0 :forms 0 :source])))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest selector-refusal-summary-names-the-miss-and-hypothesis-without-authority
  ;; @spec MCP-OP-READ-DIAG-002 MCP-OP-READ-DIAG-003 MCP-OP-READ-CONT-001
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def answer 42)\n(def beta 7)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "before" "operation" "forms"
                      "file" "src/demo.clj" "forms" ["beta"]
                      "expect" {"forms" 1}}
                     {"id" "mistyped" "operation" "forms"
                      "file" "src/demo.clj" "forms" ["answr"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 2 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content error? structured]} (first @calls)
            summary (first content)]
        (is error?)
        (is (false? (:ok structured)))
        (is (false? (:read_complete structured)))
        (is (not (contains? structured :results)))
        (is (= {:id "mistyped" :operation "forms" :file "src/demo.clj"
                :requested_forms ["answr"]}
               (:failed_request structured)))
        (is (= [{:form "answr" :error_type "form-not-found"
                 :match_count 0}]
               (:failures structured)))
        (is (str/includes? summary "request mistyped · src/demo.clj"))
        (is (str/includes? summary "missing form answr"))
        (is (str/includes? summary
                           "I think you may have meant answer? (hypothesis only)"))
        (is (str/includes? summary
                           "available owners (2/2): answer, beta"))
        (is (str/includes? summary
                           (str "All listed owners are real snapshot evidence; "
                                "ranking is non-authoritative. Semantic selection "
                                "among them is allowed; the exact retry verifies "
                                "the selection.")))
        (is (str/includes? summary
                           "preserved 1 completed request from the frozen snapshot"))
        (is (str/includes? summary
                           "retry only mistyped; do not reread before"))
        (is (= false (get-in structured [:continuation :write_authority])))
        (is (= ["before"]
               (get-in structured [:continuation :completed_request_ids])))
        (is (= ["mistyped"]
               (get-in structured [:continuation :pending_request_ids])))
        (is (= "(def beta 7)"
               (get-in structured
                       [:continuation :completed_results 0 :forms 0 :source])))
        (is (not (str/includes? summary "(def answer"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest uninitialized-handler-reports-elapsed-time
  (let [calls (atom [])]
    (inspect-tool/init! nil)
    (inspect-tool/handle-inspect
      nil
      {"requests" [] "expect" {"requests" 0 "files" 0}}
      (fn [content error? structured]
        (swap! calls conj {:content (first content)
                           :error? error?
                           :structured structured})))
    (let [{:keys [content error? structured]} (first @calls)
          elapsed (:elapsed_ms structured)]
      (is (true? error?))
      (is (number? elapsed))
      (when (number? elapsed)
        (is (<= 0 elapsed))
        (is (str/includes?
              content (format "%.2f ms" elapsed)))))))

(deftest callback-queries-a-cold-job-without-rereading-source
  (let [project (temp-dir)
        calls (atom [])]
    (try
      (cold-verify/clear-jobs!)
      (inspect-tool/init! {:project-root (.getPath project)})
      (let [launched (cold-verify/launch!
                       (.getPath project) "full"
                       {:command ["/bin/sh" "-c" "printf cold-ok"]
                        :timeout-ms 1000})
            job (:verification_job launched)]
        (loop [attempt 0]
          (when (and (not (:verification_complete
                            (cold-verify/status (.getPath project) job)))
                     (< attempt 100))
            (Thread/sleep 10)
            (recur (inc attempt))))
        (inspect-tool/handle-inspect
          nil
          {"workspace_root" (.getPath project)
           "verification_job" job
           "view" "verification"}
          (fn [content error? structured]
            (swap! calls conj {:content content :error? error?
                               :structured structured})))
        (is (= false (:error? (first @calls))))
        (is (str/starts-with? (first (:content (first @calls)))
                              "inspect_clojure · cold verification\n"))
        (is (number? (get-in @calls [0 :structured :elapsed_ms])))
        (is (str/includes?
              (first (:content (first @calls)))
              (format "request %.2f ms"
                      (get-in @calls [0 :structured :elapsed_ms]))))
        (is (= :passed (get-in @calls [0 :structured :status])))
        (is (true? (get-in @calls [0 :structured :verification_complete])))
        (is (= 0 (get-in @calls [0 :structured :file_read_count] 0))))
      (finally
        (cold-verify/clear-jobs!)
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest handler-namespace-reload-preserves-the-live-runtime
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def answer 42)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (require 'clj-surgeon.mcp-inspect-tool :reload)
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "outline" "operation" "outline"
                      "file" "src/demo.clj"}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (is (= false (:error? (first @calls))))
      (is (= true (get-in @calls [0 :structured :read_complete])))
      (is (= "outline" (get-in @calls [0 :structured :results 0 :id])))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest metrics-telemetry-never-records-source-bodies
  (let [directory (temp-dir)
        state (telemetry/start! {:mode :metrics
                                 :directory (.getPath directory)
                                 :session-id "inspect-metrics"})
        request {"requests" [{"id" "secret" "operation" "forms"
                              "file" "src/private.clj"
                              "forms" ["secret"]
                              "expect" {"forms" 1}}]
                 "expect" {"requests" 1 "files" 1}}
        response {:ok true :read_complete true :request_count 1 :file_count 1
                  :file_read_count 1 :source_character_count 99
                  :results [{:forms [{:source "TOP-SECRET-SOURCE"}]}]}]
    (try
      (telemetry/record-inspect-call! state request response {:total_ms 1})
      (let [raw (slurp (:file state))
            event (json/parse-string (str/trim raw) true)]
        (is (= "inspect_clojure" (:tool event)))
        (is (= 1 (get-in event [:request_shape :requests])))
        (is (= 1 (get-in event [:outcome :file_reads])))
        (is (not (str/includes? raw "TOP-SECRET-SOURCE")))
        (is (not (str/includes? raw "private.clj"))))
      (finally
        (delete-tree! directory)))))

(deftest prepare-change-normalizes-the-real-java-map-boundary
  (let [project (temp-dir)
        source (write-source! project "src/sample/core.clj"
                              "(ns sample.core)\n(defn target [] :ok)\n")
        params (doto (java.util.HashMap.)
                 (.put "mode" "prepare-change")
                 (.put "subject" "sample.core/target")
                 (.put "intent" "Prepare one exact target change")
                 (.put "verify" "fast"))]
    (try
      (let [result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :semantic-resolver
               (fn [_]
                 (let [session "lsp-test-session"]
                   {:ok true
                    :version 2
                    :lsp_session session
                    :definition {:lsp_session session
                                 :file "src/sample/core.clj"
                                 :file_path (.getCanonicalPath source)
                                 :source_sha256 (structural-lens/source-hash (slurp source))
                                 :owner "target"
                                 :range {:start {:line 1 :character 6}
                                         :end {:line 1 :character 12}}
                                 :line 2 :character 7
                                 :name "target"}
                    :references []}))}
              params)]
        (is (:ok result))
        (is (= "inspect_clojure" (:operation result)))
        (is (= "prepare-change" (:mode result)))
        (is (:read_complete result))
        (is (= 1 (:site-count result))))
      (finally
        (delete-tree! project)))))

(deftest prepare-change-supplements-the-semantic-surface-with-quoted-vars
  (let [project (temp-dir)
        source (write-source! project "src/sample/core.clj"
                              "(ns sample.core)\n(defn target [] :ok)\n")
        _ (write-source! project "test/sample/core_test.clj"
                         (str "(ns sample.core-test\n"
                              "  (:require [sample.core :as sut]))\n"
                              "(deftest private-var-test\n"
                              "  (is (var? #'sut/target)))\n"))]
    (try
      (let [result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :semantic-resolver
               (fn [_]
                 (let [session "lsp-quoted-var-test"]
                   {:ok true
                    :version 3
                    :lsp_session session
                    :definition {:lsp_session session
                                 :file "src/sample/core.clj"
                                 :file_path (.getCanonicalPath source)
                                 :source_sha256
                                 (structural-lens/source-hash (slurp source))
                                 :owner_status "found"
                                 :owner "target"
                                 :range {:start {:line 1 :character 6}
                                         :end {:line 1 :character 12}}
                                 :line 2 :character 7
                                 :name "target"}
                    :references []}))}
              {:mode "prepare-change"
               :subject "sample.core/target"
               :intent "Prove the private Var caller"
               :scope "surface"
               :label "quoted"
               :verify "fast"})]
        (is (:ok result))
        (is (= 1 (get-in result [:quoted_var_proof :reference-count])))
        (is (= ["target" "private-var-test"]
               (mapv :form (:surface result))))
        (is (= [:language-server+exact-source :structural-var-quote]
               (mapv :authority (:surface result)))))
      (finally
        (delete-tree! project)))))

(deftest prepare-change-callback-is-compact-and-carries-the-full-basis-structurally
  (let [project (temp-dir)
        source (write-source! project "src/sample/core.clj"
                              "(ns sample.core)\n(defn target [] :ok)\n")
        calls (atom [])]
    (try
      (inspect-tool/init!
        {:project-root (.getPath project)
         :semantic-resolver
         (fn [_]
           (let [session "lsp-test-session"]
             {:ok true
              :version 2
              :lsp_session session
              :definition {:lsp_session session
                           :file "src/sample/core.clj"
                           :file_path (.getCanonicalPath source)
                           :source_sha256 (structural-lens/source-hash (slurp source))
                           :owner "target"
                           :range {:start {:line 1 :character 6}
                                   :end {:line 1 :character 12}}
                           :line 2 :character 7
                           :name "target"}
              :references []}))})
      (inspect-tool/handle-inspect
        nil
        {"mode" "prepare-change"
         "scope" "definition"
         "subject" "sample.core/target"
         "intent" "Prepare one exact target change"}
        (fn [content error? structured]
          (swap! calls conj {:content (first content)
                             :error? error?
                             :structured structured})))
      (is (false? (:error? (first @calls))))
      (is (.startsWith ^String (:content (first @calls))
                       "inspect_clojure · prepare-change"))
      (is (str/includes?
            (:content (first @calls))
            (format "%.2f ms"
                    (get-in @calls [0 :structured :elapsed_ms]))))
      (is (not (.contains ^String (:content (first @calls)) "(defn target")))
      (is (= "definition" (get-in @calls [0 :structured :scope])))
      (is (= "(defn target [] :ok)"
             (get-in @calls [0 :structured :decision-sites 0 :source])))
      (is (= [{:id "change/s01"
               :role :definition
               :file "src/sample/core.clj"
               :form "target"
               :line 2
               :end-line 2
               :subjects ["sample.core/target"]
               :authority :language-server+exact-source}]
             (get-in @calls [0 :structured :surface])))
      (is (<= (inspect-tool/mcp-result-byte-count
                (:content (first @calls))
                (:structured (first @calls)))
              inspect-tool/max-public-result-bytes))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest exact-source-prepare-crosses-the-mcp-adapter-without-semantic-lookup
  (let [project (temp-dir)
        _source (write-source! project "src/sample/core.clj"
                               "(ns sample.core)\n(defn target [] :ok)\n")
        semantic-calls (atom 0)
        calls (atom [])]
    (try
      (inspect-tool/init!
        {:project-root (.getPath project)
         :semantic-resolver
         (fn [_]
           (swap! semantic-calls inc)
           (throw (ex-info "semantic resolver must not run" {})))})
      (inspect-tool/handle-inspect
        nil
        {"mode" "prepare-change"
         "file" "src/sample/core.clj"
         "form" "target"
         "intent" "Delete one exact owner"
         "label" "delete"}
        (fn [content error? structured]
          (swap! calls conj {:content (first content)
                             :error? error?
                             :structured structured})))
      (is (zero? @semantic-calls))
      (is (false? (:error? (first @calls))))
      (is (str/includes? (:content (first @calls))
                         "exact named owner · no semantic index required"))
      (is (= :exact-source
             (get-in @calls [0 :structured :surface 0 :authority])))
      (is (= [{:site "delete/s01" :replace nil}]
             (get-in @calls [0 :structured :next_call :decisions])))
      (finally
        (inspect-tool/init! nil)
        (change-buffer/clear-bases!)
        (delete-tree! project)))))

(deftest oversized-public-decision-packet-refuses-and-retains-no-basis
  ;; Regression for the live definition-scoped call that overflowed the caller
  ;; even though only one named form required a decision.
  (change-buffer/clear-bases!)
  (let [project (temp-dir)
        source (write-source! project "src/sample/core.clj"
                              "(ns sample.core)\n(defn target [] :ok)\n")
        calls (atom [])]
    (try
      (inspect-tool/init!
        {:project-root (.getPath project)
         :semantic-resolver
         (fn [_]
           (let [session "lsp-test-session"]
             {:ok true
              :version 2
              :lsp_session session
              :definition {:lsp_session session
                           :file "src/sample/core.clj"
                           :file_path (.getCanonicalPath source)
                           :source_sha256 (structural-lens/source-hash (slurp source))
                           :owner "target"
                           :range {:start {:line 1 :character 6}
                                   :end {:line 1 :character 12}}
                           :line 2 :character 7
                           :name "target"}
              :references []}))})
      (with-redefs [inspect-tool/max-public-result-bytes 128]
        (inspect-tool/handle-inspect
          nil
          {"mode" "prepare-change"
           "scope" "definition"
           "subject" "sample.core/target"
           "intent" "Prepare one exact target change"}
          (fn [content error? structured]
            (swap! calls conj {:content (first content)
                               :error? error?
                               :structured structured}))))
      (is (:error? (first @calls)))
      (is (= :decision-output-budget-exceeded
             (get-in @calls [0 :structured :error-type])))
      (is (false? (get-in @calls [0 :structured :basis-retained])))
      (is (nil? (get-in @calls [0 :structured :basis])))
      (is (zero? (change-buffer/retained-basis-count)))
      (is (< (count ^String (:content (first @calls))) 2048)
          "the refusal itself remains bounded")
      (finally
        (inspect-tool/init! nil)
        (change-buffer/clear-bases!)
        (delete-tree! project)))))

(deftest exact-source-anchor-is-pure-and-refuses-wrong-or-ambiguous-owners
  ;; Field regression: formatting moved post-card while workspace/symbol kept
  ;; its previous range. The consumer must derive a new anchor from its bytes.
  (let [source (str "(ns sample.core)\n\n"
                    "(>defn target\n"
                    "  []\n"
                    "  :ok)\n")
        built (source-anchor/build-source-anchor
                "sample.core/target" "src/sample/core.clj" source {})]
    (is (:ok built))
    (is (= {:file "src/sample/core.clj"
            :source_sha256 (structural-lens/source-hash source)
            :owner "target"
            :range {:start {:line 2 :character 0}
                    :end {:line 4 :character 6}}
            :selection_range {:start {:line 2 :character 7}
                              :end {:line 2 :character 13}}}
           (:source-anchor built)))
    (is (= :semantic-candidate-namespace-mismatch
           (:error-type
             (source-anchor/build-source-anchor
               "other.core/target" "src/sample/core.clj" source {}))))
    (is (= :semantic-candidate-owner-mismatch
           (:error-type
             (source-anchor/build-source-anchor
               "sample.core/missing" "src/sample/core.clj" source {}))))
    (is (= 2
           (:owner-count
             (source-anchor/build-source-anchor
               "sample.core/target"
               "src/sample/core.clj"
               (str source "\n(>defn target [] :duplicate)\n")
               {}))))))

(deftest source-anchor-selects-exact-owner-tokens-across-defining-syntax
  (doseq [{:keys [label definition aliases]}
          [{:label "ordinary defn"
            :definition "(defn target [] :ok)"
            :aliases {}}
           {:label "private defn"
            :definition "(defn- target [] :ok)"
            :aliases {}}
           {:label "metadata-wrapped owner"
            :definition "(defn ^:private target [] :ok)"
            :aliases {}}
           {:label "custom defining-form alias"
            :definition "(defendpoint target [] :ok)"
            :aliases {"defendpoint" {:kind :defn}}}
           {:label "non-ASCII metadata before owner"
            :definition "(defn ^{:doc \"🧪\"} target [] :ok)"
            :aliases {}}]]
    (testing label
      (let [source (str "(ns sample.core)\n\n" definition "\n")
            built (source-anchor/build-source-anchor
                    "sample.core/target" "src/sample/core.clj" source aliases)
            {:keys [start end]} (get-in built [:source-anchor :selection_range])
            definition-line (nth (str/split-lines source) (:line start))]
        (is (:ok built))
        (is (= 2 (:line start) (:line end)))
        (is (= "target"
               (subs definition-line (:character start) (:character end)))))))
  (testing "a claimed owner absent from the exact form refuses before LSP"
    (let [missing (source-anchor/build-form-source-anchor
                    "src/sample/core.clj"
                    "(defn other [] :ok)\n"
                    {:name "target" :line 1 :end-line 1})]
      (is (false? (:ok missing)))
      (is (= :semantic-candidate-selection-missing (:error-type missing))))))

(deftest workspace-source-roots-are-deterministic-and-confined
  (let [root (temp-dir)]
    (try
      (spit (io/file root "deps.edn")
            "{:paths [\"src\" \"../escape\" \"/tmp/absolute\"] :aliases {:test {:extra-paths [\"test\"]}}}\n")
      (spit (io/file root "bb.edn") "{:paths [\"script\"]}\n")
      (is (= ["" "src" "test" "dev" "script"]
             (source-anchor/workspace-source-roots root)))
      (finally
        (delete-tree! root)))))

(deftest prepare-prefers-a-local-fqn-anchor-and-retains-fuzzy-discovery-only-as-fallback
  (testing "a conventional namespace path never pays for fuzzy workspace-symbol discovery"
    (let [root (temp-dir)
          relative "src/sample/core.clj"
          source (str "(ns sample.core)\n\n"
                      "(defn target [] :current)\n")
          _ (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
          _ (write-source! root relative source)
          calls (atom [])
          provider
          (fn [anchor]
            (swap! calls conj anchor)
            (if anchor
              {:ok true
               :resolution "source-anchor"
               :source_anchor anchor
               :definition {:file relative}}
              {:ok false
               :error-type :fuzzy-discovery-must-not-run}))]
      (try
        (let [result (inspect-tool/resolve-var!
                       {:project-root (.getCanonicalPath root)
                        :project-aliases {}
                        :semantic-provider provider}
                       "sample.core/target")
              anchor (first @calls)]
          (is (:ok result))
          (is (= 1 (count @calls)))
          (is (= relative (:file anchor)))
          (is (= "target" (:owner anchor)))
          (is (= {:start {:line 2 :character 0}
                  :end {:line 2 :character 25}}
                 (:range anchor)))
          (is (= (structural-lens/source-hash source)
                 (:source_sha256 anchor))))
        (finally
          (delete-tree! root)))))

  (testing "a nonstandard source path keeps the unanchored compatibility route"
    (let [root (temp-dir)
          relative "unusual/location.clj"
          source (str "(ns sample.core)\n\n"
                      "(defn target [] :current)\n")
          _ (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
          _ (write-source! root relative source)
          calls (atom [])
          provider
          (fn [anchor]
            (swap! calls conj anchor)
            (if anchor
              {:ok true
               :resolution "source-anchor"
               :source_anchor anchor
               :definition {:file relative}}
              {:ok true
               :definition {:file relative
                            :range {:start {:line 0 :character 0}
                                    :end {:line 0 :character 1}}}}))]
      (try
        (let [result (inspect-tool/resolve-var!
                       {:project-root (.getCanonicalPath root)
                        :project-aliases {}
                        :semantic-provider provider}
                       "sample.core/target")]
          (is (:ok result))
          (is (= 2 (count @calls)))
          (is (nil? (first @calls)))
          (is (= relative (:file (second @calls)))))
        (finally
          (delete-tree! root))))))

(deftest prepare-refuses-when-cclsp-does-not-confirm-the-exact-anchor
  (let [root (temp-dir)
        relative "src/sample/core.clj"
        _ (write-source! root relative "(ns sample.core)\n(defn target [] :ok)\n")]
    (try
      (let [result
            (inspect-tool/resolve-var!
              {:project-root (.getCanonicalPath root)
               :project-aliases {}
               :semantic-provider
               (fn [anchor]
                 (if anchor
                   {:ok true :resolution "workspace-symbol"}
                   {:ok true :definition {:file relative}}))}
              "sample.core/target")]
        (is (= :semantic-source-anchor-not-confirmed (:error-type result)))
        (is (true? (:source-unchanged result))))
      (finally
        (delete-tree! root)))))

(deftest retained-buffer-callback-opens-frozen-source-without-rereading-disk
  (change-buffer/clear-bases!)
  (let [project (temp-dir)
        source-text "(ns sample.core)\n(defn target [] :ok)\n"
        source (write-source! project "src/sample/core.clj" source-text)
        session "lsp-test-session"
        prepared
        (change-buffer/prepare-change!
          {:project-root (.getPath project)
           :semantic-resolver
           (fn [_]
             {:ok true
              :version 2
              :lsp_session session
              :definition {:lsp_session session
                           :file "src/sample/core.clj"
                           :file_path (.getCanonicalPath source)
                           :source_sha256 (structural-lens/source-hash source-text)
                           :owner "target"
                           :range {:start {:line 1 :character 6}
                                   :end {:line 1 :character 12}}
                           :line 2 :character 7
                           :name "target"}
              :references []})}
          {:subject "sample.core/target"
           :intent "Open one retained named form"
           :label "open-form"
           :scope "definition"})
        calls (atom [])]
    (try
      (spit source "(ns sample.core)\n;; changed after preparation\n")
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"basis" (:basis prepared)
         "view" "sites"
         "open" ["open-form/s01"]
         "context" "form"}
        (fn [content error? structured]
          (swap! calls conj {:content (first content)
                             :error? error?
                             :structured structured})))
      (let [{:keys [content error? structured]} (first @calls)]
        (is (false? error?))
        (is (.startsWith ^String content "inspect_clojure · retained buffers"))
        (is (str/includes?
              content
              (format "%.2f ms" (:elapsed_ms structured))))
        (is (not (.contains ^String content "(defn target"))
            "the text summary does not duplicate source")
        (is (= "basis-view" (:mode structured)))
        (is (= ["open-form/s01"] (mapv :id (:buffers structured))))
        (is (= ["(defn target [] :ok)"]
               (mapv :source (:buffers structured))))
        (is (= "(ns sample.core)\n;; changed after preparation\n"
               (slurp source))
            "opening the buffer does not reread or rewrite changed disk bytes")
        (is (<= (inspect-tool/mcp-result-byte-count content structured)
                inspect-tool/max-public-result-bytes)))
      (finally
        (inspect-tool/init! nil)
        (change-buffer/clear-bases!)
        (delete-tree! project)))))
