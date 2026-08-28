---
parent: operation-algebra-design
prefix: OP-ALG
---

# Operation Algebra Requirements

## Catalog and Trusted Context

- [x] **OP-ALG-CATALOG-001**: The operation-algebra catalog entry for canonical operation `:change` version 1 shall declare its permitted lifecycles, maximum effects, compiler function, and permitted trusted entrance profiles.
- [x] **OP-ALG-CONTEXT-001**: When a CLI or MCP adapter successfully decodes a public change request, the adapter shall assign the entrance, policy, and lifecycle from trusted internal configuration rather than from request data.
- [x] **OP-ALG-CONTEXT-002**: When a recognized public operation spelling, alias, or project-owned profile requests behavior, the trusted decoder, catalog, and profile validator shall assign the lifecycle and capability; raw caller data shall never grant or expand capability, and unknown or forged authority fields shall preserve the entrance's existing refusal behavior.
- [x] **OP-ALG-IDENTITY-001**: While the operation algebra processes either preview or commit lifecycle for the first slice, it shall use canonical operation identity `:change`; each public projection shall restore its existing public operation identity.

## Effect Authority

- [x] **OP-ALG-EFFECT-001**: When the operation runtime derives a capability set, it shall intersect the catalog maximum, lifecycle allowance, and trusted entrance capability; it shall reject an unknown operation version, lifecycle, or profile before compilation and shall refuse any required or attempted effect absent from the computed set before that effect executes.
- [x] **OP-ALG-EFFECT-002**: While the `:change` preview lifecycle is active, the operation runtime shall permit source reads and shall permit no source write, receipt stage, receipt publication, formatter launch, verifier launch, or rollback effect.
- [x] **OP-ALG-EFFECT-003**: While the `:change` commit lifecycle is active, the operation runtime shall execute effects only through the allowlisted transaction writer, receipt stage/publish functions, rollback function, and trusted entrance decorations declared for that lifecycle.
- [x] **OP-ALG-EFFECT-004**: The operation algebra shall treat CLI process exit as projection-owned behavior and shall not expose process exit as an operation-runtime capability.
- [x] **OP-ALG-EFFECT-005**: The ordinary operation-algebra architecture witness shall inventory the Vars invoked by the preview and commit entry points and shall fail if `:category` metadata is read as effect authority or if an unallowlisted writer, receipt publisher, formatter, verifier, rollback path, or lower-layer process exit appears in that bounded inventory.

## Pure Compilation and Preview

- [x] **OP-ALG-COMPILE-001**: When either trusted entrance submits an equivalent-policy change request over the same canonical source map, the shared change front end shall invoke `intent-transaction/compile-transaction` once and shall preserve identical addressed actions, future sources, hashes, counts, and diff.
- [x] **OP-ALG-PREVIEW-001**: When the trusted CLI entrance requests `change`, the operation algebra shall return the existing preview domain facts with source state `:unchanged` and no observed effect other than source reads.
- [x] **OP-ALG-PREVIEW-002**: Before preview cutover, a retained differential witness shall prove that the current and candidate compilers consume the same captured source map, that the current route alone is public authority during comparison, and that the candidate executes no effect.
- [x] **OP-ALG-REFUSE-001**: If change selection or compilation refuses before a source write, then the canonical outcome shall be `:refused`, shall name the failing pre-write phase, shall report source state `:unchanged`, and shall report no source-write or receipt-publish effect.
- [x] **OP-ALG-STALE-001**: If the immediate pre-write guard read finds a stale source hash, then the operation runtime shall refuse without recapturing the snapshot, recompiling newer bytes, writing source, or publishing a receipt.

## Commit, Receipt, and Rollback

- [x] **OP-ALG-COMMIT-001**: When the trusted commit lifecycle receives a valid compiled change, the operation runtime shall build and stage one inverse receipt before the first source write, revalidate source hashes, commit the complete file set once, read back result hashes, publish the staged receipt at most once, and remove the staging artifact.
- [x] **OP-ALG-COMMIT-002**: If inverse-receipt staging fails, then the operation runtime shall refuse before a source write and shall not attempt receipt publication.
- [x] **OP-ALG-COMMIT-003**: If a handled source-write, read-back, or receipt-publication failure occurs after a source write and rollback restores every transaction-owned original hash, then the canonical outcome shall be `:failed` with phase `:rollback` and source state `:restored`.
- [x] **OP-ALG-COMMIT-004**: If an interruption, rollback, or source read-back cannot prove the resulting source bytes after a source-write attempt, then the canonical outcome shall be `:unverified` with source state `:unknown` and shall not recommend blind retry.
- [x] **OP-ALG-RECEIPT-001**: When the compiled intent, original and future hashes, and receipt path are unchanged across the cutover, the new commit route shall preserve exact legacy receipt bytes and hash, and the existing undo route shall accept the new receipt.
- [x] **OP-ALG-RECEIPT-002**: Before commit cutover, the compatibility witness shall prove that a pre-cutover receipt is accepted by the new undo route and that a new receipt is accepted by the pre-cutover undo implementation, or shall prove exact receipt schema bytes are unchanged.
- [x] **OP-ALG-RECEIPT-003**: After any terminal path that staged an inverse receipt, the operation runtime shall remove the staging artifact when possible and shall claim removal only after proving it; unknown receipt cleanup shall not downgrade independently proved source state `:restored` to `:unknown`.
- [x] **OP-ALG-SHADOW-001**: Before commit cutover, a retained witness shall prove that comparison executes at most one authoritative live commit and publishes at most one live receipt and that candidate effect behavior runs only through fake I/O or an isolated copied workspace.
- [x] **OP-ALG-RUNTIME-001**: When one change commit succeeds, the operation-algebra entry shall invoke one shared compiler, one forward transaction commit, and at most one receipt publication, without constructing a second transaction representation.

## Entrance Decorations and Outcomes

- [D] **OP-ALG-VERIFY-001**: Where the trusted MCP entrance maps an existing project-owned verifier onto a shared commit outcome, the verifier shall remain a post-commit entrance decoration owned by the sibling MCP operation contract and shall not alter the compiled intent or add verifier behavior to CLI.
- [D] **OP-ALG-VERIFY-002**: Where the shared algebra later represents MCP verifier completion, it shall preserve the sibling MCP operation contract's pass, failure, unverified, rollback, receipt-retention, and asynchronous-pending laws without inventing a second verifier state machine.
- [x] **OP-ALG-OUTCOME-001**: The operation algebra shall emit only legal combinations of status, phase, source state, receipt/inverse evidence, and observed effects defined by the operation-algebra terminal-state table.
- [x] **OP-ALG-OUTCOME-002**: The canonical source-state algebra shall distinguish `:unchanged`, `:committed`, `:restored`, and `:unknown`; no projection shall upgrade `:unknown` to a proven source-state claim.
- [x] **OP-ALG-OUTCOME-003**: The canonical operation outcome shall omit complete source bodies, transport fields, and inapplicable fields rather than copying private compiler state or simulating evidence.
- [x] **OP-ALG-DECODE-001**: If CLI parsing or MCP schema decoding fails before trusted-context assignment, then the public adapter shall report that transport-owned failure without constructing a canonical operation outcome.

## Public Compatibility and Measurement

- [x] **OP-ALG-CLI-001**: When the CLI projects a canonical preview, commit, refusal, or failure outcome, it shall preserve the existing operation name, EDN shape, stdout/stderr placement, exit status, aliases, help, stdin, spec-file, receipt-path, receipt-byte, and undo behavior for that case.
- [x] **OP-ALG-MCP-001**: When MCP consumes canonical change facts, the operation algebra shall not alter the MCP projection behaviors owned by the sibling MCP operation contract, including confinement, budgets, callback-once publication, evidence, timing, verification, and mutation-scoped terminal response.
- [x] **OP-ALG-PARITY-001**: Before preview cutover, a no-model equivalent-policy differential (same canonical source map/spec/budgets, with programs, extraction, formatting, and verification absent) shall prove identical normalized transaction spec, compiled intent, future sources, hashes, counts, and diff for the retained apostrophe-bearing generic-change fixture.
- [x] **OP-ALG-PARITY-002**: Before commit cutover, isolated success, refusal, stale-source, write-failure, receipt-failure, and undo witnesses shall prove equal shared domain facts under equivalent policy and shall separately prove that each CLI and MCP projection remains exact against its own pre-cutover baseline, without two live commits.
- [x] **OP-ALG-PERF-001**: Before publishing a CLI-versus-MCP speed result, the harness shall retain evidence for the frozen candidate, clean-tree state, CLI wrapper and provenance receipt, MCP launch artifact, fixture/source, normalized request, semantic facts, prompt, scorer, model, reasoning effort, harness, declared cold-or-warm cache protocol, and run order for both arms.
- [x] **OP-ALG-PERF-002**: On the frozen apostrophe-bearing generic-change no-model cohort, using at least eight counterbalanced runs per arm after one declared warm-up, the operation-algebra cutover shall add no analyzer, formatter, or verifier launch and shall not regress CLI subprocess p50 or p95 by more than five percent.

## Deferred Expansion

- [D] **OP-ALG-EXPAND-001**: Where another public operation earns migration, the system shall add a separately reviewed catalog entry and parity matrix before routing that operation through the algebra.
- [D] **OP-ALG-CLI-002**: Where a versioned CLI canonical-output mode is approved, the CLI shall expose canonical terminal fields without changing legacy output mode.
- [D] **OP-ALG-VERIFY-004**: Where shared verifier policy is approved after a compatibility study, the system shall represent it as an entrance-neutral capability without changing existing CLI defaults.
