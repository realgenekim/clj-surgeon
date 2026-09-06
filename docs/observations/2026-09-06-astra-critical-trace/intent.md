# Astra process-span trace prototype

Goal: observe one manual task's full begin/end interval and named process/external
spans without recording raw argv, environment, source or command output in receipts.
Events append to JSONL even when filename ends.json. Every event binds task id,
host/boot clock identity, process.hrtime monotonic timestamp, UTC and recorder PID.
A short exclusive filesystem lock serializes validation+single append; concurrent
run commands execute outside that lock. begin uses exclusive create; no overwrite.

Declared dependencies must already have completed when a new span starts. Duplicate
IDs, malformed events, unknown dependencies, cycles, reversed intervals, unfinished
spans, wrong clock/task identity and events after task end refuse a complete report.
Process spans end after close and group cleanup, not merely after parent exit.
A failed exit or spawn error is a terminal recorded span, not missing evidence.
A run has a fixed60-second deadline; owned process group receives TERM then KILL
on timeout or if left alive after parent close. This is Linux/POSIX supervision,
not a hostile-process sandbox; escape into another session is not contained.

External mark-start/mark-end brackets are explicitly EXTERNAL BRACKET: their wall
includes commands, model delays and any intervening work. They are not edit-only
execution timings. Task begin/end exposes gaps around all spans. Union, sum and
uncovered intervals are observed; uncovered time is UNATTRIBUTED, never all model
thinking. Longest duration path belongs to a DECLARED dependency DAG, not a causal
critical-path claim or estimate of removable wall. Missing token/JVM/service markers
are UNKNOWN. No automatic provider instrumentation or whole-tool routing claims.

Validation before handoff: deterministic overlapping/sequential synthetic intervals,
malformed DAG/task/clock/open/duplicate cases, real failed/nonexistent processes,
concurrent append, external span label and full task report. No provider or JVM.
