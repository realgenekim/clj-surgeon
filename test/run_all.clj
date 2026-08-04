(ns run-all
  (:require
   [clj-surgeon.analyze-test]
   [clj-surgeon.cli-dispatch-test]
   [clj-surgeon.cljc-existing-ops-test]
   [clj-surgeon.cljc.analyze-test]
   [clj-surgeon.cljc.merge-test]
   [clj-surgeon.cljc.require-ops-test]
   [clj-surgeon.cljc.split-test]
   [clj-surgeon.edn-config-integration-test]
   [clj-surgeon.extract-test]
   [clj-surgeon.fix-declares-test]
   [clj-surgeon.forms-test]
   [clj-surgeon.help-test]
   [clj-surgeon.install-test]
   [clj-surgeon.lens-query-test]
   [clj-surgeon.ls-tree-test]
   [clj-surgeon.move-dependency-test]
   [clj-surgeon.move-test]
   [clj-surgeon.outline-test]
   [clj-surgeon.rename-test]
   [clj-surgeon.show-form-test]
   [clj-surgeon.structural-lens-test]
   [clojure.test :refer [run-tests]]))

(let [r (run-tests 'clj-surgeon.forms-test
                   'clj-surgeon.outline-test
                   'clj-surgeon.move-test
                   'clj-surgeon.move-dependency-test
                   'clj-surgeon.analyze-test
                   'clj-surgeon.rename-test
                   'clj-surgeon.fix-declares-test
                   'clj-surgeon.extract-test
                   'clj-surgeon.show-form-test
                   'clj-surgeon.structural-lens-test
                   'clj-surgeon.lens-query-test
                   'clj-surgeon.cljc.merge-test
                   'clj-surgeon.cljc.split-test
                   'clj-surgeon.cljc.require-ops-test
                   'clj-surgeon.cljc.analyze-test
                   'clj-surgeon.edn-config-integration-test
                   'clj-surgeon.cljc-existing-ops-test
                   'clj-surgeon.ls-tree-test
                   'clj-surgeon.help-test
                   'clj-surgeon.install-test
                   'clj-surgeon.cli-dispatch-test)]
  (System/exit (+ (:fail r) (:error r))))
