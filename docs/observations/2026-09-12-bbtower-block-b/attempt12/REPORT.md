# Attempt 12: PASS-WITH-REPAIR

Every coordinator bb lane child now receives `-Djava.io.tmpdir=<selected root>`.
Selection mirrors SELF_TEST_TMP: preserve nonblank TMPDIR; fall back to `/var/tmp`
when unset/blank or equal to/beneath `/tmp` or `/dev/shm`. The value stays one argv
element, including spaces. The coordinator has one bb launch site, now shared
with the witness. The stale private-temp comment now describes actual behavior.

Final source tip: `843c79104ddcd77f204190ee693747f05413fa8f`, on
`bb-rewrite-tower-local`, starting from `da247b05d4169b4704e60d081bbe158d096ac34b`.
The final commit containing this directory is evidence-only; the gate attests the
source tip above, not a claim that it ran on a future documentation commit.
No push, tag, install, shared-service operation or landing occurred.

## Red, green and repair

* Red `7abd2bb21dd4e0a44b4df12838ad067c760d5459`: behavior-preserving command
  extraction plus the real bb child property witness. Eight cases returned
  `/tmp`, producing 8 failures, 16 passing assertions and no errors (red.log).
  The payload only reads the property; it creates no temp files even when red.
* Green `7e63d53d2c004df2dd4a42f8c9871490c9afe385`: explicit property and
  fallback selection. Coordinator tests: 41 tests, 291 assertions, zero
  failures/errors (green.log). Lint: zero errors/warnings (lint.log).
* The one `make test-fast` invocation exited 2: its fast sum cleared the
  unchanged ceiling, but the new subprocess witness violated fast-lane purity
  and was missing from the census. These were this attempt's mistakes, not
  pre-existing failures (test-fast.log, fast-receipt.edn).
* Repair `843c79104ddcd77f204190ee693747f05413fa8f`: retain a pure argv matrix
  in fast; move the real-child witness to the existing battery namespace
  receipt-artifacts-boundary-test, which the alias gate executes; regenerate
  census with exactly two additions and no removals. No existing namespace's
  runtime, budget or membership changed; no witness was deleted or skipped.
  Focused checks passed: 68 tests / 799 assertions (repair-pure.log), plus
  1 test / 24 assertions in a cold JVM launching the bb children
  (repair-boundary.log). Final lint: zero errors/warnings (lint-repair.log).

The exact once-only test-fast command was **not green** and was not repeated.
Final prewarm re-executed the fast witnesses successfully at the repaired tip.
This verdict describes repaired-source acceptance, not first-attempt success.

## Final prewarm

One invocation, after the repair, with TMPDIR `/var/tmp/forge/bbtower-fx` and
JAVA_TOOL_OPTIONS `-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx`.
Run `a6d7952d-420e-4190-bb66-b050feb26336`: exit 0, `:state :passed`,
`:problems []`, `:landing? false`, `:wall-ms 391681`.
Full unabridged output: [gate.md](gate.md); machine receipt: gate-receipt.edn.

| Measurement | Result | Ceiling | Evidence |
| --- | ---: | ---: | --- |
| Once-only test-fast, fast cadence | 42,891 ms | 60,000 ms | test-fast.log:342 |
| Final MCP fast cadence | 44,420 ms | 60,000 ms | gate.log:479 |
| Final MCP integration cadence | 68,293 ms | 240,000 ms | gate.log:480 |
| Final bb runtime sum | 238,097 ms | 343,102 ms | gate.log:757 |
| Final bb runtime makespan | 87,700 ms | — | gate.log:757 |
| Alias suite battery cadence | 83,311 ms | 1,800,000 ms | gate.log:79 |
| Complete prewarm | 391,681 ms | — | gate-receipt.edn |

Alias: 182 tests / 3,639 assertions. MCP: 1,409 / 15,557. BB: 898 / 7,981.
Serial bb diagnostic: 829 / 7,281. All passed. Recovery, shell checks,
repository hygiene and intent audit also exited zero. The full transcript
preserves all emitted census/refusal/budget text; no new census result is
inferred from the mere presence of a test name.

## Temp and write audit

`tmp-before.txt` and `tmp-after.txt` are full `LC_ALL=C ls -1A /tmp` listings
around fast plus prewarm. `tmp-before-gate.txt` brackets prewarm alone.
Both final set differences are empty (`tmp-new-all.txt`, `tmp-new-gate.txt`):
**zero new entries, hence zero new clj-surgeon entries**. After fast alone,
one unattributed `tmp.*` entry appeared, and was gone by the final listing.
Endpoint listings cannot rule out files created and deleted between snapshots;
the subprocess witness independently proves the property used by bb children.
No pre-existing shared /tmp entry was removed by this agent.

[write-audit.md](write-audit.md) lists external writers, overrides and XDG
support, including the separate packet alias receipt failures and fixed fixture
roots. No audited external writer was changed. Durable artifact roots honour
XDG_STATE_HOME; analyzer locks, telemetry and legacy workspace state do not.
The packet's external publication failures need their own envelope/state-root
repair and are not repaired by selecting bb's temp directory.

## DOGFOOD, uncertainty and owed

Every source edit and its mechanism/refusal/repair is in
[commands.md](commands.md), along with reproduction commands. Native exact-form
patches followed the binding routing default; the resulting runner was exercised
by test-fast and the full final prewarm. No editing-speed claim is made.

Least sure: this is a static external-write inventory, not a syscall trace of
all conditional profiles or third-party cache writes. TMPDIR matching mirrors
the Makefile's literal policy; it does not add symlink or arbitrary-mount
validation. Existing gate prerequisite/root validation retains that role.

Disagreement/deviation: my initial witness placement broke two gate rules;
one repair corrected it. The user requested test-fast once and it was run once,
so its failed result remains visible even though final fast gate coverage is green.

Owed to Fable: file and own the separate external-state/write-envelope item,
review this branch, and decide subsequent packet acceptance/landing. A passing
prewarm deliberately omits battery-fresh; it is not a landing receipt. No
independent review, packet certification or full battery freshness is claimed.
