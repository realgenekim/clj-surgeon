NO-GO
CLASS: REAL-FIRST-CONTACT-GATE-MATRIX — all 31 nonzero registration-surface masks must be planted in a copied repository and exercised through `make test-fast`; oracle: the first emitted checklist namespace and remedy must name the planted namespace and requested lane/runtime.

Finding `REAL-GATE-001`: the current battery matrix is not seam-free. It invokes the seven gate Vars, but replaces the pinned-count test body with `eval` and redefines repository state. Consequently, it misses the real gate’s global count interaction.

With a new metadata-bearing, otherwise-unregistered `clj-surgeon.sol-first-contact-test`, `make test-fast` exited 2. Its first failure was:

```text
FAIL in (every-manifest-entry-exists-on-disk)
every discovered test namespace has a closed runtime declaration
Registration checklist for clj-surgeon.admit-patch-test:
...
Remedy: make register-test-ns NS='clj-surgeon.admit-patch-test' LANE='battery' RUNTIME='jvm'
```

That namespace is already registered. The correct diagnostic for the planted namespace appeared only at output line 157:

```text
Registration checklist for clj-surgeon.sol-first-contact-test:
...
Remedy: make register-test-ns NS='clj-surgeon.sol-first-contact-test' LANE='fast' RUNTIME='jvm'
```

The global `165 → 166` pin mismatch makes `repository-checklist` emit misleading rows for every existing namespace before reaching the actual newcomer. It also inflated `lane-manifest-test` to 8,151 ms, exceeding its 8,000 ms namespace budget.

Other pressure points passed:

- Both stale-control attacks at `02249934` used:

  ```sh
  make register-test-ns NS=clj-surgeon.sol-stale-test LANE=battery RUNTIME=jvm
  ```

  Both exited 2 with `:state :rolled-back` and `:error-type :register-control-stale`. The correct-hash/no-provenance plant still triggered fresh JVM and bb executions, each reporting one failed test. Enrollment bytes were restored.

- Qualified-name comparison produced:

  ```text
  qualified 99b count: 45
  qualified 022 union count: 47
  missing original qualified names:
  duplicate qualified names:
  ```

  The additions are the source-retention witness and battery-qualified matrix. The fast representative is mask 1; the battery executes all 32 masks, subject to the seam above.

- `rg -n "deftest_census\\.edn" src test Makefile` found the only writer definition in `src/clj_surgeon/test_census.clj`; other matches are readers/tests. Both entrances call its `derived-census` and `regenerate-census!`. `make census-regenerate` returned:

  ```text
  census-regenerate: +0/-0
  ```

  Divergence is not constructible inside the entrance’s admitted canonical `.clj` namespace/file domain.

- The manifest assigns `clj-surgeon.test-registration-battery-test` to `:battery`; `lane-budget-ms` gives battery `1800000`. The real fast-run lane receipt query returned:

  ```edn
  {:namespace clj-surgeon.lane-manifest-test,
   :spawn-ledger-process-rows 0,
   :violations []}
  ```

No `make test` or battery suite was run. No patch was left in the worktree.

> END RECEIPT (fence-run): worktree HEAD at review exit = f6cbc21368aef14d98651671e65f17246b59164c = fenced sha.
