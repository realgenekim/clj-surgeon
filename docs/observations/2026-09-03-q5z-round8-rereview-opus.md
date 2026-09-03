# q5z-alias-migration b119bef — Opus executed round-8 re-check: GO-WITH-FIX (mutation_attempted/next_action ruled fixes; measured rollback counts) — round 9 launched

# q5z-alias-migration b119bef — Opus executed round-8 re-review: **GO-WITH-FIX**

**Opus-first branch, not a fallback.** This spec family's probes are symlink races, `.git`
receipt-directory writes and receipt-name pinning; OpenAI's content filter refuses those fixtures,
so rounds 7 and 8 are reviewed by Opus by design rather than by escalation.

Executed independently in a scratch clone at
`b119befbe8685e85c0b37026603fcd44e8027a95`. Nothing committed, stashed, or pushed; no server
started; no port in 7888–7895 or 7906 contacted. `make mcp-test` was **not** used (it chains
`cclsp-start-self-test` against 127.0.0.1:7890, inside the fence) — `clojure -M:clj-surgeon/mcp-test`
was run directly, so the make-target's five trailing self-tests are outside this receipt. Fixtures
under `/home/forge/tmp/opus-q5z2-fx`. Tree clean at `b119bef` at exit (`git status --porcelain`
empty).

## Gates, re-run under `suite-run`

| gate | this run | builder's claim |
|---|---|---|
| `clojure -M:clj-surgeon/mcp-test` | **444 tests, 5118 assertions, 0 failures, 0 errors** | 444 / 5118 / 0 — match |
| `bb test/run_all.clj` (test-fast) | **734 tests, 6257 assertions, 0 failures, 0 errors** | 734 / 6257 / 0 — match |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | **pass**, exit 0 (`legacy counterexamples=[verification_failed,verification_pending]`) | pass — match |

## The witnesses were red first

Checked out `5281f91` (the red commit: round-8 tests on round-7 source) and ran the four new
deftests. All four fail there, and the control-directory one fails on the TREE as well — at
`f4d196b` the linked-worktree case migrated twelve files and published a receipt into
`<main>/.git/worktrees/wt/refs/heads`:

```
a-post-write-refusal-whose-rollback-failed-reports-a-migrated-tree   7 failures
a-post-write-refusal-whose-rollback-succeeded-reports-a-restored-tree 2 failures
a-receipt-resolved-onto-a-name-without-edn-is-refused-not-reported-ok 7 failures
a-control-directory-outside-the-workspace-root-is-still-refused      17 failures
```

---

## Round-7 findings 1–7

### 1. Failed post-write rollback published as `source_unchanged: true` — **CLOSED**

`src/clj_surgeon/mcp_alias_migration.clj:1338-1348` (`commit-refusal` now reads
`(contains? commit :source-unchanged)` before synthesising), `:1151-1165` (`rollback-report`).

My re-run, the round-6 race with an injected failing undo (my own race harness, not the branch's):

```
{:ok false :error_type "alias-migration-receipt-published-elsewhere" :phase "post-write"
 :source_unchanged FALSE :rolled_back false :files_still_migrated 12}
receipt_file = /tmp/opusq5z…/.clj-surgeon/alias-migration/f86367f2-….edn   (exists: true)
error = "The undo receipt was published outside the receipt directory whose identity this verb
         proved; rollback FAILED; 12 files remain migrated; receipt at /tmp/opusq5z…/…edn"
TREE still-migrated = 12
```

`receipt_file` is the REAL path — the link's target directory, not the configured one. Prose,
structured fields and the tree all agree.

The succeeding-undo race:

```
{:ok false :phase "post-write" :source_unchanged TRUE :rolled_back TRUE :files_still_migrated 0}
error = "…; the alias migration was rolled back"      TREE still-migrated = 0, receipt deleted
```

Pure witness, no race: `(am/commit-refusal {:totals {…}} {:error "x" :source-unchanged false
:rolled-back false})` → `{:source_unchanged false :rolled_back false}`. The constant is gone.

### 2. `rolled_back` never reached the caller — **CLOSED**

`:1362-1377`. The allowlist now passes `:rolled_back`, `:files_still_migrated`, `:receipt_file`
and `:control_directory`. Observed key set on every post-write refusal:
`(:error :error_type :files :files_still_migrated :mutation_attempted :next_action :ok :operation
:phase :receipt_file :remedy :rolled_back :sites :source_unchanged :write_authority)`.

### 3. The unfalsifiable witness — **CLOSED**

`test/clj_surgeon/mcp_alias_migration_test.clj:1926-1934` replaces `(is (true? (:source_unchanged
result)))` with `(is (= (:rolled_back result) (:source_unchanged result)))` plus
`(is (zero? (:files_still_migrated result)))`, and `:1972` is the injected-failure witness that
moves them. Proven red at `5281f91` (7 failures). The oracle that missed the bug was corrected in
the same fix, per the house rule.

### 4. "the atomic rename replaces a link on the destination name" — **CLOSED**

`src/clj_surgeon/intent_transaction.clj:2612-2627` (docstring) and MCP-OP-ALIAS-056 in
`docs/intent/alias-migration/alias-migration-specs.md:103`. Both now say the rename protects the
STAGING name only, that `canonical-receipt-path` resolves a destination link before staging, and
that the post-write proof — not the rename — is what catches it. The residual case is now a typed
refusal rather than a reported `ok`: my re-run of the pinned link,

```
receipts/pinned.edn -> receipts/victim.txt
=> ok=false, alias-migration-receipt-published-elsewhere, phase post-write,
   rolled_back false, source_unchanged false, files_still_migrated 12,
   receipt_file ".../receipts/victim.txt"
   error "The undo receipt resolved onto a name no undo can read — a published receipt must still
          be an .edn file after resolution; rollback FAILED; 12 files remain migrated; …"
```

### 5. `control_directory` unpublished — **CLOSED**

Published at both phases (`:1525` pre-write, `:1611` post-create) and carried through the shaper at
`:1376`. Every refusal I produced — relative `.git/refs/heads`, `.hg/store`, `.svn/pristine`,
`.jj/repo`, `target/receipts`, `node_modules/r`, `.cpcache/r`, and the absolute linked-worktree
form — returned `:control_directory` with the exact segment name.

### 6. Control directory outside the workspace root — **CLOSED**

`src/clj_surgeon/mcp_alias_migration.clj:52-67` (`control-directories-anywhere`), `:984-1007`
(`control-directory-of`, `or`-ing containment with a segment scan over the whole resolved path).
My re-run, all absolute and OUTSIDE the workspace:

```
<main>/.git/worktrees/wt/refs/heads  -> REFUSED, control_directory ".git", 0 .edn files landed
<main>/a/b/.git/x                    -> REFUSED, control_directory ".git", 0 .edn files landed
<main>/a/.hg/x                       -> REFUSED, control_directory ".hg",  0 .edn files landed
<main>/target/receipts               -> ok=true, receipt published   (containment-only, as stated)
<main>/node_modules/receipts         -> ok=true, receipt published   (containment-only)
<main>/.cpcache/receipts             -> ok=true, receipt published   (containment-only)
```

The split is exactly the one the docstring and the spec now state.

### 7. Oracle blind one frame down — **PARTIAL** (closed for the named case; a new masking gap opened)

`test/clj_surgeon/operation_algebra_test.clj:81-110`. Closed for its own case: I added
`(spit "…" "written\n" :append true)` to `receipt-source` (`intent_transaction.clj:2606`) and the
oracle went **RED** — `:receipt-stage` gained `spit`, `1 failures`. Round 7's exact escape is shut,
and the two-frame limit is explicitly stated in the docstring rather than implied.

What the widening cost is in finding N6 below.

---

## The two things the builder deliberately left — my rulings

### (a) `mutation_attempted: false` beside `files_still_migrated: 12` — **WRONG, and fix it in this branch**

`src/clj_surgeon/mcp_alias_migration.clj:72-82` — the shared `refusal` helper hardcodes
`:mutation_attempted false` and `commit-refusal`'s extras never override it.

**The contract does not conflict.** `mcp-operation-contract-design.md:1113`'s "Exactly `false`" row
is the **compact-relation** envelope: the paragraph that introduces it (`:1104`) scopes it to "a
failure originating in Phase A, Phase B, or relation composition," and MCP-OP-EDIT-024 repeats it
for relation-admission / path-resolution / require-lowering / relation-composition refusals — all
**source-blind, pre-write** stages. A post-write refusal is not one of them. The same document's
refusal matrix, eleven lines above the table, already names this state: *"Rollback cannot prove
restoration | existing effect path | Recovery-required outcome ; never claim `source_unchanged=true`."*

**And the branch already contradicts itself, in its own test file.**
`test/clj_surgeon/mcp_alias_migration_test.clj:891-893` asserts, for the OOM-after-the-write case:

```clojure
(is (false? (:source_unchanged result)))
(is (true?  (:mutation_attempted result)))
(is (= "review_receipt" (:next_action result)))
```

That is the identical tree state — twelve files migrated, a receipt on disk — reported honestly on
one path (`execute!` at `:1840-1844`, ALIAS-047) and as `false` / `correct_request` on three others.
This is not a contract decision to defer; it is one verb disagreeing with itself, and the value it
should publish is already written down and already tested.

**Ruling: not a follow-up. Fix before merge.** It is a false green on the one field whose entire
purpose is "did we touch the tree," and a caller that gates its retry on `mutation_attempted`
retries blindly over a mid-migration workspace. It is **not** a NO-GO on its own, because unlike
round 7 the same map now says `source_unchanged false`, `rolled_back false`,
`files_still_migrated 12` and "rollback FAILED" — a caller reading the documented rollback fields
cannot be misled about the tree, only about a secondary flag. One line in `commit-refusal`.

### (b) `next_action: "correct_request"` when the remedy says "undo it by hand" — **WRONG, same fix**

Same site (`:72-82`). The honest value is not a new token: `review_receipt` is this verb's own
vocabulary (`:1320`, `:1844`) and is asserted for the mutated case at test `:893`. The refusal's own
`remedy` already reads *"The tree is MID-MIGRATION: undo it by hand from the receipt named in
receipt_file"* — the structured field contradicts the prose beside it, and the prose is right.
`correct_request` tells an automated caller to re-send a corrected request; re-sending over twelve
already-migrated files is precisely the wrong next action.

**Ruling: fix in this branch, with (a), in the same edit.** MCP-OP-ALIAS-056's new sentence
enumerates four fields that must be honest — `source_unchanged`, `rolled_back`,
`files_still_migrated`, `receipt_file` — and stops one field short. Extend it to these two.

---

## Hunting round 8's own additions

Answers to the four questions asked, each with a re-run.

**`files_still_migrated` under a PARTIAL restore — WRONG. Reachable.** `:1164` computes
`(if rolled-back? 0 file-count)` where `file-count` is `(count files)`, the PLAN's file count. It is
never a measurement. Injected an undo that restores six of twelve and then reports failure:

```
PUBLISHED files_still_migrated = 12       ACTUAL still-migrated = 6
```

This is not a synthetic-only path. `execute-undo!` → `commit-compiled!`
(`intent_transaction.clj:2278-2299`) has a documented partial outcome: on a write exception it runs
`recover-transaction!`, and when recovery is incomplete it returns `:transaction-recovery-required`
with a per-file `:recovery` map — the exact answer — which `rollback-report` discards. Safe
direction (over-states, so `source_unchanged false` stays correct), but the count is published as a
fact and is the number a human uses to decide what to undo by hand.

**The `contains?`-based `source_unchanged` read, with a nil — SAFE, no defect.**

```
{:source-unchanged nil}                    => source_unchanged false
{:source_unchanged nil}                    => source_unchanged false
{:source-unchanged false :rolled-back false} => source_unchanged false, rolled_back false
{}                                          => source_unchanged true   (absent still falls back)
{:committed true}                           => source_unchanged false
```

An explicit nil resolves to `false` — the tree is reported as possibly changed. Fails in the
conservative direction; no false green is constructible this way.

**A legal receipt directory whose real path ends in `.edn` as a DIRECTORY name — NOT a defect.**
`:1137` tests `(.getFileName actual)`, the published FILE's own name, never its parent. Re-ran with
`receipt-dir = <main>/receipts.edn`: `ok=true`, receipt `99e361ff-….edn` published inside it.

**The one-frame oracle boundary — stated, and it over-permits. See N6.**

---

## Findings for the mayor's merge queue

1. **MUST-FIX — `mutation_attempted` is a hardcoded `false` on every refusal that leaves the tree
   migrated.** `src/clj_surgeon/mcp_alias_migration.clj:79`.
   *Witness:* post-write refusal returns `{:mutation_attempted false :files_still_migrated 12
   :source_unchanged false}` while twelve files on disk differ from `:pre`; the same verb publishes
   `mutation_attempted true` for the identical state at `:1840`, asserted at test `:892`.

2. **MUST-FIX — `next_action "correct_request"` on a mid-migration tree.**
   `src/clj_surgeon/mcp_alias_migration.clj:81`.
   *Witness:* the same map's `remedy` reads "The tree is MID-MIGRATION: undo it by hand from the
   receipt named in receipt_file," while `next_action` tells the caller to re-send a corrected
   request; `review_receipt` is already this verb's value for that state (`:1844`, test `:893`).

3. **SHOULD-FIX — `files_still_migrated` is a plan constant, not a measurement, and is wrong under
   a partial restore.** `src/clj_surgeon/mcp_alias_migration.clj:1164`.
   *Witness:* undo restores 6 of 12 then reports failure → published `files_still_migrated 12`,
   actual still-migrated 6. `commit-compiled!`'s `:recovery` map
   (`intent_transaction.clj:2287-2299`) holds the per-file answer and is discarded.

4. **SHOULD-FIX — `rolled-back?` reads only `:ok`, so the kernel's "recovered after a write failure"
   shape is published as a failed rollback.** `src/clj_surgeon/mcp_alias_migration.clj:1667`,
   `:1707`, `:1741`; the shape is minted at `intent_transaction.clj:2286-2299`
   (`{:error "Transaction write failed; all files restored" :rolled-back true}` — **no `:ok` key**).
   *Witness:* undo restores every file and returns that shape → published
   `{:rolled_back false :source_unchanged false :files_still_migrated 12}` and prose
   "rollback FAILED; 12 files remain migrated" over a tree with **0** files migrated. A false RED —
   the mirror of round 7 — and one line: read `(or (:ok rollback) (:rolled-back rollback))`.

5. **NIT — a control-directory refusal for a directory OUTSIDE the workspace calls it "the
   workspace's".** `src/clj_surgeon/mcp_alias_migration.clj:1521`.
   *Witness:* `receipt-dir = <main>/.git/worktrees/wt/refs/heads`, with `<main>` outside the
   workspace, refuses with "The configured receipt directory lies inside **the workspace's** .git
   directory" — it is the main repository's, which is the whole point of the new guard.

6. **OBSERVATION — the one-frame oracle masks a raw effect symbol already present in a set.**
   `test/clj_surgeon/operation_algebra_test.clj:93-110`, expected map `:191-289`.
   *Witness:* `(when nil (let [^java.io.Writer w nil] (.write w "smuggled")))` added to
   `validate-complete-source!` (`intent_transaction.clj:79`) — a helper inside the bounded commit
   entry's one-frame closure — leaves the oracle **GREEN**, because `.write` is already in the
   expected `:commit-entry` set, inherited from a different helper. The widening added five raw
   write primitives (`.write`, `Files/move`, `Files/newOutputStream`, `file-ops/atomic-write!`,
   `file-ops/atomic-create!`) to a set that previously carried none, so a *novel* symbol like `spit`
   goes red anywhere in the closure while a *repeated* one is invisible. Cheap ratchet: key the
   inventory by owner form instead of unioning into one set per entry, so an effect is attributed to
   the form that names it. The comment's claim that "every symbol here is a call the bounded entry
   can reach" is true; the protection a reader infers from it is not.

7. **NOTE (no action) — the retire-failed branch does not restore the retired file.**
   `src/clj_surgeon/mcp_alias_migration.clj:1696-1725` calls `execute-undo!` but, unlike the
   verification branch at `:1735-1738`, never calls `restore-retired!`. Pre-existing, and the retire
   throwing is what defines the branch, so there may be nothing to restore — but no field reports
   the superseded file's state and `files_still_migrated` does not count it. Worth a sentence in the
   spec rather than code.

---

## Verdict — **GO-WITH-FIX**

Round 7's three blocking findings are one defect and its missing oracle, and all three are closed
with evidence I produced independently: `source_unchanged` is now the rollback's own answer and
moves under an injected failure; `rolled_back`, `files_still_migrated`, `receipt_file` and
`control_directory` reach the wire; the witness that asserted the constant is replaced by four that
are provably red on the parent commit. Findings 4, 5 and 6 are closed outright, and the spec text
now describes what the code does instead of a protection it never had. Gates match the builder's
numbers exactly on all three.

What is left is smaller than what round 7 blocked on, and one of it is a false green: **fix items 1
and 2 before merge** — they are a single edit in `commit-refusal`, they contradict the verb's own
tested behaviour at `mcp_alias_migration_test.clj:891-893`, and MCP-OP-ALIAS-056's new honesty
sentence should name those two fields alongside the four it already names. Items 3 and 4 are
inaccurate facts in the safe direction and can ride the same edit or the next round; item 6 is a
ratchet, not a defect.

*Rules of engagement honoured: scratch clone only, no commit/stash/push, no port in 7888–7895 or
7906 contacted, `make mcp-test` avoided in favour of the direct alias, all JVM suites under
`suite-run`, working tree verified clean at `b119bef` at exit.*
