#!/bin/sh
set -eu

test_root=$(mktemp -d "${TMPDIR:-/var/tmp}/clj-surgeon-kondo-path.XXXXXX")
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

# SPF-002: direct-shell mode builds `args.pressure_status` from
# CLJ_SURGEON_PRESSURE_STATUS and never routes through mcp_process.clj, so the
# PYTHON DEFAULT (with no override at all -- neither args nor the env var) is
# its own live contract, not dead duplication of the Clojure one.
default_status=$(env -u CLJ_SURGEON_PRESSURE_STATUS python3 -c '
import importlib.util
from types import SimpleNamespace
spec = importlib.util.spec_from_file_location(
    "clj_kondo_admission", "resources/clj-kondo-admission.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
print(mod.pressure_status_path(SimpleNamespace(pressure_status=None)))
')

case "$default_status" in
  */.local/state/clj-surgeon/pressure-status.json) ;;
  *)
    echo "SPF-002 regression: Python fallback pressure_status_path defaulted to unexpected path: $default_status" >&2
    exit 1
    ;;
esac

case "$default_status" in
  *diagnose-skiff-cpu-memory*)
    echo "SPF-002 regression: the old seat-named monitor path survived in the Python default: $default_status" >&2
    exit 1
    ;;
esac

echo "clj-kondo admission path regression passed"
