# WRITE-REFUSAL-001 live-route price protocol

Status: frozen before execution on 2026-08-30. Measurement only; no install,
reload, registration, or shared-runtime action is authorized.

## Question and scope correction

Measure the exact serialized MCP price of `MCP-OP-WRITE-REFUSAL-001` at
candidate `9af88fba` against the installed-surface control `b445a8c`.

The candidate implements generic write `expect-count-mismatch`. It does not
implement the read-side `batch-form-selection-failed` refusal. Therefore the
retained 191 firings, 2.72 MB, and 33.5 minutes of recovery-read evidence must
not be priced as this slice's benefit. The live measurement may establish only
the response-input price and no-cue behavior. Recovery-turn reduction for this
write family remains unmeasured.

## Frozen calls

Both arms expose exact `edit_clojure` over private stdio MCP processes and
receive byte-identical requests without `workspace_root`:

1. A realistic one-file, 27-owner request with 27 matches and `expect.matches`
   28. The control shall refuse; the candidate shall refuse with 27 complete,
   source-free, inert evidence rows.
2. A one-file, 129-owner request with 129 matches and `expect.matches` 130. The
   candidate shall return 128 rows, exact available/returned/omitted counts,
   and an inert bounded continuation.
3. An ordinary one-owner successful edit. Both arms shall commit the same
   effect and return byte-identical normalized structured content.

Capture `tools/list`, the exact `edit_clojure` tool object, every request,
response, command, stderr stream, normalized success result, report, and a
SHA-256 manifest.

## Token and price law

Count UTF-8 bytes and `tiktoken==0.9.0` `o200k_base` proxy tokens on exact
serialized JSON. Catalog and refusal-response growth is caller input/prefill.
It is cheap relative to caller output/decode, but it persists in context and is
not free. This experiment contains no model and cannot claim wall-time or turn
reduction.

## Validity

- `environment_valid`: exact clean control/candidate commit and tree; two
  isolated 512 MiB MCP processes; telemetry, nREPL, and shared runtime off.
- `semantic_correct`: both expected refusals occur; 27-row evidence is
  complete; 129-row evidence is honestly bounded; authority is absent; refusal
  sources stay unchanged; successful effects match.
- `route_adherent`: actual MCP stdio JSON-RPC initialize, `tools/list`, and
  `tools/call` routes only.
- No-cue equality excludes only named dynamic timing keys, per-run
  `receipt_hash` and `undo_receipt`, and private arm/root path spellings. Raw
  payloads remain retained so the normalization is auditable.

Any failed validity field makes the measurement invalid. Losses remain in the
report.
