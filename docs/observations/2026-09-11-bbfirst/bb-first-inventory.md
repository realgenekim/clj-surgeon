# Babashka-first inventory

Base eae1e432; isolated bb require probes, 2026-09-11. Namespace names read with rewrite-clj, including metadata. The initial regex-based draft misread ns metadata and is superseded here. Classification names the first unavailable dependency on the current bb classpath; load success does not prove behavior. Tests mean *_test.clj, excluding executable runner/support files.

Base load counts: 103 bb / 20 JVM-only source namespaces; 109 bb / 50 JVM-only test namespaces. The hybrid execution witness reclassifies two tests below: final test eligibility is 107 bb / 52 JVM-only. The new bb probe namespace raises final source counts to 104 bb / 20 JVM-only. JVM-only total: 72. Existing bb lane: 50 namespaces (brief baseline: 898 tests). The explicitly authorized Make entrances run the existing admission helper; no Python implementation was added.

The table lists every JVM-only namespace and its reason. The final fast union has 109 namespaces, preserving the original bb and fast sets without duplicates. Runtime and cadence are independent; battery eligibility does not move a battery into the inner loop. The coordinator itself loads under bb and the fast, bb and prewarm Make entrances now use bb.

| New source namespace | Runtime | Evidence |
|---|---|---|
| src/clj_surgeon/probe.clj / clj-surgeon.probe | bb-portable | bb load, pure tests and live HTTP probe passed |

| File / namespace | Runtime | Evidence |
|---|---|---|
| src/clj_surgeon/agent_routing.clj / clj-surgeon.agent-routing | bb-portable | bb require passed |
| src/clj_surgeon/alias_migration.clj / clj-surgeon.alias-migration | bb-portable | bb require passed |
| src/clj_surgeon/analyze.clj / clj-surgeon.analyze | bb-portable | bb require passed |
| src/clj_surgeon/binding_rename.clj / clj-surgeon.binding-rename | bb-portable | bb require passed |
| src/clj_surgeon/census_discovery.clj / clj-surgeon.census-discovery | bb-portable | bb require passed |
| src/clj_surgeon/census_pool.clj / clj-surgeon.census-pool | JVM-only | BB-LOAD-FAIL Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| src/clj_surgeon/cljc/analyze.clj / clj-surgeon.cljc.analyze | bb-portable | bb require passed |
| src/clj_surgeon/cljc/merge.clj / clj-surgeon.cljc.merge | bb-portable | bb require passed |
| src/clj_surgeon/cljc/require_ops.clj / clj-surgeon.cljc.require-ops | bb-portable | bb require passed |
| src/clj_surgeon/cljc/split.clj / clj-surgeon.cljc.split | bb-portable | bb require passed |
| src/clj_surgeon/cljc/walk.clj / clj-surgeon.cljc.walk | bb-portable | bb require passed |
| src/clj_surgeon/core.clj / clj-surgeon.core | bb-portable | bb require passed |
| src/clj_surgeon/diagnostic_delta.clj / clj-surgeon.diagnostic-delta | bb-portable | bb require passed |
| src/clj_surgeon/edit_dsl.clj / clj-surgeon.edit-dsl | bb-portable | bb require passed |
| src/clj_surgeon/extract.clj / clj-surgeon.extract | bb-portable | bb require passed |
| src/clj_surgeon/extract_header.clj / clj-surgeon.extract-header | bb-portable | bb require passed |
| src/clj_surgeon/failure_report.clj / clj-surgeon.failure-report | bb-portable | bb require passed |
| src/clj_surgeon/fields.clj / clj-surgeon.fields | bb-portable | bb require passed |
| src/clj_surgeon/file_ops.clj / clj-surgeon.file-ops | bb-portable | bb require passed |
| src/clj_surgeon/fix_declares.clj / clj-surgeon.fix-declares | bb-portable | bb require passed |
| src/clj_surgeon/form_identity.clj / clj-surgeon.form-identity | bb-portable | bb require passed |
| src/clj_surgeon/forms.clj / clj-surgeon.forms | bb-portable | bb require passed |
| src/clj_surgeon/forward_refs.clj / clj-surgeon.forward-refs | bb-portable | bb require passed |
| src/clj_surgeon/helper_extraction.clj / clj-surgeon.helper-extraction | bb-portable | bb require passed |
| src/clj_surgeon/insert_forms.clj / clj-surgeon.insert-forms | bb-portable | bb require passed |
| src/clj_surgeon/insert_forms_plan.clj / clj-surgeon.insert-forms-plan | bb-portable | bb require passed |
| src/clj_surgeon/insert_forms_schema.clj / clj-surgeon.insert-forms-schema | bb-portable | bb require passed |
| src/clj_surgeon/intent_transaction.clj / clj-surgeon.intent-transaction | bb-portable | bb require passed |
| src/clj_surgeon/jvm_error.clj / clj-surgeon.jvm-error | bb-portable | bb require passed |
| src/clj_surgeon/mcp_admit_tool.clj / clj-surgeon.mcp-admit-tool | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_alias_migration.clj / clj-surgeon.mcp-alias-migration | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_change_buffer.clj / clj-surgeon.mcp-change-buffer | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_cold_verify.clj / clj-surgeon.mcp-cold-verify | bb-portable | bb require passed |
| src/clj_surgeon/mcp_combinable_transaction.clj / clj-surgeon.mcp-combinable-transaction | bb-portable | bb require passed |
| src/clj_surgeon/mcp_compact_edit_fields.clj / clj-surgeon.mcp-compact-edit-fields | bb-portable | bb require passed |
| src/clj_surgeon/mcp_compact_location.clj / clj-surgeon.mcp-compact-location | bb-portable | bb require passed |
| src/clj_surgeon/mcp_compact_relations.clj / clj-surgeon.mcp-compact-relations | bb-portable | bb require passed |
| src/clj_surgeon/mcp_contract.clj / clj-surgeon.mcp-contract | bb-portable | bb require passed |
| src/clj_surgeon/mcp_extraction.clj / clj-surgeon.mcp-extraction | bb-portable | bb require passed |
| src/clj_surgeon/mcp_extraction_plan.clj / clj-surgeon.mcp-extraction-plan | bb-portable | bb require passed |
| src/clj_surgeon/mcp_feature_thread.clj / clj-surgeon.mcp-feature-thread | bb-portable | bb require passed |
| src/clj_surgeon/mcp_formatter.clj / clj-surgeon.mcp-formatter | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_helper_extraction.clj / clj-surgeon.mcp-helper-extraction | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_hot_verify.clj / clj-surgeon.mcp-hot-verify | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_http_server.clj / clj-surgeon.mcp-http-server | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_insert_forms.clj / clj-surgeon.mcp-insert-forms | bb-portable | bb require passed |
| src/clj_surgeon/mcp_inspect.clj / clj-surgeon.mcp-inspect | bb-portable | bb require passed |
| src/clj_surgeon/mcp_inspect_tool.clj / clj-surgeon.mcp-inspect-tool | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_intent_contract.clj / clj-surgeon.mcp-intent-contract | bb-portable | bb require passed |
| src/clj_surgeon/mcp_namespace_split.clj / clj-surgeon.mcp-namespace-split | bb-portable | bb require passed |
| src/clj_surgeon/mcp_operation.clj / clj-surgeon.mcp-operation | bb-portable | bb require passed |
| src/clj_surgeon/mcp_paths.clj / clj-surgeon.mcp-paths | bb-portable | bb require passed |
| src/clj_surgeon/mcp_prepared_confirmation.clj / clj-surgeon.mcp-prepared-confirmation | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| src/clj_surgeon/mcp_prepared_request.clj / clj-surgeon.mcp-prepared-request | bb-portable | bb require passed |
| src/clj_surgeon/mcp_process.clj / clj-surgeon.mcp-process | bb-portable | bb require passed |
| src/clj_surgeon/mcp_program_tool.clj / clj-surgeon.mcp-program-tool | bb-portable | bb require passed |
| src/clj_surgeon/mcp_recovery.clj / clj-surgeon.mcp-recovery | bb-portable | bb require passed |
| src/clj_surgeon/mcp_relation_census.clj / clj-surgeon.mcp-relation-census | JVM-only | BB-LOAD-FAIL Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| src/clj_surgeon/mcp_rename_alias.clj / clj-surgeon.mcp-rename-alias | bb-portable | bb require passed |
| src/clj_surgeon/mcp_require_change.clj / clj-surgeon.mcp-require-change | bb-portable | bb require passed |
| src/clj_surgeon/mcp_runtime.clj / clj-surgeon.mcp-runtime | bb-portable | bb require passed |
| src/clj_surgeon/mcp_schema.clj / clj-surgeon.mcp-schema | bb-portable | bb require passed |
| src/clj_surgeon/mcp_semantic_client.clj / clj-surgeon.mcp-semantic-client | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.client.McpClient |
| src/clj_surgeon/mcp_server.clj / clj-surgeon.mcp-server | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| src/clj_surgeon/mcp_source_anchor.clj / clj-surgeon.mcp-source-anchor | bb-portable | bb require passed |
| src/clj_surgeon/mcp_telemetry.clj / clj-surgeon.mcp-telemetry | bb-portable | bb require passed |
| src/clj_surgeon/mcp_tool.clj / clj-surgeon.mcp-tool | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mcp_workspace.clj / clj-surgeon.mcp-workspace | bb-portable | bb require passed |
| src/clj_surgeon/mcp_workspace_sources.clj / clj-surgeon.mcp-workspace-sources | bb-portable | bb require passed |
| src/clj_surgeon/mcp_write_refusal.clj / clj-surgeon.mcp-write-refusal | bb-portable | bb require passed |
| src/clj_surgeon/memory_battery.clj / clj-surgeon.memory-battery | bb-portable | bb require passed |
| src/clj_surgeon/memory_battery_runner.clj / clj-surgeon.memory-battery-runner | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.lang.management.ManagementFactory |
| src/clj_surgeon/mission.clj / clj-surgeon.mission | bb-portable | bb require passed |
| src/clj_surgeon/mission_candidate.clj / clj-surgeon.mission-candidate | bb-portable | bb require passed |
| src/clj_surgeon/mission_candidate_race.clj / clj-surgeon.mission-candidate-race | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.util.concurrent.ExecutorCompletionService |
| src/clj_surgeon/mission_cli.clj / clj-surgeon.mission-cli | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mission_display.clj / clj-surgeon.mission-display | bb-portable | bb require passed |
| src/clj_surgeon/mission_events.clj / clj-surgeon.mission-events | bb-portable | bb require passed |
| src/clj_surgeon/mission_fallback.clj / clj-surgeon.mission-fallback | bb-portable | bb require passed |
| src/clj_surgeon/mission_forms.clj / clj-surgeon.mission-forms | bb-portable | bb require passed |
| src/clj_surgeon/mission_forms_source.clj / clj-surgeon.mission-forms-source | bb-portable | bb require passed |
| src/clj_surgeon/mission_git.clj / clj-surgeon.mission-git | bb-portable | bb require passed |
| src/clj_surgeon/mission_git_ledger.clj / clj-surgeon.mission-git-ledger | bb-portable | bb require passed |
| src/clj_surgeon/mission_git_process.clj / clj-surgeon.mission-git-process | bb-portable | bb require passed |
| src/clj_surgeon/mission_plain_forms.clj / clj-surgeon.mission-plain-forms | bb-portable | bb require passed |
| src/clj_surgeon/mission_typist.clj / clj-surgeon.mission-typist | bb-portable | bb require passed |
| src/clj_surgeon/mission_typist_executor.clj / clj-surgeon.mission-typist-executor | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| src/clj_surgeon/mission_usage.clj / clj-surgeon.mission-usage | bb-portable | bb require passed |
| src/clj_surgeon/move.clj / clj-surgeon.move | bb-portable | bb require passed |
| src/clj_surgeon/namespace_split.clj / clj-surgeon.namespace-split | bb-portable | bb require passed |
| src/clj_surgeon/namespace_split_io.clj / clj-surgeon.namespace-split-io | bb-portable | bb require passed |
| src/clj_surgeon/namespace_split_warm.clj / clj-surgeon.namespace-split-warm | bb-portable | bb require passed |
| src/clj_surgeon/operation_algebra.clj / clj-surgeon.operation-algebra | bb-portable | bb require passed |
| src/clj_surgeon/outline.clj / clj-surgeon.outline | bb-portable | bb require passed |
| src/clj_surgeon/owner_hypotheses.clj / clj-surgeon.owner-hypotheses | bb-portable | bb require passed |
| src/clj_surgeon/parse_admission.clj / clj-surgeon.parse-admission | bb-portable | bb require passed |
| src/clj_surgeon/patch_apply.clj / clj-surgeon.patch-apply | bb-portable | bb require passed |
| src/clj_surgeon/path_classification.clj / clj-surgeon.path-classification | bb-portable | bb require passed |
| src/clj_surgeon/quoted_var_refs.clj / clj-surgeon.quoted-var-refs | bb-portable | bb require passed |
| src/clj_surgeon/receipt_artifacts.clj / clj-surgeon.receipt-artifacts | bb-portable | bb require passed |
| src/clj_surgeon/recovery.clj / clj-surgeon.recovery | bb-portable | bb require passed |
| src/clj_surgeon/relation_census.clj / clj-surgeon.relation-census | bb-portable | bb require passed |
| src/clj_surgeon/rename.clj / clj-surgeon.rename | bb-portable | bb require passed |
| src/clj_surgeon/rename_alias.clj / clj-surgeon.rename-alias | bb-portable | bb require passed |
| src/clj_surgeon/rename_alias_plan.clj / clj-surgeon.rename-alias-plan | bb-portable | bb require passed |
| src/clj_surgeon/rename_alias_schema.clj / clj-surgeon.rename-alias-schema | bb-portable | bb require passed |
| src/clj_surgeon/require_change.clj / clj-surgeon.require-change | bb-portable | bb require passed |
| src/clj_surgeon/require_change_io.clj / clj-surgeon.require-change-io | bb-portable | bb require passed |
| src/clj_surgeon/scope_stream.clj / clj-surgeon.scope-stream | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.nio.file.SimpleFileVisitor |
| src/clj_surgeon/show_form.clj / clj-surgeon.show-form | bb-portable | bb require passed |
| src/clj_surgeon/spawn_ledger.clj / clj-surgeon.spawn-ledger | bb-portable | bb require passed |
| src/clj_surgeon/splice_projection.clj / clj-surgeon.splice-projection | bb-portable | bb require passed |
| src/clj_surgeon/split_proof_gate.clj / clj-surgeon.split-proof-gate | bb-portable | bb require passed |
| src/clj_surgeon/structural_lens.clj / clj-surgeon.structural-lens | bb-portable | bb require passed |
| src/clj_surgeon/synchronous_verification.clj / clj-surgeon.synchronous-verification | bb-portable | bb require passed |
| src/clj_surgeon/syntax_var_refs.clj / clj-surgeon.syntax-var-refs | bb-portable | bb require passed |
| src/clj_surgeon/telemetry_events.clj / clj-surgeon.telemetry-events | bb-portable | bb require passed |
| src/clj_surgeon/txn_journal.clj / clj-surgeon.txn-journal | bb-portable | bb require passed |
| src/clj_surgeon/verification_process.clj / clj-surgeon.verification-process | bb-portable | bb require passed |
| src/clj_surgeon/workspace_lock.clj / clj-surgeon.workspace-lock | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.nio.channels.OverlappingFileLockException |
| src/clj_surgeon/workspace_onboarding.clj / clj-surgeon.workspace-onboarding | bb-portable | bb require passed |
| src/clj_surgeon/worktree_lifecycle.clj / clj-surgeon.worktree-lifecycle | bb-portable | bb require passed |
| src/clj_surgeon/worktree_lifecycle_io.clj / clj-surgeon.worktree-lifecycle-io | bb-portable | bb require passed |
| test/clj_surgeon/admit_patch_test.clj / clj-surgeon.admit-patch-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/agent_routing_test.clj / clj-surgeon.agent-routing-test | bb-portable | bb require passed |
| test/clj_surgeon/alias_migration_test.clj / clj-surgeon.alias-migration-test | bb-portable | bb require passed |
| test/clj_surgeon/analyze_test.clj / clj-surgeon.analyze-test | bb-portable | bb require passed |
| test/clj_surgeon/analyzer_contract_test.clj / clj-surgeon.analyzer-contract-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/battery_ledger_test.clj / clj-surgeon.battery-ledger-test | bb-portable | bb require passed |
| test/clj_surgeon/battery_parallel_test.clj / clj-surgeon.battery-parallel-test | bb-portable | bb require passed |
| test/clj_surgeon/cell_b_oracle_test.clj / clj-surgeon.cell-b-oracle-test | bb-portable | bb require passed |
| test/clj_surgeon/census_pool_test.clj / clj-surgeon.census-pool-test | JVM-only | BB-LOAD-FAIL Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| test/clj_surgeon/cli_dispatch_test.clj / clj-surgeon.cli-dispatch-test | bb-portable | bb require passed |
| test/clj_surgeon/cljc/analyze_test.clj / clj-surgeon.cljc.analyze-test | bb-portable | bb require passed |
| test/clj_surgeon/cljc/merge_test.clj / clj-surgeon.cljc.merge-test | bb-portable | bb require passed |
| test/clj_surgeon/cljc/require_ops_test.clj / clj-surgeon.cljc.require-ops-test | bb-portable | bb require passed |
| test/clj_surgeon/cljc/split_test.clj / clj-surgeon.cljc.split-test | bb-portable | bb require passed |
| test/clj_surgeon/cljc_existing_ops_test.clj / clj-surgeon.cljc-existing-ops-test | bb-portable | bb require passed |
| test/clj_surgeon/core_discovery_test.clj / clj-surgeon.core-discovery-test | bb-portable | bb require passed |
| test/clj_surgeon/diagnostic_delta_test.clj / clj-surgeon.diagnostic-delta-test | bb-portable | bb require passed |
| test/clj_surgeon/edit_dsl_test.clj / clj-surgeon.edit-dsl-test | bb-portable | bb require passed |
| test/clj_surgeon/edit_test.clj / clj-surgeon.edit-test | bb-portable | bb require passed |
| test/clj_surgeon/edn_config_integration_test.clj / clj-surgeon.edn-config-integration-test | bb-portable | bb require passed |
| test/clj_surgeon/extract_header_test.clj / clj-surgeon.extract-header-test | bb-portable | bb require passed |
| test/clj_surgeon/extract_test.clj / clj-surgeon.extract-test | bb-portable | bb require passed |
| test/clj_surgeon/failure_report_test.clj / clj-surgeon.failure-report-test | bb-portable | bb require passed |
| test/clj_surgeon/fast_lane_isolation_test.clj / clj-surgeon.fast-lane-isolation-test | bb-portable | bb require passed |
| test/clj_surgeon/file_ops_test.clj / clj-surgeon.file-ops-test | bb-portable | bb require passed |
| test/clj_surgeon/fix_declares_test.clj / clj-surgeon.fix-declares-test | bb-portable | bb require passed |
| test/clj_surgeon/forms_test.clj / clj-surgeon.forms-test | bb-portable | bb require passed |
| test/clj_surgeon/help_test.clj / clj-surgeon.help-test | bb-portable | bb require passed |
| test/clj_surgeon/helper_extraction_test.clj / clj-surgeon.helper-extraction-test | bb-portable | bb require passed |
| test/clj_surgeon/insert_forms_parity_test.clj / clj-surgeon.insert-forms-parity-test | bb-portable | bb require passed |
| test/clj_surgeon/insert_forms_receipt_test.clj / clj-surgeon.insert-forms-receipt-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/insert_forms_test.clj / clj-surgeon.insert-forms-test | bb-portable | bb require passed |
| test/clj_surgeon/insertion_gap_test.clj / clj-surgeon.insertion-gap-test | bb-portable | bb require passed |
| test/clj_surgeon/install_test.clj / clj-surgeon.install-test | bb-portable | bb require passed |
| test/clj_surgeon/intent_transaction_test.clj / clj-surgeon.intent-transaction-test | bb-portable | bb require passed |
| test/clj_surgeon/jvm_error_test.clj / clj-surgeon.jvm-error-test | bb-portable | bb require passed |
| test/clj_surgeon/lane_manifest_test.clj / clj-surgeon.lane-manifest-test | bb-portable | bb require passed |
| test/clj_surgeon/lens_query_test.clj / clj-surgeon.lens-query-test | bb-portable | bb require passed |
| test/clj_surgeon/ls_tree_test.clj / clj-surgeon.ls-tree-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_alias_migration_test.clj / clj-surgeon.mcp-alias-migration-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_change_buffer_test.clj / clj-surgeon.mcp-change-buffer-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_cold_verify_test.clj / clj-surgeon.mcp-cold-verify-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_combinable_transaction_test.clj / clj-surgeon.mcp-combinable-transaction-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_compact_edit_fields_test.clj / clj-surgeon.mcp-compact-edit-fields-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_compact_edit_test.clj / clj-surgeon.mcp-compact-edit-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_compact_location_test.clj / clj-surgeon.mcp-compact-location-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_compact_relations_test.clj / clj-surgeon.mcp-compact-relations-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_contract_test.clj / clj-surgeon.mcp-contract-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_create_files_test.clj / clj-surgeon.mcp-create-files-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_expect_guard_test.clj / clj-surgeon.mcp-expect-guard-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_extraction_plan_test.clj / clj-surgeon.mcp-extraction-plan-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_extraction_test.clj / clj-surgeon.mcp-extraction-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_feature_thread_sed_test.clj / clj-surgeon.mcp-feature-thread-sed-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_feature_thread_test.clj / clj-surgeon.mcp-feature-thread-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_formatter_test.clj / clj-surgeon.mcp-formatter-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_helper_extraction_test.clj / clj-surgeon.mcp-helper-extraction-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_hot_verify_test.clj / clj-surgeon.mcp-hot-verify-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_http_server_test.clj / clj-surgeon.mcp-http-server-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_inspect_cold_job_test.clj / clj-surgeon.mcp-inspect-cold-job-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_inspect_contract_test.clj / clj-surgeon.mcp-inspect-contract-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_inspect_tool_test.clj / clj-surgeon.mcp-inspect-tool-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_intent_contract_test.clj / clj-surgeon.mcp-intent-contract-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_namespace_split_test.clj / clj-surgeon.mcp-namespace-split-test | JVM-only | bb load passes; executed empty-profile witness dynamically requires nrepl/core |
| test/clj_surgeon/mcp_operation_async_test.clj / clj-surgeon.mcp-operation-async-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_operation_registry_test.clj / clj-surgeon.mcp-operation-registry-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| test/clj_surgeon/mcp_operation_test.clj / clj-surgeon.mcp-operation-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.util.Locale$Category |
| test/clj_surgeon/mcp_paths_test.clj / clj-surgeon.mcp-paths-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_prepared_confirmation_test.clj / clj-surgeon.mcp-prepared-confirmation-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_prepared_request_test.clj / clj-surgeon.mcp-prepared-request-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_prepared_wire_test.clj / clj-surgeon.mcp-prepared-wire-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| test/clj_surgeon/mcp_process_test.clj / clj-surgeon.mcp-process-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_program_tool_test.clj / clj-surgeon.mcp-program-tool-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_read_request_normalization_test.clj / clj-surgeon.mcp-read-request-normalization-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_recovery_test.clj / clj-surgeon.mcp-recovery-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_relation_census_launcher_test.clj / clj-surgeon.mcp-relation-census-launcher-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_relation_census_round20_test.clj / clj-surgeon.mcp-relation-census-round20-test | JVM-only | BB-LOAD-FAIL Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| test/clj_surgeon/mcp_relation_census_test.clj / clj-surgeon.mcp-relation-census-test | JVM-only | BB-LOAD-FAIL Could not locate com/climate/claypoole.bb, com/climate/claypoole.clj or com/climate/claypoole.cljc on classpath. |
| test/clj_surgeon/mcp_schema_test.clj / clj-surgeon.mcp-schema-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_semantic_client_test.clj / clj-surgeon.mcp-semantic-client-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.client.McpClient |
| test/clj_surgeon/mcp_server_test.clj / clj-surgeon.mcp-server-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_telemetry_test.clj / clj-surgeon.mcp-telemetry-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_tool_test.clj / clj-surgeon.mcp-tool-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mcp_workspace_test.clj / clj-surgeon.mcp-workspace-test | bb-portable | bb require passed |
| test/clj_surgeon/mcp_write_refusal_test.clj / clj-surgeon.mcp-write-refusal-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/memory/journal_green_test.clj / clj-surgeon.memory.journal-green-test | bb-portable | bb require passed |
| test/clj_surgeon/memory/oom_reproduction_test.clj / clj-surgeon.memory.oom-reproduction-test | bb-portable | bb require passed |
| test/clj_surgeon/memory_battery_test.clj / clj-surgeon.memory-battery-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_candidate_race_test.clj / clj-surgeon.mission-candidate-race-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.util.concurrent.ExecutorCompletionService |
| test/clj_surgeon/mission_candidate_test.clj / clj-surgeon.mission-candidate-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_commit_cli_test.clj / clj-surgeon.mission-commit-cli-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_display_test.clj / clj-surgeon.mission-display-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_events_test.clj / clj-surgeon.mission-events-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_fallback_test.clj / clj-surgeon.mission-fallback-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_forms_source_test.clj / clj-surgeon.mission-forms-source-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_forms_test.clj / clj-surgeon.mission-forms-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_boundary_test.clj / clj-surgeon.mission-git-boundary-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_fence_test.clj / clj-surgeon.mission-git-fence-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_identity_test.clj / clj-surgeon.mission-git-identity-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_ledger_test.clj / clj-surgeon.mission-git-ledger-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_git_process_test.clj / clj-surgeon.mission-git-process-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_submodule_test.clj / clj-surgeon.mission-git-submodule-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_git_test.clj / clj-surgeon.mission-git-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_phase_events_test.clj / clj-surgeon.mission-phase-events-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_plain_forms_test.clj / clj-surgeon.mission-plain-forms-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_provider_fallback_events_test.clj / clj-surgeon.mission-provider-fallback-events-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.util.concurrent.ExecutorCompletionService |
| test/clj_surgeon/mission_publication_test.clj / clj-surgeon.mission-publication-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_run_test.clj / clj-surgeon.mission-run-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_test.clj / clj-surgeon.mission-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_typist_executor_admission_test.clj / clj-surgeon.mission-typist-executor-admission-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_typist_executor_test.clj / clj-surgeon.mission-typist-executor-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_typist_test.clj / clj-surgeon.mission-typist-test | bb-portable | bb require passed |
| test/clj_surgeon/mission_usage_executor_test.clj / clj-surgeon.mission-usage-executor-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/mission_usage_test.clj / clj-surgeon.mission-usage-test | bb-portable | bb require passed |
| test/clj_surgeon/move_dependency_test.clj / clj-surgeon.move-dependency-test | bb-portable | bb require passed |
| test/clj_surgeon/move_test.clj / clj-surgeon.move-test | bb-portable | bb require passed |
| test/clj_surgeon/namespace_split_test.clj / clj-surgeon.namespace-split-test | bb-portable | bb require passed |
| test/clj_surgeon/namespace_split_warm_test.clj / clj-surgeon.namespace-split-warm-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/server.bb, nrepl/server.clj or nrepl/server.cljc on classpath. |
| test/clj_surgeon/ns_isolation_test.clj / clj-surgeon.ns-isolation-test | JVM-only | bb load passes; executed reflection witnesses require JVM Var.getRawRoot |
| test/clj_surgeon/operation_algebra_test.clj / clj-surgeon.operation-algebra-test | bb-portable | bb require passed |
| test/clj_surgeon/outermost_test.clj / clj-surgeon.outermost-test | bb-portable | bb require passed |
| test/clj_surgeon/outline_corpus_integration_test.clj / clj-surgeon.outline-corpus-integration-test | bb-portable | bb require passed |
| test/clj_surgeon/outline_differential_test.clj / clj-surgeon.outline-differential-test | bb-portable | bb require passed |
| test/clj_surgeon/outline_memory_test.clj / clj-surgeon.outline-memory-test | JVM-only | BB-LOAD-FAIL Unable to resolve symbol: java.lang.management.ManagementFactory/getThreadMXBean |
| test/clj_surgeon/outline_test.clj / clj-surgeon.outline-test | bb-portable | bb require passed |
| test/clj_surgeon/owner_hypotheses_test.clj / clj-surgeon.owner-hypotheses-test | bb-portable | bb require passed |
| test/clj_surgeon/parser_admission_test.clj / clj-surgeon.parser-admission-test | bb-portable | bb require passed |
| test/clj_surgeon/partition_all_test.clj / clj-surgeon.partition-all-test | bb-portable | bb require passed |
| test/clj_surgeon/platform_selector_test.clj / clj-surgeon.platform-selector-test | bb-portable | bb require passed |
| test/clj_surgeon/quoted_var_refs_test.clj / clj-surgeon.quoted-var-refs-test | bb-portable | bb require passed |
| test/clj_surgeon/reader_eval_fence_test.clj / clj-surgeon.reader-eval-fence-test | bb-portable | bb require passed |
| test/clj_surgeon/receipt_artifacts_boundary_test.clj / clj-surgeon.receipt-artifacts-boundary-test | JVM-only | BB-LOAD-FAIL Could not locate nrepl/core.bb, nrepl/core.clj or nrepl/core.cljc on classpath. |
| test/clj_surgeon/receipt_booleans_test.clj / clj-surgeon.receipt-booleans-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: io.modelcontextprotocol.server.McpAsyncServerExchange |
| test/clj_surgeon/recovery_test.clj / clj-surgeon.recovery-test | bb-portable | bb require passed |
| test/clj_surgeon/relation_census_test.clj / clj-surgeon.relation-census-test | bb-portable | bb require passed |
| test/clj_surgeon/rename_alias_parity_test.clj / clj-surgeon.rename-alias-parity-test | bb-portable | bb require passed |
| test/clj_surgeon/rename_alias_performance_test.clj / clj-surgeon.rename-alias-performance-test | bb-portable | bb require passed |
| test/clj_surgeon/rename_alias_receipt_test.clj / clj-surgeon.rename-alias-receipt-test | bb-portable | bb require passed |
| test/clj_surgeon/rename_alias_test.clj / clj-surgeon.rename-alias-test | bb-portable | bb require passed |
| test/clj_surgeon/rename_test.clj / clj-surgeon.rename-test | bb-portable | bb require passed |
| test/clj_surgeon/repository_hygiene_test.clj / clj-surgeon.repository-hygiene-test | bb-portable | bb require passed |
| test/clj_surgeon/require_change_boundary_test.clj / clj-surgeon.require-change-boundary-test | bb-portable | bb require passed |
| test/clj_surgeon/require_change_test.clj / clj-surgeon.require-change-test | bb-portable | bb require passed |
| test/clj_surgeon/scope_stream_test.clj / clj-surgeon.scope-stream-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.nio.file.SimpleFileVisitor |
| test/clj_surgeon/show_form_test.clj / clj-surgeon.show-form-test | bb-portable | bb require passed |
| test/clj_surgeon/splice_envelope_test.clj / clj-surgeon.splice-envelope-test | bb-portable | bb require passed |
| test/clj_surgeon/split_proof_gate_boundary_test.clj / clj-surgeon.split-proof-gate-boundary-test | bb-portable | bb require passed |
| test/clj_surgeon/split_proof_gate_test.clj / clj-surgeon.split-proof-gate-test | bb-portable | bb require passed |
| test/clj_surgeon/structural_lens_test.clj / clj-surgeon.structural-lens-test | bb-portable | bb require passed |
| test/clj_surgeon/syntax_var_refs_test.clj / clj-surgeon.syntax-var-refs-test | bb-portable | bb require passed |
| test/clj_surgeon/telemetry_events_test.clj / clj-surgeon.telemetry-events-test | bb-portable | bb require passed |
| test/clj_surgeon/tmp_leak_support_test.clj / clj-surgeon.tmp-leak-support-test | bb-portable | bb require passed |
| test/clj_surgeon/txn_journal_test.clj / clj-surgeon.txn-journal-test | JVM-only | BB-LOAD-FAIL Unable to resolve classname: java.nio.file.SimpleFileVisitor |
| test/clj_surgeon/workspace_onboarding_test.clj / clj-surgeon.workspace-onboarding-test | bb-portable | bb require passed |
| test/clj_surgeon/worktree_lifecycle_cli_test.clj / clj-surgeon.worktree-lifecycle-cli-test | bb-portable | bb require passed |
| test/clj_surgeon/worktree_lifecycle_io_test.clj / clj-surgeon.worktree-lifecycle-io-test | bb-portable | bb require passed |
| test/clj_surgeon/worktree_lifecycle_prune_test.clj / clj-surgeon.worktree-lifecycle-prune-test | bb-portable | bb require passed |
| test/clj_surgeon/worktree_lifecycle_recovery_test.clj / clj-surgeon.worktree-lifecycle-recovery-test | bb-portable | bb require passed |
| test/clj_surgeon/worktree_lifecycle_test.clj / clj-surgeon.worktree-lifecycle-test | bb-portable | bb require passed |
| test/clj_surgeon/xray_test.clj / clj-surgeon.xray-test | bb-portable | bb require passed |
