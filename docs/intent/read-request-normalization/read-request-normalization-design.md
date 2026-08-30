# Closed Inspect Request Normalization

Status: draft LLD for ratification.

## Context

An `inspect_clojure` batch contains subject identity and call-local
bookkeeping. File paths, owner names, forms, expressions, expectations,
snapshot guards, and a prepared basis participate in the requested read.
Subrequest IDs correlate ordered results inside one call. The literal
`operation` field selects one of four closed request shapes.

Call-local bookkeeping can be normalized only when the remaining request has
one interpretation. Normalization must not guess a subject, widen a read, turn
partial evidence into a complete batch, or create mutation authority.

## Boundary

The first slice applies only to the typed `requests` batch accepted by
`inspect_clojure`.

- A batch in which every subrequest supplies `id` preserves the supplied IDs
  and current duplicate-ID refusal.
- A batch in which no subrequest supplies `id` receives deterministic IDs in
  input order: `request-1`, `request-2`, and so on.
- A batch that mixes supplied and omitted IDs refuses before snapshot capture.
- A subrequest may omit `operation` only when it supplies `file`, a non-empty
  `forms` array, and exact `expect.forms`, with no fields owned by another
  operation. That shape normalizes to `operation=forms`.
- Every other operation-less shape refuses before snapshot capture.
- Explicit `forms`, `outline`, `match`, and `xray` requests preserve their
  current validation and evaluation behavior.

The aggregate `expect.requests` and `expect.files` fields remain exact. ID and
operation normalization runs before the current request and aggregate
validation, then delegates to that validation unchanged.

## ID ownership

Generated IDs are deterministic, call-local presentation evidence. They are
not server state, a JSON-RPC identifier, a basis, a snapshot guard, a subject
selector, or write authority.

Every result projection within the call uses the normalized ID:

- ordered success rows;
- `request_id` on a refusal;
- completed and pending continuation IDs;
- retry-template requests and hole evidence; and
- concise result labels.

A server-produced retry template may repeat the generated string as an
explicit label. The string carries no authority beyond the snapshot guards and
exact selectors in that template. Separate calls may generate the same IDs.

## Closed forms implication

Operation omission is accepted by structure, not frequency:

```text
operation absent
AND file present
AND non-empty forms present
AND exact expect.forms present
AND no outline, match, or xray-only field present
  -> operation=forms
```

The `forms` operation's observed share does not grant authority. The complete
remaining shape does. A request containing only `file`, or containing `match`,
`inside`, `expression`, a partial forms pair, or fields from several variants
has no unique operation and refuses.

## Refusal contract

Mixed ID ownership returns a typed validation refusal:

```clojure
{:operation "inspect_clojure"
 :reason :mixed-request-ids
 :path ["requests"]
 :source_unchanged true
 :read_started false}
```

Ambiguous operation omission returns:

```clojure
{:operation "inspect_clojure"
 :reason :operation-required
 :path ["requests" INDEX "operation"]
 :source_unchanged true
 :read_started false
 :supported ["forms" "match" "outline" "xray"]}
```

The refusal may report supplied field names. It does not choose an operation,
return source, publish a continuation, or emit an executable next call.

## Public schema

The MCP input schema must describe the same closed language as runtime
validation:

- `id` is optional in each typed request variant;
- `operation` is optional only in the complete forms variant;
- the operation-less forms variant still requires `file`, `forms`, and
  `expect`;
- no operation-less variant admits `match`, `inside`, or `expression`; and
- runtime validation remains authoritative for the all-supplied/all-omitted
  batch relation, which JSON Schema cannot express locally without duplicating
  the complete batch grammar.

## Behavior matrix

| ID shape | Operation shape | Outcome |
|---|---|---|
| All explicit and unique | Explicit valid operation | Preserve current request and result IDs. |
| All explicit with duplicate | Explicit valid operation | Existing duplicate-ID refusal. |
| All omitted | Explicit valid operations | Assign ordered call-local IDs. |
| Mixed supplied and omitted | Any | Typed refusal before snapshot capture. |
| All supplied or all omitted | Operation-less complete forms shape | Normalize to `forms`, then apply ordinary validation. |
| All supplied or all omitted | Operation-less file-only outline shape | Typed refusal before snapshot capture. |
| All supplied or all omitted | Operation-less match or xray fields | Typed refusal before snapshot capture. |
| All supplied or all omitted | Partial forms shape | Typed refusal before snapshot capture. |
| All supplied or all omitted | Fields from several variants | Typed refusal before snapshot capture. |

## Alternatives rejected

- **Default every absent operation to `forms`.** File-only outline requests
  would silently return a different result.
- **Generate only missing IDs.** Collision and ownership rules would become
  order-dependent and would blur caller versus server labels.
- **Use array indexes without materialized IDs.** Existing refusal,
  continuation, retry, and concise projections require a stable call-local
  correlation value.
- **Persist generated IDs across calls.** Snapshot guards and retained bases
  already own cross-call continuity. A second state mechanism adds no subject
  proof.

## Verification

Permanent witnesses must cover the complete behavior matrix at the pure
validator and real MCP schema boundaries. A loader spy must prove mixed-ID and
ambiguous-operation refusals perform zero snapshot reads. One selector-local
failure must prove that a generated ID is preserved through refusal,
continuation, and retry-template evidence. The retained request corpus may
verify the projection and size claim, but it cannot replace the ordinary
contract tests or authorize promotion from projected to measured wall time.
