---
parent: sibling-pair-edit-design
prefix: MCP-OP-PAIR-EDIT
status: "proposed (bridge, 2026-09-02); awaiting Gene's ratification"
---

# Sibling-Pair Edit EARS Specifications

Status: **proposed (bridge, 2026-09-02); awaiting Gene's ratification**.
Every ID is `[D]`; none authorizes tests or code.

## Requirements

- [D] **MCP-OP-PAIR-EDIT-001 — Admission.** When `edit_clojure` or the compact
  `edits` branch of `apply_clojure_changes` receives an `insert_pair` item,
  clj-surgeon shall require one project-relative `file`, one exact
  `within.form`, one closed `context`, an exact-arity nonempty `forms` vector,
  exactly one of an exact-arity `before` anchor, exact-arity `after` anchor, or
  `position=end`, and no replacement aliases or unknown fields; it shall parse
  each form independently as one complete non-comment form and refuse invalid
  input before write.

- [D] **MCP-OP-PAIR-EDIT-002 — Closed unit table.** When an admitted
  `insert_pair` item names `cond`, `cond->`, `cond->>`, `condp`, `case`, `let`,
  `loop`, `binding`, `map`, `->`, `->>`, or `doto`, clj-surgeon shall derive
  the container, skipped prefix, unit width, and pair-run boundary exactly from
  the parent design's closed table; it shall refuse every other context,
  qualified head, malformed run, `condp :>>` clause, constructor map, or
  caller-supplied/inferred width without mutation.

- [D] **MCP-OP-PAIR-EDIT-003 — Anchored resolution.** When an admitted
  `insert_pair` item uses `before` or `after`, clj-surgeon shall search only
  eligible containers inside the exact named owner and requested platform,
  compare the ordered anchor by lossless form fingerprint, and require the
  exact retained-site count declared by `matches`; zero, unexpected, or
  ambiguous results shall refuse without choosing a container or anchor.

- [D] **MCP-OP-PAIR-EDIT-004 — End resolution.** When an admitted
  `insert_pair` item uses `position=end`, clj-surgeon shall require exactly one
  eligible nonempty pair run inside the exact named owner and requested
  platform and shall address the boundary after its final unit, except that a
  `case` default shall remain after the insertion; zero or several eligible
  containers shall refuse without mutation.

- [D] **MCP-OP-PAIR-EDIT-005 — Lossless layout.** When one insertion site is
  resolved, clj-surgeon shall preserve every pre-existing source byte, retain
  each payload form's exact spelling, and construct only new separator bytes by
  copying the required existing comment-free whitespace templates defined by
  the parent design; a missing, empty, comma-bearing, comment-bearing,
  discard-bearing, reader-conditional, or otherwise non-whitespace template
  shall refuse before write.

- [D] **MCP-OP-PAIR-EDIT-006 — CLJC.** When `insert_pair` targets `.cljc`,
  clj-surgeon shall require exactly one `within.platform` value of `clj` or
  `cljs`, resolve counts in that view, and lower only an unambiguous physical
  span; when a reader conditional crosses, contains, or duplicates the owner,
  container, anchor, run, or template proof, clj-surgeon shall refuse
  `reader-conditional-pair-ambiguous` without editing either branch.

- [D] **MCP-OP-PAIR-EDIT-007 — Frozen transaction.** When every
  `insert_pair` site is admitted and resolved, clj-surgeon shall lower it to a
  guarded zero-width atomic effect against the one frozen snapshot, enforce
  existing positive count, resource, parse, overlap, atomic write, read-back,
  verification, rollback, and undo laws, and refuse stale or failed future
  state without recapture or partial commit.

- [D] **MCP-OP-PAIR-EDIT-008 — Disjoint order.** When several compact effects
  include `insert_pair`, clj-surgeon shall admit canonical effect identity only
  after exact location and complete disjointness proof; disjoint permutations
  shall have the same future identity, while two insertions at one boundary or
  an insertion intersecting another effect shall refuse every permutation,
  and caller order shall never grant sibling-placement authority.

- [D] **MCP-OP-PAIR-EDIT-009 — Receipt.** When an `insert_pair` transaction
  succeeds, clj-surgeon shall retain existing receipt, hash, read-back, inverse,
  and provenance laws and report bounded intent fields `operator=insert-pair`,
  `context`, `width`, `position`, `match-count`, and, for anchored edits, the
  lossless anchor fingerprint; it shall not report source bodies, semantic
  validation, or authority beyond the completed verified mutation.

- [D] **MCP-OP-PAIR-EDIT-010 — Refusal completeness.** When any
  `insert_pair` request refuses, clj-surgeon shall preserve the stable family
  named in the parent behavior matrix, return exact expected/available counts
  and bounded source-free candidate locations already computed under the
  ratified refusal-completeness envelope, set `source_unchanged=true`,
  `mutation_attempted=false`, and `write_authority=false`, and return no chosen
  candidate, source body, replacement, prepared request, `next_call`,
  executable retry, receipt, or inherited write authority.

## Falsifiers

| ID | Defensible opposite | Required witness |
|---|---|---|
| 001 | One two-form string or open objects are simpler. | detached comments, extra forms, aliases, unknown keys, wrong arities |
| 002 | Width can be inferred from any head. | every table row plus qualified/custom heads, odd runs, `condp :>>` |
| 003 | First matching anchor is sufficient. | zero/one/many anchors, duplicate containers, lossless inequality |
| 004 | End can choose the first matching container. | zero/one/many containers; `case` default retained |
| 005 | Canonical spaces or formatting are harmless. | byte proofs for multiline gaps; comments, commas, discards, empty gaps |
| 006 | Reader conditionals can be flattened or both branches edited. | clj/cljs views, shared physical node, crossed and duplicated branches |
| 007 | Insertion can bypass ordinary compilation. | stale hash, parse failure, write failure, rollback, exact undo |
| 008 | Caller order safely orders same-boundary insertions. | disjoint permutations; same boundary and insertion/deletion overlap |
| 009 | Ordinary counts need no operator provenance. | compact result and durable receipt fields; no source bodies |
| 010 | A unique candidate may become a retry. | every refusal family, one candidate, unchanged hash, no receipt/authority |

## Cross-family invariants

All requirements use literal syntax, never macroexpansion or semantic judgment.
The caller owns context, payload, anchor, position, owner, platform, and count;
clj-surgeon owns bounded shape, exact location, layout proof, frozen compilation,
and mechanical evidence. Existing bytes outside the zero-width effect remain
identical. Refusal precedes write or uses existing failure-atomic rollback,
never retries against newer source, and never converts evidence into authority.
No requirement authorizes reordering, dedupe, semantic checks, formatting,
tests, implementation, installation, reload, benchmark, commit, or push.
