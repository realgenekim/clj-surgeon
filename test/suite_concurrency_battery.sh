#!/usr/bin/env bash
# TEST-ISO-009 -- the concurrency battery, and the spike's merge gate.
#
# N REAL `git clone`s of the tip, each running `make mcp-test` (the fast +
# integration merge-gate lane) CONCURRENTLY. Passes only if ALL N report zero
# failures and zero errors and exit 0. One green copy next to one red copy is
# a FAILURE, not a 50% pass: round one's interference hunt found exactly that
# shape twice -- in both pairs one member failed and the other was clean --
# and a gate that averaged them would have called a scheduler race healthy.
#
# Clones, not archive copies, on purpose: the lane must survive a real .git
# directory, real per-clone `.cpcache`, and the seat-shared `~/.m2` and
# `~/.gitlibs` that a real checkout resolves against.
#
# Usage: make suite-concurrency-battery N=4
#   SUITE_BATTERY_TARGET   make target to run in each clone (default mcp-test)
#   SUITE_BATTERY_MAX_LOAD load ceiling before starting     (default 10)
#   SUITE_BATTERY_WAITS    60 s waits for the load to fall  (default 10)
set -uo pipefail

N="${N:-4}"
TARGET="${SUITE_BATTERY_TARGET:-mcp-test}"
MAX_LOAD="${SUITE_BATTERY_MAX_LOAD:-10}"
MAX_WAITS="${SUITE_BATTERY_WAITS:-10}"
FX="${SUITE_BATTERY_FX:-/var/tmp/forge/suite-battery-fx}"

REPO="$(git rev-parse --show-toplevel)"
SHA="$(git -C "$REPO" rev-parse HEAD)"
export TMPDIR="${TMPDIR:-/var/tmp/forge}"

case "$TMPDIR" in
  /tmp|/tmp/*|/dev/shm|/dev/shm/*)
    echo "suite-battery: REFUSED -- TMPDIR=$TMPDIR is RAM-backed" >&2; exit 97;;
esac

load1 () { awk '{print $1}' /proc/loadavg; }

# The box carries other seats. Starting a 4-wide JVM battery on top of a
# already-loaded machine measures the neighbours, not the lane.
waits=0
while [ "$(awk -v a="$(load1)" -v b="$MAX_LOAD" 'BEGIN{print (a>b)}')" = "1" ] \
      && [ "$waits" -lt "$MAX_WAITS" ]; do
  echo "suite-battery: load $(load1) > $MAX_LOAD, waiting 60 s ($((waits+1))/$MAX_WAITS)"
  sleep 60; waits=$((waits+1))
done
START_LOAD="$(cat /proc/loadavg)"
if [ "$(awk -v a="$(load1)" -v b="$MAX_LOAD" 'BEGIN{print (a>b)}')" = "1" ]; then
  echo "suite-battery: WARNING -- starting at load $(load1), above the $MAX_LOAD ceiling," \
       "after $MAX_WAITS waits. Every figure below is an upper bound." >&2
fi

echo "suite-battery: N=$N target=$TARGET sha=${SHA:0:8}"
echo "suite-battery: load at start $START_LOAD"

rm -rf "$FX"; mkdir -p "$FX"
for i in $(seq 1 "$N"); do
  git clone --quiet --no-hardlinks "$REPO" "$FX/c$i" || { echo "clone $i failed" >&2; exit 1; }
  git -C "$FX/c$i" checkout --quiet "$SHA" || { echo "checkout $i failed" >&2; exit 1; }
done

BEGIN=$(date +%s)
PIDS=()
for i in $(seq 1 "$N"); do
  ( cd "$FX/c$i" && timeout 3600 make --no-print-directory "$TARGET" > "$FX/c$i.log" 2>&1
    echo $? > "$FX/c$i.exit" ) &
  PIDS+=($!)
done
wait "${PIDS[@]}"
FINISH=$(date +%s)

echo "suite-battery: load at end   $(cat /proc/loadavg)"
echo "suite-battery: wall $((FINISH-BEGIN)) s"

FAILED=0
for i in $(seq 1 "$N"); do
  exit_code="$(cat "$FX/c$i.exit" 2>/dev/null || echo 99)"
  summary="$(grep -E '^Ran [0-9]+ tests' -A1 "$FX/c$i.log" | tr '\n' ' ' | sed 's/  */ /g')"
  echo "--- clone $i: exit $exit_code | $summary"
  grep -E "^(FAIL|ERROR) in|temp-leak:|lane-refused:|tmp-refused:" "$FX/c$i.log" | head -10
  # A receipt must name its subject AND its evidence: exit 0 alone is not
  # enough (a target can exit 0 having run nothing), and a summary line alone
  # is not enough (a later self-test can fail after a green suite).
  [ "$exit_code" = "0" ] || FAILED=1
  echo "$summary" | grep -q "0 failures, 0 errors" || FAILED=1
done

if [ "$FAILED" = "0" ]; then
  echo "suite-battery: VERDICT PASS -- all $N clones 0 failures, 0 errors"
else
  echo "suite-battery: VERDICT FAIL -- at least one clone was not clean"
fi
exit "$FAILED"
