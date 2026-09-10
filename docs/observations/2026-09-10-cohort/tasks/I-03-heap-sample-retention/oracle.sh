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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/memory/heap.clj" "$TASKDIR"

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

want_fact vars 'sample-retention!' "no sample-retention! checkpoint function was defined"
want_fact vars 'retention-peak' "no private peak state was defined"
want_re '\(def \^:private retention-peak' "the peak state is not private"
want_adjacent to-mb retention-peak "the new state is not directly after to-mb"
want_adjacent retention-peak 'sample-retention!' "the checkpoint function is not beside its state"
want_adjacent 'sample-retention!' measure "the new pair is not directly before measure"
want_re '\(System/gc\)' "nothing forces a collection"
want_re '\(swap! retention-peak max' "the highest reading is never remembered"
want_fact vars 'measure' "measure must survive untouched"
want_fact vars 'emit-receipt!' "emit-receipt! must survive untouched"
preserve_only --changed '' --added 'retention-peak,sample-retention!' --gap-changed ''
verdict "I-03-heap-sample-retention: intent met, and every other byte of test/clj_surgeon/memory/heap.clj is where it was"
