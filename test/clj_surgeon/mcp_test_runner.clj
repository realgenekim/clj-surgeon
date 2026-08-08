(ns clj-surgeon.mcp-test-runner
  (:require
   [clj-surgeon.mcp-change-buffer-test]
   [clj-surgeon.mcp-compact-edit-test]
   [clj-surgeon.mcp-contract-test]
   [clj-surgeon.mcp-http-server-test]
   [clj-surgeon.mcp-inspect-contract-test]
   [clj-surgeon.mcp-inspect-tool-test]
   [clj-surgeon.mcp-semantic-client-test]
   [clj-surgeon.mcp-server-test]
   [clj-surgeon.mcp-telemetry-test]
   [clj-surgeon.mcp-tool-test]
   [clj-surgeon.mcp-workspace-test]
   [clj-surgeon.workspace-onboarding-test]
   [clojure.test :refer [run-tests]]))

(defn -main
  [& _]
  (let [result
        (run-tests
          'clj-surgeon.mcp-contract-test
          'clj-surgeon.mcp-change-buffer-test
          'clj-surgeon.mcp-compact-edit-test
          'clj-surgeon.mcp-http-server-test
          'clj-surgeon.mcp-inspect-contract-test
          'clj-surgeon.mcp-inspect-tool-test
          'clj-surgeon.mcp-telemetry-test
          'clj-surgeon.mcp-tool-test
          'clj-surgeon.mcp-workspace-test
          'clj-surgeon.mcp-server-test
          'clj-surgeon.mcp-semantic-client-test
          'clj-surgeon.workspace-onboarding-test)]
    (System/exit (+ (:fail result) (:error result)))))
