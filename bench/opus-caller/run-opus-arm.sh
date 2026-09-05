#!/usr/bin/env bash
# run-opus-arm.sh <cell:N|T|O> <rep> [prepare|launch|all] — ONE Claude-Opus arm of
# Astra's frozen 21-owner alias-migration comparison.
#
# Every measured factor Astra froze is used HERE BY PATH AND SHA, never copied and
# never re-specified.  What differs is only the caller: `claude -p` on the seat's
# subscription instead of `codex exec`, and therefore a different attribution path.
# See docs/observations/2026-09-05-opus-caller-harness.md, and his NO-GO review at
# docs/observations/2026-09-05-opus-caller-harness-review-astra-NO-GO.md.
#
# TWO PHASES, because a tool arm's server must be bound to a worktree that ALREADY
# EXISTS (his blocker 1: round two started the server against a clone the arm had not
# made yet, and then the arm refused the directory the parent had created):
#
#   prepare   make the arm dir and the clone, take the protected inventory, compose
#             the prompt, write the MCP config.  Refuses an existing arm dir.
#   launch    attest the server (T/O), run the caller, freeze the diff, attribute,
#             then run the oracle.  Refuses unless `prepare` finished and this arm
#             has not already been launched.
#   all       prepare then launch (the default; correct for N, and for a hand-run T
#             whose server is already up against a prepared clone).
#
# EXIT CODES ARE TERMINAL OUTCOMES (his blocker 4): 0 accepted; 2 refused before or
# during setup; 3 attribution :unverified; 4 the oracle did not accept; 5 the caller
# itself failed.  A non-zero exit stops the cohort.
#
# Nothing here contacts 7888, 7890, 7894, 7895, 8171, 8173, 8174, 8175 or 8300-8339.
set -uo pipefail

die() { echo "OPUS-ARM REFUSED: $*" >&2; exit 2; }
mono() { cut -d' ' -f1 /proc/uptime; }
loadavg() { cat /proc/loadavg; }

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
CPUS=${OPUS_CPUS:-12,13}
TASKSET=${OPUS_TASKSET:-/usr/bin/taskset}
POLICY=${OPUS_POLICY_BIN:-$HERE/astra_policy.py}
LOAD_SAMPLE_S=${OPUS_LOAD_SAMPLE_S:-5}
LOAD_CEILING=${OPUS_LOAD_CEILING:-10}
# The frozen prompt files are IMMUTABLE ROUTE INPUTS and are bound by sha, not by
# existence (his review: "a prompt is hashed only after composition, without
# comparison to its frozen selected-route hash").
EXPECT_NATIVE_SHA=${OPUS_EXPECT_NATIVE_SHA:-409e71963a9522773ddf32f6dd923f9e6de4cbc5b4b84b84cddaeb05710efadd}
EXPECT_TOOL_SHA=${OPUS_EXPECT_TOOL_SHA:-150d072777421ee1ca90da62690205f161d33e552f305e7320c48e86a3e4ce1f}
EXPECT_COMMON_SHA=${OPUS_EXPECT_COMMON_SHA:-e5fafb6e1722272c18b72b5730f6bffbd3bf4b6bb35619b98df7fd803b85b6e9}
EXPECT_MANIFEST_SHA=${OPUS_EXPECT_MANIFEST_SHA:-8d08871bf38fc06d6938eed305e2bc9248eaefac527c5d208505f5cca8b273d8}
EXPECT_PROFILE_SHA=${OPUS_EXPECT_PROFILE_SHA:-8f70e1c73630bcf82488c6eb51136f370126f151fecacb11fe33d5b80ae90d3c}

CELL=${1:-}; REP=${2:-}; PHASE=${3:-all}
[ -n "$CELL" ] && [ -n "$REP" ] || { echo "usage: run-opus-arm.sh <N|T|O> <rep> [prepare|launch|all]" >&2; exit 64; }
case "$CELL" in N|T|O) ;; *) die "unknown cell '$CELL' (want N, T or O)";; esac
case "$PHASE" in prepare|launch|all) ;; *) die "unknown phase '$PHASE'";; esac
[[ $REP =~ ^[0-9]{1,3}$ ]] || die "rep '$REP' is not 1-3 digits"

# ---- the run gate: nothing launches, and nothing is contacted, without allocation --
[ "${RUNTIME_ALLOWED:-0}" = 1 ] || die "RUNTIME_ALLOWED is not 1 — this harness is preparation-only until runtime is allocated"
# THE QUIET WINDOW IS OWNED, NOT MERELY ABSENT.
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

adapter_start=$(mono); adapter_load_start=$(loadavg)

sha_of() { sha256sum "$1" | cut -d' ' -f1; }
# bind_sha <label> <path> <expected> — an immutable input, bound by SHA.  The result
# lands in $BOUND_SHA rather than on stdout: inside `$( )` a `die` would only kill the
# subshell, and a refusal that does not stop the arm is not a refusal.
BOUND_SHA=""
bind_sha() {
  local actual
  actual=$(sha256sum "$2" 2>/dev/null | cut -d" " -f1)
  [ -n "$actual" ] || die "cannot hash $1 at $2"
  [ "$actual" = "$3" ] || die "$1 sha mismatch: $2 is $actual, pinned $3"
  BOUND_SHA=$actual
}

# ================================ PHASE: prepare ==================================
do_prepare() {
  [ -e "$A" ] && die "arm directory already exists: $A (a run identity is used ONCE; pick a new rep)"

  [ -d "$FIXTURE_SRC/.git" ] || die "fixture source is not a git repo: $FIXTURE_SRC"
  local src_head; src_head=$(git -C "$FIXTURE_SRC" rev-parse HEAD 2>/dev/null) || die "cannot read fixture HEAD at $FIXTURE_SRC"
  [ "$src_head" = "$FIXTURE_SHA" ] || die "fixture sha mismatch: $FIXTURE_SRC is at $src_head, pinned base is $FIXTURE_SHA"
  [ -f "$ORACLE" ] || die "acceptance oracle missing: $ORACLE"
  bind_sha "oracle" "$ORACLE" "$ORACLE_SHA"; ORACLE_ACTUAL=$BOUND_SHA
  [ -f "$ORACLE_FIX/manifest-$ORACLE_N.edn" ] || die "oracle manifest missing: $ORACLE_FIX/manifest-$ORACLE_N.edn"
  [ -d "$ORACLE_FIX/canonical-$ORACLE_N" ] || die "oracle canonical missing: $ORACLE_FIX/canonical-$ORACLE_N"
  # the oracle's FIXTURES are immutable inputs too: the executable's sha binds the
  # checker, not the manifest and canonical tree it judges against (his review).
  bind_sha "oracle manifest" "$ORACLE_FIX/manifest-$ORACLE_N.edn" "$EXPECT_MANIFEST_SHA"; MANIFEST_ACTUAL=$BOUND_SHA
  CANONICAL_DIGEST=$(cd "$ORACLE_FIX/canonical-$ORACLE_N" && find . -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)

  case "$CELL" in
    N) BASE_PROMPT="$PROMPT_DIR/fanout-native.txt"; EXPECT_PROMPT_SHA=$EXPECT_NATIVE_SHA;;
    T) BASE_PROMPT="$PROMPT_DIR/fanout-tool.txt";   EXPECT_PROMPT_SHA=$EXPECT_TOOL_SHA;;
    O) BASE_PROMPT="$PROMPT_DIR/fanout-common.txt"; EXPECT_PROMPT_SHA=$EXPECT_COMMON_SHA;;
  esac
  [ -f "$BASE_PROMPT" ] || die "frozen prompt missing: $BASE_PROMPT"
  bind_sha "frozen $CELL prompt" "$BASE_PROMPT" "$EXPECT_PROMPT_SHA"; FROZEN_PROMPT_SHA=$BOUND_SHA

  if [ "$CELL" = N ]; then
    [ -z "${OPUS_MCP_URL:-}" ] || die "native arm cannot carry an MCP URL"
  else
    [ -n "${OPUS_MCP_URL:-}" ] || die "cell $CELL requires OPUS_MCP_URL"
    [[ ${OPUS_MCP_URL} =~ ^http://127\.0\.0\.1:(8[3][4-7][0-9])(/[A-Za-z0-9._/-]*)?$ ]] \
      || die "MCP URL must be loopback HTTP on this cohort's own band 8340-8379 (got '${OPUS_MCP_URL}')"
  fi

  [[ $CPUS =~ ^[0-9]+(-[0-9]+)?(,[0-9]+(-[0-9]+)?)*$ ]] || die "invalid CPU list '$CPUS'"
  [ -x "$TASKSET" ] || die "taskset not executable at $TASKSET — the same-cores doctrine cannot be honoured"

  mkdir -p "$A" || die "cannot create $A"
  [ -x /home/forge/bin/trust-dir ] && /home/forge/bin/trust-dir "$A" >/dev/null 2>&1

  git clone --quiet --no-hardlinks "$FIXTURE_SRC" "$WT" >>"$A/setup.log" 2>&1 \
    || die "clone failed (see $A/setup.log)"
  git -C "$WT" checkout --quiet --detach "$FIXTURE_SHA" >>"$A/setup.log" 2>&1 \
    || die "cannot detach clone at $FIXTURE_SHA"
  local wt_head; wt_head=$(git -C "$WT" rev-parse HEAD)
  [ "$wt_head" = "$FIXTURE_SHA" ] || die "clone HEAD $wt_head != pinned $FIXTURE_SHA"
  [ -z "$(git -C "$WT" status --porcelain)" ] || die "fresh clone is not clean"
  for need in src test bin/fan-test; do
    [ -e "$WT/$need" ] || die "fixture lacks $need"
  done
  echo "$FIXTURE_SHA" > "$A/base.sha"

  # the selected verified PROFILE is an immutable input and is inside the guard
  if [ -f "$WT/.clj-surgeon.edn" ]; then
    bind_sha "verification profile" "$WT/.clj-surgeon.edn" "$EXPECT_PROFILE_SHA"; PROFILE_SHA=$BOUND_SHA
  else
    PROFILE_SHA=""
  fi

  # protected inventory through HIS snapshot: bytes AND mode, symlinks refused
  python3 "$HERE/guard.py" snapshot "$WT" "$A/guard-before.json" \
    || die "protected inventory refused (see guard.py output)"

  # ---- compose the prompt: frozen cell text + the identical caller stanza ---------
  {
    cat "$BASE_PROMPT"
    cat "$HERE/prompts/claude-caller-common.txt"
    case "$CELL" in
      T) cat "$HERE/prompts/T-tooling-claude.txt"; printf '\nMCP server URL: %s\n' "$OPUS_MCP_URL";;
      O) cat "$HERE/prompts/O-tooling.txt";        printf '\nMCP server URL: %s\n' "$OPUS_MCP_URL";;
    esac
  } > "$A/prompt.txt"

  # ---- MCP: EXPLICIT ABSENCE for N, not merely a refused variable (his blocker 3) --
  if [ "$CELL" = N ]; then
    printf '{"mcpServers":{}}\n' > "$A/mcp.json"
  else
    printf '{"mcpServers":{"clj-surgeon":{"type":"http","url":"%s"}}}\n' "$OPUS_MCP_URL" > "$A/mcp.json"
  fi

  python3 - "$A/prepared.json" <<PYPREP
import json, sys
json.dump({"cell": "$CELL", "rep": "$REP", "worktree": "$WT",
           "base": "$FIXTURE_SHA",
           "frozen_prompt": "$BASE_PROMPT", "frozen_prompt_sha256": "$FROZEN_PROMPT_SHA",
           "composed_prompt_sha256": "$(sha_of "$A/prompt.txt")",
           "oracle": "$ORACLE", "oracle_sha256": "$ORACLE_ACTUAL",
           "oracle_manifest_sha256": "$MANIFEST_ACTUAL",
           "oracle_canonical_tree_sha256": "$CANONICAL_DIGEST",
           "verification_profile_sha256": "$PROFILE_SHA" or None,
           "mcp_url": "${OPUS_MCP_URL:-}" or None,
           "mcp_config_mode": "explicit-empty" if "$CELL" == "N" else "explicit-server",
           "cpus": "$CPUS"}, open(sys.argv[1], "w"), indent=2, sort_keys=True)
PYPREP
  echo "--- $ID prepared: clone at $FIXTURE_SHA, prompt $(wc -c < "$A/prompt.txt") B, mcp=$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcp_config_mode"])' "$A/prepared.json")"
}

# ================================ PHASE: launch ===================================
do_launch() {
  [ -f "$A/prepared.json" ] || die "arm $ID has not been prepared (run the prepare phase first)"
  [ -f "$A/arm.json" ] && die "arm $ID has already been launched (a run identity is used ONCE)"
  [ -d "$WT" ] || die "prepared arm has no worktree at $WT"

  local CALLER_BIN CLI_VERSION
  CALLER_BIN=$(command -v "$CALLER") || die "caller '$CALLER' not on PATH"
  CLI_VERSION=$("$CALLER_BIN" --version 2>&1 | head -1) || die "cannot read caller version"

  # ---- COMPLETE server attestation for T/O: ready.json + the server's OWN ready.edn
  # ---- + pid birth (pid/start-ticks/boot-id) + the actual checkout HEAD -----------
  READY=${OPUS_READY:-}; SERVER_SHA=${OPUS_SERVER_SHA:-}
  if [ "$CELL" != N ]; then
    [ -n "$READY" ] && [ -f "$READY" ] || die "cell $CELL requires OPUS_READY (the parent's server ready JSON)"
    [ -n "$SERVER_SHA" ] || die "cell $CELL requires OPUS_SERVER_SHA"
    local READY_EDN=${OPUS_READY_EDN:-$A/server/ready.edn}
    local SPAWNED=${OPUS_SPAWNED:-$A/server/spawned.pid}
    if ! python3 "$POLICY" attest-server --ready "$READY" --ready-edn "$READY_EDN" \
          --spawned "$SPAWNED" --url "${OPUS_MCP_URL:?}" --server-sha "$SERVER_SHA" \
          --worktree "$WT" > "$A/server-attest.json" 2>"$A/server-attest.err"; then
      die "server attestation rejected: $(head -1 "$A/server-attest.err")"
    fi
  elif [ -n "$READY$SERVER_SHA" ]; then
    die "native arm cannot carry server evidence"
  fi

  local SID ESCAPED SESSION_FILE
  SID=$(cat /proc/sys/kernel/random/uuid)
  ESCAPED=$(printf '%s' "$(readlink -m "$WT")" | sed 's#[^A-Za-z0-9]#-#g')
  SESSION_FILE="$PROJECTS_ROOT/$ESCAPED/$SID.jsonl"

  # --mcp-config is passed for EVERY cell.  N gets an explicitly EMPTY server map,
  # so "no MCP" is a stated configuration rather than an unstated hope, and
  # --strict-mcp-config keeps the seat's own servers out of every arm.
  local DRIVER_CMD=("$TASKSET" -c "$CPUS" "$CALLER_BIN" -p --model "$MODEL"
              --dangerously-skip-permissions
              --session-id "$SID" --output-format stream-json --verbose
              --add-dir "$WT" --mcp-config "$A/mcp.json" --strict-mcp-config
              --disallowedTools Task Skill ToolSearch WebFetch WebSearch SendMessage
                  ListAgents Monitor TaskOutput TaskStop NotebookEdit Artifact
                  EnterWorktree ExitWorktree Workflow)
  printf '%s\n' "${DRIVER_CMD[@]}" > "$A/command.txt"

  # ---- a load SAMPLER, not two endpoints (his follow-up b) ----------------------
  : > "$A/load.jsonl"
  ( while :; do
      printf '{"t_monotonic":%s,"utc":"%s","phase":"%s","loadavg":"%s"}\n' \
        "$(mono)" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$(cat "$A/.phase" 2>/dev/null || echo unknown)" \
        "$(loadavg)" >> "$A/load.jsonl"
      sleep "$LOAD_SAMPLE_S"
    done ) &
  local SAMPLER=$!
  trap 'kill '"$SAMPLER"' 2>/dev/null' EXIT

  echo driver > "$A/.phase"
  local load_start mono_start utc_start
  load_start=$(loadavg); mono_start=$(mono); utc_start=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  ( cd "$WT" && exec timeout --signal=TERM --kill-after=30 "$MAX_WALL" \
      "${DRIVER_CMD[@]}" ) < "$A/prompt.txt" > "$A/run.log" 2>&1
  local driver_rc=$?
  local mono_end utc_end load_end
  mono_end=$(mono); utc_end=$(date -u +%Y-%m-%dT%H:%M:%SZ); load_end=$(loadavg)

  echo freeze > "$A/.phase"
  # ---- bind the run to its session transcript ----------------------------------
  if [ ! -f "$SESSION_FILE" ]; then
    local found; found=$(find "$PROJECTS_ROOT" -maxdepth 2 -name "$SID.jsonl" -type f 2>/dev/null | head -1)
    [ -n "$found" ] && SESSION_FILE=$found
  fi
  local session_sha="" session_bound=false
  if [ -f "$SESSION_FILE" ]; then
    session_sha=$(sha_of "$SESSION_FILE"); session_bound=true
  fi

  # ---- STAGE the final diff, and mean it (his review: round two staged nothing) --
  # `git add -- .` stages tracked AND untracked content under the worktree root, so
  # the cached diff is a COMPLETE replay artifact including new files.  (`-A` is not
  # used; the pathspec does the same job under the tree the arm owns.)
  # the untracked inventory is taken BEFORE staging: once staged, `ls-files --others`
  # correctly stops listing the file, and "which files the caller created" is still a
  # fact worth keeping separately from the patch.
  git -C "$WT" ls-files --others --exclude-standard > "$A/untracked.txt" 2>/dev/null
  git -C "$WT" add -- . >>"$A/setup.log" 2>&1 || echo "ADD-FAILED" >> "$A/setup.log"
  if ! git -C "$WT" diff --cached --binary "$FIXTURE_SHA" > "$A/diff.patch" 2>>"$A/setup.log"; then
    rm -f "$A/diff.patch"; echo "DIFF-FAILED" >> "$A/setup.log"
  fi
  git -C "$WT" status --porcelain > "$A/status.porcelain" 2>>"$A/setup.log"
  python3 "$HERE/guard.py" snapshot "$WT" "$A/guard-after.json" 2>>"$A/setup.log"
  local guard_match=false
  python3 "$HERE/guard.py" compare "$A/guard-before.json" "$A/guard-after.json" \
    > "$A/guard.log" 2>&1 && guard_match=true

  echo attribution > "$A/.phase"
  local expect_no_mcp=()
  [ "$CELL" = N ] && expect_no_mcp=(--expect-no-mcp)
  python3 "$HERE/extract_attribution.py" \
    --arm "$A" --session "$SESSION_FILE" --run-log "$A/run.log" \
    --session-id "$SID" --requested-model "$MODEL" "${expect_no_mcp[@]}" \
    > "$A/attribution.log" 2>&1
  local attrib_rc=$?

  # THE ADAPTER WALL ENDS HERE — after freeze and attestation — because that is what
  # `adapter_wall_scope` says it measures (his follow-up c: round two ended it at
  # caller exit and labelled it otherwise).
  local mono_attested load_attested
  mono_attested=$(mono); load_attested=$(loadavg)

  echo acceptance > "$A/.phase"
  local CANON="$ORACLE_FIX/canonical-$ORACLE_N" canonical_src_match=false
  [ -d "$CANON/src" ] && diff -rq "$CANON/src" "$WT/src" >/dev/null 2>&1 && canonical_src_match=true
  local mono_oracle_start; mono_oracle_start=$(mono)
  FAN_BASE="$FIXTURE_SHA" "$TASKSET" -c "$CPUS" bash "$ORACLE" "$WT" "$ORACLE_N" "$ORACLE_FIX" > "$A/oracle.log" 2>&1
  local oracle_rc=$?
  local mono_oracle_end; mono_oracle_end=$(mono)
  local oracle_verdict; oracle_verdict=$(grep -E '^rescore-FAN: (6/6 checks passed|FAILED)' "$A/oracle.log" | tail -1)

  # the human-readable wall record, alongside the machine receipts
  {
    echo "monotonic_start_s=$mono_start"
    echo "monotonic_end_s=$mono_end"
    echo "task_wall_s=$(awk -v a="$mono_start" -v b="$mono_end" 'BEGIN{printf "%.3f", b-a}')"
    echo "attested_end_s=$mono_attested"
    echo "adapter_wall_s=$(awk -v a="$adapter_start" -v b="$mono_attested" 'BEGIN{printf "%.3f", b-a}')"
    echo "acceptance_wall_s=$(awk -v a="$mono_oracle_start" -v b="$mono_oracle_end" 'BEGIN{printf "%.3f", b-a}')"
    echo "utc_start=$utc_start"
    echo "utc_end=$utc_end"
    echo "driver_rc=$driver_rc"
    echo "max_wall_s=$MAX_WALL"
    echo "load_start=$load_start"
    echo "load_end=$load_end"
    echo "load_samples=$(wc -l < "$A/load.jsonl")"
    echo "wall_scope=see adapter-result.json timing; task_wall_s is the CALLER only"
  } > "$A/task-wall.txt"

  echo done > "$A/.phase"
  kill "$SAMPLER" 2>/dev/null; trap - EXIT

  local resolved_model
  resolved_model=$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));m=d.get("models_in_transcript") or [];print(m[0] if len(m)==1 else "")' "$A/attribution.json" 2>/dev/null)

  OPUS_ID="$ID" OPUS_CELL="$CELL" OPUS_REP="$REP" OPUS_CLI="$CLI_VERSION" \
  OPUS_CALLER_PATH="$CALLER_BIN" OPUS_FIX_SRC="$FIXTURE_SRC" OPUS_BASE="$FIXTURE_SHA" \
  OPUS_URL="${OPUS_MCP_URL:-}" OPUS_SID="$SID" \
  OPUS_SESSION_FILE="$SESSION_FILE" OPUS_SESSION_BOUND="$session_bound" \
  OPUS_SESSION_SHA="$session_sha" OPUS_DRIVER_RC="$driver_rc" OPUS_ATTRIB_RC="$attrib_rc" \
  OPUS_GUARD="$guard_match" OPUS_ORACLE_PATH="$ORACLE" OPUS_ORACLE_SHA="$ORACLE_SHA" \
  OPUS_ORACLE_FIXDIR="$ORACLE_FIX" OPUS_ORACLE_RC="$oracle_rc" OPUS_VERDICT="$oracle_verdict" \
  OPUS_MODEL_REQ="$MODEL" OPUS_MODEL_RESOLVED="$resolved_model" \
  OPUS_ARMJSON="$A/arm.json" OPUS_ARMDIR="$A" OPUS_CPUS="$CPUS" \
  OPUS_PROMPT_PATH="$A/prompt.txt" OPUS_WT="$WT" \
  OPUS_SERVER_SHA_V="$SERVER_SHA" OPUS_READY_V="$READY" OPUS_HERE="$HERE" \
  OPUS_ADAPTER_START="$adapter_start" OPUS_ADAPTER_LOAD_START="$adapter_load_start" \
  OPUS_DRIVER_START="$mono_start" OPUS_DRIVER_END="$mono_end" \
  OPUS_ATTESTED_END="$mono_attested" OPUS_ATTESTED_LOAD="$load_attested" \
  OPUS_ORACLE_START="$mono_oracle_start" OPUS_ORACLE_END="$mono_oracle_end" \
  OPUS_LOAD_START="$load_start" OPUS_LOAD_END="$load_end" OPUS_UTC_START="$utc_start" \
  OPUS_UTC_END="$utc_end" OPUS_CANONICAL_MATCH="$canonical_src_match" \
  OPUS_LOAD_CEILING="$LOAD_CEILING" \
  python3 "$HERE/write_arm_json.py"

  echo "--- $ID driver_rc=$driver_rc session_bound=$session_bound attrib_rc=$attrib_rc oracle_rc=$oracle_rc"
  echo "--- oracle: ${oracle_verdict:-MISSING}"

  # ---- TERMINAL OUTCOME PROPAGATION (his blocker 4) ----------------------------
  if [ "$driver_rc" -ne 0 ]; then
    echo "OPUS-ARM FAILED: the caller exited $driver_rc" >&2; exit 5
  fi
  if [ "$session_bound" != true ] || [ "$attrib_rc" -ne 0 ]; then
    echo "OPUS-ARM :unverified — attribution rc=$attrib_rc (see $A/attribution.log)" >&2; exit 3
  fi
  if [ "$oracle_rc" -ne 0 ]; then
    echo "OPUS-ARM NOT ACCEPTED: oracle rc=$oracle_rc — ${oracle_verdict:-no verdict line}" >&2; exit 4
  fi
  exit 0
}

case "$PHASE" in
  prepare) do_prepare;;
  launch)  do_launch;;
  all)     do_prepare && do_launch;;
esac
