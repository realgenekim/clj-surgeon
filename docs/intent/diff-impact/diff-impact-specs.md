# Diff-impact requirements

Parent: [selection design](design.md). Plan: [round 1](../../plans/diff-impact-edges.md).

- [x] **DIFF-IMPACT-001**: When an existing repository data file under docs/ or resources/ is changed, the oracle shall select each test namespace containing its string path, and each transitive test dependent. A composed path with a literal directory prefix shall conservatively depend on that directory's existing files.
- [x] **DIFF-IMPACT-002**: When source text changes, the oracle shall select tests with that source file literal and tests slurping or scanning its literal source root, including directory walks and scan helpers, without requiring a namespace edge. It shall ignore comments, regex contents, absent files and escaping paths.
- [x] **DIFF-IMPACT-003**: When a namespace file under test/ changes, the oracle shall seed that namespace and select every direct or transitive test dependent using the same fixed point as src/, preserving require-edge behavior and terminating on cycles.
- [x] **DIFF-IMPACT-004**: When any changed file is unmatched, every non-list mode shall write and print :status :hold-unmatched-files, :reason :unmatched-files and each unmatched file/reason, then exit 1 before launching children, even if other files select tests. List mode shall retain that typed inventory and exit 0 as selection evidence only. Only an empty diff may emit :nothing-selected and exit 0 without running tests; the tracked no-test-can-depend.edn allowlist is empty, witnessed as empty, and grants no exemptions. Neither case shall read a missing results file. A seed with any declared dependent but no reachable test shall be :no-dependency-edge; :no-test-dependent is reserved for an isolated namespace seed.
- [x] **DIFF-IMPACT-005**: When selection completes, the oracle shall retain deterministic edge kinds and counts and print each selected namespace's changed-file reason as `selected <ns> via <edge-kind> <file>`. list and fixed-point shall share the selector; the third positional argument shall remain the mode.

- [x] **DIFF-IMPACT-006**: When a test transitively requires a source namespace containing a bounded repository file literal or source-scan root, the oracle shall propagate those content edges through the same require closure, preserving root bounds, cycles and disconnected negatives. EDN-config value paths and relative io/resource names remain outside bounded resolution; their unmatched changed files shall HOLD under DIFF-IMPACT-004.

- [x] **DIFF-IMPACT-007**: When an explicit selected lane subset and selection-receipt SHA-256 are supplied,
the lane runner shall admit only distinct members of that suite, run exactly that
subset through its normal scheduler and child isolation, and emit `:partial true`,
`:selected` and `:selection-sha`. Census shall detect missing, duplicate and outside
runs against the subset. Full-suite validation and battery freshness shall refuse
a partial receipt with `partial-receipt-not-a-gate-receipt`.

Fixed-point shall execute fast/all as separately scoped observations, selecting
fast members for fast and every selected member through its own lane for all.
Registered dedicated witnesses shall run serially through the existing observed
runner, preserving the analyzer mission and the memory entrance's exclusive lock.
Unknown membership shall refuse before any run. Each observation shall record wall,
per-namespace results, lane receipts and selection provenance. Existing HOLD and
nothing-selected precedence is unchanged. A fixture comparison shall witness
per-namespace verdict parity with a full fast run and print measured walls.
