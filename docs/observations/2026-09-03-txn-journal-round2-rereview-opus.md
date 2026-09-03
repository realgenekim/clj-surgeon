# txn-journal 7c9a9b1 (kernel round 2) — Opus executed re-check (Sol filter fallback): GO-WITH-FIX, three blocker-class before adoption (stale LOCK deadlock; lease fails open; undo! unlocked) — round 3 launched

# txn-journal round 2 — Opus executed re-review at `7c9a9b1`: GO-WITH-FIX for the merge queue (kernel adopted by no verb), 8 items, 2 of them blockers before any verb adopts it

**Reviewer of record.** OpenAI's content filter refused this round; this is the Opus fallback
re-review, executed independently. Scratch clone at `/home/forge/tmp/opus-txn` (checked out
`7c9a9b1`, never committed, never pushed). Fixtures under `/home/forge/tmp/opus-txn-fx`. All six
of Sol's round-one blockers are CLOSED against my own injections. Round 2 introduced two new
defects of its own — a stale project lock whose advertised remedy does not clear it, and a
retention refcount that fails OPEN when its lease file is missing — plus the standing fact that
`with-publish-lock*` has exactly one call site in the whole repository, so today the advisory
lock excludes nobody who exists.

## Executed gates (this reviewer, at `7c9a9b1`)

| gate | how | result | builder's claim | agrees |
|---|---|---|---|---|
| `make memory-red` | `flock /home/forge/tmp/suite.lock`, run **once** | **4 tests / 25 assertions / 0 failures / 0 errors** | 4/25/0 | yes |
| `make test-fast` | `suite-run` | **720 tests / 5976 assertions / 0 / 0** | 720/5976/0 | yes |
| `make mcp-test` | `suite-run` | **420 tests / 4201 assertions / 0 / 0** | 420/4201/0 | yes |
| `mcp-operation-oracle` | inside `mcp-test` | `pass; legacy counterexamples=[verification_failed,verification_pending]` | pass | yes |
| `make memory-battery-self-test` | `suite-run` | **18 tests / 64 assertions / 0 / 0** | 18/64/0 | yes |
| `make txn-kernel-warning-check` | inside `mcp-test` **and** standalone | `2 namespace(s), 0 warning(s)` both times | 0 | yes |

No full battery was run. Every number above is from my own run, not read from a builder log.

### `make memory-red`, verbatim

```text
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/memory-test

Testing clj-surgeon.memory.oom-reproduction-test
CONTROL scope: {:root /home/forge/tmp/clj-surgeon-memory-control-1788424269304, :files 8, :bytes 4196920}
CONTROL receipt: {:arm :frozen-read, :files 8, :bytes 4196920, :result-hash-count 8, :tree-hash f1ff28cb792b72d27c6b91e884a55487ef9fb435b28f807bbd4724859c637ae6, :memory {:xmx-mb 256.0, :heap-used-start-mb 17.469482421875, :heap-used-peak-mb 232.77477264404297, :heap-used-end-mb 12.241256713867188, :heap-after-gc-peak-mb 171.5541000366211, :heap-retained-peak-mb 0.0, :wall-ms 2341}}
RED scope: {:root /home/forge/tmp/clj-surgeon-memory-frozen-1788424273956, :files 600, :bytes 314772270}
RED exit: 3
RED err:
RED out: Terminating due to java.lang.OutOfMemoryError: Java heap space


Testing clj-surgeon.memory.journal-green-test
GREEN scope: {:root /home/forge/tmp/clj-surgeon-green-journal-1788424277142, :files 600, :bytes 314772270}
GREEN reference receipt: {:arm :frozen-read, :files 600, :bytes 314772270, :result-hash-count 600, :tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :memory {:xmx-mb 2048.0, :heap-used-start-mb 19.081146240234375, :heap-used-peak-mb 2046.7608795166016, :heap-used-end-mb 12.438667297363281, :heap-after-gc-peak-mb 1736.629005432129, :heap-retained-peak-mb 0.0, :wall-ms 150666}}
GREEN journal receipt: {:tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :arm :journal, :work {:walk-entries 604, :files-discovered 600, :files-read 600, :source-bytes 314772270, :largest-file-bytes 524621, :receipt-records 0, :receipt-bytes 0}, :memory {:xmx-mb 256.0, :heap-used-start-mb 36.368873596191406, :heap-used-peak-mb 253.80926513671875, :heap-used-end-mb 10.938331604003906, :heap-after-gc-peak-mb 202.89849090576172, :heap-retained-peak-mb 14.279563903808594, :wall-ms 170743}, :read-set-files 600, :commit-error nil, :committed true, :refusals [], :files 600, :reserved {:staged-files 600, :aggregate-bytes 314772270, :heap-reserved-peak-bytes 29446956, :path-list-bytes 68180, :journal-bytes-max 1073741824, :staged-files-max 2000, :aggregate-bytes-max 1073741824, :journal-bytes-peak 629544540, :journal-bytes 629544540, :work-budget-bytes 201326592, :discovered-files 600, :parse-factor 56}, :files-written 600}
GREEN err:
FLATNESS 60 {:xmx-mb 256.0, :heap-used-start-mb 37.59193420410156, :heap-used-peak-mb 252.87520599365234, :heap-used-end-mb 10.930549621582031, :heap-after-gc-peak-mb 245.84609985351562, :heap-retained-peak-mb 11.987800598144531, :wall-ms 16805}
FLATNESS 600 {:xmx-mb 256.0, :heap-used-start-mb 52.31791687011719, :heap-used-peak-mb 254.11409759521484, :heap-used-end-mb 11.016189575195312, :heap-after-gc-peak-mb 244.67450714111328, :heap-retained-peak-mb 14.515296936035156, :wall-ms 174909}

Ran 4 tests containing 25 assertions.
0 failures, 0 errors.
```

**The two fix commits.** `dfcfbfe` ("fix an unbalanced quote in the green arm's docstring") changed
exactly one line of `test/clj_surgeon/memory/journal_green_test.clj:13`, replacing a bare `"` with
backticks; before it, `make memory-red` died in `ns`-form macroexpansion in ~2 s and measured
nothing. **The numbers in this report are from my own single run at `7c9a9b1`, not from either
builder run**, and they reproduce the second-run figures within ordinary GC variance (retained
peak 14.28 MB vs the filed 14.40; `heap-reserved-peak-bytes` and `path-list-bytes` are identical to
the byte). The first, 2-second run consumed no measurement, and the record says so — correct
practice.

## Sol's fourteen, re-judged

### 1. Undetected recheck→rename race — **CLOSED (narrowed honestly, and the narrowing is real)**

`src/clj_surgeon/txn_journal.clj:897-939`. Sol's injection now lands before the recheck and is
refused.

- **My re-run (P1a):** writer spits `THEIRS` in `:before-recheck` →
  `:error-type :txn-conflict`, `:conflict :digest`, `:files-written 0`, `:rolled-back true`,
  `:recovery []`, and the other writer's bytes survive on disk untouched.
- **My re-run (P1c, clean two-file commit):** receipt carries
  `:commit-window {:ops [:recheck-digest :recheck-identity :journal-write-begin :rename],
  :staging-copy-inside false, :lock :workspace-publish-lock, :max-ns 1025909}` — the ops list is
  the real one and `:max-ns` is measured, not asserted.
- **My re-run (P1b, the residual):** a writer injected *inside* the window (`:in-commit-window`)
  **is silently overwritten** — commit returns `:ok true`, `:files-written 1`, disk holds `:ours`.
  The window is real and the contract says so rather than claiming prevention.

**Judging the MEM-007 clause.** The clause is accurate about ordering and about the outcome. Two
qualifications:

- **It is not the minimal window the JVM allows, and it is not O(1).** The digest recheck inside
  the lock is a *full re-read of the target file* (`sha256-file`, `txn_journal.clj:124-135`), so the
  window scales with file size. Measured, same machine, same run: **1 KB target → `:max-ns`
  846,048; 2 MiB target → `:max-ns` 3,009,022.** `:staging-copy-inside false` is literally true —
  no byte *copying* — but "a digest read" in the contract text (`txn_journal.clj:49`) reads like a
  stat and is an O(size) read. The `write-begin` fsync must stay inside (intent must be durable
  before the rename), so ~0.85 ms is the floor; the file-size term is avoidable in principle
  (recheck via `mtime`+`size`+`ctime` under the lock, full digest before) and is not today.
- **Every writer in this repository is a non-cooperating writer by construction** (see finding 4
  below). So the residual window is not the exceptional case; today it is the only case.

### 2. Count-only scope membership — **CLOSED**

`txn_journal.clj:176-194` folds path + NOFOLLOW kind + file-key into one digest;
`txn_journal.clj:777-789` compares the digest, not the count.
**My re-run (P2b):** scope of two files, one non-read-set member deleted and a different one
created — equal count — gives `:error-type :txn-scope-membership-changed`, `:planned-files 2`,
`:observed-files 2`, `:files-written 0`. A count comparison would have passed this.

### 3. Deleted read-set file — **still CLOSED.** Reproduced incidentally in P2: deleting a read-set
member yields `:txn-conflict` with `:files-written 0` and the write-set file byte-unchanged.

### 4. Lexical `..` refused pre-canonicalisation — **CLOSED**

`txn_journal.clj:554-604`, refusal at `:574-578` on the RAW string.
**My re-run (P3):** `<root>/src/../src/a.clj` → both `pin!` and `stage!` return
`:txn-path-outside-workspace` with `:cause :lexical-parent-traversal`; `objects/` and `staging/`
are both empty afterwards and the target bytes are unchanged.

### 5. Identity not pinned — **CLOSED at both checkpoints, with one honest narrowing**

`pin!` records `path-identity` (`txn_journal.clj:150-170`, `:631/:635`); `identity-drift`
(`:726-734`) runs in `revalidate!`; `publish-one!` rechecks identity again inside the lock
(`:921/:927-928`).

- **My re-run (P4a, revalidation):** regular file replaced by a symlink to a byte-identical twin →
  `revalidate!` returns `:conflict :identity-changed`, `expected {:kind :regular …}`,
  `actual {:kind :symlink …}`; `commit!` refuses with `:files-written 0`.
- **My re-run (P4b, pre-rename):** the same swap injected in `:before-recheck` on the *second* of
  two files → `:conflict :identity-changed`, `:files-written 1`, `:rolled-back true`, and the
  first file verified back to H0.
- **Narrowing (P4c).** `fileKey` is a (device, inode) pair with **no generation counter**. I deleted
  a pinned regular file and recreated it with identical bytes; ext4 handed back the *same inode*
  (`(dev=801,ino=32353490)` before and after), `:kind` was unchanged, and the commit **succeeded
  with no conflict**. The docstring's claim — "Two files can hold identical bytes and still not be
  the same file; a content digest cannot tell them apart and this can" — is true for the symlink
  case Sol raised and **false in general**. The exposure is small (the bytes still equal H0), but
  the docstring overstates the instrument. Add `ctime` to the identity tuple, or narrow the claim
  to *type* changes plus inode changes the OS happens to expose.

### 6. Battery ignores the accountant — **CLOSED, and the arithmetic reproduces to the byte**

`memory_battery.clj:59` reads only `:reserved`/`:resources` and is deliberately blind to the
sampled peak; `memory_battery_runner.clj:208/222` records it per reading; `:247` aggregates;
`memory_battery.clj:165-173` renders `nil` as `:unmeasured`, never a pass.

- **Attributable per op:** yes. `reserved-peak-over-budget` is evaluated per op from that op's own
  cells (`memory_battery.clj:164-165`); only `:journal-scope-stream` reports a block, and the other
  four arms stay UNMEASURED rather than borrowing it.
- **The sum reproduces exactly.** From my own run: `:heap-reserved-peak-bytes 29446956`,
  `:path-list-bytes 68180`. **29,378,776 + 68,180 = 29,446,956.** ✓ — Sol's pre-fix figure plus the
  newly charged path list, unchanged to the byte.

### 7. Reservation completeness (the retained path list) — **CLOSED**

`scope_stream.clj:70-82` charges it; `:268-284` refuses a path list that does not fit the work
budget **before the first source is read**; `:302-308` publishes `:path-list-bytes` and
`:discovered-files` in the receipt. Witnessed at `test/clj_surgeon/scope_stream_test.clj:220-263`
including the at-limit/one-past pair.

### 8. MEM-001 two authorities — **CLOSED**

`docs/intent/memory/memory-transaction-specs.md:28-45` now carries a `[D]` deferred row that states
no requirement and demands no witness;
`docs/intent/memory-boundedness/memory-boundedness-specs.md:38` holds the sole unchecked statement.
One id, one authority.

### 9. Reflection and boxed math — **CLOSED**

`test/kernel_warning_check.clj` reloads both kernel namespaces under `*warn-on-reflection*` and
`*unchecked-math* :warn-on-boxed` and exits non-zero on any line.
**My re-run:** `kernel warning check: 2 namespace(s), 0 warning(s)`, both inside `make mcp-test`
and standalone. On the Make-ordering caveat: `Makefile:185-187` runs the suite first and the check
second, so a failing suite *would* hide the check — the ordering is real, but it hides a *green*
check behind a *red* suite, which fails loud either way. Not a defect; I ran it standalone to be
sure.

### 10. Journal quota wording — **CLOSED as a derivation, NARROWED as an invariant**

`txn_journal.clj:86-108` derives `max-journal-bytes = 2 × scope-stream max-aggregate-bytes` =
1 GiB, witnessed at `test/clj_surgeon/txn_journal_test.clj:78-91`, and
`test/clj_surgeon/memory/journal_child.clj:34-36` now takes **no journal override at all** — the
green arm's receipt reports `:journal-bytes-max 1073741824`, the shipped default. That is a genuine
fix and the strongest single item in round 2.

**Is 2× the right derivation?** For the failure it was built to kill — "a scope the read path admits
is one the journal refuses to stage" — yes, and it is the *tight* bound for that failure: the worst
case is that every admitted byte is rewritten, costing one pre-image plus one future image.
Three caveats:

- It bounds the **whole aggregate**, not the **write set**. Nothing requires the write set to be a
  subset of the read set (`stage!` needs a `pin!`, never a `record-read!`), so the derivation's
  premise is not enforced; it survives only because the quota is a hard ceiling regardless.
- It is slack in the common case: rewriting three files of a 512 MiB scope reserves nothing near
  1 GiB, so the quota is over-provisioned by orders of magnitude in ordinary use. That is the safe
  direction, and it should be said in the doc rather than left to be inferred.
- **The invariant is enforced only between the two DEFAULTS.** At the hard maxima it is violated by
  construction — `scope_stream.clj:54` allows `max-aggregate-bytes` 8 GiB while
  `txn_journal.clj:113` caps `max-journal-bytes` at 4 GiB, so `quota ≥ 2 × aggregate` is
  *unsatisfiable* at the ceiling. Nothing refuses that configuration at `begin!`; it surfaces
  later as a per-file quota refusal. See finding 6 below for the same violation appearing in the
  green arm's own configuration.

## Round 2's own additions — what I hunted, and what I found

- **`PUBLISH.lock` ordering vs the project `LOCK`: no deadlock.** The order is always `LOCK`
  (`begin!`, `txn_journal.clj:255-273`) then `PUBLISH.lock` (`commit!`, `:917`), never the reverse,
  and there is exactly one `with-publish-lock*` call site. No inversion exists.
- **`PUBLISH.lock` is genuinely cross-process, and cannot go stale.** I held it from this JVM and
  ran a *second* clojure process through a full commit against the same workspace: the child
  blocked **23,985 ms**, the target stayed at H0 for the whole hold, and it committed the instant I
  released. `FileChannel.lock` is an OS advisory lock, released by the kernel on process death —
  a crash leaves the file, never the lock. The leftover zero-byte `PUBLISH.lock` file in the
  transactions dir is inert and does not pollute `retained-transactions` (directories only).
- **`prepare-publish!` temporaries do not leak.** A `publish-fn` that throws after prepare leaves
  nothing: `publish-one!`'s `finally` (`:938-939`) deletes the temp, and `src/` afterwards contained
  only `a.clj`. A temp *orphaned* by a real crash is removed by recovery's sibling sweep
  (`:1097-1102`) — I planted `.clj-surgeon-publish-orphan.tmp`, forced `state.edn` back to `:open`,
  ran `recover!`, and it was gone with the target restored to H0.
- **`evict!` refuses BEFORE it writes.** `discard-journal!` (`:1165-1196`) checks status and refcount
  *before* `delete-tree!`. After a refused eviction the directory and both pinned objects were still
  present. Confirmed.
- **Retention, undo, forget, failed restore, lease refcount — all confirmed by my own runs.**
  Committed journal retained with `objects/` intact and `staging/` reclaimed; `lease.edn`
  `{:status :committed :receipt-refs 1 :evictable true}`; `evict!` → `:txn-journal-referenced`;
  `undo!` restored both files to H0 with every path `:verified`; `release-receipt!` then `evict!`
  succeeded and a later `undo!` returned `:txn-journal-missing` rather than pretending. A failed
  restoration (pinned objects destroyed mid-flight) produced `:status :restore-failed`,
  `:evictable false`, and **both** `forget!` and `evict!` refused with `:txn-journal-retained`.

## Numbered findings (round 2's own)

1. **BLOCKER (adoption) — a stale project `LOCK` is a permanent workspace deadlock, and the remedy
   the refusal names does not clear it.** `src/clj_surgeon/txn_journal.clj:1274` —
   `(when (seq results) (release-lock! transactions))`. **Witness:** I planted a `LOCK` naming a dead
   pid (`{:txid "ghost-1" :pid 999999}`) with no transaction directory beside it; `begin!` refused
   `:txn-lock-held` with `:next_call {:op :txn/recover}`; `recover!` returned
   `{:ok true :transactions-recovered 0}`, **left the LOCK in place**, and the next `begin!` refused
   again — forever. Nothing anywhere checks whether `:pid` is alive (`:255-273` records it and never
   reads it back). A crash after the transaction directory is finished, or any hand-cleaned journal,
   strands the workspace with no in-tool recovery path. Fix: have `recover!` release a lock whose
   holder pid is not live (`ProcessHandle/of`), or make the refusal say so.

2. **BLOCKER (adoption) — the retention refcount fails OPEN when `lease.edn` is missing.**
   `txn_journal.clj:1185` reads `(:receipt-refs lease 0)` and `:1143` reads `(:evictable lease true)`;
   both default a *committed* journal to "nobody holds it, sweep it." **Witness:** I deleted
   `lease.edn` by hand from a committed journal; `retained-transactions` reported
   `{:status :committed :receipt-refs 0 :evictable true}` (status recovered from `state.edn`, refcount
   silently invented), a quota-driven `evict!` returned `{:ok true :forgotten true}` and destroyed the
   pre-images, and the receipt became permanently un-undoable (`undo!` → `:txn-journal-missing`).
   The status is recoverable from `state.edn` and the refcount is not — so a missing lease beside a
   `:committed` state must default to `receipt-refs 1` / `:evictable false`, or be a typed refusal.
   Sol's blocker 10 is otherwise closed; this is the one door left open in it.

3. **BLOCKER (adoption) — `undo!` and `recover!` write with neither the publish lock nor any digest
   recheck.** `txn_journal.clj:1146-1163` → `restore-from-journal!` (`:1060`) → `publish-file!`
   (`:1086`), and `recover!` at `:1256`. **Witness:** I held `PUBLISH.lock` from this JVM, wrote
   `SOMEBODY-ELSE\n` over a committed target, then called `undo!` — it returned `{:ok true}` in
   **2.5 ms** and clobbered that write with H0 without blocking and without noticing. `undo!` is a
   live user-facing operation, not a startup path; it is exactly the "writer that ignores the lock"
   the contract warns about, implemented inside the kernel that warns about it. At minimum `undo!`
   must take `with-publish-lock*`; ideally it should refuse (or type) a target whose current digest
   is neither H0 nor the committed result.

4. **OPEN (documented-scope, must be named in the adoption plan) — `with-publish-lock*` has exactly
   one call site in the entire repository, so the advisory lock excludes nobody who exists today.**
   `txn_journal.clj:917` is the only caller. Every other source-mutating path in this repo writes
   through `file-ops/atomic-write!` / `atomic-create!` (`src/clj_surgeon/file_ops.clj:20`, `:85`) and
   takes no lock: `extract.clj:543-544,594,674,689,695`, `move.clj:434`,
   `structural_lens.clj:1313,1048,1077`, `mcp_change_buffer.clj:1558`, `mcp_extraction.clj:435,543`,
   `intent_transaction.clj:2186`, `workspace_onboarding.clj:236,261,383`, `agent_routing.clj:124`,
   `mcp_cold_verify.clj:56`, `mcp_tool.clj:381` — plus the kernel's own `restore-path!` (`:799`) and
   `restore-from-journal!` (`:1086`). **Every one of them is a non-cooperating writer by
   construction.** That does not make the contract dishonest — it says exactly this — but it means
   the "excludes any writer that ASKS for it" clause currently has an empty referent, and the
   residual window is the normal path rather than the exception. The adoption build must either move
   these onto the publish lock or state, in `docs/txn-journal.md`, that it does not.

5. **OPEN (minor) — the project `LOCK` is scoped to the state-home, not to the workspace.**
   `begin!` locks `workspace/transactions-dir root state-home` (`:321-324`). **Witness:** two
   transactions on one workspace root with two different `:state-home` values both acquired a lock
   and both reached commit; the second was caught only by the optimistic digest recheck
   (`:txn-conflict`), never by exclusion, and their `PUBLISH.lock` files were different files. The
   optimistic layer held, which is the design — but `contract`'s "the transaction lock" reads as
   workspace-scoped and is state-home-scoped. One sentence in `docs/txn-journal.md`.

6. **OPEN (minor) — the green arm still raises the READER's aggregate ceiling, producing exactly
   the configuration the new derivation forbids.** `test/clj_surgeon/memory/journal_child.clj:58`
   passes `:max-aggregate-bytes (* 1024 1024 1024)`. The journal override is genuinely gone
   (`:34-36`), and the docstring's claim is about the journal quota — but the published receipt reads
   `:aggregate-bytes-max 1073741824` beside `:journal-bytes-max 1073741824`, i.e. `quota = aggregate`
   where the rule demands `quota ≥ 2 × aggregate`. The 314 MB scope fits the *default* 512 MiB
   reader ceiling, so the override buys nothing and can simply be deleted — which would make the
   arm's receipt an honest witness of the derivation instead of a counterexample to it. Ratchet:
   assert the invariant against the *effective* limits, not only the two defaults
   (`txn_journal_test.clj:84`).

7. **OPEN (minor) — the battery-shape witness asserts a number that is 74 bytes wrong and passes by
   rounding.** `test/clj_surgeon/scope_stream_test.clj:283` asserts
   `(= (battery/bytes->mb (* 3000 56)) observed)`, but the reserved peak is now
   `path-list-bytes + 3000×56` = 168,074 B. Both round to 0.2 MiB at `bytes->mb`'s one-decimal
   granularity, so it passes. It would keep passing if path-list accounting were removed — the exact
   regression finding 7 of round one exists to bind. Assert `(+ path-list (* 3000 56))`.

8. **OPEN (minor) — recovery's orphan sweep deletes any `.clj-surgeon-publish-*` sibling, including a
   live transaction's.** `txn_journal.clj:1097-1102` walks the parent directory of every `write-begin`
   path and deletes every file with that prefix. Combined with finding 5 (two state homes → no mutual
   exclusion), one workspace's recovery can delete another in-flight transaction's *prepared* temp
   between its `prepare-publish!` and its rename. Bound the sweep to temps this journal's own
   transaction created (record the temp name in the journal beside `write-begin`).

## Verdict

**GO-WITH-FIX** for the mayor's merge queue, as a kernel adopted by no verb.

*For merging:* all six of Sol's blockers are closed against my own injections rather than against
the builder's; every gate reproduces the builder's numbers exactly; the memory result is real
(RED OOM at `-Xmx256m`, GREEN commits 600 files at the same ceiling with a byte-identical tree hash,
retention flat at 12.0/14.3/14.5 MB across 60 and 600 files); the derivation fix removed a real
override rather than re-describing one; and the eight items above live entirely inside two
namespaces that nothing calls. Merging cannot break anything that exists.

*Against merging as-is:* findings 1, 2 and 3 are the same class the kernel was built to eliminate —
a recovery path that does not recover, a refcount that fails open, and a write path inside the
kernel that ignores the kernel's own lock. Each is small and each is a house-rules ratchet case
(`every fix earns a ratchet`; `fail closed is only half the rule`).

**Condition on the merge:** findings 1, 2 and 3 are fixed with witnesses, or filed as named
adoption blockers with bead ids before any verb calls `txn-journal`. Findings 4 and 5 are
documentation obligations on `docs/txn-journal.md` in the same change. Findings 6, 7 and 8 are
cleanups that can ride the adoption build.
