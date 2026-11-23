# study-ops-mcp ec5a592 — Opus executed round-4 re-check (Sol filter fallback): GO-WITH-FIX (blocker closed 6/6; per-file budget term must scale with subject length) — round 5 launched

# study-ops-mcp `ec5a592` — Opus executed round-4 re-review (Sol filter fallback): **GO-WITH-FIX**

**Reviewer / provenance.** Independent executed re-review of `bridge/study-ops-mcp` at `ec5a592`,
run by the **Opus fallback reviewer**. OpenAI's content filter refuses this branch's
`rg`/confinement fixtures, so Sol cannot review it; this seat is the fallback path and every
number below was measured here, in a scratch clone (`/home/forge/tmp/opus-study2`, remote
re-pointed to the GitHub origin), nothing committed, nothing stashed, nothing pushed, no server
started, synthetic trees only under `/home/forge/tmp/opus-study2-fx`. Probes ran in-process on an
explicit classpath (`java -Xmx1g`, one JVM at a time); the two suites ran under
`/home/forge/bin/suite-run`.

## Headline

**The round-3 BLOCKER is genuinely closed, and the fix is the right shape.**
`ls-tree dir="src" ns_grep="(.*.*.*.*.*.*)*x"` went from **43,589 ms `ok=true`** to a
**15.9 ms typed refusal** (`ns-grep-match-budget-exceeded`), no file parsed, continuation drops
`ns_grep`, honest `mcp` unchanged at **405.7 ms**. The mechanism is the one the round-3 remedy
named — a step-counting `CharSequence`, not a wall clock — and I confirmed empirically that
`java.util.regex` reads the subject **only** through `charAt` (15 constructs, lookbehind and
backreferences included) and that **backtracking re-reads are counted** (1,006,476 charges for a
36-character subject). All five other round-3 items are closed. All three gates are green.

**One new hole, and it is the reason this is not a plain GO:** the budget is a flat 20,000
characters per file, but the cost of an ordinary `.*a.*b.*` pattern is **quadratic in path
length**, and the constant was calibrated against this repository's 36-character paths.
A completely honest pattern — `ns_grep=".*handler.*internal.*"` — over a 2,000-file monorepo with
111-character source paths is now **REFUSED**. That call would have cost **150 ms**. The
catastrophic pattern it exists to stop costs **5,846 ms per file**. The real gap is roughly
39,000×; the threshold was placed at **2.07× under** the honest cost.

| | measured here |
|---|---|
| `(.*.*.*.*.*.*)*x` over `src` (67 files), round 3 | 43,589 ms, `ok=true` |
| same, at `ec5a592`, through `tool/execute-ls-tree` | **15.9 ms**, `ok=false`, `error_type=ns-grep-match-budget-exceeded`, `match_budget=1340000`, 0 files parsed, `next_call {:mode "ls-tree" :dir "src"}` (no `ns_grep`) |
| `(.*.*.*)*x` / `(.*.*.*.*)*x` / `(.*.*.*.*.*)*x` | 28.7 / 23.0 / 16.4 ms, all the same typed refusal |
| honest `mcp` over `src` | **405.7 ms**, `ok=true`, `file_count=32`, `project_count=1` — unchanged |
| **honest `.*handler.*internal.*` over 2,000 files, 111-char paths** | **REFUSED** at 219.8 ms — 41,322 chars/file against a 20,000 allowance. Unbudgeted cost of the same work: **149.6 ms** |

## Gates, run once each under `suite-run`

| gate | result | builder's claim |
|---|---|---|
| `make mcp-test` | **427 tests / 5,569 assertions / 0 failures / 0 errors**, `EXIT=0`; all six sub-self-tests passed | 427/5569/0 ✅ |
| `make test-fast` | **724 tests / 5,997 assertions / 0 failures / 0 errors**, `EXIT=0` | 724/5997/0 ✅ |
| `mcp-operation-oracle` (swipl) | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | pass ✅ |

All three reproduce the builder's figures exactly.

## The six round-3 items, re-run here

| # | round-3 item | verdict | witness (mine) |
|---|---|---|---|
| 1 | **BLOCKER** — `ns_grep` compiled under a guard, matched without one | **CLOSED** | table above; `src/clj_surgeon/study.clj:509` `budgeted-subject`, `:534` `ns-grep-hit?`, `:577` pooled sizing, `:1022` the typed catch in `ls-tree`, `src/clj_surgeon/mcp_inspect_tool.clj:739` the continuation |
| 2 | empty receipt rendered `[]` projects; STUDY-024 vs STUDY-030 contradiction | **CLOSED** | two 50-file projects, `format="text"`: at `limit` 100 / 40 / **1** the body is `── alpha (50 files; 0 shown)` · `── beta (50 files; 0 shown)` · `── total: 100 files; 0 shown, 100 omitted`. `mcp_inspect_tool.clj:461` now renders `(study/outline-take projects 0 (atom {}))`. The spec contradiction is **decided, not deferred**: `docs/intent/study-ops/study-ops-specs.md:68` marks STUDY-030 "SUPERSEDED in part … STUDY-024 wins", and STUDY-032 (`:74`) states the new floor |
| 3 | cap tested only after a whole candidate materialised | **CLOSED** | `study.clj:166` `walk-clj-files` (`into` + `take limit` transducer, `:203-210`) and `:371` `discover-projects` (`limit (inc cap)`, `:386`). Instrumented `mcp-paths/real-path-within`: **3,000-file project at `max_files 10` → 12 calls** (was 3,001), 37.3 ms, `file_count 11`, `observed_at_least true`, message says "at least 11". Holds on 10,000 files (12 calls) and on the no-build-file fallback (11). At `max_files 2000`: 2,002 calls — exactly `cap+2` |
| 4 | floor comment said "38 characters for a two-digit tree" | **CLOSED** | `mcp_inspect_tool.clj:441` now says `36 + 2 x digits(file_count)`. Measured floors 38 / 40 / 42 / 46 for `file_count` 5 / 50 / 100 / 20000 — the formula matches at every width |
| 5 | `outline`'s `:args` returned verbatim source uncharged | **CLOSED** | `mcp_inspect.clj:901` enumerates `#{:source "source" :args "args"}`. `outline` of `intent_transaction.clj` now charges **2,696** (was 0). And my round-3 observation that `request_source` could never fire is **resolved in effect**: source is tested before result at `mcp_inspect.clj:931` (inside `enforce-output-budget`, `:914`), so an arglist overrun reports `scope="request_source" actual=70000` (the actionable remedy) while a derived-structure overrun still reports `scope="request_result" actual=70026`. Both scopes reachable, correctly distinguished |
| 6 | a `:paths` entry through a symlink yielded nothing, silently | **CLOSED** | `study.clj:250` `unresolved-source-dir`, `:265` `walkable-source-dirs`. On **success**: `{:ok true :file_count 1 :paths_unresolved [{:project "symproj" :path "srclink" :reason "symlink"}]}`. On **refusal**: same key plus `remedy "Declared source path \"srclink\" could not be walked (symlink): scan the directory the link resolves to, or declare its real path in :paths."` — the round-3 complaint (being told to "scan a directory that contains Clojure sources") is gone |

## What round four introduced — the hunt, item by item

### (a) The pooled budget — **a real false positive, with a plausible trigger**

The pool is `ns-grep-match-steps-per-file` (20,000) × the number of files discovery found
(`study.clj:577`, inside `filter-projects-by-ns-grep` at `:560`). Sizing it on the file count is **the right shape**: the allowance and the work
both scale linearly with the tree, so *adding files can never turn a passing call into a failing
one* — I confirmed this on 10,000 files (deep) and 10,000 files (flat), where every honest
pattern answered. The unit that actually decides is the **per-file average**, and that is where
the calibration fails.

**The cost of `.*a.*b.*` against a non-matching subject is O(len²).** Measured with the shipped
`ns-grep-hit?` and a large pool:

| pattern | 36-char path | 70-char path | 97-char path | 111-char path |
|---|---|---|---|---|
| `.*handler.*internal.*` | — | 17,304 | **27,306** | **41,322** |
| `.*handler.*internal.*platform.*` | 2,478 | 15,984 | **26,418** | — |
| `.*stage.*pipeline.*ingest.*platform.*` | — | 12,806 | **28,644** | — |
| `.*/.*/.*/.*\.clj` | 7,038 | 489 | — | — |
| six-way alternation | 134 | 842 | 1,194 | — |
| `mcp` (literal) | 19 | 144 | 196 | — |

The 20,000 allowance corresponds to a path length of roughly **77 characters**. A Java-shaped
Clojure monorepo (`packages/backend/services/ingestNN/src/main/clojure/com/example/platform/…`)
exceeds that routinely, and `.*a.*b.*` is the most natural `ns_grep` an agent writes. Proved
through the real entrance on a 2,000-file fixture with 111-character relative paths:

```
handler0                          ms= 163.7  ok=true   file_count=200
.*handler.*internal.*             ms= 219.8  ok=false  ns-grep-match-budget-exceeded  budget=40000000
.*handler.*internal.*pipeline.*   ms= 198.1  ok=false  ns-grep-match-budget-exceeded  budget=40000000
(handler|service)                 ms= 105.1  ok=true   file_count=2000
.*internal.*                      ms= 117.8  ok=true   file_count=2000
```

**Severity is bounded and the failure is honest** — it is a typed, fail-closed refusal with a
continuation that drops `ns_grep`, so the caller recovers in one call. That is why this is
GO-WITH-FIX and not NO-GO. But three things are wrong and two of them are cheap:

1. **The threshold is misplaced by ~1–2 orders of magnitude.** Honest worst measured ≈ **3.35 ×
   len²**; the catastrophic pattern costs **776 × len²** at len 36 and grows exponentially with
   nesting depth. A per-file allowance of `max(20000, 64 × len²)` sits ~19× above the worst honest
   cost at len 111 (788,544 vs 41,322) and still refuses `(.*.*.*)*x` at len 36 after ~83,000
   reads — under a millisecond. Keep the pool; make the per-file term a function of the subject.
2. **The remedy names the wrong cause.** `study.clj` / `mcp_inspect_tool.clj:1024`:
   *"Use an ns_grep pattern without nested unbounded repetition."* The pattern that actually
   trips it — `.*handler.*internal.*` — has **no nested repetition at all**. An agent reading that
   remedy will look for a nesting it does not have.
3. **The docstring's calibration claim is falsified.** `study.clj:490-494`: *"20,000 per file is
   roughly sixty times the worst honest cost measured here and an order of magnitude below the
   cheapest catastrophic one."* Measured, an honest cost is **2.07× OVER** it. The claim is true
   only of 36-character paths, which is the tree the author measured on.

**On "per-subject with a pass cap" (the question asked):** pooling is the better bound and should
stay — a per-subject cap alone lets a pattern costing just under it be paid once per file, which
is the very DoS being closed. What pooling gives up is *diagnosis*: the refusal cannot name which
file or which subject drained it (`match_budget=40000000` and nothing else). The right shape is
**pooled, with a length-scaled per-file term, and the exhausting subject named in the refusal**.

### (b) The step-counting `CharSequence` — **CLOSED, one latent residual**

I built a strict `CharSequence` whose `subSequence` and `toString` **throw**, and ran 15 regex
constructs against a real path: literal, anchored, `.*`-globs, six-way alternation, positive and
negative **lookbehind**, **backreference**, word boundary `\b`, `(?i)`, `(?u)(?i)`, `(?m)`, `(?s)`,
`(?x)`, POSIX classes, and a supplementary-plane range `[\x{1F600}-\x{1F64F}]`.
**Every one matched without touching `subSequence` or `toString`.** The engine reads the subject
only through `charAt`, so the counter sees everything the match costs.

**Backtracking re-reads are counted, and this is the load-bearing property.** The shipped
`budgeted-subject` charged **1,006,476** reads for `(.*.*.*)*x` against a 36-character subject —
27,957× the subject length. A counter that charged only distinct positions would have been
useless.

**Residual (latent, not exploitable today):** `budgeted-subject` (`study.clj:519-520`) delegates
`subSequence` to the underlying `String` and `toString` to the raw string, both **uncounted**.
Today the only caller is `Matcher.group()` during `re-find`'s group extraction — post-match,
`O(match length)`, and it must work (I confirmed a grouped `ns_grep="(mcp)_(inspect)"` answers
`ok=true file_count=2` through the entrance). But the invariant the docstring asserts is *"exactly
`budget` reads succeed; the next one throws"*, and two methods on the same object silently break
it. One-line ratchet: return a wrapped, pool-charged sequence from `subSequence`, and charge
`length` characters in `toString`.

### (c) The cap as a transducer — **CLOSED, one residual worth a line in STUDY-033**

Instrumented `real-path-within` on my own 3,000-file project at `max_files 10`: **12 calls**
(eleven walk entries plus the project's own `deps.edn`), 37.3 ms, exactly the bound the loosened
witness (`test/clj_surgeon/mcp_study_test.clj:1558`, `(is (<= @calls 12))`) states. I found **no
other path that reads past the cap**: 10,000 files in one project → 12; 10,000 files with no build
file (the fallback branch, `study.clj:400-409`) → 11; `discover-projects-grep`'s orphan-hit branch
also takes `(take limit)` (`study.clj:891`). The loosening from ≤11 to ≤12 is honest — the extra
call is `find-build-files` canonicalising the project's own `deps.edn`, not a walk entry.

**Residual:** the docstring at `study.clj:383-385` says the bound "keeps the syscalls proportional
to the cap instead of to the tree." That is true of `toRealPath` and **false of the walk itself**:
`find` still enumerates the entire tree and its stdout is fully materialised into one JVM string
by `:out :string` before the transducer runs. Measured at `max_files 10` over the 10,000-file
corpus: **1,130,000 bytes** read into heap, and wall time still tracks tree size (37 ms at 3,000
files vs 69 ms at 10,000, both at cap 10). Fine at today's ceilings; it is the real bound on a
huge tree, and the sentence should say `toRealPath` rather than "syscalls".

### (d) `:args` charged — **CLOSED, uniformly**

The charge is at **one chokepoint**, not in `outline`: `enforce-output-budget`
(`mcp_inspect.clj:914`) runs over the whole result vector at `mcp_inspect.clj:1002` and `:1058`,
and `returned-source-character-count` `postwalk`s every result whatever mode produced it. So
`:args` is charged identically for `outline`, `forms`, `ls-deps`, `show_form`, `match` — any mode
that ever emits the key. Measured: `outline` of `intent_transaction.clj` charges 2,696 (`:args`
2,696, `:source` 0); `deps` charges 0 and returns no `:args`. The both-spelling set
(`:args` / `"args"`) is correct — `json-data` normalises keywords to strings downstream.

No false charge from the `:args` collision in `src/clj_surgeon/core.clj` (the CLI command table's
`:args` values are **maps**, and the walk only counts `(string? (val node))`).

The "arglists alone exceed the limit" case is genuinely pinned at the bound by
`mcp_study_test.clj:1590` and `:1616` (exactly-at-budget passes, one character more refuses with
`scope="request_source"`), and I reproduced the scope logic directly.

### (e) `paths_unresolved` — **CLOSED, bb parity holds**

Reported on **both** surfaces (witnessed above). Published in `inspect-output-schema`
(`mcp_inspect_tool.clj:905`), and the success receipt has **zero** keys outside the declared
properties. **bb parity holds** — the part I most expected to break, since `reify CharSequence`
under SCI is not guaranteed. Under `bb --classpath src:test`: `clj-surgeon.study` loads, the
counter counts (`mcp` = 19 steps), and the typed throw fires
(`:ns-grep-match-budget-exceeded`). `make test-fast` (which *is* `bb test/run_all.clj`) is green.

**Residual:** `"paths_unresolved" {:type "array"}` constrains only the container. The entry shape
`{:project :path :reason}` is published in prose and in STUDY-035, but not in the schema, so a
caller has no machine-readable contract for the fields. (`:additionalProperties true` and the
undeclared `error` / `error_type` / `source_unchanged` keys are pre-existing, not branch-introduced.)

### (f) The intent-contract fix at `ec5a592` — **CLOSED**

Every one of STUDY-030…035 now carries an `@spec` tag at an **implementation** site in `src/`:

```
030 -> study.clj, mcp_inspect_tool.clj        033 -> study.clj x3 (walk-clj-files,
031 -> study.clj x5                                   accumulate-projects, discover-projects)
032 -> mcp_inspect_tool.clj (ls-tree-bounded) 034 -> mcp_inspect.clj
                                              035 -> study.clj x2, mcp_inspect_tool.clj
```

and each has at least one witness in `test/`. `ec5a592` adds exactly the four missing tags its
message claims and nothing else (4 insertions, 2 files). The commit is worth naming: the
bidirectional contract oracle **caught the author** — 032 and 033 had tests and registry rows but
no implementation tag, so the promise could have been deleted by deleting its code with every
test still green — and the fix is recorded rather than quietly folded in. That is the ratchet
working as designed.

## Verdict for the mayor's merge queue: **GO-WITH-FIX**

Merge it. The round-3 blocker — a one-call, caller-supplied, hours-of-CPU denial of service in
the read entrance the whole fleet uses, with no cancellation path — is genuinely closed, by the
only mechanism that can close it, and I verified the mechanism rather than the claim. All five
other round-3 items are closed. All three gates reproduce the builder's figures exactly.
Reverting would restore the DoS, so **item 1 below is not a reason to hold the merge**; it is a
reason to schedule the next commit.

Items 1 and 2 should ride the same fix round: item 2 is a two-line string change and item 1 is a
one-expression change to how the per-file term is computed. Items 3–6 are documentation and
latent-invariant hygiene.

### Numbered fix list

1. **`src/clj_surgeon/study.clj:480`/`:497` (`ns-grep-match-steps-per-file`) and `:577` (pool sizing)** —
   a flat 20,000 per file is calibrated on 36-character paths, but `.*a.*b.*` costs O(len²).
   Scale the per-file term with subject length (`max(20000, 64 × len²)` clears every honest
   pattern measured by ~19× and still refuses `(.*.*.*)*x` at len 36 in well under a millisecond).
   *Witness that fails first:* `ns_grep=".*handler.*internal.*"` over 2,000 files with 111-character
   relative paths returns `ok=false error_type="ns-grep-match-budget-exceeded"`; the same work
   unbudgeted costs **149.6 ms**.
2. **`src/clj_surgeon/study.clj:1044`** — the remedy says *"without nested unbounded
   repetition"*, but the pattern that trips it in practice has none.
   *Witness:* `.*handler.*internal.*` is refused and told to remove a nesting it does not contain.
   Say what is true: the pattern's cost grows with path length; anchor it, or use a literal
   segment or alternation. Naming the exhausting subject in the refusal would make it actionable.
3. **`src/clj_surgeon/study.clj:495-496`** — the docstring claims 20,000 is *"roughly sixty times
   the worst honest cost measured here."*
   *Witness:* `(ns-grep-hit? #".*handler.*internal.*" <111-char path> pool)` charges **41,322** —
   2.07× over, not 60× under. The measurement was taken on one tree's path width.
4. **`src/clj_surgeon/study.clj:529-530` (`budgeted-subject`, `:509`)** — `subSequence` and `toString`
   delegate to the raw `String`, uncounted, which contradicts the docstring's *"exactly `budget`
   reads succeed; the next one throws."* Not reachable for matching today (proved: 15 constructs,
   lookbehind and backrefs included, read only through `charAt`), but the invariant should be
   total. *Witness:* wrap the return of `subSequence`, charge `length` in `toString`, and the
   existing grouped-pattern path (`ns_grep="(mcp)_(inspect)"` → `ok=true file_count=2`) must stay green.
5. **`src/clj_surgeon/study.clj:383-385`** — *"keeps the syscalls proportional to the cap instead
   of to the tree"* is true of `toRealPath` only. `find` still enumerates the whole tree and
   `:out :string` materialises all of it before the transducer.
   *Witness:* at `max_files 10` over the 10,000-file corpus, **1,130,000 bytes** of `find` stdout
   enter the heap and wall time tracks tree size (37 ms at 3,000 files, 69 ms at 10,000). Say
   `toRealPath`, or stream the subprocess output.
6. **`src/clj_surgeon/mcp_inspect_tool.clj:905`** — `"paths_unresolved" {:type "array"}` publishes
   the container but not the entry.
   *Witness:* the receipt sends `{:project "symproj" :path "srclink" :reason "symlink"}`; nothing
   in the schema tells a caller those three fields exist. Declare the item schema.

*Method note.* Fixtures built under `/home/forge/tmp/opus-study2-fx` only: a 3,000-file
single-project tree, a 10,000-file deep-path tree (70-char relatives), a 2,000-file tree with
111-character relatives, a two-project 100-file tree, and a symlinked-`:paths` project; plus the
pre-existing 10,000-file flat corpus at `/home/forge/tmp/corpus`. No git write of any kind, no
server started, no port in 7888–7895 or 7906 contacted. Suites via `/home/forge/bin/suite-run`;
probes under `java -Xmx1g`, one JVM at a time, on a box at load ~5.
