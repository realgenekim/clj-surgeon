#!/usr/bin/env bash
set -euo pipefail

arm=${1:?usage: run_anvil_calibration.sh ARM RESULT_DIR [core4|full8]}
result_dir=${2:?usage: run_anvil_calibration.sh ARM RESULT_DIR [core4|full8]}
task_set=${3:-core4}

case "$arm" in
  cli)
    run_matrix=post:matched-skill
    ;;
  cli-mcp)
    run_matrix=mcp:mcp-exploratory-rule-no-skill
    ;;
  none)
    run_matrix=native:no-skill
    ;;
  *)
    echo "unknown calibration arm: $arm" >&2
    exit 2
    ;;
esac

case "$task_set" in
  core4)
    default_tasks='decision-batch-edit pair-view-expect-edit dependency-move-edit exploratory-shell-edit'
    ;;
  full8)
    default_tasks='decision-batch-edit pair-view-expect-edit dependency-move-edit exploratory-shell-edit exact-nested-edit literal-source-edit native-text-edit three-site-delete-edit-delete'
    ;;
  hone4)
    default_tasks='decision-batch-edit pair-view-expect-edit exact-nested-edit exploratory-shell-edit'
    ;;
  *)
    echo "unknown calibration task set: $task_set" >&2
    exit 2
    ;;
esac

export BENCH_MODEL=gpt-5.6-sol
export BENCH_REASONING=high
export BENCH_POST_COMMIT=WORKTREE
export BENCH_RUN_MATRIX="$run_matrix"
export BENCH_TASKS="${BENCH_TASKS:-$default_tasks}"
export BENCH_INCLUDE_COMPACT=false
export BENCH_REPLICATES="${BENCH_REPLICATES:-1}"
export BENCH_PARALLELISM="${BENCH_PARALLELISM:-1}"
export BENCH_RETENTION=local
export BENCH_RESULT_DIR="$result_dir"
export BENCH_SANDBOX_MODE="${BENCH_SANDBOX_MODE:-danger-full-access}"

exec make benchmark-edit-portfolio
