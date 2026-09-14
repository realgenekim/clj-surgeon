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
result, command and subject. Every invocation executes fresh controls; saved receipts are never execution evidence. After execution, a pre-existing receipt with a different source digest or missing execution provenance refuses with `:register-control-stale`. Receipts record argv, canonical root, source SHA-256 at execution, exit/result, wall time, pid and Linux start ticks. Failed or absent
controls cannot become a passing declaration. Projection uses the existing fold
format; only the requested namespace's control artifacts and the two generated
inventory files are published. No all-corpus runner is invoked.

Publication is serialized by a lock file inside the canonical registration root.
A same-directory prepared holder record (pid/root) is published by atomic hard link;
only an existing lock means busy. Request/snapshot validation precedes acquisition,
and the snapshot is rechecked after acquisition. Every control child is recorded
in the TEST-ISO-002 spawn ledger immediately after launch. All
source candidates are parsed before writing. Registration is temporarily installed
for the focused runner to load the correct lane. Execution failure restores all
registration and control bytes; its diagnostic reports the real failed execution.
This is process-local rollback, not crash-atomic multi-file publication. Concurrent
noncooperating source writers are outside that guarantee and snapshots are checked
before publication. A repeated invocation preserves enrollment bytes but executes and refreshes control artifacts.

Fast pure and stubbed-boundary witnesses remain in lane-manifest-test, with one ordinary-order first-contact mask. The complete 32-mask gate matrix lives in test-registration-battery-test (:battery), enrolled through register-test-ns. Its matrix body is preserved byte-for-byte from 99b57bd1; a source witness freezes the original test names and matrix digest. The existing qualified fast name remains the representative witness, so census changes are additions only. The executed path/acceptance matrix and writer census live in registration-controls-test, enrolled by the entrance itself. A copied
repository with a real fixture namespace demonstrates author metadata, enrollment,
actual focused control results, whole oracle success, preserved enrollment bytes on repeat and
conflict preservation. Pure tests exhaust missing-surface combinations and CST
preservation; boundary tests cover root scanning and execution refusal. Protected
battery ledgers are never output paths.

The initial entrance admits `.clj` test files and the repository's literal
map/set registration shapes. Other source shapes refuse rather than being
rewritten heuristically. Runtime measurements are read from the root snapshot;
historical control overrides retain their existing paths. The bb-load exclusion
is exactly the existing gate's JVM-assignment exception; it is not a passing JVM
test claim. Historical controls remain diagnostic inputs to the ordinary inventory oracle only; the mutation entrance never reuses them as evidence. A killed process may leave the named lock file for manual recovery.

Round 3 class repairs: the earliest registry-sensitive assertion in each ordinary
gate carries the complete checklist and exact remedy. A 32-mask witness executes
the real gate Vars in namespace order and inspects the first failure, including
Sol's runtime declaration assertion. Census derivation, removed-name refusal and
canonical one-name-per-line serialization belong to `clj-surgeon.test-census`;
both registration and explicit regeneration delegate to it. The registration
transaction supplies its guarded writer to retain rollback. All working paths
come from the canonical repository root or inherited admitted temporary root.

The runner clears `CLJ_SURGEON_TMPDIR_REEXEC` before starting a control so
nested runners create their own cleanup roots. Each argv receives the inherited
admitted temporary base through both environment and startup property. A bb load
failure still requires the JVM test to execute and pass; the shared portability
classifier accepts that executed JVM row beside the bb load refusal. Unknown
process provenance refuses rather than reporting registration success.
