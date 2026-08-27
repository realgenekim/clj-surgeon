# Captain's Log: The Var Surface Was a Small Index

<!-- agent-usage-window-end: 2026-08-27T18:05:57.856773Z -->

## Question

The public cclsp boundary is gone, but Surgeon still delegates semantic Var
surface resolution to the internal broker and `clojure-lsp`. Is the capability
we actually use difficult to reproduce, or are we paying language-server
ceremony for a small relation?

The measured operation is:

```text
fully qualified Var
  -> exact definition
  -> resolved usages
  -> enclosing owners, ranges, and source hashes
```

This log tests the definition/usage relation. It does not generalize the result
to call hierarchy, protocol implementations, macro-generated relationships,
classpath symbols, or hard reader-conditionals.

## Fresh bounded window

The canonical agent-usage collector covered:

- UTC: `2026-08-27T07:31:45.757004Z` through
  `2026-08-27T18:05:57.856773Z`;
- Pacific: `2026-08-27 00:31:45` through `11:05:57` PDT;
- receipt:
  `/tmp/clj-surgeon-agent-usage-20260827T073145757004Z-20260827T180557856773Z.json`;
- receipt SHA-256:
  `da93125b00e8db0e11e1f147f23741afb123664baf8f7b8ea0ac0d3146988809`.

The privacy-safe counting receipt observed five `resolve_var_surface` calls
over four unique workspace/subject keys. One key repeated after the first.
Three LSP sessions initialized for four underlying LSP requests. Initialization
used 9,603ms of 11,506ms LSP wall, or 83.46%. The one useful references request
used 1,903ms. No safe cache hit was claimed because the repeated key lacked a
workspace snapshot fence.

## Four retained replays

The four unique requests were replayed against their preserved workspaces.
Workspace and Var names are anonymized here; the exact local receipt remains
available for reproducibility.

The refined syntax pass scans one frozen source map, proves fully qualified and
alias-qualified references, and emits non-authoritative proof gaps for bare
symbols that can resolve through the same namespace, explicit `:refer`, or
`:use`.

| Case | Clojure files | Exact syntax usages | Bare proof gaps | Syntax outcome |
|---|---:|---:|---:|---|
| Tool Var A | 236 | 9 | 1 | one bounded semantic gap |
| Tool Var B | 236 | 3 | 1 | one bounded semantic gap |
| Application Var C | 203 | 4 | 0 | syntax-terminal for this relation |
| Application Var D | 457 | 1 | 0 | syntax-terminal for this relation |

The final read-plus-scan walls were approximately 1.04s, 0.87s, 3.29s, and
1.01s. These were local read-only probes through a standalone 512MiB analysis
nREPL. Case C ran while host load was elevated; do not use its absolute time as
a clean low-load estimate. An earlier pass completed the same scan in about
1.28s.

Every cross-namespace usage in the cohort was alias-qualified. Namespace alias
parsing was not a missing capability. The only real gaps were one bare
same-namespace use in each tool-repository case.

## The existing dependency already resolves the hard gap

Surgeon already invokes clj-kondo analysis for forward references and
binding-aware local renames. A direct clj-kondo analysis exposes resolved
`:var-definitions` and `:var-usages` with `:from`, `:to`, name, file, row, and
column.

Running clj-kondo only over the syntax candidate files produced the exact
definition and usage surface for all four retained queries:

| Case | Candidate files | Resolved usages | Wall | Maximum RSS |
|---|---:|---:|---:|---:|
| Tool Var A | 4 | 10 | 2.27s | 92.9MB |
| Tool Var B | 2 | 4 | 0.99s | 77.9MB |
| Application Var C | 4 | 4 | 10.13s | 192.8MB |
| Application Var D | 3 | 1 | 1.26s | 121.9MB |

The two application cases do not need this fallback after the refined syntax
pass. The candidate-bounded clj-kondo result matters for the two tool cases: it
proved the one bare usage and rejected unrelated same-named locals.

Whole-repository clj-kondo is not the paved road. One tool-repository run
emitted 14.3MB of JSON and took 3.79--5.97s. One large application workspace
took 15.23s and reached about 1.32GB maximum RSS. Candidate narrowing and a
small result projection are required.

The host load average rose from 18 to 120 during the four-case cold batch. The
clj-kondo processes had exited when inspected; the dominant runnable process
was macOS Spotlight metadata work, not cclsp. The experiment therefore does
not assign load causality to either component. Further cold runs stopped.

## Falsification result

The retained evidence does not prove that the cclsp Var-surface capability is
hard to reproduce. It proves the opposite:

- syntax alone resolved two of four unique cases completely;
- syntax reduced the other two to one exact bare-symbol question each;
- an existing Surgeon dependency resolved all four exact surfaces without
  `clojure-lsp`;
- the broker/LSP path supplied no unique evidence on this cohort;
- one prior LSP result omitted a real alias-qualified caller, while syntax
  found it;
- another prior LSP result duplicated worktree and canonical identities.

The honest conclusion is narrow: `resolve_var_surface` appears to be a small,
reproducible index for the observed Clojure workload. Other semantic operations
remain unproven and keep their escalation status.

## Cache invalidation law

A complete surface result cannot be cached by subject alone. A concurrent agent
can add or remove a usage in any relevant source file.

The first safe key is:

```text
subject
+ Merkle root of relevant source hashes
+ source-root/project configuration
+ analyzer and result-contract version
+ platform/reader mode
```

Only changed Merkle leaves and their ancestors need recomputation, but a changed
relevant root logically invalidates the old complete-surface result. Selective
per-Var invalidation requires a maintained reverse index; building that index is
the same class of work as the proposed replacement, so it must be earned later.

## Product ratchet

`syntax_var_refs/scan-sources` now returns:

- exact qualified locations with `authority=true`;
- ordered `candidate-files`;
- ordered bare-symbol `proof-gaps` with `authority=false` and
  `reason=:bare-symbol-needs-resolution`;
- no proof gap for definition names, namespace declarations, inert syntax, or
  same-named locals in unrelated namespaces.

The pure contract is covered by synthetic `:refer`, `:use`, lexical-shadow,
inert-syntax, parsing, budget, ordering, and real-program-derived witnesses.
It does not yet change the MCP path or shared runtime.

## Next hill

1. Add one pure reducer from clj-kondo analysis plus frozen candidate sources to
   the existing Var-surface algebra. It must filter exact `:to` namespace and
   `:name`, attach source hashes, and refuse missing or drifted candidate files.
2. Choose an execution shape that avoids whole-analysis JSON: a pinned
   in-process clj-kondo dependency or one bounded subprocess with a compact
   projection. Measure before choosing.
3. Integrate the syntax-first ladder into `prepare-change` through the scoped
   Linked-Intent workflow. Do not change shared MCP behavior from this spike.
4. Run the existing 20-request stratified acceptance gate before retiring
   `clojure-lsp` for Var surfaces.
5. Retain cclsp escalation for call hierarchy, protocols, generated semantics,
   classpath relationships, and hard reader conditionals until each receives
   an independent corpus and falsifier.

Durable owner: `clj-surgeon-tmr.8`.
