# clj-surgeon startup memory: adversarial static scan B

Date: 2026-08-23
Bead: `clj-surgeon-ajg`
Checkout: `/srv/fleet/dev-b/clj-surgeon-one-shot-canary-20260823`

## Scope and standard of proof

This is an independent, static path-B scan of the clj-surgeon MCP cold-start
path. I did not use the internet, start MCP/cclsp/nREPL/Jetty, or run a large
heap stress test. Clojure evidence came from the repository-required
`clj-surgeon` structural CLI. The only probes were bounded source-structure
counts and ordinary filesystem/config checks.

The observed operational premise is accepted: `-Xmx2g` reaches readiness,
`-Xmx1g` OOMs, 1.5 GiB is suspected to OOM, and settled RSS is materially
lower. Static source can establish ordering and roots. It cannot establish
allocation bytes, dominator sizes, GC reserve, metaspace/native pressure, or
the exact failing phase. Every causal claim below therefore has a falsifier.

The acceptance criterion for a memory fix must cover the entire first-use
sequence, not only readiness:

```text
max(cold require, startup, first inspect, first apply, first hot reload)
```

A change **reduces** memory only if that lifecycle maximum and the post-GC
retained set fall without transferring equivalent memory to a child process.
If readiness improves but the first tool call or reload reaches the old peak,
the change only **moves** memory.

## Executive result

The strongest static finding is a two-stage eager frontier:

1. Before `clj-surgeon.mcp-http-server/start` can execute, requiring its
   namespace reaches 36 internal namespaces containing 14,965 source lines and
   822 top-level forms. This includes the complete edit, extraction,
   inspection, rewrite-clj, and SCI-bearing kernel.
2. During `start-http-server!`, the normal managed configuration then loads and
   starts the full CIDER nREPL before it builds MCP schemas or Jetty. The Make
   target omits `:nrepl-port :none`, and omission means enabled.

That ordering is consistent with a large transient compilation/class-loading
set followed by a retained CIDER handler/server and then SDK/Jetty allocation.
It is also a warning: disabling CIDER may lower startup yet merely move its
cost to the first reload if CIDER is later attached. A valid comparison must
exercise the first reload.

A second high-value finding is an avoidable eager edge:
`mcp_server.clj` requires `clojure-mcp.core`, but the only use is the
non-structured-tool branch of `create-async-tool`. Both currently published
tools are structured. This dependency can be isolated independently of CIDER
and the application kernel.

Conversely, no source-tree scan, cclsp connection, SCI interpreter creation,
prepared basis, verification future, or populated workspace cache occurs on a
fresh path to readiness. Those are first-request or later-retention risks, not
credible explanations for the initial OOM without contrary runtime evidence.

## Bounded structural probes performed

### Internal require closure

I structurally outlined all 47 `src/clj_surgeon/*.clj` files and followed only
internal namespace requires from `clj-surgeon.mcp-http-server`. The closed set
was 36 namespaces, 14,965 lines, and 822 top-level forms. Largest members:

| File | Lines | Forms |
|---|---:|---:|
| `intent_transaction.clj` | 1,946 | 97 |
| `mcp_change_buffer.clj` | 1,559 | 45 |
| `structural_lens.clj` | 1,290 | 79 |
| `edit_dsl.clj` | 865 | 68 |
| `mcp_contract.clj` | 728 | 55 |
| `show_form.clj` | 713 | 45 |
| `mcp_inspect_tool.clj` | 677 | 31 |
| `extract.clj` | 649 | 18 |
| `mcp_inspect.clj` | 599 | 39 |
| `analyze.clj` | 598 | 28 |

Lines/forms are compilation-surface indicators, not heap-byte estimates.

### Schema surface

A structural batch of the five input-schema forms in `mcp_schema.clj` and six
inspect input/output-schema forms in `mcp_inspect_tool.clj` contained 21,859
source characters (17,453 semantic characters). This proves schema conversion
has duplicate representations, but the visible data is too small to explain a
gigabyte directly. A huge measured delta would implicate SDK/Jackson/class
initialization rather than the schema text itself.

### Cold state exclusions

This checkout has no `.clj-surgeon.edn`. `basis-store`, `job-store`, and the
workspace router `contexts` atom are initialized empty. The semantic client is
initialized with `:client nil`. These facts narrow, but do not replace, runtime
measurement.

## Startup call graph with allocation and root boundaries

```text
Makefile:mcp-start (201-240)
  -> cclsp-start (132-190; separate Bun/native-child process family)
  -> clojure -J-Xms64m -J-Xmx2g -X:clj-surgeon/mcp (28, 216-224)
     -> deps.edn alias resolves mcp-http-server/start (10-19)
     -> require clj-surgeon.mcp-http-server
        -> eager 36-namespace application closure
        -> external clojure-mcp/nREPL/rewrite-clj/SCI/SDK/Jetty classes
     -> mcp-http-server/start (mcp_http_server.clj 286-292)
        -> start-http-server! (185-272)
           -> read-project-config (105-117; absent here => {})
           -> resolve verification data
           -> telemetry/start! -> prune! (mcp_telemetry.clj 44-88)
           -> start-embedded-nrepl! (mcp_server.clj 220-233)
              -> requiring-resolve cider.nrepl/cider-nrepl-handler
              -> nrepl.server/start-server
              -> nREPL server/handler retained
           -> mcp-tool/init! (mcp_tool.clj 68-75)
              -> root config + empty workspace router
              -> inspect-tool/init! (mcp_inspect_tool.clj 345-355)
                 -> load semantic-client namespace, store URL only
           -> build HTTP transport
           -> configure-specification (mcp_server.clj 200-210)
              -> JSON-encode input/output schemas
              -> SDK parse/build two tool specifications
           -> build MCP server
           -> register-live-server! (140-145)
              -> root server + registered contract maps
           -> construct/start Jetty context, servlets, filter, threads
           -> write readiness; emit telemetry
        -> retain returned `running` map while blocked in Server.join
```

Readiness is not doing hidden discovery. `health-servlet` calls
`runtime/readiness`, which checks only `tool-config` and `live-tool-state`
(`mcp_http_server.clj`, form `health-servlet`, lines 158-168;
`mcp_runtime.clj`, form `readiness`, lines 8-15).

## Proven GC roots and eligibility

### Proven long-lived at readiness

- `runtime/tool-config` and `runtime/live-tool-state` are `defonce` atoms
  (`mcp_runtime.clj`, forms `tool-config` and `live-tool-state`, lines 5-6).
- `mcp-tool/init!` stores the base config plus a workspace router. The router
  contains the base config and an initially empty contexts atom
  (`mcp_tool.clj`, form `init!`, lines 68-75; `mcp_workspace.clj`, form
  `router`, lines 54-58). Persistent map structure is shared; this is not a
  proven deep copy.
- `register-live-server!` stores the SDK server plus registered contracts in
  `live-tool-state` (`mcp_server.clj`, lines 140-145). `tool-contract` keeps
  descriptions, schemas, output schemas, annotations, and structured flags
  (lines 44-49).
- The returned `running` map holds Jetty, MCP, transport, nREPL, telemetry, and
  readiness values, and `start` holds it while blocking in `.join`
  (`mcp_http_server.clj`, lines 185-292).
- Loaded namespace Vars, generated classes, literal strings/maps, and the
  CIDER/nREPL server/handler remain reachable.

### Expected to become eligible, magnitude unproven

- Reader/compiler intermediates after namespace loading.
- `project-config` and verification-selection intermediates not copied into
  the final config.
- Schema JSON strings and SDK builders after tool construction.
- Telemetry pruning arrays/sequences after `start!` returns.
- MCP/Jetty builders after final objects are built.

The single `let` in `start-http-server!` does not prove every local is live to
the end: JVM GC maps may mark dead locals. The final products do provably
overlap because they are returned together. Runtime liveness data is required
for intermediate overlap.

### Cleanup asymmetry

`start-http-server!` registers the MCP server before Jetty `.start`. Its
`catch Exception` stops nREPL and closes MCP, but does not call
`unregister-live-server!` (`mcp_http_server.clj`, lines 185-272). Normal
`stop-http-server!` does unregister (lines 274-284;
`mcp_server.clj`, form `unregister-live-server!`, lines 147-153). This can root
a closed server if a caller catches a Jetty-start exception and keeps the JVM
alive. It does not explain an uncaught `OutOfMemoryError`: `Error` is not an
`Exception`, and the `-X` process normally exits.

## Ranked hypotheses and falsifiers

### 1. Eager application namespace/class loading creates the first high-water shelf

**Evidence.** `mcp-http-server` directly requires `mcp-tool`
(`mcp_http_server.clj`, namespace form, lines 1-25). `mcp-tool` directly
requires extraction, intent transaction, change buffer, contract, formatter,
inspection, schema, telemetry, and workspace implementations (namespace form,
lines 1-22). `mcp_inspect_tool` itself requires forms, change buffer, inspect,
quoted-var scanning, and structural lens (namespace form, lines 1-15). The
bounded closure is 36 namespaces / 14,965 lines / 822 forms.

**Mechanism.** Cold Clojure compilation allocates reader/compiler structures,
metadata, constants, bytecode, Vars, and generated classes. Compiler objects
can die; classes/Vars remain. The next startup phases begin only after this
frontier has been crossed.

**Confidence.** High for breadth and retention of classes; medium-high for
threshold causality.

**Smallest discriminating experiment.** In a canary JVM, require only
`mcp-http-server` without calling `start`, using the failing heap caps and
phase-tagged GC/class-load logs. Compare with a tiny HTTP contract namespace,
then require the two real handler implementations before exit.

**Falsifier.** If the require-only current closure has a small peak and live
delta, while OOM occurs only after `start`, this drops below CIDER/SDK/Jetty.

**Likely fix and risk.** Separate published contract data from handler kernels
and single-flight `requiring-resolve` each kernel on first use. Risk: this may
only move memory to first inspect/apply; first-call peak and typed failure must
be part of acceptance.

### 2. Default CIDER handler construction pushes the already-loaded JVM over the cap

**Evidence.** The alias includes `cider/cider-nrepl` (`deps.edn`, lines 10-19).
Managed `mcp-start` supplies a port-file but no `:nrepl-port :none`
(Makefile lines 216-224). `start-http-server!` starts nREPL for every value
except exactly `:none`, before MCP/Jetty construction (lines 185-272).
`start-embedded-nrepl!` requiring-resolves and dereferences
`cider.nrepl/cider-nrepl-handler`, then starts nREPL (`mcp_server.clj`, lines
220-233). The benchmark target already demonstrates the supported off switch
(Makefile lines 110-111).

**Mechanism.** CIDER middleware namespace/class loading and handler assembly
allocate after the application closure is retained. The handler/server and
threads remain live while schemas and Jetty allocate.

**Confidence.** High for path and retention; medium-high for OOM causality.

**Smallest discriminating experiment.** Exact managed arguments at 1 GiB and
1.5 GiB, changing only `:nrepl-port :none`. If off succeeds, invoke one inspect
and one apply. In a separate dev arm attach/start CIDER and run one reload;
record the maximum across all phases.

**Falsifier.** Similar phase peaks and identical failure points with nREPL off
falsify CIDER as the dominant startup cause.

**Likely fix and risk.** Make CIDER an explicit development attachment or
start it after required readiness. A plain nREPL middle arm identifies CIDER
versus nREPL base cost. Risk: `mcp-reload` depends on this capability; a lower
readiness peak with the old peak on reload is moved, not reduced.

### 3. An unused `clojure-mcp.core` fallback is eagerly loaded

**Evidence.** `mcp_server.clj` requires `clojure-mcp.core` in its namespace
form (lines 1-18). Its local use is only the false branch of
`create-async-tool` (lines 126-130). `all-tools` returns only
`inspect-tool` and `clj-change-tool` (`mcp_tool.clj`, lines 512-514), and both
tool maps set `:structured? true` (`mcp_inspect_tool.clj`, lines 669-677;
`mcp_tool.clj`, lines 503-510). Current startup therefore never calls the
fallback.

**Mechanism.** Namespace load can pull generic clojure-mcp implementation and
transitive classes into a process that exclusively uses SDK-native structured
tools.

**Confidence.** High that the edge is unused; medium for its memory size.

**Smallest discriminating experiment.** Replace just this require with a
`requiring-resolve` in the non-structured branch; compare require-only class
counts/peak. Exercise a synthetic non-structured tool once to measure the moved
cost and preserve compatibility.

**Falsifier.** No meaningful class/allocation delta makes this cleanup, not a
memory fix.

**Likely fix and risk.** Lazy-resolve or remove the fallback. Risk: stdio or
future extension tools may require it; keep a focused fallback test.

### 4. Startup products overlap later allocations even after intermediates die

**Evidence.** `start-http-server!` creates telemetry, then nREPL, then tool
config, transport/MCP, live registration, and Jetty in one ordered form
(`mcp_http_server.clj`, lines 185-272). The final return map contains all major
products, so these are simultaneously live at readiness.

**Mechanism.** Each phase increases the live floor below later transient
allocation. GC cannot reclaim the nREPL/server/config products to create space
for schema and Jetty construction.

**Confidence.** High for product overlap; unknown for intermediate liveness.

**Smallest discriminating experiment.** Successful 2 GiB run with phase
markers after require, nREPL, tool init, MCP build, and Jetty start; capture
used heap, class count, and allocation rate at each marker without forced GC,
then one post-readiness full-GC retained measurement.

**Falsifier.** A flat live floor with one isolated phase burst points to that
phase rather than accumulation.

**Likely fix and risk.** Start optional components last and narrow phase-local
values/functions. Risk: publishing readiness before required functionality is
a false green; only optional nREPL can safely follow readiness.

### 5. Schema maps, JSON strings, Jackson data, and SDK contracts coexist

**Evidence.** Input schemas are rooted Vars in `mcp_schema.clj` (notably
`explicit-change-schema`, lines 63-201, and `clj-change-schema`, lines
271-298) and `mcp_inspect_tool.clj` (lines 60-229).
`create-structured-async-tool` calls `json/generate-string` for input and
output schemas and passes strings to SDK builders (`mcp_server.clj`, lines
80-124). `register-live-server!` retains selected original contract maps in
addition to the SDK server (lines 140-145). The selected schema forms total
21,859 source characters.

**Mechanism.** Original persistent maps, generated char/string buffers,
Jackson/SDK parse objects, and final specifications overlap during build; maps
and SDK products remain afterward.

**Confidence.** High for duplicate representations; low-medium for a
gigabyte-scale cause.

**Smallest discriminating experiment.** Measure only
`configure-specification` using current schemas versus a constant tiny schema,
with nREPL off and the same SDK. Group allocation by `String`/char arrays,
Jackson, and SDK schema classes.

**Falsifier.** A small delta demotes schema optimization regardless of visible
duplication. A large delta implicates SDK conversion/class initialization, not
the 22K source payload alone.

**Likely fix and risk.** Avoid repeated JSON roundtrips where SDK APIs permit;
retain fingerprints rather than full reload contracts only if exact list-change
semantics remain provable. Risk: protocol/schema equality regressions.

### 6. MCP SDK, Reactor, and Jetty initialize at the highest live floor

**Evidence.** HTTP startup imports MCP transport and Jetty server/servlet
classes (`mcp_http_server.clj`, namespace form, lines 1-25).
`mcp_server.clj` imports SDK specification classes and Reactor `Mono`
(namespace form, lines 1-18). `start-http-server!` builds transport, async MCP
server, Jetty server/context, two servlets, filter, connector, and server
threads after nREPL/tool config (lines 185-272).

**Mechanism.** Class initialization, Jackson mapper state, server thread pools,
thread stacks, native/direct buffers, and JIT work add heap and non-heap memory
late.

**Confidence.** Medium.

**Smallest discriminating experiment.** With nREPL off and identical tools,
compare stdio construction versus HTTP, then HTTP with zero/tiny tools. Capture
native memory and thread counts as well as Java heap.

**Falsifier.** Small HTTP-minus-stdio and two-tool-minus-zero deltas demote this
below compiler/CIDER phases.

**Likely fix and risk.** Tune/bound Jetty only after a measured thread/native
delta. Transport replacement has high streaming, shutdown, and client risk.

### 7. Rooted runtime state retains more contract/config data than readiness needs

**Evidence.** `tool-config` roots base configuration plus router;
`live-tool-state` roots SDK server plus schema-bearing contracts. `tool-contracts`
creates new small maps but references the same descriptions/schemas
(`mcp_server.clj`, forms `tool-contract` lines 44-49 and `tool-contracts`
lines 51-56). The SDK owns a separate parsed representation.

**Mechanism.** Reload/readiness convenience retains both source contract data
and SDK graph. Persistent values are shared rather than deeply duplicated, so
the exact retained cost requires dominators.

**Confidence.** High for roots; low-medium for cold OOM.

**Smallest discriminating experiment.** Post-readiness heap dump, report
dominator paths from the two atoms, then compare a canary that stores only
server plus collision-safe contract digests.

**Falsifier.** Negligible retained size from both atom paths makes this a
clarity cleanup only.

**Likely fix and risk.** Minimize registry state and clear it symmetrically on
startup failure/stop. Risk: breaking live tool synchronization or readiness.

### 8. cclsp URL eagerly loads the semantic SDK client namespace, but not a connection

**Evidence.** Managed start passes `:cclsp-url` (Makefile lines 222-223).
`inspect-tool/init!` calls `semantic-init!`, which requiring-resolves
`mcp-semantic-client/init!` (`mcp_inspect_tool.clj`, lines 243-245 and
345-355). That namespace imports MCP sync/HTTP client classes (namespace form,
lines 1-11). Its `runtime` starts `:client nil`; `init!` stores only the URL;
`client!` calls `connect-client` later (`mcp_semantic_client.clj`, lines
14-64).

**Mechanism.** Client SDK classes load at startup without network/index work.

**Confidence.** High for namespace load, low-medium for size.

**Smallest discriminating experiment.** Compare identical nREPL-off launches
with and without `:cclsp-url`, then issue the first semantic request in the
deferred arm to expose moved cost.

**Falsifier.** Small class/peak delta demotes it; any startup network activity
would contradict the static call path and needs a trace.

**Likely fix and risk.** Store URL only in the base adapter; require/connect on
first semantic operation. Risk: dependency/config failure shifts to first use.

### 9. Telemetry pruning can allocate a full directory listing

**Evidence.** Managed start enables full telemetry and a persistent directory
(Makefile lines 219-221). `telemetry/start!` calls `prune!` before later phases
(`mcp_telemetry.clj`, lines 62-88). `prune!` uses `.listFiles`, then filters,
sorts, and realizes deleted paths (lines 44-60). Telemetry events are written
immediately with `spit`; there is no in-memory event buffer (`emit!`, lines
90-106).

**Mechanism.** An unusually large telemetry directory produces a transient
`File[]` and sorted sequence. This is environmental, not proportional to source.

**Confidence.** High mechanism, low likelihood absent extreme file count.

**Smallest discriminating experiment.** Count directory entries, then compare
real versus new empty telemetry directory at the same cap. Do not delete data.

**Falsifier.** A modest entry count and flat delta remove it from contention.

**Likely fix and risk.** Use `DirectoryStream` and a scan budget, or prune after
readiness. Risk: disk retention backlog.

### 10. Basis, job, and workspace caches explain later growth, not fresh startup

**Evidence.** `basis-store` starts empty and is populated only after successful
`prepare-change!` (`mcp_change_buffer.clj`, lines 24-29 and 908-990). It can
retain full captured source/site maps in `compile-prepared-basis` (lines
794-906), bounded by 32 bases and four million snapshot characters per basis.
`job-store` starts empty; `launch!` creates the only verification `future`
(`mcp_cold_verify.clj`, lines 15-19 and 156-195). Workspace contexts start
empty and grow only in `resolve-request` (`mcp_workspace.clj`, lines 54-90).
The full-tree `file-seq`/`slurp` in `workspace-sources` is request-only
(`mcp_tool.clj`, lines 122-129).

**Mechanism.** After traffic, retained source characters, executor/future state,
and unbounded workspace roots can increase the live floor. They cannot cause a
fresh pre-readiness OOM while empty.

**Confidence.** High exclusion for cold startup; high later-retention risk.

**Smallest discriminating experiment.** Report store counts and retained source
characters at readiness, after first inspect/apply, and after a bounded
multi-root trace.

**Falsifier.** Any nonempty fresh-start store falsifies the static assumption
and identifies an unexpected initializer; otherwise keep this out of the cold
fix.

**Likely fix and risk.** Byte-budget/TTL/lease workspace contexts and discard
terminal bases when safe. Risk: invalidating active multi-step transactions.

## Moved-memory traps

The most tempting fixes can produce misleading green readiness:

- **Disable CIDER:** memory may reappear on first `mcp-reload`.
- **Lazy-load tool kernels:** memory may reappear on first inspect/apply, and
  concurrent first calls may duplicate initialization unless single-flight.
- **Connect cclsp lazily:** SDK/client allocation may move to first semantic
  request or into the cclsp/clojure-lsp process family.
- **AOT application namespaces:** runtime compiler allocation may fall while
  metaspace/class footprint or artifact mapping rises.
- **Move telemetry pruning/background work:** readiness improves while a later
  background peak still overlaps first tool use.
- **Spawn helper JVM/process:** the MCP heap falls but machine-wide memory does
  not; report the process-tree maximum.

For every canary, record these separately:

1. bytes allocated per phase;
2. maximum live/used Java heap;
3. metaspace/native committed memory and thread count;
4. post-GC retained heap after readiness;
5. maximum across first inspect, apply, and reload;
6. whole process-tree RSS/footprint.

Lower idle RSS alone is insufficient evidence.

## Next three safest measurements

### 1. Require-only and CIDER subtraction matrix

This is two bounded axes, no 2 GiB stress requirement:

- require `mcp-http-server` without calling `start`;
- start with exact managed arguments at the already-known failing/safe-small
  cap, once with default nREPL and once with `:nrepl-port :none`;
- optional third arm uses plain nREPL.

Use GC/class-load logs and phase markers. If nREPL-off reaches readiness, run
first inspect/apply and a development reload before claiming reduction.

### 2. Phase-tagged successful-start allocation trace

On the smallest already-safe cap, mark: entry after namespace load, telemetry,
nREPL, tool init, MCP specification build, Jetty start, first inspect, first
apply, and first reload. Use JFR allocation stacks plus GC logs and Native
Memory Tracking. Take one post-readiness and one post-first-use dominator view.
This identifies the crossing phase without changing functionality.

### 3. Lazy-edge canary with full first-use exercise

In one canary, lazy-resolve only the unused `clojure-mcp.core` fallback and the
two heavy handler kernels while publishing identical schemas. Require/start,
then invoke each tool and the reload path. Compare lifecycle maximum, retained
heap, loaded classes, and process-tree memory. This directly distinguishes a
real reduction from a shifted spike and keeps the architectural experiment
small.

## Conclusion

Static evidence supports transient startup overlap as the leading model. The
most defensible causal sequence is: broad eager application compilation,
default CIDER handler/server retention, schema/SDK construction, then Jetty at
the highest live floor. Source-tree scanning and runtime caches are excluded
from a fresh start. The first safe action is measurement, not another heap
increase: subtract CIDER, isolate require-only loading, and carry the comparison
through first inspect/apply/reload so moved memory cannot masquerade as reduced
memory.
