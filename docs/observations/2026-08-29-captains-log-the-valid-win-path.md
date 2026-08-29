# Captain's Log: The Valid Win Path

Date: 2026-08-29

Beads: `clj-surgeon-45j`, `clj-surgeon-45j.1`

Decision state: HLD review. No new product code, model cohort, install, reload,
or performance claim is authorized by this log.

## The promising number remains a HOLD

The final retained Block 1 made the opportunity visible:

| Arm | `T_emit` midpoint | Complete-wall midpoint | Versus retained 122.278 s native |
|---|---:|---:|---:|
| Normalized flat | 55.594 s | 61.646 s | 1.98x faster |
| Closed relation | 34.141 s | 39.380 s | 3.11x faster |
| Descriptive reduction | 38.6% | 36.1% | not promotable |

All four runs were semantically correct, one-shot, and exactly verified. One
flat request expressed the same edit multiset in a different row order and
failed the frozen ordered-transaction identity. Block 2 correctly did not run.
The retained number is strong evidence for another experiment, not a result to
rescore.

## The architectural answer

The mutation kernel already resolves every compact edit against one frozen
source map, sorts concrete effects by original source address, refuses
intersections, and applies accepted effects in reverse address order. Caller
row order is therefore not mutation authority after exact resolution and a
complete disjointness proof.

The safe design is a dual contract:

```text
submitted order             proven effect identity
---------------             ----------------------
request provenance          semantic equivalence
diagnostic indexes          canonical future identity
exact request replay        causal cohort admission
receipt presentation
```

The new identity must be derived after the common generic compiler proves
paths, guards, concrete spans, and non-overlap. It must not sort untrusted
request rows before resolution. Same-span, overlapping, nested, cascading, and
same-boundary insertion cases still refuse under every permutation. Caller
order remains authority inside one insertion payload and in routes outside the
first compact-edit slice.

This is documented in
`docs/decisions/2026-08-29-disjoint-compact-edit-order.md`. If approved, the
next LID phases are LLD, EARS, red witnesses, and only then the pure projection.

## The fleet corrected the causal story

The initial story was that the relation made the model think less. Retained
telemetry does not support that claim.

- Normalized-flat arguments contained 6,509 compact JSON characters.
- Relation arguments contained 2,871 characters, 55.9% fewer.
- Median visible output was about 51.0% lower for the relation arm.
- Median recorded reasoning tokens were slightly higher for the relation arm,
  not lower.
- Across the four points, output tokens predicted `T_emit` with
  `R² = 0.99886`; the predicted arm delta was 21.417 s and the observed delta
  was 21.453 s.

The honest surviving hypothesis is simpler and still valuable: the closed
relation lets the model emit substantially less exact JSON. It may reduce
planning work too, but this cohort does not show that. A serialization win is a
product win when correctness and complete verified time also improve.

## What could still make the signal an artifact

The adversarial fleet ranked the remaining threats:

1. `N R R N` aliases the relation arm with the two middle service periods. A
   reversed `R N N R` block is required to break that alias.
2. The shared production description recommends the relation route. That is
   appropriate for a production-package claim but confounds a pure
   representation-mechanism claim.
3. The prompt presents the decision in relation-shaped tables. The result may
   measure task-to-request alignment rather than a general algebra advantage.
4. Provider cache telemetry differed by period. A fresh reversed block, not a
   post-hoc regression, is the remedy.
5. The winning candidate followed several correctness-driven grammar changes.
   A new frozen cohort is necessary to bound survivor bias.

Server time is not the explanation: the relation kernel was about 62 ms slower
at the midpoint. Result interpretation explains less than one second of the
complete-wall delta. The opportunity remains before the first call, in exact
request construction and emission.

## What constitutes a valid win

The smallest promotion screen remains eight fresh runs:

```text
Block 1: N R R N
Block 2: R N N R
```

Before the first token, freeze candidate commit and tree, prompt, complete
client surface, workspace fixture, oracle, scorer, clock law, and thresholds.
After the first token, retain every attempt. Do not replace or rescore an
incorrect, nonadherent, retried, unverified, or incomplete run.

Admission must keep four judgments separate and require all four:

1. representation adherence: N used complete flat data or R used the complete
   closed relation pair, with no omitted or extra decision;
2. effect correctness: the public compiler produced the exact canonical effect
   identity, 51 edits, 9 files, future/read-back hashes, and exact verifier;
3. route adherence: first actionable item, one joined mutation call, no
   preamble/read/shell/file/retry path; and
4. task completion: exact required terminal response and final turn.

Use precise clock names:

- `T_emit`: turn start to the complete argument event;
- `T_apply_verified`: turn start to exact MCP verification completion; and
- `T_complete_verified`: turn start to the completed final response.

`T_emit` is the causal mechanism metric. `T_complete_verified` is the user
outcome and the promotion metric. Block 1 should authorize Block 2 only if all
four runs are admitted, R improves `T_emit` by at least 20%, and R improves
`T_complete_verified` by at least 15%. Final promotion requires 8/8 admission,
R `T_emit` at least 20% faster in each block and pooled, R
`T_complete_verified` lower in each block and at least 20% faster pooled. The
20% laws are unchanged; equality passes and a result just below does not.

An independent second eight-run cohort is required before quoting a headline
rate. The first eight-run screen establishes this one frozen mechanism, not a
p95 or general task-family claim.

## Options deliberately kept separate

The fleet searched for a larger retained task that might amplify the effect.
No retained candidate is currently admissible under the existing one-pair
grammar and frozen-evidence laws.

- The Sessionize `format` extraction has roughly 105 effects but lacks a
  frozen post-move/pre-cleanup snapshot and exact owner-row corpus.
- The full views split has hundreds of sites but needs a closed multi-relation
  grammar and an isolated scorer.
- A four-foundation bundle is expressible only as four pairs, not one current
  call.
- A 60-site server slice crosses many target aliases and is not one relation.
- The retained 60-site SCI transformation is already an orthogonal O(1)
  expression and is a useful negative control, not a reason to widen relations.

Do not enlarge the first causal cohort. A larger single task tests scaling; a
heterogeneous family tests generalization. After the 51/9 mechanism is decided,
run any small/medium/large family as a separate predeclared experiment using
within-task ratios rather than pooled raw seconds.

## Course

1. Review the disjoint-effect HLD decision.
2. If approved, complete the LID chain and permanent permutation/refusal
   witnesses.
3. Freeze a new candidate and execute a fresh Block 1.
4. Run the reverse block only if the unchanged stop law authorizes it.
5. Keep the old HOLD and every loss in the chart.

This path maximizes option value without laundering old evidence. The hopeful
part is real: a 36% descriptive complete-wall reduction appeared on a fully
correct, exactly verified route. The discipline is equally real: only new,
counterbalanced, predeclared evidence can turn it into a win.
