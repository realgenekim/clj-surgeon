#!/usr/bin/env bash
# TEST-ISO race witnesses, RUN UNDER CONTENTION.
#
# Round one's two load-fragile namespaces (mcp-prepared-wire-test's teardown
# race and mcp-process-test's wall-clock-calibrated deadlines) both PASS on a
# quiet box. An example test on a quiet box is not a reproduction of a race;
# it is a measurement of how idle the machine was. This script makes the box
# not idle: N copies of the same lane from N git-archive copies of the tip,
# plus a CPU-burner sidecar sized to the box.
#
# Usage: contention_witness.sh <N> <burners> <clojure-args...>
#   e.g. contention_witness.sh 2 12 -M:clj-surgeon/test-battery
#
# Fixtures live under /var/tmp/forge/suite-spike-fx (never /tmp -- RAM).
set -uo pipefail
N="${1:?N}"; BURNERS="${2:?burners}"; shift 2
FX=/var/tmp/forge/suite-spike-fx/contention
REPO="$(git rev-parse --show-toplevel)"
SHA="$(git -C "$REPO" rev-parse --short HEAD)"
export TMPDIR=/var/tmp/forge

rm -rf "$FX"; mkdir -p "$FX"
for i in $(seq 1 "$N"); do
  mkdir -p "$FX/c$i"
  git -C "$REPO" archive HEAD | tar -x -C "$FX/c$i"
done

echo "contention: N=$N burners=$BURNERS sha=$SHA"
echo "contention: load BEFORE $(uptime | sed 's/.*load average: //')"

BURN_PIDS=()
for _ in $(seq 1 "$BURNERS"); do
  ( while :; do :; done ) & BURN_PIDS+=($!)
done
trap 'kill "${BURN_PIDS[@]}" 2>/dev/null' EXIT

START=$(date +%s)
PIDS=()
for i in $(seq 1 "$N"); do
  ( cd "$FX/c$i" && timeout 1800 clojure "$@" > "$FX/c$i.log" 2>&1; echo $? > "$FX/c$i.exit" ) &
  PIDS+=($!)
done
wait "${PIDS[@]}"
END=$(date +%s)

kill "${BURN_PIDS[@]}" 2>/dev/null; trap - EXIT
echo "contention: load AFTER  $(uptime | sed 's/.*load average: //')"
echo "contention: wall $((END-START)) s"
FAILED=0
for i in $(seq 1 "$N"); do
  echo "--- copy $i (exit $(cat "$FX/c$i.exit")) ---"
  grep -E "^(FAIL|ERROR) in|^Ran |failures,|temp-leak:|lane-refused:" "$FX/c$i.log" | tail -20
  [ "$(cat "$FX/c$i.exit")" = "0" ] || FAILED=1
done
echo "contention: VERDICT $([ $FAILED = 0 ] && echo ALL-GREEN || echo RED)"
exit $FAILED
