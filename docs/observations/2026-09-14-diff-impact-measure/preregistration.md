# Preregistration — diff-impact selection vs the full fast lane — sealed 2026-09-14T13:59:04Z

Subject: clj-surgeon trunk 866df39b (stable/2026-09-14.5; diff-impact with file-content, source-scan, test-side edges and HOLD on unmatched files). Selector: `test/diff-impact BASE OUT fixed-point` (list mode for the selection receipt) as landed. Native control: the gate's full fast lane on the same tip snapshot, same environment (`make test-fast` or the exact runner argv the landing gate uses for cadence :fast), run FIRST for each diff.

## Five diffs, frozen before any run (base..tip on trunk history)

| id | base..tip | kind | expectation |
|---|---|---|---|
| D1 | df0c9e1c^..df0c9e1c | one line in src/clj_surgeon/probe.clj | historical miss: at df0c9e1c the full lane had splice-envelope-test red; selection must include it (92 selected on this snapshot in the round-2 replay) |
| D2 | 00566756..8aedb65e | test-side runner change (battery_ledger.clj, battery_parallel_runner.clj) | selection includes battery-ledger-test, battery-parallel-test, lane-manifest-test (9 selected in replay) |
| D3 | ad05e27c^..ad05e27c | committed RED: the tombstone-collision race schedule oracle | the full lane is red on the race oracle namespace(s); selection must include every namespace the full lane fails |
| D4 | 7bf44cfe^..7bf44cfe | green src fix (probe image cleanup) | narrow selection; full lane green |
| D5 | 6e77580f^..6e77580f | docs-only (battery pass record) | :nothing-selected (docs allowlist) or HOLD; zero namespaces run |

## Arms and meters (per diff, n=2 runs per arm, native first, alternating thereafter; box load < 4 at launch, else wait)

- NATIVE: full fast lane on the tip snapshot. Record: wall to verdict (ms), pass/fail per namespace, exit.
- TOOL: selection (list + fixed-point) on base..tip, then run the selected namespaces through the same lane runner. Record: selection wall, run wall, total wall to verdict, selected count, HOLD/nothing-selected with its reason, pass/fail per namespace, exit.
- Catch preservation: for every namespace the NATIVE arm fails, it must be in the TOOL selection AND fail there too. Reported as k/k per diff; vacuous (0/0) diffs are marked vacuous, never counted as preserved.
- First-attempt: TOOL arm counts a HOLD as a refusal with a named reason; a crash or missing receipt is a failure.

## Bets (Fable, sealed now)

- Wall ratio TOOL/NATIVE: D1 ≈ 0.6 (92 of ~165 selected), D2 < 0.2, D3 < 0.3, D4 < 0.2, D5 ≈ 0 (nothing run) — median across D1–D4 < 0.35.
- Catch preservation: D1 1/1, D3 k/k with k ≥ 1; D2, D4 vacuous.
- First-attempt: 5/5 TOOL, 5/5 NATIVE.
- If any NATIVE failure is absent from the TOOL selection, the selector loses regardless of wall (kill-switch class).

## What this does and does not establish

Historical replay establishes performance and regression evidence for the selector on this snapshot. It does not establish prospective transfer (Astra, 2026-09-14): that is the sealed diff-impact encounter, accepted by an independent owner on the next real diff.

Setup and noise: report the setup cost once (worktree + classpath warm), report each arm's wall raw, and the noise gate = the spread of the two NATIVE runs per diff; a ratio inside the noise gate is a tie.

## Addendum — sealed 2026-09-15T05:10:19Z, before the repaired replay (one replay only)

Ordered by Gene ("Ask Astra, then go on best plan"); Astra's order and corrections in 2026-09-15-astra-plan-order.md. The original bets stand as BROKEN; this addendum specifies the repaired comparison and new predictions.

Repair (product change, to land on trunk first): the selector hands its selected subset to the existing lane runner (the same machinery the gate uses, 16 lanes for :fast), with the receipt marked :partial and the census check scoped to the subset; the child launcher carries -Duser.home inside the run root like the gate's fast-lane child. No new executor.

Repaired comparison, same five frozen diffs, n=2 per arm, native first, load < 4:
- NATIVE: the full fast lane on the tip, unchanged.
- TOOL-FAST (the speed claim): selection, then the selected ∩ fast-lane members through the fast runner. Wall to verdict, catch preservation against NATIVE's fast-lane failures.
- TOOL-ALL (operational, reported separately, never blended): the selected set across all lanes through the lane runner; its extra catches (battery-lane) and cost reported.

Bets (TOOL-FAST/NATIVE):
- Fable: D1/D3/D4 median 0.8 (plausible 0.6–1.1); D2 0.45; D5 0.35; catch preservation D1 1/1, D3 k/k; first-attempt 5/5 both arms; TOOL-ALL slower than NATIVE on D1/D3/D4 (extra catches are its value, not wall).
- Astra: D1/D3/D4 median 0.9 (0.7–1.2); D2 0.5 (0.35–0.75); D5 0.4 (0.3–0.65); selection floor ~4.3 s = 0.14 of a 30 s lane; will not bet on an all-lanes win.
Decision rule (Astra's, accepted): absent a TOOL-FAST win beyond the noise gate with preserved catches, native routing stays and the next transfer candidate is chosen; no further apparatus on the selector.
