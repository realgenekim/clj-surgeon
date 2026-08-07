# The Clojure Agent Tool Stack

This repository uses four distinct layers. They cooperate, but they do not
have the same runtime, protocol, data model, or authority.

```text
coding agent
    |
    | MCP: inspect_clojure / apply_clojure_changes
    v
clj-surgeon MCP                    exact source and transaction authority
    |
    | MCP: resolve_var_surface
    v
cclsp                              MCP-to-LSP protocol bridge
    |
    | LSP over child-process stdio
    v
clojure-lsp                        persistent Clojure project model
    |
    | embedded analysis library
    v
clj-kondo                          Clojure analysis and lint facts
```

## Comparison

| Layer | Implementation and runtime | Main libraries or substrate | Protocol and lifecycle | What it owns here | What it does not own |
|---|---|---|---|---|---|
| clj-kondo | Clojure; embedded as a library in clojure-lsp's GraalVM image; the separate CLI is also a GraalVM native image | Its Clojure analyzer, parser, hooks, configuration, findings, and analysis records | Library calls when embedded; CLI when run separately | Resolved Clojure analysis facts and lint findings | LSP sessions, MCP, lossless source edits, transactions |
| clojure-lsp | Clojure; release executables are GraalVM native images. A JVM JAR and JVM API also exist. | Embedded clj-kondo analysis, lsp4clj, rewrite-clj, cljfmt, and persistent project caches | LSP; one long-lived process per configured workspace in this setup | Incremental project model, definitions, references, symbols, call hierarchy, diagnostics, and optional LSP refactoring calculations | Agent-facing MCP contracts and clj-surgeon write authority |
| cclsp | TypeScript; Bun 1.3.14 for local development and tests; bundled JavaScript can run on Node | Official TypeScript MCP SDK 1.30.x, Node HTTP and child processes, one configured LSP client | Streamable HTTP MCP on port 7890; it starts clojure-lsp as an LSP child over stdio; Bun restarts cclsp on save behind the same URL | Translation between agent-shaped MCP calls and LSP; compact semantic results; owner enrichment | Clojure analysis itself, exact concrete syntax, or mutation authority in this workflow |
| clj-surgeon CLI | Clojure on Babashka | rewrite-clj, SCI, EDN, clj-kondo as a verification command | One CLI process per invocation | Fast structural reads, local structural analysis, plans, and guarded file operations | A persistent cross-namespace semantic index |
| clj-surgeon MCP | Clojure 1.12.1 on a HotSpot JVM | clojure-mcp, Java MCP SDK with Jackson JSON, Jetty 12 servlet transport, rewrite-clj, SCI, nREPL | Streamable HTTP MCP on port 7888; one persistent JVM; handler Vars reload through nREPL | Exact source snapshots, lossless addresses, basis state, guarded multi-file transactions, rollback, receipts, and verification | Reimplementing the cross-workspace graph that clojure-lsp already maintains |

## Is clojure-lsp Graal?

Yes, with one precise qualification: clojure-lsp is written in Clojure, and
its release executables are compiled ahead of time with GraalVM Native Image.
GraalVM is the packaging and execution technology, not the implementation
language.

The [official build documentation](https://clojure-lsp.io/building/) says that
every release's Windows, Linux, and macOS native binaries are compiled with
GraalVM. The project also supports an editor/CLI JAR and a JVM API. The
[installation documentation](https://clojure-lsp.io/installation/) recommends
the native executable.

The native executable starts without a HotSpot JVM. It can still invoke Java
or a Clojure build tool while it discovers a project's classpath. Native Image
therefore removes the language server's JVM startup; it does not remove every
possible Java subprocess from Clojure project analysis.

GraalVM is a good fit for the released language server because it gives a
self-contained executable and fast startup. It is less convenient for live
development than a JVM REPL. This stack handles that trade-off by keeping the
native clojure-lsp process hot and reloading the layers above it:

- cclsp restarts TypeScript on save behind a stable URL;
- clj-surgeon reloads Clojure handler Vars through nREPL;
- clojure-lsp keeps its incremental workspace model in a persistent native
  process.

## What each semantic layer adds

clj-kondo is the analyzer. It produces the raw resolved definitions, usages,
findings, and related records. Its
[build documentation](https://cljdoc.org/d/clj-kondo/clj-kondo/2026.05.25/doc/building-from-source)
describes its GraalVM native CLI. clojure-lsp embeds a specific clj-kondo
version as a library and turns those facts into a stateful language-server model. The
[clojure-lsp settings documentation](https://clojure-lsp.io/settings/) states
that most features use embedded clj-kondo analysis.

clojure-lsp adds the project and protocol layer:

- classpath and source-path discovery;
- incremental workspace state and caches;
- LSP document synchronization and locations;
- definitions, references, implementations, symbols, and call hierarchy;
- diagnostics, formatting, and refactoring calculations.

cclsp adds no new Clojure analyzer. It adapts those LSP capabilities to MCP,
normalizes results for an agent, and keeps the LSP child hot. Our local fork
also attaches named owners to references and preserves non-file dependency
targets instead of failing the complete answer.

clj-surgeon adds the authority that neither semantic layer provides in this
workflow. It converts semantic locations to exact rewrite-clj forms, binds
them to source hashes, asks the model for explicit decisions, and commits one
failure-atomic transaction. This is the architectural rule:

```text
rent semantic facts; keep the transaction
```

clojure-lsp and cclsp can calculate edits, but clj-surgeon does not delegate
write authority to them. An LSP edit can become untrusted candidate input in a
future experiment. It must still pass clj-surgeon's source, scope, hash,
parse, verification, rollback, and receipt contracts.

## Local measured snapshot

These values describe this machine on 2026-08-07. They are observations, not
portable capacity promises.

| Process or artifact | Observed version or mode | Observed size |
|---|---|---:|
| Installed clojure-lsp binary | 2026.02.20; embeds clj-kondo 2026.01.19 | 123,512,760 B on disk; about 14 MiB RSS while the current workspace process was idle; an earlier indexing probe reached about 397 MiB RSS |
| Active cclsp development process | Bun watch mode | about 20 MiB RSS |
| Active clj-surgeon MCP process | Clojure 1.12.1 on HotSpot | about 55 MiB RSS |
| Standalone clj-kondo binary on `PATH` | 2023.10.20 | 42,554,998 B on disk |

The standalone clj-kondo executable is not the clj-kondo embedded in
clojure-lsp. `clojure-lsp --version` reports its embedded version. Running
`clj-kondo --version` reports the separate verification executable. The local
versions currently differ.

RSS changes with project size and analysis phase. A native executable can use
much more memory while indexing than it uses when idle. Measure the process
tree during the actual workload before making a capacity decision.

## Why this shape is useful

The layers match the work:

| Question | Best authority |
|---|---|
| What Var does this symbol resolve to? | clojure-lsp through cclsp |
| Which named forms reference this Var? | cclsp owner-enriched references |
| What exact comments, reader syntax, and whitespace surround the site? | clj-surgeon and rewrite-clj |
| What should the program mean after the change? | The model and the human |
| Can all selected changes commit together against unchanged source? | clj-surgeon transaction compiler |
| Does the result lint, format, compile, and pass tests? | clj-kondo, formatter, compiler, and tests |

This separation is the performance strategy. Keep semantic indexes and MCP
servers hot. Return complete decision context once. Let the model decide once.
Then apply and verify one transaction without asking the model to reconstruct
files, positions, hashes, or partial progress.
