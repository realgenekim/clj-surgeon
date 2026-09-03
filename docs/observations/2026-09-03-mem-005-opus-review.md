# parser-admission 8a55dbc (MEM-005) — Opus executed review (Sol filter fallback): GO-WITH-FIX, two merge-blockers (reader-macro prefixes add zero depth; worker catches Exception not Error) — round 2 launched

# MCP-OP-MEM-005 — independent executed review, `bridge/parser-admission` @ `8a55dbc`

**Reviewer:** Opus (fallback). OpenAI's content filter refused this review on the
adversarial-input probes, so the adversarial arm was executed here instead. Every
number below was produced by running code in a scratch clone
(`/home/forge/tmp/opus-admit`, fixtures in `/home/forge/tmp/opus-admit-fx`);
nothing was committed, stashed, or pushed. Anvil, JVM 21, load average 6–9.

---

## VERDICT: **GO-WITH-FIX** — findings 1 and 2 merge-blocking, 3–6 follow-up

The control does exactly what its EARS row promises and is a large, measured,
regression-free improvement: the four adversarial cells that peaked 264–386 MB or
died now refuse in 5–69 ms at JVM-baseline heap, with **zero false refusals on 165
real sources** and every gate green. Its lexical handling of strings, regex, char
literals and comments is genuinely correct — I attacked it 24 ways and could not
make it miscount one.

But the ceiling it enforces is a **delimiter** ceiling, not a nesting ceiling, and
the branch's registry row is checked `[x] implemented` against a promise the code
does not keep. **A 716-byte file is admitted with `parse_depth 0` and then kills the
entire `ls-tree` scan with a StackOverflowError** — the exact catastrophe MEM-005
was built to eliminate, at 1/155th the size of the 111 KB file that motivated it.
Merging it as-is writes a false green: a later lane that reads "MEM-005 bounds
nesting depth" will be wrong, and the `safe-outline` scan-kill will look closed
when it is open. Findings 1 and 2 are each a few lines and I have measured that
neither costs the corpus anything, so they belong in this merge rather than after it.

---

## Findings

### 1. `scan-shape` is blind to every reader-macro prefix; a 716-byte file still kills the whole `ls-tree` scan — **P0, merge-blocking**

`src/clj_surgeon/parse_admission.clj:183-195` — only `( [ {` increment `depth`
(the `opens?` branch, line 183). Every reader macro that makes the rewrite-clj
reader **recurse one more frame** — `'` `` ` `` `~` `~@` `@` `^` `#'` `#_` `#=` `#?` —
falls through to the `:else` token branch (line 190) and contributes **zero depth**.

Witness — `(def x @@@…y)`, ungated vs. gated, cold JVM, default `-Xss1m`:

| shape | bytes | `scan-depth` | admission | `outline-source` |
|---|---|---|---|---|
| `@` x600 | 616 | **0** | ADMITTED | completed |
| `@` x700 | **716** | **0** | **ADMITTED** | **StackOverflowError** |
| `'` x2000 | 2,001 | 0 | ADMITTED | StackOverflowError |
| `` ` `` x2000 | 2,001 | 0 | ADMITTED | StackOverflowError |
| `#'` x2000 | 4,001 | 0 | ADMITTED | StackOverflowError |
| `^:a ` x2000 | 8,001 | 0 | ADMITTED | StackOverflowError |

And the end-to-end consequence, `run-ls-tree` over a directory holding two ordinary
files and one 3,025-byte `@`-tower — the precise scenario section 1 of the receipt
says is now fixed:

```
{:FATAL "ExecutionException", :msg "java.lang.StackOverflowError"}
```

The scan dies. No file's outline is returned. `core/safe-outline`
(`src/clj_surgeon/core.clj:314-336`) still catches only `ExceptionInfo` and
`Exception`; its own docstring names `Error` as the thing that used to kill the
scan, and `Error` still does.

Divergence against the real tree (`rewrite-clj.parser/parse-string-all`, node walk):

| source | scan depth | real tree depth |
|---|---|---|
| `'` x200 then `x` | **0** | **201** |
| `@` x200 then `x` | **0** | **201** |
| `^:a ` x200 then `x` | **0** | **201** |
| `#_` x200 then `x` | **0** | (reader error) |
| `#?@(:clj [1 2] :cljs [3])` | 2 | 4 |
| `#=(+ 1 2)` | 1 | 3 |

**Fix, measured safe.** Count a reader-macro prefix as one nesting level. I
simulated exactly that over **252** real sources (`src/`, `test/`, **and** `bench/`):
the longest consecutive prefix run in the whole repository is **2**, the worst file
moves from depth 22 to 23, the margin under the 150 ceiling stays **6.5x**, and
**0 files would be refused**. Conservative and free.

**Second, independent fix — take it regardless.** Add `(catch StackOverflowError e …)`
to `safe-outline` at `src/clj_surgeon/core.clj:335`. One line, and it converts
*any* residual reader recursion — this family, or the next one nobody has thought of —
from a dead scan into a named per-file skip. The estimator will always be an
estimate; the scan should not depend on it being complete. (Note it must be caught
inside the `pmap` worker at `core.clj:361`, since the parallel path re-wraps it as
`ExecutionException`.)

### 2. The registry row and three docstrings claim coverage the code does not have — **merge-blocking (documentation, but load-bearing)**

- `docs/intent/read-path-memory/read-path-memory-specs.md:20` — *"**clj-surgeon** shall reject inputs whose … nesting depth exceeds the ceiling"*, marked `[x]`. The subject is the whole product; the implementation is two entries in one namespace. Scope it to the outline read path, and do not mark it implemented while finding 1 stands.
- `src/clj_surgeon/outline.clj:278-282` — *"the read path's **two** tree constructors … outline, ls_tree, show_form, compact locations, **the extract read path**, the change buffer's owner scan — inherits the ceiling."* Measured false. `rg` finds ~60 direct `z/of-string` / `parse-string-all` sites across 25 namespaces. On a 4,222-byte 600-level `{:k [` file (scan depth 1,201, which admission *can* see):

| entrance | result |
|---|---|
| `outline/outline-source` | REFUSED (typed) |
| `show-form/show-file` | refused, but **flattened to a string** (see finding 4) |
| `analyze/string->zloc` (`analyze.clj:31`) | **StackOverflowError** |
| `structural-lens/find-subforms` (`structural_lens.clj:612`) | **StackOverflowError** |
| `structural-lens/find-file` (`structural_lens.clj:646`) | **StackOverflowError** |
| `extract/plan` (`extract.clj:37,168,283`) | **OutOfMemoryError** |

- `src/clj_surgeon/parse_admission.clj:6-10` — the namespace docstring states the scan
  "measures its maximum nesting depth." It measures maximum *delimiter* depth. Say so.

### 3. Ruling on the ungated third constructor (`clj-surgeon.analyze`) — **acceptable to merge, but the stated reason is wrong, and there is a fourth**

The builder's rule ("a refusal is only safe where the caller turns it into a named,
counted skip; gating `analyze` today would convert an operation that *completes* into
one that throws") is sound doctrine and I would apply it too. **Its factual premise is
false.** `analyze/string->zloc` does not complete on the shape in question — it throws
`StackOverflowError`, an uncatchable-by-convention `Error` that no caller handles.
Gating it would swap an `Error` for a typed `ExceptionInfo`, which is strictly better
for every caller even with no skip surface, and the ~300 MB peak the note concedes
would go away.

It is nonetheless **acceptable for this merge**: it is pre-existing behaviour, not a
regression this branch introduces, and `rename` has no receipt to carry the skip.
But it must be recorded as an **open defect with a named owner**, not as a considered
boundary — and `clj-surgeon.structural-lens` is a **fourth** ungated constructor the
note does not mention, reached from the MCP read surface at
`src/clj_surgeon/mcp_inspect.clj:530` and the CLI `:find-subform` op at
`src/clj_surgeon/core.clj:737`. A read-path memory bound that misses the read path's
own `find_subforms` verb is a gap in *this* leaf, not a neighbouring lane's.

### 4. `show_form` loses the typed refusal — the spec's own witness family is not met on that entrance

`src/clj_surgeon/show_form.clj:444-452` catches `Exception` and flattens the refusal
to `{:error "parser admission refused max_parse_depth: …"}`. Measured:
`{:refusal nil, :reason nil, :limit nil}`. The falsifier table requires "a refusal
carrying `:reason`, `:limit`, `:observed`, and `:remedy`," and `safe-outline` was
specifically taught to keep those (`core.clj:326-329`). `show_form` — named in the
coverage claim at `outline.clj:280` — was not. Same three-line treatment.

Likewise `core/run-outline` (`core.clj:34`) lets the typed `ExceptionInfo` escape to
the CLI rather than rendering a receipt. Defensible for a single-file op, but it means
`parser_admission_refused` is a **named, counted skip on exactly one entrance**
(`ls-tree`), not on the four the docstring lists. Answering the review question
directly: the receipt surface is `ls-tree` text and EDN only.

### 5. The scan's cost is never charged or reported in a production receipt

`rg scan-ms` hits **only** `bench/parser_admission/red_witness.clj:126,147,162` and
`bench/parser_admission/shape_probe.clj:81,95`. No `src/` path emits it. I measured
the charge on `intent_transaction.clj` (126,596 B): **scan 0.647 ms vs outline
51.032 ms = 1.27%**, exactly one `scan-shape` call per parse entry (verified by
redefining the var) — so the cost is genuinely small and there is no double-scan.
But an unreported cost is one nobody will notice regressing; the first draft of this
very function was **638x** slower and every test passed. Put `scan_ms` in the
`ls-tree` receipt beside the refusal count.

### 6. `#!` shebang is unhandled — the balance-zero invariant is breakable, and the corpus witness looks in the wrong place

`parse_admission.clj:141-147` handles `;` but not `#!`, which Clojure's reader treats
as a line comment. `#!/usr/bin/env foo ((((\n(def x 1)` scores **scanD=5, balance=4,
realD=2** — the only overcount I found in 24 lexical attacks, and the only violation of
the invariant the spec calls "proof it respects strings, regex literals, character
literals, and comments."

Impact today is nil and I checked rather than assumed: the repository has **20 shebang
files, all under `bench/`, all balance 0**. But `default-ceilings-admit-every-source-in-this-repository`
(`test/clj_surgeon/parser_admission_test.clj:184`) and the receipt's "163 sources" both
scan only `src/` and `test/` — the corpus witness excludes the only directory that
contains the construct it cannot parse. Widen the corpus to `bench/`; add the two lines
for `#!`.

**Everything else in the lexical attack was correct** — 24 fixtures covering delimiters
inside strings, `\(`, `\;`, `\"`, `\\`, `#"a\"b"`, `#"a\\"`, `#"[(){}]+"`, `;` inside
strings, a comment ending the file with no newline, `#?`/`#?@`, `#{}`, `#()`, `^{}`,
`#=`, `a#` followed by a regex, and a trailing backslash: **zero overcounts, zero
balance errors**. That part of the scanner is well built.

---

## What I verified and could NOT break

- **False refusals: 0.** All 165 `.clj`/`.cljc`/`.cljs` under `src/` and `test/` admit under the shipped defaults. Max nodes 19,528 (`intent_transaction.clj`) → **10.2x** margin; max depth 22 → **6.8x** margin. Both figures reproduce the receipt exactly.
- **Delimiter balance 0 on all 165**, and on all 87 files outside `src/`+`test/` I additionally checked.
- **The ceiling is exact.** N admits, N+1 refuses, for `()`, `[]`, `{}`, `#{}` and a mixed `({[` tower (147/150/153): 149 and 150 admit and parse; 151 refuses with `:max-parse-depth`. No off-by-one.
- **Zero parse calls on refusal** — `test/clj_surgeon/parser_admission_test.clj:134-147` redefines `z/of-string` and asserts 0; I re-ran it green.
- **Small thread stacks: 150 is still safe.** With the ceiling lifted, ungated cold SOE thresholds: `-Xss1m` paren >460, `{:k [` at 250 reps (depth 500); `-Xss512k` paren between 150 and 200, `{:k [` at 100 reps (depth 200). At `-Xss512k` a shape at exactly scan-depth 150 (`{:k [` x75) **completes**. So 150 holds at half the default stack. `-Xss256k` is not a real datapoint: the JVM cannot start Clojure at all there, MEM-005 or not.
- **Zero reflection warnings** in `clj-surgeon.parse-admission` under `*warn-on-reflection*` — the 638x `(long (.charAt …))` regression is fully cleared. (For the build-hygiene lane the builder asks for: `clj-surgeon.outline` loads with 11 warnings — 1 in `outline.clj:31`, 8 in `forms.clj`, 2 in `fields.clj`; `clj-surgeon.core` with 98. Nothing in the repo fails on them.)
- **The `ls-tree` skip surface is right**, and it is the one thing House Rule 17 most cares about. On a directory with one refusable file the scan **completes**, the good file's output is unchanged, and the refusal is named *and counted* in both renderings — text `── parser_admission_refused: 1 file(s)` plus a per-file line, and EDN with a trailing `{:receipt {:parser_admission_refused {:count 1 :files [...]}}}` carrying `:reason`, `:limit`, `:observed`, `:remedy`. Nothing is appended when nothing is refused.
- **No ID collisions.** `MCP-OP-MEM-005` and `-015` are registered only in `read-path-memory-specs.md`; `-001` and `-011` only in `memory-boundedness-specs.md`. But there is **no cross-lane allocation table** anywhere — the specs file says IDs "are allocated across several lanes" and then names no register. Three lanes minting into one series with no shared table is a collision waiting to happen; worth one table before the next lane allocates.
- **EARS vs code:** the row's *predicate* is honest — it promises rejection on the **lexical estimate**, and that is precisely what the code does, at the ceiling, exactly. Its *subject* ("clj-surgeon") and its `[x]` are what overreach (finding 2). Boundaries correctly name MEM-002 as the byte-ceiling owner (`parse_admission.clj:18-22`, design `:130-133`), and the deliberate absence of `:next_call` is both stated and tested.

## Gates — all green, run once each on this box

| gate | result | vs. builder's claim |
|---|---|---|
| `make test-fast` (suite-run) | 737 tests / 6103 assertions / 0 fail / 0 err | matches |
| `clojure -M:clj-surgeon/mcp-test` (suite-run) | 385 tests / 3971 assertions / 0 fail / 0 err | matches |
| `make mcp-operation-oracle` (suite-run) | pass; legacy counterexamples `[verification_failed, verification_pending]` | matches |
| `make memory-battery-self-test` (suite-run) | 24 tests / 138 assertions / 0 fail / 0 err | matches |
| `make memory-red PARSER_RED_EXPECT=green` (exclusive lock, run once) | **6/6 assertions held** | matches |
| intent traceability contract | `:ok true`, 0 violations | matches |

`make memory-red` was run **once**, under `flock /home/forge/tmp/suite.lock` (it queued
behind another tenant's job and ran clean). The full battery was **not** run, per the
receipt at `docs/observations/2026-09-03-mem-005-parser-admission.md`.

One honest discrepancy in the receipt's own numbers: my green re-run measured
**nested-warm peak 134.8 MB** where §3 of the receipt reports **52.5 MB** (all other
cells landed within noise: 45.9/60.1/59.8 MB against 44.6/52.6/65.9). Same verdict —
2.3x under the 247.8 MB budget and 2.3x below the 312.4 MB pre-fix figure — but the
published cell does not reproduce on a loaded box and should be restated as a range.

---

## Numbered fix list

1. **`src/clj_surgeon/parse_admission.clj:190`** — count reader-macro prefixes (`' \` ~ ~@ @ ^ #' #_ #= #?`) as nesting levels in `scan-shape`. *Witness:* `(str (apply str (repeat 700 "@")) "x")` must refuse `max_parse_depth`; today it is ADMITTED at `parse_depth 0` and StackOverflows. Costs the corpus nothing — max prefix run over 252 real files is 2. **Merge-blocking.**
2. **`src/clj_surgeon/core.clj:335`** — add `(catch StackOverflowError e {:file file :error … :refusal :parser_admission_refused-ish})` (or catch `Throwable`) inside the `pmap` worker at `core.clj:361`. *Witness:* `run-ls-tree` over a directory containing one 716-byte `@`-tower returns the other files' outlines instead of dying with `ExecutionException: StackOverflowError`. **Merge-blocking, one line, independent of #1.**
3. **`docs/intent/read-path-memory/read-path-memory-specs.md:20`** — scope the subject from "clj-surgeon" to the outline read path, and add a witness family for reader-macro nesting. *Witness:* a test named for the `@`-tower exists and fails before fix 1. **Merge-blocking as documentation.**
4. **`src/clj_surgeon/outline.clj:278-282`** — delete or correct the coverage list. *Witness:* `extract/plan` on the 600-level `{:k [` fixture OOMs today while the docstring says it inherits the ceiling.
5. **`src/clj_surgeon/show_form.clj:444`** — re-raise or preserve the typed refusal the way `safe-outline` does. *Witness:* `(sf/show-file {:file deep600 :form "x"})` returns `{:refusal nil :reason nil :limit nil}` where the spec requires `:reason`/`:limit`/`:observed`/`:remedy`.
6. **`src/clj_surgeon/structural_lens.clj:612,646`** — file a bead: a fourth ungated constructor on the MCP read surface (`mcp_inspect.clj:530`, `core.clj:737`), not mentioned in the receipt's §5 gap. *Witness:* `find-subforms` StackOverflows on the 4,222-byte 600-level fixture.
7. **`src/clj_surgeon/parse_admission.clj:141`** — handle `#!` as a line comment, and widen `default-ceilings-admit-every-source-in-this-repository` (`test/clj_surgeon/parser_admission_test.clj:184`) to `bench/`. *Witness:* `#!/usr/bin/env foo ((((\n(def x 1)` scores `balance=4`, breaking the invariant the spec calls proof of comment handling.
8. **`src/clj_surgeon/core.clj:420-431`** — emit `scan_ms` in the `ls-tree` receipt beside the refusal count. *Witness:* `rg scan-ms src/` returns nothing today; the measured charge is 1.27% and the first draft of this function was 638x slower with every test passing.
