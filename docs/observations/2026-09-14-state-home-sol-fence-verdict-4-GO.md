GO

Reviewed sealed candidate `2091bb4c84549a3bd741a63a80caed133f390d07`; its parents are the stated base and tip.

- Canonical root and final descriptor admission occur before creation. Relative, empty, symlinked, checkout, and outside-envelope cases are covered without widening authority.
- Workspace keys are SHA-256 of the UTF-8 canonical workspace path—not HEAD or repository contents. Distinct canonical paths separate checkouts; moving a checkout changes the key. This matches `STATE-HOME-003`.
- Probe receipts attach the resolved path before the final whole-receipt bound. Local absence, malformed data, permissions, oversize data, and invalid paths are classified before transport.
- Atomic publication preserves the prior descriptor before rename, cleans temporary residue, and reports both primary and cleanup failures. Rename remains the explicit commit boundary.
- The current 240,000 ms namespace allowance is a measured declaration, not the earlier relaxed 1,000,000 ms allowance: the remaining battery namespace measured 116,659–139,833 ms. The independent 1,800,000 ms lane ceiling remains unchanged.
- The real-process witnesses use fresh committed copies, exact warm PIDs, bounded process-group teardown, and TMPDIR-owned fixtures. Inspection found no additional envelope writer.
- Removing `:telemetry_dropped` from cross-runtime equality is correct: the test separately proves JVM value `16` and bb absence before comparing the stable operation contract.
- Registration evidence is concrete: JVM/bb controls name commands, subjects, exits, and counters; the merged census now consistently contains 161 namespaces.

Sealed-tree checks:

```text
targeted bb checks: 2 tests, 636 assertions, 0 failures, 0 errors
clj-kondo: 0 errors, 0 warnings
Python AST: 2 files ok
git diff --check: clean
```

No `make test` was run. No patch was created; the pre-existing untracked review log was preserved.

> END RECEIPT (fence-run): worktree HEAD at review exit = 2091bb4c84549a3bd741a63a80caed133f390d07 = fenced sha.
