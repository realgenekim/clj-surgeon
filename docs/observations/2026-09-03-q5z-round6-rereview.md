# q5z-alias-migration 9d72bcf — Sol executed round-6 re-check: NO-GO (post-create window; concurrent creates share claims; receipt-dir inside .git committed) — round 7 launched

The first round-five hole is closed; the second is only partial. Two new blocking witnesses remain, and the receipt-directory deviation is insufficient because it permits writes into Git metadata.

Gates under `suite-run`:

- `mcp-test`: 435 tests, 4914 assertions, 0 failures/errors
- `test-fast`: 734 tests, 6257 assertions, 0 failures/errors
- MCP operation oracle: pass
- Four final round-six witnesses: 4 tests, 17 assertions, 0 failures/errors
- Reverted `:run-id` rule: witness failed with 2 assertions, confirming it is genuinely red

Checkout remains clean at `9d72bcf`; no persistent server was started.

The state-root restriction itself would be too broad because externally configured receipt directories are legitimate. But “canonicalizable and outside the details subtree” is not enough: a targeted refusal for workspace control directories—at least canonical `.git` containment—is required.

## NO-GO — mayor’s merge queue

1. **CLOSED** — [mcp_alias_migration.clj:858](src/clj_surgeon/mcp_alias_migration.clj:858), [mcp_alias_migration_test.clj:1565](test/clj_surgeon/mcp_alias_migration_test.clj:1565). Witness: `missing/../.clj-surgeon/alias-migration` returned `alias-migration-receipt-detail-collision` and created no directory.

2. **CLOSED** — [mcp_alias_migration.clj:901](src/clj_surgeon/mcp_alias_migration.clj:901), [mcp_alias_migration_test.clj:1592](test/clj_surgeon/mcp_alias_migration_test.clj:1592). Witness: a remainder climbing above its nearest existing ancestor returned `alias-migration-receipt-dir-escapes` and created nothing.

3. **PARTIAL — BLOCKING** — [mcp_alias_migration.clj:1306](src/clj_surgeon/mcp_alias_migration.clj:1306), [mcp_alias_migration.clj:1343](src/clj_surgeon/mcp_alias_migration.clj:1343). Witness: the requested precheck-to-mkdir race now refuses at `phase="post-create"` with an empty details directory, but installing the symlink immediately after the second containment check produced `ok=true` and an undo receipt canonically inside the details directory.

4. **OPEN — BLOCKING** — [mcp_alias_migration.clj:944](src/clj_surgeon/mcp_alias_migration.clj:944). Witness: two simultaneous `create-receipt-directory!` calls both recorded the same 80 directories as their own; a peer file prevented deletion, but once empty, one caller’s cleanup removed directories also claimed by the peer.

5. **CLOSED** — [mcp_alias_migration.clj:962](src/clj_surgeon/mcp_alias_migration.clj:962), [mcp_alias_migration.clj:1307](src/clj_surgeon/mcp_alias_migration.clj:1307). Witness: an existing symlink-to-directory remained legal and the collision predicate was invoked twice, confirming the `isDirectory` shortcut does not skip post-create re-proof.

6. **OPEN — BLOCKING** — [mcp_alias_migration.clj:901](src/clj_surgeon/mcp_alias_migration.clj:901), [mcp_alias_migration.clj:1343](src/clj_surgeon/mcp_alias_migration.clj:1343). Witness: an existing symlink component pointing outside was canonicalized and accepted (`receipt-dir-escapes?=false`); more seriously, `receipt-dir=.git/refs/heads` committed successfully and made `git show-ref` fail 128 with a bad ref.

7. **CLOSED** — [mcp_alias_migration.clj:806](src/clj_surgeon/mcp_alias_migration.clj:806), [mcp_alias_migration_test.clj:1652](test/clj_surgeon/mcp_alias_migration_test.clj:1652). Witness: the corrected two-pruning-run test passes now and fails twice when ownership is dynamically reverted to marker-only.