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
oracle_init "${1:?usage: oracle.sh <worktree>}" "test/clj_surgeon/mcp_workspace_test.clj" "$TASKDIR"

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

want_fact requires 'clojure.string:str' "clojure.string is not required with the alias str"
deny_text 'clojure.string/includes?' "the longhand clojure.string/ call is still there"
want_text '(is (str/includes? a1 "/edit-clojure-receipts/"))' "the call site does not go through the alias with its original arguments"
want_fact requires 'clj-surgeon.mcp-workspace:workspace' "the mcp-workspace require or its alias was lost"
want_fact requires 'clojure.java.io:io' "the clojure.java.io require or its alias was lost"
want_text ':refer [deftest is testing]' "the clojure.test :refer list changed"
want_fact usages 'clojure.string/includes?' "the analyzer no longer resolves the call to clojure.string/includes?"
require_sorted
preserve_only --changed 'clj-surgeon.mcp-workspace-test,receipt-directories-are-deterministic-and-workspace-isolated' --added '' --gap-changed ''
verdict "R-03-workspace-string-alias: intent met, and every other byte of test/clj_surgeon/mcp_workspace_test.clj is where it was"
