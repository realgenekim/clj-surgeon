bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 24352 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T12:35:52.342077519Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=119
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=140
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=179
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 10795}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 78124 ms
  process 0 phase 0 (est 5117 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 78124 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 47707 ms
  process 0 phase 0 (est 4493 ms): clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-extraction-plan-test
  process 1 phase 0 (est 294 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.mission-usage-test clj-surgeon.mcp-read-request-normalization-test
  process 2 phase 0 (est 4479 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 3 phase 0 (est 308 ms): clj-surgeon.mcp-operation-test clj-surgeon.mcp-relation-census-round20-test
  process 4 phase 0 (est 3604 ms): clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test
  process 5 phase 0 (est 1184 ms): clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mcp-workspace-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.file-ops-test clj-surgeon.mission-forms-test
  process 6 phase 0 (est 3510 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-compact-edit-test
  process 7 phase 0 (est 1278 ms): clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.structural-lens-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.mission-plain-forms-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.forms-test clj-surgeon.syntax-var-refs-test
  process 8 phase 0 (est 4382 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test
  process 9 phase 0 (est 406 ms): clj-surgeon.mcp-inspect-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.cljc.merge-test clj-surgeon.battery-ledger-test
  process 10 phase 0 (est 4626 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test
  process 11 phase 0 (est 162 ms): clj-surgeon.fix-declares-test clj-surgeon.mcp-extraction-test clj-surgeon.outermost-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.split-proof-gate-test clj-surgeon.mission-candidate-test
  process 12 phase 0 (est 7469 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 8114 ms): clj-surgeon.mcp-compact-relations-test
  process 14 phase 1 (est 231 ms): clj-surgeon.mcp-server-test
  process 15 phase 1 (est 1355 ms): clj-surgeon.namespace-split-warm-test
  process 16 phase 1 (est 2292 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 2646 ms): clj-surgeon.mcp-http-server-test
  process 18 phase 1 (est 2914 ms): clj-surgeon.mcp-tool-test
  process 19 phase 1 (est 9855 ms): clj-surgeon.outline-corpus-integration-test
  process 20 phase 1 (est 47707 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 83139 ms
  process 0 phase 0 (est 7996 ms): clj-surgeon.edit-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  process 1 phase 0 (est 5753 ms): clj-surgeon.intent-transaction-test
  process 2 phase 0 (est 375 ms): clj-surgeon.alias-migration-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test
  process 3 phase 0 (est 13947 ms): clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  process 4 phase 0 (est 177 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 5 phase 0 (est 13048 ms): clj-surgeon.help-test clj-surgeon.platform-selector-test
  process 6 phase 0 (est 1077 ms): clj-surgeon.outline-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.failure-report-test
  process 7 phase 0 (est 13247 ms): clj-surgeon.tmp-leak-support-test
  process 8 phase 0 (est 879 ms): clj-surgeon.operation-algebra-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.syntax-var-refs-test clj-surgeon.forms-test
  process 9 phase 0 (est 13335 ms): clj-surgeon.show-form-test
  process 10 phase 0 (est 791 ms): clj-surgeon.analyze-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test
  process 11 phase 0 (est 13836 ms): clj-surgeon.cli-dispatch-test
  process 12 phase 0 (est 290 ms): clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test
  process 13 phase 0 (est 72165 ms): clj-surgeon.parser-admission-test
  process 14 phase 0 (est 83139 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 13696 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4608 ms):
      4608 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 88976 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 76521 ms):
     76521 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3641 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 88977 ms
cadence :battery: serial-equivalent 81129 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 81129 ms):
     76521 ms  clj-surgeon.mcp-alias-migration-test
      4608 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 88977 ms:
  lane 0     13696 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     88976 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 88977 ms over 2 lane(s); serial-equivalent 81129 ms; skipped 0

========== lane 0 (clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-extraction-plan-test) exit 0, 20542 ms ==========
lanes: --ns -- 7 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-extraction-plan-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (7, slowest first, total 4456 ms):
      1476 ms  clj-surgeon.splice-envelope-test
      1096 ms  clj-surgeon.ns-isolation-test
       729 ms  clj-surgeon.rename-alias-receipt-test
       488 ms  clj-surgeon.outline-memory-test
       328 ms  clj-surgeon.mcp-compact-location-test
       205 ms  clj-surgeon.mcp-prepared-request-test
       134 ms  clj-surgeon.mcp-extraction-plan-test

========== lane 1 (clj-surgeon.cljc-existing-ops-test clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.mission-usage-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 939 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 2 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 5137 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.outline-test

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.mcp-compact-edit-fields-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 3 (clj-surgeon.mcp-operation-test clj-surgeon.mcp-relation-census-round20-test) exit 0, 11234 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-test

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
---------- lane 3 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (2, slowest first, total 409 ms):
       207 ms  clj-surgeon.mcp-relation-census-round20-test
       202 ms  clj-surgeon.mcp-operation-test

========== lane 4 (clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test) exit 0, 19869 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 4580 ms):
      2769 ms  clj-surgeon.insert-forms-test
       811 ms  clj-surgeon.insert-forms-receipt-test
       368 ms  clj-surgeon.scope-stream-test
       305 ms  clj-surgeon.census-pool-test
       199 ms  clj-surgeon.mcp-prepared-confirmation-test
       128 ms  clj-surgeon.mcp-formatter-test

========== lane 5 (clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mcp-workspace-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.file-ops-test clj-surgeon.mission-forms-test) exit 0, 1724 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.move-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mission-forms-test

========== lane 6 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-compact-edit-test) exit 0, 18723 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 3568 ms):
      2681 ms  clj-surgeon.mcp-inspect-tool-test
       422 ms  clj-surgeon.mcp-change-buffer-test
       324 ms  clj-surgeon.mcp-expect-guard-test
       141 ms  clj-surgeon.mcp-compact-edit-test

========== lane 7 (clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.structural-lens-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.mission-plain-forms-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.forms-test clj-surgeon.syntax-var-refs-test) exit 0, 1795 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test) exit 0, 21962 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-operation-async-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 2882 ms):
      2132 ms  clj-surgeon.mcp-namespace-split-test
       352 ms  clj-surgeon.receipt-booleans-test
       170 ms  clj-surgeon.mcp-combinable-transaction-test
       117 ms  clj-surgeon.mcp-operation-async-test
       111 ms  clj-surgeon.mcp-semantic-client-test

========== lane 9 (clj-surgeon.mcp-inspect-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.cljc.merge-test clj-surgeon.battery-ledger-test) exit 0, 1089 ms ==========

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.battery-ledger-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test) exit 0, 22715 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 4909 ms):
      4280 ms  clj-surgeon.mcp-operation-registry-test
       337 ms  clj-surgeon.mcp-create-files-test
       159 ms  clj-surgeon.mcp-write-refusal-test
       133 ms  clj-surgeon.mission-candidate-race-test

========== lane 11 (clj-surgeon.fix-declares-test clj-surgeon.mcp-extraction-test clj-surgeon.outermost-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.split-proof-gate-test clj-surgeon.mission-candidate-test) exit 0, 639 ms ==========

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.split-proof-gate-test

Testing clj-surgeon.mission-candidate-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 7116 ms ==========

Testing clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.mcp-compact-relations-test) exit 0, 23368 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 13 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8359 ms):
      8359 ms  clj-surgeon.mcp-compact-relations-test

========== lane 14 (clj-surgeon.mcp-server-test) exit 0, 10222 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 44389 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2108109-b9748ada/clj-surgeon-mcp-server-test-14114896349474727746/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 37301 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2108109-b9748ada/clj-surgeon-mcp-server-test-9608316534132439828/.nrepl-port )

namespace walls (1, slowest first, total 199 ms):
       199 ms  clj-surgeon.mcp-server-test

========== lane 15 (clj-surgeon.namespace-split-warm-test) exit 0, 10041 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1435 ms):
      1435 ms  clj-surgeon.namespace-split-warm-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 7498 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2283 ms):
      2283 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.mcp-http-server-test) exit 0, 15329 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 44185 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2099201-2d16905e/clj-surgeon-mcp-http-test-5064567038274173505/.nrepl-port )

namespace walls (1, slowest first, total 2337 ms):
      2337 ms  clj-surgeon.mcp-http-server-test

========== lane 18 (clj-surgeon.mcp-tool-test) exit 0, 13658 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 3028 ms):
      3028 ms  clj-surgeon.mcp-tool-test

========== lane 19 (clj-surgeon.outline-corpus-integration-test) exit 0, 14824 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8923 ms):
      8923 ms  clj-surgeon.outline-corpus-integration-test

========== lane 20 (clj-surgeon.mcp-feature-thread-test) exit 0, 54499 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 47238 ms):
     47238 ms  clj-surgeon.mcp-feature-thread-test

Ran 1411 tests containing 15576 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 88568 ms
cadence :fast: serial-equivalent 43566 ms; budget 60000 ms
cadence :integration: serial-equivalent 65443 ms; budget 240000 ms
bb-runtime: serial-equivalent 14403 ms; budget 343102 ms; makespan 10244 ms

namespace walls (101, slowest first, serial-equivalent total 109009 ms):
     47238 ms  clj-surgeon.mcp-feature-thread-test
      8923 ms  clj-surgeon.outline-corpus-integration-test
      8359 ms  clj-surgeon.mcp-compact-relations-test
      6731 ms  clj-surgeon.rename-alias-test
      4280 ms  clj-surgeon.mcp-operation-registry-test
      3028 ms  clj-surgeon.mcp-tool-test
      2769 ms  clj-surgeon.insert-forms-test
      2681 ms  clj-surgeon.mcp-inspect-tool-test
      2337 ms  clj-surgeon.mcp-http-server-test
      2283 ms  clj-surgeon.mcp-hot-verify-test
      2132 ms  clj-surgeon.mcp-namespace-split-test
      1476 ms  clj-surgeon.splice-envelope-test
      1435 ms  clj-surgeon.namespace-split-warm-test
      1353 ms  clj-surgeon.lane-manifest-test
      1096 ms  clj-surgeon.ns-isolation-test
       960 ms  clj-surgeon.outline-test
       863 ms  clj-surgeon.helper-extraction-test
       811 ms  clj-surgeon.insert-forms-receipt-test
       762 ms  clj-surgeon.operation-algebra-test
       745 ms  clj-surgeon.namespace-split-test
       729 ms  clj-surgeon.rename-alias-receipt-test
       708 ms  clj-surgeon.analyze-test
       488 ms  clj-surgeon.outline-memory-test
       422 ms  clj-surgeon.mcp-change-buffer-test
       368 ms  clj-surgeon.scope-stream-test
       352 ms  clj-surgeon.receipt-booleans-test
       337 ms  clj-surgeon.mcp-create-files-test
       328 ms  clj-surgeon.mcp-compact-location-test
       324 ms  clj-surgeon.mcp-expect-guard-test
       309 ms  clj-surgeon.mcp-inspect-contract-test
       305 ms  clj-surgeon.census-pool-test
       253 ms  clj-surgeon.mcp-intent-contract-test
       207 ms  clj-surgeon.mcp-relation-census-round20-test
       205 ms  clj-surgeon.mcp-prepared-request-test
       202 ms  clj-surgeon.mcp-operation-test
       199 ms  clj-surgeon.mcp-prepared-confirmation-test
       199 ms  clj-surgeon.mcp-server-test
       188 ms  clj-surgeon.alias-migration-test
       170 ms  clj-surgeon.mcp-combinable-transaction-test
       159 ms  clj-surgeon.mcp-write-refusal-test
       147 ms  clj-surgeon.ls-tree-test
       141 ms  clj-surgeon.mcp-compact-edit-test
       140 ms  clj-surgeon.cljc-existing-ops-test
       134 ms  clj-surgeon.mcp-extraction-plan-test
       133 ms  clj-surgeon.mission-candidate-race-test
       128 ms  clj-surgeon.mcp-formatter-test
       127 ms  clj-surgeon.workspace-onboarding-test
       122 ms  clj-surgeon.move-dependency-test
       117 ms  clj-surgeon.mcp-operation-async-test
       111 ms  clj-surgeon.mcp-semantic-client-test
        84 ms  clj-surgeon.jvm-error-test
        71 ms  clj-surgeon.fix-declares-test
        65 ms  clj-surgeon.telemetry-events-test
        64 ms  clj-surgeon.recovery-test
        64 ms  clj-surgeon.worktree-lifecycle-io-test
        47 ms  clj-surgeon.agent-routing-test
        45 ms  clj-surgeon.mission-plain-forms-test
        41 ms  clj-surgeon.structural-lens-test
        40 ms  clj-surgeon.mcp-extraction-test
        35 ms  clj-surgeon.require-change-test
        34 ms  clj-surgeon.battery-parallel-test
        34 ms  clj-surgeon.mcp-program-tool-test
        34 ms  clj-surgeon.mcp-workspace-test
        33 ms  clj-surgeon.move-test
        31 ms  clj-surgeon.fast-lane-isolation-test
        29 ms  clj-surgeon.outermost-test
        26 ms  clj-surgeon.owner-hypotheses-test
        26 ms  clj-surgeon.relation-census-test
        25 ms  clj-surgeon.memory-battery-test
        21 ms  clj-surgeon.cljc.split-test
        17 ms  clj-surgeon.mcp-contract-test
        17 ms  clj-surgeon.quoted-var-refs-test
        16 ms  clj-surgeon.worktree-lifecycle-test
        14 ms  clj-surgeon.edit-dsl-test
         8 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.mission-forms-source-test
         7 ms  clj-surgeon.mission-typist-test
         6 ms  clj-surgeon.cljc.analyze-test
         6 ms  clj-surgeon.rename-test
         5 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.mcp-telemetry-test
         4 ms  clj-surgeon.outline-differential-test
         3 ms  clj-surgeon.battery-ledger-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.mission-usage-test
         3 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.mcp-compact-edit-fields-test
         2 ms  clj-surgeon.mission-forms-test
         2 ms  clj-surgeon.split-proof-gate-test
         1 ms  clj-surgeon.failure-report-test
         1 ms  clj-surgeon.file-ops-test
         1 ms  clj-surgeon.mcp-read-request-normalization-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (21), makespan 88568 ms:
  lane 0     20542 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-extraction-plan-test
  lane 1       939 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.mission-usage-test clj-surgeon.mcp-read-request-normalization-test
  lane 2      5137 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-program-tool-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 3     11234 ms  exit 0  clj-surgeon.mcp-operation-test clj-surgeon.mcp-relation-census-round20-test
  lane 4     19869 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test
  lane 5      1724 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mcp-workspace-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.mcp-recovery-test clj-surgeon.file-ops-test clj-surgeon.mission-forms-test
  lane 6     18723 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-compact-edit-test
  lane 7      1795 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.structural-lens-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.mission-plain-forms-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.forms-test clj-surgeon.syntax-var-refs-test
  lane 8     21962 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test
  lane 9      1089 ms  exit 0  clj-surgeon.mcp-inspect-contract-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.cljc.merge-test clj-surgeon.battery-ledger-test
  lane 10     22715 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test
  lane 11       639 ms  exit 0  clj-surgeon.fix-declares-test clj-surgeon.mcp-extraction-test clj-surgeon.outermost-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.split-proof-gate-test clj-surgeon.mission-candidate-test
  lane 12      7116 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13     23368 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 14     10222 ms  exit 0  clj-surgeon.mcp-server-test
  lane 15     10041 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 16      7498 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     15329 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 18     13658 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 19     14824 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 20     54499 ms  exit 0  clj-surgeon.mcp-feature-thread-test

TEST-ISOLATION: 4 violation(s) -- the suite's own purity rules, per namespace:
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-compact-relations-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt14/DOGFOOD.md was modified in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-namespace-split-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt14/DOGFOOD.md was modified in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-operation-registry-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt14/DOGFOOD.md was modified in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-prepared-request-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt14/DOGFOOD.md was modified in the repository working tree
battery-parallel: makespan 88568 ms over 8 lane(s); serial-equivalent 109009 ms; skipped 0

========== lane 0 (clj-surgeon.edit-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 8415 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test

========== lane 1 (clj-surgeon.intent-transaction-test) exit 0, 12693 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5486 ms):
      5486 ms  clj-surgeon.intent-transaction-test

========== lane 2 (clj-surgeon.alias-migration-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test) exit 0, 791 ms ==========

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.move-test

Testing clj-surgeon.insertion-gap-test

========== lane 3 (clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test) exit 0, 13624 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.edn-config-integration-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2077767-e2b79051/andon-shell-safety14472543418937149404/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2077767-e2b79051/andon-shell-safety14472543418937149404/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2077767-e2b79051/andon-shell-safety12788301840653750596/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2077767-e2b79051/andon-shell-safety12788301840653750596/PWNED-SUBST)"

========== lane 4 (clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 453 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.platform-selector-test) exit 0, 12070 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.platform-selector-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.failure-report-test) exit 0, 1347 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.failure-report-test

========== lane 7 (clj-surgeon.tmp-leak-support-test) exit 0, 13354 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 7 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2085046-1b88e499/tmp-leak-sweep-guard-14024800468053765870/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 8 (clj-surgeon.operation-algebra-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.syntax-var-refs-test clj-surgeon.forms-test) exit 0, 1285 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.forms-test

========== lane 9 (clj-surgeon.show-form-test) exit 0, 13968 ms ==========

Testing clj-surgeon.show-form-test

========== lane 10 (clj-surgeon.analyze-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test) exit 0, 1092 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.extract-header-test

========== lane 11 (clj-surgeon.cli-dispatch-test) exit 0, 14434 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 12 (clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test) exit 0, 698 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.cljc.split-test

========== lane 13 (clj-surgeon.parser-admission-test) exit 0, 72085 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 14 (clj-surgeon.install-test) exit 0, 85309 ms ==========

Testing clj-surgeon.install-test

Ran 899 tests containing 7984 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 85309 ms
cadence :battery: serial-equivalent 236105 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3485 ms; budget 60000 ms
bb-runtime: serial-equivalent 234104 ms; budget 343102 ms; makespan 85309 ms

namespace walls (50, slowest first, serial-equivalent total 239590 ms):
     85154 ms  clj-surgeon.install-test
     71863 ms  clj-surgeon.parser-admission-test
     13998 ms  clj-surgeon.cli-dispatch-test
     13552 ms  clj-surgeon.show-form-test
     13217 ms  clj-surgeon.tmp-leak-support-test
     11404 ms  clj-surgeon.help-test
      6162 ms  clj-surgeon.edit-test
      5486 ms  clj-surgeon.intent-transaction-test
      5475 ms  clj-surgeon.extract-test
      4668 ms  clj-surgeon.core-discovery-test
      2439 ms  clj-surgeon.lens-query-test
      1445 ms  clj-surgeon.xray-test
       914 ms  clj-surgeon.outline-test
       761 ms  clj-surgeon.operation-algebra-test
       680 ms  clj-surgeon.analyze-test
       598 ms  clj-surgeon.edn-config-integration-test
       366 ms  clj-surgeon.partition-all-test
       278 ms  clj-surgeon.platform-selector-test
       197 ms  clj-surgeon.alias-migration-test
       122 ms  clj-surgeon.jvm-error-test
       114 ms  clj-surgeon.workspace-onboarding-test
       103 ms  clj-surgeon.ls-tree-test
        98 ms  clj-surgeon.cljc-existing-ops-test
        62 ms  clj-surgeon.recovery-test
        60 ms  clj-surgeon.structural-lens-test
        49 ms  clj-surgeon.move-dependency-test
        47 ms  clj-surgeon.agent-routing-test
        36 ms  clj-surgeon.outermost-test
        32 ms  clj-surgeon.relation-census-test
        31 ms  clj-surgeon.owner-hypotheses-test
        30 ms  clj-surgeon.fix-declares-test
        26 ms  clj-surgeon.worktree-lifecycle-io-test
        21 ms  clj-surgeon.memory-battery-test
        17 ms  clj-surgeon.cljc.split-test
        16 ms  clj-surgeon.edit-dsl-test
        15 ms  clj-surgeon.move-test
        11 ms  clj-surgeon.worktree-lifecycle-test
         9 ms  clj-surgeon.quoted-var-refs-test
         8 ms  clj-surgeon.insertion-gap-test
         6 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.syntax-var-refs-test
         3 ms  clj-surgeon.cljc.merge-test
         2 ms  clj-surgeon.failure-report-test
         1 ms  clj-surgeon.cljc.analyze-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (15), makespan 85309 ms:
  lane 0      8415 ms  exit 0  clj-surgeon.edit-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 1     12693 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 2       791 ms  exit 0  clj-surgeon.alias-migration-test clj-surgeon.cljc-existing-ops-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test
  lane 3     13624 ms  exit 0  clj-surgeon.extract-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  lane 4       453 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 5     12070 ms  exit 0  clj-surgeon.help-test clj-surgeon.platform-selector-test
  lane 6      1347 ms  exit 0  clj-surgeon.outline-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.failure-report-test
  lane 7     13354 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 8      1285 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.syntax-var-refs-test clj-surgeon.forms-test
  lane 9     13968 ms  exit 0  clj-surgeon.show-form-test
  lane 10      1092 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test
  lane 11     14434 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 12       698 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.cljc.split-test
  lane 13     72085 ms  exit 0  clj-surgeon.parser-admission-test
  lane 14     85309 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 85309 ms over 8 lane(s); serial-equivalent 239590 ms; skipped 0
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2114547-c4fc23c9 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2114547-c4fc23c9 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2114547-c4fc23c9
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2117072-e46def5c node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2117072-e46def5c node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2117072-e46def5c
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2118393-b7f0c3f2 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2118393-b7f0c3f2 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/realdisk/clj-surgeon-suite-2118393-b7f0c3f2
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/subbase/clj-surgeon-suite-2118506-5b474a28 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/subbase/clj-surgeon-suite-2118506-5b474a28 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/subbase/clj-surgeon-suite-2118506-5b474a28
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/subbase/clj-surgeon-suite-2118506-5b474a28/tmp.zlvCqE7CaE
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/subbase/clj-surgeon-suite-2118506-5b474a28: tmp.zlvCqE7CaE
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/stalebase/clj-surgeon-suite-2121033-241cfc73 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/stalebase/clj-surgeon-suite-2121033-241cfc73 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/stalebase/clj-surgeon-suite-2121033-241cfc73
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-2111639-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX/nowrite/clj-surgeon-suite-2122357-f26a0d62 Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.I7fyoX ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.8UWFe0/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.8UWFe0/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s........
----------------------------------------------------------------------
Ran 20 tests in 0.384s

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
Ran 3 tests in 1.331s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.igate-refused: gate-refused: runtime pool failed {:shell-exit 0, :suites [{:suite "alias", :state :passed, :problems []} {:suite "mcp", :state :failed, :problems []} {:suite "bb", :state :passed, :problems []}]}
make: *** [Makefile:1259: landing-gate-prewarm] Error 1
