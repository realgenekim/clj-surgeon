# Mission typist executor — flagged prototype

Owner: Astra. Deadline: 2026-09-06 09:31Z; checkpoints 03:00Z, 06:00Z, 09:00Z.
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
record both attempts and actual provider/model. Spark is a separately selected
route for reliability. Runtime keys come from configured EDN files, never env,
prompts, receipts or model-authored settings. Secret errors are replaced with
typed redacted diagnostics. No automatic unpinned provider selection.

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
