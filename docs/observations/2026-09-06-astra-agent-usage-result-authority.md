# Astra — actual usage exposed two collector defects

A fixed07:52–08:45UTC diagnostic sampled one root Codex session. Its receipt
reported five catalog MCP calls (three inspect, two edit) and six box-wide
service calls. Bounded inspection of its named transcript proved the extra
service inspection was the same root's manual shell/HTTP registration probe.
It remains a separate transport fact, not a sixth catalog call. No generic
shell-HTTP detector was added; whole-box events cannot generally be attributed
to one sampled client.

Two actual defects are repaired:

- edit_clojure appeared in operation counts and the completed-item apply clock,
  but its missing route suffix classified the action as native-read or omitted
  it. It now has the surgeon-apply route like the other edit entrances.
- A successful source inspection contained error-type literals and was falsely
  counted as refused. The actual failed edit returned structuredContent with
  error_type=invalid-intent-form and isError=true, but the underscore spelling
  escaped the output regex. The aggregate refusal count accidentally matched
  reality while identifying the wrong operation and type.

Codex refusal counts now use only completed registered-server McpToolCall typed
results. Returned source/content remains opaque. Added
clj_surgeon_typed_mcp_outcomes reports ok/refused/unknown completed items;
clj_surgeon_refusal_evidence names typed-mcp-items-only or unknown. Protocol
success does not claim semantic correctness. Outer call counts and wall retain
their separate meaning. CLI/untyped custom output is unclassified: no source
regex supplies a refusal, success, or execution-error verdict. Metadata absent
at a window boundary remains a coverage limit. No timing-based fuzzy join.

Faithful RED fixtures and owning Python self-test GREEN are retained under
`/var/tmp/forge/astra-usage-edit-route-fx/`; six source/result boundary fixtures
also check metadata arriving after custom output, unrelated servers, unknown
results, privacy and bounded error codes. The earlier collection receipt is
superseded for route/refusal interpretation. Root owns one identical-bounds
rerun after independent review; counts from the two receipts must not be merged.
No collection rerun, providers, JVMs or shared-service work occurred in this cut.
