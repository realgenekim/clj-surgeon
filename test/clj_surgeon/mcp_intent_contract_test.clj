(ns ^{:lane :fast} clj-surgeon.mcp-intent-contract-test
  (:require
   [clj-surgeon.mcp-intent-contract]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.set :as set]
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
          'clj-surgeon.mcp-intent-contract/audit-current-repository)]
    (is (:ok (audit-current-repository)))))

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
  "The MCP-OP intent IDs the audit would parse out of these repo-relative files."
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
            (spec-line "x" "MCP-OP-TEMPLANE-001"))
      ;; a sibling that is NOT a spec document, and a `-specs.from-*.md` variant,
      ;; must both be ignored.
      (spit (io/file leaf "temp-lane-design.md") "design\n")
      (spit (io/file leaf "temp-lane-specs.from-docs--x.md")
            (spec-line "x" "MCP-OP-TEMPLANE-999"))
      (is (= ["docs/intent/temp-lane/temp-lane-specs.md"] (spec-doc-paths root {})))
      (is (= #{"MCP-OP-TEMPLANE-001"}
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

(deftest the-derived-audit-covers-exactly-the-registered-lane-intents
  (testing "deriving the list changed WHICH FILES are scanned, not WHICH INTENTS are audited"
    (let [registered (spec-ids "." (concat pre-derivation-literal-vector
                                           lanes-added-since-derivation))
          derived (spec-ids "." (spec-doc-paths "."))]
      ;; The additionally-scanned documents contribute either no MCP-OP IDs at all
      ;; (measurement-evidence, operation-algebra, performance-regression-sentinel,
      ;; sibling-pair-edit, worktree-lifecycle use other prefixes) or a duplicate of
      ;; the prepared-request IDs (the 2026-08-30 ratification copy).
      (is (= registered derived)
          (str "added: " (sort (set/difference derived registered))
               " removed: " (sort (set/difference registered derived)))))))
