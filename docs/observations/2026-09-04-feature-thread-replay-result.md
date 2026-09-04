# Replay result — Dequote/Format on social-media-writer @ 2df99c98 (2026-09-04 07:30–07:5xZ), counted from the codex rollout files

Two arms, same model (gpt-5.6-sol), same request text (Gene's three messages verbatim), same clone, same gate (`make runtests-unit` + `make test-js`), run concurrently on Anvil (load 9–16).
- N = native: no feature_thread. T1 = the same, with ONE feature_thread receipt (24,420 B: six legs with bodies/ranges/sha/anchors, verify row, governance anchors, next_call) placed in the prompt as "your first tool call's output".

| meter (from the rollout, hand-classified) | N native | T1 receipt | ratio |
|---|---|---|---|
| RAW tool calls, everything the harness recorded | 32 | 24 (+1 receipt = 25) | 1.3× |
| calls before the first patch | 13 | 6 (+1) | 2.2× |
| SOURCE READS before the first patch (net of CLAUDE.md/bd) | 10 (≈20 sed ranges, 5 rg) | 2 (rg selectionStart; sed 40-90p, both batched inside bd calls) | 5× |
| substantive calls, whole task (reads + patches + test runs; excl. CLAUDE.md, bd, .beads patches, stdin waits, git-status checks) | 21 | 9 (+1) | 2.1–2.3× |
| patches (real files) | 3 | 2 | — |
| suite runs | 4 | 4 | equal |
| repo ceremony (CLAUDE.md, bd prime/create/claim/close, .beads patches, status checks) | 8 | 12 | equal-ish; T1 spent MORE on beads tidy-up at the end |
| files changed | 9 (+1 untracked JS test) | 7 | both land on the six real sites |
| gates (agent's claim) | unit 227/712/0, once 934/3694/0, js exit 0 | same figures | INDEPENDENTLY RE-RUN by the seat 07:5xZ: N test-js exit 0 + runtests-unit exit 0; T1 test-js exit 0 + runtests-unit exit 0 — both arms GREEN |

Pre-registered predictions and the honest scoring: N ≈ 15–20 → MISSED (32 raw / 21 net); T1 ≈ 3–5 → MISSED on raw (24) and on net (9), HIT on calls-before-write (6 raw / 2 net + the receipt). Withdrawal line as written ("if T1 needs ≥ 8 calls the receipt is not an edit basis") — by the letter T1 crosses it; by the mechanism it does not: the receipt removed 8 of 10 discovery reads, and every remaining T1 call is a test run, a patch, or the repo's own ceremony, none of which a read-side receipt can remove.
What T1 still read, and why: the selection precedent (openTransformFromSelection / selectionStart) and the test-suite classification (`:fast`) — both named as round-three gaps by the naive readers before this run. T1 wrote its handler AFTER handle-format (L682) rather than extending mechanical-format (L133 in Gene's real edit) — a design choice, both green.
What the 10× would need: (a) the round-three receipt (peer commands + request contract) to take the last two reads to zero; (b) a harness where the write is the admit gate (one call: verify + commit) instead of patch → 3–4 suite runs; (c) a repo whose CLAUDE.md does not mandate eight calls of ceremony per task — or counting them as the fixed cost they are. On THIS harness the honest sentence is: discovery 5× (10 reads → 2), whole task 2.1–2.3× net, 1.3× raw.

## Fleet poll — Opus (07:4xZ), the seat's concessions, and the next arms

Opus, adversarial, four bucket moves and one ceiling. CONCEDED: (A) classify a call by what it READS, not the command it is stapled to — T1's two batched sed/rg reads are discovery, so discovery is 10 → 3–4 calls, **2.5–3.3×, not 5×**; (B) one unit per row (calls), the "≈20 sed ranges" parenthetical goes; (C) ceremony is not "equal-ish" — T1 spent 12 vs 8, four calls refunded to beads tidy-up; (D) T1 is an ORACLE arm (the receipt was injected, not called) — the tool arm is T2. THE CEILING: reads are 10 of 32 raw = 31%, so a perfect read-side receipt caps at 1.45× raw (Amdahl); 10× on this harness = 3.2 calls, below the floor of 2 patches + 1 suite + 8–12 ceremony; the only path to 10× is the admit-gate write (one call) plus not charging the ceremony — and then the honest split is "receipt ~1.4×, harness ~7×". CORRECTION TO OPUS: the gates were re-run by the seat before its answer landed — both arms verified green. NEXT ARMS (Opus's K=6, seat's order): X stale receipt (ranges +40, shas altered) and P placebo (an unrelated feature's receipt) LAUNCHED 07:46Z; R0 replicates ×3 each; T2 MCP-attached; T3 round-three receipt; G admit-gate write; C ceremony-free repo. Withdrawal lines as Opus wrote them: X or P ≈ T1 → the effect is priming, the receipt line is confounded; T2 ≥ N → the receipt helps only when injected (a prompt, not a server); G suite runs not < 3 → publish the ~2.9× ceiling and stop chasing 10×.

## Fleet poll — Sol (07:47Z), and the seat's verdict on its own pre-registration

Sol, independently (full text in 2026-09-04-feature-thread-replay-poll-sol.md): discovery 5× survives as an OPERATION count; task-core 2.1–2.3×; raw 1.28×; "calls before first patch" is 1.86× once the receipt is charged. The written ≥8 withdrawal line HAS FIRED: the pre-registered "edit basis" claim is WITHDRAWN for this experiment; the narrower claim — a strong discovery accelerator — remains plausible. 10× cannot occur through a read-side receipt (floor ≈ 14 calls: ceremony + suites + receipt + write → at most 2.3× on 32); the mechanism is transaction granularity — receipt with a complete edit contract → ONE atomic apply/verify/commit call — and the native arm must receive the same gate or the comparison is confounded. Sol's K: R (five paired replicas), T2 (MCP first call), T3 (round-three receipt), G (T3 + atomic admit), C0 (no-ceremony fixture × N/T3 × ordinary/G) — with the end-to-end 10× withdrawn if the execution-matched paired median is < 8×.

**Seat's verdict, both polls in hand:** the two readers disagree only on whether the batched reads count (Opus: yes → discovery 2.5–3.3×; Sol: operations → 5×). Everything else converges, and the seat adopts it: (1) "edit basis" is withdrawn for this run; the receipt is a discovery accelerator, 1.3× raw, ~2.2× task-core; (2) the 10× is a HARNESS claim — one atomic write-and-verify call and no per-task ceremony — and the receipt contributes ~1.4× of it; (3) the next arms are X/P (running), replicates, T2, T3, G. Gene's question "can this reach 10×" has a measured answer tonight: not the receipt alone, arithmetically; possibly the receipt + the admit gate as one call, to be measured with the native arm given the same gate.

A second finding from building the placebo: on the real repo, formatDraft is the ONLY subject that returns COMPLETE; expound, openTransformFromSelection, handleApply, refineTransform, saveDraft all come back INCOMPLETE (2–3 of 5, routes/handlers ABSENT). The verb's recall on the repo's OTHER features is a tweezer item for round three (route naming conventions; the conventions file was tuned on one feature).

## Negative controls X and P — finished 08:0xZ (rollout counts; gates being re-run independently)

| arm | raw | source reads before first patch | first patch at call | patches (real) | suite runs | sites | agent-claimed gates |
|---|---|---|---|---|---|---|---|
| N native | 32 | 10 | 14 | 3 | 4 | six real sites | green (verified by the seat) |
| T1 receipt (correct) | 24 | 2 | 7 | 2 | 4 | six real sites | green (verified) |
| **X stale receipt** (every range +40, every sha altered, bodies intact) | 25 | 1 (a `sha256sum` check batched into `bd claim` — it verified the shas, found them wrong) + 2 later (test runner) | 5 | 2 | 7 (unit, js×2, integration, component, all) | six real sites, same as T1 | green — VERIFIED by the seat (JS_EXIT=0 CLJ_EXIT=0 JS_EXIT=0 CLJ_EXIT=0) |
| **P placebo** (a real receipt for an unrelated feature, 6 KB, INCOMPLETE 2 of 5) | 31 | 10 | 13 | 2 | 3 | six real sites | green — VERIFIED by the seat |

Reading, against the pre-registered withdrawal lines (Opus's): **P ≈ N** (31 vs 32; 10 reads each) — the effect is CONTENT, not priming; the confound is ruled out. **X ≈ T1** (25 vs 24) with the patch on the correct sites — Opus's line said "if X lands at ~24 like T1 the receipt is not an edit basis"; the mechanism says something sharper: X kept the BODIES and mangled only ranges and shas, and `apply_patch` anchors on context lines, not line numbers, so on THIS harness the bodies carry the whole discovery value and ranges/shas carry none. The agent even checked the shas (call 4), saw they did not match, and wrote from the bodies anyway. That is the finding: for a patch harness the receipt is a BODY delivery; ranges/shas become load-bearing only when the write is the admit gate, which binds on them. The seat's own receipt-line "a mismatch is a REFUSAL, never a retry" was ignored by the agent, as the round-one reviewer predicted a printed instruction would be.

Independent gate re-run for X and P (seat, 08:1xZ): JS_EXIT=0 CLJ_EXIT=0 JS_EXIT=0 CLJ_EXIT=0 

## Replicates (launched 08:04Z) — T1b finished 08:1xZ
| arm | raw | source reads before first patch | first patch at call | sites | gates |
|---|---|---|---|---|---|
| T1b (receipt, replicate) | 22 | 2 (registry ids; intent_contract_test + test_runner — the governance/test-classification reads) | 9 | six real sites (routes @2148, transform @682, js @456, tests @96/@386, registry @557) | green — VERIFIED by the seat (JS_EXIT=0 CLJ_EXIT=0) |
T1b reproduces T1 within two calls (24 → 22; reads 2 → 2; first patch 7 → 9). N2 still running (33 raw / 16 reads at 08:1xZ, no patch yet — already above N's 32).

## Replicate N2 — finished 08:2xZ
| arm | raw | source reads before first patch | first patch at call | patches (real) | suite runs | sites | gates |
|---|---|---|---|---|---|---|---|
| N2 (native, replicate) | 50 | 13 (editor-commands.js 1-480; rg formatDraft/Edit; AGENTS.md; editor-specs; editor-controller; state.clj; editor_journal; Makefile; test tree; transform_test; @spec greps) | 19 | 5 | ≥4 | the six real sites (+ Makefile, editor-design/specs docs; the clj test went into transform_test.clj; JS test added as an untracked new file) | green — VERIFIED by the seat (JS_EXIT=0 CLJ_EXIT=0) |

**Paired figures at n=2 per arm (raw / reads-before-first-patch):** native 32, 50 (mean 41 / reads 10, 13); receipt 24, 22 (mean 23 / reads 2, 2). Raw ratio 1.8× at n=2 (was 1.3× at n=1); the native arm's spread (32–50) is wider than the receipt's (22–24). Discovery reads 11.5 → 2, 5.75×. Withdrawal check (Opus's R0 line: overlapping intervals at n=3 → withdraw the raw claim): intervals do not overlap at n=2; a third pair decides.

## Third replicate pair — N3 and T1c finished 08:3xZ (gates re-running)
| arm | raw | source reads before first patch | first patch at call | sites | note |
|---|---|---|---|---|---|
| N3 (native) | 33 | 12 | 18 | the six code sites + Makefile + editor-specs + tests.edn; tests as two NEW untracked files (test/js/dequote_format_command_test.js, test/writer/dequote_format_test.clj) | verified green |
| T1c (receipt) | 22 | **0** | 4 | six real sites incl. both tests | two later reads: test classification (Makefile 225-296, tests.edn); verified green |

**Paired table at n=3 per arm (raw / source reads before the first patch):** native 32, 50, 33 → mean 38.3, reads 10, 13, 12 (mean 11.7); receipt 24, 22, 22 → mean 22.7, reads 2, 2, 0 (mean 1.3). Intervals do NOT overlap (native 32–50 vs receipt 22–24) — Opus's R0 withdrawal line is not met; the raw claim stands at n=3: **1.7× raw, 8.8× discovery reads**. Task-core figures to be recomputed after hand classification of all six.

Independent gate re-run for N3 and T1c (seat, 08:4xZ): === N3 test-js JS_EXIT=0 === N3 runtests-unit CLJ_EXIT=0 === T1c test-js JS_EXIT=0 === T1c runtests-unit CLJ_EXIT=0 

## One rule, applied by code to all eight arms (`~/bin/rollout-taskcore`; a call is classified by WHAT IT READS, batched or not; ceremony = CLAUDE.md/AGENTS.md, bd/beads, git status/diff checks, tool-list probes, stdin waits, .beads patches)
| arm | raw | task-core | reads (whole task) | patches | suite runs | ceremony |
|---|---|---|---|---|---|---|
| N | 32 | 23 | 12 | 3 | 8 | 9 |
| N2 | 50 | 29 | 15 | 6 | 8 | 21 |
| N3 | 33 | 19 | 12 | 3 | 4 | 14 |
| T1 (+1 receipt) | 24 | 10 | 3 | 2 | 5 | 14 |
| T1b (+1) | 22 | 8 | 3 | 3 | 2 | 14 |
| T1c (+1) | 22 | 12 | 4 | 3 | 5 | 10 |
| X stale | 25 | 14 | 5 | 2 | 7 | 11 |
| P placebo | 31 | 19 | 12 | 2 | 5 | 12 |
Means, n=3: native raw 38.3 / task-core 23.7 / reads 13.0; receipt raw 22.7 / task-core 10.0 (+1 receipt = 11.0) / reads 3.3. Ratios: **raw 1.7×, task-core 2.2×, reads 3.9×** (reads over the whole task; before-first-patch reads were 11.7 vs 1.3, 8.8×). Ceremony is ~35% of native raw and ~55% of receipt raw — the receipt arms' ceremony did not shrink, so the raw ratio is capped by it exactly as the Amdahl argument said.

## T2 (MCP-attached, the TOOL arm) and T1C (receipt, ceremony-free) — finished 08:5xZ (gates re-running); NC (native, ceremony-free) still running
| arm | raw | task-core | reads (whole task) | patches | suite runs | ceremony | first call | sites |
|---|---|---|---|---|---|---|---|---|
| T2 — agent calls feature_thread itself via MCP (server :8165 from 9139b2c5, conventions installed) | 31 | 18 | 6 | 10 | 2 | 13 | mcp__clj_surgeon__feature_thread (as mandated) | seven files, the six real sites |
| T1C — receipt injected, CLAUDE.md 3 lines, no beads | 27 | 17 | 6 | 4 | 5 | 10 (tool-list probe, git checks, stdin waits) | — | six real sites |
| NC — native, ceremony-free (running at 08:5xZ) | 22+ | 21 | 15 | 5 | 1 | **1** | — | — |

Two things T2 shows. (1) The tool arm's raw count (31) is NATIVE-like, but its reads are 6 vs native 12–15: the receipt through MCP still halves discovery; the count is inflated by TEN apply_patch calls (vs 2–3 in the injected arms) — the agent patched piecemeal; whether the MCP text face invites that (whitespace/newline differences between faces) is now a round-four item. (2) Its two post-receipt source reads were the lines immediately AFTER each anchor (transform.clj 668-712, editor-commands.js 438-492): apply_patch needs post-context to insert "after:L680" — the receipt should carry `after_context` per anchor (round-four addendum sent). T1C read the peers (editor-commands.js 230-388, the selection commands) and the test classification — the same two gaps as every receipt arm; ceremony did NOT vanish for it (probe + git checks + waits), so raw fell only 24 → 27?? — no: T1C is 27 vs T1's 24, i.e. no improvement; the ceremony-free change removed beads (~5 calls) and the agent spent them on reads instead. NC's ceremony did collapse (1) — its raw will decide whether ceremony was the diluter for native.
Independent gate re-run for T2 and T1C (seat, 08:43Z): === T2 test-js JS_EXIT=0 === T2 runtests-unit CLJ_EXIT=0 === T1C test-js JS_EXIT=0 === T1C runtests-unit CLJ_EXIT=0 

## Ceremony-free pair (C) — NC finished 08:5xZ (gates re-running)
| arm | raw | task-core | reads | patches | suite runs | ceremony | first patch at |
|---|---|---|---|---|---|---|---|
| NC (native, no beads mandate) | 38 | 28 | 16 | 6 | 6 | 10 (tool probe, git checks, waits) | 16 |
| T1C (receipt, no beads mandate) | 27 | 17 | 6 | 4 | 5 | 10 | 5 |
**Pre-registered line (Opus): "if the ratio moves < 0.3, ceremony was never the diluter."** NC 38 vs the native mean 38.3; T1C 27 vs the receipt mean 22.7; C-pair ratio 1.4× vs 1.7× at n=3 — the ratio moved the WRONG way by 0.3. **WITHDRAWN: "count the ceremony as a fixed cost and the ratio grows."** Removing the beads mandate did not lower native's total (the agent spent the freed calls on reads and git checks) and raised the receipt arm's (more reads). Ceremony was a fixed cost in both arms, not a diluter of the ratio; the harness floor is the write path (patches + suite runs), not the ceremony. What remains for a large ratio is exactly one thing: the write as one call (G).
Independent gate re-run for NC (seat, 08:54Z): JS_EXIT=0 CLJ_EXIT=0 

## T3 — the round-four receipt (after_context, request_contract, absent, verify; peers ELIDED by the structured cap) — finished 12:1xZ (gates re-running)
| arm | raw | task-core | source reads BEFORE the first patch | first patch at call | reads later | patches (real) | suite runs | sites |
|---|---|---|---|---|---|---|---|---|
| T3 | **19** | 13 | **0** (calls 1–4: two tool-list probes, CLAUDE.md + bd prime, bd claim + a sha256sum check of the receipt) | 5 | 1 (Makefile 220-310 + rg for the test-namespace → alias mapping) | 4 | 5 | the six real sites + registry (same as T1) |
Against the receipt arms at n=3 (24/22/22 raw; reads-before-patch 2/2/0): **19 raw, 0 reads before the patch** — the after_context + request_contract closed the last pre-patch read; what remains is ONE read for the test classification (the verify row names the targets by alias evidence; the agent still looked up which alias runs the new test namespace) and the suite runs. Against native (32/50/33): 2.0× raw at n=1, discovery reads 11.7 → 0. The pre-registered T3 line (Sol: "withdraw 'complete edit basis' if any successful run needs an unplanned source read before writing") — MET: zero source reads before the write. The receipt IS an edit basis for the write; the remaining calls are verification, not discovery.
Independent gate re-run for T3 (seat, 12:13Z): JS_EXIT=0 CLJ_EXIT=0 

## T3b (replicate of the round-four receipt) — finished 12:2xZ (gates re-running)
| arm | raw | task-core (code rule) | reads (whole task) | patches | suite runs | ceremony |
|---|---|---|---|---|---|---|
| T3b | 18 | 7 | 2 | 2 | 3 | 11 |
T3 pair: 19, 18 raw (mean 18.5) vs the injected round-two receipt 24/22/22 (22.7) vs native 32/50/33 (38.3): **2.1× raw at n=2 for T3**, and the per-call listing below decides whether the write again came before any source read.
