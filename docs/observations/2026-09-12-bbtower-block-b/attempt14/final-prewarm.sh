#!/usr/bin/env bash
set -u
# No writes in the repository until this process has completed. In particular,
# do not edit observations while namespace purity snapshots are active.
evidence=docs/observations/2026-09-12-bbtower-block-b/attempt14
capture=/var/tmp/forge/bbtower-fx/attempt14-final-prewarm.log
env TMPDIR=/var/tmp/forge/bbtower-fx JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx' \
  make landing-gate-prewarm > "$capture" 2>&1
result=$?
cp "$capture" "$evidence/gate.md"
printf '%s\n' "$result" > "$evidence/gate.exit"
cmp "$capture" "$evidence/gate.md"
exit "$result"
