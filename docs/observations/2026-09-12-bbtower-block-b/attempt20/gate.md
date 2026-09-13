cd libs/clj-splice && bb -e '(System/setProperty "java.io.tmpdir" (or (System/getenv "TMPDIR") (throw (ex-info "TMPDIR required" {})))) (require (quote clj-splice.test-runner)) (clj-splice.test-runner/-main)'

Testing clj-splice.core-test

Ran 5 tests containing 59 assertions.
0 failures, 0 errors.
projection-sha256 eb7f6099906935cd68d29b6a7c99b96a4e129fee2ef9b4640f4b70e76260b622
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite fast
battery-parallel: 94 namespace(s) in 94 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-compact-relations-test at 8913 ms
  process 0 phase 0 (est 2615 ms): clj-surgeon.splice-envelope-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test
  process 1 phase 0 (est 2174 ms): clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.file-ops-test clj-surgeon.mcp-telemetry-test clj-surgeon.forms-test
  process 2 phase 0 (est 2624 ms): clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mission-usage-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test
  process 3 phase 0 (est 2164 ms): clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-compact-edit-test
  process 4 phase 0 (est 3659 ms): clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  process 5 phase 0 (est 1129 ms): clj-surgeon.namespace-split-test clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 6 phase 0 (est 3813 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-test
  process 7 phase 0 (est 976 ms): clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.battery-parallel-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.battery-ledger-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-forms-test
  process 8 phase 0 (est 4330 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mission-candidate-race-test
  process 9 phase 0 (est 460 ms): clj-surgeon.mcp-intent-contract-test clj-surgeon.telemetry-events-test clj-surgeon.cljc.split-test clj-surgeon.fast-lane-isolation-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.mcp-recovery-test clj-surgeon.outline-differential-test clj-surgeon.split-proof-gate-test
  process 10 phase 0 (est 4530 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-semantic-client-test
  process 11 phase 0 (est 259 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.mission-plain-forms-test clj-surgeon.fix-declares-test clj-surgeon.require-change-test clj-surgeon.mission-forms-source-test clj-surgeon.syntax-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-candidate-test
  process 12 phase 0 (est 7088 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 8913 ms): clj-surgeon.mcp-compact-relations-test

========== lane 0 (clj-surgeon.splice-envelope-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test) exit 0, 20710 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 3416 ms):
      1529 ms  clj-surgeon.splice-envelope-test
       864 ms  clj-surgeon.outline-memory-test
       472 ms  clj-surgeon.mcp-compact-location-test
       403 ms  clj-surgeon.mcp-expect-guard-test
       148 ms  clj-surgeon.mcp-formatter-test

========== lane 1 (clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.file-ops-test clj-surgeon.mcp-telemetry-test clj-surgeon.forms-test) exit 0, 2946 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.forms-test

========== lane 2 (clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mission-usage-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 3210 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 3 (clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-compact-edit-test) exit 0, 17175 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.scope-stream-test

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

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 3 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 2950 ms):
      1473 ms  clj-surgeon.ns-isolation-test
       817 ms  clj-surgeon.rename-alias-receipt-test
       318 ms  clj-surgeon.scope-stream-test
       181 ms  clj-surgeon.mcp-relation-census-round20-test
       161 ms  clj-surgeon.mcp-compact-edit-test

========== lane 4 (clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test) exit 0, 21529 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 4449 ms):
      2960 ms  clj-surgeon.insert-forms-test
       771 ms  clj-surgeon.insert-forms-receipt-test
       291 ms  clj-surgeon.mcp-create-files-test
       282 ms  clj-surgeon.mcp-prepared-confirmation-test
       145 ms  clj-surgeon.mcp-operation-async-test

========== lane 5 (clj-surgeon.namespace-split-test clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1891 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mcp-compact-edit-fields-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 6 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-test) exit 0, 23884 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-operation-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 2664 ms):
      1730 ms  clj-surgeon.mcp-inspect-tool-test
       405 ms  clj-surgeon.receipt-booleans-test
       201 ms  clj-surgeon.mcp-write-refusal-test
       187 ms  clj-surgeon.mcp-combinable-transaction-test
       141 ms  clj-surgeon.mcp-operation-test

========== lane 7 (clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.battery-parallel-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.battery-ledger-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-forms-test) exit 0, 1353 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mission-forms-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mission-candidate-race-test) exit 0, 21380 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 5251 ms):
      4110 ms  clj-surgeon.mcp-namespace-split-test
       485 ms  clj-surgeon.mcp-change-buffer-test
       287 ms  clj-surgeon.census-pool-test
       201 ms  clj-surgeon.mcp-extraction-plan-test
       168 ms  clj-surgeon.mission-candidate-race-test

========== lane 9 (clj-surgeon.mcp-intent-contract-test clj-surgeon.telemetry-events-test clj-surgeon.cljc.split-test clj-surgeon.fast-lane-isolation-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.mcp-recovery-test clj-surgeon.outline-differential-test clj-surgeon.split-proof-gate-test) exit 0, 893 ms ==========

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.move-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.split-proof-gate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-semantic-client-test) exit 0, 24102 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 4926 ms):
      4461 ms  clj-surgeon.mcp-operation-registry-test
       326 ms  clj-surgeon.mcp-prepared-request-test
       139 ms  clj-surgeon.mcp-semantic-client-test

========== lane 11 (clj-surgeon.workspace-onboarding-test clj-surgeon.mission-plain-forms-test clj-surgeon.fix-declares-test clj-surgeon.require-change-test clj-surgeon.mission-forms-source-test clj-surgeon.syntax-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-candidate-test) exit 0, 713 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.mission-candidate-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 19144 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5088 ms):
      5088 ms  clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.mcp-compact-relations-test) exit 0, 25898 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 13 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8725 ms):
      8725 ms  clj-surgeon.mcp-compact-relations-test

Ran 1243 tests containing 12317 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 25952 ms
cadence :fast: serial-equivalent 44924 ms; budget 60000 ms
bb-runtime: serial-equivalent 7455 ms; budget 343102 ms; makespan 22094 ms

namespace walls (94, slowest first, serial-equivalent total 44924 ms):
      8725 ms  clj-surgeon.mcp-compact-relations-test
      5088 ms  clj-surgeon.rename-alias-test
      4461 ms  clj-surgeon.mcp-operation-registry-test
      4110 ms  clj-surgeon.mcp-namespace-split-test
      2960 ms  clj-surgeon.insert-forms-test
      1730 ms  clj-surgeon.mcp-inspect-tool-test
      1529 ms  clj-surgeon.splice-envelope-test
      1473 ms  clj-surgeon.ns-isolation-test
      1365 ms  clj-surgeon.lane-manifest-test
       956 ms  clj-surgeon.outline-test
       864 ms  clj-surgeon.outline-memory-test
       861 ms  clj-surgeon.helper-extraction-test
       817 ms  clj-surgeon.rename-alias-receipt-test
       798 ms  clj-surgeon.operation-algebra-test
       771 ms  clj-surgeon.insert-forms-receipt-test
       739 ms  clj-surgeon.namespace-split-test
       680 ms  clj-surgeon.analyze-test
       485 ms  clj-surgeon.mcp-change-buffer-test
       472 ms  clj-surgeon.mcp-compact-location-test
       405 ms  clj-surgeon.receipt-booleans-test
       403 ms  clj-surgeon.mcp-expect-guard-test
       326 ms  clj-surgeon.mcp-prepared-request-test
       318 ms  clj-surgeon.scope-stream-test
       299 ms  clj-surgeon.mcp-inspect-contract-test
       291 ms  clj-surgeon.mcp-create-files-test
       287 ms  clj-surgeon.census-pool-test
       282 ms  clj-surgeon.mcp-prepared-confirmation-test
       235 ms  clj-surgeon.mcp-intent-contract-test
       201 ms  clj-surgeon.mcp-extraction-plan-test
       201 ms  clj-surgeon.mcp-write-refusal-test
       187 ms  clj-surgeon.mcp-combinable-transaction-test
       181 ms  clj-surgeon.mcp-relation-census-round20-test
       178 ms  clj-surgeon.alias-migration-test
       168 ms  clj-surgeon.mission-candidate-race-test
       161 ms  clj-surgeon.mcp-compact-edit-test
       148 ms  clj-surgeon.mcp-formatter-test
       145 ms  clj-surgeon.mcp-operation-async-test
       141 ms  clj-surgeon.mcp-operation-test
       139 ms  clj-surgeon.mcp-semantic-client-test
       130 ms  clj-surgeon.ls-tree-test
       115 ms  clj-surgeon.recovery-test
       112 ms  clj-surgeon.workspace-onboarding-test
        91 ms  clj-surgeon.move-dependency-test
        90 ms  clj-surgeon.cljc-existing-ops-test
        78 ms  clj-surgeon.jvm-error-test
        61 ms  clj-surgeon.telemetry-events-test
        56 ms  clj-surgeon.owner-hypotheses-test
        50 ms  clj-surgeon.mcp-extraction-test
        45 ms  clj-surgeon.relation-census-test
        41 ms  clj-surgeon.fix-declares-test
        40 ms  clj-surgeon.structural-lens-test
        38 ms  clj-surgeon.mcp-workspace-test
        34 ms  clj-surgeon.agent-routing-test
        33 ms  clj-surgeon.battery-parallel-test
        32 ms  clj-surgeon.fast-lane-isolation-test
        29 ms  clj-surgeon.require-change-test
        27 ms  clj-surgeon.mission-plain-forms-test
        25 ms  clj-surgeon.move-test
        25 ms  clj-surgeon.worktree-lifecycle-io-test
        20 ms  clj-surgeon.memory-battery-test
        20 ms  clj-surgeon.outermost-test
        20 ms  clj-surgeon.syntax-var-refs-test
        17 ms  clj-surgeon.mcp-program-tool-test
        15 ms  clj-surgeon.edit-dsl-test
        11 ms  clj-surgeon.cljc.split-test
        11 ms  clj-surgeon.mcp-contract-test
         8 ms  clj-surgeon.mission-forms-source-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.mission-typist-test
         5 ms  clj-surgeon.outline-differential-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.mcp-telemetry-test
         4 ms  clj-surgeon.rename-test
         3 ms  clj-surgeon.battery-ledger-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.split-proof-gate-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.mission-usage-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-read-request-normalization-test
         1 ms  clj-surgeon.mission-candidate-test
         1 ms  clj-surgeon.mission-forms-test
         1 ms  clj-surgeon.worktree-lifecycle-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (14), makespan 25952 ms:
  lane 0     20710 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test
  lane 1      2946 ms  exit 0  clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.file-ops-test clj-surgeon.mcp-telemetry-test clj-surgeon.forms-test
  lane 2      3210 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.structural-lens-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mission-usage-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-read-request-normalization-test
  lane 3     17175 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-compact-edit-test
  lane 4     21529 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  lane 5      1891 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.rename-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 6     23884 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-test
  lane 7      1353 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.battery-parallel-test clj-surgeon.relation-census-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.battery-ledger-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-forms-test
  lane 8     21380 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mission-candidate-race-test
  lane 9       893 ms  exit 0  clj-surgeon.mcp-intent-contract-test clj-surgeon.telemetry-events-test clj-surgeon.cljc.split-test clj-surgeon.fast-lane-isolation-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.mcp-recovery-test clj-surgeon.outline-differential-test clj-surgeon.split-proof-gate-test
  lane 10     24102 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-semantic-client-test
  lane 11       713 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.mission-plain-forms-test clj-surgeon.fix-declares-test clj-surgeon.require-change-test clj-surgeon.mission-forms-source-test clj-surgeon.syntax-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-candidate-test
  lane 12     19144 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13     25898 ms  exit 0  clj-surgeon.mcp-compact-relations-test

test-isolation: 0 violations across 94 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 25952 ms over 8 lane(s); serial-equivalent 44924 ms; skipped 0
{"command": ["make", "landing-gate-prewarm"], "writable_roots": ["/home/forge/src/clj-surgeon-bbtower", "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp", "/home/forge/.local/state/clj-surgeon"], "writable_devices": ["/dev/null"], "started": 1789232542.8102164, "diagnostic": true}
/home/forge/src/clj-surgeon-bbtower/docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py:35: DeprecationWarning: Due to '_pack_', the 'Rule' Structure will use memory layout compatible with MSVC (Windows). If this is intended, set _layout_ to 'ms'. The implicit default is deprecated and slated to become an error in Python 3.19.
  class Rule(ctypes.Structure):
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 25272 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T17:02:25.128730566Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=126
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=121
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=197
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11172}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 77069 ms
  process 0 phase 0 (est 4460 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 77069 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 47760 ms
  process 0 phase 0 (est 4553 ms): clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-compact-edit-test
  process 1 phase 0 (est 309 ms): clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.telemetry-events-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-candidate-test
  process 2 phase 0 (est 3619 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.mcp-schema-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test
  process 3 phase 0 (est 1243 ms): clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  process 4 phase 0 (est 3062 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test
  process 5 phase 0 (est 1800 ms): clj-surgeon.namespace-split-test clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.outline-differential-test clj-surgeon.mcp-read-request-normalization-test
  process 6 phase 0 (est 3107 ms): clj-surgeon.insert-forms-test clj-surgeon.mcp-combinable-transaction-test
  process 7 phase 0 (est 1755 ms): clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.mcp-extraction-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-usage-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 8 phase 0 (est 4683 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-operation-test
  process 9 phase 0 (est 180 ms): clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.split-proof-gate-test
  process 10 phase 0 (est 4317 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test
  process 11 phase 0 (est 546 ms): clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test clj-surgeon.mission-forms-test
  process 12 phase 0 (est 7113 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 8394 ms): clj-surgeon.mcp-compact-relations-test
  process 14 phase 1 (est 217 ms): clj-surgeon.mcp-server-test
  process 15 phase 1 (est 1492 ms): clj-surgeon.namespace-split-warm-test
  process 16 phase 1 (est 2089 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 2585 ms): clj-surgeon.mcp-http-server-test
  process 18 phase 1 (est 3306 ms): clj-surgeon.mcp-tool-test
  process 19 phase 1 (est 8964 ms): clj-surgeon.outline-corpus-integration-test
  process 20 phase 1 (est 47760 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 89921 ms
  process 0 phase 0 (est 5963 ms): clj-surgeon.intent-transaction-test
  process 1 phase 0 (est 5680 ms): clj-surgeon.extract-test clj-surgeon.edn-config-integration-test
  process 2 phase 0 (est 1971 ms): clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.recovery-test clj-surgeon.move-dependency-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 3 phase 0 (est 13407 ms): clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.platform-selector-test
  process 4 phase 0 (est 209 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.agent-routing-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test
  process 5 phase 0 (est 12595 ms): clj-surgeon.help-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  process 6 phase 0 (est 1021 ms): clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.memory-battery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  process 7 phase 0 (est 13379 ms): clj-surgeon.show-form-test
  process 8 phase 0 (est 237 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.outermost-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test
  process 9 phase 0 (est 13827 ms): clj-surgeon.tmp-leak-support-test
  process 10 phase 0 (est 14079 ms): clj-surgeon.cli-dispatch-test
  process 11 phase 0 (est 72228 ms): clj-surgeon.parser-admission-test
  process 12 phase 0 (est 89921 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 15069 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 4473 ms):
      4473 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 91199 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 78737 ms):
     78737 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3648 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91200 ms
cadence :battery: serial-equivalent 83210 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 83210 ms):
     78737 ms  clj-surgeon.mcp-alias-migration-test
      4473 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 91200 ms:
  lane 0     15069 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     91199 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 91200 ms over 2 lane(s); serial-equivalent 83210 ms; skipped 0

========== lane 0 (clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-compact-edit-test) exit 0, 22153 ms ==========
lanes: --ns -- 7 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (7, slowest first, total 4811 ms):
      1447 ms  clj-surgeon.ns-isolation-test
      1310 ms  clj-surgeon.splice-envelope-test
       691 ms  clj-surgeon.rename-alias-receipt-test
       496 ms  clj-surgeon.mcp-compact-location-test
       477 ms  clj-surgeon.mcp-change-buffer-test
       228 ms  clj-surgeon.mcp-prepared-request-test
       162 ms  clj-surgeon.mcp-compact-edit-test

========== lane 1 (clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.telemetry-events-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-candidate-test) exit 0, 940 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.mission-candidate-test

========== lane 2 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.mcp-schema-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 3959 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.outline-test

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 3 (clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test) exit 0, 15769 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-prepared-confirmation-test

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

Testing clj-surgeon.mission-candidate-race-test
---------- lane 3 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (5, slowest first, total 1274 ms):
       407 ms  clj-surgeon.scope-stream-test
       287 ms  clj-surgeon.census-pool-test
       257 ms  clj-surgeon.mcp-prepared-confirmation-test
       179 ms  clj-surgeon.mcp-relation-census-round20-test
       144 ms  clj-surgeon.mission-candidate-race-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test) exit 0, 23002 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-operation-async-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (5, slowest first, total 2662 ms):
      1764 ms  clj-surgeon.mcp-inspect-tool-test
       426 ms  clj-surgeon.receipt-booleans-test
       180 ms  clj-surgeon.mcp-extraction-plan-test
       149 ms  clj-surgeon.mcp-semantic-client-test
       143 ms  clj-surgeon.mcp-operation-async-test

========== lane 5 (clj-surgeon.namespace-split-test clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.outline-differential-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 2529 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 6 (clj-surgeon.insert-forms-test clj-surgeon.mcp-combinable-transaction-test) exit 0, 20068 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.mcp-combinable-transaction-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (2, slowest first, total 3359 ms):
      3082 ms  clj-surgeon.insert-forms-test
       277 ms  clj-surgeon.mcp-combinable-transaction-test

========== lane 7 (clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.mcp-extraction-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-usage-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 2338 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-operation-test) exit 0, 22209 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-formatter-test

Testing clj-surgeon.mcp-operation-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (6, slowest first, total 4718 ms):
      3076 ms  clj-surgeon.mcp-namespace-split-test
       715 ms  clj-surgeon.insert-forms-receipt-test
       365 ms  clj-surgeon.outline-memory-test
       248 ms  clj-surgeon.mcp-expect-guard-test
       165 ms  clj-surgeon.mcp-formatter-test
       149 ms  clj-surgeon.mcp-operation-test

========== lane 9 (clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.split-proof-gate-test) exit 0, 479 ms ==========

Testing clj-surgeon.recovery-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.move-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.split-proof-gate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test) exit 0, 23796 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-write-refusal-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (3, slowest first, total 5140 ms):
      4547 ms  clj-surgeon.mcp-operation-registry-test
       400 ms  clj-surgeon.mcp-create-files-test
       193 ms  clj-surgeon.mcp-write-refusal-test

========== lane 11 (clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test clj-surgeon.mission-forms-test) exit 0, 1142 ms ==========

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.mission-forms-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 17890 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 4953 ms):
      4953 ms  clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.mcp-compact-relations-test) exit 0, 25568 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 13 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 8686 ms):
      8686 ms  clj-surgeon.mcp-compact-relations-test

========== lane 14 (clj-surgeon.mcp-server-test) exit 0, 11244 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
clj-surgeon MCP: embedded nREPL on 45379 ( /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3423646-699b6d1d/clj-surgeon-mcp-server-test-7177818184009300689/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 41299 ( /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3423646-699b6d1d/clj-surgeon-mcp-server-test-15359812294519636927/.nrepl-port )

namespace walls (1, slowest first, total 256 ms):
       256 ms  clj-surgeon.mcp-server-test

========== lane 15 (clj-surgeon.namespace-split-warm-test) exit 0, 9928 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 1470 ms):
      1470 ms  clj-surgeon.namespace-split-warm-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 7533 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 2216 ms):
      2216 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.mcp-http-server-test) exit 0, 15935 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
clj-surgeon MCP: embedded nREPL on 40529 ( /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3416819-b8bec3f3/clj-surgeon-mcp-http-test-6471092715672269842/.nrepl-port )

namespace walls (1, slowest first, total 2889 ms):
      2889 ms  clj-surgeon.mcp-http-server-test

========== lane 18 (clj-surgeon.mcp-tool-test) exit 0, 14232 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 3235 ms):
      3235 ms  clj-surgeon.mcp-tool-test

========== lane 19 (clj-surgeon.outline-corpus-integration-test) exit 0, 15291 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 8983 ms):
      8983 ms  clj-surgeon.outline-corpus-integration-test

========== lane 20 (clj-surgeon.mcp-feature-thread-test) exit 0, 54765 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 47539 ms):
     47539 ms  clj-surgeon.mcp-feature-thread-test

Ran 1414 tests containing 16173 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91581 ms
cadence :fast: serial-equivalent 43611 ms; budget 60000 ms
cadence :integration: serial-equivalent 66588 ms; budget 240000 ms
bb-runtime: serial-equivalent 8008 ms; budget 343102 ms; makespan 20456 ms

namespace walls (101, slowest first, serial-equivalent total 110199 ms):
     47539 ms  clj-surgeon.mcp-feature-thread-test
      8983 ms  clj-surgeon.outline-corpus-integration-test
      8686 ms  clj-surgeon.mcp-compact-relations-test
      4953 ms  clj-surgeon.rename-alias-test
      4547 ms  clj-surgeon.mcp-operation-registry-test
      3235 ms  clj-surgeon.mcp-tool-test
      3082 ms  clj-surgeon.insert-forms-test
      3076 ms  clj-surgeon.mcp-namespace-split-test
      2889 ms  clj-surgeon.mcp-http-server-test
      2216 ms  clj-surgeon.mcp-hot-verify-test
      1764 ms  clj-surgeon.mcp-inspect-tool-test
      1519 ms  clj-surgeon.lane-manifest-test
      1470 ms  clj-surgeon.namespace-split-warm-test
      1447 ms  clj-surgeon.ns-isolation-test
      1310 ms  clj-surgeon.splice-envelope-test
       929 ms  clj-surgeon.outline-test
       878 ms  clj-surgeon.helper-extraction-test
       840 ms  clj-surgeon.namespace-split-test
       801 ms  clj-surgeon.operation-algebra-test
       747 ms  clj-surgeon.analyze-test
       715 ms  clj-surgeon.insert-forms-receipt-test
       691 ms  clj-surgeon.rename-alias-receipt-test
       496 ms  clj-surgeon.mcp-compact-location-test
       477 ms  clj-surgeon.mcp-change-buffer-test
       426 ms  clj-surgeon.receipt-booleans-test
       407 ms  clj-surgeon.scope-stream-test
       400 ms  clj-surgeon.mcp-create-files-test
       365 ms  clj-surgeon.outline-memory-test
       335 ms  clj-surgeon.mcp-intent-contract-test
       321 ms  clj-surgeon.mcp-inspect-contract-test
       287 ms  clj-surgeon.census-pool-test
       277 ms  clj-surgeon.mcp-combinable-transaction-test
       257 ms  clj-surgeon.mcp-prepared-confirmation-test
       256 ms  clj-surgeon.mcp-server-test
       248 ms  clj-surgeon.mcp-expect-guard-test
       228 ms  clj-surgeon.mcp-prepared-request-test
       193 ms  clj-surgeon.mcp-write-refusal-test
       180 ms  clj-surgeon.mcp-extraction-plan-test
       179 ms  clj-surgeon.mcp-relation-census-round20-test
       175 ms  clj-surgeon.alias-migration-test
       165 ms  clj-surgeon.mcp-formatter-test
       162 ms  clj-surgeon.mcp-compact-edit-test
       149 ms  clj-surgeon.mcp-operation-test
       149 ms  clj-surgeon.mcp-semantic-client-test
       144 ms  clj-surgeon.mission-candidate-race-test
       143 ms  clj-surgeon.mcp-operation-async-test
       127 ms  clj-surgeon.cljc-existing-ops-test
       113 ms  clj-surgeon.workspace-onboarding-test
       111 ms  clj-surgeon.ls-tree-test
       105 ms  clj-surgeon.move-dependency-test
        77 ms  clj-surgeon.jvm-error-test
        71 ms  clj-surgeon.mcp-workspace-test
        69 ms  clj-surgeon.fix-declares-test
        65 ms  clj-surgeon.telemetry-events-test
        60 ms  clj-surgeon.edit-dsl-test
        51 ms  clj-surgeon.agent-routing-test
        50 ms  clj-surgeon.recovery-test
        45 ms  clj-surgeon.mcp-extraction-test
        45 ms  clj-surgeon.owner-hypotheses-test
        41 ms  clj-surgeon.relation-census-test
        36 ms  clj-surgeon.structural-lens-test
        34 ms  clj-surgeon.battery-parallel-test
        34 ms  clj-surgeon.require-change-test
        33 ms  clj-surgeon.fast-lane-isolation-test
        30 ms  clj-surgeon.move-test
        28 ms  clj-surgeon.mcp-program-tool-test
        24 ms  clj-surgeon.mission-plain-forms-test
        24 ms  clj-surgeon.worktree-lifecycle-io-test
        23 ms  clj-surgeon.memory-battery-test
        20 ms  clj-surgeon.mission-typist-test
        18 ms  clj-surgeon.outermost-test
        18 ms  clj-surgeon.worktree-lifecycle-test
        10 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.mcp-contract-test
         9 ms  clj-surgeon.forms-test
         8 ms  clj-surgeon.mcp-schema-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.mission-forms-source-test
         6 ms  clj-surgeon.cljc.merge-test
         5 ms  clj-surgeon.rename-test
         5 ms  clj-surgeon.syntax-var-refs-test
         4 ms  clj-surgeon.battery-ledger-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.mcp-recovery-test
         4 ms  clj-surgeon.outline-differential-test
         4 ms  clj-surgeon.split-proof-gate-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.mcp-telemetry-test
         3 ms  clj-surgeon.mission-usage-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.mission-forms-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-read-request-normalization-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (21), makespan 91581 ms:
  lane 0     22153 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-compact-edit-test
  lane 1       940 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.telemetry-events-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.extract-header-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-candidate-test
  lane 2      3959 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.helper-extraction-test clj-surgeon.battery-parallel-test clj-surgeon.mission-plain-forms-test clj-surgeon.structural-lens-test clj-surgeon.outermost-test clj-surgeon.mcp-schema-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-compact-edit-fields-test
  lane 3     15769 ms  exit 0  clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  lane 4     23002 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-async-test clj-surgeon.mcp-semantic-client-test
  lane 5      2529 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.outline-differential-test clj-surgeon.mcp-read-request-normalization-test
  lane 6     20068 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.mcp-combinable-transaction-test
  lane 7      2338 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.mcp-workspace-test clj-surgeon.move-dependency-test clj-surgeon.mcp-extraction-test clj-surgeon.memory-battery-test clj-surgeon.quoted-var-refs-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-usage-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 8     22209 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-operation-test
  lane 9       479 ms  exit 0  clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.split-proof-gate-test
  lane 10     23796 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-write-refusal-test
  lane 11      1142 ms  exit 0  clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test clj-surgeon.mission-forms-test
  lane 12     17890 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13     25568 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 14     11244 ms  exit 0  clj-surgeon.mcp-server-test
  lane 15      9928 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 16      7533 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     15935 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 18     14232 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 19     15291 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 20     54765 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 91581 ms over 8 lane(s); serial-equivalent 110199 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 14862 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp

namespace walls (1, slowest first, total 6511 ms):
      6511 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.edn-config-integration-test) exit 0, 6513 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.edn-config-integration-test

========== lane 2 (clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.recovery-test clj-surgeon.move-dependency-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 2452 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.move-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.platform-selector-test) exit 0, 13934 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.platform-selector-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393251-9be5a03d/andon-shell-safety16528809149355299592/H; touch /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393251-9be5a03d/andon-shell-safety16528809149355299592/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393251-9be5a03d/andon-shell-safety1142700083281683253/H$(touch /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393251-9be5a03d/andon-shell-safety1142700083281683253/PWNED-SUBST)"

========== lane 4 (clj-surgeon.workspace-onboarding-test clj-surgeon.agent-routing-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test) exit 0, 669 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.forms-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 13075 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test

========== lane 6 (clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.memory-battery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test) exit 0, 1624 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.failure-report-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 13861 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.cljc-existing-ops-test clj-surgeon.outermost-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test) exit 0, 569 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.cljc.analyze-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14165 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393243-ce9a6d55/tmp-leak-sweep-guard-18307133173541028981/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.cli-dispatch-test) exit 0, 14671 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 11 (clj-surgeon.parser-admission-test) exit 0, 72397 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 12 (clj-surgeon.install-test) exit 0, 91493 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/bin with spaces/clj-surgeon from commit 09588a14a784e220907462f46676900402bc80aa, source hash ad50675b1de7895cacbe897169e3c9c35d9c0fd978e1c1ce7480abc06af8f8ee
Receipt: /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x62a77de1 "Process[pid=3406411, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x60bbeedd "java.lang.ProcessImpl$ProcessPipeOutputStream@60bbeedd"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.214079, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x316fedad "Process[pid=3406565, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x5ed0993e "java.lang.ProcessImpl$ProcessPipeOutputStream@5ed0993e"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.24766, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x97c8710 "Process[pid=3406697, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x318269ef "java.lang.ProcessImpl$ProcessPipeOutputStream@318269ef"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3393245-6cc14606/clj-surgeon-installed-splice-445128073306936786/sample.clj"]}

Ran 901 tests containing 7998 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91494 ms
cadence :battery: serial-equivalent 244026 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3527 ms; budget 60000 ms
bb-runtime: serial-equivalent 241042 ms; budget 343102 ms; makespan 91494 ms

namespace walls (50, slowest first, serial-equivalent total 247553 ms):
     91320 ms  clj-surgeon.install-test
     72153 ms  clj-surgeon.parser-admission-test
     14255 ms  clj-surgeon.cli-dispatch-test
     14009 ms  clj-surgeon.tmp-leak-support-test
     13446 ms  clj-surgeon.show-form-test
     10783 ms  clj-surgeon.help-test
      6511 ms  clj-surgeon.intent-transaction-test
      6238 ms  clj-surgeon.edit-test
      5681 ms  clj-surgeon.extract-test
      4625 ms  clj-surgeon.core-discovery-test
      2349 ms  clj-surgeon.lens-query-test
      1505 ms  clj-surgeon.xray-test
       931 ms  clj-surgeon.outline-test
       761 ms  clj-surgeon.operation-algebra-test
       661 ms  clj-surgeon.analyze-test
       556 ms  clj-surgeon.edn-config-integration-test
       326 ms  clj-surgeon.partition-all-test
       269 ms  clj-surgeon.platform-selector-test
       166 ms  clj-surgeon.alias-migration-test
       145 ms  clj-surgeon.ls-tree-test
       119 ms  clj-surgeon.workspace-onboarding-test
       116 ms  clj-surgeon.cljc-existing-ops-test
       101 ms  clj-surgeon.jvm-error-test
        75 ms  clj-surgeon.recovery-test
        74 ms  clj-surgeon.move-dependency-test
        62 ms  clj-surgeon.agent-routing-test
        54 ms  clj-surgeon.structural-lens-test
        44 ms  clj-surgeon.fix-declares-test
        28 ms  clj-surgeon.relation-census-test
        25 ms  clj-surgeon.memory-battery-test
        25 ms  clj-surgeon.owner-hypotheses-test
        24 ms  clj-surgeon.worktree-lifecycle-io-test
        21 ms  clj-surgeon.outermost-test
        20 ms  clj-surgeon.edit-dsl-test
        19 ms  clj-surgeon.move-test
        11 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.insertion-gap-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.cljc.require-ops-test
         4 ms  clj-surgeon.worktree-lifecycle-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.cljc.merge-test
         2 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.cljc.analyze-test
         1 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (13), makespan 91494 ms:
  lane 0     14862 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      6513 ms  exit 0  clj-surgeon.extract-test clj-surgeon.edn-config-integration-test
  lane 2      2452 ms  exit 0  clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.recovery-test clj-surgeon.move-dependency-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 3     13934 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.platform-selector-test
  lane 4       669 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.agent-routing-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test
  lane 5     13075 ms  exit 0  clj-surgeon.help-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 6      1624 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.jvm-error-test clj-surgeon.memory-battery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  lane 7     13861 ms  exit 0  clj-surgeon.show-form-test
  lane 8       569 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.outermost-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test
  lane 9     14165 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10     14671 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 11     72397 ms  exit 0  clj-surgeon.parser-admission-test
  lane 12     91493 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 91494 ms over 8 lane(s); serial-equivalent 247553 ms; skipped 0
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
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- arm-b (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- name-only (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- devshm (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/dev/shm is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- unknown-fstype (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3432430-f662631d node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3432430-f662631d node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3432430-f662631d
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3434784-cabf59c9 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3434784-cabf59c9 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3434784-cabf59c9
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3436104-74029af5 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3436104-74029af5 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/realdisk/clj-surgeon-suite-3436104-74029af5
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/subbase/clj-surgeon-suite-3436218-e6e06fd1 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/subbase/clj-surgeon-suite-3436218-e6e06fd1 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/subbase/clj-surgeon-suite-3436218-e6e06fd1
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/subbase/clj-surgeon-suite-3436218-e6e06fd1/tmp.fwWPAs7PKa
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/subbase/clj-surgeon-suite-3436218-e6e06fd1: tmp.fwWPAs7PKa
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/stalebase/clj-surgeon-suite-3437699-8bff89ca node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/stalebase/clj-surgeon-suite-3437699-8bff89ca node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/stalebase/clj-surgeon-suite-3437699-8bff89ca
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-3427937-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f/nowrite/clj-surgeon-suite-3438060-f320ea0a Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner analyzer-contract-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.memory.memory-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.memory-battery-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.mcp-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- SELF_TEST_TMP with TMPDIR=/tmp -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/tmp/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/dev/shm/probe -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR unset -> /var/tmp ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f -> /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-tmpleak-witness.7Ibj6f ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-kondo-path.VmkyNm/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-kondo-path.VmkyNm/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s.s.......
----------------------------------------------------------------------
Ran 21 tests in 0.402s

OK (skipped=3)
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
Ran 3 tests in 1.357s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp
gate-stage: {:target "alias-migration-test", :exit 0, :wall-ms 91200}
gate-stage: {:target "mcp-test", :exit 0, :wall-ms 91581}
gate-stage: {:target "test-bb", :exit 0, :wall-ms 91494}
gate-stage: test-bb-diagnostic started 2026-09-12T17:05:13.021388658Z
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp" test/run_all.clj
SERIAL/NOT-A-GATE: direct Babashka diagnostic

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.tmp-leak-support-test
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/tmp-leak-sweep-guard-16578974592521371598/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

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
Installed stable CLI /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/bin with spaces/clj-surgeon from commit 09588a14a784e220907462f46676900402bc80aa, source hash ad50675b1de7895cacbe897169e3c9c35d9c0fd978e1c1ce7480abc06af8f8ee
Receipt: /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x2c2d199c "Process[pid=3503001, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x1dfd56c5 "java.lang.ProcessImpl$ProcessPipeOutputStream@1dfd56c5"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.19491, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x77ce6c36 "Process[pid=3503005, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x2838d0de "java.lang.ProcessImpl$ProcessPipeOutputStream@2838d0de"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.237724, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x64ac956f "Process[pid=3503009, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x16873ac0 "java.lang.ProcessImpl$ProcessPipeOutputStream@16873ac0"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/clj-surgeon-installed-splice-17154170881153903725/sample.clj"]}

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.worktree-lifecycle-cli-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.cli-dispatch-test

Testing clj-surgeon.core-discovery-test
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/andon-shell-safety4807893156888255551/H; touch /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/andon-shell-safety4807893156888255551/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/andon-shell-safety10923439549347286695/H$(touch /var/tmp/forge/bbtower-fx/packets/b1ecdabf-ab7d-470b-9f55-21a1585822bc/tmp/clj-surgeon-suite-3457332-3368a4c8/andon-shell-safety10923439549347286695/PWNED-SUBST)"

Ran 832 tests containing 7297 assertions.
0 failures, 0 errors.
gate-stage: {:target "test-bb-diagnostic", :exit 0, :wall-ms 224369}
gate-stage: repository-hygiene started 2026-09-12T17:08:57.390788259Z
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
repository hygiene: no machine-local build cache is tracked at any depth
gate-stage: {:target "repository-hygiene", :exit 0, :wall-ms 2021}
gate-stage: intent-audit started 2026-09-12T17:08:59.412552841Z
{:ok true, :specs {"MCP-OP-ADMIT-135" :implemented, "MCP-OP-ADMIT-051" :implemented, "MCP-OP-HELPER-020" :active-gap, "MCP-OP-THREAD-021" :implemented, "INSERT-FORMS-020" :implemented, "MCP-OP-ALIAS-015" :implemented, "MCP-OP-THREAD-046" :implemented, "MCP-OP-ALIAS-032" :implemented, "MCP-OP-HELPER-022" :active-gap, "MCP-OP-ADMIT-020" :implemented, "WTL-APPLY-007" :implemented, "MCP-OP-CENSUS-012" :implemented, "MCP-OP-EDIT-008" :implemented, "MCP-OP-PREP-ACT-018" :active-gap, "NS-SPLIT-043" :implemented, "MCP-OP-DISPATCH-004" :implemented, "MCP-OP-EDIT-035" :implemented, "MCP-OP-EDIT-024" :implemented, "MCP-OP-ADMIT-121" :implemented, "WTL-PRUNE-005" :implemented, "PERF-SENT-IMPORT-001" :active-gap, "MCP-OP-VERIFY-010" :implemented, "MCP-OP-MEM-011" :deferred, "MCP-OP-PREP-ACT-017" :active-gap, "MCP-OP-ALIAS-002" :implemented, "OP-ALG-EFFECT-002" :implemented, "MCP-OP-HELPER-014" :active-gap, "MCP-OP-READ-MISSION-001" :deferred, "MCP-OP-READ-MISSION-002" :deferred, "MCP-OP-CENSUS-027" :implemented, "REQUIRE-CHANGE-005" :implemented, "MCP-OP-ALIAS-055" :implemented, "MCP-OP-EDIT-004" :implemented, "INSERT-FORMS-011" :implemented, "MCP-OP-ADMIT-152" :active-gap, "NS-SPLIT-057" :implemented, "MCP-OP-READ-RESOLVE-002" :deferred, "TELEMETRY-EVENTS-001" :implemented, "NS-SPLIT-060" :implemented, "MCP-OP-ADMIT-106" :implemented, "MCP-OP-ADMIT-147" :active-gap, "TEST-ISO-RACE-002" :implemented, "MCP-OP-PREP-ACT-015" :active-gap, "MCP-OP-EDIT-034" :implemented, "MCP-OP-ADMIT-069" :implemented, "INSERT-FORMS-006" :implemented, "MCP-OP-ALIAS-059" :implemented, "MCP-OP-ADMIT-150" :implemented, "OP-ALG-PREVIEW-002" :implemented, "MCP-OP-INSERT-009" :implemented, "MCP-OP-CENSUS-029" :implemented, "BB-PROBE-004" :implemented, "MCP-OP-VERIFY-009" :implemented, "MCP-OP-VERIFY-008" :implemented, "MCP-OP-ALIAS-040" :implemented, "PERF-SENT-VERDICT-002" :active-gap, "NS-SPLIT-029" :implemented, "MCP-OP-CENSUS-017" :implemented, "MCP-OP-ADMIT-021" :implemented, "MCP-OP-ADMIT-110" :implemented, "MCP-OP-FIELD-003" :implemented, "PERF-SENT-AUTH-003" :active-gap, "MCP-OP-PREP-REQ-007" :implemented, "MCP-OP-ADMIT-151" :implemented, "MCP-OP-THREAD-042" :implemented, "PERF-SENT-VERDICT-003" :active-gap, "MCP-OP-HELPER-002" :active-gap, "ALIAS-MIGRATION-005" :implemented, "PERF-SENT-PROJECT-001" :active-gap, "TEST-ISO-001a" :implemented, "MCP-OP-SHELL-ARGV-006" :implemented, "MCP-OP-ALIAS-017" :implemented, "MCP-OP-ADMIT-023" :implemented, "PERF-SENT-LEDGER-004" :active-gap, "MCP-OP-PREP-ACT-007" :active-gap, "MCP-OP-PREP-ACT-002" :active-gap, "MCP-OP-WRITE-REFUSAL-002" :deferred, "INSERT-FORMS-010" :implemented, "MCP-OP-PLAN-001" :implemented, "WTL-SEAL-006" :implemented, "PERF-SENT-RELEASE-001" :active-gap, "MCP-OP-READ-GUARD-001" :implemented, "MCP-OP-HELPER-011" :active-gap, "MCP-OP-SHELL-ARGV-007" :implemented, "WTL-PRUNE-001" :implemented, "MCP-OP-ALIAS-014" :implemented, "MCP-OP-EDIT-028" :implemented, "MCP-OP-RESULT-005" :implemented, "MCP-OP-COVERAGE-001" :implemented, "NS-SPLIT-072" :implemented, "MCP-OP-EDIT-007" :implemented, "WTL-HAND-005" :implemented, "MCP-OP-MATCHED-001" :implemented, "MCP-OP-ADMIT-107" :implemented, "TEST-ISO-002" :implemented, "MCP-OP-EDIT-012" :implemented, "MCP-OP-VERIFY-004" :implemented, "MCP-OP-POS-AUTH-003" :implemented, "MCP-OP-WRITE-REFUSAL-003" :deferred, "MCP-OP-ALIAS-051" :implemented, "MCP-OP-HELPER-018" :active-gap, "MCP-OP-ADMIT-144" :implemented, "MCP-OP-ADMIT-034" :implemented, "MCP-OP-HELPER-009" :active-gap, "NS-SPLIT-001" :implemented, "PERF-SENT-TIME-001" :active-gap, "ALIAS-MIGRATION-003" :implemented, "MCP-OP-ALIAS-046" :implemented, "MCP-OP-ADMIT-099" :implemented, "MCP-OP-THREAD-029" :implemented, "MEASURE-EVID-001" :active-gap, "MCP-OP-ADMIT-111" :implemented, "MCP-OP-ALIAS-065" :active-gap, "MCP-OP-EDIT-018" :implemented, "MCP-OP-MATCHED-004" :implemented, "MCP-OP-ALIAS-047" :implemented, "MCP-OP-PREP-ACT-001" :active-gap, "MCP-OP-THREAD-039" :implemented, "MCP-OP-ADMIT-032" :implemented, "MCP-OP-TMPHYG-012" :implemented, "MCP-OP-HELPER-019" :active-gap, "MCP-OP-PLAN-004" :implemented, "NS-SPLIT-008" :implemented, "NS-SPLIT-046" :implemented, "NS-SPLIT-042" :deferred, "MCP-OP-TRACE-002" :implemented, "OP-ALG-VERIFY-004" :deferred, "REQUIRE-CHANGE-004" :implemented, "MCP-OP-ALIAS-019" :implemented, "WTL-APPLY-002" :implemented, "NS-SPLIT-056" :implemented, "MCP-OP-EDIT-027" :implemented, "MCP-OP-INSERT-003" :active-gap, "NS-SPLIT-071" :implemented, "MCP-OP-HOTVER-002" :implemented, "MCP-OP-ALIAS-036" :implemented, "REQUIRE-CHANGE-002" :implemented, "MCP-OP-CENSUS-035" :implemented, "OP-ALG-EFFECT-005" :implemented, "MCP-OP-ADMIT-065" :implemented, "MCP-OP-ANALYZER-008" :implemented, "MCP-OP-MATCH-005" :deferred, "PERF-SENT-SCHEDULE-002" :active-gap, "MCP-OP-CENSUS-005" :implemented, "MCP-OP-ADMIT-066" :implemented, "RENAME-ALIAS-001" :implemented, "MCP-OP-ALIAS-016" :implemented, "PERF-SENT-CONFIG-001" :active-gap, "MCP-OP-ANALYZER-009" :implemented, "MCP-OP-CENSUS-034" :implemented, "MCP-OP-ASYNC-002" :implemented, "MCP-OP-PREP-REQ-005" :implemented, "OP-ALG-COMMIT-003" :implemented, "MCP-OP-PREP-ACT-014" :active-gap, "RENAME-ALIAS-003" :implemented, "MCP-OP-ADMIT-141" :implemented, "TEST-ISO-014" :implemented, "MCP-OP-HELPER-021" :active-gap, "WTL-CLI-003" :implemented, "MCP-OP-ADMIT-148" :implemented, "MCP-OP-ALIAS-063" :active-gap, "MCP-OP-ANALYZER-006" :active-gap, "INSERT-FORMS-012" :implemented, "MCP-OP-EDIT-001" :implemented, "MCP-OP-EDIT-033" :implemented, "NS-SPLIT-052" :implemented, "OP-ALG-RUNTIME-001" :implemented, "NS-SPLIT-062" :implemented, "MCP-OP-ADMIT-061" :implemented, "MCP-OP-EDIT-032" :implemented, "NS-SPLIT-035" :implemented, "MCP-OP-ALIAS-044" :implemented, "MCP-OP-SHELL-ARGV-002" :implemented, "WTL-HAND-002" :implemented, "MCP-OP-VERIFY-007" :implemented, "MCP-OP-MATCHED-005" :implemented, "MCP-OP-THREAD-006" :implemented, "WTL-SEAL-001" :implemented, "MCP-OP-PREP-ACT-005" :active-gap, "RENAME-ALIAS-014" :implemented, "WTL-INV-001" :implemented, "MCP-OP-VERIFY-003" :implemented, "MCP-OP-ADMIT-105" :implemented, "MCP-OP-ADMIT-088" :implemented, "MCP-OP-ADMIT-030" :implemented, "PERF-SENT-CUTOVER-002" :active-gap, "MCP-OP-SCHEMA-001" :implemented, "INSERT-FORMS-008" :implemented, "MCP-OP-ADMIT-149" :implemented, "RENAME-ALIAS-007" :implemented, "RENAME-ALIAS-010" :implemented, "MCP-OP-VERIFY-005" :implemented, "MCP-OP-ADMIT-139" :implemented, "MCP-OP-THREAD-018" :implemented, "MCP-OP-VERIFY-006" :implemented, "MCP-OP-READ-HYP-002" :implemented, "MCP-OP-THREAD-012" :implemented, "OP-ALG-STALE-001" :implemented, "MCP-OP-MEM-020" :implemented, "MCP-OP-TRACE-006" :implemented, "INSERT-FORMS-017" :implemented, "MCP-OP-CENSUS-010" :implemented, "MCP-OP-ALIAS-054" :implemented, "MCP-OP-READ-NORM-001" :active-gap, "MCP-OP-EDIT-009" :implemented, "NS-SPLIT-015" :implemented, "TEST-ISO-001" :implemented, "MCP-OP-READ-RESOLVE-001" :deferred, "MCP-OP-ALIAS-057" :implemented, "MCP-OP-ALIAS-009" :implemented, "WTL-INV-002" :implemented, "MCP-OP-EDIT-015" :implemented, "MCP-OP-DISPATCH-002" :implemented, "MCP-OP-WRITE-REFUSAL-006" :deferred, "MCP-OP-TIME-003" :implemented, "MCP-OP-CENSUS-028" :implemented, "PERF-SENT-AUTH-001" :active-gap, "NS-SPLIT-041" :implemented, "MCP-OP-POS-AUTH-007" :implemented, "MCP-OP-MATCHED-002" :implemented, "MCP-OP-ANALYZER-002" :implemented, "TEST-ISO-010" :implemented, "WTL-PLAN-004" :implemented, "MCP-OP-MEM-007" :implemented, "MCP-OP-ALIAS-013" :implemented, "WTL-PRUNE-008" :implemented, "PERF-SENT-LEDGER-003" :active-gap, "MCP-OP-DISPATCH-003" :implemented, "MCP-OP-TMPHYG-009" :implemented, "WTL-PRUNE-002" :implemented, "NS-SPLIT-064" :implemented, "MCP-OP-EDIT-037" :implemented, "MCP-OP-HOTVER-001" :implemented, "MCP-OP-POS-AUTH-004" :implemented, "MCP-OP-ADMIT-100" :implemented, "NS-SPLIT-049" :implemented, "MCP-OP-TMPHYG-011" :implemented, "MCP-OP-ADMIT-120" :implemented, "MCP-OP-ADMIT-060" :implemented, "WTL-HAND-003" :implemented, "MCP-OP-THREAD-001" :implemented, "MCP-OP-EDIT-023" :implemented, "MCP-OP-CENSUS-030" :implemented, "MCP-OP-RESULT-003" :implemented, "INSERT-FORMS-019" :implemented, "MCP-OP-ADMIT-033" :implemented, "WTL-APPLY-004" :implemented, "MCP-OP-THREAD-008" :implemented, "MCP-OP-ADMIT-112" :implemented, "MCP-OP-THREAD-015" :implemented, "MCP-OP-EDIT-016" :implemented, "TEST-ISO-009" :implemented, "MCP-OP-ALIAS-028" :implemented, "WTL-PRUNE-010" :implemented, "MCP-OP-ALIAS-039" :implemented, "RECEIPT-BOOL-001" :implemented, "MCP-OP-ADMIT-081" :implemented, "MCP-OP-CENSUS-003" :implemented, "MCP-OP-CENSUS-019" :implemented, "MCP-OP-ALIAS-067" :implemented, "MCP-OP-CENSUS-020" :implemented, "RENAME-ALIAS-008" :implemented, "WTL-SEAL-008" :deferred, "MCP-OP-ADMIT-062" :implemented, "REQUIRE-CHANGE-008" :implemented, "MCP-OP-FIELD-002" :implemented, "MCP-OP-READ-NORM-004" :active-gap, "MCP-OP-THREAD-041" :implemented, "MCP-OP-THREAD-011" :implemented, "MCP-OP-TMPHYG-003" :implemented, "MCP-OP-MEM-015" :implemented, "MCP-OP-THREAD-051" :implemented, "OP-ALG-COMPILE-001" :implemented, "MCP-OP-ALIAS-030" :implemented, "BB-PROBE-001" :implemented, "OP-ALG-EXPAND-001" :deferred, "MCP-OP-DISPATCH-005" :implemented, "MCP-OP-POS-AUTH-010" :implemented, "MCP-OP-THREAD-038" :implemented, "MCP-OP-EDIT-011" :implemented, "MCP-OP-ADMIT-004" :implemented, "NS-SPLIT-013" :implemented, "MCP-OP-ASYNC-001" :implemented, "MCP-OP-ADMIT-070" :implemented, "MCP-OP-ALIAS-022" :implemented, "MCP-OP-ADMIT-142" :implemented, "TEST-ISO-009b" :implemented, "MCP-OP-PREP-ACT-013" :active-gap, "MCP-OP-ALIAS-005" :implemented, "NS-SPLIT-005" :implemented, "NS-SPLIT-004" :implemented, "MCP-OP-HELPER-007" :active-gap, "MCP-OP-HELPER-005" :active-gap, "NS-SPLIT-055" :implemented, "MCP-OP-READ-CONT-001" :implemented, "MCP-OP-ALIAS-023" :implemented, "MCP-OP-THREAD-035" :implemented, "PERF-SENT-SCHEDULE-003" :active-gap, "MCP-OP-INSERT-010" :implemented, "CLI-PACKAGE-001" :implemented, "MCP-OP-ALIAS-038" :implemented, "RENAME-ALIAS-012" :implemented, "INSERT-FORMS-018" :implemented, "MCP-OP-ADMIT-103" :implemented, "OP-ALG-EFFECT-004" :implemented, "MCP-OP-CENSUS-006" :implemented, "INSERT-FORMS-016" :implemented, "MCP-OP-THREAD-014" :implemented, "SPLIT-REPAIR-002" :implemented, "MCP-OP-HELPER-013" :active-gap, "MCP-OP-DISPATCH-001" :implemented, "INSERT-FORMS-023" :implemented, "MCP-OP-ADMIT-125" :implemented, "OP-ALG-DECODE-001" :implemented, "INSERT-FORMS-021" :implemented, "MCP-OP-ANALYZER-004" :implemented, "MCP-OP-THREAD-023" :implemented, "NS-SPLIT-036" :implemented, "OP-ALG-COMMIT-001" :implemented, "MCP-OP-ADMIT-098" :implemented, "MCP-OP-ALIAS-050" :implemented, "MCP-OP-ANALYZER-003" :implemented, "PERF-SENT-BASELINE-001" :active-gap, "MCP-OP-ALIAS-026" :implemented, "MCP-OP-PREP-ACT-003" :active-gap, "TEST-ISO-015" :implemented, "WTL-APPLY-009" :implemented, "WTL-PRUNE-004" :implemented, "MCP-OP-ALIAS-061" :implemented, "MCP-OP-FIELD-008" :implemented, "PERF-SENT-AUTH-002" :active-gap, "MCP-OP-THREAD-031" :implemented, "MCP-OP-THREAD-019" :implemented, "MCP-OP-ADMIT-096" :implemented, "MCP-OP-RELAY-002" :implemented, "WTL-HAND-001" :implemented, "MCP-OP-CENSUS-018" :implemented, "PERF-SENT-CUTOVER-003" :active-gap, "MCP-OP-WRITE-REFUSAL-001" :implemented, "NS-SPLIT-065" :implemented, "MCP-OP-HELPER-004" :active-gap, "OP-ALG-OUTCOME-003" :implemented, "TEST-ISO-016" :implemented, "MCP-OP-ALIAS-007" :implemented, "OP-ALG-CLI-001" :implemented, "MCP-OP-THREAD-032" :implemented, "MCP-OP-ADMIT-119" :implemented, "MCP-OP-TMPHYG-006" :implemented, "MCP-OP-HELPER-016" :active-gap, "MCP-OP-ALIAS-060" :implemented, "TEST-ISO-001c" :implemented, "MCP-OP-PLAN-002" :implemented, "WTL-INV-007" :implemented, "MCP-OP-ADMIT-024" :implemented, "NS-SPLIT-034" :implemented, "MCP-OP-WRITE-REFUSAL-008" :deferred, "PERF-SENT-RETAIN-001" :active-gap, "MCP-OP-TMPHYG-004" :implemented, "MCP-OP-THREAD-045" :implemented, "NS-SPLIT-045" :implemented, "REQUIRE-CHANGE-011" :implemented, "MCP-OP-THREAD-040" :implemented, "MCP-OP-RELAY-001" :implemented, "MCP-OP-CENSUS-004" :implemented, "MCP-OP-SHELL-ARGV-005" :implemented, "MCP-OP-CENSUS-023" :implemented, "PERF-SENT-CLIENT-001" :active-gap, "MCP-OP-ADMIT-064" :implemented, "NS-SPLIT-033" :implemented, "MCP-OP-READ-DIAG-001" :implemented, "INSERT-FORMS-015" :implemented, "NS-SPLIT-038" :implemented, "MCP-OP-THREAD-020" :implemented, "MCP-OP-WRITE-REFUSAL-004" :deferred, "MCP-OP-TMPHYG-001" :implemented, "MCP-OP-READ-PARITY-001" :implemented, "MCP-OP-ADMIT-012" :implemented, "ROUTING-FANOUT-001" :implemented, "MCP-OP-EDIT-038" :implemented, "INSERT-FORMS-013" :implemented, "RENAME-ALIAS-004" :implemented, "MCP-OP-HELPER-012" :active-gap, "NS-SPLIT-059" :implemented, "MCP-OP-ANALYZER-007" :implemented, "MCP-OP-PREP-REQ-004" :implemented, "MCP-OP-CENSUS-021" :implemented, "MCP-OP-THREAD-033" :implemented, "WTL-SEAL-004" :implemented, "PERF-SENT-RECEIPT-001" :active-gap, "RENAME-ALIAS-009" :implemented, "MCP-OP-ALIAS-024" :implemented, "MCP-OP-INSERT-001" :active-gap, "MCP-OP-POS-AUTH-001" :implemented, "MCP-OP-VERIFY-001" :implemented, "MCP-OP-ALIAS-041" :implemented, "NS-SPLIT-002" :implemented, "PERF-SENT-INVALID-001" :active-gap, "MCP-OP-TRACE-001" :implemented, "REQUIRE-CHANGE-009" :implemented, "MCP-OP-ALIAS-025" :implemented, "MCP-OP-ADMIT-094" :implemented, "RENAME-ALIAS-011" :implemented, "MCP-OP-THREAD-050" :implemented, "INSERT-FORMS-005" :implemented, "OP-ALG-OUTCOME-002" :implemented, "REQUIRE-CHANGE-003" :implemented, "MCP-OP-ADMIT-063" :implemented, "WTL-APPLY-010" :implemented, "MCP-OP-ALIAS-029" :implemented, "MCP-OP-THREAD-052" :implemented, "MCP-OP-MEM-001" :deferred, "MCP-OP-ALIAS-018" :implemented, "PERF-SENT-RECOVERY-002" :active-gap, "MCP-OP-ADMIT-050" :implemented, "MCP-OP-ADMIT-137" :implemented, "MCP-OP-ADMIT-053" :implemented, "MCP-OP-EDIT-003" :implemented, "OP-ALG-REFUSE-001" :implemented, "MCP-OP-ADMIT-132" :implemented, "OP-ALG-CLI-002" :deferred, "PERF-SENT-IDENT-001" :active-gap, "PERF-SENT-SCHEDULE-001" :active-gap, "MCP-OP-ADMIT-067" :implemented, "MCP-OP-ADMIT-090" :implemented, "MCP-OP-CENSUS-022" :implemented, "INSERT-FORMS-022" :implemented, "MCP-OP-TMPHYG-005" :implemented, "MCP-OP-HELPER-003" :active-gap, "MCP-OP-SHELL-ARGV-003" :implemented, "OP-ALG-EFFECT-001" :implemented, "INSERT-FORMS-001" :implemented, "INSERT-FORMS-007" :implemented, "MCP-OP-ALIAS-001" :implemented, "MCP-OP-ALIAS-006" :implemented, "MCP-OP-CENSUS-013" :implemented, "NS-SPLIT-007" :implemented, "MCP-OP-THREAD-027" :implemented, "MCP-OP-ADMIT-113" :implemented, "SPLIT-REPAIR-001" :implemented, "MCP-OP-THREAD-007" :implemented, "MCP-OP-ADMIT-116" :implemented, "MCP-OP-PREP-ACT-004" :active-gap, "MCP-OP-PLAN-009" :implemented, "MCP-OP-ALIAS-020" :implemented, "MCP-OP-THREAD-048" :implemented, "MEASURE-WALL-002" :active-gap, "MCP-OP-ADMIT-005" :implemented, "MCP-OP-PREP-REQ-003" :implemented, "MCP-OP-READ-AUTH-001" :deferred, "PERF-SENT-PROMOTION-002" :active-gap, "MCP-OP-SHELL-ARGV-001" :implemented, "MCP-OP-TMPHYG-008" :implemented, "MCP-OP-PREP-REQ-006" :implemented, "REQUIRE-CHANGE-010" :implemented, "MCP-OP-ADMIT-109" :implemented, "TEST-ISO-008" :active-gap, "PERF-SENT-SCHEDULE-004" :active-gap, "MCP-OP-EDIT-022" :implemented, "MCP-OP-ADMIT-127" :implemented, "WTL-SEAL-005" :implemented, "MCP-OP-TMPHYG-007" :implemented, "MCP-OP-ADMIT-082" :implemented, "WTL-HAND-004" :implemented, "MCP-OP-RELAY-003" :implemented, "NS-SPLIT-067" :implemented, "MCP-OP-WRITE-REFUSAL-007" :deferred, "OP-ALG-COMMIT-004" :implemented, "NS-SPLIT-032" :implemented, "MCP-OP-EDIT-041" :implemented, "INSERT-FORMS-004" :implemented, "MCP-OP-THREAD-013" :implemented, "RENAME-ALIAS-005" :implemented, "PERF-SENT-IMPORT-002" :active-gap, "MCP-OP-EDIT-040" :implemented, "MCP-OP-ALIAS-033" :implemented, "MCP-OP-ADMIT-003" :implemented, "NS-SPLIT-048" :implemented, "PERF-SENT-BACKFILL-001" :active-gap, "MCP-OP-POS-AUTH-002" :implemented, "ALIAS-MIGRATION-004" :implemented, "MCP-OP-MEM-006" :implemented, "MCP-OP-ADMIT-089" :implemented, "MCP-OP-ADMIT-136" :implemented, "MCP-OP-INSERT-002" :active-gap, "PERF-SENT-LEDGER-002" :active-gap, "TEST-ISO-011" :active-gap, "MCP-OP-FIELD-001" :implemented, "MCP-OP-POS-AUTH-005" :implemented, "OP-ALG-FORM-COUNT-001" :implemented, "MCP-OP-MATCH-004" :implemented, "MCP-OP-ADMIT-071" :implemented, "OP-ALG-VERIFY-001" :deferred, "MCP-OP-THREAD-026" :implemented, "MCP-OP-ALIAS-053" :implemented, "TEST-ISO-009a" :implemented, "ALIAS-MIGRATION-001" :implemented, "INSERT-FORMS-003" :implemented, "MCP-OP-EDIT-006" :implemented, "PERF-SENT-PUBLISH-002" :active-gap, "PERF-SENT-LEDGER-ROOT-001" :active-gap, "MCP-OP-CENSUS-009" :implemented, "MCP-OP-PLAN-003" :implemented, "MCP-OP-INSERT-005" :active-gap, "MCP-OP-INSERT-004" :active-gap, "MCP-OP-ADMIT-143" :implemented, "MCP-OP-EDIT-030" :implemented, "MCP-OP-ALIAS-062" :active-gap, "OP-ALG-CONTEXT-002" :implemented, "MCP-OP-TMPHYG-010" :implemented, "MCP-OP-THREAD-034" :implemented, "MCP-OP-READ-NORM-005" :active-gap, "MCP-OP-CENSUS-016" :implemented, "MCP-OP-EDIT-013" :implemented, "WTL-INV-006" :implemented, "MCP-OP-ADMIT-084" :implemented, "WTL-APPLY-003" :implemented, "INSERT-FORMS-014" :implemented, "MCP-OP-THREAD-009" :implemented, "PERF-SENT-PROMOTION-001" :active-gap, "NS-SPLIT-053" :implemented, "MCP-OP-INSERT-007" :implemented, "NS-SPLIT-044" :implemented, "NS-SPLIT-063" :implemented, "MCP-OP-MATCHED-003" :implemented, "MCP-OP-MEM-012" :implemented, "MCP-OP-ASYNC-005" :implemented, "REQUIRE-CHANGE-013" :implemented, "MCP-OP-ADMIT-130" :implemented, "MCP-OP-ADMIT-128" :implemented, "BB-PROBE-002" :implemented, "REQUIRE-CHANGE-001" :implemented, "MCP-OP-EDIT-025" :implemented, "MCP-OP-ADMIT-091" :implemented, "MCP-OP-THREAD-037" :implemented, "MCP-OP-HELPER-023" :active-gap, "NS-SPLIT-047" :implemented, "NS-SPLIT-051" :implemented, "MCP-OP-ADMIT-145" :implemented, "MCP-OP-HELPER-010" :active-gap, "MCP-OP-READ-NORM-003" :active-gap, "PERF-SENT-SURFACE-001" :active-gap, "MCP-OP-THREAD-044" :implemented, "MCP-OP-ALIAS-037" :implemented, "MCP-OP-HELPER-001" :active-gap, "MCP-OP-ADMIT-108" :implemented, "PERF-SENT-ATTEMPT-003" :active-gap, "MCP-OP-READ-HYP-001" :implemented, "PERF-SENT-ATTEMPT-001" :active-gap, "OP-ALG-RECEIPT-002" :implemented, "OP-ALG-EFFECT-003" :implemented, "NS-SPLIT-069" :implemented, "MCP-OP-TRACE-003" :implemented, "MCP-OP-VERIFY-013" :implemented, "MCP-OP-ADMIT-022" :implemented, "MCP-OP-ADMIT-118" :implemented, "MCP-OP-ADMIT-002" :implemented, "MCP-OP-RESULT-006" :implemented, "OP-ALG-VERIFY-002" :deferred, "MCP-OP-POS-AUTH-009" :implemented, "OP-ALG-SHADOW-001" :implemented, "MCP-OP-ADMIT-133" :implemented, "MCP-OP-ALIAS-021" :implemented, "MCP-OP-TMPHYG-013" :implemented, "MCP-OP-PREP-REQ-008" :implemented, "OP-ALG-CONTEXT-001" :implemented, "NS-SPLIT-012" :implemented, "MCP-OP-CENSUS-008" :implemented, "MCP-OP-THREAD-047" :implemented, "MCP-OP-CENSUS-001" :implemented, "MCP-OP-EDIT-019" :implemented, "MCP-OP-CENSUS-031" :implemented, "MCP-OP-ALIAS-049" :implemented, "NS-SPLIT-061" :implemented, "SPLIT-REPAIR-004" :implemented, "MCP-OP-PREP-ACT-010" :active-gap, "OP-ALG-PARITY-001" :implemented, "WTL-APPLY-008" :implemented, "MCP-OP-CENSUS-033" :implemented, "WTL-INV-004" :implemented, "INSERT-FORMS-009" :implemented, "PERF-SENT-RECEIPT-002" :active-gap, "WTL-PLAN-003" :implemented, "NS-SPLIT-054" :implemented, "PERF-SENT-ATTEMPT-002" :active-gap, "REQUIRE-CHANGE-012" :implemented, "MCP-OP-ALIAS-004" :implemented, "MCP-OP-ADMIT-080" :implemented, "MCP-OP-HELPER-006" :active-gap, "MCP-OP-ALIAS-003" :implemented, "MCP-OP-EDIT-039" :implemented, "TEST-ISO-003" :implemented, "MCP-OP-ALIAS-035" :implemented, "MCP-OP-THREAD-028" :implemented, "MCP-OP-TRACE-005" :implemented, "MCP-OP-THREAD-036" :implemented, "RENAME-ALIAS-006" :implemented, "MCP-OP-ADMIT-040" :implemented, "MCP-OP-ADMIT-092" :implemented, "REQUIRE-CHANGE-007" :implemented, "WTL-SEAL-002" :implemented, "MCP-OP-EDIT-014" :implemented, "SPLIT-REPAIR-003" :implemented, "MCP-OP-ADMIT-097" :implemented, "MCP-OP-ADMIT-055" :implemented, "MCP-OP-EDIT-002" :implemented, "MCP-OP-PREP-ACT-009" :active-gap, "MCP-OP-ADMIT-001" :implemented, "NS-SPLIT-040" :implemented, "MCP-OP-PLAN-005" :implemented, "NS-SPLIT-058" :implemented, "MCP-OP-COVERAGE-002" :implemented, "MCP-OP-ADMIT-126" :implemented, "OP-ALG-CATALOG-001" :implemented, "MCP-OP-ADMIT-146" :implemented, "PERF-SENT-IDENT-002" :active-gap, "MCP-OP-CENSUS-024" :implemented, "MCP-OP-ADMIT-104" :implemented, "MCP-OP-MATCH-002" :implemented, "MCP-OP-READ-CONT-002" :implemented, "MCP-OP-ADMIT-044" :implemented, "MCP-OP-ALIAS-058" :implemented, "WTL-APPLY-001" :implemented, "MCP-OP-ADMIT-122" :implemented, "MCP-OP-CENSUS-011" :implemented, "NS-SPLIT-070" :implemented, "RENAME-ALIAS-015" :implemented, "MCP-OP-ADMIT-052" :implemented, "WTL-PRUNE-007" :implemented, "MCP-OP-ALIAS-048" :implemented, "NS-SPLIT-039" :implemented, "MCP-OP-CENSUS-002" :implemented, "MCP-OP-ADMIT-101" :implemented, "MCP-OP-EDIT-021" :implemented, "MCP-OP-FIELD-009" :implemented, "MCP-OP-THREAD-024" :implemented, "PERF-SENT-VERDICT-001" :active-gap, "NS-SPLIT-066" :implemented, "MCP-OP-CENSUS-014" :implemented, "MCP-OP-HELPER-015" :active-gap, "MCP-OP-PREP-ACT-006" :active-gap, "MCP-OP-MEM-013" :implemented, "WTL-APPLY-011" :implemented, "TEST-ISO-004" :implemented, "WTL-CLI-001" :implemented, "MCP-OP-EDIT-036" :implemented, "RENAME-ALIAS-013" :implemented, "MCP-OP-ADMIT-115" :implemented, "WTL-PRUNE-009" :implemented, "OP-ALG-OUTCOME-001" :implemented, "MCP-OP-RELAY-005" :implemented, "MCP-OP-VERIFY-012" :implemented, "MCP-OP-PREP-ACT-012" :active-gap, "PERF-SENT-ADMIT-001" :active-gap, "MCP-OP-TRACE-004" :implemented, "MCP-OP-WRITE-REFUSAL-005" :deferred, "MCP-OP-THREAD-022" :implemented, "MCP-OP-FIELD-005" :implemented, "MCP-OP-ADMIT-123" :implemented, "MCP-OP-READ-RETRY-002" :deferred, "TEST-ISO-012" :active-gap, "NS-SPLIT-006" :implemented, "MCP-OP-TIME-001" :implemented, "MEASURE-WALL-001" :active-gap, "NS-SPLIT-028" :implemented, "MCP-OP-CENSUS-015" :implemented, "MCP-OP-EDIT-020" :implemented, "NS-SPLIT-068" :implemented, "MCP-OP-CENSUS-025" :implemented, "MCP-OP-ASYNC-004" :implemented, "MCP-OP-THREAD-004" :implemented, "WTL-INV-005" :implemented, "MCP-OP-HELPER-008" :active-gap, "OP-ALG-PARITY-002" :implemented, "INSERT-FORMS-002" :implemented, "MCP-OP-ALIAS-043" :implemented, "MCP-OP-ALIAS-052" :implemented, "MCP-OP-INSERT-008" :implemented, "MCP-OP-THREAD-002" :implemented, "MCP-OP-EDIT-017" :implemented, "NS-SPLIT-030" :implemented, "ROUTING-PARITY-001" :implemented, "WTL-SEAL-007" :implemented, "WTL-PLAN-006" :implemented, "MCP-OP-ALIAS-008" :implemented, "RENAME-ALIAS-002" :implemented, "WTL-INV-003" :implemented, "MCP-OP-ALIAS-012" :implemented, "MCP-OP-RESULT-001" :implemented, "MCP-OP-VERIFY-011" :implemented, "MCP-OP-POS-AUTH-008" :implemented, "WTL-CLI-002" :implemented, "NS-SPLIT-003" :implemented, "MCP-OP-ALIAS-034" :implemented, "MCP-OP-THREAD-003" :implemented, "MCP-OP-RESULT-004" :implemented, "MCP-OP-TIME-002" :implemented, "MCP-OP-ADMIT-087" :implemented, "MCP-OP-MATCH-001" :implemented, "MCP-OP-EDIT-010" :implemented, "MCP-OP-ADMIT-011" :implemented, "MCP-OP-ADMIT-083" :implemented, "MCP-OP-HELPER-024" :active-gap, "PERF-SENT-PUBLISH-001" :active-gap, "OP-ALG-IDENTITY-001" :implemented, "MCP-OP-PLAN-010" :implemented, "PERF-SENT-CUTOVER-001" :active-gap, "MCP-OP-HELPER-017" :active-gap, "MCP-OP-MATCH-003" :implemented, "WTL-SEAL-003" :implemented, "MCP-OP-PREP-REQ-002" :implemented, "TEST-ISO-006" :implemented, "MCP-OP-CENSUS-026" :implemented, "MCP-OP-READ-RETRY-001" :deferred, "WTL-INV-008" :implemented, "MCP-OP-ANALYZER-001" :implemented, "WTL-PRUNE-003" :implemented, "WTL-APPLY-005" :implemented, "MCP-OP-RELAY-004" :implemented, "NS-SPLIT-009" :implemented, "NS-SPLIT-010" :implemented, "MCP-OP-ADMIT-117" :implemented, "OP-ALG-PERF-002" :implemented, "TEST-ISO-013" :implemented, "MCP-OP-THREAD-016" :implemented, "MCP-OP-ALIAS-027" :implemented, "PERF-SENT-RECOVERY-001" :active-gap, "REQUIRE-CHANGE-006" :implemented, "MCP-OP-ADMIT-095" :implemented, "MCP-OP-ADMIT-010" :implemented, "MCP-OP-FIELD-006" :implemented, "ALIAS-MIGRATION-002" :implemented, "RENAME-ALIAS-016" :implemented, "MCP-OP-ADMIT-042" :implemented, "MCP-OP-ALIAS-045" :implemented, "MCP-OP-THREAD-025" :implemented, "MCP-OP-ADMIT-093" :implemented, "MCP-OP-ALIAS-056" :implemented, "MCP-OP-TMPHYG-002" :implemented, "MCP-OP-ADMIT-131" :implemented, "MCP-OP-VERIFY-002" :implemented, "WTL-APPLY-006" :implemented, "MCP-OP-MEM-014" :implemented, "MCP-OP-RESULT-002" :implemented, "MCP-OP-EDIT-029" :implemented, "WTL-CLI-004" :deferred, "MCP-OP-PLAN-008" :implemented, "MCP-OP-ADMIT-031" :implemented, "MCP-OP-ADMIT-129" :implemented, "OP-ALG-RECEIPT-003" :implemented, "MCP-OP-ADMIT-041" :implemented, "MCP-OP-CENSUS-007" :implemented, "MCP-OP-PREP-ACT-016" :active-gap, "MCP-OP-ADMIT-124" :implemented, "WTL-PLAN-005" :implemented, "MCP-OP-FIELD-007" :implemented, "TEST-ISO-007" :implemented, "MCP-OP-MEM-005" :implemented, "MCP-OP-ASYNC-003" :implemented, "MCP-OP-ADMIT-138" :implemented, "MCP-OP-INSERT-006" :active-gap, "PERF-SENT-LEDGER-001" :active-gap, "MCP-OP-ALIAS-010" :implemented, "MCP-OP-ADMIT-140" :implemented, "MCP-OP-ADMIT-043" :implemented, "OP-ALG-RECEIPT-001" :implemented, "PERF-SENT-RECONCILE-001" :active-gap, "MCP-OP-ALIAS-066" :active-gap, "PERF-SENT-RECOVERY-004" :active-gap, "MCP-OP-THREAD-010" :implemented, "PERF-SENT-RECOVERY-003" :active-gap, "PERF-SENT-THRESHOLD-001" :active-gap, "MCP-OP-ADMIT-085" :implemented, "MCP-OP-THREAD-017" :implemented, "WTL-PLAN-001" :implemented, "MCP-OP-CENSUS-032" :implemented, "NS-SPLIT-050" :implemented, "MCP-OP-ANALYZER-005" :implemented, "OP-ALG-PREVIEW-001" :implemented, "WTL-PLAN-002" :implemented, "MCP-OP-READ-DIAG-002" :implemented, "MCP-OP-ADMIT-086" :implemented, "MCP-OP-HELPER-025" :active-gap, "MCP-OP-ADMIT-134" :implemented, "OP-ALG-PERF-001" :implemented, "MCP-OP-ALIAS-031" :implemented, "OP-ALG-MCP-001" :implemented, "TEST-ISO-RACE-001" :implemented, "MEASURE-WALL-003" :active-gap, "MCP-OP-READ-DIAG-003" :implemented, "MCP-OP-EDIT-005" :implemented, "MCP-OP-ADMIT-014" :deferred, "MCP-OP-ADMIT-102" :implemented, "TEST-ISO-001b" :implemented, "MCP-OP-PLAN-006" :implemented, "MCP-OP-THREAD-043" :implemented, "PERF-SENT-INVALID-002" :active-gap, "REQUIRE-CHANGE-014" :implemented, "INSERT-FORMS-024" :implemented, "WTL-PRUNE-006" :implemented, "PERF-SENT-ADMIT-002" :active-gap, "MCP-OP-PLAN-007" :implemented, "BB-PROBE-003" :implemented, "MCP-OP-THREAD-049" :implemented, "MCP-OP-ALIAS-042" :implemented, "MCP-OP-THREAD-005" :implemented, "OP-ALG-COMMIT-002" :implemented, "MCP-OP-ADMIT-114" :implemented, "TEST-ISO-005" :implemented, "MCP-OP-ADMIT-013" :implemented, "MCP-OP-TIME-004" :implemented, "MCP-OP-THREAD-030" :implemented, "MCP-OP-ALIAS-011" :implemented, "MCP-OP-PREP-ACT-011" :active-gap, "MCP-OP-POS-AUTH-006" :implemented, "CLI-PACKAGE-002" :implemented, "MCP-OP-PREP-REQ-001" :implemented, "MCP-OP-ALIAS-064" :active-gap, "MCP-OP-ADMIT-054" :implemented, "NS-SPLIT-011" :implemented, "MCP-OP-READ-NORM-002" :active-gap, "MCP-OP-EDIT-042" :implemented, "MCP-OP-PREP-ACT-008" :active-gap, "MCP-OP-ORACLE-001" :implemented, "WTL-APPLY-012" :implemented, "MCP-OP-EDIT-031" :implemented, "NS-SPLIT-014" :implemented, "MCP-OP-EDIT-026" :implemented, "MCP-OP-PREP-REQ-009" :implemented, "ROUTING-SPLIT-001" :implemented, "MCP-OP-SHELL-ARGV-004" :implemented, "MCP-OP-ADMIT-068" :implemented, "NS-SPLIT-037" :implemented}, :implementation-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "OP-ALG-RUNTIME-001" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "TEST-ISO-009" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "OP-ALG-OUTCOME-002" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "OP-ALG-CONTEXT-002" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "MCP-OP-ADMIT-084" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "MCP-OP-PREP-REQ-002" "MCP-OP-CENSUS-026" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "OP-ALG-RECEIPT-003" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "OP-ALG-MCP-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "OP-ALG-COMMIT-002" "MCP-OP-ADMIT-114" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :test-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "WTL-APPLY-007" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "OP-ALG-EFFECT-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "MCP-OP-ADMIT-152" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-ADMIT-147" "TEST-ISO-RACE-002" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "WTL-SEAL-006" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "WTL-HAND-005" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "WTL-APPLY-002" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "WTL-CLI-003" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "MCP-OP-ANALYZER-006" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "WTL-HAND-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "OP-ALG-STALE-001" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "WTL-INV-002" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "TEST-ISO-010" "WTL-PLAN-004" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "WTL-APPLY-004" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "WTL-SEAL-004" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "WTL-APPLY-010" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "WTL-INV-006" "MCP-OP-ADMIT-084" "WTL-APPLY-003" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "MCP-OP-HELPER-023" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "OP-ALG-EFFECT-003" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "OP-ALG-SHADOW-001" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "WTL-APPLY-008" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "WTL-PLAN-003" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "TEST-ISO-003" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "WTL-SEAL-002" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-APPLY-011" "TEST-ISO-004" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "OP-ALG-OUTCOME-001" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "WTL-INV-005" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "WTL-SEAL-007" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "WTL-INV-003" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "WTL-CLI-002" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "WTL-SEAL-003" "MCP-OP-PREP-REQ-002" "TEST-ISO-006" "MCP-OP-CENSUS-026" "WTL-INV-008" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "WTL-APPLY-006" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "TEST-ISO-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "TEST-ISO-RACE-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "MCP-OP-ADMIT-114" "TEST-ISO-005" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ALIAS-064" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "WTL-APPLY-012" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :violations [], :pending-witness-violations [{:type :missing-test-witness, :intent "MEASURE-EVID-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-002", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-003", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-COMMIT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-CONTEXT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-DECODE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-DECODE-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-EFFECT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-MCP-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-REFUSE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-REFUSE-001", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RUNTIME-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-SHADOW-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-STALE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BACKFILL-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BASELINE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CLIENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CONFIG-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-ROOT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROJECT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECONCILE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RELEASE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RETAIN-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SURFACE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-THRESHOLD-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-TIME-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001a", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001a", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001b", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001b", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001c", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001c", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-007", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-008", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-009", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-010", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-011", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-012", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-007", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-010", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-011", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-012", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-004", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-HAND-004", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-HAND-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-002", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-002", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PLAN-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-006", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-006", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-008", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-008", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-009", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-009", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-SEAL-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-007", :source-kind :implementation}], :witness-debt-ledger {"WTL-APPLY-007" #{:implementation}, "PERF-SENT-IMPORT-001" #{:test}, "OP-ALG-EFFECT-002" #{:implementation}, "TEST-ISO-RACE-002" #{:implementation}, "OP-ALG-PREVIEW-002" #{:implementation :test}, "PERF-SENT-VERDICT-002" #{:test}, "PERF-SENT-AUTH-003" #{:test}, "PERF-SENT-VERDICT-003" #{:test}, "PERF-SENT-PROJECT-001" #{:test}, "TEST-ISO-001a" #{:implementation :test}, "PERF-SENT-LEDGER-004" #{:test}, "WTL-SEAL-006" #{:implementation}, "PERF-SENT-RELEASE-001" #{:test}, "WTL-HAND-005" #{:implementation}, "PERF-SENT-TIME-001" #{:test}, "MEASURE-EVID-001" #{:test}, "WTL-APPLY-002" #{:implementation}, "PERF-SENT-SCHEDULE-002" #{:test}, "PERF-SENT-CONFIG-001" #{:test}, "WTL-CLI-003" #{:implementation}, "OP-ALG-RUNTIME-001" #{:test}, "WTL-HAND-002" #{:implementation}, "PERF-SENT-CUTOVER-002" #{:test}, "OP-ALG-STALE-001" #{:implementation}, "WTL-INV-002" #{:implementation}, "PERF-SENT-AUTH-001" #{:test}, "TEST-ISO-010" #{:implementation}, "WTL-PLAN-004" #{:implementation}, "WTL-PRUNE-008" #{:implementation :test}, "PERF-SENT-LEDGER-003" #{:test}, "WTL-APPLY-004" #{:implementation}, "TEST-ISO-009" #{:test}, "PERF-SENT-SCHEDULE-003" #{:test}, "OP-ALG-EFFECT-004" #{:implementation :test}, "OP-ALG-DECODE-001" #{:implementation :test}, "PERF-SENT-BASELINE-001" #{:test}, "PERF-SENT-AUTH-002" #{:test}, "PERF-SENT-CUTOVER-003" #{:test}, "OP-ALG-OUTCOME-003" #{:implementation :test}, "TEST-ISO-001c" #{:implementation :test}, "PERF-SENT-RETAIN-001" #{:test}, "PERF-SENT-CLIENT-001" #{:test}, "WTL-SEAL-004" #{:implementation}, "PERF-SENT-RECEIPT-001" #{:test}, "PERF-SENT-INVALID-001" #{:test}, "OP-ALG-OUTCOME-002" #{:test}, "WTL-APPLY-010" #{:implementation}, "PERF-SENT-RECOVERY-002" #{:test}, "OP-ALG-REFUSE-001" #{:implementation :test}, "PERF-SENT-IDENT-001" #{:test}, "PERF-SENT-SCHEDULE-001" #{:test}, "MEASURE-WALL-002" #{:test}, "PERF-SENT-PROMOTION-002" #{:test}, "TEST-ISO-008" #{:test}, "PERF-SENT-SCHEDULE-004" #{:test}, "WTL-HAND-004" #{:implementation :test}, "OP-ALG-COMMIT-004" #{:implementation :test}, "PERF-SENT-IMPORT-002" #{:test}, "PERF-SENT-BACKFILL-001" #{:test}, "PERF-SENT-LEDGER-002" #{:test}, "TEST-ISO-011" #{:test}, "PERF-SENT-PUBLISH-002" #{:test}, "PERF-SENT-LEDGER-ROOT-001" #{:test}, "OP-ALG-CONTEXT-002" #{:test}, "WTL-INV-006" #{:implementation}, "WTL-APPLY-003" #{:implementation}, "PERF-SENT-PROMOTION-001" #{:test}, "PERF-SENT-SURFACE-001" #{:test}, "PERF-SENT-ATTEMPT-003" #{:test}, "PERF-SENT-ATTEMPT-001" #{:test}, "OP-ALG-RECEIPT-002" #{:implementation :test}, "OP-ALG-EFFECT-003" #{:implementation}, "OP-ALG-SHADOW-001" #{:implementation}, "OP-ALG-PARITY-001" #{:implementation :test}, "WTL-APPLY-008" #{:implementation}, "PERF-SENT-RECEIPT-002" #{:test}, "WTL-PLAN-003" #{:implementation}, "PERF-SENT-ATTEMPT-002" #{:test}, "TEST-ISO-003" #{:implementation}, "WTL-SEAL-002" #{:implementation}, "PERF-SENT-IDENT-002" #{:test}, "PERF-SENT-VERDICT-001" #{:test}, "WTL-APPLY-011" #{:implementation}, "TEST-ISO-004" #{:implementation}, "WTL-PRUNE-009" #{:implementation :test}, "OP-ALG-OUTCOME-001" #{:implementation}, "PERF-SENT-ADMIT-001" #{:test}, "TEST-ISO-012" #{:test}, "MEASURE-WALL-001" #{:test}, "WTL-INV-005" #{:implementation}, "OP-ALG-PARITY-002" #{:implementation :test}, "WTL-SEAL-007" #{:implementation}, "WTL-PLAN-006" #{:implementation :test}, "WTL-INV-003" #{:implementation}, "WTL-CLI-002" #{:implementation}, "PERF-SENT-PUBLISH-001" #{:test}, "PERF-SENT-CUTOVER-001" #{:test}, "WTL-SEAL-003" #{:implementation}, "TEST-ISO-006" #{:implementation}, "WTL-INV-008" #{:implementation}, "OP-ALG-PERF-002" #{:implementation :test}, "PERF-SENT-RECOVERY-001" #{:test}, "WTL-APPLY-006" #{:implementation}, "OP-ALG-RECEIPT-003" #{:test}, "TEST-ISO-007" #{:implementation}, "PERF-SENT-LEDGER-001" #{:test}, "OP-ALG-RECEIPT-001" #{:implementation :test}, "PERF-SENT-RECONCILE-001" #{:test}, "PERF-SENT-RECOVERY-004" #{:test}, "PERF-SENT-RECOVERY-003" #{:test}, "PERF-SENT-THRESHOLD-001" #{:test}, "OP-ALG-PREVIEW-001" #{:implementation :test}, "WTL-PLAN-002" #{:implementation :test}, "OP-ALG-PERF-001" #{:implementation :test}, "OP-ALG-MCP-001" #{:test}, "TEST-ISO-RACE-001" #{:implementation}, "MEASURE-WALL-003" #{:test}, "TEST-ISO-001b" #{:implementation :test}, "PERF-SENT-INVALID-002" #{:test}, "PERF-SENT-ADMIT-002" #{:test}, "OP-ALG-COMMIT-002" #{:test}, "TEST-ISO-005" #{:implementation}, "WTL-APPLY-012" #{:implementation}}}
gate-stage: {:target "intent-audit", :exit 0, :wall-ms 2163}
landing-gate: {:pool {:target "runtime-pool", :exit 0, :wall-ms 156307, :phases [{:phase 0, :process-count 14, :wall-ms 25570} {:phase 1, :process-count 23, :wall-ms 130685}], :preparation {:command ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"], :exit 0, :wall-ms 51}, :peak-worker-count 8, :shell-checks {:namespaces [], :index 0, :exit 0, :started-ms 1789232582121, :phase 1, :err-log "target/gate-prewarm/489d4c5b-bd8c-4f91-99fa-bd7334996dbf/lane-0.err", :runtime nil, :wall-ms 130684, :suite "shell", :completed-ms 1789232712806, :log "target/gate-prewarm/489d4c5b-bd8c-4f91-99fa-bd7334996dbf/lane-0.out"}}, :stages [{:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11172} {:target "alias-migration-test", :exit 0, :wall-ms 91200} {:target "mcp-test", :exit 0, :wall-ms 91581} {:target "test-bb", :exit 0, :wall-ms 91494} {:target "test-bb-diagnostic", :exit 0, :wall-ms 224369} {:target "repository-hygiene", :exit 0, :wall-ms 2021} {:target "intent-audit", :exit 0, :wall-ms 2163}], :landing? false, :problems [], :capacity {:lanes 8, :memory-mib 25272, :admission :abstract-socket, :lane-charge-mib 1536, :reserve-mib 2048, :slot-root "abstract", :floor-mib 3584, :heap-mib 512, :cpus 16}, :started-at "2026-09-12T17:02:24.988490794Z", :state :passed, :git-tree "a3aa5e7eea0c9e0fc572abc74a7c8cf2653dd1b8", :wall-ms 396675, :completed-at "2026-09-12T17:09:01.664330754Z", :source-digest "23390c9bfa92b3095f591e3d6473f4b11057cde9cd8c6cb4ad087dcaeeebe297", :prewarm? true, :run-id "489d4c5b-bd8c-4f91-99fa-bd7334996dbf", :git-head "09588a14a784e220907462f46676900402bc80aa"}
{"exit": 0, "wall_seconds": 398.8831995828077}
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 25286 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T17:11:09.280190027Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=133
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=105
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=186
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11019}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 78737 ms
  process 0 phase 0 (est 4473 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 78737 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 47539 ms
  process 0 phase 0 (est 3596 ms): clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test
  process 1 phase 0 (est 1393 ms): clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.cljc-existing-ops-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.mission-plain-forms-test clj-surgeon.forms-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-telemetry-test
  process 2 phase 0 (est 3494 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.jvm-error-test clj-surgeon.edit-dsl-test clj-surgeon.require-change-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-forms-test
  process 3 phase 0 (est 1495 ms): clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-semantic-client-test
  process 4 phase 0 (est 2612 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test
  process 5 phase 0 (est 2377 ms): clj-surgeon.helper-extraction-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.recovery-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test
  process 6 phase 0 (est 4772 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-test
  process 7 phase 0 (est 218 ms): clj-surgeon.ls-tree-test clj-surgeon.mcp-extraction-test clj-surgeon.move-test clj-surgeon.worktree-lifecycle-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test
  process 8 phase 0 (est 4780 ms): clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  process 9 phase 0 (est 209 ms): clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.split-proof-gate-test clj-surgeon.mcp-read-request-normalization-test
  process 10 phase 0 (est 4709 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-compact-edit-test
  process 11 phase 0 (est 280 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 12 phase 0 (est 4953 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 37 ms): clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mission-candidate-test
  process 14 phase 0 (est 8686 ms): clj-surgeon.mcp-compact-relations-test
  process 15 phase 1 (est 256 ms): clj-surgeon.mcp-server-test
  process 16 phase 1 (est 1470 ms): clj-surgeon.namespace-split-warm-test
  process 17 phase 1 (est 2216 ms): clj-surgeon.mcp-hot-verify-test
  process 18 phase 1 (est 2889 ms): clj-surgeon.mcp-http-server-test
  process 19 phase 1 (est 3235 ms): clj-surgeon.mcp-tool-test
  process 20 phase 1 (est 8983 ms): clj-surgeon.outline-corpus-integration-test
  process 21 phase 1 (est 47539 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 91320 ms
  process 0 phase 0 (est 11919 ms): clj-surgeon.edit-test clj-surgeon.extract-test
  process 1 phase 0 (est 2035 ms): clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.insertion-gap-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test
  process 2 phase 0 (est 6511 ms): clj-surgeon.intent-transaction-test
  process 3 phase 0 (est 6456 ms): clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  process 4 phase 0 (est 987 ms): clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.forms-test
  process 5 phase 0 (est 13688 ms): clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  process 6 phase 0 (est 266 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test
  process 7 phase 0 (est 13715 ms): clj-surgeon.show-form-test clj-surgeon.platform-selector-test
  process 8 phase 0 (est 239 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.split-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 9 phase 0 (est 14009 ms): clj-surgeon.tmp-leak-support-test
  process 10 phase 0 (est 14255 ms): clj-surgeon.cli-dispatch-test
  process 11 phase 0 (est 72153 ms): clj-surgeon.parser-admission-test
  process 12 phase 0 (est 91320 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 14048 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4992 ms):
      4992 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 87958 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 75661 ms):
     75661 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3648 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 87959 ms
cadence :battery: serial-equivalent 80653 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 80653 ms):
     75661 ms  clj-surgeon.mcp-alias-migration-test
      4992 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 87959 ms:
  lane 0     14048 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     87958 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 87959 ms over 2 lane(s); serial-equivalent 80653 ms; skipped 0

========== lane 0 (clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test) exit 0, 23812 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 3330 ms):
      1426 ms  clj-surgeon.ns-isolation-test
       989 ms  clj-surgeon.splice-envelope-test
       414 ms  clj-surgeon.receipt-booleans-test
       348 ms  clj-surgeon.mcp-expect-guard-test
       153 ms  clj-surgeon.mcp-formatter-test

========== lane 1 (clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.cljc-existing-ops-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.mission-plain-forms-test clj-surgeon.forms-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-telemetry-test) exit 0, 1990 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mcp-telemetry-test

========== lane 2 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.jvm-error-test clj-surgeon.edit-dsl-test clj-surgeon.require-change-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-forms-test) exit 0, 3889 ms ==========

Testing clj-surgeon.lane-manifest-test

Testing clj-surgeon.outline-test

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.mission-forms-test

========== lane 3 (clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-semantic-client-test) exit 0, 17039 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 3 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 1339 ms):
       522 ms  clj-surgeon.mcp-compact-location-test
       300 ms  clj-surgeon.mcp-create-files-test
       211 ms  clj-surgeon.mcp-prepared-confirmation-test
       169 ms  clj-surgeon.mcp-write-refusal-test
       137 ms  clj-surgeon.mcp-semantic-client-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test) exit 0, 19567 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 3478 ms):
      2297 ms  clj-surgeon.mcp-inspect-tool-test
       637 ms  clj-surgeon.mcp-change-buffer-test
       335 ms  clj-surgeon.mcp-prepared-request-test
       209 ms  clj-surgeon.mcp-operation-async-test

========== lane 5 (clj-surgeon.helper-extraction-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.recovery-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test) exit 0, 3394 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.cljc.analyze-test

========== lane 6 (clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-test) exit 0, 23491 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-operation-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 5803 ms):
      4158 ms  clj-surgeon.mcp-namespace-split-test
       684 ms  clj-surgeon.insert-forms-receipt-test
       345 ms  clj-surgeon.outline-memory-test
       306 ms  clj-surgeon.census-pool-test
       167 ms  clj-surgeon.mcp-extraction-plan-test
       143 ms  clj-surgeon.mcp-operation-test

========== lane 7 (clj-surgeon.ls-tree-test clj-surgeon.mcp-extraction-test clj-surgeon.move-test clj-surgeon.worktree-lifecycle-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test) exit 0, 725 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.move-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.mission-usage-test

========== lane 8 (clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test) exit 0, 23192 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.mcp-combinable-transaction-test

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

Testing clj-surgeon.mission-candidate-race-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 5046 ms):
      3479 ms  clj-surgeon.insert-forms-test
       663 ms  clj-surgeon.rename-alias-receipt-test
       342 ms  clj-surgeon.scope-stream-test
       238 ms  clj-surgeon.mcp-combinable-transaction-test
       175 ms  clj-surgeon.mcp-relation-census-round20-test
       149 ms  clj-surgeon.mission-candidate-race-test

========== lane 9 (clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.split-proof-gate-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 631 ms ==========

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.split-proof-gate-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-compact-edit-test) exit 0, 24563 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (2, slowest first, total 4634 ms):
      4444 ms  clj-surgeon.mcp-operation-registry-test
       190 ms  clj-surgeon.mcp-compact-edit-test

========== lane 11 (clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 888 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 18264 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5153 ms):
      5153 ms  clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mission-candidate-test) exit 0, 339 ms ==========

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.mcp-compact-edit-fields-test

Testing clj-surgeon.mission-candidate-test

========== lane 14 (clj-surgeon.mcp-compact-relations-test) exit 0, 25835 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8400 ms):
      8400 ms  clj-surgeon.mcp-compact-relations-test

========== lane 15 (clj-surgeon.mcp-server-test) exit 0, 10767 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 39189 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3619420-a4b45fc4/clj-surgeon-mcp-server-test-7093637831522475821/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 42853 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3619420-a4b45fc4/clj-surgeon-mcp-server-test-16699799675018664470/.nrepl-port )

namespace walls (1, slowest first, total 248 ms):
       248 ms  clj-surgeon.mcp-server-test

========== lane 16 (clj-surgeon.namespace-split-warm-test) exit 0, 9895 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1432 ms):
      1432 ms  clj-surgeon.namespace-split-warm-test

========== lane 17 (clj-surgeon.mcp-hot-verify-test) exit 0, 7162 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2153 ms):
      2153 ms  clj-surgeon.mcp-hot-verify-test

========== lane 18 (clj-surgeon.mcp-http-server-test) exit 0, 16307 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 35439 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3610517-4a82dd85/clj-surgeon-mcp-http-test-8846313065481883932/.nrepl-port )

namespace walls (1, slowest first, total 2937 ms):
      2937 ms  clj-surgeon.mcp-http-server-test

========== lane 19 (clj-surgeon.mcp-tool-test) exit 0, 13745 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2988 ms):
      2988 ms  clj-surgeon.mcp-tool-test

========== lane 20 (clj-surgeon.outline-corpus-integration-test) exit 0, 15276 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9232 ms):
      9232 ms  clj-surgeon.outline-corpus-integration-test

========== lane 21 (clj-surgeon.mcp-feature-thread-test) exit 0, 55374 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 21 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 48226 ms):
     48226 ms  clj-surgeon.mcp-feature-thread-test

Ran 1414 tests containing 16173 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91087 ms
cadence :fast: serial-equivalent 45275 ms; budget 60000 ms
cadence :integration: serial-equivalent 67216 ms; budget 240000 ms
bb-runtime: serial-equivalent 8092 ms; budget 343102 ms; makespan 21324 ms

namespace walls (101, slowest first, serial-equivalent total 112491 ms):
     48226 ms  clj-surgeon.mcp-feature-thread-test
      9232 ms  clj-surgeon.outline-corpus-integration-test
      8400 ms  clj-surgeon.mcp-compact-relations-test
      5153 ms  clj-surgeon.rename-alias-test
      4444 ms  clj-surgeon.mcp-operation-registry-test
      4158 ms  clj-surgeon.mcp-namespace-split-test
      3479 ms  clj-surgeon.insert-forms-test
      2988 ms  clj-surgeon.mcp-tool-test
      2937 ms  clj-surgeon.mcp-http-server-test
      2297 ms  clj-surgeon.mcp-inspect-tool-test
      2153 ms  clj-surgeon.mcp-hot-verify-test
      1432 ms  clj-surgeon.namespace-split-warm-test
      1426 ms  clj-surgeon.ns-isolation-test
      1367 ms  clj-surgeon.lane-manifest-test
       989 ms  clj-surgeon.splice-envelope-test
       936 ms  clj-surgeon.outline-test
       888 ms  clj-surgeon.helper-extraction-test
       820 ms  clj-surgeon.namespace-split-test
       786 ms  clj-surgeon.operation-algebra-test
       750 ms  clj-surgeon.analyze-test
       684 ms  clj-surgeon.insert-forms-receipt-test
       663 ms  clj-surgeon.rename-alias-receipt-test
       637 ms  clj-surgeon.mcp-change-buffer-test
       522 ms  clj-surgeon.mcp-compact-location-test
       414 ms  clj-surgeon.receipt-booleans-test
       409 ms  clj-surgeon.mcp-intent-contract-test
       348 ms  clj-surgeon.mcp-expect-guard-test
       345 ms  clj-surgeon.outline-memory-test
       342 ms  clj-surgeon.scope-stream-test
       335 ms  clj-surgeon.mcp-prepared-request-test
       306 ms  clj-surgeon.census-pool-test
       300 ms  clj-surgeon.mcp-create-files-test
       300 ms  clj-surgeon.mcp-inspect-contract-test
       248 ms  clj-surgeon.mcp-server-test
       238 ms  clj-surgeon.mcp-combinable-transaction-test
       231 ms  clj-surgeon.alias-migration-test
       211 ms  clj-surgeon.mcp-prepared-confirmation-test
       209 ms  clj-surgeon.mcp-operation-async-test
       190 ms  clj-surgeon.mcp-compact-edit-test
       175 ms  clj-surgeon.mcp-relation-census-round20-test
       169 ms  clj-surgeon.mcp-write-refusal-test
       167 ms  clj-surgeon.mcp-extraction-plan-test
       153 ms  clj-surgeon.mcp-formatter-test
       149 ms  clj-surgeon.mission-candidate-race-test
       143 ms  clj-surgeon.mcp-operation-test
       137 ms  clj-surgeon.mcp-semantic-client-test
       122 ms  clj-surgeon.fix-declares-test
       121 ms  clj-surgeon.workspace-onboarding-test
       115 ms  clj-surgeon.cljc-existing-ops-test
       113 ms  clj-surgeon.ls-tree-test
       105 ms  clj-surgeon.battery-parallel-test
        95 ms  clj-surgeon.recovery-test
        81 ms  clj-surgeon.telemetry-events-test
        73 ms  clj-surgeon.jvm-error-test
        68 ms  clj-surgeon.mcp-extraction-test
        66 ms  clj-surgeon.move-dependency-test
        59 ms  clj-surgeon.structural-lens-test
        53 ms  clj-surgeon.memory-battery-test
        48 ms  clj-surgeon.mcp-workspace-test
        44 ms  clj-surgeon.agent-routing-test
        42 ms  clj-surgeon.mcp-program-tool-test
        36 ms  clj-surgeon.require-change-test
        33 ms  clj-surgeon.fast-lane-isolation-test
        29 ms  clj-surgeon.mission-plain-forms-test
        28 ms  clj-surgeon.relation-census-test
        26 ms  clj-surgeon.owner-hypotheses-test
        26 ms  clj-surgeon.worktree-lifecycle-io-test
        24 ms  clj-surgeon.outline-differential-test
        20 ms  clj-surgeon.edit-dsl-test
        20 ms  clj-surgeon.outermost-test
        19 ms  clj-surgeon.cljc.merge-test
        16 ms  clj-surgeon.mcp-contract-test
        15 ms  clj-surgeon.cljc.split-test
        15 ms  clj-surgeon.move-test
        13 ms  clj-surgeon.mission-forms-source-test
        12 ms  clj-surgeon.mission-typist-test
        10 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.insertion-gap-test
         8 ms  clj-surgeon.quoted-var-refs-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.mcp-recovery-test
         4 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.mcp-telemetry-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mission-usage-test
         3 ms  clj-surgeon.split-proof-gate-test
         2 ms  clj-surgeon.battery-ledger-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-read-request-normalization-test
         1 ms  clj-surgeon.mission-candidate-test
         1 ms  clj-surgeon.mission-forms-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (22), makespan 91087 ms:
  lane 0     23812 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-formatter-test
  lane 1      1990 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.cljc-existing-ops-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.structural-lens-test clj-surgeon.mission-plain-forms-test clj-surgeon.forms-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-telemetry-test
  lane 2      3889 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.jvm-error-test clj-surgeon.edit-dsl-test clj-surgeon.require-change-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-forms-test
  lane 3     17039 ms  exit 0  clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-semantic-client-test
  lane 4     19567 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test
  lane 5      3394 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.recovery-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-recovery-test clj-surgeon.cljc.analyze-test
  lane 6     23491 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-operation-test
  lane 7       725 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.mcp-extraction-test clj-surgeon.move-test clj-surgeon.worktree-lifecycle-test clj-surgeon.insertion-gap-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test
  lane 8     23192 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.scope-stream-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  lane 9       631 ms  exit 0  clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.split-proof-gate-test clj-surgeon.mcp-read-request-normalization-test
  lane 10     24563 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-compact-edit-test
  lane 11       888 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.fast-lane-isolation-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 12     18264 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13       339 ms  exit 0  clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-forms-source-test clj-surgeon.outline-differential-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.mission-candidate-test
  lane 14     25835 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 15     10767 ms  exit 0  clj-surgeon.mcp-server-test
  lane 16      9895 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 17      7162 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 18     16307 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 19     13745 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 20     15276 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 21     55374 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 91087 ms over 8 lane(s); serial-equivalent 112491 ms; skipped 0

========== lane 0 (clj-surgeon.edit-test clj-surgeon.extract-test) exit 0, 11505 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.extract-test

========== lane 1 (clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.insertion-gap-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test) exit 0, 2785 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 2 (clj-surgeon.intent-transaction-test) exit 0, 13258 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 6339 ms):
      6339 ms  clj-surgeon.intent-transaction-test

========== lane 3 (clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 6989 ms ==========

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3604581-5b603ba9/andon-shell-safety7343190449369189857/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3604581-5b603ba9/andon-shell-safety7343190449369189857/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3604581-5b603ba9/andon-shell-safety12424383779558710564/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3604581-5b603ba9/andon-shell-safety12424383779558710564/PWNED-SUBST)"

========== lane 4 (clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.forms-test) exit 0, 1459 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.forms-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test) exit 0, 14784 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.edn-config-integration-test

========== lane 6 (clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test) exit 0, 575 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.move-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.failure-report-test

========== lane 7 (clj-surgeon.show-form-test clj-surgeon.platform-selector-test) exit 0, 14258 ms ==========

Testing clj-surgeon.show-form-test

Testing clj-surgeon.platform-selector-test

========== lane 8 (clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.split-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 649 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14135 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588029-bd08c2b8/tmp-leak-sweep-guard-5104373312853315777/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.cli-dispatch-test) exit 0, 14550 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 11 (clj-surgeon.parser-admission-test) exit 0, 72362 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 12 (clj-surgeon.install-test) exit 0, 90742 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/bin with spaces/clj-surgeon from commit 09588a14a784e220907462f46676900402bc80aa, source hash ad50675b1de7895cacbe897169e3c9c35d9c0fd978e1c1ce7480abc06af8f8ee
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x21e61f86 "Process[pid=3601166, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x413a70d7 "java.lang.ProcessImpl$ProcessPipeOutputStream@413a70d7"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.424935, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x2545ff77 "Process[pid=3601303, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x50923356 "java.lang.ProcessImpl$ProcessPipeOutputStream@50923356"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.291474, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x74117dea "Process[pid=3601459, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x5a9602c3 "java.lang.ProcessImpl$ProcessPipeOutputStream@5a9602c3"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3588022-b01edf38/clj-surgeon-installed-splice-3204049494503717992/sample.clj"]}

Ran 901 tests containing 7998 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 90742 ms
cadence :battery: serial-equivalent 242958 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3640 ms; budget 60000 ms
bb-runtime: serial-equivalent 240259 ms; budget 343102 ms; makespan 90742 ms

namespace walls (50, slowest first, serial-equivalent total 246598 ms):
     90581 ms  clj-surgeon.install-test
     72109 ms  clj-surgeon.parser-admission-test
     14144 ms  clj-surgeon.cli-dispatch-test
     13979 ms  clj-surgeon.tmp-leak-support-test
     13516 ms  clj-surgeon.show-form-test
     11394 ms  clj-surgeon.help-test
      6339 ms  clj-surgeon.intent-transaction-test
      6269 ms  clj-surgeon.edit-test
      4808 ms  clj-surgeon.extract-test
      4689 ms  clj-surgeon.core-discovery-test
      2380 ms  clj-surgeon.lens-query-test
      1547 ms  clj-surgeon.xray-test
       923 ms  clj-surgeon.outline-test
       815 ms  clj-surgeon.operation-algebra-test
       708 ms  clj-surgeon.analyze-test
       575 ms  clj-surgeon.edn-config-integration-test
       334 ms  clj-surgeon.partition-all-test
       294 ms  clj-surgeon.platform-selector-test
       203 ms  clj-surgeon.ls-tree-test
       187 ms  clj-surgeon.alias-migration-test
       114 ms  clj-surgeon.workspace-onboarding-test
       113 ms  clj-surgeon.cljc-existing-ops-test
        88 ms  clj-surgeon.move-dependency-test
        75 ms  clj-surgeon.jvm-error-test
        56 ms  clj-surgeon.recovery-test
        48 ms  clj-surgeon.fix-declares-test
        36 ms  clj-surgeon.agent-routing-test
        36 ms  clj-surgeon.structural-lens-test
        35 ms  clj-surgeon.owner-hypotheses-test
        28 ms  clj-surgeon.relation-census-test
        24 ms  clj-surgeon.move-test
        23 ms  clj-surgeon.edit-dsl-test
        23 ms  clj-surgeon.outermost-test
        23 ms  clj-surgeon.worktree-lifecycle-io-test
        19 ms  clj-surgeon.memory-battery-test
        11 ms  clj-surgeon.worktree-lifecycle-test
        10 ms  clj-surgeon.cljc.split-test
         9 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.quoted-var-refs-test
         6 ms  clj-surgeon.rename-test
         5 ms  clj-surgeon.cljc.merge-test
         4 ms  clj-surgeon.failure-report-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.cljc.require-ops-test
         2 ms  clj-surgeon.forms-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.cljc.analyze-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (13), makespan 90742 ms:
  lane 0     11505 ms  exit 0  clj-surgeon.edit-test clj-surgeon.extract-test
  lane 1      2785 ms  exit 0  clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.alias-migration-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.owner-hypotheses-test clj-surgeon.edit-dsl-test clj-surgeon.insertion-gap-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test
  lane 2     13258 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 3      6989 ms  exit 0  clj-surgeon.core-discovery-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 4      1459 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.require-ops-test clj-surgeon.forms-test
  lane 5     14784 ms  exit 0  clj-surgeon.help-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  lane 6       575 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test
  lane 7     14258 ms  exit 0  clj-surgeon.show-form-test clj-surgeon.platform-selector-test
  lane 8       649 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.split-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 9     14135 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10     14550 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 11     72362 ms  exit 0  clj-surgeon.parser-admission-test
  lane 12     90742 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 90742 ms over 8 lane(s); serial-equivalent 246598 ms; skipped 0
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
bash test/performance_regression_sentinel_intent_test.sh
Sentinel intent witness audit passed: 50/50 requirements named
bash test/performance_regression_sentinel_intent_self_test.sh
Sentinel intent audit self-test passed: missing simple/multipart and unknown multipart IDs refused
python3 -B -m unittest discover -s test/oracles -p test_gate_slot.py
sun_path witness selected: policy TMPDIR=/var/tmp/forge/bbtower-fx/csg-1011 root_bytes=34 sun_path_bytes=76 writable=True
sun_path witness branch: real bind; root=/var/tmp/forge/bbtower-fx/csg-1011
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3627182-caddd1c6 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3627182-caddd1c6 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3627182-caddd1c6
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3629655-004c000c node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3629655-004c000c node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3629655-004c000c
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3630955-f5b86cdf node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3630955-f5b86cdf node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/realdisk/clj-surgeon-suite-3630955-f5b86cdf
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/subbase/clj-surgeon-suite-3631075-7346fb31 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/subbase/clj-surgeon-suite-3631075-7346fb31 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/subbase/clj-surgeon-suite-3631075-7346fb31
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/subbase/clj-surgeon-suite-3631075-7346fb31/tmp.4xEo66ZdIH
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/subbase/clj-surgeon-suite-3631075-7346fb31: tmp.4xEo66ZdIH
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/stalebase/clj-surgeon-suite-3633852-2a969529 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/stalebase/clj-surgeon-suite-3633852-2a969529 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/stalebase/clj-surgeon-suite-3633852-2a969529
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-3622827-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9/nowrite/clj-surgeon-suite-3634274-7ac02fc5 Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9 -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.oRaxb9 ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.MuaPUW/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.MuaPUW/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s.........
----------------------------------------------------------------------
Ran 21 tests in 0.398s

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
Ran 3 tests in 1.321s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
gate-stage: {:target "alias-migration-test", :exit 0, :wall-ms 87959}
gate-stage: {:target "mcp-test", :exit 0, :wall-ms 91087}
gate-stage: {:target "test-bb", :exit 0, :wall-ms 90742}
gate-stage: test-bb-diagnostic started 2026-09-12T17:13:57.535332205Z
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" test/run_all.clj
SERIAL/NOT-A-GATE: direct Babashka diagnostic

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.tmp-leak-support-test
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/tmp-leak-sweep-guard-17602656951051650703/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

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
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/bin with spaces/clj-surgeon from commit 09588a14a784e220907462f46676900402bc80aa, source hash ad50675b1de7895cacbe897169e3c9c35d9c0fd978e1c1ce7480abc06af8f8ee
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x75e1ec6c "Process[pid=3700590, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x7930bd2d "java.lang.ProcessImpl$ProcessPipeOutputStream@7930bd2d"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.197614, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0xcf6b05f "Process[pid=3700597, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x2953f579 "java.lang.ProcessImpl$ProcessPipeOutputStream@2953f579"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.206276, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x2644074b "Process[pid=3700653, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x61c61746 "java.lang.ProcessImpl$ProcessPipeOutputStream@61c61746"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/clj-surgeon-installed-splice-4646367398222358651/sample.clj"]}

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.worktree-lifecycle-cli-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.cli-dispatch-test

Testing clj-surgeon.core-discovery-test
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/andon-shell-safety1358828271130057226/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/andon-shell-safety1358828271130057226/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/andon-shell-safety6052922134176198173/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-3652182-7563fc8c/andon-shell-safety6052922134176198173/PWNED-SUBST)"

Ran 832 tests containing 7297 assertions.
0 failures, 0 errors.
gate-stage: {:target "test-bb-diagnostic", :exit 0, :wall-ms 224615}
gate-stage: repository-hygiene started 2026-09-12T17:17:42.151345288Z
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
repository hygiene: no machine-local build cache is tracked at any depth
gate-stage: {:target "repository-hygiene", :exit 0, :wall-ms 2087}
gate-stage: intent-audit started 2026-09-12T17:17:44.239353635Z
{:ok true, :specs {"MCP-OP-ADMIT-135" :implemented, "MCP-OP-ADMIT-051" :implemented, "MCP-OP-HELPER-020" :active-gap, "MCP-OP-THREAD-021" :implemented, "INSERT-FORMS-020" :implemented, "MCP-OP-ALIAS-015" :implemented, "MCP-OP-THREAD-046" :implemented, "MCP-OP-ALIAS-032" :implemented, "MCP-OP-HELPER-022" :active-gap, "MCP-OP-ADMIT-020" :implemented, "WTL-APPLY-007" :implemented, "MCP-OP-CENSUS-012" :implemented, "MCP-OP-EDIT-008" :implemented, "MCP-OP-PREP-ACT-018" :active-gap, "NS-SPLIT-043" :implemented, "MCP-OP-DISPATCH-004" :implemented, "MCP-OP-EDIT-035" :implemented, "MCP-OP-EDIT-024" :implemented, "MCP-OP-ADMIT-121" :implemented, "WTL-PRUNE-005" :implemented, "PERF-SENT-IMPORT-001" :active-gap, "MCP-OP-VERIFY-010" :implemented, "MCP-OP-MEM-011" :deferred, "MCP-OP-PREP-ACT-017" :active-gap, "MCP-OP-ALIAS-002" :implemented, "OP-ALG-EFFECT-002" :implemented, "MCP-OP-HELPER-014" :active-gap, "MCP-OP-READ-MISSION-001" :deferred, "MCP-OP-READ-MISSION-002" :deferred, "MCP-OP-CENSUS-027" :implemented, "REQUIRE-CHANGE-005" :implemented, "MCP-OP-ALIAS-055" :implemented, "MCP-OP-EDIT-004" :implemented, "INSERT-FORMS-011" :implemented, "MCP-OP-ADMIT-152" :active-gap, "NS-SPLIT-057" :implemented, "MCP-OP-READ-RESOLVE-002" :deferred, "TELEMETRY-EVENTS-001" :implemented, "NS-SPLIT-060" :implemented, "MCP-OP-ADMIT-106" :implemented, "MCP-OP-ADMIT-147" :active-gap, "TEST-ISO-RACE-002" :implemented, "MCP-OP-PREP-ACT-015" :active-gap, "MCP-OP-EDIT-034" :implemented, "MCP-OP-ADMIT-069" :implemented, "INSERT-FORMS-006" :implemented, "MCP-OP-ALIAS-059" :implemented, "MCP-OP-ADMIT-150" :implemented, "OP-ALG-PREVIEW-002" :implemented, "MCP-OP-INSERT-009" :implemented, "MCP-OP-CENSUS-029" :implemented, "BB-PROBE-004" :implemented, "MCP-OP-VERIFY-009" :implemented, "MCP-OP-VERIFY-008" :implemented, "MCP-OP-ALIAS-040" :implemented, "PERF-SENT-VERDICT-002" :active-gap, "NS-SPLIT-029" :implemented, "MCP-OP-CENSUS-017" :implemented, "MCP-OP-ADMIT-021" :implemented, "MCP-OP-ADMIT-110" :implemented, "MCP-OP-FIELD-003" :implemented, "PERF-SENT-AUTH-003" :active-gap, "MCP-OP-PREP-REQ-007" :implemented, "MCP-OP-ADMIT-151" :implemented, "MCP-OP-THREAD-042" :implemented, "PERF-SENT-VERDICT-003" :active-gap, "MCP-OP-HELPER-002" :active-gap, "ALIAS-MIGRATION-005" :implemented, "PERF-SENT-PROJECT-001" :active-gap, "TEST-ISO-001a" :implemented, "MCP-OP-SHELL-ARGV-006" :implemented, "MCP-OP-ALIAS-017" :implemented, "MCP-OP-ADMIT-023" :implemented, "PERF-SENT-LEDGER-004" :active-gap, "MCP-OP-PREP-ACT-007" :active-gap, "MCP-OP-PREP-ACT-002" :active-gap, "MCP-OP-WRITE-REFUSAL-002" :deferred, "INSERT-FORMS-010" :implemented, "MCP-OP-PLAN-001" :implemented, "WTL-SEAL-006" :implemented, "PERF-SENT-RELEASE-001" :active-gap, "MCP-OP-READ-GUARD-001" :implemented, "MCP-OP-HELPER-011" :active-gap, "MCP-OP-SHELL-ARGV-007" :implemented, "WTL-PRUNE-001" :implemented, "MCP-OP-ALIAS-014" :implemented, "MCP-OP-EDIT-028" :implemented, "MCP-OP-RESULT-005" :implemented, "MCP-OP-COVERAGE-001" :implemented, "NS-SPLIT-072" :implemented, "MCP-OP-EDIT-007" :implemented, "WTL-HAND-005" :implemented, "MCP-OP-MATCHED-001" :implemented, "MCP-OP-ADMIT-107" :implemented, "TEST-ISO-002" :implemented, "MCP-OP-EDIT-012" :implemented, "MCP-OP-VERIFY-004" :implemented, "MCP-OP-POS-AUTH-003" :implemented, "MCP-OP-WRITE-REFUSAL-003" :deferred, "MCP-OP-ALIAS-051" :implemented, "MCP-OP-HELPER-018" :active-gap, "MCP-OP-ADMIT-144" :implemented, "MCP-OP-ADMIT-034" :implemented, "MCP-OP-HELPER-009" :active-gap, "NS-SPLIT-001" :implemented, "PERF-SENT-TIME-001" :active-gap, "ALIAS-MIGRATION-003" :implemented, "MCP-OP-ALIAS-046" :implemented, "MCP-OP-ADMIT-099" :implemented, "MCP-OP-THREAD-029" :implemented, "MEASURE-EVID-001" :active-gap, "MCP-OP-ADMIT-111" :implemented, "MCP-OP-ALIAS-065" :active-gap, "MCP-OP-EDIT-018" :implemented, "MCP-OP-MATCHED-004" :implemented, "MCP-OP-ALIAS-047" :implemented, "MCP-OP-PREP-ACT-001" :active-gap, "MCP-OP-THREAD-039" :implemented, "MCP-OP-ADMIT-032" :implemented, "MCP-OP-TMPHYG-012" :implemented, "MCP-OP-HELPER-019" :active-gap, "MCP-OP-PLAN-004" :implemented, "NS-SPLIT-008" :implemented, "NS-SPLIT-046" :implemented, "NS-SPLIT-042" :deferred, "MCP-OP-TRACE-002" :implemented, "OP-ALG-VERIFY-004" :deferred, "REQUIRE-CHANGE-004" :implemented, "MCP-OP-ALIAS-019" :implemented, "WTL-APPLY-002" :implemented, "NS-SPLIT-056" :implemented, "MCP-OP-EDIT-027" :implemented, "MCP-OP-INSERT-003" :active-gap, "NS-SPLIT-071" :implemented, "MCP-OP-HOTVER-002" :implemented, "MCP-OP-ALIAS-036" :implemented, "REQUIRE-CHANGE-002" :implemented, "MCP-OP-CENSUS-035" :implemented, "OP-ALG-EFFECT-005" :implemented, "MCP-OP-ADMIT-065" :implemented, "MCP-OP-ANALYZER-008" :implemented, "MCP-OP-MATCH-005" :deferred, "PERF-SENT-SCHEDULE-002" :active-gap, "MCP-OP-CENSUS-005" :implemented, "MCP-OP-ADMIT-066" :implemented, "RENAME-ALIAS-001" :implemented, "MCP-OP-ALIAS-016" :implemented, "PERF-SENT-CONFIG-001" :active-gap, "MCP-OP-ANALYZER-009" :implemented, "MCP-OP-CENSUS-034" :implemented, "MCP-OP-ASYNC-002" :implemented, "MCP-OP-PREP-REQ-005" :implemented, "OP-ALG-COMMIT-003" :implemented, "MCP-OP-PREP-ACT-014" :active-gap, "RENAME-ALIAS-003" :implemented, "MCP-OP-ADMIT-141" :implemented, "TEST-ISO-014" :implemented, "MCP-OP-HELPER-021" :active-gap, "WTL-CLI-003" :implemented, "MCP-OP-ADMIT-148" :implemented, "MCP-OP-ALIAS-063" :active-gap, "MCP-OP-ANALYZER-006" :active-gap, "INSERT-FORMS-012" :implemented, "MCP-OP-EDIT-001" :implemented, "MCP-OP-EDIT-033" :implemented, "NS-SPLIT-052" :implemented, "OP-ALG-RUNTIME-001" :implemented, "NS-SPLIT-062" :implemented, "MCP-OP-ADMIT-061" :implemented, "MCP-OP-EDIT-032" :implemented, "NS-SPLIT-035" :implemented, "MCP-OP-ALIAS-044" :implemented, "MCP-OP-SHELL-ARGV-002" :implemented, "WTL-HAND-002" :implemented, "MCP-OP-VERIFY-007" :implemented, "MCP-OP-MATCHED-005" :implemented, "MCP-OP-THREAD-006" :implemented, "WTL-SEAL-001" :implemented, "MCP-OP-PREP-ACT-005" :active-gap, "RENAME-ALIAS-014" :implemented, "WTL-INV-001" :implemented, "MCP-OP-VERIFY-003" :implemented, "MCP-OP-ADMIT-105" :implemented, "MCP-OP-ADMIT-088" :implemented, "MCP-OP-ADMIT-030" :implemented, "PERF-SENT-CUTOVER-002" :active-gap, "MCP-OP-SCHEMA-001" :implemented, "INSERT-FORMS-008" :implemented, "MCP-OP-ADMIT-149" :implemented, "RENAME-ALIAS-007" :implemented, "RENAME-ALIAS-010" :implemented, "MCP-OP-VERIFY-005" :implemented, "MCP-OP-ADMIT-139" :implemented, "MCP-OP-THREAD-018" :implemented, "MCP-OP-VERIFY-006" :implemented, "MCP-OP-READ-HYP-002" :implemented, "MCP-OP-THREAD-012" :implemented, "OP-ALG-STALE-001" :implemented, "MCP-OP-MEM-020" :implemented, "MCP-OP-TRACE-006" :implemented, "INSERT-FORMS-017" :implemented, "MCP-OP-CENSUS-010" :implemented, "MCP-OP-ALIAS-054" :implemented, "MCP-OP-READ-NORM-001" :active-gap, "MCP-OP-EDIT-009" :implemented, "NS-SPLIT-015" :implemented, "TEST-ISO-001" :implemented, "MCP-OP-READ-RESOLVE-001" :deferred, "MCP-OP-ALIAS-057" :implemented, "MCP-OP-ALIAS-009" :implemented, "WTL-INV-002" :implemented, "MCP-OP-EDIT-015" :implemented, "MCP-OP-DISPATCH-002" :implemented, "MCP-OP-WRITE-REFUSAL-006" :deferred, "MCP-OP-TIME-003" :implemented, "MCP-OP-CENSUS-028" :implemented, "PERF-SENT-AUTH-001" :active-gap, "NS-SPLIT-041" :implemented, "MCP-OP-POS-AUTH-007" :implemented, "MCP-OP-MATCHED-002" :implemented, "MCP-OP-ANALYZER-002" :implemented, "TEST-ISO-010" :implemented, "WTL-PLAN-004" :implemented, "MCP-OP-MEM-007" :implemented, "MCP-OP-ALIAS-013" :implemented, "WTL-PRUNE-008" :implemented, "PERF-SENT-LEDGER-003" :active-gap, "MCP-OP-DISPATCH-003" :implemented, "MCP-OP-TMPHYG-009" :implemented, "WTL-PRUNE-002" :implemented, "NS-SPLIT-064" :implemented, "MCP-OP-EDIT-037" :implemented, "MCP-OP-HOTVER-001" :implemented, "MCP-OP-POS-AUTH-004" :implemented, "MCP-OP-ADMIT-100" :implemented, "NS-SPLIT-049" :implemented, "MCP-OP-TMPHYG-011" :implemented, "MCP-OP-ADMIT-120" :implemented, "MCP-OP-ADMIT-060" :implemented, "WTL-HAND-003" :implemented, "MCP-OP-THREAD-001" :implemented, "MCP-OP-EDIT-023" :implemented, "MCP-OP-CENSUS-030" :implemented, "MCP-OP-RESULT-003" :implemented, "INSERT-FORMS-019" :implemented, "MCP-OP-ADMIT-033" :implemented, "WTL-APPLY-004" :implemented, "MCP-OP-THREAD-008" :implemented, "MCP-OP-ADMIT-112" :implemented, "MCP-OP-THREAD-015" :implemented, "MCP-OP-EDIT-016" :implemented, "TEST-ISO-009" :implemented, "MCP-OP-ALIAS-028" :implemented, "WTL-PRUNE-010" :implemented, "MCP-OP-ALIAS-039" :implemented, "RECEIPT-BOOL-001" :implemented, "MCP-OP-ADMIT-081" :implemented, "MCP-OP-CENSUS-003" :implemented, "MCP-OP-CENSUS-019" :implemented, "MCP-OP-ALIAS-067" :implemented, "MCP-OP-CENSUS-020" :implemented, "RENAME-ALIAS-008" :implemented, "WTL-SEAL-008" :deferred, "MCP-OP-ADMIT-062" :implemented, "REQUIRE-CHANGE-008" :implemented, "MCP-OP-FIELD-002" :implemented, "MCP-OP-READ-NORM-004" :active-gap, "MCP-OP-THREAD-041" :implemented, "MCP-OP-THREAD-011" :implemented, "MCP-OP-TMPHYG-003" :implemented, "MCP-OP-MEM-015" :implemented, "MCP-OP-THREAD-051" :implemented, "OP-ALG-COMPILE-001" :implemented, "MCP-OP-ALIAS-030" :implemented, "BB-PROBE-001" :implemented, "OP-ALG-EXPAND-001" :deferred, "MCP-OP-DISPATCH-005" :implemented, "MCP-OP-POS-AUTH-010" :implemented, "MCP-OP-THREAD-038" :implemented, "MCP-OP-EDIT-011" :implemented, "MCP-OP-ADMIT-004" :implemented, "NS-SPLIT-013" :implemented, "MCP-OP-ASYNC-001" :implemented, "MCP-OP-ADMIT-070" :implemented, "MCP-OP-ALIAS-022" :implemented, "MCP-OP-ADMIT-142" :implemented, "TEST-ISO-009b" :implemented, "MCP-OP-PREP-ACT-013" :active-gap, "MCP-OP-ALIAS-005" :implemented, "NS-SPLIT-005" :implemented, "NS-SPLIT-004" :implemented, "MCP-OP-HELPER-007" :active-gap, "MCP-OP-HELPER-005" :active-gap, "NS-SPLIT-055" :implemented, "MCP-OP-READ-CONT-001" :implemented, "MCP-OP-ALIAS-023" :implemented, "MCP-OP-THREAD-035" :implemented, "PERF-SENT-SCHEDULE-003" :active-gap, "MCP-OP-INSERT-010" :implemented, "CLI-PACKAGE-001" :implemented, "MCP-OP-ALIAS-038" :implemented, "RENAME-ALIAS-012" :implemented, "INSERT-FORMS-018" :implemented, "MCP-OP-ADMIT-103" :implemented, "OP-ALG-EFFECT-004" :implemented, "MCP-OP-CENSUS-006" :implemented, "INSERT-FORMS-016" :implemented, "MCP-OP-THREAD-014" :implemented, "SPLIT-REPAIR-002" :implemented, "MCP-OP-HELPER-013" :active-gap, "MCP-OP-DISPATCH-001" :implemented, "INSERT-FORMS-023" :implemented, "MCP-OP-ADMIT-125" :implemented, "OP-ALG-DECODE-001" :implemented, "INSERT-FORMS-021" :implemented, "MCP-OP-ANALYZER-004" :implemented, "MCP-OP-THREAD-023" :implemented, "NS-SPLIT-036" :implemented, "OP-ALG-COMMIT-001" :implemented, "MCP-OP-ADMIT-098" :implemented, "MCP-OP-ALIAS-050" :implemented, "MCP-OP-ANALYZER-003" :implemented, "PERF-SENT-BASELINE-001" :active-gap, "MCP-OP-ALIAS-026" :implemented, "MCP-OP-PREP-ACT-003" :active-gap, "TEST-ISO-015" :implemented, "WTL-APPLY-009" :implemented, "WTL-PRUNE-004" :implemented, "MCP-OP-ALIAS-061" :implemented, "MCP-OP-FIELD-008" :implemented, "PERF-SENT-AUTH-002" :active-gap, "MCP-OP-THREAD-031" :implemented, "MCP-OP-THREAD-019" :implemented, "MCP-OP-ADMIT-096" :implemented, "MCP-OP-RELAY-002" :implemented, "WTL-HAND-001" :implemented, "MCP-OP-CENSUS-018" :implemented, "PERF-SENT-CUTOVER-003" :active-gap, "MCP-OP-WRITE-REFUSAL-001" :implemented, "NS-SPLIT-065" :implemented, "MCP-OP-HELPER-004" :active-gap, "OP-ALG-OUTCOME-003" :implemented, "TEST-ISO-016" :implemented, "MCP-OP-ALIAS-007" :implemented, "OP-ALG-CLI-001" :implemented, "MCP-OP-THREAD-032" :implemented, "MCP-OP-ADMIT-119" :implemented, "MCP-OP-TMPHYG-006" :implemented, "MCP-OP-HELPER-016" :active-gap, "MCP-OP-ALIAS-060" :implemented, "TEST-ISO-001c" :implemented, "MCP-OP-PLAN-002" :implemented, "WTL-INV-007" :implemented, "MCP-OP-ADMIT-024" :implemented, "NS-SPLIT-034" :implemented, "MCP-OP-WRITE-REFUSAL-008" :deferred, "PERF-SENT-RETAIN-001" :active-gap, "MCP-OP-TMPHYG-004" :implemented, "MCP-OP-THREAD-045" :implemented, "NS-SPLIT-045" :implemented, "REQUIRE-CHANGE-011" :implemented, "MCP-OP-THREAD-040" :implemented, "MCP-OP-RELAY-001" :implemented, "MCP-OP-CENSUS-004" :implemented, "MCP-OP-SHELL-ARGV-005" :implemented, "MCP-OP-CENSUS-023" :implemented, "PERF-SENT-CLIENT-001" :active-gap, "MCP-OP-ADMIT-064" :implemented, "NS-SPLIT-033" :implemented, "MCP-OP-READ-DIAG-001" :implemented, "INSERT-FORMS-015" :implemented, "NS-SPLIT-038" :implemented, "MCP-OP-THREAD-020" :implemented, "MCP-OP-WRITE-REFUSAL-004" :deferred, "MCP-OP-TMPHYG-001" :implemented, "MCP-OP-READ-PARITY-001" :implemented, "MCP-OP-ADMIT-012" :implemented, "ROUTING-FANOUT-001" :implemented, "MCP-OP-EDIT-038" :implemented, "INSERT-FORMS-013" :implemented, "RENAME-ALIAS-004" :implemented, "MCP-OP-HELPER-012" :active-gap, "NS-SPLIT-059" :implemented, "MCP-OP-ANALYZER-007" :implemented, "MCP-OP-PREP-REQ-004" :implemented, "MCP-OP-CENSUS-021" :implemented, "MCP-OP-THREAD-033" :implemented, "WTL-SEAL-004" :implemented, "PERF-SENT-RECEIPT-001" :active-gap, "RENAME-ALIAS-009" :implemented, "MCP-OP-ALIAS-024" :implemented, "MCP-OP-INSERT-001" :active-gap, "MCP-OP-POS-AUTH-001" :implemented, "MCP-OP-VERIFY-001" :implemented, "MCP-OP-ALIAS-041" :implemented, "NS-SPLIT-002" :implemented, "PERF-SENT-INVALID-001" :active-gap, "MCP-OP-TRACE-001" :implemented, "REQUIRE-CHANGE-009" :implemented, "MCP-OP-ALIAS-025" :implemented, "MCP-OP-ADMIT-094" :implemented, "RENAME-ALIAS-011" :implemented, "MCP-OP-THREAD-050" :implemented, "INSERT-FORMS-005" :implemented, "OP-ALG-OUTCOME-002" :implemented, "REQUIRE-CHANGE-003" :implemented, "MCP-OP-ADMIT-063" :implemented, "WTL-APPLY-010" :implemented, "MCP-OP-ALIAS-029" :implemented, "MCP-OP-THREAD-052" :implemented, "MCP-OP-MEM-001" :deferred, "MCP-OP-ALIAS-018" :implemented, "PERF-SENT-RECOVERY-002" :active-gap, "MCP-OP-ADMIT-050" :implemented, "MCP-OP-ADMIT-137" :implemented, "MCP-OP-ADMIT-053" :implemented, "MCP-OP-EDIT-003" :implemented, "OP-ALG-REFUSE-001" :implemented, "MCP-OP-ADMIT-132" :implemented, "OP-ALG-CLI-002" :deferred, "PERF-SENT-IDENT-001" :active-gap, "PERF-SENT-SCHEDULE-001" :active-gap, "MCP-OP-ADMIT-067" :implemented, "MCP-OP-ADMIT-090" :implemented, "MCP-OP-CENSUS-022" :implemented, "INSERT-FORMS-022" :implemented, "MCP-OP-TMPHYG-005" :implemented, "MCP-OP-HELPER-003" :active-gap, "MCP-OP-SHELL-ARGV-003" :implemented, "OP-ALG-EFFECT-001" :implemented, "INSERT-FORMS-001" :implemented, "INSERT-FORMS-007" :implemented, "MCP-OP-ALIAS-001" :implemented, "MCP-OP-ALIAS-006" :implemented, "MCP-OP-CENSUS-013" :implemented, "NS-SPLIT-007" :implemented, "MCP-OP-THREAD-027" :implemented, "MCP-OP-ADMIT-113" :implemented, "SPLIT-REPAIR-001" :implemented, "MCP-OP-THREAD-007" :implemented, "MCP-OP-ADMIT-116" :implemented, "MCP-OP-PREP-ACT-004" :active-gap, "MCP-OP-PLAN-009" :implemented, "MCP-OP-ALIAS-020" :implemented, "MCP-OP-THREAD-048" :implemented, "MEASURE-WALL-002" :active-gap, "MCP-OP-ADMIT-005" :implemented, "MCP-OP-PREP-REQ-003" :implemented, "MCP-OP-READ-AUTH-001" :deferred, "PERF-SENT-PROMOTION-002" :active-gap, "MCP-OP-SHELL-ARGV-001" :implemented, "MCP-OP-TMPHYG-008" :implemented, "MCP-OP-PREP-REQ-006" :implemented, "REQUIRE-CHANGE-010" :implemented, "MCP-OP-ADMIT-109" :implemented, "TEST-ISO-008" :active-gap, "PERF-SENT-SCHEDULE-004" :active-gap, "MCP-OP-EDIT-022" :implemented, "MCP-OP-ADMIT-127" :implemented, "WTL-SEAL-005" :implemented, "MCP-OP-TMPHYG-007" :implemented, "MCP-OP-ADMIT-082" :implemented, "WTL-HAND-004" :implemented, "MCP-OP-RELAY-003" :implemented, "NS-SPLIT-067" :implemented, "MCP-OP-WRITE-REFUSAL-007" :deferred, "OP-ALG-COMMIT-004" :implemented, "NS-SPLIT-032" :implemented, "MCP-OP-EDIT-041" :implemented, "INSERT-FORMS-004" :implemented, "MCP-OP-THREAD-013" :implemented, "RENAME-ALIAS-005" :implemented, "PERF-SENT-IMPORT-002" :active-gap, "MCP-OP-EDIT-040" :implemented, "MCP-OP-ALIAS-033" :implemented, "MCP-OP-ADMIT-003" :implemented, "NS-SPLIT-048" :implemented, "PERF-SENT-BACKFILL-001" :active-gap, "MCP-OP-POS-AUTH-002" :implemented, "ALIAS-MIGRATION-004" :implemented, "MCP-OP-MEM-006" :implemented, "MCP-OP-ADMIT-089" :implemented, "MCP-OP-ADMIT-136" :implemented, "MCP-OP-INSERT-002" :active-gap, "PERF-SENT-LEDGER-002" :active-gap, "TEST-ISO-011" :active-gap, "MCP-OP-FIELD-001" :implemented, "MCP-OP-POS-AUTH-005" :implemented, "OP-ALG-FORM-COUNT-001" :implemented, "MCP-OP-MATCH-004" :implemented, "MCP-OP-ADMIT-071" :implemented, "OP-ALG-VERIFY-001" :deferred, "MCP-OP-THREAD-026" :implemented, "MCP-OP-ALIAS-053" :implemented, "TEST-ISO-009a" :implemented, "ALIAS-MIGRATION-001" :implemented, "INSERT-FORMS-003" :implemented, "MCP-OP-EDIT-006" :implemented, "PERF-SENT-PUBLISH-002" :active-gap, "PERF-SENT-LEDGER-ROOT-001" :active-gap, "MCP-OP-CENSUS-009" :implemented, "MCP-OP-PLAN-003" :implemented, "MCP-OP-INSERT-005" :active-gap, "MCP-OP-INSERT-004" :active-gap, "MCP-OP-ADMIT-143" :implemented, "MCP-OP-EDIT-030" :implemented, "MCP-OP-ALIAS-062" :active-gap, "OP-ALG-CONTEXT-002" :implemented, "MCP-OP-TMPHYG-010" :implemented, "MCP-OP-THREAD-034" :implemented, "MCP-OP-READ-NORM-005" :active-gap, "MCP-OP-CENSUS-016" :implemented, "MCP-OP-EDIT-013" :implemented, "WTL-INV-006" :implemented, "MCP-OP-ADMIT-084" :implemented, "WTL-APPLY-003" :implemented, "INSERT-FORMS-014" :implemented, "MCP-OP-THREAD-009" :implemented, "PERF-SENT-PROMOTION-001" :active-gap, "NS-SPLIT-053" :implemented, "MCP-OP-INSERT-007" :implemented, "NS-SPLIT-044" :implemented, "NS-SPLIT-063" :implemented, "MCP-OP-MATCHED-003" :implemented, "MCP-OP-MEM-012" :implemented, "MCP-OP-ASYNC-005" :implemented, "REQUIRE-CHANGE-013" :implemented, "MCP-OP-ADMIT-130" :implemented, "MCP-OP-ADMIT-128" :implemented, "BB-PROBE-002" :implemented, "REQUIRE-CHANGE-001" :implemented, "MCP-OP-EDIT-025" :implemented, "MCP-OP-ADMIT-091" :implemented, "MCP-OP-THREAD-037" :implemented, "MCP-OP-HELPER-023" :active-gap, "NS-SPLIT-047" :implemented, "NS-SPLIT-051" :implemented, "MCP-OP-ADMIT-145" :implemented, "MCP-OP-HELPER-010" :active-gap, "MCP-OP-READ-NORM-003" :active-gap, "PERF-SENT-SURFACE-001" :active-gap, "MCP-OP-THREAD-044" :implemented, "MCP-OP-ALIAS-037" :implemented, "MCP-OP-HELPER-001" :active-gap, "MCP-OP-ADMIT-108" :implemented, "PERF-SENT-ATTEMPT-003" :active-gap, "MCP-OP-READ-HYP-001" :implemented, "PERF-SENT-ATTEMPT-001" :active-gap, "OP-ALG-RECEIPT-002" :implemented, "OP-ALG-EFFECT-003" :implemented, "NS-SPLIT-069" :implemented, "MCP-OP-TRACE-003" :implemented, "MCP-OP-VERIFY-013" :implemented, "MCP-OP-ADMIT-022" :implemented, "MCP-OP-ADMIT-118" :implemented, "MCP-OP-ADMIT-002" :implemented, "MCP-OP-RESULT-006" :implemented, "OP-ALG-VERIFY-002" :deferred, "MCP-OP-POS-AUTH-009" :implemented, "OP-ALG-SHADOW-001" :implemented, "MCP-OP-ADMIT-133" :implemented, "MCP-OP-ALIAS-021" :implemented, "MCP-OP-TMPHYG-013" :implemented, "MCP-OP-PREP-REQ-008" :implemented, "OP-ALG-CONTEXT-001" :implemented, "NS-SPLIT-012" :implemented, "MCP-OP-CENSUS-008" :implemented, "MCP-OP-THREAD-047" :implemented, "MCP-OP-CENSUS-001" :implemented, "MCP-OP-EDIT-019" :implemented, "MCP-OP-CENSUS-031" :implemented, "MCP-OP-ALIAS-049" :implemented, "NS-SPLIT-061" :implemented, "SPLIT-REPAIR-004" :implemented, "MCP-OP-PREP-ACT-010" :active-gap, "OP-ALG-PARITY-001" :implemented, "WTL-APPLY-008" :implemented, "MCP-OP-CENSUS-033" :implemented, "WTL-INV-004" :implemented, "INSERT-FORMS-009" :implemented, "PERF-SENT-RECEIPT-002" :active-gap, "WTL-PLAN-003" :implemented, "NS-SPLIT-054" :implemented, "PERF-SENT-ATTEMPT-002" :active-gap, "REQUIRE-CHANGE-012" :implemented, "MCP-OP-ALIAS-004" :implemented, "MCP-OP-ADMIT-080" :implemented, "MCP-OP-HELPER-006" :active-gap, "MCP-OP-ALIAS-003" :implemented, "MCP-OP-EDIT-039" :implemented, "TEST-ISO-003" :implemented, "MCP-OP-ALIAS-035" :implemented, "MCP-OP-THREAD-028" :implemented, "MCP-OP-TRACE-005" :implemented, "MCP-OP-THREAD-036" :implemented, "RENAME-ALIAS-006" :implemented, "MCP-OP-ADMIT-040" :implemented, "MCP-OP-ADMIT-092" :implemented, "REQUIRE-CHANGE-007" :implemented, "WTL-SEAL-002" :implemented, "MCP-OP-EDIT-014" :implemented, "SPLIT-REPAIR-003" :implemented, "MCP-OP-ADMIT-097" :implemented, "MCP-OP-ADMIT-055" :implemented, "MCP-OP-EDIT-002" :implemented, "MCP-OP-PREP-ACT-009" :active-gap, "MCP-OP-ADMIT-001" :implemented, "NS-SPLIT-040" :implemented, "MCP-OP-PLAN-005" :implemented, "NS-SPLIT-058" :implemented, "MCP-OP-COVERAGE-002" :implemented, "MCP-OP-ADMIT-126" :implemented, "OP-ALG-CATALOG-001" :implemented, "MCP-OP-ADMIT-146" :implemented, "PERF-SENT-IDENT-002" :active-gap, "MCP-OP-CENSUS-024" :implemented, "MCP-OP-ADMIT-104" :implemented, "MCP-OP-MATCH-002" :implemented, "MCP-OP-READ-CONT-002" :implemented, "MCP-OP-ADMIT-044" :implemented, "MCP-OP-ALIAS-058" :implemented, "WTL-APPLY-001" :implemented, "MCP-OP-ADMIT-122" :implemented, "MCP-OP-CENSUS-011" :implemented, "NS-SPLIT-070" :implemented, "RENAME-ALIAS-015" :implemented, "MCP-OP-ADMIT-052" :implemented, "WTL-PRUNE-007" :implemented, "MCP-OP-ALIAS-048" :implemented, "NS-SPLIT-039" :implemented, "MCP-OP-CENSUS-002" :implemented, "MCP-OP-ADMIT-101" :implemented, "MCP-OP-EDIT-021" :implemented, "MCP-OP-FIELD-009" :implemented, "MCP-OP-THREAD-024" :implemented, "PERF-SENT-VERDICT-001" :active-gap, "NS-SPLIT-066" :implemented, "MCP-OP-CENSUS-014" :implemented, "MCP-OP-HELPER-015" :active-gap, "MCP-OP-PREP-ACT-006" :active-gap, "MCP-OP-MEM-013" :implemented, "WTL-APPLY-011" :implemented, "TEST-ISO-004" :implemented, "WTL-CLI-001" :implemented, "MCP-OP-EDIT-036" :implemented, "RENAME-ALIAS-013" :implemented, "MCP-OP-ADMIT-115" :implemented, "WTL-PRUNE-009" :implemented, "OP-ALG-OUTCOME-001" :implemented, "MCP-OP-RELAY-005" :implemented, "MCP-OP-VERIFY-012" :implemented, "MCP-OP-PREP-ACT-012" :active-gap, "PERF-SENT-ADMIT-001" :active-gap, "MCP-OP-TRACE-004" :implemented, "MCP-OP-WRITE-REFUSAL-005" :deferred, "MCP-OP-THREAD-022" :implemented, "MCP-OP-FIELD-005" :implemented, "MCP-OP-ADMIT-123" :implemented, "MCP-OP-READ-RETRY-002" :deferred, "TEST-ISO-012" :active-gap, "NS-SPLIT-006" :implemented, "MCP-OP-TIME-001" :implemented, "MEASURE-WALL-001" :active-gap, "NS-SPLIT-028" :implemented, "MCP-OP-CENSUS-015" :implemented, "MCP-OP-EDIT-020" :implemented, "NS-SPLIT-068" :implemented, "MCP-OP-CENSUS-025" :implemented, "MCP-OP-ASYNC-004" :implemented, "MCP-OP-THREAD-004" :implemented, "WTL-INV-005" :implemented, "MCP-OP-HELPER-008" :active-gap, "OP-ALG-PARITY-002" :implemented, "INSERT-FORMS-002" :implemented, "MCP-OP-ALIAS-043" :implemented, "MCP-OP-ALIAS-052" :implemented, "MCP-OP-INSERT-008" :implemented, "MCP-OP-THREAD-002" :implemented, "MCP-OP-EDIT-017" :implemented, "NS-SPLIT-030" :implemented, "ROUTING-PARITY-001" :implemented, "WTL-SEAL-007" :implemented, "WTL-PLAN-006" :implemented, "MCP-OP-ALIAS-008" :implemented, "RENAME-ALIAS-002" :implemented, "WTL-INV-003" :implemented, "MCP-OP-ALIAS-012" :implemented, "MCP-OP-RESULT-001" :implemented, "MCP-OP-VERIFY-011" :implemented, "MCP-OP-POS-AUTH-008" :implemented, "WTL-CLI-002" :implemented, "NS-SPLIT-003" :implemented, "MCP-OP-ALIAS-034" :implemented, "MCP-OP-THREAD-003" :implemented, "MCP-OP-RESULT-004" :implemented, "MCP-OP-TIME-002" :implemented, "MCP-OP-ADMIT-087" :implemented, "MCP-OP-MATCH-001" :implemented, "MCP-OP-EDIT-010" :implemented, "MCP-OP-ADMIT-011" :implemented, "MCP-OP-ADMIT-083" :implemented, "MCP-OP-HELPER-024" :active-gap, "PERF-SENT-PUBLISH-001" :active-gap, "OP-ALG-IDENTITY-001" :implemented, "MCP-OP-PLAN-010" :implemented, "PERF-SENT-CUTOVER-001" :active-gap, "MCP-OP-HELPER-017" :active-gap, "MCP-OP-MATCH-003" :implemented, "WTL-SEAL-003" :implemented, "MCP-OP-PREP-REQ-002" :implemented, "TEST-ISO-006" :implemented, "MCP-OP-CENSUS-026" :implemented, "MCP-OP-READ-RETRY-001" :deferred, "WTL-INV-008" :implemented, "MCP-OP-ANALYZER-001" :implemented, "WTL-PRUNE-003" :implemented, "WTL-APPLY-005" :implemented, "MCP-OP-RELAY-004" :implemented, "NS-SPLIT-009" :implemented, "NS-SPLIT-010" :implemented, "MCP-OP-ADMIT-117" :implemented, "OP-ALG-PERF-002" :implemented, "TEST-ISO-013" :implemented, "MCP-OP-THREAD-016" :implemented, "MCP-OP-ALIAS-027" :implemented, "PERF-SENT-RECOVERY-001" :active-gap, "REQUIRE-CHANGE-006" :implemented, "MCP-OP-ADMIT-095" :implemented, "MCP-OP-ADMIT-010" :implemented, "MCP-OP-FIELD-006" :implemented, "ALIAS-MIGRATION-002" :implemented, "RENAME-ALIAS-016" :implemented, "MCP-OP-ADMIT-042" :implemented, "MCP-OP-ALIAS-045" :implemented, "MCP-OP-THREAD-025" :implemented, "MCP-OP-ADMIT-093" :implemented, "MCP-OP-ALIAS-056" :implemented, "MCP-OP-TMPHYG-002" :implemented, "MCP-OP-ADMIT-131" :implemented, "MCP-OP-VERIFY-002" :implemented, "WTL-APPLY-006" :implemented, "MCP-OP-MEM-014" :implemented, "MCP-OP-RESULT-002" :implemented, "MCP-OP-EDIT-029" :implemented, "WTL-CLI-004" :deferred, "MCP-OP-PLAN-008" :implemented, "MCP-OP-ADMIT-031" :implemented, "MCP-OP-ADMIT-129" :implemented, "OP-ALG-RECEIPT-003" :implemented, "MCP-OP-ADMIT-041" :implemented, "MCP-OP-CENSUS-007" :implemented, "MCP-OP-PREP-ACT-016" :active-gap, "MCP-OP-ADMIT-124" :implemented, "WTL-PLAN-005" :implemented, "MCP-OP-FIELD-007" :implemented, "TEST-ISO-007" :implemented, "MCP-OP-MEM-005" :implemented, "MCP-OP-ASYNC-003" :implemented, "MCP-OP-ADMIT-138" :implemented, "MCP-OP-INSERT-006" :active-gap, "PERF-SENT-LEDGER-001" :active-gap, "MCP-OP-ALIAS-010" :implemented, "MCP-OP-ADMIT-140" :implemented, "MCP-OP-ADMIT-043" :implemented, "OP-ALG-RECEIPT-001" :implemented, "PERF-SENT-RECONCILE-001" :active-gap, "MCP-OP-ALIAS-066" :active-gap, "PERF-SENT-RECOVERY-004" :active-gap, "MCP-OP-THREAD-010" :implemented, "PERF-SENT-RECOVERY-003" :active-gap, "PERF-SENT-THRESHOLD-001" :active-gap, "MCP-OP-ADMIT-085" :implemented, "MCP-OP-THREAD-017" :implemented, "WTL-PLAN-001" :implemented, "MCP-OP-CENSUS-032" :implemented, "NS-SPLIT-050" :implemented, "MCP-OP-ANALYZER-005" :implemented, "OP-ALG-PREVIEW-001" :implemented, "WTL-PLAN-002" :implemented, "MCP-OP-READ-DIAG-002" :implemented, "MCP-OP-ADMIT-086" :implemented, "MCP-OP-HELPER-025" :active-gap, "MCP-OP-ADMIT-134" :implemented, "OP-ALG-PERF-001" :implemented, "MCP-OP-ALIAS-031" :implemented, "OP-ALG-MCP-001" :implemented, "TEST-ISO-RACE-001" :implemented, "MEASURE-WALL-003" :active-gap, "MCP-OP-READ-DIAG-003" :implemented, "MCP-OP-EDIT-005" :implemented, "MCP-OP-ADMIT-014" :deferred, "MCP-OP-ADMIT-102" :implemented, "TEST-ISO-001b" :implemented, "MCP-OP-PLAN-006" :implemented, "MCP-OP-THREAD-043" :implemented, "PERF-SENT-INVALID-002" :active-gap, "REQUIRE-CHANGE-014" :implemented, "INSERT-FORMS-024" :implemented, "WTL-PRUNE-006" :implemented, "PERF-SENT-ADMIT-002" :active-gap, "MCP-OP-PLAN-007" :implemented, "BB-PROBE-003" :implemented, "MCP-OP-THREAD-049" :implemented, "MCP-OP-ALIAS-042" :implemented, "MCP-OP-THREAD-005" :implemented, "OP-ALG-COMMIT-002" :implemented, "MCP-OP-ADMIT-114" :implemented, "TEST-ISO-005" :implemented, "MCP-OP-ADMIT-013" :implemented, "MCP-OP-TIME-004" :implemented, "MCP-OP-THREAD-030" :implemented, "MCP-OP-ALIAS-011" :implemented, "MCP-OP-PREP-ACT-011" :active-gap, "MCP-OP-POS-AUTH-006" :implemented, "CLI-PACKAGE-002" :implemented, "MCP-OP-PREP-REQ-001" :implemented, "MCP-OP-ALIAS-064" :active-gap, "MCP-OP-ADMIT-054" :implemented, "NS-SPLIT-011" :implemented, "MCP-OP-READ-NORM-002" :active-gap, "MCP-OP-EDIT-042" :implemented, "MCP-OP-PREP-ACT-008" :active-gap, "MCP-OP-ORACLE-001" :implemented, "WTL-APPLY-012" :implemented, "MCP-OP-EDIT-031" :implemented, "NS-SPLIT-014" :implemented, "MCP-OP-EDIT-026" :implemented, "MCP-OP-PREP-REQ-009" :implemented, "ROUTING-SPLIT-001" :implemented, "MCP-OP-SHELL-ARGV-004" :implemented, "MCP-OP-ADMIT-068" :implemented, "NS-SPLIT-037" :implemented}, :implementation-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "OP-ALG-RUNTIME-001" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "TEST-ISO-009" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "OP-ALG-OUTCOME-002" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "OP-ALG-CONTEXT-002" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "MCP-OP-ADMIT-084" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "MCP-OP-PREP-REQ-002" "MCP-OP-CENSUS-026" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "OP-ALG-RECEIPT-003" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "OP-ALG-MCP-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "OP-ALG-COMMIT-002" "MCP-OP-ADMIT-114" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :test-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "WTL-APPLY-007" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "OP-ALG-EFFECT-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "MCP-OP-ADMIT-152" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-ADMIT-147" "TEST-ISO-RACE-002" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "WTL-SEAL-006" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "WTL-HAND-005" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "WTL-APPLY-002" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "WTL-CLI-003" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "MCP-OP-ANALYZER-006" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "WTL-HAND-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "OP-ALG-STALE-001" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "WTL-INV-002" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "TEST-ISO-010" "WTL-PLAN-004" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "WTL-APPLY-004" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "WTL-SEAL-004" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "WTL-APPLY-010" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "WTL-INV-006" "MCP-OP-ADMIT-084" "WTL-APPLY-003" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "MCP-OP-HELPER-023" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "OP-ALG-EFFECT-003" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "OP-ALG-SHADOW-001" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "WTL-APPLY-008" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "WTL-PLAN-003" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "TEST-ISO-003" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "WTL-SEAL-002" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-APPLY-011" "TEST-ISO-004" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "OP-ALG-OUTCOME-001" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "WTL-INV-005" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "WTL-SEAL-007" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "WTL-INV-003" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "WTL-CLI-002" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "WTL-SEAL-003" "MCP-OP-PREP-REQ-002" "TEST-ISO-006" "MCP-OP-CENSUS-026" "WTL-INV-008" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "WTL-APPLY-006" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "TEST-ISO-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "TEST-ISO-RACE-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "MCP-OP-ADMIT-114" "TEST-ISO-005" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ALIAS-064" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "WTL-APPLY-012" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :violations [], :pending-witness-violations [{:type :missing-test-witness, :intent "MEASURE-EVID-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-002", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-003", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-COMMIT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-CONTEXT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-DECODE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-DECODE-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-EFFECT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-MCP-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-REFUSE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-REFUSE-001", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RUNTIME-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-SHADOW-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-STALE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BACKFILL-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BASELINE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CLIENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CONFIG-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-ROOT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROJECT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECONCILE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RELEASE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RETAIN-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SURFACE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-THRESHOLD-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-TIME-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001a", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001a", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001b", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001b", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001c", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001c", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-007", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-008", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-009", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-010", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-011", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-012", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-007", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-010", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-011", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-012", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-004", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-HAND-004", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-HAND-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-002", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-002", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PLAN-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-006", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-006", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-008", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-008", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-009", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-009", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-SEAL-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-007", :source-kind :implementation}], :witness-debt-ledger {"WTL-APPLY-007" #{:implementation}, "PERF-SENT-IMPORT-001" #{:test}, "OP-ALG-EFFECT-002" #{:implementation}, "TEST-ISO-RACE-002" #{:implementation}, "OP-ALG-PREVIEW-002" #{:implementation :test}, "PERF-SENT-VERDICT-002" #{:test}, "PERF-SENT-AUTH-003" #{:test}, "PERF-SENT-VERDICT-003" #{:test}, "PERF-SENT-PROJECT-001" #{:test}, "TEST-ISO-001a" #{:implementation :test}, "PERF-SENT-LEDGER-004" #{:test}, "WTL-SEAL-006" #{:implementation}, "PERF-SENT-RELEASE-001" #{:test}, "WTL-HAND-005" #{:implementation}, "PERF-SENT-TIME-001" #{:test}, "MEASURE-EVID-001" #{:test}, "WTL-APPLY-002" #{:implementation}, "PERF-SENT-SCHEDULE-002" #{:test}, "PERF-SENT-CONFIG-001" #{:test}, "WTL-CLI-003" #{:implementation}, "OP-ALG-RUNTIME-001" #{:test}, "WTL-HAND-002" #{:implementation}, "PERF-SENT-CUTOVER-002" #{:test}, "OP-ALG-STALE-001" #{:implementation}, "WTL-INV-002" #{:implementation}, "PERF-SENT-AUTH-001" #{:test}, "TEST-ISO-010" #{:implementation}, "WTL-PLAN-004" #{:implementation}, "WTL-PRUNE-008" #{:implementation :test}, "PERF-SENT-LEDGER-003" #{:test}, "WTL-APPLY-004" #{:implementation}, "TEST-ISO-009" #{:test}, "PERF-SENT-SCHEDULE-003" #{:test}, "OP-ALG-EFFECT-004" #{:implementation :test}, "OP-ALG-DECODE-001" #{:implementation :test}, "PERF-SENT-BASELINE-001" #{:test}, "PERF-SENT-AUTH-002" #{:test}, "PERF-SENT-CUTOVER-003" #{:test}, "OP-ALG-OUTCOME-003" #{:implementation :test}, "TEST-ISO-001c" #{:implementation :test}, "PERF-SENT-RETAIN-001" #{:test}, "PERF-SENT-CLIENT-001" #{:test}, "WTL-SEAL-004" #{:implementation}, "PERF-SENT-RECEIPT-001" #{:test}, "PERF-SENT-INVALID-001" #{:test}, "OP-ALG-OUTCOME-002" #{:test}, "WTL-APPLY-010" #{:implementation}, "PERF-SENT-RECOVERY-002" #{:test}, "OP-ALG-REFUSE-001" #{:implementation :test}, "PERF-SENT-IDENT-001" #{:test}, "PERF-SENT-SCHEDULE-001" #{:test}, "MEASURE-WALL-002" #{:test}, "PERF-SENT-PROMOTION-002" #{:test}, "TEST-ISO-008" #{:test}, "PERF-SENT-SCHEDULE-004" #{:test}, "WTL-HAND-004" #{:implementation :test}, "OP-ALG-COMMIT-004" #{:implementation :test}, "PERF-SENT-IMPORT-002" #{:test}, "PERF-SENT-BACKFILL-001" #{:test}, "PERF-SENT-LEDGER-002" #{:test}, "TEST-ISO-011" #{:test}, "PERF-SENT-PUBLISH-002" #{:test}, "PERF-SENT-LEDGER-ROOT-001" #{:test}, "OP-ALG-CONTEXT-002" #{:test}, "WTL-INV-006" #{:implementation}, "WTL-APPLY-003" #{:implementation}, "PERF-SENT-PROMOTION-001" #{:test}, "PERF-SENT-SURFACE-001" #{:test}, "PERF-SENT-ATTEMPT-003" #{:test}, "PERF-SENT-ATTEMPT-001" #{:test}, "OP-ALG-RECEIPT-002" #{:implementation :test}, "OP-ALG-EFFECT-003" #{:implementation}, "OP-ALG-SHADOW-001" #{:implementation}, "OP-ALG-PARITY-001" #{:implementation :test}, "WTL-APPLY-008" #{:implementation}, "PERF-SENT-RECEIPT-002" #{:test}, "WTL-PLAN-003" #{:implementation}, "PERF-SENT-ATTEMPT-002" #{:test}, "TEST-ISO-003" #{:implementation}, "WTL-SEAL-002" #{:implementation}, "PERF-SENT-IDENT-002" #{:test}, "PERF-SENT-VERDICT-001" #{:test}, "WTL-APPLY-011" #{:implementation}, "TEST-ISO-004" #{:implementation}, "WTL-PRUNE-009" #{:implementation :test}, "OP-ALG-OUTCOME-001" #{:implementation}, "PERF-SENT-ADMIT-001" #{:test}, "TEST-ISO-012" #{:test}, "MEASURE-WALL-001" #{:test}, "WTL-INV-005" #{:implementation}, "OP-ALG-PARITY-002" #{:implementation :test}, "WTL-SEAL-007" #{:implementation}, "WTL-PLAN-006" #{:implementation :test}, "WTL-INV-003" #{:implementation}, "WTL-CLI-002" #{:implementation}, "PERF-SENT-PUBLISH-001" #{:test}, "PERF-SENT-CUTOVER-001" #{:test}, "WTL-SEAL-003" #{:implementation}, "TEST-ISO-006" #{:implementation}, "WTL-INV-008" #{:implementation}, "OP-ALG-PERF-002" #{:implementation :test}, "PERF-SENT-RECOVERY-001" #{:test}, "WTL-APPLY-006" #{:implementation}, "OP-ALG-RECEIPT-003" #{:test}, "TEST-ISO-007" #{:implementation}, "PERF-SENT-LEDGER-001" #{:test}, "OP-ALG-RECEIPT-001" #{:implementation :test}, "PERF-SENT-RECONCILE-001" #{:test}, "PERF-SENT-RECOVERY-004" #{:test}, "PERF-SENT-RECOVERY-003" #{:test}, "PERF-SENT-THRESHOLD-001" #{:test}, "OP-ALG-PREVIEW-001" #{:implementation :test}, "WTL-PLAN-002" #{:implementation :test}, "OP-ALG-PERF-001" #{:implementation :test}, "OP-ALG-MCP-001" #{:test}, "TEST-ISO-RACE-001" #{:implementation}, "MEASURE-WALL-003" #{:test}, "TEST-ISO-001b" #{:implementation :test}, "PERF-SENT-INVALID-002" #{:test}, "PERF-SENT-ADMIT-002" #{:test}, "OP-ALG-COMMIT-002" #{:test}, "TEST-ISO-005" #{:implementation}, "WTL-APPLY-012" #{:implementation}}}
gate-stage: {:target "intent-audit", :exit 0, :wall-ms 2142}
landing-gate: {:pool {:target "runtime-pool", :exit 0, :wall-ms 156828, :phases [{:phase 0, :process-count 15, :wall-ms 25836} {:phase 1, :process-count 23, :wall-ms 130938}], :preparation {:command ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"], :exit 0, :wall-ms 53}, :peak-worker-count 8, :shell-checks {:namespaces [], :index 0, :exit 0, :started-ms 1789233106383, :phase 1, :err-log "target/gate-prewarm/6d3a79ba-885f-47d7-9eaa-aa20ba626062/lane-0.err", :runtime nil, :wall-ms 130938, :suite "shell", :completed-ms 1789233237321, :log "target/gate-prewarm/6d3a79ba-885f-47d7-9eaa-aa20ba626062/lane-0.out"}}, :stages [{:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11019} {:target "alias-migration-test", :exit 0, :wall-ms 87959} {:target "mcp-test", :exit 0, :wall-ms 91087} {:target "test-bb", :exit 0, :wall-ms 90742} {:target "test-bb-diagnostic", :exit 0, :wall-ms 224615} {:target "repository-hygiene", :exit 0, :wall-ms 2087} {:target "intent-audit", :exit 0, :wall-ms 2142}], :landing? false, :problems [], :capacity {:lanes 8, :memory-mib 25286, :admission :abstract-socket, :lane-charge-mib 1536, :reserve-mib 2048, :slot-root "abstract", :floor-mib 3584, :heap-mib 512, :cpus 16}, :started-at "2026-09-12T17:11:09.141179139Z", :state :passed, :git-tree "a3aa5e7eea0c9e0fc572abc74a7c8cf2653dd1b8", :wall-ms 397327, :completed-at "2026-09-12T17:17:46.469234990Z", :source-digest "23390c9bfa92b3095f591e3d6473f4b11057cde9cd8c6cb4ad087dcaeeebe297", :prewarm? true, :run-id "6d3a79ba-885f-47d7-9eaa-aa20ba626062", :git-head "09588a14a784e220907462f46676900402bc80aa"}
