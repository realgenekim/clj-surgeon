(ns run-all
  (:require [clojure.test :refer [run-tests]]
            [clj-surgeon.outline-test]
            [clj-surgeon.move-test]
            [clj-surgeon.analyze-test]
            [clj-surgeon.rename-test]
            [clj-surgeon.fix-declares-test]))

(let [r (run-tests 'clj-surgeon.outline-test
                   'clj-surgeon.move-test
                   'clj-surgeon.analyze-test
                   'clj-surgeon.rename-test
                   'clj-surgeon.fix-declares-test)]
  (System/exit (+ (:fail r) (:error r))))
