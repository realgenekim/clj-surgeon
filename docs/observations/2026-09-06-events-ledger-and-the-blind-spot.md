# The events ledger, and the blind spot measured (2026-09-06, 01:4x–01:5xZ)

Gene, verbatim: "I think telemetry / watching is in wrong place; I think it should be in the MCP fns, and written as JSONL file someplace? Reduces need for watches -- it's a side effect of the fns that are doing the work." and the path: `~/.clj-surgeon/events.jsonl`.

## What was built (fable/typist-real-repo, landing after fence + gates)
Inside the single funnel every public tool already passes through (`mcp-telemetry/emit!`, on `:tool.call`, BEFORE the off/nil guard so a server started with telemetry off still lands in the ledger — exactly the call that went missing tonight): append one JSON line to `~/.clj-surgeon/events.jsonl` (dir 700, file 600, env override `CLJ_SURGEON_EVENTS_FILE`, O_APPEND with one write per line, lines under 4 KB with free text truncated at 1 KB). Fields: ts, seat, pid, kind, tool, ok, error_type, wall_ms, mission_id (null until the ledger phase carries one). A write failure never fails the tool call; it is counted and reported in the next line as telemetry_dropped. Collector: `--events` prints count-first `events: N (seats, pids, dropped)` then per-tool ok/refused and top refusal reasons; `make study-agent-events`. Nine witnesses (34 assertions): one line per call, valid JSON, fields present, 8 threads × 50 lines = 400 intact, oversize truncated, unwritable path → call succeeds and the drop is counted. Not yet witnessed: line atomicity ACROSS processes (needs a battery-lane test spawning JVMs).

## Tonight's blind spot, measured once (read-only union of every telemetry root under /var/tmp/forge/*-fx and the seat default, 2026-09-01..07)
```
roots scanned: 43 (present 43, missing 0)
union: 421 MCP tool calls, 525 events, 102 server starts, ok 312 / refused 109
```
| root | calls | ok | refused |
|---|---|---|---|
| the seat default (the only root the hourly watch read) | 315 | 218 | 97 |
| astra-program handdrive server | 58 | 48 | 10 |
| astra-program repair-wire server | 26 | 26 | 0 |
| astra-program binding-wire-round2 | 7 | 7 | 0 |
| astra-fair2 preflight-v2 | 2 | 1 | 1 |
| 12 × astra-fair2 pair servers + gate11-review | 1 each | | |
| 24 further roots | server start only | | |

**The hourly watch saw 315 of 421 calls: 106 public calls, 25% of the night, were invisible.** Tools across the union: inspect_clojure 299, admit_clojure_patch 60, apply_clojure_changes 46, helper_extraction 16; 275 file reads, 1,205,475 source characters returned. Top refusals: verification-failed 14, no-clojure-files 11, invalid-mcp-request 10, verification-incomplete 9, invalid-grep-pattern 7. Every hourly "unchanged since 22:56Z" line tonight was a collector artifact, not a fact about use.

Ratchet: the ledger makes the class impossible (every process, every seat, one file); the count-first first line makes an absent ledger read as "absent", never as zero use.
