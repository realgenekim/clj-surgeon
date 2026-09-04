# Kernel round 5 — Opus executed re-check of bridge/txn-journal at 4b12c9e (2026-09-03T12:27Z)

Verdict: **GO-WITH-FIX**, HELD for round 6. Round-4 items: 1 CLOSED (0 of 9,948 clobbered), 2 CLOSED, 3 PARTIAL, 4 CLOSED, 5 PARTIAL, 6 CLOSED. NEW blocker: `recover!` prunes the tombstone it just created (retention on `.lastModified`; `Files/move` preserves mtime so the tombstone inherits the old LOCK's age) and returns a receipt naming a file that no longer exists — deterministic, public verb. Minors: two mtime reads where the age basis is now newest-of-mtime/ctime; collision guard is `.exists` + ATOMIC_MOVE (13/4,000 races destroyed the judged claim); a crash between createLink and deleteIfExists leaves a tombstone hard-linked to the live LOCK. Round 6 launched.

## Opus verdict, verbatim

# txn-journal round 5 (`4b12c9e`) — Opus executed re-review: **GO-WITH-FIX**

**Reviewer of record.** Opus, fourth consecutive round on this branch. Round-four verdict under
re-judgement: `git show origin/main:docs/observations/2026-09-03-txn-journal-round4-rereview-opus.md`;
round three's findings 6, 8–10 remain adoption-scoped and are re-checked below.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn5-wt` at `4b12c9e` (`git rev-parse --short HEAD`
=> `4b12c9e`; `git status --short` empty at start and end; nothing committed, stashed or pushed).
Fixtures under `/tmp/txn5-fx` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M <probe>` from the worktree — never `make mcp-test`,
never a live server, no port in 7888–7895 or 7906 contacted. `make memory-battery` /
`make memory-red` were NOT run (exclusive lock, another lane). Every process signalled was one I
started.

**Headline.** Both round-four blockers are CLOSED against my own injections: the restore clobbered
**0 of 9,948** third-party claims where round four measured 129 of 29,012, and a throw in
`finish!`'s post-release tail no longer un-commits a commit. Items 4 and 6 are CLOSED, item 3 is
half closed and half re-introduced in a new sentence, and item 5 is **PARTIAL** — the tombstone is
now visible, counted and named, but its retention age is inherited from the LOCK it broke, so
**the same `recover!` that breaks a stale lock deletes its own evidence in the same call and
returns a receipt naming a file that no longer exists.** That is deterministic, needs no race, and
runs through the public verb with no injection point. It is round 5's own defect, in the same
commit whose subject line is "the break's evidence is visible, bounded, and cannot be overwritten".
All four gates reproduce the builder's counts exactly, and the kernel is still adopted by **no
verb**, which is what keeps this GO-WITH-FIX rather than NO-GO.

---

## Executed gates (mine, at `4b12c9e`, each once under `/home/forge/bin/suite-run`)

```
$ /home/forge/bin/suite-run bash -c 'cd /home/forge/tmp/sol/txn5-wt && bb test/run_all.clj'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.
TESTFAST_EXIT=0

$ /home/forge/bin/suite-run bash -c '... swipl -q -f test/mcp_operation_contract_oracle.pl ...'
ORACLE_EXIT=0
Ran 449 tests containing 4393 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
kernel warning check: 2 namespace(s), 0 warning(s)
KWC_EXIT=0
```

Builder's stated counts: test-fast 720/5976/0, mcp-test 449/4393/0, oracle pass, warning check 0.
All four match.

**Adoption, re-derived.** `grep -rn 'with-cooperating-writes' src/` returns the definition
(`txn_journal.clj:827`) and one docstring mention (`file_ops.clj:24`) — zero call sites.
`grep -rn 'txn-journal' src/ --include=*.clj` outside the kernel returns one line,
`scope_stream.clj:24`, used only for `journal/sha256-string` at `:365`. No `:txn/recover`
operation is registered anywhere in `src/`. Merging cannot break anything that exists today.

---

## Round-four items 1–6, re-judged on my own probes

### Item 1 — `break-lock!`'s restore is a check-then-act and can destroy a third acquirer's live claim — **CLOSED**

`txn_journal.clj:629-652` (`restore-lock!`, `Files/createLink` at `:642` then
`Files/deleteIfExists` at `:643`), `:645-647` (`FileAlreadyExistsException` → `:restored false
:restore-cause :lock-reappeared`, counted), `:648` (`UnsupportedOperationException` →
`restore-by-move!` at `:606-627`), `:530-542` (`displaced-claims` atom and
`displaced-claim-count`), `:596-604` (`displaced-line`), `:781/:784` (`acquire-lock!` keeps it),
`:2110-2112` (`recover!` keeps it).

**Witness (probe A1, `/tmp/txn5-fx/p1_storm.clj`), my own storm and my own clobber counter.** A
hammer thread creates a uniquely-named third-party claim by `Files/createLink` whenever `LOCK` is
absent; `break-lock!` is called 20,000 times with a deliberately mismatching claim so the restore
branch always runs; a claim is counted CLOBBERED when its id is on neither `LOCK` nor any
tombstone at the end — i.e. destroyed rather than displaced:

```
A1 third-acquirer claims created = 9948
A1 break outcomes = {:lock-vanished 21, :holder-changed 19979}
A1 tombstones left = 9947
A1 claims SILENTLY CLOBBERED (created but on neither LOCK nor any tombstone) = 0
A1 displaced-claim-count = 9947
A1 sample clobbered = ()
```

Round four's shape here was **129 of 29,012 clobbered** and **20,356 `:restored false` never
surfaced**. Zero clobbered now, and every one of the 9,947 refusals is counted in
`displaced-claim-count` and carried to the caller as `:lock-break-displaced` with `:cause
:holder-changed`, `:restored false`, `:restore-cause :lock-reappeared`, `:tombstone` and
`:displaced-claims-total`. `link(2)` is create-if-absent in the kernel and the JDK's
`statx`-then-`rename` is gone from this path. Both halves of the blocker are closed.

The `restore-by-move!` fallback (`:606-627`) is still check-then-act and says so in its own
docstring; on ext4 it is unreachable. Accepted as the builder states it.

### Item 2 — `finish-after-throw!` has no terminal-status guard, so a throw in `finish!`'s post-release tail un-commits a committed transaction — **CLOSED**

`txn_journal.clj:1436-1439` (the TERMINAL comment and `(swap! (:state txn) assoc :finished? true)`,
after `append-journal!` at `:1432` and `write-state!` at `:1433-1435`), `:1552-1555`
(`finish-after-throw!` degrades to the bare `release-lock!` past the flag).

**Witness (probe D2, `/tmp/txn5-fx/p2_tail.clj`), round four's exact injection —
`write-lease!` raising `IOException "ENOSPC writing lease.edn"`:**

```
---- D2-write-lease ----
 state.edn BEFORE commit  = :sealed
 commit! threw            = java.io.IOException: ENOSPC writing lease.edn
 bytes on disk            = "(ns f0) (def v :NEW)\n" (= NEW)
 state.edn AFTER          = {..., :status :committed, :finished-at "...", :retained true, :restore-failed false}
 journal.log tail         = (... "write-done\t..." "committed")
 LOCK present             = false
 next begin! ok           = true
 recover! =>              = {:ok true, :transactions-recovered 0, ...}
 bytes AFTER recover!     = "(ns f0) (def v :NEW)\n"
```

Round four's shape was `bytes on disk => "H0\n"` beside `state.edn :committed` — a permanent
disagreement. The record and the tree now agree, the LOCK is released, the original exception
still escapes, and `recover!` correctly leaves the terminal journal alone. A throw in
`reclaim-staging!` — the other member of the post-flag tail — behaves identically
(`B3-reclaim-staging`: bytes `:NEW`, `state.edn :committed`, LOCK released).

### Item 3 — two contract sentences stronger than the code — **PARTIAL (both fixed; two NEW sentences of the same class ship)**

*(a) CLOSED.* The old "`Files/move` … refuses if a LOCK has appeared meanwhile" is gone.
`restore-lock!`'s docstring (`:629-641`) now names the defect explicitly and `break-lock!`
(`:673-690`) states the guarantee exactly: "It never replaces a LOCK that appeared meanwhile. It
does NOT guarantee the judged claim gets back." My probe A1 (0 clobbers, 9,947 counted refusals)
is the witness that this text is now true.

*(b) CLOSED.* `docs/txn-journal.md:126-132` now reads "The journal is marked too **whenever the
marking itself has not failed**; when the rollback or the marking is what threw, the LAST-RESORT
path releases the lock alone and leaves `state.edn` at `:sealed`, which `recover!` picks up".
That matches round four's probe D1 exactly.

*NEW, same class — two sentences the code does not keep. See findings N1 and N3.*
`txn_journal.clj:684-685`: "an existing tombstone of that name is a typed refusal taken BEFORE the
LOCK is touched, **never a replacement**" — falsified 13 times in 4,000 races (probe D1 below).
`txn_journal.clj:2118-2120`: "Pruned AFTER this run's own break, so **a tombstone made a moment ago
is never retired by the recovery that made it**" — falsified deterministically (probe P6 below).
The second is also the EARS text of MEM-013, not only a comment.

### Item 4 — `publish-staged-receipt!` outside the writer table — **CLOSED**

`docs/txn-journal.md:97` now carries the row, and I re-read the cited site:
`src/clj_surgeon/intent_transaction.clj:2595-2601` is
`(Files/move (.toPath staged) (.toPath (io/file receipt-path)) (into-array CopyOption
[StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))`, called at `:2756`. The
row names it a raw `Files/move` reaching neither `atomic-write!` nor `atomic-create!`, states why
the table's derivation missed it, and says it needs a code change rather than an opt-in. Exactly
the edit the finding asked for.

### Item 5 — the tombstone accumulates without bound, is invisible, and its name can collide — **PARTIAL**

*Visible and counted: CLOSED.* `txn_journal.clj:562-570` (`broken-lock-files`), `:1888-1896`
(`retained-transactions` `:kind :broken-lock` rows), `:2121` (`recover!` returns
`:broken-locks`).

**Witness (probe C1, `/tmp/txn5-fx/p3_tomb.clj`):**

```
C1 retained-transactions rows = [{... :txid "LOCK.broken.FORGED-OLD" :kind :broken-lock :bytes 31 :age-ms 2592000019 :retention-ms 86400000 :retired-by :txn/recover :evictable false} ... x3]
C1 recover! :broken-locks     = {:found 3, :pruned 2, :remaining 1, :retention-ms 86400000}
C1 tombstones surviving       = ("LOCK.broken.FRESH")
```

Round four's "no verb lists them, counts them or clears them" is answered.

*Bounded: NOT closed — see finding N1.* The retention age is measured from the tombstone's mtime,
and `Files/move` PRESERVES mtime, so the tombstone inherits the age of the LOCK it broke. A stale
lock older than 24 h — the ordinary case a break exists for — produces a tombstone that is already
past retention the instant it is created, and the same `recover!` deletes it.

*Collision: NOT closed — see finding N3.* The `.exists tomb` guard at `:692` is a check-then-act,
and the `Files/move` at `:700-702` carries `ATOMIC_MOVE`, which on POSIX is `rename(2)` and
replaces unconditionally; the `FileAlreadyExistsException` catch at `:717-718` cannot fire on
ext4. Two breakers sharing one txid destroy each other's evidence, measured.

### Item 6 — the legacy break's age half rests on mtime — **CLOSED**

`txn_journal.clj:424-448` (`lock-age-basis-ms`, the newest of mtime and `unix:ctime`, `nil` for a
file that is not there), `:450-465` (`legacy-lock-dead?`), `:405-422` (the docstring naming which
half is checkable), `:824-825` (the refusal's remedy quoting the rule).

**Witness (probe L1, `/tmp/txn5-fx/p4_age.clj`) — round four's own forgery, `touch` 11 days back
on a dead-pid legacy LOCK:**

```
L1 fresh legacy lock: dead?      = false
L1 after `touch` 11 days back:
   mtime age-ms = 950400006  ctime age-ms = 3  basis age-ms = 3
   legacy-lock-dead? (mtime forged old) = false    <- round 4 said TRUE here
L1 recover! --break-legacy-lock  = {:ok true, :transactions-recovered 0, :broken-locks {:found 0, :pruned 0, :remaining 0, :retention-ms 86400000}}
L1 LOCK still present            = true
```

The absent-file half is closed too: `lock-age-basis-ms` is guarded by `(when (.isFile lock) ...)`
and returns `nil`, so an absent lock is absent rather than infinitely old. The pre-existing
ceiling witness was re-based honestly — `test/clj_surgeon/txn_journal_test.clj:1734-1744` still
asserts one millisecond under the ceiling refuses and exactly at it breaks, now by moving the
`:now-ms` clock against the file's real basis instead of back-dating mtime to 2023.

**Note, not a defect.** The same fix makes a workspace restored from a snapshot NOT breakable for
one hour where it used to be breakable at once — `cp -p` preserves mtime and gives a fresh ctime
(`E3 cp -p COPY: mtime age-ms = 950400408  ctime age-ms = 403  basis age-ms = 403  dead? = false`).
Bounded, fails closed, and stated in the docstring. Accepted.

### Round-three findings 6, 8, 9, 10 — **OPEN, as scoped**

Unchanged and correctly so, riding the adoption build: `:ctime-ns` degrading to nil while
`:stat-fields` advertises it; `release-receipt!` reading the lease with `read-edn-file`;
`:lease :unreadable` covering a lease that was never written; the `undo!`-of-a-deleted-target
remedy with no verb behind it.

---

## Round 5's own additions, attacked

### (a) the `createLink` restore — the tomb is a hard link to LOCK content

**Does the tombstone's deletion race a concurrent reader?** Not into evidence loss. On a
successful restore the claim is back in `LOCK`, and the outcome does **not** publish a
`:tombstone` name — `txn_journal.clj:714-715` attaches it only `(false? (:restored outcome))` — so
no receipt points a reader at the transient file. A reader that had the tomb open keeps the inode
by POSIX semantics; a reader that arrives after the unlink sees ENOENT.

**What it does cost is listing accuracy.** Witness (probe A5, `/tmp/txn5-fx/p7_reader.clj`), a
concurrent lister doing exactly what `broken-lock-files` does while 3,000 restores run:

```
A5 tombstone rows a concurrent lister saw       = 9831
A5 rows whose file had VANISHED (lastModified 0)= 822  <- the sweep reads these as infinitely old and bills bytes 0
A5 rows with a zero-length body                 = 513
```

8.4% of rows were of files already gone. `prune-broken-locks!` (`:572-593`) reads
`(.lastModified f)` = 0 for those, treats them as infinitely old, calls `deleteIfExists` (false),
counts them in `:found` but not `:pruned`, so `:remaining` over-reports; `retained-transactions`
(`:1888-1896`) emits `:age-ms` ≈ 1.79e12 and `:bytes 0`. No claim is lost. See finding N2 — this
is the absent-file bug that `lock-age-basis-ms` was written to fix, re-appearing in the new code.

**Is the inode identity check still meaningful with two links?** Yes. The check at `:706-708`
runs on the tomb BEFORE any restore, when the rename has left it with exactly one link, and it
compares content AND `(dev,ino)` — inode-number reuse alone cannot pass it. The two-link window
exists only between `Files/createLink` (`:642`) and `Files/deleteIfExists` (`:643`), after the
check.

**One real residual in that window.** A crash between those two lines leaves a tombstone that is a
hard link to the LIVE `LOCK`. Witness (probe A3, `/tmp/txn5-fx/p5_race.clj`):

```
A3 LOCK file-key  = (dev=24,ino=21295571)
A3 tomb file-key  = (dev=24,ino=21295571)  same inode? = true
A3 tomb content   = "{:txid \"A-LIVE\", :pid 2746129}"  <- the LIVE holder's claim, filed as break evidence
A3 deleting the tomb leaves LOCK intact? = true
```

No data loss, but `retained-transactions` then reports a `:kind :broken-lock` row for a break that
never happened, naming a claim that currently holds the lock — false evidence — and a later break
by that txid is refused `:tombstone-exists`. Finding N4; low severity.

### (b) `:finished?` — set where exactly, and the window before it

Set at `txn_journal.clj:1439`, AFTER `append-journal! txn (name status)` (`:1432`) and
`write-state!` (`:1433-1435`), and BEFORE the journal-stream close, `release-lock!` (`:1442`) and
the `reclaim-staging!`/`write-lease!` tail (`:1443-1446`). That is the correct order: the flag
means "the durable record already says so", so it must not be set before the record exists.

**Can a throw BETWEEN the last byte publish and the flag set still revert? Yes — and it should.**
Witness (probe P2), throws injected at each point in that window:

```
---- B1-append-journal-terminal ----   (throw inside append-journal! of "committed")
 state.edn BEFORE commit  = :sealed
 commit! threw            = java.io.IOException: ENOSPC appending 'committed'
 bytes on disk            = "(ns f0) (def v 0)\n" (= H0)
 state.edn AFTER          = {:READ-ERR ".../state.edn (No such file or directory)"}
 journal.log tail         = nil
 LOCK present             = false
 next begin! ok           = true
 recover! =>              = {:ok true, :transactions-recovered 0, ...}
 bytes AFTER recover!     = "(ns f0) (def v 0)\n"

---- B2-write-state-terminal ----      (throw inside write-state!, AFTER journal.log says "committed")
 state.edn BEFORE commit  = :sealed
 commit! threw            = java.io.IOException: ENOSPC writing state.edn
 bytes on disk            = "(ns f0) (def v 0)\n" (= H0)
 state.edn AFTER          = {:READ-ERR ".../state.edn (No such file or directory)"}
 journal.log tail         = nil
 LOCK present             = false
 next begin! ok           = true
 bytes AFTER recover!     = "(ns f0) (def v 0)\n"
```

Both revert to H0 and then `finish!` the transaction `:rolled-back`; because a rolled-back journal
is not retained, `delete-tree!` removes the directory, so `state.edn` and `journal.log` are gone
entirely. **The tree and the record do not disagree in this window** — the record simply does not
exist, and `:committed` was never the terminal status. `state.edn` reads `:sealed` throughout the
window, which is what a crash there would leave for `recover!` to finish. Correct.

*One structural note, pre-existing and out of round 5's scope:* the terminal record is written in
two steps with different durability — `append-journal!` (`:310-315`) fsyncs, `write-state!`
(`:317-323`) is a plain `spit` with no fsync and no atomic rename. A crash between them leaves
`journal.log` ending `committed` beside `state.edn` `:sealed`, and `recover!` reverts on the
`state.edn` authority. Safe, because `state.edn` is the authority and the flag follows both — but
the code comment at `:1436-1438` speaks of them as one act.

### (c) tombstone retention pruned on mtime — is evidence deletable by the party it incriminates?

**Yes, and worse than the question supposes.** Witness (probe C1/C2), a tombstone whose mtime is
back-dated 30 days and whose ctime is 0 ms old:

```
C1 forged tombstone mtime age-ms = 2592000000  ctime age-ms = -12
C1 recover! :broken-locks        = {:found 3, :pruned 2, :remaining 1, :retention-ms 86400000}
C1 FORGED-OLD still on disk?     = false
C2 prune predicate uses .lastModified  -> would prune? = true
C2 the SAME build's lock-age-basis-ms   -> would prune? = false
```

`prune-broken-locks!` at `:581-582` reads `(.lastModified f)` alone. `lock-age-basis-ms` — the
newest-of-mtime-and-ctime rule this same commit series introduced 150 lines earlier for exactly
this reason — would have kept the file. So the build hardened one timestamp read and left two
others (this, and the absent-file case in (a)) reading the stamp it declared insufficient.

The adversarial framing is the weaker half: a breaker that can `touch` its own tombstone can also
`rm` it. The accidental framing is the one that bites, and it is the build's own list: `cp -p`,
`rsync -t`, `tar -x` and a restore from backup all preserve mtime, so a transactions directory
restored from a snapshot loses every break receipt on its first `recover!`. Finding N2.

### (d) the collision residual — is the window narrower than a caller can hit on purpose?

**No. I hit it deliberately, 13 times in 4,000 attempts.** `begin!` takes `:txid` verbatim from
its caller (`txn_journal.clj:870`, `txid (or txid (new-txid))`), and `recover!` uses a fresh
`recover-<new-txid>` (`:2103`), so this needs two `begin!`-side breakers sharing a txid — which a
caller supplies. Witness (probe D1, `/tmp/txn5-fx/p5_race.clj`): two futures released from one
barrier, both calling `break-lock!` with txid `"SAME-TXID"` and the same judged claim, with a
hammer re-creating a LOCK so the second breaker has something to move:

```
D1 rounds                                = 4000
D1 rounds where BOTH breakers moved a file = 13
D1 :tombstone-exists refusals             = 3638
D1 rounds where a claim was DESTROYED     = 13
D1 first destroyed sample                 = [19 "{:broken false, :cause :holder-changed, :restored false, :restore-cause :lock-reappeared, :tombstone \"LOCK.broken.SAME-TXID\"}" "{:broken false, :cause :holder-changed, :restored false, :restore-cause :lock-reappeared, :tombstone \"LOCK.broken.SAME-TXID\"}" "#{\"H-19-81\" \"H-19-80\"}"]
```

The guard works 3,638 times sequentially and fails 13 times under the race — 0.33%, and the
consequence is not merely a lost file. **Both callers received the same `:tombstone
"LOCK.broken.SAME-TXID"` in their displacement receipt, and only one of them is true**; the judged
claim `VICTIM-19` is absent from the directory entirely, while the receipt tells both breakers
where to find it. That is a receipt naming a subject it is not bound to — house rule 20 — and it
falsifies MEM-013's own EARS clause ("a name already present being a typed refusal taken before
the lock is touched, **never a replacement**") and its own violation row ("or a break overwrites
another break's evidence"). Finding N3.

### (e) newest-of-mtime/ctime — can a live holder be made to look FRESH for ever?

**Yes, and it is by design, not a liveness regression.** ctime advances on `chmod`, `chown` and
`link(2)`, none of which touch mtime. Witness (probe E1/E2):

```
E1 before chmod: basis age-ms   = 1  dead? = false
E1 after chmod:  mtime age-ms   = 950400004  ctime age-ms = 2  basis age-ms = 2
E1 legacy-lock-dead? after chmod= false   <- FALSE means a dead holder's lock is now UNBREAKABLE
E1 recover! --break-legacy-lock = {:ok true, :transactions-recovered 0, :broken-locks {...}}
E1 LOCK still present           = true
E2 after a hard link:  ctime age-ms = 1  basis age-ms = 1
```

So a watchdog that keeps `chmod`-ing (or hard-linking) a legacy LOCK keeps a genuinely dead
holder's claim unbreakable indefinitely. **Not a regression:** under the previous mtime-only rule
the same watchdog running `touch` achieved exactly the same thing, and more cheaply. The change
removes the ability to forge a lock OLD (the break direction) and leaves the ability to keep it
FRESH (the refuse direction) — it moves the only remaining forgery into the fail-closed direction,
which is the correct trade. The bounded real cost is the restore-from-backup case in item 6 above:
one hour of unbreakability, with `:break-legacy-lock` as the remedy once it passes.

Nothing inside the kernel refreshes a LOCK's ctime behind the operator's back:
`breakable-causes` (`:519-526`) excludes `:legacy-format`, so `acquire-lock!` never renames a
legacy claim, and `recover!` reaches `break-lock!` only after `legacy-lock-dead?` has already
returned true (`:2098-2100`). The self-refresh path I looked for does not exist.

---

## Numbered findings (round 5's own)

1. **BLOCKER (adoption) — `recover!` deletes its own break's evidence in the same call, and
   returns a receipt naming a file that does not exist.** `txn_journal.clj:581-582` (the prune
   reads `(.lastModified f)`), `:700-702` (the break's `Files/move`, which preserves mtime), `:2121`
   (the prune call), and the comment at `:2118-2120` that asserts the opposite. A tombstone
   inherits the mtime of the LOCK it broke, so a stale lock older than `broken-lock-retention-ms`
   is already past retention the instant its tombstone exists. Witness (probe P6,
   `/tmp/txn5-fx/p6_prune.clj`), a crashed holder whose LOCK is two days old — the ordinary case a
   break exists for:

   ```
   P6 LOCK mtime age-ms before break = 172800004
   P6 recover! :lock-broken          = {:reason :stale-holder, :cause :process-not-alive, :pid 2811931, :holder-txid "CRASHED-HOLDER", :broken-at "2026-09-03T12:22:07.053523054Z", :tombstone "LOCK.broken.recover-1788438127052-40f058e5", :content-sha256 "68e330bf..."}
   P6 recover! :broken-locks         = {:found 1, :pruned 1, :remaining 0, :retention-ms 86400000}
   P6 tombstones on disk AFTER the SAME recover! = ()
   P6 the tombstone recover! NAMED still exists? = false
   P6 retained-transactions rows     = []
   ```

   Deterministic, no race, no injection point, through the public verb. The receipt is worse than
   a missing one: it names a subject that was destroyed by the same call that minted the name.
   MEM-013 requires the evidence be "bound[ed] by a published retention age that recovery enforces
   and counts"; recovery enforces it against the wrong clock. *Fix:* stamp the tombstone's own
   creation time — `(.setLastModified tomb (System/currentTimeMillis))` immediately after the
   rename, or better, carry `:broken-at` (already computed at `:487`) into the tombstone's name or
   a sidecar and prune on that. Add a witness that breaks a LOCK older than the retention age and
   asserts the tombstone survives the breaking `recover!`.

2. **OPEN (minor, but the same class the build just fixed) — two mtime reads in the new tombstone
   code use the stamp this build declared insufficient.** `txn_journal.clj:581-582` and
   `:1894` both read `(.lastModified f)` while `lock-age-basis-ms` (`:424-448`) exists 150 lines
   above for exactly this. Consequences measured: a back-dated tombstone is pruned on the next
   `recover!` although its ctime is 12 ms old (`C2 prune predicate ... would prune? = true` vs
   `the SAME build's lock-age-basis-ms -> would prune? = false`), so `cp -p` / `rsync -t` /
   `tar -x` / a snapshot restore silently retire every break receipt; and a file that vanishes
   between the listing and the stat reads as infinitely old (`A4 .lastModified of an ABSENT
   tombstone = 0 -> prune reads its age as = 1788437953935`), inflating `:remaining` and emitting
   `retained-transactions` rows with `:bytes 0` and an epoch-sized `:age-ms` — 822 of 9,831 rows
   under concurrency (probe A5). *Fix:* use `lock-age-basis-ms` in the prune and in the row, and
   skip a file whose basis is `nil`.

3. **OPEN (minor) — the tombstone-collision guard is a check-then-act, and both racing breakers
   are told the same false tombstone.** `txn_journal.clj:692` (`.exists tomb`), `:700-702`
   (`ATOMIC_MOVE`, i.e. `rename(2)`, which replaces unconditionally), `:717-718` (a
   `FileAlreadyExistsException` catch that cannot fire on ext4); the claim it fails is at
   `:684-685` and in MEM-013's EARS text. Measured 13 of 4,000 deliberate races (probe D1 above):
   the judged claim is destroyed and both breakers' `:lock-break-displaced` names
   `LOCK.broken.SAME-TXID` as the place to find it. The builder discloses the race; the docstring
   and the intent still say "never a replacement". *Fix:* either name the tombstone by the
   breaker's txid AND the broken claim's SHA-256 prefix (already computed at `:487`) so two
   breakers cannot share a name, or create the tombstone with `Files/createLink` from a
   per-attempt temp before the rename. Failing that, weaken the two sentences to match the code.

4. **OPEN (minor) — a crash between `createLink` and `deleteIfExists` leaves false break
   evidence.** `txn_journal.clj:642-643`. The tombstone is then a hard link to the LIVE `LOCK`:
   same `(dev,ino)`, identical content (probe A3 above). `retained-transactions` reports it as a
   `:kind :broken-lock` row for a break that never occurred, naming a claim that currently holds
   the lock, and a later break by that txid is refused `:tombstone-exists`. Deleting it leaves
   `LOCK` intact, so there is no data loss. *Fix:* one sentence in `restore-lock!`'s docstring, or
   have the prune skip a tombstone whose `(dev,ino)` equals the live `LOCK`'s.

5. **NOTE (accepted, by design) — the ctime half can be kept fresh for ever, and that is the
   fail-closed direction.** `txn_journal.clj:424-448`. A `chmod` or `link(2)` watchdog holds a
   dead holder's legacy claim unbreakable indefinitely (probe E1/E2). This is not a liveness
   regression: the previous mtime-only rule had the same exposure via `touch`, and the change
   removes the forgery in the *breaking* direction while leaving it in the *refusing* one. The
   bounded new cost is one hour of unbreakability for a workspace restored with `cp -p` (probe
   E3). Stated in the docstring; no action.

6. **NOTE (pre-existing, out of scope) — the terminal record is two writes with different
   durability.** `txn_journal.clj:310-315` (`append-journal!` fsyncs) versus `:317-323`
   (`write-state!` is a plain `spit`). `:finished?` is correctly set after both (`:1439`), and
   every throw I injected in that window reverts cleanly to H0 with no record left behind (probe
   B1/B2 above), because `state.edn` is the authority and never said `:committed`. The comment at
   `:1436-1438` speaks of the two as one act; worth a clause if the kernel is ever adopted under a
   power-loss requirement.

**Round-three findings 6, 8, 9 and 10 remain OPEN and unchanged**, as scoped to the adoption build.

---

## Verdict

# GO-WITH-FIX

*For merging.* Both round-four blockers are closed against my own injections, not the builder's:
the restore clobbered **0 of 9,948** third-party claims where round four measured 129 of 29,012,
and every one of the 9,947 refusals is typed, counted in `displaced-claim-count` and carried to
both callers; and a throw in `finish!`'s post-release tail now leaves the committed bytes on disk
beside a `state.edn` that says `:committed`, where round four found `H0` beside `:committed`
permanently. Items 4 and 6 are closed on my own probes — the mtime forgery that worked in round
four now leaves a basis age of 3 ms and the break is refused — and the ceiling witness was re-based
honestly, still asserting one millisecond under and exactly at the ceiling. Item 5's visibility and
counting half is closed. All four gates reproduce the builder's counts exactly, and the kernel is
still adopted by **no verb**: `with-cooperating-writes` has zero call sites, no `:txn/recover`
operation is registered, and the only cross-namespace use of the kernel is `sha256-string`.
Merging cannot break anything that exists today.

*Against merging as-is.* Round 5's headline commit is `8fffc85`, "the break's evidence is visible,
bounded, and cannot be overwritten", and two of those three clauses are false. **Bounded** is false
deterministically: a tombstone inherits the mtime of the LOCK it broke, so breaking a two-day-old
crashed holder's lock produces a receipt naming `LOCK.broken.recover-…` and a directory that no
longer contains it — the same `recover!` call, no race, no injection, the ordinary case. **Cannot
be overwritten** is false under a race a caller can drive on purpose: 13 of 4,000, with both
breakers handed the same tombstone name for claims only one of them owns. Both are the shape this
branch keeps producing — a receipt that names a subject the evidence source cannot actually
supply — and finding N1 in particular fails MEM-013's own EARS clause and would be blessed by an
oracle that is green.

**Condition on the merge:** finding 1 is fixed with a witness that breaks a lock older than
`broken-lock-retention-ms` and asserts the tombstone survives the breaking `recover!`, and findings
2 and 3 are fixed or filed as named adoption blockers with bead ids, before any verb calls
`with-cooperating-writes`, `begin!` or `recover!`. Finding 4 is a docstring clause that should ride
this change; findings 5 and 6 need no action.
