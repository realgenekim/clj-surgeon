---
parent: hot-verification-design
prefix: MCP-OP-HOTVER
status: "implemented 2026-09-06; inb-adcc9e"
---

# Hot-Verification Response-Stream Specifications

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

- [ ] **MCP-OP-HOTVER-001**: When clj-surgeon reads the application nREPL's
  responses for one hot-verification evaluation, it shall stop reading at the
  first response for that message whose status contains ANY terminal status —
  `done`, `error`, `eval-error`, or `interrupted` — and shall report from the
  responses read up to and including it. It shall never wait for the
  configured `:timeout-ms` ceiling to elapse before returning a result the
  server has already finished sending. A response stream ended by an error
  status that the server never follows with `done` shall produce the ordinary
  typed `hot-verification-failed` result, carrying the server's output, at the
  moment that status arrives.

- [ ] **MCP-OP-HOTVER-002**: `:timeout-ms` shall be a true ceiling on the whole
  read, not a per-response timeout that each arriving response resets. When no
  terminal status arrives before that ceiling, clj-surgeon shall return a typed
  `hot-verification-timeout` failure naming the ceiling. When the transport
  closes before any terminal status arrives, it shall return a typed
  `hot-verification-transport-closed` failure rather than blocking until the
  ceiling. Neither refusal shall claim a verification result.
