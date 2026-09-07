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

## Wave 2 (14:38–14:42Z, four concurrent runs)

| run | arm | task | edits phase (s) | wall to verify-done (s) | refusals | tool sites |
|---|---|---|---|---|---|---|
| A-N2 | native | speaker-identity | 36.1 | 124.8 | — | — |
| A-T2 | tool | speaker-identity | 80.0 | 168.6 | 0 | 3/3, one apply, 235.8 ms |
| B-N2 | native | rejected | 21.5 | 111.6 | — | — |
| B-T2 | tool | rejected | 83.3 | 209.0 | 0 | 21/21, one apply, 587.6 ms |

## Headline (n = 2 per arm per task; all 8 runs correct)

| task | native median wall (s) | tool median wall (s) | native / tool | tool first-attempt |
|---|---|---|---|---|
| A speaker-identity (3 owners / 3 files) | 123.7 | 181.3 | 0.68x (native faster) | 1 of 2 (A-T1 one inspect refusal) |
| B crm adopts speakers/rejected (21 sites / 6 owners) | 116.6 | 197.8 | 0.59x (native faster) | 2 of 2 |

Verdict against the preregistered falsifier: **falsified** — the tool median is above the native median on both tasks. Correctness was equal (8/8 green, identical diffs modulo require order and one wrapped-line indent that neither route preserves).

Where the time goes: verify is ~85 s in every run (identical). The edits phase is 21–36 s native vs 76–106 s tool. The native callers read in one batch and applied one scripted edit with count guards; the tool callers loaded schemas, composed an inspect batch, read owner_counts, composed the apply, then still did the ns/require/deletion edits natively. Server time per tool run < 1 s. The tool's contribution was 3 or 21 of ~9–27 edit operations; the rest of the task was native in both arms.

What this changes: the fan-out class's earlier 1.75x (cohort I) was measured with fresh actors discovering owners themselves; here both arms were given owners and counts, which removes the discovery advantage — exactly the informed setting the plate encodes. In the informed setting, on these sizes (3 and 21 sites), the tool does not beat native. Route implication: the fan-out auto-route should be narrowed to cases where discovery is the cost (owners unknown, or counts large enough that a scripted native edit needs its own verification), or retired pending a larger-N pair. Filed as a routing decision for Gene/Astra, not changed on the plate by this seat.

Friction found: inb-f313b8 (match cannot see inside #(); cardinality refusal names no request). Receipts: /var/tmp/forge/pair-1/<run>/ copied to this directory's pair-1/ subfolder.
