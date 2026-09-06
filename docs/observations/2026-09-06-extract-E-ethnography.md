# extract-E ethnography (Opus reader, read-only, 19:4xZ; timelines from retained rollouts under /var/tmp/forge/cell-prep/runner-b/cohort-extract-E/)

| arm | wall | 1st call | orientation | edit/compose | outer calls | MCP attempts/refusals | server ms | verify | tokens |
|---|---|---|---|---|---|---|---|---|---|
| E1 | 93.2 | 8.2 | 18.3 | 21.5 | 7 | 3/2 | 2.64 + 221 + 2052 = 2.28 s | 28.8 | 242,096 |
| N1 | 101.4 | 7.6 | 17.3 | 43.2 (incl. one failed cell) | 8 | 0 | — | 15.6 | 216,011 |
| N2 | 94.6 | 6.7 | 16.9 | 28.9 | 7 | 0 | — | 22.5 (incl. one rejected cell) | 187,244 |
| E2 | 117.9 | 11.3 | 12.6 | 19.5 | 7 | 3/2 | 3.22 + 211 + 1560 = 1.77 s | 38.5 | 255,938 |

## Where E's wall went
Surgeon server time = 2.28 s (E1, 2.4% of wall) / 1.77 s (E2, 1.5%). Native's whole 20-file mutation ran in 0.1 s (one python cell). Neither mechanical step matters against a ~95 s wall. E paid: a tool-learning preamble (reading SKILL.md and mcp-advanced.md, 5–10 s native never pays); two refusal round trips; ~19 s composing 31 edit specs from completed_plan; then MORE verification than native (28.8 / 38.5 s vs 15.6 / 22.5 s) — the receipt said "written bytes read back and verified · verification_complete" and, in the same block, "⚠ caller proof · structural candidates only; not semantic completeness", so both E actors re-verified natively (byte snapshot/diff, a hand-rolled s-expression reader for form identity, revert-and-compare on the mixed callers). The receipt bought no verification credit. E1's 8 s edge over N1 is N1's one failed cell (a JVM "Warning:" line inside JSON stdout) plus a longer compose; E2 vs N2 (+23 s) is the honest cost of the route.

## Paper cuts
1. inspect_clojure ROOT `expect` refusal hit 2/2 E arms identically (both supplied expect inside the request; the description marks it optional; E2: "the server requires a root expect field that its schema marks optional"). Both then declined to retry, so inspect contributed zero information; all structural knowledge came from the apply refusal's completed_plan.
2. next_call is not directly callable: E2 had to delete args.extraction.public_forms and source_hash before the server accepted its own suggested payload; E1 rebuilt from completed_plan instead.
3. extraction-decisions-required prices all 18 callers as genuine unknowns (per-file source_hash), forcing 31 hand-authored edit specs — the fan-out the extraction verb was meant to absorb; no bulk "apply the obvious retarget to all candidates" affordance.
4. The "structural candidates only" disclaimer is honest and is exactly why verification_complete bought nothing.
5. Native side, for symmetry: N1 lost a call to a JVM Warning line corrupting JSON stdout; N2 lost one to the harness rejecting a bash -lc gate command.
