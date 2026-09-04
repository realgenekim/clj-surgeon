#!/usr/bin/env bash
# sabotage-FAN.sh — prove the FAN scorer can go RED before any cohort trusts it green.
#
#   sabotage-FAN.sh <fixtures-dir> <N> [scratch-dir]
#   sabotage-FAN.sh --selftest-<mode> [N] [seed] [scratch-dir]
#   sabotage-FAN.sh --selftest-list
#
# The self-test MODES ARE NOT LISTED HERE AS A NUMBER.  A hand-maintained count is a
# claim about a file that changes without it: the round-3 review found the request
# asking for "all seven self-test modes" against a tree that exposed six.  So
# `--selftest-list` derives the roster from THIS FILE's own dispatch lines, every
# self-test run prints that same roster before its first assertion, and the count is
# computed, never typed.
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

# --- the self-test roster, DERIVED from this file's own dispatch ------------------
# `selftest_modes` greps the very lines that decide which mode runs, so the printed
# list cannot drift from the modes that exist -- which a typed count already did once.
selftest_modes () {
  sed -n 's/^if \[ "\${1:-}" = "\(--selftest-[a-z-]*\)" \];.*/\1/p' "${BASH_SOURCE[0]}" \
    | grep -v -- '^--selftest-list$'
}
print_selftest_roster () {
  echo "sabotage-FAN: self-test modes ($(selftest_modes | wc -l | tr -d ' ')): $(selftest_modes | tr '\n' ' ')"
}

if [ "${1:-}" = "--selftest-list" ]; then
  print_selftest_roster
  selftest_modes
  exit 0
fi

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
  print_selftest_roster
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

# --- self-test: CHECK 1 on a legal POSIX tree containing a directory literally ----
# named backslash (a single "\" byte, not an escape) -----------------------------
#   sabotage-FAN.sh --selftest-backslash [N] [seed] [scratch-dir]
#
# Defect inb-9c18e2: `git diff --name-only` C-quotes any path containing a backslash
# regardless of core.quotePath, so a raw manifest path with a literal "\" component
# never string-matches the quoted spelling git prints -- CHECK 1 reports the two
# owners both missing (raw spelling) and extra (quoted spelling) even though the
# migration is byte-identical to canonical.  This witness:
#   - takes gen-fanout's own repo-N/canonical-N/manifest-N.edn at k=6 as the base,
#   - plants two owners as BYTE COPIES of ns_003 and ns_005 under a directory named
#     exactly one backslash (src/acid/fanout/\/), committed as the pre-migration BASE,
#   - derives the matching canonical the same way (byte copies into the same paths),
#   - extends the manifest to N+2 targets for the two new owners,
#   - simulates the migration by copying canonical/src over the worktree -- no server,
#   - asserts CHECK 1 reports missing=0 extras=0 (the correct, fixed reading).
# Pre-fix (git diff --name-only, no -z) this goes RED with missing=2 extras=2.
if [ "${1:-}" = "--selftest-backslash" ]; then
  print_selftest_roster
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fan-selftest-backslash}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  T3="src/acid/fanout/ns_003.clj"; T5="src/acid/fanout/ns_005.clj"
  BS_T3="src/acid/fanout/\\/ns_003.clj"; BS_T5="src/acid/fanout/\\/ns_005.clj"

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && git init -q . \
      && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  mkdir -p "$SCRATCH/repo/src/acid/fanout/\\"
  cp "$SCRATCH/repo/$T3" "$SCRATCH/repo/$BS_T3"
  cp "$SCRATCH/repo/$T5" "$SCRATCH/repo/$BS_T5"
  ( cd "$SCRATCH/repo" && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q \
           -m "fanout: plant two owners under a directory literally named backslash (pre-migration)" )
  BASE=$(git -C "$SCRATCH/repo" rev-parse HEAD)

  rm -rf "$SCRATCH/canonical"; mkdir -p "$SCRATCH/canonical"
  cp -r "$SCRATCH/gen/canonical-$N/." "$SCRATCH/canonical/"
  mkdir -p "$SCRATCH/canonical/src/acid/fanout/\\"
  cp "$SCRATCH/gen/canonical-$N/$T3" "$SCRATCH/canonical/$BS_T3"
  cp "$SCRATCH/gen/canonical-$N/$T5" "$SCRATCH/canonical/$BS_T5"

  cat > "$SCRATCH/extend-manifest.clj" <<'CLJEOF'
(require '[clojure.pprint :as pp])
(let [[in out t3-path t5-path bs-t3 bs-t5 n2-str] *command-line-args*
      n2 (Integer/parseInt n2-str)
      m (read-string (slurp in))
      t3 (first (filter #(= (:file %) t3-path) (:targets m)))
      t5 (first (filter #(= (:file %) t5-path) (:targets m)))
      new3 (assoc t3 :file bs-t3 :ns "acid.fanout.owner3")
      new5 (assoc t5 :file bs-t5 :ns "acid.fanout.owner5")
      m2 (-> m (assoc :n n2) (update :targets #(vec (concat % [new3 new5]))))]
  (spit out (with-out-str (pp/pprint m2))))
CLJEOF
  N2=$((N + 2))
  bb "$SCRATCH/extend-manifest.clj" "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/manifest-$N2.edn" \
    "$T3" "$T5" "$BS_T3" "$BS_T5" "$N2"

  # simulate the migration: copy canonical src over the worktree -- no server
  cp -r "$SCRATCH/canonical/src/." "$SCRATCH/repo/src/"

  OUT=$(bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/manifest-$N2.edn" "$SCRATCH/canonical" "$BASE" 2>&1)
  RC=$?
  CHECK1=$(printf '%s\n' "$OUT" | grep '^CHECK 1 ' | head -1)
  echo "SELFTEST-BACKSLASH: $CHECK1"
  if printf '%s' "$CHECK1" | grep -q 'PASS' \
     && printf '%s' "$CHECK1" | grep -q 'missing=0' \
     && printf '%s' "$CHECK1" | grep -q 'extras=0'; then
    echo "SELFTEST-BACKSLASH: PASS -- CHECK 1 correctly reads a directory literally named backslash (missing=0 extras=0)"
    exit 0
  else
    echo "SELFTEST-BACKSLASH: FAIL -- CHECK 1 misreads the backslash-named directory (want missing=0 extras=0)"
    exit 1
  fi
fi

# --- self-test: a failing `git ls-files`/`git diff` must fail the gate closed, ---
# never a silent false PASS (Sol round-1 review, finding 1, BLOCKER) --------------
#   sabotage-FAN.sh --selftest-listing-failure [N] [seed] [scratch-dir]
#
# fan_check.clj:47-55 checked only `git diff`'s exit before consuming its output;
# `git ls-files --others --exclude-standard` was parsed regardless of its own exit,
# so a failing listing process read as an empty set of untracked files and an
# untracked EXTRA file could disappear from CHECK 1 -- the gate could false-PASS.
# This witness builds a tree that is genuinely 6/6-correct EXCEPT for one untracked
# extra file the manifest does not own, then runs fan_check.clj three ways under a
# PATH-shimmed `git` that exits 42 for one named subcommand only:
#   - real git (control)   -> CHECK 1 must FAIL, extras=1 (the extra is really there)
#   - `ls-files` shimmed   -> must NOT print CHECK 1 file-set: PASS; must exit
#                             nonzero with an ERROR line naming the exit code
#   - `diff` shimmed       -> same requirement (already fail-closed pre-fix; must
#                             stay that way)
# The `ls-files` case also runs the full six-check gate via rescore-FAN.sh, exactly
# as the reviewer did, to prove the false PASS cannot propagate past CHECK 1.
if [ "${1:-}" = "--selftest-listing-failure" ]; then
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/tmp/fanout-r2-fx/selftest-listing-failure}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  LPASS=0; LFAIL=0
  ok()  { LPASS=$((LPASS+1)); echo "SELFTEST-LISTING-FAILURE $1: PASS $2"; }
  bad() { LFAIL=$((LFAIL+1)); echo "SELFTEST-LISTING-FAILURE $1: FAIL $2"; }

  REALGIT=$(command -v git)

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && git init -q . \
      && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  BASE=$(git -C "$SCRATCH/repo" rev-parse HEAD)

  # simulate the CORRECT migration -- no server -- so the only defect present is
  # the untracked extra file planted below (an ordinary migration would be 6/6).
  cp -r "$SCRATCH/gen/canonical-$N/src/." "$SCRATCH/repo/src/"

  # plant one untracked extra file the manifest does not own (exactly the
  # reviewer's repro: src/acid/fanout/extra.clj, never `git add`ed).
  printf '(ns acid.fanout.extra)\n;; an untracked file the manifest does not own\n' \
    > "$SCRATCH/repo/src/acid/fanout/extra.clj"

  # --- build the two PATH shims: exit 42 for exactly one subcommand, real git otherwise
  mk_shim () {   # mk_shim <dir> <failing-subcommand>
    local dir=$1 sub=$2
    mkdir -p "$dir"
    cat > "$dir/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = "$sub" ]; then echo "simulated-$sub-failure" >&2; exit 42; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
    chmod +x "$dir/git"
  }
  mk_shim "$SCRATCH/bin-ls-files" "ls-files"
  mk_shim "$SCRATCH/bin-diff" "diff"

  # --- control: real git must catch the extra -------------------------------------
  OUT_REAL=$(bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/gen/manifest-$N.edn" \
    "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  CHECK1_REAL=$(printf '%s\n' "$OUT_REAL" | grep '^CHECK 1 ' | head -1)
  echo "SELFTEST-LISTING-FAILURE control: $CHECK1_REAL"
  if printf '%s' "$CHECK1_REAL" | grep -q 'FAIL' && printf '%s' "$CHECK1_REAL" | grep -q 'extras=1'; then
    ok "control real-git" "CHECK 1 correctly catches the untracked extra -- $CHECK1_REAL"
  else
    bad "control real-git" "expected CHECK 1 FAIL extras=1, got: $CHECK1_REAL"
  fi

  # --- ls-files shimmed: must not false-PASS ---------------------------------------
  OUT_LS=$(PATH="$SCRATCH/bin-ls-files:$PATH" FAN_GIT="$SCRATCH/bin-ls-files/git" bb "$HERE/fan_check.clj" "$SCRATCH/repo" \
    "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  RC_LS=$?
  echo "SELFTEST-LISTING-FAILURE ls-files-shimmed: rc=$RC_LS"
  printf '%s\n' "$OUT_LS" | sed 's/^/    /'
  if [ $RC_LS -ne 0 ] \
     && printf '%s' "$OUT_LS" | grep -Eq 'CHECK 1 file-set: (ERROR|FAIL).*ls-files.*exit=42' \
     && ! printf '%s' "$OUT_LS" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "ls-files-shimmed fail-closed" "rc=$RC_LS, no false PASS, named the ls-files exit"
  else
    bad "ls-files-shimmed fail-closed" "rc=$RC_LS -- want nonzero rc, an ERROR/FAIL line naming ls-files exit=42, and no PASS line"
  fi

  # --- diff shimmed: must also fail closed (already correct pre-fix; must stay so)
  OUT_DIFF=$(PATH="$SCRATCH/bin-diff:$PATH" FAN_GIT="$SCRATCH/bin-diff/git" bb "$HERE/fan_check.clj" "$SCRATCH/repo" \
    "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  RC_DIFF=$?
  echo "SELFTEST-LISTING-FAILURE diff-shimmed: rc=$RC_DIFF"
  printf '%s\n' "$OUT_DIFF" | sed 's/^/    /'
  if [ $RC_DIFF -ne 0 ] \
     && printf '%s' "$OUT_DIFF" | grep -Eq 'CHECK 1 file-set: (ERROR|FAIL).*diff' \
     && ! printf '%s' "$OUT_DIFF" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "diff-shimmed fail-closed" "rc=$RC_DIFF, no false PASS, named the git diff failure"
  else
    bad "diff-shimmed fail-closed" "rc=$RC_DIFF -- want nonzero rc, an ERROR/FAIL line naming the git diff failure, and no PASS line"
  fi

  # --- the same, through the full six-check gate, exactly as the reviewer did -----
  RSOUT=$(PATH="$SCRATCH/bin-ls-files:$PATH" FAN_GIT="$SCRATCH/bin-ls-files/git" FAN_FIXTURES="$SCRATCH/gen" FAN_BASE="$BASE" \
    bash "$HERE/rescore-FAN.sh" "$SCRATCH/repo" "$N" 2>&1)
  RSRC=$?
  echo "SELFTEST-LISTING-FAILURE full-gate ls-files-shimmed: rc=$RSRC"
  printf '%s\n' "$RSOUT" | sed 's/^/    /'
  if [ $RSRC -ne 0 ] && ! printf '%s' "$RSOUT" | grep -q '6/6 checks passed'; then
    ok "full-gate fail-closed" "rescore-FAN did not report 6/6 with a failing ls-files"
  else
    bad "full-gate fail-closed" "rescore-FAN reported 6/6 (or rc=0) with a failing ls-files -- false PASS reached the gate"
  fi

  echo "sabotage-FAN --selftest-listing-failure: $LPASS passed, $LFAIL failed"
  [ $LFAIL -eq 0 ]
  exit $?
fi

# --- self-test: CHECK 1 must not false-FAIL a legal path consisting solely -------
# of whitespace (Sol round-1 review, finding 2) -----------------------------------
#   sabotage-FAN.sh --selftest-whitespace-path [N] [seed] [scratch-dir]
#
# fan_check.clj's NUL-splitter used `str/blank?` to drop empty separators, but
# `str/blank?` is also true for a legal path that is itself all whitespace (e.g. a
# file literally named " ").  NUL framing (-z) never produces an empty-but-nonblank
# separator, so the right predicate is `empty?`.  This witness plants one manifest
# owner at the path " " (a single space, the exact byte from the reviewer's repro),
# migrates it correctly, and asserts CHECK 1 reads it present (missing=0 extras=0)
# rather than reporting it missing.
if [ "${1:-}" = "--selftest-whitespace-path" ]; then
  print_selftest_roster
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/tmp/fanout-r2-fx/selftest-whitespace-path}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  T3="src/acid/fanout/ns_003.clj"
  WS=" "   # a legal POSIX relative path consisting solely of one whitespace byte

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && git init -q . \
      && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  cp "$SCRATCH/repo/$T3" "$SCRATCH/repo/$WS"
  ( cd "$SCRATCH/repo" && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q \
           -m "fanout: plant one owner at a path consisting solely of whitespace (pre-migration)" )
  BASE=$(git -C "$SCRATCH/repo" rev-parse HEAD)

  rm -rf "$SCRATCH/canonical"; mkdir -p "$SCRATCH/canonical"
  cp -r "$SCRATCH/gen/canonical-$N/." "$SCRATCH/canonical/"
  cp "$SCRATCH/gen/canonical-$N/$T3" "$SCRATCH/canonical/$WS"

  cat > "$SCRATCH/extend-manifest.clj" <<'CLJEOF'
(require '[clojure.pprint :as pp])
(let [[in out t3-path ws n2-str] *command-line-args*
      n2 (Integer/parseInt n2-str)
      m (read-string (slurp in))
      t3 (first (filter #(= (:file %) t3-path) (:targets m)))
      new-ws (assoc t3 :file ws :ns "acid.fanout.ownerws")
      m2 (-> m (assoc :n n2) (update :targets #(vec (concat % [new-ws]))))]
  (spit out (with-out-str (pp/pprint m2))))
CLJEOF
  N2=$((N + 1))
  bb "$SCRATCH/extend-manifest.clj" "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/manifest-$N2.edn" \
    "$T3" "$WS" "$N2"

  # simulate the FULL migration -- no server -- so every one of the 22 targets
  # (the 21 ordinary owners plus the whitespace-only path) is genuinely migrated;
  # otherwise the 21 ordinary owners would show as unrelated missing=21 and mask
  # the one assertion this witness is for.
  cp -r "$SCRATCH/canonical/src/." "$SCRATCH/repo/src/"
  cp "$SCRATCH/canonical/$WS" "$SCRATCH/repo/$WS"

  OUT=$(bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/manifest-$N2.edn" "$SCRATCH/canonical" "$BASE" 2>&1)
  CHECK1=$(printf '%s\n' "$OUT" | grep '^CHECK 1 ' | head -1)
  echo "SELFTEST-WHITESPACE-PATH: $CHECK1"
  if printf '%s' "$CHECK1" | grep -q 'PASS' \
     && printf '%s' "$CHECK1" | grep -q 'missing=0' \
     && printf '%s' "$CHECK1" | grep -q 'extras=0'; then
    echo "SELFTEST-WHITESPACE-PATH: PASS -- CHECK 1 correctly reads a legal path consisting solely of whitespace (missing=0 extras=0)"
    exit 0
  else
    echo "SELFTEST-WHITESPACE-PATH: FAIL -- CHECK 1 misreads the whitespace-only path (want missing=0 extras=0)"
    exit 1
  fi
fi

# --- self-test: a `git ls-files` that exits 0 but is INCOMPLETE must fail the ----
# gate closed, never a silent false PASS (Sol round-2 review, finding 1, BLOCKER) --
#   sabotage-FAN.sh --selftest-incomplete-listing [N] [seed] [scratch-dir]
#
# --selftest-listing-failure (round 1) proved a NONZERO-exit ls-files fails closed.
# It does not prove an exit-0, INCOMPLETE ls-files fails closed -- and the round-2
# reviewer's exact PATH shim (`for arg in "$@"; do if [ "$arg" = ls-files ]; then
# exit 0; fi; done; exec git "$@"`) does exactly that: exit 0, empty stdout, no
# stderr, for a real untracked extra file the manifest does not own. This witness
# plants TWO untracked extras, then runs fan_check.clj under two shims:
#   - empty-output shim  -> exit 0, stdout entirely empty (the reviewer's repro)
#   - partial-output shim -> exit 0, echoes ONE real record, silently drops the
#     other (a shim that half-lies is a harder case than one that lies completely)
# Since round 4 the scorer never resolves `git` through PATH, so each shim is ALSO
# handed to it as $FAN_GIT: the adversary's binary becomes the one the scorer runs,
# which is strictly harder than the PATH attack these cases originally modelled.
# Both must fail closed: nonzero exit, a `CHECK 1 file-set: ERROR listing-incomplete`
# line, and no `CHECK 1 file-set: PASS` line -- and the full six-check gate must not
# report 6/6 with either shim on PATH.
if [ "${1:-}" = "--selftest-incomplete-listing" ]; then
  print_selftest_roster
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fanout-r3-fx/selftest-incomplete-listing}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  IPASS=0; IFAIL=0
  ok()  { IPASS=$((IPASS+1)); echo "SELFTEST-INCOMPLETE-LISTING $1: PASS $2"; }
  bad() { IFAIL=$((IFAIL+1)); echo "SELFTEST-INCOMPLETE-LISTING $1: FAIL $2"; }

  REALGIT=$(command -v git)

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && git init -q . \
      && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  BASE=$(git -C "$SCRATCH/repo" rev-parse HEAD)

  # simulate the CORRECT migration -- no server -- so the only defect present is
  # the two untracked extras planted below (an ordinary migration would be 6/6).
  cp -r "$SCRATCH/gen/canonical-$N/src/." "$SCRATCH/repo/src/"

  # plant TWO untracked extras the manifest does not own.
  printf '(ns acid.fanout.extra1)\n;; untracked, manifest does not own it\n' \
    > "$SCRATCH/repo/src/acid/fanout/extra1.clj"
  printf '(ns acid.fanout.extra2)\n;; untracked, manifest does not own it\n' \
    > "$SCRATCH/repo/src/acid/fanout/extra2.clj"

  # --- shim 1: exit 0, EMPTY stdout for `ls-files` -- the reviewer's exact repro
  mkdir -p "$SCRATCH/bin-empty"
  cat > "$SCRATCH/bin-empty/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-empty/git"

  # --- shim 2: exit 0, PARTIAL stdout for `ls-files` -- echoes extra1.clj only,
  # silently drops extra2.clj, no stderr either way
  mkdir -p "$SCRATCH/bin-partial"
  cat > "$SCRATCH/bin-partial/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then printf 'src/acid/fanout/extra1.clj\0'; exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-partial/git"

  # --- control: real git must catch both extras ------------------------------------
  OUT_REAL=$(bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/gen/manifest-$N.edn" \
    "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  CHECK1_REAL=$(printf '%s\n' "$OUT_REAL" | grep '^CHECK 1 ' | head -1)
  echo "SELFTEST-INCOMPLETE-LISTING control: $CHECK1_REAL"
  if printf '%s' "$CHECK1_REAL" | grep -q 'FAIL' && printf '%s' "$CHECK1_REAL" | grep -q 'extras=2'; then
    ok "control real-git" "CHECK 1 correctly catches both untracked extras -- $CHECK1_REAL"
  else
    bad "control real-git" "expected CHECK 1 FAIL extras=2, got: $CHECK1_REAL"
  fi

  # --- empty-output shim: must not false-PASS ---------------------------------------
  OUT_EMPTY=$(PATH="$SCRATCH/bin-empty:$PATH" FAN_GIT="$SCRATCH/bin-empty/git" bb "$HERE/fan_check.clj" "$SCRATCH/repo" \
    "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  RC_EMPTY=$?
  echo "SELFTEST-INCOMPLETE-LISTING empty-output-shimmed: rc=$RC_EMPTY"
  printf '%s\n' "$OUT_EMPTY" | sed 's/^/    /'
  if [ $RC_EMPTY -ne 0 ] \
     && printf '%s' "$OUT_EMPTY" | grep -q 'CHECK 1 file-set: ERROR listing-incomplete' \
     && ! printf '%s' "$OUT_EMPTY" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "empty-output-shimmed fail-closed" "rc=$RC_EMPTY, no false PASS, named listing-incomplete"
  else
    bad "empty-output-shimmed fail-closed" "rc=$RC_EMPTY -- want nonzero rc, a listing-incomplete ERROR line, and no PASS line"
  fi

  # --- partial-output shim: must not false-PASS -------------------------------------
  OUT_PARTIAL=$(PATH="$SCRATCH/bin-partial:$PATH" FAN_GIT="$SCRATCH/bin-partial/git" bb "$HERE/fan_check.clj" "$SCRATCH/repo" \
    "$SCRATCH/gen/manifest-$N.edn" "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  RC_PARTIAL=$?
  echo "SELFTEST-INCOMPLETE-LISTING partial-output-shimmed: rc=$RC_PARTIAL"
  printf '%s\n' "$OUT_PARTIAL" | sed 's/^/    /'
  if [ $RC_PARTIAL -ne 0 ] \
     && printf '%s' "$OUT_PARTIAL" | grep -q 'CHECK 1 file-set: ERROR listing-incomplete' \
     && ! printf '%s' "$OUT_PARTIAL" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "partial-output-shimmed fail-closed" "rc=$RC_PARTIAL, no false PASS, named listing-incomplete"
  else
    bad "partial-output-shimmed fail-closed" "rc=$RC_PARTIAL -- want nonzero rc, a listing-incomplete ERROR line, and no PASS line"
  fi

  # --- the same, through the full six-check gate, exactly as the reviewer did ------
  RSOUT=$(PATH="$SCRATCH/bin-empty:$PATH" FAN_GIT="$SCRATCH/bin-empty/git" FAN_FIXTURES="$SCRATCH/gen" FAN_BASE="$BASE" \
    bash "$HERE/rescore-FAN.sh" "$SCRATCH/repo" "$N" 2>&1)
  RSRC=$?
  echo "SELFTEST-INCOMPLETE-LISTING full-gate empty-output-shimmed: rc=$RSRC"
  printf '%s\n' "$RSOUT" | sed 's/^/    /'
  if [ $RSRC -ne 0 ] && ! printf '%s' "$RSOUT" | grep -q '6/6 checks passed'; then
    ok "full-gate fail-closed" "rescore-FAN did not report 6/6 with an incomplete listing"
  else
    bad "full-gate fail-closed" "rescore-FAN reported 6/6 (or rc=0) with an incomplete listing -- false PASS reached the gate"
  fi

  echo "sabotage-FAN --selftest-incomplete-listing: $IPASS passed, $IFAIL failed"
  [ $IFAIL -eq 0 ]
  exit $?
fi

# --- self-test: an UNREADABLE subdirectory under src/ (real stock Git, no shim) ---
# must fail the gate closed, never a silent false PASS (Sol round-2 review, --------
# finding 1, BLOCKER -- the concrete stock-Git failure mode) -----------------------
#   sabotage-FAN.sh --selftest-pruned-walk [N] [seed] [scratch-dir]
#
# With a real `chmod 000` subdirectory under src/, Git 2.53+ prints a warning on
# stderr ("could not open directory ... Permission denied") but returns 0 and an
# empty stdout listing for that subtree; Java's file-seq silently drops the same
# subtree with no signal at all. This witness plants an untracked src/hidden/
# containing forbidden `acid.fanout.store` residue -- CHECK 6 must catch it -- then
# makes it unreadable with plain `chmod`, and proves fan_check.clj and the full
# gate both fail closed under STOCK git, no PATH shim. The directory is restored
# (chmod 755) on every exit path, including failure, via a trap.
if [ "${1:-}" = "--selftest-pruned-walk" ]; then
  print_selftest_roster
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fanout-r3-fx/selftest-pruned-walk}
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  PPASS=0; PFAIL=0
  ok()  { PPASS=$((PPASS+1)); echo "SELFTEST-PRUNED-WALK $1: PASS $2"; }
  bad() { PFAIL=$((PFAIL+1)); echo "SELFTEST-PRUNED-WALK $1: FAIL $2"; }

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && git init -q . \
      && git -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && git -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  BASE=$(git -C "$SCRATCH/repo" rev-parse HEAD)

  # simulate the CORRECT migration -- no server
  cp -r "$SCRATCH/gen/canonical-$N/src/." "$SCRATCH/repo/src/"

  # plant an untracked subdirectory with forbidden residue, then make it
  # UNREADABLE -- the real stock-Git failure mode, not a lying shim.
  mkdir -p "$SCRATCH/repo/src/hidden"
  printf '(ns hidden.extra)\n(require (quote acid.fanout.store))\n' \
    > "$SCRATCH/repo/src/hidden/extra.clj"
  chmod 000 "$SCRATCH/repo/src/hidden"
  restore_perms() { chmod 755 "$SCRATCH/repo/src/hidden" 2>/dev/null || true; }
  trap restore_perms EXIT

  GIT_STDERR=$(mktemp)
  git -C "$SCRATCH/repo" ls-files -z --others --exclude-standard 2> "$GIT_STDERR" > /dev/null
  echo "SELFTEST-PRUNED-WALK stock-git stderr: $(sed -n '1p' "$GIT_STDERR")"
  rm -f "$GIT_STDERR"

  OUT=$(bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/gen/manifest-$N.edn" \
    "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1)
  RC=$?
  echo "SELFTEST-PRUNED-WALK fan_check: rc=$RC"
  printf '%s\n' "$OUT" | sed 's/^/    /'
  if [ $RC -ne 0 ] \
     && printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: ERROR listing-incomplete' \
     && ! printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "fan_check fail-closed" "rc=$RC, no false PASS, named listing-incomplete"
  else
    bad "fan_check fail-closed" "rc=$RC -- want nonzero rc, a listing-incomplete ERROR line, and no PASS line"
  fi

  RSOUT=$(FAN_FIXTURES="$SCRATCH/gen" FAN_BASE="$BASE" \
    bash "$HERE/rescore-FAN.sh" "$SCRATCH/repo" "$N" 2>&1)
  RSRC=$?
  echo "SELFTEST-PRUNED-WALK full-gate: rc=$RSRC"
  printf '%s\n' "$RSOUT" | sed 's/^/    /'
  if [ $RSRC -ne 0 ] && ! printf '%s' "$RSOUT" | grep -q '6/6 checks passed'; then
    ok "full-gate fail-closed" "rescore-FAN did not report 6/6 with an unreadable subdirectory"
  else
    bad "full-gate fail-closed" "rescore-FAN reported 6/6 (or rc=0) with an unreadable subdirectory -- false PASS reached the gate"
  fi

  restore_perms
  trap - EXIT

  echo "sabotage-FAN --selftest-pruned-walk: $PPASS passed, $PFAIL failed"
  [ $PFAIL -eq 0 ]
  exit $?
fi

# --- self-test: a COORDINATED git shim that forges the BASE inventory -------------
# (Sol round-3 review, finding 2, BLOCKER) ----------------------------------------
#   sabotage-FAN.sh --selftest-shimmed-baseline [N] [seed] [scratch-dir]
#
# Round 3 closed "ls-files lies" by cross-checking git's untracked listing against an
# independent filesystem walk of src/.  But the OTHER side of that cross-check --
# "what src/ held at the base commit" -- was itself fetched with `git ls-tree`
# through the same PATH-resolved binary.  One shim that (a) returns an empty
# `ls-files` and (b) appends the planted extra to `ls-tree` therefore makes the walk
# and the baseline agree on a lie, `unreported` empty, and the full gate 6/6 GREEN
# with a real untracked extra file on disk.
#
# This witness plants one untracked extra the manifest does not own and runs four
# ways.  `bin-bless-extra` is the reviewer's coordinated shim.  `bin-forge-object`
# is the same shim plus a `cat-file` that returns bytes which are not the requested
# object -- it exists to prove that the fix's authority is CONTENT ADDRESSING, not
# the name of a subcommand: a binary asked for object <id> must return bytes whose
# sha1 IS <id>, and no shim can satisfy that without telling the truth.
#
# The two routes the shim can take are both covered:
#   - PATH-shimmed  : the scorer must never resolve `git` through PATH at all, so
#                     the shim is not executed and the REAL listing catches the extra
#   - FAN_GIT-shimmed: the adversary's binary IS the one the scorer runs -- the
#                     strictly harder case -- and the content-addressed base
#                     inventory must catch the forged `ls-tree`
if [ "${1:-}" = "--selftest-shimmed-baseline" ]; then
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fanout-r4-fx/selftest-shimmed-baseline}
  print_selftest_roster
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  BPASS=0; BFAIL=0
  ok()  { BPASS=$((BPASS+1)); echo "SELFTEST-SHIMMED-BASELINE $1: PASS $2"; }
  bad() { BFAIL=$((BFAIL+1)); echo "SELFTEST-SHIMMED-BASELINE $1: FAIL $2"; }
  REALGIT=${FAN_REAL_GIT:-/usr/bin/git}

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  rm -rf "$SCRATCH/repo"; mkdir -p "$SCRATCH/repo"
  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
  ( cd "$SCRATCH/repo" && "$REALGIT" init -q . \
      && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil add -A . \
      && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" )
  BASE=$("$REALGIT" -C "$SCRATCH/repo" rev-parse HEAD)

  # the CORRECT migration -- so the only defect present is the untracked extra
  cp -r "$SCRATCH/gen/canonical-$N/src/." "$SCRATCH/repo/src/"
  EXTRA='src/acid/fanout/extra.clj'
  printf '(ns acid.fanout.extra)\n;; untracked, the manifest does not own it\n' \
    > "$SCRATCH/repo/$EXTRA"

  # --- shim A: ls-files lies EMPTY and ls-tree falsely blesses the extra as base ---
  mkdir -p "$SCRATCH/bin-bless-extra"
  cat > "$SCRATCH/bin-bless-extra/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then exit 0; fi
  if [ "\$arg" = ls-tree ]; then "$REALGIT" "\$@"; printf '$EXTRA\0'; exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-bless-extra/git"

  # --- shim B: the same, plus a cat-file that returns bytes that are not the object -
  mkdir -p "$SCRATCH/bin-forge-object"
  cat > "$SCRATCH/bin-forge-object/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then exit 0; fi
  if [ "\$arg" = ls-tree ]; then "$REALGIT" "\$@"; printf '$EXTRA\0'; exit 0; fi
  if [ "\$arg" = cat-file ]; then printf 'forged-object-bytes'; exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-forge-object/git"

  fan () { bb "$HERE/fan_check.clj" "$SCRATCH/repo" "$SCRATCH/gen/manifest-$N.edn" \
             "$SCRATCH/gen/canonical-$N" "$BASE" 2>&1; }

  # --- control: real git, no shim, must catch the extra -----------------------------
  OUT=$(fan); RC=$?
  C1=$(printf '%s\n' "$OUT" | grep '^CHECK 1 ' | head -1)
  echo "SELFTEST-SHIMMED-BASELINE control: $C1"
  if [ $RC -ne 0 ] && printf '%s' "$C1" | grep -q 'FAIL' && printf '%s' "$C1" | grep -q 'extras=1'; then
    ok "control real-git" "CHECK 1 correctly catches the untracked extra -- $C1"
  else
    bad "control real-git" "expected CHECK 1 FAIL extras=1, got rc=$RC: $C1"
  fi

  # --- route 1: the shim is on PATH only (the reviewer's exact attack) ---------------
  OUT=$(PATH="$SCRATCH/bin-bless-extra:$PATH" fan); RC=$?
  echo "SELFTEST-SHIMMED-BASELINE path-shimmed: rc=$RC"
  printf '%s\n' "$OUT" | sed 's/^/    /'
  if [ $RC -ne 0 ] && ! printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "path-shimmed fail-closed" "rc=$RC, no CHECK 1 PASS -- a PATH shim never becomes the scorer's git"
  else
    bad "path-shimmed fail-closed" "rc=$RC -- a PATH-resolved git let a coordinated shim bless the extra"
  fi

  # --- route 2: the adversary's binary IS the scorer's git --------------------------
  OUT=$(PATH="$SCRATCH/bin-bless-extra:$PATH" FAN_GIT="$SCRATCH/bin-bless-extra/git" fan); RC=$?
  echo "SELFTEST-SHIMMED-BASELINE fan-git-shimmed: rc=$RC"
  printf '%s\n' "$OUT" | sed 's/^/    /'
  if [ $RC -ne 0 ] \
     && printf '%s' "$OUT" | grep -q 'ERROR base-inventory mismatch' \
     && ! printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "fan-git-shimmed fail-closed" "rc=$RC, named base-inventory mismatch, no false PASS"
  else
    bad "fan-git-shimmed fail-closed" "rc=$RC -- want a 'ERROR base-inventory mismatch' line and no PASS line"
  fi

  # --- route 3: the shim also forges cat-file -> content addressing must refuse ------
  OUT=$(PATH="$SCRATCH/bin-forge-object:$PATH" FAN_GIT="$SCRATCH/bin-forge-object/git" fan); RC=$?
  echo "SELFTEST-SHIMMED-BASELINE forged-object: rc=$RC"
  printf '%s\n' "$OUT" | sed 's/^/    /'
  if [ $RC -ne 0 ] \
     && printf '%s' "$OUT" | grep -q 'ERROR base-inventory object' \
     && ! printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: PASS'; then
    ok "forged-object fail-closed" "rc=$RC, named base-inventory object, no false PASS"
  else
    bad "forged-object fail-closed" "rc=$RC -- want a 'ERROR base-inventory object' line and no PASS line"
  fi

  # --- the same through the full six-check gate, both routes ------------------------
  RSOUT=$(PATH="$SCRATCH/bin-bless-extra:$PATH" FAN_FIXTURES="$SCRATCH/gen" FAN_BASE="$BASE" \
    bash "$HERE/rescore-FAN.sh" "$SCRATCH/repo" "$N" 2>&1); RSRC=$?
  echo "SELFTEST-SHIMMED-BASELINE full-gate path-shimmed: rc=$RSRC"
  printf '%s\n' "$RSOUT" | sed 's/^/    /'
  if [ $RSRC -ne 0 ] && ! printf '%s' "$RSOUT" | grep -q '6/6 checks passed'; then
    ok "full-gate path-shimmed" "rescore-FAN did not report 6/6 with the coordinated shim on PATH"
  else
    bad "full-gate path-shimmed" "rescore-FAN reported 6/6 (or rc=0) -- the coordinated shim reached a false GREEN"
  fi

  RSOUT=$(PATH="$SCRATCH/bin-bless-extra:$PATH" FAN_GIT="$SCRATCH/bin-bless-extra/git" \
    FAN_FIXTURES="$SCRATCH/gen" FAN_BASE="$BASE" \
    bash "$HERE/rescore-FAN.sh" "$SCRATCH/repo" "$N" 2>&1); RSRC=$?
  echo "SELFTEST-SHIMMED-BASELINE full-gate fan-git-shimmed: rc=$RSRC"
  printf '%s\n' "$RSOUT" | sed 's/^/    /'
  if [ $RSRC -ne 0 ] && ! printf '%s' "$RSOUT" | grep -q '6/6 checks passed'; then
    ok "full-gate fan-git-shimmed" "rescore-FAN did not report 6/6 with the adversary's binary as its git"
  else
    bad "full-gate fan-git-shimmed" "rescore-FAN reported 6/6 (or rc=0) -- the coordinated shim reached a false GREEN"
  fi

  echo "sabotage-FAN --selftest-shimmed-baseline: $BPASS passed, $BFAIL failed"
  [ $BFAIL -eq 0 ]
  exit $?
fi

# --- self-test: directory ENTRIES the walk cannot classify (Sol round-3 review, ---
# finding 3, BLOCKER) --------------------------------------------------------------
#   sabotage-FAN.sh --selftest-symlink-entries [N] [seed] [scratch-dir]
#
# The round-3 walk recorded only `.isFile` paths and recursed on `.isDirectory` --
# both of which FOLLOW symlinks -- and silently ignored everything else.  Git does
# neither: it inventories a symlink as ONE leaf path (mode 120000) and never looks
# through it.  So a symlink to an EMPTY directory, a dangling symlink and a FIFO all
# vanished from the walk entirely, and with a successful-but-empty `ls-files` the
# complete gate reported 6/6; a symlink to a NONEMPTY directory was worse than
# missing -- the walk reported the path THROUGH the link, which is a path git can
# never list.
#
# Each sub-case gets its own repo, so each is a single-defect witness:
#   A linked-empty     symlink -> empty dir      (walk saw nothing at all)
#   B oddity           FIFO                      (neither isFile nor isDirectory)
#   C linked-file.clj  symlink -> regular file   (walk followed it and called it a file)
#   D linked-dangling  symlink -> nowhere        (walk saw nothing at all)
#   E linked-dir       symlink -> nonempty dir   (walk descended THROUGH the link)
# The walk must inventory every one of them as exactly one classified entry -- a
# symlink leaf, or a named error -- never a silent skip and never a path through a
# link.
if [ "${1:-}" = "--selftest-symlink-entries" ]; then
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fanout-r4-fx/selftest-symlink-entries}
  print_selftest_roster
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  SPASS=0; SFAIL=0
  ok()  { SPASS=$((SPASS+1)); echo "SELFTEST-SYMLINK-ENTRIES $1: PASS $2"; }
  bad() { SFAIL=$((SFAIL+1)); echo "SELFTEST-SYMLINK-ENTRIES $1: FAIL $2"; }
  REALGIT=${FAN_REAL_GIT:-/usr/bin/git}

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  # the external targets the links point at -- OUTSIDE the repo, so nothing the walk
  # finds through a link could ever be a path git can list.
  mkdir -p "$SCRATCH/ext-empty" "$SCRATCH/ext-nonempty"
  printf '(ns through.link)\n' > "$SCRATCH/ext-nonempty/through-link.clj"
  printf '(ns ext.file)\n'     > "$SCRATCH/ext-file.clj"

  # a shim whose `ls-files` succeeds and reports NOTHING: the stated threat model, and
  # the only way to isolate the walk from git's own (correct) untracked listing.
  mkdir -p "$SCRATCH/bin-empty"
  cat > "$SCRATCH/bin-empty/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-empty/git"

  mk_repo () {   # mk_repo <dir> -> echoes the base sha
    rm -rf "$1"; mkdir -p "$1"; cp -r "$SCRATCH/gen/repo-$N/." "$1/"
    ( cd "$1" && "$REALGIT" init -q . \
        && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil add -A . \
        && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" ) >/dev/null
    cp -r "$SCRATCH/gen/canonical-$N/src/." "$1/src/"
    "$REALGIT" -C "$1" rev-parse HEAD
  }

  probe () { bb "$HERE/fan_check.clj" --probe-walk "$1" 2>&1; }
  gate  () { PATH="$SCRATCH/bin-empty:$PATH" FAN_GIT="$SCRATCH/bin-empty/git" \
               bb "$HERE/fan_check.clj" "$1" "$SCRATCH/gen/manifest-$N.edn" \
                  "$SCRATCH/gen/canonical-$N" "$2" 2>&1; }

  # want_probe <case> <grep-pattern> <human>
  want_probe () {
    if printf '%s\n' "$PROBE" | grep -qF -- "$2"; then
      ok "$1 probe" "$3"
    else
      bad "$1 probe" "want a WALK-PROBE line containing '$2' ($3); got: $(printf '%s' "$PROBE" | tr '\n' '|' | cut -c1-300)"
    fi
  }
  # deny_probe <case> <grep-pattern> <human>
  deny_probe () {
    if printf '%s\n' "$PROBE" | grep -qF -- "$2"; then
      bad "$1 probe-deny" "a WALK-PROBE line still contains '$2' ($3)"
    else
      ok "$1 probe-deny" "$3"
    fi
  }
  # want_closed <case> <grep-pattern> <human>
  want_closed () {
    if [ "$RC" -ne 0 ] && printf '%s' "$OUT" | grep -q -- "$2" \
       && ! printf '%s' "$OUT" | grep -q 'CHECK 1 file-set: PASS'; then
      ok "$1 fail-closed" "rc=$RC, $3"
    else
      bad "$1 fail-closed" "rc=$RC -- want nonzero rc, a line matching '$2' ($3), and no CHECK 1 PASS line; got: $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
    fi
  }

  # ---- A: symlink to an EMPTY directory --------------------------------------------
  RA="$SCRATCH/a"; BA=$(mk_repo "$RA"); ln -s "$SCRATCH/ext-empty" "$RA/src/linked-empty"
  PROBE=$(probe "$RA"); OUT=$(gate "$RA" "$BA"); RC=$?
  echo "SELFTEST-SYMLINK-ENTRIES A linked-empty: rc=$RC $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
  want_probe "A linked-empty" "WALK-PROBE symlink src/linked-empty -> dir" "inventoried as one symlink leaf whose target is a directory"
  deny_probe "A linked-empty" "WALK-PROBE file src/linked-empty" "not recorded as a regular file"
  want_closed "A linked-empty" 'src/linked-empty' "the gate names src/linked-empty"

  # ---- B: a FIFO (an entry that is neither file, directory nor symlink) -------------
  RB="$SCRATCH/b"; BB=$(mk_repo "$RB"); mkfifo "$RB/src/oddity"
  PROBE=$(probe "$RB"); OUT=$(gate "$RB" "$BB"); RC=$?
  echo "SELFTEST-SYMLINK-ENTRIES B fifo: rc=$RC $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
  want_probe "B fifo" "WALK-PROBE error :unclassifiable-entry src/oddity" "an entry the walk cannot classify is a named error"
  want_closed "B fifo" 'unclassifiable-entry' "the gate refuses on the unclassifiable entry"

  # ---- C: symlink to a regular file ------------------------------------------------
  RC_DIR="$SCRATCH/c"; BC=$(mk_repo "$RC_DIR"); ln -s "$SCRATCH/ext-file.clj" "$RC_DIR/src/linked-file.clj"
  PROBE=$(probe "$RC_DIR"); OUT=$(gate "$RC_DIR" "$BC"); RC=$?
  echo "SELFTEST-SYMLINK-ENTRIES C linked-file: rc=$RC $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
  want_probe "C linked-file" "WALK-PROBE symlink src/linked-file.clj -> file" "inventoried as a symlink leaf, exactly as git lists it"
  deny_probe "C linked-file" "WALK-PROBE file src/linked-file.clj" "not silently promoted to a regular file by following the link"
  want_closed "C linked-file" 'src/linked-file.clj' "the gate names src/linked-file.clj"

  # ---- D: a DANGLING symlink -------------------------------------------------------
  RD="$SCRATCH/d"; BD=$(mk_repo "$RD"); ln -s "$SCRATCH/nowhere-at-all" "$RD/src/linked-dangling"
  PROBE=$(probe "$RD"); OUT=$(gate "$RD" "$BD"); RC=$?
  echo "SELFTEST-SYMLINK-ENTRIES D dangling: rc=$RC $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
  want_probe "D dangling" "WALK-PROBE symlink src/linked-dangling -> dangling" "a dangling link is still one inventoried entry"
  want_closed "D dangling" 'src/linked-dangling' "the gate names src/linked-dangling"

  # ---- E: symlink to a NONEMPTY directory -- the walk must not descend --------------
  RE="$SCRATCH/e"; BE=$(mk_repo "$RE"); ln -s "$SCRATCH/ext-nonempty" "$RE/src/linked-dir"
  PROBE=$(probe "$RE"); OUT=$(gate "$RE" "$BE"); RC=$?
  echo "SELFTEST-SYMLINK-ENTRIES E linked-dir: rc=$RC $(printf '%s' "$OUT" | grep '^CHECK 1' | head -1)"
  want_probe "E linked-dir" "WALK-PROBE symlink src/linked-dir -> dir" "inventoried as one symlink leaf"
  deny_probe "E linked-dir" "src/linked-dir/through-link.clj" "the walk never descends through a link"
  want_closed "E linked-dir" 'src/linked-dir' "the gate names src/linked-dir"
  if printf '%s' "$OUT" | grep -q 'src/linked-dir/through-link.clj'; then
    bad "E linked-dir no-follow" "the gate reported a path THROUGH the link -- a path git can never list"
  else
    ok "E linked-dir no-follow" "the gate reported the link itself, not a path through it"
  fi

  echo "sabotage-FAN --selftest-symlink-entries: $SPASS passed, $SFAIL failed"
  [ $SFAIL -eq 0 ]
  exit $?
fi

# --- self-test: a directory that is READABLE but NOT SEARCHABLE -------------------
# (Sol round-3 review, finding 4, BLOCKER) ----------------------------------------
#   sabotage-FAN.sh --selftest-unsearchable-dir [N] [seed] [scratch-dir]
#
# The round-3 walk inferred pruning from `dirs-found` vs `dirs-entered`, i.e. from
# `.listFiles` returning null.  At mode 0400 `.listFiles` returns the NAMES (opendir
# needs r), but every child stat fails (that needs x), so `.isFile`/`.isDirectory`
# were both false and each child was dropped with the two counters still equal and
# `:pruned` empty.  Counting directories is not a completeness proof: every NAME a
# successful listing returns must become exactly one classified entry or a named
# error, and a directory that cannot be entered is an error at ANY mode.
#
# Modes covered: 0400 (readable, not searchable -- the reviewer's false 6/6), 0200
# (searchable-ish but not readable), 0000, and a 0400 directory ONE LEVEL UP so the
# whole subtree below it is unreachable.  Every mode is restored on every exit path.
if [ "${1:-}" = "--selftest-unsearchable-dir" ]; then
  N=${2:-21}; SEED=${3:-7}
  SCRATCH=${4:-/home/forge/tmp/fanout-r4-fx/selftest-unsearchable-dir}
  print_selftest_roster
  chmod -R u+rwX "$SCRATCH" 2>/dev/null || true
  rm -rf "$SCRATCH"; mkdir -p "$SCRATCH"
  UPASS=0; UFAIL=0
  ok()  { UPASS=$((UPASS+1)); echo "SELFTEST-UNSEARCHABLE-DIR $1: PASS $2"; }
  bad() { UFAIL=$((UFAIL+1)); echo "SELFTEST-UNSEARCHABLE-DIR $1: FAIL $2"; }
  REALGIT=${FAN_REAL_GIT:-/usr/bin/git}

  bb "$HERE/gen-fanout.clj" --n "$N" --seed "$SEED" --k 6 --out "$SCRATCH/gen" \
    > "$SCRATCH/gen.log" 2>&1

  mkdir -p "$SCRATCH/bin-empty"
  cat > "$SCRATCH/bin-empty/git" <<SHIMEOF
#!/usr/bin/env bash
for arg in "\$@"; do
  if [ "\$arg" = ls-files ]; then exit 0; fi
done
exec "$REALGIT" "\$@"
SHIMEOF
  chmod +x "$SCRATCH/bin-empty/git"

  restore_all () { chmod -R u+rwX "$SCRATCH" 2>/dev/null || true; }
  trap restore_all EXIT

  mk_repo () {   # mk_repo <dir> -> echoes the base sha; plants src/probe/outer/blocked/
    rm -rf "$1"; mkdir -p "$1"; cp -r "$SCRATCH/gen/repo-$N/." "$1/"
    ( cd "$1" && "$REALGIT" init -q . \
        && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil add -A . \
        && "$REALGIT" -c user.name=fanout -c user.email=fanout@anvil commit -q -m "fanout base repo-$N (pre-migration)" ) >/dev/null
    cp -r "$SCRATCH/gen/canonical-$N/src/." "$1/src/"
    mkdir -p "$1/src/probe/outer/blocked"
    printf '(ns probe.outer.blocked.residue)\n(require (quote acid.fanout.store))\n' \
      > "$1/src/probe/outer/blocked/residue.clj"
    "$REALGIT" -C "$1" rev-parse HEAD
  }

  # case <label> <dir-to-chmod-rel> <mode> <path the error must name>
  run_case () {
    local label=$1 reld=$2 mode=$3 want=$4 dir="$5"
    local base; base=$(mk_repo "$dir")
    chmod "$mode" "$dir/$reld"
    local probe out rc
    probe=$(bb "$HERE/fan_check.clj" --probe-walk "$dir" 2>&1)
    out=$(PATH="$SCRATCH/bin-empty:$PATH" FAN_GIT="$SCRATCH/bin-empty/git" \
            bb "$HERE/fan_check.clj" "$dir" "$SCRATCH/gen/manifest-$N.edn" \
               "$SCRATCH/gen/canonical-$N" "$base" 2>&1); rc=$?
    chmod 755 "$dir/$reld"
    echo "SELFTEST-UNSEARCHABLE-DIR $label (mode $mode): rc=$rc $(printf '%s' "$out" | grep '^CHECK 1' | head -1)"
    if printf '%s\n' "$probe" | grep -qF "WALK-PROBE error :unenterable-dir $want"; then
      ok "$label probe" "the walk names $want as an unenterable directory at mode $mode"
    else
      bad "$label probe" "want 'WALK-PROBE error :unenterable-dir $want'; got: $(printf '%s' "$probe" | grep -E 'WALK-PROBE (error|dirs)' | tr '\n' '|' | cut -c1-300)"
    fi
    if [ $rc -ne 0 ] \
       && printf '%s' "$out" | grep -q 'unenterable-dir' \
       && printf '%s' "$out" | grep -qF "$want" \
       && ! printf '%s' "$out" | grep -q 'CHECK 1 file-set: PASS'; then
      ok "$label fail-closed" "rc=$rc, typed unenterable-dir error naming $want, no false PASS"
    else
      bad "$label fail-closed" "rc=$rc -- want a typed 'unenterable-dir' error naming $want and no CHECK 1 PASS line"
    fi
    printf 'SELFTEST-UNSEARCHABLE-DIR %s restored-mode=%s path=%s\n' \
      "$label" "$(stat -c '%a' "$dir/$reld")" "$dir/$reld"
  }

  run_case "0400-readable-not-searchable" "src/probe/outer/blocked" 400 "src/probe/outer/blocked" "$SCRATCH/r400"
  run_case "0200-not-readable"            "src/probe/outer/blocked" 200 "src/probe/outer/blocked" "$SCRATCH/r200"
  run_case "0000-no-access"               "src/probe/outer/blocked" 000 "src/probe/outer/blocked" "$SCRATCH/r000"
  run_case "0400-one-level-up"            "src/probe"               400 "src/probe"               "$SCRATCH/rup"

  restore_all
  trap - EXIT

  echo "sabotage-FAN --selftest-unsearchable-dir: $UPASS passed, $UFAIL failed"
  [ $UFAIL -eq 0 ]
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
