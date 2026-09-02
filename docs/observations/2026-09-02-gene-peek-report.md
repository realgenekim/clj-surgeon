# Gene peek report — Surgeon program, 2026-09-02 13:47 PDT (20:47Z UTC)

## 1. Headline

**A one-call structural verb did in 1.3 seconds what native typed for 141 to 152 seconds, byte-identical, and on the fan-out slope it was correct at every point where native failed four times out of six.** Wall 4.5× at scale, returns 2×, correctness decisive; the 10× on returns is not there because a competent native agent writes a generator at scale.

*Events to the contrary:* none against the closure itself (three of three byte-identical runs; six of six slope points green). Two boundaries stand: free-choice adoption is 0 of 10, so the win exists under mandate; and the gate costs ~1.9× where there is nothing to remove (rung L control).

## 2. Wins vs native

| task | native | tool | ratio | correctness | n | receipt |
|---|---|---|---|---|---|---|
| extract nine forms + rewire 23 sites (rf1 task) | 141–152 s to the move, 9–10 returns | 1.3 s of tool time, 1 call | ~110× closure, ~4× on the step, ~1.15× whole task | byte-identical to reference | 3 hand runs | big-aha log "what is possible" |
| same task, hand-driven to green (session 2) | 14.3 returns / 328 s (stripped) | 8 returns / 293 s | 0.56× returns, 0.89× wall | suites green | 1 | tweezer-session-2-watch |
| fan-out alias migration, N=5…80 + control (sl1) | 3–11 returns, 55–127 s, passes 2/6 | 2–3 returns, 24–27 s, passes 6/6 | 2× returns, 4.5× wall at scale | native fails 3 ways | 1 per point | sl1-score.md |
| admit gate on the extraction (z7b) | 17.7 returns, 433 s | 14.0 returns, 331 s | 0.79× returns, 0.76× wall | 3/3 complete, all verified | 3+3 | z7b-score.md |
| ritual strip on native (rs1) | 22.0 returns | 14.3 returns | −35 % returns, wall flat | churn canonical | 3 | rs1-score.md |
| fold-idempotence LID ratchet (curtain-call) | one arm fixed by hand | 9 non-idempotent arms found and closed, incl. one inside the fix | — | 121/121 arms, 0 gaps | 1 build | bridge/fold-idempotence |

## 3. Losses vs native

| task | native | tool | ratio | note | n | receipt |
|---|---|---|---|---|---|---|
| Surgeon-on-Surgeon extraction, old extract (rf1) | 22.0 returns, 327 s | 31.0 / 406 s (shipped), 38.5 / 460 s (main) | 1.24–1.41× wall | quality tie; the tool cut but could not sew | 2 per arm | rf1-score.md |
| gate on rung L control (z8) | 149 s | 277 s | 1.86× | gate's own suite per commit; nothing to remove | 4+4 | z8-score.md |
| gate on rung M at n=7 (z6) | 297 s | 293 s | flat; z3's 20 % win was a slow baseline | fix holds 15/15 | 7+7 | z6-score.md |
| real-repo anchor rename (sl1-R) | 122 s, suite red (spec hole) | 164 s, unloadable (var-form miss) | native faster | both failed; fixes in flight | 1+1 | log 02:35Z |

## 4. Exactly what the win is

An agent's cost is its count of decisions, not edits: both arms type the same bytes, and the control proved native pays for site discovery (240 sites in 5 files cost the same as in 80 files). A verb that takes the whole intent absorbs the site set in one call and returns a verdict; the gate absorbs the verification tail the same way. It stops where the model already holds the change, where there is nothing to discover, and wherever the agent is free to ignore the tool.

## 5. Surprises

- Free-choice adoption 0 of 10, even with the exact one-call command named in the task's terms.
- z3's 20 % gate win was a slow native baseline: n=4 with two 400 s runs; at n=7 it was 1 %.
- The gate committed on  because I ratified that waiver; a prompt is not a control.
- Native at N=80 wrote a correct Python generator and got cheaper; the ratio's non-monotone falsifier fired because native improved.
- The structural query found 6 of 9 vulnerable folds; only the generative property found the other three and the hole inside the emergency fix.

## 6. Learnings crystallized

- Decisions, not edits, are the agent's cost (vision.md "The law of decisions", 30c8357).
- The receipt is the product; a cold reader must be able to name its next call from it (CLAUDE.md evening amendments).
- The caller's real bytes beat every review: four instances today (memory: test-with-the-callers-real-bytes).
- Tweezers before the woodchipper; the watcher is the meter; the driver's self-count is never the figure (docs/tweezer-loop.md).
- Hand-drive every mode you ship; the ladder is per mode (memory: hand-drive-every-mode-you-ship).

## 7. Best news / worst news

Best: the two-call shape, extract-with-rewire plus gate, exists and was driven to green by hand; six field defects found and fixed in three hours.
Worst: nobody picks the tool unprompted, so every claim is a mandate claim.

## 8. Board (Pacific)

- rf2 (mandated rewiring verb vs native, n=3) running now; readout = native bytes after the extract, returns after the receipt.
- z7c (gate at n=6) queued behind it, ~15 min.
- q5z var-form fix and the anchor spec amendment in builders; anchor re-arms after both.
- Five clj-surgeon branches in the mayor's queue.

## 9. Decisions waiting on Gene

- Merge bridge/fold-idempotence on curtain-call — inb-d603ce — recommend yes; then the write-side bead.
- Merge bridge/safe-refactor-1 — inb-e12ff9 — recommend yes; X3 needs your authorization call.
- Product claim wording: "mandated verb + gate makes an agent faster and safer on fan-out and extraction" — recommend that, never "agents prefer it".
- Anchor scope: does the rename include the defining file? — recommend yes, and exempt the three path-reading tests.

## 10. Answers to your questions today

- *Surprises, learnings, wins?* Above. *Best/worst news?* §7. *What allowed us to beat native?* Every gate call removed a return instead of adding one; on the slope, one call absorbed the site set. *Nature of the win?* The parser fix let the gate accept the caller's bytes; the verb's win is closure at machine speed. *1.3 s vs native?* 141–152 s. *Good news?* §2. *No events to the contrary?* Correct for the closure; boundaries in §1. *We are tweezering?* Yes, three sessions, metered. *Why Anvil?* Claims only; discovery by hand. *Is ls-tree in the MCP kernel?* Not on main; inb-f403aa.
