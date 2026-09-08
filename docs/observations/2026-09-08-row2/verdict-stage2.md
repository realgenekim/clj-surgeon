# Row 2 Stage-2 verdict

**Ruling: UNKNOWN pending one fresh fixed-build cohort.** The frozen Stage-2
verdict remains a loss under its literal gate; the corrected design shows a
large caller-work win, but Artifact A was an observed treatment defect and must
be removed before clause 1 can be earned.

## 1. Artifact A: tool defect; rerun after the fix

Yes. A migration verb may not put receipt/detail/undo files in the repository's
staged change set. They belong under `/var/tmp/forge/<verb>-receipts/` (preferred)
or an explicitly configured gitignored external path, and the returned receipt
must name them. This is a paper cut for `alias_migration`, not a migration
correctness failure: all 21 sites, nine callers, three collisions, protected
prose, lint, load, and tests were correct.

Do **not** re-score clause 1 from the diagnostic as an official observation.
Moving `.clj-surgeon/` after the arm changed the treatment's actual output; O2.a
and O5.2 existed to catch exactly that pollution. The diagnostic is strong
repair evidence—6/6 D candidates then pass O1–O5 with zero minors—but it is not
a prospective accepted run. Land a fail-first artifact-location witness and the
fix, freeze the new tool hash, then rerun the six counterbalanced N/D pairs.
That costs more than reclassification, but preserves the rule that a gate is
not weakened after seeing which treatment failed it.

## 2. Artifact B: meter defect; gate caller work

Yes. Clause 2 must use the already prospective caller-work endpoint
`orient-start -> candidate-complete`. Keep raw `orient-start -> oracle-complete`
wall in every result, but do not gate its ratio. Do not manufacture an
"adjusted all-oracle" clock by subtraction: that adds another classification
boundary while the direct candidate freeze already measures the causal work.
Acceptance remains mandatory, so omitted or failed proof still rejects an arm.

An additive arm-independent constant `k` makes `(D_C+k)/(N_C+k)` approach 1 as
`k` grows. Stage 2 demonstrated the defect: the same caller work yields 0.8002
with the observed apparatus, 0.7366 with `k=161.3 s`, and 0.6483 with
`k=90.5 s`. A threshold whose result changes with idle-detector silence is not
a route-performance gate.

Corrected timing clauses from the frozen pair stamps:

| Clause | Observed | Ruling |
|---|---:|---|
| 2: `median(D_C/N_C) <= 0.75` | **0.3832** | PASS |
| 3: `median(1-D_C/N_C) >= 0.50` | **0.6168** | PASS |
| 4: `median(N_C-D_C) > 46.346 s` | **74.531 s** | PASS |

Retire the all-oracle 0.75/0.70 pass/fail labels. Report the observed raw
all-oracle ratio, **0.8002**, beside the gated caller-work result.

## 3. O5: minors are accepted-with-minor

O5's zero-minor rule is too strict for acceptance. It rejected six correct N
migrations for two continuation-line indents; in P3/P5 it also let harmless
placement of the new libspec become an O2 token-order rejection. Neither changes
resolution, behavior, protected material, or owned migration tokens.

Add `accepted-with-minor`: O1–O4 substantive correctness passes, O5 minor-only
findings count as accepted for the acceptance clause and are reported by arm.
Critical or major findings still reject. Apply this symmetrically to N and D.
Valid insertion position and whitespace are minors; changed non-migration token
order or unrelated require edits remain major. On these facts N is 6/6
accepted-with-minor; the diagnostic D outputs are 6/6 clean accepts.

## 4. Number and tree ruling

Gene should read: **2.61x less caller-work time**—the reciprocal of the
registered median `D_C/N_C = 0.3832`, equivalently **61.68% saved**—with raw
all-oracle `D_A/N_A = 0.8002` beside it. For completeness, the median of the six
individual `N_C/D_C` ratios is 2.65x; 2.61x is the reciprocal of the gate's
registered paired statistic and is the canonical figure.

The corrected timing gate passes, with zero refusals, repairs, second calls,
fallbacks, or operator interventions during source production. Clause 1 is
nevertheless pending a prospective fixed-build run. Therefore row 2 is not yet
a win or a loss under the corrected gate: **UNKNOWN**. It receives no phase
promotion and remains **rough**. A fresh 6/6 clean fixed-build cohort converts
this to the natural-collision win; only then may the row advance through the
tree's linked-witness, landing, review, telemetry, and pilot gates.
