# Captain's Log: Sol Kept Its Chord; Claude Found It

**Date:** 2026-08-27
**Product commit:** `573e240c052809abb93f6ed8ed766fb5ff888aaa`
**Decision:** Preserve the Sol route unchanged. Add a caller adapter for
Claude's deferred MCP discovery. Do not alter the transaction to manufacture
identical route geometry.

## Question

The extraction transaction had already beaten native editing with Codex
Sol/high. Could Claude Fable/high and Opus/high use the same product and obtain
the same class of gain without imposing a discovery or compatibility tax on
Sol?

The performance law was asymmetric on purpose:

1. Sol is the frozen control. Its one direct apply must not regress.
2. Claude may use one caller-specific discovery action if its MCP catalog is
   deferred.
3. Every caller must beat its own same-model native control. Cross-model wall
   comparisons are descriptive only.
4. The scorer, task, semantic requirements, exact verifier, and source
   snapshot are identical.

## Frozen task

The task is the retained `sessionize-format-extraction` counterfactual. It
moves 15 named forms and attached comments from a 4,594-line namespace into a
new namespace. It preserves 63 remaining caller occurrences, changes only
`not-blank` from private to public, derives a dependency-minimal target, and
runs the repository-declared exact clj-kondo command.

The semantic scorer ignores inconsequential whitespace but requires the moved
owners, comments, dependencies, callers, visibility law, and unrelated forms
to retain their meaning.

## Directly routed local canaries

The first local screen used a fresh isolated workspace and a fresh 512 MiB MCP
for each Surgeon arm.

| Caller | Surgeon | Native | Speedup | Result |
|---|---:|---:|---:|---|
| Claude Fable/high | 18.850s | 102.051s | 5.41x | both semantic-correct |
| Claude Opus/high | 12.754s | 107.484s | 8.43x | both semantic-correct |

Each Claude Surgeon route was exactly:

```text
ToolSearch(select apply_clojure_changes)
  -> apply_clojure_changes(extraction + verify=exact)
  -> terminal_response relayed exactly
```

There were no source reads, native edits, shell calls, CLI calls, inspections,
refusals, or recovery calls.

## Serial Anvil pilot

The exact Git bundle, runner scripts, and result archive were SHA-verified.
The model arms ran serially so analyzer and model load did not overlap.

| Caller | Surgeon | Matched native | Speedup | Native actions | Surgeon actions |
|---|---:|---:|---:|---:|---:|
| Codex Sol/high | 21.446s | 207.898s | 9.69x | 8 commands | 1 apply |
| Codex Terra/high | 23.621s | 178.477s | 7.56x | 4 commands | 1 apply |
| Claude Fable/high | 14.325s | 128.512s | 8.97x | 9 tools | 1 search + 1 apply |
| Claude Opus/high | 12.924s | 82.377s | 6.37x | 9 tools | 1 search + 1 apply |

All eight matched arms were semantic-correct. The Surgeon arms received
exact-exit zero, complete staged formatting, committed-byte read-back hashes,
an inverse receipt hash, `verification_complete=true`, and the exact terminal
response.

The Sol result is the non-regression gate. Its 21.446-second wall is inside the
previous promoted product cohort's 17.004-to-21.428-second range. Claude support
required no Sol prompt, schema, kernel, response, or tool-catalog change.

## What Claude did differently

Claude Code 2.1.228 reported the isolated HTTP server connected and placed all
four Surgeon operations in its deferred catalog. Both Fable and Opus used
`ToolSearch` with the exact selector
`mcp__clj-surgeon__apply_clojure_changes`, received one tool reference, and
then emitted the same transaction object as Sol.

The discovery step is real work and remains in both the action count and
complete wall. Claude's `modelUsage` also records a small Haiku catalog-helper
charge in addition to the requested primary model. We do not hide or pool it.

Despite discovery, Claude materialized the transaction faster in this pilot:

```text
Sol/high
  0.000  turn begins
 14.651  apply starts
 17.052  apply result observed (2.367s server authority)
 20.082  exact terminal response materialized
 21.446  process exits

Fable/high
  0.000  Claude init observed
  2.839  ToolSearch emitted
 ~6.763  apply emitted
 ~8.753  apply result observed (1.949s server authority)
 ~10.382  exact terminal response emitted
 14.325  process wall

Opus/high
  0.000  Claude init observed
  1.802  ToolSearch emitted
 ~5.477  apply emitted
 ~7.920  apply result observed (2.400s server authority)
 ~8.954  exact terminal response emitted
 12.924  process wall
```

Claude stream events and Codex item events have different clock semantics, so
the approximate Claude offsets are observer clocks, not a pooled phase model.
The important route fact is exact: both Claude callers needed one discovery
turn; neither needed a source-decision turn.

## Why the native gap is large

The Surgeon result is not primarily a faster text editor. It deletes decision
and recovery phases that every native caller recreated:

```text
native
  locate spans
  -> inspect namespace and caller occurrences
  -> devise extraction script or patch
  -> encode stale-source assertions
  -> execute
  -> repair script/route mistakes when present
  -> run verifier
  -> interpret and narrate

Surgeon
  supplied decision
  -> one compiled transaction
     [snapshot fence, extraction, caller rewrite, formatting,
      exact verification, atomic commit, read-back, inverse receipt]
  -> constant terminal relay
```

Fable native used nine actions on Anvil. Opus native also used nine. Sol native
used eight commands and 196,644 input tokens; Sol Surgeon used 27,759 input
tokens.

## The Perl parenthesis incident

The additive Terra/high screen exposed the native route's hidden assignment:
the model had to invent a Clojure structural editor before it could perform the
requested extraction. Terra chose Perl. Its generated mutation script replaced
the complete `(:import ...)))` clause with:

```clojure
))
```

That left an unmatched closing delimiter at source line 23. The result was not
a harmless formatting difference or a strict byte-comparison failure; the
namespace did not parse. The 219.303-second native attempt had used seven
commands before reaching that state. Its requested `~/bin/clj-kondo` verifier
also did not exist on the Anvil image, so verification exited 127 rather than
catching and guiding repair.

This failure is representative of a recurring coding-agent behavior. When an
agent lacks a structural primitive, it will use Perl, Python, regexes, line
slicing, delimiter counters, or repeated patches to balance Clojure
parentheses. That is more than a quoting problem and more than a Perl problem:
comments, metadata, strings, quoting, reader conditionals, and nested
collections make it a parser problem. Every caller is otherwise forced to
rebuild a fragile parser during the task.

Surgeon deletes that assignment. Terra supplied the same architectural
decision to one `apply_clojure_changes` call; rewrite-clj owned the tree and
delimiters, the transaction owned rollback and verification, and the result was
correct in 23.621 seconds.

A fresh serial Terra/high native retry preserved the prompt, task, scorer,
model, effort, and product commit. Only the missing conventional verifier
entrance was mapped to the already-installed `/usr/local/bin/clj-kondo`. The
retry became semantic-correct in 178.477 seconds and four command round trips,
and the required exact verifier ran to exit zero with no errors. Its first
mutation/verifier command still failed because the destination file was absent;
Terra then used the prompt-authorized bounded repair and succeeded on the
second mutation/verifier command. The correct matched result is 7.56x in
Surgeon's favor. The malformed first attempt remains reliability evidence; it
is not substituted into the speed calculation.

Spark/high reinforced the distinction. Its Surgeon arm was correct in 11.966
seconds with one apply. Its first native attempt took 62.207 seconds and 26
commands, produced parseable Clojure, but deleted almost the entire 4,594-line
source namespace and failed the semantic scorer. Parseability alone is not the
goal; the structural transaction preserves the named decision and everything
outside it.

## Experimental failures retained

Three harness defects were useful findings and were not scored as model or
product failures:

1. Anvil lacked `rg`. The Codex harness refused before any model turn. The
   standard `ripgrep` package was installed on the benchmark seat.
2. The temporary Claude scorer and the existing Codex scorer were first
   launched outside the cloned repository. Babashka could not resolve the
   project classpath. The launcher now enters the exact clone before scoring.
3. The general Codex harness began optional compact-skill contexts after the
   requested pair because `BENCH_INCLUDE_COMPACT=false` was omitted. The pair
   was already complete. Both experiment-owned process trees were terminated
   using their result-owner PIDs and validated CWDs; no unrelated process was
   touched.
4. Anvil provided `/usr/local/bin/clj-kondo` but not the literal
   `~/bin/clj-kondo` entrance in the native prompt. Fable and Opus recovered to
   the installed binary. Terra and Spark did not. Retry runs establish the
   conventional path explicitly and still require semantic correctness; this
   environment repair does not erase either source failure.

These are harness portability and bounded-execution lessons. They do not alter
the eight matched result rows.

## Evidence

Immutable archive:

`/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-27/clj-surgeon-cross-caller-anvil-573e240-r1.tar.gz`

SHA-256:

`f6e5944d5640a8a375be9307fc7d2937578b58e92314226c366dda0aabd28d70`

The archive contains the four Claude workspaces, raw stream events, observer
clocks, tool calls, semantic scores, summaries, MCP telemetry, and the two
requested Sol run directories plus `runs.tsv`.

The additive Terra/Spark screens and the successful Terra retry are preserved
separately at:

`/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-27/clj-surgeon-terra-spark-anvil-573e240-r1-r2.tar.gz`

SHA-256:

`865c1bbeeec179eac391208ad9b8240292155cc82450febb62164c310b909c00`

The decisive Terra scorer hashes are
`6a8ce5f91deff460283e8aed51c70f2794c922c5e88322a835de1bf679c46f25`
for the malformed first attempt and
`3e3dc6631727f86750f5a139a5203c14c3b41e30a846b8d7c6f82795148b4206`
for the correct retry. The temporary Anvil verifier symlink was removed after
the retry, restoring the original seat state.

## Decision and next hill

Keep the public operation and Sol fast path unchanged. Give Claude a thin,
explicit deferred-discovery route and score its discovery honestly.

This N=1-per-caller Anvil pilot is sufficient to establish compatibility and
a large matched gain. It is not a tail-latency estimate. If publication needs
a stronger cross-caller number, run one reverse-order pair per caller rather
than widening models or changing the API.

The next product question is natural adoption: when Fable or Opus receives an
ordinary coding request rather than the directly routed benchmark, does the
installed global routing lead it to the same transaction? Keep that as a
separate ethnographic hill. Do not contaminate the proven capability result or
slow Sol to solve Claude discoverability.
