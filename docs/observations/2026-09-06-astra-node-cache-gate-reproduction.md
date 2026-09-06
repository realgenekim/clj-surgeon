# Node compile-cache leak: bounded diagnosis

The failed full battery remains failed:682 tests/13209 assertions were green,
but its final temp ratchet found node-compile-cache. Original log unchanged at
/var/tmp/forge/astra-telemetry-fx/merged-battery.log. Its temporary root had already
been removed before investigation, so no original creating PID or exact test
attribution is claimed.

The actual default formatter command is npx @chrisoakman/standard-clojure-style
fix {files} (Surgeon inspected mcp_formatter/default-command and format-candidates!).
Installed /usr/bin/npx loads /usr/lib/node_modules/npm/lib/cli.js, which immediately
calls node:module.enableCompileCache() without a directory. Under Nodev22.23.2 that
creates node-compile-cache inside TMPDIR.

Faithful reproduction used this same cached formatter entrance, --offline
--no-install, private TMPDIR, disabled update notifier/audit, isolated events, and
strace of execve/mkdir/mkdirat/openat. Both runs exited0 and produced identical
formatted source with standard-clj0.29.0. RED with NODE_DISABLE_COMPILE_CACHE unset
left exactly node-compile-cache. The trace binds its mkdir to PID3786773, the Node
process executing npx. GREEN with NODE_DISABLE_COMPILE_CACHE=1 left no temp entries.
No package installation, provider/model calls, JVM or battery rerun occurred.
Raw argv/env/outputs and syscall traces remain beside this report.

## Recommended minimal fix

Prefix only the quality-test invocation with NODE_DISABLE_COMPILE_CACHE=1. Keep
the existing suite temp ratchet exactly as is, retain its failed receipt, and
use a fresh log/events path for the authorized rerun. Do not globally change the
host environment or ignore leaked directory names. No source edit is necessary.

Parent should rerun its same full battery command/affinity/JVM options with this
one additional environment binding, e.g. from its frozen reviewed worktree:

    NODE_DISABLE_COMPILE_CACHE=1 CLJ_SURGEON_EVENTS_FILE=/var/tmp/forge/astra-telemetry-fx/merged-battery-nodecache-off-events.jsonl TMPDIR=/var/tmp/forge SLOT_OWNER=astra /home/forge/bin/suite-run make test-battery

Preserve the original invocation's CPU/JVM parameters in the actual command;
the example does not authorize changes to them. The lead owns the exact rerun.
A future durable Makefile default would be an LID-scoped edit; none is made or
needed for this bounded command-level repair. This is a correctness-gate setup
fix, not a benchmark timing/environment amendment.
