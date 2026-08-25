#!/usr/bin/env bash
set -euo pipefail

result_dir=${1:?usage: run_anvil_compiled_edit_canary.sh RESULT_DIR [REPLICATES]}
replicates=${2:-1}

if ! [[ "$replicates" =~ ^[1-9][0-9]*$ ]]; then
  echo "REPLICATES must be a positive integer: $replicates" >&2
  exit 2
fi

export BENCH_MODEL=gpt-5.6-sol
export BENCH_REASONING=high
export BENCH_POST_COMMIT=WORKTREE
export BENCH_RUN_MATRIX="${BENCH_RUN_MATRIX:-mcp:native-computed-hint-no-skill mcp:edit-computed-hint-no-skill mcp:mcp-transform-hint-no-skill}"
export BENCH_TASKS=computed-edit
export BENCH_INCLUDE_COMPACT=false
export BENCH_REPLICATES="$replicates"
export BENCH_PARALLELISM=1
export BENCH_RETENTION=local
export BENCH_RESULT_DIR="$result_dir"
export BENCH_SANDBOX_MODE=danger-full-access
export BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m'

exec make benchmark-edit-portfolio
