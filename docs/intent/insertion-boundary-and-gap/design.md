---
parent: mcp-operation-contract-design
prefix: MCP-OP-INSERT
status: "approved LLD by bridge lead under Gene's 2026-09-02 authorization; awaiting Gene's ratification"
---

# Insertion Boundary and Gap

## Context

Two retained `apply_clojure_changes` field failures made one coherent sibling
insertion unnecessarily expensive. First, a top-level `insert_before` was
classified as overlapping six replacements strictly inside its anchor owner,
although the insertion is a zero-width effect at the owner's boundary. Second,
an `insert_after` beside a long one-line string copied an empty same-side gap
and fell back to one space even though the opposite sibling gap established the
surrounding newline-and-indent style. Each refusal or poor layout cost another
model/tool round trip.

## Boundary

This leaf changes only transaction compilation for raw sibling insertions in
`intent_transaction.clj`: disjointness of compiled edits and selection of the
whitespace inserted beside an anchor. It preserves the existing request
schema, selector semantics, parsing, formatting, verification, atomic commit,
receipt, undo, and error types.

## Behavior matrix

| Situation | Required result |
|---|---|
| Boundary insertion plus edits strictly inside its anchor owner | Compile as disjoint and apply all effects against the frozen source. |
| Boundary insertion plus replacement or deletion of the anchor owner | Refuse as `overlapping-intents`. |
| Two insertions at the same boundary | Refuse as `overlapping-intents`; row order grants no placement authority. |
| Boundary insertion plus a non-owner span containing the boundary | Refuse as `overlapping-intents`. |
| Accepted same-side sibling whitespace/comma gap exists | Preserve that gap verbatim. |
| Same-side gap is absent and the opposite sibling gap is newline-based | Use one newline followed by the anchor's indentation. |
| Same-side gap is absent and siblings genuinely share a line | Use one space. |
| Candidate gap contains comments or detached source | Preserve the existing `ambiguous-insertion-gap` refusal. |
| Any refusal | Return the existing error type, grant no write authority, and expose no future source; persisted bytes remain unchanged. |

## Decisions and alternatives

An insertion is modeled as a zero-width boundary effect while retaining its
anchor owner as provenance. The owner is a special overlap exception only for
edits strictly inside it; equality with the owner span and containment by any
other span still overlap. This is narrower than treating all zero-width effects
as disjoint and safer than ordering coincident insertions by caller order.

Gap selection first preserves a valid same-side sibling gap. Only when that gap
is empty does it inspect the anchor's opposite sibling gap. A newline there is
style evidence for newline plus the anchor's existing indentation; otherwise a
single space expresses the established same-line relationship. A column-count
heuristic was rejected because local sibling evidence is exact and does not
invent a project style threshold.

## Non-goals

This leaf does not reorder coincident insertions, infer formatter policy,
reindent insertion payloads, normalize existing whitespace, move comments,
change overlap behavior for non-insert edits, or grant recovery/write authority
after refusal. Whitespace changes are confined to the newly inserted gap.
