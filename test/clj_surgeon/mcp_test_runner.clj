(ns clj-surgeon.mcp-test-runner
  (:require
   [clj-surgeon.mcp-contract-test]
   [clj-surgeon.mcp-http-server-test]
   [clj-surgeon.mcp-server-test]
   [clj-surgeon.mcp-telemetry-test]
   [clj-surgeon.mcp-tool-test]
   [clojure.test :refer [run-tests]]))

(defn -main
  [& _]
  (let [result
        (run-tests
         'clj-surgeon.mcp-contract-test
         'clj-surgeon.mcp-http-server-test
         'clj-surgeon.mcp-telemetry-test
         'clj-surgeon.mcp-tool-test
         'clj-surgeon.mcp-server-test)]
    (System/exit (+ (:fail result) (:error result)))))
