# Where the wall goes in REAL coding sessions — CORRECTED measurement-only pass (21:10Z; scripts /var/tmp/forge/ethno-real-fx/v2/analyze3.py, extract_endturn.py)

> History: the first pass (same sources) was WITHDRAWN at 21:1xZ on Astra's method red-line (bb counted as JVM; instruction-containing gaps labelled idle; remaining gaps labelled generation; overlapping durations summed without union; loose failure detection). This corrected pass separates JVM-launching commands from bb, splits gaps at the first inbound timestamp into before/after and otherwise labels them UNATTRIBUTED, unions intervals (max-end) and prints the naive sum beside it, takes failures only from structured exit codes/errors, and makes NO causal categories and NO mechanism ranking. Two corrections the fix produced: union == naive in all four sessions (0 s overlap); Astra's '42 JVM commands' is 17 JVM-launching (10 s) + 15 bb (3 s); the 1.9 h builder's 100 becomes 44 JVM-launching (1,485 s) + 30 bb (2,100 s) — bb, not the JVM, was the larger cost there.

# Real-session wall-clock ethnography — corrected pass (v2)

Sources (unchanged, read-only): Astra codex rollout
`/home/forge/.codex/sessions/2026/09/04/rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`
restricted to events 2026-09-06T07:00Z–20:40Z; three largest Claude subagent transcripts under
`.../b623492c-.../subagents/agent-*.jsonl` (the `tasks/*.output` entries are symlinks to these;
the largest literal `b*.output` files are bash outputs, not transcripts). Scripts: `v2/analyze3.py`,
`v2/extract_endturn.py`. Every number is a millisecond event timestamp or an event count.

## Classifier rules applied in this pass
- **JVM-launching** = command text matches `java`, `clojure -M/-A/-X/-P` (or bare `clojure`), `clj -M/...`, `lein`.
  **`bb` counted separately** and NOT as a JVM launch. These are *command-text matches*: they show a command was
  issued, not that a JVM process started, and nothing here measures cold vs warm startup.
- **Gaps** are computed between merged (union) busy intervals. A gap containing an inbound message is split at the
  first inbound timestamp into *before* and *after*; neither half is called idle. The label
  "idle awaiting coordinator" is not used anywhere in this pass.
- **Remaining gaps** are "unattributed gap (no tool, no message)". No gap is attributed to model inference:
  no inference-timing field exists in either source.
- **Durations** are interval-unioned (max-end merge) before summing; naive sums are printed beside them.
  **Measured result: union == naive in all four sessions (0 s of overlap).** No call overlapped another.
- **Failures** come only from a structured field: Claude `tool_result.is_error`, or a parsed `"exit_code": N` with
  N≠0 in codex output. A call following a failure is a **retry candidate**, not a verified repair.

## S1 — Astra (codex), 2026-09-06T07:00:42Z → 20:12:10Z
wall **47,488 s (13.19 h)** · calls **1,182** · inbound messages **431** · turn-ending (`task_complete`) events **52**

| command class | union s | naive sum s | calls |
|---|---|---|---|
| JVM-launching | 10 | 10 | 17 |
| bb | 3 | 3 | 15 |
| other shell | 1,528 | 1,528 | 560 |
| mutating writes | 62 | 62 | 83 |
| reads | 168 | 168 | 507 |
| **all (union)** | **1,771** | 1,771 | 1,182 |

Gaps: unattributed (no tool, no message) **22,743 s**; gap after instruction **16,644 s**; gap before instruction **6,329 s**.
Failures (structured) **41**; retry candidates **41**.

Top-5 gaps:
| s | class | inbound / turn-end in gap | preceded by | followed by |
|---|---|---|---|---|
| 6,367 | before instruction | 2 / 1 (turn-end after the first inbound) | `followup_task` → `typist_boundary_audit` | `exec` python3 heredoc |
| 1,266 | before instruction | 3 / 3 (turn-end precedes first inbound) | `exec` python3 heredoc | `exec tail -n 38 /var/tmp/forge/fable-to-astra.md` |
| 1,148 | before instruction | 4 / 4 (turn-end first) | `exec` python3 heredoc | `exec` python3 heredoc |
| 1,093 | before instruction | 3 / 3 (turn-end first) | `exec` python3 heredoc | `exec cat /var/tmp/forge/plate-fanout-section.md` |
| 552 | before instruction | 2 / 2 (turn-end first) | `exec` read of `fable-to-astra.md` | `exec Promise.allSettled([...cat...])` |

## S2 — builder `ab3b8d03` (Claude), 2026-09-04T07:16:21Z → 08:38:04Z
wall **4,903 s (1.36 h)** · calls **845** · inbound messages **1** · turn-ending assistant messages **20**

| command class | union s | naive sum s | calls |
|---|---|---|---|
| JVM-launching | 944 | 944 | 33 |
| bb | 0 | 0 | 0 |
| other shell | 122 | 122 | 13 |
| mutating writes | 5 | 5 | 18 |
| reads | 140 | 140 | 781 |
| **all (union)** | **1,210** | 1,210 | 845 |

Gaps: unattributed (no tool, no message) **3,692 s**. No gap contained an inbound message.
Failures **4**; retry candidates **4**.

Top-5 gaps (all "no instruction in gap"): 99 s (after an `export …` Bash, before an `rg`), 88 s (after a `Read`
of a task output file, before a `python3` heredoc), 82 s (after an `rg`, before a `sed -n`), 71 s (after a
`grep` of a gate log, before a `python3` heredoc; one turn-ending message inside), 38 s (after a `grep -A12`,
before a `python3` heredoc).

## S3 — builder `a1111fae` (Claude), 2026-09-03T22:08:27Z → 2026-09-04T00:01:38Z
wall **6,791 s (1.89 h)** · calls **435** · inbound messages **18** · turn-ending assistant messages **53**

| command class | union s | naive sum s | calls |
|---|---|---|---|
| JVM-launching | 1,485 | 1,485 | 44 |
| bb | 2,100 | 2,100 | 30 |
| other shell | 16 | 16 | 5 |
| mutating writes | 3 | 3 | 26 |
| reads | 676 | 676 | 330 |
| **all (union)** | **4,280** | 4,280 | 435 |

Gaps: unattributed (no tool, no message) **2,469 s**; gap before instruction **31 s**; gap after instruction **11 s**.
Failures **3**; retry candidates **3**.

Top-5 gaps: 77 s (after a `nohup` launch, before a shell var assignment), 57 s (after a `CP=$(clojure …)`
classpath command, before a `python3` heredoc), 42 s (before instruction; after `git status`, before an `echo`),
38 s (after a `suite-run` invocation, before a `sed -n`), 35 s (after a timed `suite-run`, before a `mkdir`).

## S4 — builder `a8ed0d89` (Claude), 2026-09-05T05:03:34Z → 10:00:12Z
wall **17,798 s (4.94 h)** · calls **304** · inbound messages **17** · turn-ending assistant messages **73**

| command class | union s | naive sum s | calls |
|---|---|---|---|
| JVM-launching | 1,458 | 1,458 | 67 |
| bb | 1 | 1 | 20 |
| other shell | 70 | 70 | 29 |
| mutating writes | 1,166 | 1,166 | 47 |
| reads | 240 | 240 | 141 |
| **all (union)** | **2,936** | 2,936 | 304 |

Gaps: gap before instruction **11,690 s**; unattributed (no tool, no message) **2,838 s**; gap after instruction **335 s**.
Failures **3**; retry candidates **3**.

Top-5 gaps (each contains exactly 1 inbound message, and in each the agent's last event before that message was a
turn-ending assistant text message with no tool call after it):
| s | preceded by | followed by |
|---|---|---|
| 2,364 | `grep -n -B4 -A22` in `clj-surgeon-helperimpl` | `grep -n` in the same worktree |
| 1,957 | `rm …` in the same worktree | `wc -l docs/observations/2026-09-05-…` |
| 1,331 | `TaskStop {"task_id":"bzdwa2igu"}` | `grep -n` |
| 1,144 | `rm …` | `grep -n` |
| 993 | `rm …` | `grep -n` |

## What these measurements can and cannot say

They can say, from events alone: how long each session ran end to end; how many tool calls and inbound messages
occurred; exactly how many seconds elapsed inside tool calls, by command-text class, with the overlap check run
and returning zero; how many seconds fell outside any tool call, and whether an inbound message landed inside
each such stretch; which command preceded and followed every large gap; and how many calls reported a structured
failure. They **cannot** say where the wall actually went. The largest quantity in three of the four sessions is
time in which the transcript records nothing at all — no process, no message, no timing field — and this pass
deliberately leaves it unattributed. Nothing here distinguishes model inference from queueing, rate-limiting,
harness overhead, a human reading, or a delegated agent still running; the "gap before instruction" figure counts
wall between the last recorded call and the next inbound message, which is a boundary, not a cause. Likewise the
command classes are text matches: they do not prove a JVM started, do not measure process startup, and do not
show whether anything was reused warm. Nothing in this data supports ranking remedies.

A paired real-task critical-path study would need: (1) **process-lifecycle markers** — per command, a fork/exec
timestamp, PID, exit code, and JVM-specific start/first-eval/finish marks, so JVM startup is separated from work
and warm reuse is observable; (2) **inference timing** — per model turn, request-sent, first-token, last-token,
and any queue/retry/rate-limit interval, so the unattributed gap is decomposed instead of labelled; (3)
**user- and coordinator-presence markers** — when a human or coordinator was actually attending, when a
dispatch was available to be sent, and when a delegated agent started and finished, so "waiting for work" is
distinguishable from "working"; plus (4) an explicit request→result correlation id on every call so overlapping
and backgrounded work is attributable rather than inferred from adjacency. Two runs of the *same* task, one
instrumented per arm, would then let the critical path be computed rather than partitioned by heuristic label.
