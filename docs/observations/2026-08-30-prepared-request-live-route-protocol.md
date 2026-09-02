# Prepared-request live-route measurement protocol — 2026-08-30

Status: frozen before execution. This protocol measures wire cost; it cannot
authorize installation or promote a recovery claim.

## Question

What input cost does the prepared-request slice add to `tools/list`, and what
output cost does one eligible `inspect_clojure` response add for one and six
forms? Does an ineligible seven-form response remain semantically identical to
the control? How many request bytes and tokens must the caller emit when it
fills the descriptor compared with constructing the same canonical public
arguments from scratch?

## Frozen subjects

- control commit `c55de2279826af5ed21c90981591479dd2e802b2`, tree
  `565f009f0ff25fdedbc2fba5ad9ba5f55783e023`;
- candidate commit `b445a8c3595d70f6f05b6edccb9b1a924539a195`, tree
  `b1e21af8073e66283f82f4036583bfe2971c4b0a`; and
- one clean generated workspace containing seven named `def` forms.

Both arms use fresh private 512 MiB stdio MCP processes, telemetry off, no
nREPL, the same workspace bytes, and the same ordered JSON-RPC requests. The
measurement shall not register, reload, install, or contact a shared MCP
runtime.

## Calls and metrics

Capture the exact serialized initialize, `tools/list`, and tool-call request
and response bytes for:

1. one explicit `forms` read containing one named owner;
2. one explicit `forms` read containing six named owners; and
3. one explicit `forms` read containing seven named owners, which is
   intrinsically ineligible for a descriptor; and
4. one separate eligible read followed by one real `edit_clojure` submission
   after filling the descriptor's only null hole.

Measure bytes and `o200k_base` proxy tokens using exactly `tiktoken 0.9.0` for
the full `tools/list` response, the `inspect_clojure` tool definition, input
schema, output schema, each request, arguments object, response, and each
emitted descriptor. Tokens are a stable local proxy, not provider billing
authority.

For the paid path, independently construct the equivalent guarded
`edit_clojure` arguments from the same frozen source. Retain both serialized
argument objects, require exact decoded JSON equality, submit the
filled-descriptor object through the real MCP route, and require the exact
expected committed source. JSON object-member order is not semantic and may
differ; preserve and measure both raw serializations. This separates
request-size savings from construction and recovery savings.

## Predeclared validity

- `environment_valid`: both exact commit/tree subjects are clean before any
  process starts;
- `route_adherent`: both arms use the real MCP stdio JSON-RPC route and the
  actual `tools/list` and `tools/call` serialization;
- `semantic_correct`: every call succeeds, control emits no descriptor,
  candidate emits one and six ordered edits for the eligible calls, candidate
  emits no descriptor for seven forms, and ordinary structured results are
  equal after removing candidate `prepared_request` and timing fields; and
- `source_unchanged`: the fixture hash is byte-identical before and after both
  read-only arms; and
- `paid_path_correct`: filled and independently constructed arguments decode
  to equal JSON values, the filled request commits with verification complete in a
  separate workspace, and the final source hash equals the frozen expected
  hash.

If any validity condition fails, retain the attempt as invalid and publish no
cost interpretation.

## Interpretation law

Report catalog input overhead separately from result output overhead. Do not
price them as interchangeable: prior measurement found model input far cheaper
than model output. Also report the filled-versus-from-scratch emitted-request
delta even if it is exactly zero. This screen measures delivery and final-call
size; it does not prove that callers use the descriptor, avoid a refusal, or
save a turn. Compare these measured costs with the separately replicated
recovery direction only in the install decision card, with every quantity and
its different confidence class stated.

Retain every raw payload, process command, stderr stream, report, and SHA-256
manifest.
