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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/outline_memory_test.clj" "$TASKDIR"

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

want_re '^;; @spec MCP-OP-MEM-015$' "the spec marker comment is not present on its own line"
python3 - "$CAND" <<'PY' || fail "the marker is not on the line immediately above outline-of-one-file-allocates-within-its-ceiling"
import sys
lines = open(sys.argv[1]).read().splitlines()
for i, l in enumerate(lines):
    if l.startswith('(deftest outline-of-one-file-allocates-within-its-ceiling'):
        raise SystemExit(0 if i and lines[i-1].strip() == ';; @spec MCP-OP-MEM-015' else 1)
raise SystemExit(1)
PY
[ "$(grep -c '@spec MCP-OP-MEM-015' "$CAND")" = 1 ] || fail "the marker appears more than once"
want_fact vars outline-of-one-file-allocates-within-its-ceiling "the marked test itself must survive"
preserve_only --changed '' --added '' --gap-changed 'outline-of-one-file-allocates-within-its-ceiling'
verdict "T-01-outline-memory-spec-marker: intent met, and every other byte of test/clj_surgeon/outline_memory_test.clj is where it was"
