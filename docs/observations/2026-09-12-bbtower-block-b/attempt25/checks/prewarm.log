STARTED=2026-09-12T21:46:41Z
925a40d7d440069b3396b11690a740b38246e4a4
COMMAND: make landing-gate-prewarm
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 25379 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T21:46:43.536446144Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=134
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=127
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=171
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11268}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 80888 ms
  process 0 phase 0 (est 4674 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 80888 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 46979 ms
  process 0 phase 0 (est 2846 ms): clj-surgeon.lane-manifest-test clj-surgeon.helper-extraction-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.telemetry-events-test clj-surgeon.recovery-test clj-surgeon.fast-lane-isolation-test clj-surgeon.insertion-gap-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-usage-test clj-surgeon.mission-forms-test
  process 1 phase 0 (est 2405 ms): clj-surgeon.splice-envelope-test clj-surgeon.mcp-expect-guard-test clj-surgeon.scope-stream-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mission-candidate-race-test
  process 2 phase 0 (est 3710 ms): clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-operation-test
  process 3 phase 0 (est 1541 ms): clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.forms-test clj-surgeon.extract-header-test clj-surgeon.mcp-read-request-normalization-test
  process 4 phase 0 (est 4066 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  process 5 phase 0 (est 1185 ms): clj-surgeon.outline-test clj-surgeon.ls-tree-test clj-surgeon.relation-census-test clj-surgeon.mcp-extraction-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test
  process 6 phase 0 (est 4363 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-create-files-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-formatter-test
  process 7 phase 0 (est 888 ms): clj-surgeon.namespace-split-test clj-surgeon.structural-lens-test clj-surgeon.mission-forms-source-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.cljc.merge-test clj-surgeon.mission-candidate-test clj-surgeon.failure-report-test
  process 8 phase 0 (est 4246 ms): clj-surgeon.insert-forms-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test
  process 9 phase 0 (est 1005 ms): clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mission-plain-forms-test clj-surgeon.cljc.split-test clj-surgeon.mission-typist-test clj-surgeon.split-proof-gate-test
  process 10 phase 0 (est 5058 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-semantic-client-test
  process 11 phase 0 (est 193 ms): clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.move-test clj-surgeon.mcp-program-tool-test clj-surgeon.outline-differential-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 12 phase 0 (est 5084 ms): clj-surgeon.rename-alias-test
  process 13 phase 0 (est 168 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.worktree-lifecycle-test
  process 14 phase 0 (est 9118 ms): clj-surgeon.mcp-compact-relations-test
  process 15 phase 1 (est 273 ms): clj-surgeon.mcp-server-test
  process 16 phase 1 (est 1661 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 2171 ms): clj-surgeon.namespace-split-warm-test
  process 18 phase 1 (est 3337 ms): clj-surgeon.mcp-tool-test
  process 19 phase 1 (est 3814 ms): clj-surgeon.mcp-http-server-test
  process 20 phase 1 (est 9170 ms): clj-surgeon.outline-corpus-integration-test
  process 21 phase 1 (est 46979 ms): clj-surgeon.mcp-feature-thread-test
battery-parallel: 50 namespace(s) in 50 unit(s) over 8 lane(s); floor is clj-surgeon.install-test at 93377 ms
  process 0 phase 0 (est 5950 ms): clj-surgeon.intent-transaction-test
  process 1 phase 0 (est 6737 ms): clj-surgeon.extract-test clj-surgeon.xray-test
  process 2 phase 0 (est 944 ms): clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.memory-battery-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.file-ops-test
  process 3 phase 0 (est 13345 ms): clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  process 4 phase 0 (est 286 ms): clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  process 5 phase 0 (est 11817 ms): clj-surgeon.help-test clj-surgeon.partition-all-test clj-surgeon.platform-selector-test
  process 6 phase 0 (est 1814 ms): clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.move-dependency-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.worktree-lifecycle-test clj-surgeon.diagnostic-delta-test clj-surgeon.worktree-lifecycle-cli-test
  process 7 phase 0 (est 13193 ms): clj-surgeon.show-form-test
  process 8 phase 0 (est 439 ms): clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test
  process 9 phase 0 (est 14053 ms): clj-surgeon.tmp-leak-support-test
  process 10 phase 0 (est 14481 ms): clj-surgeon.cli-dispatch-test
  process 11 phase 0 (est 72644 ms): clj-surgeon.parser-admission-test
  process 12 phase 0 (est 93377 ms): clj-surgeon.install-test

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 15075 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4587 ms):
      4587 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 93679 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 81139 ms):
     81139 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3669 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 93679 ms
cadence :battery: serial-equivalent 85726 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 85726 ms):
     81139 ms  clj-surgeon.mcp-alias-migration-test
      4587 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 93679 ms:
  lane 0     15075 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     93679 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 93679 ms over 2 lane(s); serial-equivalent 85726 ms; skipped 0

========== lane 0 (clj-surgeon.lane-manifest-test clj-surgeon.helper-extraction-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.telemetry-events-test clj-surgeon.recovery-test clj-surgeon.fast-lane-isolation-test clj-surgeon.insertion-gap-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-usage-test clj-surgeon.mission-forms-test) exit 0, 4180 ms ==========

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

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.mission-forms-test

========== lane 1 (clj-surgeon.splice-envelope-test clj-surgeon.mcp-expect-guard-test clj-surgeon.scope-stream-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mission-candidate-race-test) exit 0, 19867 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.mcp-expect-guard-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.mcp-compact-edit-test

Testing clj-surgeon.mission-candidate-race-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 2612 ms):
      1281 ms  clj-surgeon.splice-envelope-test
       488 ms  clj-surgeon.scope-stream-test
       405 ms  clj-surgeon.mcp-expect-guard-test
       249 ms  clj-surgeon.mcp-compact-edit-test
       189 ms  clj-surgeon.mission-candidate-race-test

========== lane 2 (clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-operation-test) exit 0, 25184 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.receipt-booleans-test

Testing clj-surgeon.mcp-operation-test
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 3520 ms):
      1603 ms  clj-surgeon.ns-isolation-test
       850 ms  clj-surgeon.insert-forms-receipt-test
       502 ms  clj-surgeon.outline-memory-test
       381 ms  clj-surgeon.receipt-booleans-test
       184 ms  clj-surgeon.mcp-operation-test

========== lane 3 (clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.forms-test clj-surgeon.extract-header-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 2254 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.forms-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test) exit 0, 23207 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 4338 ms):
      2596 ms  clj-surgeon.mcp-inspect-tool-test
       738 ms  clj-surgeon.rename-alias-receipt-test
       358 ms  clj-surgeon.mcp-compact-location-test
       237 ms  clj-surgeon.mcp-prepared-request-test
       216 ms  clj-surgeon.mcp-prepared-confirmation-test
       193 ms  clj-surgeon.mcp-operation-async-test

========== lane 5 (clj-surgeon.outline-test clj-surgeon.ls-tree-test clj-surgeon.relation-census-test clj-surgeon.mcp-extraction-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 1630 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 6 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-create-files-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-formatter-test) exit 0, 23193 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 4646 ms):
      3590 ms  clj-surgeon.mcp-namespace-split-test
       360 ms  clj-surgeon.census-pool-test
       312 ms  clj-surgeon.mcp-create-files-test
       204 ms  clj-surgeon.mcp-extraction-plan-test
       180 ms  clj-surgeon.mcp-formatter-test

========== lane 7 (clj-surgeon.namespace-split-test clj-surgeon.structural-lens-test clj-surgeon.mission-forms-source-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.cljc.merge-test clj-surgeon.mission-candidate-test clj-surgeon.failure-report-test) exit 0, 1407 ms ==========

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mission-candidate-test

Testing clj-surgeon.failure-report-test

========== lane 8 (clj-surgeon.insert-forms-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test) exit 0, 22541 ms ==========
lanes: --ns -- 4 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.mcp-write-refusal-test

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
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (4, slowest first, total 4626 ms):
      3574 ms  clj-surgeon.insert-forms-test
       594 ms  clj-surgeon.mcp-change-buffer-test
       231 ms  clj-surgeon.mcp-write-refusal-test
       227 ms  clj-surgeon.mcp-relation-census-round20-test

========== lane 9 (clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mission-plain-forms-test clj-surgeon.cljc.split-test clj-surgeon.mission-typist-test clj-surgeon.split-proof-gate-test) exit 0, 1449 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.split-proof-gate-test

========== lane 10 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-semantic-client-test) exit 0, 24919 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 5136 ms):
      4638 ms  clj-surgeon.mcp-operation-registry-test
       304 ms  clj-surgeon.mcp-combinable-transaction-test
       194 ms  clj-surgeon.mcp-semantic-client-test

========== lane 11 (clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.move-test clj-surgeon.mcp-program-tool-test clj-surgeon.outline-differential-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 599 ms ==========

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.move-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 12 (clj-surgeon.rename-alias-test) exit 0, 18229 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4578 ms):
      4578 ms  clj-surgeon.rename-alias-test

========== lane 13 (clj-surgeon.cljc-existing-ops-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.worktree-lifecycle-test) exit 0, 537 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.worktree-lifecycle-test

========== lane 14 (clj-surgeon.mcp-compact-relations-test) exit 0, 25935 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8746 ms):
      8746 ms  clj-surgeon.mcp-compact-relations-test

========== lane 15 (clj-surgeon.mcp-server-test) exit 0, 10968 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 46061 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1251833-4ce36791/clj-surgeon-mcp-server-test-5022225602539017675/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 43965 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1251833-4ce36791/clj-surgeon-mcp-server-test-2809420895368331182/.nrepl-port )

namespace walls (1, slowest first, total 259 ms):
       259 ms  clj-surgeon.mcp-server-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 8426 ms ==========
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

Testing sol-round5.probe-test

FAIL in (probe-reloads-image-classpath-experiment-dependency stale-check) (probe_test.clj:1)
expected: (= 1 d/value)
  actual: (not (= 1 2))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
:sol-round5 {:elapsed_ms 13.054765, :reloaded [sol-round5.dependency sol-round5.probe-test], :proof_pending [:landing-gate], :tests 1, :external 1, :closure-expected 2, :state :probe-failed, :roots [/home/forge/src/clj-surgeon-bbtower/test /home/forge/src/clj-surgeon-bbtower/dev/experiments /home/forge/src/clj-surgeon-bbtower/src /home/forge/src/clj-surgeon-bbtower/libs/clj-splice/src /home/forge/.gitlibs/libs/io.github.bhauman/clojure-mcp/35a660be1236ea2d15e3e68593fa74054e0aca4e/src /home/forge/.gitlibs/libs/io.github.bhauman/clojure-mcp/35a660be1236ea2d15e3e68593fa74054e0aca4e/resources /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1250163-a2b76bcc/probe-classpath-7071228908287236911/test /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1250163-a2b76bcc/probe-classpath-7071228908287236911/dev/experiments], :failures 1, :assertions 1} :loaded-value 2 :disk-value 2
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1507 ms):
      1507 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.namespace-split-warm-test) exit 0, 11684 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1921 ms):
      1921 ms  clj-surgeon.namespace-split-warm-test

========== lane 18 (clj-surgeon.mcp-tool-test) exit 0, 14648 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 2829 ms):
      2829 ms  clj-surgeon.mcp-tool-test

========== lane 19 (clj-surgeon.mcp-http-server-test) exit 0, 16739 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 43715 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1239357-a7dff541/clj-surgeon-mcp-http-test-7976071167546718920/.nrepl-port )

namespace walls (1, slowest first, total 3400 ms):
      3400 ms  clj-surgeon.mcp-http-server-test

========== lane 20 (clj-surgeon.outline-corpus-integration-test) exit 0, 15209 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9194 ms):
      9194 ms  clj-surgeon.outline-corpus-integration-test

========== lane 21 (clj-surgeon.mcp-feature-thread-test) exit 0, 56258 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 21 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 49058 ms):
     49058 ms  clj-surgeon.mcp-feature-thread-test

Ran 1422 tests containing 16511 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 94257 ms
cadence :fast: serial-equivalent 46275 ms; budget 60000 ms
cadence :integration: serial-equivalent 68168 ms; budget 240000 ms
bb-runtime: serial-equivalent 8073 ms; budget 343102 ms; makespan 23950 ms

namespace walls (101, slowest first, serial-equivalent total 114443 ms):
     49058 ms  clj-surgeon.mcp-feature-thread-test
      9194 ms  clj-surgeon.outline-corpus-integration-test
      8746 ms  clj-surgeon.mcp-compact-relations-test
      4638 ms  clj-surgeon.mcp-operation-registry-test
      4578 ms  clj-surgeon.rename-alias-test
      3590 ms  clj-surgeon.mcp-namespace-split-test
      3574 ms  clj-surgeon.insert-forms-test
      3400 ms  clj-surgeon.mcp-http-server-test
      2829 ms  clj-surgeon.mcp-tool-test
      2596 ms  clj-surgeon.mcp-inspect-tool-test
      1921 ms  clj-surgeon.namespace-split-warm-test
      1603 ms  clj-surgeon.ns-isolation-test
      1507 ms  clj-surgeon.mcp-hot-verify-test
      1435 ms  clj-surgeon.helper-extraction-test
      1385 ms  clj-surgeon.lane-manifest-test
      1281 ms  clj-surgeon.splice-envelope-test
       927 ms  clj-surgeon.operation-algebra-test
       889 ms  clj-surgeon.outline-test
       850 ms  clj-surgeon.insert-forms-receipt-test
       739 ms  clj-surgeon.namespace-split-test
       738 ms  clj-surgeon.rename-alias-receipt-test
       693 ms  clj-surgeon.analyze-test
       594 ms  clj-surgeon.mcp-change-buffer-test
       502 ms  clj-surgeon.outline-memory-test
       488 ms  clj-surgeon.scope-stream-test
       405 ms  clj-surgeon.mcp-expect-guard-test
       381 ms  clj-surgeon.receipt-booleans-test
       360 ms  clj-surgeon.census-pool-test
       358 ms  clj-surgeon.mcp-compact-location-test
       312 ms  clj-surgeon.mcp-create-files-test
       304 ms  clj-surgeon.mcp-combinable-transaction-test
       288 ms  clj-surgeon.mcp-inspect-contract-test
       259 ms  clj-surgeon.mcp-server-test
       249 ms  clj-surgeon.mcp-compact-edit-test
       238 ms  clj-surgeon.mcp-intent-contract-test
       237 ms  clj-surgeon.mcp-prepared-request-test
       231 ms  clj-surgeon.mcp-write-refusal-test
       227 ms  clj-surgeon.mcp-relation-census-round20-test
       216 ms  clj-surgeon.mcp-prepared-confirmation-test
       204 ms  clj-surgeon.mcp-extraction-plan-test
       194 ms  clj-surgeon.mcp-semantic-client-test
       193 ms  clj-surgeon.mcp-operation-async-test
       189 ms  clj-surgeon.mission-candidate-race-test
       184 ms  clj-surgeon.mcp-operation-test
       180 ms  clj-surgeon.mcp-formatter-test
       178 ms  clj-surgeon.alias-migration-test
       116 ms  clj-surgeon.workspace-onboarding-test
       115 ms  clj-surgeon.cljc-existing-ops-test
       112 ms  clj-surgeon.ls-tree-test
        79 ms  clj-surgeon.jvm-error-test
        73 ms  clj-surgeon.recovery-test
        58 ms  clj-surgeon.mcp-extraction-test
        52 ms  clj-surgeon.worktree-lifecycle-io-test
        49 ms  clj-surgeon.agent-routing-test
        49 ms  clj-surgeon.relation-census-test
        48 ms  clj-surgeon.telemetry-events-test
        47 ms  clj-surgeon.move-dependency-test
        44 ms  clj-surgeon.outermost-test
        37 ms  clj-surgeon.fast-lane-isolation-test
        36 ms  clj-surgeon.mcp-workspace-test
        36 ms  clj-surgeon.structural-lens-test
        34 ms  clj-surgeon.owner-hypotheses-test
        34 ms  clj-surgeon.require-change-test
        33 ms  clj-surgeon.battery-parallel-test
        30 ms  clj-surgeon.fix-declares-test
        23 ms  clj-surgeon.mission-plain-forms-test
        18 ms  clj-surgeon.edit-dsl-test
        18 ms  clj-surgeon.memory-battery-test
        15 ms  clj-surgeon.mcp-contract-test
        15 ms  clj-surgeon.move-test
        14 ms  clj-surgeon.mcp-program-tool-test
        10 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.insertion-gap-test
        10 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.mcp-schema-test
         7 ms  clj-surgeon.mission-forms-source-test
         7 ms  clj-surgeon.mission-typist-test
         7 ms  clj-surgeon.outline-differential-test
         6 ms  clj-surgeon.mcp-telemetry-test
         5 ms  clj-surgeon.cljc.merge-test
         5 ms  clj-surgeon.extract-header-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.battery-ledger-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.mission-usage-test
         3 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.mcp-read-request-normalization-test
         2 ms  clj-surgeon.mission-forms-test
         2 ms  clj-surgeon.split-proof-gate-test
         1 ms  clj-surgeon.cljc.analyze-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-paths-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (22), makespan 94257 ms:
  lane 0      4180 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.helper-extraction-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.telemetry-events-test clj-surgeon.recovery-test clj-surgeon.fast-lane-isolation-test clj-surgeon.insertion-gap-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test clj-surgeon.mission-usage-test clj-surgeon.mission-forms-test
  lane 1     19867 ms  exit 0  clj-surgeon.splice-envelope-test clj-surgeon.mcp-expect-guard-test clj-surgeon.scope-stream-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mission-candidate-race-test
  lane 2     25184 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.insert-forms-receipt-test clj-surgeon.outline-memory-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-operation-test
  lane 3      2254 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.mcp-intent-contract-test clj-surgeon.alias-migration-test clj-surgeon.mcp-workspace-test clj-surgeon.agent-routing-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-contract-test clj-surgeon.forms-test clj-surgeon.extract-header-test clj-surgeon.mcp-read-request-normalization-test
  lane 4     23207 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-operation-async-test
  lane 5      1630 ms  exit 0  clj-surgeon.outline-test clj-surgeon.ls-tree-test clj-surgeon.relation-census-test clj-surgeon.mcp-extraction-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.quoted-var-refs-test clj-surgeon.rename-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test
  lane 6     23193 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-create-files-test clj-surgeon.census-pool-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-formatter-test
  lane 7      1407 ms  exit 0  clj-surgeon.namespace-split-test clj-surgeon.structural-lens-test clj-surgeon.mission-forms-source-test clj-surgeon.memory-battery-test clj-surgeon.outermost-test clj-surgeon.cljc.merge-test clj-surgeon.mission-candidate-test clj-surgeon.failure-report-test
  lane 8     22541 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-write-refusal-test clj-surgeon.mcp-relation-census-round20-test
  lane 9      1449 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.move-dependency-test clj-surgeon.require-change-test clj-surgeon.mission-plain-forms-test clj-surgeon.cljc.split-test clj-surgeon.mission-typist-test clj-surgeon.split-proof-gate-test
  lane 10     24919 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-semantic-client-test
  lane 11       599 ms  exit 0  clj-surgeon.jvm-error-test clj-surgeon.battery-parallel-test clj-surgeon.move-test clj-surgeon.mcp-program-tool-test clj-surgeon.outline-differential-test clj-surgeon.mcp-telemetry-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 12     18229 ms  exit 0  clj-surgeon.rename-alias-test
  lane 13       537 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-recovery-test clj-surgeon.worktree-lifecycle-test
  lane 14     25935 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 15     10968 ms  exit 0  clj-surgeon.mcp-server-test
  lane 16      8426 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     11684 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 18     14648 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 19     16739 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 20     15209 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 21     56258 ms  exit 0  clj-surgeon.mcp-feature-thread-test

test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 94257 ms over 8 lane(s); serial-equivalent 114443 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 14573 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 5993 ms):
      5993 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.xray-test) exit 0, 7153 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.xray-test

========== lane 2 (clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.memory-battery-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.file-ops-test) exit 0, 1346 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.syntax-var-refs-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.file-ops-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test) exit 0, 14271 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.edn-config-integration-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220136-f5cb78ef/andon-shell-safety9221944120776127898/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220136-f5cb78ef/andon-shell-safety9221944120776127898/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220136-f5cb78ef/andon-shell-safety18007732576848855334/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220136-f5cb78ef/andon-shell-safety18007732576848855334/PWNED-SUBST)"

========== lane 4 (clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test) exit 0, 672 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.move-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.failure-report-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.partition-all-test clj-surgeon.platform-selector-test) exit 0, 12059 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.partition-all-test

Testing clj-surgeon.platform-selector-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.move-dependency-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.worktree-lifecycle-test clj-surgeon.diagnostic-delta-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 2328 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 13845 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test) exit 0, 882 ms ==========

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.forms-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14106 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220134-11a17c86/tmp-leak-sweep-guard-11775583891631679405/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.cli-dispatch-test) exit 0, 14496 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 11 (clj-surgeon.parser-admission-test) exit 0, 73035 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 12 (clj-surgeon.install-test) exit 0, 91625 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/bin with spaces/clj-surgeon from commit 925a40d7d440069b3396b11690a740b38246e4a4, source hash 42e087fe5a9b13881bcbf23ff4aa6782f31b2badcaf13e04633c47de562e6a87
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x9ee52fc "Process[pid=1233101, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x5316e706 "java.lang.ProcessImpl$ProcessPipeOutputStream@5316e706"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.287691, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x2fef2220 "Process[pid=1233240, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x66ba732b "java.lang.ProcessImpl$ProcessPipeOutputStream@66ba732b"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.263534, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x42c7caca "Process[pid=1233405, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x2a6fce35 "java.lang.ProcessImpl$ProcessPipeOutputStream@2a6fce35"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1220123-787faaa8/clj-surgeon-installed-splice-6064567723733475008/sample.clj"]}

Ran 902 tests containing 8029 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 91625 ms
cadence :battery: serial-equivalent 243766 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3470 ms; budget 60000 ms
bb-runtime: serial-equivalent 241243 ms; budget 343102 ms; makespan 91625 ms

namespace walls (50, slowest first, serial-equivalent total 247236 ms):
     91453 ms  clj-surgeon.install-test
     72781 ms  clj-surgeon.parser-admission-test
     14049 ms  clj-surgeon.cli-dispatch-test
     13934 ms  clj-surgeon.tmp-leak-support-test
     13397 ms  clj-surgeon.show-form-test
     10999 ms  clj-surgeon.help-test
      6115 ms  clj-surgeon.edit-test
      5993 ms  clj-surgeon.intent-transaction-test
      5223 ms  clj-surgeon.extract-test
      4780 ms  clj-surgeon.core-discovery-test
      2370 ms  clj-surgeon.lens-query-test
      1507 ms  clj-surgeon.xray-test
       925 ms  clj-surgeon.outline-test
       759 ms  clj-surgeon.operation-algebra-test
       677 ms  clj-surgeon.analyze-test
       535 ms  clj-surgeon.edn-config-integration-test
       355 ms  clj-surgeon.partition-all-test
       275 ms  clj-surgeon.platform-selector-test
       194 ms  clj-surgeon.alias-migration-test
       112 ms  clj-surgeon.workspace-onboarding-test
       105 ms  clj-surgeon.ls-tree-test
        88 ms  clj-surgeon.cljc-existing-ops-test
        82 ms  clj-surgeon.agent-routing-test
        78 ms  clj-surgeon.jvm-error-test
        69 ms  clj-surgeon.recovery-test
        51 ms  clj-surgeon.move-dependency-test
        42 ms  clj-surgeon.structural-lens-test
        37 ms  clj-surgeon.relation-census-test
        36 ms  clj-surgeon.owner-hypotheses-test
        32 ms  clj-surgeon.fix-declares-test
        29 ms  clj-surgeon.memory-battery-test
        27 ms  clj-surgeon.worktree-lifecycle-io-test
        24 ms  clj-surgeon.outermost-test
        21 ms  clj-surgeon.edit-dsl-test
        17 ms  clj-surgeon.move-test
        14 ms  clj-surgeon.insertion-gap-test
        10 ms  clj-surgeon.cljc.split-test
         8 ms  clj-surgeon.quoted-var-refs-test
         7 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.forms-test
         3 ms  clj-surgeon.syntax-var-refs-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.cljc.analyze-test
         3 ms  clj-surgeon.worktree-lifecycle-test
         2 ms  clj-surgeon.failure-report-test
         1 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (13), makespan 91625 ms:
  lane 0     14573 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      7153 ms  exit 0  clj-surgeon.extract-test clj-surgeon.xray-test
  lane 2      1346 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.owner-hypotheses-test clj-surgeon.memory-battery-test clj-surgeon.insertion-gap-test clj-surgeon.syntax-var-refs-test clj-surgeon.cljc.analyze-test clj-surgeon.file-ops-test
  lane 3     14271 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  lane 4       672 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.move-test clj-surgeon.cljc.split-test clj-surgeon.cljc.require-ops-test clj-surgeon.failure-report-test
  lane 5     12059 ms  exit 0  clj-surgeon.help-test clj-surgeon.partition-all-test clj-surgeon.platform-selector-test
  lane 6      2328 ms  exit 0  clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.move-dependency-test clj-surgeon.structural-lens-test clj-surgeon.relation-census-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.worktree-lifecycle-test clj-surgeon.diagnostic-delta-test clj-surgeon.worktree-lifecycle-cli-test
  lane 7     13845 ms  exit 0  clj-surgeon.show-form-test
  lane 8       882 ms  exit 0  clj-surgeon.alias-migration-test clj-surgeon.workspace-onboarding-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.edit-dsl-test clj-surgeon.quoted-var-refs-test clj-surgeon.cljc.merge-test clj-surgeon.forms-test
  lane 9     14106 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10     14496 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 11     73035 ms  exit 0  clj-surgeon.parser-admission-test
  lane 12     91625 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 91625 ms over 8 lane(s); serial-equivalent 247236 ms; skipped 0
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1259961-c6265052 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1259961-c6265052 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1259961-c6265052
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1262679-7b8b014e node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1262679-7b8b014e node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1262679-7b8b014e
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1264070-ff56dd37 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1264070-ff56dd37 node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/realdisk/clj-surgeon-suite-1264070-ff56dd37
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/subbase/clj-surgeon-suite-1264184-3d2ef03d node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/subbase/clj-surgeon-suite-1264184-3d2ef03d node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/subbase/clj-surgeon-suite-1264184-3d2ef03d
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/subbase/clj-surgeon-suite-1264184-3d2ef03d/tmp.xoBcMKjhld
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/subbase/clj-surgeon-suite-1264184-3d2ef03d: tmp.xoBcMKjhld
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/stalebase/clj-surgeon-suite-1265445-dcaf0828 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/stalebase/clj-surgeon-suite-1265445-dcaf0828 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/stalebase/clj-surgeon-suite-1265445-dcaf0828
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-1255500-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE/nowrite/clj-surgeon-suite-1265812-cf3a2ee6 Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.eK7eBE ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.3GSAEA/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.3GSAEA/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s.........
----------------------------------------------------------------------
Ran 21 tests in 0.402s

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
Ran 3 tests in 1.313s

OK
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
gate-stage: {:target "alias-migration-test", :exit 0, :wall-ms 93679}
gate-stage: {:target "mcp-test", :exit 0, :wall-ms 94257}
gate-stage: {:target "test-bb", :exit 0, :wall-ms 91625}
gate-stage: test-bb-diagnostic started 2026-09-12T21:49:34.296802962Z
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" test/run_all.clj
SERIAL/NOT-A-GATE: direct Babashka diagnostic

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.tmp-leak-support-test
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/tmp-leak-sweep-guard-16842661045388205044/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

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
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/bin with spaces/clj-surgeon from commit 925a40d7d440069b3396b11690a740b38246e4a4, source hash 42e087fe5a9b13881bcbf23ff4aa6782f31b2badcaf13e04633c47de562e6a87
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x5624f177 "Process[pid=1331965, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x438d02fb "java.lang.ProcessImpl$ProcessPipeOutputStream@438d02fb"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.224165, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x46fd2c4f "Process[pid=1332122, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x79814a83 "java.lang.ProcessImpl$ProcessPipeOutputStream@79814a83"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.611867, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x280dbea3 "Process[pid=1332288, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x4ed7eb0f "java.lang.ProcessImpl$ProcessPipeOutputStream@4ed7eb0f"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/clj-surgeon-installed-splice-10891230854580791518/sample.clj"]}

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.worktree-lifecycle-cli-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.cli-dispatch-test

Testing clj-surgeon.core-discovery-test
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/andon-shell-safety15203538214791729587/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/andon-shell-safety15203538214791729587/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/andon-shell-safety6871275208381374270/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-1286398-c3a0dedb/andon-shell-safety6871275208381374270/PWNED-SUBST)"

Ran 833 tests containing 7328 assertions.
0 failures, 0 errors.
gate-stage: {:target "test-bb-diagnostic", :exit 0, :wall-ms 226024}
gate-stage: repository-hygiene started 2026-09-12T21:53:20.321550173Z
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
repository hygiene: no machine-local build cache is tracked at any depth
gate-stage: {:target "repository-hygiene", :exit 0, :wall-ms 2090}
gate-stage: intent-audit started 2026-09-12T21:53:22.412006105Z
{:ok true, :specs {"MCP-OP-ADMIT-135" :implemented, "MCP-OP-ADMIT-051" :implemented, "MCP-OP-HELPER-020" :active-gap, "MCP-OP-THREAD-021" :implemented, "INSERT-FORMS-020" :implemented, "MCP-OP-ALIAS-015" :implemented, "MCP-OP-THREAD-046" :implemented, "MCP-OP-ALIAS-032" :implemented, "MCP-OP-HELPER-022" :active-gap, "MCP-OP-ADMIT-020" :implemented, "WTL-APPLY-007" :implemented, "MCP-OP-CENSUS-012" :implemented, "MCP-OP-EDIT-008" :implemented, "MCP-OP-PREP-ACT-018" :active-gap, "NS-SPLIT-043" :implemented, "MCP-OP-DISPATCH-004" :implemented, "MCP-OP-EDIT-035" :implemented, "MCP-OP-EDIT-024" :implemented, "MCP-OP-ADMIT-121" :implemented, "WTL-PRUNE-005" :implemented, "PERF-SENT-IMPORT-001" :active-gap, "MCP-OP-VERIFY-010" :implemented, "MCP-OP-MEM-011" :deferred, "MCP-OP-PREP-ACT-017" :active-gap, "MCP-OP-ALIAS-002" :implemented, "OP-ALG-EFFECT-002" :implemented, "MCP-OP-HELPER-014" :active-gap, "MCP-OP-READ-MISSION-001" :deferred, "MCP-OP-READ-MISSION-002" :deferred, "MCP-OP-CENSUS-027" :implemented, "REQUIRE-CHANGE-005" :implemented, "MCP-OP-ALIAS-055" :implemented, "MCP-OP-EDIT-004" :implemented, "INSERT-FORMS-011" :implemented, "MCP-OP-ADMIT-152" :active-gap, "NS-SPLIT-057" :implemented, "MCP-OP-READ-RESOLVE-002" :deferred, "TELEMETRY-EVENTS-001" :implemented, "NS-SPLIT-060" :implemented, "MCP-OP-ADMIT-106" :implemented, "MCP-OP-ADMIT-147" :active-gap, "TEST-ISO-RACE-002" :implemented, "MCP-OP-PREP-ACT-015" :active-gap, "MCP-OP-EDIT-034" :implemented, "MCP-OP-ADMIT-069" :implemented, "INSERT-FORMS-006" :implemented, "MCP-OP-ALIAS-059" :implemented, "MCP-OP-ADMIT-150" :implemented, "OP-ALG-PREVIEW-002" :implemented, "MCP-OP-INSERT-009" :implemented, "MCP-OP-CENSUS-029" :implemented, "BB-PROBE-004" :implemented, "MCP-OP-VERIFY-009" :implemented, "MCP-OP-VERIFY-008" :implemented, "MCP-OP-ALIAS-040" :implemented, "PERF-SENT-VERDICT-002" :active-gap, "NS-SPLIT-029" :implemented, "MCP-OP-CENSUS-017" :implemented, "MCP-OP-ADMIT-021" :implemented, "MCP-OP-ADMIT-110" :implemented, "MCP-OP-FIELD-003" :implemented, "PERF-SENT-AUTH-003" :active-gap, "MCP-OP-PREP-REQ-007" :implemented, "MCP-OP-ADMIT-151" :implemented, "MCP-OP-THREAD-042" :implemented, "PERF-SENT-VERDICT-003" :active-gap, "MCP-OP-HELPER-002" :active-gap, "ALIAS-MIGRATION-005" :implemented, "PERF-SENT-PROJECT-001" :active-gap, "TEST-ISO-001a" :implemented, "MCP-OP-SHELL-ARGV-006" :implemented, "MCP-OP-ALIAS-017" :implemented, "MCP-OP-ADMIT-023" :implemented, "PERF-SENT-LEDGER-004" :active-gap, "MCP-OP-PREP-ACT-007" :active-gap, "MCP-OP-PREP-ACT-002" :active-gap, "MCP-OP-WRITE-REFUSAL-002" :deferred, "INSERT-FORMS-010" :implemented, "MCP-OP-PLAN-001" :implemented, "WTL-SEAL-006" :implemented, "PERF-SENT-RELEASE-001" :active-gap, "MCP-OP-READ-GUARD-001" :implemented, "MCP-OP-HELPER-011" :active-gap, "MCP-OP-SHELL-ARGV-007" :implemented, "WTL-PRUNE-001" :implemented, "MCP-OP-ALIAS-014" :implemented, "MCP-OP-EDIT-028" :implemented, "MCP-OP-RESULT-005" :implemented, "MCP-OP-COVERAGE-001" :implemented, "NS-SPLIT-072" :implemented, "MCP-OP-EDIT-007" :implemented, "WTL-HAND-005" :implemented, "MCP-OP-MATCHED-001" :implemented, "MCP-OP-ADMIT-107" :implemented, "TEST-ISO-002" :implemented, "MCP-OP-EDIT-012" :implemented, "MCP-OP-VERIFY-004" :implemented, "MCP-OP-POS-AUTH-003" :implemented, "MCP-OP-WRITE-REFUSAL-003" :deferred, "MCP-OP-ALIAS-051" :implemented, "MCP-OP-HELPER-018" :active-gap, "MCP-OP-ADMIT-144" :implemented, "MCP-OP-ADMIT-034" :implemented, "MCP-OP-HELPER-009" :active-gap, "NS-SPLIT-001" :implemented, "PERF-SENT-TIME-001" :active-gap, "ALIAS-MIGRATION-003" :implemented, "MCP-OP-ALIAS-046" :implemented, "MCP-OP-ADMIT-099" :implemented, "MCP-OP-THREAD-029" :implemented, "MEASURE-EVID-001" :active-gap, "MCP-OP-ADMIT-111" :implemented, "MCP-OP-ALIAS-065" :active-gap, "MCP-OP-EDIT-018" :implemented, "MCP-OP-MATCHED-004" :implemented, "MCP-OP-ALIAS-047" :implemented, "MCP-OP-PREP-ACT-001" :active-gap, "MCP-OP-THREAD-039" :implemented, "MCP-OP-ADMIT-032" :implemented, "MCP-OP-TMPHYG-012" :implemented, "MCP-OP-HELPER-019" :active-gap, "MCP-OP-PLAN-004" :implemented, "NS-SPLIT-008" :implemented, "NS-SPLIT-046" :implemented, "NS-SPLIT-042" :deferred, "MCP-OP-TRACE-002" :implemented, "OP-ALG-VERIFY-004" :deferred, "REQUIRE-CHANGE-004" :implemented, "MCP-OP-ALIAS-019" :implemented, "WTL-APPLY-002" :implemented, "NS-SPLIT-056" :implemented, "MCP-OP-EDIT-027" :implemented, "MCP-OP-INSERT-003" :active-gap, "NS-SPLIT-071" :implemented, "MCP-OP-HOTVER-002" :implemented, "MCP-OP-ALIAS-036" :implemented, "REQUIRE-CHANGE-002" :implemented, "MCP-OP-CENSUS-035" :implemented, "OP-ALG-EFFECT-005" :implemented, "MCP-OP-ADMIT-065" :implemented, "MCP-OP-ANALYZER-008" :implemented, "MCP-OP-MATCH-005" :deferred, "PERF-SENT-SCHEDULE-002" :active-gap, "MCP-OP-CENSUS-005" :implemented, "MCP-OP-ADMIT-066" :implemented, "RENAME-ALIAS-001" :implemented, "MCP-OP-ALIAS-016" :implemented, "PERF-SENT-CONFIG-001" :active-gap, "MCP-OP-ANALYZER-009" :implemented, "MCP-OP-CENSUS-034" :implemented, "MCP-OP-ASYNC-002" :implemented, "MCP-OP-PREP-REQ-005" :implemented, "OP-ALG-COMMIT-003" :implemented, "MCP-OP-PREP-ACT-014" :active-gap, "RENAME-ALIAS-003" :implemented, "MCP-OP-ADMIT-141" :implemented, "TEST-ISO-014" :implemented, "MCP-OP-HELPER-021" :active-gap, "WTL-CLI-003" :implemented, "MCP-OP-ADMIT-148" :implemented, "MCP-OP-ALIAS-063" :active-gap, "MCP-OP-ANALYZER-006" :active-gap, "INSERT-FORMS-012" :implemented, "MCP-OP-EDIT-001" :implemented, "MCP-OP-EDIT-033" :implemented, "NS-SPLIT-052" :implemented, "OP-ALG-RUNTIME-001" :implemented, "NS-SPLIT-062" :implemented, "MCP-OP-ADMIT-061" :implemented, "MCP-OP-EDIT-032" :implemented, "NS-SPLIT-035" :implemented, "MCP-OP-ALIAS-044" :implemented, "MCP-OP-SHELL-ARGV-002" :implemented, "WTL-HAND-002" :implemented, "MCP-OP-VERIFY-007" :implemented, "MCP-OP-MATCHED-005" :implemented, "MCP-OP-THREAD-006" :implemented, "WTL-SEAL-001" :implemented, "MCP-OP-PREP-ACT-005" :active-gap, "RENAME-ALIAS-014" :implemented, "WTL-INV-001" :implemented, "MCP-OP-VERIFY-003" :implemented, "MCP-OP-ADMIT-105" :implemented, "MCP-OP-ADMIT-088" :implemented, "MCP-OP-ADMIT-030" :implemented, "PERF-SENT-CUTOVER-002" :active-gap, "MCP-OP-SCHEMA-001" :implemented, "INSERT-FORMS-008" :implemented, "MCP-OP-ADMIT-149" :implemented, "RENAME-ALIAS-007" :implemented, "RENAME-ALIAS-010" :implemented, "MCP-OP-VERIFY-005" :implemented, "MCP-OP-ADMIT-139" :implemented, "MCP-OP-THREAD-018" :implemented, "MCP-OP-VERIFY-006" :implemented, "MCP-OP-READ-HYP-002" :implemented, "MCP-OP-THREAD-012" :implemented, "OP-ALG-STALE-001" :implemented, "MCP-OP-MEM-020" :implemented, "MCP-OP-TRACE-006" :implemented, "INSERT-FORMS-017" :implemented, "MCP-OP-CENSUS-010" :implemented, "MCP-OP-ALIAS-054" :implemented, "MCP-OP-READ-NORM-001" :active-gap, "MCP-OP-EDIT-009" :implemented, "NS-SPLIT-015" :implemented, "TEST-ISO-001" :implemented, "MCP-OP-READ-RESOLVE-001" :deferred, "MCP-OP-ALIAS-057" :implemented, "MCP-OP-ALIAS-009" :implemented, "WTL-INV-002" :implemented, "MCP-OP-EDIT-015" :implemented, "MCP-OP-DISPATCH-002" :implemented, "MCP-OP-WRITE-REFUSAL-006" :deferred, "MCP-OP-TIME-003" :implemented, "MCP-OP-CENSUS-028" :implemented, "PERF-SENT-AUTH-001" :active-gap, "NS-SPLIT-041" :implemented, "MCP-OP-POS-AUTH-007" :implemented, "MCP-OP-MATCHED-002" :implemented, "MCP-OP-ANALYZER-002" :implemented, "TEST-ISO-010" :implemented, "WTL-PLAN-004" :implemented, "MCP-OP-MEM-007" :implemented, "MCP-OP-ALIAS-013" :implemented, "WTL-PRUNE-008" :implemented, "PERF-SENT-LEDGER-003" :active-gap, "MCP-OP-DISPATCH-003" :implemented, "MCP-OP-TMPHYG-009" :implemented, "WTL-PRUNE-002" :implemented, "NS-SPLIT-064" :implemented, "MCP-OP-EDIT-037" :implemented, "MCP-OP-HOTVER-001" :implemented, "MCP-OP-POS-AUTH-004" :implemented, "MCP-OP-ADMIT-100" :implemented, "NS-SPLIT-049" :implemented, "MCP-OP-TMPHYG-011" :implemented, "MCP-OP-ADMIT-120" :implemented, "MCP-OP-ADMIT-060" :implemented, "WTL-HAND-003" :implemented, "MCP-OP-THREAD-001" :implemented, "MCP-OP-EDIT-023" :implemented, "MCP-OP-CENSUS-030" :implemented, "MCP-OP-RESULT-003" :implemented, "INSERT-FORMS-019" :implemented, "MCP-OP-ADMIT-033" :implemented, "WTL-APPLY-004" :implemented, "MCP-OP-THREAD-008" :implemented, "MCP-OP-ADMIT-112" :implemented, "MCP-OP-THREAD-015" :implemented, "MCP-OP-EDIT-016" :implemented, "TEST-ISO-009" :implemented, "MCP-OP-ALIAS-028" :implemented, "WTL-PRUNE-010" :implemented, "MCP-OP-ALIAS-039" :implemented, "RECEIPT-BOOL-001" :implemented, "MCP-OP-ADMIT-081" :implemented, "MCP-OP-CENSUS-003" :implemented, "MCP-OP-CENSUS-019" :implemented, "MCP-OP-ALIAS-067" :implemented, "MCP-OP-CENSUS-020" :implemented, "RENAME-ALIAS-008" :implemented, "WTL-SEAL-008" :deferred, "MCP-OP-ADMIT-062" :implemented, "REQUIRE-CHANGE-008" :implemented, "MCP-OP-FIELD-002" :implemented, "MCP-OP-READ-NORM-004" :active-gap, "MCP-OP-THREAD-041" :implemented, "MCP-OP-THREAD-011" :implemented, "MCP-OP-TMPHYG-003" :implemented, "MCP-OP-MEM-015" :implemented, "MCP-OP-THREAD-051" :implemented, "OP-ALG-COMPILE-001" :implemented, "MCP-OP-ALIAS-030" :implemented, "BB-PROBE-001" :implemented, "OP-ALG-EXPAND-001" :deferred, "MCP-OP-DISPATCH-005" :implemented, "MCP-OP-POS-AUTH-010" :implemented, "MCP-OP-THREAD-038" :implemented, "MCP-OP-EDIT-011" :implemented, "MCP-OP-ADMIT-004" :implemented, "NS-SPLIT-013" :implemented, "MCP-OP-ASYNC-001" :implemented, "MCP-OP-ADMIT-070" :implemented, "MCP-OP-ALIAS-022" :implemented, "MCP-OP-ADMIT-142" :implemented, "TEST-ISO-009b" :implemented, "MCP-OP-PREP-ACT-013" :active-gap, "MCP-OP-ALIAS-005" :implemented, "NS-SPLIT-005" :implemented, "NS-SPLIT-004" :implemented, "MCP-OP-HELPER-007" :active-gap, "MCP-OP-HELPER-005" :active-gap, "NS-SPLIT-055" :implemented, "MCP-OP-READ-CONT-001" :implemented, "MCP-OP-ALIAS-023" :implemented, "MCP-OP-THREAD-035" :implemented, "PERF-SENT-SCHEDULE-003" :active-gap, "MCP-OP-INSERT-010" :implemented, "CLI-PACKAGE-001" :implemented, "MCP-OP-ALIAS-038" :implemented, "RENAME-ALIAS-012" :implemented, "INSERT-FORMS-018" :implemented, "MCP-OP-ADMIT-103" :implemented, "OP-ALG-EFFECT-004" :implemented, "MCP-OP-CENSUS-006" :implemented, "INSERT-FORMS-016" :implemented, "MCP-OP-THREAD-014" :implemented, "SPLIT-REPAIR-002" :implemented, "MCP-OP-HELPER-013" :active-gap, "MCP-OP-DISPATCH-001" :implemented, "INSERT-FORMS-023" :implemented, "MCP-OP-ADMIT-125" :implemented, "OP-ALG-DECODE-001" :implemented, "INSERT-FORMS-021" :implemented, "MCP-OP-ANALYZER-004" :implemented, "MCP-OP-THREAD-023" :implemented, "NS-SPLIT-036" :implemented, "OP-ALG-COMMIT-001" :implemented, "MCP-OP-ADMIT-098" :implemented, "MCP-OP-ALIAS-050" :implemented, "MCP-OP-ANALYZER-003" :implemented, "PERF-SENT-BASELINE-001" :active-gap, "MCP-OP-ALIAS-026" :implemented, "MCP-OP-PREP-ACT-003" :active-gap, "TEST-ISO-015" :implemented, "WTL-APPLY-009" :implemented, "WTL-PRUNE-004" :implemented, "MCP-OP-ALIAS-061" :implemented, "MCP-OP-FIELD-008" :implemented, "PERF-SENT-AUTH-002" :active-gap, "MCP-OP-THREAD-031" :implemented, "MCP-OP-THREAD-019" :implemented, "MCP-OP-ADMIT-096" :implemented, "MCP-OP-RELAY-002" :implemented, "WTL-HAND-001" :implemented, "MCP-OP-CENSUS-018" :implemented, "PERF-SENT-CUTOVER-003" :active-gap, "MCP-OP-WRITE-REFUSAL-001" :implemented, "NS-SPLIT-065" :implemented, "MCP-OP-HELPER-004" :active-gap, "OP-ALG-OUTCOME-003" :implemented, "TEST-ISO-016" :implemented, "MCP-OP-ALIAS-007" :implemented, "OP-ALG-CLI-001" :implemented, "MCP-OP-THREAD-032" :implemented, "MCP-OP-ADMIT-119" :implemented, "MCP-OP-TMPHYG-006" :implemented, "MCP-OP-HELPER-016" :active-gap, "MCP-OP-ALIAS-060" :implemented, "TEST-ISO-001c" :implemented, "MCP-OP-PLAN-002" :implemented, "WTL-INV-007" :implemented, "MCP-OP-ADMIT-024" :implemented, "NS-SPLIT-034" :implemented, "MCP-OP-WRITE-REFUSAL-008" :deferred, "PERF-SENT-RETAIN-001" :active-gap, "MCP-OP-TMPHYG-004" :implemented, "MCP-OP-THREAD-045" :implemented, "NS-SPLIT-045" :implemented, "REQUIRE-CHANGE-011" :implemented, "MCP-OP-THREAD-040" :implemented, "MCP-OP-RELAY-001" :implemented, "MCP-OP-CENSUS-004" :implemented, "MCP-OP-SHELL-ARGV-005" :implemented, "MCP-OP-CENSUS-023" :implemented, "PERF-SENT-CLIENT-001" :active-gap, "MCP-OP-ADMIT-064" :implemented, "NS-SPLIT-033" :implemented, "MCP-OP-READ-DIAG-001" :implemented, "INSERT-FORMS-015" :implemented, "NS-SPLIT-038" :implemented, "MCP-OP-THREAD-020" :implemented, "MCP-OP-WRITE-REFUSAL-004" :deferred, "MCP-OP-TMPHYG-001" :implemented, "MCP-OP-READ-PARITY-001" :implemented, "MCP-OP-ADMIT-012" :implemented, "ROUTING-FANOUT-001" :implemented, "MCP-OP-EDIT-038" :implemented, "INSERT-FORMS-013" :implemented, "RENAME-ALIAS-004" :implemented, "MCP-OP-HELPER-012" :active-gap, "NS-SPLIT-059" :implemented, "MCP-OP-ANALYZER-007" :implemented, "MCP-OP-PREP-REQ-004" :implemented, "MCP-OP-CENSUS-021" :implemented, "MCP-OP-THREAD-033" :implemented, "WTL-SEAL-004" :implemented, "PERF-SENT-RECEIPT-001" :active-gap, "RENAME-ALIAS-009" :implemented, "MCP-OP-ALIAS-024" :implemented, "MCP-OP-INSERT-001" :active-gap, "MCP-OP-POS-AUTH-001" :implemented, "MCP-OP-VERIFY-001" :implemented, "MCP-OP-ALIAS-041" :implemented, "NS-SPLIT-002" :implemented, "PERF-SENT-INVALID-001" :active-gap, "MCP-OP-TRACE-001" :implemented, "REQUIRE-CHANGE-009" :implemented, "MCP-OP-ALIAS-025" :implemented, "MCP-OP-ADMIT-094" :implemented, "RENAME-ALIAS-011" :implemented, "MCP-OP-THREAD-050" :implemented, "INSERT-FORMS-005" :implemented, "OP-ALG-OUTCOME-002" :implemented, "REQUIRE-CHANGE-003" :implemented, "MCP-OP-ADMIT-063" :implemented, "WTL-APPLY-010" :implemented, "MCP-OP-ALIAS-029" :implemented, "MCP-OP-THREAD-052" :implemented, "MCP-OP-MEM-001" :deferred, "MCP-OP-ALIAS-018" :implemented, "PERF-SENT-RECOVERY-002" :active-gap, "MCP-OP-ADMIT-050" :implemented, "MCP-OP-ADMIT-137" :implemented, "MCP-OP-ADMIT-053" :implemented, "MCP-OP-EDIT-003" :implemented, "OP-ALG-REFUSE-001" :implemented, "MCP-OP-ADMIT-132" :implemented, "OP-ALG-CLI-002" :deferred, "PERF-SENT-IDENT-001" :active-gap, "PERF-SENT-SCHEDULE-001" :active-gap, "MCP-OP-ADMIT-067" :implemented, "MCP-OP-ADMIT-090" :implemented, "MCP-OP-CENSUS-022" :implemented, "INSERT-FORMS-022" :implemented, "MCP-OP-TMPHYG-005" :implemented, "MCP-OP-HELPER-003" :active-gap, "MCP-OP-SHELL-ARGV-003" :implemented, "OP-ALG-EFFECT-001" :implemented, "INSERT-FORMS-001" :implemented, "INSERT-FORMS-007" :implemented, "MCP-OP-ALIAS-001" :implemented, "MCP-OP-ALIAS-006" :implemented, "MCP-OP-CENSUS-013" :implemented, "NS-SPLIT-007" :implemented, "MCP-OP-THREAD-027" :implemented, "MCP-OP-ADMIT-113" :implemented, "SPLIT-REPAIR-001" :implemented, "MCP-OP-THREAD-007" :implemented, "MCP-OP-ADMIT-116" :implemented, "MCP-OP-PREP-ACT-004" :active-gap, "MCP-OP-PLAN-009" :implemented, "MCP-OP-ALIAS-020" :implemented, "MCP-OP-THREAD-048" :implemented, "MEASURE-WALL-002" :active-gap, "MCP-OP-ADMIT-005" :implemented, "MCP-OP-PREP-REQ-003" :implemented, "MCP-OP-READ-AUTH-001" :deferred, "PERF-SENT-PROMOTION-002" :active-gap, "MCP-OP-SHELL-ARGV-001" :implemented, "MCP-OP-TMPHYG-008" :implemented, "MCP-OP-PREP-REQ-006" :implemented, "REQUIRE-CHANGE-010" :implemented, "MCP-OP-ADMIT-109" :implemented, "TEST-ISO-008" :active-gap, "PERF-SENT-SCHEDULE-004" :active-gap, "MCP-OP-EDIT-022" :implemented, "MCP-OP-ADMIT-127" :implemented, "WTL-SEAL-005" :implemented, "MCP-OP-TMPHYG-007" :implemented, "MCP-OP-ADMIT-082" :implemented, "WTL-HAND-004" :implemented, "MCP-OP-RELAY-003" :implemented, "NS-SPLIT-067" :implemented, "MCP-OP-WRITE-REFUSAL-007" :deferred, "OP-ALG-COMMIT-004" :implemented, "NS-SPLIT-032" :implemented, "MCP-OP-EDIT-041" :implemented, "INSERT-FORMS-004" :implemented, "MCP-OP-THREAD-013" :implemented, "RENAME-ALIAS-005" :implemented, "PERF-SENT-IMPORT-002" :active-gap, "MCP-OP-EDIT-040" :implemented, "MCP-OP-ALIAS-033" :implemented, "MCP-OP-ADMIT-003" :implemented, "NS-SPLIT-048" :implemented, "PERF-SENT-BACKFILL-001" :active-gap, "MCP-OP-POS-AUTH-002" :implemented, "ALIAS-MIGRATION-004" :implemented, "MCP-OP-MEM-006" :implemented, "MCP-OP-ADMIT-089" :implemented, "MCP-OP-ADMIT-136" :implemented, "MCP-OP-INSERT-002" :active-gap, "PERF-SENT-LEDGER-002" :active-gap, "TEST-ISO-011" :active-gap, "MCP-OP-FIELD-001" :implemented, "MCP-OP-POS-AUTH-005" :implemented, "OP-ALG-FORM-COUNT-001" :implemented, "MCP-OP-MATCH-004" :implemented, "MCP-OP-ADMIT-071" :implemented, "OP-ALG-VERIFY-001" :deferred, "MCP-OP-THREAD-026" :implemented, "MCP-OP-ALIAS-053" :implemented, "TEST-ISO-009a" :implemented, "ALIAS-MIGRATION-001" :implemented, "INSERT-FORMS-003" :implemented, "MCP-OP-EDIT-006" :implemented, "PERF-SENT-PUBLISH-002" :active-gap, "PERF-SENT-LEDGER-ROOT-001" :active-gap, "MCP-OP-CENSUS-009" :implemented, "MCP-OP-PLAN-003" :implemented, "MCP-OP-INSERT-005" :active-gap, "MCP-OP-INSERT-004" :active-gap, "MCP-OP-ADMIT-143" :implemented, "MCP-OP-EDIT-030" :implemented, "MCP-OP-ALIAS-062" :active-gap, "OP-ALG-CONTEXT-002" :implemented, "MCP-OP-TMPHYG-010" :implemented, "MCP-OP-THREAD-034" :implemented, "MCP-OP-READ-NORM-005" :active-gap, "MCP-OP-CENSUS-016" :implemented, "MCP-OP-EDIT-013" :implemented, "WTL-INV-006" :implemented, "MCP-OP-ADMIT-084" :implemented, "WTL-APPLY-003" :implemented, "INSERT-FORMS-014" :implemented, "MCP-OP-THREAD-009" :implemented, "PERF-SENT-PROMOTION-001" :active-gap, "NS-SPLIT-053" :implemented, "MCP-OP-INSERT-007" :implemented, "NS-SPLIT-044" :implemented, "NS-SPLIT-063" :implemented, "MCP-OP-MATCHED-003" :implemented, "MCP-OP-MEM-012" :implemented, "MCP-OP-ASYNC-005" :implemented, "REQUIRE-CHANGE-013" :implemented, "MCP-OP-ADMIT-130" :implemented, "MCP-OP-ADMIT-128" :implemented, "BB-PROBE-002" :implemented, "REQUIRE-CHANGE-001" :implemented, "MCP-OP-EDIT-025" :implemented, "MCP-OP-ADMIT-091" :implemented, "MCP-OP-THREAD-037" :implemented, "MCP-OP-HELPER-023" :active-gap, "NS-SPLIT-047" :implemented, "NS-SPLIT-051" :implemented, "MCP-OP-ADMIT-145" :implemented, "MCP-OP-HELPER-010" :active-gap, "MCP-OP-READ-NORM-003" :active-gap, "PERF-SENT-SURFACE-001" :active-gap, "MCP-OP-THREAD-044" :implemented, "MCP-OP-ALIAS-037" :implemented, "MCP-OP-HELPER-001" :active-gap, "MCP-OP-ADMIT-108" :implemented, "PERF-SENT-ATTEMPT-003" :active-gap, "MCP-OP-READ-HYP-001" :implemented, "PERF-SENT-ATTEMPT-001" :active-gap, "OP-ALG-RECEIPT-002" :implemented, "OP-ALG-EFFECT-003" :implemented, "NS-SPLIT-069" :implemented, "MCP-OP-TRACE-003" :implemented, "MCP-OP-VERIFY-013" :implemented, "MCP-OP-ADMIT-022" :implemented, "MCP-OP-ADMIT-118" :implemented, "MCP-OP-ADMIT-002" :implemented, "MCP-OP-RESULT-006" :implemented, "OP-ALG-VERIFY-002" :deferred, "MCP-OP-POS-AUTH-009" :implemented, "OP-ALG-SHADOW-001" :implemented, "MCP-OP-ADMIT-133" :implemented, "MCP-OP-ALIAS-021" :implemented, "MCP-OP-TMPHYG-013" :implemented, "MCP-OP-PREP-REQ-008" :implemented, "OP-ALG-CONTEXT-001" :implemented, "NS-SPLIT-012" :implemented, "MCP-OP-CENSUS-008" :implemented, "MCP-OP-THREAD-047" :implemented, "MCP-OP-CENSUS-001" :implemented, "MCP-OP-EDIT-019" :implemented, "MCP-OP-CENSUS-031" :implemented, "MCP-OP-ALIAS-049" :implemented, "NS-SPLIT-061" :implemented, "SPLIT-REPAIR-004" :implemented, "MCP-OP-PREP-ACT-010" :active-gap, "OP-ALG-PARITY-001" :implemented, "WTL-APPLY-008" :implemented, "MCP-OP-CENSUS-033" :implemented, "WTL-INV-004" :implemented, "INSERT-FORMS-009" :implemented, "PERF-SENT-RECEIPT-002" :active-gap, "WTL-PLAN-003" :implemented, "NS-SPLIT-054" :implemented, "PERF-SENT-ATTEMPT-002" :active-gap, "REQUIRE-CHANGE-012" :implemented, "MCP-OP-ALIAS-004" :implemented, "MCP-OP-ADMIT-080" :implemented, "MCP-OP-HELPER-006" :active-gap, "MCP-OP-ALIAS-003" :implemented, "MCP-OP-EDIT-039" :implemented, "TEST-ISO-003" :implemented, "MCP-OP-ALIAS-035" :implemented, "MCP-OP-THREAD-028" :implemented, "MCP-OP-TRACE-005" :implemented, "MCP-OP-THREAD-036" :implemented, "RENAME-ALIAS-006" :implemented, "MCP-OP-ADMIT-040" :implemented, "MCP-OP-ADMIT-092" :implemented, "REQUIRE-CHANGE-007" :implemented, "WTL-SEAL-002" :implemented, "MCP-OP-EDIT-014" :implemented, "SPLIT-REPAIR-003" :implemented, "MCP-OP-ADMIT-097" :implemented, "MCP-OP-ADMIT-055" :implemented, "MCP-OP-EDIT-002" :implemented, "MCP-OP-PREP-ACT-009" :active-gap, "MCP-OP-ADMIT-001" :implemented, "NS-SPLIT-040" :implemented, "MCP-OP-PLAN-005" :implemented, "NS-SPLIT-058" :implemented, "MCP-OP-COVERAGE-002" :implemented, "MCP-OP-ADMIT-126" :implemented, "OP-ALG-CATALOG-001" :implemented, "MCP-OP-ADMIT-146" :implemented, "PERF-SENT-IDENT-002" :active-gap, "MCP-OP-CENSUS-024" :implemented, "MCP-OP-ADMIT-104" :implemented, "MCP-OP-MATCH-002" :implemented, "MCP-OP-READ-CONT-002" :implemented, "MCP-OP-ADMIT-044" :implemented, "MCP-OP-ALIAS-058" :implemented, "WTL-APPLY-001" :implemented, "MCP-OP-ADMIT-122" :implemented, "MCP-OP-CENSUS-011" :implemented, "NS-SPLIT-070" :implemented, "RENAME-ALIAS-015" :implemented, "MCP-OP-ADMIT-052" :implemented, "WTL-PRUNE-007" :implemented, "MCP-OP-ALIAS-048" :implemented, "NS-SPLIT-039" :implemented, "MCP-OP-CENSUS-002" :implemented, "MCP-OP-ADMIT-101" :implemented, "MCP-OP-EDIT-021" :implemented, "MCP-OP-FIELD-009" :implemented, "MCP-OP-THREAD-024" :implemented, "PERF-SENT-VERDICT-001" :active-gap, "NS-SPLIT-066" :implemented, "MCP-OP-CENSUS-014" :implemented, "MCP-OP-HELPER-015" :active-gap, "MCP-OP-PREP-ACT-006" :active-gap, "MCP-OP-MEM-013" :implemented, "WTL-APPLY-011" :implemented, "TEST-ISO-004" :implemented, "WTL-CLI-001" :implemented, "MCP-OP-EDIT-036" :implemented, "RENAME-ALIAS-013" :implemented, "MCP-OP-ADMIT-115" :implemented, "WTL-PRUNE-009" :implemented, "OP-ALG-OUTCOME-001" :implemented, "MCP-OP-RELAY-005" :implemented, "MCP-OP-VERIFY-012" :implemented, "MCP-OP-PREP-ACT-012" :active-gap, "PERF-SENT-ADMIT-001" :active-gap, "MCP-OP-TRACE-004" :implemented, "MCP-OP-WRITE-REFUSAL-005" :deferred, "MCP-OP-THREAD-022" :implemented, "MCP-OP-FIELD-005" :implemented, "MCP-OP-ADMIT-123" :implemented, "MCP-OP-READ-RETRY-002" :deferred, "TEST-ISO-012" :active-gap, "NS-SPLIT-006" :implemented, "MCP-OP-TIME-001" :implemented, "MEASURE-WALL-001" :active-gap, "NS-SPLIT-028" :implemented, "MCP-OP-CENSUS-015" :implemented, "MCP-OP-EDIT-020" :implemented, "NS-SPLIT-068" :implemented, "MCP-OP-CENSUS-025" :implemented, "MCP-OP-ASYNC-004" :implemented, "MCP-OP-THREAD-004" :implemented, "WTL-INV-005" :implemented, "MCP-OP-HELPER-008" :active-gap, "OP-ALG-PARITY-002" :implemented, "INSERT-FORMS-002" :implemented, "MCP-OP-ALIAS-043" :implemented, "MCP-OP-ALIAS-052" :implemented, "MCP-OP-INSERT-008" :implemented, "MCP-OP-THREAD-002" :implemented, "MCP-OP-EDIT-017" :implemented, "NS-SPLIT-030" :implemented, "ROUTING-PARITY-001" :implemented, "WTL-SEAL-007" :implemented, "WTL-PLAN-006" :implemented, "MCP-OP-ALIAS-008" :implemented, "RENAME-ALIAS-002" :implemented, "WTL-INV-003" :implemented, "MCP-OP-ALIAS-012" :implemented, "MCP-OP-RESULT-001" :implemented, "MCP-OP-VERIFY-011" :implemented, "MCP-OP-POS-AUTH-008" :implemented, "WTL-CLI-002" :implemented, "NS-SPLIT-003" :implemented, "MCP-OP-ALIAS-034" :implemented, "MCP-OP-THREAD-003" :implemented, "MCP-OP-RESULT-004" :implemented, "MCP-OP-TIME-002" :implemented, "MCP-OP-ADMIT-087" :implemented, "MCP-OP-MATCH-001" :implemented, "MCP-OP-EDIT-010" :implemented, "MCP-OP-ADMIT-011" :implemented, "MCP-OP-ADMIT-083" :implemented, "MCP-OP-HELPER-024" :active-gap, "PERF-SENT-PUBLISH-001" :active-gap, "OP-ALG-IDENTITY-001" :implemented, "MCP-OP-PLAN-010" :implemented, "PERF-SENT-CUTOVER-001" :active-gap, "MCP-OP-HELPER-017" :active-gap, "MCP-OP-MATCH-003" :implemented, "WTL-SEAL-003" :implemented, "MCP-OP-PREP-REQ-002" :implemented, "TEST-ISO-006" :implemented, "MCP-OP-CENSUS-026" :implemented, "MCP-OP-READ-RETRY-001" :deferred, "WTL-INV-008" :implemented, "MCP-OP-ANALYZER-001" :implemented, "WTL-PRUNE-003" :implemented, "WTL-APPLY-005" :implemented, "MCP-OP-RELAY-004" :implemented, "NS-SPLIT-009" :implemented, "NS-SPLIT-010" :implemented, "MCP-OP-ADMIT-117" :implemented, "OP-ALG-PERF-002" :implemented, "TEST-ISO-013" :implemented, "MCP-OP-THREAD-016" :implemented, "MCP-OP-ALIAS-027" :implemented, "PERF-SENT-RECOVERY-001" :active-gap, "REQUIRE-CHANGE-006" :implemented, "MCP-OP-ADMIT-095" :implemented, "MCP-OP-ADMIT-010" :implemented, "MCP-OP-FIELD-006" :implemented, "ALIAS-MIGRATION-002" :implemented, "RENAME-ALIAS-016" :implemented, "MCP-OP-ADMIT-042" :implemented, "MCP-OP-ALIAS-045" :implemented, "MCP-OP-THREAD-025" :implemented, "MCP-OP-ADMIT-093" :implemented, "MCP-OP-ALIAS-056" :implemented, "MCP-OP-TMPHYG-002" :implemented, "MCP-OP-ADMIT-131" :implemented, "MCP-OP-VERIFY-002" :implemented, "WTL-APPLY-006" :implemented, "MCP-OP-MEM-014" :implemented, "MCP-OP-RESULT-002" :implemented, "MCP-OP-EDIT-029" :implemented, "WTL-CLI-004" :deferred, "MCP-OP-PLAN-008" :implemented, "MCP-OP-ADMIT-031" :implemented, "MCP-OP-ADMIT-129" :implemented, "OP-ALG-RECEIPT-003" :implemented, "MCP-OP-ADMIT-041" :implemented, "MCP-OP-CENSUS-007" :implemented, "MCP-OP-PREP-ACT-016" :active-gap, "MCP-OP-ADMIT-124" :implemented, "WTL-PLAN-005" :implemented, "MCP-OP-FIELD-007" :implemented, "TEST-ISO-007" :implemented, "MCP-OP-MEM-005" :implemented, "MCP-OP-ASYNC-003" :implemented, "MCP-OP-ADMIT-138" :implemented, "MCP-OP-INSERT-006" :active-gap, "PERF-SENT-LEDGER-001" :active-gap, "MCP-OP-ALIAS-010" :implemented, "MCP-OP-ADMIT-140" :implemented, "MCP-OP-ADMIT-043" :implemented, "OP-ALG-RECEIPT-001" :implemented, "PERF-SENT-RECONCILE-001" :active-gap, "MCP-OP-ALIAS-066" :active-gap, "PERF-SENT-RECOVERY-004" :active-gap, "MCP-OP-THREAD-010" :implemented, "PERF-SENT-RECOVERY-003" :active-gap, "PERF-SENT-THRESHOLD-001" :active-gap, "MCP-OP-ADMIT-085" :implemented, "MCP-OP-THREAD-017" :implemented, "WTL-PLAN-001" :implemented, "MCP-OP-CENSUS-032" :implemented, "NS-SPLIT-050" :implemented, "MCP-OP-ANALYZER-005" :implemented, "OP-ALG-PREVIEW-001" :implemented, "WTL-PLAN-002" :implemented, "MCP-OP-READ-DIAG-002" :implemented, "MCP-OP-ADMIT-086" :implemented, "MCP-OP-HELPER-025" :active-gap, "MCP-OP-ADMIT-134" :implemented, "OP-ALG-PERF-001" :implemented, "MCP-OP-ALIAS-031" :implemented, "OP-ALG-MCP-001" :implemented, "TEST-ISO-RACE-001" :implemented, "MEASURE-WALL-003" :active-gap, "MCP-OP-READ-DIAG-003" :implemented, "MCP-OP-EDIT-005" :implemented, "MCP-OP-ADMIT-014" :deferred, "MCP-OP-ADMIT-102" :implemented, "TEST-ISO-001b" :implemented, "MCP-OP-PLAN-006" :implemented, "MCP-OP-THREAD-043" :implemented, "PERF-SENT-INVALID-002" :active-gap, "REQUIRE-CHANGE-014" :implemented, "INSERT-FORMS-024" :implemented, "WTL-PRUNE-006" :implemented, "PERF-SENT-ADMIT-002" :active-gap, "MCP-OP-PLAN-007" :implemented, "BB-PROBE-003" :implemented, "MCP-OP-THREAD-049" :implemented, "MCP-OP-ALIAS-042" :implemented, "MCP-OP-THREAD-005" :implemented, "OP-ALG-COMMIT-002" :implemented, "MCP-OP-ADMIT-114" :implemented, "TEST-ISO-005" :implemented, "MCP-OP-ADMIT-013" :implemented, "MCP-OP-TIME-004" :implemented, "MCP-OP-THREAD-030" :implemented, "MCP-OP-ALIAS-011" :implemented, "MCP-OP-PREP-ACT-011" :active-gap, "MCP-OP-POS-AUTH-006" :implemented, "CLI-PACKAGE-002" :implemented, "MCP-OP-PREP-REQ-001" :implemented, "MCP-OP-ALIAS-064" :active-gap, "MCP-OP-ADMIT-054" :implemented, "NS-SPLIT-011" :implemented, "MCP-OP-READ-NORM-002" :active-gap, "MCP-OP-EDIT-042" :implemented, "MCP-OP-PREP-ACT-008" :active-gap, "MCP-OP-ORACLE-001" :implemented, "WTL-APPLY-012" :implemented, "MCP-OP-EDIT-031" :implemented, "NS-SPLIT-014" :implemented, "MCP-OP-EDIT-026" :implemented, "MCP-OP-PREP-REQ-009" :implemented, "ROUTING-SPLIT-001" :implemented, "MCP-OP-SHELL-ARGV-004" :implemented, "MCP-OP-ADMIT-068" :implemented, "NS-SPLIT-037" :implemented}, :implementation-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "OP-ALG-RUNTIME-001" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "TEST-ISO-009" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "OP-ALG-OUTCOME-002" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "OP-ALG-CONTEXT-002" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "MCP-OP-ADMIT-084" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "MCP-OP-PREP-REQ-002" "MCP-OP-CENSUS-026" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "OP-ALG-RECEIPT-003" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "OP-ALG-MCP-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "OP-ALG-COMMIT-002" "MCP-OP-ADMIT-114" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :test-witnesses #{"MCP-OP-ADMIT-135" "MCP-OP-ADMIT-051" "MCP-OP-HELPER-020" "MCP-OP-THREAD-021" "INSERT-FORMS-020" "MCP-OP-ALIAS-015" "MCP-OP-THREAD-046" "MCP-OP-ALIAS-032" "MCP-OP-HELPER-022" "MCP-OP-ADMIT-020" "WTL-APPLY-007" "MCP-OP-CENSUS-012" "MCP-OP-EDIT-008" "MCP-OP-PREP-ACT-018" "NS-SPLIT-043" "MCP-OP-DISPATCH-004" "MCP-OP-EDIT-035" "MCP-OP-EDIT-024" "MCP-OP-ADMIT-121" "WTL-PRUNE-005" "MCP-OP-VERIFY-010" "MCP-OP-MEM-011" "MCP-OP-PREP-ACT-017" "MCP-OP-ALIAS-002" "OP-ALG-EFFECT-002" "MCP-OP-HELPER-014" "MCP-OP-CENSUS-027" "REQUIRE-CHANGE-005" "MCP-OP-ALIAS-055" "MCP-OP-EDIT-004" "INSERT-FORMS-011" "MCP-OP-ADMIT-152" "NS-SPLIT-057" "TELEMETRY-EVENTS-001" "NS-SPLIT-060" "MCP-OP-ADMIT-106" "MCP-OP-ADMIT-147" "TEST-ISO-RACE-002" "MCP-OP-PREP-ACT-015" "MCP-OP-EDIT-034" "MCP-OP-ADMIT-069" "INSERT-FORMS-006" "MCP-OP-ALIAS-059" "MCP-OP-ADMIT-150" "MCP-OP-INSERT-009" "MCP-OP-CENSUS-029" "BB-PROBE-004" "MCP-OP-VERIFY-009" "MCP-OP-VERIFY-008" "MCP-OP-ALIAS-040" "NS-SPLIT-029" "MCP-OP-CENSUS-017" "MCP-OP-ADMIT-021" "MCP-OP-ADMIT-110" "MCP-OP-FIELD-003" "MCP-OP-PREP-REQ-007" "MCP-OP-ADMIT-151" "MCP-OP-THREAD-042" "MCP-OP-HELPER-002" "ALIAS-MIGRATION-005" "MCP-OP-SHELL-ARGV-006" "MCP-OP-ALIAS-017" "MCP-OP-ADMIT-023" "MCP-OP-PREP-ACT-007" "MCP-OP-PREP-ACT-002" "INSERT-FORMS-010" "MCP-OP-PLAN-001" "WTL-SEAL-006" "MCP-OP-READ-GUARD-001" "MCP-OP-HELPER-011" "MCP-OP-SHELL-ARGV-007" "WTL-PRUNE-001" "MCP-OP-ALIAS-014" "MCP-OP-EDIT-028" "MCP-OP-RESULT-005" "MCP-OP-COVERAGE-001" "NS-SPLIT-072" "MCP-OP-EDIT-007" "WTL-HAND-005" "MCP-OP-MATCHED-001" "MCP-OP-ADMIT-107" "TEST-ISO-002" "MCP-OP-EDIT-012" "MCP-OP-VERIFY-004" "MCP-OP-POS-AUTH-003" "MCP-OP-ALIAS-051" "MCP-OP-HELPER-018" "MCP-OP-ADMIT-144" "MCP-OP-ADMIT-034" "MCP-OP-HELPER-009" "NS-SPLIT-001" "ALIAS-MIGRATION-003" "MCP-OP-ALIAS-046" "MCP-OP-ADMIT-099" "MCP-OP-THREAD-029" "MCP-OP-ADMIT-111" "MCP-OP-ALIAS-065" "MCP-OP-EDIT-018" "MCP-OP-MATCHED-004" "MCP-OP-ALIAS-047" "MCP-OP-PREP-ACT-001" "MCP-OP-THREAD-039" "MCP-OP-ADMIT-032" "MCP-OP-TMPHYG-012" "MCP-OP-HELPER-019" "MCP-OP-PLAN-004" "NS-SPLIT-008" "NS-SPLIT-046" "MCP-OP-TRACE-002" "REQUIRE-CHANGE-004" "MCP-OP-ALIAS-019" "WTL-APPLY-002" "NS-SPLIT-056" "MCP-OP-EDIT-027" "MCP-OP-INSERT-003" "NS-SPLIT-071" "MCP-OP-HOTVER-002" "MCP-OP-ALIAS-036" "REQUIRE-CHANGE-002" "MCP-OP-CENSUS-035" "OP-ALG-EFFECT-005" "MCP-OP-ADMIT-065" "MCP-OP-ANALYZER-008" "MCP-OP-CENSUS-005" "MCP-OP-ADMIT-066" "RENAME-ALIAS-001" "MCP-OP-ALIAS-016" "MCP-OP-ANALYZER-009" "MCP-OP-CENSUS-034" "MCP-OP-ASYNC-002" "MCP-OP-PREP-REQ-005" "OP-ALG-COMMIT-003" "MCP-OP-PREP-ACT-014" "RENAME-ALIAS-003" "MCP-OP-ADMIT-141" "TEST-ISO-014" "MCP-OP-HELPER-021" "WTL-CLI-003" "MCP-OP-ADMIT-148" "MCP-OP-ALIAS-063" "MCP-OP-ANALYZER-006" "INSERT-FORMS-012" "MCP-OP-EDIT-001" "MCP-OP-EDIT-033" "NS-SPLIT-052" "NS-SPLIT-062" "MCP-OP-ADMIT-061" "MCP-OP-EDIT-032" "NS-SPLIT-035" "MCP-OP-ALIAS-044" "MCP-OP-SHELL-ARGV-002" "WTL-HAND-002" "MCP-OP-VERIFY-007" "MCP-OP-MATCHED-005" "MCP-OP-THREAD-006" "WTL-SEAL-001" "MCP-OP-PREP-ACT-005" "RENAME-ALIAS-014" "WTL-INV-001" "MCP-OP-VERIFY-003" "MCP-OP-ADMIT-105" "MCP-OP-ADMIT-088" "MCP-OP-ADMIT-030" "MCP-OP-SCHEMA-001" "INSERT-FORMS-008" "MCP-OP-ADMIT-149" "RENAME-ALIAS-007" "RENAME-ALIAS-010" "MCP-OP-VERIFY-005" "MCP-OP-ADMIT-139" "MCP-OP-THREAD-018" "MCP-OP-VERIFY-006" "MCP-OP-READ-HYP-002" "MCP-OP-THREAD-012" "OP-ALG-STALE-001" "MCP-OP-MEM-020" "MCP-OP-TRACE-006" "INSERT-FORMS-017" "MCP-OP-CENSUS-010" "MCP-OP-ALIAS-054" "MCP-OP-READ-NORM-001" "MCP-OP-EDIT-009" "NS-SPLIT-015" "TEST-ISO-001" "MCP-OP-ALIAS-057" "MCP-OP-ALIAS-009" "WTL-INV-002" "MCP-OP-EDIT-015" "MCP-OP-DISPATCH-002" "MCP-OP-TIME-003" "MCP-OP-CENSUS-028" "NS-SPLIT-041" "MCP-OP-POS-AUTH-007" "MCP-OP-MATCHED-002" "MCP-OP-ANALYZER-002" "TEST-ISO-010" "WTL-PLAN-004" "MCP-OP-MEM-007" "MCP-OP-ALIAS-013" "MCP-OP-DISPATCH-003" "MCP-OP-TMPHYG-009" "WTL-PRUNE-002" "NS-SPLIT-064" "MCP-OP-EDIT-037" "MCP-OP-HOTVER-001" "MCP-OP-POS-AUTH-004" "MCP-OP-ADMIT-100" "NS-SPLIT-049" "MCP-OP-TMPHYG-011" "MCP-OP-ADMIT-120" "MCP-OP-ADMIT-060" "WTL-HAND-003" "MCP-OP-THREAD-001" "MCP-OP-EDIT-023" "MCP-OP-CENSUS-030" "MCP-OP-RESULT-003" "INSERT-FORMS-019" "MCP-OP-ADMIT-033" "WTL-APPLY-004" "MCP-OP-THREAD-008" "MCP-OP-ADMIT-112" "MCP-OP-THREAD-015" "MCP-OP-EDIT-016" "MCP-OP-ALIAS-028" "WTL-PRUNE-010" "MCP-OP-ALIAS-039" "RECEIPT-BOOL-001" "MCP-OP-ADMIT-081" "MCP-OP-CENSUS-003" "MCP-OP-CENSUS-019" "MCP-OP-ALIAS-067" "MCP-OP-CENSUS-020" "RENAME-ALIAS-008" "MCP-OP-ADMIT-062" "REQUIRE-CHANGE-008" "MCP-OP-FIELD-002" "MCP-OP-READ-NORM-004" "MCP-OP-THREAD-041" "MCP-OP-THREAD-011" "MCP-OP-TMPHYG-003" "MCP-OP-MEM-015" "MCP-OP-THREAD-051" "OP-ALG-COMPILE-001" "MCP-OP-ALIAS-030" "BB-PROBE-001" "MCP-OP-DISPATCH-005" "MCP-OP-POS-AUTH-010" "MCP-OP-THREAD-038" "MCP-OP-EDIT-011" "MCP-OP-ADMIT-004" "NS-SPLIT-013" "MCP-OP-ASYNC-001" "MCP-OP-ADMIT-070" "MCP-OP-ALIAS-022" "MCP-OP-ADMIT-142" "TEST-ISO-009b" "MCP-OP-PREP-ACT-013" "MCP-OP-ALIAS-005" "NS-SPLIT-005" "NS-SPLIT-004" "MCP-OP-HELPER-007" "MCP-OP-HELPER-005" "NS-SPLIT-055" "MCP-OP-READ-CONT-001" "MCP-OP-ALIAS-023" "MCP-OP-THREAD-035" "MCP-OP-INSERT-010" "CLI-PACKAGE-001" "MCP-OP-ALIAS-038" "RENAME-ALIAS-012" "INSERT-FORMS-018" "MCP-OP-ADMIT-103" "MCP-OP-CENSUS-006" "INSERT-FORMS-016" "MCP-OP-THREAD-014" "SPLIT-REPAIR-002" "MCP-OP-HELPER-013" "MCP-OP-DISPATCH-001" "INSERT-FORMS-023" "MCP-OP-ADMIT-125" "INSERT-FORMS-021" "MCP-OP-ANALYZER-004" "MCP-OP-THREAD-023" "NS-SPLIT-036" "OP-ALG-COMMIT-001" "MCP-OP-ADMIT-098" "MCP-OP-ALIAS-050" "MCP-OP-ANALYZER-003" "MCP-OP-ALIAS-026" "MCP-OP-PREP-ACT-003" "TEST-ISO-015" "WTL-APPLY-009" "WTL-PRUNE-004" "MCP-OP-ALIAS-061" "MCP-OP-FIELD-008" "MCP-OP-THREAD-031" "MCP-OP-THREAD-019" "MCP-OP-ADMIT-096" "MCP-OP-RELAY-002" "WTL-HAND-001" "MCP-OP-CENSUS-018" "MCP-OP-WRITE-REFUSAL-001" "NS-SPLIT-065" "MCP-OP-HELPER-004" "TEST-ISO-016" "MCP-OP-ALIAS-007" "OP-ALG-CLI-001" "MCP-OP-THREAD-032" "MCP-OP-ADMIT-119" "MCP-OP-TMPHYG-006" "MCP-OP-HELPER-016" "MCP-OP-ALIAS-060" "MCP-OP-PLAN-002" "WTL-INV-007" "MCP-OP-ADMIT-024" "NS-SPLIT-034" "MCP-OP-TMPHYG-004" "MCP-OP-THREAD-045" "NS-SPLIT-045" "REQUIRE-CHANGE-011" "MCP-OP-THREAD-040" "MCP-OP-RELAY-001" "MCP-OP-CENSUS-004" "MCP-OP-SHELL-ARGV-005" "MCP-OP-CENSUS-023" "MCP-OP-ADMIT-064" "NS-SPLIT-033" "MCP-OP-READ-DIAG-001" "INSERT-FORMS-015" "NS-SPLIT-038" "MCP-OP-THREAD-020" "MCP-OP-TMPHYG-001" "MCP-OP-READ-PARITY-001" "MCP-OP-ADMIT-012" "ROUTING-FANOUT-001" "MCP-OP-EDIT-038" "INSERT-FORMS-013" "RENAME-ALIAS-004" "MCP-OP-HELPER-012" "NS-SPLIT-059" "MCP-OP-ANALYZER-007" "MCP-OP-PREP-REQ-004" "MCP-OP-CENSUS-021" "MCP-OP-THREAD-033" "WTL-SEAL-004" "RENAME-ALIAS-009" "MCP-OP-ALIAS-024" "MCP-OP-INSERT-001" "MCP-OP-POS-AUTH-001" "MCP-OP-VERIFY-001" "MCP-OP-ALIAS-041" "NS-SPLIT-002" "MCP-OP-TRACE-001" "REQUIRE-CHANGE-009" "MCP-OP-ALIAS-025" "MCP-OP-ADMIT-094" "RENAME-ALIAS-011" "MCP-OP-THREAD-050" "INSERT-FORMS-005" "REQUIRE-CHANGE-003" "MCP-OP-ADMIT-063" "WTL-APPLY-010" "MCP-OP-ALIAS-029" "MCP-OP-THREAD-052" "MCP-OP-MEM-001" "MCP-OP-ALIAS-018" "MCP-OP-ADMIT-050" "MCP-OP-ADMIT-137" "MCP-OP-ADMIT-053" "MCP-OP-EDIT-003" "MCP-OP-ADMIT-132" "MCP-OP-ADMIT-067" "MCP-OP-ADMIT-090" "MCP-OP-CENSUS-022" "INSERT-FORMS-022" "MCP-OP-TMPHYG-005" "MCP-OP-HELPER-003" "MCP-OP-SHELL-ARGV-003" "OP-ALG-EFFECT-001" "INSERT-FORMS-001" "INSERT-FORMS-007" "MCP-OP-ALIAS-001" "MCP-OP-ALIAS-006" "MCP-OP-CENSUS-013" "NS-SPLIT-007" "MCP-OP-THREAD-027" "MCP-OP-ADMIT-113" "SPLIT-REPAIR-001" "MCP-OP-THREAD-007" "MCP-OP-ADMIT-116" "MCP-OP-PREP-ACT-004" "MCP-OP-PLAN-009" "MCP-OP-ALIAS-020" "MCP-OP-THREAD-048" "MCP-OP-ADMIT-005" "MCP-OP-PREP-REQ-003" "MCP-OP-SHELL-ARGV-001" "MCP-OP-TMPHYG-008" "MCP-OP-PREP-REQ-006" "REQUIRE-CHANGE-010" "MCP-OP-ADMIT-109" "MCP-OP-EDIT-022" "MCP-OP-ADMIT-127" "WTL-SEAL-005" "MCP-OP-TMPHYG-007" "MCP-OP-ADMIT-082" "MCP-OP-RELAY-003" "NS-SPLIT-067" "NS-SPLIT-032" "MCP-OP-EDIT-041" "INSERT-FORMS-004" "MCP-OP-THREAD-013" "RENAME-ALIAS-005" "MCP-OP-EDIT-040" "MCP-OP-ALIAS-033" "MCP-OP-ADMIT-003" "NS-SPLIT-048" "MCP-OP-POS-AUTH-002" "ALIAS-MIGRATION-004" "MCP-OP-MEM-006" "MCP-OP-ADMIT-089" "MCP-OP-ADMIT-136" "MCP-OP-INSERT-002" "MCP-OP-FIELD-001" "MCP-OP-POS-AUTH-005" "OP-ALG-FORM-COUNT-001" "MCP-OP-MATCH-004" "MCP-OP-ADMIT-071" "MCP-OP-THREAD-026" "MCP-OP-ALIAS-053" "TEST-ISO-009a" "ALIAS-MIGRATION-001" "INSERT-FORMS-003" "MCP-OP-EDIT-006" "MCP-OP-CENSUS-009" "MCP-OP-PLAN-003" "MCP-OP-INSERT-005" "MCP-OP-INSERT-004" "MCP-OP-ADMIT-143" "MCP-OP-EDIT-030" "MCP-OP-ALIAS-062" "MCP-OP-TMPHYG-010" "MCP-OP-THREAD-034" "MCP-OP-READ-NORM-005" "MCP-OP-CENSUS-016" "MCP-OP-EDIT-013" "WTL-INV-006" "MCP-OP-ADMIT-084" "WTL-APPLY-003" "INSERT-FORMS-014" "MCP-OP-THREAD-009" "NS-SPLIT-053" "MCP-OP-INSERT-007" "NS-SPLIT-044" "NS-SPLIT-063" "MCP-OP-MATCHED-003" "MCP-OP-MEM-012" "MCP-OP-ASYNC-005" "REQUIRE-CHANGE-013" "MCP-OP-ADMIT-130" "MCP-OP-ADMIT-128" "BB-PROBE-002" "REQUIRE-CHANGE-001" "MCP-OP-EDIT-025" "MCP-OP-ADMIT-091" "MCP-OP-THREAD-037" "MCP-OP-HELPER-023" "NS-SPLIT-047" "NS-SPLIT-051" "MCP-OP-ADMIT-145" "MCP-OP-HELPER-010" "MCP-OP-READ-NORM-003" "MCP-OP-THREAD-044" "MCP-OP-ALIAS-037" "MCP-OP-HELPER-001" "MCP-OP-ADMIT-108" "MCP-OP-READ-HYP-001" "OP-ALG-EFFECT-003" "NS-SPLIT-069" "MCP-OP-TRACE-003" "MCP-OP-VERIFY-013" "MCP-OP-ADMIT-022" "MCP-OP-ADMIT-118" "MCP-OP-ADMIT-002" "MCP-OP-RESULT-006" "MCP-OP-POS-AUTH-009" "OP-ALG-SHADOW-001" "MCP-OP-ADMIT-133" "MCP-OP-ALIAS-021" "MCP-OP-TMPHYG-013" "MCP-OP-PREP-REQ-008" "OP-ALG-CONTEXT-001" "NS-SPLIT-012" "MCP-OP-CENSUS-008" "MCP-OP-THREAD-047" "MCP-OP-CENSUS-001" "MCP-OP-EDIT-019" "MCP-OP-CENSUS-031" "MCP-OP-ALIAS-049" "NS-SPLIT-061" "SPLIT-REPAIR-004" "MCP-OP-PREP-ACT-010" "WTL-APPLY-008" "MCP-OP-CENSUS-033" "WTL-INV-004" "INSERT-FORMS-009" "WTL-PLAN-003" "NS-SPLIT-054" "REQUIRE-CHANGE-012" "MCP-OP-ALIAS-004" "MCP-OP-ADMIT-080" "MCP-OP-HELPER-006" "MCP-OP-ALIAS-003" "MCP-OP-EDIT-039" "TEST-ISO-003" "MCP-OP-ALIAS-035" "MCP-OP-THREAD-028" "MCP-OP-TRACE-005" "MCP-OP-THREAD-036" "RENAME-ALIAS-006" "MCP-OP-ADMIT-040" "MCP-OP-ADMIT-092" "REQUIRE-CHANGE-007" "WTL-SEAL-002" "MCP-OP-EDIT-014" "SPLIT-REPAIR-003" "MCP-OP-ADMIT-097" "MCP-OP-ADMIT-055" "MCP-OP-EDIT-002" "MCP-OP-PREP-ACT-009" "MCP-OP-ADMIT-001" "NS-SPLIT-040" "MCP-OP-PLAN-005" "NS-SPLIT-058" "MCP-OP-COVERAGE-002" "MCP-OP-ADMIT-126" "OP-ALG-CATALOG-001" "MCP-OP-ADMIT-146" "MCP-OP-CENSUS-024" "MCP-OP-ADMIT-104" "MCP-OP-MATCH-002" "MCP-OP-READ-CONT-002" "MCP-OP-ADMIT-044" "MCP-OP-ALIAS-058" "WTL-APPLY-001" "MCP-OP-ADMIT-122" "MCP-OP-CENSUS-011" "NS-SPLIT-070" "RENAME-ALIAS-015" "MCP-OP-ADMIT-052" "WTL-PRUNE-007" "MCP-OP-ALIAS-048" "NS-SPLIT-039" "MCP-OP-CENSUS-002" "MCP-OP-ADMIT-101" "MCP-OP-EDIT-021" "MCP-OP-FIELD-009" "MCP-OP-THREAD-024" "NS-SPLIT-066" "MCP-OP-CENSUS-014" "MCP-OP-HELPER-015" "MCP-OP-PREP-ACT-006" "MCP-OP-MEM-013" "WTL-APPLY-011" "TEST-ISO-004" "WTL-CLI-001" "MCP-OP-EDIT-036" "RENAME-ALIAS-013" "MCP-OP-ADMIT-115" "OP-ALG-OUTCOME-001" "MCP-OP-RELAY-005" "MCP-OP-VERIFY-012" "MCP-OP-PREP-ACT-012" "MCP-OP-TRACE-004" "MCP-OP-THREAD-022" "MCP-OP-FIELD-005" "MCP-OP-ADMIT-123" "NS-SPLIT-006" "MCP-OP-TIME-001" "NS-SPLIT-028" "MCP-OP-CENSUS-015" "MCP-OP-EDIT-020" "NS-SPLIT-068" "MCP-OP-CENSUS-025" "MCP-OP-ASYNC-004" "MCP-OP-THREAD-004" "WTL-INV-005" "MCP-OP-HELPER-008" "INSERT-FORMS-002" "MCP-OP-ALIAS-043" "MCP-OP-ALIAS-052" "MCP-OP-INSERT-008" "MCP-OP-THREAD-002" "MCP-OP-EDIT-017" "NS-SPLIT-030" "ROUTING-PARITY-001" "WTL-SEAL-007" "MCP-OP-ALIAS-008" "RENAME-ALIAS-002" "WTL-INV-003" "MCP-OP-ALIAS-012" "MCP-OP-RESULT-001" "MCP-OP-VERIFY-011" "MCP-OP-POS-AUTH-008" "WTL-CLI-002" "NS-SPLIT-003" "MCP-OP-ALIAS-034" "MCP-OP-THREAD-003" "MCP-OP-RESULT-004" "MCP-OP-TIME-002" "MCP-OP-ADMIT-087" "MCP-OP-MATCH-001" "MCP-OP-EDIT-010" "MCP-OP-ADMIT-011" "MCP-OP-ADMIT-083" "MCP-OP-HELPER-024" "OP-ALG-IDENTITY-001" "MCP-OP-PLAN-010" "MCP-OP-HELPER-017" "MCP-OP-MATCH-003" "WTL-SEAL-003" "MCP-OP-PREP-REQ-002" "TEST-ISO-006" "MCP-OP-CENSUS-026" "WTL-INV-008" "MCP-OP-ANALYZER-001" "WTL-PRUNE-003" "WTL-APPLY-005" "MCP-OP-RELAY-004" "NS-SPLIT-009" "NS-SPLIT-010" "MCP-OP-ADMIT-117" "TEST-ISO-013" "MCP-OP-THREAD-016" "MCP-OP-ALIAS-027" "REQUIRE-CHANGE-006" "MCP-OP-ADMIT-095" "MCP-OP-ADMIT-010" "MCP-OP-FIELD-006" "ALIAS-MIGRATION-002" "RENAME-ALIAS-016" "MCP-OP-ADMIT-042" "MCP-OP-ALIAS-045" "MCP-OP-THREAD-025" "MCP-OP-ADMIT-093" "MCP-OP-ALIAS-056" "MCP-OP-TMPHYG-002" "MCP-OP-ADMIT-131" "MCP-OP-VERIFY-002" "WTL-APPLY-006" "MCP-OP-MEM-014" "MCP-OP-RESULT-002" "MCP-OP-EDIT-029" "MCP-OP-PLAN-008" "MCP-OP-ADMIT-031" "MCP-OP-ADMIT-129" "MCP-OP-ADMIT-041" "MCP-OP-CENSUS-007" "MCP-OP-PREP-ACT-016" "MCP-OP-ADMIT-124" "WTL-PLAN-005" "MCP-OP-FIELD-007" "TEST-ISO-007" "MCP-OP-MEM-005" "MCP-OP-ASYNC-003" "MCP-OP-ADMIT-138" "MCP-OP-INSERT-006" "MCP-OP-ALIAS-010" "MCP-OP-ADMIT-140" "MCP-OP-ADMIT-043" "MCP-OP-ALIAS-066" "MCP-OP-THREAD-010" "MCP-OP-ADMIT-085" "MCP-OP-THREAD-017" "WTL-PLAN-001" "MCP-OP-CENSUS-032" "NS-SPLIT-050" "MCP-OP-ANALYZER-005" "MCP-OP-READ-DIAG-002" "MCP-OP-ADMIT-086" "MCP-OP-HELPER-025" "MCP-OP-ADMIT-134" "MCP-OP-ALIAS-031" "TEST-ISO-RACE-001" "MCP-OP-READ-DIAG-003" "MCP-OP-EDIT-005" "MCP-OP-ADMIT-102" "MCP-OP-PLAN-006" "MCP-OP-THREAD-043" "REQUIRE-CHANGE-014" "INSERT-FORMS-024" "WTL-PRUNE-006" "MCP-OP-PLAN-007" "BB-PROBE-003" "MCP-OP-THREAD-049" "MCP-OP-ALIAS-042" "MCP-OP-THREAD-005" "MCP-OP-ADMIT-114" "TEST-ISO-005" "MCP-OP-ADMIT-013" "MCP-OP-TIME-004" "MCP-OP-THREAD-030" "MCP-OP-ALIAS-011" "MCP-OP-PREP-ACT-011" "MCP-OP-POS-AUTH-006" "CLI-PACKAGE-002" "MCP-OP-PREP-REQ-001" "MCP-OP-ALIAS-064" "MCP-OP-ADMIT-054" "NS-SPLIT-011" "MCP-OP-READ-NORM-002" "MCP-OP-EDIT-042" "MCP-OP-PREP-ACT-008" "MCP-OP-ORACLE-001" "WTL-APPLY-012" "MCP-OP-EDIT-031" "NS-SPLIT-014" "MCP-OP-EDIT-026" "MCP-OP-PREP-REQ-009" "ROUTING-SPLIT-001" "MCP-OP-SHELL-ARGV-004" "MCP-OP-ADMIT-068" "NS-SPLIT-037"}, :violations [], :pending-witness-violations [{:type :missing-test-witness, :intent "MEASURE-EVID-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-001", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-002", :source-kind :test} {:type :missing-test-witness, :intent "MEASURE-WALL-003", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-COMMIT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-COMMIT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-CONTEXT-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-DECODE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-DECODE-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-EFFECT-004", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-EFFECT-004", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-MCP-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-OUTCOME-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PARITY-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PARITY-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PERF-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PERF-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-PREVIEW-002", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RECEIPT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-REFUSE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "OP-ALG-REFUSE-001", :source-kind :test} {:type :missing-test-witness, :intent "OP-ALG-RUNTIME-001", :source-kind :test} {:type :missing-implementation-witness, :intent "OP-ALG-SHADOW-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "OP-ALG-STALE-001", :source-kind :implementation} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ADMIT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-ATTEMPT-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-AUTH-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BACKFILL-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-BASELINE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CLIENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CONFIG-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-CUTOVER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IDENT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-IMPORT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-INVALID-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-LEDGER-ROOT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROJECT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PROMOTION-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-PUBLISH-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECEIPT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECONCILE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RECOVERY-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RELEASE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-RETAIN-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-003", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SCHEDULE-004", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-SURFACE-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-THRESHOLD-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-TIME-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-001", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-002", :source-kind :test} {:type :missing-test-witness, :intent "PERF-SENT-VERDICT-003", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001a", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001a", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001b", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001b", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-001c", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-001c", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-007", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-008", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-009", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-010", :source-kind :implementation} {:type :missing-test-witness, :intent "TEST-ISO-011", :source-kind :test} {:type :missing-test-witness, :intent "TEST-ISO-012", :source-kind :test} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-001", :source-kind :implementation} {:type :missing-implementation-witness, :intent "TEST-ISO-RACE-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-007", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-010", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-011", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-APPLY-012", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-CLI-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-HAND-004", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-HAND-004", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-HAND-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-005", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-INV-008", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-002", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-002", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PLAN-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-PLAN-006", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PLAN-006", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-008", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-008", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-PRUNE-009", :source-kind :implementation} {:type :missing-test-witness, :intent "WTL-PRUNE-009", :source-kind :test} {:type :missing-implementation-witness, :intent "WTL-SEAL-002", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-003", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-004", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-006", :source-kind :implementation} {:type :missing-implementation-witness, :intent "WTL-SEAL-007", :source-kind :implementation}], :witness-debt-ledger {"WTL-APPLY-007" #{:implementation}, "PERF-SENT-IMPORT-001" #{:test}, "OP-ALG-EFFECT-002" #{:implementation}, "TEST-ISO-RACE-002" #{:implementation}, "OP-ALG-PREVIEW-002" #{:implementation :test}, "PERF-SENT-VERDICT-002" #{:test}, "PERF-SENT-AUTH-003" #{:test}, "PERF-SENT-VERDICT-003" #{:test}, "PERF-SENT-PROJECT-001" #{:test}, "TEST-ISO-001a" #{:implementation :test}, "PERF-SENT-LEDGER-004" #{:test}, "WTL-SEAL-006" #{:implementation}, "PERF-SENT-RELEASE-001" #{:test}, "WTL-HAND-005" #{:implementation}, "PERF-SENT-TIME-001" #{:test}, "MEASURE-EVID-001" #{:test}, "WTL-APPLY-002" #{:implementation}, "PERF-SENT-SCHEDULE-002" #{:test}, "PERF-SENT-CONFIG-001" #{:test}, "WTL-CLI-003" #{:implementation}, "OP-ALG-RUNTIME-001" #{:test}, "WTL-HAND-002" #{:implementation}, "PERF-SENT-CUTOVER-002" #{:test}, "OP-ALG-STALE-001" #{:implementation}, "WTL-INV-002" #{:implementation}, "PERF-SENT-AUTH-001" #{:test}, "TEST-ISO-010" #{:implementation}, "WTL-PLAN-004" #{:implementation}, "WTL-PRUNE-008" #{:implementation :test}, "PERF-SENT-LEDGER-003" #{:test}, "WTL-APPLY-004" #{:implementation}, "TEST-ISO-009" #{:test}, "PERF-SENT-SCHEDULE-003" #{:test}, "OP-ALG-EFFECT-004" #{:implementation :test}, "OP-ALG-DECODE-001" #{:implementation :test}, "PERF-SENT-BASELINE-001" #{:test}, "PERF-SENT-AUTH-002" #{:test}, "PERF-SENT-CUTOVER-003" #{:test}, "OP-ALG-OUTCOME-003" #{:implementation :test}, "TEST-ISO-001c" #{:implementation :test}, "PERF-SENT-RETAIN-001" #{:test}, "PERF-SENT-CLIENT-001" #{:test}, "WTL-SEAL-004" #{:implementation}, "PERF-SENT-RECEIPT-001" #{:test}, "PERF-SENT-INVALID-001" #{:test}, "OP-ALG-OUTCOME-002" #{:test}, "WTL-APPLY-010" #{:implementation}, "PERF-SENT-RECOVERY-002" #{:test}, "OP-ALG-REFUSE-001" #{:implementation :test}, "PERF-SENT-IDENT-001" #{:test}, "PERF-SENT-SCHEDULE-001" #{:test}, "MEASURE-WALL-002" #{:test}, "PERF-SENT-PROMOTION-002" #{:test}, "TEST-ISO-008" #{:test}, "PERF-SENT-SCHEDULE-004" #{:test}, "WTL-HAND-004" #{:implementation :test}, "OP-ALG-COMMIT-004" #{:implementation :test}, "PERF-SENT-IMPORT-002" #{:test}, "PERF-SENT-BACKFILL-001" #{:test}, "PERF-SENT-LEDGER-002" #{:test}, "TEST-ISO-011" #{:test}, "PERF-SENT-PUBLISH-002" #{:test}, "PERF-SENT-LEDGER-ROOT-001" #{:test}, "OP-ALG-CONTEXT-002" #{:test}, "WTL-INV-006" #{:implementation}, "WTL-APPLY-003" #{:implementation}, "PERF-SENT-PROMOTION-001" #{:test}, "PERF-SENT-SURFACE-001" #{:test}, "PERF-SENT-ATTEMPT-003" #{:test}, "PERF-SENT-ATTEMPT-001" #{:test}, "OP-ALG-RECEIPT-002" #{:implementation :test}, "OP-ALG-EFFECT-003" #{:implementation}, "OP-ALG-SHADOW-001" #{:implementation}, "OP-ALG-PARITY-001" #{:implementation :test}, "WTL-APPLY-008" #{:implementation}, "PERF-SENT-RECEIPT-002" #{:test}, "WTL-PLAN-003" #{:implementation}, "PERF-SENT-ATTEMPT-002" #{:test}, "TEST-ISO-003" #{:implementation}, "WTL-SEAL-002" #{:implementation}, "PERF-SENT-IDENT-002" #{:test}, "PERF-SENT-VERDICT-001" #{:test}, "WTL-APPLY-011" #{:implementation}, "TEST-ISO-004" #{:implementation}, "WTL-PRUNE-009" #{:implementation :test}, "OP-ALG-OUTCOME-001" #{:implementation}, "PERF-SENT-ADMIT-001" #{:test}, "TEST-ISO-012" #{:test}, "MEASURE-WALL-001" #{:test}, "WTL-INV-005" #{:implementation}, "OP-ALG-PARITY-002" #{:implementation :test}, "WTL-SEAL-007" #{:implementation}, "WTL-PLAN-006" #{:implementation :test}, "WTL-INV-003" #{:implementation}, "WTL-CLI-002" #{:implementation}, "PERF-SENT-PUBLISH-001" #{:test}, "PERF-SENT-CUTOVER-001" #{:test}, "WTL-SEAL-003" #{:implementation}, "TEST-ISO-006" #{:implementation}, "WTL-INV-008" #{:implementation}, "OP-ALG-PERF-002" #{:implementation :test}, "PERF-SENT-RECOVERY-001" #{:test}, "WTL-APPLY-006" #{:implementation}, "OP-ALG-RECEIPT-003" #{:test}, "TEST-ISO-007" #{:implementation}, "PERF-SENT-LEDGER-001" #{:test}, "OP-ALG-RECEIPT-001" #{:implementation :test}, "PERF-SENT-RECONCILE-001" #{:test}, "PERF-SENT-RECOVERY-004" #{:test}, "PERF-SENT-RECOVERY-003" #{:test}, "PERF-SENT-THRESHOLD-001" #{:test}, "OP-ALG-PREVIEW-001" #{:implementation :test}, "WTL-PLAN-002" #{:implementation :test}, "OP-ALG-PERF-001" #{:implementation :test}, "OP-ALG-MCP-001" #{:test}, "TEST-ISO-RACE-001" #{:implementation}, "MEASURE-WALL-003" #{:test}, "TEST-ISO-001b" #{:implementation :test}, "PERF-SENT-INVALID-002" #{:test}, "PERF-SENT-ADMIT-002" #{:test}, "OP-ALG-COMMIT-002" #{:test}, "TEST-ISO-005" #{:implementation}, "WTL-APPLY-012" #{:implementation}}}
gate-stage: {:target "intent-audit", :exit 0, :wall-ms 2159}
landing-gate: {:pool {:target "runtime-pool", :exit 0, :wall-ms 159083, :phases [{:phase 0, :process-count 15, :wall-ms 25936} {:phase 1, :process-count 23, :wall-ms 133093}], :preparation {:command ["clojure" "-J-Xms64m" "-J-Xmx512m" "-Spath" "-M:clj-surgeon/test-deps"], :exit 0, :wall-ms 52}, :peak-worker-count 8, :shell-checks {:namespaces [], :index 0, :exit 0, :started-ms 1789249640987, :phase 1, :err-log "target/gate-prewarm/e5939d95-8159-4eac-8057-5cd46b00909d/lane-0.err", :runtime nil, :wall-ms 133093, :suite "shell", :completed-ms 1789249774080, :log "target/gate-prewarm/e5939d95-8159-4eac-8057-5cd46b00909d/lane-0.out"}}, :stages [{:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11268} {:target "alias-migration-test", :exit 0, :wall-ms 93679} {:target "mcp-test", :exit 0, :wall-ms 94257} {:target "test-bb", :exit 0, :wall-ms 91625} {:target "test-bb-diagnostic", :exit 0, :wall-ms 226024} {:target "repository-hygiene", :exit 0, :wall-ms 2090} {:target "intent-audit", :exit 0, :wall-ms 2159}], :landing? false, :problems [], :capacity {:lanes 8, :memory-mib 25379, :admission :abstract-socket, :lane-charge-mib 1536, :reserve-mib 2048, :slot-root "abstract", :floor-mib 3584, :heap-mib 512, :cpus 16}, :started-at "2026-09-12T21:46:43.387672532Z", :state :passed, :git-tree "8e2ce5a69d7ac02b87e12fae5502e5472f22655b", :wall-ms 401271, :completed-at "2026-09-12T21:53:24.659224448Z", :source-digest "b4f3ebcafa447e61e6f2c34c700bb561ae63a017007a251d17c792b2dae71659", :prewarm? true, :run-id "e5939d95-8159-4eac-8057-5cd46b00909d", :git-head "925a40d7d440069b3396b11690a740b38246e4a4"}
EXIT=0 WALL_S=403
