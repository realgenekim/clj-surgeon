---
parent: insertion-boundary-and-gap-design
prefix: MCP-OP-INSERT
status: "approved LLD by bridge lead under Gene's 2026-09-02 authorization; awaiting Gene's ratification"
---

# Insertion Boundary and Gap Specifications

These IDs are stable and must not be reused if a requirement is deleted.

- [ ] **MCP-OP-INSERT-001**: When a zero-width `insert_before` or `insert_after` is anchored on an owner and every other edit is strictly inside that owner, clj-surgeon shall compile the effects as disjoint against one frozen source and shall apply both the insertion and every interior edit to the future source.

- [ ] **MCP-OP-INSERT-002**: When a boundary insertion shares its anchor with a replacement or deletion of the complete anchor owner, shares its exact boundary with another insertion, or lies at a boundary contained by a non-owner edit span, clj-surgeon shall refuse with the existing `overlapping-intents` error type and shall not produce future source or write authority.

- [ ] **MCP-OP-INSERT-003**: When a sibling insertion has a non-empty accepted same-side gap containing only whitespace and optional commas, clj-surgeon shall preserve that gap verbatim and shall confine new whitespace to the inserted gap.

- [ ] **MCP-OP-INSERT-004**: When a sibling insertion has no same-side gap and the anchor's opposite sibling gap contains a newline, clj-surgeon shall separate the insertion with one newline followed by the anchor's existing indentation, regardless of the anchor's one-line width.

- [ ] **MCP-OP-INSERT-005**: When a sibling insertion has no same-side gap and the anchor and its opposite sibling genuinely share a line, clj-surgeon shall use one space as the insertion gap.

- [ ] **MCP-OP-INSERT-006**: When overlap or insertion-gap validation refuses a transaction, clj-surgeon shall preserve the existing refusal type, shall grant no write authority, and shall leave persisted source bytes unchanged.

## Falsifiers

| Requirement | Falsifying observation |
|---|---|
| MCP-OP-INSERT-001 | An owner-boundary insertion is refused solely because a replacement is strictly inside that owner, or an accepted future source omits either effect. |
| MCP-OP-INSERT-002 | Whole-owner replacement/deletion, coincident insertion, or non-owner boundary containment compiles successfully. |
| MCP-OP-INSERT-003 | A non-empty accepted same-side whitespace/comma gap changes, or unrelated whitespace moves. |
| MCP-OP-INSERT-004 | An empty same-side gap beside an anchor whose opposite gap is newline-based produces a space, copies unrelated indentation, or depends on a column threshold. |
| MCP-OP-INSERT-005 | Genuine same-line siblings receive a newline when no newline-based sibling evidence exists. |
| MCP-OP-INSERT-006 | A refusal changes its stable error type, returns future source or write authority, or changes persisted bytes. |
