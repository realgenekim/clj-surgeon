# Gate evidence

No prewarm ran: the fast stop fired first. Consequently there are no prewarm refusal-census or budget lines. The following measured budget/refusal lines are reproduced verbatim, with their source logs.

[a-base.log](a-base.log)

```text
battery-parallel: 61 namespace(s) have no measured wall and are scheduled first at the 300000 ms fallback: clj-surgeon.battery-ledger-test clj-surgeon.battery-parallel-test clj-surgeon.census-pool-test clj-surgeon.fast-lane-isolation-test clj-surgeon.helper-extraction-test clj-surgeon.insert-forms-receipt-test clj-surgeon.insert-forms-test clj-surgeon.lane-manifest-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-compact-relations-test clj-surgeon.mcp-contract-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-intent-contract-test clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-paths-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-program-tool-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.mcp-recovery-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-semantic-client-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test clj-surgeon.mission-candidate-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-forms-test clj-surgeon.mission-git-test clj-surgeon.mission-plain-forms-test clj-surgeon.mission-typist-test clj-surgeon.mission-usage-test clj-surgeon.namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.outline-differential-test clj-surgeon.outline-memory-test clj-surgeon.quoted-var-refs-test clj-surgeon.receipt-booleans-test clj-surgeon.rename-alias-receipt-test clj-surgeon.rename-alias-test clj-surgeon.require-change-test clj-surgeon.scope-stream-test clj-surgeon.splice-envelope-test clj-surgeon.split-proof-gate-test clj-surgeon.telemetry-events-test clj-surgeon.workspace-onboarding-test
```
[a-base.log](a-base.log)

```text
  process 5 phase 0 (est 2400000 ms): clj-surgeon.census-pool-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-schema-test clj-surgeon.mission-forms-test clj-surgeon.outline-memory-test clj-surgeon.split-proof-gate-test
```
[a-base.log](a-base.log)

```text
  process 6 phase 0 (est 2400000 ms): clj-surgeon.battery-parallel-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.splice-envelope-test
```
[a-base.log](a-base.log)

```text
========== lane 5 (clj-surgeon.census-pool-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-schema-test clj-surgeon.mission-forms-test clj-surgeon.outline-memory-test clj-surgeon.split-proof-gate-test) exit 0, 16077 ms ==========
```
[a-base.log](a-base.log)

```text
Testing clj-surgeon.census-pool-test
```
[a-base.log](a-base.log)

```text
       353 ms  clj-surgeon.census-pool-test
```
[a-base.log](a-base.log)

```text
========== lane 6 (clj-surgeon.battery-parallel-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.splice-envelope-test) exit 0, 25894 ms ==========
```
[a-base.log](a-base.log)

```text
Testing clj-surgeon.mcp-relation-census-round20-test
```
[a-base.log](a-base.log)

```text
       134 ms  clj-surgeon.mcp-relation-census-round20-test
```
[a-base.log](a-base.log)

```text
       353 ms  clj-surgeon.census-pool-test
```
[a-base.log](a-base.log)

```text
       134 ms  clj-surgeon.mcp-relation-census-round20-test
```
[a-base.log](a-base.log)

```text
  lane 5     16077 ms  exit 0  clj-surgeon.census-pool-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-schema-test clj-surgeon.mission-forms-test clj-surgeon.outline-memory-test clj-surgeon.split-proof-gate-test
```
[a-base.log](a-base.log)

```text
  lane 6     25894 ms  exit 0  clj-surgeon.battery-parallel-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.splice-envelope-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
  process 0 phase 0 (est 4869 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.lane-manifest-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test clj-surgeon.telemetry-events-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-recovery-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
========== lane 0 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.lane-manifest-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test clj-surgeon.telemetry-events-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-recovery-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test) exit 0, 24942 ms ==========
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
Testing clj-surgeon.census-pool-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
Testing clj-surgeon.mcp-relation-census-round20-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
       224 ms  clj-surgeon.census-pool-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
       124 ms  clj-surgeon.mcp-relation-census-round20-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
       224 ms  clj-surgeon.census-pool-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
       124 ms  clj-surgeon.mcp-relation-census-round20-test
```
[b-subject-jvm.log](b-subject-jvm.log)

```text
  lane 0     24942 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.lane-manifest-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test clj-surgeon.telemetry-events-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-recovery-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test
```
[c-subject.log](c-subject.log)

```text
battery-parallel: 48 namespace(s) have no measured wall and are scheduled first at the 300000 ms fallback: clj-surgeon.agent-routing-test clj-surgeon.alias-migration-test clj-surgeon.analyze-test clj-surgeon.cli-dispatch-test clj-surgeon.cljc-existing-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.split-test clj-surgeon.core-discovery-test clj-surgeon.diagnostic-delta-test clj-surgeon.edit-dsl-test clj-surgeon.edit-test clj-surgeon.edn-config-integration-test clj-surgeon.extract-header-test clj-surgeon.extract-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.fix-declares-test clj-surgeon.forms-test clj-surgeon.help-test clj-surgeon.insertion-gap-test clj-surgeon.install-test clj-surgeon.intent-transaction-test clj-surgeon.jvm-error-test clj-surgeon.lens-query-test clj-surgeon.ls-tree-test clj-surgeon.memory-battery-test clj-surgeon.move-dependency-test clj-surgeon.move-test clj-surgeon.operation-algebra-test clj-surgeon.outermost-test clj-surgeon.outline-test clj-surgeon.owner-hypotheses-test clj-surgeon.parser-admission-test clj-surgeon.partition-all-test clj-surgeon.platform-selector-test clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.rename-test clj-surgeon.show-form-test clj-surgeon.structural-lens-test clj-surgeon.syntax-var-refs-test clj-surgeon.tmp-leak-support-test clj-surgeon.worktree-lifecycle-cli-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.worktree-lifecycle-test clj-surgeon.xray-test
```
[c-subject.log](c-subject.log)

```text
  process 3 phase 0 (est 1800000 ms): clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test
```
[c-subject.log](c-subject.log)

```text
  process 7 phase 0 (est 3531 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test
```
[c-subject.log](c-subject.log)

```text
  process 14 phase 0 (est 1467 ms): clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test
```
[c-subject.log](c-subject.log)

```text
========== lane 3 (clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test) exit 0, 82644 ms ==========
```
[c-subject.log](c-subject.log)

```text
Testing clj-surgeon.relation-census-test
```
[c-subject.log](c-subject.log)

```text
========== lane 7 (clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test) exit 0, 15015 ms ==========
```
[c-subject.log](c-subject.log)

```text
Testing clj-surgeon.mcp-relation-census-round20-test
```
[c-subject.log](c-subject.log)

```text
       183 ms  clj-surgeon.mcp-relation-census-round20-test
```
[c-subject.log](c-subject.log)

```text
========== lane 14 (clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test) exit 0, 15264 ms ==========
```
[c-subject.log](c-subject.log)

```text
Testing clj-surgeon.census-pool-test
```
[c-subject.log](c-subject.log)

```text
       222 ms  clj-surgeon.census-pool-test
```
[c-subject.log](c-subject.log)

```text
       222 ms  clj-surgeon.census-pool-test
```
[c-subject.log](c-subject.log)

```text
       183 ms  clj-surgeon.mcp-relation-census-round20-test
```
[c-subject.log](c-subject.log)

```text
        25 ms  clj-surgeon.relation-census-test
```
[c-subject.log](c-subject.log)

```text
  lane 3     82644 ms  exit 0  clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test
```
[c-subject.log](c-subject.log)

```text
  lane 7     15015 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test
```
[c-subject.log](c-subject.log)

```text
  lane 14     15264 ms  exit 0  clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test
```
[c-subject.log](c-subject.log)

```text
   TEST-ISO-007 VIOLATION in :fast -- lane time budget: the fast lane took 68679 ms, over its 60000 ms budget
```
[test-fast.log](test-fast.log)

```text
battery-parallel: 48 namespace(s) have no measured wall and are scheduled first at the 300000 ms fallback: clj-surgeon.agent-routing-test clj-surgeon.alias-migration-test clj-surgeon.analyze-test clj-surgeon.cli-dispatch-test clj-surgeon.cljc-existing-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.split-test clj-surgeon.core-discovery-test clj-surgeon.diagnostic-delta-test clj-surgeon.edit-dsl-test clj-surgeon.edit-test clj-surgeon.edn-config-integration-test clj-surgeon.extract-header-test clj-surgeon.extract-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.fix-declares-test clj-surgeon.forms-test clj-surgeon.help-test clj-surgeon.insertion-gap-test clj-surgeon.install-test clj-surgeon.intent-transaction-test clj-surgeon.jvm-error-test clj-surgeon.lens-query-test clj-surgeon.ls-tree-test clj-surgeon.memory-battery-test clj-surgeon.move-dependency-test clj-surgeon.move-test clj-surgeon.operation-algebra-test clj-surgeon.outermost-test clj-surgeon.outline-test clj-surgeon.owner-hypotheses-test clj-surgeon.parser-admission-test clj-surgeon.partition-all-test clj-surgeon.platform-selector-test clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.rename-test clj-surgeon.show-form-test clj-surgeon.structural-lens-test clj-surgeon.syntax-var-refs-test clj-surgeon.tmp-leak-support-test clj-surgeon.worktree-lifecycle-cli-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.worktree-lifecycle-test clj-surgeon.xray-test
```
[test-fast.log](test-fast.log)

```text
  process 3 phase 0 (est 1800000 ms): clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test
```
[test-fast.log](test-fast.log)

```text
  process 7 phase 0 (est 3531 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test
```
[test-fast.log](test-fast.log)

```text
  process 14 phase 0 (est 1467 ms): clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test
```
[test-fast.log](test-fast.log)

```text
========== lane 3 (clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test) exit 0, 82370 ms ==========
```
[test-fast.log](test-fast.log)

```text
Testing clj-surgeon.relation-census-test
```
[test-fast.log](test-fast.log)

```text
========== lane 7 (clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test) exit 0, 15669 ms ==========
```
[test-fast.log](test-fast.log)

```text
Testing clj-surgeon.mcp-relation-census-round20-test
```
[test-fast.log](test-fast.log)

```text
       132 ms  clj-surgeon.mcp-relation-census-round20-test
```
[test-fast.log](test-fast.log)

```text
========== lane 14 (clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test) exit 0, 14811 ms ==========
```
[test-fast.log](test-fast.log)

```text
Testing clj-surgeon.census-pool-test
```
[test-fast.log](test-fast.log)

```text
       250 ms  clj-surgeon.census-pool-test
```
[test-fast.log](test-fast.log)

```text
       250 ms  clj-surgeon.census-pool-test
```
[test-fast.log](test-fast.log)

```text
       132 ms  clj-surgeon.mcp-relation-census-round20-test
```
[test-fast.log](test-fast.log)

```text
        24 ms  clj-surgeon.relation-census-test
```
[test-fast.log](test-fast.log)

```text
  lane 3     82370 ms  exit 0  clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test clj-surgeon.install-test clj-surgeon.operation-algebra-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-test
```
[test-fast.log](test-fast.log)

```text
  lane 7     15669 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.ns-isolation-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-compact-edit-test
```
[test-fast.log](test-fast.log)

```text
  lane 14     14811 ms  exit 0  clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-operation-test
```
[test-fast.log](test-fast.log)

```text
   TEST-ISO-007 VIOLATION in :fast -- lane time budget: the fast lane took 68664 ms, over its 60000 ms budget
```
[test-fast.log](test-fast.log)

```text
gate-refused: gate-refused: suite failed {:suite "fast", :problems ["lane 0 exited 1 (log target/gate-parallel/b13a2dfe-4de8-407b-ad0c-ee36021404f0/fast/lane-0.out)"], :result {:test 1631, :pass 15787, :fail 1, :error 0}}
```
