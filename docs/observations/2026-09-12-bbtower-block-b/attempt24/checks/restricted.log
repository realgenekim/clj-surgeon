STARTED=2026-09-12T20:50:19Z
58b33e652b5200232f5c4521b5202051e5a1b7eb
COMMAND: python3 docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py -- make landing-gate-prewarm
{"command": ["make", "landing-gate-prewarm"], "writable_roots": ["/home/forge/src/clj-surgeon-bbtower", "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp", "/home/forge/.local/state/clj-surgeon"], "writable_devices": ["/dev/null"], "started": 1789246219.827788, "diagnostic": true}
/home/forge/src/clj-surgeon-bbtower/docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py:35: DeprecationWarning: Due to '_pack_', the 'Rule' Structure will use memory layout compatible with MSVC (Windows). If this is intended, set _layout_ to 'ms'. The implicit default is deprecated and slated to become an error in Python 3.19.
  class Rule(ctypes.Structure):
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 24608 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T20:50:22.095550639Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=135
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=127
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=178
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11184}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 77578 ms
  process 0 phase 0 (est 4554 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 77578 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 48487 ms
  process 0 phase 0 (est 2359 ms): clj-surgeon.splice-envelope-test clj-surgeon.scope-stream-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test
  process 1 phase 0 (est 3153 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test
  process 2 phase 0 (est 4651 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-semantic-client-test
  process 3 phase 0 (est 860 ms): clj-surgeon.helper-extraction-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.split-proof-gate-test
  process 4 phase 0 (est 4294 ms): clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test
  process 5 phase 0 (est 1217 ms): clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.fast-lane-isolation-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test
  process 6 phase 0 (est 4525 ms): clj-surgeon.insert-forms-test clj-surgeon.mcp-compact-location-test clj-surgeon.census-pool-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test
  process 7 phase 0 (est 986 ms): clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 8 phase 0 (est 5277 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-formatter-test
  process 9 phase 0 (est 235 ms): clj-surgeon.alias-migration-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.move-test clj-surgeon.mission-forms-test clj-surgeon.rename-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-candidate-test
  process 10 phase 0 (est 5011 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-operation-test
  process 11 phase 0 (est 501 ms): clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.battery-parallel-test clj-surgeon.outermost-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-recovery-test clj-surgeon.mcp-read-request-normalization-test
  process 12 phase 0 (est 5329 ms): clj-surgeon.rename-alias-test clj-surgeon.mission-candidate-race-test
  process 13 phase 0 (est 184 ms): clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.mcp-workspace-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.mcp-compact-edit-fields-test
  process 14 phase 0 (est 8765 ms): clj-surgeon.mcp-compact-relations-test
  process 15 phase 1 (est 286 ms): clj-surgeon.mcp-server-test
  process 16 phase 1 (est 1840 ms): clj-surgeon.namespace-split-warm-test
  process 17 phase 1 (est 2510 ms): clj-surgeon.mcp-http-server-test
  process 18 phase 1 (est 2944 ms): clj-surgeon.mcp-tool-test
  process 19 phase 1 (est 3339 ms): clj-surgeon.mcp-hot-verify-test
  process 20 phase 1 (est 9293 ms): clj-surgeon.outline-corpus-integration-test
  process 21 phase 1 (est 48487 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 90693 ms
  process 0 phase 0 (est 5980 ms): clj-surgeon.intent-transaction-test
  process 1 phase 0 (est 6923 ms): clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  process 2 phase 0 (est 1016 ms): clj-surgeon.analyze-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.forms-test
  process 3 phase 0 (est 13777 ms): clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  process 4 phase 0 (est 143 ms): clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test
  process 5 phase 0 (est 11927 ms): clj-surgeon.help-test clj-surgeon.platform-selector-test
  process 6 phase 0 (est 1992 ms): clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test
  process 7 phase 0 (est 13584 ms): clj-surgeon.show-form-test
  process 8 phase 0 (est 336 ms): clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test
  process 9 phase 0 (est 13849 ms): clj-surgeon.tmp-leak-support-test
  process 10 phase 0 (est 70 ms): clj-surgeon.memory-battery-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  process 11 phase 0 (est 14167 ms): clj-surgeon.cli-dispatch-test
  process 12 phase 0 (est 72672 ms): clj-surgeon.parser-admission-test
  process 13 phase 0 (est 90693 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 14805 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 4380 ms):
      4380 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 94998 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 82273 ms):
     82273 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3662 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 94998 ms
cadence :battery: serial-equivalent 86653 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 86653 ms):
     82273 ms  clj-surgeon.mcp-alias-migration-test
      4380 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 94998 ms:
  lane 0     14805 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     94998 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 94998 ms over 2 lane(s); serial-equivalent 86653 ms; skipped 0

========== lane 0 (clj-surgeon.splice-envelope-test clj-surgeon.scope-stream-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test) exit 0, 18109 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (4, slowest first, total 2384 ms):
      1309 ms  clj-surgeon.splice-envelope-test
       494 ms  clj-surgeon.scope-stream-test
       402 ms  clj-surgeon.mcp-prepared-request-test
       179 ms  clj-surgeon.mcp-operation-async-test

========== lane 1 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test) exit 0, 4445 ms ==========

Testing clj-surgeon.lane-manifest-test
BB-LOAD-EXCLUDED clj-surgeon.admit-patch-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.admit-patch-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.analyzer-contract-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.analyzer-contract-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.census-pool-test {:status :load-failed, :message "Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath.", :causes ["Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.census-pool-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.insert-forms-receipt-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.insert-forms-receipt-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-alias-migration-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-alias-migration-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-change-buffer-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-change-buffer-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.mcp-cold-verify-test :jvm {:reasons #{:sci-host-interop}, :detail "SCI refuses FileLockImpl.close in admission-timeout witness"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-combinable-transaction-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-combinable-transaction-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-compact-edit-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-compact-edit-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-compact-location-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-compact-location-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-compact-relations-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-compact-relations-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-create-files-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-create-files-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-expect-guard-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-expect-guard-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-extraction-plan-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-extraction-plan-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-formatter-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-formatter-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-helper-extraction-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-helper-extraction-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-hot-verify-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-hot-verify-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-http-server-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-http-server-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-inspect-cold-job-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-inspect-cold-job-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-inspect-tool-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-inspect-tool-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.mcp-namespace-split-test :jvm {:reasons #{:bb-classpath-missing/nrepl.core}, :detail "Late helper-extraction requiring-resolve needs absent nrepl.core"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-operation-async-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-operation-async-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-operation-registry-test {:status :load-failed, :message "Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange", :causes ["Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-operation-registry-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-operation-test {:status :load-failed, :message "Unable to resolve classname: java.util.Locale$Category", :causes ["Unable to resolve classname: java.util.Locale$Category"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-operation-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-prepared-confirmation-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-prepared-confirmation-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-prepared-request-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-prepared-request-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-prepared-wire-test {:status :load-failed, :message "Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange", :causes ["Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-prepared-wire-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.mcp-relation-census-launcher-test :jvm {:reasons #{:bb-hosted-jvm-launcher :native-image-reflection}, :detail "java.class.path lacks JVM test dependencies; native image refuses StackOverflowError constructor"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-relation-census-round20-test {:status :load-failed, :message "Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath.", :causes ["Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-relation-census-round20-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-relation-census-test {:status :load-failed, :message "Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath.", :causes ["Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-relation-census-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-semantic-client-test {:status :load-failed, :message "Unable to resolve classname: io.modelcontextprotocol.client.McpClient", :causes ["Unable to resolve classname: io.modelcontextprotocol.client.McpClient"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-semantic-client-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-server-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-server-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-tool-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-tool-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mcp-write-refusal-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mcp-write-refusal-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.memory.journal-green-test :jvm {:reasons #{:bb-hosted-jvm-launcher}, :detail "Child Java resolves to nonexistent bin/java under bb"}
BB-INELIGIBLE clj-surgeon.memory.oom-reproduction-test :jvm {:reasons #{:bb-hosted-jvm-launcher}, :detail "Child Java resolves to nonexistent bin/java under bb"}
BB-LOAD-EXCLUDED clj-surgeon.mission-candidate-race-test {:status :load-failed, :message "Unable to resolve classname: java.util.concurrent.ExecutorCompletionService", :causes ["Unable to resolve classname: java.util.concurrent.ExecutorCompletionService"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-candidate-race-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-commit-cli-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-commit-cli-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.mission-display-test :jvm {:reasons #{:bb-classpath-missing/nrepl.core}, :detail "Late mission CLI resolution needs absent nrepl.core"}
BB-LOAD-EXCLUDED clj-surgeon.mission-events-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-events-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-fallback-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-fallback-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-git-ledger-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-git-ledger-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-phase-events-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-phase-events-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-provider-fallback-events-test {:status :load-failed, :message "Unable to resolve classname: java.util.concurrent.ExecutorCompletionService", :causes ["Unable to resolve classname: java.util.concurrent.ExecutorCompletionService"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-provider-fallback-events-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-publication-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-publication-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-run-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-run-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-typist-executor-admission-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-typist-executor-admission-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-typist-executor-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-typist-executor-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.mission-usage-executor-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.mission-usage-executor-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.namespace-split-warm-test {:status :load-failed, :message "Could not locate nrepl/server.bb, nrepl/server.clj or nrepl/server.cljc on classpath.", :causes ["Could not locate nrepl/server.bb, nrepl/server.clj or nrepl/server.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.namespace-split-warm-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.ns-isolation-test :jvm {:reasons #{:native-image-reflection}, :detail "Native image cannot invoke sci.lang.Var.getRawRoot"}
BB-LOAD-EXCLUDED clj-surgeon.outline-memory-test {:status :load-failed, :message "Unable to resolve symbol: java.lang.management.ManagementFactory/getThreadMXBean", :causes ["Unable to resolve symbol: java.lang.management.ManagementFactory/getThreadMXBean"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.outline-memory-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.reader-eval-fence-test :jvm {:reasons #{:bb-hosted-jvm-launcher}, :detail "bb-hosted JVM launcher throws ClassNotFoundException: clojure.main"}
BB-LOAD-EXCLUDED clj-surgeon.receipt-artifacts-boundary-test {:status :load-failed, :message "Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath.", :causes ["Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath."], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.receipt-artifacts-boundary-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.receipt-booleans-test {:status :load-failed, :message "Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange", :causes ["Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.receipt-booleans-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.scope-stream-test {:status :load-failed, :message "Unable to resolve classname: java.nio.file.SimpleFileVisitor", :causes ["Unable to resolve classname: java.nio.file.SimpleFileVisitor"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.scope-stream-test-bb-load.control.edn"}
BB-LOAD-EXCLUDED clj-surgeon.txn-journal-test {:status :load-failed, :message "Unable to resolve classname: java.nio.file.SimpleFileVisitor", :causes ["Unable to resolve classname: java.nio.file.SimpleFileVisitor"], :receipt "docs/observations/2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.txn-journal-test-bb-load.control.edn"}
BB-INELIGIBLE clj-surgeon.worktree-lifecycle-prune-test :jvm {:reasons #{:sci-host-interop}, :detail "After TMPDIR and witnessed Git admission repairs, SCI refuses shared FileLockImpl.release; prune replay retains its lock", :probe "docs/observations/2026-09-12-bbtower-block-b/attempt23/recovery-lock-probe.log"}
BB-INELIGIBLE clj-surgeon.worktree-lifecycle-recovery-test :jvm {:reasons #{:sci-host-interop}, :detail "SCI refuses FileLockImpl.release; replay retains the lock and reports lifecycle-target-locked", :probe "docs/observations/2026-09-12-bbtower-block-b/attempt23/recovery-lock-probe.log"}

Testing clj-surgeon.outline-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.forms-test

========== lane 2 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-semantic-client-test) exit 0, 22437 ms ==========
lanes: --ns -- 7 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.mcp-write-refusal-test

Testing clj-surgeon.mcp-compact-edit-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (7, slowest first, total 4875 ms):
      2631 ms  clj-surgeon.mcp-inspect-tool-test
       879 ms  clj-surgeon.rename-alias-receipt-test
       420 ms  clj-surgeon.outline-memory-test
       341 ms  clj-surgeon.mcp-expect-guard-test
       231 ms  clj-surgeon.mcp-write-refusal-test
       187 ms  clj-surgeon.mcp-semantic-client-test
       186 ms  clj-surgeon.mcp-compact-edit-test

========== lane 3 (clj-surgeon.helper-extraction-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.split-proof-gate-test) exit 0, 1381 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.split-proof-gate-test

========== lane 4 (clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test) exit 0, 21457 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-extraction-plan-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (5, slowest first, total 4245 ms):
      1890 ms  clj-surgeon.ns-isolation-test
      1235 ms  clj-surgeon.insert-forms-receipt-test
       596 ms  clj-surgeon.mcp-change-buffer-test
       272 ms  clj-surgeon.mcp-prepared-confirmation-test
       252 ms  clj-surgeon.mcp-extraction-plan-test

========== lane 5 (clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.fast-lane-isolation-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test) exit 0, 1888 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.mission-usage-test

========== lane 6 (clj-surgeon.insert-forms-test clj-surgeon.mcp-compact-location-test clj-surgeon.census-pool-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test) exit 0, 21636 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.census-pool-test

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
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (5, slowest first, total 4390 ms):
      2761 ms  clj-surgeon.insert-forms-test
       710 ms  clj-surgeon.mcp-compact-location-test
       396 ms  clj-surgeon.census-pool-test
       284 ms  clj-surgeon.mcp-combinable-transaction-test
       239 ms  clj-surgeon.mcp-relation-census-round20-test

========== lane 7 (clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1521 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-formatter-test) exit 0, 23696 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (4, slowest first, total 3811 ms):
      2865 ms  clj-surgeon.mcp-namespace-split-test
       458 ms  clj-surgeon.receipt-booleans-test
       311 ms  clj-surgeon.mcp-create-files-test
       177 ms  clj-surgeon.mcp-formatter-test

========== lane 9 (clj-surgeon.alias-migration-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.move-test clj-surgeon.mission-forms-test clj-surgeon.rename-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-candidate-test) exit 0, 608 ms ==========

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.move-test

Testing clj-surgeon.mission-forms-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mission-candidate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-operation-test) exit 0, 23646 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-operation-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (2, slowest first, total 4904 ms):
      4707 ms  clj-surgeon.mcp-operation-registry-test
       197 ms  clj-surgeon.mcp-operation-test

========== lane 11 (clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.battery-parallel-test clj-surgeon.outermost-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-recovery-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 1066 ms ==========

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.outermost-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 12 (clj-surgeon.rename-alias-test clj-surgeon.mission-candidate-race-test) exit 0, 18537 ms ==========
lanes: --ns -- 2 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (2, slowest first, total 5107 ms):
      4689 ms  clj-surgeon.rename-alias-test
       418 ms  clj-surgeon.mission-candidate-race-test

========== lane 13 (clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.mcp-workspace-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 528 ms ==========

Testing clj-surgeon.recovery-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 14 (clj-surgeon.mcp-compact-relations-test) exit 0, 24913 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 8655 ms):
      8655 ms  clj-surgeon.mcp-compact-relations-test

========== lane 15 (clj-surgeon.mcp-server-test) exit 0, 11102 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
clj-surgeon MCP: embedded nREPL on 42971 ( /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4104704-29723bbf/clj-surgeon-mcp-server-test-9484883327224876210/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 38271 ( /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4104704-29723bbf/clj-surgeon-mcp-server-test-13407206094395046689/.nrepl-port )

namespace walls (1, slowest first, total 264 ms):
       264 ms  clj-surgeon.mcp-server-test

========== lane 16 (clj-surgeon.namespace-split-warm-test) exit 0, 11130 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 1728 ms):
      1728 ms  clj-surgeon.namespace-split-warm-test

========== lane 17 (clj-surgeon.mcp-http-server-test) exit 0, 16068 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
clj-surgeon MCP: embedded nREPL on 38635 ( /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4098519-c4cf8b7b/clj-surgeon-mcp-http-test-10949228478718536950/.nrepl-port )

namespace walls (1, slowest first, total 2814 ms):
      2814 ms  clj-surgeon.mcp-http-server-test

========== lane 18 (clj-surgeon.mcp-tool-test) exit 0, 13917 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 3004 ms):
      3004 ms  clj-surgeon.mcp-tool-test

========== lane 19 (clj-surgeon.mcp-hot-verify-test) exit 0, 9030 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-hot-verify-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.

Testing probe-fixture.check-test

FAIL in (probe-observes-changed-prefix-dependency-and-refuses-unknown-before-reload stale-check) (check_test.clj:1)
expected: (= 1 d/value)
  actual: (not (= 1 2))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 1429 ms):
      1429 ms  clj-surgeon.mcp-hot-verify-test

========== lane 20 (clj-surgeon.outline-corpus-integration-test) exit 0, 15094 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 9066 ms):
      9066 ms  clj-surgeon.outline-corpus-integration-test

========== lane 21 (clj-surgeon.mcp-feature-thread-test) exit 0, 56108 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 21 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 48878 ms):
     48878 ms  clj-surgeon.mcp-feature-thread-test

Ran 1420 tests containing 16487 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 93393 ms
cadence :fast: serial-equivalent 46281 ms; budget 60000 ms
cadence :integration: serial-equivalent 67183 ms; budget 240000 ms
bb-runtime: serial-equivalent 7910 ms; budget 343102 ms; makespan 22967 ms

namespace walls (101, slowest first, serial-equivalent total 113464 ms):
     48878 ms  clj-surgeon.mcp-feature-thread-test
      9066 ms  clj-surgeon.outline-corpus-integration-test
      8655 ms  clj-surgeon.mcp-compact-relations-test
      4707 ms  clj-surgeon.mcp-operation-registry-test
      4689 ms  clj-surgeon.rename-alias-test
      3004 ms  clj-surgeon.mcp-tool-test
      2865 ms  clj-surgeon.mcp-namespace-split-test
      2814 ms  clj-surgeon.mcp-http-server-test
      2761 ms  clj-surgeon.insert-forms-test
      2631 ms  clj-surgeon.mcp-inspect-tool-test
      1890 ms  clj-surgeon.ns-isolation-test
      1728 ms  clj-surgeon.namespace-split-warm-test
      1563 ms  clj-surgeon.lane-manifest-test
      1429 ms  clj-surgeon.mcp-hot-verify-test
      1337 ms  clj-surgeon.outline-test
      1309 ms  clj-surgeon.splice-envelope-test
      1235 ms  clj-surgeon.insert-forms-receipt-test
       879 ms  clj-surgeon.rename-alias-receipt-test
       828 ms  clj-surgeon.helper-extraction-test
       778 ms  clj-surgeon.namespace-split-test
       773 ms  clj-surgeon.operation-algebra-test
       710 ms  clj-surgeon.mcp-compact-location-test
       662 ms  clj-surgeon.analyze-test
       596 ms  clj-surgeon.mcp-change-buffer-test
       494 ms  clj-surgeon.scope-stream-test
       458 ms  clj-surgeon.receipt-booleans-test
       420 ms  clj-surgeon.outline-memory-test
       418 ms  clj-surgeon.mission-candidate-race-test
       402 ms  clj-surgeon.mcp-prepared-request-test
       396 ms  clj-surgeon.census-pool-test
       341 ms  clj-surgeon.mcp-expect-guard-test
       319 ms  clj-surgeon.mcp-inspect-contract-test
       311 ms  clj-surgeon.mcp-create-files-test
       284 ms  clj-surgeon.mcp-combinable-transaction-test
       272 ms  clj-surgeon.mcp-prepared-confirmation-test
       264 ms  clj-surgeon.mcp-server-test
       253 ms  clj-surgeon.mcp-intent-contract-test
       252 ms  clj-surgeon.mcp-extraction-plan-test
       239 ms  clj-surgeon.mcp-relation-census-round20-test
       231 ms  clj-surgeon.mcp-write-refusal-test
       197 ms  clj-surgeon.alias-migration-test
       197 ms  clj-surgeon.mcp-operation-test
       187 ms  clj-surgeon.mcp-semantic-client-test
       186 ms  clj-surgeon.mcp-compact-edit-test
       179 ms  clj-surgeon.mcp-operation-async-test
       177 ms  clj-surgeon.mcp-formatter-test
       118 ms  clj-surgeon.workspace-onboarding-test
       113 ms  clj-surgeon.ls-tree-test
        93 ms  clj-surgeon.jvm-error-test
        88 ms  clj-surgeon.cljc-existing-ops-test
        57 ms  clj-surgeon.fix-declares-test
        52 ms  clj-surgeon.move-dependency-test
        50 ms  clj-surgeon.agent-routing-test
        46 ms  clj-surgeon.telemetry-events-test
        40 ms  clj-surgeon.structural-lens-test
        38 ms  clj-surgeon.relation-census-test
        37 ms  clj-surgeon.recovery-test
        35 ms  clj-surgeon.mcp-extraction-test
        33 ms  clj-surgeon.battery-parallel-test
        33 ms  clj-surgeon.fast-lane-isolation-test
        30 ms  clj-surgeon.mcp-workspace-test
        29 ms  clj-surgeon.owner-hypotheses-test
        29 ms  clj-surgeon.require-change-test
        27 ms  clj-surgeon.insertion-gap-test
        26 ms  clj-surgeon.mission-plain-forms-test
        25 ms  clj-surgeon.memory-battery-test
        25 ms  clj-surgeon.worktree-lifecycle-io-test
        22 ms  clj-surgeon.outermost-test
        16 ms  clj-surgeon.mcp-program-tool-test
        15 ms  clj-surgeon.edit-dsl-test
        15 ms  clj-surgeon.move-test
        11 ms  clj-surgeon.cljc.split-test
        11 ms  clj-surgeon.mcp-contract-test
        11 ms  clj-surgeon.mission-forms-source-test
         9 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.mission-typist-test
         5 ms  clj-surgeon.cljc.merge-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.mcp-recovery-test
         4 ms  clj-surgeon.mcp-schema-test
         4 ms  clj-surgeon.mcp-telemetry-test
         4 ms  clj-surgeon.outline-differential-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.split-proof-gate-test
         3 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.battery-ledger-test
         2 ms  clj-surgeon.mission-forms-test
         2 ms  clj-surgeon.mission-usage-test
         1 ms  clj-surgeon.cljc.analyze-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-read-request-normalization-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mcp-paths-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (22), makespan 93393 ms:
  lane 0     18109 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.scope-stream-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-operation-async-test
  lane 1      4445 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.move-dependency-test clj-surgeon.mission-plain-forms-test clj-surgeon.mcp-program-tool-test clj-surgeon.cljc.split-test clj-surgeon.mcp-contract-test clj-surgeon.outline-differential-test clj-surgeon.battery-ledger-test clj-surgeon.forms-test
  lane 2     22437 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.outline-memory-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-semantic-client-test
  lane 3      1381 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-typist-test clj-surgeon.mcp-schema-test clj-surgeon.split-proof-gate-test
  lane 4     21457 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-extraction-plan-test
  lane 5      1888 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.mcp-intent-contract-test clj-surgeon.workspace-onboarding-test clj-surgeon.fast-lane-isolation-test clj-surgeon.telemetry-events-test clj-surgeon.relation-census-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-usage-test
  lane 6     21636 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.mcp-compact-location-test clj-surgeon.census-pool-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-relation-census-round20-test
  lane 7      1521 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.memory-battery-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 8     23696 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-formatter-test
  lane 9       608 ms  exit 0  clj-surgeon.alias-migration-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.move-test clj-surgeon.mission-forms-test clj-surgeon.rename-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-candidate-test
  lane 10     23646 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-operation-test
  lane 11      1066 ms  exit 0  clj-surgeon.mcp-inspect-contract-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.owner-hypotheses-test clj-surgeon.battery-parallel-test clj-surgeon.outermost-test clj-surgeon.worktree-lifecycle-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-recovery-test clj-surgeon.mcp-read-request-normalization-test
  lane 12     18537 ms  exit 0  clj-surgeon.rename-alias-test clj-surgeon.mission-candidate-race-test
  lane 13       528 ms  exit 0  clj-surgeon.recovery-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.mcp-workspace-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.mcp-compact-edit-fields-test
  lane 14     24913 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 15     11102 ms  exit 0  clj-surgeon.mcp-server-test
  lane 16     11130 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 17     16068 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 18     13917 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 19      9030 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 20     15094 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 21     56108 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 93393 ms over 8 lane(s); serial-equivalent 113464 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 14843 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp

namespace walls (1, slowest first, total 6027 ms):
      6027 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 7451 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test

========== lane 2 (clj-surgeon.analyze-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.forms-test) exit 0, 1457 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.forms-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test) exit 0, 14268 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.edn-config-integration-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073598-149c7e6e/andon-shell-safety9884553675289924013/H; touch /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073598-149c7e6e/andon-shell-safety9884553675289924013/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073598-149c7e6e/andon-shell-safety7191258996080580877/H$(touch /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073598-149c7e6e/andon-shell-safety7191258996080580877/PWNED-SUBST)"

========== lane 4 (clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test) exit 0, 559 ms ==========

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.move-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.failure-report-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.platform-selector-test) exit 1, 11869 ms ==========

Testing clj-surgeon.help-test

FAIL in (resolve-op-canonical-ops) (/home/forge/src/clj-surgeon-bbtower/test/clj_surgeon/help_test.clj:15)
warm probe identity and non-proof verdict contract
expected: (= {:state :probe-passed, :proof_pending [:landing-gate], :reloaded ["example-test"], :tests 2, :assertions 3, :failures 0, :elapsed_ms 12.5} (verdict ["example-test"] {:test 2, :pass 3, :fail 0, :error 0} 12.5))
  actual: (not (= {:state :probe-passed, :proof_pending [:landing-gate], :reloaded ["example-test"], :tests 2, :assertions 3, :failures 0, :elapsed_ms 12.5} {:state :probe-passed, :proof_pending [:landing-gate], :reloaded ["example-test"], :closure-expected 1, :tests 2, :assertions 3, :failures 0, :elapsed_ms 12.5}))

Testing clj-surgeon.platform-selector-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test) exit 0, 2520 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.cljc.analyze-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 14023 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test) exit 0, 814 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14288 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073595-7dbe79d6/tmp-leak-sweep-guard-16652112958855551804/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.memory-battery-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 353 ms ==========

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 11 (clj-surgeon.cli-dispatch-test) exit 0, 14634 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 12 (clj-surgeon.parser-admission-test) exit 0, 73240 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 13 (clj-surgeon.install-test) exit 0, 92692 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/bin with spaces/clj-surgeon from commit 58b33e652b5200232f5c4521b5202051e5a1b7eb, source hash e459f435fbe5ef552a037bb536f6c1fb23324a8ff6d12d80f40548b048632e1b
Receipt: /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0xf5db3b0 "Process[pid=4084942, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x7cf66477 "java.lang.ProcessImpl$ProcessPipeOutputStream@7cf66477"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.192768, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x4fe544fc "Process[pid=4085078, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x2364e022 "java.lang.ProcessImpl$ProcessPipeOutputStream@2364e022"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.246768, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x71eb8e30 "Process[pid=4085237, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x5f368573 "java.lang.ProcessImpl$ProcessPipeOutputStream@5f368573"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-suite-4073588-b495f941/clj-surgeon-installed-splice-14158942149650616196/sample.clj"]}

Ran 902 tests containing 8029 assertions.
1 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 92692 ms
cadence :battery: serial-equivalent 245689 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3510 ms; budget 60000 ms
bb-runtime: serial-equivalent 243172 ms; budget 343102 ms; makespan 92692 ms

namespace walls (50, slowest first, serial-equivalent total 249199 ms):
     92519 ms  clj-surgeon.install-test
     72994 ms  clj-surgeon.parser-admission-test
     14182 ms  clj-surgeon.cli-dispatch-test
     14121 ms  clj-surgeon.tmp-leak-support-test
     13578 ms  clj-surgeon.show-form-test
     11163 ms  clj-surgeon.help-test
      6131 ms  clj-surgeon.edit-test
      6027 ms  clj-surgeon.intent-transaction-test
      5142 ms  clj-surgeon.extract-test
      4820 ms  clj-surgeon.core-discovery-test
      2337 ms  clj-surgeon.lens-query-test
      1510 ms  clj-surgeon.xray-test
       911 ms  clj-surgeon.outline-test
       766 ms  clj-surgeon.operation-algebra-test
       700 ms  clj-surgeon.analyze-test
       520 ms  clj-surgeon.edn-config-integration-test
       368 ms  clj-surgeon.partition-all-test
       277 ms  clj-surgeon.platform-selector-test
       167 ms  clj-surgeon.alias-migration-test
       126 ms  clj-surgeon.ls-tree-test
       121 ms  clj-surgeon.cljc-existing-ops-test
       114 ms  clj-surgeon.workspace-onboarding-test
        92 ms  clj-surgeon.move-dependency-test
        76 ms  clj-surgeon.jvm-error-test
        67 ms  clj-surgeon.relation-census-test
        58 ms  clj-surgeon.recovery-test
        42 ms  clj-surgeon.agent-routing-test
        39 ms  clj-surgeon.structural-lens-test
        31 ms  clj-surgeon.fix-declares-test
        25 ms  clj-surgeon.owner-hypotheses-test
        24 ms  clj-surgeon.worktree-lifecycle-io-test
        21 ms  clj-surgeon.move-test
        21 ms  clj-surgeon.memory-battery-test
        20 ms  clj-surgeon.outermost-test
        17 ms  clj-surgeon.edit-dsl-test
        13 ms  clj-surgeon.insertion-gap-test
        11 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.worktree-lifecycle-test
         7 ms  clj-surgeon.extract-header-test
         7 ms  clj-surgeon.quoted-var-refs-test
         6 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.cljc.merge-test
         4 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.failure-report-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.file-ops-test
         1 ms  clj-surgeon.cljc.analyze-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (14), makespan 92692 ms:
  lane 0     14843 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      7451 ms  exit 0  clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 2      1457 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.forms-test
  lane 3     14268 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  lane 4       559 ms  exit 0  clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test
  lane 5     11869 ms  exit 1  clj-surgeon.help-test clj-surgeon.platform-selector-test
  lane 6      2520 ms  exit 0  clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test
  lane 7     14023 ms  exit 0  clj-surgeon.show-form-test
  lane 8       814 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test
  lane 9     14288 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10       353 ms  exit 0  clj-surgeon.memory-battery-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 11     14634 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 12     73240 ms  exit 0  clj-surgeon.parser-admission-test
  lane 13     92692 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim

BATTERY-LANE: 1 lane failure(s):
   lane 5 exited 1 (log target/gate-prewarm/6572ce47-2709-4a83-b816-eb7afbef9654/bb/lane-5.out)
battery-parallel: makespan 92692 ms over 8 lane(s); serial-equivalent 249199 ms; skipped 0
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
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- arm-b (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- name-only (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- devshm (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/dev/shm is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- unknown-fstype (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4113659-26b4faf8 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4113659-26b4faf8 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4113659-26b4faf8
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114194-3f0dae75 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114194-3f0dae75 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114194-3f0dae75
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114955-ce2fe118 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114955-ce2fe118 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/realdisk/clj-surgeon-suite-4114955-ce2fe118
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/subbase/clj-surgeon-suite-4115066-6fdb47c2 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/subbase/clj-surgeon-suite-4115066-6fdb47c2 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/subbase/clj-surgeon-suite-4115066-6fdb47c2
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/subbase/clj-surgeon-suite-4115066-6fdb47c2/tmp.9lZ4XRjmRI
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/subbase/clj-surgeon-suite-4115066-6fdb47c2: tmp.9lZ4XRjmRI
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/stalebase/clj-surgeon-suite-4117645-ee7e6012 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/stalebase/clj-surgeon-suite-4117645-ee7e6012 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/stalebase/clj-surgeon-suite-4117645-ee7e6012
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-4109338-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp/clj-surgeon-tmpleak-witness.GnQWim/nowrite/clj-surgeon-suite-4118877-ce2eef40 Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner analyzer-contract-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/497943ea-00f2-4239-87b8-269292f431a9/tmp
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- runner clj-surgeon.memory.memory-test-runner (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx/packets/49gate-refused: gate-refused: runtime pool failed {:shell-exit 0, :suites [{:suite "alias", :state :passed, :problems []} {:suite "mcp", :state :passed, :problems []} {:suite "bb", :state :failed, :problems ["lane 5 exited 1 (log target/gate-prewarm/6572ce47-2709-4a83-b816-eb7afbef9654/bb/lane-5.out)"]}]}
make: *** [Makefile:1262: landing-gate-prewarm] Error 1
{"exit": 2, "wall_seconds": 171.65125091467053}
EXIT=2 WALL_S=172
