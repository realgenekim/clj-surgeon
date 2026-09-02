---
parent: mcp-operation-contract-design
prefix: MCP-OP-PAIR-EDIT
status: "proposed (bridge, 2026-09-02); awaiting Gene's ratification"
---

# Sibling-Pair Edit

Status: **proposed (bridge, 2026-09-02); awaiting Gene's ratification**.
This packet is design authority only. It grants no test, code, installation,
reload, benchmark, or write authority.

## Context

On 2026-09-02 an agent needed to add one clause to `note-parts` in
`marvin-voice-remote/friction_ui.clj`:

```clojure
(cond-> parts
  note (conj note-part)
  audio (conj audio-part))
```

The desired addition was a test and expression, not one form. The compact
editor refused the two-form replacement as `invalid-intent-form` because a
replacement must contain exactly one complete form with no detached comments.
The agent described the gap as “No append verb for pair-structured macros” and
resubmitted the complete 20-line `cond->`, costing a model round-trip.
`assoc_entry` solves the same arity problem only for map literals.

The existing parser and transaction laws are sound: one-form replacement must
remain one-form replacement. The missing primitive is a narrow insertion that
owns one sibling unit without owning or reprinting its container.

## Boundary

This leaf adds one `insert_pair` action to each item of the compact `edits`
array accepted by `edit_clojure` and the compact branch of
`apply_clojure_changes`. It does not add an operator to generic `changes`, a
CLI spelling, a read operation, or a new executor.

An admitted item has this closed conceptual shape:

```json
{
  "file": "src/ui.clj",
  "within": {"form": "note-parts"},
  "insert_pair": {
    "context": "cond->",
    "forms": ["recording?", "(conj (recording-part state))"],
    "at": {"after": ["note", "(conj note-part)"]}
  },
  "matches": 1
}
```

`at` is exactly one of `before`, `after`, or `position`. `before` and `after`
contain the complete anchor unit as an ordered source-string vector.
`position` has the sole value `end`. `matches` remains the positive exact
cardinality guard and defaults to one under the existing compact contract.
`file` (not `files`) and `within.form` are required. For `.cljc`, `within` also
requires `platform: "clj"` or `"cljs"`; that field is invalid elsewhere.

The action is mutually exclusive with `from`/`to` and their aliases. It may be
batched with other disjoint compact actions under the existing frozen-snapshot
transaction, limits, parsing, overlap, atomic-write, read-back, verification,
rollback, and receipt laws.

## Closed context table

The caller names `context`; clj-surgeon does not infer macro semantics. It
finds physical candidate containers whose literal unqualified head equals the
named token, except `map`, which names a map literal. Qualified heads,
lookalike macros, constructor maps, destructuring semantics, and macroexpansion
are outside the table.

| Context | Container and sibling run | Unit width | Required structural precondition |
|---|---|---:|---|
| `cond` | list tail after head | 2 | tail count even |
| `cond->`, `cond->>` | list tail after head and seed | 2 | remaining count even |
| `condp` | list tail after head, predicate, expression | 2 | remaining count even; no three-form `:>>` clauses |
| `case` | list tail after head and dispatch expression | 2 | an odd tail treats its final form as default, outside the pair run |
| `let`, `loop`, `binding` | direct binding vector after head | 2 | vector count even; body is outside the run |
| `map` | direct children of a map literal | 2 | child count even |
| `->`, `->>`, `doto` | list tail after head and seed/object | 1 | every remaining child is one unit |

For `case`, `position:end` means after the last dispatch/body pair and before a
default form. An anchor may name only a dispatch/body pair, never the default.
`condp :>>` is refused rather than partially modeled. `letfn`, comprehensions,
`if`, `when`, arbitrary macro heads, set literals, map constructors, and nested
destructuring are not admitted.

## Address and insertion contract

Resolution is physical and lossless:

1. Resolve exactly one named top-level owner in the requested platform view.
2. Enumerate only table-eligible containers inside that owner.
3. For `before` or `after`, parse every payload and anchor item as exactly one
   complete non-comment form; retain containers containing exactly one
   lossless anchor unit. For `end`, retain eligible containers of `context`.
4. Require exactly `matches` retained insertion sites and require every site to
   have a mechanically reusable, comment-free separator template.
5. Lower each site to one zero-width atomic insertion against the frozen
   original source. Reparse the complete future file before write.

An anchor matches lossless form fingerprints, not evaluated equality. Its
ordered arity must equal the table width. Duplicate identical anchor units are
ambiguous unless the declared `matches` equals their exact count; each then
becomes an independently guarded insertion site. `matches` never permits two
different candidate containers to collapse into one selected site.

The inserted unit is constructed from the caller's exact form spellings plus
copied separator bytes. Width-two contexts copy the anchor's internal
comment-free gap; `end` copies the final unit's internal gap. The outer gap is
copied from the addressed boundary: the gap immediately before an anchor,
immediately after an anchor, or immediately before the container close/default
at end. Existing gap bytes are not moved, normalized, or deleted; a copy is
used for the new boundary. Width-one contexts need only the outer gap.

A missing template, empty pair run, zero-width gap, comma, comment, discard,
or other non-whitespace material in a required template refuses. This is a
deliberately smaller authority than pretty-printing: the verb reproduces
observed layout but never chooses layout. Comments elsewhere in the owner and
between unaffected peers remain byte-identical.

## CLJC and reader conditionals

`.cljc` requests must name one platform. Owner and candidate counts are taken
from that platform view, but edits target the one physical frozen source span.
A candidate container, anchor unit, pair boundary, or separator template that
crosses, contains, or is duplicated by a reader conditional refuses with
`reader-conditional-pair-ambiguous`. The operation never edits both branches,
merges platform results, or treats a conditional branch as an ordinary peer.

Ordinary `.clj` and `.cljs` requests reject `within.platform`. Reader
conditionals encountered there receive the same fail-closed treatment.

## Behavior matrix

The rows below are exhaustive for an admitted schema. Schema failures precede
source capture; all later refusals are pre-write.

| Context/state | Address | Result or stable refusal |
|---|---|---|
| supported width 2, one anchor | `before` | insert one unit before; preserve original bytes |
| supported width 2, one anchor | `after` | insert one unit after; preserve original bytes |
| supported width 2, valid nonempty run | `end` | append after pair run; before `case` default |
| supported width 1 | any address | insert one exact form using outer template |
| unknown/qualified context | any | `unsupported-pair-context` |
| wrong payload or anchor arity | any | `invalid-pair-arity` |
| payload/anchor item is not one complete form | any | `invalid-pair-form` |
| owner absent or duplicated | any | existing `compact-location-unresolved` |
| no eligible container | any | `pair-container-not-found` |
| several eligible end containers | `end` | `ambiguous-pair-container` |
| no lossless anchor unit | before/after | `pair-anchor-not-found` |
| retained anchors differ from `matches` | before/after | `pair-expect-count-mismatch` |
| malformed sibling run | any | `malformed-pair-run` |
| `condp` run contains `:>>` | any | `unsupported-pair-shape` |
| `case` anchor names default | before/after | `pair-anchor-not-found` |
| required layout template unavailable/unsafe | any | `comment-sensitive-pair-boundary` |
| CLJC platform absent/invalid | any | `pair-platform-required` / `invalid-pair-platform` |
| reader conditional affects proof | any | `reader-conditional-pair-ambiguous` |
| two edits resolve to same insertion boundary | any | existing overlap refusal, no ordering |
| source/hash/parse/verification/write failure | any | existing transaction refusal/rollback family |

`ambiguous-pair-container` is specific to `end`: anchored addressing reports
cardinality through `pair-expect-count-mismatch`. Every refusal returns exact
available/expected counts and bounded source-free candidate locations already
computed, subject to the ratified refusal-completeness envelope. It returns no
selected candidate, source body, replacement command, `next_call`, retry, or
write authority.

## Disjoint compact-edit order

An accepted `insert_pair` lowers to an ordinary guarded zero-width effect. It
joins canonical effect identity only after location, snapshot guard, and
complete disjointness proof. Disjoint insertions are permutation-invariant.
Two insertions at the same boundary, or an insertion intersecting replacement
or deletion authority, refuse every permutation. Caller order remains receipt
provenance and cannot order siblings. The order inside `forms` is explicit
payload authority and is never canonicalized.

## Receipt

The existing transaction receipt remains authoritative. Each logical intent
adds bounded provenance: `operator=insert-pair`, `context`, `width`, `position`,
`match-count`, and the lossless anchor fingerprint when anchored. Concrete
inverse edits record the exact inserted spans; file source/result hashes,
read-back proof, diff, receipt hash, and undo behavior are unchanged. Receipts
do not include owner source, inserted source bodies, inferred semantics, or a
claim that tests/guards are semantically valid.

## Decisions and alternatives

| Decision | Chosen | Alternatives and reason rejected/deferred |
|---|---|---|
| Public name | `insert_pair` | `conj_clause` implies collection semantics and excludes maps/bindings/thread steps. |
| Entrance | compact `edits` only | Generic `changes` duplicates a slower surface; a new tool duplicates transaction authority. |
| Address | named owner + closed context + anchor or end | Full-container `find` recreates the 20-line resubmission; ordinal indexes are fragile; fuzzy matching grants choice. |
| Unit model | bounded syntax table | Macroexpansion/inference crosses the bookkeeping boundary; caller-supplied width can misdescribe syntax. |
| Payload | ordered `forms` vector | One multi-form string conflicts with `parse-one-form`; named `test`/`expr` fields do not fit maps or threads. |
| Layout | copy proven comment-free gaps | Formatter choice widens authority; canonical whitespace destroys byte-locality; accepting comment gaps risks ownership changes. |
| Thread forms | width-one table rows | Included because the same peer insertion and layout proof applies without special semantics. |
| CLJC | mandatory platform, no conditional crossing | Editing all views or choosing a branch is ambiguous; banning all CLJC is unnecessarily broad. |
| `case` default | preserve at tail, outside pair run | Treating default as a pair makes the run malformed and prevents safe append. |
| `condp :>>` | refuse | Its clause arity is three and violates the sibling-pair contract. |

## Non-goals

- Reordering, replacing, deleting, deduplicating, or sorting existing units.
- Semantic checks of a condition, dispatch value, key, binding, or thread step.
- Duplicate-key detection, binding uniqueness, exhaustiveness, reachability, or
  macro validity beyond the bounded physical shape.
- Macroexpansion, qualified/custom macro support, generalized N-tuples, or
  inference from names or indentation.
- Comment movement, formatter selection, delimiter repair, or insertion where
  no safe layout template exists.
- Cross-owner insertion, ordinal addressing, cascading edits, or sequential
  observation inside one frozen transaction.
- A retry recipe, prepared write request, or any authority on refusal.

## Verification design

Red-first implementation, if separately authorized, must provide pure table,
parser, resolution, layout, and lowering tests; schema/normalization tests for
both compact entrances; transaction tests for snapshot, counts, disjointness,
parse, rollback, receipt, and undo; and protocol tests for public JSON names.

The matrix must cover every table row at before/after/end; multiline and
single-line separators; all refusal rows; duplicate anchors; several
containers; comments, commas, discards, metadata, Unicode, reader conditionals,
both CLJC platforms, `case` with/without default, and `condp :>>`. Every
refusal witness hashes source before and after and proves no receipt or write
authority. Accepted fixtures prove unrelated byte identity and exact inverse.
The motivating `note-parts` fixture must fail under the old one-form route and
succeed in one `insert_pair` call.

Permutation witnesses cover disjoint boundaries and same-boundary refusal.
One end-to-end `edit_clojure` call and one mixed `apply_clojure_changes` call
must exercise the public schemas and compact receipts. Normal repository style,
focused, lint, full-suite, `git diff --check`, and live-contract gates remain
mandatory after code is authorized.

## Ratification boundary

Ratification would approve the observable design and EARS registry only. All
requirements remain `[D]`. Tests, implementation, documentation outside this
leaf, schema reload, port 7888, installation, benchmarking, commit, and push
require later explicit authority. Refusal never grants mutation authority;
the first write can occur only after complete admission and existing frozen
transaction proof.
