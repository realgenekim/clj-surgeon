(ns clj-surgeon.memory.memory-test-runner
  "Runner for the memory namespaces, which are deliberately outside the fast
   suites: they spawn child JVMs, write hundreds of megabytes of fixture, and
   cost minutes of wall. `make memory-red` invokes this."
  (:require
   [clj-surgeon.memory.journal-green-test]
   [clj-surgeon.memory.oom-reproduction-test]
   [clojure.test :refer [run-tests]]))

(defn -main
  [& _]
  (let [result (run-tests 'clj-surgeon.memory.oom-reproduction-test
                             'clj-surgeon.memory.journal-green-test)]
    (shutdown-agents)
    (System/exit (if (and (zero? (:fail result)) (zero? (:error result))) 0 1))))
