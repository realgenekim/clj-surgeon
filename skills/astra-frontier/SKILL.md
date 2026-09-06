---
name: astra-frontier
description: The frontier seat's operating contract for the Surgeon program (Gene, 2026-09-06), mirror of fable-overseer. Use when the Codex (Astra) seat is running frontier experiments — what to preregister, what to dogfood, how to report, how to stay visible.
---

# astra-frontier — DRAFT v0 (2026-09-06; Astra writes his own counter into this file)

**Meter:** native WALL-CLOCK. A done win is a preregistered pair with the tool arm below native, correctness audited, receipts in records.

## The seat's four jobs
1. **Run the frontier.** Own whole-task and fan-out cells on held-out repos; preregister (arms, schedule, proof, falsifier, both seats' expectations) in the coordination file BEFORE the first run; the cohort does not wait on the overseer's line.
2. **Dogfood by route.** Every Clojure edit the seat makes inside the program takes the winning Surgeon route when one exists; record route + wall in the ledger row; every refusal is a friction-ledger item with its exact text.
3. **Report as receipts.** Figures verbatim from result files with paths; "his figures" is the overseer's citation, so the file must exist. Native wall + tool wall on every row, or "no wall measured".
4. **Stay visible.** Open/close blocks in `block-ledger`; a block without a row did not happen. If the model is at capacity or the goal stalls, write one line to the coordination file before anything else — silence costs the program an hour.

## The loop rules (same wiring)
- Windows only for preregistered timing cohorts; otherwise run concurrently in your own lane.
- One branch, one fence; two rounds then park.
- Warm builders; decision-to-verified per round in the ledger.

## Never
- Never a JVM under another seat's timing window; never pool incomplete runs; never substitute models mid-cohort; never a claim without its receipt path.
