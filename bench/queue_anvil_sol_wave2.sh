#!/usr/bin/env bash
set -euo pipefail

wave1_root() {
  case "$1" in
    dev-a) printf '%s\n' /srv/fleet/dev-a/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-cli/results ;;
    dev-b) printf '%s\n' /srv/fleet/dev-b/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-cli-mcp/results ;;
    dev-c) printf '%s\n' /srv/fleet/dev-c/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-none/results ;;
    *) return 2 ;;
  esac
}

for user in dev-a dev-b dev-c; do
  root=$(wave1_root "$user")
  while [ -f "$root/.benchmark-owner/owner.tsv" ]; do
    sleep 15
  done
  receipt_count=$(find "$root" -mindepth 2 -maxdepth 2 -name terminal.tsv -type f | wc -l)
  if [ "$receipt_count" -ne 8 ]; then
    echo "refusing wave 2: $user wave 1 has $receipt_count/8 terminal receipts" >&2
    exit 3
  fi
done

launch() {
  local user=$1 arm=$2
  local repo="/srv/fleet/$user/clj-surgeon-study-20260823-sol"
  local output="/srv/fleet/$user/clj-surgeon-study-results/2026-08-23-sol-high-hone4-wave2-r2-$arm"
  test ! -e "$output"
  sudo -iu "$user" mkdir -p "$output"
  sudo -iu "$user" bash -c \
    "cd '$repo' && BENCH_REPLICATES=2 bash bench/run_anvil_calibration.sh '$arm' '$output/results' hone4 >'$output/supervisor.log' 2>&1" &
  printf '%s\t%s\t%s\t%s\n' "$user" "$arm" "$!" "$output"
}

# Rotate every arm away from its wave-1 seat.
launch dev-a none
pid_a=$!
launch dev-b cli
pid_b=$!
launch dev-c cli-mcp
pid_c=$!

status=0
wait "$pid_a" || status=1
wait "$pid_b" || status=1
wait "$pid_c" || status=1
printf 'wave2_terminal_status\t%s\n' "$status"
exit "$status"
