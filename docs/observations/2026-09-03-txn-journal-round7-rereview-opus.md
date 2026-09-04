# Kernel round 7 — Opus executed re-check of bridge/txn-journal at 11c7377 (2026-09-03T20:24Z)

Verdict: **GO-WITH-FIX** — "Merge bridge/txn-journal at 11c7377. Gates verified independently; adoption zero, so merging cannot break anything that exists." All four round-6 minors CLOSED on the reviewer's own probes. Before ANY adoption: (1) a marker-only interrupted-break match whose LOCK is gone or names a different inode is uncorroborated — stamp and keep it, typed, never delete (probe FORGE: one hand-written sidecar deleted a genuine recover! break's evidence); (2) a failed stamp write must refuse the unlink; (3) marker written BEFORE createLink (two orderings still yield a false real break); (4) extend the receipt witness to "exists at return OR carries `:evidence`" and drive a revert; (5) count `:unreadable-stamps` (61 s NFS skew marks every row unreadable with no count). Round 8 launched.

## Opus verdict, verbatim

# txn-journal round 7 (`11c7377`) — Opus executed re-review: **GO-WITH-FIX**

**Reviewer of record.** Opus, sixth consecutive round. Round-six verdict under re-judgement:
`git show origin/main:docs/observations/2026-09-03-txn-journal-round6-rereview-opus.md`.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn7-wt` at `11c7377` (`git rev-parse --short HEAD`
=> `11c7377`; `git status --porcelain` empty at start and at end; `git stash list` 0; nothing
committed, stashed or pushed). Fixtures under `/tmp/txn7-fx-sol` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M <probe>` from the worktree. Gates run once each
under `/home/forge/bin/suite-run`; `make mcp-test`, `make memory-battery` and `make memory-red`
were never invoked, no port in 7888-7895 or 7906 was contacted, and every process signalled was
one I started.

**Headline.** All four of round six's minors are CLOSED against my own re-run probes, and three of
them are closed structurally rather than narrowed. But the fix for finding 4 — the `:phase :linked`
marker — introduces a failure whose DIRECTION is worse than the defect it replaced. Round six's
worst forgery outcome was a tombstone kept for ever (fail-safe). Round seven's marker makes a
COMPLETED break's evidence deletable: a real break wearing a `:phase :linked` sidecar is typed
`:interrupted-break` and `recover!` REVERTS it, unlinking the tombstone and its sidecar in one
call. I reached that state two ways — a swallowed write failure in `stamp-broken-at!`
(`txn_journal.clj:674-675`), and an ordinary writer of the transactions directory, the same threat
model the builder accepted and fixed for the stamp one commit earlier at `8151dbd`. No claim is
lost in any shape I drove, and adoption is still zero, so this is a merge-safe branch with one
finding that must land before anything adopts the kernel.

---

## Executed gates (mine, at `11c7377`, each once under `/home/forge/bin/suite-run`)

```
$ /home/forge/bin/suite-run bash -c 'cd /home/forge/tmp/sol/txn7-wt && bb test/run_all.clj; echo "TESTFAST_EXIT=$?"'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.
TESTFAST_EXIT=0

$ /home/forge/bin/suite-run bash -c '... swipl -q -f test/mcp_operation_contract_oracle.pl ...
                                     clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test ...
                                     clojure -M test/kernel_warning_check.clj'
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0
Ran 463 tests containing 4492 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
kernel warning check: 2 namespace(s), 0 warning(s)
KWC_EXIT=0
```

Builder's stated counts: test-fast 720/5976/0, mcp-test 463/4492/0, oracle pass, warning check 0.
All four match exactly.

**Adoption, re-derived at `11c7377`.** `grep -rn 'txn-journal\|txn_journal' src/ --include=*.clj`
excluding the kernel itself returns two lines: `scope_stream.clj:24` (a require) and
`file_ops.clj:24` (a docstring mention). No `:txn/recover` operation is registered anywhere in
`src/` — the two `:next_call {:op :txn/recover ...}` occurrences at `txn_journal.clj:1224` and
`:1242` are hint strings inside a refusal, not registrations. Merging cannot break anything that
exists today.

---

## Round six's four minors, re-judged on my own re-run probes

### Minor 1 — the break path is unrecorded and the opt is reachable by an ordinary word — **CLOSED**

`txn_journal.clj:1143-1145` (`break-lock!` dispatches on `:unsafe-break-by-move`), `:1010` and
`:1085` (`:break-path :move` / `:break-path :link` produced), `:1202-1208` (`acquire-lock!` now
`select-keys` includes `:break-path`), `:2644-2648` (`recover!` the same),
`:981-994` (`break-by-move!` docstring names the rename as check-then-act and the opt as the only
way to reach it where `link(2)` works). The `UnsupportedOperationException` catch at `:1097`
keeps the fallback reachable on a filesystem without `link(2)` without anyone typing the word.

**Witness (probe D, `/tmp/txn7-fx-sol/p1_minors.clj`) — my round-six probe, re-run, plus the old
keyword and the `begin!` surface:**

```
D1 recover! {:unsafe-break-by-move true} :lock-broken = {:reason :stale-holder, :cause :process-not-alive, :pid 3815895, :holder-txid "CRASHED", :broken-at "2026-09-03T20:11:34.389099302Z", :tombstone "LOCK.broken.recover-1788466294385-6950fbf9", :content-sha256 "668e1c4ca399c6082a183f0ddd3aef1549b25bf52c30c0129909b336df5ccf3f", :break-path :move}
D1 :break-path in receipt = :move
D2 recover! default :break-path = :link
D3 recover! {:no-hard-links true} (the OLD name) :break-path = :link
D4 begin! {:unsafe-break-by-move true} :lock-broken = {... :tombstone "LOCK.broken.B1", :break-path :move}
```

Round six: an identical `:lock-broken` line whichever primitive ran, and `:no-hard-links true`
selected the measured check-then-act path. Round seven: both public callers publish the path, and
the retired word no longer selects anything (D3 => `:link`).

### Minor 2 — the retention stamp is settable and orphan sidecars are invisible — **CLOSED**

`txn_journal.clj:562-580` (`broken-lock-stamp-tolerance-ms`, 60000), `:805-806` (a stamp at or
past `now + tolerance` is `:stamp :unreadable` and the basis falls back to the file's own),
`:821` (`(max 0 ...)`, the age clamped), `:625-640` (`orphan-sidecar-files`), `:884-886` (orphans
read BEFORE the tombstone prune), `:918` (`:orphan-sidecars` bucket), `:2345-2357` (the
`:kind :orphan-sidecar` row).

**Witness (probes B2/B3/O1, `p1_minors.clj`) — my round-six forgeries, re-run, each also driven
past the retention with `:now-ms`:**

```
B2 forged-future (10y ahead) row = ({:txid "LOCK.broken.recover-1788466294442-00af6a9e", :stamp :unreadable, :status :lock-broken, :kind :broken-lock, :bytes 80, :age-ms 2})
B2 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
B2 with now-ms +25h :broken-locks = {:found 1, :pruned 1, :remaining 0, ...}
B2 tombstone after +25h sweep? = false

B3 forged-ancient row = ({:txid "LOCK.broken.recover-1788466294462-1eb0b92d", :stamp :ok, :bytes 80, :age-ms 1})
B3 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, ...}
B3 tombstone survives forged-ancient stamp? = true

O1 tombstone removed by hand; tombs = ()  sidecars = ("LOCK.broken-at.recover-1788466294477-1dd660b0")
O1 rows = ({:txid "LOCK.broken-at.recover-1788466294477-1dd660b0", :stamp :ok, :status :orphan-sidecar, :kind :orphan-sidecar, :bytes 125, :age-ms 2})
O1 recover! :broken-locks = {... :orphan-sidecars {:found 1, :pruned 0, :remaining 1} ...}
O1 with now-ms +25h :broken-locks = {... :orphan-sidecars {:found 1, :pruned 1, :remaining 0} ...}
O1 orphan sidecar still on disk = ()
```

Round six: `:age-ms -315360000000`, `:pruned 0`, a file nothing would ever retire, and an orphan
sidecar `retained rows = []` / `{:found 0}` / still on disk. Round seven: the age is a number a
clock produced, the stamp says it was not read, the file retires on its own basis, and the orphan
is a row, a count and a retirement. B3 confirms the fail-safe direction is preserved: an ancient
forged stamp still does NOT prune a fresh tombstone.

### Minor 3 — a zero-length tombstone is named, counted, and denied by the listing — **CLOSED**

`txn_journal.clj:747-773` (`evidence-stat`, ONE `Files/readAttributes` at `:763-766` binding size
to the same observation as the times), `:2329` (`:when (map? aged)`, absence rather than
zero-bytes as the filter), `:2333-2335` (`:status :empty-evidence`).

**Witness (probe C0, `p1_minors.clj`) — my round-six probe, re-run:**

```
C0 LOCK length = 0
C0 recover! :lock-broken  = {:reason :stale-holder, :cause :no-recorded-holder, :pid nil, :holder-txid nil, :broken-at "2026-09-03T20:11:34.486385533Z", :tombstone "LOCK.broken.recover-1788466294485-155cdcf4", :content-sha256 "e3b0c44...b7852b855", :break-path :link}
C0 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
C0 tombstones on disk = ("LOCK.broken.recover-1788466294485-155cdcf4")
C0 tombstone lengths  = [0]
C0 rows = ({:txid "LOCK.broken.recover-1788466294485-155cdcf4", :stamp :ok, :status :empty-evidence, :kind :broken-lock, :bytes 0, :age-ms 4})
```

Round six: `retained-transactions => []` beside a receipt naming the file and `:remaining 1`.
Round seven: three verbs agree on one file, and the row says what the file is.

The builder's disclosure is confirmed and is the more important half: the `(pos? bytes)` guard —
not the age computation — was what kept the 822-ghost class out, so removing it required absence
to be decided at the source. It is: `evidence-stat` returns nil only on
`NoSuchFileException`, and every row is built from one observation. Measured under concurrency in
attack (d) below: **0** rows of the old fabricated shape in 34,920.

### Minor 4 — an interrupted break is typed by a neighbour that can leave, and only the first — **CLOSED (both stated shapes), with a narrowed residual**

`txn_journal.clj:715-737` (`mark-break-linked!`), `:739-745` (`break-phase`), `:831-859`
(`interrupted-break-files`, `filterv` at `:856` over BOTH rules at `:857-858`), `:2623`
(`recover!` maps over every one), `:2668` (`:interrupted-breaks`, a vector).

**Witness (probes A / M1, `/tmp/txn7-fx-sol/p2_interrupted.clj`) — my round-six probes, re-run
against the vector key:**

```
A sidecar content = "{:tombstone \"LOCK.broken.BREAKER-A\", :phase :linked, :linked-at-ms 1788466348184}"
A LOCK key = (dev=24,ino=26847504)  tomb key = (dev=24,ino=26847504)
A rows BEFORE the holder releases = [{:txid "LOCK.broken.BREAKER-A", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :absent, :bytes 128}]
A release-lock! by the holder = true
A rows AFTER the holder released = [{:txid "LOCK.broken.BREAKER-A", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :absent, :bytes 128}]
A recover! :interrupted-breaks = [{:tombstone "LOCK.broken.BREAKER-A", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid nil, :holder-live false, :lock-present false, :cause :lock-is-no-longer-the-linked-claim}]

M1 tombs = ["LOCK.broken.BRK-A" "LOCK.broken.BRK-B"]
M1 both share the LIVE LOCK inode? = [true true]
M1 rows = [{:txid "LOCK.broken.BRK-A", :kind :interrupted-break, :status :lock-break-interrupted} {:txid "LOCK.broken.BRK-B", :kind :interrupted-break, :status :lock-break-interrupted}]
M1 ONE recover! :interrupted-breaks = [{:tombstone "LOCK.broken.BRK-A", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid "LIVE", :holder-live true, :lock-present true} {:tombstone "LOCK.broken.BRK-B", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid "LIVE", :holder-live true, :lock-present true}]
M1 tombs after ONE recover! = []  sidecars = []
M1 LOCK present = true
M1 LOCK content = "{:txid \"LIVE\", :lock-format 2, :pid 3833233, :start-ticks 1788466346370, :boot-id \"e5f1df7e-...\"}"
```

Round six (probe A): after the holder's clean release the row re-typed to
`:kind :broken-lock :status :lock-broken` with `:broken-locks {:found 1 :remaining 1}` and no
resolution. Round seven: the marker outlives the neighbour and the row does not move. Round six
(probe M1): the listing typed the first and published the second as a real break; two `recover!`
calls were needed. Round seven: both typed, both resolved in ONE call, the live holder's claim
byte-identical afterwards.

The residual is the window the marker itself opens, and it is finding 2 below.

---

## Round seven's own additions, attacked

### (a) the `:phase :linked` marker window

`break-by-link!`'s order is `Files/createLink` (`:1057`), `mark-break-linked!` (`:1061`), the
recheck (`:1062-1073`), `stamp-broken-at!` (`:1078`, which OVERWRITES the marker), the LOCK unlink
(`:1079`), `touch-tombstone!` (`:1080`).

**Crash BEFORE the marker is written, after `createLink` (link exists, no marker): typed
`:interrupted-break` while the LOCK is there, and re-typed `:broken-lock` the moment the holder
releases.** The inode rule at `:858` carries it until the neighbour leaves — which is exactly the
round-six defect, surviving in a window narrowed from the whole recheck to one `spit`.

**Witness (probe AW1, `p2_interrupted.clj`) — the state a crash in that window leaves, built by
hand because there is no seam between those two statements:**

```
AW1 link exists, NO marker; tombs = ["LOCK.broken.WINDOW-1"]  sidecars = []
AW1 rows with the LOCK still there = [{:txid "LOCK.broken.WINDOW-1", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :absent, :bytes 128}]
AW1 release-lock! = true
AW1 rows AFTER the holder released = [{:txid "LOCK.broken.WINDOW-1", :kind :broken-lock, :status :lock-broken, :stamp :absent, :bytes 128}]
AW1 recover! :interrupted-breaks = nil
AW1 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
```

The same shape is reachable WITHOUT a crash inside a two-statement window, because `spit`
truncates before it writes: a process killed mid-`spit` leaves a zero-length marker, and
`break-phase` (`:745`) returns nil for it — `read-string ""` throws and is caught.

**Witness (probe AZ, `p2_interrupted.clj`):**

```
AZ zero-length marker; break-phase = nil
AZ rows with LOCK present = [{:txid "LOCK.broken.ZL", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :unreadable, :bytes 128, :age-ms 1}]
AZ rows after the holder released = [{:txid "LOCK.broken.ZL", :kind :broken-lock, :status :lock-broken, :stamp :unreadable, :bytes 128, :age-ms 0}]
```

**Crash after the marker is cleared but before the unlink (marker gone, two links): typed
correctly, by the inode rule, and resolved.** The docstring at `:1074-1077` predicts this and it
holds.

**Witness (probe AW2):**

```
AW2 two links, stamp written, LOCK NOT unlinked
AW2 rows = [{:txid "LOCK.broken.WINDOW-2", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :ok, :bytes 128}]
AW2 recover! :interrupted-breaks = [{:tombstone "LOCK.broken.WINDOW-2", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid "LIVE-HOLDER", :holder-live true, :lock-present true}]
AW2 LOCK present = true  tombs = []
```

**Does any ordering yield a false "real break"? YES — two of them**, both requiring the holder to
release cleanly afterwards: AW1 (crash before the marker lands) and AW3 (stamped, then released
before the unlink):

```
AW3 stamped, then holder released cleanly; rows = [{:txid "LOCK.broken.WINDOW-3", :kind :broken-lock, :status :lock-broken, :stamp :ok, :bytes 128}]
AW3 recover! :interrupted-breaks = nil
AW3 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, ...}
```

AW3 is arguably correct — the stamp was written, so the break's own record says it happened —
but the LOCK it names was released by its owner rather than taken, so the evidence still
describes something that did not occur. AW1 is not arguable: nothing was stamped and nothing
happened.

**And the ordering the docstring says is impossible is reachable, in the OTHER direction, with
consequences that are worse.** `mark-break-linked!` claims at `:729-732` that the marker is
"cleared by `stamp-broken-at!` BEFORE the unlink, so a crash between the unlink and the stamp can
never leave a completed break wearing this marker." That is true of a CRASH and false of a FAILED
WRITE: `stamp-broken-at!` swallows its own exception at `:674` and returns `now` at `:675`
regardless, so `break-by-link!` proceeds to `Files/deleteIfExists (.toPath lock)` at `:1079` with
the `:phase :linked` marker still on disk. A completed break then wears the marker — and
`resolve-interrupted-break!` REVERTS it, deleting the sidecar at `:2537` and the tombstone at
`:2538`.

**Witness (probe AI, `/tmp/txn7-fx-sol/p3_attacks.clj`):**

```
AI a COMPLETED break (LOCK gone) still wearing :phase :linked
AI LOCK present = false  tombs = ["LOCK.broken.REAL-BREAK"]
AI rows = [{:txid "LOCK.broken.REAL-BREAK", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :absent, :bytes 84, :age-ms 6}]
AI recover! :interrupted-breaks = [{:tombstone "LOCK.broken.REAL-BREAK", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid nil, :holder-live false, :lock-present false, :cause :lock-is-no-longer-the-linked-claim}]
AI recover! :broken-locks = {:found 0, :pruned 0, :remaining 0, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
AI tombs AFTER recover! = []  <== real break evidence
```

**It is also reachable by an ordinary writer of the transactions directory** — the threat model the
builder ACCEPTED one commit earlier when it made a forgeable stamp `:unreadable` at `8151dbd`.
A genuine break, then one forged sidecar, then the next `recover!`:

**Witness (probe FORGE, `/tmp/txn7-fx-sol/p4_race.clj`):**

```
FORGE a REAL break happened: {:tombstone "LOCK.broken.recover-1788466487404-00ca6c31", :break-path :link}
FORGE evidence on disk = ["LOCK.broken.recover-1788466487404-00ca6c31"]
FORGE row after the forged marker = [{:txid "LOCK.broken.recover-1788466487404-00ca6c31", :kind :interrupted-break, :status :lock-break-interrupted, :stamp :absent, :bytes 80, :age-ms 8}]
FORGE next recover! :interrupted-breaks = [{:tombstone "LOCK.broken.recover-1788466487404-00ca6c31", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid nil, :holder-live false, :lock-present false, :cause :lock-is-no-longer-the-linked-claim}]
FORGE next recover! :broken-locks = {:found 0, :pruned 0, :remaining 0, ...}
FORGE evidence on disk AFTER = []
```

**The direction is the finding.** Round six's forged stamp made a file PERMANENT — fail-safe, and
the builder still fixed it. Round seven's forged marker makes a file DISAPPEAR — fail-open, in the
directory whose whole purpose on this branch is that evidence is bounded, counted and kept. And
there is no witness: `grep -n ':phase' test/clj_surgeon/txn_journal_test.clj` matches nothing, and
MEM-013 carries no falsifier row for "a `:phase :linked` marker is the interrupted break."

*Fix:* when `interrupted-break-files` matches on the MARKER ALONE and the LOCK is gone or names a
different inode, the marker cannot be corroborated by anything — so the safe resolution is to
STAMP AND KEEP (treat it as a completed break, `:evidence :retained`, typed
`:cause :marker-uncorroborated`), never to delete. Reserve the revert for a match the inode rule
also confirms, which is the case where the LOCK is provably still the linked claim. Additionally:
`stamp-broken-at!` must not report success it did not achieve — return nil on a failed write and
have `break-by-link!` refuse the unlink rather than complete a break it cannot record.

### (b) the `:stamp :unreadable` fallback to the file basis — early prune or accumulation?

**NEITHER.** The fallback basis is `evidence-stat`'s `(max mtime ctime)` at `:770-771`, and ctime
cannot be set through the filesystem API — every restore path (`cp -p`, `tar -x`, `rsync -t`)
creates a new inode with a FRESH ctime while preserving mtime, so the basis is bounded below by
the moment of the restore. An unreadable-stamp tombstone therefore re-ages from its restore and
retires 24 h later, which is late by the restore interval and never early. B2 above proves the
retirement actually happens (`:pruned 1` at `+25h`).

**Witness (probe B, `p3_attacks.clj`) — an unreadable stamp plus the `cp -p` back-date, which is
exactly the forgery the sidecar was introduced to defeat:**

```
B stamp 1h ahead -> row = [{:txid "LOCK.broken.recover-1788466425551-56f3be2b", :kind :broken-lock, :status :lock-broken, :stamp :unreadable, :bytes 80, :age-ms 2}]
B mtime back-dated 30d, row = [{... :stamp :unreadable, :bytes 80, :age-ms 1}]
B recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
B tombstone survives the back-date? = true
B cp -p copy row = [{:txid "LOCK.broken.CP-COPY", :kind :broken-lock, :status :lock-broken, :stamp :absent, :bytes 80, :age-ms 1}]
```

A 30-day mtime back-date on an unreadable-stamp tombstone leaves `:age-ms 1` and `:pruned 0`.
No evidence loss, no permanence. Closed as designed.

### (c) `:evidence :removed` and the standing witness

`a-receipt-never-names-a-file-that-is-not-there`
(`test/clj_surgeon/txn_journal_test.clj:1695-1725`) walks every `:tombstone` value anywhere in the
receipt (`receipt-file-names`, `:1636-1652`) and asserts each names a file present at return. Its
current scenario plants a dead holder and drives a BREAK, so it never reaches a revert and it
passes (my gate run is green).

**Extended to drive a revert, it FAILS.**

**Witness (probe C, `p3_attacks.clj`) — the witness's own predicate over a receipt from an
interrupted break with a live holder:**

```
C receipt = {:interrupted-breaks [{:tombstone "LOCK.broken.REVERT-ME", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid "LIVE-HOLDER", :holder-live true, :lock-present true}], :broken-locks {:found 0, :pruned 0, :remaining 0, ...}}
C every :tombstone the receipt names = ["LOCK.broken.REVERT-ME"]
C ... of which MISSING at return = ("LOCK.broken.REVERT-ME")
C would `a-receipt-never-names-a-file-that-is-not-there` PASS here? = false
```

**Should it be extended? Yes — but not as written.** House rule 20 requires a receipt to name its
subject and its evidence, not that every named file survive; a revert that names the tombstone it
deleted and says `:evidence :removed` (`:2545`) satisfies the rule and is strictly better than
silence. The correct invariant is: **every `:tombstone` a receipt names either exists at return OR
carries an explicit `:evidence` key saying what became of it.** Extending the witness in that form
covers the revert, keeps the original assertion for breaks, and — importantly — would have caught
finding 1's fix if it had been written first, because a `:evidence :retained` resolution has to
leave the file there. As written today the witness is silent on the entire revert path, which is
where round seven's new deletion lives.

### (d) a file deleted BETWEEN `readAttributes` and the row's emission

**A stale row carrying REAL values — never a throw, never a fabricated one.** `evidence-stat`
(`:747-773`) performs one `Files/readAttributes` at `:763` and returns nil only on
`NoSuchFileException`; the row at `:2331-2342` is then built entirely from that single observation.
A delete landing after the stat therefore produces a row whose `:bytes` and `:age-ms` were both
genuinely measured on a file that existed — the honest TOCTOU tail of any listing — and never the
old `:bytes 0` / epoch-age shape that two separate reads manufactured.

**Witness (probe D2, `/tmp/txn7-fx-sol/p5_race2.clj`) — 4,000 tombstones with a churner deleting
and recreating them for the whole duration of 60 concurrent listings:**

```
D2 total rows across the concurrent listings = 34920
D2 rows naming a file ABSENT at check time   = 6942
D2 rows with an EPOCH-sized age (the old ghost signature) = 0
D2 rows with :bytes 0                        = 0
D2 recover! :broken-locks = {:found 431, :pruned 0, :remaining 431, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
```

**0 of 34,920** rows carry the fabricated shape — the fix does exactly what it claims. The 6,942
absent-at-check-time rows are staleness, not falsity, and the same run's `:vanished` bucket in
`prune-broken-locks!` (`:916`) is the path that types absence when it matters. The one thing to
note is that the standing witness's second assertion — "the sweep lists no row for a file that is
not there" — is a single-threaded truth, and should not be strengthened into a concurrent one.

### (e) is the name `:unsafe-break-by-move` enough?

**Yes, on this build.** `grep -n 'getenv\|System/getProperty' src/clj_surgeon/txn_journal.clj`
returns NOTHING — the kernel reads no environment variable and no system property. There is no
config file on any path into `opts`: `break-lock!` reads the key straight from its `opts` argument
(`:1143`), which arrives from `acquire-lock!` (`:974`) from `begin!` (`:1061-1069`) or from
`recover!` (`:2343`), all of them ordinary Clojure arguments. And there is no MCP surface at all:
no `:txn/recover` operation is registered in `src/` (the two matches at `:1224` and `:1242` are
hint strings inside refusal payloads), and the only cross-namespace reference to the kernel is a
require in `scope_stream.clj:24`.

So the only way to reach the check-then-act path where `link(2)` works is for a Clojure caller to
type the literal keyword `:unsafe-break-by-move`. **The caveat to carry forward:** this holds
because adoption is zero. The moment an MCP operation passes a caller-supplied argument map
through to `begin!`/`recover!`, the word stops being a barrier — an unrecognised-key refusal or an
explicit allowlist at that boundary is what will be needed then, not the name.

### (f) a 61-second clock skew on a shared filesystem

**Every tombstone written by the skewed host reads `:stamp :unreadable`, silently, and NOTHING
counts them.** The predicate at `:805` is a hard cliff at exactly `broken-lock-stamp-tolerance-ms`,
and `prune-broken-locks!` returns no stamp bucket at all (`:913-919`): `:found`, `:pruned`,
`:remaining`, `:vanished`, `:interrupted`, `:orphan-sidecars`, `:retention-ms`. The `:stamp` key
exists only per-row in `retained-transactions` (`:2340`, `:2355`), which a caller must go and read.

**Witness (probe F / F59, `p3_attacks.clj`) — the skewed host wrote BOTH halves from its own
clock:**

```
F 61s-skewed tombstone row = [{:txid "LOCK.broken.recover-1788466425587-2957a8ce", :kind :broken-lock, :status :lock-broken, :stamp :unreadable, :bytes 80, :age-ms 0}]
F recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :retention-ms 86400000}
F is :stamp :unreadable counted in ANY bucket above? (keys) = (:found :pruned :remaining :vanished :interrupted :orphan-sidecars :retention-ms)

F59 59s-skewed row = [{:txid "LOCK.broken.recover-1788466425599-0c7ceb7d", :kind :broken-lock, :status :lock-broken, :stamp :ok, :bytes 80, :age-ms 0}]
```

The consequence is benign — the fallback basis is the same host's future mtime, so the age clamps
to 0 and the tombstone retires 61 s late rather than early — but the REPORTING is the defect. The
docstring at `:576-580` asserts "a minute is wider than any skew between two writers of one
directory on one host," which is true and is not the deployment being asked about: two hosts
sharing an NFS mount routinely drift further, and every tombstone in that directory then reads
`:stamp :unreadable` with no count anywhere telling an operator that the stamp mechanism has
stopped working. This is the same defect class as the `:vanished` bucket that round five's 822
ghost rows produced: a condition that is typed per-row and absent from the standing count.
*Fix:* add `:unreadable-stamps` to `prune-broken-locks!`'s bucket, on the same terms as
`:orphan-sidecars` — a non-zero value is an alarm about a clock, not an archive.

---

## Numbered findings (round seven's own)

1. **OPEN (must fix before adoption) — a COMPLETED break wearing `:phase :linked` is REVERTED, and
   its evidence deleted.** `txn_journal.clj:674-675` (`stamp-broken-at!` swallows its write failure
   and returns `now` regardless), `:1078-1079` (`break-by-link!` unlinks the LOCK on that return
   value), `:857` (`interrupted-break-files` matches on the marker alone), `:2537-2538`
   (`resolve-interrupted-break!` deletes the sidecar and then the tombstone), `:2550`
   (`:cause :lock-is-no-longer-the-linked-claim`). Measured two ways: probe AI drove the state a
   swallowed stamp failure leaves and watched `recover!` delete a real break's evidence with
   `:broken-locks {:found 0}`; probe FORGE reached the same outcome on a genuine `recover!` break
   with ONE hand-written sidecar, under the same directory-writer threat model the builder accepted
   and fixed for the stamp at `8151dbd`. This is a DIRECTION regression, not a residual: round six's
   worst forgery kept a file for ever (fail-safe); this one removes it (fail-open), in the directory
   whose stated purpose is bounded, counted, kept evidence. `mark-break-linked!`'s docstring at
   `:729-732` asserts the ordering makes it impossible, which is true of a crash and false of a
   failed write. No witness exists (`:phase` appears nowhere in the test file) and MEM-013 has no
   falsifier row for it. *Fix:* a marker-only match with the LOCK gone or naming a different inode
   is UNCORROBORATED — stamp and keep it, typed, never delete; reserve the revert for matches the
   inode rule confirms. And make `stamp-broken-at!` return nil on a failed write so
   `break-by-link!` refuses an unlink it cannot record.

2. **OPEN (minor) — the marker window still yields a false "real break."**
   `txn_journal.clj:1057` (`createLink`) to `:1061` (`mark-break-linked!`), plus `:745`
   (`break-phase` returns nil for a zero-length marker, because `spit` truncates before it writes).
   Measured: probe AW1 — no marker, LOCK present, correctly `:interrupted-break`; holder releases
   cleanly; row becomes `:kind :broken-lock :status :lock-broken` with
   `:broken-locks {:found 1 :remaining 1}` and `:interrupted-breaks nil`. Probe AZ reproduces it
   from a truncated marker with no crash-in-a-two-statement-window required. This is round six's
   finding 4(i) narrowed from the whole recheck to one `spit`, not eliminated. Probe AW3 is the
   milder cousin: stamped, then released before the unlink, published as a real break of a claim
   its owner gave up. *Fix:* write the marker BEFORE `createLink` (a marker with no tombstone is
   an orphan sidecar, which round seven already lists and retires), so the window contains no state
   the two rules cannot both see.

3. **OPEN (minor) — the standing witness is silent on the entire revert path.**
   `test/clj_surgeon/txn_journal_test.clj:1695-1725` with `receipt-file-names` at `:1636-1652`.
   Measured (probe C): extended to drive a revert it FAILS —
   `:tombstone "LOCK.broken.REVERT-ME"` named, missing at return. It should be extended, but as
   "exists at return OR carries an explicit `:evidence` key saying what became of it," which the
   revert's `:evidence :removed` (`:2545`) satisfies and which would have constrained finding 1's
   fix. As written it asserts nothing about the one path round seven added a deletion to.

4. **OPEN (minor) — `:stamp :unreadable` is typed per row and counted by nothing.**
   `txn_journal.clj:805-806` (the cliff), `:913-919` (the bucket, which has no stamp field),
   `:2340`/`:2355` (the only place the fact appears). Measured (probes F/F59): 61 s of skew =>
   every row `:stamp :unreadable` with `:broken-locks` unchanged; 59 s => `:stamp :ok`. The
   docstring's justification at `:576-580` is scoped to "two writers of one directory on one host"
   and the question is two hosts on one filesystem. Harmless to retention (the age clamps to 0 and
   the file retires late, never early) and invisible to an operator. *Fix:* an `:unreadable-stamps`
   count in the sweep bucket, on the same terms as `:orphan-sidecars`.

5. **NOTE (accepted, and the fix is confirmed non-vacuous) — the listing is a snapshot with a
   TOCTOU tail.** `:747-773` (one `readAttributes`), `:2329` (`:when (map? aged)`). Measured
   (probe D2): 6,942 of 34,920 rows named a file absent at check time under a churner, and **0**
   carried the old fabricated `:bytes 0` / epoch-age shape. Staleness, not falsity; inherent to any
   listing; the `:vanished` bucket at `:916` is where absence is typed when it matters. The
   builder's disclosure that the `(pos? bytes)` guard was doing the ghost-row work is correct and
   is now properly replaced at the source.

6. **NOTE (accepted) — the reverted-break disclosure is honest and the receipt is better than the
   alternative.** `:2545` `:evidence :removed`, `:2534` `:evidence :retained`. A receipt that names
   a file it deleted AND says so satisfies house rule 20; a receipt that stayed silent about the
   deletion would not. See finding 3 for the witness that should encode this.

7. **NOTE (out of scope, unchanged) — the terminal record is still two writes with different
   durability.** `:310-315` fsyncs through `sync-stream!`; `:317-323` is a plain `spit`. Safe
   because `state.edn` is the authority. Round-three findings 6, 8, 9 and 10 remain OPEN and
   adoption-scoped.

---

## Verdict

# GO-WITH-FIX

*For merging.* All four of round six's minors are closed against my own re-run probes, and three
are closed structurally: `:break-path` now rides both public receipts and the fallback needs a word
nobody types by accident (D1-D4); a stamp ten years ahead is `:unreadable`, the age is a number a
clock produced, and the file actually retires at `+25h` where round six kept it for ever (B2);
orphan sidecars are a row, a count and a retirement (O1); a present zero-length tombstone is
`:status :empty-evidence` and three verbs agree on it (C0); and the interrupted-break typing now
survives the holder's clean release and resolves EVERY match in ONE call (A, M1). The gates match
the builder's counts exactly — 720/5976/0, 463/4492/0, oracle pass, 0 warnings — and I re-derived
adoption at zero, so merging cannot break anything that exists.

*Why not GO.* Round seven's own fix opened a hole its predecessor did not have. The `:phase
:linked` marker is the first mechanism on this branch whose forgery DELETES evidence rather than
retaining it, and I reached that outcome twice: from a swallowed write failure inside
`stamp-broken-at!`, and from one hand-written sidecar dropped next to a genuine break by any writer
of the transactions directory — the exact threat model the builder accepted and fixed for the stamp
one commit earlier. The claim is never at risk in any shape I drove, and the LOCK is never touched
wrongly, so this is not a blocker for a latent kernel with no call sites. It is a blocker for
adoption, it has no witness, and MEM-013 has no falsifier row for it.

*Why not NO-GO.* Nothing regressed that a caller can observe today, no claim is lost, the fallback
direction of every OTHER mechanism on the branch is still fail-safe, and the finding's fix is three
lines plus a witness. Holding a branch that closed four findings and re-blessed four gates, over a
defect in a code path nothing calls, would cost more than it buys.

**Numbered, for the merge queue:**

1. Merge `bridge/txn-journal` at `11c7377`. Gates verified independently: test-fast 720/5976/0,
   mcp-test 463/4492/0, oracle pass, warning check 0, adoption zero.
2. **Before ANY adoption of the kernel — finding 1.** A marker-only interrupted-break match whose
   LOCK is gone or names a different inode is uncorroborated: stamp and keep it, typed, never
   delete. Reserve the revert for matches the inode rule also confirms.
3. **Same fix, the other half.** `stamp-broken-at!` must return nil on a failed write, and
   `break-by-link!` must refuse the unlink rather than complete a break it could not record.
   A break that cannot write its own evidence is not a break that happened.
4. **A witness for finding 1**, red first: a completed break (LOCK unlinked) wearing a `:phase
   :linked` sidecar survives `recover!` with `:evidence :retained`. Nothing in the suite matches
   `:phase` today.
5. **A MEM-013 falsifier row** for "a `:phase :linked` marker is the interrupted break" — the
   marker is a claim by the breaker, and a claim any directory writer can forge is not evidence.
6. **Finding 2:** write the marker BEFORE `Files/createLink`, so the window between the two rules
   contains no state neither can see. An early marker with no tombstone is an orphan sidecar, which
   this branch already lists and retires.
7. **Finding 3:** extend `a-receipt-never-names-a-file-that-is-not-there` to "exists at return OR
   carries an explicit `:evidence` key," and drive it through a revert as well as a break.
8. **Finding 4:** add `:unreadable-stamps` to `prune-broken-locks!`'s bucket. A stamp mechanism
   that has stopped working across a skewed pair of hosts is currently visible only row by row.
9. **Carry forward, at the adoption boundary, not now:** `:unsafe-break-by-move` is protected by
   its name only because no MCP operation passes caller arguments into `begin!`/`recover!`. When
   one does, that boundary needs an allowlist or an unrecognised-key refusal — the word will stop
   being a barrier the day an argument map reaches the kernel.
10. Round-three findings 6, 8, 9 and 10 remain OPEN and adoption-scoped, unchanged.
