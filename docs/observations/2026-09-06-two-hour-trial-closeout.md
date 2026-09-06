# Two-hour trial close-out — v2 STAMPED 18:0xZ (Astra's eight red-lines of 17:51Z folded; Gene: "yes on all recommendations", with two rulings below)

Gene, 16:5xZ: "Okay. Two hour trial. If we can't get this right. I've gotta pull the plug on this. Juice not worth the squeeze." … "Go… Stay focused on wall improvements!… You have 2 hours to generate done wins" … "Watching LLMs edit code, i am fully convinced there is juice to be squeezed. 2-4x potentially in sight wall time."

## Block ledger, 16:47–18:50Z (UTC; Pacific = −7 h)
| block | seat | kind | tried | native wall | tool wall | result | receipt |
|---|---|---|---|---|---|---|---|
| fanout-I | fable | tock | informed batched route vs native, fresh actors, 4 pairs | 101.2 s (proof-incl. median) | 57.8 s | WIN 1.75x proof-inclusive (1.81x actor-only); tool 4/4, native 3/4 UNDER THE FROZEN WITNESS (N3 = layout-sensitive witness false-negative, not a shown semantic defect); Astra's four-correct-per-arm prediction unmet | docs/observations/2026-09-06-fanout-I-result.md |
| wiring-instruments | fable | tick | block ledger + cadence watcher + cron + role skills v1 (Astra's 5 amendments folded) | none | none | done; 57/57 tests; cron live 17:00Z | ~/bin/block-ledger, ~/bin/cadence-watch, skills/fable-overseer, skills/astra-frontier |
| whole-maven | astra | tock | one real feature on the-gene-maven, native vs tool, independent acceptance | 130.6 s | 135.1 s | LOSS 1.03x as long proof-inclusive (1.04x actor-only); both correct and committed, both independent GO | Astra's note 17:12Z; his fx dir |
| whole-maven-fast | astra | tock | typist client supplies the new defn/deftest owners under the frozen proof, one provider call (Cerebras OSS-120B, 2.08 s, $0.0026) | 125.0 s | 229.0 s | LOSS 1.83x as long proof-inclusive (1.85x actor-only); both correct, both independent GO | Astra note 17:39Z; his fx dir |
| fanout-J-prep | fable | tock | served discovery (one inspect match batch) vs I | 101.2 s (ref) | 59.7 s | LOSS: ≈ I on wall; 0/4 under the spelling-sensitive witness | docs/observations/2026-09-06-fanout-J-result.md |

(Ledger row fanout-J-prep was closed with tool-wall 57.8 by mistake; the J proof-inclusive median is 59.7 s. Corrected here; the ledger is append-only.)

## Versus native wall, everything measured tonight (proof-inclusive medians unless marked)
| task class | native | tool | ratio | n | correctness | caveat |
|---|---|---|---|---|---|---|
| 20-file fan-out, deterministic route (cohort B, 10:45Z) | 84.7 s | 237.6 s | 0.36x LOSS | 4 pairs | native 10/10, tool 1/4 | wrong route; refusal text since fixed |
| 20-file fan-out, informed batched route (I) | 101.2 s | 57.8 s | 1.75x WIN (actor-only 1.81x) | 4 pairs | tool 4/4, native 3/4 under the frozen witness | route mandated; six historical B controls are the floor, distinct from B's four paired native runs; correct-only 1.64x proof-inclusive (1.69x actor-only) |
| 20-file fan-out, served discovery (J) | 101.2 s (ref) | 59.7 s | ≈ I, no gain | 4 arms | 0/4 (helper spelling vs witness) | falsified as preregistered |
| whole feature on maven, admit gate (Astra pair 1) | 130.6 s | 135.1 s | 0.97x LOSS (1.03x as long) | 1 pair | both correct, independent GO | one pair; task was 2 min, not 10–20; receipt: Astra pair-result + review artifacts, docs/observations/2026-09-06-astra-whole-maven-task.md (his branch 96d49433) |
| whole feature on maven, typist client (Astra pair 2) | 125.0 s | 229.0 s | 0.55x LOSS (1.83x as long) | 1 pair | both correct, independent GO | provider call 2.08 s; 16 outer actions vs 7 (JSON-decode repair, deleted-report lookup, direct proof, own-test 14→15 error); receipt /var/tmp/forge/astra-whole-maven-fast-fx/pair-result.json |
| 3-owner rename, typist route (earlier tonight) | 27.5–29.7 s | 15.0–15.1 s | ~1.9x actor-only | 2+2 | correct | preparation charged once ≈ 1.0x |

## What the evidence says (draft verdict, both sides)
FOR continuing: one preregistered fresh-actor class beats native by 1.75x on the full boundary, tool 4/4 and native 3/4 under the frozen witness, fewer tokens in 3/4 pairs; the tool call itself is 2 s. Causes of the losses are NOT isolated: T2 shows generation, glue, review, own-test repairs and extra proof; J is wall-neutral in this contrast, not a universal falsifier of discovery's value; a dead diagnostic path in the gate is a real tool paper cut.
AGAINST: both whole-task pairs — the number Gene actually buys — lost (1.03x and 1.83x as long); a served owner list did not shorten the actor; three of four wall bets tonight lost; every win so far is a mandated route on a task built for it.
Committed recommendation (Fable, written 17:4xZ from complete receipts; Astra red-lines by 18:50Z):
1. **Do not pull the plug on the one proven square; pull it on everything else.** The informed batched fan-out route is a preregistered fresh-actor 1.75x on the full boundary (tool 4/4, native 3/4 under the frozen witness). Make it the harness's DEFAULT route for the witnessed class — batched, known-intent fan-out edits — as an experimental default (one four-pair class does not earn all multi-owner edits or a production release). Mandated first-attempt success measures reliability; voluntary route choice is separate adoption evidence and is measured separately. GENE 18:0xZ: "Yes, harness default sounds good".
2. **Park the whole-task line.** Two approaches, two losses (0.97x, 0.55x), both correct. The tool components are fast (2 s admit, 2 s typist) and the workflow around them costs 100 s. A hand-drive without an actor is a feasibility SCREEN, never whole-task evidence (it excludes the intent/review/glue bottleneck we measured). Any restart must name a mechanism that removes observed whole-task work, then test fresh actors under identical proof.
3. **Stop funding gate hardening and discovery verbs for wall.** The gate reached correctness parity with no wall gain; served discovery was wall-neutral. Both keep their correctness value; neither earns another wall block.
4. **Keep the management system; it cost one tick block and already caught two slips.** Ledger, watcher, parallel pairs, first-pair gate, stub dry-run receipt, prefix files: the loop target is ≤ 10 min prereg-to-verdict — a TARGET, not achieved: tonight's root pairs ran 22.72 and 18.44 min preparation-to-independent-decision; observer and preparation costs stay visible. Warm-session savings (6–9 s) are unsupported and parked.
5. **2–4x remains a hypothesis; this class earned about 1.75x on the full boundary.** Native scripting need not degrade at 50–200 owners; any scaling comparison needs equal native scripting opportunity, recorded startup, and a witness that accepts equivalent layout/spelling before paid actors. GENE 18:0xZ, verbatim: "Don't spend more than 30m on the 50 and 200 owner quest -- we know that is our home territory; make sure tool is perfect for use in those scenarios, and then merrge to MCP/main and then move on." → no scaling cohort; a 30-minute tool-perfect block on the batched route's measured paper cuts (per-owner counts and file keys in the inspect match result), landed to MCP/main, then move on.
Both sides, plainly: FOR stopping — three of four preregistered wall bets tonight lost, and the win is on a mandated route for a task built for it. FOR continuing narrowly — the win replicated across two seats' cells and the losses each name a mechanism (route hazard, workflow overhead, textual witnesses), not a ceiling.

## Learnings that became ratchets tonight
- native WALL is the meter; every block a ledger row; watcher alarms with owner+action (live).
- proof-inclusive medians are the headline; actor-only is a component (Astra's audit).
- J: served discovery was wall-neutral in this contrast (compose flattened; an expect pre-count and a 24.5 KB result absorbed the saving); the 0/4 was the route's patch-then-scan self-match hazard scored by a spelling-sensitive witness.
- textual witnesses (require layout, alias spelling) produce false negatives on both sides; correctness claims name the predicate.
- a JVM under another seat's timing window: one-shot refuses (usage-watch guard).
- partner wedge: three signals, exit+resume, never a kill from mtime.

## Disclosures
- Fable's usage collector (a JVM) overlapped Astra's N1 from ~17:06 to 17:10Z under his timing window (load 2.4–3.5); the pair is retained without rerun; the one-shot now refuses under another owner's window.
- Ledger row fanout-J-prep was closed with tool-wall 57.8; a correction event carrying 59.7 (J proof-inclusive median) was appended to /var/tmp/forge/block-ledger.jsonl at 18:0xZ so machine readers see the right figure.
- Cohort B's six controls (04:44Z clock, historical) are the variance floor; B's own four paired native runs (10/10 correct across controls + arms) are a separate set; I is n = 4 pairs.
