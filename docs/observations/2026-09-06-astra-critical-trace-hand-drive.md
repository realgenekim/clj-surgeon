# Astra: a traced real keeper, with measurement overhead charged

Gene's go90 block, 2026-09-06. Ledger `critical-trace-astra`; stop 22:45Z.
This is a process-span prototype and one real repository task, not a Surgeon
versus native task-speed experiment. Native reading/editing remained the route.

## Outcome

The keeper makes the previous executable-specimen example self-contained: local
archived fixtures, manifest-relative paths, fresh output directories and a relocated
offline verifier. Commit **43b99afb**, independently reviewed GO. All six copied
request/result files exactly match their retained originals. The historical timing
receipt is unchanged. No MCP service, provider or runtime product source changed.

The recorder captured a real failed byte-integrity check and its repair. I had
copied a receipt through a text patch, adding a final newline; ordinary file copies
preserved the exact bytes. JSON equality alone would have missed this. The lesson
from the prior block applies to my own work: preserve artifacts rather than
reconstruct them, and keep the failed attempt in the bill.

## Task wall: observed, not inferred

Task begin **21:24:42.918Z**, end **21:28:40.128Z** (14:24:42–14:28:40 PDT).
Monotonic duration **237.210 seconds** includes read, edits, verification, failed
integrity check, repair, independent review, commit and clean-tree check. Prototype
preparation started earlier at approximately 21:20Z; reporting continued afterward.
This one task reused context from the preceding block. It is not a fresh-caller
orientation test, and task elapsed wall was observed on the shared box.

| Quantity | Seconds | Interpretation |
|---|---:|---|
| Full task | 237.210 | Begin through committed proof and clean-tree check |
| Union of declared spans | 59.988 | No overlaps occurred in this keeper |
| Sum of declared spans | 59.988 | Equal here; overlap is separately tested |
| External review bracket | 57.837 | Contains review waiting and other root work; not pure reviewer compute |
| Uncovered wall | 177.222 | **Unattributed**, not automatically thinking or waste |
| Longest declared duration path | 58.909 | Omits inter-span gaps; **not a causal critical path or possible saving** |

The 68 events describe 15 spans, including external edit/review brackets. Process
events retain wrapper PID, child PID, observed spawn/exit/close, status and argv
hash. External brackets include transport and intervening work; their code zero
means the bracket closed, not that its edit was correct. Separate proof supplies
that evidence. One recorded process exited 1; its successful repair follows it.

Provider request/first-token/last-token, presence, runtime-ready, separate OS
fork/exec and descendant runtime readiness remain **unknown**. The wrapper observes
subprocess launch and completion, not a complete process tree. No command-name
regex labels cold JVM startup. The trace does not identify all of the 177 seconds;
its useful property is refusing to pretend otherwise.

## Controlled instrumentation overhead

After the keeper, five alternating-order pairs ran the exact same frozen
`verify-portable.js` command directly or under the trace wrapper. Each starts fresh
processes, relocates the example, runs ten positive renders and eight expected
refusals, checks three exact examples and removes its temporary copy. Both arms
returned the same success text in all five pairs. The historical receipt remained
unchanged. No failed timing attempt was discarded.

The first window, 21:29:05–21:29:11Z, was later disclosed by Fable as potentially
overlapping its warm-proof JVM. Those ten successful runs remain in `overhead/`
but are not the primary estimate (510.758/532.767 ms medians; +22.009 ms).
With Fable's lane explicitly closed and port 7961 confirmed without a listener,
the unchanged five pairs were repeated at **21:32:44–21:32:50Z**, under a fresh
quiet marker via `SLOT_OWNER=astra ~/bin/slot -t`. Per-run one-minute load was 2.05.
Results below are that clean repeat, retained in `overhead-clean/`. No source or
argv changed; no unsuccessful attempt was hidden. Filesystem/runtime caches were
not flushed. These are small local measurements, not an isolated-host benchmark
or a statistical/generalization claim.

| Pair | Direct ms | Traced command ms |
|---|---:|---:|
| 1 | 491.190 | 527.483 |
| 2 | 478.628 | 525.723 |
| 3 | 500.893 | 511.809 |
| 4 | 468.082 | 510.041 |
| 5 | 460.941 | 491.269 |
| Median | **478.628** | **511.809** |

Difference of medians **33.181 ms**; traced command takes **1.069x as long**.
Including begin/end/report processes gives a **601.237 ms** median full lifecycle.
The extra lifecycle work costs time too. Predictions of under 100 ms command-wrapper
overhead and under 2 seconds verification held on this specimen. Neither is a
speedup claim. One trace can contain many commands, amortizing begin/end/report,
but the effect on an agent's complete task remains unmeasured.

## Verification and boundaries

Nineteen recorder checks pass: synthetic overlap union versus sum, sequential
dependencies, unknown/open/cyclic dependencies, malformed identity/time/event
ordering, failed process, missing executable, concurrent append, external bracket
and refusal to overwrite a ledger. Actual root and builder smoke calls also pass.
The keeper's independent review confirmed path resolution, exact original fixture
bytes and preservation of its historical result. No production battery was needed
for these isolated research scripts and documentation.

Recording runtime SHA256:
`92f4bb8b56b7d14774a3ce5410f1ee9f800787573d5e2a11eeb1e2d3fea56bba`.
The fixed 60-second timeout was not executed in validation. Process-group cleanup
is not confinement of descendants that deliberately escape the group. The prototype
is Linux/local-filesystem scoped, not a general telemetry SDK or sandbox.
Its frozen analyzer also requires the recording host/boot clock identity when
reading a ledger: `report` refuses after a reboot or on another host. The retained
`keeper-report.json` remains readable anywhere. A future offline analyzer should
validate consistency within an archived clock domain while keeping cross-clock
appends forbidden; this limitation was found in review, not silently fixed after
the timed runs. Do not install this research wrapper as a production tracing API.

## What I would do next

Use this kind of trace on one expected task win before building scheduler or warm
proof infrastructure. Explicitly record when dependent work becomes ready and
when it is dispatched; record test-runtime readiness if startup is the hypothesis.
Keep the same known work and proof obligations available to the native comparator.
Do not convert a longest declared path into hours recoverable by a queue.

For Surgeon, the strongest demonstrated territory is still a compact structural
intent replacing many edits. This block improved the measuring instrument and
delivered a small publishing fix. It did **not** discover another whole-task wall
win. Instrumentation should stay cheap, optional and outside ordinary editing flows.

Local evidence: `/var/tmp/forge/astra-critical-trace-fx/keeper.jsonl`,
`keeper-report.json`, `selftest.log`; `/var/tmp/forge/astra-critical-overhead-clean/`
(primary) and `/var/tmp/forge/astra-critical-overhead-results/` (overlap disclosed).
The adjacent [trace snapshot](2026-09-06-astra-critical-trace/) preserves scripts,
preregistration and receipts. This is a bounded hand-drive, not a complete collector
study; no default study-window marker is advanced. Model token cost is unknown;
no external typist/provider experiment was run.
