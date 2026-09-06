(ns ^{:lane :fast} clj-surgeon.mcp-inspect-tool-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.owner-hypotheses :as hypotheses]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(deftest outline-schema-advertises-the-non-default-string-symbol-projection
  (let [outline-route (get-in inspect-tool/typed-inspect-schema
                              [:properties "requests" :items :oneOf 2])]
    (is (= {:type "boolean" :default false}
           (select-keys
             (get-in outline-route
                     [:properties "include_string_symbols"])
             [:type :default])))
    (is (not-any? #{"include_string_symbols"} (:required outline-route)))))

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
        ;; @spec MCP-OP-FIELD-008 -- this site's source WAS ":old", byte-for-byte
        ;; the request's own match string, so it is now omitted as derivable.
        ;; The reconstruction basis is the request-level match on the receipt.
        (is (not (contains? (get-in result [:results 2 :matches 0]) :source)))
        (is (= ":old" (get-in result [:results 2 :match])))
        (is (true? (get-in result [:results 2 :source_omitted_when_equal_to_match])))
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
               "expect" {"requests" 1 "files" 1}})
            target-hash (structural-lens/source-hash (slurp target))
            guarded-alias-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"snapshot_guards" {"src/target.clj" target-hash
                                  "src/alias.clj" target-hash}
               "requests"
               [{"id" "one" "operation" "outline" "file" "src/target.clj"}]
               "expect" {"requests" 1 "files" 1}})
            guarded-escape-result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)
               :read-source (fn [path] (swap! reads inc) (slurp path))}
              {"snapshot_guards" {"src/outside.clj" target-hash}
               "requests"
               [{"id" "escape" "operation" "outline"
                 "file" "src/outside.clj"}]
               "expect" {"requests" 1 "files" 1}})]
        (is (= "aggregate-file-expectation-mismatch"
               (:error_type alias-result)))
        (is (= "path-outside-project" (:error_type escape-result)))
        (is (= "snapshot-guard-alias-collision"
               (:error_type guarded-alias-result)))
        (is (= "snapshot" (:failed_stage guarded-alias-result)))
        (is (= "path-outside-project" (:error_type guarded-escape-result)))
        (is (= "snapshot" (:failed_stage guarded-escape-result)))
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

(def ^:private folds-arms-source
  (str "(ns cfp-scheduler-killer.folds)\n\n"
       "(defmulti fold-event (fn [_state payload] (:type payload)))\n\n"
       (apply str
              (for [dispatch ["\"schedule.locked\"" "\"schedule.unlocked\""
                              "\"agenda.published\"" "\"replay.marked\""
                              "\"sink.registered\""]]
                (str "(defmethod fold-event " dispatch "\n"
                     "  [state payload]\n"
                     "  ;; INTENT: LENS-004\n"
                     "  (if-let [slug (:slug (event-by-id state"
                     " (:event-id payload)))]\n"
                     "    (assoc-in state [:events slug :settings :x] true)\n"
                     "    state))\n\n")))
       "(defn event-by-id [state id] nil)\n"))

(deftest selector-refusal-summary-shows-the-exact-defmethod-owner-form
  ;; @spec MCP-OP-DISPATCH-002
  ;; @spec MCP-OP-DISPATCH-003
  (let [project (temp-dir)
        _source (write-source! project "src/folds.clj" folds-arms-source)
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "owner-probe" "operation" "forms"
                      "file" "src/folds.clj"
                      "forms" ["fold-event \"schedule.locked\""]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content error? structured]} (first @calls)
            summary (first content)
            failure (first (:selection_failures structured))
            owner (:defmethod_owner failure)]
        (is error?)
        (is (= "batch-form-selection-failed" (:error_type structured)))
        (testing "the structured refusal carries the exact owner form"
          (is (= {:kind "defmethod" :name "fold-event"
                  :dispatch "\"schedule.locked\""}
                 (:owner_form owner)))
          (is (true? (:owner_form_is_exact owner)))
          (is (= "apply_clojure_changes changes[].forms" (:accepted_by owner)))
          (is (= 5 (:arm_count owner)))
          (is (= ["\"schedule.locked\"" "\"schedule.unlocked\""
                  "\"agenda.published\"" "\"replay.marked\""
                  "\"sink.registered\""]
                 (:dispatch_vocabulary owner)))
          (is (false? (:authority owner))))
        (testing "the visible summary teaches the shape, not just the name"
          (is (str/includes?
                summary
                "owner is a multimethod · 5 defmethod arms share the name fold-event"))
          (is (str/includes?
                summary
                (str "send this exact owner form to apply_clojure_changes"
                     " changes[].forms: {kind: \"defmethod\","
                     " name: \"fold-event\","
                     " dispatch: \"\\\"schedule.locked\\\"\"}")))
          (is (str/includes?
                summary
                (str "dispatch values (5/5): \"schedule.locked\","
                     " \"schedule.unlocked\", \"agenda.published\","
                     " \"replay.marked\", \"sink.registered\"")))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(def ^:private noisy-dispatch-arms-source
  (str "(ns cfp-scheduler-killer.noisy)\n\n"
       "(defmulti fold-event (fn [_state payload] (:type payload)))\n\n"
       (apply str
              (for [index (range 60)]
                (str "(defmethod fold-event"
                     " [:conference.schedule/event-with-a-long-qualified-name-"
                     index "\n"
                     "                       ;; kept for the 2026 migration\n"
                     "                       :legacy-arm]\n"
                     "  [state payload]\n"
                     "  state)\n\n")))))

(deftest selector-refusal-bounds-dispatch-vocabulary-characters
  ;; @spec MCP-OP-DISPATCH-004
  (let [project (temp-dir)
        _source (write-source! project "src/noisy.clj"
                               noisy-dispatch-arms-source)
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "owner-probe" "operation" "forms"
                      "file" "src/noisy.clj"
                      "forms" ["fold-event"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content error? structured]} (first @calls)
            summary (first content)
            owner (:defmethod_owner (first (:selection_failures structured)))
            vocabulary (:dispatch_vocabulary owner)]
        (is error?)
        (is (= 60 (:dispatch_count owner)))
        (testing "the published vocabulary fits its character budget"
          (is (<= (count (json/generate-string vocabulary))
                  hypotheses/dispatch-vocabulary-character-limit))
          (is (true? (:dispatch_vocabulary_truncated owner)))
          (is (= (count vocabulary) (:dispatch_vocabulary_returned owner)))
          (is (= (- 60 (count vocabulary)) (:dispatch_vocabulary_omitted owner))))
        (testing "no entry can comment out or break the joined summary line"
          (is (not-any? #(str/includes? % ";;") vocabulary))
          (is (not-any? #(str/includes? % "\n") vocabulary))
          (let [line (first (filter #(str/includes? % "dispatch values")
                                    (str/split-lines summary)))]
            (is (some? line))
            (is (not (str/includes? line ";;")))
            (is (str/includes? line "; truncated"))
            (is (str/includes?
                  line
                  "[:conference.schedule/event-with-a-long-qualified-name-0 :legacy-arm]")))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest selector-refusal-summary-says-one-defmethod-arm-in-the-singular
  ;; @spec MCP-OP-DISPATCH-003
  (let [project (temp-dir)
        _source (write-source!
                  project "src/one.clj"
                  (str "(ns one)\n\n"
                       "(defmulti fold-event (fn [_state payload] (:type payload)))\n\n"
                       "(defmethod fold-event \"only.arm\"\n"
                       "  [state payload]\n"
                       "  state)\n"))
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "owner-probe" "operation" "forms"
                      "file" "src/one.clj"
                      "forms" ["fold-event"]
                      "expect" {"forms" 1}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content error?]} (first @calls)
            summary (first content)]
        (is error?)
        (is (str/includes?
              summary
              "owner is a multimethod · 1 defmethod arm shares the name fold-event"))
        (is (not (str/includes? summary "1 defmethod arms"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest missing-field-evidence-computes-its-minimal-shape-once
  ;; @spec MCP-OP-FIELD-001
  (let [calls (atom 0)
        original @#'clj-surgeon.mcp-inspect/minimal-request-shape]
    (with-redefs [clj-surgeon.mcp-inspect/minimal-request-shape
                  (fn [path required]
                    (swap! calls inc)
                    (original path required))]
      (let [evidence (#'clj-surgeon.mcp-inspect/missing-fields-evidence
                       ["requests" 0] #{"file" "id" "operation"} ["file"])]
        (is (= {"file" "src/example.clj" "id" "r1" "operation" "outline"}
               (:minimal_request evidence)))
        (is (= 1 @calls))))))

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
        (is (str/includes? summary
                           "copy continuation.retry_template.arguments"))
        (is (= false (get-in structured [:continuation :write_authority])))
        (is (= ["before"]
               (get-in structured [:continuation :completed_request_ids])))
        (is (= ["mistyped"]
               (get-in structured [:continuation :pending_request_ids])))
        (is (= "(def beta 7)"
               (get-in structured
                       [:continuation :completed_results 0 :forms 0 :source])))
        (is (= (.getCanonicalPath project)
               (get-in structured
                       [:continuation :retry_template :arguments
                        :workspace_root])))
        (is (= [nil]
               (get-in structured
                       [:continuation :retry_template :arguments
                        :requests 0 :forms])))
        (is (= {:requests 1 :files 1}
               (get-in structured
                       [:continuation :retry_template :arguments :expect])))
        (is (not (str/includes? summary "(def answer"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest guarded-selector-retry-preserves-completed-siblings-across-retries
  ;; @spec MCP-OP-READ-GUARD-001 MCP-OP-READ-CONT-001 MCP-OP-READ-CONT-002
  (let [project (temp-dir)
        a-file (write-source! project "src/a.clj"
                              "(ns a)\n(def alpha 1)\n")
        _b-file (write-source! project "src/b.clj"
                               "(ns b)\n(def beta 2)\n")
        _c-file (write-source! project "src/c.clj"
                               "(ns c)\n(def gamma 3)\n")
        initial
        (inspect-tool/execute-inspect!
          {:project-root (.getPath project)}
          {"requests" [{"id" "a" "operation" "forms"
                        "file" "src/a.clj" "forms" ["alpha"]
                        "expect" {"forms" 1}}
                       {"id" "b" "operation" "forms"
                        "file" "src/b.clj" "forms" ["bet"]
                        "expect" {"forms" 1}}
                       {"id" "c" "operation" "forms"
                        "file" "src/c.clj" "forms" ["gamm"]
                        "expect" {"forms" 1}}]
           "expect" {"requests" 3 "files" 3}})
        guards-1 (get-in initial [:continuation :snapshot_guards])
        retry-1 (get-in initial [:continuation :retry_template :arguments])
        unfilled
        (inspect-tool/execute-inspect!
          {:project-root (.getPath project)}
          retry-1)
        second
        (inspect-tool/execute-inspect!
          {:project-root (.getPath project)}
          (assoc-in retry-1 [:requests 0 :forms 0] "beta"))
        guards-2 (get-in second [:continuation :snapshot_guards])
        retry-2 (get-in second [:continuation :retry_template :arguments])]
    (try
      (is (= ["a"]
             (get-in initial [:continuation :completed_request_ids])))
      (is (= #{"src/a.clj" "src/b.clj" "src/c.clj"}
             (set (keys guards-1))))
      (is (= "invalid-mcp-request" (:error_type unfilled)))
      (is (not (contains? unfilled :continuation)))
      (is (= ["b"]
             (get-in second [:continuation :completed_request_ids])))
      (is (= #{"src/a.clj" "src/b.clj" "src/c.clj"}
             (set (keys guards-2))))
      (is (= {:requests 1 :files 1} (:expect retry-2)))
      (is (= [nil] (get-in retry-2 [:requests 0 :forms])))
      (spit a-file "(ns a)\n(def alpha :changed)\n")
      (let [stale
            (inspect-tool/execute-inspect!
              {:project-root (.getPath project)}
              (assoc-in retry-2 [:requests 0 :forms 0] "gamma"))]
        (is (false? (:ok stale)))
        (is (= "snapshot-guard-mismatch" (:error_type stale)))
        (is (= "snapshot" (:failed_stage stale)))
        (is (= "src/a.clj" (:file stale)))
        (is (not (contains? stale :results)))
        (is (not (contains? stale :continuation)))
        (is (not (contains? stale :source)))
        (is (= "refresh_snapshot" (:next_action stale))))
      (finally
        (delete-tree! project)))))

(deftest selector-continuation-obeys-the-complete-public-envelope-budget
  ;; @spec MCP-OP-READ-CONT-002
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(def alpha 1)\n(def beta 2)\n")
        calls (atom [])
        budget-var (ns-resolve 'clj-surgeon.mcp-inspect-tool
                               'max-public-result-bytes)]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (with-redefs-fn
        {budget-var 1}
        #(inspect-tool/handle-inspect
           nil
           {"requests" [{"id" "before" "operation" "forms"
                         "file" "src/demo.clj" "forms" ["beta"]
                         "expect" {"forms" 1}}
                        {"id" "mistyped" "operation" "forms"
                         "file" "src/demo.clj" "forms" ["bet"]
                         "expect" {"forms" 1}}]
            "expect" {"requests" 2 "files" 1}}
           (fn [content error? structured]
             (swap! calls conj {:content content :error? error?
                                :structured structured}))))
      (let [{:keys [content error? structured]} (first @calls)
            summary (first content)]
        (is error?)
        (is (= "inspect-output-limit" (:error_type structured)))
        (is (= "output-budget" (:failed_stage structured)))
        (is (not (contains? structured :continuation)))
        (is (not (contains? structured :results)))
        (is (not (contains? structured :source)))
        (is (not (str/includes? summary "(def beta"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest guarded-path-and-read-failures-refuse-at-the-snapshot-stage
  ;; @spec MCP-OP-READ-GUARD-001 MCP-OP-READ-CONT-002
  (let [project (temp-dir)
        source-file (write-source! project "src/demo.clj"
                                   "(ns demo)\n(def alpha 1)\n")
        unreadable (io/file project "src/unreadable.clj")
        _ (.mkdirs unreadable)
        source-hash (structural-lens/source-hash (slurp source-file))
        dummy-hash (apply str (repeat 64 "a"))
        request {"requests" [{"id" "demo" "operation" "forms"
                              "file" "src/demo.clj" "forms" ["alpha"]
                              "expect" {"forms" 1}}]
                 "expect" {"requests" 1 "files" 1}}
        run (fn [guards]
              (inspect-tool/execute-inspect!
                {:project-root (.getPath project)}
                (assoc request "snapshot_guards" guards)))]
    (try
      (doseq [[label result]
              [[:invalid-path
                (run {"src/demo.clj" source-hash
                      "../outside.clj" dummy-hash})]
               [:missing-file
                (run {"src/demo.clj" source-hash
                      "src/missing.clj" dummy-hash})]
               [:unreadable-file
                (run {"src/demo.clj" source-hash
                      "src/unreadable.clj" dummy-hash})]]]
        (testing (name label)
          (is (false? (:ok result)))
          (is (= "snapshot" (:failed_stage result)))
          (is (not (contains? result :results)))
          (is (not (contains? result :continuation)))
          (is (not (contains? result :source)))))
      (finally
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

;; MOVED, round five: `callback-queries-a-cold-job-without-rereading-source`
;; now lives in `clj-surgeon.mcp-inspect-cold-job-test` (:battery). It drove
;; /bin/sh through the production cold-verify helper from inside a namespace
;; declared `:fast`, whose lane rule reads "No child process" -- the
;; round-three landing review's finding 6. The behaviour is unchanged and
;; still runs; the LANE now matches what it does.

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

;; ---------------------------------------------------------------------------
;; Refusals that name their own field, in the visible summary. Friction ledger
;; items 3, 4 and 6 (2026-09-02): "two refusals name no field" and "`_`
;; silently misses longer paths".
;; ---------------------------------------------------------------------------

(deftest missing-fields-summary-names-the-field-and-the-minimal-shape
  ;; @spec MCP-OP-FIELD-001
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj" "(ns demo)\n(def a 1)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "outline" "operation" "outline"
                      "file" "src/demo.clj"}]}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content structured]} (first @calls)
            summary (first content)]
        (is (false? (:ok structured)))
        (is (= "missing-fields" (:reason structured)))
        (is (str/includes?
              summary
              "missing required field at the request root: expect"))
        (is (str/includes? summary "required there: expect, requests"))
        (is (str/includes?
              summary
              (str "minimal valid shape: "
                   "{\"expect\":{\"requests\":1,\"files\":1},"
                   "\"requests\":[{\"id\":\"r1\",\"operation\":\"outline\","
                   "\"file\":\"src/example.clj\"}]}")))
        (is (str/includes?
              summary
              (str "→ add the named field(s) in the minimal valid shape above "
                   "and call inspect_clojure once"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest invalid-require-policy-summary-names-the-field-and-its-values
  ;; @spec MCP-OP-FIELD-002
  (let [project (temp-dir)
        _source (write-source! project "src/demo.clj"
                               "(ns demo)\n(defn a [] 1)\n")
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"mode" "plan-extraction"
         "file" "src/demo.clj"
         "to" "src/demo_moved.clj"
         "forms" ["a"]}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content structured]} (first @calls)
            summary (first content)]
        (is (false? (:ok structured)))
        (is (= "invalid-require-policy" (:error_type structured)))
        (is (= "require_policy" (:field structured)))
        (is (= ["minimal" "copy-all"] (:accepted structured)))
        (is (str/includes?
              summary "field require_policy accepts: minimal, copy-all"))
        (is (str/includes?
              summary
              "require_policy is required and is never defaulted")))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

(deftest match-cardinality-refusal-summary-explains-the-wildcard
  ;; @spec MCP-OP-FIELD-003
  (let [project (temp-dir)
        _source (write-source!
                  project "src/demo.clj"
                  (str "(ns demo)\n"
                       "(def paths [[:events slug :settings :hero :url]\n"
                       "            [:events slug :settings :blind :on]])\n"))
        calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil
        {"requests" [{"id" "paths" "operation" "match"
                      "file" "src/demo.clj"
                      "match" "[:events slug :settings _]"
                      "expect" {"matches" 2}}]
         "expect" {"requests" 1 "files" 1}}
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content structured]} (first @calls)
            summary (first content)]
        (is (false? (:ok structured)))
        (is (= "inspect-cardinality-mismatch" (:error_type structured)))
        (is (str/includes?
              summary
              (str "note: each `_` matches exactly one subtree; "
                   "a longer form needs a longer pattern"))))
      (finally
        (inspect-tool/init! nil)
        (delete-tree! project)))))

;; ---------------------------------------------------------------------------
;; The 32,768-byte public-result budget, MEASURED through the public tool path.
;; Round 2, 2026-09-06.
;;
;; READ THE VERB CAREFULLY. `enforce-public-result-budget` is a HELPER: it
;; measures a result and returns a typed refusal for one that is over. These
;; witnesses call `execute-inspect!` and then ask the helper; they pin the
;; measured byte count and what the helper says about it, and they are NOT the
;; enforcement witnesses. Enforcement on the ordinary path -- the handler
;; refusing rather than publishing -- is MCP-OP-FIELD-009, witnessed through
;; `handle-inspect` at the bottom of this namespace (inb-b60d6e, fixed
;; 2026-09-06; when these witnesses were written the ordinary path reached
;; `:else raw-result` and published whole).
;;
;; The other budget, `clj-surgeon.mcp-inspect/enforce-output-budget`
;; (per-request-result 65,536), IS on the ordinary path and does refuse.
;;
;; 200 owners is a TESTED FIXTURE, not a capacity guarantee: the measured byte
;; count depends on the length of each site's path, address, and owner name, so
;; a file with longer names or deeper paths crosses the budget sooner.
;; ---------------------------------------------------------------------------

(def ^:private fence-literal "(send! :ping)")

(defn- repeated-literal-source
  [owner-count]
  (str "(ns many)\n"
       (str/join
         (map (fn [i] (format "(defn owner-%03d []\n  %s)\n" i fence-literal))
              (range owner-count)))))

(defn- measure-repeated-literal
  "One real inspect_clojure request through the public tool path, measured the
   way the server measures it before it publishes anything."
  [owner-count]
  (let [project (temp-dir)]
    (try
      (write-source! project "src/many.clj" (repeated-literal-source owner-count))
      (let [result (inspect-tool/execute-inspect!
                     {:project-root (.getPath project)}
                     {"requests" [{"id" "m00" "operation" "match"
                                   "file" "src/many.clj"
                                   "match" fence-literal}]
                      "expect" {"requests" 1 "files" 1}})
            normalized (assoc result :elapsed_ms 0.0)
            summary (#'inspect-tool/inspect-summary normalized)]
        {:result result
         :bytes (inspect-tool/mcp-result-byte-count summary normalized)
         :gated (inspect-tool/enforce-public-result-budget summary normalized)})
      (finally (delete-tree! project)))))

(deftest omitting-the-source-echo-pays-back-what-owner-counts-cost-in-bytes
  ;; @spec MCP-OP-FIELD-008
  ;; NOT a trunk-to-tip capability gain. Trunk (d95e6304) already measured 101
  ;; repeated-literal owners under the budget; adding owner_counts (MCP-OP-FIELD-007)
  ;; pushed the same fixture over it, and dropping the derivable source echo
  ;; brings it back under. This witness pins the restoration, not a new capacity.
  (let [{:keys [result bytes gated]} (measure-repeated-literal 101)
        request (first (:results result))]
    (testing "101 repeated-literal owners measure under the published budget"
      (is (:ok result))
      (is (pos? bytes))
      (is (< bytes inspect-tool/max-public-result-bytes))
      (is (:ok gated))
      (is (nil? (:error-type gated))))
    (testing "with every site, count, hash and owner tally intact"
      (is (= 101 (:match_count request)))
      (is (= 101 (count (:matches request))))
      (is (= 101 (count (:owner_counts request))))
      (is (= 101 (reduce + 0 (map :matches (:owner_counts request)))))
      (is (every? :hash (:matches request)))
      (is (every? :file_hash (:matches request))))
    (testing "and the reconstruction basis on the receipt"
      (is (true? (:source_omitted_when_equal_to_match request)))
      (is (= fence-literal (:match request)))
      (is (not-any? #(contains? % :source) (:matches request))))))

(deftest an-oversized-match-is-measured-over-budget-and-nothing-is-truncated
  ;; @spec MCP-OP-FIELD-008
  ;; 200 owners is over the budget with or without the echo. What is pinned:
  ;; the omission never truncates a result to fit, every site and count survives
  ;; at 200 as at 101, and the budget helper -- when it is asked -- names itself
  ;; and returns no partial result. `execute-inspect!` is below the handler's
  ;; ceiling gate, so the whole result is what this witness measures; what the
  ;; public handler now does with it is MCP-OP-FIELD-009's witness.
  (let [{:keys [result bytes gated]} (measure-repeated-literal 200)]
    (is (:ok result))
    (is (= 200 (:match_count (first (:results result)))))
    (is (= 200 (count (:matches (first (:results result))))))
    (is (every? :hash (:matches (first (:results result)))))
    (is (> bytes inspect-tool/max-public-result-bytes))
    (is (false? (:ok gated)))
    (is (= :structural-buffer-output-budget-exceeded (:error-type gated)))
    (is (nil? (:results gated)))
    (is (true? (:source-unchanged gated)))
    (is (= inspect-tool/max-public-result-bytes
           (get-in gated [:limits :public-result-bytes])))))

;; ---------------------------------------------------------------------------
;; The 32,768-byte public-result ceiling, ENFORCED on the ordinary read path.
;; inb-b60d6e, fixed 2026-09-06.
;;
;; The witnesses above measure through `execute-inspect!` and ask the budget
;; HELPER what it says. These drive the PUBLIC handler Var the MCP server
;; registers (`handle-inspect`) and read what it actually published: an
;; oversized ordinary match result is now refused with the typed refusal and no
;; results, and an under-budget one is published whole with every site.
;;
;; The measurement is the real one: the UTF-8 byte length of the exact JSON
;; envelope the transport sends, never a character count and never an estimate.
;; ---------------------------------------------------------------------------

(defn- published
  "One inspect_clojure call through the public handler, with the exact JSON
   envelope that publication produces measured in both characters and UTF-8
   bytes."
  [project params]
  (let [calls (atom [])]
    (try
      (inspect-tool/init! {:project-root (.getPath project)})
      (inspect-tool/handle-inspect
        nil params
        (fn [content error? structured]
          (swap! calls conj {:content content :error? error?
                             :structured structured})))
      (let [{:keys [content structured error?]} (first @calls)
            envelope (json/generate-string
                       {:content [{:type "text" :text (first content)}]
                        :structuredContent structured
                        :isError (boolean error?)})]
        {:summary (first content)
         :structured structured
         :envelope-characters (count envelope)
         :envelope-bytes (count (.getBytes envelope "UTF-8"))})
      (finally (inspect-tool/init! nil)))))

(defn- match-params
  []
  {"requests" [{"id" "m00" "operation" "match"
                "file" "src/many.clj"
                "match" fence-literal}]
   "expect" {"requests" 1 "files" 1}})

(defn- named-owner-source
  [owner-count owner-name]
  (str "(ns many)\n"
       (str/join
         (map (fn [i] (format "(defn %s []\n  %s)\n"
                              (owner-name i) fence-literal))
              (range owner-count)))))

(deftest the-public-handler-refuses-an-ordinary-match-over-the-result-ceiling
  ;; @spec MCP-OP-FIELD-009
  ;; 200 repeated-literal owners: 62,150 measured bytes against a 32,768-byte
  ;; ceiling. Before this fix the handler published all of it with ok=true.
  (let [project (temp-dir)]
    (try
      (write-source! project "src/many.clj" (repeated-literal-source 200))
      (let [{:keys [structured summary envelope-bytes]}
            (published project (match-params))
            measured (:bytes (measure-repeated-literal 200))]
        (testing "the typed refusal, naming the measured bytes and the ceiling"
          (is (false? (:ok structured)))
          (is (= "structural-buffer-output-budget-exceeded"
                 (:error_type structured)))
          (is (> (get-in structured [:required :public_result_bytes])
                 inspect-tool/max-public-result-bytes))
          ;; The same request measured independently through `execute-inspect!`.
          ;; Equality is to within the digits of the result's own timing fields
          ;; (`inspection_elapsed_ms` is not fixed at measurement time), which
          ;; is why this is an emission gate and not a final-wire byte cap.
          (is (< (Math/abs
                   (- measured
                      (long (get-in structured
                                    [:required :public_result_bytes]))))
                 64))
          (is (= inspect-tool/max-public-result-bytes
                 (get-in structured [:limits :public_result_bytes])))
          (is (= (str "split the request into bounded file groups; "
                      "keep every site and count")
                 (:remedy structured))))
        (testing "and no false completion: nothing read, nothing published"
          (is (nil? (:results structured)))
          (is (false? (:read_complete structured)))
          (is (true? (:source_unchanged structured)))
          (is (not= "none" (:next_action structured))))
        (testing "the refusal itself fits the ceiling it names"
          (is (<= envelope-bytes inspect-tool/max-public-result-bytes))
          (is (str/includes? summary "split the request into bounded file groups"))))
      (finally (delete-tree! project)))))

(deftest a-match-under-the-ceiling-is-still-published-whole-with-every-site
  ;; @spec MCP-OP-FIELD-009
  ;; The ceiling refuses; it never truncates, elides, or drops a site. 101
  ;; owners measure under it, so all 101 sites are published.
  (let [project (temp-dir)]
    (try
      (write-source! project "src/many.clj" (repeated-literal-source 101))
      (let [{:keys [structured envelope-bytes envelope-characters]}
            (published project (match-params))
            request (first (:results structured))]
        (is (true? (:ok structured)))
        (is (<= envelope-bytes inspect-tool/max-public-result-bytes))
        (is (pos? envelope-characters))
        (is (= 101 (:match_count request)))
        (is (= 101 (count (:matches request))))
        (is (= 101 (reduce + 0 (map :matches (:owner_counts request)))))
        (is (every? :hash (:matches request))))
      (finally (delete-tree! project)))))

(deftest the-public-ceiling-is-measured-in-utf-8-bytes-not-characters
  ;; @spec MCP-OP-FIELD-009
  ;; 55 owners whose names are multi-byte characters: the would-be result is
  ;; UNDER the ceiling counted in characters and OVER it counted in UTF-8
  ;; bytes. A character-counting ceiling would publish it.
  (let [project (temp-dir)
        owner-name (fn [i] (str (apply str (repeat 60 "漢"))
                                "-" (format "%03d" i)))]
    (try
      (write-source! project "src/many.clj"
                     (named-owner-source 55 owner-name))
      (let [whole (let [calls (atom [])]
                    (try
                      (inspect-tool/init! {:project-root (.getPath project)})
                      (let [result (inspect-tool/execute-inspect!
                                     {:project-root (.getPath project)}
                                     (match-params))
                            normalized (assoc result :elapsed_ms 0.0)
                            envelope (json/generate-string
                                       {:content
                                        [{:type "text"
                                          :text (#'inspect-tool/inspect-summary
                                                  normalized)}]
                                        :structuredContent normalized
                                        :isError (not (:ok normalized))})]
                        (swap! calls conj result)
                        {:match-count (:match_count (first (:results result)))
                         :characters (count envelope)
                         :bytes (count (.getBytes envelope "UTF-8"))})
                      (finally (inspect-tool/init! nil))))
            {:keys [structured]} (published project (match-params))]
        (testing "the fixture separates the two counts across the ceiling"
          (is (= 55 (:match-count whole)))
          (is (< (:characters whole) inspect-tool/max-public-result-bytes))
          (is (> (:bytes whole) inspect-tool/max-public-result-bytes)))
        (testing "and the handler refuses on the byte count"
          (is (false? (:ok structured)))
          (is (= "structural-buffer-output-budget-exceeded"
                 (:error_type structured)))
          (is (< (Math/abs
                   (- (:bytes whole)
                      (long (get-in structured
                                    [:required :public_result_bytes]))))
                 64))
          (is (nil? (:results structured)))))
      (finally (delete-tree! project)))))
