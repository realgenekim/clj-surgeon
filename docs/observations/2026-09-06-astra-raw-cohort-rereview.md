# Astra separate-author offline re-review

**GO for the four apparatus corrections at 562887db28e359d27994d10358e013c16495d598**, limited to offline review. This is not launch authorization or a performance result. Reviewer did not author this runner or correction. Isolated checkout: `/var/tmp/forge/astra-raw-cohort-rereview-fx`, branch `astra/raw-cohort-rereview`.

Executed with isolated events, PYTHONDONTWRITEBYTECODE=1 and nice10: `python3 bench/raw-cohort-v2/test_run.py` -> **15 tests passed**, 0.090 s harness time. No paid calls, providers, prospective fixture preparation, services or source edits. Tests use small temporary inventory fixtures and mocked dispatch; those are not cohort materialization or performance evidence.

Executed original in-memory probes independently:
- Native verified=false with first_verified_s=1 -> `native-terminal-success-contradicts-candidate`.
- Native clock string or NaN -> `native-verification-clock-malformed`.
- Actual retained raw pilot -> accepted (`None` evidence fault); three completed requests with only winner index2 compiled remains allowed.
- Pilot with empty candidate list -> `tool-terminal-success-without-passing-winner`.
- Pilot with all compiled proof replaced by failed gate exit1 -> same terminal-success refusal.
- Only index0 completed and no cancellations -> `tool-requested-candidate-accounting-incomplete`.

Code review closes the other findings:
- Preparation owns one canonical native-preimage with bounded exact byte/mode/file inventory. Both parent dispatch and native child check it before use; every completed command checks it afterward. Native gets explicit `--fixture`; the inherited launcher binds PREIMAGE from that argument and puts its generated manifest beside it. Native receipt preimage must match that cohort-owned path. The changed-seed test refuses before launcher loading. Existing shared global PREIMAGE is not adopted.
- The clock now includes T child setup/copy/spec serialization before exec of mission. Native actual manifest/profile/reset and independent judge costs remain charged. Shared seed creation is outside both, decomposition is not subtracted, and prereg explicitly describes different actual workflows. No artificial native overhead is added to T.
- Engine HEAD plus tracked source cleanliness, hashed runner/launcher/transport/dossier/template/prereg/prompt and parent-reviewed frozen manifest remain checked. Proof/source starting bytes are inventory-bound; T generated spec hash is retained in setup.json. Parent still owns final spec/proof adequacy and actual launch environment review.
- Fixed6 then NT/TN/NT/TN, wrong outcomes retained, fault rows saved before stop, and no replacement/retry remain unchanged. Results open exclusively; preexisting T workspace/spec/setup refuses; child checks copied seed before dispatch. Opening native header/fresh fork checks remain intact.

Limits: no executed live cohort, model identity response, process quiescence, latency, power-loss durability, detached-child cleanup, proof independence or performance extrapolation is established here. Those remain the documented parent prelaunch/analysis responsibilities. This GO closes the original four offline apparatus HOLDs; it is not a general security certification of every nested receipt shape.
