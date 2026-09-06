# Mission typist executor — flagged prototype

Owner: Astra. Extended deadline: 2026-09-06 15:31Z (Gene, recorded 03:33Z).
Completed checkpoint 03:00Z; remaining checkpoints 06:00Z, 09:00Z, 12:00Z, 15:00Z.
Base: bridge/mission-ledger f2efc87c. Gene authorized the seven-piece experiment;
Fable owns the real-repository mission, narrow gate and independent witness.

## Observable contract

An explicit typist executor request is decided during planning and persisted with
the mission's frozen dossier and proof authority. Ordinary missions retain their
current executor. A typist sees source fragments, owner identities, exact allowed
spans and the intended change, never provider keys or authority to expand scope.
The route is refused before network or write when any admission fact is missing.

Eligibility requires complete discovery; a mechanical mission class; a measured
gate below 5000 ms with named receipt evidence; a distinct acceptance witness;
bounded named files/spans and change budget; supported plain Clojure source; and
a guarded commit/rollback authority. Generated, reader-conditional and declared
format-sensitive inputs refuse. Missing proof or rate evidence is not zero cost
or a guessed success rate. Rates bind to a mission class and a pinned provider.
Candidate count: >=85% verified gives 1, <=70% gives 5, otherwise 3.

Primary provider is OpenRouter openai/gpt-oss-120b pinned to Cerebras with upstream
fallback disabled. An explicit bounded typed provider refusal may activate Groq;
record both attempts and actual provider/model. Spark remains available to pure
policy experiments, but this executor has no Spark transport. Executor planning
must refuse it as `:typist-executor-provider-unavailable` before reporting a ready
mission or freezing transport authority. Runtime keys come from configured EDN files, never env,
prompts, receipts or model-authored settings. Secret errors are replaced with
typed redacted diagnostics. No automatic unpinned provider selection.

Executor capability regression (2026-09-06): a fully eligible Spark request
previously produced `:ok true` and a saved `:ready` mission, then failed in
`request-one!`. The executor planner now checks its implemented provider set after
pure dossier admission and before transport capture. Pure policy still admits
Spark for experiments; OpenRouter/Cerebras and direct Groq plans remain admitted.
The named `spark-policy-does-not-admit-unimplemented-executor` witness reproduced
five failed assertions before the fix, including the incorrect public ready state
and two premature transport-authority calls. Afterward the combined policy,
executor and admission suites pass 17 tests / 264 assertions, with no provider
calls. New admission namespace: `:battery`, two deftests / nine assertions.

## Seven implementation checkpoints

1. Pure admission and frozen dossier projection from planner-owned spans; literal
   tests for every refusal and rate boundary. Receipt names the failed condition.
2. Flagged executor inside mission apply; route/k/provider/proof carried from plan.
   First fake-candidate end-to-end fixture goes through this path before paid calls.
3. Pure owner-keyed replacement forms, then existing guarded transaction
   machinery. Frozen planner spans supply the preimage; the model never emits old
   whitespace. Format only owned replacements, preserving all other bytes.
   Model output edits scratch candidates only; live source is written
   only after independent proof and stale-source recheck. Reject duplicate blocks,
   ambiguous anchors, escapes, overlap, budget breach and unauthorized changes.
4. Fable's real-repository gate, including measured cost and command authority.
5. Independent acceptance outside candidate write authority; behavioral checks plus
   byte/mode identity outside the allowed spans/files. Corrupt witnesses must fail.
6. Runtime key loading and bounded pinned provider transport. Full retained usage,
   reasoning-token count, typed refusal and terminal candidate state; secrets absent.
7. Real-repository and resident-session A/B, fence review, repository landing gates.
   No self-landing, no production-ready claim before these pass.

Each checkpoint is independently committed with its verification command. Scoped
kernel/API changes follow the repository's linked-intent workflow when reached.
Early pure modules and prototype CLI seams are outside that scoped path set.

## Initial behavior matrix and ratchets

Admission: disabled flag; unknown class; incomplete/empty dossier; missing owners;
invalid path/span; unknown source flags; generated/CLJC/format-sensitive source;
missing, negative or slow gate cost; missing/shared acceptance authority; absent
atomicity; invalid counts or class/provider mismatch; rate boundaries 70/85;
missing scope budget; valid route and frozen evidence.

Candidate compilation: unique anchor success; missing/ambiguous anchor; duplicate
file block (field regression: prototype silently discards earlier block); alias
paths; same-file disjoint hunks; overlap; blank/empty change; added/removed files
only if planned; stale snapshot; budget; parse failure; unchanged protected bytes.

Executor: no network on refusal; fake good/bad candidates; all rejected; gate pass
but independent acceptance fail; provider length/refusal/wrong upstream/model;
redacted transport errors; fallback counted; apply failure and rollback; interrupted
apply/resume; guarded undo; receipts that describe what actually happened.

## Experiments and stopping rule

Preregister prediction, falsifier, exact prompts/fixture/proof and lifecycle before
each A/B. Reserve a quiet window, use slot -t, sample load, retain all failures.
Compare whole task with the same independent acceptance. Include deterministic
Surgeon when its planner already computes the entire edit: adding a typist there
may lose, and that loss is informative. Separate a supplied-dossier author test
from problem-to-done timing, and cold from resident-session results.

First prediction: a known complete one-file fixture can reach a verified mission
through the flagged executor; performance is unknown until startup, dossier,
provider, candidate proof and commit are all charged. A real JVM proof above the
route's budget refuses admission rather than becoming a hidden slow exception.
If the executor cannot beat native, keep reusable dossier/proof primitives and
report the lost route. Do not scale candidate count to conceal a bad experiment.

## Real-1 finding: representation revision

Fable 4b277bfc reports a preregistered real-repository loss: Cerebras 0/20
candidates applied, versus cold Sol 3/4 at median 29.68 s. These are Fable
measurements, not a new Astra run. Model-emitted old context failed whitespace
matching. The model interface is now `{:file :owner :form}`; `:new-owner` is
planner-owned rename authority. Whole-form syntax must parse without evaluation;
comments, metadata and reader-discard syntax refuse until preservation is proven.
Literal anchors remain a private lowering seam, never a demand made of the model.
Astra owns forms; Fable owns the whole-file comparator. Neither has established
a gain. Formatting, independent proof and guarded commit are still charged.

## Optional one-process run entrance

`bin/mission run --spec-file owner-forms.edn --state-home H` explicitly authorizes
planning and immediate application of an `owner_forms` mission in one JVM.
The same request may still use `propose` for review followed by `apply`.
`run` never accepts an existing mission id or another mission verb. It calls
`propose!` once, lets that public path persist its frozen plan, then calls
`apply!` once by the new id and recorded root only when the saved proposal is
ready. No fresh request, profiles or spec are forwarded to apply. A blocked
proposal stays recorded; run returns a typed nonzero refusal with that mission
id and decision. Other admission or apply failures retain their existing shape.

This removes one cold process start, not proof or stale-source checks. It is a
mechanism prediction with no speedup claim. A caller must choose `propose` when
it needs an intervening review; run does not imply a preview-only action.

Witness matrix: accepted owner_forms; unsupported/missing verb; existing id;
blocked/error proposal with zero applies; terminal apply refusal unchanged;
one saved plan/one execution; persisted authority rather than supplied profiles;
CLI parsing, global/per-verb help, launcher dispatch, EDN and nonzero refusal;
filesystem refusal preserves source. Fake executor tests must make no network
calls. Existing mission/helper behavior remains covered by its legacy suite.

## Function-emitted mission events (Astra, 02:10Z)

Each public propose/apply/undo boundary emits one bounded completion event to the existing telemetry-events writer and its ~/.clj-surgeon/events.jsonl destination. Kinds are mission-plan, mission-apply and mission-undo; mission id, terminal state, admitted fixed route scalars and observed wall are projected without raw requests, source, provider replies or credentials. Shared line-map validates optional scalar fields as well as the caller projection. Refusals emit their bounded typed reason; exceptions emit a generic mission-exception and are rethrown unchanged. Writer failures cannot turn a successful mission into a failure.

These boundary events do not invent proof or commit stages from a terminal state. Actual verification/commit emission requires a later executor hook at the work site. Tests must witness a real call producing its matching event, refusal leaving workspace unchanged, allowlisted fields, bounded output, and isolated logging failure. Source edits enter the executor first; protected-comment refusal is retained before native fallback.

## Raw Clojure candidate experiment

A caller may select `:typist {:candidate-format :clojure-forms}` for exactly one
target file. Omission retains `:owner-forms` JSON; other formats and multi-file raw
requests refuse during planning. The format is frozen in route and dossier, and
apply dispatches solely from saved authority. The provider emits plain complete
named definitions, no string envelope. The decoder maps each expected emitted
name (new-owner or original owner) to exactly one frozen original owner and file.
It must cover the owner set exactly and lowers literal parsed source to the
existing compiler. No evaluation, unescaping, prose/fence stripping or scope
inference. Existing formatting, independent gate/witness, protected syntax,
byte/mode guards and commit/undo remain mandatory.

Field motivation: T4 in the four-pair JSON cohort emitted double-escaped newline
characters and was correctly refused before write. This new protocol does not
reclassify or repair that failed sample. It requires a separately recorded
hand-drive before another replicated cohort. Any transferred routing prior must
name its source representation; it is not raw-format reliability evidence.

## Candidate refusal survival (Astra, paper cuts round)

Every rejected compilation retains its typed diagnostic in the candidate receipt. The public response projects only error-type, error, condition, lost, moved and next_call, capped at 4096 printed characters per candidate. If oversized, it returns the type and explicit truncation plus a path to the complete diagnostic artifact; it never presents a truncated next call as runnable. Full compilation refusal data is retained locally in the mission artifact directory. Successful compilations do not copy staged source into receipts. Proof failures retain their existing independent gate/witness receipt. Witnesses: faithful lost-comment diagnostic survives all-candidates-rejected without write; oversized diagnostic points to complete artifact; valid later candidate retains earlier refusal. Transport failures remain typed separately; no raw provider message is promoted into these fields.

## Paper cut: readable mission receipt and supported recovery

`mission show ID` projects the saved ledger row into bounded EDN (at most 4096
UTF-8 bytes as pretty-printed), retaining actual state, receipt success/failure,
route and bounded candidate refusals including lost content and next_call.
It never implies that reading the receipt reran proof. Omitted candidate counts
and truncation are explicit; `show ID --full` preserves the previous full view.
Both BB and JVM entrances use one projection. Missing/corrupt show is nonzero;
a successful read of a failed mission is still a successful read.

Mission CLI refusals receive executable examples for supported inspection or
help recovery, with real workspace/state-home arguments and safe shell quoting.
A stale/failed mutation is never blindly replayed by an example. This concerns
the mission CLI only; the reported Surgeon core receipt-out/source-string errors
remain outside this change. No `mission commit` capability belongs to this cut.

Missing-workspace field regression: `show M-1 --state-home H` must return
`:mission-workspace-required`, exit 1, and executable help rather than a nil
workspace stacktrace. Shared BB/JVM show projection owns the same refusal;
read-only BB list/ready/blocked use this guard too. Storage home never implies
a workspace. Preserve the exact field invocation as a subprocess witness.
### Explicit native fallback report

`bin/mission fallback M-ID --reason TYPE --workspace R` records the caller's
report that they selected native tools. Admitted reasons are `refusal`,
`unsupported`, `slower-than-native`, and `user-choice`. This command neither
performs nor verifies a native edit, and cannot change saved mission state,
proof, history, or source. It is not provider fallback or verified adoption.
A successful append returns `:recorded true` and the actual bounded event;
a failed append returns `:recorded false`, `:ok false`, and exit 1. Missing
missions and unsupported reasons append nothing. JSONL admits only fixed
fallback enums. Tests cover both writer failure modes, all reasons, schema
rejection, unchanged saved bytes, and the real CLI with an isolated ledger.


### Historical decision presentation

Historical blocked owner_forms rows can contain a nil top-level decision
error and a typed nested evidence refusal, beside an incompatible saved
helper_extraction example. Default display promotes the saved nested code
only when the direct code is absent, labels that source, and omits a saved
example whose declared verb conflicts with the mission verb. An explicit
omission reason and runnable verb-appropriate help replace that example.
This is presentation only: saved bytes and --full remain unchanged. Witnesses
use the actual historical forms-protected-syntax row and the BB CLI reader.

### Event-only fallback runtime

`mission fallback` appends a user report and needs no source executor or JVM
planner. A small shared handler keeps BB and JVM validation, JSONL schema,
append-success truth, response text and unchanged-state semantics identical.
The public launcher routes this verb to BB. Shared telemetry must load under BB;
POSIX permission tightening retains only existing owner bits, without granting
new ones. Verification compares BB/JVM receipts excluding process/time fields,
blocks any Clojure executable in the real CLI witness, and checks BB privacy and
append failures. Any single observed command wall is an unreplicated snapshot,
not a speedup claim. Source and saved mission bytes remain unchanged.

BB fallback verification: combined fallback, shared telemetry and display gate
passes 33 tests / 268 assertions. Three new fallback witnesses cover runtime
parity, a poisoned Clojure executable proving public launch uses BB, and POSIX
owner-write preservation on a read-only parent. Existing append-failure and
unchanged mission/source tests now exercise the BB launcher. The schema is
unchanged; the only shared writer change replaces unsupported EnumSet calls
with filtering existing permission bits. An actual isolated command receipt
and single unreplicated wall snapshot are retained at
`/var/tmp/forge/mission-fallback-bb-dogfood-fx`; no comparative claim follows.

### Caller-facing schema and help

The inherited-context caller-surface review found missing nested typist fields,
undiscoverable propose help, and incomplete publication inspection guidance.
Publish the complete caller contract and a parseable template with unknown facts
left nil (never fabricated calibration counts, source-policy facts or proof
measurements). The template is intentionally not admissible until completed from
retained evidence. Propose previews frozen intent/authority, not a generated
candidate diff; generation/proof/write happen in apply. Dedicated propose and
undo help link that user contract and disclose source-versus-ledger writes,
publication refusals and read-only Git inspection. This is documentation/help
repair, not a fresh blind adoption result, new capability or performance claim.

### Compact proposal presentation

A real count-name proposal printed 60,118 bytes of repeated saved authority.
The CLI-only default for an id-bearing `propose` will return the existing bounded
`mission-show` view of that saved mission, with its state, effective next action
and workspace/state-home-bound full-details command. `propose --full` retains
the previous complete result. Internal propose!, run!, saved bytes and exit
classification stay unchanged. This is an authority display, not candidate-diff
approval or proof. Refusals without a saved id keep their existing response.

### Proposal presentation failure exit

A saved proposal and its later compact display are separate outcomes. If the
display read returns a typed refusal, stdout must not report that failure with
exit zero. Classify both the original operation and rendered response; preserve
the saved ledger and --full bypass. A subprocess witness injects failure only
at show after real proposal persistence. No execution/provider behavior changes.

### Explicit artifact destination

Owner-forms execution must receive an explicit nonblank receipt directory; it
may not silently retain frozen source in a home-directory default. CLI apply
(and run through apply) refuses before :applied, leaving the ready mission
unchanged and returning exact retry argv bound to id/workspace/state-home. The
executor independently rejects missing destinations before artifact or provider
calls. Explicit destinations keep their existing semantics; helper_extraction
is unchanged. Refusal does not create a suggested directory. The closed event
normalizer admits only the new typist-receipt-dir-required code and the separately
observed mission-snapshot-stale spelling; no arbitrary-code passthrough.
