# Prepared confirmation and preview: independent public-wire NO-GO

Date: 2026-08-30

## Verdict

**NO-GO** for measurement, installation, publication, or promotion of exact
candidate `90486da5d4d2d4ed7a6efed443142105a2b41d2d` (tree
`5775162523df3008aa03e5698a08c590293465af`).

The focused Clojure tests are green, but the official W1 request shape is not
accepted through either real MCP transport. JSON decoding supplies
`java.util.Map` values. The confirmation validator normalizes only Clojure
maps, so it rejects the valid public fields `confirm`, `fill`, and `preview`
before session lookup. The defect blocks both W1 confirm-by-hash and W2
preview.

No install, reload, shared-runtime action, model call, or live measurement was
performed.

## Identity and ratified intent

- Candidate: `90486da5d4d2d4ed7a6efed443142105a2b41d2d`
- Candidate tree: `5775162523df3008aa03e5698a08c590293465af`
- Frozen red parent: `fe27c6a8cedcf3b4a34948b42c3c9400afdb740a`
- Ratified packet: `714cadab`
- The six owning design, specification, and review blobs were byte-identical
  between the ratified packet and the candidate.

The ratified contract requires the public request
`{"confirm": descriptor_sha256, "fill": caller_holes}` with optional
`"preview": true` to join the descriptor to the same MCP session, validate it,
and either preview inertly or enter the ordinary transaction.

## Independent evidence

### Frozen red and focused green

- Frozen red replay at `fe27c6a8`: 18 tests, 18 assertions, exactly 18
  failures, 0 errors.
- Candidate focused replay: 19 tests, 92 assertions, 0 failures, 0 errors.

These results reproduce the registered red/green geometry. They do not prove
the public JSON boundary because the focused tests pass Clojure persistent maps
directly.

### Pure boundary differential

The same logical request was passed to `validate-confirm-request` in two
representations:

- Clojure map with string keys: accepted.
- `java.util.LinkedHashMap`, matching JSON transport decoding: refused as
  `invalid-prepared-confirmation`, with `confirm` and `fill` reported as
  invalid fields.

The cause is `public-keyword-map` in
`src/clj_surgeon/mcp_prepared_confirmation.clj`. Its first predicate is
`map?`. That predicate is false for a Java map, so neither the top-level
request nor the nested `fill` map is normalized to the closed keyword
vocabulary.

### Real transport probe

Probe source: `/tmp/w1w2_transport_probe.clj`

Probe SHA-256:
`9541fd001fd77c64757e00aa7f8fd23b736de5df4fa01de6d2b6888d922cc92f`

The probe started fresh isolated real Streamable HTTP and stdio MCP servers,
performed protocol initialization, obtained a real served prepared descriptor
from `inspect_clojure`, and submitted the official public JSON shapes.

Observed on Streamable HTTP:

- Cross-session confirmation: `invalid-prepared-confirmation` at request-shape
  validation, not the required indistinguishable unknown-identity refusal.
- Same-session preview: the same request-shape refusal.
- Same-session commit: the same request-shape refusal.
- Replay: the same request-shape refusal.
- Source remained byte-identical.

Observed on stdio:

- Same-session preview: `invalid-prepared-confirmation` at request-shape
  validation.
- Same-session commit: the same request-shape refusal.
- Replay: the same request-shape refusal.
- Source remained byte-identical.

The error evidence was fail-closed (`source_unchanged=true`,
`mutation_attempted=false`). This is safe, but it does not implement the
ratified feature. Because validation stops before registry lookup, the real
wire route cannot establish the required cross-session unknown identity,
preview non-authority, consume-once, tombstone, or commit behavior.

### Why the existing transport-looking test is a false green

`real-handler-preview-stales-and-fresh-confirmation-commits-once` constructs an
`McpAsyncServerExchange` directly and supplies Clojure maps. It proves handler
composition after normalization; it does not cross JSON decoding. The public
transport contract therefore lacked a permanent executable witness.

## Required successor

A successor must remain fail-closed and:

1. Normalize actual JSON-decoded `java.util.Map` values at the public boundary,
   including nested `fill`, without weakening duplicate-field or unknown-field
   refusal.
2. Add permanent real-wire stdio and Streamable HTTP witnesses that obtain a
   descriptor, then cover same-session preview, same-session commit,
   cross-session unknown identity, consume-once replay, and byte-exact source
   effects.
3. Retain the existing pure projector, registry, tombstone, budget,
   object-identity restore, and coaching byte-identity tests.
4. Replay the focused and complete gates only after the real-wire contract is
   green.

The complete cold suite was intentionally not rerun on this candidate after
the deterministic public-contract failure. A broad green cannot repair an
unusable official route.

## Merge-queue consequence

This NO-GO does not convoy unrelated candidates. It blocks only this W1/W2
head and any dependent publication. The substantiation-telemetry candidate
remains provisional; after W1 is repaired and rebased, telemetry must add the
separate cross-feature witness that an official `{confirm, fill}` consumption
is counted exactly once.
