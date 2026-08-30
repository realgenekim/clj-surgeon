#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd -P)
spec_file="$repo_root/docs/intent/performance-regression-sentinel/performance-regression-sentinel-specs.md"
test_files=(
  "$repo_root/bench/performance_regression_sentinel_test.clj"
  "$repo_root/bench/performance_regression_sentinel_io_test.clj"
  "$repo_root/test/performance_regression_sentinel_runner_test.sh"
)

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

tmp_root=$(mktemp -d "${TMPDIR:-/tmp}/clj-surgeon-sentinel-intent.XXXXXX")
trap 'rm -rf "$tmp_root"' EXIT

for test_file in "${test_files[@]}"; do
  test -f "$test_file" || fail "missing red witness file ${test_file#$repo_root/}"
done

sed -nE 's/^- \[ \] \*\*(PERF-SENT-[A-Z]+-[0-9]{3})\*\*:.*/\1/p' \
  "$spec_file" | LC_ALL=C sort -u > "$tmp_root/requirements.txt"

grep -hE '@spec PERF-SENT-' "${test_files[@]}" \
  | grep -Eo 'PERF-SENT-[A-Z]+-[0-9]{3}' \
  | LC_ALL=C sort -u > "$tmp_root/witnesses.txt"

test -s "$tmp_root/requirements.txt" || fail "no sentinel requirements decoded"

comm -23 "$tmp_root/requirements.txt" "$tmp_root/witnesses.txt" \
  > "$tmp_root/missing.txt"
comm -13 "$tmp_root/requirements.txt" "$tmp_root/witnesses.txt" \
  > "$tmp_root/unknown.txt"

if test -s "$tmp_root/missing.txt"; then
  printf 'Missing sentinel witnesses:\n' >&2
  sed 's/^/  /' "$tmp_root/missing.txt" >&2
  exit 1
fi

if test -s "$tmp_root/unknown.txt"; then
  printf 'Unknown sentinel witness identifiers:\n' >&2
  sed 's/^/  /' "$tmp_root/unknown.txt" >&2
  exit 1
fi

requirement_count=$(wc -l < "$tmp_root/requirements.txt" | tr -d ' ')
printf 'Sentinel intent witness audit passed: %s/%s requirements named\n' \
  "$requirement_count" "$requirement_count"
