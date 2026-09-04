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

# Like run_probe, but with extra flags for the probe's own JVM and extra args
# for the probe itself.
# usage: run_probe_flags <label> "<jvm flags>" "<probe args>" [env assignments...]
run_probe_flags() {
  label=$1; flags=$2; pargs=$3; shift 3
  out_file="$FX/$label.out"
  set +e
  # shellcheck disable=SC2086
  env "$@" java $flags -cp "$CP" clojure.main -m "$PROBE" $pargs >"$out_file" 2>&1
  PROBE_EXIT=$?
  set -e
  echo "--- $label (exit=$PROBE_EXIT) ---"
  cat "$out_file"
}

# The value of `<field>=` on the PROBE line for `<role>`.
probe_field() {
  sed -n "s/^PROBE role=$2 .*$3=\\(.*\\)$/\\1/p" "$FX/$1.out" | head -1
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

# 3f. Neither mount source can answer AND the base is /tmp: only the literal
# name check can refuse here. (3b alone does not witness it: on this box the
# mounts table still answers "tmpfs" for /tmp when findmnt is shimmed away.)
run_probe name-only TMPDIR=/tmp PATH="$FX/shim:$PATH" \
  CLJ_SURGEON_MOUNTS_FILE="$FX/no-such-mounts-file"
[ "$PROBE_EXIT" -eq 97 ] || fail "3f: /tmp must be refused BY NAME when no mount source can answer, got $PROBE_EXIT"
grep -q 'RAM-backed path by name' "$FX/name-only.out" \
  || fail "3f: the refusal did not come from the literal name check"

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

# 3g. The witness SEAM cannot grant a PASS. `CLJ_SURGEON_MOUNTS_FILE` exists so
# the gate can execute the "no mount source can answer" branch (3d/3f); it must
# never be able to turn a refusal into a run. Round two's review drove exactly
# that: with findmnt dead and a forged table claiming ext4, a full suite ran
# with java.io.tmpdir on a REAL tmpfs. A seam-sourced fstype is therefore never
# positive proof of disk -- any non-tmpfs answer from it reads as :unknown, so
# this base (genuinely ext4, and accepted in 3e via the REAL mounts table)
# must still be refused when only the forged table can answer for it.
# @spec MCP-OP-TMPHYG-011
mkdir -p "$FX/seamdisk"
cat >"$FX/forged-mounts" <<EOFORGED
/dev/forged / ext4 rw,relatime 0 0
/dev/forged $FX ext4 rw,relatime 0 0
EOFORGED
run_probe seam-escape TMPDIR="$FX/seamdisk" PATH="$FX/shim:$PATH" \
  CLJ_SURGEON_MOUNTS_FILE="$FX/forged-mounts"
[ "$PROBE_EXIT" -eq 97 ] \
  || fail "3g: a forged mounts table PROVED disk -- the witness seam granted a pass (exit $PROBE_EXIT)"
grep -q 'UNDETERMINABLE' "$FX/seam-escape.out" \
  || fail "3g: the refusal must name the undeterminable fstype"
grep -q 'PROBE role=child' "$FX/seam-escape.out" \
  && fail "3g: the child RAN on a base whose only proof of disk came from the seam"

# 3h. The seam is still SOUND in the refusing direction: a table that says
# tmpfs refuses, so the seam can never be used to hide RAM either.
# @spec MCP-OP-TMPHYG-011
cat >"$FX/forged-tmpfs-mounts" <<EOTMPFS
/dev/forged / ext4 rw,relatime 0 0
tmpfs $FX tmpfs rw,relatime 0 0
EOTMPFS
run_probe seam-tmpfs TMPDIR="$FX/seamdisk" PATH="$FX/shim:$PATH" \
  CLJ_SURGEON_MOUNTS_FILE="$FX/forged-tmpfs-mounts"
[ "$PROBE_EXIT" -eq 97 ] || fail "3h: a seam-sourced tmpfs answer must refuse, got $PROBE_EXIT"
grep -q 'RAM-backed (tmpfs' "$FX/seam-tmpfs.out" \
  || fail "3h: the refusal did not come from the tmpfs branch"

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

run_probe_flags sentinel-decoy "-Djava.io.tmpdir=$DECOY" "" \
  TMPDIR="$DECOY" CLJ_SURGEON_TMPDIR_REEXEC=1
[ "$PROBE_EXIT" -eq 97 ] || fail "4a: an unowned sentinel must exit 97, got $PROBE_EXIT"
[ -d "$DECOY/other-seat-precious-fixture" ] \
  || fail "4a: another tenant's directory was DELETED by the sweep"
[ -f "$DECOY/other-seat-file.txt" ] \
  || fail "4a: another tenant's file was DELETED by the sweep"

# A sentinel that names a DIFFERENT root than this process actually got is
# equally untrustworthy.
run_probe_flags sentinel-mismatch "-Djava.io.tmpdir=$DECOY" "" \
  TMPDIR="$DECOY" CLJ_SURGEON_TMPDIR_REEXEC="$FX/some-other-root"
[ "$PROBE_EXIT" -eq 97 ] || fail "4b: a mismatched sentinel must exit 97, got $PROBE_EXIT"
[ -d "$DECOY/other-seat-precious-fixture" ] \
  || fail "4b: another tenant's directory was DELETED by the sweep"

# ============================================================
# MCP-OP-TMPHYG-006: the re-exec preserves the parent's launch
# ============================================================

# The parent's JVM flags. `make mcp-test` pins the suite at -Xmx512m; round
# one rebuilt the child as a bare `java -cp ... clojure.main` and the suite
# silently ran at the box default (7.8 GB) while the heap-config gate — which
# only reads `make -n` TEXT — stayed green.
run_probe_flags heap-args "-Xmx317m" "alpha beta" TMPDIR="$FX/realdisk"
[ "$PROBE_EXIT" -eq 0 ] || fail "6: probe exit $PROBE_EXIT"
parent_mb=$(probe_field heap-args parent max-mb)
child_mb=$(probe_field heap-args child max-mb)
parent_mb=${parent_mb%% *}
child_mb=${child_mb%% *}
[ -n "$parent_mb" ] && [ -n "$child_mb" ] || fail "6: could not read max-mb from the probe"
[ "$parent_mb" -lt 1000 ] || fail "6: the parent's own -Xmx317m did not take (max-mb=$parent_mb)"
[ "$child_mb" = "$parent_mb" ] \
  || fail "6a: the re-exec discarded the parent's heap ceiling: parent=$parent_mb MB child=$child_mb MB"

# The parent's argv. Harmless today (both runners ignore args) but a silent
# arg sink: the day a runner takes a test selector it vanishes with no error.
parent_args=$(probe_field heap-args parent args)
child_args=$(probe_field heap-args child args)
[ "$parent_args" = '["alpha" "beta"]' ] || fail "6: parent argv not seen: $parent_args"
[ "$child_args" = "$parent_args" ] \
  || fail "6b: the re-exec dropped the test-selection args: parent=$parent_args child=$child_args"

# The bb lane re-execs by script path and appends args after the script.
set +e
env TMPDIR="$FX/realdisk" bb test/tmp_leak_probe.clj alpha beta >"$FX/bb-args.out" 2>&1
BB_EXIT=$?
set -e
echo "--- bb-args (exit=$BB_EXIT) ---"
cat "$FX/bb-args.out"
[ "$BB_EXIT" -eq 0 ] || fail "6c: the bb lane exited $BB_EXIT"
[ "$(probe_field bb-args child args)" = '["alpha" "beta"]' ] \
  || fail "6c: the bb re-exec dropped its args"

# ============================================================
# MCP-OP-TMPHYG-005: descendant processes inherit the isolated root
# ============================================================

# -Djava.io.tmpdir is a JVM-internal property no child PROCESS inherits, so a
# subprocess that picks its own temp location (mktemp -d, tempfile.mkdtemp)
# wrote to the SHARED base — outside the isolated root, invisible to the leak
# witness, and left behind on a multi-tenant box.
mkdir -p "$FX/subbase"
run_probe_flags subproc "" "--leak-subprocess" TMPDIR="$FX/subbase"
sub_root=$(sed -n 's/^PROBE root=//p' "$FX/subproc.out" | head -1)
sub_dir=$(sed -n 's/^PROBE subprocess-tmpdir=//p' "$FX/subproc.out" | head -1)
[ -n "$sub_root" ] && [ -n "$sub_dir" ] || fail "5: could not read the probe root / subprocess dir"
case "$sub_dir" in
  "$sub_root"/*) : ;;
  *) fail "5a: a subprocess temp dir ESCAPED the isolated root: $sub_dir (root $sub_root)" ;;
esac
[ "$PROBE_EXIT" -ne 0 ] \
  || fail "5b: the escaped subprocess temp dir was not reported as a leak"
grep -q 'temp-leak:' "$FX/subproc.out" || fail "5b: no temp-leak: line naming it"
# and nothing survives in the shared base
[ -z "$(ls -A "$FX/subbase")" ] \
  || fail "5c: entries left in the shared base: $(ls -A "$FX/subbase")"

# ============================================================
# MCP-OP-TMPHYG-007: an isolated root does not outlive its run
# ============================================================

# 7a. A killed parent. ~/bin/suite-run jobs are routinely wrapped in
# `timeout`, and a timeout kill is a SIGTERM. Round one only swept on the
# normal return path, so every killed run left one root behind for ever.
mkdir -p "$FX/killbase"
env TMPDIR="$FX/killbase" java -cp "$CP" clojure.main -m "$PROBE" --sleep \
  >"$FX/kill.out" 2>&1 &
kill_pid=$!
i=0
while [ "$i" -lt 150 ]; do
  grep -q 'PROBE sleeping' "$FX/kill.out" 2>/dev/null && break
  i=$((i + 1))
  sleep 0.2
done
grep -q 'PROBE sleeping' "$FX/kill.out" || fail "7a: the probe never reached its sleep"
kill -TERM "$kill_pid" 2>/dev/null || true
wait "$kill_pid" 2>/dev/null || true
i=0
while [ "$i" -lt 50 ]; do
  [ -z "$(ls -A "$FX/killbase")" ] && break
  i=$((i + 1))
  sleep 0.2
done
echo "--- kill-term (left in base) ---"
ls -A "$FX/killbase" || true
[ -z "$(ls -A "$FX/killbase")" ] \
  || fail "7a: a SIGTERMed run left its isolated root behind: $(ls -A "$FX/killbase")"

# 7b. Startup sweep of stale roots — and ONLY of stale roots this namespace
# could have created, whose owning pid is dead. Never another tenant's entry.
mkdir -p "$FX/stalebase"
mkdir -p "$FX/stalebase/clj-surgeon-suite-4194303-deadbeef"      # dead pid, old
mkdir -p "$FX/stalebase/clj-surgeon-suite-$$-aaaaaaaa"           # LIVE pid, old
mkdir -p "$FX/stalebase/other-seat-precious-fixture"             # not ours
touch -d '10 hours ago' "$FX/stalebase/clj-surgeon-suite-4194303-deadbeef" \
  "$FX/stalebase/clj-surgeon-suite-$$-aaaaaaaa" \
  "$FX/stalebase/other-seat-precious-fixture"
run_probe stale-sweep TMPDIR="$FX/stalebase"
[ "$PROBE_EXIT" -eq 0 ] || fail "7b: probe exit $PROBE_EXIT"
echo "--- stalebase after ---"
ls -A "$FX/stalebase" || true
[ ! -d "$FX/stalebase/clj-surgeon-suite-4194303-deadbeef" ] \
  || fail "7b: a stale root whose pid is dead was not swept"
[ -d "$FX/stalebase/clj-surgeon-suite-$$-aaaaaaaa" ] \
  || fail "7b: a root whose owning process is STILL ALIVE was swept"
[ -d "$FX/stalebase/other-seat-precious-fixture" ] \
  || fail "7b: another tenant's entry was swept"

# ============================================================
# MCP-OP-TMPHYG-008: an unusable base is a typed refusal, not a stack trace
# ============================================================

mkdir -p "$FX/nowrite"
chmod 500 "$FX/nowrite"
run_probe unwritable TMPDIR="$FX/nowrite"
chmod 700 "$FX/nowrite"
[ "$PROBE_EXIT" -eq 97 ] \
  || fail "8: an unwritable base must exit 97 like every other refusal, got $PROBE_EXIT"
grep -q 'tmp-refused:' "$FX/unwritable.out" || fail "8: no tmp-refused: line"
grep -q 'Execution error' "$FX/unwritable.out" \
  && fail "8: the refusal is a raw stack trace, not a named message"

# ============================================================
# MCP-OP-TMPHYG-009: EVERY test entry point enforces the ratchet
# ============================================================

# Gene's ask was "make it impossible to make this mistake again". Round one
# guarded 2 of the repo's 5 test entry points, and `make test` runs
# analyzer-contract-test between the two protected lanes.
assert_runner_refuses() {
  ns=$1
  out_file="$FX/runner-$(printf '%s' "$ns" | tr './' '--').out"
  set +e
  env TMPDIR=/tmp timeout 45 java -cp "$CP" clojure.main -m "$ns" >"$out_file" 2>&1
  rc=$?
  set -e
  echo "--- runner $ns (exit=$rc) ---"
  head -5 "$out_file"
  [ "$rc" -eq 97 ] \
    || fail "9: $ns ran (exit $rc) with TMPDIR=/tmp instead of refusing"
  grep -q 'tmp-refused:' "$out_file" || fail "9: $ns printed no tmp-refused: line"
}

assert_runner_refuses analyzer-contract-test-runner
assert_runner_refuses clj-surgeon.memory.memory-test-runner
assert_runner_refuses clj-surgeon.memory-battery-runner
assert_runner_refuses clj-surgeon.mcp-test-runner

# ============================================================
# MCP-OP-TMPHYG-010: no gate writes to a hard-coded /tmp path
# ============================================================

# A refusal at the runner is worthless if the gates around it still create
# directories in RAM by name. This is a source scan on purpose -- "never X
# anywhere" cannot be witnessed by executing one path.
# Prose mentions of /tmp are not matches; only a literal /tmp/<name> used as a
# path is. A TMPDIR fallback that NAMES the RAM path is also a match as of
# round two: that shape takes /tmp whenever TMPDIR is unset, which is every
# shell without seat-tmp-guard.sh. Default to /var/tmp instead.
# (This comment deliberately avoids writing the offending shape out, because
# this file is one of the files the scan reads.)
#
# `bench/*.sh` is IN SCOPE (round two): `make test` runs
# `bench/retain_benchmark_result.sh`, `bench/run_clean_codex.sh`,
# `bench/run_clean_claude.sh` and `bench/run_inspect_mcp_benchmark.sh`, so a
# hard-coded root in a bench self-test is a directory this repo's own test
# command creates in RAM. The design doc previously called `bench/*.sh` out of
# scope on the grounds that no gate reached it; that sentence was wrong.
hardcoded=$(grep -nE '(^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]|TMPDIR:-/tmp\}' Makefile test/*.sh bench/*.sh || true)
if [ -n "$hardcoded" ]; then
  echo "$hardcoded" >&2
  fail "10: hard-coded /tmp write targets remain in Makefile / test / bench shell gates"
fi

# ============================================================
# MCP-OP-TMPHYG-012: the Make layer refuses to propagate a RAM TMPDIR
# ============================================================

# `SELF_TEST_TMP` is the scratch root the benchmark self-test recipes hand to
# their harnesses. Its default was `$(or $(TMPDIR),/var/tmp)`, which is right
# when TMPDIR is unset and WRONG when TMPDIR is itself a RAM path: the Make
# layer propagated /tmp happily and only the Clojure layer refused, so a
# recipe that never reaches Clojure wrote to RAM. This arm EXECUTES make's own
# variable expansion (--eval defines a throwaway target in the real Makefile;
# nothing is added to the Makefile itself) and asserts the value, rather than
# reading the assignment as text.
# @spec MCP-OP-TMPHYG-012
self_test_tmp() {
  env TMPDIR="$1" make -s --eval='__tmphyg_print:; @echo $(SELF_TEST_TMP)' \
    __tmphyg_print 2>/dev/null | tail -n 1
}

# The subpath cases are built from variables rather than written as a literal
# path, on purpose: arm 10 (MCP-OP-TMPHYG-010) below source-scans this very
# file for a hard-coded /tmp/<name> write target, and a literal RAM subpath
# here would be a false positive against that scan even though it is a test
# input value, not a write target.
ram_tmp_root=/tmp
ram_shm_root=/dev/shm
ram_sub=probe
for ram in "$ram_tmp_root" "$ram_tmp_root/$ram_sub" "$ram_shm_root" "$ram_shm_root/$ram_sub"; do
  got=$(self_test_tmp "$ram")
  echo "--- SELF_TEST_TMP with TMPDIR=$ram -> $got ---"
  case "$got" in
    /tmp|/tmp/*|/dev/shm|/dev/shm/*)
      fail "11: the Make layer propagated a RAM TMPDIR into SELF_TEST_TMP ($got)" ;;
  esac
done

got=$(self_test_tmp "")
echo "--- SELF_TEST_TMP with TMPDIR unset -> $got ---"
[ "$got" = "/var/tmp" ] \
  || fail "11: an unset TMPDIR must default SELF_TEST_TMP to /var/tmp, got $got"

got=$(self_test_tmp "$FX")
echo "--- SELF_TEST_TMP with TMPDIR=$FX -> $got ---"
[ "$got" = "$FX" ] \
  || fail "11: a real-disk TMPDIR must be honoured, got $got"

got=$(self_test_tmp "/var/tmp/forge")
echo "--- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> $got ---"
[ "$got" = "/var/tmp/forge" ] \
  || fail "11: a real-disk TMPDIR (/var/tmp/forge) must be honoured, got $got"

echo "tmp-leak ratchet witness passed"
