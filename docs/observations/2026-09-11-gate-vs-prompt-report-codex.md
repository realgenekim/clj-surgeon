# The Codex arms of the gate-vs-prompt experiment — measured

Design and bets: Astra, `2026-09-11-gate-vs-prompt-codex-arm-astra.md`, frozen before any caller ran. Built and run by an Opus agent; Astra did not touch the runner. Arms **L, P, M, X0, X1** (Amendment 1's Codex list); Astra's sixth arm **N** was not run and its forecasts stay **unmeasured**.

Delivery evidence, including two delivery failures found and fixed before scoring (the seat's own `~/.codex/AGENTS.md` routing plate reaching every arm, and the repo-level `.codex/config.toml` not delivering MCP at all): `delivery-probes.md`.

**Two corrections this report makes to its own earlier claims, kept visible rather than tidied away.** (1) The pre-run delivery note said R-01/R-02/R-03 had *no* Surgeon entrance for their require-add, because `require_change` refuses an `ns` carrying `[clojure.test :refer …]` on both the CLI and MCP. The cohort falsified half of it: arm M's `gpt-6-astra` did all three with `edit_clojure` scoped by `within: {"namespace": …}`. The CLI half stands — no P-arm run ever routed those three tasks to Surgeon. (2) The first mechanism classifier read Codex's rollout as JSON; it is JavaScript with unquoted keys, and it filed 41 of 41 real calls as "other-tool". Every run keeps its raw `events.jsonl`, `rollout.jsonl` and shim journal, so all 130 runs were re-derived under the corrected classifier (`rederive.sh`); no run was re-executed and no oracle or gate verdict moved.

**Scored runs: 130** of 130 assigned (13 tasks x 5 arms x 2 models). All assigned runs stay in every denominator.

## 1. Exposure first — did the boundary ever fire?

Astra: *"No exposure means no evidence for the conditional repair mechanism."* A shim firing is an external, append-only journal row written by the shim itself, not an inference from prose.

| arm | model | tasks where the shim FIRED | of those, fired BEFORE the edit was already written | tasks where it would have (observe) | what happened after the refusal |
|---|---|---:|---:|---:|---|
| L | gpt-6-astra | **0/13** | 0 | 0/13 | — |
| L | gpt-5.6-sol | **0/13** | 0 | 0/13 | — |
| P | gpt-6-astra | **0/13** | 0 | 5/13 | — |
| P | gpt-5.6-sol | **0/13** | 0 | 0/13 | — |
| M | gpt-6-astra | **0/13** | 0 | 0/13 | — |
| M | gpt-5.6-sol | **0/13** | 0 | 0/13 | — |
| X0 | gpt-6-astra | **1/13** | 0 | 0/13 | post-write-exposure-no-hand-left x1 |
| X0 | gpt-5.6-sol | **0/13** | 0 | 0/13 | — |
| X1 | gpt-6-astra | **1/13** | 0 | 0/13 | post-write-exposure-no-hand-left x1 |
| X1 | gpt-5.6-sol | **0/13** | 0 | 0/13 | — |

Astra's exposure bet: **3/13 for gpt-6-astra, 0/13 for gpt-5.6-sol** in X0/X1.

## 2. The main estimand — M − L in Surgeon-final tasks, per model

Surgeon-final = every surviving intended edit was authored by Surgeon (CLI or MCP). Multiple surviving producer classes are **mixed**, never last-tool-wins; missing provenance is **unknown**, never folded into a class.

| model | L Surgeon-final | M Surgeon-final | **M − L** | Astra's bar | held? |
|---|---:|---:|---:|---|---|
| gpt-6-astra | 0/13 | 10/13 | **+10** | ≥ +3 and ≤1 accepted completion lost | YES |
| gpt-5.6-sol | 0/13 | 4/13 | **+4** | ≥ +2 and ≤1 accepted completion lost | YES |

## 3. Astra's bet table, beside the measurement

Each cell is **gpt-6-astra / gpt-5.6-sol**, tasks out of 13. `bet` is Astra's forecast written before any caller ran; `got` is measured.

| arm | Surgeon final (bet) | Surgeon final (got) | Program final (bet) | Program final (got) | Accepted (bet) | Accepted (got) |
|---|---|---|---|---|---|---|
| L | 1 / 0 | 0 / 0 | 4 / 0 | 0 / 0 | 8 / 9 | 7 / 9 |
| P | 4 / 2 | 4 / 5 | 2 / 0 | 2 / 0 | 8 / 9 | 7 / 9 |
| M | 6 / 4 | 10 / 4 | 1 / 0 | 0 / 0 | 8 / 9 | 7 / 9 |
| X0 | 1 / 0 | 0 / 0 | 2 / 0 | 0 / 0 | 8 / 9 | 7 / 7 |
| X1 | 3 / 0 | 0 / 0 | 1 / 0 | 0 / 0 | 8 / 9 | 7 / 9 |
| N *(not run)* | 0 / 0 | — / — | 4 / 0 | — / — | 8 / 9 | — / — |

## 4. What actually produced the surviving bytes

| arm | model | n | surgeon-cli | surgeon-mcp | patch | program | editor | mixed | no write | ambiguous |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| L | gpt-6-astra | 13 | 0 | 0 | 12 | 0 | 0 | 1 | 0 | 1 |
| L | gpt-5.6-sol | 13 | 0 | 0 | 12 | 0 | 0 | 1 | 0 | 1 |
| P | gpt-6-astra | 13 | 4 | 0 | 5 | 2 | 0 | 2 | 0 | 2 |
| P | gpt-5.6-sol | 13 | 5 | 0 | 6 | 0 | 0 | 2 | 0 | 2 |
| M | gpt-6-astra | 13 | 0 | 10 | 2 | 0 | 0 | 1 | 0 | 1 |
| M | gpt-5.6-sol | 13 | 2 | 2 | 5 | 0 | 0 | 4 | 0 | 4 |
| X0 | gpt-6-astra | 13 | 0 | 0 | 13 | 0 | 0 | 0 | 0 | 0 |
| X0 | gpt-5.6-sol | 13 | 0 | 0 | 12 | 0 | 0 | 0 | 1 | 0 |
| X1 | gpt-6-astra | 13 | 0 | 0 | 13 | 0 | 0 | 0 | 0 | 0 |
| X1 | gpt-5.6-sol | 13 | 0 | 0 | 12 | 0 | 0 | 1 | 0 | 1 |

Census-compatible `git apply` program calls (the 2026-09-08 census counting rule, kept beside the finer taxonomy): 22 across all scored runs.

## 5. Oracle and acceptance

One run of the 130 did not complete: `R-03-workspace-string-alias__X0__gpt-5.6-sol` died after 19 s with the provider's own *"Selected model is at capacity"* and wrote nothing. It is a **caller error, not a zero-adoption result**, it stays in every denominator, and it was **not re-run** — so X0/gpt-5.6-sol reads 7 accepted where the other gpt-5.6-sol arms read 9, and one of those two is this capacity failure rather than anything the arm did.

| arm | model | n | oracle pass | finished | gate green | gate UNKNOWN | **accepted** |
|---|---|---:|---:|---:|---:|---:|---:|
| L | gpt-6-astra | 13 | 10 | 13 | 10 | 0 | **7** |
| L | gpt-5.6-sol | 13 | 11 | 13 | 10 | 0 | **9** |
| P | gpt-6-astra | 13 | 10 | 13 | 10 | 0 | **7** |
| P | gpt-5.6-sol | 13 | 11 | 13 | 10 | 0 | **9** |
| M | gpt-6-astra | 13 | 10 | 13 | 10 | 0 | **7** |
| M | gpt-5.6-sol | 13 | 12 | 13 | 10 | 0 | **9** |
| X0 | gpt-6-astra | 13 | 10 | 13 | 10 | 0 | **7** |
| X0 | gpt-5.6-sol | 13 | 9 | 12 | 10 | 0 | **7** |
| X1 | gpt-6-astra | 13 | 10 | 13 | 10 | 0 | **7** |
| X1 | gpt-5.6-sol | 13 | 12 | 13 | 10 | 0 | **9** |

## 6. Complete wall, with failed-run time kept separate

Wall is the caller's own request-to-exit time. The repository gate's wall is recorded separately and is NOT folded into it: a wall number that silently acquires a second gate is a different measurement wearing the first one's name.

| arm | model | accepted runs: median wall (s) | all runs: median (s) | failed/timeout runs: total wall (s) | median gate wall (s) |
|---|---|---:|---:|---:|---:|
| L | gpt-6-astra | 89.5 | 113.0 | 0.0 | 12.8 |
| L | gpt-5.6-sol | 129.3 | 135.9 | 0.0 | 10.4 |
| P | gpt-6-astra | 124.4 | 124.4 | 0.0 | 48.1 |
| P | gpt-5.6-sol | 200.9 | 210.3 | 0.0 | 12.3 |
| M | gpt-6-astra | 89.5 | 91.1 | 0.0 | 8.3 |
| M | gpt-5.6-sol | 191.1 | 191.1 | 0.0 | 10.1 |
| X0 | gpt-6-astra | 153.7 | 142.6 | 0.0 | 14.0 |
| X0 | gpt-5.6-sol | 144.0 | 125.4 | 19.0 | 10.5 |
| X1 | gpt-6-astra | 80.8 | 87.2 | 0.0 | 12.5 |
| X1 | gpt-5.6-sol | 155.4 | 129.3 | 0.0 | 31.5 |

Astra's cost bet is accepted-run median **M/N** complete wall 1.30 (astra) and 1.25 (sol), and **X1/X0** 1.15 and 1.00. N was not run, so the M/N ratio is **unmeasured**; the M/L ratio is reported in its place and is a different quantity.

| model | M/L accepted median wall | X1/X0 accepted median wall |
|---|---:|---:|
| gpt-6-astra | 1.00 | 0.53 |
| gpt-5.6-sol | 1.48 | 1.08 |

## 7. Per-task detail

| task | family | L·astra | L·sol | P·astra | P·sol | M·astra | M·sol | X0·astra | X0·sol | X1·astra | X1·sol |
|---|---|---|---|---|---|---|---|---|---|---|---|
| I-01-mission-cli-stale | I | patch x | mixed ✓ | S-cli x | patch ✓ | S-mcp x | patch ✓ | patch x | patch x | patch x | mixed x |
| I-02-mission-cli-admitted-profiles | I | mixed x | patch ✓ | prog x | patch ✓ | mixed x | mixed ✓ | patch x! | patch ✓ | patch x | patch ✓ |
| I-03-heap-sample-retention | I | patch x | patch ✓ | patch x | S-cli ✓ | S-mcp x | S-cli ✓ | patch x | patch ✓ | patch x | patch ✓ |
| I-04-mission-display-decision-view | I | patch ✓ | patch ✓ | mixed ✓ | S-cli ✓ | S-mcp ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓! | patch ✓ |
| R-01-core-discovery-require-edn | R | patch ✓ | patch ✓ | patch ✓ | patch ✓ | S-mcp ✓ | mixed ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓ |
| R-02-recovery-require-measured | R | patch o | patch o | patch o | patch o | S-mcp o | patch o | patch o | patch o | patch o | patch o |
| R-03-workspace-string-alias | R | patch ✓ | patch ✓ | patch ✓ | mixed ✓ | S-mcp ✓ | patch ✓ | patch ✓ | none x | patch ✓ | patch ✓ |
| R-04-namespace-split-require-cheshire | R | patch ✓ | patch ✓ | S-cli ✓ | S-cli ✓ | S-mcp ✓ | S-cli ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓ |
| T-01-outline-memory-spec-marker | T | patch o | patch o | patch o | patch o | patch o | mixed o | patch o | patch o | patch o | patch o |
| T-02-study-row-line-range | T | patch ✓ | patch x | S-cli ✓ | S-cli x | S-mcp ✓ | S-mcp x | patch ✓ | patch x | patch ✓ | patch ✓ |
| T-03-alias-migration-kind-count | T | patch ✓ | patch ✓ | S-cli ✓ | mixed ✓ | S-mcp ✓ | S-mcp ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓ |
| T-04-lane-manifest-total | T | patch o | patch x | prog o | S-cli x | patch o | mixed o | patch o | patch x | patch o | patch o |
| example-R-e4 | R | patch ✓ | patch ✓ | mixed ✓ | patch ✓ | S-mcp ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓ | patch ✓ |

`✓` accepted · `o` oracle passed but the gate did not clear or the run did not finish · `x` oracle failed · `!` the boundary fired in that run.

## 8. Every bet Astra wrote down, in one sentence each

* **Main bet — M beats L by >= 3 Surgeon-final tasks (astra) and >= 2 (sol), without losing more than one accepted completion.** Measured M-L = +10 for gpt-6-astra (10/13 vs 0/13) and +4 for gpt-5.6-sol (4/13 vs 0/13); accepted completions changed by +0 and +0. **HELD** for both callers.
* **Plate bet — P-L at least +2 (astra) and +1 (sol).** Measured +4 and +5. **HELD**.
* **Incremental MCP bet — M-P at least +1 for each caller.** Measured +6 for gpt-6-astra and -1 for gpt-5.6-sol. **MISSED** (it held for one caller and not the other).
* **Exposure bet — the PATH boundary fires in 3/13 gpt-6-astra tasks and 0/13 gpt-5.6-sol tasks.** Measured 2 firings across the 26 gpt-6-astra X0/X1 runs and 0 across the 26 gpt-5.6-sol ones; **0 of them fired before the edit had already been written**. The bet was close in direction and low in magnitude for astra and exactly right for sol.
* **Conditional repair bet — of exposed gpt-6-astra tasks, next-route Surgeon in 1/3 (X0) and 2/3 (X1), and two extra accepted Surgeon completions in X1-X0.** **NOT TESTABLE as run.** Both firings happened AFTER the edit was already written, so there was no next route to take: X1-X0 Surgeon-final is +0 and accepted is +0, and neither number is evidence about repair.
* **Astra's per-arm Surgeon-final forecasts.** L 1/0 vs measured 0/0; P 4/2 vs 4/5; M 6/4 vs 10/4; X0 1/0 vs 0/0; X1 3/0 vs 0/0.
* **Program-final forecasts.** Astra bet 4/0 (L), 2/0 (P), 1/0 (M), 2/0 (X0), 1/0 (X1). Measured 0/0, 2/0, 0/0, 0/0, 0/0. The direction of the L bet is the big miss: this caller does not reach for a string program by default at all; its default is `apply_patch`, which is a structured patch and not the census's `program` class.
* **Acceptance forecasts (8 astra / 9 sol in every arm).** Measured 7/7/7/7/7 (astra) and 9/9/9/7/9 (sol) across L, P, M, X0, X1. Acceptance is essentially flat across arms, which is what Astra predicted: routing moved, acceptance did not.
* **Cost bet — accepted-run median M/N wall 1.30 (astra), 1.25 (sol); X1/X0 1.15 and 1.00.** The M/N half is **unmeasured** (arm N was not run). Against L instead, M/L is reported in section 6.

## 9. What stays UNKNOWN

* **Refusal warrant.** Every run records `:refusal/warrant :unknown`. "Unwarranted" means a refusal on a request the oracle would later have shown correct, and the refused request was never executed. Nothing here adjudicates it.
* **Arm N.** Not run. Its four forecast cells are unmeasured, and the M/N cost ratio Astra bet on cannot be computed.
* **Blinded grading.** Astra asked for two blinded graders on every exposed run and every ambiguous final producer. This report names the ambiguous runs (§4) instead of grading them; they are pending audit, not resolved.
