# Kernel round 6 — Opus executed re-check of bridge/txn-journal at 9aa5baa (2026-09-03T19:18Z)

Verdict: **GO-WITH-FIX** — first kernel round with NO blocker. Round-5 items all CLOSED on the reviewer's own probes (20,000-break storm: hammer got 1 window, 0 clobbered; 4,000-race: 0 destroyed, 3,974 typed `:tombstone-exists`; two-day-old lock broken → `:pruned 0`, tombstone + sidecar + row `:age-ms 17`; 0 of 2,221 ghost rows, `:vanished 29`). Four minors: `:no-hard-links` opt reachable from public verbs and the receipt does not say which break path ran; retention stamp settable (a sidecar 10 years ahead → negative age, never retired; fail-safe direction) and orphan sidecars unlisted/unpruned; zero-length tombstone named by the receipt but denied by the listing; interrupted-break typing depends on the shared LOCK still existing and handles only the first. Adoption still zero (kernel is latent). Round 7 launched.

## Opus verdict, verbatim

# txn-journal round 6 (`9aa5baa`) — Opus executed re-review: **GO-WITH-FIX**

**Reviewer of record.** Opus, fifth consecutive round. Round-five verdict under re-judgement:
`git show origin/main:docs/observations/2026-09-03-txn-journal-round5-rereview-opus.md`.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn6-wt` at `9aa5baa` (`git rev-parse --short HEAD`
=> `9aa5baa`; `git status --porcelain` empty at start and at end; nothing committed, stashed or
pushed). Fixtures under `/tmp/txn6-fx-sol` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M <probe>` from the worktree. Gates run once each
under `/home/forge/bin/suite-run`; `make mcp-test` was never invoked, `make memory-battery` and
`make memory-red` were NOT run, no port in 7888-7895 or 7906 was contacted, and every process
signalled was one I started.

**Headline.** All three of round 5's live findings are CLOSED against my own probes, and two of
them are closed *structurally* rather than narrowed: the link-first break removes the displacement
window entirely (my 20,000-break storm gave the hammer **1** chance to create a third-party claim,
where round 5 gave it 9,948), and the 4,000-race that destroyed 13 judged claims in round 5
destroyed **0** with 3,974 typed `:tombstone-exists` refusals. The tombstone that round 5 watched
`recover!` delete in the same call it minted survives, with a sidecar and a listing row. What is
left is three receipt-accuracy residuals of the class this branch keeps producing, none of which
loses a claim: a zero-length tombstone that the break's own receipt names and the listing denies;
a sidecar stamp that any writer of the directory can set into the future, publishing a negative
`:age-ms` and retaining the file for ever; and `:no-hard-links true`, which is an ordinary opt on
the **public** `begin!`/`recover!` surface — not a test-only seam — selecting the measured
check-then-act path with no field in any public receipt saying so.

---

## Executed gates (mine, at `9aa5baa`, each once under `/home/forge/bin/suite-run`)

```
$ /home/forge/bin/suite-run bash -c 'cd /home/forge/tmp/sol/txn6-wt && bb test/run_all.clj'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.
TESTFAST_EXIT=0

$ /home/forge/bin/suite-run bash -c '... swipl -q -f test/mcp_operation_contract_oracle.pl ...
                                     clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test ...
                                     clojure -M test/kernel_warning_check.clj'
ORACLE_EXIT=0
Ran 457 tests containing 4436 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
kernel warning check: 2 namespace(s), 0 warning(s)
KWC_EXIT=0
```

Builder's stated counts: test-fast 720/5976/0, mcp-test 457/4436/0, oracle pass, warning check 0.
All four match exactly.

**Adoption, re-derived at `9aa5baa`.** `grep -rn 'with-cooperating-writes' src/` returns the
definition (`txn_journal.clj:1024`) and one docstring mention (`file_ops.clj:24`) — zero call
sites. The only cross-namespace use of the kernel is `scope_stream.clj:24`. No `:txn/recover`
operation is registered in `src/`. Merging cannot break anything that exists today.

---

## Round-five items 1-4, re-judged on my own re-run probes

### Item 1 — the restore is a check-then-act and can destroy a third acquirer's live claim — **CLOSED (structurally)**

`txn_journal.clj:850` (`Files/createLink (.toPath tomb) (.toPath lock)` — the tombstone NAME is
claimed first), `:852-857` (content AND `(dev,ino)` of the link AND `(dev,ino)` of the LOCK),
`:861` (`Files/deleteIfExists` of the LOCK, after the proof), `:871-873` (a claim that is not the
judged one: `{:broken false :cause :holder-changed :restored true :restore-path :lock-never-left}`,
the LOCK untouched), `:919-921` (dispatch). `restore-lock!` (`:752-776`) is now reachable only
from `break-by-move!`.

**Witness (probe A1, `/tmp/txn6-fx-sol/p2_storm.clj`) — my round-five storm, re-run.** A hammer
thread creates a uniquely-named third-party claim by `Files/createLink` whenever `LOCK` is absent;
`break-lock!` runs 20,000 times with a deliberately mismatching claim:

```
A1 third-acquirer claims created = 1
A1 break outcomes = {:lock-vanished 146, :holder-changed 19854}
A1 tombstones left = 0
A1 claims SILENTLY CLOBBERED (created but on neither LOCK nor any tombstone) = 0
A1 displaced-claim-count = 0
A1 sample clobbered = ()
```

Round 4: 129 of 29,012 clobbered. Round 5: 0 of 9,948, but 9,947 tombstones and 9,947 counted
displacements. Round 6: **the hammer got 1 opportunity in 20,000 breaks**, because the LOCK never
leaves — there is no gap to land in. 0 tombstones, 0 displacements. The defect class is gone
rather than counted.

### Item 2 — `finish-after-throw!` un-commits a committed transaction — **CLOSED**

`txn_journal.clj:1436-1439` (the TERMINAL comment and `(swap! (:state txn) assoc :finished? true)`
after `append-journal!` `:1432` and `write-state!` `:1433-1435`), `:1751-1753`
(`finish-after-throw!` degrades to the bare `release-lock!` past the flag). Unchanged since round
5; re-run to confirm it survived the round-6 edits.

**Witness (probe D2, `/tmp/txn6-fx-sol/p4_misc.clj`), round four's exact injection —
`write-lease!` raising `IOException "ENOSPC writing lease.edn"`:**

```
D2 state.edn BEFORE commit = :sealed
D2 commit! threw        = java.io.IOException: ENOSPC writing lease.edn
D2 bytes on disk        = "(ns f0) (def v :NEW)\n" (= NEW) true
D2 state.edn AFTER      = {:txid TAIL, :workspace-root /tmp/txn6-fx-sol/ws4, :status :committed, :finished-at 2026-09-03T19:10:46.379379312Z, :retained true, :restore-failed false}
D2 journal.log tail     = committed
D2 LOCK present         = false
D2 next begin! ok       = true
D2 recover! =>          = {:ok true, :transactions-recovered 0}
D2 bytes AFTER recover! = "(ns f0) (def v :NEW)\n"
```

### Item 3 — contract sentences stronger than the code — **CLOSED**

Round 5 flagged two NEW sentences of this class. Both are now true against my own probes.

*(a)* `txn_journal.clj:899-902` and `docs/txn-journal.md:312` — "a name already present being that
primitive's own refusal, typed, taken BEFORE the LOCK is touched, never a replacement."

**Witness (probe D1, `/tmp/txn6-fx-sol/p2_storm.clj`) — my round-five 4,000-race, re-run.** Two
futures released from one `CyclicBarrier`, both calling `break-lock!` with txid `"SAME-TXID"` and
the same judged claim, with a hammer re-creating a LOCK:

```
D1 rounds                                  = 4000
D1 rounds where BOTH breakers moved a file  = 0
D1 :tombstone-exists refusals               = 3974
D1 :lock-vanished outcomes                  = 26
D1 rounds where the judged claim was DESTROYED = 0
D1 first destroyed sample = nil
```

Round 5: 13 destroyed of 4,000. Round 6: **0 of 4,000**, and the guard is `link(2)`'s EEXIST
(`:874-875`), not a stat this code performs.

*(b)* `txn_journal.clj:2364-2366` — "Pruned AFTER this run's own break, so a tombstone made a
moment ago is never retired by the recovery that made it." True now, because retention is measured
against the tombstone's own stamp (`stamp-tombstone!` `:602-632`, `tombstone-basis-ms` `:635-646`)
rather than the ordering. Witness under item 5 below (probe P6).

### Item 4 — `publish-staged-receipt!` outside the writer table — **CLOSED**

`docs/txn-journal.md:97` still carries the row naming
`intent_transaction.clj:2595-2601` as a raw `Files/move` (ATOMIC_MOVE + REPLACE_EXISTING) reaching
neither `atomic-write!` nor `atomic-create!`, called at `:2756`. Re-read both sites at `9aa5baa`;
unchanged and correct.

### Item 5 (round 5's blocker) — `recover!` deletes its own break's evidence — **CLOSED**

`txn_journal.clj:602-632` (`stamp-tombstone!`: `Files/setLastModifiedTime` at `:627` AND the
`LOCK.broken-at.<txid>` sidecar at `:628-631`), `:635-646` (`tombstone-basis-ms`, the `max` of the
file basis and the sidecar stamp at `:646`), `:648-657` (`tombstone-age-ms`, `:absent` at `:657`),
`:804`/`:814`/`:865` (the three stamp sites), `:2316` (the interrupted-finish stamp).

**Witness (probe P6, `/tmp/txn6-fx-sol/p1_prune.clj`) — my round-five probe, re-run: a crashed
holder whose LOCK is TWO DAYS old, i.e. far past the 24 h retention:**

```
P6 LOCK mtime age-ms before break = 172800002
P6 recover! :lock-broken   = {:reason :stale-holder, :cause :process-not-alive, :pid 999999, :holder-txid CRASHED-HOLDER, :broken-at 2026-09-03T19:07:21.552400483Z, :tombstone LOCK.broken.recover-1788462441548-2efc5d6e, :content-sha256 94e71e7e4c3c30393d3b05907eed815a0a9ff5bc7c0981b4f9ff41c8fbfcfc97}
P6 recover! :broken-locks  = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
P6 tombstones on disk AFTER the SAME recover! = (LOCK.broken.recover-1788462441548-2efc5d6e)
P6 sidecars  on disk AFTER the SAME recover!  = (LOCK.broken-at.recover-1788462441548-2efc5d6e)
P6 the tombstone recover! NAMED still exists? = true
P6 retained-transactions rows = [{:evictable false, :txid LOCK.broken.recover-1788462441548-2efc5d6e, :retention-ms 86400000, :retired-by :txn/recover, :status :lock-broken, :kind :broken-lock, :receipt-refs 0, :bytes 118, :age-ms 17}]
```

Round 5: `{:found 1 :pruned 1 :remaining 0}` beside a receipt naming a file that no longer existed.
Round 6: `:pruned 0`, the file is there, the sidecar is there, the row is there, `:age-ms 17`.

### Round-five finding 2 (two mtime reads / the 822 ghost rows) — **CLOSED**

`txn_journal.clj:695-698` (`prune-broken-locks!` folds through `tombstone-age-ms` and separates
`present` from `vanished`), `:715-716` (`:vanished` and `:interrupted` buckets), `:2093-2097`
(`retained-transactions` emits a row only `:when (and (number? age) (pos? (long bytes)))`).

**Witness (probe M2, `/tmp/txn6-fx-sol/p6_multi.clj`) — 3,000 tombstones with a concurrent remover
walking the same directory while a lister and `recover!` run:**

```
M2 rows a concurrent lister saw       = 2221
M2 rows with a NEGATIVE or epoch age  = 0
M2 rows with :bytes 0                 = 0
M2 recover! :broken-locks             = {:found 390, :pruned 0, :remaining 390, :vanished 29, :interrupted 0, :retention-ms 86400000}
```

Round 5: 822 of 9,831 rows were of files already gone, each billed `:bytes 0` with an epoch-sized
`:age-ms`, and `:remaining` was inflated by them. Round 6: **0 such rows**, and the 29 that
vanished under me are counted in their own bucket and excluded from `:found`.

Round 5's `cp -p` half is closed too. **Witness (probes B1/B4, `p1_prune.clj`)** — a real stamped
tombstone whose mtime is back-dated 30 days, once with its sidecar and once with the sidecar
deleted:

```
B1 sidecar deleted; sidecars now = ()
B1 after mtime back-dated 30d, retained rows = [{:txid LOCK.broken.recover-1788462441548-2efc5d6e, :age-ms 2}]
B1 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
B1 tombstones after = (LOCK.broken.recover-1788462441548-2efc5d6e)
B4 tombstone LOCK.broken.recover-1788462441603-2f78f77f mtime back-dated 30d, sidecar present = true
B4 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
B4 tombstone survives? = true
```

### Round-five finding 4 (a crash between `createLink` and the unlink leaves false evidence) — **PARTIAL**

`txn_journal.clj:659-672` (`interrupted-break-file`), `:693-694` (the prune excludes them),
`:2074`/`:2098-2101` (the listing types them `:kind :interrupted-break :status
:lock-break-interrupted`), `:2298-2327` (`recover!` resolves one before it decides anything else).
Round 5 asked for a docstring clause; the build shipped a resolution verb. The mechanism works —
and three residuals remain, all measured below in **attack (e)** and **finding 4**.

### Notes 5 and 6 — unchanged, as stated boundaries

*Note 5 (the ctime half can be kept fresh for ever, fail-closed direction).* `:423-447`
unchanged. Accepted.

*Note 6 (the terminal record is two writes with different durability).* `txn_journal.clj:310-315`
(`append-journal!` fsyncs through `sync-stream!`) versus `:317-323` (`write-state!` is still a
plain `spit`, no fsync, no atomic rename). Unchanged, correctly out of scope, and safe because
`state.edn` is the authority.

**Round-three findings 6, 8, 9 and 10 remain OPEN**, adoption-scoped and unchanged.

### Round-five item 6 (the legacy break's age basis) — **re-confirmed CLOSED and non-vacuous**

**Witness (probe L1, `/tmp/txn6-fx-sol/p3_interrupted.clj`), round four's own forgery:**

```
L1 fresh legacy lock: recover! --break-legacy-lock => {:broken-locks {:found 0, :pruned 0, :remaining 0, :vanished 0, :interrupted 0, :retention-ms 86400000}}
L1 LOCK still present = true
L1 after `touch` 11 days back: mtime age-ms = 950400000
L1 recover! --break-legacy-lock => {:broken-locks {:found 0, :pruned 0, :remaining 0, :vanished 0, :interrupted 0, :retention-ms 86400000}}
L1 LOCK still present = true  tombs = ()
```

And the refusal is not vacuous — **witness (probe L2, `/tmp/txn6-fx-sol/p5_ceiling.clj`)**, the
clock moved against the file's REAL basis, Sol's shape:

```
L2 one ms UNDER the ceiling     age=3599999 ceiling=3600000 broke? = false  LOCK present = true
L2 exactly AT the ceiling       age=3600000 ceiling=3600000 broke? = true   LOCK present = false
```

---

## Round 6's own additions, attacked

### (a) link-first break, a LIVE holder judged dead wrongly — is the holder's later release txid-scoped?

**Yes, on both halves, and I drove both.** `release-lock!` (`:517-528`) reads
`(lock-file transactions-dir)` — the name `LOCK` and nothing else — and unlinks only when the
claim it finds names the releasing txid. `broken-lock-files` (`:566-574`) selects on the
`LOCK.broken.` prefix, which the name `LOCK` cannot match, so no release path can reach a
tombstone by name; and there is no unguarded delete arity.

**Witness (probe A2, `p3_interrupted.clj`) — a LIVE holder (this process's own pid, start-ticks
and boot id) broken by a completed link-first break, then releasing:**

```
A2 break outcome = {:broken true, :tombstone LOCK.broken.BREAKER-4, :content-sha256 a03a128fc634be9debc3fe2cbb9f25c94771c57a17f8d7be6d34a8a312db8884, :broken-at-ms 1788462585250, :break-path :link}
A2 LOCK present after break = false  tombs = (LOCK.broken.BREAKER-4)
A2 the live holder now calls release-lock! => false
A2 tombstone survives the holder's release? = (LOCK.broken.BREAKER-4)  sidecars = (LOCK.broken-at.BREAKER-4)
A2 tombstone content =  "{:txid \"LIVE-JUDGED-DEAD\", :lock-format 2, :pid 2513552, :start-ticks 1788462583420, :boot-id \"e5f1df7e-bdc2-44b1-8312-50751b793d6c\"}"
```

The wrongly-broken holder's `release-lock!` returns `false` and the evidence of what was taken
from it survives intact. **And in the two-link window** (probe A, same file), where a release
*would* be operating on the shared inode:

```
A pre-release: tombs = (LOCK.broken.BREAKER-3)  LOCK present = true
A release-lock! with a DIFFERENT txid => false  tombs = (LOCK.broken.BREAKER-3)  LOCK = true
A release-lock! with the HOLDER's txid => true  tombs = (LOCK.broken.BREAKER-3)  LOCK = false
A the tombstone SURVIVED the holder's release? = true
```

**The residual the same probe exposes.** Once that live holder releases normally, the interrupted
tombstone stops being detectable — `interrupted-break-file` (`:659-672`) recognises it only by
sharing the live LOCK's inode — and the listing silently re-types it:

```
A ... and is it still typed :interrupted-break? = [{:txid LOCK.broken.BREAKER-3, :kind :broken-lock, :status :lock-broken}]
A recover! after the holder released = {:broken-locks {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}}
```

A break that never happened is then billed for 24 h as evidence of one, naming a claim whose owner
released the lock cleanly, with no `:interrupted-break` resolution in any receipt. See finding 4.

### (b) the sidecar — a tombstone with no sidecar, and a forged one

**No sidecar retains correctly, because the file basis is `lock-age-basis-ms` and ctime cannot be
forged.** `link(2)`, `rename(2)` and `setLastModifiedTime` all advance the inode's ctime, so a
tombstone made by either break path and then robbed of its sidecar still reads as young (probe B1
above: `:age-ms 2` after a 30-day mtime back-date, `:pruned 0`). Billed normally, with bytes and a
row. Old-format tombstones inherit the same protection: whatever produced them touched the inode.

**A forged sidecar can keep a tombstone for ever. It cannot prune one instantly.** The basis is
`(max file-basis sidecar-stamp)` at `:646`, which is the fail-safe direction for retiring and the
fail-open direction for retaining. **Witness (probes B2/B3, `p1_prune.clj`):**

```
B2 forged-future retained row = [{:txid LOCK.broken.FORGED-FUTURE, :age-ms -315360000000}]
B2 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
B2 tombstone survives? = true

B3 forged-ancient retained row = [{:txid LOCK.broken.FORGED-OLD, :age-ms 1}]
B3 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
B3 tombstone survives forged-ancient sidecar? = true
```

Two things fall out. The published bound is breakable — MEM-013 says recovery "bound[s] them by a
published retention age," and any writer of the directory (or a clock that steps forward once and
back) makes a file permanent. And the row publishes **`:age-ms -315360000000`**, a negative age no
clock produced, unclamped and untyped. Finding 2.

**Two smaller sidecar facts, both measured.** An orphan sidecar — one whose tombstone was removed
by anything but the prune or the revert — is invisible to every listing and to every sweep
(probe O1): `retained rows = []`, `:broken-locks {:found 0 …}`, `orphan sidecar still on disk =
true`. And a stale orphan sidecar is NOT inherited by a later tombstone of the same name on the
success path, because `stamp-tombstone!` overwrites it (probe O2: a `:broken-at-ms` 10 years in
the future was replaced, `retained row age-ms 3`); it *is* inherited by an interrupted break, which
never stamps.

### (c) zero-length skip — can a break lose the evidence?

**Not by a mid-write LOCK: that state does not exist in the kernel.** `write-lock!` (`:929-939`)
writes a temporary in full and gives it the LOCK's name by `Files/createLink`, so no reader ever
observes a half-written LOCK, and `grep -rn '"LOCK"' src/` returns exactly one site
(`txn_journal.clj:329`, `lock-file`) — there is no second, non-atomic LOCK writer anywhere in
`src/`.

**But an externally-created empty LOCK produces a receipt the listing denies.** `recover!` acts on
ANY cause (`:2337-2340`), including `:no-recorded-holder`, so it breaks an empty LOCK; the
tombstone is then zero-length, `prune-broken-locks!` bills it in `:found`/`:remaining`
(`:695-717` has no zero-length clause), and `retained-transactions` drops it at `:2096`.
**Witness (probe C0, `p1_prune.clj`):**

```
C0 LOCK length = 0
C0 recover! :lock-broken  = {:reason :stale-holder, :cause :no-recorded-holder, :pid nil, :holder-txid nil, :broken-at 2026-09-03T19:07:21.610239599Z, :tombstone LOCK.broken.recover-1788462441609-507b6bd4, :content-sha256 e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855}
C0 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :retention-ms 86400000}
C0 tombstones on disk     = (LOCK.broken.recover-1788462441609-507b6bd4)
C0 tombstone lengths      = [0]
C0 retained-transactions rows = []
```

No claim is lost — an empty LOCK carries none. What is lost is agreement: the break's receipt names
`LOCK.broken.recover-…`, `:broken-locks` counts it as remaining, and the listing MEM-013 requires
it be "visible to" reports nothing. The doc's rationale at `docs/txn-journal.md:242-246` argues the
absent case ("`lastModified` of a missing file is 0") and then applies it to a file that is
present. Finding 3.

### (d) the `:no-hard-links true` opt — reachable, and unnamed in the receipt

**Reachable from the public surface, not a test-only seam.** `break-lock!` reads it straight out of
`opts` at `:919`; `acquire-lock!` passes its caller's `opts` through at `:974`; `begin!`
(`:1061-1069`) passes the caller's whole options map to `acquire-lock!`; `recover!` passes its own
at `:2343`. **Witness (probe D, `p3_interrupted.clj`) — the public verb, one keyword:**

```
D recover! {:no-hard-links true} :lock-broken = {:reason :stale-holder, :cause :process-not-alive, :pid 999999, :holder-txid CRASHED, :broken-at 2026-09-03T19:09:45.271912169Z, :tombstone LOCK.broken.recover-1788462585270-50cd26c3, :content-sha256 ba543b2b67407ced50550cbe7ddd439086aa19196c74e6d99a9334e57826649a}
```

**The old move path still carries every round-4/5 witness**, which I drove one at a time:

```
D tombs = (LOCK.broken.recover-1788462585270-50cd26c3)  sidecars = (LOCK.broken-at.recover-1788462585270-50cd26c3)
D retained rows = [{:txid LOCK.broken.recover-1788462585270-50cd26c3, :kind :broken-lock, :age-ms 3, :bytes 111}]
D break-lock! direct, move path outcome = {:broken true, :tombstone LOCK.broken.MOVE-1, :content-sha256 673ec609111be5faf004773da8b9063029343f020b2b24f3cea130aa05ba1eae, :broken-at-ms 1788462585279, :break-path :no-hard-links}
D move-path tombstone stamped? sidecar = (LOCK.broken-at.MOVE-1)  retained rows = [{:txid LOCK.broken.MOVE-1, :age-ms 1}]
D move-path holder-changed + third acquirer = {:broken false, :cause :holder-changed, :restored false, :restore-cause :lock-reappeared, :tombstone LOCK.broken.MOVE-2}
D displaced delta = 1  tombs = (LOCK.broken.MOVE-2)  sidecars = (LOCK.broken-at.MOVE-2)
D move-path displaced tombstone retained rows = [{:txid LOCK.broken.MOVE-2, :age-ms 2, :bytes 113}]
```

Stamping (`:804`), the refused-restore stamp (`:814`), the sidecar, the listing row, the
`displaced-claims` increment and the typed `:restore-cause :lock-reappeared` all ride the fallback.
That half is right.

**What is wrong is that nothing above the kernel can tell the two paths apart.** `:break-path` is
produced at `:805` and `:866` and then discarded: `acquire-lock!` keeps only
`(select-keys outcome [:tombstone :content-sha256])` at `:978`, and `recover!` the same at `:2347`.
So `begin!` and `recover!` publish an identical `:lock-broken` line whether the break was the
kernel-atomic link or the check-then-act rename that this branch measured destroying 13 claims in
4,000 races — and `docs/txn-journal.md:328` describes the opt as "the seam its witnesses use."
Finding 1.

### (e) interrupted-break revert with a live holder — by name or by inode?

**By NAME, and it cannot reach the LOCK.** `recover!:2321-2322` deletes
`(broken-at-file tomb)` and then `tomb` — both `File` values built from `interrupted-break-file`'s
result, whose names are `LOCK.broken-at.<txid>` and `LOCK.broken.<txid>`. `lock-file` is the name
`LOCK` exactly, and `broken-lock-files` requires the `LOCK.broken.` prefix, so the two name spaces
are disjoint by construction: there is no argument to that `deleteIfExists` that can be the LOCK.
Name-based is also the correct choice here — an inode-based delete would be ambiguous between the
two links to the same inode, which is precisely the state being resolved.

**Witness (probes E1/E2, `p3_interrupted.clj`) — the crash driven through the `:before-unlink`
seam, once with a live holder and once with a dead one:**

```
E1 break outcome            = {:broken false, :cause :break-failed, :message crash between link and unlink}
E1 LOCK key = (dev=24,ino=24437955)  tomb key = (dev=24,ino=24437955)  same? = true
E1 retained rows = [{:txid LOCK.broken.BREAKER-1, :kind :interrupted-break, :status :lock-break-interrupted, :bytes 128}]
E1 recover! :interrupted-break = {:tombstone LOCK.broken.BREAKER-1, :resolution :interrupted-break-reverted, :holder-txid LIVE-HOLDER, :holder-live true}
E1 tombstones AFTER = ()  sidecars AFTER = ()
E1 LOCK still present? = true  content = "{:txid \"LIVE-HOLDER\", :lock-format 2, :pid 2513552, :start-ticks 1788462583420, :boot-id \"e5f1df7e-bdc2-44b1-8312-50751b793d6c\"}"
E1 LOCK txid preserved? = true

E2 recover! :interrupted-break = {:tombstone LOCK.broken.BREAKER-2, :resolution :interrupted-break-finished, :holder-txid DEAD-HOLDER, :holder-cause :process-not-alive}
E2 LOCK present after? = false
E2 tombs after = (LOCK.broken.BREAKER-2)  sidecars after = (LOCK.broken-at.BREAKER-2)
E2 retained rows after = [{:txid LOCK.broken.BREAKER-2, :kind :broken-lock, :status :lock-broken, :age-ms 8}]
```

Both directions are correct: the live holder's claim survives byte-for-byte, the dead holder's is
finished and stamped. Two residuals: the finish branch's own unlink (`:2316`) is the same
stat-then-unlink the builder discloses, and `interrupted-break-file` takes only the **first** match
(`:672`) while the prune excludes **all** of them (`:693`). **Witness (probe M1,
`p6_multi.clj`), two interrupted breaks at once:**

```
M1 tombs (both are second links to the LIVE LOCK) = (LOCK.broken.BRK-A LOCK.broken.BRK-B)
M1 retained rows = [{:txid LOCK.broken.BRK-A, :kind :interrupted-break, :status :lock-break-interrupted} {:txid LOCK.broken.BRK-B, :kind :broken-lock, :status :lock-broken}]
M1 recover! :interrupted-break = {:tombstone LOCK.broken.BRK-A, :resolution :interrupted-break-reverted, :holder-txid LIVE, :holder-live true}
M1 tombs after one recover!    = (LOCK.broken.BRK-B)
M1 recover! again              = {:interrupted-break {:tombstone LOCK.broken.BRK-B, :resolution :interrupted-break-reverted, :holder-txid LIVE, :holder-live true}, :broken-locks {...}}
M1 tombs after two recover!s   = ()
M1 LOCK present = true
```

The second interrupted tombstone is published as `:kind :broken-lock :status :lock-broken` — false
evidence of a break, naming the claim that currently holds the lock — until a second `recover!`
runs. Self-healing, and wrong while it lasts. Finding 4.

---

## Numbered findings (round 6's own)

1. **OPEN (minor) — `:no-hard-links true` is a public opt that silently selects the measured
   check-then-act break, and no receipt records which path ran.** `txn_journal.clj:919` (read
   straight from `opts`), `:974` and `:1061-1069` (`begin!` → `acquire-lock!` → `break-lock!`),
   `:2343` (`recover!`), against `:978` and `:2347`, where `select-keys` keeps only `:tombstone`
   and `:content-sha256` and drops the `:break-path` produced at `:805`/`:866`. Measured: probe D
   above drove the fallback through the public `recover!` with one keyword and got a `:lock-broken`
   line indistinguishable from the atomic path's. `docs/txn-journal.md:328` calls it "the seam its
   witnesses use." *Fix:* carry `:break-path` into `:lock-broken` in both callers, and either gate
   the opt behind an explicit test-only key or say in the docstring that a caller may select it.

2. **OPEN (minor) — the retention bound is settable by any writer of the transactions directory,
   and a bad stamp publishes a negative age.** `txn_journal.clj:646` (`max` of the file basis and
   the sidecar's `:broken-at-ms`), `:656` (the subtraction, unclamped), `:2094`/`:2104` (the row).
   Measured (probe B2): a sidecar stamped 10 years ahead gives `:age-ms -315360000000`,
   `:pruned 0`, and a file that is never retired — while MEM-013 promises evidence "bound … by a
   published retention age that recovery enforces." The direction is the safe one (probe B3: a
   forged ancient stamp does NOT prune a fresh tombstone), so this is a bound and a receipt-value
   defect, not a loss. *Fix:* ignore a sidecar stamp in the future (or clamp the age at 0 and type
   it), and count such tombstones in a named bucket rather than leaving them silently permanent.
   *Related, same file:* an orphan `LOCK.broken-at.*` whose tombstone is gone is listed by nothing
   and pruned by nothing (probe O1) — a small unbounded accumulation of the kind
   `broken-lock-retention-ms` exists to prevent.

3. **OPEN (minor) — a zero-length tombstone is named by the break's receipt, counted by
   `:broken-locks`, and denied by the listing.** `txn_journal.clj:2096`
   (`:when (and (number? age) (pos? (long bytes)))`) against `:695-717` (the prune has no
   zero-length clause) and `:2337-2347` (`recover!` breaks on any cause, `:no-recorded-holder`
   included). Measured (probe C0): `:lock-broken {… :tombstone "LOCK.broken.recover-…"}` and
   `:broken-locks {:found 1 :remaining 1}` beside `retained-transactions => []`. Unreachable from
   the kernel — `write-lock!` at `:929-939` never leaves an empty LOCK and `grep -rn '"LOCK"' src/`
   finds no other writer — so it needs a foreign or hand-made LOCK. MEM-013's "visible to the same
   listing that reads retained journals" is nonetheless false for that file, and
   `docs/txn-journal.md:242-246` justifies the skip with the *absent*-file argument applied to a
   present file. *Fix:* skip zero-length files in `prune-broken-locks!` too (so the two counts
   agree), or emit the row with `:bytes 0` and a `:claim :empty` marker; and either way stop
   naming a file in `:lock-broken` that the listing will not show.

4. **OPEN (minor) — an interrupted break is typed correctly only while the LOCK it shares an inode
   with is still there, and only for the first of them.** `txn_journal.clj:659-672`
   (`interrupted-break-file`, identity by inode, `first` at `:672`), `:693-694`, `:2074`,
   `:2098-2101`, `:2303-2327`. Two measured shapes. (i) Probe A: after the live holder's own
   `release-lock!`, the tombstone re-types from `:kind :interrupted-break` to
   `:kind :broken-lock :status :lock-broken` and `recover!` reports
   `:broken-locks {:found 1 :remaining 1}` with no resolution — a break that never happened billed
   as evidence for 24 h, naming a holder that released cleanly, and blocking a later break by that
   txid with `:tombstone-exists`. (ii) Probe M1: with two interrupted tombstones the listing types
   the first and publishes the second as a real break; two `recover!` calls are needed. *Fix:* map
   over every match rather than `first`, and record the interruption in the sidecar (a
   `:interrupted true` field written by `break-by-link!` before the unlink) so the state survives
   the LOCK it was inferred from.

5. **NOTE (accepted) — the finish branch's unlink is the same stat-then-unlink residual.**
   `txn_journal.clj:2314-2315`: `(when (= (lock-file-key tomb) (lock-file-key lock)) (Files/deleteIfExists (.toPath lock)))`.
   Identical in kind to `break-by-link!`'s disclosed window at `:852-861`, with no I/O between and
   entered only after the identity proof. Disclosed by the builder; no action.

6. **NOTE (pre-existing, out of scope) — the terminal record is still two writes with different
   durability.** `:310-315` fsyncs, `:317-323` is a plain `spit`. Unchanged from round 5, safe
   because `state.edn` is the authority and `:finished?` follows both.

**Round-three findings 6, 8, 9 and 10 remain OPEN and unchanged**, as scoped to the adoption build.

---

## Verdict

# GO-WITH-FIX

*For merging.* Every live round-five finding is closed against my own probes, and two of them are
closed by removing the mechanism rather than by counting its failures. The link-first break gave my
hammer **1** opportunity in 20,000 breaks where round five gave it 9,948, with
`displaced-claim-count 0` and no tombstones left behind — the displacement class is gone, not
bounded. The 4,000-race that destroyed 13 judged claims in round five destroyed **0**, with 3,974
refusals typed by `link(2)`'s own EEXIST rather than by a stat this code performs. The tombstone
that round five watched `recover!` delete in the same call that minted its name now survives a
two-day-old lock's break with a sidecar, a listing row and `:pruned 0`, and the 822-of-9,831 ghost
rows are 0 of 2,221 under a live remover. The legacy ceiling refuses one millisecond under and
breaks exactly at it against the file's real basis. All four gates reproduce the builder's counts
exactly, and the kernel is still adopted by **no verb** — `with-cooperating-writes` has zero call
sites, no `:txn/recover` operation is registered, and the only cross-namespace use is
`sha256-string`. Merging cannot break anything that exists today.

*Against merging as-is.* The branch has stopped losing claims and has not yet stopped publishing
receipts its own listings will not corroborate — the same shape for the third round running, now in
three smaller places. A zero-length tombstone is named in `:lock-broken`, counted in
`:broken-locks`, and absent from `retained-transactions`, so MEM-013's "visible to the same
listing" clause is false for it. A sidecar stamp in the future publishes `:age-ms
-315360000000` and makes a file permanent, so MEM-013's "bound … by a published retention age" is
settable by anyone who can write the directory. And `:no-hard-links true` is one keyword on the
public `begin!`/`recover!` surface that silently selects the check-then-act path this very branch
measured destroying 13 claims in 4,000 races, while `:break-path` — the field that would say so —
is dropped by `select-keys` in both callers. None of these is a blocker at zero adoption; all three
would be, the first time a verb calls `recover!` and a human reads its receipt.

**Condition on the merge:** findings 1 and 3 are fixed or filed as named adoption blockers with
bead ids, and finding 2's future-stamp clamp and finding 4's `first`-versus-every fix ride the same
change, before any verb calls `with-cooperating-writes`, `begin!` or `recover!`. Findings 5 and 6
need no action.
