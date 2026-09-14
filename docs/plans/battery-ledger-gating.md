# Battery evidence ownership

Build round 1, incident inb-16b396. Check-only packets currently modify two
tracked evidence files, requiring manual reverts and causing merge conflicts.

The battery emits a candidate receipt on every run, but appends it only with
`BATTERY_LEDGER_APPEND=1`. Freshness continues to read committed receipts with
the existing arithmetic. Namespace timings belong to seat state, resolved by
the shared state-root policy; the tracked timing file is an initial seed only.
The explicit `make census-regenerate` entrance regenerates the deftest census,
prints added/removed counts, and refuses removals before writing.

Owning design: [battery evidence](../intent/battery-ledger/battery-ledger-design.md).
No receipt-chain changes, freshness-policy changes, runtime reassignment, new
test namespaces, evidence commits, or performance admission are in scope.

Verification matrix: unset/empty/0/true/1 append environment; pass/fail entries;
existing/absent ledger; custom/XDG/default state roots; absent/existing state
with namespace and shard timings; census no-op/add/delete/rename; make gate
environment still refuses census regeneration. Witnesses run red first, then
focused namespaces, diff-impact, lint, intent audit, and one serial-width battery.
Retain execution evidence outside the checkout in `/var/tmp/forge/ledger-fx`.
