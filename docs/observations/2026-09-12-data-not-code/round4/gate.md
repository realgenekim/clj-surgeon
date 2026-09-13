# Gate excerpts

Verbatim lines; complete logs and structured receipts are retained alongside this file.

## Standalone fast

```text
test-isolation: 0 violations across 94 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 29682 ms over 8 lane(s); serial-equivalent 41690 ms; skipped 0
```

## First prewarm

```text
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 12702}
the entrance's refusal enumeration changed size: 165 kinds
test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 91385 ms over 2 lane(s); serial-equivalent 82783 ms; skipped 0
battery-parallel: makespan 101705 ms over 8 lane(s); serial-equivalent 105438 ms; skipped 0
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 91011 ms over 8 lane(s); serial-equivalent 251490 ms; skipped 0
gate-refused: gate-refused: runtime pool failed {:shell-exit 2, :suites [{:suite "alias", :state :failed, :problems ["lane 1 exited 3 (log target/gate-prewarm/2fa9def9-13e0-4d48-b49b-2dcffc3880ee/alias/lane-1.out)"]} {:suite "mcp", :state :failed, :problems []} {:suite "bb", :state :passed, :problems []}]}
```

## First alias census failure

```text
the entrance's refusal enumeration changed size: 165 kinds
```

## Final prewarm

```text
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 11222}
test-isolation: 0 violations across 2 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 91759 ms over 2 lane(s); serial-equivalent 83769 ms; skipped 0
test-isolation: 0 violations across 101 namespace(s) (TEST-ISO-002/003/004/005/007/010)
battery-parallel: makespan 97614 ms over 8 lane(s); serial-equivalent 120742 ms; skipped 0
bb-isolation: existing per-process temp leak contract; no JVM probe claim
battery-parallel: makespan 92864 ms over 8 lane(s); serial-equivalent 251566 ms; skipped 0
kernel warning check: 2 namespace(s), 0 warning(s)
gate-stage: {:target "alias-migration-test", :exit 0, :wall-ms 91759}
gate-stage: {:target "mcp-test", :exit 0, :wall-ms 97614}
gate-stage: {:target "test-bb", :exit 0, :wall-ms 92864}
gate-stage: {:target "test-bb-diagnostic", :exit 0, :wall-ms 240305}
gate-stage: {:target "repository-hygiene", :exit 0, :wall-ms 2090}
gate-stage: {:target "intent-audit", :exit 0, :wall-ms 2210}
```
