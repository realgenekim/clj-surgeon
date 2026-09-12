# Arm N: retained results and attempt 2 audit

**Verdict: blocked for a conformant complete-wall comparison.** All 26 assigned N results already existed at the start of this attempt. The ledger is now complete; no caller, gate, or delivery probe was re-run. Recorded outcomes are reported below without treating apparatus failures as model-quality evidence.

The handoff said the batch died after nine runs. Retained `queue-N.log` instead ends with `run-arm: done 2026-09-12T03:49:38Z launched=26`; all 26 planned IDs have result and rollout receipts. The original nine ledger rows are preserved byte for byte, and 17 retained rows were appended. `launched=26` counts queue submissions, not new callers: the already-scored T-03 Astra result retains its 03:18:09 launch and was skipped by run-one. See [inventory](runs-N/attempt-2/inventory.json), [prior ledger](runs-N/attempt-2/ledger-before.tsv), and [receipt hashes](runs-N/attempt-2/retained-sha256.json). Every ledger row has `fixture=false`.

Acceptance follows the retained report.py rule: caller completed, oracle passed, gate green relative to its frozen baseline. Independent acceptance of this build remains external. Each model keeps all 13 assigned runs in its denominator.

| Model | Oracle pass | Finished | Accepted | Patch final | No write |
|---|---:|---:|---:|---:|---:|
| gpt-6-astra | 9/13 | 12/13 | 1/13 | 12 | 1 |
| gpt-5.6-sol | 12/13 | 13/13 | 1/13 | 13 | 0 |

| Model | Forecast | Bet | Recorded | Held/missed |
|---|---|---:|---:|---|
| gpt-6-astra | Surgeon final | 0/13 | 0/13 | HELD |
| gpt-6-astra | Program final | 4/13 | 0/13 | MISSED |
| gpt-6-astra | Accepted | 8/13 | 1/13 | MISSED |
| gpt-5.6-sol | Surgeon final | 0/13 | 0/13 | HELD |
| gpt-5.6-sol | Program final | 0/13 | 0/13 | HELD |
| gpt-5.6-sol | Accepted | 9/13 | 1/13 | MISSED |

These are descriptive scores under the delivered treatment. All 26 mechanism receipts record zero Surgeon calls and zero program-final edits. The no-write Astra I-04 run failed with `Selected model is at capacity. Please try a different model.` after 2.38 seconds; this is a delivery/caller failure, retained in the denominator. Classifier labels are retained, not a new blinded authorship audit.

| Model | N accepted caller median (s; n=1) | M accepted caller median (s) | M/N caller ratio | Cost bet | Descriptive point score |
|---|---:|---:|---:|---:|---|
| gpt-6-astra | 95.93 | 89.51 | 0.9331 | 1.30 | MISSED |
| gpt-5.6-sol | 111.03 | 191.12 | 1.7213 | 1.25 | MISSED |

The prior report calls caller request-to-exit time “wall” and records gate wall separately. Using that published convention gives 0.9331 Astra and 1.7213 Sol, missing both point forecasts (no post-hoc tolerance band). **The literal complete-verification-wall cost bets remain UNMEASURED**, not scored held or missed: run-one.sh records T1 before restoration, oracle, and gate; oracle duration and the full request-to-verified endpoint are absent. Caller+gate is not a complete-wall measurement either. No clocks were changed or reconstructed from file mtimes.

N acceptance is only R-03 for both models, while M acceptance covers 7 Astra and 9 Sol tasks. For the sole common accepted task, the paired caller ratios are 0.4954 Astra and 1.1261 Sol. Neither marginal accepted medians nor this single pair establish a speed advantage.

| Model | N caller-error/timeout time (s) | M caller-error/timeout time (s) | N all unaccepted caller time (s) | M all unaccepted caller time (s) | N gate total (s) | M gate total (s) |
|---|---:|---:|---:|---:|---:|---:|
| gpt-6-astra | 2.38 | 0.00 | 1151.08 | 580.73 | 259.22 | 424.17 |
| gpt-5.6-sol | 0.00 | 0.00 | 1554.11 | 837.06 | 245.99 | 577.09 |

Failed-run time is separated above both by caller failure and by failure to meet acceptance; the latter includes oracle and gate failures. Gate totals are separately recorded gate intervals, including their lock wait, and are not added to caller times.

Per-task paired caller walls and acceptance (`yes` means the retained rule passed); gate intervals and absolute M/N receipt paths are in [paired-walls.tsv](runs-N/attempt-2/paired-walls.tsv).

| Task | Model | N caller (s) | M caller (s) | M/N | N accepted | M accepted |
|---|---|---:|---:|---:|---|---|
| I-01-mission-cli-stale | gpt-5.6-sol | 171.87 | 187.32 | 1.0899 | no | yes |
| I-01-mission-cli-stale | gpt-6-astra | 142.13 | 108.66 | 0.7645 | no | no |
| I-02-mission-cli-admitted-profiles | gpt-5.6-sol | 195.12 | 548.24 | 2.8098 | no | yes |
| I-02-mission-cli-admitted-profiles | gpt-6-astra | 211.72 | 124.55 | 0.5883 | no | no |
| I-03-heap-sample-retention | gpt-5.6-sol | 264.50 | 191.12 | 0.7226 | no | yes |
| I-03-heap-sample-retention | gpt-6-astra | 114.15 | 118.07 | 1.0343 | no | no |
| I-04-mission-display-decision-view | gpt-5.6-sol | 194.71 | 372.23 | 1.9117 | no | yes |
| I-04-mission-display-decision-view | gpt-6-astra | 2.38 | 139.13 | 58.4580 | no | yes |
| R-01-core-discovery-require-edn | gpt-5.6-sol | 76.96 | 333.36 | 4.3316 | no | yes |
| R-01-core-discovery-require-edn | gpt-6-astra | 102.48 | 177.88 | 1.7358 | no | yes |
| R-02-recovery-require-measured | gpt-5.6-sol | 91.28 | 163.23 | 1.7882 | no | no |
| R-02-recovery-require-measured | gpt-6-astra | 45.44 | 55.19 | 1.2146 | no | no |
| R-03-workspace-string-alias | gpt-5.6-sol | 111.03 | 125.03 | 1.1261 | yes | yes |
| R-03-workspace-string-alias | gpt-6-astra | 95.93 | 47.52 | 0.4954 | yes | yes |
| R-04-namespace-split-require-cheshire | gpt-5.6-sol | 119.85 | 123.48 | 1.0303 | no | yes |
| R-04-namespace-split-require-cheshire | gpt-6-astra | 56.98 | 73.50 | 1.2899 | no | yes |
| T-01-outline-memory-spec-marker | gpt-5.6-sol | 55.43 | 187.38 | 3.3805 | no | no |
| T-01-outline-memory-spec-marker | gpt-6-astra | 69.95 | 83.19 | 1.1893 | no | no |
| T-02-study-row-line-range | gpt-5.6-sol | 149.12 | 235.21 | 1.5773 | no | no |
| T-02-study-row-line-range | gpt-6-astra | 123.51 | 211.72 | 1.7142 | no | yes |
| T-03-alias-migration-kind-count | gpt-5.6-sol | 109.49 | 247.25 | 2.2582 | no | yes |
| T-03-alias-migration-kind-count | gpt-6-astra | 103.37 | 89.24 | 0.8633 | no | yes |
| T-04-lane-manifest-total | gpt-5.6-sol | 99.27 | 251.24 | 2.5309 | no | no |
| T-04-lane-manifest-total | gpt-6-astra | 133.08 | 91.07 | 0.6843 | no | no |
| example-R-e4 | gpt-5.6-sol | 26.51 | 159.19 | 6.0049 | no | yes |
| example-R-e4 | gpt-6-astra | 45.89 | 89.51 | 1.9505 | no | yes |

Contract and apparatus findings:

- The retained runner shares `CODEX_HOME_BASE/N` across runs (run-one.sh:231); it does not deliver the requested per-run home. Its neutral AGENTS text lacks the packet’s apply_patch/Edit-only, no-script restriction. Every manifest plants observe shims for sed and python3 (run-one.sh:150–151), contrary to this packet’s no-shim requirement, though consistent with the older Astra design. These cannot be repaired retroactively without prohibited re-runs.
- N has no MCP table in its archived configs and no planted Surgeon launcher. That does not prove no CLI availability: archived PATH includes `/home/forge/bin`, which currently contains executable `clj-surgeon`. Historical in-session absence is unproven. Zero calls are evidence of recorded non-use, not proof of unavailability.
- 24 of 26 N gates are recorded red. Logs include `/tmp` AccessDeniedException failures (for example I-04 Sol gate.log:7, R-01 Astra gate.log:72), unlike a comparable successful-baseline environment. These are concrete apparatus errors; other failures need individual adjudication. Gate verdicts were not changed and oracle passes were not promoted to acceptance. N’s 1/1 versus M’s 7/9 accepted counts do not establish a native-method quality loss.
- The request-to-verified clock was not captured. The earlier draft’s ±0.15 forecast tolerance was unregistered and is not used here. Independent/blinded acceptance and ambiguous M producer review remain external.

The three config-delivery launches are already spent. [config-delivery-repro.md](config-delivery-repro.md) and its probeA/B/C event receipts are retained unchanged: all three carriers delivered a real MCP call under the recorded argv without --ignore-user-config. Typed missing-fields responses prove delivery, not successful inspection. The context pack already contains the narrowed isolation-memory correction. This attempt does not edit either memory or generalize the three probes to untested flag combinations.

**Typed refusal:** `{:type :cohort-reexecution-forbidden :owner "Fable" :remaining-runs 0 :reason "All 26 assigned results exist; conformant isolation and complete verification clocks require a newly authorized experiment."}` No new batch was needed or backgrounded in this attempt. Gene or the registered owner must authorize any changed task, oracle, prompt, argv, or clock; Fable owns the no-rerun stop.

All writes in this attempt are local report/collection artifacts. No production ledger, runner, task, oracle, config, or retained run receipt was edited; no new commit was made. Measurements are in [summary.json](runs-N/attempt-2/summary.json) and the structured [report.edn](report.edn).
