#!/bin/sh
set -eu

test_root=$(mktemp -d -t clj-surgeon-kondo-path.XXXXXX)
trap 'rm -rf "$test_root"' EXIT HUP INT TERM

test_home="$test_root/home"
test_events="$test_root/events.jsonl"
test_status="$test_root/missing-status.json"
mkdir -p "$test_home/bin"

PYTHONPYCACHEPREFIX="$test_root/pycache" \
  python3 -m py_compile resources/clj-kondo-admission.py

make --no-print-directory install-clj-kondo-admission \
  HOME="$test_home" \
  CLJ_KONDO_ADMISSION_DEST="$test_home/bin/clj-kondo-admission" \
  CLJ_KONDO_SHIM_DEST="$test_home/bin/clj-kondo"

test -x "$test_home/bin/clj-kondo-admission"
test -x "$test_home/bin/clj-kondo"

HOME="$test_home" \
CLJ_SURGEON_CLJ_KONDO_REAL=/usr/bin/true \
CLJ_SURGEON_CLJ_KONDO_EVENTS="$test_events" \
CLJ_SURGEON_PRESSURE_STATUS="$test_status" \
CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD=1000000 \
  "$test_home/bin/clj-kondo"

grep -Fq '"status": "admitted"' "$test_events"
grep -Fq '"lane": "interactive"' "$test_events"

for file in \
  src/clj_surgeon/forward_refs.clj \
  src/clj_surgeon/binding_rename.clj \
  src/clj_surgeon/mcp_cold_verify.clj \
  test/clj_surgeon/analyzer_contract_test.clj; do
  if grep -Fq 'clojure.java.shell' "$file"; then
    echo "Analyzer entrance bypasses the bounded process adapter: $file" >&2
    exit 1
  fi
done

grep -Fq 'process-env/run-bounded!' src/clj_surgeon/forward_refs.clj
grep -Fq 'process-env/run-bounded!' src/clj_surgeon/binding_rename.clj
grep -Fq 'process-env/run-bounded!' src/clj_surgeon/mcp_cold_verify.clj
grep -Fq 'process-env/run-bounded!' test/clj_surgeon/analyzer_contract_test.clj

echo "clj-kondo admission path regression passed"
