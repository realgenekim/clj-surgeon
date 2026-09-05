# Primary six-pair aggregate and mechanism audit

All 24 scheduled arms are retained: six native/tool pairs within each model. No outcome-based exclusion or rerun was used. Independent receipt rechecks found no mismatches. This is a measurement audit, not source or shipping approval; the full gate remains pending.

| Model | Native median s | Tool median s | Median paired ratio | Descriptive paired bootstrap 95% interval | Median paired saving s | Frozen native 2-SD floor s |
|---|---:|---:|---:|---:|---:|---:|
| gpt-5.6-sol | 117.85 | 34.40 | 3.319× | 2.851–3.953× | 84.25 | 53.69 |
| gpt-6-astra | 56.00 | 45.10 | 1.240× | 1.048–1.366× | 11.05 | 8.91 |

Ratios always divide a model’s native arm by its own same-pair tool arm. Exact bootstrap enumeration uses all 6^6 = 46,656 ordered paired resamples, then linear 2.5/97.5 percentile endpoints of their median ratios. This is a descriptive small-n interval: six paired observations do not establish stable population coverage, independence over time, or a general causal effect. Pair order was alternated by the frozen schedule, not randomized here. Astra pair 2 favors native (0.988×); it remains included. No 10× claim is supported.

Both median paired savings exceed the same-model frozen calibration two-SD floor. Differences of route medians also exceed it: Sol 83.45 s versus 53.687 s; Astra 10.90 s versus 8.907 s. This frozen threshold comparison is not a hypothesis test. Ratio of medians differs from median paired ratio: Sol 3.426×; Astra 1.242×.

Primary wall is run.json wall_s, not adapter preparation-through-attestation wall, wrapper wall, slot waiting, or external acceptance duration. The watcher end timestamp has whole-second rounding, so printed tenths do not confer true 0.1-second resolution. Assistant-message records and outer caller actions do not identify model API round trips. MCP server elapsed is execution time, not full task wall or CPU attribution.

## Receipt and cold-start checks

All 24 pass independent acceptance (six structural/behavior/load checks), canonical/protected attestation, protected bytes and modes rechecked from fixture and frozen guard, original fixture HEAD, prompt hash, requested/resolved/session model and high effort binding, unique session IDs, clean watcher termination, and exact scheduled start order. All adapter, acceptance and combined intervals are clean after recalculation from original one-second load samples and boundary records using the frozen load>10 rule. This sampling does not rule out subsecond interference or hardware/network variance.

Every tool arm has a distinct server PID/birth tuple, correct project and pinned da7ba418cbe3e1de22efdd1471a0c295c0422d80 source attestation, matching ready/health byte hashes, ready before task start, and recorded stop with no survivors. These historical receipts establish distinct process startups; this audit cannot re-attest departed processes or claim cold filesystem/JIT dependency caches. No server was launched by this audit.

| Model | Median startup s | Median tool wall + own startup s | Median paired native/(tool+startup) |
|---|---:|---:|---:|
| gpt-5.6-sol | 9.540 | 43.940 | 2.720× |
| gpt-6-astra | 9.561 | 54.939 | 1.021× |

The startup-inclusive sensitivity adds each tool arm’s measured startup to that arm, not the group median to every arm. It excludes stop, external acceptance, orchestration gaps and scheduling waits. Astra’s warm advantage is nearly exhausted by a fresh server startup; these are separate estimands. Here, “warm” means server startup is excluded, not steady-state JIT/cache warmth: each arm used a fresh ready server and its first migration. A long-lived repeatedly used server was not this model cohort’s measured condition.

## Observed mechanism

All 24 arms actually ran bin/fan-test once successfully in-session (21 tests, 147 assertions, zero failures/errors). All final states independently loaded 100 namespaces. Five Sol tool arms (pairs 1, 2, 3, 5, 6) have no observed in-session all-namespace load command; only its pair 4 does. Every Astra arm and every Sol native arm executes a successful caller load. External oracle loads are not imputed as caller actions. Primary obligations require the suite actually run and the final state loadable; these five omissions are a caller-evidence difference, not retrospective failures of the future verified screen’s explicit two-proof-command rule.

Twelve tool migrations each committed 21 files/63 sites successfully; all report focused_test and kondo_delta not-requested, with no verify argument. No MCP refusal, repeated migration, or native-write fallback is observed. They are not verified-profile receipts. Sol tool usually stops after the suite and status; Astra tool typically reads runner/config material and then loads namespaces.

Astra native uses one direct Python rewrite batch per arm, preserving selected text by assertions; pairs 2, 3 and 6 include checks in the same outer action as rewriting. Sol native uses one successful 21-file apply_patch batch per arm: literal construction in pairs 1, 4, 5, 6; JavaScript generation in pairs 2, 3. Those latter two contain one and two failed patch attempts respectively before success. Both routes batch writes; native is not a per-file-write baseline. Sol native has three failed load attempts in total before later successful load checks. Astra pair 6 tool has a failed AGENTS filename discovery (exit 1), not a migration refusal or test failure.

| Model/route | Median outer actions | Postwrite runner/config read actions | Successful caller-load arms |
|---|---:|---:|---:|
| gpt-5.6-sol/native | 10.5 | 12 | 6/6 |
| gpt-5.6-sol/tool | 4 | 2 | 1/6 |
| gpt-6-astra/native | 5 | 1 | 6/6 |
| gpt-6-astra/tool | 6 | 10 | 6/6 |

MCP server elapsed: median 1.192 s, range 1.111–1.312 s. These short executions do not explain all task-wall gaps; caller preparation, output handling and subsequent checks are included in task wall but not isolated causally.

Read-action counts exclude filename-only discovery, status, and residue/diff checks. Astra native pair 5 reads a test runner after writing in the same outer action; pair 3 also makes an inline byte-preservation reread. Consequently zero separate read actions is not evidence of zero postwrite filesystem reads. Actual runtime transitions are mapped to unambiguous enclosing outer intervals; one outer action can contain multiple commands.

## Per-arm evidence

For every arm below, original paths are `/var/tmp/forge/astra-program/arms/ARM/{run.json,adapter-result.json,attest.json,guard.json,rollout.jsonl,watch.jsonl}` and `/var/tmp/forge/astra-program/receipts/ARM/{acceptance-result.json,acceptance.log,orchestration-result.json,load.jsonl,adapter-start.json,adapter-end.json,acceptance-start.json}`. Tool startup evidence is `/var/tmp/forge/astra-program/servers/ARM/{ready.json,ready.edn,healthz.json,plan.json,stopped.json}`. Structured summaries contain resolved exact paths and original SHA256 hashes. Rollout numbers below are 1-based completed-event lines, not assistant reasoning.

| Arm | Task wall s | Outer actions | Suite rollout line | Successful caller-load rollout line | MCP rollout line |
|---|---:|---:|---:|---|---|
| pair-1-astra-native | 55.3 | 5 | 40 | 41 | — |
| pair-1-astra-tool | 45.4 | 5 | 26 | 41 | 20 |
| pair-1-sol-native | 117.2 | 7 | 49 | 63 | — |
| pair-1-sol-tool | 35.2 | 4 | 32 | none observed | 23 |
| pair-2-astra-native | 49.7 | 4 | 36 | 36 | — |
| pair-2-astra-tool | 50.3 | 6 | 26 | 45 | 21 |
| pair-2-sol-native | 117.7 | 14 | 69 | 117 | — |
| pair-2-sol-tool | 39.0 | 4 | 32 | none observed | 23 |
| pair-3-astra-native | 60.1 | 5 | 43 | 43 | — |
| pair-3-astra-tool | 42.4 | 6 | 24 | 42 | 20 |
| pair-3-sol-native | 118.0 | 10 | 70 | 86 | — |
| pair-3-sol-tool | 31.5 | 3 | 32 | none observed | 23 |
| pair-4-astra-native | 58.6 | 5 | 42 | 41 | — |
| pair-4-astra-tool | 46.4 | 6 | 24 | 45 | 19 |
| pair-4-sol-native | 181.6 | 12 | 56 | 106 | — |
| pair-4-sol-tool | 54.9 | 7 | 32 | 62 | 23 |
| pair-5-astra-native | 56.7 | 5 | 41 | 41 | — |
| pair-5-astra-tool | 43.1 | 6 | 25 | 42 | 20 |
| pair-5-sol-native | 135.2 | 11 | 63 | 93 | — |
| pair-5-sol-tool | 32.5 | 4 | 32 | none observed | 23 |
| pair-6-astra-native | 49.6 | 4 | 35 | 36 | — |
| pair-6-astra-tool | 44.8 | 6 | 28 | 44 | 24 |
| pair-6-sol-native | 90.2 | 8 | 49 | 72 | — |
| pair-6-sol-tool | 33.6 | 4 | 32 | none observed | 23 |

Reproduction and detailed results: [aggregate.py](/var/tmp/forge/astra-program/primary-mechanism-audit-24/aggregate.py), [aggregate.json](aggregate.json), [safe-summary.json](mechanism.json), [build_safe_summary.py](/var/tmp/forge/astra-program/primary-mechanism-audit-24/build_safe_summary.py). Original four-pair snapshot is preserved unchanged in [primary-mechanism-audit.md](/var/tmp/forge/astra-program/primary-mechanism-audit.md) and `primary-mechanism-audit/`. Existing supplement_calls.py was reused; private reasoning text and assistant prose were neither used as evidence nor copied into this report. Raw receipts were not changed. No new model, JVM or BB execution occurred.
