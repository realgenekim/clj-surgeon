# Literal census at 2f7b4cf8

Complete tracked scan: `git grep -n /var/tmp/forge 2f7b4cf8 -- test libs`, preserved in [literals-before.txt](literals-before.txt). There is no `lib/` directory; the actual `libs/` tree has no matches. Each row accounts for every occurrence on its named line, including repeated strings. Current scan is [literals-after.txt](literals-after.txt).

Scope is the named repairs and gate-executed I/O; negative paths, pure expectations, historical comments, and configurable out-of-prewarm battery/experiment defaults retain their roles. No budgets, membership, runtime classifications, or test skips changed.

| Original location | Decision | Reason |
|---|---|---|
| `test/clj_surgeon/artifact_boundary_support.clj:6` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/artifact_boundary_support.clj:7` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/battery_parallel_test.clj:29` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7181` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7291` | keep | Negative nonexistent path: refusal witness, never a writable fixture root. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7325` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7334` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7407` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mcp_alias_migration_test.clj:7408` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/mcp_expect_guard_test.clj:116` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mcp_expect_guard_test.clj:274` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mcp_feature_thread_test.clj:391` | keep | Negative nonexistent path: refusal witness, never a writable fixture root. |
| `test/clj_surgeon/mcp_helper_extraction_test.clj:57` | keep | Configurable battery-only fixture default (CLJ_SURGEON_MISSION_TMP / CLJ_SURGEON_HELPER_TMP); outside prewarm alias/mcp/bb membership. Kept as env-or-default, like SUITE_BATTERY_FX. A sandboxed full battery must supply these overrides; this attempt does not certify their defaults. |
| `test/clj_surgeon/mcp_inspect_tool_test.clj:1822` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mcp_namespace_split_test.clj:232` | keep | Negative nonexistent path: refusal witness, never a writable fixture root. |
| `test/clj_surgeon/mcp_process_test.clj:49` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/mission_commit_cli_test.clj:54` | keep | Negative nonexistent path: refusal witness, never a writable fixture root. |
| `test/clj_surgeon/mission_display_test.clj:131` | keep | Historical missing-workspace refusal input; CLI and function reject absent workspace before opening state-home. No real state is read or written. |
| `test/clj_surgeon/mission_display_test.clj:137` | keep | Historical missing-workspace refusal input; CLI and function reject absent workspace before opening state-home. No real state is read or written. |
| `test/clj_surgeon/mission_git_identity_test.clj:41` | change | Fresh java.io.tmpdir scratch via with-temp-dir, deleted in finally. Process/identity use it as real cwd; ledger rejects invalid options before I/O but now uses an owned workspace too. |
| `test/clj_surgeon/mission_git_ledger_test.clj:70` | change | Fresh java.io.tmpdir scratch via with-temp-dir, deleted in finally. Process/identity use it as real cwd; ledger rejects invalid options before I/O but now uses an owned workspace too. |
| `test/clj_surgeon/mission_git_process_test.clj:14` | change | Fresh java.io.tmpdir scratch via with-temp-dir, deleted in finally. Process/identity use it as real cwd; ledger rejects invalid options before I/O but now uses an owned workspace too. |
| `test/clj_surgeon/mission_provider_fallback_events_test.clj:27` | keep | Synthetic authority map; run-bounded! is replaced with fake-process. No process uses this cwd. |
| `test/clj_surgeon/mission_test.clj:30` | keep | Configurable battery-only fixture default (CLJ_SURGEON_MISSION_TMP / CLJ_SURGEON_HELPER_TMP); outside prewarm alias/mcp/bb membership. Kept as env-or-default, like SUITE_BATTERY_FX. A sandboxed full battery must supply these overrides; this attempt does not certify their defaults. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:33` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:95` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:96` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:166` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:167` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/receipt_artifacts_boundary_test.clj:169` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support.clj:17` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/tmp_leak_support.clj:20` | keep | Comment/docstring provenance or invocation example; no runtime filesystem access. |
| `test/clj_surgeon/tmp_leak_support_test.clj:55` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:82` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:83` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:85` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:121` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:156` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/clj_surgeon/tmp_leak_support_test.clj:321` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/oracles/b07_split_adapter.clj:6` | keep | Standalone experiment adapter, not a landing-gate command; TMPDIR overrides the historical fallback. |
| `test/oracles/cell_b_oracle.sh:3` | keep | Standalone shell adapter/comment. The gate Python oracle reads and executes only its A8 lint fragment, not these header/default lines; standalone callers can override ORACLE_TMP. |
| `test/oracles/cell_b_oracle.sh:8` | keep | Standalone shell adapter/comment. The gate Python oracle reads and executes only its A8 lint fragment, not these header/default lines; standalone callers can override ORACLE_TMP. |
| `test/oracles/cell_b_oracle.sh:14` | keep | Standalone shell adapter/comment. The gate Python oracle reads and executes only its A8 lint fragment, not these header/default lines; standalone callers can override ORACLE_TMP. |
| `test/oracles/test_gate_slot.py:404` | keep | Pure slot_root_for expectation; no filesystem access. |
| `test/oracles/test_gate_slot.py:405` | keep | Pure slot_root_for expectation; no filesystem access. |
| `test/oracles/test_gate_slot.py:414` | change | Real root or budget fixture now derives from disk TMPDIR; private short-root seam keeps the real bind inside the writable envelope. |
| `test/oracles/test_gate_slot.py:448` | change | Real root or budget fixture now derives from disk TMPDIR; private short-root seam keeps the real bind inside the writable envelope. |
| `test/oracles/test_gate_slot.py:473` | change | Real root or budget fixture now derives from disk TMPDIR; private short-root seam keeps the real bind inside the writable envelope. |
| `test/suite_concurrency_battery.sh:25` | keep | Explicitly retained env-or-default: SUITE_BATTERY_FX at line 25; inherited TMPDIR wins at line 29. |
| `test/suite_concurrency_battery.sh:29` | keep | Explicitly retained env-or-default: SUITE_BATTERY_FX at line 25; inherited TMPDIR wins at line 29. |
| `test/tmp_leak_ratchet_test.sh:457` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/tmp_leak_ratchet_test.sh:458` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/tmp_leak_ratchet_test.sh:459` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |
| `test/tmp_leak_ratchet_test.sh:460` | keep | Pure policy/expectation, mock receipt, or source-pattern fixture; string is compared/validated, not used as a writable root. |

The 120-byte witness originally failed while binding under `/tmp/csg-<uid>`, not while creating its long TMPDIR. The Landlock red log confirms this separately from the two `/var/tmp/forge` mkdir failures. Its pure short-root selection remains tested; the boundary witness substitutes a private short root and still reaches kernel bind, occupied-slot refusal, mode checks, and cleanup. Byte arithmetic uses UTF-8 encoded lengths.
