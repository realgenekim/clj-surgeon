# Independent calibration receipt audit

Verdict: the native floor in calibration-final-13.json is correctly derived from retained original metadata. All 13 rows and both models' reported summary statistics match independent recomputation exactly. There are six clean controls per model, with the original contaminated observation retained separately. No raw receipts were modified and no models/JVMs/tests were launched.

| Model | Clean n | Median task wall | Mean | Sample SD | Twice SD | Range |
|---|---:|---:|---:|---:|---:|---|
| gpt-5.6-sol | 6 | 134.75 s | 135.8667 s | 26.8435 s | 53.6870 s | 99.8–167.8 s |
| gpt-6-astra | 6 | 51.15 s | 51.75 s | 4.45365 s | 8.90730 s | 46.4–59.5 s |

The primary values are original run.json wall_s. Adapter wall includes preparation/freezing/attestation; orchestration wall additionally includes wrapper/slot and external acceptance work. Neither was silently substituted. Acceptance is independently clocked and separately available; watcher wall plus acceptance gives the stated verified-task total, while wrapper elapsed is a different observable. Watcher wall is an apparatus interval, not pure model/API latency.

## Evidence integrity and inclusion

- Rechecked all 13 adapter model assertions against only session_meta/turn_context metadata in retained rollouts. Every requested/observed model agrees; every unique session ID agrees with run.rollout_binding and the session announced by driver-output.log. Hidden reasoning and message contents were not used.
- Recomputed prompt SHA256, checked fixture HEAD against both attested base fields, and verified protected test/runner SHA256 plus modes against guard.json and retained guard copies. All match. Six-check acceptance logs are present and pass for every observation. All runs have successful driver/watch outcomes and no recorded orphan survivors.
- Prompt, adapter, watcher, scorer, launcher and vendor executable hash identities are constant across all calibration attestations; the amended wrapper hash is constant across its 12 observations. The first Sol run predates that wrapper and retains its separate inline-sampler and acceptance evidence.
- Recomputed contamination directly from raw one-second load records and phase-boundary records for the 12 wrapped observations, matching every reported adapter, acceptance and verified-completion sample count, maximum and contamination flag. The original cal-sol-1 raw inline sampler reproduces 124 samples and maximum 13.19. The exclusion rule is host load >10, independently of result or elapsed wall.
- cal-sol-1 was accepted, 122.3 s, and excluded only from clean timing because of its measured load. Amendment 1 records the authorization at 22:48:12Z, before the remaining model comparisons, to append one fresh cleanretry per contaminated identity after original calibration. Observed ordering matches the alternating/swapped model schedule and ends with cal-sol-1-cleanretry. That retry is accepted but slower, 167.8 s, and is retained. The fastest/slowest clean observations are also retained. The records therefore show no favorable-outcome or fast-run exclusion. This audits observable selection, not anyone's private intent.
- valid_measurement=true and contaminated=true on the first Sol run are compatible: model/session/task attestation passed, while host-load eligibility failed. They must remain separate fields.

## Credible limitations

The clean designation means sampled host load never exceeded the declared threshold. It does not establish an idle/exclusive host, bound subsecond interference, or eliminate time drift. Wrapped contamination uses the enclosing adapter interval rather than an independently sampled exact watcher interval; this is conservative for task eligibility. The original Sol run lacks the later wrapper's separate acceptance-interval sampler, so acceptance contamination for that observation is unknown rather than clean. It is excluded from the clean floor anyway.

The watcher's utcnow endpoint is formatted to whole seconds before wall_s is rounded to one decimal. Thus the printed decimal does not confer 0.1-second physical precision: the end timestamp can truncate almost a second. This is small relative to the observed floors but should travel with subsecond comparisons. Do not replace frozen primary numbers retroactively; adapter monotonic clocks remain separately reported.

At n=6 per model, SD estimates are uncertain. Twice SD is the preregistered screening threshold, not a confidence interval or universal significance test. The task is one known synthetic fanout family, not evidence for other repositories. All 13 accepted results establish observed acceptance, not error-free future behavior. Frozen hash attestations agree; this metadata review cannot prove the historical host binary against post-hoc adversarial tampering beyond the retained evidence.

## First complete pair available at audit time

Sol replicate 1: native 117.2 s, tool 35.2 s, both independently accepted and clean. This is an 82.0 s difference and 3.3295x task-wall ratio. Native/tool external acceptance costs are 0.5375/0.5278 s; verified-task totals are 117.7375/35.7278 s. Tool startup is separately attested at 9.5268 s and is not inside the 35.2 s primary wall. Wrapper elapsed is 118.7591/36.4777 s; those also exclude the separately completed server startup. The tool attests repaired server commit da7ba418cbe3e1de22efdd1471a0c295c0422d80, per amendment 2.

The observed 82.0 s difference clears Sol's 53.6870 s screening floor in this one pair, but **one pair is exploratory and does not establish a repeatable tool advantage**. This snapshot does not establish tool-call mechanism, independent full paired replication, free-choice adoption, or cross-model causal attribution. No Astra tool result or future pair is imputed; no waiting was done for additional arms.

Machine evidence and per-observation secondary clocks: independent-calibration-audit.json. Reproducer: independent_calibration_audit.py. The JSON also records additional six-check/session-announcement checks and the first-pair snapshot. Original run, adapter, acceptance, orchestration and contamination receipts remain untouched.

Audit recorded UTC: 2026-09-04T23:54:54.375249+00:00
