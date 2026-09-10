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
oracle_init "${1:?usage: oracle.sh <worktree>}" "src/clj_surgeon/namespace_split.clj" "$TASKDIR"

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

want_fact requires 'cheshire.core:json' "cheshire.core is not required with the alias json"
for r in 'clj-surgeon.extract:extract' 'clj-surgeon.mcp-operation:operation' \
         'clj-surgeon.namespace-split-warm:warm' 'clj-surgeon.outline:outline' \
         'clj-surgeon.quoted-var-refs:quoted-vars' 'clj-surgeon.structural-lens:lens' \
         'clojure.set:set' 'clojure.string:str' 'clojure.walk:walk' \
         'rewrite-clj.node:n' 'rewrite-clj.parser:parser' 'rewrite-clj.zip:z'; do
  want_fact requires "$r" "a require that was already there was lost or re-aliased: $r"
done
require_sorted
preserve_only --changed 'clj-surgeon.namespace-split' --added '' --gap-changed ''
verdict "R-04-namespace-split-require-cheshire: intent met, and every other byte of src/clj_surgeon/namespace_split.clj is where it was"
