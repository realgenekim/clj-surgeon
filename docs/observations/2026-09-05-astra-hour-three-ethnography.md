# Astra ethnography: third hour

<!-- agent-usage-window-end: 2026-09-05T01:05:00Z -->

Observation window: 2026-09-05 00:05–01:05 UTC, or September 4 17:05–18:05 PDT. Collected once using explicit boundaries and the program-owned server telemetry root. Receipt: /var/tmp/forge/astra-program/usage-hour-3.json. Status is ok; collection time is 2026-09-05T01:35:27.660881Z. No raw transcript inspection or collector change was used for this report.

| Provider | Sessions in window | Clojure-relevant sessions | Recognized client Surgeon calls | Native read actions | Native patch actions | Verification actions |
|---|---:|---:|---:|---:|---:|---:|
| Codex | 21 | 17 | 0 | 63 | 9 | 16 |
| Claude | 5 | 4 | 0 | 68 | 0 recorded | 19 |

These are provider populations, not a matched performance cohort or this program's team total. Session membership can overlap prior windows. Shell classification does not prove all reads or writes touched existing Clojure source. Skill loads were recognized in two Codex sessions and zero Claude sessions; visibility and loading remain separate from actual use.

The zero recognized client calls are a known coverage failure: this program's Python HTTP gateway appears as shell execution, not direct MCP events. The program-owned server root independently records 19 calls: 17 inspect and two writes, with 15 successes and four refusals. Its 26 events also contain seven server starts. Typed refusals were two batch-form-selection-failed and two invalid-mcp-request. Inspection requested 19 forms, eight outlines and one xray operation across batches. These server counts are incomplete because the pinned emitter omits alias_migration and uses the historical apply name for compact edits. Zero alias events is not zero migrations.

Recorded direct server wall totals 3.623 seconds (inspection 1.441, writes 2.182); median call 74 milliseconds, maximum 1.141 seconds. It cannot be divided into whole repair wall: these are service intervals, while planning, review, gate waiting and client gaps remain outside them. Codex completed event clocks record 763.278 seconds of model-reasoning items across the provider population, 102.443 seconds of compaction, and 71.200 seconds of shell items. Those are recorded item totals across sessions, not one elapsed task or a causal attribution of every gap. Claude lacks equivalent item clocks here.

Observed route phases include repeated native-read → verify → native-read and batched read/verify/git work. Because gateway operations are opaque in those phases, this report does not infer native fallback or adoption from them. The independent dogfood gateway ledger and controlled primary cohort retain actual call evidence; they must not be silently added to provider totals.

The useful result is diagnostic: service execution is short, while complete decisions, verification and instrumentation occupy the program. Fresh caller capability and the primary controlled efficiency result exist separately; this hourly aggregate earns no new speed or shipping claim. The smallest falsifiable improvement is dual-sided coverage: one successful and one refused alias migration should each appear exactly once under their public operation name in a fresh emitter epoch and its client transcript. Fable's telemetry branch remains gated; historical receipts will not be rewritten as though the new emitter existed earlier.

The study-agent-usage self-test passed during the root ActiveProcessorCount8 make test run. That invocation later failed in the unrelated Claude timeout harness; the localized fix and remaining tail checks passed separately. No redundant self-test or global history recollection was run for this unchanged collector.
