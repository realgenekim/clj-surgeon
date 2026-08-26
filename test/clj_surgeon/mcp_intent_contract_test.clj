(ns clj-surgeon.mcp-intent-contract-test
  (:require
   [clojure.test :refer [deftest is testing]]))

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
