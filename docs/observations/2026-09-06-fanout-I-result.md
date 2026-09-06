# Fan-out I result — informed batched route vs native, fresh gpt-6-astra actors (cohort 16:47:03–16:57:30Z, rc 0, 8 arms, no apparatus fault)

Preregistration: docs/observations/2026-09-06-fanout-I-preregistration.md (written before any run). Receipts: this directory's results.jsonl, summary.json, cohort.log, freeze-b.json (14 pinned files). Servers 7906/8171 on trunk 181c365c. Client Codex CLI 0.153.3 (0.153.4 update declined during the window). Load 1.7–2.6 at arm starts.

| Pair | N arm wall | I arm wall | I/N | correct |
|---|---|---|---|---|
| 1 | N1 103.2 s | I1 69.2 s | 0.67 | both |
| 2 | N2 90.0 s | I2 48.2 s | 0.54 | both |
| 3 | N3 107.2 s | I3 49.8 s | 0.46 | N3 audited INCORRECT, I3 correct |
| 4 | N4 66.2 s | I4 57.0 s | 0.86 | both |
| median | 96.6 s | 53.4 s | 0.55 (1.81x) | I 4/4, N 3/4 |

Preregistered hypothesis: I median < N median by more than the 2SD floor (34.1 s, from cohort B's six controls) with ≥ 3/4 I arms correct → MET (gap 43.2 s; 4/4). Secondary (≤ 2 refusals per I arm): see the ethnography doc (rollout-derived counts) when filed.
Per-pair honesty: pairs 2 and 3 beat the floor on their own (41.8 s, 57.4 s); pair 1 sits on it (34.0 s); pair 4 is inside it (9.2 s). The claim is the median, as preregistered.
Tokens (last cumulative total_token_usage per actor): I 278,505 / 187,310 / 185,367 / 229,832; N 279,292 / 276,182 / 385,679 / 224,562 — the tool arm used fewer tokens in every pair.
Compared with cohort B (same seed, witness, runner; the ONLY change is the route the actor is told): B's deterministic route 232.7 s median, 1/4 correct → I's informed batched route 53.4 s, 4/4. The capability was the same server build both times; the difference is wiring.
Limitations: no new controls (B's six retained as the floor; drift unmeasured); n = 4 pairs; route MANDATED (no adoption claim); pairs are positional (N_k, I_k), not adjacency; the 38 execute-one! sites out of scope; no real DB behaviour proven.
Retained losses cited: cohort B D-route 2.8x loss (docs/observations/2026-09-06-fanout-B-cohort-result.md).

## CORRECTIONS (Astra's audit 17:11Z, accepted; recomputed by this seat from results.jsonl at 17:1xZ)
- The 1.81x is the ACTOR-wall median ratio (96.6 / 53.4). With the external proof included (total_with_proof_s) the medians are N 101.2 s / I 57.8 s = **1.75x**. Report the proof-inclusive figure as the headline; the actor-only figure is a component.
- Correct-only: N 90.0 s (n = 3) vs I 53.4 s = 1.69x by medians (Astra's pair-based reading: 1.64x). Both are descriptive; n is tiny.
- Tokens: the tool arm used fewer tokens in 3 of 4 pairs, not 4 (I4 229,832 > N4 224,562). The 17:0xZ note "every pair" was wrong.
- N3's failure is the witness's textual require-layout check (a seed line no longer present after the require insertion), the SAME layout-sensitive class seen earlier in the day — not a demonstrated semantic defect. The preregistered correctness is the witness, so N3 stays incorrect under the preregistration; the language "native wrong, tool right" overstated what was shown.
- Astra's 16:54Z prediction required four correct per arm including native; unmet (N3). The preregistered hypothesis in preregistration-I.md is met.
