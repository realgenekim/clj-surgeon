# ship v3.4 — STAGED, NOT INSTALLED (three field defects + the independent red-team's INSTALL-WITH-FIX)

**Round 2 (red-team applied).** The independent red-team returned INSTALL-WITH-FIX
(`opus-v34-redteam-report.md`); F1, F2, F3, F4 and F5(a) are applied in the staged set and each has a
row that is RED against the pre-red-team bytes. F5(owner file) and F6 are filed as notes at the end,
not built. Corpus is now **129 rows, 0 mismatches**.

Staged at `/var/tmp/forge/ship-v3.4/` (copy of the installed v3.3 set). `~/bin` untouched; the
installer was NOT run. Baseline check: `~/bin/ship` and `~/bin/ship-fix-block-spec.md` are
byte-identical to `/var/tmp/forge/ship-v3.3/` (`b5eb34392f2bc9af`, `875cffac2664294c`), so v3.4 is
exactly v3.3 plus the diff below (228 changed lines in `ship`, 1587 lines total).

## The table — every corpus against the STAGED bytes

| fixture | rows | mismatches | rc |
|---|---|---|---|
| run-land-auto | 19 | 0 | 0 |
| run-land-publication-truth | 12 | 0 | 0 |
| run-ship-v2 | 28 | 0 | 0 |
| run-ship-v3 | 22 | 0 | 0 |
| run-ship-v3.1 | 13 | 0 | 0 |
| run-ship-v3.2 | 8 | 0 | 0 |
| run-ship-v3.3 | 13 | 0 | 0 |
| **run-ship-v3.4 (new)** | **14** | **0** | **0** |
| **TOTAL** | **129** | **0** | — |

Logs and exit codes: `/var/tmp/forge/ship-v3.4/corpus/<fixture>.log` / `.rc`.

**The new rows are not vacuous — two red witnesses.**

* against the *installed v3.3* bytes (`V3_DIR=/var/tmp/forge/ship-v3.3`): **11 of 14 red** —
  `corpus/RED-witness-against-v3.3.log`;
* against the *pre-red-team* staged bytes, kept for exactly this purpose at
  `/var/tmp/forge/ship-v34-preredteam/` (round-1 v3.4): **4 red — rows 9, 10, 12, 13**, one per
  red-team fix that has a row — `corpus/RED-witness-against-pre-redteam.log`.

Row-by-row against v3.3:

| row | v3.3 (defective) | v3.4 (staged) |
|---|---|---|
| 1 fix-block naming the TIP is accepted and printed | MISMATCH | ok |
| 2 fix-block naming neither sealed nor tip still STOPS | MISMATCH (refusal did not name the tip) | ok |
| 3 fix-block naming the SEALED candidate is accepted | ok (non-regression) | ok |
| 4 every round brief opens with the sealed candidate | MISMATCH | ok |
| 5 a STOP after a green fast lane prints fastlane=green + wall | MISMATCH | ok |
| 6 the record carries the candidate it measured | MISMATCH | ok |
| 7 a delta round retires the old lane and observes its own | ok (non-regression) | ok |
| 8 a STOP after a staged self-fix leaves the fence tree clean | MISMATCH | ok |
| 9 a dirty fence tree is reported as fence-worktree-dirty (+ F3's loud no-archive line) | MISMATCH | ok |
| 10 a live process ONE DIRECTORY DOWN blocks the release (F5a) | MISMATCH | ok |
| 11 a verdict that moved after the archive is copied out first | MISMATCH | ok |
| 12 every doomed file is kept, under both stores, before the sweep (F1) | MISMATCH | ok |
| 13 an abbreviated CANDIDATE that resolves to the tip is accepted (F2) | MISMATCH | ok |
| 14 an abbreviation that resolves to nothing still STOPS | ok (non-regression) | ok |

## What changed, and why

### 1. `fix-block-wrong-candidate` on the tip that composes the sealed candidate

Evidence: run `20260909T045222Z-da4396517d50` sealed `1d73259d` (base `d2c3aa80` + tip `da439651`);
Sol's `SHIP-FIX-BLOCK` said `CANDIDATE: da439651` — the sha in the brief's own title. The block was
otherwise valid and fence-run's END RECEIPT proved the worktree HEAD at review exit *was* the fenced
sha. A finished, tree-bound review was thrown away over a naming convention.

* `ship` now accepts `CANDIDATE:` equal to the sealed candidate **or** to `$CUR_TIP`, the tip that
  composes it — and nothing else. The accepted-tip case prints
  `fix-block candidate=tip <sha> composes sealed <sha>` and writes the same line to `events.log`,
  so a second accepted name is never silent.
* The refusal for any other sha now also names the tip it would have accepted.
* The binding to the reviewed **bytes** is unchanged: the pre-existing `fence-head-moved` check has
  already proved the fence worktree sits on `$CAND` before this comparison runs.
* `ship` now prepends, to the brief the reviewer actually reads (round 1 *and* every delta round,
  after the seal exists):
  `SEALED CANDIDATE (use in SHIP-FIX-BLOCK CANDIDATE:): <sha>` plus a line naming base, tip, and the
  two accepted spellings. Round 1's brief is composed *before* the merge that mints the candidate,
  which is why the header is written at seal time, not at brief-compose time.
* `ship-fix-block-spec.md` CANDIDATE line is now
  `<the sealed candidate sha printed in the brief header, or the branch tip it was sealed from>`,
  with a paragraph explaining both spellings and that anything else stops the run.

### 2. `fastlane=unknown` after an observed green

Evidence: the same run printed `gates_prewarm=green wall=480s` on line 12 and
`fastlane=unknown fastlane_elapsed=unknown` on its own SHIP line five minutes later. One variable was
doing two jobs: the GO-WITH-FIX delta round's `fastlane_abandon` cleared the fields the SHIP line
reads.

* Split into a **RECORD** and a **LIVE** state. `FASTLANE` / `FASTLANE_EL` / `FASTLANE_CAND` are
  written by `fl_record` **at the moment of observation** (green, red, or unknown), tagged with the
  candidate the lane measured, and never cleared by abandon or by a new round.
* `FL_STATE` is the live lane, reset per round and on abandon; **every gating decision now reads
  `FL_STATE`** (phase B / consumable gate receipt at ship:1282, gate disposition at ship:1385-1386),
  so a retired lane can never shorten a later candidate's landing.
* The SHIP line gains one field, `fastlane_candidate=<12-char sha|->`, immediately after
  `fastlane_elapsed`. Without it, a record that outlives its candidate would read as a green about
  the final tree. All existing SHIP-line assertions are substring greps; all 115 pre-existing rows
  stay green.

### 3. (coordinator's third defect) the fence worktree is borrowed

Evidence: the stop above walked away leaving Sol's self-fix **staged** in
`/home/forge/src/clj-surgeon-fence`; `git checkout -- .` and `git clean -fd` do not touch the index,
so run `20260909T051614Z` died in 2 s with `reason=no-start-receipt rc=3` — a reason about a launcher
that never spoke, when it had spoken precisely and named the file.

* **(a)** New `fence_release()`, called from `finish()` (every SHIP-STOP / HOLD / RED / LANDED path),
  from the EXIT trap, and before each round's review launch. It refuses to sweep unless this run's
  verdict archive exists and is non-empty; it compares the worktree copy with the archive and, when
  they differ, copies the divergent bytes to `/var/tmp/forge/fence-verdicts/<name>.ship-exit-<stamp>.md`
  and says so before sweeping (and refuses to sweep at all if that copy fails); it refuses while any
  process other than fence-run's own END-RECEIPT waiter is living in that worktree
  (`SHIP-FENCE-NOT-RELEASED pids=…`); then `git reset -q --hard` + `git clean -qfd`, idempotent via
  `FENCE_RELEASED`, with `== ship: fence worktree released clean` on stdout and
  `FENCE released worktree=… archive=…` in the journal.
* **(b)** When `fence-run` refuses with `worktree not clean`, ship now stops with
  `reason=fence-worktree-dirty`, prints the `git status --short` lines (from fence-run's own output,
  falling back to a live read), prints the exact sweep command, and carries the reason on the SHIP
  line. `no-start-receipt` remains for a launcher that genuinely never produced a receipt.
* One collision found and avoided: the release event was first spelled `dir=$FENCE_DIR`, which made
  `run-ship-v3.1` row 8 (`the-lane-ran-in-the-FENCE-worktree`, a bare journal grep for `dir=$FX/fence`)
  go red. Renamed to `worktree=` — the fixture's control is left intact rather than relaxed.

## Round 2 — the red-team's INSTALL-WITH-FIX items, applied

Everything below is in `ship` (patch script: `patch-ship6.py`, exact-text and asserted like the
others). Their fix diffs were followed; where I deviated it is noted.

**F1 (HIGH) — `fence_release` destroyed every unarchived byte except the verdict.** It now enumerates
`ls-files --others --exclude-standard` + `diff --name-only` + `diff --name-only --cached`, copies
every one of those files to **both** `$FENCE_ARCHDIR/swept-<stamp>/<path>` *and*
`$RUNDIR/fence-swept-<stamp>/<path>` (the coordinator's two-destination requirement), and prints
`SHIP-FENCE-SWEEPING files=N kept=<both dirs> list='<paths>'` plus a `FENCE sweeping` journal line
**before** the reset+clean. A copy that fails aborts the sweep with
`SHIP-FENCE-KEPT reason=cannot-copy-doomed-file`. The stamped `swept-<stamp>/` subdirectory is a
namespace of its own, so fence-run's `<basename>-<stamp>.md` archive files are never overwritten,
moved or raced — v3.4 only ever *adds* copies beside the 2026-09-08 ratchet's.
Witness: row 12 asserts `xc.md.last.md`, `xc.md.codex.log` and `repro.clj` are all recoverable
afterwards; it is red against the pre-red-team bytes with `SOLS-FINAL-MESSAGE-WAS-DESTROYED`.

**F2 — an abbreviated sha was still thrown away.** `CANDIDATE:` is now RESOLVED, not string-compared:
an all-hex string of ≥7 characters is put through `git rev-parse --verify -q "<x>^{commit}"` in the
**fence worktree** (falling back to `$GIT_REPO`), and the resolution is printed
(`fix-block candidate=<abbrev> resolves to <sha>`). Deviation from the red-team's diff, deliberately
tighter: their `case [0-9a-fA-F]*)` would also resolve a *branch* whose name begins with a hex letter;
mine requires the whole string to be hex and ≥7 chars, so a ref name can never be accepted here.
Ambiguous or unknown still resolves to nothing and falls through to the unchanged refusal.
Witnesses: row 13 (12-char abbreviation of the tip → accepted, lands) and row 14
(`deadbeef1234` → still stops).

**F3 — the one guard that refused in silence.** The archive-missing guard now prints
`SHIP-FENCE-KEPT reason=no-archive archive=<-> dir=<fence>` and journals `FENCE kept reason=no-archive`
whenever it declines to sweep a tree that is actually dirty. (Named `SHIP-FENCE-KEPT` rather than the
report's `SHIP-FENCE-NOT-RELEASED` so the "nobody swept, and here is why" class has one greppable
token distinct from the busy case.) Witness: row 9 now also asserts that line.

**F4 — the untested branches.** The fixture's `fence-run` stub now writes the verdict **inside** the
fence worktree (`$FX/fence/docs/observations/<name>.md`), as the real one does, which is what made
two branches reachable; it also exports `FENCE_VERDICT_ARCHIVE="$FX/fence-verdicts"` so no fixture can
write into the real store. New rows 10 (busy → `SHIP-FENCE-NOT-RELEASED pids=`, tree still dirty,
litter intact), 11 (in-tree verdict moving after the archive → `SHIP-FENCE-VERDICT-DIVERGED`, both
copies present), 12 (F1's witness), 13 and 14 (F2). Every branch of `fence_release` now has a row
except `SHIP-FENCE-RELEASE-FAILED`, which needs a `git reset` that fails — noted below.

**F5(a) — `fence_busy` was cwd-exact.** Now `case "$d" in "$FENCE_DIR"|"$FENCE_DIR"/*)`. Row 10's
lingering process deliberately sits in `docs/observations/`, one level down, so the row is the
subtree witness and is red against the pre-red-team bytes.

### Filed, not built (from the red-team, per the coordinator)

* **F5 (owner file).** `fence_busy` answers "is someone standing here", not "does another run still
  need these bytes" — and there is a real ~90 s hole after a reviewer exits while `autofix` is still
  polling for the END RECEIPT. The proper fix is an owner file inside the fence worktree's git dir
  carrying `pid=<n> run=<id>`, written before the review launches and checked by `fence_release`,
  the same shape as `$LEASEDIR/owner`. **Not built this round** (coordinator: note it). Until then the
  standing risk is two ships with different `SHIP_ROOT` sharing the global fence worktree; the
  red-team rated it CREDIBLE, NOT YET PROVEN.
* **F6 (reporting nit).** `fastlane=green` about a retired candidate is legible only by prefix-matching
  `fastlane_candidate` (12 chars) against `final_candidate` (40). Suggested `fastlane=green-stale` /
  `fastlane_scope=prior-candidate`. Not adopted: the red-team's own live row was inconclusive, and no
  decision reads the record (all gating reads `FL_STATE`).
* **`SHIP-FENCE-RELEASE-FAILED`** has no fixture row — reaching it needs a `git reset --hard` that
  fails inside a fixture worktree. Cheap follow-up: a read-only-directory row.

## Staged hashes (sha256)

```
8e0833b7132b43ef1e71544fd32f73d0efbb34b12a411a29541a7d679765d8bb  ship                       (CHANGED)
864632b6bee6ebbbd6e52d85d86797e40398ff6043ca872c8859dd5fc67a720a  ship-fix-block-spec.md     (CHANGED)
c3782d4211480a06823348e39cd2af4e11ea70338cf11f6ae85e3517f89850cf  install.sh                 (CHANGED)
3b85d733c08d0ed87946ba45baa8c542c14bd8c9578a96f99879e280ae2672e1  fixtures/run-ship-v3.4.sh  (NEW, 14 rows)
cc09e618af202f4b33d662373fc6b714fbb854417fe061f43ebf6d1d1f267e98  fence-run                  (unchanged)
4a1eae94a7e289b631683d629a411e8f382ed5bbc17b8b556dffa88968635972  land                       (unchanged)
edcac9bdc9725ba9b31132032504eb78641c93e0f80c02c2e40221e6bf9d7b9e  land-auto                  (unchanged)
fedf998c2bb635cdd59fbc55b7d0350e2f5f8a01e27dd600776784df93e11e3d  receipt-chain              (unchanged)
55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907  records-push               (unchanged)
45c3d1dd5ed5f12037948d1e316ad993a62122b6da6ac44f9b0e6447297a802d  run-bg                     (unchanged)
44401a573e08d4f3540d5760d3ff92857532d2f6bad1caa6fab84d2ad719ef48  sol-yolo                   (unchanged)
```

Also staged, for the record of how the bytes were produced (not installed):
`patch-ship4.py` (defects 1 and 2), `patch-ship5.py` (defect 3) and `patch-ship6.py` (the red-team's
F1/F2/F3/F5a) — each replacement is exact-text and asserted, so a miss is a refusal rather than a
silent no-op. `/var/tmp/forge/ship-v34-preredteam/` is the round-1 snapshot kept as the red witness's
control; it is not an install candidate and can be deleted once this lands.

## The exact install command (for Gene / the installing seat — I did not run it)

```bash
bash /var/tmp/forge/ship-v3.4/install.sh
```

It refuses while any target script is running or a ship lease names a live pid, backs every replaced
file up under `/var/tmp/forge/<name>.bak.<stamp>`, installs by atomic rename, and then re-runs all
eight fixtures **against the installed bytes**, refusing to call itself done on any mismatch.
New in v3.4's installer: it copies `$SRC/fixtures/*.sh` into `/var/tmp/forge/tighten/fixtures`
(backing up any replaced file with the same stamp) so `run-ship-v3.4.sh` joins the canonical set,
and the re-run loop includes `run-ship-v3.4`.

Rollback, if a fixture mismatches against the installed bytes:
`cp /var/tmp/forge/ship.bak.<stamp> ~/bin/ship` (and the same for `ship-fix-block-spec.md`).

## Caveats

* The fixtures are the proof; **no v3.4 bytes have run a real landing yet.** The first real `ship`
  run after install is the field receipt, and rows 5-7's lane is a `sleep 2` stub, not a suite.
* Row 9 drives ship through a fixture `fence-run` that reproduces the real refusal text and exit 3.
  It proves ship's *reporting*; it does not re-prove fence-run's own sweep sequence.
* `fence_release` resets the **shared** `/home/forge/src/clj-surgeon-fence` worktree. It is now
  guarded five ways (archive-exists-and-says-so, byte-identity-or-copy-first, every-doomed-file-copied-
  to-two-stores-first, nobody-living-in-the-subtree, abort-on-any-failed-copy) — but it is still the
  one change in this batch that destroys state outside a run's own directories.
* That real worktree is dirty **right now** with exactly the three files F1 predicted
  (`census-derived-fence.md`, `.md.codex.log`, `.md.last.md`). Under the fixed bytes the next run
  copies all three to `/var/tmp/forge/fence-verdicts/swept-<stamp>/` and its own run dir, and prints
  `SHIP-FENCE-SWEEPING files=3 …`, before sweeping. Under the pre-red-team bytes two of them would
  have gone. Worth reading that line on the first real run.
* F5's owner-file gap is open: `fence_busy` cannot see a run that needs the bytes but has nobody
  standing in the tree (the ~90 s END-RECEIPT window). Single-ship operation is unaffected.
