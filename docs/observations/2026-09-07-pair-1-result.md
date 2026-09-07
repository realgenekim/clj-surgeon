# pair-1 result — native vs tool on two Curtain Call fan-out tasks (in progress)

Preregistration: 2026-09-07-pair-1-preregistration.md. Walls are process stamps, orient-start → verify-done (proof-inclusive; verify = bin/kaocha unit, ~85 s in every run). All runs correct (success-condition greps, kondo, suite 1021/0).

## Wave 1 (14:32–14:36Z, four concurrent runs)

| run | arm | task | edits phase (s) | wall to verify-done (s) | refusals | tool sites |
|---|---|---|---|---|---|---|
| A-N1 | native | speaker-identity (3 owners / 3 files) | 32.3 | 122.5 | — | — |
| A-T1 | tool | speaker-identity | 106.0 | 194.0 | 1 (inspect cardinality; match cannot see inside #()) | 3/3, one apply, 213.6 ms |
| B-N1 | native | crm adopts speakers/rejected (21 sites / 6 owners / 1 file) | 21.4 | 121.5 | — | — |
| B-T1 | tool | rejected | 76.2 | 186.6 | 0 | 21/21, one apply, 677.6 ms |

Wave-1 reading: native faster on both tasks (A: 0.63x tool wall; B: 0.65x). The whole gap is the edits phase — the tool caller spends ~50–75 s more orienting and composing the request (A-T1 lost ~70 s to the #() match defect, inb-f313b8; B-T1 had no refusal and still took 76 s vs 21 s). Server time inside each tool run: < 1 s.
