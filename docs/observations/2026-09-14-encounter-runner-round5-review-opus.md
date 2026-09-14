# Independent review — v3.13 round 5 (`v3.13-round5`, `03d9af2a`, one commit on `7f40ffa`)

**NO-GO** for installing this round on the seat.

**CLASS:** every protective mechanism this round adds was witnessed against a subject it had
already changed or never reached. `install.sh --ledger-probe` **deletes the ledger it is
protecting before it refuses to probe it** (proven: 5 rows → 0). The in-installer probe line
(`install.sh:307`) has **never executed**, and the only witness that would execute it
(`test/install.sh`) **destroys its own probe fixture** at line 89 and would fail the next install
in the same file even after the circuits gate is lifted. A control proven only against a subject
it mutated, or never run at all, is `:unverified` — not green. Independently: installing this
round on the live seat ledger flips `read()` from **0 unknown rows to 3,253 `ledger-chain-broken`**,
and `lib/measure_store.py:20` (`chain_broken = bool(snapshot['unknown'])`) turns **every encounter
score into `untested` for the life of that file**. The line stays up and the scoreboard goes dark;
that trade is not stated anywhere in `REPORT.md` and has no remedy in the tree.

**Oracle** (cheapest rung that makes the class unrepresentable): the probe must resolve its
destination and refuse `probe-ledger-is-production` **before** `rm -rf`, with a witness that
asserts the subject's byte count is unchanged after the refusal; and `read()`/`score()` must gain
a witness that asserts the live production ledger's *actual* unknown count before and after an
install, so "detection moved" is a measured number rather than a sentence.

The root-cause analysis is **correct**, the RED and GREEN reproduce independently, mutants A, B and
C all die, and the round-4 refusal matrix survives the new repair verb. The NO-GO is about
installing on this evidence, not about the diagnosis or the core design.

---

## 1. Is the root-cause analysis correct?  **Yes — verified, with one correction and one addition.**

### 1a. The preserved anchor is byte-identical to the live one

```
$ sha256sum /var/tmp/forge/ledger/events.jsonl.anchor \
            /var/tmp/forge/ship-v313-fx/events.jsonl.anchor.broken-20260913T1855Z
5f8dbbc9c424078cd657e62cf01332a1f564703f3245f36b6ce008c040b3f278  .../events.jsonl.anchor
5f8dbbc9c424078cd657e62cf01332a1f564703f3245f36b6ce008c040b3f278  .../events.jsonl.anchor.broken-20260913T1855Z
```

Three rows, `10211, 10212, 10213`, strictly contiguous. No duplicate anchor row anywhere. The
P0's "duplicate sequence rows `10211,10212,10213,10212,10213`" hypothesis is **not supported**;
the builder is right to reject it.

### 1b. 10211–10213 are v2 chained; 10214 is v1 unchained from an older-vintage process

Read-only scan of the live ledger:

```
10208 ship-ledger/v1 heartbeat 2026-09-13T18:54:00Z NO-PREV run=1facdf71 epoch=3b2a8181 wid=None
10209 ship-ledger/v1 heartbeat 2026-09-13T18:54:26Z NO-PREV run=8fffa81a epoch=4a608c51 wid=None
10210 ship-ledger/v1 heartbeat 2026-09-13T18:55:00Z NO-PREV run=1facdf71 epoch=3b2a8181 wid=None
10211 ship-ledger/v2 requested 2026-09-13T18:55:02Z prev_ck run=b4fe5af7 epoch=4f3bf0d4 wid={pid 1994473}
10212 ship-ledger/v2 started   2026-09-13T18:55:02Z prev_ck run=b4fe5af7 epoch=4f3bf0d4 wid={pid 1994477}
10213 ship-ledger/v2 terminal  2026-09-13T18:55:03Z prev_ck run=b4fe5af7 epoch=4f3bf0d4 wid={pid 1994477}
10214 ship-ledger/v1 heartbeat 2026-09-13T18:55:26Z NO-PREV run=8fffa81a epoch=4a608c51 wid=None
10215 ship-ledger/v1 heartbeat 2026-09-13T18:56:00Z NO-PREV run=1facdf71 epoch=3b2a8181 wid=None
```

Confirmed exactly as reported: 10214 carries epoch `4a608c51`, the same epoch as 10209 written
**26 seconds before the install** — a supervisor that had already imported the pre-anchor module.

**Addition the report misses:** epoch `3b2a8181` (run `1facdf71`) wrote 10210 *before* the install
and 10215 *after* it. There were at least **two** old-vintage supervisors alive across the install,
not one. This strengthens the diagnosis; a single-writer explanation would have been the weaker one.

### 1c. The two predicates, on the live files

```
$ python3  # both modules loaded by path; live bytes read, never written
ledger bytes 13084867 rows 13460
7f40ffa anchor_mismatch(live) : True   0.13s
round5  anchor_defect(live)   : None   0.13s
round5  anchor_mismatch(live) : False  0.14s
```

Reinstalling `7f40ffa` unchanged would stop the seat line on its first one-shot. Confirmed.
The installed byte at `~/bin/lib/ledger.py` is `09c9c10d…` = the v3.12 rollback, as claimed.

### 1d. RED reproduced on a pristine `7f40ffa` export

```
$ git archive 7f40ffa | tar -x -C .../review5/base-7f40ffa
$ cp test/ledger-anchor.sh test/legacy-writer.py base-7f40ffa/test/
$ WITNESS_ROOT=.../review5/red-fx bash base-7f40ffa/test/ledger-anchor.sh   # EXIT=1
ValueError: ledger-anchor-mismatch          (lib/ledger.py line 157, append)
  anchor seq: [5, 6]
  ledger seq: [1 … 32]
```

Same shape as the builder's receipt (`[5]` / 31 rows — the difference is only scheduling).
GREEN at HEAD reproduced too: `WITNESS_ROOT=… bash /var/tmp/forge/ship-v3.13/test/ledger-anchor.sh`
→ `EXIT=0`, "58 rows, anchor 5..57", all three PASS lines.

### 1e. What the report gets wrong

> "The installer's running-target gate reads argv from `/proc`; a Python supervisor holding an old
> import is invisible to it."

**False as stated.** See §5: argv names `lib/run.py` exactly, in the very form the gate already
matches. The gate is blind because `EXEC` — the target *list* — contains only the ~/bin wrapper
scripts. That matters because it converts "no lock can see this" into "a one-line list change can".

---

## 2. Fail-closed at append vs. round 5's lag design — and a third design

**For fail-closed (7f40ffa).** `ENCOUNTER-RUN-020` boundary 1 is a real promise: legacy history is
allowed *only* below the first anchored sequence. A stop at `append` is the only place a bad row can
be prevented rather than described; detection at `read` is an observation nobody is obliged to act
on, and this ledger's read output is already a list nobody reads. And the field outcome —7 minutes
and a rollback— was the *gate working*, loudly, exactly once.

**Against.** The refusal was not bounded: it named no row, no offset, no remedy, and nothing in the
tree could rebuild an anchor, so the only exit was a rollback of the whole release. Worse, the stop
was **not terminating**: the offending writer was a 60-second heartbeat loop with hours of life left
(the live one on this box is 4 h 45 m old), so a bounded-refusal-plus-automatic-remedy design
degenerates into *stop, repair, stop again 60 seconds later*, for the life of every pre-install
supervisor. A gate that flaps on a timer is worse than one that records.

**The three candidates.**

| design | cost | excludes | cannot exclude |
|---|---|---|---|
| (a) bounded refusal at append + automatic `anchor-repair` | the line flaps every heartbeat interval for hours; the "repair" it runs *is* the relaxation, spelled as a manual step | nothing the relaxation doesn't already allow | any old-vintage writer — it re-appears 60 s later |
| (b) supervisor re-imports / re-execs after an install | needs a signal handler `lib/run.py` does not have; cannot be imposed on a process **already** running (which is the whole case); a per-heartbeat re-exec costs a fork per minute | future vintage races, if it ever lands | `ship-ledger.sh`, `ship-event.py`, a human's python, any other opener |
| (c) installer refuses while a `lib/run.py` supervisor is alive | blocks installs for hours behind one long child; racy (a supervisor may start one second later) | exactly the 2026-09-13 writer, and it is **now proven visible** (§5) | any other stale importer; a supervisor started after the check |

**Verdict (five sentences).** Ship round 5's core — a row above the anchor high-water mark is a lag,
not a defect — because it is the only one of the four that does not make the line's uptime depend on
a writer nobody can exclude. Add (c) **demoted from a refusal to a named warning**: the installer
should print the live `lib/run.py` supervisor pids and say that their rows will be legacy until they
exit, which costs one line in the existing `/proc` scan and stops no install. Do **not** ship (a) or
(b): (a) converts a seven-minute outage into an indefinite flap, and (b) cannot reach the process
that actually caused this. But round 5 is **not complete** without the piece none of the three
addresses — the chain, not the anchor, is what goes permanently broken on the real ledger (§6), and
the cheapest honest fix is to chain over the **v2 subsequence** (`previous_checksum` links a v2 row
to the previous *v2* row, interleaved legacy rows ignored), which still catches downgrade, delete,
rechain and reorder and restores scoring. That change needs its own red/green before it ships; I am
recommending it, not certifying it.

---

## 3. Attacking the repair

Command: `/var/tmp/forge/ship-v313-fx/review5/attack/matrix.py` — for each case, a real ledger
(2 legacy rows + 5 anchored appends), the forgery, then `probe → anchor-repair → probe`.

| case | append before repair | `anchor-repair` | after repair |
|---|---|---|---|
| payload | refuse `anchor-covered-row-altered` | **REFUSED** `ledger-anchor-covered-row-altered seq=5` | still refuses |
| rechain | refuse `anchor-covered-row-altered` | **REFUSED** same | still refuses |
| downgrade | refuse `anchor-covered-row-altered` | **REFUSED** same | still refuses |
| bad-link | refuse `anchor-covered-row-altered` | **REFUSED** same | still refuses |
| no-link | refuse `anchor-covered-row-altered` | **REFUSED** same | still refuses |
| delete | refuse `anchor-covered-row-missing` | **REFUSED** `ledger-torn ledger-seq-noncontiguous 4->6` | still refuses |
| missing (ledger unlinked) | refuse `anchor-ahead-of-ledger` | **REFUSED** `ledger-empty` | still refuses |
| reorder | **accepted** (`defect=None`); `read` reports `ledger-chain-broken` | **REFUSED** `ledger-torn` | unchanged |
| **truncate (2 anchored tail rows)** | refuse `anchor-ahead-of-ledger` | **REPAIRED**, `dropped_anchor_rows=[{seq 6},{seq 7}]` | `defect=None`, **`read` unknown = 0**, append OK |
| **truncate-tail-only (1 row)** | refuse `anchor-ahead-of-ledger` | **REPAIRED**, `dropped_anchor_rows=[{seq 7}]` | `defect=None`, **`read` unknown = 0**, append OK |
| forged row above the HWM (unchained, self-consistent) | **accepted**; carried into the anchor by the next honest append | REPAIRED (no-op) | `read` still reports `ledger-chain-broken` |

**Answers.** Repair cannot launder a **rewritten or reordered** row: checksums are read from the
ledger rows, so any edit inside the anchored run either fails the `altered` test or fails
`ledger_tear`. Seven of the nine round-4 cases still refuse after repair, each by its own reason.

Repair **does** legitimize one thing: **truncation of the anchored tail**. One supported command
turns "someone deleted the last N rows" into a ledger whose anchor matches and whose `read()`
reports nothing at all. `ENCOUNTER-RUN-024` declares this ("derivation cannot distinguish an
interrupted append from truncation of the anchored tail; both are named"), and the evidence is
preserved — but only as a `.anchor.broken-<utc>` file and a `.repair-<utc>.json` receipt sitting in
the same directory, same UID, deletable, that nothing obliges anyone to read. Against `7f40ffa`
this is a real reduction in the cost of erasing tail rows: before, a truncation left a permanent
mismatch; now it is one verb.

Second laundering vector, no repair needed: **`append`'s carry-forward anchors anything above the
high-water mark**, including an unchained forged row, automatically, on the next honest append.
`read()` still flags it `ledger-chain-broken`, so it cannot reach `proven` — but the anchor
thereafter asserts those bytes. On the live ledger this means the first anchored append after an
install carries **3,254 legacy rows** into the anchor in one transaction (measured, §6).

**Recommended ratchet (cheap, keeps the automatic case automatic):** split the verb. `gap_closed`
(a lag, or an interrupted append) repairs automatically. `dropped_anchor_rows` non-empty is
destructive and must require an explicit `--drop-anchored` argument, so truncation cannot be
cleared by the same reflex that fixes a lag.

**One asymmetry worth naming:** `append` accepts a state (`reorder`) that `anchor-repair` calls
`ledger-torn`. The writer is more permissive than its own remedy.

---

## 4. Mutant sensitivity

Scratch copies under `/var/tmp/forge/ship-v313-fx/review5/{mut,mutB,mutC}`; the worktree was not touched.

**Mutant A — carry-forward removed** (`carried=[]` in `append`):

```
$ ROOT=.../mut bash .../mut/test/ledger-anchor.sh          # EXIT=1
AssertionError: v3.13-writer exited 1
ValueError: ledger-anchor-mismatch defect=anchor-seq-noncontiguous file=anchor seq=6->9 offset=234 remedy=…
  anchor seq: [5, 6, 9]
```
**Killed.**

**Mutant B — the exclusive lock around sequence derivation removed** (`fcntl.flock` deleted):

```
$ ROOT=.../mutB bash .../mutB/test/ledger-anchor.sh        # EXIT=1
AssertionError: ledger is not contiguous
  ledger seq: [... 38, 39, 39, 40 ...]
$ ROOT=.../mutB bash .../mutB/test/ledger.sh               # EXIT=1
```
**Killed by both witnesses.**

**Mutant C — the probe's driven-false path removed** (`verdict()` always prints `contiguous=true`,
exits 0). Driven through the same broken-anchor fixture `test/install.sh` seeds:

```
HEAD   : INSTALL LEDGER-PROBE ok writers=2 seq=11..18 contiguous=false reason=anchored-writer-refused:… rc=3
MUTANT : INSTALL LEDGER-PROBE ok writers=2 seq=11..18 contiguous=true                                  rc=0
ASSERTION (test/install.sh:80-83): mutant KILLED
```
The assertion **does** catch it — when it runs.

**Can it be exercised without subscription clients?** Only by hand, the way I just did
(`install.sh --ledger-probe <ledger> <dir> <lib>` against a seeded fixture). It cannot be reached by
any suite. I ran `test/install.sh` to completion:

```
$ WITNESS_ROOT=.../instfx bash /var/tmp/forge/ship-v3.13/test/install.sh   # TEST_INSTALL_EXIT=1
line 2735: SCOPED CIRCUITS OWED: RUN_SCOPED_CIRCUITS=1 test/run.sh, subscription clients required
line 2738: INSTALL NOT PROVEN — v3.9 witness failed against installed bytes
$ grep -c LEDGER-PROBE .../test-install.out   → 0
```

It dies at the **first** install, `test/install.sh:54`. All four round-5 probe assertions (lines
55–95) are unreachable. **Is that acceptable for an install gate? No** — and for a reason beyond the
circuits gate:

1. The gate that protects the seat is the **in-installer** call at `install.sh:307`, whose arguments
   (`$CASE/ledger-probe`, `$DEST/lib`, `$SRC/test/legacy-writer.py`, the sourced fixture env) have
   **never been executed even once**. The `--ledger-probe` CLI path shares the function, not the call
   site. Note `install.sh:307` also sits *after* `test/run.sh` at line 304, so on this box it is
   currently unreachable for the same reason.
2. `test/install.sh` is **red by construction**, not merely blocked. Line 89 calls the probe with
   `dir = $(dirname "$INSTALL_PROBE_LEDGER")`, `ledger_probe` runs `rm -rf "$dir"` **before** the
   guard fires, and line 96 restores only the *anchor*. Reproduced:

```
seeded: 10 ledger rows, 1 anchor rows
after test/install.sh's restore: 0 ledger rows, 1 anchor rows
next install's probe: INSTALL LEDGER-PROBE … contiguous=false reason=anchored-writer-refused:…anchor-ahead-of-ledger… rc=3
```

   Every later install in that witness would report `INSTALL NOT PROVEN`. Refreshing the circuits
   index will not make this file green.

**Bonus finding — a one-word fix that arms all of round 5's witnesses.** `test/run.sh:21` orders the
suite `… publication circuits ledger ledger-anchor lifecycle …`. `circuits` is a *retained-index*
gate, not a correctness precondition, and it sits **before** every witness this round adds. Move it
to the end of that list and `ledger-anchor` (and eight others) run in the default suite today.

---

## 5. The install gate and `/proc`: the builder's claim is **wrong**, and the fix is one line

There is a live pre-install-vintage supervisor on this box **right now**, found via the pid files as
instructed (pid file → child pid → `PPid`), never `pgrep -f`:

```
$ P=$(cat /var/tmp/forge/run-bg/756c82ff-5ca2-41f1-997c-658c644abc62/pid)   # 4018065 (the JVM child)
$ awk '/^PPid:/{print $2}' /proc/$P/status                                  # 4018048
$ tr '\0' ' ' < /proc/4018048/cmdline
/usr/bin/python3 /home/forge/bin/lib/run.py supervise /var/tmp/forge/run-bg/756c82ff-…/config.json
$ ps -o lstart=,etime= -p 4018048
Mon Sep 14 02:31:29 2026    04:44:49
```

It is writing `ship-ledger/v1` heartbeats to production every 60 s (tail of the live ledger:
`13460…13464 ship-ledger/v1 heartbeat 07:11:34Z…07:15:34Z run=756c82ff`).

argv[1] is the **exact absolute path** `<DEST>/lib/run.py`, a complete NUL-delimited entry — the
precise form `install.sh:165-200` already matches. Running the installer's own scan with
`lib/run.py` added to the target set:

```
$ DEST=/home/forge/bin EXEC_EXTRA="… run-bg sol-yolo board await lib/run.py" python3 <installer scan>
  4018048:lib/run.py
```

So `/proc` is not the limitation; `EXEC` is. The installer **can** see it.

**What it should do: warn, not refuse.** Refusing would block installs for the 4 h 45 m (and
counting) that one long child lives, and it still could not exclude a supervisor that starts a second
later. The right output is an observation with names — *"3 `lib/run.py` supervisors hold a
pre-install import (pids …); their rows will be `ship-ledger/v1` until they exit"* — which costs one
entry in the existing scan, stops nothing, and would have made the 2026-09-13 outage diagnosable in
seconds instead of seven minutes. Pair it with the §2 chain fix, and the warning becomes purely
informational.

---

## 6. Verdict, the field consequence, and what remains owed

### The thing that decides it

```
$ python3   # both modules loaded by path, live production ledger, read-only
INSTALLED v3.12 read(live production): events=13466 unknown=0    {}
ROUND5     read(live production): events=13466 unknown=3253 {'ledger-chain-broken': 3253}
```

`lib/measure_store.py:20` → `chain_broken = bool(snapshot['unknown'])`; `lib/encounters.py:185,193`
→ `if events.chain_broken: return p` with `transfer='untested'`. **Installing round 5 today turns
every encounter score on this seat into `untested`, permanently, on a ledger that reads clean under
the currently installed v3.12.** `anchor-repair` does not touch the chain; there is no rotate or seal
verb; the count grows by ~6,000 rows/day for as long as any pre-install supervisor lives. "Detection
moved, it did not go away" is true, and it undersells the cost by the whole scoreboard.

The status quo (v3.12 installed) is currently better on uptime *and* on scoring, and worse only on
integrity enforcement — so there is no urgency that outweighs fixing the above first.

### Also measured (owed, not blocking)

Append cost at real production scale, on a copy of the live ledger:

```
ledger rows 13466 (13.1 MB), anchor rows 3
append #1 (carries 3,254 rows into the anchor): 0.29s   → anchor rows 3257 (0.4 MB)
appends #2-6: 0.293 0.271 0.271 0.269 0.268  mean 0.274s
v3.12-shaped tail read + seq derive:          0.019s
```

~**14× per append**, O(n) in ledger size, twice per append (`anchor_defect` then `parse_rows` for the
carry), with a second file of equal cardinality and no compaction or rotation story. At the current
growth rate this is ~1.2 s per append within a week. The anchor protocol needs a rotation plan.

### Confirmed green

Manifest verifies (`sha256sum --quiet -c MANIFEST.staged.sha256` → rc 0, 96 entries, new files
present). Registry lint clean. `ENCOUNTER-RUN-022..025` are well written and their `:misreadings`
are the right ones.

**Registry inconsistency:** `ENCOUNTER-RUN-020` is **unmodified** by this commit — its boundary 1
still reads *"Legacy history is allowed only below the first anchored sequence"* — while the new
`ENCOUNTER-RUN-022` lists *"A legacy row above the first anchored sequence must refuse every later
append"* as a **misreading**. Two `:status :active` intents now contradict each other in the same
file, and the lint is clean (errors: 0) because it does not compare boundaries across entries. If
the relaxation is accepted, 020 must be amended or superseded in the same commit; if it is refused,
022 must go. Right now the registry records both positions.

### Owed

1. **The probe must refuse before it destroys.** Resolve `dir` vs `origin` and refuse
   `probe-ledger-is-production` **ahead of `rm -rf "$dir"`**, plus a witness asserting the subject's
   byte count is unchanged after the refusal. (Blocking.)
2. **Fix `test/install.sh:89-96`.** It deletes its own seeded probe ledger and restores only the
   anchor; every later install in the file would then report `INSTALL NOT PROVEN`. (Blocking —
   the round's install gate has no working witness without it.)
3. **State and decide the scoring consequence.** 0 → 3,253 `ledger-chain-broken` on the live ledger,
   `transfer='untested'` for every score. Either accept it explicitly, or land the v2-subsequence
   chain (§2) with its own red/green. (Blocking.)
4. **Correct the record in `REPORT.md`** — argv *does* name `lib/run.py`; the gate is blind because
   of `EXEC`, not `/proc`. Add the second supervisor (epoch `3b2a8181`, rows 10210/10215).
5. **Reconcile `ENCOUNTER-RUN-020` boundary 1 with `-022`**, and add a lint rule that a new intent's
   `:misreadings` may not restate an active intent's `:boundaries`.
6. **Split `anchor-repair`**: `gap_closed` automatic, `dropped_anchor_rows` behind an explicit
   destructive flag.
7. **Move `circuits` to the end of `test/run.sh:21`** so `ledger-anchor` and eight other witnesses
   run in the default suite instead of sitting behind a subscription gate.
8. **Installer warning** naming live `lib/run.py` supervisors (one entry in the existing `/proc` scan).
9. **Anchor growth/rotation plan**, with the 0.27 s-per-append measurement as its baseline.
10. Carried from the builder: fresh scoped circuits for this closure; field verification of a
    reinstall on the real ledger; independent acceptance of the 020 relaxation;
    `lib/ledger-probe.py` absent from `lib/packet.py`'s `required` set (fails closed — an installer
    from a packet without it refuses to install — but it should be in the set).

---

*Review performed read-only. The worktree, `~/bin`, and `/var/tmp/forge/ledger/events.jsonl` were
never written. All mutants and attacks ran under `/var/tmp/forge/ship-v313-fx/review5/`. The one
process I started (`run-bg reviewprobe sleep 90`, §5 method check) wrote only to a fixture ledger
under that root; `fixture_assert_production` confirmed 0 unrelated events from my witnesses.*
