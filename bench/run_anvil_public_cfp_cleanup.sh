#!/usr/bin/env bash
set -euo pipefail

result_dir=${1:?usage: run_anvil_public_cfp_cleanup.sh RESULT_DIR ORDER [REPLICATES]}
order=${2:?usage: run_anvil_public_cfp_cleanup.sh RESULT_DIR ORDER [REPLICATES]}
replicates=${3:-1}

if ! [[ "$replicates" =~ ^[1-9][0-9]*$ ]]; then
  echo "REPLICATES must be a positive integer: $replicates" >&2
  exit 2
fi

case "$order" in
  compact-first)
    run_matrix='mcp:mcp-hint-no-skill native:native-read-hint-no-skill'
    ;;
  native-first)
    run_matrix='native:native-read-hint-no-skill mcp:mcp-hint-no-skill'
    ;;
  *)
    echo "ORDER must be compact-first or native-first: $order" >&2
    exit 2
    ;;
esac

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Public-CFP benchmark requires a clean worktree" >&2
  exit 2
fi

benchmark_commit=$(git rev-parse --verify HEAD)
printf 'benchmark_commit\t%s\norder\t%s\nreplicates\t%s\n' \
  "$benchmark_commit" "$order" "$replicates"

export BENCH_MODEL=gpt-5.6-sol
export BENCH_REASONING=high
export BENCH_POST_COMMIT="$benchmark_commit"
export BENCH_RUN_MATRIX="$run_matrix"
export BENCH_TASKS=public-cfp-extraction-cleanup
export BENCH_INCLUDE_COMPACT=false
export BENCH_REPLICATES="$replicates"
export BENCH_PARALLELISM=1
export BENCH_RETENTION=local
export BENCH_RESULT_DIR="$result_dir"
export BENCH_SANDBOX_MODE=danger-full-access
export BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m'

exec make benchmark-edit-portfolio
