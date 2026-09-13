# Subjects and commands

Initial checkout: bb-rewrite-tower-local at cc50aa2b. Preserve pending battery
row and run12 check-only evidence: ac001e20. Exact red reproduction: 3f276210.

All JVM commands use TMPDIR=/var/tmp/forge/bbtower-fx and
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx';
_JAVA_OPTIONS is unset. Discovery found no worktree nREPL. `make nrepl` started
the owned JVM on 46871 with explicit -J-Xmx512m; the first evaluation asserted
user.dir equals this worktree. System/exit stopped it before cold checks (the
client's EOF is the expected consequence of stopping its server).

Warm tests use `clj-nrepl-eval --port 46871`, requiring changed namespaces with
:reload. red.log retains the old-image false green and failing outer assertion.
focused-first.log is a test-authoring scope error. focused-second.log records
the default io/resource loader mismatch. focused-third.log is the complete
green probe namespace after selecting RT/baseLoader. focused-all.log runs
mcp-hot-verify-test, mcp-http-server-test, admit-patch-test, splice-envelope-test,
and receipt-booleans-test. Inner stale-value assertion failures are intentional;
the enclosing regression suite must pass. refusal-census.log pins the new kind.

sol-nine.log runs exactly these Vars, with explicit test counters:

```
clj-surgeon.cljc.merge-test/unmatched-body-counts-emit-strict-split
clj-surgeon.cljc.split-test/round-trip-unmatched-counts
clj-surgeon.battery-parallel-test/the-lane-budget-is-folded-over-the-union-not-per-lane
clj-surgeon.lane-manifest-test/every-manifest-entry-exists-on-disk
clj-surgeon.lane-manifest-test/runtime-portability-controls-cover-every-assignment
clj-surgeon.mcp-hot-verify-test/probe-authorizes-the-requested-test-target-before-reload
clj-surgeon.mcp-http-server-test/probe-output-degrades-without-deleting-the-verdict
clj-surgeon.mcp-http-server-test/probe-servlet-bounds-the-actual-writer
clj-surgeon.mcp-http-server-test/probe-spec-receipt-shape-matches-an-executed-probe
```

Formatting: `npx --offline @chrisoakman/standard-clojure-style fix` on the two
changed implementation/regression files. Lint: `~/bin/clj-kondo --lint`.
Census: standalone CENSUS_REGENERATE=1 clojure -J-Xmx1g
-M:clj-surgeon/test-deps, running the corpus-growth and sleep-registration Vars,
printing counters and exiting nonzero on failure. No regenerate flag enters Make.

Full checks use `bash attempt25/run_gate.sh LABEL COMMAND...` (full path from
repository root). The wrapper records clock time, HEAD, argv, exit and wall:

```
fast make test-fast
restricted python3 docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py -- make landing-gate-prewarm
prewarm make landing-gate-prewarm
```

Run one suite/gate at a time. Freeze workspace writes during gates; copy receipts
only after exit. The restricted run is diagnostic, and prewarm is branch build
evidence, not independent review or landing authority. No push.
