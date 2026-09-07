# plan-2 Cell B result — exports ICS/calendar extraction (25 forms → cfp-scheduler-killer.exports.calendar), Astra's rule, pinned lanes (in progress)

Task frozen by the independent acceptance owner (cellB/task.md); helpers list supplied, caller tables NOT supplied; oracle A–E mutation-tested (clean PASS, M1–M5 FAIL). Two concurrent runs per pair on taskset lanes 0–5 / 6–11; Opus callers; 30-min cap. Walls are process stamps from orient-start; "work" = orient-start → callers-complete; "oracle" = through the full A–E oracle (~4 min of apparatus in every run).

| pair | arm | plan | moves | callers (work) | suite green | oracle (all) | correct |
|---|---|---|---|---|---|---|---|
| 1 | N1 native (lane 0–5) | 63 | 121 | **142** | 249 | **491** | yes |
| 1 | M1 helper_extraction MCP (lane 6–11) | 97 | 163 | **290** | 387 | **627** | yes, 4 MCP calls |

| 2 | N2 native (lane 6–11) | 96 | 153 | **185** | 284 | **530** | yes |
| 2 | C1 CLI :extract! (lane 0–5) | 57 | 82 | **148** | 245 | **490** | yes |

Pair 2 reading: both correct; the CLI move took 25 s after a 57 s plan; C1 work 148 s vs N2 185 s (native 1.25x SLOWER — first tool win on work time today); to the oracle 490 vs 530. The caller repaired the CLI's known defects (cyclic source require, unresolved retained refs) inside its callers phase.

Pair 1 reading: both correct; native work time 142 s vs MCP 290 s (native 0.49x); to the oracle 491 vs 627 (0.78x). Pairs 2–4 (N2+C1, N3+C2, N4+M2) follow per the precommitted order.
