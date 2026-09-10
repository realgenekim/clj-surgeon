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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/lane_manifest_test.clj" "$TASKDIR"

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

want_re '\(is \(= 1373 total\)' "the corpus total is not pinned at 1373"
deny_re '\(is \(= 1370 total\)' "the old total 1370 is still asserted"
want_re '\(is \(= 435 adopted\)' "the adopted-tests pin was changed; it is not part of this task"
want_re '\(is \(>= r1 865\)' "the round-one floor was changed; it is not part of this task"
want_text '(is (= total (+ r1 adopted))' "the closing arithmetic assertion was changed or removed"
python3 - "$CAND" <<'PY' || fail "the changed pin carries no reason beside it"
import sys
lines = open(sys.argv[1]).read().splitlines()
for i, l in enumerate(lines):
    if '(is (= 1373 total)' in l:
        above = [x.strip() for x in lines[max(0, i-8):i]]
        raise SystemExit(0 if any(x.startswith(';;') and '1373' in x for x in above) else 1)
raise SystemExit(1)
PY
preserve_only --changed 'the-corpus-only-ever-grows-and-the-arithmetic-is-shown' --added '' --gap-changed ''
verdict "T-04-lane-manifest-total: intent met, and every other byte of test/clj_surgeon/lane_manifest_test.clj is where it was"
