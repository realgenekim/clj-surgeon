# Quoted Var reference proof

**Status:** Implemented and live-reloaded
**Motivating issue/incidents:** `clj-surgeon-nj2`; a production refactor found
two `#'.../private-var` callers only after the resolved-reference proof had
declared the first migration complete.

## Outcome

`inspect_clojure mode=prepare-change` returns every structurally provable
Var-quoted caller in its change surface, even when clojure-lsp or clj-kondo
omits that reference. Fully qualified, namespace-aliased, and same-namespace
unqualified Var quotes share one proof route. Every site states whether its
authority is the language server or the lossless structural supplement.

## Bitter-Lesson Boundary

The tool does not infer architecture, choose callers to migrate, or replace
the language server. It mechanically unions two kinds of evidence that are
already present in the workspace: resolved references and exact Var syntax.
The model still decides what the complete caller surface means for the change.

## Public Contract

The existing `prepare-change` request is unchanged. Its result adds a bounded
`quoted_var_proof` summary and an `authority` field on surface sites.

- `:language-server+exact-source` means the semantic provider found the site
  and clj-surgeon independently addressed its containing form.
- `:structural-var-quote+exact-source` means a lossless scan found `(var x)` or
  `#'x`, resolved `x` through the file namespace, and addressed its containing
  form.

The supplement recognizes fully qualified Vars, aliases declared by the file
namespace, and unqualified Vars in the defining namespace. It ignores strings,
comments, and quoted data. Invalid candidate source or a bounded-scan overflow
refuses the preparation before retaining a basis.

## Safety Invariants

- Structural evidence is never labeled as LSP evidence.
- Every retained source is read once, hashed, parsed, and confined to the
  canonical workspace.
- Duplicate semantic and structural evidence produces one decision site.
- Comments, strings, and quoted data never authorize a caller mutation.
- Any scan, parse, path, count, or source-drift failure retains no basis and
  changes no source.

## Implementation Shape

Add a pure quoted-Var recognizer plus a small workspace scanner. The recognizer
uses rewrite-clj nodes and namespace aliases; the I/O shell enumerates only
Clojure source under declared workspace roots and returns exact locations plus
a source cache. `mcp-change-buffer` unions those locations before its existing
capture, owner-addressing, budget, basis, and transaction stages.

## Test Plan

- Pure matrix: reader syntax and `(var ...)`; fully qualified, aliased, and
  same-namespace names; comments, strings, quoted data, unrelated aliases, and
  ordinary symbol calls.
- Integration: a semantic provider deliberately omits two production-shaped
  private-Var callers; `prepare-change` returns both with structural authority,
  deduplicates an LSP-provided copy, and reads each relevant file once.
- Refusal: malformed candidate source and scan-budget overflow retain no basis.
- Real-program regression: the fully qualified private-Var form from the field
  report is represented in a stable fixture, not read from a live repository.

## Documentation and Release Checklist

Update README, MCP help, installed Codex/Claude skills, and the Captain's Log.
Close the incident Bead with focused, full-suite, live-MCP, and source-unchanged
evidence.

## Verification Gates

Format changed Clojure files, run focused tests, clj-kondo, the complete test
suite, `make install`, hot reload the MCP handler, and perform one live
`prepare-change` request whose semantic provider misses a quoted Var.

## Definition of Done

The field fixture produces one complete, authority-labeled caller surface in a
single `prepare-change` call, negative lookalikes remain absent, duplicate
evidence collapses, every refusal is pre-mutation, and all repository gates are
green without weakening an existing assertion.

## Completion evidence

- Pure and integration suite: fully qualified, aliased, same-namespace,
  comment, string, quoted, syntax-quoted, discarded, malformed, and semantic-
  omission cases are covered.
- Main suite: 616 tests, 5,312 assertions, 0 failures.
- MCP suite: 131 tests, 1,079 assertions, 0 failures.
- clj-kondo: 0 errors, 0 warnings on all changed Clojure files.
- Live MCP contract synchronized without a server restart; the field workspace
  replay retained exact definition evidence while its LSP session warmed.
