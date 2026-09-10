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
oracle_init "${1:?usage: oracle.sh <worktree>}" "src/clj_surgeon/mission_cli.clj" "$TASKDIR"

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

want_fact vars 'occupant-sizes' "no occupant-sizes helper was defined"
want_re '\(defn-\s+occupant-sizes' "the helper is not private"
want_adjacent occupant-sizes ledger-of "the helper is not immediately above ledger-of"
want_text '(mission/write-index! state-dir missions)' "ledger-of was modified; it is not part of this task"
want_fact vars 'ledger-of' "ledger-of must survive"
want_re ':destination' "the helper does not consider every key a refusal can name a file under"
want_re 'zero\?' "nothing distinguishes the zero-byte case"
preserve_only --changed '' --added 'occupant-sizes' --gap-changed ''
verdict "I-02-mission-cli-admitted-profiles: intent met, and every other byte of src/clj_surgeon/mission_cli.clj is where it was"
