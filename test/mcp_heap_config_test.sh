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

assert_heap_flags mcp-serve '-J-Xms64m -J-Xmx512m'
assert_heap_flags mcp-serve-benchmark '-J-Xms64m -J-Xmx512m'
assert_heap_flags mcp-start '-J-Xms64m -J-Xmx512m'

start_output=$(make --no-print-directory -n mcp-start \
  MCP_STATE_DIR='/tmp/clj-surgeon-mcp-lifecycle-test' \
  MCP_STOP_ATTEMPTS=7)
printf '%s\n' "$start_output" | grep -Fq -- \
  'test -f "/tmp/clj-surgeon-mcp-lifecycle-test/ready.edn"'
printf '%s\n' "$start_output" | grep -Fq -- 'seq 1 7'
printf '%s\n' "$start_output" | grep -Fq -- 'refusing a competing launch'

test_output=$(make --no-print-directory -n mcp-test)
printf '%s\n' "$test_output" | grep -Fq -- '-J-Xms64m -J-Xmx512m'
printf '%s\n' "$test_output" | grep -Fq -- '-M:clj-surgeon/mcp-test'

nrepl_output=$(make --no-print-directory -n nrepl)
printf '%s\n' "$nrepl_output" | grep -Fq -- '-J-Xms64m -J-Xmx512m'
printf '%s\n' "$nrepl_output" | grep -Fq -- \
  '-M:clj-surgeon/mcp-test:clj-surgeon/nrepl'

reload_output=$(make --no-print-directory -n mcp-reload)
printf '%s\n' "$reload_output" | grep -Fq -- \
  'clj-surgeon.mcp-process clj-surgeon.forward-refs clj-surgeon.fix-declares clj-surgeon.binding-rename'

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

# Regression: `make mcp-serve` used to omit `:port`, so it always bound the
# default 7888 regardless of MCP_PORT and failed "Address already in use"
# whenever another clj-surgeon MCP (or another seat's) already held that
# port. The recipe must pass MCP_PORT through explicitly, and MCP_URL
# (derived from MCP_PORT) must stay consistent with the port actually
# requested; the default must stay 7888 when MCP_PORT is not overridden.
default_serve_output=$(make --no-print-directory -n mcp-serve)
printf '%s\n' "$default_serve_output" | grep -Fq -- ":port '7888'"

override_serve_output=$(make --no-print-directory -n mcp-serve MCP_PORT=7901)
printf '%s\n' "$override_serve_output" | grep -Fq -- ":port '7901'"
printf '%s\n' "$override_serve_output" | grep -Fqv -- ":port '7888'"

url_output=$(make --no-print-directory -n mcp-start MCP_PORT=7901 2>/dev/null || true)
if [ -n "$url_output" ]; then
  printf '%s\n' "$url_output" | grep -Fq -- 'http://127.0.0.1:7901/mcp'
fi

echo "MCP heap configuration regression passed"
