---
parent: sibling-pair-edit-design
status: "proposed (bridge, 2026-09-02); awaiting Gene's ratification"
---

# Sibling-Pair Edit Adversarial Review

Status: **proposed (bridge, 2026-09-02); awaiting Gene's ratification**.

## Method

Two exact self-REFUTE passes attacked the complete design/spec packet against
the field fixture, current schemas and parser, refusal no-authority law,
disjoint-order decision, CLJC ambiguity, layout fidelity, and context-free LID
requirements. This records design review, not independent ratification.

## Exact pass 1

Reviewed draft contract: `insert_pair`, owner + context + anchor/end, bounded
pair table, lossless insertion, frozen transaction, receipt provenance.

Verdict: **REFUTED**.

| Attack | Failure found | Correction incorporated |
|---|---|---|
| Can `position=end` identify one of two `cond->` forms? | Context plus owner did not provide singular authority. | Require exactly one eligible container for end; stable `ambiguous-pair-container`. |
| Does “preserve gaps” explain new bytes? | It protected old gaps but left separator synthesis implicit. | Define copied internal and outer comment-free templates and refuse missing/unsafe templates. |
| What is the tail of `case` with a default? | An odd tail was either malformed or could swallow the default. | Define final odd form as default, outside the pair run; end inserts before it. |
| Is all `condp` pair-structured? | `:>>` clauses have a third form. | Refuse `unsupported-pair-shape`; make `condp :>>` an explicit non-goal. |
| Can `matches=2` hide two different container choices? | Cardinality alone could conflate candidate containers. | Define every retained anchor occurrence as a separately guarded site; no collapsing. |
| Does pair exclude threading? | Width-one behavior was underspecified. | Add closed width-one rows and make only outer separator proof applicable. |
| Are comments merely preserved or safely owned? | Copying a comment-bearing gap could duplicate or reattach a comment. | Refuse any required non-whitespace template; preserve unrelated comments byte-for-byte. |

## Exact pass 2

Reviewed corrected contract including singular end resolution, copied gap
templates, `case` default, `condp :>>` refusal, and width-one rows.

Verdict: **REFUTED, then corrected to SURVIVES for proposal**.

| Attack | Failure found | Correction incorporated |
|---|---|---|
| How does `.cljc` avoid branch choice? | Physical uniqueness alone did not bind platform-visible owner counts. | Require `within.platform` for `.cljc`; refuse conditional crossing/duplication. |
| Can request order order two appended pairs? | Two zero-width effects could share a boundary without intersecting spans. | Declare identical insertion boundaries overlapping for admission; refuse every permutation. |
| Does the receipt expose enough to audit pair insertion? | Generic intent counts did not distinguish width, position, or anchor. | Add bounded operator/context/width/position/count and anchor-fingerprint provenance. |
| Does refusal naming cover schema through commit? | The matrix omitted explicit platform and existing transaction families. | Add platform refusals and delegate stale/parse/verification/write failures to existing stable families. |
| Does anchor equality normalize spelling? | Value equality could collapse metadata, reader spelling, or commas. | Require lossless form fingerprints and preserve exact payload spelling. |
| Is map insertion duplicating `assoc_entry`? | Both can add a map entry, with different admission promises. | State this is the compact cross-context action; `assoc_entry` remains generic-change compatibility and is not changed. |
| Could ratification trigger implementation? | Phase authority was not repeated in each artifact. | Mark all statuses proposed, every ID `[D]`, and enumerate excluded later phases. |

## Residual risks and verdict

The largest implementation risk is proving reusable concrete-syntax gaps
without accidentally treating commas, discards, or reader conditionals as
whitespace. The largest interface risk is end-address ambiguity in owners with
several same-head containers; the deliberate refusal may still require an
anchored call. The largest scope risk is pressure to generalize from pairs to
arbitrary tuples; the closed table and `condp :>>` refusal contain it.

No remaining contradiction was found between the corrected proposal, frozen
snapshot/count laws, refusal completeness, or disjoint edit order. Verdict:
**SURVIVES as a proposal; independent review and Gene's ratification remain
required.**
