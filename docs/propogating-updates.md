# Propagating clj-surgeon updates

clj-surgeon has three publication paths. They are related, but they do not
publish the same artifact.

| Changed artifact | Publish command | Who receives it |
|---|---|---|
| Clojure MCP implementation | `make mcp-reload` | Every caller of the shared dev MCP server on port 7888 |
| MCP schema or tool description | `make mcp-reload` | The shared server immediately; connected clients that honor `tools/list_changed` refresh their tool catalog |
| cclsp TypeScript | Save the file | The shared Bun supervisor rebuilds/reloads cclsp on port 7890 |
| CLI implementation or installed agent skills | `make install` | Future `~/bin/clj-surgeon` calls and future Codex/Claude skill loads |

## The shared development route

There is one shared clj-surgeon MCP server and one shared cclsp server. A
repository joins them with:

```bash
clj-surgeon up "$PWD"
```

Do not start a repository-specific replacement server. New agent sessions use
the configured shared services and discover their current tool catalog.

After a Clojure MCP change:

```bash
make mcp-test
make mcp-reload
```

`mcp-reload` reloads changed namespaces, synchronizes the live registry, and
publishes current contract hashes. It does not restart the shared JVM.

The server advertises `tools.listChanged=true` and emits
`notifications/tools/list_changed` after a contract change. Compatible MCP
clients issue a new `tools/list` request on the existing connection.

## The Codex schema-cache boundary

Server behavior becomes live immediately. Model-visible tool text is a client
concern. A running Codex session can retain the schema and description that it
already received for its current turn. Therefore:

- implementation-only changes require no agent restart;
- new agents reliably see the current schema;
- connected clients that honor `tools/list_changed` can refresh without a
  reconnect;
- if an existing Codex session rejects a newly added field from its cached
  schema, start one new session rather than restarting either shared service.

Do not describe this client cache as a server restart requirement.

## CLI and skill publication

MCP publication does not update `~/bin/clj-surgeon` or installed skills. When
CLI behavior, help, README-guided routing, or skill guidance changes, run:

```bash
make install
```

An already loaded skill remains in an agent's context. Future skill loads use
the installed update. Existing agents may need one short handoff when the new
capability changes the preferred route.

## Release proof

Before telling another agent that a fix is available:

1. Run the focused tests and the repository-required full gates.
2. Publish MCP changes with `make mcp-reload`.
3. Exercise one real `inspect_clojure` or `apply_clojure_changes` request
   against the shared server. Health alone is not proof.
4. Run `make install` when CLI or skill artifacts changed.
5. State whether the consumer can continue in place or needs one new agent
   session because its visible schema is stale.

The desired handoff is concrete:

```text
The shared MCP implementation is live and a real request passed. Continue in
the current session for behavior-only fixes. Start one new agent session only
if your cached schema refuses the newly documented field. Do not restart the
shared services or enter a recovery loop.
```
