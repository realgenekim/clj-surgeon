#!/usr/bin/env bash
# run-arm.sh — one arm-run: attest -> watch(driver) -> freeze diff -> score.
#
# It composes the three meters of docs/observations/2026-09-04-e3-e6-prestaged.md
# (A.4 attestation, A.10 watcher, A.7 predicates) for a single arm, from a prompt
# file and an arm spec.  It never scores an arm the attestation refused: attest.sh
# exits 2 and this script returns 2 WITHOUT launching a driver.
#
#   run-arm.sh --root DIR --exp e3 --rung P --arm T --slot 1 \
#              --prompt /path/E3-P-T.md --port 7907 \
#              --expected-server-sha <sha> [options]
#
# Options
#   --base SHA | --base-file PATH   the pinned base (default <armdir>/base.sha)
#   --worktree-src PATH             clone it into <armdir>/worktree if absent
#   --driver sol|claude|fake        default sol
#   --fixture NAME                  fake driver fixture (default pf5)
#   --model ID                      recorded as :model
#   --server-src PATH               start this arm's MCP server from that checkout
#   --churn-band lo,hi,lo,hi        churn pass band handed to score.py
#   --watch-arg ARG                 extra argument passed through to watch.py (repeatable)
#   --dry-run                       print the plan and stop
#
# Boundaries this script enforces, not merely documents:
#   * --root must resolve inside /home/forge/tmp/arms; anything else is ROOT-REFUSED
#     before a single directory is created.  The driver's CODEX_HOME is per-arm, so
#     nothing writes into a shared ~/.codex either.
#   * every identity component (exp/rung/arm/slot) is a PATH SEGMENT, so each is
#     validated against ^[A-Za-z0-9._-]{1,40}$ with no `..`, and the resolved arm
#     directory must itself lie inside the validated root: IDENTITY-REFUSED otherwise,
#     again before a single directory is created.
#   * a tool/free-choice arm's port must be in COHORT_PORTS (default 7907-7910).
#     Nothing here ever contacts 7888, 7894, 7895 or 7906.
#   * it kills only a server process THIS script started, by the pid in ready.edn.
#   * the diff is frozen with a plain `git diff <base>` BEFORE any acceptance file
#     is copied in, and a non-zero rc writes DIFF-FAILED rather than passing an
#     empty diff off as a clean one.
set -uo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
COHORT_PORTS=${COHORT_PORTS:-7907 7908 7909 7910}

ROOT=""; EXP=""; RUNG=""; ARM=""; SLOT=""; PROMPT=""; PORT="-"
EXPECTED_SERVER_SHA=""; BASE=""; BASE_FILE=""; WORKTREE_SRC=""
DRIVER=sol; FIXTURE=pf5; MODEL=""; SERVER_SRC=""; CHURN_BAND=""; DRY=0
WATCH_ARGS=()

while [ $# -gt 0 ]; do
  case "$1" in
    --root) ROOT=$2; shift 2;;
    --exp) EXP=$2; shift 2;;
    --rung) RUNG=$2; shift 2;;
    --arm) ARM=$2; shift 2;;
    --slot) SLOT=$2; shift 2;;
    --prompt) PROMPT=$2; shift 2;;
    --port) PORT=$2; shift 2;;
    --expected-server-sha) EXPECTED_SERVER_SHA=$2; shift 2;;
    --base) BASE=$2; shift 2;;
    --base-file) BASE_FILE=$2; shift 2;;
    --worktree-src) WORKTREE_SRC=$2; shift 2;;
    --driver) DRIVER=$2; shift 2;;
    --fixture) FIXTURE=$2; shift 2;;
    --model) MODEL=$2; shift 2;;
    --server-src) SERVER_SRC=$2; shift 2;;
    --churn-band) CHURN_BAND=$2; shift 2;;
    --watch-arg) WATCH_ARGS+=("$2"); shift 2;;
    --dry-run) DRY=1; shift;;
    *) echo "run-arm: unknown argument $1" >&2; exit 64;;
  esac
done

for required in ROOT EXP RUNG ARM SLOT PROMPT; do
  [ -n "${!required}" ] || { echo "run-arm: --${required,,} is required" >&2; exit 64; }
done

# Every byte this apparatus writes lands under the runner root or the arm's own
# worktree.  Sol, item 12: --root was unconstrained, so an arm-run could scatter
# evidence anywhere the caller pointed it, and afterwards nobody could say which bytes
# on the box were part of the experiment.  The check is on the RESOLVED path, so a
# ../ spelling cannot walk out of the root.
ARMS_ROOT_BASE=/home/forge/tmp/arms
ROOT_REAL=$(readlink -m "$ROOT")
case "$ROOT_REAL" in
  "$ARMS_ROOT_BASE"|"$ARMS_ROOT_BASE"/*) ;;
  *) echo "run-arm: ROOT-REFUSED '$ROOT' resolves to '$ROOT_REAL', outside $ARMS_ROOT_BASE — nothing was created" >&2
     exit 2;;
esac
ROOT=$ROOT_REAL

# EVERY IDENTITY COMPONENT BECOMES A PATH SEGMENT of the arm directory.  Sol round two,
# item 9: --root was refused on its resolved path and then exp/rung/arm/slot were
# interpolated in unvalidated, so `--exp ../component-escape` created a directory
# outside the runner root the caller had just been checked for.  Confining the root and
# then building the path out of unchecked caller strings confines nothing.
for component in EXP RUNG ARM SLOT; do
  value=${!component}
  case "$value" in
    *..*) echo "run-arm: IDENTITY-REFUSED --${component,,} '$value' contains '..' — nothing was created" >&2
          exit 2;;
  esac
  if [[ ! $value =~ ^[A-Za-z0-9._-]{1,40}$ ]]; then
    echo "run-arm: IDENTITY-REFUSED --${component,,} '$value' is not [A-Za-z0-9._-]{1,40} — nothing was created" >&2
    exit 2
  fi
done

A="$ROOT/$EXP-$RUNG-$ARM-$SLOT"
# Belt and braces: the arm directory itself must resolve INSIDE the validated root,
# whatever the components spelled.  A refusal on the resolved path is the only one a
# spelling cannot walk around.
A_REAL=$(readlink -m "$A")
case "$A_REAL" in
  "$ROOT"/*) ;;
  *) echo "run-arm: IDENTITY-REFUSED arm dir '$A' resolves to '$A_REAL', outside the runner root $ROOT — nothing was created" >&2
     exit 2;;
esac
A=$A_REAL
case "$ARM" in
  N) PORT="-"; URL="";;
  T|F)
    ok=no; for p in $COHORT_PORTS; do [ "$PORT" = "$p" ] && ok=yes; done
    [ "$ok" = yes ] || { echo "run-arm: REFUSING port '$PORT' — arm $ARM must use one of: $COHORT_PORTS" >&2; exit 2; }
    URL="http://127.0.0.1:$PORT/mcp";;
  *) echo "run-arm: unknown arm '$ARM' (expect N, T or F)" >&2; exit 64;;
esac

case "$DRIVER" in
  sol)    DRIVER_NAME=codex-exec-sol; MODEL=${MODEL:-gpt-5.6-sol};;
  claude) DRIVER_NAME=claude-p;       MODEL=${MODEL:-unverified};;
  fake)   DRIVER_NAME=fake-driver;    MODEL=${MODEL:-none-fake-driver};;
  *) echo "run-arm: unknown driver '$DRIVER'" >&2; exit 64;;
esac

if [ $DRY -eq 1 ]; then
  echo "PLAN arm=$A prompt=$PROMPT driver=$DRIVER_NAME model=$MODEL port=$PORT url=${URL:-none}"
  exit 0
fi

mkdir -p "$A/server" || exit 2

# --- 1. worktree ------------------------------------------------------------------
if [ ! -d "$A/worktree" ] && [ -n "$WORKTREE_SRC" ]; then
  git clone -q --no-hardlinks "$WORKTREE_SRC" "$A/worktree" || { echo "run-arm: clone failed" >&2; exit 2; }
  [ -n "$BASE" ] && git -C "$A/worktree" checkout -q --detach "$BASE"
fi
[ -d "$A/worktree" ] || { echo "run-arm: no worktree at $A/worktree" >&2; exit 2; }

if [ -n "$BASE_FILE" ]; then cp "$BASE_FILE" "$A/base.sha"
elif [ -n "$BASE" ]; then printf '%s\n' "$BASE" > "$A/base.sha"
elif [ ! -f "$A/base.sha" ]; then git -C "$A/worktree" rev-parse HEAD > "$A/base.sha"; fi
BASE_SHA=$(tr -d '[:space:]' < "$A/base.sha")

# --- 2. the prompt actually served, and its hash ----------------------------------
cp "$PROMPT" "$A/prompt.md" || { echo "run-arm: cannot copy prompt" >&2; exit 2; }
sha256sum "$A/prompt.md" | cut -d' ' -f1 > "$A/prompt.sha256"

# --- 2b. resolve this worktree's Make targets to the commands they RUN -------------
# The Makefile is PARSED, never executed, and only inside a whitelisted subset: the
# watcher and the scorer both read this map, so a test runner behind a target whose
# NAME does not say "test" (e.g. `make verify`) is metered as a test action instead of
# a non-test one -- and a Makefile outside the subset resolves NOTHING, which makes
# every `make` call in that arm `incomplete-run` rather than a wrong number.
python3 "$HERE/_make_targets.py" "$A/worktree" "$A/make-targets.json" >> "$A/driver.log" 2>&1
make_map_rc=$?
[ $make_map_rc -eq 0 ] || echo "run-arm: make targets unresolved (rc=$make_map_rc)" >> "$A/driver.log"
make_map_whitelist=$(python3 -c 'import json,sys
try:
    print(json.load(open(sys.argv[1])).get("whitelist_refusal") or "")
except Exception:
    print("")' "$A/make-targets.json" 2>/dev/null)
[ -z "$make_map_whitelist" ] || echo "run-arm: $make_map_whitelist — no make target resolves in this worktree; any \`make\` call this arm issues is incomplete-run, so the arm must invoke its test runner directly to be metered" >> "$A/driver.log"

# --- 3. this arm's MCP server, bound to THIS worktree (A.5) ------------------------
SERVER_STARTED=0
rm -f "$A/server/spawned.pid"
if [ "$ARM" != "N" ] && [ -n "$SERVER_SRC" ]; then
  ( cd "$SERVER_SRC" && exec nohup clojure -X:clj-surgeon/mcp \
      :project-dir "\"$A/worktree\"" :port "$PORT" :telemetry :full \
      :telemetry-dir "\"$A/server/telemetry\"" :run-id "\"$EXP-$RUNG-$ARM-$SLOT\"" \
      :ready-file "\"$A/server/ready.edn\"" :nrepl-port :none \
      > "$A/server/server.log" 2>&1 ) &
  SERVER_PID=$!
  # RECORD WHAT WE SPAWNED, with its start time, so cleanup can prove authorship.
  # ready.edn is the SERVER's claim about itself; this file is THIS SCRIPT's record of
  # what it forked, and only the second warrants a signal.  The start time is what
  # keeps a reused pid from being mistaken for our server -- within one boot; the boot
  # id is what keeps a pre-reboot record from being mistaken for anything at all.
  # pid, start ticks, and the BOOT ID those ticks are counted from: start ticks repeat
  # across reboots, and an arm directory on disk outlives a boot (Sol round two, item 8).
  printf '%s %s %s\n' "$SERVER_PID" \
    "$(cut -d')' -f2- "/proc/$SERVER_PID/stat" 2>/dev/null | awk '{print $20}')" \
    "$(cat /proc/sys/kernel/random/boot_id 2>/dev/null)" \
    > "$A/server/spawned.pid"
  SERVER_STARTED=1
  for _ in $(seq 1 90); do
    curl -fsS --max-time 3 "http://127.0.0.1:$PORT/healthz" >/dev/null 2>&1 && break
    sleep 1
  done
fi

stop_server () {
  [ "$SERVER_STARTED" = 1 ] || return 0
  bash "$HERE/stop-server.sh" "$A"
}

# --- 4. ATTEST BEFORE THE DRIVER STARTS -------------------------------------------
EXP="$EXP" RUNG="$RUNG" SLOT="$SLOT" MODEL="$MODEL" DRIVER="$DRIVER_NAME" \
RUNNER="$HERE/run-arm.sh" MCP_URL="$URL" PROMPT="$A/prompt.md" \
COHORT_PORTS="$COHORT_PORTS" MAKE_TARGETS="$A/make-targets.json" \
  bash "$HERE/attest.sh" "$A" "$ARM" "$PORT" "$EXPECTED_SERVER_SHA"
attest_rc=$?
if [ $attest_rc -ne 0 ]; then
  echo "ATTEST-MISMATCH $A — the driver was never launched" >&2
  stop_server
  exit 2
fi

# --- 5. drive + meter -------------------------------------------------------------
case "$DRIVER" in
  sol)
    # A CODEX_HOME PRIVATE TO THIS ARM, and a rollout bound to the session codex
    # announces about itself.  The old form globbed $HOME/.codex/sessions and took the
    # newest file, which mis-binds any concurrent codex session on this box and writes
    # outside the arm root besides.
    mkdir -p "$A/codex-home"
    # A private CODEX_HOME keeps this arm's rollout out of a shared ~/.codex, but codex
    # reads its CREDENTIALS from that same directory: an empty one makes every arm exit
    # 1 with zero returns before a single model turn (measured 2026-09-04, E3-P PF-5 --
    # the first time this apparatus drove a live codex rather than the fake driver).
    # The credential is SYMLINKED, never copied: one file, one refresh, no fan-out of a
    # secret across a dozen arm directories, and nothing to delete afterwards.
    if [ -e "$HOME/.codex/auth.json" ] && [ ! -e "$A/codex-home/auth.json" ]; then
      ln -s "$HOME/.codex/auth.json" "$A/codex-home/auth.json"
    fi
    WATCH_ARGS+=(--codex-home "$A/codex-home")
    DRIVER_CMD=(env "CODEX_HOME=$A/codex-home"
                "$HOME/bin/sol-yolo" "$A/worktree" "$A/prompt.md" "$URL" "$A/driver-report.md")
    ;;
  claude)
    WATCH_ARGS+=(--capture-stdout)
    DRIVER_CMD=(claude -p --model "$MODEL" --output-format stream-json --verbose
                --add-dir "$A/worktree")
    if [ -n "$URL" ]; then
      # written here, per arm, so the MCP binding is a property of the command line
      # and not of a config file someone might forget to revert (A.9)
      printf '{"mcpServers":{"clj-surgeon":{"type":"http","url":"%s"}}}\n' "$URL" \
        > "$A/mcp.json"
      DRIVER_CMD+=(--mcp-config "$A/mcp.json")
    fi
    ;;
  fake)
    DRIVER_CMD=(bash "$HERE/fake-driver.sh" "$A" "$FIXTURE")
    ;;
esac

python3 "$HERE/watch.py" --arm "$A" "${WATCH_ARGS[@]}" -- "${DRIVER_CMD[@]}" \
  < /dev/null >> "$A/driver.log" 2>&1
watch_rc=$?

# --- 6. freeze the diff BEFORE any acceptance file touches the worktree ------------
if git -C "$A/worktree" diff "$BASE_SHA" > "$A/diff.patch" 2>>"$A/driver.log"; then
  :
else
  rc=$?
  echo "DIFF-FAILED rc=$rc base=$BASE_SHA" >> "$A/driver.log"
  rm -f "$A/diff.patch"
fi

stop_server

# --- 7. score ---------------------------------------------------------------------
SCORE_ARGS=("$A")
[ -n "$CHURN_BAND" ] && SCORE_ARGS+=(--churn-band "$CHURN_BAND")
python3 "$HERE/score.py" "${SCORE_ARGS[@]}"
score_rc=$?

echo "run-arm: $A watch_rc=$watch_rc score_rc=$score_rc"
[ $score_rc -ne 0 ] && exit $score_rc
exit 0
