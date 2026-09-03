# parser-admission ad439f4 (MEM-005 round 2) — Opus executed re-check: GO-WITH-FIX, one NEW blocker introduced by round 2 (an extra `)` crashes the scan) — round 3 launched

# parser-admission ad439f4 (MEM-005) — Opus executed RE-review, round 2: GO-WITH-FIX, one NEW merge-blocker (round 2's own prefix stack crashes on an ordinary extra `)`)

**Reviewer:** Opus, executed. This is an **Opus-first branch**: OpenAI's content
filter refuses Sol's probes on this material (adversarial malformed/deep-input
fixtures), so both review rounds were run here. Every number below came from
running code in a scratch clone at `ad439f4`
(`/home/forge/tmp/opus-admit2`, fixtures in `/home/forge/tmp/opus-admit2-fx`);
nothing was committed, stashed, or pushed. Anvil, JVM 21, load average 1.8–7.8.
Round 1 is `origin/main:docs/observations/2026-09-03-mem-005-opus-review.md`
(GO-WITH-FIX, 8 items). Round 2 is `6665963..ad439f4`, 16 commits, red/green
per item.

---

## VERDICT: **GO-WITH-FIX** — one merge-blocker, and it is new

Round 2 does what it says. **Five of round 1's six findings are closed with
witnesses I re-ran and could not break**, and the two that were merge-blocking
are the two that are most convincingly closed: a 710-byte `@`-tower is now
refused `max_parse_depth` at observed 701 in ~0.5 ms with **zero** parse calls,
and an `ls-tree` over a directory whose overflowing file the estimator **cannot
see** now completes with one named, counted `stack-overflow-during-parse` skip
instead of dying. That second one is the important architectural move: the
scan-kill class is now closed *independently of estimator completeness*, which
is the only way it stays closed.

But the mechanism round 2 added to close finding 1 — a per-delimiter `int-array`
saving each level's pending prefix count — **crashes on a file with one extra
closing paren.** `ddepth` is allowed to go negative on an unmatched close and is
then used unclamped as the array index on the next open:

```
scan-shape "(defn f [x] (inc x)))\n(defn g [y] ...)"
  -> java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 64
```

The parent commit `6665963` handled the identical input cleanly
(`{:parse-nodes 5, :parse-depth 1, :delimiter-balance -1}`), and pre-branch
`outline/outline` on that file threw the reader's real, useful error:
`Unmatched delimiter: ) [at line 2, column 21]`. At `ad439f4` every read
entrance returns `Index -1 out of bounds for length 64` instead — and
`outline/outline`, `core/run-outline`, `core/run-deps` and
`analyze/file->zloc` throw it **unhandled**, because an
`ArrayIndexOutOfBoundsException` is neither an `ExceptionInfo` nor a refusal and
nothing in the new `named-plan-refusal` path catches it.

A syntax error is the single most common defect a structural editing tool meets.
This branch turns it into an internal array crash across the whole read path, and
**no gate saw it**: the corpus witness scans only well-formed sources, and both
suites are green. It is a one-line fix and it belongs in this merge, not after it.

Everything else I found is a follow-up. Nothing in round 2 writes a false green,
and I could not make the control produce a false refusal on any real source.

---

## Round-1 findings — status, re-run here

| # | round-1 finding | status |
|---|---|---|
| 1 | reader-macro prefixes add zero depth; a 716-byte tower StackOverflows the scan | **PARTIAL** — closed for its stated witness and family; `^` metadata still invisible |
| 2 | `safe-outline` catches `Exception`, not `Error` | **CLOSED** |
| 3 | gate `analyze` + the structural-lens 4th constructor | **CLOSED** |
| 4 | `show_form` flattens the typed refusal | **CLOSED** |
| 5 | `scan_ms` never reported in a production receipt | **CLOSED**, with a ruling on the rule (below) |
| 6 | `#!` shebang unhandled; corpus witness excluded `bench/` | **CLOSED** |
| 7 | registry row `[x]` / docstring coverage overreach | **PARTIAL** |
| 8 | nested-warm cell published as a point | **PARTIAL** — my run is outside the new range |

### 1 — PARTIAL. The family is counted; `^` is not, and one shape is halved

`src/clj_surgeon/parse_admission.clj:267-277` (`reader-prefix-start?` +
`prefix-length` + the pending/stack machinery) now counts prefixes as nesting.
Re-run, JVM, cold:

| tower | bytes | scan depth | refusal | observed | scan wall |
|---|---|---|---|---|---|
| `@` x600 | 610 | 601 | `:max-parse-depth` | 601 | 1.712 ms |
| **`@` x700** | **710** | **701** | **`:max-parse-depth`** | **701** | **0.637 ms** |
| `@` x2000 | 2,010 | 2001 | `:max-parse-depth` | 2001 | 1.103 ms |

End-to-end through `outline-source`, `z/of-string` redefined and counted:
`[:refused :max-parse-depth 701]`, **`z/of-string` calls = 0**, `scan_ms 0.468`.
Warm on-disk repeats: 0.983 / 0.731 / 0.512 ms wall, `scan_ms` 0.521 / 0.345 / 0.335.
Round 1's exact witness is dead.

**Corpus, re-run at `ad439f4` over `src/` + `test/` + `bench/`: 252 files, max
depth 22 (`src/clj_surgeon/intent_transaction.clj`), max nodes 33,740, non-zero
delimiter balance 0, refusals 0.** Both margins reproduce (6.8x on depth,
5.9x on nodes against the 200,000 ceiling). Prefix counting cost the corpus
nothing, exactly as round 1 predicted.

**What is still open.** `^` is the one prefix that consumes **two** forms — the
metadata *and* the value — and the scan lets the metadata satisfy it:

| source | bytes | scan depth | real reader depth |
|---|---|---|---|
| `^:a ^:b ^:c x` | 13 | **1** | 4 |
| `^String ^:private x` | 19 | **1** | 3 |
| `(def x ^:a  x50 y)` | 210 | **2** | 51 |
| `(def x ^:a  x700 y)` | **2,810** | **2** | 701 |
| `(def x ^:a  x2000 y)` | 8,010 | **2** | 2001 |

Scan depth is **2 for every N**. A 2,810-byte, two-line file is admitted and
`outline-source` throws `StackOverflowError` (measured at both 700 and 2000).
A second, milder undercount: a prefix immediately followed by a dispatch
construct loses the prefix's level, because the `#`-fallthrough at
`parse_admission.clj:274-276` zeroes `pending` before the `{` opens —
`'#{1 2}` scans 1 against a real depth of 3, and a `'#{` tower scans 200
against 401. That one is safe by margin (it still counts half, so the ceiling
still bites long before the ~460 cold SOE threshold); `^` is not, because it
is a constant.

This is **no longer merge-blocking**, and that is entirely because of finding 2:
`ls-tree` now survives the metadata tower as a named skip. It is still a real
gap on every single-file entrance (see item 4 in the fix list).

The witness that should have caught it is
`test/clj_surgeon/parser_admission_test.clj:218` — it builds `prefix-tower "^" 40`,
i.e. `(def x ^^^^…^^y)`, a run of bare carets with nothing between them. That is
the *one* caret shape the scan does count, and it is not valid Clojure. The real
field shape is `^:a ^:b ^:c`. Same scar as `test-with-the-callers-real-bytes`:
the witness passes on a shape the field does not produce.

### 2 — CLOSED. The scan-kill class is closed independently of the estimator

`src/clj_surgeon/core.clj:366` catches `StackOverflowError` inside `safe-outline`
and converts it to `admission/stack-overflow-refusal`
(`parse_admission.clj:361-390`). Re-run over a 4-file tree holding two ordinary
files, one estimator-**visible** 600-level `{:k [` file, and one estimator-**blind**
metadata tower — the scan **completes**:

```
src/deep_visible.clj  0 lines, 0 forms
  ⚠ parser admission refused max_parse_depth: … observed 1201, limit 150
src/meta_tower.clj  0 lines, 0 forms
  ⚠ parser admission refused stack_overflow_during_parse: …
src/ordinary_a.clj  2 lines, 1 forms      ← unchanged
src/ordinary_b.clj  2 lines, 1 forms      ← unchanged
── total: 4 files, 2 forms
── parser_admission_refused: 2 file(s)
   src/deep_visible.clj  max_parse_depth limit 150, observed 1201
   src/meta_tower.clj  stack_overflow_during_parse
── resources: scan_ms 22.596
```

The EDN receipt carries the same two rows with `:reason`, `:limit` (nil, honestly),
`:observed` (nil) and a `:remedy` that names the estimator miss as a defect and
asks for the file. That last touch is the right one — House Rule 17 wants a
refusal bucket somebody reads, and this one tells the reader what to do with it.

**`OutOfMemoryError` still propagates**, verified by redefining `outline/outline`
to throw one inside the `pmap`: `[:PROPAGATED "java.lang.OutOfMemoryError" …]`.
Deliberate, and correct — OOM belongs to the typed resource refusal, not here.

### 3 — CLOSED. Third and fourth constructors gated, with a named plan refusal

`analyze/file->zloc` (`analyze.clj:32`), `analyze/string->zloc` (`analyze.clj:39`),
`structural-lens/find-subforms` (`structural_lens.clj:619`) and `find-file`
(which routes through it) all now `admit!` before constructing. `core/named-plan-refusal`
(`core.clj:34`) wraps the CLI ops. Re-run on the estimator-visible deep file —
**six** ops, not five, since `run-outline` is wrapped too:

```
outline   -> NAMED REFUSAL {:refusal :parser_admission_refused :reason :max-parse-depth :limit 150 :observed 1201}
deps      -> NAMED REFUSAL   (same)
topo      -> NAMED REFUSAL   (same)
closure   -> NAMED REFUSAL   (same)
ls-deps   -> NAMED REFUSAL   (same)
declares  -> NAMED REFUSAL   (same)
analyze/string->zloc -> typed refusal :max-parse-depth observed 1201
analyze/file->zloc   -> typed refusal :max-parse-depth
lens/find-subforms   -> {:refusal … :reason :max-parse-depth :limit 150 :observed 1201 :error-type :parser-admission-refused}
lens/find-file       -> same, and it names the real path
```

I also checked the blast radius the new gate opened: `fix_declares.clj`,
`extract.clj` and `extract_header.clj` read through `analyze` and are **not**
wrapped in `named-plan-refusal`. `-main`'s global `ex-data` handler renders them
anyway — `clj-surgeon :op :fix-declares :file <deep>` prints the full typed
refusal with `:remedy`. No stack-trace regression there.

Residual: `mcp_inspect.clj:530` and `structural_lens.clj:814` call `find-subforms`
without `:file`, though both have the filename in hand, so those refusals name
`"<source>"`. House Rule 20 — a receipt must name its subject.

### 4 — CLOSED. `show_form.clj:435-446` carries the typed map

`(sf/show-file {:file <deep600> :form "x"})` returns
`{:refusal :parser_admission_refused, :reason :max-parse-depth, :limit 150,
:observed 1201, :error-type :parser-admission-refused}` where round 1 measured
`{:refusal nil :reason nil :limit nil}`.

### 5 — CLOSED, and the rule is wrong. My ruling, asked for explicitly

`core.clj:487` (text) and `core.clj:512` (EDN) emit `scan_ms`, but **only inside
the refusal block**. Verified both ways: an `ls-tree` over a clean directory
contains no `scan_ms`, no `resources`, and no `:receipt` at all; the refusing
scan carries `── resources: scan_ms 22.596` and `:resources {:scan_ms 7.859}`.

**Ruling: the placement is defensible and the rule is wrong. Not merge-blocking;
fix it in the follow-up.** Three reasons, in order of weight:

1. **The meter is dark in exactly the case it exists for.** The stated purpose
   is regression detection — "the first draft was 638x slower and every test
   passed." A 638x scan regression shows up on ORDINARY scans, which is ~100%
   of production runs and 0% of the runs that print the number. A gauge wired
   to the rare branch is a gauge nobody will ever see move.
2. **The counter is a single global atom** (`parse_admission.clj:302`) reset at
   the top of `outline-all-files` (`core.clj:400`). Two concurrent `ls-tree`
   scans — and the MCP server is multi-client — zero each other's clock and both
   publish a wrong figure. A receipt number that is silently wrong under
   concurrency is worse than an absent one.
3. **The figure has no denominator and its first reading is JIT-cold.** Same
   4-file, 12 KB tree: 22.596 ms cold, 7.859 ms warm, against 0.335 ms for one
   warm 710-byte file. Nobody can tell "slow" from "cold" without bytes or file
   count beside it.

The argument the other way is real and is why this is not blocking: keeping the
line inside the refusal block preserves byte-identical output for ordinary scans,
which `ls-tree-output-is-unchanged-when-nothing-is-refused` pins as a contract.
**Resolution I'd take:** leave the TEXT rendering conditional (human goldens
untouched), make the **EDN** receipt carry `:resources {:scan_ms _ :files _ :bytes _}`
unconditionally, and make the accumulator per-scan rather than a global atom.

### 6 — CLOSED. `#!` handled, corpus widened to `bench/`

`parse_admission.clj:213-220` treats `#!` as a line comment anywhere.
`#!/usr/bin/env foo ((((\n(def x 1)` → depth 1, **balance 0** (round 1 measured
scanD 5, balance 4). Corpus witness now includes `bench/`
(`parser_admission_test.clj:54-62`): **20 shebang files, all balance 0**,
252 files total, all admitted.

### 7 — PARTIAL. The row is honest about mechanism and still overreaches on `[x]`

`read-path-memory-specs.md:20` now reads "the clj-surgeon **read path**" and
spells out "nesting depth counts every construct that makes the reader recurse —
reader-macro prefixes as well as structural delimiters." Both corrections are
right. But it is checked `[x]` while the scan does not count `^`, and while
`rg` finds **63** direct `z/of-string` / `parse-string-all` sites across **26**
namespaces, of which four are gated. `outline.clj:283-288` is now scrupulously
honest about this ("Read that as a claim about THESE TWO ENTRANCES, not about
the product … `rg z/of-string` finds many more … and this intent does not reach
them") — the docstring is better than the registry row above it.

### 8 — PARTIAL. The range does not hold either; my run is outside it

The receipt now states nested-warm as `52.5–134.8 MB` over three runs. **My
`memory-red` run measured 149.0 MB**, above the top of the published range, at
load 7.8. Four measurements now: **52.5 / 107.3 / 134.8 / 149.0 MB**. Other
cells this run: 62.4 / 60.2 / 70.7 (receipt: 44.6 / 52.6 / 65.9). The receipt's
prose "the worst of them is 1.8x under the 247.8 MB budget" is stale — at 149.0
it is 1.66x.

**My reading:** the cell is measuring GC scheduling on a shared box, not the
control. The *gate* held — `PASS nested warm: typed refusal, well under budget
{:peak-mb 149.0}` — so nothing about the verdict moves. But a cell that has
drifted upward on every successive run and has now escaped its own published
range should stop being republished as a figure at all. State it as a **bound
with its margin** ("peak ≤ 149 MB observed over four runs, 1.66x under the
247.8 MB budget"), or pin the collector (`-XX:+UseSerialGC`, fixed `-Xmn`) so
the number is reproducible and can be tightened.

---

## Round 2's own additions — the hunt

### (a) The prefix-level `int-array` stack — **one crash, and it is the blocker**

`parse_admission.clj:278-294`. On `opens?` it does `(aset stack ddepth (int pending))`
after growing the array only when `ddepth >= (alength stack)`; on `closes?` it does
`(recur … (dec ddepth) …)` with no floor. So **any unmatched close followed by an
open** indexes the array negatively:

| input | `ad439f4` | parent `6665963` |
|---|---|---|
| `)(` | **AIOOBE: Index -1** | `{:parse-depth 0 :delimiter-balance 0}` |
| `)))(((` | **AIOOBE: Index -3** | (no crash) |
| `(a)) (b)` | **AIOOBE: Index -1** | `{:parse-depth 1 :delimiter-balance -1}` |
| `] [` | **AIOOBE: Index -1** | (no crash) |
| `} {a 1}` | **AIOOBE: Index -1** | (no crash) |
| `(def x 1)\n)\n(def y 2)\n` | **AIOOBE: Index -1** | `{:parse-depth 1 :delimiter-balance -1}` |

The `:delimiter-balance` docstring at `parse_admission.clj:188-191` promises
exactly the behaviour that no longer happens: *"a non-zero balance means
unbalanced source, which the PARSER reports as a syntax error. Admission does not
refuse it."* Admission now crashes instead of passing it through.

Blast radius, measured on a two-file project where `bad.clj` has one extra `)`:

```
ls-tree      -> completes, but bad.clj reads "⚠ Index -1 out of bounds for length 64"
outline/outline      -> *** AIOOBE (unhandled)          [was: Unmatched delimiter: ) at line 2, column 21]
core/run-outline     -> *** AIOOBE (unhandled)
core/run-deps        -> *** AIOOBE (unhandled)
analyze/file->zloc   -> *** AIOOBE (unhandled)
show-form/show-file  -> {:error "Index -1 out of bounds for length 64" :error-type :invalid-source}
lens/find-subforms   -> {:error "Index -1 out of bounds for length 64" :error-type :invalid-source}
```

`ls-tree` survives only because `AIOOBE` happens to be an `Exception` and
`safe-outline`'s last catch swallows it — a correct-looking scan that has
replaced the reader's line-and-column syntax error with an internal array
message. `named-plan-refusal` (`core.clj:34`) re-throws it untouched, as designed.

Everything else about the stack is sound: growth past 64 verified at depths
65 / 100 / 149 / 150 / 151 / 500 / 5000 (all exact, balance 0), and a `'(` x70
nested-quote tower saves and unwinds its pending correctly (scan 140).

**Fix:** clamp the index and keep the balance signed — `ddepth` is doing double
duty as an array index and as the reported balance, and only one of those may go
negative. Round 1's own `24-fixture lexical attack` never fed the scanner
malformed source, and neither does the corpus witness, so the ratchet is a witness
family for **unbalanced** input, not another well-formed shape.

### (b) Anti-accumulation across siblings — **verified, no accumulation**

`'(a)` and `'(a) '(b) '(c)` both scan depth 2, and it holds at 1 / 5 / 20 / 100
siblings (real tree depth 3 throughout). `(def a 'x 'y 'z)` = 2, `(a) (b)` = 1,
`'(a) (b)` = 2, `(def a '(x))` = 3. Prefixes unwind at the next atom **and** at
the closing delimiter, and `:delimiter-balance` stays 0 across
`'(a) `[b] ~@{c 1}`. The stack-save/unwind design is right; only its index is not.

Prefix runs at EOF (`'''`, `@@@@@`, `~@`) score 3 / 5 / 1 with balance 0 and no
crash — the parser errors on them, as it should, and admission does not pretend
otherwise. `#_` discarding a prefix run (`#_'''x` = 4, real 5), `^` meta on a
prefix (`^'a b` = 2, real 3), `#?@` splicing (3, real 4) and `#=` (2, real 3) all
track the real tree at the constant 1-level offset the estimator carries by
construction.

### (c) `rename.clj:67,166` left ungated — **acceptable to merge**

Verified: `clj-surgeon.rename` requires `rewrite-clj.zip` **directly**
(`rename.clj:7`) and calls `z/of-string` at 67 and 166; it does **not** read
through `analyze`. So the receipt's corrected §5 is now factually right where the
original text mis-attributed those two 298.8 / 306.9 MB peaks to the `analyze`
constructor.

**Ruling: merge it.** Three reasons. It is pre-existing behaviour this branch
neither introduces nor worsens. It is on the **edit/plan** path, not the read path
this intent scopes, and the branch's own registry row and docstring now say so in
the same breath. And `inb-07c5e7` already carries it with an owner, which is what
round 1 asked for — an open defect with a name, not a considered boundary.

One thing I would insist on, non-blocking: the receipt closes with *"belongs to
whichever lane owns the rename receipt."* That is not a named owner and a reader
cannot act on it. Put `inb-07c5e7` in the receipt text so the next person finds
the owner without asking.

### (d) The one unexplained full-suite failure at load 10 — **2/2 clean, diagnosis accepted as `:unverified`**

`make test-fast` run twice under `suite-run` at `ad439f4`: **745 tests / 6171
assertions / 0 failures / 0 errors**, both runs, identical. Nothing flaked.
With the builder's 4 full + 12 isolated runs, that is 6 full runs with no
reproduction.

The replaced assertion is `parser_admission_test.clj:267`:
`(is (< refusal-ns (* 20 one-scan-ns)))` in place of the old `(< ms 50.0)`.
**The shape is right** — a wall-clock constant on a 16-core box that routinely
passes load 10 measures the neighbours, and the ratio catches what actually
matters (a second scan or a parse creeping into the refusal path). Two residual
notes: the baseline is a **single un-repeated sample** guarded by `(max 1 …)`, so
one GC pause landing inside the refusal window can still trip a 20x band —
median-of-3, or a 50x band, would cost nothing; and the reported failure remains
**`:unverified`, not "fixed"**, because the failing test's *name* was lost to
`tail -4` and never recovered. The honest statement is "the only load-dependent
assertion in the file is gone by construction, and the failure has not recurred
in six full runs." The ratchet worth having is procedural: **suite output gets
kept whole, so a failing test name is never lost to a pipe again.**

### (e) The nested-warm range — my reading

Covered under round-1 finding 8: **149.0 MB, outside the published 52.5–134.8 MB
range.** Restate the cell as a bound with its margin, or pin the collector.

---

## Gates — run once each on this box, under `suite-run`

| gate | result | vs. builder's claim |
|---|---|---|
| `make test-fast` (run 1) | **745 tests / 6171 assertions / 0 fail / 0 err** | matches |
| `make test-fast` (run 2) | **745 / 6171 / 0 / 0** — identical, no flake | — |
| `make mcp-test` | **385 tests / 3971 assertions / 0 fail / 0 err** | matches |
| `make mcp-operation-oracle` | **pass**; legacy counterexamples `[verification_failed, verification_pending]` | matches |
| `make memory-battery-self-test` | **24 tests / 138 assertions / 0 fail / 0 err** | matches |
| `make memory-red PARSER_RED_EXPECT=green` | **6/6 assertions held** | matches |

`memory-red` was run **exactly once**, taking the exclusive
`flock /home/forge/tmp/suite.lock`. No full battery was run. Its four cells this
run: nested cold 62.4 MB / 23 ms, nested warm **149.0 MB** / 16 ms, giant 128m
60.2 MB / 78 ms, giant 512m 70.7 MB / 76 ms.

**One stale figure in the branch's own receipt:**
`docs/observations/2026-09-03-mem-005-parser-admission.md` §7 still reports
`make test-fast` at **737 tests / 6103 assertions**. Round 2 added five deftests;
the real number at `ad439f4` is **745 / 6171**. A gate table that does not match
the code it ships is the cheapest possible way to lose a reader's trust in the
rest of the receipt.

---

## Numbered fix list

1. **`src/clj_surgeon/parse_admission.clj:282` and `:293`** — clamp the stack
   index; `ddepth` cannot be both a signed balance and an array subscript. Floor
   the subscript at 0 (or carry the balance in a separate accumulator).
   *Witness:* `(pa/scan-shape "(defn f [x] (inc x)))\n(defn g [y] 1)")` throws
   `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 64` at
   `ad439f4` and returns `{:parse-depth 1 :delimiter-balance -1}` at `6665963`;
   `outline/outline` on that file returns `Unmatched delimiter: ) [at line 2,
   column 21]` before this branch and an unhandled AIOOBE after it.
   **MERGE-BLOCKING.**
2. **`test/clj_surgeon/parser_admission_test.clj:91`** (corpus witness) — add a
   witness family for **unbalanced** source: `)(`, `(a)) (b)`, `] [`, `} {a 1}`,
   and an unmatched close mid-file must each return a shape with a negative
   `:delimiter-balance` and must not throw. Every fixture in the existing
   24-shape lexical attack is well-formed; that is why finding 1 shipped.
   **MERGE-BLOCKING as the ratchet on 1.**
3. **`src/clj_surgeon/parse_admission.clj:154`** (`prefix-length`, the `CARET`
   arm) — `^` consumes **two** forms, the metadata and the value, so it must stay
   pending after the metadata form is read. *Witness:* `^:a ^:b ^:c x` scans
   depth **1** against a real reader depth of 4; a 2,810-byte two-line
   `(def x ^:a ×700 y)` scans depth **2**, is admitted, and `outline-source`
   throws `StackOverflowError`. Measured corpus cost of the fix: the longest
   metadata run in this repository is 1, so zero refusals — free, exactly like
   the round-1 prefix fix.
4. **`test/clj_surgeon/parser_admission_test.clj:218`** — the `^` case of
   `reader-macro-prefixes-count-as-nesting` builds `(def x ^^^^…^^y)`, a bare
   caret run that is not valid Clojure and is the one caret shape the scan does
   count. Replace it with the field shape `^:a ^:b ^:c …`. *Witness:* the test is
   green today while item 3's defect is live.
5. **`src/clj_surgeon/core.clj:366`** — the `StackOverflowError` catch exists only
   in `safe-outline`. *Witness:* on the metadata tower, `run-outline`, `run-deps`,
   `run-topo`, `run-ls-deps`, `run-declares`, `show-form/show-file` and
   `lens/find-subforms` all die with a raw `StackOverflowError`; only `ls-tree`
   survives. Give `named-plan-refusal` (`core.clj:34`) the same `Error` arm the
   scan has, so the class is closed on every entrance, not one.
6. **`src/clj_surgeon/core.clj:512`, `parse_admission.clj:302`** — make the EDN
   receipt carry `:resources` unconditionally with a denominator, and make the
   scan clock per-scan rather than one global atom. *Witness:* a clean `ls-tree`
   emits no `scan_ms` at all, so the 638x regression the counter exists to catch
   would be invisible on 100% of ordinary scans; and two concurrent `ls-tree`
   calls reset each other's clock, so the published figure is wrong under the
   server's normal concurrency. Keep the text rendering conditional.
7. **`src/clj_surgeon/mcp_inspect.clj:530` and `src/clj_surgeon/structural_lens.clj:814`**
   — pass `:file` to `find-subforms`; both callers have it. *Witness:* a refusal
   from the MCP `inspect_clojure` match surface reports `:file "<source>"`
   instead of the file it refused. House Rule 20.
8. **`docs/observations/2026-09-03-mem-005-parser-admission.md` §7** — the gate
   table says `test-fast` 737 / 6103; measured at `ad439f4` it is **745 / 6171**.
   Same section: name **`inb-07c5e7`** as the owner of the `rename.clj:67,166`
   gap instead of "whichever lane owns the rename receipt."
9. **`docs/observations/2026-09-03-mem-005-parser-admission.md` §3** — the
   nested-warm cell is published as 52.5–134.8 MB; my run measured **149.0 MB**
   at load 7.8, and the prose "1.8x under the 247.8 MB budget" is now 1.66x.
   Restate it as a bound with its margin over four runs, or pin the collector
   so the cell reproduces.
10. **`docs/intent/read-path-memory/read-path-memory-specs.md:20`** — the row is
    checked `[x]` and promises "nesting depth counts **every** construct that
    makes the reader recurse." *Witness:* `^` is a construct that makes the
    reader recurse and contributes a constant 1 for any N (item 3). Either land
    item 3 before ticking the box, or narrow the clause to the prefix families
    the scan actually counts.
