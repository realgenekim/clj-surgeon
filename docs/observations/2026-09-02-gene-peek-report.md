# Gene peek report — Surgeon program, 2026-09-02 14:57 PDT (21:57Z UTC)

## 1. Headline

**The rewiring extract verb beat stripped native on every one of nine cross-pairs, 243 s against 336 s, with zero native bytes after the verb and identical acceptance; all five of Sol's pre-registered promotion criteria PASS (rf2, n=3+3, `~/acid/receipts/rf2-score.md`).** The one-call closure stands at 1.3 s of tool time against 141–152 s of native typing, byte-identical.

*Events to the contrary:* one, and it is the gate, not the verb. z7c (gate at n=6, same task, both arms stripped) is **wall-neutral**: Z 339 s vs N 348 s, 0.975×, Welch p 0.79; returns 17.5 vs 18.0 (`z7c-score.md`). z7b's 0.76× is withdrawn: its native arm was slow (327.7 / 432.7 / 348.2 s across rs1 / z7b / z7c on the same prompt), the same shape as z3's withdrawn 20 %. Correctness holds 6/6: seven commits all complete, `verify none` never used, acceptance 108/108 PASS, churn canonical in all twelve runs. Free-choice adoption is still 0 of 10; the gate still costs ~1.9× where there is nothing to remove.

## 2. Wins vs native

| task | native | tool | ratio | correctness | n | receipt |
|---|---|---|---|---|---|---|
| extract nine forms + rewire 26 callers, mandated verb (rf2) | 336 s (sd 44), 17.3 returns, 868k tokens | **243 s (sd 12)**, 14.7 returns, 639k tokens | **0.72× wall**, 0.85× returns, every C < every N | acceptance identical; bytes_beyond_verb 0/0/0; 0 apply_patch cells | 3+3 | rf2-score.md |
| the closure alone (the move + rewire) | 141–152 s, 9–10 returns | 1.3 s, 1 call | ~110× on the step | byte-identical to reference | 3 hand runs | big-aha log "what is possible" |
| same task hand-driven with watcher (session 2) | 14.3 returns / 328 s | 8 returns / 293 s | 0.56× returns, 0.89× wall | suites green | 1 | tweezer-session-2-watch |
| fan-out alias migration N=5…80 + control (sl1) | 3–11 returns, 55–127 s, passes 2/6 | 2–3 returns, 24–27 s, passes 6/6 | 2× returns, 4.5× wall at scale | native fails 3 ways | 1 per point | sl1-score.md |
| alias migration on the real repo, fixed verb (q5z 2753f23) | — (anchor arm re-armed, sl1-R queued) | 171 files, 1,872 sites, one call, kondo delta 0 | pending sl1-R | only the six r4-allowed failures | 1 hand run | log 21:13Z |
| ritual strip on native (rs1) | 22.0 returns | 14.3 returns | −35 % returns | churn canonical | 3 | rs1-score.md |
| curtain-call fold ratchet (round two) | one arm fixed by hand (00e8f0fa) | 9 arms closed + tagged identity + adopt no longer drops person-id | — | 121/121 arms; 3,246-fact golden byte-identical; unit 1016/12599/0 | 1 build | bridge/fold-idempotence f115cc2d |

## 3. Losses vs native

| task | native | tool | ratio | note | n | receipt |
|---|---|---|---|---|---|---|
| admit gate at n=6 (z7c) | 348 s, 18.0 returns | 339 s, 17.5 returns, +13 % tokens | 0.975× wall, p 0.79 | z7b's 0.76× withdrawn (slow native arm); correctness 6/6, gate 7.4 % of wall, refusals bimodal (8 in 2 runs) | 6+6 | z7c-score.md |
| old extract without rewire (rf1) | 327 s, 22.0 returns | 406 s, 31.0 returns | 1.24× wall | the tool cut but could not sew | 2 per arm | rf1-score.md |
| gate on rung L control (z8) | 149 s | 277 s | 1.86× | nothing to remove | 4+4 | z8-score.md |
| gate on rung M at n=7 (z6) | 297 s | 293 s | flat | z3's 20 % was a slow baseline | 7+7 | z6-score.md |
| real-repo anchor rename (sl1-R, first run) | 122 s, suite red (spec hole) | 164 s, unloadable | native faster | both fixed; re-run queued behind z7c | 1+1 | log 02:35Z |
| the finder's one positive (task chases) | — | flagged a guarded write as raw | false positive | guard was three lines above the match | 1 | log 44b6a36 |

## 4. Exactly what the win is

The verb absorbs the whole decision set of a fan-out or an extraction into one call and returns a terminal receipt, so the agent never discovers sites and never re-reads what it rewired: rf2's agents went receipt → compile check with zero returns between. The boundary: where the change is already in the model's head (small edits), where the tail is small (the gate at n=6), and wherever the agent is free to decline (0 of 10).

### 4a. Storyboards (Gene, 2026-09-02: "Show in ascii art storyboards")

**1. The extraction (rf2): native cuts and sews by hand; the verb does both in one call.**

```
NATIVE (stripped, n=3, mean 336 s, 17.3 returns)        VERB :extract! :rewire-callers (n=3, mean 243 s, 14.7 returns)

 read ns ─▶ patch A (cut 9 forms) ─▶ patch B (paste,      read ns ─▶ ONE CALL {:file :forms :to :rewire-callers}
 hand-write header) ─▶ grep callers (26 sites, 5 files)              │ 1.3 s of tool time
 ─▶ patch C, D, … (2.8 apply_patch on .clj per run)                   ▼
 ─▶ compile ─▶ missed caller, back to patch ─▶ 4 suites   RECEIPT {:applied true :complete true
                                                                    :callers-unresolved [] :compile 5 ns ok}
 t = 336 s (fastest 291)                                    ─▶ compile ─▶ 3 suites   (B = 0 returns between receipt and check;
                                                            t = 243 s (slowest 253)   A = 0 native bytes after the verb, 3/3)
 Every verb run beat every native run: {230, 246, 253} vs {291, 339, 378}. Acceptance byte-identical.
```

**2. The fan-out (sl1, alias migration N=5…80): native pays per site; the verb pays once.**

```
 N=5   native: grep→edit ×5 → suite            55 s,  3 returns, PASS   │ verb: ONE CALL {from to}   24 s, 2 returns, PASS
 N=20  native: grep→sed→collision→fix→suite    90 s,  7 returns, FAIL   │ verb: same call            25 s, 2 returns, PASS
 N=80  native: writes a Python generator      127 s, 11 returns, PASS   │ verb: same call            27 s, 3 returns, PASS
 control: 240 sites in 5 files cost native the same as in 80 files → the cost is site DISCOVERY, not typing.
 Wall 4.5× at scale, returns 2×, native 2/6, verb 6/6. Real repo: 171 files, 1,872 sites, one call, kondo delta 0.
```

**3. The gate (z7c, n=6): same speed, different last frame.**

```
 NATIVE 348 s:  read → patch → patch → compile → suites → git commit        (agent decides it is done)
 GATE   339 s:  read → patch → patch → compile → suites → admit_clojure_patch verify:focused
                                                            ├─ REFUSED 0.01 s  (bash error text piped in as a patch)
                                                            ├─ REFUSED 7.5 s   (blocking lint in the post-image)
                                                            └─ COMMIT complete  7/7 commits, 0 on verify none
 0.975×, p 0.79: FLAT. The gate buys the last frame (every commit verified, no waiver path); it costs 25 s/run
 and a fat tail (8 refusals, all in the 2 slowest runs). A ratchet, not an engine.
```

**4. Where the win stops.**

```
 (a) nothing to discover (rung L control, z8): native 149 s, gate 277 s (1.86×) — the gate's own suite is pure cost
 (b) free choice: tool called 0 / 10 — the win exists under MANDATE only
 (c) small n: z3 0.80× → z6 1.00×; z7b 0.76× → z7c 0.975× — no wall claim below n=6
 (d) structural match sees the write, not the guard: (fnil conj []) flagged raw; (not-any? #(= chase-id …)) three lines up
```

## 5. Surprises

- z7b's gate win evaporated at n=6 (0.76× → 0.97×): the second cohort in a row where a small-n gate win was a slow native trio.
- The store builder's own boundary sentence was false: "no announced-speaker-removed fact" — `event.speaker-unannounced` exists; a forever key would have refused Ann's unpublish-then-publish for good.
- Sol's first-round NO-GO found my generation fix had the same flaw as the original bug: computed outside the lock.
- Adopting a legacy announced speaker removed the row and re-added it without its person-id: silent identity loss, found only by a characterization replay.
- Structural match sees the write and not its guard: the finder's only positive was a guarded site.

## 6. Learnings crystallized

- Decisions, not edits, are the agent's cost; a verb that takes the whole intent wins, a gate on a small tail does not (vision.md "The law of decisions", 30c8357; rf2 + z7c).
- The receipt is the product: A=0, B=0 in rf2 because the receipt was terminal (CLAUDE.md evening amendments).
- The review checks the premise, not the code: three of Sol's nine store findings were false premises (forever key, any-23505, IF NOT EXISTS) (log eedf25b).
- A key is a complete guard only for a relation whose every writer carries it (store builder, item 1; participation keeps both guards).
- Small-n gate wins are slow baselines until n≥6 says otherwise (z3→z6, z7b→z7c).

## 7. Best news / worst news

Best: the rewiring verb is a reproduced, pre-registered, receipt-backed win with no overlap between arms, and Sol's criteria for promotion are all met.
Worst: the gate is flat at n=6 on the task where it looked fastest, so the gate's claim is correctness, not speed.

## 8. Board (Pacific)

- z7c scored: wall-neutral, correctness 6/6 (z7c-score.md).
- sl1-R (anchor rename with the fixed q5z verb on 7895) queued behind z7c on the cohort lock; ~15:40 PDT.
- Store branch 3aac4338 (nine Sol items + generational key, unit 1032/12800/0 by the builder): my own unit run and Sol's second review running; push on both green, ~15:30 PDT.
- Fold branch f115cc2d pushed and verified; merge is yours (inb-d603ce).
- Mayor's queue (no bridge merges): admit-gate 17125fe, close-losers 205e13a, format-form-scope 62981ee, rf2-extract-rewire a66b626, q5z 2753f23.

## 9. Decisions waiting on Gene

- Merge bridge/fold-idempotence f115cc2d — inb-d603ce — recommend yes; the store branch stacks on it.
- Merge bridge/safe-refactor-1 — inb-e12ff9 — recommend yes.
- Product claim wording — recommend "the mandated rewiring verb makes extraction 28 % faster with zero native fallback; the gate makes commits verified, not faster".
- Adoption-collision winner, name-vs-id merging, blank identities (Sol's NO-GO list) — no id yet; recommend keep-not-merge until a product rule exists.
- Announce UI has no remove control (both unannounce arities are dead writers) — recommend a bead; without it nothing in production can open a new generation.
- Census verb (the finder, guard-aware) — inb-f5ee92 — recommend build after the anchor scores.

## 10. Answers to your questions today

- *Found Ann's error and triggered it?* Found by reading; now pinned by replay witness `two-anns-then-unannounce` (19 assertions red, then green, f115cc2d). *Kickass LID, generalized?* The relation law + the finder; the finder's one positive was false, the guard lesson is in inb-f5ee92. *New primitives that humiliate grep?* Structural match with enclosing path, caller proof that sees `#'x` and binding vectors, extract-with-rewire, alias migration; the census verb is next. *Value at all levels?* §2 and §3 are the ledger: verb wins, gate flat, curtain-call safer by two ratchets and one review. *Isn't the getter cleaner?* Yes; Sol agrees, lens first, no path fn (log 961186b).

