bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 24328 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T12:07:41.083534630Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=119
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=130
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=165
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 10967}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 77636 ms
  process 0 phase 0 (est 5675 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 77636 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 48010 ms
  process 0 phase 0 (est 4537 ms): clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-operation-async-test
  process 1 phase 0 (est 240 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-extraction-test clj-surgeon.syntax-var-refs-test clj-surgeon.insertion-gap-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test clj-surgeon.mcp-compact-edit-fields-test
  process 2 phase 0 (est 3257 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.mcp-workspace-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.failure-report-test
  process 3 phase 0 (est 1520 ms): clj-surgeon.scope-stream-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-semantic-client-test clj-surgeon.mcp-formatter-test
  process 4 phase 0 (est 2913 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test
  process 5 phase 0 (est 1865 ms): clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.telemetry-events-test clj-surgeon.move-test clj-surgeon.require-change-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test clj-surgeon.worktree-lifecycle-test
  process 6 phase 0 (est 3471 ms): clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-compact-edit-test
  process 7 phase 0 (est 1306 ms): clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.fix-declares-test clj-surgeon.outermost-test clj-surgeon.mission-typist-test clj-surgeon.cljc.merge-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 8 phase 0 (est 4132 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-operation-test clj-surgeon.mission-candidate-race-test
  process 9 phase 0 (est 646 ms): clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.move-dependency-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.extract-header-test clj-surgeon.split-proof-gate-test
  process 10 phase 0 (est 4280 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-extraction-plan-test
  process 11 phase 0 (est 498 ms): clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.edit-dsl-test clj-surgeon.cljc.split-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  process 12 phase 0 (est 6919 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 8056 ms): clj-surgeon.mcp-compact-relations-test
  process 14 phase 1 (est 199 ms): clj-surgeon.mcp-server-test
  process 15 phase 1 (est 1296 ms): clj-surgeon.namespace-split-warm-test
  process 16 phase 1 (est 2406 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 2768 ms): clj-surgeon.mcp-tool-test
  process 18 phase 1 (est 2923 ms): clj-surgeon.mcp-http-server-test
  process 19 phase 1 (est 9631 ms): clj-surgeon.outline-corpus-integration-test
  process 20 phase 1 (est 48010 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 87514 ms
  process 0 phase 0 (est 6272 ms): clj-surgeon.edit-test clj-surgeon.platform-selector-test
  process 1 phase 0 (est 5856 ms): clj-surgeon.intent-transaction-test
  process 2 phase 0 (est 1837 ms): clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test
  process 3 phase 0 (est 13092 ms): clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.edn-config-integration-test
  process 4 phase 0 (est 873 ms): clj-surgeon.operation-algebra-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.syntax-var-refs-test clj-surgeon.failure-report-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 5 phase 0 (est 13712 ms): clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test
  process 6 phase 0 (est 254 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test
  process 7 phase 0 (est 13484 ms): clj-surgeon.show-form-test
  process 8 phase 0 (est 483 ms): clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.rename-test clj-surgeon.forms-test
  process 9 phase 0 (est 14065 ms): clj-surgeon.cli-dispatch-test
  process 10 phase 0 (est 14152 ms): clj-surgeon.tmp-leak-support-test
  process 11 phase 0 (est 72359 ms): clj-surgeon.parser-admission-test
  process 12 phase 0 (est 87514 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 15113 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5025 ms):
      5025 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 90286 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 76794 ms):
     76794 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3639 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 90286 ms
cadence :battery: serial-equivalent 81819 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 81819 ms):
     76794 ms  clj-surgeon.mcp-alias-migration-test
      5025 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 90286 ms:
  lane 0     15113 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     90286 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 90286 ms over 2 lane(s); serial-equivalent 81819 ms; skipped 0

========== lane 0 (clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-operation-async-test) exit 0, 19949 ms ==========
lanes: --ns -- 8 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (8, slowest first, total 3751 ms):
      1183 ms  clj-surgeon.splice-envelope-test
       865 ms  clj-surgeon.ns-isolation-test
       568 ms  clj-surgeon.insert-forms-receipt-test
       410 ms  clj-surgeon.outline-memory-test
       277 ms  clj-surgeon.mcp-create-files-test
       204 ms  clj-surgeon.mcp-prepared-request-test
       132 ms  clj-surgeon.mcp-write-refusal-test
       112 ms  clj-surgeon.mcp-operation-async-test

========== lane 1 (clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-extraction-test clj-surgeon.syntax-var-refs-test clj-surgeon.insertion-gap-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 622 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 2 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.mcp-workspace-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.failure-report-test) exit 0, 4006 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.outline-test

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.mcp-read-request-normalization-test

Testing clj-surgeon.failure-report-test

========== lane 3 (clj-surgeon.scope-stream-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-semantic-client-test clj-surgeon.mcp-formatter-test) exit 0, 17038 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-semantic-client-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 3 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 1550 ms):
       417 ms  clj-surgeon.receipt-booleans-test
       387 ms  clj-surgeon.scope-stream-test
       344 ms  clj-surgeon.mcp-compact-location-test
       180 ms  clj-surgeon.mcp-prepared-confirmation-test
       113 ms  clj-surgeon.mcp-formatter-test
       109 ms  clj-surgeon.mcp-semantic-client-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test) exit 0, 17142 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-relation-census-round20-test
WORKSPACE-PARITY-ENUMERATION:
  unresolvable-root-escaping-link      expected invalid-workspace-root   tool "invalid-workspace-root" cli "invalid-workspace-root" scanned 0  agree true
  unresolvable-root-outside-absolute   expected invalid-workspace-root   tool "invalid-workspace-root" cli "invalid-workspace-root" scanned 0  agree true
ROOT-SHAPE-PARITY-ENUMERATION:
  plain            expected no-fold-arms-found       tool "no-fold-arms-found"     cli "no-fold-arms-found"     scanned 1  agree true
  trailing-slash   expected no-fold-arms-found       tool "no-fold-arms-found"     cli "no-fold-arms-found"     scanned 1  agree true
  double-slash     expected no-fold-arms-found       tool "no-fold-arms-found"     cli "no-fold-arms-found"     scanned 1  agree true
  dot              expected no-fold-arms-found       tool "no-fold-arms-found"     cli "no-fold-arms-found"     scanned 1  agree true
  dotdot           expected no-fold-arms-found       tool "no-fold-arms-found"     cli "no-fold-arms-found"     scanned 1  agree true
  dir-is-a-file    expected invalid-workspace-root   tool "invalid-workspace-root" cli "invalid-workspace-root" scanned 0  agree true
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 2850 ms):
      1894 ms  clj-surgeon.mcp-inspect-tool-test
       501 ms  clj-surgeon.mcp-change-buffer-test
       312 ms  clj-surgeon.census-pool-test
       143 ms  clj-surgeon.mcp-relation-census-round20-test

========== lane 5 (clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.telemetry-events-test clj-surgeon.move-test clj-surgeon.require-change-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test clj-surgeon.worktree-lifecycle-test) exit 0, 2327 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.move-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.worktree-lifecycle-test

========== lane 6 (clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-compact-edit-test) exit 0, 18440 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 3187 ms):
      2122 ms  clj-surgeon.insert-forms-test
       518 ms  clj-surgeon.rename-alias-receipt-test
       253 ms  clj-surgeon.mcp-expect-guard-test
       177 ms  clj-surgeon.mcp-combinable-transaction-test
       117 ms  clj-surgeon.mcp-compact-edit-test

========== lane 7 (clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.fix-declares-test clj-surgeon.outermost-test clj-surgeon.mission-typist-test clj-surgeon.cljc.merge-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1785 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-operation-test clj-surgeon.mission-candidate-race-test) exit 0, 16723 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-operation-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 3529 ms):
      3259 ms  clj-surgeon.mcp-namespace-split-test
       137 ms  clj-surgeon.mcp-operation-test
       133 ms  clj-surgeon.mission-candidate-race-test

========== lane 9 (clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.move-dependency-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.extract-header-test clj-surgeon.split-proof-gate-test) exit 0, 1374 ms ==========

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.split-proof-gate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-extraction-plan-test) exit 0, 20908 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-extraction-plan-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (2, slowest first, total 4077 ms):
      3828 ms  clj-surgeon.mcp-operation-registry-test
       249 ms  clj-surgeon.mcp-extraction-plan-test

========== lane 11 (clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.edit-dsl-test clj-surgeon.cljc.split-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test) exit 0, 1432 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.mission-forms-test

Testing clj-surgeon.mission-candidate-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 7369 ms ==========

Testing clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.mcp-compact-relations-test) exit 0, 23199 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 13 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 7655 ms):
      7655 ms  clj-surgeon.mcp-compact-relations-test

========== lane 14 (clj-surgeon.mcp-server-test) exit 0, 10930 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 35813 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1508244-10ad5968/clj-surgeon-mcp-server-test-7549445045636806166/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 35465 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1508244-10ad5968/clj-surgeon-mcp-server-test-11191772697832357702/.nrepl-port )

namespace walls (1, slowest first, total 225 ms):
       225 ms  clj-surgeon.mcp-server-test

========== lane 15 (clj-surgeon.namespace-split-warm-test) exit 0, 9416 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1612 ms):
      1612 ms  clj-surgeon.namespace-split-warm-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 7538 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1956 ms):
      1956 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.mcp-tool-test) exit 0, 13566 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2561 ms):
      2561 ms  clj-surgeon.mcp-tool-test

========== lane 18 (clj-surgeon.mcp-http-server-test) exit 0, 16063 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 38487 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1497363-3b0a3cff/clj-surgeon-mcp-http-test-9638429638135171844/.nrepl-port )

namespace walls (1, slowest first, total 3053 ms):
      3053 ms  clj-surgeon.mcp-http-server-test

========== lane 19 (clj-surgeon.outline-corpus-integration-test) exit 0, 14446 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8631 ms):
      8631 ms  clj-surgeon.outline-corpus-integration-test

========== lane 20 (clj-surgeon.mcp-feature-thread-test) exit 0, 55331 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 47448 ms):
     47448 ms  clj-surgeon.mcp-feature-thread-test

Ran 1410 tests containing 15563 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 89627 ms
cadence :fast: serial-equivalent 41198 ms; budget 60000 ms
cadence :integration: serial-equivalent 65486 ms; budget 240000 ms
bb-runtime: serial-equivalent 14599 ms; budget 343102 ms; makespan 12588 ms

namespace walls (101, slowest first, serial-equivalent total 106684 ms):
     47448 ms  clj-surgeon.mcp-feature-thread-test
      8631 ms  clj-surgeon.outline-corpus-integration-test
      7655 ms  clj-surgeon.mcp-compact-relations-test
      6955 ms  clj-surgeon.rename-alias-test
      3828 ms  clj-surgeon.mcp-operation-registry-test
      3259 ms  clj-surgeon.mcp-namespace-split-test
      3053 ms  clj-surgeon.mcp-http-server-test
      2561 ms  clj-surgeon.mcp-tool-test
      2122 ms  clj-surgeon.insert-forms-test
      1956 ms  clj-surgeon.mcp-hot-verify-test
      1894 ms  clj-surgeon.mcp-inspect-tool-test
      1612 ms  clj-surgeon.namespace-split-warm-test
      1424 ms  clj-surgeon.lane-manifest-test
      1183 ms  clj-surgeon.splice-envelope-test
      1015 ms  clj-surgeon.outline-test
       865 ms  clj-surgeon.ns-isolation-test
       862 ms  clj-surgeon.helper-extraction-test
       760 ms  clj-surgeon.operation-algebra-test
       739 ms  clj-surgeon.namespace-split-test
       672 ms  clj-surgeon.analyze-test
       568 ms  clj-surgeon.insert-forms-receipt-test
       518 ms  clj-surgeon.rename-alias-receipt-test
       501 ms  clj-surgeon.mcp-change-buffer-test
       417 ms  clj-surgeon.receipt-booleans-test
       410 ms  clj-surgeon.outline-memory-test
       387 ms  clj-surgeon.scope-stream-test
       344 ms  clj-surgeon.mcp-compact-location-test
       327 ms  clj-surgeon.mcp-inspect-contract-test
       312 ms  clj-surgeon.census-pool-test
       277 ms  clj-surgeon.mcp-create-files-test
       253 ms  clj-surgeon.mcp-expect-guard-test
       249 ms  clj-surgeon.mcp-extraction-plan-test
       225 ms  clj-surgeon.mcp-server-test
       223 ms  clj-surgeon.mcp-intent-contract-test
       204 ms  clj-surgeon.mcp-prepared-request-test
       180 ms  clj-surgeon.mcp-prepared-confirmation-test
       177 ms  clj-surgeon.mcp-combinable-transaction-test
       175 ms  clj-surgeon.alias-migration-test
       143 ms  clj-surgeon.mcp-relation-census-round20-test
       143 ms  clj-surgeon.workspace-onboarding-test
       137 ms  clj-surgeon.mcp-operation-test
       133 ms  clj-surgeon.mission-candidate-race-test
       132 ms  clj-surgeon.mcp-write-refusal-test
       130 ms  clj-surgeon.ls-tree-test
       117 ms  clj-surgeon.mcp-compact-edit-test
       113 ms  clj-surgeon.mcp-formatter-test
       112 ms  clj-surgeon.mcp-operation-async-test
       109 ms  clj-surgeon.mcp-semantic-client-test
       102 ms  clj-surgeon.cljc-existing-ops-test
        88 ms  clj-surgeon.fix-declares-test
        83 ms  clj-surgeon.recovery-test
        78 ms  clj-surgeon.jvm-error-test
        65 ms  clj-surgeon.telemetry-events-test
        64 ms  clj-surgeon.structural-lens-test
        62 ms  clj-surgeon.agent-routing-test
        50 ms  clj-surgeon.move-dependency-test
        42 ms  clj-surgeon.outermost-test
        41 ms  clj-surgeon.mcp-extraction-test
        41 ms  clj-surgeon.relation-census-test
        39 ms  clj-surgeon.battery-parallel-test
        38 ms  clj-surgeon.worktree-lifecycle-io-test
        36 ms  clj-surgeon.mcp-workspace-test
        32 ms  clj-surgeon.cljc.split-test
        32 ms  clj-surgeon.fast-lane-isolation-test
        27 ms  clj-surgeon.owner-hypotheses-test
        26 ms  clj-surgeon.require-change-test
        25 ms  clj-surgeon.mission-plain-forms-test
        24 ms  clj-surgeon.memory-battery-test
        22 ms  clj-surgeon.move-test
        15 ms  clj-surgeon.edit-dsl-test
        14 ms  clj-surgeon.cljc.merge-test
        14 ms  clj-surgeon.mcp-program-tool-test
        13 ms  clj-surgeon.mission-typist-test
        11 ms  clj-surgeon.mcp-contract-test
        10 ms  clj-surgeon.mission-forms-source-test
        10 ms  clj-surgeon.mission-usage-test
         9 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.quoted-var-refs-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.outline-differential-test
         3 ms  clj-surgeon.battery-ledger-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.file-ops-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.mcp-telemetry-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.mcp-read-request-normalization-test
         2 ms  clj-surgeon.split-proof-gate-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-paths-test
         1 ms  clj-surgeon.mission-candidate-test
         1 ms  clj-surgeon.mission-forms-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (21), makespan 89627 ms:
  lane 0     19949 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-operation-async-test
  lane 1       622 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-extraction-test clj-surgeon.syntax-var-refs-test clj-surgeon.insertion-gap-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test clj-surgeon.mcp-compact-edit-fields-test
  lane 2      4006 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.mcp-workspace-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.failure-report-test
  lane 3     17038 ms  exit 0  clj-surgeon.scope-stream-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-semantic-client-test clj-surgeon.mcp-formatter-test
  lane 4     17142 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-relation-census-round20-test
  lane 5      2327 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.telemetry-events-test clj-surgeon.move-test clj-surgeon.require-change-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test clj-surgeon.worktree-lifecycle-test
  lane 6     18440 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-compact-edit-test
  lane 7      1785 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.fix-declares-test clj-surgeon.outermost-test clj-surgeon.mission-typist-test clj-surgeon.cljc.merge-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 8     16723 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-operation-test clj-surgeon.mission-candidate-race-test
  lane 9      1374 ms  exit 0  clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.move-dependency-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.extract-header-test clj-surgeon.split-proof-gate-test
  lane 10     20908 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-extraction-plan-test
  lane 11      1432 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.edit-dsl-test clj-surgeon.cljc.split-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  lane 12      7369 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13     23199 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 14     10930 ms  exit 0  clj-surgeon.mcp-server-test
  lane 15      9416 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 16      7538 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     13566 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 18     16063 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 19     14446 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 20     55331 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 89627 ms over 8 lane(s); serial-equivalent 106684 ms; skipped 0

========== lane 0 (clj-surgeon.edit-test clj-surgeon.platform-selector-test) exit 0, 6821 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.platform-selector-test

========== lane 1 (clj-surgeon.intent-transaction-test) exit 1, 13931 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test

FAIL in (change-cli-dogfoods-one-shot-multi-file-apply-and-exact-undo) (intent_transaction_test.clj:2206)
expected: (< (count out) (count (slurp receipt-file)))
  actual: (not (< 3970 3660))
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5886 ms):
      5886 ms  clj-surgeon.intent-transaction-test

========== lane 2 (clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test) exit 0, 2227 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.extract-header-test

========== lane 3 (clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.edn-config-integration-test) exit 0, 11989 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.edn-config-integration-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1487862-229f87cb/andon-shell-safety2743936854078554505/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1487862-229f87cb/andon-shell-safety2743936854078554505/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1487862-229f87cb/andon-shell-safety9816344937484278706/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1487862-229f87cb/andon-shell-safety9816344937484278706/PWNED-SUBST)"

========== lane 4 (clj-surgeon.operation-algebra-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.syntax-var-refs-test clj-surgeon.failure-report-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1313 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test) exit 0, 15078 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.partition-all-test

========== lane 6 (clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test) exit 0, 751 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.cljc.analyze-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 13693 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.rename-test clj-surgeon.forms-test) exit 0, 1069 ms ==========

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.move-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.forms-test

========== lane 9 (clj-surgeon.cli-dispatch-test) exit 0, 14712 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 10 (clj-surgeon.tmp-leak-support-test) exit 0, 14439 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 10 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1478149-676df6c9/tmp-leak-sweep-guard-14804504620456210805/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 11 (clj-surgeon.parser-admission-test) exit 0, 72437 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 12 (clj-surgeon.install-test) exit 0, 85960 ms ==========

Testing clj-surgeon.install-test

Ran 899 tests containing 7983 assertions.
1 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 85960 ms
cadence :battery: serial-equivalent 238285 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3594 ms; budget 60000 ms
bb-runtime: serial-equivalent 235993 ms; budget 343102 ms; makespan 85960 ms

namespace walls (50, slowest first, serial-equivalent total 241879 ms):
     85808 ms  clj-surgeon.install-test
     72198 ms  clj-surgeon.parser-admission-test
     14280 ms  clj-surgeon.tmp-leak-support-test
     14278 ms  clj-surgeon.cli-dispatch-test
     13276 ms  clj-surgeon.show-form-test
     11954 ms  clj-surgeon.help-test
      6130 ms  clj-surgeon.edit-test
      5886 ms  clj-surgeon.intent-transaction-test
      4939 ms  clj-surgeon.extract-test
      4544 ms  clj-surgeon.core-discovery-test
      2332 ms  clj-surgeon.lens-query-test
      1518 ms  clj-surgeon.xray-test
       920 ms  clj-surgeon.outline-test
       808 ms  clj-surgeon.operation-algebra-test
       734 ms  clj-surgeon.analyze-test
       547 ms  clj-surgeon.edn-config-integration-test
       315 ms  clj-surgeon.partition-all-test
       280 ms  clj-surgeon.platform-selector-test
       191 ms  clj-surgeon.alias-migration-test
       120 ms  clj-surgeon.cljc-existing-ops-test
       116 ms  clj-surgeon.workspace-onboarding-test
       107 ms  clj-surgeon.ls-tree-test
        78 ms  clj-surgeon.jvm-error-test
        73 ms  clj-surgeon.recovery-test
        69 ms  clj-surgeon.move-dependency-test
        55 ms  clj-surgeon.agent-routing-test
        46 ms  clj-surgeon.fix-declares-test
        36 ms  clj-surgeon.structural-lens-test
        30 ms  clj-surgeon.relation-census-test
        29 ms  clj-surgeon.memory-battery-test
        29 ms  clj-surgeon.owner-hypotheses-test
        27 ms  clj-surgeon.edit-dsl-test
        24 ms  clj-surgeon.worktree-lifecycle-io-test
        20 ms  clj-surgeon.outermost-test
        16 ms  clj-surgeon.cljc.split-test
        14 ms  clj-surgeon.move-test
        13 ms  clj-surgeon.worktree-lifecycle-test
         7 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.insertion-gap-test
         5 ms  clj-surgeon.cljc.require-ops-test
         4 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.forms-test
         2 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.cljc.analyze-test
         1 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (13), makespan 85960 ms:
  lane 0      6821 ms  exit 0  clj-surgeon.edit-test clj-surgeon.platform-selector-test
  lane 1     13931 ms  exit 1  clj-surgeon.intent-transaction-test
  lane 2      2227 ms  exit 0  clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test
  lane 3     11989 ms  exit 0  clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.edn-config-integration-test
  lane 4      1313 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.syntax-var-refs-test clj-surgeon.failure-report-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 5     15078 ms  exit 0  clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test
  lane 6       751 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test
  lane 7     13693 ms  exit 0  clj-surgeon.show-form-test
  lane 8      1069 ms  exit 0  clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.rename-test clj-surgeon.forms-test
  lane 9     14712 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 10     14439 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 11     72437 ms  exit 0  clj-surgeon.parser-admission-test
  lane 12     85960 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim

BATTERY-LANE: 1 lane failure(s):
   lane 1 exited 1 (log target/gate-prewarm/c375bec2-ea76-4bb6-befe-408f0ebafaa9/bb/lane-1.out)
battery-parallel: makespan 85960 ms over 8 lane(s); serial-equivalent 241879 ms; skipped 0
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
bash test/performance_regression_sentinel_intent_test.sh
Sentinel intent witness audit passed: 50/50 requirements named
bash test/performance_regression_sentinel_intent_self_test.sh
Sentinel intent audit self-test passed: missing simple/multipart and unknown multipart IDs refused
python3 -B -m unittest discover -s test/oracles -p test_gate_slot.py
python3 -B -m unittest discover -s test/oracles -p test_namespace_split_papercut_oracle.py
# @spec REQUIRE-CHANGE-014
python3 -B -m unittest discover -s test/oracles -p test_require_change_oracle.py
python3 -B -m unittest discover -s test/oracles -p test_cell_b_oracle.py
# @spec MCP-OP-ALIAS-053
ok: a clean repository passes (exit 0)
ok: a cache forced in below the root fails the gate (exit 1)
ok: a cache forced in at the root fails the gate (exit 1)
ok: a repository with no ignore rule fails the gate (exit 1)
ok: a directory git cannot answer for fails the gate (exit 1)
ok: a tree whose file inventory git cannot produce fails the gate (exit 1)
repository hygiene gate self-test: all cases pass
clojure -M test/kernel_warning_check.clj
kernel warning check: 2 namespace(s), 0 warning(s)
MCP heap configuration regression passed
--- tmpfs-literal (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- arm-b (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- name-only (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- devshm (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/dev/shm is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- unknown-fstype (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1516928-1ce31a28 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1516928-1ce31a28 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1516928-1ce31a28
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1519507-e8ef2fa5 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1519507-e8ef2fa5 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1519507-e8ef2fa5
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1520842-0425172a node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1520842-0425172a node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/realdisk/clj-surgeon-suite-1520842-0425172a
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/subbase/clj-surgeon-suite-1520967-206d4b4b node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/subbase/clj-surgeon-suite-1520967-206d4b4b node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/subbase/clj-surgeon-suite-1520967-206d4b4b
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/subbase/clj-surgeon-suite-1520967-206d4b4b/tmp.rZIzXFEJVn
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/subbase/clj-surgeon-suite-1520967-206d4b4b: tmp.rZIzXFEJVn
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/stalebase/clj-surgeon-suite-1521676-6efed58a node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/stalebase/clj-surgeon-suite-1521676-6efed58a node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/stalebase/clj-surgeon-suite-1521676-6efed58a
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-1512473-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9/nowrite/clj-surgeon-suite-1521986-4bb17a5d Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner analyzer-contract-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.memory.memory-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.memory-battery-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.mcp-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- SELF_TEST_TMP with TMPDIR=/tmp -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/tmp/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR unset -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9 -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.Naojy9 ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.QJmZLV/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.QJmZLV/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s........
----------------------------------------------------------------------
Ran 20 tests in 0.395s

OK (skipped=2)
.
----------------------------------------------------------------------
Ran 1 test in 0.000s

OK
....
----------------------------------------------------------------------
Ran 4 tests in 0.003s

OK
...
----------------------------------------------------------------------
Ran 3 tests in 1.429s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.igate-refused: gate-refused: runtime pool failed {:shell-exit 0, :suites [{:suite "alias", :state :passed, :problems []} {:suite "mcp", :state :passed, :problems []} {:suite "bb", :state :failed, :problems ["lane 1 exited 1 (log target/gate-prewarm/c375bec2-ea76-4bb6-befe-408f0ebafaa9/bb/lane-1.out)"]}]}
make: *** [Makefile:1259: landing-gate-prewarm] Error 1
