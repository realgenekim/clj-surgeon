# Edit-to-probe meter, block A

Seat: /home/forge/src/clj-surgeon-bbtower, bb-rewrite-tower-local, September 11,
2026. Same namespace throughout: clj-surgeon.forms-test, 24 tests / 94 assertions.
The one warm JVM was started with `make warm PORT=9107`, heap 1 GiB. Both CLI
and cold portable runs use bb. No other local suite ran during these samples.

Each sample lands a reversible comment edit through native apply_patch (no
structural form/alias verb fits a comment replacement), captures `date +%s.%N`
after the patch returns, runs the focused command, and captures the same clock
after the process prints its verdict and exits. Thus the wall includes startup,
transport, reload/tests, rendering and exit; it excludes authoring and patch time.
These are comment-edit reload costs, not a semantic-edit workload or a speed
admission. All 15 exits were zero and all runs retained 24 tests / 94 assertions.

| Run | Cold JVM seconds | Cold bb seconds | Warm probe seconds |
|---|---:|---:|---:|
| 1 | 3.135338176 | 0.045910513 | 0.359213035 |
| 2 | 3.103918246 | 0.052564329 | 0.348543651 |
| 3 | 3.071775631 | 0.047694287 | 0.355682673 |
| 4 | 3.140376179 | 0.044548521 | 0.352632251 |
| 5 | 3.185531059 | 0.042671989 | 0.353943146 |
| Median | 3.135338176 | 0.045910513 | 0.353943146 |

Cold JVM: `clojure -J-Xmx1g -M:clj-surgeon/test-deps -e` requiring forms-test,
running clojure.test/run-tests and exiting from fail+error. Cold bb uses the
same expression with explicit `-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx`.
Warm: `bb -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -m clj-surgeon.core :probe
:ns clj-surgeon.forms-test`. TMPDIR and JAVA_TOOL_OPTIONS point to the same root.

The complete raw timestamps, exact script, patch logs and per-run verdicts are
in /var/tmp/forge/bbtower-fx/meter.tsv, meter.sh and meter-*.log. The portable
cold bb command wins this namespace; the warm probe is useful when the cold
alternative needs the JVM. No claim of free-choice adoption or statistical
performance admission follows from five ordered samples.

Live negative witnesses: replacing the first expected classification with
:probe-deliberate-red returned probe-failed, 24 tests / 94 assertions / 1 failure,
86.376872 server ms and exit 1. Restoring it returned probe-passed, 0 failures,
89.375258 server ms. Both retain verification_complete false and pending
landing-gate. A changed server fingerprint refused at the client; a forged
generation descriptor reached the server and refused as stale-probe-image
before reload (0.757867 server ms). All specimen edits were restored.

## Complete coordinator wall

Shell timestamps bracket the Make coordinator entrance, including its bb
library prerequisite. The retained eae1e432 control took 35.050757448 s,
passing 755 tests / 7,931 assertions across 61 namespaces, plus the library's
5 tests / 59 assertions. The final hybrid took 86.983589821 s with 1 GiB bb
children, running 1,631 tests / 15,788 assertions across 109 namespaces, plus
the same library. Every assertion passed, but TEST-ISO-007 failed: 68,210 ms
summed fast-lane test time exceeds its unchanged 60,000 ms budget.

This is not an equal-coverage speed comparison: the new entrance includes the
old bb lane. The complete wall regressed, and the performance gate remains
outstanding. A preceding 512 MiB run took 161.427512594 s and failed the same
budget at 70,937 ms. Raising the heap improved complete wall but did not earn
fast-lane acceptance. No budget was relaxed or namespace dropped to turn it green.

The standalone new-shape bb gate passed all 898 tests. The twenty verb groups
passed 420 assertions within the hybrid run. The checked-in deftest census
remains 1,690 entries, with no additions or removals.
