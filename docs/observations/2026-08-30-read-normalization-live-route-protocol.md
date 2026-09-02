# Read-request normalization live-route token protocol

Date: 2026-08-30 PT

State: preregistered before either measured MCP server launch.

## Objective

Measure the actual serialized MCP cost of read-request normalization. Compare
the exact candidate `c55de2279826af5ed21c90981591479dd2e802b2` against its
exact parent `b9db064a86c3919660a38f79ab5031dcf6d49f98`. The parent is the
causal control. Its five relevant MCP surface files are byte-identical to the
installed stable reference `19ab864889799b0028a5f7cb66c63b957ff7b973`.

This experiment measures only. It does not install, reload, register, or
publish either server.

## Route

Launch one control and one candidate process through the repository's real
`-X:clj-surgeon/mcp-stdio` entrance. Each process has:

- a clean, exact worktree;
- a common private read-only fixture after initialization;
- private receipt and log paths;
- `-J-Xms64m -J-Xmx512m`;
- telemetry off;
- nREPL disabled; and
- no shared port or MCP configuration.

Send compact UTF-8 JSON-RPC 2.0 payloads terminated by one newline. Retain the
exact request and response JSON bytes. Count both JSON payload bytes and wire
bytes including the newline delimiter.

## Token basis

Use local `tiktoken 0.9.0` with `o200k_base`, matching the request-carriage
token screen. It is a stable comparison proxy, not a claim about the
provider's billing tokenizer. Count tokens over the exact retained JSON payload
without the transport newline.

For calls, report two request measurements:

1. the complete JSON-RPC envelope; and
2. the `arguments` object the model must construct.

This distinction prevents fixed envelope bytes from hiding the grammar delta.

## Frozen call matrix

Use one source file containing named forms `alpha` and `beta`.

| Shape | Purpose |
|---|---|
| explicit single | unchanged one-form control |
| operation-less single | omit only `operation: forms` |
| explicit-ID multi | two-request control with `request-1` and `request-2` |
| omitted-ID multi | omit both IDs, retain explicit operations |
| mixed IDs | one supplied ID and one omitted ID; must refuse |

Submit every shape to both servers. The current server is expected to refuse
the two new shorthand shapes at public schema admission. The candidate is
expected to accept them. Both servers must accept the explicit controls.

## Validity gates

The measurement is valid only when all conditions hold:

- exact control and candidate head/tree identities are clean before launch;
- the control's relevant surface files equal the installed stable reference;
- `tools/list` advertises `inspect_clojure` in both arms;
- explicit single and explicit multi calls succeed in both arms;
- candidate operation-less and omitted-ID calls succeed;
- current operation-less and omitted-ID calls refuse;
- candidate mixed IDs refuse with `mixed-request-ids`,
  `source_unchanged=true`, and `read_started=false`;
- after removing only timing fields, candidate shorthand structured results
  equal the current explicit controls;
- the fixture hash is identical before and after all calls; and
- every request traverses actual MCP stdio JSON-RPC. No internal Clojure
  function call may stand in for the route.

Report `environment_valid`, `semantic_correct`, and `route_adherent`
separately. A failure in one field must not overwrite another.

## Comparisons

Report exact bytes, `o200k_base` tokens, and SHA-256 for:

- complete `tools/list` responses;
- the complete serialized `inspect_clojure` tool definition;
- the `inspect_clojure` input schema alone;
- every call's complete request, arguments, and complete response; and
- the following causal pairs:
  - candidate operation-less single versus current explicit single;
  - candidate omitted-ID multi versus current explicit-ID multi;
  - same-shape explicit controls; and
  - same-shape mixed-ID refusals.

Savings are `(current - candidate) / current`. Negative savings are retained
as losses. Schema growth and response growth remain visible even when request
arguments shrink.
