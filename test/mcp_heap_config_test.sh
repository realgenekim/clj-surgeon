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

# @spec MCP-OP-TMPHYG-010
lifecycle_state_dir="${TMPDIR:-/var/tmp}/clj-surgeon-mcp-lifecycle-test"
start_output=$(make --no-print-directory -n mcp-start \
  MCP_STATE_DIR="$lifecycle_state_dir" \
  MCP_STOP_ATTEMPTS=7)
printf '%s\n' "$start_output" | grep -Fq -- \
  "test -f \"$lifecycle_state_dir/ready.edn\""
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
# `grep -Fqv` succeeds when ANY line lacks the string, which is always true of
# a multi-line recipe: the assertion below is the one that actually holds the
# overridden recipe to carrying no default port.
if printf '%s\n' "$override_serve_output" | grep -Fq -- ":port '7888'"; then
  echo 'mcp-serve still carries the default port when MCP_PORT is overridden' >&2
  exit 1
fi

# Regression: `make mcp-serve-benchmark` carried the same defect mcp-serve
# did — no `:port` at all, so it always bound the 7888 default regardless of
# MCP_PORT and failed "Address already in use" against any clj-surgeon MCP
# already holding that port. Benchmarks are exactly the workload that runs
# beside an existing server.
default_benchmark_output=$(make --no-print-directory -n mcp-serve-benchmark)
printf '%s\n' "$default_benchmark_output" | grep -Fq -- ":port '7888'"

override_benchmark_output=$(make --no-print-directory -n mcp-serve-benchmark MCP_PORT=7902)
printf '%s\n' "$override_benchmark_output" | grep -Fq -- ":port '7902'"
if printf '%s\n' "$override_benchmark_output" | grep -Fq -- ":port '7888'"; then
  echo 'mcp-serve-benchmark still carries the default port when MCP_PORT is overridden' >&2
  exit 1
fi

url_output=$(make --no-print-directory -n mcp-start MCP_PORT=7901 2>/dev/null || true)
if [ -n "$url_output" ]; then
  printf '%s\n' "$url_output" | grep -Fq -- 'http://127.0.0.1:7901/mcp'
fi

# EXECUTION, not printed recipe text. Every assertion above reads `make -n`
# output; that is exactly how the suite came to run at the box default heap
# (7.8 GB) for a whole day while this gate stayed green — the test runner
# re-execs itself into a child JVM, and round one of the temp-dir hygiene
# ratchet rebuilt that child without the parent's flags. Spawn the real
# mechanism and read the child's actual Runtime/maxMemory.
# @spec MCP-OP-TMPHYG-006
heap_cp=$(clojure -Spath -A:clj-surgeon/mcp-test)
heap_out=$(java -Xmx317m -cp "$heap_cp" clojure.main -m clj-surgeon.tmp-leak-probe 2>&1)
heap_parent=$(printf '%s\n' "$heap_out" | sed -n 's/^PROBE role=parent max-mb=\([0-9]*\) .*$/\1/p' | head -1)
heap_child=$(printf '%s\n' "$heap_out" | sed -n 's/^PROBE role=child max-mb=\([0-9]*\) .*$/\1/p' | head -1)
if [ -z "$heap_parent" ] || [ -z "$heap_child" ]; then
  printf '%s\n' "$heap_out" >&2
  echo 'could not read the probe heap ceilings' >&2
  exit 1
fi
if [ "$heap_parent" -ge 1000 ]; then
  echo "the probe parent ignored -Xmx317m (max-mb=$heap_parent)" >&2
  exit 1
fi
if [ "$heap_child" != "$heap_parent" ]; then
  echo "the test-runner re-exec did not preserve the heap ceiling: parent=$heap_parent MB child=$heap_child MB" >&2
  exit 1
fi

echo "MCP heap configuration regression passed"
