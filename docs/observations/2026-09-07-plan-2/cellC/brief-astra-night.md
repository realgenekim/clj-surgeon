# NIGHT BRIEF for Astra — takeover and/or builder of record until 12:00Z (5am PT), 2026-09-08

Gene, verbatim (~06:50Z): "Riff with Astra on recommendations. Goal: 4-8x tempo. I'm going to sleep. You have until 5am PT tomorrow. Show what you can do!!!! Tell Astra: if you go dark or drop down opus, astra is to take over as best it can. Godspeed. Achieve peak sublime! Be the sociotechnical maestro making the wiring and management system sing!!!"

You (Astra) are the builder of record for the night. If this brief reached you through ~/bin/astra-watch, the Fable seat has gone dark (pulse stale > 45 min) and you are ALSO the operator: proceed as best you can under the rules below; do not wait for Fable.

## The program
- The tree: /home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-perfect-the-wins-tree.md (phases rough → friction-high/medium/low → perfect; descend at friction-low, never medium, never wait for perfect). Your plan: /var/tmp/forge/plan2/cellC/astra-plan-tree.md. Your cadence riff (if present): /var/tmp/forge/plan2/cellC/astra-cadence.md — its frozen battery and two-lane design are ADOPTED.
- State: trunk MCP/main carries namespace_split rounds 1–5, LID registration, the routing plate (fan-out suspended, split admitted exact-contract; installed on this seat, hash 67e45724…). Row 1 = friction-medium; blockers = apparatus qualification (your B02 refusals; B02d delivered; an Opus acceptor ran your third acceptance because your provider's filter killed the hidden-JVM/socket adversaries — report /var/tmp/forge/plan2/cellC/opus-B02-acceptance-3.md when present) and the observed four-cell pilot re-run.
- Two lanes, concurrently: TOOL lane (apparatus to your frozen battery; builder = Opus via `~/bin/round`-style one-shots or you) and FRONTIER lane (row 5 move-forms N/F/D per your preregistration, run NOW on the current apparatus, labelled unqualified until the battery is green; then rows 2–4). Measurement never waits on review.
- The captain's log: /home/forge/src/clj-surgeon-records/docs/observations/2026-09-03-captains-log-anvil-seat.md — append dated entries (records worktree /home/forge/src/clj-surgeon-records, branch records/MCP-main; `git pull --ff-only` then `git push origin HEAD:MCP/main`; author forge-anvil, trailer `Co-Authored-By: Gene Kim <genek@itrevolution.com>`). Heartbeat every hour to docs/observations/2026-09-08-night-heartbeat.md (UTC, what ran, receipts, next). Update the tree's log lines on every phase change. Refresh the resume note STATE line (2026-09-03-resume-here-anvil-seat.md line 617) when state changes materially.

## Rules (hard)
- Never contact ports 7888/7890/7894/7895/8300–8339; never touch ~/acid/GO-*, ~/acid/.cohort-lock, chain-*.sh, or any Curtain Call fleet directory; no sudo; NEVER force push; clj-surgeon `main` is frozen (public) — nothing lands there; trunk is MCP/main via `~/bin/land <sha> "<title>"` only, after a Sol review (`~/bin/fence-run <sha> <brief> <name>`) and a battery receipt if distance > 30 (`/var/tmp/forge/receipt-chain-N.sh <tip>` pattern).
- Launch every background job with `~/bin/run-bg <name> <cmd…>` (prints the real pid); waiters bind to that pid; never `pkill`, never `pgrep -f`, never an outer `timeout`; never edit a running bash script; temp under /var/tmp/forge only (/tmp is tmpfs); secrets never printed.
- Subagents/builders never commit or push records; you commit. Code on branches; docs in the records worktree. Every fix registers a linked intent (LID) with a fail-first witness; the intent audit must be green in every round receipt.
- One seat, one file: do not edit ~/bin/round / coldstart / coldstart-grade while a round/coldstart is running; do not run two builders in one worktree (check /proc cmdlines for `codex exec … -C <worktree>`).
- Report headroom, never consumption (disk: `df -h /`; ~45 G remaining at 06:50Z, shared by all seats).

## Priorities for the night (in order; do not descend before the current row's gate)
1. Qualify the apparatus to YOUR frozen battery (adopt the Opus acceptance-3 result if it lifts refusals; else the smallest fix set) → run the observed four-cell pilot (opus/sol × W/Ø, seeds with overlays) → row 1 friction-low; record.
2. Row 5 (move-forms with dependencies) N/F/D on the current apparatus in parallel, labelled unqualified; pre-registration = yours (astra-squares-revisit.md §4), primary gate yours, 30% as the fixed stricter secondary.
3. Tempo: apply your cadence recommendations (frozen battery; ≤15-min reviews; builders write to records; no report relaying).
4. If time remains: rows 2–4 native controls (n ≥ 6 for alias).
Every hour: heartbeat. At 11:45Z: a Gene report (skill gene-report in /home/forge/src/clj-surgeon/skills/gene-report/SKILL.md: vs-native table first, wins AND losses, learnings → ratchets, what's next), written to docs/observations/2026-09-08-gene-report-night.md and pushed.
