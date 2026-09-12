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
#
# RATCHET (2026-09-10, ship fast-lane RED on 49232f08): `importlib`'s
# `exec_module` byte-compiles the module it loads exactly like a real
# `import` does, and this call named no cache destination -- Python wrote
# resources/__pycache__/clj-kondo-admission.cpython-*.pyc INTO THE TRACKED
# WORKING TREE, which every parallel gate suite in the same worktree
# independently detects as a mid-run tree mutation and refuses on
# (`gate-refused: … :tree-changed-during-suite`), in a FRESH worktree where
# no stale __pycache__/ already existed to hide it. PYTHONPYCACHEPREFIX below
# matches the py_compile check further up this file, which already avoided
# this the same way; PYTHONDONTWRITEBYTECODE=1 makes it unconditional so no
# future import in this witness can regress it silently.
default_status=$(env -u CLJ_SURGEON_PRESSURE_STATUS \
  PYTHONDONTWRITEBYTECODE=1 PYTHONPYCACHEPREFIX="$test_root/pycache-default" \
  python3 -c '
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

# RATCHET (2026-09-10, ship fast-lane RED on 49232f08): the Python fallback
# check above once wrote resources/__pycache__/clj-kondo-admission.cpython-*.pyc
# INTO THE TRACKED TREE -- `importlib`'s exec_module byte-compiles exactly
# like a real `import` unless told not to, and nothing here named a cache
# destination. This dev checkout already carries a stale __pycache__/ from
# the earlier runs above, so re-running the same command here would not
# reproduce the failure -- it only shows up as a NEW file in a tree that had
# none, which is exactly what ship's fast lane runs against (`make
# landing-gate-prewarm` in a fresh disposable worktree). So this proves the
# fix the same way: a REAL fresh checkout of the committed HEAD, the exact
# suspect command, and an untracked/modified-file diff that must be empty.
# Attempt18: a linked worktree writes registration into the source common
# Git directory, which may be outside the packet. Own the clone's Git state
# under TMPDIR too; shared objects are read-only and HEAD remains exact.
hygiene_worktree=$(mktemp -d "${TMPDIR:-/var/tmp}/clj-surgeon-kondo-hygiene.XXXXXX")
trap 'rm -rf "$test_root" "$hygiene_worktree"' EXIT HUP INT TERM
git clone -q --shared --no-checkout . "$hygiene_worktree"
git -C "$hygiene_worktree" checkout -q --detach "$(git rev-parse HEAD)"
before_status=$(cd "$hygiene_worktree" && git status --porcelain --ignored)
(
  cd "$hygiene_worktree"
  env -u CLJ_SURGEON_PRESSURE_STATUS \
    PYTHONDONTWRITEBYTECODE=1 PYTHONPYCACHEPREFIX="$hygiene_worktree/pycache-hygiene-check" \
    python3 -c '
import importlib.util
from types import SimpleNamespace
spec = importlib.util.spec_from_file_location(
    "clj_kondo_admission", "resources/clj-kondo-admission.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
print(mod.pressure_status_path(SimpleNamespace(pressure_status=None)))
' >/dev/null
)
after_status=$(cd "$hygiene_worktree" && git status --porcelain --ignored)

if [ "$before_status" != "$after_status" ]; then
  echo "clj-kondo admission path hygiene regression: the Python fallback witness dirtied a fresh checkout of HEAD" >&2
  echo "before: $before_status" >&2
  echo "after:  $after_status" >&2
  exit 1
fi

echo "clj-kondo admission path regression passed"
