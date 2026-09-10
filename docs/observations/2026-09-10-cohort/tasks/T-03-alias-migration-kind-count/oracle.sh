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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/mcp_alias_migration_test.clj" "$TASKDIR"

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

want_text 'receipt-dir-escapes' "the kind receipt-dir-escapes is not in the file"
want_text 'receipt-dir-inside-workspace' "the kind receipt-dir-inside-workspace is not in the file"
want_re '\(is \(= 149 \(count kinds\)\)' "the count assertion does not agree with the new set size"
deny_re '\(is \(= 147 \(count kinds\)\)' "the old count 147 is still asserted"
python3 - "$CAND" <<'PY' || fail "the two new kinds are not inside the frozen-refusal-kinds set"
import sys, re
src = open(sys.argv[1]).read()
i = src.index('(def ^:private frozen-refusal-kinds')
j = src.index('\n(', i + 1)
body = src[i:j]
raise SystemExit(0 if '"receipt-dir-escapes"' in body
                 and '"receipt-dir-inside-workspace"' in body else 1)
PY
preserve_only --changed 'frozen-refusal-kinds,the-refusal-enumeration-is-pinned-in-count-and-in-membership' --added '' --gap-changed ''
verdict "T-03-alias-migration-kind-count: intent met, and every other byte of test/clj_surgeon/mcp_alias_migration_test.clj is where it was"
