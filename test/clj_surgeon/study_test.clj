(ns clj-surgeon.study-test
  "Kernel witnesses for the read-only study operations.

   @spec MCP-OP-STUDY-002 MCP-OP-STUDY-003 MCP-OP-STUDY-004
   @spec MCP-OP-STUDY-005 MCP-OP-STUDY-008 MCP-OP-STUDY-009"
  (:require
   [babashka.process :as process]
   [clj-surgeon.core :as core]
   [clj-surgeon.study :as study]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private real-file "src/clj_surgeon/analyze.clj")
(def ^:private fixture-dir "test-fixtures/cljc/existing-ops")
(def ^:private golden-file "test-fixtures/study/ls-tree-existing-ops.golden.txt")

;; ============================================================
;; The kernel takes data and returns data
;; ============================================================

;; @spec MCP-OP-STUDY-002
(deftest deps-kernel-returns-the-call-graph-and-one-owner-row
  (let [source (slurp real-file)
        rows (study/deps source {})]
    (is (vector? rows))
    (is (seq rows))
    (is (every? #(and (string? (:name %)) (set? (:depends-on %))) rows))
    (testing "one owner"
      (let [row (study/deps source {:form "dep-tree"})]
        (is (= "dep-tree" (:name row)))
        (is (= "defn" (:type row)))))
    (testing "an absent owner is nil, never an invented row"
      (is (nil? (study/deps source {:form "no-such-form"}))))))

;; @spec MCP-OP-STUDY-003
(deftest topo-kernel-reports-order-and-cycles
  (let [result (study/topo (slurp real-file))]
    (is (vector? (:sorted result)))
    (is (vector? (:cycles result)))
    (is (contains? result :has-cycles?))))

;; @spec MCP-OP-STUDY-004
(deftest ls-deps-kernel-returns-a-tree-or-nil
  (let [source (slurp real-file)]
    (is (= "extraction-closure"
           (:name (study/ls-deps source {:form "extraction-closure"}))))
    (is (nil? (study/ls-deps source {:form "no-such-form"})))))

;; @spec MCP-OP-STUDY-005
(deftest ls-extract-kernel-returns-the-minimal-closure
  (let [result (study/ls-extract (slurp real-file)
                                 {:form "extraction-closure"})]
    (is (= "extraction-closure" (:target result)))
    (is (= ["extraction-closure"] (mapv :name (:forms result))))))

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-kernel-returns-typed-refusals-without-printing
  (testing "a missing dir is data, not a println"
    (let [result (study/ls-tree {})]
      (is (false? (:ok result)))
      (is (= :missing-dir (:error-type result)))
      (is (= "Error: :dir is required for :ls-tree" (:error result)))))
  (testing "a directory with no Clojure sources refuses with its message"
    (let [result (study/ls-tree {:dir "docs/intent/study-ops"})]
      (is (false? (:ok result)))
      (is (= :no-clojure-files (:error-type result)))
      (is (str/starts-with? (:error result) "No Clojure files found under "))))
  (testing "a real tree returns outlined projects"
    (let [result (study/ls-tree {:dir fixture-dir})]
      (is (true? (:ok result)))
      (is (= 7 (reduce + 0 (map #(count (:outlines %)) (:projects result))))))))

;; ============================================================
;; One kernel: the CLI handler adds print, never a second answer
;; ============================================================

;; @spec MCP-OP-STUDY-008
(deftest cli-handlers-are-the-kernel-plus-nothing
  (let [source (slurp real-file)]
    (is (= (study/deps source {}) (core/run-deps {:file real-file})))
    (is (= (study/deps source {:form "dep-tree"})
           (core/run-deps {:file real-file :form "dep-tree"})))
    (is (= (study/topo source) (core/run-topo {:file real-file})))
    (is (= (study/ls-deps source {:form "extraction-closure"})
           (core/run-ls-deps {:file real-file :form "extraction-closure"})))
    (is (= (study/ls-extract source {:form "extraction-closure"})
           (core/run-closure {:file real-file :form "extraction-closure"})))))

;; @spec MCP-OP-STUDY-008
(deftest cli-ls-tree-bytes-match-the-frozen-golden
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" fixture-dir)]
    (is (zero? (:exit result)))
    (is (= (slurp golden-file) (:out result)))))

;; ============================================================
;; The format shadow that made a documented refusal unreachable
;; ============================================================

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-refusal-message-is-reachable
  ;; Regression: run-ls-tree destructured `format` as a local, shadowing
  ;; clojure.core/format, so the "No Clojure files found" branch threw a
  ;; NullPointerException and the CLI reported {:error nil}.
  (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "-m" "clj-surgeon.core"
                              ":op" ":ls-tree" ":dir" "docs/intent/study-ops")]
    (is (= 1 (:exit result)))
    (is (str/starts-with? (:out result) "No Clojure files found under "))
    (is (not (str/includes? (:out result) ":error nil")))))

;; ============================================================
;; No write authority
;; ============================================================

;; @spec MCP-OP-STUDY-009
(deftest the-study-kernel-exposes-no-write-operation
  (let [public-names (set (map name (keys (ns-publics 'clj-surgeon.study))))]
    (is (empty? (filter #(str/ends-with? % "!") public-names))
        "a bang-suffixed public in the study kernel would be a write reachable from the read entrance")
    (is (empty? (filter #{"mv" "rename-ns" "fix-declares"} public-names)))))
