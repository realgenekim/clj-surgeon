# Perfect-the-wins tree (opened 2026-09-08T03:31Z)

Gene, verbatim (2026-09-08): "Take each move, highest vs native first, and make it perfect -- replicate fast clojure feedback loops, replicate amazing way you've hardened the 10x win. Write this into a todo file or tech tree or something, so you can use it to keep track: phases: rough, friction-[high/medium/low], perfect. loop until all the winning rows are perfect."

## Phase gates (a row moves up only when every gate is met, with a receipt)
- **rough** — one witnessed win vs a native control, n ≤ 2; no oracle.
- **friction-high** — paper-cut oracle exists for the output; friction ledger (inbox items with receipts + triggers) open items > 5; rounds run through `~/bin/round`.
- **friction-medium** — oracle 0 on the fixture; reviewer GO; landed on trunk; open items < 5; in-call wall decomposed (analysis / write / probe / proof).
- **friction-low** — routed class under "strictly better, or native" (witnessed contract, complete receipt path, kill switch); second consecutive rerun clears the killer threshold; cold-start pilot (4 cells) passes.
- **perfect** — two consecutive complete cold-start cohorts pass (Astra's criterion, zero forbidden events, zero operator intervention); apparatus share of the caller's actions < 50%; no hand step anywhere in fix → grade → review → land; doctrine + prompt plate installed and verified on every seat; a month of dogfood without a regression.

## The ladder every row climbs (the method that hardened the split)
1. Native control measured first (matched fixture, process stamps, four oracles) — the meter.
2. Whole-intent verb: the plan as data, one call, proof inside the call, undo receipt.
3. Paper-cut oracle over the output (seconds); reviewer probes become red witnesses in the repo.
4. Rounds via `~/bin/round`: builder in the warm image → grade → witnesses → one receipt; LLM review + landing once per batch.
5. Timing rerun on the clean tip (never wait on review); land via `~/bin/land` after battery receipt.
6. Cold-start pilot with the skill (fresh agents, W/Ø), counterexamples by owner, fix, repeat.

## The table (update in place; newest receipt wins)

| # | move | vs native (measured) | phase | evidence | next action (one) |
|---|---|---|---|---|---|
| 1 | namespace_split (whole partition, plan supplied) | 8.0x / 9.1x (wave 5, landed 2b39bd37) | **friction-low** (pending pilot 2) | 2026-09-07-plan-2-cellC-result.md; paper cuts 0; Sol GO r5 | cold-start pilot 2 → admit to routed classes → measure `:proof :warm` |
| 2 | alias_migration | 1.4x (42.9 vs 31.1 s, synthetic) | rough | routed class today; narrow scope | natural fixture + collision cases; oracle; native control n=4 |
| 3 | require_change across namespaces | zero churn measured; NO ratio | rough | 2026-09-02 doctrine | matched native control first |
| 4 | CLI :extract! (helper extraction) | 1.13x inside noise; MCP 0.77x | rough / losing | cellB-result | rebuild as an entrance beneath the split compiler; N/F/D |
| 5 | move-forms-with-deps (Gene's "killer") | unknown | not started | Astra: closest transfer | the preregistered N/F/D Cell B experiment (native / facts-only / verb) |
| 6 | rename-ns with callers | unknown | not started | — | fixture + controls after row 5 |
| 7 | whole-intent fan-out | unknown (old form 0.5x, pair-1) | not started; SUSPEND the routed class | pair-1-result | one bounded retest after full task coverage; doctrine edit for the suspend |
| 8 | the round routine | 208 s/round, 4 repairs on first use | friction-medium | round ledger | builder session persistence (codex resume); lock out of worktree; receipt schema (Astra r15) |
| 9 | cold start of the skill | 1/4 sessions executed the workflow | rough | coldstart/counterexamples.md | pilot 2 (running) → matrix |
| 10 | REPL-driven tool development | 11 s/iteration, no matched control | unknown | round-3 loop log | meter accepted-fix wall vs a non-image control once |

## Loop rule
Take the highest-ratio row that is not perfect; run the ladder; do not descend until it reaches friction-low; then the next row. Every state change here also goes to the captain's log and the resume note.

## Log
- 2026-09-08T03:31Z opened; row 1 at friction-low pending pilot 2; rows 2–4 rough; rows 5–7 not started; Sol's independent squares answer pending, cross-attack next.
