# Caller #1 dream list: lawful fulfillment designs

Date: 2026-08-30

Status: design ideation only; no mechanism below is product intent, no experiment
was run, and no performance claim is made

Branch: `docs/dreamlist-designs-20260830`

Gene, verbatim: "summon brain fleet on clever ways we can fulfill wish without
violating rules/constraints/etc... Or still achieve goals with new
rules/constraints!"

## Executive answer

All eight wishes have a useful lawful form. Four distinctions keep the useful
forms from becoming authority bugs:

1. A hash, label, campaign head, or ordinal may point to complete identity that
   was already proved. It may not replace that identity. The server must resolve
   the pointer, revalidate the complete identity, and echo it in the receipt.
2. A read-side elaborated replacement and its preview are candidate data, not a
   caller decision. The ordinary `to` hole stays null until the caller promotes
   the candidate. In the separately ratified write-side elaborator, the caller's
   complete guarded edit plus `elaborate.decision` is itself the authorization.
3. Disposable caches may improve latency. They may never be required for
   correctness, freshness, or authority. Durable memory is an append-only event
   chain, not mutable server session state.
4. An explicitly asked read may answer an availability question. Default and
   ineligible ordinary results still remain byte-identical and cue-free.

The legal end-to-end composition is:

```text
explicit read / explicit capability query
  -> complete inert descriptor with full identity and null caller holes
  -> optional identity-free elaborator candidate sidecar
  -> candidate-bound diff/post-source and honestly typed verification evidence
  -> caller independently promotes/fills the candidate
  -> hash/label lookup expands the exact full descriptor
  -> ordinary compiler recaptures source and revalidates every identity leg
  -> atomic commit + full resolved-identity receipt + next_action=none
  -> optional caller-owned daily chronicle fold
```

W1 has a no-amendment client implementation and a better server implementation
that needs a narrow retained-descriptor rule. W2 is exact only after replacement
bytes exist. Literal W3 requires a new read-side candidate leaf because the
ratified elaborator deliberately forbids inspect-time generation. W4 should use
append-only campaign heads, not mutable sessions. W5 can number everything for
presentation today; direct ordinal write selection needs a narrow positional-
authority exception. W6 should begin as warm-cache-only. W7 should be an offline
digest-fenced fold over receipts and substantiation reports. W8 is an explicit
ask on an ordinary read whose ineligible result is the unchanged ordinary result.

## Binding-law interpretation

These designs preserve the ratified laws as follows:

| Law | Design consequence |
|---|---|
| No-cue | No unsolicited field, text, timing dependency, or omission notice appears on default/ineligible ordinary results. Explicit asks remain caller-selected read input; ineligible output is still ordinary. |
| Prepared things are inert | Every descriptor, candidate, preview, choice set, prefetch, and campaign seed says `executable=false`, `write_authority=false`, and retains `next_action=none`. No refusal contains an executable continuation. |
| Exact identity | Server-generated labels are complete/all-or-none and snapshot-fenced. Resolution has no numeric tolerance, fuzzy match, nearest member, or silent rebase. Receipts expand full identity and owner-token proof. Spark never supplies identity. |
| No model-code eval | Elaborator output is one data field, validated as untrusted replacement bytes through the ordinary compiler. The caller authors guards. |
| Stateless retry fences | Caller-carried `snapshot_guards` remain the freshness fence. Durable convenience state is append-only and content-addressed; TTL state is a disposable projection only. |
| Selective offering | Median writes pay nothing. Model contact and large emission exist only on an explicit wall-class request. |
| Compress repetition, never identity | Short references consume previously proved identity. They do not weaken or truncate it. Turn deletion outranks byte golf. |
| Rule 5 price accounting | Screens report caller output, server output/caller input, and complete wall separately. They never convert cheap input bytes into an output-token claim. |
| Claims discipline | Screens can prove mechanisms and kill designs. They cannot mint product speed, adoption, or causal savings claims. |

## Ranking method

`Caller value` and `feasibility` are preregistration priorities, each on a 1–5
ordinal scale. The product is only a screen-ordering aid; it is not evidence.
Any safety kill overrides the score.

| Overall rank | Wish | Caller value | Feasibility | Product | Why it sits here |
|---:|---|---:|---:|---:|---|
| 1 | W1 confirm-by-hash | 5 | 4 | 20 | Client rehydration can fulfill the caller experience now; server CAS is a contained next leaf. |
| 2 | W8 asked-for discoverability | 4 | 5 | 20 | An explicit forms-read ask is cheap, pure, and preserves ineligible byte parity. |
| 3 | W2 dry-run preview | 5 | 4 | 20 | Exact candidate preview reuses the ordinary compiler; only predictive verification is rejected. |
| 4 | W4 campaign memory | 4 | 4 | 16 | An append-only head is feasible, but mechanical replay covers less than semantic “same transformation.” |
| 5 | W3 intent-specialized forms | 5 | 3 | 15 | High value and one existing elaborator path, but literal read-side candidate generation needs ratification. |
| 6 | W7 receipt auto-chronicle | 3 | 4 | 12 | Easy after telemetry, honest, and useful; it does not directly shorten the edit path. |
| 7 | W6 speculative prefetch | 3 | 4 | 12 | Warm-cache-only is safe, but next-read predictability has not yet been earned. |
| 8 | W5 universal ordinals | 3 | 3 | 9 | Presentation is easy; a bare digit cannot safely or uniquely carry universal write authority. |

Ties are broken by dependency leverage and the chance to delete a caller turn.

## W1 — confirm a served descriptor by SHA plus holes

### Design A — client-side canonical rehydration (ship/screen first)

The server keeps the ratified complete descriptor. The MCP client caches its
canonical bytes by full SHA-256. The caller emits only the digest and hole
values to trusted client middleware; the middleware verifies the digest, fills
only declared holes, and sends the full ordinary `edit_clojure` arguments.

```json
READ -> {
  "prepared_request": {
    "executable": false,
    "write_authority": false,
    "arguments": {"workspace_root":"...","edits":[{"file":"...","within":{"form":"render"},"from":"...","to":null,"matches":1}]},
    "caller_holes": ["arguments.edits[0].to"]
  },
  "next_action": "none"
}

CLIENT CACHE -> {"<locally computed full canonical SHA-256>":"<exact served descriptor bytes>"}
CALLER -> {"confirm":"<digest>","holes":{"arguments.edits[0].to":"(new-form)"}}
CLIENT -> ordinary edit_clojure with the complete rehydrated arguments
```

**State and authority.** The caller/client owns the exact cache and computes the
already-specified canonical descriptor SHA locally; no new public descriptor
field is required. The server retains no descriptor state and sees the same
explicit request it accepts today. A local miss or digest mismatch produces no
write call. The server then recaptures current source and refuses file, owner,
old-source, count, or schema drift through the ordinary path.

This fulfills the paid-emission experience without a law change. It does not
reduce the server transport request, and the screen must say so. A full SHA-256
is 32 raw bytes or 43 base64url characters before holes and envelope; a literal
total of about 30 bytes is impossible without truncation or a shorter server
label.

### Design B — server-side content-addressed confirmation (preferred end state)

```json
WRITE -> {"prepared_sha256":"<full digest>","fill":{"H1":"(new-form)"}}
```

The server stores the canonical served descriptor as one immutable CAS object,
recomputes the digest on lookup, fills only its typed holes, expands it into the
ordinary public request, and invokes the sole existing compiler/transaction.
The object is bound to workspace, schema, complete snapshot identity, owner-
token proof, and emission evidence. An append-only segment is authoritative; a
bounded index is only acceleration.

Unknown, expired, tampered, cross-workspace, wrong-schema, wrong-kind,
duplicate-hole, or omitted-hole lookups refuse before write. A miss is never
reconstructed by scanning the repository. Current target drift still refuses
through the ordinary transaction. The success receipt includes the submitted
digest plus full resolved file, owner, operation, before/result hashes,
owner-token proof, canonical effect identity, verification, and
`next_action=none`.

**Required new rule — `MCP-OP-PREP-CONFIRM`.** A full digest may consume exactly
one previously emitted eligible descriptor whose complete identity is retained
immutably by the server. It may not select, change, widen, omit, truncate, or
rebind a target. All labels and holes are server-generated, complete, typed,
and all-or-none. Caller values may fill only declared decision holes. Expansion
must enter the ordinary public compiler and receipt path.

**Justification.** This compresses repetition, not identity. The full identity
exists in the immutable object, participates in ordinary validation, and is
legible in the receipt.

**Blast radius.** Prepared-result schema, one confirm input branch, descriptor
CAS/retention/restart policy, telemetry stages, and receipt projection. It does
not change ordinary edits, ineligible reads, refusals, the writer, or rollback.

**Falsifier.** Any truncated digest, collision ambiguity, cache-miss rebound,
mutable descriptor update, cross-root replay, field override, guard bypass,
second executor, or digest-only receipt kills the rule.

**Cheapest preregistered screen and kill.** Zero-model replay of the retained
one- and six-form fixtures. Compare rehydrated/CAS-expanded arguments, compiled
candidate, future bytes, and canonical effect identity with the original
explicit request. Inject stale, tampered, reordered, cross-workspace, expired,
wrong-kind, unknown, duplicate-hole, and coherent-wrong-valid-descriptor cases.
Kill on less than 30% frozen-token reduction in caller-emitted arguments, less
than 100% effect parity, any guard bypass, any accepted digest truncation, or
any executable lookup refusal.

**Rank:** caller value 5 × feasibility 4 = **20**, overall **#1**.

## W2 — dry-run preview on prepared candidates

An exact diff, post-source, or result hash cannot exist while `to` is null. The
honest object is therefore a preview bound to concrete candidate bytes, whether
caller-supplied or W3-generated.

### Design A — inert candidate sidecar plus pure compilation (selected)

```json
PREVIEW INPUT (caller-supplied candidate) -> {
  "prepared_request":"<complete inert served descriptor>",
  "candidate_fill":{"path":"arguments.edits[0].to","replacement":"(new-form)"}
}

PREVIEW INPUT (W3 candidate) -> {
  "prepared_request":"<same complete inert descriptor>",
  "candidate_sidecar":{"intent_sha256":"...","elaboration_sha256":"...","replacement":"(new-form)"}
}

{
  "prepared_request": {
    "executable": false,
    "write_authority": false,
    "arguments": {"edits":[{"file":"src/demo.clj","within":{"form":"render"},"from":"...","to":null,"matches":1}]},
    "caller_holes": ["arguments.edits[0].to"]
  },
  "candidate_preview": {
    "status": "candidate-not-decision",
    "provenance": "caller-fill-or-elaborator-sidecar",
    "prepared_descriptor_sha256": "...",
    "replacement_sha256": "...",
    "basis_file_sha256": "...",
    "result_sha256": "...",
    "diff": "...",
    "post_source": "...",
    "validated": {"ordinary_compiler":"passed","whole_file_parse":"passed"},
    "verification_forecast": {"applicable_profiles":["affected-clojure"],"selected":null,"status":"not_run","claim":"plan-only"},
    "executable": false,
    "write_authority": false
  },
  "next_action": "none"
}
```

The server computes `replacement_sha256` over the exact candidate bytes from
one of the two input lanes and binds them to the complete prepared descriptor.
It then runs the same pure candidate builder and formatter used by the ordinary
path against the read snapshot. It reports parse/compiler/formatter facts only
when actually observed. A verifier that did not run is `not_run`; profile
applicability is not profile selection and never predicts a pass.

**State and staleness.** The response/caller holds the preview. A malformed,
unchanged, ambiguous, non-parsing, or over-budget candidate yields no attachment;
the saved ordinary result is restored byte-for-byte. Apply always recompiles
and recaptures current source. Any candidate, environment, or target drift
invalidates prior evidence and refuses.

Server result bytes are caller input, so a complete diff can be cheap relative
to caller output, but it is not free. The attachment has an all-or-none emission
gate; it never truncates identity or a diff and still calls it exact.

### Design B — symbolic hole-aware forecast

```json
"preview_contract": {
  "status":"awaiting-caller-hole",
  "executable":false,
  "write_authority":false,
  "changed_files_if_valid":1,
  "guarded_owner":"render",
  "verification_profiles_applicable":["affected-clojure"],
  "diff":null,
  "post_source":null,
  "result_sha256":null
}
```

This is lawful without candidate bytes, but only describes the mechanics that
will run. It does not fulfill the exact-preview half of the wish.

If the desired product requires actual precommit tests, add a separately
ratified **read-only candidate verification** operation. It runs only a closed,
project-owned verifier against an immutable overlay and reports observed
results bound to candidate, toolchain, dependency, environment, and snapshot
hashes with `source_unchanged=true`. Calling this a read-price forecast is
forbidden.

**Amendment for actual verification.** Permit closed verifier execution on a
candidate overlay, never predictive success. Blast radius includes wall time,
process isolation, overlay filesystem, environmental identity, cancellation,
and output budgets. Falsify it with any host/source mutation, unexecuted pass
claim, server-selected verifier, unbound environment, or later commit treating
stale verification as current.

**Cheapest preregistered screen and kill.** Zero-model replay of existing
preview/apply fixtures. Compare diff, complete post-source, formatting,
comments, metadata, reader conditionals, line endings, result hashes, and
canonical effect identity with the ordinary dry run. Kill on one mismatched
byte, a field override, an otherwise unnecessary preview turn, ordinary-result
drift, a claimed pass that did not execute, or a stale apply that commits.

**Rank:** caller value 5 × feasibility 4 = **20**, overall **#3**.

## W3 — intent-specialized forms with candidate-prefilled holes

Literal W3 is blocked by the current leaves. The prepared-request contract
requires caller-owned null `to` holes and forbids inferred replacements or
state. The embedded-elaborator packet at `eaba46b2` deliberately selects a
write-side branch, rejects inspect-side generation, and says the shipped
prepared descriptor gains neither `elaborate` nor a filled body.

### Design A — explicit read-side candidate sidecar (preferred amendment)

Reuse the exact `eaba46b2` supervisor, isolation policy, canonical intent,
model pin, identity firewall, one-turn/one-candidate schema, quota ledger, and
ordinary validator. Do not build another elaborator.

```json
REQUEST -> {
  "requests":[{"operation":"forms","file":"src/demo.clj","forms":["render"]}],
  "candidate_elaboration":{"decision":"Stream the selected body in batches."}
}

RESULT -> {
  "prepared_request": {
    "executable":false,
    "write_authority":false,
    "arguments":{"workspace_root":"...","edits":[{"file":"src/demo.clj","within":{"form":"render"},"from":"...","to":null,"matches":1}]},
    "caller_holes":["arguments.edits[0].to"]
  },
  "candidate_elaboration":{
    "hole":"arguments.edits[0].to",
    "candidate":{"status":"untrusted-candidate-not-decision","replacement":"...","elaboration_sha256":"..."},
    "executable":false,
    "write_authority":false
  },
  "candidate_evidence":{"intent_sha256":"...","model":"<exact pinned Spark slug>","turn_count":1,"tool_items":0,"rerouted":false},
  "next_action":"none"
}
```

The caller supplies the exact subject selector, guards, and one-line decision.
Spark receives only the selected old body and decision. Its closed output is
exactly one replacement string as data. It never receives or emits workspace,
path, owner, selector, count, operation, verifier, receipt, or write authority.

The actual ordinary `to` remains null. Putting candidate text directly in
`arguments.edits[0].to` would create a schema-valid executable object and turn
the server candidate into an apparent caller decision. The sidecar is the
critical representation rule. W2 may preview it; only the caller may promote
it into the hole.

Model/tool/reroute/timeout/quota/schema/parse failure produces no candidate and
no mutation. An unasked or ineligible ordinary read contacts no child, waits on
no child, and returns the saved byte-identical ordinary result. Apply recaptures
current source and repeats ordinary validation.

**Required new rule — `MCP-OP-ELAB-CANDIDATE-READ`.** Only an explicit,
successful, terminal, one-owner, wall-class prepared read may request one
candidate. Retain `eaba46b2`'s 1,024-byte old-body floor, 512-byte/25% decision
ceiling, one turn, one candidate, exact pin, isolation, quota, and output
schema. Candidate data is an inert sidecar; `to` stays null; no accept/commit
cue or automatic copy exists. W1 acceptance remains a separate authority leaf.

**Justification.** This adds review-before-commit while reusing the ratified
security and economic boundary, rather than inventing a parallel generator.

**Blast radius.** Inspect input/output schema, prepared-result byte gate,
elaborator dispatcher/result classes, no-cue witnesses, quota accounting, and
W2 composition. Writer, compiler, verifier, rollback, and Spark identity
authority do not change.

**Falsifier.** Candidate in an authority-bearing field, auto-copy/commit,
identity-bearing accepted model output, more than one turn/candidate, child
contact outside explicit wall eligibility, ordinary byte/timing cue, automatic
replay, guard weakening, or candidate/hash treated as semantic correctness.

### Design B — ratified write-side elaborator (safe fallback)

```json
EDIT -> complete caller-authored identity/guards
        + to:null
        + elaborate:{decision:"one line"}
```

This already achieves the emission goal: the caller's complete identity,
guards, null hole, and `elaborate.decision` authorize one guarded write call;
the elaborator supplies only replacement data, then the ordinary path validates
and commits. No separate candidate promotion is required in this branch. It has
higher feasibility, but it does not provide review-before-commit or return a
candidate in the read. It is the safe fallback, not a claim that literal W3 is
already fulfilled.

**Cheapest preregistered screen and kill.** Use the existing independent
isolation receipt; do not rerun a parallel isolation program. First run a
zero-model fake-adapter contract matrix with exact, malformed, multi-candidate,
tool-bearing, rerouted, oversized, delayed, and identity-bearing outputs.
Require sidecar-only placement, null `to`, no writes, identity-free model input,
ordinary compiler validation, and byte-identical default/ineligible output.
Any authority leak, mutation, automatic replay, unasked child contact, or hash
linkage gap kills the leaf before a model cohort.

**Rank:** caller value 5 × feasibility 3 = **15**, overall **#5**.

## W4 — campaign memory across calls

The server may remember a closed mechanical recipe and immutable evidence
history. It may not decide that “same transformation” semantically applies to
`refund-card`. The caller still chooses the new subject, decision, and guards.
History may persist across snapshots; label validity may not.

### Design A — caller-carried append-only campaign head (selected)

```json
READ -> {
  "campaign_seed": {
    "campaign":"C7K","head":"sha256:E0","basis":"B0",
    "snapshot_guards":{"src/cards.clj":"sha256:old"},
    "labels_complete":true,
    "labels":{"R1":{"kind":"whole_owner","file":"src/cards.clj","owner":"charge-card","owner_token":"sha256:..."}},
    "recipe":{"kind":"whole_owner_replace","caller_holes":["subject","replacement"]},
    "executable":false,"write_authority":false
  }
}

NEXT -> {
  "campaign":"C7K","head":"sha256:E0",
  "snapshot_guards":{"src/cards.clj":"sha256:old"},
  "extend":{"subject":{"file":"src/cards.clj","form":"refund-card"}},
  "decision":{"replacement":"(defn refund-card ...)"}
}

RECEIPT -> {
  "campaign":"C7K","parent_head":"sha256:E0","head":"sha256:E1",
  "resolved_effects":[{"label":"R2","file":"src/cards.clj","owner":"refund-card","owner_token":"...","before_sha256":"...","result_sha256":"..."}],
  "next_action":"none"
}
```

The caller holds campaign/head and semantic intention. The server owns an
append-only content-addressed event artifact. There is no mutable “current
campaign.” Each event contains the complete basis, closed recipe, guards, and
receipt facts needed to fold that head. Concurrent continuations explicitly
branch or name one parent head; neither overwrites the other.

Unknown, tampered, cross-workspace, mixed-basis, incomplete-label, or stale
heads refuse before write. A changed source requires an explicit advance read
that appends a snapshot event and mints a complete new label universe. Old
`R1` never rebinds. An event-store failure publishes no new head. Refusals stay
inert; successes expand full identity and retain `next_action=none`.

This should be a separate explicit campaign capability, not a field smuggled
into the first-slice prepared request that forbids bases. Formalize the lawful
precedent: a campaign reference may name only one immutable evidence event; it
may never name mutable server state, replace snapshot revalidation, or suppress
receipt identity.

### Design B — bounded TTL basis/session

```json
READ -> {"campaign_basis":"CB9","expires_at":"...","labels":{...},"snapshot_guards":{...},"executable":false,"write_authority":false}
NEXT -> {"campaign_basis":"CB9","fill":{"subject":"R2","replacement":"..."}}
```

An immutable process-local TTL basis is lawful and fast. Expiry, restart, or
eviction returns exact `unknown-or-expired-basis`; drift returns an exact hash
refusal. There is no rehydration hint, name/position fallback, or mutable update:
a change mints a new basis. It fulfills short continuity, not durable memory.

A genuinely mutable TTL session is blocked. The only safe amendment would
allow mutable **projections** while authority remains the caller-supplied
immutable event head and every transition appends an event. Its falsifier is an
accepted call that cannot be reconstructed from `(event head, request, guarded
snapshot)` after deleting the cache. Since that amendment collapses into
Design A, do not create a mutable-session exception.

**Cheapest preregistered screen and kill.** Zero-model replay of retained
repeated-operation sequences against an explicit oracle. Require identical
subjects, operations, future bytes, effect identity, and receipt identity.
Inject stale files, reordered labels, forked heads, cache loss/restart,
cross-workspace tokens, and same-text/different-owner cases. Safety kills
precede value. Then kill the product claim if fewer than 50% of frozen repeated
transform sequences reuse a head without an added model turn or the caller
must re-emit more than half of the explicit mechanical identity.

**Rank:** caller value 4 × feasibility 4 = **16**, overall **#4**.

## W5 — universal ordinals for single-digit answers

One bare digit cannot uniquely select more than nine choices and cannot carry
snapshot or choice-set identity. “Universal” must mean every bounded choice
card has ordinals 1–9. Larger complete universes use deterministic radix-9
pages; each interaction may be one digit, but each digit is scoped by exact
`(choice_set_sha256, choice_point, page_hash, integer)`.

### Design A — universal presentational ordinals (lawful today)

```json
RESULT -> {
  "choice_set_sha256":"sha256:B7K",
  "choice_attestation":"server-signature-over-canonical-choice-card-and-root",
  "executable":false,
  "write_authority":false,
  "choice_card":{"choice_points":{"D1":{"kind":"owner_candidate","authority":false,"complete":true,"universe_count":2,"page_hash":"sha256:P0","options":[
    {"ordinal":1,"label":"R1","resolved_identity":{"file":"src/demo.clj","owner":"render-dashboard","owner_token":"sha256:...","snapshot_guards":{"src/demo.clj":"sha256:old"}}},
    {"ordinal":2,"label":"R2","resolved_identity":{"file":"src/demo.clj","owner":"refund-card","owner_token":"sha256:...","snapshot_guards":{"src/demo.clj":"sha256:old"}}}
  ]}}}
}

RESOLVE/READ -> {"choice_set_sha256":"sha256:B7K","choice_attestation":"<exact server signature>","choice_card":"<complete canonical card above>","choice_point":"D1","page_hash":"sha256:P0","answer":2}
RESOLVE RESULT -> {"selected_label":"R2","resolved_identity":{"file":"...","owner":"refund-card","owner_token":"..."},"authority":false,"next_action":"none"}
```

The response/caller carries the complete mapping, guards, and a server
attestation bound to canonical card bytes and workspace root. The server
verifies the attestation and rederives both hashes; it needs no mutable table or
opaque retained basis, and the caller cannot forge a new valid mapping merely
by recomputing an unkeyed hash. `2`, `"2"`, `2.0`, zero, negative, stale page,
mixed card, bad attestation, duplicate ordinal, incomplete universe, or
reordered-page hash do not coerce or fall back. Resolution is inert read
evidence. A client may let the human/model type only `2` while automatically
carrying the exact choice-card envelope; the server protocol still sees
complete scope.

Refusal-side numbering remains inert presentation. It can never become an
executable retry. For Spark-class callers, ordinals may organize evidence but
may not assert subject identity.

### Design B — snapshot-scoped ordinal alias in a prepared success

```json
PREPARED -> {
  "basis":"B7K",
  "choice_points":{"D1":{"complete":true,"options":[{"ordinal":2,"label":"R2","resolved_identity":{...}}]}},
  "caller_holes":["answers.D1"],"executable":false,"write_authority":false
}
CALLER -> {"basis":"B7K","answers":{"D1":2},"replacement":"..."}
RECEIPT -> {"answers":{"D1":{"ordinal":2,"label":"R2","file":"...","owner":"refund-card","owner_token":"..."}},"next_action":"none"}
```

**Required amended rule.** An exact caller integer may select a write subject
only as an alias for one server-generated label in a complete, immutable,
snapshot-fenced, typed choice basis. The request supplies exact basis and
choice-point identity; every reference is all-or-none from that basis; the
receipt echoes ordinal, label, owner-token proof, file, owner, operation,
before hash, and result hash. Raw source order, line/preorder indices, partial
lists, cross-basis ordinals, numeric coercion, fuzzy fallback, and refusal-
originated executable ordinals remain forbidden.

**Justification.** The wrong-owner positional bug allowed position to select
whatever subject currently occupied it. Here a permutation changes the exact
basis/page hash and the ordinal lowers only to its retained label. The intended
benefit is deletion of a recovery read, not locator-byte savings.

**Blast radius and falsifier.** This touches positional-authority specs,
label-basis kernel, all write decoders that accept answers, pagination,
stale-source preflight, and receipts. One permutation, cross-basis splice,
duplicate-text owner, stale replay, coherent-wrong-label construction, or
incomplete receipt that changes canonical subject/effect kills the exception.

**Cheapest preregistered screen and kill.** Property-test every registered
choice vector for complete injective cards, stable page hashes, exact label
recovery, and unchanged no-choice bytes. Include 0/1/9/10/27 choices and all
numeric/coercion attacks. Then purely lower the retained 27-owner ordinal
fixture and every permutation to explicit named-owner calls. Kill on one wrong
subject, ordinal drift without hash drift, an executable refusal, or more model
boundaries than the read it replaces.

**Rank:** caller value 3 × feasibility 3 = **9**, overall **#8**.

## W6 — speculative prefetch of likely next reads

### Design A — warm-cache-only speculation (selected first)

```text
READ -> ordinary byte-identical result

AFTER RESPONSE PUBLICATION:
  key = SHA-256(workspace identity,
                exact snapshot guards,
                provider/index identity,
                predictor policy version,
                canonical likely request)
  cache[key] = complete ordinary read result

LATER EXPLICIT READ -> exact guarded hit, otherwise ordinary computation
```

The caller still chooses every question. The server owns only a bounded,
workspace-scoped TTL/LRU derived cache that can be deleted without semantic
effect. The predictor is a closed mechanical policy such as definition to
references/callers; it is not natural-language planning and launches no model.

Every hit rechecks exact guards. Drift, incomplete index, provider change,
policy change, workspace mismatch, cancellation, timeout, eviction, or output-
budget difference becomes a silent miss followed by the ordinary read. A cache
failure never changes or delays the triggering response, creates a hint,
refusal, or `next_action`, or competes with foreground analyzer admission.

No amendment is required because the cache is disposable performance state,
not authority or session state. Observable contention or latency is still a
no-cue failure.

### Design B — explicitly requested inline curiosity dossier

```json
READ -> {
  "...ordinary_result":"unchanged prefix",
  "speculative_reads": {
    "policy":"callers+references/v1","authority":false,
    "executable":false,"write_authority":false,
    "snapshot_guards":{"src/a.clj":"sha256:old"},
    "complete_for_policy":true,
    "items":[
      {"ordinal":1,"request_sha256":"...","operation":"references","result":{...}},
      {"ordinal":2,"request_sha256":"...","operation":"callers","result":{...}}
    ]
  },
  "next_action":"none"
}
```

The first inline slice is explicit opt-in. It returns complete read evidence
for a named closed policy from one frozen snapshot, never an executable next
call or recommendation. The attachment is all-or-none under a byte/evidence
gate; failure restores the saved ordinary result with no omission cue. Later
writes recapture their own identity and guards.

Truly automatic inline attachment needs a narrow amendment: the server may
speculate only through a versioned closed mechanical read policy; evidence is
`authority=false`, complete-for-policy or absent, never auto-consumed, never
sets next action, and has zero observable cost/cue on ineligible ordinary
results. The justification is turn deletion on mechanically predictable read
families without transferring question choice or write authority to a model.
The blast radius is inspect eligibility/finalization, provider admission,
result budgets, foreground/background clocks, cancellation, telemetry, and
every consumer that might mistake attached evidence for a directive. Any
non-derivable item, partial flattering subset, ordinary byte/latency regression,
fixation correctness loss, or write consumption without ordinary revalidation
falsifies it.

**Cheapest preregistered screen and kill.** First run an offline, zero-model
next-call oracle that sees only call N and must name canonical call N+1. Kill
below 50% mechanically knowable second reads. If earned, deterministic cache
replay must return byte/semantic-identical results. Kill on a stale/cross-root
hit, any statistically detectable foreground latency regression under the
frozen differential, an absolute p95 regression above 5%, starvation, cap
breach, or exact hit rate below 35%. Only then compare ordinary PRE with one
attached dossier:
exact correctness, one read, zero fallback, no more than 1.25× unique evidence
bytes, and at least 30% lower complete wall. The oracle alone makes no speed
claim.

**Rank:** caller value 3 × feasibility 4 = **12**, overall **#7**.

## W7 — receipt auto-chronicle and personal savings

### Design A — offline manifest-bound daily fold (selected)

Do not add an ambient server write. A caller-owned paved command folds named
terminal receipts and the marker-bound substantiation report into deterministic
artifacts.

```json
INPUT -> {
  "schema":"clj-surgeon.receipt-chronicle-manifest.v1",
  "day":"2026-08-30","timezone":"America/Los_Angeles",
  "receipts":[{"path":"...","sha256":"..."}],
  "substantiation":{"report":{"path":"...","sha256":"..."},"receipt":{"path":"...","sha256":"..."}}
}

OUTPUT -> chronicle.json + chronicle.md + receipt.edn

chronicle.json -> {
  "day":"2026-08-30",
  "source_digests":{"receipt_manifest":"...","substantiation_report":"..."},
  "receipt_facts":{"validated":12,"verified_transactions":9,"classification":"measured"},
  "telemetry_window":{"start":"2026-08-30T00:00:00-07:00","end":"2026-08-31T00:00:00-07:00","transport_sessions":3,"prepared_committed":7,"classification":"measured","coverage":"separate marker-bounded denominator; not proven personal"},
  "window_projection":{"decode_equivalent_seconds":4.2,"classification":"projected","rate":"3.5237 ms/byte"},
  "personal_savings":{"status":"unavailable","reason":"no authenticated receipt-to-event join in phase 1"},
  "narrative":[
    "12 receipts validated; 9 verified transactions. [MEASURED]",
    "7 prepared commits occurred in the separate marker-bounded telemetry window. [MEASURED]",
    "Window decode-equivalent: 4.2 seconds at the named retained rate. [PROJECTED]",
    "Personal saving is unavailable because receipt and telemetry identities are not joined. [UNAVAILABLE]"
  ],
  "unavailable":[],
  "promotion_authority":false
}
```

The fold is closed-template and zero-model. It deduplicates by receipt SHA,
validates each named digest, narrates success/refusal/unverified/undo separately,
and never uses file mtime as event time. Day placement uses receipt-owned time
or explicit caller-attested `occurred_at`. Undo never erases gross activity.

It reuses the substantiation claims compiler's four types: `measured`,
`observed-before-after`, `projected`, and `unavailable`. Verified transaction,
file, edit, and registered feature-stage counts may be measured. A conversion
using the retained 3.5237 ms/byte rate remains **projected**. No screen, receipt,
or pretty narrative upgrades it to personal measured savings.

Receipt and telemetry denominators stay separate. The telemetry leaf explicitly
stores no receipt or public hash, so phase 1 may say “N validated receipts on
this day” and “M committed feature events in this marker window.” It may not
claim that M of those N receipts used a feature. Missing or partial telemetry is
`unavailable`, never zero. Phase 1 fulfills the daily narrative, but personal
savings remain unavailable unless caller-owned evidence supplies an exact join
and day/session confinement. Every artifact carries
`promotion_authority=false`.

### Design B — opt-in caller-side append-only chronicle inbox

A client observer appends terminal receipt hash, captured time, caller-paid
output bytes, and the digest of any registered comparison rule to a private
inbox, then invokes the same fold. That caller-owned exact join may publish a
**projected personal** number for the confined day; it still cannot call the
counterfactual measured. This improves automation and completes the personal-
projection wish, but adds client integration, append-gap, and retention policy.
Observer failure never changes or obscures the mutation result. A server-owned
mutable daily narrative is rejected because it taxes ordinary writes and
breaks the append-only posture.

No law amendment is required. A later cross-session “personal” server report
would require an authenticated stable caller identity, per-user privacy key,
opt-in, retention, and deletion policy; caller-supplied identity is not enough.

**Cheapest preregistered screen and kill.** Fold a frozen fixture set containing
success, refusal, unverified result, undo, duplicate, tampered receipt,
DST/midnight boundary, and partial telemetry twice. Kill on nondeterministic
bytes, double counting, accepted tamper, mtime inference, projection relabeled
measured, partial evidence coerced to zero, false per-receipt join, model/network
use, output outside the named root, or any ordinary tool/result/timing change.

**Rank:** caller value 3 × feasibility 4 = **12**, overall **#6**.

## W8 — explicitly ask “what could you prepare here?”

### Design A — explicit opt-in on an ordinary forms read (selected)

```json
REQUEST -> {
  "operation":"forms",
  "file":"src/demo.clj",
  "within":{"form":"render"},
  "snapshot_guards":{"src/demo.clj":"sha256:old"},
  "ask":{"preparations":true}
}

RESULT -> {
  "...ordinary_forms_result":"unchanged prefix",
  "preparations":{
    "choice_set_sha256":"...",
    "complete":true,
    "executable":false,
    "write_authority":false,
    "choices":[{
      "ordinal":1,
      "prepared_request":{"executable":false,"write_authority":false,"arguments":{"...":"full ordinary identity with null holes"},"caller_holes":["..."]}
    }]
  },
  "next_action":"none"
}
```

The caller explicitly asks on a read it already chose. This is not a new model,
write entrance, or passive offer. The request repeats readable subject identity
and an optional exact snapshot guard. A closed preparation registry returns a
complete all-or-none choice set. Each concrete descriptor repeats full file,
owner, old-source, count, and owner-token identity and keeps every decision
hole caller-owned. Ordinals are display only.

The server owns only the closed registry and current frozen read. It retains no
session, basis, or plan. If the requested subject is intrinsically ineligible,
the server returns the saved ordinary forms result byte-for-byte, exactly as if
`ask.preparations` were absent. It emits no empty array, `available:false`,
reason, near miss, omission field, coaching text, or timing dependency. A stale
ordinary snapshot guard follows the existing ordinary read behavior. Later
writes still recapture and validate everything.

This is the strongest preservation of no-cue: the explicit ask may reveal a
positive eligible answer, but an ineligible answer is literally the ordinary
result, not a negative capability cue.

### Design B — dedicated contextual query or static catalog

A static registry can answer which preparation kinds exist without inspecting
the requested subject. It needs no amendment and is safest, but cannot answer
“here.” A dedicated `operation:"preparations"` could return a complete positive
or empty contextual answer and save the caller from interpreting silent
omission, but that behavior would amend the observability law.

**Proposed amended rule for a dedicated query.** No-cue remains absolute for
default and ineligible ordinary operations. One authenticated, explicit,
read-only capability operation may return a complete positive or empty answer
about the caller-authorized subject. It may expose no near-miss reason, ranking,
hidden path, model readiness, or executable artifact, and may not change any
ordinary result.

**Justification.** The caller deliberately asks for availability, so a bounded
answer is the requested read evidence rather than an unsolicited steering cue.
Design A remains available if the house rejects that distinction.

**Blast radius and falsifier.** The amendment touches the operation registry,
authorization/confinement, no-cue equivalence tests, output budgets, and client
rendering. One default ordinary byte change, unauthorized subject fact, partial
catalog, near-miss reason, readiness/timing leak, or executable returned object
falsifies it.

**Cheapest preregistered screen and kill.** Exercise one eligible fixture and
every intrinsic ineligible case. For Design A, require exact default and asked-
ineligible result byte parity. For amended Design B, require a complete empty
contextual answer while the corresponding default ordinary call remains byte-
identical. In both designs require complete registry enumeration on an eligible
ask, all-or-none choices, null caller holes, public-schema validation after
caller fill, and ordinary stale recheck. Kill on Design A ineligible drift,
Design B default ordinary drift or nonempty invented eligibility, partial or
selectively omitted eligible offering,
filled decision hole, executable continuation, ordinal identity authority,
model/plan/write/session state, unauthorized workspace enumeration, or a stale
candidate surviving commit validation.

**Rank:** caller value 4 × feasibility 5 = **20**, overall **#2**.

## Self-adversarial house pass

| Attack | What would go wrong | Required defense / verdict |
|---|---|---|
| W1 digest authority laundering | A hash is treated as consent, freshness, or self-contained identity; miss/restart triggers repository search. | Digest is lookup/confirmation only. Resolve one exact retained object, expand full identity, recapture, and refuse inertly on miss/stale. Client rehydration is the first slice. |
| W2 preview theater | A polished diff says “verified,” an effectful verifier is called a forecast, or preview races commit. | Bind every fact to exact candidate/snapshot/environment hashes; unrun verification is `not_run`; commit always recompiles. |
| W3 candidate becomes decision | Candidate is inserted into executable `to`; Spark supplies target/guard; prompt-injected output is evaluated. | Keep `to=null`, candidate adjacent and non-authoritative, identity-free one-field output as data, caller-authored guards, independent promotion. |
| W4 mutable “current campaign” | Friendly labels silently rebind after source changes; concurrent calls overwrite one session head. | Append immutable events with caller-supplied parent head; remint labels on every snapshot advance; cache deletion cannot change authority. |
| W5 positional write authority | A digit and valid guard splice select a coherent wrong owner; >9 options are silently truncated. | Scope ordinal by complete choice-set hash; presentation/read resolution first; executable alias only under the explicit exception; radix pages preserve completeness. |
| W6 hidden no-cue tax | Wire stays identical but background work delays foreground calls, poisons stale caches, or anchors the caller on irrelevant evidence. | Warm-only first, exact guarded cache keys, silent miss, admission priority, bounded resources, inline only by explicit opt-in and all-or-none evidence. |
| W7 claims laundering | Duplicate/undo receipts inflate savings; paths leak into telemetry; a projection becomes measured. | Digest-set dedup, caller-owned artifacts, separate denominators, closed claims types, no model narrative, `promotion_authority=false`. |
| W8 discoverability becomes passive cue | Default results reveal near misses or readiness; the ask launches a preparer/model or returns a flattering subset. | Explicit forms-read ask, default/ineligible byte parity, ordinary path confinement, closed complete registry, inert descriptors, zero retained authority. |

The most dangerous composition is individually harmless objects laundering one
another:

```text
W6 prefetched choice -> W5 digit -> W4 label -> W3 candidate
  -> W2 preview -> W1 hash -> write
```

That chain is legal only when each arrow stays non-authoritative until the
caller fills/promotes the decision, and the final W1 lookup expands to complete
identity before the ordinary compiler recaptures it. W4 carries mechanical
context, W5 selects only an inert object, and W6 never supplies semantic
authority.

Global kill laws override every local score:

- any refusal becomes executable;
- any default/ineligible ordinary result changes bytes or acquires an
  observable eligibility timing dependency;
- any artifact survives root, schema, choice-set, or snapshot drift;
- any Spark-authored identity or guard is accepted;
- any cache/session state becomes necessary for correctness;
- any label universe is partial or tolerantly resolved;
- any receipt omits complete resolved identity;
- any ledger widens its privacy vocabulary without a separately ratified leaf;
- any derived saving is called measured; or
- any byte win is promoted without complete verified wall and correctness.

## Build order for the top four

### 1. W8 explicit preparations ask

Build the pure closed registry and no-cue restoration seam first. It gives the
caller an intentional place to ask for preparation, eliminates pressure to add
passive hints, and becomes the lawful entrance for W3 candidate requests and
W2 previews. Its zero-model parity screen has no dependency on the elaborator
or telemetry implementation.

### 2. W1 client rehydration, then gate the server CAS

Screen client-side SHA rehydration against the existing prepared-request
fixtures. This isolates whether the 217-byte-to-short-confirm experience is
valuable without changing server authority. If it clears the caller-output and
effect-parity gates, ratify the retained-descriptor rule and build the same
typed basis/CAS kernel that W4 can later reuse. Do not wait for W3.

### 3. W4 append-only campaign chain

Build immutable event heads and caller-carried parent-head continuation after
the W1 client screen establishes the value of short references. Reuse the same
complete snapshot-fenced label and receipt-identity kernel intended for a later
W1 server CAS, but do not make either feature depend on a TTL cache. Prove that
deleting every materialized projection leaves campaign authority reconstructible
from the event head, request, and guarded snapshot.

### 4. W2 pure candidate preview, then optional observed verification

Build pure preview first for explicit caller-supplied candidate bytes and the
ordinary candidate compiler. Ship diff/post-source/hash plus `not_run`
verification plan. Treat actual overlay verification as a separate later
ratification. Once W1 CAS exists, a reviewed preview may be confirmed by its
exact candidate/descriptor identity, but W2 never grants acceptance authority
itself.

### Parallel dependency lanes — telemetry/W7 and elaborator/W3

The ratified substantiation design authority is `4831b8a7`; `bb4506ca` is its
frozen-red test commit, not a runtime ledger. The green implementation following
that red must land and stabilize before a fresh causal/model screen relies on
W1 emission/consumption, W3 candidate stages, or W2 preview exposure. W7 then
becomes the first reporting follow-on: add only the offline receipt-chronicle
fold after the telemetry claims compiler and marker receipt stabilize. It need
not block the top four mechanisms, and it may not retrofit a receipt-to-event
join the ledger does not contain.

Literal W3 waits for `docs/embedded-elaborator-ratification-20260830 @ eaba46b2`,
its independent isolation admission, and the green production implementation.
Extend that one adapter with a candidate-sidecar result class and plug it into
the already-built W2 preview input; never create an inspect-specific model
runner. Register `elaborator.*` stages before any claims fold and freeze the
fake-adapter authority matrix before one model call.

W6 warm-cache-only may run its oracle independently but should not consume
analyzer capacity while the top four are being measured. W5 presentational
numbering can be added to W8 cards without write authority; the executable
ordinal exception waits for the basis kernel and its own wrong-subject screen.

## Evidence and design anchors

- Prepared request and no-cue/identity laws: `docs/intent/prepared-request/` on
  `docs/prepared-confirm-preview-ratification-20260830`; installed receipt
  `4ec9394c`; live-route measurement `9f2b1ba4`.
- Complete label-basis, prepared-hole bang, preview-accept, and cross-cutting
  gates: `docs/observations/2026-08-30-label-addressed-write-designs.md` at
  `9c57fa71`.
- Embedded elaborator authority/lifecycle packet:
  `docs/intent/embedded-elaborator/` at `eaba46b2`; Spark identity falsifier
  `b0432c25`; independent isolation receipt `3c2cc192`.
- Canonical effect identity plan:
  `docs/plans/2026-08-29-canonical-effect-identity.md` in the 2026-08-30
  integration lineage.
- Stateless read guards/continuations and retained-basis precedent:
  `docs/intent/mcp-operation-contract/` at `eaba46b2` and
  `docs/plans/proof-carrying-change-buffer.md` at `28f4a11a`.
- Positional-authority boundary and ordinal recovery evidence:
  `docs/intent/positional-mutation-authority/` at `3117fe44` and ordinal screen
  `2919ad8c`.
- Read-transition/dossier gates and Rule 5 economics:
  `docs/plans/2026-08-27-brain-fleet-next-hills.md` at `98801805`, emission
  study `935cc0d2`, and `docs/why-reading-is-cheap-and-writing-is-expensive.md`.
- Substantiation telemetry and claims discipline: ratified design `4831b8a7`
  in `docs/intent/substantiation-telemetry/`, followed by frozen red
  `bb4506ca`; neither is described here as a green runtime ledger.

## Decision

The dream list does not require weakening the laws. It requires making the
laws do more work earlier.

The server should lavish cheap read-side bytes on complete proof, candidate
data, and legible forecasts; retain exact immutable objects where repetition
really pays; let the caller emit only decisions; and still expand everything
back to complete identity before the sole ordinary writer runs. The amendments
worth considering are narrow and falsifiable: retained descriptor confirmation,
read-side elaborator candidate data, observed overlay verification, and
snapshot-scoped ordinal aliases. Mutable sessions, predictive verification,
bare-digest authority, globally meaningful digits, and passive eligibility
cues remain rejected.

That is the common fulfillment mechanism: **prepare lavishly, decide narrowly,
confirm exactly, revalidate completely, and chronicle honestly.**
