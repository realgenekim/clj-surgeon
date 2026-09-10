# Seat-path defaults landing, stage 1 — Sonnet

Written 2026-09-10T17:40Z, updated 2026-09-10T17:52Z (stage 2: rebase onto
moved trunk), updated 2026-09-10T18:38Z (stage 3: fence NO-GO repair,
SPF-001/002/003). Worktree `/home/forge/src/clj-surgeon-seatpaths`, local
branch `fable/seat-path-defaults`, created with `~/bin/worktree-add` from
`stable/2026-09-10`, proved at creation:
HEAD = `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1` = `stable/2026-09-10^{commit}`.

**Current HEAD: `49232f0879e8aef69ce40dca58eb86d81d9366a5`**, pushed to
**`fable/seat-path-defaults-r2`** (new branch, per the coordinator's
instruction — the old ref, local `fable/seat-path-defaults`, was left
untouched and was never pushed). Three commits on top of trunk `13635f74`:

| sha | what |
|---|---|
| `3020950b6e965514e932fecefafa9baf8d42043b` | stage 2: the rebased defect-fix commit (was `ee1727b9d3efaf259f9ca073549bdafaa74c2d58` before the rebase; rebase rewrites the sha even with no textual change, since the parent changed) |
| `bac7ba22b357a100c672de130d0ad6a1c62d66f5` | stage 2 follow-up: drop an unused `clojure.string` require left over from conflict resolution |
| `49232f0879e8aef69ce40dca58eb86d81d9366a5` | stage 3: SPF-001/002/003 fence repair (see below) |

All three commits: author/committer `forge-anvil <forge-anvil@anvil>` (this
seat's standing `GIT_AUTHOR_*`/`GIT_COMMITTER_*`, matching 6f74db0e's
original author), trailers `Co-Authored-By: Gene Kim`, `Co-Authored-By:
Claude Fable 5.1`, `Claude-Session`. **No force-push, no amend** — `git push
origin HEAD:refs/heads/fable/seat-path-defaults-r2` created the new ref;
`fable/seat-path-defaults` still points at `bac7ba22` locally and was never
pushed anywhere.

## Stage 3 — Sol's fence NO-GO on `bac7ba22`, three findings (2026-09-10T18:38Z)

Verdict: `/var/tmp/forge/ship/20260910T175054Z-bac7ba22b357/verdict-1.md`
(sealed candidate `1403bc72`, a ship-created merge onto trunk for review —
the fence ran against that tree, findings apply identically to `bac7ba22`
itself since the merge changed nothing under `src/`/`test/`/`docs/intent`).
Four new stage-1/2 witnesses passed the fence read; `make test` was not
repeated by the fence.

### SPF-001 — `default-artifact-root` selected but never validated

The fence's own probe: with both env vars unset the default was fine
(`/home/forge/.local/state/clj-surgeon/artifacts`), but
`XDG_STATE_HOME=/tmp` produced `/tmp/clj-surgeon/artifacts` (RAM-backed, and
nothing refused it), `CLJ_SURGEON_ARTIFACT_ROOT=/home/someone-else/state` was
accepted outright, and a blank `CLJ_SURGEON_ARTIFACT_ROOT` produced `""`. The
writer's only guard (in `directory`) checked containment inside the *current
workspace*; it never asked whether the root itself was safe.

**Fix — `validate-artifact-root!`** (`src/clj_surgeon/receipt_artifacts.clj`),
called from `directory` before any receipt write. Fails closed, in order:

1. non-blank
2. absolute
3. (after a best-effort `.mkdirs`, so an explicit override under a shared
   sticky-bit base like `/var/tmp/forge` is judged by what it actually
   resolves to rather than by a path-string convention) owned by the
   invoking user — checked by real filesystem ownership
   (`Files/getOwner` on the nearest existing ancestor) — or under `$HOME`
4. real disk, never RAM-backed

Every failure is a typed `ex-info` naming `:error-type` and the offending
`:root`. The fence's explicit non-negotiable — the parity harness's
`CLJ_SURGEON_ARTIFACT_ROOT=/var/tmp/forge`, matching the stable build's own
literal — still passes, because `/var/tmp/forge` is real disk this
invocation owns; a `validate-artifact-root!` call against that exact literal
is one of the six new witnesses.

**A detour that mattered.** The verdict said to "apply the repository's
temp-hygiene base-refusal" (`clj-surgeon.tmp-leak-support/base-refusal`,
`src/clj_surgeon/receipt_artifacts.clj` → require it directly). Tried that
first — it compiled, its own unit checks passed — but requiring a
`test/`-located namespace from `receipt_artifacts.clj` (which
`clj-surgeon.core` loads, so every bb CLI invocation loads it too) broke the
**bb CLI outright**: `bb -m clj-surgeon.core --help` still worked (bb's own
`bb.edn` puts `test/` on its classpath), but `cli_dispatch_test.clj`'s own bb
subprocess invocations — which exercise a narrower, install-shaped classpath
— went from 0 to 115 failures/8 errors, all
`FileNotFoundException: clj_surgeon/tmp_leak_support.clj … on classpath`.
Reverted the require; `validate-artifact-root!` instead carries a small,
explicitly-documented self-contained real-disk check (`ram-backed-path?` —
literal `/tmp`/`/dev/shm`, or `findmnt` reporting `tmpfs`) so every verb
reachable from the bb CLI can depend on it without widening what the
production entrance requires. This is the one place stage 3 diverges from
the verdict's literal suggestion, and it's load-bearing: without the
divergence the CLI does not start.

**A second, unrelated gap the new reachability exposed.**
`mcp_alias_migration_test.clj`'s `refusal-kinds-in-source` scanner (used by
`the-refusal-enumeration-is-pinned-in-count-and-in-membership`) walks every
`clj-surgeon.*` namespace reachable from the alias-migration entrance and
`slurp`s its source file, assuming `src/<path>.clj` unconditionally. Once
`receipt_artifacts.clj` was (transiently, during the detour above) reachable
to a `test/`-located namespace, the scanner crashed the same way the CLI
did. Fixed `namespace-source-path` to check `test/` when `src/` doesn't
exist — general, and correct regardless of the detour's outcome, so kept
even after the require was reverted.

**Witnesses** (all in `mcp_alias_migration_test.clj`): `artifact-root-refuses-blank`,
`artifact-root-refuses-relative`, `artifact-root-refuses-ram-backed`,
`artifact-root-refuses-another-users-path`, `artifact-root-accepts-a-path-under-home`,
`artifact-root-retains-the-controlled-parity-harness-override`, and
`artifact-root-is-validated-before-every-receipt-directory-is-computed`
(binds `*artifact-root*` to a relative path and proves `directory` itself
refuses before computing anything). The entrance's frozen refusal-kind
enumeration is repinned **149 → 153** for the four new kinds
(`artifact-root-blank`/`-relative`/`-ram-backed`/`-not-owned`).

### SPF-002 — the Python admission entrance kept the same live defect

`resources/clj-kondo-admission.py`'s `pressure_status_path` still defaulted
to `~/.local/state/diagnose-skiff-cpu-memory/monitor/status.json`.
Direct-shell mode builds `pressure_status` from `CLJ_SURGEON_PRESSURE_STATUS`
and falls back to this literal *without* going through `mcp_process.clj` —
a live second default for the same admission contract, not dead
duplication. Changed to `~/.local/state/clj-surgeon/pressure-status.json`,
matching stage 1's `mcp_process.clj` fix.

**Witness:** `test/clj_kondo_admission_path_test.sh` gained a check that
imports the module directly (`importlib.util`) and calls
`pressure_status_path(SimpleNamespace(pressure_status=None))` with
`CLJ_SURGEON_PRESSURE_STATUS` unset — the true Python fallback path, which
the file's existing check never exercised (it always passes
`CLJ_SURGEON_PRESSURE_STATUS` explicitly). Asserts the new path is produced
and the old seat-named fragment is gone. Ran standalone: `sh
test/clj_kondo_admission_path_test.sh` → `clj-kondo admission path
regression passed`.

### SPF-003 — the owning intent still specified the removed literal root

`docs/intent/alias-migration/receipt-artifacts-specs.md`
(`ALIAS-MIGRATION-001`) required publication under the literal
`/var/tmp/forge/<verb>-receipts/`, contradicting the derived-per-user
implementation stage 1 shipped. Updated the EARS requirement text to
describe `<artifact-root>` as derived (naming the resolution order) and
validated (naming the four `validate-artifact-root!` checks), before
touching any test or code for this finding (the linked-intent order the
coordinator named). `make intent-audit`: `:ok true`, `ALIAS-MIGRATION-001
:implemented`. Did not touch the other files that echo the same literal
outside the owning intent (`docs/high-level-design.md`,
`docs/plans/row2-receipt-artifacts.md`, several `docs/observations/*` —
records lane, never touched) — SPF-003 named this one file specifically.

## Stage 2 — rebase onto moved trunk (2026-09-10T17:52Z)

Trunk moved from `59d8bc0c` to `13635f74` while this branch sat uncommitted-
then-committed: three merges landed the skiff's macOS gate fixes, including
`0656af1d`/`c196aab1`/`98f7d23b` — a **canonicalized-both-sides** rewrite of
the exact eleven literal-root assertions this branch had also just rewritten,
in the same eight test files. Two independent repairs of the same defect
class, textually colliding.

**Rebase, not merge** (my call): one commit, replaying cleanly onto the new
tip is simpler to read than a merge commit here, and it keeps the branch's
history matching the "cherry-pick style" the stage-1 commit already used.
`git fetch origin && git rebase origin/MCP/main`.

**Trunk's fix is strictly better than mine, and fully compatible with the
stage-1 src change.** Trunk added `clj-surgeon.artifact-boundary-support`
with two functions, both reading the root from
`clj-surgeon.receipt-artifacts/*artifact-root*` (never a literal):
  - `under-root?` — LEXICAL placement, canonical on both sides
    (`getCanonicalFile`), no existence required. For a *computed* path, e.g.
    `workspace/receipt-dir`.
  - `published-under-root?` — a receipt that EXISTS inside the root, BOTH
    sides resolved with `toRealPath`, typed `:artifact-path-unresolvable`
    refusal on a plausible-but-nonexistent path (Sol's SKF-001 finding: the
    original `getCanonicalFile`-only version failed OPEN on a nonexistent
    artifact).
  Because `verb-receipt-root` reads `*artifact-root*` at call time, and my
  stage-1 change made that var's *default* resolve per-user instead of the
  literal `/var/tmp/forge`, trunk's helper picks up the stage-1 fix
  automatically with zero further change — the two commits fix orthogonal
  halves of the same class (trunk: the COMPARISON method; stage 1: the
  DEFAULT ROOT VALUE) and compose without any semantic conflict, only a
  textual one (both rewrote the same lines).

### The eight conflicts, and their resolution

All eight were the exact same shape: `<<<<<<< HEAD` carried trunk's
`boundary/published-under-root?` or `boundary/under-root?` call;
`>>>>>>> ee1727b9` carried my stage-1 `str/includes? … "/<verb>-receipts/"`.
**Resolution: took trunk's (HEAD) side in every case, dropped mine.** Trunk's
version is strictly better (symlink-safe, existence-checked where that's the
right question, typed refusal on failure) and, per the compatibility
argument above, already carries the stage-1 fix through `*artifact-root*` —
keeping my weaker substring check alongside it would have been redundant,
not "both."

| file | conflict(s) | resolution |
|---|---|---|
| `admit_patch_test.clj` | 1 (`lock_path`) | trunk's `published-under-root?` |
| `cli_dispatch_test.clj` | 1 (`change-receipts`) | trunk's `published-under-root?` |
| `extract_test.clj` | 1 (`extract-receipts`) | trunk's `published-under-root?` |
| `mcp_alias_migration_test.clj` | 5 (`details_path`/`undo_receipt`, `retired_to` ×2, `require-change-receipts`, `namespace-split-receipts`) | trunk's `published-under-root?`, each site |
| `mcp_helper_extraction_test.clj` | 2 (`helper-extraction-receipts` ×2) | trunk's `published-under-root?`, each site |
| `mcp_workspace_test.clj` | 1 (`receipt-directories-are-deterministic-and-workspace-isolated`) | trunk's `under-root?` — this call site is a *computed*, unpublished path (`workspace/receipt-dir` writes nothing), which is exactly the distinction `98f7d23b` carved out; `published-under-root?` would have been the wrong function here even though it's the one used everywhere else |
| `mission_typist_executor_test.clj` | 1 (`typist-receipts`) | trunk's `published-under-root?` |
| `receipt_artifacts_boundary_test.clj` | 1 (`assert-artifact!`) | trunk's `published-under-root?`, plus trunk's richer failure message (`str verb " published at " path " -- outside " (boundary/verb-receipt-root verb)`) |

`test/clj_surgeon/deftest_census.edn` and `src/clj_surgeon/memory_battery_runner.clj`
auto-merged with no conflict (trunk's `memory_battery_runner.clj` change —
`clj-surgeon.jvm-error`/`out-of-memory?` for babashka SCI compatibility — sits
in a different part of the file from the stage-1 `MEMBAT_ROOT` line).
`mcp_process_test.clj` and `memory_battery_test.clj` (carrying the other two
new witnesses) weren't touched by trunk at all and auto-merged clean.

**No literal seat path survives as a comparison in any of the eight** — every
one now calls `clj-surgeon.artifact-boundary-support`. What *does* still
contain the substring `/var/tmp/forge` or `/home/forge` in these files (and
is fine, checked individually): comments/docstrings narrating the darwin
symlink history; two `mcp_alias_migration_test.clj` **fixture values** (a
`:details_path` literal used as arbitrary test input, and a workspace_root
in a "no such workspace" negative test — data, not a root comparison, and
explicitly the kind of thing trunk's own new ratchet test
`no-witness-compares-an-artifact-path-against-a-literal-root` asserts must
NOT be flagged); and my own stage-1 witness's negative assertion
`(is (not= "/var/tmp/forge" root) …)`, which names the old literal only to
prove the new value isn't it. Confirmed by running that exact ratchet test
(part of `receipt-artifacts-boundary-test`, green below) — it scans every
`test/**/*.cljc?` file for a `starts-with?`/`startsWith`/`includes?` call
within 160 chars of such a literal and asserts zero offenders.

**Fix-up commit:** the pre-rebase `mcp_workspace_test.clj` resolution had
added `[clojure.string :as str]` (needed for my `str/includes?` call, before
I knew trunk's `under-root?` didn't need it). After taking HEAD's side for
that conflict the require was unused — kondo caught it — dropped in a
second, separate commit (`bac7ba22`, not squashed, per the "always create
NEW commits" rule). That file is now byte-identical to trunk.

**Final diff vs `origin/MCP/main`: 7 files, 82 insertions, 3 deletions** —
purely additive (the 3 src defect fixes + the 3 new witnesses +
`deftest_census.edn`'s 2 new names). None of the 8 conflicted test files
appear in this diff at all — my resolution converged exactly to what trunk
already had for the parts trunk touched.

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

**Stage 1 (against the tag):** none — every hunk applied cleanly by hand,
not `git cherry-pick`, so there was no merge machinery to conflict.
**Stage 2 (rebase onto `origin/MCP/main` after trunk moved to `13635f74`):**
8 conflicts in the 8 files listed above, all resolved by taking trunk's
canonicalized-boundary helper calls. Full detail in "Stage 2" above.

## Gates

### Stage 3 (SPF-001/002/003 repair, after the rebase)
- `validate-artifact-root!` smoke-tested directly (`clojure -M:clj-surgeon/test-deps
  -e …`, before writing the deftests) against all six cases the fence and the
  witnesses cover: blank → `:artifact-root-blank`; relative →
  `:artifact-root-relative`; `/tmp/…` → `:artifact-root-ram-backed`;
  `/root/…` → `:artifact-root-not-owned`; a path under `$HOME` → accepted
  unchanged; `/var/tmp/forge` → accepted unchanged (the parity harness case).
  Re-ran the same six after reverting the `tmp-leak-support` require (the
  detour) — identical results, confirming `ram-backed-path?` behaves the
  same as the reused `base-refusal` would have for these cases.
- `bb -m clj-surgeon.core -- --help`: loads and prints help (confirms the
  revert actually fixed the CLI-load break the detour caused).
- Focused namespace runs: `mcp-alias-migration-test` 151 tests/3375
  assertions (0 failures — includes all seven new SPF-001 witnesses and the
  repinned enumeration); the six receipt-path namespaces
  (`receipt-artifacts-boundary-test`, `admit-patch-test`,
  `mcp-helper-extraction-test`, `mcp-process-test`, `mcp-workspace-test`,
  `mission-typist-executor-test`) 278/5872, 0 failures — re-run after the
  `tmp-leak-support` revert to confirm nothing regressed;
  `cli-dispatch-test` + `extract-test` 60/449, 0 failures — this is the pair
  that caught the CLI-load break (115 failures/8 errors mid-detour, 0 after
  the revert); `bb test/run_all.clj` 898/7939, 0 failures.
- `~/bin/clj-kondo` over `receipt_artifacts.clj` and
  `mcp_alias_migration_test.clj`: 0 errors, 0 warnings (two pre-existing
  `info`-level notes, unrelated lines).
- `sh test/clj_kondo_admission_path_test.sh` standalone: passed, including
  the new SPF-002 witness.
- `make intent-audit`: `:ok true`, `ALIAS-MIGRATION-001 :implemented`
  (SPF-003).
- **Full landing gate**, `~/bin/suite-run make test`, three attempts:
  1. Red — `lane-manifest-test`'s deftest census, correctly: the seven new
     JVM deftests weren't in `test/clj_surgeon/deftest_census.edn` yet.
     Regenerated via its own printed remedy (`CENSUS_REGENERATE=1 clojure
     -M:clj-surgeon/test-deps -e "…lane-manifest-test…"`); diff was exactly
     those seven names.
  2. Red — `gate-refused: … :tree-changed-during-suite`, my own mistake: I
     `rm -rf`'d a stray `resources/__pycache__/` (untracked, produced by
     `py_compile`/the SPF-002 witness's own `importlib` load; not gitignored
     — a real but out-of-scope hygiene gap) **while the gate was still
     running**, and the gate's own tree-change detector correctly caught
     the mid-run mutation. Let it regenerate and land alone on the third
     attempt instead of touching the tree during a run.
  3. Green:

     | stage | exit | wall |
     |---|---:|---:|
     | admit-transaction-recovery-battery | 0 | 11.0 s |
     | battery-fresh | 0 | 1.9 s |
     | alias-migration-test | 0 | 90.2 s |
     | mcp-test | 0 | 99.0 s |
     | test-bb | 0 | 85.2 s |
     | repository-hygiene | 0 | 1.8 s |
     | intent-audit | 0 | 1.9 s |

     `landing-gate: {... :state :passed ... :wall-ms 171888 :git-head
     "bac7ba22b357a100c672de130d0ad6a1c62d66f5" ...}` — `git-head` is the
     stage-3 commit's parent, matching the working tree the gate actually
     ran against (the stage-3 commit itself was made *after* this run, per
     "no commits during"). Zero `FAIL in`/`ERROR in`/`gate-refused` in the
     log. Log: `/var/tmp/forge/plan2/cellC/sonnet-seatpaths-gate6.log`.

### Stage 1 (against the tag `59d8bc0c`)
- Focused namespace runs, `~/bin/clj-kondo`, and one full `~/bin/suite-run
  make test` (run twice — red once on the deftest census, green on retry,
  7/7 stages exit 0, wall 194229 ms). See prior report content preserved
  below under "Stage 1 gate detail."

### Stage 2 (after the rebase onto `13635f74`)
- **Focused namespace runs** of the exact eight conflicted namespaces plus
  the two other touched ones, via
  `clojure -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns …`
  and direct `clojure.test/run-tests` for `cli-dispatch-test`/`extract-test`
  (still absent from `lane-manifest`, pre-existing): **0 failures, 0
  errors** — `receipt-artifacts-boundary-test` (includes the new
  `no-witness-compares-an-artifact-path-against-a-literal-root` ratchet run
  against these exact files), `admit-patch-test`, `mcp-helper-extraction-test`,
  `mcp-process-test`, `mcp-workspace-test`, `mission-typist-executor-test`
  (278 tests/5872 assertions, 0 precondition skips this time — trunk's
  `battery-fresh` receipt was fresh), `cli-dispatch-test` + `extract-test`
  (60/449), `mcp-alias-migration-test` (144/3335), `bb test/run_all.clj`
  (898/7939, includes `memory-battery-test`).
- `~/bin/clj-kondo` over all 13 touched files after the rebase: 0 errors, 6
  warnings, all pre-existing (unrelated lines) — confirmed by diffing
  against the pre-rebase lint run.
- **Full landing gate**, `~/bin/suite-run make test`, one run, green,
  background PID waited on directly (not polled/guessed):

  | stage | exit | wall |
  |---|---:|---:|
  | admit-transaction-recovery-battery | 0 | 11.0 s |
  | battery-fresh | 0 | 1.9 s |
  | alias-migration-test | 0 | 87.3 s |
  | mcp-test | 0 | 96.1 s |
  | test-bb | 0 | 81.5 s |
  | repository-hygiene | 0 | 2.0 s |
  | intent-audit | 0 | 2.0 s |

  `landing-gate: {... :state :passed ... :wall-ms 169788 :git-head
  "3020950b6e965514e932fecefafa9baf8d42043b" ...}` — `git-head` matches the
  stage-2 commit exactly. Zero `FAIL in`/`ERROR in`/`gate-refused` in the
  log. Log: `/var/tmp/forge/plan2/cellC/sonnet-seatpaths-gate3.log`. (The
  trailing `bac7ba22` cleanup commit — dropping an unused require — landed
  after this run; it changes nothing the gate exercises, confirmed by the
  focused `mcp-workspace-test` rerun above already covering it.)

### Stage 1 gate detail (preserved, against the tag before trunk moved)
- Focused runs: 276 tests/5862 assertions (six namespaces) +
  60/449 (cli-dispatch/extract) + 144/3335 (mcp-alias-migration) +
  893/7914 (bb) — all 0 failures/0 errors.
- One iteration: first full-gate run correctly refused on
  `lane-manifest-test`'s deftest census (2 new JVM deftests not yet listed);
  regenerated via its own printed remedy, diff was exactly the 2 new names.
- `~/bin/clj-kondo`: 0 errors; fixed one self-introduced warning
  (`clojure.string/includes?` used without a require in
  `mcp_workspace_test.clj` — later made moot by the stage-2 rebase, which
  replaced that whole assertion with trunk's `under-root?`).
- Full landing gate (second attempt): all 7 stages exit 0, `:state :passed`,
  wall 194229 ms. Log: `/var/tmp/forge/plan2/cellC/sonnet-seatpaths-gate2.log`.

## Boundaries observed

No `pkill`/`pgrep -f`. All temp/log output under `/var/tmp/forge`. Never
touched ports 7888/7890/7894/7895/8300-8339. No `-Xmx` set directly (suite-run
and the Makefile's own JVM launches carry their own bounded heaps, unchanged).
Never touched `clj-surgeon-pubcand` or any other builder's worktree; only read
from it via `git show <rev>:<path>` against the shared object store. No
commits during any of the three background suite-run gates (stage 3's second
attempt was ME mutating the tree mid-run by hand, a `rm -rf`, not a commit —
still a boundary lesson, logged above rather than repeated here). **Stage 3
push:** `git push origin HEAD:refs/heads/fable/seat-path-defaults-r2` — a new
branch, no `--force`, local `fable/seat-path-defaults` untouched and still
unpushed. Stages 1/2 did not push at all. Stage 1 inside its 45-minute
timebox; stage 2 inside its 40-minute timebox; stage 3 inside its 40-minute
timebox.

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
