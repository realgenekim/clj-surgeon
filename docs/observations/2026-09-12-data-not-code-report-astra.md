# Data-not-code — round 4

Verdict: **PASS — local build and required prewarm complete.** No release or
independent adversarial approval is implied.

The transport drop did not invalidate the committed work. Round 4 resumed
`fable/data-not-code-local` at `6c5b1230`, retained the four draft files, and
completed their verification repairs. The binding original brief is named
`brief-astra-datacode.md`; there is no `brief-astra-datacode-1.md`. Round 3
explicitly approved the remaining development phases.

## Delivered behavior

1. Final artifact destinations are resolved and admitted under trusted startup
   authority before creation/publication. Request-carried authority is refused;
   a missing launcher uses bounded policy roots; a narrow startup value is
   retained across subsequent initialization. Outside bytes and absent parents
   remain untouched on refusal. Ancestor/final-file symlinks and the telemetry
   ledger are covered. Successful full and compact require-change receipts
   carry the envelope identity. The ledger's effect is `:telemetry-append`.
2. Sandbox denial classification comes from `:sandbox/decision :deny`, including
   through runtime wrappers. Changed wording cannot change classification, and
   English-looking untyped failures cannot impersonate denial data. The compact
   X-ray remedy preserves the pre-existing CLI output ceiling.
3. The fold publishes validated EDN evidence rows; the manifest consumes them.
   Missing receipts refuse; independent statistics/assignment witnesses remain.
4. Sleep identities use test owner, call ordinal and temporal purpose. Current
   lines are diagnostics. Moving an unrelated line passes; purpose drift fails
   by name. The census retains every old test and adds eight new named tests.
5. The [six-task probe preregistration](../round3/probe-measure-preregistration.md)
   contains both authors' bets, two arms, matched order, n=3, exact commands,
   complete-wall accounting and falsification rules. No probe experiment ran.

Coverage is the shared artifact boundary and its inventoried consumers, **not
every writer, subprocess or adversarial filesystem race**. Landlock is unchanged.

## Red-to-green record

| Item | Committed red | Committed implementation | Retained evidence |
|---|---|---|---|
| Envelope | `d458160d` | `dfa16a54` | `../round3/item1-red.log` (24 failures), `item1-green.log` (29 tests / 251 passes) |
| SCI values | `91e9ebbf` | `579f00e7` | `../round3/item2-red.log` (13 failures), JVM and bb green logs (26 / 480 each) |
| Evidence rows | `f468a954` | `c22660f3` | `../round3/item3-red.log` (7 failures), fold and green logs |
| Sleep identity | `c8a4067e` | `2b71963c` | `../round3/item4-red.log` (2 failures), JVM and bb green logs |
| Preregistration | — | `6c5b1230` | Design only, not a measurement |

Round 4 reproduced the remaining architecture-inventory failure before adding
the newly reachable pure `receipt-hash` owner. It retained the draft admission
call inventory and verified the new trusted context instead of requiring the
old exact map. The first broader bb run then caught a 1,119-character unsafe
X-ray refusal against the existing 1,024-character bound. The remedy was
shortened; typed fields and all existing assertions remain.

The first prewarm found three alias assertions (stored receipt enrichment and
the refusal census) plus four kernel reflection warnings. The repair preserves
envelope identity in compact output, pins the two real refusal kinds, gives
the ledger an explicit effect name outside the census's broad alias-prefix
heuristic, and declares the admitted file's return type. No budget, namespace
runtime assignment, test membership or gate was weakened. The census heuristic
itself remains a known limit; a telemetry effect must not be pinned as a refusal.

## Verification

| Check | Result | Receipt |
|---|---|---|
| Formatting | Standard-clj completed | `format.log` |
| Lint | 0 errors; 7 inherited warnings; temporary return-hint warning repaired | `lint-post-repair.log`, `lint-return-hint-final.log`, `lint-baseline-corrected.log` |
| Intent audit | Pass | `intent-audit.log`, `intent-audit-final-summary.log` |
| bb focused | 70 tests / 1,122 assertions, 0 failures/errors | `focused-bb-green.log` |
| JVM focused | 196 tests / 3,370 assertions, 0 failures/errors | `focused-jvm.log`, `focused-jvm-summary.edn` |
| Repair focused | 3 tests / 18 assertions, 0 failures/errors | `prewarm-repair-focused-green.log` |
| Kernel compilation | 2 namespaces / 0 warnings | `kernel-warning-final.log` |
| `make test-fast`, once | 1,246 tests / 12,632 assertions; 0 isolation violations, skipped 0 | `test-fast.log`, `test-fast-receipt.edn` |
| First prewarm | Failed; bb passed; MCP behavior passed but shell compile gate failed; alias 3 failures | `prewarm.log`, `prewarm-red/` |
| Final prewarm | Pass; all 7 required stages exit 0, no problems | `prewarm-final.log`, `prewarm-final-receipt.edn` |

The final prewarm reports 422,819 ms gate wall; complete command wall is
424,964 ms (`prewarm-final-command.json`). Its final suites are alias 187 tests /
3,727 assertions, MCP 1,418 / 16,506, and bb 903 / 8,061, all with zero failures
and errors. The final MCP lane sums are fast 50,656 ms and integration 70,086 ms.
The additional bb diagnostic, repository hygiene and intent audit also pass.
The verified source digest is
`56fc87b089c2506439a734a08e17827bcefdd6799639f147cdeada3fd9c70d00`.
The receipt names pre-commit HEAD `6c5b1230`; its source digest binds the tested
uncommitted production bytes and is checked again during report finalization.

The fast receipt reports 29,682 ms makespan and 41,690 ms namespace sum
(bb 7,518 ms + JVM 34,172 ms). It predates the prewarm repair; the final prewarm
is the verification of the final production snapshot. There was no second
standalone fast run. Prewarm is not a landing or independent review receipt.

The first host gate was visibly active even though no `jvm-lease*` marker
existed. Our runner waited on the same `/home/forge/tmp/suite-1.lock`, acquired
it at the timestamp in `lock-acquired.txt`, and retained it across focused JVM,
fast and first prewarm. The final prewarm reacquires that lock and publishes an
owned marker. No independent suite ran concurrently with either gate.

`checks.tsv` records exits, but its timing columns are **invalid**: this host's
`date +%s%3N` emitted malformed variable-width subsecond values. Those values
are not milliseconds and are not measurements. Gate receipts supply their own
monotonic timings; the final command uses Python `monotonic_ns` and retains argv,
exit and complete command wall. No request-to-verdict performance claim follows.

The owned nREPL stopped successfully; its client reports the expected EOF from
`System/exit`, retained in `nrepl-stop.log`. Temporary lint snapshots were removed
by explicit filename; briefs and retained test evidence remain. Both gate runs
released the owned lease marker and host lock.

The kernel, receipt and enumeration repair is recorded as one prewarm repair
batch. An intermediate focused command had a misplaced closing delimiter and
loaded only one witness; its output is retained and is not counted as green.
The corrected command's 3-test result is the repair evidence. The baseline lint
snapshot initially had the wrong directory layout; that failed log is retained
beside the corrected one. The seven warnings are independently reproduced at
`3383da9b`; none was silenced.

## Judgment, limits and remaining work

The [source-edit table](dogfood.md) accounts for inherited work and every round 4
source edit. Round 3's editing transport is unknown where its receipts omit it.
The formatter normalized existing layout in touched files; this is visible in
the final diff and covered by the final gate.

Least sure: cross-platform passwd-home/realpath behavior and publication races
are not certified by this Linux run. The static refusal census still uses a
broad alias-prefix text scan. New path-resolution refusal membership is pinned;
that enumeration witness alone does not prove every pathological link topology.

Disagreement: Astra's preregistered JVM complete-wall bet is 0.45× native,
versus Fable's 0.30×. Neither was measured here. The expected sublime-score
benefit is qualitative: stronger authority, stable diagnostic classification,
and less measured-data maintenance. Numeric score delta remains unknown.

Owed before release/merge: independent adversarial review of the path boundary
and sandbox changes, plus the separate preregistered probe measurement before
any speed claim. No merge, push, tag, shared install, routing admission or probe
experiment was performed. The final commit contains this report and all retained
round 4 evidence; its identity is the enclosing Git commit.
