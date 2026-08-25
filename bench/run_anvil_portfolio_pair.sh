#!/usr/bin/env bash
set -euo pipefail

result_dir=${1:?usage: run_anvil_portfolio_pair.sh RESULT_DIR TASK ORDER [REPLICATES]}
task=${2:?usage: run_anvil_portfolio_pair.sh RESULT_DIR TASK ORDER [REPLICATES]}
order=${3:?usage: run_anvil_portfolio_pair.sh RESULT_DIR TASK ORDER [REPLICATES]}
replicates=${4:-1}

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

if ! [[ "$task" =~ ^[a-z0-9][a-z0-9-]*$ ]] \
  || [ ! -f "bench/fixtures/edit_portfolio/$task/capsule.edn" ]; then
  echo "TASK must name a frozen edit-portfolio capsule: $task" >&2
  exit 2
fi

if [ "${ANVIL_PAIR_CONFIG_SELF_TEST:-false}" != true ] \
  && { ! git diff --quiet || ! git diff --cached --quiet; }; then
  echo "Anvil portfolio-pair benchmark requires a clean worktree" >&2
  exit 2
fi

benchmark_commit=$(git rev-parse --verify HEAD)
printf 'benchmark_commit\t%s\ntask\t%s\norder\t%s\nreplicates\t%s\n' \
  "$benchmark_commit" "$task" "$order" "$replicates"

if [ "${ANVIL_PAIR_CONFIG_SELF_TEST:-false}" = true ]; then
  echo "Anvil portfolio-pair configuration self-test passed"
  exit 0
fi

export BENCH_MODEL=gpt-5.6-sol
export BENCH_REASONING=high
export BENCH_POST_COMMIT="$benchmark_commit"
export BENCH_RUN_MATRIX="$run_matrix"
export BENCH_TASKS="$task"
export BENCH_INCLUDE_COMPACT=false
export BENCH_REPLICATES="$replicates"
export BENCH_PARALLELISM=1
export BENCH_RETENTION=local
export BENCH_RESULT_DIR="$result_dir"
export BENCH_SANDBOX_MODE=danger-full-access
export BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m'

exec make benchmark-edit-portfolio
