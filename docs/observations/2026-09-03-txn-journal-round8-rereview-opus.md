# Kernel round 8 — Opus executed re-check of bridge/txn-journal at 5a2d254 (2026-09-03T21:29Z)

Verdict: **GO-WITH-FIX — and 5a2d254 REPLACES 11c7377 as the merge point** ("merging at 11c7377 would ship the evidence-deleting forgery my round-7 verdict called the blocker"). All five round-7 findings CLOSED on the reviewer's own probes; gates verified at 5a2d254 (720/5976/0, 467/4534/0, oracle, 0 warnings); adoption zero. Before ANY adoption (round 9): `touch-tombstone!`/`stamp-tombstone!` must report a failed stamp and both resolution branches must type `:evidence :vanished` for a tombstone not on disk (six concurrent recover! calls: 188 of 240 lines said `:retained` for a file not there); `stamp-broken-at!` must not write a sidecar for an absent tombstone (40 orphans minted); the `:sidecar-name-taken` remedy must name the orphan (today it sends the owner to chmod/df for a 24 h name collision); the blocking-file case gets its own key; `:interrupted` split into corroborated/uncorroborated; MEM-013 says the round-6 property lives in `:status`, not `:kind`. Attacks: forged markers add no capability (counted, 2 files each, retention refresh already available via touch); a ≤24 h self-lockout for a stable txid is real, fail-safe, self-clearing; no half state on `:unsupported`; evidence bounded by retention only, not count.

## Opus verdict, verbatim

# txn-journal round 8 (`5a2d254`) — Opus executed re-review: **GO-WITH-FIX**

**Reviewer of record.** Opus, seventh consecutive round. Round-seven verdict under re-judgement:
`git show origin/main:docs/observations/2026-09-03-txn-journal-round7-rereview-opus.md`.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn8-wt` at `5a2d254` (`git rev-parse --short HEAD`
=> `5a2d254`; `git status --porcelain` empty at start and at end; `git stash list` empty; nothing
committed, stashed or pushed). Fixtures under `/tmp/txn8-fx-sol` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M -e '(load-file …)'` from the worktree. Gates run once
each under `/home/forge/bin/suite-run`; `make mcp-test`, `make memory-battery` and `make
memory-red` were never invoked, no port in 7888-7895 or 7906 was contacted, and every process
signalled was one I started (`sleep` children I spawned, `.destroyForcibly` in a `finally`).

**Headline.** All five of round seven's findings are CLOSED against my own re-run probes, and the
worst of them — a forged `:phase :linked` marker DELETING a real break's evidence — is closed in
the fail-safe direction: probes FORGE and AI now return `:interrupted-break-uncorroborated
:evidence :retained` with the tombstone still on disk, where at `11c7377` the same bytes returned
`:interrupted-break-reverted :evidence :removed` and `:broken-locks {:found 0}`. The four attacks
the builder disclosed all behave as disclosed. But the `:evidence :retained ⇒ on disk` invariant
that round seven's finding 3 just spent a commit encoding is FALSE under ordinary concurrency:
six concurrent `recover!` calls on one workspace produced **188 of 240** resolution lines saying
`:evidence :retained` for a file that was not on disk, and minted **40** orphan sidecars doing it.
No claim is lost in any shape I drove, the LOCK is never touched wrongly, and adoption is still
zero — so this is a merge-safe branch with one finding that must land before anything adopts it.

---

## Executed gates (mine, at `5a2d254`, each once under `/home/forge/bin/suite-run`)

```
$ /home/forge/bin/suite-run bash -c 'cd /home/forge/tmp/sol/txn8-wt && bb test/run_all.clj; echo "TESTFAST_EXIT=$?"'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.
TESTFAST_EXIT=0

$ /home/forge/bin/suite-run bash -c '... swipl -q -f test/mcp_operation_contract_oracle.pl ...
                                     clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test ...
                                     clojure -M test/kernel_warning_check.clj'
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0
Ran 467 tests containing 4534 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
kernel warning check: 2 namespace(s), 0 warning(s)
KWC_EXIT=0
```

Builder's stated counts: test-fast 720/5976/0, mcp-test 467/4534/0, oracle pass, warning check 0.
**All four match exactly.**

**Adoption, re-derived at `5a2d254`.** `grep -rn 'txn-journal\|txn_journal' src/ --include=*.clj`
excluding the kernel returns two lines: `src/clj_surgeon/scope_stream.clj:24` (a require) and
`src/clj_surgeon/file_ops.clj:24` (a docstring mention). No `:txn/recover` operation is registered
anywhere in `src/`. Merging cannot break anything that exists today.

---

## Round seven's five findings, re-judged on my own re-run probes

### Finding 1 — a COMPLETED break wearing `:phase :linked` is REVERTED and its evidence deleted — **CLOSED**

`txn_journal.clj:922-955` (`interrupted-break-entries` now returns a PAIR, `:952`
`[f :uncorroborated-marker]` for the marker-only match), `:2793-2816`
(`resolve-interrupted-break!`'s `:else` branch: `:2809` `:resolution
:interrupted-break-uncorroborated`, `:2811` `:evidence :retained`, `:2812` `:cause
:marker-uncorroborated`, `:2813` `:stamp (if stamped :ok :unrecorded)` — no `deleteIfExists`
anywhere on this branch), `:2563` (`retained-transactions` types the row
`:status :uncorroborated-marker`). Witness: `a-completed-break-wearing-a-linked-marker-keeps-its-evidence`
(`test/clj_surgeon/txn_journal_test.clj:2941`, `:2987`).

**Witness (probe FORGE, `/tmp/txn8-fx-sol/p1.clj`) — my round-seven probe, re-run: a GENUINE
`recover!` break, then ONE hand-written sidecar by any writer of the directory:**

```
FORGE a REAL break happened: {:tombstone "LOCK.broken.recover-1788470393638-5e6c4860", :break-path :link, :cause :process-not-alive}
FORGE evidence on disk = ["LOCK.broken.recover-1788470393638-5e6c4860"]
FORGE forged sidecar = {:tombstone "LOCK.broken.recover-1788470393638-5e6c4860", :phase :linked, :linked-at-ms 1788470393650}
FORGE row after the forged marker = [{... :status :uncorroborated-marker, :kind :broken-lock, :stamp :absent, :bytes 96, :age-ms 10}]
FORGE next recover! :interrupted-breaks = [{:tombstone "LOCK.broken.recover-1788470393638-5e6c4860", :resolution :interrupted-break-uncorroborated, :evidence :retained, :cause :marker-uncorroborated, :stamp :ok, :holder-txid nil, :holder-live false, :lock-present false}]
FORGE next recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :unreadable-stamps 0, :retention-ms 86400000}
FORGE evidence on disk AFTER = ["LOCK.broken.recover-1788470393638-5e6c4860"]
FORGE +25h :broken-locks = {:found 1, :pruned 1, :remaining 0, ...}
FORGE +25h tombs on disk = [] sidecars = []
```

Round seven: the same probe returned `:resolution :interrupted-break-reverted :evidence :removed`,
`:broken-locks {:found 0}`, and `evidence on disk AFTER = []`. Round eight keeps the file, types
it, counts it, and still retires it at `+25h` — kept is not permanent.

**Witness (probe AI) — the state a swallowed stamp failure used to leave: a completed break (LOCK
gone) still wearing the marker:**

```
AI LOCK present = false  tombs = ["LOCK.broken.REAL-BREAK"]
AI rows = [{:txid "LOCK.broken.REAL-BREAK", :status :uncorroborated-marker, :kind :broken-lock, :stamp :absent, :bytes 36, :age-ms 1}]
AI recover! :interrupted-breaks = [{:tombstone "LOCK.broken.REAL-BREAK", :resolution :interrupted-break-uncorroborated, :evidence :retained, :cause :marker-uncorroborated, :stamp :ok, :holder-txid nil, :holder-live false, :lock-present false}]
AI recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, ...}
AI tombs AFTER recover! = ["LOCK.broken.REAL-BREAK"]  <== real break evidence
```

### Finding 1, other half — `stamp-broken-at!` must refuse the unlink on a failed write — **CLOSED**

`txn_journal.clj:725-748` (`stamp-broken-at!` returns `now` only inside `(when (replace-sidecar!
…))`, so a failed write is nil), `:1186` (`break-by-link-finish!` is `(if-let [broken-at
(stamp-broken-at! tomb)] …)`), `:1197-1205` (the else: `delete-quietly!` the sidecar AND our own
extra link, `{:broken false :cause :evidence-unrecordable :restored true :restore-path
:lock-never-left}` — the LOCK is never unlinked). `:707-723` (`replace-sidecar!` is an
ATOMIC_MOVE, so `spit`'s truncate-then-write no longer exists). Witness:
`a-break-that-cannot-record-itself-does-not-happen` (`test:3024`).

### Finding 2 — the marker window still yields a false "real break" — **CLOSED for the code path, PARTIAL for legacy tombstones**

`txn_journal.clj:1221-1311` (`break-by-link!`: `:1257` `(case (mark-break-linked! tomb) …)` runs
BEFORE `:1290` `Files/createLink`), `:1295` (`delete-quietly! side` takes the marker back when the
link never happened), `:793-828` (`mark-break-linked!` uses `create-sidecar!` at `:826`, which
links from a fully written temp — no observable half-written marker). Witness:
`the-break-claims-its-marker-before-it-claims-the-name` (`test:3078`).

The two shapes round seven measured are now **unreachable from the code**, and I could only build
them by hand:

```
AW1 link exists, NO marker; tombs = ["LOCK.broken.WINDOW-1"]  sidecars = []
AW1 rows with the LOCK still there = [{:txid "LOCK.broken.WINDOW-1", :status :lock-break-interrupted, :kind :interrupted-break, :bytes 93}]
AW1 release-lock! = true
AW1 rows AFTER the holder released = [{:txid "LOCK.broken.WINDOW-1", :status :lock-broken, :kind :broken-lock, :bytes 93}]
AW1 recover! :interrupted-breaks = nil
AW1 recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, ...}

AZ zero-length marker; break-phase = nil
AZ read-broken-at-ms = :unreadable
AZ rows with LOCK present = [{:txid "LOCK.broken.ZL", :status :lock-break-interrupted, :kind :interrupted-break, :stamp :unreadable}]
AZ rows after the holder released = [{:txid "LOCK.broken.ZL", :status :lock-broken, :kind :broken-lock, :stamp :unreadable}]
```

**The residual, stated exactly:** a tombstone with NO marker is still typed by the inode rule alone
and still re-types to `:kind :broken-lock` when the holder releases. `break-by-link!` can no longer
produce one, and `create-sidecar!`/`replace-sidecar!` can no longer produce a truncated one — but
`interrupted-break-files`' own docstring (`:956-980`) names "old tombstones from builds that wrote
no marker" as a rule the inode branch exists for, and for those the round-six defect is intact.
That is a MIGRATION residual, not a live one, and the correct disposition is the one the branch
already took. **PARTIAL, accepted.**

### Finding 3 — the standing witness is silent on the revert path — **CLOSED (and see round eight's finding 1)**

`test/clj_surgeon/txn_journal_test.clj:1636-1655` (`receipt-file-names` now collects the enclosing
map and its `:evidence` word), `:1657-1663` (`unaccounted-names` = absent AND no `:evidence` key),
`:1706-1780` (`a-receipt-never-names-a-file-that-is-not-there` driven through a break, a revert
AND a finish), `:1744` (the second assertion: `:evidence :retained` must mean the file is on disk).

**Witness (probe C) — my round-seven probe, re-run against the extended predicate:**

```
C receipt :interrupted-breaks = [{:tombstone "LOCK.broken.REVERT-ME", :resolution :interrupted-break-reverted, :evidence :removed, :holder-txid "LIVE-HOLDER", :holder-live true, :lock-present true}]
C every :tombstone the receipt names = ("LOCK.broken.REVERT-ME")
C ... UNACCOUNTED (absent AND no :evidence key) = ()
C ... :evidence :retained but NOT on disk = ()
C would the witness PASS here? = true
```

Round seven: the same receipt returned `would ... PASS here? = false`. The fix is exactly the form
I asked for, including the `:retained ⇒ on disk` half. **That second half is what round eight's
finding 1 breaks under concurrency** — the witness is correct and the code does not satisfy it.

### Finding 4 — `:stamp :unreadable` is typed per row and counted by nothing — **CLOSED**

`txn_journal.clj:1041-1045` (the count, over `(concat aged orphans)`), `:1054`
(`:unreadable-stamps unreadable` in the bucket), `:981-1010` (the docstring: "an alarm about a
clock, not an archive"). Witness: `an-unreadable-stamp-is-counted-by-the-sweep` (`test:3161`).

**Witness (probes F/F59) — the same 61 s / 59 s skew pair, re-run:**

```
F  61s skewed row = [{... :stamp :unreadable, :status :lock-broken, :bytes 96, :age-ms 2}]
F  61s recover! :broken-locks = {:found 1, :pruned 0, :remaining 1, :vanished 0, :interrupted 0, :orphan-sidecars {:found 0, :pruned 0, :remaining 0}, :unreadable-stamps 1, :retention-ms 86400000}
F  61s :unreadable-stamps = 1
F59 59s skewed row = [{... :stamp :ok, :status :lock-broken, :bytes 96, :age-ms 0}]
F59 59s :unreadable-stamps = 0
```

Round seven: the bucket had no stamp field at all and `F` was indistinguishable from `F59`.

### Finding 5 — the listing is a snapshot with a TOCTOU tail — **NOTE, unchanged and still accepted**

`:838-864` (`evidence-stat`, one `Files/readAttributes`), `:2549` (`:when (map? aged)`). Not
re-raced this round; nothing in the round-eight diff touches it. But see round eight's finding 1:
the SAME window, one function further on, now produces a false POSITIVE assertion rather than a
stale row, and that is a different class.

---

## Round eight's own additions, attacked

### (a) "never delete on uncorroborated" — can forged markers make evidence accumulate without bound? — **NO NEW CAPABILITY, but the standing count is ambiguous**

**Cost per forged file, measured.** A forged marker is **one 76-96 byte sidecar**
(`A6 forged marker bytes = 76`). To manufacture a NEW retained row an attacker needs **two** files
— a tombstone and its marker — and can skip the marker entirely, because a bare `LOCK.broken.*`
with no marker is already a counted, retained row:

```
A2 tombs = ["LOCK.broken.FAKE-2" "LOCK.broken.FAKE-1" "LOCK.broken.FAKE-0"]
A2 recover! :broken-locks = {:found 3, :pruned 0, :remaining 3, ...}
A2 +25h :broken-locks = {:found 3, :pruned 3, :remaining 0, ...}
A2 +25h tombs = []

A3 files planted = 10 (5 tombstones + 5 markers)
A3 :interrupted-breaks count = 5
A3 resolutions = {:interrupted-break-uncorroborated 5}
A3 :broken-locks = {:found 5, :pruned 0, :remaining 5, ...}
```

**Is it counted? YES, in both directions — and this is the part that defeats the attack.**

```
A5 rows BEFORE recover! = {:uncorroborated-marker 4}
A5 :broken-locks BEFORE any resolve = {:found 0, :pruned 0, :remaining 0, :vanished 0, :interrupted 4, :orphan-sidecars {...}, :unreadable-stamps 0, :retention-ms 86400000}
A5 receipt :interrupted-breaks lines = 4
A5 :broken-locks in the SAME receipt = {:found 4, :pruned 0, :remaining 4, :vanished 0, :interrupted 0, ...}
A5 rows AFTER = {:lock-broken 4}
```

A standalone sweep counts them in `:interrupted` (`:1052`); the resolving `recover!` publishes one
receipt line each and then counts them in `:found`/`:remaining`. Nothing is silent.

**The one real capability the marker adds, and why it is not a finding.** A forged marker makes
`recover!` ITSELF re-date the evidence, because the uncorroborated branch calls `stamp-tombstone!`
(`:2808`), which writes `:broken-at-ms = now` and `setLastModifiedTime = now`:

```
A6 stamp before = 1788387882686 (23h ago)
A6 stamp after an ORDINARY recover! = 1788387882686
A6 stamp after a FORGED marker + recover! = 1788470682699
A6 moved forward by 82800013 ms  <== the sweep re-dated the evidence
```

So a writer who re-forges one 76-byte sidecar once a day keeps one tombstone alive for ever. **But
that writer already had that, for free, with no marker at all** — `evidence-basis` is
`max(mtime, ctime, stamp)` by design (`:866-898`), so a bare `touch` does the same thing:

```
A4 baseline age after a bare `touch` (no marker, no recover!) = 1  <== the same reset, already available
```

**Verdict on (a): the accumulation is bounded by the SAME bound as before — retention times the
attacker's write rate — and the marker adds no capability an ordinary directory writer lacked.
Both the count and the retirement work.** The one thing I would change is cosmetic and matches the
branch's own discipline: `:interrupted` (`:1052`) now mixes corroborated interrupted breaks with
uncorroborated forged markers, so `A5`'s `:interrupted 4` cannot be told from four genuine crashes
mid-break. Round seven split `:orphan-sidecars` and `:unreadable-stamps` out for exactly this
reason. **MINOR, cosmetic, adoption-scoped.**

### (b) the marker-before-link ordering — is an orphan sidecar a self-inflicted lockout? — **YES, for up to 24 h, and the REMEDY STRING IS WRONG**

`txn_journal.clj:1262-1268` (`:name-taken` with no tombstone => `{:broken false :cause
:evidence-unrecordable :evidence :sidecar-name-taken :sidecar …}`), `:1313-1325`
(`break-refusal-remedy`), `:623-639` (`orphan-sidecar-files`), `:1053` (`:orphan-sidecars` in the
bucket).

**Witness (probe B1) — the exact state a crash between `mark-break-linked!` and `createLink`
leaves, then the same txid retrying:**

```
B1 orphan sidecar = ["LOCK.broken-at.STABLE-TXID"]  tombs = []
B1 rows = (["LOCK.broken-at.STABLE-TXID" :orphan-sidecar :orphan-sidecar])
B1 SAME txid retries the break -> {:broken false, :cause :evidence-unrecordable, :evidence :sidecar-name-taken, :sidecar "LOCK.broken-at.STABLE-TXID"}
B1 receipt carries a :tombstone key? = false
B1 LOCK still there? = true
B1 retry #2 -> {:broken false, :cause :evidence-unrecordable, :evidence :sidecar-name-taken}
B1 +25h sweep :broken-locks = {:found 1, :pruned 1, :remaining 0, :vanished 0, :interrupted 0, :orphan-sidecars {:found 1, :pruned 1, :remaining 0}, :unreadable-stamps 0, :retention-ms 86400000}
B1 +25h sidecars on disk = []
B1 after the orphan retires, SAME txid -> {:broken true, :tombstone "LOCK.broken.STABLE-TXID", :break-path :link}
B1 remedy text for :evidence-unrecordable = "The break could not write the evidence file that records when it happened, so it left the claim alone: an unrecorded break is a destroyed claim with nothing on disk naming who took it. Check that the transactions directory is writable and has space, then retry."
```

**How it clears: only by the 24-hour retention sweep** (`prune-broken-locks!`'s `:orphan-sidecars`
retire), or by a hand delete. Not forever — but for up to `broken-lock-retention-ms`.

**Scope.** `new-txid` (`:1494-1496`) is `<ms>-<8 hex>`, unique per `begin!`, and `recover!` mints
`recover-<ms>-<rand>` per call — so neither default caller can lock itself out. It bites only a
caller that supplies `:txid` verbatim, which `break-lock!`'s own docstring (`:1339-1340`) says
`begin!` accepts. It is fail-SAFE (the LOCK is untouched, `B1 LOCK still there? = true`), typed,
counted and self-clearing.

**The finding is the remedy string, not the lockout.** `break-refusal-remedy` tells the operator to
"check that the transactions directory is writable and has space, then retry" — the directory IS
writable, there IS space, and retrying will fail identically for 24 hours. A refusal whose remedy
points at the wrong cause is worse than a bare cause: it sends the owner to `chmod` and `df` while
the actual fix is one named file. House rule 17: a refusal must name its owner AND what they can
do. **MINOR, must be fixed before adoption.** Secondary: this refusal names a file in `:sidecar`
and carries `:evidence :sidecar-name-taken`, and `receipt-file-names` (`test:1636`) only walks maps
with a string `:tombstone` — so the one refusal that names a file is the one the standing
invariant cannot see. And `:evidence` is now overloaded: `:retained`/`:removed` mean "what became
of the file", `:sidecar-name-taken` means "which file blocked us", and `unaccounted-names`
(`test:1657`) treats ANY `:evidence` value as accounted-for. The day a refusal carries both keys,
the witness passes vacuously.

### (c) `:unsupported` from `createLink` — can it leave a half state? — **NO**

`txn_journal.clj:702` (`create-sidecar!` catches `UnsupportedOperationException` => `:unsupported`),
`:1275` (marker `:unsupported` => `break-by-move!`, the whole point of `5a2d254`), `:1288` (the
tombstone link's own catch => `:unsupported`), `:1295-1300` (`delete-quietly! side` FIRST, then
`break-by-move!`).

**Witness (probe C1) — the marker path, forced with `with-redefs`:**

```
C1 marker :unsupported -> {:broken true, :break-path :move, :tombstone "LOCK.broken.MOVE-FALLBACK"}
C1 LOCK gone? = true  tombs = ["LOCK.broken.MOVE-FALLBACK"]  sidecars = ["LOCK.broken-at.MOVE-FALLBACK"]
C1 rows = (["LOCK.broken.MOVE-FALLBACK" :broken-lock :lock-broken :ok])
```

The fallback runs, completes, and stamps (`:stamp :ok`) — `break-by-move!`'s stamp goes through
`replace-sidecar!`'s ATOMIC_MOVE, which needs no `link(2)`. Without `5a2d254` this returned
`:evidence-unrecordable` and the fallback was unreachable.

**Witness (probe C2) — the createLink-refuses-after-the-marker branch, on a real filesystem.**
`FileAlreadyExistsException` takes the IDENTICAL cleanup path as `:unsupported` (`:1295`
`delete-quietly! side` precedes the `case`), so this measures the same code:

```
C2 -> {:broken false, :cause :tombstone-exists, :tombstone "LOCK.broken.PRE-EXISTING"}
C2 LOCK still there? = true
C2 sidecars left behind = []  <== an orphan means the marker was NOT taken back
C2 pre-existing tombstone content unchanged? = true
```

The marker IS taken back. And the "mid-way" shape the question posits — `link(2)` supported for
`LOCK.broken-at.X` and unsupported for `LOCK.broken.X`, two names in one directory on one
filesystem — is not a state POSIX offers; even granted it, `:1295` deletes the marker and
`break-by-move!` starts from `.exists tomb = false`. **No half state. CLOSED.**

**A note the fix earns, not against it.** `write-lock!` (`:1369-1385`) creates the LOCK with
`Files/createLink`, and `try-write-lock!` (`:1387-1394`) catches only `FileAlreadyExistsException`
— so on a filesystem with no `link(2)` you cannot ACQUIRE the lock at all and never reach a break.
The `:unsupported` fallback `5a2d254` preserves is therefore reachable in practice only through
`:unsafe-break-by-move`. The commit is still right (it removed a wrong refusal); its real-world
blast radius is nil.

### (d) the overturned round-6 witness — is the property still witnessed BY NAME? — **YES, in `:status`; NARROWED in `:kind`**

`test/clj_surgeon/txn_journal_test.clj:2787` — `an-interrupted-break-stays-typed-after-its-holder-releases-the-lock`
is retained under its original name, its docstring carries a "REVISED at round 8" paragraph
(`:2801-2814`), and the property is asserted at `:2843-2845`:

```
(is (= :uncorroborated-marker (:status after))
    (str "the marker still types it - it is never silently re-typed "
         "as a break that happened: " (pr-str after)))
```

I re-ran the corresponding shape myself: probe FORGE's row before resolution reads
`:status :uncorroborated-marker`, and probe A5 reads `{:uncorroborated-marker 4}` for four such
files. The property survives.

**The narrowing, stated honestly.** The very next assertion (`:2846-2849`) now requires
`(= :broken-lock (:kind after))` — the same `:kind` a real break carries (`:2554-2555`). So a
consumer that keys on `:kind` (which is what a quota sweep or `evict!` would do) can no longer
distinguish. The distinction lives in `:status` alone. That is defensible — the file may well BE a
real break, which is the whole reason it is kept — but it IS weaker than round six's property, and
the MEM-013 falsifier should say so in the field name a reader is expected to check.

### (e) `:evidence :retained ⇒ on disk` — raced — **OPEN, and this is round eight's finding**

`txn_journal.clj:750-761` (`touch-tombstone!` catches its own failure at `:760` and returns `now`
at `:761` REGARDLESS), `:786-790` (`stamp-tombstone!`'s truth value comes from `stamp-broken-at!`,
which writes the SIDECAR, not the tombstone; `:790` touches with `(or stamped …)` and ignores the
result), `:2808-2816` (the uncorroborated branch: `:2811` `:evidence :retained` and `:2813`
`:stamp :ok` are emitted with no check that the tombstone exists), `:2778-2780` (the FINISH branch,
same shape).

**The receipt is dishonest AT CONSTRUCTION, not merely at return** — which is the strictest form of
the question and the one that matters.

**Witness (probe E1) — `resolve-interrupted-break!` handed a tombstone that is not there:**

```
E1 tombstone exists at call time? = false
E1 resolution line = {:tombstone "LOCK.broken.GONE", :resolution :interrupted-break-uncorroborated, :evidence :retained, :cause :marker-uncorroborated, :stamp :ok, :holder-txid nil, :holder-live false, :lock-present false}
E1 :evidence = :retained  :stamp = :ok
E1 file named by the line on disk at return? = false
E1 would `a-receipt-never-names-a-file-that-is-not-there` PASS? = false
E1 sidecars the call left behind = ["LOCK.broken-at.GONE"]  tombs = []
```

The call asserts `:retained` and `:stamp :ok` for a file that never existed during the call, and
**mints an orphan sidecar doing it** — `replace-sidecar!` happily creates `LOCK.broken-at.GONE`
next to no tombstone. The FINISH branch has the identical hole (probe E2, tombstone deleted between
the listing and the resolve => same `:evidence :retained`, `file named on disk at return? = false`).

**And it is reachable through the PUBLIC verb with no forgery, no hand cleanup and no crash — just
two `recover!` calls at once.**

**Witness (probe E3) — the deterministic two-call form, which is exactly what two concurrent
`recover!`s do (one reverts; the other had already listed the same file):**

```
E3 one interrupted break, LIVE holder; tombs = ["LOCK.broken.RACE"]
E3 recover! #1 line = {:tombstone "LOCK.broken.RACE", :resolution :interrupted-break-reverted, :evidence :removed}
E3 after #1: tombs = [] sidecars = []
E3 recover! #2 line = {:tombstone "LOCK.broken.RACE", :resolution :interrupted-break-uncorroborated, :evidence :retained, :cause :marker-uncorroborated, :stamp :ok, :holder-txid "LIVE", :holder-live true, :lock-present true}
E3 #2 says :evidence :retained for a file on disk? = false
E3 after #2: tombs = [] sidecars = ["LOCK.broken-at.RACE"]  <== the resolve MINTED the orphan it will later report
```

**Witness (probe E4) — six real concurrent `recover!` calls over 40 interrupted breaks on one
live claim, through the public verb:**

```
E4 planted 40 interrupted breaks over ONE live claim
E4 total resolution lines = 240  resolutions = {:interrupted-break-uncorroborated 188, :interrupted-break-reverted 52}
E4 lines saying :evidence :retained whose file is NOT on disk = 188
E4 example = {:tombstone "LOCK.broken.R-0", :resolution :interrupted-break-uncorroborated, :evidence :retained, :cause :marker-uncorroborated, :stamp :ok, :holder-txid "LIVE", :holder-live true, :lock-present true}
E4 orphan sidecars minted = 40 beside 0 tombstones
```

**188 of 240 lines — 78% — assert `:evidence :retained` for a file that is not on disk**, and the
resolves manufactured 40 orphan sidecars. `recover!` takes no mutual exclusion before resolving
(`:2879-2889` maps `resolve-interrupted-break!` over `interrupted-break-files` with no lock), so
concurrent recovery is a supported shape on a kernel whose acquisition path is witnessed at 4,000
races.

**Why this is a finding and not the accepted TOCTOU tail.** Round seven's finding 5 accepted that a
LISTING row can be stale — a stale row was TRUE when it was measured. `:evidence :retained` is a
POSITIVE assertion about the state at return, the standing witness asserts it verbatim
(`test:1744`), and the branch spent a whole commit (`23f19c6`) encoding it. A false green here
terminates the investigation it exists to start (house rule 20), and the same call fabricates the
orphan it will report tomorrow.

**Why it is not a blocker for merging.** No claim is lost (the LOCK is untouched on every one of
these paths); the tombstone that "vanished" was deleted by a CORRECT concurrent revert, so no
evidence is destroyed; the minted orphans are counted (`:orphan-sidecars`) and retired at 24 h; and
adoption is zero.

***Fix, and it is the twin of the fix `51c015c` already landed.*** `touch-tombstone!` (`:750-761`)
swallows its failure and returns `now` regardless — precisely what `stamp-broken-at!` did before
round eight fixed it two functions away. Make it return nil when `setLastModifiedTime` throws; have
`stamp-tombstone!` (`:786-790`) report that; and have both resolution branches type
`:evidence :vanished` (with no `:stamp :ok`) instead of `:retained` when the tombstone is not
there. And `stamp-broken-at!` should not write a sidecar for a tombstone that is absent — that
write is where the orphan comes from.

### (f) the duplicate-tombstone cost — bounded by count as well as retention? — **BY RETENTION ONLY**

**Witness (probe F1) — five interrupted breaks over ONE claim, resolved in one call:**

```
F1 tombs = ["LOCK.broken.BRK-4" ... "LOCK.broken.BRK-0"]
F1 rows = (["LOCK.broken.BRK-0" :interrupted-break :lock-break-interrupted] ... x5)
F1 ONE recover! resolutions = (["LOCK.broken.BRK-0" :interrupted-break-finished :retained] ["LOCK.broken.BRK-1" :interrupted-break-uncorroborated :retained] ["LOCK.broken.BRK-2" :interrupted-break-uncorroborated :retained] ["LOCK.broken.BRK-3" :interrupted-break-uncorroborated :retained] ["LOCK.broken.BRK-4" :interrupted-break-uncorroborated :retained])
F1 :broken-locks = {:found 5, :pruned 0, :remaining 5, ...}
F1 tombs AFTER = ["LOCK.broken.BRK-4" ... "LOCK.broken.BRK-0"]
F1 +25h :broken-locks = {:found 5, :pruned 5, :remaining 0, ...}
F1 +25h tombs = []
```

The builder's disclosure is exact: the first is FINISHED (the holder was dead), the rest are KEPT
as uncorroborated. All five are retained; all five retire together at `+25h`. Round seven left one.

**The bound.** `prune-broken-locks!` (`:981-1055`) retires on AGE only — there is no cap, and
nothing refuses on `:remaining`. So the ceiling is (distinct tombstone names minted per 24 h) x
(one hard link + one small sidecar each). `recover!` mints a unique name per call, so a crash-loop
of N recoveries in a day leaves N tombstones — each one a directory entry and a link to the SAME
inode, so the data cost is near zero and the cost is inodes, dirents and listing time.
`:remaining` is the standing count and the docstring calls a non-zero value an alarm, but nothing
enforces it. **This is the pre-existing "bounded by retention, not by count" property of the whole
evidence directory, unchanged by round eight; it is the same bound `A2`'s three forged tombstones
sit under.** Worth a named cap at the adoption boundary, alongside round-three findings 6/8/9/10.
**NOTE, adoption-scoped.**

---

## Numbered findings (round eight's own)

1. **OPEN (must fix before adoption) — `:evidence :retained` is asserted for a tombstone that is
   not on disk, and the same call mints the orphan it will later report.**
   `txn_journal.clj:750-761` (`touch-tombstone!` catches its own failure and returns `now`
   regardless), `:786-790` (`stamp-tombstone!`'s truth value is the SIDECAR write, not the
   tombstone's presence), `:2811`/`:2813` (`:evidence :retained` and `:stamp :ok` emitted
   unconditionally on the uncorroborated branch), `:2779` (the same on the finish branch).
   Measured three ways: probe E1 (direct, `would the witness PASS? = false`, orphan
   `LOCK.broken-at.GONE` minted); probe E2 (the finish path); probe E4 — **six concurrent
   `recover!` calls through the PUBLIC verb, 188 of 240 lines saying `:evidence :retained` for a
   file that is not there, and 40 orphan sidecars minted.** This is the exact invariant `23f19c6`
   added a witness for (`test:1744`), and the witness is right while the code is not — it never
   fires because `recover!`'s own scenarios list and resolve with no concurrent deleter. It is the
   TWIN of the bug `51c015c` fixed two functions away: one swallowed write failure was corrected
   and its neighbour was not. *Fix:* `touch-tombstone!` returns nil on failure;
   `stamp-tombstone!` reports it; both resolution branches type `:evidence :vanished` rather than
   `:retained` when the tombstone is absent; `stamp-broken-at!` does not write a sidecar for a
   tombstone that is not there. Red first: two `recover!` calls over one interrupted break.

2. **OPEN (minor) — the `:sidecar-name-taken` refusal's REMEDY names the wrong cause, and can lock
   a stable txid out for 24 hours.** `txn_journal.clj:1262-1268`, `:1313-1325`. Measured (probe
   B1): an orphan sidecar from a crash between `mark-break-linked!` and `createLink` refuses the
   same txid's next break with `:cause :evidence-unrecordable :evidence :sidecar-name-taken` and
   NO `:tombstone` key, repeatably, while the remedy says "check that the transactions directory
   is writable and has space, then retry" — it is writable, there is space, and retrying fails
   identically until the orphan retires (`B1 +25h … then :broken true`). Fail-safe (the LOCK is
   untouched), typed, counted and self-clearing, so the lockout is acceptable; the misdirecting
   remedy is not. *Fix:* a remedy that names the orphan sidecar, says `recover!` retires it on the
   published retention, and gives the file name.

3. **OPEN (minor) — `:evidence` is overloaded across two meanings, and the standing witness cannot
   see the one refusal that names a file.** `:1266` (`:evidence :sidecar-name-taken` = "which file
   blocked us") against `:2779`/`:2796`/`:2811` (`:evidence :retained`/`:removed` = "what became
   of the file"); `test:1657` (`unaccounted-names` treats ANY `:evidence` value as accounted-for)
   and `test:1636` (`receipt-file-names` walks only maps with a string `:tombstone`, so the
   `:sidecar` name is invisible to the invariant). No collision exists today — the `:name-taken`
   branch that carries `:tombstone` (`:1263-1264`) carries no `:evidence`. It is one line away.
   *Fix:* a distinct key for the blocking-file case, and extend the walk to `:sidecar`.

4. **MINOR (cosmetic) — `:interrupted` mixes corroborated interrupted breaks with uncorroborated
   forged markers.** `:1052`, `:1014`. Measured (probe A5): four hand-written sidecars read
   `:interrupted 4`, indistinguishable from four genuine crashes mid-break. Round seven split
   `:orphan-sidecars` and `:unreadable-stamps` out on exactly this argument. *Fix:* the same split.

5. **NOTE (accepted) — the no-marker inode-only tombstone still re-types on the holder's clean
   release, for MIGRATION only.** `:956-980`. Measured (probes AW1, AZ) — but I had to build both
   by hand: `break-by-link!` claims the marker first (`:1257` before `:1290`) and `create-sidecar!`
   / `replace-sidecar!` are atomic, so the kernel can no longer produce either shape. The residual
   is old tombstones from builds that wrote no marker, which the docstring names deliberately.

6. **NOTE (accepted) — the round-six property survives in `:status`, not in `:kind`.** `test:2787`
   keeps the witness under its original name and asserts `:status :uncorroborated-marker` at
   `:2843`; `:2846` now requires `:kind :broken-lock`, the same kind a real break carries. A
   consumer keying on `:kind` cannot distinguish. Defensible, and the MEM-013 falsifier should name
   the field a reader must check.

7. **NOTE (adoption-scoped) — the evidence directory is bounded by RETENTION, not by COUNT.**
   `:981-1055` retires on age only; nothing refuses on `:remaining`. Measured (probes F1, A2, A3).
   Unchanged by round eight. A named cap belongs with round-three findings 6/8/9/10.

8. **NOTE — the `:unsupported` fallback `5a2d254` preserves is nearly unreachable in practice.**
   `write-lock!` (`:1369-1385`) creates the LOCK with `Files/createLink` and `try-write-lock!`
   (`:1387-1394`) catches only `FileAlreadyExistsException`, so a filesystem without `link(2)`
   cannot acquire the lock at all. The commit is still correct — it removed a wrong
   `:evidence-unrecordable` refusal (probe C1) — but its blast radius outside
   `:unsafe-break-by-move` is nil.

9. **Carry forward, unchanged:** round seven's item 9 (`:unsafe-break-by-move` is protected by its
   NAME only because no MCP operation passes caller arguments into `begin!`/`recover!`; when one
   does, that boundary needs an allowlist or an unrecognised-key refusal), and round-three findings
   6, 8, 9 and 10, all adoption-scoped.

---

## Verdict

# GO-WITH-FIX

**Yes — `5a2d254` should replace `11c7377` as the merge point.**

*Why.* All five of round seven's findings are closed against my own re-run probes, and the one that
mattered is closed in the right DIRECTION: at `11c7377`, probe FORGE deleted a genuine break's
evidence from one hand-written 76-byte sidecar and returned `:broken-locks {:found 0}`; at
`5a2d254` the same bytes return `:interrupted-break-uncorroborated :evidence :retained` with the
file still on disk, counted, and retired on the ordinary 24 h (`FORGE +25h {:found 1 :pruned 1
:remaining 0}`). A break that cannot write its stamp now refuses the unlink instead of destroying a
claim it could not record. The marker is claimed before the name, so the kernel can no longer
produce the no-marker window at all — I had to hand-build AW1 and AZ. The receipt witness covers
the revert path and my probe C passes where round seven's failed. `:unreadable-stamps` is 1 at 61 s
of skew and 0 at 59 s. And `5a2d254` itself keeps the move fallback reachable when `link(2)` is
absent, with no half state (probes C1, C2). The gates match the builder's counts exactly —
720/5976/0, 467/4534/0, oracle pass, 0 warnings — and adoption is still two lines, a require and a
docstring, so merging cannot break anything that exists.

**Merging at `11c7377` instead would ship the evidence-deleting forgery my own round-seven verdict
called the blocker.** That is the decisive comparison: round eight's worst finding is a DISHONEST
RECEIPT about a file a correct concurrent revert already removed; round seven's was the DESTRUCTION
of a real break's evidence by any writer of the directory. Fail-open reporting is strictly better
than fail-open deletion.

*Why not GO.* Finding 1 is real, reachable through the public verb with no forgery and no crash,
and it falsifies the exact invariant the branch just added a witness for — `:evidence :retained`
must mean the file is on disk, `test:1744`. 188 of 240 lines lied under six concurrent recoveries,
and the resolves minted 40 orphan sidecars in the process. A false green terminates the
investigation it exists to start, and the branch's whole thesis is that evidence is bounded,
counted and honestly reported. It has no witness because `recover!`'s scenarios never have a
concurrent deleter.

*Why not NO-GO.* No claim is lost on any path I drove — the LOCK is untouched in E1, E2, E3, E4,
B1 and C2. The "vanished" tombstone was deleted by a CORRECT concurrent revert, so no evidence is
destroyed. The minted orphans are counted and retire on the published retention. Nothing calls the
kernel. And the fix is the twin of one this very branch already landed: one swallowed exception in
`touch-tombstone!` (`:760-761`) and two `:evidence` words.

**Numbered, for the merge queue:**

1. **Merge `bridge/txn-journal` at `5a2d254`, and use it in place of `11c7377`.** Gates verified
   independently at `5a2d254`: test-fast 720/5976/0, mcp-test 467/4534/0, oracle pass, warning
   check 0, adoption zero (`scope_stream.clj:24` require, `file_ops.clj:24` docstring).
2. **Before ANY adoption — round eight, finding 1.** `touch-tombstone!` (`txn_journal.clj:750-761`)
   must return nil when `setLastModifiedTime` throws, `stamp-tombstone!` (`:786-790`) must report
   it, and both resolution branches (`:2779`, `:2811`) must type `:evidence :vanished` — never
   `:retained`, never `:stamp :ok` — for a tombstone that is not on disk.
3. **Same fix, the other half.** `stamp-broken-at!` must not write a sidecar for a tombstone that
   is absent: that write is where E1's and E4's orphans come from (40 minted in one probe).
4. **A witness for finding 1, red first:** two `recover!` calls over one interrupted break with a
   live holder — the second must not say `:evidence :retained` for the file the first removed.
   `a-receipt-never-names-a-file-that-is-not-there` (`test:1706`) already carries the predicate at
   `test:1744`; it needs the concurrent scenario, not a new assertion.
5. **A MEM-013 falsifier row** for "a resolution says `:retained` for a file it did not keep."
6. **Finding 2:** `break-refusal-remedy` (`:1313-1325`) must name the ORPHAN SIDECAR for the
   `:sidecar-name-taken` case — the file, that `recover!` retires it on the published retention,
   and that retrying before then will fail identically. Today it sends the owner to `chmod` and
   `df` for a 24-hour name collision.
7. **Finding 3:** give the blocking-file case its own key rather than reusing `:evidence`, and
   extend `receipt-file-names` (`test:1636`) to walk `:sidecar` — the one refusal that names a file
   is currently the one the standing invariant cannot see.
8. **Finding 4:** split `:interrupted` (`:1052`) into corroborated and uncorroborated, the same
   discipline round seven applied to `:orphan-sidecars` and `:unreadable-stamps`.
9. **Finding 6:** the MEM-013 text should say that the round-six property now lives in `:status`
   (`:uncorroborated-marker`) and NOT in `:kind`, which is `:broken-lock` for both a real break and
   an uncorroborated marker (`test:2843` vs `test:2846`).
10. **Carry forward at the adoption boundary, not now:** a COUNT cap on the evidence directory
    (finding 7 — it is bounded by retention only); the `:unsafe-break-by-move` allowlist when an
    argument map first reaches `begin!`/`recover!` (round seven, item 9); and round-three findings
    6, 8, 9 and 10, unchanged.
