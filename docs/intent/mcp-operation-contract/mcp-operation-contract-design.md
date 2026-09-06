---
parent: high-level-design
prefix: MCP-OP
---

 #MCP Operation Contract

# #Context and Design Philosophy

clj-surgeon exposes four public MCP tools through three handler families:
structural inspection, exact or prepared mutation, and computed transformation.
Their domain results differ, but callers need the same basic evidence from
each invocation. A public operation must say whether it completed or refused
how much server-owned execution time it consumed, and what the caller should do
next.

The common contract is additive. It must not change structural selection
source guards, mutation, rollback, verification, or refusal semantics. Domain
handlers continue to own their result data and concise explanation. A shared
finalizer owns only evidence that is common to every public MCP result.

# #Public Operation Inventory

| Public tool | Handler family | Outcome classes |
|---|---|---|
| `inspect_clojure` | inspect | read success, prepared basis, verification pending, verification complete, verification failed, typed refusal |
| `apply_clojure_changes` | change | committed or verification-pending success, typed refusal |
| `edit_clojure` | change | committed success, typed refusal |
| `transform_clojure` | program | preview or committed success, typed refusal |

The registry in `mcp_server.clj` is the authoritative inventory. Contract tests
derive their public-tool census from that registry rather than maintaining an
unrelated hand-written list.

# #Operation Lifecycle

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
Early domain decisions return data to that point ; they do not invoke the MCP
callback themselves. This makes bypassing finalization structurally visible.

A handler-produced map with `:ok false` and a stable refusal reason is a typed
refusal. Handler validation failures are typed refusals. SDK rejection before
handler entry and unexpected exceptions that prevent a result remain MCP
errors and are outside this result contract. Finalization preserves the
existing MCP success/error mapping ; elapsed evidence does not reclassify an
outcome.

# #Shared Finalizer

The shared operation runner and finalizer form one small explicit boundary. The
runner accepts a clock function, a domain-execution function, and the handler's
summary function. It captures start time, calls domain execution exactly once
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

# #Timing Boundary

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
Tests inject or redefine the clock only at the narrow timing seam ; domain tests
do not sleep.

# #Structured and Human Evidence

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

# #Selector Refusal Recovery

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

- the failed request ID and index ;
- the file and failed stage ;
- the requested owner, failure kind, and exact match count ;
- the complete available-owner count ;
- every unique available owner name when the name-only vector fits the public
result budget ;
- at most ten ranked owner hypotheses ;
- the returned and omitted hypothesis counts ; and
- the source hash that owned the selection decision.

If the complete name-only owner vector would exceed the public result budget
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

## #Keep hypotheses separate from authority

Owner ranking is a perception aid for the model. Rank, score, lexical order, a
unique top result, or a large score gap cannot select an owner. Every published
hypothesis states `authority=false`. The first vertical slice publishes no
automatic correction and no executable retry. The next call succeeds only when
the caller submits an exact real owner through the ordinary public contract.

This boundary follows the Bitter Lesson. Surgeon exposes real structure that a
more capable model can use better. It does not accumulate rules for particular
misspellings, plural forms, API migrations, or test names.

## #Rank each missing owner independently

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

## #Keep selector hypotheses stateless ; preserve proven siblings inline

Selector hypotheses remain stateless. A selector-local refusal after one or
more complete sibling requests may additionally return an inline continuation.
The continuation contains the already-computed ordered sibling results, exact
hash guards for every original request file, and completed and pending request
IDs. It remains
`ok=false`, `read_complete=false`, `source_unchanged=true`, and
`write_authority=false`. It returns no ordinary successful `results`, retains
no server state, publishes no executable `next_call`, accepts no new input
fields other than the optional stateless `snapshot_guards` retry fence, and
does not involve a semantic provider. `snapshot_guards` maps every requested
file and any completed guard-only sibling file to a 64-lowercase-hex SHA-256.
The server captures and verifies the complete guarded union before it evaluates
the first retry request.

The inline continuation is read evidence from one frozen snapshot, not a
completed batch and not mutation authority. Its purpose is narrow: after the
caller corrects a selector, it can retry only the failed and unevaluated suffix
without requesting already-returned source again. That retry must copy every
guard, including files absent from the retry requests. The kernel captures the
union of requested and guard-only files and checks all hashes before evaluating
request zero. A stale completed-sibling file therefore blocks the retry. A
failure inside the first request has no complete sibling evidence and remains
fail-empty. Schema, path, parse, snapshot, cardinality, guard, and output-budget
failures also remain fail-empty.

The continuation publishes a non-executable `retry_template` so the model does
not reconstruct retry bookkeeping. Its `arguments` contain the canonical
workspace root, pending request suffix, mechanically recomputed aggregate
expectation, and every snapshot guard. Each failed selector is replaced by
`null` and named in `holes` with its rejected value and `authority=false`. The
caller fills only those holes. Replaying the template unchanged fails schema
validation ; the template cannot repeat the known-bad selector or imply a
correction.

Exact proof relations, hash-guarded executable retries, explicit clue
resolution, and declarative read missions remain separate modules. They
advance only when selector diagnostics plus inline sibling continuation cannot
meet the two-call and no-native-discovery gate.

# #Asynchronous Verification

Cold verification preserves two separate clocks:

- `elapsed_ms` is the bounded MCP request that launched or inspected the job.
- `job_elapsed_ms` is the background verification work reported by the job.

The human summary canonically labels both values as `request` and `job` ; their
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
that snapshot, the response remains an honest report of the observed state ;
the caller may make a new bounded inspection request.

# #Registration-Wide Enforcement

The canonical registration collection in `mcp_server.clj` is a permanent
ratchet and the only supported path for public tool registration. Each registry
entry declares the tool name, output schema, handler, and public outcome
classes. A separate test witness catalog keys each registered tool to the
outcome classes it exercises. The suite requires exact tool and outcome-class
equality between registration and witnesses, so a new tool, mode, or alternate
registration cannot silently bypass the contract.

For every registered tool and outcome class it checks:

- the output schema requires a non-negative numeric `elapsed_ms` ;
- the outcome reaches the single publication choke point and finalizer ;
- the summary contains the same formatted value as structured content.

The catalog covers ordinary success and typed refusal for every tool, plus
preview/commit, prepared-basis, verification-pending, and every terminal
verification class exposed by that tool. `verification-result` is deliberately
not one outcome class: the Prolog shadow found that one completion witness could
then conceal the distinct pending and failed clock laws. Adding a tool or
outcome mode without a witness makes the ordinary suite fail.

# #Linked Intent and Logic Oracle

The MCP operation contract uses stable EARS identifiers with implementation and
test witnesses. A bidirectional coherence check rejects an active intent with
no code or test witness and rejects a witness naming an unknown intent.

Status controls the gate deliberately:

- `[]` is committed work and requires a direct red test witness ;
- `[x]` requires both implementation and direct test witnesses ;
- `[D]` requires neither witness ;
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
the coherence check and oracle run from `make runtests` ; otherwise only the
native linked-intent gate remains.

# #CLI Boundary

The exact-form selector recovery evidence is transport-neutral. CLI EDN and MCP
structured output consume the same compiled available-owner vector, per-owner
hypotheses, counts, truncation state, and `authority=false` law. The CLI does
not consume the MCP envelope. CLI exit status, stdout and stderr, process
startup time, compatibility, and broader operation receipts remain separate
public contracts tracked by `clj-surgeon-9xi`.

CLI direct structural mutation also follows the shared authority tenet that
request data cannot widen effect authority. A direct `:expect`-guarded
`:edit` must start with a caller-visible named owner. Lines, ordinals, indexes,
and relative positions remain valid for reads, diagnostics, and reviewable
hash-fenced plans, but they cannot select the subject of a direct write. The
complete leaf design and retained wrong-owner falsifiers are in
[Positional mutation authority](../positional-mutation-authority/positional-mutation-authority-design.md).

# #Extraction Planning Boundary

The CLI and MCP share the pure extraction planner, not each other's transport
contracts. `inspect_clojure` exposes a top-level `plan-extraction` mission that
captures one deterministic workspace source universe and delegates to
`extract/compile-plan`. The result contains the public migration manifest
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
authoritative form list, guarded caller changes, and affected file set ; legacy
explicit counts remain accepted and must match.

Visibility changes are part of the reviewed extraction manifest, not a
post-extraction text patch. The pure planner identifies moved private forms
that remaining source owners will necessarily call through the destination
namespace and publishes that exact required set. The ready next call carries
the set as `public_forms` ; invoking apply is the authorization. Apply may also
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
commits that compiled result in the same request. This is internal compilation
not an inferred architecture decision and not a second plan representation.

Omission never means that a caller was reviewed. Any structural or quoted-Var
candidate leaves a genuine decision, so the executor refuses before writing and
returns the completed snapshot-bound plan and exact unknowns. Explicit values
remain authoritative: `public_forms: []` does not authorize a required private
form to become public, and a supplied aggregate expectation is never repaired.
Similarity and ranking evidence cannot close an unknown. This fast path is an
MCP apply contract only ; the CLI `extract!` review policy and its source-hash
surface remain unchanged.

# #Exact Repository Verifier Boundary

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
timeout from 1 through 120000 milliseconds, inclusive. An absent timeout or a
value outside that range refuses before source mutation. The profile cannot
contain hot or cold verification, and the argument vector cannot contain
`{files}`. This preserves the repository's
declared file scope and relative spelling instead of replacing them with the
transaction's absolute changed-file set. The process runner resolves only the
executable through the paved path. It preserves every remaining argument in
order, uses the canonical project root as cwd, inherits the server environment
with the paved PATH adjustment, and does not invoke a shell. Equivalence claims
cover only canonical cwd, resolved executable, remaining argument order, exit
acceptance, and captured output evidence. They do not claim identity with an
external login shell's complete environment.

Exact-exit is separate from the existing diagnostic-delta policy. In
diagnostic-delta mode, clj-kondo runs before and after a transaction with cache
disabled and EDN output so introduced findings can be classified. That is
useful for a generic changed-file profile, but it is not the command the
repository declared. Exact-exit therefore performs no pre-write verifier run
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
-> timeout / launch failure / crash-style exit: unverified, undo all files
```

The runner aggregates stdout and stderr in arrival order because the MCP command
surface already reports aggregated diagnostics. As bytes arrive, it hashes and
counts the complete captured stream while retaining only a bounded visible
prefix in memory. It never requires unbounded output retention. A passing check
returns the project profile name, acceptance policy, stable SHA-256 of the
normalized project profile definition, canonical cwd, resolved argv, exit zero
elapsed time, complete captured-output byte count and hash, and whether visible
output was truncated. The compiled in-memory profile remains execution
authority even if `.clj-surgeon.edn` changes after selection. A failed or
unverified check additionally returns bounded aggregated diagnostics. Timeout
launch failure, crash-or-signal-style exit, and ordinary nonzero exit remain
distinct so a caller does not mistake an unavailable authority for a semantic
rejection. Java exit status alone does not prove an operating-system signal ;
exit 137 is therefore not reported as proven `SIGKILL`.

Every non-pass attempts the existing receipt-backed whole-transaction undo.
Only verified undo may publish `source_unchanged=true`. A rollback failure keeps
the recovery evidence and reports manual recovery required. Neither a
deterministic verifier failure nor an unverified process outcome recommends an
automatic or blind retry. The caller must fix the reported program diagnostics
or restore the verifier authority before submitting a new guarded request.

## #Behavior matrix

| Profile / process state | Write before verifier | Public outcome | Required evidence | Final source state |
|---|---|---|---|---|
| Project `exact`, exit 0 with warnings | Candidate written and read back | success, `verification_complete=true` | profile source, cwd, resolved argv, exit 0, elapsed, output bytes/hash | candidate retained ; inverse receipt retained |
| Project `exact`, ordinary nonzero | Candidate written and read back | `verification-failed` | exit, bounded aggregated diagnostics, verified rollback | original bytes restored ; created files removed |
| Project `exact`, deadline exceeded | Candidate written and read back | `verification-unverified` / timeout | deadline, elapsed, bounded partial output, verified rollback | original bytes restored ; created files removed |
| Project `exact`, executable cannot launch | Candidate written and read back | `verification-unverified` / launch failure | resolved argv, launch diagnostic, verified rollback | original bytes restored ; created files removed |
| Project `exact`, crash-or-signal-style exit | Candidate written and read back | `verification-unverified` / crash | exit status without a claimed OS signal, bounded output, verified rollback | original bytes restored ; created files removed |
| Project `exact`, verifier non-pass and undo fails | Candidate written and read back | recovery required | verifier evidence plus per-file recovery evidence | unknown ; never claim `source_unchanged` |
| Exact profile absent, process-owned, malformed, or not `:exact-exit` | none | pre-write refusal | failed profile/source validation | source unchanged |
| `fast` or `full` selected | Existing behavior | existing profile result | existing profile evidence | existing semantics unchanged |

Shadow equivalence must precede activation. At minimum, the external command and
exact profile route must agree on canonical cwd, resolved executable, remaining
argument order, exit acceptance, and diagnostic meaning for a warning-bearing
pass, a normal lint failure, a missing file or executable, and one unverified
process outcome. Each route must publish its own complete captured-output byte
count and hash. Separate invocations need not have identical bytes when the
verifier emits nondeterministic timing text. Equivalence does not claim complete
environment identity.
The extraction boundary witness must prove the verifier observed staged
destination bytes and that failure restored every original byte, removed every
created path, and did not leave a usable receipt.

# #Exact Terminal Response

The exact-verifier success route may compile one terminal response from the
normalized public mutation receipt. This is an operation-owned projection, not
a generic transport feature:

```text
kernel result
-> normalize commit, read-back, receipt, and verification evidence
-> apply-owned terminal-response projector
-> shared finalizer adds elapsed_ms only
-> apply summary and structured serialization
-> one callback
```

The projector is total and non-throwing. It returns the constant text
`Done — changes committed and exact verification completed.` only when the
normalized result proves all of the following:

- `ok=true`, `committed=true`, `verification_complete=true`, and
`next_action=none` ;
- non-empty whole-file read-back hashes, inverse receipt path, and receipt hash ;
- project-owned profile `exact`, acceptance `exact-exit`, process outcome
`pass`, and exit zero.

Missing, malformed, or inconsistent evidence omits the field. It does not turn
an already-classified domain result into an MCP transport error. Non-exact
success, pending verification, typed refusal, ordinary failure, rollback
timeout, launch failure, crash-style exit, and every unverified state retain
their existing result and summary.

The optional `terminal_response` field is published in structured content. The
visible apply summary contains the identical string exactly once while
preserving its existing elapsed time, warnings, and terminal evidence. The
string does not interpolate request text, paths, source, diagnostics, verifier
output, receipt identifiers, or project-controlled values. It does not claim
behavior preservation, semantic caller completeness, tests, or "all checks."

Agent routing is conditional: if `terminal_response` is present and this
mutation completes all remaining user-requested work, return its value exactly
without rereading, reverifying, or writing a second summary. If work remains
the response is terminal evidence for this operation only and the agent
continues. This preserves model judgment about task completion while deleting
mechanical post-receipt narration.

`next_action=none` and `terminal_response` describe only the completed
mutation. They never prove that the complete user request is finished. The
caller continues when any user-requested work remains.

The shared operation finalizer, MCP server transport, CLI entrance, and other
tools do not change in this slice. Generalization to compact edit or transform
requires a separate evidence law because their terminal verification meaning
is weaker than a project-owned exact-exit profile.

# #Tolerant Direct-Change Compilation

Direct `changes` keep exact per-change `matches`, `each_form`, and `each_file`
guards as mutation authority. Top-level aggregate `expect` is bookkeeping that
the compiler can derive after every change has validated. Its absence therefore
does not create an unknown, and a supplied disagreement does not override the
guarded compiled transaction. The public result reports that normalization so
the caller can learn the smaller request shape.

Insertion arrays remain explicit action boundaries, but one array item may be
a pasted block containing several complete forms. The compiler parses the
complete block losslessly and expands it into ordered insertion forms. It does
not auto-balance delimiters, detach comments, select an owner, or repair source.
Malformed input refuses before write at the original array-item path.

Verification remains proportional and caller-owned. The tool contract shall
not invite a model to add `verify` merely because it is available. A caller
uses a project-owned transaction profile only when the user or repository
explicitly requests that profile ; otherwise whole-file parse, read-back, and
receipt evidence remain the mutation proof.

Top-level `edits` and `changes` are alternative request languages, not arrays
to concatenate. When every action is a compact exact replacement, `edits` is
the smaller language. If any action needs insertion, deletion, rename
map-entry insertion, or another direct-change operator, the caller expresses
the complete atomic decision in `changes`. The public schema and tool text make
that choice explicit before payload construction.

A malformed packed insertion does not grant the compiler authority to repair
source syntax. The refusal may return a non-executable `retry_template` that
preserves every valid request field and replaces only the malformed array item
with a null caller-owned hole. The template names that exact path and publishes
`selector_authority=false` and `write_authority=false`. It is not a
`next_call` ; after the caller supplies valid syntax, the ordinary request starts
validation again with no inherited authority.

# #Injective Compact Location Normalization

The compact `edits` request language may accept three alternate spellings of an
otherwise explicit edit location. This is representation tolerance, not owner
selection. Each accepted spelling must prove one structural address from the
request and the frozen source snapshot. If the proof is incomplete, the
ordinary request refuses before write.

The normalizer belongs only to the compact edit compiler used by
`edit_clojure` and the `edits` branch of `apply_clojure_changes`. It does not
change generic direct `changes`, retained-basis changes, extraction, computed
programs, or CLI `:forms` semantics. A source-blind JSON adapter may preserve an
omitted location, but it cannot infer or default that location. The pure
normalizer runs after all target source bytes have been captured once and
before the unchanged generic transaction compiler resolves explicit owners.

```text
compact edit request
-> source-blind shape validation
-> one frozen source capture
-> pure compact location normalization
-> explicit named-owner or namespace selector
-> typed pre-write refusal
-> unchanged direct transaction compiler
-> existing parse, atomic write, read-back, receipt, and rollback
```

The normalizer receives the complete compact request, canonical file selector
parsed `from` and `to` forms, declared positive match count, and frozen source
map. It returns an ordinary request with an explicit location plus bounded
normalization evidence. It never writes, formats, invokes semantic tooling, or
retains authority. The generic compiler revalidates the emitted request and
remains the mutation authority.

## #Relation A: exact namespace name in named-owner position

If `within.form` resolves exactly one named owner, that owner remains
authoritative and no fallback runs. If it resolves more than one owner, the
request refuses as ambiguous. Only when it resolves zero named owners may the
normalizer compare the supplied string with the parsed name of the file's one
direct namespace owner. Exact equality emits explicit namespace scope. Zero or
several namespace owners, a different name, or reader-conditional ownership
refuses.

## #Relation B: complete namespace clause without a location

An omitted location may become namespace scope only when all of the following
are true:

- the edit identifies exactly one supported Clojure source file, either as
`file` or a singleton `files` vector ;
- `from` and `to` each parse as one complete list-shaped namespace clause with
the same recognized clause keyword ;
- the file has exactly one direct namespace owner ;
- the declared match count equals the number of lossless `from` fingerprints
among the namespace's direct clause children ;
- the namespace-descendant count and whole-file count equal that direct-child
count, proving that no nested or external lookalike competes ; and
- no reader-conditional or platform ambiguity owns the selected clause.

Inside that complete proof, a singleton `files` vector becomes the identical
scalar `file`. A different clause kind, detached comment, malformed form
nested-only match, competing equal subtree, stale fingerprint, or count
mismatch leaves the request unnormalized and therefore refused by the compact
contract.

## #Relation C: complete named top-level owner without a location

An omitted location may become named-owner scope only when all of the following
are true:

- the edit identifies exactly one supported Clojure source file ;
- `matches` is exactly one ;
- `from` and `to` each parse as one complete named top-level form ;
- both forms have the same owner kind and exact owner name ;
- the frozen file has exactly one corresponding direct owner ; and
- that owner's complete lossless fingerprint equals `from`.

The complete fingerprint, not kind and name alone, is the stale-source guard.
It retains comments, metadata, reader macros, and token spelling while ignoring
only insignificant whitespace according to the existing transaction kernel.
An anonymous form, renamed owner, changed owner kind, nested lookalike
duplicate owner, reader-conditional ambiguity, or stale count refuses.

## #Result evidence and atomic refusal

Successful normalization reports the edit identity or index, the relation used
the requested location shape, and the emitted explicit location. It reports no
source body and grants no authority beyond the ordinary transaction that
rechecks it. A request containing several edits may use several relations, but
all locations compile against one frozen source map. If one edit cannot be
proved, the complete transaction refuses before mutation ; successful sibling
normalizations do not create partial write authority or an executable retry.

Omitted `within` never means root scope. Similarity, edit distance, source
proximity, a unique lexical hint, or a sole remaining candidate never satisfies
these relations. EDN root-scoped edits retain their existing explicit contract.

# #Injective Compact Edit Field Normalization

The compact editor accepts three closed spellings of one exact replacement
relation: canonical `from`/`to`, `old`/`new`, and `before`/`after`. The alias
compiler is a source-blind representation boundary, not a source-repair or
semantic inference engine. It runs before location normalization, path
resolution, source capture, and the unchanged transaction compiler.

```text
compact edit JSON
-> exact value-pair algebra
-> preserve from/to
-> lower old/new to from/to
-> lower before/after to from/to
-> typed pre-source refusal
-> ordinary compact location compiler
-> unchanged direct transaction compiler
```

Exactly one complete pair is accepted per edit. Partial pairs, cross-pair
mixtures, two complete pairs, and canonical fields combined with an alias pair
refuse the complete request even when their values agree. One invalid sibling
refuses the batch ; successful siblings grant no partial normalization or write
authority. The lowering preserves the two values byte-for-byte and emits only
the canonical field names plus bounded relation evidence.

This seam belongs only to the compact `edits` route. Generic `changes`, basis
continuations, extraction, programs, CLI request semantics, and source-aware
location normalization do not invoke it. Refusals name the exact edit index and
supplied value fields, state the complete mapping, and tell the caller to retry
the exposed `edit_clojure` operation. They do not redirect the caller to the
heavier public tool.

# #Closed Compact Relations

Closed compact relations let a caller state one already-decided mechanical
relationship once instead of repeating its expanded source fragments. The
first relation mode pairs an exact symbol migration with an exact require
change. It is available only through the MCP compact-edit request language used
by `edit_clojure` and the compact `edits` branch of
`apply_clojure_changes`. It lowers to the same ordinary guarded edits
compact-location normalization, and generic transaction compiler as the
normalized flat representation.

The relation compiler is a pure request facade. It owns no source discovery
plan identity, cache, write path, receipt, or retry authority. Its output is
ordinary compact data, bounded normalization evidence, or one typed refusal.
The existing transaction remains the only component that captures source
authorizes mutation, writes, reads back, verifies, receipts, and rolls back.

Implementation status on 2026-08-29: the pure facade, paired admission,
single-capture lowering, atomic apply path, and non-relation isolation are
implemented under `MCP-OP-EDIT-020..024` and `MCP-OP-EDIT-026..027`.
Performance promotion remains on **HOLD** under `MCP-OP-EDIT-025`. In the first
real-mutation block, all four calls were semantically correct and exact
verified, and the relation midpoint was descriptively lower. One normalized-flat
call expressed the same request edit multiset in a different order and therefore
failed the predeclared exact canonical-transaction identity gate. Block 2 did
not run. The gate remains unchanged; any order-invariant canonicalization is a
separate design decision and requires a new cohort.

Canonical effect identity is approved for implementation as the forward-only
answer to that decision. This LLD defines it after generic disjointness proof.
It does not alter the held scorer, authorize the held second block, or mark
performance promotion complete. Active requirements `MCP-OP-EDIT-028..030`
own the pure projection, permutation/refusal law, and bounded public evidence.

Superseding promotion evidence on 2026-08-29 closed that hold without
rescoring or splicing the retained attempt. Candidate `90b47d1` ran one fresh
whole `N R R N` then `R N N R` real-mutation cohort on Anvil dev-a. Both block
environment fences passed, all eight calls were correct, route-adherent,
one-shot, exact-verified transactions, and the scorer returned
`promote=true`. Relation `T_emit` improved by 40.21 percent in Block 1, 37.75
percent in Block 2, and 37.90 percent pooled. Complete verified time improved
by 35.72 percent, 34.71 percent, and 33.99 percent respectively. This evidence
completes `MCP-OP-EDIT-025`; the earlier HOLD remains the historical reason
canonical effect identity and the second counterbalanced block were required.

## #Public request shape

The first slice admits `symbol_migration` and `require_change` only as a pair.
Both fields are closed objects, and their ordered canonical file lists must be
identical. A request may also contain ordinary compact `edits` and exact
`delete_owners`. The `apply_clojure_changes` compact branch retains its
existing `verify` field ; `edit_clojure` does not gain one. The relation pair
may not combine with `changes`, `programs`, extraction, a retained basis, or
another relation spelling.

`symbol_migration` has this closed shape:

| Field | Contract |
|---|---|
| `target_alias` | One nonblank Clojure alias symbol. |
| `target_rule` | Exactly `preserve-name`. |
| `columns` | Exactly `[owner, from, matches]`, in that order. |
| `files` | A nonempty ordered vector of `[file, rows]` tuples. Each canonical file is unique and each ordered `rows` vector is nonempty. |
| row `owner` | One nonblank exact named top-level owner. |
| row `from` | One unqualified or singly qualified Clojure symbol token. |
| row `matches` | One positive exact integer count. |

Each migration row is unique by canonical file, owner, and source symbol. The
only derived value is `target_alias/name (from)`: the qualifier changes and the
exact unqualified name is preserved. The relation does not rename the local
name, resolve Vars, discover references, select owners, or choose a target
namespace.

Alias, owner, namespace, and symbol fields use closed lexical symbol predicates;
public relation data never enters the Clojure reader. `from` must be one exact
unqualified or singly qualified symbol spelling with a nonblank name. The
reader literals `nil`, `true`, and `false`, keywords, strings, quoted forms,
metadata-bearing forms, reader forms, and multiply qualified spellings are
outside this slice. The
generated target must differ from `from` ; a target alias equal to the source
qualifier is a no-op refusal.

`require_change` has this closed shape:

| Field | Contract |
|---|---|
| `add` | One exact `{lib, as}` namespace and alias pair. |
| `files` | A nonempty ordered vector of `{file, remove?}` objects with exactly the migration file set. |
| file `remove` | Omitted, or one exact `{lib, as}` pair to remove from that file. |

The normative JSON shape for one file is:

```json
{"symbol_migration": {"target_alias": "next"
                      "target_rule": "preserve-name"
                      "columns": ["owner", "from", "matches"]
                      "files": [["src/sample/core.clj", [["render", "old/view", 2]]]]}
 "require_change": {"add": {"lib": "sample.next", "as": "next"}
                    "files": [{"file": "src/sample/core.clj"
                               "remove": {"lib": "sample.old", "as": "old"}}]}}
```

Every object shown is closed: `symbol_migration`, `require_change`, `add`
each require file, and `remove` reject additional properties. File paths
owners, aliases, namespaces, and symbols are JSON strings ; `matches` is a JSON
integer. Each migration file tuple has exactly two elements, `[file, rows]`.
Each migration row has exactly three values aligned with the fixed columns.
Missing or additional tuple or row values refuse. Presence of either top-level
relation key activates paired admission, including when its decoded value is
`null`. A null relation, add, file, row, or removal refuses. Omission of a
file's `remove` property is the only spelling that means no removal.

The migration `target_alias` must equal `require_change.add.as`. The explicit
`add.lib` is the target namespace for every generated qualified symbol. The
compiler never derives a namespace from an alias. A removal is a caller-owned
decision ; absence of `remove` means no removal, not inferred cleanup.

The two ordered file vectors are compared element-by-element after canonical
root-confined resolution. Each canonical file occurs once. Two raw spellings
of one canonical path within either vector refuse. Across the vectors, the same
normalized workspace-relative spelling must occur once at the same index ;
different spellings that resolve to the same file refuse. The required paired
occurrence is not a duplicate. Public evidence retains those normalized paths
in request order ; absolute resolved paths remain internal. Canonicalization may
validate identity, but it cannot reorder files.

Unknown keys, duplicate decoded entries, partial pairs, cross-pair mixtures
legacy fields that mask an incomplete pair, empty values, nonpositive counts
duplicate files or rows, file-set mismatch, unsupported target rules, and
disallowed route combinations refuse before source capture. The application
does not claim authority over duplicate raw JSON keys already collapsed by an
upstream decoder.

## #Compilation pipeline and single capture

Compilation proceeds in one direction:

```text
compact MCP request
-> validate the closed relation pair and route combination
-> run existing source-blind compact edit-field normalization
-> lower symbol rows source-blind to ordinary owner-scoped compact edits
-> combine relation files with literal-edit and owner-deletion files
-> resolve a unique canonical, root-confined capture universe
-> capture that complete source map exactly once
-> lower require changes from the captured source map
-> combine require, symbol, normalized literal, and deletion actions
-> run existing compact location normalization
-> run the unchanged generic transaction compiler and effect path
```

Existing compact edit-field normalization and source-blind symbol lowering
establish every literal and relation edit before the transaction captures
source. The paired file-set law therefore makes `require_change` source-aware
without a private `capture_files` protocol, prepared-request branch, or second
read. Literal edits and owner deletions may add files to the capture universe
but the require compiler may not widen it.

The pure facade has two explicit phases. Phase A accepts the decoded request
validates the closed pair, normalizes caller-supplied edit fields through the
existing source-blind adapter, emits canonical symbol edits, and returns the
complete ordered declared file references plus closed pending require data. It
performs no I/O or canonical filesystem resolution. The declared intent comes
from normalized literal edits, generated symbol edits, expanded
owner-deletion actions, and the paired relation. Phase A consumes and removes
`symbol_migration` after it emits symbol edits ; only the closed
`require_change` remains as non-executable pending data. The existing
root-confined resolver maps that complete intent to one unique canonical
capture universe, rejects aliases, and captures it exactly once.

Phase B accepts only the pending require data and that captured source map. It
emits complete namespace edits, consumes and removes `require_change`, and
delegates to compact-location normalization. Its output contains neither
public relation key. Neither phase may be applied twice to the same internal
request. Phase B cannot add a file, read or recapture source, or retain an
independently executable plan.

Canonical root confinement and path-alias rejection occur before capture. Two
request spellings that resolve to the same canonical file refuse instead of
silently coalescing. The pure require lowerer receives the already-captured map ;
it cannot read a path, stat a file, query a semantic provider, invoke a parser
service, or recapture after a refusal.

## #Frozen-source require lowering

Each paired file must contain exactly one direct top-level namespace and one
direct `:require` clause. The first slice accepts only comment-free direct
vector libspecs. Prefix lists, reader conditionals, platform-specific clauses
unsupported libspec options, detached comments, duplicate owners, and
ambiguous clause ownership refuse.

Against the frozen namespace, the compiler proves all of these facts:

1. The declared target namespace is absent from every direct libspec
regardless of alias or options.
2. Its alias is not bound to any direct libspec.
3. Each declared removal identifies exactly one direct libspec.
4. The add and removal pairs are distinct, and a removal cannot use the target
alias.
5. No undeclared removal or layout choice is required.
6. The emitted namespace edit is one complete exact clause replacement with
namespace scope and one match.

Removal decisions and migration source qualifiers are independent. A file may
retain its old alias for unrelated uses, and several migration rows in one file
may have different source qualifiers. The relation compiler does not infer a
removal from those rows or require `from` qualifiers to equal `remove.as`.

Byte generation follows one lossless relation. The compiler removes an exact
declared libspec first. For a non-first removal it removes that node and its
immediately preceding whitespace separator ; for the first removal it removes
the node and its immediately following whitespace separator. When that would
remove the sole libspec, it replaces that exact libspec node with the target
instead. Otherwise it appends the exact canonical vector
`[add.lib :as add.as]` after the last surviving direct libspec and before the
clause delimiter, copying the exact whitespace separator immediately preceding
that last survivor. The separator must contain only whitespace and commas. An
empty clause, a missing separator, a
comment-bearing separator, or a separator that cannot be associated with one
direct sibling refuses. Single-line and multiline clauses are both supported
when this relation is unique ; trailing namespace whitespace and newline bytes
outside the replaced clause remain unchanged.

The lowerer preserves every unrelated byte and comment. For an admitted
request and frozen source map, it emits one deterministic byte result. When
that result is not uniquely determined by the supported syntax, it refuses ; it
does not choose a layout, infer an unused require, or normalize nearby source.

## #Composition, disjointness, and authority

The composed request has four ordered action classes:

1. generated require-clause edits ;
2. generated symbol-migration edits ;
3. caller-supplied literal compact edits ; and
4. exact owner deletions.

Within each class, request file and row order is stable. Across classes
canonical addresses must be disjoint. Duplicate generated rows, two edits that
address the same exact subtree, a literal edit that overlaps a generated edit
or deletion of an owner containing a generated or literal edit refuses the
complete request. One stale owner, subtree, count, or source hash also refuses
the complete request. Successful sibling lowering never creates partial write
authority or an executable retry.

Disjointness is decided after the ordinary compiler resolves matches against
the frozen map. Every generated and literal match has a canonical file and
half-open byte span. All spans must be pairwise nonintersecting: identical
nested, and partially intersecting spans refuse. A deletion owner may contain
no generated or literal span. A generated replacement whose future value is
equal to its source value refuses as a no-op. The generic compiler remains the
span and overlap authority ; the relation facade may reject obvious duplicate
file/owner/row identities earlier but cannot declare unresolved edits disjoint.

Expanded and declared rows are charged against the existing request, file
edit, match, source-byte, and output-byte budgets before mutation. Every
generated edit then passes through the ordinary compact-location normalizer and
generic compiler. That compiler re-resolves owners, exact `from` subtrees
cardinality, future parse, and frozen hashes. Relation lowering is therefore
representation compilation, not mutation authority.

Budget accounting is singular. Wire and decoded-request limits apply to the
submitted pair, including raw relation rows. The canonical file limit applies
to the unique resolved union. Expanded edit and match limits use the canonical
direct actions after the existing source-blind adapter expands grouped literal
files and owner-deletion groups, plus the generated migration and require
actions. Each canonical action contributes its authoritative match count.
Source-byte limits run once on the captured map ; future-output limits run once
on the generic compiled result. The same action is never charged again merely
because it crossed a lowering phase.

## #Canonical effect identity after proof

Caller order remains immutable request provenance. It owns request and relation
indexes, generated IDs, diagnostic paths, diff presentation, receipt vectors,
and exact request replay. It does not become mutation authority after the
generic compiler has resolved every compact action against one frozen source
map and proved the complete concrete edit set disjoint.

The transaction namespace exposes one pure `canonical-effect-identity`
function over a canonical project root and a successful compiled transaction.
The caller invokes it only after `compile-transaction*` has returned `ok=true`;
it does not accept a wire
request, source map, partial plan, refusal, or receipt. It validates that every
compiled file resolves under that already-canonical root, has complete source
and result hashes, and that every concrete effect has one project-relative
file identity, structural address, exact
original string, and exact replacement string.

The internal version-one projection is a vector, not an unordered map:

```text
[:canonical-effect/v1
 [[:file normalized-project-relative-file
   source-sha256 result-sha256
   [[:effect resolved-kind
     structural-path preorder end-preorder raw-offset
     exact-before-string exact-after-string]
    ...]]
  ...]
 file-count effect-count]
```

The function performs no file read or `stat`; it only relativizes the already
resolved compiled file identities against the supplied canonical root and
refuses an outside-root or empty identity. Files are sorted lexicographically
by normalized project-relative path. Effects within a file are sorted by
structural path, preorder, end preorder, raw offset, resolved kind, exact
before string, and exact after string. `raw-offset`
is the compiler's exact source-string offset when present, not a separately
computed byte index. The
resolved kind is `insert-left` or `insert-right` when the concrete edit carries
that insertion side, `delete` when the concrete edit carries deletion, and
`replace` otherwise. A missing required field is an internal contract error ;
the function never invents a default.

`exact-before-string` and `exact-after-string` are the complete JVM strings
already consumed by `apply-edits`. They are not parsed, whitespace-normalized,
formatted, fingerprinted, truncated, or replaced by hashes inside the
projection. Structural paths and numeric address fields remain numeric vectors
and integers. The projection is serialized with one versioned canonical EDN
vector grammar and hashed with SHA-256. Because the collision boundary contains
the exact strings and exact resolved addresses, two semantically different
concrete effects cannot become equal merely through normalization. SHA-256 is
the public compact identifier for that exact internal projection ; it is not
used to discard or deduplicate effects.

For the current branch, the projection intentionally excludes create effects.
A request that includes `create_files` therefore publishes a successful result
without `canonical_effect_identity` and with explicit suppression metadata:
`canonical_effect_identity_suppressed_reason="create-files-present"` until the
versioned create-aware identity contract (MCP-OP-EDIT-033) is approved.

The public successful compact result contains the closed object
`canonical_effect_identity={version,sha256,files,effects}`. It contains no
source body, replacement body, absolute path, request ID, receipt path, or
receipt hash. Both `edit_clojure` and the compact `edits` branch of
`apply_clojure_changes` publish it. Generic `changes`, extraction, programs,
retained basis, planning, and CLI results do not publish it in the first slice.
Relation lowering receives no special identity path: it produces ordinary
compact edits, and the identity is derived only from their common compiled
result.

Permutation invariance is authorized only for complete successful compact
transactions. A permutation of the same resolved disjoint effects has the same
projection, SHA-256, file count, effect count, source hashes, result hashes, and
future bytes. The submitted request hash, positional IDs, diagnostics, diff
order, receipt bytes, receipt hash, and inverse-edit vector may differ. Those
differences remain valid provenance and rollback evidence and are never inputs
to canonical effect identity.

The following existing consumers remain unchanged:

| Consumer | Existing identity/order authority | Canonical-effect change |
|---|---|---|
| `compile-transaction*`, `compile-file`, `assert-disjoint-edits!`, `apply-edits` | Resolve original-snapshot effects, refuse intersections, apply address order. | Pure identity reads their successful output; no execution reorder. |
| `build-receipt`, `receipt-hash`, `validate-receipt!`, `compile-inverse` | Exact receipt bytes, inverse vectors, rollback validation. | No field or hash change; receipt identity remains request-specific. |
| `commit-compiled!`, `observe-change-result` | Commit/read-back evidence and runtime outcome. | Success copies the already-derived compact identity; it grants no write authority. |
| `mcp-contract/normalize-success-receipt` | Public terminal receipt, read-back, and verification evidence. | Adds only the closed compact identity object when present. |
| `mcp_compact_relations` normalization evidence | Ordered request files, relation rows, and generated IDs. | Remains ordered provenance and is not canonical-effect input. |
| cold verification and undo attachment | Receipt path and receipt hash. | Unchanged; they never accept canonical-effect SHA as receipt authority. |
| relation causal scorer | Frozen request representation and exact future/verifier evidence. | A new candidate compares canonical-effect identity; the held scorer and cohort remain immutable. |

The projection never makes an unresolved batch commutative. Identical or
intersecting spans, parent/nested edits, deletion of an edited owner, two
insertions at one boundary, and transformations that would need another edit's
future output refuse before any identity exists. Form order inside one insertion
payload remains explicit caller authority. A sequence such as `old -> middle`
then `middle -> new` is one composed replacement or two transactions ; request
permutation cannot make it one snapshot-compiled batch.

## #Refusal and rollback matrix

| Condition | Stage | Result law |
|---|---|---|
| One relation field absent, malformed, unknown, duplicated, or combined with a disallowed route | source-blind admission | Typed pre-source refusal ; no capture and `write_authority=false`. |
| Ordered relation file sets differ, alias bindings disagree, or a canonical path aliases another | source-blind admission/path resolution | Complete pre-capture refusal ; no sibling continuation. |
| Namespace, require clause, addition, removal, comment, reader conditional, platform, or layout is missing or ambiguous | frozen-source require lowering | Complete pre-write refusal against the one captured map ; no recapture or guessed remedy. |
| Generated, literal, or deletion actions overlap, duplicate, exceed a budget, or fail ordinary generic validation | composition/generic compilation | Complete pre-write refusal ; every source remains unchanged. |
| A source guard changes before the first write | commit guard | Existing stale-source refusal ; no recompilation against newer bytes. |
| A race or configured verifier failure occurs after writing begins | existing effect path | Existing failure-atomic rollback and read-back proof ; no blind retry. |
| Receipt publication fails after writes begin, or write channel staging has an unrecoverable error | existing effect path, publication-specific inverse | Rollback edits and all creations with full proof; no `source_unchanged=true` if any restore proof is incomplete. |
| Rollback cannot prove restoration | existing effect path | Recovery-required outcome ; never claim `source_unchanged=true`. |

The transaction promises failure atomicity with rollback, not simultaneous
multi-file isolation. Similarity, edit distance, source proximity, lexical
ranking, and semantic-provider evidence cannot satisfy a missing relation
decision.

Only a failure originating in Phase A, Phase B, or relation composition adds a
closed `compact_relation_diagnostic` object to the ordinary structured result.
The ordinary envelope has these required top-level fields:

| Field | Contract |
|---|---|
| `error_type` | Stable relation-specific keyword spelling serialized as a string. |
| `error` | One bounded human-readable reason. |
| `mutation_attempted` | Exactly `false`. |
| `write_authority` | Exactly `false`. |
| `next_action` | Exactly `correct_request`. |

`compact_relation_diagnostic` contains `failed_stage`, one of
`relation-admission`, `path-resolution`, `require-lowering`, or
`relation-composition`, plus optional `path`. When present, `path` is the closed
object `{field, file_index?, row_index?}`. `field` is the exact top-level JSON
field string ; indexes are zero-based integers. The diagnostic omits `path` when
no single request location owns the failure.

The first slice has one error type per relation-owned stage:
`invalid-compact-relation`, `compact-relation-path-conflict`
`require-change-unprovable`, and `compact-relation-overlap`, respectively.
Failures delegated to existing compact or transaction stages retain their
existing error type and ordinary envelope and omit
`compact_relation_diagnostic` ; they are not rebranded as relation failures.

The refusal includes no source body, generated partial request, executable
retry, or `terminal_response`. One exception is named and bounded: an
`invalid-compact-relation` refusal that names a `symbol_migration.files` entry
carries `expected_shape_example`, a single illustration of the accepted
`[file, rows]` shape of at most 200 characters, built only from values the
caller supplied in that same request. It is an example, not a request — it is
not executable, is not a normalized or partial version of what the caller sent,
grants no retry authority, and is derived without any source read. An oversized
caller path is shortened from the middle with a visible elision marker, and if
even that does not fit, one fixed schematic example is used; the field is never
omitted for an applicable refusal, because a caller that cannot see the accepted
shape retries the shape that was just refused.

The same rule governs the visible text block for every refusal, not only this
one: whenever the structured receipt carries a one-sentence `error`, that
sentence appears verbatim in the text, after the error type and request path and
before the remedy. The text a model reads is a superset of the structured
refusal, never a lossy summary of it. `MCP-OP-EDIT-037` owns both halves.

It may claim `source_unchanged=true` only after
the source boundary has proved that no write began. Once effects begin, the
existing verification, rollback, recovery-required, and manual-recovery
envelopes own the outcome ; relation diagnostics do not overwrite them.

## #Result evidence and route isolation

A successful relation transaction must contain the top-level structured-result
field `compact_relation_normalization` ; a relation-absent transaction must omit
that field. The object has this closed shape:

| Field | Contract |
|---|---|
| `version` | Integer `1`. |
| `relations` | Exactly the JSON string vector `["symbol_migration", "require_change"]`. |
| `target_rule` | Exactly `preserve-name`. |
| `files` | Complete ordered normalized workspace-relative relation file vector. |
| `migration_rows` | Declared migration-row count. |
| `require_files` | Declared require-file count. |
| `literal_edits` | Caller-supplied literal-edit count. |
| `deleted_owners` | Exact named-owner deletion count. |
| `declared_matches` | Sum of authoritative expanded match counts. |
| `expanded_edits` | Total expanded edit count charged to the transaction. |
| `edit_ids` | Complete ordered generated IDs: `relation/require/N` then `relation/symbol/N`. |

Generated IDs use unpadded zero-based decimal ordinals assigned in request
order after canonical file resolution. Require IDs precede symbol IDs in the
evidence vector ; each class has its own ordinal sequence beginning at zero.

The existing request limits bound the complete file and ID vectors, so this
evidence is never silently truncated. A result that cannot return the complete
evidence within the public output budget refuses before mutation.

It returns no source bodies and does not imply that relation lowering verified
program semantics. Transaction hashes, read-back, receipt, verifier, rollback
and terminal-response evidence retain their existing owners and meanings.

Flat compact edits remain supported. Relation-absent requests do not call the
relation lowerer. Generic `changes`, programs, retained-basis changes
extraction, CLI operations, and unsupported CLJC require shapes do not expose
or invoke this relation mode. Both MCP tools project the same relation fields
and lowering evidence ; only `apply_clojure_changes` may select its existing
transaction verifier. CLI projection is a separate future adapter over the
same pure compiler ; the first slice changes only the measured compact MCP
request language.

## #Causal acceptance boundary

The relation mode is not promoted because it uses fewer bytes. Its hypothesis
is that naming a complete repeated relationship lets the caller emit less exact
request syntax without losing a decision or verified outcome. A
same-candidate, same-surface, real-mutation cohort must therefore
compare the already-correct normalized flat representation with the relation
representation while holding the production description, schema, location
normalizer, transaction, verifier, task, model, and scorer constant.

Both arms must make exactly one compact `apply_clojure_changes` call with the
same project-owned `verify="exact"` profile, compile to byte-identical canonical
effect identities and frozen future files, and complete exact verification in
that first call. This benchmark shape preserves the authority boundary:
`apply_clojure_changes` owns verifier selection and `edit_clojure` does not gain
it. The relation arm must reduce median request-emission time in both
counterbalanced blocks and by at least 20 percent pooled. It must independently
reduce complete verified wall time by at least 20 percent pooled. Capture-only
evidence, payload-size reduction, meaning-preserving byte drift, or a
complete-wall win without the emission-time result cannot promote the
mechanism.

The immutable causal protocol defines two four-run serial blocks:
`N R R N`, then `R N N R`, where `N` is normalized flat and `R` is the
relation representation. Every post-launch attempt is retained. Both arms see
the same relation-capable tool catalog, task, model, effort, fixture, verifier
and scorer. `T_emit` runs from turn start through the observer event containing
the complete tool arguments. `T_apply_verified` runs from turn start through
the exact first-call MCP verification completion. `T_complete_verified` runs
from turn start through the final response after that verified mutation. The
latter is the primary user-outcome metric ; the former localizes server and
post-result time. Medians and improvement are computed per block and pooled
only from eligible exact runs ; a missing,
incorrect, nonadherent, retried, or unverified run fails the cohort rather than
being dropped. The complete identity, isolation, estimator, and stop laws live
in the referenced Correct Flat-Control Causal Protocol.

# #Compact Root-Scoped Data Edits

`edit_clojure` admits `.edn` only for an exact literal edit whose location is
`within.root`. Root scope searches the complete lossless concrete-syntax tree ;
it does not imply replacement of the whole document. The exact `from` subtree
and cardinality remain the stale-source and ambiguity guards.

One edit may name either one `file` or an explicit non-empty `files` array.
Grouped `files` are available only with root scope. `matches` defaults to one
and is authoritative per file. The adapter derives the aggregate match count
expands the gesture into the existing direct transaction representation, and
commits all affected files against one frozen snapshot. Duplicate files
unsupported extensions, a count mismatch in any file, malformed future data
or any write failure refuses or rolls back the complete transaction.

EDN does not gain namespace, named-owner, owner-deletion, extraction, semantic
index, or formatter behavior. The kernel parses and rewrites concrete syntax ;
it does not invoke tagged-literal readers or evaluate data. Bytes outside the
exact replaced subtrees, including comments and metadata, remain under the
existing lossless transaction contract.

# #Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|---|---|---|---|
| Common evidence owner | Shared runner/finalizer and one publication choke point per public handler | Server-registration middleware ; independent handler instrumentation | The boundary is visible around domain execution without hiding asynchronous semantics or allowing early-return or per-handler drift. |
| Elapsed authority | One handler-entry monotonic request clock | Preserve any domain-provided elapsed value ; measure during serialization; end-to-end wall clock | One clock has a stable owner and meaning. Other phases retain distinct names. |
| Summary contract | Operation-specific prose containing the finalized duration | One universal summary grammar ; structured-only timing | Existing concise summaries remain useful while humans can see performance without inspecting structured content. |
| New-tool enforcement | Registry entries declare outcome classes ; an independent witness catalog must match | Fixed list of current tools; finalizer-only proof; documentation review | Registration-derived outcome coverage makes omission fail when either the tool surface or a tool's public modes grow. |
| Intent status gate | `[]` needs tests, `[x]` needs code and tests, `[D]` is exempt | Gate only implemented specs ; require every non-deferred spec to be fully implemented | Tests preload active intent while genuinely deferred work remains non-blocking. |
| Prolog retention | Keep only after an independently found native-test gap | Retain unconditionally ; never model relational states | A second model earns maintenance cost only by demonstrating additional fault-finding power. |
| CLI reuse | Share only the transport-neutral exact-selector recovery compiler | Reuse the MCP envelope directly ; duplicate all evidence; defer all parity | The owner universe and hypotheses are domain evidence, while transport envelopes and process semantics remain distinct. |
| Internal extraction completion | Preserve omitted visibility through validation, derive only from the executor's complete frozen workspace, and reuse the existing extraction transaction | Require public `plan-extraction` ; add `resolve: mechanical`; route CLI through MCP policy | Omission is the shortest honest caller shape, explicit values stay authoritative, and no second plan algebra or CLI compatibility change is introduced. |
| Extraction planning entrance | A top-level `inspect_clojure` mission over the shared pure planner | Fifth public tool ; typed batch read operation; CLI subprocess | Planning is a coherent workspace-wide read mission, not one file read, and reuses the existing public read envelope. |
| Plan-to-apply authority | Exact source hash plus explicit caller decisions | Retained in-memory plan ; similarity; unguarded replay | The hash is transport-neutral, stale-safe, and grants no implicit write authority. |
| Workspace source universe | One deterministic shared scanner for planning and execution | Duplicate scans in each handler ; semantic index as authority | Both phases must reason over identical eligible paths and exact bytes without another index lifecycle. |
| Exact repository verifier | Project-owned `"exact"` profile with `:acceptance :exact-exit` ; execute one closed argv after candidate read-back | Arbitrary request command; generic `verify=fast`; clj-kondo diagnostic delta; external second action | It deletes one model boundary without changing file scope, warning policy, command arguments, or rollback authority. |
| EDN edit scope | Exact root-scoped literal edits, optionally grouped across explicit files | Extension allowlist only ; all structural operations; native patch only | Root scope reuses the lossless transaction kernel while preventing namespace/owner claims that EDN cannot support. |
| Selector recovery | Per-failed-owner bounded hypotheses with no automatic selection | Aggregate candidates, automatic fuzzy selection, or immediate retained continuation | One refusal gives the model enough real structure for an exact retry without letting presentation rank become authority. |
| Compact location tolerance | Three compact-only injective relations over one frozen snapshot, lowered to explicit generic selectors | Global namespace fallback ; root-scope default; fuzzy owner selection; source-blind inference | The accepted spellings recover observed model mistakes while every zero/many, stale, nested, or competing case remains a pre-write refusal and generic CLI/direct semantics do not widen. |
| Compact edit field tolerance | One source-blind closed algebra that preserves `from`/`to` and lowers exactly one complete `old`/`new` or `before`/`after` pair | Prompt-only correction ; fuzzy key repair; accept equal duplicate pairs; widen generic changes | The three observed spellings encode the same exact guarded relation, while all 61 other six-field subsets remain pre-source refusals. |
| Closed compact relations | Require one paired `symbol_migration` plus `require_change`, lower through one captured source map, then delegate to the existing compact and generic transaction path | Flat rows only ; standalone require language; private capture protocol; new plan or executor; heuristic migration | The pair states one complete repeated decision without granting discovery or write authority. Identical file sets make source-aware require lowering possible inside the existing one-capture transaction. |
| Canonical effect identity with create files | Suppress identity publication when `create_files` is present and attach an explicit suppression reason (`canonical_effect_identity_suppressed_reason="create-files-present"`) | Continue publishing identities for mixed create requests despite collision risk; treat suppression as implementation detail only | Suppression is visible and prevents hash collisions until a versioned create-aware projection can be made lossless. |

# #Open Questions & Future Decisions

## #Resolved

1. The public request clock excludes model, network, callback, serialization
and background-job time.
2. Typed refusals carry timing and concise summaries just like successes.
3. A retained Prolog oracle and the linked-intent coherence gate belong in
`make runtests`.
4. CLI and MCP exact-selector refusals share one transport-neutral recovery
compiler. Broader operation-receipt convergence remains a sibling segment.

## #Deferred

1. Whether future telemetry should expose named internal phase timings in
addition to the single public request clock.
2. Whether transport-level exceptions should eventually become a separately
specified structured MCP failure envelope.
3. Whether an exact proof relation should emit a hash-guarded executable read
retry after the stateless hypothesis slice has field evidence.
4. Whether inline successful-sibling evidence earns a later server-retained
continuation. The current slice deliberately retains no server state.

# #References

- [clj-surgeon HLD] (../../high-level-design.md)
- [Uniform MCP elapsed-time plan] (../../plans/uniform-mcp-elapsed-time.md)
- [Closed-relation product seam audit] (../../observations/2026-08-29-symbol-migration-require-change-product-seam-audit.md)
- [Correct flat-control causal protocol] (../../observations/2026-08-29-correct-flat-control-and-relation-causal-protocol.md)
- CLI/MCP receipt issue: `clj-surgeon-9xi`
