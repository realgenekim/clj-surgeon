#!/bin/sh
# @spec TEST-ISO-015
#
# ONE READER: what `bin/install-preflight` prints for memory is what the gate's
# own reader answers -- the same number, and the same refusal.
#
# The defect this pins (skiff, Darwin arm64, 2026-09-10): the preflight printed
#   memory   read from vm_stat + sysctl hw.memsize
# and `make test` refused
#   gate-refused: available memory is unknown {:os "Mac OS X"}
# on the same box, minutes apart. The preflight had never called a reader; it
# had checked that a BINARY EXISTED and printed a source. Nothing could have
# caught that, because agreement between two implementations was never asserted.
#
# No Mac is reachable from the box that runs this suite, so the darwin path is
# driven here with GATE_MEMORY_SOURCE=darwin and a fake `vm_stat`/`sysctl` on
# PATH. That is the whole point of the forced source: a platform nobody can
# reach is a platform nobody is testing.
set -eu

repo=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d "${TMPDIR:-/var/tmp}/gate-memory-one-reader-XXXXXX")
trap 'rm -rf "$work"' 0 INT TERM

fake="$work/bin"
mkdir -p "$fake"

# 430 000 reclaimable pages of 16 KiB = 7 045 120 000 B = 6718 MiB, under a
# 64 GiB hw.memsize cap. Apple silicon really does report a 16384-byte page,
# and the trailing periods are vm_stat's real output shape.
cat > "$fake/vm_stat" <<'FAKE'
#!/bin/sh
cat <<'OUT'
Mach Virtual Memory Statistics: (page size of 16384 bytes)
Pages free:                              300000.
Pages active:                            111111.
Pages inactive:                          100000.
Pages speculative:                        20000.
Pages throttled:                              0.
Pages wired down:                        222222.
Pages purgeable:                          10000.
"Translation faults":                 123456789.
OUT
FAKE
cat > "$fake/sysctl" <<'FAKE'
#!/bin/sh
# Only hw.memsize is asked for here; anything else is not this fixture's business.
case "$*" in
  *hw.memsize*) echo 68719476736 ;;
  *) exit 1 ;;
esac
FAKE
chmod +x "$fake/vm_stat" "$fake/sysctl"

expected_mib=6718

unset GATE_MEMAVAIL_MIB || true

reader_line() {
  ( cd "$repo" && bb --classpath test -m clj-surgeon.gate-memory 2>&1 ) \
    | grep -v '^Picked up ' | tail -1
}

preflight_memory_line() {
  ( cd "$repo" && sh bin/install-preflight "$repo" 2>&1 ) \
    | sed -n 's/^ *memory  *//p' | tail -1
}

fail() { echo "gate-memory-one-reader: FAILED -- $1" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 1. tools present: the same MiB from both, and it is the arithmetic we mean
# ---------------------------------------------------------------------------
export GATE_MEMORY_SOURCE=darwin
export PATH="$fake:$PATH"

reader=$(reader_line)
preflight=$(preflight_memory_line)

case "$reader" in
  "OK $expected_mib MiB available"*) : ;;
  *) fail "gate reader answered [$reader], expected OK $expected_mib MiB" ;;
esac
case "$preflight" in
  "$expected_mib MiB available"*) : ;;
  *) fail "preflight printed [$preflight], expected $expected_mib MiB" ;;
esac
echo "gate-memory-one-reader: present  reader=[$reader] preflight=[$preflight]"

# ---------------------------------------------------------------------------
# 2. tools missing: BOTH refuse, and both name the same step
# ---------------------------------------------------------------------------
rm -f "$fake/vm_stat" "$fake/sysctl"

reader=$(reader_line)
preflight=$(preflight_memory_line)

case "$reader" in
  REFUSED*available\ memory\ is\ unknown*:step\ \"vm_stat\"*) : ;;
  *) fail "gate reader answered [$reader], expected a vm_stat refusal" ;;
esac
case "$preflight" in
  *available\ memory\ is\ unknown*:step\ \"vm_stat\"*) : ;;
  *) fail "preflight printed [$preflight], expected the gate's vm_stat refusal" ;;
esac
if [ "${reader#REFUSED }" != "$preflight" ]; then
  fail "preflight refusal [$preflight] is not the gate's refusal [${reader#REFUSED }]"
fi
echo "gate-memory-one-reader: missing  both refuse identically: $preflight"

# ---------------------------------------------------------------------------
# 3. the floor is PRINTED, not discovered by refusal
#
# The skiff's operator declared GATE_MEMAVAIL_MIB=3072 -- more than a whole
# lane costs -- and learned 3584 only from `{:memory-mib 3072 :required-mib
# 3584}` after the refusal. Both screens must carry the floor and its formula
# before anything runs, including on a box with room to spare.
# ---------------------------------------------------------------------------
floor_note="3584 MiB floor = reserve 2048 + 1536 per lane"

healthy=$( cd "$repo" && GATE_MEMAVAIL_MIB=8192 sh bin/install-preflight "$repo" 2>&1 )
case "$healthy" in
  *"$floor_note"*) : ;;
  *) fail "the preflight does not print the floor on a box with room to spare" ;;
esac

short=$( cd "$repo" && GATE_MEMAVAIL_MIB=3072 sh bin/install-preflight "$repo" 2>&1 )
case "$short" in
  *"$floor_note"*"make test\` will refuse because of: insufficient-memory"*) : ;;
  *) fail "a below-floor grant is not named as a gate blocker by the preflight" ;;
esac

refusal=$( cd "$repo" && GATE_MEMAVAIL_MIB=3072 bb --classpath src:test -e \
  '(require (quote [clj-surgeon.battery-parallel-runner :as bp]))
   (try (bp/machine-capacity) (catch Exception e (println (ex-message e))))' 2>&1 \
  | grep -v '^Picked up ' | tail -1 )
case "$refusal" in
  *"3072 MiB available, $floor_note"*GATE_MEMAVAIL_MIB*) : ;;
  *) fail "the gate refusal does not name the floor and the override: [$refusal]" ;;
esac
echo "gate-memory-one-reader: floor    printed by the preflight and by the refusal"
echo "gate-memory-one-reader: OK"
