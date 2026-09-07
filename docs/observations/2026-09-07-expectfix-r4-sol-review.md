# NO-GO for landing `f2bee388` on `MCP/main`

The round-3 blocking count defect is fixed, but the required lint gate exits nonzero and the branch now conflicts with current `origin/MCP/main`.

| Item | Verdict | Executed result |
|---|---|---|
| Multi-match program | **NOT-REPRODUCED** | `{2,3,2}` refused before mutation with derived `changes=3`; `{3,3,2}` committed. Persisted receipt reported `intent-count=3`, `match-count=3`, `changed-file-count=2`. |
| One-match program + edit | **NOT-REPRODUCED** | One-short `{1,1,2}` refused with derived `{2,2,2}`, unchanged bytes, no receipt. `{2,2,2}` committed and both public and persisted counts matched. |
| Programs + `delete_owners` | **CONFIRMED-DEFECT (pre-existing, inb-0e8dc5)** | Matching `{2,2,2}` was schema-admitted but returned `stale-subform`; unchanged bytes, no receipt. Adding an edit with matching `{3,3,3}` produced the same result. |
| Remaining committed-count escape | **NOT-REPRODUCED** | Successful program, direct multi-file, multi-owner deletion, and hybrid-create transactions all produced guarded counts equal to public and persisted counts. A declared two-file change that matched only one file refused `transaction-expectation-mismatch`, preventing a mismatched receipt. |
| Omitted-root `next_call` | **NOT-REPRODUCED** | JSON round-trip equal; root absent and exactly one `expect` key. |
| Explicit-root `next_call` | **NOT-REPRODUCED** | JSON round-trip equal; exact root preserved and exactly one `expect` key. |
| Programs-only schema | **NOT-REPRODUCED** | Public admission denied the request with `public-schema-denied`. The new schema constraint and program description are present in [mcp_schema.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:335). |
| Programs-only denial text | **JUDGMENT** | The caller sees only “The invoked public tool does not authorize this request”; it does not name `changes` or the required companion gesture. Safe, but weak remediation. |
| Empty-changes guard bypass | **JUDGMENT** | Still present at [mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:1065), but programs-only is now denied at the public boundary. Direct internal invocation still reaches the older `non-empty-array ["changes"]` refusal. |
| EARS wording | **JUDGMENT** | [MCP-OP-EDIT-042](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:69) still calls `programs` a route admitted with and without `expect`, while the implemented intent says programs are not a route themselves. Narrowing it to programs combined with edits or deletion would remove the ambiguity. |

## Gates

- `make mcp-test`: **PASS**, exit `0` — 738 tests, 9,076 assertions, zero failures/errors.
- `~/bin/clj-kondo --lint <all 10 changed Clojure files>`: **FAIL**, exit `2` — 0 errors, 1 warning, 1 info.
  - [lane_manifest_test.clj:211](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:211): unused `ctx`.
  - `mcp_tool_test.clj:2309`: informational redundant one-argument `str`.
- Merge-tree against current `origin/MCP/main` (`eaa69c0d`): exit `1`, content conflict in `lane_manifest_test.clj`.
- `git diff --check`: exit `0`.
- Worktree clean; HEAD remained `f2bee388`.

The `clj-surgeon` receipt discipline materially shaped the review: successful public results were checked against persisted EDN receipt values, not inferred from committed bytes alone.

> END RECEIPT (fence-run): worktree HEAD at review exit = f2bee388524945b00af5818539cd8dd3980b4cab = fenced sha.
