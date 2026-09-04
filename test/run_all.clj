(ns run-all
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clj-surgeon.tmp-leak-support-test]
   [clj-surgeon.agent-routing-test]
   [clj-surgeon.alias-migration-test]
   [clj-surgeon.analyze-test]
   [clj-surgeon.cli-dispatch-test]
   [clj-surgeon.cljc-existing-ops-test]
   [clj-surgeon.cljc.analyze-test]
   [clj-surgeon.cljc.merge-test]
   [clj-surgeon.cljc.require-ops-test]
   [clj-surgeon.cljc.split-test]
   [clj-surgeon.core-discovery-test]
   [clj-surgeon.diagnostic-delta-test]
   [clj-surgeon.edit-dsl-test]
   [clj-surgeon.edit-test]
   [clj-surgeon.edn-config-integration-test]
   [clj-surgeon.extract-header-test]
   [clj-surgeon.extract-test]
   [clj-surgeon.failure-report-test]
   [clj-surgeon.file-ops-test]
   [clj-surgeon.fix-declares-test]
   [clj-surgeon.forms-test]
   [clj-surgeon.help-test]
   [clj-surgeon.insertion-gap-test]
   [clj-surgeon.install-test]
   [clj-surgeon.intent-transaction-test]
   [clj-surgeon.lens-query-test]
   [clj-surgeon.memory-battery-test]
   [clj-surgeon.ls-tree-test]
   [clj-surgeon.move-dependency-test]
   [clj-surgeon.move-test]
   [clj-surgeon.operation-algebra-test]
   [clj-surgeon.outermost-test]
   [clj-surgeon.outline-test]
   [clj-surgeon.owner-hypotheses-test]
   [clj-surgeon.parser-admission-test]
   [clj-surgeon.partition-all-test]
   [clj-surgeon.platform-selector-test]
   [clj-surgeon.quoted-var-refs-test]
   [clj-surgeon.recovery-test]
   [clj-surgeon.rename-test]
   [clj-surgeon.show-form-test]
   [clj-surgeon.structural-lens-test]
   [clj-surgeon.study-test]
   [clj-surgeon.syntax-var-refs-test]
   [clj-surgeon.workspace-onboarding-test]
   [clj-surgeon.worktree-lifecycle-cli-test]
   [clj-surgeon.worktree-lifecycle-io-test]
   [clj-surgeon.worktree-lifecycle-test]
   [clj-surgeon.xray-test]
   [clojure.test :refer [run-tests]]))

;; RATCHET (2026-09-04, inb-9483a4): refuse to run at all on tmpfs, then
;; isolate java.io.tmpdir into a private per-run root so any leaked fixture
;; dir fails the run by name -- with no false positives from concurrent
;; seats sharing /var/tmp/forge. See clj-surgeon.tmp-leak-support.
(let [{:keys [refused root]} (tmp-leak/secure-tmpdir! {:bb-script *file*} *command-line-args*)
      _ (when refused (System/exit 97))
      tmp-root root
      tmp-before (tmp-leak/tmp-entries)
      r (run-tests 'clj-surgeon.tmp-leak-support-test
                   'clj-surgeon.forms-test
                   'clj-surgeon.alias-migration-test
                   'clj-surgeon.agent-routing-test
                   'clj-surgeon.outline-test
                   'clj-surgeon.move-test
                   'clj-surgeon.operation-algebra-test
                   'clj-surgeon.move-dependency-test
                   'clj-surgeon.analyze-test
                   'clj-surgeon.diagnostic-delta-test
                   'clj-surgeon.rename-test
                   'clj-surgeon.fix-declares-test
                   'clj-surgeon.extract-header-test
                   'clj-surgeon.extract-test
                   'clj-surgeon.failure-report-test
                   'clj-surgeon.file-ops-test
                   'clj-surgeon.show-form-test
                   'clj-surgeon.structural-lens-test
                   'clj-surgeon.syntax-var-refs-test
                   'clj-surgeon.lens-query-test
                   'clj-surgeon.memory-battery-test
                   'clj-surgeon.cljc.merge-test
                   'clj-surgeon.cljc.split-test
                   'clj-surgeon.cljc.require-ops-test
                   'clj-surgeon.cljc.analyze-test
                   'clj-surgeon.edn-config-integration-test
                   'clj-surgeon.edit-test
                   'clj-surgeon.edit-dsl-test
                   'clj-surgeon.cljc-existing-ops-test
                   'clj-surgeon.study-test
                   'clj-surgeon.ls-tree-test
                   'clj-surgeon.outermost-test
                   'clj-surgeon.owner-hypotheses-test
                   'clj-surgeon.parser-admission-test
                   'clj-surgeon.partition-all-test
                   'clj-surgeon.platform-selector-test
                   'clj-surgeon.quoted-var-refs-test
                   'clj-surgeon.xray-test
                   'clj-surgeon.help-test
                   'clj-surgeon.install-test
                   'clj-surgeon.insertion-gap-test
                   'clj-surgeon.intent-transaction-test
                   'clj-surgeon.workspace-onboarding-test
                   'clj-surgeon.worktree-lifecycle-test
                   'clj-surgeon.worktree-lifecycle-io-test
                   'clj-surgeon.worktree-lifecycle-cli-test
                   'clj-surgeon.recovery-test
                   'clj-surgeon.cli-dispatch-test
                   'clj-surgeon.core-discovery-test)
      leak-fail (tmp-leak/report-and-sweep-leak! tmp-root tmp-before)]
  (System/exit (+ (:fail r) (:error r) leak-fail)))
