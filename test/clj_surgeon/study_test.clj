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

;; @spec MCP-OP-STUDY-001
(deftest ls-tree-refuses-a-flag-shaped-grep-or-ns-grep-pattern
  ;; A pattern beginning with '-' would otherwise reach rg/grep looking like
  ;; a flag (e.g. "--pre=/bin/sh" runs an arbitrary preprocessor command).
  ;; Refused before any subprocess runs, never silently reinterpreted.
  (testing "grep"
    (let [result (study/ls-tree {:dir fixture-dir :grep "--pre=/bin/sh"})]
      (is (false? (:ok result)))
      (is (= :invalid-grep-pattern (:error-type result)))
      (is (str/includes? (:error result) "must not start with"))))
  (testing "ns-grep"
    (let [result (study/ls-tree {:dir fixture-dir :ns-grep "-x"})]
      (is (false? (:ok result)))
      (is (= :invalid-ns-grep-pattern (:error-type result)))
      (is (str/includes? (:error result) "must not start with")))))

;; @spec MCP-OP-STUDY-001
(deftest grep-tree-argv-always-separates-the-pattern-with-double-dash
  ;; Defense in depth alongside the leading-dash refusal above: even a
  ;; pattern that is not flag-shaped must never be adjacent to an
  ;; unseparated argv, so a caller cannot smuggle an rg/grep flag by any
  ;; other means. Captures the real subprocess argv via with-redefs rather
  ;; than asserting on live rg/grep output.
  (let [captured (atom nil)
        grep-tree #'study/grep-tree]
    (with-redefs [babashka.process/shell
                  (fn [_opts & args]
                    (if (= ["rg" "--version"] (vec args))
                      {:exit 0 :out "ripgrep 14.0.0" :err ""}
                      (do (reset! captured (vec args))
                          {:exit 1 :out "" :err ""})))]
      (grep-tree "some-pattern" "/tmp/somewhere"))
    (is (some? @captured) "the real rg invocation must have been captured")
    (let [args @captured
          dash-dash-idx (.indexOf ^java.util.List args "--")
          pattern-idx (.indexOf ^java.util.List args "some-pattern")]
      (is (not= -1 dash-dash-idx))
      (is (= (inc dash-dash-idx) pattern-idx)
          "the pattern must immediately follow the -- argv separator"))))

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
