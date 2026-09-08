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
