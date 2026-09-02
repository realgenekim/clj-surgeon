---
parent: mcp-operation-contract-design
prefix: MCP-OP-INSERT
status: "Phase 2 revision approved by Mayor; implementation pending"
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

Subsequent controlled use found two defects in that repair. A named top-level
owner insertion refused whenever ordinary comments occupied the gap beside the
owner. The refusal was safe but not useful: it appeared in every treatment run,
and two of three agents escaped to `create_files` instead of repairing the
original file. More seriously, the anchor-indentation probe selected the first
blank line anywhere before a nested anchor. A source containing an earlier
blank line therefore returned `ok=true` while emitting the inserted form at
column zero. This is a silent wrong write, not a recoverable presentation
problem.

## Boundary

This leaf changes only transaction compilation for raw sibling insertions in
`intent_transaction.clj`: disjointness of compiled edits, owner-comment
boundaries, and selection of the whitespace inserted beside an anchor. It
preserves the existing request schema, selector semantics, parsing, formatting,
verification, atomic commit, receipt, undo, and error types.

There are two distinct anchor classes:

- A **named owner anchor** is a top-level insertion without `find`, admitted
  only when the caller names exactly one `forms` owner. Its owner identity and
  the top-level inventory's comment-attachment boundaries are exact
  transaction facts.
- A **subform anchor** is selected by exact `find` inside an owner. Comment or
  detached-source ownership between nested siblings is not mechanically known.

The named owner anchor plus mechanically attached top-level comments are
sufficient authority to place an insertion without reinterpreting those
comments. A leading comment block remains with the right owner; a same-line
trailing comment remains with the left owner. The insertion occupies the
boundary between those owned spans. Existing gap bytes remain in place and are
never copied into the newly emitted separator. A top-level gap containing
source that the existing attachment rules do not assign to either owner, or a
comment-bearing subform gap, remains ambiguous and refuses.

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
| Named owner anchor has comments mechanically attached to an adjacent top-level owner | Insert between the adjacent owned spans, preserve the complete existing gap once, and do not refuse solely because the gap contains comments. |
| Named owner anchor has detached source not assigned to either adjacent owner | Preserve the existing `ambiguous-insertion-gap` refusal. |
| Subform anchor gap contains comments or detached source | Preserve the existing `ambiguous-insertion-gap` refusal because comment ownership remains undecided. |
| Source has blank lines before a nested anchor | Derive indentation only from the anchor's own line; earlier lines cannot affect the separator. |
| Any refusal | Return the existing error type, grant no write authority, and expose no future source; persisted bytes remain unchanged. |

## Decisions and alternatives

An insertion is modeled as a zero-width boundary effect while retaining its
anchor owner as provenance. The owner is a special overlap exception only for
edits strictly inside it; equality with the owner span and containment by any
other span still overlap. This is narrower than treating all zero-width effects
as disjoint and safer than ordering coincident insertions by caller order.

Gap selection first distinguishes existing source from newly emitted
separation. A whitespace-and-comma same-side sibling gap may be reused verbatim
as the new separator. A mechanically owned comment gap is not reused: it
remains byte-for-byte with its existing owner, while the insertion receives a
fresh separator derived from local sibling style. This prevents comment
duplication and reassignment. Source in the gap that cannot be assigned by the
existing leading-block and same-line-trailing rules still refuses.

Only when the reusable same-side gap is empty does compilation inspect the
anchor's opposite sibling gap. A newline there is style evidence for newline
plus the anchor's existing indentation; otherwise a single space expresses the
established same-line relationship. Anchor indentation is the exact
whitespace-only prefix between the final newline before the anchor and the
anchor start. It is never found by a multiline search across earlier source.
A column-count heuristic was rejected because local sibling evidence is exact
and does not invent a project style threshold.

The original broad refusal for comment-bearing gaps protected a real invariant:
byte preservation alone does not preserve which sibling a comment appears to
describe. Removing that refusal for every exact `find` would transfer comment
ownership judgment into the kernel. Applying it to a named owner gap whose
comments are already mechanically attached, however, ignored authority the
caller and top-level owner inventory had supplied. The selected exemption is
therefore limited to the named owner path and only to comments with a proven
adjacent owner. Merely making the old refusal more actionable was considered
but rejected for this path because it would preserve an unnecessary extra
round trip on ordinary top-level source; refusal remains the safe behavior for
unowned or nested gaps.

## Non-goals

This leaf does not reorder coincident insertions, infer formatter policy,
reindent insertion payloads, normalize existing whitespace, infer nested
comment ownership, move or duplicate comments, change overlap behavior for
non-insert edits, or grant recovery/write authority after refusal. Whitespace
changes are confined to the newly inserted separator.
