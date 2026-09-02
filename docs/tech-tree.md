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
| workspace-wide inspect (ls-tree through MCP) | OPEN | MCP | inb-f403aa |
| inspect that returns the next write's literals (the dossier) | OPEN | MCP | fleet round 6, Plan 3 (mission-design) |

### Writes
| node | status | lives in | receipt |
|---|---|---|---|
| native `apply_patch` | FLOOR | agent | n1, l1: one patch cell for 21 owners |
| `require_change` across N namespaces | WON | MCP | l1 Y-5, zero churn (churn attribution receipt) |
| `within` + `from`/`to` surgical edit | WON | MCP | l1 A-0, A-4, Y-0, zero churn |
| `:extract!` to a new namespace | WON | CLI + MCP extraction verb | no native equivalent; safe-refactor skill |
| `:mv` + `:fix-declares!`, `:rename-ns!` | WON | CLI | no native equivalent |
| owner-kind-namespace insertion (whole-file churn) | LOST; mechanism corrected: Surgeon runs standard-clojure-style whole-file on changes/basis/extraction routes, never on edits | MCP | clj-surgeon-46o re-scoped to formatter scope; big-aha log |
| per-form writes for a fan-out change | LOST | MCP | l1: 8 to 10 writes on top of a native patch (fbcaed1) |
| drift gate + typed refusals for the loser shapes | BUILT, pushed bridge/close-losers 205e13a, awaiting merge review | MCP | four red-team rounds; big-aha log |
| format only the enclosing top-level form (46o fix) | OPEN, feasible: isolated top-level form formats byte-identically to in-file | MCP prepare-compiled! | red team round three |
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

## Live experiment backlog (prediction and cost on every item)

| id | experiment | prediction | cost | depends on |
|---|---|---|---|---|
| E1 | z1 DONE, FAILED: gate arm 2.2x native, 69% of admit calls refused on patch grammar (agents emit apply_patch format, gate parsed unified diff only), mandate abandoned 6/6, no hazard caught; fix = accept apply_patch grammar, then z3 with a free-choice arm | post-write shell calls to zero; wall within 1 sd of native; stale-onset and shadowed-kwCheck caught at the gate | 24 arm-runs, two Anvil evenings | gate branch green |
| E2 | rung L, native vs shipped, driven by Claude as caller | if Claude also declines or layers, the finding is about the tool; if it substitutes, it was about Sol | 12 arm-runs + Claude login on Anvil | Gene's decision |
| E3 | fan-out intent verb vs native on rung L and a purpose-built 21-owner parameter-threading rung | one write call; non-test actions at or below 10.5; churn within 20 percent; wall positive only on high fan-out | 12 arm-runs | q5z built |
| E4 | T2: intent by the strong model, hunks by the typist, verification by the gate, rung L | wins on strong-model tokens; wall break-even unless N exceeds about twenty | 12 arm-runs | E1, E3 |
| E5 | DONE: stale-onset predicate, one build, 9 shipped vs 9 native, rung M | shipped 6 of 9 defective, native 0 of 9, p about 0.009; skew is real | 18 arm-runs | big-aha log E5 receipt |
| E6 | free-choice adoption of `:ls-tree` via MCP once exposed | agents call it once at the start and read fewer files; if they do not call it, the exposure failed | 6 arm-runs | inb-f403aa |
| E7 | `prove`: load the unwritten candidate into the warm JVM and run named vars | one return replaces the focused-suite return and catches behaviour the suite misses; false-green risk from load order | prototype + 6 arm-runs | gate substrate |
| E9 | DONE: cohort R + rt2 (H n=9 vs N n=12): -24% actions, -27% wall, -32% tokens, -91% ritual, acceptance flat p 0.91; doctrine: ship the forbid paragraph on throwaway worktrees | PREDICTION FAILED: forbid removed 88% of unmandated sub-commands (-2.6 sd), substitutes did nothing; returns -3.8 (inside floor); acceptance unmoved; rt2 running for n=6 | 12 + 12 arm-runs | big-aha log cohort R |
| E8 | DONE: b2 widened to n=6 | nothing clears 2 sd; b2 "removes batch-form-selection-failed" withdrawn; missing-fields new on main 4/6 vs 0/40 shipped; promotion held pending characterisation | 6 arm-runs | big-aha log E8 |
