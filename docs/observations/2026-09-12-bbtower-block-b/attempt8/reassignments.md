# Runtime rule: TEST-ISO-016

Portable AND bb/JVM <= 2.0; unmeasured assignments stay unchanged. Contract failures carry an explicit reason. The original 35 bb-assigned fast members are all paired.

| Namespace | JVM ms | bb ms | Ratio | Decision | Logs / reason |
|---|---:|---:|---:|---|---|
| clj-surgeon.battery-ledger-test | 194 | 3 | 0.015464 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.battery-parallel-test | 339 | 50 | 0.147493 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.fast-lane-isolation-test | 228 | 8 | 0.035088 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.helper-extraction-test | 1439 | 788 | 0.547603 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.insert-forms-test | 2017 | 5222 | 2.588994 | reassign JVM | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.intent-transaction-test | 4709 | 4905 | 1.041622 | reassign JVM | docs/observations/2026-09-12-bbtower-block-b/attempt8/intent-jvm.log; docs/observations/2026-09-12-bbtower-block-b/attempt8/intent-bb.log; Shared CLI workspace_status.unexpected_paths is unbounded; attempt8/owed.md. Runtime comparison found no bb-only leak. |
| clj-surgeon.lane-manifest-test | 1525 | 1039 | 0.681311 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-compact-edit-fields-test | 128 | 2 | 0.015625 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-contract-test | 204 | 12 | 0.058824 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-extraction-test | 341 | 32 | 0.093842 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-feature-thread-test | 43280 | 728265 | 16.826825 | reassign JVM | docs/observations/2026-09-12-bbtower-block-b/attempt8/feature-jvm.log; docs/observations/2026-09-12-bbtower-block-b/attempt8/feature-bb.log |
| clj-surgeon.mcp-inspect-contract-test | 324 | 288 | 0.888889 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-intent-contract-test | 352 | 226 | 0.642045 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-paths-test | 104 | 0 | 0.000000 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-program-tool-test | 249 | 12 | 0.048193 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-read-request-normalization-test | 105 | 2 | 0.019048 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-recovery-test | 251 | 5 | 0.019920 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-schema-test | 123 | 9 | 0.073171 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-telemetry-test | 120 | 5 | 0.041667 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mcp-workspace-test | 108 | 21 | 0.194444 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-candidate-test | 109 | 7 | 0.064220 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-forms-source-test | 219 | 10 | 0.045662 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-forms-test | 127 | 3 | 0.023622 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-git-test | 100 | 0 | 0.000000 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-plain-forms-test | 124 | 25 | 0.201613 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-typist-test | 114 | 7 | 0.061404 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.mission-usage-test | 145 | 4 | 0.027586 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.namespace-split-test | 696 | 744 | 1.068966 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.outline-differential-test | 114 | 12 | 0.105263 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.quoted-var-refs-test | 211 | 12 | 0.056872 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.rename-alias-receipt-test | 414 | 1553 | 3.751208 | reassign JVM | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.rename-alias-test | 3265 | 6478 | 1.984074 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.require-change-test | 149 | 42 | 0.281879 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.splice-envelope-test | 834 | 28591 | 34.281775 | reassign JVM | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.split-proof-gate-test | 114 | 2 | 0.017544 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.telemetry-events-test | 242 | 36 | 0.148760 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |
| clj-surgeon.workspace-onboarding-test | 240 | 111 | 0.462500 | retain bb | docs/observations/2026-09-12-bbtower-block-b/attempt7/a-base/receipt.edn; docs/observations/2026-09-12-bbtower-block-b/attempt7/c-subject/receipt.edn |

## Unmeasured: retained assignments

- clj-surgeon.admit-patch-test :jvm
- clj-surgeon.agent-routing-test :bb
- clj-surgeon.alias-migration-test :bb
- clj-surgeon.analyze-test :bb
- clj-surgeon.analyzer-contract-test :jvm
- clj-surgeon.cell-b-oracle-test :bb
- clj-surgeon.census-pool-test :jvm
- clj-surgeon.cli-dispatch-test :bb
- clj-surgeon.cljc-existing-ops-test :bb
- clj-surgeon.cljc.analyze-test :bb
- clj-surgeon.cljc.merge-test :bb
- clj-surgeon.cljc.require-ops-test :bb
- clj-surgeon.cljc.split-test :bb
- clj-surgeon.core-discovery-test :bb
- clj-surgeon.diagnostic-delta-test :bb
- clj-surgeon.edit-dsl-test :bb
- clj-surgeon.edit-test :bb
- clj-surgeon.edn-config-integration-test :bb
- clj-surgeon.extract-header-test :bb
- clj-surgeon.extract-test :bb
- clj-surgeon.failure-report-test :bb
- clj-surgeon.file-ops-test :bb
- clj-surgeon.fix-declares-test :bb
- clj-surgeon.forms-test :bb
- clj-surgeon.help-test :bb
- clj-surgeon.insert-forms-parity-test :bb
- clj-surgeon.insert-forms-receipt-test :jvm
- clj-surgeon.insertion-gap-test :bb
- clj-surgeon.install-test :bb
- clj-surgeon.jvm-error-test :bb
- clj-surgeon.lens-query-test :bb
- clj-surgeon.ls-tree-test :bb
- clj-surgeon.mcp-alias-migration-test :jvm
- clj-surgeon.mcp-change-buffer-test :jvm
- clj-surgeon.mcp-cold-verify-test :bb
- clj-surgeon.mcp-combinable-transaction-test :jvm
- clj-surgeon.mcp-compact-edit-test :jvm
- clj-surgeon.mcp-compact-location-test :jvm
- clj-surgeon.mcp-compact-relations-test :jvm
- clj-surgeon.mcp-create-files-test :jvm
- clj-surgeon.mcp-expect-guard-test :jvm
- clj-surgeon.mcp-extraction-plan-test :jvm
- clj-surgeon.mcp-feature-thread-sed-test :bb
- clj-surgeon.mcp-formatter-test :jvm
- clj-surgeon.mcp-helper-extraction-test :jvm
- clj-surgeon.mcp-hot-verify-test :jvm
- clj-surgeon.mcp-http-server-test :jvm
- clj-surgeon.mcp-inspect-cold-job-test :jvm
- clj-surgeon.mcp-inspect-tool-test :jvm
- clj-surgeon.mcp-namespace-split-test :jvm
- clj-surgeon.mcp-operation-async-test :jvm
- clj-surgeon.mcp-operation-registry-test :jvm
- clj-surgeon.mcp-operation-test :jvm
- clj-surgeon.mcp-prepared-confirmation-test :jvm
- clj-surgeon.mcp-prepared-request-test :jvm
- clj-surgeon.mcp-prepared-wire-test :jvm
- clj-surgeon.mcp-process-test :bb
- clj-surgeon.mcp-relation-census-launcher-test :bb
- clj-surgeon.mcp-relation-census-round20-test :jvm
- clj-surgeon.mcp-relation-census-test :jvm
- clj-surgeon.mcp-semantic-client-test :jvm
- clj-surgeon.mcp-server-test :jvm
- clj-surgeon.mcp-tool-test :jvm
- clj-surgeon.mcp-write-refusal-test :jvm
- clj-surgeon.memory-battery-test :bb
- clj-surgeon.memory.journal-green-test :bb
- clj-surgeon.memory.oom-reproduction-test :bb
- clj-surgeon.mission-candidate-race-test :jvm
- clj-surgeon.mission-commit-cli-test :jvm
- clj-surgeon.mission-display-test :bb
- clj-surgeon.mission-events-test :jvm
- clj-surgeon.mission-fallback-test :jvm
- clj-surgeon.mission-git-boundary-test :bb
- clj-surgeon.mission-git-fence-test :bb
- clj-surgeon.mission-git-identity-test :bb
- clj-surgeon.mission-git-ledger-test :jvm
- clj-surgeon.mission-git-process-test :bb
- clj-surgeon.mission-git-submodule-test :bb
- clj-surgeon.mission-phase-events-test :jvm
- clj-surgeon.mission-provider-fallback-events-test :jvm
- clj-surgeon.mission-publication-test :jvm
- clj-surgeon.mission-run-test :jvm
- clj-surgeon.mission-test :jvm
- clj-surgeon.mission-typist-executor-admission-test :jvm
- clj-surgeon.mission-typist-executor-test :jvm
- clj-surgeon.mission-usage-executor-test :jvm
- clj-surgeon.move-dependency-test :bb
- clj-surgeon.move-test :bb
- clj-surgeon.namespace-split-warm-test :jvm
- clj-surgeon.ns-isolation-test :jvm
- clj-surgeon.operation-algebra-test :bb
- clj-surgeon.outermost-test :bb
- clj-surgeon.outline-corpus-integration-test :bb
- clj-surgeon.outline-memory-test :jvm
- clj-surgeon.outline-test :bb
- clj-surgeon.owner-hypotheses-test :bb
- clj-surgeon.parser-admission-test :bb
- clj-surgeon.partition-all-test :bb
- clj-surgeon.platform-selector-test :bb
- clj-surgeon.reader-eval-fence-test :bb
- clj-surgeon.receipt-artifacts-boundary-test :jvm
- clj-surgeon.receipt-booleans-test :jvm
- clj-surgeon.recovery-test :bb
- clj-surgeon.relation-census-test :bb
- clj-surgeon.rename-alias-parity-test :bb
- clj-surgeon.rename-alias-performance-test :bb
- clj-surgeon.rename-test :bb
- clj-surgeon.repository-hygiene-test :bb
- clj-surgeon.require-change-boundary-test :bb
- clj-surgeon.scope-stream-test :jvm
- clj-surgeon.show-form-test :bb
- clj-surgeon.split-proof-gate-boundary-test :bb
- clj-surgeon.structural-lens-test :bb
- clj-surgeon.syntax-var-refs-test :bb
- clj-surgeon.tmp-leak-support-test :bb
- clj-surgeon.txn-journal-test :jvm
- clj-surgeon.worktree-lifecycle-cli-test :bb
- clj-surgeon.worktree-lifecycle-io-test :bb
- clj-surgeon.worktree-lifecycle-prune-test :bb
- clj-surgeon.worktree-lifecycle-recovery-test :bb
- clj-surgeon.worktree-lifecycle-test :bb
- clj-surgeon.xray-test :bb
