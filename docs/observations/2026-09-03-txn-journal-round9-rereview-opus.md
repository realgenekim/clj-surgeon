# Kernel round 9 — Opus executed re-check of bridge/txn-journal at 2df05b3 (2026-09-03T22:20Z)

Verdict: **GO — 2df05b3 replaces 5a2d254 as the merge point. BLOCKER: none.** Round-8 findings 1–4 and 6 CLOSED on the reviewer's own probes (6×20 storm: 0 lying lines, 0 orphans, was 188/240 and 40; a second storm shows no over-correction: 120 honest `:retained`, 20/20 kept); the attack on `:interrupted-break-vanished` found no deadlock (400-trial real race, 54 entries, 54/54 cleared by the follow-on recover!). Adoption-scoped notes: two key schemas under one `:resolution` word; no concurrent-shape witness in the suite (the storm lives only in reviews) — the first ratchet to build at adoption; kernel witnesses run only under mcp-test. Outside the branch: the mcp-test readiness-file flake diagnosed as a create-then-write race in `write-ready-file!` (mcp_http_server.clj:210-216) — inb-00d296. Gates match the builder: 472/4572/0 (after one flake re-run), 720/5976/0, oracle, 0 warnings.

## Opus verdict, verbatim

# txn-journal round 9 (`2df05b3`) — Opus executed re-review: **GO**

**Reviewer of record.** Opus, eighth consecutive round. Round-eight verdict under re-judgement:
`git show origin/main:docs/observations/2026-09-03-txn-journal-round8-rereview-opus.md`.

**Apparatus.** Worktree `/home/forge/tmp/sol/txn9-wt` at `2df05b3`
(`git rev-parse --short HEAD` => `2df05b3`; `git status --porcelain` and `git stash list` both
empty at start and at end; nothing committed, stashed or pushed; the reflog shows one checkout and
nothing else). Fixtures under `/tmp/txn9-fx-sol` only. Probes run as
`clojure -Sdeps '{:paths ["src" "test"]}' -M -e '(load-file …)'` from the worktree. Gates under
`/home/forge/bin/suite-run`; `make mcp-test`, `make memory-battery` and `make memory-red` were
never invoked; no port in 7888-7895 or 7906-7910 was contacted; every process signalled was one I
started (`sleep` children, `.destroyForcibly` in a `finally`).

**Headline.** All four OPEN findings from round eight are CLOSED against my own re-run probes, and
the worst of them is closed at the number that found it: my own 6x20 storm through the public verb
produced **0 lying lines and 0 minted orphan sidecars**, where the same shape at `5a2d254` produced
188 of 240 lines asserting `:evidence :retained` for a file that was not there and manufactured 40
orphans. The fix does not over-correct: a second storm over 20 markers that legitimately survive
returns 120 `:retained` lines, all true, and keeps all 20 files. Round nine's own addition —
`:interrupted-break-vanished` refusing the unlink — was attacked with a real 400-trial race that
entered the branch 54 times, and in **54 of 54** the ordinary break path cleared the dead holder's
LOCK in the follow-on `recover!`. There is no stranding and there is no BLOCKER. Adoption is still
zero. `2df05b3` should replace `5a2d254` as the merge point.

---

## Executed gates (mine, at `2df05b3`, under `/home/forge/bin/suite-run`)

```
$ suite-run bash -c 'cd …/txn9-wt && bb test/run_all.clj'
Ran 720 tests containing 5976 assertions.
0 failures, 0 errors.                                     TESTFAST_EXIT=0

$ suite-run bash -c '… swipl -q -f test/mcp_operation_contract_oracle.pl …'
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
                                                          ORACLE_EXIT=0
$ suite-run bash -c '… clojure -M test/kernel_warning_check.clj'
kernel warning check: 2 namespace(s), 0 warning(s)        KWC_EXIT=0

$ suite-run bash -c '… clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test'   (run 1)
Ran 472 tests containing 4558 assertions.
0 failures, 1 errors.                                     MCPTEST_EXIT=1
$ … (run 2, identical command)
Ran 472 tests containing 4572 assertions.
0 failures, 0 errors.                                     MCPTEST2_EXIT=0
```

Builder's stated counts: mcp-test 472/4572/0, test-fast 720/5976/0, oracle pass, warning check 0.
**Run 2 matches all four exactly.**

**The run-1 error, classified.** `prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire`
(`test/clj_surgeon/mcp_prepared_wire_test.clj:205`) threw
`NullPointerException: Cannot invoke "String.length()" because "this.input" is null` out of
`URI/create` at `:181`, because the readiness file's `:url` was nil. Root cause is a TOCTOU in the
test harness, not in the kernel: `write-ready-file!` (`src/clj_surgeon/mcp_http_server.clj:210-216`)
publishes readiness with `spit`, which CREATES then WRITES, while the test's `await!` predicate
(`mcp_prepared_wire_test.clj:50-58`, used at `:223`) is `.isFile ready-file` — so a reader that wins
the race slurps an empty file and reads nil. Load-sensitive; the box was at load ~2.5-5 with three
suite lanes and my probes running. It is **outside this branch's subject** and did not touch a
txn-journal witness. I record it because it is the exact defect class this kernel spent three rounds
eliminating — a non-atomic publish of a small state file — and the kernel's own answer
(`replace-sidecar!`, an `ATOMIC_MOVE` from a fully-written temp) is sitting one namespace away.
Follow-up, not a blocker.

**Adoption, re-derived at `2df05b3`.** `grep -rn 'txn-journal\|txn_journal' src/ --include=*.clj`
excluding the kernel returns two lines: `src/clj_surgeon/scope_stream.clj:24` (a require) and
`src/clj_surgeon/file_ops.clj:24` (a docstring mention). No `:txn/recover` operation is registered.
Merging cannot break anything that exists today. Every reference to the removed `:interrupted`
bucket key is prose or history, plus one witness asserting the key is gone
(`test/clj_surgeon/txn_journal_test.clj:2149`).

---

## Round eight's six findings, re-judged on my own re-run probes

### Finding 1 — `:evidence :retained` asserted for a tombstone that is not on disk, minting the orphan it will later report — **CLOSED**

`src/clj_surgeon/txn_journal.clj:751-757` (`stamp-broken-at!` is now wrapped in
`(when (.isFile tomb) …)`, so no sidecar is written beside an absent tombstone), `:770-777`
(`touch-tombstone!` returns `now` in the body and `nil` from the catch — the swallow is gone),
`:805-838` (`stamp-tombstone!` returns a TYPED result `{:evidence :retained|:vanished
:stamp :ok|:unrecorded|:absent :broken-at-ms ms}`, re-checking `.isFile` AFTER the stamp),
`:2874-2902` (the finish branch reads the stamp's word and refuses the unlink when the evidence is
gone), `:2926-2944` (the uncorroborated branch reads the same word). Witnesses:
`a-resolution-never-says-retained-for-a-file-it-did-not-keep` (`test:1955-2019`, three parts) and
`a-stamp-mints-no-sidecar-for-a-tombstone-that-is-not-there` (`test:2022-2056`).

**Witness (my 6x20 storm, probe STORM) — the E4 shape at round nine, through the public verb:**

```
STORM planted tombs = 20   (hard links to ONE live claim)
STORM total resolution lines = 120
STORM resolutions = {:interrupted-break-reverted 32, :interrupted-break-vanished 88}
STORM evidence words = {:removed 32, :vanished 88}
STORM LYING LINES (:evidence :retained, file NOT on disk) = 0
STORM tombstones on disk after = 0
STORM sidecars on disk after = 0
STORM ORPHAN SIDECARS (no tombstone) = 0 []
STORM LOCK still present? = true
STORM UNACCOUNTED names (absent AND no :evidence) = 0
```

At `5a2d254` the same shape produced 188 of 240 `:evidence :retained` lines for absent files and 40
orphan sidecars. The 88 lines that now read `:vanished` are the exact lines that used to lie, so the
probe demonstrably exercises the changed branch. The live claim was never touched.

**Witness (probe E1) — `resolve-interrupted-break!` handed a tombstone that is not there:**

```
E1 tombstone exists at call time? = false
E1 line = {:tombstone "LOCK.broken.GONE", :resolution :interrupted-break-vanished,
           :evidence :vanished, :cause :evidence-vanished, :stamp :absent,
           :holder-txid "LIVE", :holder-live true, :lock-present true}
E1 file named by the line on disk at return? = false
E1 would `:retained => on disk` hold? = true
E1b dead-holder branch, absent tombstone -> {… :resolution :interrupted-break-vanished,
           :evidence :vanished, :stamp :absent, :cause :evidence-vanished,
           :holder-txid "DEAD", :holder-cause :process-not-alive}
E1b LOCK still present? = true
```

At `5a2d254` this returned `:evidence :retained :stamp :ok` and minted `LOCK.broken-at.GONE`.

**Witness (probe STORM2) — the over-correction check the fix has to survive.** 20 uncorroborated
markers that legitimately stay, six concurrent `recover!`:

```
S2 lines = 120  resolutions = {:interrupted-break-uncorroborated 120}
S2 LYING (:retained, absent) = 0
S2 OVER-CORRECTED (:vanished, but file IS on disk at end) = 0
S2 tombs on disk after = 20 of 20    orphan sidecars = 0
S2 bucket = {… :remaining 20 … :found 20 …}    S2 LOCK present = true
```

The fix narrows nothing it should not: a file that is kept is still reported kept, and every one of
the 120 lines is true.

### Finding 2 — the `:sidecar-name-taken` remedy names the wrong cause — **CLOSED**

`:1324-1335` (the `:name-taken` refusal), `:1381-1416` (`break-refusal-remedy` now takes the LINE,
not the cause, and branches on `blocking-sidecar`), `:1420-1432` (`break-refusal-line` attaches the
remedy AT CONSTRUCTION, so it travels to `acquire-lock!`'s success path and `recover!`'s receipt as
well as the refusal branch that used to be the only surface that added one — `:1538`, `:3047`).
Witness: `a-refused-break-names-the-file-that-blocked-it-and-how-it-clears` (`test:1655+`).

**Witness (probe B1) — the round-eight probe, re-run:**

```
B1 orphan sidecar = [LOCK.broken-at.STABLE-TXID]  tombs = []
B1 SAME txid begin! refusal line = {:cause :evidence-unrecordable,
    :blocking-sidecar "LOCK.broken-at.STABLE-TXID",
    :remedy "The break claims its own marker file before it claims the evidence name, and that
      marker name was already taken: LOCK.broken-at.STABLE-TXID. It is an ORPHAN SIDECAR - what a
      break interrupted between its marker and its evidence name leaves behind - so the claim was
      not touched and nothing about this directory needs fixing. Break with a different :txid to
      proceed now; recovery retires the orphan on the published retention of 86400000 ms, after
      which this :txid breaks normally. Retrying with this :txid before then fails identically."}
B1 has :remedy at construction? = true
B1 LOCK still there? = true
B1 DIFFERENT txid proceeds now? lock-broken = {… :tombstone "LOCK.broken.OTHER-TXID",
                                               :break-path :link}
```

The remedy names the file, names the escape that works IMMEDIATELY (a different `:txid`), quotes the
retention as a number, and says plainly that retrying this `:txid` fails identically. House rule 17
is satisfied: an owner and an action. At `5a2d254` the same refusal sent the operator to `chmod` and
`df`.

### Finding 3 — `:evidence` overloaded; the invariant blind to the one refusal that names a file — **CLOSED**

`:1334` (`:blocking-sidecar` is its own key; `:evidence :sidecar-name-taken` is gone),
`test:1696-1704` (`receipt-file-keys` is `[:tombstone :blocking-sidecar :sidecar]` and the walk is
driven off it, so a key added later is covered on the day it appears), `test:2061-2110`
(`the-standing-invariant-sees-the-file-a-refusal-names`, which also FIRES: a forged
`:blocking-sidecar "LOCK.broken-at.NOT-THERE"` comes back UNACCOUNTED).

My probe: `B1 has :blocking-sidecar key? = true`, `B1 has :evidence key? = false`. The two meanings
are separated; the vacuous-pass path is closed.

### Finding 4 — `:interrupted` mixes corroborated with uncorroborated — **CLOSED**

`:1073-1077` (the sweep folds `interrupted-break-entries` into `frequencies` of the class),
`:1113-1114` (two keys), `:967-1000` (the docstring stating why the two rules are not equal
evidence). Witness: `the-sweep-counts-corroborated-and-uncorroborated-interrupted-breaks-apart`
(`test:2113+`), which also asserts the combined key is gone (`test:2149`).

**Witness (probe SPLIT) — 3 hard links to the present LOCK, 4 hand-dropped `:phase :linked` markers:**

```
SPLIT bucket = {… :interrupted-uncorroborated 4, :interrupted-corroborated 3 …}
SPLIT combined :interrupted key still present? = false
```

At `5a2d254` this read `:interrupted 7`.

### Finding 5 — the no-marker inode-only tombstone re-types on the holder's clean release — **NOTE, unchanged, still accepted**

`:1001-1024`. Nothing in the round-nine diff touches `interrupted-break-files` or the inode rule; it
remains a MIGRATION residual for tombstones written by builds that wrote no marker, named
deliberately in the docstring. Not re-probed; no change to judge.

### Finding 6 — the round-six property lives in `:status`, not `:kind` — **CLOSED**

`docs/intent/memory/memory-transaction-specs.md:117` adds the falsifier row: *"The round-six
property survives because the witness kept its name." It survives in `:status`
(`:uncorroborated-marker`) and NOT in `:kind` … so a consumer keying on `:kind`, which is what a
quota sweep or an eviction pass would do, cannot tell them apart. The field a reader must check is
named here because the witness's name no longer says it.* That is exactly the correction round eight
asked for, in the field name a reader is expected to check.

**Also landed in MEM-013, and correctly scoped.** The requirement sentence gains the four new
clauses (never construct `:retained` or a recorded stamp for an absent tombstone; no sidecar beside
an absent tombstone; no unlink to finish a break whose evidence is gone; the remedy names the
blocking file; every name a receipt carries is visible to its own walk; corroborated and
uncorroborated counted apart), the falsifier table gains five rows, and a new section, *"Open at the
adoption boundary (MEM-013)"*, writes down the two items the builder disclosed as NOT built — the
count cap and the `:unsafe-break-by-move` allowlist. Written down before adoption rather than
discovered after it is the right disposition, and the section says so in those words.

---

## The attack on round nine's addition: can `:interrupted-break-vanished` leave a dead holder's LOCK permanently unbreakable?

**No. Constructed as a real race, entered 54 times, and cleared 54 times.**

The question is sharp because the new branch (`:2874-2882`) genuinely REFUSES the unlink: handed a
dead holder whose tombstone is gone, it returns `:interrupted-break-vanished` and leaves the LOCK
where it is. If nothing else broke that LOCK, a concurrent recovery would have converted an honest
receipt into a deadlock.

**Structural answer first.** `dead?` requires `same-claim?` (`:2866-2868`), which requires
`lock-file-key tomb` to equal the LOCK's key — so the tombstone must exist AND share the LOCK's
inode at that moment. The vanished-inside-`dead?` state is therefore only reachable through the
window between `same-claim?` and `.isFile tomb`, which spans `read-lock-claim` and `stale-holder`'s
process-liveness check. It is a narrow TOCTOU, not a state a caller can simply hand in.

**Probe D — a real 400-trial race, no `with-redefs`, no hand-forgery.** Each trial: a LOCK with a
reaped pid, one tombstone hard-linked to it, then `resolve-interrupted-break!` and a deleter
released from one latch with the deleter's spin swept across 0-59 us. The dead-branch line is
identifiable by carrying `:holder-cause` and no `:lock-present`:

```
D trials = 400
D DEAD-branch :interrupted-break-vanished HITS = 54
D example line = {:tombstone "LOCK.broken.RACE", :resolution :interrupted-break-vanished,
                  :evidence :vanished, :stamp :absent, :cause :evidence-vanished,
                  :holder-txid "DEAD", :holder-cause :process-not-alive}
D of those hits, LOCK left on disk after a follow-on recover! = 0
D of those hits, LOCK cleared by the ordinary break path      = 54
```

**Probe A — the same question without a race, driven through the public verb.** A dead holder plus
five uncorroborated markers, so EVERY interrupted-break resolution declines to touch the LOCK:

```
A LOCK before = true  tombs = 5
A resolutions = {:interrupted-break-uncorroborated 5}
A :lock-broken = {:reason :stale-holder, :cause :process-not-alive, :holder-txid "DEAD",
                  :tombstone "LOCK.broken.recover-1788473501985-34d138ec", :break-path :link}
A :lock-break-refused = nil
A LOCK after = false
```

**Probe C — the state a correct concurrent revert leaves, resolved serially:**

```
C tombstone gone = true  LOCK present = true
C :interrupted-breaks = nil
C :lock-broken = {… :cause :process-not-alive, :tombstone "LOCK.broken.recover-…", :break-path :link}
C LOCK after = false
C a fresh begin! acquires? = true {:txid "1788473504009-76eb17c7"}
```

**Why it cannot deadlock, stated once.** `recover!` resolves interrupted breaks FIRST (`:3013-3017`)
and then runs the ordinary break path in the SAME call (`:3018-3033`), against a freshly minted
`recover-<ms>-<8 hex>` txid (`:3031`). That name can collide with neither an existing tombstone nor
an orphan sidecar, so neither of the two refusal causes (`:tombstone-exists`,
`:evidence-unrecordable`) is reachable for it, and `break-lock!` proceeds on any dead or unreadable
claim. The refusal round nine added is therefore fail-safe in the strict sense: it declines to
destroy a claim whose evidence is gone, and hands the same claim to a path that breaks it with
evidence of its own, in the same call. **Not a blocker.**

---

## Numbered findings (round nine's own)

1. **CLOSED — round-eight finding 1.** `:751-757`, `:770-777`, `:805-838`, `:2874-2902`,
   `:2926-2944`. My 6x20 storm: **0 lying lines and 0 minted orphans** against 188/240 and 40 at
   `5a2d254`; probe E1 both branches; probe STORM2 shows no over-correction (120 true `:retained`
   lines, 20 of 20 files kept). Witnesses `test:1955`, `test:2022`.

2. **CLOSED — round-eight finding 2.** `:1381-1416`, `:1420-1432`. The remedy names the orphan by
   file name, names the escape that works NOW (a different `:txid`, which I drove and which broke
   the lock immediately), and quotes the retention as a number. Attaching it at construction also
   fixes a defect round eight did not name: the remedy previously reached only ONE of the three
   surfaces that publish the refusal.

3. **CLOSED — round-eight finding 3.** `:1334`, `test:1696-1704`, `test:2061`. `:blocking-sidecar`
   is its own key, `:evidence` is absent from the refusal, and the walk that backs the standing
   invariant is driven off a named key list rather than a hard-coded `:tombstone`.

4. **CLOSED — round-eight finding 4.** `:1073-1077`, `:1113-1114`, `test:2113`. 3 corroborated / 4
   uncorroborated read apart; the combined key is removed and its absence is witnessed.

5. **NOTE, unchanged and still accepted — round-eight finding 5.** `:1001-1024`. Migration residual
   only; untouched by this diff.

6. **CLOSED — round-eight finding 6.** MEM-013 falsifier row names `:status`
   (`:uncorroborated-marker`) as the field a reader must check and says explicitly that the property
   does NOT live in `:kind`.

7. **NOTE (adoption-scoped, new) — one `:resolution` word, two key schemas.** The dead-holder
   `:interrupted-break-vanished` line (`:2875-2881`) carries `:holder-cause` and NO `:lock-present`
   or `:holder-live`; the uncorroborated one (`:2929-2944`) carries `:lock-present` and
   `:holder-live` and no `:holder-cause`. A consumer reading `:lock-present` off a vanished line
   gets `nil` in one shape and cannot distinguish an absent key from `false`. Measured in probes E1
   and D. Harmless while adoption is zero; it should be one shape, or the difference should be
   typed, before a verb reads these lines.

8. **NOTE (adoption-scoped, new) — no witness drives the CONCURRENT public-verb shape.** All five
   round-nine witnesses are single-threaded and hand-planted, and they are real witnesses (part (c)
   of `test:1955` reproduces exactly the state that returned `:retained` at `5a2d254`). But the
   defect was found by a storm and is verified by a storm only in this review. Round eight's own
   argument applies unchanged: the previous invariant `test:1744` was correct and never fired,
   because no scenario had a concurrent deleter. *Ratchet:* N `recover!` futures over one live claim,
   asserting zero lines with `:evidence :retained` for an absent file and zero orphan sidecars
   minted — the exact predicate my STORM probe computes.

9. **NOTE (new) — the kernel's witnesses run in ONE gate.** `test/run_all.clj` does not require
   `clj-surgeon.txn-journal-test`; only `test/clj_surgeon/mcp_test_runner.clj` does (`:39`, `:83`).
   So the whole subject of this branch is covered by `mcp-test` alone — the gate that flaked on my
   first run. The builder disclosed this; I record it because a single-gate subject plus a flaky
   gate is how a green becomes uninformative.

10. **NOTE (new, outside the branch) — the MCP readiness file is published non-atomically.**
    `src/clj_surgeon/mcp_http_server.clj:210-216` `spit`s the ready-file while
    `test/clj_surgeon/mcp_prepared_wire_test.clj:50-58,223` waits on `.isFile` — a create-then-write
    race that cost one gate run an error (NPE at `URI/create`, `:url` nil). Not a kernel defect and
    not a merge blocker; it is the same class the kernel fixed with `replace-sidecar!`'s
    `ATOMIC_MOVE`, and the fix is either that or a readiness predicate that parses.

11. **Carry forward, unchanged, now WRITTEN DOWN rather than built (as disclosed):** the evidence
    directory is bounded by RETENTION and not by COUNT, and `:unsafe-break-by-move` is protected by
    its NAME only. Both are recorded in MEM-013's new "Open at the adoption boundary" section, with
    the round-three findings 6/8/9/10 named alongside. Correct disposition for a kernel whose next
    step is adoption; they become requirements on the day a verb forwards caller-supplied keys.

---

## Verdict

# GO

**Yes — `2df05b3` should replace `5a2d254` as the merge point.**

*Why.* Every OPEN finding from round eight is closed against my own probes, and the one that
mattered is closed at the measurement that found it — 188 dishonest lines and 40 manufactured
orphans became 0 and 0, under six concurrent recoveries through the public verb, with no
over-correction on the 120 lines that are honestly `:retained`. The refusal round nine added was
attacked as a deadlock and is not one: 54 genuine race entries, 54 clean recoveries, and the reason
is structural rather than lucky. Gates match the builder exactly on a clean run; the single error I
saw is a readiness-file race in an unrelated HTTP wire test, reproduced-clean on re-run and
diagnosed to a line. Adoption is zero, so merging can break nothing that exists.

**BLOCKER: none.**

**Adoption-scoped follow-ups** (items 7, 8, 9, 10, 11 above): unify the two
`:interrupted-break-vanished` key shapes; add the concurrency ratchet to the suite; give the kernel
witnesses a second gate; make the MCP readiness publish atomic; and convert the two written-down
boundary rules into refusals when a verb first forwards caller-supplied keys. None of these should
hold the merge, and item 8 is the one I would build first — it is the ratchet for the defect this
round exists to fix.
