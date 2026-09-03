#!/usr/bin/env bash
# sabotage-FAN.sh — prove the FAN scorer can go RED before any cohort trusts it green.
#
#   sabotage-FAN.sh <fixtures-dir> <N> [scratch-dir]
#
# A scorer that has never gone red is a verdict label, not a meter (this program's
# `verdict-label-was-a-noun` scar).  This harness builds the CORRECT tree (repo-N with
# canonical-N's src applied), proves it 6/6 green, then damages a fresh copy six ways
# and asserts each damage is caught by the check that is supposed to catch it:
#
#   1 wrong alias        -> CHECK 6  (the alias is not the policy's)
#   2 one site missed    -> CHECK 6  (a qualified use of the old var survives)
#   3 one extra file     -> CHECK 1  (a non-target changed)
#   4 corrupted docstring-> CHECK 3  (a protected region is gone)
#   5 reordered require  -> CHECK 2  (the form tree differs from canonical)
#   6 unparseable file   -> CHECK 2  (the file does not parse)
#
# Every case asserts on the named check's own FAIL line, not merely on a non-zero exit,
# so a scorer that fails everything for one unrelated reason does not pass this harness.
set -uo pipefail
FIX=${1:-/home/forge/tmp/arms/e3/fanout}; N=${2:-21}
SCRATCH=${3:-/home/forge/tmp/fan-sabotage}
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
cp -r "$FIX/repo-$N" "$SCRATCH/base"
( cd "$SCRATCH/base" && git init -q . && git add -A . \
    && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout rung N=$N (generated)" )
BASE=$(git -C "$SCRATCH/base" rev-parse HEAD)
mk_good () { rm -rf "$1"; cp -r "$SCRATCH/base" "$1"; cp -r "$FIX/canonical-$N/src/." "$1/src/"; }

score () { FAN_FIXTURES="$FIX" FAN_BASE="$BASE" bash "$HERE/rescore-FAN.sh" "$1" "$N" 2>&1; }

PASS=0; FAIL=0
T1=$(bb -e "(println (:file (first (:targets (read-string (slurp \"$FIX/manifest-$N.edn\"))))))")
A1=$(bb -e "(println (:new-alias (first (:targets (read-string (slurp \"$FIX/manifest-$N.edn\"))))))")
NT=$(bb -e "(println (first (:non-targets (read-string (slurp \"$FIX/manifest-$N.edn\")))))")

expect_red () {   # expect_red <case> <expected CHECK n> <worktree>
  local case=$1 want=$2 wt=$3
  local out; out=$(score "$wt"); local rc=$?
  local line; line=$(printf '%s' "$out" | grep -E "^CHECK $want .*: FAIL" | head -1)
  if [ $rc -ne 0 ] && [ -n "$line" ]; then
    PASS=$((PASS+1)); echo "SABOTAGE $case: RED as designed -> $line"
  else
    FAIL=$((FAIL+1)); echo "SABOTAGE $case: NOT CAUGHT (rc=$rc, no CHECK $want FAIL line)"
    printf '%s\n' "$out" | sed 's/^/    /'
  fi
}

echo "=== positive control: the correct tree must be 6/6 GREEN ==="
mk_good "$SCRATCH/good"
if out=$(score "$SCRATCH/good") && printf '%s' "$out" | grep -q "6/6 checks passed"; then
  PASS=$((PASS+1)); echo "POSITIVE CONTROL: GREEN 6/6"
else
  FAIL=$((FAIL+1)); echo "POSITIVE CONTROL: NOT GREEN — every sabotage below is meaningless"
  printf '%s\n' "$out" | sed 's/^/    /'
fi

echo "=== 1. wrong alias (policy says $A1) ==="
mk_good "$SCRATCH/s1"; sed -i "s/:as $A1\]/:as wrongalias]/; s/\b$A1\//wrongalias\//g" "$SCRATCH/s1/$T1"
expect_red "1 wrong-alias" 6 "$SCRATCH/s1"

echo "=== 2. one site missed ==="
mk_good "$SCRATCH/s2"; perl -0pi -e 's{/fetch-event}{/find-event}' -- "$SCRATCH/s2/$T1"
perl -0pi -e 's{/find-event}{/fetch-event}g; s{/fetch-event ids\)\)}{/find-event ids))}' -- "$SCRATCH/s2/$T1"
expect_red "2 one-site-missed" 6 "$SCRATCH/s2"

echo "=== 3. one extra file touched ($NT) ==="
mk_good "$SCRATCH/s3"; printf '\n;; an unrelated edit in a namespace that must not change\n' >> "$SCRATCH/s3/$NT"
expect_red "3 extra-file" 1 "$SCRATCH/s3"

echo "=== 4. corrupted docstring ==="
mk_good "$SCRATCH/s4"
perl -0pi -e 's{the old name find-event stays}{the old name fetch-event stays}' -- "$SCRATCH/s4/$T1"
expect_red "4 corrupted-docstring" 3 "$SCRATCH/s4"

echo "=== 5. reordered require ==="
mk_good "$SCRATCH/s5"
python3 - "$SCRATCH/s5/$T1" <<'PY'
import sys,re
p=sys.argv[1]; s=open(p).read().split("\n")
i=[k for k,l in enumerate(s) if ":require" in l][0]
j=i+1
# swap the first two require clause lines (the store2 clause and the one after it)
s[i],s[j]=s[i].replace(s[i][s[i].index("["):], s[j].strip()), s[j].replace(s[j].strip(), s[i][s[i].index("["):])
open(p,"w").write("\n".join(s))
PY
expect_red "5 reordered-require" 2 "$SCRATCH/s5"

echo "=== 6. unparseable file ==="
mk_good "$SCRATCH/s6"
python3 - "$SCRATCH/s6/$T1" <<'PY6'
import sys
p = sys.argv[1]; s = open(p).read()
i = s.rindex(")")                     # drop the file's last closing paren
open(p, "w").write(s[:i] + s[i+1:])
PY6
expect_red "6 unparseable" 2 "$SCRATCH/s6"

echo "sabotage-FAN: $PASS passed, $FAIL failed (1 positive control + 6 sabotages)"
[ $FAIL -eq 0 ] || exit 1
