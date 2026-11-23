# rf2 round-3 executed re-review — `bridge/rf2-extract-rewire` @ 465c956

**Reviewer:** Opus (fallback). OpenAI's content filter refused this review because of the
symlink / path-confinement fixtures, so this is the fallback independent re-review, not the
originally-assigned Sol pass.

**Method.** Scratch clone `/home/forge/tmp/opus-rf2` at 465c956 (never committed, stashed or
pushed). Synthetic trees under `/home/forge/tmp/opus-rf2-fx` only. Every verdict below was
RE-RUN by me in-process against those fixtures (probe scripts `probe.clj`, `probe2.clj`,
`probe3.clj`, `probe4.clj`, `probe5.clj` in the scratch clone); nothing is graded from the
branch's own tests. One JVM at a time, `-Xmx1g`; suites under `flock /home/forge/tmp/suite.lock`.
No server started; no port contacted.

**Round 3 = 5ccb4f0..465c956, 9 commits** (EXTRACT-031 … EXTRACT-038 plus one docs commit).
Fences re-checked as untouched by this range: `mcp_paths.clj`, `mcp_workspace.clj`,
`file_ops.clj`, `edit_dsl.clj` SCI allowlist, `mcp_process.clj` — the diff touches only
`core.clj` (two `:desc` strings), `extract.clj`, `forms.clj` (a docstring), tests and docs.

---

## VERDICT: **GO-WITH-FIX**

Items **1 and 2 before the merge queue**; 3–5 may ride behind it.

The seven round-2 items are **all CLOSED**, each re-witnessed. The ruling is **PARTIAL**:
closed exactly as specified on the CLI / in-process entrance, and **completely absent on the
MCP extraction entrance, where I wrote bytes into `.git/hooks/` through the same symlink shape
the ruling forbids** (finding 1). Round 3 also introduced a new over-refusal: canonicalising the
pruned-tree set turns a directory the walk *did* read into a forbidden tree, and in the
`out -> src` shape it refuses **every** extraction in the workspace (finding 2).

---

## Part 1 — the round-2 items, each re-run

| # | round-2 finding | verdict | file:line | witness I re-ran |
|---|---|---|---|---|
| 1 | skip-list matched by bare `.getName` anywhere; `app.out.writer` silently dropped, `:complete true` | **CLOSED** | `extract.clj:307-313`, `:144-158` | fixture `wA`: `src/app/out/writer.clj` is discovered and rewired (`{:file "src/app/out/writer.clj" … :sites 1}`), while root `target/` and `node_modules/` appear as `:discovery {:skipped-directories [{:dir …/target :reason :build-tree} {:dir …/node_modules :reason :build-tree}]}` |
| 2 | oversized caller skipped, receipt still `:complete true`, no override | **CLOSED** | `extract.clj:350-355`, `:1245-1275` | fixture `wB` (700,085-byte caller): `:complete false`, `:callers-unresolved [{:reason :workspace-scan-incomplete :sources-too-large ["…/big.clj"] :remedy "…raise :max-workspace-file-bytes and re-run"}]`; re-run with `:max-workspace-file-bytes "800000"` → `:complete true` and `big.clj` rewired |
| 3 | caps unreachable / `(long "3000")` threw as `:extraction-snapshot-failed`; spec on a function-level fixture | **CLOSED** | `extract.clj:179-215`, `:1909-1916`; `core.clj:570-571,591` | `"abc"` → `:invalid-workspace-cap` *"The :max-workspace-file-bytes cap must be a positive whole number; got \"abc\""*; `0` → same type; `"800000"` (the CLI's string) coerces; `:max-workspace-files 1` → `:workspace-file-cap-exceeded`. Both caps are on both arg maps |
| 4 | any out-of-root dir symlink refused EVERY extraction, mislabelled `invalid-relative-source-path` | **CLOSED** | `extract.clj:315-336`, `:463-489`, `:1234-1242` | fixture `wC` (`vendor -> /…/empty-outside`): no refusal, `:discovery {:skipped-directories [{:dir …/vendor :reason :link-outside-root :resolves-to "…/empty-outside"}]}`, `:complete true`, the real caller rewired |
| 5 | "Could not locate" naming a touched ns → `:unverified`; a failure naming no file → `{:ok false}` | **CLOSED** | `extract.clj:961-1035` | pure re-run, five inputs: foreign `vendor/core.clj` → `:unverified :failure-outside-the-changed-files`; our `app.moved` not located → `{:ok false :reason :changed-namespace-not-on-classpath}`; a foreign ns not located → `:unverified :classpath-incomplete`; no file named → `:unverified :unattributable`; our own file in the trace → `{:ok false}` |
| 6 | a symlinked file inside the root counted twice, link replaced by a regular file | **CLOSED** | `extract.clj:527-615` | fixture `w2b` (`alias_caller.clj -> caller.clj`): `:external-callers-rewired` has exactly ONE entry, `src/app/caller.clj`; the real file requires `app.distillery`; `alias_caller.clj` is still a symlink after `execute!` |
| 7 | `:to /outside/pwn.clj` applied and stamped an illegal ns | **CLOSED** | `extract.clj:375-462` | fixture `wD`: `/…/opus-rf2-fx/pwn.clj` → `:target-outside-project-root`, and `pwn.clj` does not exist afterwards; `src/app/2bad.clj` → `:invalid-target-namespace` (`app.2bad`); `src/app/ok_target.clj` → accepted as `app.ok-target` |
| 8 | `load-project-aliases` walk; two interpolated `:command` strings | **FILED, not fixed — as agreed** | `forms.clj:249-262`, `docs/intent/extraction-rewire/extraction-rewire-design.md` "Filed, not fixed" | commit 5839b52 records both with the intended fix. Residual injection surface in `:would` is now only `:file` (operator-typed), since `:alias` is a validated simple symbol (EXTRACT-031) and `:to` is confined + legal-ns checked (EXTRACT-038). Acceptable as filed |

## Part 2 — the ruling

> *a discovered caller whose canonical real path lands inside a tree the walk pruned
> (.git, target, node_modules, link-outside-root) must be a typed refusal before any byte,
> never a write.*

**PARTIAL.** Closed on `:extract` / `:extract!`; open on the MCP extraction entrance.

**Closed, exactly as specified (the assigned probe, re-run):** fixture `w1`,
`src/app/alias_caller.clj -> ../../.git/hooks/caller.clj`, `extract/execute!` returns

```
:error-type          :caller-path-in-skipped-tree
:path                /…/w1/src/app/alias_caller.clj
:resolves-to         /…/w1/.git/hooks/caller.clj
:tree                /…/w1/.git            (class java.lang.String)
:source-unchanged true  :target-unchanged true
```

and my post-conditions: `.git/hooks/caller.clj` **byte-identical** (SHA-256 before == after),
`alias_caller.clj` **still a symlink**, **no target created**, source still holds `defn distill`.
The same refusal fires on the dry run and with `:rewire-callers false`. A **chain** of links
(`a.clj -> b.clj -> .git/hooks/c.clj`, fixture `w5`) is fully resolved and refused, `.git`
byte-identical, both links intact. The **non-pruned** counterpart (fixture `w2b`) is unaffected:
one caller plan, the real file written, the link preserved.

**Open on the MCP entrance — see finding 1.**

## Part 3 — findings, in fix order

### 1. MED-HIGH — the ruling's refusal does not exist on the MCP extraction entrance; I wrote into `.git/hooks/` through it
`src/clj_surgeon/mcp_tool.clj:323` · `src/clj_surgeon/mcp_workspace_sources.clj:14-19` ·
`src/clj_surgeon/mcp_extraction.clj:211,442`

`execute-extraction!` never calls `extract/plan-raw`. It builds its source universe with
`workspace-sources/read-all` and calls `extract/compile-plan` directly with
`:rewire-callers false`, so `walk-workspace-sources`, `canonical-workspace-paths` and
EXTRACT-037's prune refusal are all bypassed. `read-all` uses `file-seq` (which follows
directory symlinks), excludes `.git` by the **lexical** test
`(remove #(str/includes? (.getPath %) "/.git/"))` on the walk path, and then keys the map by
`.getCanonicalPath` — so a symlink named under `src/` defeats the filter and lands the `.git`
file in the universe under its canonical name. `mcp-paths/resolve-source-path` then admits it
(inside root, regular file), and `mcp-extraction/commit!` writes `future-sources` with
`file-ops/atomic-write!` keyed by that canonical path.

**Witness (fixture `w9`, `src/app/alias_caller.clj -> ../../.git/hooks/caller.clj`, the only
caller):**

```
read-all source keys:  /…/w9/.git/hooks/caller.clj
                       /…/w9/src/app/state.clj
resolve-source-path "src/app/alias_caller.clj" -> {:ok true :path "/…/w9/.git/hooks/caller.clj"}
compile-extraction   -> ok: true
future-source keys   -> ("/…/w9/.git/hooks/caller.clj" …distillery.clj …state.clj)
commit!              -> ok: true
.git/hooks/caller.clj CHANGED BY MCP: true
  (defn go [x] (distillery/distill x))
```

**Fix:** route the MCP extraction's caller/source set through the same gate — either reuse
`canonical-workspace-paths`, or give `read-all` the NOFOLLOW walk and prune-by-canonical-path
that `walk-workspace-sources` now has. A single-entrance rule ("one rule, two entry points") is
what `resolve-discovered-source-path`'s own docstring claims; today extraction has three.
This is pre-existing code, but the ruling is not entrance-scoped, so I score it against the ruling.

### 2. MED — canonicalising the pruned set forbids trees the walk actually READ; `out -> src` refuses every extraction
`src/clj_surgeon/extract.clj:509-524` (`skipped-tree-prefixes`) · `:564-573` (`in-pruned-tree`)

`skipped-tree-prefixes` canonicalises each pruned entry. When a pruned NAME at the root is a
symlink to a real in-root directory, the prune prefix becomes that real directory — which the
walk descended and read. Every source in it then refuses.

**Witness A** (fixture `w4`, `target -> build_out`, `build_out/` a real root-level dir the walk
read): `extract/plan` → `:error-type :caller-path-in-skipped-tree`,
`:path "/…/w4/build_out/gen.clj"`, `:tree "/…/w4/build_out"` — a file the walk had just read.

**Witness B** (fixture `w8`, `out -> src`): every extraction in the workspace refuses, and the
error names the **source file itself**:

```
:error "A workspace path resolves into a tree this walk does not read, and this extraction
        will not write there: /…/w8/src/app/state.clj -> /…/w8/src/app/state.clj"
:tree  /…/w8/src
```

The message is self-contradictory — the walk did read that tree — and the workspace has no
remedy short of deleting the symlink. Same class as round-2 item 4 (an over-broad fatal refusal
for a tree that was never going to be a problem), reintroduced by canonicalisation.

**Fix:** prune by the tree the walk actually declined to enter. Record the resolved target on the
`:build-tree` branch (`extract.clj:310-313` conjes no `:resolves-to`) and subtract from the prune
set any canonical prefix that also appears as an ancestor of a path in `(:files walked)` — i.e.
a tree the walk demonstrably read is not a pruned tree, whatever spelling reaches it.

### 3. MED — an unreadable directory disappears silently; the receipt still says `:complete true`
`src/clj_surgeon/extract.clj:282`, `:342` (and `:232,245` in `holds-clojure-sources?`)

`(or (.listFiles entry) [])` swallows a `null` return (permissions, I/O). The directory is not
descended, is **not** recorded in `:skipped-directories`, and `scan-gap` therefore sees no gap.

**Witness** (fixture `w6`, `src/app/locked` mode 000 holding one caller): `extract/plan` →
`:discovery {:files 1}`, `:complete true`, `:callers-unresolved []`, no `:skipped-directories`.
That is precisely the failure round-2 items 1 and 2 were raised for — a caller silently dropped
under a success receipt — surviving in a different branch of the same walk.

**Fix:** treat `nil` from `.listFiles` as a skipped directory with a distinct, **non-harmless**
reason (`:unreadable`), so `scan-gap` makes `:complete` false and names it.

### 4. LOW — the pre-write gate is not the gate its docstring claims; EXTRACT-037 has no pre-write half
`src/clj_surgeon/extract.clj:470-506` (`confine-workspace-paths`) · `:2075-2077`, `:2122`

`confine-workspace-paths` is documented as running "once where the walk turns the workspace into
a read set, and once in the instant before the first `atomic-write!` … because the filesystem can
change between proving a plan and committing it." The plan-time call is
`canonical-workspace-paths` (dedup + outside-root + **prune** check); the pre-write call is
`confine-workspace-paths` (outside-root only). The prune check has no pre-write half, so the two
calls are not the same gate.

Practically bounded — plan-time paths are already canonical, `atomic-write!`'s `Files/move`
replaces a symlink rather than following it (`file_ops.clj:20-33`), and the caller hash fence at
`extract.clj:2093-2099` would catch a swap unless the attacker made the pruned file
byte-identical — so I did not find an exploitable TOCTOU. But the docstring asserts a symmetry
that is not there, which is the kind of claim a later reader builds on.

**Fix:** make the pre-write call the same function (pass the walk's `:skipped-directories`
through the plan), or correct the docstring to say which check runs twice and which runs once.

### 5. LOW (usability) — the refusal is total and drops its context
`src/clj_surgeon/extract.clj:1938-1948`

One bad link refuses the **dry run** (`:extract`) and refuses with `:rewire-callers false`, neither
of which writes a caller byte, and the refusal map carries no `:discovery`, so the reader cannot
see what was scanned or which other trees were pruned. Witnessed on `w1` (dry run and
`:rewire-callers false` both `:caller-path-in-skipped-tree`) and `w4`/`w8` (`:discovery nil`).
Attach `:discovery` to the refusal, and consider letting the dry run *report* the offending link
as an unresolved caller rather than refusing to preview.

## Part 4 — hunts that came back clean (each re-run, so the negatives are evidence)

- **`:tree` never prints `true` or nil.** `in-pruned-tree` returns the matched prefix string
  (`extract.clj:566-573`); observed `java.lang.String` in all four refusals (w1, w4, w5, w8).
- **Prefix comparison is segment-wise, not textual.** `(str/starts-with? candidate (str tree "/"))`
  at `extract.clj:568-572`. Fixture `w3b` with root-level `targeted/`, `.github/`,
  `node_modules_shim/` and `outer/` alongside a pruned-name vocabulary: no refusal,
  `:discovery {:files 5}`, and all four are rewired —
  `[".github/g.clj" "node_modules_shim/n.clj" "outer/o.clj" "targeted/t.clj"]`.
  `.github` vs `.git` is the sharp case and it passes.
- **A link recorded under one spelling that resolves elsewhere** is caught, because both the entry
  and its `:resolves-to` are canonicalised (`extract.clj:517-524`) — that is what makes w4/w8
  over-refuse rather than under-refuse.
- **A pruned tree that is itself a symlink pointing OUT of the root, holding sources** (fixture
  `w7`, `target -> /…/outside-build`, caller link into it): typed refusal, no write — though the
  type is `:caller-path-outside-root`, not `:caller-path-in-skipped-tree`, because the outside-root
  gate fires first. Acceptable; worth a sentence in the spec so a reader is not surprised.
- **Pruning by depth or ceiling does not exist**, so it cannot hide a tree: the walk has no depth
  limit, and the file cap is a hard refusal (`:workspace-file-cap-exceeded`, `extract.clj:348`,
  witnessed on `wB` with `:max-workspace-files 1`), not a silent truncation.
- **No symlink-cycle recursion.** The walk never descends a link (`extract.clj:315-336`) and
  `holds-clojure-sources?` skips links and is budget-bounded (`extract.clj:232-249`).
- **The refusal precedes every byte, read as well as write.** `workspace-sources` is slurped only
  under `(when-not (or escape over-cap) …)` at `extract.clj:1957-1960`, after `confined` is
  decided at `:1942-1945`.

## Part 5 — ratchets missing for round 3's own behaviour

`test/clj_surgeon/extract_test.clj:1562-1635` carries good witnesses for both halves of the
ruling (the dedup case and the `.git` refusal, with byte-identity and link-survival assertions).
No witness exists for: the MCP entrance (finding 1), a pruned tree that is itself a symlink to a
tree the walk read (finding 2), or an unreadable directory (finding 3). Each finding above should
land with the example test that reproduces it, per the ratchet rule.


## Part 6 — suite receipt (run by me, twice)

`flock /home/forge/tmp/suite.lock clojure -J-Xmx1g -M:clj-surgeon/mcp-test` in the scratch clone
at 465c956, run twice with identical results:

```
Ran 385 tests containing 4071 assertions.
1 failures, 0 errors.

FAIL in (exact-profile-compilation-is-project-owned-and-snapshot-bound)
  (mcp_change_buffer_test.clj:686)
expected: ["/opt/homebrew/bin/clj-kondo" "--lint" "src/app.clj" "--fail-level" "error"]
  actual: ["/usr/local/bin/clj-kondo"    "--lint" "src/app.clj" "--fail-level" "error"]
```

**Not rf2's.** The assertion hardcodes a macOS Homebrew absolute path; on Anvil `clj-kondo`
resolves to `/usr/local/bin`. The test was last touched by 7f691b8 ("Move provider checks into
analyzer contracts"), which is an ancestor of 5ccb4f0 — it pre-dates round 3, and
`mcp_change_buffer.clj` is not in the round-3 diff. **Every extraction test passes**, including
the two EXTRACT-037 witnesses.

Separately worth someone's attention (not a merge blocker, and not this branch's to fix): a
fleet gate that is red on every Linux box because one assertion is bound to `/opt/homebrew`
teaches seats to read "1 failures" as normal. `mcp_change_buffer_test.clj:686` should assert the
resolved path, not a host's spelling of it.

## Housekeeping

Scratch clone `/home/forge/tmp/opus-rf2` at 465c956 — no commit, no stash, no push
(`git log -1` is still 465c956; only untracked `probe*.clj`, `.cpcache/` and a
`.hot-transaction-*` directory left by the suite). Fixtures confined to
`/home/forge/tmp/opus-rf2-fx`. No MCP server started, no port contacted. One JVM at a time,
`-Xmx1g`, suites under the shared lock.
