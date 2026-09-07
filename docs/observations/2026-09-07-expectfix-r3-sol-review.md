# NO-GO for landing `a917b5a9` on `MCP/main`

One blocking aggregate-guard defect remains.

## Blocking defect

**CONFIRMED-DEFECT — multi-match programs undercount `changes`.**

Executed request:

- One literal edit with one match.
- One program with `expect.matches=2`.
- Aggregate `expect={changes:2, edits:3, files:2}`.

The guard accepted it and committed both files, but returned:

```text
declared:  changes=2 edits=3 files=2
result:    changes=3 edits=3 files=2
receipt:   intent-count=3 match-count=3 changed-file-count=2
```

The cause is [mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:1025), which adds `(count programs)` to `changes`. Programs are subsequently flattened into one addressed intent per concrete match in [mcp_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:557), and the transaction receipt counts those intents in [intent_transaction.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/intent_transaction.clj:1997).

Thus the new LLD assertion that guarded and committed receipt counts always agree is false at [mcp-operation-contract-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-design.md:569). The new regression uses only `expect.matches=1`, so it misses this case.

## Probe classifications

| Probe | Classification | Executed result |
|---|---|---|
| Mixed edit+program, one-short expect | **NOT-REPRODUCED** | Correctly refused `expect-mismatch`, derived `{2,2,1}`, `mutation_attempted=false`, bytes unchanged, no receipt written. |
| Mixed edit+program, matching expect | **NOT-REPRODUCED** | `{2,2,1}` committed; public result and persisted receipt both reported `{2,2,1}`. |
| Programs + `delete_owners` + edits | **CONFIRMED-DEFECT (pre-existing)** | Valid `{3,3,3}` request refused `stale-subform` before writing. `programs + delete_owners` also failed without aggregate `expect` on historical `f3d922ac`. This is not introduced by Round 3, but the advertised combination remains unusable. |
| Programs-only with wrong expect | **JUDGMENT** | Caller sees `non-empty-array` at `["changes"]`; no `next_call`, receipt, or mutation. Historical execution on `f3d922ac` confirms programs-only was already unexecutable without `expect`. Standing aside is safe for mutation authority, although the schema/runtime incoherence remains: the schema admits programs-only at [mcp_schema.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:544), while validation requires non-empty changes at [mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:730). |
| Omitted-root `next_call` | **NOT-REPRODUCED** | JSON round-trip equality passed; root absent on both sides and exactly one `expect` key. |
| Explicit-root `next_call` | **NOT-REPRODUCED** | JSON round-trip equality passed; identical root preserved and exactly one `expect` key. |
| Zero declared `changes` | **NOT-REPRODUCED** | Runtime witness passed: `positive-integer` at `["expect","changes"]`, source unchanged. |
| Remaining committed-count escape | **CONFIRMED-DEFECT** | The multi-match program probe above commits `changes=3` after the guard authorized `changes=2`. |

Additional judgment: a mixed edit+creation request with `expect={1,1,1}` commits two filesystem effects and returns `files=1, created=1` with two read-back hashes. This is internally consistent only if aggregate `expect` explicitly excludes creations; the schema wording “every transformation” should be narrowed because create-only is not the only literal-effect exception.

## Gates

- `make mcp-test`: **PASS** — 736 tests, 9,062 assertions, zero failures/errors; all Makefile self-checks passed.
- `~/bin/clj-kondo --lint <all branch-changed Clojure files>`: 0 errors, 1 warning, 1 info; exit 2. Both findings predate this branch:
  - `lane_manifest_test.clj:211`: unused `ctx`.
  - `mcp_tool_test.clj:2309`: redundant single-argument `str`.
- Merge-tree simulation against current `origin/MCP/main` (`87ce35a0`): clean.
- Worktree: clean.

The blocking repair is to make a program’s `changes` contribution agree with the concrete addressed intents the receipt publishes—currently the sum of program match counts—and add a multi-match aggregate-guard witness.

Relevant skill: `clj-surgeon`’s working-tree guidance shaped the review by requiring persisted receipt values, not successful write/read-back alone, as the count evidence.

> END RECEIPT (fence-run): worktree HEAD at review exit = a917b5a9e41fc25786c06353123234c6096118f4 = fenced sha.
