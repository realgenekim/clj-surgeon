#!/usr/bin/env bash
# round <worktree> <findings.md> <name> [options] — ONE command that runs a complete
# namespace-split builder round and grades it. Gene, 2026-09-08: "It should have taken
# only 4m -- why is this taking so long! We must make this routine perfect".
#
# The routine, end to end, with every gotcha this seat paid for baked in:
#   LOCK -> PREFLIGHT -> BUILD (sol-yolo/codex, bound to the CHILD pid) -> COMMIT CHECK
#   -> GRADE (fresh fixture, branch build, papercut-oracle + 4 fixture oracles)
#   -> WITNESSES (focused cold test namespaces) -> RECEIPT (table + one ledger line).
#
# Options:
#   --minutes N     builder wall ceiling, default 25
#   --fixture PATH  reuse an existing fixture worktree instead of creating one
#   --request EDN   split request, default D1-request.edn repointed at the fixture
#   --model NAME    codex model, default gpt-6-astra (verified against the builder log)
#   --no-build      skip the builder; grade whatever tip the worktree already has
#   --adopt PID     do NOT launch; wait on an already-running builder pid, then grade.
#                   For the exact case that burned us: discovery failed, the round exited,
#                   and a perfectly healthy builder was left with nobody watching it.
#
# RATCHETS BAKED IN (each one is a scar, not a preference):
#  * [[sol-yolo-wrapper-pid-is-not-the-run]] the sol-yolo WRAPPER exits while the codex
#    CHILD keeps working. We find the child by scanning /proc for cwd==worktree and
#    "codex exec" in argv, print a START RECEIPT naming it, and wait on THAT pid.
#  * [[waiters-bind-to-pid-not-argv]] no `pgrep -f` anywhere — an unanchored argv match
#    self-matches this script's own bash -c. Every scan is /proc + cwd + argv.
#  * never `pkill`; the only kill is `kill <exact child pid>`.
#  * never wrap the builder in an outer `timeout` — timeout kills your WAIT, not the
#    child, and an unsupervised codex keeps editing the worktree.
#  * a per-worktree flock, so two rounds can never collide in one tree (the collision
#    that cost round 4: two Astra runs, one paused itself, uncommitted merge left behind).
#  * `</dev/null` on every subprocess (a codex/bb/clojure that inherits a tty stdin hangs).
#  * temp under /var/tmp/forge, never /tmp (RAM tmpfs, filled twice).
#  * `date` here is uutils: no %3N. We take %N and cut to milliseconds.
#  * the fixture's suite APPENDS to the tracked 00SERVER-LOGS.txt — excluded from every
#    clean check, in the fixture only.
#  * JAVA_TOOL_OPTIONS prints "Picked up ..." on stderr; never treated as failure.
#  * the model is verified from the builder log's `model:` line, not from the flag we
#    passed — a flag is a request, the log is the receipt.
set -uo pipefail

# ---------------------------------------------------------------------------
# args
# ---------------------------------------------------------------------------
usage() {
  sed -n '2,30p' "$0" >&2
  exit 2
}
[ $# -ge 3 ] || usage
WT=$(readlink -f "${1:?worktree}"); FINDINGS=${2:?findings.md}; NAME=${3:?name}
shift 3
MINUTES=25 FIXTURE="" REQUEST="" MODEL=gpt-6-astra NOBUILD=0 ADOPT=""
while [ $# -gt 0 ]; do
  case "$1" in
    --minutes) MINUTES=${2:?}; shift 2 ;;
    --adopt)   ADOPT=${2:?}; shift 2 ;;
    --fixture) FIXTURE=$(readlink -f "${2:?}"); shift 2 ;;
    --request) REQUEST=$(readlink -f "${2:?}"); shift 2 ;;
    --model)   MODEL=${2:?}; shift 2 ;;
    --no-build) NOBUILD=1; shift ;;
    *) echo "round: unknown option $1" >&2; usage ;;
  esac
done

CC_SRC=/home/forge/src/curtaincall-cfp
CC_SHA=d9205abcc9b9f31d93bd4a76537b74fccd583ca6
CELLC=/var/tmp/forge/plan2/cellC
GUARD_TEST=/var/tmp/forge/plan2/split/view_architecture_test.clj
PAPERCUT=$CELLC/papercut-oracle.py
CC_ORACLE=$CELLC/cc-oracle.py
BASE_REQUEST=$CELLC/D1-request.edn
OUT=/var/tmp/forge/round
mkdir -p "$OUT"
LOG=$OUT/$NAME.log
LEDGER=$OUT/ledger.txt

# ---------------------------------------------------------------------------
# ledger / timing.  `date` on this box is uutils: %3N is NOT supported, %N is.
# ---------------------------------------------------------------------------
stamp() { date -u +%FT%T.%N | cut -c1-23; }
T0=$(date +%s)
PHASES=()          # "phase|wall|result"
phase_t0() { date +%s; }
say() { printf '%s\n' "$*" | tee -a "$LOG"; }
step() {   # step <phase> <wall-seconds> <result>
  PHASES+=("$1|$2|$3")
  say "$(stamp)  [+$(( $(date +%s) - T0 ))s]  $1 ${2}s :: $3"
}
refuse() { say "$(stamp)  REFUSED: $*"; exit 3; }
: > "$LOG"
say "$(stamp)  ROUND $NAME start  worktree=$WT findings=$FINDINGS model=$MODEL minutes=$MINUTES no_build=$NOBUILD"

# ---------------------------------------------------------------------------
# /proc scan for a live codex bound to a given WORKTREE.  NEVER pgrep -f: an
# unanchored argv match self-matches the harness's own `bash -c` and never fires.
#
# DEFECT 1, first live round (2026-09-08): the codex process's cwd is NOT the
# worktree.  sol-yolo passes `-C <worktree>` and codex keeps the LAUNCHER's cwd —
# observed cwd=/home/forge/src/marvin-voice-remote/channel-connector while argv
# read `codex exec ... -C /home/forge/src/clj-surgeon-split ...`.  A cwd-only scan
# is blind to every real builder.  So the AUTHORITY is argv: "codex exec" AND
# `-C <worktree>` (exact, trailing slash accepted).  cwd==worktree is kept as a
# SECONDARY match — it costs nothing and would catch a launcher that does chdir.
# ---------------------------------------------------------------------------
codex_pids_in() {  # codex_pids_in <worktree> [min-start-epoch]  -> pids, numerically sorted
  local dir=${1%/} minstart=${2:-0} p cwd args
  for p in /proc/[0-9]*; do
    p=${p#/proc/}
    # a process can exit between the glob and the read; that is a race, not an error,
    # so the whole read is stderr-silenced (redirecting only `tr` still lets bash
    # print "No such file or directory" for the failed input redirection).
    args=$({ tr '\0' ' ' < "/proc/$p/cmdline"; } 2>/dev/null) || continue
    case "$args" in *"codex exec "*) ;; *) continue ;; esac
    cwd=$(readlink "/proc/$p/cwd" 2>/dev/null)
    case "$args" in
      *" -C $dir "*|*" -C $dir/ "*) ;;                 # primary: the -C fence
      *)  [ "$cwd" = "$dir" ] || continue ;;           # secondary: an actual chdir
    esac
    if [ "$minstart" != 0 ]; then
      [ "$(proc_start "$p")" -ge "$minstart" ] 2>/dev/null || continue
    fi
    echo "$p"
  done | sort -n
}

# One launch shows up as TWO matches: the `node .../codex` shim and the native
# `codex-linux-x64/.../codex` binary it execs as its child. Wait on the PARENT —
# it outlives nothing and dies when the run ends. `sort -n | head -1` picks it
# deterministically; the /proc glob's own order is LEXICOGRAPHIC and would pick
# the wrong one the moment pid width changes (1000001 sorts before 999999).
codex_leader_in() { codex_pids_in "$@" | head -1; }

proc_start() {  # epoch seconds at which <pid> started (the /proc dir's mtime)
  stat -c %Y "/proc/$1" 2>/dev/null || echo 0
}

# is_ours <pid> — true when <pid> belongs to THIS round: same process group, or a
# PPid chain that walks up to us. Used only to keep our own children out of
# diagnostics; never to decide what to reap.
is_ours() {
  local pid=$1 g n=0
  g=$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ')
  [ -n "$MYPGID" ] && [ "$g" = "$MYPGID" ] && return 0
  while [ "$pid" != 1 ] && [ -n "$pid" ] && [ "$n" -lt 24 ]; do
    [ "$pid" = "$$" ] && return 0
    pid=$(awk '/^PPid:/{print $2}' "/proc/$pid/status" 2>/dev/null)
    n=$((n+1))
  done
  return 1
}

# ===========================================================================
# 1. LOCK
# ===========================================================================
t=$(phase_t0)
[ -d "$WT" ] || refuse "no such worktree: $WT"
LOCKF=$WT/.round.lock
ADOPT_LOCKF=$WT/.round.adopt.lock
ADOPTED_MARK=$WT/.round.lock.adopted
MYPGID=$(ps -o pgid= -p $$ 2>/dev/null | tr -d ' ')

# ---------------------------------------------------------------------------
# THE ROOT CAUSE, first --adopt attempt (2026-09-08): `exec 9>$LOCKF` puts the
# lock on a descriptor that EVERY child inherits across fork+exec. sol-yolo, its
# bash wrapper, and codex all carried fd 9, so the flock outlived the round that
# took it and was "held" by the very builder we were trying to adopt:
#     REFUSED: round already running (pid 2655184 2655203 2829462 2829483 2829485)
# — 2655184 being the wrapper of the FAILED round, and 2829462+ this round's own
# short-lived children, all of them mere INHERITORS of one fd.
#
# Two fixes, both needed:
#  (a) the builder is launched with `9>&-` (and `8>&-`), so no child ever inherits
#      the lock again — see the BUILD phase. That is the actual repair.
#  (b) --adopt does not contend for $LOCKF at all. It cannot: by construction the
#      holder is inside the tree it is adopting. It takes a SEPARATE lock
#      ($ADOPT_LOCKF, so two adopters still cannot collide) and drops a marker
#      naming itself, removed on exit.
# ---------------------------------------------------------------------------
if [ -n "$ADOPT" ]; then
  exec 8>"$ADOPT_LOCKF" || refuse "cannot open adopt lock $ADOPT_LOCKF"
  flock -n 8 || refuse "another --adopt round is already watching $WT ($ADOPT_LOCKF)"
  printf 'round_pid=%s adopted_pid=%s started=%s\n' "$$" "$ADOPT" "$(stamp)" > "$ADOPTED_MARK"
  trap 'rm -f "$ADOPTED_MARK"' EXIT
  LOCKMODE="adopt: no exclusive lock (holder is inside the adopted tree); marker=$ADOPTED_MARK"
else
  exec 9>"$LOCKF" || refuse "cannot open lock $LOCKF"
  if ! flock -n 9; then
    # A held lock is now genuinely ambiguous no longer — but an OLD leaked fd may
    # still be out there, so name the adoptable builder instead of dead-ending.
    # fuser reports every INHERITOR, our own transient children included — which is
    # what made the last refusal unreadable. Drop anything in our own process group.
    holder=""
    for _h in $(fuser "$LOCKF" 2>/dev/null | tr -s ' '); do
      case "$_h" in ''|*[!0-9]*) continue ;; esac
      # a holder that is already gone was one of fuser's own transient siblings —
      # it raced us and cannot be pgid-checked, so it is ours by elimination.
      kill -0 "$_h" 2>/dev/null || continue
      is_ours "$_h" && continue
      holder="$holder $_h"
    done
    [ -n "${holder// /}" ] || holder=" (all holders are our own children — a stale leaked fd)"
    leader=$(codex_leader_in "$WT")
    if [ -n "$leader" ]; then
      refuse "round already running in $WT (fd holders:$holder) — builder is pid $leader; wait on it with: round $WT $FINDINGS $NAME --adopt $leader"
    fi
    refuse "round already running in $WT (fd holders:$holder)"
  fi
  LOCKMODE="flock held on $LOCKF"
fi

# A codex fenced to this worktree means a round is already building here — UNLESS we
# were asked to adopt it, which is the whole point of --adopt.
#
# ONE launch presents as TWO pids: the `node .../codex` shim and the native codex
# binary it execs. Exempting only $ADOPT would refuse on its own sibling, so an
# --adopt whose pid IS in the discovered set adopts the whole fenced set.
#
# Our OWN process group is excluded here: nothing we have spawned at this point can
# legitimately be a builder, and a stray match on our own children is exactly the
# noise that made the last refusal unreadable. The exclusion is deliberately scoped
# to this pre-launch scan only — post-launch discovery and reaping must NOT filter
# by pgid, or a builder whose `setsid` failed would become both undiscoverable and
# unreapable.
found=""
for _p in $(codex_pids_in "$WT"); do
  _g=$(ps -o pgid= -p "$_p" 2>/dev/null | tr -d ' ')
  [ -n "$MYPGID" ] && [ "$_g" = "$MYPGID" ] && continue
  found="$found $_p"
done
found=${found# }
existing=$found
if [ -n "$ADOPT" ]; then
  case " $found " in *" $ADOPT "*) existing="" ;; esac
fi
if [ -n "${existing// /}" ]; then
  refuse "a codex process already lives in $WT (pid $existing) — use --adopt $(echo "$existing" | awk '{print $1}') to wait on it"
fi
step LOCK $(( $(date +%s) - t )) "ok ($LOCKMODE; no unowned codex fenced to tree${ADOPT:+; adopting $ADOPT})"

# ===========================================================================
# 2. PREFLIGHT
# ===========================================================================
t=$(phase_t0)
git -C "$WT" rev-parse --git-dir >/dev/null 2>&1 || refuse "$WT is not a git worktree"
# A clean tree is a precondition for LAUNCHING a round — it is NOT a precondition for
# ADOPTING one: the builder we are adopting is editing the tree at this very moment,
# and refusing it as "dirty" would make --adopt structurally impossible. The check is
# not dropped, it is MOVED to COMMIT_CHECK, where it is asked after the builder exits
# and where the honest question ("did it leave the tree clean?") actually lives.
if [ -n "$ADOPT" ]; then
  dirty=$(git -C "$WT" status --porcelain --untracked-files=no 2>/dev/null | wc -l)
  say "$(stamp)  adopt: skipping the pre-clean check — builder $ADOPT is mid-flight ($dirty tracked file(s) currently modified); enforced at COMMIT_CHECK instead"
else
  dirty=$(git -C "$WT" status --porcelain --untracked-files=no 2>/dev/null)
  [ -z "$dirty" ] || { say "$dirty"; refuse "dirty: $WT has uncommitted tracked changes"; }
fi
OLD_TIP=$(git -C "$WT" rev-parse HEAD)
BRANCH=$(git -C "$WT" rev-parse --abbrev-ref HEAD)
command -v bb >/dev/null || refuse "bb not on PATH"
[ -x /home/forge/bin/clj-kondo ] || refuse "~/bin/clj-kondo missing or not executable"
[ -f "$PAPERCUT" ] || refuse "papercut oracle missing: $PAPERCUT"
[ -f "$CC_ORACLE" ] || refuse "cc oracle missing: $CC_ORACLE"
[ -f "$GUARD_TEST" ] || refuse "guard test missing: $GUARD_TEST"
if [ -n "$ADOPT" ]; then PRE="adopt mode (pre-clean check deferred), "; else PRE="clean, "; fi
step PREFLIGHT $(( $(date +%s) - t )) "${PRE}branch=$BRANCH tip=${OLD_TIP:0:8}, bb+clj-kondo+oracles present"

# ===========================================================================
# 3. BUILD
# ===========================================================================
BUILD=ok
BUILDER_PID=""
LAUNCH_EPOCH=0
WRAPPER=""

# DEFECT 2, first live round (2026-09-08): discovery failed after 30 s, the script
# printed REFUSED and exited — releasing the lock and ORPHANING the builder it had
# just spawned.  A failure after launch owns what it launched.  This reaps the
# wrapper and every codex bearing our `-C <worktree>` that started after we did,
# says so in the receipt, and exits non-zero.
reap_launched() {
  local victims p
  [ -n "$WRAPPER" ] && kill "$WRAPPER" 2>/dev/null && say "$(stamp)  reaped wrapper $WRAPPER"
  victims=$(codex_pids_in "$WT" "$LAUNCH_EPOCH")
  for p in $victims; do
    kill "$p" 2>/dev/null && say "$(stamp)  reaped codex $p (launched by this round)"
  done
  [ -z "$victims" ] && say "$(stamp)  reaped: no codex process of ours was running"
  return 0
}
refuse_after_launch() {   # never leave a builder nobody is watching
  say "$(stamp)  post-launch failure — reaping what this round launched"
  reap_launched
  refuse "$*"
}

if [ "$NOBUILD" = 1 ]; then
  step BUILD 0 "skipped (--no-build)"
elif [ -n "$ADOPT" ]; then
  t=$(phase_t0)
  BUILDER_PID=$ADOPT
  kill -0 "$BUILDER_PID" 2>/dev/null || refuse "--adopt $ADOPT: no such live process"
  argv=$(tr '\0' ' ' < "/proc/$BUILDER_PID/cmdline" 2>/dev/null)
  case "$argv" in
    *"codex exec "*" -C ${WT%/} "*|*"codex exec "*" -C ${WT%/}/ "*) ;;
    *) refuse "--adopt $ADOPT: argv is not a codex exec fenced to $WT: $argv" ;;
  esac
  BLOG=$(printf '%s' "$argv" | sed -nE 's#.* -o ([^ ]*)\.last\.md.*#\1#p')
  say "START RECEIPT (adopted): builder_pid=$BUILDER_PID started=$(date -u -d @"$(proc_start "$BUILDER_PID")" +%FT%T 2>/dev/null) argv=$argv"
  trap 'say "$(stamp)  interrupted while watching adopted builder $BUILDER_PID — it is STILL RUNNING and now unwatched."; say "        re-attach with:  round $WT $FINDINGS $NAME --adopt $BUILDER_PID"; exit 130' INT TERM
  deadline=$(( $(date +%s) + MINUTES * 60 ))
  while kill -0 "$BUILDER_PID" 2>/dev/null; do
    if [ "$(date +%s)" -ge "$deadline" ]; then kill "$BUILDER_PID" 2>/dev/null; BUILD=timeout; break; fi
    sleep 10
  done
  trap - INT TERM
  step BUILD $(( $(date +%s) - t )) "$BUILD (adopted pid=$BUILDER_PID)"
else
  t=$(phase_t0)
  [ -f "$FINDINGS" ] || refuse "no such findings file: $FINDINGS"
  BRIEF=$OUT/$NAME-brief.md
  REPORT=$OUT/$NAME-report.md
  BLOG=$OUT/$NAME-builder.log
  rm -f "$REPORT" "$REPORT.last.md"
  cat > "$BRIEF" <<EOF
You have EXCLUSIVE ownership of $WT.

METHOD — the in-image loop (this is the method, not a suggestion):
  * warm image: one nREPL for the worktree, kept for the whole round; never kill a JVM
    you did not start.
  * after EVERY change: split-in-image, then $PAPERCUT <fixture>. Seconds, not minutes.
  * per iteration: warm \`clojure.test/run-tests\` on the AFFECTED namespaces in that
    image. Never a cold kaocha per iteration.
  * ONE cold \`make test\` at the very end, as the final proof.
  * every fix gets a FAIL-FIRST witness with a linked-intent id (NS-SPLIT-0xx,
    linked-intent-testing markers); write the witness from the oracle's example, not
    from prose.
  * commit on the branch $BRANCH as forge-anvil with trailer
    \`Co-Authored-By: Gene Kim <genek@itrevolution.com>\`. DO NOT PUSH.
  * write your report to $REPORT and print your PID on its first line.
  * log iterations to $OUT/$NAME-loop.log — one line per iteration: wall seconds,
    warm-test wall, oracle total.

FINDINGS TO ACT ON (verbatim, below this line):
--------------------------------------------------------------------------
EOF
  cat "$FINDINGS" >> "$BRIEF"

  LAUNCH_EPOCH=$(date +%s)
  # `9>&- 8>&-` IS THE REPAIR for the leaked-lock defect: the builder outlives this
  # round by design, so if it inherits the lock descriptor the lock outlives the
  # round too and every later attempt — including --adopt — is refused by its own
  # subject. Closing them here is what makes .round.lock mean "a round is running"
  # again instead of "a round once started here". Closing an fd that is not open is
  # not an error, so this is correct in adopt mode too.
  CODEX_BIN=/home/forge/.local/bin/codex CODEX_MODEL="$MODEL" \
    nohup setsid /home/forge/bin/sol-yolo "$WT" "$BRIEF" "" "$REPORT" \
    > "$BLOG" 2>&1 </dev/null 9>&- 8>&- &
  WRAPPER=$!

  # THE RECEIPT: the wrapper pid is NOT the run. Find the codex CHILD by its `-C`
  # fence (see codex_pids_in).  90 s, not 30: a cold codex start behind a slow
  # network has been seen past 30 s, and the cost of giving up early is an orphan.
  for _ in $(seq 1 90); do
    BUILDER_PID=$(codex_leader_in "$WT" "$LAUNCH_EPOCH")
    [ -n "$BUILDER_PID" ] && break
    sleep 1
  done
  if [ -z "$BUILDER_PID" ]; then
    say "$(stamp)  builder log head:"; head -c 600 "$BLOG" | tee -a "$LOG"
    refuse_after_launch "no codex child fenced to $WT after 90 s (wrapper=$WRAPPER, log=$BLOG)"
  fi
  LOGMODEL=$(grep -m1 -E '^[[:space:]]*model:' "$BLOG" 2>/dev/null | sed -E 's/.*model:[[:space:]]*//' | tr -d '\r')
  say "START RECEIPT: builder_pid=$BUILDER_PID wrapper=$WRAPPER model=${LOGMODEL:-<not-yet-logged>} log=$BLOG brief=$BRIEF report=$REPORT started=$(stamp)"

  # An interrupt while we wait must NOT silently orphan the builder. It also must not
  # DESTROY it — the builder is the expensive thing in this routine. So we hand over
  # the handle instead: print the exact re-attach command and leave the run alone.
  trap 'say "$(stamp)  interrupted while waiting on builder $BUILDER_PID — it is STILL RUNNING and now unwatched."; say "        re-attach with:  round $WT $FINDINGS $NAME --adopt $BUILDER_PID"; exit 130' INT TERM

  # wait on the CHILD, never on the wrapper, never under an outer `timeout`.
  deadline=$(( $(date +%s) + MINUTES * 60 ))
  while kill -0 "$BUILDER_PID" 2>/dev/null; do
    if [ "$(date +%s)" -ge "$deadline" ]; then
      # the exact pid, never pkill, never an outer `timeout`; then the wrapper, so
      # a ceiling leaves nothing of ours running.
      kill "$BUILDER_PID" 2>/dev/null
      kill "$WRAPPER" 2>/dev/null
      BUILD=timeout
      break
    fi
    sleep 10
  done
  trap - INT TERM
  BW=$(( $(date +%s) - t ))

  # the model line only appears once codex has started; re-read it now that it has run.
  LOGMODEL=$(grep -m1 -E '^[[:space:]]*model:' "$BLOG" 2>/dev/null | sed -E 's/.*model:[[:space:]]*//' | tr -d '\r')
  case "$LOGMODEL" in
    "$MODEL"*) : ;;
    "") say "$(stamp)  FAIL: builder log has no 'model:' line ($BLOG)"; BUILD=wrong-model ;;
    *)  say "$(stamp)  FAIL: wrong model — asked $MODEL, log says '$LOGMODEL'"; BUILD=wrong-model ;;
  esac
  step BUILD "$BW" "$BUILD (pid=$BUILDER_PID model='${LOGMODEL:-none}')"
fi

# ===========================================================================
# 4. COMMIT CHECK
# ===========================================================================
t=$(phase_t0)
NEW_TIP=$(git -C "$WT" rev-parse HEAD)
dirty=$(git -C "$WT" status --porcelain --untracked-files=no 2>&1)
if [ "$NOBUILD" = 1 ]; then
  step COMMIT_CHECK $(( $(date +%s) - t )) "skipped (--no-build); grading tip ${NEW_TIP:0:8}"
else
  if [ "$NEW_TIP" = "$OLD_TIP" ]; then
    BUILD=nocommit
    :
    step COMMIT_CHECK $(( $(date +%s) - t )) "NO COMMIT — tip unchanged at ${OLD_TIP:0:8}"
  elif [ -n "$dirty" ]; then
    BUILD=nocommit
    say "$dirty"
    step COMMIT_CHECK $(( $(date +%s) - t )) "NO COMMIT — tip moved but tree is dirty"
  else
    step COMMIT_CHECK $(( $(date +%s) - t )) "ok ${OLD_TIP:0:8} -> ${NEW_TIP:0:8}, tree clean"
  fi
fi

# ===========================================================================
# 5. GRADE
# ===========================================================================
t=$(phase_t0)
if [ -z "$FIXTURE" ]; then
  FIXTURE=/var/tmp/forge/plan2/build/round-$NAME-fx
  if [ ! -d "$FIXTURE/.git" ] && [ ! -f "$FIXTURE/.git" ]; then
    git -C "$CC_SRC" worktree add --detach "$FIXTURE" "$CC_SHA" >>"$LOG" 2>&1 </dev/null \
      || refuse "cannot create fixture worktree $FIXTURE"
  fi
fi
[ -d "$FIXTURE" ] || refuse "no such fixture: $FIXTURE"

# reset: tracked back to HEAD, untracked split output removed.  The guard test and
# .clj-surgeon.edn are untracked, so they are re-laid AFTER the clean, every time.
git -C "$FIXTURE" checkout -q -- . </dev/null
git -C "$FIXTURE" clean -fdq src test </dev/null
cp "$GUARD_TEST" "$FIXTURE/test/cfp_scheduler_killer/view_architecture_test.clj"
cat > "$FIXTURE/.clj-surgeon.edn" <<EOF
{:verification-profiles {"split-unit" {:commands [["bin/kaocha" "unit" "--fail-fast"]
                                                  ["python3" "$CC_ORACLE" "$FIXTURE"]]}}}
EOF

if [ -z "$REQUEST" ]; then
  REQUEST=$OUT/$NAME-request.edn
  sed -E "s#:workspace_root \"[^\"]*\"#:workspace_root \"$FIXTURE\"#" "$BASE_REQUEST" > "$REQUEST"
fi

RECEIPT=$OUT/$NAME-receipt.edn
( cd "$FIXTURE" && bb --classpath "$WT/src" -m clj-surgeon.core \
    :op :split-ns! :request-file "$REQUEST" ) > "$RECEIPT" 2>"$OUT/$NAME-receipt.err" </dev/null 9>&- 8>&-
CALL_RC=$?
STATE=$(grep -o ':state "[^"]*"' "$RECEIPT" | head -1 | sed 's/.*"\(.*\)"/\1/')
IN_CALL=$(grep -o ':elapsed_ms [0-9.]*' "$RECEIPT" | head -1 | awk '{printf "%.0f", $2}')
[ -n "$STATE" ] || STATE="no-receipt(rc=$CALL_RC)"
[ -n "$IN_CALL" ] || IN_CALL=0
say "$(stamp)  split call: rc=$CALL_RC state=$STATE in_call_ms=$IN_CALL receipt=$RECEIPT"

# papercut oracle
POUT=$OUT/$NAME-papercuts.log
python3 "$PAPERCUT" "$FIXTURE" > "$POUT" 2>&1 </dev/null 9>&- 8>&-
PAPERCUTS=$(grep -m1 '^PAPERCUTS:' "$POUT" | awk '{print $2}')
[ -n "$PAPERCUTS" ] || PAPERCUTS=ERR
say "$(stamp)  PAPERCUTS: $PAPERCUTS   (full report: $POUT)"
# the non-advisory rows with count > 0, from the oracle's own table
awk '$NF=="yes" && $(NF-2)+0 > 0 {printf "        papercut row: %s = %s %s\n", $1, $(NF-2), $(NF-1)}' "$POUT" | tee -a "$LOG"

# the four fixture oracles
O1=fail O2=fail O3=fail O4=fail
[ ! -e "$FIXTURE/src/cfp_scheduler_killer/views.clj" ] && O1=pass
python3 "$CC_ORACLE" "$FIXTURE" > "$OUT/$NAME-cc-oracle.log" 2>&1 </dev/null 9>&- 8>&-
grep -q '^PASS:' "$OUT/$NAME-cc-oracle.log" && O2=pass
# the unit suite ran INSIDE the call (state committed => it was green); do not rerun it.
case "$STATE" in committed*) O3=pass ;; esac
( cd "$FIXTURE" && bin/kaocha unit --focus cfp-scheduler-killer.view-architecture-test ) \
  > "$OUT/$NAME-guard.log" 2>&1 </dev/null 9>&- 8>&-
grep -qE '7 assertions, 0 failures' "$OUT/$NAME-guard.log" && O4=pass
ORACLES=0
for o in "$O1" "$O2" "$O3" "$O4"; do [ "$o" = pass ] && ORACLES=$((ORACLES+1)); done
say "$(stamp)  oracles: views.clj-gone=$O1 cc-oracle=$O2 unit-suite-in-call=$O3 arch-guard-7/7=$O4  => $ORACLES/4"
step GRADE $(( $(date +%s) - t )) "state=$STATE in_call=${IN_CALL}ms PAPERCUTS=$PAPERCUTS oracles=$ORACLES/4"

# ===========================================================================
# 6. WITNESSES — the namespace-split test namespaces, cold but FOCUSED.
# The lane aliases concatenate their :main-opts, so `--ns` must go through
# :clj-surgeon/test-deps, which carries the deps and names no entry point.
# Measured cheapest correct invocation: ~8 s for all three namespaces.
# ===========================================================================
t=$(phase_t0)
WLOG=$OUT/$NAME-witnesses.log
( cd "$WT" && clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps \
    -m clj-surgeon.mcp-test-runner --ns \
    clj-surgeon.namespace-split-test \
    clj-surgeon.mcp-namespace-split-test \
    clj-surgeon.namespace-split-warm-test ) > "$WLOG" 2>&1 </dev/null 9>&- 8>&-
WRC=$?
WLINE=$(grep -m1 -E '^Ran [0-9]+ tests' "$WLOG")
WT_TESTS=$(echo "$WLINE" | awk '{print $2}'); WT_ASSERTS=$(echo "$WLINE" | awk '{print $5}')
WFAIL=$(grep -m1 -E '^[0-9]+ failures, [0-9]+ errors' "$WLOG")
WT_FAILS=$(echo "$WFAIL" | awk '{print $1}'); WT_ERRS=$(echo "$WFAIL" | awk '{print $3}')
: "${WT_TESTS:=?}" "${WT_ASSERTS:=?}" "${WT_FAILS:=?}" "${WT_ERRS:=?}"
step WITNESSES $(( $(date +%s) - t )) "rc=$WRC ${WT_TESTS}t/${WT_ASSERTS}a/${WT_FAILS}f/${WT_ERRS}e ($WLOG)"

# ===========================================================================
# 7. RECEIPT
# ===========================================================================
TOTAL=$(( $(date +%s) - T0 ))
say ""
say "  $(printf '%-14s %8s  %s' PHASE 'WALL s' RESULT)"
say "  ------------------------------------------------------------------"
for p in "${PHASES[@]}"; do
  say "  $(printf '%-14s %8s  %s' "${p%%|*}" "$(echo "$p" | cut -d'|' -f2)" "$(echo "$p" | cut -d'|' -f3-)")"
done
say "  ------------------------------------------------------------------"
say "  $(printf '%-14s %8s' TOTAL "$TOTAL")"
say ""
LINE="ROUND $NAME: tip ${OLD_TIP:0:8}→${NEW_TIP:0:8} build=$BUILD state=$STATE in_call=${IN_CALL}ms PAPERCUTS=$PAPERCUTS oracles=$ORACLES/4 witnesses=${WT_TESTS}/${WT_ASSERTS}/${WT_FAILS} total=${TOTAL}s"
say "$LINE"
printf '%s  %s\n' "$(stamp)" "$LINE" >> "$LEDGER"

RC=0
case "$BUILD" in ok) : ;; *) RC=1 ;; esac
case "$STATE" in committed*) : ;; *) RC=1 ;; esac
[ "$PAPERCUTS" = 0 ] || RC=1
[ "$ORACLES" = 4 ] || RC=1
[ "$WT_FAILS" = 0 ] && [ "$WT_ERRS" = 0 ] || RC=1
say "$(stamp)  exit $RC   (log: $LOG, ledger: $LEDGER)"
exit $RC
