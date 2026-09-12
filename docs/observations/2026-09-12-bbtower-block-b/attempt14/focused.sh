#!/usr/bin/env bash
set -u
evidence=docs/observations/2026-09-12-bbtower-block-b/attempt14
run_root=$(mktemp -d /var/tmp/forge/bbtower-fx/attempt14-focused.XXXXXX)
run_tmp="$run_root/tmp"
scratch_home="$run_root/home"
mkdir "$run_tmp" "$scratch_home"
printf '%s\n' "$run_root" > "$evidence/focused-root.txt"
# Resolve dependencies before the HOME audit; the product runs via java.
env TMPDIR=/var/tmp/forge/bbtower-fx JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx' \
  clojure -J-Xmx1g -Spath -M:clj-surgeon/test-deps > "$evidence/focused-classpath.txt"
test_classpath=$(cat "$evidence/focused-classpath.txt")
LC_ALL=C find "$scratch_home" -printf '%P %y\n' | sort > "$evidence/home-before.txt"
LC_ALL=C ls -la "$run_tmp" > "$evidence/tmp-before.txt"
env HOME="$scratch_home" TMPDIR="$run_tmp" JAVA_TOOL_OPTIONS="-Xmx1g -Djava.io.tmpdir=$run_tmp" \
  java -Xmx1g -cp "$test_classpath" clojure.main -m clj-surgeon.mcp-test-runner \
  --ns clj-surgeon.receipt-artifacts-boundary-test > "$evidence/namespace.log" 2>&1
result=$?
printf '%s\n' "$result" > "$evidence/namespace.exit"
LC_ALL=C find "$scratch_home" -printf '%P %y\n' | sort > "$evidence/home-after.txt"
diff -u "$evidence/home-before.txt" "$evidence/home-after.txt" > "$evidence/home-diff.txt"
home_result=$?
LC_ALL=C ls -la "$run_tmp" > "$evidence/tmp-after.txt"
if [ "$result" -eq 0 ] && [ "$home_result" -eq 0 ]; then
  rmdir "$scratch_home" "$run_tmp" "$run_root"
else
  exit 1
fi
