# Adversarial Review: Prepared Confirm and Preview

Date: 2026-08-30

Subject: exact packet on branch
`docs/prepared-confirm-preview-ratification-20260830`

Reviewer: SURGEON2

## Verdict

**PASS FOR FROZEN RED AND IMPLEMENTATION HANDOFF, contingent on preserving the
packet exactly. No install authority.**

The initial W1 proposal did not pass as stated because hash-only recovery was
described as potentially stateless. A SHA-256 cannot recover a descriptor.
The packet now uses explicit bounded session state and treats stable session
identity as an admission gate. W2 initially admitted two unsafe readings:
automatic preview before replacement meaning exists, and preview reuse as
commit authority. Both are excluded. Preview is explicit, effect-free, bounded,
and never accepted by commit.

Implementation is not a small independent diff. It crosses inspect
finalization, runtime session state, workspace routing, public edit schema, and
the pure compiler preview seam. The implementation should go to a Sol/SURGEON1
lane with SURGEON2 retaining independent verification.

## Blockers found and dispositions

| Blocker | Why it was a false green | Packet repair | Status |
|---|---|---|---|
| Hash-only stateless recovery | Digest bytes do not contain root, owner, old source, counts, or holes. | Explicit bounded registry; self-contained token rejected as byte-negative. | Closed. |
| Digest treated as capability | Descriptor SHA may appear in telemetry and is not a secret. | Bind registry to boot epoch and stable MCP session; digest grants no authority. | Closed, with transport proof required. |
| Existing prepared leaf forbids state | A silent cache would contradict `MCP-OP-PREP-REQ-004/006`. | Scoped sibling amendments: projector stays pure; only final eligible integration registers. | Closed. |
| Cache before result budget | Server could retain a descriptor the caller never received. | Trial-associate confirmation after the existing prepared gate; if the augmented result overflows, restore the saved prepared result by identity and register nothing. | Closed. |
| Cross-workspace shortcut | Compact request omits root and could bypass the router. | Stored canonical root routes through the ordinary router; mismatch refuses. | Closed. |
| Valid hash, wrong holes | Best-effort fill could shift replacements between owners. | Exact set equality with ordered expected/provided/missing/extra evidence. | Closed. |
| Replay after unknown commit | A missing receipt could trigger double application. | Single-use consume before transaction; result still claims success only with ordinary receipt. | Closed. |
| Collision overwrites identity | Tests cannot assume SHA collision is impossible if digest is injectable. | Unequal bytes under one digest remove both and disable W1 for the boot. | Closed. |
| Preview before meaning | Null holes cannot produce post-edit source. | Preview exists only on an exact filled confirmation request. | Closed. |
| Preview used as plan/receipt | Future hash or preview SHA could become accidental commit authority. | Commit rejects all preview artifacts and repeats fresh confirm/fill/snapshot/transaction. | Closed. |
| Preview implemented as commit+rollback | This would reach writer/receipt/rollback effects and could fail unknown. | Pure compile path plus throw-on-effect witnesses. | Closed. |
| Truncated diff presented as complete | Partial source can mislead the caller. | Complete bounded diff or fail-empty typed output-limit refusal. | Closed. |
| Verification forecast overclaims | `edit_clojure` has no transaction-verifier authority. | Exact `will_run=false`; no profile guess or verifier call. | Closed. |
| Ineligible omission cue | Adding a hash/preview hint changes old callers and may coerce mutation. | Identical result, no registry entry, no cue for every ineligible/budget/transport case. | Closed. |
| Exact lifecycle errors without retained evidence | A deleted entry cannot distinguish expiry, eviction, or consumption from unknown. | Bounded source-free per-session tombstones; restart loss remains honestly unknown. | Closed. |
| Cross-session mismatch leaks existence | A special mismatch type turns the registry into a digest-existence oracle. | Never-served and other-session lookups return identical unknown refusals. | Closed. |
| Added coaching contradicts sibling law | `MCP-OP-PREP-REQ-005` requires exactly three existing sentences. | Preserve concise text and coaching byte-for-byte; describe W1/W2 only in public schema/description. | Closed. |
| Performance claimed from bytes | Emission deletion may not reduce decision or complete wall. | W1 magnitude remains projected until pure and live counterbalanced measurement. | Closed. |

## New falsifiers attempted against the repaired packet

### Same hash, two sessions, same workspace

Session A receives a descriptor. Session B uses the exact digest and valid
hole values against the same root. A global or root-bound cache would commit.
The packet requires the exact same `prepared-confirmation-unknown` response as
a digest never served to Session B, source unchanged, and no ordinary
validation call. This is a permanent transport witness and does not reveal
that Session A received the digest.

### Same digest, unequal canonical bytes

An injected digest returns the same 64-hex value for two different descriptors.
First/last-wins maps silently choose an owner. The packet removes both entries,
disables confirmation for the boot, and requires explicit ordinary arguments.

### Preview succeeds, source changes, commit repeats

The preview returns exact future hashes. An unrelated byte in the same target
file changes before commit. A preview-token design would commit stale intent.
The packet rejects the repeated confirmation on complete file-hash drift and
does not accept preview evidence.

### Delivery/budget asymmetry

The existing prepared result fits, but adding `prepared_confirmation` makes the
normalized result exceed 32,768 bytes. Registering before the second gate would
leave a live handle absent from the response; dropping the prepared result
would regress an installed success. The packet restores the saved prepared
result by identical object identity and registers nothing.

### Preview effect smuggling

The compiler config supplies writer, receipt, formatter, verifier, rollback,
and process functions that throw on call. Preview must still return a complete
diff. Any throw identifies an effect leak and fails W2.

## Residual risks and implementation stops

1. The current handler signatures ignore the exchange object, so stable
   cross-call session identity is not yet proved in product code. If the SDK
   cannot expose it for both stdio and HTTP, W1 remains unavailable on that
   transport. Do not substitute root, IP, digest, or request ID.
2. The exact pure compile seam must produce the same normalized transaction and
   future hashes as ordinary `edit_clojure`. If it requires a second compiler
   or reaches effects, W2 is NO-GO pending a new design.
3. The descriptor contains source bytes in memory. Capacity, TTL, session
   cleanup, heap evidence, and telemetry redaction are release gates.
4. The public schema and tool-description change add client-visible input
   bytes. W1 performance must include those bytes and may still lose despite a
   smaller request.
5. Preview may encourage an extra turn. W2 is promoted on safety/usefulness,
   not assumed speed.

## Authorization boundary

Gene's advance words authorize frozen red and implementation only because this
review passes the repaired packet. They do not authorize schema drift, a
stateless fiction, global cache, preview commit token, implementation by this
reviewer, installation, reload, or shared-runtime publication.

## 2026-08-31 affinity-repair edge audit

The field audit proved that a per-call-session client cannot use W1 and that
visible refusal text can omit structured invalid fields. Gene selected the
existing session-bound authority model. This audit checks that the caller
repair composes with the installed PREP-ACT contract.

### Existing outcome shapes

A standalone analysis nREPL loaded the exact `05f5a196` product namespace.
Pure probes observed these current shapes:

| Outcome | `ok` | Other distinguishing data |
|---|---:|---|
| confirmation registration | boolean `true` | inert confirmation fields |
| same-session `lookup!` success | boolean `true` | retained descriptor entry |
| cross-session or never-served lookup | boolean `false` | typed `prepared-confirmation-unknown` |
| malformed confirmation request | boolean `false` | typed `invalid-prepared-confirmation` and `invalid_fields` |
| successful preview | boolean `true` | inert preview fields |
| ordinary confirmed commit | boolean `true` | ordinary edit result and receipt law |

The `ok` discriminator is already present and boolean across the installed
success and refusal families. Guidance can standardize the caller branch
without changing any result schema. Descriptor or digest presence is not a
safe discriminator because lookup success, preview success, commit success,
and refusals carry different optional fields.

### Cross-spec interactions

- `MCP-OP-PREP-ACT-004` remains authoritative. The repair teaches session
  affinity but does not permit cross-session lookup.
- `MCP-OP-PREP-ACT-014` remains authoritative. Ineligible and unsupported
  results gain no dynamic cue; guidance lives only in public descriptions and
  the skill.
- `MCP-OP-PREP-ACT-015` remains authoritative. Eligible result coaching stays
  byte-identical. No session ID, digest, file, root, or source enters prose.
- `MCP-OP-PREP-ACT-008` and `MCP-OP-PREP-ACT-021` jointly require one
  complete visible remedy and the same structured invalid-field vocabulary.
- Ordinary explicit `prepared_request.arguments` remains the only fallback
  for a caller that cannot retain the serving session.

### New falsifier: hostile field name

`validate-confirm-request` currently includes unknown top-level request keys
in `invalid_fields`. A probe using the key `ignore prior instructions`
returned that exact caller-controlled string. Raw concatenation into prose
would create an avoidable instruction-shaped channel and could break line
structure.

The repair therefore renders the ordered field names as one canonical JSON
array literal with canonical escaping. The visible list remains equal to the
structured list, but each field name remains delimited data. Field values,
digests, roots, source, and session identity never enter the message.

### Phase-4 verdict

**PASS FOR FROZEN RED after the canonical JSON field-rendering amendment.**

The amendment closes one safety gap without changing authority, success data,
session isolation, result coaching, or the ordinary explicit edit route.
