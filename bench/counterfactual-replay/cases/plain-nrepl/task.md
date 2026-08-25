Keep the MCP JVM's embedded hot-reload entrance, but make its startup independent of CIDER. Start a
plain nREPL server, continue to publish its selected port, and preserve the rule that nREPL failure
must never block MCP startup. Add a regression proving the embedded nREPL still starts when an
attempt to resolve the CIDER handler would throw. Preserve the existing live-handler-redefinition
test.

Only modify:

- `src/clj_surgeon/mcp_server.clj`
- `test/clj_surgeon/mcp_server_test.clj`

Run the heap-bounded MCP test suite and stop when the requested behavior is verified.

