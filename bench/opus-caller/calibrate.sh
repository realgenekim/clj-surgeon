#!/usr/bin/env bash
# calibrate.sh — the Opus cohort's ORDER, frozen as a file before any arm runs.
#
#   calibrate.sh plan  [out]   write the plan file (default; touches nothing else)
#   calibrate.sh run           execute the plan, arm by arm, serially
#
# `run` is refused unless RUNTIME_ALLOWED=1 and the shared quiet window is absent or
# owned by this seat.  Preparation is the default because runtime on this seat is a
# metered pool Astra allocates, not a resource this script may decide to spend.
#
# ORDER (Astra's rule: six native calibrations before any comparison; do not select a
# favourable run as the baseline; a failed arm is recorded, never quietly replaced):
#
#   Block A  calibration, 6 native      N-1 .. N-6
#   Block B  comparison, 6 tool         T-1 .. T-6, interleaved with Block C's
#                                       drift controls when those are enabled
#   Block C  drift controls, 3 native   N-7 .. N-9   (OPTIONAL, off by default)
#   Block D  adoption, 3 optional       O-1 .. O-3   LAST, and never a speed cell
#
# THE ONE DEVIATION FROM ASTRA'S PRE-REGISTRATION, stated plainly: his design runs six
# matched native/tool PAIRS after calibration, which costs six further native arms.
# This cohort's budget is the seat's Claude weekly pool, so by default Block A's six
# natives serve as the native controls for Block B's six tool arms, and the later
# natives that protect against drift are Block C, off unless DRIFT=1.  With DRIFT=0
# the cohort is exploratory on drift and must say so; a speed claim needs DRIFT=1.
#
# Every arm goes through ~/bin/slot -t: load-gated (refuses above 1-minute load 10),
# quiet-window-aware, one of the box-wide slots shared with Astra.  One arm at a time.
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ACTION=${1:-plan}
PLAN=${2:-$HERE/plan-opus-cohort.txt}
MCP_URL=${OPUS_MCP_URL:-http://127.0.0.1:8341/mcp}
DRIFT=${DRIFT:-0}
SLOT=${SLOT_BIN:-/home/forge/bin/slot}

order() {
  local i
  for i in 1 2 3 4 5 6; do echo "N $i calibration"; done
  if [ "$DRIFT" = 1 ]; then
    # interleaved: T, then a drift native, alternating which cell leads each pair
    echo "T 1 comparison"; echo "N 7 drift"
    echo "N 8 drift";      echo "T 2 comparison"
    echo "T 3 comparison"; echo "N 9 drift"
    for i in 4 5 6; do echo "T $i comparison"; done
  else
    for i in 1 2 3 4 5 6; do echo "T $i comparison"; done
  fi
  for i in 1 2 3; do echo "O $i adoption"; done
}

write_plan() {
  {
    echo "# Opus-caller cohort plan — astra-fanout, 21-owner alias migration"
    echo "# written $(date -u +%Y-%m-%dT%H:%M:%SZ) by $(basename "${BASH_SOURCE[0]}")"
    echo "# caller: claude -p --model ${OPUS_MODEL:-claude-opus-5} --dangerously-skip-permissions"
    echo "# fixture: ${OPUS_FIXTURE_SRC:-/var/tmp/forge/astra-program/verified21/base-repo}"
    echo "#          at ${OPUS_FIXTURE_SHA:-92fdf5d1545af934ff14250d39cef41c400e5df8}"
    echo "# oracle:  ${OPUS_ORACLE:-/var/tmp/forge/astra-program/repo/bench/fanout/rescore-FAN.sh}"
    echo "# mcp:     $MCP_URL   (T and O only; this cohort's own band 8340-8379)"
    echo "# drift controls (Block C): $([ "$DRIFT" = 1 ] && echo ENABLED || echo "OFF — cohort is exploratory on drift")"
    echo "# every arm: $SLOT -t  (refuses above 1-min load 10, honours the quiet window)"
    echo "#"
    echo "# ord cell rep block  command"
    local n=0 cell rep block
    while read -r cell rep block; do
      n=$((n+1))
      printf '%4d %4s %3s %-11s ' "$n" "$cell" "$rep" "$block"
      if [ "$cell" = N ]; then
        printf '%s -t bash %s %s %s\n' "$SLOT" "$HERE/run-opus-arm.sh" "$cell" "$rep"
      else
        printf 'OPUS_MCP_URL=%s %s -t bash %s %s %s\n' "$MCP_URL" "$SLOT" "$HERE/run-opus-arm.sh" "$cell" "$rep"
      fi
    done < <(order)
    echo "#"
    echo "# total arms: $(order | wc -l)"
    echo "# stopping rules (Astra's, inherited): a 900s arm is a failed task, not a"
    echo "#   missing observation; a failed arm is recorded and NOT replaced by a rerun;"
    echo "#   an instrument-invalid run gets a fresh rep number; two identical tool"
    echo "#   refusals stop Block B for a contract investigation."
  } > "$PLAN"
  echo "plan written: $PLAN ($(order | wc -l) arms)"
}

case "$ACTION" in
  plan) write_plan;;
  run)
    [ "${RUNTIME_ALLOWED:-0}" = 1 ] || { echo "calibrate: REFUSED — RUNTIME_ALLOWED is not 1; runtime is allocated by the program owner, not by this script" >&2; exit 2; }
    Q=/var/tmp/forge/quiet-window.md
    if [ -f "$Q" ] && ! grep -q "owner=${SLOT_OWNER:-fable}" "$Q"; then
      echo "calibrate: REFUSED — quiet window held by another agent: $(head -1 "$Q")" >&2; exit 2
    fi
    write_plan
    while read -r cell rep block; do
      echo "=== $(date -u +%H:%M:%SZ) $cell-$rep ($block) load=$(cut -d' ' -f1 /proc/loadavg)"
      if [ "$cell" = N ]; then
        "$SLOT" -t bash "$HERE/run-opus-arm.sh" "$cell" "$rep"
      else
        OPUS_MCP_URL="$MCP_URL" "$SLOT" -t bash "$HERE/run-opus-arm.sh" "$cell" "$rep"
      fi
      rc=$?
      echo "=== $cell-$rep rc=$rc  (recorded; a failed arm is NOT replaced by a rerun)"
    done < <(order)
    ;;
  *) echo "usage: calibrate.sh [plan [out] | run]" >&2; exit 64;;
esac
