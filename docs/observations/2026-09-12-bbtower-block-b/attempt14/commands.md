# Reproduction and provenance

Start: `git checkout bb-rewrite-tower-local`, observed c2ec3039a5b4382d2353e9c9f2b2f1b7e0db3dd1.
The existing prewarm-checkonly directory was moved without editing its bytes and
committed as 5197d751. Its report points to packet 53079781's original log.

All JVM checks use an explicit heap; environment is
`JAVA_TOOL_OPTIONS=-Xmx1g -Djava.io.tmpdir=<run TMPDIR>`. TMPDIR is beneath
`/var/tmp/forge/bbtower-fx`. No `_JAVA_OPTIONS` was assigned.

Red: create `attempt14-red/cache` (empty; mode 0555) and `attempt14-red/tmp`
beneath the allowed temp parent. Set TMPDIR to the latter and
`npm_config_cache` to the former, then run
`clojure -J-Xmx1g -M:clj-surgeon/test-deps attempt14/formatter-witness.clj`
(with the full repository observation path). `red.log` records native EACCES,
exit 1. The first BB invocation lacked the nrepl dependency; a subsequent
witness form had a missing closer. Both setup logs are retained separately.

Green: same roots, with both inherited `npm_config_cache` and
`npm_config_logs_dir` pointing to the read-only cache, run
`clojure -J-Xmx1g -M:clj-surgeon/test-deps -i <attempt14>/formatter-witness.clj <attempt14>/fallback-witness.clj`.
`green.log` contains the resolved then fallback receipts; `green.exit` is the
combined exit status. The refined multiline fixture checks observable indentation;
the initial inline-spacing expectation failed because standard-clj preserves it.
`fallback-syntax-failure.exit` belongs to the retained initial syntax failure, not the
final successful combined run. `npm-paths.txt` shows the read-only inherited
cache and the actual cache/log directories beneath selected TMPDIR.

Focused unit check: `make nrepl`, port 37067, verified user.dir equals this
worktree, then reload and run mcp-process-test and mcp-formatter-test through
clj-nrepl-eval. Result in focused-green.log. The owned nREPL was stopped with
System/exit before the fast suite. No shared port was touched.

The fresh namespace check is exactly `bash <attempt14>/focused.sh`. Dependencies
are resolved before the audit, then direct java runs the repository namespace
runner with scratch HOME. JVM user.home remains its real account value, as in
the product; the HOME audit does not claim filesystem confinement. Home endpoint
listings must match; owned empty directories are removed on success.

Milestones, sequentially under the standard environment above:
`make test-fast` once, then `make landing-gate-prewarm`.
Fast redirects complete stdout/stderr to test-fast.log. First prewarm output is
retained verbatim as gate-first.md. The final prewarm captures outside the tree
at /var/tmp/forge/bbtower-fx/attempt14-final-prewarm.log; only after it exits is
that log copied verbatim to gate.md. Each shell exit has a matching .exit file.
No outer timeout is used. No repository edits occur during the final prewarm.

The first prewarm had zero assertion failures/errors and passing budgets, but
four TEST-ISO-003 violations named DOGFOOD.md, which this agent edited during
the run. The one repair is execution discipline: freeze all repository writes,
capture outside the worktree, then copy evidence back after process completion.
No oracle, budget, membership, or product source is changed for that repair.

Formatting: installed `standard-clj check` on the six changed Clojure source/test
files (format.log). Lint: `~/bin/clj-kondo --lint` on those same files (lint.log).
`git diff --check` also passes. The new test is registered in deftest_census.edn.
