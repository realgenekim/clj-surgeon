# Independent review — round 8 (merge of the two lanes + the install source-descendant ratchet)

**GO-WITH-FIX** for installing this merge on the seat with
`RUN_SCOPED_CIRCUITS=1 PYTHONDONTWRITEBYTECODE=1 bash install.sh`.

The merge is complete — I checked every changed file on both parents and **no hunk is lost**.
The ratchet is real, red-to-green, and it fires before any destination byte moves. Two things
keep this off a clean GO, and both are cheap:

* **FIX-A (must do before the install, operator):** the named command has never been run, and
  `RUN_SCOPED_CIRCUITS=1` makes the installer's *nested* suite **launch two live subscription
  circuits (`claude` and `codex`) after `~/bin` has already been replaced**, for a closure that
  has no retained index and *cannot be pre-warmed*. `test/circuits.py:31-34` durably reserves an
  attempt **before** launching, so a failed launch poisons that closure and every later install of
  the same tree refuses `SCOPED CIRCUIT OWED: prior attempt retained` until a human deletes the
  attempt file. Know the unblock before you type the command (below).
* **FIX-B (one witness):** the ratchet's wiring into the **real** install path is not witnessed by
  anything. I deleted the `source_gate "$DEST" "$SRC"` call from `install.sh` entirely and
  `test/install-source-gate.sh` stayed **49 checks, 0 failed, exit 0**.

Everything else below is verified evidence, with the commands and their outputs.

Subject: `/var/tmp/forge/ship-v3.13`, branch `v3.13-round5`,
HEAD `13aef7d9ceb11939f7ab75d518b1ceb5cc1c0e01` (parents `bd514cd`, `06fa57e`; base `f8e4515`).
All mutants and witnesses were run from a `git archive HEAD` export at
`/var/tmp/forge/ship-v313-fx/review8/` with `WITNESS_ROOT=/var/tmp/forge/ship-v313-fx/review8-fx`.
Nothing in the worktree, `~/bin`, `/var/tmp/forge/ledger/events.jsonl` or `/tmp` was written.

---

## 1. Merge completeness — no hunk lost

`git merge-base bd514cd 06fa57e` → `f8e45151e897a9feade7c04654a0731dcc7c7b91`, so `f8e4515` is
the true common base. Both parents are ancestors of HEAD (`git merge-base --is-ancestor` clean for
`bd514cd`, `06fa57e` and `f8e4515`).

**Files changed base→parent:** 74 on `bd514cd`, 30 on `06fa57e`. Exactly **7** were touched by both:
`MANIFEST.staged.sha256`, `REPORT.md`, `docs/intent/registry.edn`, `install.sh`, `report.edn`,
`test/measure.py`, `test/run.sh` — matching the report's "6 conflicted + `test/measure.py`
auto-merged".

### 1a. Single-side files: byte identity with their parent

`git diff --quiet <parent> HEAD -- <file>` for every file the other side did not touch:

* **`bd514cd`-only (73 files): all IDENTICAL.** Including `lib/ledger.py`, `lib/encounters.py`,
  `lib/measure_store.py`, `lib/ledger-probe.py`, `lib/packet.py`, `lib/measure.py`,
  `test/ledger-anchor.sh`, `test/encounter-run.py`, `test/install.sh`, `test/ledger.sh`,
  `test/legacy-writer.py`, `test/measure.sh`, `.gitignore`, and all 50 `test/receipts/*`.
  The two `lib/__pycache__/*.pyc` **deletions** also survive (absent in HEAD).
* **`06fa57e`-only (29 files): all IDENTICAL.** Including `ship`, `receipt-chain`,
  `test/bookkeeping.sh`, `test/bookkeeping.py`, `test/bookkeeping-autofix.sh` and all 24
  `test/evidence/*`.

The only single-side file that is *not* byte-identical to either parent is
`MANIFEST.staged.sha256`, which both sides regenerated (it is in the both-touched set).

### 1b. Both-sides files: every added line survives

For each both-touched source file I took every `+` line from `git diff f8e4515 <parent>` and
required it to be present in HEAD (whitespace-normalised):

| file | round side added / missing | bookkeeping side added / missing |
|---|---|---|
| `test/measure.py` | 188 / **0** | 3 / **0** |
| `install.sh` | 135 / 1 | 2 / **0** |
| `docs/intent/registry.edn` | 47 / 1 | 15 / 1 |
| `test/run.sh` | 5 / 1 | 1 / 1 |

Each of the four "missing" lines is a line **both sides rewrote**, and each is resolved keeping
both. Checked set-wise rather than by eye:

* **`test/run.sh` witness list.** HEAD: `bookkeeping measure packet refusals fix1 fix2 publication
  ledger ledger-anchor lifecycle ship board compatibility production-content heartbeat fixture
  install-source-gate install circuits`. Witnesses in the round-side list missing from HEAD: **none**.
  In the bookkeeping-side list missing from HEAD: **none**. HEAD's only extra is
  `install-source-gate`. So **bookkeeping first, circuits last, nothing dropped** — the requested
  ordering, both rationale comments kept.
* **`install.sh` `FILES`.** Round-side entries missing from HEAD: **none**. Bookkeeping-side entries
  missing from HEAD: **none**. HEAD adds exactly one: `test/install-source-gate.sh`. The bookkeeping
  side's two comment lines are present verbatim at `install.sh:54-55`.
* **`docs/intent/registry.edn`.** Comparing every quoted claim string ≥25 chars: round side 147
  claims, **0** missing; bookkeeping side 79 claims, **0** missing. The "lost" lines are only the
  last entry's closing bracket. 48 top-level intents: `ENCOUNTER-RUN-001..025`, `AUTOFIX-BRANCH-001`,
  `AUTOFIX-CENSUS-001`, `RECEIPT-BATTERY-APPEND-001`, `AUTOFIX-FIXTURE-ISOLATION-001`,
  `INSTALL-SOURCE-001`, plus the 18 pre-existing.
* **`REPORT.md`.** Bookkeeping side: 9 substantive lines, **0** missing. Round side: 289 substantive
  lines, 3 missing — all three are the round-7 "verdict pointer" header paragraph, replaced by the
  round-8 equivalent. Prose only.
* **`report.edn`** was replaced wholesale with a round-8 report, as declared.

**Verdict on completeness: no hunk lost, on either side.**

### 1c. Specific confirmations requested, in HEAD

| required in HEAD | evidence |
|---|---|
| ship's fix-branch selection excludes `^fable/battery-receipt-[0-9a-f]+$` | `ship:773` and `ship:775` — awk `function receipt(s) {return s ~ /^fable\/battery-receipt-[0-9a-f][0-9a-f]*$/ && length(s)>=29 && length(s)<=62}`, applied as `!receipt($1)` on the source-branch scan |
| ship refuses `fix-branch-ambiguous` | `ship:1129-1132`, `STOP_REASON=fix-branch-ambiguous`, `SHIP-STOP ... repair=export SHIP_FIX_BRANCH=<one>`, `finish HOLD 3` |
| the autofix census hook and its **five** refusals | `fix-touches-census-policy` (1167), `fix-census-failed` (1210…1283), `fix-adds-deftest-without-census` (1218), `fix-census-touched-unexpected-paths` (1261), `fix-census-removed-lines` (1267/1273) — exactly five distinct reasons |
| `receipt-chain` exports `BATTERY_LEDGER_APPEND=1` around `make test-battery` | `receipt-chain:98` — `BATTERY_LEDGER_APPEND=1 flock … make test-battery`, immediately under `# INTENT: RECEIPT-BATTERY-APPEND-001` at :97. (A per-command env prefix, not a shell `export`; it reaches `flock`'s child `make`, which is what the intent says. Note the *recovery* battery at :94 is deliberately not prefixed.) |
| `lib/ledger.py` `anchor_defect` / `anchor-repair` / carried anchor | `anchor_defect` at :126 with the high-water-mark semantics (rows above the anchor are LAG, downgrade of an ANCHORED row still refuses); `anchor_repair` at :197; `anchor-repair` CLI op at :435; `anchor_remedy` at :89. Byte-identical to `bd514cd`. |
| `install.sh` `ledger_probe` with subject naming and `subject-missing` | `ledger_probe()` at :131; `print('subject=%s'%origin)` and `rows=` on every path; `reason=subject-missing` when the subject is absent or empty; one-snapshot-under-the-shared-lock copy, anchor first |
| the older-vintage supervisor **warning** | `install.sh:428-445` — `SUPERVISORS=$(proc_scan "lib/run.py")`, `INSTALL WARNING reason=older-vintage-supervisors-alive count=…`, pid + real start time, `"This is an observation, not a refusal"` |
| `test/run.sh` order, bookkeeping first and circuits last | see 1b |

**Marker table reproduced exactly** (all eight rows match the report): `fix-branch-ambiguous` 3,
`fix-touches-census-policy` 1, `BATTERY_LEDGER_APPEND` 1, `bookkeeping-autofix` 1, `anchor-repair` 5,
`subject-missing` 2, older-vintage warning 1, `install-drops-installed-fixes` 3.

**Staged manifest.** `FILES` has **109** entries, `MANIFEST.staged.sha256` has **109**, the two sets
are identical, and `sha256sum --quiet -c MANIFEST.staged.sha256` exits **0**.

### 1d. Two report inaccuracies (documentation only)

1. **"Not committed, not pushed."** The merge **is** committed: HEAD is the merge commit `13aef7d`
   with both parents, and `git status --porcelain` is empty. It is **not pushed** — no remote refs
   exist in this worktree at all, and only `v3.13-round5` contains HEAD. The report's `:owed`
   entry "The commit and push of this merge … left uncommitted" is stale on its first half.
2. **"47 entries"** in the merged registry (`REPORT.md` and `report.edn`). The actual count is
   **48** — the count was taken before `INSTALL-SOURCE-001` was added by the ratchet.

Neither changes a behaviour. Both should be corrected so the record matches the tree.

---

## 2. The ratchet, driven independently

### 2a. The witness, re-run from my export

```
WITNESS_ROOT=/var/tmp/forge/ship-v313-fx/review8-fx PYTHONDONTWRITEBYTECODE=1
bash review8/test/install-source-gate.sh
  → install-source-gate: 49 checks, 0 failed
  → PASS install source gate: the installed source is recorded and a non-descendant candidate refuses by name
  → exit 0        (captured from $?, never through a pipe)
```

### 2b. Hand-built fixtures, not the witness's assertions

I built my own `origin` with a common base and two divergent lanes, cloned `srca` (lane A) and
`srcb` (lane B), and drove `install.sh`'s entry points directly. Raw output:

```
A=b88984c… (LANE A: encounter runner)   B=1ba31c0… (LANE B: bookkeeping autofix fixes)

# install A into the fixture DEST
$ install.sh --source-stamp $GX/dest $GX/srca
INSTALL SOURCE-STAMP ok destination=…/dest PROVEN=true stamp=20260914T113928Z backup=…
  source=b88984c1b1005f4d59d54ec3739023090600165f source_repo=…/srca      rc=0

# candidate B, not descending from A
$ install.sh --source-gate $GX/dest $GX/srcb
INSTALL REFUSED reason=install-drops-installed-fixes installed_source=b88984c… installed_source_repo=…/srca
  candidate=1ba31c0… candidate_repo=…/srcb
  The candidate is not a descendant of the commit that is installed. These commits are
  installed now and are NOT in the candidate -- installing would silently drop them:
    b88984c LANE A: encounter runner
  Repair: merge the installed source into the candidate and install the merge:
    git -C …/srcb merge b88984c…
  Override (records override=non-descendant in the stamp):
    INSTALL_ALLOW_NON_DESCENDANT=1 bash install.sh                        rc=2

# the merge proceeds
$ install.sh --source-gate $GX/dest $GX/srcm       (srcm = lane-b + merge of lane-a)
INSTALL SOURCE-GATE ok candidate=62f4827… candidate_repo=…/srcm            rc=0

# the override proceeds and is RECORDED
$ INSTALL_ALLOW_NON_DESCENDANT=1 install.sh --source-stamp $GX/dest $GX/srcb
INSTALL OVERRIDE reason=install-drops-installed-fixes … installing drops them:
    b88984c LANE A: encounter runner
INSTALL SOURCE-STAMP ok … source=1ba31c0… source_repo=…/srcb override=non-descendant   rc=0

# a pre-ratchet stamp (no source=) passes
$ printf 'PROVEN=true stamp=20260101T000000Z backup=/var/tmp/forge/ship/backups/x\n' > dest/INSTALL.PROVEN
$ install.sh --source-gate $GX/dest $GX/srcb
INSTALL SOURCE-GATE ok candidate=1ba31c0… candidate_repo=…/srcb            rc=0
```

Every scenario the brief names reproduces, including the refusal **naming A's dropped commit by
sha and subject**.

### 2c. Do `--source-gate` / `--source-stamp` exercise the real install's code path?

**Partly, and the gap matters.**

* `--source-gate` (`install.sh:296-301`) calls `source_gate "$2" "$3"` — the *identical function*
  the real install calls at `install.sh:346`. Same code, different arguments. ✔
* `--source-stamp` (`install.sh:303-308`) calls the same `source_gate`, then `stamp_write true`,
  the *single writer* the real install uses at `:480` (`false`) and `:553` (`true`). ✔
* **NOT exercised: the call sites themselves** — line 346, and its position relative to the first
  destination mutation. A witness that drives only the entry points cannot see whether the
  installer still calls them.

### 2d. Mutants

**Mutant A — remove the descendant check** (`install.sh:265`,
`git -C "$src" merge-base --is-ancestor "$installed" "$candidate" && return 0` → `return 0`):

```
install-source-gate: 49 checks, 9 failed     exit 1
  FAIL divergent candidate gate rc: got '0' want '2'
  FAIL refusal is typed: 'INSTALL REFUSED reason=install-drops-installed-fixes' absent from:
       INSTALL SOURCE-GATE ok candidate=2d2b90f… …
  FAIL refusal names the installed source / LISTS the commit / names what that commit is /
       names the override / override is announced / override still prints what is dropped /
       the stamp records the override
```

**The witness goes red. The descendant check is load-bearing.** ✔

**Mutant B — delete the gate from the REAL install path** (remove
`source_gate "$DEST" "$SRC" || exit 2` at `install.sh:346`, leaving the function and both entry
points intact):

```
install-source-gate: 49 checks, 0 failed     exit 0
```

**The witness stays fully green with the ratchet unwired from the installer.** No other file in the
tree references `install-drops-installed-fixes`, `source-gate` or `INSTALL-SOURCE-001` except
`install.sh`, `test/run.sh` and `test/install-source-gate.sh`, and `test/install.sh` asserts only
`grep -q '^PROVEN=true '` (line 68) — nothing asserts `source=` after a real install. This is the
"verifier blind to its own subject" shape: the one assertion nobody wrote is the one that says the
gate is still called. **This is FIX-B.**

---

## 3. Does the ratchet fire on the REAL install path before any destination byte moves?

**Yes — by reading. Line numbers in HEAD's `install.sh`:**

| line | what |
|---|---|
| 314 | `[ -z "${1:-}" ] \|\| { echo "INSTALL REFUSED reason=unknown-argument …"; exit 2; }` |
| 315 | `MUTATED=0`; 325 `trap install_exit EXIT` |
| 326 | `echo "== install-ship-v3.9 …"` — the **first** mention of `$DEST`, an echo |
| 328-334 | source-side reads only: every `FILES` entry exists under `$SRC`; `sha256sum -c MANIFEST.staged.sha256` in `$SRC` |
| **346** | **`source_gate "$DEST" "$SRC" \|\| exit 2`** (under `# INTENT: INSTALL-SOURCE-001`) |
| 347 | `START_HASH=$(stage_hash)` |
| 411 | `BUSY=$(proc_scan "$EXEC")` |
| **413** | `INSTALL REFUSED reason=target-scripts-are-running` |
| 428-445 | the older-vintage supervisor **warning** |
| 450-461 | `INSTALL REFUSED reason=ship-lease-live` |
| **463** | **`mkdir -p "$DEST" "$BACKUP_ROOT"`** — the first destination mutation |
| 465-477 | backup inventory + `rollback.json` |
| **479-480** | `MUTATED=1`; `stamp_write false "$DEST/INSTALL.PROVEN"` — the first destination **byte** |
| 483-490 | `cp` + `mv -f` the 109 files |
| 538 | `UNDER_TEST="$DEST" SKIP_INSTALL_WITNESS=1 bash "$SRC/test/run.sh"` |
| 541 | `ledger_probe …` |
| 553 | `stamp_write true "$DEST/INSTALL.PROVEN"` |

Auditing every `$DEST` reference at or before line 346: only `DEST=${DEST:-/home/forge/bin}` (15),
the echo (326) and the gate call (346). **The gate runs 117 lines before `mkdir -p "$DEST"` and 134
before the first byte, and 65 lines *before* the running-target refusal at 413.**

Two consequences worth naming:

* The ordering is **correct and strictly stronger than needed** — a refusal here cannot leave a
  half-installed destination, and `install_exit`'s rollback is not even reached because
  `MUTATED` is still 0.
* The gate now **precedes** `target-scripts-are-running`, so on a box where both would fire the
  operator sees `install-drops-installed-fixes` and not the running-process refusal. That is a
  reasonable priority (the ordering question is the more serious one), but it is a behaviour
  change to the refusal *order* that the report does not mention.

**But nothing witnesses this ordering** (mutant B). FIX-B should be a real-install-path witness, not
a comment: drive `install.sh` with `DEST` pointed at a fixture destination that already carries a
non-descendant `source=` stamp, and assert (a) `INSTALL REFUSED reason=install-drops-installed-fixes`,
(b) exit 2, (c) the destination is byte-for-byte unchanged and `MANIFEST.installed.sha256` /
`INSTALL.PROVEN` were never rewritten. Mutant B must turn that red.

---

## 4. Individual witnesses — exit codes captured from `$?`

All run from the export with `WITNESS_ROOT=/var/tmp/forge/ship-v313-fx/review8-fx`,
`PYTHONDONTWRITEBYTECODE=1`, under the same preamble `test/run.sh` uses.

| witness | exit | evidence |
|---|---|---|
| `test/bookkeeping.py` | **0** | all rounds 2 and 3 cases PASS: `fix-adds-deftest-without-census`, `census=+0/-0` for comment/string/discard/nested-discard/multiline/header, `fix-touches-census-policy` for policy-add/replace/delete and rule-add/replace, `.cljc`/`.cljs`, `PASS mawk receipt bounds, ambiguous exclusions, HEAD omitted` |
| `test/bookkeeping-autofix.sh` | **0** | drove **real `ship`** end to end in an isolated fixture; ended `SHIP status=HOLD … reason=fix-touches-census-policy`, `PASS real allowlist-only patch refused, remote unchanged` |
| `test/ledger-anchor.sh` | **0** | 8 `PASS` lines, last: `anchor-repair split: a gap repairs automatically; dropping anchored rows 6,7 refuses by name until --drop-anchored is given` |
| `test/ledger.sh` | **0** | `PASS ledger envelope/checksum, duplicate id, torn tail`; `PASS concurrent writers: 6 x 200, contiguous sequence and anchor, verified checksums` |
| `test/measure.py` | **0** | `Ran 18 tests … OK (skipped=1)` — the skip is `test_no_tracked_library_bytes_outside_the_closure`, which skips because a `git archive` export is not a work tree. **Re-run against a git-tracked copy of the same tree: `Ran 18 tests … OK`, 0 skipped, exit 0** — so the merged registry passes the round-7 contradiction lint, the packet required-set rule *and* the tracked-bytes closure rule. |
| `test/encounter-run.py` | **0** | `Ran 24 tests in 24.723s … OK` |
| `test/install.sh` probe assertions (lines 1-47 setup + **94-187 verbatim**) | **0** | 5 PASS from the cited range + the fixture production-isolation PASS: `install ledger probe driven false: contiguous=false rc=3`; `refuses to run against the ledger it is protecting; subject unchanged (10 rows, sha aca03f15103a4d7a)`; `seeded probe ledger still holds its 10 seeded rows after three probes`; `an absent or empty subject refuses subject-missing rc=3, naming the subject`; `an anchored append landing between the two copies still reads contiguous=true` |
| `test/install-source-gate.sh` | **0** | 49 checks, 0 failed (§2a) |

Every fixture run also printed `PASS production ledger content isolated` — 0 unrelated new events on
most runs; the 8/25/1 "unrelated new events" on `ledger.sh`, `bookkeeping-autofix.sh` and
`ledger-anchor.sh` are another live process appending to the production ledger during my run, not
contamination (no witness event IDs leaked).

**I did not run `bash test/run.sh`** — in the worktree because I was told not to, and from the
export because it stops where the report says it does and the individual witnesses above are the
stronger evidence. The report's full-suite line (exit 1, 307 PASS, stopping at
`SCOPED CIRCUITS OWED` inside the installer's nested run, then `INSTALL NOT PROVEN` and a clean
`ROLLBACK OK`) is consistent with everything I measured, and I did not independently reproduce it.

---

## 5. The install decision — GO-WITH-FIX, and why

### What is currently on the seat

```
$ cat /home/forge/bin/INSTALL.PROVEN
PROVEN=true stamp=20260914T054127Z backup=/var/tmp/forge/ship/backups/20260914T054127Z   ← no source=
$ grep -c fix-branch-ambiguous   /home/forge/bin/ship            → 3
$ grep -c BATTERY_LEDGER_APPEND  /home/forge/bin/receipt-chain    → 1
$ grep -c anchor-repair          /home/forge/bin/lib/ledger.py    → 0
$ ls /home/forge/bin/install.sh                                   → does not exist
```

So `~/bin` is `06fa57e` vintage: bookkeeping restored, **the rounds 5-7 anchor work absent**, and
`install.sh` itself not installed (it entered `FILES` only on the `bd514cd` side). The merge is the
right thing to install, the stamp has no `source=` so the gate passes as a no-op, and this install
is the one that **arms** the ratchet.

### The thing that has never been run

`test/circuits.py` keys its retained proof on
`closure_id = sha256(json of {name: sha256(DEST/name)} for the 109 inventory names)`. For the
merged tree that is:

```
closure_id = 38d4b9a0abc0bf9eceb2ce7f28275a177d169d313a567b40d3b20bd4416b5252
/var/tmp/forge/ship-v3.10-fx/scoped-circuits/38d4b9a0…   exists=False
/var/tmp/forge/ship-v313-fx/round8/fx/scoped-circuits/38d4b9a0…  exists=False
```

**No retained index exists, and none can be created in advance** — the closure is computed from the
*installed* bytes, which do not exist until the install has already replaced `~/bin`. So with
`RUN_SCOPED_CIRCUITS=1`, the sequence on the seat is:

1. `~/bin` is replaced (109 files, `install.sh` **added**), `INSTALL.PROVEN` → `PROVEN=false source=13aef7d…`;
2. the nested `test/run.sh` runs against the new bytes — bookkeeping, …, `install-source-gate`
   (this *is* where the installed `install.sh`'s gate gets exercised), then **`circuits`**;
3. `circuits.py:31-34` writes `<closure>/claude-attempt.json` **before** launching, then runs
   `run build` for a live `claude` circuit, then the same for `codex`.

If either launch fails: `SCOPED CIRCUIT FAILED` → nested run non-zero → `INSTALL NOT PROVEN` →
`install_exit` rolls back, `~/bin` returns to today's `06fa57e` state with its old `INSTALL.PROVEN`
(**the ratchet stays unarmed**) — *and the attempt file survives the rollback*, so the next attempt
refuses with `SCOPED CIRCUIT OWED: prior attempt retained; Fable owns another launch` until a human
deletes it. That is an expensive, one-shot, manually-unblocked step wired into an install, and it
has not been demonstrated once.

**Before typing the command:**

* confirm the `claude` and `codex` subscription clients the `run build` path needs are actually
  available on this box (a bounded `RUN_SCOPED_CIRCUITS=1 bash test/circuits.sh` from a scratch
  export, against the *source-tree* closure, proves the launcher works even though it cannot
  pre-warm the install's closure);
* know the unblock: on failure, remove
  `<WITNESS_ROOT>/scoped-circuits/38d4b9a0…/{claude,codex}-attempt.json` before retrying;
* note that `~/bin` is in the new state for the whole nested-suite window, which under
  `RUN_SCOPED_CIRCUITS=1` now includes two live agent circuits — much longer than a normal install,
  and `target-scripts-are-running` only refuses at gate time;
* the rollback path is exercised (the report's full-suite line shows
  `ROLLBACK OK … added-files-removed=true`), which matters here because `install.sh` is an
  *added* file.

If the circuits path cannot be made to work today, the honest state is: **the merge is correct and
ready, and it cannot be proven installed on this box** — which is the same boundary the report
states, and is a NO-GO on *proving* the install, not on the merge.

---

## 6. Further findings (none is a merge defect; all are ratchet gaps)

**F-1 — MAJOR. The real-install wiring of the gate is unwitnessed.** Mutant B, §2d. Fix: a
real-install-path refusal witness. Class: *a control proven only through its test entry point*.

**F-2 — MAJOR. A dirty source worktree records only `HEAD`, and the gate then blesses dropping the
uncommitted work.** `install.sh` installs *working-tree* bytes (`cp "$SRC/$f"`), but `source_gate`
records `git -C "$src" rev-parse HEAD`. There is no `status --porcelain` / `diff --quiet` check
anywhere in `install.sh`. Demonstrated:

```
# install from srca with an uncommitted local fix in the tree
INSTALL SOURCE-STAMP ok … source=b88984c…            (the uncommitted fix is invisible in the record)
# later, a CLEAN descendant of that same HEAD
INSTALL SOURCE-GATE ok candidate=43487de… …   rc=0   ← silently drops the uncommitted fix
```

This is exactly the class round 8 exists to close, and it is directly live for this program: rounds
1-7 were installed from uncommitted worktrees, and the report itself believed this merge was
uncommitted. Fix: refuse, or record `dirty=true` and treat a dirty recorded source as
non-descendant-by-default.

**F-3 — MEDIUM. `--source-stamp`'s confinement is relative to a caller-controlled variable.** The
predicate is only "`LEDGER_FIXTURE=true` and the destination is under `ledger.fixture_root()`".
Widen the root and any directory qualifies:

```
$ LEDGER_FIXTURE=true LEDGER_FIXTURE_ROOT=/var/tmp/forge install.sh --source-stamp /var/tmp/forge/ship-v313-fx/fakebin …
INSTALL SOURCE-STAMP ok destination=/var/tmp/forge/ship-v313-fx/fakebin PROVEN=true stamp=… source=…   rc=0
```

It never checks the destination against the installer's own default `DEST`, nor requires the
destination to be one the witness created. A seat that set `LEDGER_FIXTURE_ROOT=/home/forge` for any
reason could mint `PROVEN=true` on the real `~/bin`. Cheap fix: also hard-refuse the default `DEST`
and anything not under `$CASE`, independent of the fixture root.

**F-4 — MEDIUM. An install from a non-git source disarms the ratchet for the next install.**
`candidate=unknown` → `INSTALL_SOURCE_SHA=unknown` is written, and the gate's
`[ "$installed" != unknown ] || return 0` then makes the *next* install a silent no-op. This is the
witness's own check 8 (`source=unknown gate rc (0)`), i.e. deliberate — but it means installing once
from a tarball or a `git archive` export silently removes the memory the ratchet depends on. At
minimum it should print a warning naming that the gate is unarmed.

**F-5 — MEDIUM. The bookkeeping witnesses are not shipped, and `test/run.sh` skips silently.**
`FILES` ships `test/install-source-gate.sh`, `test/measure.py`, `test/ledger-anchor.sh`,
`test/encounter-run.py`, `test/ledger.sh`, `test/legacy-writer.py` — but **not**
`test/bookkeeping.sh`, `test/bookkeeping.py`, `test/bookkeeping-autofix.sh` (grep of `FILES`
returns 0 for `bookkeeping`). `test/run.sh`'s loop is
`[ -f "$(dirname "$0")/$t.sh" ] || continue`, so when the installed tree is the witness source those
three witnesses vanish **with no message**. The report records this as a deliberate disagreement;
the asymmetry with round 8 shipping its own witness is what makes it worth naming, and a silent
`continue` on a missing witness is the wrong failure mode either way. (`test/run.sh` and
`test/fixture-env.sh` are themselves unshipped — pre-existing, not this round's.)

**F-6 — INFO. Refusal ordering changed.** The source gate now precedes
`target-scripts-are-running`. Defensible, undocumented. §3.

---

## 7. What remains owed

| owed | owner | why it is still owed |
|---|---|---|
| A real-install-path witness for the gate (F-1) | builder | mutant B is green today |
| Dirty-source-tree handling (F-2) | builder | the recorded sha can disagree with the installed bytes |
| `--source-stamp` destination hard-refusal (F-3) | builder | confinement rides on a caller-set env var |
| A complete `test/run.sh` | Astra seat executor | circuits needs live subscription clients |
| `RUN_SCOPED_CIRCUITS=1 bash install.sh` demonstrated once | Astra seat executor | never run; closure `38d4b9a0…` has no index and cannot be pre-warmed; a failed attempt is durably retained |
| An end-to-end `INSTALL OK` carrying `source=` | Astra seat executor | the ratchet is not armed until an install reaches `PROVEN=true` |
| Push of `13aef7d` (it **is** committed) | the seat | no remote ref contains it |
| Correct `REPORT.md`/`report.edn`: "not committed" → committed-not-pushed; "47 entries" → 48 | the seat | the record disagrees with the tree |

---

### Reviewer's boundary

Read-only throughout. Every mutant and witness ran from a `git archive HEAD` export at
`/var/tmp/forge/ship-v313-fx/review8/` (and a git-initialised copy `review8g/` solely to un-skip one
`measure.py` test), with `WITNESS_ROOT=/var/tmp/forge/ship-v313-fx/review8-fx`. I never edited,
committed or pushed in the worktree; never wrote `~/bin`, `/var/tmp/forge/ledger/events.jsonl` or
`/tmp`; never ran `pkill`, `pgrep -f`, or an outer `timeout`; and never ran `bash test/run.sh` in the
worktree. I did **not** reproduce the full-suite line, the red `install-source-gate` run against
`bd514cd`, or any claim about a live install.
