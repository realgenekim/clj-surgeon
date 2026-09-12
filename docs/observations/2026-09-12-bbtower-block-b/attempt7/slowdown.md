# Slowdown localization — stopped at step 2

| Arm | Original fast sum (ms) | All namespace sum (ms) | Makespan (ms) | Width / processes | Result | Logs |
|---|---:|---:|---:|---|---|---|
| a-base | 36335 | 36335 | 27638 | 8 / 8 | passed | [log](a-base.log), [receipt](a-base/receipt.edn) |
| b-subject-jvm | 37661 | 37661 | 26957 | 8 / 8 | passed | [log](b-subject-jvm.log), [receipt](b-subject-jvm/receipt.edn) |
| c-subject | 68679 | 302525 | 82699 | 8 / 22 | failed | [log](c-subject.log), [receipt](c-subject/receipt.edn) |
| test-fast | 68664 | 302378 | 82426 | 8 / 22 | failed | [log](test-fast.log), [receipt](test-fast/receipt.edn) |

The comparison charges exactly the original 61 fast members in every arm; c additionally runs the shipped bb union. The top deltas below are computed from the three linked receipts, sorted by c minus a.

| Namespace | a JVM (ms) | b JVM (ms) | c (ms) | c − a (ms) | c runtime |
|---|---:|---:|---:|---:|---|
| clj-surgeon.splice-envelope-test | 834 | 1297 | 28591 | 27757 | :bb |
| clj-surgeon.rename-alias-test | 3265 | 4520 | 6478 | 3213 | :bb |
| clj-surgeon.insert-forms-test | 2017 | 2519 | 5222 | 3205 | :bb |
| clj-surgeon.mcp-inspect-tool-test | 854 | 2273 | 2018 | 1164 | :jvm |
| clj-surgeon.rename-alias-receipt-test | 414 | 673 | 1553 | 1139 | :bb |
| clj-surgeon.mcp-compact-relations-test | 7273 | 8077 | 8079 | 806 | :jvm |
| clj-surgeon.mcp-operation-registry-test | 3651 | 3867 | 4285 | 634 | :jvm |
| clj-surgeon.mcp-namespace-split-test | 2060 | 2055 | 2453 | 393 | :jvm |
| clj-surgeon.outline-memory-test | 476 | 477 | 651 | 175 | :jvm |
| clj-surgeon.receipt-booleans-test | 337 | 263 | 475 | 138 | :jvm |

The dominant cost is execution of splice-envelope-test under bb. Its b JVM measurement is close to a; the hybrid increase concentrates in portable namespaces executing under bb. This is not evidence that the bb union was mistakenly charged to fast. Neither a common heap cap nor unchanged automatic width explains this concentration. Both runners require namespaces before starting their namespace timers, so JVM process startup is not charged to these namespace walls (test/run_all.clj and test/clj_surgeon/mcp_test_runner.clj).

A scheduling barrier does not remove intrinsic bb execution cost. No coordinator-only fix is established by these observations, and changing runtime classification for speed is explicitly forbidden in step 2. The one required make test-fast was run unchanged and triggered the stop.

## Method and limitations

- Single observations, no variance floor: these are localization findings, not a certified performance claim.
- Base uses JVM coordination; subject uses bb. Runtime assignments change between JVM-only b and shipped c by experimental design.
- Scheduling cost caches differ and b updates the subject fast cache before c; saved plans expose the resulting grouping. Width is eight throughout, but process counts differ.
- The repeated b/c pair is sequential after a, not a new matched a/b/c wave; base was not rerun.
- All a/b/c JVM workers explicitly use 1 GiB; absolute sums remain provisional. Make test-fast uses the shipped explicit 512 MiB JVM command-line cap with the same JAVA_TOOL_OPTIONS.
- Secondary contention cannot be excluded; the measurements do not isolate a small effect from ordinary variation.
- Coordinator makespan is timed inside execute-plan!, not complete shell launch wall. JVM classpath preparation uses its shipped 512 MiB cap.

Run a used the existing verified detached base checkout and the JVM test-deps entrance. Runs b/c used bb and measure.clj at the subject. The harness changes only worker heap options in a/c; b additionally selects the original fast set and forces JVM before planning. Exact worker plans are in each measurement-plan.edn. The coordinator's printed estimates precede the harness heap override.

The first b/c pair is preserved in b-subject-jvm-contaminated*/c-subject-contaminated*. A log modification under docs/observations triggered the b ns-isolation witness. The corrected pair wrote live evidence under /var/tmp/forge/bbtower-fx/attempt7 and was copied here only after all suites ended. Base logs were already outside the base checkout. No failed observation is presented as green.

Reproduction: from the appropriate checkout, use JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx', TMPDIR=/var/tmp/forge/bbtower-fx and unset _JAVA_OPTIONS. For a: clojure -J-Xms64m -J-Xmx1g -M:clj-surgeon/test-deps ABS/measure.clj a EXTERNAL-OUTPUT. For b/c: bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx ABS/measure.clj ARM EXTERNAL-OUTPUT. Run sequentially. make test-fast used the same environment. No outer timeout or shared-server entrance was used.
