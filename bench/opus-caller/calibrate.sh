#!/usr/bin/env bash
# calibrate.sh — the Opus cohort's ORDER and its per-arm lifecycle, frozen as a file
# before any arm runs.
#
#   calibrate.sh plan  [out]   write the plan file (default; touches nothing else)
#   calibrate.sh run           execute the plan, arm by arm, serially
#
# `run` is refused unless RUNTIME_ALLOWED=1.  Preparation is the default because
# runtime on this seat is a metered pool the program owner allocates, not a resource
# this script may decide to spend.
#
# ORDER — Astra's, as he ruled it in round two.  21 arms.
#
#   Block A  calibration      6 native            N-1 .. N-6
#   Block B  matched pairs    6 native + 6 tool   N-7 .. N-12 with T-1 .. T-6,
#                             BALANCED INTERLEAVE: the pairs alternate which cell
#                             leads -- N,T then T,N then N,T ... so neither cell
#                             systematically occupies the warmer or cooler half of a
#                             pair.  These are SIX MORE natives; Block A is never
#                             reused as Block B's control.  They are the later natives
#                             that protect against drift after calibration.
#   Block C  adoption         3 optional          O-1 .. O-3, LAST, never a speed cell
#
# SAME-CORES DOCTRINE.  Every arm -- the caller, its MCP server, and any profile check
# the task runs -- is pinned to cores 12,13, the cores Astra pins for Sol and Astra, so
# a cross-caller reader is looking at the same silicon.
#
# OWNED QUIET WINDOW.  Each arm runs inside a window this script OPENS and CLOSES:
# /var/tmp/forge/quiet-window.md is created with `set -o noclobber` (so a peer's
# existing window is a refusal, never an overwrite), held for that arm's duration, and
# removed afterwards -- including on interrupt.  run-opus-arm.sh independently refuses
# unless an owned window is present, so a hand-run arm cannot skip this.
#
# SERVER LIFECYCLE.  For T and O the parent starts ONE server per arm from a pinned
# checkout, bound to that arm's worktree, on that arm's port, under the same taskset;
# writes the ready evidence in Astra's field shape; and stops only the process it
# started, by pid + start-ticks + boot id.  The ARM then attests that evidence through
# HIS adapter (astra_policy.py -> adapter.validate_ready + pid_listens).  Parent owns
# start/stop, arm owns attestation: that is his contract, not a new one.
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ACTION=${1:-plan}
PLAN=${2:-$HERE/plan-opus-cohort.txt}
PORT=${OPUS_PORT:-8341}
MCP_URL=${OPUS_MCP_URL:-http://127.0.0.1:$PORT/mcp}
SLOT=${SLOT_BIN:-/home/forge/bin/slot}
CPUS=${OPUS_CPUS:-12,13}
TASKSET=${OPUS_TASKSET:-/usr/bin/taskset}
ARMS_ROOT=${OPUS_ARMS_ROOT:-/var/tmp/forge/opus-arms}
SERVER_SRC=${OPUS_SERVER_SRC:-}
OWNER=${SLOT_OWNER:-fable}
Q=${OPUS_QUIET_WINDOW:-/var/tmp/forge/quiet-window.md}

order() {
  local i
  for i in 1 2 3 4 5 6; do echo "N $i A-calibration"; done
  # Block B: six matched pairs, alternating which cell leads
  echo "N 7  B-pair1"; echo "T 1  B-pair1"
  echo "T 2  B-pair2"; echo "N 8  B-pair2"
  echo "N 9  B-pair3"; echo "T 3  B-pair3"
  echo "T 4  B-pair4"; echo "N 10 B-pair4"
  echo "N 11 B-pair5"; echo "T 5  B-pair5"
  echo "T 6  B-pair6"; echo "N 12 B-pair6"
  for i in 1 2 3; do echo "O $i C-adoption"; done
}

write_plan() {
  {
    echo "# Opus-caller cohort plan — astra-fanout, 21-owner alias migration"
    echo "# written $(date -u +%Y-%m-%dT%H:%M:%SZ) by $(basename "${BASH_SOURCE[0]}")"
    echo "# caller: taskset -c $CPUS claude -p --model ${OPUS_MODEL:-claude-opus-5} --dangerously-skip-permissions"
    echo "#   (the command alias is NEVER the model claim; the resolved id comes from"
    echo "#    the session transcript — see attribution.json models_in_transcript)"
    echo "# fixture: ${OPUS_FIXTURE_SRC:-/var/tmp/forge/astra-program/verified21/base-repo}"
    echo "#          at ${OPUS_FIXTURE_SHA:-92fdf5d1545af934ff14250d39cef41c400e5df8}  (Astra's base, unchanged)"
    echo "# prompts: ${OPUS_PROMPT_DIR:-/var/tmp/forge/astra-program/verified21/prompts}  (his files, verbatim)"
    echo "# oracle:  ${OPUS_ORACLE:-/var/tmp/forge/astra-program/repo/bench/fanout/rescore-FAN.sh}  (his six checks)"
    echo "# mcp:     $MCP_URL   (T and O only; this flank's own band 8340-8379)"
    echo "# cores:   $CPUS for the caller, its server, and any profile check"
    echo "# window:  $Q opened per arm as owner=$OWNER with noclobber, removed after"
    echo "# slot:    $SLOT -t  (refuses above 1-min load 10)"
    echo "#"
    echo "# ord cell rep block          command"
    local n=0 cell rep block
    while read -r cell rep block; do
      n=$((n+1))
      printf '%4d %4s %3s %-14s ' "$n" "$cell" "$rep" "$block"
      if [ "$cell" = N ]; then
        printf '%s -t bash %s %s %s\n' "$SLOT" "$HERE/run-opus-arm.sh" "$cell" "$rep"
      else
        printf 'OPUS_MCP_URL=%s OPUS_READY=<armdir>/server/ready.json OPUS_SERVER_SHA=<pinned> %s -t bash %s %s %s\n' \
          "$MCP_URL" "$SLOT" "$HERE/run-opus-arm.sh" "$cell" "$rep"
      fi
    done < <(order)
    echo "#"
    echo "# TOTAL ARMS: $(order | wc -l)   (6 calibration native + 6 pair native + 6 pair tool + 3 adoption)"
    echo "#"
    echo "# PREFLIGHT: the FIRST arm, N-1, is the instrument preflight, run ALONE."
    echo "#   The 38 fake-caller tests prove the harness, not live readiness: they never"
    echo "#   call a model, never start a server, never run the real oracle.  N-1 is the"
    echo "#   first evidence that a live Claude session binds, that the transcript names"
    echo "#   the resolved model, and that the six checks run on a real tree.  Read its"
    echo "#   receipt AND the pool meter before arm 2."
    echo "#"
    echo "# stopping rules (Astra's, inherited): a 900s arm is a failed task, not a"
    echo "#   missing observation; a failed arm is recorded and NOT replaced by a rerun;"
    echo "#   an instrument-invalid run gets a fresh rep number; two identical tool"
    echo "#   refusals stop Block B for a contract investigation; if load exceeds 10"
    echo "#   during an arm it is preserved as a contaminated observation and excluded"
    echo "#   from any clean-wall claim, never deleted."
  } > "$PLAN"
  echo "plan written: $PLAN ($(order | wc -l) arms)"
}

# --- the owned quiet window -------------------------------------------------------
open_window() {   # open_window <label>
  set -o noclobber
  if ! { printf 'owner=%s arm=%s opened=%s pid=%s\n' \
           "$OWNER" "$1" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$$" > "$Q"; } 2>/dev/null; then
    set +o noclobber
    echo "calibrate: REFUSED — a quiet window already exists (peer's, or a leftover): $(head -1 "$Q" 2>/dev/null)" >&2
    return 1
  fi
  set +o noclobber
  WINDOW_OPEN=1
}
close_window() { [ "${WINDOW_OPEN:-0}" = 1 ] || return 0; rm -f "$Q"; WINDOW_OPEN=0; }

# --- one arm's MCP server: started here, stopped here, attested by the arm ---------
start_server() {  # start_server <armdir>
  local A=$1
  [ -n "$SERVER_SRC" ] || { echo "calibrate: REFUSED — a tool arm needs OPUS_SERVER_SRC (a pinned server checkout)" >&2; return 1; }
  mkdir -p "$A/server"
  ( cd "$SERVER_SRC" && exec nohup "$TASKSET" -c "$CPUS" clojure -X:clj-surgeon/mcp \
      :project-dir "\"$A/wt\"" :port "$PORT" :telemetry :full \
      :telemetry-dir "\"$A/server/telemetry\"" :run-id "\"$(basename "$A")\"" \
      :ready-file "\"$A/server/ready.edn\"" :nrepl-port :none \
      > "$A/server/server.log" 2>&1 ) &
  local pid=$!
  # pid + start ticks + boot id: only this triple warrants a later signal (his rule)
  printf '%s %s %s\n' "$pid" \
    "$(cut -d')' -f2- "/proc/$pid/stat" 2>/dev/null | awk '{print $20}')" \
    "$(cat /proc/sys/kernel/random/boot_id 2>/dev/null)" > "$A/server/spawned.pid"
  local i
  for i in $(seq 1 90); do
    curl -fsS --max-time 3 "http://127.0.0.1:$PORT/healthz" >/dev/null 2>&1 && break
    sleep 1
  done
  local health
  health=$(curl -fsS --max-time 5 "http://127.0.0.1:$PORT/healthz") || {
    echo "calibrate: REFUSED — server never became healthy on $PORT" >&2; return 1; }
  # ready evidence in ASTRA'S FIELD SHAPE; his validate_ready is what checks it
  python3 - "$A/server/ready.json" "$MCP_URL" "http://127.0.0.1:$PORT/healthz" \
           "$(git -C "$SERVER_SRC" rev-parse HEAD)" "$A/wt" "$pid" "$SERVER_SRC" <<'PY'
import hashlib, json, sys, urllib.request
out, url, hz, sha, root, pid, cwd = sys.argv[1:8]
body = urllib.request.urlopen(hz, timeout=5).read()
json.dump({"mcp_url": url, "healthz_url": hz, "server_sha": sha,
           "project_root": root, "port_pid": int(pid), "server_cwd": cwd,
           "healthz_sha256": hashlib.sha256(body).hexdigest()},
          open(out, "w"), indent=2, sort_keys=True)
PY
  SERVER_STARTED=1
}
stop_server() {   # stop only what WE started, by pid+start-ticks+boot id
  [ "${SERVER_STARTED:-0}" = 1 ] || return 0
  bash "$HERE/../anvil-arms/stop-server.sh" "$1" || true
  SERVER_STARTED=0
}

cleanup() { stop_server "${CURRENT_ARM:-/nonexistent}"; close_window; }
trap cleanup EXIT INT TERM

case "$ACTION" in
  plan) write_plan;;
  run)
    [ "${RUNTIME_ALLOWED:-0}" = 1 ] || { echo "calibrate: REFUSED — RUNTIME_ALLOWED is not 1; runtime is allocated by the program owner, not by this script" >&2; exit 2; }
    write_plan
    while read -r cell rep block; do
      A="$ARMS_ROOT/opus-$cell-$rep"; CURRENT_ARM=$A
      echo "=== $(date -u +%H:%M:%SZ) $cell-$rep ($block) load=$(cut -d' ' -f1 /proc/loadavg)"
      open_window "$cell-$rep" || exit 2
      if [ "$cell" = N ]; then
        "$SLOT" -t bash "$HERE/run-opus-arm.sh" "$cell" "$rep"; rc=$?
      else
        # the server is bound to the arm's worktree, so the clone must exist first:
        # run-opus-arm.sh makes it, so the server is started against the arm dir it
        # will create.  mkdir here, clone there, server between -- see README note.
        mkdir -p "$A"
        if start_server "$A"; then
          OPUS_MCP_URL="$MCP_URL" OPUS_READY="$A/server/ready.json" \
          OPUS_SERVER_SHA="$(git -C "$SERVER_SRC" rev-parse HEAD)" \
            "$SLOT" -t bash "$HERE/run-opus-arm.sh" "$cell" "$rep"; rc=$?
        else
          rc=2
        fi
        stop_server "$A"
      fi
      close_window
      echo "=== $cell-$rep rc=$rc  (recorded; a failed arm is NOT replaced by a rerun)"
      if [ "$cell$rep" = "N1" ]; then
        echo "=== PREFLIGHT ARM COMPLETE.  Read $A/arm.json, $A/attribution.json and the"
        echo "=== pool meter before arm 2.  Stop here if anything is :unverified."
        [ "${OPUS_CONTINUE_AFTER_PREFLIGHT:-0}" = 1 ] || { echo "=== halting after preflight (set OPUS_CONTINUE_AFTER_PREFLIGHT=1 to run the cohort)"; exit $rc; }
      fi
    done < <(order)
    ;;
  *) echo "usage: calibrate.sh [plan [out] | run]" >&2; exit 64;;
esac
