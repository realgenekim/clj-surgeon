# Attempt 12 commands and routing

All test commands use `TMPDIR=/var/tmp/forge/bbtower-fx` and
`JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'`.
Every explicit bb/JVM launch includes `-Xmx1g` (Clojure CLI `-J-Xmx1g`);
the repository coordinator applies its existing bounded worker policy.
No _JAVA_OPTIONS was introduced. One suite ran at a time.

Red command (at the behavior-preserving command extraction):

```sh
bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -e '(require (quote clj-surgeon.battery-parallel-test)) (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (clojure.test/test-vars [(resolve (quote clj-surgeon.battery-parallel-test/bb-lane-child-honours-disk-tmpdir))]) (prn @clojure.test/*report-counters*) (System/exit (if (zero? (+ (:fail @clojure.test/*report-counters*) (:error @clojure.test/*report-counters*))) 0 1)))'
```

An initial shell invocation used a malformed Var literal and failed to parse
before running tests. It was corrected to `resolve`; `red.log` records the
actual behavioral red, not the parse error. No edit-tool refusal occurred.

Initial green: `clojure.test/run-tests` on battery-parallel-test under the
same bb flags, exit chosen from fail+error. Logs: green.log, lint.log.

`make test-fast` ran exactly once. Its two integration failures and unchanged
budget lines are retained in test-fast.log. Repair: preserve the subprocess
witness in the existing receipt-artifacts-boundary-test battery namespace,
leave a pure argv matrix in battery-parallel-test, and regenerate the census:

```sh
CENSUS_REGENERATE=1 bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -e '(require (quote clj-surgeon.lane-manifest-test)) (clojure.test/test-vars [(resolve (quote clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown))])'
```

The reviewed ledger diff added the two named witnesses and removed nothing.
Focused repair checks: `clojure.test/run-tests` on lane-manifest-test and
battery-parallel-test under bb; the subprocess witness alone under
`clojure -J-Xmx1g -M:clj-surgeon/test-deps`, with bound report counters and a
nonzero exit on fail/error. Logs: repair-pure.log, repair-boundary.log.

Formatter: `npx --yes @chrisoakman/standard-clojure-style fix` on each changed
Clojure file. Unrelated indentation churn in two existing boundary forms was
restored with exact native patches. Linter: `~/bin/clj-kondo --lint` on changed
Clojure files; lint-repair.log is the final lint result.

Final prewarm command, source SHA and timestamp: gate-command.txt. It ran once,
after the repair commit. `ls -1A /tmp` with `LC_ALL=C` was captured before
fast, after fast, immediately before prewarm, and after prewarm. Set differences
retain every newly visible entry, without deleting other seats' files.

## DOGFOOD source-edit table

| File | Intent | Mechanism | Refusal type | Repair text sufficient? |
| --- | --- | --- | --- | --- |
| test/clj_surgeon/battery_parallel_runner.clj | Extract identical bb argv for red witness; pass selected tmpdir explicitly; correct stale private-temp comment | Native apply_patch on exact forms, standard-clojure-style | None | N/A |
| test/clj_surgeon/battery_parallel_test.clj | Initial real-child red witness, then retain pure command matrix in fast lane | Native apply_patch, formatter | Gate check: no-fast-lane-namespace-spells-a-child-process | Yes: it identified the launcher and required battery placement |
| test/clj_surgeon/receipt_artifacts_boundary_test.clj | Preserve real-child witness in an existing gate-executed battery namespace | Native apply_patch, formatter; restore unrelated formatting | None | N/A |
| test/clj_surgeon/deftest_census.edn | Register both new witness names, with no removals | Repository CENSUS_REGENERATE entrance, reviewed Git diff | Gate check: census drift | Yes: exact missing name and regeneration command supplied |

Native routing follows the binding brief and working-tree skill's native default
for this bounded change. No Surgeon MCP operations were exposed in this session;
no CLI wrapper substitution, shared service restart, install, push or tag was
performed. The changed coordinator was dogfooded by the real test-fast and
prewarm commands. No performance advantage over native editing is claimed.
