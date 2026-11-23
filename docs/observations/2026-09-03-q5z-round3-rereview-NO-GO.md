# q5z-alias-migration ca677bc — Sol executed round-3 re-check: NO-GO (6 items; round 4 launched)

## Round-three verdict

Round three is **NO-GO**. The headline probes mostly pass, but the round introduced one merge-blocking receipt-loss bug and several contract gaps.

| Spec | Status | Re-run witness |
|---|---|---|
| MCP-OP-ALIAS-046 | CLOSED | [mcp_alias_migration.clj:338](src/clj_surgeon/mcp_alias_migration.clj:338): 450 × 1,900,000 B returned `scope_bytes 855000000`, `scanned_files 450`, `source_unchanged true`; instrumented `slurp_calls 0` at `-Xmx512m`. |
| MCP-OP-ALIAS-047 | PARTIAL | [mcp_alias_migration.clj:970](src/clj_surgeon/mcp_alias_migration.clj:970): post-write OOM correctly reported `source_unchanged false`, but an OOM thrown immediately on entering `commit!`, before the kernel or any write, also reported false with `changed_files []`; the flag is set too early. |
| MCP-OP-ALIAS-048 | CLOSED | [mcp_alias_migration.clj:187](src/clj_surgeon/mcp_alias_migration.clj:187): a 65-segment source returned `scope-too-deep`, depth 65, bound 64, naming the exact path. |
| MCP-OP-ALIAS-049 | CLOSED | [mcp_alias_migration.clj:187](src/clj_surgeon/mcp_alias_migration.clj:187): `chmod 000 src/locked` returned `scope-unreadable`, paths `["src/locked"]`, count 1—never `expect-mismatch`. |
| MCP-OP-ALIAS-050 | PARTIAL | [mcp_alias_migration.clj:187](src/clj_surgeon/mcp_alias_migration.clj:187): 60,000 ordinary non-source files stopped at 50,001 with zero relative-path constructions, but 50,010 unreadable entries reached 50,013 because `visitFileFailed` always returns `CONTINUE`. |
| MCP-OP-ALIAS-051 | PARTIAL | [mcp_alias_migration.clj:370](src/clj_surgeon/mcp_alias_migration.clj:370): normalized `source-too-large` and `scope-path-refused` replays both committed 12 files with zero byte mismatches; aggregate refusals still incorrectly have nil `next_call`. |
| MCP-OP-ALIAS-052 | PARTIAL / blocker | [mcp_alias_migration.clj:612](src/clj_surgeon/mcp_alias_migration.clj:612): best-effort detail disclosure is honest, but a valid `receipt-dir` co-located with the detail directory caused a successful run’s own `undo_receipt` to be pruned while `ok=true` and `committed=true`. |
| MCP-OP-ALIAS-053 | PARTIAL | [repository_hygiene_gate.sh:14](test/repository_hygiene_gate.sh:14): clean/absent-git/outside-repo/forced-depth-3 exits were `0/1/1/1`; however, line 21 swallows `git ls-files` failure, and a stub where `rev-parse` and `check-ignore` succeed but `ls-files` cannot answer produced exit 0. |

## Introduced-edge rulings

- Aggregate `next_call`: the builder’s impossibility argument is false. A 296-character call narrowing to `src/wide/largest/fit/**` selected 141 files and 267,900,000 bytes, under 268,435,456. The analogous count-ceiling call was 288 characters and selected exactly 2,000 files. This also conflicts with [MCP-OP-ALIAS-015](docs/intent/alias-migration/alias-migration-specs.md:40).

- Exclusion union: two oversized non-requiring files drove `expect.files` `2 → 1 → 0`, although the true requiring count remained 2. Final discovery safely returned `expect-mismatch`; it did not commit incompletely. The union preserves only prior and refusal-named exclusions, but the assumption that every refused source was counted is unsound.

- OOM after a write: it cannot report `source_unchanged true`; the witness returned false and `review_receipt`. The inverse defect is present: pre-kernel OOM also returns false.

- Symlink/depth: `FOLLOW_LINKS` is disabled, so a bare link loop terminated with `{:ok true :files []}`. A large tree containing a loop still hit 50,001. `Integer/MAX_VALUE` is therefore not opening link recursion, but the failed-entry callback bypasses immediate termination.

- Suffix ordering: correct. A source inside `target/`, a directory named `name.clj`, and `upper.CLJ` were omitted; only `src/lower.clj` was selected. Lower-case suffix matching is consistent with the declared suffix set at [mcp_alias_migration.clj:151](src/clj_surgeon/mcp_alias_migration.clj:151).

- Retention: disproved. With `receipt-dir=.clj-surgeon/alias-migration`, the successful receipt named an undo file that no longer existed immediately after `write-details!`.

- Hygiene: requested ordinary cases pass, including `git add -f a/b/.cpcache/x`; full fail-closed behavior does not.

Verification: 419 tests / 4,712 assertions ran with the single known baseline failure at [mcp_change_buffer_test.clj:686](test/clj_surgeon/mcp_change_buffer_test.clj:686). The checkout remains clean at `ca677bc`; all JVMs were sequential and capped at 512 MiB, and no forbidden port was used.

## NO-GO — required before the mayor’s merge queue

1. [mcp_alias_migration.clj:612](src/clj_surgeon/mcp_alias_migration.clj:612) — restrict pruning to identifiable detail documents or reject receipt/detail directory co-location; witness: committed receipt’s `undo_receipt` existed=false.

2. [mcp_alias_migration.clj:970](src/clj_surgeon/mcp_alias_migration.clj:970) — set the OOM state marker at the actual transaction-kernel entrance; witness: pre-kernel OOM reported mutation attempted with zero changed files.

3. [mcp_alias_migration.clj:187](src/clj_surgeon/mcp_alias_migration.clj:187) — return `TERMINATE` from `visitFileFailed` once over-bound; witness: 50,013 visited instead of 50,001.

4. [alias-migration-specs.md:98](docs/intent/alias-migration/alias-migration-specs.md:98) — replace nil aggregate remedies with constant-size prefix narrowing; witness: 141-file/267,900,000-byte and 2,000-file calls fit in under 300 characters.

5. [alias_migration.clj:623](src/clj_surgeon/alias_migration.clj:623) — stop decrementing `expect.files` for unread sources whose requiring status is unknown; witness: true count 2 was reduced to 0.

6. [repository_hygiene_gate.sh:21](test/repository_hygiene_gate.sh:21) — check `git ls-files` independently before filtering; witness: failed inventory command was converted into a green empty view.