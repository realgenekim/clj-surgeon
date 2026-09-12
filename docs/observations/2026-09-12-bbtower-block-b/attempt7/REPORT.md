# Block B — STOPPED

The required make test-fast reports **68664 ms against 60,000 ms**. Step 2's stop is honored; steps 3–6 were not started. No production source, budget, membership or runtime classification changed. No push, tag, shared install or prewarm was performed.

| Arm | Original fast sum (ms) | All namespace sum (ms) | Makespan (ms) | Width / processes | Result | Logs |
|---|---:|---:|---:|---|---|---|
| a-base | 36335 | 36335 | 27638 | 8 / 8 | passed | [log](a-base.log), [receipt](a-base/receipt.edn) |
| b-subject-jvm | 37661 | 37661 | 26957 | 8 / 8 | passed | [log](b-subject-jvm.log), [receipt](b-subject-jvm/receipt.edn) |
| c-subject | 68679 | 302525 | 82699 | 8 / 22 | failed | [log](c-subject.log), [receipt](c-subject/receipt.edn) |
| test-fast | 68664 | 302378 | 82426 | 8 / 22 | failed | [log](test-fast.log), [receipt](test-fast/receipt.edn) |

## Finding and decision

- The original 61 fast members are identical at base and subject.
- The dominant delta is splice-envelope-test under bb; its subject JVM wall remains small.
- No coordinator contention cause is established. Step 2 forbids runtime changes for speed; no speculative coordinator patch was made.
- make test-fast exceeded the unchanged 60000 ms ceiling; steps 3 through 6 were stopped.
- The corrected hybrid measurement and make test-fast also fail the CLI-output length witness in intent-transaction-test.
- The first b/c logs were written in the checkout and invalidated isolation. They are retained as contaminated, not acceptance evidence.

The coordinator-only implementation is **not completed**: the evidence does not support claiming that scheduling fixes the dominant runtime cost. A speculative barrier was not substituted for a demonstrated fix. Detailed deltas, exact commands, discarded-run failures and qualifications are in [slowdown.md](slowdown.md). The separate intent-transaction failure is in [c lane 0](c-subject/lane-0.out) and [fast lane 0](test-fast/lane-0.out).

## BB policy and remaining contract

No bb ceiling was selected. The original bb inventory's sum in the required fast run is 233838 ms; all bb-runtime namespaces sum to 279148 ms ([receipt](test-fast/receipt.edn)). These are distinct sets and neither is an admitted ceiling. Step 3 requires a new declaration and Fable review after step 2 passes; it is not restoration of a pre-existing budget. Feature-thread, integration and prewarm measurements are unknown.

## DOGFOOD — every source edit

| File | Intent | Mechanism | Refusal type | Repair text sufficed |
|---|---|---|---|---|
| [measure.clj](measure.clj) | Create a/b/c measurement harness and save exact plans/receipts | native apply_patch on exact forms | none | n/a |
| [measure.clj](measure.clj) | Force b runtime map before partitioning, so disabling bb does not create spurious split jobs | native apply_patch on exact forms | none | n/a |
| [summarize.clj](summarize.clj) | Create and complete this form-based receipt summarizer; derive report numbers without manual transcription | native apply_patch on exact forms | none | n/a |
| [summarize.clj](summarize.clj) | Remove unused row destructuring after lint named the unused bindings | native apply_patch on exact forms | none | n/a |

Only observation tooling was edited. measure.clj was executed through the real base and subject coordinator entrances; summarize.clj derives this report from their receipts. Runtime-map adaptation was made before b ran. The log-location repair came from the failing isolation assertion, not an editor refusal. No Surgeon operation was attempted and no tool refusal is invented. docs/tech-tree.md records the finding through a separate native prose patch. report.edn is serialized from Clojure data by summarize.clj. Initial lint identified unused bindings (repaired) and duplicate requires from analyzing both standalone user-namespace scripts together. Each script then passed separately through ~/bin/clj-kondo with zero errors/warnings; see lint-initial.log, lint-measure.log and lint-summarize.log.

## Least sure and disagreements

- Single observations, no variance floor: these are localization findings, not a certified performance claim.
- Base uses JVM coordination; subject uses bb. Runtime assignments change between JVM-only b and shipped c by experimental design.
- Scheduling cost caches differ and b updates the subject fast cache before c; saved plans expose the resulting grouping. Width is eight throughout, but process counts differ.
- The repeated b/c pair is sequential after a, not a new matched a/b/c wave; base was not rerun.
- All a/b/c JVM workers explicitly use 1 GiB; absolute sums remain provisional. Make test-fast uses the shipped explicit 512 MiB JVM command-line cap with the same JAVA_TOOL_OPTIONS.
- Secondary contention cannot be excluded; the measurements do not isolate a small effect from ordinary variation.
- Coordinator makespan is timed inside execute-plan!, not complete shell launch wall. JVM classpath preparation uses its shipped 512 MiB cap.

Fable has not reviewed this attempt. The disagreement is with the assumption that a coordinator-only fix follows from the measured slowdown. Independent acceptance remains external.

## Owed

- Fable review of the finding and authority to address fast-member bb runtime cost without violating step 2.
- A supported implementation fix and a passing fast lane.
- A NEW bb ceiling, derived only after a passing step 2 measurement, its TEST-ISO-007 declaration and boundary witnesses; Fable ratification.
- Probe refusal registry/completeness witness and receipt booleans with false seams for the third verb.
- Second receipt-boolean encounter scoring: not attempted because step 4 was not reached.
- Tip feature-thread bb/JVM measurements, measured runtime assignment and unchanged integration gate.
- Final-tip prewarm and any authorized repair/retry; zero prewarm attempts were used.
- Independent review; no built/certified probe claim.

Finding commit: `fd00e46b8a26b0db1b23764522f8a23e788a8878`. This directory is committed last; its own commit SHA is intentionally not self-embedded. Measured source: `25137b25b6145b0870d3c006ccf0c33ccb10e312`; base: `eae1e43280635be9b4e317e176d29476fd358702`. [Machine report](report.edn), [meter](meter.tsv), [verbatim gate lines](gate.md).
