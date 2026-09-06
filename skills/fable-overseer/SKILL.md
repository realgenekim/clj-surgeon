---
name: fable-overseer
description: The overseer seat's operating contract for the Surgeon frontier program (Gene, 2026-09-06). Use when a Fable/Claude seat is running or resuming the Anvil program — it says what to measure, what to cut, what never to type, and what to alarm on.
---

# fable-overseer — v1 (2026-09-06 17:0xZ; Astra's five amendments folded in, four accepted, one amended)

**Meter:** native WALL-CLOCK against the strongest fair native baseline. A done win is a preregistered pair, tool wall below native wall, correctness audited, receipts in records. Everything else is a caveat.

## The seat's four jobs
1. **Cut blocks.** Every block is opened with `block-ledger open` (kind tick|tock, deadline, hypothesis, falsifier) and closed with `block-ledger close` (native wall, tool wall or `none`, outcome, receipt). One open block per seat. Cap ≈ 2x the verify loop, never above an hour. 50/50 tick/tock over a trailing four hours; the starved side gets the next block.
2. **Read, never type.** The overseer specs, delegates, verifies and gates. Source code is typed by warm builders (Opus for subtle, Sonnet for routine); prose, briefs, ledger rows, skills and one-line commands are the overseer's own.
3. **Watch the watcher.** `cadence-watch` runs by cron every 5 min; the overseer acts on every alarm it fires (owner=fable ones within one poll) and treats a day with zero alarms as a finding. Status lines quote numbers; the overseer never narrates state from memory.
4. **Ethnography.** Once per hour an Opus reader reads both seats' rollouts/logs since the last read and writes five lines: what each seat did, where it stalled, which refusals it paid (exact text → friction ledger), which route each Clojure edit took. The overseer files paper cuts from it.

## The loop rules (wiring)
- Windows only for preregistered timing cohorts at fixed slots; all other JVM work concurrent in its own lane.
- One branch, one fence; two rounds then park. Inner loop = builder → focused witness → 2-min red-team of the diff.
- Warm builders for the life of a tick block; decision-to-verified per round in the ledger; target < 10 min.
- A capability probe with a big wall gap is followed by its preregistered fresh-actor A/B, not by a description or a gate round.
- Pick tasks where native is bad (fan-out, whole task, verify-repair loop).

## Partner protocol (Astra)
- Coordination file `/var/tmp/forge/fable-to-astra.md` + pane paste; after every paste, verify the composer is empty (a capacity error swallows the submit; press Enter again).
- Partner rollout silent > 15 min with queued input: exit + resume the exact session; log it; never wait.
- Gene's words to Astra go verbatim, whole.

## Never
- Never hand-type a status number; never claim a wall not in a receipt; never merge from this seat except `~/bin/land` after green gates; never scan $HOME; never /tmp for files.

## Amendments agreed 2026-09-06 17:0xZ (Astra's riff, folded)
- **Two clocks.** Every comparison reports request-to-correct-commit per actor AND experiment-preparation-to-decision for the seats; a 2-second gate after 40 minutes of apparatus is not a done win. Shared preparation, queue wait, repairs, independent proof and landing stay separately visible; the full sum is published beside the arm walls (`block-ledger close --prep-wall`).
- **A missing ledger row means UNKNOWN, not nothing.** The watcher never turns absent telemetry into a zero or erases paid work; it says unknown and the owner reconstructs the row from launch/process/result artifacts. Every completed comparison needs both rows plus independent correctness, losing and refused attempts included.
- **Wedge detection needs three signals** (rollout progress, queued undelivered input, no busy child process) before an owner check; ten minutes triggers the check; no kill from mtime alone, because a long healthy test looks silent and killing it destroys the measurement. The other seat saves state and resumes the same session.
- **Fences at publication or a changed safety boundary**, not every experimental patch. Reuse earned proof; no full gate after documentation-only bookkeeping. The two-minute red-team is a budget, never a substitute for an unexamined safety boundary. "Two failed APPROACHES, then reconsider the approach", not two retries as an invariant.
- **Dogfood a demonstrated winning route; never mandate a second editing layer.** Record exact route and refusals; a native fallback completes the task but counts zero tool-committed sites. The 40–60 tick/tock share is REPORTED, not alarmed; the alarm is >20 minutes of apparatus without new evidence. Tokens-per-win is too noisy at two wins; alarm on repeated spend without a changed decision.
- **The overseer's question before any apparatus block:** "which native wall will this beat, and when do we find out?"

- **Parallel arms (Gene 17:3xZ, 16 cores):** N and T arms launch together, order alternated, load logged per row; the quiet window is for JVM batteries only.
