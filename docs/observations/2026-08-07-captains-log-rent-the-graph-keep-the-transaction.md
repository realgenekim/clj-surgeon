# Captain's Log: Rent the Graph, Keep the Transaction

**Date:** 2026-08-07

**Question:** An independent "code atlas" design proposed a lossless code
tree with call-graph overlays, node dossiers, and a clj-kondo-backed edge
index inside clj-surgeon. After paper exercises and a sibling-repository
audit, how much of that should be built here — and what is clj-surgeon's
surviving unique value?

## Bottom line

Almost none of the cross-namespace graph should be built here. The semantic
index already exists, installed and persistent, one directory away. The atlas
survives as internal architecture. It rents resolved semantic evidence from
the hot clojure-lsp index through a bridge, but it does not rent mutation.
Positions never reach the caller, and semantic evidence never gains write
authority. What remains uniquely clj-surgeon's — hash-bound snapshots,
candidate futures, structural pattern match, budget-guarded batched reads,
and the guarded intent compiler — has no other owner anywhere in the
ecosystem.

The audit established this boundary by elimination, not ambition.

## What the audit found, verified at the meter

- **clojure-lsp 2026.02.20** (embedding clj-kondo 2026.01.19) installed,
  with a persistent incremental workspace index answering references,
  definitions, call hierarchy, workspace symbols, and rename computation.
- **cclsp** (`../cclsp`) wrapping it as a persistent MCP server:
  `find_references`, `prepare_call_hierarchy`, `find_workspace_symbols`,
  `rename_symbol` with dry-run.
- **Mothership** (`../mothership/src/app/analysis.clj`, 27 functions)
  already projecting kondo var records into caller/callee summaries.
- A live `clojure-lsp references` call on the paper-exercise target
  `mcp-contract/normalize-success-receipt` returned all four sites,
  including the test reference, in one answer — behind roughly nine seconds
  of cold JVM start per CLI invocation.

The capability list and the usage record differ. The studied sessions used
cclsp for navigation and semantic evidence. They did not use cclsp or
clojure-lsp to rewrite code. Therefore, `rename_symbol` proves that an
alternative implementation exists. It does not prove adoption or justify a
write-path integration.

The [Three Rounds roadmap](../plans/three-rounds-roadmap.md) step 2 as
originally written — a kondo-backed caller/callee index inside clj-surgeon —
would therefore have been the fourth implementation of the same graph on
this machine. The audit killed it. This is the make-our-tools-perfect
discipline applied to our own design: one bounded reconnaissance pass found
the paved road before we built a parallel one.

## The critique, both sides

**For the atlas proposal:** its unifying substrate is correct — one internal
node-and-edge model makes semantic zoom, registers, diffs, and call graphs
projections of the same thing rather than accreting subsystems. Its
epistemics (typed edges, named authorities, uncertainty preserved) match
this repository's receipt culture. Its best idea, the queryable candidate
future, survives fully intact and unowned by any sibling.

**Against building its graph here:** it proposed constructing what
clojure-lsp already maintains incrementally and cclsp already serves. It
carried no round accounting, no keep gates, and no sequencing — the exact
failure mode the vision's Bitter-Lesson boundary exists to stop. And its
caller-facing node ids would have reimported cursor-state bookkeeping that
the semantic-address design already litigated away.

## Three sharpenings beyond "just reuse LSP"

1. **The position-addressing trap.** LSP's native addresses are file, line,
   and column against current editor state — literally the cursor-state
   this project eliminates. If the bridge leaks positions to the caller,
   the disease returns through the reuse door. Substrate rule 6: semantic
   address in, LSP position for the question, every answer re-anchored to
   the smallest containing hash-bound structural node — and the batch
   **refuses on drift** between snapshot hash and LSP index rather than
   returning stale positions as fact. The refusal is what carries the
   evidence discipline across the authority boundary.

2. **Rent semantic facts, not mutation.** cclsp's navigation tools provide
   observed value. Its rewrite tools do not yet have adoption evidence.
   clj-surgeon therefore does not import LSP workspace edits in this build
   order. The model states the intended structural change, and clj-surgeon
   compiles that intent into a guarded transaction. If repeated field use
   later demonstrates value in LSP-generated edits, clj-surgeon can evaluate
   them as untrusted candidate input. LSP must never apply them directly.

3. **Mothership's `analysis.clj` is evidence, not a dependency.** It proves
   the projections are about 27 functions of cheap pure code. Taking more —
   a cross-repo runtime coupling to an application's internal namespace —
   would trade a week of typing for a permanent architectural debt. The
   durable authority is clojure-lsp itself. Corollary (rule 7): the
   nine-second CLI walls were cold JVM starts; the bridge must connect the
   hot clj-surgeon server to the hot LSP index, persistent-to-persistent,
   or the rounds argument dies at the transport.

## The re-scored thesis

The [paper exercises](../plans/atlas-paper-exercises.md) were re-scored
against the audit. E1 through E4 — traces, impact cones, rename resolution,
keyword flow — all have an owner to bridge to. **E6 and E7 have no owner
anywhere**: immutable hash-bound snapshots, queryable candidate futures with
relationship deltas, structural form-pattern queries, budget-guarded
batching, and the guarded transaction engine. No sibling on this machine
and, to current knowledge, no tool in the Clojure ecosystem offers them.

```text
rented evidence:  references, call hierarchy, symbols, keyword records
owned action:      structural intent compilation, snapshots, candidate
                   futures, structural match, budgets, guards, atomic
                   commit, receipts, undo
field-gated:       importing LSP-generated edits as candidate input
```

## Revised build order

1. Run the frozen read portfolio against the live `inspect_clojure`
   (unchanged; the standing experiment).
2. Run the frozen relationship tasks against hot cclsp. Add a read-only LSP
   bridge for `callers`, `callees`, or `witness-path` only when composition
   removes measured caller rounds. Re-anchor every result and refuse on
   drift. Do not build a kondo index in clj-surgeon.
3. Manifest symmetry and queryable candidate snapshots — the unowned
   ground.
4. Runtime overlays and broadcast edits wait for a field flail, as before.

Less to build, more to prove, same three rounds.

## Bitter-Lesson boundary

cclsp is a semantic sensor. clj-surgeon is a structural intent compiler.
The model decides scope, meaning, architecture, and the desired result.
clj-surgeon owns address resolution, count proofs, exact source preservation,
write ordering, atomic commit, and verification. Adding a catalog of named
refactoring opinions would move judgment into the tool and age poorly as
models improve. A small general transformation algebra makes a better model
more capable without teaching the kernel what the model should want.
