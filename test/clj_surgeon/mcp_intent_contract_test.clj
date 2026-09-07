(ns clj-surgeon.mcp-intent-contract-test
  {:lane :fast}
  (:require
   [clj-surgeon.mcp-intent-contract]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; RATCHET (2026-09-04, inb-9483a4): `temp-dir` below never deleted what it
;; created (surgeon-intent-scan / surgeon-intent-empty). Track and sweep.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(defn- audit-contract
  [input]
  ((requiring-resolve 'clj-surgeon.mcp-intent-contract/audit-contract) input))

(defn- violations
  [result]
  (set (map #(select-keys % [:type :intent :source-kind])
            (:violations result))))

(defn- spec-line
  [status intent]
  (str "- [" status "] **" intent "**: fixture requirement\n"))

(defn- annotation
  [intent]
  (str ";; @" "spec " intent "\n"))

;; @spec MCP-OP-TRACE-005
(deftest non-mcp-intent-with-missing-witnesses-is-reported
  ;; September 7 audit gap 1: a literal WTL-shaped fixture, not live WTL edits.
  (let [intent "WTL-FIXTURE-001"]
    (is (= [{:type :missing-implementation-witness
             :intent intent :source-kind :implementation}
            {:type :missing-test-witness
             :intent intent :source-kind :test}]
           (:violations
             (audit-contract {:spec-text (spec-line "x" intent)
                              :implementation-sources {}
                              :test-sources {}}))))))

;; @spec MCP-OP-TRACE-005
(deftest prefix-agnostic-witness-rules-preserve-the-legacy-contract
  (doseq [intent ["MCP-OP-FIXTURE-001" "MCP-OP-LEGACY-NONNUMERIC"
                  "MCP-OP-LEGACY-1" "WTL-APPLY-001" "PERF-SENT-TIME-001"
                  "OP-ALG-COMMIT-001" "TEST-ISO-003" "MEASURE-WALL-001"
                  "TELEMETRY-EVENTS-001" "FUTURE2-001" "A-001"]
          [status implementation? test? expected]
          [["x" false false [:missing-implementation-witness :missing-test-witness]]
           ["x" true false [:missing-test-witness]]
           ["x" false true [:missing-implementation-witness]]
           ["x" true true []]
           [" " false false [:missing-test-witness]]
           [" " true false [:missing-test-witness]]
           [" " false true []]
           [" " true true []]
           ["D" false false []]
           ["D" true false []]
           ["D" false true []]
           ["D" true true []]]]
    (testing (str [intent status implementation? test?])
      (let [result (audit-contract
                     {:spec-text (spec-line status intent)
                      :implementation-sources (if implementation? {"f.clj" (annotation intent)} {})
                      :test-sources (if test? {"t.clj" (annotation intent)} {})})]
        (is (= [intent] (vec (keys (:specs result)))))
        (is (= expected (mapv :type (:violations result))))
        (is (= (empty? expected) (:ok result))))))
  (doseq [intent ["WTL-UNKNOWN-999" "FUTURE2-999" "TELEMETRY-EVENTS-001"]]
    (is (= [{:type :unknown-intent-witness :intent intent :source-kind :implementation}
            {:type :unknown-intent-witness :intent intent :source-kind :test}]
           (:violations
             (audit-contract {:spec-text ""
                              :implementation-sources {"f.clj" (annotation intent)}
                              :test-sources {"t.clj" (annotation intent)}}))))))

;; @spec MCP-OP-TRACE-005
(deftest generic-witnesses-require-complete-numeric-identifiers
  (doseq [intent ["PERF-SENT-" "TEST-ISO-" "WTL-0012" "WTL-001-MORE" "WTL-01"]]
    (is (empty? (:implementation-witnesses
                  (audit-contract {:spec-text ""
                                   :implementation-sources {"f.clj" (annotation intent)}
                                   :test-sources {}})))))
  (is (= #{"TEST-ISO-002"}
         (:implementation-witnesses
           (audit-contract {:spec-text ""
                            :implementation-sources {"f.clj" (annotation "TEST-ISO-002/003/004")}
                            :test-sources {}})))))

;; @spec MCP-OP-TRACE-005
(deftest legacy-mcp-amendment-spelling-keeps-its-historical-result
  ;; The legacy row parser ignores lowercase suffixes; its witness parser reads
  ;; the uppercase/numeric stem. Widening must not reinterpret that MCP behavior.
  (is (= {:ok false :specs {}
          :implementation-witnesses #{"MCP-OP-FIXTURE-001"}
          :test-witnesses #{}
          :violations [{:type :unknown-intent-witness
                        :intent "MCP-OP-FIXTURE-001" :source-kind :implementation}]}
         (audit-contract {:spec-text (spec-line "x" "MCP-OP-FIXTURE-001a")
                          :implementation-sources {"f.clj" (annotation "MCP-OP-FIXTURE-001a")}
                          :test-sources {}}))))

;; @spec MCP-OP-TRACE-005
(deftest amendment-identifiers-are-not-parent-witnesses
  (let [parent "TEST-ISO-001"
        amendment "TEST-ISO-001a"
        result (audit-contract
                 {:spec-text (str (spec-line "x" parent) (spec-line "x" amendment))
                  :implementation-sources {"f.clj" (annotation amendment)}
                  :test-sources {"t.clj" (annotation amendment)}})]
    (is (= {parent :implemented amendment :implemented} (:specs result)))
    (is (= #{amendment} (:implementation-witnesses result)))
    (is (= #{amendment} (:test-witnesses result)))
    (is (= [{:type :missing-implementation-witness :intent parent :source-kind :implementation}
            {:type :missing-test-witness :intent parent :source-kind :test}]
           (:violations result)))))

;; @spec MCP-OP-TRACE-005
(deftest explicit-prefix-debt-never-hides-unknown-or-new-prefixes
  (let [defer-missing-witnesses
        (requiring-resolve 'clj-surgeon.mcp-intent-contract/defer-missing-witnesses)
        debt-id "WTL-FIXTURE-001"
        future-id "FUTURE2-001"
        unknown-id "WTL-UNKNOWN-999"
        raw (audit-contract
              {:spec-text (str (spec-line "x" debt-id) (spec-line "x" future-id))
               :implementation-sources {"f.clj" (annotation unknown-id)}
               :test-sources {"t.clj" (annotation unknown-id)}})
        allowed {"WTL-" "TODO: fixture debt"}
        gated (defer-missing-witnesses raw allowed)]
    (is (= raw (defer-missing-witnesses raw {})))
    (is (false? (:ok gated)))
    (is (= [{:type :missing-implementation-witness :intent debt-id :source-kind :implementation}
            {:type :missing-test-witness :intent debt-id :source-kind :test}]
           (:pending-witness-violations gated)))
    (is (= [{:type :missing-implementation-witness :intent future-id :source-kind :implementation}
            {:type :missing-test-witness :intent future-id :source-kind :test}
            {:type :unknown-intent-witness :intent unknown-id :source-kind :implementation}
            {:type :unknown-intent-witness :intent unknown-id :source-kind :test}]
           (:violations gated)))
    (is (= (select-keys raw [:specs :implementation-witnesses :test-witnesses])
           (select-keys gated [:specs :implementation-witnesses :test-witnesses])))
    (is (= allowed (:missing-witness-prefix-allowlist gated)))
    (let [only-debt (audit-contract {:spec-text (spec-line "x" debt-id)
                                     :implementation-sources {} :test-sources {}})]
      (is (:ok (defer-missing-witnesses only-debt allowed)))
      (is (false? (:ok only-debt))))))

;; @spec MCP-OP-TRACE-001
(deftest active-gap-requires-a-direct-test-witness
  (let [intent "MCP-OP-FIXTURE-001"
        spec (spec-line " " intent)]
    (is (= #{{:type :missing-test-witness
              :intent intent
              :source-kind :test}}
           (violations
             (audit-contract {:spec-text spec
                              :implementation-sources {}
                              :test-sources {}}))))
    (is (:ok (audit-contract
               {:spec-text spec
                :implementation-sources {}
                :test-sources {"fixture_test.clj" (annotation intent)}})))))

;; @spec MCP-OP-TRACE-002
(deftest unknown-implementation-and-test-annotations-are-rejected
  (let [known "MCP-OP-FIXTURE-001"
        unknown "MCP-OP-FIXTURE-999"
        result (audit-contract
                 {:spec-text (spec-line "D" known)
                  :implementation-sources
                  {"fixture.clj" (annotation unknown)}
                  :test-sources
                  {"fixture_test.clj" (annotation unknown)}})]
    (is (= #{{:type :unknown-intent-witness
              :intent unknown
              :source-kind :implementation}
             {:type :unknown-intent-witness
              :intent unknown
              :source-kind :test}}
           (violations result)))))

;; @spec MCP-OP-TRACE-003
(deftest implemented-intent-requires-both-implementation-and-test-witnesses
  (let [intent "MCP-OP-FIXTURE-001"
        spec (spec-line "x" intent)]
    (is (= #{{:type :missing-implementation-witness
              :intent intent
              :source-kind :implementation}
             {:type :missing-test-witness
              :intent intent
              :source-kind :test}}
           (violations
             (audit-contract {:spec-text spec
                              :implementation-sources {}
                              :test-sources {}}))))
    (is (:ok
          (audit-contract
            {:spec-text spec
             :implementation-sources {"fixture.clj" (annotation intent)}
             :test-sources {"fixture_test.clj" (annotation intent)}})))))

;; @spec MCP-OP-TRACE-004
(deftest deferred-intent-needs-no-placeholder-witness
  (let [result (audit-contract
                 {:spec-text (spec-line "D" "MCP-OP-FIXTURE-001")
                  :implementation-sources {}
                  :test-sources {}})]
    (is (:ok result))
    (is (empty? (:violations result)))))

(deftest repository-operation-intent-contract-is-coherent
  (let [audit-current-repository
        (requiring-resolve
          'clj-surgeon.mcp-intent-contract/audit-current-repository)
        result (audit-current-repository)]
    (is (:ok result) (pr-str (:violations result)))
    (is (= #{"MEASURE-" "OP-ALG-" "PERF-SENT-" "TEST-ISO-" "WTL-"}
           (set (keys (:missing-witness-prefix-allowlist result)))))
    (doseq [[prefix reason] (:missing-witness-prefix-allowlist result)]
      (is (str/starts-with? reason "TODO:"))
      (is (some #(str/starts-with? (:intent %) prefix)
                (:pending-witness-violations result))
          (str "remove repaired prefix exception: " prefix)))
    (let [unrestricted (audit-current-repository "." {})]
      (is (false? (:ok unrestricted)))
      (is (= (:pending-witness-violations result) (:violations unrestricted)))
      (is (= (:specs result) (:specs unrestricted))))))

;; ---------------------------------------------------------------------------
;; The spec-document registry is DERIVED, not listed.
;;
;; Ratchet, 2026-09-03 (integration branch): the audited spec documents used to
;; live in a literal vector inside `audit-current-repository`. Every lane that
;; added an intent leaf appended a line to that one vector, so every lane
;; conflicted with every other lane by construction. The list is now scanned from
;; docs/intent/<leaf>/<name>-specs.md, so a new lane adds a FILE and touches no
;; shared line. These witnesses keep the scan honest: pickup, loud failure on an
;; orphan listing, named reasons for every exclusion, and an EXACT expected set
;; so drift is visible rather than silent.
;; ---------------------------------------------------------------------------

(defn- spec-doc-paths
  ([] ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths)))
  ([root] ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths) root))
  ([root excluded]
   ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths) root excluded)))

(defn- excluded-spec-docs
  []
  @(requiring-resolve 'clj-surgeon.mcp-intent-contract/excluded-spec-docs))

(defn- spec-ids
  "The intent IDs the audit would parse out of these repo-relative files."
  [root paths]
  (set (keys (:specs (audit-contract
                       {:spec-text (str/join
                                     "\n"
                                     (map #(slurp (io/file root %)) paths))
                        :implementation-sources {}
                        :test-sources {}})))))

(defn- temp-dir
  [prefix]
  (str (tmp-leak/track!
         temp-roots
         (java.nio.file.Files/createTempDirectory
           prefix (into-array java.nio.file.attribute.FileAttribute [])))))

(deftest a-new-intent-leaf-is-picked-up-by-adding-only-a-file
  (testing "a lane adds docs/intent/<leaf>/<leaf>-specs.md and nothing else"
    (let [root (temp-dir "surgeon-intent-scan")
          leaf (io/file root "docs" "intent" "temp-lane")]
      (.mkdirs leaf)
      (spit (io/file leaf "temp-lane-specs.md")
            (str (spec-line "x" "MCP-OP-TEMPLANE-001")
                 (spec-line "x" "FUTURE2-001")))
      ;; a sibling that is NOT a spec document, and a `-specs.from-*.md` variant,
      ;; must both be ignored.
      (spit (io/file leaf "temp-lane-design.md") "design\n")
      (spit (io/file leaf "temp-lane-specs.from-docs--x.md")
            (spec-line "x" "MCP-OP-TEMPLANE-999"))
      (is (= ["docs/intent/temp-lane/temp-lane-specs.md"] (spec-doc-paths root {})))
      (is (= #{"MCP-OP-TEMPLANE-001" "FUTURE2-001"}
             (spec-ids root (spec-doc-paths root {})))))))

(deftest an-orphan-spec-doc-listing-fails-loudly
  (testing "an exclusion naming a file that does not exist throws, never shrinks silently"
    (let [thrown (try
                   (spec-doc-paths "." {"docs/intent/no-such-leaf/no-such-leaf-specs.md"
                                        "deliberately absent fixture"})
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
      (is (some? thrown) "an orphan listing must throw")
      (is (= :orphan-spec-doc-listing (:type (ex-data thrown))))
      (is (= ["docs/intent/no-such-leaf/no-such-leaf-specs.md"]
             (:paths (ex-data thrown)))))))

(deftest an-empty-intent-tree-fails-loudly
  (testing "a moved or emptied docs/intent is an error, not an empty audit"
    (let [root (temp-dir "surgeon-intent-empty")
          thrown (try (spec-doc-paths root {}) nil
                      (catch clojure.lang.ExceptionInfo e e))]
      (is (some? thrown))
      (is (= :no-spec-docs-found (:type (ex-data thrown)))))))

(deftest every-spec-doc-exclusion-carries-a-named-reason
  (testing "the exclusion set is non-empty only for named, existing reasons"
    (doseq [[path reason] (excluded-spec-docs)]
      (is (string? path))
      (is (.isFile (io/file "." path))
          (str "an excluded spec document must exist: " path))
      (is (and (string? reason) (<= 40 (count (str/trim reason))))
          (str "exclusion needs a substantive one-line reason: " path)))))

(def ^:private expected-spec-docs
  "The spec documents the scan is expected to find at this HEAD, asserted exactly so
   that drift in docs/intent is VISIBLE rather than silent. A lane that adds an intent
   leaf adds one line here and one line to `lanes-added-since-derivation` below --
   in the WITNESS, never in the production registry, which is what the ratchet was for."
  ["docs/intent/2026-08-29-ratification/measurement-evidence-specs.md"
   "docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.md"
   "docs/intent/alias-migration/alias-migration-specs.md"
   "docs/intent/feature-thread/feature-thread-specs.md"
   "docs/intent/helper-extraction/helper-extraction-specs.md"
   "docs/intent/hot-verification/hot-verification-specs.md"
   "docs/intent/insertion-boundary-and-gap/insertion-boundary-and-gap-specs.md"
   "docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md"
   "docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md"
   "docs/intent/memory-boundedness/memory-boundedness-specs.md"
   "docs/intent/memory/memory-transaction-specs.md"
   "docs/intent/operation-algebra/operation-algebra-specs.md"
   "docs/intent/performance-regression-sentinel/performance-regression-sentinel-specs.md"
   "docs/intent/prepared-request-actions/prepared-request-actions-specs.md"
   "docs/intent/prepared-request/prepared-request-specs.md"
   "docs/intent/read-path-memory/read-path-memory-specs.md"
   "docs/intent/read-request-normalization/read-request-normalization-specs.md"
   "docs/intent/relation-census/relation-census-specs.md"
   "docs/intent/shell-argv-safety/shell-argv-safety-specs.md"
   "docs/intent/sibling-pair-edit/sibling-pair-edit-specs.md"
   "docs/intent/telemetry-events/telemetry-events-specs.md"
   "docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md"
   "docs/intent/test-isolation/test-isolation-specs.md"
   "docs/intent/worktree-lifecycle/worktree-lifecycle-specs.md"
   "docs/intent/write-refusal-completeness/write-refusal-completeness-specs.md"])

(def ^:private pre-derivation-literal-vector
  "The literal vector `audit-current-repository` carried before the registry was
   derived (main @ 99394bf)."
  ["docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md"
   "docs/intent/read-request-normalization/read-request-normalization-specs.md"
   "docs/intent/prepared-request/prepared-request-specs.md"
   "docs/intent/prepared-request-actions/prepared-request-actions-specs.md"
   "docs/intent/write-refusal-completeness/write-refusal-completeness-specs.md"
   "docs/intent/insertion-boundary-and-gap/insertion-boundary-and-gap-specs.md"
   "docs/intent/shell-argv-safety/shell-argv-safety-specs.md"])

(def ^:private lanes-added-since-derivation
  "Intent leaves merged onto the integration branch after the registry was derived.
   Each one used to mean a line in the shared production vector; now it means a file."
  ["docs/intent/alias-migration/alias-migration-specs.md"
   "docs/intent/memory-boundedness/memory-boundedness-specs.md"
   "docs/intent/memory/memory-transaction-specs.md"
   "docs/intent/read-path-memory/read-path-memory-specs.md"
   "docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md"
   "docs/intent/relation-census/relation-census-specs.md"
   "docs/intent/feature-thread/feature-thread-specs.md"
   "docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md"
   "docs/intent/test-isolation/test-isolation-specs.md"
   "docs/intent/helper-extraction/helper-extraction-specs.md"
   "docs/intent/hot-verification/hot-verification-specs.md"])

(deftest the-derived-spec-doc-set-matches-the-expected-set-exactly
  (testing "drift in docs/intent is visible here, not silent"
    (is (= expected-spec-docs (spec-doc-paths ".")))))

(deftest the-derived-audit-preserves-the-registered-mcp-intents
  (testing "prefix widening preserves the previously registered MCP-OP intent set"
    (let [registered (spec-ids "." (concat pre-derivation-literal-vector
                                           lanes-added-since-derivation))
          derived (spec-ids "." (spec-doc-paths "."))]
      (is (= (set (filter #(str/starts-with? % "MCP-OP-") registered))
             (set (filter #(str/starts-with? % "MCP-OP-") derived)))))))

;; @spec MCP-OP-TRACE-005
(deftest the-derived-audit-includes-every-previously-invisible-row
  (let [ids (spec-ids "." (spec-doc-paths "."))
        non-mcp (set (remove #(str/starts-with? % "MCP-OP-") ids))]
    ;; Audit ledger: 165 original non-MCP rows, plus the repaired telemetry row.
    (is (= 166 (count non-mcp)))
    (is (= {"WTL-" 53 "PERF-SENT-" 50 "OP-ALG-" 39 "TEST-ISO-" 19
            "MEASURE-" 4 "TELEMETRY-EVENTS-" 1}
           (into {} (for [prefix ["WTL-" "PERF-SENT-" "OP-ALG-" "TEST-ISO-"
                                  "MEASURE-" "TELEMETRY-EVENTS-"]]
                      [prefix (count (filter #(str/starts-with? % prefix) non-mcp))]))))))
