# Astra: actual warm transition and Spark availability

Recorded 2026-09-06T09:30:10.545202+00:00

I personally invoked both frozen harnesses in an exclusive Astra timing window after Fable's explicit release. No shared service was changed. Both commands ended successfully and their owned processes were cleaned up.

| Observation | Result | What it establishes |
|---|---:|---|
| Actual source transition, reload, gate and witness | 865.773 ms | One closed six-namespace transition passed |
| Existing cold candidate gate and witness | 4,386.441 ms | Same proof counts, using two existing cold JVM commands |
| Warm startup, including baseline proof | 3,428.464 ms | Startup remains a real cost |
| Warm teardown | 466.971 ms | Startup + transition + teardown totals 4,761.209 ms |
| Spark availability/accounting probe | 3.906 s parent wall | Actual Spark/low completion and usable token accounting |

The original warm/cold component ratio is 5.07 against the two-JVM path. A later single-JVM comparator took 2.612 seconds, reducing the observed component ratio to 3.02; see the [stronger comparison](2026-09-06-astra-single-jvm-cold-comparison.md). It is neither a replicated speed estimate nor an editing-task multiplier. The one-shot path including teardown loses to the current cold pair, and also loses to the later single-JVM cold comparator. The under-100 ms target still fails. This result earns exploration of repeated real transitions, not a production release.

Warm and cold candidate proofs both passed the gate's two tests / five assertions and the witness's four tests / 24 assertions. Baseline proofs matched at two/five plus three/12. Five exact negative cases refused graph changes, macro changes, stale generation, a deleted called Var, and reuse after poisoning. The old/new generation chain and candidate hash match; the owned runtime was stopped with no remaining processes. This does not establish arbitrary hot reload or interruption during an in-flight transition.

Independent review found a receipt defect: reuse correctly refuses as poisoned but its receipt reports `poisoned=false`, confusing whether this request dispatched with existing runtime state. Frozen evidence is retained unchanged; a successor one-expression correction now has independent review and offline fail-first evidence (six tests passing), without replaying or modifying the original runtime evidence.

The one permitted Spark probe bound its actual rollout to `gpt-5.3-codex-spark`, low effort, CLI 0.153.3, read-only sandbox, never approvals, and a fresh empty workspace. It returned the fixed sentinel with no observed tool calls. Usage was 5,987 input, zero cached input, 38 output, including 27 reasoning tokens; total 6,025. These are the bound last cumulative snapshot, not summed snapshots. Subscription cost is unknown. Parent and child cleanup show no remaining processes. This establishes availability/accounting, not an empty advertised tool registry, production adapter suitability, code quality, or model speed ranking. No retry occurred.

Evidence: `/var/tmp/forge/astra-warm-transition-fx/results-01/`, `transition-01/stopped.json`, `independent-outcome.md`; `/var/tmp/forge/astra-spark-boundary-fx/attempt/result.json` and `parent-attempt/status.json`. Spark independent artifact review subsequently passed; its report is `independent-outcome.md` in the Spark evidence directory. This closes the bounded availability/accounting review only.
