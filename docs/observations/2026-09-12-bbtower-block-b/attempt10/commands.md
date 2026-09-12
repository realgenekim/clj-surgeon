# Reproduction and execution order

All suite processes use `TMPDIR=/var/tmp/forge/bbtower-fx`,
`JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'`,
and no `_JAVA_OPTIONS`. JVM commands explicitly specify heap caps.
Only one suite/gate/measurement ran at a time; coordinator-owned workers
used the repository's bounded scheduler.

Focused probe checks used:

```sh
clojure -J-Xmx1g -M:clj-surgeon/test-deps -e '(require (quote clj-surgeon.splice-envelope-test) (quote clj-surgeon.receipt-booleans-test) (quote clj-surgeon.help-test)) (let [r (apply clojure.test/run-tests (quote [clj-surgeon.splice-envelope-test clj-surgeon.receipt-booleans-test clj-surgeon.help-test]))] (System/exit (+ (:fail r) (:error r))))'
```

`servlet-plant-red.log` changes only probe-servlet's refusal argument from
`:invalid-probe-request` to `:probe-servlet-unregistered-kind`, then executes
splice-envelope-test. `dead-row-red.log` inserts
`{:type :probe-registered-but-never-emitted}` into the refusal registry and
executes the same witness: the vocabulary equality names the extra kind;
the deliberately minimal row also fails row-shape validation. Both plants
were restored. `probe-final-green.log` verifies the restored registry and
owner sources. The later `dead-valid-row-red.log` check copies a complete
registry row, changes only its type and injects that registry through the
`slurp` boundary while the real completeness test runs. It isolates the
vocabulary failure from row-shape validation; the scoped reader binding
restores automatically.

Calibration, after rejecting the live-in-tree output attempt:

```sh
bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -m clj-surgeon.battery-parallel-runner --suite fast --work-dir /var/tmp/forge/bbtower-fx/attempt10-ceiling
bb -Xmx1g docs/observations/2026-09-12-bbtower-block-b/attempt10/derive-ceiling.clj
```

The calibration receipt was copied unchanged to `ceiling-run/`. Frozen
maps in `ceiling-inputs.edn` preserve the run's cadence; the replay recomputes
bb membership from shipped runtime assignments, asserting agreement for
every measured member. An unrelated integration runtime repair cannot
silently alter either input sum.

The actual diagnostic was run before and after its fix:

```sh
bb -Xmx1g test/run_all.clj
```

Before: exit 96, diagnostic-red.log. After: exit 0, diagnostic-green.log.
Default selection and required-stage tests are in diagnostic-gate-final.log.

The cadence ledger was regenerated only through its declared direct
`CENSUS_REGENERATE=1` entrance, outside make; census-proof.clj independently
compares all baseline/current test names and ledger additions/removals.

Final standalone and gate sequence:

```sh
make test-fast
/usr/bin/time -p make test-integration
/usr/bin/time -p make landing-gate-prewarm
bb -Xmx1g test/run_all.clj --emit-edn /var/tmp/forge/bbtower-fx/attempt10-outline-bb.edn --ns clj-surgeon.outline-corpus-integration-test
clojure -J-Xmx1g -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --emit-edn /var/tmp/forge/bbtower-fx/attempt10-outline-jvm.edn --ns clj-surgeon.outline-corpus-integration-test
/usr/bin/time -p make landing-gate-prewarm
```

There was exactly one standalone make test-fast and one make test-integration.
The fast run's fixture-scan false positive was repaired and focused-tested
before integration. Prewarm 1's bb namespace budget refusal received the one
allowed runtime repair; prewarm 2 was the final permitted attempt. No budget
was raised and no test was deleted or skipped.

Live outputs stayed under `/var/tmp` or the coordinator's ignored `target/`
tree, and were archived only after execution. The untracked observation
directory was temporarily held under `/var/tmp` during complete runs, because
the inherited CLI receipt currently enumerates every untracked path without
a bound. This does not repair or conceal that inherited defect: the failed
calibration transcript and explicit owed item retain it.
