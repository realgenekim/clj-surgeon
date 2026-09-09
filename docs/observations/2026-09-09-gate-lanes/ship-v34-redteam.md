# Red-team: ship v3.4's `fence_release` (staged, NOT installed)

**VERDICT: INSTALL-WITH-FIX** — four fixes below, all in `ship`, all small. Nothing found is a
reason to keep v3.3 (v3.3's three field defects are worse than anything here), but F1 is a
*regression of a named 2026-09-08 ratchet* and should not reach `~/bin` unrepaired.

Everything below was run in a scratch copy at `/var/tmp/forge/v34-redteam/` against a `cp -r` of
the staged set (`/var/tmp/forge/v34-redteam/ship/ship`), with `SHIP_FENCE_DIR`/`GIT_REPO`/`SHIP_ROOT`
pointed at scratch worktrees. The real `/home/forge/src/clj-surgeon-fence` was never touched; `~/bin`
was never edited; no `pgrep -f`/`pkill`; no ports.

---

## Findings, ranked

### F1 (HIGH) — `fence_release` destroys every unarchived byte in the borrowed tree except the verdict, silently

`fence_release` guards **one** file (`VERDICT_SRC` vs `VERDICT_ARCHIVE`) and then runs
`git reset -q --hard && git clean -qfd`. That sweep destroys **every** untracked or modified file in
the shared worktree — with no copy, no list, and no journal line naming what went.

This is not hypothetical litter. `sol-yolo` writes, *into the fence worktree*, next to the verdict:

* `docs/observations/<name>.md.last.md` — codex's final message (`sol_turn "$OUT.last.md"`, sol-yolo:78)
* `docs/observations/<name>.md.codex.log` — the whole turn log (`TURNLOG=${SOL_YOLO_TURNLOG:-$OUT.codex.log}`, sol-yolo:50)
* `docs/observations/<name>.md.followup-N.last.md` — one per follow-up turn

`fence-run`'s own 2026-09-08 ratchet exists precisely to save these: *"Every verdict this worktree
still carries is archived BEFORE anything is cleaned … and it includes `<name>.md.last.md`"*
(fence-run:14-34). v3.4 introduces a **second** cleaner of the same tree that does not honour that
ratchet — and it runs *first* (at `finish()`, before the next run's `fence-run` ever gets to archive).
The field scar this whole family comes from is a verdict that *"was recovered only from Sol's log."*
v3.4 deletes Sol's log.

It also destroys a `.clj` repro, a scratch note, or a half-finished second fix a reviewer left in
the tree, in the same silent way.

**The real tree says so right now.** `/home/forge/src/clj-surgeon-fence` (read only, nothing touched)
holds exactly the three files this finding predicts, from its last review:

```
$ git -C /home/forge/src/clj-surgeon-fence status --short
?? docs/observations/census-derived-fence.md
?? docs/observations/census-derived-fence.md.codex.log
?? docs/observations/census-derived-fence.md.last.md
```

Under v3.4 the next run's `finish()` archives the first and deletes the other two.

**Reproduction** (scratch worktree `/var/tmp/forge/v34-redteam/fence`, driving the *verbatim*
`fence_busy`/`fence_release`/`ship_exit_trap` bytes sliced out of the staged `ship` —
`sed -n '/^fence_busy() {/,/^trap ship_exit_trap EXIT/p'` — with only `event`/`utc`/`gates_cleanup`/
`run_wt_cleanup` stubbed):

```
$ printf 'GO\n> END RECEIPT (fence-run): ...\n' > $FENCE/docs/observations/rows-fence.md
$ printf 'THE ONLY COPY of the reviewer final message\n' > $FENCE/docs/observations/rows-fence.md.last.md
$ cp -p $FENCE/docs/observations/rows-fence.md run1/verdict-1.md
$ git -C $FENCE status --short
?? docs/observations/rows-fence.md
?? docs/observations/rows-fence.md.last.md
$ FENCE_DIR=$FENCE VERDICT_ARCHIVE=$PWD/run1/verdict-1.md \
  VERDICT_SRC=$FENCE/docs/observations/rows-fence.md bash -c ". harness.sh; fence_release"
== ship: fence worktree released clean (/var/tmp/forge/v34-redteam/fence) -- the next run starts on a swept tree
$ ls $FENCE/docs/observations/rows-fence.md.last.md
ls: cannot access '.../rows-fence.md.last.md': No such file or directory
$ ls fence-verdicts
ls: cannot access 'fence-verdicts': No such file or directory        # nothing was kept anywhere
```

Same run with a staged self-fix + a Sol scratch note + a repro file:

```
$ git -C $FENCE status --short
M  README.md                                   # STAGED (the v3.4 field defect)
?? docs/observations/sol-scratch-notes.md
?? docs/observations/verdict.md
?? repro.clj
$ ... fence_release
== ship: fence worktree released clean (...)
$ git -C $FENCE status --short                 # (empty)
$ ls $FENCE/docs/observations/sol-scratch-notes.md $FENCE/repro.clj
ls: cannot access '.../sol-scratch-notes.md': No such file or directory
ls: cannot access '.../repro.clj': No such file or directory
```

**Fix** — archive what the sweep is about to destroy, and *name it*, before destroying it. Insert
immediately before the `git reset -q --hard` line in `fence_release` (ship:311), and add
`doomed n f` to the function's `local` line:

```diff
--- a/ship
+++ b/ship
@@ fence_release()
-  local src stamp extra busy
+  local src stamp extra busy doomed n f
@@
     fi
   fi
+  # RED-TEAM 2026-09-09: `git clean -qfd` below destroys EVERY untracked file in the borrowed tree,
+  # not only the verdict -- including sol-yolo's own `<name>.md.last.md` and `<name>.md.codex.log`,
+  # which fence-run's 2026-09-08 ratchet exists to preserve, and any repro or note a reviewer left.
+  # A cleanup that destroys evidence it never saved is not a cleanup: copy it out, and SAY what went.
+  doomed=$( { git -C "$FENCE_DIR" ls-files --others --exclude-standard 2>/dev/null
+              git -C "$FENCE_DIR" diff --name-only 2>/dev/null
+              git -C "$FENCE_DIR" diff --name-only --cached 2>/dev/null; } | sort -u )
+  n=0
+  if [ -n "$doomed" ]; then
+    stamp=${stamp:-$(date -u +%Y%m%dT%H%M%SZ)}
+    while IFS= read -r f; do
+      [ -n "$f" ] && [ -f "$FENCE_DIR/$f" ] || continue
+      mkdir -p "$FENCE_ARCHDIR/swept-$stamp/$(dirname "$f")" 2>/dev/null
+      cp -p "$FENCE_DIR/$f" "$FENCE_ARCHDIR/swept-$stamp/$f" 2>/dev/null && n=$((n+1)) && continue
+      echo "SHIP-FENCE-NOT-RELEASED reason=cannot-copy-doomed-file file=$f (nothing is swept: the worktree holds bytes no archive has)"
+      event "FENCE not-released reason=cannot-copy-doomed-file file=$f"; return 0
+    done <<EOF
+$doomed
+EOF
+    echo "SHIP-FENCE-SWEEPING files=$n kept=$FENCE_ARCHDIR/swept-$stamp/ list='$(printf '%s' "$doomed" | tr '\n' ';' | cut -c1-300)'"
+    event "FENCE sweeping files=$n kept=$FENCE_ARCHDIR/swept-$stamp"
+  fi
   if git -C "$FENCE_DIR" reset -q --hard 2>/dev/null && git -C "$FENCE_DIR" clean -qfd 2>/dev/null; then
```

---

### F2 (MEDIUM) — the CANDIDATE spec was loosened; the check was not. An abbreviated sha is still thrown away

v3.3's spec said `<full 40-char sha of the candidate you reviewed>`. v3.4's says
`<the sealed candidate sha printed in the brief header, or the branch tip it was sealed from>` —
**no length requirement at all** — while the check is still exact string equality
(`[ "$b_cand" = "$CUR_TIP" ]`, ship:760). A reviewer that writes the 8- or 12-char form it has been
reading all night has its finished, tree-bound review thrown away for a naming convention: the exact
failure mode v3.4 was written to end.

To be fair to the builder: the *observed* field verdict did carry the full 40 chars —
`/var/tmp/forge/ship/20260909T045222Z-da4396517d50/verdict-1.md:70`
`CANDIDATE: da4396517d5069fc54f4d95938aad3c507123b54` — so v3.4 does cover its own evidence. This is
the residual, and the spec change makes it *more* likely, not less.

**Reproduction** (real `ship` bytes, real repo/fence/origin, the staged fixture's harness with one
row added; `BLOCK_CAND_OVERRIDE="${T:0:8}"`):

```
$ V3_DIR=/var/tmp/forge/v34-redteam/ship bash redteam-rows.sh
  A: short-tip(db9eba67) -> reason=fix-block-wrong-candidate
     SHIP-STOP reason=fix-block-wrong-candidate block_candidate=db9eba67
       reviewed=3d7c085a0f140094e31e8c1537f9c1a2e2daf61a
       tip=db9eba6780665a7a077efd60338c945e7fba84bd
       (the result names neither the sealed candidate nor the tip that composes it, ...)
```

**Fix** — resolve the name through git instead of string-comparing it. An ambiguous or unknown
prefix resolves to empty and falls straight through to the existing refusal, so nothing is widened:

```diff
--- a/ship
+++ b/ship
@@ autofix() -- the fix-block candidate check
-  local b_cand b_find b_repair b_prod
+  local b_cand b_find b_repair b_prod b_res
@@
+  # a reviewer that abbreviates is naming the same commit. RESOLVE the name; do not string-compare
+  # it. An ambiguous or unknown prefix resolves to nothing and falls through to the refusal below.
+  case "$b_cand" in
+    [0-9a-fA-F]*) b_res=$(git -C "$GIT_REPO" rev-parse --verify -q "${b_cand}^{commit}" 2>/dev/null) ;;
+    *) b_res="" ;;
+  esac
+  [ -n "$b_res" ] && b_cand=$b_res
   if [ "$b_cand" != "$CAND" ] && [ -n "$CUR_TIP" ] && [ "$b_cand" = "$CUR_TIP" ]; then
```

---

### F3 (MEDIUM) — the archive-exists guard is the one guard that refuses in *silence*

`fence_release`'s other two refusals print (`SHIP-FENCE-NOT-RELEASED pids=…`,
`…reason=cannot-copy-divergent-verdict`). The archive guard (ship:290) is a bare
`… || return 0`: an empty or missing archive leaves the borrowed tree dirty and says **nothing** —
on stdout or in the journal. The next run then dies on `fence-worktree-dirty` with no line anywhere
saying which run declined to sweep or why. That is the "fail closed but not loud" shape house rules
call a refusal nobody hears.

**Reproduction:**

```
$ : > run1/verdict-empty.md ; printf 'leftover\n' > $FENCE/docs/observations/leftover.md
$ FENCE_DIR=$FENCE VERDICT_ARCHIVE=$PWD/run1/verdict-empty.md \
  VERDICT_SRC=$FENCE/docs/observations/leftover.md bash -c ". harness.sh; fence_release; echo rc=\$?"
rc=0                                              # no output at all
$ git -C $FENCE status --short
?? docs/observations/leftover.md                  # left dirty, unexplained
```

**Fix:**

```diff
--- a/ship
+++ b/ship
-  [ -n "${VERDICT_ARCHIVE:-}" ] && [ "$VERDICT_ARCHIVE" != - ] && [ -s "$VERDICT_ARCHIVE" ] || return 0
+  if ! { [ -n "${VERDICT_ARCHIVE:-}" ] && [ "$VERDICT_ARCHIVE" != - ] && [ -s "$VERDICT_ARCHIVE" ]; }; then
+    if [ -n "$(git -C "$FENCE_DIR" status --short 2>/dev/null)" ]; then
+      echo "SHIP-FENCE-NOT-RELEASED reason=no-verdict-archive archive=${VERDICT_ARCHIVE:--} dir=$FENCE_DIR (this run archived nothing, so it sweeps nothing; the tree is dirty and the NEXT run will refuse on it)"
+      event "FENCE not-released reason=no-verdict-archive archive=${VERDICT_ARCHIVE:--}"
+    fi
+    return 0
+  fi
```

---

### F4 (MEDIUM) — four of the five branches of the tree-destroying function have no fixture row

`run-ship-v3.4.sh` row 8 covers exactly one path: release succeeds. Every guard is untested, and the
fixture cannot test two of them by construction, because its `fence-run` stub writes the verdict to
`$FX/verdicts/$3.md` — **outside** the fence worktree — so `VERDICT_SRC` is never a file the sweep
could destroy and `cmp` never disagrees.

| branch in `fence_release` | fixture row |
|---|---|
| released clean (happy path) | row 8 |
| `SHIP-FENCE-NOT-RELEASED pids=…` (busy) | **none** |
| `SHIP-FENCE-VERDICT-DIVERGED` (copy-first) | **none** — unreachable in the fixture |
| `…reason=cannot-copy-divergent-verdict` | **none** |
| `SHIP-FENCE-RELEASE-FAILED` | **none** |
| archive missing/empty → no sweep | **none** |

Add three rows (and move the stub's verdict *into* `$FX/fence/docs/observations/` so the last two
are reachable at all):

1. a live `( cd "$FX/fence"; sleep 30 ) &` across a stop → asserts `SHIP-FENCE-NOT-RELEASED pids=`,
   asserts the tree is **still dirty**, and asserts the litter survived;
2. the stub appends one byte to the in-tree verdict after ship archives it → asserts
   `SHIP-FENCE-VERDICT-DIVERGED` and that both copies exist;
3. the stub leaves `<name>.md.last.md` and a `repro.clj` in the tree → asserts they are recoverable
   after the release (this row is red against the staged bytes today; it is F1's witness).

---

### F5 (MEDIUM, design) — the lease does not cover the resource being reset

`LEASEDIR="$SHIP_ROOT/.lease"` (ship:528) but `FENCE_DIR=${SHIP_FENCE_DIR:-/home/forge/src/clj-surgeon-fence}`
(ship:93). The lease is *per `SHIP_ROOT`*; the fence worktree is *global*. Two ships with different
`SHIP_ROOT` — a fixture run that forgot `SHIP_FENCE_DIR`, a second seat, an operator with the env var
set — never see each other's lease and share the tree that `fence_release` resets. The only
cross-run guard is `fence_busy`, and it has two blind spots:

**(a) `fence_busy` is cwd-EXACT.** A reviewer that `cd`s into a subdirectory is invisible:

```
$ ( cd "$FENCE/src"; exec sleep 40 ) & SUB=$!
$ FENCE_DIR=$FENCE bash -c ". harness.sh; fence_busy"
                                       # empty -- pid 2617738 cwd=.../fence/src is not seen
$ ( cd "$FENCE"; exec sleep 40 ) & TOP=$!
$ FENCE_DIR=$FENCE bash -c ". harness.sh; fence_busy"
2618179                                # only the exact-cwd process is seen
```

**(b) the 90-second hole after the reviewer exits.** `fence-run`'s END-RECEIPT waiter `cd /`s
deliberately (fence-run:45-48), so once codex exits **nothing lives in the tree** — while ship's
`autofix()` is still polling that tree's verdict for the END RECEIPT for up to
`SHIP_END_RECEIPT_WAIT_S` (default **90 s**, ship:684, re-`cp`ing `$VERDICT_SRC` each loop). A
concurrent run's `fence_release` in that window sees `fence_busy` empty, sweeps, and the waiting run
stops with `reason=reviewer-unfinished` — a reason about a reviewer that in fact finished.

I did **not** stage a two-ship interleave live (out of timebox, and it needs two real reviewers);
this is read from the code plus the `fence_busy` measurements above. Treat it as CREDIBLE, NOT YET
PROVEN.

Cheapest real fix, one line — match the subtree, not the exact directory:

```diff
--- a/ship
+++ b/ship
@@ fence_busy()
-    [ "$d" = "$FENCE_DIR" ] || continue
+    case "$d" in "$FENCE_DIR"|"$FENCE_DIR"/*) ;; *) continue ;; esac
```

Proper fix (follow-up, not a blocker): an owner file inside the fence worktree's git dir carrying
`pid=<n> run=<id>`, written before the review launches and checked by `fence_release` — the same
shape as `$LEASEDIR/owner`. `fence_busy` answers "is someone standing here"; it cannot answer "does
another run still need these bytes."

---

### F6 (LOW) — `fastlane=green` about a tree that is not the landed one is legible only by comparing a 12-char prefix to a 40-char field

By design (and correctly) `fl_record` keeps the observation and tags it with `FASTLANE_CAND`. But a
SHIP line that reads `fastlane=green fastlane_elapsed=…s fastlane_candidate=abc123abc123 …
final_candidate=<40 chars>` requires the reader to prefix-match two differently-truncated fields to
notice the green is about a retired candidate. Every gating decision already reads `FL_STATE`
(ship:1368, 1471-1472), so nothing is *decided* wrongly — this is a reporting nit with a real
misreading cost on a 25-field line.

I attempted a live row (round 1 lane green, round 2 lane still running at a NO-GO stop) twice; both
attempts came back `fastlane=unknown fastlane_candidate=-` — the lane never reached a terminal
marker in my harness, so the row is **inconclusive**, not evidence either way. Reporting it as
unproven rather than dressing it up.

Suggested (optional): in `finish()`, when `[ "$FASTLANE" != unknown ] && [ "$FASTLANE_CAND" != "$CAND" ]`,
emit `fastlane=${FASTLANE}-stale` (or add `fastlane_scope=prior-candidate`). All existing assertions
are substring greps for `fastlane=green`, so `fastlane=green-stale` keeps every current row green —
verify that against the corpus before adopting.

---

## Attacks that HELD (worth recording — these guards are good)

| attack | result |
|---|---|
| verdict with **CRLF** line endings vs an LF archive | `SHIP-FENCE-VERDICT-DIVERGED … kept=.../v2.ship-exit-20260909T053933Z.md` — copied out **before** the sweep. Held. |
| trailing-newline-only difference | same path (byte `cmp`, not a text compare). Held. |
| verdict that is a **symlink out of the worktree** | `cp -p` copies the *content* to the archive; `git clean` removes the link, not the target — `outside-secret.md` intact afterwards. Held. |
| **empty** verdict archive | refuses to sweep at all (fail-safe). Held — but silently, see F3. |
| **SIGTERM** with the EXIT trap armed | trap runs: `kill -TERM` → `EXIT-TRAP RAN`, rc=143; `kill -INT` → trap runs, rc=0; `kill -KILL` → trap does **not** run, rc=137 (unavoidable; the next run now reports `fence-worktree-dirty` by name, which is v3.4's own defect-3b repair). Held. |
| SIGTERM **mid-archive** (truncated `VERDICT_ARCHIVE`) | ordering is `VERDICT_SRC=` (1269) → `FENCE_RELEASED=0` (1270) → `VERDICT_ARCHIVE=` (1271) → `cp` (1272). A 0-byte archive fails `-s` → no sweep; a partial archive fails `cmp` → divergent bytes copied out first. Held. |
| cmdline impostor: a process whose argv contains `END RECEIPT (fence-run)` sitting in the fence dir | the exclusion is real, but **not reachable through a brief**: `sol-yolo` feeds the prompt on **stdin** (`sol_turn … - < "$PF"`, sol-yolo:62/78), so no brief text ever lands in codex's argv. Held. |
| a fixture sweeping the **real** fence worktree | all five `run-ship-v*.sh` that invoke `ship` export `SHIP_FENCE_DIR="$FX/fence"`; the two fixtures that don't (`run-land-auto.sh`, `run-land-publication-truth.sh`) never invoke `ship`. Held. |
| **question 4** — a fix-block naming an *older* tip (round 1's tip after a fix round, a genuine ancestor of the sealed candidate) | **refused, and that is right.** `B: is the refused name an ancestor of the sealed candidate? YES` — and it still stops: `SHIP-STOP reason=fix-block-wrong-candidate block_candidate=30e1b63b… reviewed=718fcc1a… tip=3bf4ba93…`. An ancestor did not *compose* this candidate; accepting it would bind a result to a tree the reviewer never saw. Round 1's accepted-as-tip line still printed in the same run (`round-1 accepted-as-tip? 1`), so the widening and the refusal coexist correctly. |

---

## Scratch used (nothing outside it was written)

```
/var/tmp/forge/v34-redteam/ship/          cp -r of /var/tmp/forge/ship-v3.4 (bytes under test)
/var/tmp/forge/v34-redteam/fence-funcs.sh verbatim fence_busy/fence_release/ship_exit_trap slice
/var/tmp/forge/v34-redteam/harness.sh     stubs event/utc/gates_cleanup/run_wt_cleanup, sources the slice
/var/tmp/forge/v34-redteam/repo, fence    scratch git repo + scratch fence worktree
/var/tmp/forge/v34-redteam/redteam-rows.sh  staged fixture header (lines 1-164) + rows A/B/C
/var/tmp/forge/ship-v34-fx.*              the fixture's own mktemp scratch (self-removing)
```

## Install order

1. F1 (evidence destruction) and F3 (silent refusal) — both inside `fence_release`.
2. F2 (one `rev-parse`) and F5(a) (one `case`).
3. Re-run all eight fixtures against the patched bytes; **add F4's three rows and prove row 3 is red
   against today's staged `ship` before it goes green against the fixed one** — a guard with no red
   row is untested, and that is the whole reason F1 shipped.
4. Then `bash /var/tmp/forge/ship-v3.4/install.sh`.
