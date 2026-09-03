# Fold-diff round 8 — Sol executed re-check of curtaincall-cfp bridge/fold-diff-tool at 17fa3183 (2026-09-03T12:03Z)

Verdict: **NO-GO** for replacing the production pin (stays 347fe6d3). Fixture item CLOSED; required-var scan PARTIAL (fails open through bound intermediates: an own copy defining only `validate` judged complete); scanner crash → exit 1 with no REFUSED/FAILED line (exit 1 = DIFFERENCES); `:data-dir-with-postgres` still masks unknown-ref refusals against the documented order. `:sessions` structural claim not falsified. Round 9 launched.

## Sol verdict, verbatim

## Round-eight verdict: NO-GO

Do not replace the production read pinned at `347fe6d3` with `17fa3183`. The fixture work is closed, but the required-var scan still fails open through indirection, scanner crashes are misclassified as exit 1, and refusal precedence still contradicts FOLD-DIFF-013.

### Prior open items

- PARTIAL — required-var guard. Direct computed `resolve` and `ns-resolve` now correctly produce `REFUSED :required-vars-unresolvable`, but indirection does not. [fold-diff-required-vars.bb:119](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-required-vars.bb:119)
  
  Re-run: an externally bound namespace/name produced `derived-vars=validate`, an own copy defining only `validate` produced `missing-vars=<none>`, and the guard exited 0.

- CLOSED — representative fixture. `multi-session-log-lines` now includes two sessions; the new real-worktree witnesses cover gap refusal/allowance and real redaction rendering. [fold_diff_test.clj:78](/home/forge/tmp/sol/folddiff8-wt/test/cfp_scheduler_killer/fold_diff_test.clj:78), [fold_diff_test.clj:940](/home/forge/tmp/sol/folddiff8-wt/test/cfp_scheduler_killer/fold_diff_test.clj:940), [fold_diff_test.clj:1023](/home/forge/tmp/sol/folddiff8-wt/test/cfp_scheduler_killer/fold_diff_test.clj:1023)
  
  Re-run: focused suite `56 tests / 226 assertions / 0 failures`; checkpoint driver also folded a private three-event log containing two live sessions.

### Round-eight attacks

The scan behavior was:

- Reader conditional, selected `:clj` branch: enumerated `checkpoint-path`; exit 0.
- Plain string: ignored correctly; exit 0.
- Regex literal: ignored correctly; exit 0.
- `#_` discard: ignored correctly; exit 0.
- Direct computed `resolve`/`ns-resolve`: refused, exit 2.
- Computed symbol through bound intermediates: silently passed, exit 0.

The errexit propagation repair works: a fake `bb` exiting 47 stopped the wrapper before the guard continued. However, [fold-diff-checkpoint:115](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-checkpoint:115) converts that crash to exit 1 with no `REFUSED` or `FAILED` line. Exit 1 is documented as “differences,” so the failure is still semantically misreported.

The actual normal-run refusal order is:

1. `:baseline-ref-unset`
2. `:data-dir-with-postgres`
3. `:baseline-ref-unknown`
4. `:candidate-ref-unknown`
5. `:checkpoint-absent` — shell preflight
6. `:required-vars-unresolvable`
7. `:required-vars-unresolved`
8. Baseline emit: `:checkpoint-absent` or `:checkpoint-invalid`
9. `:baseline-digest-unreadable`
10. Candidate phase: `:baseline-absent`
11. `:unknown-redact-relation`
12. Candidate checkpoint: `:checkpoint-absent` or `:checkpoint-invalid`
13. `:baseline-not-the-expected-ref`
14. `:baseline-other-checkpoint`
15. `:log-shorter-than-checkpoint`
16. `:log-prefix-digest-mismatch`
17. `:log-past-checkpoint`

That is 16 unique refusal kinds; `:checkpoint-absent` has three possible sites.

The documented order says unknown refs precede `:data-dir-with-postgres`, but the data-dir check at [fold-diff-checkpoint:360](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-checkpoint:360) still precedes resolution at [fold-diff-checkpoint:396](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-checkpoint:396). Both `BASELINE_REF=no/such/ref` and an unknown candidate were masked by `:data-dir-with-postgres`, exit 2.

I could not falsify the `:sessions` structural claim. `180f2b3` versus `fc3508d` is the only history pair where checkpoint parsing changed without either digest-covered fold source changing. Both reported digest `5293fb7b…5fa0`, folded sessions `sess-1` and `sess-2`, and returned IDENTICAL. The older parser lacks `validate`, so the driver visibly substitutes the same candidate parser on that side.

### Gates

- Effective focused suite: `56 / 226 / 0`
- Full unit, repository store initially absent: `1102 / 13436 / 0`
- Registry suites: `9 / 1083 / 0`
- `bin/test-fold-diff-checkpoint`, private JSONL mode: exit 0
- Literal `--focus fold-diff-test` selected no tests; the fully qualified focus produced the result above.
- Checkout restored to its initial state: repository store absent; only the pre-existing `.codex/config.toml` disablement remains.

### NO-GO findings

1. [fold-diff-required-vars.bb:133](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-required-vars.bb:133) — computed resolver targets hidden behind bindings pass as only `validate`, and an incomplete own copy is judged complete.

2. [fold-diff-checkpoint:115](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-checkpoint:115) — injected scanner failure stops execution but exits 1, the code reserved for a real DIFFERENCES verdict.

3. [fold-diff-checkpoint:360](/home/forge/tmp/sol/folddiff8-wt/bin/fold-diff-checkpoint:360) — `:data-dir-with-postgres` masks both unknown-ref refusals despite the documented opposite precedence at line 318.