# Gene peek report — Surgeon program, 2026-09-02 17:02 PDT (00:02Z UTC)

## 1. Headline

**Two receipt-backed wins today, both on real repos: the rewiring extract verb beat native on every cross-pair (243 s vs 336 s, zero native bytes after the verb, rf2 n=3+3), and on the curtain-call anchor the fixed alias-migration verb beat native on every cost axis where native was predicted to win (228 s vs 283 s, 0.69× actions, 0.64× tokens, both PASS, sl1-R n=1).** The session-4 refactor then shipped through Surgeon: 18 fold arms migrated in two transactions with zero churn outside the replaced forms, pushed as `bridge/settings-lens` aebb7b9a with 1053 tests green.

*Events to the contrary:* the gate is wall-neutral at n=6 (z7c: 339 s vs 348 s, p 0.79); z7b's 0.76× is withdrawn as a slow native arm. The gate's claim is correctness (6/6 complete, `verify none` never used), not speed. Free-choice adoption is still 0 of 10. Two apparatus false greens were found and fixed today (the chain's hardcoded "pass"; no FAN arm ever wrote a diff). The finder's first structural scan produced one false positive (a guarded write), which is why the census verb classifies by enclosing guard. Session 5 (the new study ops, hand-driven on curtain-call with the watcher) came back MIXED: the intra-file ops answer in 50–90 ms, but `ls-tree` bounded out at 13 of 116 files and no exposed op answers "who requires this namespace", the one question rg gets wrong. Two fixes are building; the adoption cohort waits on them.

## 2. Wins vs native

| task | native | tool | ratio | correctness | n | receipt |
|---|---|---|---|---|---|---|
| extract nine forms + rewire 26 callers, mandated verb (rf2) | 336 s (sd 44), 17.3 returns, 868k tokens | **243 s (sd 12)**, 14.7 returns, 639k tokens | **0.72× wall**, every C < every N | acceptance identical; bytes_beyond_verb 0/0/0 | 3+3 | rf2-score.md |
| alias migration on the real repo, the anchor (sl1-R, q5z 2753f23) | 283 s, 14 returns, 13 actions, 499k tokens; one suite run RED at load, hand-fixed unverified | **228 s**, 13 returns, 9 actions, 322k tokens; 171 files / 1,872 sites in one 62 s call | 0.81× wall, 0.69× actions, 0.64× tokens | both PASS r1–r7; trees differ in one line; native was predicted to win | 1+1 | sl1-R-score.md |
| settings-lens migration, dry plans (session 4) | 465 s planning, 18/19 arms, **149 lines touched outside guard+path** (86 whitespace), one hoist changes evaluation order | 299 s session / ~7 s in tool, 16/19 arms, **0 lines outside the replaced forms**, gate green on scratch | churn 0 vs 149 | native found the 24th settings write; Surgeon receipt did not say why 16 of 19 | 1+1 | session-4-comparison-receipt.md |
| the closure alone (the move + rewire) | 141–152 s, 9–10 returns | 1.3 s, 1 call | ~110× on the step | byte-identical | 3 hand runs | big-aha log |
| fan-out alias migration N=5…80 + control (sl1) | 3–11 returns, 55–127 s, passes 2/6 | 2–3 returns, 24–27 s, passes 6/6 | 4.5× wall at scale | native fails 3 ways | 1 per point | sl1-score.md |
| curtain-call ratchets shipped today | one emergency fix by hand (00e8f0fa) | fold (9 arms + tagged identity), store (key inside the lock, 3 Sol rounds), lens (18 arms via Surgeon), all verified | — | unit 1016 → 1040 → 1053, 0 failures | 3 branches | inb-d603ce, inb-70711c, inb-554636 |

## 3. Losses vs native

| task | native | tool | ratio | note | n | receipt |
|---|---|---|---|---|---|---|
| admit gate at n=6 (z7c) | 348 s, 18.0 returns | 339 s, 17.5 returns, +13 % tokens | 0.975× wall, p 0.79 | z7b's 0.76× withdrawn; correctness 6/6; refusals bimodal (8 in 2 runs) | 6+6 | z7c-score.md |
| old extract without rewire (rf1) | 327 s, 22.0 returns | 406 s, 31.0 returns | 1.24× wall | the tool cut but could not sew | 2 per arm | rf1-score.md |
| gate on rung L control (z8) | 149 s | 277 s | 1.86× | nothing to remove | 4+4 | z8-score.md |
| anchor, first attempt (13d86bb) | 122 s, suite red (spec hole) | 164 s, unloadable | native faster | fixed; the re-run is the §2 win | 1+1 | log 02:35Z |
| the finder's one positive (task chases) | — | flagged a guarded write as raw | false positive | guard three lines above the match | 1 | log 44b6a36 |
| session 4 addressing | — | outline names every arm `fold-event`; one refusal to learn the defmethod owner shape | +1 return | ratchet filed (inb-11a6ae) | 1 | friction ledger 8392477 |
| study ops on curtain-call, session 5 | rg: 0.01–0.02 s, q3 answer WRONG (180 vs 171) | `ls-tree` 1.76 s → 1 of 116 files, 0.98 s → 13 of 116 at the ceiling; `deps`/`topo` 0.13–0.17 s but intra-file | ~100× wall, no correctness win | names-only rendering + tree-level requirers building (inb-a0f37e, inb-0a6315) | 4 calls | tweezer-session-5-watch.md |

## 4. Exactly what the win is

The verb absorbs the whole decision set of a fan-out or an extraction into one call and returns a terminal receipt, so the agent never discovers sites and never re-reads what it rewired. On the anchor the mechanism was visible: native spent four cells re-deriving a token census to prove completeness, which the receipt discharges. The boundary: small edits already in the model's head, small verification tails (the gate), and any agent free to decline.

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

- Native was predicted to win the anchor ("uniform alias, one sed") and lost every cost axis; a uniform alias does not make the verification uniform.
- The chain's "pass" was a hardcoded word over an empty glob, and no fan-out arm had ever written a diff: one negative git pathspec on an ignored path.
- Native's dry plan touched 149 lines outside the change (86 whitespace) — the reprint cost landed on the native side this time.
- Three of Sol's nine store findings, and the mayor's composition finding, were false premises in "winners": `:extract!` wrote what `:ls` could not read. "Winner" now means composition (vision.md 47c1eee).
- 23j (target ns from the server root) reproduced live on main and was already fixed on the rf2 branch that nobody had merged.
- The study ops' `ls-tree` cannot list a 116-file tree inside its own receipt ceiling; the CLI text golden is compact, the MCP receipt is not. Built is not won.

## 6. Learnings crystallized

- A winner is a receipt the next verb accepts without hand repair; A = 0 and B = 0 on real bytes promote it (vision.md "What winner is allowed to mean").
- Characterization before every edit: whole-projection replay equality with a readable first-difference report was the gate for 18 arms (LENS-001, curtain-call).
- Structural match sees the write, not the guard: classify by dominance, target, identity, polarity, else `:unknown` (census verb, Sol's rule).
- A scoring step prints computed counts, never a label; an empty glob aborts (memory: verdict-label-was-a-noun).
- Two builders and one suite at a time on Buster; batteries on Anvil (memory: buster-builder-cap).

## 7. Best news / worst news

Best: the anchor. On the point chosen because native should win, the tool won wall, actions and tokens with an identical tree, and the refactor it enabled shipped the same afternoon through Surgeon with zero outside churn.
Worst: the gate is a correctness ratchet, not a speed win, and nobody picks any of this unprompted.

## 8. Board (Pacific)

- Branches awaiting your merge, in order: `bridge/fold-idempotence` f115cc2d (inb-d603ce) → `bridge/store-idempotency` 96387535 (inb-70711c, after the mayor's index paste) → `bridge/settings-lens` aebb7b9a (inb-554636). Main auto-deploys to live Postgres, so I did not merge.
- Mayor's owner ops on live Postgres (inb-3a9818): create the idempotency index and paste `pg_get_indexdef`; run `bin/fold-diff-checkpoint` from `bridge/fold-diff-tool` f2d8f6eb (pushed; read-only by construction; the naive read path would have created the index as a side effect).
- Running now: one builder on the study branch — names-only `ls-tree` default + `ns_grep` (inb-a0f37e) and `make mcp-serve` honouring MCP_PORT (inb-d8a635). The E6 adoption cohort (inb-c973d2) is gated on it and on the tree-level requirers op (inb-0a6315).
- Every open suggestion is a maven inbox item with owner + trigger (12 filed at your ask; list in the resume note 6eb3e68).
- clj-surgeon branches for the mayor's queue: rf2 5e6cdd2 (closes 23j/3s5/c37/dk9), census 7244141, **study ops b3c17bb** (your ls-tree nudge: inside inspect_clojure, one kernel, parity witness, a latent `format`-shadow bug found; adoption unmeasured), q5z 2753f23, admit-gate 17125fe, close-losers, format-form-scope. Contract note: the "two public tools" invariant in the one-compiler plan needs your ruling before census merges.
- Anvil: idle, lock free. Builder seat per your ruling: the existing `tester` user (brief amended: worktrees under `~/build`, never while a GO file or the lock exists; fleet seats untouched).

## 9. Decisions waiting on Gene

- Merge order above — recommend fold first once the mayor's fold-diff output is reviewed, store after the index paste, lens last.
- Product rules the fold branch deliberately did not guess: name-vs-id merging, adoption-collision winner, blank identities — recommend keep-not-merge until you rule.
- The announce UI has no unannounce control (both writer arities are dead) — recommend a bead; without it no generation ever advances in production.
- The one-compiler plan's "two public tools" invariant is stale and census adds a fifth — inb-78e75c — recommend: amend the invariant to "read tools compose through inspect; write tools stay gated", then merge census.
- Start the tester@anvil builder seat? Recommend yes when you have ten minutes: token into `~/secrets`, point it at the brief; I hand it inb-11a6ae/a97614/3cb0f4 first.

## 10. Answers to your questions today

- *Found Ann's error and triggered it?* Yes: pinned by replay witness (f115cc2d). *Kickass LID, generalized?* The relation law; the census verb ships it with evidence and `:unknown`. *Primitives that humiliate grep?* Match with enclosing path and hashes, defmethod-addressed replacement, extract-with-rewire, alias migration, relation census. *Value at all levels?* §2/§3. *Isn't the getter cleaner?* Yes; shipped as the lens. *pmap at large n?* Read verbs yes (claypoole, measured 319 → 197 ms on 48 files), write path no. *Why hadn't we moved ls-tree?* My ordering mistake; building now. *Tweezering, useful friction data?* Yes; six-item ledger with ratchets (8392477). *Load / farm to Anvil / single-task?* Cap set; a second seat on Anvil recommended, not a move. *What user?* You ruled: existing seats; `tester`. *Why did the forge answer land on dictation?* The connector's seat "bridge" is that page; answers now go to whichever surface you wrote from. *What did you learn about Surgeon usage?* Eight lines in the log (280c8d3): the win is site discovery; form-scoped replacement is the clean write; the tool does not say how to address what it shows; receipts are thinner than the driver's head; winners must compose; match sees the write not the guard; the gate buys correctness; hand-driving finds what suites miss. *Suggestions in a trusted place?* Twelve inbox items with owners and triggers (6eb3e68).
