# Captain's log — Astra four-hour performance program

## Astra — 2026-09-04T22:07:46Z — Command, scope, and first falsifier

Gene appointed Astra to lead the next four hours, with Fable continuing in parallel. Gene then directed: use Surgeon extensively, observe complete wall time ethnographically, seek 10×+ gains from Clojure's homoiconic structure, revisit previously ceded squares where useful, and teach him the findings. Public `main` remains frozen.

This log is owned by Astra on branch `MCP/astra-performance-2026-09-04`, isolated checkout `/var/tmp/forge/astra-program/repo`, based on `5b531d3b709b64fcae0dfcc9398942fe795da145`. Fable owns the existing checkout and build/review/landing lanes. Durable coordination: `/var/tmp/forge/astra-fable-coordination.md` and `/var/tmp/forge/fable-to-astra.md`; tmux panes `forge-anvil:1.0` and `forge-anvil:0.0`.

Fable has accepted disjoint roots and reserved ports 8300–8339 for Astra. Timed arms wait for Fable's T6/T6b to finish, then use at most two model arms and one JVM suite from Astra; initial plan is one model arm at a time, interleaved, launch load below 10. Existing foreign JVMs mean this is not the exclusive-box E-SCALE-WALL experiment. Shared-load results require explicit uncertainty and subsequent isolated confirmation.

First apparatus finding: the reviewed `bench/anvil-arms/run-arm.sh` accepted `--model`, but the Sol driver invoked a launcher hardcoded to `gpt-5.6-sol`; the requested model could therefore change the attestation without changing the subject. Fable now reports launcher support for CODEX_MODEL/CODEX_BIN. Astra's new adapter will verify the actual rollout model, pin the same 0.153.3 client for both models, and reuse the existing watcher. A model comparison cannot be founded on the requested model string alone.

First scientific correction: the old fanout prompt protects unrelated old-name literals/locals in section 2, then says no old name may remain anywhere in section 3. The new cohort will resolve this contradiction identically for both arms. It is a fresh experiment, not a byte-identical historical replication.

Current judgment, not a new measurement: compiler-owned discovery and exact whole-task transformations remain the best 10× candidates. A known-site atomic native patch is a useful control; stronger models may change refusal and scripting behavior in either direction. Source equality, protected syntax, runtime acceptance, complete wall, refusal recovery, and free-choice usage are separate observations. The goal is not to make every action a Surgeon call; Gene's dogfooding request will expose where that helps or adds cost.

Independent ethnography will use the repository's study-agent-usage collector and watcher records, not driver self-counts. No retrospective inference that all unattributed gaps were model reasoning. Findings and negative results carry Astra attribution and the receipt path.

## Astra — 2026-09-04T22:17:45.816187+00:00 — The 110× spark was real; its denominator matters

Gene asked whether the remembered 53-site/100× refactor was real. The original September 2 19:19Z receipt records nine forms, seven internal sites, sixteen external sites across three caller files: 1.3 seconds of hand-driven tool execution versus native agents reaching the move in 141 and 152 seconds. It explicitly distinguishes approximately 110× mechanical execution, approximately 4× at the agent step, and much smaller whole-task gain with the verification/report tail intact. The nearby 53 is recorded as churn, not a confirmed site count. The later magic-moments retelling mentions whitespace/docstring-wrapping differences against reference, whereas the original summary says byte-identical; exact byte fidelity should be rechecked from frozen artifacts before repeating that stronger claim. Sources: 2026-09-02-captains-log-the-big-aha-and-reset.md, 19:19Z entry; 2026-09-03-magic-moments-tweezer-watcher-anvil.md, item 9.

Extrapolation: one semantic decision can compile into all its mechanical consequences—caller rewiring, requires/imports, visibility, and exact source movement. This is stronger than faster patch application. Sustained 10× task gains require that those consequences dominate the native task and that discovery, invocation, verification and recovery do not consume the savings. That is the new program's falsifiable hypothesis, not an already demonstrated universal result.

Own-work Surgeon dogfood: batched outlines of two oracle/generator files took 90.96 ms observed RPC wall; six named forms across those files took 118.18 ms. Both returned read_complete=true with exact source retained in structuredContent. Receipts: /var/tmp/forge/astra-program/inspect-oracle-result.json and inspect-oracle-forms-result.json. Session has no registered Surgeon tools, so a minimal read-only HTTP MCP adapter used Fable-authorized port 8171; this shell-carried entrance is an instrumentation/visibility limitation, not a native registered MCP adoption claim. No comparative speed claim from these calls.

Oracle hand-drive: canonical 21-file migration passed all six checks, including 100 namespace loads, 21 tests/147 assertions, and 106 preserved regions. An unchanged clone failed file-set/form-equality/residue while its same 147 assertions still passed. Thus the structural acceptance catches task non-completion that the behavioral suite alone does not. Negative receipt: /var/tmp/forge/astra-program/oracle-check/negative.log.

Astra client handshake completed: actual session model gpt-6-astra, watcher bound to announced session 01a06e7e-c302-7d72-a11d-4a4aec838c44, no source changes. This was an untimed pwd-only smoke, excluded from performance comparisons. Sol same-client smoke follows.

## Astra — 2026-09-04T22:32:50.627663+00:00 — Frozen comparison; actual capacity is the gate

Both requested models were independently observed in successful untimed client smoke sessions. The common-client fanout protocol and adapter were frozen at 22:26:34Z before any measured migration model arm (FROZEN.json). A dedicated detached server-source worktree now pins 5b531d3b; changing journal HEAD cannot silently change server identity.

Warm alias_migration hand-drive changed 63 sites in 21 files, observed HTTP call wall 1.309879 seconds (server 1.22098 seconds), and the independent six-part oracle passed. Its built-in kondo/focused-test fields said not-requested; external acceptance supplies those separate completion checks. This is not a comparative model-task result.

The 22:16–22:36 quiet window did not produce reliably quiet capacity: load briefly fell below 8 then rose above 13 as multiple tmp-leak test JVMs appeared. No timed model migration arm was launched above threshold. Fable has been asked to identify their ownership, stop new competing suites, and prioritize the live discovery experiment plus essential safety fixes. Existing work is preserved.

Ethnographic correction: the apparent native apply_patch samples in the parent session were real mixed wrappers that created request/helper files before issuing HTTP MCP calls. They are not false-positive native-patch classification and their outer durations are not patch-kernel latencies. Service telemetry correctly captured two inspect calls; generic script-carried RPCs cannot safely be inferred as first-class tool calls from filename matching. Keep service and session meters separate and correlate rather than double count.
