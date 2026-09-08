# Sublime score, five days by half-day — 2026-09-04 … 2026-09-08

Written 2026-09-08. Source of record: `docs/observations/2026-09-03-captains-log-anvil-seat.md`
(the running Anvil seat log; 4,389 lines at the time of writing), plus the dated docs it names —
`2026-09-04-gene-report*`, `2026-09-05-captains-log-astra-four-hour-comparison.md`,
`2026-09-07-plan-2-cellA/B/C-result.md`, `2026-09-07-pair-1-result.md`,
`2026-09-08-namespace-split-papercuts-round2/round3.md`, `2026-09-08-night-heartbeat.md`,
`2026-09-08-gene-report-night.md`, `2026-09-08-gene-report-evening.md`,
`2026-09-08-perfect-the-wins-tree.md`, `2026-09-08-row5-adopt.md`,
`2026-09-08-row2-fresh-seat-pilot.md`, `2026-09-08-battery-parallel-verdict-parity.md`.

**One correction to the record before anything else.** The brief asked for the rubric "the seat
used at 21:25Z on 2026-09-08 (… 7 of 10)". **There is no 21:25Z entry in the committed log** — the
last committed entry is `2026-09-08T21:12Z`, and no entry in the file scores the seat "7 of 10".
The nearest committed assessment on the same four axes is **`2026-09-08T16:14Z`** ("review last 2.5
hours — how is level of sublime doing?"), which reads: *"INCREASING at the inner loop … FLAT at the
routine … FLAT AND RED on the defining meter."* That entry, plus Astra's 12:56Z rubric assessment
("productive hardening with pockets of smooth interaction, not yet sublime development flow"), are
the calibration anchors used here. Every number below is quoted from the log; none is invented.

## Method

Ten half-days (00–12Z and 12–24Z, UTC). Four sub-scores, 0–10, averaged:

- **(a) inner loop** — is edit→verdict measured in seconds, with receipts?
- **(b) routine** — is the repeated routine one command with zero hand interventions? (hand
  interventions, pid/wait mistakes and operator re-runs named in the log are counted)
- **(c) measurement** — did preregistered vs-native / vs-control measurements land with honest
  verdicts, losses published? (closed cells and published losses counted)
- **(d) integrity** — false greens, misattributions, corrections-to-the-record (each costs)

Date reconstruction: the log carries explicit `YYYY-MM-DDTHH:MMZ` headings on 2026-09-03, -09-04
and -09-08, and bare `HH:MMZ` headings in between; days were recovered from the midnight rollovers
and cross-checked against the dated docs. Day boundaries: 09-04 = lines 955–1885, 09-05 = 1886–2422,
09-06 = 2423–3310, 09-07 = 3311–3546, 09-08 = 3547–4389.

**No half-day is "no data."** All ten carry log entries. Two long silences are noted as *partial
coverage*, not absence: 09-07 **10:51Z → 14:28Z (3 h 37 m)**, straddling the boundary — Gene had
said "Stop all loops" at 06:29Z and the session was restarted at 14:29:15Z; and 09-08 **11:24Z →
12:55Z (91 min)**, the handover from the night program to Gene waking.

## The chart

```
sublime score (1–10), by half-day, 2026-09-04 → 2026-09-08
### = full point   +++ = half point

10 |
 9 |
 8 |                                              ###
 7 |                                    +++  ###  ###
 6 | +++            +++  +++  +++       ###  ###  ###
 5 | ###  ###       ###  ###  ###  +++  ###  ###  ###
 4 | ###  ###  ###  ###  ###  ###  ###  ###  ###  ###
 3 | ###  ###  ###  ###  ###  ###  ###  ###  ###  ###
 2 | ###  ###  ###  ###  ###  ###  ###  ###  ###  ###
 1 | ###  ###  ###  ###  ###  ###  ###  ###  ###  ###
   +--------------------------------------------------
     04A  04P  05A  05P  06A  06P  07A  07P  08A  08P
      A    ·    ·    B    ·    C    ·    D    E    F

     5.5  5.3  4.0  5.8  5.8  5.8  4.5  6.5  7.3  8.3

turning points the log itself names
  A  09-04 04:26Z  "THE ADMIT GATE LANDED ON MCP/main"; 01:56Z Gene makes MCP/main the trunk
  B  09-05 18:47Z  Mission Ledger, first prototype in the aperture window; 18:54Z bb-routed reads
                   "show 6.7 s → 0.091 s (74×), ready 8.4 s → 0.029 s (290×)"
  C  09-06 20:39Z  "STRICTLY-BETTER LANDED, INSTALLED, TAGGED"; 22:15Z hot verification
                   61,166 ms → 965.5 ms live receipt
  D  09-07 21:40Z  ANDON PULLED (extraction wrote wrong namespaces 20/20 with committed=true);
                   23:18Z Cell C wave 1 — the split verb "beats native 5.2x"
  E  09-08 01:11Z  PAPER CUTS 81 → 0 (81 → 77 → 7 → 0); 03:18Z the first 10× landing —
                   wave 5 "56 s (CLI) / 49 s (MCP) … ÷8–9 vs native"; 06:48Z NIGHT ORDERS
  F  09-08 17:20Z  kaocha plugins "12.46 s → 0.058 s (215×); 127.68 s → 0.159 s (800×)";
                   17:50Z ship v2 first run; 18:08Z battery "839 s → 218 s median (3.85×)"
```

**Trend: UP, and the rise is only 36 hours old.** Three flat days at ~5.5 (09-04 → 09-06), a dip to
4.5 on 09-07 AM, then +3.8 points across the last three half-days. The climb is carried entirely by
(a) inner loop and (c) measurement; **(b) routine is the laggard in every one of the ten half-days**
(range 2–6, mean 3.1) — exactly the reading the seat gave itself at 16:14Z: "INCREASING at the inner
loop … FLAT at the routine."

## Sub-scores

| half-day | (a) inner loop | (b) routine | (c) measurement | (d) integrity | mean |
|---|---:|---:|---:|---:|---:|
| 09-04 AM | 4 | 2 | 9 | 7 | **5.5** |
| 09-04 PM | 5 | 3 | 8 | 5 | **5.3** |
| 09-05 AM | 5 | 3 | 5 | 3 | **4.0** |
| 09-05 PM | 8 | 2 | 9 | 4 | **5.8** |
| 09-06 AM | 8 | 2 | 8 | 5 | **5.8** |
| 09-06 PM | 8 | 2 | 9 | 4 | **5.8** |
| 09-07 AM | 7 | 2 | 3 | 6 | **4.5** |
| 09-07 PM | 7 | 3 | 10 | 6 | **6.5** |
| 09-08 AM | 9 | 4 | 9 | 7 | **7.3** |
| 09-08 PM | 10 | 6 | 10 | 7 | **8.3** |

## What set each score

### 2026-09-04 AM (00:00Z–11:36Z) — 5.5
- (a) 4: the loop is build → Sol review → NO-GO, in tens of minutes. The only seconds-scale figures
  are refusals: "in-JVM refusal measured under 100 ms … bb `:dir` 0.30 s wall"; the harness pays
  "~60 s regardless of work" on JVM exit.
- (b) 2: HTTP 429 killed six subagents mid-flight (~09:3xZ), all relaunched by hand; Sol's content
  filter refused reviews repeatedly, tallied "eight" by 05:07Z and "20th tonight" by 10:13Z, each
  rerouted to Opus by hand; the collector was hand-killed and re-run at least four times.
- (c) 9: ten experiments CLOSED with published verdicts, most of them negative — E6-Q2 "SQUARE 3
  WITHDRAWN", E-PREWRITE "SQUARE 4 WITHDRAWN", E-GATE-R "square 1 detection withdrawn, 0/14",
  E-AFFORD "INCONCLUSIVE by pre-registered wording; tool 5.70× (salient)", E-NSWEEP "N* = 23",
  E-HARNESS-2, E-CALLER "2.22× means / 3.66× medians", E-THREAD "do not build the verb" — against
  one win, E-REG, "first vs-native WIN on a load-immune metric".
- (d) 7: one heredoc CORRECTION; census r21 found "a sabotage receipt was fiction"; E-THREAD's
  "eight 'false-completes' were the oracle's" and the runner corrected the record with timestamps.

### 2026-09-04 PM (12:00Z–23:53Z) — 5.3
- (a) 5: the merge gate went "717 s → 150 s (fast lane 30 s)"; CI "green in 205 s on runners";
  T6/T6b "13/16 turns, 5.3/5.0 min … 2.3× fewer turns than same-plate native (33)".
- (b) 3: "Process defect, mine: the admit-gate merge reached origin before its gates finished";
  "a fourth self-kill → `~/bin/kill-pattern`"; the usage-watch collector timed out or returned
  zero on roughly six consecutive hourly ticks.
- (c) 8: Arm G WITHDRAWN as pre-registered ("2.6–2.8× wall LOSS in its current contract");
  T5 scored then withdrawn; the held-out SMW acceptance check "25 arms ACCEPT 4/4, G2 REJECT";
  Astra published his own tool arm as "SLOWER than native (1.53×/1.29× medians, n=3)".
- (d) 5: four corrections, two of them minutes apart on the same claim — "My 'now MCP/main
  (verified)' line was wrong — I wrote the sentence before reading the output"; and Astra's
  adversarial probe found "a real alias_migration bug … after a reported successful commit".

### 2026-09-05 AM (00:13Z–11:53Z) — 4.0 (the low)
- (a) 5: walls are quoted to four decimals — public call "27.6372s", rollback "18.0402s",
  positive-03 "26.99 s" — but the gate is still 716–739 s and the loop is a cold battery.
- (b) 3: two fence launches went to the wrong sha in one night (→ ratchet `~/bin/fence-run`);
  `land`'s own log truncated itself through a `tee /dev/stderr` bug; landing refused twice and
  was resolved by hand; the acceptance oracle was over-constrained and invalidated epoch 2.
- (c) 5: epoch 2 closed "with one failed-proof actor and 14 unstarted, ZERO accepted controls,
  no vs-native claim this window" — an honest null, and no ratio survived it.
- (d) 3: **three false greens in one night, in the same class.** "'the tree really is restored
  byte-for-byte' was NOT established … 'restored' passed because nothing was ever written";
  then a wrong Var invoked nil and passed on a never-written tree; then "a fixture that supplies
  a field the production path does not is a green that certifies the fixture". Sol's fence found
  "the builders' own witnesses were green … while a critical rollback gap sat one line above a try".

### 2026-09-05 PM (12:53Z–23:53Z) — 5.8
- (a) 8: the Mission Ledger's own primitives — "propose 102.7 ms, show 1.0 ms, ready 1.7 ms,
  apply 315.9 ms … undo 12.6 ms" — and the routing win, "show 6.7 s → 0.091 s (74×), ready
  8.4 s → 0.029 s (290×)", help "5 s → 0.04 s per call (≈125×)".
- (b) 2: three unreviewed commits reached MCP/main because a pane agent switched the shared
  checkout; `land` printed "LANDED" on a no-op merge; usage-watch pushed to the wrong git ref
  twice; the ollama kill self-matched the operator's own shell through `pgrep -f`.
- (c) 9: typist cohort 1 published, "F median 2.37 s vs N 12.41 s, 25/30 verified"; the one-site
  control recorded "NOT KEEP"; Astra "withdraws his epoch-1 native rows as accepted controls …
  Recorded as a withdrawal, not a result"; and the first honest gain — Sol "saving 63.52 s vs
  hurdle 29.08 s — clears", beside his own arm that "does not clear … 10.13 s vs hurdle 11.69 s".
- (d) 4: the false LANDED receipt and the three unreviewed commits are both integrity events;
  against them, the model-identity drift was recorded correctly as "unexplained, not attributed".

### 2026-09-06 AM (00:22Z–11:56Z) — 5.8
- (a) 8: the A/B ladder is entirely in seconds — "median 1.89 s (vs diff form 0/20; vs cold Sol
  3/4 at 29.68 s → 15.7x)", the gate-cost curve "15.7x at 0.06 s gate → ≈7.9x at 0.75 s →
  2.90x at 7.1 s", the warm comparator "12.4x", and Astra's paired forms cohort "native median
  22.588 s (SD 7.79) vs forms 7.405 s (SD 0.45)".
- (b) 2: fourteen named hand interventions, including "an unquoted heredoc ate a skill name,
  again"; "`~/bin/slot` … silently redirected the wrapper's own stderr"; "I misread the failing
  witness … for an hour"; the battery-fresh refusal paid by a manual receipt branch "~20 min
  each", twice; the restarter self-matching via `pgrep`, "the September 5 lesson repeated"; and
  **a force push caused by the seat's own wrong rebase instruction — "the house rule forbids
  force pushes … Recorded, not buried."**
- (c) 8: the Fan-out B / DN cohort closed as "A clean, retained NEGATIVE for the deterministic
  route" — native "10/10 correct, median 80–101 s (SD 15.6 s)" vs the D route "≈ 2.9x SLOWER";
  Lane A's warm-profile prototype failed "the per-edit falsifier 0/8"; cell-draft was a
  "draft-quality LOSS".
- (d) 5: the token study was corrected twice and its fleet-routing claim "withdrawn"; the SD
  misquote corrected on the record ("sample SD 17.07 s (I quoted population 15.6)"); the force
  push disclosed rather than buried.

### 2026-09-06 PM (12:03Z–23:55Z) — 5.8
- (a) 8: hot verification "trunk 61,166 ms → tip 1,089 ms", re-measured live at "965.5 ms";
  the first real public admits at "2,144.939 ms" and "2,068.714 ms".
- (b) 2: the fence reviewed the wrong git tree for two rounds — "Cost: two Sol rounds (~40 min)
  reviewing a stale tree"; the pattern-kill killed the operator's own shell for the "fifth time";
  the partner session was "WEDGED since 15:05Z … ~100 min of partner-seat time lost silently";
  the model silently drifted off its pin twice; the prompt-plate drift tripwire had been "failing
  silently since install" on a bad path.
- (c) 9: Cohort J FALSIFIED, "0/4 correct"; both whole-task Maven pairs LOSSES ("N1=128.4199s,
  T1=133.0268s"; "N2=122.6164s, T2=226.7611s") → "whole-task line PARKED"; Extract-E FALSIFIED
  ("medians N 98.0 vs E 105.5"); Cohort I's win published and then **corrected downward** to
  "1.75x (N 101.2 s / I 57.8 s)"; Astra's alias cohort won at 1.3796x but "prereg >= 1.5
  prediction MISSED".
- (d) 4: **"FALSE GREEN, mine: I told Gene 'Landed' … from the land one-shot's final line
  '=== done 18:38Z ok=0'; its FIRST line reads 'NOT LANDED'"**; and the real-session ethnography
  was **WITHDRAWN in full** — "Percentages were heuristic labels, not measured categories."

### 2026-09-07 AM (00:01Z–10:51Z) — 4.5
- (a) 7: "hot verify 61 s → 965 ms live"; the CC fan-out through 7906 "3/3 sites, 0 refusals,
  277 ms"; the guard proof "wrong expect refused … 12.6 ms, 0 files written" vs "right expect
  committed 3 edits / 3 files in 493 ms".
- (b) 2: the P0 — "every MCP call from this session goes to port 7888", the wrong server, forcing
  a hand-built `~/bin/surgeon-call` workaround; the fence is single-lane and killed a review
  mid-run ("My error"); `~/bin/land` refused on the battery-fresh tripwire "35 commits behind
  (limit 30)" and the receipt chain was run by hand; ten minutes of manual pre-existing-lint
  cleanup; five NO-GO rounds across two lanes.
- (c) 3: **no preregistered vs-native measurement closed in this half-day.** The seat labelled
  that honestly — dogfood-1 closed "No native arm → no gain claim", and the live alias replay
  states "Single run, no timed native arm for the call itself … not a controlled pair."
- (d) 6: the dogfood wall was misreported as 103 s and corrected within a minute to "151.8 s
  (stopwatch verify-done stamp)"; the wrong-port P0 was disclosed with the boundary stated
  ("no writes through 7888"); a cadence miss self-recorded as "mine".

### 2026-09-07 PM (14:28Z–23:56Z, partial coverage) — 6.5
- (a) 7: Cell C wave 1 — "D1 70 s, D2 86 s, N5 408 s, N6 ≥453 s"; Arm R's "inner loop 29x faster
  (0.75 s vs 22 s) but the wall lost"; and the papercut oracle at "0.9 s per tree, no JVM" — built
  only after Gene's "I find this arduous full loop absurd to fix paper cuts".
- (b) 3: the session had to be restarted to reload `.mcp.json`; the REPL arms were wrongly charged
  JVM start by the brief's own design and needed manual relaunches ("R3 relaunched, ~40 s; R4
  hacked .addURL, ~60 s"); two refused land attempts; a `dogfood-stamp.sh` date bug fixed by hand.
- (c) 10: the best measurement half-day of the five days — **five cells closed, four of them
  losses.** pair-1 "native faster on both fan-out tasks — A 123.7 s vs 181.3 s (0.68x), B 116.6 s
  vs 197.8 s (0.59x)"; Cell A "native 331 / 393 s vs tool 1066 / 1039 s … native 0.34x of tool
  wall"; Arm R's REPL hypothesis "NOT supported on a plan-supplied task"; Cell B NO-GO, "native
  work median 217 s, CLI 189 s (13% faster), MCP 285 s (32% slower) — killer threshold not met";
  and the one win, Cell C wave 1, "beats native 5.2x", confirmed at ÷4–5 in waves 2–3.
- (d) 6: **the Andon was pulled** on a real correctness defect — extraction "derives destination
  namespaces from the absolute path", wrong in 20/20 with `committed=true` — with the mayor
  offline the freeze stayed closed and Gene was told directly; forged receipt lines caught by Sol
  before landing; a JVM-start miscount corrected, which *reversed* an earlier "REPL loses" verdict.

### 2026-09-08 AM (00:01Z–11:24Z) — 7.3
- (a) 9: "focused ns 8.0 s cold → 0.003 s warm; all 29 nses 16.9 → 6.7 s; edit+reload+test
  8.0 s → 0.018 s"; round 3 "8 iterations, 95 s of machine time, PAPERCUTS 0" at 10.3–13.5 s per
  iteration; and the arc, wave 5 on the landed build — "56 s (CLI) / 49 s (MCP), 0 paper cuts,
  4/4 — ÷8–9 vs native".
- (b) 4: still the weakest axis, but the half-day in which the routine was *built*. Four pid
  misbindings named in one accounting ("sol-yolo wrapper pid; setsid `$!`; a helper pid; discovery
  by cwd"), costing "one false 'dead' + relaunch + two colliding Astra runs"; Gene's "Round 4
  accounting: 45 min wall for < 3 min machine work"; `~/bin/round` needed three live repairs on
  its first run; the disk cleanup at 06:35Z destroyed nine dirty worktrees.
- (c) 9: Row 1 → **friction-low**; Row 2's stage-1 gate found "UNSATISFIABLE", amended, then won —
  "3.98× less caller work (median D/N 0.25, 96.7 s saved > 2·s_N), zero refusals"; Row 5 won at
  "median D/N 0.43 on caller work, 182 s saved"; Row 3 left "uncalibrated (honest)"; the fan-out
  route SUSPENDED at "0.68×/0.59× native/tool walls at 3 and 21 sites".
- (d) 7: the destroyed worktrees disclosed as "a loss I chose too fast"; an explicit clock
  self-correction — "it is 10:1xZ, not 11:2x as I wrote in two entries"; and four rounds of
  Astra's acceptance refusals caught false-green designs *in the apparatus itself* (an exit-17
  scored green, forged receipts, a substring gate-exemption match) before any of them shipped.

### 2026-09-08 PM (12:55Z–21:12Z) — 8.3 (the high)
- (a) 10: the whole serial chain moved at once, all measured today — watch-mode red "12.46 s →
  0.058 s (215×)" and "127.68 s → 0.159 s (800×)"; "is my run done / mine / green?" from tailing
  a log to "one call, one typed line, 24 ms median"; the landing battery "839 s → 218 s median
  (3.85×) with identical verdicts".
- (b) 6: the first half-day where the routine actually moves — landing goes from "hand steps +
  50 min" to "one `ship` command". But the battery branch still took seven ship runs to land
  ("conflict, GO-WITH-FIX, NO-GO on docs, GO-WITH-FIX comment, GO then a dirty scratch worktree,
  GO then the ledger-delta guard, LANDED"), and "the 20th hand-written receipt-chain script was
  the stop signal" before `~/bin/receipt-chain` existed.
- (c) 10: losses dominate and all are published. Row-1's caller-overlap cohort: **NO ADMISSION**,
  "overlap saves ~10 %, not the 25 % the gate demands"; Row-5's F arms cleared the clock but
  "0/6 F callers used the facts inside the clock — the ablated variable was never manipulated",
  so attribution was left open; Row-5 adoption "NOT MET on inspection (≤6.0 required, 9.0
  observed)"; the row-2 fresh-seat pilot disclosed the apparatus cost honestly — "cold 89.3 s vs
  the registered 27.0 s directed … 1.36×, not 3.98×".
- (d) 7: two integrity events, both caught and ratcheted the same hour. **"CORRECTION: the 'stray
  post-land sync merge' … is MINE, not land's"**, proved from the commit parents; and the battery
  doc's "4,141 / 4,143" figure, NO-GO'd by Sol, corrected to "168 / 4,325 … only `:skipped`
  changes", with the builder finding its own claim "wrong in principle" rather than merely stale.
  The class was then named: *"a derived verdict word is not evidence; its derivation is."*

## Reading

The line is **up**, steeply, and only since 2026-09-07 21:00Z. What moved was not effort — the
09-04 and 09-06 half-days are as dense with work as any — but **which layer got instrumented**.
Three of the four axes were already strong by 09-04: measurement never scored below 8 except on the
two half-days where nothing closed (09-05 AM, 09-07 AM), and integrity held at 3–7 with every
false green published rather than buried. The score was pinned at ~5.5 by **(b) routine**, which
sat at 2–3 for eight consecutive half-days: pid misbindings, hand-written receipt chains, fences
pointed at stale trees, a force push, a "LANDED" printed off the wrong line of a one-shot's output.

The 09-08 climb is the first time (b) moves — 4, then 6 — and it moves for the same reason (a)
jumped: the seat stopped hand-driving and built the one-shots (`~/bin/round`, `~/bin/receipt-chain`,
`ship` v2, the kaocha plugins, the parallel battery). The ceiling is now visible in the same place
Astra put it at 12:56Z: **"61.2% of actions apparatus"**. Until that number falls, the routine axis
caps the score somewhere around 8, however fast the inner loop gets.
