# parser-admission 52c5d85 (MEM-005 round 3) — Opus executed re-check: GO (both blockers closed; two pre-existing estimator-blind shapes named as follow-ups, inb-114faa)

# parser-admission `52c5d85` (MEM-005 round 3) — Opus executed RE-review: **GO**

**Reviewer:** Opus, executed. This is an **Opus-first branch** — OpenAI's content filter
refuses Sol's probes on this material (adversarial malformed / deep-input fixtures), so
all three review rounds were run here. A previous attempt at this round was killed by a
session limit before doing anything; this one started fresh.

Every number below came from running code in a scratch clone at `52c5d85`
(`/home/forge/tmp/opus-admit3`), with two comparison clones at `ad439f4`
(`/home/forge/tmp/opus-admit3-r2`) and at `origin/main` `73d856b`
(`/home/forge/tmp/opus-admit3-main`). Fixtures in `/home/forge/tmp/opus-admit3-fx`.
Nothing was committed, stashed, or pushed. Anvil, JVM 21.0.12, load 1.6–7.8.
Round 1: `origin/main:docs/observations/2026-09-03-mem-005-opus-review.md`.
Round 2: `origin/main:docs/observations/2026-09-03-mem-005-round2-rereview-opus.md`.
Round 3 under review: `32c6fa4..52c5d85` (4 commits after `32c6fa4`, 5 counting it).

---

## VERDICT: **GO** — merge it

Round 2's two merge-blockers are **closed, and I verified both directions myself**:
the new witness families are genuinely **red at `ad439f4` (15 failures + 4 errors)** and
green at `52c5d85`, and a differential of `outline` over the whole malformed family
against pre-branch `origin/main` is **identical on every file** where it differed on six
before the fix. The `^` undercount is closed with the field shape, at zero corpus cost —
I re-ran it against 792 third-party files from `~/.m2` (clojure.core, sci, malli, ring,
timbre, next.jdbc, rewrite-clj) and **eleven** files moved depth, all by +1 or +2, with a
corpus maximum of 27 against the ceiling of 150. The `scan_ms` ruling was accepted in
full and the fix is real: two concurrent tree scans now account for **exactly** their own
bytes, and the meter survives the `pmap` (2,890,068 bytes charged across 16 workers,
byte-exact against the sum on disk).

All six gates reproduce the builder's numbers exactly. I found **no new merge-blocker**.

The two out-of-scope leftovers I was asked to rule on **do not block** (items 6 and 7
below), and I say so having found a *live* witness for the first one that the branch does
not know about: a `#inst` tagged-literal tower and a `#^` metadata tower are both scored
at a **constant depth 1 for any N**, admitted, and StackOverflow the reader. Both predate
this branch, both are honestly outside the enumerated spec row this round rewrote, and
the tree-scale class stays closed regardless because `safe-outline` catches `Error`. That
is the architecture working. But it is now the strongest argument for the three-line
`Error` arm in `named-plan-refusal`, and it should be the first follow-up.

---

## Round-2 items — status, with my own re-run

| # | round-2 finding | status |
|---|---|---|
| 1 | extra `)` → AIOOBE, unhandled across the read path | **CLOSED** (was MERGE-BLOCKING) |
| 2 | no witness family for unbalanced input | **CLOSED** (was MERGE-BLOCKING) |
| 3 | `^` consumes two forms; scan blind, constant for any N | **CLOSED** |
| 4 | the `^` witness built a bare-caret run the field never produces | **CLOSED** |
| 5 | `StackOverflowError` caught only in `safe-outline` | **OPEN** — does not block (item 6) |
| 6 | `scan_ms` dark on ordinary scans, global atom, no denominator | **CLOSED**, one residual |
| 7 | registry row `[x]` overreach | **CLOSED** |
| 8 | `mcp_inspect:530` / `structural_lens:814` pass no `:file` | **OPEN** — does not block (item 7) |
| 9 | nested-warm published as a range my run escaped | **CLOSED** |
| 10 | specs row "counts EVERY construct that makes the reader recurse" | **CLOSED** |

### 1 + 2 — CLOSED. The scan never throws, and the reader's error comes back untouched

`parse_admission.clj:196-208` splits the signed balance `ddepth` from the floored nesting
depth `sdepth`; only `sdepth` indexes `stack`/`ostack` (`:315-327`) and only `sdepth`
feeds `max-depth`. Re-run, every shape round 2 crashed on:

| input | `52c5d85` | `ad439f4` |
|---|---|---|
| `)(` | `{:parse-depth 1 :delimiter-balance 0}` | AIOOBE Index -1 |
| `)))((( ` | `{:parse-depth 3 :delimiter-balance 0}` | AIOOBE Index -3 |
| `(a)) (b)` | `{:parse-depth 1 :delimiter-balance -1}` | AIOOBE Index -1 |
| `] [` | `{:parse-depth 1 :delimiter-balance 0}` | AIOOBE Index -1 |
| `} {a 1}` | `{:parse-depth 1 :delimiter-balance -1}` | AIOOBE Index -1 |
| `(def x 1)\n)\n(def y 2)\n` | `{:parse-depth 1 :delimiter-balance -1}` | AIOOBE Index -1 |

On the one-extra-`)` file, **every read entrance now returns the reader's own error**,
byte-identical to the pre-branch path:

```
outline/outline        Unmatched delimiter: ) [at line 1, column 21]
analyze/file->zloc     Unmatched delimiter: ) [at line 1, column 21]
analyze/string->zloc   Unmatched delimiter: ) [at line 1, column 21]
core/run-outline       Unmatched delimiter: ) [at line 1, column 21]
core/run-deps          Unmatched delimiter: ) [at line 1, column 21]
show-form/show-file    {:error "Unmatched delimiter: ) [at line 1, column 21]"
                        :error-type :invalid-source}
```

**The differential, reproduced independently.** I rebuilt the family the witness uses —
the 20 generated malformed shapes plus all 41 checked-in fixtures — wrote each to a file,
and ran `outline/outline` over all of them in three checkouts:

| checkout | result |
|---|---|
| `origin/main` `73d856b` (pre-branch, no `parse_admission.clj`) | baseline |
| `ad439f4` (round 2) | **6 of 61 differ** — five unmatched closes + `mixed-garbage`, each an `Index -1`/`Index -3` where main returned `Unmatched delimiter` |
| **`52c5d85`** | **61 of 61 identical to main** |

The receipt calls this family **62** files (`§5`, round-3 section). It is **61**: 20
generated shapes + 41 fixtures. The finding is unaffected; the arithmetic is wrong and
should be corrected. The receipt also names the differential baseline as `6c07015`;
`origin/main` is `73d856b` as of this run and I used that.

The ratchet is real and I proved it bites: with `52c5d85`'s
`test/clj_surgeon/parser_admission_test.clj` copied into the `ad439f4` clone,
`malformed-source-never-crashes-the-scan` (`:161`) is **2 errors + 2 failures**, naming
all six crashing shapes in its own message.

### 3 + 4 — CLOSED. `^` is two forms, and it costs the corpus nothing

`parse_admission.clj:305` — the prefix run now carries `owed`, and `^` owes 2.

| source | bytes | scan depth at `ad439f4` | scan depth at `52c5d85` |
|---|---|---|---|
| `^:a ^:b ^:c x` | 13 | 1 | **3** |
| `^String ^:private x` | 19 | 1 | **2** |
| `^:a x` | 5 | 1 | **1** (unwound by the value, not the metadata) |
| `(def x ^:a ×700 y)` | **2,810** | 2 | **701** |

The 2,810-byte two-line tower is refused `:max-parse-depth` **limit 150, observed 701,
with `z/of-string` calls = 0**, in 2.296 ms cold.

**No false refusals, on two corpora.** This repository: 252 files under `src/` + `test/`
+ `bench/`, **max depth 22** (`src/clj_surgeon/intent_transaction.clj`), max nodes 33,740,
non-zero delimiter balance **0**, refusals **0** — every figure identical to round 2, so
counting `^` honestly cost this repo nothing. Independently, **792 third-party files**
extracted from `~/.m2` jars: **0 threw, 0 refused, max depth 27**
(`sci/impl/analyzer.cljc`), a 5.6x margin. Comparing depths file-by-file against
`ad439f4`, only **11 of 792** moved at all: `malli/core.cljc` 21→23,
`clojure/pprint/cl_format.clj` 16→17, and nine more at +1.

The bare-caret witness is gone: `^` is removed from the one-form prefix family at
`parser_admission_test.clj:332` and given its own field-shape assertions at `:341-348` (deftest at `:322`), which
I verified fail at `ad439f4` (`(not (= 3 1))`, `(not (= 2 1))`, `(not (< 2 2))`).

### 6 — CLOSED, with one residual on the numerator

`parse_admission.clj:357-399` replaces the global atom with a per-scan meter;
`core.clj:399-420` makes it in `outline-all-files`, closes over it lexically, and rebinds
it inside each `pmap` worker; `core.clj:528-539` makes `:resources` unconditional in EDN.
My runs:

- **Clean scan, EDN:** `{:receipt {:resources {:scan_ms 3.175, :bytes_scanned 108}}}` —
  108 is exactly the bytes on disk. **Text unchanged**: no `resources` line, no
  `refused` line.
- **Two concurrent scans:** small tree expected 72 bytes, got `{:scan_ms 1.044,
  :bytes_scanned 72}`; big tree expected 1,470, got `{:scan_ms 11.09, :bytes_scanned
  1470}`. **Exact, both.**
- **Meter across `pmap` workers:** scanning this repo's own tree, `:bytes_scanned
  2890068`, byte-exact against the sum of the source files on disk. Binding conveyance
  is not being relied on and it works.

`concurrent-scans-charge-their-own-meters` (`:674`) is red at `ad439f4`
(`(not (= 82 nil))`), so this one is a real ratchet too.

**Residual (follow-up, item 8).** `scan_ms` is now a **sum of per-worker durations**, and
nothing in the receipt says so. On this repo's tree:

| run | wall of the whole `ls-tree` | published `scan_ms` | `bytes_scanned` |
|---|---|---|---|
| cold | 1,047.6 ms | **7,320.871** | 2,890,068 |
| warm | 417.7 ms | 63.081 | 2,890,068 |
| warm | 578.0 ms | 26.333 | 2,890,068 |

Honest single-threaded cost of the same class of bytes, measured three times:
**10.3 / 10.4 / 10.9 ms per 1.39 MB**, i.e. ~21 ms for 2.89 MB. So the cold receipt
reports a scan cost **7x the entire operation's wall clock** and **~350x the honest
serial cost**, and the cold/warm spread of the published number is 278x. `bytes_scanned`
is exact and stable across all three; `scan_ms` is not yet a figure a reader can act on.
The denominator landed, which is what round 2 asked for; the numerator now needs a unit
note ("aggregate scan time across workers") or a rate (`ns_per_kb`).

### 7, 9, 10 — CLOSED

- **7 / 10.** `read-path-memory-specs.md:20` no longer says "every construct that makes
  the reader recurse." It **enumerates** the eleven prefix families the scan counts,
  names `^` as the two-form prefix it is, and adds "shall never itself throw on malformed
  source, which belongs to the parser." That is exactly the claim the code makes.
  `outline.clj:284-288` still spells out the boundary ("THESE TWO ENTRANCES, not the
  product"). Re-counted: **63** direct `z/of-string`/`parse-string-all` sites across
  **26** namespaces, **5** gated (`outline.clj:291,334`, `analyze.clj:34,42`,
  `structural_lens.clj:622`). The `[x]` tick is now defensible.
- **9.** The cell is restated as a **bound with its margin** — "peak ≤ 160 MB observed
  across five runs" — rather than a range. My `memory-red` run this round, at load 7.4,
  measured **nested warm 86.0 MB**, comfortably inside it. Six measurements now:
  52.5 / 107.3 / 134.8 / 149.0 / 100.9 / **86.0**. The bound held; the restatement was
  the right move.

---

## Round 3's own additions — the hunt

### (a) `ddepth` / `sdepth` — a leading run of `)` cannot buy depth

This was the obvious way to break the split, and it does not work. `sdepth` floors at 0
on every close (`parse_admission.clj:341`) and is the only value that indexes a stack or
reaches `max-depth`; `ddepth` is decremented freely and only reported.

| input | scan depth | balance |
|---|---|---|
| 200 `)` then 200 `(` | **200** — exactly the opens, no credit | 0 |
| 200 `)` then 200 `(` then 200 `)` | 200 | -200 |
| 5 `)` then 200 `(` | **200** | 195 |

There is no "admitted at 196": the ceiling is fed by `sdepth`, and `sdepth` never sees the
leading closes. The stack growth past 64 was already verified in round 2 at 65 / 100 /
149 / 150 / 151 / 500 / 5000 and is untouched by this change.

**One honest note, unchanged from before the branch:** `)(`, `] [` and `)))((( ` all
report `:delimiter-balance 0` for genuinely unbalanced source, because the counter counts
delimiters and these cancel. The docstring (`:188-191`) claims only the forward direction
("a non-zero balance means unbalanced source") and never the converse, so it is not
overclaiming — but a reader should not treat `balance 0` as "this file parses."

### (b) `pending` / `owed` for `^` — no crash, no leak, no overcount

`^` then `)` → `(^:a)` depth 2. `^` at EOF → depth 1. `^:a` at EOF → depth 1.
`#_^:a x` → depth 2. `^{:a 1} x` → 2. `^:a (foo) bar` → 2. `^:a ^:b (f) g` → 3.
`(a ^:x ^:y b (c ^:z d))` → 3. `(defn ^:private f [x] x)` → 2. None throws; the run
unwinds at the closing delimiter as well as at the atom, and the `ostack` restores the
interrupted `owed` correctly. Two corpora (252 + 792 files) show no leak across a file.

### (c) The per-scan meter in the `pmap` workers

Verified above: byte-exact across workers and across two concurrent scans. Sound.

**One design residual worth a follow-up (item 9).** The meter is carried on the
**metadata of the projects vector** (`core.clj:420`), and `scan-resources`
(`core.clj:422-428`) falls back to `{:scan_ms 0.0 :bytes_scanned 0}` when it is missing.
Production is safe — `run-ls-tree` (`core.clj:615-618`) hands the vector straight to the
formatters and is the only production consumer. But a future caller that `mapv`s over the
vector loses the meta and publishes a **false zero**, not an absent field; and
`ls_tree_test.clj:245-252` now pins that false-zero shape as the correct answer. A
receipt that says `0` where it means "I was not measured" is the exact shape House Rule 20
warns about. `:unmetered` (or an absent `:resources`) costs nothing and cannot be
misread.

### (d) The install-test HEAD-stamp race — **plausible, and the mechanism is confirmed**

I read the code rather than taking the claim. It holds:

- `test/clj_surgeon/install_test.clj:52-58` — `source-commit` is a **`def`**, resolving
  `git rev-parse HEAD` **once at namespace load**, and `stable-copy-stamp` is built from
  it.
- `Makefile:16` — `SOURCE_COMMIT := $(shell git -C ... rev-parse HEAD)`, re-resolved on
  **every** `make` invocation, and stamped into the package at `Makefile:541`.
- `install_test.clj:321` — asserts
  `(= (str (slurp canonical-skill-path) stable-copy-stamp) (slurp installed-SKILL.md))`.

So any commit — including a `git commit --amend` — landing between the JVM loading that
namespace and the `make install` subprocess makes the two stamps disagree by exactly the
line the builder quoted. **The mechanism is real and this test is a genuine flake source
for a merge queue that commits while a suite runs.**

Two corrections to the receipt's prose, neither material: it says "the test installs
twice and compares the two files" — it actually compares the canonical source plus the
*load-time* stamp against the *installed* file, which is why the two shas in the quoted
failure differ. And the round-2 failure it is a candidate for occurred on the **builder's**
box while the builder was committing, which is consistent; my own round-2 runs were in a
scratch clone where nothing committed, so this mechanism could not have fired there. The
receipt is right to label it a **candidate, not an attribution** — round 2's failing test
name is unrecoverable and nobody can say it was this test.

---

## The two out-of-scope leftovers — my ruling

### Leftover 1: single-file entrances still die on an estimator-blind `StackOverflowError`

**Confirmed OPEN, with a live witness the branch does not know about — and it does NOT
block the merge.**

`core.clj:366` catches `StackOverflowError` inside `safe-outline` only.
`named-plan-refusal` (`core.clj:34-52`) catches `clojure.lang.ExceptionInfo` and nothing
else. Round 3 closed the `^` family, so round 2's witness is gone — but the class is not,
and I found two more shapes that are still scored at a **constant depth 1 for any N**:

| shape | bytes | scan depth | admitted? | rewrite-clj |
|---|---|---|---|---|
| `(def x #inst ×700 "2020")` | 4,214 | **1** | yes, refusal `nil` | **StackOverflowError** |
| `(def x #^:a ×700 y)` | 3,509 | **1** | yes, refusal `nil` | **StackOverflowError** |

Both fall through `prefix-length`'s `HASH … :else 0` arm (`parse_admission.clj:143-150`)
into the token branch. On the `#^` tower:

```
ls-tree            completes — "⚠ parser admission refused stack_overflow_during_parse"
outline/outline    *** StackOverflowError (unhandled)
analyze/file->zloc *** StackOverflowError (unhandled)
core/run-outline   *** StackOverflowError (unhandled)
show-form/show-file*** StackOverflowError (unhandled)
```

**Why it still does not block.** Both shapes score depth 1 at `ad439f4` too, and at
pre-branch `main` there is no scan at all and the same entrances die on the same input —
so this is **not a regression, and the branch strictly narrows the blind set** (it removed
`^`, the one blind family that appears in ordinary Clojure). The intent's own row now
**enumerates** the prefix families it counts rather than claiming completeness, so the
spec is satisfied as written, and the falsifier row demands the `StackOverflowError` skip
at the **tree-scale** scan, which is exactly what `safe-outline` delivers. Blocking a
merge on a pre-existing gap that the merge shrinks would be the wrong trade.

**But it is the first follow-up, and the priority went up this round.** The moment the
spec row stopped claiming estimator completeness, the `Error` catch became the *only*
thing closing this class — and it exists at one entrance out of six. It is a three-line
`(catch StackOverflowError _ …)` arm on `named-plan-refusal`.

### Leftover 2: `mcp_inspect.clj:530` / `structural_lens.clj:814` pass no `:file`

**Confirmed OPEN. Does NOT block.** Both callers have the filename in hand
(`mcp_inspect.clj:519` reads `(:file request)`, eleven lines above the call;
`structural_lens.clj:810` destructures `file` in the same `let`). Measured:

```
find-subforms without :file  {:refusal :parser_admission_refused
                              :reason :max-parse-depth
                              :file "<source>"
                              :error "parser admission refused max_parse_depth:
                                      <source> observed 800, limit 150"}
find-subforms with :file     {:file "/tmp/real.clj"}
```

House Rule 20 hygiene on a refusal receipt: it names the reason and the numbers correctly
and misnames only its subject. No false green, no wrong control decision, two one-word
edits. Follow-up, not a gate.

---

## Gates — each run once, on this box, under `~/bin/suite-run`

| gate | my result | builder's claim |
|---|---|---|
| `make test-fast` | **748 tests / 6196 assertions / 0 fail / 0 err** | 748 / 6196 / 0 — **matches** |
| `clojure -M:clj-surgeon/mcp-test` | **385 / 3971 / 0 / 0** | 385 / 3971 / 0 — **matches** |
| `make mcp-operation-oracle` | **pass**; legacy counterexamples `[verification_failed, verification_pending]` | **matches** |
| `make memory-battery-self-test` | **24 / 138 / 0 / 0** | **matches** |
| `make memory-red PARSER_RED_EXPECT=green` | **6/6 held**; cells 45.5 / **86.0** / 47.7 / 72.9 MB at load 7.4 | 6/6; 65.4 / 100.9 / 52.4 / 71.5 — **matches, inside the ≤160 MB bound** |

`memory-red` ran **exactly once**, taking the exclusive `flock /home/forge/tmp/suite.lock`.
**No full battery was run.** No port in 7888–7906 was contacted; `make mcp-test` was never
invoked (the alias was run directly, and the oracle separately).

**Red/green, independently re-derived.** `52c5d85`'s `parser_admission_test.clj` run
against the `ad439f4` source tree: **22 tests, 145 assertions, 15 failures, 4 errors** —
every one of them in the four deftests round 3 added or amended
(`malformed-source-never-crashes-the-scan`, the `^` arm of
`reader-macro-prefixes-count-as-nesting`,
`a-metadata-tower-is-refused-before-it-overflows-the-reader`,
`concurrent-scans-charge-their-own-meters`,
`the-scan-charges-itself-in-the-production-receipt`,
`ls-tree-output-is-unchanged-when-nothing-is-refused`). Green at `52c5d85`. These are
ratchets, not decoration.

---

## Numbered follow-up list — none of these block the merge

1. **`src/clj_surgeon/core.clj:34`** (`named-plan-refusal`) — add the
   `(catch StackOverflowError _ …)` arm `safe-outline` has at `core.clj:366`, so the
   scan-kill class is closed at every entrance rather than one.
   *Witness:* a 3,509-byte `(def x #^:a ×700 y)` scans at **depth 1**, is admitted
   (`refusal` → `nil`), `ls-tree` names it `stack_overflow_during_parse`, and
   `outline/outline`, `analyze/file->zloc`, `core/run-outline` and `show-form/show-file`
   all die with a raw `StackOverflowError`.
2. **`src/clj_surgeon/parse_admission.clj:143-150`** (`prefix-length`, the `HASH` arm) —
   `#^` (old-style metadata) and tagged literals (`#inst`, `#uuid`, any `#tag`) are the
   two remaining prefix families scored at a constant, and tagged literals are far more
   common in the field than a metadata tower.
   *Witness:* `(def x #inst ×700 "2020")`, 4,214 bytes, scan depth **1** for any N,
   admitted, `z/of-string` → `StackOverflowError`. Same family as the `^` defect this
   round fixed. Corpus cost of counting them: the longest such run in this repository and
   in 792 third-party files is 1, so zero refusals — free, exactly like `^`.
3. **`src/clj_surgeon/mcp_inspect.clj:530`** — pass `:file (:file request)` to
   `find-subforms`; it is read at `:519` and again at `:524`.
   *Witness:* the refusal from the MCP `inspect_clojure` match surface reports
   `:file "<source>"` and an `:error` string reading
   `parser admission refused max_parse_depth: <source> observed 800, limit 150`.
4. **`src/clj_surgeon/structural_lens.clj:814`** — same, in `plan-replacement`; `file` is
   destructured in the same `let` at `:810` and passed to `build-replacement-plan` five
   lines later, but not to `find-subforms`.
   *Witness:* identical `<source>` receipt. House Rule 20.
5. **`docs/observations/2026-09-03-mem-005-parser-admission.md` §5 (round-3 section)** —
   the malformed differential family is **61** files (20 generated + 41 fixtures), not 62;
   my reproduction is **6/61 differing at `ad439f4`, 61/61 identical at `52c5d85`**. Same
   paragraph names the baseline as `6c07015`; `origin/main` is `73d856b`.
   *Witness:* `malformed-shapes` at `parser_admission_test.clj:116-151` has 20 entries;
   `find test-fixtures -type f` returns 41.
6. **`src/clj_surgeon/core.clj:512`, `parse_admission.clj:391`** — `scan_ms` is a **sum of
   per-worker durations** and nothing says so. Add the unit to the key or publish a rate.
   *Witness:* on this repo's own tree, a cold `ls-tree` publishes `scan_ms 7320.871`
   against a **1,047.6 ms** wall for the whole operation and an honest serial scan cost of
   ~21 ms for the same 2,890,068 bytes; warm runs of the identical scan publish 63.081
   and 26.333. `bytes_scanned` is exact (2,890,068) in all three.
7. **`src/clj_surgeon/core.clj:422-428`** (`scan-resources`) — an absent meter publishes
   `{:scan_ms 0.0 :bytes_scanned 0}`, a **false zero** rather than an absent field, and
   `ls_tree_test.clj:245-252` now pins that shape as correct. Emit `:unmetered` (or omit
   `:resources`) so an unmeasured scan cannot be read as a fast one. Production is safe
   today: `run-ls-tree` (`core.clj:615-618`) is the only production consumer and it
   preserves the metadata.
8. **`test/clj_surgeon/install_test.clj:52`** — `source-commit` is resolved once at
   namespace load while `Makefile:16` re-resolves `SOURCE_COMMIT` per invocation, so a
   commit landing mid-suite fails `stable-install-isolates-cli-and-both-agent-skills` at
   `install_test.clj:321`. Not MEM-005's to land, but it is a live merge-queue flake:
   drop the sha from the compared bytes, or resolve HEAD once per assertion.
   *Witness:* the two stamps in the builder's quoted failure differ by exactly the
   `Stable copy installed from commit <sha>` line, and that line is written from
   `$(SOURCE_COMMIT)` at `Makefile:541`.
9. **Procedural, carried forward from round 2 and still owed:** keep suite output whole,
   so a failing test name is never lost to a `tail -4` again. The one unexplained round-2
   failure remains `:unverified` for exactly that reason, and item 8 is a candidate cause,
   not an identification.
