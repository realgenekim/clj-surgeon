(ns clj-surgeon.mcp-extraction-plan-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-workspace-sources :as workspace-sources]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

(def source
  "(ns sample.core)\n\n(defn moved [x] (inc x))\n")

(def private-source
  (str "(ns sample.core)\n\n"
       "(defn- moved [x] (inc x))\n\n"
       "(defn retained [] (moved 1))\n"))

(def caller
  (str "(ns sample.caller (:require [sample.core :as core]))\n"
       "(defn call [] (core/moved 1))\n"))

(def source-path "/tmp/sample-project/src/sample/core.clj")
(def target-path "/tmp/sample-project/src/sample/moved.clj")
(def caller-path "/tmp/sample-project/src/sample/caller.clj")

(def plan-request
  {:workspace-root "/tmp/sample-project"
   :file "src/sample/core.clj"
   :to "src/sample/moved.clj"
   :source-path source-path
   :target-path target-path
   :source source
   :forms ["moved"]
   :target-ns "sample.moved"
   :workspace-sources {source-path source caller-path caller}
   :relative-paths {source-path "src/sample/core.clj"
                    caller-path "src/sample/caller.clj"}
   :require-policy :minimal})

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

;; @spec MCP-OP-PLAN-001
;; @spec MCP-OP-PLAN-002
;; @spec MCP-OP-PLAN-003
(deftest compiles-public-snapshot-bound-extraction-plan
  (let [result (extraction-plan/compile-plan-response plan-request)
        public-plan (:plan result)
        next-extraction (get-in result [:next_call :extraction])]
    (is (:ok result))
    (is (true? (:read_complete result)))
    (is (true? (:source_unchanged result)))
    (is (= "plan-extraction" (:mode result)))
    (is (= ["src/sample/caller.clj"]
           (:callers-to-review public-plan)))
    (is (= {:caller_candidates {:returned 1 :omitted 0 :truncated false}
            :quoted_var_references {:returned 0 :omitted 0 :truncated false}}
           (:evidence_counts result)))
    (is (empty? (filter #(-> % name (.startsWith "_"))
                        (keys public-plan))))
    (is (= {:workspace_root "/tmp/sample-project"
            :extraction
            {:file "src/sample/core.clj"
             :to "src/sample/moved.clj"
             :forms ["moved"]
             :public_forms []
             :require_policy "minimal"
             :source_hash (:source_hash result)
             :caller_changes []
             :ignored_caller_files []}}
           (:next_call result)))
    (is (= "src/sample/core.clj" (:file public-plan)))
    (is (= "src/sample/moved.clj" (:to public-plan)))
    (is (= (:source_hash result) (:source_hash next-extraction)))))

;; @spec MCP-OP-PLAN-006
(deftest extraction-plan-makes-required-visibility-an-explicit-intent
  (let [request (assoc plan-request
                       :source private-source
                       :workspace-sources {source-path private-source})
        result (extraction-plan/compile-plan-response request)]
    (is (:ok result) (pr-str result))
    (is (= ["moved"]
           (get-in result [:plan :required-public-forms])))
    (is (= ["moved"]
           (get-in result [:next_call :extraction :public_forms])))))

;; @spec MCP-OP-PLAN-007
(deftest extraction-apply-requires-and-atomically-publicizes-private-callers
  (let [base-request
        {:file source-path
         :to target-path
         :forms ["moved"]
         :source private-source
         :target-ns "sample.moved"
         :workspace-sources {source-path private-source}
         :require-policy :minimal
         :expect {:forms 1 :caller-edits 0 :files 2}
         :caller-changes []
         :ignored-caller-files []}
        missing (extraction/compile-extraction
                  (assoc base-request :public-forms []))
        authorized (extraction/compile-extraction
                     (assoc base-request :public-forms ["moved"]))]
    (is (= :required-public-forms-missing (:error-type missing)))
    (is (= ["moved"] (:required-public-forms missing)))
    (is (:ok authorized) (pr-str authorized))
    (is (.contains ^String (get-in authorized [:future-sources target-path])
                   "(defn moved "))
    (is (.contains ^String (get-in authorized [:future-sources source-path])
                   ":refer [moved]"))))

(deftest extraction-apply-refuses-public-forms-without-private-moved-ownership
  (let [result
        (extraction/compile-extraction
          {:file source-path
           :to target-path
           :forms ["moved"]
           :public-forms ["retained"]
           :source private-source
           :target-ns "sample.moved"
           :workspace-sources {source-path private-source}
           :require-policy :minimal
           :expect {:forms 1 :caller-edits 0 :files 2}
           :caller-changes []
           :ignored-caller-files []})]
    (is (= :invalid-public-forms (:error-type result)))
    (is (= ["retained"] (:invalid-public-forms result)))))

;; @spec MCP-OP-PLAN-005
(deftest derives-extraction-expectations-without-duplicated-aggregate-counts
  (let [validated
        (contract/validate-tool-params
          {"extraction"
           {"file" "src/sample/core.clj"
            "to" "src/sample/moved.clj"
            "forms" ["moved"]
            "public_forms" ["moved"]
            "require_policy" "minimal"
            "source_hash" (apply str (repeat 64 "a"))
            "caller_changes" []
            "ignored_caller_files" []}})]
    (is (:ok validated))
    (is (= {:forms 1 :caller-edits 0 :files 2}
           (get-in validated [:params :extraction :expect])))
    (is (= ["moved"]
           (get-in validated [:params :extraction :public-forms])))
    (is (= (apply str (repeat 64 "a"))
           (get-in validated [:params :extraction :source-hash])))))

;; @spec MCP-OP-PLAN-008
;; @spec MCP-OP-PLAN-010
(deftest omitted-extraction-decisions-remain-distinct-from-explicit-values
  (let [minimal
        (contract/validate-tool-params
          {"extraction"
           {"file" "src/sample/core.clj"
            "to" "src/sample/moved.clj"
            "forms" ["moved"]
            "require_policy" "minimal"}})
        explicit
        (contract/validate-tool-params
          {"extraction"
           {"file" "src/sample/core.clj"
            "to" "src/sample/moved.clj"
            "forms" ["moved"]
            "public_forms" []
            "require_policy" "minimal"
            "caller_changes" []
            "ignored_caller_files" []}})]
    (is (:ok minimal) (pr-str minimal))
    (is (= {:forms 1 :caller-edits 0 :files 2}
           (get-in minimal [:params :extraction :expect])))
    (is (= [] (get-in minimal [:params :extraction :caller-changes])))
    (is (= [] (get-in minimal [:params :extraction :ignored-caller-files])))
    (is (not (contains? (get-in minimal [:params :extraction])
                        :public-forms)))
    (is (:ok explicit) (pr-str explicit))
    (is (contains? (get-in explicit [:params :extraction]) :public-forms))
    (is (= [] (get-in explicit [:params :extraction :public-forms])))))

;; @spec MCP-OP-PLAN-004
(deftest stale-planned-source-hash-refuses-before-compilation
  (let [result
        (extraction/compile-extraction
          {:file source-path
           :to target-path
           :forms ["moved"]
           :require-policy :minimal
           :source-hash (apply str (repeat 64 "0"))
           :expect {:forms 1 :caller-edits 0 :files 2}
           :caller-changes []
           :ignored-caller-files []
           :source source
           :target-ns "sample.moved"
           :workspace-sources {source-path source}})]
    (is (false? (:ok result)))
    (is (= :source-hash-mismatch (:error-type result)))
    (is (true? (:source-unchanged result)))))

(deftest explicit-extraction-expectations-remain-authoritative
  (testing "compatible callers may still supply exact aggregate counts"
    (let [validated
          (contract/validate-tool-params
            {"extraction"
             {"file" "src/sample/core.clj"
              "to" "src/sample/moved.clj"
              "forms" ["moved"]
              "require_policy" "minimal"
              "caller_changes" []
              "ignored_caller_files" []
              "expect" {"forms" 2 "caller_edits" 0 "files" 2}}})]
      (is (:ok validated))
      (is (= {:forms 2 :caller-edits 0 :files 2}
             (get-in validated [:params :extraction :expect]))))))

;; @spec MCP-OP-PLAN-001
;; @spec MCP-OP-PLAN-002
(deftest boundary-plan-uses-the-shared-workspace-source-universe-without-writing
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "clj-surgeon-extraction-plan"
                        (make-array java.nio.file.attribute.FileAttribute 0)))
        source-file (io/file root "src/sample/core.clj")
        caller-file (io/file root "src/sample/caller.clj")
        ignored-data (io/file root "src/sample/data.edn")
        target-file (io/file root "src/sample/moved.clj")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (spit caller-file caller)
      (spit ignored-data "{:moved true}\n")
      (let [sources (:sources (workspace-sources/read-all (.toPath root)))
            result (extraction-plan/plan!
                     {:project-root (.getPath root)}
                     {:mode "plan-extraction"
                      :file "src/sample/core.clj"
                      :to "src/sample/moved.clj"
                      :forms ["moved"]
                      :require_policy "minimal"})]
        (is (= 2 (count sources)))
        (is (:ok result))
        (is (= ["src/sample/caller.clj"]
               (get-in result [:plan :callers-to-review])))
        (is (false? (.exists target-file)))
        (is (= #{"prepare-change" "plan-extraction"}
               (set (get-in inspect-tool/inspect-schema
                            [:properties "mode" :enum])))))
      (finally
        (delete-tree! root)))))

;; @spec MCP-OP-EXTRACT-014
;; @spec MCP-OP-EXTRACT-023
(deftest plan-extraction-derives-target-ns-from-the-requested-workspace-root-not-the-servers
  ;; clj-surgeon-23j: a server started from ITS OWN project directory,
  ;; answering a plan-extraction call for a DIFFERENT workspace_root, must
  ;; derive target-ns from that requested workspace_root -- never from its
  ;; own configured project-root. The requested workspace also sits under an
  ;; ancestor directory literally named "src" (as every checkout under
  ;; ~/src/<repo> does), which is the exact shape that made an absolute-path
  ;; "/src/" substring match the WRONG ancestor when target-ns was derived
  ;; against the wrong root.
  (let [harness (.toFile (Files/createTempDirectory
                           "clj-surgeon-23j-plan-"
                           (make-array java.nio.file.attribute.FileAttribute 0)))
        server-root (io/file harness "server-project")
        workspace-root (io/file harness "src" "curtaincall-cfp-lens-scratch-fixture")
        source-file (io/file workspace-root "src/cfp_scheduler_killer/folds.clj")
        source
        (str "(ns cfp-scheduler-killer.folds\n"
             "  \"Pure event-log projection: facts in, derived state out.\")\n\n"
             "(defn- settings\n"
             "  \"The event's settings map.\"\n"
             "  [state event-id]\n"
             "  (get-in state [:events event-id :settings]))\n\n"
             "(defn- update-settings\n"
             "  \"Apply f to the event's settings map.\"\n"
             "  [state event-id f & args]\n"
             "  (apply update-in state [:events event-id :settings] f args))\n")]
    (try
      (.mkdirs server-root)
      (.mkdirs (.getParentFile source-file))
      (spit (io/file workspace-root "deps.edn") "{:paths [\"src\"]}\n")
      (spit source-file source)
      (let [result
            (inspect-tool/execute-inspect!
              {:project-root (.getPath server-root)}
              {"workspace_root" (.getPath workspace-root)
               "mode" "plan-extraction"
               "file" "src/cfp_scheduler_killer/folds.clj"
               "to" "src/cfp_scheduler_killer/settings_lens.clj"
               "forms" ["settings" "update-settings"]
               "require_policy" "minimal"})]
        (is (:ok result) (pr-str result))
        (is (= "cfp-scheduler-killer.settings-lens"
               (get-in result [:plan :target-ns])))
        (is (= "(ns cfp-scheduler-killer.settings-lens)"
               (get-in result [:plan :new-file-preview :ns-form]))))
      (finally
        (delete-tree! harness)))))

;; @spec MCP-OP-PLAN-002
(deftest oversized-extraction-plan-refuses-without-partial-evidence
  (let [result
        (inspect-tool/enforce-public-result-budget
          "large extraction plan"
          {:ok true
           :operation "inspect_clojure"
           :mode "plan-extraction"
           :plan {:new-file-preview (apply str (repeat 40000 "x"))}
           :read_complete true
           :source_unchanged true})]
    (is (false? (:ok result)))
    (is (= :structural-buffer-output-budget-exceeded (:error-type result)))
    (is (nil? (:plan result)))
    (is (true? (:source-unchanged result)))))
