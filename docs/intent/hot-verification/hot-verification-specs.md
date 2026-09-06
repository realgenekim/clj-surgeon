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

- [x] **MCP-OP-HOTVER-001**: When clj-surgeon reads the application nREPL's
  responses for one hot-verification evaluation, it shall stop reading at the
  first response for that message whose status contains ANY terminal status —
  `done`, `error`, `eval-error`, or `interrupted` — and shall report from the
  responses read up to and including it. It shall never wait for the
  configured `:timeout-ms` ceiling to elapse before returning a result the
  server has already finished sending. A response stream ended by an error
  status that the server never follows with `done` shall produce the ordinary
  typed `hot-verification-failed` result, carrying the server's output, at the
  moment that status arrives. `interrupted` is terminal AND a failure: a
  verification shall be reported successful only when a `done` status for that
  message arrives with no `error`, `eval-error`, `timeout`, or `interrupted`
  status among the responses read, so a value that arrived before an interrupt
  can never be read as a pass.

- [x] **MCP-OP-HOTVER-002**: `:timeout-ms` shall be a true ceiling on the whole
  read, not a per-response timeout that each arriving response resets. When no
  terminal status arrives before that ceiling, clj-surgeon shall return a typed
  `hot-verification-timeout` failure naming the ceiling. When the transport
  closes before any terminal status arrives, it shall return a typed
  `hot-verification-transport-closed` failure rather than blocking until the
  ceiling. The ceiling bounds the READ, and is not a hard wall-time bound on
  `verify!`: the time remaining is rounded UP to whole milliseconds so a read
  can never expire before the deadline, which permits a sub-millisecond
  rounding overshoot, and the thread must still be scheduled to observe the
  expiry. A caller may rely on "not appreciably longer than the ceiling", never
  on "no longer than the ceiling by any margin". Failing to CONNECT to the
  application nREPL is a distinct typed
  `hot-verification-connection-failed`, never reported as a closure during an
  established read. Neither refusal shall claim a verification result, and each
  shall carry the bounded output read before it so a caller has a diagnostic.
