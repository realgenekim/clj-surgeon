## 1. Verdict

**LAND YES.** No release-authority counterexample found.

Review base was pinned to `origin/MCP/main` at `fe8d8b20`; it advanced during review through records-only commits, with no change to the reviewed implementation delta.

## 2. Production entrance

Both worktrees used the identical ledger blob (`3fa1c436`, SHA-256 `d5b166…a728`).

- Preserved failed trunk tree `41981e5a`: `make battery-fresh` refused at **55 commits**, ceiling 30.
- Tip `846c8631`: accepted and displayed:
  `{:commits-behind 14, :raw-commits-behind 22, :ignored-archive-commits 8}`.
- Candidate implementation evaluated against the preserved 55-commit history produced the intended audit: **55 raw, 33 ignored, 22 counted**, accepted.

The tip’s own raw count is 22 because its branch diverges before most record commits; this does not affect the 55/33/22 counterfactual result.

## 3. Adversarial history

A real synthetic Git DAG confirmed:

- Pure archive commit: raw 2, counted 1.
- Archive + `test/` commit: counted increased to 2.
- Empty-diff commit: counted increased to 3.
- Merge whose first-parent diff was archive-only but second-parent diff contained `src/`: merge counted.
- Failed diff inspection at the ceiling: control was counted 30 and accepted; injected diff failure became counted 31 and **REFUSED**.

The exact assertions are in [battery_ledger_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_ledger_test.clj:159):

- Empty diff: merge-parent row `["" 0 false]`.
- Failed diff: `[good 1 false]`.
- Second-parent source: `src/x.clj`, exit 0, expected false.
- Mixed archive/source commit: concatenated archive and `src/x.clj` raw records, asserted non-exempt.

Thus none passed silently.

## 4. Authority boundaries and enrollment

[battery_ledger.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_ledger.clj:206) contains exactly three literal exemptions:

- `docs/observations/2026-09-03-captains-log-anvil-seat.md`
- `docs/observations/2026-09-05-captains-log-astra-four-hour-comparison.md`
- `docs/observations/2026-09-06-live-astra-typist-commentary.md`

An executed classifier probe returned false for `src/`, `test/`, `bin/`, `config/`, `Makefile`, and `deps.edn`, including nested paths. Only existing `100644 → 100644`, status `M`, exact-path records qualify.

The Makefile is unchanged. The ceiling remains 30; execution confirmed 30 passes and 31 refuses. Enrollment pins are **14 battery-ledger, 435 adopted, 1355 total** in [lane_manifest_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:438).

## 5. Verification

- Focused enrollment run: **39 tests, 188 assertions, 0 failures/errors**.
- Complete fast lane: **523 tests, 5,004 assertions, 0 failures/errors**, zero isolation violations.
- `~/bin/clj-kondo`: **0 errors, 1 warning**. The warning at [lane_manifest_test.clj:211](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:211) reproduces unchanged on trunk.
- `git diff --check`: clean.
- Review worktree: clean.

The working-tree clj-surgeon routing guided the review toward native delta inspection and the actual Make production entrance.