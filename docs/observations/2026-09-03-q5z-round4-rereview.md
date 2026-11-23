# q5z-alias-migration 562b214 — Sol executed round-4 re-check: NO-GO (3 remaining; tie-break ruling conceded to the builder) — round 5 launched

## Round-four re-check

| # | Status | Evidence and my re-run |
|---|---|---|
| 1 | **PARTIAL** | [mcp_alias_migration.clj:732](src/clj_surgeon/mcp_alias_migration.clj:732), [mcp_alias_migration.clj:761](src/clj_surgeon/mcp_alias_migration.clj:761): co-located success retained its undo receipt, and produced `detail-dcbec83d-3495-4834-b2e0-13928d032430.edn`. However, pruning treats every direct `detail-*.edn` as owned: a caller-created set fell from 20 files to 19. The collision case returned `alias-migration-receipt-detail-collision`, made zero transaction calls, and left sources unchanged—but created the detail directory before refusing at [mcp_alias_migration.clj:1012](src/clj_surgeon/mcp_alias_migration.clj:1012). |
| 2 | **PARTIAL** | [mcp_alias_migration.clj:1054](src/clj_surgeon/mcp_alias_migration.clj:1054): requested witnesses pass—pre-`commit!` OOM reported `source_unchanged=true`, `mutation_attempted=false`; post-write OOM reported `false`, `true`, `review_receipt`, with a receipt present. But an OOM at the `execute-mcp-change!` entrance reported `source_unchanged=false`, `mutation_attempted=true`, although sources were byte-identical and no receipt existed. The marker still precedes substantial non-write work in [intent_transaction.clj:2683](src/clj_surgeon/intent_transaction.clj:2683). |
| 3 | **CLOSED** | [mcp_alias_migration.clj:273](src/clj_surgeon/mcp_alias_migration.clj:273), [mcp_alias_migration.clj:289](src/clj_surgeon/mcp_alias_migration.clj:289): unreadable-entry run with bound 5 stopped at exactly 6 and returned `alias-migration-walk-too-large`; `postVisitDirectory` is overridden too. |
| 4 | **CLOSED** | [mcp_alias_migration.clj:373](src/clj_surgeon/mcp_alias_migration.clj:373), [mcp_alias_migration.clj:400](src/clj_surgeon/mcp_alias_migration.clj:400): count/byte calls were 316/315 published JSON characters and replayed to commits of two files. The builder’s ranking is correct: the exact counterexample selected `src/wide/largest/fit`, 141 files and 267,900,000 bytes. My earlier depth-first rule would prefer a tiny deep subtree and was wrong; largest first, then deepest, then lexicographic is the useful deterministic rule. |
| 5 | **CLOSED** | [alias_migration.clj:607](src/clj_surgeon/alias_migration.clj:607), [alias_migration.clj:655](src/clj_surgeon/alias_migration.clj:655): two oversized non-requiring files produced expectations `[2 2]`, cumulative exclusions of one then two files, and then committed exactly two requiring files. `expect_files_unchanged_reason` and aggregate `would_select_*` stayed outside `next_call`; exclusions survived JSON replay. |
| 6 | **CLOSED** | [repository_hygiene_gate.sh:25](test/repository_hygiene_gate.sh:25), [repository_hygiene_gate_self_test.sh:67](test/repository_hygiene_gate_self_test.sh:67): the stub where only `ls-files` failed returned exit 1; all hygiene self-test cases passed. |

Additional hunt:

- Collision means textual absolute-path equality, not canonical identity. Same directory returned true; a nested directory returned false, which is safe because pruning is non-recursive; a symlink to the same directory returned false even though both canonical paths were equal.
- A 60,000-file same-directory aggregate produced only three prefix-map entries. More importantly, an actual 60,000-entry scan refuses at 50,001 before reaching the fold. A symlinked directory selected no foreign files. No aggregate-fold defect found.
- Checkout remains clean at `562b214af84cfce341bdecf227831058134b1325`; no persistent server was started.

Gates under `suite-run`:

```text
Ran 734 tests containing 6257 assertions.
0 failures, 0 errors.

Ran 426 tests containing 4848 assertions.
0 failures, 0 errors.

mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```

## NO-GO — mayor’s merge queue

1. [mcp_alias_migration.clj:732](src/clj_surgeon/mcp_alias_migration.clj:732) — prefix matching is not proof of ownership; witness: a caller-owned `detail-*.edn` file was pruned.

2. [mcp_alias_migration.clj:1012](src/clj_surgeon/mcp_alias_migration.clj:1012) — the collision refusal occurs after `mkdirs`, and [line 757](src/clj_surgeon/mcp_alias_migration.clj:757) misses symlink-equivalent directories; witness: refusal created the directory and the symlinked-same-directory predicate returned false.

3. [mcp_alias_migration.clj:1054](src/clj_surgeon/mcp_alias_migration.clj:1054) — `attempted` is still set before the transaction’s actual write boundary; witness: transaction-entry OOM reported mutation attempted while sources stayed unchanged and no receipt existed.