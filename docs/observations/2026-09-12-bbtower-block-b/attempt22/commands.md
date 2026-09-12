# Executed entrances

All suite processes used `TMPDIR=/var/tmp/forge/bbtower-fx`,
`JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'`, and
unset `_JAVA_OPTIONS`. Later bb controls explicitly pass
`-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx` as recorded by the runner.

## Original red controls

The tip controls ran at ac197797, the receipt-preservation commit above
357a2b79; source was unchanged. Trunk used:

```sh
git clone --shared --no-checkout . target/attempt22-trunk
git -C target/attempt22-trunk checkout --detach a15531ee
```

In each checkout, JVM and bb respectively executed:

```sh
clojure -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.xray-test)(let [r (clojure.test/run-tests 'clj-surgeon.xray-test)] (prn r) (shutdown-agents) (System/exit (+ (:fail r) (:error r))))"
bb -e "(require 'clj-surgeon.xray-test)(let [r (clojure.test/run-tests 'clj-surgeon.xray-test)] (prn r) (shutdown-agents) (System/exit (+ (:fail r) (:error r))))"
```

The `red-{tip,trunk}-{jvm,bb}.log` files retain dates, subjects, expressions,
summaries and exit codes. `preexisting-xray-diff.patch` shows the only change
between a15531ee and 357a2b79 in these files: xray-test's battery metadata.

## Correctness census

```sh
bb docs/observations/2026-09-12-bbtower-block-b/attempt22/run_census.clj
bb docs/observations/2026-09-12-bbtower-block-b/attempt22/fold_census.clj
```

The actual one-time census is at 4492f9df. Every child argv and subject is
retained in `controls/*.command.edn`. Raw results are `*.edn`, stdout/stderr
are adjacent `*.log`, and `*.control.edn` folds the raw result with the
observed process exit and command. `controls/index.edn` is the controller's
append-only result stream. The initial runner argument-order failure ran no
test control and is retained in `census-start-refusal.log`.

## Final evaluator and runtime-rule checks

On each runtime, final evaluator validation required xray-test and edit-dsl-test,
then ran each through `clojure.test/run-tests`, summed failures/errors and exited
with that sum. The JVM entrance was `clojure -M:clj-surgeon/test-deps -e`; bb used
`bb -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -e`. Results are in
`final-evaluator-{jvm,bb}.log`. The exact added real-evaluator matrix is retained
in `xray-real-evaluator.patch`; its pre-correction reds are `real-evaluator-red-*`.

The runtime-rule checks used the same runtime entrances and
`clojure.test/test-vars` on
`clj-surgeon.lane-manifest-test/runtime-portability-controls-cover-every-assignment`,
with bound report counters and failure/error exit codes. Both report exactly
the twelve recorded failing-control namespaces in `runtime-rule-{jvm,bb}.log`.

The named ledger regeneration entrance was:

```sh
CENSUS_REGENERATE=1 bb -e "(require 'clj-surgeon.lane-manifest-test)(clojure.test/test-vars [#'clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown])"
```

## Requested gates

`run_gate.sh` executes exactly one Make target, preserving command, subject,
dates, wall and exit. `checks/index.edn` is the execution ledger. The calls are:

```sh
bash docs/observations/2026-09-12-bbtower-block-b/attempt22/run_gate.sh test-fast fast
bash docs/observations/2026-09-12-bbtower-block-b/attempt22/run_gate.sh test-battery battery
bash docs/observations/2026-09-12-bbtower-block-b/attempt22/run_gate.sh landing-gate-prewarm prewarm
```
