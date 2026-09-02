---
parent: insertion-boundary-and-gap-design
prefix: MCP-OP-INSERT
status: "Phase 3 revision approved for implementation by Mayor"
---

# Insertion Boundary and Gap Specifications

These IDs are stable and must not be reused if a requirement is deleted.

- [ ] **MCP-OP-INSERT-001**: When a zero-width `insert_before` or `insert_after` is anchored on an owner and every other edit is strictly inside that owner, clj-surgeon shall compile the effects as disjoint against one frozen source and shall apply both the insertion and every interior edit to the future source.

- [ ] **MCP-OP-INSERT-002**: When a boundary insertion shares its anchor with a replacement or deletion of the complete anchor owner, shares its exact boundary with another insertion, or lies at a boundary contained by a non-owner edit span, clj-surgeon shall refuse with the existing `overlapping-intents` error type and shall not produce future source or write authority.

- [ ] **MCP-OP-INSERT-003**: When a sibling insertion has a non-empty accepted same-side gap containing only whitespace and optional commas, clj-surgeon shall preserve that gap verbatim and shall confine new whitespace to the inserted gap.

- [ ] **MCP-OP-INSERT-004**: When a sibling insertion has no same-side gap and the anchor's opposite sibling gap contains a newline, clj-surgeon shall separate the insertion with one newline followed by the anchor's existing indentation, regardless of the anchor's one-line width.

- [ ] **MCP-OP-INSERT-005**: When a sibling insertion has no same-side gap and the anchor and its opposite sibling genuinely share a line, clj-surgeon shall use one space as the insertion gap.

- [ ] **MCP-OP-INSERT-006**: When overlap or insertion-gap validation refuses a transaction, clj-surgeon shall preserve the existing refusal type, shall grant no write authority, and shall leave persisted source bytes unchanged.

- [ ] **MCP-OP-INSERT-007**: When `insert_before` or `insert_after` names exactly one top-level `forms` owner without `find` and comments in the adjacent sibling gap are mechanically attached to a top-level owner, clj-surgeon shall insert between the adjacent owned spans, shall keep a leading comment block with the right owner and a same-line trailing comment with the left owner, and shall preserve every existing gap byte exactly once.

- [ ] **MCP-OP-INSERT-008**: When a named top-level owner insertion accepts a mechanically owned comment gap, clj-surgeon shall derive a fresh separator from local sibling style and shall not copy comments or detached source into that separator.

- [ ] **MCP-OP-INSERT-009**: When a sibling insertion uses a subform anchor whose gap contains comments or detached source, or a named top-level owner gap contains source that the top-level attachment rules assign to neither adjacent owner, clj-surgeon shall refuse with `ambiguous-insertion-gap`, shall grant no write authority, and shall produce no future source.

- [ ] **MCP-OP-INSERT-010**: When a sibling insertion uses newline-based local style, clj-surgeon shall derive indentation from the exact whitespace-only prefix on the anchor's own line, and blank or indented lines anywhere earlier in the source shall not affect the emitted separator.

## Falsifiers

| Requirement | Falsifying observation |
|---|---|
| MCP-OP-INSERT-001 | An owner-boundary insertion is refused solely because a replacement is strictly inside that owner, or an accepted future source omits either effect. |
| MCP-OP-INSERT-002 | Whole-owner replacement/deletion, coincident insertion, or non-owner boundary containment compiles successfully. |
| MCP-OP-INSERT-003 | A non-empty accepted same-side whitespace/comma gap changes, or unrelated whitespace moves. |
| MCP-OP-INSERT-004 | An empty same-side gap beside an anchor whose opposite gap is newline-based produces a space, copies unrelated indentation, or depends on a column threshold. |
| MCP-OP-INSERT-005 | Genuine same-line siblings receive a newline when no newline-based sibling evidence exists. |
| MCP-OP-INSERT-006 | A refusal changes its stable error type, returns future source or write authority, or changes persisted bytes. |
| MCP-OP-INSERT-007 | A named owner insertion refuses solely because an adjacent comment is mechanically owned, moves a leading block away from the right owner, moves a same-line trailing comment away from the left owner, or duplicates an existing gap byte. |
| MCP-OP-INSERT-008 | An accepted owner insertion copies a comment or detached source into newly emitted separation instead of deriving a comment-free separator from local style. |
| MCP-OP-INSERT-009 | A nested comment-bearing gap or an unowned top-level gap compiles successfully, returns future source, or grants write authority. |
| MCP-OP-INSERT-010 | An earlier blank or indented line changes the separator for an otherwise identical anchor, or an accepted newline-based insertion does not align with the anchor's own indentation. |
