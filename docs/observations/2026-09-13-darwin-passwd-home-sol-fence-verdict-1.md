GO-WITH-FIX

CLASS: F1 — OS-backed passwd-home resolver outcomes; the census-tracked production-seam matrix enumerates load timing, lookup ordering, platform gating, missing/present binaries, nonzero exits, malformed output, and path/username shapes.

The candidate fixed macOS boot but left four issues:

- `policy-default` still resolved passwd data during namespace loading.
- `safe-sh` swallowed every `IOException`, including permission and stream failures.
- `parse-dscl-home` accepted any colon-delimited first line.
- The parser-only test did not exercise missing `getent`, fallback ordering, or Linux behavior.

The uncommitted repair makes policy resolution lazy, preserves real I/O failures, reports sanitized attempt statuses, requires the exact `NFSHomeDirectory` attribute, and proves `/etc/passwd → getent → dscl` ordering. Linux never attempts `dscl`. Diagnostics contain no username, home, stdout, or stderr.

Residual: CLI launcher initialization still resolves passwd home once when first needed; namespace loading no longer performs or duplicates that work. This review does not establish a 20 ms Darwin startup claim.

Verification:

- Focused JVM runner: 5 tests, 37 assertions; 0 failures, 0 errors; 0 isolation violations.
- `~/bin/clj-kondo --lint …`: 0 errors, 0 warnings.
- `bb -cp src -m clj-surgeon.core :op :help`: exit 0.
- `git diff --check HEAD`: clean.
- `make test` was not run, as requested.

The patch remains uncommitted and requires the independent second reviewer mandated for source changes.

<!-- SHIP-FIX-BLOCK
CANDIDATE: d05bd721b467c2261e35c6552e0cd9b3209a0051
FINDINGS: F1
REPAIR: Make passwd-home lazy, preserve real I/O failures, validate dscl output, report sanitized attempts, and census the cross-platform resolver matrix.
PRODUCER: Codex <forge-anvil@anvil> model=gpt-5 session=sol-darwin-passwd-home-r1
PATCH-MANIFEST:
  src/clj_surgeon/receipt_artifacts.clj +52 -20
  test/clj_surgeon/deftest_census.edn +2 -0
  test/clj_surgeon/receipt_artifacts_boundary_test.clj +98 -0
SHIP-FIX-BLOCK -->

> END RECEIPT (fence-run): worktree HEAD at review exit = d05bd721b467c2261e35c6552e0cd9b3209a0051 = fenced sha.
