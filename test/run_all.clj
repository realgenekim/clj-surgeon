(ns run-all
  (:require [clojure.test :refer [run-tests]]
            [clj-surgeon.outline-test]
            [clj-surgeon.move-test]
            [clj-surgeon.analyze-test]
            [clj-surgeon.rename-test]
            [clj-surgeon.fix-declares-test]
            [clj-surgeon.extract-test]
            [clj-surgeon.cljc.merge-test]
            [clj-surgeon.cljc.split-test]
            [clj-surgeon.cljc.require-ops-test]
            [clj-surgeon.cljc.analyze-test]))

(let [r (run-tests 'clj-surgeon.outline-test
                   'clj-surgeon.move-test
                   'clj-surgeon.analyze-test
                   'clj-surgeon.rename-test
                   'clj-surgeon.fix-declares-test
                   'clj-surgeon.extract-test
                   'clj-surgeon.cljc.merge-test
                   'clj-surgeon.cljc.split-test
                   'clj-surgeon.cljc.require-ops-test
                   'clj-surgeon.cljc.analyze-test)]
  (System/exit (+ (:fail r) (:error r))))
