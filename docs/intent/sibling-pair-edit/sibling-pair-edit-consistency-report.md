---
parent: sibling-pair-edit-design
status: "proposed (bridge, 2026-09-02); awaiting Gene's ratification"
---

# Sibling-Pair Edit Consistency Report

Status: **proposed (bridge, 2026-09-02); awaiting Gene's ratification**.

## LID checks

| Check | Method | Result |
|---|---|---|
| Coverage | Mapped all behavior-matrix rows to ten EARS families and falsifiers. | Pass: admission 001; table/shape 002; anchor 003; end 004; layout 005; CLJC 006; transaction 007; ordering 008; receipt 009; refusals 010. |
| Contradiction | Compared with HLD bookkeeping/fail-closed laws, current compact schemas/parser, refusal completeness, and disjoint-order decision. | Pass: one-form replacement is unchanged; new authority is bounded insertion only; same-boundary effects refuse; refusals never write. |
| Implicit scoping | Read each EARS statement without its heading and checked actor, entrance, trigger, result/refusal, and forbidden authority. | Pass: every requirement names clj-surgeon and carries its operative boundary; parent table/templates supply closed shared terms. |
| Context-free | Removed conversation history and checked field evidence, status, request shape, full table, refusal names, verification, and phase stop. | Pass: the four-document packet carries current intent independently. |

## Coverage ledger

| Observable contract | Requirement(s) | Matrix/falsifier evidence |
|---|---|---|
| Closed public object and one-form fields | 001 | malformed source, arity, aliases, unknown keys |
| Pair determination | 002 | all 11 tokens, widths 1/2, prefixes, defaults, excluded shapes |
| Before/after | 003 | zero/one/many lossless anchors and exact counts |
| End | 004 | zero/one/many containers, nonempty run, `case` default |
| Byte preservation and layout | 005 | copied gaps; comments, commas, discards, empty templates |
| CLJC/reader conditional | 006 | both views and every conditional ambiguity boundary |
| Snapshot and mutation safety | 007 | stale, parse, overlap, write, rollback, read-back, undo |
| Disjoint compact order | 008 | permutations and identical-boundary refusal |
| Success evidence | 009 | result/receipt provenance, hashes, inverse, no semantics claim |
| Refusal families and authority | 010 | every stable family, counts/evidence, unchanged bytes, no retry |

## Stable refusal ledger

| Family | Owning requirement |
|---|---|
| `unsupported-pair-context`, `invalid-pair-arity`, `invalid-pair-form` | 001–002 |
| `pair-container-not-found`, `ambiguous-pair-container` | 003–004 |
| `pair-anchor-not-found`, `pair-expect-count-mismatch` | 003 |
| `malformed-pair-run`, `unsupported-pair-shape` | 002 |
| `comment-sensitive-pair-boundary` | 005 |
| `pair-platform-required`, `invalid-pair-platform`, `reader-conditional-pair-ambiguous` | 006 |
| existing compact-location, overlap, snapshot, parse, verification, write, rollback families | 007–008 |

## Boundary findings

The smallest honest verb is `insert_pair`, not a relaxation of replacement.
Independent parsing of payload members retains the current `parse-one-form`
law and makes pair arity explicit. A closed table is required because syntactic
pair boundaries differ by head prefix, `case` default, and width-one threads.

Named owner plus anchor is compact and singular for the motivating case. End
addressing cannot be singular when several eligible containers share an owner,
so refusal is part of the contract rather than an ordinal or best-match rule.
The resulting limitation is visible and recoverable with an anchored address.

Exact preservation cannot coexist with invented formatting authority. Copying
an already-observed whitespace template is mechanical; comments, commas,
discards, empty runs, and reader conditionals therefore form refusal boundaries.
No existing byte is moved or rewritten.

The action compiles into the existing transaction rather than creating a new
writer. Canonical effect identity begins only after disjointness; identical
zero-width boundaries overlap by definition. Receipt additions are provenance,
not semantic proof.

CLJC needs an explicit platform view but still edits one physical span. Any
conditional ambiguity refuses. Refusal evidence is bounded and helpful but
inert under the ratified no-write-authority invariant.

Verdict: **the proposal covers its declared boundary and passes the four LID
consistency checks. It remains unratified design with no downstream authority.**
