#!/usr/bin/env bash
# sabotage-FAN.sh — prove the FAN scorer can go RED before any cohort trusts it green.
#
#   sabotage-FAN.sh <fixtures-dir> <N> [scratch-dir]
#   sabotage-FAN.sh --selftest-k [N] [seed] [scratch-dir]
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
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# --- self-test: the --k irregularity knob on gen-fanout.clj -----------------------
#   sabotage-FAN.sh --selftest-k [N] [seed] [scratch-dir]
# Witnesses, for each k in {1,2,3,6} at the given N/seed (default 21/7 — the E3-P rung):
#   - two independent runs at the same n/seed/k are byte-identical (repo-N, canonical-N,
#     manifest-N.edn)
#   - k=1: manifest's :old-alias-histogram has exactly 1 key, :collisions is 0, and the
#     target count is still N (the shape a single sed closes)
#   - k=3: exactly 3 distinct old aliases, target count still N
#   - k=6: exactly 6 distinct old aliases, target count still N (today's shape:
#     store2/st2/es/store-2 at n=21 seed=7, 30 collisions -- docs/observations/
#     2026-09-04-e3-p-cohort.md)
#   - rescore-FAN.sh passes 6/6 against each k's own canonical-N
if [ "${1:-}" = "--selftest-k" ]; then
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fan-selftest-k}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  KPASS=0; KFAIL=0
  ok()  { KPASS=$((KPASS+1)); echo "SELFTEST-K $1: PASS $2"; }
  bad() { KFAIL=$((KFAIL+1)); echo "SELFTEST-K $1: FAIL $2"; }

  for k in 1 2 3 6; do
    A="$SCRATCH/k$k-a"; B="$SCRATCH/k$k-b"
    bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k "$k" --out "$A" \
      > "$SCRATCH/k$k-a.log" 2>&1
    bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k "$k" --out "$B" \
      > "$SCRATCH/k$k-b.log" 2>&1

    if diff -rq "$A/repo-$N" "$B/repo-$N" >/dev/null 2>&1 \
       && diff -rq "$A/canonical-$N" "$B/canonical-$N" >/dev/null 2>&1 \
       && diff -q "$A/manifest-$N.edn" "$B/manifest-$N.edn" >/dev/null 2>&1; then
      ok "k=$k byte-identical" "two independent runs, repo-$N + canonical-$N + manifest-$N.edn identical"
    else
      bad "k=$k byte-identical" "runs diverged -- see $A vs $B"
    fi

    HIST=$(bb -e "(print (pr-str (:old-alias-histogram (read-string (slurp \"$A/manifest-$N.edn\")))))")
    DISTINCT=$(bb -e "(print (count (:old-alias-histogram (read-string (slurp \"$A/manifest-$N.edn\")))))")
    COLL=$(bb -e "(print (:collisions (read-string (slurp \"$A/manifest-$N.edn\"))))")
    NTARGETS=$(bb -e "(print (count (:targets (read-string (slurp \"$A/manifest-$N.edn\")))))")
    echo "SELFTEST-K k=$k: manifest :k=$k distinct-old-aliases=$DISTINCT collisions=$COLL targets=$NTARGETS histogram=$HIST"

    case "$k" in
      1) if [ "$DISTINCT" = 1 ] && [ "$COLL" = 0 ] && [ "$NTARGETS" = "$N" ]; then
           ok "k=1 witness" "distinct=1 collisions=0 targets=$N"
         else bad "k=1 witness" "expected distinct=1 collisions=0 targets=$N, got distinct=$DISTINCT collisions=$COLL targets=$NTARGETS"; fi ;;
      3) if [ "$DISTINCT" = 3 ] && [ "$NTARGETS" = "$N" ]; then
           ok "k=3 witness" "distinct=3 targets=$N"
         else bad "k=3 witness" "expected distinct=3 targets=$N, got distinct=$DISTINCT targets=$NTARGETS"; fi ;;
      6) if [ "$DISTINCT" = 6 ] && [ "$NTARGETS" = "$N" ]; then
           ok "k=6 witness" "distinct=6 targets=$N (today's shape)"
         else bad "k=6 witness" "expected distinct=6 targets=$N, got distinct=$DISTINCT targets=$NTARGETS"; fi ;;
    esac

    RS="$SCRATCH/k$k-rescore"; rm -rf "$RS"; mkdir -p "$RS"
    cp -r "$A/repo-$N" "$RS/base"
    ( cd "$RS/base" && git init -q . && git add -A . \
        && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout rung k=$k (generated)" )
    RBASE=$(git -C "$RS/base" rev-parse HEAD)
    rm -rf "$RS/good"; cp -r "$RS/base" "$RS/good"; cp -r "$A/canonical-$N/src/." "$RS/good/src/"
    RSOUT=$(FAN_FIXTURES="$A" FAN_BASE="$RBASE" bash "$HERE/rescore-FAN.sh" "$RS/good" "$N" 2>&1)
    if printf '%s' "$RSOUT" | grep -q "6/6 checks passed"; then
      ok "k=$k rescore-FAN" "6/6 on canonical-$N"
    else
      bad "k=$k rescore-FAN" "not 6/6 -- $(printf '%s' "$RSOUT" | tail -3 | tr '\n' ' ')"
    fi
  done

  echo "sabotage-FAN --selftest-k: $KPASS passed, $KFAIL failed"
  [ $KFAIL -eq 0 ]
  exit $?
fi

FIX=${1:-/home/forge/tmp/arms/e3/fanout}; N=${2:-21}
SCRATCH=${3:-/home/forge/tmp/fan-sabotage}

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
