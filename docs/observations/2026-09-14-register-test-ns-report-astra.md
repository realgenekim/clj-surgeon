# Round 1: test namespace registration — Astra

Recorded 2026-09-14T09:48:51.113124+00:00. Branch `fable/regns-entrance`; base `8aedb65e`.
The seat approved HLD `f7094e3a` and all remaining phases in advance.

Implemented all five surfaces, including **executed focused controls**. The
surface-5 fallback was not needed. `make register-test-ns` and the census oracle
share the registration model. Missing or conflicting fields identify file,
form, actual and expected values; ordinary census refusals include the complete
checklist and concrete Make remedy. Author metadata remains the lane authority.
Removed census names still refuse.

## Commits and linked intent

- `d274f96d`: low-level design, requirements and committed RED witnesses, before implementation.
- `802735a5`: structural enrollment, oracle, focused execution, rollback, help and documentation.
- `1ad9b8aa`: stronger filesystem witness invoking the oracle against all missing registrations.

The owning leaf is [test-registration](../intent/test-registration/design.md).
`registration-specs.md` registers REGNS-001 through REGNS-008 using the discovered
`docs/intent/<leaf>/*-specs.md` convention. Implementation and tests carry disjoint
`INTENT:` / `INTENT-TEST:` links plus the repository-audited `@spec` markers.
The affected intent-contract tests passed. Every commit carries all three requested trailers.

## RED first

Command (with `TMPDIR=/var/tmp/forge/regns-fx` and
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/regns-fx`):

```sh
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.lane-manifest-test) (clojure.test/test-vars (map #(ns-resolve 'clj-surgeon.lane-manifest-test %) '[registration-oracle-enumerates-all-surfaces registration-planner-refuses-conflicts registration-structural-plan-preserves-and-repeats registration-control-results-are-executions]))"
```

Observed before implementation, retained in `/var/tmp/forge/regns-fx/red.log`:

```text
FAIL in (registration-oracle-enumerates-all-surfaces)
current census sees membership only; need the complete registry oracle
expected: (some? oracle)
  actual: (not (some? nil))
FAIL in (registration-planner-refuses-conflicts)
FAIL in (registration-structural-plan-preserves-and-repeats)
FAIL in (registration-control-results-are-executions)
```

Four named failures. This initial `test-vars` command reports assertion failures;
it did not translate them into a process exit. The final gate below explicitly
does so. The regression first shows that membership comparison sees a registered
fixture while the other four surfaces are absent. The final root-backed witness
reads those actual fixture files and returns missing surfaces `[2 3 4 5]` in one
report. A separate pure witness exhausts all 32 missing-surface subsets.

## Dogfood and control execution

No permanent new test namespace was added: eight new deftests live in the already
registered `lane-manifest-test`. The new fixture namespace was enrolled using the
entrance itself in a copied repository under `/var/tmp/forge/regns-fx/repo-final`:

```sh
make -C /var/tmp/forge/regns-fx/repo-final register-test-ns \
  NS=clj-surgeon.registration-fixture-test LANE=battery RUNTIME=jvm
```

The author supplied `{:lane :battery}` and a real test asserting `(+ 2 2) = 4`.
Observed output, selected fields from `dogfood-final.log` (exit 0):

```edn
{:ok true :state :registered
 :changes
 [{:file "test/clj_surgeon/deftest_census.edn"
    :forms [{:form deftest-census :before-count 2642 :after-count 2643}]}
  {:file "test/clj_surgeon/lane_manifest.clj"
    :forms [{:form manifest :before-count 157 :after-count 158}
            {:form portability-runtimes :before-count 162 :after-count 163}]}
  {:file "test/clj_surgeon/lane_manifest_test.clj"
    :forms [{:form adopted-since-round-one :before-count 108 :after-count 109}
            {:form every-manifest-entry-exists-on-disk :before-count 162 :after-count 163}]}]
 :controls
 {:bb-load {:status :loaded :exit 0}
  :jvm {:status :passed :exit 0 :result {:test 1 :pass 1 :fail 0 :error 0 :type :summary}}
  :bb {:status :passed :exit 0 :result {:test 1 :pass 1 :fail 0 :error 0 :type :summary}}}}
```

The entrance invokes attempt22's existing `portability_runner.clj` with argv,
records each actual exit, command, source hash and result, then structurally adds
the inventory row and updates the Markdown projection. It does not invoke
`run_census.clj`'s all-namespace loop or replay historical control files through
`fold_census.clj`. The existing fold format remains consumable.

The whole copied-root oracle was run with:

```sh
bb -e '(require (quote [clj-surgeon.test-registration :as r])) (let [xs (r/repository-checklist ".")] (prn {:ok (empty? xs) :missing (mapv :missing xs)}) (System/exit (if (empty? xs) 0 1)))'
```

Output: `{:ok true, :missing []}`, exit 0. Repeating the exact enrollment returned
`{:ok true :state :unchanged :changes [] :artifacts []}`. SHA-256 comparison of
all 10,419 fixture files except `.cpcache` found **zero changed paths**.

Wrong lane produced CLI exit 1 (Make exit 2), with no byte changes:

```edn
{:ok false :changes [] :error-type :register-conflict
 :field :metadata :actual :battery :expected :fast}
```

A separate real failing namespace was enrolled in the first copied root. Both
JVM and bb actually executed its failing assertion: each returned
`{:test 1 :pass 0 :fail 1 :error 0}`, exit 1. The entrance returned
`:register-control-failed`, `:state :rolled-back`, and the actual results.
After removing the author-created failing source, whole-copy comparison against
the pre-refusal snapshot again found zero changed paths. A permanent boundary
witness also checks rollback with a controlled execution-result seam.

## Final verification

All JVM commands used `-Xmx1024m`, except the project-local warm development
nREPL started by `make nrepl`, whose required Make entrance uses 512 MB.
The warm loop verified `user.dir` before reload and ran registration witnesses.
All process scratch and fixtures stayed beneath `/var/tmp/forge/regns-fx`.

```sh
npx --yes @chrisoakman/standard-clojure-style fix \
  src/clj_surgeon/test_registration.clj test/clj_surgeon/lane_manifest_test.clj
~/bin/clj-kondo --lint \
  src/clj_surgeon/test_registration.clj test/clj_surgeon/lane_manifest_test.clj
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.lane-manifest-test 'clj-surgeon.mcp-intent-contract-test) (let [r (clojure.test/run-tests 'clj-surgeon.lane-manifest-test 'clj-surgeon.mcp-intent-contract-test)] (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
make census-regenerate
```

```text
linting: errors: 0, warnings: 0
Ran 65 tests containing 2592 assertions.
0 failures, 0 errors.
census-regenerate: +0/-0
```

Each final command exited 0. Census regeneration added eight named tests during
development; the final repeat added zero and removed zero (2642 total names).
Neither `make test` nor `make test-battery` ran. CLI help was executed; malformed
namespace, absent source, missing metadata, conflicting lane/runtime, ambiguous
owner, escaping symlink, removed tests, invalid receipts and failed execution
have focused witnesses. Formatting and `git diff --check` passed.

Protected files remain byte-identical to the initial snapshot:

```text
c7fb402d7580ace47675d02f7b813bcd5837bdfab5788c0c78f477721f72b0d9  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

## Limits

This is a branch implementation, not a performance or merge-readiness claim.
The entrance admits `.clj` tests and the repository's literal map/set shapes.
Existing valid historical receipts are reused under the existing gate contract;
no new freshness claim is made for them. The existing bb-load incompatibility
exception accounts for JVM assignment without claiming that a JVM test passed.

Multi-file rollback handles ordinary process exceptions; it is not crash-atomic
publication or protection against unrelated concurrent writers. A killed entrance
can leave its named directory lock requiring recovery. Independent adversarial
review remains a pre-merge gate; no merge was requested or performed.

Fixture copies and the owned warm nREPL are removed/stopped at handoff. Text logs
and hash comparisons remain under `/var/tmp/forge/regns-fx`; this report retains
the decisive red/green outputs and counts.

## Round 2

Recorded 2026-09-14T10:09:52.601914+00:00; reviewed base `32eec035`.
Standing Round 1 approval covers this repair. No full suite or battery ran.

F1 root cause: at base, `test_registration.clj:364` started the shared
control child and immediately waited without recording its launch. All bb-load,
JVM-test and bb-test launches use that one site. The fix requires spawn-ledger
and calls `spawn/record!` immediately after start (current lines 347–366),
matching the other src builders. TEST-ISO-002's scanner enumerates
`ProcessBuilder.` in src and requires `spawn/record!` in the same file.
This source-scanning class oracle has no require edge from test-registration:
it is precisely the missing impact class addressed by sibling branch
`fable/impact-entrance`'s diff-impact source-scan edges. Focused require closure
alone could not select it.

F2 root cause: base lines 418–423 canonicalized the supplied root and selected
`/var/tmp/forge/regns-fx/register-<SHA256(canonical-root)>.lock`.
Thus the parent was fixed/shared, but the key was already per-root—not the real
repository root or a JVM-global singleton. Lines 421–423 ignored parent mkdir
failure and converted **every** false mkdir result into register-busy, without
holder evidence. Snapshot/path validation followed acquisition at lines 425–426.
The synchronized regression confirms different roots could overlap even on the
old implementation; it disproves an unqualified hash/key collision diagnosis.
The prewarm log retained neither the filesystem errno nor holder identity, so
the original gate's precise mkdir failure cannot be reconstructed from it.

Current lines 421–436 publish a complete pid/root holder record by atomic hard
link to `<canonical-registration-root>/.clj-surgeon-register.lock`.
There is no shared external lock parent. Only FileAlreadyExists is contention;
other filesystem exceptions return register-io-failed. Lines 438–448 perform
snapshot/path and request validation before acquiring the lock; snapshot
comparison still guards publication after acquisition. Only an acquired lock
is removed by its invocation. Hard-link support is required; unsupported
filesystems refuse. A killed holder may leave the named lock for recovery.

The permanent REGNS-009 witness at lane_manifest_test.clj:1696 uses promises to
hold root A inside control execution. Root B independently reaches its expected
control-failed result; A's contender alone returns busy with holder pid/root.
An invalid lane and an escaping symlink on the held root retain their own
refusal types. Releasing A gives exactly one busy across the two A calls and
removes its lock. Controls are stubbed at the execution boundary in this fast
namespace; the witness proves real filesystem locking, not control execution.

Prewarm RED, from `prewarm1-check.log` (Make exit 2):

```text
FAIL every-src-spawn-site-records-into-the-ledger: test_registration.clj
registration-failed-control-rolls-back-enrollment: register-control-failed != register-busy
registration-boundary-refusals-preserve-source-bytes: register-path-escape != register-busy
```

New witness RED on the old source, in `round2-red.log`:

```text
registration-lock-is-per-root-and-identifies-holder:
  holder root: expected fixture root, actual nil
  holder pid: expected positive integer, actual nil
  register-conflict != register-busy
  register-path-escape != register-busy
every-src-spawn-site-records-into-the-ledger:
  1 src spawn site(s) ... src/clj_surgeon/test_registration.clj
```

This targeted test-vars command reports assertion failures but does not translate
them to process exit. An initial malformed test edit failed to read and was
corrected before obtaining this behavioral RED. The first full focused run
then exposed the new test's missing census enrollment (three class-oracle
failures); census-regenerate added the one name before the final green run.

Final commands used `TMPDIR=/var/tmp/forge/regns-fx` and
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/regns-fx`:

```sh
npx --yes @chrisoakman/standard-clojure-style fix src/clj_surgeon/test_registration.clj test/clj_surgeon/lane_manifest_test.clj
~/bin/clj-kondo --lint src/clj_surgeon/test_registration.clj test/clj_surgeon/lane_manifest_test.clj
make census-regenerate
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.ns-isolation-test 'clj-surgeon.lane-manifest-test) (let [r (clojure.test/run-tests 'clj-surgeon.ns-isolation-test 'clj-surgeon.lane-manifest-test)] (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
sha256sum -c /var/tmp/forge/regns-fx/round2-protected.sha256
git diff --check
```

GREEN, each command exit 0:

```text
linting: errors: 0, warnings: 0
census-regenerate: +1/-0
Ran 71 tests containing 2197 assertions.
0 failures, 0 errors.
docs/observations/battery-ledger.edn: OK
docs/observations/battery-namespace-walls.edn: OK
```

Census ran with an additional JAVA_TOOL_OPTIONS `-Xmx1024m`.
The protected SHA-256 values remain those recorded in Round 1. Formatter, lint,
census and focused output are retained as `round2-*.log` under the approved
scratch root. A bb load and the three lock/refusal witnesses also exited 0.

The stale-check probe source is **byte-identical to trunk 8aedb65e**:
`test/clj_surgeon/mcp_hot_verify_test.clj` has Git blob
`51adf4f27114d4012020ef399fa437479a999a41` at both trunk and this branch.
It generates the sol-round5 dev/experiments fixture, changes dependency value
from 1 to 2, and expects probe-failed with one failure. The prefix-dependency
probe is in that same unchanged file. Those fixture failures are expected
nested output; the source and generated-fixture definitions were left untouched.
The additional affected intent-contract check ran with the same 1024 MB JVM
command pattern: 22 tests, 571 assertions, zero failures/errors, exit 0
(`round2-intent.log`). No landing-gate rerun or merge-readiness claim is made.

## Round 3

Recorded 2026-09-14T11:20:20.204125+00:00; base `e81a9c75d7fdc106656e97a2bf1ac4c420b563e3`,
branch `fable/regns-entrance`. Read Sol's complete `/var/tmp/forge/regns-fx/sol-verdict-1.md`
first. The seat's standing approval covered design, requirements, tests, code,
documentation and verification without review pauses. REGNS-004/005/006 now state
the corrected repeat/execution/first-contact contracts; REGNS-010/011 retain the
stale-control and shared-census promises.

**F1 — STALE-CONTROL-MATRIX, red then green.** At the unmodified base, the
copied repository under `round3-red` received this exact source:

```clojure
(ns clj-surgeon.sol-stale-test {:lane :battery}
  (:require [clojure.test :refer [deftest is]]))
(deftest sol-plant (is false))
```

At both predictable `controls/clj-surgeon.sol-stale-test-{jvm,bb}-test.control.edn`
paths under attempt22, the planted map was:

```clojure
{:namespace clj-surgeon.sol-stale-test :runtime :jvm ; :bb in the other file
 :status :passed :exit 0 :source-sha256 "stale"
 :result {:test 1 :pass 1 :fail 0 :error 0}}
```

Command in both copied roots:

```sh
make register-test-ns NS=clj-surgeon.sol-stale-test LANE=battery RUNTIME=jvm
```

RED: Make exit 0, `{:ok true :state :registered}`, both controls claimed pass,
neither contained `:command`; the failing test did not run. Full receipt:
`/var/tmp/forge/regns-fx/round3-sol-red.log`.

GREEN: the same plant against the changed sources ran the control argv and
returned `{:ok false :state :rolled-back :error-type :register-control-stale}`.
The JVM execution had `:exit 1`, `:status :test-failed`, and
`:result {:test 1 :pass 0 :fail 1 :error 0}`; Make propagated failure as exit 2
(the Clojure entrance exits 1). Its actual argv included `clojure`,
`-J-Xmx1024m`, `-M:clj-surgeon/test-deps`, `-m portability-runner`, `jvm`,
`clj-surgeon.sol-stale-test`, the raw receipt path and `test`. The recorded
execution had pid 3137341, start ticks 282263436, wall 4198.30848 ms and source
SHA-256 `6338f1a6f7b03dc0bab19f24d95816dd1a55d22e51a61276a4ab445cff201f83`.
All eight checked source/registration/planted-control files were byte-identical
after rollback; the only Sol control files remaining were the original two
plants. Receipts: `round3-sol-green.log` and `round3-sol-green-check.json` under
the same scratch root.

The permanent matrix independently corrupts each of the three control paths
across portable/JVM, portable/bb, bb-ineligible/JVM and bb-load-incompatible/JVM
acceptance branches, with stale, valid-format wrong-hash and missing-provenance
receipts: **36 cells**. Every cell plants the real failing test, executes JVM
argv, reports fail=1, refuses stale controls and restores fixture bytes.
Additional checks remove every required test-control provenance field and each
subject field. Saved controls never select a shortcut in `register!`.
`control!` writes argv, execution-time source hash/root, actual exit and runner
result/status, parent-measured wall, pid and Linux start ticks from the process
it just ran. A bb load failure still requires an executed passing JVM test.
Unknown provenance refuses. Historical inventory classification remains read-only.

The real runner boundary also exposed inherited cleanup ownership: nested
controls now clear `CLJ_SURGEON_TMPDIR_REEXEC`, allowing each runner to own its
own nested scratch root. Product code inherits the admitted temporary base;
it contains no `/var/tmp/forge/regns-fx` constant. The JVM's ordinary procfs
reader could not obtain start ticks on this host; the direct file reader did.
Both fixes are exercised by the real nested controls, not fabricated pass maps.

**F2 — FIRST-CONTACT-GATE-MATRIX.** The old runtime-count/closure assertions
produced a first failure with no checklist or remedy; the red matrix captured
that actual assertion/message pair. The runtime assertions now carry the
registry message directly, as do the metadata and inventory assertions that
can make first contact. Successful per-namespace control assertions avoid
rebuilding the diagnostic. The matrix runs the seven ordinary registration
inventory gate Vars in their actual `ns-interns` order using `test-vars`, without
reordering tests or restating assertion bodies. Only the fixture pin literal
is instantiated from the actual gate source. It preserves outer temporary-root
tracking across the nested ordinary fixtures. Complete fixture state passes;
each nonzero mask's first failure contains all five checklist rows and the exact
shell-quoted Make remedy. The read-only matrix lives in lane-manifest-test so
it does not demand its own unpublished enrollment controls.

Final output below: `:fail` is the expected first gate failure caused by a
planted missing-surface mask, not a failed matrix assertion.

```text
FIRST-CONTACT-MATRIX 0 nil nil
FIRST-CONTACT-MATRIX 1 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 2 every-manifest-namespace-declares-its-lane-in-its-own-ns-form :fail
FIRST-CONTACT-MATRIX 3 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 4 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 5 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 6 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 7 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 8 every-test-namespace-on-disk-is-accounted-for :fail
FIRST-CONTACT-MATRIX 9 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 10 every-manifest-namespace-declares-its-lane-in-its-own-ns-form :fail
FIRST-CONTACT-MATRIX 11 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 12 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 13 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 14 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 15 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 16 runtime-portability-controls-cover-every-assignment :fail
FIRST-CONTACT-MATRIX 17 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 18 every-manifest-namespace-declares-its-lane-in-its-own-ns-form :fail
FIRST-CONTACT-MATRIX 19 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 20 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 21 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 22 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 23 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 24 runtime-portability-controls-cover-every-assignment :fail
FIRST-CONTACT-MATRIX 25 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 26 every-manifest-namespace-declares-its-lane-in-its-own-ns-form :fail
FIRST-CONTACT-MATRIX 27 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 28 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 29 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 30 every-manifest-entry-exists-on-disk :fail
FIRST-CONTACT-MATRIX 31 every-manifest-entry-exists-on-disk :fail
```

**F3 — CENSUS-WRITER-CENSUS.** `clj-surgeon.test-census` owns the original
source derivation, named-removal guard and canonical serialization. Both the
registration planner and explicit regenerator call it; registration supplies
its snapshot/write callback and retains transaction rollback. The former
independent scan/append implementation is gone. The canonical header and sorted
one-qualified-name-per-line format remain byte-for-byte the previous writer's
format. The real ledger diff contains exactly three added test names, with no
removals or unrelated line changes. A removed source now yields named removals
instead of trying to slurp nil.

The executable census runs `rg` over `src/`, `test/` and `Makefile`, enumerates
all literal-path files, checks both entrances' delegation, and asserts the
single writer implementation:

```text
CENSUS-WRITER-CENSUS references
  ("src/clj_surgeon/test_census.clj"
   "test/clj_surgeon/lane_manifest_test.clj"
   "test/clj_surgeon/registration_controls_test.clj")
writers #{"src/clj_surgeon/test_census.clj"}
```

**Executed enrollment and final verification.** The new battery namespace was
registered through the production entrance itself, with no BB_INELIGIBLE override:

```sh
make register-test-ns NS=clj-surgeon.registration-controls-test LANE=battery RUNTIME=jvm
```

Exit 0, `:state :registered`, classification portable. JVM and bb each ran
**2 tests / 238 assertions / 0 failures / 0 errors**, plus a real bb load.
The retained [JVM control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.registration-controls-test-jvm-test.control.edn),
[bb control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.registration-controls-test-bb-test.control.edn)
and adjacent complete logs/argv files bind source SHA-256 `713e34875347d3e02b29ddafd037939765b0f35615df4ec474608dc70bb6cb3c` to
`/home/forge/src/clj-surgeon-regns`. All three hashes still match the source.
The initial circular-fixture and nested-root failures were repaired before this
successful enrollment; no failed receipt was promoted to passing evidence.

With `TMPDIR` and the JVM startup temporary directory under the round's scratch
root, final checks were:

```text
clojure -J-Xmx1024m -M:clj-surgeon/test-deps
  clojure.test/run-tests: lane-manifest-test, ns-isolation-test
  Ran 72 tests containing 2460 assertions. 0 failures, 0 errors. Exit 0.
~/bin/clj-kondo --lint <all five changed Clojure files>
  0 errors, 0 warnings. Exit 0.
make census-regenerate
  census-regenerate: +0/-0. Exit 0.
make intent-audit
  :ok true. Exit 0.
Standard Clojure Style formatting and git diff --check passed.
```

Protected ledgers match the pre-round hashes:

```text
c7fb402d7580ace47675d02f7b813bcd5837bdfab5788c0c78f477721f72b0d9  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

No `make test` or `test-battery` was run. This is branch-only correctness work;
there is no main merge, performance admission or claim of a new Sol GO verdict.

Round 3 fixture copies and test temporary roots were removed after verification;
receipt logs remain under `/var/tmp/forge/regns-fx`. The owned nREPL was stopped.


## Round 4

Recorded 2026-09-14T11:59:31.212474+00:00; base `99b57bd1d05babf954ce202109e2b1f7e9999c94`,
branch `fable/regns-entrance`. Standing approval covered every phase.

The FAST cadence is green through the same shared coordinator that serves the
landing gate: **52,514 ms serial-equivalent against 60,000 ms**, leaving
**7,486 ms (12.5%) headroom**. All 95 selected fast namespaces ran: **1,265 tests,
13,532 assertions, 0 failures, 0 errors**, exit 0, no isolation violations.
`--suite fast` selects the fast subset of the mcp suite through the same manifest,
runtime assignment, automatic pool allocation, and cadence accounting. This is
focused fast-cadence proof, not a claim that the complete landing gate ran.

`lane-manifest-test` alone passed **46 tests / 2,063 assertions**, with
require-plus-tests wall **9,532.579 ms** and external process wall
**10,250 ms**, both below 15,000 ms. Its one-mask first-contact witness costs
**25.159 ms**. The new battery alone passed
**1 test / 251 assertions**, require-plus-tests wall **80,527.932 ms**
and external process wall **81,210 ms**, at `-Xmx1024m`.
That full matrix wall belongs to the battery budget.

Measurement command (the timing script wraps `clojure.test/test-var`, recording
only outer deftests so nested ordinary gate calls are not counted a second time):

```sh
export TMPDIR=/var/tmp/forge/regns-fx
export JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/regns-fx
clojure -J-Xmx1024m -M:clj-surgeon/test-deps \
  /var/tmp/forge/regns-fx/round4/time.clj clj-surgeon.lane-manifest-test
clojure -J-Xmx1024m -M:clj-surgeon/test-deps \
  /var/tmp/forge/regns-fx/round4/time.clj clj-surgeon.test-registration-battery-test
CLJ_SURGEON_STATE_HOME=/var/tmp/forge/regns-fx/round4/state \
CLJ_SURGEON_ARTIFACT_ROOT=/var/tmp/forge/regns-fx/round4/state \
bb -Xmx1g -Djava.io.tmpdir="$TMPDIR" -m clj-surgeon.battery-parallel-runner \
  --suite fast --work-dir /var/tmp/forge/regns-fx/round4/fast-green
```

The authoritative outer-deftest baseline passed 45 tests / 2,293 assertions,
require-plus-tests wall **98,951.540 ms**. An initial instrumentation run
also recorded nested gate Vars; it is retained as `before.log` but is not the
per-deftest table below. `before-outer.log` is the clean baseline. The following
table is sorted by before wall, so its first ten rows are the requested top ten.
All values are milliseconds. The matrix row's fast-after column is the retained
single-mask witness; the full original body executes separately in battery.

| Deftest | Before fast | After fast |
|---|---:|---:|
| registration-first-contact-gate-matrix | 79505.494 | 25.159 |
| generated-portability-census-agrees-with-all-inventories | 5037.595 | 1723.735 |
| every-manifest-entry-exists-on-disk | 2406.526 | 2434.443 |
| every-manifest-namespace-declares-its-lane-in-its-own-ns-form | 1728.526 | 0.324 |
| every-test-namespace-on-disk-is-accounted-for | 1704.815 | 1716.174 |
| runtime-portability-controls-cover-every-assignment | 1652.586 | 27.592 |
| a-namespace-in-the-tree-but-absent-from-the-census-is-named | 1650.190 | 2.031 |
| the-regenerate-entrance-refuses-inside-make | 1635.389 | 1.638 |
| no-living-prose-still-calls-the-bb-lane-by-its-old-name | 370.539 | 338.977 |
| every-sleep-on-the-merge-gate-is-declared-with-its-reason | 248.577 | 241.443 |
| the-landing-gate-runs-both-the-merge-gate-and-the-battery-tripwire | 189.540 | 172.027 |
| no-fast-lane-namespace-spells-a-child-process | 178.272 | 252.581 |
| the-corpus-only-ever-grows-and-the-arithmetic-is-shown | 109.918 | 120.753 |
| the-lane-runner-resolves-to-exactly-the-lane-it-names | 35.296 | 32.395 |
| registration-lock-is-per-root-and-identifies-holder | 33.620 | 34.495 |
| runtime-evidence-binds-statistics-to-receipt-files | 29.284 | 40.291 |
| every-implemented-requirement-is-claimed-by-a-marker | 18.497 | 18.129 |
| every-test-iso-marker-in-the-tree-is-a-registered-requirement | 18.045 | 19.539 |
| sleep-pins-survive-line-movement-and-refuse-purpose-drift | 16.776 | 21.961 |
| runtime-steering-fields-cannot-outvote-control-receipts | 13.558 | 13.923 |
| every-exclusion-is-actually-run-by-the-runner-it-names | 13.062 | 14.119 |
| registration-failed-control-rolls-back-enrollment | 9.686 | 8.601 |
| registration-oracle-enumerates-all-surfaces | 8.434 | 7.257 |
| a-false-redirection-to-an-existing-target-is-refused-by-name | 7.093 | 7.318 |
| runtime-evidence-is-consumed-and-receipts-are-required | 6.229 | 6.927 |
| registration-existing-values-and-ambiguous-owners-refuse | 5.967 | 10.220 |
| registration-boundary-refusals-preserve-source-bytes | 5.528 | 7.142 |
| registration-planner-refuses-conflicts | 4.885 | 5.048 |
| registration-structural-plan-preserves-and-repeats | 4.783 | 5.752 |
| the-landing-gate-refuses-a-stale-battery-receipt | 3.896 | 4.064 |
| runtime-receipts-must-stay-in-retained-evidence-roots | 2.612 | 2.024 |
| census-regeneration-refuses-named-removals | 2.551 | 2.321 |
| an-exclusion-naming-an-unreadable-runner-fails-closed | 2.485 | 2.362 |
| registration-checklist-covers-every-missing-surface-subset | 1.667 | 2.076 |
| loaded-namespaces-carry-their-lane-at-runtime | 1.605 | 1.313 |
| registration-control-results-are-executions | 0.752 | 0.936 |
| every-lane-declares-a-cadence-the-runner-knows | 0.644 | 3.245 |
| the-partition-matches-round-ones-measurement | 0.635 | 1.742 |
| the-runner-refuses-an-undeclared-namespace | 0.553 | 0.671 |
| the-runner-resolves-a-declared-lane | 0.431 | 0.565 |
| the-refusal-message-names-the-cadence-a-lane-costs | 0.391 | 0.482 |
| excluded-entries-are-real-and-carry-a-reason | 0.344 | 0.428 |
| every-manifest-namespace-resolves-to-a-known-cadence | 0.313 | 0.422 |
| the-partition-drops-nothing-round-one-measured | 0.237 | 0.207 |
| the-rename-scanner-cannot-see-a-bb-less-mention-and-says-so | 0.159 | 0.172 |
| registration-witnesses-retain-names-and-battery-matrix (new source ratchet) | — | 62.431 |

| Battery deftest | After |
|---|---:|
| registration-first-contact-gate-matrix (all 32 masks) | 78427.950 |

**TDD and preservation.** Commit `3cf4a99e` recorded the source regression before
the move. It failed because the battery namespace was absent and the fast witness
still traversed all 32 masks (exit 1). The green source witness freezes all 45
original deftest names and asserts the complete moved matrix body SHA-256:
`f147ae9a17cd5eaacda8202be351cda9bef62250da69d06f62dee0a50b11afec`.
The moved body is byte-identical to 99b57bd1. The old qualified fast name remains
for its one ordinary-order gate mask, and the full body has the same unqualified
name in `test-registration-battery-test`, with author metadata `{:lane :battery}`.
This preserves the original census entry while adding the full battery witness.
No renamed/deleted census entry or removal override was needed.

The 36-cell stale-control matrix and executed failing JVM/bb controls were
already in `registration-controls-test` with `:lane :battery` at 99b57bd1;
that entire source file remains byte-identical. It was not in the measured fast
namespace and did not need another move. Both full matrices remain in battery.

The fast lane retains the pure registration model/oracle matrix, fixture byte
preservation/conflict refusals, and rollback/lock witnesses. A fast fixture now
refuses any real `control!` call; boundary witnesses provide their own stubs.
No control process can silently return to fast through registration's indirect
execution boundary. Two existing fixture-message tests stub the unrelated live
repository checklist lookup. Ordinary live inventory assertions remain intact.
The generated-inventory gate computes its identical diagnostic once per test,
and successful metadata/control agreement avoids building failure diagnostics.
No assertion was removed from those ordinary gate bodies, and the full matrix
still exercises their real first failures in ordinary namespace order.

**Dogfood.** Executed on the actual worktree, exit 0:

```sh
make register-test-ns NS=clj-surgeon.test-registration-battery-test LANE=battery RUNTIME=jvm
```

Selected fields from the actual printed `enroll-final.log` receipt:

```edn
{:ok true :state :registered
 :changes [{:file "test/clj_surgeon/deftest_census.edn"
             :forms [{:form deftest-census :before-count 2646 :after-count 2648}]}
           {:file "test/clj_surgeon/lane_manifest.clj"
             :forms [{:form manifest :before-count 158 :after-count 159}
                     {:form portability-runtimes :before-count 163 :after-count 164}]}
           {:file "test/clj_surgeon/lane_manifest_test.clj"
             :forms [{:form adopted-since-round-one :before-count 109 :after-count 110}
                     {:form every-manifest-entry-exists-on-disk :before-count 163 :after-count 164}]}]
 :controls {:bb-load {:status :loaded :exit 0 :wall-ms 364.145163}
            :jvm {:status :passed :exit 0 :wall-ms 81176.911308
                   :result {:test 1 :pass 251 :fail 0 :error 0}}
            :bb {:status :passed :exit 0 :wall-ms 42041.227597
                  :result {:test 1 :pass 251 :fail 0 :error 0}}}}
```

The retained [JVM control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.test-registration-battery-test-jvm-test.control.edn),
[bb control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.test-registration-battery-test-bb-test.control.edn)
and adjacent load/argv/log artifacts bind the new source SHA-256
`7e7d360ab9b385caa8bc78cbbe56ebda6a80b0ff591f6f8e053897a03d023bc4`
to this worktree. Each test log prints all 32 masks. During first enrollment,
a read-only fixture supplies its own projected inventory in memory and temporary
control rows to the gate specimen; those rows never enter `register!` or retained
control paths. The entrance publishes only its actual executed controls.

The first enrollment failed: independent fast/battery tracking atoms made the
verbatim matrix's outer-root restoration duplicate retained roots until heap
exhaustion. The failed JVM child and subsequent bb control were terminated, and
Make returned `:state :rolled-back`, `:error-type :register-control-failed`, exit 2.
Sharing the original fast tracker fixed ownership without changing matrix bytes.
Only the later passing execution artifacts are retained in the repository.

The first fast run reached 53,110 ms but had four state-envelope errors: the
scratch state root was outside the children’s narrowed temporary-root envelope.
Setting its matching `CLJ_SURGEON_ARTIFACT_ROOT` at launch corrected the setup.
The second run passed assertions at 51,623 ms but exposed the namespace's separate
8,000 ms limit (8,921 ms). Stubbing the unrelated live scans in the two pure
message specimens produced the final exit-0 result above. No budget or override
was raised, and no failed run is presented as a passing gate.

**Final checks.** Paved `~/bin/clj-kondo` over all three changed Clojure files:
0 errors / 0 warnings, exit 0. Standard Clojure Style and `git diff --check`
passed. `make census-regenerate`: `+0/-0`, exit 0 (Make registration had already
installed the two additions). The complete ledger diff from 99b57bd1 is **+2/-0**.
`make intent-audit`: `:ok true`, exit 0. Protected ledger SHA-256 values remain:

```text
c7fb402d7580ace47675d02f7b813bcd5837bdfab5788c0c78f477721f72b0d9  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

Round-4 scratch and logs are under `/var/tmp/forge/regns-fx/round4`; runner
fixtures are created under the inherited admitted temporary root and swept.
Product paths derive from the repository or admitted state root; no round-specific
scratch constant was added to product/test source. No `make test` or `test-battery`
ran. Both commits carry the three requested trailers and land only on
`fable/regns-entrance`; `main` remains untouched.


## Round 5

Recorded 2026-09-14T12:18:37.988406+00:00. Merged `origin/MCP/main` at
`866df39bbfcae712d35f37497aa0c7d5927212e2` into `fable/regns-entrance`
from `022499340b9d67fad28da31083b35bc4a6790d27`, after fetching origin.
All five conflicts were resolved as the union. Standing approval covered this
merge and its verification.

| File | Both sides' change | Resolution | Computed count |
| --- | --- | --- | --- |
| `test/clj_surgeon/lane_manifest.clj` | Branch registered the registration battery; trunk registered diff-impact and its JVM runtime. | Retained both lane entries and both runtime entries, including all earlier registration-control entries. | 165 namespaces |
| `test/clj_surgeon/lane_manifest_test.clj` | Branch retained registration diagnostics and battery adoption; trunk added diff-impact adoption. The actual parent pins were 164 and 163. | Kept both adoption entries and branch diagnostics; replaced the pin using the namespace set read from disk. | 165 namespaces |
| `docs/observations/2026-09-12-bbtower-block-b/attempt22/portability-census.md` | Each side added its namespace's census row and adjusted the projection totals. | Retained both rows; refreshed generation time and totals from the unioned control map. | 165 rows: 102 portable, 50 bb-load-incompatible, 13 non-portable |
| `docs/observations/2026-09-12-bbtower-block-b/attempt22/portability-controls.edn` | Each side added its namespace's control references. | Parsed both parent EDN maps, asserted that overlapping keys have equal values, and serialized their union. Both sets of control artifacts remain unchanged. | 165 entries |
| `docs/tech-tree.md` | Branch documented registration entrance and battery placement; trunk documented diff-impact selection and HOLD repair. | Retained both summaries, rows and detailed entries. | Repository count 165; no tech-tree pin |

**Count derivation.** Enumerated every `*_test.clj` / `*_test.cljc` under
`test/`, read each first form with reader evaluation disabled, and counted the
set of declared namespace symbols: **165**. The registration entrance's
rewrite-clj `replace-pin` helper installed that result. No arithmetic bump was
used. Clojure conflict resolution preserved the branch's complete forms and
inserted the missing runtime map entry; EDN was merged as parsed maps. No
sed/awk conflict-marker editing was used.

**Verification.** All commands exited 0, with temporary files and JVM temporary
roots under `/var/tmp/forge/regns-fx`:

- Whole-repository oracle: `bb -e '(require (quote [clj-surgeon.test-registration :as r])) (let [xs (r/repository-checklist ".")] (prn {:ok (empty? xs) :missing (mapv :missing xs)}) (System/exit (if (empty? xs) 0 1)))'` returned `{:ok true, :missing []}`.
- `make census-regenerate`: **+0/-0** on its first execution. No lost census member needed repair. Independently checked that the merged deftest census contains every line from both parents.
- Standard Clojure Style formatted the two resolved Clojure files. Paved `~/bin/clj-kondo` linted those files, the resolved controls EDN, and trunk's three diff-impact Clojure files: **0 errors, 0 warnings**.
- `clojure -J-Xmx1024m -M:clj-surgeon/test-deps`, requiring and running `clj-surgeon.lane-manifest-test`, `clj-surgeon.diff-impact-test`, and `clj-surgeon.test-registration-battery-test`: **61 tests, 2562 assertions, 0 failures, 0 errors**. The first-contact matrix's printed `:fail` rows are deliberate missing-surface probes; the enclosing suite passed.
- `git diff --cached --check` passed; all five conflict entries were resolved.
- All 12 registration-battery control/artifact files are byte-identical to `02249934`; all six diff-impact control/artifact files are byte-identical to `866df39b`.

**Protected ledger baseline.** Both protected files are byte-identical to the
incoming trunk `866df39b`. Trunk already contains receipt commit `f4891346`,
which added the battery result for `d0334982`; preserving that existing receipt
changes the battery-ledger hash relative to Round 4. This merge generated no
new battery-ledger receipt and did not rewrite either protected file.

```text
08a1d6ee9c7f319c4a146ec23798e535273a47b7633697354014b318bff8ad39  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

Logs and the structural count/union script are retained as `round5-*` under
`/var/tmp/forge/regns-fx`. This is a branch merge with focused correctness
verification; no new performance admission is claimed.

## Round 6

Recorded 2026-09-14T13:09:34.977995+00:00. Starting tip:
`bfbec7f910b4d1488bb2bc2c7bf80f2c4eab771d`, branch `fable/regns-entrance`.
Sol's full `/var/tmp/forge/regns-fx/sol-verdict-2.md` was read before changes.
Standing approval covered every phase. The RED witness commit is `eac8dab1`.
The current contract and matrix are recorded in
[the Round 6 plan](../plans/register-test-ns-round6.md) and the registration leaf.

**REAL-GATE-001 RED.** An archive of the starting tip with the metadata-bearing,
otherwise unregistered `clj-surgeon.sol-first-contact-test` reproduced Sol's
first failure through `make test-fast`: exit **2**, first checklist and remedy
for already-registered **clj-surgeon.admit-patch-test**, pin **165 → 166**.
The focused production JVM lane runner took **10,883 ms**; the real Make lane
receipt measured **9,108 ms**, both above 8,000 ms. Full logs are
`round6/red-make.log` and `round6/red-lane.log` under the approved scratch root.
The replacement copied-file matrix, run against the original gate bodies,
passed the valid seed and masks 1–3, then rejected mask 4 because its first
checklist was again `admit-patch-test`. This is the global-count interaction
that the old evaluated/redefined specimen did not exercise.

**Semantics and cost.** The repository sweep removes count facts from local
namespace checklists. It emits one separate `:scope :repository`, `:surface 3`
row carrying the actual pin, discovered/runtime union count and the sorted
`disk − manifest − excluded` subjects. Complete registered namespaces emit no
checklist. Local defects retain the author's lane (or an existing manifest lane)
and the requested runtime. The lane gate shares one delayed checklist per
namespace run; the delay is discarded between runs. A repository snapshot also
parses the portability inventory and markdown once, rather than once per subject.
Copied-root probes calculate their own snapshot and do not use the live-root cache.

**Count-only edge case.** Mask **4** has only the count pin missing: its planted
namespace is already in the manifest, so `disk − manifest` is empty. Requiring a
namespace checklist in that case conflicts with the requirement that registered
namespaces receive none. This round uses the repository-only diagnostic, with
no namespace checklist or namespace registration remedy. The clarification and
this working assumption were stated during execution. The other **30 nonzero
masks** require the planted namespace and its exact requested
`LANE='battery' RUNTIME='jvm'`; the real Make plant separately verifies `fast/jvm`.
This report does not claim that all 31 nonzero masks emit a namespace checklist.

**Class oracle.** The battery uses a complete repository copy below the inherited
admitted temporary root (this run: `/var/tmp/forge/regns-fx/round6/`). A seed is
registered through actual freshly executed controls. Each mask restores the seed's
file bytes before removing its surfaces. The helper obtains its cold JVM argv
from the production coordinator's `lane-command`:

```text
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --emit-edn lane.edn --ns clj-surgeon.lane-manifest-test
```

No diagnostic, repository Var, or test body is replaced. Only the actual
registration files and the surface-3 pin literal change in the copy. The oracle
checks the **first failure block**, all emitted checklist subjects, exact remedy,
count-row cardinality, no test errors and a lane wall **below 8,000 ms** in every
cell. Mask 0 must pass the real runner. The fast representative removes one real
namespace's declarations from copied registration files, including their real
count and controls; it emits no child process. The obsolete 99b57bd1 matrix hash
is retired, while its original qualified names remain protected.

**Final matrix:** **32/32 cells passed** (valid seed plus all 31 nonzero masks).
The battery namespace took **341,403 ms (341.403 s)**, including seed
controls, repository copying, mask setup, all cold lane executions and cleanup;
runner result **1 test, 1 assertion, 0 failures, 0 errors**, no isolation violations.
The slowest nested lane took **5,455 ms**.

| Mask | Lane wall (ms) | First failing gate | Checklist subject |
| --- | ---: | --- | --- |
| 0 | 5221 | none (valid seed) | none |
| 1 | 5145 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 2 | 5310 | every-manifest-namespace-declares-its-lane-in-its-own-ns-form | sol-first-contact-test |
| 3 | 5126 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 4 | 5142 | every-manifest-entry-exists-on-disk | repository only |
| 5 | 5415 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 6 | 5320 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 7 | 5447 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 8 | 5147 | every-test-namespace-on-disk-is-accounted-for | sol-first-contact-test |
| 9 | 5114 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 10 | 5408 | every-manifest-namespace-declares-its-lane-in-its-own-ns-form | sol-first-contact-test |
| 11 | 5268 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 12 | 5167 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 13 | 5209 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 14 | 5178 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 15 | 5127 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 16 | 5157 | runtime-portability-controls-cover-every-assignment | sol-first-contact-test |
| 17 | 5455 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 18 | 5247 | every-manifest-namespace-declares-its-lane-in-its-own-ns-form | sol-first-contact-test |
| 19 | 5173 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 20 | 5396 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 21 | 5267 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 22 | 5337 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 23 | 5213 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 24 | 5069 | runtime-portability-controls-cover-every-assignment | sol-first-contact-test |
| 25 | 5144 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 26 | 5199 | every-manifest-namespace-declares-its-lane-in-its-own-ns-form | sol-first-contact-test |
| 27 | 5223 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 28 | 5373 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 29 | 5151 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 30 | 5141 | every-manifest-entry-exists-on-disk | sol-first-contact-test |
| 31 | 5338 | every-manifest-entry-exists-on-disk | sol-first-contact-test |

The preliminary matrix took **393,668 ms** with the first cache change. Two
nested cells measured **8,016 / 8,945 ms** while a full fast gate was also running;
that preliminary oracle did not yet enforce the nested budget. Those measurements
are retained as non-qualifying. After sharing parsed projection data, the final
matrix above enforces the budget and passes every cell. No general speed or
routed-operation performance admission is claimed.

The first strict matrix execution took **337,496 ms** and passed all cell
assertions, but its receipt exposed the separate **300,000 ms battery namespace**
default (the **1,800,000 ms** limit is the whole cadence). The existing measured
exception mechanism now gives this namespace **800,000 ms**, approximately twice
the largest measured 393,668 ms cost of the required cold class oracle. No fast
limit or whole-battery limit was raised. The final matrix result above is a fresh
execution with that declared override and no isolation violations.

**Final focused and landing verification.**

| Execution | Result | Namespace / coordinator wall |
| --- | --- | --- |
| lane-manifest-test alone, no plant | exit 0; 46 tests, 2064 passes, 0 failures/errors; no isolation violations | **5,105 ms** |
| lane-manifest-test alone, Sol plant in copy | exit 5; 46 tests, 2059 passes, 5 intended registration failures, 0 errors; no isolation violations | **5,285 ms** |
| `make test-fast`, Sol plant in archive copy | exit **2**; first checklist is Sol's namespace; 1265 tests, 13529 passes, 5 intended registration failures, 0 errors | lane **4,193 ms**; coordinator **32,388 ms** |

The first failure from the final real `make test-fast` is pasted verbatim below,
through its complete `actual` line. No already-registered namespace appears as a
**checklist subject**; the assertion's set dump still necessarily lists the full
inventory being compared.

```text
FAIL in (every-manifest-entry-exists-on-disk) (/var/tmp/forge/regns-fx/round6/green/test/clj_surgeon/lane_manifest_test.clj:185)
every discovered test namespace has a closed runtime declaration
Registration checklist for clj-surgeon.sol-first-contact-test: 1 lane/runtime test/clj_surgeon/lane_manifest.clj -> [manifest portability-runtimes] actual=[nil nil] expected=[:fast :jvm]; 2 ns metadata test/clj_surgeon/sol_first_contact_test.clj -> ns actual=:fast expected=:fast; 4 adoption test/clj_surgeon/lane_manifest_test.clj -> adopted-since-round-one actual=false expected=true; 5 census/control ["test/clj_surgeon/deftest_census.edn" "docs/observations/2026-09-12-bbtower-block-b/attempt22/portability-controls.edn"] -> [deftest-census portability-controls] actual={:missing-tests #{clj-surgeon.sol-first-contact-test/works}, :controls-valid? false} expected={:missing-tests #{}, :controls-valid? true}. Remedy: make register-test-ns NS='clj-surgeon.sol-first-contact-test' LANE='fast' RUNTIME='jvm'
Repository registration count: runtime count test/clj_surgeon/lane_manifest_test.clj -> every-manifest-entry-exists-on-disk actual=165 expected=166; unregistered namespaces=[clj-surgeon.sol-first-contact-test]. Reconcile the repository pin with the discovered/runtime union.
expected: (= (set (keys (clojure.core/deref on-disk))) (set (keys runtimes)))
  actual: (not (= #{clj-surgeon.mcp-http-server-test clj-surgeon.require-change-boundary-test clj-surgeon.show-form-test clj-surgeon.mcp-recovery-test clj-surgeon.parser-admission-test clj-surgeon.core-discovery-test clj-surgeon.mission-plain-forms-test clj-surgeon.mission-git-process-test clj-surgeon.move-test clj-surgeon.outermost-test clj-surgeon.mcp-expect-guard-test clj-surgeon.workspace-onboarding-test clj-surgeon.mcp-alias-migration-test clj-surgeon.sol-first-contact-test clj-surgeon.relation-census-test clj-surgeon.mission-test clj-surgeon.move-dependency-test clj-surgeon.extract-test clj-surgeon.lens-query-test clj-surgeon.mcp-paths-test clj-surgeon.ns-isolation-test clj-surgeon.tmp-leak-support-test clj-surgeon.mission-candidate-race-test clj-surgeon.mission-git-ledger-test clj-surgeon.ls-tree-test clj-surgeon.mission-candidate-test clj-surgeon.mission-git-fence-test clj-surgeon.battery-state-admission-test clj-surgeon.receipt-artifacts-boundary-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-typist-executor-test clj-surgeon.cell-b-oracle-test clj-surgeon.mcp-relation-census-launcher-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test clj-surgeon.diagnostic-delta-test clj-surgeon.mission-run-test clj-surgeon.telemetry-events-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mcp-schema-test clj-surgeon.memory.oom-reproduction-test clj-surgeon.mission-usage-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-git-submodule-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-provider-fallback-events-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-feature-thread-test clj-surgeon.reader-eval-fence-test clj-surgeon.insertion-gap-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-cold-verify-test clj-surgeon.file-ops-test clj-surgeon.mission-git-identity-test clj-surgeon.namespace-split-test clj-surgeon.worktree-lifecycle-cli-test clj-surgeon.mcp-program-tool-test clj-surgeon.analyze-test clj-surgeon.memory-battery-test clj-surgeon.mission-display-test clj-surgeon.scope-stream-test clj-surgeon.admit-patch-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.registration-controls-test clj-surgeon.mcp-operation-registry-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-tool-test clj-surgeon.failure-report-test clj-surgeon.mission-git-boundary-test clj-surgeon.structural-lens-test clj-surgeon.mcp-inspect-tool-test clj-surgeon.census-pool-test clj-surgeon.namespace-split-warm-test clj-surgeon.mission-git-test clj-surgeon.diff-impact-test clj-surgeon.worktree-lifecycle-recovery-test clj-surgeon.alias-migration-test clj-surgeon.mcp-server-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-helper-extraction-test clj-surgeon.mission-forms-test clj-surgeon.outline-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.rename-alias-parity-test clj-surgeon.platform-selector-test clj-surgeon.mcp-feature-thread-sed-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-contract-test clj-surgeon.xray-test clj-surgeon.insert-forms-test clj-surgeon.analyzer-contract-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mission-commit-cli-test clj-surgeon.mcp-compact-relations-test clj-surgeon.memory.journal-green-test clj-surgeon.mcp-prepared-wire-test clj-surgeon.mission-events-test clj-surgeon.syntax-var-refs-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mission-typist-executor-admission-test clj-surgeon.rename-alias-receipt-test clj-surgeon.rename-alias-performance-test clj-surgeon.battery-parallel-test clj-surgeon.mcp-hot-verify-test clj-surgeon.helper-extraction-test clj-surgeon.test-registration-battery-test clj-surgeon.mcp-operation-test clj-surgeon.mission-phase-events-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.split-proof-gate-test clj-surgeon.outline-corpus-integration-test clj-surgeon.mcp-operation-async-test clj-surgeon.require-change-test clj-surgeon.mcp-write-refusal-test clj-surgeon.recovery-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.insert-forms-parity-test clj-surgeon.mission-fallback-test clj-surgeon.partition-all-test clj-surgeon.repository-hygiene-test clj-surgeon.mcp-compact-location-test clj-surgeon.lane-manifest-test clj-surgeon.agent-routing-test clj-surgeon.mcp-intent-contract-test clj-surgeon.outline-memory-test clj-surgeon.cljc.merge-test clj-surgeon.mission-usage-executor-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.cljc.split-test clj-surgeon.extract-header-test clj-surgeon.fix-declares-test clj-surgeon.mission-typist-test clj-surgeon.quoted-var-refs-test clj-surgeon.state-home-admission-test clj-surgeon.cli-dispatch-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-relation-census-test clj-surgeon.edit-test clj-surgeon.jvm-error-test clj-surgeon.cljc-existing-ops-test clj-surgeon.forms-test clj-surgeon.rename-alias-test clj-surgeon.insert-forms-receipt-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.owner-hypotheses-test clj-surgeon.split-proof-gate-boundary-test clj-surgeon.rename-test clj-surgeon.outline-differential-test clj-surgeon.help-test clj-surgeon.install-test clj-surgeon.mcp-change-buffer-test clj-surgeon.worktree-lifecycle-prune-test clj-surgeon.probe-state-test clj-surgeon.mcp-process-test clj-surgeon.splice-envelope-test clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-inspect-cold-job-test clj-surgeon.edn-config-integration-test clj-surgeon.intent-transaction-test clj-surgeon.mission-publication-test clj-surgeon.operation-algebra-test clj-surgeon.txn-journal-test} #{clj-surgeon.mcp-http-server-test clj-surgeon.require-change-boundary-test clj-surgeon.show-form-test clj-surgeon.mcp-recovery-test clj-surgeon.parser-admission-test clj-surgeon.core-discovery-test clj-surgeon.mission-plain-forms-test clj-surgeon.mission-git-process-test clj-surgeon.move-test clj-surgeon.outermost-test clj-surgeon.mcp-expect-guard-test clj-surgeon.workspace-onboarding-test clj-surgeon.mcp-alias-migration-test clj-surgeon.relation-census-test clj-surgeon.mission-test clj-surgeon.move-dependency-test clj-surgeon.extract-test clj-surgeon.lens-query-test clj-surgeon.mcp-paths-test clj-surgeon.ns-isolation-test clj-surgeon.tmp-leak-support-test clj-surgeon.mission-candidate-race-test clj-surgeon.mission-git-ledger-test clj-surgeon.ls-tree-test clj-surgeon.mission-candidate-test clj-surgeon.mission-git-fence-test clj-surgeon.battery-state-admission-test clj-surgeon.receipt-artifacts-boundary-test clj-surgeon.cljc.analyze-test clj-surgeon.mission-typist-executor-test clj-surgeon.cell-b-oracle-test clj-surgeon.mcp-relation-census-launcher-test clj-surgeon.mcp-extraction-plan-test clj-surgeon.mcp-semantic-client-test clj-surgeon.diagnostic-delta-test clj-surgeon.mission-run-test clj-surgeon.telemetry-events-test clj-surgeon.fast-lane-isolation-test clj-surgeon.mcp-schema-test clj-surgeon.memory.oom-reproduction-test clj-surgeon.mission-usage-test clj-surgeon.mission-forms-source-test clj-surgeon.mission-git-submodule-test clj-surgeon.mcp-telemetry-test clj-surgeon.mission-provider-fallback-events-test clj-surgeon.mcp-workspace-test clj-surgeon.mcp-formatter-test clj-surgeon.mcp-feature-thread-test clj-surgeon.reader-eval-fence-test clj-surgeon.insertion-gap-test clj-surgeon.mcp-extraction-test clj-surgeon.mcp-cold-verify-test clj-surgeon.file-ops-test clj-surgeon.mission-git-identity-test clj-surgeon.namespace-split-test clj-surgeon.worktree-lifecycle-cli-test clj-surgeon.mcp-program-tool-test clj-surgeon.analyze-test clj-surgeon.memory-battery-test clj-surgeon.mission-display-test clj-surgeon.scope-stream-test clj-surgeon.admit-patch-test clj-surgeon.mcp-read-request-normalization-test clj-surgeon.registration-controls-test clj-surgeon.mcp-operation-registry-test clj-surgeon.battery-ledger-test clj-surgeon.mcp-tool-test clj-surgeon.failure-report-test clj-surgeon.mission-git-boundary-test clj-surgeon.structural-lens-test clj-surgeon.mcp-inspect-tool-test clj-surgeon.census-pool-test clj-surgeon.namespace-split-warm-test clj-surgeon.mission-git-test clj-surgeon.diff-impact-test clj-surgeon.worktree-lifecycle-recovery-test clj-surgeon.alias-migration-test clj-surgeon.mcp-server-test clj-surgeon.mcp-create-files-test clj-surgeon.mcp-helper-extraction-test clj-surgeon.mission-forms-test clj-surgeon.outline-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.rename-alias-parity-test clj-surgeon.platform-selector-test clj-surgeon.mcp-feature-thread-sed-test clj-surgeon.mcp-compact-edit-test clj-surgeon.mcp-contract-test clj-surgeon.xray-test clj-surgeon.insert-forms-test clj-surgeon.analyzer-contract-test clj-surgeon.mcp-combinable-transaction-test clj-surgeon.mission-commit-cli-test clj-surgeon.mcp-compact-relations-test clj-surgeon.memory.journal-green-test clj-surgeon.mcp-prepared-wire-test clj-surgeon.mission-events-test clj-surgeon.syntax-var-refs-test clj-surgeon.mcp-prepared-request-test clj-surgeon.mission-typist-executor-admission-test clj-surgeon.rename-alias-receipt-test clj-surgeon.rename-alias-performance-test clj-surgeon.battery-parallel-test clj-surgeon.mcp-hot-verify-test clj-surgeon.helper-extraction-test clj-surgeon.test-registration-battery-test clj-surgeon.mcp-operation-test clj-surgeon.mission-phase-events-test clj-surgeon.edit-dsl-test clj-surgeon.worktree-lifecycle-test clj-surgeon.split-proof-gate-test clj-surgeon.outline-corpus-integration-test clj-surgeon.mcp-operation-async-test clj-surgeon.require-change-test clj-surgeon.mcp-write-refusal-test clj-surgeon.recovery-test clj-surgeon.mcp-relation-census-round20-test clj-surgeon.insert-forms-parity-test clj-surgeon.mission-fallback-test clj-surgeon.partition-all-test clj-surgeon.repository-hygiene-test clj-surgeon.mcp-compact-location-test clj-surgeon.lane-manifest-test clj-surgeon.agent-routing-test clj-surgeon.mcp-intent-contract-test clj-surgeon.outline-memory-test clj-surgeon.cljc.merge-test clj-surgeon.mission-usage-executor-test clj-surgeon.mcp-compact-edit-fields-test clj-surgeon.cljc.split-test clj-surgeon.extract-header-test clj-surgeon.fix-declares-test clj-surgeon.mission-typist-test clj-surgeon.quoted-var-refs-test clj-surgeon.state-home-admission-test clj-surgeon.cli-dispatch-test clj-surgeon.receipt-booleans-test clj-surgeon.mcp-relation-census-test clj-surgeon.edit-test clj-surgeon.jvm-error-test clj-surgeon.cljc-existing-ops-test clj-surgeon.forms-test clj-surgeon.rename-alias-test clj-surgeon.insert-forms-receipt-test clj-surgeon.worktree-lifecycle-io-test clj-surgeon.owner-hypotheses-test clj-surgeon.split-proof-gate-boundary-test clj-surgeon.rename-test clj-surgeon.outline-differential-test clj-surgeon.help-test clj-surgeon.install-test clj-surgeon.mcp-change-buffer-test clj-surgeon.worktree-lifecycle-prune-test clj-surgeon.probe-state-test clj-surgeon.mcp-process-test clj-surgeon.splice-envelope-test clj-surgeon.mcp-namespace-split-test clj-surgeon.mcp-inspect-contract-test clj-surgeon.cljc.require-ops-test clj-surgeon.mcp-inspect-cold-job-test clj-surgeon.edn-config-integration-test clj-surgeon.intent-transaction-test clj-surgeon.mission-publication-test clj-surgeon.operation-algebra-test clj-surgeon.txn-journal-test}))
```

**Other checks and preservation.** Standard Clojure Style formatted the five
changed Clojure files; paved `~/bin/clj-kondo` reported **0 errors, 0 warnings**.
Python syntax parsed successfully. The focused budget-isolation suite passed
**27 tests / 167 assertions**, with no failures, errors or isolation violations. `make census-regenerate` exited **0** with
**+0/-0**. Qualified-name comparison with 99b57bd1: **45 → 47**, missing **[]**,
duplicates **[]**. The two protected ledgers are byte-identical to the starting tip:

```text
08a1d6ee9c7f319c4a146ec23798e535273a47b7633697354014b318bff8ad39  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

No `make test` or `make test-battery` ran on the live tree. The matrix's ordinary
runner executions and all plants were in copies; no live namespace was planted.
No product path derives from the operator's scratch root. The focused battery
runner was invoked directly, without the full battery suite or ledger publication.
All matrix fixtures clean up in `finally`; retained verification logs and copies
remain under the approved scratch root for review. Cell logs were independently
retained before cleanup as `round6/final-matrix-logs/`, with
`round6/final-matrix-cells.json`; aggregate receipts are `final-battery.edn`,
`final-lane-clean.edn`, `final-lane-plant.edn`, and `final-make-lane.json`.
The test wrapper uses `println` so successful per-cell evidence is flushed
before the runner exits; the final matrix log includes all 32 JSON cell receipts.

## Round 7

Recorded 2026-09-14T14:25:38.073733+00:00; branch `fable/regns-entrance`.

### Finding: the packet does not demonstrate a live-tree plant

The reported six assertions are the **expected mask-10 copied gate refusal**,
printed inside lane 3's matrix output, then repeated in its failed assertion.
`prewarm6-battery.log:124` opens lane 3; lines 129–139 print masks 0–10; line 139
records `lane_wall_ms: 8261, okay: false`; lines 140–250 are the copied gate's
output. Line 254 is the outer matrix assertion. The next outer lane header is
line 426. None of the eight outer lane inventories contains lane-manifest-test.
That namespace is :fast, not part of the outer battery inventory.

Mask 10 removes author metadata and adoption. Its six expected failures name
Sol's namespace; its 8,261 ms wall exceeded the 8,000 ms assertion in the old
`test/regns_gate_matrix.py:72`. This is a budget failure, not evidence that
another live-tree lane observed the plant. No claim of a reproduced live-tree
contamination RED is made.

### Trace and original-code concurrent witness

Original fixture source: `4a43f8dbe817670d58726ebe14a3a86586fee88e`.
The original seed, mask 10 and cold lane argv were traced in
`/var/tmp/forge/regns-fx/round7/trace/repo` using `strace -f -yy -s 4096 -e trace=%file`.
The decoder joins unfinished/resumed syscalls and collects successful write-open,
create, rename, link, unlink, directory and metadata operations. Raw traces and
all resolved paths remain in the scratch evidence; instrumentation is not product
code. It found 1,197 write-intent records, 545 distinct paths outside the copy,
and **zero paths in the live checkout**. External paths are temporary test trees
(528 paths), JVM `/tmp/hsperfdata_forge/<pid>` (six), `/proc/<pid>/coredump_filter`
(six), `/dev/null`, and four Clojure dependency-cache paths under
`~/.gitlibs/_repos/https/github.com/bhauman/clojure-mcp/` (`FETCH_HEAD`, `HEAD`,
`HEAD.lock`, `objects/maintenance.lock`). The trace demonstrates the incidental
JVM PerfData exception to java.io.tmpdir; the new Make environment supplies `-XX:-UsePerfData`; the existing registration
control wrapper replaces JAVA_TOOL_OPTIONS, so this does not claim suppression
of PerfData in every registration control JVM.
No external registration, census or caller-state walls/control write was observed.
The complete unique-path list is linked below; no outside path is silently
classified as a registration leak.

Path ownership at the original tip:

- `test/regns_gate_matrix.py:19`: source inferred from cwd; `:24` inherits all
  environment routing, while `:33` correctly sets subprocess cwd to the copy.
- `test/clj_surgeon/registration_gate_fixture.clj:26,30,42–55`: fixture uses
  relative source/manifest/witness/control names and registers `"."`.
- `src/clj_surgeon/test_registration.clj:292–300,367–372`: all registration
  file reads/writes resolve through `safe-file root path`; the trace confirms
  copy-local manifest, witness, census and controls. `:408–415` sets child cwd
  and temporary environment; it does not select a different registration root.
- `src/clj_surgeon/test_census.clj:47–61`: the filesystem adapter takes a path;
  the registration planner calls its four-argument data/writer adapter
  (`test_registration.clj:263`). That adapter does not write to cwd.
- `test/clj_surgeon/battery_parallel_runner.clj:66–77`: walls derive from seat
  state and workspace admission. The old direct lane argv does not publish the
  coordinator's battery walls. Real Make cells now admit their own external
  state root instead of inheriting this seat's routing.

Two bounded JVMs launched the unchanged battery namespace and live manifest
namespace from the live checkout. Before/after snapshots include git porcelain,
SHA-256 of **all tracked files**, census and state walls/controls. They are
byte-identical. The live namespace passed: **46 tests / 2,064 assertions**, zero
failures/errors, 5,253 ms. The matrix failed its timing check at mask 17 (10,315 ms),
with the expected Sol-only copied refusal. Scratch tracing and a real-Make pilot
also overlapped this run; these walls are not a controlled performance comparison.

A second trace starts from `git archive 4a43f8db` and includes actual `write`,
`writev`, `pwrite64`, `pwritev`, `ftruncate`, `copy_file_range` and `sendfile`
syscalls as well as `%file`. It records **2,568 destination events**, including
**1,340 successful filesystem write syscalls**. There are **550 distinct outside
paths** (546 reached by successful operations), **zero live-tree destinations**
and no unresolved filesystem write descriptors. Pipes, sockets and eventfds are
non-filesystem communication, not omitted destination paths. Failed attempts
include `/dev/tty` and dependency-cache bookkeeping; successful outside writes
remain runtime/temp/cache/log destinations, not live registration or state files.

Every outside path is listed in [the byte-trace destination list](2026-09-14-register-test-ns-round7/outside-write-paths.txt);
[the initial file-operation list](2026-09-14-register-test-ns-round7/initial-outside-paths.txt)
retains the earlier run's distinct temporary names. The trace driver, decoder,
summary and raw-artifact SHA-256 manifest are in the same evidence directory.
The raw syscall traces and complete per-write JSON remain under the approved
scratch root and can be reconstructed from the frozen tip.

The original copied mask-10 output is preserved as
[copied-mask10-red.log](2026-09-14-register-test-ns-round7/copied-mask10-red.log).
Its first refusal begins:

```text
FAIL in (every-manifest-namespace-declares-its-lane-in-its-own-ns-form) (lane_manifest_test.clj:662)
source metadata agrees with the manifest, per namespace
1 namespace(s) whose ns metadata does not declare the manifest's lane (expected/declared): clj-surgeon.sol-first-contact-test want :battery got nil
```

### TDD and implemented ratchet

RED commit `25ef0489` adds REGNS-012, the explicit-root invocation from an
unrelated cwd, and independent inherited-routing/snapshot tests. All four Python
checks fail on the old harness because it has no isolation/snapshot boundary.
This RED is recorded accurately; it is not a fabricated live plant.

The matrix now receives explicit source and scratch roots. Fixture writes receive
the copy's explicit root. Each mask has its own copied repository and private
state/artifact/temp directories. The nested command is actual
`make -C <copy> test-fast`, with cwd set; no Make target, test body or repository
Var is replaced. Inherited CLJ_SURGEON/GIT/Make routing, JVM property injections,
census-write authorization and ledger-append authorization are discarded.
The private state root is also the explicit artifact envelope: a pilot with a
sibling state root but no admitted envelope correctly refused state admission.

The matrix launches a separate 1024 MB manifest JVM against the caller tree
concurrently with actual seed registration. It uses direct runner mode, whose
exit includes namespace isolation and budget violations. It hashes the caller's test/registration
surfaces, control inventory, protected ledgers and state walls/controls, and
compares git status and hashes after every cell and in `finally`. The snapshot
oracle independently rejects changed bytes despite unchanged git status, added
or removed state controls, and changes observed on disk. Reports identify the
child root/argv and distinguish diagnostic, budget and live-observer outcomes.

The public registration entrance already resolves registration writes from its
explicit root, and its shared census transaction adapter was not a leak site.
Those production implementations retain their behavior (register! gains its
REGNS-012 traceability annotation); this change removes implicit
roots and inherited routing at the witnessed harness boundary.

Real-Make pilots took 31–34 seconds per cell. Thirty-two complete Make gates plus
the concurrent observer require a larger namespace ceiling than 32 direct lane processes:
1,500,000 ms is reserved for this matrix, retaining fast's 8,000 ms ceiling and
the battery cadence's independent 1,800,000 ms limit. This is additional
acceptance coverage, not a speed claim.

The first stronger-matrix execution ran a JVM observer for every mask. Its
saved mask-15 live receipt had passing counters but a TEST-ISO-007 violation:
`elapsed-ms 8064`, over 8000 ms. `--emit-edn` intentionally does not fold that
violation into its process exit. That preliminary run was stopped after its
active mask-19 gate finished; its live snapshot was still byte-identical. It is
not a GREEN receipt. The final harness uses direct observer mode and observes
seed registration once, matching the requested two-process class boundary,
instead of multiplying fresh JVM startup variance across 32 observers. Every
mask still runs complete real Make, and every mask still checks caller bytes.

The old copied matrix forced `lane-command` (JVM) for lane-manifest-test.
Real `make test-fast` uses the manifest's :bb assignment for that namespace.
The final receipts therefore measure the actual fast-gate runtime; they do not
claim a matched performance comparison between the two process shapes.

### Verification

The final two namespaces ran concurrently in separate `-Xmx1024m` JVMs with
`CLJ_SURGEON_STATE_HOME` explicitly pointing at the live state root in the outer
environment. Both exited **0**; each EDN receipt has zero failures/errors, zero
leaks and an empty isolation-violations vector:

- `test-registration-battery-test`: **1 test / 2 assertions**, **1,332,598 ms**.
- Live `lane-manifest-test`: **46 tests / 2,064 assertions**, **5,775 ms**.
- The matrix's direct live observer during enrollment: **46 tests / 2,064
  assertions**, **5,651 ms**, zero isolation violations.
- All **32 real-Make cells** accepted their required outcome. Nested manifest
  walls were **3,709–4,274 ms**. The matrix wall was **1,329.814 s**.
- The permanent snapshot covered **1,966 files**. The external witness compared
  **11,688 tracked/state entries** plus git porcelain before and after: identical.
- All 32 retained raw Make logs are complete, have zero test errors and zero
  isolation violations, and have no failing test outside lane-manifest-test.
- Four independent Python isolation tests pass. Standard Clojure Style and paved
  `~/bin/clj-kondo` pass: **0 errors, 0 warnings**.
- `make census-regenerate`: **exit 0, +0/-0**. Intent audit passes after linking
  the already-rooted registration entry point to REGNS-012.

Actual green output:

```text
test-registration-battery-test 0
lane-manifest-test 0
snapshot identical True
REAL-FIRST-CONTACT-WALL 1329.814
LIVE-SNAPSHOT byte-identical {"root": "/home/forge/src/clj-surgeon-regns", "files": 1966, "state": "/home/forge/.local/state/clj-surgeon"}
census-regenerate: +0/-0
```

| Mask | Make exit | Manifest wall (ms) | Class oracle |
| --- | --- | --- | --- |
| 0 | 0 | 3937 | PASS |
| 1 | 2 | 3936 | PASS |
| 2 | 2 | 3888 | PASS |
| 3 | 2 | 3925 | PASS |
| 4 | 2 | 3864 | PASS |
| 5 | 2 | 3907 | PASS |
| 6 | 2 | 3944 | PASS |
| 7 | 2 | 3878 | PASS |
| 8 | 2 | 4059 | PASS |
| 9 | 2 | 3709 | PASS |
| 10 | 2 | 3765 | PASS |
| 11 | 2 | 3902 | PASS |
| 12 | 2 | 3839 | PASS |
| 13 | 2 | 3872 | PASS |
| 14 | 2 | 4007 | PASS |
| 15 | 2 | 3772 | PASS |
| 16 | 2 | 3817 | PASS |
| 17 | 2 | 3996 | PASS |
| 18 | 2 | 4040 | PASS |
| 19 | 2 | 3759 | PASS |
| 20 | 2 | 3846 | PASS |
| 21 | 2 | 3960 | PASS |
| 22 | 2 | 4274 | PASS |
| 23 | 2 | 4074 | PASS |
| 24 | 2 | 3964 | PASS |
| 25 | 2 | 3810 | PASS |
| 26 | 2 | 3824 | PASS |
| 27 | 2 | 3814 | PASS |
| 28 | 2 | 3808 | PASS |
| 29 | 2 | 3813 | PASS |
| 30 | 2 | 3876 | PASS |
| 31 | 2 | 3769 | PASS |

Nonzero Make exits above are the deliberate missing-registration refusals.
[matrix-cells.json](2026-09-14-register-test-ns-round7/matrix-cells.json) preserves
each exact argv, copy root, first failure/checklist subject and budget result.
[namespace-results.json](2026-09-14-register-test-ns-round7/namespace-results.json)
and [snapshots.json](2026-09-14-register-test-ns-round7/snapshots.json) retain the
independent live results and every tracked registration/control/state hash.
The full before/after snapshots, all 32 raw copied Make logs and syscall traces
remain in `/var/tmp/forge/regns-fx/round7`, with hashes in the evidence manifest.

The protected ledger and census bytes still equal the starting `4a43f8db`:

```text
08a1d6ee9c7f319c4a146ec23798e535273a47b7633697354014b318bff8ad39  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
7cc932304e6d74c0caa62b96095fb0232119e3052c9e6e64124c02eb0775fadd  test/clj_surgeon/deftest_census.edn
```

No full `make test` or `make test-battery` ran on the live tree. Verification used the requested
namespace JVMs, the matrix's live observer, Python isolation tests, lint, census
regeneration and the intent audit. All plants, real Make gates and tracing ran in scratch;
product paths do not contain the operator's scratch prefix. Matrix copies clean
up automatically. The report deliberately does not claim the requested live-leak
RED: the evidence supports a copied-cell timing failure and harness hardening.
