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


## Evidence ledger refresh — 2026-09-04 22:1xZ (supersedes stale rows above; live ledger = docs/observations/2026-09-03-merge-queue-for-mayor.md)
| build | task / caller | comparator | result | unresolved |
|---|---|---|---|---|
| admit gate, landed 8d32d619; r16 tip 2ac33278 | SMW Dequote/Format, Sol | native same hour 6.5 min | G/GN 18.5/17.0, G3 12.7, T5 13.2 min — WITHDRAWN as a wall lever; admits first call at r16 | r17 NO-GO: overlay escapes root (truncates outside files), modes lost, tail is head — r18 building |
| feature_thread (injected receipt), tip 508f26f5 | same task, Sol | native morning mean 9.0 | 6.0 min mean of 4 (1.45×); live MCP arm 31 calls ≈ native | landing review running; frozen after |
| alias_migration (landed) | N=21 fan-out, Sol and a second caller | native same harness | 5.7–7.2× fewer write chars, byte-identical; second caller 13/16/9 s vs 51/23/41 s | wall secondary by pre-registration |
| suite spike 2ecce8c4 | clj-surgeon's own suite | round one 717 s | merge gate 150 s, fast 30 s, N=4 12/12 | r2 NO-GO: orphan formatter test; r3 building |
| GHA bridge/gha 5ce8aaea | CI | seat serial | gate 205 s green, battery 11-wide | lands with the spike; nightly dormant until default branch = MCP/main |

| 2026-09-06 | BUILDING astra/typist-route: pure typist admission + frozen dossier projection, 141 assertions; no integrated executor or performance claim yet. Existing scratch applier duplicate-file-block loss and provider identity/redaction hazards reproduced independently. | docs/plans/mission-typist-executor.md |

## Astra 2026-09-06 00:57Z: edit representation and retained losses

Fable real-1: diff candidates 0/20 applied; whole-file candidates 16/20 verified,
4/4 rounds, reported first-verified median 1.89 s. Cold Sol 3/4, median 29.68 s.
These are Fable receipts on fable/typist-real-repo, not fresh Astra measurements
or an established equal-boundary full-task ratio. Warm native controls requested.
Astra owner-forms seam has 250 assertions across 14 pure tests; candidate schema
is file/original-owner/replacement-form, with rename authority held by the planner.
Outside-owner bytes survive the splice; inside-owner semantic drift still needs
independent proof. Nested reader evaluation, attribute metadata and docstring
loss now have audit regressions. Runtime transport and ledger remain incomplete.

## Astra 2026-09-06 01:41Z: first live owner-forms keeper

`981372ee`: actual diagnostic rename emitted by one Cerebras candidate and
committed through saved mission M-1. Gate/witness/protected bytes and cleanup
passed. Cold propose+apply15.062s; executor internal2.850s; no comparative claim.
Review pending. Receipt counter/formatter timing need repair. Evidence:
[the live dogfood log](observations/2026-09-06-astra-live-dogfood.md).


### Astra: optional one-process owner-form mission entrance

`bin/mission run` saves a ready owner_forms plan and applies it by id in the same
JVM. It preserves the existing propose/apply boundaries and refuses other verbs
or an existing id. This targets the two-cold-start cost observed in the first
real-1 keeper; it is not a speedup measurement. New fake-executor and public CLI
witnesses cover saved authority, zero apply on blocked planning, one plan/apply,
help, dispatch, EDN/nonzero refusal and unchanged source. See
[the executor plan](plans/mission-typist-executor.md#optional-one-process-run-entrance).

## Astra 2026-09-06 02:35Z — typist representation and complete CLI cost

- JSON owner forms: fourpaired completecommand measurements, native4/4 median22.588s vs tool3/4 median7.405s.3.05x latency-only; reliability prediction FAILED. T4doubleescaping lost parsed docstrings, correctly refused beforewrite; all5names matched in exactJVM replay. See [retained result](observations/2026-09-06-astra-forms-cohort-result.md).
- Raw Clojure one-file definitions: optional frozenformat, exact ownercoverage, existing guardedcompiler. One live k3missionverified and3/3retainedcandidateproofs passed; concurrentreview/lowpriority makes elapsednoncomparative. Char literals, comments/meta and multfile remain unsupported. See [captain log](observations/2026-09-06-astra-live-dogfood.md).
- Single-process mission run removes one process boundary; replicated JSONcohort above charges remainingcoldstart. Lazy helperplanner loading is a specific next startuphypothesis, notmeasured yet.
- Next: rawformatreplication after freshnativevariancefloor, faithful commentattachment preservation prototype, complete observerhooks and combinedfence. Strongernativecontrolremainsmandatory; do not trade3/4reliability for a braggingratio.

### Astra 2026-09-06T03:58:11.332837+00:00: receipt and comment cuts — BUILT, not measured wins

- Strict comment source identity (98c3a1c3) replaces the false ordinal invariant.
  Independent swap/reindent/string/insertion/nested-comment probes and combined
  93 tests/529 assertions pass. Real commented-owner dogfood remains.
- Saved-mission Git receipt and BB fallback (fd76badc): combined 80 tests/564
  assertions pass; independent Git kernel review still open. BB event-only
  bookkeeping removes a cold JVM, with no replicated wall claim yet.
- Usage/history summaries distinguish actual known values from unknowns; no
  repeated proof required for a saved view.
- Routing r2 passes 14 tests/130 assertions and five-copy parity; synthetic
  admission example must be removed before installation.

Receipt and limitations: [Astra captain log](observations/2026-09-05-captains-log-astra-four-hour-comparison.md),
[current completion audit](observations/2026-09-06-astra-typist-completion-audit.md).
No cohort result is upgraded by these functional gates.

- Astra 04:40Z: Git publication safety ratchets (189e0086, e50c4403): force visibility of staged submodules despite Git config; durable publication intent and conservative undo refusal on uncertain outcomes. Combined root gate 101/827 green; independent re-review pending. Deterministic Surgeon literal-edit dogfood receipt retained at /var/tmp/forge/astra-git-submodule-fx/receipt.edn, not a speed comparison. This is proof-cost evidence, not a newly won latency square.

- Astra 05:30Z: raw-v2 completed real-derived five-owner rename loop, independent outcome audit GO: paired N4/4median25.193s,T4/4median7.655s=3.2909x; sixcontrolSD1.7856, gap clears2SD. Fourteen final sources byte-identical,28/28 independent scratch gate/witness replays pass. Prepared task only; excludes orientation and intent/proof preparation, no free-choice adoption or general/Astra superiority. Earlier JSON reliability loss retained. Report: docs/observations/2026-09-06-astra-raw-cohort-v2-result.md. Actual keeper6022ad35 separately demonstrates productive use in7.08s, with malformed candidate refused.

### Astra 2026-09-06T08:15:56.611443+00:00: prepared real-repository typing win; preparation remains costly

- Maven3owner/1file rename, actual app/JVM closure, independently replayed10outcomes: Sol paired medians29.713/15.038s=1.976x; actualAstra extension27.508/15.088s=1.823x. Separate exploratory n2pairs each, no pooling/free-choice/shipping claim.
- Shared1035s task/proof preparation reduces Sol two-attempt comparison to1.028x when charged. Preparation removal/reuse is the larger frontier; cold JVM proofs cost about1.7s each.
- Actual Astra batched native patch+bothproofs into one turn after orientation. Stronger models can improve the native route too. Deterministic exact-relation reference0.197s beforeproof motivates avoiding typists when existing operations already express the intent; no complete-task ratio claimed for that reference.
- Warm proof prototype is under construction with generation/source/dependency/test binding. Immutable-byte reuse first; changed candidates restart. Not a measured warm/per-edit win.

Receipt and bounds: [Astra Maven comparison](observations/2026-09-06-astra-maven-native-comparison.md). Earlier positive and negative cohorts retain their own scopes.

- Astra 2026-09-06T08:26:57.350919+00:00: inducedprimary429→realGroq fallback transport VERIFIED once,84prompt/175completion/159reasoningsubset, unknowncost. Not a realoutage or missionfailover benchmark. Node testboundary leak fix fullgate/independentreviewGO; receipt mock leak focusedRED/GREEN, combinedgatepending. These remove observed papercuts, not new latency squares.

### Astra evidence update — 2026-09-06T09:30:10.545202+00:00

Closed-closure warm transition: actual source change and matching proof earned, one866ms observation versus existing two-cold-JVM4386ms. Startup+transition+teardown4761ms; one-shot win and100ms target unearned. Next useful comparator is batched cold proof, then repeated real transitions; receipt poison-state paper cut precedes product use. Spark: actual single availability/accounting boundary passed, production tool-free adapter still unsupported. See [Astra report](observations/2026-09-06-astra-warm-transition-and-spark.md).

### Astra stronger comparator — 2026-09-06T09:58:04.633110+00:00

Single-JVM cold proof now executed at2.612s complete command; earlier warmtransition0.866s is3.02x component ratio (one observation), replacing two-JVM5.07x as the stronger comparison. Warmstartup+transition4.294s loses isolateduse. This batching opportunity benefits both editing arms. [Evidence](observations/2026-09-06-astra-single-jvm-cold-comparison.md). Freshness narrow archive exemption landed38a2cec4 and avoided the next receipt-only battery rerun; no source budget raised.

Astra correction, 2026-09-06 10:22Z: possible competing builder JVM load was disclosed for the cold comparator. Retain the 2.612s observation; 3.02x is potentially contaminated, not a clean routing estimate. No replacement run. [Disclosure](observations/2026-09-06-astra-single-jvm-cold-comparison.md).

Astra2026-09-06: completed native-control traces show script-generated batch patches, with repeated custom-proof repair after the edit and occasional output framing errors. Hypothesis: reusable independent proof and reliable output handling may save more decisions than faster replacement. No causal or paired speed claim yet; [observations](observations/2026-09-06-astra-native-control-usage.md).

Astra2026-09-06 D1:7refusals/9MCPcalls drove a hybrid workaround removing/restoring18existingrequirebindings. Investigate existing-binding reuse in the compact migration API after the frozen cohort; no implementation or new speed claim. Frozen witness also rejects ordinary require-closing layout despite independent scope pass. Preserve this acceptance mismatch separately from API latency. [Evidence](observations/2026-09-06-astra-d1-refusal-and-witness.md).

Astra 2026-09-06 11:28Z: one experimental Spark source-generation candidate passed original Maven kernel/scope/proofs on its first paid attempt: 4.152 s generation, 13.020 s complete paid parent. Separate unpaid preflight 10.263 s; preparation/review excluded, no native ratio or reliability claim. Production Spark route remains unsupported. [Receipt and limits](observations/2026-09-06-astra-spark-utility.md).

Astra2026-09-06: two fresh recovery-pilot callers both correct (native67.262s, tool-encouraged87.680s), but zero tool-committed sites. Caller naturally tried a gate around its native patch, requested commit without verification, received one correct refusal and used native fallback. Ordinary-batch discovery failed; no speedup/free-choice/general reliability claim. Proof-profile readiness is the next concrete question, not a larger compact editing API. [Evidence](observations/2026-09-06-astra-recovery-pilot-result.md).

### Astra 12:47Z: patch/proof integration, grounded in caller behavior

[Next-slice design](plans/2026-09-06-astra-patch-proof-usage.md): existing native
patch admission, explicit focused verification and a candidate-bound runner.
No new API. The current snapshot is partial, so native external proof commands
are not automatically admission profiles. Prepared integration must earn back
its setup cost. [Pilot follow-up](observations/2026-09-06-astra-recovery-pilot-result.md)
records the repaired mixed read/patch usage classification; service timing stays
unknown where the producer omitted it. No new speed claim or rerun.

### Astra 2026-09-06T14:33:19.384566+00:00 — existing patch gate with a real supplied profile

One real Maven clarity edit committed in a scratch workspace through the existing admission API after one retained profile-integration refusal. Candidate/live polarity checks passed; actual cold suite in the successful receipt: 2 tests, 0 failures/errors, clean lint. Success call 2.145 s, not a native ratio; profile preparation 326 s plus patch setup and repair/queue costs remain material. Restriction: one existing file in a frozen real closure, no generic runner/deletions/free-choice claim. [Evidence](observations/2026-09-06-astra-real-profile-utility.md).

Astra 2026-09-07T17:27:26Z: LID batch 1 widens intent parsing beyond MCP-OP, including
the five TEST-ISO amendment IDs. The original committed tree exposes 145
violations: 144 missing witnesses and one dangling TELEMETRY-EVENTS-001.
The telemetry row is now linked; five explicit prefix TODOs retain the other
144 as visible debt. The numeric-only proposal would have missed amendments
and conflated their witnesses with parent IDs. [Plan and debt table](plans/intent-contract-all-prefixes.md);
receipts: `/var/tmp/forge/lid-batch/surgeon-b1-report.md`. No performance claim.

Astra 2026-09-07T18:05:15.776609+00:00: LID batch 2 repairs the
prefix exemption rejected by Sol: a removed WTL-APPLY-001 marker had grown
pending debt 144 -> 145 while green. The exact ID/witness ledger now blocks
new missing pairs, repaired pairs, and orphan IDs; all three attacks turn
red, and retiring the repaired MEASURE-EVID-001 pair returns green. The
initial ledger preserves 123 IDs / 144 missing pairs. Raw audit results on
identical repository inputs remain byte-identical to bd854b91. The existing
non-MCP amendment regression rejects parent truncation; legacy MCP behavior
is explicitly preserved. Default changed-files kondo is warning-free.
Receipts and limits: `/var/tmp/forge/lid-batch/surgeon-b2-report.md`. No
performance claim; deferred witness debt remains unrepaired.

Astra 2026-09-07T19:11:35Z: LID gate/HLD batch wires the PERF-SENT shell
audit into the ordinary Make merge gate, outside the child-free JVM fast
lane. Scratch deletion exposed a second defect: the old single-topic regex
reported 49/49 while omitting PERF-SENT-LEDGER-ROOT-001 from a 50-row leaf.
The repaired audit reports 50/50; its permanent scratch self-test rejects
missing simple/multi-part and unknown multi-part IDs. WTL and OP-ALG already
reject lost and invented witnesses under the exact per-ID ledger; their
35 and 37 pending pairs remain debt. The HLD gains three flagship operation
accounts, mission-name disambiguation and the audit's 15 leaf references.
Plan: [sentinel gate and HLD](plans/sentinel-intent-gate-and-hld.md); receipts:
`/var/tmp/forge/lid-batch/surgeon-b3-report.md`. No performance claim or
mission-ledger implementation change.

## Structural match recovery — 2026-09-07T21:23:46+00:00

Pair-1/A-T1 (inb-f313b8), Recommendation 4 batch 1: native probes reproduce
the missing `#()` call-body view; bare-symbol, set, quote and syntax-quote
descent already worked. Added the unexpanded anonymous call-body view while
retaining original reader bytes/address. Cardinality failures now identify all
evaluated failing requests with expected/actual counts in data and text; the
longer-pattern hint requires scoped shape evidence. Existing match-test bodies
remain byte-identical. Linked intent: MCP-OP-MATCH-001..003 in
[the operation-contract registry](intent/mcp-operation-contract/mcp-operation-contract-specs.md).
Red/green receipts and required gates: `/var/tmp/forge/plan2/friction1-report.md`.
This is a correctness and recovery repair; no battery or performance claim.

## Cardinality receipt integrity — 2026-09-07T21:59:25+00:00

Sol reproduced a forged receipt line through `rogue<U+2028>→ forged` at
`7acf599b`. Round 2 routes quoted cardinality IDs/files and diagnostic notes
through the existing bounded safe-line encoder. Structured identity and ordered
counts remain exact; display collisions are distinguished by request index.
Two additive witnesses reproduce the public attack and cover escaped strings,
hostile paths/notes, truncation and complete failure membership. Red: 14
failures; affected green: 77 tests / 803 assertions. Linked intent:
MCP-OP-MATCH-004 and MCP-OP-EDIT-038. Reader-discard exclusion remains deferred
as MCP-OP-MATCH-005; base matching semantics are unchanged. Gates and doubts:
`/var/tmp/forge/plan2/friction1-r2-report.md`. No performance claim.
- 2026-09-07, `astra/namespace-split`: Andon inb-731473 repairs anchored destination
  namespace identity and helper-classified caller handoff. Five regression witnesses;
  588 fast tests green, operation oracle green, kondo zero warnings/errors. Evidence:
  `/var/tmp/forge/plan2/split-build-report.md`. Correctness repair, no wall claim.

- 2026-09-07, `astra/namespace-split`: whole namespace partition compiler, one
  captured reference analysis and final graph, shared extraction transaction/proof/
  inverse, MCP `namespace_split` and CLI `:split-ns!` with the same plan-only
  projection. The d9205abc views copy passes all four acceptance oracles: 141
  owners, 20 destinations, five callers and 87 sites. Final ready-mapping CLI wall
  is 30.33 s including those checks; mapping/profile setup is excluded. This is a
  feasibility result, not a matched fresh-caller native crossover or routing claim.
  [Design](intent/helper-extraction/namespace-split-design.md); full receipts and
  caveats: `/var/tmp/forge/plan2/split-build-report.md`.

- 2026-09-07T23:35:46.950536+00:00, `astra/namespace-split`: Cell C paper cuts
  repaired with NS-SPLIT-016..021 and eight new witnesses. Candidate lint is
  baseline-relative and blocks new error findings before publication; caller
  requires preserve indentation/order/trivia, ns docstrings preserve raw tokens,
  prose mentions are advisory, and checks name their commands and wall. Fresh
  d9205abc split passes all four oracles. Observed in-call 33.866 s versus original
  D1/D2 30.666/30.382 s; added candidate lint costs 3.375 s, no verification cut.
  One functional replay, not a replicated crossover claim.
  [Proof, red/green witnesses and retained warning delta](observations/2026-09-07-namespace-split-papercuts.md).

- 2026-09-08T00:15:34.529206+00:00, `astra/namespace-split`: round-2
  NS-SPLIT-022..027 replace cloned docs with explicit/generated role summaries,
  prune FQ-only imports, preserve grouped caller and sorted host-indented destination
  requires, align ordinary/anonymous/nested call heads, and report unrequired
  qualified references as advisory. Fresh d9205abc split passes four oracles,
  isolated whole-snapshot lint delta 0/0; direct view lint has no new warning
  identities, but repeats one pre-existing store warning per destination (14 → 15
  raw rows). In-call before 35.393 s, after 33.391 s, with suite variation explaining
  the difference; no new native crossover claim.
  [Witnesses, raw warning caveat, source diff and proof](observations/2026-09-08-namespace-split-papercuts-round2.md).


### 2026-09-08T00:59:47.250939+00:00 — namespace split round 3

Branch `astra/namespace-split`, based on `5b78bed2`: exact retired-require position
now passes the independent paper-cut oracle (zero new findings). The optional
existing-workspace nREPL probe rolls back before cold verification on failure.
Cold proof: 42.579 s including 11.609 s warm + 22.222 s kaocha; warm-only: 19.359 s,
explicitly incomplete proof. Eight logged loops cost 95.121 s in aggregate; the
<5 s reset/split target was not met (two analyzer calls alone ~5.53 s). No native
control was run, so no new comparative speed claim. The first cold gate found
test-lane and nested-reporting defects invisible to the focused warm tests.
Receipt: [round 3](observations/2026-09-08-namespace-split-papercuts-round3.md).
The third final make attempt passed: 786 JVM tests / 9,983 assertions and 873 BB
tests / 7,511 assertions, zero failures/errors. After harness repair, the second
attempt caught stale corpus pins and missing EARS @spec links; their 46-test
warm audit passed before the final cold gate. No product behavior changed after
the measured real fixture calls.


### 2026-09-08T01:23:12.498218+00:00 — namespace split round 4

Sol’s nested-body drift on `5b78bed2` is reproduced by faithful string/do
inputs and list, vector, map, set, anonymous-function, comment and closing-line
boundaries, in both alias-width directions. Structural spans now protect lines
owned by later-opened forms while allowing their opener and outer siblings to
align. The exact d9205abc forms/polish headers pin round 3’s in-place require
repair. NS-SPLIT-032/033; two witnesses fail with 15 assertions on the reviewed
compiler and pass on the repair. The image oracle is clean, but it did not
detect the synthetic nested-body defect; those witnesses remain necessary.
The merged manifest source census is 1,489 tests. Final gates, loop timings
and exclusions: `/var/tmp/forge/plan2/cellC/astra-round4-report.md`.
No native control was run; no comparative performance claim.


### 2026-09-08T02:11:49.709534+00:00 — namespace split round 5

Fable's ruling supersedes round 4's nested-body protection and exact-column
predicate. NS-SPLIT-032 now shifts every line structurally inside a changed call,
at any depth, except multiline string contents. Shared lines sum each changed
head's own width delta once. Sol's unindented growth/shrinkage literals fail in
both the helper and compile-split on 14c0501f; the revised layout matrix records
16 red assertions before the production change. The first green warm run passes
33 tests / 305 assertions, with a 10.924 s split/oracle/test loop and PAPERCUTS: 0.
The independent fixture oracle is unchanged and was also zero on the red code;
the literal witnesses supply the missing detection. Later collection-boundary
coverage and final gates are recorded in `/var/tmp/forge/round/r5-report.md` and
`/var/tmp/forge/round/r5-loop.log`. No comparative performance claim.


### 2026-09-08T04:27:53.453439+00:00 — B03 routing prep

Branch-only doctrine suspends the informed 3/21-site fan-out route and admits
only the frozen Cell C namespace split source shape and manifest. Generic
partition routing stays native. The existing verifier gains explicit plate-only
validation, a fixed 188-line / 10,738-byte budget, doctrine identity and installed
block hashes; no-target installed checks now refuse. Ten focused tests / 142
assertions pass, and the direct intent audit is green with existing debt visible.
No global installation, push, observer qualification, fresh timing claim or
friction-low promotion. Full gates and operator commands are retained at
`/var/tmp/forge/plan2/cellC/astra-B03prep-report.md`; the first make test attempt
stopped at the pre-existing stale battery receipt.
[Intent and witness matrix](intent/agent-routing/agent-routing-design.md).

### 2026-09-08T05:22:34.331594+00:00 — B03b routing verifier repair

Sol r7 found four doctrine mutations that the 18-needle verifier accepted.
The [routing intent registry](intent/agent-routing/agent-routing-specs.md) now
owns complete passages and schemas, consumed by the verifier with declared-ID
coverage checks. Four independent mutant witnesses fail before the repair and
refuse afterward; every registered passage has a deletion witness. The plate
limits nonzero exits to the two baseline-attributed analysis checks, and the
69-line canonical skill and both worktree mirrors match its admission boundary.
Both trunk battery rows and the branch row are preserved in receipt order.
No routing performance claim, installation or push; verification and merge
receipts are in `/var/tmp/forge/plan2/cellC/astra-B03b-report.md`.

### 2026-09-08 — B07 partial-retention feasibility gate

The shared namespace split compiler now prepares snapshot-bound facts before
emission, supports a supplied subset with the source retained, qualifies both
reference directions, and applies required authorized promotions. The read-only
CLI/MCP facts path never invokes the emitter. NS-SPLIT-037..046 own the
retention, facts, oracle, analysis-cap and header-cleanup witnesses. The exact
Cell B fixture supplies 25 owners, 43 external sites, eight retained dependencies
and three promotions; the original Cell C full split still has zero papercuts.
A8's case repair exposed five unchanged baseline diagnostics, so the frozen
repair independently analyzes the exact base and rejects every new or increased
unresolved identity. The absolute-check failure and rollback remain evidence.
Build, cold gates, hand-drive receipts and the frozen sequential 24-run operator
protocol: `/var/tmp/forge/plan2/cellC/astra-B07-report.md`. This is a feasibility
experiment, with no automatic routing admission or comparative performance claim.


### 2026-09-08T08:56:05.125982+00:00 — row 3 standalone require schema

Standalone require-only MCP/CLI compilation, ordered aliases, exact counts,
comment-preserving line splices and shared proof/undo are implemented on the
branch. The frozen natural eight-file admission is **blocked**: two target
libspecs share require opener lines, and one sorted append requires moving an
inherited closer that O4 protects. One untimed original-seed handler call refuses
before write; no cohort or routing claim.
[Contradictions and retained evidence](observations/2026-09-08-row3-schema-admission.md).


### 2026-09-08T09:41:33.993231+00:00 — row-2 receipt artifact isolation

The six D-arm scratch failures reproduce in a fail-first Git witness. Verb receipt,
detail and inverse storage now uses external per-verb receipt directories, with
post-write Git exception evidence. The frozen nine-file/21-site alias request on
a fresh history-isolated seed worktree passes O1–O5 with MINOR 0. Eight new row-2
witnesses and the helper/typist boundary suites pass; no N-arm layout change or
comparative performance claim. ALIAS-MIGRATION-001/002 own the linked contract.
Final gates, review findings and the initial handler setup refusal are retained in
`/var/tmp/forge/plan2/cellC/astra-row2-papercut-report.md`.


### 2026-09-08T10:36:46.731434+00:00 — Sol r10 alias artifact landing repair

The complete alias boundary namespace at 827a751a reproduced 137 tests / 3,164
assertions with 135 failures and nine errors. Its absolute-path, retention,
configured-path guard fixtures and refusal census are repaired; the full warm
namespace passes 137 tests / 3,237 assertions. The default landing gate now runs
both this namespace and a new 13-witness publication battery. An explicit
membership witness was red before the Makefile wiring. The installed Anvil
`~/bin/land` also bypassed that gate; its local gate list now calls `make test`.

Active MCP-OP-ALIAS-019/020/026/044/045 are amended for external artifacts and
measured Git exception paths. ALIAS-MIGRATION-003 owns the mandatory execution
contract. Identical publication witnesses on 3d55fa34 produce 22 failures and
zero errors across all 13 publishers; the repaired run passes 19 tests / 156
assertions, including six reused behavior/proof/undo fixtures. This supplies
behavioral evidence beyond the earlier static census. No new performance or
row-3 admission claim. Full gates, installed launcher diff, final commit and dry
merge evidence: `/var/tmp/forge/plan2/cellC/astra-r10fix-report.md`.

Final r10 landing gate passes: 156 affected-battery tests / 3,393 assertions,
814 JVM tests / 10,377 assertions and 888 Babashka tests / 7,858 assertions,
zero failures/errors. Intent audit is clean; serialized changed-files lint has
zero errors/warnings. The first cold gate's five count/catalog failures and their
repairs remain in the report.


### 2026-09-08T13:35:31.736584+00:00 — rows sublime: proof configuration, honesty and route telemetry

NS-SPLIT-047..049 add external profile files, a named empty-profile refusal and
incomplete receipts for true-only cold profiles. The Row 5 D2–D6 receipt shape
was red before the fix; a fresh Cell C in-image loop passes 52 tests / 530
assertions with PAPERCUTS=0. Configuration adds no workspace file; the CLI
external profile path is exercised through its actual Babashka entrance.

ALIAS-MIGRATION-004/005 record refusal price and unknown caller outcomes, plus
one external ledger line per call. The full alias boundary passes 143 tests /
3,331 assertions. A fresh frozen nine-file / 21-site replay has three resolved
collisions, no workspace artifact pollution, and all five Row 2 oracles PASS.
Missing counts, fallback, first-attempt and complete verified wall are not
manufactured. Ledger failure preserves the operation result.

Single hand-driven Cell C observation on split/base2 4f6283aa (d9205abc source
plus the authorized nREPL test alias and architecture test): warm 19.736 s,
cold 43.305 s, then the hand-run pending cold gate on the warm candidate
23.915 s. Warm remains verification_complete=false with all three cold commands
pending in its immutable receipt; the separate hand gate closes them, including
231 tests / 2,258 assertions. This is one observation, without native controls
or variance qualification, and carries no routing-performance claim.

Final commit, red/green logs, intent audit and cold repository gate are recorded
in `/var/tmp/forge/plan2/cellC/astra-rows-sublime-report.md`.

### Rows sublime batch 3 — detached closure and embedded facts (2026-09-08)

NS-SPLIT-050..054 add detached background proof for warm profiles, immutable
original receipts, snapshot-checked closure/status, and bounded finished-work
facts for full and retained-source splits. One split/base2 observation returned
in 19.830 s and closed a 24.436 s background gate, leaving 24.509 s available
after return. Complete wall remains 44.339 s; this is overlap evidence, not a
matched speed claim. Cell C facts grew the receipt from 6,983 to 13,788 bytes.
Cell B retained-source acceptance passed with 25 moved owners, 43 sites, eight
retained dependencies and a 22,015-byte receipt. The graph budget must charge the
emitted summary, not the full planning graph; the real-fixture refusal is retained.
Raw evidence: `/var/tmp/forge/rows-sublime-3/`. Build report:
`/var/tmp/forge/plan2/cellC/astra-rows-sublime-3-report.md`. Routing admission and
free-choice adoption remain unchanged; Sol's fence precedes any landing.

### 2026-09-08T20:13:21.485706+00:00 — row 5 candidate negatives

Branch-only NS-SPLIT-060..066 adds scoped candidate absence scans, comment-line
evidence, actual owner multiplicities, and body hashes separating raw equality
from authorized reference/alignment/promotion replay. Committed receipts explain
proof tier/status and name proof-status without changing verification_complete.
The fresh receipt-only Codex reader distinguished committed work from pending
proof. Sol's executable fence found and earned witnesses for namesake comment
swaps, caller-rewritten namesake identity, and broader facade shapes. The initial
NO-GO and repair evidence are retained. Cell C keeps 141 owners/20 destinations
and zero papercuts; this is feasibility evidence, not a new adoption or wall claim.
Final receipt sizes, red/green gates, scope limitations and prediction:
`/var/tmp/forge/plan2/cellC/astra-rows-sublime-4-report.md`.

### 2026-09-08T22:10:57.373419+00:00 — TEST-ISO-014 battery launcher cells

The unchanged six-launch reader-eval matrix now exposes six independent deftests.
One locked default eight-lane battery run on candidate `5cdd5dcc` passed
752 tests / 13,828 assertions with zero failures, errors, skipped prerequisites
or isolation violations. End-to-end wall was 172 s against the requested
historical 216 s baseline (20.4% lower); coordinator makespan was 169.508 s.
This is one observation against a historical baseline, not a fresh matched estimate.

Measured cells are asymmetric: JVM 66.946/67.749/68.044 s; Babashka
0.308/0.354/0.298 s (deps.edn/bb.edn/project.clj). Their sum is 203.699 s;
six equal ~35 s cells were an incorrect expectation. The next indivisible
floor is the path-escape witness at 136.888 s. Namespace summed-budget semantics
and the exception remain intact. Actual test-var timings now survive grouped
shards, and unmeasured vars use the namespace-share estimate in both packing
stages; the final packer previously still charged 1 ms. Frozen pair coverage,
one launch per cell, shard eligibility and grouped timing have direct witnesses.
Report: `/var/tmp/forge/plan2/cellC/astra-battery-floor-report.md`.
Evidence: `/var/tmp/forge/battery-floor/`.
