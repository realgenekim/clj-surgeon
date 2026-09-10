# Seat-path defaults landing, stage 1 — Sonnet

Written 2026-09-10T17:40Z. Worktree `/home/forge/src/clj-surgeon-seatpaths`,
branch `fable/seat-path-defaults`, created with `~/bin/worktree-add` from
`stable/2026-09-10`, proved at creation:
HEAD = `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1` = `stable/2026-09-10^{commit}`.

Committed as `ee1727b9d3efaf259f9ca073549bdafaa74c2d58`, author/committer
`forge-anvil <forge-anvil@anvil>` (matches 6f74db0e's original author, and is
this seat's standing `GIT_AUTHOR_*`/`GIT_COMMITTER_*`), trailers
`Co-Authored-By: Gene Kim`, `Co-Authored-By: Claude Fable 5.1`, `Claude-Session`.
**Not pushed.**

## What this is, and is not

Source: `fable/public-candidate` (`/home/forge/src/clj-surgeon-pubcand`, READ
ONLY), commits `6f74db0e` (the defect fix, 87 files) and `e6b9c7e9` (its
regression repair, 32 files). This is **not** a `git cherry-pick` of either
commit — both mix the three defects into a much larger cosmetic seat-name
scrub (docs, comments, bench scripts, dev/experiments, a scanner + allowlist,
a routing-feature removal, a Makefile backtick-recursion fix, a CLAUDE.md
restoration). Cherry-picking either whole commit would have pulled all of
that in, which the brief explicitly excluded ("nothing else… no removals, no
allowlist"). Instead I hand-applied exactly the hunks that implement or
witness the three named defects, verified against both commits' full diffs
file-by-file, and wrote three new witnesses neither candidate commit had.

**14 files changed, 109 insertions, 17 deletions** — 3 src, 10 test, 1 ledger
(`deftest_census.edn`, mechanically regenerated for 2 new deftest names).

## The three defects, before/after, and their witness

### 1. `src/clj_surgeon/receipt_artifacts.clj` — `*artifact-root*`

- **Before:** `(def ^:dynamic *artifact-root* "/var/tmp/forge")` — a literal
  path under this seat's home. Unwritable on any other machine, or on a
  shared box writable by everyone.
- **After:** a new `default-artifact-root` fn: `CLJ_SURGEON_ARTIFACT_ROOT` env
  override, else `$XDG_STATE_HOME/clj-surgeon/artifacts`, else
  `$HOME/.local/state/clj-surgeon/artifacts` (the convention
  `clj-surgeon.mcp-process` already used for pressure status). The existing
  workspace-confinement checks in `directory`/`target` are untouched.
- **Witness — repaired existing sites (8 files, matching e6b9c7e9's own
  finding "eleven witnesses asserted the SPELLING of the artifact root, not
  the contract"):** `admit_patch_test.clj`, `cli_dispatch_test.clj`,
  `extract_test.clj`, `mcp_alias_migration_test.clj` (5 sites),
  `mcp_helper_extraction_test.clj` (2 sites), `mcp_workspace_test.clj`,
  `mission_typist_executor_test.clj`, `receipt_artifacts_boundary_test.clj` —
  each changed from asserting the literal old path to asserting the
  **contract** (`str/includes?` on `/<verb>-receipts/`, or in
  `mcp_workspace_test` an explicit absolute + outside-workspace check).
  Applied directly against the tag text, skipping 6f74db0e's own intermediate
  cosmetic rewrite (`/var/tmp/forge` → `/var/tmp/clj-surgeon-scratch`), which
  is out of scope here.
- **New witness:** `mcp_alias_migration_test/artifact-root-default-is-derived-per-user`
  calls `default-artifact-root` directly and asserts it's not the old literal,
  is absolute, is namespaced under `clj-surgeon`, and (absent an override in
  this environment) resolves under `$HOME`.
- **Correction I made vs. e6b9c7e9's literal text:** three of e6b9c7e9's own
  hunks (cli_dispatch_test's `change-receipts` assertion, and both
  `mcp_alias_migration_test` `retired_to` assertions) kept `str/starts-with?`
  but truncated the literal to just `"/change-receipts/"` /
  `"/alias-migration-receipts/"` — a prefix that can never match an absolute
  path under a derived root (`directory` always prepends the artifact root).
  I used `str/includes?` instead, matching the pattern e6b9c7e9 itself used
  correctly at every other site touching this same contract. Verified: with
  e6b9c7e9's literal text these three assertions would fail against the real
  derived root on this box; with `includes?` they pass (confirmed by the
  green focused run below).

### 2. `src/clj_surgeon/memory_battery_runner.clj` — `MEMBAT_ROOT`

- **Before:** `(env "MEMBAT_ROOT" "/home/forge/tmp/membat")`.
- **After:** `(env "MEMBAT_ROOT" (str (System/getProperty "user.home") "/.local/state/clj-surgeon/memory-battery"))`.
- **Witness:** no existing test hard-coded this literal (none broke). New:
  `memory_battery_test/membat-root-default-is-derived-per-user` — a
  source-text witness (this default is computed inline in `-main`, not a
  separately callable pure fn like #1, and this bb-lane namespace already
  uses the same slurp-and-assert technique for Makefile text) asserting the
  old literal is gone and the new per-user form is present.

### 3. `src/clj_surgeon/mcp_process.clj` — pressure-status default

- **Before:** `"/.local/state/diagnose-skiff-cpu-memory/monitor/status.json"`
  — named after one seat's monitor.
- **After:** `"/.local/state/clj-surgeon/pressure-status.json"`. The
  `CLJ_SURGEON_PRESSURE_STATUS` env override (unchanged) already has an
  existing test (`mcp_process_test.clj:336`), proving override reachability
  continues to work.
- **Witness:** no existing test hard-coded the old default literal. New:
  `mcp_process_test/pressure-status-default-is-derived-per-user` calls the
  private `pressure-status-path` via `#'process/pressure-status-path` with
  `*pressure-status-path*` bound to nil, asserting the old seat-named
  fragment is gone and (absent env override) the new path is used.

## Deliberately excluded from e6b9c7e9 (regressions it repaired that this
cherry-pick does not need)

e6b9c7e9 repaired **three** regressions; only one was introduced by 6f74db0e
itself:

1. Makefile backtick recursion — introduced by a **later** candidate commit
   (`9405a6eb`, the routing-plate removal), which this branch never took.
   Not applicable here.
2. **The eleven artifact-root witnesses — this one applies**, folded in
   above.
3. CLAUDE.md restoration — repairs `9405a6eb`'s CLAUDE.md replacement, also
   never taken here. Not applicable.

Also excluded from e6b9c7e9's "residual hygiene" grab-bag: the
`resources/clj-kondo-admission.py` fix (same pressure-status defect, but in a
file 6f74db0e never touched — a fourth site of the same bug class, not a
regression 6f74db0e introduced; flagging as a follow-up, see below),
`.gitignore`, `LQBM-REPORT.md`/`REPAIR-REPORT.md` removal, the `bridge`
scanner-rule narrowing, and every seat-name-in-a-comment scrub (`Astra` →
"independent review", `anvil` → "the reference host", `SKIFF-INSTALL-FENCE-001`
→ `INSTALL-FENCE-001") that rode along in the same files as real witness
repairs — none of those touch the three named paths.

## Conflicts

None. Every hunk applied cleanly against the tag text (hand-applied, not
`git cherry-pick`, so there was no merge machinery to conflict).

## Gates

- **Focused namespace runs** (before the full suite), via
  `clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns …`
  and direct `clojure.test/run-tests` for two namespaces absent from
  `lane-manifest` (`cli-dispatch-test`, `extract-test` — a pre-existing gap,
  unrelated to this change): **0 failures, 0 errors** across
  `receipt-artifacts-boundary-test`, `admit-patch-test`,
  `mcp-helper-extraction-test`, `mcp-process-test`, `mcp-workspace-test`,
  `mission-typist-executor-test` (276 tests/5862 assertions),
  `cli-dispatch-test` + `extract-test` (60/449), `mcp-alias-migration-test`
  (144/3335), and `bb test/run_all.clj` (893/7914, includes
  `memory-battery-test`).
- One iteration: the first full-gate run correctly refused on
  `lane-manifest-test`'s deftest census — my two new JVM deftests weren't in
  `test/clj_surgeon/deftest_census.edn`. Regenerated per its own printed
  remedy (`CENSUS_REGENERATE=1 clojure …`); diff was exactly the two new
  names, nothing else.
- `~/bin/clj-kondo` over all 13 touched Clojure files: 0 errors. One warning
  I introduced (`clojure.string/includes?` used without a require in
  `mcp_workspace_test.clj`) — fixed by adding `[clojure.string :as str]` and
  using the file's own alias; re-linted clean (0 errors, 0 warnings) and
  re-ran that namespace (0 failures).
- **Full landing gate**, `~/bin/suite-run make test`, run twice (red once on
  the census, green on retry), background PID waited on directly (not
  polled/guessed):

  | stage | exit | wall |
  |---|---:|---:|
  | admit-transaction-recovery-battery | 0 | 13.2 s |
  | battery-fresh | 0 | 2.1 s |
  | alias-migration-test | 0 | 89.5 s |
  | mcp-test | 0 | 116.9 s |
  | test-bb | 0 | 100.5 s |
  | repository-hygiene | 0 | 1.9 s |
  | intent-audit | 0 | 2.0 s |

  `landing-gate: {... :state :passed ... :wall-ms 194229}`, zero
  `FAIL in`/`ERROR in`/`gate-refused` in the log. Log:
  `/var/tmp/forge/plan2/cellC/sonnet-seatpaths-gate2.log`.

## Boundaries observed

No `pkill`/`pgrep -f`. All temp/log output under `/var/tmp/forge`. Never
touched ports 7888/7890/7894/7895/8300-8339. No `-Xmx` set directly (suite-run
and the Makefile's own JVM launches carry their own bounded heaps, unchanged).
Never touched `clj-surgeon-pubcand` or any other builder's worktree; only read
from it via `git show <rev>:<path>` against the shared object store. Did not
push. Total elapsed inside the 45-minute timebox.

## What's left for whoever picks this up next

- **`resources/clj-kondo-admission.py`** carries the same pressure-status
  seat-named default (`~/.local/state/diagnose-skiff-cpu-memory/monitor/status.json`)
  that `src/clj_surgeon/mcp_process.clj` had. 6f74db0e never touched this
  file; e6b9c7e9 fixed it as "residual hygiene… the same defect fixed in
  mcp_process.clj." Out of strict scope for this task (not a regression
  6f74db0e introduced, and not one of the three named paths), but it is the
  same live defect in a fourth location and worth a one-line follow-up.
- **`cli-dispatch-test` and `extract-test` are absent from
  `clj-surgeon.lane-manifest`**, pre-existing and unrelated to this change —
  the lane-gated runner refuses them by name. Surfaced here only because I
  had to route around it to run focused tests; not touched.
