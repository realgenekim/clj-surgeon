#!/usr/bin/env bash
# run-opus-arm.sh <cell:N|T|O> <rep> — ONE Claude-Opus arm of Astra's frozen
# 21-owner alias-migration comparison.
#
# This is the Opus flank of the fanout cohort.  Every measured factor Astra froze
# (fixture + base sha, task prompt, acceptance oracle, proof obligations, one arm at
# a time under a load-gated slot) is used HERE BY PATH AND SHA, never copied and
# never re-specified.  What differs is only the caller: `claude -p` on the seat's
# subscription instead of `codex exec`, and therefore a different attribution path
# (a Claude session transcript instead of a codex rollout).  See
# docs/observations/2026-09-05-opus-caller-harness.md.
#
# Cells
#   N  native      — Astra's frozen fanout-native.txt, no MCP server attached
#   T  tool        — Astra's frozen fanout-tool.txt (mandate to try alias_migration
#                    for the write) + the MCP server URL and the tool's Claude name
#   O  optional    — Astra's frozen fanout-common.txt + a neutral stanza: the same
#                    server is attached and the tool merely EXISTS, no mandate.
#                    This is the adoption cell; it is not a speed cell.
#
# Refusals are typed, loud and terminal: every one prints "OPUS-ARM REFUSED: <reason>"
# and exits 2 WITHOUT creating or mutating an arm directory where that is still
# possible.  Nothing here overwrites an existing arm directory, ever.
#
# Nothing in this file contacts 7888, 7890, 7894, 7895, 8171, 8173, 8174, 8175 or
# 8300-8339.  A tool/optional arm's MCP URL must be loopback HTTP in 8340-8379, this
# cohort's own band, and the harness only contacts it once RUNTIME_ALLOWED=1.
set -uo pipefail

die() { echo "OPUS-ARM REFUSED: $*" >&2; exit 2; }
mono() { cut -d' ' -f1 /proc/uptime; }

# Astra's adapter-result timing fields begin at the ADAPTER's start, not the driver's.
adapter_start=$(mono); adapter_load_start=$(cat /proc/loadavg)

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# ---- pinned inputs (Astra's frozen screen; READ-ONLY, referenced by path+sha) ------
FIXTURE_SRC=${OPUS_FIXTURE_SRC:-/var/tmp/forge/astra-program/verified21/base-repo}
FIXTURE_SHA=${OPUS_FIXTURE_SHA:-92fdf5d1545af934ff14250d39cef41c400e5df8}
PROMPT_DIR=${OPUS_PROMPT_DIR:-/var/tmp/forge/astra-program/verified21/prompts}
ORACLE=${OPUS_ORACLE:-/var/tmp/forge/astra-program/repo/bench/fanout/rescore-FAN.sh}
ORACLE_SHA=${OPUS_ORACLE_SHA256:-97486d75ff5f051b831c7997452d31ca15560fc85476ae1d57e12c69b9560eaa}
ORACLE_FIX=${OPUS_ORACLE_FIX:-/var/tmp/forge/astra-program/verified21/oracle}
ORACLE_N=${OPUS_ORACLE_N:-21}
ARMS_ROOT=${OPUS_ARMS_ROOT:-/var/tmp/forge/opus-arms}
PROJECTS_ROOT=${CLAUDE_PROJECTS_ROOT:-$HOME/.claude/projects}
CALLER=${OPUS_CALLER_BIN:-claude}
MODEL=${OPUS_MODEL:-claude-opus-5}
MAX_WALL=${OPUS_MAX_WALL:-900}
# SAME-CORES DOCTRINE, inherited from Astra's cohort: the caller, its MCP server and
# any profile check all run pinned to the same two cores he pins.
CPUS=${OPUS_CPUS:-12,13}
TASKSET=${OPUS_TASKSET:-/usr/bin/taskset}

CELL=${1:-}; REP=${2:-}
[ -n "$CELL" ] && [ -n "$REP" ] || { echo "usage: run-opus-arm.sh <N|T|O> <rep>" >&2; exit 64; }
case "$CELL" in N|T|O) ;; *) die "unknown cell '$CELL' (want N, T or O)";; esac
[[ $REP =~ ^[0-9]{1,3}$ ]] || die "rep '$REP' is not 1-3 digits"

# ---- the run gate: nothing launches, and nothing is contacted, without allocation --
[ "${RUNTIME_ALLOWED:-0}" = 1 ] || die "RUNTIME_ALLOWED is not 1 — this harness is preparation-only until runtime is allocated"
# THE QUIET WINDOW IS OWNED, NOT MERELY ABSENT.  Astra's arms run under an owned
# window on pinned cores; an arm that runs while a peer's JVM suite drains is a
# contaminated observation.  calibrate.sh opens one window per arm with `set -o
# noclobber` and removes it afterwards; a hand-run arm must do the same.
Q=${OPUS_QUIET_WINDOW:-/var/tmp/forge/quiet-window.md}
OWNER=${SLOT_OWNER:-fable}
[ -f "$Q" ] || die "no quiet window at $Q — an arm runs inside an OWNED window (calibrate.sh opens one per arm)"
grep -q "owner=$OWNER" "$Q" || die "quiet window is held by another agent: $(head -1 "$Q")"

ID="opus-$CELL-$REP"
A="$ARMS_ROOT/$ID"
WT="$A/wt"
case "$(readlink -m "$A")" in
  "$(readlink -m "$ARMS_ROOT")"/*) ;;
  *) die "arm dir '$A' resolves outside $ARMS_ROOT";;
esac
[ -e "$A" ] && die "arm directory already exists: $A (a run identity is used ONCE; pick a new rep)"

# ---- inputs must be exactly what was frozen, BEFORE anything is created ------------
[ -d "$FIXTURE_SRC/.git" ] || die "fixture source is not a git repo: $FIXTURE_SRC"
src_head=$(git -C "$FIXTURE_SRC" rev-parse HEAD 2>/dev/null) || die "cannot read fixture HEAD at $FIXTURE_SRC"
[ "$src_head" = "$FIXTURE_SHA" ] || die "fixture sha mismatch: $FIXTURE_SRC is at $src_head, pinned base is $FIXTURE_SHA"
[ -f "$ORACLE" ] || die "acceptance oracle missing: $ORACLE"
oracle_actual=$(sha256sum "$ORACLE" | cut -d' ' -f1)
[ "$oracle_actual" = "$ORACLE_SHA" ] || die "oracle sha mismatch: $ORACLE is $oracle_actual, pinned $ORACLE_SHA"
[ -f "$ORACLE_FIX/manifest-$ORACLE_N.edn" ] || die "oracle manifest missing: $ORACLE_FIX/manifest-$ORACLE_N.edn"
[ -d "$ORACLE_FIX/canonical-$ORACLE_N" ] || die "oracle canonical missing: $ORACLE_FIX/canonical-$ORACLE_N"

MCP_URL=${OPUS_MCP_URL:-}
if [ "$CELL" = N ]; then
  [ -z "$MCP_URL" ] || die "native cell cannot carry an MCP URL"
  BASE_PROMPT="$PROMPT_DIR/fanout-native.txt"
else
  [ -n "$MCP_URL" ] || die "cell $CELL requires OPUS_MCP_URL"
  [[ $MCP_URL =~ ^http://127\.0\.0\.1:(8[3][4-7][0-9])(/[A-Za-z0-9._/-]*)?$ ]] \
    || die "MCP URL must be loopback HTTP on this cohort's own band 8340-8379 (got '$MCP_URL')"
  [ "$CELL" = T ] && BASE_PROMPT="$PROMPT_DIR/fanout-tool.txt" || BASE_PROMPT="$PROMPT_DIR/fanout-common.txt"
fi
[ -f "$BASE_PROMPT" ] || die "frozen prompt missing: $BASE_PROMPT"

# A tool/optional arm carries SERVER READY EVIDENCE, and it is validated by Astra's own
# adapter predicate (astra_policy.py imports his adapter.py after checking its sha and
# calls validate_ready + pid_listens directly).  The parent owns start/stop; the arm
# owns attestation -- exactly his contract.
READY=${OPUS_READY:-}
SERVER_SHA=${OPUS_SERVER_SHA:-}
if [ "$CELL" != N ]; then
  [ -n "$READY" ] && [ -f "$READY" ] || die "cell $CELL requires OPUS_READY (the parent's server ready JSON)"
  [ -n "$SERVER_SHA" ] || die "cell $CELL requires OPUS_SERVER_SHA"
elif [ -n "$READY$SERVER_SHA" ]; then
  die "native arm cannot carry server evidence"
fi

[[ $CPUS =~ ^[0-9]+(-[0-9]+)?(,[0-9]+(-[0-9]+)?)*$ ]] || die "invalid CPU list '$CPUS'"
[ -x "$TASKSET" ] || die "taskset not executable at $TASKSET — the same-cores doctrine cannot be honoured"
CALLER_BIN=$(command -v "$CALLER") || die "caller '$CALLER' not on PATH"
CLI_VERSION=$("$CALLER_BIN" --version 2>&1 | head -1) || die "cannot read caller version"

# ---- create the arm; from here a refusal still leaves the evidence in place --------
mkdir -p "$A" || die "cannot create $A"
[ -x /home/forge/bin/trust-dir ] && /home/forge/bin/trust-dir "$A" >/dev/null 2>&1

# fresh clone of the pinned fixture, detached at the pinned sha, verified clean
git clone --quiet --no-hardlinks "$FIXTURE_SRC" "$WT" >>"$A/setup.log" 2>&1 \
  || die "clone failed (see $A/setup.log)"
git -C "$WT" checkout --quiet --detach "$FIXTURE_SHA" >>"$A/setup.log" 2>&1 \
  || die "cannot detach clone at $FIXTURE_SHA"
wt_head=$(git -C "$WT" rev-parse HEAD)
[ "$wt_head" = "$FIXTURE_SHA" ] || die "clone HEAD $wt_head != pinned $FIXTURE_SHA"
[ -z "$(git -C "$WT" status --porcelain)" ] || die "fresh clone is not clean"
for need in src test bin/fan-test; do
  [ -e "$WT/$need" ] || die "fixture lacks $need"
done
echo "$FIXTURE_SHA" > "$A/base.sha"

# guard snapshot of the bytes the task may not touch
( cd "$WT" && find test bin/fan-test -type f -print0 | sort -z | xargs -0 sha256sum ) > "$A/guard-before.txt"

# ---- server attestation, through Astra's reviewed predicate --------------------
if [ "$CELL" != N ]; then
  if ! python3 "${OPUS_POLICY_BIN:-$HERE/astra_policy.py}" validate-ready --ready "$READY" --url "$MCP_URL" \
        --server-sha "$SERVER_SHA" --worktree "$WT" > "$A/server-ready.json" 2>"$A/server-ready.err"; then
    die "server ready evidence rejected: $(head -1 "$A/server-ready.err")"
  fi
fi

# ---- compose the prompt: frozen cell text + the identical caller stanza ------------
{
  cat "$BASE_PROMPT"
  cat "$HERE/prompts/claude-caller-common.txt"
  case "$CELL" in
    T) cat "$HERE/prompts/T-tooling-claude.txt"; printf '\nMCP server URL: %s\n' "$MCP_URL";;
    O) cat "$HERE/prompts/O-tooling.txt";        printf '\nMCP server URL: %s\n' "$MCP_URL";;
  esac
} > "$A/prompt.txt"

# ---- MCP binding lives on the command line, per arm, never in a shared config ------
MCP_ARGS=()
if [ "$CELL" != N ]; then
  printf '{"mcpServers":{"clj-surgeon":{"type":"http","url":"%s"}}}\n' "$MCP_URL" > "$A/mcp.json"
  MCP_ARGS=(--mcp-config "$A/mcp.json" --strict-mcp-config)
fi

# ---- the session identity is CHOSEN, not discovered ------------------------------
SID=$(cat /proc/sys/kernel/random/uuid)
ESCAPED=$(printf '%s' "$(readlink -m "$WT")" | sed 's#[^A-Za-z0-9]#-#g')
SESSION_FILE="$PROJECTS_ROOT/$ESCAPED/$SID.jsonl"

DRIVER_CMD=("$TASKSET" -c "$CPUS" "$CALLER_BIN" -p --model "$MODEL" --dangerously-skip-permissions
            --session-id "$SID" --output-format stream-json --verbose
            --add-dir "$WT" "${MCP_ARGS[@]}"
            --disallowedTools Task Skill ToolSearch WebFetch WebSearch SendMessage
                ListAgents Monitor TaskOutput TaskStop NotebookEdit Artifact
                EnterWorktree ExitWorktree Workflow)
printf '%s\n' "${DRIVER_CMD[@]}" > "$A/command.txt"

# ---- launch ----------------------------------------------------------------------
load_start=$(cat /proc/loadavg)
mono_start=$(mono); epoch_start=$(date -u +%s.%N); utc_start=$(date -u +%Y-%m-%dT%H:%M:%SZ)

( cd "$WT" && exec timeout --signal=TERM --kill-after=30 "$MAX_WALL" \
    "${DRIVER_CMD[@]}" ) < "$A/prompt.txt" > "$A/run.log" 2>&1
driver_rc=$?

mono_end=$(mono); epoch_end=$(date -u +%s.%N); utc_end=$(date -u +%Y-%m-%dT%H:%M:%SZ)
load_end=$(cat /proc/loadavg)
{
  echo "monotonic_start_s=$mono_start"
  echo "monotonic_end_s=$mono_end"
  echo "task_wall_s=$(awk -v a="$mono_start" -v b="$mono_end" 'BEGIN{printf "%.3f", b-a}')"
  echo "utc_start=$utc_start"
  echo "utc_end=$utc_end"
  echo "epoch_start=$epoch_start"
  echo "epoch_end=$epoch_end"
  echo "driver_rc=$driver_rc"
  echo "max_wall_s=$MAX_WALL"
  echo "load_start=$load_start"
  echo "load_end=$load_end"
  echo "wall_scope=launcher-start-to-launcher-exit; excludes clone, prompt composition and the acceptance oracle"
} > "$A/task-wall.txt"

# ---- bind the run to its session transcript --------------------------------------
if [ ! -f "$SESSION_FILE" ]; then
  found=$(find "$PROJECTS_ROOT" -maxdepth 2 -name "$SID.jsonl" -type f 2>/dev/null | head -1)
  [ -n "$found" ] && SESSION_FILE=$found
fi
if [ -f "$SESSION_FILE" ]; then
  session_sha=$(sha256sum "$SESSION_FILE" | cut -d' ' -f1)
  session_bound=true
else
  session_sha=""
  session_bound=false
fi

# ---- stage the final diff BEFORE the oracle touches the tree ---------------------
git -C "$WT" diff "$FIXTURE_SHA" > "$A/diff.patch" 2>>"$A/setup.log" || { rm -f "$A/diff.patch"; echo "DIFF-FAILED" >> "$A/setup.log"; }
git -C "$WT" status --porcelain > "$A/status.porcelain" 2>>"$A/setup.log"
git -C "$WT" ls-files --others --exclude-standard > "$A/untracked.txt" 2>/dev/null
if [ -s "$A/untracked.txt" ]; then
  ( cd "$WT" && tr '\n' '\0' < "$A/untracked.txt" | xargs -0 -r sha256sum ) > "$A/untracked-sha256.txt" 2>/dev/null
fi
( cd "$WT" && find test bin/fan-test -type f -print0 | sort -z | xargs -0 sha256sum ) > "$A/guard-after.txt"
if cmp -s "$A/guard-before.txt" "$A/guard-after.txt"; then guard_match=true; else guard_match=false; fi

# ---- attribution: model id and every tool call, from the session transcript -------
python3 "$HERE/extract_attribution.py" \
  --arm "$A" --session "$SESSION_FILE" --run-log "$A/run.log" \
  --session-id "$SID" --requested-model "$MODEL" \
  > "$A/attribution.log" 2>&1
attrib_rc=$?

# canonical byte identity, his additional diagnostic (never the acceptance itself)
CANON="$ORACLE_FIX/canonical-$ORACLE_N"
if [ -d "$CANON/src" ] && diff -rq "$CANON/src" "$WT/src" >/dev/null 2>&1; then
  canonical_src_match=true
else
  canonical_src_match=false
fi

# ---- Astra's SAME six-check acceptance oracle, read-only against the clone --------
FAN_BASE="$FIXTURE_SHA" bash "$ORACLE" "$WT" "$ORACLE_N" "$ORACLE_FIX" > "$A/oracle.log" 2>&1
oracle_rc=$?
oracle_verdict=$(grep -E '^rescore-FAN: (6/6 checks passed|FAILED)' "$A/oracle.log" | tail -1)

OPUS_ID="$ID" OPUS_CELL="$CELL" OPUS_REP="$REP" OPUS_CLI="$CLI_VERSION" \
OPUS_CALLER_PATH="$CALLER_BIN" OPUS_FIX_SRC="$FIXTURE_SRC" OPUS_BASE="$FIXTURE_SHA" \
OPUS_FROZEN_PROMPT="$BASE_PROMPT" OPUS_URL="$MCP_URL" OPUS_SID="$SID" \
OPUS_SESSION_FILE="$SESSION_FILE" OPUS_SESSION_BOUND="$session_bound" \
OPUS_SESSION_SHA="$session_sha" OPUS_DRIVER_RC="$driver_rc" OPUS_ATTRIB_RC="$attrib_rc" \
OPUS_GUARD="$guard_match" OPUS_ORACLE_PATH="$ORACLE" OPUS_ORACLE_SHA="$ORACLE_SHA" \
OPUS_ORACLE_FIXDIR="$ORACLE_FIX" OPUS_ORACLE_RC="$oracle_rc" OPUS_VERDICT="$oracle_verdict" \
OPUS_MODEL_REQ="$MODEL" OPUS_ARMJSON="$A/arm.json" OPUS_ARMDIR="$A" \
OPUS_CPUS="$CPUS" OPUS_PROMPT_PATH="$A/prompt.txt" OPUS_WT="$WT" \
OPUS_SERVER_SHA_V="$SERVER_SHA" OPUS_READY_V="$READY" OPUS_HERE="$HERE" \
OPUS_ADAPTER_START="$adapter_start" OPUS_ADAPTER_LOAD_START="$adapter_load_start" \
OPUS_DRIVER_START="$mono_start" OPUS_DRIVER_END="$mono_end" \
OPUS_LOAD_START="$load_start" OPUS_LOAD_END="$load_end" OPUS_UTC_START="$utc_start" \
OPUS_CANONICAL_MATCH="$canonical_src_match" \
python3 "$HERE/write_arm_json.py"

echo "--- $ID rc=$driver_rc wall=$(grep task_wall_s "$A/task-wall.txt" | cut -d= -f2)s session_bound=$session_bound oracle_rc=$oracle_rc"
echo "--- oracle: ${oracle_verdict:-MISSING}"
[ "$session_bound" = true ] || { echo "OPUS-ARM :unverified — no session transcript bound for $SID" >&2; exit 3; }
exit 0
