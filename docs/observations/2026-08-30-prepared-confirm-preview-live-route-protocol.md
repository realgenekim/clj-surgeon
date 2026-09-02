# Prepared confirm/preview live-route price protocol

Status: frozen before execution on 2026-08-30. Measurement only; no install,
reload, registration, publication, or shared-runtime action is authorized.

## Question

Measure the exact serialized MCP price of W1 confirm-by-hash and W2 dry-run
preview at candidate `05f5a1962e5a0c5aa0365c673994eca9024c1a44`
against installed-surface control
`9af88fbae9ee720613599feaf8cf58432c5898bb`.

The primary number is caller-emitted request size for one prepared mutation:
candidate `{confirm, fill}` versus the complete equivalent guarded
`edit_clojure` arguments. W2 response size is caller input. Neither byte count
is a wall-time or routing claim.

## Route and subjects

Launch one control and one candidate process through the repository's real
`-X:clj-surgeon/mcp-stdio` entrance. Each arm has an exact clean worktree, a
fresh private workspace, private receipts/logs, `-Xms64m/-Xmx512m`, telemetry
off, nREPL disabled, and no shared port or MCP registration.

Each workspace contains three byte-identical files:

- `src/demo.clj`: eligible `alpha` form used for preparation and mutation;
- `src/ineligible.clj`: namespace-only read used for the no-cue check; and
- `src/ordinary.clj`: one direct ordinary edit used for compatibility.

Both arms receive the same eligible `inspect_clojure` request. The control
must return the installed complete `prepared_request`; the candidate must
return the same descriptor plus `prepared_confirmation`. The control fills
and submits the complete prepared arguments. The candidate first previews,
then submits `{confirm, fill}`, then replays that compact commit once to prove
single-use consumption.

## Token and price law

Count exact compact UTF-8 JSON bytes and `tiktoken==0.9.0` `o200k_base` proxy
tokens. Count complete JSON-RPC envelopes and the `arguments` objects the model
must construct separately.

Catalog and response growth are caller input/prefill. They are cheap and the
catalog is cacheable, but neither is free. The complete-arguments versus
confirm/fill delta is caller output/decode and is the claim's home. The
previous recovery cohorts remain separate evidence; this zero-model screen
cannot claim fewer turns or lower complete wall.

## Frozen measurements

Report bytes, proxy tokens, and SHA-256 for:

1. complete `tools/list` responses and exact inspect/edit tool objects;
2. complete equivalent full-arguments commit versus compact confirm/fill;
3. candidate preview request and response;
4. candidate consumed replay response;
5. ineligible inspect responses; and
6. ordinary direct edit requests and responses.

Raw JSON request/response payloads, command lines, stderr, source fixtures,
normalized no-cue objects, report JSON, and a complete SHA-256 manifest are
retained.

## Validity gates

- `environment_valid`: exact clean control/candidate commit and tree; private
  512 MiB processes; identical fixture sources; telemetry/nREPL/shared runtime
  disabled.
- `semantic_correct`: the control full request and candidate compact request
  commit the exact same source bytes; preview is complete and inert; compact
  replay refuses `prepared-confirmation-consumed`; no-cue calls succeed; the
  confirmation is inert and session-bound.
- `route_adherent`: every result comes through real stdio JSON-RPC
  `initialize`, `tools/list`, and `tools/call`; no direct Clojure map or private
  registry call substitutes for the wire.

The ineligible inspect result and ordinary direct edit result must be
byte-identical after excluding only timing fields, per-run receipt/undo values,
and private arm path spellings. Raw payloads remain authoritative and expose
every excluded difference. Any failed validity field invalidates the run.
Losses remain in the report.

