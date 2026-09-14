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
