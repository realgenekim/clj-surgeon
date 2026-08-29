# Decision: Disjoint Compact Edit Order

Date: 2026-08-29

Bead: `clj-surgeon-45j.1`

Status: proposed HLD decision; no EARS, tests, code, or benchmark authority yet

## Decision

Two compact requests that resolve against one frozen source map to the same
multiset of exact, disjoint atomic effects describe the same semantic mutation,
regardless of caller row order.

Caller order remains immutable provenance. It continues to own public request
indexes, relation row indexes, diagnostic paths, and the submitted request
hash. A new canonical effect identity represents successful mutation
equivalence only after the existing compiler proves exact guards and
disjointness. It does not replace or rewrite request evidence.

This is a dual contract:

```text
submitted request order
  -> provenance, diagnostics, exact request replay

resolved disjoint effect order
  -> semantic equivalence, future-state identity, causal cohort admission
```

## Why this matches kernel semantics

The current compiler already behaves as if disjoint effects are unordered:

1. Every intent resolves against the same original source map. No edit observes
   another edit's result.
2. Atomic edits are grouped by canonical file.
3. Each file's edits are sorted by original source address.
4. Intersecting source spans refuse before a future source is produced.
5. Accepted edits are applied in descending address order so earlier
   replacements cannot shift later addresses.
6. Successful future bytes and read-back hashes therefore depend on the
   resolved effects, not on their submitted row order.

Today the submitted vector order still leaks into synthetic `edit-N` IDs,
intent indexes, first-seen file order, diff order, receipt vectors, and receipt
hashes. The held relation cohort exposed that representational difference: N1
and N2 had the same sorted request-edit multiset and exact future hashes, while
their precompiled transaction specs had different vector order and hashes.

The owning seam is the existing transaction compiler, not a relation-specific
normalizer. `compile-transaction*` resolves the full request against one source
map; `compile-file` and `assert-disjoint-edits!` establish resolved address
order and non-overlap; `apply-edits` consumes those accepted effects in reverse
address order. The new projection belongs after that proof and before evidence
is compared. Relation lowering remains only a producer of ordinary compact
edits.

## Canonical effect projection

The projection is derived only after public admission, canonical path
confinement, relation lowering against frozen source, compact-location
normalization, generic intent compilation, and complete overlap proof.

Each effect key contains:

1. canonical project-relative file;
2. resolved half-open original-source span or structural address;
3. operation class;
4. lossless before identity; and
5. lossless after identity.

The complete projection also contains canonical source and result hashes plus
logical file, intent, and edit counts. It excludes:

- synthetic IDs and request indexes;
- relation file and row declaration indexes;
- diff concatenation order;
- receipt path and receipt hash; and
- transient write or recovery order.

The ordinary receipt retains all existing provenance. Different submitted
orders may continue to produce different receipt bytes and hashes. Every such
receipt must still undo its own verified result to the same original hashes.

## What order still means

| Case | Law |
|---|---|
| Disjoint files or disjoint spans | Canonical effect identity is permutation-invariant. |
| Same owner, disjoint exact subforms | Permutation-invariant after resolved-span proof. |
| Same replacement text at different spans | Permutation-invariant; the span distinguishes the effects. |
| Same or intersecting span | Refuse every permutation; never deduplicate. |
| Parent replacement plus nested replacement | Refuse every permutation. |
| Owner deletion plus edit inside that owner | Refuse every permutation. |
| Edit plus deletion of another owner | Permutation-invariant. |
| Two insertions at the same boundary | Refuse; caller order cannot grant sibling-placement authority. |
| Several forms inside one insertion payload | Preserve payload order as explicit authority. |
| `old -> middle` plus `middle -> new` | Both compile against original source; a missing guard refuses. Use one composed replacement or sequential transactions. |
| Namespace edit plus disjoint owner edit | Permutation-invariant. |
| Whole namespace replacement plus nested require edit | Refuse every permutation. |
| Relation migration and require file vectors | Preserve indexed pairing through admission; canonicalize only resolved effects. |
| Generic caller-ID-bearing `changes` | Outside the first slice; preserve current order and identity. |
| Programs, extraction, and retained basis | Outside the first slice. |

## Alternatives

### Keep request order as semantic authority

Rejected. The executor already reorders concrete edits by source address, and
the frozen fixture proved identical final bytes under a row permutation. This
alternative would preserve an accidental evidence distinction that the
mutation kernel does not observe.

### Sort submitted compact rows before resolution

Rejected. Before source capture and exact resolution, the system has not proved
that two rows are disjoint. Early sorting would also overwrite request-owned
diagnostic indexes and make provenance harder to reconstruct.

### Rewrite the transaction engine and receipt order

Rejected for the first slice. The executor's address ordering, commit staging,
rollback, verification, and receipt behavior already satisfy the mutation law.
Changing them increases risk without being necessary to express semantic
equivalence.

### Derive a separate post-compilation effect identity

Chosen. It adds one pure projection at the point where exact spans and
disjointness are already proved, while preserving every existing effect and
provenance path.

## Required falsification matrix

Before implementation can be accepted, permanent pure witnesses must cover:

1. all six permutations of three effects spanning two files and two owners;
2. both permutations of two disjoint subforms in one owner;
3. equal replacement payloads at different spans;
4. duplicate exact spans and parent/nested overlaps;
5. a non-cascading `old -> middle`, `middle -> new` dependency;
6. owner deletion with inside and outside edits;
7. namespace edits with disjoint and nested owner edits;
8. all 24 permutations of representative generated-require,
   generated-symbol, literal, and owner-deletion effects; and
9. inverse receipts restoring the same originals for every accepted
   permutation, without requiring equal receipt hashes.

Every refused case must produce no future source and no write authority.
Formatters are outside the first compact-edit slice. Any later
formatter-inclusive claim must bind one exact deterministic formatter and
compare post-format hashes separately.

## Experiment consequence

The retained `b36d494` cohort remains an invalid but informative HOLD. This
decision cannot turn it into a pass. A changed authority projection changes the
candidate, oracle, and scorer law.

After the LID chain is approved and implemented:

1. freeze the new candidate, source tree, prompt, public surface, oracle, and
   scorer;
2. run a fresh complete `N R R N` Block 1 in isolated workspaces;
3. require 4/4 one-shot correctness, representation adherence, exact
   verification, canonical effect identity, future hashes, and read-back
   hashes before considering timing; and
4. authorize fresh `R N N R` only under the unchanged emission and verified-wall
   stop laws.

Never rerun N1 alone, splice old and new blocks, or rescore the held cohort into
a win.

## Next LID step

If this HLD decision is approved, write the owning MCP operation-contract LLD
and EARS requirements for the pure projection and refusal matrix. Stop again
before red tests.
