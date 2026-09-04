# Suite spike round one — per-namespace classification (101 test namespaces)

Companion to `2026-09-04-suite-spike-round1.md`. Produced by `bb dev/experiments/suite_classify.clj`
(static scan, file:line evidence) joined to the solo timing run. Tags: SPAWN = launches a process,
TMP = temp filesystem, PORT = binds/holds a port, SHARED = target//$HOME/.cpcache, GLOBAL =
alter-var-root/with-redefs/defonce/System-setProperty, SLEEP = Thread-sleep or a poll loop.

A STATIC TAG IS A SPELLING, NOT A BEHAVIOUR. The parent document adjudicates each tag against the
runtime evidence; several collapse (every PORT hit in the JVM lane is either `:port 0` or string
data; no namespace touches the repository's own target/; 18 of 20 GLOBAL hits are thread-local
with-redefs). Read the adjudication there, not the raw tags here.


### JVM lane — `clojure -M:clj-surgeon/mcp-test` (49 namespaces)

| namespace | wall s | tags | first evidence (file:line) |
|---|---:|---|---|
| `clj-surgeon.reader-eval-fence-test` | 464.9 | SPAWN TMP | `babashka.process` reader_eval_fence_test.clj:56; `create-temp-dir` reader_eval_fence_test.clj:97 |
| `clj-surgeon.mcp-relation-census-launcher-test` | 63.7 | SPAWN TMP | `babashka.process` mcp_relation_census_launcher_test.clj:22; `create-temp-dir` mcp_relation_census_launcher_test.clj:353 |
| `clj-surgeon.mcp-alias-migration-test` | 59.9 | SLEEP GLOBAL SHARED SPAWN TMP | `Thread/sleep` mcp_alias_migration_test.clj:6666; `with-redefs` mcp_alias_migration_test.clj:3132; `target/` mcp_alias_migration_test.clj:2755 |
| `clj-surgeon.mcp-relation-census-test` | 36.2 | GLOBAL SPAWN TMP | `with-redefs` mcp_relation_census_test.clj:372; `babashka.process` mcp_relation_census_test.clj:9; `java.io.tmpdir` mcp_relation_census_test.clj:3881 |
| `clj-surgeon.mcp-prepared-wire-test` | 18.6 | SLEEP SPAWN TMP | `Thread/sleep` mcp_prepared_wire_test.clj:56; `ProcessBuilder` mcp_prepared_wire_test.clj:32; `create-temp-dir` mcp_prepared_wire_test.clj:21 |
| `clj-surgeon.txn-journal-test` | 17.0 | SLEEP GLOBAL SPAWN TMP | `Thread/sleep` txn_journal_test.clj:1058; `with-redefs` txn_journal_test.clj:1250; `ProcessBuilder` txn_journal_test.clj:120 |
| `clj-surgeon.mcp-hot-verify-test` | 10.0 | PORT TMP | `:port 0 (ephemeral)` mcp_hot_verify_test.clj:12; `/tmp literal` mcp_hot_verify_test.clj:73 |
| `clj-surgeon.admit-patch-test` | 6.5 | SLEEP SHARED SPAWN TMP | `Thread/sleep` admit_patch_test.clj:1443; `$HOME` admit_patch_test.clj:4099; `ProcessBuilder` admit_patch_test.clj:4095 |
| `clj-surgeon.mcp-compact-relations-test` | 6.4 | GLOBAL TMP | `with-redefs` mcp_compact_relations_test.clj:777; `create-temp-dir` mcp_compact_relations_test.clj:241 |
| `clj-surgeon.mcp-tool-test` | 6.0 | SLEEP GLOBAL PORT SPAWN TMP | `Thread/sleep` mcp_tool_test.clj:1380; `with-redefs` mcp_tool_test.clj:1262; `:port 0 (ephemeral)` mcp_tool_test.clj:1464 |
| `clj-surgeon.outline-differential-test` | 5.7 | **pure** |  |
| `clj-surgeon.mcp-process-test` | 3.6 | SLEEP SHARED SPAWN TMP | `Thread/sleep` mcp_process_test.clj:103; `$HOME` mcp_process_test.clj:128; `ProcessBuilder` mcp_process_test.clj:107 |
| `clj-surgeon.core-discovery-test` | 3.0 | SPAWN TMP | `babashka.process` core_discovery_test.clj:18; `create-temp-dir` core_discovery_test.clj:32 |
| `clj-surgeon.mcp-http-server-test` | 1.3 | GLOBAL PORT SPAWN TMP | `alter-var-root` mcp_http_server_test.clj:563; `:port 0 (ephemeral)` mcp_http_server_test.clj:193; `make` mcp_http_server_test.clj:80 |
| `clj-surgeon.mcp-operation-registry-test` | 0.9 | **pure** |  |
| `clj-surgeon.mcp-cold-verify-test` | 0.6 | SLEEP GLOBAL SPAWN TMP | `Thread/sleep` mcp_cold_verify_test.clj:26; `with-redefs` mcp_cold_verify_test.clj:180; `make` mcp_cold_verify_test.clj:202 |
| `clj-surgeon.scope-stream-test` | 0.5 | SLEEP SHARED | `Thread/sleep` scope_stream_test.clj:76; `target/` scope_stream_test.clj:319 |
| `clj-surgeon.mcp-inspect-tool-test` | 0.3 | SLEEP GLOBAL TMP | `Thread/sleep` mcp_inspect_tool_test.clj:733; `with-redefs` mcp_inspect_tool_test.clj:479; `create-temp-dir` mcp_inspect_tool_test.clj:32 |
| `clj-surgeon.outline-memory-test` | 0.2 | GLOBAL | `with-redefs` outline_memory_test.clj:112 |
| `clj-surgeon.mcp-change-buffer-test` | 0.1 | GLOBAL SPAWN TMP | `with-redefs` mcp_change_buffer_test.clj:774; `launcher/spawn naming` mcp_change_buffer_test.clj:761; `create-temp-dir` mcp_change_buffer_test.clj:36 |
| `clj-surgeon.workspace-onboarding-test` | 0.1 | SLEEP GLOBAL PORT TMP | `deref with timeout` workspace_onboarding_test.clj:308; `with-redefs` workspace_onboarding_test.clj:295; `fixed host:port` workspace_onboarding_test.clj:201 |
| `clj-surgeon.census-pool-test` | 0.1 | SLEEP | `Thread/sleep` census_pool_test.clj:19 |
| `clj-surgeon.mcp-inspect-contract-test` | 0.1 | TMP | `/tmp literal` mcp_inspect_contract_test.clj:186 |
| `clj-surgeon.mcp-compact-location-test` | 0.1 | GLOBAL TMP | `with-redefs` mcp_compact_location_test.clj:309; `create-temp-dir` mcp_compact_location_test.clj:19 |
| `clj-surgeon.mcp-create-files-test` | 0.1 | GLOBAL TMP | `with-redefs` mcp_create_files_test.clj:398; `create-temp-dir` mcp_create_files_test.clj:21 |
| `clj-surgeon.repository-hygiene-test` | 0.1 | SHARED SPAWN TMP | `.cpcache` repository_hygiene_test.clj:21; `clojure.java.shell` repository_hygiene_test.clj:10; `create-temp-dir` repository_hygiene_test.clj:93 |
| `clj-surgeon.mcp-program-tool-test` | 0.0 | SLEEP GLOBAL TMP | `:timeout` mcp_program_tool_test.clj:27; `with-redefs` mcp_program_tool_test.clj:174; `create-temp-dir` mcp_program_tool_test.clj:14 |
| `clj-surgeon.mcp-intent-contract-test` | 0.0 | TMP | `create-temp-dir` mcp_intent_contract_test.clj:138 |
| `clj-surgeon.mcp-prepared-request-test` | 0.0 | GLOBAL TMP | `with-redefs` mcp_prepared_request_test.clj:334; `create-temp-dir` mcp_prepared_request_test.clj:34 |
| `clj-surgeon.mcp-extraction-test` | 0.0 | GLOBAL | `with-redefs` mcp_extraction_test.clj:194 |
| `clj-surgeon.mcp-contract-test` | 0.0 | TMP | `/tmp literal` mcp_contract_test.clj:303 |
| `clj-surgeon.mcp-prepared-confirmation-test` | 0.0 | SLEEP TMP | `deref with timeout` mcp_prepared_confirmation_test.clj:438; `create-temp-dir` mcp_prepared_confirmation_test.clj:442 |
| `clj-surgeon.mcp-relation-census-round20-test` | 0.0 | SPAWN TMP | `launcher/spawn naming` mcp_relation_census_round20_test.clj:9; `create-temp-dir` mcp_relation_census_round20_test.clj:69 |
| `clj-surgeon.mcp-write-refusal-test` | 0.0 | TMP | `create-temp-dir` mcp_write_refusal_test.clj:32 |
| `clj-surgeon.mcp-combinable-transaction-test` | 0.0 | SLEEP TMP | `deref with timeout` mcp_combinable_transaction_test.clj:409; `create-temp-dir` mcp_combinable_transaction_test.clj:413 |
| `clj-surgeon.mcp-extraction-plan-test` | 0.0 | TMP | `create-temp-dir` mcp_extraction_plan_test.clj:224 |
| `clj-surgeon.quoted-var-refs-test` | 0.0 | GLOBAL TMP | `with-redefs` quoted_var_refs_test.clj:73; `create-temp-dir` quoted_var_refs_test.clj:190 |
| `clj-surgeon.mcp-server-test` | 0.0 | GLOBAL TMP | `alter-var-root` mcp_server_test.clj:247; `create-temp-dir` mcp_server_test.clj:20 |
| `clj-surgeon.mcp-schema-test` | 0.0 | **pure** |  |
| `clj-surgeon.mcp-recovery-test` | 0.0 | TMP | `create-temp-dir` mcp_recovery_test.clj:39 |
| `clj-surgeon.mcp-telemetry-test` | 0.0 | TMP | `create-temp-dir` mcp_telemetry_test.clj:35 |
| `clj-surgeon.mcp-workspace-test` | 0.0 | TMP | `create-temp-dir` mcp_workspace_test.clj:10 |
| `clj-surgeon.mcp-compact-edit-fields-test` | 0.0 | **pure** |  |
| `clj-surgeon.mcp-compact-edit-test` | 0.0 | **pure** |  |
| `clj-surgeon.mcp-read-request-normalization-test` | 0.0 | **pure** |  |
| `clj-surgeon.mcp-operation-async-test` | 0.0 | GLOBAL | `with-redefs` mcp_operation_async_test.clj:13 |
| `clj-surgeon.mcp-operation-test` | 0.0 | **pure** |  |
| `clj-surgeon.mcp-semantic-client-test` | 0.0 | GLOBAL | `with-redefs` mcp_semantic_client_test.clj:20 |
| `clj-surgeon.mcp-paths-test` | 0.0 | TMP | `create-temp-dir` mcp_paths_test.clj:14 |

### bb lane — `bb test/run_all.clj` (not measured this round) (52 namespaces)

| namespace | wall s | tags | first evidence (file:line) |
|---|---:|---|---|
| `clj-surgeon.agent-routing-test` | — | TMP | `create-temp-dir` agent_routing_test.clj:57 |
| `clj-surgeon.alias-migration-test` | — | GLOBAL | `alter-var-root` alias_migration_test.clj:464 |
| `clj-surgeon.analyze-test` | — | PORT | `:port <fixed>` analyze_test.clj:27 |
| `clj-surgeon.analyzer-contract-test` | — | TMP | `create-temp-dir` analyzer_contract_test.clj:144 |
| `clj-surgeon.cli-dispatch-test` | — | SPAWN TMP | `babashka.process` cli_dispatch_test.clj:13; `java.io.tmpdir` cli_dispatch_test.clj:394 |
| `clj-surgeon.cljc-existing-ops-test` | — | TMP | `create-temp-file` cljc_existing_ops_test.clj:30 |
| `clj-surgeon.cljc.analyze-test` | — | **pure** |  |
| `clj-surgeon.cljc.merge-test` | — | **pure** |  |
| `clj-surgeon.cljc.require-ops-test` | — | **pure** |  |
| `clj-surgeon.cljc.split-test` | — | TMP | `create-temp-file` cljc/split_test.clj:112 |
| `clj-surgeon.diagnostic-delta-test` | — | **pure** |  |
| `clj-surgeon.edit-dsl-test` | — | SPAWN TMP | `clojure.java.shell` edit_dsl_test.clj:232; `/tmp literal` edit_dsl_test.clj:230 |
| `clj-surgeon.edit-test` | — | GLOBAL SPAWN TMP | `with-redefs` edit_test.clj:573; `babashka.process` edit_test.clj:4; `create-temp-dir` edit_test.clj:175 |
| `clj-surgeon.edn-config-integration-test` | — | GLOBAL SPAWN TMP | `reset! on a foreign atom` edn_config_integration_test.clj:66; `babashka.process` edn_config_integration_test.clj:33; `create-temp-dir` edn_config_integration_test.clj:45 |
| `clj-surgeon.extract-header-test` | — | **pure** |  |
| `clj-surgeon.extract-test` | — | GLOBAL PORT SPAWN TMP | `with-redefs` extract_test.clj:423; `:port <fixed>` extract_test.clj:23; `clojure.java.shell` extract_test.clj:6 |
| `clj-surgeon.failure-report-test` | — | TMP | `create-temp-dir` failure_report_test.clj:35 |
| `clj-surgeon.file-ops-test` | — | TMP | `create-temp-dir` file_ops_test.clj:8 |
| `clj-surgeon.fix-declares-test` | — | GLOBAL TMP | `with-redefs` fix_declares_test.clj:124; `create-temp-file` fix_declares_test.clj:10 |
| `clj-surgeon.forms-test` | — | GLOBAL TMP | `with-redefs` forms_test.clj:141; `create-temp-dir` forms_test.clj:209 |
| `clj-surgeon.help-test` | — | SPAWN TMP | `babashka.process` help_test.clj:3; `create-temp-file` help_test.clj:471 |
| `clj-surgeon.insertion-gap-test` | — | **pure** |  |
| `clj-surgeon.install-test` | — | SLEEP PORT SPAWN TMP | `poll/wait loop` install_test.clj:107; `http server` install_test.clj:228; `babashka.process` install_test.clj:4 |
| `clj-surgeon.intent-transaction-test` | — | GLOBAL SPAWN TMP | `with-redefs` intent_transaction_test.clj:540; `babashka.process` intent_transaction_test.clj:4; `create-temp-dir` intent_transaction_test.clj:599 |
| `clj-surgeon.lens-query-test` | — | SPAWN TMP | `babashka.process` lens_query_test.clj:4; `create-temp-dir` lens_query_test.clj:503 |
| `clj-surgeon.ls-tree-test` | — | TMP | `create-temp-dir` ls_tree_test.clj:276 |
| `clj-surgeon.mcp-formatter-test` | — | SPAWN TMP | `make` mcp_formatter_test.clj:76; `create-temp-dir` mcp_formatter_test.clj:13 |
| `clj-surgeon.memory-battery-test` | — | **pure** |  |
| `clj-surgeon.memory.journal-green-test` | — | **pure** |  |
| `clj-surgeon.memory.oom-reproduction-test` | — | **pure** |  |
| `clj-surgeon.move-dependency-test` | — | TMP | `create-temp-file` move_dependency_test.clj:291 |
| `clj-surgeon.move-test` | — | GLOBAL TMP | `alter-var-root` move_test.clj:380; `create-temp-file` move_test.clj:22 |
| `clj-surgeon.operation-algebra-test` | — | GLOBAL TMP | `with-redefs` operation_algebra_test.clj:610; `create-temp-dir` operation_algebra_test.clj:572 |
| `clj-surgeon.outermost-test` | — | **pure** |  |
| `clj-surgeon.outline-test` | — | GLOBAL TMP | `defonce` outline_test.clj:21; `create-temp-file` outline_test.clj:38 |
| `clj-surgeon.owner-hypotheses-test` | — | **pure** |  |
| `clj-surgeon.parser-admission-test` | — | GLOBAL TMP | `with-redefs` parser_admission_test.clj:276; `create-temp-dir` parser_admission_test.clj:579 |
| `clj-surgeon.partition-all-test` | — | SLEEP SPAWN TMP | `:timeout` partition_all_test.clj:27; `babashka.process` partition_all_test.clj:3; `create-temp-file` partition_all_test.clj:146 |
| `clj-surgeon.platform-selector-test` | — | SPAWN TMP | `babashka.process` platform_selector_test.clj:4; `create-temp-dir` platform_selector_test.clj:139 |
| `clj-surgeon.recovery-test` | — | TMP | `create-temp-dir` recovery_test.clj:17 |
| `clj-surgeon.relation-census-test` | — | GLOBAL | `with-redefs` relation_census_test.clj:126 |
| `clj-surgeon.rename-test` | — | TMP | `create-temp-file` rename_test.clj:61 |
| `clj-surgeon.show-form-test` | — | GLOBAL SPAWN TMP | `with-redefs` show_form_test.clj:107; `babashka.process` show_form_test.clj:4; `create-temp-dir` show_form_test.clj:474 |
| `clj-surgeon.structural-lens-test` | — | TMP | `create-temp-file` structural_lens_test.clj:15 |
| `clj-surgeon.syntax-var-refs-test` | — | **pure** |  |
| `clj-surgeon.tmp-leak-support-test` | — | GLOBAL SPAWN TMP | `with-redefs` tmp_leak_support_test.clj:152; `clojure.java.shell` tmp_leak_support_test.clj:20; `java.io.tmpdir` tmp_leak_support_test.clj:202 |
| `clj-surgeon.worktree-lifecycle-cli-test` | — | SPAWN | `make` worktree_lifecycle_cli_test.clj:33 |
| `clj-surgeon.worktree-lifecycle-io-test` | — | TMP | `create-temp-dir` worktree_lifecycle_io_test.clj:204 |
| `clj-surgeon.worktree-lifecycle-prune-test` | — | GLOBAL SPAWN TMP | `with-redefs` worktree_lifecycle_prune_test.clj:536; `ProcessBuilder` worktree_lifecycle_prune_test.clj:98; `create-temp-dir` worktree_lifecycle_prune_test.clj:125 |
| `clj-surgeon.worktree-lifecycle-recovery-test` | — | GLOBAL TMP | `with-redefs` worktree_lifecycle_recovery_test.clj:205; `create-temp-dir` worktree_lifecycle_recovery_test.clj:92 |
| `clj-surgeon.worktree-lifecycle-test` | — | **pure** |  |
| `clj-surgeon.xray-test` | — | SPAWN TMP | `babashka.process` xray_test.clj:4; `create-temp-dir` xray_test.clj:619 |
