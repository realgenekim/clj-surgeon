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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/core_discovery_test.clj" "$TASKDIR"

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

want_fact requires 'clojure.edn:edn' "clojure.edn is not required with the alias edn"
want_fact requires 'clj-surgeon.core:core' "the clj-surgeon.core require or its alias was lost"
want_fact requires 'babashka.fs:fs' "the babashka.fs require or its alias was lost"
want_fact requires 'babashka.process:proc' "the babashka.process require or its alias was lost"
want_fact requires 'clojure.string:str' "the clojure.string require or its alias was lost"
want_fact requires 'rewrite-clj.node:rn' "the rewrite-clj.node require or its alias was lost"
want_fact requires 'rewrite-clj.parser:rp' "the rewrite-clj.parser require or its alias was lost"
require_sorted
preserve_only --changed 'clj-surgeon.core-discovery-test' --added '' --gap-changed ''
verdict "R-01-core-discovery-require-edn: intent met, and every other byte of test/clj_surgeon/core_discovery_test.clj is where it was"
