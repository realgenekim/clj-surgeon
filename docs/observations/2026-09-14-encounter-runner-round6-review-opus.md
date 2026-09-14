# Independent review — v3.13 round 6 (`v3.13-round5`, `5f95c97`, one commit on `03d9af2`)

**GO-WITH-FIX** for installing `5f95c97` on the seat with
`RUN_SCOPED_CIRCUITS=1 PYTHONDONTWRITEBYTECODE=1 bash install.sh`.

Every one of the three blocking items from my round-5 NO-GO is closed **in code**, and each one is
proven here by a command, its output, and a mutant that dies. The round-5 class — *a protective
mechanism witnessed against a subject it had already changed or never reached* — does not survive
this round: I reproduced the round-5 destruction (10 seeded rows → 0, anchor gone) and then showed
the round-6 probe refusing **before** any mutation with the subject byte-identical; I executed the
new `test/install.sh` probe assertions verbatim by hand and killed a mutant that restores the
round-5 ordering; and I confirmed on a copy of the live production ledger that `read()` goes
**3,322 `ledger-chain-broken` → 0**, `chain_broken False`, and `register()` from **refused
`ledger-corrupt`** to **OK** — which is a bigger win than my round-5 review claimed, because round 5
stopped `measure` itself, not only the score.

**The FIX (post-install, none of it gates the install):**

1. **`ENCOUNTER-RUN-022` boundary 2 is now false as shipped** — *"An unchained row above the floor is
   still reported by read as ledger-chain-broken"* — and the new reconcile lint **cannot see it**: it
   compares misreadings to boundaries only, never boundary to boundary, and the `:reconciles`
   declaration on 020 **blanket-exempts the 020/022 pair in both directions** (proven below: a fresh
   contradictory misreading injected on 022 lints clean). This is my round-5 item 5 recurring one
   entry to the left, and the lint was built to the shape of the found instance rather than to the
   class.
2. **The probe's receipt does not name its subject.** `INSTALL LEDGER-PROBE ok writers=2 seq=…
   contiguous=true` never says which ledger it probed. A stray `INSTALL_PROBE_LEDGER` in the
   environment silently substitutes it, and a **missing** production ledger yields a green
   `contiguous=true` off an empty file (measured). Print the resolved origin path and its row count
   on the line.
3. **The probe copies the ledger and its anchor with two unsynchronised `cp`s outside the ledger
   lock.** An anchored append landing between them leaves the anchor **ahead** of the ledger copy and
   the gate returns `contiguous=false` → `INSTALL NOT PROVEN` on a perfectly healthy production
   ledger (reproduced below). One-line mitigation: copy the **anchor first** — anchor-behind is the
   lag this round is built to tolerate — or take both copies under the shared lock `read()` already
   uses.
4. **Astra condition (a) is implemented for *supply*, not for *override*.** `chained_only()` filters
   `current()`/`assess()`, but `emit_once()`'s `measure_key` scan reads the unfiltered snapshot, so a
   self-consistent `ship-ledger/v1` row planted above the high-water mark makes `score()` return
   `transfer=proven` with the attacker's own text (measured). **Not a regression** — an ordinary
   same-UID `ledger.append` reaches the same hole identically in round 5 and round 6 — so it does not
   block, but the boundary as written in the amended 020 ("may neither supply nor override") is not
   yet true.

Installation is de-risked by the installer's own `install_exit`: any failure after the destination is
mutated auto-rolls back (`ROLLBACK OK …` is in the round-6 suite receipt), and the manual escape
`bash install.sh --rollback <backup>` is unchanged.

---

## 1. Blocking item 1 — the probe refuses before it destroys **CLOSED**

### 1a. Round-5 destruction reproduced

Seeded subject: 10 rows (6 legacy, an anchor floor, 3 legacy above it), 1 anchor row.

```
$ bash .../tree5/install.sh --ledger-probe "$SUB" "$(dirname "$SUB")" .../tree5/lib
BEFORE: rows=10 sha=70af9755eb983c66 anchor=3974687c645737eb
INSTALL LEDGER-PROBE ok writers=2 seq=1..1 contiguous=false reason=probe-ledger-is-production
rc=3
AFTER : rows=0 sha=e3b0c44298fc1c14 anchor=MISSING
```

### 1b. Round 6, same command, same seeded subject

```
$ bash .../tree6/install.sh --ledger-probe "$SUB" "$(dirname "$SUB")" .../tree6/lib
BEFORE: rows=10 sha=70af9755eb983c66 anchor=3974687c645737eb
INSTALL LEDGER-PROBE ok writers=2 seq=0..0 contiguous=false reason=probe-ledger-is-production
rc=3
AFTER : rows=10 sha=70af9755eb983c66 anchor=3974687c645737eb
```

Byte-identical across the refusal, ledger **and** anchor. The three containment variants:

| `dir` | rc | line | subject after |
|---|---|---|---|
| `dirname(ledger)` (the round-5 bug) | 3 | `reason=probe-ledger-is-production` | 10 rows, sha unchanged |
| `$WITNESS_ROOT` (a directory *containing* the ledger) | 3 | same | 10 rows, sha unchanged |
| the ledger **file path** itself | 3 | same | 10 rows, sha unchanged |
| a sibling dir under the ledger's directory | 0 | `seq=11..27 contiguous=true` | 10 rows, sha unchanged |

The last row is the right answer, not a hole: `origin` is a *file*, the `rm -rf` target is the
`mkdtemp` the probe itself created, and the subject is untouched.

### 1c. Mutant — move the refusal back after the `rm -rf`

Scratch copy `review6/mut1`, `ledger_probe()` body reverted to the round-5 form (`rm -rf "$dir" &&
mkdir -p "$dir"` first; the only refusal inside `ledger-probe.py`). Everything else is round 6. The
witness assertions are `test/install.sh:107-127` **verbatim**, driven by hand (see §2 for why):

```
$ bash review6/witness_item1.sh .../tree6 head
PASS(head) install ledger probe refuses to run against the ledger it is protecting; subject unchanged (10 rows, sha adcef7c1a158b66d)
PASS(head) seeded probe ledger still holds its 10 seeded rows
PASS(head) probe-later rc=0 : INSTALL LEDGER-PROBE ok writers=2 seq=11..27 contiguous=true
HEAD EXIT=0

$ bash review6/witness_item1.sh .../mut1 mutant
FAIL(mutant): subject row count changed: 10 -> 0
MUTANT EXIT=1
```

**Killed.**

---

## 2. Blocking item 2 — `test/install.sh` keeps its fixture; a second install proves **CLOSED in code; the witness still cannot execute here**

The witness now (a) records the subject's row count *and* sha256 before the self-probe and asserts
both across the refusal, (b) restores only the anchor it deliberately broke and asserts the ledger
still holds its 10 seeded rows, (c) asserts the **second** install in the file prints
`contiguous=true`. That is the right shape and it is exactly what my round-5 item 2 asked for.

**Can it execute on this box? No.** `grep -c LEDGER-PROBE test/receipts/round6-full-suite.log` →
**0**. The round-6 full suite still stops at `test/install.sh`'s *first* `bash "$ROOT/install.sh"`:

```
5553: SCOPED CIRCUITS OWED: RUN_SCOPED_CIRCUITS=1 test/run.sh, subscription clients required
5556: INSTALL NOT PROVEN — v3.9 witness failed against installed bytes
5557: INSTALL REFUSED destination was already replaced; PROVEN=false; … rolling back including added files
5558: ROLLBACK OK destination=… backup=… added-files-removed=true
```

So the builder's item-2 GREEN row comes from a hand-driven harness
(`test/receipts/round6-install-probe.log`), not from `test/install.sh`. That is stated honestly in
the round-6 residue, and it is not a defect in the fix.

**What I executed by hand:** the four probe outcomes against the seeded production-shaped fixture,
plus the two assertions the round-5 file could not survive.

```
1 probe-ok      rc=0 : INSTALL LEDGER-PROBE ok writers=2 seq=11..27 contiguous=true
2 probe-self    rc=3 : INSTALL LEDGER-PROBE ok writers=2 seq=0..0 contiguous=false reason=probe-ledger-is-production
                       subject rows 10 -> 10 ; sha adcef7c1a158b66d -> adcef7c1a158b66d
3 probe-later   rc=0 : INSTALL LEDGER-PROBE ok writers=2 seq=11..27 contiguous=true     <- round 5 failed here
```

**One circuits pair unblocks it.** `test/circuits.py` keys its retained index on
`sha256(name → sha)` over the sealed inventory. I computed both:

```
staged entries: 104   FILES entries: 104   symmetric difference: {} / {}
closure_id(MANIFEST.staged.sha256) = fb7f71c7f327494f82426909
closure_id(install.sh FILES at DEST) = fb7f71c7f327494f82426909
```

Identical, so the circuits the seat's install launches also satisfy a later
`bash test/run.sh` / `test/install.sh` at the same `WITNESS_ROOT`. **After the install, run
`bash test/run.sh` and require the four probe assertions to appear** — that is the owed item this
review cannot discharge.

---

## 3. Blocking item 3 — the v2-subsequence chain **CLOSED**

### 3a. On a copy of the live ledger (`cp` only; production never opened for writing)

Copy: 13,535 rows / 13.1 MB, anchor 3 rows `10211,10212,10213` (`sha256
5f8dbbc9c424078c…`, still byte-identical to the preserved `.anchor.broken-20260913T1855Z`).

| on the same bytes | round 5 (`03d9af2`) | round 6 (`5f95c97`) |
|---|---|---|
| `read()['unknown']` | **3322**, all `ledger-chain-broken` | **0** |
| `read()['legacy']` | — (no such key) | **3322**, all `legacy-row-above-floor` |
| legacy producers | — | `run-bg/v3.9` 1886, `ship/v3.9` 744, `build/v3.10` 692 |
| `measure_store.LedgerEvents.chain_broken` | `True` | `False` |
| `encounters.register(...)` | **REFUSED** `ValueError: ledger-corrupt; preserve and reconcile before ingestion` | **OK** `ident=d1855227a57d5f88` (0.71 s) |
| `encounters.score(ident, <no such run>)` | `untested` / **`ledger-chain-broken`** | `untested` / **`run-not-produced`** |
| 10211 → 10210 transition | `previous_checksum == checksum(10210)` → `True` | `True`, and 10211 is not in `unknown` |

The round-5 column independently reproduces my round-5 finding and sharpens it: under round 5 this
ledger could not even *register*, so the P0's blast radius was `measure` itself, not just the score.

### 3b. Astra (c) — no anchored row was rewritten by the chain change

One round-6 `register()` append on the live copy:

```
ASTRA(c): prefix of ledger identical to pre-append bytes : True
ASTRA(c): sha256 before=93c8214432517419  sha256(after minus appended)=93c8214432517419  appended_rows=2
ASTRA(c): anchor rows 3 -> 3328 ; the 3 pre-existing anchor rows byte-identical: True
ASTRA(c): anchor seqs before=[10211, 10212, 10213]  after first/last=10211/13538
```

The anchor **grows** (the carry-forward, by design, 3,325 rows in one transaction) and the three
pre-existing anchor rows are byte-identical. Nothing anchored is reinterpreted or rewritten.

### 3c. Astra (b) — a rewritten anchored legacy row still refuses

Subject: the live copy *after* that append, so 3,323 legacy rows are now anchored. Rewriting one
self-consistently (payload changed **and** checksum recomputed):

```
MODE=anchored target seq=11875 schema=ship-ledger/v1 anchored=True
  read(): unknown=1 {'ledger-anchor-mismatch': 1} legacy=3323
    {"reason":"ledger-anchor-mismatch","defect":"anchor-covered-row-altered","file":"ledger","offset":11502119,"seq":11875,"previous_seq":11874,…}
  append: REFUSED ledger-anchor-mismatch defect=anchor-covered-row-altered file=ledger seq=11874->11875 offset=11502119 remedy=…
  anchor-repair: REFUSED ledger-anchor-covered-row-altered seq=11875 file=ledger
```

Named by sequence and byte offset, refused at both `append` and `anchor-repair`. The complementary
half is also right: a **tampered** legacy row above the anchor lands in `unknown`, never in the
legacy count (`test/ledger-anchor.sh` section 4 asserts it; I ran it green).

**The accepted window, reproduced.** A legacy row **above** the high-water mark, rewritten
self-consistently, is invisible and is then carried into the anchor:

```
after 3 stale v1 appends: unknown=0 legacy=3326
anchor high-water mark: 13538  ledger tail: 13541
rewriting seq=13540 schema=ship-ledger/v1 anchored=False
  read(): unknown=0 {} legacy=3326
  append: ACCEPTED -- the laundered row is now carried into the anchor
```

This is exactly the "legacy-tail laundering" Astra named and accepted. Round 5 did not detect the
*rewrite* either (the row was already `ledger-chain-broken` whether or not you touched it); what
changes is that the round-6 file reads **clean**. The mitigation Astra required is `chained_only()` —
see 3e for the half of it that is not yet true.

### 3d. Astra (a) — a planted self-consistent v1 copy of a proven run

`test/encounter-run.py` ran green from a scratch copy with my own `WITNESS_ROOT` (23 tests, OK):

```
R6-1 legacy-rows-above-floor 9 {… "transfer": "proven", "acceptance_event": "cb904ed5-…"}
R6-2 forged-legacy-run        {… "transfer": "untested", "acceptance_event": null, "reason": "run-not-produced"}
R6-3 payload   4 untested ledger-anchor-mismatch
R6-3 delete    4 untested ledger-anchor-mismatch
R6-3 reorder   4 untested ledger-chain-broken
R6-3 downgrade 5 untested ledger-anchor-mismatch
R6-3 bad-link  4 untested ledger-anchor-mismatch
R6-3 no-link   4 untested ledger-anchor-mismatch
```

The honest v2 run still proves on a ledger carrying 9 interleaved legacy rows; the planted v1 copy of
that same run scores `untested` / `run-not-produced` with a null acceptance event. Condition (a) for
**supply** holds.

### 3e. My own attack — condition (a) for **override** does not hold (non-blocking)

`chained_only()` filters the rows `current()` and `assess()` read. It does **not** filter
`emit_once()`, which returns any existing event whose `payload.measure_key` matches the key
`score()` is about to emit. A same-UID attacker who computes that key can plant it:

```
round6 baseline: unknown=0 legacy=5
  honest assess -> transfer=untested reason=run-not-produced ; measure_key=4ba92d3f56d444fb
  after plant: unknown=0 legacy=6  (planted seq 8 in legacy: True)
  SCORE RETURNS: transfer=proven reason=FORGED BY A STALE-VINTAGE ROW acceptance_event=forged-acceptance
```

Round-5 control on the same fixture: `SCORE RETURNS: transfer=untested reason=ledger-chain-broken` —
round 5 refuses, by the sledgehammer. **But this is not a round-6 regression**, because an ordinary
chained `ledger.append` reaches it identically in both:

```
round5 clean ledger: unknown=0 legacy=0 → SCORE RETURNS: transfer=proven reason=FORGED BY AN ORDINARY CHAINED APPEND
round6 clean ledger: unknown=0 legacy=0 → SCORE RETURNS: transfer=proven reason=FORGED BY AN ORDINARY CHAINED APPEND
```

So: pre-existing same-UID weakness in `emit_once`, parity with the status quo, **not blocking** — and
the amended `ENCOUNTER-RUN-020` boundary 2 ("may neither supply **nor override**") is only half
implemented. The cheap ratchet is to pass `chained_only(events)` into `emit_once`'s dedupe scan, or
to key the scan on chained rows only.

### 3f. Round-5 attack matrix, re-run against both rounds

Real ledger per case (2 legacy rows + 5 anchored appends), forgery, then `probe → anchor-repair →
probe`. **Exactly three outcomes changed; nine are identical.**

| case | round 5 | round 6 | delta |
|---|---|---|---|
| payload | refuse `anchor-covered-row-altered`; repair refuses | identical | — |
| delete | refuse `anchor-covered-row-missing`; repair `ledger-torn` | identical | — |
| reorder | append **OK**, read `ledger-chain-broken`+`sequence-gap`; repair `ledger-torn` | identical | — |
| downgrade | refuse `anchor-covered-row-altered` | identical | — |
| bad-link | refuse `anchor-covered-row-altered` | identical | — |
| no-link | refuse `anchor-covered-row-altered` | identical | — |
| rechain | refuse `anchor-covered-row-altered` | identical | — |
| missing | refuse `anchor-ahead-of-ledger`; repair `ledger-empty` | identical | — |
| **truncate** (2 anchored tail rows) | **REPAIRED** automatically → `defect=None`, read clean, append OK | **REPAIR-REFUSED** `ledger-anchor-repair-drops-anchored-rows seq=6,7 rows=2` | **tightened** |
| **truncate-tail-only** | **REPAIRED** automatically | **REPAIR-REFUSED** `…seq=7 rows=1` | **tightened** |
| **forge-above-hwm** | read `ledger-chain-broken` | read **clean** (counted as legacy) | **relaxed — the accepted design** |

The `append`/`anchor-repair` asymmetry I named in round 5 is unchanged and is now asserted
explicitly in `R6-3` rather than left implicit.

---

## 4. Item 4 — `anchor-repair` split **CLOSED**

`bash test/ledger-anchor.sh` from a scratch copy, my own `WITNESS_ROOT`, **EXIT=0**:

```
PASS anchor contiguity under two concurrent writers of different vintages: 58 rows, anchor 5..57, 26 legacy-rows-above-floor, unknown=0, 0.3s
PASS broken anchor: refusal names row and remedy; anchor-repair closes it with a receipt
PASS anchor-repair refuses a torn ledger and a rewritten anchored row, each by its own reason
PASS v2-subsequence chain: 18 rows, 8 legacy-rows-above-floor, unknown=0, transition row 5 still links to legacy row 4
PASS anchored rows keep their own checksum and order checks: a rewritten anchored legacy row refuses, a tampered legacy row above the anchor is a failure not a count
   gap_closed automatic: {"from": 5, "to": 7, "rows": 3}
   refusal: ValueError: ledger-anchor-repair-drops-anchored-rows seq=6,7 rows=2 file=ledger; … re-run it deliberately: … lib/ledger.py anchor-repair --drop-anchored
PASS anchor-repair split: a gap repairs automatically; dropping anchored rows 6,7 refuses by name until --drop-anchored is given
```

`gap_closed` is automatic with **no flag** and restores the derived anchor byte-for-byte; a refused
repair leaves no `.anchor.broken-*` behind and `append` stays refused.

**Mutant** (`review6/mut4`, the 8-line `if dropped and not drop_anchored:` refusal deleted):

```
   gap_closed automatic: {"from": 5, "to": 7, "rows": 3}
AssertionError: repair dropped anchored rows without being asked: {… "dropped_anchor_rows": [{"seq": 6, …}, {"seq": 7, …}], "drop_anchored": false}
MUTANT4 EXIT=1
```

**Killed.**

---

## 5. Item 5 — the installer warning, against the **real** `/proc` **CLOSED**

Live `lib/run.py` supervisors found as instructed (run-bg `pid` file → child pid → `PPid`, never
`pgrep -f`):

```
child=3069857 parent=3069856 cmd=/usr/bin/python3 /home/forge/bin/lib/run.py supervise /var/tmp/forge/run-bg/455ae74f-…/config.json
child=4018065 parent=4018048 cmd=/usr/bin/python3 /home/forge/bin/lib/run.py supervise /var/tmp/forge/run-bg/756c82ff-…/config.json
```

`install.sh`'s own `proc_scan()` and the round-6 warning block, extracted verbatim and run against
`PROC_ROOT=/proc`, `DEST=/home/forge/bin`:

```
--- EXEC scan (the REFUSAL list, unchanged) ---
BUSY=[]
--- lib/run.py scan (the WARNING, new) ---
INSTALL WARNING reason=older-vintage-supervisors-alive count=2
  lib/run.py supervisor pid=4018048 started=2026-09-14T02:31:30Z — it holds a pre-install import of lib/ledger.py;
    its rows stay ship-ledger/v1 until it exits. They are counted as legacy-rows-above-floor, never a chain break.
  lib/run.py supervisor pid=3069856 started=2026-09-14T08:14:02Z — it holds a pre-install import of lib/ledger.py;
    its rows stay ship-ledger/v1 until it exits. They are counted as legacy-rows-above-floor, never a chain break.
  This is an observation, not a refusal: no lock can exclude a writer that is already running.
EXIT-OF-BLOCK=0
```

Both real supervisors named, with real start times (`4018048` is the same 4 h-old one my round-5
review found), `count=2` correct, `BUSY` empty so the refusal path is untouched, and the block exits
0. This is the one line that would have made 2026-09-13 diagnosable in seconds.

---

## 6. Items 6 / 7 / 8

### 6. Suite order — **CLOSED**

`test/run.sh:24` now reads `… publication ledger ledger-anchor lifecycle ship board compatibility
production-content heartbeat fixture install circuits`. `circuits` is last and the gate itself is
untouched. Round 5's receipt: **63** PASS then `SCOPED CIRCUITS OWED`, with `ledger`, `ledger-anchor`
and seven others never run. Round 6's: **217** PASS, stopping one step later inside `install`.
Spot-checked independently from a scratch copy with my own `WITNESS_ROOT`: `ledger` rc=0,
`lifecycle` rc=0, `heartbeat` rc=0, `ledger-anchor` rc=0, `encounter-run.py` 23 tests OK.

### 7. `ENCOUNTER-RUN-020` amendment, and the reconcile lint — **PARTIAL**

Amended 020 carries Astra's three conditions as boundaries 2–4 verbatim in substance, cites the
consult in `:ratification`, and declares `:reconciles ["ENCOUNTER-RUN-022"]`. The lint fires exactly
once on the pre-amendment registry and is green at HEAD:

```
$ ROOT=review6/lintpre …  (docs/intent/registry.edn replaced with `git show 03d9af2:…`)
AssertionError: registry intents contradict: unreconciled-claim ENCOUNTER-RUN-022 vs ENCOUNTER-RUN-020
  boundary: A legacy row above the first anchored sequence must refuse every later append.
FAILED (failures=1)

$ ROOT=review6/tree6 …   (HEAD registry, both lint tests)
Ran 2 tests in 0.031s
OK
```

**But the registry still records two positions, and the lint is structurally unable to see it:**

```
022 boundary 2 : An unchained row above the floor is still reported by read as ledger-chain-broken.
020 boundary 1 : Unchained legacy rows above the first anchored sequence are permitted and counted
                 as legacy-rows-above-floor; they are excluded from the v2 chain and never set chain_broken.
-> flagged by the lint? False
```

`ENCOUNTER-RUN-022` is `:status :active` and its boundary 2 is **false as shipped** —
`lib/ledger.py:72` emits `legacy-row-above-floor` and `test/ledger-anchor.sh:85` asserts
`snapshot['unknown'] == []` for exactly that row. Two reasons the lint misses it:

1. It has **no boundary-vs-boundary rule at all** — only misreading-vs-boundary.
2. `:reconciles` is a **blanket exemption for the pair in both directions**
   (`b['id'] not in declared and a['id'] not in b['reconciles']`). Injecting a brand-new
   contradictory misreading on 022 about 020's boundary 1 lints clean: `conflicts: []`.

This is my round-5 item 5 recurring one entry to the left. **Fix:** amend 022 boundary 2 to the lag
design (or supersede it), and make the lint compare *boundaries* across active intents, with
`:reconciles` scoped to the specific claim it reconciles rather than to the whole pair.

### 8. `lib/packet.py` required set — **CLOSED**

`test_packet_required_covers_shipped_library` is green at HEAD and is a **class** rule
(`shipped_lib - required == {}` and `required - names == {}`), not the two names.
`lib/ledger-probe.py` and `lib/production_content.py` are both in the set.
`sha256sum --quiet -c MANIFEST.staged.sha256` → rc 0, 104 entries.
`test/receipts/round6-lint.log`: `linting took 5ms, errors: 0, warnings: 0`.
The record correction I owed in round 5 is made: `REPORT.md:15,33` retracts the `/proc` claim and
names `EXEC` as the blindness, and `REPORT.md:35` adds the second supervisor (epoch `3b2a8181`, run
`1facdf71`, rows 10210/10215).

---

## 7. The remaining gap: what the real install will print and gate on

`install.sh:368` is `ledger_probe "${INSTALL_PROBE_LEDGER:-/var/tmp/forge/ledger/events.jsonl}"
"$CASE/ledger-probe" "$DEST/lib"`. `$CASE` is a `mktemp -d` under `$WITNESS_ROOT`
(default `/var/tmp/forge/ship-v3.10-fx`), so it neither is, contains, nor sits inside
`/var/tmp/forge/ledger` — the new resolver passes and the probe proceeds on a **copy**.

**I ran exactly those arguments against the live production ledger with the round-6 bytes:**

```
live ledger rows: 13533  sha: 9f4307e905837d80
INSTALL LEDGER-PROBE ok writers=2 seq=13534..13550 contiguous=true
real 0m3.357s
PROBE rc=0
live ledger after: rows=13533 sha=9f4307e905837d80
```

So under `RUN_SCOPED_CIRCUITS=1 bash install.sh` the install will, **if the nested suite gets
there**, print one line — `INSTALL LEDGER-PROBE ok writers=2 seq=<tail+1>..<tail+17>
contiguous=true` — in about 3.4 s, rc 0, then `check_staged_drift`, the staged-manifest check,
`fixture_assert_production`, `INSTALL.PROVEN`, and `INSTALL OK v3.9 stamp=… files='…'`. On any
failure it prints `INSTALL NOT PROVEN — the production ledger protocol failed its concurrency probe`
and `exit 3`, and `install_exit` rolls the destination back automatically.

**Whether it gets there.** `install.sh:365` runs the nested `UNDER_TEST=$DEST SKIP_INSTALL_WITNESS=1
bash test/run.sh`, which — with `circuits` now last — runs all fifteen other witnesses against the
installed bytes and **then** launches two fresh subscription circuits (`RUN_SCOPED_CIRCUITS=1` is
inherited). If either circuit fails, line 366 exits 3 and the probe is never reached. The probe is
therefore gated behind a 2 × 4-minute subscription launch. The one-line reordering that would make
it reachable today was deliberately not made; that is the right call for a reviewer to accept, not a
builder to take.

**Can anything between the nested suite and `INSTALL OK` silently skip the probe?** Three things, all
worth fixing, none of them a hard blocker:

1. **`INSTALL_PROBE_LEDGER` in the environment silently substitutes the subject** and the receipt
   never says so. The printed line carries no path.
2. **A missing production ledger is a silent green.** Measured:
   `bash install.sh --ledger-probe "$W/does-not-exist/events.jsonl" … → INSTALL LEDGER-PROBE ok
   writers=2 seq=1..17 contiguous=true, rc=0`. The empty file the probe creates passes every check.
   The only tell is the `seq` range, and nothing asserts it.
3. **The two-`cp` race.** `ledger_probe` copies `events.jsonl` then `events.jsonl.anchor`, not under
   the ledger lock. An anchored append in between leaves the anchor **ahead** of the ledger copy:

   ```
   work copy: ledger rows=7  anchor last seq=8
   INSTALL LEDGER-PROBE ok writers=2 seq=8..15 contiguous=false reason=anchored-writer-refused:ValueError:_ledger-anchor-mismatch_defect=anchor-covered-row-altered_file=ledger_seq=7->8_…
   PROBE rc=3
   ```

   A false red on a healthy production ledger. On this box the window is small (the only anchored
   writers during the install would be one-shots started after the atomic rename), the cost is one
   auto-rollback and a retry, and reversing the copy order fixes it — anchor-behind is the lag this
   round is built to tolerate (`forge-above-hwm` in the matrix: `defect=None`).

Nothing else between line 365 and line 381 can pass over the probe silently: `set -u` makes an unset
`$CASE` a hard exit, the probe's `verdict()` always prints a line and exits 0 or 3, any crash gives a
non-zero rc, and the call site is `ledger_probe … || { … exit 3; }`.

**One thing the probe is not.** It is **not** a gate on the chain or the scoring consequence. It
filters `read()['unknown']` to `ledger-anchor-mismatch` only, so I measured round 5's probe on the
same live ledger:

```
=== ROUND 5 control, same args, live production ledger ===
INSTALL LEDGER-PROBE ok writers=2 seq=13535..13551 contiguous=true
rc=0
```

Round 5 would have installed green through this gate while `register()` refused `ledger-corrupt` on
the very next one-shot. The round-5 outage class was never in this probe's field of view; it is
closed by the chain change, not by the probe.

---

## 8. Verdict, and what remains owed after install

**GO-WITH-FIX** for `RUN_SCOPED_CIRCUITS=1 PYTHONDONTWRITEBYTECODE=1 bash install.sh`.
Rollback: automatic via `install_exit` on any post-mutation failure; manual
`bash install.sh --rollback <backup>`.

Round-5 owed ledger:

| # | round-5 item | status |
|---|---|---|
| 1 | probe refuses before it destroys (**blocking**) | **CLOSED** — §1, mutant killed |
| 2 | `test/install.sh:89-96` (**blocking**) | **CLOSED in code** — §2, verbatim assertions green by hand, mutant killed; the witness itself still needs circuits |
| 3 | state/decide the scoring consequence (**blocking**) | **CLOSED** — §3, `unknown 3322→0`, `register` refused→OK, Astra (a) supply / (b) / (c) verified |
| 4 | correct the `/proc` record | **CLOSED** — `REPORT.md:15,33,35` |
| 5 | reconcile 020/022 + a lint | **PARTIAL** — §6.7, 022 boundary 2 is now false-as-shipped and the lint cannot see it |
| 6 | split `anchor-repair` | **CLOSED** — §4, mutant killed |
| 7 | `circuits` last | **CLOSED** — 63 → 217 PASS |
| 8 | installer warning | **CLOSED** — §5, two real supervisors named |
| 9 | anchor growth/rotation plan | **OPEN** — measured (0.269 s first append, 0.281 s mean, 13,488 rows), explicitly out of scope |
| 10 | fresh circuits / field reinstall / 020 acceptance / packet required | packet set **CLOSED**; the other three **OPEN** |

**Owed after installing:**

1. **Run `bash test/run.sh` in the worktree once the install's circuits land** and require the four
   `INSTALL LEDGER-PROBE` assertions in `test/install.sh` to appear. One closure id covers both
   (`fb7f71c7f327494f…`, proven in §2), so no second circuit pair is needed.
2. **Amend `ENCOUNTER-RUN-022` boundary 2**, and give the reconcile lint a boundary-vs-boundary rule
   with claim-scoped `:reconciles`. (§6.7 — the class, not the instance.)
3. **Name the probe's subject on its own receipt line**, and refuse (or at minimum say
   `subject-missing`) when the resolved origin does not exist. (§7.1, §7.2)
4. **Copy the anchor before the ledger in `ledger_probe`**, or take both under the shared lock.
   (§7.3)
5. **Close Astra condition (a) for *override*** — route `emit_once`'s `measure_key` scan through
   `chained_only()`. Status-quo parity today, so not a blocker, but the amended boundary asserts
   something that is not yet true. (§3e)
6. **Anchor rotation/compaction plan**, baseline 0.281 s per append at 13,488 rows / 13.1 MB, growing
   ~6,000 rows/day while any pre-install supervisor lives.
7. **Field verification of the reinstall on the real production ledger**, and independent acceptance
   of the amended `ENCOUNTER-RUN-020` beyond Astra's design acceptance.
8. Cosmetic: `lib/__pycache__/ledger.cpython-314.pyc` and `packet.cpython-314.pyc` are committed in
   this change but are in neither `MANIFEST.staged.sha256` nor `install.sh FILES` — delivered bytes
   outside the closure the round's own ratchet was written to cover.

---

*Review performed read-only on `/var/tmp/forge/ship-v3.13` (`git status` clean at `5f95c97`), `~/bin`
and `/var/tmp/forge/ledger/events.jsonl`. `bash test/run.sh` was never run in the worktree; every
witness was run from a `git archive` export under `/var/tmp/forge/ship-v313-fx/review6/` with its own
`WITNESS_ROOT`. The production ledger was read with `cp` and `open(...,'rb')` only; its anchor is
still `363` bytes, mtime `2026-09-13T18:55:03Z`, unchanged. No `pkill`, no `pgrep -f`, no outer
`timeout`; the two live supervisors were found through `run-bg/*/pid` → `PPid`. Mutants:
`review6/mut1` (probe ordering) and `review6/mut4` (drop-anchored refusal), both in scratch copies,
both killed.*
