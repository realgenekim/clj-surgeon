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

Fast pure and stubbed-boundary witnesses remain in lane-manifest-test, with one
copied-snapshot first-contact mask. The complete cold gate matrix lives in
test-registration-battery-test (:battery), enrolled through register-test-ns.
It seeds a copied repository through actual registration controls and copies
that seed into a fresh repository for each of the 31 missing-surface masks and
the zero-mask control. Every cell launches real `make -C <copy> test-fast`.
The first emitted checklist names only the defective subject
and its requested lane/runtime; count-only drift emits a repository row instead.
The fast qualified name and all original test names remain, keeping the census
additions-only. The executed path/acceptance matrix and writer census live in
registration-controls-test. Protected battery ledgers are never output paths.

The initial entrance admits `.clj` test files and the repository's literal
map/set registration shapes. Other source shapes refuse rather than being
rewritten heuristically. Runtime measurements are read from the root snapshot;
historical control overrides retain their existing paths. The bb-load exclusion
is exactly the existing gate's JVM-assignment exception; it is not a passing JVM
test claim. Historical controls remain diagnostic inputs to the ordinary inventory oracle only; the mutation entrance never reuses them as evidence. A killed process may leave the named lock file for manual recovery.

Round 3 class repairs: the earliest registry-sensitive assertion in each ordinary
gate carries the complete checklist and exact remedy. The cold copied-repository matrix inspects the first emitted checklist, including
Sol's runtime declaration assertion, without replacing repository Vars or test bodies. Census derivation, removed-name refusal and
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

Round 6 (REAL-GATE-001): a pinned-count discrepancy is one repository fact.
Namespace checklists describe local missing registrations; a complete registered
namespace never inherits a count failure. The repository row records the actual
pin, discovered/runtime union count and disk-minus-manifest subjects. With no
such subjects (the count-only mask), it remains a repository-only diagnostic.
The ordinary gate shares one checklist calculation per namespace run, discarded
between runs; root-specific copied specimens always calculate their own snapshot.
The battery replaces the old redefined/eval matrix with 31 cold runner executions
on copied repository files, plus the valid zero-mask control. The fast mask removes
one real namespace's declarations in a copied snapshot and reads the real count.
Original qualified test names remain; the old matrix digest is retired because it
froze the seam that concealed this defect. No product output path is based on the
operator's scratch path; the harness inherits its admitted temporary directory.

The Round 6 direct-lane matrix had a declared namespace budget of 800000 ms
(393668 ms before projection sharing; 337496 ms afterward), declared through
the existing namespace override map. Its nested fast namespace retains 8000 ms,
and the battery cadence retains 1800000 ms. The matrix asserts the nested budget
for every cell, including expected refusals.

Round 7 isolation (REGNS-012): the matrix receives explicit source and scratch
roots, and the file fixture receives its repository root. Each mask runs real
`make -C <copy> test-fast` with cwd set to that copy. Each cell owns a scratch
envelope containing its repository, temporary directory and external state root;
the state root is explicitly admitted through the artifact-root environment.
Inherited checkout routing and write authorization are discarded. A separate
real manifest JVM observes the caller during seed registration. Its direct
runner mode folds isolation and budget violations into the exit status.
The witness compares git
status and SHA-256 snapshots of registration/census/control surfaces and caller
state walls/controls, including after failure. Child failures report their root,
command and separate diagnostic/budget results so expected copied failures cannot
be mistaken for live-tree failures. The trace and baseline must establish the
actual failure; no contamination RED may be inferred from nested log text alone.
The complete Round 7 witness measured 1332598 ms and has a 1500000 ms namespace
ceiling. The fast namespace's 8000 ms ceiling and battery cadence's independent
1800000 ms ceiling remain in force.
