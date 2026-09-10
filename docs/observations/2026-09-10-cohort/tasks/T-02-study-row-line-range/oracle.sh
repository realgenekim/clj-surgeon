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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/mcp_study_test.clj" "$TASKDIR"

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

deny_text 'reader-cond?@37-39' "the hardcoded 37-39 line range is still pinned in the file"
deny_re 'reader-cond\?@[0-9]+-[0-9]+' "the assertion still pins a literal line range instead of computing it"
want_re ':line[ )]' "the rewritten assertion does not read the form's :line"
want_re ':end_line[ )]' "the rewritten assertion does not read the form's :end_line"
want_text 'reader-cond?@' "the assertion no longer names the reader-cond? row at all"
want_fact vars forms-text-carries-the-source-a-caller-asked-for "the test itself must survive"
preserve_only --changed 'forms-text-carries-the-source-a-caller-asked-for' --added '' --gap-changed ''
verdict "T-02-study-row-line-range: intent met, and every other byte of test/clj_surgeon/mcp_study_test.clj is where it was"
