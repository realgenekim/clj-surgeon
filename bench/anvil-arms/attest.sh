#!/usr/bin/env bash
# attest.sh — A.4 of docs/observations/2026-09-04-e3-e6-prestaged.md.
#
#   attest.sh <ARMDIR> <arm> <port|-> <expected-server-sha>
#
# Writes <ARMDIR>/attest.json (canonical) and <ARMDIR>/attest.edn (the shape A.4
# names) BEFORE any driver launches, and exits 2 with ATTEST-MISMATCH when any
# fail-closed condition trips.  The caller must treat a non-zero exit as "the arm
# never ran": nothing downstream of this script may start.
#
# Every field this script cannot establish is written as the literal string
# "unverified" — never empty, never omitted.  A receipt whose attestation carries
# "unverified" in port_pid, server_sha or prompt_sha256 is :unverified, not success.
#
# Environment (all optional; each has a recorded default):
#   EXP RUNG SLOT GROUP      override the values derived from the ARMDIR basename
#   MODEL DRIVER             the exact model id / driver name for :model
#   RUNNER                   path to the runner script whose sha256 is :runner-sha256
#   MCP_URL                  the MCP url configured for this arm ("" for native arms)
#   BASE_SHA / BASE_SHA_FILE the pinned base ("" -> <ARMDIR>/base.sha)
#   PROMPT                   the prompt file ("" -> <ARMDIR>/prompt.md)
#   COHORT_PORTS             ports this apparatus owns (default "7907 7908 7909 7910")
set -uo pipefail

A=${1:?usage: attest.sh <ARMDIR> <arm> <port|-> <expected-server-sha>}
ARM=${2:?arm letter (N|T|F)}
PORT=${3:--}
EXPECTED_SERVER_SHA=${4:-}

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
UNV=unverified

# --- derive the run identity from the directory name, e.g. e3-P-T-1 --------------
BASENAME=$(basename "$A")
IFS='-' read -r d_exp d_rung d_arm d_slot <<<"$BASENAME"
EXP=${EXP:-${d_exp:-$UNV}}
RUNG=${RUNG:-${d_rung:-$UNV}}
SLOT=${SLOT:-${d_slot:-$UNV}}
GROUP=${GROUP:-1}
[ -n "${d_arm:-}" ] && [ "$d_arm" != "$ARM" ] && \
  echo "attest: warning: directory says arm=$d_arm, argument says arm=$ARM" >&2

MODEL=${MODEL:-$UNV}
DRIVER=${DRIVER:-$UNV}
RUNNER=${RUNNER:-$HERE/run-arm.sh}
MCP_URL=${MCP_URL:-}
PROMPT=${PROMPT:-$A/prompt.md}
BASE_SHA_FILE=${BASE_SHA_FILE:-$A/base.sha}
COHORT_PORTS=${COHORT_PORTS:-7907 7908 7909 7910}

mkdir -p "$A" || { echo "attest: cannot create $A" >&2; exit 2; }

# --- helpers ---------------------------------------------------------------------
# `field <cmd...>` prints the command's stdout (first line, trimmed) or "unverified".
field () {
  local out
  if out=$("$@" 2>/dev/null); then
    out=$(printf '%s' "$out" | head -n1 | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
    [ -n "$out" ] && { printf '%s' "$out"; return 0; }
  fi
  printf '%s' "$UNV"
}

sha_of () { [ -f "$1" ] && field bash -c "sha256sum \"\$1\" | cut -d' ' -f1" _ "$1" || printf '%s' "$UNV"; }
git_head () { [ -d "$1" ] && field git -C "$1" rev-parse HEAD || printf '%s' "$UNV"; }

# --- fields, all arms -------------------------------------------------------------
WORKTREE="$A/worktree"
[ -d "$WORKTREE" ] || WORKTREE=$UNV
WORKTREE_HEAD=$( [ "$WORKTREE" != "$UNV" ] && git_head "$WORKTREE" || printf '%s' "$UNV" )
if [ -n "${BASE_SHA:-}" ]; then BASE=$BASE_SHA
elif [ -f "$BASE_SHA_FILE" ]; then BASE=$(field cat "$BASE_SHA_FILE")
else BASE=$UNV; fi

PROMPT_SHA=$(sha_of "$PROMPT")
RUNNER_SHA=$(sha_of "$RUNNER")
ATTEST_SHA=$(sha_of "$HERE/attest.sh")
WATCH_SHA=$(sha_of "$HERE/watch.py")
SCORE_SHA=$(sha_of "$HERE/score.py")
MAKE_TARGETS=${MAKE_TARGETS:-$A/make-targets.json}
MAKE_TARGETS_SHA=$(sha_of "$MAKE_TARGETS")

# --- listener inventory: LOCAL LISTING ONLY, we contact nothing here ---------------
# `ss -ltn` lists; it does not connect.  We never curl a port outside COHORT_PORTS.
LISTENERS=$(ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | sort -un | tr '\n' ' ')
[ -n "$LISTENERS" ] || LISTENERS=$UNV

HEALTHZ=$UNV; PORT_PID=$UNV; READY_PID=$UNV; READY_PROJECT_ROOT=$UNV
SERVER_PROJECT_HEAD=$UNV; SERVER_CWD=$UNV; SERVER_SHA=$UNV
MCP_ABSENT_PROOF=$UNV
PORT_IN_RANGE=no
for p in $COHORT_PORTS; do [ "$PORT" = "$p" ] && PORT_IN_RANGE=yes; done

if [ "$ARM" = "N" ]; then
  # Native positive control: no MCP server may be configured for this arm, and none
  # of the ports THIS apparatus owns may be listening (a stale arm server is exactly
  # the contamination this check exists for).  Ports belonging to other seats are
  # listed, never probed, and never a reason to refuse a native arm.
  stale=""
  for p in $COHORT_PORTS; do
    case " $LISTENERS " in *" $p "*) stale="$stale $p";; esac
  done
  if [ -n "$MCP_URL" ]; then
    MCP_ABSENT_PROOF="FAILED: arm N was handed mcp_url=$MCP_URL"
  elif [ -n "$stale" ]; then
    MCP_ABSENT_PROOF="FAILED: cohort port(s)$stale are listening"
  else
    MCP_ABSENT_PROOF="ok: no mcp url configured; cohort ports ($COHORT_PORTS) show no listener"
  fi
else
  # Tool / free-choice arm: the server identity is READ FROM THE SERVER.
  if [ "$PORT_IN_RANGE" = yes ]; then
    HEALTHZ=$(field curl -fsS --max-time 5 "http://127.0.0.1:$PORT/healthz")
    PORT_PID=$(field bash -c \
      "ss -ltnp \"sport = :\$1\" 2>/dev/null | grep -o 'pid=[0-9]*' | head -n1 | cut -d= -f2" _ "$PORT")
    if [ -f "$A/server/ready.edn" ]; then
      READY_PID=$(field bash -c "sed -n 's/.*:pid \\([0-9][0-9]*\\).*/\\1/p' \"\$1\" | head -n1" _ "$A/server/ready.edn")
      READY_PROJECT_ROOT=$(field bash -c "sed -n 's/.*:project-root \"\\([^\"]*\\)\".*/\\1/p' \"\$1\" | head -n1" _ "$A/server/ready.edn")
    fi
    [ "$READY_PROJECT_ROOT" != "$UNV" ] && SERVER_PROJECT_HEAD=$(git_head "$READY_PROJECT_ROOT")
    # The server's OWN code: the cwd of the process that owns the port.  This is the
    # only identity readable from the running server itself (healthz reports the
    # served project, not the server's source), so it is what :expected-server-sha
    # is checked against.
    if [ "$PORT_PID" != "$UNV" ]; then
      SERVER_CWD=$(field readlink -f "/proc/$PORT_PID/cwd")
      [ "$SERVER_CWD" != "$UNV" ] && SERVER_SHA=$(git_head "$SERVER_CWD")
    fi
  fi
fi

# --- refusal evaluation + the write, in one place ---------------------------------
export A ARM PORT EXP RUNG SLOT GROUP MODEL DRIVER RUNNER MCP_URL PROMPT BASE \
       WORKTREE WORKTREE_HEAD PROMPT_SHA RUNNER_SHA ATTEST_SHA WATCH_SHA SCORE_SHA \
       MAKE_TARGETS MAKE_TARGETS_SHA \
       LISTENERS HEALTHZ PORT_PID READY_PID READY_PROJECT_ROOT SERVER_PROJECT_HEAD \
       SERVER_CWD SERVER_SHA MCP_ABSENT_PROOF EXPECTED_SERVER_SHA PORT_IN_RANGE UNV

python3 "$HERE/_attest_write.py"
rc=$?
if [ $rc -ne 0 ]; then
  echo "ATTEST-MISMATCH $A" >&2
  exit 2
fi
exit 0
