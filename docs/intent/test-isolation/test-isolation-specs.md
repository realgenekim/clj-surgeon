---
parent: test-isolation-design
prefix: TEST-ISO
status: "round four implemented 2026-09-04 (002/003/004/005/007/010 runtime witnesses); spike docs/observations/2026-09-04-suite-spike-spec.md, record -round4.md"
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
Round FOUR builds the six RUNTIME purity witnesses -- 002, 003, 004, 005, 007
and 010 -- on ONE per-namespace snapshot fixture
(`clj-surgeon.ns-isolation`, driven by `clj-surgeon.mcp-test-runner`).
008, 011 and 012 remain FILED and are listed with `[ ]` so the family is one
document rather than a memory.

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

- [x] **TEST-ISO-009a**: Every run of the battery lane shall append ONE line
  to `docs/observations/battery-ledger.edn` naming `{:sha :started :wall_s
  :verdict :host}`, whether it passed or FAILED. The ledger shall be
  append-only -- appending shall never read or rewrite what is already there
  -- and an entry that does not read shall be NAMED rather than skipped, so a
  corrupted receipt cannot make the ledger look merely shorter. The runner
  writes the file; the seat commits it.
  *Witness:* `clj-surgeon.battery-ledger-test/an-entry-round-trips-through-one-line`,
  `.../appending-never-rewrites-what-is-already-there`,
  `.../a-failed-battery-is-still-recorded`,
  `.../the-make-targets-are-wired-to-this-mechanism`.

- [x] **TEST-ISO-009b**: `make battery-fresh` shall REFUSE, naming its subject
  and its number and printing the exact remedy, when the newest battery
  receipt is older than 26 h, when it records a failure, when the commit it
  names is not an ancestor of HEAD, when that commit is more than 30 counted commits
  behind HEAD, or when the ledger is empty or corrupt. The battery is out of
  the merge gate by design, which makes its ABSENCE silent; a stale nightly
  shall therefore be a refusal rather than a silence. The verdict shall be a
  PURE function of the entries, the instant, and an injected ancestry lookup,
  so that every refusal state is reachable in the fast lane without a `git`
  call, a clone, or the passage of a day.
  *Witness:* `clj-surgeon.battery-ledger-test/a-fresh-receipt-on-this-tree-passes`,
  `.../an-empty-ledger-refuses`,
  `.../a-receipt-older-than-the-budget-refuses-and-names-the-age`,
  `.../a-failed-newest-receipt-refuses`,
  `.../a-receipt-from-a-tree-this-one-does-not-descend-from-refuses`,
  `.../a-receipt-more-than-thirty-commits-behind-refuses`,
  `.../a-corrupt-line-refuses-rather-than-shortening-the-ledger`,
  `.../every-refusal-carries-the-remedy`.

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

## Round four -- the six runtime purity witnesses, implemented

ONE mechanism carries all six: `clj-surgeon.ns-isolation/probe` takes a
snapshot of every watched resource, `clj-surgeon.mcp-test-runner` runs each
namespace between a pair of them, and `clj-surgeon.ns-isolation/violations` --
a PURE function of (namespace, before, after, opts) -- turns the difference
into typed refusals. The probe emits FACTS and the fold emits VERDICTS, which
is what makes every refusal below reachable from a fast-lane witness without
committing the violation it detects.

Which lane is held to which rule is DATA, in
`clj-surgeon.ns-isolation/enforced-intents-by-lane`, and pinned by
`.../each-lane-is-held-only-to-the-rules-that-lane-can-keep`: the fast lane is
held to all six; the integration lane to 002, 007 and 010 (binding an
ephemeral port and writing a per-test workspace are what put a namespace in
it); the battery lane to 007 alone (it exists to launch cold child JVMs).

- [x] **TEST-ISO-002**: A fast-lane test shall spawn no process. Every
  namespace shall run between a `ProcessHandle.current().descendants()` diff,
  and a descendant alive after a namespace that did not exist before it shall
  be a typed refusal naming the namespace, the PID and the CHILD'S COMMAND
  LINE. The source scan corroborates and is explicitly NOT the proof: it reads
  a spelling, and a helper in another namespace defeats it.
  *Witness:* `clj-surgeon.ns-isolation-test/a-child-process-fails-the-namespace-by-pid-and-command-line`,
  `.../every-violation-names-its-intent-its-namespace-and-its-resource`.

- [x] **TEST-ISO-003**: A fast-lane test shall write only inside its own
  subdirectory of `java.io.tmpdir`, witnessed by a diff of the run's temp
  root, of `target/`, and of the repository working tree around each
  namespace, failing with the PATH. A new top-level temp entry shall be
  allowed only when it is the running namespace's own allocated subdir
  (`clj-surgeon.ns-isolation/namespace-tmp-dir-name`). The working-tree check
  shall be performed IN-PROCESS rather than by `git status`, because a witness
  that spawns a child to prove no child was spawned is blind to its own
  subject.
  *Witness:* `clj-surgeon.ns-isolation-test/a-write-outside-the-namespaces-own-tmp-subdir-fails-with-the-path`,
  `.../the-fixture-catches-a-real-write-outside-the-namespaces-own-subdir`
  (the real probe, end to end, with no planted map).

- [x] **TEST-ISO-004**: No fixed ports. Every server start shall go through
  the port-0 allocator (`clj-surgeon.ns-isolation/allocate-port!`), which
  records every allocation in a ledger, and a socket THIS JVM is still
  listening on after a namespace that it was not listening on before shall be
  a typed refusal naming the PORT and saying which of the two failures it is:
  an allocation that leaked (it is in the ledger) or a fixed port literal (it
  is not). The listener scan shall be restricted to sockets this process owns,
  read from `/proc/self/fd`, so that another seat's listener on a shared box
  can never fail this lane.
  *Witness:* `clj-surgeon.ns-isolation-test/a-leaked-listener-fails-naming-the-port-and-whether-it-was-allocated`,
  `.../the-port-allocator-hands-out-ephemeral-ports-and-records-every-one`.

- [x] **TEST-ISO-005**: No global mutation leaks. Every var interned in a
  loaded `clj-surgeon.*` namespace shall have its ROOT OBJECT IDENTITY
  snapshotted around each namespace, and a root that changed shall fail naming
  the var -- that is a `with-redefs` or `alter-var-root` that escaped its
  scope, and it shall NOT be exemptible by any allowlist. Separately, the
  VALUE held by an atom, ref or volatile a var's root IS shall be snapshotted,
  and a change shall fail naming the var unless it is declared in
  `clj-surgeon.ns-isolation/mutable-global-allowlist` WITH its reason. Roots
  shall be read with `getRawRoot` and never `deref`, so that observing a
  `delay` or a `promise` cannot realise it.
  *Witness:* `clj-surgeon.ns-isolation-test/a-leaked-with-redefs-fails-naming-the-var`,
  `.../a-mutated-global-atom-fails-unless-it-is-declared-mutable-with-a-reason`,
  `.../the-allowlist-cannot-exempt-a-leaked-var-root`.

- [x] **TEST-ISO-007**: A time budget per namespace AND per lane. A namespace
  over its budget shall fail with its measured WALL, its budget, and the
  remedy; a namespace may raise its own ceiling only through a declared entry
  in `clj-surgeon.ns-isolation/namespace-budget-overrides`. The LANE total
  shall have its own budget -- the fast lane's is the 60 s the partition
  exists to buy -- because 38 namespaces each inside an 8 s ceiling can still
  sum to five minutes, and the sum is the number the fleet pays.
  *Witness:* `clj-surgeon.ns-isolation-test/a-namespace-over-its-budget-fails-with-its-wall`
  (at the ceiling passes, one ms past it refuses),
  `.../the-lane-total-has-its-own-budget-because-the-sum-is-what-the-fleet-pays`.

- [x] **TEST-ISO-010**: No thread or executor leaks. The live NON-DAEMON
  thread set shall be snapshotted around each namespace and a thread alive
  afterwards that was not alive before shall fail naming its id and its name.
  Non-daemon specifically: a leaked non-daemon thread is the one that keeps
  the JVM from exiting, which is how a suite reporting 0 failures still hangs
  a runner until its timeout.
  *Witness:* `clj-surgeon.ns-isolation-test/a-leaked-non-daemon-thread-fails-naming-it`,
  `.../the-thread-probe-sees-this-jvms-real-threads`.

**The family's own ceiling, witnessed once for all six** (an id of its own was
deliberately NOT minted: the property belongs to every rule above, and a
separate id would let one of them be repaired while the others quietly lost
it). The fixture shall never accuse a namespace that did nothing -- a snapshot
pair taken back to back around no work at all produces zero violations,
including from the fixture's OWN probing, which is why the process set is
captured LAST on the way in and FIRST on the way out. A witness that fires on
correct behaviour is one somebody deletes.
*Witness:* `clj-surgeon.ns-isolation-test/a-clean-namespace-produces-no-violations-at-all`,
`.../each-lane-is-held-only-to-the-rules-that-lane-can-keep`.

## Round three -- still filed

- [ ] **TEST-ISO-008**: Order independence -- shuffled namespace order with a
  printed seed, two seeds per gate, a one-seed failure reproducible from it.
- [ ] **TEST-ISO-011**: No sleeps or polls in the fast lane outside one
  sanctioned wait helper.
- [ ] **TEST-ISO-012**: No two fast-lane namespaces share a mutable resource
  -- a relational oracle, KEPT ONLY if it finds a counterexample the native
  witnesses missed.

### TEST-ISO-009b archival distance

Only modifications of existing regular non-executable files at these exact
paths may be excluded from commit distance:

- `docs/observations/2026-09-03-captains-log-anvil-seat.md`
- `docs/observations/2026-09-05-captains-log-astra-four-hour-comparison.md`
- `docs/observations/2026-09-06-live-astra-typist-commentary.md`

Every changed entry against every parent must be status M, mode 100644 before
and after, and one of these paths. Empty commits, additions, deletion, rename,
mode/type changes, mixed commits, unknown paths and unreadable diffs count.
The complete DAG remains authoritative; raw distance and excluded archive count
must accompany counted distance. Above 1000 raw commits, count all commits
without archival exemptions to bound per-commit inspection. Ancestry, age,
newest-failure authority and the 30-commit budget remain unchanged.
