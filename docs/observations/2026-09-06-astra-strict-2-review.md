# ASTRA strict-2 review — 2026-09-06 19:58Z

ACK strict-2 CHANGES. Reviewed e3a7a9ae and both exported sections. The direction is right; the following text changes are needed before GO. No new cohort requested.

1. TWO automatic mutation classes, not three automatic classes. “Outline a large file once instead of reading it” adds an automatic read route without a measured wall advantage. Keep inspect as an optional supporting read when structural information is actually needed; native discovery may already suffice. Do not mandate an inspect pass after sufficient native discovery. Remove the inherited ~150x token claim from the always-loaded router; no supporting comparison was supplied here, and tokens are not the prime meter. Also, a wildcard underscore matches one subtree: `(store/find-event _)` does not enumerate calls of arbitrary arity. Label the example or use the exact required arity.

2. Replace the alias example with the actual measured shape below. The draft adds `verify: "clojure -M:test"`, `refer_policy`, a different alias policy, and an exclusion list. Those were not in this replication; a hard-coded test command is not a repository-independent proof contract. State that the actor runs the repository's existing required load/tests afterward unless the receipt explicitly proves them. Executed example receipts must correspond to the exact published request, live schema and fixture.

```json
{"op":"alias_migration","workspace_root":"/absolute/repository",
 "from":{"lib":"acid.fanout.store","var":"find-event"},
 "to":{"lib":"acid.fanout.store2","var":"fetch-event",
       "alias_policy":["store2","st2","es","store-2"]},
 "scope":{"paths":["src"]},"expect":{"files":21}}
```

Explain that lib/Var/scope/count values are task inputs, not constants to copy unchanged. Include workspace_root in all three examples when routing another repository; the skiff case must not silently use the server's default project.

3. Remove contradictions between “native everywhere else,” “unwitnessed calls allowed,” and “do not use any extraction/whole-feature through any gate.” Native is the performance default outside the two classes; explicit user requests or separately approved experiments may still use other capabilities. The tested extraction and whole-feature losses justify that default, not universal impossibility claims for every extraction or gate. This must agree across skill and plate.

4. The receipt section must test values, not mention field names: `verification_complete=true` plus the named successful checks on the current snapshot. A false/pending field is not evidence. Byte read-back proves the write, not task semantics; even a successful alias receipt still required behavior/load tests in our run. Replace “run tests ONCE and stop” with “run the outstanding required checks; repair any failure before claiming completion.” Do not let a prescribed single test invocation convert a failed test into a completed task.

5. One repair means ONE clear, safely correctable argument error, not any refusal. Stale/conflict cases require fresh evidence; unavailable capability goes native immediately. Before fallback inspect the receipt's mutation/commit status: do not assume every refusal means no mutation, and do not reapply a completed change blindly. This matters especially when several operations have already succeeded.

6. Apply the19:50 correction to the kill switch: correctness failure suspends the route; a wall loss is assessed against controls, not an automatic permanent ban based on one noisy pair. Unknown telemetry means unknown performance, not a measured loss. Conservatively using native pending investigation is reasonable, but do not require a fresh experiment after every missing telemetry row. The collector's claimed complete task-wall/fallback coverage needs an actual receipt; otherwise phrase these as required measurements with unknowns retained.

7. Keep the always-loaded text short. Put experiment histories, detailed ratios/caveats and operating the weekly meter in a linked evidence/maintainer document; retain trigger, complete calls, real prerequisites, receipt scope and escape rule in the plate. The objective is to remove user work. A longer compulsory policy is itself a potential cost and has not been measured as zero.

The final example executions and base-versus-tip test comparison remain Fable's release evidence. This is a text review, not a fence GO or authorization to install an unreviewed revision.
