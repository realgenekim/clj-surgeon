# tighten — the daily one-shots (forge@anvil, 2026-09-08)

Cell B of the `tighten` block: the mechanism behind the practice. Astra owns the skill
(`tighten-the-loop`, `/tighten`); this file records what is actually INSTALLED on this seat, what
it proved on its first run, and where it knowingly diverges from the skill's draft contract.

Design of record: `/var/tmp/forge/plan2/cellC/astra-daily-sublime.md` (mechanism | owner | cadence
| receipt | tripwire) and the merged captain's-log entry of 2026-09-08T12:55Z.

## What is installed

| one-shot | cadence | receipt line | tripwire |
|---|---|---|---|
| `~/bin/seat-receipt` | cron 05:30Z daily | `SEAT-RECEIPT seat=… on_loop=… …` | `on_loop=false`, or two consecutive PRECEDING days missing → maven inbox item to the mayor |
| `~/bin/canary-cell` | cron 06:00Z daily | `CANARY-CELL date=… grade=… apparatus=…%` | `grade≠PASS` or `apparatus>50%` → inbox item naming the counterexample run dir |
| `~/bin/verb-sentinel` | cron Sun 07:00Z, or `--now` | `VERB-SENTINEL verb=… oracle=… verdict=OK\|SUSPEND` | oracle FAIL, or wall drift beyond the registered band → inbox item + a `SUSPEND` note. It never edits the routing plate. |
| `~/bin/ledger-to-findings` | on demand | `LEDGER-TO-FINDINGS scope=… items=N ids=… round_cmd='…'` | refuses `no-open-items-in-scope`; never launches the round |
| `~/bin/tighten` | umbrella | `tighten status` / `tighten day` / `tighten help` | — |

Crontab lines (box TZ is `Etc/UTC`, so these are UTC):

```
30 5 * * * /home/forge/bin/seat-receipt      >> /var/tmp/forge/tighten/cron-seat-receipt.log 2>&1
0  6 * * * /home/forge/bin/canary-cell       >> /var/tmp/forge/tighten/cron-canary.log 2>&1
0  7 * * 0 /home/forge/bin/verb-sentinel     >> /var/tmp/forge/tighten/cron-verb-sentinel.log 2>&1
```

Backup of the pre-change crontab: `/var/tmp/forge/crontab-backup-20260908T131433Z.txt`. Lines were
added, never removed.

## First run, 2026-09-08

| job | result |
|---|---|
| seat-receipt | `on_loop=true` — routing block `67e45724…` installed == source, coldstart corpus 52 rows / 0 mismatches (155 s), round corpus 28 rows / 0 mismatches, disk 29 G remaining, 28 open friction items. Published to `docs/observations/seat-receipts/forge@anvil/2026-09-08.edn`. |
| canary-cell | cell 3 (day-of-year 251 mod 4) = mvr / sol / cond=W on `origin/nrepl/test-alias` 94393708. `grade=PASS`, task PASS, cold gate 580/7842/0, **apparatus 57.1%** → tripwire fired correctly (`inb-236769`): over 50% is the skill's own "not sublime" alarm. |
| verb-sentinel namespace_split | trunk 921cb465, fresh `d9205abc` fixture, oracles **4/4**, PAPERCUTS 0, split call **34 s** (registered tool 56 s CLI; native 447 s, n=2). `verdict=OK`. |
| verb-sentinel alias_migration | frozen row-2 request on the history-isolated seed repo via MCP 7906, **ACCEPTED YES** (O1-O5 all PASS), call wall **4.310 s**. `verdict=OK`. |
| ledger-to-findings | scope `namespace_split` → 5 open items (`inb-525d27 inb-7be8dd inb-45b82d inb-a67d24 inb-6b185b`) frozen into `/var/tmp/forge/round/findings/namespace_split-2026-09-08.md`, round command printed, not launched. |

## Three apparatus defects the first runs found, and their ratchets

1. **PATH.** `canary-cell` pinned a minimal PATH without `~/.local/bin`, where `clj-nrepl-eval`
   lives; `coldstart --cond W` refused at the attestation step. Every tighten one-shot now carries
   `/home/forge/.local/bin` in its PATH, and `seat-receipt` resolves the client with `command -v`
   instead of a hard-coded directory (a package folder is not command-resolution proof).
   Tripwire item `inb-e1abd9`, closed with the fix.
2. **Run names must be unique per RUN, not per day.** `coldstart` reserves its bundle by name
   atomically and correctly refused `run-exists` on a same-day re-run. The canary name now carries
   `HHMMSSZ`; the records page still groups by date. Item `inb-5fc72a`, closed with the fix.
3. **A fabricated headroom number.** uutils `df` refuses `-i` together with `--output`, so the
   first receipt printed `:inodes-remaining-pct 0` — a panic number with no source behind it. It
   now reads the `IUse%` column from plain `df -i` and reports `unknown` if that fails.

A fourth, methodological: the alias sentinel first compared its 4.310 s **runner-issued call** with
the registered **27 s agent-caller** median and printed "IN". Not like for like. The band now runs
against the PREVIOUS SENTINEL, measured the same way, and reads `no-baseline` on a first run; the
registered 27 s / 121 s travel as context. The published row carries the correction.

Two more, found by running the jobs rather than reading them:

5. **Backticks in an unquoted heredoc executed as commands.** The receipt's own schema comment
   contained `` `tighten-seat-receipt/v1` ``; the EDN heredoc must stay unquoted so variables
   expand, so bash tried to run it and the receipt printed a shell error where the contract name
   belonged. The known rule ([[quoted-heredocs-for-prose]]) applies to prose *inside* an expanding
   heredoc too. Fixed to straight quotes.
6. **The seat boundary was three phases too far downstream.** A careless `verb-sentinel
   --port 7888` test reached `surgeon-call`, which refused correctly (exit 3) — but only after a
   fixture had been built and graded, and the job published a `SUSPEND` row that meant nothing.
   Two ratchets: 7888/7890/7894/7895/83xx are now refused at ARGV before anything is spent; and a
   call that does not complete yields `verdict=UNVERIFIED`, never `SUSPEND`, because an
   unavailable or refused capability is unknown performance, not a recorded correctness loss.
   The bad row is retracted in its own heading (`## [RETRACTED] …`) so `tighten status` cannot
   show it as the standing result, and its inbox item is closed with the explanation.

## Declared divergence from the skill's draft contract

`~/.claude/skills/tighten-the-loop/SEAT-RECEIPT.md` specifies `tighten-seat-receipt/v1`: an
independent recorder, a published seat binding, no-argument jobs, and a destination of
`docs/observations/tighten/YYYY-MM-DD/<seat-id>/<run-id>/`. What is installed is
`tighten-seat-receipt/v0-installed-subset`, and it says so in its own first lines: flags instead of
a binding, `docs/observations/seat-receipts/<seat>/<date>.edn`, and **self-built by the subject
seat** — v1 forbids that. Until an independent recorder and a seat binding are named, the honest
reading of a seat receipt is configuration evidence plus corpora behaviour, never an accepted-work
grade. That reconciliation is the next owner's decision, not a silent choice by the program.

## What could not be made a one-shot

* **The weekly sentinel's alias arm is not a matched PAIR.** It runs the tool arm only. A native
  control needs a fresh agent and roughly two minutes of caller work per arm; scheduling that
  unattended would spend a model every week on a number the records already hold. The sentinel is
  a drift detector against its own previous run, and says so.
* **Row 1 has no registered noise band.** Two native points (408/486 s) that the records explicitly
  call "historical context, NOT controls for a new build/wave". The split arm therefore reports its
  wall and suspends only on an oracle failure. Manufacturing a band from n=2 would have been the
  easiest and worst thing to automate.
* **The candidate worktree for a round.** `ledger-to-findings` prints a placeholder rather than
  guessing which tree a builder should own. Choosing it is a decision.
* **Second-encounter (transfer) evidence.** Astra's test of recursion — a later eligible task, on a
  different seat or repo, that avoided the original obstruction — cannot be produced by a cron job
  on one seat. Nothing here claims it.


---

# R12.receipt_bound — binding a boot-block report line to the run it claims (2026-09-08T17:54:03Z)

Gene: *"if you can disambiguate test runs, that would be epic."*

## The hole this closes

`COLDSTART-RECEIPT … proof=pass:<gate>:231/2258/0` is the same sentence for a run that
happened and a run that did not. Nothing in the grader could tell two executions of the
same gate apart, so every check downstream of it graded a **claim about a run** rather
than the run. kaocha-sublime's run-receipt plugin now writes one immutable EDN per
completed run (`<id>.edn.tmp` reserved, `<id>.edn.ready`, atomic rename to `<id>.edn`,
then a published `LATEST`), carrying `:run-id`, `:started`/`:finished`, `:totals`,
`:config-hash` and `:jvm-pid`. COLDSTART.md step 6 makes the agent copy that path into
its report line. This change makes the grader **open the file and bind it**.

The check is binding, not trust. A receipt is evidence only when it is *this* run's
(finished inside this run's stamped window), from *this* worktree (under the specimen's
own `target/kaocha-runs/`), and carries the totals the agent reported. Each of those
fails separately, because "the numbers disagree" and "that file is another run's" are
different findings and a single MISS would blur them.

* **R12.receipt_bound** (required) — path exists, parses as EDN, is confined, is in
  window, and `:totals` `(:tests, :assertions, :fail + :error)` equals the agent's
  `proof=…:<t>/<a>/<f>`.
* **F13.foreign_receipt** (forbidden) — a receipt path outside the specimen worktree.
* **F14.claimed_pass_with_red_receipt** (forbidden) — `proof=pass` over a receipt whose
  `:fail + :error` is non-zero.
* Grade line gains `receipt_check=<ok|miss|unavailable|foreign|forged>`; the report
  gains `receipt_reason`, `receipt_path`, `receipt_totals`, `receipt_totals_claimed`.

## `unavailable` is a third state, and it is the load-bearing one

Every cell graded before today ran against a block that never asked for a receipt, on a
specimen whose gate prints none. Failing them would have graded **the apparatus as the
agent** — the CX-1 mistake in a new coat. So `receipt=none`, and an absent `receipt=`
field, cost a MISS **only when the specimen's gate is known to print `TEST-RECEIPT`**
(detected from its `bin/test-probe`/`Makefile`); otherwise R12 is N/A and reports
`receipt_check=unavailable`. It is appended OK so the verdict arithmetic is unchanged,
and **rendered `N/A`, never `OK`** — a check that could not apply must not read as a
check that passed. That is the F9/F10 lesson (unverified is not clean) applied to a
required check.

## Evidence

| meter | result |
|---|---|
| full corpus, `fixtures/run-fixtures.sh` (live rows included) | **fixture mismatches: 0**, 40 graded rows |
| new rows, `fixtures/r12-receipt-bound.sh` | **18 rows, mismatches: 0** |
| p30–p40 regrade, **pre-R12 backup grader vs this one** | **11 bundles, verdict/check changes: 0**; every one `receipt_check=unavailable`, `R12=N/A` |
| grader | `/home/forge/bin/coldstart-grade`, sha256 `1fade323866fd5a9…`; backup `/var/tmp/forge/coldstart-grade.bak.20260908T173012Z` |

Verbatim, the two ends of the range:

```text
COLDSTART-GRADE: PASS required=11/11 forbidden=0 … gate_exit=0 receipt_check=ok starts=1/0 …
COLDSTART-GRADE: FAIL required=10/11 forbidden=1 … gate_exit=0 receipt_check=foreign starts=1/0 …
```

and the typed line a totals mismatch emits, as a note beside the rendered check:

```text
note: R12.receipt_bound MISS agent=231/2258/0 receipt=230/2258/0 file=…/kaocha-runs/differ.edn reason=totals_mismatch
```

## Three things worth keeping

**The stored `grade.json` is not a baseline.** The first regrade showed p37-sol-H-cc
going FAIL → PASS and I nearly reported it as a change. It is not: p37's stored grade
was written by a **pre-CX-21** grader, and the pre-R12 backup grades it PASS on the same
bytes with the same arguments. *A regrade's control is the previous EXECUTABLE, never
the previous ARTIFACT* — the artifact records what some grader once thought, and the
question is what this change did. Both figures above are backup-vs-new.

**Confinement is a test on the resolved file, never on the spelling.** A symlink inside
`target/kaocha-runs/` pointing at another worktree's green receipt, and a
`target/kaocha-runs/../../../outside/…` climb, both look confined as strings. I ran them
as probes, and then made them permanent rows (`r12-symlink-escape`, `r12-dotdot-escape`)
— a hardening I only ran in my own scrollback is a hardening the next change deletes.
Same distinction B02e item 2 had to make for the gate program.

**A delete names its own subject.** The fixture script's scratch root was
`/var/tmp/forge/grade-r12-fx` and it opened with `rm -rf "$FX"` — which deleted the
corpus report being written into that same directory, mid-run. It now owns and deletes
only `$ROOT/specimens`, with a literal guard on the path. Exactly the lesson
`fixture-cleanup.sh` already carries: "my scratch dir" is not the same subject as "the
scratch dir".

## Known holes, stated rather than hidden

* **Capability detection reads files the graded party owns.** An agent that deleted
  `TEST-RECEIPT` from its own `Makefile` would downgrade `receipt=none` from MISS to
  `unavailable`. It cannot downgrade a receipt it *did* name — every path is bound
  whatever the Makefile says — and a changed gate program is already F12. The fix is a
  capability flag in the frozen GATE_QUALIFY bundle beside `gate-program.frozen`. Until
  that exists this is a known hole, not a proof. (B02e item 1: the graded party may not
  write the oracle's exemption rules.)
* **A `proof=fail` word over an exit-0 gate is still unconvicted.** Found while building
  `r12-honest-red`, where I expected FAIL and got PASS: R9 compares the claimed proof
  word to the gate's exit only for `pending` (Astra 9). The run receipt is now exactly
  the evidence that would settle it — `:totals` disagreeing with an observed exit 0 is
  the two-sources-disagree finding B02d item 7 types elsewhere. Left out deliberately:
  it widens R9, not R12, and a check written in the same batch as its own fixture is a
  check nobody attacked. Filed rather than smuggled.
* **Time-window checks need stamps.** A transcript with no `@epoch|` lines cannot place
  a receipt in its window; the path, confinement, parse and totals checks still run.
