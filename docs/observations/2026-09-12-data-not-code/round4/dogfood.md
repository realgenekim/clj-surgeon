# Source-edit accounting

The resumed checkout started at `6c5b1230` with four draft files. The table
covers the complete block since `3383da9b`, then identifies this round's edits.
Round 3 commits and logs are retained, not replayed. Their original editing
transport is not recorded in those receipts and remains unknown here.

| File | Intent | Mechanism in round 4 | Refusal / repair text |
|---|---|---|---|
| `src/clj_surgeon/receipt_artifacts.clj` | Trusted startup envelope, final-target admission, receipt identity | Inherited `dfa16a54`; native return-type hint plus standard-clj | Kernel reflection gate names the missing File return type; corrected hint placement clears lint |
| `src/clj_surgeon/operation_algebra.clj` | Validate and carry trusted envelope authority | Inherited `dfa16a54`; reviewed | Original editing transport unknown |
| `src/clj_surgeon/intent_transaction.clj` | Bind context and admit inverse receipt before mutation | Inherited `dfa16a54`; reviewed | Original editing transport unknown |
| `src/clj_surgeon/core.clj` | Initialize CLI envelope and admit receipt targets | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_server.clj` | Initialize stdio startup envelope | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_http_server.clj` | Initialize HTTP startup envelope | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_alias_migration.clj` | Admit telemetry directory and final ledger before creation | Inherited `dfa16a54`; native effect-label patch plus standard-clj | Census misreads alias-migration-prefixed effect data as a refusal; explicit family is now :telemetry-append |
| `src/clj_surgeon/extract.clj` | Admit extraction bookkeeping targets | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/insert_forms.clj` | Admit insertion bookkeeping targets | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_change_buffer.clj` | Admit buffer artifacts | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_cold_verify.clj` | Admit final cold-proof publication | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_extraction.clj` | Admit extraction receipts | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_helper_extraction.clj` | Admit helper-extraction artifacts | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mcp_tool.clj` | Admit MCP edit receipts | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/mission_typist_executor.clj` | Admit typist bookkeeping | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/namespace_split_io.clj` | Admit final split detail files | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/rename_alias.clj` | Admit final alias detail files | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/require_change_io.clj` | Admit final require-change detail files; preserve envelope identity in compact output | Inherited `dfa16a54`; exact-form native patch plus standard-clj | Red equality witness exposed receipt enrichment; compact and stored receipts now both name the envelope |
| `src/clj_surgeon/txn_journal.clj` | Admit journal/lock/preimage artifacts | Inherited `dfa16a54` | Original editing transport unknown |
| `src/clj_surgeon/edit_dsl.clj` | Data-valued sandbox refusal; compact symbol remedy | Inherited `579f00e7`; exact-form native patch plus standard-clj | No tool refusal; existing CLI witness exposed 1,119-character output and supplied the 1,024 bound |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj` | Real publication, absent parents, symlinks, ledger, startup/default authority | Retained draft tags; exact-form native patch adds default/initialize-once checks; standard-clj | No tool refusal |
| `test/clj_surgeon/operation_algebra_test.clj` | Exact effect inventory and trusted-context assertions | Retained draft; exact-form native patch adds reachable pure receipt-hash owner; standard-clj | No tool refusal; red inventory names the missing owner |
| `test/clj_surgeon/mcp_alias_migration_test.clj` | Pin two actual new refusal kinds and compact receipt envelope | Exact-form native patch plus standard-clj | Prewarm supplied the new kinds and receipt difference; one misplaced closing delimiter was repaired before the green focused run |
| `test/clj_surgeon/xray_test.clj` | Changed spelling, typed denials and English-looking untyped errors | Inherited `91e9ebbf`; executed on both runtimes | Original editing transport unknown |
| `docs/observations/2026-09-12-bbtower-block-b/attempt20/fold.clj` | Publish validated EDN rows instead of source patch syntax | Inherited `c22660f3` | Original editing transport unknown |
| `test/clj_surgeon/lane_manifest.clj` | Consume measured rows, preserve declared policy | Inherited `c22660f3` | Original editing transport unknown |
| `test/clj_surgeon/lane_manifest_test.clj` | Independent row statistics, receipt admission and temporal identities | Inherited `f468a954`, `c22660f3`, `c8a4067e`, `2b71963c`; executed | Original editing transport unknown |
| `test/clj_surgeon/census_pool_test.clj` | Sleep purpose metadata and named test ownership | Inherited `2b71963c` | Original editing transport unknown |
| `test/clj_surgeon/mcp_hot_verify_test.clj` | Explicit spaced-stimulus identity | Inherited `2b71963c` | Original editing transport unknown |
| `test/clj_surgeon/mcp_tool_test.clj` | Temporal purpose identity / formatter normalization | Inherited `2b71963c` | Original editing transport unknown |
| `test/clj_surgeon/scope_stream_test.clj` | Poll ownership and temporal metadata | Inherited `2b71963c` | Original editing transport unknown |
| `test/clj_surgeon/deftest_census.edn` | Register eight new named witnesses without removing any | Retained round 3 generated draft | No new generation in round 4; gates check membership |
| `Makefile` | Link measured-row and temporal-identity intents to the coordinator gate | Retained round 3 draft | No new route or budget change |
| `round4/finalize-report.clj` | Bind machine-readable report to passing gate and current source digest | Native new-file patch; executed successfully | Replaced an inline reporting command with an unmatched delimiter; that command wrote no report |

Native is the working-tree skill's selected route for this class. There were no
Surgeon MCP calls or capability refusals in round 4. An initial shell witness
command had an invalid reader escape; the corrected command produced the
retained inventory red. The baseline lint snapshot was first placed outside
its namespace directory, producing three path-mismatch errors; the corrected
layout produced zero errors and the same seven inherited warnings. An initial
prose patch missed its context and made no write; the corrected patch applied.
Standard-clj also normalized existing indentation, require ordering and blank
lines in touched files; the large alias source diff is formatting apart from
the two effect arguments. The final prewarm exercises those formatted bytes.
