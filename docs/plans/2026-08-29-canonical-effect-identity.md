# Canonical Effect Identity Plan

Date: 2026-08-29

Bead: `clj-surgeon-45j.1`

Approved chain: HLD -> LLD -> EARS -> red witnesses -> pure implementation ->
fresh causal cohort.

## Outcome

Two compact MCP requests that compile from one frozen snapshot to the same
complete set of exact, proven-disjoint concrete effects publish the same
canonical effect identity, independent of caller row order. Request evidence,
receipts, rollback, and diagnostics retain their existing order and hashes.

## Production seam

1. Add a pure `canonical-effect-identity` function to
   `clj-surgeon.intent-transaction`. It accepts the canonical project root and
   one successful compiled transaction. It reads no file and mutates no state.
2. Compute the identity in the compact MCP path after generic compilation and
   complete disjointness proof. Relation lowering uses no private path.
3. Copy the identity through the existing successful transaction result.
4. Project the closed `{version,sha256,files,effects}` object through
   `mcp-contract/normalize-success-receipt` only when compact identity evidence
   is present.
5. Preserve every existing receipt and inverse field byte-for-byte.

Expected production files:

- `src/clj_surgeon/intent_transaction.clj`
- `src/clj_surgeon/mcp_tool.clj`
- `src/clj_surgeon/mcp_contract.clj`

No schema change is required: the public MCP output schema is intentionally
open and already carries operation-specific terminal evidence. No request
field changes.

## Permanent witnesses

Pure transaction witnesses:

- all six permutations of three disjoint effects across two files;
- two disjoint subforms in one owner;
- identical replacement strings at different spans;
- insertion and deletion effects whose exact strings and addresses survive the
  projection;
- same-span, overlap, parent/nested, deletion-containing-edit, same-boundary
  insertion, and non-cascading sequential refusals produce no identity;
- all 24 permutations of a representative require/symbol/literal/delete set;
- same future and inverse result under accepted permutations while receipt
  hashes may differ.

MCP witnesses:

- normalized-flat and relation forms compile to the same canonical effect SHA,
  file count, effect count, future hashes, read-back hashes, and verifier;
- public identity omits source, replacement, absolute path, request IDs, and
  receipt identity;
- generic changes, programs, extraction, basis, planning, and CLI preserve
  their current surface and do not publish compact identity;
- a permuted request that is not provably disjoint refuses before mutation.

## Fast feedback

1. Format only changed Clojure files with the repository-configured formatter.
2. Reload changed namespaces in the standalone 512 MiB project nREPL and run
   the focused pure namespace tests.
3. Run focused cold tests for transaction, contract, tool, and compact
   relations once the pure seam is green.
4. Run the intent audit and full MCP/core suites only at the milestone.
5. Ask an independent lane to falsify public/internal identity drift and
   receipt-authority leakage before freezing the experiment candidate.

## Causal experiment

The held `b36d494` archive is never rescored. After the product and harness are
green, freeze a new candidate commit/tree, task, fixture, full client surface,
scorer, oracle, clock law, and thresholds. Run fresh serial `N R R N`. Only if
all four runs pass the predeclared correctness, route, canonical-effect,
`T_emit >= 20%`, and `T_complete_verified >= 15%` block-one laws, run fresh
`R N N R`. Final promotion keeps the unchanged 20% emission and complete-wall
laws and retains every post-token attempt.

## Rollback

Before publication, rollback is ordinary Git reversion of the pure identity
and projection commits. The mutation engine, receipt format, and saved receipts
are not migrated. After publication, old callers may ignore the additive result
field. Removing it requires first retiring its EARS and scorer consumers; no
receipt or source migration is involved.
