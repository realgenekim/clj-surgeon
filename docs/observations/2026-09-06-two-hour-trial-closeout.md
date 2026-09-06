# Two-hour trial close-out — v1 with verdict (17:4xZ), Astra red-line pending, Gene verdict 18:50Z

Gene, 16:5xZ: "Okay. Two hour trial. If we can't get this right. I've gotta pull the plug on this. Juice not worth the squeeze." … "Go… Stay focused on wall improvements!… You have 2 hours to generate done wins" … "Watching LLMs edit code, i am fully convinced there is juice to be squeezed. 2-4x potentially in sight wall time."

## Block ledger, 16:47–18:50Z (UTC; Pacific = −7 h)
| block | seat | kind | tried | native wall | tool wall | result | receipt |
|---|---|---|---|---|---|---|---|
| fanout-I | fable | tock | informed batched route vs native, fresh actors, 4 pairs | 101.2 s (proof-incl. median) | 57.8 s | WIN 1.75x; tool 4/4 correct, native 3/4 (witness layout class) | docs/observations/2026-09-06-fanout-I-result.md |
| wiring-instruments | fable | tick | block ledger + cadence watcher + cron + role skills v1 (Astra's 5 amendments folded) | none | none | done; 57/57 tests; cron live 17:00Z | ~/bin/block-ledger, ~/bin/cadence-watch, skills/fable-overseer, skills/astra-frontier |
| whole-maven | astra | tock | one real feature on the-gene-maven, native vs tool, independent acceptance | 130.6 s | 135.1 s | LOSS 1.04x as long; both correct and committed | Astra's note 17:12Z; his fx dir |
| whole-maven-fast | astra | tock | typist client supplies the new defn/deftest owners under the frozen proof, one provider call (Cerebras OSS-120B, 2.08 s, $0.0026) | 125.0 s | 229.0 s | LOSS 1.85x as long; both correct | Astra note 17:39Z; his fx dir |
| fanout-J-prep | fable | tock | served discovery (one inspect match batch) vs I | 101.2 s (ref) | 59.7 s | LOSS: ≈ I on wall; 0/4 under the spelling-sensitive witness | docs/observations/2026-09-06-fanout-J-result.md |

(Ledger row fanout-J-prep was closed with tool-wall 57.8 by mistake; the J proof-inclusive median is 59.7 s. Corrected here; the ledger is append-only.)

## Versus native wall, everything measured tonight (proof-inclusive medians unless marked)
| task class | native | tool | ratio | n | correctness | caveat |
|---|---|---|---|---|---|---|
| 20-file fan-out, deterministic route (cohort B, 10:45Z) | 84.7 s | 237.6 s | 0.36x LOSS | 4 pairs | native 10/10, tool 1/4 | wrong route; refusal text since fixed |
| 20-file fan-out, informed batched route (I) | 101.2 s | 57.8 s | 1.75x WIN | 4 pairs | tool 4/4, native 3/4 | route mandated; B's controls as floor; correct-only 1.64–1.69x |
| 20-file fan-out, served discovery (J) | 101.2 s (ref) | 59.7 s | ≈ I, no gain | 4 arms | 0/4 (helper spelling vs witness) | falsified as preregistered |
| whole feature on maven, admit gate (Astra pair 1) | 130.6 s | 135.1 s | 0.97x LOSS | 1 pair | both correct | one pair; task was 2 min, not 10–20 |
| whole feature on maven, typist client (Astra pair 2) | 125.0 s | 229.0 s | 0.55x LOSS | 1 pair | both correct | component 2 s; workflow +100 s |
| 3-owner rename, typist route (earlier tonight) | 27.5–29.7 s | 15.0–15.1 s | ~1.9x actor-only | 2+2 | correct | preparation charged once ≈ 1.0x |

## What the evidence says (draft verdict, both sides)
FOR continuing: one preregistered fresh-actor class beats native by 1.75x with better audited correctness and fewer tokens in 3/4 pairs; the tool call itself is 2 s; the losing arms lose on the actor's understanding time and on textual witness sensitivity, not on the tool.
AGAINST: the whole-task pair — the number Gene actually buys — lost by 4%; a served owner list did not shorten the actor at all, so "discovery is the sink" was wrong; two of three squeezes tonight lost; every win so far is a mandated route on a task built for it.
Committed recommendation (Fable, written 17:4xZ from complete receipts; Astra red-lines by 18:50Z):
1. **Do not pull the plug on the one proven square; pull it on everything else.** The informed batched fan-out route is a real, preregistered, fresh-actor 1.75x with better audited correctness. Ship it as the harness's DEFAULT route for multi-owner/fan-out Clojure edits (route without asking — description-based adoption is dead), and measure adoption as first-attempt successes on real fleet tasks. That is the deliverable of this program.
2. **Park the whole-task line.** Two approaches, two losses (0.97x, 0.55x), both correct. The tool components are fast (2 s admit, 2 s typist) and the workflow around them costs 100 s. No third approach until someone hand-drives a complete whole-task workflow in ≤ 60 s end to end WITHOUT an actor; only then a pair.
3. **Stop funding gate hardening and discovery verbs for wall.** The gate reached correctness parity with no wall gain; served discovery was wall-neutral. Both keep their correctness value; neither earns another wall block.
4. **Keep the management system; it cost one tick block and already caught two slips.** Ledger, watcher, parallel pairs, first-pair gate, stub dry-run receipt, prefix files: the loop target is ≤ 10 min prereg-to-verdict, and it is what makes the next 2x cheap to find or cheap to kill.
5. **The 2–4x Gene sees is real for the class where the actor's work is REQUEST CONSTRUCTION over many owners, and not yet real for single-feature work.** The next bet with a wall meter is that class at larger n (50–200 owners, cross-namespace), where native scripting degrades and one batched call does not — one cohort, parallel pairs, one hour.
Both sides, plainly: FOR stopping — three of four preregistered wall bets tonight lost, and the win is on a mandated route for a task built for it. FOR continuing narrowly — the win replicated across two seats' cells and the losses each name a mechanism (route hazard, workflow overhead, textual witnesses), not a ceiling.

## Learnings that became ratchets tonight
- native WALL is the meter; every block a ledger row; watcher alarms with owner+action (live).
- proof-inclusive medians are the headline; actor-only is a component (Astra's audit).
- a served list is not understanding: compose time was task-comprehension time (J).
- textual witnesses (require layout, alias spelling) produce false negatives on both sides; correctness claims name the predicate.
- a JVM under another seat's timing window: one-shot refuses (usage-watch guard).
- partner wedge: three signals, exit+resume, never a kill from mtime.
