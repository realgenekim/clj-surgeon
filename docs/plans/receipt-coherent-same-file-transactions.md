# Receipt-coherent same-file transactions

**Status:** Implemented
**Motivating issue/incidents:** `clj-surgeon-dij`; the 2026-08-10 self-dogfood
transaction that combined a namespace edit and two named-form edits in one file,
then refused with `invalid-transaction-receipt` after formatting collapsed those
logical edits into one exact-byte undo edit.

## Outcome

One `apply_clojure_changes` call may make several disjoint edits to several
owners in the same file, format that candidate once, commit it atomically, and
publish one valid reversible receipt. The success result continues to report the
number of logical matches requested by the caller. The receipt independently
reports the number of physical inverse edits needed to restore the original
bytes.

Every refusal states whether source is unchanged. Pre-write overlap and count
failures name the involved change IDs. A receipt-publication failure restores
the original files or returns an explicit recovery-required result.

## Bitter-Lesson Boundary

The model still chooses the owners, replacements, counts, and verification
level. Clj-surgeon owns only mechanical composition: canonical paths, snapshot
addresses, overlap detection, formatting, byte-exact rollback, and receipt
bookkeeping. This feature does not infer edits, merge conflicting intentions, or
weaken exact match guards.

## Public Contract

The existing `apply_clojure_changes` schema does not change. A request such as
one namespace-owner replacement plus two named-form replacements in the same
file succeeds when all three selections are exact and disjoint.

Success distinguishes:

- `edits`: logical matched changes supplied by the caller;
- `files`: changed canonical files;
- the durable receipt's `:inverse-edit-count`: physical inverse records.

New receipts always carry `:inverse-edit-count`. Existing version-1 receipts
without that field remain valid and interpret their inverse count as
`:match-count`. The existing `:match-count-kind :binding-occurrences` evidence
remains valid but no longer controls whether an inverse count may appear.

Unsupported overlap, ambiguity, stale source, parse failure, formatter failure,
and receipt failure remain fail-closed. Diagnostics expose stable change IDs and
ranges when two compiled edits conflict.

## Safety Invariants

1. Every selector resolves against one original snapshot; edits never cascade.
2. Each canonical file is compiled, formatted, written, read back, and listed in
   the receipt exactly once.
3. Logical match count and physical inverse edit count are separate facts.
4. Formatting may coalesce physical edits but may not alter caller-visible
   logical counts.
5. All exact edits are disjoint before any formatter or write runs.
6. Receipt validation proves its own hash, distinct canonical files, counts,
   complete inverse records, and exact guarded hashes.
7. Old receipts remain undoable.
8. Any returned `source_unchanged=true` is proven by no write or successful
   rollback; it is never inferred from error type alone after mutation begins.
9. Tests are only added or strengthened.

## Implementation Shape

Keep the existing compiler and formatter boundary:

1. `compile-transaction*` groups disjoint logical edits by canonical file.
2. `with-future-sources` may replace several file-local edits with one raw
   original-to-formatted edit.
3. `build-receipt` records both logical `:match-count` and the independently
   derived physical `:inverse-edit-count`.
4. `validate-receipt!` validates those facts independently and accepts legacy
   receipts that omit the new count.
5. MCP normalization preserves the logical `edits` result and publishes a
   compact, truthful refusal if receipt publication or rollback fails.

No second transaction compiler and no formatter-specific receipt schema are
introduced.

## Adversarial Review That Changed the Plan

The first draft proposed grouping same-file edits in the compiler. Inspection
showed that grouping, original-snapshot selection, overlap refusal, canonical
paths, and permutation invariance already exist. Adding another grouping layer
would duplicate a correct subsystem and hide the real defect.

The revised design addresses these attacks:

- **Formatter coalescing:** three logical edits may become one byte-exact inverse
  edit without invalidating the receipt.
- **No formatter change:** three logical edits remain three inverse edits and
  report the same public logical count.
- **Binding rename:** logical binding occurrences and physical inverse records
  remain independently checkable.
- **Legacy receipt:** a pre-change receipt without `:inverse-edit-count` still
  validates and undoes.
- **Tampered count:** changing either count without recomputing coherent receipt
  contents refuses.
- **Overlap disguised by owner type:** namespace and named-form edits that truly
  overlap refuse before formatting, with both change IDs.
- **Path aliases:** two spellings of one physical file still produce one file
  plan and receipt entry.
- **Order permutation:** changing request order does not change future bytes,
  receipt file grouping, or undo result.
- **Publication failure:** a valid commit followed by a receipt failure restores
  exact original bytes and reports the rollback truthfully.

Top-level insertion, extraction, semantic-provider recovery, and trace control
are deliberately excluded. They have separate public contracts and should not
ride inside a receipt-count repair.

## Test Plan

### Pure exhaustive matrix

- logical edit counts `1`, `2`, and `3+` crossed with physical inverse counts
  equal to and smaller than the logical count;
- formatted and unchanged candidates;
- one file and several files;
- namespace plus named forms, named forms only, and binding rename;
- request-order permutations and aliased canonical paths;
- legacy receipt, new receipt, and tampered logical/physical counts;
- equal, ancestor, descendant, and boundary-touching edit ranges.

### Field-failure regression

Use a small real Clojure fixture with one namespace form and two defns. Submit
the exact production-shaped MCP transaction with a formatter that changes the
candidate. Assert one commit, three logical edits, one changed file, a valid
receipt with one physical inverse edit, and byte-exact undo.

### Boundary tests

- public MCP success and concise output;
- public MCP overlap refusal names the conflicting changes and proves no write;
- receipt publication failure rolls back;
- source hashes before apply and after undo are identical;
- formatter runs once per canonical file.

## Documentation and Release Checklist

- Update the receipt contract in README/help/agent skill only if the new field is
  exposed to callers; do not make agents reason about physical inverse edits.
- Record the dogfood failure, cause, fix, and measured final route in the
  Captain's Log.
- Update `clj-surgeon-dij` with exact test and dogfood evidence.
- Run `make install` and hot-reload the shared MCP only after all gates pass.

## Verification Gates

1. Format only changed Clojure files.
2. Run focused pure transaction and MCP contract/tool tests.
3. Run MCP JVM and stdio suites.
4. Run clj-kondo with zero new diagnostics.
5. Run the full repository suite.
6. Dogfood the original namespace-plus-two-forms request through the hot MCP.
7. Apply the emitted undo receipt and prove the fixture hash is restored.

## Definition of Done

The production-shaped same-file transaction succeeds in one MCP call, reports
three logical edits and one changed file, emits a validator-approved receipt
whose physical inverse count matches its records, and restores the exact
original bytes through undo. All adversarial refusals remain pre-write or prove
rollback, legacy receipts still work, and every verification gate is green.
