STARTED=2026-09-12T20:54:45Z
c53fd9c59b44e3e72b4c9fd9ee4203d7018de01d
COMMAND: make landing-gate-prewarm
bb -Xmx1g -Djava.io.tmpdir="/var/tmp/forge/bbtower-fx" -m clj-surgeon.battery-parallel-runner --suite gate --prewarm true
gate-capacity: 24531 MiB available, 16 cpus, 8 lane(s); slots abstract-socket abstract; 3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
gate-stage: admit-transaction-recovery-battery started 2026-09-12T20:54:47.654660399Z
java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -Xmx512m
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=129
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=137
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=193
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11125}
battery-parallel: 2 namespace(s) in 2 unit(s) over 2 lane(s); floor is clj-surgeon.mcp-alias-migration-test at 82273 ms
  process 0 phase 0 (est 4380 ms): clj-surgeon.receipt-artifacts-boundary-test
  process 1 phase 0 (est 82273 ms): clj-surgeon.mcp-alias-migration-test
battery-parallel: 101 namespace(s) in 101 unit(s) over 8 lane(s); floor is clj-surgeon.mcp-feature-thread-test at 48878 ms
  process 0 phase 0 (est 4519 ms): clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-read-request-normalization-test
  process 1 phase 0 (est 856 ms): clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-compact-edit-test
  process 2 phase 0 (est 4424 ms): clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-semantic-client-test
  process 3 phase 0 (est 951 ms): clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test
  process 4 phase 0 (est 4953 ms): clj-surgeon.mcp-inspect-tool-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-operation-test
  process 5 phase 0 (est 422 ms): clj-surgeon.mcp-intent-contract-test clj-surgeon.move-dependency-test clj-surgeon.telemetry-events-test clj-surgeon.fast-lane-isolation-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-typist-test clj-surgeon.outline-differential-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test
  process 6 phase 0 (est 5201 ms): clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test
  process 7 phase 0 (est 174 ms): clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.mission-plain-forms-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test
  process 8 phase 0 (est 4396 ms): clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.outline-memory-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-write-refusal-test
  process 9 phase 0 (est 979 ms): clj-surgeon.helper-extraction-test clj-surgeon.agent-routing-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.mcp-contract-test clj-surgeon.mcp-recovery-test clj-surgeon.forms-test
  process 10 phase 0 (est 4689 ms): clj-surgeon.rename-alias-test
  process 11 phase 0 (est 686 ms): clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  process 12 phase 0 (est 5197 ms): clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-operation-async-test
  process 13 phase 0 (est 179 ms): clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  process 14 phase 0 (est 8655 ms): clj-surgeon.mcp-compact-relations-test
  process 15 phase 1 (est 264 ms): clj-surgeon.mcp-server-test
  process 16 phase 1 (est 1429 ms): clj-surgeon.mcp-hot-verify-test
  process 17 phase 1 (est 1728 ms): clj-surgeon.namespace-split-warm-test
  process 18 phase 1 (est 2814 ms): clj-surgeon.mcp-http-server-test
  process 19 phase 1 (est 3004 ms): clj-surgeon.mcp-tool-test
  process 20 phase 1 (est 9066 ms): clj-surgeon.outline-corpus-integration-test
  process 21 phase 1 (est 48878 ms): clj-surgeon.mcp-feature-thread-test
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

========== lane 0 (clj-surgeon.receipt-artifacts-boundary-test) exit 0, 15099 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.receipt-artifacts-boundary-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4238 ms):
      4238 ms  clj-surgeon.receipt-artifacts-boundary-test

========== lane 1 (clj-surgeon.mcp-alias-migration-test) exit 0, 94691 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.mcp-alias-migration-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 82133 ms):
     82133 ms  clj-surgeon.mcp-alias-migration-test

Ran 182 tests containing 3662 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 94691 ms
cadence :battery: serial-equivalent 86371 ms; budget 1800000 ms
bb-runtime: serial-equivalent 0 ms; budget 343102 ms; makespan unmeasured ms

namespace walls (2, slowest first, serial-equivalent total 86371 ms):
     82133 ms  clj-surgeon.mcp-alias-migration-test
      4238 ms  clj-surgeon.receipt-artifacts-boundary-test

lanes (2), makespan 94691 ms:
  lane 0     15099 ms  exit 0  clj-surgeon.receipt-artifacts-boundary-test
  lane 1     94691 ms  exit 0  clj-surgeon.mcp-alias-migration-test

test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 94691 ms over 2 lane(s); serial-equivalent 86371 ms; skipped 0

========== lane 0 (clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-read-request-normalization-test) exit 0, 5357 ms ==========

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

Testing clj-surgeon.namespace-split-test

Testing clj-surgeon.analyze-test

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.require-change-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.mission-forms-source-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.battery-ledger-test

Testing clj-surgeon.mcp-read-request-normalization-test

========== lane 1 (clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-compact-edit-test) exit 0, 13830 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mission-candidate-race-test

Testing clj-surgeon.mcp-extraction-plan-test

Testing clj-surgeon.mcp-compact-edit-test
---------- lane 1 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 695 ms):
       286 ms  clj-surgeon.mcp-extraction-plan-test
       216 ms  clj-surgeon.mission-candidate-race-test
       193 ms  clj-surgeon.mcp-compact-edit-test

========== lane 2 (clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-semantic-client-test) exit 0, 24947 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.ns-isolation-test

Testing clj-surgeon.splice-envelope-test

Testing clj-surgeon.receipt-booleans-test

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

Testing clj-surgeon.mcp-semantic-client-test
---------- lane 2 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 3639 ms):
      1576 ms  clj-surgeon.ns-isolation-test
       951 ms  clj-surgeon.splice-envelope-test
       415 ms  clj-surgeon.receipt-booleans-test
       328 ms  clj-surgeon.mcp-expect-guard-test
       201 ms  clj-surgeon.mcp-relation-census-round20-test
       168 ms  clj-surgeon.mcp-semantic-client-test

========== lane 3 (clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test) exit 0, 1564 ms ==========

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.mcp-extraction-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.mcp-schema-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 4 (clj-surgeon.mcp-inspect-tool-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-operation-test) exit 0, 20555 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-inspect-tool-test

Testing clj-surgeon.insert-forms-receipt-test

Testing clj-surgeon.scope-stream-test

Testing clj-surgeon.census-pool-test

Testing clj-surgeon.mcp-operation-test
---------- lane 4 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 4399 ms):
      2142 ms  clj-surgeon.mcp-inspect-tool-test
      1025 ms  clj-surgeon.insert-forms-receipt-test
       550 ms  clj-surgeon.scope-stream-test
       478 ms  clj-surgeon.census-pool-test
       204 ms  clj-surgeon.mcp-operation-test

========== lane 5 (clj-surgeon.mcp-intent-contract-test clj-surgeon.move-dependency-test clj-surgeon.telemetry-events-test clj-surgeon.fast-lane-isolation-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-typist-test clj-surgeon.outline-differential-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test) exit 0, 866 ms ==========

Testing clj-surgeon.mcp-intent-contract-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.telemetry-events-test

Testing clj-surgeon.fast-lane-isolation-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.mission-typist-test

Testing clj-surgeon.outline-differential-test

Testing clj-surgeon.cljc.analyze-test

Testing clj-surgeon.mcp-compact-edit-fields-test

========== lane 6 (clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test) exit 0, 23082 ms ==========
lanes: --ns -- 6 namespace(s), home-isolated true

Testing clj-surgeon.insert-forms-test

Testing clj-surgeon.rename-alias-receipt-test

Testing clj-surgeon.mcp-compact-location-test

Testing clj-surgeon.mcp-prepared-request-test

Testing clj-surgeon.mcp-prepared-confirmation-test

Testing clj-surgeon.mcp-formatter-test
---------- lane 6 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (6, slowest first, total 5123 ms):
      3372 ms  clj-surgeon.insert-forms-test
       694 ms  clj-surgeon.rename-alias-receipt-test
       399 ms  clj-surgeon.mcp-compact-location-test
       249 ms  clj-surgeon.mcp-prepared-request-test
       235 ms  clj-surgeon.mcp-prepared-confirmation-test
       174 ms  clj-surgeon.mcp-formatter-test

========== lane 7 (clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.mission-plain-forms-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test) exit 0, 599 ms ==========

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.recovery-test

Testing clj-surgeon.mission-plain-forms-test

Testing clj-surgeon.move-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.split-proof-gate-test

========== lane 8 (clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.outline-memory-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-write-refusal-test) exit 0, 22856 ms ==========
lanes: --ns -- 5 namespace(s), home-isolated true

Testing clj-surgeon.mcp-namespace-split-test

Testing clj-surgeon.mcp-change-buffer-test

Testing clj-surgeon.outline-memory-test

Testing clj-surgeon.mcp-combinable-transaction-test

Testing clj-surgeon.mcp-write-refusal-test
---------- lane 8 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (5, slowest first, total 4644 ms):
      3296 ms  clj-surgeon.mcp-namespace-split-test
       498 ms  clj-surgeon.mcp-change-buffer-test
       412 ms  clj-surgeon.outline-memory-test
       230 ms  clj-surgeon.mcp-combinable-transaction-test
       208 ms  clj-surgeon.mcp-write-refusal-test

========== lane 9 (clj-surgeon.helper-extraction-test clj-surgeon.agent-routing-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.mcp-contract-test clj-surgeon.mcp-recovery-test clj-surgeon.forms-test) exit 0, 1727 ms ==========

Testing clj-surgeon.helper-extraction-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.mcp-program-tool-test

Testing clj-surgeon.mcp-contract-test

Testing clj-surgeon.mcp-recovery-test

Testing clj-surgeon.forms-test

========== lane 10 (clj-surgeon.rename-alias-test) exit 0, 18412 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.rename-alias-test
---------- lane 10 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 4632 ms):
      4632 ms  clj-surgeon.rename-alias-test

========== lane 11 (clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 1362 ms ==========

Testing clj-surgeon.mcp-inspect-contract-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.battery-parallel-test

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.battery-parallel-hook-probe

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.mcp-telemetry-test

Testing clj-surgeon.mission-usage-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.failure-report-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.mcp-paths-test

Testing clj-surgeon.mission-git-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 12 (clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-operation-async-test) exit 0, 24636 ms ==========
lanes: --ns -- 3 namespace(s), home-isolated true

Testing clj-surgeon.mcp-operation-registry-test

Testing clj-surgeon.mcp-create-files-test

Testing clj-surgeon.mcp-operation-async-test
---------- lane 12 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (3, slowest first, total 4966 ms):
      4349 ms  clj-surgeon.mcp-operation-registry-test
       430 ms  clj-surgeon.mcp-create-files-test
       187 ms  clj-surgeon.mcp-operation-async-test

========== lane 13 (clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test) exit 0, 672 ms ==========

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.mcp-workspace-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.mission-forms-test

Testing clj-surgeon.mission-candidate-test

========== lane 14 (clj-surgeon.mcp-compact-relations-test) exit 0, 25523 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-compact-relations-test
---------- lane 14 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 8802 ms):
      8802 ms  clj-surgeon.mcp-compact-relations-test

========== lane 15 (clj-surgeon.mcp-server-test) exit 0, 10885 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-server-test
---------- lane 15 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 35507 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-18597-33d7910d/clj-surgeon-mcp-server-test-9953544578598889344/.nrepl-port )
clj-surgeon MCP: embedded nREPL on 43451 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-18597-33d7910d/clj-surgeon-mcp-server-test-13510864628326506910/.nrepl-port )

namespace walls (1, slowest first, total 266 ms):
       266 ms  clj-surgeon.mcp-server-test

========== lane 16 (clj-surgeon.mcp-hot-verify-test) exit 0, 9931 ms ==========
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
---------- lane 16 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1615 ms):
      1615 ms  clj-surgeon.mcp-hot-verify-test

========== lane 17 (clj-surgeon.namespace-split-warm-test) exit 0, 11530 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.namespace-split-warm-test
---------- lane 17 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 1567 ms):
      1567 ms  clj-surgeon.namespace-split-warm-test

========== lane 18 (clj-surgeon.mcp-http-server-test) exit 0, 16716 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-http-server-test

Testing clj-surgeon.forms-test

Ran 24 tests containing 94 assertions.
0 failures, 0 errors.
---------- lane 18 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
clj-surgeon MCP: embedded nREPL on 34291 ( /var/tmp/forge/bbtower-fx/clj-surgeon-suite-10960-c11d3784/clj-surgeon-mcp-http-test-1902270329261865392/.nrepl-port )

namespace walls (1, slowest first, total 2647 ms):
      2647 ms  clj-surgeon.mcp-http-server-test

========== lane 19 (clj-surgeon.mcp-tool-test) exit 0, 14351 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-tool-test
---------- lane 19 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 3145 ms):
      3145 ms  clj-surgeon.mcp-tool-test

========== lane 20 (clj-surgeon.outline-corpus-integration-test) exit 0, 15424 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.outline-corpus-integration-test
---------- lane 20 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 9217 ms):
      9217 ms  clj-surgeon.outline-corpus-integration-test

========== lane 21 (clj-surgeon.mcp-feature-thread-test) exit 0, 56548 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated true

Testing clj-surgeon.mcp-feature-thread-test
---------- lane 21 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 49141 ms):
     49141 ms  clj-surgeon.mcp-feature-thread-test

Ran 1420 tests containing 16487 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 94434 ms
cadence :fast: serial-equivalent 45031 ms; budget 60000 ms
cadence :integration: serial-equivalent 67598 ms; budget 240000 ms
bb-runtime: serial-equivalent 8131 ms; budget 343102 ms; makespan 21247 ms

namespace walls (101, slowest first, serial-equivalent total 112629 ms):
     49141 ms  clj-surgeon.mcp-feature-thread-test
      9217 ms  clj-surgeon.outline-corpus-integration-test
      8802 ms  clj-surgeon.mcp-compact-relations-test
      4632 ms  clj-surgeon.rename-alias-test
      4349 ms  clj-surgeon.mcp-operation-registry-test
      3372 ms  clj-surgeon.insert-forms-test
      3296 ms  clj-surgeon.mcp-namespace-split-test
      3145 ms  clj-surgeon.mcp-tool-test
      2647 ms  clj-surgeon.mcp-http-server-test
      2142 ms  clj-surgeon.mcp-inspect-tool-test
      1615 ms  clj-surgeon.mcp-hot-verify-test
      1576 ms  clj-surgeon.ns-isolation-test
      1567 ms  clj-surgeon.namespace-split-warm-test
      1407 ms  clj-surgeon.lane-manifest-test
      1393 ms  clj-surgeon.outline-test
      1025 ms  clj-surgeon.insert-forms-receipt-test
       951 ms  clj-surgeon.splice-envelope-test
       925 ms  clj-surgeon.helper-extraction-test
       835 ms  clj-surgeon.operation-algebra-test
       795 ms  clj-surgeon.namespace-split-test
       717 ms  clj-surgeon.analyze-test
       694 ms  clj-surgeon.rename-alias-receipt-test
       550 ms  clj-surgeon.scope-stream-test
       498 ms  clj-surgeon.mcp-change-buffer-test
       478 ms  clj-surgeon.census-pool-test
       430 ms  clj-surgeon.mcp-create-files-test
       415 ms  clj-surgeon.receipt-booleans-test
       412 ms  clj-surgeon.outline-memory-test
       399 ms  clj-surgeon.mcp-compact-location-test
       328 ms  clj-surgeon.mcp-expect-guard-test
       326 ms  clj-surgeon.mcp-inspect-contract-test
       286 ms  clj-surgeon.mcp-extraction-plan-test
       266 ms  clj-surgeon.mcp-server-test
       253 ms  clj-surgeon.mcp-intent-contract-test
       249 ms  clj-surgeon.mcp-prepared-request-test
       235 ms  clj-surgeon.mcp-prepared-confirmation-test
       230 ms  clj-surgeon.mcp-combinable-transaction-test
       216 ms  clj-surgeon.mission-candidate-race-test
       208 ms  clj-surgeon.mcp-write-refusal-test
       204 ms  clj-surgeon.mcp-operation-test
       201 ms  clj-surgeon.mcp-relation-census-round20-test
       193 ms  clj-surgeon.mcp-compact-edit-test
       187 ms  clj-surgeon.mcp-operation-async-test
       175 ms  clj-surgeon.alias-migration-test
       174 ms  clj-surgeon.mcp-formatter-test
       168 ms  clj-surgeon.mcp-semantic-client-test
       136 ms  clj-surgeon.workspace-onboarding-test
       120 ms  clj-surgeon.cljc-existing-ops-test
       113 ms  clj-surgeon.ls-tree-test
        77 ms  clj-surgeon.jvm-error-test
        73 ms  clj-surgeon.move-dependency-test
        65 ms  clj-surgeon.require-change-test
        52 ms  clj-surgeon.outermost-test
        48 ms  clj-surgeon.telemetry-events-test
        46 ms  clj-surgeon.battery-parallel-test
        45 ms  clj-surgeon.recovery-test
        42 ms  clj-surgeon.fast-lane-isolation-test
        38 ms  clj-surgeon.agent-routing-test
        37 ms  clj-surgeon.fix-declares-test
        37 ms  clj-surgeon.relation-census-test
        36 ms  clj-surgeon.mission-plain-forms-test
        35 ms  clj-surgeon.mcp-extraction-test
        34 ms  clj-surgeon.mcp-workspace-test
        34 ms  clj-surgeon.structural-lens-test
        25 ms  clj-surgeon.owner-hypotheses-test
        23 ms  clj-surgeon.worktree-lifecycle-io-test
        19 ms  clj-surgeon.move-test
        18 ms  clj-surgeon.memory-battery-test
        14 ms  clj-surgeon.edit-dsl-test
        14 ms  clj-surgeon.mcp-program-tool-test
        11 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.insertion-gap-test
        10 ms  clj-surgeon.mcp-contract-test
        10 ms  clj-surgeon.mission-forms-source-test
        10 ms  clj-surgeon.quoted-var-refs-test
         9 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.split-proof-gate-test
         7 ms  clj-surgeon.mission-typist-test
         5 ms  clj-surgeon.forms-test
         5 ms  clj-surgeon.outline-differential-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.mcp-schema-test
         3 ms  clj-surgeon.cljc.merge-test
         3 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.extract-header-test
         3 ms  clj-surgeon.mcp-recovery-test
         3 ms  clj-surgeon.mcp-telemetry-test
         2 ms  clj-surgeon.battery-ledger-test
         2 ms  clj-surgeon.cljc.analyze-test
         2 ms  clj-surgeon.mcp-read-request-normalization-test
         2 ms  clj-surgeon.mission-forms-test
         2 ms  clj-surgeon.mission-usage-test
         2 ms  clj-surgeon.syntax-var-refs-test
         1 ms  clj-surgeon.mcp-compact-edit-fields-test
         1 ms  clj-surgeon.mcp-paths-test
         1 ms  clj-surgeon.mission-candidate-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.failure-report-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.mission-git-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (22), makespan 94434 ms:
  lane 0      5357 ms  exit 0  clj-surgeon.lane-manifest-test clj-surgeon.outline-test clj-surgeon.namespace-split-test clj-surgeon.analyze-test clj-surgeon.workspace-onboarding-test clj-surgeon.require-change-test clj-surgeon.edit-dsl-test clj-surgeon.mission-forms-source-test clj-surgeon.cljc.require-ops-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-read-request-normalization-test
  lane 1     13830 ms  exit 0  clj-surgeon.mission-candidate-race-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-compact-edit-test
  lane 2     24947 ms  exit 0  clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-expect-guard-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.mcp-semantic-client-test
  lane 3      1564 ms  exit 0  clj-surgeon.operation-algebra-test clj-surgeon.jvm-error-test clj-surgeon.mcp-extraction-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.split-test clj-surgeon.cljc.merge-test clj-surgeon.mcp-schema-test clj-surgeon.syntax-var-refs-test
  lane 4     20555 ms  exit 0  clj-surgeon.mcp-inspect-tool-test clj-surgeon.insert-forms-receipt-test clj-surgeon.scope-stream-test clj-surgeon.census-pool-test clj-surgeon.mcp-operation-test
  lane 5       866 ms  exit 0  clj-surgeon.mcp-intent-contract-test clj-surgeon.move-dependency-test clj-surgeon.telemetry-events-test clj-surgeon.fast-lane-isolation-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.mission-typist-test clj-surgeon.outline-differential-test clj-surgeon.cljc.analyze-test clj-surgeon.mcp-compact-edit-fields-test
  lane 6     23082 ms  exit 0  clj-surgeon.insert-forms-test clj-surgeon.rename-alias-receipt-test clj-surgeon.mcp-compact-location-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test
  lane 7       599 ms  exit 0  clj-surgeon.cljc-existing-ops-test clj-surgeon.recovery-test clj-surgeon.mission-plain-forms-test clj-surgeon.move-test clj-surgeon.rename-test clj-surgeon.split-proof-gate-test
  lane 8     22856 ms  exit 0  clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-change-buffer-test clj-surgeon.outline-memory-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mcp-write-refusal-test
  lane 9      1727 ms  exit 0  clj-surgeon.helper-extraction-test clj-surgeon.agent-routing-test clj-surgeon.relation-census-test clj-surgeon.owner-hypotheses-test clj-surgeon.mcp-program-tool-test clj-surgeon.mcp-contract-test clj-surgeon.mcp-recovery-test clj-surgeon.forms-test
  lane 10     18412 ms  exit 0  clj-surgeon.rename-alias-test
  lane 11      1362 ms  exit 0  clj-surgeon.mcp-inspect-contract-test clj-surgeon.alias-migration-test clj-surgeon.fix-declares-test clj-surgeon.structural-lens-test clj-surgeon.battery-parallel-test clj-surgeon.memory-battery-test clj-surgeon.worktree-lifecycle-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-usage-test clj-surgeon.diagnostic-delta-test clj-surgeon.failure-report-test clj-surgeon.file-ops-test clj-surgeon.mcp-paths-test clj-surgeon.mission-git-test clj-surgeon.worktree-lifecycle-cli-test
  lane 12     24636 ms  exit 0  clj-surgeon.mcp-operation-registry-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-operation-async-test
  lane 13       672 ms  exit 0  clj-surgeon.ls-tree-test clj-surgeon.mcp-workspace-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.extract-header-test clj-surgeon.mission-forms-test clj-surgeon.mission-candidate-test
  lane 14     25523 ms  exit 0  clj-surgeon.mcp-compact-relations-test
  lane 15     10885 ms  exit 0  clj-surgeon.mcp-server-test
  lane 16      9931 ms  exit 0  clj-surgeon.mcp-hot-verify-test
  lane 17     11530 ms  exit 0  clj-surgeon.namespace-split-warm-test
  lane 18     16716 ms  exit 0  clj-surgeon.mcp-http-server-test
  lane 19     14351 ms  exit 0  clj-surgeon.mcp-tool-test
  lane 20     15424 ms  exit 0  clj-surgeon.outline-corpus-integration-test
  lane 21     56548 ms  exit 0  clj-surgeon.mcp-feature-thread-test

TEST-ISOLATION: 4 violation(s) -- the suite's own purity rules, per namespace:
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-inspect-tool-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt24/restricted-summary.edn was created in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.mcp-inspect-tool-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt24/source.patch was modified in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.rename-alias-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt24/restricted-summary.edn was created in the repository working tree
   TEST-ISO-003 VIOLATION in clj-surgeon.rename-alias-test -- working tree: docs/observations/2026-09-12-bbtower-block-b/attempt24/source.patch was modified in the repository working tree
battery-parallel: makespan 94434 ms over 8 lane(s); serial-equivalent 112629 ms; skipped 0

========== lane 0 (clj-surgeon.intent-transaction-test) exit 0, 14717 ms ==========
lanes: --ns -- 1 namespace(s), home-isolated false

Testing clj-surgeon.intent-transaction-test
---------- lane 0 stderr ----------
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx

namespace walls (1, slowest first, total 6076 ms):
      6076 ms  clj-surgeon.intent-transaction-test

========== lane 1 (clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test) exit 0, 7220 ms ==========

Testing clj-surgeon.extract-test

Testing clj-surgeon.xray-test

Testing clj-surgeon.partition-all-test

========== lane 2 (clj-surgeon.analyze-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.forms-test) exit 0, 1472 ms ==========

Testing clj-surgeon.analyze-test

Testing clj-surgeon.ls-tree-test

Testing clj-surgeon.move-dependency-test

Testing clj-surgeon.agent-routing-test

Testing clj-surgeon.outermost-test

Testing clj-surgeon.quoted-var-refs-test

Testing clj-surgeon.forms-test

========== lane 3 (clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test) exit 0, 14455 ms ==========

Testing clj-surgeon.edit-test

Testing clj-surgeon.core-discovery-test

Testing clj-surgeon.lens-query-test

Testing clj-surgeon.edn-config-integration-test
---------- lane 3 stderr ----------
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180126-0af633ea/andon-shell-safety10763440115312756994/H; touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180126-0af633ea/andon-shell-safety10763440115312756994/PWNED-SEMICOLON ; echo z"
clj-surgeon: skipping project discovery; not an existing directory: "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180126-0af633ea/andon-shell-safety2431348067097354304/H$(touch /var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180126-0af633ea/andon-shell-safety2431348067097354304/PWNED-SUBST)"

========== lane 4 (clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test) exit 0, 486 ms ==========

Testing clj-surgeon.recovery-test

Testing clj-surgeon.relation-census-test

Testing clj-surgeon.move-test

Testing clj-surgeon.insertion-gap-test

Testing clj-surgeon.cljc.merge-test

Testing clj-surgeon.failure-report-test

========== lane 5 (clj-surgeon.help-test clj-surgeon.platform-selector-test) exit 0, 11988 ms ==========

Testing clj-surgeon.help-test

Testing clj-surgeon.platform-selector-test

========== lane 6 (clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test) exit 0, 2576 ms ==========

Testing clj-surgeon.outline-test

Testing clj-surgeon.operation-algebra-test

Testing clj-surgeon.alias-migration-test

Testing clj-surgeon.jvm-error-test

Testing clj-surgeon.fix-declares-test

Testing clj-surgeon.worktree-lifecycle-io-test

Testing clj-surgeon.cljc.require-ops-test

Testing clj-surgeon.cljc.analyze-test

========== lane 7 (clj-surgeon.show-form-test) exit 0, 14185 ms ==========

Testing clj-surgeon.show-form-test

========== lane 8 (clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test) exit 0, 828 ms ==========

Testing clj-surgeon.workspace-onboarding-test

Testing clj-surgeon.cljc-existing-ops-test

Testing clj-surgeon.structural-lens-test

Testing clj-surgeon.owner-hypotheses-test

Testing clj-surgeon.cljc.split-test

Testing clj-surgeon.worktree-lifecycle-test

Testing clj-surgeon.syntax-var-refs-test

========== lane 9 (clj-surgeon.tmp-leak-support-test) exit 0, 14376 ms ==========

Testing clj-surgeon.tmp-leak-support-test
---------- lane 9 stderr ----------
tmp-refused: refusing to delete /var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180128-93e2047b/tmp-leak-sweep-guard-7529177887526125180/other-seat-precious-fixture -- it is not a private per-run root (its name must start with clj-surgeon-suite-). Sweeping a shared base would destroy another tenant's working set.

========== lane 10 (clj-surgeon.memory-battery-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test) exit 0, 351 ms ==========

Testing clj-surgeon.memory-battery-test

Testing clj-surgeon.edit-dsl-test

Testing clj-surgeon.rename-test

Testing clj-surgeon.extract-header-test

Testing clj-surgeon.diagnostic-delta-test

Testing clj-surgeon.file-ops-test

Testing clj-surgeon.worktree-lifecycle-cli-test

========== lane 11 (clj-surgeon.cli-dispatch-test) exit 0, 14508 ms ==========

Testing clj-surgeon.cli-dispatch-test

========== lane 12 (clj-surgeon.parser-admission-test) exit 0, 73329 ms ==========

Testing clj-surgeon.parser-admission-test

========== lane 13 (clj-surgeon.install-test) exit 0, 92619 ms ==========

Testing clj-surgeon.install-test
Installed stable CLI /var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/bin with spaces/clj-surgeon from commit c53fd9c59b44e3e72b4c9fd9ee4203d7018de01d, source hash e459f435fbe5ef552a037bb536f6c1fb23324a8ff6d12d80f40548b048632e1b
Receipt: /var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/bin with spaces/clj-surgeon.receipt.edn
 
:insert-forms! {:proc #object[java.lang.ProcessImpl 0x5c7d0904 "Process[pid=4193261, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x2771ffb5 "java.lang.ProcessImpl$ProcessPipeOutputStream@2771ffb5"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.26131, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"insert_forms\", :remedy \"Follow the closed request schema at [].\"}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/bin with spaces/clj-surgeon" ":op" ":insert-forms!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/empty.edn"]}
:rename-alias! {:proc #object[java.lang.ProcessImpl 0x6655937f "Process[pid=4193404, exitValue=2]"], :exit 2, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x5625b27a "java.lang.ProcessImpl$ProcessPipeOutputStream@5625b27a"], :out "{:state \"refused\", :committed false, :mutation_attempted false, :source_unchanged true, :at [], :elapsed_ms 2.362221, :error \"Closed map has missing or unsupported fields.\", :error-type :invalid-request, :next_action \"revise-request\", :ok false, :operation \"rename_alias\", :remedy \"Follow the closed request schema at [].\", :version 1}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/bin with spaces/clj-surgeon" ":op" ":rename-alias!" ":request-file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/empty.edn"]}
:ls {:proc #object[java.lang.ProcessImpl 0x53ee66e5 "Process[pid=4193572, exitValue=0]"], :exit 0, :in #object[java.lang.ProcessImpl$ProcessPipeOutputStream 0x701dcc49 "java.lang.ProcessImpl$ProcessPipeOutputStream@701dcc49"], :out "{:ns sample,\n :file\n \"/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/sample.clj\",\n :lines 2,\n :form-count 1,\n :forms\n [{:type defn,\n   :platforms [:clj],\n   :line 2,\n   :end-line 2,\n   :name answer,\n   :args \"[]\"}],\n :requires [],\n :forward-refs []}\n", :err "", :prev nil, :cmd ["/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/bin with spaces/clj-surgeon" ":op" ":ls" ":file" "/var/tmp/forge/bbtower-fx/clj-surgeon-suite-4180117-21bf9eec/clj-surgeon-installed-splice-8587448158312869659/sample.clj"]}

Ran 902 tests containing 8029 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
makespan: 92619 ms
cadence :battery: serial-equivalent 245861 ms; budget 1800000 ms
cadence :fast: serial-equivalent 3565 ms; budget 60000 ms
bb-runtime: serial-equivalent 243350 ms; budget 343102 ms; makespan 92619 ms

namespace walls (50, slowest first, serial-equivalent total 249426 ms):
     92450 ms  clj-surgeon.install-test
     73055 ms  clj-surgeon.parser-admission-test
     14205 ms  clj-surgeon.tmp-leak-support-test
     14075 ms  clj-surgeon.cli-dispatch-test
     13713 ms  clj-surgeon.show-form-test
     11269 ms  clj-surgeon.help-test
      6270 ms  clj-surgeon.edit-test
      6076 ms  clj-surgeon.intent-transaction-test
      4885 ms  clj-surgeon.extract-test
      4726 ms  clj-surgeon.core-discovery-test
      2381 ms  clj-surgeon.lens-query-test
      1531 ms  clj-surgeon.xray-test
       899 ms  clj-surgeon.outline-test
       807 ms  clj-surgeon.operation-algebra-test
       705 ms  clj-surgeon.analyze-test
       591 ms  clj-surgeon.edn-config-integration-test
       356 ms  clj-surgeon.partition-all-test
       278 ms  clj-surgeon.platform-selector-test
       172 ms  clj-surgeon.alias-migration-test
       144 ms  clj-surgeon.ls-tree-test
       133 ms  clj-surgeon.cljc-existing-ops-test
       116 ms  clj-surgeon.workspace-onboarding-test
        75 ms  clj-surgeon.move-dependency-test
        74 ms  clj-surgeon.jvm-error-test
        64 ms  clj-surgeon.relation-census-test
        51 ms  clj-surgeon.recovery-test
        48 ms  clj-surgeon.agent-routing-test
        38 ms  clj-surgeon.fix-declares-test
        38 ms  clj-surgeon.structural-lens-test
        33 ms  clj-surgeon.owner-hypotheses-test
        26 ms  clj-surgeon.worktree-lifecycle-io-test
        22 ms  clj-surgeon.memory-battery-test
        21 ms  clj-surgeon.outermost-test
        18 ms  clj-surgeon.edit-dsl-test
        16 ms  clj-surgeon.move-test
        11 ms  clj-surgeon.cljc.split-test
        10 ms  clj-surgeon.insertion-gap-test
        10 ms  clj-surgeon.worktree-lifecycle-test
         8 ms  clj-surgeon.quoted-var-refs-test
         5 ms  clj-surgeon.rename-test
         4 ms  clj-surgeon.extract-header-test
         4 ms  clj-surgeon.cljc.merge-test
         4 ms  clj-surgeon.cljc.require-ops-test
         3 ms  clj-surgeon.forms-test
         2 ms  clj-surgeon.failure-report-test
         2 ms  clj-surgeon.syntax-var-refs-test
         2 ms  clj-surgeon.cljc.analyze-test
         0 ms  clj-surgeon.diagnostic-delta-test
         0 ms  clj-surgeon.file-ops-test
         0 ms  clj-surgeon.worktree-lifecycle-cli-test

lanes (14), makespan 92619 ms:
  lane 0     14717 ms  exit 0  clj-surgeon.intent-transaction-test
  lane 1      7220 ms  exit 0  clj-surgeon.extract-test clj-surgeon.xray-test clj-surgeon.partition-all-test
  lane 2      1472 ms  exit 0  clj-surgeon.analyze-test clj-surgeon.ls-tree-test clj-surgeon.move-dependency-test clj-surgeon.agent-routing-test clj-surgeon.outermost-test clj-surgeon.quoted-var-refs-test clj-surgeon.forms-test
  lane 3     14455 ms  exit 0  clj-surgeon.edit-test clj-surgeon.core-discovery-test clj-surgeon.lens-query-test clj-surgeon.edn-config-integration-test
  lane 4       486 ms  exit 0  clj-surgeon.recovery-test clj-surgeon.relation-census-test clj-surgeon.move-test clj-surgeon.insertion-gap-test clj-surgeon.cljc.merge-test clj-surgeon.failure-report-test
  lane 5     11988 ms  exit 0  clj-surgeon.help-test clj-surgeon.platform-selector-test
  lane 6      2576 ms  exit 0  clj-surgeon.outline-test clj-surgeon.operation-algebra-test clj-surgeon.alias-migration-test clj-surgeon.jvm-error-test clj-surgeon.fix-declares-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.cljc.require-ops-test clj-surgeon.cljc.analyze-test
  lane 7     14185 ms  exit 0  clj-surgeon.show-form-test
  lane 8       828 ms  exit 0  clj-surgeon.workspace-onboarding-test clj-surgeon.cljc-existing-ops-test clj-surgeon.structural-lens-test clj-surgeon.owner-hypotheses-test clj-surgeon.cljc.split-test clj-surgeon.worktree-lifecycle-test clj-surgeon.syntax-var-refs-test
  lane 9     14376 ms  exit 0  clj-surgeon.tmp-leak-support-test
  lane 10       351 ms  exit 0  clj-surgeon.memory-battery-test clj-surgeon.edit-dsl-test clj-surgeon.rename-test clj-surgeon.extract-header-test clj-surgeon.diagnostic-delta-test clj-surgeon.file-ops-test clj-surgeon.worktree-lifecycle-cli-test
  lane 11     14508 ms  exit 0  clj-surgeon.cli-dispatch-test
  lane 12     73329 ms  exit 0  clj-surgeon.parser-admission-test
  lane 13     92619 ms  exit 0  clj-surgeon.install-test
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 92619 ms over 8 lane(s); serial-equivalent 249426 ms; skipped 0
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
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-escape (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/seamdisk has an UNDETERMINABLE filesystem type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM. Refusing rather than assuming disk. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- seam-tmpfs (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/seamdisk is RAM-backed (tmpfs, fstype=tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- fallback-alive (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-26451-7814a607 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-26451-7814a607 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-26451-7814a607
PROBE leak-exit=0
--- sentinel-decoy (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base was handed a re-exec sentinel it does not own: it names root="1" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- sentinel-mismatch (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/some-other-root" but this process's java.io.tmpdir is "/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/decoy-base". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
--- heap-args (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx node-cache=0 args=["alpha" "beta" "--node-cache-env"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-28919-f269752e node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=318 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-28919-f269752e node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-28919-f269752e
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- bb-args (exit=0) ---
PROBE role=parent max-mb=25061 tmpdir=/tmp node-cache=<unset> args=["alpha" "beta" "--node-cache-env"]
PROBE role=child-pre max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-30222-cbd56c9b node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE role=child max-mb=25061 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-30222-cbd56c9b node-cache=1 args=["alpha" "beta" "--node-cache-env"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/realdisk/clj-surgeon-suite-30222-cbd56c9b
PROBE descendant-node-cache=1
PROBE leak-exit=0
--- subproc (exit=1) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=["--leak-subprocess"]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/subbase/clj-surgeon-suite-30332-77698950 node-cache=1 args=["--leak-subprocess"]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/subbase/clj-surgeon-suite-30332-77698950 node-cache=1 args=["--leak-subprocess"]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/subbase/clj-surgeon-suite-30332-77698950
PROBE subprocess-tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/subbase/clj-surgeon-suite-30332-77698950/tmp.nt3OMgD2YE
temp-leak: 1 entries left under /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/subbase/clj-surgeon-suite-30332-77698950: tmp.nt3OMgD2YE
PROBE leak-exit=1
--- kill-term (left in base) ---
--- stale-sweep (exit=0) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=child-pre max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/stalebase/clj-surgeon-suite-32235-d91d0aa2 node-cache=1 args=[]
PROBE role=child max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/stalebase/clj-surgeon-suite-32235-d91d0aa2 node-cache=1 args=[]
PROBE root=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/stalebase/clj-surgeon-suite-32235-d91d0aa2
PROBE leak-exit=0
--- stalebase after ---
clj-surgeon-suite-22034-aaaaaaaa
other-seat-precious-fixture
--- unwritable (exit=97) ---
Picked up JAVA_TOOL_OPTIONS: -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx
PROBE role=parent max-mb=1024 tmpdir=/var/tmp/forge/bbtower-fx node-cache=<unset> args=[]
tmp-refused: java.io.tmpdir base=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/nowrite cannot be used as a temp base: AccessDeniedException: /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf/nowrite/clj-surgeon-suite-32498-b68cf28f Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
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
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf -> /var/tmp/forge/bbtower-fx/clj-surgeon-tmpleak-witness.NgqKaf ---
--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
tmp-leak ratchet witness passed
Installed analyzer gate /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.USes4M/home/bin/clj-kondo-admission and shell entrance /var/tmp/forge/bbtower-fx/clj-surgeon-kondo-path.USes4M/home/bin/clj-kondo
clj-kondo admission path regression passed
cclsp already ready at http://127.0.0.1:7890/mcp
cclsp-start transient-health regression passed
cclsp ready at http://127.0.0.1:7890/mcp with restart-on-save TypeScript
cclsp launch PATH regression passed
direct cclsp client audit self-test: ok
.......s...s.........
----------------------------------------------------------------------
Ran 21 tests in 0.399s

OK (skipped=2)
.
----------------------------------------------------------------------
Ran 1 test in 0.000s

OK
....
----------------------------------------------------------------------
Ran 4 testgate-refused: gate-refused: runtime pool failed {:shell-exit 0, :suites [{:suite "alias", :state :passed, :problems []} {:suite "mcp", :state :failed, :problems []} {:suite "bb", :state :passed, :problems []}]}
make: *** [Makefile:1262: landing-gate-prewarm] Error 1
EXIT=2 WALL_S=172
