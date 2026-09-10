#!/usr/bin/env bash
# oracle.sh <worktree>   ->  exit 0 GREEN / exit 1 RED, with the reason on stdout.
#
# Held out from every arm. Two independent hashers decide:
#   INTENT       clj-kondo's own analysis (--config '{:analysis true :output {:format :edn}}')
#                plus fixed-string assertions about the requested change
#   PRESERVATION a python3 bracket-matching per-top-level-form hasher: raw bytes
#                per form, the exact bytes BETWEEN forms, and every form's
#                ordinal position
# Neither can see the other's answer, and neither uses rewrite-clj.
set -uo pipefail
ORACLE_LIB=/var/tmp/forge/cohort-fx/curation/oracle-lib.sh
[ -f "$ORACLE_LIB" ] || { echo "RED: oracle library missing at $ORACLE_LIB"; exit 1; }
# shellcheck source=/dev/null
. "$ORACLE_LIB"

TASKDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
oracle_init "${1:?usage: oracle.sh <worktree>}" "src/clj_surgeon/mission_display.clj" "$TASKDIR"

require_sorted() {  # the repository's requires are alphabetical; keep them so
  python3 - "$CAND" <<'PY' || fail "the :require list is no longer in alphabetical order"
import sys, re
src = open(sys.argv[1]).read()
m = re.search(r'\(:require\s*(.*?)\)\s*(?:\(:import|\)\s*$|\)\n)', src, re.S)
if not m:
    raise SystemExit(0)
libs = re.findall(r'\[\s*([\w.\-]+)', m.group(1))
raise SystemExit(0 if libs == sorted(libs) else 1)
PY
}

want_fact vars 'decision-view' "no decision-view function was defined"
want_adjacent decision-view show-result "decision-view is not immediately above show-result"
want_re ':error_type' "the buried error type is never lifted"
want_re ':evidence' "the saved evidence is never consulted"
want_re 'map\?' "a non-map decision is not passed through untouched"
want_text '(:full opts) view' "show-result was modified; it is not part of this task"
want_fact vars 'show-result' "show-result must survive"
want_fact usages 'clj-surgeon.mission-display/command' "the clarified view offers no runnable command"
preserve_only --changed '' --added 'decision-view' --gap-changed ''
verdict "I-04-mission-display-decision-view: intent met, and every other byte of src/clj_surgeon/mission_display.clj is where it was"
