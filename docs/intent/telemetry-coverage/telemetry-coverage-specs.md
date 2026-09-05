---
parent: telemetry-coverage-design
prefix: MCP-OP-TELCOV
status: "implemented 2026-09-05"
---

# Server-Side Telemetry Coverage Specifications

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

- [x] **MCP-OP-TELCOV-001**: When any public MCP tool call completes at the
  server — successfully, as a typed refusal, or by throwing — clj-surgeon
  shall append exactly one `tool.dispatch` telemetry event for that call,
  carrying the telemetry session id, a request id unique to that call, the
  tool, monotonic start and finish clocks with wall milliseconds, the outcome,
  and serialized payload sizes in and out.

- [x] **MCP-OP-TELCOV-002**: When clj-surgeon records a tool call, it shall
  name the PUBLIC tool the caller invoked, never the internal operation or
  shared handler the server routed to — so a compact `edit_clojure` call is
  never attributed to `apply_clojure_changes`.

- [x] **MCP-OP-TELCOV-003**: While a public tool refuses, clj-surgeon shall
  record that refusal's typed kind on the same event, so a refused call is
  counted as a call and its class is attributable without reading receipts.

- [x] **MCP-OP-TELCOV-004**: When a public MCP tool is registered without a
  structured specification — the one shape that would be built outside the
  dispatch boundary and therefore recorded nowhere — clj-surgeon shall refuse
  to build that tool, naming the tool and the remedy, rather than serving it
  with silent telemetry.

- [x] **MCP-OP-TELCOV-005**: When the agent-usage collector reports Surgeon
  MCP usage, it shall report call counts per public tool name, per telemetry
  session id, and per telemetry ROOT, and shall print every root it read in
  the receipt, rather than inferring ownership from launcher process names.

- [x] **MCP-OP-TELCOV-006**: While a scanned telemetry root exists but holds
  no telemetry files, the collector shall report that root as `no files under
  <root>` rather than as zero calls; and every Surgeon telemetry receipt shall
  state the explicit window it covers.
