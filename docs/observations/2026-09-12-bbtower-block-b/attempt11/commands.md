# Attempt11 commands and evidence

All JVM/bb execution used TMPDIR=/var/tmp/forge/bbtower-fx and
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'. Explicit
heap flags were supplied on direct JVM/bb commands; Make's worker commands
retain their shipped explicit caps. No _JAVA_OPTIONS, outer timeout, shared
MCP port, install, push, tag, or concurrent suite was used.

```sh
bb -Xmx1g test/clj_surgeon/bb_ceiling.clj docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-run/receipt.edn
npx @chrisoakman/standard-clojure-style fix test/clj_surgeon/bb_ceiling.clj test/clj_surgeon/ns_isolation.clj test/clj_surgeon/ns_isolation_test.clj test/clj_surgeon/battery_parallel_test.clj test/clj_surgeon/splice_envelope_test.clj
~/bin/clj-kondo --lint test/clj_surgeon/bb_ceiling.clj test/clj_surgeon/ns_isolation.clj test/clj_surgeon/ns_isolation_test.clj test/clj_surgeon/battery_parallel_test.clj test/clj_surgeon/splice_envelope_test.clj
```

Focused tests used this runner, selecting ns-isolation-test for ceiling/spec
reds, splice-envelope-test for each plant, and all three for focused/restored green:

```sh
clojure -J-Xmx1g -M:clj-surgeon/test-deps -e "(require '[clojure.test :as t] 'clj-surgeon.ns-isolation-test 'clj-surgeon.splice-envelope-test 'clj-surgeon.battery-parallel-test) (let [r (t/run-tests 'clj-surgeon.ns-isolation-test 'clj-surgeon.splice-envelope-test 'clj-surgeon.battery-parallel-test)] (System/exit (+ (:fail r) (:error r))))"
CENSUS_REGENERATE=1 clojure -J-Xmx1g -M:clj-surgeon/test-deps -e "(require 'clj-surgeon.lane-manifest-test 'clojure.test) (clojure.test/test-vars [#'clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown])"
make test-fast
make test-integration
make landing-gate-prewarm
```

Each plant ran separately and was restored before the next. The ceiling plant
changes only :bb 343102 to 343103. Source plant patches are retained alongside
their red logs. The stale spec and calibration witnesses first ran against the
original registered 374149 and spec 399155/399156, then passed after repair.

Live suite logs were written under /var/tmp/forge/bbtower-fx and archived after
execution. The untracked attempt11 directory was held there during complete
runs, following attempt10's workaround for the inherited unbounded dirty-tree
CLI receipt. No live output was written into the tracked observation tree.
