# Astra: real proof-profile utility

Initial observation 2026-09-06T14:25:10.000524+00:00. This is an integration dogfood report, not a speed comparison or free-choice adoption cohort.

The cardinality diagnostic keeper landed as `ee911577` on the working trunk after an exact-tip independent LAND YES and merged-tree gates. Public main remains frozen. The next exercise uses Maven's actual recording-query source and unchanged tests to check the existing admission profile ABI.

| Attempt | Observed outcome | Wall |
|---|---|---:|
| Good candidate / broken live source, direct profile v2 | 2 tests pass; candidate loaded instead of broken live file | 2.006 s |
| Broken candidate / good live source, direct profile v2 | 2 tests, 3 timestamp assertion failures, 0 errors; candidate loaded instead of good live file | 2.006 s |
| First public admission, v2 | Refused before tests or write: adapter rejected gate-created bookkeeping files; lint clean | 264.011 ms service receipt; 279 ms enclosing call clock; 287 ms caller event clock |
| Same-patch public retry, v3 | Committed 1 file / 1 owner; actual named suite 2 tests, 0 failures/errors; clean lint and complete verification | 2144.939 ms service receipt; 2162 ms enclosing call; 2249 ms caller event clock |

These clocks cover different boundaries. Neither the direct runner nor the first public call is a complete task wall or a native speedup. The negative case's two tests contain five assertions; its three failures are assertions, not three additional tests.

Both direct cases retained actual namespace reports, unchanged live/candidate hashes, and empty cleanup intervention/survivor sets. The bad candidate changes timestamp conversion from divisor1000 to100; its captured assertions show expected00:12 becoming02:00 and expected01:05 becoming10:50. Failure details are retained locally and may contain source; they are not privacy-safe telemetry. Independent actual-result review confirmed both outcomes and the retained file modes.

The public edit is a small real clarity refactor: bind transcript text length once within each timed-segment iteration and reuse it in the four existing bounds. The placement preserves empty-timing behavior. Existing independently authored tests cover the affected bounds, separators, timestamps and absent timing. No runtime performance improvement is claimed for this refactor.

The first public call had a real repository profile, focused verification and an exact pre-image hash. It still failed because the adapter treated `.clj-surgeon/.gitignore` and `.clj-surgeon/write.lock`, created by the gate itself, as unexpected source-closure files. No original source, dependency or test file changed. The gate did the right thing: no report means no verified commit.

The v3 repair admits exactly those two regular, non-executable control files with their observed contents; it does not honor broad ignore patterns, admit new Clojure files, relax source/test/dependency pins or copy control files into the classpath. Five adversarial preflight cases subsequently refused with the exact expected reasons: unknown extra Clojure file, symlink bookkeeping, unrelated source drift, nonempty lock and directory lock. The public retry then committed the same prepared patch with complete candidate-bound verification. First-call complete receipts remain **0/1**; across both calls, **1/2 committed successfully**. The repair does not rewrite the first failure.

Preparation is material: the original restricted profile took326.234s; the useful patch/fixture preparation took109s. Diagnostic repair, independent reviews, and shared-box queueing add further cost and are not silently included in the two-second rows. This adapter is deliberately frozen to one existing file in a real Maven closure; it is not a generic repository runner or deletion/new-file support.

A single bounded usage collection found the call in both service event sources. The preferred ledger's matching event is associated by operation/time/PID984724, not a workspace fingerprint. Its wall field is null, and normal telemetry also lacks elapsed time. Caller clocks supply the measured call duration; no client value was substituted into a service metric. Full task/account token balances are not inferred from this short window.

Evidence retained under `/var/tmp/forge/astra-real-profile-example-fx` (v2 actual receipts and independent review; immutable v1/v2 archives beside it) and `/var/tmp/forge/astra-admission-use-fx` (exact native patch/request, first refusal/timing, unchanged-file review, and privacy-safe usage receipt). Current production-source code was not changed for this profile repair.

## Verified retry — updated 2026-09-06T14:33:19.384566+00:00

The receipt identifies `maven.recording-query-test`, 2 tests, 0 failures/errors, clean lint, one changed `timed-segments` owner and zero byte drift outside the hunk. Its post-image hash `52b314ea…` matches the independently reviewed, pre-authored candidate. `next_call` is null. No live source reread or redundant external suite was needed to accept this receipt. A separate reviewer checked the saved evidence and scope; this is not another runtime replay.

The public success receipt does not expose the adapter’s success stdout or explicit cleanup census. The reviewed adapter returns zero only after its cleanup predicate succeeds; do not relabel that as an additional observed process census. The five malformed-input preflights returned before the launcher phase according to the reviewed control flow and exact error reasons.

The second bounded usage study found the successful call in both event sources, again with missing producer elapsed time. Current source inspection shows admission emits telemetry before public elapsed is finalized; merely restarting the old server would not fix it. This does not invalidate the separately recorded public/client clocks, and it was not used to excuse the first profile failure.

This demonstrates one useful frozen-fixture integration of the existing API. It does not demonstrate a generic profile runner, free-choice adoption, a native speedup, or publication of the Maven edit into its original repository. The keeper is in the explicitly supplied scratch workspace. Tool-description guidance and portable example packaging remain separate unfinished parts of the usage plan.
