# Test namespace registration

The approved [HLD](../../high-level-design.md#one-entrance-for-test-namespace-registration-inb-b10334)
is implemented by a shared, data-only snapshot model in `clj-surgeon.test-registration`.
The source scanner supplies namespace forms and top-level deftests; the model reads
literal registration collections without evaluating repository code. Each oracle
row names surface, file, form, actual and expected values. The ordinary lane and
census failures append the same model's checklist and a shell-quoted Make remedy.

The planner resolves unique CST owners, appends map/set members, and replaces only
the runtime count integer. It never reprints an entire source owner. Matching
members retain their exact bytes. Existing incompatible lane/runtime/reason values,
missing author metadata, ambiguous syntax, path escape and deleted census names
refuse before writes. The namespace pin counts the union of runtime declarations
and discovered namespaces. New historical membership is always adoption.
BB_INELIGIBLE is EDN `{:reasons #{<existing vocabulary keyword>} :detail "..."}`;
it is evidence interpretation, never a substitute for executing a control.

Control execution uses the existing attempt22 portability runner, once for bb
load and, if it loads, once each for JVM and bb tests. Commands are argv vectors;
JVM heap is 1024 MB. The parent records the actual process exit with the runner's
result, command and subject. Existing valid controls are reused. Failed or absent
controls cannot become a passing declaration. Projection uses the existing fold
format; only the requested namespace's control artifacts and the two generated
inventory files are published. No all-corpus runner is invoked.

Publication is serialized by a root-keyed directory lock in external scratch. All
source candidates are parsed before writing. Registration is temporarily installed
for the focused runner to load the correct lane. Execution failure restores all
registration and control bytes; its diagnostic reports the real failed execution.
This is process-local rollback, not crash-atomic multi-file publication. Concurrent
noncooperating source writers are outside that guarantee and snapshots are checked
before publication. A successful second invocation performs no subprocess or write.

Tests remain in the already registered lane-manifest-test namespace. A copied
repository with a real fixture namespace demonstrates author metadata, enrollment,
actual focused control results, whole oracle success, byte identity on repeat and
conflict preservation. Pure tests exhaust missing-surface combinations and CST
preservation; boundary tests cover root scanning and execution refusal. Protected
battery ledgers are never output paths.

The initial entrance admits `.clj` test files and the repository's literal
map/set registration shapes. Other source shapes refuse rather than being
rewritten heuristically. Runtime measurements are read from the root snapshot;
historical control overrides retain their existing paths. The bb-load exclusion
is exactly the existing gate's JVM-assignment exception; it is not a passing JVM
test claim. Existing valid historical controls are reused without a new freshness
claim. A killed process may leave the named directory lock for manual recovery.
