# q5z round 10 — Opus executed review of bridge/q5z-alias-migration at 0085db8 (2026-09-04T00:00Z)

Verdict: **GO-WITH-FIX for merging 0085db8 via the integration branch.** All five round-10 claims reproduced (one-call commit on the cohort's exact request, 21 files / 63 sites / 30 collisions, rescore 6/6, byte-identical; 13 hostile scope paths typed; derived remedy selects 113/113 on a tree with root .clj/.cljs/.cljc; ALIAS-059 ratchet real — 29 assertions fail when the remedy clause is deleted; every quoted-symbol shape per spec, requiring-resolve works from another ns). Fixes (round 11): derived remedy truncates at 6 roots alphabetically and drops `src/**` without saying so (12 of 118 files selected); `./src` refused because the glob is built from the raw entry; a malformed glob throws `PatternSyntaxException` → `mcp-adapter-failure` (pre-existing) instead of the typed scope-path refusal; the "derived, not listed" enumeration reads three files and misses `invalid-workspace-root`, `mcp-adapter-failure` and five kernel error-types passed through verbatim; `string_mentions` hardcoded `[]` in var mode (pre-existing; every E3-P receipt's 0 was vacuous). Gates match exactly.

## Opus verdict, verbatim

# q5z-alias-migration `0085db8` — Opus executed round-10 review: **GO-WITH-FIX**

Executed independently in `/home/forge/tmp/sol/q5z10-wt`, verified at
`0085db88446ec54228f8a4dd87a4ff929289ba50`, working tree `git status --porcelain` empty at entry
and at exit, `git stash list` empty. Nothing committed, stashed, or pushed. No server started; no
port in 7888–7895 or 7906–7910 contacted. `clojure -M:clj-surgeon/mcp-test` run directly, never
`make mcp-test`. JVM suites one at a time under `/home/forge/bin/suite-run`. Fixtures and probes
confined to `/tmp/q5z10-sol-fx`. No process signalled that I did not start.

*Procedural deviation, recorded:* my first probe invocation (`clojure -A:clj-surgeon/mcp-test …`)
picked up that alias's `:main-opts` and ran the mcp-test suite outside `suite-run` before I noticed.
Load was 9.6/16 at the time; the suite was re-run under `suite-run` and that second, compliant run
is the one quoted in the gate table. Both runs agreed: 457 / 5414 / 0.

---

## Gates, once each under `suite-run`

| gate | this run | builder's claim |
|---|---|---|
| `bb test/run_all.clj` | **737 tests, 6273 assertions, 0 failures, 0 errors** | 737 / 6273 / 0 — match |
| `clojure -M:clj-surgeon/mcp-test` | **457 tests, 5414 assertions, 0 failures, 0 errors** | 457 / 5414 / 0 — match |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | **pass**, exit 0 (`legacy counterexamples=[verification_failed,verification_pending]`) | pass — match |
| `clj-surgeon.repository-hygiene-test` | ran inside mcp-test, 0 failures | green — match |

---

## (1) The cohort's exact request, reproduced

Fresh `bb bench/fanout/gen-fanout.clj --n 21 --seed 7` (from `origin/bridge/fanout-fixtures-in-git`,
`b62a501`) into `/tmp/q5z10-sol-fx/gen`; tree copied to a git-initialised worktree at base
`f33bfcd`. **One** `alias_migration` call, `scope.paths ["src"]`, `expect.files 21`,
`refer_policy preserve-refer` — the spelling that was refused four times in four of four E3-P arms:

```
ok=true  committed=true  files=21  sites=63  collisions_resolved=30
alias_histogram {es 5, st2 5, store-2 5, store2 6}   648.43 ms
```

```
CHECK 1 file-set:          PASS changed=21 expected=21 missing=0 extras=0
CHECK 2 form-equality:     PASS compared=21 equal=21 unparseable=0 unequal=0
CHECK 3 protected-regions: PASS regions=106 intact=106 damaged=0
CHECK 4 load:              PASS namespaces=100 rc=0
CHECK 5 behaviour:         PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0
CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 old-site-residue=0
                                wrong-or-missing-alias=0 shadowing=0
rescore-FAN: 6/6 checks passed
diff -r worktree canonical-21 (minus .git/.clj-surgeon): BYTE-IDENTICAL
```

**ALIAS-057 delivers exactly what it claims.** E3-P clause 1 is reachable at one call.

## (2) Attacking the directory reading — 13 hostile scope paths, no escape, no data loss

Each on its own fresh copy of the tree, each run through the real `execute!`:

| scope.paths | outcome |
|---|---|
| `["src/acid/fanout/ns_003.clj"]` (a FILE) | commit, 1 file |
| `["src/"]` trailing slash | commit, 21 files |
| `["src" "test/**"]` glob + dir mixed | commit, 21 files |
| `["lnk"]` symlink → `src` | typed `scope-matches-nothing`, 0 matched |
| `["esc"]` symlink → outside root | typed refusal; **the outside file is byte-unchanged** |
| `["nosrc"]` dir with no sources | typed refusal |
| `["."]` | typed refusal (root contributes no subtree pattern, by design) |
| `[""]` | typed `invalid-mcp-request` |
| `["./src"]` | typed refusal — **see finding 2** |
| `["/tmp/…/p2-outside"]` absolute outside | typed refusal; outside file unchanged |
| `["../p2-outside"]` | typed refusal; outside file unchanged |
| `["/tmp/…/absroot-x"]` absolute | typed refusal |
| 12 malformed globs (`[`, `{a,b`, `**/{`, …) | **untyped `PatternSyntaxException` — see finding 3** |

Confinement holds. The realpath/`startsWith` gate in `scope-glob-patterns` is purely lexical, but
`scan-scope` calls `walkFileTree` with an empty option set, so `FOLLOW_LINKS` is off and the
symlinked subtree is never entered. I verified the escape target's bytes after every run.

## (3) `scope-matches-nothing` vs `empty-scope` — the boundary is correct

| scope | scanned | found | error_type |
|---|---|---|---|
| `["test/**"]` (3 files, none require the lib) | 3 | 0 | `alias-migration-empty-scope` |
| `["libsrc/**"]` (7 files, none require it) | 7 | 0 | `alias-migration-empty-scope` |
| `["src/acid/fanout/ns_000.clj"]` (a non-target that exists but the glob is exact) | — | — | `scope-matches-nothing`, `files_matched 0` |
| `["srk/**"]`, `["zzz/**"]` | — | — | `scope-matches-nothing` |

The domain refusal fires **only** when the scope matched files. The `scope-matches-nothing` refusal
never asserts the domain fact — it says *"Whether any namespace here requires … is not known,
because no file was read."*

**The derived remedy's spelling is right on the shapes it names.** On a tree carrying a root-level
`.clj`, a `.cljs` and a `.cljc` under a fresh directory, `suggested-scope-paths` returned
`["*" "cljsdir/**" "libsrc/**" "src/**" "test/**"]`, which selects **113 of 113** source files the
walk can see — **0 missed**. The `src/**/*.clj` spelling the docstring prohibits would indeed have
dropped every root file and every `.cljc`/`.cljs`; the shipped spelling does not. That claim is
sound. What is not sound is the *bound* on the list — finding 1.

## (4) The text ⊇ structured ratchet — real, and its enumeration is not

**Ratchet proved by mutation.** On a scratch copy (`/tmp/q5z10-sol-fx/scratch`) I deleted the
three-line remedy clause from `alias-migration-summary` and ran
`clj-surgeon.mcp-alias-migration-test` alone: **29 failures, 0 errors**, naming exactly
*"… · the text block drops the remedy"* for every enumerated kind plus both live refusals. The
witness is load-bearing, not decorative.

**But the enumeration is not what it claims to be** — findings 3 and 4.

## (5) The quoted-symbol rule — every shape correct, and it resolves from outside

One real migration of a nine-shape file (`src/acid/fanout/qshapes.clj`, 8 sites, one call):

| shape | before | after | verdict |
|---|---|---|---|
| plain quote, fully qualified | `'acid.fanout.store/find-event` | `'acid.fanout.store2/fetch-event` | fully qualified ✓ |
| plain quote inside a vector literal | `'[acid.fanout.store/find-event :marker]` | `'[acid.fanout.store2/fetch-event :marker]` | fully qualified ✓ |
| plain quote inside a map literal | `'{:lookup acid.fanout.store/find-event}` | `'{:lookup acid.fanout.store2/fetch-event}` | fully qualified ✓ |
| syntax quote | `` `acid.fanout.store/find-event `` | `` `store2/fetch-event `` | alias ✓ (reader resolves) |
| var quote | `#'acid.fanout.store/find-event` | `#'store2/fetch-event` | alias ✓ |
| var quote via alias | `#'store/find-event` | `#'store2/fetch-event` | alias ✓ |
| syntax quote via alias | `` `store/find-event `` | `` `store2/fetch-event `` | alias ✓ |
| call site | `(store/find-event id)` | `(store2/fetch-event id)` | alias ✓ |
| string literal | `"acid.fanout.store/find-event"` | untouched | ✓ (but see finding 5) |
| plain quote of the OLD ALIAS `'store/find-event` | — | **refused** `alias-migration-indirect-reference`, `form "'store/find-event"`, source unchanged, `next_call` excludes the file | ✓ |

**`requiring-resolve` from a different namespace**, run in a real JVM over the migrated tree:

```
:a  acid.fanout.store2/fetch-event  ->  #'acid.fanout.store2/fetch-event
:b  acid.fanout.store2/fetch-event  ->  #'acid.fanout.store2/fetch-event
:d  acid.fanout.store2/fetch-event  ->  #'acid.fanout.store2/fetch-event
:i  acid.fanout.store2/fetch-event  ->  #'acid.fanout.store2/fetch-event
:j  acid.fanout.store2/fetch-event  ->  #'acid.fanout.store2/fetch-event
:c / :h  (var-quotes)               ->  #'acid.fanout.store2/fetch-event
```

Every migrated data value resolves from outside the defining namespace. **PF-4's semantic blocker
is closed** — the shape that came back as `'store2/fetch-event` and died with *"Could not locate
store2.clj on classpath"* now comes back fully qualified.

---

# Verdict

**GO-WITH-FIX** for merging `0085db8` via the integration branch.

All five round-10 claims reproduce independently; both suites and the oracle match the builder's
numbers exactly; the ALIAS-059 ratchet is proved by mutation; the quoted-symbol fix is verified by
running code, not by grep. Nothing found regresses `f51ceae`, corrupts a tree, escapes the
workspace root, or loses a byte. Findings 1, 2 and 4 are in code this round introduced; 3 and 5 are
pre-existing and merge-neutral, but 3 sits squarely inside this round's declared subject and should
not be left unfiled.

1. **`src/clj_surgeon/mcp_alias_migration.clj:394,427,625` — the derived remedy silently drops
   source roots and the sentence over-claims.** `max-suggested-scope-paths` is 6, `take`n from a
   *sorted set*, so truncation is alphabetical rather than by size, while line 625 still asserts the
   list is *"the source roots this tree actually holds."*
   *Witness:* a root with 9 top-level source dirs (`r0..r7` plus the fixture's `libsrc`, `src`,
   `test`) → `suggested_paths ["libsrc/**" "r0/**" … "r4/**"]`; the `next_call` selects **12 of 118**
   source files and **drops `src/**` entirely** — the directory holding 100 namespaces. No field
   says the list was truncated (`source_files_under_root 118` is the only hint). Fix: state the
   truncation ("the first 6 of 9"), or rank roots by file count, and prove it with a witness at the
   bound.

2. **`src/clj_surgeon/mcp_alias_migration.clj:206` — the directory check normalizes, the emitted
   glob does not.** The existence test resolves and `.normalize`s, but the pattern is built from the
   raw `trimmed`, so a directory that IS detected yields a glob that can never match.
   *Witness:* `(scope-glob-patterns root "./src")` → `["./src" "./src/**"]`; `expand-scope` with
   `["./src"]` = **0 files** while `["src"]` = **21**, and `execute!` refuses `scope-matches-nothing`
   on a spelling as natural as the one ALIAS-057 exists to accept (`src/.` fails the same way).
   Build the pattern from `root.relativize(normalized)`. Related nit, same line: `["."]` refuses
   because the root is excluded by `(not (.equals candidate root))` — mapping `.` to `**` is free.

3. **`src/clj_surgeon/mcp_alias_migration.clj:169,278` — a malformed `scope.paths` glob produces no
   `alias_migration` refusal at all.** `glob-matcher` calls `getPathMatcher` on caller text with no
   `try`; `execute!` (`:2031`) catches only `OutOfMemoryError`, and `mcp-operation/invoke!` has no
   catch, so the throw reaches `mcp_server.clj:152` and is published as `mcp-adapter-failure` — a
   receipt with **no `source_unchanged`, no `mutation_attempted`, no `remedy`, no `next_call`, and
   whose `content[0].text` is raw JSON that never passes through `alias-migration-summary`.** The
   entire ALIAS-059 contract is bypassed for this class.
   *Witness:* 8 of 12 probe patterns threw — `["["]`, `["{a,b"]`, `["**/{"]`, `["src/{**"]`,
   `["src/[a-"]`, `["src/**\\"]`, `["\\"]`, `["a{b"]` — each
   `java.util.regex.PatternSyntaxException`. Pre-existing at `f51ceae` (same `mapv glob-matcher`),
   so not a regression; the fix has a home already, `:alias-migration-scope-path-refused`
   (`:677`), and an unbalanced `{` is exactly what a model emits reaching for `src/{clj,cljs}/**`.

4. **`test/clj_surgeon/mcp_alias_migration_test.clj:458-459` — the "derived, not listed" enumeration
   reads three files and misses every refusal kind that reaches the same renderer from elsewhere.**
   Its docstring claims *"Every refusal kind alias_migration can emit."* It seeds only
   `invalid-mcp-request` and `server-not-initialized`, so it does not see `invalid-workspace-root`
   (`mcp_workspace.clj:12`, reaching the renderer through `handle-alias-migration`
   `mcp_tool.clj:1399`), `mcp-adapter-failure` (`mcp_server.clj:152`), or the five kernel error-types
   passed through verbatim by `mcp_alias_migration.clj:1537` (`:transaction-write-exception`,
   `:target-ancestor-changed`, `:intent-compiler-failure`, `:invalid-transaction-receipt`,
   `:future-source-transformation-failed`).
   *Witness:* rendering the live `invalid-workspace-root` refusal for `workspace_root "relative/path"`
   yields `next_call · none — … the remedy above names what only the caller can decide`
   (`mcp_tool.clj:1318`) on a receipt carrying **no `:remedy`** — the text points at a remedy that
   does not exist; and the one discriminating fact, the bad `workspace_root` value, is suppressed as
   an envelope key. Widen the enumeration to `mcp_workspace.clj` + `mcp_server.clj` + the kernel
   pass-through set, and make `rendered-next-call` not name a remedy it cannot see.

5. **`src/clj_surgeon/alias_migration.clj:1236-1239` — `string_mentions` is a hardcoded zero in
   var mode.** `(if (nil? from-var) (string-mentions …) [])`, so every var-mode migration publishes
   `string_mentions: 0` regardless of the tree, against a docstring (`:1072`) that says *"a silent
   zero would hide real work, so the count travels in the receipt."*
   *Witness:* the nine-shape migration above left `(def f "acid.fanout.store/find-event")` — a live
   string naming the retired lib — verbatim in the file, and its receipt reported
   `string_mentions: 0`. Pre-existing (untouched by this round's diff) and it means every E3-P
   receipt's `string_mentions: 0` was vacuous too.
