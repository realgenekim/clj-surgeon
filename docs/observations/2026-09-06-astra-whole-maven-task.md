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


## Astra follow-up: fast typing succeeded, the complete workflow lost

Observed 2026-09-06 at17:43Z; independently reviewed before reporting. A second prospective pair tested the same feature with fresh Astra/medium actors, the same full repository and unchanged independent proof. The treatment added one existing Cerebras OSS-120B transport request to supply new source/test/README text. Astra still supplied intent, reviewed and integrated the response, and used public admission before a local Git commit. This was an external-client experiment, not newly integrated mission-ledger support for adding owners. Exactly one candidate was allowed; repairs remained inside the clock. No paid provider preflight or actor reruns occurred.

| Clock | Native N2 | Fast typist plus admission T2 | Native / tool |
|---|---:|---:|---:|
| Request dispatch through actor completion and cleanup (primary) |122.616s|226.761s|0.5407x|
| Through independent final acceptance |125.040s|228.981s|0.5461x|

**LOSS: tool took1.8494x as long on the primary clock, or1.8313x through independent acceptance.** The preregistered prediction of at least1.5x speedup failed. Both separate reviews returned GO: original source/tests remained exact prefixes, protected files and scope were intact, and all three required paths were committed with explicit Astra identity. Native commit `9a980a3a091d852fa5ee5a18597786d57c40da69`; tool commit `8e2955d4b6509edf6cfab08e40d2f7b07b0e6938`. These remain local experimental commits. One pair does not establish a general slowdown distribution.

The provider contribution was real. One usable `openai/gpt-oss-120b` response from Cerebras took1.914s for the request and2.075s through the wrapper. Its algorithm, basic test and README text survived into the accepted result; Astra revised bindings/docstring and added boundary tests. Provider usage was3,127prompt plus2,015completion tokens, including830reasoning tokens, costing$0.0026057 as reported by the provider. Count the nested attempt and candidate receipt once, not twice. No provider retry or native source fallback occurred.

The whole workflow used16outer actions versus native's7. Astra attempted an unnecessary extra JSON decode of already decoded strings, repaired that local error, expanded the tests, then received an admission refusal for its own incorrect offset14 instead of15. The provider's basic test and independent acceptance were already passing. The implementation stayed unchanged during test repair. The refusal again offered a deleted temporary report path; Astra read the proof adapter and ran direct proof for diagnostics before successful admission. Successful admission verified10tests, clean lint and zero outside-hunk drift.

Measured event gaps help locate work but do not isolate causes. Native first-source-return to first patch was58.089s. Tool first-source-return to prompt/transport was28.948s; reading the returned response to staging the patch was64.716s. That latter interval includes integration, review, extra tests, the decode repair, generation and scheduling. Failure-visible to success-visible was20.036s native and61.030s tool. These spans cannot be subtracted as guaranteed savings from fixing one paper cut. A two-second text producer did not eliminate the actor's responsibility or the work required to integrate its output.

Static preparation reused the first pair's proof within the10min cap. Preparation through independent decision still took1,106.637s (18.44min), including actors and review. Actor usage was169,929tokens native and622,640tool, totaling792,569; provider added5,142. Cached inputs and reasoning are subsets. Actor dollar cost remains unknown. This is not a cheap whole-task win merely because the provider cost a quarter cent.

Frozen authority: `/var/tmp/forge/astra-whole-maven-fast-fx/`, manifest SHA `7b2d3cd6dc2d185a857e7518a87ae2a2683859affbac5693a484e60a181bc460`, `preregistration.md`, final `pair-result.json`, both per-arm independent reviews, provider response/process receipts and `usage/whole-task-ethnography.md`. Initial common commit was `d188a4be944a39677b6a7c4cea6a22c3f1113bc1`. Existing original proof and transport were pinned before launch. Both actors and all provider/proof processes are terminal; the timing window was explicitly released.

Both whole-task approaches are now **parked for this trial**. No third variant follows these two losses. Reopening needs a concrete mechanism that removes observed intent-to-commit work, with the same fresh-actor and correctness boundary. Faster generation by itself is insufficient. The separate roughly1.75x proof-inclusive fan-out result remains narrower evidence for batching a known repetitive intent; it does not establish general editor superiority or2–4x whole-feature gains.

Timing disclosure for the first pair: Fable reported a usage-collector JVM overlapping N1 around17:06–17:10Z, with load2.4–3.5. We retained the original pair and disclosed the overlap rather than silently rerunning a favorable control. The later pair was separately preregistered, not a replacement for that result.
