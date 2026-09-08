#!/usr/bin/env bash
# Cell B independent acceptance oracle — frozen 2026-09-07 by the acceptance owner.
# Run from the ROOT of the worktree under test:  bash /var/tmp/forge/plan2/cellB/oracle.sh
# Prints PASS/FAIL per check. Exits 0 only if every check passed.
#
# Env:
#   ORACLE_BASE   base sha (default: the frozen Cell B base)
#   ORACLE_TMP    scratch dir (default: /var/tmp/forge/plan2/cellB/run-$$)
# No env var can skip or weaken a check.

set -uo pipefail

BASE="${ORACLE_BASE:-92a7ca14fd904e4d962df38614b9af3a7ef41a11}"
TMP="${ORACLE_TMP:-/var/tmp/forge/plan2/cellB/run-$$}"
KONDO="${KONDO:-$HOME/bin/clj-kondo}"
ROOT="$(pwd)"
mkdir -p "$TMP"

FAILS=0
pass(){ printf 'PASS  %s\n' "$*"; }
noansi(){ sed -e 's/\x1b\[[0-9;]*m//g' "$1"; }
summary_of(){ noansi "$1" | grep -E '^[0-9]+ tests?, ' | tail -1; }
fail(){ printf 'FAIL  %s\n' "$*"; FAILS=$((FAILS+1)); }
info(){ printf '      %s\n' "$*"; }
hdr(){  printf '\n=== %s\n' "$*"; }

DEST_FILE="src/cfp_scheduler_killer/exports/calendar.clj"
SRC_FILE="src/cfp_scheduler_killer/exports.clj"

# The authorised footprint (task.md rule 7). Everything else must be byte-identical to BASE.
ALLOWED="$SRC_FILE
$DEST_FILE
src/cfp_scheduler_killer/agent/commands.clj
src/cfp_scheduler_killer/handlers/exports.clj
src/cfp_scheduler_killer/handlers/public_cfp.clj
src/cfp_scheduler_killer/handlers/public_widgets.clj
src/cfp_scheduler_killer/inform.clj
src/cfp_scheduler_killer/session_invites.clj
test/cfp_scheduler_killer/comms_test.clj
test/cfp_scheduler_killer/exports_test.clj
test/cfp_scheduler_killer/schedule_test.clj"

hdr "0. preflight"
[ -f deps.edn ] && [ -d src/cfp_scheduler_killer ] || { fail "not a curtaincall-cfp worktree root ($ROOT)"; echo; echo "ORACLE RESULT: FAIL (1 check failed)"; exit 1; }
git rev-parse --verify -q "$BASE^{commit}" >/dev/null || { fail "base commit $BASE not found in this repo"; echo; echo "ORACLE RESULT: FAIL (1 check failed)"; exit 1; }
[ -x "$KONDO" ] || { fail "clj-kondo not executable at $KONDO"; echo; echo "ORACLE RESULT: FAIL (1 check failed)"; exit 1; }
pass "preflight: worktree=$ROOT base=$BASE kondo=$($KONDO --version)"

# ---------------------------------------------------------------------------
hdr "A. structural inventory (clj-kondo analysis, resolved var usages — not string matching)"
"$KONDO" --lint src test --config \
  '{:output {:analysis {:var-definitions {:meta true} :var-usages true} :format :json} :skip-lint true}' \
  > "$TMP/analysis.json" 2>"$TMP/analysis.err"
if [ ! -s "$TMP/analysis.json" ]; then
  fail "A: clj-kondo produced no analysis"; sed -n '1,20p' "$TMP/analysis.err"
else
python3 - "$TMP/analysis.json" <<'PY'
import json,sys,collections
A=json.load(open(sys.argv[1]))['analysis']
SRC='cfp-scheduler-killer.exports'
DST='cfp-scheduler-killer.exports.calendar'
DSTFILE='src/cfp_scheduler_killer/exports/calendar.clj'
# frozen baseline, measured on base 92a7ca14 -------------------------------
CLOSURE={  # name -> (private, fixed-arities, defined-by)
 'calendar-prodid':(True,None,'clojure.core/def'),
 'ical-domain':(False,None,'clojure.core/def'),
 'ics-escape':(False,[1],'clojure.core/defn'),
 'fold-line':(False,[1],'clojure.core/defn'),
 'ics-date':(True,[1],'clojure.core/defn-'),
 'ics-stamp':(True,[0],'clojure.core/defn-'),
 'ics-uid':(False,[1],'clojure.core/defn'),
 'ics-sequence':(False,[1],'clojure.core/defn'),
 'ics-local-datetime':(True,[2],'clojure.core/defn-'),
 'absolute-link':(True,[2],'clojure.core/defn-'),
 'vevent':(False,[2],'clojure.core/defn'),
 'snapshot-published?':(True,[2],'clojure.core/defn-'),
 'last-publication-snapshot':(True,[1],'clojure.core/defn-'),
 'ics-sequence-at':(True,[3],'clojure.core/defn-'),
 'cancellation-vevent':(True,[1],'clojure.core/defn-'),
 'cancellation-for':(False,[2],'clojure.core/defn'),
 'hold-the-date-uid':(False,[1],'clojure.core/defn'),
 'hold-the-date-sequence':(False,[1],'clojure.core/defn'),
 'hold-the-date-vevent':(False,[1,2],'clojure.core/defn'),
 'cfp-deadline-uid':(False,[1],'clojure.core/defn'),
 'cfp-deadline-sequence':(False,[1],'clojure.core/defn'),
 'cfp-deadline-ics':(False,[1,2],'clojure.core/defn'),
 'submission-ics':(False,[2],'clojure.core/defn'),
 'calendar-ics-for':(False,[2],'clojure.core/defn'),
 'calendar-ics':(False,[1,2],'clojure.core/defn'),
}
MOVED_SITES={  # caller ns -> number of references to closure vars
 'cfp-scheduler-killer.agent.commands':1,
 'cfp-scheduler-killer.comms-test':3,
 'cfp-scheduler-killer.exports-test':21,
 'cfp-scheduler-killer.handlers.exports':1,
 'cfp-scheduler-killer.handlers.public-cfp':1,
 'cfp-scheduler-killer.handlers.public-widgets':1,
 'cfp-scheduler-killer.inform':1,
 'cfp-scheduler-killer.schedule-test':12,
 'cfp-scheduler-killer.session-invites':2,
}
RETAINED_SITES={  # the same 9 callers' references to vars that STAY in exports
 'cfp-scheduler-killer.agent.commands':11,
 'cfp-scheduler-killer.comms-test':1,
 'cfp-scheduler-killer.exports-test':72,
 'cfp-scheduler-killer.handlers.exports':9,
 'cfp-scheduler-killer.handlers.public-cfp':0,
 'cfp-scheduler-killer.handlers.public-widgets':1,
 'cfp-scheduler-killer.inform':0,
 'cfp-scheduler-killer.schedule-test':4,
 'cfp-scheduler-killer.session-invites':1,
}
RETAINED_TOTAL=197  # references to retained exports vars from ALL external namespaces
out=[]; fails=0
def ok(m): out.append("PASS  "+m)
def no(m):
    global fails; fails+=1; out.append("FAIL  "+m)

defs=A['var-definitions']; uses=A['var-usages']
byname=collections.defaultdict(list)
for d in defs: byname[d['name']].append(d)

# A1: every closure form defined exactly once, repo-wide, in the destination file
bad=[]
for n in CLOSURE:
    ds=[d for d in byname.get(n,[]) if d['ns'] in (SRC,DST)] or byname.get(n,[])
    ds=byname.get(n,[])
    if len(ds)!=1: bad.append(f"{n}: {len(ds)} definitions in {[d['ns'] for d in ds]}"); continue
    d=ds[0]
    if d['ns']!=DST: bad.append(f"{n}: defined in {d['ns']}, expected {DST}")
    elif not d['filename'].endswith(DSTFILE): bad.append(f"{n}: file {d['filename']}, expected {DSTFILE}")
if bad: no("A1 destination inventory: "+ "; ".join(bad))
else: ok(f"A1 all {len(CLOSURE)} closure forms defined exactly once, in {DST} ({DSTFILE})")

# A2: none of them remain in the source namespace
left=sorted(d['name'] for d in defs if d['ns']==SRC and d['name'] in CLOSURE)
if left: no("A2 stale definitions still in "+SRC+": "+", ".join(left))
else: ok("A2 no closure form is still defined in "+SRC)

# A3: no facade / forwarding — the source namespace must not reference the destination at all
fac=[u for u in uses if u.get('to')==DST and u.get('from')==SRC]
if fac: no(f"A3 facade/back-reference: {SRC} references {DST} at "+", ".join(f"{u['filename']}:{u['row']}" for u in fac[:8]))
else: ok(f"A3 no facade: {SRC} contains no reference to {DST} (also the acyclic-require guard)")

# A3b: no duplicate/forwarding definition of a closure name anywhere else
dupes=[f"{n}@{d['ns']}" for n in CLOSURE for d in byname.get(n,[]) if d['ns']!=DST]
if dupes: no("A3b duplicate definitions: "+", ".join(sorted(set(dupes))))
else: ok("A3b no duplicate definition of any closure name outside "+DST)

# A4: every former caller reference now resolves to the destination namespace
got=collections.Counter(u['from'] for u in uses if u.get('to')==DST and u['name'] in CLOSURE)
diffs=[]
for ns,n in sorted(MOVED_SITES.items()):
    if got.get(ns,0)!=n: diffs.append(f"{ns}: {got.get(ns,0)} != {n}")
extra=[ns for ns in got if ns not in MOVED_SITES and ns!=DST]
if extra: diffs.append("unexpected caller ns: "+", ".join(sorted(extra)))
if diffs: no("A4 resolved references to "+DST+": "+"; ".join(diffs))
else: ok(f"A4 all 43 external references across 9 namespaces resolve to {DST}")

# A5: no reference anywhere still resolves a closure name to the source namespace
stale=[u for u in uses if u.get('to')==SRC and u['name'] in CLOSURE]
if stale: no("A5 left-behind references to "+SRC+": "+", ".join(f"{u['filename']}:{u['row']} {u['name']}" for u in stale[:10]))
else: ok("A5 no reference resolves a moved var to "+SRC)

# A6: mixed callers keep their references to RETAINED exports vars
ret=collections.Counter(u['from'] for u in uses
                        if u.get('to')==SRC and u['name'] not in CLOSURE
                        and u['from'] not in (SRC,DST))
diffs=[]
for ns,n in sorted(RETAINED_SITES.items()):
    if ret.get(ns,0)!=n: diffs.append(f"{ns}: {ret.get(ns,0)} != {n}")
tot=sum(ret.values())
if tot!=RETAINED_TOTAL: diffs.append(f"total external retained refs {tot} != {RETAINED_TOTAL}")
if diffs: no("A6 retained references redirected or lost: "+"; ".join(diffs))
else: ok(f"A6 retained-var references unchanged ({RETAINED_TOTAL} external refs still resolve to {SRC})")

# A7: arity / visibility / def-form preservation
diffs=[]
for n,(priv,ar,by) in sorted(CLOSURE.items()):
    ds=[d for d in byname.get(n,[]) if d['ns']==DST]
    if not ds: continue
    d=ds[0]
    if bool(d.get('private',False))!=priv: diffs.append(f"{n}: private {d.get('private',False)} != {priv}")
    if d.get('fixed-arities') and sorted(d['fixed-arities'])!=sorted(ar or []): diffs.append(f"{n}: arities {d.get('fixed-arities')} != {ar}")
    if (ar is None) != (d.get('fixed-arities') is None): diffs.append(f"{n}: arity shape changed ({d.get('fixed-arities')} vs {ar})")
    if d.get('defined-by')!=by: diffs.append(f"{n}: defined-by {d.get('defined-by')} != {by}")
if diffs: no("A7 signature/visibility drift: "+"; ".join(diffs))
else: ok("A7 arities, visibility and def form preserved for all 25 forms")
print("\n".join(out))
sys.exit(1 if fails else 0)
PY
  [ $? -eq 0 ] || FAILS=$((FAILS+1))
fi

# @spec NS-SPLIT-042
# INTENT: NS-SPLIT-042
# @spec NS-SPLIT-046
# INTENT: NS-SPLIT-046
hdr "A8. lint: no new or increased unresolved findings against the exact base"
"$KONDO" --lint src test > "$TMP/lint.txt" 2>&1
LINT_RC=$?
if [ "$LINT_RC" -ne 0 ] && [ "$LINT_RC" -ne 2 ] && [ "$LINT_RC" -ne 3 ]; then
  fail "A8 candidate analyzer failed (rc=$LINT_RC)"
fi
[ -s "$TMP/lint.txt" ] || fail "A8 candidate analyzer returned empty output"
BASELINE_TREE="$(mktemp -d "$TMP/a8-base.XXXXXX")"
if [ -n "$BASELINE_TREE" ] && git -C "$ROOT" archive "$BASE" | tar -x -C "$BASELINE_TREE"; then
  (cd "$BASELINE_TREE" && "$KONDO" --lint src test) > "$TMP/baseline-lint.txt" 2>&1
  BASELINE_RC=$?
  if [ "$BASELINE_RC" -ne 0 ] && [ "$BASELINE_RC" -ne 2 ] && [ "$BASELINE_RC" -ne 3 ]; then
    fail "A8 baseline analyzer failed (rc=$BASELINE_RC)"
  fi
  [ -s "$TMP/baseline-lint.txt" ] || fail "A8 baseline analyzer returned empty output"
  if python3 "$(dirname "$0")/cell_b_lint.py" "$TMP/baseline-lint.txt" "$TMP/lint.txt"; then
    pass "A8 no new or increased unresolved var/namespace/symbol findings"
  else
    fail "A8 unresolved finding comparison failed"
  fi
else
  fail "A8 could not archive the exact base"
fi
[ -z "$BASELINE_TREE" ] || rm -rf -- "$BASELINE_TREE"

# ---------------------------------------------------------------------------
# @spec NS-SPLIT-043
# INTENT: NS-SPLIT-043
hdr "B07. independent preservation and negative-mutation guard"
bb "$(dirname "$(readlink -f "$0")")/cell_b_preservation.clj" "$ROOT" || fail "B07 preservation oracle"

hdr "B. fresh-process load in two orders (cycles, stale declares, stale interns)"
read -r -d '' RUNTIME_ASSERT <<'CLJ'
(let [closure '[calendar-prodid ical-domain ics-escape fold-line ics-date ics-stamp ics-uid
                ics-sequence ics-local-datetime absolute-link vevent snapshot-published?
                last-publication-snapshot ics-sequence-at cancellation-vevent cancellation-for
                hold-the-date-uid hold-the-date-sequence hold-the-date-vevent cfp-deadline-uid
                cfp-deadline-sequence cfp-deadline-ics submission-ics calendar-ics-for calendar-ics]
      src  (find-ns 'cfp-scheduler-killer.exports)
      dst  (find-ns 'cfp-scheduler-killer.exports.calendar)
      _    (when-not dst (throw (ex-info "destination namespace not loaded" {})))
      si   (ns-interns src)
      di   (ns-interns dst)
      left (filterv #(contains? si %) closure)
      miss (filterv #(not (contains? di %)) closure)]
  (when (seq left) (throw (ex-info (str "still interned in exports: " (pr-str left)) {})))
  (when (seq miss) (throw (ex-info (str "not interned in exports.calendar: " (pr-str miss)) {})))
  (println "runtime-interns-ok"))
CLJ
run_order () {
  local label="$1"; shift
  local expr="$1"
  ( cd "$ROOT" && timeout 300 clojure -J-Xmx512m -M -e "$expr" ) > "$TMP/load-$label.txt" 2>&1
  local rc=$?
  if [ $rc -ne 0 ] || ! grep -q runtime-interns-ok "$TMP/load-$label.txt"; then
    fail "B ($label) fresh-process load failed (rc=$rc)"
    sed -n '1,25p' "$TMP/load-$label.txt" | sed 's/^/      /'
  else
    pass "B ($label) fresh process loaded clean; all 25 vars interned in the destination only"
  fi
}
CALLERS="(require 'cfp-scheduler-killer.agent.commands 'cfp-scheduler-killer.handlers.exports 'cfp-scheduler-killer.handlers.public-cfp 'cfp-scheduler-killer.handlers.public-widgets 'cfp-scheduler-killer.inform 'cfp-scheduler-killer.session-invites)"
run_order "destination-first" "(require 'cfp-scheduler-killer.exports.calendar) (require 'cfp-scheduler-killer.exports) $CALLERS $RUNTIME_ASSERT"
run_order "source-first"      "(require 'cfp-scheduler-killer.exports) (require 'cfp-scheduler-killer.exports.calendar) $CALLERS $RUNTIME_ASSERT"
info "bootstrap note: both orders use the plain \`clojure -M\` classpath (src only, no :run-tests alias),"
info "so they prove the production namespaces load standalone. They do NOT prove test-path loading;"
info "check C covers that."

# ---------------------------------------------------------------------------
hdr "C. behavioural: the focused tests over the cluster + one integration path"
FOCUS_ARGS=(
  --focus cfp-scheduler-killer.exports-test/ics-test
  --focus cfp-scheduler-killer.exports-test/ics-escaping-test
  --focus cfp-scheduler-killer.exports-test/issued-invite-uid-survives-title-amendment-test
  --focus cfp-scheduler-killer.exports-test/unroomed-session-calendar-uses-event-location
  --focus cfp-scheduler-killer.exports-test/cfp-deadline-ics-amends-and-never-duplicates-test
  --focus cfp-scheduler-killer.schedule-test/exports-reflect-placement-test
  --focus cfp-scheduler-killer.schedule-test/published-session-removal-emits-a-snapshot-cancellation-test
  --focus cfp-scheduler-killer.comms-test/ics-attachment-matches-the-feed-test
)
( cd "$ROOT" && timeout 900 bin/kaocha unit "${FOCUS_ARGS[@]}" ) > "$TMP/focus.txt" 2>&1
RC=$?
SUMMARY=$(summary_of "$TMP/focus.txt")
NTESTS=$(printf '%s' "$SUMMARY" | sed -nE 's/^([0-9]+) tests?,.*/\1/p')
if [ $RC -ne 0 ]; then
  fail "C focused cluster tests failed (rc=$RC): ${SUMMARY:-no summary line}"
  tail -30 "$TMP/focus.txt" | sed 's/^/      /'
elif [ -z "${NTESTS:-}" ] || [ "$NTESTS" -lt 8 ]; then
  fail "C focused run matched only ${NTESTS:-0} tests (expected 8 — a --focus that matches nothing is not a pass)"
  tail -10 "$TMP/focus.txt" | sed 's/^/      /'
else
  pass "C focused cluster tests: $SUMMARY"
fi

( cd "$ROOT" && timeout 900 bin/kaocha unit --focus cfp-scheduler-killer.public-widgets-test ) > "$TMP/integration.txt" 2>&1
RC=$?
SUMMARY=$(summary_of "$TMP/integration.txt")
NTESTS=$(printf '%s' "$SUMMARY" | sed -nE 's/^([0-9]+) tests?,.*/\1/p')
if [ $RC -ne 0 ]; then
  fail "C integration path (POST /agenda/public-widgets/my.ics through the ring handler) failed: ${SUMMARY:-no summary}"
  tail -30 "$TMP/integration.txt" | sed 's/^/      /'
elif [ -z "${NTESTS:-}" ] || [ "$NTESTS" -lt 1 ]; then
  fail "C integration focus matched no tests"
else
  pass "C integration path (public-widgets .ics route → handler → calendar-ics-for): $SUMMARY"
fi

# ---------------------------------------------------------------------------
hdr "D. the repository's own required entry points"
# PRECONDITION (environment, not the refactor): bin/test-new-mission-worktree builds
# throwaway git repos with `git init --initial-branch=main` and then clones them. On a
# box where init.defaultBranch is unset the bare remote's HEAD points at master and the
# clone has no `main` — the target fails IDENTICALLY AT THE BASE COMMIT. The oracle
# supplies the setting so the check can actually run; it does not skip or weaken it.
info "precondition: init.defaultBranch is $(git config --global init.defaultBranch || echo unset) globally; the oracle pins it to main for this gate only"
( cd "$ROOT" && GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=init.defaultBranch GIT_CONFIG_VALUE_0=main timeout 2400 make runtests-once ) > "$TMP/runtests-once.txt" 2>&1
RC=$?
if [ $RC -ne 0 ]; then
  fail "D make runtests-once failed (rc=$RC)"
  tail -40 "$TMP/runtests-once.txt" | sed 's/^/      /'
else
  pass "D make runtests-once green — $(summary_of "$TMP/runtests-once.txt")"
  info "components: $(grep -c '^oracle: ' "$TMP/runtests-once.txt") Prolog oracle files, node --test, new-mission worktree test, bin/kaocha unit"
fi

( cd "$ROOT" && timeout 1800 bin/kaocha unit --fail-fast ) > "$TMP/kaocha-unit.txt" 2>&1
RC=$?
if [ $RC -ne 0 ]; then
  fail "D bin/kaocha unit --fail-fast failed (rc=$RC)"
  tail -40 "$TMP/kaocha-unit.txt" | sed 's/^/      /'
else
  pass "D bin/kaocha unit --fail-fast green — $(summary_of "$TMP/kaocha-unit.txt")"
fi

# ---------------------------------------------------------------------------
hdr "E. protected content: everything outside the authorised footprint is byte-identical to $BASE"
printf '%s\n' "$ALLOWED" | sed '/^$/d' | sort > "$TMP/allowed.txt"
VIOL=0
# git's own comparison of the working tree against the base commit: it handles symlinks
# (mode 120000), executable-bit changes and type changes correctly, which a hash-object
# loop does not (a dangling symlink in the tree reads as a deleted file).
git -C "$ROOT" diff --name-only "$BASE" -- . > "$TMP/changed.txt" 2>/dev/null
while read -r path; do
  [ -n "$path" ] || continue
  grep -qxF -- "$path" "$TMP/allowed.txt" && continue
  echo "      CHANGED  $path"; VIOL=$((VIOL+1))
done < "$TMP/changed.txt"
# new files outside the footprint
while read -r path; do
  grep -qxF -- "$path" "$TMP/allowed.txt" && continue
  case "$path" in .beads/*|*.log|00TESTLOG.txt|.testwatch*|.cpcache/*) continue;; esac
  echo "      ADDED    $path"; VIOL=$((VIOL+1))
done < <(git -C "$ROOT" status --porcelain --untracked-files=all | awk '$1=="??"{print $2}')
if [ "$VIOL" -ne 0 ]; then
  fail "E $VIOL file(s) outside the authorised footprint differ from $BASE"
else
  pass "E every file outside the authorised footprint is byte-identical to $BASE"
fi
info "footprint diff vs base:"
git -C "$ROOT" diff --stat "$BASE" -- $(printf '%s ' $ALLOWED) 2>/dev/null | sed 's/^/      /'
[ -f "$ROOT/$DEST_FILE" ] && info "new file $DEST_FILE: $(wc -l < "$ROOT/$DEST_FILE") lines"

echo
if [ "$FAILS" -eq 0 ]; then
  echo "ORACLE RESULT: PASS (all checks)"; exit 0
else
  echo "ORACLE RESULT: FAIL ($FAILS check group(s) failed)"; exit 1
fi
