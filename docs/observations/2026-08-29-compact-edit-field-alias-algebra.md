# Compact edit field aliases need one closed algebra

Date: 2026-08-29

Lane: SURGEON2 isolated design/falsification

Product base: `24056d28fc42f071fb8948bc339a03e716eac4ef`

Product edits, model calls, installation, reload, and shared runtime changes: none

## Finding

The frozen four-run location cohort did not expose a second location problem.
It exposed one edit-field vocabulary problem three times.

All eight attempted calls describe the same 51-edit, nine-file decision. After
removing the optional `workspace_root` and lowering `old/new` or
`before/after` to `from/to`, all eight requests have one canonical semantic
SHA-256:

`a38392ed0b1791fdebb94440ed4edd4fb226a7480bee74c2dcdc2a233ec3144d`

The model got every file, location, replacement, count, and deletion right.
The server refused before write because the public edit item accepts only
`from/to`.

```text
complete structural decision
          |
          +-- old/new ----------> unknown-fields
          |                            |
          |                            +--> generic remedy names a hidden tool
          |
          +-- before/after -----> unknown-fields
          |
          +-- from/to ----------> 51 edits commit atomically
```

This is a good tolerance target. The relation is closed and injective. It does
not infer a file, owner, count, source subtree, or replacement.

## Immutable source evidence

Archive:

- `clj-surgeon-location-abba-20260829T060610Z.tar.gz`
- SHA-256 `d109fa0bef5c40a9cdb9313bfa5ff9e361258d338e9fd80c5ba92c8d81b5eded`
- durable capture manifest:
  `dev/experiments/edit_field_alias_capture_manifest.edn`

The reconstruction reads `events.jsonl` for every run and preserves the exact
argument hashes. `01-A` and `04-A` emitted the same root-omitting first call,
so eight calls contain seven distinct raw argument hashes.

| Run | Call | Pair | Root supplied | Result | Exact argument SHA-256 |
|---|---:|---|---:|---|---|
| 01-A | 1 | old/new | no | refused | `9071930b...fccf92` |
| 01-A | 2 | from/to | yes | committed | `4a9b5758...cdd69` |
| 02-B | 1 | old/new | yes | refused | `f4183255...dd49` |
| 02-B | 2 | before/after | yes | refused | `2eaf60e5...9328d` |
| 03-B | 1 | before/after | yes | refused | `529a7c8b...16b5` |
| 03-B | 2 | from/to | yes | committed | `4dadd3e9...c870e` |
| 04-A | 1 | old/new | no | refused | `9071930b...fccf92` |
| 04-A | 2 | from/to | yes | committed | `342e7138...680b3` |

Every call contains 33 literal edit items: nine namespace locations and 24
named-form locations. Literal match counts total 37. The owner-deletion item
names 14 owners. Therefore every correctly lowered request still asks for the
same 51 edits.

## Current refusal and remedy audit

The refusal is mechanically safe but operationally expensive.

- `mcp_schema/editor-gesture-schema` permits only `from` and `to`, requires
  both, and rejects additional fields.
- `mcp_contract/editor-gestures->direct-params` calls `validate-fields!` on an
  edit before reading its pair. `old/new` and `before/after` therefore fail as
  unknown fields before the canonical compiler can see the decision.
- `mcp_contract/refuse!` adds one generic remedy to every invalid request:
  `Correct the named field and call apply_clojure_changes once.`
- The cohort exposed only `edit_clojure`. The named fallback was not in the
  client registry. The remedy also does not say which fields to correct.
- Run `02-B` demonstrates the cost: after `old/new` refused, the caller chose
  another natural pair, `before/after`, and refused again. It never mutated.

The visible remedy should remain on the same public operation and state the
closed repair:

> Provide exactly one complete edit pair: from/to, old/new, or before/after;
> then call edit_clojure once. No source was changed.

## Proposed algebra

Canonical pair:

```clojure
["from" "to"]
```

Accepted alias pairs:

```clojure
["old" "new"]
["before" "after"]
```

For each edit item, intersect its keys with the six pair fields. Accept only
when that set equals exactly one complete pair. Lower the pair to `from/to`
before ordinary edit-field validation. Preserve every unrelated field. Emit
bounded non-authoritative normalization evidence. Then run the unchanged
location normalization, exact cardinality guards, frozen compiler, and atomic
writer.

```text
JSON edit
   |
   v
closed pair classifier
   |-- exactly one complete pair --> from/to
   |                                  |
   |                                  v
   |                         existing field validator
   |                                  |
   |                                  v
   |                         existing frozen compiler
   |
   `-- every other shape ----------> pre-write refusal
```

Do not compare duplicate pair values and choose one. Canonical plus alias is
duplicate mutation authority even when the values agree. Refuse it.

## Adversarial matrix

| Input shape | Result | Reason |
|---|---|---|
| `from` + `to` | accept unchanged | one canonical complete pair |
| `old` + `new` | lower to `from/to` | one complete closed alias |
| `before` + `after` | lower to `from/to` | one complete closed alias |
| no pair field | refuse | missing pair |
| any one field | refuse | partial pair |
| `old` + `to` or another cross-pair mix | refuse | mixed pairs |
| complete pair plus one field from another | refuse | mixed authority |
| two or three complete pairs | refuse | multiple authorities |
| canonical plus identical alias values | refuse | duplicate authority remains ambiguous |
| one bad item among good siblings | refuse whole batch | no partial normalized result or write |

The pure screen enumerates all 64 subsets of the six field names. Exactly
three subsets are accepted. The remaining 61 refuse with
`source_unchanged=true` and `write_authority=false`. The complete experiment
suite is 6 tests and 299 assertions.

Raw JSON can contain the same key twice, but ordinary JSON decoding collapses
that distinction before this algebra receives a map. Detecting repeated raw
keys would require a decoder-level policy. This slice treats duplicate
*semantic pairs* as its authority boundary and does not claim raw-key proof.

## Intent ownership

If this mechanism earns product implementation, it belongs in these existing
intent seams:

1. `docs/high-level-design.md`, compact editor section: add one source-blind,
   closed vocabulary normalization before source-aware location normalization.
2. `docs/intent/mcp-operation-contract/mcp-operation-contract-design.md`:
   add a sibling leaf after Injective Compact Location Normalization. Pair
   normalization changes spelling only; it grants no selector or write
   authority.
3. `docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md`:
   add new requirements after `MCP-OP-EDIT-016`; do not widen the existing
   location requirements.
4. `mcp_schema`, `mcp_contract`, and their direct tests: advertise the three
   closed pairs and lower before `validate-fields!`.
5. `mcp_tool` description and refusal summary: name the accepted pair spellings
   and keep the repair on `edit_clojure`.

The mechanism must not change direct `changes`, retained-basis changes,
extraction, computed programs, owner deletion, or CLI selector semantics.

## Product acceptance gates

1. All 64 field subsets pass the fail-closed matrix. Exactly three complete
   pairs accept.
2. Canonical, `old/new`, and `before/after` compile to identical generic
   transaction intent and exact future hashes on the frozen 51-edit capsule.
3. Mixed, partial, duplicate, conflicting, and canonical-plus-alias inputs
   refuse before write with the exact item path, no lowered siblings, no
   receipt, and `source_unchanged=true`.
4. The advertised MCP schema, SDK projection, and Codex registry expose the
   same pair algebra. Each accepted alias reaches the server unchanged.
5. The operation-specific remedy names `edit_clojure` and the exact pair law;
   it never names an unavailable fallback.
6. Throw-on-call witnesses prove generic changes, extraction, programs,
   deletion-only requests, and CLI routes never enter alias normalization.
7. A fresh serial counterbalanced Sol/high cohort preserves 51/9 correctness,
   removes first-call field refusals, and does not increase complete-task wall
   or action count relative to the accepted control.

## Recommendation

Proceed to Linked-Intent design for this bounded algebra. Do not add fuzzy
synonyms, automatic pair completion, or value-based conflict resolution. The
observed compatibility win comes from recognizing three exact complete pairs,
not from guessing what an incomplete edit meant.
