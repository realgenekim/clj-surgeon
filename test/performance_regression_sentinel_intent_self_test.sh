#!/usr/bin/env bash
set -euo pipefail

# @spec MCP-OP-TRACE-006
# Exercise the real audit on copied inputs; never run sentinel workloads.
repo_root=$(cd "$(dirname "$0")/.." && pwd -P)
fixture=$(mktemp -d "${TMPDIR:-/var/tmp}/sentinel-intent-self-test.XXXXXX")
trap 'rm -rf "$fixture"' EXIT
inputs=(
  test/performance_regression_sentinel_intent_test.sh
  docs/intent/performance-regression-sentinel/performance-regression-sentinel-specs.md
  bench/performance_regression_sentinel_test.clj
  bench/performance_regression_sentinel_io_test.clj
  test/performance_regression_sentinel_runner_test.sh
)
for input in "${inputs[@]}"; do
  mkdir -p "$fixture/$(dirname "$input")"
  cp "$repo_root/$input" "$fixture/$input"
done

audit() {
  bash "$fixture/test/performance_regression_sentinel_intent_test.sh"
}

audit > "$fixture/green.log" 2>&1
for id in PERF-SENT-CONFIG-001 PERF-SENT-LEDGER-ROOT-001; do
  for input in "${inputs[@]:2}"; do
    sed "s/$id/REMOVED-WITNESS/g" "$repo_root/$input" > "$fixture/$input"
  done
  if audit > "$fixture/red.log" 2>&1; then
    printf 'FAIL: sentinel audit accepted missing %s\n' "$id" >&2
    exit 1
  fi
  grep -F 'Missing sentinel witnesses:' "$fixture/red.log" > /dev/null
  grep -Fx "  $id" "$fixture/red.log" > /dev/null
done

for input in "${inputs[@]:2}"; do
  cp "$repo_root/$input" "$fixture/$input"
done
printf '\n;; @spec PERF-SENT-UNKNOWN-PART-999\n' >> "$fixture/${inputs[2]}"
if audit > "$fixture/unknown.log" 2>&1; then
  printf 'FAIL: sentinel audit accepted an unknown multipart ID\n' >&2
  exit 1
fi
grep -F 'Unknown sentinel witness identifiers:' "$fixture/unknown.log" > /dev/null
grep -Fx '  PERF-SENT-UNKNOWN-PART-999' "$fixture/unknown.log" > /dev/null
printf 'Sentinel intent audit self-test passed: missing simple/multipart and unknown multipart IDs refused\n'
