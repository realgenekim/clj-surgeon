#!/usr/bin/env bash
# self-test.sh — PF-5 for the apparatus itself.  Proves the attest -> watch -> score
# chain end to end against a FAKE driver, in seconds, with no MCP server, no network
# and no arm-run budget spent.
#
# It asserts COMPUTED fields, never words: a scorer that silently returns 0 is the
# `verdict-label-was-a-noun` defect and it has already taken this program down once.
#
#   make anvil-arms-self-test        (or: bash bench/anvil-arms/self-test.sh)
#
# Cases:
#   1  PF-5 chain: 3 returns, 2 tool calls, 1 test call -> receipt with those counts
#   2  rich fixture: verb calls, a typed refusal with next_call, native apply_patch
#      landing .clj bytes, ls-tree adoption ordinal, computed churn
#   3  a MISSING rollout yields exit 3 and NO receipt
#   4  a rollout with zero returns aborts the watcher with a typed line
#   5  the zero-return WINDOW fires on a hanging driver
#   6  an attestation mismatch exits 2 and the driver is never launched
#   7  a native arm handed an MCP url is refused
#   8  run-cohort.sh produces the mirrored order  N-1 T-1 T-2 N-2 N-3 T-3
#   9  the four B.4 prompts still match the doc, byte for byte
#  10  test_call is matched AT COMMAND POSITION, not anywhere in the string
set -uo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
WORK=${ANVIL_ARMS_SELFTEST_DIR:-$(mktemp -d "${TMPDIR:-/tmp}/anvil-arms-selftest.XXXXXX")}
CLEAN=${ANVIL_ARMS_SELFTEST_KEEP:-0}
PASS=0; FAIL=0

ok   () { PASS=$((PASS+1)); printf 'ok   %s\n' "$1"; }
bad  () { FAIL=$((FAIL+1)); printf 'FAIL %s\n' "$1"; }
want () { # want <label> <expected> <actual>
  if [ "$2" = "$3" ]; then ok "$1 = $3"; else bad "$1: expected $2, got $3"; fi
}
jqf () { python3 -c 'import json,sys;d=json.load(open(sys.argv[1]))
for k in sys.argv[2].split("."):
    d = d[int(k)] if isinstance(d, list) else d.get(k)
    if d is None: break
print(json.dumps(d) if isinstance(d,(dict,list,bool)) or d is None else d)' "$1" "$2" 2>/dev/null || echo MISSING; }

# --- a tiny throwaway git repo to serve as every arm's worktree --------------------
BASE_REPO="$WORK/base-repo"
mkdir -p "$BASE_REPO/src/fake"
cat > "$BASE_REPO/src/fake/sample.clj" <<'CLJ'
(ns fake.sample)
(defn now [] (System/currentTimeMillis))
CLJ
git -C "$BASE_REPO" init -q
git -C "$BASE_REPO" -c user.email=selftest@anvil -c user.name=selftest add src/fake/sample.clj
git -C "$BASE_REPO" -c user.email=selftest@anvil -c user.name=selftest commit -qm base
BASE_SHA=$(git -C "$BASE_REPO" rev-parse HEAD)

run_arm () { bash "$HERE/run-arm.sh" --driver fake --root "$WORK" "$@"; }

echo "== case 1: the PF-5 chain (3 returns, 2 tool calls, 1 test call) =="
run_arm --exp st --rung P --arm N --slot 1 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture pf5 \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case1.out" 2>&1
rc1=$?
A1="$WORK/st-P-N-1"
want "case1 run-arm rc" 0 "$rc1"
if [ -s "$A1/receipt.json" ]; then
  want "case1 returns"           3 "$(jqf "$A1/receipt.json" meter.returns)"
  want "case1 total_actions"     2 "$(jqf "$A1/receipt.json" meter.total_actions)"
  want "case1 test_actions"      1 "$(jqf "$A1/receipt.json" meter.test_actions)"
  want "case1 non_test_actions"  1 "$(jqf "$A1/receipt.json" meter.non_test_actions)"
  want "case1 self_reported"     2 "$(jqf "$A1/receipt.json" meter.self_reported_toolcalls)"
  want "case1 meters agree"   true "$(jqf "$A1/receipt.json" meter.sources.agree)"
  want "case1 churn insertions"  0 "$(jqf "$A1/receipt.json" churn.insertions)"
  want "case1 churn status" computed "$(jqf "$A1/receipt.json" churn.status)"
  want "case1 attest_ok"      true "$(jqf "$A1/receipt.json" attest.attest_ok)"
  want "case1 gate unverified" unverified "$(jqf "$A1/receipt.json" gate.green)"
  w=$(jqf "$A1/receipt.json" meter.wall_s)
  case "$w" in ''|*[!0-9.]*) bad "case1 wall_s is not a number: $w";; *) ok "case1 wall_s = $w (computed, not hand-typed)";; esac
  ph=$(jqf "$A1/receipt.json" attest.prompt_sha256)
  want "case1 prompt sha matches prompts/E3-P-N.sha256" "$(cut -d' ' -f1 < "$HERE/prompts/E3-P-N.sha256")" "$ph"
else
  bad "case1 no receipt.json written"; cat "$WORK/case1.out"
fi

echo "== case 2: rich fixture — verbs, refusal, native fallback, adoption, churn =="
run_arm --exp st --rung P --arm N --slot 2 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture rich \
        --churn-band 1,10,0,2 \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case2.out" 2>&1
A2="$WORK/st-P-N-2"
if [ -s "$A2/receipt.json" ]; then
  want "case2 returns"                 6 "$(jqf "$A2/receipt.json" meter.returns)"
  want "case2 total_actions"           5 "$(jqf "$A2/receipt.json" meter.total_actions)"
  want "case2 non_test_actions"        4 "$(jqf "$A2/receipt.json" meter.non_test_actions)"
  want "case2 write calls via verb"    2 "$(jqf "$A2/receipt.json" writes.via_verb)"
  want "case2 committed verb calls"    1 "$(jqf "$A2/receipt.json" writes.via_verb_committed)"
  want "case2 native apply_patch .clj" 1 "$(jqf "$A2/receipt.json" writes.native_apply_patch_clj)"
  want "case2 refusals"                1 "$(jqf "$A2/receipt.json" refusals.0.n)"
  want "case2 refusal error_type" alias-migration-expect-mismatch \
       "$(jqf "$A2/receipt.json" refusals.0.error_type)"
  want "case2 refusal next_call_present" true "$(jqf "$A2/receipt.json" refusals.0.next_call_present)"
  want "case2 refusal outcome"  recovered "$(jqf "$A2/receipt.json" refusals.0.outcome)"
  want "case2 ls-tree calls"           1 "$(jqf "$A2/receipt.json" adoption.ls_tree_calls)"
  want "case2 ls-tree first return"    1 "$(jqf "$A2/receipt.json" adoption.first_ls_tree_return)"
  want "case2 ls-tree early"        true "$(jqf "$A2/receipt.json" adoption.early)"
  want "case2 churn insertions"        3 "$(jqf "$A2/receipt.json" churn.insertions)"
  want "case2 churn within band"    true "$(jqf "$A2/receipt.json" churn.within_band)"
else
  bad "case2 no receipt.json written"; cat "$WORK/case2.out"
fi

echo "== case 3: a MISSING rollout is exit 3 and NO receipt =="
A3="$WORK/st-P-N-3"; mkdir -p "$A3/worktree"
cp "$A1/attest.json" "$A3/attest.json"
cp "$A1/watch.jsonl" "$A3/watch.jsonl"
python3 "$HERE/score.py" "$A3" > "$WORK/case3.out" 2>&1
want "case3 score rc"                3 "$?"
if [ -e "$A3/receipt.json" ]; then bad "case3 a receipt was written over a missing rollout"
else ok "case3 no receipt.json written"; fi
grep -q 'SCORE-ABORT missing-rollout' "$WORK/case3.out" \
  && ok "case3 typed abort line" || { bad "case3 no typed abort line"; cat "$WORK/case3.out"; }

echo "== case 3b: an EMPTY rollout is exit 3 and NO receipt =="
: > "$A3/rollout.jsonl"
python3 "$HERE/score.py" "$A3" > "$WORK/case3b.out" 2>&1
want "case3b score rc"               3 "$?"
[ -e "$A3/receipt.json" ] && bad "case3b receipt written over an empty rollout" \
  || ok "case3b no receipt.json written"
grep -q 'SCORE-ABORT empty-rollout' "$WORK/case3b.out" \
  && ok "case3b typed abort line" || bad "case3b no typed abort line"

echo "== case 4: zero returns aborts the watcher with a typed line =="
run_arm --exp st --rung P --arm N --slot 4 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture zero \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case4.out" 2>&1
A4="$WORK/st-P-N-4"
grep -q 'WATCH-ABORT zero-returns' "$A4/driver.log" \
  && ok "case4 WATCH-ABORT zero-returns" || { bad "case4 no typed watcher abort"; cat "$A4/driver.log"; }
[ -e "$A4/receipt.json" ] && bad "case4 receipt written for a zero-return run" \
  || ok "case4 no receipt.json written"
want "case4 run.json abort" zero-returns "$(jqf "$A4/run.json" abort)"

echo "== case 5: the zero-return WINDOW fires on a hanging driver =="
A5="$WORK/st-P-N-5"; mkdir -p "$A5"
git clone -q --no-hardlinks "$BASE_REPO" "$A5/worktree"
printf '%s\n' "$BASE_SHA" > "$A5/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A5/prompt.md"
EXP=st RUNG=P SLOT=5 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A5" N - "" > /dev/null 2>&1
t_start=$(date +%s)
python3 "$HERE/watch.py" --arm "$A5" --zero-return-window 2 --poll 0.2 \
  -- bash "$HERE/fake-driver.sh" "$A5" hang > "$WORK/case5.out" 2>&1
want "case5 watch rc" 4 "$?"
t_elapsed=$(( $(date +%s) - t_start ))
[ "$t_elapsed" -lt 20 ] && ok "case5 window fired in ${t_elapsed}s (driver was killed, not waited out)" \
  || bad "case5 took ${t_elapsed}s — the window did not fire"
grep -q 'WATCH-ABORT zero-returns' "$WORK/case5.out" \
  && ok "case5 typed abort line" || bad "case5 no typed abort line"

echo "== case 6: an attestation mismatch exits 2 and never launches the driver =="
A6="$WORK/st-P-N-6"; mkdir -p "$A6"
git clone -q --no-hardlinks "$BASE_REPO" "$A6/worktree"
printf '%s\n' "0000000000000000000000000000000000000000" > "$A6/base.sha"
run_arm --exp st --rung P --arm N --slot 6 --prompt "$HERE/prompts/E3-P-N.md" \
        --base-file "$A6/base.sha" --fixture pf5 > "$WORK/case6.out" 2>&1
want "case6 run-arm rc" 2 "$?"
[ -e "$A6/rollout.jsonl" ] && bad "case6 the driver ran despite ATTEST-MISMATCH" \
  || ok "case6 no rollout.jsonl — the driver never launched"
[ -s "$A6/ATTEST-MISMATCH" ] && ok "case6 ATTEST-MISMATCH file: $(cat "$A6/ATTEST-MISMATCH")" \
  || bad "case6 no ATTEST-MISMATCH file"
want "case6 attest_ok" false "$(jqf "$A6/attest.json" attest_ok)"

echo "== case 7: a native arm handed an MCP url is refused =="
A7="$WORK/st-P-N-7"; mkdir -p "$A7"
git clone -q --no-hardlinks "$BASE_REPO" "$A7/worktree"
printf '%s\n' "$BASE_SHA" > "$A7/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A7/prompt.md"
EXP=st RUNG=P SLOT=7 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  MCP_URL="http://127.0.0.1:7907/mcp" \
  bash "$HERE/attest.sh" "$A7" N - "" > "$WORK/case7.out" 2>&1
want "case7 attest rc" 2 "$?"
grep -q 'mcp-absent-proof' "$WORK/case7.out" \
  && ok "case7 refused on mcp-absent-proof" || { bad "case7 wrong refusal"; cat "$WORK/case7.out"; }

echo "== case 7b: a tool arm outside the cohort port range is refused =="
run_arm --exp st --rung P --arm T --slot 7 --prompt "$HERE/prompts/E3-P-T.md" \
        --port 7888 --worktree-src "$BASE_REPO" --base "$BASE_SHA" \
        --fixture pf5 > "$WORK/case7b.out" 2>&1
want "case7b run-arm rc" 2 "$?"
grep -q 'REFUSING port' "$WORK/case7b.out" \
  && ok "case7b refused a forbidden port before touching it" \
  || { bad "case7b did not refuse the port"; cat "$WORK/case7b.out"; }

echo "== case 8: the mirrored cohort order =="
ORDER=$(bash "$HERE/run-cohort.sh" --root "$WORK/cohort" --exp st --rung P --arms N,T \
        --n 3 --prompt-dir "$HERE/prompts" --prompt-prefix E3-P --ports "T=7907" \
        --dry-run 2>&1 | sed -n 's/^ORDER //p')
want "case8 mirrored order" "N-1 T-1 T-2 N-2 N-3 T-3" "$ORDER"

echo "== case 9: the four B.4 prompts still match the doc =="
python3 "$HERE/prompts/build-prompts.py" --check > "$WORK/case9.out" 2>&1 \
  && ok "case9 prompts match the doc" || { bad "case9 prompt drift"; cat "$WORK/case9.out"; }
for p in E3-P-N E3-P-T E3-L-N E3-L-T; do
  [ -s "$HERE/prompts/$p.md" ] && [ -s "$HERE/prompts/$p.sha256" ] \
    && ok "case9 $p installed + hashed" || bad "case9 $p missing"
done

echo "== case 10: test_call is matched AT COMMAND POSITION =="
python3 - "$HERE" <<'PY'
import sys, pathlib
sys.path.insert(0, sys.argv[1])
from watch import is_test_command
cases = [
    ("bin/kaocha --focus marvin-voice-remote.bridge3-new-test", True),
    ("cd src && bin/kaocha --focus x", True),
    ("bin/fan-test", True),
    ("clojure -M:test", True),
    ("make test-fast", True),
    ("bb test/run_all.clj", True),
    ("flock -x 9 bin/kaocha", True),
    ("timeout 600 bin/kaocha", True),
    ("rg -n 'kaocha' src/", False),
    ("echo bin/kaocha", False),
    ("git log --grep=kaocha", False),
    ("cat bin/fan-test", False),
    ("apply_patch < p.diff", False),
]
bad = [(c, want, is_test_command(c)[0]) for c, want in cases
       if is_test_command(c)[0] != want]
for c, want, got in bad:
    print(f"FAIL case10 {c!r}: expected {want}, got {got}")
print(f"ok   case10 command-position matching {len(cases)-len(bad)}/{len(cases)}")
sys.exit(1 if bad else 0)
PY
if [ $? -eq 0 ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi

echo "== case 11: a receipt is emitted ONLY from a stream that validates =="
# Sol's executed probes: a truncated final record scored rc 0; duplicated and fully
# reversed streams produced receipts with sources.agree=true; a stale receipt.json
# survived an rc-3 abort; an empty watch.jsonl scored 0 returns; attest_ok=false scored.
mk11 () {                       # mk11 <suffix> -> prints a fresh arm dir holding case 1's evidence
  local d="$WORK/st-P-N-11$1"
  rm -rf "$d"; mkdir -p "$d"
  cp "$A1/attest.json" "$A1/rollout.jsonl" "$A1/watch.jsonl" "$A1/run.json" "$d/"
  printf '%s' "$d"
}
score11 () {                    # score11 <dir> <label> <want-rc> <want-abort-substring>
  local d=$1 label=$2 wrc=$3 sub=$4 rc
  python3 "$HERE/score.py" "$d" > "$WORK/$label.out" 2>&1; rc=$?
  want "$label rc" "$wrc" "$rc"
  [ -e "$d/receipt.json" ] && bad "$label a receipt was written from an invalid stream" \
    || ok "$label no receipt.json written"
  grep -q "$sub" "$WORK/$label.out" \
    && ok "$label typed abort ($sub)" || { bad "$label no typed abort ($sub)"; cat "$WORK/$label.out"; }
}

D=$(mk11 a); head -c -12 "$A1/rollout.jsonl" > "$D/rollout.jsonl"
score11 "$D" case11a 3 'SCORE-ABORT malformed-rollout'

D=$(mk11 b); cat "$A1/rollout.jsonl" "$A1/rollout.jsonl" > "$D/rollout.jsonl"
score11 "$D" case11b 3 'SCORE-ABORT malformed-rollout duplicate-call-id'

D=$(mk11 c); tac "$A1/rollout.jsonl" > "$D/rollout.jsonl"
score11 "$D" case11c 3 'SCORE-ABORT malformed-rollout'

D=$(mk11 d); rm -f "$D/rollout.jsonl"
printf '{"stale":true}\n' > "$D/receipt.json"; printf 'stale\n' > "$D/receipt.md"
score11 "$D" case11d 3 'SCORE-ABORT missing-rollout'
[ -e "$D/receipt.md" ] && bad "case11d a stale receipt.md survived an rc-3 abort" \
  || ok "case11d the stale receipt.md was deleted too"

D=$(mk11 e); : > "$D/watch.jsonl"
score11 "$D" case11e 3 'SCORE-ABORT empty-watch'

D=$(mk11 f)
python3 -c 'import json,sys;p=sys.argv[1];d=json.load(open(p));d["attest_ok"]=False;d["refusals"]=["injected"];open(p,"w").write(json.dumps(d))' "$D/attest.json"
score11 "$D" case11f 2 'SCORE-ABORT attest-not-ok'

D=$(mk11 g); cat "$A1/watch.jsonl" "$A1/watch.jsonl" > "$D/watch.jsonl"
score11 "$D" case11g 3 'SCORE-ABORT malformed-watch'

echo
echo "anvil-arms self-test: $PASS passed, $FAIL failed  (workdir $WORK)"
[ "$CLEAN" = "1" ] || rm -rf "$WORK"
[ "$FAIL" -eq 0 ] || exit 1
exit 0
