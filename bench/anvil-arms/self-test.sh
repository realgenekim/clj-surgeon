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
#
# Cases 11-21 are the repairs for Sol's executed instrument review of 2026-09-03
# (/home/forge/tmp/sol/arms-sol-review.md), each one written RED first:
#  11  a receipt is emitted only from a stream that validates (truncated, duplicated,
#      reversed, empty watch, attest_ok=false) and every abort deletes a stale receipt
#  12  a tool call with no result is an incomplete-run refusal, not a receipt
#  13  a test runner behind a non-test-named Make target is metered as a test action
#  14  aborting the watcher reaps the driver's WHOLE process group -- no orphans
#  15  the health JSON is validated against independent witnesses, not merely parsed
#  16  the rollout is bound to the session the driver announces, never a newest-mtime
#      glob; an unannounced session is refused rather than guessed
#  17  prose drift in a governing section fails --check; sections are bounded
#  18  the cohort STOPS on the first refused arm; n<1 is refused
#  19  cleanup signals only the pid this invocation spawned, start time verified
#  20  every write path is confined to the runner root and the worktree
#  21  item 7's PASS behaviours, kept as standing witnesses
set -uo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# The runner root for every arm on this box.  run-arm.sh refuses a --root outside it,
# so the self-test runs where a real cohort runs -- not in an ambient system temp dir.
ARMS_ROOT_BASE=/home/forge/tmp/arms
mkdir -p "$ARMS_ROOT_BASE"
# A test suite must leave the source tree exactly as it found it.  Case 17b imports
# build-prompts.py and score.py imports watch.py, both of which would otherwise drop
# __pycache__ into the repo -- and one such .pyc is already tracked, so a self-test run
# showed up as a dirty worktree.
export PYTHONDONTWRITEBYTECODE=1
# THE CALLER'S PORT SCOPE WINS.  The apparatus default (7907-7910) is a range a shared
# box may not entirely own -- on 2026-09-03 another seat's JVM held 7908 and every
# native arm correctly refused with `mcp-absent-proof: cohort port(s) 7908 are
# listening`.  That refusal is the instrument working; scoping the smoke test keeps it
# measuring the apparatus rather than the box's other tenants.
#
# Sol round two, item 10: this used to be a forced assignment, which overrode a
# reviewer's COHORT_PORTS and left them unable to run the checked-in target on a shared
# box at all.  A caller who names their ports is honoured; only an unset COHORT_PORTS
# takes the single-port default.  Every port named is preflighted, and a held port is a
# refusal BEFORE anything is created.
export COHORT_PORTS=${COHORT_PORTS:-7907}
SELFTEST_PORT=${COHORT_PORTS%% *}
for p in $COHORT_PORTS; do
  if ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$p"; then
    echo "SELFTEST-REFUSED: port $p already has a listener; this self-test owns" >&2
    echo "COHORT_PORTS=$COHORT_PORTS.  Stop that server (only one you started), or set" >&2
    echo "COHORT_PORTS to ports you hold, and re-run.  Nothing was executed." >&2
    exit 2
  fi
done

WORK=${ANVIL_ARMS_SELFTEST_DIR:-$(mktemp -d "$ARMS_ROOT_BASE/selftest.XXXXXX")}
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
cat > "$BASE_REPO/Makefile" <<'MK'
KAOCHA = bin/kaocha
verify:
	$(KAOCHA) --focus marvin-voice-remote.bridge3-new-test
build:
	echo building
MK
git -C "$BASE_REPO" init -q
git -C "$BASE_REPO" -c user.email=selftest@anvil -c user.name=selftest add src/fake/sample.clj Makefile
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
  MCP_URL="http://127.0.0.1:$SELFTEST_PORT/mcp" \
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
        --n 3 --prompt-dir "$HERE/prompts" --prompt-prefix E3-P --ports "T=$SELFTEST_PORT" \
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

echo "== case 12: a call with no result is an INCOMPLETE-RUN refusal, not a receipt =="
# Sol, item 3: the watcher flushed the open call as `no-output`, returned 0, and the
# scorer produced a citeable receipt over a run whose last action has no outcome.
run_arm --exp st --rung P --arm N --slot 12 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture partial \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case12.out" 2>&1
rc12=$?
A12="$WORK/st-P-N-12"
want "case12 run-arm rc" 3 "$rc12"
grep -q 'WATCH-ABORT incomplete-run' "$A12/driver.log" \
  && ok "case12 WATCH-ABORT incomplete-run" \
  || { bad "case12 no typed watcher refusal"; tail -5 "$A12/driver.log"; }
want "case12 run.json abort" incomplete-run "$(jqf "$A12/run.json" abort)"
want "case12 run.json calls_without_output" 1 "$(jqf "$A12/run.json" calls_without_output)"
[ -e "$A12/receipt.json" ] && bad "case12 a receipt was written over an incomplete run" \
  || ok "case12 no receipt.json written"
grep -q 'SCORE-ABORT incomplete-run' "$WORK/case12.out" \
  && ok "case12 SCORE-ABORT incomplete-run" || bad "case12 scorer did not refuse the incomplete run"

echo "== case 13: a test runner reached through a non-test-named Make target =="
# Sol, item 4: `make verify` was test_call=false, so a whole kaocha run counted as a
# NON-TEST action -- the exact quantity E3's pass line is stated in.
run_arm --exp st --rung P --arm N --slot 13 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture makeverify \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case13.out" 2>&1
A13="$WORK/st-P-N-13"
if [ -s "$A13/receipt.json" ]; then
  want "case13 total_actions"     1 "$(jqf "$A13/receipt.json" meter.total_actions)"
  want "case13 test_actions"      1 "$(jqf "$A13/receipt.json" meter.test_actions)"
  want "case13 non_test_actions"  0 "$(jqf "$A13/receipt.json" meter.non_test_actions)"
  want "case13 meters agree"   true "$(jqf "$A13/receipt.json" meter.sources.agree)"
else
  bad "case13 no receipt.json written"; cat "$WORK/case13.out"
fi
[ -s "$A13/make-targets.json" ] \
  && ok "case13 make targets resolved at attest time: $(jqf "$A13/make-targets.json" targets.verify)" \
  || bad "case13 no make-targets.json written at attest time"
want "case13 attest records the make map sha" 64 \
     "$(printf '%s' "$(jqf "$A13/attest.json" make_targets_sha256)" | wc -c)"

echo "== case 13b: make-target resolution is a MAP LOOKUP, never a name guess =="
python3 - "$HERE" <<'PY13'
import sys
sys.path.insert(0, sys.argv[1])
from watch import is_test_command
m = {"verify": "bin/kaocha --focus marvin-voice-remote.bridge3-new-test",
     "build": "echo building",
     "ship": "make verify && echo shipped"}
cases = [
    ("make verify", m, True),           # expands to a test runner
    ("make build", m, False),           # expands to something else
    ("make ship", m, True),             # expands through a second target
    ("make verify", None, False),       # no map: never guessed from the name
    ("make test-fast", None, True),     # the name rule still stands on its own
    ("cd sub && make verify", m, True), # still at command position
]
bad = [(c, w, is_test_command(c, make_map=mm)[0]) for c, mm, w in cases
       if is_test_command(c, make_map=mm)[0] != w]
for c, w, got in bad:
    print(f"FAIL case13b {c!r}: expected {w}, got {got}")
print(f"ok   case13b make-target resolution {len(cases)-len(bad)}/{len(cases)}")
sys.exit(1 if bad else 0)
PY13
if [ $? -eq 0 ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi

echo "== case 14: aborting the watcher reaps the driver's WHOLE process group =="
# Sol, item 5: the driver was not placed in its own session, the timeout signalled only
# its pid, and the executed probe left `sleep 60` alive under PPID 1.  An orphan of an
# aborted arm keeps writing into a run nobody is metering any more.
A14="$WORK/st-P-N-14"; mkdir -p "$A14"
git clone -q --no-hardlinks "$BASE_REPO" "$A14/worktree"
printf '%s\n' "$BASE_SHA" > "$A14/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A14/prompt.md"
EXP=st RUNG=P SLOT=14 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A14" N - "" > /dev/null 2>&1
python3 "$HERE/watch.py" --arm "$A14" --zero-return-window 2 --poll 0.2 \
  -- bash "$HERE/fake-driver.sh" "$A14" hang > "$WORK/case14.out" 2>&1
want "case14 watch rc" 4 "$?"
dpid=$(jqf "$A14/run.json" driver_pid)
dpgid=$(jqf "$A14/run.json" driver_pgid)
case "$dpid" in ''|MISSING|null|*[!0-9]*) bad "case14 run.json records no driver_pid: $dpid";;
  *) ok "case14 run.json records driver_pid = $dpid";; esac
want "case14 the driver leads its own process group" "$dpid" "$dpgid"
case "$dpgid" in ''|MISSING|null|*[!0-9]*) bad "case14 run.json records no driver_pgid: $dpgid";;
  *) if pgrep -g "$dpgid" > /dev/null 2>&1; then
       bad "case14 processes survive in group $dpgid: $(pgrep -g "$dpgid" | tr '\n' ' ')"
     else ok "case14 no process left in the driver's group $dpgid"; fi;;
esac
child=$(cat "$A14/fake-driver-child.pid" 2>/dev/null)
if [ -n "$child" ] && kill -0 "$child" 2>/dev/null; then
  bad "case14 ORPHAN: the driver's child $child outlived the abort"
  kill -9 "$child" 2>/dev/null          # a process this self-test started; clean it up
else
  ok "case14 the driver's recorded child (${child:-none}) was reaped by the abort"
fi
want "case14 run.json orphans after the abort" 0 "$(jqf "$A14/run.json" driver_group_orphans)"

echo "== case 15: the health JSON is VALIDATED, not merely parsed =="
# Sol, item 6: `{"ok":false,"pid":999,"project_root":"/wrong/project","port":1}` was
# accepted with attest_ok=true, because the only test applied to it was "does it parse".
# A receipt whose server identity came from a health document about a DIFFERENT process
# is not evidence about the server the arm actually used.
A15="$WORK/st-P-T-15"; mkdir -p "$A15"
git clone -q --no-hardlinks "$BASE_REPO" "$A15/worktree"
WT15=$(cd "$A15/worktree" && pwd -P)
HEAD15=$(git -C "$A15/worktree" rev-parse HEAD)
attest15 () {   # attest15 <label> <healthz-json> <want-rc> <want-reason-substring>
  local label=$1 health=$2 wrc=$3 sub=$4 rc
  rm -f "$A15/attest.json" "$A15/ATTEST-MISMATCH"
  A="$A15" ARM=T PORT=$SELFTEST_PORT EXP=st RUNG=P SLOT=15 MODEL=none DRIVER=fake \
  WORKTREE="$WT15" WORKTREE_HEAD="$HEAD15" BASE="$HEAD15" \
  PROMPT_SHA=deadbeef RUNNER_SHA=deadbeef PORT_IN_RANGE=yes \
  PORT_PID=$$ READY_PID=$$ READY_PROJECT_ROOT="$WT15" \
  SERVER_PROJECT_HEAD="$HEAD15" SERVER_SHA="$HEAD15" EXPECTED_SERVER_SHA="$HEAD15" \
  MCP_URL="http://127.0.0.1:$SELFTEST_PORT/mcp" HEALTHZ="$health" \
    python3 "$HERE/_attest_write.py" > "$WORK/$label.out" 2>&1
  rc=$?
  want "$label rc" "$wrc" "$rc"
  if [ -n "$sub" ]; then
    grep -q "$sub" "$WORK/$label.out" \
      && ok "$label refused on $sub" || { bad "$label wrong refusal ($sub)"; cat "$WORK/$label.out"; }
  fi
}

GOOD15="{\"ok\":true,\"pid\":$$,\"port\":$SELFTEST_PORT,\"project-root\":\"$WT15\"}"
attest15 case15ok  "$GOOD15" 0 ""
want "case15ok attest_ok" true "$(jqf "$A15/attest.json" attest_ok)"

attest15 case15a "{\"ok\":false,\"pid\":$$,\"port\":$SELFTEST_PORT,\"project-root\":\"$WT15\"}" \
         2 'healthz-not-ok'
attest15 case15b "{\"ok\":true,\"pid\":999999,\"port\":$SELFTEST_PORT,\"project-root\":\"$WT15\"}" \
         2 'healthz-pid-ne-port-pid'
attest15 case15c "{\"ok\":true,\"pid\":$$,\"port\":$SELFTEST_PORT,\"project-root\":\"/wrong/project\"}" \
         2 'healthz-project-root-ne-worktree'
attest15 case15d "{\"ok\":true,\"pid\":$$,\"port\":1,\"project-root\":\"$WT15\"}" \
         2 'healthz-port-ne-arm-port'
attest15 case15e "{\"ok\":false,\"pid\":999,\"project_root\":\"/wrong/project\",\"port\":1}" \
         2 'healthz-not-ok'

echo "== case 16: the rollout is bound to THIS session, never to the newest file =="
# Sol, item 8 (BLOCKER): newest-mtime discovery selected rollout-other-session.jsonl and
# then latched it permanently.  Cohort seriality does not stop another Codex session from
# existing, so the binding must come from the session's OWN announced id, inside a
# CODEX_HOME private to this arm.
A16="$WORK/st-P-N-16"; mkdir -p "$A16"
git clone -q --no-hardlinks "$BASE_REPO" "$A16/worktree"
printf '%s\n' "$BASE_SHA" > "$A16/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A16/prompt.md"
EXP=st RUNG=P SLOT=16 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A16" N - "" > /dev/null 2>&1
CH16="$A16/codex-home"
env CODEX_HOME="$CH16" python3 "$HERE/watch.py" --arm "$A16" --codex-home "$CH16" \
  --zero-return-window 30 --poll 0.2 \
  -- env CODEX_HOME="$CH16" bash "$HERE/fake-driver.sh" "$A16" codexsession \
  > "$WORK/case16.out" 2>&1
want "case16 watch rc" 0 "$?"
want "case16 returns metered (3 = this session, 9 = the decoy)" 3 "$(jqf "$A16/run.json" returns)"
want "case16 calls metered"  2 "$(jqf "$A16/run.json" calls)"
rp16=$(jqf "$A16/run.json" rollout_path)
case "$rp16" in
  *11111111-2222-3333-4444-555555555555*) ok "case16 bound to this session's rollout: $(basename "$rp16")";;
  *) bad "case16 bound to the wrong rollout: $rp16";;
esac
want "case16 binding is by session id, not mtime" "session-id:11111111-2222-3333-4444-555555555555" \
     "$(jqf "$A16/run.json" rollout_binding)"
want "case16 the codex home is this arm's own" "$CH16" "$(jqf "$A16/run.json" codex_home)"
[ -f "$CH16/sessions/2026/09/03/rollout-2026-09-03T05-00-01-99999999-8888-7777-6666-555555555555.jsonl" ] \
  && ok "case16 the decoy existed and was newer, and was not metered" \
  || bad "case16 the decoy was never written — the test proves nothing"

echo "== case 16b: a driver that never announces a session is UNBOUND, not guessed =="
A16B="$WORK/st-P-N-16b"; mkdir -p "$A16B"
git clone -q --no-hardlinks "$BASE_REPO" "$A16B/worktree"
printf '%s\n' "$BASE_SHA" > "$A16B/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A16B/prompt.md"
EXP=st RUNG=P SLOT=16b MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A16B" N - "" > /dev/null 2>&1
CH16B="$A16B/codex-home"
mkdir -p "$CH16B/sessions/2026/09/03"
cp "$CH16/sessions/2026/09/03/rollout-2026-09-03T05-00-00-11111111-2222-3333-4444-555555555555.jsonl" \
   "$CH16B/sessions/2026/09/03/rollout-someone-elses.jsonl"
python3 "$HERE/watch.py" --arm "$A16B" --codex-home "$CH16B" \
  --zero-return-window 3 --poll 0.2 \
  -- bash "$HERE/fake-driver.sh" "$A16B" hang > "$WORK/case16b.out" 2>&1
want "case16b watch rc" 7 "$?"
grep -q 'WATCH-ABORT rollout-unbound' "$WORK/case16b.out" \
  && ok "case16b typed refusal: rollout-unbound" \
  || { bad "case16b latched a rollout it was never told about"; cat "$WORK/case16b.out"; }
[ -e "$A16B/receipt.json" ] && bad "case16b a receipt was written for an unbound rollout" \
  || ok "case16b no receipt.json written"
c16b=$(cat "$A16B/fake-driver-child.pid" 2>/dev/null)
[ -n "$c16b" ] && kill -0 "$c16b" 2>/dev/null && { bad "case16b orphan $c16b"; kill -9 "$c16b" 2>/dev/null; } \
  || ok "case16b no orphan left behind"

echo "== case 17: prose drift in the governing sections fails --check =="
# Sol, item 9: fence lookup scanned from a loose heading to EOF, and only the FENCES
# were part of the source contract.  Changing B.4.4 from "exactly three edits" to
# "exactly four edits" produced byte-identical prompts and a passing check -- the doc
# and the installed prompts disagreed about what the prompts ARE, silently.
DOCSRC="$HERE/../../docs/observations/2026-09-04-e3-e6-prestaged.md"
[ -s "$DOCSRC" ] && ok "case17 the pre-registration doc is readable" \
  || bad "case17 cannot read $DOCSRC"
DOC17="$WORK/doc17-clean.md";  cp "$DOCSRC" "$DOC17"
DOC17M="$WORK/doc17-mutated.md"
python3 - "$DOCSRC" "$DOC17M" <<'PY17'
import sys, pathlib
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
text = src.read_text()
old = "exactly three edits"
assert text.count(old) == 1, f"expected one {old!r}, found {text.count(old)}"
dst.write_text(text.replace(old, "exactly four edits"))
PY17
[ $? -eq 0 ] && ok "case17 mutated B.4.4 prose: three edits -> four" || bad "case17 could not mutate the doc"

python3 "$HERE/prompts/build-prompts.py" --check --doc "$DOC17" > "$WORK/case17ctl.out" 2>&1
want "case17 control (unmutated copy) rc" 0 "$?"

python3 "$HERE/prompts/build-prompts.py" --check --doc "$DOC17M" > "$WORK/case17.out" 2>&1
want "case17 mutated-prose rc" 3 "$?"
grep -q 'PROMPT-DRIFT' "$WORK/case17.out" \
  && ok "case17 prose drift is loud: $(head -n1 "$WORK/case17.out")" \
  || { bad "case17 prose changed and --check still passed"; cat "$WORK/case17.out"; }

grep -q '^[0-9a-f]\{64\}  section:B.4.4$' "$HERE/prompts/MANIFEST.sha256" \
  && ok "case17 the manifest hashes B.4.4's governing prose" \
  || { bad "case17 MANIFEST.sha256 carries no section prose hashes"; cat "$HERE/prompts/MANIFEST.sha256"; }

echo "== case 17b: a section's fences are bounded by the NEXT heading =="
python3 - "$HERE" "$DOCSRC" <<'PY17B'
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(sys.argv[1]) / "prompts"))
import importlib.util
spec = importlib.util.spec_from_file_location(
    "bp", str(pathlib.Path(sys.argv[1]) / "prompts" / "build-prompts.py"))
bp = importlib.util.module_from_spec(spec); spec.loader.exec_module(bp)
doc = pathlib.Path(sys.argv[2]).read_text()
fails = []
# B.4.4 owns exactly two fences; a fence lookup that runs to EOF would find many more
blocks = bp.fences_in(bp.section(doc, "### B.4.4 "))
if len(blocks) != 2:
    fails.append(f"B.4.4 has {len(blocks)} fences inside its own section, expected 2")
# asking for a fence the section does not own must FAIL, not reach into the next one
try:
    bp.fence_in_section(doc, "### B.4.4 ", which=3)
    fails.append("fence #3 of B.4.4 resolved — the lookup escaped the section")
except bp.BuildError:
    pass
if bp.section(doc, "### B.4.2 ").count("### B.4.3") != 0:
    fails.append("the B.4.2 section bleeds into B.4.3")
for f in fails:
    print(f"FAIL case17b {f}")
print(f"ok   case17b section bounds hold ({3-len(fails)}/3)")
sys.exit(1 if fails else 0)
PY17B
if [ $? -eq 0 ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi

echo "== case 18: the cohort STOPS on the first refused arm =="
# Sol, item 10: the loop recorded failure and never broke.  Executed at n=1: T emitted
# ATTEST-MISMATCH, then N launched anyway and wrote a receipt.  An arm-run is the scarce
# resource this whole apparatus exists to protect; spending the rest of a cohort after
# the instrument has already refused once is spending it on evidence nobody may cite.
C18="$WORK/cohort18"
bash "$HERE/run-cohort.sh" --root "$C18" --exp st --rung P --arms N,T --n 1 \
     --prompt-dir "$HERE/prompts" --prompt-prefix E3-P --ports "T=$SELFTEST_PORT" \
     --driver fake --fixture pf5 --worktree-src "$BASE_REPO" \
     --base 0000000000000000000000000000000000000000 > "$WORK/case18.out" 2>&1
rc18=$?
[ "$rc18" -ne 0 ] && ok "case18 cohort rc = $rc18 (nonzero)" \
  || bad "case18 cohort exited 0 after an arm was refused"
grep -q 'COHORT-ABORT' "$WORK/case18.out" \
  && ok "case18 typed abort: $(grep -m1 'COHORT-ABORT' "$WORK/case18.out")" \
  || { bad "case18 no COHORT-ABORT line"; cat "$WORK/case18.out"; }
[ -s "$C18/st-P-N-1/ATTEST-MISMATCH" ] \
  && ok "case18 the first arm did refuse (the premise of the test)" \
  || bad "case18 the first arm did NOT refuse — the test proves nothing"
[ -e "$C18/st-P-T-1" ] \
  && bad "case18 the next arm launched after a refusal: $(ls "$C18/st-P-T-1")" \
  || ok "case18 no arm ran after the refusal"

echo "== case 18b: a cohort of n<1 is refused, not silently empty =="
# Sol, item 10, second half: n=0 exited SUCCESSFULLY with an empty cohort -- a green
# receipt over zero evidence, which is the verdict-label-was-a-noun defect again.
C18B="$WORK/cohort18b"
bash "$HERE/run-cohort.sh" --root "$C18B" --exp st --rung P --arms N,T --n 0 \
     --prompt-dir "$HERE/prompts" --prompt-prefix E3-P --ports "T=$SELFTEST_PORT" \
     --dry-run > "$WORK/case18b.out" 2>&1
want "case18b n=0 rc" 64 "$?"
grep -q 'run-cohort: --n' "$WORK/case18b.out" \
  && ok "case18b typed refusal of n=0" || { bad "case18b n=0 was accepted"; cat "$WORK/case18b.out"; }
for bad_n in -1 abc 1.5; do
  bash "$HERE/run-cohort.sh" --root "$C18B" --exp st --rung P --arms N,T --n "$bad_n" \
       --prompt-dir "$HERE/prompts" --prompt-prefix E3-P --ports "T=$SELFTEST_PORT" \
       --dry-run > "$WORK/case18b-$bad_n.out" 2>&1
  want "case18b n=$bad_n rc" 64 "$?"
done

echo "== case 19: cleanup signals ONLY the server this invocation spawned =="
# Sol, item 11: SERVER_STARTED=1 was not tied to the spawned pid, and cleanup read
# whatever pid happened to be in ready.edn -- a stale file, or one written by another
# arm, could make this script signal a process it never started.  A pid is not an
# identity: pids are reused, so the recorded start time is checked too.
starttime_of () { cut -d')' -f2 "/proc/$1/stat" 2>/dev/null | awk '{print $20}'; }
# A spawn record is (pid, start ticks, boot id): start ticks are counted from THIS boot
# and repeat across reboots, so the boot id is part of the identity (Sol round two,
# item 8, pinned by case 29).
BOOT_ID=$(cat /proc/sys/kernel/random/boot_id 2>/dev/null)

# 19a -- a pid this test really started, correctly recorded: it must be stopped
A19A="$WORK/st-P-T-19a"; mkdir -p "$A19A/server"
sleep 300 & P19A=$!
printf '%s %s %s\n' "$P19A" "$(starttime_of "$P19A")" "$BOOT_ID" > "$A19A/server/spawned.pid"
printf '{:ok true :pid %s :project-root "%s"}\n' "$P19A" "$A19A" > "$A19A/server/ready.edn"
bash "$HERE/stop-server.sh" "$A19A" > "$WORK/case19a.out" 2>&1
want "case19a stop-server rc" 0 "$?"
sleep 1
kill -0 "$P19A" 2>/dev/null \
  && { bad "case19a the server this script started ($P19A) was not stopped"; kill -9 "$P19A" 2>/dev/null; } \
  || ok "case19a the spawned server $P19A was stopped"

# 19b -- NO spawned.pid, only a ready.edn naming a live process: signal NOTHING
A19B="$WORK/st-P-T-19b"; mkdir -p "$A19B/server"
sleep 300 & P19B=$!
printf '{:ok true :pid %s :project-root "%s"}\n' "$P19B" "$A19B" > "$A19B/server/ready.edn"
bash "$HERE/stop-server.sh" "$A19B" > "$WORK/case19b.out" 2>&1
rc19b=$?
[ "$rc19b" -ne 0 ] && ok "case19b refused (rc $rc19b) with no recorded spawn" \
  || bad "case19b returned 0 while signalling nothing was the only safe act"
sleep 1
kill -0 "$P19B" 2>/dev/null \
  && ok "case19b the unrelated process $P19B was NOT signalled" \
  || bad "case19b signalled a process this invocation never started"
grep -q 'no recorded spawn' "$WORK/case19b.out" \
  && ok "case19b typed refusal" || { bad "case19b untyped refusal"; cat "$WORK/case19b.out"; }
kill -9 "$P19B" 2>/dev/null

# 19c -- the recorded pid is live but its START TIME differs: a REUSED pid, not our server
A19C="$WORK/st-P-T-19c"; mkdir -p "$A19C/server"
sleep 300 & P19C=$!
printf '%s %s %s\n' "$P19C" "$(( $(starttime_of "$P19C") + 4242 ))" "$BOOT_ID" \
  > "$A19C/server/spawned.pid"
bash "$HERE/stop-server.sh" "$A19C" > "$WORK/case19c.out" 2>&1
rc19c=$?
[ "$rc19c" -ne 0 ] && ok "case19c refused (rc $rc19c) on a start-time mismatch" \
  || bad "case19c signalled a pid whose start time does not match the recorded spawn"
sleep 1
kill -0 "$P19C" 2>/dev/null \
  && ok "case19c the reused pid $P19C was NOT signalled" \
  || bad "case19c killed a process that merely inherited the pid"
grep -q 'start-time-mismatch' "$WORK/case19c.out" \
  && ok "case19c typed refusal: start-time-mismatch" \
  || { bad "case19c untyped refusal"; cat "$WORK/case19c.out"; }
kill -9 "$P19C" 2>/dev/null

# 19d -- run-arm.sh records what it spawned
grep -q 'spawned.pid' "$HERE/run-arm.sh" \
  && ok "case19d run-arm.sh records the pid it spawned" \
  || bad "case19d run-arm.sh still trusts whatever pid ready.edn holds"

echo "== case 20: every write path is confined to the runner root and the worktree =="
# Sol, item 12: --root was unconstrained, the sol driver wrote into $HOME/.codex/sessions,
# and --check built into the system temp dir.  An apparatus whose write paths are wherever
# the caller points it cannot say afterwards which bytes were part of the experiment.
ARMS_BASE=/home/forge/tmp/arms

bash "$HERE/run-arm.sh" --root /tmp/anvil-arms-escape --exp st --rung P --arm N --slot 20 \
     --prompt "$HERE/prompts/E3-P-N.md" --driver fake --dry-run > "$WORK/case20a.out" 2>&1
want "case20a --root outside $ARMS_BASE rc" 2 "$?"
grep -q 'ROOT-REFUSED' "$WORK/case20a.out" \
  && ok "case20a typed refusal: $(grep -m1 ROOT-REFUSED "$WORK/case20a.out")" \
  || { bad "case20a an unconfined --root was accepted"; cat "$WORK/case20a.out"; }
[ -e /tmp/anvil-arms-escape ] && bad "case20a it created /tmp/anvil-arms-escape anyway" \
  || ok "case20a nothing was created outside the runner root"

bash "$HERE/run-arm.sh" --root "$ARMS_BASE/../arms-escape-via-dotdot" --exp st --rung P \
     --arm N --slot 20 --prompt "$HERE/prompts/E3-P-N.md" --driver fake --dry-run \
     > "$WORK/case20a2.out" 2>&1
want "case20a2 a ../ escape rc" 2 "$?"
grep -q 'ROOT-REFUSED' "$WORK/case20a2.out" \
  && ok "case20a2 the refusal is on the RESOLVED path, not the spelling" \
  || bad "case20a2 a ../ escape slipped through"

case "$WORK" in "$ARMS_BASE"/*) ok "case20b this self-test itself runs under $ARMS_BASE";;
  *) bad "case20b the self-test workdir $WORK is outside $ARMS_BASE";; esac

# The prompt check must BUILD SOMEWHERE IT NAMES, under the apparatus, not into an
# ambient system temp dir.  (Probing this with TMPDIR does not work: Python's tempfile
# silently falls back to /tmp when TMPDIR is unusable, so the test would pass either way.
# The honest test is that the tool states the directory it used.)
python3 "$HERE/prompts/build-prompts.py" --check > "$WORK/case20c.out" 2>&1
want "case20c --check rc" 0 "$?"
cdir=$(sed -n 's/^check-dir: //p' "$WORK/case20c.out" | head -n1)
if [ -z "$cdir" ]; then
  bad "case20c --check does not say which directory it built into"; cat "$WORK/case20c.out"
else
  case "$cdir" in
    "$HERE"/*) ok "case20c --check built under the apparatus: $cdir";;
    *) bad "case20c --check built outside the apparatus: $cdir";;
  esac
  [ -e "$cdir" ] && bad "case20c --check left $cdir behind" || ok "case20c --check removed its build dir"
fi

# no apparatus file may reach a shared codex home (comments about its removal excepted)
grep -rn 'codex/sessions' "$HERE" --include='*.sh' --include='*.py' 2>/dev/null \
  | grep -v '/self-test.sh:' | grep -vE ':[0-9]+:[[:space:]]*#' > "$WORK/case20d.out"
[ -s "$WORK/case20d.out" ] \
  && { bad "case20d a shared codex sessions path survives"; cat "$WORK/case20d.out"; } \
  || ok "case20d no apparatus file writes or reads a shared codex sessions dir"

echo "== case 21: item 7's witnesses — a native arm refuses a LISTENING cohort port =="
# Sol marked attest.sh PASS: "a native arm observing an owned 7907 listener also refused",
# and "inaccessible server-source identity became server_sha=unverified and triggered
# ATTEST-MISMATCH".  Those are the two behaviours the COHORT_PORTS scoping above touches,
# so they get standing witnesses rather than a note in a review.
A21="$WORK/st-P-N-21"; mkdir -p "$A21"
git clone -q --no-hardlinks "$BASE_REPO" "$A21/worktree"
printf '%s\n' "$BASE_SHA" > "$A21/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A21/prompt.md"
# a listener on 7907 -- a process THIS TEST starts, whose pid it records and reaps
python3 -c 'import socket,sys,time
s=socket.socket(); s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind(("127.0.0.1", int(sys.argv[1]))); s.listen(1); time.sleep(30)' "$SELFTEST_PORT" &
L21=$!
for _ in 1 2 3 4 5 6 7 8 9 10; do
  ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$SELFTEST_PORT" && break
  sleep 0.3
done
if ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$SELFTEST_PORT"; then
  ok "case21 a cohort-port listener is up (pid $L21, port $SELFTEST_PORT)"
  EXP=st RUNG=P SLOT=21 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
    bash "$HERE/attest.sh" "$A21" N - "" > "$WORK/case21.out" 2>&1
  want "case21 native arm attest rc" 2 "$?"
  grep -q "cohort port(s) $SELFTEST_PORT are listening" "$WORK/case21.out" \
    && ok "case21 refused: a stale arm server on a cohort port contaminates a native arm" \
    || { bad "case21 wrong refusal"; cat "$WORK/case21.out"; }
  want "case21 attest_ok" false "$(jqf "$A21/attest.json" attest_ok)"
else
  bad "case21 could not bind $SELFTEST_PORT to run the witness"
fi
kill "$L21" 2>/dev/null; wait "$L21" 2>/dev/null
ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$SELFTEST_PORT" \
  && bad "case21 the listener this test started is still up" \
  || ok "case21 the listener this test started was stopped"

echo "== case 21b: an unreadable server source is server_sha=unverified, and refuses =="
A21B="$WORK/st-P-T-21b"; mkdir -p "$A21B"
git clone -q --no-hardlinks "$BASE_REPO" "$A21B/worktree"
WT21=$(cd "$A21B/worktree" && pwd -P)
HEAD21=$(git -C "$A21B/worktree" rev-parse HEAD)
A="$A21B" ARM=T PORT=$SELFTEST_PORT EXP=st RUNG=P SLOT=21b MODEL=none DRIVER=fake \
WORKTREE="$WT21" WORKTREE_HEAD="$HEAD21" BASE="$HEAD21" \
PROMPT_SHA=deadbeef RUNNER_SHA=deadbeef PORT_IN_RANGE=yes \
PORT_PID=$$ READY_PID=$$ READY_PROJECT_ROOT="$WT21" SERVER_PROJECT_HEAD="$HEAD21" \
SERVER_SHA="" EXPECTED_SERVER_SHA="$HEAD21" MCP_URL="http://127.0.0.1:$SELFTEST_PORT/mcp" \
HEALTHZ="{\"ok\":true,\"pid\":$$,\"port\":$SELFTEST_PORT,\"project-root\":\"$WT21\"}" \
  python3 "$HERE/_attest_write.py" > "$WORK/case21b.out" 2>&1
want "case21b rc" 2 "$?"
grep -q 'server-sha-unverified' "$WORK/case21b.out" \
  && ok "case21b refused on server-sha-unverified" \
  || { bad "case21b an unreadable server source attested"; cat "$WORK/case21b.out"; }
want "case21b server_sha" unverified "$(jqf "$A21B/attest.json" server_sha)"

echo "== case 22: the make map is a STATIC PARSE — attest never executes the Makefile =="
# Sol round two, item 1: the map was generated with `make -n`, which PARSES the whole
# Makefile.  A `:=` assignment holding $(shell …) ran at attest time, and a recipe line
# prefixed `+` runs even under -n.  Sol's probe watched both fire: repo-controlled code
# executed inside the attestation whose whole job is to decide whether the repo may run.
MK22="$WORK/mk22"; mkdir -p "$MK22"
{ printf 'KAOCHA = bin/kaocha\n'
  printf 'SIDE := $(shell touch %s/shell-expansion-ran)\n' "$MK22"
  printf 'verify:\n\t$(KAOCHA) --focus marvin-voice-remote.bridge3-new-test\n'
  printf 'recurse:\n\t+$(MAKE) --no-print-directory build\n\t+touch %s/recursive-recipe-ran\n' "$MK22"
  printf 'build:\n\techo building\n'
} > "$MK22/Makefile"
python3 "$HERE/_make_targets.py" "$MK22" "$MK22/map.json" > "$WORK/case22.out" 2>&1
want "case22 map rc" 0 "$?"
[ -e "$MK22/shell-expansion-ran" ] \
  && bad "case22 attest-time mapping EXECUTED \$(shell …)" \
  || ok "case22 \$(shell …) was parsed, never executed"
[ -e "$MK22/recursive-recipe-ran" ] \
  && bad "case22 attest-time mapping EXECUTED a + recipe line" \
  || ok "case22 the + recipe lines were parsed, never executed"
want "case22 parser" static "$(jqf "$MK22/map.json" parser)"
want "case22 verify resolves through \$(KAOCHA)" \
     "bin/kaocha --focus marvin-voice-remote.bridge3-new-test" \
     "$(jqf "$MK22/map.json" targets.verify)"
want "case22 build resolves" "echo building" "$(jqf "$MK22/map.json" targets.build)"
want "case22 recurse is REFUSED, not resolved" 'dynamic:+$(MAKE)' \
     "$(jqf "$MK22/map.json" refused.recurse)"
want "case22 recurse is not in the resolved map" null \
     "$(jqf "$MK22/map.json" targets.recurse)"

# 22b -- a hard `include` of a file make would GENERATE: the parse cannot see those
# targets at all, so the whole map is untrustworthy and the arm never launches.
MK22B="$WORK/mk22b"; mkdir -p "$MK22B"
{ printf 'include generated.mk\n'
  printf 'generated.mk:\n\techo "verify:" > generated.mk\n'
  printf 'build:\n\techo building\n'
} > "$MK22B/Makefile"
python3 "$HERE/_make_targets.py" "$MK22B" "$MK22B/map.json" > "$WORK/case22b.out" 2>&1
want "case22b map rc" 4 "$?"
case "$(jqf "$MK22B/map.json" dynamic_refusal)" in
  include-generated*) ok "case22b typed whole-map refusal: $(jqf "$MK22B/map.json" dynamic_refusal)";;
  *) bad "case22b an include of a generated file was accepted: $(jqf "$MK22B/map.json" dynamic_refusal)";;
esac
A22B="$WORK/st-P-N-22b"; mkdir -p "$A22B"
git clone -q --no-hardlinks "$BASE_REPO" "$A22B/worktree"
printf '%s\n' "$BASE_SHA" > "$A22B/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A22B/prompt.md"
cp "$MK22B/map.json" "$A22B/make-targets.json"
EXP=st RUNG=P SLOT=22b MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
MAKE_TARGETS="$A22B/make-targets.json" \
  bash "$HERE/attest.sh" "$A22B" N - "" > "$WORK/case22b-attest.out" 2>&1
want "case22b attest rc" 2 "$?"
grep -q 'makefile-dynamic' "$WORK/case22b-attest.out" \
  && ok "case22b ATTEST-MISMATCH makefile-dynamic — the driver is never launched" \
  || { bad "case22b an untrustworthy make map still attested"; cat "$WORK/case22b-attest.out"; }

# 22c -- a target defined inside a conditional: which recipe make would pick depends on
# the environment the driver runs in, so it is refused rather than guessed.
MK22C="$WORK/mk22c"; mkdir -p "$MK22C"
{ printf 'ifeq ($(CI),1)\n'
  printf 'conditional:\n\techo ci\n'
  printf 'else\n'
  printf 'conditional:\n\tbin/kaocha --focus x\n'
  printf 'endif\n'
  printf 'build:\n\techo building\n'
} > "$MK22C/Makefile"
python3 "$HERE/_make_targets.py" "$MK22C" "$MK22C/map.json" > "$WORK/case22c.out" 2>&1
want "case22c map rc" 0 "$?"
want "case22c conditional target refused" conditional \
     "$(jqf "$MK22C/map.json" refused.conditional)"
want "case22c conditional target not resolved" null \
     "$(jqf "$MK22C/map.json" targets.conditional)"
want "case22c an unconditional target beside it still resolves" "echo building" \
     "$(jqf "$MK22C/map.json" targets.build)"

echo "== case 24: ANY watcher abort refuses at score time — no receipt, ever =="
# Sol round two, item 3: an abort was merely appended to `notes`.  Sol's probe stopped a
# run on the idle timeout -- watcher rc 5 -- and the scorer returned 0 and wrote a
# citeable receipt over a run the meter had given up on.  A receipt whose own notes say
# the meter stopped is worse than no receipt: it terminates the investigation.
mk24 () {                       # mk24 <suffix> -> a fresh arm dir holding case 1's evidence
  local d="$WORK/st-P-N-24$1"
  rm -rf "$d"; mkdir -p "$d"
  cp "$A1/attest.json" "$A1/rollout.jsonl" "$A1/watch.jsonl" "$A1/run.json" "$d/"
  printf '%s' "$d"
}
inject_abort () {               # inject_abort <dir> <error_type>  (before the final end)
  python3 - "$1/watch.jsonl" "$2" <<'PY24'
import json, sys
path, kind = sys.argv[1], sys.argv[2]
recs = [json.loads(l) for l in open(path) if l.strip()]
end = recs.pop() if recs and recs[-1].get("kind") == "end" else None
ms = recs[-1]["ms_since_start"] if recs else 0
recs.append({"t": "2026-09-03T00:00:00Z", "ms_since_start": ms, "kind": "abort",
             "error_type": kind, "detail": "injected by case 24", "returns": 3})
if end is not None:
    recs.append(end)
open(path, "w").write("".join(json.dumps(r) + "\n" for r in recs))
PY24
}

for kind in idle-stop max-wall zero-returns rollout-rotated; do
  D=$(mk24 "-$kind")
  inject_abort "$D" "$kind"
  python3 -c 'import json,sys;p=sys.argv[1];d=json.load(open(p));d["abort"]=sys.argv[2];open(p,"w").write(json.dumps(d,indent=2))' \
    "$D/run.json" "$kind"
  printf '{"stale":true}\n' > "$D/receipt.json"
  python3 "$HERE/score.py" "$D" > "$WORK/case24-$kind.out" 2>&1
  want "case24 $kind score rc" 3 "$?"
  [ -e "$D/receipt.json" ] && bad "case24 $kind a receipt survived a watcher abort" \
    || ok "case24 $kind no receipt.json — the stale one was removed too"
  grep -q "SCORE-ABORT watch-abort:$kind" "$WORK/case24-$kind.out" \
    && ok "case24 $kind typed refusal names the abort" \
    || { bad "case24 $kind untyped refusal"; cat "$WORK/case24-$kind.out"; }
  want "case24 $kind run.json still carries the abort as the terminal fact" "$kind" \
       "$(jqf "$D/run.json" abort)"
done

# run.json alone is enough: an abort recorded there and nowhere else still refuses
D=$(mk24 -runjson)
python3 -c 'import json,sys;p=sys.argv[1];d=json.load(open(p));d["abort"]="idle-stop";open(p,"w").write(json.dumps(d,indent=2))' \
  "$D/run.json"
python3 "$HERE/score.py" "$D" > "$WORK/case24-runjson.out" 2>&1
want "case24 run.json-only abort score rc" 3 "$?"
[ -e "$D/receipt.json" ] && bad "case24 run.json recorded an abort and a receipt was written" \
  || ok "case24 run.json-only abort: no receipt.json written"

# the control: the same evidence with no abort anywhere still scores
D=$(mk24 -control)
python3 "$HERE/score.py" "$D" > "$WORK/case24-control.out" 2>&1
want "case24 control (no abort) score rc" 0 "$?"
[ -s "$D/receipt.json" ] && ok "case24 control still produces a receipt" \
  || bad "case24 control lost its receipt — the refusal is too broad"

echo "== case 25: a watch stream with no final `end` is unterminated, not scoreable =="
# Sol round two, item 4: validate_watch documents "one final `end`" in its own docstring
# and never checks for it.  Sol deleted the end record from a good stream and the scorer
# returned 0 with a receipt.  The `end` record IS the completion stamp -- driver rc and
# wall are carried nowhere else in the stream -- so a stream without one is a run whose
# ending nobody witnessed, and wall_s on that receipt would be a number about a moment
# the meter never observed.
mk25 () {
  local d="$WORK/st-P-N-25$1"
  rm -rf "$d"; mkdir -p "$d"
  cp "$A1/attest.json" "$A1/rollout.jsonl" "$A1/watch.jsonl" "$A1/run.json" "$d/"
  printf '%s' "$d"
}
edit_end () {                   # edit_end <dir> drop|strip-rc|strip-wall
  python3 - "$1/watch.jsonl" "$2" <<'PY25'
import json, sys
path, how = sys.argv[1], sys.argv[2]
recs = [json.loads(l) for l in open(path) if l.strip()]
assert recs[-1].get("kind") == "end", f"fixture has no end record: {recs[-1]}"
if how == "drop":
    recs.pop()
elif how == "strip-rc":
    recs[-1].pop("driver_rc", None)
elif how == "strip-wall":
    recs[-1].pop("wall_s", None)
open(path, "w").write("".join(json.dumps(r) + "\n" for r in recs))
PY25
}
for how in drop strip-rc strip-wall; do
  D=$(mk25 "-$how")
  edit_end "$D" "$how" || bad "case25 $how could not edit the fixture"
  printf '{"stale":true}\n' > "$D/receipt.json"
  python3 "$HERE/score.py" "$D" > "$WORK/case25-$how.out" 2>&1
  want "case25 $how score rc" 3 "$?"
  grep -q 'watch-unterminated' "$WORK/case25-$how.out" \
    && ok "case25 $how typed refusal: watch-unterminated" \
    || { bad "case25 $how untyped refusal"; cat "$WORK/case25-$how.out"; }
  [ -e "$D/receipt.json" ] && bad "case25 $how a receipt survived an unterminated stream" \
    || ok "case25 $how no receipt.json — the stale one was removed too"
done
D=$(mk25 -control)
python3 "$HERE/score.py" "$D" > "$WORK/case25-control.out" 2>&1
want "case25 control (end intact) score rc" 0 "$?"
want "case25 control wall_s comes from the end stamp" \
     "$(jqf "$A1/run.json" wall_s)" "$(jqf "$D/receipt.json" meter.wall_s)"

echo "== case 23: a rollout REPLACED mid-run is a typed abort, never a split-brain receipt =="
# Sol round two, item 2: the watcher held an open fd on the original inode while the
# retained copy was taken BY PATH from the replacement.  The two witnesses were derived
# from two DIFFERENT FILES and, because each was internally consistent, the receipt
# asserted sources.agree=true and reported writes.via_verb=1 for a session whose
# surviving bytes contain no verb call at all.  Sol's rc-0 receipt is the artifact.
A23="$WORK/st-P-N-23"; mkdir -p "$A23"
git clone -q --no-hardlinks "$BASE_REPO" "$A23/worktree"
printf '%s\n' "$BASE_SHA" > "$A23/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A23/prompt.md"
EXP=st RUNG=P SLOT=23 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A23" N - "" > /dev/null 2>&1
CH23="$A23/codex-home"
env CODEX_HOME="$CH23" python3 "$HERE/watch.py" --arm "$A23" --codex-home "$CH23" \
  --zero-return-window 30 --poll 0.2 \
  -- env CODEX_HOME="$CH23" bash "$HERE/fake-driver.sh" "$A23" rotate \
  > "$WORK/case23.out" 2>&1
want "case23 watch rc" 8 "$?"
grep -q 'WATCH-ABORT rollout-rotated' "$WORK/case23.out" \
  && ok "case23 typed refusal: rollout-rotated" \
  || { bad "case23 a replaced rollout was metered as one file"; cat "$WORK/case23.out"; }
want "case23 run.json carries the abort" rollout-rotated "$(jqf "$A23/run.json" abort)"
[ -e "$A23/receipt.json" ] && bad "case23 a receipt was written over two different files" \
  || ok "case23 no receipt.json written"
# the retained copy must be the bytes the watcher actually metered -- taken from its own
# open fd -- not whatever the path names afterwards
if [ -s "$A23/rollout.jsonl" ]; then
  grep -q alias_migration "$A23/rollout.jsonl" \
    && ok "case23 the retained rollout is the inode the watcher read" \
    || bad "case23 the retained rollout came from the replacement inode"
  grep -q 'a replacement file nobody metered' "$A23/rollout.jsonl" \
    && bad "case23 the retained rollout holds bytes from the replacement inode" \
    || ok "case23 no replacement bytes leaked into the retained rollout"
else
  bad "case23 nothing was retained at all"
fi
python3 "$HERE/score.py" "$A23" > "$WORK/case23score.out" 2>&1
want "case23 score rc" 3 "$?"
[ -e "$A23/receipt.json" ] && bad "case23 the scorer wrote a receipt over an aborted watch" \
  || ok "case23 the scorer refuses an aborted watch and writes no receipt"

echo "== case 26: a descendant that leaves the PGID via setsid is still reaped, and COUNTED =="
# Sol round two, item 5: cleanup signals the driver's process GROUP, so a descendant
# that calls setsid is invisible to it.  Sol's probe left pid 3289785 alive after the
# abort while run.json reported driver_group_orphans=0 -- a number that was not measured
# but assumed, over a process still working inside a run nobody was metering any more.
A26="$WORK/st-P-N-26"; mkdir -p "$A26"
git clone -q --no-hardlinks "$BASE_REPO" "$A26/worktree"
printf '%s\n' "$BASE_SHA" > "$A26/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A26/prompt.md"
EXP=st RUNG=P SLOT=26 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A26" N - "" > /dev/null 2>&1
python3 "$HERE/watch.py" --arm "$A26" --zero-return-window 3 --poll 0.2 \
  -- bash "$HERE/fake-driver.sh" "$A26" setsidhang > "$WORK/case26.out" 2>&1
want "case26 watch rc" 4 "$?"
esc=$(cat "$A26/fake-driver-setsid.pid" 2>/dev/null)
if [ -z "$esc" ]; then
  bad "case26 the fixture never recorded its setsid pid — the test proves nothing"
else
  ok "case26 the fixture recorded the escaping pid $esc"
  sleep 1
  if kill -0 "$esc" 2>/dev/null; then
    bad "case26 ORPHAN: the setsid descendant $esc outlived the abort"
    kill -9 "$esc" 2>/dev/null          # a process this self-test started; clean it up
  else
    ok "case26 the setsid descendant $esc was reaped by the abort"
  fi
fi
child=$(cat "$A26/fake-driver-child.pid" 2>/dev/null)
[ -n "$child" ] && kill -0 "$child" 2>/dev/null \
  && { bad "case26 the in-group child $child outlived the abort"; kill -9 "$child" 2>/dev/null; } \
  || ok "case26 the in-group child (${child:-none}) was reaped too"
# the count must be MEASURED from a final /proc scan of the pids actually recorded,
# never the zero a group walk returns because it cannot see outside its own group
dr=$(jqf "$A26/run.json" descendants_recorded)
case "$dr" in ''|MISSING|null|0|*[!0-9]*) bad "case26 run.json recorded no descendants: $dr";;
  *) ok "case26 run.json recorded $dr descendant pid(s) while the driver lived";; esac
want "case26 run.json orphans_after_reap" 0 "$(jqf "$A26/run.json" orphans_after_reap)"
want "case26 run.json names the surviving pids" "[]" "$(jqf "$A26/run.json" orphan_pids)"

echo "== case 27: a Make target the map does not resolve makes the run INCOMPLETE =="
# Sol round two, item 6: an unknown or conditional target fell through to the name rule
# and was metered as one more NON-TEST ACTION.  Sol's probes -- `make ghost` and
# `make conditional` -- both produced rc-0 receipts reading test_actions=0,
# non_test_actions=1.  Non-test actions is the exact quantity E3's pass line is stated
# in, so an unmetered test run does not merely go missing: it lands on the other side
# of the comparison.
run_arm --exp st --rung P --arm N --slot 27 --prompt "$HERE/prompts/E3-P-N.md" \
        --worktree-src "$BASE_REPO" --base "$BASE_SHA" --fixture makeunknown \
        --watch-arg --zero-return-window --watch-arg 30 > "$WORK/case27.out" 2>&1
rc27=$?
A27="$WORK/st-P-N-27"
want "case27 run-arm rc" 3 "$rc27"
grep -q 'WATCH-ABORT incomplete-run' "$A27/driver.log" \
  && ok "case27 WATCH-ABORT incomplete-run" \
  || { bad "case27 an unresolvable make target was metered as a non-test action"; tail -5 "$A27/driver.log"; }
want "case27 run.json abort" incomplete-run "$(jqf "$A27/run.json" abort)"
want "case27 run.json names the unresolved target" '["ghost"]' \
     "$(jqf "$A27/run.json" unresolved_make_targets)"
[ -e "$A27/receipt.json" ] && bad "case27 a receipt was written over an unmetered action" \
  || ok "case27 no receipt.json written"
grep -q 'SCORE-ABORT incomplete-run' "$WORK/case27.out" \
  && ok "case27 SCORE-ABORT incomplete-run names it too" \
  || { bad "case27 the scorer did not refuse"; cat "$WORK/case27.out"; }

echo "== case 27b: which targets are unresolved is a MAP LOOKUP, and refusal counts =="
python3 - "$HERE" <<'PY27'
import sys
sys.path.insert(0, sys.argv[1])
from watch import unresolved_make_targets as u
m = {"verify": "bin/kaocha --focus x", "build": "echo building",
     "ship": "make verify && echo shipped"}
cases = [
    ("make verify", m, []),                       # resolved
    ("make build && make verify", m, []),         # both resolved
    ("make ghost", m, ["ghost"]),                 # never declared
    ("make conditional", m, ["conditional"]),     # declared but REFUSED by the parser
    ("bash -lc 'make ghost'", m, ["ghost"]),      # still found at command position
    ("make", m, ["(default-goal)"]),              # which goal? the map cannot say
    ("make V=1 verify", m, []),                   # a variable override is not a target
    ("make verify", None, ["verify"]),            # no map at all resolves nothing
    ("bin/kaocha --focus x", m, []),              # not a make invocation
]
bad = [(c, w, u(c, mm)) for c, mm, w in cases if u(c, mm) != w]
for c, w, got in bad:
    print(f"FAIL case27b {c!r}: expected {w}, got {got}")
print(f"ok   case27b unresolved-target lookup {len(cases)-len(bad)}/{len(cases)}")
sys.exit(1 if bad else 0)
PY27
if [ $? -eq 0 ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi

echo "== case 28: the B.4 PARENT paragraph is part of the source contract =="
# Sol round two, item 9/7: the manifest hashes the prose of A.8, B.4.1, B.4.2, B.4.3
# and B.4.4 -- and the load-bearing sentence sits in NONE of them.  "Both arms are
# byte-identical outside §5" is the parent paragraph every B.4.x block is written
# under: it is what makes the pair a controlled comparison at all.  Sol changed it to
# permit differences and `--check` returned 0, because every fence, every prompt and
# every prompt hash was byte-identical.
DOC28M="$WORK/doc28-mutated.md"
python3 - "$DOCSRC" "$DOC28M" <<'PY28'
import sys, pathlib
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
text = src.read_text()
old = "Both arms are **byte-identical outside §5**."
assert text.count(old) == 1, f"expected one {old!r}, found {text.count(old)}"
dst.write_text(text.replace(
    old, "The two arms **may differ anywhere**, at the runner's discretion."))
PY28
[ $? -eq 0 ] && ok "case28 mutated the B.4 parent paragraph to permit differences" \
  || bad "case28 could not mutate the doc"

python3 "$HERE/prompts/build-prompts.py" --check --doc "$DOC17" > "$WORK/case28ctl.out" 2>&1
want "case28 control (unmutated copy) rc" 0 "$?"
python3 "$HERE/prompts/build-prompts.py" --check --doc "$DOC28M" > "$WORK/case28.out" 2>&1
want "case28 mutated-parent rc" 3 "$?"
grep -q 'PROMPT-DRIFT' "$WORK/case28.out" \
  && ok "case28 the parent paragraph is hashed: $(head -n1 "$WORK/case28.out")" \
  || { bad "case28 the governing sentence changed and --check still passed"; cat "$WORK/case28.out"; }
grep -q '^[0-9a-f]\{64\}  section:B.4$' "$HERE/prompts/MANIFEST.sha256" \
  && ok "case28 the manifest carries section:B.4" \
  || { bad "case28 MANIFEST.sha256 has no B.4 parent hash"; cat "$HERE/prompts/MANIFEST.sha256"; }

# the parent hash must cover the PARENT PARAGRAPH ONLY: if it swallowed B.4.1-B.4.4 it
# would still catch this mutation, but it would also fire for every subsection edit and
# say nothing about which one moved.
python3 - "$HERE" "$DOCSRC" <<'PY28B'
import sys, pathlib, importlib.util
spec = importlib.util.spec_from_file_location(
    "bp", str(pathlib.Path(sys.argv[1]) / "prompts" / "build-prompts.py"))
bp = importlib.util.module_from_spec(spec); spec.loader.exec_module(bp)
doc = pathlib.Path(sys.argv[2]).read_text()
pre = bp.preamble_of(bp.section(doc, "## B.4 "))
fails = []
if "byte-identical outside" not in pre:
    fails.append("the B.4 preamble does not contain the governing sentence")
if "### B.4.1" in pre:
    fails.append("the B.4 preamble swallowed its subsections")
if [lab for lab, _, mode in bp.GOVERNING_SECTIONS if mode == "preamble"] != ["B.4"]:
    fails.append("B.4 is not registered as a preamble-scoped governing section")
for f in fails:
    print(f"FAIL case28b {f}")
print(f"ok   case28b the parent hash is scoped to the parent paragraph ({3-len(fails)}/3)")
sys.exit(1 if fails else 0)
PY28B
if [ $? -eq 0 ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi

echo "== case 29: a recorded spawn is (pid, start ticks, BOOT ID) — reboots reuse both =="
# Sol round two, item 8: /proc start time is measured in ticks since THIS boot, so a pid
# plus its start ticks is unique within one boot and repeats across reboots.  Sol wrote a
# mismatching boot-ID field into the record and stop-server.sh ignored it and signalled.
# A record that survives a reboot -- an arm directory on disk is exactly that -- can name
# a process this apparatus never started.
boot29=$(cat /proc/sys/kernel/random/boot_id 2>/dev/null)
[ -n "$boot29" ] && ok "case29 the box reports a boot id" || bad "case29 no /proc/sys/kernel/random/boot_id"

# 29a -- correct pid, start ticks and boot id: the server this arm spawned is stopped
A29A="$WORK/st-P-T-29a"; mkdir -p "$A29A/server"
sleep 300 & P29A=$!
printf '%s %s %s\n' "$P29A" "$(starttime_of "$P29A")" "$boot29" > "$A29A/server/spawned.pid"
bash "$HERE/stop-server.sh" "$A29A" > "$WORK/case29a.out" 2>&1
want "case29a stop-server rc" 0 "$?"
sleep 1
kill -0 "$P29A" 2>/dev/null \
  && { bad "case29a the server this script started ($P29A) was not stopped"; kill -9 "$P29A" 2>/dev/null; } \
  || ok "case29a the spawned server $P29A was stopped"

# 29b -- the boot id does not match: this record was written before a reboot
A29B="$WORK/st-P-T-29b"; mkdir -p "$A29B/server"
sleep 300 & P29B=$!
printf '%s %s %s\n' "$P29B" "$(starttime_of "$P29B")" \
  "00000000-1111-2222-3333-444444444444" > "$A29B/server/spawned.pid"
bash "$HERE/stop-server.sh" "$A29B" > "$WORK/case29b.out" 2>&1
rc29b=$?
[ "$rc29b" -ne 0 ] && ok "case29b refused (rc $rc29b) on a boot-id mismatch" \
  || bad "case29b signalled a pid recorded under a different boot"
sleep 1
kill -0 "$P29B" 2>/dev/null \
  && ok "case29b the process $P29B was NOT signalled" \
  || bad "case29b killed a process whose pid was reused across a reboot"
grep -q 'boot-id-mismatch' "$WORK/case29b.out" \
  && ok "case29b typed refusal: boot-id-mismatch" \
  || { bad "case29b untyped refusal"; cat "$WORK/case29b.out"; }
kill -9 "$P29B" 2>/dev/null

# 29c -- a legacy two-field record carries no boot id at all: unusable, signal nothing
A29C="$WORK/st-P-T-29c"; mkdir -p "$A29C/server"
sleep 300 & P29C=$!
printf '%s %s\n' "$P29C" "$(starttime_of "$P29C")" > "$A29C/server/spawned.pid"
bash "$HERE/stop-server.sh" "$A29C" > "$WORK/case29c.out" 2>&1
rc29c=$?
[ "$rc29c" -ne 0 ] && ok "case29c refused (rc $rc29c) a record with no boot id" \
  || bad "case29c signalled on a record that cannot be checked against a boot"
sleep 1
kill -0 "$P29C" 2>/dev/null \
  && ok "case29c the process $P29C was NOT signalled" \
  || bad "case29c signalled on an uncheckable record"
grep -q 'no recorded boot id' "$WORK/case29c.out" \
  && ok "case29c typed refusal names the missing field" \
  || { bad "case29c untyped refusal"; cat "$WORK/case29c.out"; }
kill -9 "$P29C" 2>/dev/null

# 29d -- run-arm.sh writes the boot id it spawned under
grep -q 'boot_id' "$HERE/run-arm.sh" \
  && ok "case29d run-arm.sh records the boot id with the pid it spawned" \
  || bad "case29d run-arm.sh records a pid whose identity cannot survive a reboot"

echo "== case 30: every identity component is validated before it becomes a path segment =="
# Sol round two, item 9: --root is checked on its resolved path, and then exp/rung/arm/
# slot are interpolated into the arm directory unvalidated.  Sol's `--exp
# ../component-escape` created a directory OUTSIDE the runner root the caller had just
# been checked for.  Confining the root and then building the path out of unchecked
# caller strings confines nothing.
ESC30="component-escape-30"
for comp in exp rung slot; do
  for badv in "../$ESC30" ".." "a/b" "a b" "0123456789012345678901234567890123456789X"; do
    args=(--root "$WORK" --exp st --rung P --arm N --slot 30 --driver fake --dry-run
          --prompt "$HERE/prompts/E3-P-N.md")
    case "$comp" in
      exp)  args=(--root "$WORK" --exp "$badv" --rung P --arm N --slot 30 --driver fake --dry-run --prompt "$HERE/prompts/E3-P-N.md");;
      rung) args=(--root "$WORK" --exp st --rung "$badv" --arm N --slot 30 --driver fake --dry-run --prompt "$HERE/prompts/E3-P-N.md");;
      slot) args=(--root "$WORK" --exp st --rung P --arm N --slot "$badv" --driver fake --dry-run --prompt "$HERE/prompts/E3-P-N.md");;
    esac
    bash "$HERE/run-arm.sh" "${args[@]}" > "$WORK/case30.out" 2>&1
    rc30=$?
    if [ "$rc30" -eq 2 ] && grep -q 'IDENTITY-REFUSED' "$WORK/case30.out"; then
      ok "case30 --$comp '$badv' refused: $(grep -m1 -o 'IDENTITY-REFUSED.*' "$WORK/case30.out" | cut -c1-90)"
    else
      bad "case30 --$comp '$badv' was accepted (rc $rc30)"; head -2 "$WORK/case30.out"
    fi
  done
done
n30=$(ls -a /home/forge/tmp/arms 2>/dev/null | grep -c "$ESC30")
want "case30 directories created outside the caller's runner root" 0 "$n30"

# the control: a legal identity still plans an arm dir INSIDE the caller's root
bash "$HERE/run-arm.sh" --root "$WORK" --exp st --rung P --arm N --slot 30 \
     --driver fake --dry-run --prompt "$HERE/prompts/E3-P-N.md" > "$WORK/case30ok.out" 2>&1
want "case30 control rc" 0 "$?"
plan30=$(sed -n 's/^PLAN arm=\([^ ]*\).*/\1/p' "$WORK/case30ok.out" | head -n1)
case "$plan30" in
  "$WORK"/st-P-N-30) ok "case30 the planned arm dir is inside the runner root: $plan30";;
  *) bad "case30 unexpected planned arm dir: $plan30";;
esac

echo "== case 31: the self-test honours the CALLER's COHORT_PORTS =="
# Sol round two, item 10: the smoke test forcibly sets its own port, overriding the
# COHORT_PORTS the caller exported.  Sol was reviewing on a shared box with 7909 as its
# scope and could not run the checked-in Make target at all without crossing a port
# boundary; the review ran a hand-edited copy instead, which is one more artifact nobody
# can bind to this repository.  A test that will not respect the caller's port scope is
# unrunnable exactly where running it matters.
p31=${SELFTEST_PORT-}
if [ -z "$p31" ]; then
  bad "case31 the self-test derives no port from COHORT_PORTS"
else
  want "case31 the port under test is the first COHORT_PORTS entry" "${COHORT_PORTS%% *}" "$p31"
fi
# The scan excludes its own line by the marker at its end, so the witness cannot be
# satisfied by the very literal it is looking for.
scan31 () {
  grep -n '790[7-9]\|7910' "$HERE/self-test.sh" | grep -v 'COHORT_PORTS:-' | grep -v 'PORT-LITERAL-SCAN' | grep -vE '^[0-9]+:[[:space:]]*#'   # PORT-LITERAL-SCAN
}
n31=$(scan31 | wc -l)
[ "$n31" -eq 0 ] && ok "case31 no port literal outside the single default assignment" \
  || { bad "case31 $n31 hardcoded cohort-port literal(s) survive"; scan31 | head -5; }

# the preflight must FAIL CLOSED on a held port, for whatever ports the caller named,
# and it must refuse before it creates anything
if [ -n "$p31" ]; then
  before31=$(ls -1 "$ARMS_ROOT_BASE" 2>/dev/null | wc -l)
  python3 -c 'import socket,sys,time
s=socket.socket(); s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind(("127.0.0.1", int(sys.argv[1]))); s.listen(1); time.sleep(20)' "$p31" &
  L31=$!
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$p31" && break
    sleep 0.3
  done
  if ss -ltn 2>/dev/null | awk 'NR>1{print $4}' | sed 's/.*://' | grep -qx "$p31"; then
    ok "case31 a listener this case started holds port $p31 (pid $L31)"
    env COHORT_PORTS="$p31" ANVIL_ARMS_SELFTEST_DIR= bash "$HERE/self-test.sh" \
      > "$WORK/case31.out" 2>&1
    want "case31 preflight rc on a held port" 2 "$?"
    grep -q 'SELFTEST-REFUSED' "$WORK/case31.out" \
      && ok "case31 typed preflight refusal: $(grep -m1 SELFTEST-REFUSED "$WORK/case31.out" | cut -c1-90)" \
      || { bad "case31 the self-test ran on a port it does not own"; head -3 "$WORK/case31.out"; }
    after31=$(ls -1 "$ARMS_ROOT_BASE" 2>/dev/null | wc -l)
    want "case31 the refused run created nothing under $ARMS_ROOT_BASE" "$before31" "$after31"
  else
    bad "case31 could not bind $p31 to run the witness"
  fi
  kill "$L31" 2>/dev/null; wait "$L31" 2>/dev/null
fi

echo "== case 32: a session id past the scan ceiling fails closed AND says so truthfully =="
# Sol round two, round-two probes: "ID after the 64 KiB banner scan ceiling: rc 7/3, no
# receipt -- fail closed, though the diagnostic incorrectly says no ID was announced."
# The refusal is right and the sentence is false.  A diagnostic that misnames the cause
# sends the next reader to look for a driver that never spoke, when the driver spoke and
# the scan stopped short -- and this apparatus has already been taken down once by a
# verdict word printed over a fact nobody checked.
A32="$WORK/st-P-N-32"; mkdir -p "$A32"
git clone -q --no-hardlinks "$BASE_REPO" "$A32/worktree"
printf '%s\n' "$BASE_SHA" > "$A32/base.sha"
cp "$HERE/prompts/E3-P-N.md" "$A32/prompt.md"
EXP=st RUNG=P SLOT=32 MODEL=none DRIVER=fake RUNNER="$HERE/run-arm.sh" \
  bash "$HERE/attest.sh" "$A32" N - "" > /dev/null 2>&1
CH32="$A32/codex-home"
env CODEX_HOME="$CH32" python3 "$HERE/watch.py" --arm "$A32" --codex-home "$CH32" \
  --zero-return-window 30 --poll 0.2 \
  -- env CODEX_HOME="$CH32" bash "$HERE/fake-driver.sh" "$A32" ceiling \
  > "$WORK/case32.out" 2>&1
want "case32 watch rc" 7 "$?"
grep -q 'WATCH-ABORT rollout-unbound' "$WORK/case32.out" \
  && ok "case32 still fails closed: rollout-unbound" \
  || { bad "case32 a rollout was bound past the scan ceiling"; cat "$WORK/case32.out"; }
[ -e "$A32/receipt.json" ] && bad "case32 a receipt was written for an unbound rollout" \
  || ok "case32 no receipt.json written"
grep -q 'never announced a session id' "$WORK/case32.out" \
  && bad "case32 the diagnostic says no ID was announced about a driver that announced one" \
  || ok "case32 the diagnostic does not claim the driver was silent"
grep -q '33333333-4444-5555-6666-777777777777' "$WORK/case32.out" \
  && ok "case32 the diagnostic quotes the id the driver DID announce" \
  || { bad "case32 the diagnostic does not name the announced id"; cat "$WORK/case32.out"; }
grep -q '65536' "$WORK/case32.out" \
  && ok "case32 the diagnostic names the scan ceiling it stopped at" \
  || bad "case32 the diagnostic does not say where the scan stopped"

# the control: a driver that really is silent must still be described as silent
grep -q 'never announced a session id' "$WORK/case16b.out" \
  && ok "case32 a genuinely silent driver is still reported as silent (case 16b)" \
  || { bad "case32 the silent-driver wording was lost"; cat "$WORK/case16b.out"; }

echo
echo "anvil-arms self-test: $PASS passed, $FAIL failed  (workdir $WORK)"
[ "$CLEAN" = "1" ] || rm -rf "$WORK"
[ "$FAIL" -eq 0 ] || exit 1
exit 0
