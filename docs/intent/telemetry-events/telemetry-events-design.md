# Public-call event ledger

The [HLD](../../high-level-design.md) requires observable operation evidence.
The event ledger records completed public calls at the function boundary so
usage collection does not depend on discovering launcher-specific telemetry
directories. `mcp-telemetry/record-call!` reaches the annotated `ledger!` through
`emit!`; `ledger!` feeds `telemetry-events/record!`.
The default ledger is `~/.clj-surgeon/events.jsonl`; an explicit
`CLJ_SURGEON_EVENTS_FILE` override supports isolated runs.

This leaf registers the already implemented TELEMETRY-EVENTS-001 promise that
the September 7 prefix audit found dangling. It changes no telemetry behavior.
The [spec row](telemetry-events-specs.md) links the existing append entry point
and direct JSONL witness. Other existing privacy, size, concurrency, and failure
tests remain useful evidence but are not new promises introduced by this batch.
