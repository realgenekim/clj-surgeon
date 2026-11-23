# q5z-alias-migration f51ceae — Opus executed round-9 re-check: GO (the builder's dispute upheld; reviewer item withdrawn with its own reproduction)

# q5z-alias-migration f51ceae — Opus executed round-9 (round-10) re-review: **GO**

**Opus-first branch, not a fallback.** This spec family's probes are symlink races, `.git`
receipt-directory writes and receipt-name pinning; OpenAI's content filter refuses those fixtures,
so rounds 7, 8 and 9 are reviewed by Opus by design rather than by escalation.

Executed independently in a scratch clone at
`f51ceae3fa623ab62b3add92e0f9fe45859dadf2` (11 commits after `b119bef`). Nothing committed,
stashed, or pushed; no server started; no port in 7888–7895 or 7906 contacted; `make mcp-test`
avoided in favour of `clojure -M:clj-surgeon/mcp-test` directly, so the make-target's trailing
self-tests are outside this receipt. All JVM suites under `/home/forge/bin/suite-run`. Fixtures and
probes under `/home/forge/tmp/opus-q5z3-fx`. Working tree verified clean at `f51ceae` at exit
(`git status --porcelain` empty).

*Delivery note, not a finding:* at the start of this review `git ls-remote` showed GitHub's
`bridge/q5z-alias-migration` at `b119bef`; it now reads `f51ceae`. The push landed mid-review. The
mayor should confirm the queue is pointed at `f51ceae`, not the tip my first fetch saw.

---

## Gates, re-run once each under `suite-run`

| gate | this run | builder's claim |
|---|---|---|
| `clojure -M:clj-surgeon/mcp-test` | **452 tests, 5198 assertions, 0 failures, 0 errors** | 452 / 5198 / 0 — match |
| `bb test/run_all.clj` (test-fast) | **735 tests, 6263 assertions, 0 failures, 0 errors** | 735 / 6263 / 0 — match |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | **pass**, exit 0 (`legacy counterexamples=[verification_failed,verification_pending]`) | pass — match |

## The witnesses were red first

Each red commit checked out and its own namespace run. Every one fails there:

| red commit | failing deftest | failures |
|---|---|---|
| `16cef25` | `a-post-write-refusal-says-a-mutation-was-attempted` | 2 |
| `a0484f3` | `a-mid-migration-refusal-sends-the-caller-to-the-receipt-not-a-retry` | 1 |
| `95d3c39` | `a-partial-rollback-counts-the-files-it-left-migrated` (4) + `a-rollback-with-no-per-file-answer-still-over-states-the-migration` (1) | 5 |
| `c0753c3` | `a-control-directory-outside-the-workspace-is-not-called-the-workspaces` | 1 |
| `2e3db13` | `the-effect-inventory-attributes-each-effect-to-the-form-that-names-it` | 3 |

`c08921c` carries no red commit, correctly: its two tests
(`mcp_alias_migration_test.clj:2432`, `:2484`) PIN behaviour that was already right. They are a
ratchet against my round-8 item 4, not a defect witness, and labelling that commit
"REFUSED AS SPECIFIED" rather than manufacturing a red is the honest form.

---

# I. The dispute over round-8 item 4 — **THE BUILDER IS RIGHT. I WAS WRONG.**

My round-8 item 4 said `rolled-back?` reading only `(:ok rollback)` produced a **false RED** and
asked for the one-line `(or (:ok rollback) (:rolled-back rollback))`. I reproduced the builder's
scenario from scratch — my own harness, not the branch's test — and **the tree says the builder is
right and my requested reading would have published a restored tree over a fully migrated one.**

## The reproduction

`/home/forge/tmp/opus-q5z3-fx/probe1.clj`: build the fixture workspace, run a real
`alias-migration/execute!` (12 files migrated), then run a REAL `transaction/execute-undo!` whose
seventh `file-ops/atomic-write!` throws `IOException`. Nothing faked, nothing stubbed but the one
write.

```
=== MIGRATION ===
ok= true    still-migrated after migration = 12

=== KERNEL RESULT OF THE UNDO ===
keys         = (:cause-error :cause-error-type :error :error-type :recovery :rolled-back)
:ok          = nil
:rolled-back = true
:error       = "Transaction write failed; all files restored"
:error-type  = :transaction-write-failed
recovery statuses = {:original 6, :restored 6}

=== TREE AFTER THE FAILED UNDO ===
still-migrated (differs from :pre) = 12
matching :post (MIGRATED bytes)    = 24
matching :pre  (RESTORED bytes)    = 12

=== WHAT EACH READING WOULD PUBLISH ===
builder  (:ok only)             rolled_back = false
round-8  (or :ok :rolled-back)  rolled_back = true
```

**The tree is the evidence: twelve files are still migrated.** The kernel's own prose on that
result — "all files restored" — is TRUE *of the undo transaction*, and the undo transaction's
restoration is the migration's re-application.

## The mechanism, read out of the kernel rather than inferred

`intent_transaction.clj:1975-2003` (`recovery-result`) closes it. Recovery compares the file on
disk against the transaction's own `source-hash` (what THIS transaction read) and `result-hash`
(what it was writing), and on a match with `result-hash` it writes `originals` back:

- for an UNDO, `originals` **are the migrated bytes**;
- `:original` = the undo's write never landed → **still migrated**;
- `:restored` = the undo's write landed and was written back to `originals` → **migrated again**;
- `recovered?` (`:2015`) is true when every status is in `#{:original :restored :absent :deleted
  :present}`, and `commit-compiled!` (`:2288-2299`) then mints
  `{:error "Transaction write failed; all files restored" :rolled-back true}` **with no `:ok`.**

So `:rolled-back true` on an `execute-undo!` result means *the undo was reversed*. My round-8
reading would have turned that into `rolled_back true`, `source_unchanged true`,
`files_still_migrated 0` over a tree with twelve migrated files — **the exact false green this whole
requirement family exists to forbid, and the mirror of round 7's defect rather than its correction.**

**Verdict: item 4 is WITHDRAWN.** It was not a false RED; `rolled_back false` over that shape is the
true answer. The builder was right to refuse it, right to refuse it with a test rather than a note,
and right to give the reading a name — `undo-restored-the-migration?`
(`mcp_alias_migration.clj:1151-1169`) — so the question is asked once in words instead of
re-derived at three call sites. The status mapping
`undo-recovery-unmigrated-statuses #{:restore-failed}` (`:1172-1188`) follows from the same
mechanism and is correct: `:restore-failed` is the only status that leaves pre-migration bytes on
disk.

**What I got right, and it matters:** my round-8 item 3 (`files_still_migrated` a plan constant) was
correct, and the fix for it is what makes the failed-rollback case honest. My *diagnosis of the
mechanism* in item 4 was the error, and it was the kind that a review must own explicitly: I read a
key name (`:rolled-back`) as a fact about the caller's subject without asking whose transaction it
described. That is exactly the "the verifier was blind to its own subject" failure the house rules
name, committed by the verifier.

---

# II. My other items — status, with my own re-run

All field values below are from `/home/forge/tmp/opus-q5z3-fx/probe2.clj` and `probe4.clj`, each
scenario driven end to end through `alias-migration/execute!` and each number checked against the
files on disk.

## 1. `mutation_attempted` hardcoded `false` — **CLOSED**

`src/clj_surgeon/mcp_alias_migration.clj:1438` (`:mutation_attempted (boolean mutated?)`), fed from
`execute-migration!`'s `@attempted` at `:1916`, set by the kernel's own
`*on-write-boundary*` callback plumbed at `:1741-1742` — the same volatile ALIAS-047's heap guard
reads (`:1935-1949`). The argument is REQUIRED, not defaulted, which is the right shape: a default
is how the constant came back.

My re-run, five scenarios:

| scenario | tree | `mutation_attempted` |
|---|---|---|
| A. post-write refusal, real undo failed at write 7 | 12 migrated | **true** |
| B. post-write refusal, undo SUCCEEDED | 0 migrated | **true** |
| C. post-write refusal, partial restore | 6 migrated | **true** |
| D. pre-write control-directory refusal (`.git/refs/heads`) | 0 migrated, nothing written | **false** |
| E. control directory outside the workspace (linked-worktree shape) | 0 migrated | **false** |

B is the case that proves it is the write BOUNDARY and not a restatement of `source_unchanged`:
`source_unchanged true` beside `mutation_attempted true` is the honest pair for a rollback that
succeeded, and the spec at `alias-migration-specs.md:103` now says so in those words.

## 2. `next_action "correct_request"` on a mid-migration tree — **CLOSED, and I endorse the split**

`:1439-1441` — `next_action` follows `source_unchanged`, not `mutated?`.

```
A (12 migrated) next_action = "review_receipt"   receipt_file exists? true
C ( 6 migrated) next_action = "review_receipt"   receipt_file exists? true
B ( 0 migrated) next_action = "correct_request"  receipt_file exists? FALSE
F (verification failure, 0 migrated) next_action = "correct_request"
G (verification failure, 12 migrated) next_action = "review_receipt"
```

**Ruling on the builder's choice of `correct_request` for a SUCCESSFUL rollback: correct, and
better than the alternative.** Two independent reasons, both checked:

1. **`review_receipt` there would name a file that does not exist.** All three rollback sites delete
   the orphan receipt when the rollback succeeded (`:1775`, `:1853`, and the retire branch at `:1820`), and my
   probe confirms `receipt_file exists? = false` in scenario B. A `next_action` that sends a caller
   to a deleted path is worse than the field being absent.
2. **The tree is genuinely restored, so re-sending IS the right next action** — and the remedy
   beside it correctly drops the "MID-MIGRATION: undo it by hand" clause in that branch
   (`:1793-1795`, conditional on `(not rolled-back?)`). Structured field and prose agree in both
   directions now, which is the thing round 7 broke.

## 3. `files_still_migrated` a plan constant — **CLOSED, measured, and `files_restored` added**

`:1190-1206` (`undo-recovery-counts`), `:1208-1230` (`rollback-report`), wire at `:1459` and
`:1466`. The prose is fed the MEASURED number, read back out of the report rather than counted off
the plan a second time (`:1773`, `:1815`, `:1851`).

My partial-restore re-run — a real undo whose writes AND whose recovery write-backs throw, giving
the kernel `{:original 6, :restore-failed 6}`, `:error-type :transaction-recovery-required`:

```
C. files_still_migrated = 6   files_restored = 6   files (plan) = 12
   error = "...; rollback FAILED; 6 files remain migrated; receipt at ..."
   TREE still-migrated = 6
```

Published 6, actual 6, prose says 6, and 6 + 6 = the plan's 12. Round 8's witness (published 12,
actual 6) is dead. The no-`:recovery` case still over-states to the whole plan (`:1199-1202`,
pinned at test `:2412`), which is the safe direction.

## 4. `rolled-back?` reads only `:ok` — **WITHDRAWN by me** (see §I). The branch is right.

## 5. "the workspace's" control directory — **CLOSED**

`:1622` and `:1705` now read "lies inside a `<name>` directory, which belongs to another tool".
Confirmed on both phases and on both shapes:

```
D (relative .git/refs/heads, inside the workspace)  error = "The configured receipt directory
    lies inside a .git directory, which belongs to another tool"   control_directory = ".git"
E (absolute <main>/.git/worktrees/wt/refs/heads, OUTSIDE the workspace)  identical wording,
    control_directory = ".git"
```

## 6. The oracle over-permitting a repeated primitive — **CLOSED, and the builder corrected MY example**

`test/clj_surgeon/operation_algebra_test.clj:104-124`
(`architecture-references-one-frame-deeper` now returns a map from OWNER FORM to that form's own
effects, never a union), expected inventory asserted at `:221`, ratchet deftest `:807-851`.

I verified **both** of the builder's claims by smuggling
`(when nil (let [^java.io.Writer w nil] (.write w "smuggled")))` into each named form in my own
working copy and re-running the architecture oracle (reverted immediately; tree clean):

| smuggle target | oracle | why |
|---|---|---|
| baseline | `{:test 1 :pass 18 :fail 0}` | — |
| `validate-complete-source!` (`intent_transaction.clj:79`) — **my round-8 example** | **GREEN** | it is a callee of `compile-inverse`, which is itself the one-frame callee of `execute-change-with-context!`. **Two frames out — outside the stated boundary, not masked by the union.** It is not a key of any inventory entry. |
| `canonical-receipt-path` (`intent_transaction.clj:2590`) | **RED**, 1 failure | one frame from the bounded entry; the diff is exactly `canonical-receipt-path #{refuse!}` → `#{.write refuse!}` and **no other entry moves**, confirming it roots no other entry. |

**So the builder is right on both counts, and my round-8 finding 6 was right about the DEFECT and
wrong about the EXAMPLE.** The union blind spot was real — `canonical-receipt-path` is the correct
demonstration of it, and it is now red. My example was outside the boundary the docstring already
stated. The builder's test comment says this in the file (`:738-741`), which is the right place for
it.

The two-frame limit remains, and remains stated rather than implied — see finding 3 below.

## 7. The retire-failed branch does not restore the retired file — **still OPEN as a NOTE**

`src/clj_surgeon/mcp_alias_migration.clj:1807-1829` calls `execute-undo!` but, unlike the
verification branch at `:1841-1843`, never calls `restore-retired!`. Unchanged this round, no spec
sentence added. Still not a defect I can demonstrate — the retire throwing is what defines the
branch — but no field reports the superseded file's state and `files_still_migrated` does not count
it. A sentence in ALIAS-056, not code.

---

# III. New findings from round 9's own additions

## 1. `:restore-failed` from the DELETIONS rollback is counted against the EDITED-file plan — off by one, in the LOSING direction

`src/clj_surgeon/mcp_alias_migration.clj:1196-1205`.

`undo-recovery-counts` filters the whole `:recovery` vector by `:status`, but `commit-compiled!`
concatenates three different recoveries into it (`intent_transaction.clj:2284-2291`): the edited
files, `rollback-creations!`, and `rollback-deletions!`. **`rollback-deletions!` also mints
`:restore-failed`** (`intent_transaction.clj:2162`) — for a file the transaction DELETED and could
not put back, which has nothing to do with an edited file's migration state. Because
`file-count` is `(count files)` — the plan's EDITED files only (`:1905`) — such an entry
decrements a count it was never part of.

*Witness* (`probe3.clj`, calling `rollback-report` with the kernel's own shape):

```
recovery = 12 × {:status :original}  +  {:file "src/new_lib.clj" :status :restore-failed}
=> {:files-still-migrated 11, :files-restored 1}      twelve files are still migrated
```

Reachability: an alias migration WITH a lib rename carries `:create-files`
(`mcp_alias_migration.clj:714-719`, populated whenever `plan` has `:lib-rename`), whose undo carries
`:deleted-files`. It then needs the undo to fail mid-write and the deletion-restore to fail too — a
two-fault path, and the fixture request I ran produces no `:lib-rename`, so I could not drive it end
to end. **Severity: SHOULD-FIX, not blocking** — it is an off-by-one on a diagnostic count under two
faults. But it is in the direction the builder's own docstring (`:1183-1187`) commits against:
*"under-stating it loses a file."* Fix is one line at the call site: pass the plan's file set and
count only recovery entries whose `:file` is in it.

## 2. `mutation_attempted` is publishable as `true` beside `next_action "correct_request"` — intended, and worth one spec sentence more

Scenarios B and F. A caller that gates a retry on `mutation_attempted` alone now sees `true` on a
tree that is fully restored and is being told to `correct_request`. That is the honest pair and the
spec argues for it, but the spec argues it from the *rollback* side only. The one sentence missing
is the CALLER's rule: **`next_action` is the retry gate; `mutation_attempted` is the forensic
one.** Documentation, no code. **NIT.**

## 3. Two frames down is still outside the oracle — stated limit, standing observation

`test/clj_surgeon/operation_algebra_test.clj:96-102` and `:739-741`. Proven above: `.write` in
`validate-complete-source!` is invisible. The boundary is deliberate (one more frame collapses the
file into one set and the oracle asserts nothing) and is now stated in three places rather than
implied — but it remains true that a raw write added two frames below a bounded entry ships green.
**OBSERVATION, no action this round.** If it is ever to close, the shape is a fixed-point over
callees with the entry set frozen, not another manual frame.

---

# IV. Findings for the mayor's merge queue

1. **WITHDRAWN — my round-8 item 4 (`rolled-back?` reads only `:ok`) was WRONG; the branch is
   right.** `src/clj_surgeon/mcp_alias_migration.clj:1151-1169`.
   *Witness:* a real undo failed at its 7th write returns `{:rolled-back true, no :ok, recovery
   {:original 6 :restored 6}}` and leaves **12 of 12 files migrated on disk**; my requested
   `(or (:ok …) (:rolled-back …))` would have published `rolled_back true / source_unchanged true /
   files_still_migrated 0` over that tree.

2. **CLOSED — `mutation_attempted` is the kernel's write boundary.**
   `src/clj_surgeon/mcp_alias_migration.clj:1438`, fed at `:1916` from `:1741-1742`.
   *Witness:* `true` on both the failed rollback (12 migrated) and the SUCCEEDED rollback
   (0 migrated); `false` on the pre-write control-directory refusal, which wrote nothing.

3. **CLOSED — `next_action` follows the tree, and `correct_request` on a successful rollback is the
   right call.** `src/clj_surgeon/mcp_alias_migration.clj:1439-1441`.
   *Witness:* `review_receipt` with `receipt_file` present on a 12- and a 6-file mid-migration tree;
   `correct_request` with `receipt_file exists? = false` on the restored tree, where
   `review_receipt` would have named a deleted file.

4. **CLOSED — `files_still_migrated` / `files_restored` are measured and sum to the plan count.**
   `src/clj_surgeon/mcp_alias_migration.clj:1190-1230`, `:1459`, `:1466`.
   *Witness:* a real undo leaving `{:original 6, :restore-failed 6}` publishes `6` and `6` over a
   tree with exactly 6 migrated files, and the prose repeats the measured 6.

5. **CLOSED — the control-directory refusal no longer calls an outside directory "the
   workspace's".** `src/clj_surgeon/mcp_alias_migration.clj:1622`, `:1705`.
   *Witness:* both the in-workspace `.git/refs/heads` and the absolute linked-worktree
   `<main>/.git/worktrees/wt/refs/heads` refuse with "lies inside a .git directory", `control_directory ".git"`.

6. **CLOSED — the effect inventory is keyed by owner form; the repeated-symbol blind spot is shut.**
   `test/clj_surgeon/operation_algebra_test.clj:104-124`, ratchet at `:807-851`.
   *Witness:* `.write` smuggled into `canonical-receipt-path` turns the oracle RED
   (`canonical-receipt-path #{refuse!}` → `#{.write refuse!}`, and no other entry moves), where the
   round-8 union left it byte-identical.

7. **SHOULD-FIX (non-blocking) — a `:restore-failed` from `rollback-deletions!` decrements a count
   built from the EDITED-file plan.** `src/clj_surgeon/mcp_alias_migration.clj:1196-1205`;
   the foreign status is minted at `intent_transaction.clj:2162`.
   *Witness:* `rollback-report` fed `12 × :original` plus one deletions `:restore-failed` publishes
   `files_still_migrated 11` over twelve migrated files — under-stating, which the function's own
   docstring says loses a file. Two-fault reachability (lib-rename + failed undo + failed
   deletion-restore).

8. **NIT — the spec argues `mutation_attempted true` beside `next_action "correct_request"` from the
   rollback's side, never from the caller's.** `docs/intent/alias-migration/alias-migration-specs.md:103`.
   *Witness:* scenarios B and F publish exactly that pair over a fully restored tree; one sentence
   naming `next_action` as the retry gate and `mutation_attempted` as the forensic field removes the
   only reading under which the pair looks contradictory.

9. **NOTE (no action) — the retire-failed branch still does not call `restore-retired!`.**
   `src/clj_surgeon/mcp_alias_migration.clj:1807-1829` vs `:1841-1843`. Carried unchanged from
   round 8; a sentence in ALIAS-056, not code.

10. **OBSERVATION — the effect oracle's boundary is one frame, and two frames down still ships
    green.** `test/clj_surgeon/operation_algebra_test.clj:96-102`, `:739-741`.
    *Witness:* `.write` smuggled into `validate-complete-source!` (`intent_transaction.clj:79`), a
    callee of `compile-inverse`, leaves the oracle at `{:pass 18 :fail 0}`. Stated limit, not an
    oversight — and the correction of my own round-8 example.

---

## Verdict — **GO**

Every must-fix I ruled in round 8 is closed with evidence I produced independently, and the two
items I got wrong were caught by the builder with executed proofs that I have now reproduced from
scratch. The branch's honesty surface — `source_unchanged`, `rolled_back`, `mutation_attempted`,
`next_action`, `files_still_migrated`, `files_restored`, `receipt_file`, `control_directory` —
agrees with the files on disk in all five scenarios I drove through the real verb, at all three
rollback sites, in both the succeeded and failed directions. Five red witnesses fail on their
parents. The oracle that missed the union defect is corrected in the same round, and the correction
is itself ratcheted by a test that smuggles the symbol.

Nothing left is blocking. Item 7 is an off-by-one under two faults in the losing direction and
should ride the next touch of this file; items 8, 9 and 10 are prose and a stated limit.

**One thing I want on the record for the mayor, because it bears on how this review should be
weighted:** the strongest signal in round 9 is not that the builder fixed four findings — it is that
it **refused one with a test.** `c08921c` ships no red commit and says "REFUSED AS SPECIFIED", and
the two tests it adds (`mcp_alias_migration_test.clj:2432`, `:2484`) exist precisely to make my
one-line "fix" unlandable by anyone who reads the key name the way I did. That is the correct
response to a wrong review, and it is what a merge queue should want to see.

*Rules of engagement honoured: scratch clone only, no commit/stash/push, no port in 7888–7895 or
7906 contacted, `make mcp-test` avoided in favour of the direct alias, all JVM suites under
`suite-run`, fixtures confined to `/home/forge/tmp/opus-q5z3-fx`, working tree verified clean at
`f51ceae` at exit.*
