# Publish Local MCP Changes

## Per-worktree dev instance

Use `make mcp-dev-start` when a branch must dogfood the Clojure source in its
own worktree. It starts a second, bounded-heap MCP instance on port `7889` by
default, with PID, readiness, log, nREPL port, and telemetry isolated under
`~/.local/state/clj-surgeon/dev-7889/`. The shared instance on port `7888`
remains running and untouched.

Run `make mcp-dev-status`, `make mcp-dev-reload`, and `make mcp-dev-stop` for
the branch instance. `make mcp-dev-register` registers its
`http://127.0.0.1:7889/mcp` endpoint as `clj-surgeon-dev` with Codex and in the
worktree-local ignored `.mcp.json`. Override `MCP_DEV_PORT` to run another
isolated branch instance; the branch dogfoods itself through that dev port.

clj-surgeon uses one shared local development stack. Coding agents do not
start a production MCP server or a server pair for each repository.

```text
clojure-lsp <-> cclsp http://127.0.0.1:7890/mcp
                         |
                         v
              clj-surgeon http://127.0.0.1:7888/mcp
```

The clj-surgeon JVM on port `7888` starts from this checkout:
`/Users/genekim/src.local/clj-surgeon/`. Its classpath contains this
checkout's `src` directory. Agents that use port `7888` therefore share the
same development server. There is no separate production clj-surgeon MCP
server on this laptop.

## Choose the publication action

| Change | Required action | Agent restart required? |
|---|---|---|
| Clojure implementation | `make mcp-reload` | No |
| MCP tool schema or description | `make mcp-reload` | Only if an existing client keeps an unusable cached schema |
| CLI or installed skill | `make install` | No, but existing CLI callers keep the old snapshot until installation |
| cclsp TypeScript | Save the file. The shared Bun watcher reloads it. | No |

`make mcp-reload` reloads the changed Clojure namespaces, synchronizes the
live tool registry, and publishes the current contract hashes. It does not
restart the server on port `7888`.

The server advertises `tools.listChanged=true` and sends
`notifications/tools/list_changed`. A compatible MCP client refreshes the
tool catalog on the existing connection. A Codex session can retain its
model-visible schema text for the current turn. Start a new Codex session only
when that cached text causes a request refusal.

## Publish and verify Clojure MCP changes

1. Run the focused tests for the changed behavior.
2. Run `make mcp-reload`.

   Expected: the command reports the synchronized live registry and current
   contract hashes.

3. Call `inspect_clojure` against one real workspace.

   Expected: the call returns `read_complete=true`. Treat this request as the
   functional readiness proof.

4. Run `make mcp-status` if the functional request fails.

   Do not treat a listening port as proof that the tool runtime is usable.

## Publish CLI and skill changes

1. Run the required focused and full verification gates.
2. Run `make install`.

   Expected: `~/bin/clj-surgeon` and the installed agent skills contain the
   verified working-tree snapshot.

3. Run the documented CLI invocation from outside this checkout when the
   change affects installation or command discovery.

## Prepare another repository

Run this command once from the target repository or pass its absolute path:

```bash
clj-surgeon up "$PWD"
```

The command joins the repository to the shared services. It must not launch a
repository-specific MCP server. MCP requests for a non-default repository must
include its canonical absolute `workspace_root`.

If the stack reports stale state or false-green readiness, run one recovery:

```bash
clj-surgeon recover "$PWD"
```

Continue only when recovery returns `:terminal-state :recovered`. If recovery
fails, run its redacted `:report-command` once and use its named fallback. Do
not loop or restart healthy shared services.

## Handoff rule

Another coding agent inherits an implementation fix after `make mcp-reload`
when it uses the shared `7888` endpoint. It inherits a CLI or skill fix only
after `make install`. If a tool schema changed, warn the agent that an existing
session can require a new session even though the shared server is already
current.
