Add an internal failure-atomic commit substrate for already compiled multi-file transactions. The
compiled value must retain exact original sources for recovery, while the public plan must not
expose original or future full source.

The commit protocol must:

- accept injected source I/O, with ordinary filesystem defaults using atomic writes;
- preflight every changed file by source hash before performing any write;
- recheck immediately before each replacement and verify every write by read-back hash;
- return a verified success receipt for the complete transaction;
- on failure, restore files only when their bytes still equal the original or transaction result;
- never overwrite an unknown concurrent third state;
- distinguish complete rollback from manual-recovery-required partial state; and
- report the original failure type plus per-file recovery status.

Add focused in-memory tests for successful two-file commit, stale preflight with zero writes,
second-write failure and exact rollback, concurrent source preservation, read-back corruption, and
rollback failure. Update the existing Captain's Log with the proven failure matrix and filesystem
dogfood result. Keep mutation internal; do not publish a new CLI operation in this change.

Only modify:

- `docs/observations/2026-08-06-captains-log-from-microscope-to-intent-transaction.md`
- `src/clj_surgeon/intent_transaction.clj`
- `test/clj_surgeon/intent_transaction_test.clj`

Run the focused `clj-surgeon.intent-transaction-test` namespace. Do not run the unrelated full
repository suite in the foreground; the harness owns broader verification. Stop when the requested
behavior is verified.
