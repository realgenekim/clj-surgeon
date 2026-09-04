---
parent: test-isolation-design
prefix: TEST-ISO
status: "round two implemented 2026-09-04; spike docs/observations/2026-09-04-suite-spike-spec.md"
---

# JVM Test-Suite Isolation Specifications

Gene, filing the spike (2026-09-04): *"I think a spike to clean up JVM test
suite and speed it up and ensure pure and at least tests that don't interfere
with each other is definitely warranted. Unacceptable that we have to gate
parallelism because of tests!!!"* and *"We need LID rules on our tests to make
sure they don't interfere!!!!"*

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

Round two builds 001, 006 and 009 plus the two race fixes round one measured.
002-005, 007, 008, 010-012 are FILED and are round three's work; they are
listed here with `[ ]` so the family is one document rather than a memory.

## Round two -- implemented

- [x] **TEST-ISO-001**: Every JVM test namespace shall declare its lane --
  `:fast`, `:integration` or `:battery` -- in its own ns metadata, and that
  declaration shall agree with `clj-surgeon.lane-manifest/manifest`, which is
  the single authority the runner reads. When the runner is asked to run a
  namespace the manifest does not declare, or a namespace whose loaded ns
  metadata disagrees with the manifest, clj-surgeon shall refuse with a typed
  message naming the subject and the remedy and exit non-zero, rather than
  silently skipping it.
  *Witness:* `clj-surgeon.lane-manifest-test/the-runner-refuses-an-undeclared-namespace`,
  `.../loaded-namespaces-carry-their-lane-at-runtime`.

- [x] **TEST-ISO-001a**: The manifest, the ns metadata, and the `*_test.clj`
  files on disk shall be checked for SET EQUALITY IN BOTH DIRECTIONS: a
  manifest entry with no file, and a test namespace on disk that is in no
  lane, in `test/run_all.clj`, or in a declared-exclusion map carrying a
  reason, shall each fail by name. Absence shall be as loud as presence.
  Every exclusion shall be a REDIRECTION and never a declaration of
  orphanhood: its reason shall name a `make <target>` or a
  `:clj-surgeon/<alias>` that EXISTS in the tree and runs it, so that a
  namespace no runner runs cannot be declared away -- it can only be adopted
  into a lane or deleted.
  *Witness:* `.../every-manifest-entry-exists-on-disk`,
  `.../every-test-namespace-on-disk-is-accounted-for`,
  `.../excluded-entries-are-real-and-carry-a-reason`,
  `.../every-exclusion-names-a-runner-that-actually-exists`.

- [x] **TEST-ISO-001b**: Partitioning shall never become dropping: the 49
  namespaces round one measured shall all remain in some lane, checked
  against a pinned set, so that a green suite with less in it is
  impossible. The corpus shall only ever GROW against round one's measured
  865 tests, and the growth shall be shown as arithmetic at the pin: the 49
  measured namespaces declare at least 865 tests today, every namespace in a
  lane that round one did not measure is enumerated with its exact test
  count, and the two shall sum to the manifest's total.
  *Witness:* `.../the-partition-drops-nothing-round-one-measured`,
  `.../the-corpus-only-ever-grows-and-the-arithmetic-is-shown`.

- [x] **TEST-ISO-001c**: The lane manifest shall declare, in the SAME source
  of truth as the lane, the CADENCE at which each lane runs -- `:fast` every
  run, `:integration` the merge gate with fast, `:battery` before every
  landing under `flock /home/forge/tmp/suite.lock` and nightly on the trunk
  tip. A lane with no cadence, a cadence the runner does not know, or a
  manifest namespace that resolves to neither shall be a refusal, and the
  runner's lane refusal shall NAME the cadence beside each lane, because the
  lane chosen decides how often the test runs and a refusal that hides that
  makes the choice look free.
  *Witness:* `clj-surgeon.lane-manifest-test/every-lane-declares-a-cadence-the-runner-knows`,
  `.../every-manifest-namespace-resolves-to-a-known-cadence`,
  `.../the-refusal-message-names-the-cadence-a-lane-costs`.
  *(Building the nightly cron and a cadence receipt ledger is round three.)*

- [x] **TEST-ISO-006**: Any run that contains no BATTERY namespace -- the
  fast lane, the integration lane, and the merge gate that is both -- shall
  have its JVM launched with both
  `java.io.tmpdir` and `user.home` on throwaway directories created for that
  run and deleted when it ends, so that a fast-lane test cannot read the
  seat's real home state or leave anything in it. Both shall be set as
  process-startup `-D` flags (a runtime `System/setProperty` is not honoured
  for real file creation), `HOME` shall be set in the child's environment to
  the same directory so descendants agree, and a run that asked for an
  isolated home and is not running on one shall refuse, typed, rather than
  proceed. The decision shall be a property of the RESOLVED NAMESPACE SET and
  not of how the invocation was spelled, so that the merge gate cannot run
  uninsulated while a fast-lane witness asserts that it did not.
  *Witness:* `clj-surgeon.fast-lane-isolation-test` (all three deftests),
  plus the planted-sabotage receipt in
  `docs/observations/2026-09-04-suite-spike-round2.md`.

- [x] **TEST-ISO-009**: `make suite-concurrency-battery N=<n>` shall make N
  real clones of the tip and run the merge-gate lane in all N concurrently,
  and shall pass ONLY when every clone reports zero failures and zero errors
  AND exits zero. One clean clone beside one failing clone shall be a
  failure, never a partial pass.
  *Witness:* `test/suite_concurrency_battery.sh`; three runs recorded in
  `docs/observations/2026-09-04-suite-spike-round2.md`.

- [x] **TEST-ISO-RACE-001**: A test's child-process teardown shall not be
  able to throw. `stop-child!` shall return a typed receipt naming any
  teardown failure and shall never propagate one, and the workspace's
  deletion shall be owned by a fixture rather than by a step sequenced after
  a call that can throw -- so that a teardown exception cannot skip cleanup
  and turn one race into one error plus one temp leak.
  *Witness:* `clj-surgeon.mcp-prepared-wire-test/stop-child-reports-a-stderr-reader-failure-as-a-typed-fact`,
  `.../a-teardown-failure-still-deletes-the-workspace`, and the contention
  runs recorded in the round-two observation.

- [x] **TEST-ISO-RACE-002**: A test shall not assert on a wall-clock margin
  it does not control. Every RENDEZVOUS bound -- "did the other party show
  up?" -- shall use one named, environment-overridable ceiling; every
  CONTRACT bound shall be asserted where the contract lives (`elapsed >=
  timeout`), and where a ceiling is genuinely needed to discriminate two
  hypotheses, the SIGNAL shall be widened rather than the tolerance
  narrowed. A rendezvous shall additionally be WINNABLE: the window in which
  the other party is observable shall outlast the observation latency.
  *Witness:* `clj-surgeon.mcp-process-test` under
  `dev/experiments/contention_witness.sh`, 3/3 all-green at load 22-31.

## Round three -- filed

- [ ] **TEST-ISO-002**: A fast-lane test shall spawn no process, witnessed at
  runtime by a `ProcessHandle.current().descendants()` diff around each
  namespace. (A source scan corroborates today and is explicitly NOT the
  proof: it reads a spelling and a helper in another namespace defeats it.)
- [ ] **TEST-ISO-003**: A fast-lane test shall write only inside its own
  tmpdir subtree, witnessed by a diff of the temp root, `target/`, and
  `git status --porcelain` around each namespace, failing with the path.
- [ ] **TEST-ISO-004**: No fixed ports; every server start through a port-0
  allocator with a ledger, witnessed by a listener diff per namespace.
- [ ] **TEST-ISO-005**: No global mutation leaks; every project var root and
  known global atom snapshotted around each namespace, failing by name.
- [ ] **TEST-ISO-007**: A per-namespace and per-lane time budget, the timing
  reporter failing a namespace over budget with its wall.
- [ ] **TEST-ISO-008**: Order independence -- shuffled namespace order with a
  printed seed, two seeds per gate, a one-seed failure reproducible from it.
- [ ] **TEST-ISO-010**: No thread or executor leaks (live non-daemon thread
  count around each namespace).
- [ ] **TEST-ISO-011**: No sleeps or polls in the fast lane outside one
  sanctioned wait helper.
- [ ] **TEST-ISO-012**: No two fast-lane namespaces share a mutable resource
  -- a relational oracle, KEPT ONLY if it finds a counterexample the native
  witnesses missed.
