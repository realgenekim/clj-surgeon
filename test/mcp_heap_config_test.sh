#!/bin/sh
set -eu

assert_heap_flags() {
  target=$1
  expected=$2
  output=$(make --no-print-directory -n "$target")

  printf '%s\n' "$output" | grep -Fq -- "$expected"
  printf '%s\n' "$output" | grep -Fq -- '-X:clj-surgeon/mcp'
}

if [ -n "${JAVA_HOME:-}" ]; then
  expected_java="$JAVA_HOME/bin/java"
else
  expected_java=$(command -v java)
fi

assert_heap_flags mcp-serve '-J-Xms64m -J-Xmx2g'
assert_heap_flags mcp-serve-benchmark '-J-Xms64m -J-Xmx2g'
assert_heap_flags mcp-start '-J-Xms64m -J-Xmx2g'

for target in mcp-serve mcp-serve-benchmark mcp-start; do
  output=$(make --no-print-directory -n "$target")
  printf '%s\n' "$output" | grep -Fq -- "JAVA_CMD=\"$expected_java\""
done

override_output=$(make --no-print-directory -n mcp-start \
  MCP_JAVA_HOME='/opt/test-java' \
  MCP_JAVA_OPTS='-J-Xms32m -J-Xmx768m')
printf '%s\n' "$override_output" | grep -Fq -- '-J-Xms32m -J-Xmx768m'
printf '%s\n' "$override_output" | grep -Fq -- 'JAVA_HOME="/opt/test-java"'
printf '%s\n' "$override_output" | grep -Fq -- 'JAVA_CMD="/opt/test-java/bin/java"'

if grep -Eq 'sdkman|[0-9]+\.[0-9]+\.[0-9]+-open' Makefile; then
  echo 'Makefile must not assume a Java distributor, version manager, or version' >&2
  exit 1
fi

echo "MCP heap configuration regression passed"
