#!/usr/bin/env bash
set -u
evidence=docs/observations/2026-09-12-bbtower-block-b/attempt13
run_tmp=$(mktemp -d /var/tmp/forge/bbtower-fx/attempt13-empty.XXXXXX)
printf '%s\n' "$run_tmp" > "$evidence/focused-root.txt"
LC_ALL=C ls -la /tmp > "$evidence/tmp-before-ls-la.txt"
LC_ALL=C ls -1A /tmp > "$evidence/tmp-before.txt"
env TMPDIR="$run_tmp" JAVA_TOOL_OPTIONS="-Xmx1g -Djava.io.tmpdir=$run_tmp" \
  clojure -J-Xmx1g -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner \
  --ns clj-surgeon.cli-dispatch-test clj-surgeon.mission-typist-executor-test \
  clj-surgeon.mcp-alias-migration-test > "$evidence/three-namespaces.log" 2>&1
result=$?
printf '%s\n' "$result" > "$evidence/three-namespaces.exit"
LC_ALL=C ls -la /tmp > "$evidence/tmp-after-ls-la.txt"
LC_ALL=C ls -1A /tmp > "$evidence/tmp-after.txt"
comm -13 "$evidence/tmp-before.txt" "$evidence/tmp-after.txt" > "$evidence/tmp-new.txt"
LC_ALL=C ls -la "$run_tmp" > "$evidence/focused-root-after.txt"
rmdir "$run_tmp"
exit "$result"
