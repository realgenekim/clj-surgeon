# study-ops-mcp 3a237ab — Opus executed round-3 re-check (Sol filter fallback): GO-WITH-FIX, one BLOCKER (ReDoS at match time) — round 4 launched

# study-ops round-three executed re-review at `3a237ab` — **GO-WITH-FIX**

**Reviewer / provenance.** Independent executed re-review of `bridge/study-ops-mcp` at `3a237ab`,
run by the Opus fallback reviewer. **OpenAI's content filter refused this review twice on the
`rg`/confinement fixtures**; this seat is the fallback path and every number below was measured
here, in a scratch clone (`/home/forge/tmp/opus-study`, remote re-pointed to the GitHub origin,
nothing committed, nothing pushed, no server started, synthetic trees only under
`/home/forge/tmp/opus-study-fx`).

## Headline

Eleven of eleven round-two items are materially closed and the fixes are good — the dedup rewrite
turns 8.4 s / 250,500 files into **114 ms / 500 files, each file listed exactly once**; the source
budget stops charging a read count, so all five files in this repo above 65,536 characters now
outline `ok=true read_complete=true` with **0 returned source**; thirteen malformed `ls-tree`
parameter shapes refuse typed with no exception and no schema-breaking value; the futile
continuation is gone. **One new hole survives round three and it is the reason this is not a
plain GO:** `ns_grep` is compiled under a guard but **matched without one**. A single MCP call —
`ls-tree dir="src" ns_grep="(.*.*.*.*.*.*)*x"` — burned **41.8 seconds** of CPU on 67 files, and
each extra `.*` multiplies the cost about six-fold. At the default `max_files` 2000 the same
pattern is ~21 minutes; at the 20,000 ceiling, ~3.5 hours; two more `.*` puts it past a day.
There is no timeout, no length bound, and no cancellation anywhere on that path. The `grep` path
is immune (ripgrep, 177 ms) — the exposure is exactly the branch-introduced `ns_grep`.

**Merge mechanics, verified:** the branch is 139 commits behind `origin/main` and 28 ahead.
In that window `origin/main` touched exactly **one** file under `src/`, `test/`, `test-fixtures/`,
`Makefile`, or `deps.edn` — `test/clj_surgeon/mcp_change_buffer_test.clj` — and the branch does not
touch it. Overlap is `CLAUDE.md` and `docs/tech-tree.md` only. The branch's own suite is
**418 tests / 5451 assertions / 1 failure**, and that one failure is
`exact-profile-compilation-is-project-owned-and-snapshot-bound` asserting the hardcoded
`/opt/homebrew/bin/clj-kondo` — **the very test `origin/main` rewrote to be host-independent**.
Absorbing main takes this branch's baseline to zero; the failure is not branch-introduced.

## The eleven round-two items, re-run here

| # | round-2 item | verdict | witness (re-run by me) |
|---|---|---|---|
| 1 | source budget charged the READ count | **CLOSED** | all 5 files >65,536 chars: `ok=true read_complete=true`, `returned-source=0`, `source_character_count` = the true read (126,596 / 86,670 / 76,346 / 72,714 / 69,133) |
| 2 | `max_files` bounded a materialised count; overlapping projects double-counted | **CLOSED** (one residual, #3 below) | fx-a (500 files + 500 sibling `deps.edn` `:paths [".."]`): `file_count 500`, 1 project, distinct=500, sum-per-project=500, **114 ms**. fx-d (root `:paths ["."]` + nested project): 2 files, each printed once, nested file owned by the nested project |
| 3 | `ns_grep` `re-pattern` unguarded; `grep:5` broke the output schema | **CLOSED for compile+type / OPEN for match** (#1 below) | 13 shapes probed: `ns_grep "["` → `invalid-ns-grep-pattern`; `ns_grep 5`/`grep 5`/`grep []`/`limit "x"`/`limit 1.5`/`limit true`/`dir 5`/`format 5`/`max_files "9"` → `invalid-parameter-type` naming parameter+expected+actual, value never echoed; all `read_complete=false source_unchanged=true`; **no exception thrown on any** |
| 4 | spelling `limit 4096` revived the self-returning `next_call` | **CLOSED** | `{dir "." limit 4096}` → `next_call {mode, dir ".", limit 16384}`; same with `format`/`max_files` spelled; the continuation now carries `limit` |
| 5 | per-project headers printed shown counts; a dropped project vanished | **PARTIAL** (#2 below) | limit 5000 → `── alpha (50 files, 50 forms)` / `── beta (50 files; 23 shown)`; limit 2000/600/200 → `── beta (50 files; 0 shown)` present. **At limit ≤100 (`returned=0`) the body is the total line alone — zero headers — while `project_count` still says 2** |
| 6 | `invalid-grep-pattern`'s continuation echoed the rejected pattern | **CLOSED** | `grep "-x"` → `next_call {mode, dir "src"}`; `ns_grep "-x"` and `ns_grep "["` likewise; the caller's `dir` is kept, the pattern dropped |
| 7 | refusals embedded the canonical root | **CLOSED** | 6 refusal kinds: no `/`-rooted path in `error`, no workspace root anywhere in the receipt. `"Discovery found 162 Clojure files under ., above the 3-file scan cap"`; `"No Clojure files found under src matching 'zzzzz-no-such-token'"` |
| 8 | known-futile `next_call` at the ceiling | **CLOSED** | `ls-deps execute-mcp-change!` in `intent_transaction.clj`: `required=22141` at limit 4096 **and** at 16384 → `next_action="narrow_scope"`, `next_call=nil` |
| 9 | nothing pinned the parallel strategy | **CLOSED** | `test/clj_surgeon/mcp_study_test.clj:1171` redefs `parallel/bounded-map` and counts calls — a swap to serial `map` in `ls-tree-bounded` makes it fail. Partial-n equality re-run by me over the real 67-file `src/` tree: `outline-take` under `map` = under `bounded-map` for **every n in 0..40** |
| 10 | rendered `rg` remedy lacked `--` | **CLOSED** | `src/clj_surgeon/core.clj:740`; 8 patterns rendered, all with `--` before the pattern and all correctly single-quoted (spaces, `'`, newline, `$(id)`, backticks). It is the only rendered `rg`/`grep` command in `src/` |
| 11 | `text` floor undocumented; `-prune` hid `src/app/target/` unwitnessed | **PARTIAL** (#4 below) | the golden (`test-fixtures/study/ls-tree-prune-target.golden.txt`) holds both `target/foo.clj` and `src/app/target/bar.clj` and lists neither — correct. The floor **test** computes the expected string rather than hardcoding it — correct. The **source comment's number is mislabelled** |

## What round three introduced — the hunt

### 1. BLOCKER — `ns_grep` is guarded at compile time and unguarded at match time (ReDoS)

`src/clj_surgeon/study.clj:388` compiles once under `try` (the round-three fix, correct).
`src/clj_surgeon/study.clj:404` (`ns-grep-hit?`, `re-find` at `:416`) then runs it on `java.util.regex` — a backtracking engine —
**twice per discovered file** (raw relative path, then the `_`→`-` variant), with no step budget,
no wall-clock deadline, no subject-length bound, and nothing on the path that can cancel it
(`grep -rn "timeout" src/clj_surgeon/{mcp_inspect_tool,mcp_inspect,study}.clj` → nothing).

Measured through the real entrance, `tool/execute-ls-tree {:project-root <repo>} {:mode "ls-tree" :dir "src" :ns_grep <pat>}`:

| `ns_grep` | wall (67 files) | extrapolated at `max_files` 2000 | at the 20,000 ceiling |
|---|---|---|---|
| `(.*.*.*)*x` | 294 ms | ~9 s | ~1.5 min |
| `(.*.*.*.*)*x` | 1,242 ms | ~37 s | ~6 min |
| `(.*.*.*.*.*)*x` | 7,091 ms | ~3.5 min | ~35 min |
| `(.*.*.*.*.*.*)*x` | **41,804 ms** | **~21 min** | **~3.5 h** |
| same pattern as `grep` (ripgrep) | 177 ms | — | — |

The subject is an ordinary 36-character repo path (`src/clj_surgeon/mcp_inspect_tool.clj`) — no
adversarial file names needed; the pattern alone does it. **The whole cost is paid inside
`study/ls-tree`, before `ls-tree-bounded` and before any byte budget applies** — exactly the
"work between 'directory named' and 'first byte budgeted'" that MCP-OP-STUDY-015 exists to bound,
now re-entered through the regex instead of through the walk. `ns_grep` is MCP-only; the CLI never
exposes it, so this is a read-entrance surface with an agent (or a prompt-injected agent) on the
other end. Fleet context: this is the same class as the scar in house rules — an unbounded local
operation that starves a shared box while looking like a normal call.

**Ratchet (rung d/e):** run each `re-find` against a step-counting `CharSequence` that throws once
a budget is exceeded, and convert that into the existing typed refusal
(`invalid-ns-grep-pattern`, or a new `ns-grep-too-costly` with the same shape:
`read_complete=false`, `source_unchanged=true`, continuation dropping `ns_grep`). Witness that
fails first: `(.*.*.*.*.*.*)*x` over `dir "src"` returns a typed refusal in under 100 ms.
A wall-clock future is *not* sufficient — `future-cancel` cannot interrupt a running matcher.

*Context, not a defence:* `src/clj_surgeon/edit_dsl.clj:44` already allows `re-pattern`/`re-find`
inside the `xray` SCI fence, so the same class is reachable there. That is pre-existing and out of
scope for this branch, but it means no wall-clock guard exists anywhere in the read entrance and a
fix here should be shaped so it can be reused there.

### 2. FIX — at `returned=0` the `text` body drops every project header while `project_count` still counts them

`src/clj_surgeon/mcp_inspect_tool.clj:434` builds the empty receipt as
`(ls-tree-render [] dir output-format total)` — an **empty project vector** — whereas every
`n ≥ 1` attempt goes through `study/outline-take`, which deliberately keeps all projects
(`src/clj_surgeon/study.clj:522-525`). So the fix for item 5 stops exactly one file short of the
floor. Measured on a two-project / 100-file fixture:

```
limit=200  project_count=2 returned=1 headers=["── alpha (50 files; 1 shown)" "── beta (50 files; 0 shown)" "── total: …"]
limit=100  project_count=2 returned=0 headers=["── total: 100 files; 0 shown, 100 omitted"]
limit=1    project_count=2 returned=0 headers=["── total: 100 files; 0 shown, 100 omitted"]
```

This is the same body-contradicts-its-own-receipt defect item 5 named, surviving at `returned=0`.
It is also a **spec contradiction the branch introduced**: MCP-OP-STUDY-024 says *"Every project
counted by `project_count` shall appear in the body, including one the byte budget reached no file
of, which reads `0 shown`"*, while MCP-OP-STUDY-030 says the `text` floor is *"its own trailing
total line"* — and the round-three test at `mcp_study_test.clj:953` asserts the total line **and
nothing else**. Both cannot be true. Decide which, then make the other say so; the existing
STUDY-024 test (`mcp_study_test.clj:1064`) only exercises limits 5000 and 600, so nothing fails today.

### 3. FIX (or amend the spec) — the cap stops the walk *between* candidates, never *inside* one

`src/clj_surgeon/study.clj:275` checks `(> (count seen-after) cap)` **after** a whole candidate's
files have been walked and canonicalised. Measured:

- fx-a, 500 candidates, `max_files 10` → refuses in 39 ms, `"Discovery found **at least** 500 …"` ✅
- fx-b, **one** project of 3000 files, `max_files 10` → refuses in 59 ms with
  `"Discovery found **3000** Clojure files under ., above the 10-file scan cap"` — the full `find`
  ran and `mcp-paths/real-path-within` was called 3000 times, i.e. one `toRealPath` syscall per
  file, at a cap of 10.

MCP-OP-STUDY-021 as written permits this (*"no candidate after the one that passes the cap shall
be listed"*), and the branch's own test says so out loud (`mcp_study_test.clj:420`, and its `:448` `testing` block, *"a single
oversized project still names its exact count"*). But the round-two remedy this closes was worded
*"apply the cap during accumulation"*, and per-candidate is not per-file. **The claim is
oversold, not false.** Either bound the per-candidate walk (stream `find` output and stop reading
at `cap+1`) or say plainly in STUDY-021 that a single project's walk is unbounded by `max_files`.

### 4. FIX (doc) — the documented floor is attached to the wrong tree width

`src/clj_surgeon/mcp_inspect_tool.clj:426`: *"`text` bottoms out at its own trailing total line —
**38 characters for a two-digit tree**"*. Measured, the floor is `35 + digits(file_count) +
1 + digits(omitted)`:

| `file_count` | floor | payload |
|---|---|---|
| 5 | **38** | `── total: 5 files; 0 shown, 5 omitted\n` |
| 50 | **40** | `── total: 50 files; 0 shown, 50 omitted\n` |
| 100 | 42 | `── total: 100 files; 0 shown, 100 omitted\n` |
| 20000 | 46 | `── total: 20000 files; 0 shown, 20000 omitted\n` |

38 is a **one**-digit tree (the 7-file fixture the test uses); 40 is the two-digit tree the prior
reviewer measured. The test itself is fine — it computes the string rather than hardcoding a
constant — but STUDY-030 says the floor shall be "documented", and the documentation is wrong.
Say `35 + digits`, not a single number.

### 5. OBSERVATION — the per-request *source* budget is now unreachable at default limits

`returned-source-character-count` (`src/clj_surgeon/mcp_inspect.clj:878`) sums every `:source`
string, and `enforce-output-budget` (`:917`-`:920`) checks it against `per-request-source 65536` before
checking `json-character-count` against `per-request-result 65536`. Returned source is always a
JSON-escaped substring of the result, so **`returned-source ≤ result-chars` identically** — with
both limits at 65,536 the source check can never fire alone. Measured: a 70,000-char `:source`
gives `returned-source=70000`, `json-chars=70054`, and the refusal is the same
`inspect-output-limit` either way. MCP-OP-STUDY-020's second clause is therefore proved only by
lowering the limit in a test, never in production shape. Not a hole — the aggregate bound still
holds — but the two limits should differ, or the source check should be documented as a
diagnostic rather than a bound.

Related, and answering the hunt directly: **yes, one op returns source-derived text under a key
other than `:source`, and it is uncharged** — `outline` emits each form's `:args` verbatim from
the file (2,696 characters for `intent_transaction.clj`, against a 15,243-character result).
Nothing else does: I walked every result of `outline`/`forms`/`match`/`deps`/`topo`/`ls-deps`/
`ls-extract` for every string ≥30 chars that is a verbatim substring of the file, and the only
hits were `:source` (charged, 5,549 chars in `forms`) and `:args`. `returned-source-character-count`
is also blind to `:text`, `:body`, and a string `"source"` key, and correctly sees a nested
`:source` — so the *mechanism* is right and the *coverage* is a naming convention, not a check.

### 6. OBSERVATION — canonical vs lexical identity: correct, with one silent false negative

Dedup is keyed on the **canonical** path (`study.clj:260`), so overlapping projects count once —
verified on fx-d. The memoisation key (`study.clj:211`) is the *lexical* directory string, but
`mcp-paths/normalized-path-within` normalises first, so `.`/`..`/`./src` spellings collapse and
the fx-a 500-sibling case memoises to a single walk. A symlink farm cannot defeat it either:
`find` runs with the default `-P` and never descends a symlinked project root (verified: 51 links,
`find` reports 1 `deps.edn`, `find -L` reports 51).

The flip side is a **silent false negative**: a `:paths` entry that points *through* a symlink is
walked by `find -P`, which does not follow it, so the project's sources are invisible. Measured —
`deps.edn {:paths ["srclink"]}` with `srclink -> realsrc` containing `z.clj` →
`{:ok false, :error-type :no-clojure-files}`. Pre-existing, low severity, worth one line in
STUDY-014.

## Disputed figures — ruled

- **Largest atomic result: the builder is right, 22,141, not 28,168.** `ls-deps` on
  `execute-mcp-change!` in `intent_transaction.clj` normalises to **exactly 22,141** JSON
  characters at both `limit 4096` and `limit 16384`. The prior reviewer's 28,168 does not
  reproduce for that subject. It is not an impossible number for this repo, though — scanning all
  1,681 owners in `src/`, the true maximum atomic `ls-deps` is **51,379**
  (`worktree_lifecycle_io.clj` / `-main`), with `command-result` at 51,248 and
  `apply-plan-file!` at 37,066. So the prior figure was a different subject, not an error of kind,
  and the fix behaves correctly at 22,141 and at 51,379 alike.
- **Empty `text` floor: the builder is wrong as stated, and so is the new comment.** Both 38 and
  40 are correct — for a one-digit and a two-digit tree respectively (table in #4). The prior
  reviewer's 40 was right for what they measured; the branch's replacement comment attaches 38 to
  "a two-digit tree", which is false. The number is `35 + digits`.

## Verdict for the merge queue: **GO-WITH-FIX**

Merge after **item 1** (the `ns_grep` match budget). It is a one-call, caller-supplied,
hours-of-CPU denial of service in the read entrance the whole fleet uses, with no cancellation
path, and it is branch-introduced. Items 2–4 are fix-class and can ride the same commit or the
next one; item 2 also needs the STUDY-024/STUDY-030 contradiction decided rather than left for a
future reader to discover. Items 5–6 are observations for the specs, not code.

Everything else in round three holds up under an independent re-run: 11/11 round-two items
materially closed, the two headline performance claims reproduced (114 ms and one walk for the
500×500 case; 10 refusal/continuation shapes correct), the strategy witness genuinely fails on a
swap, and the branch merges into `origin/main` with no source-file overlap and a baseline that
goes to zero once main's host-independent `clj-kondo` test comes across.

### Numbered fix list

1. **`src/clj_surgeon/study.clj:404` / `:416`** — `ns-grep-hit?` runs a caller-supplied backtracking regex
   with no budget. *Witness:* `ls-tree dir="src" ns_grep="(.*.*.*.*.*.*)*x"` returns `ok=true`
   after **41,804 ms** on 67 files; the same pattern as `grep` costs 177 ms.
2. **`src/clj_surgeon/mcp_inspect_tool.clj:434`** — the empty receipt renders `[]` projects, so at
   `returned=0` no project header is printed. *Witness:* two projects, `format="text"`,
   `limit=100` → `project_count=2`, body = `── total: 100 files; 0 shown, 100 omitted` and nothing
   else; at `limit=200` both headers appear.
3. **`src/clj_surgeon/study.clj:275`** — the cap is tested after a whole candidate is materialised.
   *Witness:* one project of 3000 files at `max_files 10` refuses with `file_count 3000` and no
   "at least", having realpath'd all 3000.
4. **`src/clj_surgeon/mcp_inspect_tool.clj:426`** — "38 characters for a two-digit tree".
   *Witness:* `(count (study/format-ls-tree-text [] "/x" {:file-count 50}))` = **40**;
   `file-count 5` = 38.
5. **`src/clj_surgeon/mcp_inspect.clj:920`** — `per-request-source` (65,536) can never fire before
   `per-request-result` (65,536), since returned source is a JSON-escaped substring of the result.
   *Witness:* a 70,000-char `:source` gives `returned-source=70000`, `json-chars=70054`, one
   refusal.
6. **`src/clj_surgeon/outline.clj:269`** (via `mcp_inspect.clj:555` `outline-result`) — `outline` returns each form's `:args` verbatim from
   the file, uncharged by the source budget. *Witness:* `intent_transaction.clj` outline →
   `returned-source=0`, `:args` chars = **2,696**.
7. **`src/clj_surgeon/study.clj:165`-`:193`** (`walk-clj-files`) — `find` runs `-P`, so a `:paths` entry through a symlink
   yields no files. *Witness:* `{:paths ["srclink"]}`, `srclink -> realsrc/z.clj` →
   `{:ok false, :error-type :no-clojure-files}`.

*Method note.* All probes ran in-process on an explicit classpath (`java -Xmx1g`, one JVM at a
time) rather than through the suite, per the queue guidance; the one full-suite run was
accidental (an alias `:main-opts` overrode `-e`) and is reported above because its single failure
turned out to be load-bearing evidence for the merge.
