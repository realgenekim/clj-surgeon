# extract-E result — extract-to-namespace, tool route vs native, fresh gpt-6-astra actors: LOSS (19:33:08–19:36:53Z, shared-box, parallel pairs, rc 0, no apparatus fault)

Preregistration: docs/observations/2026-09-06-extract-E-preregistration.md (before any actor). Task: move instance/database/iam-jdbc-url/get-ds/deep-merge from maven.db to a new maven.db-helpers and update 18 callers (4 mixed). Correctness = seed gate (58/190) + proof-e witness (18 callers, 5 moved forms, outside files byte-identical). Servers on e8076379; client 0.153.3; shared-box mode (Astra's alias controls ran concurrently through slots; load 1.6 at start) — concurrent-load performance, not a quiet-window measurement.

| pair | native | tool (E) | E/N | correct |
|---|---|---|---|---|
| 1 (launched together, E first) | N1 101.4 s | E1 93.2 s | 0.92 | both |
| 2 (launched together, N first) | N2 94.6 s | E2 117.9 s | 1.25 | both |
| median (actor wall) | 98.0 s | 105.5 s | 1.08 (tool SLOWER) | 4/4 |
Proof-inclusive: N 100.9 s / E 108.4 s.

Verdict against the preregistration: FALSIFIED (hypothesis was E median < N median by > 30 s; E is ~8 s slower). Both tool arms correct; the tool's own extraction call is ~2 s of a 93–118 s wall (hand-drive: plan probe 0.31 s + submit 2.11 s, 31 edits / 20 files). My expectation (native 150–300 s) was wrong by 2x: a fresh gpt-6-astra actor hand-rolls a 20-file extraction in ~100 s.
Reading (pending the rollout ethnography): the class "no native verb" does not translate into a wall gap when the native actor can script the move; the wall is orientation and request construction on both sides. Same shape as the whole-task pairs.
Limitations: n = 2 pairs; concurrent-load; the witness does not police extra content in the destination ns; no control floor for this cell.
