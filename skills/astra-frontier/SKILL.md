---
name: astra-frontier
description: The frontier seat's operating contract for the Surgeon program (Gene, 2026-09-06), mirror of fable-overseer. Use when the Codex (Astra) seat is running frontier experiments — what to preregister, what to dogfood, how to report, how to stay visible.
---

# astra-frontier — v1 (2026-09-06 17:0xZ; Astra's 16:54Z riff folded in)

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

## Amendments agreed 2026-09-06 17:0xZ (Astra's riff, folded)
- **Two clocks.** Every comparison reports request-to-correct-commit per actor AND experiment-preparation-to-decision for the seats; a 2-second gate after 40 minutes of apparatus is not a done win. Shared preparation, queue wait, repairs, independent proof and landing stay separately visible; the full sum is published beside the arm walls (`block-ledger close --prep-wall`).
- **A missing ledger row means UNKNOWN, not nothing.** The watcher never turns absent telemetry into a zero or erases paid work; it says unknown and the owner reconstructs the row from launch/process/result artifacts. Every completed comparison needs both rows plus independent correctness, losing and refused attempts included.
- **Wedge detection needs three signals** (rollout progress, queued undelivered input, no busy child process) before an owner check; ten minutes triggers the check; no kill from mtime alone, because a long healthy test looks silent and killing it destroys the measurement. The other seat saves state and resumes the same session.
- **Fences at publication or a changed safety boundary**, not every experimental patch. Reuse earned proof; no full gate after documentation-only bookkeeping. The two-minute red-team is a budget, never a substitute for an unexamined safety boundary. "Two failed APPROACHES, then reconsider the approach", not two retries as an invariant.
- **Dogfood a demonstrated winning route; never mandate a second editing layer.** Record exact route and refusals; a native fallback completes the task but counts zero tool-committed sites. The 40–60 tick/tock share is REPORTED, not alarmed; the alarm is >20 minutes of apparatus without new evidence. Tokens-per-win is too noisy at two wins; alarm on repeated spend without a changed decision.
- **The overseer's question before any apparatus block:** "which native wall will this beat, and when do we find out?"
