# bridge/ci-findings — checkpoint under the capacity order (2026-09-04 22:49 UTC, forge@anvil)

Halted mid-programme by a capacity order (Astra leads the box; Gene authorized):
finish the gate in flight, push, stop. No further suite launches until told to resume.

## Tip

`1086ad80804288fce5a22e31487dbfeecbe7f364`, pushed, working tree clean.
Trunk at checkpoint: `e57a85d21781474e875c4793840624e33a8d153b`.
`git merge-tree --write-tree HEAD origin/MCP/main` = `76dd979f4a9387df4c47e0542e4a7e6c1209744d`,
exit 0 — no conflict, and no file this branch touches has moved on trunk.

## Green

- `~/bin/suite-run clojure -M:clj-surgeon/mcp-test`, on a FRESH clone at the tip:
  `Ran 921 tests containing 15234 assertions.` / `0 failures, 0 errors.`
  / `1 preconditions skipped.` / `0 preconditions failed.`
- `make mcp-operation-oracle`:
  `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]`
- intent audit (`bash test/performance_regression_sentinel_intent_test.sh`):
  `Sentinel intent witness audit passed: 49/49 requirements named`
- `sh test/clj_kondo_admission_path_test.sh`: `clj-kondo admission path regression passed`

## Remaining, in order, when the box frees

1. `~/bin/suite-run bb test/run_all.clj`
2. the two subject namespaces alone (this trunk's runner has NO `--ns` flag —
   `-M:alias -e` does not override `:main-opts` either, so both invocations run the
   whole suite; a per-namespace run needs `clojure -M -e` with an explicit classpath,
   or the lane-manifest runner that lives on `bridge/suite-spike`)
3. sabotage: revert item 1's PATH walk in the prepared archive copy at
   `/var/tmp/forge/ci-fx2/sabotage` (already reverted, never run) and count the RED
4. re-run the intent audit and oracle at the final tip

## Fixtures deliberately NOT removed

`/var/tmp/forge/ci-fx2/` is kept so the remaining gates can resume without a re-clone:
`clone` (tip at 2648fcae), `clone2` (tip at 1086ad80), `sabotage` (reverted archive copy),
and the gate logs. `/var/tmp/forge/ci-fx` was removed at the start of this run.
Remove `ci-fx2` when the programme finishes.

## The finding this run added

The tip as previously pushed (`2648fcae`) was RED on the full suite and nobody had run it —
the earlier builder verified `mcp-change-buffer-test` alone. See `1086ad80` for the six
failures, the single root cause, and the fix.
