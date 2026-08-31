---
parent: high-level-design
prefix: MCP-OP-PREP-ACT
status: 'ratified in advance subject to adversarial PASS (Gene, 2026-08-30, verbatim: "If build 1 and 2 are go -- get adversarial review and build and go")'
---

# Prepared Request Confirm and Preview

This leaf owns two caller-experience extensions to the installed
`prepared_request` surface:

- **W1 confirm-by-hash:** submit one served descriptor SHA-256 plus the caller
  hole values instead of retranscribing the complete guarded arguments; and
- **W2 dry-run preview:** compile that filled request without effects and
  return a complete bounded would-be diff and forecast.

Gene authorized implementation in advance only if an independent adversarial
review passes. The packet includes that review. Frozen red, implementation,
dual verification, live measurement, and installation remain distinct gates.

## Present architecture

The prepared projector already creates a complete canonical descriptor with
explicit canonical root, project-relative file, named owner, exact old source,
`matches=1`, and null `to` holes. `descriptor-sha256` hashes its exact canonical
JSON bytes. The descriptor is currently returned without its hash, retained
state, or an alternate execution entrance. A caller must copy the arguments,
fill every hole, and emit the full request.

The inspect integration adds the descriptor only after intrinsic eligibility
and removes it again when the normalized complete result would exceed 32,768
bytes. The ordinary `edit_clojure` entrance then performs independent public
schema validation, workspace routing, confinement, source capture, compilation,
atomic mutation, read-back, receipt, and rollback.

## The non-negotiable W1 finding

A SHA-256 is not reversible. The proposed compact call:

```json
{"confirm":"<descriptor-sha256>","fill":{"arguments.edits[0].to":"..."}}
```

cannot recover root, files, owners, old source, counts, or hole order without
one of three costs:

1. the caller retransmits those bytes;
2. the server retains the served descriptor; or
3. the token itself carries the descriptor bytes.

Therefore a hash-only stateless implementation is impossible. This design
chooses bounded retained state and names it directly. The hash is an exact
content address, not a snapshot, selection, capability, or source of write
authority.

## Options and decisions

### W1 Option A — stateless hash-only confirmation

Why it might be right: no retention, expiry, eviction, or session lifecycle.

Cost: impossible. A digest cannot reconstruct the descriptor. Searching the
workspace by digest would require an unbounded source census and could select
the wrong historical descriptor.

Decision: reject.

### W1 Option B — bounded session registry (selected)

After final result budgeting preserves an eligible descriptor, the inspect
integration stores its exact canonical bytes and frozen file-hash map under
`[boot-epoch, mcp-session-key, descriptor-sha256]`. The returned result exposes
a separate inert `prepared_confirmation` object. A later compact call resolves
only inside the same session, rechecks the target-file hash, fills the exact
declared holes, and invokes the ordinary editor.

Why it is right: it achieves the byte deletion while preserving exact served
identity and ordinary terminal validation.

Cost: explicit memory, session-key, expiry, eviction, collision, restart, and
replay laws. A transport without a stable session key cannot offer W1.

Assumption: a five-minute, small bounded registry covers the human/model
handoff without becoming durable authority.

### W1 Option C — self-contained signed or encrypted token

Why it might be right: stateless across server processes and sessions.

Cost: the token must carry essentially the descriptor bytes plus framing and a
MAC, erasing the emission benefit. Key rotation, confidentiality, and replay
become a new security subsystem.

Decision: reject for the first slice.

### W2 Option A — attach preview to every prepared read

Why it might be right: zero additional caller action.

Cost: replacement holes are still null, so no post-edit result exists. Any
attached preview would invent meaning or merely repeat the old source. It also
taxes every eligible read.

Decision: reject.

### W2 Option B — explicit preview on the confirmation call (selected)

The caller supplies the same exact confirmation and fills with
`"preview":true`. The server resolves and snapshot-checks the descriptor,
compiles the ordinary edit without effects, and returns one complete bounded
diff. Preview does not consume the confirmation for commit, but the registry
permits at most three preview calls per descriptor. Commit repeats
`confirm+fill` without `preview`, recaptures source, and runs the full ordinary
transaction.

Why it is right: preview has complete caller meaning, uses the same identity
and compiler, and remains mechanically separate from commit authority.

Cost: one optional round trip and response bytes. Preview may become stale
immediately and must never be accepted by commit.

### W2 Option C — preview token as commit authority

Why it might be right: avoids recompilation after review.

Cost: turns an inert response into a saved write plan, creates replay and stale
authority, and bypasses the ordinary transaction fences.

Decision: reject absolutely.

## Served confirmation shape

Only a prepared descriptor that survives the existing 32,768-byte inspect
result gate is eligible for this separate top-level object:

```json
{
  "prepared_confirmation": {
    "descriptor_sha256": "<64 lowercase hex>",
    "expires_in_ms": 300000,
    "session_bound": true,
    "commit_single_use": true,
    "executable": false,
    "write_authority": false
  }
}
```

The object is inert evidence. It does not make `prepared_request` executable,
does not replace any explicit identity inside the descriptor, and never
appears when preparation is ineligible or removed by the result budget.
Integration then trial-associates the confirmation and applies the same
32,768-byte complete-result gate again. If the augmented candidate does not
fit, it returns the already-budgeted prepared result by identical object
identity, omits confirmation, and registers nothing. Registration and the
surfaced confirmation therefore occur together or not at all.

The eligible concise result retains its ordinary prefix and exact current
three-sentence prepared coaching byte-for-byte. The public tool description
and schema explain the alternate confirmation shape; the result adds no prose.
This preserves `MCP-OP-PREP-REQ-005` and avoids charging every eligible read
for a second instruction. No dynamic hash, source, file, owner, root, user,
session, or network value enters prose.

## Registry and session law

The process-local registry contains at most 32 live descriptors per MCP session
and 256 total. Each entry contains:

- random boot epoch and stable transport-owned MCP session key;
- canonical descriptor bytes and parsed descriptor;
- descriptor SHA-256;
- exact frozen project-relative file-hash map and canonical workspace root;
- exact ordered caller-hole paths;
- monotonic issue and expiry times;
- preview count; and
- lifecycle `live`.

The registry also retains bounded source-free per-session tombstones so it can
distinguish a locally consumed, expired, or evicted confirmation from a digest
that was never served. A tombstone contains only boot epoch, session key,
digest, terminal reason, and monotonic terminal/expiry times. It retains no
descriptor, root, identity, source hash, or fill. Each session retains at most
64 tombstones and the process at most 512; each expires 300,000 ms after the
terminal transition. Tombstones use the same deterministic capacity order as
live entries.

Entries expire 300,000 ms after the latest identical descriptor is successfully
surfaced in that session. Registration occurs only after the descriptor and
confirmation survive complete result budgeting. An identical hash with
identical canonical bytes may refresh the entry. If an injected or real digest
collision presents different canonical bytes under one key, the server removes
both entries, disables confirmation for that boot, and returns
`prepared-confirmation-hash-collision` without effect.

Capacity eviction is deterministic oldest-expiry-first, then oldest-issued,
then lexical hash. Reload, process restart, session end, and server shutdown
destroy entries. The registry is never persisted, shared across sessions, or
used to discover source.

The transport must provide one stable, unforgeable session key to both inspect
and edit handlers. Stdio may bind the one server connection. HTTP must bind the
SDK session, not an IP address, root, request ID, or caller-supplied value. If a
transport cannot prove this join, confirmation is unavailable on that
transport and the prepared result remains byte-identical to today's result.

The descriptor hash is not a secret. Session binding, not digest secrecy,
prevents one caller from exercising another caller's retained descriptor. A
lookup never reports that a digest exists in a different session: another
session receives the same `prepared-confirmation-unknown` response as a digest
that was never served. Cross-session denial is exact behavior, but not an
existence oracle.

## Confirm request and fill law

`edit_clojure` gains one disjoint alternate request shape:

```json
{
  "confirm": "<64 lowercase hex>",
  "fill": {
    "arguments.edits[0].to": "(complete replacement form)"
  }
}
```

The shape permits only `confirm`, `fill`, and optional literal
`"preview":true`. It forbids workspace root, edits, programs, deletions,
relations, verification, expected counts, basis, retry, preview hashes, and
every unknown field. `fill` must contain every and only the retained
`caller_holes` path once. Every value must be a nonblank string. The server
returns exact ordered expected, provided, missing, and extra hole vocabularies
on mismatch; it emits no source or corrected executable request.

Resolution happens before ordinary workspace routing because the compact call
does not repeat a root. The session-bound entry supplies the exact canonical
root. The ordinary router must accept that stored root; otherwise confirmation
refuses. The server re-reads every retained target file once and requires the
complete file-hash map to equal the served snapshot. This is intentionally
stricter than ordinary prepared arguments: any target-file drift expires the
confirmation and requires a new read.

After exact fill and snapshot validation, the server reconstructs the original
`prepared_request.arguments`, replaces only the declared nulls, and submits the
result to the exact public `edit_clojure` schema and ordinary transaction.
Confirmation grants no verifier, formatter, writer, rollback, or result
authority.

A commit-shaped request consumes the entry atomically after exact fill and
ordinary public schema admission but before transaction execution. Success,
stale refusal, compiler refusal, rollback, crash, or unverified outcome cannot
replay that entry. Shape and hole mismatches do not consume it; expiry and the
three-preview limit remain.

## Complete confirmation refusal vocabulary

| Error type | Condition | Required remedy |
|---|---|---|
| `invalid-prepared-confirmation` | Malformed hash, request shape, or fill value. | Correct only the named invalid fields; no registry lookup occurred. |
| `prepared-confirmation-unknown` | No live entry or local tombstone exists in this boot and session, including a digest served only to another session. | Run one eligible `inspect_clojure` read again. |
| `prepared-confirmation-expired` | Monotonic TTL elapsed. | Read again; never refresh from the confirm call. |
| `prepared-confirmation-evicted` | A known entry left the bounded registry. | Read again. |
| `prepared-confirmation-consumed` | Commit already reached the single-use consume boundary. | Read again; never retry the old commit blind. |
| `prepared-confirmation-hash-collision` | Same hash, different canonical bytes. | Confirmation is disabled for this boot; use explicit ordinary arguments. |
| `prepared-confirmation-hole-mismatch` | Missing or extra hole paths. | Fill exactly the returned expected paths. |
| `prepared-confirmation-snapshot-drift` | Current target-file hashes differ from the served snapshot. | Read again; no old confirmation is executable. |
| `prepared-confirmation-preview-limit` | More than three previews requested. | Commit from current evidence or read again; no additional preview runs. |
| `prepared-preview-output-limit` | Complete preview cannot fit its exact bounds. | Use ordinary explicit review or narrow the prepared selection. |

Every refusal has `ok=false`, `source_unchanged=true`,
`mutation_attempted=false`, `write_authority=false`, the failed stage, exact
available/returned counts where applicable, and no executable next call,
prepared request, selected replacement, receipt, inverse, or terminal success
response. Ordinary editor refusals retain their existing exact error type and
evidence after reconstruction; W1 does not wrap them into a generic failure.

## Dry-run preview

Preview uses the same session lookup, exact fill, router, and frozen target-file
hash check as commit. It then invokes the ordinary pure compile path with
effect capabilities restricted to source reads. It reaches no writer, receipt
publisher, formatter, verifier, rollback, process launcher, or lower-layer
exit.

The successful result has this closed meaning:

```json
{
  "ok": true,
  "operation": "edit_clojure-preview",
  "lifecycle": "preview",
  "committed": false,
  "mutation_attempted": false,
  "write_authority": false,
  "receipt": false,
  "source_unchanged": true,
  "descriptor_sha256": "<hash>",
  "fill_sha256": "<hash>",
  "snapshot_guards": {"src/example.clj": "<old file hash>"},
  "future_file_hashes": {"src/example.clj": "<future file hash>"},
  "changed_files": 1,
  "changed_characters": 42,
  "diff": "<complete unified diff>",
  "verification_forecast": {
    "will_run": false,
    "profile": null,
    "reason": "edit_clojure-does-not-authorize-transaction-verification"
  },
  "preview_sha256": "<canonical preview hash>",
  "next_action": "none"
}
```

The diff is complete or absent. It is limited to 16,384 UTF-8 bytes and 256
lines, and the complete normalized MCP result is limited to 32,768 bytes. If
any complete bound fails, preview returns `prepared-preview-output-limit` with
required and allowed byte/line counts and no partial source or diff. Preview
hashes are comparison evidence only. The commit schema rejects
`preview_sha256`, preview result objects, future hashes, and diffs.

`verification_forecast` is deliberately not a verifier result. The installed
prepared surface targets `edit_clojure`, which forbids transaction
verification. The first slice therefore reports `will_run=false`; it does not
guess a profile, run a verifier, or imply that parse/compile success is semantic
correctness. A later `apply_clojure_changes` preview would require a separate
leaf.

Preview does not consume the descriptor. A later commit must repeat the exact
confirmation and fill, pass a fresh snapshot check, enter ordinary validation,
and consume the entry. Preview is not accepted as evidence of freshness or
authority. Source drift between preview and commit refuses.

## No-cue and compatibility law

Intrinsic ineligible reads, prepared descriptors omitted by either byte gate,
refusals, continuations, other inspect modes, writes, CLI results, and other
tools remain byte-identical for fixed clocks. They register no descriptor and
show no confirmation, preview, omission cue, coaching, or telemetry event.

The original complete-arguments route remains supported. Existing clients may
fill and submit `prepared_request.arguments` unchanged. W1 is an optional
alternate input shape on the same `edit_clojure` tool. It preserves the exact
public operation on commit and introduces only `edit_clojure-preview` for the
inert lifecycle. `edit_clojure` still rejects `verify`; use
`apply_clojure_changes` when verification must share rollback authority.

## Telemetry and claims

Product telemetry may record only confirmation eligibility/emission, digest,
session-safe lifecycle class, age bucket, preview count, refusal class, request
and response byte counts, phase clocks, and ordinary transaction receipt hash.
It never records descriptor bytes, root, file, owner, old source, fills,
replacement, diff, preview source, or session key.

The W1 performance hypothesis is **projected until measured**: deleting repeated
descriptor arguments should reduce caller emission and request-construction
decisions. Descriptor and compact-call bytes/tokens must be measured on the
same retained tasks before any magnitude is stated. A live counterbalanced
cohort must separately report first-call correctness, route adherence, emitted
bytes/tokens, server wall, result-to-next-action wall, complete verified wall,
expiry/refusal/recovery, and every assigned loss. A smaller request alone is
not a performance win.

W2 is a safety and review feature. Its response bytes are input-side payload;
they are reported but do not count as deleted caller emission. Promotion
requires preview correctness, zero effects, and no increase in incorrect
commits or stale retries.

## Decisions and alternatives

| Decision | Selected | Rejected | Rationale |
|---|---|---|---|
| Hash recovery | Bounded session registry | Stateless digest; self-contained token | A digest is not reversible; carrying the descriptor erases the claimed deletion. |
| Registry scope | Boot + MCP session + digest | Global digest; workspace-only; IP binding | The digest is not secret and must not cross caller sessions. |
| Commit replay | Single-use after schema admission | Unlimited replay; consume only on success | Failed/unknown transactions must never invite blind retry. |
| Preview trigger | Explicit `preview=true` on confirm/fill | Automatic inspect attachment | Meaning does not exist until holes are filled. |
| Preview authority | None; repeat full confirm/fill for commit | Commit by preview hash | Preview can stale immediately and never replaces transaction fences. |
| Preview payload | Complete bounded diff + hashes | Truncated diff; unbounded full source | Partial source can mislead; unbounded results harm callers. |
| Verification | Honest no-verifier forecast | Guess/run exact verifier | `edit_clojure` explicitly lacks verifier authority. |
| Implementation owner | Sol/SURGEON1 lane; SURGEON2 verifies | SURGEON2 self-implementation | Session state, routing, and pure preview integration are not a small diff; preserve independent review. |

## Implementation boundary

The smallest product seam is:

1. one pure confirmation schema/fill/preview projector;
2. one bounded runtime registry owned beside MCP runtime state;
3. inspect integration after final prepared-result budgeting;
4. one disjoint confirm branch before ordinary workspace routing;
5. one pure compile-only preview adapter; and
6. ordinary edit reconstruction and execution unchanged below that branch.

Expected overlap is `mcp-prepared-request`, `mcp-inspect-tool`, `mcp-schema` or
the edit public schema owner, `mcp-runtime`, `mcp-tool`, and focused tests. The
ordinary transaction, writer, receipt, rollback, formatter, verifier, prepared
projector, refusal-evidence projector, and CLI executor do not gain alternate
authority.

Before implementation annotations land, the closed repository intent audit
must include this spec registry.

## Verification sequence

1. Freeze this exact packet and adversarial PASS receipt.
2. Prove stable session identity for stdio and HTTP before registry code.
3. Write frozen red for every `MCP-OP-PREP-ACT-*` requirement.
4. Implement injected clock, digest, session, and registry seams first.
5. Implement confirm/fill reconstruction with an ordinary public-schema spy.
6. Implement preview against the pure compiler with throw-on-effect spies.
7. Run affected and full gates, then an independent verifier replay.
8. Measure W1 byte/token and caller-route effects; keep the claim projected
   until the cohort passes.
9. Return immutable implementation, dual-verify, measurement, rollback, and
   live-proof plan to Gene before install.
