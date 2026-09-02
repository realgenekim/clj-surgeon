# Prepared confirmation session-affinity field audit

Date: 2026-08-31

## Outcome

Installed W1 is correct for session-retaining MCP clients and unusable for a
client that initializes a fresh MCP session for every tool call. The Mayor
harness field report also exposed a separate malformed-request problem and a
visible refusal-completeness defect.

The findings are independent:

```text
served descriptor
      |
      +-- valid request, same session ------> preview succeeds
      |
      +-- valid request, fresh session -----> prepared-confirmation-unknown
      |
      `-- malformed request, any session ---> invalid-prepared-confirmation
```

Therefore session loss cannot explain an
`invalid-prepared-confirmation` result. The Mayor request was malformed before
registry lookup as well as being unable to retain the serving session. The
field transcript does not contain the raw structured request, so this audit
does not guess which field was malformed.

No product, install, reload, or shared-runtime mutation was performed.

## Identity

- Installed launcher source:
  `~/.local/share/clj-surgeon/versions/05f5a1962e5a0c5aa0365c673994eca9024c1a44`
- Installed product commit:
  `05f5a1962e5a0c5aa0365c673994eca9024c1a44`
- Installed tree:
  `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`
- Annotated tag: `stable-prepared-confirm-preview-20260831`
- Isolated probe: `/tmp/w1w2_per_call_session_probe.clj`
- Probe SHA-256:
  `c3b6b2c813032a8bae8255d837dba9cfa107aa95a37ecf3d331962a4dc8c787e`

The probe ran an isolated HTTP server with the installed product bytes and a
temporary fixture. It did not call the shared server.

## Controlled matrix

### Same retained session

An eligible `inspect_clojure` result served one descriptor. The immediate
`edit_clojure` request used the same `Mcp-Session-Id` and the public shape
`{confirm, fill, preview:true}`.

Result:

- `ok=true`
- `operation=edit_clojure-preview`
- `source_unchanged=true`
- `write_authority=false`
- exact bounded diff returned

### Valid request in a different session

The same descriptor and valid fill were submitted after a fresh initialize.

Result:

- `error_type=prepared-confirmation-unknown`
- `failed_stage=registry-lookup`
- `source_unchanged=true`
- visible remedy: `Run one eligible inspect_clojure read again.`

The response is correctly indistinguishable from a never-served digest.

### Per-call-session harness

The harness initialized session D, performed the eligible read, discarded D,
initialized session E, and submitted the valid confirmation.

Result: the same `prepared-confirmation-unknown` refusal. Repeating the read
cannot help a caller that discards every serving session; the current remedy
therefore creates an infinite recovery loop for this caller class.

### Malformed request

The probe submitted a valid digest with an empty `fill` map.

Structured result:

```clojure
{:error_type "invalid-prepared-confirmation"
 :failed_stage "request-shape"
 :invalid_fields ["fill"]
 :supplied_fields ["confirm" "fill" "preview"]
 :source_unchanged true}
```

Visible text:

```text
edit_clojure
  refused · invalid-prepared-confirmation

✓ source unchanged
→ Correct only the named invalid fields.
```

The client did not truncate the field list: the server never rendered it into
visible content. The structured payload is complete, but callers that expose
only MCP text cannot see the named field. This contradicts the response's own
wording and the complete-vocabulary usability law.

## Product consequences

1. The existing same-session correctness and telemetry witnesses remain valid
   for session-retaining clients.
2. Adoption denominators must distinguish clients that can retain the serving
   MCP session. A served descriptor is not an actionable W1 opportunity for a
   per-call-session client.
3. The telemetry rebase must not generalize its exactly-once consumption proof
   to the sessionless caller class.
4. The unknown remedy must stop teaching an impossible loop. Without revealing
   whether the digest ever existed, it can say: use the same MCP session that
   served the descriptor, or send ordinary explicit edit arguments.
5. The malformed refusal's visible content should render `invalid_fields`.

## Decision W1-AFFINITY-1

### A. Retain session-bound authority and fix clients now

```text
inspect -- Mcp-Session-Id S --> confirm on S --> commit/preview
```

Why it might be right: this preserves the ratified safety and privacy boundary.
The descriptor cannot become mutation authority outside its serving session.
Fix the Mayor harness to retain `Mcp-Session-Id`; add explicit caller guidance
and safe capability negotiation so clients that cannot retain a session do not
treat W1 as actionable.

Cost: client adapters must retain sessions, and clients that cannot do so do
not receive W1's speed benefit. Capability negotiation is a small new design
leaf rather than a documentation-only change.

Assumption underneath: the important MCP clients can retain a standard
Streamable HTTP session, or can be upgraded cheaply.

### B. Add a portable, one-use confirmation capability

```text
inspect --> random capability + frozen descriptor
fresh session --> capability + fill --> fenced commit/preview
```

Why it might be right: it works for per-call-session harnesses without making
the caller re-emit the descriptor. The capability can remain TTL-bound,
consume-once, workspace-bound, and exact-snapshot-fenced.

Cost: this is a new security and privacy boundary. The descriptor digest must
not silently become cross-session authority. Use a separate unguessable token;
never log it raw; HMAC it in telemetry; preserve indistinguishable unknown
responses; and threat-model token disclosure and cross-client replay. It needs
full LID and adversarial review.

Assumption underneath: sessionless clients are common enough that their W1
benefit justifies portable bearer authority on the local server.

### C. Document per-call clients as unsupported

```text
fresh session per call --> ordinary explicit edit only
```

Why it might be right: zero new product risk and no expansion of mutation
authority.

Cost: the Mayor harness and possibly a material share of real clients cannot
use W1. Serving descriptors to them creates dead-end UI unless cues can be
suppressed.

Assumption underneath: sessionless callers are rare or unimportant.

## Recommendation

Choose **A** now. The deciding argument is that session binding is a ratified
authority boundary, while the immediate defect is a client that discards its
session. Repair that client and the refusal vocabulary before broadening
authority.

Fallback: if a bounded caller census shows that important clients cannot retain
sessions, design **B** explicitly with a random one-use capability. Do not make
`descriptor_sha256` itself portable authority.

If Gene is silent, default to A and do not change the server authority model.
