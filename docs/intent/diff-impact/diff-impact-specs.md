# Diff-impact requirements

Parent: [selection design](design.md). Plan: [round 1](../../plans/diff-impact-edges.md).

- [x] **DIFF-IMPACT-001**: When an existing repository data file under docs/ or resources/ is changed, the oracle shall select each test namespace containing its string path, and each transitive test dependent. A composed path with a literal directory prefix shall conservatively depend on that directory's existing files.
- [x] **DIFF-IMPACT-002**: When source text changes, the oracle shall select tests with that source file literal and tests slurping or scanning its literal source root, including directory walks and scan helpers, without requiring a namespace edge. It shall ignore comments, regex contents, absent files and escaping paths.
- [x] **DIFF-IMPACT-003**: When a namespace file under test/ changes, the oracle shall seed that namespace and select every direct or transitive test dependent using the same fixed point as src/, preserving require-edge behavior and terminating on cycles.
- [x] **DIFF-IMPACT-004**: When no namespace is selected, the oracle shall emit :status :nothing-selected with :changed-files, per-file :reason values and an empty :namespaces vector, exit 0 and never read a missing results file. This is selection evidence, not a passing test run.
- [x] **DIFF-IMPACT-005**: When selection completes, the oracle shall retain deterministic edge kinds and counts and print each selected namespace's changed-file reason as `selected <ns> via <edge-kind> <file>`. list and fixed-point shall share the selector; the third positional argument shall remain the mode.
