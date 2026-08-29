# Captain's Log: Relations Made Namespace Ownership Legible

**Date:** 2026-08-29  
**Bead:** `clj-surgeon-45j`  
**Experiment candidate:** `6328db51557bc39ef1a0d40ca171a1ac9873005a`  
**Retained archive:** `clj-surgeon-bench-archive/2026-08-29/6328db5-cohort-20260829T0851Z.tar.gz`  
**Archive SHA-256:** `1af9110d6bbdbe369cdcdf7feee0f70bac78b0f25717a24d937dfe603ecc9d2c`

## Result

The closed-relation request shape was the only arm that encoded the complete
51-edit decision with valid structural addresses on its first call.

| Arm | Shape | Exact first call | Prompt-to-call midpoint | Capture-only wall midpoint | Payload |
|---|---|---:|---:|---:|---:|
| F | flat edit rows | 0/2 | 65.841 s | 68.500 s | 6,470 B |
| A | rows grouped by file | 0/2 | 83.703 s | 87.000 s | 5,666/5,918 B |
| B | `require_change` + `symbol_migration` + bespoke edit + deletions | 2/2 | 48.912 s | 51.500 s | 2,715 B |

Every B call admitted through the public schema, described all 51 matches in
9 files, compiled to the frozen canonical transaction and future hashes, and
needed no refusal, recovery, search, file read, or second model turn.

The formal three-arm speed comparison did **not** pass: F and A were incorrect,
so their walls are not valid performance controls. B's prompt-to-call midpoint
was descriptively 25.7% below F and its wall midpoint was 24.8% below F, but
those are hypothesis-generating deltas, not a publishable causal speed claim.

This was a capture-only screen. It did not mutate the fixture, run the
transaction verifier, or reproduce the native mutation route. It therefore
does not establish a new Surgeon-versus-native multiple.

## What the models actually got wrong

The flat and file-group failures were not random parenthesis mistakes. Both
flat calls and the admitted file-group call correctly supplied:

- all 23 exact symbol sites, including the three non-default match counts;
- all 9 exact namespace before/after replacements;
- the exact guarded `detail-controls` replacement;
- all 14 owner deletions; and
- all 9 affected files.

Their semantic decision was complete. Their structural address was wrong:
every namespace edit used `within.form=<namespace-name>` instead of the exact
namespace selector `within.namespace=true`. The compiler consequently refused
both flat calls and the first grouped call with `change-owner-mismatch`. The
second grouped call also failed public-schema admission. Every refusal was
pre-mutation and correctly carried no write authority.

B made each kind of intent visible as a separate concept:

```text
one coherent decision
        |
        +-- require_change -------- 9 files / 3 removals
        +-- symbol_migration ------ 23 owner rows / 27 matches
        +-- edits ----------------- 1 exceptional guarded rewrite
        `-- delete_owners --------- 14 exact owners
```

Both B callers completed every branch. The key mechanism is therefore not
"shorter JSON." It is a schema that mirrors the model's plan and removes a
fickle representation decision: the caller states the exact require delta,
while the compiler supplies the namespace owner and guarded clause edit.

## Surprise

File grouping was worse than the flat control despite deleting repeated file
names. Its payload was somewhat smaller, but it took longer and preserved the
same namespace-address hazard. Compression by removing repeated syntax did not
reduce the model's decision burden.

Closed relations did both jobs at once:

1. They named the semantic relationship once instead of asking the model to
   spell every resulting source fragment.
2. They removed the derived namespace-location choice that defeated all four
   flat/grouped attempts.

This is the strongest evidence so far that request materialization cost is
driven by the number and visibility of model decisions, not only output bytes.

## Counterfactual and validity boundary

Had we scored only payload size or server execution, A would have looked
promising and B would have looked like an ordinary compression optimization.
Retaining every model attempt exposed the opposite: A made the model slower and
did not make it correct; B changed first-call behavior.

The cohort is small (`n=2` per arm), its controls are wrong, and it used one
Sol/high caller stratum. The correct decision is:

- **GO** to a bounded product-shaped closed-relation candidate and a real
  mutation cohort;
- **NO-GO** to claim causal speedup from this cohort;
- **NO-GO** to merge the experimental compiler wholesale; and
- **STOP** the generic file-group option unless new evidence explains why it
  would preserve conceptual coverage.

## What becomes cheaper next

The next ratchet is one pure, snapshot-bound compiler that lowers a closed
relation into the existing canonical compact transaction. The product API must
keep four properties:

- the paired relations are explicit and jointly admissible;
- expansion is mechanical against the one frozen source snapshot;
- ambiguity or stale source refuses before mutation; and
- the existing flat route remains available while the candidate is tested.

The next experiment must use actual mutation and exact verification. Its flat
oracle is already known: the retained exact 33-row request uses
`within.namespace=true` for all nine namespace edits. It must compare that
correct flat arm with a relation arm that expands to the same canonical
transaction, preserve the frozen 51-edit/9-file meaning, and measure complete
verified task time. If the closed relation remains exact and first-call
one-shot, it can finally be compared honestly with the retained native
controls.

## Method note

This result exists because the two-lane method worked as designed. SURGEON1
owned the production hypothesis and launch decision. SURGEON2 falsified two
earlier candidates that could admit partial B requests or omit exact cohort
gates. Only candidate `6328db5` earned model tokens. The ugly controls were
kept as evidence instead of tuned away.

That is `(N * K * sigma) / t` in practice: independent schema options,
parallel adversarial review, experiments spent on the uncertain model boundary,
and a short retained decision cycle.
