# Astra: a whole Maven feature, native versus patch admission

Observed 2026-09-06T17:16:25.142879+00:00. **The preregistered wall-win prediction failed.** Both fresh actors completed the feature correctly; the tool-assisted actor took about1.04x as long. This is one pair, not evidence of a statistically significant general slowdown or speedup.

| Clock | Native N1 | Tool T1 | Native / tool |
|---|---:|---:|---:|
| Request dispatch through actor completion and cleanup (primary) |128.420s|133.027s|0.9654x|
| Through independent final acceptance |130.639s|135.144s|0.9667x|
| Through final automated scope/Git audit |130.885s|135.390s|0.9667x|

Both received the full128-file tracked Maven repository at `0eecb55a`, identical proof/configuration and independent acceptance, and the same task: add `grounding-quote-candidates` returning all normalized quote locations, including overlaps, while preserving the existing first-match API. Each had to discover the code, implement the feature, add ordinary tests and README documentation, pass proof and make a local Git commit. No CLI/database/importer/dependency changes were allowed. Their initial137-file workspaces included the common proof and exact gate bookkeeping files. No solution dossier or reference candidate was supplied to either actor.

Fresh actors used pinned Codex0.153.3, `gpt-6-astra/medium`. Schedule was N1 then T1, with no reruns or additional controls. Native used ordinary reads/patches and the shared snapshot-aware proof. Tool used native orientation and candidate construction, then public `admit_clojure_patch` with focused proof; README/Git work remained native. The existing MCP server stayed on the operator-attested181c365c generation. This tests a prepared integration environment, not cold server installation or autonomous proof-profile creation.

## Correctness and actual route

Separate saved-evidence reviews returned GO for both. All protected file bytes/modes and the exact file set were preserved; original tests and old API implementations remained unchanged. Both committed all three required paths with explicit Astra identity and clean tracked trees. Native commit: `4c0e9d106f830d48bc2e528631145671d69ca92d`. Tool commit: `9f3a4c96016ceecf03cb55a33ff50e8ed65a4696`. These are local experimental repositories, not publication to the original Maven repository.

N1 used no Surgeon calls. It repaired an incorrect expected character offset16→17 in its own new test, then passed. T1 made two actual admission calls: the first correctly refused without mutation because its own new test used an incorrect offset/timing fixture18 instead of19. The six independent acceptance tests already passed. T1 repaired the test fixture, left its proposed implementation unchanged, and the second call committed source and tests with complete verification, clean lint and zero outside-hunk drift. No native source/test fallback occurred.

The actor collector labels the first tool event `invalid-mcp-request`; the structured public result is authoritative and says `verification-failed` / `focused-tests-failed`. Preserve that telemetry classification defect rather than describing a malformed request. A failed test is not an admission-system defect.

## Proof and accounting

Before actor timing, the correct private candidate against broken live source passed8tests/18assertions; an overlap mutant against correct live source produced one real assertion failure. Both input trees remained unchanged, with no cleanup intervention or survivors. An actual source-plus-test public preview then passed9tests with clean lint in2.109s. These are setup/integration evidence, not timed task attempts. No reference implementation entered actor workspaces or prompts.

Initial task preparation through actor launch took approximately886s, including shared-window waiting. Preparation through final independent decision took1363.282s (22.72min), including actor execution, queue and review. The earlier restricted-profile engineering is additional historical investment, not amortized away by this report. The same proof entrance and frozen independent suite were available to both arms; the supplied integration reduces preparation inside both actor clocks. The predicted10–20min native task actually took2.14min: our task-size estimate was poor.

Observed cumulative actor usage: N1 input201,903 (184,704cached), output2,889 (133reasoning); T1 input207,240 (184,192cached), output3,184 (233reasoning). Combined415,216tokens. Cached input and reasoning are subsets, not additional totals; dollar cost is unknown. T1 has7outeractions versus N1's8; fewer actions did not produce a wall win.

Prediction was at least1.25x tool speedup with low confidence; any incorrect/incomplete tool task, no tool commit, or tool wall at/above native falsified it. The wall condition fired. Neither this loss nor the prior two-second keeper calls justify a general-editor promotion. The next useful question is which measured discovery, request-construction or repair interval a winning route can remove.

## Retained authority

All artifacts are under `/var/tmp/forge/astra-whole-maven-fx`: `preregistration.md`, `manifest.json` (SHA9163c019b84be5d1653b934845f8e2a84bce5307d7da603f7690e72d84b499d3), `pair-result.json`, per-arm result/actor receipts and independent reviews, proof snapshots, and preflight cases. The shared block ledger `whole-maven` is closed as a completed experiment with an explicit LOSS, not a done win. No further runtime is needed to trust the already retained results.


## What the complete-task logs changed

A bounded read-only study of both exact rollouts found the largest visible gap before source/test generation:60.734s native and48.905s tool. These include generation and scheduling; they are not isolated model-compute measurements. The admission route still had Astra generate the implementation and tests. It therefore did not exercise the original fast-typist mechanism, whose current integrated executor cannot add new `defn`/`deftest` owners.

Both implementations passed independent acceptance before their actors repaired their own test literals. Native received the exact expected/actual assertion failure; tool admission returned namespace counts and a temporary report path that cleanup had already removed. The attempted read failed. This is a real diagnostic paper cut, but the read itself took0.045s and the surrounding gaps cannot honestly be assigned entirely to it. Native failure-visible through success-visible was20.018s; tool30.933s. These overlapping workflow spans are not safe counterfactual savings.

T1 made no extra actor-side native proof call after its successful admission. A blanket diagnosis that callers always layer redundant proof on top of the tool would be wrong here. The next bounded question is whether an existing fast-typist entrance can reduce source/test generation under the same proof without a new API. The current pair remains a loss regardless of any later experiment. Study and machine action timeline: `/var/tmp/forge/astra-whole-maven-fx/usage/`.
