# One compiler, two entrances

**Status:** Accepted architecture; implementation in vertical slices
**Motivating issue/incidents:** parent `clj-surgeon-tmr`; action gap
`clj-surgeon-95u`; continuation gaps `clj-surgeon-p24` and
`clj-surgeon-q7l`; observability gaps `clj-surgeon-9iy` and
`clj-surgeon-th5`; harness-isolation defects `clj-surgeon-13w` and
`clj-surgeon-78d`.

## Outcome

Clj-surgeon has two public MCP entrances and one internal execution model:

```text
inspect_clojure
  -> evidence basis: exact source, owners, addresses, hashes, authority

apply_clojure_changes
  -> explicit decision program: targets, actions, guards, verification

both converge on
  -> addressed actions
  -> one combined future snapshot
  -> format and verification program
  -> one failure-atomic commit
  -> one truthful reversible receipt
```

Known exact edits may enter the compiler directly. Exploratory or semantic
changes first obtain a retained basis. In both routes the caller makes one
coherent decision and the runtime owns addressing, ordering, formatting,
verification, rollback, and receipts.

The design closes the remaining recurrent escape hatches without adding more
MCP tools or parallel planners. Native patching remains the positive control for
prose, JavaScript, new arbitrary files, and one supplied text edit for which
Clojure structure adds no value.

## Architectural Shape

### Plane 1: evidence

An evidence item has one canonical workspace root and file, exact source hash,
lossless owner/address, bounded source or summary, and an authority label:

- `exact-source`: clj-surgeon proved bytes and syntax;
- `language-server`: cclsp proved resolved semantic locations;
- `structural-candidate`: syntax found possible callers without claiming
  resolution;
- `runtime`: an application nREPL or verification job proved live behavior.

Authorities may be joined, but never silently promoted. Exact-source mutation
does not wait for a semantic provider when the caller already supplied the
complete file and owners.

### Plane 2: intent

Every mutating planner emits the same closed addressed-action vocabulary:

- replace one exact node;
- delete one exact owner/node with owned trivia;
- insert complete siblings before or after an exact node;
- rename one resolved local binding;
- add one map entry while preserving trivia;
- create one proven-absent file from compiled source.

Extraction, moves, and namespace operations remain distinct pure planners
because their safety questions differ. They converge by emitting addressed
actions plus file-creation facts; they do not get separate writers, formatters,
or receipt implementations.

### Plane 3: transaction

One pure compiler:

1. canonicalizes workspace/file identity;
2. validates guards against one original snapshot;
3. resolves every address against that snapshot;
4. rejects identical, ancestor, descendant, crossing, or trivia-conflicting
   actions;
5. materializes one candidate per canonical file;
6. validates the complete future file set;
7. emits a closed verification program.

One effect runtime stages formatting, commits the complete file set, reads it
back, performs configured hot/cold proofs, publishes one receipt, and rolls back
when a synchronous gate or receipt publication fails.

Dependency direction is permanent and testable:

```text
MCP / CLI adapters
       ↓
small pure operation front ends
       ↓
evidence + addressed-action + transaction contracts
       ↓
confined formatter / filesystem / nREPL / process shells
```

The shared pure contracts never depend on MCP, cclsp, JSON, shell commands, or
workspace-global state. Every mutating front end must return the same compiled
transaction shape. A guard test inventories live-file writers and receipt
publishers; only the transaction runtime may own them. Convergence is not
complete while an older planner can still write through a parallel path.

### Plane 4: diagnostics and control

Every refusal uses one envelope:

```clojure
{:ok false
 :phase :select | :compile | :format | :commit | :verify | :provider
 :error-type :stable-keyword
 :source-unchanged true | false
 :retryability :correct-request | :wait | :recover | :fallback | :none
 :candidates [...]              ; bounded, when factual
 :next-call {...}}              ; only when mechanically determined
```

Every `next-call` carries the originating contract version and source or basis
hashes. Replaying it after contract or source drift refuses as stale; it never
silently reruns discovery against new bytes.

One privacy-safe trace ID joins Surgeon phases, cclsp admission, and underlying
LSP completion. Runtime inspection reports state, generation, queue, active
requests, and bounded phase timelines from the same lifecycle projection used
for request admission. Typed MCP control is the default. A ClojureScript REPL
is retained only if a measured experiment proves it materially reduces
diagnosis/change latency beyond typed control.

Trace IDs are generated at the public entrance, not trusted from caller input.
Trace records are bounded by count, age, and field allowlist. They contain no
source bodies, prompts, replacements, environment values, or command output.

## Bitter-Lesson Boundary

The model or human still chooses architecture, target owners, replacement
source, dependency consent, ignored callers, and verification strength. The
kernel supplies general lossless perception, mechanical compilation, exact
guards, and reversible execution.

This plan does not add a `refactor` oracle, fuzzy selection, automatic owner
choice, automatic dependency pulling, or recovery loops. An executable
`next-call` is emitted only when the correction is a deterministic projection
of facts already proved by the tool.

## Public Contract

- Keep exactly `inspect_clojure` and `apply_clojure_changes` in the Surgeon MCP.
- Keep current direct `changes`, retained `basis`, and typed `extraction`
  inputs; converge their compiled output internally rather than forcing callers
  through one oversized schema.
- Add action variants only when they compile to the shared addressed-action IR.
- Keep success compact: changed logical edits/files, verification state,
  read-back hashes, receipt, and terminal next action.
- Make every refusal truthful about source state and retry class.
- Preserve existing CLI aliases and receipts; MCP is the preferred hot route.

## Implementation Slices

### Slice A: complete the address algebra (`clj-surgeon-95u`)

Represent the file's top-level form sequence as a lossless sibling container.
Compile `insert_before` and `insert_after` against one exact named owner into
the existing raw addressed-edit representation. Preserve separators and comment
ownership; refuse detached or ambiguous trivia. Use the same overlap,
formatter, commit, receipt, and undo path as nested insertion.

### Slice B: make refusal a continuation (`clj-surgeon-p24`, `q7l`)

Factor one diagnostic constructor used by inspect, prepare, direct changes,
extraction, and provider failures. Return bounded owner candidates and a complete
corrected request only when fields/counts can be filled without a semantic
choice. Measure the supplied-decision portfolio at prepare once, decide once,
apply once.

### Slice C: make runtime state observable (`clj-surgeon-9iy`, `th5`)

Propagate one trace ID and phase clock through Surgeon and cclsp. Make the
existing typed runtime inspection show workspaces, generations, queued/active
requests, and timelines. Run the REPL experiment against a predeclared keep
gate; delete it if typed inspection is equally fast and complete.

### Slice D: isolate the laboratory (`clj-surgeon-13w`, `78d`)

Move clean-agent environment construction behind one pure harness compiler.
Clear parent workstream identity, allocate one canonical temporary workspace,
onboard both hot services to that root, assert returned path identity, and clean
up from one receipt. Benchmark scaffolding must never modify product state or
the parent Director projection.

Each slice must ship and improve the next slice's own development loop. No slice
waits for the entire architecture program.

## Adversarial Review That Changed the Plan

The first draft proposed one universal operation schema. That would move
complexity into a giant tagged union and erase important planner-specific
invariants. The revised architecture shares the evidence, addressed-action,
transaction, receipt, diagnostic, and runtime layers while allowing extraction,
binding rename, and exact replacement to retain small pure front-end compilers.

Other rejected failure modes:

- **Semantic graph as mandatory gate:** rejected because exact-source edits
  must remain fast and available while cclsp warms.
- **One monolithic compiler namespace:** rejected; phase contracts are closed
  data with pure functions and narrow effect shells.
- **Executable remedy that guesses:** rejected; ambiguous candidates remain
  evidence and require caller judgment.
- **Tracing source bodies:** rejected; traces carry IDs, counts, hashes,
  timings, phases, and error types—not source or prompts.
- **REPL as a second control plane:** rejected unless it beats typed MCP on a
  falsifiable diagnosis benchmark.
- **Harness policy in product runtime:** rejected; benchmark isolation consumes
  public contracts but does not change them.
- **Eliminate native tools:** rejected. Perfection means the structural route is
  irresistible where structure helps, not compulsory everywhere.
- **Compatibility shims for competing paths:** rejected. Existing public shapes
  compile into the shared core; new internal paths do not coexist indefinitely.

### Second review: defenses against an elegant-looking hodgepodge

The first accepted plan lacked a falsifiable convergence test. Code could have
adopted the new map names while old writers, diagnostics, or provider state
remained reachable. The revised plan adds these requirements:

- **One internal shape is observable:** every mutating front end has a contract
  test proving it returns the shared compiled-transaction fields before effects.
- **One writer is enforceable:** a permanent architecture test allowlists the
  namespaces that may write live source or publish receipts.
- **Front ends stay small:** operation-specific compilers may select and prove
  facts, but may not format, write, verify, recover, or publish.
- **Continuation is immutable:** executable corrections carry contract and
  source identity, so convenience cannot weaken stale-source safety.
- **Provider failure cannot poison exact work:** exact-source mutation is tested
  while the semantic provider is cold, warming, failed, and absent.
- **Tracing has a budget:** telemetry-off and telemetry-on runs must show less
  than 50 ms p95 trace overhead for a hot structural request.
- **The REPL has a deletion criterion:** if typed runtime inspection answers
  state, outstanding work, phase history, reload identity, and targeted
  recovery within the same diagnosis-call budget, the REPL experiment closes
  as rejected instead of becoming permanent infrastructure.
- **Public-tool count is evidence-backed:** two tools remain the default because
  clean callers already understand perception versus action. A future
  controlled experiment may challenge that choice; it is not theology.

## Safety Invariants

1. One original snapshot and one canonical path identity per physical file.
2. Every action is exact, counted, disjoint, and parseable before the first
   write.
3. Source, semantic, and runtime authorities remain explicit.
4. Formatting operates on staged candidates and joins the transaction receipt.
5. Synchronous failure either performs no write or proves rollback.
6. Cold failure retains the exact undo receipt and never claims terminal
   success.
7. `source_unchanged` is evidence, not an error-category guess.
8. Recovery is bounded to one typed continuation; no automatic restart loop.
9. Existing tests and guards are only strengthened.
10. The number of public Surgeon MCP tools remains two.
11. Exact-source work remains operational in every semantic-provider lifecycle
    state.
12. No operation-specific front end owns formatter, filesystem, verification,
    rollback, or receipt effects.

## Test Plan

Each plane has an exhaustive pure matrix and one real boundary proof:

- evidence: roots, path aliases, owners, hashes, authority joins, stale/mixed
  sessions;
- intent: every action, owner type, count distribution, comment/metadata/reader
  trivia, and unsupported combination;
- transaction: permutations, overlap classes, create/update/delete mixes,
  formatter coalescing, rollback, receipt publication, and exact undo;
- diagnostics: every stable error type crossed with source truth, retry class,
  candidate bound, and presence/absence of `next-call`;
- runtime: lifecycle states, request deadlines, cancellation, queue bounds,
  trace joins, hot reload, and targeted recovery;
- harness: environment scrubbing, canonical roots, parallel isolation, cleanup,
  and inability to publish parent progress.

Real-program-derived fixtures cover top-level insertion, multi-owner refactors,
comment-bearing code, one semantic surface, one extraction, one live nREPL law,
and one slow provider route. Tests never depend on a mutable external checkout.

## Documentation and Release Checklist

- Keep vision, README, help, MCP descriptions, Codex skill, and Claude skill on
  the same two-entrance model.
- Document authority labels, diagnostic retry classes, and the native boundary
  once, then derive shorter help from that contract.
- Record call counts, model recovery rounds, tool wall, total wall, and fallback
  use after each slice.
- Run `make mcp-reload` after runtime/schema changes and `make install` only
  after focused and full gates.
- Close child Beads only with real public-route evidence.

## Verification Gates

1. Pure plane-specific tests fail before implementation.
2. Changed Clojure/TypeScript files format and lint cleanly.
3. Focused tests run in the hot development runtime.
4. Surgeon and cclsp full suites pass without weaker assertions.
5. One live public dogfood and exact undo pass for each mutating slice.
6. One refusal reaches its corrected call without schema archaeology.
7. A traced slow route is reconstructable from telemetry alone.
8. Four clean supplied-decision replicas use at most three Surgeon calls and
   remain faster than native control.
9. A 12-hour field receipt reports adoption, fallbacks, median calls, and p90.
10. Architecture guards prove one live writer/receipt runtime and the declared
    dependency direction.
11. Hot structural p95 stays below two seconds and trace overhead stays below
    50 ms p95 on the local benchmark fixture.

## Definition of Done

The parent is complete when the address algebra supports exact top-level and
nested changes, supplied coherent intent normally completes as inspect once,
decide once, apply once, every refusal has one truthful typed continuation or a
clear stop, one trace explains cross-service latency without transcript review,
the typed runtime control plane makes shared-service state and recovery
observable, benchmark agents cannot contaminate parent state, all child issues
are closed with full gates, and no third Surgeon MCP tool or parallel mutation
runtime was introduced.
