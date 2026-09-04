# clj-surgeon tech tree

*A living map: every capability shape we have tried or could try, its status, the receipt
that set the status, and the live backlog of experiments. Opened 2026-09-02T12:15:40Z on Gene's request.
Rules: a status changes only with a receipt (captain's log entry with commit timestamp, or a
bead); findings are appended, never rewritten; the backlog carries a prediction and a cost
for every item, and an item leaves the backlog only into Findings.*

Statuses: **WON** (measured, keep) · **LOST** (measured, closed) · **FLOOR** (the native
competitor) · **BUILDING** (in flight, branch named) · **OPEN** (filed, not started) ·
**MULTIPLIER** (wins only in combination).

## The tree

### Perception (questions grep answers wrong)
| node | status | lives in | receipt |
|---|---|---|---|
| `:ls-tree` workspace table of contents, grep-filterable | WON | CLI | big-aha log, winners table (df432c4) |
| `:ls-deps` / `:deps` / `:topo` / `:ls-extract` | WON | CLI | same |
| `inspect_clojure` outline / forms / owners / prepare-change | KEEP | MCP | l1 taxonomy: substituted reads only under the mandate (07:36Z receipt) |
| outline emits `defmethod` dispatch; owner refusal teaches the `{kind,name,dispatch}` form | BUILT | MCP + CLI | friction ledger item 1 (session-4 watch n=2); inb-11a6ae |
| refusals name their own field (`missing-fields` shape, `invalid-require-policy` values, `_` wildcard note) | BUILT | MCP | friction ledger items 3, 4, 6; inb-3cb0f4 |
| workspace-wide inspect (ls-tree through MCP) | OPEN | MCP | inb-f403aa |
| inspect that returns the next write's literals (the dossier) | OPEN | MCP | fleet round 6, Plan 3 (mission-design) |
| `relation_census`: classify every collection write in a `defmethod fold-event` arm as door/set/guarded/raw/unknown | BUILT | MCP + CLI | `docs/intent/relation-census/`; real-bytes fixture reproduces the false positive a structural matcher shipped |

### Writes
| node | status | lives in | receipt |
|---|---|---|---|
| native `apply_patch` | FLOOR | agent | n1, l1: one patch cell for 21 owners |
| `require_change` across N namespaces | WON | MCP | l1 Y-5, zero churn (churn attribution receipt) |
| `within` + `from`/`to` surgical edit | WON | MCP | l1 A-0, A-4, Y-0, zero churn |
| `expect_matched` basis → `unaddressed_matches` in the transaction receipt | BUILT | MCP | friction ledger item 2 (19 matched, 16 addressed); inb-a97614 |
| `:extract!` to a new namespace | WON | CLI + MCP extraction verb | no native equivalent; safe-refactor skill |
| `:mv` + `:fix-declares!`, `:rename-ns!` | WON | CLI | no native equivalent |
| owner-kind-namespace insertion (whole-file churn) | LOST; mechanism corrected: Surgeon runs standard-clojure-style whole-file on changes/basis/extraction routes, never on edits | MCP | clj-surgeon-46o re-scoped to formatter scope; big-aha log |
| per-form writes for a fan-out change | LOST | MCP | l1: 8 to 10 writes on top of a native patch (fbcaed1) |
| drift gate + typed refusals for the loser shapes | BUILT, pushed bridge/close-losers 205e13a, awaiting merge review | MCP | four red-team rounds; big-aha log |
| format only the enclosing top-level form (46o fix) | BUILT on branch bridge/format-form-scope (uncommitted, on top of close-losers 205e13a); measured over the whole src tree: 1735 forms, 67/68 files identical isolated vs whole-file, the 1 disagreement is a blank line between forms; l1 fixture 230 bytes scoped vs 569 whole-file; NARROWS gate CLOSE-021 (splice guard replaced by the post-format image, formatter bounded by a positional scope proof plus a token BAG); red team with executed probes in progress before hand-off to the mayor | MCP prepare-compiled! | format-scope-design.md MCP-OP-FMT-001..009; big-aha log |
| intent verb over N owners, tool-side discovery | OPEN | MCP | clj-surgeon-q5z; the only node that can make wall positive |

### Verification
| node | status | lives in | receipt |
|---|---|---|---|
| gate on the agent's own patch (`admit_clojure_patch`) | BUILT but LOST E1 on input grammar (unified diff vs apply_patch format); round five accepts both; not ready for merge | MCP, bridge/admit-gate | z1 receipt |
| proof before write in the warm JVM (`prove`) | OPEN | MCP | mission-design-opus Plan 3 |
| behavioural assertions in the acceptance suite (gate, not score) | OPEN | acid apparatus | s1: a marker-only suite passed a broken button (2000f1b) |
| kondo delta + focused suite in one receipt | BUILDING (inside the gate) | MCP | existing diagnostic_delta, hot_verify |

### Routing and prompts (how the agent is told about the tool)
| node | status | receipt |
|---|---|---|
| "available and expected" | LOST | n1: 1.8x wall, 2.1x actions, layering (12:45Z receipt) |
| optional, "fastest safe completion" | MEASURED: declined 3 of 3, native speed | s1 (f291f38) |
| substitution mandate + trusted receipts | LOST | s1: obeyed on reads, escaped on writes, +210 s |
| turn budget arithmetic | LOST | e3, v1: actions down, wall flat, overruns 3 of 3 |
| "count your actions" (report-only) | WON on actions only, nothing on wall | e3 at n=3, fleet round 4 |
| deliberate three-plan selection in the arm prompt | LOST | e3: acceptance worse, quality flat |
| routing plate in global AGENTS.md / CLAUDE.md | REWRITTEN to native-default | mayor, 94e43f3b, block c3c0d0f5 |

### Interfaces and callers
| node | status | receipt |
|---|---|---|
| MCP server (warm JVM, telemetry, typed refusals, dev instance) | KEEP | the substrate for the gate and proof-before-write |
| CLI wrapper as MCP substitute | LOST | k2: second layer, refuses 2.2x, schema discovery, receipt plumbing (4664188) |
| fast typist (codex spark / gpt-oss) | MULTIPLIER only, behind the gate on fan-out | arm T negative (409 s); big-aha log fd60409 |
| second caller (Claude) | OPEN | never varied; Opus closing read; Gene's decision |

### Refusal classes (the dominant tax)
| class | status | receipt |
|---|---|---|
| invalid-intent-form (2/3 of rung-M refusals) | OPEN | clj-surgeon-xio |
| ambiguous-insertion-gap (wave build) | FIXED on main 2311cc09 | b1 bisect (3ed0f84), b2 no regression (a7932cb) |
| invalid-compact-relation (every rung-L Surgeon run) | OPEN | clj-surgeon-az8 |
| batch-form-selection-failed | GONE on main | b2 ledger |
| every refusal carries an executable next_call | OPEN (design constraint) | vision.md constraints |

### Apparatus (the measuring instrument)
| node | status | receipt |
|---|---|---|
| variance floor before comparison (nine identical runs) | WON | v1 (3e26e1c) |
| per-arm attestation, server identity read from the server | WON | 5f9b674 |
| typed refusal ledger + call-site taxonomy | WON | 3ed0f84, 3ccc563 |
| staged diffs, ended-gate, completeness gate, per-slot names | WON | f7c4b22 and later |
| acceptance as gate not score | WON (rule) | 3e26e1c |
| Anvil "origin" is a stale bundle | OPEN | kc-ns5i |
| tree-scale memory battery (`make memory-battery`, MCP-OP-MEM-011) | BUILT on bridge/memory-battery; RED on main | docs/observations/2026-09-03-memory-battery-baseline.md |

## Findings (append-only; newest last)

| date | finding | receipt |
|---|---|---|
| 2026-09-02 | tool execution is 3 to 4 percent of wall; 87 percent is model time between calls; wall is the sum of returns | a369097 |
| 2026-09-02 | the shipped per-form editor costs 1.8x wall, 2.1x actions, by layering; refusals and MCP count explain nothing of wall | 6e4ff8f, 3ccc563 |
| 2026-09-02 | the insertion-gap fix introduced a refusal class; overlap fix exonerated | 3ed0f84 |
| 2026-09-02 | acceptance suite spans 0 to 4 on identical inputs; cannot score arms | 3e26e1c |
| 2026-09-02 | optional: declined 3 of 3; mandated: reads in, writes around, +210 s | f291f38 |
| 2026-09-02 | fan-out is the per-form API's worst case; native does 21 owners in one patch cell | fbcaed1 |
| 2026-09-02 | owner-kind-namespace writes re-print whole files; require_change and within are churn-free | 241e1bb |
| 2026-09-02 | main 2311cc09 shows no detected regression on the ledger (n=3) | a7932cb |
| 2026-09-02 | at n=6 nothing clears 2 sd; missing-fields is a new refusal class on main (4/6 vs 0/40 shipped); promotion held pending characterisation | big-aha log E8 |
| 2026-09-02 | the fast typist is negative alone; multiplier only on fan-out behind a gate | fd60409 |
| 2026-09-02 | ritual audit: 34% of model returns / 50% of sub-commands unmandated; git diff 465x in 80/81 runs, bd arcs from the repo AGENTS.md; naming a substitute suppresses ritual, forbidding does not (K 28%, Y 100%) | big-aha log, ritual audit |
| 2026-09-02 | E5: stale-onset defect 6/9 shipped vs 0/9 native by pre-registered predicate (p about 0.009); acceptance suite passed all six | big-aha log E5 |
| 2026-09-02 | E5 mechanism: read-less hypothesis falsified (18/18 saw the reset); insertion strategy decides it (head-guard 6/8 defective, in-block 0/10); shipped picks head-guard 7/9 vs native 1/9 | big-aha log 5173dce |
| 2026-09-02 | KERNEL: commit-compiled! is check-then-write; 8-way concurrent edit_clojure on one file lost a committed edit in 2 of 3 trials; Andon pulled, scoped to shared-instance multi-writer deployments | big-aha log, Andon receipt |
| 2026-09-02 | rt2 pooled: forbid paragraph -27% wall, -24% actions, -32% tokens on native, acceptance flat; the cheapest win of the summer | big-aha log rt2 |
| 2026-09-02 | cohort R: prohibition beats explanation; forbid -88% unmandated sub-commands, J inert; ritual is cheap per return (-3.8 returns) | big-aha log cohort R |
| 2026-09-02 | E1: gate arm 2.2x native; 69% refusals from grammar mismatch; three red teams missed it because they fed unified diffs; the free-choice arm would have found it in one run | big-aha log E1 |
| 2026-09-02 | gate round one: confinement/atomicity/TOCTOU solid; hazard detector evadable via declare, reader conditionals, do, metadata; verification_complete minted on process exit; quadratic identity; fixes in round two | big-aha log, gate round one |
| 2026-09-02 | relation_census: a structural match on the write alone calls 4 of the fixture's 7 sites `:raw`, the first of them the real `task.chase-recorded` write whose `not-any?` guard sits three lines above it; guard-dominance classification returns 1 `:raw`, and the real curtaincall-cfp-lens folds (117 arms, 11 sites) census to 0 `:raw`, 9 `:door`, 1 `:set`, 1 `:unknown` | this branch's fails-first receipt |
| 2026-09-02 | claypoole `upmap` plan phase on 48 arm-files (432 arms, 336 sites), 4 cores at load 3.2: pool 1 median 319 ms, pool 2 242 ms, pool 4 197 ms, pool 8 198 ms; receipt byte-identical across pool sizes. One file: no measurable win, as expected | this branch's bench |

## Live experiment backlog (prediction and cost on every item)

| id | experiment | prediction | cost | depends on |
|---|---|---|---|---|
| E1 | z7c DONE (gate 17125fe vs native, R3, strip prompts, n=6, mirrored order): Z 339.3 s / 17.5 returns vs N 348.2 / 18.0, 0.975x, Welch p 0.79 — WALL-NEUTRAL; z7b's 0.76x WITHDRAWN (its native arm was slow: native reads 327.7 / 432.7 / 348.2 across rs1 / z7b / z7c on the same prompt and base; the gate arm moved 330.7 → 339.3); pooled n=9 vs 12: 0.924x p 0.355. Correctness holds 6/6: 7 commits all complete, verify focused on 18/18 admit calls, verify none never used, acceptance 108/108 PASS, churn 53 x12, apply_patch on .clj 0 in every Z run; refusals bimodal (8, all in 2 runs, which are also Z's two slowest); one substantive catch (blocking-lint-findings), 7 shape refusals at 0.013 s incl. a bash error piped in as a patch; gate 25.3 s/run = 7.4% of wall; Z tokens +13% (the two refusal runs). The gate's claim is CORRECTNESS, not speed. Receipt ~/acid/receipts/z7c-score.md. z7b DONE (gate 17125fe vs native, R3, strip prompts, n=3): Z 330.7 s / 14.0 returns vs N 432.7 / 17.7; 3/3 complete, 0 fix failures, refusals 24→2 (require-removed now a note), acceptance all PASS, churn 53 ×6, gate 5% of wall; wall 0.76x direction (p 0.18), needs n=6. z7b RAN after two hand-found gate defects; z8 correction: the bad commits were verify-none, fixed ADMIT-118..120. z7 DONE (rung R3, n=3): walls Z 281 374 462 vs N 429 749 486, two Z diffs touch 1 and 0 files, scoring; z8 DONE (rung L control, n=4): Z 250 254 337 267 vs N 118 126 175 178, NOT flat, the gate's own focused suite per commit costs ~1.9x on a 2-min task, scoring; gate hand-driven (G1): one false require-removed refusal fixed on the branch (f5965ad), then a verified commit in 21 s. z6 DONE (rung M, n=7, fixed gate 2cc52fa): wall Z 293.1 vs N 296.7, p 0.90, FALSIFIER TRIGGERED, the z3 win was a slow native baseline; fix holds 15/15 complete commits, partial unreachable; refusals 23.8%; two genuine hazards caught (unbalanced post-image, blocking lint); stale-onset Z 3/7 vs N 1/7; 0 post-write probes and 0 native .clj patching hold. Rung M speed claim WITHDRAWN. z7 (gate on R3) running, z8 (L control) chained. z4 DONE (rung L): three arms converge, returns 9.25 each, canonical churn 12/12, acceptance 12/12, Welch p>0.6, Z commits 4/4 at partial/no-test-evidence, refusals 33%, adoption 0/4 (0/8 with z3). z5-replay FAILED: both defective native diffs admitted after stripping git index lines (second parser hole), commit mode wrote to disk on no-test-evidence; focused suite blind to the defect (68 tests green by hand). admit-gate WITHDRAWN from review; fix in progress (typed refusal on incomplete verification, runner-failed reason, git extended headers, real-bytes fixtures). Next: stale-onset predicate as an in-gate structural hazard detector. z3 DONE (rung M, n=4 per arm, 1ca44b4 dual grammar): mandated gate 285.8 s vs native 356.2 s (p≈0.049, n=4), 59% of native tokens, 0 refusals of 5 admit calls, 0 extra post-write probes; optional arm adoption 0/4; hazards caught 0; stale-onset Z 0/4 vs N 2/4 but acid acceptance Z 2.50 vs N 0.50 (instruments disagree); z4 (rung L) frozen, scoring; z5-replay pre-registered (defective native diffs through the gate); z1 DONE, FAILED: gate arm 2.2x native, 69% of admit calls refused on patch grammar (agents emit apply_patch format, gate parsed unified diff only), mandate abandoned 6/6, no hazard caught; fix = accept apply_patch grammar, then z3 with a free-choice arm | post-write shell calls to zero; wall within 1 sd of native; stale-onset and shadowed-kwCheck caught at the gate | 24 arm-runs, two Anvil evenings | gate branch green |
| E2 | rung L, native vs shipped, driven by Claude as caller | if Claude also declines or layers, the finding is about the tool; if it substitutes, it was about Sol | 12 arm-runs + Claude login on Anvil | Gene's decision |
| E3 | fan-out intent verb vs native on rung L and a purpose-built 21-owner parameter-threading rung | one write call; non-test actions at or below 10.5; churn within 20 percent; wall positive only on high fan-out | 12 arm-runs | q5z built |
| E4 | T2: intent by the strong model, hunks by the typist, verification by the gate, rung L | wins on strong-model tokens; wall break-even unless N exceeds about twenty | 12 arm-runs | E1, E3 |
| E5 | DONE: stale-onset predicate, one build, 9 shipped vs 9 native, rung M | shipped 6 of 9 defective, native 0 of 9, p about 0.009; skew is real | 18 arm-runs | big-aha log E5 receipt |
| E6 | BUILT 2026-09-02, adoption UNMEASURED: study ops (ls-tree as an inspect_clojure mode; deps/topo/ls-deps/ls-extract as requests ops) on bridge/study-ops-mcp b3c17bb, one kernel proved by a parity witness, CLI golden 9/10 byte-identical (the tenth fixed a never-working refusal); the free-choice cohort through the MCP has not run, so no claim. Prior:  free-choice adoption of `:ls-tree` via MCP once exposed | agents call it once at the start and read fewer files; if they do not call it, the exposure failed | 6 arm-runs | inb-f403aa |
| E7 | `prove`: load the unwritten candidate into the warm JVM and run named vars | one return replaces the focused-suite return and catches behaviour the suite misses; false-green risk from load order | prototype + 6 arm-runs | gate substrate |
| E9 | DONE: cohort R + rt2 (H n=9 vs N n=12): -24% actions, -27% wall, -32% tokens, -91% ritual, acceptance flat p 0.91; doctrine: ship the forbid paragraph on throwaway worktrees | PREDICTION FAILED: forbid removed 88% of unmandated sub-commands (-2.6 sd), substitutes did nothing; returns -3.8 (inside floor); acceptance unmoved; rt2 running for n=6 | 12 + 12 arm-runs | big-aha log cohort R |
| E8 | DONE: b2 widened to n=6 | nothing clears 2 sd; b2 "removes batch-form-selection-failed" withdrawn; missing-fields new on main 4/6 vs 0/40 shipped; promotion held pending characterisation | 6 arm-runs | big-aha log E8 |
| T3 | tweezer session 3 DONE (Gene's real work, curtaincall-cfp): structural match `(fnil conj [])` found 6 sites (5 vulnerable) in one return; the generative FOLD-IDEM property over 121 fold arms found 9 (three shapes the match was blind to + a hole in the emergency fix); branch bridge/fold-idempotence pushed (inb-d603ce, merge is Gene's); write-side idempotency key = follow-up bead. Meter: 4 driver returns, 1196 s (the delegated build dominates); my close cell bundled four concerns (protocol violation, again) | the property is the ratchet, the query is the map; a structural match's receipt carries no signal that its patterns are incomplete | 1 session + 1 build | receipt docs/observations/2026-09-02-tweezer-session-3-watch.md |
| C1 | closure catalogue DONE (docs/closure-catalogue.md): 735 candidates over 592 namespaces from clj-kondo analysis of three repos; cost model = rf1's own arithmetic (predicts 140 s vs measured 141–152 s); top-5 real wins: cfp store→event-store (170 files, 2056 sites, 20.8×), cfp events rename (two spellings, 1166 sites), surgeon validate-tool-params extraction (59 forms, 7 callers, 6× rf1's cluster), cfp web.http rename, mvr channel split (3.8×) | findings: class D (parameter threading) is the largest fan-out and NOT closable, do not build param_thread; repos are alias-uniform so files-to-read barely grows (median closable 3.4×, 24 % clear 5×), the 10× slope must be synthetic; Surgeon's :ls refuses 9/10 of cfp's biggest files on main (kondo exit-code defect, fixed on rf2, live on main) | 1 agent-run | next: run the top-5 by hand at G1 with the watcher; rf2 fix to main |
| E12 | sl1 DONE: T one call at every N (2–3 returns, 24–27 s, 6/6 acceptance, 0 refusals); N 3–11 returns, 55–127 s, passes 2/6 (discards/docstrings/strings corrupted at 5 and 40; unparseable script output at 20; wrong alias at C); control (240 sites in 5 files) = same native wall as N=80, so native's cost is SITE DISCOVERY, not files or bytes; ratio not monotone (falsifier) because native wrote a generator at 80; flagship 2/5; wall 4.5x, returns 2x, correctness decisive. Anchor sl1-R: spec amended (four path literals, two in src; allowance of six tests) and native PASSES (122 s, 172 files); tool arm FAILS at load: qualified symbols in binding-vector position and a quoted fully-qualified symbol in data position not migrated (fix in progress on q5z; re-run after 7895 restarts). rf2 (mandated rewiring extract) RAN: C 243 s vs N 336 s, no overlap, readout pending. sl1 RAN: N=5 done (T 25 s VERDICT=PASS; N 55 s FAILS form equality, diagnosis pending); chain re-armed for 10 20 40 80 C after a run-name bug; anchor R blocked on repo-local cclsp config (fix in progress). q5z 13d86bb G1 PASS at N=5. q5z 40b26b1: G1 wire pass 1 died on adapter arity (3 defects fixed, real-wire witnesses); pass 2 commits 5 files 15 sites 731 ms, oracle FAILS on alias choice (my collision rule counted locals; fix dispatched). q5z BUILT (bridge/q5z-alias-migration, base 1dc018b): alias_migration, fifth MCP tool, payload constant in N, O(1) receipt, 5 typed refusals with next_call, atomic via the kernel; lib-only extension for anchor R in progress; sl1 apparatus armed (chain-sl1 on GO-SL1, chain-sl1r on GO-SL1R, repo-R pinned as the store→event-store rename) | slope predictions in docs/observations/2026-09-02-slope-spec-sl1.md; hand-drive at N=5 (G1) before GO | 14 arm-runs | q5z suites green; server 7895 + Q5Z-SHA pending |
| E11 | rs1 DONE: ritual-strip prompt, native ×3, rung R3: returns 22.0→14.3 (−35%), wall 326.5→327.7 (flat), tokens −27%; `.cpcache` line fully obeyed (−3), skill line ignored; both wall predictions failed | Sol's mechanism: polls are cheap returns, wall is suite runtime; returns and wall are two meters on suite-bound rungs | 3 arm-runs | rf2 benchmark re-based to 14.3 / 328 s |
| T1 | tweezer session 1 DONE (branch bridge/tweezer-1 92dc72c): rf1 extraction by hand at the nREPL with a watcher; three one-line tool fixes live (docstring, imports, visibility keys dropped by `plan`); MCP sewing refused on an untouched entry; move at call 14; with a rewiring extract the hand path is 4 returns | protocol docs/tweezer-loop.md G0–G6; next: G2 naive-reader on the receipts, G5 cold shadow with the rf2 verb | 1 session, ~35 min | driver receipt in the big-aha log; watcher records in the branch |
| E10 | rf2 BUILT (bridge/rf2-extract-rewire 57e3ca0 + follow-ups): extract! rewires callers, byte-identical to the reference in ONE call; G1 by hand 1.3 s; G2 naive-reader twice NOT determinable (field names carried history; then 347 KB dry run + compile unchecked; both being fixed); G5 cold shadow 0/1 and G5b (exact command named) 0/1: free choice 0/10 today, the win exists only under mandate. rf1 DONE, LOST: N 326.5 s / 22.0 returns vs A 405.5 / 31.0 vs B 460.0 / 38.5; quality tie (a–e 6/6, churn 53 canonical 6/6); extract! used 4/4 via CLI, MCP extraction verb 0/4; shipped refused 8/8 require rewirings (invalid-compact-relation, require-change-unprovable), main 2/3; native landed the new ns in ONE apply_patch; H1 H2 H3 FAIL, H6 PASS; ethnography → rf2. rf1 design: Surgeon refactors Surgeon, rung R3 (extract nine exact-verify forms out of mcp_change_buffer.clj into clj-surgeon.mcp-exact-verify, five files, one extract! call), N vs A 7893 vs B 7889, 3+3 at 4 cores; ethnography protocol E1–E6 pre-registered, then rf2 with the top-3 typed fixes | H1–H6 on record (big-aha log aee6e8e): the structural route wins here or nowhere; H1 one extract! call replaces the cut/paste/require chain; acceptance rescore-R3.sh a–e; canonical churn 53 | 6 arm-runs, ~10 min each at 4 cores | INSTALLED, armed behind z4 done (chain-rf1) |
