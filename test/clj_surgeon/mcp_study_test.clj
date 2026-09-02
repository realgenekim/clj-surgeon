(ns clj-surgeon.mcp-study-test
  "MCP read-entrance witnesses for the study operations.

   @spec MCP-OP-STUDY-001 MCP-OP-STUDY-002 MCP-OP-STUDY-003
   @spec MCP-OP-STUDY-004 MCP-OP-STUDY-005 MCP-OP-STUDY-006
   @spec MCP-OP-STUDY-007 MCP-OP-STUDY-008 MCP-OP-STUDY-009
   @spec MCP-OP-STUDY-010"
  (:require
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
  (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 16384})]
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
    (let [response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 700})]
      (is (true? (:truncated response)))
      (is (false? (:read_complete response)))
      (is (< (:returned response) (:file_count response)))
      (is (<= (count (:tree response)) 700))
      (is (str/includes? (:tree response) "total:"))
      (is (= 16384 (get-in response [:next_call :arguments :limit])))))

  (testing "at the maximum limit no continuation is served that cannot advance"
    (let [response (run {"mode" "ls-tree" "dir" "src" "limit" 16384})]
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
            response (run {"mode" "ls-tree" "dir" fixture-dir "limit" 16384})]
        (is (true? (:ok scan)))
        (is (= (study/format-ls-tree-text (:projects scan) (:dir scan))
               (:tree response)))
        (is (= (inspect/json-data
                 (study/format-ls-tree-edn (:projects scan) (:dir scan)))
               (:files (run {"mode" "ls-tree" "dir" fixture-dir
                             "format" "edn" "limit" 16384}))))))))

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
