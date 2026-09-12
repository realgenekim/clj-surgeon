# Attempt24 commands and subjects

Started with `git checkout bb-rewrite-tower-local`. The initial HEAD was
3b6d53577be3173cce209934e37bac904b3d38d7. The pending battery row and relocated
prewarm-checkonly directory became c40cb1ac. Sol's exact red test became
92fe4252. The parser/receipt repair became 5674f516. The fast-suite census/sleep
registration repair became 58b33e65.
The restricted diagnostic exposed the old help receipt oracle; its literal
closure-expected repair became c53fd9c5, the real prewarm's source subject.

JVM environment: TMPDIR=/var/tmp/forge/bbtower-fx,
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'; unset
_JAVA_OPTIONS. `make nrepl` supplied its own explicit -J-Xmx512m. Discovery
found no project-local server; the owned server started on 36757. The first
evaluation asserted user.dir was /home/forge/src/clj-surgeon-bbtower. No protected
port, shared service or shared installation was touched. System/exit stopped
the owned JVM before the full suites.

Focused runs used clj-nrepl-eval --port 36757. Changed namespaces were required
with :reload before their tests. Logs retain the actual summaries:

- red.log: exact probe-reloads-prefix-list-dependency witness on the old code.
- focused-first/second/third.log: incremental diagnostics and four new witnesses.
- focused-all.log: complete mcp-hot-verify-test, mcp-http-server-test,
  admit-patch-test, splice-envelope-test, receipt-booleans-test.
- sol-nine.log: exact nine Vars from Sol's round-four fence command (below).
- refusal-census.log: the-refusal-enumeration-is-pinned-in-count-and-in-membership.
- real-probe-receipt.log: image-identity on the real worktree followed by probe!
  for clj-surgeon.forms-test; receipt includes completed names and expected count.

Sol's nine Vars:

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

Formatting: `npx --offline @chrisoakman/standard-clojure-style fix` on changed
Clojure files; lint: ~/bin/clj-kondo --lint on those files. Unrelated formatter
hunks in the large alias test were reverted by native patch.

After the single fast run identified census/line-pin drift, the declared
CENSUS_REGENERATE=1 clojure -J-Xmx1g -M:clj-surgeon/test-deps entrance ran the
corpus census witness and the sleep-pin witness with explicit report counters
and nonzero exit on failure. Four additions, no removals.

Full checks use `bash attempt24/run_gate.sh LABEL COMMAND...`, which writes
clock-derived start, HEAD, exact argv, exit and wrapper wall into checks/:

```
fast make test-fast
restricted python3 docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py -- make landing-gate-prewarm
prewarm make landing-gate-prewarm
prewarm-final make landing-gate-prewarm
```

The restricted run is explicitly diagnostic. The real prewarm is the branch's
landing evidence. Every entrance runs sequentially; no outer timeout, budget
override, test skip or runtime reclassification is used. No push is performed.
The first real prewarm failed purity because the observer wrote evidence files
mid-run. The final allowed prewarm freezes all workspace writes until exit;
this is an operational repair, with no source or isolation-rule change.
