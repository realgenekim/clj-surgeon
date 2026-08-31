# Prepared Confirm/Preview Frozen Red Declaration

This declaration is frozen before product code for the ratified
`MCP-OP-PREP-ACT-001..018` packet at commit `714cadab` (design blob
`300418bc`, specifications blob `73c7539e`, adversarial-review blob
`c62eaec6`).

## Admission gate

The offered transports are stdio and Streamable HTTP. Both pass admission on
MCP SDK 0.17.2. The SDK creates each server session identifier with
`UUID.randomUUID()`, retains it in SDK-owned session state, and copies it into
every `McpAsyncServerExchange` delivered to tool handlers. Stdio owns one
server connection/session. Streamable HTTP resolves the opaque
`Mcp-Session-Id` only through the SDK's initialized session table. Both
`inspect_clojure` and `edit_clojure` already receive that exchange object.

No workspace root, IP address, request ID, descriptor digest, or caller field
is admitted as session identity.

## Frozen red count

The focused command is:

```sh
PREP_CP=$(clojure -Spath -M:clj-surgeon/mcp-test)
java -cp "$PREP_CP" clojure.main -e \
  "(require 'clj-surgeon.mcp-prepared-confirmation-test) (let [r (clojure.test/run-tests 'clj-surgeon.mcp-prepared-confirmation-test)] (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

Before product code, the exact expected result is:

- tests: **18**
- assertions: **18**
- failures: **18**
- errors: **0**

Each failing assertion is the missing production entrance for the matching
ratified ID. Once that entrance exists, the same 18 tests expand into the
packet's permanent literal falsifiers, including the W2 no-effect and bound
edges. The test source is frozen with this declaration. Product implementation
must make it green without weakening or deleting a witness.
