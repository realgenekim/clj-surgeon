#!/usr/bin/env bash
# rescore-FAN.sh — the six-check mechanical acceptance for the FAN family
# (docs/observations/2026-09-02-slope-spec-sl1.md, "Acceptance"; named as the E3-P gate
# in docs/observations/2026-09-04-e3-e6-prestaged.md B.2 clause 5 and A.7).
#
#   rescore-FAN.sh <worktree> <N> [fixtures-dir]
#
# fixtures-dir holds canonical-<N>/ and manifest-<N>.edn; it defaults to $FAN_FIXTURES
# and then to /home/forge/tmp/arms/e3/fanout.  The base the diff is taken against is
# $FAN_BASE, then <worktree>/../base.sha, then the worktree's root commit.
#
# The six checks:
#   1 file set      changed files == the manifest's target set exactly, no extras
#   2 form equality every changed file parses and equals canonical modulo whitespace,
#                   with comments, metadata and #_ discards present and in place
#   3 protected     every decoy region is byte-present, sha256 from the manifest
#   4 load          one process requires all 100 namespaces, zero errors
#   5 behaviour     the generated suite at base count with an empty failure set
#   6 residue       no acid.fanout.store / old-alias-qualified use left, and the alias
#                   each file introduced is the policy's and shadows nothing
#
# Every check prints a COMPUTED number.  A step whose evidence is missing is a FAIL
# naming what was missing -- never a silent zero, never a verdict word (the
# `verdict-label-was-a-noun` defect this program has already paid for once).
set -uo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
WT=${1:-}; N=${2:-}; FIX=${3:-${FAN_FIXTURES:-/home/forge/tmp/arms/e3/fanout}}
[ -n "$WT" ] && [ -n "$N" ] || { echo "usage: rescore-FAN.sh <worktree> <N> [fixtures-dir]" >&2; exit 64; }
[ -d "$WT" ] || { echo "rescore-FAN: FAIL no worktree at $WT" >&2; exit 2; }

MANIFEST="$FIX/manifest-$N.edn"
CANON="$FIX/canonical-$N"
for need in "$MANIFEST" "$CANON"; do
  [ -e "$need" ] || { echo "rescore-FAN: FAIL fixture missing: $need" >&2; exit 2; }
done

# --- git binary resolution for the base-sha fallback below, mirroring -------------
# fan_check.clj's resolve-git: FAN_GIT if it names an executable file, else the
# first executable file from a fixed absolute candidate list -- NEVER PATH (round-4
# review, fanout4-opus-review.md finding 1: a PATH-shimmed `rev-list` reached a false
# 6/6 through this exact fallback).  Only invoked when neither FAN_BASE nor
# ../base.sha already supplied a base.
FAN_GIT_CANDIDATES=(/usr/bin/git /bin/git /usr/local/bin/git /opt/homebrew/bin/git)
resolve_fan_git() {
  local -a cands
  if [ -n "${FAN_GIT:-}" ]; then cands=("$FAN_GIT"); else cands=("${FAN_GIT_CANDIDATES[@]}"); fi
  local c
  for c in "${cands[@]}"; do
    if [ -f "$c" ] && [ -x "$c" ]; then printf '%s\n' "$c"; return 0; fi
  done
  return 1
}

# --- the base the diff is taken against, VALIDATED as 40 hex on EVERY branch ------
# (finding 1a-c: a 7-char sha, the literal HEAD, and a non-zero rev-list all used to
# flow through unvalidated -- one shared check below now covers all three sources.)
BASE_FROM=""
if [ -n "${FAN_BASE:-}" ]; then
  BASE=$FAN_BASE; BASE_FROM="FAN_BASE"
elif [ -f "$WT/../base.sha" ]; then
  BASE=$(tr -d '[:space:]' < "$WT/../base.sha"); BASE_FROM="base.sha"
else
  BASE_FROM="rev-list"
  if FANGIT=$(resolve_fan_git); then
    REVLIST_ERR=$(mktemp)
    BASE=$("$FANGIT" -C "$WT" rev-list --max-parents=0 HEAD 2>"$REVLIST_ERR")
    revlist_rc=$?
    REVLIST_STDERR=$(cat "$REVLIST_ERR" 2>/dev/null); rm -f "$REVLIST_ERR"
    if [ $revlist_rc -ne 0 ]; then
      echo "rescore-FAN: FAIL base must be a 40-hex sha (got <rev-list exit=$revlist_rc stderr=${REVLIST_STDERR:-<empty>}>)" >&2
      exit 2
    fi
  else
    echo "rescore-FAN: FAIL base must be a 40-hex sha (got <no base supplied, and no git binary to fall back to -- FAN_GIT unset/not-executable, none of ${FAN_GIT_CANDIDATES[*]} is an executable file>)" >&2
    exit 2
  fi
fi
if ! [[ $BASE =~ ^[0-9a-f]{40}$ ]]; then
  echo "rescore-FAN: FAIL base must be a 40-hex sha (got ${BASE:-<empty>})" >&2
  exit 2
fi
echo "rescore-FAN: worktree=$WT n=$N base=$BASE base-from=$BASE_FROM fixtures=$FIX"

FAILED=()

# --- checks 1, 2, 3, 6 (structural; rewrite-clj) ----------------------------------
bb "$HERE/fan_check.clj" "$WT" "$MANIFEST" "$CANON" "$BASE"
structural_rc=$?
[ $structural_rc -eq 0 ] || FAILED+=("structural(1,2,3,6)")

# --- check 4: load ----------------------------------------------------------------
load_out=$(cd "$WT" && timeout 300 bb test/load_all.clj 2>&1)
load_rc=$?
load_count=$(printf '%s' "$load_out" | sed -n 's/.*LOAD-OK namespaces=\([0-9][0-9]*\).*/\1/p' | tail -1)
if [ $load_rc -eq 0 ] && [ -n "$load_count" ] && [ "$load_count" -eq 100 ]; then
  echo "CHECK 4 load: PASS namespaces=$load_count rc=$load_rc"
else
  echo "CHECK 4 load: FAIL rc=$load_rc namespaces=${load_count:-none} detail=$(printf '%s' "$load_out" | tail -3 | tr '\n' ' ')"
  FAILED+=("CHECK 4 load")
fi

# --- check 5: behaviour -----------------------------------------------------------
if [ -x "$WT/bin/fan-test" ]; then
  test_out=$(cd "$WT" && timeout 600 ./bin/fan-test 2>&1)
  test_rc=$?
  line=$(printf '%s' "$test_out" | grep -o 'FAN-TEST tests=[0-9]* assertions=[0-9]* failures=[0-9]* errors=[0-9]*' | tail -1)
  t_tests=$(printf '%s' "$line" | sed -n 's/.*tests=\([0-9]*\).*/\1/p')
  t_fail=$(printf '%s' "$line" | sed -n 's/.*failures=\([0-9]*\).*/\1/p')
  t_err=$(printf '%s' "$line" | sed -n 's/.*errors=\([0-9]*\).*/\1/p')
  if [ $test_rc -eq 0 ] && [ -n "$t_tests" ] && [ "$t_tests" -eq "$N" ] \
     && [ "$t_fail" = 0 ] && [ "$t_err" = 0 ]; then
    echo "CHECK 5 behaviour: PASS $line (base count=$N)"
  else
    echo "CHECK 5 behaviour: FAIL rc=$test_rc line='${line:-none}' expected tests=$N failures=0 errors=0 detail=$(printf '%s' "$test_out" | tail -3 | tr '\n' ' ')"
    FAILED+=("CHECK 5 behaviour")
  fi
else
  echo "CHECK 5 behaviour: FAIL no executable $WT/bin/fan-test"
  FAILED+=("CHECK 5 behaviour")
fi

# --- verdict, computed ------------------------------------------------------------
if [ ${#FAILED[@]} -eq 0 ]; then
  echo "rescore-FAN: 6/6 checks passed"
  exit 0
fi
echo "rescore-FAN: FAILED ${#FAILED[@]} group(s): ${FAILED[*]}"
exit 1
