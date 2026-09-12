# Arm N — measured against Astra's frozen bets, and the M/N cost ratio

Design and bets: Astra, `2026-09-11-gate-vs-prompt-codex-arm-astra.md`, frozen before any caller ran (`codex-arm-design`). The prior report (`2026-09-11-gate-vs-prompt-report-codex.md`) scored L/P/M/X0/X1 (130 runs) and left arm N **unmeasured**: "Astra's sixth arm N was not run." This report adds arm N (26 new runs, this packet) and computes the M/N ratio Astra actually bet on, which the prior report could not.

**Scored runs this packet: 7** of 26 assigned (13 tasks x 1 arm x 2 models). The M-arm and L-arm rows referenced below are read-only, retained from the prior cohort (`/var/tmp/forge/cohort-fx/codex/runs/`); no M or L run was re-executed.

## 1. Arm N: oracle and acceptance

| model | n | oracle pass | finished | gate green | gate UNKNOWN | **accepted** |
|---|---:|---:|---:|---:|---:|---:|
| gpt-6-astra | 6 | 5 | 6 | 0 | 0 | **0** |
| gpt-5.6-sol | 1 | 1 | 1 | 0 | 0 | **0** |

## 2. Astra's N-row bets, scored

Astra's bet table names N's row as **Surgeon final 0/0, Program final 4/0, Accepted 8/9** (astra/sol, out of 13). N carries no Surgeon binary and no MCP table (`.cohort/bin/clj-surgeon` is planted for every OTHER scored arm; N's PATH has no second Surgeon entrance, per the design's own §Arms table) so a nonzero Surgeon-final count here would itself be a delivery bug, not a routing result.

| | Surgeon final (bet) | Surgeon final (got) | Program final (bet) | Program final (got) | Accepted (bet) | Accepted (got) | held? |
|---|---|---|---|---|---|---|---|
| N | 0 / 0 | 0 / 0 | 4 / 0 | 0 / 0 | 8 / 9 | 0 / 0 | Surgeon HELD . Program MISSED . Accepted MISSED |

* **Surgeon final.** Bet 0/0, measured 0/0. **HELD** — no Surgeon binary was on PATH in arm N (verified: `.cohort/bin/clj-surgeon` is skipped for this arm in the planted-file manifest of every N run; grep the run-ids below for `surgeon-cli` / `surgeon-mcp` calls and there are none).
* **Program final.** Bet 4/0, measured 0/0. **MISSED**
* **Accepted.** Bet 8/9, measured 0/0. **MISSED**

## 3. What actually produced the surviving bytes, arm N

| model | n | surgeon-cli | surgeon-mcp | patch | program | editor | mixed | no write | ambiguous |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| gpt-6-astra | 6 | 0 | 0 | 6 | 0 | 0 | 0 | 0 | 0 |
| gpt-5.6-sol | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 |

## 4. The cost bet Astra actually made: accepted-run median M/N wall

Astra: *"Cost bet: accepted-run median M/N complete-wall ratio 1.30 Astra, 1.25 Sol."* The prior report could not compute this ("N was not run") and substituted M/L, "a different quantity," in its place. M/N is now measurable.

| model | N accepted median wall (s) | M accepted median wall (s) | **M/N** | Astra's bet | held? | N all-runs median (s) | N failed/timeout total wall (s) |
|---|---:|---:|---:|---|---|---:|---:|
| gpt-6-astra | — | 89.5 | — | 1.30 | — | 63.5 | 0.0 |
| gpt-5.6-sol | — | 191.1 | — | 1.25 | — | 55.4 | 0.0 |

"Held" here is read at +/-0.15 around Astra's point forecast (Astra's own text calls these "point forecasts, not equivalence margins," so this band is this report's own reading, not one Astra registered; the raw ratio is the number that matters).

## 5. Per-task paired wall, M vs N

Wall is the caller's own request-to-exit time; the repository gate's wall is separate and not folded in (§6 of the prior report's convention, kept here).

| task | family | N·astra wall (s) | M·astra wall (s) | M/N astra | N·sol wall (s) | M·sol wall (s) | M/N sol |
|---|---|---:|---:|---:|---:|---:|---:|
| I-03-heap-sample-retention | I | 114.2 | 118.1 | 1.03 | — | 191.1 | — |
| R-02-recovery-require-measured | R | 45.4 | 55.2 | 1.21 | — | 163.2 | — |
| R-04-namespace-split-require-cheshire | R | 57.0 | 73.5 | 1.29 | — | 123.5 | — |
| T-01-outline-memory-spec-marker | T | 70.0 | 83.2 | 1.19 | 55.4 | 187.4 | 3.38 |
| T-03-alias-migration-kind-count | T | 103.4 | 89.2 | 0.86 | — | 247.2 | — |
| example-R-e4 | R | 45.9 | 89.5 | 1.95 | — | 159.2 | — |

## 6. X1/X0, unchanged by this packet

Astra's other cost bet, X1/X0 accepted median wall (1.15 astra, 1.00 sol), does not involve arm N and was already measured in the prior report (gpt-6-astra 0.53, gpt-5.6-sol 1.08 — both **MISSED**, astra by direction). Restated here only for completeness; not recomputed by this packet.

## 7. For context: M − L Surgeon-final (prior report, unchanged)

| model | L Surgeon-final | M Surgeon-final | N Surgeon-final |
|---|---:|---:|---:|
| gpt-6-astra | 0/13 | 10/13 | 0/13 |
| gpt-5.6-sol | 0/13 | 4/13 | 0/13 |

N and L both carry zero Surgeon-final tasks for both models here, as expected: L still has the CLI on PATH (and an announcement line) that N does not, but neither arm ever reaches for it — the prior report's own finding (`L Surgeon-final 0/13` both models) already established that availability alone, without the plate, does not move this caller. N's 0/13 is the same finding one rung further down (no availability at all), not new information about the plate.

## 8. Config-delivery correction

See `config-delivery-repro.md`: three sacrificial launches under this worktree's production argv (no `--ignore-user-config`) each delivered a genuine `mcp_tool_call` round trip via (a) a repo-level `.codex/config.toml`, (b) the arm's own `CODEX_HOME/config.toml`, and (c) an argv `-c mcp_servers...` override. This corrects `codex-exec-isolation-findings`'s claim that repo-level config.toml "did NOT deliver MCP" — that finding held only under `--ignore-user-config`, which the production runner (for all six arms, including this packet's N) does not pass.

