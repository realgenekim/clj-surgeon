---
parent: high-level-design
prefix: MCP-OP-ELAB
status: 'ratified in advance (Gene, 2026-08-30, verbatim: "Go on all!!!"); implementation evidence-gated'
---

# Embedded Spark Elaborator

This is the ratified-in-advance LLD for a bounded embedded Spark elaborator.
Gene first authorized the program with `"Keep going until production ready.
Bonus points for dog fooding!!!!!"` and then ratified the direction in advance
with `"Go on all!!!"`. That approval authorizes this intent chain. It does not
waive the isolation gate, authorize installation, or grant a model write
authority.

Frozen red and product implementation may begin only after the independent
isolation screen passes and the implementation owner is coordinated with the
Mayor. Any isolation failure stops this leaf. Installation remains a separate
Gene decision after receipts.

## Problem

Large replacement bodies make a capable caller spend many output tokens
retranscribing a decision it already made. The measured wall class has
`|replacement| >> |decision|`; median writes often have no such asymmetry and
must not pay model startup or latency. A warm subscription-authenticated Spark
turn can elaborate a small decision into a complete replacement in roughly two
seconds, but a model that can assert subject identity creates the same
wrong-file failure class already observed in the splice-reference stress test.

The product needs a narrow compiler-like seam:

```text
caller-authored identity + guards + old body + short decision
                              |
                              v
                   canonical intent hash
                              |
                  old body + decision only
                              |
                              v
                  one isolated Spark turn
                              |
                              v
                  one untrusted body string
                              |
                              v
          ordinary edit schema/compiler/transaction/verification
```

The elaborator fills a body. It never selects the body, subject, operation,
workspace, file, owner, count, verifier, or effect.

## Evidence and limits

### Feasibility

The immutable feasibility probe at `44a5bac7` ran one dedicated
`codex app-server --listen stdio://` child through ChatGPT subscription auth
with exact `gpt-5.3-codex-spark` pinning. Five of five outputs parsed and were
byte-identical to the expected candidate. Startup through initialization was
90.284 ms. Turn latency was 1.240 / 2.012 / 9.064 seconds min / median / max.
There were no model reroutes or observed tool items.

The same probe found the load-bearing limitation: the installed protocol does
not expose a hard disable-all-built-in-tools switch. Five obedient turns prove
feasibility, not confinement. Production admission therefore waits for the
independent isolation screen.

### Warmth

The immutable warm-executor screen at `9b6c9708` kept one app-server process
and thread alive. Spark completed ten of ten prepared edits exactly, one-shot,
with zero wrong-subject. Its median prepared round trip fell from 6.773 seconds
cold to 2.288 seconds warm, a 66.2% reduction. The process and protocol can be
kept warm; the evidence does not prove a locally resident model or a stable
provider tail.

Production uses one warm process but a fresh ephemeral thread per intent. This
retains startup savings without cross-intent conversational state.

### Identity prohibition

The adversarial splice replication at `b0432c25` is the decisive scope limit.
Sol remained exact, but Spark paired the correct reference and read-back token
with the wrong explicit file. The identity firewall refused before mutation.
Exact recovery did not erase the wrong-subject attempt.

Consequently, Spark is licensed only for prepared-hole body fills. The output
schema cannot represent a file, owner, workspace, selector, count, operation,
verification choice, second edit, or tool action. Until a later hardening
screen earns otherwise, a Spark-class caller is explicitly prohibited from
reference-asserting or identity-bearing calls.

### Economic scope

The 3.5237 ms/byte production emission model is descriptive, not a product
SLO. The first slice uses a conservative mechanical wall classifier: one old
body of at least 1,024 UTF-8 bytes and a decision no larger than 25% of that
body. At the boundary, at least 768 repeated bytes are at stake before model
and protocol overhead. D1 dogfooding must show a complete verified wall win;
the classifier does not make that conclusion true by arithmetic.

Requests outside this class use the ordinary path. There is no automatic
elaboration, child startup, offer text, retry, or latency on median writes.

## HLD options

### Option A — optional elaboration on the existing edit intent (preferred)

The caller submits one ordinary prepared edit whose `to` remains null and adds
one closed `elaborate` object containing only a nonblank bounded `decision`.
The server validates every ordinary authority-bearing field before model
contact, freezes a canonical intent, asks Spark for one replacement body, then
reconstructs a complete ordinary `edit_clojure` request and invokes the
existing compiler and transaction.

Why it might be right:

- one caller action can elaborate and commit;
- it composes directly with the shipped `prepared_request` shape;
- the existing writer, rollback, read-back, and verification remain the only
  effect path;
- the output schema makes identity assertion unrepresentable; and
- absence of `elaborate` leaves the current request path unchanged.

Cost:

- the public edit schema gains one conditional branch;
- a transaction now has a pre-effect network/model phase;
- catalog text grows for all callers unless the capability is installed as a
  separately gated surface; and
- the server must keep model lifecycle, quota, and isolation evidence.

Assumption underneath: the one-call wall reduction exceeds added schema and
warm-turn cost on the selected class without reducing exact completion.

### Option B — separate `elaborate_edit` operation

A new operation accepts the same subject and guard tuple plus decision, returns
or commits one completed edit, and owns its own public schema.

Why it might be right: the model boundary is conspicuous, independently
permissionable, and easy to meter or disable.

Cost: it creates a third public grammar beside prepared requests and ordinary
edits, repeats identity and validation rules, adds another tool-selection
decision, and risks divergence from the canonical writer.

Assumption underneath: operational isolation is worth the permanent schema,
routing, and parity burden. Current evidence does not support that trade.

### Option C — elaborate inside successful inspect projection

An eligible `inspect_clojure` call carries a decision and returns a prepared
request with its replacement already filled.

Why it might be right: it could fuse read, preparation, and elaboration and add
no write-side conditional schema.

Cost: it couples a read to remote generation, changes the success-only
prepared leaf from caller-owned holes to server-filled meaning, makes read
latency depend on model availability, and blurs the already-ratified rule that
an inspect result has `next_action=none` and no write authority.

Assumption underneath: the caller knows the complete decision at inspect time
and the read contract can absorb generation without coercing mutation. This
conflicts with the prepared-request authority boundary.

### Decision

Choose Option A for the first slice. Do not add a dedicated tool and do not
modify inspect success semantics. The implementation is one optional
prepared-hole branch in the existing edit envelope followed by the unchanged
ordinary transaction.

## First-slice public shape

The candidate shape is illustrative; exact schema bytes freeze during red:

```json
{
  "workspace_root": "/canonical/workspace",
  "edits": [{
    "file": "src/example.clj",
    "within": {"form": "named-owner"},
    "from": "(large old form)",
    "to": null,
    "matches": 1
  }],
  "elaborate": {
    "decision": "Make the already selected form stream results in batches."
  }
}
```

The first slice accepts exactly one edit, one named whole-owner scope, one
null `to`, `matches=1`, an old body of at least 1,024 UTF-8 bytes, and a
nonblank decision of at most 512 UTF-8 bytes and at most one quarter of the old
body. It accepts no caller-supplied model, prompt, timeout, schema, tool,
environment, auth, retry, verification, or output instruction.

The conditional null is not ordinary edit authority. Without the complete
closed `elaborate` object, the existing public null refusal is unchanged.

## Pure intent and prompt boundary

The server creates two values before model contact:

1. **Canonical authority intent:** exact operation version, canonical
   workspace root, project-relative file, named owner, old body, match count,
   decision, and captured source hash. Its canonical UTF-8 bytes produce
   `intent_sha256`.
2. **Elaboration input:** old body plus decision only. It contains no workspace,
   path, owner, selector, count, operation, verifier, receipt, or other subject
   identity.

The output schema admits exactly one object with exactly one nonblank string
field, `replacement`. Unknown keys, additional values, several final messages,
tool items, interrupted turns, and output after the byte cap reject the entire
elaboration. The adapter parses this object as data. It never evaluates model
output or treats it as a Clojure program for the adapter itself.

After acceptance, the server replaces only the one null `to` with
`replacement`, removes `elaborate`, and invokes the ordinary public
`edit_clojure` schema, compiler, source capture, owner/from/count checks,
atomic writer, read-back, rollback, and configured verification. The adapter
cannot bypass or weaken any stage.

The initial guard capture is read-only and holds no transaction lock or staged
write while the remote turn runs. The ordinary edit path recaptures after
elaboration. Any intervening target drift therefore refuses before write.

## Process and lifecycle

The first product slice owns one directly supervised stdio child:

- lazy start on the first eligible request;
- exact pinned Codex CLI package and generated app-server schema hashes;
- a mode-0700 service Codex home and mode-0600 managed ChatGPT auth;
- API-key and access-token environment variables removed;
- one empty read-only workspace not containing or parenting the target repo;
- no configured MCP servers, dynamic tools, repository mounts, writable home,
  shell authority, or inherited project environment;
- one in-flight turn and a fresh ephemeral thread per intent;
- exact Spark pin at thread and turn start, provider fallback disabled, and a
  hard reject on any reroute;
- a 10,000 ms turn deadline and 32,768-byte accumulated output ceiling;
- no automatic replay, repair prompt, reflection turn, or retry; and
- bounded shutdown: close stdin, wait 1,000 ms, send `SIGTERM`, wait 1,000 ms,
  then `SIGKILL` the policy-owned child only.

On process loss, protocol desynchronization, timeout, malformed output, auth
failure, model absence, reroute, tool item, quota stop, or isolation evidence
failure, the current request returns an inert typed elaboration refusal with
`source_unchanged=true`. It also states that the ordinary caller-authored edit
path remains available. The server may attempt one supervised restart only on
a later request after bounded backoff. It never replays the failed intent.

## Isolation admission gate

The independent isolation screen is a hard predecessor to frozen red. It must
exercise hostile prompts that request shell, file, MCP, web, dynamic-tool,
process, and iterative-repair actions. Admission requires:

- zero observable tool side effects;
- no caller- or tool-directed network side effect outside the fixed
  authenticated app-server provider connection;
- zero target-repository visibility from process and filesystem evidence;
- interruption and candidate rejection on the first tool item;
- one turn and at most one final candidate;
- source byte identity after every hostile case;
- no auth or secret material in prompt, output, logs, environment receipts, or
  repository artifacts; and
- process-group cleanup after success, timeout, crash, and cancellation.

The packet currently has no immutable isolation-screen receipt. This absence
blocks tests and implementation even though intent is ratified in advance.

## Model pinning and volatility

Availability is a capability check. The child must report managed ChatGPT auth,
the pinned Spark slug in `model/list`, the same slug from `thread/start`, and no
reroute notification. `allowProviderModelFallback` is false. The server never
falls back to Sol, Terra, an API key, another account, or an unpinned model.

If Spark, subscription auth, the exact CLI, or the schema is unavailable, the
elaborator is unavailable and the ordinary edit path remains unchanged. Stable
product behavior is the refusal/degrade contract; model and provider latency
are volatile evidence.

## Quota ownership and rung-8 meter

Every call spends the subscription represented by the child service identity,
not the MCP caller's account. Deployment and receipts state that fact.

The server writes a local append-only, source-free ledger keyed by a non-secret
auth-identity hash. Each row records timestamp, intent hash, model, CLI/schema
hashes, turn ID, input/output token counts, latency, result class,
elaboration hash when present, and pre/post model-specific rate-limit windows.
It never records source, decision, path, owner, workspace, replacement, auth
bytes, email, or token material.

The first slice has a product-owned rolling 24-hour call budget. At 80% it
emits a durable operator alarm. At 90%, or whenever the meter or model-specific
rate-limit evidence is absent or inconsistent, it opens the elaborator circuit
and degrades to the ordinary path. A caller cannot change either threshold or
close the circuit. Service percentage windows are reconciliation evidence,
not exact per-call billing.

## Receipt

Every attempted elaboration returns bounded structured evidence:

- `intent_sha256` over the canonical caller authority intent;
- `elaboration_sha256` over exact accepted replacement UTF-8 bytes, or null;
- `elaborated_by` containing the exact model slug, Codex CLI hash, generated
  schema hash, non-secret auth-identity hash, and isolation-policy version;
- turn count, latency, input/output token counts, result class, and whether a
  tool item or reroute was observed;
- the ordinary transaction operation and receipt hash when mutation ran;
- ordinary verification status and verifier receipt when configured; and
- `source_unchanged`, refusal type, and ordinary-path availability on every
  pre-write failure.

Receipt fields describe evidence; they grant no continuation, retry, prepared
request, selected subject, replacement, or write authority. Source and
replacement bytes never appear in telemetry or failure summaries.

## Dogfooding plan

### D1 — experiment harness on the next eligible real implementation edit

Before install, the next real implementation task in this repository uses the
existing experiment harness for every edit that meets the wall classifier.
The work happens in an isolated worktree against an exact source snapshot. The
implementer authors the file, owner, old-body guard, count, and short decision.
The harness gives Spark only old body plus decision and records one candidate.
No product runtime, shared MCP, or installed artifact is changed by the
harness.

For each eligible edit, retain:

- canonical authority-intent and elaboration hashes;
- exact model/CLI/schema/isolation identity;
- cold/warm state, initialization, first-token, complete-turn, validation,
  ordinary compile, formatter, verifier, and complete verified wall clocks;
- candidate count, tool/reroute events, schema validity, parse validity,
  guard equality, canonical effect identity, and final source hashes;
- emitted bytes/tokens for elaborated intent versus a hand-typed complete
  replacement produced independently for the same decision; and
- whether the ordinary writer and exact verifier accept both to the same
  canonical effect.

D1 passes only if every eligible case has one candidate, zero authority-bearing
output fields, zero tool effects, exact guard preservation, exact verification,
and no slower complete verified wall than its hand-typed comparator. Any wrong
subject/effect, guard mismatch, tool side effect, reroute, receipt gap,
unverified result, automatic retry, secret exposure, or process-cleanup miss
aborts D1 and blocks product implementation or installation. If the next task
has no wall-class edit, record zero eligibility; do not manufacture one.

### D2 — one-week post-install production watch

After a separately approved install, participating seats may explicitly opt in
on real wall-class tasks for seven days. Ordinary calls and ineligible tasks
remain controls. The source-free production ledger reports:

- eligible, offered, invoked, accepted, refused, degraded, and verified counts;
- p50/p95 initialization, turn, validation, transaction, verification, and
  complete verified wall;
- model pin/reroute, process restart, auth, timeout, tool-item, quota, schema,
  parse, guard, rollback, and cleanup result classes;
- caller-emitted decision bytes/tokens versus accepted replacement size;
- ordinary-path fallback completion and latency; and
- circuit state, 24-hour calls/tokens, alarms, and rate-limit snapshots.

The immediate abort tripwire is any wrong subject/effect, model output used as
subject or guard authority, weakened guard, tool side effect, secret leak,
reroute, automatic replay, verification bypass, receipt mismatch, rollback
failure, or policy-owned child that survives cleanup. The server opens the
circuit automatically.
Operational review also disables offering if p95 elaboration exceeds 10 seconds
in two consecutive ten-call windows, verified completion falls below the
ordinary comparator, fallback becomes slower than the ordinary path, or the
80% quota alarm fires without acknowledgement. D2 evidence may justify a
later threshold change; it does not change this contract automatically.

## Falsifier table

| Claim | Defensible opposite | Required witness | Stop law |
|---|---|---|---|
| The child is confined despite no hard tools-disable field. | Prompt obedience is the only boundary. | Independent hostile isolation screen with filesystem/process/network observations and forced tool requests. | Any side effect, target visibility, surviving child, or accepted tool-bearing turn stops the leaf. |
| Spark fills body only. | A convenient schema may let it restate identity. | Output-schema property tests with injected file, owner, root, count, verifier, second edit, and unknown keys. | Any accepted authority-bearing field outside `replacement` is a design failure. Replacement content is never interpreted as subject identity. |
| Correct reference reading transfers to Spark. | Spark can pair a correct token with wrong explicit identity. | Retain `b0432c25` wrong-file attempt as permanent falsifier; prohibit reference-asserting Spark calls. | No Spark reference or identity authority without a later independent hardening screen. |
| One intent creates one candidate. | Repair loops improve completion. | Timeout, malformed JSON, partial output, tool item, EOF, 401, 429, and crash injection with turn accounting. | More than one turn/candidate or any automatic replay stops the request. |
| Failure degrades safely. | Hidden retry or partial candidate is harmless. | Every failure class must return source unchanged, bounded evidence, and ordinary-path availability. | Any mutation, retained partial candidate, or blind retry stops the leaf. |
| Warmth preserves quality. | Thread history causes drift or contamination. | Fresh thread per intent, ten sequential mixed bodies, first/last exactness, and cross-intent secret markers. | Any marker transfer or quality loss blocks admission. |
| Exact model pinning is enforceable. | A configured slug is enough. | Missing Spark, reroute notification, mismatched thread response, fallback enabled, and schema drift. | Any substitution refuses; no fallback model. |
| Wall selection excludes median writes. | An optional feature silently taxes every edit. | Boundary tests at 1,023/1,024 bytes and 25% ratio, calls without `elaborate`, catalog-size and pre-first-call telemetry. | Ineligible calls must not start/contact the child or change ordinary results. |
| Generated body is safe data. | Parse success or a hash grants correctness. | Unknown keys, tagged values, malformed reader forms, oversized output, ordinary compiler/refusal/rollback tests. | Adapter never evaluates output; ordinary validation remains mandatory. |
| Quota is attributable and bounded. | Shared subscription spend is invisible. | Missing/stale meter, 80/90% thresholds, concurrent callers, ledger failure, and restart recovery. | Missing meter opens the circuit; callers cannot override it. |
| Receipts bind the actual effect. | Model and transaction evidence can be spliced. | Intent, elaboration, transaction, read-back, and verifier hash mismatch tests. | Any mismatch is unverified and cannot claim success. |
| The feature wins complete task time. | Fast elaboration adds a phase without removing enough emission. | D1 hand-typed comparator and D2 ordinary controls in the discovered-or-supplied stratum. | No promotion claim without complete verified wall win on the wall class. |

## Implementation boundary

After isolation admission and frozen red, the smallest implementation owns:

1. one pure intent validator/canonicalizer that separates authority intent from
   the identity-free model input;
2. one supervised app-server adapter with exact lifecycle, pin, timeout,
   output, isolation, and quota policies;
3. one narrow optional branch at the existing public edit envelope that fills
   one null `to`, removes `elaborate`, and invokes the ordinary edit path; and
4. one source-free receipt/ledger projector.

The likely first-slice ownership is one new pure intent namespace, one new
supervised adapter namespace, and one new receipt/ledger namespace. Narrow
integration occurs in `clj-surgeon.mcp-schema` for the conditional public
shape and `clj-surgeon.mcp-tool` at the existing `edit_clojure`
`execute-request!` boundary. `clj-surgeon.mcp-process` may supply existing
bounded process primitives, but it does not acquire model policy. No change is
planned for `intent-transaction`, the formatter, the verifier, the writer, or
the prepared-request projector.

Before implementation annotations land, `clj-surgeon.mcp-intent-contract`
must include this spec registry in its closed repository audit. Red witnesses
then make every active ID known before production code can claim it. The leaf
must not rely on a spec file that the ordinary intent gate does not enumerate.

It does not add a transaction engine, writer, verifier, plan, basis, reference,
semantic provider, CLI executor policy, or inspect-time generator. The
prepared-request projector remains pure and non-executable. Write-refusal
evidence remains inert and never carries an elaboration request.

## Sibling scope and compatibility

The shipped `prepared_request` descriptor does not gain `elaborate`, a filled
body, or an execution cue. Its ratified null-hole, byte-budget, omission, and
`next_action=none` laws remain unchanged. After making an independent decision,
a caller may copy one eligible descriptor's explicit arguments, leave its one
`to` hole null, and add the closed `elaborate` object. This is a new edit-input
branch, not inherited read authority. The existing rule that fully filled
prepared arguments validate as an ordinary edit remains true.

The write-refusal leaf remains source-free and inert. It may not publish an
elaboration decision, replacement, prepared request, selected candidate, or
retry. CLI behavior is unchanged in the first slice. The feature is an MCP
`edit_clojure` extension and does not generalize CLI process policy.

## Intent-narrowing edge audit

| Edge | Narrow reading | Rejected widening |
|---|---|---|
| No `elaborate` field | Exact current `edit_clojure` validation and execution. | Start or warm the child speculatively. |
| `elaborate` with a non-null `to` | Refuse the mixed authority shape before model contact. | Treat the literal as a hint, fallback, or second candidate. |
| Null `to` without `elaborate` | Exact current null refusal. | Infer that every prepared hole requests generation. |
| Decision or body exactly at a limit | Count canonical UTF-8 bytes and admit only the closed boundary. | Count characters/tokens approximately or truncate. |
| Target changes during the turn | Fresh ordinary capture refuses before write. | Hold a long transaction lock, reuse stale capture, or auto-regenerate. |
| Replacement text contains a path, symbol, or owner-like string | Treat it only as replacement bytes subject to ordinary validation. | Interpret content as target, guard, operation, or receipt identity. |
| Candidate parses but changes unintended semantics | Preserve ordinary caller responsibility and verification; make no semantic-success claim. | Treat parse success, exact model pin, or elaboration hash as correctness. |
| Several MCP callers arrive | One-in-flight elaboration; other callers receive bounded defer/refusal or use ordinary edits. | Pool intents, share threads, or reorder guards/candidates. |
| Child or meter state is unknown | Circuit open; source unchanged; ordinary path remains available. | Assume healthy, retry blind, or charge another account. |
| Prepared or refusal sibling result | Preserve sibling bytes and authority exactly. | Insert an offer, filled body, or retry into those result surfaces. |

## Non-goals

- No general coding agent, loop, reflection, repair turn, or tool use.
- No Spark-authored identity, selector, guard, verifier, operation, or effect.
- No reference-asserting Spark call.
- No arbitrary prompt, model, command, environment, timeout, or output schema
  supplied by the caller.
- No model fallback, API-key fallback, shared daemon, remote listener, or
  network-auth material in MCP.
- No eval of generated Clojure and no alternate writer.
- No automatic offering to median writes and no latency on absent requests.
- No success claim from parse, model prose, or candidate hash alone.
- No installation or shared-runtime change without a later Gene gate.

## Verification sequence after isolation admission

1. Freeze exact isolation receipt, CLI/schema hashes, first-slice schema, meter
   constants, and product base.
2. Write red witnesses for every `MCP-OP-ELAB-*` requirement and falsifier.
3. Implement the pure validator and identity-free output schema before the
   process adapter.
4. Implement fake-process lifecycle, timeout, quota, output, and cleanup
   witnesses before one bounded real-child contract batch.
5. Integrate only by reconstructing and invoking the ordinary edit request.
6. Run D1 on the next eligible real implementation edit and compare with the
   hand-typed equivalent.
7. Run affected tests and a milestone full suite; preserve load-deferred gates
   as unverified rather than guessing.
8. Return implementation, D1, full-gate, and install plan receipts to Gene.
