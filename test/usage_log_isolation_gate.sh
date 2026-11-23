#!/bin/sh
# The suite must never write the usage log, however it is started (mayor's ruling on PR 62, point 3): run the whole
# babashka suite with a CUSTOM log path in the environment, seeded with one line, and refuse unless that file ends
# byte-identical to its seed. Only the CLI process entry logs (src/clj_surgeon/usage_log.clj); in-process test calls
# are silent by construction and the tests that spawn the CLI pass the off value themselves. This gate is the ratchet.
set -eu
log=$(mktemp "${TMPDIR:-/var/tmp}/clj-surgeon-usage-gate.XXXXXX")
trap 'rm -f "$log"' EXIT INT TERM
printf '%s\n' '{"seed":true}' > "$log"
before=$(cksum < "$log")
status=0
CLJ_SURGEON_USAGE_LOG="$log" bb test/run_all.clj || status=$?
after=$(cksum < "$log" 2>/dev/null || echo missing)
if [ "$before" != "$after" ]; then
  echo "usage-log isolation gate: the suite changed the usage log at $log (seed $before, after $after)" >&2
  exit 97
fi
echo "usage-log isolation gate: the usage log ends byte-identical to its seed"
exit "$status"
