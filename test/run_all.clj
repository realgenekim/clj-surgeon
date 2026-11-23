(ns run-all
  (:require
   [clj-surgeon.agent-routing-test]
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
   [clj-surgeon.ls-tree-test]
   [clj-surgeon.move-dependency-test]
   [clj-surgeon.move-test]
   [clj-surgeon.operation-algebra-test]
   [clj-surgeon.outermost-test]
   [clj-surgeon.outline-test]
   [clj-surgeon.owner-hypotheses-test]
   [clj-surgeon.partition-all-test]
   [clj-surgeon.platform-selector-test]
   [clj-surgeon.quoted-var-refs-test]
   [clj-surgeon.recovery-test]
   [clj-surgeon.rename-test]
   [clj-surgeon.show-form-test]
   [clj-surgeon.structural-lens-test]
   [clj-surgeon.syntax-var-refs-test]
   [clj-surgeon.usage-log-test]
   [clj-surgeon.workspace-onboarding-test]
   [clj-surgeon.worktree-lifecycle-cli-test]
   [clj-surgeon.worktree-lifecycle-io-test]
   [clj-surgeon.worktree-lifecycle-test]
   [clj-surgeon.xray-test]
   [babashka.process :as process]
   [clojure.test :refer [run-tests]]))

;; The suite calls the CLI in-process (core/run) and as subprocesses. Neither may reach the developer's real usage
;; log, however the suite was started: when the variable is absent, re-execute this runner with it set to off so
;; every child inherits it. (Requiring the namespaces above runs no test; run-tests below does.)
(when-not (System/getenv "CLJ_SURGEON_USAGE_LOG")
  (process/exec ["bb" *file*] {:extra-env {"CLJ_SURGEON_USAGE_LOG" "off"}}))


(let [r (run-tests 'clj-surgeon.forms-test
                   'clj-surgeon.usage-log-test
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
                   'clj-surgeon.cljc.merge-test
                   'clj-surgeon.cljc.split-test
                   'clj-surgeon.cljc.require-ops-test
                   'clj-surgeon.cljc.analyze-test
                   'clj-surgeon.edn-config-integration-test
                   'clj-surgeon.edit-test
                   'clj-surgeon.edit-dsl-test
                   'clj-surgeon.cljc-existing-ops-test
                   'clj-surgeon.ls-tree-test
                   'clj-surgeon.outermost-test
                   'clj-surgeon.owner-hypotheses-test
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
                   'clj-surgeon.core-discovery-test)]
  (System/exit (+ (:fail r) (:error r))))
