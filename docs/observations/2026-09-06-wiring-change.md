# The wiring change — batch size is wiring (Gene, 2026-09-06 16:53Z: "Let's deal with org wiring changes first (kim spear). It dominates outcomes. Batch size is wiring.")

Status: DRAFT rules in force from 16:53Z on the Anvil seat; Astra's counter folds in; becomes the two role skills (skills/fable-overseer, skills/astra-frontier) on agreement.

## What the night looked like, in Kim & Spear terms
- We learned in the PERFORMANCE zone: every lesson came from a live landing, a live cohort, a live fence. Slowification puts learning in the PLANNING zone: stub-actor dry runs, warm-REPL witnesses, a red-team of the diff before any fence. We had those tools and used them last.
- We SERIALISED sixteen cores behind one file: load sat 1–3 all night while both seats queued on /var/tmp/forge/quiet-window.md. The window exists for timing purity; it should exist only for timing cohorts.
- Feedback batches were HUGE: one Sol fence per round, five to seven rounds on one branch = five to seven decisions between checks; 20–40 min per round; N ≈ 8 decisions in 9 h.
- Signals had NO AMPLIFIER: the partner session went silent at 15:05Z (model at capacity → goal stalled → queued inputs) and nothing fired for 100 min.

## The four rules (each with its tripwire)
1. **Windows are for timing cohorts only, at fixed slots.** Every other JVM job runs concurrently in its own lane (own worktree, own port, own /var/tmp/forge/<lane>-fx). Tripwire: cadence-watch alarms on a window whose purpose is not a preregistered cohort or whose until= has passed.
2. **One branch, one fence; two rounds, then park.** The inner loop is builder → focused witness → 2-minute Opus red-team of the diff; the Sol fence runs once, when the builder says ready-to-land. Tripwire: the ledger row for a tick block carries rounds; a third round is refused by the overseer.
3. **Warm builders.** A persistent builder with the repo and JVM warm, fed specs over a file, for the life of a tick block; no fresh agent per round. Meter: decision-to-verified per round, target < 10 min, ≥ 4 decisions/h, recorded in the ledger.
4. **Every block is a ledger row, and the watcher reads the ledger.** `block-ledger open/close` (kind tick|tock, deadline, hypothesis, falsifier, native wall, tool wall, receipt, tokens). `cadence-watch` every 5 min: one quoted status line; alarms BLOCK_OVER_CAP, NO_ROW_30M, RATIO_4H, PARTNER_SILENT, FABLE_SILENT, WINDOW_EXPIRED — each with owner + action. Zero alarms in a day is itself a finding.

## Dogfood as a route rule
Every Clojure edit inside the program takes the winning Surgeon route when one exists (typist route for known-site multi-owner edits; the batched within/from/to route for fan-out; the admit gate with a proof profile for a caller's own patch). The ledger row records route + wall; every refusal becomes a friction-ledger inbox item with its exact text. "Dogfooding" is then a count the watcher can print, not an adjective.

## Who oversees
Fable, with deterministic watchers measuring and Opus reading rollouts for ethnography; Astra is the mirror seat on frontier, not a subordinate. No model computes a status line by hand.

## First evidence the wiring matters (this window)
Cohort I (informed batched route vs native), first pair: N1 103.2 s correct, I1 69.2 s correct — the same task class that lost 2.8x eleven hours earlier when the route was wrong. The difference between the two cohorts is wiring (which route the caller is put on), not capability.
