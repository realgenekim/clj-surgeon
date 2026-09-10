NO-GO

# Sol fence review, round 16: preservation brief

- reviewed: sealed candidate `f0b814f9b5f11574d4b996905f46e5fcf1174394`
- branch tip accepted by the brief: `e795251891f45c97e173b1c53bfbfdc52e672ba7`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 15 `NO-GO` on PB-FENCE-022
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-023 — the Markdown/JSON equality oracle erases duplicate rows

The round-16 repair closes PB-FENCE-022. `refusal-rows` is now the single producer,
there is exactly one `file-refusal` call site and it is inside that producer, both
the real two-index pipeline and `--refusal-probe` call the producer, and the literal,
second-definition, and pipeline-seam drift controls all break the property.

The new end-to-end renderer check is nevertheless not exact. At
`bin/preservation-tables-test:563-567`, both the rows parsed from Markdown and the
pipeline rows read from JSON are converted to sets before comparison. That discards
multiplicity. A renderer can therefore print an extra duplicate reconciliation row
while the asserted sets remain equal.

I exercised that attack against a copy of the real candidate at
`/var/tmp/forge/item3/r16-render-dup.L0D3Le`. I changed only the renderer's input at
`bin/preservation-brief:2293` from `reconcile-failures` to
`(concat reconcile-failures reconcile-failures)`. The complete copied
`bin/preservation-tables-test` still exited 0, including R8's 384-map property, all
seven real refusal specimens, and all three behavioural drift controls.

On the real generated `ns-count` specimen, the mutated Markdown contained four
rows — the base and candidate rows twice each — while JSON `refusal_rows` contained
only the two pipeline rows. The test still passed because both sides reduced to the
same two-element set. This directly falsifies the requested claim that the parser is
strict enough to notice an extra or missing row, and it violates the brief's promise
that each refused file contributes exactly one reconciliation row per tree.

Repair must compare an ordered vector or another multiplicity-preserving value,
strictly scoped to the reconciliation table, and retain this duplicate-row mutation
as a behavioural drift control. That repair changes `bin/`, which the ship-fix
contract classifies as `HOLD reason=oracle-changed`; I therefore left no
`GO-WITH-FIX` patch.

## Candidate checks completed

- The unmodified candidate's `bin/preservation-tables-test` exits 0. R1-R8 pass:
  seven generated real refusal specimens, the 384-map dispatch property, and the
  literal-branch, second-definition, and pipeline-seam behavioural controls.
- The single-producer source shape is present: one `refusal-rows` definition, one
  `file-refusal` call site, and two intended calls to `refusal-rows` (probe and real
  pipeline).
- A bounded replay reran the real Cell C clean row and the wrong-binding row before
  the blocking renderer falsifier ended the review. Clean remains 141 moved / 99
  preserved bodies, 32 preserved / 32 review-required caller bodies, zero refused
  files and `refusal_rows []`; wrong-binding reduces preserved bodies to 98. The
  table oracle independently reran all seven small end-to-end refusal specimens.
- Independence remains intact: the three implementation files import no
  `clj-surgeon.*` namespace; receipt reading remains isolated to the optional
  receipt-comparison state and section C. Replay still starts from `git archive` of
  the real Curtaincall CFP trees, not a synthetic Cell C.
- Section B's hard stops and the section-9 preregistration are unchanged from the
  prior review. The preregistration still refuses a per-class rate estimate at its
  denominator and defines safety as strict dominance.
- `b30aa9e7..e7952518` changes only `bin/preservation-brief` (+22/-12) and
  `bin/preservation-tables-test` (+30/-4); `git diff --check` is clean. The tip is
  nineteen commits over trunk.

## Decision

PB-FENCE-022 is repaired, but the new renderer/pipeline witness accepts a Markdown
table with extra rows. The reviewer still must inspect row multiplicity manually,
so the requested proof-burden boundary remains open.


> END RECEIPT (fence-run): worktree HEAD at review exit = f0b814f9b5f11574d4b996905f46e5fcf1174394 = fenced sha.
