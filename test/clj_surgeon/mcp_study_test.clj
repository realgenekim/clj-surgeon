(ns clj-surgeon.mcp-study-test
  "MCP read-entrance witnesses for the study operations.

   @spec MCP-OP-STUDY-001 MCP-OP-STUDY-002 MCP-OP-STUDY-003
   @spec MCP-OP-STUDY-004 MCP-OP-STUDY-005 MCP-OP-STUDY-006
   @spec MCP-OP-STUDY-007 MCP-OP-STUDY-008 MCP-OP-STUDY-009
   @spec MCP-OP-STUDY-010 MCP-OP-STUDY-011 MCP-OP-STUDY-012"
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.core :as core]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.study :as study]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private project-root (System/getProperty "user.dir"))
(def ^:private config {:project-root project-root})
(def ^:private real-file "src/clj_surgeon/analyze.clj")
(def ^:private fixture-dir "test-fixtures/cljc/existing-ops")

(defn- run
  [params]
  (inspect-tool/execute-inspect! config params))

(defn- one
  ([operation extra] (one operation extra {}))
  ([operation extra top]
   (run (merge {"requests" [(merge {"operation" operation
                                    "file" real-file}
                                   extra)]
                "expect" {"requests" 1 "files" 1}}
               top))))

(defn- result-of
  [response]
  (first (:results response)))

;; ============================================================
;; Operations
;; ============================================================

;; @spec MCP-OP-STUDY-002
(deftest deps-request-returns-a-bounded-call-graph
  (let [response (one "deps" {"limit" 16384})
        result (result-of response)]
    (is (true? (:ok response)))
    (is (true? (:read_complete response)))
    (is (= "deps" (:operation result)))
    (is (= real-file (:file result)))
    (is (string? (:file_hash result)))
    (is (false? (:truncated result)))
    (is (pos? (:returned result)))
    (is (= (:form_count result) (:returned result))))
  (testing "one owner"
    (let [result (result-of (one "deps" {"form" "dep-tree"}))]
      (is (= "dep-tree" (get-in result [:deps :name])))
      (is (= 1 (:returned result))))))

;; @spec MCP-OP-STUDY-003
(deftest topo-request-returns-order-and-cycles
  (let [result (result-of (one "topo" {"limit" 16384}))]
    (is (= "topo" (:operation result)))
    (is (vector? (get-in result [:topo :sorted])))
    (is (vector? (get-in result [:topo :cycles])))
    (is (contains? (:topo result) :has_cycles?))))

;; @spec MCP-OP-STUDY-004
(deftest ls-deps-request-returns-a-dependency-tree
  (let [result (result-of (one "ls-deps" {"form" "extraction-closure"
                                          "limit" 16384}))]
    (is (= "extraction-closure" (get-in result [:dep_tree :name])))))

;; @spec MCP-OP-STUDY-005
(deftest ls-extract-request-returns-the-minimal-closure
  (let [result (result-of (one "ls-extract" {"form" "extraction-closure"}))]
    (is (= "extraction-closure" (get-in result [:closure :target])))
    (is (= ["extraction-closure"]
           (mapv :name (get-in result [:closure :forms]))))))

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-mode-returns-a-bounded-tree
  (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                       "limit" 16384})]
    (is (true? (:ok response)))
    (is (= "ls-tree" (:mode response)))
    (is (= fixture-dir (:dir response)))
    (is (false? (:truncated response)))
    (is (true? (:read_complete response)))
    (is (= 7 (:file_count response)))
    (is (= 7 (:returned response)))
    (is (str/includes? (:tree response) "total: 7 files"))
    (is (not (str/includes? (:tree response) project-root))
        "a receipt must not leak host-absolute paths"))
  (testing "edn format returns rows, not a string"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                         "format" "edn" "limit" 16384})]
      (is (vector? (:files response)))
      (is (= 7 (count (:files response))))
      (is (nil? (:tree response))))))

;; @spec MCP-OP-STUDY-011
(deftest ls-tree-names-format-is-a-compact-table-of-contents
  (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 16384})]
    (testing "names is the default rendering when grep is absent"
      (is (= "names" (:format response))))
    (is (true? (:ok response)))
    (is (true? (:read_complete response)))
    (is (= 7 (:file_count response)))
    (is (vector? (:files response)))
    (is (nil? (:tree response)))
    (is (= 7 (count (:files response))))
    (doseq [entry (:files response)]
      (is (= #{:file :ns :form_count :line_count} (set (keys entry)))
          "a names entry must be exactly {file, ns, form_count, line_count}")))
  (testing "grep flips the default back to text"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "grep" "ns"
                         "limit" 16384})]
      (is (= "text" (:format response)))
      (is (string? (:tree response)))
      (is (nil? (:files response)))))
  (testing "text and edn remain reachable on explicit request regardless of grep"
    (is (= "text" (:format (run {"mode" "ls-tree" "dir" fixture-dir
                                 "format" "text" "limit" 16384}))))
    (is (= "edn" (:format (run {"mode" "ls-tree" "dir" fixture-dir
                                "format" "edn" "limit" 16384}))))))

;; ============================================================
;; A many-file scratch fixture, created and torn down per test, so the
;; default-limit/names fit and the grep-vs-ns_grep witnesses run against a
;; real-shaped tree without committing hundreds of fixture files.
;; ============================================================

(defn- write-clj-file!
  [path ns-form & body-lines]
  (fs/create-dirs (fs/parent path))
  (spit (str path)
       (str/join "\n" (concat [ns-form] body-lines))))

(defn- with-scratch-project
  "Populate dir (project-relative to project-root) via build!, run thunk,
   then always delete it — even if the assertions throw."
  [rel-path build! thunk]
  (let [dir (str (fs/path project-root rel-path))]
    (try
      (build! dir)
      (thunk)
      (finally
        (fs/delete-tree dir)))))

;; @spec MCP-OP-STUDY-011
;; @spec MCP-OP-STUDY-007
(deftest ls-tree-names-default-fits-a-many-file-tree-well-inside-the-ceiling
  ;; The field evidence this fixes: over a 116-file directory, the old
  ;; text-only rendering stayed truncated (`read_complete=false`) even at the
  ;; 16384-character ceiling, returning only 13 of 116 files with
  ;; `next_action=narrow_scope` and no executable continuation — permanently
  ;; out of reach. `names` must make a LARGER (190-file) tree fully reachable
  ;; at that same ceiling, via the normal bounded-receipt continuation.
  (with-scratch-project
    "test-fixtures/study/scratch-190"
    (fn [dir]
      (dotimes [i 190]
        (write-clj-file!
          (str dir "/src/scratch/ns" i ".clj")
          (format "(ns scratch.ns%d)" i)
          (format "(defn f%d [] :ok)" i))))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-190"
            at-default (run {"mode" "ls-tree" "dir" rel-dir})]
        (testing "names is the default rendering, and the default limit alone bounds it"
          (is (true? (:ok at-default)))
          (is (= "names" (:format at-default)))
          (is (= 190 (:file_count at-default)))
          (is (= 4096 (:limit at-default)))
          (when (:truncated at-default)
            (is (= "raise_limit_or_narrow_scope" (:next_action at-default)))
            (is (= "inspect_clojure" (get-in at-default [:next_call :tool])))))
        (testing "raising to the ceiling completes the whole 190-file tree"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir "limit" 16384})]
            (is (true? (:ok response)))
            (is (= "names" (:format response)))
            (is (true? (:read_complete response))
                "190 names-format entries must fit well inside the 16384 ceiling")
            (is (false? (:truncated response)))
            (is (= 190 (:file_count response)))
            (is (= 190 (:returned response)))
            (is (= 190 (count (:files response))))
            (is (<= (inspect/json-character-count (:files response)) 16384))
            (doseq [entry (:files response)]
              (is (= #{:file :ns :form_count :line_count} (set (keys entry)))))))))))

;; @spec MCP-OP-STUDY-012
(deftest ls-tree-ns-grep-filters-by-path-not-content
  (with-scratch-project
    "test-fixtures/study/scratch-ns-grep"
    (fn [dir]
      ;; Two real matches: path/namespace names folds or store.
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/folds.clj")
                       "(ns cfp-scheduler-killer.folds)"
                       "(defn fold-matches [] :ok)")
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/store.clj")
                       "(ns cfp-scheduler-killer.store)"
                       "(defn save! [] :ok)")
      ;; Decoys: content mentions folds/store, but the path/namespace does not.
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/scheduler.clj")
                       "(ns cfp-scheduler-killer.scheduler)"
                       ";; restore the schedule store on boot"
                       "(defn boot [] :ok)")
      (write-clj-file! (str dir "/src/cfp_scheduler_killer/api.clj")
                       "(ns cfp-scheduler-killer.api)"
                       ";; handles folds of matching requests"
                       "(defn handler [] :ok)"))
    (fn []
      (let [rel-dir "test-fixtures/study/scratch-ns-grep"]
        (testing "ns_grep narrows to the two path/namespace matches only"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "ns_grep" "folds|store" "format" "edn"})]
            (is (true? (:ok response)))
            (is (= 2 (:file_count response)))
            (is (= #{"src/cfp_scheduler_killer/folds.clj"
                     "src/cfp_scheduler_killer/store.clj"}
                   (set (map :file (:files response)))))))
        (testing "content grep over-matches the same pattern, including the decoys"
          (let [response (run {"mode" "ls-tree" "dir" rel-dir
                               "grep" "folds|store" "format" "edn"})]
            (is (true? (:ok response)))
            (is (= 4 (:file_count response))
                "grep matches file bodies, so both decoy comments count too")))))))

;; ============================================================
;; Confinement
;; ============================================================

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-outside-the-workspace-without-scanning
  (doseq [[dir expected]
          [["../../etc" "invalid-relative-directory-path"]
           ["/etc" "invalid-relative-directory-path"]
           ["src/../.." "invalid-relative-directory-path"]
           ["src/does-not-exist" "directory-not-found"]
           [real-file "path-not-directory"]]]
    (testing dir
      (let [response (run {"mode" "ls-tree" "dir" dir})]
        (is (false? (:ok response)))
        (is (= expected (:error_type response)))
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (= "correct_request" (:next_action response)))
        (is (= "inspect_clojure" (get-in response [:next_call :tool])))
        (is (nil? (:tree response)))
        (is (nil? (:files response)))))))

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-a-directory-without-clojure-sources
  (let [response (run {"mode" "ls-tree" "dir" "docs/intent/study-ops"})]
    (is (false? (:ok response)))
    (is (= "no-clojure-files" (:error_type response)))
    (is (str/starts-with? (:error response) "No Clojure files found under "))
    (is (some? (:next_call response)))))

;; @spec MCP-OP-STUDY-006
(deftest ls-tree-refuses-a-flag-shaped-grep-or-ns-grep-over-the-wire
  ;; A pattern beginning with '-' (e.g. "--pre=/bin/sh") would otherwise
  ;; reach ripgrep looking like a flag; must refuse before any scan runs.
  (testing "grep"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir
                         "grep" "--pre=/bin/sh"})]
      (is (false? (:ok response)))
      (is (= "invalid-grep-pattern" (:error_type response)))
      (is (false? (:read_complete response)))
      (is (some? (:next_call response)))))
  (testing "ns_grep"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "ns_grep" "-x"})]
      (is (false? (:ok response)))
      (is (= "invalid-ns-grep-pattern" (:error_type response)))
      (is (false? (:read_complete response))))))

;; ============================================================
;; Bounding
;; ============================================================

;; @spec MCP-OP-STUDY-007
(deftest a-study-receipt-is-bounded-and-says-so
  (testing "a tight limit truncates at row granularity with a continuation"
    (let [response (one "deps" {"limit" 200})
          result (result-of response)]
      (is (true? (:truncated result)))
      (is (pos? (:omitted result)))
      (is (< (:returned result) (:form_count result)))
      (is (<= (inspect/json-character-count (:deps result)) 200))
      (is (= "raise_limit_or_narrow_scope" (:next_action result)))
      (is (= "inspect_clojure" (get-in result [:next_call :tool])))
      (testing "and the batch refuses to call itself terminal"
        (is (false? (:read_complete response)))
        (is (true? (:truncated response)))
        (is (= "raise_limit_or_narrow_scope" (:next_action response))))))

  (testing "an atomic tree refuses rather than returning half a tree"
    (let [response (one "ls-deps" {"form" "extraction-closure" "limit" 50})]
      (is (false? (:ok response)))
      (is (= "study-output-limit" (:error_type response)))
      (is (= 50 (:limit response)))
      (is (pos? (:required response)))
      (is (= "inspect_clojure" (get-in response [:next_call :tool])))))

  (testing "ls-tree truncates whole files and stays a valid tree"
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                         "limit" 700})]
      (is (true? (:truncated response)))
      (is (false? (:read_complete response)))
      (is (< (:returned response) (:file_count response)))
      (is (<= (count (:tree response)) 700))
      (is (str/includes? (:tree response) "total:"))
      (is (= 16384 (get-in response [:next_call :arguments :limit])))))

  (testing "at the maximum limit no continuation is served that cannot advance"
    (let [response (run {"mode" "ls-tree" "dir" "src" "format" "text"
                         "limit" 16384})]
      (is (true? (:truncated response)))
      (is (nil? (:next_call response))
          "an executable call identical to the one just made is a loop")
      (is (= "narrow_scope" (:next_action response)))
      (is (string? (:remedy response))))
    ;; No single file in this repository has a deps result over 16384
    ;; characters, so the ceiling is lowered to reach the same branch on real
    ;; bytes rather than asserted on a fixture that cannot occur.
    (with-redefs [inspect/study-max-limit 200]
      (let [result (result-of (one "deps" {"limit" 200}))]
        (is (true? (:truncated result)))
        (is (nil? (:next_call result)))
        (is (= "narrow_scope" (:next_action result)))
        (is (string? (:remedy result))))))

  (testing "a limit above the maximum refuses"
    (let [response (one "deps" {"limit" 99999})]
      (is (false? (:ok response)))
      (is (= "invalid-study-limit" (:reason response))))
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 99999})]
      (is (false? (:ok response)))
      (is (= "invalid-study-limit" (:error_type response))))))

;; ============================================================
;; Refusals
;; ============================================================

;; @spec MCP-OP-STUDY-010
(deftest a-missing-owner-refuses-with-a-factual-vocabulary
  (doseq [operation ["deps" "ls-deps" "ls-extract"]]
    (testing operation
      (let [response (one operation {"form" "no-such-form"})]
        (is (false? (:ok response)))
        (is (= "study-form-not-found" (:error_type response)))
        (is (= "no-such-form" (:form response)))
        (is (false? (:read_complete response)))
        (is (true? (:source_unchanged response)))
        (is (seq (:available_owners response)))
        (is (pos? (:available_owner_count response)))
        (is (= "REPLACE-WITH-ONE-EXACT-OWNER"
               (get-in response [:next_call :arguments :requests 0 :form])))))))

;; @spec MCP-OP-STUDY-010
(deftest an-unknown-study-operation-names-the-complete-vocabulary
  (let [response (one "ls-mv" {})]
    (is (false? (:ok response)))
    (is (= "unknown-operation" (:reason response)))
    (is (= ["deps" "forms" "ls-deps" "ls-extract" "match" "outline" "topo"
            "xray"]
           (:supported response)))))

;; ============================================================
;; One kernel
;; ============================================================

;; @spec MCP-OP-STUDY-008
(deftest study-ops-both-entrances-call-one-kernel
  (let [source (slurp real-file)
        big {"limit" 16384}]
    (testing "deps"
      (let [kernel (study/deps source {})]
        (is (= (inspect/json-data kernel)
               (:deps (result-of (one "deps" big)))))
        (is (= kernel (core/run-deps {:file real-file})))))

    (testing "deps for one owner"
      (let [kernel (study/deps source {:form "dep-tree"})]
        (is (= (inspect/json-data kernel)
               (:deps (result-of (one "deps" (assoc big "form" "dep-tree"))))))
        (is (= kernel (core/run-deps {:file real-file :form "dep-tree"})))))

    (testing "topo"
      (let [kernel (study/topo source)]
        (is (= (inspect/json-data kernel)
               (:topo (result-of (one "topo" big)))))
        (is (= kernel (core/run-topo {:file real-file})))))

    (testing "ls-deps"
      (let [kernel (study/ls-deps source {:form "extraction-closure"})]
        (is (= (inspect/json-data kernel)
               (:dep_tree
                 (result-of
                   (one "ls-deps" (assoc big "form" "extraction-closure"))))))
        (is (= kernel
               (core/run-ls-deps {:file real-file
                                  :form "extraction-closure"})))))

    (testing "ls-extract"
      (let [kernel (study/ls-extract source {:form "extraction-closure"})]
        (is (= (inspect/json-data kernel)
               (:closure
                 (result-of
                   (one "ls-extract"
                        (assoc big "form" "extraction-closure"))))))
        (is (= kernel
               (core/run-closure {:file real-file
                                  :form "extraction-closure"})))))

    (testing "ls-tree renders through the one formatter"
      (let [scan (study/ls-tree {:dir fixture-dir})
            response (run {"mode" "ls-tree" "dir" fixture-dir "format" "text"
                           "limit" 16384})]
        (is (true? (:ok scan)))
        (is (= (study/format-ls-tree-text (:projects scan) (:dir scan))
               (:tree response)))
        (is (= (inspect/json-data
                 (study/format-ls-tree-edn (:projects scan) (:dir scan)))
               (:files (run {"mode" "ls-tree" "dir" fixture-dir
                             "format" "edn" "limit" 16384}))))
        (is (= (inspect/json-data
                 (study/format-ls-tree-names (:projects scan) (:dir scan)))
               (:files (run {"mode" "ls-tree" "dir" fixture-dir
                             "limit" 16384})))
            "names is the default rendering with no grep, and is the same one formatter")))))

;; ============================================================
;; No write authority
;; ============================================================

;; @spec MCP-OP-STUDY-009
(deftest the-read-entrance-exposes-no-write-operation
  (let [inspect-tool-def (first (filter #(= "inspect_clojure" (:name %))
                                        (mcp-tool/all-tools)))
        request-variants (get-in inspect-tool/inspect-schema
                                 [:properties "requests" :items :oneOf])
        operations (set (keep #(get-in % [:properties "operation" :const])
                              request-variants))
        modes (set (get-in inspect-tool/inspect-schema
                           [:properties "mode" :enum]))]
    (is (true? (get-in inspect-tool-def [:annotations :read-only])))
    (is (false? (get-in inspect-tool-def [:annotations :destructive])))
    (is (= #{"forms" "outline" "match" "xray"
             "deps" "topo" "ls-deps" "ls-extract"}
           operations))
    (is (= #{"prepare-change" "plan-extraction" "ls-tree"} modes))
    (doseq [write-op ["mv" "mv-with-deps" "rename-ns" "rename-ns!"
                      "fix-declares" "fix-declares!" "extract!"
                      "replace-subform!"]]
      (is (not (contains? operations write-op)))
      (is (not (contains? modes write-op))))))

;; @spec MCP-OP-STUDY-001
(deftest the-public-tool-catalog-did-not-grow
  (is (= #{"inspect_clojure" "apply_clojure_changes" "edit_clojure"
           "transform_clojure"}
         (set (map :name (mcp-tool/all-tools))))))
