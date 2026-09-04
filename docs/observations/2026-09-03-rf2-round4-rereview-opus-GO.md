# rf2-extract-rewire 965d49e — Opus executed round-4 re-check (Sol filter fallback): GO

# rf2 round-4 executed re-review — `bridge/rf2-extract-rewire` @ 965d49e

**Reviewer: Opus, the assigned FALLBACK.** OpenAI's content filter refuses this branch because of
its symlink / path-confinement fixtures, so this is the fallback independent re-review, not a Sol
pass. I am also the author of the round-3 verdict this re-checks, so read every "CLOSED" below as
a claim backed by a re-run I performed here, not by the branch's own tests.

**Method.** Scratch clone `/home/forge/tmp/opus-rf2b` at 965d49e — no commit, no stash, no push
(`git log -1` is still 965d49e; only untracked `probe.clj`, `probe2.clj`, `probe3.clj` and
`.cpcache/`). Fixtures confined to `/home/forge/tmp/opus-rf2b-fx`. No MCP server started; no port
contacted. Suites under `/home/forge/bin/suite-run`; probes in-process at `-Xmx512m`.

**Round 4 = 58518c7..965d49e** — five RED/GREEN pairs (EXTRACT-037 amended, 039, 040, 041, 042),
a merge of `origin/main`, and one merge repair. Source diff touches only `extract.clj` (+447/-166),
`mcp_workspace_sources.clj`, `mcp_tool.clj`, `mcp_extraction_plan.clj`, plus tests. Fences
re-checked as untouched by this range: `mcp_paths.clj`, `file_ops.clj`, `edit_dsl.clj` SCI
allowlist, `mcp_process.clj`.

---

## VERDICT: **GO** for the mayor's merge queue

All five round-3 items are **CLOSED** except item 4, which is **PARTIAL** and bounded behind a
hash fence that runs first. The two new hunts that came back positive — a nested `.git` is
writable (finding 1), and an unreadable out-of-root link now bricks the verb (finding 3) — are
**not regressions**: the first is identical on `origin/main` today and is outside the ruling's
literal scope, the second is a deliberate safety trade this round chose on purpose. Round 4 is
strictly better than either parent on every axis I measured, and the three gates are green.

Follow-ups 1 and 2 below should be filed and fixed next. **Finding 1 blocks calling the fence
complete; it does not block merging a branch that narrows the hole rather than widening it.**

---

## Part 1 — my five round-3 items, each re-run

| # | round-3 item | verdict | file:line | witness I re-ran |
|---|---|---|---|---|
| 1 | MCP entrance bypassed the walk; I wrote into `.git/hooks` through it | **CLOSED** | `mcp_workspace_sources.clj:10-33`, `mcp_tool.clj:322-343`, `mcp_extraction_plan.clj:149-178`, `extract.clj:740-822` | fixture `w9`, below |
| 2 | canonicalising the pruned set forbade trees the walk READ (`target -> build_out`, `out -> src`) | **CLOSED** | `extract.clj:513-525` | fixtures `w8`, `w4`, below |
| 3 | an unreadable directory disappeared silently under `:complete true` | **CLOSED** | `extract.clj:290-301`, `:356-364`, `:1432-1440` | fixture `w6`, below |
| 4 | the pre-write gate was not the gate its docstring claims | **PARTIAL** | `extract.clj:589-650`, `:2272-2278`, `:528-547` | fixtures `nT`, `nT2`, below |
| 5 | the refusal was total and dropped its context | **CLOSED** | `extract.clj:1459-1479`, `:2226-2229`, `:820-822` | fixture `w1`, below |

### Item 1 — CLOSED. The MCP entrance now refuses, and writes nothing.

`w9` = `src/app/alias_caller.clj -> ../../.git/hooks/caller.clj`, driven through
`mcp-tool/execute-request!` with the same `caller_changes` payload that wrote the file in round 3:

```
:ok                     false
:error_type             "caller-path-in-skipped-tree"
:phase                  "kernel"
:path                   /…/w9/src/app/alias_caller.clj
:.git-byte-identical    true      (SHA-256 before == after)
:source-byte-identical  true
:link-still-a-link      true
:no-target-created      true
```

and the universe itself is gone before the transaction exists —
`(ws/read-all root)` → `{:ok false :error-type :caller-path-in-skipped-tree}`, `:sources` nil.
The **plan** entrance refuses identically and with more context
(`mcp-extraction-plan/plan!`, valid params):

```
:ok false  :error_type "caller-path-in-skipped-tree"
:tree        /…/w9/.git
:resolves-to /…/w9/.git/hooks/caller.clj
:discovery   {:files 3 :skipped-directories [{:dir /…/w9/.git :reason :build-tree}]}
```

Three walks are now one: `discover-workspace-sources` (`extract.clj:740`) is the single kernel,
`read-all` calls it (`mcp_workspace_sources.clj:32`), and the `file-seq` plus lexical `"/.git/"`
test that carried the round-3 defect is deleted.

### Item 2 — CLOSED. Both over-refusal shapes extract normally.

- `w8` (`out -> src`): `:applied true`, caller `src/app/state.clj` rewired,
  `:discovery {:files 2 :skipped-directories [{:dir …/w8/out :reason :build-tree}]}`.
  No refusal anywhere in the workspace.
- `w4` (`target -> build_out`, `build_out/` a real root dir the walk read): `:applied true`,
  callers `["build_out/gen.clj" "src/app/state.clj"]` — including the file that was the
  round-3 false refusal. `:discovery {:files 3 …}`.

The subtraction that does it is `extract.clj:521-525`: a canonical prune prefix that is an
ancestor of a path the walk read is removed from the prune set. See Part 2(a) for whether it
subtracts too much.

### Item 3 — CLOSED. `:unreadable` is typed, counted, named, and non-harmless.

`w6` (`src/app/locked` mode 000 holding one caller), `extract/plan`:

```
:complete  false
:discovery {:files 2
            :skipped-directories [{:dir /…/w6/src/app/locked :reason :unreadable}]}
:callers-unresolved [{:file :workspace-scan :reason :workspace-scan-incomplete
                      :directories-not-read ["/…/w6/src/app/locked"]}]
```

`:unreadable` is deliberately absent from `harmless-directory-skips` (`extract.clj:1440`), so
`scan-gap` makes `:complete` false rather than inferring the gap from a smaller count. The same
`nil`-is-not-empty correction landed in the escape probe (`extract.clj:236,255-257`) — that is
finding 3 below.

### Item 4 — PARTIAL. The prune half is wired into the pre-write call, and is inert for every path the plan already canonicalized.

The wiring is real: `extract.clj:2272-2278` now passes
`(get-in p [:discovery :skipped-directories])` into `confine-workspace-paths`, and both calls share
`in-pruned-tree-fn` (`extract.clj:551-567`) and `pruned-tree-refusal` (`:569-587`). The specific
case the docstring names — *"a caller path that becomes a link into `.git` after the plan was
proved"* — genuinely refuses. `nT2`, caller path is itself a symlink into `.git/hooks`, calling the
private fn exactly as `execute!` does:

```
:caller-is-itself-a-symlink  true
:PRE-WRITE-GATE-RESULT       {:error-type :caller-path-in-skipped-tree :tree /…/nT2/.git}
```

But the broader claim in the same docstring — *"BOTH halves run in both calls. That is the whole
point"* — is false in effect, because the two calls are handed different `paths`, and the
subtraction is computed **from the very paths being checked**:

- plan time (`canonical-workspace-paths`, `extract.clj:686-687`) gets `(:files walked)` — the raw
  walk, which contains the **link spelling**. `walked-read-paths` excludes links
  (`extract.clj:538-543`), so `.git` survives subtraction and the refusal fires.
- pre-write (`extract.clj:2272-2278`) gets `(map :file caller-plans)` — paths
  `canonical-workspace-paths` already collapsed onto their canonical real file. I captured the real
  arguments with `with-redefs` during a live `execute!` on `w4`:
  `:pre-write-paths ["/…/w4/build_out/gen.clj" "/…/w4/src/app/state.clj"]`,
  `:any-path-is-a-symlink nil`.

A canonical caller path under `.git` therefore deletes `.git` from its own prune set. Directly:

```
(skipped-tree-prefixes [{:dir "/…/w1/.git" :reason :build-tree}] #{})
  => #{"/…/w1/.git"}
(skipped-tree-prefixes [{:dir "/…/w1/.git" :reason :build-tree}]
                       #{"/…/w1/.git/hooks/caller.clj"})
  => #{}                                   ; the fence erases itself
(confine-workspace-paths "/…/w1" ["/…/w1/.git/hooks/caller.clj"]
                         [{:dir "/…/w1/.git" :reason :build-tree}])
  => nil                                   ; no refusal
```

Reachable shape (`nT`): swap the caller's **parent directory** for a link
(`src/app -> ../.git/hooks`) between plan and commit. The file itself is not a symlink, so
`walked-read-paths` keeps it, canonicalizes it to `/…/nT/.git/hooks/caller.clj`, and subtracts
`.git`:

```
:caller-is-itself-a-symlink  false
:caller-canonical            /…/nT/.git/hooks/caller.clj
:pruned-after-subtraction    #{}
:PRE-WRITE-GATE-RESULT       nil
```

`atomic-write!` resolves its parent with `(.getParentFile (.getAbsoluteFile target))`
(`file_ops.clj:24`) and `Files/move`s onto the path string, so that write would land in
`.git/hooks`.

**Why this is not a merge blocker:** the caller staleness fence at `extract.clj:2293-2301` runs
**before** `@caller-escape` (`:2324`) and compares every caller's planned bytes against the file on
disk. The swap changes the bytes, so the run dies as `:stale-extraction-caller` unless the attacker
makes the swapped-in `.git` file byte-identical to the planned caller — in which case the write is
a no-op. The prune half is defence in depth that currently does not depend. The docstring should
say so, or the subtraction should exclude the candidate itself.

### Item 5 — CLOSED. The refusal carries `:discovery`, and the no-write runs report instead of refusing.

`w1`, three runs on a fresh fixture:

- **dry run** (`extract/plan`): no refusal. `:applied false`, `:complete false`,
  `:discovery {:files 3 :skipped-directories [{:dir …/.git :reason :build-tree}]
  :skipped-callers [{:path …/src/app/alias_caller.clj :resolves-to …/.git/hooks/caller.clj
  :tree …/.git}]}`, and `:callers-unresolved` names it under
  `:callers-in-skipped-trees`. That is EXTRACT-042 doing exactly what I asked for: the would-be
  refusal reported, with `:discovery`.
- **`:rewire-callers false`**: `:applied true`, `:complete false`, same `:skipped-callers`,
  `.git` byte-identical.
- **`execute!`**: refuses, and the refusal now carries its context —
  `:error-type :caller-path-in-skipped-tree`, `:tree "/…/w1/.git"` (`java.lang.String`),
  `:resolves-to`, `:remedy`, and `:discovery {:files 3 :skipped-directories […]}`.
  Post-conditions: `.git` byte-identical, source byte-identical, no target created, link still a
  link.

---

## Part 2 — hunting what round 4 introduced

### (a) The new prune rule — does the subtraction permit a write it should refuse? **No.** Clean.

The rule is: refuse only if the candidate is under a canonical pruned prefix AND that prefix is
not an ancestor of a path the walk read (`extract.clj:513-525`).

I built the case as posed and one sharper:

- **`nA`** — `target -> build_out`; `build_out/gen.clj` and `build_out/hooksdir/hook.clj` real;
  `src/app/alias_hook.clj -> ../../build_out/hooksdir/hook.clj`. Result: `:applied true`, callers
  `["build_out/gen.clj" "build_out/hooksdir/hook.clj" "src/app/state.clj"]`, `hook.clj` rewritten.
  **This is correct, not a hole:** the walk descended `build_out` and read `hook.clj` directly, so
  the write lands on a file that is in the read universe under its own name. The link merely
  collapsed onto it.
- **`nA2`** — the sharper one. `target -> build_out` (so `build_out` gets subtracted by
  `gen.clj`), plus `build_out/deep -> ../.git/hooks`, plus
  `src/app/alias_caller.clj -> ../../build_out/deep/caller.clj`. If the subtraction leaked, this
  is where it would: an un-pruned tree used as a stepping stone into a pruned one. Result:

```
:error-type       :caller-path-in-skipped-tree
:tree             /…/nA2/.git
:DOT-GIT-CHANGED  false
:discovery        {:files 4 :skipped-directories [{:dir …/.git   :reason :build-tree}
                                                  {:dir …/target :reason :build-tree}]}
```

**Why the rule holds structurally, not just on my two fixtures:** `read-paths` comes from
`(:files walked)`, and the walk never descends a link and never enters a pruned root name. So no
path under a genuinely pruned tree can ever appear in `read-paths` — the only prefixes the
subtraction can remove are ones the walk really entered by some real spelling. `build_out/deep` is
itself skipped by the "already reachable by its real path" branch (`extract.clj:326-330`) and adds
no prefix, so it cannot launder anything either.

### (b) `walked-read-paths` excludes link entries — a directory symlink inside `src`

**`nB`**: `src/app/alias -> real`, `src/app/real/one.clj` a genuine caller.

```
kernel :paths      [src/app/state.clj  src/app/real/one.clj  src/app/core.clj]   (3, no duplicate)
:applied           true
:callers           ["src/app/real/one.clj" "src/app/state.clj"]
:complete          true
:alias-still-a-link true
```

**Read exactly once**, through its real path; the link is neither refused nor duplicated. It is
skipped by `extract.clj:326-330` and — correctly — *not* recorded in `:skipped-directories`,
because the tree behind it was read. `walked-read-paths` excluding link entries costs nothing
here: the real file `src/app/real/one.clj` is itself a non-link and carries the prefix.

### (c) `read-all`'s new shape — every caller proved

`workspace-sources/read-all` has exactly **two** non-test callers, and both branch on `:ok`:

- `mcp_tool.clj:327` → `sources (:sources found)`; `compiled (if-not (:ok found) found …)` at
  `:336-340`. Proved live by `w9` (refusal propagates as `:error_type
  "caller-path-in-skipped-tree"`, no write). Nit: `request` is still `assoc`ed from a nil
  `sources` at `:329-335` before the branch — dead work, harmless.
- `mcp_extraction_plan.clj:153` → `(if-not (:ok found) (merge (refusal …) (select-keys found …)) …)`
  at `:161-166`. Proved live above.
- `mcp_tool.clj:280` (`relative-paths`) is inside `publicize-extraction-decision-refusal`, which is
  only reached on the `:ok` branch. Safe.

`mcp-extraction-plan/plan!` has one non-test caller, `mcp_inspect_tool.clj:821`, which passes the
map straight through. No caller anywhere treats the result as a seq, so there is no silent-empty-
universe path. `structural_lens.clj:945` `read-all-forms` is an unrelated name.

### (d) The merge repair, 10 → 11 — correct, not masking

Verified independently of the commit message:

- `origin/main` (`f092107^2`) replaced a hardcoded `/opt/homebrew/bin/clj-kondo` assertion in
  `mcp_change_buffer_test.clj:683-706` with a resolver-derived one, adding exactly **one**
  `change-buffer/expand-command` call — `grep -c` on the merged file = 1. (That is, incidentally,
  main's own fix for the one failure I reported in round 3.)
- `expand-command` is in `public-moved-vars` (`extract_rewire_test.clj:335`), so requalifying it
  is the specified behaviour.
- Occurrence counts in the reference file: `compile-exact-profile` 3, `classify-exact-process-outcome`
  2, `run-exact-verification!` 5, `expand-command` 1, `run-process!` 0, `admission-unverified?` 0
  → **3+2+5+1 = 11**.
- The repair is **not** a bare count bump: `extract_rewire_test.clj:401-407` adds the
  `["change-buffer/expand-command" "exact-verify/expand-command" 1]` pair to `expected`, and
  `(is (nil? (first-difference expected (:source result))))` at `:412` still pins the entire
  rewritten source. A masked regression would have to reproduce byte-for-byte.

### (e) The escape probe's "may be present" on unreadable — confirmed, and it is a real usability cost

`nE`: `vendor -> /…/nE-outside` (mode 000, holds a `.clj`). `holds-clojure-sources?`
(`extract.clj:236` top-level, `:255-257` per directory) now returns `true` when a directory will
not list, so the walk takes the `:else` branch at `extract.clj:346`:

```
plan  :error-type  :caller-path-outside-root
      :error       "A directory symlink leaves the extraction project root and the tree behind
                    it holds Clojure sources this extraction cannot confine: /…/nE/vendor -> /…/nE-outside"
      :remedy      "point the link inside the project root, remove it, or extract from a root
                    that contains its target; …"
      :discovery   {:files 0}
exec  :error-type  :caller-path-outside-root   :applied nil
```

**Every extraction in that workspace is impossible**, dry run included, with no cap or flag to
override, and the offered remedy (remove the link, or move the root) is not available for a real
vendor/NFS mount an agent does not own.

**The trade, stated:** safety is right in principle — "I could not read it" is not "no caller is
behind it", and this is the same `nil`-is-not-empty correction that fixed item 3. But the branch
answers the identical fact differently on the two sides of the root: **unreadable INSIDE the root
is soft** (`:unreadable`, `:complete false`, named — `w6`), **unreadable BEHIND an out-of-root link
is fatal**. The soft answer already exists and is honest. My recommendation: record the unreadable
escape as a *named, non-harmless* skip (`:reason :unreadable-outside-root`) that drives `:complete
false`, and keep the hard `:escape` for a tree the probe positively proved holds sources.
`harmless-directory-skips` (`extract.clj:1440`) already has the machinery.

---

## Part 3 — findings, in fix order

1. **MED-LOW — the build-tree prune is root-only, so a NESTED `.git` is an ordinary writable source
   tree; I rewrote one with no symlink and no race.** `extract.clj:281`, `:319-325`.
   Witness `nG`: `modules/vendorlib/.git/hooks/caller.clj` requiring `app.core`; `extract/execute!`
   → `:applied true`, callers `["modules/vendorlib/.git/hooks/caller.clj" "src/app/state.clj"]`,
   `:NESTED-DOT-GIT-CHANGED true`, and the file now reads
   `(ns app.nested-hook (:require [app.moved :as moved]))`. `:discovery {:files 4}` — nothing was
   skipped, nothing was named. That is the shape of a git submodule or a vendored checkout. The
   root-only rule is defensible for `target` and `node_modules` (real namespace segments); it is
   not defensible for `.git`, which is never a namespace segment at any depth. The verb's own
   remedy text says *"repository metadata are pruned because nothing in them is a source this verb
   may edit"* — and it just edited some. One-line fix: prune `.git` at every level, keep the others
   root-only. **Pre-existing and identical on `origin/main`; out of the ruling's literal scope
   ("a tree the walk pruned"), squarely inside its purpose.**

2. **LOW-MED — the pre-write prune half is inert for every path the plan canonicalized, because
   the subtraction is computed from the paths being checked.** `extract.clj:528-547` +
   `:521-525` + `:2272-2278`. Witness: `(skipped-tree-prefixes [{:dir …/.git}] #{…/.git/hooks/caller.clj})`
   → `#{}`, and `confine-workspace-paths` returns `nil` for a caller path inside `.git` (`nT`),
   while the same tree refuses for a caller that is itself a link (`nT2`). Bounded by the
   caller hash fence at `extract.clj:2293-2301`, which runs first. Fix: exclude the candidate from
   its own `read-paths`, or pass the walk's read set through the plan instead of re-deriving it —
   and correct the "BOTH halves run in both calls" docstring either way.

3. **LOW (usability) — one unreadable out-of-root directory link makes every extraction in the
   workspace impossible, dry run included, with no override.** `extract.clj:236,255-257` →
   `:346`. Witness `nE` above. Fix and rationale in Part 2(e).

4. **LOW — the MCP *apply* refusal drops the context the CLI refusal now carries, and the MCP
   *plan* refusal emits kebab-case keys into a snake_case contract.**
   `mcp_contract.clj:1169,1411-1415` (`normalize-refusal` whitelist) ·
   `mcp_extraction_plan.clj:161-166`. Witness: `execute-request!` on `w9` returns exactly
   `#{:error :error_type :ok :path :phase :reason :remedy :source_unchanged :workspace_root}` —
   no `:tree`, no `:resolves_to`, no `:discovery` — while `plan!` on the same workspace returns
   `:tree`, `:resolves-to` and `:discovery` **hyphenated**, beside `:error_type`, `:read_complete`
   and `:source_unchanged`. One entrance loses the evidence; the other spells it two ways.

5. **LOW — ratchets missing for the three shapes above.** `extract_test.clj` gained good witnesses
   this round (`:unreadable`, the pruned-caller report, the pre-write gate) and `mcp_tool_test.clj`
   gained the two-entrance equality test at `:2019-2075`. Nothing covers: a nested `.git`
   (finding 1), the pre-write subtraction erasing its own fence (finding 2), or an unreadable
   escape target (finding 3). Per the ratchet rule each fix lands with the example test that
   reproduces it.

---

## Part 4 — hunts that came back clean (re-run, so the negatives are evidence)

- **The subtraction never un-prunes a tree the walk did not enter** — `nA2`, plus the structural
  argument in Part 2(a).
- **`:tree` is always the matched prefix string**, never `true`/nil: `java.lang.String` on `w1`
  and `nA2`.
- **Prefix comparison stays segment-wise** (`extract.clj:559-563`), unchanged by round 4.
- **A directory link inside the root is read exactly once and never duplicated** — `nB`.
- **`read-all`'s shape change has no silent-empty-universe caller** — Part 2(c).
- **The merge repair pins content, not just a count** — Part 2(d).
- **The round-3 suite failure is gone**: `mcp_change_buffer_test.clj:686`'s `/opt/homebrew`
  assertion was replaced on `origin/main` by a resolver-derived one and absorbed by this merge.
  `mcp-test` is now 0 failures on Anvil.

---

## Part 5 — gate receipt (run once each, under `suite-run`, at 965d49e)

```
bb test/run_all.clj
Ran 754 tests containing 6410 assertions.
0 failures, 0 errors.

swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]

clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
Ran 387 tests containing 4091 assertions.
0 failures, 0 errors.
```

All three match the builder's claimed figures exactly (754/6410/0, 387/4091/0, oracle pass).

## Housekeeping

Scratch clone `/home/forge/tmp/opus-rf2b` still at 965d49e; untracked probes only; no commit, no
stash, no push. Fixtures under `/home/forge/tmp/opus-rf2b-fx` only. No server started, no port
contacted (7910 unused). Suites under the three `suite-run` lanes; probes in-process at `-Xmx512m`.
