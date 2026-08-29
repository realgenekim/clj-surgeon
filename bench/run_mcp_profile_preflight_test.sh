#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
test_root=$(mktemp -d /tmp/clj-surgeon-mcp-profile-preflight.XXXXXX)
trap 'rm -rf "$test_root"' EXIT

commit=$(git -C "$repo_root" rev-parse HEAD)

for profile in full edit; do
  result_dir="$test_root/$profile"
  (
    cd "$repo_root"
    BENCH_PRE_COMMIT="$commit" \
    BENCH_POST_COMMIT="$commit" \
    BENCH_RUN_MATRIX='mcp:no-skill' \
    BENCH_TASKS='submission-row-extraction-cleanup' \
    BENCH_INCLUDE_COMPACT=false \
    BENCH_REPLICATES=1 \
    BENCH_PARALLELISM=1 \
    BENCH_RETENTION=local \
    BENCH_RESULT_DIR="$result_dir" \
    BENCH_MCP_TOOL_PROFILE="$profile" \
    BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m' \
    BENCH_MCP_REGISTRY_PREFLIGHT_ONLY=true \
    bash bench/run_clean_codex.sh
  ) >"$test_root/$profile.stdout" 2>"$test_root/$profile.stderr"

  receipt=$(find "$result_dir" -name codex-mcp-registry.json -type f -print -quit)
  test -n "$receipt"
  actual=$(jq -c '."tool-names" | sort' "$receipt")
  case "$profile" in
    full)
      expected='["apply_clojure_changes","edit_clojure","inspect_clojure","transform_clojure"]'
      ;;
    edit)
      expected='["edit_clojure"]'
      ;;
  esac
  if [ "$actual" != "$expected" ]; then
    printf 'Profile %s client projection mismatch\nexpected=%s\nactual=%s\n' \
      "$profile" "$expected" "$actual" >&2
    exit 1
  fi
  run_dir=$(dirname "$receipt")
  test "$(awk -F '\t' '$1 == "state" {print $2}' \
    "$run_dir/terminal.tsv")" = completed
  test -f "$run_dir/codex-client-surface-preflight.txt"
  test ! -e "$run_dir/prompt.txt"
  test ! -e "$run_dir/events.jsonl"
done

printf '%s\n' 'MCP full/edit client-profile preflight passed'
