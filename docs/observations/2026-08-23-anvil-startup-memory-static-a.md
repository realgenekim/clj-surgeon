# clj-surgeon MCP startup memory: static reconnaissance A

**Scope:** bead `clj-surgeon-ajg`; static source analysis only. I did not start
MCP, cclsp, Jetty, nREPL, or an application JVM. Clojure source was read with
the repository's `clj-surgeon` structural CLI. Therefore object sizes,
dominators, allocation rates, and the causal threshold at 1.5 GiB remain to be
measured.

## Verdict

The strongest first experiment is to start the exact HTTP server with
`:nrepl-port :none`. The normal managed path eagerly loads and constructs the
full CIDER nREPL handler before it initializes clj-surgeon's tools, MCP SDK
objects, or Jetty. The existing benchmark target already disables that nREPL.
This is the cleanest static explanation for “1–1.5 GiB fails during startup,
2 GiB succeeds, settled RSS is much lower.” It is a high-confidence code-path
fact and a medium-to-high-confidence causal hypothesis, not yet a measured
dominator.

The strongest contrarian finding is that initial startup does **not** scan a
workspace source tree, initialize clojure-lsp, connect to cclsp, construct an
SCI interpreter, launch a verification future, or populate the retained basis
and job stores. Those mechanisms can create later bursts or retention, but the
static call path does not support blaming them for the first-start OOM.

The other credible peak mechanism is broad eager namespace loading followed
immediately by several heavyweight constructors. A structural require-closure
walk from `clj-surgeon.mcp-http-server` reaches 36 internal namespaces,
14,965 source lines, and 822 top-level forms before the entry function runs.
Then startup constructs CIDER/nREPL, tool schemas and SDK tools, an MCP server,
and Jetty in one sequence. Compiler and JSON intermediates may be dead at idle
but still overlap the growing live server graph at the startup high-water mark.

## Adversarial startup call graph

```text
Makefile:mcp-start (201-240)
  -> cclsp-start (must report healthy before MCP launch)
  -> clojure -J-Xms64m -J-Xmx2g -X:clj-surgeon/mcp
       deps.edn :clj-surgeon/mcp (10-19)
         -> load clj-surgeon.mcp-http-server
              eager require closure: 36 project namespaces / 14,965 lines
              plus clojure-mcp, Jetty, nREPL, Cheshire, rewrite-clj, SCI
         -> mcp-http-server/start (286-292)
              -> start-http-server! (185-272)
                   -> read-project-config (105-117): one .clj-surgeon.edn
                   -> resolve verification profile maps
                   -> telemetry/start! (mcp_telemetry.clj 62-88)
                        -> telemetry/prune! (44-60): one .listFiles array
                   -> start-embedded-nrepl! (mcp_server.clj 220-233)
                        -> requiring-resolve cider.nrepl/cider-nrepl-handler
                        -> deref handler constructor
                        -> nrepl.server/start-server; retain handler/server
                   -> configure logging
                   -> mcp-tool/init! (mcp_tool.clj 68-75)
                        -> workspace/router: empty contexts atom
                        -> inspect-tool/init! (mcp_inspect_tool.clj 345-355)
                             -> requiring-resolve semantic client
                             -> semantic-client/init!: store URL only
                        -> root configured map in runtime/tool-config
                   -> build HTTP transport and default JSON mapper
                   -> configure-specification (mcp_server.clj 200-210)
                        -> all-tools: two already-materialized tool maps
                        -> mapv create-async-tool
                             -> Cheshire schema -> JSON string
                             -> MCP SDK parses/builds schema objects
                   -> build async MCP server
                   -> register-live-server! (mcp_server.clj 140-145)
                        -> root server + schema-bearing contracts
                   -> construct Jetty Server and servlet context
                   -> start Jetty threads/connectors
                   -> write readiness and telemetry
              -> block in Server.join while `running` remains on the stack
```

The order matters. nREPL is live before tool initialization starts, and all
project namespace loading has already happened before the entry function is
called. Startup is mostly sequential, not a fan-out of application futures,
but live products from earlier phases coexist with transient allocations from
later phases.

## Allocation rate, peak live set, and steady retained set

| Question | Static answer |
|---|---|
| Allocation rate | Likely highest while Clojure compiles the 36-namespace require closure, CIDER constructs its middleware handler, and two schemas are converted Clojure-map -> JSON string -> SDK representation. Jetty/Reactor class initialization follows. |
| Peak live set | At minimum contains the Clojure runtime/classes, CIDER handler and nREPL server, telemetry state, configured tool map/router, schema Vars, SDK tools/MCP server, transport, and Jetty graph. Compiler, reflection, JSON, and builder intermediates may still be awaiting collection. Exact liveness is unproven. |
| Steady retained set | Proven roots are `runtime/tool-config`, `runtime/live-tool-state`, Clojure Vars/classes, the nREPL server/handler, the MCP/transport/Jetty graphs, and the `running` map held across `.join`. The workspace context map and basis/job stores begin empty. |

High allocation rate by itself should cause GC rather than an OOM. A hard
1.5-GiB threshold implies that at some collection point the live/temporarily
rooted graph, fragmentation, metaspace/native allocations, or GC reserve is
too large. The low later RSS is consistent with startup temporaries becoming
eligible after construction, but it does not prove which objects died or that
the failure was Java-heap rather than native/metaspace pressure.

## Ranked suspects

### 1. Full CIDER middleware is synchronously loaded before every normal HTTP start

**Evidence and mechanism.** The production alias includes
`cider/cider-nrepl` (`deps.edn:10-19`). `start-http-server!` calls
`mcp-server/start-embedded-nrepl!` before tool initialization
(`src/clj_surgeon/mcp_http_server.clj`, form `start-http-server!`, lines
185-272). That function performs
`requiring-resolve 'cider.nrepl/cider-nrepl-handler`, dereferences the handler,
and starts nREPL (`src/clj_surgeon/mcp_server.clj`, form
`start-embedded-nrepl!`, lines 220-233). `mcp-start` supplies no
`:nrepl-port :none` (`Makefile:216-224`), while `mcp-serve-benchmark` does
(`Makefile:110-111`). This loads a development/debugging stack even though MCP
readiness does not depend on CIDER.

**Confidence.** High that it is a large eager phase and steady root; medium-high
that it is the threshold-crossing cause.

**Smallest discriminating experiment.** At 1 GiB and 1.5 GiB, run the exact
entry twice with identical telemetry and project arguments, changing only
`:nrepl-port :none`. Record GC logs, startup completion, peak used heap, loaded
class count, and post-full-GC heap. A third arm using plain nREPL's default
handler separates nREPL from CIDER.

**Likely fix.** Make CIDER opt-in, start a small plain nREPL by default, or
defer CIDER handler loading until an explicit development attachment.

**Risk.** `mcp-reload` currently depends on the embedded CIDER-capable nREPL;
removing it without a replacement would break the hot reload workflow.

### 2. Tool discovery eagerly loads most of the implementation

**Evidence and mechanism.** `mcp-http-server` eagerly requires `mcp-server`
and `mcp-tool` (lines 1-25). `mcp-tool` in turn directly requires extraction,
the 1,946-line intent transaction, the 1,559-line change buffer, inspect,
formatter, contract, schemas, and their parser stacks
(`src/clj_surgeon/mcp_tool.clj`, namespace form, lines 1-22). A structural
require-closure traversal measured 36 internal namespaces, 14,965 lines, and
822 forms. Even routes unused during startup have Vars and generated classes
loaded before the first tool request.

**Confidence.** High for allocation/class-loading breadth; medium for the OOM
threshold because much of the resulting class/Var state is genuinely retained.

**Smallest discriminating experiment.** Compare require-only probes under the
same heap: (a) `mcp-http-server`, (b) a minimal namespace containing only the
HTTP/SDK shell, and (c) require the tool implementation namespaces one at a
time. Use JFR allocation-by-class plus post-GC class histograms after each
phase.

**Likely fix.** Split tool contract data from handlers. Load only names,
descriptions, schemas, annotations, and Var-based dispatch at startup; use
`requiring-resolve` for heavy handler namespaces on the first request. Keep
the two-tool public contract unchanged.

**Risk.** First request latency moves later, and lazy failure must remain a
typed tool refusal rather than a transport failure. Reload must update Vars
without creating per-request classloader churn.

### 3. Startup phases retain their products while later phases allocate

**Evidence and mechanism.** `start-http-server!` orders telemetry, CIDER/nREPL,
logging, tool initialization, HTTP transport, MCP tool construction, live
registration, Jetty construction, and Jetty start in one `let`
(`src/clj_surgeon/mcp_http_server.clj`, lines 185-272). Earlier values remain
lexically reachable through the end of the form and are returned in the
`running` map. nREPL threads are started before schema and Jetty allocation.

**Confidence.** High that products overlap; medium that lexical/JIT liveness of
dead intermediates materially enlarges the peak.

**Smallest discriminating experiment.** Add temporary phase markers and invoke
`jcmd GC.class_histogram` plus JFR checkpoints after nREPL, tool init, MCP build,
and Jetty start. Compare ordinary execution with nREPL moved after HTTP
readiness. Do not infer from RSS alone.

**Likely fix.** Stage startup through small functions with narrow locals; start
optional nREPL last; avoid carrying builders and serialized schema strings
across phase boundaries.

**Risk.** Readiness must not be published before required components are
functional, and startup rollback must still close each component already
created.

### 4. Tool schemas exist simultaneously as Clojure data, JSON strings, and SDK objects

**Evidence and mechanism.** Large schema maps are materialized as namespace
Vars (`src/clj_surgeon/mcp_schema.clj`, forms `explicit-change-schema` lines
63-201 and `clj-change-schema` lines 271-298;
`src/clj_surgeon/mcp_inspect_tool.clj`, forms `typed-inspect-schema` lines
60-110, `prepare-change-schema` lines 112-162, and `inspect-schema` lines
189-201). For each structured tool, `create-structured-async-tool` calls
`json/generate-string` for input and output schemas, then gives those strings
to the SDK builder (`src/clj_surgeon/mcp_server.clj`, lines 80-124). The Clojure
maps remain rooted in tool Vars and registered contracts while the SDK retains
its representation.

**Confidence.** High for duplicate representations; low-medium as a 1.5-GiB
cause because the visible schemas are not themselves remotely that large.

**Smallest discriminating experiment.** Measure retained size and allocation
for `configure-specification` with (a) current tools, (b) one tiny schema, and
(c) current schema without output schema. Heap histograms should identify
`String`, Jackson node/map, and persistent-map deltas.

**Likely fix.** Precompute one canonical SDK schema representation, avoid
round-tripping through Cheshire strings if the SDK accepts data, and keep only
a compact contract hash in `live-tool-state` if full contracts are unnecessary.

**Risk.** Schema/list equality and live reload behavior are protocol contracts;
deduplication must preserve exact advertised JSON and change detection.

### 5. Jetty, Reactor, and MCP SDK class initialization occurs at the high-water mark

**Evidence and mechanism.** The HTTP namespace imports Jetty servlet/server
types and MCP SDK transport types (`src/clj_surgeon/mcp_http_server.clj`,
namespace form, lines 1-25). After the prior phases, startup builds the
transport, async MCP server, Jetty `Server`, context, servlets/filter, and
connectors, then starts Jetty (`start-http-server!`, lines 185-272). The stdio
path is smaller (`src/clj_surgeon/mcp_server.clj`, form `build-stdio-server`,
lines 212-218).

**Confidence.** Medium that it adds a meaningful class/thread allocation step;
low-medium that it dominates the OOM.

**Smallest discriminating experiment.** At the failing heap, compare
HTTP-versus-stdio with nREPL disabled and identical tool schemas. Then compare
HTTP with a zero-tool specification. Record heap and native-memory deltas.

**Likely fix.** Only if measured: use a leaner HTTP runtime or delay optional
Jetty facilities. Do not replace the transport based on static suspicion.

**Risk.** Transport changes have outsized protocol, streaming, shutdown, and
client-compatibility risk.

### 6. Global roots retain the complete configured runtime and server graph

**Evidence and mechanism.** `runtime/tool-config` and
`runtime/live-tool-state` are `defonce` atoms
(`src/clj_surgeon/mcp_runtime.clj:5-6`). `mcp-tool/init!` roots the configured
map plus workspace router (`src/clj_surgeon/mcp_tool.clj:68-75`).
`register-live-server!` roots the SDK server and schema-bearing registered
contracts (`src/clj_surgeon/mcp_server.clj:140-145`). The tool contracts keep
description, schema, output schema, annotations, and structured flag
(`tool-contract`, lines 44-49). These are proven steady roots, though many
nested persistent values are structurally shared rather than copied.

**Confidence.** High for steady retention; low-medium for transient OOM.

**Smallest discriminating experiment.** Capture a post-readiness heap dump and
report dominator paths from both atoms. Compare retained size before and after
temporarily storing only server plus contract hashes in `live-tool-state`.

**Likely fix.** Store the smallest reload/readiness state possible; ensure stop
clears tool config and closes any semantic client when the JVM is reused.

**Risk.** Over-pruning can break workspace routing, readiness, or live tool
synchronization. Managed stop normally kills the JVM, so cleanup work may not
improve the production lifecycle.

### 7. Telemetry pruning eagerly materializes every directory entry

**Evidence and mechanism.** Full telemetry is passed by `mcp-start`
(`Makefile:219-221`). `telemetry/start!` calls `prune!` before creating the new
session file (`src/clj_surgeon/mcp_telemetry.clj:62-88`). `prune!` calls Java
`.listFiles`, which allocates an array and `File` objects for every entry, then
filters/sorts old JSONL files (`lines 44-60`).

**Confidence.** High for the eager directory array; low unless the telemetry
directory contains an extreme file count.

**Smallest discriminating experiment.** Record entry count and run startup
with the real directory versus a new empty directory and telemetry `:off`, at
the same heap. Do not delete existing telemetry for the experiment.

**Likely fix.** Use a bounded `DirectoryStream`, incremental deletion, and a
hard per-start scan budget.

**Risk.** A bound can leave old files behind; retention policy needs a later
background continuation or explicit maintenance command.

### 8. Hot reload may accumulate generated classes or stale SDK tool graphs

**Evidence and mechanism.** `make mcp-reload` reloads 27 namespaces one by one
with `require :reload`, then calls `sync-tools!` (`Makefile:116-126`).
`sync-tools!` removes/upserts SDK tools and swaps registered contracts
(`src/clj_surgeon/mcp_server.clj:155-198`). Handlers are Vars, which is the
right anti-retention choice, but Clojure reload still creates replacement
function/class objects; whether the SDK, Reactor handlers, clients, or nREPL
retain removed specifications is not visible in this repository.

**Confidence.** Medium as a repeated-reload growth risk; very low for a clean
first start.

**Smallest discriminating experiment.** On one process, force full GC and take
class histograms at baseline and after 1, 10, and 50 no-op reload cycles. Track
classloader count, old function classes, SDK tool specifications, and Reactor
handlers. A flat post-GC line falsifies this suspect.

**Likely fix.** Reload only changed namespaces in dependency order and prove
SDK removal releases old specifications; otherwise restart the bounded worker
after a measured reload budget.

**Risk.** Selective reload can leave inconsistent Vars; restart-on-budget loses
the current hot-loop advantage and requires client-stable supervision.

### 9. Workspace contexts are cached without a count or TTL

**Evidence and mechanism.** `workspace/router` creates `:contexts (atom {})`
(`src/clj_surgeon/mcp_workspace.clj:54-58`). `resolve-request` associates one
delay per canonical root and never evicts it (`lines 71-90`). Each realized
context merges base configuration and root-specific closures. This is empty at
startup, but a long-lived shared server can accumulate worktree paths.

**Confidence.** High for unbounded key retention after diverse use; effectively
zero for the initial startup OOM.

**Smallest discriminating experiment.** Issue one trivial request against
1,000 synthetic canonical roots using a factory returning small sentinels;
measure post-GC retained size and `cached-roots`, then repeat with an LRU/TTL
prototype.

**Likely fix.** Bound contexts by active lease plus idle LRU/TTL, or cache only
root strings and recreate the two small lazy closures per request.

**Risk.** Evicting an active context could lose per-root verification behavior;
current contexts appear immutable, but that invariant should be tested.

### 10. Request-time source snapshots and verification jobs are bounded but can form a later memory floor

**Evidence and mechanism.** The basis store is a global atom capped at 32
entries, one hour, 24 sites, and 4 MiB of snapshot characters per basis
(`src/clj_surgeon/mcp_change_buffer.clj:24-29`; pruning at lines 48-57). The
cold job store is capped at 64 terminal jobs and four running jobs with 12,000
characters of output (`src/clj_surgeon/mcp_cold_verify.clj:15-19`, pruning at
60-78). `launch!` uses a Clojure `future` (`lines 156-195`). Large workspace
reads exist in `mcp-tool/workspace-sources` (lines 122-129) and
`quoted-var-refs/scan-workspace` (lines 201-226), but both are request paths.

**Confidence.** High for later bounded retention/allocation; zero for a clean
startup because both atoms initialize empty and no future is launched.

**Smallest discriminating experiment.** Fill 32 bases with maximum-size
snapshots and four concurrent cold jobs, force GC, and inspect atom dominators,
future executors, and output strings. Separately record first-request peaks for
extraction and quoted-reference fallback.

**Likely fix.** Budget by total retained bytes rather than entry count, discard
bases immediately after terminal apply/refusal where safe, and use an explicit
bounded executor whose lifecycle is observable.

**Risk.** More aggressive eviction weakens multi-turn prepared-change UX;
executor replacement must preserve job capacity/refusal semantics.

## What is specifically not happening on first startup

- **No workspace enumeration.** The only eager project read is one
  `.clj-surgeon.edn` (`mcp_http_server/read-project-config`, lines 105-117).
  `workspace/router` starts with an empty contexts atom.
- **No source indexing.** `mcp-source-anchor/workspace-source-roots` only reads
  `deps.edn`/`bb.edn` when called (lines 87-96), and source-tree scans live in
  request handlers.
- **No cclsp connection or LSP initialization.** `inspect-tool/init!` lazily
  requires the semantic client, whose `init!` only stores a URL and reports
  whether an existing client is present
  (`src/clj_surgeon/mcp_semantic_client.clj:48-55`). Connection occurs later
  through `client!`.
- **No SCI context construction.** SCI namespaces are eagerly loaded, but
  `sci/init` occurs inside request-time evaluation functions, not top-level
  initializers (`src/clj_surgeon/edit_dsl.clj`, forms
  `evaluate-expression` lines 387-426 and `parse-one-sci-form` lines 472-483).
- **No verification futures or populated caches.** `basis-store` and
  `job-store` are empty atoms; `future` appears only in `cold-verify/launch!`.
- **No accidental test source path.** `:clj-surgeon/mcp` has only production
  `:paths ["src"]`; `test` is added by the separate MCP-test alias
  (`deps.edn:1,10-19,32-42`). CIDER remains a deliberate but unusually heavy
  development dependency on the production server path.

## Architectural remedies in priority order

1. **Decouple CIDER from MCP readiness.** Preserve a small optional nREPL or a
   separately leased development nREPL; make CIDER attachment explicit and
   measurable.
2. **Separate tool contracts from implementations.** Startup should load a
   compact two-tool manifest and Var-based lazy dispatch, not every edit,
   extraction, parser, verification, and source-scan implementation.
3. **Instrument phase boundaries before tuning heap.** Publish phase timing,
   used-heap-after-GC, loaded-class count, and native-memory summaries for
   require, nREPL, tool build, MCP build, and Jetty readiness.
4. **Retain one schema representation.** Avoid Clojure-map/JSON/SDK duplication
   where the SDK permits, and keep hashes rather than full contracts in the
   reload registry if exact behavior remains provable.
5. **Bound every long-lived registry by bytes and ownership.** Workspace
   contexts, bases, jobs, and reload generations need explicit count/byte/TTL
   evidence. This addresses steady growth after the startup peak is fixed.

Merely raising `-Xmx` is not a remedy. A 2-GiB cap is a useful crash fence, but
without knowing the phase and dominator it converts an unexplained startup
peak into a persistent capacity tax.

## Three highest-value next probes

1. **CIDER subtraction matrix.** Run exact managed arguments at 1 GiB and
   1.5 GiB with CIDER/default nREPL, plain nREPL, and `:nrepl-port :none`.
   Capture JFR, `-Xlog:gc*,safepoint`, class counts, and post-readiness full-GC
   used heap. This can confirm or sharply demote suspect 1 in one experiment.
2. **Phase/dominator trace.** At 2 GiB, capture histograms or heap dumps after
   namespace require, nREPL start, tool initialization, MCP build, and Jetty
   start. Add `-XX:NativeMemoryTracking=summary` so native/metaspace is not
   mislabeled Java heap. Compare peak live, allocated bytes, and post-GC
   retained bytes per phase.
3. **Minimal registry/transport matrix.** With nREPL disabled, compare current
   two tools versus a tiny schema, and HTTP versus stdio. This separates eager
   implementation loading, schema conversion, and Jetty/MCP construction.

## Falsification standard

The leading hypothesis is falsified if disabling CIDER/nREPL does not
materially reduce startup peak or permit startup under the failing caps. The
eager-namespace hypothesis is falsified if a minimal lazy registry has the same
allocation/peak profile. Schema and Jetty suspects are falsified by flat
current-versus-minimal deltas. Runtime-cache suspects are excluded from the
initial OOM unless a clean process shows them nonempty before readiness.

Any claimed fix must report all three quantities separately:

1. bytes allocated during the phase;
2. maximum live/used heap (and native committed memory) during startup; and
3. post-full-GC retained heap after readiness.

Without those three, “lower idle RSS” can conceal the same dangerous transient
peak, and “higher heap fixed it” can conceal a new steady retention floor.
