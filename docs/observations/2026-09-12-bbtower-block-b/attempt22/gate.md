STARTED=2026-09-12T19:39:14Z
3fdd5af477c0d282a0da818e837bf6f3c16624b1
TMPDIR=/var/tmp/forge/bbtower-fx JAVA_TOOL_OPTIONS=-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx make landing-gate-prewarm
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 25190 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T19:39:16.543596639Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=119
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=145
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=168
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11091}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 76823 ms
  process 0 phase 0 (est 4386 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 76823 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 47993 ms
  process 0 phase 0 (est 4040 ms): clj-surgeon.outline-test clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.mission-forms-test clj-surgeon.forms-test clj-surgeon.mission-candidate-test
  process 1 phase 0 (est 1233 ms): clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-compact-edit-test
  process 2 phase 0 (est 5057 ms): clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  process 3 phase 0 (est 216 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.memory-battery-test clj-surgeon.battery-parallel-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.mcp-compact-edit-fields-test
  process 4 phase 0 (est 4165 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test
  process 5 phase 0 (est 1108 ms): clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test
  process 6 phase 0 (est 4107 ms): clj-surgeon.insert-forms-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test
  process 7 phase 0 (est 1166 ms): clj-surgeon.helper-extraction-test clj-surgeon.ls-tree-test clj-surgeon.structural-lens-test clj-surgeon.telemetry-events-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test
  process 8 phase 0 (est 4397 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test
  process 9 phase 0 (est 876 ms): clj-surgeon.analyze-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mission-plain-forms-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-recovery-test
  process 10 phase 0 (est 4730 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.scope-stream-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-formatter-test
  process 11 phase 0 (est 543 ms): clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.syntax-var-refs-test
  process 12 phase 0 (est 5145 ms): clj-surgeon.rename-alias-test clj-surgeon.mcp-operation-test
  process 13 phase 0 (est 128 ms): clj-surgeon.relation-census-test clj-surgeon.require-change-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 14 phase 0 (est 8535 ms): clj-surgeon.mcp-compact-relations-test
  process 15 phase 1 (est 250 ms): clj-surgeon.mcp-server-test
  process 16 phase 1 (est 2018 ms): clj-surgeon.namespace-split-warm-test
  process 17 phase 1 (est 2923 ms): clj-surgeon.mcp-tool-test
  process 18 phase 1 (est 3042 ms): clj-surgeon.mcp-http-server-test
  process 19 phase 1 (est 3473 ms): clj-surgeon.mcp-hot-verify-test
  process 20 phase 1 (est 9200 ms): clj-surgeon.outline-corpus-integration-test
  process 21 phase 1 (est 47993 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 90293 ms
  process 0 phase 0 (est 5839 ms): clj-surgeon.intent-transaction-test
  process 1 phase 0 (est 7141 ms): clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.platform-selector-test
  process 2 phase 0 (est 806 ms): clj-surgeon.analyze-test clj-surgeon.fix-declares-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.failure-report-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.analyze-test
  process 3 phase 0 (est 13484 ms): clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test
  process 4 phase 0 (est 303 ms): clj-surgeon.jvm-error-test clj-surgeon.move-dependency-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.move-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test
  process 5 phase 0 (est 11833 ms): clj-surgeon.help-test clj-surgeon.edn-config-integration-test
  process 6 phase 0 (est 1954 ms): clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test
  process 7 phase 0 (est 13447 ms): clj-surgeon.show-form-test
  process 8 phase 0 (est 341 ms): clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.agent-routing-test clj-surgeon.cljc.split-test clj-surgeon.forms-test
  process 9 phase 0 (est 13643 ms): clj-surgeon.tmp-leak-support-test
  process 10 phase 0 (est 143 ms): clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 11 phase 0 (est 13944 ms): clj-surgeon.cli-dispatch-test
  process 12 phase 0 (est 72430 ms): clj-surgeon.parser-admission-test
  process 13 phase 0 (est 90293 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 15016 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4286 ms):
      4286 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 90931 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 78581 ms):
     78581 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3655 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 90932 ms
cadence :battery: serial-equivalent 82867 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 82867 ms):
     78581 ms  clj-surgeon.mcp-alias-migration-test
      4286 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 90932 ms:
  lane 0     15016 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     90931 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 90932 ms over 2 lane(s); serial-equivalent 82867 ms; skipped 0

========== lane 0 (clj-surgeon.outline-test clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.mission-forms-test clj-surgeon.forms-test clj-surgeon.mission-candidate-test) exit 1, 4287 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.lane-manifest-test

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.cljc.merge-test, :reason :runtime-control-failed, :controls {:jvm {:status :test-failed, :result {:test 14, :pass 19, :fail 0, :error 1, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.cljc.merge-test-jvm-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.cljc.split-test, :reason :runtime-control-failed, :controls {:jvm {:status :test-failed, :result {:test 11, :pass 29, :fail 1, :error 1, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.cljc.split-test-jvm-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.mcp-cold-verify-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 7, :pass 50, :fail 0, :error 1, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-cold-verify-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.mcp-namespace-split-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 27, :pass 198, :fail 0, :error 1, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-namespace-split-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.mcp-relation-census-launcher-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 5, :pass 141, :fail 38, :error 1, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-relation-census-launcher-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.memory.journal-green-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 2, :pass 0, :fail 0, :error 2, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.memory.journal-green-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.memory.oom-reproduction-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 2, :pass 0, :fail 0, :error 2, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.memory.oom-reproduction-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.mission-display-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 14, :pass 106, :fail 0, :error 3, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-display-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.ns-isolation-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 27, :pass 161, :fail 0, :error 3, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.ns-isolation-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.reader-eval-fence-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 13, :pass 69, :fail 8, :error 0, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.reader-eval-fence-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.worktree-lifecycle-prune-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 24, :pass 23, :fail 0, :error 5, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.worktree-lifecycle-prune-test-bb-test.control.edn"}, :jvm {:status :test-failed, :result {:test 24, :pass 23, :fail 0, :error 5, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.worktree-lifecycle-prune-test-jvm-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

FAIL in (runtime-portability-controls-cover-every-assignment) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/lane_manifest_test.clj:246)
{:error-type :non-portable-namespace, :namespace clj-surgeon.worktree-lifecycle-recovery-test, :reason :runtime-control-failed, :controls {:bb {:status :test-failed, :result {:test 10, :pass 26, :fail 5, :error 9, :type :summary}, :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.worktree-lifecycle-recovery-test-bb-test.control.edn"}}}
expected: (or (nil? refusal) unsupported-jvm-only?)
  actual: false

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.mission-forms-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.mission-candidate-test

========== lane 1 (clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-compact-edit-test) exit 0, 17978 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 1921 ms):
      1220 ms  clj-surgeon.rename-alias-receipt-test
       503 ms  clj-surgeon.mcp-create-files-test
       198 ms  clj-surgeon.mcp-compact-edit-test

========== lane 2 (clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test) exit 0, 23236 ms ==========
lanes: --ns -- 7 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-prepared-request-test

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
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (7, slowest first, total 5350 ms):
      1810 ms  clj-surgeon.ns-isolation-test
      1355 ms  clj-surgeon.splice-envelope-test
       944 ms  clj-surgeon.insert-forms-receipt-test
       513 ms  clj-surgeon.mcp-change-buffer-test
       325 ms  clj-surgeon.mcp-prepared-request-test
       224 ms  clj-surgeon.mcp-relation-census-round20-test
       179 ms  clj-surgeon.mission-candidate-race-test

========== lane 3 (clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.memory-battery-test clj-surgeon.battery-parallel-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 798 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mcp-read-request-normalization-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test) exit 0, 22449 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 4288 ms):
      2998 ms  clj-surgeon.mcp-inspect-tool-test
       484 ms  clj-surgeon.outline-memory-test
       445 ms  clj-surgeon.mcp-compact-location-test
       191 ms  clj-surgeon.mcp-extraction-plan-test
       170 ms  clj-surgeon.mcp-semantic-client-test

========== lane 5 (clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test) exit 0, 1823 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.cljc.analyze-test

========== lane 6 (clj-surgeon.insert-forms-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test) exit 0, 23797 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-prepared-confirmation-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 3664 ms):
      2615 ms  clj-surgeon.insert-forms-test
       478 ms  clj-surgeon.receipt-booleans-test
       310 ms  clj-surgeon.census-pool-test
       261 ms  clj-surgeon.mcp-prepared-confirmation-test

========== lane 7 (clj-surgeon.helper-extraction-test clj-surgeon.ls-tree-test clj-surgeon.structural-lens-test clj-surgeon.telemetry-events-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test) exit 0, 1855 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mission-usage-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test) exit 0, 22091 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 4537 ms):
      3755 ms  clj-surgeon.mcp-namespace-split-test
       337 ms  clj-surgeon.mcp-expect-guard-test
       262 ms  clj-surgeon.mcp-combinable-transaction-test
       183 ms  clj-surgeon.mcp-operation-async-test

========== lane 9 (clj-surgeon.analyze-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mission-plain-forms-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-recovery-test) exit 0, 1233 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.mcp-recovery-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.scope-stream-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-formatter-test) exit 0, 25503 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 5707 ms):
      4845 ms  clj-surgeon.mcp-operation-registry-test
       384 ms  clj-surgeon.scope-stream-test
       304 ms  clj-surgeon.mcp-write-refusal-test
       174 ms  clj-surgeon.mcp-formatter-test

========== lane 11 (clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.syntax-var-refs-test) exit 0, 1121 ms ==========

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 12 (clj-surgeon.rename-alias-test clj-surgeon.mcp-operation-test) exit 0, 18521 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test

Testing clj-surgeon.mcp-operation-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (2, slowest first, total 5023 ms):
      4718 ms  clj-surgeon.rename-alias-test
       305 ms  clj-surgeon.mcp-operation-test

========== lane 13 (clj-surgeon.relation-census-test clj-surgeon.require-change-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 563 ms ==========

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.move-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.split-proof-gate-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 14 (clj-surgeon.mcp-compact-relations-test) exit 0, 25481 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9044 ms):
      9044 ms  clj-surgeon.mcp-compact-relations-test

========== lane 15 (clj-surgeon.mcp-server-test) exit 0, 10853 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 34815 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2574684-ff8905b2/clj-surgeon-mcp-server-test-6036662829848807430/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 38699 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2574684-ff8905b2/clj-surgeon-mcp-server-test-14118648008901256074/.nrepl-port )

namespace walls (1, slowest first, total 273 ms):
       273 ms  clj-surgeon.mcp-server-test

========== lane 16 (clj-surgeon.namespace-split-warm-test) exit 0, 11609 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1887 ms):
      1887 ms  clj-surgeon.namespace-split-warm-test

========== lane 17 (clj-surgeon.mcp-tool-test) exit 0, 14471 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2944 ms):
      2944 ms  clj-surgeon.mcp-tool-test

========== lane 18 (clj-surgeon.mcp-http-server-test) exit 0, 16099 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 36615 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2567788-2dc2489e/clj-surgeon-mcp-http-test-13160626496187096419/.nrepl-port )

namespace walls (1, slowest first, total 2585 ms):
      2585 ms  clj-surgeon.mcp-http-server-test

========== lane 19 (clj-surgeon.mcp-hot-verify-test) exit 0, 8729 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 3457 ms):
      3457 ms  clj-surgeon.mcp-hot-verify-test

========== lane 20 (clj-surgeon.outline-corpus-integration-test) exit 0, 15314 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9495 ms):
      9495 ms  clj-surgeon.outline-corpus-integration-test

========== lane 21 (clj-surgeon.mcp-feature-thread-test) exit 0, 55789 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 21 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 48585 ms):
     48585 ms  clj-surgeon.mcp-feature-thread-test

Ran 1416 tests containing 16401 assertions.
12 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 94643 ms
cadence :fast: serial-equivalent 47143 ms; budget 60000 ms
cadence :integration: serial-equivalent 69226 ms; budget 240000 ms
bb-runtime: serial-equivalent 7609 ms; budget 343102 ms; makespan 23325 ms

namespace walls (101, slowest first, serial-equivalent total 116369 ms):
     48585 ms  clj-surgeon.mcp-feature-thread-test
      9495 ms  clj-surgeon.outline-corpus-integration-test
      9044 ms  clj-surgeon.mcp-compact-relations-test
      4845 ms  clj-surgeon.mcp-operation-registry-test
      4718 ms  clj-surgeon.rename-alias-test
      3755 ms  clj-surgeon.mcp-namespace-split-test
      3457 ms  clj-surgeon.mcp-hot-verify-test
      2998 ms  clj-surgeon.mcp-inspect-tool-test
      2944 ms  clj-surgeon.mcp-tool-test
      2615 ms  clj-surgeon.insert-forms-test
      2585 ms  clj-surgeon.mcp-http-server-test
      1887 ms  clj-surgeon.namespace-split-warm-test
      1810 ms  clj-surgeon.ns-isolation-test
      1421 ms  clj-surgeon.lane-manifest-test
      1355 ms  clj-surgeon.splice-envelope-test
      1220 ms  clj-surgeon.rename-alias-receipt-test
       944 ms  clj-surgeon.insert-forms-receipt-test
       941 ms  clj-surgeon.outline-test
       873 ms  clj-surgeon.helper-extraction-test
       827 ms  clj-surgeon.operation-algebra-test
       823 ms  clj-surgeon.namespace-split-test
       702 ms  clj-surgeon.analyze-test
       513 ms  clj-surgeon.mcp-change-buffer-test
       503 ms  clj-surgeon.mcp-create-files-test
       484 ms  clj-surgeon.outline-memory-test
       478 ms  clj-surgeon.receipt-booleans-test
       445 ms  clj-surgeon.mcp-compact-location-test
       384 ms  clj-surgeon.scope-stream-test
       337 ms  clj-surgeon.mcp-expect-guard-test
       325 ms  clj-surgeon.mcp-prepared-request-test
       310 ms  clj-surgeon.census-pool-test
       305 ms  clj-surgeon.mcp-inspect-contract-test
       305 ms  clj-surgeon.mcp-operation-test
       304 ms  clj-surgeon.mcp-write-refusal-test
       273 ms  clj-surgeon.mcp-server-test
       262 ms  clj-surgeon.mcp-combinable-transaction-test
       261 ms  clj-surgeon.mcp-prepared-confirmation-test
       224 ms  clj-surgeon.mcp-relation-census-round20-test
       223 ms  clj-surgeon.mcp-intent-contract-test
       198 ms  clj-surgeon.mcp-compact-edit-test
       191 ms  clj-surgeon.mcp-extraction-plan-test
       183 ms  clj-surgeon.mcp-operation-async-test
       179 ms  clj-surgeon.mission-candidate-race-test
       174 ms  clj-surgeon.mcp-formatter-test
       170 ms  clj-surgeon.mcp-semantic-client-test
       169 ms  clj-surgeon.alias-migration-test
       121 ms  clj-surgeon.ls-tree-test
       118 ms  clj-surgeon.cljc-existing-ops-test
       117 ms  clj-surgeon.workspace-onboarding-test
        83 ms  clj-surgeon.jvm-error-test
        76 ms  clj-surgeon.structural-lens-test
        55 ms  clj-surgeon.owner-hypotheses-test
        50 ms  clj-surgeon.mcp-extraction-test
        50 ms  clj-surgeon.recovery-test
        49 ms  clj-surgeon.move-dependency-test
        45 ms  clj-surgeon.agent-routing-test
        44 ms  clj-surgeon.require-change-test
        42 ms  clj-surgeon.mcp-workspace-test
        42 ms  clj-surgeon.telemetry-events-test
        36 ms  clj-surgeon.fast-lane-isolation-test
        36 ms  clj-surgeon.mission-plain-forms-test
        35 ms  clj-surgeon.fix-declares-test
        33 ms  clj-surgeon.battery-parallel-test
        30 ms  clj-surgeon.battery-ledger-test
        29 ms  clj-surgeon.relation-census-test
        25 ms  clj-surgeon.worktree-lifecycle-io-test
        20 ms  clj-surgeon.mcp-program-tool-test
        19 ms  clj-surgeon.memory-battery-test
        19 ms  clj-surgeon.outermost-test
        15 ms  clj-surgeon.cljc.split-test
        15 ms  clj-surgeon.move-test
        14 ms  clj-surgeon.edit-dsl-test
        13 ms  clj-surgeon.worktree-lifecycle-test
        11 ms  clj-surgeon.quoted-var-refs-test
        10 ms  clj-surgeon.mcp-contract-test
         9 ms  clj-surgeon.insertion-gap-test
         7 ms  clj-surgeon.mcp-schema-test
         7 ms  clj-surgeon.mission-forms-source-test
         7 ms  clj-surgeon.mission-typist-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.outline-differential-test
         4 ms  clj-surgeon.syntax-var-refs-test
         3 ms  clj-surgeon.cljc.analyze-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.mcp-telemetry-test
         3 ms  clj-surgeon.split-proof-gate-test
         2 ms  clj-surgeon.mission-usage-test
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

lanes (22), makespan 94643 ms:
  lane 0      4287 ms  exit 1  clj-surgeon.outline-test clj-surgeon.lane-manifest-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-program-tool-test clj-surgeon.insertion-gap-test clj-surgeon.mission-forms-test clj-surgeon.forms-test clj-surgeon.mission-candidate-test
  lane 1     17978 ms  exit 0  clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-compact-edit-test
  lane 2     23236 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mission-candidate-race-test
  lane 3       798 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.memory-battery-test clj-surgeon.battery-parallel-test clj-surgeon.mcp-contract-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.mcp-compact-edit-fields-test
  lane 4     22449 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.outline-memory-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test
  lane 5      1823 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.extract-header-test clj-surgeon.cljc.analyze-test
  lane 6     23797 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.receipt-booleans-test clj-surgeon.census-pool-test clj-surgeon.mcp-prepared-confirmation-test
  lane 7      1855 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.ls-tree-test clj-surgeon.structural-lens-test clj-surgeon.telemetry-events-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.outermost-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.mission-usage-test
  lane 8     22091 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-operation-async-test
  lane 9      1233 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mission-plain-forms-test clj-surgeon.quoted-var-refs-test clj-surgeon.mcp-schema-test clj-surgeon.mcp-recovery-test
  lane 10     25503 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.scope-stream-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-formatter-test
  lane 11      1121 ms  exit 0  clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.split-test clj-surgeon.mcp-telemetry-test clj-surgeon.syntax-var-refs-test
  lane 12     18521 ms  exit 0  clj-surgeon.rename-alias-test clj-surgeon.mcp-operation-test
  lane 13       563 ms  exit 0  clj-surgeon.relation-census-test clj-surgeon.require-change-test clj-surgeon.move-test clj-surgeon.mission-typist-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 14     25481 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 15     10853 ms  exit 0  clj-surgeon.mcp-server-test
  lane 16     11609 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 17     14471 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 18     16099 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 19      8729 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 20     15314 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 21     55789 ms  exit 0  clj-surgeon.mcp-feature-thread-test

TEST-ISOLATION: 1 violation(s) -- the suite's own purity rules, per namespace:
   TEST-ISO-003 VIOLATION in clj-surgeon.rename-alias-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt22/battery-total.edn was created in the repository working tree

BATTERY-LANE: 1 lane failure(s):
   lane 0 exited 1 (log target/gate-prewarm/359b54dc-e3c8-4e9e-b332-99523e4534c5/mcp/lane-0.out)
battery-parallel: makespan 94643 ms over 8 lane(s); serial-equivalent 116369 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 15358 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 6677 ms):
      6677 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.platform-selector-test) exit 0, 7913 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.platform-selector-test

========== lane 2 (clj-surgeon.analyze-test clj-surgeon.fix-declares-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.failure-report-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.analyze-test) exit 0, 1124 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.analyze-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test) exit 0, 13787 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.partition-all-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543235-d87aed81/andon-shell-safety3607346990051762334/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543235-d87aed81/andon-shell-safety3607346990051762334/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543235-d87aed81/andon-shell-safety14134570082246247907/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543235-d87aed81/andon-shell-safety14134570082246247907/PWNED-SUBST)"

========== lane 4 (clj-surgeon.jvm-error-test clj-surgeon.move-dependency-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.move-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test) exit 0, 536 ms ==========

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.move-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.extract-header-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.edn-config-integration-test) exit 0, 12462 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.edn-config-integration-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test) exit 0, 2492 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 13847 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.agent-routing-test clj-surgeon.cljc.split-test clj-surgeon.forms-test) exit 0, 757 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.forms-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14220 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543231-3f203427/tmp-leak-sweep-guard-15288704917430250047/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 512 ms ==========

Testing clj-surgeon.recovery-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 11 (clj-surgeon.cli-dispatch-test) exit 0, 14641 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 12 (clj-surgeon.parser-admission-test) exit 0, 72938 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 13 (clj-surgeon.install-test) exit 0, 91808 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/bin with spaces/clj-surgeon from commit 3fdd5af477c0d282a0da818e837bf6f3c16624b1, source hash 75e2d4d51ae7e8bf8672046ad2f12c310ec0dd4fa587f6c89b1109e084b3f3e8
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x6ca21d12 "Process[pid=2554240, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x120accd2 "java.lang.ProcessImpl$ProcessPipeOutputStream@120accd2"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.197314, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x333c1d2f "Process[pid=2554375, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0xfc46973 "java.lang.ProcessImpl$ProcessPipeOutputStream@fc46973"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.290694, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x7a8b15b8 "Process[pid=2554542, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x7ffc4bca "java.lang.ProcessImpl$ProcessPipeOutputStream@7ffc4bca"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-2543221-58e5749d/clj-surgeon-installed-splice-10407336570017808817/sample.clj"]}

Ran 902 tests containing 8019 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91808 ms
cadence :battery: serial-equivalent 245521 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3409 ms; budget 60000 ms
bb-runtime: serial-equivalent 242253 ms; budget 343102 ms; makespan 91808 ms

namespace walls (50, slowest first, serial-equivalent total 248930 ms):
     91642 ms  clj-surgeon.install-test
     72697 ms  clj-surgeon.parser-admission-test
     14198 ms  clj-surgeon.cli-dispatch-test
     14062 ms  clj-surgeon.tmp-leak-support-test
     13415 ms  clj-surgeon.show-form-test
     11525 ms  clj-surgeon.help-test
      6677 ms  clj-surgeon.intent-transaction-test
      6158 ms  clj-surgeon.edit-test
      5655 ms  clj-surgeon.extract-test
      4400 ms  clj-surgeon.core-discovery-test
      2435 ms  clj-surgeon.lens-query-test
      1523 ms  clj-surgeon.xray-test
       893 ms  clj-surgeon.outline-test
       729 ms  clj-surgeon.operation-algebra-test
       691 ms  clj-surgeon.analyze-test
       529 ms  clj-surgeon.edn-config-integration-test
       323 ms  clj-surgeon.partition-all-test
       282 ms  clj-surgeon.platform-selector-test
       163 ms  clj-surgeon.alias-migration-test
       153 ms  clj-surgeon.cljc-existing-ops-test
       112 ms  clj-surgeon.workspace-onboarding-test
       109 ms  clj-surgeon.ls-tree-test
        76 ms  clj-surgeon.jvm-error-test
        68 ms  clj-surgeon.structural-lens-test
        61 ms  clj-surgeon.move-dependency-test
        47 ms  clj-surgeon.recovery-test
        40 ms  clj-surgeon.worktree-lifecycle-io-test
        34 ms  clj-surgeon.agent-routing-test
        34 ms  clj-surgeon.relation-census-test
        32 ms  clj-surgeon.fix-declares-test
        28 ms  clj-surgeon.owner-hypotheses-test
        21 ms  clj-surgeon.memory-battery-test
        20 ms  clj-surgeon.outermost-test
        19 ms  clj-surgeon.edit-dsl-test
        14 ms  clj-surgeon.move-test
        13 ms  clj-surgeon.quoted-var-refs-test
        11 ms  clj-surgeon.cljc.split-test
         9 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.insertion-gap-test
         5 ms  clj-surgeon.forms-test
         5 ms  clj-surgeon.rename-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         2 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.cljc.analyze-test
         1 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (14), makespan 91808 ms:
  lane 0     15358 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      7913 ms  exit 0  clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.platform-selector-test
  lane 2      1124 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.fix-declares-test clj-surgeon.relation-census-test clj-surgeon.edit-dsl-test clj-surgeon.failure-report-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.analyze-test
  lane 3     13787 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.partition-all-test
  lane 4       536 ms  exit 0  clj-surgeon.jvm-error-test clj-surgeon.move-dependency-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.memory-battery-test clj-surgeon.move-test clj-surgeon.cljc.merge-test clj-surgeon.extract-header-test
  lane 5     12462 ms  exit 0  clj-surgeon.help-test clj-surgeon.edn-config-integration-test
  lane 6      2492 ms  exit 0  clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.syntax-var-refs-test
  lane 7     13847 ms  exit 0  clj-surgeon.show-form-test
  lane 8       757 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.cljc-existing-ops-test clj-surgeon.agent-routing-test clj-surgeon.cljc.split-test clj-surgeon.forms-test
  lane 9     14220 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10       512 ms  exit 0  clj-surgeon.recovery-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.require-ops-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 11     14641 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 12     72938 ms  exit 0  clj-surgeon.parser-admission-test
  lane 13     91808 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 91808 ms over 8 lane(s); serial-equivalent 248930 ms; skipped 0
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2582811-cba41103 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2582811-cba41103 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2582811-cba41103
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583297-afb0961b node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583297-afb0961b node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583297-afb0961b
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583957-946dca11 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583957-946dca11 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/realdisk/clj-surgeon-suite-2583957-946dca11
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/subbase/clj-surgeon-suite-2584076-ae6ad444 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/subbase/clj-surgeon-suite-2584076-ae6ad444 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/subbase/clj-surgeon-suite-2584076-ae6ad444
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/subbase/clj-surgeon-suite-2584076-ae6ad444/tmp.16lRZ7KOoT
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/subbase/clj-surgeon-suite-2584076-ae6ad444: tmp.16lRZ7KOoT
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/stalebase/clj-surgeon-suite-2586676-c36617a7 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/stalebase/clj-surgeon-suite-2586676-c36617a7 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/stalebase/clj-surgeon-suite-2586676-c36617a7
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-2578379-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG/nowrite/clj-surgeon-suite-2588005-f9517356 Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.lQXSfG ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.CMYS80/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.CMYS80/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s.........
----------------------------------------------------------------------
Ran 21 tests in 0.384s

OK (skipped=2)
.
----------------------------------------------------------------------
Ran 1 test in 0.000s

OK
....
-------------------------------------------gate-refused: gate-refused: runtime pool failed {:shell-exit 0, :suites [{:suite "alias", :state :passed, :problems []} {:suite "mcp", :state :failed, :problems ["lane 0 exited 1 (log target/gate-prewarm/359b54dc-e3c8-4e9e-b332-99523e4534c5/mcp/lane-0.out)"]} {:suite "bb", :state :passed, :problems []}]}
make: *** [Makefile:1262: landing-gate-prewarm] Error 1
EXIT=2 WALL_S=171
