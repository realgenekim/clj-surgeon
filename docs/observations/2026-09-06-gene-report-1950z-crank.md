# Gene report — the crank after the trial, 16:47Z → 19:50Z (Pacific 09:47 → 12:50)

## 1. Block ledger (machine ledger: /var/tmp/forge/block-ledger.jsonl; tick/tock over the window 57%/43%)
| block | seat | kind | tried | native wall | tool wall | result |
|---|---|---|---|---|---|---|
| fanout-I | fable | tock | informed batched route vs native, 4 fresh pairs | 101.2 s | 57.8 s | WIN 1.75x proof-incl.; tool 4/4, native 3/4 under the frozen witness |
| wiring-instruments | fable | tick | block ledger + cadence watcher + cron + role skills v1 | — | — | done; alarms caught two slips and one false alarm (fixed) |
| whole-maven | astra | tock | real feature, admit gate vs native | 130.6 s | 135.1 s | LOSS 1.03x as long; both correct |
| fanout-J | fable | tock | served discovery vs I | 101.2 s (ref) | 59.7 s | LOSS, wall-neutral; 0/4 on a spelling witness caused by my route |
| whole-maven-fast | astra | tock | typist client supplies the new forms | 125.0 s | 229.0 s | LOSS 1.83x as long; both correct; 16 outer actions vs 7 |
| loop-t-cuts | fable | tick | parallel pairs, first-pair gate, JVM-free dry-run receipt gate, prefix files | — | — | done; prereg→verdict now ≈ 10–20 min (was 20–25) |
| match-owner-counts | fable | tick | inspect match: owner_counts + file in text + source echo omitted | — | — | LANDED MCP/main 7f920915 (over a fence HOLD on a pre-existing defect, disclosed); 63 min vs a 30-min cap |
| fanout-route-plate | fable | tick | harness-default plate section, checker, install, tripwire, tag | — | — | LANDED 38e40a94; installed here (ffc885d9); tag stable/2026-09-06; skiff note written |
| explore-extract | fable | tock | extract-to-namespace vs native hand-rolled move, 2 pairs | 100.9 s | 108.4 s | LOSS 1.08x as long; 4/4 correct; server time 2% of wall |
| alias-replication | astra | tock | whole-repo alias migration, 6 fresh controls + 3 pairs | 42.9 s | 31.1 s | WIN 1.38x, gap 11.8 s > fresh 2SD 6.9 s; 3/3 correct, one call each; his ≥1.5x prediction missed |

## 2. Versus native wall, the night's exploration (proof-inclusive medians; fresh gpt-6-astra actors; Codex 0.153.3)
| class | native | tool | ratio | n | correctness | caveat |
|---|---|---|---|---|---|---|
| batched known-intent fan-out, informed route | 101.2 s | 57.8 s | 1.75x WIN | 4 pairs | 4/4 vs 3/4 (frozen witness) | route mandated; B's controls as floor |
| whole-repo alias migration (Astra) | 42.9 s | 31.1 s | 1.38x WIN | 6 controls + 3 pairs | 3/3 | fresh same-wave floor; serial controls vs parallel pairs disclosed |
| served discovery on the fan-out | 101.2 s | 59.7 s | ≈ I, no gain | 4 arms | 0/4 (my route's self-match hazard) | falsified |
| whole feature, admit gate | 130.6 s | 135.1 s | 1.03x LOSS | 1 pair | both | task ~2 min |
| whole feature, typist client | 125.0 s | 229.0 s | 1.83x LOSS | 1 pair | both | workflow overhead |
| extract-to-namespace | 100.9 s | 108.4 s | 1.08x LOSS | 2 pairs | 4/4 | tool 2% of wall; receipt disclaimer reopened verification |

## 3. Wins and losses, plainly
- Two preregistered wall wins, both in the same family: many-owner, known-intent, single-call changes (fan-out 1.75x, alias migration 1.38x). Everything else lost: two whole-feature routes, served discovery, and extraction.
- Landed and usable: owner_counts in inspect results; the fan-out route as an experimental harness default, installed here with a checker, a working tripwire, and a stable tag with a one-page skiff install note.
- The management system caught: a JVM under another seat's window, a wedged partner session (twice), a model that silently changed, one false alarm of its own, and my own false green.
- Losses that were mine: a route with a patch-then-scan hazard (J), a 63-minute landing against a 30-minute cap, a false "Landed", a hand-typed timestamp, an expectation 2x wrong on native extraction speed.

## 4. Learnings → ratchets (all filed or live)
- The tool is ~2% of every measured wall; the route around it (learning, composition, re-verification) is the cost. A receipt that disclaims semantics ("structural candidates only") buys no verification credit — reword or prove more (inb-b3b6d1).
- Order discovery before patching; exclude only the new owner (plate text).
- Caller/owner authority is the tool's plan or a symbol-aware read, never \b-anchored grep (memory).
- Parallel arms on 16 cores; window only for JVM batteries; compare wall_s, never pool serial with parallel (wiring doc).
- Partner liveness by session id, model pinned on the command line and checked against the status bar (watcher).
- One-shots end with a verdict line; the word "landed" requires an ancestry check.
- Public inspect handler does not enforce the 32 KB ceiling (inb-b60d6e); compact counts mode (inb-e02822, P2); hash policy (inb-a36079); refusal-text item parked (inb-2da8ea).

## 5. What is next (recommendation, unchanged from the close-out, sharpened by the exploration)
The product's proven square is now two cells wide: many-owner known-intent changes by one call, whether fan-out or alias migration. Freeze the surface, make that route the default everywhere it applies, meter first-attempt success on real work, and stop spending actor pairs on classes where the actor's own orientation is the wall. The next real evidence is the next naturally occurring fleet task through the installed plate.
