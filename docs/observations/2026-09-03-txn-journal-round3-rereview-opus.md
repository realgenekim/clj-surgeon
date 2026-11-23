# txn-journal eb22036 (kernel round 3) — Opus executed re-check: GO-WITH-FIX (round-2 blockers CLOSED; three new from the cooperation door: cross-thread OverlappingFileLockException deadlock, future inherits re-entrancy, non-atomic stale-lock break) — round 4 launched

# txn-journal round 3 (`eb22036`) — Opus executed re-review: **GO-WITH-FIX**, one new blocker of round 3's own making

**Reviewer of record.** This is the **Opus-first** review of this branch: OpenAI's content filter
refused the round-2 pass, Opus took it, and Opus has taken this one too. There is no Sol arm on
round 3.

**Continuity note.** A first attempt at this round was killed by a session limit mid-probe. It had
established one thing worth carrying forward, and this run relied on it: **a bash `flock` does NOT
block Java's `FileChannel/lock`** — they are different lock families — so every cross-process lock
probe below uses a real JVM holder (`clj-surgeon.txn-lock-child`, a separate `java` process), never
a shell `flock`. This run started fresh from a clean clone.

**Apparatus.** Scratch clone at `/home/forge/tmp/opus-txn3` (`git checkout eb22036`; never
committed, never stashed, never pushed). A read-only export of the pre-change tree at `3bd4fc1`
sits in `/home/forge/tmp/opus-txn3-before` (`git archive | tar -x`, no git mutation) so the window
measurement has a same-box **before** arm. Fixtures under `/home/forge/tmp/opus-txn3-fx`. No port
in 7888–7895 or 7906 was contacted; `mcp-test` ran as `clojure -M:clj-surgeon/mcp-test` through
`suite-run`, never `make mcp-test` against a live server. `make memory-red` ran **once**, under
`flock /home/forge/tmp/suite.lock`.

**Headline.** Round 3 closes all three of my round-2 blockers and four of the five minors, with
real witnesses that reproduce against my own injections. It also **introduces one new blocker**:
the single new public affordance it adds — `with-cooperating-writes`, the opt-in that finally gives
the advisory lock a non-empty referent — is not safe under concurrency in either direction. A
second thread in the same JVM throws `OverlappingFileLockException` out of `commit!` (uncaught,
transaction left `:sealed`, project LOCK stranded behind a *live* pid that neither `begin!` nor
`recover!` may break), and a `future` spawned inside the lock inherits the private re-entrancy
binding and silently takes **no lock at all**.

---

## Executed gates (this reviewer, at `eb22036`)

| gate | how | result | builder's claim | agrees |
|---|---|---|---|---|
| `make memory-red` | `flock /home/forge/tmp/suite.lock`, run **once** | **4 tests / 25 assertions / 0 failures / 0 errors** | 4/25/0 | yes |
| `make test-fast` | `suite-run` | **720 tests / 5976 assertions / 0 / 0** | 720/5976/0 | yes |
| `make mcp-test` | `suite-run` | **431 tests / 4277 assertions / 0 / 0** | 431/4277/0 | yes |
| `mcp-operation-oracle` | inside `mcp-test` | `pass; legacy counterexamples=[verification_failed,verification_pending]` | pass | yes |
| `make memory-battery-self-test` | `suite-run` | **18 tests / 64 assertions / 0 / 0** | 18/64/0 | yes |
| `make txn-kernel-warning-check` | inside `mcp-test` **and** standalone | `2 namespace(s), 0 warning(s)` both times | 0 | yes |

No full battery was run. Every number is from my own run.

### `make memory-red`, verbatim

```text
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/memory-test

Testing clj-surgeon.memory.oom-reproduction-test
CONTROL scope: {:root /home/forge/tmp/clj-surgeon-memory-control-1788429895132, :files 8, :bytes 4196920}
CONTROL receipt: {:arm :frozen-read, :files 8, :bytes 4196920, :result-hash-count 8, :tree-hash f1ff28cb792b72d27c6b91e884a55487ef9fb435b28f807bbd4724859c637ae6, :memory {:xmx-mb 256.0, :heap-used-start-mb 16.85057830810547, :heap-used-peak-mb 252.6057586669922, :heap-used-end-mb 12.312789916992188, :heap-after-gc-peak-mb 219.2398452758789, :heap-retained-peak-mb 0.0, :wall-ms 2427}}
RED scope: {:root /home/forge/tmp/clj-surgeon-memory-frozen-1788429899720, :files 600, :bytes 314772270}
RED exit: 3
RED err:
RED out: Terminating due to java.lang.OutOfMemoryError: Java heap space


Testing clj-surgeon.memory.journal-green-test
GREEN scope: {:root /home/forge/tmp/clj-surgeon-green-journal-1788429902840, :files 600, :bytes 314772270}
GREEN reference receipt: {:arm :frozen-read, :files 600, :bytes 314772270, :result-hash-count 600, :tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :memory {:xmx-mb 2048.0, :heap-used-start-mb 18.04082489013672, :heap-used-peak-mb 2046.6841278076172, :heap-used-end-mb 12.279197692871094, :heap-after-gc-peak-mb 1698.4206161499023, :heap-retained-peak-mb 0.0, :wall-ms 153740}}
GREEN journal receipt: {:tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :arm :journal, :work {:walk-entries 604, :files-discovered 600, :files-read 600, :source-bytes 314772270, :largest-file-bytes 524621, :receipt-records 0, :receipt-bytes 0}, :memory {:xmx-mb 256.0, :heap-used-start-mb 31.60443878173828, :heap-used-peak-mb 254.1679229736328, :heap-used-end-mb 10.991615295410156, :heap-after-gc-peak-mb 241.26978302001953, :heap-retained-peak-mb 14.425048828125, :wall-ms 173000}, :read-set-files 600, :commit-error nil, :committed true, :refusals [], :files 600, :reserved {:staged-files 600, :aggregate-bytes 314772270, :heap-reserved-peak-bytes 29446956, :path-list-bytes 68180, :journal-bytes-max 1073741824, :staged-files-max 2000, :aggregate-bytes-max 536870912, :journal-bytes-peak 629544540, :journal-bytes 629544540, :work-budget-bytes 201326592, :discovered-files 600, :parse-factor 56}, :files-written 600}
GREEN err:
FLATNESS 60 {:xmx-mb 256.0, :heap-used-start-mb 58.960205078125, :heap-used-peak-mb 254.14259338378906, :heap-used-end-mb 13.158294677734375, :heap-after-gc-peak-mb 219.13777923583984, :heap-retained-peak-mb 14.217391967773438, :wall-ms 21975}
FLATNESS 600 {:xmx-mb 256.0, :heap-used-start-mb 58.23155975341797, :heap-used-peak-mb 251.91898345947266, :heap-used-end-mb 11.0477294921875, :heap-after-gc-peak-mb 227.93286895751953, :heap-retained-peak-mb 14.913642883300781, :wall-ms 168544}

Ran 4 tests containing 25 assertions.
0 failures, 0 errors.
```

**What that run proves, on shipped defaults.** RED OOMs at `-Xmx256m` (exit 3); GREEN commits the
same 600-file / 314,772,270-byte scope at the same `-Xmx256m`; three-way output parity is exact at
`55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440`. **The reader override is gone
and the receipt proves it**: `:aggregate-bytes-max 536870912` (the shipped default) beside
`:journal-bytes-max 1073741824` — exactly `quota = 2 × aggregate`, so the arm now witnesses the
derivation instead of contradicting it (round-2 minor 6, closed). The accountant's identity
reproduces to the byte: `:heap-reserved-peak-bytes 29446956` = 29,378,776 + `:path-list-bytes
68180`. Retention 14.22 MB at 60 files / 14.91 MB at 600 — flat, and the run-to-run spread again
exceeds the 10× file-count spread, which is why it is a trend line and not a gate.

---

## My eight round-2 findings, re-judged

### 1. Stale project `LOCK` = permanent deadlock — **CLOSED**

`src/clj_surgeon/txn_journal.clj:355` (`stale-holder`), `:376` (`breakable-causes`), `:400`
(`write-lock!`), `:423` (`acquire-lock!`), `:535` (the durable journal line), `:1628` (recovery's
release). The holder is now the checkable triple pid + start-ticks + boot-id.

**My re-runs (probe A/B/C, all mine, not the builder's):**

- Planted `{:txid "ghost-1" :pid 999999 :start-ticks 1 :boot-id <current>}` with no transaction
  directory beside it → `begin!` **acquires** and returns
  `:lock-broken {:reason :stale-holder, :cause :process-not-alive, :pid 999999, :holder-txid "ghost-1", :broken-at "2026-09-03T10:10:40.199096789Z"}`;
  the journal carries the durable line `"lock-broken\t999999\tprocess-not-alive"`; the LOCK
  afterwards names the new holder. **Broken exactly once** — the retry loop is gated on
  `(zero? attempt)` (`:441`).
- Same stranded LOCK, nothing to recover → `recover!` returns
  `{:ok true, :transactions-recovered 0, :lock-broken {…:cause :process-not-alive…}}` and the LOCK
  is **gone**; the next `begin!` acquires cleanly with `:lock-broken nil`. Round 2's exact deadlock
  is dead.
- **A LIVE holder is never broken.** I started a real child process, wrote its true pid + true
  start-ticks + current boot-id: `begin!` → `{:error-type :txn-lock-held, :holder-pid 4061848,
  :holder-live true, :holder-cause nil}`, LOCK survives; `recover!` → `{:ok true,
  :transactions-recovered 0}` with **no** `:lock-broken` and the LOCK still there.
- **Pid reuse is caught both ways.** Same live pid with wrong start-ticks →
  `:cause :start-ticks-mismatch`, broken. Same live pid, right start-ticks, wrong boot-id →
  `:cause :boot-id-mismatch`, broken.
- **An unreadable LOCK refuses fail-closed.** `"}}}not-edn{{{"` → `{:error-type :txn-lock-held,
  :holder-pid nil, :holder-live false, :holder-cause :no-recorded-holder}` and the LOCK survives;
  a zero-byte LOCK behaves identically. `recover!` is the remedy and clears it. `:no-recorded-holder`
  is correctly absent from `breakable-causes`.
- The kernel can no longer *create* an empty LOCK: `write-lock!` (`:400`) spits a fully populated
  temp and gives it the LOCK's name by `Files/createLink`, which is create-if-absent.

*(Two residuals survive as new findings 3 and 6 below.)*

### 2. Retention refcount fails OPEN on a missing lease — **CLOSED**

`txn_journal.clj:1402` (`read-lease` → `{:lease :unreadable :receipt-refs 1 :evictable false}`),
`:1438-1440` (`retained-transactions` defaults flipped to 1 / false), `:1504-1513` (the typed
refusal), `:1483` (`presents-receipt?`).

**My re-run (probe E).** Committed one file, then deleted `lease.edn` by hand:

- `retained-transactions` → `{:status :committed, :receipt-refs 1, :evictable false,
  :lease :unreadable, :bytes 1088}` — the refcount is UNKNOWN, reported as 1, not invented as 0.
- `evict!` → `{:error-type :txn-lease-unreadable, :lease :unreadable, :receipt-refs 1,
  :evictable false, :status :committed}`. **A quota sweep can never reclaim it**, receipt or not.
- `forget!` with no receipt → `:txn-lease-unreadable`. `forget!` with a *wrong* receipt
  (`{:txid "nope" :committed true}`) → `:txn-lease-unreadable`.
- `undo!` **still restores H0** — `{:ok true, :undone true}`, path `:verified`, bytes equal to H0.
  The receipt stayed reversible, which is the whole point.
- `forget!` with the genuine commit receipt → `{:ok true, :forgotten true}`, directory gone.
- Also covered: `chmod 000` on `lease.edn` (EACCES rather than absence) → identical
  `:lease :unreadable` treatment. **The answer to "a lease merely LOCKED by another process":** an
  advisory `FileChannel` lock does not block a read on Linux, so that case does not arise; the case
  that does arise — a lease the process cannot read for any reason — is **refused fail-closed with
  no retry**, which is the right direction. There is no distinction between "transiently
  unreadable" and "gone", and no retry, so a caller hitting a transient EACCES gets a permanent-
  sounding refusal. Acceptable; noted as minor 8.

### 3. `undo!`/`recover!` write with no lock and no recheck — **CLOSED**

`txn_journal.clj:1297` (`undo-conflicts` over H1), `:1229` (the `write-done` line now carries the
identity token), `:1381` (`restore-from-journal!` runs inside `file-ops/with-publish-lock*`),
`:1443-1481` (`undo!` passes `:expect-post-commit?` and refuses), `:1596` (recovery passes the
transactions dir so crash recovery is locked too).

**My re-run (probe F), the exact round-2 injection, now with a real second JVM:** committed H1,
overwrote the target with `SOMEBODY-ELSE`, started `clj-surgeon.txn-lock-child` holding
`PUBLISH.lock` for 2500 ms, then called `undo!`.

- `undo!` **waited 2502 ms** — it blocks on the other process's lock rather than walking past it
  (round 2 measured 2.5 ms and a silent clobber).
- Then it refused: `{:error-type :txn-undo-conflict, :files-written 0, :conflicts [{:conflict :digest,
  :expected-hash "5db53ba1…", :actual-hash "003e93f8…"}]}`, and the disk still held
  `(ns f0) (def v :SOMEBODY-ELSE)` — **zero writes**.
- Restore H1 by hand → `undo!` → `{:ok true, :undone true}`, file back at H0. The refusal is a
  conflict, not a permanent brick.
- **Identity is rechecked too:** replacing the target with byte-identical H1 content through a
  *different inode* → `{:error-type :txn-undo-conflict, :conflict :identity,
  :expected-identity "regular|(dev=801,ino=32489216)",
  :actual-identity "regular|(dev=801,ino=32489207)"}`.

**The question about "a file the commit DELETED."** The kernel has no delete verb — `stage!`
(`:820`) only writes content — so an H1 record for a deleted path is unreachable through the public
API, and "undo must recreate it" is not a case this kernel can be in. The reachable neighbour is a
target somebody deleted *after* the commit: `undo-conflicts` (`:1312`) types it
`{:conflict :missing, :expected-hash …}` and the whole undo refuses with 0 writes; the file is
**not** recreated. That is the correct fail-closed choice — but it means the pre-images stay
un-republishable through any in-tool path, and the refusal's remedy ("reconcile that change first")
has no verb behind it for the delete case. Noted as minor 9.

### 4. `with-publish-lock*` had one call site — **PARTIAL (real, and it brought a blocker with it)**

`src/clj_surgeon/file_ops.clj:13` (`*publish-lock-dir*`), `:39` (`with-publish-lock*`, moved here),
`:105` (`atomic-write!` takes it when bound), `txn_journal.clj:454` (`with-cooperating-writes`).

**The cooperation works, cross-process, measured (probe H):**

- `*publish-lock-dir*` unbound (the default): with another JVM holding `PUBLISH.lock`,
  `atomic-write!` completed in **2 ms** — no cooperation, exactly as documented.
- Wrapped in `with-cooperating-writes`: the same write **waited 1801 ms** for the other process's
  lock, then landed. The clause's referent is no longer empty.
- Re-entrancy on one thread holds: nested `with-publish-lock*` → `:no-deadlock`; a cooperating
  `atomic-write!` *inside* a held lock → `:ok`.

**The ~15 non-cooperating sites are tabulated** in `docs/txn-journal.md:76-89`, with a standing
FOLLOW-UP at `:95-100` making the retrofit an explicit adoption obligation. I checked every line
number in that table against the tree and **all of them are exact**. Three gaps (minor 7):
`extract.clj:242` and `intent_transaction.clj:2187` are `file-ops/atomic-write!`/`atomic-create!`
sites absent from the table, and `worktree_lifecycle_io.clj:399` defines its **own private**
`atomic-write!` (used at `:438`) — a site the opt-in cannot reach at all, so binding the dynamic
var will silently fail to cover it during the retrofit.

**But the affordance itself is unsafe under concurrency — see blockers 1 and 2 below.** This is
why the finding is PARTIAL rather than CLOSED.

### 5. Project `LOCK` scoped to the state home, not the workspace — **CLOSED (documentation)**

`docs/txn-journal.md:102-109` now states it in the kernel's own words: two state homes on one
workspace root both acquire a lock, both reach commit, and are separated only by the optimistic
recheck. The doc obligation is discharged.

### 6. Green arm raised the reader's aggregate ceiling — **CLOSED**

`test/clj_surgeon/memory/journal_child.clj:57-65` now passes `{}` — no ceiling override on the read
path at all — and the comment says why. My own `memory-red` receipt above reads
`:aggregate-bytes-max 536870912` beside `:journal-bytes-max 1073741824`. Witness, not counterexample.

### 7. Battery-shape witness 74 bytes wrong, passing by rounding — **CLOSED**

`test/clj_surgeon/scope_stream_test.clj:279-293` asserts `(pos? path-list)`, then
`(= (+ path-list (* 3000 56)) (:heap-reserved-peak-bytes reserved))` at **byte** granularity, then
the MB form. **My independent re-run** (probe J, calling `scope/stream-scope!` directly):
`{:heap-reserved-peak-bytes 168074, :path-list-bytes 74, :discovered-files 1}` — and
`74 + 168000 = 168074`. ✓ Byte-exact; removing path-list accounting now fails the assertion.

### 8. Recovery's orphan sweep deleted any sibling — **CLOSED**

`txn_journal.clj:1121-1124` (the temp's name goes into the `write-begin` line), `:1260`
(`restore-begun!` takes the recorded `temps`), `:1287-1290` (deletes only those names).

**My re-run (probe I):** ran a real commit, read its `write-begin` line —
`write-begin\t…/src/f000.clj\t451179b4…\t.clj-surgeon-publish-11511211988842359664.tmp` — planted
both a file with that recorded name and a foreign `.clj-surgeon-publish-FOREIGN.tmp`, forced
`state.edn` back to `:open`, ran `recover!`: `{:ok true, :transactions-recovered 1}`, **own temp
deleted**, **FOREIGN temp survives**, target restored to H0. The narrow give-up (a temp orphaned
between `prepare-publish!` and the `write-begin` line is now litter nobody sweeps) is stated in the
docstring at `:1266-1270` and is bytes-safe.

---

## The window: measured before and after, my own arms

`publish-one!` (`txn_journal.clj:1072`) now takes `stat-before` → `sha256-file` → `stat-after`
**outside** the lock (`:1095-1099`); inside the lock it compares one `path-stat` (`:197`) and
re-reads the digest only if the stat moved (`:1105-1107`), reporting `:digest-rereads` in the
receipt (`:1180`).

**My reproduction** — same box, same session, 9 commits per cell after a 5-commit warmup, median
`:max-ns` straight from the receipt. The "before" arm is a clean `git archive` of `3bd4fc1`:

| target | before (`3bd4fc1`) | after (`eb22036`) |
|---|---:|---:|
| 1 KB | 672,439 ns | 633,980 ns |
| 2 MiB | 2,939,460 ns | 1,204,517 ns |
| **size term (2 MiB − 1 KB)** | **2,267,021 ns** | **570,537 ns** (**−75%**) |

The builder filed 789,665 → 623,386 (1 KB), 2,671,197 → 1,124,137 (2 MiB), size term
1,881,532 → 500,751 (−73%). **Mine reproduce theirs within ordinary timing variance and agree on
the direction and the magnitude.** The improvement is real and it is the largest single win in
round 3.

**But `O(1)` is asserted more strongly than it is measured.** After the change the 2 MiB window is
still **1.9×** the 1 KB window, a 570 µs residual size term. `docs/txn-journal.md:378-388` says so
honestly and attributes it to writeback pressure from the pre-lock copy — but the *contract value*
at `txn_journal.clj:52` ("the bound is O(1) in the target's size"), the module docstring at `:8-9`,
and the ratified MEM-007 clause all state it flatly. MEM-014's own new rule — "every statement it
makes about an instrument shall be true of that instrument in general, not only of the case that
motivated it" — is the rule this sentence fails. See minor 5.

### Probing the new stat guard (probe G — all mine)

- **Same length, same inode, timestamps bump.** Injected at `:before-recheck` (which fires after
  the pre-lock digest and before the lock is taken — the exact new gap): an in-place
  `RandomAccessFile` rewrite of equal length. Stat before/after:
  `mtime 1788430362714839351 → 1788430362731219511`, same `(dev=801,ino=32489219)`, same size 23.
  → stat moved → digest re-read inside the lock → `{:error-type :txn-conflict, :conflict :digest,
  :files-written 0, :rolled-back true}` and the injector's bytes survive on disk. **Refused.**
- **Same length, same inode, `touch -r` semantics — mtime restored exactly.** After the rewrite I
  restored `lastModifiedTime` to the recorded nanosecond value: mtime equal ✓, size equal ✓, inode
  equal ✓, **ctime differs by 10,050,349 ns**. → still `{:error-type :txn-conflict,
  :conflict :digest, :files-written 0}`. **Refused.**
  **What ctime buys, precisely:** `mtime` is settable by any owner (`utimensat`, i.e. `touch -r`),
  so mtime alone is defeatable by an attacker or by an ordinary tool that preserves timestamps.
  **`ctime` is settable by no syscall at all — not even by root**, who can only move the system
  clock. It is therefore the only field in the tuple an equal-length in-place rewrite cannot
  forge, and it is what makes the fast path sound on Linux/ext4.
- **`mtime` really is nanosecond-resolution here** — five successive writes gave
  `…758593212, …758964790, …759227664, …759491700, …759742326`, sub-millisecond digits present. So
  mtime is a strong second line even where ctime is unavailable; it is just not an unforgeable one.
- **The fast path cannot be mistaken for a skipped check.** A touch-only injection (same bytes, new
  timestamps) commits and reports `{:ok true, :files-written 1, :digest-rereads 1}`; a clean commit
  reports `:digest-rereads 0`.
- **Residual (minor 6):** `path-stat` (`:218-222`) swallows any failure of `unix:ctime` into
  `:ctime-ns nil`. On a filesystem or OS that does not publish it the guard silently degrades to
  kind + file-key + size + mtime — a strictly forgeable tuple — and **nothing in the receipt says
  so**. `:stat-fields` (`:68`) advertises `:ctime-ns` unconditionally.

---

## Round 3's own additions — what I hunted, and what I found

- **LOCK via temp + hardlink, races.** *Four separate JVMs* released simultaneously against an
  absent LOCK (file-barrier synchronised): **exactly one winner** — `proc1` WON with
  `lock-broken=nil`; `proc2`, `proc3`, `proc4` each `REFUSED :txn-lock-held holder="proc1"`. A
  16-thread same-JVM race gave the same shape (1 winner, 15 `:txn-lock-held`). `Files/createLink`
  is create-if-absent at the kernel level and the acquisition is sound.
- **The pid/ticks/boot triple across a pid-reuse simulation** — covered under finding 1: both
  `:start-ticks-mismatch` and `:boot-id-mismatch` break; the exact live triple never does.
- **`:lease :unreadable` on a lease another process holds** — covered under finding 2: refused
  fail-closed, no retry; an advisory lock does not make a file unreadable on Linux, so the
  realisable case is EACCES/absence and it is handled.
- **H1 for a deleted file** — covered under finding 3: unreachable through the API; the neighbour
  case refuses.
- **`with-publish-lock*` re-entrancy across a `future`** — **BROKEN.** See blocker 2.
- **A sibling thread taking the lock** — **BROKEN, and it strands the project lock.** See blocker 1.
- **The break is not atomic** — see blocker 3.

---

## Verdict

# GO-WITH-FIX

*For merging.* Every one of my three round-2 blockers is closed against my own injections, not the
builder's. The window measurement reproduces the builder's before/after on my own arms with a −75%
size term. `memory-red` is RED→GREEN on **shipped defaults** with byte-exact three-way parity and
the reader override genuinely deleted. All six gates reproduce the builder's counts exactly. The
kernel is still adopted by **no verb** (`with-cooperating-writes` has zero call sites in `src/`),
so merging cannot break anything that exists today.

*Against merging as-is.* Round 3's one new public affordance is not concurrency-safe, and its
worst failure mode — an uncaught `OverlappingFileLockException` out of `commit!` that leaves the
transaction `:sealed` and the project LOCK held by a **live** pid that neither `begin!` nor
`recover!` is permitted to break — is the same deadlock class round 3 was written to eliminate,
re-entered through the new door. It is invisible to every existing witness because the whole test
suite is single-threaded.

**Condition on the merge:** findings 1, 2 and 3 are fixed with witnesses, or filed as named
adoption blockers with bead ids, before any verb calls `with-cooperating-writes`. Findings 4–6 are
one-line honesty edits that should ride this change. Findings 7–10 can ride the adoption build.

---

## Numbered findings (round 3's own)

1. **BLOCKER — a cooperating write from a SECOND THREAD throws `OverlappingFileLockException` out
   of `commit!`, leaving the transaction `:sealed` and the project LOCK stranded behind a live pid.**
   `src/clj_surgeon/file_ops.clj:51-63` — the re-entrancy guard is `(= *publish-lock-held* (str dir))`,
   which is per-*binding*, not per-JVM; `FileChannel/lock` is JVM-wide, so a different thread's
   `.lock` on the same file throws rather than blocking. The throw escapes `publish-one!`
   (`txn_journal.clj:1102`), which catches only around `prepare-fn`, so `commit!` never calls
   `finish!` and never releases the lock (`:1051`).
   **Witness (probe P4):** with a sibling thread inside `with-cooperating-writes`, `commit!` →
   `{:THREW "java.nio.channels.OverlappingFileLockException", :msg ""}`; target unchanged at H0
   (data is safe); `state.edn` → `{:status :sealed}`; `LOCK` still present naming `"X1"`; the next
   `begin!` → `{:error-type :txn-lock-held, :holder-live true, :holder-cause nil}`; `recover!` →
   `{:ok true, :transactions-recovered 1}` **and the LOCK survives**, because the holder is
   genuinely alive. The workspace is deadlocked for the life of the process.
   *Fix:* guard `with-publish-lock*` with a JVM-wide per-directory monitor (a `ReentrantLock` per
   canonical path held around the `FileChannel/lock`), and catch `OverlappingFileLockException` in
   `publish-one!` as a typed refusal so a commit can never escape without `finish!`.

2. **BLOCKER — a `future` spawned inside the publish lock INHERITS the re-entrancy binding and
   takes no lock at all.** `src/clj_surgeon/file_ops.clj:28-33,51` — `*publish-lock-held*` is a
   dynamic var, and Clojure conveys dynamic bindings to `future`, `send`, `pmap` and every
   `bound-fn`. The guard means "some frame on my binding stack took it", not "this thread holds it".
   **Witness (probe H4/H5):** `(with-publish-lock* dir (fn [] @(future @#'file-ops/*publish-lock-held*)))`
   returns the lock directory — conveyed. With that binding in place (exactly a conveyed future's
   state) and **another JVM holding `PUBLISH.lock`**, `with-publish-lock*` + `atomic-write!`
   completed in **1 ms** and the bytes landed: a cooperating writer that silently stopped
   cooperating. This is the concurrency primitive an adopting verb reaches for first (house rules
   name claypoole `pmap` for Surgeon's per-file plan phases).
   *Fix:* key the guard on `(Thread/currentThread)`, or on a JVM-wide holder table, never on a
   conveyed dynamic binding.

3. **BLOCKER (narrow race) — breaking a stale lock is a read-then-unconditional-delete, so a
   breaker can delete a LIVE holder's freshly created LOCK and both then hold it.**
   `src/clj_surgeon/txn_journal.clj:396-398` (`release-lock!` is a bare `deleteIfExists`), called
   from `:441-442` after `:439-440` read the holder, and again from `:1628` in `recover!` after
   `:1626-1627` read it.
   **Witness (probe K), stepping the real private fns in `acquire-lock!`'s exact order:** B reads
   holder `"ghost"`, `stale-holder` → `:process-not-alive`, so B will break. A then legitimately
   acquires (`LOCK` names `"A-LIVE"`, live pid 4120374). B resumes: `release-lock!` deletes **A's**
   lock (`after B's release-lock!, A's LOCK still exists? => false`), `try-write-lock!` → true,
   `LOCK` now names `"B-ALSO-LIVE"`. **Two live transactions hold the project lock simultaneously**,
   and A's `finish!` will later delete B's.
   *Fix:* make the break a compare-and-delete — rename `LOCK` to a unique breaker name and proceed
   only if the renamed file still names the stale holder — and make `release-lock!` verify the
   LOCK still names this txid before unlinking.

4. **OPEN (upgrade hazard) — a LOCK written by any earlier build carries a pid and nothing else,
   and a reused pid makes it unbreakable for ever.** `txn_journal.clj:355-374` — with
   `:start-ticks` and `:boot-id` absent, both mismatch clauses are skipped and only
   `:process-not-alive` can fire.
   **Witness (probe B4):** `{:txid "old-format" :pid <live child pid>}` → `begin!` →
   `{:error-type :txn-lock-held, :holder-live true, :holder-cause nil}`, and `recover!` will not
   break it either (`cause` is nil). Exactly round-2 blocker 1, reachable on any workspace whose
   LOCK predates `6ac3379`. There is no version field in the LOCK payload.
   *Fix:* treat a holder with no `:boot-id` as an old-format claim — either breakable, or a typed
   `:txn-lock-legacy-format` refusal naming `recover!` as the remedy — and stamp a format version.

5. **OPEN (honesty) — `O(1) in the target's size` is asserted in the contract, the docstring and
   the ratified MEM-007 clause, and this build's own numbers contradict it.**
   `txn_journal.clj:52` and `:8-9`; `docs/intent/memory/memory-transaction-specs.md` MEM-007.
   **Witness:** my medians above — 2 MiB is **1.9×** 1 KB after the change (570,537 ns residual size
   term). `docs/txn-journal.md:378-388` already says the size term is not zero and explains why;
   the contract value does not. MEM-014's new rule ("every statement it makes about an instrument
   shall be true of that instrument in general") is the rule this fails — the same class as the
   identity claim round 3 just narrowed. Say "the size term is reduced ~4×, and what remains is
   writeback, not a read" in all three places, or publish the measured residual beside the claim.

6. **OPEN (minor) — `:ctime-ns` degrades silently to `nil` and the receipt still advertises it.**
   `txn_journal.clj:218-222` swallows any `unix:ctime` failure; `:68` lists `:ctime-ns` in
   `:stat-fields` unconditionally. `ctime` is the only unforgeable field in the tuple (see the
   probe-G analysis), so where it is absent the guard is materially weaker and nothing says so.
   *Fix:* report the fields actually captured in the receipt, and refuse the fast path (always
   re-read) when `ctime` is unavailable.

7. **OPEN (minor) — the non-cooperating-writer table is not complete, and one site the opt-in
   cannot reach is missing from it.** `docs/txn-journal.md:76-89`. Every line number listed is
   exact (I checked all of them), but `src/clj_surgeon/extract.clj:242` and
   `src/clj_surgeon/intent_transaction.clj:2187` are absent, and
   `src/clj_surgeon/worktree_lifecycle_io.clj:399` defines a **private duplicate** `atomic-write!`
   (called at `:438`) which binding `*publish-lock-dir*` will silently not cover. MEM-007 now says
   the doc "shall name … every source-mutating site that does not [cooperate]", so this is a spec
   obligation, not tidiness.

8. **OPEN (minor) — `release-receipt!` reports `:txn-journal-missing` for a journal that exists
   with an unreadable lease.** `txn_journal.clj:1559-1571` reads the lease with `read-edn-file`
   rather than `read-lease`. **Witness (probe E):** with `lease.edn` deleted,
   `release-receipt!` → `{:ok false, :error-type :txn-journal-missing}` — and
   `release-receipt!` is precisely the `next_call` the `:txn-journal-referenced` refusal advertises
   (`:1531`). A caller following the remedy is told the journal does not exist when it does.

9. **OPEN (minor) — an in-flight transaction is described as having an unreadable lease.**
   `txn_journal.clj:1402-1419`: an `:open` transaction has not written a lease yet, so `read-lease`
   labels it `:lease :unreadable`. **Witness (probe E3):** `retained-transactions` →
   `{:txid "OPEN1", :status :open, :receipt-refs 1, :evictable false, :lease :unreadable}`, and
   `evict!` refuses with `:txn-lease-unreadable` and a message saying the lease "is missing or
   unparsable" when it was simply never written. Fail-closed direction is right; the type is wrong.
   Distinguish `:lease :not-yet-written` for a non-terminal status.

10. **OPEN (minor) — `undo!` of a target somebody deleted refuses with no verb behind its remedy.**
    `txn_journal.clj:1312` types it `{:conflict :missing}` and the refusal at `:1471-1480`
    says "reconcile that change first, or `forget!` this journal deliberately; the pre-images are
    still here." **Witness (probe F3):** target deleted after commit → `{:error-type
    :txn-undo-conflict, :files-written 0, :conflicts [{:conflict :missing, :expected-hash …}]}`,
    file **not** recreated. Refusing is correct — but for a deleted path there is no in-tool way to
    republish the pre-image, so "the pre-images are still here" is true and unusable. Either offer
    a `--force`/`:accept-missing` republish, or say in the remedy that recreation is manual.
