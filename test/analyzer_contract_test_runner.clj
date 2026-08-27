(ns analyzer-contract-test-runner
  (:require
   [clj-surgeon.analyzer-contract-test]
   [clojure.test :refer [run-tests]]))

(let [result (run-tests 'clj-surgeon.analyzer-contract-test)]
  (System/exit (+ (:fail result) (:error result))))
