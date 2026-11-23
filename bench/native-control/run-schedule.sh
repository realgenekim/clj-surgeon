#!/usr/bin/env bash
set -euo pipefail
[[ $# == 2 ]] || { echo 'Usage: run-schedule.sh TASKS.edn SCHEDULE.tsv' >&2; exit 2; }
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
{ read -r header
  while IFS=$'\t' read -r run_id task arm rep; do
    "$script_dir/run.sh" "$1" "$run_id" "$task" "$arm" "$rep" </dev/null
  done
} < "$2"
