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

## Deferred Surface

Transport-level exception envelopes, cancellation, deadlines, queue time,
correlation IDs, internal phase telemetry, and CLI/MCP receipt convergence are
outside these requirements. They require their own reviewed intent before this
leaf expands to cover them.
