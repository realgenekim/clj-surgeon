NO-GO

CLASS: STALE-CONTROL-MATRIX — enumerate `control-paths × controls-valid?` acceptance branches; the oracle plants a failing test plus stale/wrong-hash receipts at every predictable path and requires an executed argv or refusal.

F1 — Production accepts forged/stale controls without executing the test.

I planted `(is false)` and two `.control.edn` files claiming pass with `:source-sha256 "stale"`, then ran:

```sh
make register-test-ns \
  NS=clj-surgeon.sol-stale-test LANE=battery RUNTIME=jvm
```

Output, exit 0:

```edn
{:ok true
 :state :registered
 :controls
 {:jvm {:status :passed :exit 0
        :result {:test 1 :pass 1 :fail 0 :error 0}}
  :bb  {:status :passed :exit 0
        :result {:test 1 :pass 1 :fail 0 :error 0}}}}
```

Neither returned control contained `:command`; the failing test did not execute. [`register!`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/test_registration.clj:450) trusts `read-controls` whenever `controls-valid?` accepts them, but that predicate checks neither source hash, root, command nor execution provenance.

The witness currently reinforces the hole: `registration-control-results-are-executions` constructs pass maps without execution or subjects at [lane_manifest_test.clj:1599](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:1599).

The test-only `with-redefs` seam is not reachable through production `-main`; production reaches the defect through predictable checked-in control paths instead.

CLASS: FIRST-CONTACT-GATE-MATRIX — enumerate all 32 missing-surface masks through ordinary gate execution order; the oracle must assert that the earliest failure contains the complete checklist and exact Make remedy.

F2 — The first ordinary gate failure does not print the checklist or remedy.

With a new metadata-bearing namespace and no registrations, I ran the focused ordinary gate:

```sh
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e \
"(require 'clj-surgeon.lane-manifest-test 'clojure.test)
 (binding [clojure.test/*report-counters*
           (ref clojure.test/*initial-report-counters*)]
   (clojure.test/test-vars
     [#'clj-surgeon.lane-manifest-test/every-manifest-entry-exists-on-disk])
   (System/exit 1))"
```

First output, exit 1:

```text
FAIL in (every-manifest-entry-exists-on-disk) (lane_manifest_test.clj:182)
every discovered test namespace has a closed runtime declaration
expected: (= (set (keys @on-disk)) (set (keys runtimes)))
actual: (not (= #{... clj-surgeon.sol-gate-test ...} #{...}))
```

There is no checklist and no `make register-test-ns` remedy. The complete diagnostic is attached only to later gates, after this assertion at [lane_manifest_test.clj:182](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:182).

CLASS: CENSUS-WRITER-CENSUS — enumerate every writer of `test/clj_surgeon/deftest_census.edn`; the oracle must identify exactly one implementation of derivation, removed-name refusal and serialization.

F3 — The entrance reimplements census regeneration.

```sh
make -n register-test-ns NS=clj-surgeon.example-test LANE=battery RUNTIME=jvm
make -n census-regenerate
```

Output:

```text
clojure ... -m clj-surgeon.test-registration
env ... CENSUS_REGENERATE=1 clojure ... \
  #'clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown
```

The entrance independently scans tests and appends census members at [test_registration.clj:254](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/test_registration.clj:254). The existing authoritative derivation and guarded writer are at [lane_manifest_test.clj:1056](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/lane_manifest_test.clj:1056), whose ledger explicitly says “DERIVED, regenerated, never hand-edited.” These are two implementations.

Other pressure-point results:

- Valid multiline entries and an in-map trailing comment were preserved; only the requested registrations were added. A reader conditional cannot reach this entrance in `.clj`: Clojure refused it with `Conditional read not allowed` before `-main`.
- A real failing control returned `:register-control-failed :state :rolled-back`; six relevant files were byte-identical and zero artifacts remained.
- An exact repeat returned `:state :unchanged`; `LANE=fast` refused with `:actual :battery :expected :fast`.
- Removing an enrolled source made the census gate exit 1, although through `Cannot open <nil> as a Reader`, not a named removal diagnostic.
- `make test` was not run.
- Candidate reviewed: `813a03834894a2716df0cb812e2eeea9686b587d`.
- No fix was left in the worktree; the pre-existing untracked review log was untouched.

> END RECEIPT (fence-run): worktree HEAD at review exit = 813a03834894a2716df0cb812e2eeea9686b587d = fenced sha.
