# Astra helper program: first-hour usage study

<!-- agent-usage-window-end: 2026-09-05T05:55:58Z -->

Window: 2026-09-05 04:55:58–05:55:58 UTC; 2026-09-04 21:55:58–22:55:58 PDT.
This is preparation, design review and implementation, not a matched performance
cohort. Four Clojure-relevant sessions per provider appear in the receipt.

| Recognized client observation | Codex | Claude |
|---|---:|---:|
| Surgeon invocation attempts | 12 | 0 |
| Native-read route actions | 43 | 245 |
| Skill-loaded sessions | 3 | 0 |
| Skill-visible relevant sessions | 4 | 1 |

These are classifier observations, not complete behavior or success counts.
Inherited instructions, unrecognized wrappers and missing completed clocks limit
interpretation. In particular, zero recognized Claude calls does not establish
zero actual usage or prove the tools were available to every builder. Native
construction of new files and prose remains legitimate; read-route totals do not
by themselves show avoidable Clojure reads.

Independent service aggregation reports 21 inspect calls: 15 ok, 6 refused;
21 file reads and 80,316 source characters. Service wall totals 1.391 seconds,
median 47 ms, maximum 275 ms. Refusals: two batch owner selections, two invalid
requests, one missing source and one invalid source. The retained evolving-file
read and selector corrections explain concrete members of this set; do not infer
all six were malformed user intent. No mutation or server-start event was recorded.
Cclsp telemetry reports no events for this window; this is not a universal usage claim.

The Codex wrapper subset has 4.411 seconds of outer action wall. It includes
initialization, orchestration and sometimes batched native work; it is neither
MCP-only wall nor complete task wall. Do not add its 12 calls to the 21 service
calls. The original root wrapper subset separately measured ten calls at 1.165
seconds before its cutoff, including one refusal.

Dominant observed Codex sequence: skill load → bounded Surgeon/native read
batches → read/review → another bounded read. Claude phases predominantly alternate
native reads, Git and verification. These are different roles on different work,
so their counts cannot rank provider speed. Recorded Codex clock items aggregate
1,831.319 seconds of reasoning and 507.487 seconds of compaction across sessions;
concurrency and partial-turn coverage prevent treating these as disjoint shares
of one hour. No controlled complete-task result exists in this window. Root also
reread oversized resume notes after compaction: avoidable orientation work to
reduce, not evidence that Surgeon itself is slow.

Instrumentation correction: original client classification missed the registered
Python HTTP wrapper. Branch MCP/astra-mcp-wrapper-usage, commit bc9b26cf, adds
explicit opt-in registration bound to the inspected wrapper hash. It recognizes
supported literal call shapes, not arbitrary scripts. A field witness failed
before the repair. Review caught a misplaced metadata insertion that skipped
old self-test assertions; that intermediate green claim is withdrawn. Restored
assertions, an injected-invalid-status negative and the paved self-test pass.
V2 recollection uses identical bounds and unchanged service aggregation. Historical
wrapper bytes are not independently attested, and unresolved call shapes remain
unknown. This branch is published for review, not merged into the working trunk.

Counting authority: `/var/tmp/forge/astra-helper-program/hour-1-agent-usage-wrapper-corrected-v2.json`,
SHA 1ca3e47473c153dc3cf87a74f14f870e6a8d0f9c45968152453cd426f24b5081.
Both predecessor receipts remain, superseded rather than combined. Repair and
negative-test linkage: `usage-evidence/verdict-v2.json` under the same program root.
Privacy-safe receipt fields and bounded retained structural reads informed this
study; no transcript prose or customer source is reproduced here.

Product progress: the independent acceptance fixture is ready; planner and public
boundary are under construction. Independent lexical/import counterexamples showed gaps in the happy fixture.
After the window, boundary review also found an exception-flow gap: green mapper
tests cannot prove a safe transaction.
The smallest falsifiable next step is a compact public extraction that commits
only after the shared proof, plus a failed proof that demonstrably restores a
staged candidate. Only then run native calibration and matched whole-task trials.

Written by Astra at 2026-09-05T06:10:46.902565+00:00
