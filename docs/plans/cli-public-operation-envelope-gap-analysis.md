# CLI and Public Operation Envelope Gap Analysis

**Status:** Brain-fleet design review; no product migration started

**Stable baseline:** `stable-cross-caller-6.37x-min-20260827`

**Owning issue:** `clj-surgeon-9xi`

## Decision

Make the CLI and MCP service project one transport-neutral operation algebra,
canonical domain outcome, and mutation runtime. Do not route the CLI through
HTTP, stdio, JSON, or the current MCP handler facade. Do not make the CLI
consume MCP policy.

```text
CLI argv / EDN ----> CLI decoder -----+
                                       |
MCP JSON ----------> MCP decoder ------+--> operation catalog
                                              |
                                      trusted entrance policy
                                              |
                                  operation-specific compiler
                                              |
                         frozen snapshot -> addressed transaction
                                              |
                         stage -> format -> commit -> read back
                                   -> verify / rollback -> receipt
                                              |
                                    canonical domain outcome
                                              |
                                  +-----------+-----------+
                                  |                       |
                          CLI EDN / exit          MCP text / structured
```

The trusted policy is selected by the entrance, never by request data:

- `:cli-legacy` preserves aliases, EDN shapes, exit status, stdout/stderr,
  receipts, verification defaults, and legacy operation policy;
- `:mcp-strict` preserves request budgets, confinement, project-owned exact
  verification, and explicit decision accounting.

This changes one sentence in the current architecture, not its safety law. The
CLI must not consume the MCP *transport or policy*. Both entrances should
consume the same canonical domain evidence and transaction substrate.

## Evidence law

This is not an architecture-by-aesthetic project. A convergence slice enters
the implementation queue only when it has one of these forms of evidence:

1. a measured model or tool phase it can delete;
2. a demonstrated source-corruption, rollback, or verification failure class
   it can mechanically prevent;
3. a measured fleet stability problem caused by duplicate execution paths; or
4. a smaller independently green seam that materially lowers the cost of the
   next experiment.

Pure parity without immediate wall-clock gain is still valuable, but it is
labeled option value and must not displace the next measured performance hill.

## Capability gap table

| Capability | Real evidence | CLI today | MCP today | Target placement | First migration slice |
|---|---|---|---|---|---|
| Compact guarded batch edit | One compiled transaction produced multi-fold wins on large structural changes; the cross-caller extraction measured 6.37x through 9.69x versus matched correct native routes. Terra's Perl edit did not parse; Spark's 26-command native route deleted almost the complete namespace. | `:edit` and `:change!` exist, but there is no direct projection of MCP `edits`, `programs`, and `delete_owners`. | `edit_clojure` lowers the compact gesture into the transaction core. | Pure compact-gesture compiler that emits addressed actions; projections stay distinct. | After canonical outcome parity; not the first slice. |
| Heavy intent transaction | The matched intent compiler was 38.60s versus 134.26s native while its kernel was about 103ms. Removing the public extraction-plan boundary later saved 12.070s and one model action. | `:change` and `:change!` already call `intent_transaction`, with legacy receipts and process output. | Prepared and compact MCP routes eventually converge on the same transaction family. | Operation-specific compilers over one transaction IR; no universal request union. | **First shadow experiment:** `:change` preview, then `:change!`. |
| Extraction compiler | Direct supplied extraction was 37.871s versus 49.941s plan-plus-apply, saving 12.070s. Internal compilation also removed duplicate planning work. | `extract/compile-plan` feeds `:extract` and `:extract!`; execution may commit and report callers for review. | The same pure planner feeds strict planning and one-call apply; every caller candidate must be changed or explicitly ignored. | One planner and frozen workspace enumeration with two trusted policy profiles. | Only after the policy cross-product has permanent tests. |
| Snapshot and stale-source fences | A genuine-decision refusal took 110.219ms and a stale-source refusal took 3.804ms before effects. | Guards and source hashes vary by operation; some legacy commands have no public hash surface. | Frozen snapshot and hash fences are systematic in prepared mutations. | Shared captured-snapshot value and compile-time fences. | Characterize current per-operation behavior before adding fields. |
| Staged formatting | The winning extraction performs formatting inside the transaction; separate formatter work was not the missing performance prize. | Operation-specific and uneven. | Candidate bytes are formatted before commit in the shared mutation path. | Shared runtime step selected by a trusted profile. | Join only when migrating an existing writer; never add a new formatter call merely for parity. |
| Exact verifier fusion and rollback | The promoted product fell from 25.066s to 19.216s. Exact verification plus terminal handling removed 5.850s, and failed verification proved rollback. | Verification and rollback differ by operation; arbitrary process semantics remain part of CLI compatibility. | `verify="exact"` selects one project-owned closed argv, runs after staged read-back, and rolls back every non-pass. | Shared verifier/rollback primitive; entrance policy selects whether and how it is used. | Migrate one already-exact CLI mutation only after `change!` parity. |
| Terminal mutation response | Mean receipt interpretation fell from 6.590s to 1.533s; 89% of the improvement localized to deleted narration. | No shared terminal-response contract; concise EDN is operation-specific. | A qualifying exact mutation may return a constant conditional terminal response. | Canonical mutation-terminal fact; transport-specific presentation. | Preserve CLI bytes initially; expose only behind a versioned CLI mode after measurement. |
| Receipts and inverse | The successful one-shot route returns committed-byte hashes and an inverse receipt; failure paths use the receipt to prove rollback or safe undo. | Several legacy receipt schemas and explicit `:receipt-out`; undo commands are public. | Rich normalized transaction receipt plus structured undo evidence. | Shared internal receipt facts with compatible legacy and MCP projectors. | Golden receipt and undo characterization before refactoring. |
| Uniform elapsed clocks | Across 188 operations, direct median was 0.243s while the following agent boundary was 8.901s. CLI direct median was reported at 0.665s; CLI-to-CLI boundaries were 6.464s. | Process wall and operation work are not uniformly distinguished. | The public envelope reports server-owned request time, excluding transport and model wall. | Inner operation clock in canonical outcome; CLI process startup remains separately owned. | Add the inner clock to shadow evidence first; do not change legacy stdout yet. |
| Typed refusal evidence | Safe bookkeeping refusals still cost another roughly nine-second model boundary. | Error maps, exit status, stderr, and remedies vary across operations. | Shared summaries include stable error type, source truth, next action, and bounded continuation evidence. | Canonical refusal classification and factual evidence; projections retain transport law. | Include success, stale refusal, typed refusal, verifier failure, and unexpected exception in the first differential corpus. |
| Exact owner vocabulary and hypotheses | Enriched evidence made hard rank-7 recovery 26.8% faster and eliminated native discovery, but made easy rank-1 cases 15.5% slower. The crossover is binding. | Exact selector recovery already consumes the shared owner-hypothesis compiler. | Same compiler plus MCP visible/structured projections. | Keep the existing transport-neutral compiler. | No migration work; treat it as the proven pattern. |
| Read batching / compiled dossier | There were 75 Surgeon-read-to-Surgeon-read transitions consuming 1,084s. Direct tool median was 0.243s; next action arrived around 9.1s later. Earlier generic batched reads and continuation shapes missed their keep gates. | Manifest reads and powerful CLI-only structural operations exist, but every process boundary is cold. | Persistent `inspect_clojure` batches exact reads; median observed batch width was one. | Shared snapshot capture/evaluation substrate; a compiled dossier remains an experiment. | Oracle upper-bound experiment first. Do not migrate or add a generic graph yet. |
| SCI / computed programs | SCI remained nearly constant across repeated edit counts, but lost the one-site race and can refuse comment-bearing transformations. | `:xray`/`:edit` expose the existing program language. | `transform_clojure` and compact `programs` expose computed lowering. | Shared pure program compiler into addressed actions. | Option-value parity only after a representative computed corpus wins. |
| Syntax-first Var surface | Three LSP sessions served four requests; 83.46% of LSP wall was initialization. Syntax was terminal for two of four retained cases; bounded clj-kondo reproduced all four. | No unified public Var-surface operation. | Internal semantic preparation can escalate to cclsp. | Future evidence provider below both entrances, with exact gaps and guarded escalation. | Separate 20-request experiment; not envelope slice 1. |
| Analyzer admission | A five-launch convoy in 48.658s helped drive load from 20.7 to 118.5. A naive cooldown would make a 32-launch suite take at least 31 minutes. | Stable install now supplies a machine-wide `clj-kondo` entrance. | Surgeon-owned verifier and analysis processes use the admission layer. | One host-wide process boundary below both projections. | Finish the real two-process concurrency gate independently; envelope work must not add launches. |
| CLI-only structural operations | Useful operations include CLJC split/merge, dependency movement, namespace rename, declare repair, tree/topology, plan files, and explicit undo. Their performance distribution is mixed and not all need MCP exposure. | Public and compatibility-sensitive. | Not projected as public MCP tools. | Join shared kernels/outcomes where earned; remain CLI-only projections. | Migrate one at a time after the shared outcome proves itself. |
| MCP-only preparation and jobs | Persistent basis preparation, strict decision accounting, background verification, callback-once, JSON schema, and terminal summaries serve a connected agent session. | No corresponding need has been measured. | Public MCP behavior. | Remain MCP-only projections over shared facts. | Do not add CLI flags until field demand appears. |
| Utility control plane | `up`, `recover`, `report-failure`, onboarding, and routing manage services and installations rather than source operations. | Separate direct mains and commands. | Typed service control exists elsewhere. | Separate utility plane. | Explicitly out of the first operation-algebra migration. |

## Policy gap table

| Policy seam | CLI compatibility law | MCP safety law | Convergence rule |
|---|---|---|---|
| Extraction callers | Legacy `extract!` may commit and report external callers for later review. | Every discovered caller must be changed or explicitly ignored before commit. | Share planner facts; keep `:cli-legacy` and `:mcp-strict` execution policies. Never make a sole candidate rewrite authority. |
| Verification | Preserve documented command-specific defaults and exit behavior. | Exact verification is a project-owned closed capability; generic `fast` may not substitute. | Share verifier primitives, not default policy. |
| Timing | Process startup, stdout delivery, and exit belong to the CLI caller. | Request execution excludes callback, network, serialization, and model wall. | Canonical outcome reports inner owned work; adapters retain their outer clocks. |
| Presentation | Stable EDN, stdout/stderr, exit status, help, aliases, stdin, and plan files are API. | Text content, `structuredContent`, JSON schema, and callback-once are API. | Never make one projection consume the other's presentation object. |
| Tool count | Twenty-nine canonical CLI operations and aliases are discoverable without a persistent catalog. | A deliberately small tool surface keeps connected-agent choice cheap. | Internal parity does not require public-tool parity. |
| Recovery | Receipt paths and explicit undo commands are durable CLI workflows. | Typed continuations and mutation-scoped terminal evidence optimize the connected session. | Share recovery facts; preserve the public workflow of each entrance. |

## Entrance and escape census

The current CLI launcher enters `clj-surgeon.core/-main`, parses argv/EDN, and
dispatches through `ops-registry`. The registry exposes 29 canonical
operations plus compatibility aliases across reads, plans, mutations, undo,
CLJC, namespace operations, movement, declare repair, structural programs, and
tree analysis.

Known escape hatches must be characterized before migration:

- `up`, `recover`, and `report-failure` bypass the source-operation registry;
- `run-ls-tree` calls `System/exit` internally rather than returning a domain
  outcome;
- agent-routing and workspace-onboarding have direct utility mains;
- HTTP MCP, stdio MCP, nREPL, and test runners are separate process entrances;
- CLI `ls` enriches forward-reference evidence while MCP outline deliberately
  does not claim it.

Before code changes, add a registry-derived operation/alias census and
resolved-reference inventories for every source writer, receipt publisher, and
internal `System/exit`. A seam protects only callers that actually use it.

## Architecture options

| Option | Benefit | Cost / failure mode | Decision |
|---|---|---|---|
| CLI loops through HTTP or stdio MCP | Maximum facade reuse | Adds server availability, transport failure, JSON conversion, startup, and MCP policy to a native process | Reject |
| CLI invokes current MCP handlers in-process | Fast prototype; reuses some result code | Couples CLI to callbacks, JSON normalization, four-tool vocabulary, eager MCP dependencies, and strict policy | Shadow experiment only; reject as destination |
| One operation algebra, trusted profiles, thin projections | Reuses earned mechanics while preserving compatibility and permits one-operation rollback | Requires explicit outcome and policy contracts | **Recommend** |
| One universal tagged request schema | Superficially uniform | Giant union erases planner invariants and forces a big-bang public rewrite | Reject |
| Keep shared kernels but duplicate outcomes forever | Lowest immediate change | Timing, refusal, receipt, writer, and terminal-evidence drift remain | Honest fallback if the first parity experiment fails |

## Safe migration ratchets

Each ratchet is an independently green commit. The old route remains callable
for differential tests until the new route passes its cutover gate.

1. **Update intent before code.** Amend the HLD's CLI boundary, add one owning
   operation-algebra LLD, and activate only the missing EARS guarantees.
2. **Characterize the public boundaries.** Derive the operation and alias
   census from `ops-registry`. Add golden CLI subprocess tests for EDN,
   stdout/stderr, exit status, help, aliases, stdin, receipts, and undo. Add
   writer/receipt-publisher allowlists.
3. **Extract canonical outcome classification.** Move pure classification and
   shared factual evidence below MCP presentation. Keep serialization,
   callbacks, summaries, and CLI output in their adapters.
4. **Falsify on `change` preview.** Compile one retained intent through the old
   route and the proposed operation catalog. The old path remains authority.
5. **Cut over `change!` only after parity.** Require identical future bytes,
   hashes, receipt/undo behavior, compiler count, and commit count.
6. **Move compact lowering.** Extract MCP compact edits, programs, and owner
   deletion into small pure operation compilers that emit the existing IR.
   Preserve current CLI `:edit` and `:change!` inputs.
7. **Converge the mutation runtime one operation at a time.** Join staged
   formatting, exact verification, rollback, read-back, receipt, and inverse
   only when the current operation's compatibility matrix is green.
8. **Migrate extraction last among proven mutations.** Make the strict versus
   legacy caller-accounting cross-product permanent before any cutover.
9. **Herd remaining writers and exits.** Convert internal exits to outcomes and
   move CLJC, rename, movement, declare repair, and other CLI-only operations
   individually when evidence pays for it.
10. **Keep reads and semantic escalation experimental.** A compiled dossier
    must clear its 30% oracle gate. Syntax-first Var evidence must clear its
    20-request exactness and cold-wall gates before joining the algebra.

## Boundary witnesses before implementation

- Registry-derived census of every canonical CLI operation and alias, plus
  every MCP tool/outcome class.
- Dual-projection fixtures for success, typed refusal, stale refusal,
  verification failure/pending, and unexpected exception.
- Exact CLI subprocess compatibility for EDN, stdout/stderr, exit code, help,
  aliases, stdin/spec input, receipt publication, and old-receipt undo.
- Extraction cross-product: no caller, remaining private source, external
  caller, quoted Var, explicit change, explicit ignore, omitted decisions,
  stale source, and wrong expectations.
- Mutation failure matrix: formatter failure, partial write, verifier failed,
  verifier unverified, receipt publication failure, and undo drift.
- Identical future-source hashes for one compact decision through both
  projections.
- Permanent architecture guard: no new direct source writer, receipt
  publisher, analyzer entrance, or internal `System/exit`.

## Exhaustive CLI versus MCP performance experiment

The convergence design must not assume that MCP is faster. The current evidence
is mixed:

- a four-read matched study measured persistent MCP at 27.969s versus CLI at
  32.442s: only 13.8% faster, below its 30% keep gate, although MCP used one
  tool action instead of ten shell commands and returned 27.4% fewer bytes;
- natural tiny-edit routing measured MCP at a pooled 20.665s versus native
  `Read + apply_patch` at 18.510s. This is a native-positive boundary, not a
  CLI-positive comparison;
- the closest observed installed-skill mutation routes measured CLI at 82.519s
  and 12 actions versus MCP at 52.275s and three actions, but CLI also paid
  skill, reference, help, and quoting ceremony. It is product-route evidence,
  not an isolated transport result;
- the latest event census measured an MCP direct-action median of 0.243s and a
  CLI direct-action median of 0.665s, but those are observational strata rather
  than a matched causal comparison;
- CLI mutation commands have completed in 0.643--2.025s while complete agent
  turns took 79.297s or more, showing that quoting, materialization,
  interpretation, and model boundaries can dwarf process execution;
- the cross-caller MCP extraction now produces 6.37x--9.69x matched gains over
  native editing, but there is not yet a same-kernel CLI extraction control.

The experiment asks two separate questions:

1. What complete verified task-time advantage does persistent typed MCP have
   over stable CLI when both execute equivalent kernel intent?
2. Which causal component creates any advantage: typed arguments, shell/EDN
   quoting, process startup, hot caches, tool discovery, output size,
   structured evidence, terminal response, or fewer recovery actions?

### Frozen task matrix

| Stratum | Why it belongs | Required equivalence proof |
|---|---|---|
| Batched exact read | Existing matched evidence is only 1.16x and missed its keep gate. | Identical owners/source hashes and unique evidence; same answer scorer. |
| One compact exact edit | CLI can be positive on tiny work; prevents an MCP-shaped portfolio. | Identical future bytes/hash, same guards, same proportional verification. |
| Heterogeneous multi-owner transaction | Tests quoting and large nested EDN/JSON payload materialization. | Same addressed actions, future bytes, receipt facts, and semantic scorer. |
| SCI/computed program | Shell quoting is plausibly the largest CLI tax here. | Same compiled program IR, selected nodes, exact counts, and future hashes. |
| Dependency-minimal extraction | Highest-payoff real task; tests persistent runtime, formatter, verifier, rollback, and terminal evidence. | Same planner facts and trusted policy. Use only a no-caller case first; do not pool strict and legacy caller policy. |
| Selector refusal and exact retry | Tests visible recovery evidence and result interpretation. | Same candidate universe, authority labels, selected owner, and final source read. |
| Exact verification failure and rollback | Tests whether one transport needs an extra model action to prove the same terminal state. | Same verifier argv/exit, restored bytes, receipt absence/presence, and retry law. |
| Undo | Tests durable receipt ergonomics without hiding file or process ceremony. | Same inverse actions and restored hashes. |

### Measured intervals

Every arm records:

- complete process/turn wall and model/effort identity;
- action count and transport transitions;
- time to first tool call;
- request bytes, escaping expansion, and input/output tokens;
- CLI process startup and kernel wall, or MCP observer and server-authoritative
  wall;
- parser/compiler, formatter, verifier, commit/read-back, and receipt phases;
- time from result arrival to the next action or final response;
- failed calls, refusals, retries, shell parsing failures, and fallback;
- semantic correctness, exact future hashes, receipt/inverse hashes, and
  proportional verification result;
- MCP discovery action and cache state; CLI command construction and stdout
  parsing actions;
- Anvil CPU, RSS, process count, and analyzer admission so transport comparison
  does not conceal a load convoy.

The shell-escaping hypothesis is measured, not narrated. For every CLI request,
retain the intended EDN value, rendered argv/stdin bytes, shell program bytes,
and the value parsed by the CLI. Classify any difference as model
materialization, shell quoting, CLI parsing, or kernel behavior. MCP retains the
analogous intended JSON object, transmitted object, normalized request, and
compiled intent.

### Cohort design

1. Start with one serial, same-model Sol/high pair on the batched read, compact
   edit, SCI program, and no-caller extraction strata.
2. Use a fresh isolated workspace per arm and the exact tagged product. Run a
   fresh isolated MCP server with a 512MiB cap; never use shared `:7888`.
3. Counterbalance CLI-first and MCP-first in the second batch only after both
   pilot arms are correct and route-adherent.
4. Promote a stratum to four replicas per arm. Do not pool task shapes,
   callers, cold/warm state, or policy profiles.
5. Add Fable/high only after the Sol harness proves both surfaces; Claude's
   deferred `ToolSearch` remains visible and counted.
6. Run on Anvil under the machine-wide analyzer gate. Refuse or defer when the
   pressure sample is stale/red; do not overlap arms to save calendar time.

### Keep and stop laws

- Correctness and route adherence precede timing. An incorrect arm supplies no
  speed denominator.
- A claimed MCP advantage requires at least 20% lower matched complete wall in
  the task stratum or a material correctness/recovery advantage at equal wall.
- A claimed CLI advantage is retained honestly and becomes routing guidance;
  the architecture does not force MCP adoption.
- Kernel wall must be equivalent within normal variance. A kernel difference
  means the experiment compared implementations, not entrances.
- No result is attributed to shell escaping unless retained intended/rendered/
  parsed values localize the failure or added model actions.
- No hot-cache claim pools fresh and warm runs.
- Stop a stratum after two correct counterbalanced pairs show less than 10%
  complete-wall difference and identical action geometry; record parity.
- Any CLI/MCP convergence code remains blocked until the shadow parity
  experiment below passes. Benchmark results may change routing before they
  justify architecture.

### Possible outcomes

| Finding | Consequence |
|---|---|
| MCP wins mainly through fewer model actions and structured terminal evidence | Move canonical outcome/receipt facts below both projections; preserve CLI but route agents to MCP for that stratum. |
| MCP wins mainly through hot process/cache state | Keep persistent MCP as the hot entrance; do not burden CLI with service semantics. Consider a daemon only if CLI field demand proves it. |
| MCP wins mainly through quoting avoidance | Add a typed CLI stdin/file/request mode over the shared compiler; do not force shell-literal payloads. |
| CLI wins tiny edits or simple reads | Preserve the selective CLI/native hot lane and document the crossover. |
| Results are equal | Pursue convergence for cost of change only if the shadow refactor is cheap; do not claim a performance feature. |
| Surfaces cannot express equivalent policy | Keep the stratum separate and first close the capability gap with pure shared facts, not benchmark prompt tricks. |

SURGEON2 owns the initial retained-evidence audit and smallest safe Anvil pilot.
The first receipt must include all failed harness attempts, exact hashes, and a
statement of which cells remain incomparable.

## Smallest falsification experiment

Shadow one nonmutating `change` compile through a proposed operation catalog
and canonical outcome while the current CLI remains authoritative. If that
passes, run a bounded `change!` parity corpus from the historical multi-owner
cleanup.

Continue only when all are true:

- compiled future bytes and hashes are identical;
- CLI stdout, stderr, exit status, receipt, and undo remain compatible;
- exactly one compiler and one commit occur;
- no analyzer, formatter, or verifier is added;
- CLI subprocess p50 and p95 regress by no more than 5%;
- no new source writer or receipt publisher appears.

If the gate fails, stop at shared kernels and duplicate projections. Do not
proceed to extraction, SCI, compiled dossiers, or semantic admission.

## What this enables

If the first ratchets pass, high-payoff wins stop being MCP-only product
features and become reusable operation capabilities:

- CLI can opt into compact multi-owner gestures without reproducing MCP
  normalization;
- exact verification, rollback, read-back, and inverse receipts can be
  selected once without changing CLI process semantics;
- elapsed and refusal evidence become comparable without conflating transport
  clocks;
- future compiled read dossiers and syntax-first Var evidence can project to
  both entrances from one proof implementation;
- improvements are tested once at the kernel and once per projection, which
  lowers the cost of the next hill climb.

That is the Kent Beck payoff. The first slice is intentionally small not because
the ambition is small, but because a cheap reversible change gives the fleet
more options for every experiment that follows.

## Sources

- [One compiler, two entrances](one-compiler-two-entrances.md)
- [High-level design](../high-level-design.md)
- [MCP operation contract design](../intent/mcp-operation-contract/mcp-operation-contract-design.md)
- [Three-day speed option portfolio](../observations/2026-08-26-three-day-speed-option-portfolio.md)
- [Terminal proof ended the second plan](../observations/2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md)
- [The model boundary dwarfed the scalpel](../observations/2026-08-27-captains-log-the-model-boundary-dwarfed-the-scalpel.md)
- [Nine seconds is the agent boundary](../observations/2026-08-27-captains-log-nine-seconds-is-the-agent-boundary-not-surgeon.md)
- [The language server was mostly starting itself](../observations/2026-08-27-captains-log-the-language-server-was-mostly-starting-itself.md)
- [Var surface was a small index](../observations/2026-08-27-captains-log-var-surface-was-a-small-index.md)
- [Analyzer trigger taxonomy](../observations/2026-08-27-clj-kondo-trigger-taxonomy-and-test-pyramid.md)
- [Cross-caller benchmark](cross-caller-mcp-extraction-benchmark.md)
