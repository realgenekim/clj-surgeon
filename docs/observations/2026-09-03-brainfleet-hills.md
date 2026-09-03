# Brainfleet: what's next — three independent rankings, one attack round, one verdict

*Gene, 2026-09-03 ~22:00Z, verbatim: "Look at potential hill climbs. Activate brainfleet to
prioritize what's next and go! Mayor (opus), feel free to chime in". Protocol (house rules,
"the mayor does not select either" + `fleet-poll-each-result`): Fable, Sol and Opus rank
independently WITHOUT seeing each other; each then attacks the others' top two; the
disagreement is the signal; Fable commits to a verdict and launches. The mayor contributes
live state (drain order, seats about to consume a square). Written by Fable BEFORE reading
Sol's or Opus's answer.*

## 0. Facts every ranking must respect (from the tech tree and tonight's log)

- The only vs-native result on record is the 2026-09-02 benchmark: 2x wall, 2x actions, no
  quality gain, when the tool is "available and expected"; free choice 0/10 (rf2 G5/G5b).
- Existing WINS vs native are on square 2 (fan-out) and correctness: sl1 — tool 1 call, 2–3
  returns, 24–27 s, 6/6 acceptance vs native 3–11 returns, 55–127 s, 2/6 passes (wall 4.5x,
  correctness decisive); E1 gate — wall-neutral (0.975x, p 0.79), correctness 6/6, catches
  hazards the suite passes.
- Tonight: ZERO vs-native measurements; usage watch 96/49/47 flat; 12 branches at GO
  unmerged (drain began 21:00Z, 0 merges landed by 21:52Z); apparatus GO at 77e6237 (tip).
- Everything runs locally on Anvil; arm servers 7907–7910; branch tips can be served from
  worktrees — a merge changes the receipt's provenance line, not the measurement.

## 1. Fable's ranking (written first)

| # | hill | square | pass line (machine-scorable) | prediction | cost | gated on | runnable tonight on tips? | one reason it could be waste |
|---|---|---|---|---|---|---|---|---|
| 1 | **E3 fan-out verb vs native, 21-owner rung** | 2 | tool: ONE write call; non-test actions ≤ 10.5; churn within 20%; correctness ≥ native; wall reported both ways | tool 2–3 returns vs native 9–10; wall ≤ 0.5x at N=21; tool 6/6 correct vs native ≤ 3/6 (sl1 precedent) | 12 arm-runs, 2 JVMs, ~2 h | q5z f51ceae (tip) + apparatus 77e6237 (tip) | YES | native lands the 21-owner change in one patch cell (it did once) and the tool's win is only returns, not wall |
| 2 | **E6 free-choice `:ls-tree` adoption via MCP** | 3 | agents call ls-tree once at the start and read fewer files; adoption count n/6; reads before first edit | adoption ≤ 2/6 (rf2 free choice 0/10); reads −30% in the adopters | 6 arm-runs, 1 JVM, ~1 h | study-ops 4480e3d (tip) + MEM-003 95b0881 (tip) | YES | if adoption is 0/6 the square is unmeasurable by free choice and needs a routing-prompt experiment first (E11's lesson: skill lines are ignored) |
| 3 | **E7 `prove`: load the candidate into the warm JVM, run named vars** | 4 | one return replaces the focused-suite return; catches ≥ 1 class the suite misses; false-green rate on a load-order fixture = 0 | prototype in one evening; 1 hazard class caught on the E5 stale-onset fixtures | prototype + 6 arm-runs | kernel adoption by a verb (5a2d254 latent) | NO (prototype yes; cohort no) | load order makes a false green; the square nobody has measured may be the square nobody needs |
| 4 | **Kernel first-verb adoption** (`require_change` under `with-cooperating-writes`) | enabler for 1 and 4 | verb commits through the journal; memory-red parity; zero behaviour change on the verb's golden | 4 h build + 2 review rounds | kernel 5a2d254 merged (or tip) | build yes; merge no | the kernel's value is only visible once a write fails mid-way — until then it is cost |
| 5 | **E4 T2**: intent by the strong model, hunks by the typist, verification by the gate | 1+2 | strong-model tokens −50%; wall break-even | wins on tokens only | 12 arm-runs | E3 | NO | E1 already says the gate is wall-neutral; T2 may just move cost between meters |
| 6 | **Census / fold-diff to GO** | none (hardening) | GO verdict | 1–2 more rounds each | 2 agent-hours | — | YES | fifteen rounds in, the reviewer widens the input space one notch per round; the return per round is falling |

**Two launches in the next hour, in parallel on Anvil: E3 and E6.** Both are pre-registered
(`2026-09-04-e3-e6-prestaged.md`), both run on tips, both produce a vs-native table for the
next Gene report, and they are the two squares the vision names as winnable. Everything else
either gates on them or is not on the vs-native axis.

**What Fable predicts the fleet will disagree on:** Sol will rank E7 higher (it is the novel
square); Opus will rank the kernel adoption higher (it turns latent work into value). Both are
right in a week; neither produces a number tonight.

## 4. Gene's ruling (before the fleet's answers were read), verbatim

> "I favor main functionality of speedups vs native, especially after so much non functional (but
> important) work. Let's prove it was worth it!!!! Let's target 50% functional work. But your call."
> "Let's target 50% functional work (overall). But your call."

**Fable's call:** E3 and E6 launch NOW on branch tips (q5z f51ceae + rf2 965d49e served for E3;
study-ops 4480e3d + MEM-003 95b0881 for E6; apparatus 77e6237), with the tip sha in every
attestation instead of waiting for the merges — a merge changes provenance, not the measurement.
The three hardening lanes in flight (census r15, kernel r9, fold-diff r11 — the last just closed
GO) get no successor round tonight unless a reviewer names a blocker; their remaining findings go
to the ledger. Until the Gene report opens with a vs-native table, the launch mix is 100%
functional; thereafter ≥ 50%. Sol's and Opus's rankings are appended below when read; they may
add a third hill, they do not delay these two.
