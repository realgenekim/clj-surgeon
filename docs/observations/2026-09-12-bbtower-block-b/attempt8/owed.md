# Owed

- Shared CLI output contract: `core/execute-change-with-receipt!` merges
  `receipt-artifacts/workspace-evidence`, whose
  `workspace_status.unexpected_paths` enumerates every dirty/untracked path.
  It is not bounded relative to the inverse receipt. A private GIT_INDEX_FILE
  loaded with fd00e46b (before attempt7 was committed) reproduces this on the
  current CLI without changing the real index: bb and JVM each print 26,581
  characters against a 2,876-character receipt. Clean-index comparison prints
  1,279 on both. Raw stdouts differ only in the generated receipt-path UUID;
  normalized diffs are empty. No bb-only leak exists to fix under step 3.
  A deterministic raw stdout would also require changing the receipt identity
  contract. The authorized contract escape assigns intent-transaction-test
  JVM: paired namespace walls are 4,905 ms bb / 4,709 ms JVM, both 69 tests /
  700 assertions green. This assignment is containment under the brief, not
  a claim that JVM fixes unbounded workspace reporting. A shared bounded
  workspace-evidence design and its dirty-worktree regression remain owed.
- Fable review and ratification of the NEW bb ceiling, independent acceptance.
- Encounter scoring: `~/bin/measure encounter score receipt-booleans bbtower-blockB-attempt8`
  refused `encounter-not-found receipt-booleans`. The installed seed records
  the class; the active ledger lacks its registration. Restore/identify that
  registration and a properly registered later acceptance before scoring
  transfer. No seed import, replacement registration or acceptance was invented.
  Evidence: encounter-score.log.

Evidence: cli-clean/cli-comparison.edn, cli-comparison.edn,
cli-historical-stdout.diff, cli-historical-normalized.diff,
intent-bb.log and intent-jvm.log. Historical attempt7 recorded 11,806 versus
3,674; those exact bytes were not retained there, so that exact old stdout
cannot be reconstructed as a byte-for-byte receipt.
