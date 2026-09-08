# Gene report — the night of 2026-09-07/08 (written 2026-09-08T10:12Z, for 5am PT)

Gene's orders (verbatim): "Take each move, highest vs native first, and make it perfect… loop until all the winning rows are perfect." · "Goal: 4-8x tempo. I'm going to sleep. You have until 5am PT tomorrow. Show what you can do!!!!" · "Tell Astra: if you go dark or drop down opus, astra is to take over as best it can."

## 1. vs NATIVE — the table first

| row | move | native | tool | vs native | n | correct | phase (tree) |
|---|---|---|---|---|---|---|---|
| 1 | namespace_split, whole partition (views.clj, 141 forms → 20 ns) | 408 / 486 s (with manifest); 447 s median | **56 s CLI / 46–49 s MCP** on the landed build, two frozen reruns hash-identical | **÷8–9.7** | 2+2 verb, 2 native | 4/4 every run, 0 paper cuts | **friction-low** (pilot 7: 4/4 on one frozen package; plate installed; LID debt 0) |
| 2 | alias_migration, natural MVR migration with collisions (21 sites, 9 files) | caller work median 121 s (six matched N arms); floor 198 s median n=6 | **caller work median 27 s** (six D arms on the fixed build) | **3.98× less caller work** (median D/N 0.25; 96.7 s saved > 2·s_N); all-oracle ratio 0.40 reported beside | 6 N + 6 D pairs | 6/6 clean, 0 refusals | friction-high (won tonight) |
| 5 | move-forms-with-deps (partial retention, exports.clj 25/112 owners) | caller work median **316.9 s** (six fresh native controls, 6/6 accepted, SD 52.9) | caller work **98 / 127 / 144 / 112 / 140 / 154 s** (proof by the runner for both arms; D1 proof-inclusive 341 s labelled) — median D/N **0.43** | **÷2.3 on caller work**; 182 s saved > 2·s_N = 106 s; gate PASS on every clause | 6 N + 6 D | **6/6 accepted** by the frozen Cell B oracle; 0 refusals; all six D patches byte-identical | **friction-high** (F arms pending → emission not yet credited) |
| 3 | require_change across 8 namespaces (natural MVR migration) | — | verb built (3d55fa34) | **uncalibrated**: Sol's frozen task is not admissible under its own byte/layout rules | — | — | uncalibrated (honest) |
| cold start | fresh agent one-shots the skill's loop | pilot 1: 1/4 | pilots 5/6 on the amended block: **3/4 twice**, each miss a different one-sentence skill gap, each fix verified N=1 on the failing cell | — | 4 cells × 4 pilots + 2 singles | 4/4 correct patches every pilot | friction-high |

Plainly: rows 1, 2 and 5 are measured wins against native on the caller-work clock; row 3 has no number.

## 2. Top wins AND losses

Wins
- **Row 2 converted from a synthetic 1.38× to a natural-collision 3.98×** on a real migration from Marvin Voice Remote history, six pairs, zero refusals, after two design amendments that the native floor forced (an unsatisfiable absolute-seconds clause; an alias-timing ambiguity; a golden confound) and one verb paper cut (receipts written inside the workspace → now /var/tmp/forge/<verb>-receipts for every verb, with a workspace-clean witness).
- **Row 1 held ÷8–9.7 on the landed build across two frozen reruns**, and the routing plate is landed AND installed here (fan-out route suspended, split admitted for its exact contract; verifier derives 33 required passages from the intent registry; block hash 67e45724…).
- **The apparatus was qualified by adversaries, four rounds**: certified rounds lifted; cold-start cohorts lifted; 30+ real defects found by executed counterexamples (a child that prints PASS and exits 17 scored green; the graded agent could define the grader's gate exemption; strace kept every cold cell alive to the ceiling; a native-image tool's /tmp writes came from threads the parser dropped…) — every one now a corpus row (coldstart 53, round 20 + 8).
- **Row 5's compiler is feasible and landed** (partial retention + facts projection, 1.3 engineering hours), so its cohort measures what the fleet gets.
- **The skill is real**: `clojure-fast-feedback` (claude-skills anvil/nrepl-test-alias 61644f5), COLD START block inlined as bytes into AGENTS.md/CLAUDE.md (a pointer was never delivery: Codex cannot load a Claude skill), launchers bundled, every "sublime" learning back to your 5K at mile one indexed.

Losses
- **Row 1 is still one cell short of friction-low, three pilots running.** Each time 3/4; each miss a real, different skill-text gap (temp location; a promised receipt; "loop" not saying the first turn is a run; the JVM boot treated as spare time). Each fix is one sentence and verified once on the failing cell; a clean 4/4 on ONE frozen package has not happened yet (~25 min, queued for Astra's morning or the next session).
- **Tempo did not reach 4–8× in the first half of the night.** From 01:20Z to 06:40Z one row changed phase; the loop was serial (builder → adversarial acceptance → builder) with me relaying reports. After Astra's cadence riff (frozen suites by claim, ≤15-min reviews, two lanes) the second half ran five lanes in parallel and landed a win, a feasibility, a plate, and five acceptance rounds.
- **Row 3 has no number** and its verb, though built, cannot be measured on the frozen task as written.
- **Continuation economics (codex resume) stays unmeasured** — Astra's refusal stands; the mechanism works (OTTER), the saving is not a number.
- **I deleted eight Cell B experiment trees with uncommitted outputs while reclaiming disk** (records kept every receipt; the raw trees cannot be regraded) — my error, disclosed, ratchet named.

## 3. Learnings → ratchets
- **The native floor is part of the design.** Twice tonight six controls changed the gate before any tool ran (row 2 clause 3 unsatisfiable; row 5's apparatus cancels). Ratchet: no D arm before six N controls and a Sol/Astra re-ruling on the gate (rows 2, 5 followed it).
- **The meter is the caller's own work.** Primary endpoint orient-start → candidate-complete; the apparatus that cancels between arms is reported beside, never gated. Now the tree's meter for every row.
- **A pointer is not delivery** → inline bytes; **a pid comes from a receipt** → run-bg/codex-fenced; **a probe is not proof** → receipts carry verification_complete/proof_pending; **the graded party may not define the grader** → gate frozen before the agent runs; **acceptance by adversary is unbounded** → frozen suites by claim, ≤15-min reviews; **the second hand-written brief is the stop signal** → ~/bin/round.
- **Every verb keeps its receipts outside the workspace** (witnessed, all verbs).
- **Model-specific cost-pricing of instructions**: two Opus cells priced "wait for the JVM" as latency to overlap; Sol blocked. Text that leaves a required choice open delegates it to whatever the model guesses.

## Amendment 2026-09-08T11:16Z
- Row 1 reached friction-low (pilot 7 clean 4/4 on block 08c79a61…). Row 5's six D arms ran: median caller work 140 s vs native 317 s (÷2.3) with the proof moved to the runner for both arms after D1 exposed the in-call-oracle asymmetry (341 s); F arms not run (time). Row 2's fix landed or landing (battery receipt 5495aa93; Sol GO r11). Ratchet from tonight's meter work: candidate-complete means the same thing for a verb that proves in-call and a caller that proves after — the runner proves, for both.

## 4. What's next (in order)
1. Row 1 → friction-low: one clean four-cell run on block 08c79a61… (p30 opus/Ø result pending at write time); then the routed-class telemetry check.
2. Row 5: the six N/F/D blocks on the frozen B07 apparatus (native floor is in; forecast D ≪ N on caller work).
3. Land 827a751a (Sol r10 in flight) → row 2 friction-medium; then its route telemetry + a cold-start pilot for low.
4. Row 3: Sol amends the fixture to what the verb can lawfully do; then six controls.
5. Mayor: merge list inb-708b29 (+ tonight's commits on the same branches); provisioning inb-94ba79; the skill needs your prune.

Records: captain's log (2026-09-03-captains-log-anvil-seat.md, entries from 2026-09-07T21:00Z), tree (2026-09-08-perfect-the-wins-tree.md), night heartbeat, row2/ and row5/ artifacts, cellC/ for every verdict.
