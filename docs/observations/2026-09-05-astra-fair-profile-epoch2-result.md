| Arm | Runs | Median wall | p90 | Correct? |
|---|---:|---:|---:|---|
| Sol native (variance controls) | 6 | 117.11s | 141.98s | 6/6 |
| Sol native (paired cohort) | 6 | 105.61s | 134.22s | 6/6 |
| Sol tool (paired cohort) | 6 | 45.44s | 49.50s | 6/6 |
| Astra native (variance controls) | 6 | 45.03s | 54.40s | 6/6 |
| Astra native (paired cohort) | 6 | 45.18s | 55.95s | 6/6 |
| Astra tool (paired cohort) | 6 | 35.02s | 37.13s | 6/6 |

Astra epoch-2 final report, audited 2026-09-05T23:24:41.813438+00:00.

The runner completed all 36 scheduled runs at 2026-09-05T22:56:19.960202Z. It did not stop on an error. The last scheduled arm was pair-6-sol-native; all 12 tool arms had already run in the frozen mirrored schedule. No new runs are needed.

Each tool arm made exactly one successful public helper_extraction call, with zero helper refusals. All 36 parent behavioral gates passed (110 cases each); resolved models matched, protected bytes passed, frozen diff hashes matched, all original apparatus hashes still match, and all 12 owned servers reported zero survivors. Peak sampled load was 3.61, below the preregistered contamination cutoff of 10.

Observed median paired native/tool ratios: **Sol 2.50x; Astra 1.30x**. Ratios of arm medians are 2.32x and 1.29x respectively; these are different estimators, not contradictory results. Headline walls include cold server startup, orientation/model activity, edit, external acceptance and artifact capture. Fixture provisioning and resource reservation precede the clock.

The registered two-SD hurdle matters: Sol floor SD 14.54s (hurdle 29.08s), median paired saving 63.52s — clears. Astra floor SD 5.84s (hurdle 11.69s), median paired saving 10.13s — does NOT clear. Thus Sol establishes a gain on this bounded fixture; Astra has a favorable observation, not a gain established by the registered threshold. This threshold is a preregistered heuristic, not a confidence interval. No 10x claim.

Conditions: three known selected helpers, 21 homogeneous mixed callers, 42 moved references, a preconfigured synchronous fresh-process behavioral profile, supported syntax, mandated public verb, high-effort Codex 0.153.3 subjects, CPUs 12–13. This does not establish optional adoption, warm-session gains, unseen-repository orientation, or typist-provider performance. Native scripts were allowed. Preparation of the fixture/profile and oracle is not a user-discovery cost measured by this experiment.

Watcher median model returns in the paired cohort: Sol native 4.5 / tool 3; Astra native 2 / tool 2. Outer-call metering can conceal nested shell commands inside functions.exec, so do not infer precise native patch/test counts from it. There are no independent blind-judge rulings in this epoch; correctness here means the frozen behavioral/protected-byte gate, not universal equivalence. The withdrawal of epoch 1 remains in force.

p90 uses nearest rank; with six runs it equals the maximum. Full retained receipts: /var/tmp/forge/astra-fair2-data-fx/{runs,arms,servers}; exact order: schedule.json. Parent acceptance.log and final-tree.json are the acceptance authority; the older generic scorer's gate field is not substituted for them.

| Pair | Model | Native wall | Tool wall | Native/tool |
|---|---|---:|---:|---:|
| 1 | gpt-5.6-sol | 72.036s | 47.098s | 1.529x |
| 2 | gpt-5.6-sol | 110.824s | 43.792s | 2.531x |
| 3 | gpt-5.6-sol | 56.256s | 48.153s | 1.168x |
| 4 | gpt-5.6-sol | 101.701s | 41.235s | 2.466x |
| 5 | gpt-5.6-sol | 109.516s | 42.938s | 2.551x |
| 6 | gpt-5.6-sol | 134.222s | 49.500s | 2.712x |
| 1 | gpt-6-astra | 53.348s | 34.924s | 1.528x |
| 2 | gpt-6-astra | 42.175s | 33.671s | 1.253x |
| 3 | gpt-6-astra | 43.778s | 37.128s | 1.179x |
| 4 | gpt-6-astra | 46.581s | 34.823s | 1.338x |
| 5 | gpt-6-astra | 55.951s | 35.122s | 1.593x |
| 6 | gpt-6-astra | 41.421s | 35.221s | 1.176x |
