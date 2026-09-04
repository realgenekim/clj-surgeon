# q5z-alias-migration f4d196b — Opus executed round-7 re-check (Sol filter fallback): NO-GO (constant-true source_unchanged on post-write refusals + an unfalsifiable witness; spec text) — round 8 launched

# q5z-alias-migration f4d196b — Opus executed round-7 re-review: NO-GO

**Fallback reviewer.** OpenAI's content filter refused this round on the symlink-race and
`.git` receipt-directory probes; this is the Opus fallback re-review, executed independently in a
scratch clone at `f4d196b81e75f56d16a7438358e2aaecd04da017`, tree clean at the end, nothing
committed, stashed, or pushed. No server started. Fixtures under `/home/forge/tmp/opus-q5z-fx`.

Round 7 closes two of Sol's three round-six items outright and closes the DETECTION half of the
third. It then opens a new one on the half it did not close: **when the post-write rollback fails,
the verb publishes `source_unchanged: true` and the sentence "the alias migration was rolled back"
over twelve source files that are still migrated.** That is a false green on the one field this
whole spec family exists to keep honest, and the branch's own round-7 witness asserts it in a form
that can never fail.

## Gates, re-run under `suite-run` on this clone

| gate | result |
|---|---|
| `make mcp-test` | **440 tests, 5037 assertions, 0 failures, 0 errors** — matches the builder |
| `make test-fast` | **734 tests, 6257 assertions, 0 failures, 0 errors** — matches the builder |
| MCP operation oracle (`mcp-operation-oracle`, swipl) | **pass** (prerequisite of `mcp-test`, exit 0) |
| tree at exit | clean at `f4d196b` |

*Caveat on the gate itself:* `make mcp-test`'s `cclsp-start-self-test` connects to
`http://127.0.0.1:7890/mcp`, which is inside the 7888–7895 fence. It is a prescribed gate and it
reused an already-running cclsp rather than starting one; flagging it so the fence owner knows,
and so nobody re-runs it casually.

## Sol's three round-six items

### (1) The post-create re-check has its own window — **PARTIAL, BLOCKING**

`src/clj_surgeon/mcp_alias_migration.clj:1546`, `:1083`

**Detection: CLOSED.** Re-ran the exact race — swap `receipts` for a symlink to the detail
directory inside the second `receipt-detail-collision?` call, after `real-directory` has answered:

```
{:calls 2 :ok false :error_type "alias-migration-receipt-published-elsewhere"
 :phase "post-write" :source_unchanged true
 :stray-in-details [] :tree-intact true}
```

**The stated guarantee HOLDS.** I could not produce `ok=true` together with a receipt under
`details` in any variant I tried. The builder's boundary statement is honest: there is no `openat`
on the JVM, the final real component is replaceable, and the after-the-fact `toRealPath` on the
published file is the right instrument. It also fails safe in the direction I expected to break it:
if the attacker *removes* the link after the write, `toRealPath` throws, `actual` is nil, and
`receipt-published-elsewhere?` returns true rather than passing.

**The ROLLBACK cannot be redirected the same way — ruled, with a witness.** `(:receipt-file result)`
is not the configured name; it is the kernel's `canonical-receipt-path`
(`src/clj_surgeon/intent_transaction.clj:2590` → `canonical-file` → `getCanonicalPath`), so the
symlink is already resolved at write time and `execute-undo!` reads the receipt at its **real**
path. Repointing the receipt-directory link a second time, between the post-write detection and the
undo, did not touch the rollback:

```
P1b  repoint receipts -> decoy inside execute-undo!
{:ok false :phase "post-write" :source_unchanged true
 :tree-intact true :orphan-receipt-in-landing []}
```

**What IS open is the failure path.** See finding 1 below: the branch's response is wrong whenever
that undo fails, and the same canonicalisation that protects the rollback is what makes it fail.

### (2) Two concurrent `create-receipt-directory!` calls — **CLOSED**

`src/clj_surgeon/mcp_alias_migration.clj:1021`

Raced the private fn directly from two threads on a `CyclicBarrier`, 200 rounds, a 60-component
missing chain each round, then had the left caller run its cleanup and counted what the right
caller lost:

```
{:rounds 200 :directories-recorded-by-both 0 :peer-directories-deleted-by-my-cleanup 0}
```

Per-component `Files/createDirectory` with `FileAlreadyExistsException` read as *the peer's answer,
not mine* is the correct primitive and it holds under contention. Sol's round-6 item 4 is closed.

### (3) `receipt-dir=.git/refs/heads`, relative-through-a-link, absolute-external — **CLOSED**

`src/clj_surgeon/mcp_alias_migration.clj:35`, `:918`, `:940`, `:966`, `:1391`

```
.git/refs/heads      -> ok=false, alias-migration-receipt-dir-in-control-directory,
                        no .edn in refs/heads, `git show-ref` exit 0 before AND after
relative via link    -> ok=false, alias-migration-receipt-dir-escapes,
                        nothing created outside, no junk in the process cwd
absolute external    -> ok=true, committed=true, receipt in the directory the caller named
```

Also probed beyond the ask, all correct: a receipt-dir that is a **symlink into** `.git/refs/heads`
is refused (`resolved-path` resolves the component before containment); a **nested repository's**
`.git/refs/heads` is refused (the segment scan finds `.git` at any depth); and on this Linux
filesystem `.GIT` is a genuinely different directory, so the case variant is not an aliasing
question here — on a case-insensitive filesystem `toRealPath` returns the true case before the
segment scan runs, so it would be caught there too.

---

## NO-GO — findings for the mayor's merge queue

### 1. BLOCKING — a failed post-write rollback is published as `source_unchanged: true`, with prose claiming the rollback happened

`src/clj_surgeon/mcp_alias_migration.clj:1549-1560` (the branch) and `:1247-1258` (the cause).

`commit-refusal` computes

```clojure
:source_unchanged (boolean (or (:source-unchanged commit)
                               (:source_unchanged commit)
                               (not (:committed commit))))
```

The post-write refusal map carries `:source-unchanged rolled-back?` and **no `:committed` key**.
When the rollback fails, `:source-unchanged` is `false`, `:source_unchanged` is absent, and
`(not (:committed commit))` is `(not nil)` = **true**. `source_unchanged` for this refusal is
therefore *not computable as false*: it is a constant.

Witness, organic — the round-6 race with an undo that fails the way the branch itself anticipates:

```
{:ok false :error_type "alias-migration-receipt-published-elsewhere" :phase "post-write"
 :source_unchanged true
 :error "The undo receipt was published outside the receipt directory whose identity this
         verb proved; the alias migration was rolled back"
 :tree-intact false
 :orphan-receipts ["c2c6afa1-eca9-423d-b9ed-7e0b310993dd.edn"]}
```

Twelve source files left migrated, an orphan receipt the caller is never told about, and both the
structured field and the prose assert the opposite. Witness, pure and needing no race:
`(am/commit-refusal {:totals {...}} {:error "x" :source-unchanged false :rolled-back false
:phase "post-write"})` returns `{:source_unchanged true}`.

**The class is wider than round 7.** The same shape is at `:1585` (`alias-migration-retire-failed`,
ALIAS-043) and `:1609` (verification-failed). Neither carries `:committed` either, so a failed
rollback on either of those pre-existing branches publishes the same false green. Round 7 did not
create the hole; it added a third door to it and is the first branch whose refusal *prose* also
asserts the rollback.

Fix: `commit-refusal` must not synthesise `source_unchanged` from the absence of `:committed` when
the caller supplied an explicit `:source-unchanged`. Distinguish "absent" from "false"
(`(if (contains? commit :source-unchanged) …)`), and make the error text conditional on
`rolled-back?`.

### 2. BLOCKING (with 1) — `rolled_back` never reaches the caller, so nothing distinguishes the two outcomes

`src/clj_surgeon/mcp_alias_migration.clj:1557` sets `:rolled-back`; `commit-refusal:1253-1267` is a
closed allowlist — `:files :sites :source_unchanged :remedy`, plus optional `:change_id` and
`:phase`. Every observed response carried `:rolled_back nil`, including the successful-rollback
case at finding (1)'s witness. With `source_unchanged` pinned true by finding 1, the caller has
**zero** signal that its tree is mid-migration. `mcp_contract.clj:1172` already publishes
`:rolled_back` for other verbs; alias_migration's own refusal shaper drops it.

### 3. BLOCKING — the round-7 witness for this exact behaviour is unfalsifiable

`test/clj_surgeon/mcp_alias_migration_test.clj:1921`

```clojure
(is (true? (:source_unchanged result))
    "the redirected receipt was detected but the tree was not restored")
```

Its message names precisely the failure of finding 1, and it can never fire, because
`commit-refusal` forces the value it asserts. The tree comparison two lines below is the assertion
doing real work; this one is decoration. Per the house rule — *if an oracle existed and missed the
bug, that is itself a finding, corrected in the same fix* — the witness must be rewritten to assert
`source_unchanged` **against an injected undo failure**, i.e. red before the fix.

### 4. OPEN — the "atomic rename replaces a link on the destination name" claim is false as implemented

`src/clj_surgeon/intent_transaction.clj:2611-2619` (docstring), `:2590` (`canonical-receipt-path`),
and MCP-OP-ALIAS-056's own text in `docs/intent/alias-migration/alias-migration-specs.md`.

The docstring and the spec both say the publish "renames onto the destination name and so replaces
a link sitting there instead of writing through it." The rename primitive does behave that way — I
verified it in isolation: `Files/move` with `ATOMIC_MOVE + REPLACE_EXISTING` onto a symlink
destination replaced the link and left the target untouched. **But the receipt path is
canonicalised before staging** (`canonical-receipt-path` → `canonical-file` → `getCanonicalPath`),
so by the time `stage-receipt!` computes its parent, the link is already resolved and there is no
link left to replace. Both the staging file and the published receipt land in the link's *target*
directory.

Witness (receipt name pinned so a link could be pre-placed on it):

```
receipts/pinned.edn -> /outside/victim.txt   (pre-existing, outside the workspace)
=> receipt bytes written over /outside/victim.txt
=> receipts/pinned.edn is STILL a symlink; nothing was ever renamed
=> execute-undo! refuses: ":receipt-out and :receipt must name an .edn file"
=> ok=false, source_unchanged=true, tree-intact=false      (finding 1, organically)
```

Reachability is low — the destination name is a fresh UUID, so I had to pin it — and the
post-write guard *does* catch the redirect. But the spec asserts a protection the code does not
implement, and the mechanism it does have (canonicalisation) is what turns the redirect into an
*unrecoverable* one, because the canonical name loses the `.edn` extension `execute-undo!`
requires. Correct the spec and docstring to describe canonicalisation, or move the canonicalisation
after the CREATE_NEW/rename pair.

### 5. GO-WITH-FIX — `control_directory` is set twice and published never

`src/clj_surgeon/mcp_alias_migration.clj:1415` (pre-write) and `:1501` (post-create); dropped by the
same allowlist as finding 2. Every control-directory refusal I produced — `.git/refs/heads`, a
nested repo's `.git`, a symlink into `.git`, the worktree relative form — returned
`:control_directory nil`. The `remedy` string lists all seven names, so the caller can work it out;
the field the code intends to publish does not exist on the wire.

### 6. GO-WITH-FIX — the control-directory guard cannot see a control directory that lives outside the workspace root

`src/clj_surgeon/mcp_alias_migration.clj:978` — `control-directory-of` returns nil unless the
resolved receipt directory `.startsWith` the canonical workspace root.

In a **linked git worktree**, `<workspace>/.git` is a *file* and the real control directory is
`<main>/.git/worktrees/<name>/`, outside the root. Witness:

```
.git is a file: true, gitdir = <main>/.git/worktrees/wt
relative  .git/refs/heads  -> ok=false, receipt-dir-in-control-directory     (refused)
absolute  <gitdir>/refs/heads -> ok=TRUE, receipt published:
          <main>/.git/worktrees/wt/refs/heads/11024411-….edn
```

`git show-ref` stayed at exit 0 in my run, so the damage here is latent rather than the immediate
128 Sol saw — but a `.edn` now sits in git's per-worktree ref storage, which is exactly the tree
ALIAS-056 says belongs to another tool. The same shape reaches a monorepo subproject whose
`project-root` is below the repository root. The spec deliberately makes absolute external
directories legal ("the shipped configuration lives under the user's state root"), so this is the
stated boundary rather than a contradiction — but it should be *stated*, since the relative form of
the identical directory is refused and the absolute form is not.

### 7. OBSERVATION — the effect-inventory oracle is still sharp inside its inventory, and blind one call deeper

`test/clj_surgeon/operation_algebra_test.clj:60-91`

Asked directly, as instructed. Adding a `spit` to `publish-staged-receipt!` — an inventoried form —
turned `mcp-test` **red (440 tests, 1 failure)**: the oracle did *not* go blind when it learned
`Files/newOutputStream`. Restored, then added the same `spit` to `receipt-source`
(`intent_transaction.clj:2606`), a private helper `stage-receipt!` calls on every receipt: the write
executed on every run and `mcp-test` stayed **green (0 failures)**. The inventory reads seven named
top-level forms textually and does not follow calls, which is a fair scope — but the round-7 comment
at line 63 claims "the inventory has to KNOW that call, or this oracle goes blind to the one write
it exists to bound," and a helper one frame down is exactly where it is blind. Worth a sentence in
the comment; not a merge blocker. Tree restored, `git status` clean.

### 8. OBSERVATION — a relative receipt-dir string is taken literally, and an empty one is the workspace root

`src/clj_surgeon/mcp_alias_migration.clj:918`

`~/clj-surgeon-receipts` creates a literal `~` **directory inside the workspace** and publishes
there (`ok=true`); it does not touch the real home, which is the safe half. `C:\Users\x\receipts`
and `\\server\share\receipts` become single filename components with backslashes inside the
workspace. `""` and `"."` resolve to the workspace root and publish the undo receipt at the top of
the repository. None of these escape, and production never reaches them —
`mcp_tool.clj:821` and `:1310` always derive `workspace/receipt-dir` when the config omits one —
so this is cosmetic. A one-line refusal for a receipt-dir that normalises to the workspace root
itself would be cheap.

---

## Verdict

**NO-GO.** Findings 1, 2 and 3 are one defect and its missing oracle: the post-write refusal cannot
report an unrestored tree, and the witness that should have caught that asserts the constant. The
round-six items themselves are in good shape — (2) is closed cleanly, (3) is closed cleanly, and
(1)'s detection half is closed with an honestly stated boundary. Fix 1–3 together (they are the same
five lines), correct the spec text in 4, and this is a merge.

