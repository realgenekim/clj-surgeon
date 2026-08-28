---
parent: high-level-design
prefix: OP-ALG
---

# Operation Algebra

## Context and Design Philosophy

The CLI and MCP service expose different public contracts over overlapping
structural capabilities. The CLI accepts argv and EDN, preserves aliases,
stdout, exit status, explicit receipt paths, and durable undo workflows. MCP
accepts typed JSON, confines work to a canonical workspace, applies strict
budgets and project-owned verification policy, and returns structured terminal
evidence through a persistent session.

Both entrances already converge on substantial pure kernels. In particular,
CLI `change` and `change!` and MCP direct changes ultimately compile through
`intent-transaction/compile-transaction`. A no-model differential on the same
source snapshot proved byte-identical transaction EDN, compiled intent, and
future hashes. The remaining duplication is at the boundary: operation
selection, policy, effect declarations, outcome classification, and public
projection are not represented as one explicit internal contract.

This component makes that shared contract explicit without making either
public entrance consume the other. It treats the existing transaction compiler
as authority and adds only the minimum data needed to dispatch it, constrain
effects, and project its result truthfully.

The first vertical slice is `change` preview followed by `change!`. No other
operation moves into the algebra until that slice preserves public behavior,
proves one compiler and one commit, and creates no measurable CLI regression.

## Component Boundary

```text
CLI argv / EDN decoder                 MCP JSON normalizer
          │                                     │
          └────── trusted entrance context ─────┘
                                │
                     operation catalog entry
                                │
                    small change front end
                                │
              intent-transaction/compile-transaction
                                │
                  existing compiled transaction
                       ┌────────┴────────┐
                    preview           commit
                                         │
                             one transaction runtime
                       ┌─────────────────┴─────────────────┐
                canonical outcome facts            private future state
                       ┌─────────────────┴─────────────────┐
                 CLI projection                     MCP projection
```

Dependencies point downward. Adapters may depend on the operation algebra.
The operation algebra may depend on pure structural and transaction contracts.
It does not depend on CLI parsing, MCP JSON, callbacks, summaries, shell
commands, global workspace state, or transport-specific configuration.

## Canonical Operation Context

The adapter constructs the operation context after public decoding. It is
trusted internal data, not part of either public request:

```clojure
{:operation :change
 :operation-version 1
 :entrance :cli | :mcp
 :policy :cli-legacy | :mcp-strict
 :lifecycle :preview | :commit}
```

The operation-specific front end receives this context, the canonical source
snapshot, and the existing transaction spec. It delegates compilation to
`intent-transaction/compile-transaction` and does not copy or rename the
compiled transaction's addressed actions, future sources, hashes, counts, or
diff into a second planner representation.

The adapter assigns `entrance` and `policy`. Public request fields named
`policy`, `profile`, `entrance`, `effects`, or equivalent do not select or
weaken authority. Each existing decoder applies its current unknown-field law.

The catalog entry has a closed minimum shape: canonical operation, operation
version, permitted lifecycles, maximum effects, compiler function, and
permitted trusted profiles. Canonical operation identity is `:change` for both
lifecycles. The CLI projection restores legacy `:operation :change` or
`:operation :change!` according to the lifecycle; MCP preserves its existing
public operation identity.

## Trusted Entrance Profiles

| Profile | Preserved contract |
|---|---|
| `:cli-legacy` | Existing operations and aliases; argv and EDN parsing; `:spec` and `:spec-file -`; stable stdout, stderr, and process exit behavior; explicit receipt paths; current verification defaults; public undo commands. |
| `:mcp-strict` | Canonical workspace confinement; request and output budgets; project-owned verification authority; strict decision accounting; structured and human evidence; server-owned receipts; callback-once publication; mutation-scoped terminal response. |

Profiles govern entrance policy, not domain judgment. They may constrain or
decorate an operation but may not change a supplied replacement, choose an
owner, infer a caller disposition, balance syntax, or widen a file set.

For the first differential, equivalent policy means the same canonical source
map, normalized transaction spec, source and result budgets, no programs, no
extraction, no formatter decoration, and no verifier. Canonical paths are
normalized before comparison; receipt location is excluded from semantic
comparison. A later benchmark does not claim final-byte parity when one profile
intentionally formats, verifies, or rejects work that the other does not.

## Effect Authority

Operation category is help and discovery metadata. It never grants authority.
The operation catalog separately declares a closed set of possible effects:

- `:source-read`
- `:source-write`
- `:receipt-stage`
- `:receipt-publish`
- `:formatter-launch`
- `:verifier-launch`
- `:rollback`

The catalog owns the maximum effect set. The lifecycle allowance intersects
that maximum, and the trusted entrance profile narrows it again. The operation
runtime validates the resulting capability set before any effect:

```text
catalog maximum ∩ lifecycle allowance ∩ trusted entrance capability
  = runtime capability set
```

The adapter cannot add an effect, and public request data cannot supply the
trusted context. `change` preview permits only source reads. `change!` may
receive source-write, receipt-stage, receipt-publish, and rollback capabilities.
Formatter or verifier capabilities are present only when the trusted profile
selects an already-declared repository program. A request, operation name,
alias, bang suffix, or `:category :write` cannot add an effect. Process exit is
projection-only behavior owned by the outer CLI process adapter; it is not an
operation-runtime effect.

Operation front ends never execute effects. The existing transaction runtime
is the only source mutation and rollback authority. The change slice
allowlists `file-ops/atomic-write!` as the byte primitive,
`intent-transaction/commit-compiled!` as transaction authority, and
`stage-receipt!` plus `publish-staged-receipt!` as receipt authorities. The
ordinary suite inventories these boundaries and fails when an unallowlisted
writer, publisher, launcher, or lower-layer exit appears.

## Preview Lifecycle

1. The CLI or MCP adapter validates its public request shape.
2. The adapter assigns its trusted operation context.
3. The source shell reads every declared canonical file once.
4. The change front end invokes the existing compiler once.
5. The result is classified as a canonical success or refusal.
6. The entrance projects that outcome through its existing public contract.

Preview performs no formatting, verification, source mutation, receipt
publication, or retained mutation authority. A failed preview reports the same
domain phase, error type, source-state truth, and factual diagnostics before
the entrance renders them.

The current authoritative preview is
`intent-transaction/plan-change` through CLI dispatch. During shadow
comparison, the candidate catalog/front-end compiles against the same captured
source map and is observable only to differential tests. Cutover removes the
shadow rather than retaining a permanent compatibility path. Slice-1 MCP
preview exists only as a test normalization/compile path; this design adds no
public MCP preview mode.

## Commit Lifecycle

1. The adapter validates public and entrance-policy requirements before any
   write.
2. The source shell captures one canonical original snapshot.
3. The change front end compiles once against that snapshot.
4. The runtime builds and stages the inverse receipt. A staging failure is a
   pre-write refusal.
5. The transaction runtime validates hashes immediately before the first
   write.
6. It commits the complete file set, reads every file back, and verifies the
   result hashes.
7. It publishes at most one staged inverse receipt after successful read-back,
   then deletes the staging artifact.
8. A handled commit or receipt publication failure restores only
   transaction-owned bytes and reports proven rollback state.

A stale snapshot refuses before the first write. The runtime does not
automatically re-read and recompile newer bytes. Missing receipt evidence after
an interrupted mutation is unverified, never failed-and-safe-to-retry.

Post-commit MCP verification is an entrance-policy decoration over the shared
commit result, not part of slice-1 shared transaction state. Its pass, failure,
unverified, rollback, receipt-retention, and asynchronous-pending laws remain
owned by the sibling MCP operation contract. CLI receives no new verifier
behavior. Formatter decoration is likewise excluded from slice-1 equivalent
policy. Mapping either decoration into a later shared outcome requires a
separately reviewed state matrix.

## Canonical Outcome

The canonical outcome contains domain facts that both projections can preserve:

```clojure
{:operation :change
 :operation-version 1
 :status :ok | :refused | :failed | :unverified
 :phase :select | :snapshot | :compile | :receipt-stage | :commit
        | :read-back | :receipt-publish | :verify | :rollback
 :source-state :unchanged | :committed | :restored | :unknown
 :counts {...}
 :files [{:file ... :source-hash ... :result-hash ...}]
 :verification {...}
 :receipt {...}
 :inverse {...}
 :effects {:declared #{...} :observed [...]}}
```

Fields that do not apply are absent rather than filled with simulated values.
Complete original and future source bodies remain private compiler/runtime
state. The canonical outcome does not contain shell argv, stdin, stdout,
stderr, exit status, JSON, callback state, human summaries, MCP request clocks,
or CLI process clocks.

Transport and decode failures remain projection-owned. Canonical outcomes
begin only after successful public decoding and trusted-context assignment.
`:source-state :unchanged` requires proof that no source write occurred;
`:committed` requires result-hash read-back; `:restored` requires original-hash
read-back after rollback; `:unknown` is used whenever those claims cannot be
proved. A projection may map these values into legacy booleans and fields, but
it may never upgrade `:unknown`.

### Slice-1 legal terminal states

This table is exhaustive only for synchronous equivalent-policy preview and
commit, where formatting and verification are absent. It does not replace the
sibling MCP operation contract's verifier or cold-job state machines.

| Terminal case | Status | Phase | Source state | Receipt/inverse | Required observed effects |
|---|---|---|---|---|---|
| Preview success | `:ok` | `:compile` | `:unchanged` | absent | source reads only |
| Selection/compile/stale refusal | `:refused` | failing pre-write phase | `:unchanged` | absent or staged artifact proved removed | no source write/publish |
| Receipt-stage refusal | `:refused` | `:receipt-stage` | `:unchanged` | publication absent | receipt staging attempt only |
| Commit success | `:ok` | `:receipt-publish` | `:committed` | published inverse present | one bounded commit and at most one publish |
| Handled write or publication failure, rollback proved | `:failed` | `:rollback` | `:restored` | removal state reported from evidence | write attempt plus guarded rollback |
| Interrupted or rollback-unproved mutation | `:unverified` | last observed phase | `:unknown` | never implies safe retry | only effects actually observed |

Only these combinations are legal for slice 1. Projections copy facts from
them; they do not synthesize contradictory status, phase, source, or receipt
claims.

The first slice derives this outcome from the existing compiler/runtime result.
It does not alter compiler semantics to make outcomes look more uniform.

## Public Projections

### CLI

The CLI projection preserves the current `change` and `change!` EDN maps,
stdout/stderr placement, nonzero error exit, aliases, help, stdin and spec-file
behavior, explicit receipt path, receipt bytes, and undo compatibility. New
canonical fields are not added to legacy output merely because they exist
internally. A future versioned output mode requires separate field demand and
intent.

### MCP

The MCP projection preserves workspace confinement, strict request
normalization, public schema, callback-once behavior, structured content,
concise summary, elapsed server work, verification evidence, and conditional
terminal mutation response. The operation algebra does not add an MCP tool or
make CLI-only operations public through MCP.

## Behavior Matrix

| Case | CLI legacy projection | MCP strict projection | Shared invariant |
|---|---|---|---|
| Valid `change` preview | Existing compact EDN plan and exit 0 | Test-only normalization/compile path; no public MCP preview | Identical compiler input, addressed actions, future bytes, hashes, counts, and diff; zero write effects. |
| Invalid schema | Existing EDN error and nonzero exit | Typed structured refusal | Same domain phase, error type, factual diagnostics, and unchanged source when the policy intersection is equivalent. |
| Match or count mismatch | Existing refusal | Existing refusal | Same failed intent, address, and count evidence. |
| Valid `change!` without extra verifier | Explicit receipt path | Confined server receipt path | One compile, one commit, identical candidate bytes under equivalent policy. |
| MCP exact verification | No new CLI behavior | Existing strict exact profile | Verification decorates the transaction and cannot alter compiled intent. |
| Profile-owned formatting | Preserve existing CLI behavior | Preserve existing MCP behavior | Compare the pre-policy compiled candidate; do not claim final-byte parity across different policies. |
| Stale source | Nonzero refusal | Typed refusal | Zero writes and no automatic recompilation. |
| Write failure | Existing CLI rollback/result | Existing MCP rollback/result | Same transaction-owned restoration facts. |
| Receipt publication failure | Existing CLI exit and recovery facts | Existing typed result | At most one publication attempt; rollback truth preserved. |
| Caller supplies authority fields | Existing unknown-argument behavior | Strict schema refusal | Caller gains no authority. |
| CLI alias, stdin, or spec file | Preserved exactly | Not applicable | Decoder-only behavior does not fork compiler semantics. |
| MCP workspace or budget violation | Not applicable | Strict pre-compile refusal | Internal convergence does not weaken MCP policy. |
| Undo | Existing CLI workflow | No new MCP tool | Public-tool parity is not required. |

## Architecture Guards

The ordinary suite derives and freezes these facts:

- 29 canonical CLI operations and nine aliases remain resolvable;
- category metadata is never read as mutation authority;
- each migrated catalog entry declares lifecycle and possible effects;
- operation front ends do not write, format, verify, roll back, publish, or
  depend on MCP transport;
- the allowlisted change compiler, writer, and receipt publishers are unique;
- preview reaches no writer or receipt publisher;
- stale and refused commits reach no writer or receipt publisher;
- successful `change!` invokes one compiler, one transaction commit, and at
  most one receipt publication;
- no `System/exit` occurs below a public process adapter.

The existing `run-ls-tree` lower-layer exits are a known catalog-convergence
defect. They do not block the isolated change slice, but full catalog migration
cannot complete until they return a domain outcome instead.

## Cutover and Rollback Gates

The migration advances as independent green ratchets:

1. Characterize current CLI registry, parsing, output, exit, receipt, undo,
   writer, and publisher behavior.
2. Prove no-model decoder/normalizer parity for one retained `change` fixture.
3. Compile the current and candidate preview against the same source map while
   the old route remains authority; execute no candidate effect.
4. Cut over preview only after public output and canonical compilation are
   identical.
5. Shadow `change!` compilation on success, refusal, stale source, write
   failure, receipt failure, and undo. Execute at most one authoritative live
   commit. Exercise candidate effects only with injected fake I/O or an
   isolated copied workspace; never publish two receipts for one live request.
6. Cut over commit only after future bytes, hashes, exact deterministic legacy
   receipt bytes/hash, inverse facts, compiler count, commit count, and effect
   traces are identical. Prove old receipts work with new undo and new receipts
   work with pre-cutover undo, or prove receipt schema bytes are unchanged.
7. Delete the shadow route after the cutover gate passes.

Every ratchet is a separately reversible commit. Rollback means reverting to
the previous green commit and rerunning exact stdout/stderr/exit and receipt/
undo boundary witnesses; it is not a runtime flag or permanent fallback path.
The cutover adds no analyzer,
formatter, or verifier launch and may regress CLI subprocess p50 and p95 by no
more than five percent.

## Performance and Experimental Contract

The no-model `change` differential is the architecture acceptance oracle.
Separately, a same-candidate batched-read AB qualifies the transport benchmark
harness. Only after harness qualification may a `change` transport cohort
measure performance. No timing result authorizes algebra cutover; semantic and
public compatibility do.

Transport performance is measured only after semantic parity. The benchmark
uses a candidate-owned CLI wrapper and isolated MCP server from the same exact
clean checkout. Before either arm it freezes the candidate Git commit and
clean-tree status, candidate CLI wrapper hash, CLI provenance receipt hash and
source commit, isolated MCP source commit and launch artifact hash, fixture and
source hash, normalized request and semantic-fact hashes, prompt, scorer,
model, reasoning effort, harness commit, cold/warm state, and run order.
Mutation receipt hashes are outputs, not arm identity. CLI requests travel through
`:spec-file -`; the harness retains intended EDN, stdin bytes, parsed request,
intended JSON, transmitted JSON, normalized request, compiled intent, and
future hashes.

Harness qualification is one serial same-model batched-read pair owned by the
CLI/MCP causal transport experiment. A speed ratio is published only when
semantic hashes match. A route recommendation requires at least 20 percent
lower complete wall or a material correctness/recovery advantage. Two correct
counterbalanced pairs within ten percent are recorded as parity, not tuned into
a win.

## Explicit Non-Goals

- CLI over HTTP, stdio MCP, JSON, or current MCP handlers.
- One universal request schema or equal public operation counts.
- Caller-selected entrance policy or effects.
- A second transaction representation that copies existing compiled fields.
- Extraction policy convergence in the first slice.
- New CLI verification defaults or a new MCP undo tool.
- Compiled read dossiers, syntax-first semantic replacement, or analyzer
  scheduling inside this component.
- A performance claim from observational or different-commit controls.

## Decisions & Alternatives

| Decision | Chosen | Alternatives considered | Rationale |
|---|---|---|---|
| Shared boundary | Operation algebra and canonical outcome below both adapters | CLI over MCP; in-process MCP handler reuse; duplicated envelopes | Reuses proven compiler facts without adding transport failure or coupling CLI to MCP policy. |
| Policy ownership | Trusted entrance profiles | Caller field; one weakened common policy | Preserves CLI compatibility and MCP safety; request data cannot grant authority. |
| Effect authority | Closed declarations plus allowlisted runtime capabilities | `:category :write`; bang suffix; handler naming convention | Current categories mix previews and writers; naming does not prove reachable effects. |
| First operation | `change`, then `change!` | Extraction; compact editor; all operations | Both entrances already share the intent compiler, making this the cheapest falsifier. |
| Canonical compiler shape | Reuse `compile-transaction` result | New universal transaction map | The existing shape already proved byte-identical across entrances. |
| Public parity | Shared facts with separate projections | Equal schemas and tool counts | Internal truth can converge without breaking caller-native contracts. |
| Migration | Shadow, differential gate, cut over, delete shadow | Big-bang rewrite; permanent feature flag | Small green ratchets maximize reversal and prevent parallel runtimes. |

## Open Questions and Future Decisions

### Resolved

1. The CLI remains a native process entrance and does not require the MCP
   service.
2. Category metadata remains presentation-only.
3. The first slice uses the existing intent transaction rather than another
   compiler.
4. CLI and MCP retain distinct verification and extraction policies.
5. Benchmark arms must use the same candidate commit before timing.

### Deferred

1. Which later operation earns migration after `change!`.
2. Whether a versioned CLI output mode should expose canonical terminal fields.
3. Whether public undo should ever be projected through MCP.
4. How full-catalog migration removes the two lower-layer `run-ls-tree` exits.
5. Whether shared exact verification becomes an entrance-neutral capability
   after a separate compatibility study.

## References

- [High-level design](../../high-level-design.md)
- [CLI and public operation envelope gap analysis](../../plans/cli-public-operation-envelope-gap-analysis.md)
- [One compiler, two entrances](../../plans/one-compiler-two-entrances.md)
- [MCP operation contract](../mcp-operation-contract/mcp-operation-contract-design.md)
- [CLI/MCP causal transport parity receipt](../../observations/2026-08-27-cli-mcp-causal-transport-parity-receipt.md)
