# Captain's Log: Purity Made the Safety Net Denser

**Date:** 2026-08-10

## Question

Would a functional-core/effect-shell refactor make clj-surgeon easier to
change, faster to test, and safer without weakening the real integration
proof?

## Verdict

Yes. The important gain is not that every individual assertion became faster.
The gain is that decisions which previously required a filesystem, process,
clock, UUID generator, or mutable basis registry can now be exercised as plain
data transformations. That makes larger state matrices affordable while a
smaller set of unchanged integration tests continues to prove the real
boundaries.

The test suite was already exceptional. During this refactor it caught three
different classes of defect:

1. a workspace adapter changed quoted-reference paths from absolute to
   relative, breaking the join with semantic evidence;
2. an extraction file matcher contained an over-escaped regular expression,
   which the real workspace test exposed after the pure compiler landed; and
3. a persistent-REPL run revealed that one retained-basis test assumed global
   registry isolation, while a clean full-suite run proved production behavior
   remained correct.

Those are architectural findings, not merely expected-value mismatches.

## What became pure

### Diagnostic verification

`diagnostic-delta` compares immutable before and after finding multisets. It
ignores row and column drift, retains duplicate counts, blocks only newly
introduced warnings or errors, and returns stable refusal data for malformed
snapshots. The process shell captures two cache-independent clj-kondo EDN
snapshots and delegates the decision to that function.

This fixes the stale-index failure seen during cross-namespace extraction. A
pre-existing warning can move down the file without blocking a correct change;
a newly introduced copy of the same warning still blocks it.

### Extraction

`compile-plan` now consumes source strings, requested owners, a target
namespace, and a captured workspace map. `compile-candidates` produces and
parses both complete future files. Filesystem traversal and atomic commit remain
in the shell.

The compiler now has two explicit policies:

- `:minimal` proves the exact target requirements and refuses unsupported
  source shapes.
- `:copy-all` preserves the complete source namespace header and changes only
  its namespace name.

The second policy came directly from observing agents repeatedly rebuild new
namespace headers by hand. Moving code and minimizing dependencies are
different decisions. `:copy-all` makes the first move conservative and
mechanical; dependency cleanup becomes a separate refactor after compile and
test.

### Retained change bases

`compile-prepared-basis` compiles deterministic named sites, source hashes,
budgets, and the decision viewport from captured values plus injected identity
and time. The live wrapper owns semantic calls, source capture, UUID/time, and
registry publication. Refused compilation never mutates the retained-basis
store.

### Quoted Var proof

`scan-sources` scans a captured source map deterministically. The workspace
adapter supplies authoritative absolute paths explicitly. That separation made
the path-authority regression obvious and testable.

### Transaction invariants

A new property matrix enumerates all six orders of three independent edits
across two files. Every order must produce identical bytes, preserve unselected
syntax, parse, generate an inverse receipt, and restore the exact original
sources.

## Measurements

The full repository gate after the refactor passed:

| Gate | Result |
|---|---:|
| Primary suite | 642 tests, 5,537 assertions, 0 failures |
| MCP suite | 141 tests, 1,165 assertions, 0 failures |
| Final full `make test` wall time | 138.93 s |
| Changed-file lint | 0 errors, 0 warnings |

The preceding green baseline had 634 primary tests and 5,412 assertions. No
assertion was removed or relaxed. The new design added eight primary tests and
125 assertions while retaining the real filesystem, process, commit, rollback,
MCP, and cold-runtime tests.

An immediately preceding full run of nearly the same worktree took 169.59
seconds. That spread is machine-load evidence, not a 30-second product claim;
full-suite wall time needs repeated controlled runs before attribution. The
snapshot microbenchmark below isolates the architectural difference directly.

One controlled hot-REPL probe measured the benefit of reusing a captured
workspace snapshot. Ten identical extraction compilations over a 175-file
workspace took 3,445.5 ms through the pure compiler and 6,600.4 ms through the
filesystem shell: **1.92x throughput** for the pure route. This is an
architectural microbenchmark, not an end-to-end agent claim. It measures the
work eliminated when many cases share one snapshot.

The mixed focused gate remained intentionally heavier because it includes the
retained boundaries: 131 tests and 1,033 assertions passed in 10.89 seconds in
a fresh Babashka process. The pure functions themselves run inside the hot REPL
and make combinatorial matrices cheap; the boundary cases dominate the
remaining wall time by design.

## The clean caller found two bytes and 115,172 tokens

A clean ephemeral Codex caller received the architectural goal without the
flag name: move three dependent forms, preserve the complete source header,
and do not minimize dependencies in the same change. It loaded the installed
skill, independently selected `:require-policy :copy-all`, proved the three-form
closure, reviewed the remaining caller, applied the extraction, and verified
both namespaces.

Then the repository formatter changed two pieces of generated trivia:

- `insert-into-require` left one space before its inserted newline;
- the target compiler emitted one extra final newline.

Those two bytes invalidated the exact undo receipt. The clean caller correctly
noticed, but spent **115,172 tokens** reconstructing receipt-matching bytes,
undoing, formatting first, and reapplying. This was the opposite of the
intended experience: the structural decision was right immediately, while two
formatter bytes triggered recovery archaeology.

The enduring fix changed require insertion to rewrite-clj's non-spacing
`append-child*` primitive and made the target end in exactly one newline.
Regression tests pin both lexical contracts. A new end-to-end proof now runs:

```text
extract! -> formatter -> unchanged hashes -> undo-extract!
```

The formatter changes zero bytes and the original receipt restores the source
and removes the target. The finding is a strong example of why clean callers
belong in the product loop: ordinary unit expectations did not ask whether a
mandatory formatter would preserve the receipt's exact future hashes.

## Testing doctrine

The new rule is:

```text
capture once
  -> compile many decisions as values
  -> prove invariants exhaustively
  -> cross each real effect boundary in a small retained suite
```

Purity does not replace integration evidence. It changes its job. Pure tests
prove the decision algebra over many combinations. Integration tests prove
that source capture, subprocesses, serialization, atomic writes, read-back,
recovery, and hot MCP publication obey the algebra's contract.

The release gate remains monotonic: existing tests stay, changed behavior adds
tests, and refactoring must leave the suite stronger than it found it.

## Bottom line

The suite was already the reason aggressive refactoring was possible. Purity
makes that suite denser: more adversarial states, less setup, clearer failures,
and the same battle-tested boundary proof. The tool is becoming easier to
change precisely because the tests make accidental semantic change difficult.
