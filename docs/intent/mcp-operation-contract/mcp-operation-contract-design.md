---
parent: high-level-design
prefix: MCP-OP
---

# MCP Operation Contract

## Context and Design Philosophy

clj-surgeon exposes four public MCP tools through three handler families:
structural inspection, exact or prepared mutation, and computed transformation.
Their domain results differ, but callers need the same basic evidence from
each invocation. A public operation must say whether it completed or refused,
how much server-owned execution time it consumed, and what the caller should do
next.

The common contract is additive. It must not change structural selection,
source guards, mutation, rollback, verification, or refusal semantics. Domain
handlers continue to own their result data and concise explanation. A shared
finalizer owns only evidence that is common to every public MCP result.

## Public Operation Inventory

| Public tool | Handler family | Outcome classes |
|---|---|---|
| `inspect_clojure` | inspect | read success, prepared basis, verification pending, verification complete, verification failed, typed refusal |
| `apply_clojure_changes` | change | committed or verification-pending success, typed refusal |
| `edit_clojure` | change | committed success, typed refusal |
| `transform_clojure` | program | preview or committed success, typed refusal |

The registry in `mcp_server.clj` is the authoritative inventory. Contract tests
derive their public-tool census from that registry rather than maintaining an
unrelated hand-written list.

## Operation Lifecycle

```text
public handler entry
    |
    +-- capture monotonic start time
    |
    +-- validate and execute domain operation
    |       |
    |       +-- success result
    |       `-- typed refusal result
    |
    +-- shared finalizer records elapsed_ms
    |
    +-- operation-owned summary renders that same elapsed_ms
    |
    `-- MCP adapter publishes text content + structured content
```

Every handler has one callback/publication choke point after domain execution.
Early domain decisions return data to that point; they do not invoke the MCP
callback themselves. This makes bypassing finalization structurally visible.

A handler-produced map with `:ok false` and a stable refusal reason is a typed
refusal. Handler validation failures are typed refusals. SDK rejection before
handler entry and unexpected exceptions that prevent a result remain MCP
errors and are outside this result contract. Finalization preserves the
existing MCP success/error mapping; elapsed evidence does not reclassify an
outcome.

## Shared Finalizer

The shared operation runner and finalizer form one small explicit boundary. The
runner accepts a clock function, a domain-execution function, and the handler's
summary function. It captures start time, calls domain execution exactly once,
captures finish time, and delegates the explicit values to the finalizer. It
returns the finalized result and presentation values needed by the MCP adapter.

The finalizer:

1. Requires the domain result to be a map.
2. Computes elapsed time once from the two monotonic timestamps.
3. Requires elapsed time to be finite and non-negative.
4. Associates authoritative `elapsed_ms` with the result.
5. Calls the operation-owned summary function with that finalized result.
6. Serializes the same finalized result as structured evidence.

The finalizer does not infer success, translate refusal reasons, choose next
actions, run verification, or convert programmer errors into typed refusals.
A non-map result, invalid clock delta, or summary failure is an unexpected MCP
error and publishes no malformed domain result.

Before routing a handler through the finalizer, existing top-level
`elapsed_ms` producers and consumers are inventoried. A value with the same
public-request meaning is replaced by the finalizer's measurement. A value with
a narrower meaning is preserved under a distinct, phase-specific name such as
`execution_elapsed_ms` or `job_elapsed_ms`. There is only one authoritative
top-level public request clock, and it is never silently overwritten without
this classification.

## Timing Boundary

The request clock begins immediately after entry to the public tool handler. It
ends after domain execution returns and before summary rendering, JSON
serialization, callback scheduling, network transport, and caller processing.
It therefore measures server operation execution, not end-to-end latency.
Validation and bounded job enqueue or lookup work occur inside the interval.

The value is a finite, non-negative number of milliseconds. A shared formatter
renders human durations with `Locale/ROOT`, exactly two decimal places, and the
`ms` suffix. The structured value retains numeric precision and is the
authority for machine consumers.

Monotonic time prevents wall-clock adjustment from making durations negative.
Tests inject or redefine the clock only at the narrow timing seam; domain tests
do not sleep.

## Structured and Human Evidence

Every public output schema declares `elapsed_ms` as a required number with a
minimum value of zero. Every success and typed refusal result contains it.

Every concise summary renders the same finalized value. Operation-specific
content remains free to differ:

```text
51 edits · 9 files · 24.73 ms
refused · stale-source · 1.28 ms
4 requests · 3 files · 18 forms · 42.10 ms
```

The contract does not require a universal sentence shape. It requires that the
duration be present, recognizable, and derived from the finalized structured
value rather than measured a second time.

## Asynchronous Verification

Cold verification preserves two separate clocks:

- `elapsed_ms` is the bounded MCP request that launched or inspected the job.
- `job_elapsed_ms` is the background verification work reported by the job.

The human summary canonically labels both values as `request` and `job`; their
position or coincident rounded values cannot make them ambiguous. Each display
value is formatted from its corresponding structured field.

| Observed job state | `elapsed_ms` | `job_elapsed_ms` | Result class |
|---|---:|---:|---|
| Launch accepted, job pending | required | omitted | success, verification pending |
| Inspection finds job pending | required | omitted | success, verification pending |
| Completed successfully | required | required | success, verification complete |
| Completed with verification failure or exception | required | required when execution began | typed verification result |
| Unknown or expired job | required | omitted | typed refusal |
| Workspace does not own job | required | omitted | typed refusal |

Each inspection snapshots one job state. If the job changes immediately after
that snapshot, the response remains an honest report of the observed state;
the caller may make a new bounded inspection request.

## Registration-Wide Enforcement

The canonical registration collection in `mcp_server.clj` is a permanent
ratchet and the only supported path for public tool registration. Each registry
entry declares the tool name, output schema, handler, and public outcome
classes. A separate test witness catalog keys each registered tool to the
outcome classes it exercises. The suite requires exact tool and outcome-class
equality between registration and witnesses, so a new tool, mode, or alternate
registration cannot silently bypass the contract.

For every registered tool and outcome class it checks:

- the output schema requires a non-negative numeric `elapsed_ms`;
- the outcome reaches the single publication choke point and finalizer;
- the summary contains the same formatted value as structured content.

The catalog covers ordinary success and typed refusal for every tool, plus
preview/commit, prepared-basis, verification-pending, and every terminal
verification class exposed by that tool. `verification-result` is deliberately
not one outcome class: the Prolog shadow found that one completion witness could
then conceal the distinct pending and failed clock laws. Adding a tool or
outcome mode without a witness makes the ordinary suite fail.

## Linked Intent and Logic Oracle

The MCP operation contract uses stable EARS identifiers with implementation and
test witnesses. A bidirectional coherence check rejects an active intent with
no code or test witness and rejects a witness naming an unknown intent.

Status controls the gate deliberately:

- `[ ]` is committed work and requires a direct red test witness;
- `[x]` requires both implementation and direct test witnesses;
- `[D]` requires neither witness;
- every code or test annotation must name a known intent at any status.

This lets tests preload intent before code exists without turning deferred ideas
into permanent suite failures.

The initial implementation may construct a Prolog shadow oracle over:

```text
registered tool × outcome class × verification state × required clock
```

The oracle is independent of the Clojure enumeration. It must include expected
failures such as a refusal without elapsed time and a completed asynchronous
result with only one unlabeled clock. It is retained only if it finds a
counterexample that the native contract tests missed. If it is retained, both
the coherence check and oracle run from `make runtests`; otherwise only the
native linked-intent gate remains.

## CLI Boundary

The CLI is outside this leaf's scope. It may later consume a transport-neutral
operation receipt, but it does not consume the MCP envelope directly. CLI exit
status, stdout and stderr, process startup time, and compatibility are separate
public contracts tracked by `clj-surgeon-9xi`.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|---|---|---|---|
| Common evidence owner | Shared runner/finalizer and one publication choke point per public handler | Server-registration middleware; independent handler instrumentation | The boundary is visible around domain execution without hiding asynchronous semantics or allowing early-return or per-handler drift. |
| Elapsed authority | One handler-entry monotonic request clock | Preserve any domain-provided elapsed value; measure during serialization; end-to-end wall clock | One clock has a stable owner and meaning. Other phases retain distinct names. |
| Summary contract | Operation-specific prose containing the finalized duration | One universal summary grammar; structured-only timing | Existing concise summaries remain useful while humans can see performance without inspecting structured content. |
| New-tool enforcement | Registry entries declare outcome classes; an independent witness catalog must match | Fixed list of current tools; finalizer-only proof; documentation review | Registration-derived outcome coverage makes omission fail when either the tool surface or a tool's public modes grow. |
| Intent status gate | `[ ]` needs tests, `[x]` needs code and tests, `[D]` is exempt | Gate only implemented specs; require every non-deferred spec to be fully implemented | Tests preload active intent while genuinely deferred work remains non-blocking. |
| Prolog retention | Keep only after an independently found native-test gap | Retain unconditionally; never model relational states | A second model earns maintenance cost only by demonstrating additional fault-finding power. |
| CLI reuse | Defer to a transport-neutral receipt segment | Reuse the MCP envelope directly; duplicate all evidence | Transport semantics differ even when domain evidence overlaps. |

## Open Questions & Future Decisions

### Resolved

1. The public request clock excludes model, network, callback, serialization,
   and background-job time.
2. Typed refusals carry timing and concise summaries just like successes.
3. A retained Prolog oracle and the linked-intent coherence gate belong in
   `make runtests`.
4. CLI/MCP convergence is a sibling segment, not an implicit cascade from this
   design.

### Deferred

1. Whether future telemetry should expose named internal phase timings in
   addition to the single public request clock.
2. Whether transport-level exceptions should eventually become a separately
   specified structured MCP failure envelope.

## References

- [clj-surgeon HLD](../../high-level-design.md)
- [Uniform MCP elapsed-time plan](../../plans/uniform-mcp-elapsed-time.md)
- CLI/MCP receipt issue: `clj-surgeon-9xi`
