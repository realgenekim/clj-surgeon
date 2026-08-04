# Keep Quoted Structural Symbols Searchable

**Status:** Fix present in the uncommitted worktree; verify and commit
**Severity:** P1 structural correctness

## Evidence

The first `for` safety guard walked every symbol in the authored expression by
name. It therefore rejected inert structural patterns such as `(match 'loop)`
because the quoted symbol `loop` shares a name with macro-expansion-only
executable machinery.

An incoming worktree fix now skips `(quote ...)` subtrees, retains direct
executable refusal, reports the offending symbol, and adds a table-driven
regression for all macro-expansion-only symbols. These edits appeared while the
handoff review was in progress and were not authored by the documenting agent.

## Required Outcome

Review the incoming diff, format it, and prove that quote awareness cannot be
used to execute forbidden machinery. Keep structural Clojure data searchable
while refusing the same symbol in executable position before source I/O.

## Tests and Verification

- Every guarded symbol succeeds when quoted as a `match` pattern.
- Every guarded symbol refuses in executable position with stable
  `:symbol`, `:reason`, and remedy fields.
- Namespaced `clojure.core/quote` and syntax-quote behavior are covered.
- Existing `for`, direct loop, CLI refusal, and no-source-I/O tests remain.
- Full suite and clj-kondo pass with a higher assertion count.

## Done When

Quoted structural data is never confused with executable source, the complete
matrix passes, and the fix has an intentional commit/tag on the audit branch.
