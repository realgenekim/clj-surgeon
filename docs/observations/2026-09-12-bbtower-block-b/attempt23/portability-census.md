# Census fold

Frozen attempt22 controls plus fresh attempt23 controls for merge, split and prune.
Initial-load exclusions are separately accounted; they have no complete JVM controls in this census.

| Namespace | Assignment | State | Reasons |
|---|---|---|---|
| clj-surgeon.admit-patch-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.agent-routing-test | :bb | :portable | nil  |
| clj-surgeon.alias-migration-test | :bb | :portable | nil  |
| clj-surgeon.analyze-test | :bb | :portable | nil  |
| clj-surgeon.analyzer-contract-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.battery-ledger-test | :bb | :portable | nil  |
| clj-surgeon.battery-parallel-test | :bb | :portable | nil  |
| clj-surgeon.cell-b-oracle-test | :bb | :portable | nil  |
| clj-surgeon.census-pool-test | :jvm | :bb-load-excluded | nil Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| clj-surgeon.cli-dispatch-test | :bb | :portable | nil  |
| clj-surgeon.cljc-existing-ops-test | :bb | :portable | nil  |
| clj-surgeon.cljc.analyze-test | :bb | :portable | nil  |
| clj-surgeon.cljc.merge-test | :bb | :portable | nil  |
| clj-surgeon.cljc.require-ops-test | :bb | :portable | nil  |
| clj-surgeon.cljc.split-test | :bb | :portable | nil  |
| clj-surgeon.core-discovery-test | :bb | :portable | nil  |
| clj-surgeon.diagnostic-delta-test | :bb | :portable | nil  |
| clj-surgeon.edit-dsl-test | :bb | :portable | nil  |
| clj-surgeon.edit-test | :bb | :portable | nil  |
| clj-surgeon.edn-config-integration-test | :bb | :portable | nil  |
| clj-surgeon.extract-header-test | :bb | :portable | nil  |
| clj-surgeon.extract-test | :bb | :portable | nil  |
| clj-surgeon.failure-report-test | :bb | :portable | nil  |
| clj-surgeon.fast-lane-isolation-test | :bb | :portable | nil  |
| clj-surgeon.file-ops-test | :bb | :portable | nil  |
| clj-surgeon.fix-declares-test | :bb | :portable | nil  |
| clj-surgeon.forms-test | :bb | :portable | nil  |
| clj-surgeon.help-test | :bb | :portable | nil  |
| clj-surgeon.helper-extraction-test | :bb | :portable | nil  |
| clj-surgeon.insert-forms-parity-test | :bb | :portable | nil  |
| clj-surgeon.insert-forms-receipt-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.insert-forms-test | :jvm | :portable | nil  |
| clj-surgeon.insertion-gap-test | :bb | :portable | nil  |
| clj-surgeon.install-test | :bb | :portable | nil  |
| clj-surgeon.intent-transaction-test | :jvm | :portable | nil  |
| clj-surgeon.jvm-error-test | :bb | :portable | nil  |
| clj-surgeon.lane-manifest-test | :bb | :portable | nil  |
| clj-surgeon.lens-query-test | :bb | :portable | nil  |
| clj-surgeon.ls-tree-test | :bb | :portable | nil  |
| clj-surgeon.mcp-alias-migration-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-change-buffer-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-cold-verify-test | :jvm | :bb-ineligible | #{:sci-host-interop} SCI refuses FileLockImpl.close in admission-timeout witness |
| clj-surgeon.mcp-combinable-transaction-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-compact-edit-fields-test | :bb | :portable | nil  |
| clj-surgeon.mcp-compact-edit-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-compact-location-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-compact-relations-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-contract-test | :bb | :portable | nil  |
| clj-surgeon.mcp-create-files-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-expect-guard-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-extraction-plan-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-extraction-test | :bb | :portable | nil  |
| clj-surgeon.mcp-feature-thread-sed-test | :bb | :portable | nil  |
| clj-surgeon.mcp-feature-thread-test | :jvm | :portable | nil  |
| clj-surgeon.mcp-formatter-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-helper-extraction-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-hot-verify-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-http-server-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-inspect-cold-job-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-inspect-contract-test | :bb | :portable | nil  |
| clj-surgeon.mcp-inspect-tool-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-intent-contract-test | :bb | :portable | nil  |
| clj-surgeon.mcp-namespace-split-test | :jvm | :bb-ineligible | #{:bb-classpath-missing/nrepl.core} Late helper-extraction requiring-resolve needs absent nrepl.core |
| clj-surgeon.mcp-operation-async-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-operation-registry-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| clj-surgeon.mcp-operation-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: java.util.Locale$Category |
| clj-surgeon.mcp-paths-test | :bb | :portable | nil  |
| clj-surgeon.mcp-prepared-confirmation-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-prepared-request-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-prepared-wire-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| clj-surgeon.mcp-process-test | :bb | :portable | nil  |
| clj-surgeon.mcp-program-tool-test | :bb | :portable | nil  |
| clj-surgeon.mcp-read-request-normalization-test | :bb | :portable | nil  |
| clj-surgeon.mcp-recovery-test | :bb | :portable | nil  |
| clj-surgeon.mcp-relation-census-launcher-test | :jvm | :bb-ineligible | #{:bb-hosted-jvm-launcher :native-image-reflection} java.class.path lacks JVM test dependencies; native image refuses StackOverflowError constructor |
| clj-surgeon.mcp-relation-census-round20-test | :jvm | :bb-load-excluded | nil Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| clj-surgeon.mcp-relation-census-test | :jvm | :bb-load-excluded | nil Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| clj-surgeon.mcp-schema-test | :bb | :portable | nil  |
| clj-surgeon.mcp-semantic-client-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: io.modelcontextprotocol.client.McpClient |
| clj-surgeon.mcp-server-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-telemetry-test | :bb | :portable | nil  |
| clj-surgeon.mcp-tool-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mcp-workspace-test | :bb | :portable | nil  |
| clj-surgeon.mcp-write-refusal-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.memory-battery-test | :bb | :portable | nil  |
| clj-surgeon.memory.journal-green-test | :jvm | :bb-ineligible | #{:bb-hosted-jvm-launcher} Child Java resolves to nonexistent bin/java under bb |
| clj-surgeon.memory.oom-reproduction-test | :jvm | :bb-ineligible | #{:bb-hosted-jvm-launcher} Child Java resolves to nonexistent bin/java under bb |
| clj-surgeon.mission-candidate-race-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: java.util.concurrent.ExecutorCompletionService |
| clj-surgeon.mission-candidate-test | :bb | :portable | nil  |
| clj-surgeon.mission-commit-cli-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-display-test | :jvm | :bb-ineligible | #{:bb-classpath-missing/nrepl.core} Late mission CLI resolution needs absent nrepl.core |
| clj-surgeon.mission-events-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-fallback-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-forms-source-test | :bb | :portable | nil  |
| clj-surgeon.mission-forms-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-boundary-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-fence-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-identity-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-ledger-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-git-process-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-submodule-test | :bb | :portable | nil  |
| clj-surgeon.mission-git-test | :bb | :portable | nil  |
| clj-surgeon.mission-phase-events-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-plain-forms-test | :bb | :portable | nil  |
| clj-surgeon.mission-provider-fallback-events-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: java.util.concurrent.ExecutorCompletionService |
| clj-surgeon.mission-publication-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-run-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-typist-executor-admission-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-typist-executor-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-typist-test | :bb | :portable | nil  |
| clj-surgeon.mission-usage-executor-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.mission-usage-test | :bb | :portable | nil  |
| clj-surgeon.move-dependency-test | :bb | :portable | nil  |
| clj-surgeon.move-test | :bb | :portable | nil  |
| clj-surgeon.namespace-split-test | :bb | :portable | nil  |
| clj-surgeon.namespace-split-warm-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/server.bb, nrepl/server.clj or nrepl/server.cljc on classpath. |
| clj-surgeon.ns-isolation-test | :jvm | :bb-ineligible | #{:native-image-reflection} Native image cannot invoke sci.lang.Var.getRawRoot |
| clj-surgeon.operation-algebra-test | :bb | :portable | nil  |
| clj-surgeon.outermost-test | :bb | :portable | nil  |
| clj-surgeon.outline-corpus-integration-test | :jvm | :portable | nil  |
| clj-surgeon.outline-differential-test | :bb | :portable | nil  |
| clj-surgeon.outline-memory-test | :jvm | :bb-load-excluded | nil Unable to resolve symbol: java.lang.management.ManagementFactory/getThreadMXBean |
| clj-surgeon.outline-test | :bb | :portable | nil  |
| clj-surgeon.owner-hypotheses-test | :bb | :portable | nil  |
| clj-surgeon.parser-admission-test | :bb | :portable | nil  |
| clj-surgeon.partition-all-test | :bb | :portable | nil  |
| clj-surgeon.platform-selector-test | :bb | :portable | nil  |
| clj-surgeon.quoted-var-refs-test | :bb | :portable | nil  |
| clj-surgeon.reader-eval-fence-test | :jvm | :bb-ineligible | #{:bb-hosted-jvm-launcher} bb-hosted JVM launcher throws ClassNotFoundException: clojure.main |
| clj-surgeon.receipt-artifacts-boundary-test | :jvm | :bb-load-excluded | nil Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| clj-surgeon.receipt-booleans-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| clj-surgeon.recovery-test | :bb | :portable | nil  |
| clj-surgeon.relation-census-test | :bb | :portable | nil  |
| clj-surgeon.rename-alias-parity-test | :bb | :portable | nil  |
| clj-surgeon.rename-alias-performance-test | :bb | :portable | nil  |
| clj-surgeon.rename-alias-receipt-test | :jvm | :portable | nil  |
| clj-surgeon.rename-alias-test | :jvm | :portable | nil  |
| clj-surgeon.rename-test | :bb | :portable | nil  |
| clj-surgeon.repository-hygiene-test | :bb | :portable | nil  |
| clj-surgeon.require-change-boundary-test | :bb | :portable | nil  |
| clj-surgeon.require-change-test | :bb | :portable | nil  |
| clj-surgeon.scope-stream-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: java.nio.file.SimpleFileVisitor |
| clj-surgeon.show-form-test | :bb | :portable | nil  |
| clj-surgeon.splice-envelope-test | :jvm | :portable | nil  |
| clj-surgeon.split-proof-gate-boundary-test | :bb | :portable | nil  |
| clj-surgeon.split-proof-gate-test | :bb | :portable | nil  |
| clj-surgeon.structural-lens-test | :bb | :portable | nil  |
| clj-surgeon.syntax-var-refs-test | :bb | :portable | nil  |
| clj-surgeon.telemetry-events-test | :bb | :portable | nil  |
| clj-surgeon.tmp-leak-support-test | :bb | :portable | nil  |
| clj-surgeon.txn-journal-test | :jvm | :bb-load-excluded | nil Unable to resolve classname: java.nio.file.SimpleFileVisitor |
| clj-surgeon.workspace-onboarding-test | :bb | :portable | nil  |
| clj-surgeon.worktree-lifecycle-cli-test | :bb | :portable | nil  |
| clj-surgeon.worktree-lifecycle-io-test | :bb | :portable | nil  |
| clj-surgeon.worktree-lifecycle-prune-test | :jvm | :bb-ineligible | #{:sci-host-interop} After TMPDIR and witnessed Git admission repairs, SCI refuses shared FileLockImpl.release; prune replay retains its lock |
| clj-surgeon.worktree-lifecycle-recovery-test | :jvm | :bb-ineligible | #{:sci-host-interop} SCI refuses FileLockImpl.release; replay retains the lock and reports lifecycle-target-locked |
| clj-surgeon.worktree-lifecycle-test | :bb | :portable | nil  |
| clj-surgeon.xray-test | :bb | :portable | nil  |

{:portable 99, :bb-ineligible 10, :refused 0, :bb-load-excluded 50}
