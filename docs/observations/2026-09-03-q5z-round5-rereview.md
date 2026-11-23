# q5z-alias-migration 50098e6 — Sol executed round-5 re-check: NO-GO (round-4 items closed; canonical-path collision bypass: unnormalised `..` remainder, symlink race after the check) — round 6 launched

NO-GO for the mayor’s merge queue. The three round-four items are closed, but round five introduced a canonical-path collision bypass.

### Round-four items

1. **CLOSED** — [mcp_alias_migration.clj:795](src/clj_surgeon/mcp_alias_migration.clj:795), [mcp_alias_migration.clj:913](src/clj_surgeon/mcp_alias_migration.clj:913).  
   Re-run: all 20 caller-created `detail-*.edn` files survived; the writer pruned one of its own 21 documents. A manifest-recorded file stripped of `:writer` survived, while the next-oldest owned file was pruned.

2. **CLOSED** — [mcp_alias_migration.clj:1169](src/clj_surgeon/mcp_alias_migration.clj:1169), before `mkdirs` at [line 1193](src/clj_surgeon/mcp_alias_migration.clj:1193).  
   Re-run: forced collision returned `alias-migration-receipt-detail-collision` without creating the directory; an already-existing symlink twin also refused.

3. **CLOSED** — [intent_transaction.clj:2249](src/clj_surgeon/intent_transaction.clj:2249), [mcp_alias_migration.clj:1216](src/clj_surgeon/mcp_alias_migration.clj:1216).  
   Re-run: transaction-entrance OOM produced `[source_unchanged=true, mutation_attempted=false, correct_request, 0 receipts]`; post-write OOM produced `[false, true, review_receipt, 1 receipt]`. The six focused regression tests passed 107 assertions.

### Round-five hunt

- Dynamic binding: direct execution and MCP `next_call` replay both reached `commit-compiled!` with the hook bound at [intent_transaction.clj:2782](src/clj_surgeon/intent_transaction.clj:2782); the ordinary MCP handler converges on the same entrance at [mcp_tool.clj:1313](src/clj_surgeon/mcp_tool.clj:1313). It was unbound after success and OOM. A direct reentrant `commit-compiled!` does inherit the outer hook—my nested witness fired it twice—whereas a nested `execute-change!` rebinds its omitted hook to nil. Rollback/undo commits at [intent_transaction.clj:2836](src/clj_surgeon/intent_transaction.clj:2836) are outside the binding, which is harmless after the forward boundary has latched the marker.

- Manifest race: two deliberately interleaved writers left both newly published documents on disk, but last-writer-wins manifest publication lost one entry: disk count 21, manifest count 20. This confirms the amended best-effort contract; it leaks rather than wrongly deletes.

- Corrupt manifest: truncated EDN is swallowed as an empty manifest at [mcp_alias_migration.clj:806](src/clj_surgeon/mcp_alias_migration.clj:806). The next run succeeds, deletes nothing old, and rewrites a one-entry manifest; witness was 21 files on disk and one recorded. Safe degradation, though the bound is lost.

- Forged marker: marker alone was insufficient and survived. Manifest membership plus a forged marker was pruned even with a deliberately mismatched `:run-id`. I do not treat deliberate manifest-and-marker impersonation as a blocker under the present cooperative local-filesystem contract; matching run-id would be cheap defense-in-depth but still would not stop deliberate forgery. A stored digest would better protect accidental replacement.

- Premise correction: with `prune-details!` rebound to a no-op, the two corrected tests at [mcp_alias_migration_test.clj:1306](test/clj_surgeon/mcp_alias_migration_test.clj:1306) and [line 1346](test/clj_surgeon/mcp_alias_migration_test.clj:1346) produced three failed assertions. They are not vacuous.

Gates:

```text
mcp-test: 431 tests, 4897 assertions, 0 failures/errors
test-fast: 734 tests, 6257 assertions, 0 failures/errors
mcp-operation oracle: pass
```

Checkout is clean at `50098e6`; no persistent server was started.

## NO-GO — mayor’s merge queue

1. [mcp_alias_migration.clj:859](src/clj_surgeon/mcp_alias_migration.clj:859) — the nearest-ancestor remainder is appended without normalization; witness: `missing/../.clj-surgeon/alias-migration` passed the guard and committed a receipt whose canonical parent was the detail directory.

2. [mcp_alias_migration.clj:880](src/clj_surgeon/mcp_alias_migration.clj:880) — canonical identity is checked before later path creation and is vulnerable to a missing-component symlink race; witness: installing the symlink after the false check produced `ok=true` with the receipt canonically inside the detail directory.