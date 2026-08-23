# Functional-Core Test Architecture

**Status:** Accepted design
**Motivating issue:** `clj-surgeon-i6n`; related field issues
`clj-surgeon-ss5` and `clj-surgeon-ws5`

## Outcome

Every combinatorial clj-surgeon behavior is decided by a public pure function
over source strings and ordinary data. Filesystem, subprocess, MCP, formatter,
linter, clock, and language-server code becomes a thin shell that captures one
snapshot, calls the pure core, executes the returned program, and reports a
verified receipt.

The suite becomes faster and more exhaustive without deleting boundary truth.
Existing assertions are retained or strengthened. Each real external boundary
keeps a small success/failure matrix for behavior that a pure model cannot
prove.

## Bitter-Lesson Boundary

The kernel compiles mechanics that the caller has already decided:

- future source and destination namespaces;
- exact edits, insertions, deletions, and write order;
- dependency and diagnostic deltas;
- snapshot guards, rollback data, verification steps, and receipts.

It does not select an architecture, invent an extraction boundary, decide
whether an ambiguous side-effect require is necessary, or infer which warnings
are acceptable. Those remain explicit caller or repository decisions.

Purity is not the goal when it would simulate away the fact under test. Real
symlinks, atomic replacement, subprocess exit behavior, MCP publication,
formatter/linter execution, and clojure-lsp protocol behavior remain real
boundary tests.

## Public Contract

### Pure compilation

The functional core accepts immutable input:

```clojure
{:snapshots {"src/app.clj" {:source "..." :sha256 "..."}}
 :intent ...
 :diagnostic-baseline ...
 :policy ...}
```

It returns either one complete program:

```clojure
{:ok true
 :future-files {"src/app.clj" "..."
                "src/app/render.clj" "..."}
 :edits [...]
 :write-order [...]
 :verification-plan ...
 :receipt-basis ...}
```

or one stable refusal:

```clojure
{:ok false
 :error-type :stable-keyword
 :source-unchanged true
 :remedy "One executable correction"}
```

Pure compilation performs no reads, writes, clock calls, subprocesses, network
requests, registry mutation, or receipt publication.

### Diagnostic delta

Fast verification compares normalized diagnostic sets captured by the same
configured verifier. It accepts unchanged or removed baseline diagnostics and
refuses only diagnostics introduced by the candidate. Full verification
remains an explicit absolute gate.

The verifier analyzes all changed future namespaces as one staged snapshot.
It must not ask a stale workspace index to discover a Var that the same
transaction has just introduced, and it must not require the caller to save,
format, or hot-load a callee before its caller. Ordinary `defn` and
project-macro-defined Var introductions have the same contract.

Baseline capture failure refuses before the first write. A verification
receipt reports counts and bounded new diagnostics without dumping unrelated
baseline output.

### Retained change bases

Basis preparation is two operations with separate authority:

1. a pure function turns one source/semantic snapshot into ordered sites,
   visibility, budgets, and the next decision call;
2. a store adapter applies TTL, capacity, lookup, and deletion.

The pure result is independent of wall-clock time, global atoms, repository
files, and insertion order. Storage failure retains no partially published
basis.

## Safety Invariants

- Refusal never changes a source byte, target path, basis store, or receipt.
- Intent order cannot change the compiled future files.
- Unselected bytes remain byte-identical.
- Every successful future file parses before the first write.
- Exact source and result hashes agree with the committed read-back bytes.
- Undo reconstructs the complete original snapshot or refuses on drift.
- Pre-existing diagnostics cannot become transaction failures in fast mode.
- New diagnostics cannot be hidden by baseline noise.
- Pure and boundary tests use the same compiler output; integration tests do
  not reimplement planning.
- Test strength never decreases: moved assertions remain, and every removed
  effectful case is replaced by equal or stronger pure proof plus the retained
  distinct boundary contract.

## Implementation Shape

### 1. Extraction compiler

Extend the existing pure extraction header and candidate builders into one
public compiler over captured source data. It produces the source candidate,
destination candidate, declared caller rewrites, dependency evidence,
verification plan, and inverse basis. `extract/plan`, `extract/execute!`, and
the MCP transaction route become adapters over that result.

The compiler supports an explicit conservative first move:

```clojure
{:require-policy :copy-all}
```

This copies the source namespace's existing require surface exactly into the
new namespace and reports the copied count. It does not claim dependency
minimality. The mechanically safe move and dependency cleanup are separate
transactions. `:minimal` retains the existing proved dependency compiler;
ambiguous side-effect-only requires refuse there instead of forcing the model
to assemble a header by hand. Both policies still parse and verify the complete
future namespace set and refuse cycles, alias collisions, stale snapshots, or
invalid candidates.

This completes rather than replaces
[Dependency-minimal namespace extraction](dependency-minimal-extraction.md)
and [Failure-atomic namespace extraction](failure-atomic-extraction.md).

### 2. Diagnostic compiler

Add a dependency-free diagnostic namespace that normalizes tool diagnostics
to stable identities and computes baseline, future, introduced, removed, and
unchanged sets. Verification adapters capture raw results and delegate all
policy to this namespace.

### 3. Change-basis compiler

Extract basis construction, site ordering, visibility selection, budget
checks, and decision-call construction from `mcp-change-buffer`. Inject the
captured time into the small store adapter; keep no implicit clock access in
the compiler.

### 4. Test strangler

Move behavioral matrices in `extract-test`, `intent-transaction-test`, and
`mcp-change-buffer-test` toward source literals and memory I/O. Keep a named
integration section in each namespace for genuinely external contracts.
Mechanical setup shared by tests becomes a fixture builder over data rather
than a larger testing framework.

### 5. Generative invariants

Generate bounded valid source programs from explicit dimensions: layout,
comments, metadata, adjacent owners, duplicate forms, intent order, dependency
shape, edit operator, and refusal cause. Use deterministic seeds and print the
smallest replayable input on failure. Do not add an unbounded or flaky property
runner to the default suite.

## Test Plan

### Pure behavior matrix

| Area | Required cases |
|---|---|
| Extraction | empty/single/many owners; adjacent/disjoint owners; comments; metadata; dependency-free/direct/transitive/shared dependencies; caller/no caller; exact `:copy-all` require preservation; `:minimal` proof; explicit side-effect retention/refusal; alias collision; dependency cycle; existing target; invalid future source |
| Diagnostics | clean/clean; warning retained; warning removed; one warning added; error added; duplicates; path normalization; order independence; malformed tool output; baseline capture refusal; new cross-namespace `defn`; new project-macro Var plus caller in the same future snapshot |
| Basis preparation | zero/one/many sites; definition/surface scopes; repeated files read once; stable IDs; visibility overflow; snapshot overflow; ambiguous owner; mixed semantic session; stale hashes; deterministic next call |
| Transactions | replace/insert/delete/rename/assoc; overlapping edits; order permutations; comments and reader shorthand; stale snapshots; parse refusal; inverse round-trip |

### Generative properties

- compile is deterministic;
- permutation of independent intents preserves future files and hashes;
- compile followed by inverse returns the starting snapshot;
- any refused compilation returns no executable write program;
- successful edits preserve every byte outside their declared ranges;
- diagnostic delta is order-independent and partitions the normalized union;
- site IDs and decision order depend on structural order, not map iteration.

### Boundary contracts retained

- real filesystem atomic replacement, permissions, symlinks, and rollback;
- real source/target creation and exact undo;
- real CLI stdin, EDN output, exit codes, and documented commands;
- real MCP schema publication, hot reload, HTTP/stdio transport, and compact
  terminal receipts;
- real formatter, clj-kondo, and cold Clojure load;
- coherent staged multi-namespace lint that does not consult a stale workspace
  index or require save ordering;
- real cclsp/clojure-lsp initialization, client callbacks, cancellation, and
  bounded timeout.

Each boundary test must name the external fact it proves. It must not duplicate
the complete pure behavior matrix through temporary projects.

### Baseline and non-regression evidence

Record before and after:

- full and focused test/assertion counts;
- elapsed time for pure focused suites and the complete suite;
- test namespaces and individual tests that cross each external boundary;
- assertions migrated from temporary projects to source/data values;
- retained integration contracts.

Counts are evidence, not optimization targets. Completion may increase tests
and assertions; it must not reduce covered behavior.

## Documentation and Release Checklist

- Link this plan from the plans index.
- Document the functional-core/effect-shell boundary in `CLAUDE.md` and the
  testing guide using the implemented public functions.
- Update MCP/CLI help, README, and both agent skills for any public extraction
  or verification surface.
- Record the before/after evidence and surprises in a Captain's Log.
- Run `make mcp-reload` after tool-contract changes and `make install` after
  the complete installed surface passes.

## Verification Gates

1. Preserve the green baseline of 634 tests and 5,412 assertions.
2. New pure tests fail against the missing seams before implementation.
3. Focused pure tests pass in the persistent nREPL.
4. Every retained external boundary has a focused integration test.
5. Changed Clojure files are formatted before lint and tests.
6. clj-kondo reports zero errors and warnings for changed namespaces.
7. `make mcp-test` and `make test` pass with no weakened assertions.
8. cclsp typecheck, formatting/lint, and tests pass if its code changes.
9. One real extraction and diagnostic-delta transaction succeeds, verifies,
   and undoes exactly.
10. A clean-context agent expresses the complete extraction once and receives
    one verified, reversible transaction.

## Definition of Done

`clj-surgeon-i6n` is complete when all combinatorial extraction, diagnostic,
basis, and transaction behavior is proved over immutable data; each external
boundary is covered by a minimal distinct real test; no prior assertion or
safety contract is weakened; the complete suite and lint gates are green; and
one production-shaped extraction is compiled, applied, verified, and undone
without the caller carrying intermediate mechanical state.

## Completion evidence

- Pure compilers now own diagnostic deltas, extraction planning, retained-basis
  construction, quoted Var scans, and transaction permutation/inverse proofs.
- The main suite grew from 634 tests / 5,412 assertions to 642 tests / 5,537
  assertions before the final formatter-stability regressions. No assertion or
  boundary test was removed.
- The MCP suite passed 141 tests / 1,165 assertions.
- Changed-file clj-kondo: 0 errors, 0 warnings.
- Ten repeated compilations over one captured 175-file workspace were 1.92x
  faster than ten repeated filesystem-shell plans (3,445.5 ms versus 6,600.4
  ms).
- The installed CLI planned, applied, verified, and exactly undid a
  production-shaped `:copy-all` extraction.
- A clean caller independently selected `:copy-all`. Its formatter encounter
  exposed two generator trivia bytes; the generator was corrected and the
  permanent proof now verifies that formatting changes neither result hash and
  the receipt remains executable.
- Full narrative and measurements:
  [Purity Made the Safety Net Denser](../observations/2026-08-10-captains-log-purity-made-the-safety-net-denser.md).
