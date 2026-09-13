# Measured cadence adoption

Receipt for every wall below: [ceiling-run/receipt.edn](ceiling-run/receipt.edn), shipped-runtime coordinator run. All 47 previously had `lane-of = nil`. Runtime assignments are unchanged. 33 join fast; 14 join battery; no integration move or per-namespace override is needed. Existing `core-discovery-test` was already battery and is also removed from the fast entrance's runtime union. No test is deleted or skipped.

The parser corpus scans repository sources and costs 71,055 ms despite its old millisecond-scale docstring. It cannot meet either fast's 8,000 ms or integration's 20,000 ms namespace ceiling; battery's existing 300,000 ms ceiling fits. Other battery rows exercise real subprocess boundaries; wall alone does not make those fast. Small pure and isolated temporary-file tests retain fast cadence.

| Namespace | Wall ms | New lane | Budget ms | Reason |
|---|---:|---|---:|---|
| clj-surgeon.agent-routing-test | 67 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.alias-migration-test | 203 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.analyze-test | 681 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.cli-dispatch-test | 13950 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.cljc-existing-ops-test | 88 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.cljc.analyze-test | 3 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.cljc.merge-test | 5 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.cljc.require-ops-test | 5 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.cljc.split-test | 10 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.diagnostic-delta-test | 0 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.edit-dsl-test | 31 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.edit-test | 6029 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.edn-config-integration-test | 516 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.extract-header-test | 7 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.extract-test | 4823 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.failure-report-test | 2 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.file-ops-test | 1 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.fix-declares-test | 32 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.forms-test | 3 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.help-test | 11635 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.insertion-gap-test | 13 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.install-test | 81558 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.intent-transaction-test | 5511 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.jvm-error-test | 77 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.lens-query-test | 2354 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.ls-tree-test | 126 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.memory-battery-test | 30 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.move-dependency-test | 151 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.move-test | 21 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.operation-algebra-test | 814 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.outermost-test | 41 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.outline-test | 888 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.owner-hypotheses-test | 26 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.parser-admission-test | 71055 | battery | 300000 | Measured whole-corpus scan exceeds integration ceiling |
| clj-surgeon.partition-all-test | 375 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.platform-selector-test | 323 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.recovery-test | 40 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.relation-census-test | 25 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.rename-test | 6 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.show-form-test | 13462 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.structural-lens-test | 46 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.syntax-var-refs-test | 7 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.tmp-leak-support-test | 15212 | battery | 300000 | Real subprocess boundary |
| clj-surgeon.worktree-lifecycle-cli-test | 0 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.worktree-lifecycle-io-test | 39 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.worktree-lifecycle-test | 9 | fast | 8000 | Pure computation or isolated temporary fixture |
| clj-surgeon.xray-test | 1503 | battery | 300000 | Real subprocess boundary |

No budget is raised. Final fast makespan and both fast/bb sums are reported from the standalone run in REPORT.md. Measurements here classify cadence; they do not establish a runtime speedup.

The subsequent prewarm repair changes one runtime outside these 47 owners:
outline-corpus-integration-test moves from bb to JVM, retaining integration
cadence. Isolated walls are 20,859 ms bb and 8,306 ms JVM, so the existing
2.0 ratio rule selects JVM. See [prewarm-repair.md](prewarm-repair.md).
