#!/bin/sh
# RATCHET witness (2026-09-04, inb-9483a4, round two).
#
# Drives the REAL temp-dir hygiene mechanism in SUBPROCESSES, because
# `clj-surgeon.tmp-leak-support/secure-tmpdir!` re-executes its own suite and
# calls System/exit -- it cannot be witnessed from inside a clojure.test run
# without tearing down the suite that is running the test. Round one shipped
# with NO witness on any refusal branch; an independent review then drove the
# refusal by hand and found it FAILED OPEN (a full suite ran with
# java.io.tmpdir under /tmp when findmnt could not answer).
#
# Every assertion here is executed behaviour, never printed recipe text.
#
# @spec MCP-OP-TMPHYG-003
set -eu

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPO_ROOT"

FX=$(mktemp -d "${TMPDIR:-/var/tmp}/clj-surgeon-tmpleak-witness.XXXXXX")
trap 'chmod -R u+rwX "$FX" 2>/dev/null || true; rm -rf "$FX"' EXIT INT TERM

CP=$(clojure -Spath -A:clj-surgeon/mcp-test)
PROBE=clj-surgeon.tmp-leak-probe

fail() {
  echo "tmp-leak-ratchet witness FAILED: $*" >&2
  exit 1
}

# Runs the probe as a fresh JVM. Prints combined output; sets PROBE_EXIT.
# usage: run_probe <label> [env assignments...] -- [probe args...]
run_probe() {
  label=$1; shift
  out_file="$FX/$label.out"
  set +e
  env "$@" java -cp "$CP" clojure.main -m "$PROBE" >"$out_file" 2>&1
  PROBE_EXIT=$?
  set -e
  echo "--- $label (exit=$PROBE_EXIT) ---"
  cat "$out_file"
}

# Like run_probe, but with extra flags for the probe's own JVM (used to hand
# it a java.io.tmpdir it must not trust).
# usage: run_probe_flags <label> <flags> [env assignments...]
run_probe_flags() {
  label=$1; flags=$2; shift 2
  out_file="$FX/$label.out"
  set +e
  # shellcheck disable=SC2086
  env "$@" java $flags -cp "$CP" clojure.main -m "$PROBE" >"$out_file" 2>&1
  PROBE_EXIT=$?
  set -e
  echo "--- $label (exit=$PROBE_EXIT) ---"
  cat "$out_file"
}

# A PATH shim whose `findmnt` always fails, reproducing the review's arm B:
# the case where neither mount source can answer.
mkdir -p "$FX/shim"
cat >"$FX/shim/findmnt" <<'EOSHIM'
#!/bin/sh
exit 1
EOSHIM
chmod +x "$FX/shim/findmnt"

# ============================================================
# MCP-OP-TMPHYG-003: the base refusal fails CLOSED
# ============================================================

# 3a. The original mistake: TMPDIR=/tmp, findmnt present and answering.
run_probe tmpfs-literal TMPDIR=/tmp
[ "$PROBE_EXIT" -eq 97 ] || fail "3a: TMPDIR=/tmp must exit 97, got $PROBE_EXIT"
grep -q 'tmp-refused:' "$FX/tmpfs-literal.out" \
  || fail "3a: no tmp-refused: line"
grep -q 'clj-surgeon-suite-' "$FX/tmpfs-literal.out" \
  && fail "3a: a run root was created under a refused base"

# 3b. THE ARM-B REPRODUCTION. findmnt cannot answer. Round one ran the whole
# suite here, with its temp dir on the RAM tmpfs that started this incident.
run_probe arm-b TMPDIR=/tmp PATH="$FX/shim:$PATH"
[ "$PROBE_EXIT" -eq 97 ] || fail "3b (arm B): findmnt unavailable + TMPDIR=/tmp must exit 97, got $PROBE_EXIT"
grep -q 'tmp-refused:' "$FX/arm-b.out" \
  || fail "3b (arm B): no tmp-refused: line -- the refusal failed open"
grep -q 'PROBE role=child' "$FX/arm-b.out" \
  && fail "3b (arm B): the child RAN -- the suite executed on tmpfs"

# 3c. /dev/shm is refused by literal prefix with no external binary at all.
run_probe devshm TMPDIR=/dev/shm PATH="$FX/shim:$PATH"
[ "$PROBE_EXIT" -eq 97 ] || fail "3c: /dev/shm must exit 97, got $PROBE_EXIT"

# 3d. An UNDETERMINABLE filesystem type refuses, even on a real-disk path:
# neither findmnt nor the mounts table can answer, so nothing proves it is
# not RAM. Round one coerced "unknown" to "safe".
mkdir -p "$FX/realdisk"
run_probe unknown-fstype TMPDIR="$FX/realdisk" PATH="$FX/shim:$PATH" \
  CLJ_SURGEON_MOUNTS_FILE="$FX/no-such-mounts-file"
[ "$PROBE_EXIT" -eq 97 ] || fail "3d: an undeterminable fstype must exit 97, got $PROBE_EXIT"
grep -q 'UNDETERMINABLE' "$FX/unknown-fstype.out" \
  || fail "3d: the refusal must name the undeterminable fstype"

# 3e. The mounts-table fallback is ALIVE, not dead code: with findmnt shimmed
# to fail, the real mounts table still answers "this is a real disk" and the
# run proceeds. Round one's fallback used `slurp`, which throws
# `Invalid argument` on procfs (st_size = 0) for every path.
run_probe fallback-alive TMPDIR="$FX/realdisk" PATH="$FX/shim:$PATH"
[ "$PROBE_EXIT" -eq 0 ] || fail "3e: the mounts-table fallback did not answer; exit $PROBE_EXIT"
grep -q 'PROBE role=child' "$FX/fallback-alive.out" \
  || fail "3e: the child did not run under the mounts-table fallback"

# ============================================================
# MCP-OP-TMPHYG-004: the run root is proven private, and nothing else is swept
# ============================================================

# An inherited sentinel must never make a SHARED base look like this run's
# private root -- report-and-sweep-leak! delete-trees the root it is given,
# so on this multi-tenant box that is another seat's working set.
DECOY="$FX/decoy-base"
mkdir -p "$DECOY/other-seat-precious-fixture"
echo hi >"$DECOY/other-seat-file.txt"

run_probe_flags sentinel-decoy "-Djava.io.tmpdir=$DECOY" \
  TMPDIR="$DECOY" CLJ_SURGEON_TMPDIR_REEXEC=1
[ "$PROBE_EXIT" -eq 97 ] || fail "4a: an unowned sentinel must exit 97, got $PROBE_EXIT"
[ -d "$DECOY/other-seat-precious-fixture" ] \
  || fail "4a: another tenant's directory was DELETED by the sweep"
[ -f "$DECOY/other-seat-file.txt" ] \
  || fail "4a: another tenant's file was DELETED by the sweep"

# A sentinel that names a DIFFERENT root than this process actually got is
# equally untrustworthy.
run_probe_flags sentinel-mismatch "-Djava.io.tmpdir=$DECOY" \
  TMPDIR="$DECOY" CLJ_SURGEON_TMPDIR_REEXEC="$FX/some-other-root"
[ "$PROBE_EXIT" -eq 97 ] || fail "4b: a mismatched sentinel must exit 97, got $PROBE_EXIT"
[ -d "$DECOY/other-seat-precious-fixture" ] \
  || fail "4b: another tenant's directory was DELETED by the sweep"

echo "tmp-leak ratchet witness passed"
