# Kernel round 4 — Opus executed re-check of bridge/txn-journal at ec93bd1 (2026-09-03T11:34Z)

Verdict: **GO-WITH-FIX**, HELD for round 5: round-three blockers 1–3 and minors 4–5 CLOSED on the reviewer's own re-runs; two NEW blockers in round 4's own code (restore-after-break is check-then-act — 129/29,012 third-party claims clobbered, 20,356 `:restored false` never surfaced; `finish-after-throw!` un-commits a committed txn when the post-release tail throws), plus four OPEN items (two contract sentences stronger than code; `publish-staged-receipt!` raw Files/move outside the table; tombstones unbounded/invisible/colliding; legacy age rests on mtime).

## Opus verdict, verbatim

# txn-journal round 4 (`ec93bd1`) — Opus executed re-review: **GO-WITH-FIX**

**Reviewer of record.** Opus, third consecutive round on this branch (Sol's content filter
refused round 2; there is no Sol arm on rounds 3 or 4). Round-three verdict under re-judgement:
`git show origin/main:docs/observations/2026-09-03-txn-journal-round3-rereview-opus.md`.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn4-wt` at `ec93bd1` (`git rev-parse --short HEAD`
=> `ec93bd1`; nothing committed, stashed or pushed; `git status --short` empty at start and end).
Fixtures under `/tmp/txn4-fx` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M <probe>` from the worktree — never `make mcp-test`,
never a live server, no port in 7888–7895 or 7906 contacted. Cross-process lock contention uses
the real JVM holder `clj-surgeon.txn-lock-child` (a bash `flock` does not block
`FileChannel/lock`). `make memory-battery` / `make memory-red` were NOT run (exclusive lock,
another lane). Every process signalled was one I started.

**Headline.** All three round-three blockers are CLOSED against my own injections, and both
round-three minors that were in scope for this build (4 legacy LOCK, 5 the `O(1)` honesty claim)
are CLOSED, as is finding 7 (the writer table). Round 4's own additions survive attacks (b), (c)
and (e) cleanly. Two new findings come out of the additions themselves: the two-rename break's
**restore is a check-then-act at the syscall level and can silently clobber a third acquirer**
(strace-proven, 129/29,012 measured), and `finish-after-throw!` **has no terminal-status guard,
so a throw in `finish!`'s post-release tail reverts a transaction the journal already recorded as
committed**. Neither is reachable by any verb today (`with-cooperating-writes` still has zero call
sites in `src/`), which is what keeps this a GO-WITH-FIX rather than a NO-GO.

---

## Executed gates (mine, at `ec93bd1`, each once under `/home/forge/bin/suite-run`)

```
$ /home/forge/bin/suite-run bash -c 'cd /home/forge/tmp/sol/txn4-wt && bb test/run_all.clj'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.
EXIT=0

$ /home/forge/bin/suite-run bash -c '... swipl -q -f test/mcp_operation_contract_oracle.pl ...'
ORACLE_EXIT=0
Ran 441 tests containing 4339 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
kernel warning check: 2 namespace(s), 0 warning(s)
KWC_EXIT=0
```

All four reproduce the builder's counts exactly: test-fast 720/5976/0, mcp-test 441/4339/0,
oracle pass, kernel warning check 0.

---

## Round-three blockers, re-judged

### Blocker 1 — a cooperating write from a SECOND THREAD threw `OverlappingFileLockException` out of `commit!`, stranding the project LOCK behind a live pid — **CLOSED**

`src/clj_surgeon/file_ops.clj:44` (the process-wide `publish-monitors` table),
`:47-53` (`publish-monitor`, keyed by `(.getCanonicalPath file)`), `:88` (`.lock monitor` taken
BEFORE the channel), `:100` (`.lock channel`), `:104` (`.unlock monitor` in `finally`).
`src/clj_surgeon/txn_journal.clj:1448-1450` (`catch Throwable` → `finish-after-throw!` → re-raise).

**Premise re-established, mine:** a second thread in one JVM asking for a lock the JVM holds is
thrown, not made to wait —

```
PREMISE second-thread .lock on same file, same JVM => OverlappingFileLockException
```

**Witness (probe B1), my own sibling thread inside the lock while `commit!` runs:**

```
B1 commit! => {:ok true, :committed true, :files-written 1}
B1 elapsed-ms => 1443 (>=1400 means it WAITED for the sibling)
B1 bytes => "NEW\n"
B1 LOCK present after => false
B1 state.edn => :committed
B1 next begin! => {:txid 1788434513684-02f99a44}
```

Round three's shape here was `{:THREW "java.nio.channels.OverlappingFileLockException"}`,
`state.edn` `:sealed`, `LOCK` present naming `"X1"`, next `begin!` refused
`:txn-lock-held :holder-live true`. It now **waits 1,443 ms** for the sibling and commits.

**Witness (probe B1b), a throw injected INSIDE the window:**

```
B1b commit! => {:THREW java.nio.channels.OverlappingFileLockException}
B1b bytes => "H0\n"  (H0 = rolled back)
B1b LOCK present => false
```

The throw escapes as a throw (not a false receipt), the tree is back at H0, and the project LOCK
is released. Both halves of the blocker are gone.

### Blocker 2 — a `future` spawned inside the publish lock INHERITED the re-entrancy binding and took no lock at all — **CLOSED**

`file-ops.clj:90` — `(> (.getHoldCount monitor) 1)`; `*publish-lock-held*` is deleted from the
namespace.

**Witness (probe B2), with a SEPARATE JVM holding `PUBLISH.lock` for 2,500 ms:**

```
B2a var still exists? => false
B2b future result => {:future-ms 2495}  (>=2000ms means it WAITED for the other JVM)
B2b bytes => "FUTURE\n"
B2c => {:this-thread-reentrant? :nested-ok, :future-sees {:v :BLOCKED-800ms, :ms 800}}
```

Round three measured 1 ms and a landed write against a lock another process held. The future now
waits 2,495 ms for the other JVM; a future spawned inside this thread's own hold is BLOCKED at an
800 ms deadline rather than sailing through; same-thread nesting still works.

### Blocker 3 — breaking a stale lock was a read-then-unconditional-delete, so a breaker could delete a LIVE holder's freshly created LOCK — **CLOSED**

`txn_journal.clj:495-540` (`break-lock!`, rename-to-tombstone + content/SHA-256/fileKey recheck +
restore), `:481-493` (`release-lock!`, txid-scoped, no unguarded arity), `:589` (break at most
once per acquisition), `:647-658`/`:590` (the `:before-break` injection point).

**Witness (probe B3), `:before-break` putting a LIVE holder's brand-new claim in the way — exactly
round three's probe K, driven through the public API this time:**

```
B3 pre-break stale-holder => :process-not-alive
B3 begin! => {:error-type :txn-lock-held, :holder-txid A-LIVE, :holder-live true, :holder-cause nil}
B3 LOCK now names => A-LIVE  (must still be A-LIVE)
B3 tombstone for breaker present? => false
B3 dir listing => (LOCK)
B3 release-lock! as 'someone-else' => false  LOCK still there? => true
B3 release-lock! as 'A-LIVE'       => true  LOCK still there? => false
```

Round three's shape was `after B's release-lock!, A's LOCK still exists? => false` and `LOCK` then
naming `"B-ALSO-LIVE"` — two live holders. The break now restores A-LIVE's claim untouched, leaves
no tombstone, and the breaker is refused. The guarded release also holds: a different txid's
`finish!` cannot unlink this holder's claim.

---

## Round-three minors, re-judged

### Minor 4 — a legacy pid-only LOCK is unbreakable for ever on a reused pid — **CLOSED**

`txn_journal.clj:377-402` (`stale-holder`, `:legacy-format` at `:397`), `:405-415`
(`legacy-lock-break-age-ms` = 3,600,000), `:417-430` (`legacy-lock-dead?`, the two-part receipt,
`:now-ms` seam at `:428`), `:431-437` (`breakable-causes` excludes `:legacy-format`),
`:597-620` (the typed refusal and its remedy), `:1845-1849` (recovery's legacy gate).

**Witness (probe L4), round three's probe B4 — `{:txid "old-format" :pid <LIVE pid>}`:**

```
L4 begin! on a LIVE-pid legacy LOCK => {:error-type :txn-lock-held, :holder-live false, :holder-cause :legacy-format, :holder-format :pid-only, :lock-format-expected 2}
L4 remedy => "This LOCK predates the checkable holder triple, so it is unreadable rather than unbreakable. Run recovery with :break-legacy-lock true; it breaks the claim only on a receipt of the holder's death - the recorded pid naming no live process AND the lock older than 3600000 ms."
L4 next_call => {:op :txn/recover, :workspace_root nil, :break_legacy_lock true}
L4 recover! WITHOUT the flag => {:ok true, :transactions-recovered 0}  LOCK still there = true
L4 recover! WITH flag, holder ALIVE => nil  LOCK still there = true
L4 recover! WITH flag, dead pid but FRESH lock => nil  LOCK still there = true
L4 recover! WITH flag + now-ms past the ceiling => {:reason :stale-holder, :cause :legacy-format, :pid 1586743, :holder-txid old-format, :tombstone LOCK.broken.recover-1788434924595-0cdf16ae}  LOCK still there = false
```

Round three's `{:holder-cause nil, :holder-live true}` with no remedy is replaced by a named
cause, a named format, and a remedy with an executable `next_call`. Both halves of the receipt are
enforced independently (live holder refused; dead pid but fresh lock refused), and the break is
the same compare-and-break verb. `:holder-live false` is now correct where round three said `true`.

### Minor 5 — `O(1) in the target's size` asserted where the build's own numbers say 1.9x — **CLOSED**

`txn_journal.clj:11-12`, `:51-72` (the docstring now leads "THE SIZE TERM IS REDUCED, NOT
ELIMINATED" and carries both measured medians), `:82-87` (`:size-term` in the contract VALUE),
`:1255-1256` (`publish-one!`), `docs/txn-journal.md:39-42`, `:437-452`, and
`docs/intent/memory/memory-transaction-specs.md` MEM-007 plus the new forbidden-claim row at `:78`.

**Witness:** `git diff eb22036..ec93bd1 -- src/clj_surgeon/txn_journal.clj` removes
`bound is O(1) in the target's size`, `The bound is O(1) in the target's size and it did not use
to be.` and `so the window is O(1) in the target's size`, and replaces them with the measured
form. All three places round three named are corrected, and the claim now appears in the receipt
value itself. The one surviving `O(1)` (`:217`, "What makes the pre-image recheck O(1)") is about
the STAT COMPARISON, which genuinely is O(1); it is not the claim that was wrong.

### Finding 7 — the non-cooperating-writer table was incomplete — **CLOSED**

`docs/txn-journal.md:82-97`. I re-derived the table rather than trusting it:

```
$ grep -rn 'atomic-write!\|atomic-create!\|atomic-publish!' src/ | grep -v file_ops.clj | grep -v txn_journal.clj
```

24 call sites; every one appears in the table, and I read each cited line to confirm it is the
call it claims (`extract.clj:242` receipt write, `intent_transaction.clj:2187` `:create-source!
atomic-create!`, `worktree_lifecycle_io.clj:399` `(defn- atomic-write!` called at `:438`
`(atomic-write! path (lifecycle/canonical-edn value) create-only)`). The three sites round three
named as missing are the three the doc now adds, and the private duplicate is annotated
"**Binding `*publish-lock-dir*` cannot reach it**". See new finding 4 for the one publication
primitive the sweep still leaves out.

### Findings 6, 8, 9, 10 — **OPEN, as scoped**

Unchanged, and correctly so: `:ctime-ns` still degrades to nil at `txn_journal.clj:235-238` while
`:stat-fields` at `:80` advertises it unconditionally (6); `release-receipt!` still reads the
lease with `read-edn-file` (8); `:lease :unreadable` at `:1619`/`:1711` still covers a
never-written lease (9); the `undo!`-of-a-deleted-target remedy at `:1682` still has no verb
behind it (10). These ride the adoption build.

---

## Round 4's own additions, attacked

### (a) the two-rename break, a third acquirer between rename-away and restore — **a real, narrow defect, and one false docstring claim**

`txn_journal.clj:520-524` (rename LOCK → `LOCK.broken.<txid>`, ATOMIC_MOVE + REPLACE_EXISTING),
`:526-530` (content + SHA-256 + fileKey recheck), `:531-537` (the restore).
The docstring at `:511-513` states the guarantee under test:

> "the moved file is put straight back - `Files/move` with no REPLACE_EXISTING refuses if a LOCK
> has appeared meanwhile"

**Witness 1 — the syscall trace. That guarantee is a check-then-act, not an atomic operation:**

```
$ strace -f -e trace=...,rename,renameat,renameat2 clojure -M /tmp/txn4-fx/mv.clj
statx(AT_FDCWD, "/tmp/txn4-fx/mv/b", ...) = -1 ENOENT (No such file or directory)
rename("/tmp/txn4-fx/mv/a", "/tmp/txn4-fx/mv/b") = 0
```

The JDK stats the target, then calls `rename(2)` — and POSIX `rename` replaces unconditionally.
Anything that creates `LOCK` between those two syscalls is overwritten silently.

**Witness 2 — measured, probe A2. A hammer thread creates a live third-party claim whenever `LOCK`
is absent and watches its own inode; `break-lock!` is called 30,000 times with a deliberately
mismatching claim so the restore branch always runs:**

```
A2 third-acquirer claims created = 29012
A2 third-acquirer claims SILENTLY CLOBBERED by the restore = 129
A2 break-lock! outcomes: broke= 0  lock-vanished= 0  holder-changed+RESTORED-FALSE= 20356  other= 0
A2 tombstones left in dir = 20356
```

Two distinct bad outcomes, both reachable:

1. **129 of 29,012 (~0.4%)** third-party claims were destroyed by the restore's own rename. If
   that claim belonged to a live holder, the workspace is back to round-three blocker 3's end
   state — two live holders — reached through the fix instead of the defect.
2. **`:restored false`.** When the restore is refused, the claim that was renamed away is NOT put
   back: `LOCK` names the third acquirer and the renamed-away claim survives only inside the
   tombstone. Its owner's `finish!` will call the txid-guarded `release-lock!`, find a LOCK that
   does not name it, return `false`, and never learn it lost the lock.

**Is any state lost, is the tombstone always left?** In outcome 2 the tombstone always holds the
displaced claim, so nothing is *destroyed* — but nothing *reads* it either. `acquire-lock!` at
`:589-596` and `recover!` at `:1849-1854` both keep only `(:broken outcome)`; `:cause
:holder-changed` and `:restored false` are discarded, and the caller's refusal names the third
acquirer with no hint that a claim was displaced. That is a silent refusal with no owner —
house rule 17. In outcome 1 the displaced file IS destroyed and there is no tombstone for it.

**Severity.** Both need a double race: the LOCK must change between `read-lock-claim` and the
first rename AND a third party must land in the microsecond restore gap. Far narrower than round
three's unbounded read-judge-delete window, and the kernel's read-back verification
(`:txn-read-back-mismatch`) still catches the resulting concurrent write. Not a merge blocker for
a kernel no verb calls; it is a blocker for adoption, and the docstring's guarantee should not
ship as written either way.

*Fix:* make the restore `Files/move` with `ATOMIC_MOVE` into a target proven absent by an
`O_EXCL` create of a sentinel, or better — do the restore by `Files/createLink(tomb → LOCK)`
followed by `deleteIfExists(tomb)`, which IS create-if-absent at the kernel level and is the same
primitive `write-lock!` already uses at `:557`. Then report `:restored false` as a typed,
counted event rather than dropping it.

### (b) `ReentrantLock` keyed by canonical path — two spellings of one workspace — **CLOSED, with one documented residual**

`file_ops.clj:47-53`, `:87`.

**Witness (probe B), five spellings — plain, trailing slash, `/./`, `/../<name>`, and through a
symlinked directory:**

```
B  spellings tried = 5  distinct monitors created = 1
B  monitor keys = [/tmp/txn4-fx/b-alias/state/.local/state/clj-surgeon/workspaces/29028.../transactions/PUBLISH.lock]
B  two-spelling thread order = [:A-in :A-out :B-in :B-out]   errors = []
```

One monitor, and two threads reaching it by different spellings serialise with no exception.
`getCanonicalPath` is the right key because `.mkdirs` on the parent runs first (`:86`), so the
symlinks are resolved against an existing prefix.

**Residual, mine (probe B2b).** The key is the PATH, and the OS lock is on the INODE, so two
canonical paths naming one inode defeat it:

```
B-alias hardlinked lock files, two canonical paths => order [:A-in :A-out]  errors [[:B OverlappingFileLockException]]
```

Reachable only by hard-linking two workspaces' `PUBLISH.lock` or bind-mounting a transactions
directory twice — I could not construct it any other way. And the consequence is now a loud throw
that `commit!`'s `catch Throwable` converts into a rollback plus a released LOCK, not the round-3
deadlock. Worth one sentence in the docstring; not worth a code change.

Two smaller notes on the same table: `publish-monitors` (`:44`) grows without bound (one
`ReentrantLock` per canonical path for the life of the JVM — bytes, not a leak that matters), and
`(.getCanonicalPath file)` can throw `IOException` before the monitor is taken; inside `commit!`
that lands in `catch Throwable` and is handled.

### (c) hold-count re-entrancy — a nested commit-path write on one thread — **CLOSED**

`file_ops.clj:88-104`. The monitor is taken unconditionally at `:88` and released in a `finally`
at `:104`; the hold-count test at `:90` only decides whether the OS lock is taken, so lock and
unlock are always paired. `getHoldCount` counts the CURRENT thread's holds only, so `> 1` cannot
be answered yes by anything the thread spawned — which probe B2c confirms independently.

**Witness (probe C), a nested cooperating write driven from inside the real commit window:**

```
C  nested-in-window = :nested-ok   commit = {:ok true, :files-written 1}
C  bytes = ["OUTER-NEW\n" "NESTED\n"]
C  LOCK after = false   monitor hold count now = 0
C  post-nest cooperating write = :ok
```

Hold count back to 0, project LOCK released, and the next cooperating write still acquires. No
double release.

### (d) rollback inside `catch Throwable` — **half CLOSED, half a NEW finding**

`txn_journal.clj:1310-1328` (`finish-after-throw!`), `:1448-1450` (`commit!`'s catch),
`:1196-1230` (`finish!`; note `release-lock!` at `:1225` runs BEFORE `reclaim-staging!` and
`write-lease!` at `:1227-1229`).

**d1 — the rollback itself throws. Witness (probe D1), `rollback-written!` redefined to throw
while an `IOException` is injected inside the window:**

```
D1 commit! => {:THREW IOException, :msg boom}   (the ORIGINAL throw must escape, not the rollback's)
D1 LOCK released? => true
D1 journal marked? => state.edn {:txid "D1", ..., :status :sealed, ...}
D1 bytes on disk => "H0\n"
D1 next begin! => {:txid 1788434971599-11be36ac}
D1 recover! => {:ok true, :transactions-recovered 2}
```

Correct, and the last-resort path works: the caller's own exception escapes (not the rollback's),
the project LOCK is released, the next `begin!` succeeds. **One honesty gap:** the journal is
NOT marked on this path — `state.edn` stays `:sealed` — while `docs/txn-journal.md:129-131` says
every exception path "releases the project `LOCK` and marks the journal". `recover!` picks the
journal up, so the outcome is safe; the sentence is one clause too strong.

**d2 — a throw AFTER `finish!` has published, journalled and released. NEW FINDING. Witness (probe
D2), `write-lease!` redefined to raise `IOException "ENOSPC writing lease.edn"` — the exact fault a
full disk produces in `finish!`'s post-release tail:**

```
D2 commit! => {:THREW IOException, :msg ENOSPC writing lease.edn}
D2 bytes on disk => "H0\n"
D2 journal dir still there? => true
D2 state.edn => {:txid "D2", ..., :status :committed, :finished-at "2026-09-03T11:29:31.620304983Z", :retained true, :restore-failed false}
D2 journal.log tail => "committed"
D2 LOCK => false
```

`finish-after-throw!` has **no terminal-status guard**. The staged bytes were published, the
journal appended `committed`, `state.edn` was written `:committed`, and the project LOCK was
released — and then the catch reverted every written path to H0. The durable record and the tree
now disagree permanently: `state.edn` says `:committed`, `journal.log` says `committed`, the file
says `H0`. `recover!` will not touch it (`:committed` is terminal at `:1806-1810`) and `undo!`
would refuse it as `:conflict :digest`. Worse, the revert runs AFTER `release-lock!` at `:1225`,
so those writes happen **with the project LOCK released and outside the publish lock** — the exact
class round 4 exists to eliminate, re-entered from the recovery path.

*Fix, two lines:* record a `:finished?` flag in the transaction's state atom when `finish!`
completes its journal marking, and make `finish-after-throw!` a no-op past that point (or degrade
to the bare `release-lock!` it already falls back to). The tail of `finish!` after `release-lock!`
is bookkeeping; a failure there must not un-commit a commit.

### (e) the age ceiling with the clock going backwards — **CLOSED, two notes**

`txn_journal.clj:417-430`, arithmetic at `:428`.

**Witness (probe E):**

```
E  now=real     => true
E  now=+2h      => true
E  now=BACKWARD => false  (false = fails closed)
E  now=0        => false
E  age of a MISSING lock file: lastModified = 0  -> legacy-lock-dead? = true
E  after `touch`-ing the LOCK 11 days into the past => true
```

A backwards clock yields a negative age and the `>=` refuses — fail-closed, which is the right
direction, and `4bd3a3d`'s `long` coercion makes the comparison exact rather than boxed.

Two notes, neither a defect in reach:
- `(.lastModified lock)` returns `0` for a file that is not there, making a MISSING lock read as
  infinitely old. `recover!` guards this at `:1837` (`(when (.isFile lock) ...)`) and `break-lock!`
  would return `:lock-vanished`, so it is unreachable — but the predicate is wrong on its own
  terms and would bite the next caller.
- The age half of the "receipt" is **mtime**, which is settable by any process and is PRESERVED by
  `cp -p`, `rsync -t`, `tar -x` and a restore from backup. A workspace restored from a snapshot
  presents a legacy LOCK that is old by mtime and fresh in this boot. The pid half is still doing
  the real work; the docstring's word "receipt" (`:406-415`, `:418-423`) is stronger than what
  mtime supports. New-format LOCKs carry `:acquired-at` (`:553`); legacy ones do not, which is
  precisely why mtime is used — say so.

### (f) the tombstone `LOCK.broken.<txid>` — accumulation and collision — **both real, both minor**

`txn_journal.clj:518` (the name), `:520-524` (created with REPLACE_EXISTING).

**Accumulation.** Nothing sweeps them. `grep -rn 'LOCK.broken' src/` returns only `break-lock!`
itself; `recover!` (`:1806`) and `retained-transactions` (`:1634-1636`) both filter
`(.isDirectory d)`, so tombstones are invisible to recovery, to the lease/quota sweep, and to
`dir-bytes`. Probe A2 left **20,356 tombstone files in one transactions directory** from 30,000
break attempts, and every one of them is permanent. No verb lists them, counts them or clears
them, so the "receipt beside the claim it broke" is a receipt nobody can read and nobody can
retire.

**Collision.** The name is the BREAKER's txid, and `begin!` accepts `:txid` from the caller
(`:661`). Two breaks by one txid overwrite each other silently, because the rename carries
REPLACE_EXISTING:

```
F  two breaks with the SAME txid => LOCK.broken.SAME-TXID LOCK.broken.SAME-TXID  tombstone content now = "{:txid \"c2\", :pid 2}"  (c1's receipt is GONE)
F  tombstones in dir = [LOCK.broken.SAME-TXID LOCK.broken.recover-1788434924595-0cdf16ae]
```

`acquire-lock!` breaks at most once per call (`(zero? attempt)`, `:589`) and `recover!` uses
`recover-<new-txid>`, so the collision needs a caller-supplied duplicate txid — narrow, but the
file this build calls evidence is overwritable by name.

*Fix:* name the tombstone by the breaker's txid AND the broken claim's content SHA-256 prefix
(already computed at `:478`), drop REPLACE_EXISTING from the tombstone rename, and give
`retained-transactions` a `:broken-locks` count so a non-zero bucket is visible.

---

## Verdict

# GO-WITH-FIX

*For merging.* All three of my round-three blockers are closed against my own injections rather
than the builder's — a sibling thread now makes `commit!` wait 1,443 ms instead of throwing, a
spawned future waits 2,495 ms for another process instead of writing in 1 ms, and a break against
a live holder restores the live claim and refuses. Both in-scope minors and the doc-completeness
finding are closed, the O(1) claim is corrected in all four places including the receipt value,
and every one of the four gates reproduces the builder's counts exactly. Attacks (b), (c) and (e)
found nothing that changes behaviour in reach. The kernel is still adopted by **no verb** —
`with-cooperating-writes` has zero call sites in `src/` — so merging cannot break anything that
exists today.

*Against merging as-is.* Round 4's own two additions each carry a defect. The break's restore is a
`statx`-then-`rename` check-then-act, proven at the syscall level and measured clobbering a live
third-party claim 129 times in 29,012 — the same end state as the blocker it replaces, reached
through the fix. And `finish-after-throw!` has no terminal-status guard, so an ENOSPC in
`finish!`'s post-release tail reverts a transaction the journal has already recorded as
`:committed`, leaving a permanent disagreement between the record and the tree that no recovery
verb will touch, with the revert writes issued after the project LOCK was released.

**Condition on the merge:** finding 1 (the restore primitive) and finding 2 (the
`finish-after-throw!` guard) are fixed with witnesses, or filed as named adoption blockers with
bead ids, before any verb calls `with-cooperating-writes`. Findings 3 and 4 are honesty edits that
should ride this change; 5 and 6 can ride the adoption build.

---

## Numbered findings (round 4's own)

1. **BLOCKER (adoption) — `break-lock!`'s restore is a check-then-act and can silently destroy a
   third acquirer's live claim.** `txn_journal.clj:534-537`; the guarantee is asserted at
   `:511-513`. `strace` shows `statx(target) = -1 ENOENT` followed by `rename(2)`, which replaces
   unconditionally. Measured: **129 of 29,012** third-party claims clobbered, and **20,356**
   `:restored false` outcomes in which the displaced claim is never put back and its owner is
   never told (`:cause :holder-changed` is discarded by both callers, `:589-596` and
   `:1849-1854`). *Fix:* restore by `Files/createLink(tomb → LOCK)` + `deleteIfExists(tomb)` —
   create-if-absent at the kernel level, the same primitive `write-lock!` uses at `:557` — and
   surface `:restored false` as a typed, counted refusal rather than dropping it.

2. **BLOCKER (adoption) — `finish-after-throw!` has no terminal-status guard, so a throw in
   `finish!`'s post-release tail un-commits a committed transaction.** `txn_journal.clj:1310-1328`,
   `:1448-1450`; `release-lock!` at `:1225` precedes `reclaim-staging!`/`write-lease!` at
   `:1227-1229`. Witness (probe D2, `write-lease!` raising `IOException "ENOSPC"`): bytes on disk
   `"H0\n"` while `state.edn` reads `{:status :committed, :retained true}` and `journal.log` ends
   `committed`; `recover!` will not touch it (`:committed` is terminal at `:1806-1810`) and
   `undo!` would refuse it as `:conflict :digest`. The revert also runs after the project LOCK was
   released, i.e. outside every lock. *Fix:* set a `:finished?` flag in the state atom when
   `finish!` completes its marking, and make `finish-after-throw!` degrade to the bare
   `release-lock!` past that point.

3. **OPEN (honesty) — two contract sentences are stronger than the code.**
   (a) `txn_journal.clj:511-513` says the restore "refuses if a LOCK has appeared meanwhile"; it
   refuses *usually* — see finding 1. (b) `docs/txn-journal.md:129-131` says every exception path
   out of `commit!` "releases the project `LOCK` **and marks the journal**"; witness D1 shows the
   last-resort path releases the LOCK and leaves `state.edn` at `:sealed` for `recover!` to
   finish. Both are one-clause edits, and MEM-014's own rule is the one they fail.

4. **OPEN (minor) — one publication primitive is still outside the writer table and outside the
   opt-in's reach.** `src/clj_surgeon/intent_transaction.clj:2595-2601`, `publish-staged-receipt!`,
   a raw `Files/move` with ATOMIC_MOVE + REPLACE_EXISTING. The table (`docs/txn-journal.md:82-97`)
   is complete for `atomic-write!`/`atomic-create!` — I re-derived all 24 sites and every one is
   listed — but it is derived from those two verbs, and this site uses neither. It publishes a
   receipt, and the table already carries three receipt writers (`extract.clj:242`,
   `mcp_change_buffer.clj:1558`, `mcp_tool.clj:381`), so by its own scope it belongs there. Add a
   row, or say the table is scoped to the two `file-ops` verbs.

5. **OPEN (minor) — the tombstone accumulates without bound, is invisible to every sweep, and its
   name can collide.** `txn_journal.clj:518`, `:520-524`. Probe A2 left 20,356 permanent
   `LOCK.broken.*` files in one transactions directory; `recover!` (`:1806`) and
   `retained-transactions` (`:1634-1636`) both filter `.isDirectory`, so nothing counts them,
   sweeps them, or bills them against the quota — a receipt nobody reads and nobody can retire.
   Two breaks by one caller-supplied txid (`begin!` accepts `:txid` at `:661`) overwrite each
   other silently because the rename carries REPLACE_EXISTING. *Fix:* include the broken claim's
   SHA-256 prefix in the name (it is already computed at `:478`), drop REPLACE_EXISTING, and
   expose a `:broken-locks` count so a non-zero bucket is visible.

6. **OPEN (minor) — the legacy break's age half rests on mtime, which is neither monotonic nor
   unforgeable.** `txn_journal.clj:428`. A backwards clock fails closed, which is right. But mtime
   is settable by any process and is PRESERVED by `cp -p`, `rsync -t`, `tar -x` and a restore from
   backup, so a workspace restored from a snapshot presents a legacy LOCK that is "old" and fresh
   in this boot; and `(.lastModified)` of a missing file returns `0`, making an absent lock read
   as infinitely old (unreachable today — `recover!` guards it at `:1837` — but wrong on its own
   terms). The pid half carries the real weight; the word "receipt" at `:406-415`/`:418-423`
   should say which half is checkable and which is only a heuristic.

**Round-three findings 6, 8, 9 and 10 remain OPEN and unchanged**, as scoped to the adoption
build: `:ctime-ns` degrading to nil while `:stat-fields` advertises it (`:80`, `:235-238`);
`release-receipt!` reading the lease with `read-edn-file`; `:lease :unreadable` for a lease that
was simply never written (`:1619`, `:1711`); and the `undo!`-of-a-deleted-target remedy with no
verb behind it (`:1682`).
