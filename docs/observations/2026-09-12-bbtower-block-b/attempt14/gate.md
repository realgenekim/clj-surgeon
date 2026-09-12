bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 24333 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T12:40:18.457917048Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=128
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=139
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=191
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11089}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 76521 ms
  process 0 phase 0 (est 4608 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 76521 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 47238 ms
  process 0 phase 0 (est 2501 ms): clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-contract-test clj-surgeon.worktree-lifecycle-test clj-surgeon.outline-differential-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  process 1 phase 0 (est 2245 ms): clj-surgeon.ns-isolation-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-formatter-test
  process 2 phase 0 (est 3338 ms): clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test
  process 3 phase 0 (est 1408 ms): clj-surgeon.outline-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.relation-census-test clj-surgeon.mission-forms-source-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-read-request-normalization-test
  process 4 phase 0 (est 3078 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test
  process 5 phase 0 (est 1668 ms): clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.require-change-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-typist-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-recovery-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test
  process 6 phase 0 (est 3392 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.scope-stream-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-compact-edit-test
  process 7 phase 0 (est 1354 ms): clj-surgeon.namespace-split-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.move-dependency-test clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.cljc.merge-test clj-surgeon.syntax-var-refs-test
  process 8 phase 0 (est 4471 ms): clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  process 9 phase 0 (est 275 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.fast-lane-isolation-test clj-surgeon.memory-battery-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-schema-test clj-surgeon.forms-test clj-surgeon.split-proof-gate-test
  process 10 phase 0 (est 4280 ms): clj-surgeon.mcp-operation-registry-test
  process 11 phase 0 (est 466 ms): clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.agent-routing-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 12 phase 0 (est 6731 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 8359 ms): clj-surgeon.mcp-compact-relations-test
  process 14 phase 1 (est 199 ms): clj-surgeon.mcp-server-test
  process 15 phase 1 (est 1435 ms): clj-surgeon.namespace-split-warm-test
  process 16 phase 1 (est 2283 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 2337 ms): clj-surgeon.mcp-http-server-test
  process 18 phase 1 (est 3028 ms): clj-surgeon.mcp-tool-test
  process 19 phase 1 (est 8923 ms): clj-surgeon.outline-corpus-integration-test
  process 20 phase 1 (est 47238 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 85154 ms
  process 0 phase 0 (est 5486 ms): clj-surgeon.intent-transaction-test
  process 1 phase 0 (est 7286 ms): clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  process 2 phase 0 (est 942 ms): clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 3 phase 0 (est 13269 ms): clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test
  process 4 phase 0 (est 446 ms): clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.fix-declares-test clj-surgeon.rename-test clj-surgeon.forms-test
  process 5 phase 0 (est 12002 ms): clj-surgeon.help-test clj-surgeon.edn-config-integration-test
  process 6 phase 0 (est 1713 ms): clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test
  process 7 phase 0 (est 13495 ms): clj-surgeon.tmp-leak-support-test clj-surgeon.platform-selector-test
  process 8 phase 0 (est 220 ms): clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  process 9 phase 0 (est 13552 ms): clj-surgeon.show-form-test
  process 10 phase 0 (est 164 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.move-test clj-surgeon.syntax-var-refs-test
  process 11 phase 0 (est 13998 ms): clj-surgeon.cli-dispatch-test
  process 12 phase 0 (est 71863 ms): clj-surgeon.parser-admission-test
  process 13 phase 0 (est 85154 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 14165 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4110 ms):
      4110 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 89471 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 77264 ms):
     77264 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3641 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 89472 ms
cadence :battery: serial-equivalent 81374 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 81374 ms):
     77264 ms  clj-surgeon.mcp-alias-migration-test
      4110 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 89472 ms:
  lane 0     14165 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     89471 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 89472 ms over 2 lane(s); serial-equivalent 81374 ms; skipped 0

========== lane 0 (clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-contract-test clj-surgeon.worktree-lifecycle-test clj-surgeon.outline-differential-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test) exit 0, 3206 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.mission-forms-test

Testing clj-surgeon.mission-candidate-test

========== lane 1 (clj-surgeon.ns-isolation-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-formatter-test) exit 0, 17748 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 2257 ms):
      1011 ms  clj-surgeon.ns-isolation-test
       534 ms  clj-surgeon.outline-memory-test
       374 ms  clj-surgeon.mcp-compact-location-test
       220 ms  clj-surgeon.mcp-prepared-request-test
       118 ms  clj-surgeon.mcp-formatter-test

========== lane 2 (clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test) exit 0, 19154 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 3466 ms):
      1493 ms  clj-surgeon.splice-envelope-test
       908 ms  clj-surgeon.insert-forms-receipt-test
       473 ms  clj-surgeon.mcp-change-buffer-test
       328 ms  clj-surgeon.mcp-create-files-test
       145 ms  clj-surgeon.mcp-write-refusal-test
       119 ms  clj-surgeon.mission-candidate-race-test

========== lane 3 (clj-surgeon.outline-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.relation-census-test clj-surgeon.mission-forms-source-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 2037 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.mcp-compact-edit-fields-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 4 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test) exit 0, 19971 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-expect-guard-test

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

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 3892 ms):
      3058 ms  clj-surgeon.mcp-namespace-split-test
       249 ms  clj-surgeon.mcp-expect-guard-test
       164 ms  clj-surgeon.mcp-combinable-transaction-test
       164 ms  clj-surgeon.mcp-extraction-plan-test
       139 ms  clj-surgeon.mcp-relation-census-round20-test
       118 ms  clj-surgeon.mcp-semantic-client-test

========== lane 5 (clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.require-change-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-typist-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-recovery-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test) exit 0, 2192 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

========== lane 6 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.scope-stream-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-compact-edit-test) exit 0, 16526 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.mcp-operation-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 2804 ms):
      2078 ms  clj-surgeon.mcp-inspect-tool-test
       483 ms  clj-surgeon.scope-stream-test
       123 ms  clj-surgeon.mcp-operation-test
       120 ms  clj-surgeon.mcp-compact-edit-test

========== lane 7 (clj-surgeon.namespace-split-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.move-dependency-test clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.cljc.merge-test clj-surgeon.syntax-var-refs-test) exit 0, 1969 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.move-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 8 (clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test) exit 0, 21653 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 3405 ms):
      1994 ms  clj-surgeon.insert-forms-test
       523 ms  clj-surgeon.rename-alias-receipt-test
       308 ms  clj-surgeon.receipt-booleans-test
       237 ms  clj-surgeon.census-pool-test
       227 ms  clj-surgeon.mcp-prepared-confirmation-test
       116 ms  clj-surgeon.mcp-operation-async-test

========== lane 9 (clj-surgeon.cljc-existing-ops-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.fast-lane-isolation-test clj-surgeon.memory-battery-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-schema-test clj-surgeon.forms-test clj-surgeon.split-proof-gate-test) exit 0, 688 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.split-proof-gate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test) exit 0, 21780 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4183 ms):
      4183 ms  clj-surgeon.mcp-operation-registry-test

========== lane 11 (clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.agent-routing-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1145 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 7618 ms ==========

Testing clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.mcp-compact-relations-test) exit 0, 23371 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 13 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8286 ms):
      8286 ms  clj-surgeon.mcp-compact-relations-test

========== lane 14 (clj-surgeon.mcp-server-test) exit 0, 11692 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 38963 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2212127-cddcfbf1/clj-surgeon-mcp-server-test-13038004267897362138/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 39809 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2212127-cddcfbf1/clj-surgeon-mcp-server-test-7086144758658042669/.nrepl-port )

namespace walls (1, slowest first, total 200 ms):
       200 ms  clj-surgeon.mcp-server-test

========== lane 15 (clj-surgeon.namespace-split-warm-test) exit 0, 10538 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1576 ms):
      1576 ms  clj-surgeon.namespace-split-warm-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 7139 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2235 ms):
      2235 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.mcp-http-server-test) exit 0, 15696 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 35077 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2206600-b543ca12/clj-surgeon-mcp-http-test-11891508680791461728/.nrepl-port )

namespace walls (1, slowest first, total 2782 ms):
      2782 ms  clj-surgeon.mcp-http-server-test

========== lane 18 (clj-surgeon.mcp-tool-test) exit 0, 13718 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2931 ms):
      2931 ms  clj-surgeon.mcp-tool-test

========== lane 19 (clj-surgeon.outline-corpus-integration-test) exit 0, 15376 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9495 ms):
      9495 ms  clj-surgeon.outline-corpus-integration-test

========== lane 20 (clj-surgeon.mcp-feature-thread-test) exit 0, 55430 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 48387 ms):
     48387 ms  clj-surgeon.mcp-feature-thread-test

Ran 1411 tests containing 15576 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 89255 ms
cadence :fast: serial-equivalent 43167 ms; budget 60000 ms
cadence :integration: serial-equivalent 67606 ms; budget 240000 ms
bb-runtime: serial-equivalent 14874 ms; budget 343102 ms; makespan 15657 ms

namespace walls (101, slowest first, serial-equivalent total 110773 ms):
     48387 ms  clj-surgeon.mcp-feature-thread-test
      9495 ms  clj-surgeon.outline-corpus-integration-test
      8286 ms  clj-surgeon.mcp-compact-relations-test
      7230 ms  clj-surgeon.rename-alias-test
      4183 ms  clj-surgeon.mcp-operation-registry-test
      3058 ms  clj-surgeon.mcp-namespace-split-test
      2931 ms  clj-surgeon.mcp-tool-test
      2782 ms  clj-surgeon.mcp-http-server-test
      2235 ms  clj-surgeon.mcp-hot-verify-test
      2078 ms  clj-surgeon.mcp-inspect-tool-test
      1994 ms  clj-surgeon.insert-forms-test
      1576 ms  clj-surgeon.namespace-split-warm-test
      1493 ms  clj-surgeon.splice-envelope-test
      1326 ms  clj-surgeon.lane-manifest-test
      1099 ms  clj-surgeon.outline-test
      1011 ms  clj-surgeon.ns-isolation-test
       908 ms  clj-surgeon.insert-forms-receipt-test
       855 ms  clj-surgeon.helper-extraction-test
       828 ms  clj-surgeon.operation-algebra-test
       728 ms  clj-surgeon.namespace-split-test
       706 ms  clj-surgeon.analyze-test
       534 ms  clj-surgeon.outline-memory-test
       523 ms  clj-surgeon.rename-alias-receipt-test
       483 ms  clj-surgeon.scope-stream-test
       473 ms  clj-surgeon.mcp-change-buffer-test
       374 ms  clj-surgeon.mcp-compact-location-test
       328 ms  clj-surgeon.mcp-create-files-test
       310 ms  clj-surgeon.mcp-inspect-contract-test
       308 ms  clj-surgeon.receipt-booleans-test
       249 ms  clj-surgeon.mcp-expect-guard-test
       237 ms  clj-surgeon.census-pool-test
       234 ms  clj-surgeon.mcp-intent-contract-test
       227 ms  clj-surgeon.mcp-prepared-confirmation-test
       220 ms  clj-surgeon.mcp-prepared-request-test
       200 ms  clj-surgeon.mcp-server-test
       164 ms  clj-surgeon.mcp-combinable-transaction-test
       164 ms  clj-surgeon.mcp-extraction-plan-test
       160 ms  clj-surgeon.alias-migration-test
       149 ms  clj-surgeon.ls-tree-test
       145 ms  clj-surgeon.mcp-write-refusal-test
       139 ms  clj-surgeon.mcp-relation-census-round20-test
       135 ms  clj-surgeon.workspace-onboarding-test
       125 ms  clj-surgeon.cljc-existing-ops-test
       123 ms  clj-surgeon.mcp-operation-test
       120 ms  clj-surgeon.mcp-compact-edit-test
       119 ms  clj-surgeon.mission-candidate-race-test
       118 ms  clj-surgeon.mcp-formatter-test
       118 ms  clj-surgeon.mcp-semantic-client-test
       116 ms  clj-surgeon.mcp-operation-async-test
       111 ms  clj-surgeon.telemetry-events-test
        80 ms  clj-surgeon.jvm-error-test
        56 ms  clj-surgeon.move-dependency-test
        55 ms  clj-surgeon.structural-lens-test
        51 ms  clj-surgeon.mcp-workspace-test
        46 ms  clj-surgeon.fast-lane-isolation-test
        46 ms  clj-surgeon.recovery-test
        42 ms  clj-surgeon.agent-routing-test
        36 ms  clj-surgeon.battery-parallel-test
        35 ms  clj-surgeon.mcp-extraction-test
        31 ms  clj-surgeon.fix-declares-test
        31 ms  clj-surgeon.relation-census-test
        27 ms  clj-surgeon.require-change-test
        26 ms  clj-surgeon.owner-hypotheses-test
        25 ms  clj-surgeon.mission-plain-forms-test
        24 ms  clj-surgeon.mcp-contract-test
        24 ms  clj-surgeon.worktree-lifecycle-io-test
        21 ms  clj-surgeon.mcp-program-tool-test
        21 ms  clj-surgeon.memory-battery-test
        21 ms  clj-surgeon.outermost-test
        19 ms  clj-surgeon.edit-dsl-test
        18 ms  clj-surgeon.worktree-lifecycle-test
        17 ms  clj-surgeon.move-test
        15 ms  clj-surgeon.mission-forms-source-test
        12 ms  clj-surgeon.rename-test
        10 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.insertion-gap-test
         8 ms  clj-surgeon.mission-typist-test
         8 ms  clj-surgeon.outline-differential-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.cljc.require-ops-test
         5 ms  clj-surgeon.mcp-compact-edit-fields-test
         5 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.mcp-read-request-normalization-test
         4 ms  clj-surgeon.mcp-telemetry-test
         3 ms  clj-surgeon.battery-ledger-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mission-usage-test
         3 ms  clj-surgeon.split-proof-gate-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.failure-report-test
         2 ms  clj-surgeon.mcp-recovery-test
         2 ms  clj-surgeon.mission-forms-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.file-ops-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (21), makespan 89255 ms:
  lane 0      3206 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-contract-test clj-surgeon.worktree-lifecycle-test clj-surgeon.outline-differential-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  lane 1     17748 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-formatter-test
  lane 2     19154 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mission-candidate-race-test
  lane 3      2037 ms  exit 0  clj-surgeon.outline-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.relation-census-test clj-surgeon.mission-forms-source-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mcp-read-request-normalization-test
  lane 4     19971 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test
  lane 5      2192 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.analyze-test clj-surgeon.require-change-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-typist-test clj-surgeon.mcp-telemetry-test clj-surgeon.mcp-recovery-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test
  lane 6     16526 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.scope-stream-test clj-surgeon.mcp-operation-test clj-surgeon.mcp-compact-edit-test
  lane 7      1969 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.move-dependency-test clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.cljc.merge-test clj-surgeon.syntax-var-refs-test
  lane 8     21653 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  lane 9       688 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.fast-lane-isolation-test clj-surgeon.memory-battery-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-schema-test clj-surgeon.forms-test clj-surgeon.split-proof-gate-test
  lane 10     21780 ms  exit 0  clj-surgeon.mcp-operation-registry-test
  lane 11      1145 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.agent-routing-test clj-surgeon.battery-parallel-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 12      7618 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13     23371 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 14     11692 ms  exit 0  clj-surgeon.mcp-server-test
  lane 15     10538 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 16      7139 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     15696 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 18     13718 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 19     15376 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 20     55430 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 89255 ms over 8 lane(s); serial-equivalent 110773 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 14113 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5579 ms):
      5579 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 7590 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test

========== lane 2 (clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1309 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test) exit 0, 13663 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2191022-b58944ab/andon-shell-safety8828300962775659839/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2191022-b58944ab/andon-shell-safety8828300962775659839/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2191022-b58944ab/andon-shell-safety17517150861553334823/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2191022-b58944ab/andon-shell-safety17517150861553334823/PWNED-SUBST)"

========== lane 4 (clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.fix-declares-test clj-surgeon.rename-test clj-surgeon.forms-test) exit 0, 826 ms ==========

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.forms-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.edn-config-integration-test) exit 0, 11956 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.edn-config-integration-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test) exit 0, 2137 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.cljc.analyze-test

========== lane 7 (clj-surgeon.tmp-leak-support-test clj-surgeon.platform-selector-test) exit 0, 14145 ms ==========

Testing clj-surgeon.tmp-leak-support-test

Testing clj-surgeon.platform-selector-test
---------- lane 7 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2182653-70d962bc/tmp-leak-sweep-guard-5258609649805356847/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 8 (clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test) exit 0, 760 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.failure-report-test

========== lane 9 (clj-surgeon.show-form-test) exit 0, 13842 ms ==========

Testing clj-surgeon.show-form-test

========== lane 10 (clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.move-test clj-surgeon.syntax-var-refs-test) exit 0, 649 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.move-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 11 (clj-surgeon.cli-dispatch-test) exit 0, 14603 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 12 (clj-surgeon.parser-admission-test) exit 0, 72173 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 13 (clj-surgeon.install-test) exit 0, 86492 ms ==========

Testing clj-surgeon.install-test

Ran 899 tests containing 7984 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 86493 ms
cadence :battery: serial-equivalent 237274 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3581 ms; budget 60000 ms
bb-runtime: serial-equivalent 235276 ms; budget 343102 ms; makespan 86493 ms

namespace walls (50, slowest first, serial-equivalent total 240855 ms):
     86341 ms  clj-surgeon.install-test
     71954 ms  clj-surgeon.parser-admission-test
     14127 ms  clj-surgeon.cli-dispatch-test
     13659 ms  clj-surgeon.tmp-leak-support-test
     13394 ms  clj-surgeon.show-form-test
     11009 ms  clj-surgeon.help-test
      6150 ms  clj-surgeon.edit-test
      5579 ms  clj-surgeon.intent-transaction-test
      5356 ms  clj-surgeon.extract-test
      4775 ms  clj-surgeon.core-discovery-test
      2327 ms  clj-surgeon.lens-query-test
      1459 ms  clj-surgeon.xray-test
       919 ms  clj-surgeon.outline-test
       747 ms  clj-surgeon.operation-algebra-test
       712 ms  clj-surgeon.analyze-test
       537 ms  clj-surgeon.edn-config-integration-test
       342 ms  clj-surgeon.partition-all-test
       265 ms  clj-surgeon.platform-selector-test
       201 ms  clj-surgeon.alias-migration-test
       123 ms  clj-surgeon.ls-tree-test
       123 ms  clj-surgeon.workspace-onboarding-test
       102 ms  clj-surgeon.cljc-existing-ops-test
        88 ms  clj-surgeon.move-dependency-test
        84 ms  clj-surgeon.recovery-test
        81 ms  clj-surgeon.jvm-error-test
        55 ms  clj-surgeon.structural-lens-test
        50 ms  clj-surgeon.agent-routing-test
        48 ms  clj-surgeon.relation-census-test
        37 ms  clj-surgeon.fix-declares-test
        29 ms  clj-surgeon.owner-hypotheses-test
        27 ms  clj-surgeon.edit-dsl-test
        24 ms  clj-surgeon.outermost-test
        23 ms  clj-surgeon.worktree-lifecycle-io-test
        20 ms  clj-surgeon.memory-battery-test
        15 ms  clj-surgeon.move-test
        14 ms  clj-surgeon.worktree-lifecycle-test
        13 ms  clj-surgeon.rename-test
        10 ms  clj-surgeon.cljc.split-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.insertion-gap-test
         6 ms  clj-surgeon.forms-test
         5 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.syntax-var-refs-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         1 ms  clj-surgeon.cljc.analyze-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (14), makespan 86493 ms:
  lane 0     14113 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      7590 ms  exit 0  clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 2      1309 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.agent-routing-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 3     13663 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test
  lane 4       826 ms  exit 0  clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.fix-declares-test clj-surgeon.rename-test clj-surgeon.forms-test
  lane 5     11956 ms  exit 0  clj-surgeon.help-test clj-surgeon.edn-config-integration-test
  lane 6      2137 ms  exit 0  clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.cljc.analyze-test
  lane 7     14145 ms  exit 0  clj-surgeon.tmp-leak-support-test clj-surgeon.platform-selector-test
  lane 8       760 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  lane 9     13842 ms  exit 0  clj-surgeon.show-form-test
  lane 10       649 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.move-test clj-surgeon.syntax-var-refs-test
  lane 11     14603 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 12     72173 ms  exit 0  clj-surgeon.parser-admission-test
  lane 13     86492 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 86493 ms over 8 lane(s); serial-equivalent 240855 ms; skipped 0
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2221594-b4ecda3e node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2221594-b4ecda3e node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2221594-b4ecda3e
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2222295-109dc828 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2222295-109dc828 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2222295-109dc828
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2223117-252f90ea node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2223117-252f90ea node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/realdisk/clj-surgeon-suite-2223117-252f90ea
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/subbase/clj-surgeon-suite-2223227-6faebb94 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/subbase/clj-surgeon-suite-2223227-6faebb94 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/subbase/clj-surgeon-suite-2223227-6faebb94
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/subbase/clj-surgeon-suite-2223227-6faebb94/tmp.6NcIF0Vr8w
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/subbase/clj-surgeon-suite-2223227-6faebb94: tmp.6NcIF0Vr8w
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/stalebase/clj-surgeon-suite-2225739-3b8dd133 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/stalebase/clj-surgeon-suite-2225739-3b8dd133 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/stalebase/clj-surgeon-suite-2225739-3b8dd133
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-2217455-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y/nowrite/clj-surgeon-suite-2227013-d91dcfdb Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.ihFL5Y ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.HLPf8l/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.HLPf8l/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s........
----------------------------------------------------------------------
Ran 20 tests in 0.404s

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
Ran 3 tests in 1.304s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
gate-stage: {:target "alias-migration-test", :exit 0, :wall-ms 89472}
gate-stage: {:target "mcp-test", :exit 0, :wall-ms 89255}
gate-stage: {:target "test-bb", :exit 0, :wall-ms 86493}
gate-stage: test-bb-diagnostic started 2026-09-12T12:43:02.794199205Z
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" test/run_all.clj
SERIAL/NOT-A-GATE: direct Babashka diagnostic

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.tmp-leak-support-test
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2246186-0ee81007/tmp-leak-sweep-guard-15760850327174020830/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

Testing clj-surgeon.forms-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outline-test

Testing clj-surgeon.move-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.extract-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.show-form-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.edn-config-integration-test

Testing clj-surgeon.edit-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.parser-admission-test

Testing clj-surgeon.partition-all-test

Testing clj-surgeon.platform-selector-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.help-test

Testing clj-surgeon.install-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.worktree-lifecycle-cli-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.cli-dispatch-test

Testing clj-surgeon.core-discovery-test
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2246186-0ee81007/andon-shell-safety4723418959663311010/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2246186-0ee81007/andon-shell-safety4723418959663311010/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2246186-0ee81007/andon-shell-safety3968381214438428432/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2246186-0ee81007/andon-shell-safety3968381214438428432/PWNED-SUBST)"

Ran 830 tests containing 7283 assertions.
0 failures, 0 errors.
gate-stage: {:target "test-bb-diagnostic", :exit 0, :wall-ms 218655}
gate-stage: repository-hygiene started 2026-09-12T12:46:41.450404238Z
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
repository hygiene: no machine-local build cache is tracked at any depth
gate-stage: {:target "repository-hygiene", :exit 0, :wall-ms 2011}
gate-stage: intent-audit started 2026-09-12T12:46:43.462509935Z
{:ok true, :specs {"MCP-OP-ADMIT-135" :implemented, "MCP-OP-ADMIT-051" :implemented, "MCP-OP-HELPER-020" :active-gap, "MCP-OP-THREAD-021" :implemented, "INSERT-FORMS-020" :implemented, "MCP-OP-ALIAS-015" :implemented, "MCP-OP-THREAD-046" :implemented, "MCP-OP-ALIAS-032" :implemented, "MCP-OP-HELPER-022" :active-gap, "MCP-OP-ADMIT-020" :implemented, "WTL-APPLY-007" :implemented, "MCP-OP-CENSUS-012" :implemented, "MCP-OP-EDIT-008" :implemented, "MCP-OP-PREP-ACT-018" :active-gap, "NS-SPLIT-043" :implemented, "MCP-OP-DISPATCH-004" :implemented, "MCP-OP-EDIT-035" :implemented, "MCP-OP-EDIT-024" :implemented, "MCP-OP-ADMIT-121" :implemented, "WTL-PRUNE-005" :implemented, "PERF-SENT-IMPORT-001" :active-gap, "MCP-OP-VERIFY-010" :implemented, "MCP-OP-MEM-011" :deferred, "MCP-OP-PREP-ACT-017" :active-gap, "MCP-OP-ALIAS-002" :implemented, "OP-ALG-EFFECT-002" :implemented, "MCP-OP-HELPER-014" :active-gap, "MCP-OP-READ-MISSION-001" :deferred, "MCP-OP-READ-MISSION-002" :deferred, "MCP-OP-CENSUS-027" :implemented, "REQUIRE-CHANGE-005" :implemented, "MCP-OP-ALIAS-055" :implemented, "MCP-OP-EDIT-004" :implemented, "INSERT-FORMS-011" :implemented, "MCP-OP-ADMIT-152" :active-gap, "NS-SPLIT-057" :implemented, "MCP-OP-READ-RESOLVE-002" :deferred, "TELEMETRY-EVENTS-001" :implemented, "NS-SPLIT-060" :implemented, "MCP-OP-ADMIT-106" :implemented, "MCP-OP-ADMIT-147" :active-gap, "TEST-ISO-RACE-002" :implemented, "MCP-OP-PREP-ACT-015" :active-gap, "MCP-OP-EDIT-034" :implemented, "MCP-OP-ADMIT-069" :implemented, "INSERT-FORMS-006" :implemented, "MCP-OP-ALIAS-059" :implemented, "MCP-OP-ADMIT-150" :implemented, "OP-ALG-PREVIEW-002" :implemented, "MCP-OP-INSERT-009" :implemented, "MCP-OP-CENSUS-029" :implemented, "MCP-OP-VERIFY-009" :implemented, "MCP-OP-VERIFY-008" :implemented, "MCP-OP-ALIAS-040" :implemented, "PERF-SENT-VERDICT-002" :active-gap, "NS-SPLIT-029" :implemented, "MCP-OP-CENSUS-017" :implemented, "MCP-OP-ADMIT-021" :implemented, "MCP-OP-ADMIT-110" :implemented, "MCP-OP-FIELD-003" :implemented, "PERF-SENT-AUTH-003" :active-gap, "MCP-OP-PREP-REQ-007" :implemented, "MCP-OP-ADMIT-151" :implemented, "MCP-OP-THREAD-042" :implemented, "PERF-SENT-VERDICT-003" :active-gap, "MCP-OP-HELPER-002" :active-gap, "ALIAS-MIGRATION-005" :implemented, "PERF-SENT-PROJECT-001" :active-gap, "TEST-ISO-001a" :implemented, "MCP-OP-SHELL-ARGV-006" :implemented, "MCP-OP-ALIAS-017" :implemented, "MCP-OP-ADMIT-023" :implemented, "PERF-SENT-LEDGER-004" :active-gap, "MCP-OP-PREP-ACT-007" :active-gap, "MCP-OP-PREP-ACT-002" :active-gap, "MCP-OP-WRITE-REFUSAL-002" :deferred, "INSERT-FORMS-010" :implemented, "MCP-OP-PLAN-001" :implemented, "WTL-SEAL-006" :implemented, "PERF-SENT-RELEASE-001" :active-gap, "MCP-OP-READ-GUARD-001" :implemented, "MCP-OP-HELPER-011" :active-gap, "MCP-OP-SHELL-ARGV-007" :implemented, "WTL-PRUNE-001" :implemented, "MCP-OP-ALIAS-014" :implemented, "MCP-OP-EDIT-028" :implemented, "MCP-OP-RESULT-005" :implemented, "MCP-OP-COVERAGE-001" :implemented, "NS-SPLIT-072" :implemented, "MCP-OP-EDIT-007" :implemented, "WTL-HAND-005" :implemented, "MCP-OP-MATCHED-001" :implemented, "MCP-OP-ADMIT-107" :implemented, "TEST-ISO-002" :implemented, "MCP-OP-EDIT-012" :implemented, "MCP-OP-VERIFY-004" :implemented, "MCP-OP-POS-AUTH-003" :implemented, "MCP-OP-WRITE-REFUSAL-003" :deferred, "MCP-OP-ALIAS-051" :implemented, "MCP-OP-HELPER-018" :active-gap, "MCP-OP-ADMIT-144" :implemented, "MCP-OP-ADMIT-034" :implemented, "MCP-OP-HELPER-009" :active-gap, "NS-SPLIT-001" :implemented, "PERF-SENT-TIME-001" :active-gap, "ALIAS-MIGRATION-003" :implemented, "MCP-OP-ALIAS-046" :implemented, "MCP-OP-ADMIT-099" :implemented, "MCP-OP-THREAD-029" :implemented, "MEASURE-EVID-001" :active-gap, "MCP-OP-ADMIT-111" :implemented, "MCP-OP-ALIAS-065" :active-gap, "MCP-OP-EDIT-018" :implemented, "MCP-OP-MATCHED-004" :implemented, "MCP-OP-ALIAS-047" :implemented, "MCP-OP-PREP-ACT-001" :active-gap, "MCP-OP-THREAD-039" :implemented, "MCP-OP-ADMIT-032" :implemented, "MCP-OP-TMPHYG-012" :implemented, "MCP-OP-HELPER-019" :active-gap, "MCP-OP-PLAN-004" :implemented, "NS-SPLIT-008" :implemented, "NS-SPLIT-046" :implemented, "NS-SPLIT-042" :deferred, "MCP-OP-TRACE-002" :implemented, "OP-ALG-VERIFY-004" :deferred, "REQUIRE-CHANGE-004" :implemented, "MCP-OP-ALIAS-019" :implemented, "WTL-APPLY-002" :implemented, "NS-SPLIT-056" :implemented, "MCP-OP-EDIT-027" :implemented, "MCP-OP-INSERT-003" :active-gap, "NS-SPLIT-071" :implemented, "MCP-OP-HOTVER-002" :implemented, "MCP-OP-ALIAS-036" :implemented, "REQUIRE-CHANGE-002" :implemented, "MCP-OP-CENSUS-035" :implemented, "OP-ALG-EFFECT-005" :implemented, "MCP-OP-ADMIT-065" :implemented, "MCP-OP-ANALYZER-008" :implemented, "MCP-OP-MATCH-005" :deferred, "PERF-SENT-SCHEDULE-002" :active-gap, "MCP-OP-CENSUS-005" :implemented, "MCP-OP-ADMIT-066" :implemented, "RENAME-ALIAS-001" :implemented, "MCP-OP-ALIAS-016" :implemented, "PERF-SENT-CONFIG-001" :active-gap, "MCP-OP-ANALYZER-009" :implemented, "MCP-OP-CENSUS-034" :implemented, "MCP-OP-ASYNC-002" :implemented, "MCP-OP-PREP-REQ-005" :implemented, "OP-ALG-COMMIT-003" :implemented, "MCP-OP-PREP-ACT-014" :active-gap, "RENAME-ALIAS-003" :implemented, "MCP-OP-ADMIT-141" :implemented, "TEST-ISO-014" :implemented, "MCP-OP-HELPER-021" :active-gap, "WTL-CLI-003" :implemented, "MCP-OP-ADMIT-148" :implemented, "MCP-OP-ALIAS-063" :active-gap, "MCP-OP-ANALYZER-006" :active-gap, "INSERT-FORMS-012" :implemented, "MCP-OP-EDIT-001" :implemented, "MCP-OP-EDIT-033" :implemented, "NS-SPLIT-052" :implemented, "OP-ALG-RUNTIME-001" :implemented, "NS-SPLIT-062" :implemented, "MCP-OP-ADMIT-061" :implemented, "MCP-OP-EDIT-032" :implemented, "NS-SPLIT-035" :implemented, "MCP-OP-ALIAS-044" :implemented, "MCP-OP-SHELL-ARGV-002" :implemented, "WTL-HAND-002" :implemented, "MCP-OP-VERIFY-007" :implemented, "MCP-OP-MATCHED-005" :implemented, "MCP-OP-THREAD-006" :implemented, "WTL-SEAL-001" :implemented, "MCP-OP-PREP-ACT-005" :active-gap, "RENAME-ALIAS-014" :implemented, "WTL-INV-001" :implemented, "MCP-OP-VERIFY-003" :implemented, "MCP-OP-ADMIT-105" :implemented, "MCP-OP-ADMIT-088" :implemented, "MCP-OP-ADMIT-030" :implemented, "PERF-SENT-CUTOVER-002" :active-gap, "MCP-OP-SCHEMA-001" :implemented, "INSERT-FORMS-008" :implemented, "MCP-OP-ADMIT-149" :implemented, "RENAME-ALIAS-007" :implemented, "RENAME-ALIAS-010" :implemented, "MCP-OP-VERIFY-005" :implemented, "MCP-OP-ADMIT-139" :implemented, "MCP-OP-THREAD-018" :implemented, "MCP-OP-VERIFY-006" :implemented, "MCP-OP-READ-HYP-002" :implemented, "MCP-OP-THREAD-012" :implemented, "OP-ALG-STALE-001" :implemented, "MCP-OP-MEM-020" :implemented, "MCP-OP-TRACE-006" :implemented, "INSERT-FORMS-017" :implemented, "MCP-OP-CENSUS-010" :implemented, "MCP-OP-ALIAS-054" :implemented, "MCP-OP-READ-NORM-001" :active-gap, "MCP-OP-EDIT-009" :implemented, "NS-SPLIT-015" :implemented, "TEST-ISO-001" :implemented, "MCP-OP-READ-RESOLVE-001" :deferred, "MCP-OP-ALIAS-057" :implemented, "MCP-OP-ALIAS-009" :implemented, "WTL-INV-002" :implemented, "MCP-OP-EDIT-015" :implemented, "MCP-OP-DISPATCH-002" :implemented, "MCP-OP-WRITE-REFUSAL-006" :deferred, "MCP-OP-TIME-003" :implemented, "MCP-OP-CENSUS-028" :implemented, "PERF-SENT-AUTH-001" :active-gap, "NS-SPLIT-041" :implemented, "MCP-OP-POS-AUTH-007" :implemented, "MCP-OP-MATCHED-002" :implemented, "MCP-OP-ANALYZER-002" :implemented, "TEST-ISO-010" :implemented, "WTL-PLAN-004" :implemented, "MCP-OP-MEM-007" :implemented, "MCP-OP-ALIAS-013" :implemented, "WTL-PRUNE-008" :implemented, "PERF-SENT-LEDGER-003" :active-gap, "MCP-OP-DISPATCH-003" :implemented, "MCP-OP-TMPHYG-009" :implemented, "WTL-PRUNE-002" :implemented, "NS-SPLIT-064" :implemented, "MCP-OP-EDIT-037" :implemented, "MCP-OP-HOTVER-001" :implemented, "MCP-OP-POS-AUTH-004" :implemented, "MCP-OP-ADMIT-100" :implemented, "NS-SPLIT-049" :implemented, "MCP-OP-TMPHYG-011" :implemented, "MCP-OP-ADMIT-120" :implemented, "MCP-OP-ADMIT-060" :implemented, "WTL-HAND-003" :implemented, "MCP-OP-THREAD-001" :implemented, "MCP-OP-EDIT-023" :implemented, "MCP-OP-CENSUS-030" :implemented, "MCP-OP-RESULT-003" :implemented, "INSERT-FORMS-019" :implemented, "MCP-OP-ADMIT-033" :implemented, "WTL-APPLY-004" :implemented, "MCP-OP-THREAD-008" :implemented, "MCP-OP-ADMIT-112" :implemented, "MCP-OP-THREAD-015" :implemented, "MCP-OP-EDIT-016" :implemented, "TEST-ISO-009" :implemented, "MCP-OP-ALIAS-028" :implemented, "WTL-PRUNE-010" :implemented, "MCP-OP-ALIAS-039" :implemented, "RECEIPT-BOOL-001" :implemented, "MCP-OP-ADMIT-081" :implemented, "MCP-OP-CENSUS-003" :implemented, "MCP-OP-CENSUS-019" :implemented, "MCP-OP-ALIAS-067" :implemented, "MCP-OP-CENSUS-020" :implemented, "RENAME-ALIAS-008" :implemented, "WTL-SEAL-008" :deferred, "MCP-OP-ADMIT-062" :implemented, "REQUIRE-CHANGE-008" :implemented, "MCP-OP-FIELD-002" :implemented, "MCP-OP-READ-NORM-004" :active-gap, "MCP-OP-THREAD-041" :implemented, "MCP-OP-THREAD-011" :implemented, "MCP-OP-TMPHYG-003" :implemented, "MCP-OP-MEM-015" :implemented, "MCP-OP-THREAD-051" :implemented, "OP-ALG-COMPILE-001" :implemented, "MCP-OP-ALIAS-030" :implemented, "BB-PROBE-001" :implemented, "OP-ALG-EXPAND-001" :deferred, "MCP-OP-DISPATCH-005" :implemented, "MCP-OP-POS-AUTH-010" :implemented, "MCP-OP-THREAD-038" :implemented, "MCP-OP-EDIT-011" :implemented, "MCP-OP-ADMIT-004" :implemented, "NS-SPLIT-013" :implemented, "MCP-OP-ASYNC-001" :implemented, "MCP-OP-ADMIT-070" :implemented, "MCP-OP-ALIAS-022" :implemented, "MCP-OP-ADMIT-142" :implemented, "TEST-ISO-009b" :implemented, "MCP-OP-PREP-ACT-013" :active-gap, "MCP-OP-ALIAS-005" :implemented, "NS-SPLIT-005" :implemented, "NS-SPLIT-004" :implemented, "MCP-OP-HELPER-007" :active-gap, "MCP-OP-HELPER-005" :active-gap, "NS-SPLIT-055" :implemented, "MCP-OP-READ-CONT-001" :implemented, "MCP-OP-ALIAS-023" :implemented, "MCP-OP-THREAD-035" :implemented, "PERF-SENT-SCHEDULE-003" :active-gap, "MCP-OP-INSERT-010" :implemented, "MCP-OP-ALIAS-038" :implemented, "RENAME-ALIAS-012" :implemented, "INSERT-FORMS-018" :implemented, "MCP-OP-ADMIT-103" :implemented, "OP-ALG-EFFECT-004" :implemented, "MCP-OP-CENSUS-006" :implemented, "INSERT-FORMS-016" :implemented, "MCP-OP-THREAD-014" :implemented, "SPLIT-REPAIR-002" :implemented, "MCP-OP-HELPER-013" :active-gap, "MCP-OP-DISPATCH-001" :implemented, "INSERT-FORMS-023" :implemented, "MCP-OP-ADMIT-125" :implemented, "OP-ALG-DECODE-001" :implemented, "INSERT-FORMS-021" :implemented, "MCP-OP-ANALYZER-004" :implemented, "MCP-OP-THREAD-023" :implemented, "NS-SPLIT-036" :implemented, "OP-ALG-COMMIT-001" :implemented, "MCP-OP-ADMIT-098" :implemented, "MCP-OP-ALIAS-050" :implemented, "MCP-OP-ANALYZER-003" :implemented, "PERF-SENT-BASELINE-001" :active-gap, "MCP-OP-ALIAS-026" :implemented, "MCP-OP-PREP-ACT-003" :active-gap, "TEST-ISO-015" :implemented, "WTL-APPLY-009" :implemented, "WTL-PRUNE-004" :implemented, "MCP-OP-ALIAS-061" :implemented, "MCP-OP-FIELD-008" :implemented, "PERF-SENT-AUTH-002" :active-gap, "MCP-OP-THREAD-031" :implemented, "MCP-OP-THREAD-019" :implemented, "MCP-OP-ADMIT-096" :implemented, "MCP-OP-RELAY-002" :implemented, "WTL-HAND-001" :implemented, "MCP-OP-CENSUS-018" :implemented, "PERF-SENT-CUTOVER-003" :active-gap, "MCP-OP-WRITE-REFUSAL-001" :implemented, "NS-SPLIT-065" :implemented, "MCP-OP-HELPER-004" :active-gap, "OP-ALG-OUTCOME-003" :implemented, "TEST-ISO-016" :implemented, "MCP-OP-ALIAS-007" :implemented, "OP-ALG-CLI-001" :implemented, "MCP-OP-THREAD-032" :implemented, "MCP-OP-ADMIT-119" :implemented, "MCP-OP-TMPHYG-006" :implemented, "MCP-OP-HELPER-016" :active-gap, "MCP-OP-ALIAS-060" :implemented, "TEST-ISO-001c" :implemented, "MCP-OP-PLAN-002" :implemented, "WTL-INV-007" :implemented, "MCP-OP-ADMIT-024" :implemented, "NS-SPLIT-034" :implemented, "MCP-OP-WRITE-REFUSAL-008" :deferred, "PERF-SENT-RETAIN-001" :active-gap, "MCP-OP-TMPHYG-004" :implemented, "MCP-OP-THREAD-045" :implemented, "NS-SPLIT-045" :implemented, "REQUIRE-CHANGE-011" :implemented, "MCP-OP-THREAD-040" :implemented, "MCP-OP-RELAY-001" :implemented, "MCP-OP-CENSUS-004" :implemented, "MCP-OP-SHELL-ARGV-005" :implemented, "MCP-OP-CENSUS-023" :implemented, "PERF-SENT-CLIENT-001" :active-gap, "MCP-OP-ADMIT-064" :implemented, "NS-SPLIT-033" :implemented, "MCP-OP-READ-DIAG-001" :implemented, "INSERT-FORMS-015" :implemented, "NS-SPLIT-038" :implemented, "MCP-OP-THREAD-020" :implemented, "MCP-OP-WRITE-REFUSAL-004" :deferred, "MCP-OP-TMPHYG-001" :implemented, "MCP-OP-READ-PARITY-001" :implemented, "MCP-OP-ADMIT-012" :implemented, "ROUTING-FANOUT-001" :implemented, "MCP-OP-EDIT-038" :implemented, "INSERT-FORMS-013" :implemented, "RENAME-ALIAS-004" :implemented, "MCP-OP-HELPER-012" :active-gap, "NS-SPLIT-059" :implemented, "MCP-OP-ANALYZER-007" :implemented, "MCP-OP-PREP-REQ-004" :implemented, "MCP-OP-CENSUS-021" :implemented, "MCP-OP-THREAD-033" :implemented, "WTL-SEAL-004" :implemented, "PERF-SENT-RECEIPT-001" :active-gap, "RENAME-ALIAS-009" :implemented, "MCP-OP-ALIAS-024" :implemented, "MCP-OP-INSERT-001" :active-gap, "MCP-OP-POS-AUTH-001" :implemented, "MCP-OP-VERIFY-001" :implemented, "MCP-OP-ALIAS-041" :implemented, "NS-SPLIT-002" :implemented, "PERF-SENT-INVALID-001" :active-gap, "MCP-OP-TRACE-001" :implemented, "REQUIRE-CHANGE-009" :implemented, "MCP-OP-ALIAS-025" :implemented, "MCP-OP-ADMIT-094" :implemented, "RENAME-ALIAS-011" :implemented, "MCP-OP-THREAD-050" :implemented, "INSERT-FORMS-005" :implemented, "OP-ALG-OUTCOME-002" :implemented, "REQUIRE-CHANGE-003" :implemented, "MCP-OP-ADMIT-063" :implemented, "WTL-APPLY-010" :implemented, "MCP-OP-ALIAS-029" :implemented, "MCP-OP-THREAD-052" :implemented, "MCP-OP-MEM-001" :deferred, "MCP-OP-ALIAS-018" :implemented, "PERF-SENT-RECOVERY-002" :active-gap, "MCP-OP-ADMIT-050" :implemented, "MCP-OP-ADMIT-137" :implemented, "MCP-OP-ADMIT-053" :implemented, "MCP-OP-EDIT-003" :implemented, "OP-ALG-REFUSE-001" :implemented, "MCP-OP-ADMIT-132" :implemented, "OP-ALG-CLI-002" :deferred, "PERF-SENT-IDENT-001" :active-gap, "PERF-SENT-SCHEDULE-001" :active-gap, "MCP-OP-ADMIT-067" :implemented, "MCP-OP-ADMIT-090" :implemented, "MCP-OP-CENSUS-022" :implemented, "INSERT-FORMS-022" :implemented, "MCP-OP-TMPHYG-005" :implemented, "MCP-OP-HELPER-003" :active-gap, "MCP-OP-SHELL-ARGV-003" :implemented, "OP-ALG-EFFECT-001" :implemented, "INSERT-FORMS-001" :implemented, "INSERT-FORMS-007" :implemented, "MCP-OP-ALIAS-001" :implemented, "MCP-OP-ALIAS-006" :implemented, "MCP-OP-CENSUS-013" :implemented, "NS-SPLIT-007" :implemented, "MCP-OP-THREAD-027" :implemented, "MCP-OP-ADMIT-113" :implemented, "SPLIT-REPAIR-001" :implemented, "MCP-OP-THREAD-007" :implemented, "MCP-OP-ADMIT-116" :implemented, "MCP-OP-PREP-ACT-004" :active-gap, "MCP-OP-PLAN-009" :implemented, "MCP-OP-ALIAS-020" :implemented, "MCP-OP-THREAD-048" :implemented, "MEASURE-WALL-002" :active-gap, "MCP-OP-ADMIT-005" :implemented, "MCP-OP-PREP-REQ-003" :implemented, "MCP-OP-READ-AUTH-001" :deferred, "PERF-SENT-PROMOTION-002" :active-gap, "MCP-OP-SHELL-ARGV-001" :implemented, "MCP-OP-TMPHYG-008" :implemented, "MCP-OP-PREP-REQ-006" :implemented, "REQUIRE-CHANGE-010" :implemented, "MCP-OP-ADMIT-109" :implemented, "TEST-ISO-008" :active-gap, "PERF-SENT-SCHEDULE-004" :active-gap, "MCP-OP-EDIT-022" :implemented, "MCP-OP-ADMIT-127" :implemented, "WTL-SEAL-005" :implemented, "MCP-OP-TMPHYG-007" :implemented, "MCP-OP-ADMIT-082" :implemented, "WTL-HAND-004" :implemented, "MCP-OP-RELAY-003" :implemented, "NS-SPLIT-067" :implemented, "MCP-OP-WRITE-REFUSAL-007" :deferred, "OP-ALG-COMMIT-004" :implemented, "NS-SPLIT-032" :implemented, "MCP-OP-EDIT-041" :implemented, "INSERT-FORMS-004" :implemented, "MCP-OP-THREAD-013" :implemented, "RENAME-ALIAS-005" :implemented, "PERF-SENT-IMPORT-002" :active-gap, "MCP-OP-EDIT-040" :implemented, "MCP-OP-ALIAS-033" :implemented, "MCP-OP-ADMIT-003" :implemented, "NS-SPLIT-048" :implemented, "PERF-SENT-BACKFILL-001" :active-gap, "MCP-OP-POS-AUTH-002" :implemented, "ALIAS-MIGRATION-004" :implemented, "MCP-OP-MEM-006" :implemented, "MCP-OP-ADMIT-089" :implemented, "MCP-OP-ADMIT-136" :implemented, "MCP-OP-INSERT-002" :active-gap, "PERF-SENT-LEDGER-002" :active-gap, "TEST-ISO-011" :active-gap, "MCP-OP-FIELD-001" :implemented, "MCP-OP-POS-AUTH-005" :implemented, "OP-ALG-FORM-COUNT-001" :implemented, "MCP-OP-MATCH-004" :implemented, "MCP-OP-ADMIT-071" :implemented, "OP-ALG-VERIFY-001" :deferred, "MCP-OP-THREAD-026" :implemented, "MCP-OP-ALIAS-053" :implemented, "TEST-ISO-009a" :implemented, "ALIAS-MIGRATION-001" :implemented, "INSERT-FORMS-003" :implemented, "MCP-OP-EDIT-006" :implemented, "PERF-SENT-PUBLISH-002" :active-gap, "PERF-SENT-LEDGER-ROOT-001" :active-gap, "MCP-OP-CENSUS-009" :implemented, "MCP-OP-PLAN-003" :implemented, "MCP-OP-INSERT-005" :active-gap, "MCP-OP-INSERT-004" :active-gap, "MCP-OP-ADMIT-143" :implemented, "MCP-OP-EDIT-030" :implemented, "MCP-OP-ALIAS-062" :active-gap, "OP-ALG-CONTEXT-002" :implemented, "MCP-OP-TMPHYG-010" :implemented, "MCP-OP-THREAD-034" :implemented, "MCP-OP-READ-NORM-005" :active-gap, "MCP-OP-CENSUS-016" :implemented, "MCP-OP-EDIT-013" :implemented, "WTL-INV-006" :implemented, "MCP-OP-ADMIT-084" :implemented, "WTL-APPLY-003" :implemented, "INSERT-FORMS-014" :implemented, "MCP-OP-THREAD-009" :implemented, "PERF-SENT-PROMOTION-001" :active-gap, "NS-SPLIT-053" :implemented, "MCP-OP-INSERT-007" :implemented, "NS-SPLIT-044" :implemented, "NS-SPLIT-063" :implemented, "MCP-OP-MATCHED-003" :implemented, "MCP-OP-MEM-012" :implemented, "MCP-OP-ASYNC-005" :implemented, "REQUIRE-CHANGE-013" :implemented, "MCP-OP-ADMIT-130" :implemented, "MCP-OP-ADMIT-128" :implemented, "BB-PROBE-002" :implemented, "REQUIRE-CHANGE-001" :implemented, "MCP-OP-EDIT-025" :implemented, "MCP-OP-ADMIT-091" :implemented, "MCP-OP-THREAD-037" :implemented, "MCP-OP-HELPER-023" :active-gap, "NS-SPLIT-047" :implemented, "NS-SPLIT-051" :implemented, "MCP-OP-ADMIT-145" :implemented, "MCP-OP-HELPER-010" :active-gap, "MCP-OP-READ-NORM-003" :active-gap, "PERF-SENT-SURFACE-001" :active-gap, "MCP-OP-THREAD-044" :implemented, "MCP-OP-ALIAS-037" :implemented, "MCP-OP-HELPER-001" :active-gap, "MCP-OP-ADMIT-108" :implemented, "PERF-SENT-ATTEMPT-003" :active-gap, "MCP-OP-READ-HYP-001" :implemented, "PERF-SENT-ATTEMPT-001" :active-gap, "OP-ALG-RECEIPT-002" :implemented, "OP-ALG-EFFECT-003" :implemented, "NS-SPLIT-069" :implemented, "MCP-OP-TRACE-003" :implemented, "MCP-OP-VERIFY-013" :implemented, "MCP-OP-ADMIT-022" :implemented, "MCP-OP-ADMIT-118" :implemented, "MCP-OP-ADMIT-002" :implemented, "MCP-OP-RESULT-006" :implemented, "OP-ALG-VERIFY-002" :deferred, "MCP-OP-POS-AUTH-009" :implemented, "OP-ALG-SHADOW-001" :implemented, "MCP-OP-ADMIT-133" :implemented, "MCP-OP-ALIAS-021" :implemented, "MCP-OP-TMPHYG-013" :implemented, "MCP-OP-PREP-REQ-008" :implemented, "OP-ALG-CONTEXT-001" :implemented, "NS-SPLIT-012" :implemented, "MCP-OP-CENSUS-008" :implemented, "MCP-OP-THREAD-047" :implemented, "MCP-OP-CENSUS-001" :implemented, "MCP-OP-EDIT-019" :implemented, "MCP-OP-CENSUS-031" :implemented, "MCP-OP-ALIAS-049" :implemented, "NS-SPLIT-061" :implemented, "SPLIT-REPAIR-004" :implemented, "MCP-OP-PREP-ACT-010" :active-gap, "OP-ALG-PARITY-001" :implemented, "WTL-APPLY-008" :implemented, "MCP-OP-CENSUS-033" :implemented, "WTL-INV-004" :implemented, "INSERT-FORMS-009" :implemented, "PERF-SENT-RECEIPT-002" :active-gap, "WTL-PLAN-003" :implemented, "NS-SPLIT-054" :implemented, "PERF-SENT-ATTEMPT-002" :active-gap, "REQUIRE-CHANGE-012" :implemented, "MCP-OP-ALIAS-004" :implemented, "MCP-OP-ADMIT-080" :implemented, "MCP-OP-HELPER-006" :active-gap, "MCP-OP-ALIAS-003" :implemented, "MCP-OP-EDIT-039" :implemented, "TEST-ISO-003" :implemented, "MCP-OP-ALIAS-035" :implemented, "MCP-OP-THREAD-028" :implemented, "MCP-OP-TRACE-005" :implemented, "MCP-OP-THREAD-036" :implemented, "RENAME-ALIAS-006" :implemented, "MCP-OP-ADMIT-040" :implemented, "MCP-OP-ADMIT-092" :implemented, "REQUIRE-CHANGE-007" :implemented, "WTL-SEAL-002" :implemented, "MCP-OP-EDIT-014" :implemented, "SPLIT-REPAIR-003" :implemented, "MCP-OP-ADMIT-097" :implemented, "MCP-OP-ADMIT-055" :implemented, "MCP-OP-EDIT-002" :implemented, "MCP-OP-PREP-ACT-009" :active-gap, "MCP-OP-ADMIT-001" :implemented, "NS-SPLIT-040" :implemented, "MCP-OP-PLAN-005" :implemented, "NS-SPLIT-058" :implemented, "MCP-OP-COVERAGE-002" :implemented, "MCP-OP-ADMIT-126" :implemented, "OP-ALG-CATALOG-001" :implemented, "MCP-OP-ADMIT-146" :implemented, "PERF-SENT-IDENT-002" :active-gap, "MCP-OP-CENSUS-024" :implemented, "MCP-OP-ADMIT-104" :implemented, "MCP-OP-MATCH-002" :implemented, "MCP-OP-READ-CONT-002" :implemented, "MCP-OP-ADMIT-044" :implemented, "MCP-OP-ALIAS-058" :implemented, "WTL-APPLY-001" :implemented, "MCP-OP-ADMIT-122" :implemented, "MCP-OP-CENSUS-011" :implemented, "NS-SPLIT-070" :implemented, "RENAME-ALIAS-015" :implemented, "MCP-OP-ADMIT-052" :implemented, "WTL-PRUNE-007" :implemented, "MCP-OP-ALIAS-048" :implemented, "NS-SPLIT-039" :implemented, "MCP-OP-CENSUS-002" :implemented, "MCP-OP-ADMIT-101" :implemented, "MCP-OP-EDIT-021" :implemented, "MCP-OP-FIELD-009" :implemented, "MCP-OP-THREAD-024" :implemented, "PERF-SENT-VERDICT-001" :active-gap, "NS-SPLIT-066" :implemented, "MCP-OP-CENSUS-014" :implemented, "MCP-OP-HELPER-015" :active-gap, "MCP-OP-PREP-ACT-006" :active-gap, "MCP-OP-MEM-013" :implemented, "WTL-APPLY-011" :implemented, "TEST-ISO-004" :implemented, "WTL-CLI-001" :implemented, "MCP-OP-EDIT-036" :implemented, "RENAME-ALIAS-013" :implemented, "MCP-OP-ADMIT-115" :implemented, "WTL-PRUNE-009" :implemented, "OP-ALG-OUTCOME-001" :implemented, "MCP-OP-RELAY-005" :implemented, "MCP-OP-VERIFY-012" :implemented, "MCP-OP-PREP-ACT-012" :active-gap, "PERF-SENT-ADMIT-001" :active-gap, "MCP-OP-TRACE-004" :implemented, "MCP-OP-WRITE-REFUSAL-005" :deferred, "MCP-OP-THREAD-022" :implemented, "MCP-OP-FIELD-005" :implemented, "MCP-OP-ADMIT-123" :implemented, "MCP-OP-READ-RETRY-002" :deferred, "TEST-ISO-012" :active-gap, "NS-SPLIT-006" :implemented, "MCP-OP-TIME-001" :implemented, "MEASURE-WALL-001" :active-gap, "NS-SPLIT-028" :implemented, "MCP-OP-CENSUS-015" :implemented, "MCP-OP-EDIT-020" :implemented, "NS-SPLIT-068" :implemented, "MCP-OP-CENSUS-025" :implemented, "MCP-OP-ASYNC-004" :implemented, "MCP-OP-THREAD-004" :implemented, "WTL-INV-005" :implemented, "MCP-OP-HELPER-008" :active-gap, "OP-ALG-PARITY-002" :implemented, "INSERT-FORMS-002" :implemented, "MCP-OP-ALIAS-043" :implemented, "MCP-OP-ALIAS-052" :implemented, "MCP-OP-INSERT-008" :implemented, "MCP-OP-THREAD-002" :implemented, "MCP-OP-EDIT-017" :implemented, "NS-SPLIT-030" :implemented, "ROUTING-PARITY-001" :implemented, "WTL-SEAL-007" :implemented, "WTL-PLAN-006" :implemented, "MCP-OP-ALIAS-008" :implemented, "RENAME-ALIAS-002" :implemented, "WTL-INV-003" :implemented, "MCP-OP-ALIAS-012" :implemented, "MCP-OP-RESULT-001" :implemented, "MCP-OP-VERIFY-011" :implemented, "MCP-OP-POS-AUTH-008" :implemented, "WTL-CLI-002" :implemented, "NS-SPLIT-003" :implemented, "MCP-OP-ALIAS-034" :implemented, "MCP-OP-THREAD-003" :implemented, "MCP-OP-RESULT-004" :implemented, "MCP-OP-TIME-002" :implemented, "MCP-OP-ADMIT-087" :implemented, "MCP-OP-MATCH-001" :implemented, "MCP-OP-EDIT-010" :implemented, "MCP-OP-ADMIT-011" :implemented, "MCP-OP-ADMIT-083" :implemented, "MCP-OP-HELPER-024" :active-gap, "PERF-SENT-PUBLISH-001" :active-gap, "OP-ALG-IDENTITY-001" :implemented, "MCP-OP-PLAN-010" :implemented, "PERF-SENT-CUTOVER-001" :active-gap, "MCP-OP-HELPER-017" :active-gap, "MCP-OP-MATCH-003" :implemented, "WTL-SEAL-003" :implemented, "MCP-OP-PREP-REQ-002" :implemented, "TEST-ISO-006" :implemented, "MCP-OP-CENSUS-026" :implemented, "MCP-OP-READ-RETRY-001" :deferred, "WTL-INV-008" :implemented, "MCP-OP-ANALYZER-001" :implemented, "WTL-PRUNE-003" :implemented, "WTL-APPLY-005" :implemented, "MCP-OP-RELAY-004" :implemented, "NS-SPLIT-009" :implemented, "NS-SPLIT-010" :implemented, "MCP-OP-ADMIT-117" :implemented, "OP-ALG-PERF-002" :implemented, "TEST-ISO-013" :implemented, "MCP-OP-THREAD-016" :implemented, "MCP-OP-ALIAS-027" :implemented, "PERF-SENT-RECOVERY-001" :active-gap, "REQUIRE-CHANGE-006" :implemented, "MCP-OP-ADMIT-095" :implemented, "MCP-OP-ADMIT-010" :implemented, "MCP-OP-FIELD-006" :implemented, "ALIAS-MIGRATION-002" :implemented, "RENAME-ALIAS-016" :implemented, "MCP-OP-ADMIT-042" :implemented, "MCP-OP-ALIAS-045" :implemented, "MCP-OP-THREAD-025" :implemented, "MCP-OP-ADMIT-093" :implemented, "MCP-OP-ALIAS-056" :implemented, "MCP-OP-TMPHYG-002" :implemented, "MCP-OP-ADMIT-131" :implemented, "MCP-OP-VERIFY-002" :implemented, "WTL-APPLY-006" :implemented, "MCP-OP-MEM-014" :implemented, "MCP-OP-RESULT-002" :implemented, "MCP-OP-EDIT-029" :implemented, "WTL-CLI-004" :deferred, "MCP-OP-PLAN-008" :implemented, "MCP-OP-ADMIT-031" :implemented, "MCP-OP-ADMIT-129" :implemented, "OP-ALG-RECEIPT-003" :implemented, "MCP-OP-ADMIT-041" :implemented, "MCP-OP-CENSUS-007" :implemented, "MCP-OP-PREP-ACT-016" :active-gap, "MCP-OP-ADMIT-124" :implemented, "WTL-PLAN-005" :implemented, "MCP-OP-FIELD-007" :implemented, "TEST-ISO-007" :implemented, "MCP-OP-MEM-005" :implemented, "MCP-OP-ASYNC-003" :implemented, "MCP-OP-ADMIT-138" :implemented, "MCP-OP-INSERT-006" :active-gap, "PERF-SENT-LEDGER-001" :active-gap, "MCP-OP-ALIAS-010" :implemented, "MCP-OP-ADMIT-140" :implemented, "MCP-OP-ADMIT-043" :implemented, "OP-ALG-RECEIPT-001" :implemented, "PERF-SENT-RECONCILE-001" :active-gap, "MCP-OP-ALIAS-066" :active-gap, "PERF-SENT-RECOVERY-004" :active-gap, "MCP-OP-THREAD-010" :implemented, "PERF-SENT-RECOVERY-003" :active-gap, "PERF-SENT-THRESHOLD-001" :active-gap, "MCP-OP-ADMIT-085" :implemented, "MCP-OP-THREAD-017" :implemented, "WTL-PLAN-001" :implemented, "MCP-OP-CENSUS-032" :implemented, "NS-SPLIT-050" :implemented, "MCP-OP-ANALYZER-005" :implemented, "OP-ALG-PREVIEW-001" :implemented, "WTL-PLAN-002" :implemented, "MCP-OP-READ-DIAG-002" :implemented, "MCP-OP-ADMIT-086" :implemented, "MCP-OP-HELPER-025" :active-gap, "MCP-OP-ADMIT-134" :implemented, "OP-ALG-PERF-001" :implemented, "MCP-OP-ALIAS-031" :implemented, "OP-ALG-MCP-001" :implemented, "TEST-ISO-RACE-001" :implemented, "MEASURE-WALL-003" :active-gap, "MCP-OP-READ-DIAG-003" :implemented, "MCP-OP-EDIT-005" :implemented, "MCP-OP-ADMIT-014" :deferred, "MCP-OP-ADMIT-102" :implemented, "TEST-ISO-001b" :implemented, "MCP-OP-PLAN-006" :implemented, "MCP-OP-THREAD-043" :implemented, "PERF-SENT-INVALID-002" :active-gap, "REQUIRE-CHANGE-014" :implemented, "INSERT-FORMS-024" :implemented, "WTL-PRUNE-006" :implemented, "PERF-SENT-ADMIT-002" :active-gap, "MCP-OP-PLAN-007" :implemented, "BB-PROBE-003" :implemented, "MCP-OP-THREAD-049" :implemented, "MCP-OP-ALIAS-042" :implemented, "MCP-OP-THREAD-005" :implemented, "OP-ALG-COMMIT-002" :implemented, "MCP-OP-ADMIT-114" :implemented, "TEST-ISO-005" :implemented, "MCP-OP-ADMIT-013" :implemented, "MCP-OP-TIME-004" :implemented, "MCP-OP-THREAD-030" :implemented, "MCP-OP-ALIAS-011" :implemented, "MCP-OP-PREP-ACT-011" :active-gap, "MCP-OP-POS-AUTH-006" :implemented, "MCP-OP-PREP-REQ-001" :implemented, "MCP-OP-ALIAS-064" :active-gap, "MCP-OP-ADMIT-054" :implemented, "NS-SPLIT-011" :implemented, "MCP-OP-READ-NORM-002" :active-gap, "MCP-OP-EDIT-042" :implemented, "MCP-OP-PREP-ACT-008" :active-gap, "MCP-OP-ORACLE-001" :implemented, "WTL-APPLY-012" :implemented, "MCP-OP-EDIT-031" :implemented, "NS-SPLIT-014" :implemented, "MCP-OP-EDIT-026" :implemented, "MCP-OP-PREP-REQ-009" :implemented, "ROUTING-SPLIT-001" :implemented, "MCP-OP-SHELL-ARGV-004" :implemented, "MCP-OP-ADMIT-068" :implemented, "NS-SPLIT-037" :implemented}, :implementation-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "OP-ALG-RUNTIME-001" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "TEST-ISO-009" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "OP-ALG-OUTCOME-002" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "OP-ALG-CONTEXT-002" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "MCP-OP-ADMIT-084" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "MCP-OP-PREP-REQ-002" "MCP-OP-CENSUS-026" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "OP-ALG-RECEIPT-003" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "OP-ALG-MCP-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "OP-ALG-COMMIT-002" "MCP-OP-ADMIT-114" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "MCP-OP-PREP-REQ-001" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :test-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "WTL-APPLY-007" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "OP-ALG-EFFECT-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "MCP-OP-ADMIT-152" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-ADMIT-147" "TEST-ISO-RACE-002" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "WTL-SEAL-006" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "WTL-HAND-005" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "WTL-APPLY-002" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "WTL-CLI-003" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "MCP-OP-ANALYZER-006" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "WTL-HAND-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "OP-ALG-STALE-001" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "WTL-INV-002" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "TEST-ISO-010" "WTL-PLAN-004" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "WTL-APPLY-004" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "WTL-SEAL-004" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "WTL-APPLY-010" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "WTL-INV-006" "MCP-OP-ADMIT-084" "WTL-APPLY-003" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "MCP-OP-HELPER-023" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "OP-ALG-EFFECT-003" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "OP-ALG-SHADOW-001" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "WTL-APPLY-008" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "WTL-PLAN-003" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "TEST-ISO-003" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "WTL-SEAL-002" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-APPLY-011" "TEST-ISO-004" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "OP-ALG-OUTCOME-001" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "WTL-INV-005" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "WTL-SEAL-007" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "WTL-INV-003" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "WTL-CLI-002" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "WTL-SEAL-003" "MCP-OP-PREP-REQ-002" "TEST-ISO-006" "MCP-OP-CENSUS-026" "WTL-INV-008" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "WTL-APPLY-006" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "TEST-ISO-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "TEST-ISO-RACE-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "MCP-OP-ADMIT-114" "TEST-ISO-005" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "MCP-OP-PREP-REQ-001" "MCP-OP-ALIAS-064" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "WTL-APPLY-012" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :violations [], :pending-witness-violations [{:type :missing-test-witness, :intent "MEASURE-EVID-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-002", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-003", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-COMMIT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-CONTEXT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-DECODE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-DECODE-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-EFFECT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-MCP-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-REFUSE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-REFUSE-001", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RUNTIME-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-SHADOW-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-STALE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BACKFILL-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BASELINE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CLIENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CONFIG-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-ROOT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROJECT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECONCILE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RELEASE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RETAIN-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SURFACE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-THRESHOLD-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-TIME-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001a", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001a", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001b", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001b", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001c", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001c", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-007", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-008", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-009", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-010", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-011", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-012", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-007", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-010", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-011", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-012", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-004", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-HAND-004", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-HAND-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-002", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-002", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PLAN-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-006", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-006", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-008", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-008", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-009", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-009", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-SEAL-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-007", :source-kind :implementation}], :witness-debt-ledger {"WTL-APPLY-007" #{:implementation}, "PERF-SENT-IMPORT-001" #{:test}, "OP-ALG-EFFECT-002" #{:implementation}, "TEST-ISO-RACE-002" #{:implementation}, "OP-ALG-PREVIEW-002" #{:implementation :test}, "PERF-SENT-VERDICT-002" #{:test}, "PERF-SENT-AUTH-003" #{:test}, "PERF-SENT-VERDICT-003" #{:test}, "PERF-SENT-PROJECT-001" #{:test}, "TEST-ISO-001a" #{:implementation :test}, "PERF-SENT-LEDGER-004" #{:test}, "WTL-SEAL-006" #{:implementation}, "PERF-SENT-RELEASE-001" #{:test}, "WTL-HAND-005" #{:implementation}, "PERF-SENT-TIME-001" #{:test}, "MEASURE-EVID-001" #{:test}, "WTL-APPLY-002" #{:implementation}, "PERF-SENT-SCHEDULE-002" #{:test}, "PERF-SENT-CONFIG-001" #{:test}, "WTL-CLI-003" #{:implementation}, "OP-ALG-RUNTIME-001" #{:test}, "WTL-HAND-002" #{:implementation}, "PERF-SENT-CUTOVER-002" #{:test}, "OP-ALG-STALE-001" #{:implementation}, "WTL-INV-002" #{:implementation}, "PERF-SENT-AUTH-001" #{:test}, "TEST-ISO-010" #{:implementation}, "WTL-PLAN-004" #{:implementation}, "WTL-PRUNE-008" #{:implementation :test}, "PERF-SENT-LEDGER-003" #{:test}, "WTL-APPLY-004" #{:implementation}, "TEST-ISO-009" #{:test}, "PERF-SENT-SCHEDULE-003" #{:test}, "OP-ALG-EFFECT-004" #{:implementation :test}, "OP-ALG-DECODE-001" #{:implementation :test}, "PERF-SENT-BASELINE-001" #{:test}, "PERF-SENT-AUTH-002" #{:test}, "PERF-SENT-CUTOVER-003" #{:test}, "OP-ALG-OUTCOME-003" #{:implementation :test}, "TEST-ISO-001c" #{:implementation :test}, "PERF-SENT-RETAIN-001" #{:test}, "PERF-SENT-CLIENT-001" #{:test}, "WTL-SEAL-004" #{:implementation}, "PERF-SENT-RECEIPT-001" #{:test}, "PERF-SENT-INVALID-001" #{:test}, "OP-ALG-OUTCOME-002" #{:test}, "WTL-APPLY-010" #{:implementation}, "PERF-SENT-RECOVERY-002" #{:test}, "OP-ALG-REFUSE-001" #{:implementation :test}, "PERF-SENT-IDENT-001" #{:test}, "PERF-SENT-SCHEDULE-001" #{:test}, "MEASURE-WALL-002" #{:test}, "PERF-SENT-PROMOTION-002" #{:test}, "TEST-ISO-008" #{:test}, "PERF-SENT-SCHEDULE-004" #{:test}, "WTL-HAND-004" #{:implementation :test}, "OP-ALG-COMMIT-004" #{:implementation :test}, "PERF-SENT-IMPORT-002" #{:test}, "PERF-SENT-BACKFILL-001" #{:test}, "PERF-SENT-LEDGER-002" #{:test}, "TEST-ISO-011" #{:test}, "PERF-SENT-PUBLISH-002" #{:test}, "PERF-SENT-LEDGER-ROOT-001" #{:test}, "OP-ALG-CONTEXT-002" #{:test}, "WTL-INV-006" #{:implementation}, "WTL-APPLY-003" #{:implementation}, "PERF-SENT-PROMOTION-001" #{:test}, "PERF-SENT-SURFACE-001" #{:test}, "PERF-SENT-ATTEMPT-003" #{:test}, "PERF-SENT-ATTEMPT-001" #{:test}, "OP-ALG-RECEIPT-002" #{:implementation :test}, "OP-ALG-EFFECT-003" #{:implementation}, "OP-ALG-SHADOW-001" #{:implementation}, "OP-ALG-PARITY-001" #{:implementation :test}, "WTL-APPLY-008" #{:implementation}, "PERF-SENT-RECEIPT-002" #{:test}, "WTL-PLAN-003" #{:implementation}, "PERF-SENT-ATTEMPT-002" #{:test}, "TEST-ISO-003" #{:implementation}, "WTL-SEAL-002" #{:implementation}, "PERF-SENT-IDENT-002" #{:test}, "PERF-SENT-VERDICT-001" #{:test}, "WTL-APPLY-011" #{:implementation}, "TEST-ISO-004" #{:implementation}, "WTL-PRUNE-009" #{:implementation :test}, "OP-ALG-OUTCOME-001" #{:implementation}, "PERF-SENT-ADMIT-001" #{:test}, "TEST-ISO-012" #{:test}, "MEASURE-WALL-001" #{:test}, "WTL-INV-005" #{:implementation}, "OP-ALG-PARITY-002" #{:implementation :test}, "WTL-SEAL-007" #{:implementation}, "WTL-PLAN-006" #{:implementation :test}, "WTL-INV-003" #{:implementation}, "WTL-CLI-002" #{:implementation}, "PERF-SENT-PUBLISH-001" #{:test}, "PERF-SENT-CUTOVER-001" #{:test}, "WTL-SEAL-003" #{:implementation}, "TEST-ISO-006" #{:implementation}, "WTL-INV-008" #{:implementation}, "OP-ALG-PERF-002" #{:implementation :test}, "PERF-SENT-RECOVERY-001" #{:test}, "WTL-APPLY-006" #{:implementation}, "OP-ALG-RECEIPT-003" #{:test}, "TEST-ISO-007" #{:implementation}, "PERF-SENT-LEDGER-001" #{:test}, "OP-ALG-RECEIPT-001" #{:implementation :test}, "PERF-SENT-RECONCILE-001" #{:test}, "PERF-SENT-RECOVERY-004" #{:test}, "PERF-SENT-RECOVERY-003" #{:test}, "PERF-SENT-THRESHOLD-001" #{:test}, "OP-ALG-PREVIEW-001" #{:implementation :test}, "WTL-PLAN-002" #{:implementation :test}, "OP-ALG-PERF-001" #{:implementation :test}, "OP-ALG-MCP-001" #{:test}, "TEST-ISO-RACE-001" #{:implementation}, "MEASURE-WALL-003" #{:test}, "TEST-ISO-001b" #{:implementation :test}, "PERF-SENT-INVALID-002" #{:test}, "PERF-SENT-ADMIT-002" #{:test}, "OP-ALG-COMMIT-002" #{:test}, "TEST-ISO-005" #{:implementation}, "WTL-APPLY-012" #{:implementation}}}
gate-stage: {:target "intent-audit", :exit 0, :wall-ms 2149}
landing-gate: {:pool {:target "runtime-pool", :exit 0, :wall-ms 152828, :phases [{:phase 0, :process-count 14, :wall-ms 23373} {:phase 1, :process-count 24, :wall-ms 129401}], :preparation {:command ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"], :exit 0, :wall-ms 53}, :peak-worker-count 8, :shell-checks {:namespaces [], :index 0, :exit 0, :started-ms 1789216853164, :phase 1, :err-log "target/gate-prewarm/f9b818cd-f120-4bb6-8f33-dfc611e81e12/lane-0.err", :runtime nil, :wall-ms 129401, :suite "shell", :completed-ms 1789216982565, :log "target/gate-prewarm/f9b818cd-f120-4bb6-8f33-dfc611e81e12/lane-0.out"}}, :stages [{:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11089} {:target "alias-migration-test", :exit 0, :wall-ms 89472} {:target "mcp-test", :exit 0, :wall-ms 89255} {:target "test-bb", :exit 0, :wall-ms 86493} {:target "test-bb-diagnostic", :exit 0, :wall-ms 218655} {:target "repository-hygiene", :exit 0, :wall-ms 2011} {:target "intent-audit", :exit 0, :wall-ms 2149}], :landing? false, :problems [], :capacity {:lanes 8, :memory-mib 24333, :admission :abstract-socket, :lane-charge-mib 1536, :reserve-mib 2048, :slot-root "abstract", :floor-mib 3584, :heap-mib 512, :cpus 16}, :started-at "2026-09-12T12:40:18.317176051Z", :state :passed, :git-tree "e06f32818441ed08acb03ee88579c4c2bd15b863", :wall-ms 387383, :completed-at "2026-09-12T12:46:45.700481800Z", :source-digest "d00f19c4ff9d5c68f9d4b10efd833ce3a977d31c47113d216aeac883376155fc", :prewarm? true, :run-id "f9b818cd-f120-4bb6-8f33-dfc611e81e12", :git-head "e679393fe5b122b3f21d7b8418f7c57b58da70f8"}
