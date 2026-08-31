(ns clj-surgeon.mcp-test-runner
  (:require
   [clj-surgeon.mcp-change-buffer-test]
   [clj-surgeon.mcp-cold-verify-test]
   [clj-surgeon.mcp-compact-edit-fields-test]
   [clj-surgeon.mcp-compact-edit-test]
   [clj-surgeon.mcp-compact-location-test]
   [clj-surgeon.mcp-compact-relations-test]
   [clj-surgeon.mcp-contract-test]
   [clj-surgeon.mcp-extraction-plan-test]
   [clj-surgeon.mcp-extraction-test]
   [clj-surgeon.mcp-hot-verify-test]
   [clj-surgeon.mcp-http-server-test]
   [clj-surgeon.mcp-inspect-contract-test]
   [clj-surgeon.mcp-inspect-tool-test]
   [clj-surgeon.mcp-intent-contract-test]
   [clj-surgeon.mcp-operation-async-test]
   [clj-surgeon.mcp-operation-registry-test]
   [clj-surgeon.mcp-operation-test]
   [clj-surgeon.mcp-paths-test]
   [clj-surgeon.mcp-prepared-request-test]
   [clj-surgeon.mcp-process-test]
   [clj-surgeon.mcp-program-tool-test]
   [clj-surgeon.mcp-read-request-normalization-test]
   [clj-surgeon.mcp-recovery-test]
   [clj-surgeon.mcp-schema-test]
   [clj-surgeon.mcp-semantic-client-test]
   [clj-surgeon.mcp-server-test]
   [clj-surgeon.mcp-substantiation-report-test]
   [clj-surgeon.mcp-substantiation-test]
   [clj-surgeon.mcp-telemetry-test]
   [clj-surgeon.mcp-tool-test]
   [clj-surgeon.mcp-workspace-test]
   [clj-surgeon.mcp-write-refusal-test]
   [clj-surgeon.quoted-var-refs-test]
   [clj-surgeon.workspace-onboarding-test]
   [clojure.test :refer [run-tests]]))

(defn -main
  [& _]
  (let [result
        (run-tests
          'clj-surgeon.mcp-contract-test
          'clj-surgeon.mcp-extraction-test
          'clj-surgeon.mcp-extraction-plan-test
          'clj-surgeon.mcp-change-buffer-test
          'clj-surgeon.mcp-cold-verify-test
          'clj-surgeon.mcp-compact-edit-test
          'clj-surgeon.mcp-compact-edit-fields-test
          'clj-surgeon.mcp-compact-location-test
          'clj-surgeon.mcp-compact-relations-test
          'clj-surgeon.mcp-http-server-test
          'clj-surgeon.mcp-hot-verify-test
          'clj-surgeon.mcp-inspect-contract-test
          'clj-surgeon.mcp-inspect-tool-test
          'clj-surgeon.mcp-intent-contract-test
          'clj-surgeon.mcp-operation-async-test
          'clj-surgeon.mcp-operation-registry-test
          'clj-surgeon.mcp-operation-test
          'clj-surgeon.mcp-recovery-test
          'clj-surgeon.mcp-paths-test
          'clj-surgeon.mcp-process-test
          'clj-surgeon.mcp-prepared-request-test
          'clj-surgeon.mcp-substantiation-test
          'clj-surgeon.mcp-substantiation-report-test
          'clj-surgeon.mcp-program-tool-test
          'clj-surgeon.mcp-read-request-normalization-test
          'clj-surgeon.mcp-schema-test
          'clj-surgeon.mcp-telemetry-test
          'clj-surgeon.mcp-tool-test
          'clj-surgeon.mcp-workspace-test
          'clj-surgeon.mcp-write-refusal-test
          'clj-surgeon.mcp-server-test
          'clj-surgeon.mcp-semantic-client-test
          'clj-surgeon.quoted-var-refs-test
          'clj-surgeon.workspace-onboarding-test)]
    (System/exit (+ (:fail result) (:error result)))))
