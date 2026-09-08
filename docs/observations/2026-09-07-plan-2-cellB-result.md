# plan-2 Cell B result — exports ICS/calendar extraction (25 forms → cfp-scheduler-killer.exports.calendar), Astra's rule, pinned lanes (in progress)

Task frozen by the independent acceptance owner (cellB/task.md); helpers list supplied, caller tables NOT supplied; oracle A–E mutation-tested (clean PASS, M1–M5 FAIL). Two concurrent runs per pair on taskset lanes 0–5 / 6–11; Opus callers; 30-min cap. Walls are process stamps from orient-start; "work" = orient-start → callers-complete; "oracle" = through the full A–E oracle (~4 min of apparatus in every run).

| pair | arm | plan | moves | callers (work) | suite green | oracle (all) | correct |
|---|---|---|---|---|---|---|---|
| 1 | N1 native (lane 0–5) | 63 | 121 | **142** | 249 | **491** | yes |
| 1 | M1 helper_extraction MCP (lane 6–11) | 97 | 163 | **290** | 387 | **627** | yes, 4 MCP calls |

| 2 | N2 native (lane 6–11) | 96 | 153 | **185** | 284 | **530** | yes |
| 2 | C1 CLI :extract! (lane 0–5) | 57 | 82 | **148** | 245 | **490** | yes |

| 3 | N3 native (lane 0–5) | 153 | 217 | **263** | — | **602** | yes |
| 3 | C2 CLI :extract! (lane 6–11) | 73 | 92 | **230** | 333 | **578** | yes |

| 4 | N4 native (lane 6–11) | 164 | 200 | **248** | 337 | **581** | yes |
| 4 | M2 helper_extraction MCP (lane 0–5) | 84 | 238 | **280** | 367 | **614** | yes |

Pair 4 reading: both correct; N4 work 248 s vs M2 280 s.

## Headline (8 runs, all correct)

| arm | work (s) | median | oracle (s) | median |
|---|---|---|---|---|
| native ×4 | 142, 185, 248, 263 | 217 | 491, 530, 581, 602 | 556 |
| CLI :extract! ×2 | 148, 230 | 189 | 490, 578 | 534 |
| MCP extraction ×2 | 290, 280 | 285 | 627, 614 | 621 |

Verdict against the preregistered killer threshold: NOT met. CLI is ~13% faster on work (27 s), inside the native spread (142–263 s); MCP ~32% slower. The CLI's win is the move step alone (19–25 s vs 36–64 s); discovery, retained-ref qualification, promotions, cycle-breaking and callers stayed native (C2 put the 43-site caller sweep through one :change! transaction). MCP lost to a categorical helper refusal plus repairs of its fallback's output. Planning/discovery 57–164 s did not differ by route. n=2 per tool arm; 4 native controls (deviation from 6 recorded).

Pair 3 reading: both correct; C2 work 230 s vs N3 263 s (CLI again faster on work, by 33 s); oracle 578 vs 602. CLI arms so far: 148, 230 s; native controls so far: 142, 185, 263 s.

Pair 2 reading: both correct; the CLI move took 25 s after a 57 s plan; C1 work 148 s vs N2 185 s (native 1.25x SLOWER — first tool win on work time today); to the oracle 490 vs 530. The caller repaired the CLI's known defects (cyclic source require, unresolved retained refs) inside its callers phase.

Pair 1 reading: both correct; native work time 142 s vs MCP 290 s (native 0.49x); to the oracle 491 vs 627 (0.78x). Pairs 2–4 (N2+C1, N3+C2, N4+M2) follow per the precommitted order.

Notes from C2's report: the whole 43-site caller sweep went through ONE `clj-surgeon :op :change!` transaction (9 changes, per-change expect matches, aggregate expect {changes 9 edits 43 files 9}; the selector is token-aware — `exports/calendar-ics` matched 14 not 16), so C2 is the first arm with tool-committed caller rewrites. Its `:extract!` dry run reported `required-public-forms []` for a block with 8 retained-var dependencies (false negative; the file would not compile) — same defect class as M1's. Apparatus caveat (both C arms noticed): oracle A8's grep is case-sensitive while clj-kondo emits `Unresolved symbol`, so A8 may be vacuous; the arms checked lint output directly. To fix in the apparatus before any further cohort.
