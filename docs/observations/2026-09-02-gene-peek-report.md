# Gene peek report — Surgeon program, 2026-09-02 20:13 PDT (03:13Z UTC), seat now on Anvil

## 1. Headline

**The two receipt-backed wins stand (rf2: 243 s vs 336 s on every cross-pair; the anchor: 228 s vs 283 s where native was predicted to win), and the settings-lens refactor shipped through Surgeon with zero outside churn. Tonight's finding is the other half of "winner": of the four clj-surgeon branches the mayor's queue held as ready, none was mergeable as it stood.** Executed red-teams found two code-execution holes reachable from one inspect call on the study-ops branch (ripgrep flag injection through `grep`; reader eval on scanned deps.edn), a witnessed write outside the project root on rf2 (a directory symlink turned an unconfined read walk into an unconfined write set), schema-only bounds on census, and committed build caches on q5z. Every one has a fix round; rf2's eight fixes are already pushed and under re-review; the fence-review doctrine is widened so a read verb is reviewed like a write.

*Events to the contrary:* the gate is wall-neutral at n=6; free-choice adoption is still 0 of 10; the study ops as first built could not answer the tree-level questions they exist for (session 5, MIXED) and are now gated on the names-only rendering (landed) and the security round. Codex is unauthenticated on this seat, so Sol is unavailable until the mayor provisions it; Opus did tonight's reviews.

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

- A read-only verb had two RCE-class holes without touching any fence file: `grep` reached ripgrep as a flag (`--pre=/bin/sh` executed files in the reviewer's probe), and `clojure.core/read-string` with read-eval ran on every scanned deps.edn.
- rf2's compile check was designed right (argv, `-A` not `-M`, no `:main-opts`) and the branch still escaped the root, because the pre-existing discovery walk became a write set.
- The receipt ratchet built to catch missed sites counted two sites on one line as one: wrong failure direction, caught by its own review.
- Two fix rounds swept `.cpcache/` into commits with `git add -A`; one caught itself and rewrote its own history. `.cpcache/` is now ignored on main.
- Hand-driving the second mode of the alias rule found a ninth hole the review had not named.
- The reminder-schedule fix from yesterday was itself a regression: a slug-only payload would be silently dropped. The reviewer measured it; the writer has always carried an event id, so the practical risk is near zero, and it is still being fixed.

## 6. Learnings crystallized

- Fence review scope now covers every reachable subprocess (argv, `--`, no interpolation), every reader on repo bytes (edn only), every walked path (realpath in root), bounds before parsing, and `next_call` termination; executed, before a branch enters the queue (CLAUDE.md; memory security-boundary-review-before-merge).
- "Ready" in a queue means "unreviewed" until an executed verdict is filed beside it. Four of four failed tonight.
- Every refusal paid by hand becomes a ledger item with text, ratchet, and trigger; three built, reviewed, and now in a fix round of their own (memory friction-ledger-to-ratchets).
- Builders never `git add -A`; add files by name.
- A characterization golden proves only the histories it contains: 4 of 18 lens arms appear in the real log; the rest are pinned synthetically, adequate for a guard collapse, not for a semantics change.

## 7. Best news / worst news

Best: the review machinery works at the speed of the build machinery. Eight branches red-teamed, seven fix rounds running or landed, rf2 already pushed with every original probe closed, all in one night on Anvil at load 3 to 5 on 16 cores.
Worst: nothing in the clj-surgeon queue was mergeable when the night began, and the study ops built on your nudge were the least mergeable of all.

## 8. Board (Pacific)

- Seat: forge@anvil since ~18:15 PDT. Missing until the mayor provisions: the phone channel (no reply tool), maven creds, codex auth, clj-nrepl-eval, `~/bin`, `/opt/claude-skills`, crons, swipl (the Prolog gate cannot run here).
- clj-surgeon, pushed and re-reviewing: rf2 5ccb4f0. Fix rounds running: study ops (items 2–12), census (11), q5z (7), ratchets (8). Each → my suites → push → re-review → queue. The queue is frozen until then.
- curtain-call, for your merge in order: fold f115cc2d → store 96387535 → settings-lens aebb7b9a (GO, measured) → template-upsert (fix running: raw-id identity) → lens-followups (fix running: slug cross-check). The fold-diff tool gets its seven fixes before the mayor runs it against production; its read-only claim was near-vacuous under Postgres and its default baseline compared main against the whole stack.
- Verdict documents on main: study-ops NO-GO, census GO-WITH-FIX, rf2-q5z, folddiff-lens, ratchets.
- Prosecution list: twelve items filed yesterday plus S9 (inspect's `forms` should accept the defmethod owner map); inbox updates wait on maven creds here.

## 9. Decisions waiting on Gene

- Curtain-call merge order above. Recommend: fold after the mayor's fold-diff run (post-fix), store after the index paste, then the lens stack with goldens re-run at each step.
- The "two public tools" invariant in the one-compiler plan (inb-78e75c) before census merges. Recommend amend to "read tools compose through inspect; write tools stay gated".
- The announce UI has no unannounce control (inb-041b28). Product call.
- rf2's chosen posture: an out-of-root directory symlink anywhere under a repo now refuses every extraction there. Fail-visible as mandated; say if you want a skip-and-name posture instead before it meets a monorepo.
- Provisioning priority for the mayor: codex auth (Sol) and maven creds first, the phone channel second.

## 10. Answers to your questions today

- *What model?* Fable 5.1, same as before the move. *What host?* Anvil, user forge, 16 cores. *Crank up parallelism?* Done: up to nine lanes, suites serialized behind one lock, load 3–5. *Friction ledger?* Ratified and saved as practice. *On deck / exploring / option value?* Answered in full earlier; the highest-option item is still the tree-level requirers op, now behind the q5z merge. *Prosecution list in a trusted place?* Twelve inbox items; S9 waits on creds.
