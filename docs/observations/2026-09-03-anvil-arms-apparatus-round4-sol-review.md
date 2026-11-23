# anvil-arms-apparatus 895eed0 — Sol executed round-4 review: GO-WITH-FIX (6/6 closed; runtime make overrides bypass the map; duplicate header accepted) — round 5 launched

GO-WITH-FIX. All six round-three findings are closed, but round four introduced two fail-closed gaps that should receive ratchets before spending a real cohort: runtime Make overrides bypass the whitelist model, and the scorer accepts contradictory duplicate headers.

### Round-three six-item replay

| # | Status | Executed witness |
|---|---|---|
| 1 | CLOSED | [_make_targets.py:127](bench/anvil-arms/_make_targets.py:127) rejects the whole file. My replay produced `false_resolved=[]`; all eight adversarial fixtures returned their expected `makefile-outside-whitelist:<feature>`, while baseline, phony, and recursive assignment resolved to `bin/kaocha`. [Result](/home/forge/tmp/arms/solreview4/static-parser-result.json) |
| 2 | CLOSED | [watch.py:521](bench/anvil-arms/watch.py:521) enables the subreaper and [watch.py:912](bench/anvil-arms/watch.py:912) walks the watcher’s descendants. Replay: `escaped_alive_after_watcher=false`, `descendants_recorded=2`, `orphans_after_reap=0`. [Result](/home/forge/tmp/arms/solreview4/subreaper-result.json) |
| 3 | CLOSED | [score.py:225](bench/anvil-arms/score.py:225) requires the schema-v2 header and provenance keys. The unversioned stream returned rc 3, `watch-schema-unsupported`, and no receipt; the suite also replayed the original round-three rotation artifact successfully. [Result](/home/forge/tmp/arms/solreview4/schema-result.json) |
| 4 | CLOSED | [watch.py:417](bench/anvil-arms/watch.py:417) types both failures and [watch.py:1156](bench/anvil-arms/watch.py:1156) maps them to rc 8. Both no-errno mocks produced `rollout-stat-failed:UNKNOWN` with `ESTALE` retained in the detail; real EACCES produced rc 8, scorer rc 3, no receipt. [Result](/home/forge/tmp/arms/solreview4/inode-failure-result.json) |
| 5 | CLOSED | [self-test.sh:102](bench/anvil-arms/self-test.sh:102) captures suite stderr and [self-test.sh:1885](bench/anvil-arms/self-test.sh:1885) gates it. The escaped backticks print literally, and the full suite reported no unintended command. |
| 6 | CLOSED | [README.md:10](bench/anvil-arms/README.md:10) describes computed totals without hand-typing them. My repository-wide README count grep was empty, and case 38 passed. |

### Round-four findings

- The B.4 prompt set is compatible with whole-file Make refusal. Both P prompts use `bin/fan-test`; both L prompts require direct `bin/kaocha --focus …`. Every `make ` occurrence is “Do not run `make golden-update`.” A compliant cohort can complete.

- `?=` is genuinely outside a safe static subset. [Makefile:2](Makefile:2) triggers `makefile-outside-whitelist:assignment-operator:?=` and resolves zero targets. My isolated fixture changed behavior when `CMD` was supplied through the environment, confirming it cannot be admitted without modeling that environment.

- New Make gap: [\_make_targets.py:133](bench/anvil-arms/_make_targets.py:133) admits plain `=`, while [watch.py:199](bench/anvil-arms/watch.py:199) ignores the semantic effect of command-line assignments. For `make CMD=bin/kaocha verify`, GNU Make ran the Kaocha stub, but the meter reported `watch_is_test=false` and `watch_unresolved=[]`. This falsifies the README’s general “exact on the whitelist” claim, although it is unreachable in the two current repositories because both maps resolve nothing.

- The subreaper survives the requested edge cases. An adopted zombie did not stall exit (`0.686s`); an independently subreaper-enabled driver’s setsid orphan was killed; and a sibling subreaper got `ECHILD`, confirming that an unrelated tenant’s orphan cannot reparent to it. The comment at [watch.py:535](bench/anvil-arms/watch.py:535) saying the subreaper setting is inherited by the driver is inaccurate, but the implementation does not depend on that claim.

- Genuine late binding remains exact: the first output was record 1, `rollout-bound` was record 2, and its session/dev/inode exactly matched the selected rollout. However, [score.py:181](bench/anvil-arms/score.py:181) permits `header` anywhere, and a second contradictory header—schema 999, different session and inode—scored rc 0 and wrote a receipt. Require exactly one header at record zero, and preferably at most one `rollout-bound`.

- The stderr gate is properly scoped. [run-arm.sh:247](bench/anvil-arms/run-arm.sh:247) redirects watcher/driver output into the arm’s `driver.log`; an isolated `command not found` there yielded a suite-gate count of zero, while the same phrase on suite stderr yielded one. [Result](/home/forge/tmp/arms/solreview4/stderr-scope-result.json)

- At host one-minute load 7.74–10.08, the 250 ms scan consumed 2.52 watcher CPU seconds over 61.479 seconds: 4.099% of one core, approximately 240 scans. Watcher and scorer both exited 0. [Result](/home/forge/tmp/arms/solreview4/cpu-result.json)

The required self-test summary line, verbatim:

```text
anvil-arms self-test: 354 passed, 0 failed  (workdir /home/forge/tmp/arms/selftest.IJ8O8N)
```

The checkout remained clean at `895eed0`; port 7909 is free and no review-owned process remains.

### Verdict: GO-WITH-FIX

1. [watch.py:199](bench/anvil-arms/watch.py:199) — refuse runtime Make assignments/options not represented by the attested map; witness: `make CMD=bin/kaocha verify` ran Kaocha but was classified non-test and resolved.
2. [score.py:181](bench/anvil-arms/score.py:181) — require exactly one header at record zero; witness: two contradictory headers scored rc 0 and produced a receipt.
3. [E3-L-N.md:18](bench/anvil-arms/prompts/E3-L-N.md:18) — retain the pre-registered direct-runner prompts; witness: no prompt asks an arm to execute Make.
4. [watch.py:922](bench/anvil-arms/watch.py:922) — subreaper reap is cohort-ready; witness: fast fork, adopted zombie, and nested subreaper all left zero live orphans.
5. [run-arm.sh:247](bench/anvil-arms/run-arm.sh:247) — stderr scope is correct; witness: driver-local `command not found` never entered the suite’s stderr gate.
6. [watch.py:1017](bench/anvil-arms/watch.py:1017) — record the scan cost in the cohort receipt; witness: 2.52 CPU seconds over a 61.479-second fake run near load 10.