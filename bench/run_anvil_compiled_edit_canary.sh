#!/usr/bin/env bash
set -euo pipefail

result_dir=${1:?usage: run_anvil_compiled_edit_canary.sh RESULT_DIR LATIN_ROW [REPLICATES]}
latin_row=${2:?usage: run_anvil_compiled_edit_canary.sh RESULT_DIR LATIN_ROW [REPLICATES]}
replicates=${3:-1}

if ! [[ "$replicates" =~ ^[1-9][0-9]*$ ]]; then
  echo "REPLICATES must be a positive integer: $replicates" >&2
  exit 2
fi

case "$latin_row" in
  1)
    run_matrix='mcp:native-computed-hint-no-skill mcp:edit-computed-hint-no-skill mcp:mcp-transform-hint-no-skill'
    ;;
  2)
    run_matrix='mcp:edit-computed-hint-no-skill mcp:mcp-transform-hint-no-skill mcp:native-computed-hint-no-skill'
    ;;
  3)
    run_matrix='mcp:mcp-transform-hint-no-skill mcp:native-computed-hint-no-skill mcp:edit-computed-hint-no-skill'
    ;;
  *)
    echo "LATIN_ROW must be 1, 2, or 3: $latin_row" >&2
    exit 2
    ;;
esac

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Compiled-edit benchmark requires a clean worktree" >&2
  exit 2
fi
benchmark_commit=$(git rev-parse --verify HEAD)
printf 'benchmark_commit\t%s\nlatin_row\t%s\n' "$benchmark_commit" "$latin_row"

export BENCH_MODEL=gpt-5.6-sol
export BENCH_REASONING=high
export BENCH_POST_COMMIT="$benchmark_commit"
export BENCH_RUN_MATRIX="$run_matrix"
export BENCH_TASKS=computed-edit
export BENCH_INCLUDE_COMPACT=false
export BENCH_REPLICATES="$replicates"
export BENCH_PARALLELISM=1
export BENCH_RETENTION=local
export BENCH_RESULT_DIR="$result_dir"
export BENCH_SANDBOX_MODE=danger-full-access
export BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m'

exec make benchmark-edit-portfolio
