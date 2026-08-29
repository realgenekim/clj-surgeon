# Owner-aware symbol-migration compiler receipt

**Date:** 2026-08-28

**Base:** `e9f8e100c839b4664505b45f15f7bb7d44eef9a3`

**Branch:** `experiment/owner-aware-symbol-migration`

**Decision:** the smallest pure payload compiler passed; retain it as experiment evidence, not product code

## Question

Can a grouped owner-aware symbol-migration table materially reduce model
materialization while compiling through the current product normalization and
transaction compiler to the exact frozen 51-change result?

The experiment was intentionally bounded:

- no public schema or product namespace changed;
- no install, reload, shared MCP call, source mutation, or model job ran;
- the compiler only expands a transport-neutral table into today's
  `edit_clojure` request;
- the current MCP validator, transaction conversion, and pure source compiler
  remain authoritative.

## Frozen inputs

The oracle is the current minified 6,409-byte request reconstructed from
`submission-row-extraction-cleanup`. It contains 33 exact edits plus one
14-owner deletion group.

The candidate retains the nine namespace replacements, one bespoke owner
replacement, and exact deletion group. It replaces only the 23 repeated
owner-scoped symbol-edit objects with one table that declares:

- `target_alias = submission-row`;
- `target_rule = preserve-name`;
- columns `owner`, `from`, and `matches`;
- all nine files, 23 owner rows, and 27 exact matches.

## Result

| Gate | Result |
|---|---:|
| Oracle payload | 6,409 bytes |
| Candidate payload | 4,403 bytes |
| Reduction | 2,006 bytes / 31.3% |
| Budget | at most 4,500 bytes — pass with 97 bytes headroom |
| Owner rows preserved | 23 / 23 |
| Declared matches preserved | 27 / 27 |
| Normalized transaction equal | true |
| Addressed replacements equal | true |
| Concrete matches | 51 |
| Changed files | 9 |
| Future hashes equal | true |
| Candidate hashes equal capsule after-hashes | true |
| Wrong owner | refused as `:change-owner-mismatch` |
| Wrong count | refused as `:expect-count-mismatch` |
| Model calls / mutation actions | 0 / 0 |

The compiler path for both oracle and candidate was:

```text
JSON normalization
  -> workspace_root removal at the execute-request! routing boundary
  -> mcp-contract/validate-tool-params
  -> mcp-contract/tool-params->transaction
  -> intent-transaction/compile-transaction against frozen before sources
```

No counts, addresses, source results, or hashes were mocked. The first probe
incorrectly passed `workspace_root` directly to the validator and both arms
refused `:invalid-mcp-request`. That red result was retained as a harness
finding. The correction reproduced the existing `execute-request!` boundary,
which removes the routed root before validation; no product behavior changed.

## Verification

```text
bb -cp src:test:dev/experiments \
  dev/experiments/owner_aware_symbol_migration_test.clj
  3 tests, 25 assertions, 0 failures, 0 errors

make test-fast
  636 tests, 5,467 assertions, 0 failures, 0 errors

make mcp-test
  operation oracle pass
  269 tests, 2,284 assertions, 0 failures, 0 errors
  heap, clj-kondo admission, analyzer, and cclsp self-tests pass

clj-kondo --lint <two experiment files>
  0 errors, 0 warnings

standard-clj check <two experiment files>
  both files formatted
```

An earlier ad hoc cold command accidentally invoked the complete MCP test alias
and observed two transient clj-kondo-admission assertions: it expected timeout
but found an already delegated admission. The canonical `make mcp-test` retry
passed unchanged. No failure was hidden or repaired in source.

## Immutable evidence

- Machine-readable result:
  `docs/observations/evidence/owner-aware-symbol-migration-e9f8e10.edn`
- Experiment source SHA-256:
  `c86d0876fcce0c9ae2b1e0ec4cc1a133121c3bdddda68ba3533e805225fca60e`
- Experiment test SHA-256:
  `e30e22628f1233b3da85718d7c22d4754914836bc4a4b8acb74e759d5428e3de`
- Successful report stdout SHA-256:
  `deef89aeaa109581cbc5f0e4c3ac4fcdc2e511f3d9dfd98563e896cd1d8c21d4`

## Decision and falsifier

The pure compiler hypothesis passes. It removes 31.3% of the complete payload
without hiding any owner or cardinality decision and without changing the
current transaction semantics.

The 97-byte budget headroom is deliberately uncomfortable. Do not promote the
prototype by adding public-schema prose around this exact encoding and assuming
the gate still holds. The next decision, if authorized, is a clean-context
call-construction screen comparing the existing request with this table. The
candidate loses if it adds a tool action or refusal, fails first-call exactness,
or does not reduce the observable call-emission interval despite its smaller
payload.
