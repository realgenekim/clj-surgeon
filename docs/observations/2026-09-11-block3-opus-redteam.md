# Opus red-team — Block 3 candidate `03e2f330` (ship v3.11)

Subject: `/var/tmp/forge/ship-v3.11` @ `03e2f330`, base `4ab97c96` (= installed v3.10 bytes).
Builder: astra-anvil, packet `207c15bb-99cc-4c99-a970-b35c18a4f4b4`. Reviewer: Opus, read-only on the
candidate; every mutation confined to `/var/tmp/forge/block3-fx/redteam/`.
Isolation receipt: production ledger `/var/tmp/forge/ledger/events.jsonl` carries **0** events with
producer `measure/v3.11`, **0** `bet-analysis`/`cohort-*`/`encounter-*`/`ship-history` payloads, and exactly
**1** `payload.fixture=true` event — seq 2104, `2026-09-11T13:47:01Z`, the pre-existing 13:00Z
fixture-pollution reconcile, which predates this review. No writes to `/home/forge/bin`, no servers, no
model launches, no forbidden ports.

## VERDICT: **GO-WITH-FIX** — install on this seat, with F-1 and F-2 fixed in the same landing.

The seven deliverables do what the report says they do, and they survived every falsification I could
build. The installer proves itself end to end against a real fixture destination. What is not safe to
install as-is is the scoring path's *authority*: `measure fold` will score a silently edited registry and
publish the result under the same registration label, and one registered class of bet (`op: ratio`)
cannot ever read "held". Both are small, local fixes in `lib/bets.py` + `lib/bets/registry.edn`.

---

## 1. Claim table

| # | Claim | Verdict | Evidence |
|---|---|---|---|
| A1 | `measure fold` reproduces Astra 11/12 on Claude from the report alone | **HELD** | `reported labels: held 11, missed 1` |
| A2 | Fable's two H bets score missed | **HELD** | bets 25 (≥7, got 1) and 29 (≥4, got 0) both `missed` |
| A3 | `measure fold` reproduces Codex M−L +10/+4, both held, acceptance loss 0 | **HELD** | bets 73/80 `measured 10.0 / 4.0`, `held` |
| A4 | Corrupting one number changes the verdict | **HELD** | AC opus 3/13→9/13 in both tables flips bet 25 `missed`→`held` |
| A5 | A cross-table inconsistency is refused, not averaged | **HELD (bonus)** | `REFUSED reason=conflicting-report-cell ('claude','H','opus-5','surgeon')` |
| A6 | A report with a missing table refuses rather than scoring zero | **PARTIAL** | no silent zero (every cell `unknown`), but **no top-level refusal**: an empty file folds at exit 0 → F-10 |
| A7 | Registry texts match the frozen records | **HELD** | all 3 retained sources byte-identical to their committed files, sha256 match |
| A8 | The registry itself is protected | **NOT HELD** | tampered registry (text + sha + bet target) folds at exit 0 → **F-1** |
| B1 | Register → score against a later run works | **HELD** | `transfer=proven`, `protected_transfer=unknown`, `independence=self-issued` |
| B2 | A run that started BEFORE registration is refused (prospective only) | **HELD** | `encounter-run-not-later; register before the next request` |
| B3 | No later run ⇒ stays untested, never discharged | **HELD** | `transfer=untested` |
| B4 | An exit-0 terminal alone cannot prove transfer | **HELD** | `transfer=untested, reason=missing later request or recurrence-free acceptance` |
| B5 | Forged acceptance is refused | **HELD (3/3)** | no digests / wrong digest / wrong `encounter_id` all → `untested` |
| B6 | The two retroactive encounters read unregistered, not proven | **HELD** | `registered:false, unregistered:true, transfer=proven-retroactively`; `daily.coverage {registered 2, unregistered 3}`; prospective numerator excludes them |
| C1 | The four meters compute the right numbers | **HELD** | inner median 4.0 / p90 10.0 from 2s,4s,10s; routine 600 s; pending packet `continuing_age` live |
| C2 | Bad clocks never become numbers | **HELD** | negative and malformed durations → `known 3, unknown 2`, never a negative |
| C3 | Recorder absent ⇒ unknown, not a number | **HELD** | `recorder:absent`, `eligible:unknown`, `protected_rate:unknown`, `observed:unknown` |
| C4 | A self-issued receipt cannot read "observed" | **HELD** | `verdict.status=observed` + `issuer=independent` → exit 2, typed refusal; a payload claiming `issuer=mayor` is overwritten to `self-issued` |
| D1 | Cohort replay re-folds the 156 Claude runs byte-identically | **HELD** | 8/8 generated tables byte-identical substrings of the published report; `model_launches: 0`; 1.0 s |
| D2 | An altered artefact is refused | **HELD** | one `:mechanism/final` edited → `REFUSED reason=cohort-drift artifacts`, **no output written** |
| D3 | The fold changes when the artefact really changes | **HELD** | re-sealed plan: AC opus `3/13`→`2/13`, editor `7/13`→`8/13`, new `analysis_id` |
| E1 | `board --report` regenerates today's facts | **HELD on the retained evidence** | 3 landings, launches 5/1/1, median 823 s; morning window 3/5/1, median 823 s — exact match to the builder's report |
| E2 | Those facts appear on the **real** production ledger | **NOT HELD** | production `board --report` = **1 landing**, median 433 s (ledger begins 13:04:17Z; ship history needs a manual `measure import-ship`) → **F-6** |
| E3 | The fold agrees with the captain's log "Sublime index from the logs" | **DISAGREES — finding against the log** | see §3 |
| F | Four machinery findings + the board RUNNING gap carried | **PARTIAL, 3 of 5** | see §4 |
| G1 | RUN line / JSON handle / SHIP status line / `INSTALL OK` literal unchanged | **HELD** | `git diff 4ab97c96 03e2f330 -- ship land run run-bg lib/run.py lib/ship-event.py lib/ship-ledger.sh` = empty; `INSTALL OK v3.9` literal intact; DEST copies byte-identical |
| G2 | Full installer into a fixture DEST reaches INSTALL OK | **HELD** | `INSTALLER EXIT=0`, `INSTALL OK v3.9 stamp=20260911T231128Z`, `PROVEN=true`, 48-file installed manifest |
| G3 | All inherited suites 0 mismatches | **HELD** | 11/11 legacy suites `mismatches: 0` (the single `mismatches: 1` is inside the fix1-P0-3 *negative* witness that proves refusal+rollback) |
| G4 | `test/run.sh` green with a live producer present | **HELD** | 92 PASS, 0 real FAIL, with `run-bg/v3.9` heartbeating into production throughout; `PASS production ledger content isolated` at every checkpoint |

---

## 2. Findings by severity

### P1 — fix before or with the install

**F-1. `measure fold` will score a tampered registry, and publishes the result under the same registration label.**
The registry retains each registration's `path` + `sha256` + full `text`, but `fold` never re-reads or
verifies them, and never notices that bet content changed while `revision` did not.

```
$ python3 -c "...rewrite sources[0].text, set sources[0].sha256='0'*64, move bet 25 target 7 -> 1..."
$ measure --ledger <fx> fold tampered-registry.edn 2026-09-11-gate-vs-prompt-report-claude.md --out tampered.edn
exit=0
Fable H opus bet 25 now: [(1, 1.0, 'held')]        # the losing bet now "holds"
```
The append-only design is a real mitigation — the tampered analysis appends with a `predecessors`
pointer and retained input digests:
```
analysis cd52a5d1 revision original-and-amendment-1 -> bet25 target 7 status missed
analysis f94e90c6 revision original-and-amendment-1 -> bet25 target 1 status held
```
— but `board --report` lists **both, under one revision string, with no conflict flag**
(`learnings_ratchets` keys: `findings, encounters, bets, cohorts, refusal_registry`). That is exactly the
plan's own rule 4 ("a composite state has NO single authority — return contradictions **typed**")
unmet, and a consumer taking "the latest" gets the tampered verdict.
**Ratchet:** at fold time re-read every `sources[].path`, verify `sha256`, refuse
`registry-source-drift`; refuse when the registry content hash changes while `revision` does not; and
have `report/fold` emit a typed `conflicting_analyses` row when two analyses of one `registry_id`
disagree on any cell.

**F-2. `op: ratio` is scored by exact float equality, so four registered bets can never read "held".**
`lib/bets.py`: `if op in ['eq','ratio']: held = value == target`. Bets 78/79/85/86 are cost-ratio
forecasts (1.30, 1.15, 1.25, 1.00); no measured ratio will ever equal a registered constant exactly.
```
Cost ratios: [('78', None, 'unknown'), ('79', 0.5256994144437216, 'missed'),
              ('85', None, 'unknown'), ('86', 1.0791666666666666, 'missed')]
```
Bet 86 is published as a **loss** because 1.0791666… ≠ 1.0. A bet that cannot be won is not a bet.
**Ratchet:** give ratio bets a registered `tolerance` (scored like `within`), or mark them
`unqualified: true` — the treatment the registry already applies correctly to the "≈AC" bets.

### P2 — should be fixed soon; name an owner now

**F-3. The board still cannot see a live packet build; the 22:11Z dogfood finding is neither fixed nor owed.**
`lib/board.clj` is byte-unchanged from the base. A `build/v3.10` `started` carrying a `builder_pid`,
with no run-bg heartbeat, falls out of RUNNING after 180 s:
```
RUNNING
{"producer":"run-bg/v3.9","run_id":"runbg-1",...,"state":"RUNNING","last_kind":"heartbeat"}
UNKNOWN
{"producer":"build/v3.10","run_id":"packet-build-1",...,"state":"UNKNOWN","reason":"stale-heartbeat","last_kind":"started"}
```
I found no OWED item, no `report.edn` finding and no witness for it — only captain's-log prose.

**F-4. "No seat-executor path for a check-only packet" is carried nowhere.**
Three of the four machinery findings are properly owed in the ledger (`c093b461` Landlock/N1 →
Fable/Astra; `cc580474` stale-session script → Astra; `9103577d` profile-inferred make gates →
Gene/Astra). The fourth exists only in the captain's log. Related: the only witness line containing
"stale-session" (`test/refusals.py:89`) is about an **Andon freeze created after admission** — a
different mechanism with the same name, so a grep-level audit of that finding false-greens.

**F-5. `--ledger` means two different things in one verb.**
`board --meters/--report/--receipt` honours `--ledger` (routed to `lib/views.clj`). The default
`board` view routes to `lib/board.clj`, which calls `snapshot.py` **with no ledger argument** and reads
`RUN_LEDGER` from the environment; `--ledger` is silently ignored. `board --ledger X` therefore answers
confidently about a different ledger.

**F-6. `board --report` has no coverage line for the ship population, and under-reports on the real ledger.**
On `/var/tmp/forge/ledger/events.jsonl` today: `landings: 1`, median 433 s — because the ledger begins
`2026-09-11T13:04:17Z` and holds **0** `ship-history` rows. The published 3 landings / 5/1/1 / 823 s
exist only after `measure import-ship /var/tmp/forge/ship.ledger` (75 rows). Every other meter names its
missingness; `wins_losses` does not. An operator running `board --report` tomorrow gets a tidy,
confident under-count.

**F-7. The two fold routes disagree on missing-value semantics, and the disagreement is not typed.**
Folding the same cohort from the Markdown report vs from the replayed ledger disagrees on 19 cells. Twelve
are the `op: reported` labels (correct: the ledger has no published label). The rest are real:
```
('87', pooled oracle-percent)  markdown 69.23 held  | ledger None unknown
('102',AC sonnet-5 program)    markdown 0.0   held  | ledger None unknown
('109',S  opus-5   program)    markdown 4.0   missed| ledger None unknown
```
Cause: from the ledger, `program` becomes `None` for the **whole cell** if any run has an empty `mech`
— i.e. a "wrote nothing" run voids the cell — and the ledger route never computes the pooled
percentages at all. Same cohort, two instruments, two answers, no disagreement named in `least_sure`.

**F-8. 26 of the 130 bets have no retained registration.**
Bets 101–126 (Astra's "committed program-writing tasks: 0–1 per caller" and the ±1-task quality bounds)
come from `2026-09-11-gate-vs-prompt-astra-review.md` §6. The registry retains and hashes the
pre-registration, amendment 1 and the Codex design — **not** the review. The registry is not
self-auditing for a fifth of its own bets.

**F-9. Fable's bets 87/88 substitute a stale constant for the registered baseline.**
The registration reads "within ±10 pp **of AC**". The registry hard-codes 71 and 46 (the *old* cohort's
AC); the contemporaneous re-run AC is 73.1 % oracle / 50.0 % accepted. Not verdict-changing here (held
either way), but it is a baseline substitution inside a frozen registration — exactly the class the
change rule exists to catch.

### P3 — low / record-keeping

- **F-10.** A source with no measurement tables (including an empty file) folds at exit 0 and appends a
  `bet-analysis` with 130/130 `unknown`, which `board --report` then lists beside real analyses with
  nothing marking it degenerate. Per-cell `unknown` is right; a `measured_cells: 0` refusal or flag is missing.
- **F-11.** For `op: reported` bets the fold **transcribes** the report's `held?` column. That column is
  computed by `report_gvp.py:235` as `held = "yes" if abs(int(s) - got) <= 1 else "no"` — an **undeclared
  ±1** applied to *point* predictions. Demonstrated: with AC opus tampered to 2/13 against a registered 3,
  the regenerated table still prints `Astra yes`. The candidate discloses this and scores the same cells
  exactly (**4/12**) — so this is a finding against the published "Astra 11/12" headline and the
  2026-09-11T05:14Z captain's-log entry, not against the candidate.
- **F-12.** `number()` reads the numerator and discards the denominator: changing `**3/13**` to `**3/26**`
  produces an identical verdict (`reported: held 11, missed 1`; bet 1 still `held`). The registered
  population ("13 tasks per caller/arm") is prose, never checked.
- **F-13.** Any event carrying an `encounter_id` becomes an encounter row in `board --meters`. A stray
  acceptance check with an unregistered id showed up as `enc other, registered=None` inside
  `daily.coverage.unregistered`. Correctly labelled, but the unregistered denominator is forgeable noise.
- **F-14.** `recorder: "absent"`, `fleet.observed: "unknown"` and the roster `{forge@anvil, skiff}` are
  **literals** in `lib/meters.clj`, not folds. When the mayor's recorder or a third seat exists, the meter
  will keep saying `absent` / `2` until someone edits the source.
- **F-15.** Refusal format differs: `lib/views.clj` prints `REFUSED reason= <msg>` (note the space);
  the Python verbs print `REFUSED reason=<msg>`. Anything grepping `reason=<token>` sees one and not the other.
- **F-16.** `bets.fold` (`retained()`) and `cohort.replay` write their `--out` to an unconfined
  caller-supplied path, in a system where every other path is confined.
- **F-17 (packaging).** The candidate tracks `lib/__pycache__/packet.cpython-314.pyc` in git, so any
  interpreter that writes bytecode dirties the tree. It is excluded from `MANIFEST.staged.sha256`, the
  installer's `stage_hash` and the install `FILES` list, so it cannot affect an install — but it makes
  "the tree is clean" a weaker statement than it looks. (I dirtied it during this review and restored it;
  the candidate is back at `03e2f330` clean.)
- **F-18 (record).** The 2026-09-11T22:59Z captain's-log entry says "the one FAIL line is the content
  oracle's own red probe". There are **5** such FAIL lines in `candidate-full-witness.log` (and 5 in my
  installer run). Same benign class; the count is wrong.

---

## 3. Claim E in full — `board --report` vs the captain's log "Sublime index from the logs" (21:26Z)

Read-only on `/var/tmp/forge/ledger/events.jsonl` (hash unchanged across the read) and on the builder's
retained `verified-events.jsonl`, folded at the log's **own** stated window (44 h ending 21:26Z):

| the log says | the fold says (ship.ledger) | the fold says (run-bg ship-*.log) |
|---|---|---|
| 36 ship runs | **30 distinct runs** (31 SHIP lines) | **29 runs** with a SHIP status line |
| 8 landed | **7** | **7** |
| launches per landing 4.5 overall | **4.14** (by base) / 4.29 (runs÷landings) | — |
| landed ship wall median 436 s (423–846) | **436 s, range 423–846** ✅ | — |
| 17 HOLD no-go | **11 HOLD** (+6 RED, +5 UNVERIFIED) | 11 HOLD |

A window sweep (24/30/36/40/44/48/60/72/96 h) produces landed counts 4, 6, 6, 6, 7, 7, 7, 12, 14 — **no
window yields 8**, and no window yields both 36 runs and a 436 s median. The median and range reproduce
exactly; the counts do not, from either population.

**Finding for the log side, not the tool.** The hand-count over-states runs by ~6 and landings by 1, and
4.5 follows from neither denominator. The composite "sublime index: actually 4" rests partly on those
counts. This is precisely the "hand-written report tables" the plan says Block 3 retires, and it is the
strongest argument in the packet for installing this candidate.

Verified arithmetic on the fold's side: the 5-launch landing is real —
`base=a6e53564e635` carries exactly 5 SHIP rows (RED, UNVERIFIED, RED, RED, LANDED), and the fold's own
`scope` string already says "ship attempts sharing base; builder launches unknown".

---

## 4. Claim F in full — machinery findings

| # | Finding (from the 22:44Z / 22:11Z log entries) | Witness? | OWED with an owner? |
|---|---|---|---|
| 1 | A packet cannot run its own witness suite under its Landlock envelope | no | **yes** — `c093b461`, Fable/Astra; plus a blocking `report.edn` finding with the exact `PermissionError` |
| 2 | `stale-session` on a check whose script the builder changed | no (the one "stale-session" PASS line is a different mechanism — F-4) | **yes** — `cc580474`, Astra, `unblock: stale-session script .../test/run.sh` |
| 3 | Profile-inferred make gates in a one-shot repo | no; `lib/packet.py:176` still appends `make landing-gate-prewarm` whenever a catalog exists | **yes** — `9103577d`, Gene or gate owner Astra |
| 4 | No seat-executor path for a check-only packet | no | **no** — nothing in the ledger or `report.edn` |
| 5 | The board did not show a build/started packet under RUNNING | no — `lib/board.clj` unchanged, reproduced above | **no** |

Independently: my fixture install reproduced the envelope theory for (1) — outside a Landlock packet,
`fix2` N1 passes (`PASS N1 artifacts .../fix2-N1-b2014085-…`). The finding is about the envelope, not the test.

**Scope note on Block 3's own DONE criterion.** On the real production ledger all four meters read empty
today: `inner {recorded 0}`, `routine {recorded 0}`, `daily {registered 0, unregistered 0}`,
`fleet {registered_active 2, observed unknown}`. The candidate ships the folds and writes down the
recording contracts (`payload.type=inner` / `findings-selected` / `final-batch` / `name=acceptance`), but
**no producer emits them yet**. "Another agent answers what changed, what failed, what remains owed and
which bet lost from one query" is not reachable by installing this candidate alone.

## 5. Claim G in full — installer into the fixture destination

```
DEST=/var/tmp/forge/block3-fx/redteam/dest   FIXTURES=<private copy>   WITNESS_ROOT=<private>
INSTALL OK v3.9 stamp=20260911T231128Z files='lib/measure.py … lib/board.clj'
INSTALLER EXIT=0        PROVEN=true stamp=20260911T231128Z
MANIFEST.installed.sha256: 48 lines
legacy fixture suites: run-land-auto 19 rows / run-land-publication-truth 12 / run-ship-v2 28 /
  v3 22 / v3.1 13 / v3.2 8 / v3.3 13 / v3.4 14 / v3.5 5 / v3.6 9 / v3.7 73  — all "mismatches: 0"
installed-byte witness suite: 92 PASS, 0 real FAIL (5 FAIL lines are the content oracle's own red
  probes on a scratch copy, each followed by its PASS; 92 vs the seat's 99 = test/install.sh skipped
  by SKIP_INSTALL_WITNESS=1, as the installer intends)
"PASS production ledger content isolated" at every checkpoint, with a live run-bg/v3.9 producer
  heartbeating into the production ledger throughout
```
Contract literals: `git diff 4ab97c96 03e2f330 -- ship land run run-bg lib/run.py lib/ship-event.py
lib/ship-ledger.sh` is empty; `ship:592` SHIP line and `run-bg`/`lib/run.py` byte-identical in DEST;
`INSTALL OK v3.9 …` literal unchanged. The only installer change is the 288-line payload extension, and
`lib/packet.py`'s one-line closure list, which correctly refuses a packet sealed without the new files
(`refused: closure-incomplete omitted=…lib/bets.py,lib/cohort.py,…`).

## 6. What I could not test

- **Independent acceptance / independent issuance.** Every receipt here is same-account and self-issued;
  the candidate says so in every payload. The mayor's account remains the only way to move
  `protected_transfer` off `unknown`.
- **A real prospective second encounter.** I registered and scored a synthetic one. `d0a77033` (Astra)
  stays open; nothing here discharges it.
- **`measure cohort` on a fabricated-provenance run.** Replay binds artefact digests, the schedule TSV
  order, the reader hash and the packet manifest — but all of them are same-account. A run directory that
  never existed cannot be distinguished from one that did, only a *changed* one can.
- **Whether the pinned apparatus readers are correct instruments.** Byte-identical replay proves the
  fold, not the classifier. `report_gvp.py:235`'s ±1 (F-11) is one concrete place where the reader's
  judgement is doing work the registry did not register.
- **The real N1-under-Landlock refusal.** I could only show that N1 passes *outside* a packet envelope.
- **Live `run check` re-sealing** (the F-4 gap) — that needs a packet launch, which I did not make.
- **Ratio-bet regression after a fix** — F-2 has no witness in the suite to go red first.

## 7. Suggested landing order

1. Fix **F-2** (ratio tolerance or `unqualified`) — one line plus registry edit; add a witness that a
   ratio bet at the registered value reads `held` and one 20 % away reads `missed`.
2. Fix **F-1** — verify `sources[].sha256` at fold and refuse `registry-source-drift`; refuse a content
   change under an unchanged `revision`; add a `conflicting_analyses` row to `report/fold`. Witness:
   the exact tamper above must refuse, and the honest fold must still pass.
3. Install. Then file **F-3**, **F-4**, **F-5**, **F-6**, **F-7** as OWED with owners before the next
   packet, since three of them are about the board telling a confident story with a missing denominator —
   the class that produced §3's finding in the first place.
