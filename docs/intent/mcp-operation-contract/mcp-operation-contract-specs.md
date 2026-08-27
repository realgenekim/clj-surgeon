---
parent: mcp-operation-contract-design
prefix: MCP-OP
---

# MCP Operation Contract Specifications

This file is the stable intent registry for the MCP operation-contract leaf.
IDs are never reused. The status marker records whether the current code and
tests witness the requirement.

## Finalized Results

- [x] **MCP-OP-RESULT-001**: When a public MCP handler produces a successful domain result, clj-surgeon shall publish a finite, non-negative numeric `elapsed_ms` in that result.
- [x] **MCP-OP-RESULT-002**: When a public MCP handler produces a typed refusal, clj-surgeon shall publish a finite, non-negative numeric `elapsed_ms` in that refusal.
- [x] **MCP-OP-RESULT-003**: When clj-surgeon finalizes a domain result, the finalized domain fields shall equal the produced domain fields except for the authoritative top-level `elapsed_ms`.
- [x] **MCP-OP-RESULT-004**: When timing evidence is added to an MCP domain result, clj-surgeon shall preserve the domain outcome's existing MCP success or error classification.
- [x] **MCP-OP-RESULT-005**: If MCP result finalization receives a non-map result or an invalid elapsed interval, then clj-surgeon shall fail the MCP invocation as an unexpected MCP error without publishing a malformed domain result.
- [x] **MCP-OP-RESULT-006**: If MCP summary rendering or result serialization fails, then clj-surgeon shall fail the MCP invocation as an unexpected MCP error without publishing a partial domain result.

## Timing Boundary and Presentation

- [x] **MCP-OP-TIME-001**: When a public MCP handler is invoked, clj-surgeon shall start its request clock before handler validation and domain execution.
- [x] **MCP-OP-TIME-002**: When public MCP domain execution returns, clj-surgeon shall stop its request clock before summary rendering, serialization, callback scheduling, and transport.
- [x] **MCP-OP-TIME-003**: When a public MCP result is summarized, clj-surgeon shall render its structured `elapsed_ms` with a locale-independent decimal point, exactly two decimal places, and the `ms` suffix.
- [x] **MCP-OP-TIME-004**: Where an existing top-level MCP timer measures a narrower internal phase, clj-surgeon shall preserve that value under a phase-specific field instead of using it as public `elapsed_ms`.
- [x] **MCP-OP-SCHEMA-001**: For every tool in the canonical public MCP registry, clj-surgeon shall require `elapsed_ms` as a non-negative number in that tool's output schema.

## Asynchronous Verification

- [x] **MCP-OP-ASYNC-001**: While a cold verification job is pending, an MCP launch or inspection result shall omit `job_elapsed_ms`.
- [x] **MCP-OP-ASYNC-002**: When inspection observes a cold verification job that completed after execution began, the MCP result shall contain finite, non-negative `elapsed_ms` and `job_elapsed_ms` values.
- [x] **MCP-OP-ASYNC-003**: When a cold verification result contains `elapsed_ms` and `job_elapsed_ms`, its human summary shall label the corresponding formatted values as `request` and `job`.
- [x] **MCP-OP-ASYNC-004**: If cold verification inspection cannot identify an owned job that began execution, then the typed refusal shall omit `job_elapsed_ms`.
- [x] **MCP-OP-ASYNC-005**: When cold verification inspection reads a job state, clj-surgeon shall publish evidence from exactly that observed state even if the job transitions afterward.

## Registration and Traceability Gates

- [x] **MCP-OP-COVERAGE-001**: When the canonical public MCP registry changes, `make runtests` shall fail unless the operation witness catalog has exactly the same tool keyset.
- [x] **MCP-OP-COVERAGE-002**: For each canonical public MCP registry entry, `make runtests` shall fail unless the witness catalog exercises exactly the public outcome classes declared by that entry.
- [x] **MCP-OP-TRACE-001**: When `make runtests` executes, it shall fail if an active-gap `[ ]` MCP operation-contract intent lacks a direct test witness.
- [x] **MCP-OP-TRACE-002**: When `make runtests` executes, it shall fail if an MCP operation-contract implementation or test witness names an unknown intent ID.
- [x] **MCP-OP-TRACE-003**: When `make runtests` executes, it shall fail if an implemented `[x]` MCP operation-contract intent lacks an implementation witness or a direct test witness.
- [x] **MCP-OP-TRACE-004**: When `make runtests` executes, it shall not require implementation or test witnesses for a deferred `[D]` MCP operation-contract intent.
- [x] **MCP-OP-ORACLE-001**: Where a Prolog shadow oracle is retained after finding an independent counterexample, `make runtests` shall execute that oracle as a blocking gate.

## Compact Root-Scoped Data Edits

- [x] **MCP-OP-EDIT-001**: When one compact literal edit names an explicit non-empty `files` array and `within.root` is true, clj-surgeon shall apply the exact `from` to `to` replacement with the declared match count in every file as one frozen failure-atomic transaction.
- [x] **MCP-OP-EDIT-002**: When a compact literal edit targets an `.edn` file, clj-surgeon shall require root scope and refuse namespace ownership, named-form ownership, and owner deletion before writing.
- [x] **MCP-OP-EDIT-003**: If a grouped compact edit repeats a file, combines `file` with `files`, omits both, uses grouped files outside root scope, or fails its per-file cardinality, clj-surgeon shall refuse the complete request without changing any source.
- [x] **MCP-OP-EDIT-004**: When a root-scoped EDN edit succeeds, clj-surgeon shall parse and read back every future file and preserve all bytes outside the exact replaced subtrees, including comments and metadata.
- [x] **MCP-OP-EDIT-005**: When a compact Clojure edit uses `within.namespace=true`, clj-surgeon shall resolve the file's unique namespace form without requiring the caller to repeat its name, and shall refuse before writing if that owner is not unique.

## Extraction Planning

- [x] **MCP-OP-PLAN-001**: When `inspect_clojure` receives a valid `plan-extraction` mission, clj-surgeon shall compile the result through the same pure extraction planner used by execution and shall not write or retain mutation authority.
- [x] **MCP-OP-PLAN-002**: When extraction planning succeeds, clj-surgeon shall return the complete bounded structural caller and quoted-Var evidence, exact returned and omitted counts, and no private future-source fields.
- [x] **MCP-OP-PLAN-003**: When extraction planning succeeds, clj-surgeon shall return one ready-to-fill `apply_clojure_changes` call containing the exact planned forms, destination, require policy, workspace root, and frozen source hash while leaving semantic caller decisions to the caller.
- [x] **MCP-OP-PLAN-004**: If a plan-followed extraction supplies a source hash that differs from the executor's fresh source snapshot, clj-surgeon shall refuse before changing any file.
- [x] **MCP-OP-PLAN-005**: When extraction omits aggregate expectations, clj-surgeon shall derive them from the exact forms, guarded caller edits, and affected files; when explicit expectations are supplied, they remain authoritative and a mismatch refuses.
- [x] **MCP-OP-PLAN-006**: When a remaining source owner will call a moved private form through the destination namespace, extraction planning shall publish that exact form as a required visibility change and shall include it in the snapshot-bound next call without writing source.
- [x] **MCP-OP-PLAN-007**: When extraction apply receives `public_forms`, clj-surgeon shall require every mechanically necessary moved private form, refuse unmoved, already-public, or unsupported declarations, and apply each supported visibility change inside the same parsed, read-back-verified atomic transaction.
- [x] **MCP-OP-PLAN-008**: When extraction apply omits `public_forms`, `caller_changes`, `ignored_caller_files`, and aggregate expectations, and the complete frozen workspace proves that no caller decision remains, clj-surgeon shall derive the required visibility changes and exact counts and commit through the existing extraction transaction in the same request.
- [x] **MCP-OP-PLAN-009**: When extraction apply omits caller decisions and the complete frozen workspace contains any structural or quoted-Var caller candidate, clj-surgeon shall refuse before writing and return the completed snapshot-bound plan plus an exact `genuine_unknowns` vector; omission shall never account for a discovered caller.
- [x] **MCP-OP-PLAN-010**: When extraction apply explicitly supplies `public_forms` or aggregate expectations, those values shall remain authoritative; an explicit empty visibility decision or mismatched exact count shall refuse rather than be replaced by mechanical derivation.

## Exact Repository Verification

These requirements define the project-owned exact-verifier fusion contract.

- [x] **MCP-OP-VERIFY-001**: When an apply request selects `verify="exact"`, clj-surgeon shall resolve only a project-owned `"exact"` profile whose acceptance is `:exact-exit`; if that profile is absent, process-owned, malformed, or has another acceptance policy, clj-surgeon shall refuse before changing source.
- [x] **MCP-OP-VERIFY-002**: When a project exact profile is validated, clj-surgeon shall require exactly one non-empty string argument vector, no `{files}` placeholder, no hot or cold verifier, and an explicit timeout from 1 through 120000 milliseconds inclusive; absence or a value outside that range shall refuse before changing source.
- [x] **MCP-OP-VERIFY-003**: When the exact verifier executes, clj-surgeon shall resolve only its executable, preserve every remaining declared argument and its order, use the canonical project root as cwd, inherit the documented server environment with the paved PATH adjustment, and shall not invoke a shell or claim complete environment identity with an external login shell.
- [x] **MCP-OP-VERIFY-004**: When a clj-kondo exact-exit profile executes, clj-surgeon shall not capture or compare a diagnostic baseline and shall not add cache, output, file-scope, or fail-level arguments.
- [x] **MCP-OP-VERIFY-005**: When the exact verifier exits zero after candidate read-back, clj-surgeon shall retain the complete transaction and inverse receipt and return terminal verification evidence containing the project profile name, acceptance policy, stable normalized profile-definition SHA-256, cwd, resolved argv, exit, elapsed time, complete captured-output byte count and hash, and visible-output truncation state; the selected in-memory profile shall remain execution authority if its project file changes later.
- [x] **MCP-OP-VERIFY-006**: When the exact verifier completes with an ordinary nonzero exit, clj-surgeon shall undo the complete transaction and return the exit, bounded aggregated diagnostics, rollback evidence, and `source_unchanged=true` only when undo read-back succeeds.
- [x] **MCP-OP-VERIFY-007**: If the exact verifier times out, cannot launch, or terminates with a crash-or-signal-style exit, clj-surgeon shall classify the verification as unverified, undo the complete transaction, and return the distinct process outcome plus bounded aggregated diagnostics without claiming an operating-system signal from exit status alone.
- [x] **MCP-OP-VERIFY-008**: If rollback after any exact-verifier non-pass cannot prove restoration of every original and created path, clj-surgeon shall report recovery required and shall not claim `source_unchanged=true`.
- [x] **MCP-OP-VERIFY-009**: When exact verification fails or is unverified, clj-surgeon shall neither auto-fix nor recommend a blind retry; its remedy shall require correction of the deterministic diagnostics or restoration of verifier authority before a new guarded request.
- [x] **MCP-OP-VERIFY-010**: Before exact-verifier fusion is activated, permanent witnesses shall prove equivalence with the declared external command for canonical cwd, resolved executable, remaining argument order, exit acceptance, and diagnostic meaning across a warning-bearing pass, ordinary lint failure, missing authority, timeout, crash-or-signal-style exit, staged-byte visibility, and complete rollback; each invocation shall publish its own complete captured-output byte count and hash without requiring identical bytes when verifier output contains nondeterministic timing text, and witnesses shall not claim complete environment identity.

## Read Selector Recovery

- [x] **MCP-OP-READ-DIAG-001**: When a `forms` request cannot select each requested owner exactly, clj-surgeon shall report the failed stage, request identity, file, and each failed owner. It shall include failure kinds, exact match counts, available-owner count, source hash, and bounded-presentation counts without source bodies.
- [x] **MCP-OP-READ-DIAG-002**: When clj-surgeon summarizes a selector refusal, it shall name the failed request, file, and owner and disclose hypothesis truncation. It shall state that listed owners are real snapshot evidence, ranking is non-authoritative, semantic selection among listed owners is allowed, and the exact retry verifies the selection. It shall label the first suggestion as a hypothesis only and require the caller to choose one exact owner and retry.
- [x] **MCP-OP-READ-DIAG-003**: When the complete name-only owner vector fits the public result budget, a selector refusal shall return every unique available owner in deterministic order. Otherwise, it shall return a bounded prefix and exact returned and omitted counts.
- [x] **MCP-OP-READ-HYP-001**: When selector recovery ranks possible owners, clj-surgeon shall rank the complete available-owner universe independently for each failed owner and publish each returned candidate with its rank, evidence basis, and `authority=false`.
- [x] **MCP-OP-READ-HYP-002**: When the hypothesis presentation exceeds its bound, clj-surgeon shall report available, returned, and omitted counts. It shall not use presentation evidence as selection authority.
- [x] **MCP-OP-READ-PARITY-001**: When the transport-neutral exact-form selector refuses a missing or ambiguous owner, the CLI and MCP projections shall expose the same complete bounded owner vocabulary and non-authoritative per-owner hypotheses without source bodies.

## Deferred Read Mission Surface

- [D] **MCP-OP-READ-AUTH-001**: If one declared exact relation over the complete frozen candidate universe proves one correction, clj-surgeon shall publish its inputs, owner, cardinality, and snapshot evidence as authority. Otherwise, it shall publish no authority.
- [D] **MCP-OP-READ-GUARD-001**: When an inspect request supplies frozen file-hash guards, clj-surgeon shall compare every captured canonical snapshot with its declared hash before request evaluation and shall refuse the complete read on any mismatch.
- [D] **MCP-OP-READ-RETRY-001**: When one selector failure has exact authority and no other request fails, clj-surgeon shall return one schema-valid next call. It shall change only the proved selector and bind every requested file to its frozen hash.
- [D] **MCP-OP-READ-RETRY-002**: If correction authority is absent, non-unique, stale, or accompanied by another failure, clj-surgeon shall omit the executable next call. It shall keep `ok=false` and `read_complete=false` and return no ordinary successful results.
- [D] **MCP-OP-READ-RESOLVE-001**: When one explicit clue resolves exactly one owner in a frozen snapshot, clj-surgeon shall return that owner and its proof trace. Exact clues are literals, containing lines, declared aliases, or fully qualified owners.
- [D] **MCP-OP-READ-RESOLVE-002**: If an explicit resolution clue matches zero or multiple owners, clj-surgeon shall refuse without choosing an owner.
- [D] **MCP-OP-READ-CONT-001**: When one selector-local failure interrupts a batch, clj-surgeon shall preserve completed sibling evidence in a snapshot-bound continuation outside ordinary successful results.
- [D] **MCP-OP-READ-CONT-002**: If schema, path, parse, snapshot, or output-budget validation fails, clj-surgeon shall return no continuation or partial successful evidence.
- [D] **MCP-OP-READ-MISSION-001**: When a caller supplies a declarative read-question graph, clj-surgeon shall reuse frozen snapshots and owner selections while enforcing the declared evidence budget.
- [D] **MCP-OP-READ-MISSION-002**: When a declarative read mission completes, clj-surgeon shall return guard-ready source anchors without granting write authority or inventing replacement text.

## Misreadings and Boundaries

| Intent | Plausible wrong reading to prevent | Boundary examples |
|---|---|---|
| `MCP-OP-RESULT-001` | Timing is required only for mutations because reads are cheap. | Empty read; metadata-only read; preview; committed edit; verification-pending edit. |
| `MCP-OP-RESULT-002` | A fast refusal needs no timing, or failures should use only MCP transport errors. | Invalid arguments; stale source; count mismatch; unknown verification job; workspace mismatch. |
| `MCP-OP-RESULT-003` | The shared finalizer may normalize operation-specific fields while it already owns the result. | Nested maps, false and nil values, existing phase-specific timings, operation-specific next actions. |
| `MCP-OP-RESULT-004` | Adding common evidence may normalize a typed refusal into a transport error or make every result look successful. | Successful read; typed validation refusal; stale-source refusal; verification failure. |
| `MCP-OP-RESULT-005` | Clamp a broken clock to zero or stringify a non-map result so the caller receives something. | Negative delta; NaN; positive infinity; nil, vector, or string result. |
| `MCP-OP-RESULT-006` | Publish structured content first and tolerate a missing summary, or return partially serialized evidence. | Summary exception; JSON encoding exception; callback must not observe half-built values. |
| `MCP-OP-TIME-001` | Start timing after validation, thereby hiding expensive rejected requests. | Large valid request; large invalid request; bounded job enqueue; verification lookup. |
| `MCP-OP-TIME-002` | Include JSON serialization or network delivery and call the value server execution time. | Slow callback; large structured result; disconnected client; caller scheduling delay. |
| `MCP-OP-TIME-003` | Re-measure during summary creation or use the process locale. | Locale with decimal comma; values rounding to zero; request and job values that round identically. |
| `MCP-OP-TIME-004` | Preserve inspect's narrower clock as the public value because callers already see it, leaving tools with different elapsed boundaries. | Existing inspect execution timer; telemetry consumer; no narrower consumer; asynchronous job timer. |
| `MCP-OP-SCHEMA-001` | Runtime output is enough even when clients cannot discover the field from the schema. | Each current tool; a newly registered tool; a tool with a specialized output schema. |
| `MCP-OP-ASYNC-001` | Report elapsed job time so far for a pending job. | Launch-pending; inspect-pending; job finishes immediately after the state snapshot. |
| `MCP-OP-ASYNC-002` | Replace request time with job time after completion, or report only one unlabeled duration. | Completed success; verification failure; background exception after execution begins. |
| `MCP-OP-ASYNC-003` | Positional order distinguishes the two clocks without labels. | Equal rounded values; reordered summary details; localized surrounding prose. |
| `MCP-OP-ASYNC-004` | Fabricate `job_elapsed_ms: 0` when no owned execution was observed. | Unknown ID; expired record; wrong workspace; refusal before execution. |
| `MCP-OP-ASYNC-005` | Merge fields from two job reads to return the newest possible answer. | Pending-to-complete race; completion-to-expiration race; retry during lookup. |
| `MCP-OP-COVERAGE-001` | A hand-written list of today's four tools is equivalent to the live registry. | Added tool; renamed tool; deleted tool; alternate registration path. |
| `MCP-OP-COVERAGE-002` | One happy path and one refusal prove every public mode, or tests may declare the same incomplete mode list they witness. | Inspect read, prepared basis, pending/completed verification; transform preview/commit; edit verification modes. |
| `MCP-OP-TRACE-001` | An active-gap spec can land without a red witness because its implementation has not started. | Missing direct test; test exists without annotation; deferred intent. |
| `MCP-OP-TRACE-002` | A plausible-looking annotation may introduce a new intent without design review. | Typo; retired ID; ID from another leaf; code-only or test-only invention. |
| `MCP-OP-TRACE-003` | Marking a spec implemented is documentation enough even when code or test linkage disappeared. | Missing code annotation; missing direct test; helper-only annotation. |
| `MCP-OP-TRACE-004` | Deferred intent must carry placeholder code or a skipped test to satisfy traceability. | No witnesses; accidental witness to deferred ID; transition from deferred to active gap. |
| `MCP-OP-ORACLE-001` | Keeping a Prolog file in the repository is sufficient even if the normal suite never runs it. | Missing `swipl`; expected-fail case begins succeeding; oracle command exits nonzero. |
| `MCP-OP-PLAN-001` | MCP should shell out to the CLI planner or retain an opaque write-capable plan. | Pure compiler parity; source unchanged; no retained basis or mutation token. |
| `MCP-OP-PLAN-002` | A top-ranked caller sample is enough, or private future bytes may be returned because the caller already requested a plan. | Zero, one, and many callers; quoted Vars; result budget overflow; private underscore-prefixed fields. |
| `MCP-OP-PLAN-003` | The kernel may guess caller rewrites or mark structural candidates as semantically complete. | Empty decision arrays; exact candidate list; explicit ignored files; source hash in next call. |
| `MCP-OP-PLAN-004` | Named forms are sufficient stale guards across two calls, or a stale plan can be automatically recomputed during apply. | Source changed inside moved owner; source changed elsewhere; destination appears after planning. |
| `MCP-OP-PLAN-005` | Derived aggregate counts weaken exact per-edit guards, or legacy explicit counts may be silently ignored. | No callers; several caller edits; repeated caller file; explicit count too high or low. |
| `MCP-OP-PLAN-008` | A caller must invoke `plan-extraction` even when one frozen compiler snapshot proves every omitted fact. | Required private visibility with zero external callers; omitted counts; same future sources as explicit input. |
| `MCP-OP-PLAN-009` | Empty or omitted caller arrays prove that no callers exist, or a ranked/fuzzy candidate may be ignored automatically. | One structural caller; one quoted Var; several candidates; source unchanged and exact completed plan. |
| `MCP-OP-PLAN-010` | Mechanical derivation may overwrite a supplied empty visibility set or repair an incorrect supplied expectation. | Explicit `public_forms: []`; exact correct expect; expect too high or low. |
| `MCP-OP-READ-DIAG-001` | One aggregate candidate list is enough even when several owners failed, or source bodies should accompany the refusal. | One missing owner; several missing owners; ambiguous duplicate owner; successful siblings before failure. |
| `MCP-OP-READ-DIAG-002` | Structured evidence is sufficient when the visible summary says only `correct_request`. | Missing owner in a large test file; candidate list truncated; no useful lexical hypothesis. |
| `MCP-OP-READ-DIAG-003` | A ranked list makes the complete owner vocabulary unnecessary, or returning source is required for useful evidence. | Small namespace; semantic rename outside the top ten; repeated owner names; name-only vector over budget. |
| `MCP-OP-READ-HYP-001` | The highest-ranked owner can be selected automatically because the list is deterministic. | One-character typo; semantic paraphrase; one candidate; tied candidates; candidate input permutation. |
| `MCP-OP-READ-HYP-002` | A top-ten presentation is the complete candidate universe, or a score gap proves intent. | Eleven or more owners; intended owner at rank ten; intended owner outside the bound; one displayed candidate from many available owners. |

## Deferred Surface

Transport-level exception envelopes, cancellation, deadlines, queue time,
correlation IDs, internal phase telemetry, and CLI/MCP receipt convergence are
outside these requirements. They require their own reviewed intent before this
leaf expands to cover them.

Exact read correction authority, hash-guarded retries, successful-sibling
continuations, explicit clue resolution, and declarative read missions remain
deferred until the stateless selector-evidence slice has clean-context and field
evidence.
