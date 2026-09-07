---
parent: telemetry-events-design
prefix: TELEMETRY-EVENTS
---

# Public-call event ledger requirement

- [x] **TELEMETRY-EVENTS-001**: While the event ledger is writable, when a completed public MCP call is recorded, the ledger shall append one complete JSON line containing the call's timestamp, seat, process, kind, tool, outcome, error type, wall time, and policy-projected mission identity.

Implementation: `clj-surgeon.mcp-telemetry/ledger!` (via `emit!`) and
`clj-surgeon.telemetry-events/record!`.
Direct witness: `one-call-appends-one-valid-json-line-with-every-field` in
`test/clj_surgeon/telemetry_events_test.clj`. Public-call wiring is also exercised
by `a-tool-call-writes-the-ledger-even-with-per-server-telemetry-off`.

Misreadings: a launcher-specific log is sufficient; a prose identifier counts
as a linked annotation; copying an unprojected mission identity is acceptable.
Boundaries: successful/refused call, absent mission identity, one complete line
per recording, and per-server telemetry disabled. The writable precondition
does not claim delivery on an unwritable filesystem.
