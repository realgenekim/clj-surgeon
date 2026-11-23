#!/usr/bin/env bash
set -euo pipefail
[[ $# == 2 ]] || { echo 'Usage: run-schedule.sh TASKS.edn SCHEDULE.tsv' >&2; exit 2; }
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
failed=0; total=0
{ read -r header
  while IFS=$'\t' read -r run_id task arm rep; do
    total=$((total+1))
    # one run's refusal or failure is recorded by that run (run.json or failure.json); the schedule goes on,
    # but exits nonzero if any run did, so nothing can look complete with runs missing.
    if ! "$script_dir/run.sh" "$1" "$run_id" "$task" "$arm" "$rep" </dev/null; then
      failed=$((failed+1)); echo "run $run_id failed" >&2
    fi
  done
} < "$2"
echo "schedule: $total runs, $failed failed"
[[ $failed -eq 0 ]]
