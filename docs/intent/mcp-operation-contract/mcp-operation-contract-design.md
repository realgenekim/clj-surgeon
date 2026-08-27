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

## Selector Refusal Recovery

A `forms` selector refusal must give the model enough bounded structural
evidence to correct its hypothesis without using `rg`, an outline, `sed`, or a
second discovery call. The refusal remains fail-closed. It does not read a
different owner, return source from successful sibling requests, or authorize a
write.

The transport-neutral exact-form selector reports each missing or ambiguous
requested owner and the complete available owner universe. The CLI and MCP
projections preserve that evidence per failed owner. They do not combine
several failures into one aggregate candidate ranking.

Each projected failure contains:

- the failed request ID and index;
- the file and failed stage;
- the requested owner, failure kind, and exact match count;
- the complete available-owner count;
- every unique available owner name when the name-only vector fits the public
  result budget;
- at most ten ranked owner hypotheses;
- the returned and omitted hypothesis counts; and
- the source hash that owned the selection decision.

If the complete name-only owner vector would exceed the public result budget,
the refusal returns a deterministic bounded prefix and reports its returned and
omitted counts. It does not include source bodies. This keeps the usual refusal
equivalent to an outline while preserving the existing output bound.

The concise summary names the failed request, file, and requested owner. It
shows the first ranked owner as a question labeled `hypothesis only`, then
prints the complete bounded available-owner vocabulary so a text-only caller
can see lower-ranked semantic corrections. It tells the caller to choose one
exact owner and retry. It also names the decision boundary explicitly: listed
owners are real evidence from the frozen source snapshot, ranking is
non-authoritative, semantic selection among those owners is allowed, and the
ordinary exact retry verifies the selection. The structured result preserves
every failed owner, including failures that do not receive a useful lexical
hypothesis.

### Keep hypotheses separate from authority

Owner ranking is a perception aid for the model. Rank, score, lexical order, a
unique top result, or a large score gap cannot select an owner. Every published
hypothesis states `authority=false`. The first vertical slice publishes no
automatic correction and no executable retry. The next call succeeds only when
the caller submits an exact real owner through the ordinary public contract.

This boundary follows the Bitter Lesson. Surgeon exposes real structure that a
more capable model can use better. It does not accumulate rules for particular
misspellings, plural forms, API migrations, or test names.

### Rank each missing owner independently

The current aggregate candidate list compares available owners with all
requested names. Owners that already resolved can therefore crowd the useful
correction out of the bounded result. The recovery compiler instead ranks the
complete candidate universe independently for each failed owner.

The ranker is a pure deterministic function. Its evaluation compares normalized
Levenshtein distance, normalized Damerau-Levenshtein distance, kebab-token
overlap, character trigrams, and a deterministic character/token hybrid against
the frozen real-refusal corpus. The implementation keeps the simplest ranker
that places every strict corpus correction in the first ten. Candidate input
order cannot change the result. The exact owner name breaks any remaining score
tie.

The public hypothesis need not expose a numeric confidence value. It reports
rank, basis, and `authority=false`. This prevents a caller from mistaking a
presentation score for proof.

### Keep the first recovery slice stateless

The first slice changes only selector-refusal evidence. It preserves
`ok=false`, `read_complete=false`, and `source_unchanged=true`. It returns no
ordinary successful `results`, retains no server continuation, accepts no new
input fields, and does not involve a semantic provider.

Exact proof relations, hash-guarded executable retries, successful-sibling
continuation, explicit clue resolution, and declarative read missions remain
separate modules. They advance only when the stateless evidence slice cannot
meet the two-call and no-native-discovery gate.

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

The exact-form selector recovery evidence is transport-neutral. CLI EDN and MCP
structured output consume the same compiled available-owner vector, per-owner
hypotheses, counts, truncation state, and `authority=false` law. The CLI does
not consume the MCP envelope. CLI exit status, stdout and stderr, process
startup time, compatibility, and broader operation receipts remain separate
public contracts tracked by `clj-surgeon-9xi`.

## Extraction Planning Boundary

The CLI and MCP share the pure extraction planner, not each other's transport
contracts. `inspect_clojure` exposes a top-level `plan-extraction` mission that
captures one deterministic workspace source universe and delegates to
`extract/compile-plan`. The result contains the public migration manifest,
complete bounded structural caller evidence, a frozen source hash, and a
ready-to-fill `apply_clojure_changes` call. Private future-source bytes and
other executor internals never cross the public boundary.

Structural caller candidates are evidence for model judgment, not proof of
semantic completeness. The planner does not choose caller changes or ignores.
The execution request must account for each candidate, and a source hash from a
prior plan is an exact compare-and-swap guard. A plan never retains mutation
authority: source changes, missing caller decisions, expectation mismatch, or
budget overflow refuse before writing.

The existing failure-atomic extraction executor remains the only mutation
path. Direct one-call extraction may omit a prior source hash because planning
and commit already share one captured request. Plan-followed extraction binds
the two requests with the exact hash. Aggregate counts may be derived from the
authoritative form list, guarded caller changes, and affected file set; legacy
explicit counts remain accepted and must match.

Visibility changes are part of the reviewed extraction manifest, not a
post-extraction text patch. The pure planner identifies moved private forms
that remaining source owners will necessarily call through the destination
namespace and publishes that exact required set. The ready next call carries
the set as `public_forms`; invoking apply is the authorization. Apply may also
accept additional moved private forms for externally proven callers, but it
refuses names that are not both selected and private. The initial lossless
projection supports only `defn-` to `defn`. Unsupported private metadata and
custom macros fail closed until a separate projection is specified and tested.

The same pure planner also serves an internal fast path for a mechanically
complete apply request. `file`, `to`, `forms`, and `require_policy` remain the
caller's mandatory authority. When `public_forms`, caller-decision arrays, and
aggregate expectations are absent, the executor preserves that absence until
it owns the complete frozen workspace source universe. It then derives required
visibility, dependencies, and counts from `extract/compile-plan`. If the
complete candidate universe is empty, the existing failure-atomic executor
commits that compiled result in the same request. This is internal compilation,
not an inferred architecture decision and not a second plan representation.

Omission never means that a caller was reviewed. Any structural or quoted-Var
candidate leaves a genuine decision, so the executor refuses before writing and
returns the completed snapshot-bound plan and exact unknowns. Explicit values
remain authoritative: `public_forms: []` does not authorize a required private
form to become public, and a supplied aggregate expectation is never repaired.
Similarity and ranking evidence cannot close an unknown. This fast path is an
MCP apply contract only; the CLI `extract!` review policy and its source-hash
surface remain unchanged.

## Exact Repository Verifier Boundary

The exact verifier is a closed workspace capability, not an arbitrary command
surface. An `apply_clojure_changes` request may select `verify="exact"`, but it
cannot supply executable text, arguments, environment values, or a timeout. The
workspace's `.clj-surgeon.edn` must declare the conventional `"exact"` profile.
Process-level profiles and built-in `fast` or `full` profiles cannot satisfy
that selector. Absence or invalid closed data refuses before source mutation.

The first exact profile shape is deliberately singular and bounded:

```clojure
{:verification-profiles
 {"exact"
  {:acceptance :exact-exit
   :timeout-ms 120000
   :commands
   [["clj-kondo" "--lint"
     "src/example/core.clj"
     "src/example/moved.clj"
     "--fail-level" "error"]]}}}
```

The profile contains exactly one non-empty string argument vector and one
positive bounded timeout. It cannot contain hot or cold verification, and the
argument vector cannot contain `{files}`. This preserves the repository's
declared file scope and relative spelling instead of replacing them with the
transaction's absolute changed-file set. The process runner resolves only the
executable through the paved path. It preserves every remaining argument in
order, uses the canonical project root as cwd, inherits the server environment
with the paved PATH adjustment, and does not invoke a shell.

Exact-exit is separate from the existing diagnostic-delta policy. In
diagnostic-delta mode, clj-kondo runs before and after a transaction with cache
disabled and EDN output so introduced findings can be classified. That is
useful for a generic changed-file profile, but it is not the command the
repository declared. Exact-exit therefore performs no pre-write verifier run,
adds no cache or output arguments, and accepts only the declared process exit.
This distinction is the permanent ratchet from the rejected `verify=fast`
experiment, which rolled back safely but rejected warnings that the requested
`--fail-level error` command permitted.

The existing extraction transaction remains the only mutation authority:

```text
compile and format candidate bytes
  -> guarded whole-file write and read-back
  -> stage inverse receipt
  -> execute exact project argv against those candidate bytes
       -> exit 0: publish terminal verified receipt
       -> ordinary nonzero: exact verification failure, undo all files
       -> timeout / launch failure / signal crash: unverified, undo all files
```

The runner aggregates stdout and stderr in arrival order because the MCP command
surface already reports aggregated diagnostics. It hashes and counts the full
output before bounding visible text. A passing check returns the project profile
identity, canonical cwd, resolved argv, exit zero, elapsed time, output byte
count, and output hash without forcing warning bodies into model context. A
failed or unverified check additionally returns bounded aggregated diagnostics.
Timeout, launch failure, crash, and ordinary nonzero exit remain distinct so a
caller does not mistake an unavailable authority for a semantic rejection.

Every non-pass attempts the existing receipt-backed whole-transaction undo.
Only verified undo may publish `source_unchanged=true`. A rollback failure keeps
the recovery evidence and reports manual recovery required. Neither a
deterministic verifier failure nor an unverified process outcome recommends an
automatic or blind retry. The caller must fix the reported program diagnostics
or restore the verifier authority before submitting a new guarded request.

### Behavior matrix

| Profile / process state | Write before verifier | Public outcome | Required evidence | Final source state |
|---|---|---|---|---|
| Project `exact`, exit 0 with warnings | Candidate written and read back | success, `verification_complete=true` | profile source, cwd, resolved argv, exit 0, elapsed, output bytes/hash | candidate retained; inverse receipt retained |
| Project `exact`, ordinary nonzero | Candidate written and read back | `verification-failed` | exit, bounded aggregated diagnostics, verified rollback | original bytes restored; created files removed |
| Project `exact`, deadline exceeded | Candidate written and read back | `verification-unverified` / timeout | deadline, elapsed, bounded partial output, verified rollback | original bytes restored; created files removed |
| Project `exact`, executable cannot launch | Candidate written and read back | `verification-unverified` / launch failure | resolved argv, launch diagnostic, verified rollback | original bytes restored; created files removed |
| Project `exact`, signal-style crash | Candidate written and read back | `verification-unverified` / crash | exit/signal-style status, bounded output, verified rollback | original bytes restored; created files removed |
| Project `exact`, verifier non-pass and undo fails | Candidate written and read back | recovery required | verifier evidence plus per-file recovery evidence | unknown; never claim `source_unchanged` |
| Exact profile absent, process-owned, malformed, or not `:exact-exit` | none | pre-write refusal | failed profile/source validation | source unchanged |
| `fast` or `full` selected | Existing behavior | existing profile result | existing profile evidence | existing semantics unchanged |

Shadow equivalence must precede activation. At minimum, the external command and
exact profile route must agree on a warning-bearing pass, a normal lint failure,
a missing file or executable, and one unverified process outcome. The extraction
boundary witness must prove the verifier observed staged destination bytes and
that failure restored every original byte, removed every created path, and did
not leave a usable receipt.

## Compact Root-Scoped Data Edits

`edit_clojure` admits `.edn` only for an exact literal edit whose location is
`within.root`. Root scope searches the complete lossless concrete-syntax tree;
it does not imply replacement of the whole document. The exact `from` subtree
and cardinality remain the stale-source and ambiguity guards.

One edit may name either one `file` or an explicit non-empty `files` array.
Grouped `files` are available only with root scope. `matches` defaults to one
and is authoritative per file. The adapter derives the aggregate match count,
expands the gesture into the existing direct transaction representation, and
commits all affected files against one frozen snapshot. Duplicate files,
unsupported extensions, a count mismatch in any file, malformed future data,
or any write failure refuses or rolls back the complete transaction.

EDN does not gain namespace, named-owner, owner-deletion, extraction, semantic
index, or formatter behavior. The kernel parses and rewrites concrete syntax;
it does not invoke tagged-literal readers or evaluate data. Bytes outside the
exact replaced subtrees, including comments and metadata, remain under the
existing lossless transaction contract.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|---|---|---|---|
| Common evidence owner | Shared runner/finalizer and one publication choke point per public handler | Server-registration middleware; independent handler instrumentation | The boundary is visible around domain execution without hiding asynchronous semantics or allowing early-return or per-handler drift. |
| Elapsed authority | One handler-entry monotonic request clock | Preserve any domain-provided elapsed value; measure during serialization; end-to-end wall clock | One clock has a stable owner and meaning. Other phases retain distinct names. |
| Summary contract | Operation-specific prose containing the finalized duration | One universal summary grammar; structured-only timing | Existing concise summaries remain useful while humans can see performance without inspecting structured content. |
| New-tool enforcement | Registry entries declare outcome classes; an independent witness catalog must match | Fixed list of current tools; finalizer-only proof; documentation review | Registration-derived outcome coverage makes omission fail when either the tool surface or a tool's public modes grow. |
| Intent status gate | `[ ]` needs tests, `[x]` needs code and tests, `[D]` is exempt | Gate only implemented specs; require every non-deferred spec to be fully implemented | Tests preload active intent while genuinely deferred work remains non-blocking. |
| Prolog retention | Keep only after an independently found native-test gap | Retain unconditionally; never model relational states | A second model earns maintenance cost only by demonstrating additional fault-finding power. |
| CLI reuse | Share only the transport-neutral exact-selector recovery compiler | Reuse the MCP envelope directly; duplicate all evidence; defer all parity | The owner universe and hypotheses are domain evidence, while transport envelopes and process semantics remain distinct. |
| Internal extraction completion | Preserve omitted visibility through validation, derive only from the executor's complete frozen workspace, and reuse the existing extraction transaction | Require public `plan-extraction`; add `resolve: mechanical`; route CLI through MCP policy | Omission is the shortest honest caller shape, explicit values stay authoritative, and no second plan algebra or CLI compatibility change is introduced. |
| Extraction planning entrance | A top-level `inspect_clojure` mission over the shared pure planner | Fifth public tool; typed batch read operation; CLI subprocess | Planning is a coherent workspace-wide read mission, not one file read, and reuses the existing public read envelope. |
| Plan-to-apply authority | Exact source hash plus explicit caller decisions | Retained in-memory plan; similarity; unguarded replay | The hash is transport-neutral, stale-safe, and grants no implicit write authority. |
| Workspace source universe | One deterministic shared scanner for planning and execution | Duplicate scans in each handler; semantic index as authority | Both phases must reason over identical eligible paths and exact bytes without another index lifecycle. |
| Exact repository verifier | Project-owned `"exact"` profile with `:acceptance :exact-exit`; execute one closed argv after candidate read-back | Arbitrary request command; generic `verify=fast`; clj-kondo diagnostic delta; external second action | It deletes one model boundary without changing file scope, warning policy, command arguments, or rollback authority. |
| EDN edit scope | Exact root-scoped literal edits, optionally grouped across explicit files | Extension allowlist only; all structural operations; native patch only | Root scope reuses the lossless transaction kernel while preventing namespace/owner claims that EDN cannot support. |
| Selector recovery | Per-failed-owner bounded hypotheses with no automatic selection | Aggregate candidates, automatic fuzzy selection, or immediate retained continuation | One refusal gives the model enough real structure for an exact retry without letting presentation rank become authority. |

## Open Questions & Future Decisions

### Resolved

1. The public request clock excludes model, network, callback, serialization,
   and background-job time.
2. Typed refusals carry timing and concise summaries just like successes.
3. A retained Prolog oracle and the linked-intent coherence gate belong in
   `make runtests`.
4. CLI and MCP exact-selector refusals share one transport-neutral recovery
   compiler. Broader operation-receipt convergence remains a sibling segment.

### Deferred

1. Whether future telemetry should expose named internal phase timings in
   addition to the single public request clock.
2. Whether transport-level exceptions should eventually become a separately
   specified structured MCP failure envelope.
3. Whether an exact proof relation should emit a hash-guarded executable read
   retry after the stateless hypothesis slice has field evidence.
4. Whether successful sibling reads should use a server-retained continuation
   instead of remaining fail-empty.

## References

- [clj-surgeon HLD](../../high-level-design.md)
- [Uniform MCP elapsed-time plan](../../plans/uniform-mcp-elapsed-time.md)
- CLI/MCP receipt issue: `clj-surgeon-9xi`
