(ns clj-surgeon.mcp-operation-registry-test
  (:require
   [clj-surgeon.mcp-server :as server]
   [clojure.test :refer [deftest is testing]]))

(def operation-witness-catalog
  {"inspect_clojure"
   {:read-success
    'clj-surgeon.mcp-inspect-tool-test/callback-separates-concise-content-from-full-structured-evidence
    :prepared-basis
    'clj-surgeon.mcp-inspect-tool-test/prepare-change-callback-is-compact-and-carries-the-full-basis-structurally
    :verification-pending
    'clj-surgeon.mcp-operation-async-test/pending-verification-publishes-one-observed-state-without-job-time
    :verification-complete
    'clj-surgeon.mcp-operation-async-test/completed-verification-labels-request-and-job-clocks
    :verification-failed
    'clj-surgeon.mcp-operation-async-test/completed-verification-labels-request-and-job-clocks
    :typed-refusal
    'clj-surgeon.mcp-inspect-tool-test/uninitialized-handler-reports-elapsed-time}
   "apply_clojure_changes"
   {:committed
    'clj-surgeon.mcp-tool-test/callback-uses-mcp-success-and-error-channels
    :verification-pending
    'clj-surgeon.mcp-tool-test/direct-change-returns-after-hot-proof-and-publishes-a-cold-job
    :typed-refusal
    'clj-surgeon.mcp-tool-test/callback-uses-mcp-success-and-error-channels}
   "edit_clojure"
   {:committed
    'clj-surgeon.mcp-tool-test/editor-gesture-is-exact-guarded-and-undoable
    :typed-refusal
    'clj-surgeon.mcp-tool-test/refuses-before-write-and-publishes-no-receipt}
   "transform_clojure"
   {:preview
    'clj-surgeon.mcp-program-tool-test/previews-one-program-as-several-lossless-addressed-edits
    :committed
    'clj-surgeon.mcp-program-tool-test/commits-the-compiled-addressed-edits-with-read-back-proof
    :typed-refusal
    'clj-surgeon.mcp-program-tool-test/callback-reports-elapsed-time-on-success-and-refusal}
   "relation_census"
   {:read-success
    'clj-surgeon.mcp-relation-census-test/censuses-the-real-bytes-fixture-through-the-tool
    :typed-refusal
    'clj-surgeon.mcp-relation-census-test/refuses-with-a-typed-reason-and-an-executable-next-call}})

(defn- public-tool-registry
  []
  ((requiring-resolve 'clj-surgeon.mcp-server/public-tool-registry)))

;; @spec MCP-OP-COVERAGE-001
;; @spec MCP-OP-COVERAGE-002
(deftest canonical-registry-and-independent-witness-catalog-match-exactly
  (let [registry (public-tool-registry)
        registered (into {} (map (juxt :name identity)) registry)
        advertised (server/make-tools nil ".")]
    (is (= (set (keys registered))
           (set (keys operation-witness-catalog))))
    (is (= (set (keys registered))
           (set (map :name advertised))))
    (doseq [[tool-name witnesses] operation-witness-catalog]
      (testing tool-name
        (is (= (set (keys witnesses))
               (set (:outcome-classes (get registered tool-name)))))
        (doseq [[outcome test-symbol] witnesses]
          (testing (name outcome)
            (let [test-var (requiring-resolve test-symbol)]
              (is (var? test-var))
              (is (fn? (:test (meta test-var)))))))))))
