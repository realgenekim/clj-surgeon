# Two-hour trial close-out — DRAFT for Astra's red-line (Gene verdict 18:50Z)

Gene, 16:5xZ: "Okay. Two hour trial. If we can't get this right. I've gotta pull the plug on this. Juice not worth the squeeze." … "Go… Stay focused on wall improvements!… You have 2 hours to generate done wins" … "Watching LLMs edit code, i am fully convinced there is juice to be squeezed. 2-4x potentially in sight wall time."

## Block ledger, 16:47–18:50Z (UTC; Pacific = −7 h)
| block | seat | kind | tried | native wall | tool wall | result | receipt |
|---|---|---|---|---|---|---|---|
| fanout-I | fable | tock | informed batched route vs native, fresh actors, 4 pairs | 101.2 s (proof-incl. median) | 57.8 s | WIN 1.75x; tool 4/4 correct, native 3/4 (witness layout class) | docs/observations/2026-09-06-fanout-I-result.md |
| wiring-instruments | fable | tick | block ledger + cadence watcher + cron + role skills v1 (Astra's 5 amendments folded) | none | none | done; 57/57 tests; cron live 17:00Z | ~/bin/block-ledger, ~/bin/cadence-watch, skills/fable-overseer, skills/astra-frontier |
| whole-maven | astra | tock | one real feature on the-gene-maven, native vs tool, independent acceptance | 130.6 s | 135.1 s | LOSS 1.04x as long; both correct and committed | Astra's note 17:12Z; his fx dir |
| fanout-J-prep | fable | tock | served discovery (one inspect match batch) vs I | 101.2 s (ref) | 59.7 s | LOSS: ≈ I on wall; 0/4 under the spelling-sensitive witness | docs/observations/2026-09-06-fanout-J-result.md |

(Ledger row fanout-J-prep was closed with tool-wall 57.8 by mistake; the J proof-inclusive median is 59.7 s. Corrected here; the ledger is append-only.)

## Versus native wall, everything measured tonight (proof-inclusive medians unless marked)
| task class | native | tool | ratio | n | correctness | caveat |
|---|---|---|---|---|---|---|
| 20-file fan-out, deterministic route (cohort B, 10:45Z) | 84.7 s | 237.6 s | 0.36x LOSS | 4 pairs | native 10/10, tool 1/4 | wrong route; refusal text since fixed |
| 20-file fan-out, informed batched route (I) | 101.2 s | 57.8 s | 1.75x WIN | 4 pairs | tool 4/4, native 3/4 | route mandated; B's controls as floor; correct-only 1.64–1.69x |
| 20-file fan-out, served discovery (J) | 101.2 s (ref) | 59.7 s | ≈ I, no gain | 4 arms | 0/4 (helper spelling vs witness) | falsified as preregistered |
| whole feature on maven (Astra) | 130.6 s | 135.1 s | 0.97x LOSS | 1 pair | both correct | one pair; task was 2 min, not 10–20 |
| 3-owner rename, typist route (earlier tonight) | 27.5–29.7 s | 15.0–15.1 s | ~1.9x actor-only | 2+2 | correct | preparation charged once ≈ 1.0x |

## What the evidence says (draft verdict, both sides)
FOR continuing: one preregistered fresh-actor class beats native by 1.75x with better audited correctness and fewer tokens in 3/4 pairs; the tool call itself is 2 s; the losing arms lose on the actor's understanding time and on textual witness sensitivity, not on the tool.
AGAINST: the whole-task pair — the number Gene actually buys — lost by 4%; a served owner list did not shorten the actor at all, so "discovery is the sink" was wrong; two of three squeezes tonight lost; every win so far is a mandated route on a task built for it.
Committed recommendation (Fable; Astra red-lines): [to be written at 18:40Z after the J ethnography and Astra's two-task study]

## Learnings that became ratchets tonight
- native WALL is the meter; every block a ledger row; watcher alarms with owner+action (live).
- proof-inclusive medians are the headline; actor-only is a component (Astra's audit).
- a served list is not understanding: compose time was task-comprehension time (J).
- textual witnesses (require layout, alias spelling) produce false negatives on both sides; correctness claims name the predicate.
- a JVM under another seat's timing window: one-shot refuses (usage-watch guard).
- partner wedge: three signals, exit+resume, never a kill from mtime.
