#!/bin/sh
# The suite must never write the usage log, however it is started (mayor's ruling on PR 62, point 3): run the whole
# babashka suite with a CUSTOM log path in the environment, seeded with one line, and refuse if that file changed.
# Only the CLI process entry logs (src/clj_surgeon/usage_log.clj); in-process test calls are silent by construction and
# the tests that spawn the CLI pass the off value themselves. This gate is the ratchet that keeps it so.
set -eu
log="${TMPDIR:-/var/tmp}/clj-surgeon-usage-gate-$$.jsonl"
printf '%s\n' '{"seed":true}' > "$log"
before=$(cksum < "$log")
status=0
CLJ_SURGEON_USAGE_LOG="$log" bb test/run_all.clj || status=$?
after=$(cksum < "$log")
rm -f "$log"
if [ "$before" != "$after" ]; then
  echo "usage-log isolation gate: the suite wrote the usage log at $log" >&2
  exit 97
fi
echo "usage-log isolation gate: the suite left the usage log byte-identical"
exit "$status"
