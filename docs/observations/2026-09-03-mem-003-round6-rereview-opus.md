# MEM-003 round 6 — Opus confirm re-check of bridge/streaming-ls-tree at 3cedd44 (2026-09-03T19:22Z)

Verdict: **NO-GO** — one code fix plus one witness from GO. BLOCKER: the round-6 `.isDirectory` gate (ls_tree_snapshot.clj:613) assumes discovery lists only files, but core.clj:215 passes no `-type` predicate — an untampered tree with a directory `src/mydir.clj` is listed, and page 3 now refuses `:unconfined-manifest-row "src/mydir.clj" is not inside the scanned root` (a false statement; two innocent files lost; pagination unfinishable) while an unbounded scan serves all 13. The round-6 witness reaches the case only via `repin-row!` — a test on a tree where DISCOVERY produced the row is required. Items 3, 4, 5 (stated case), 6 CLOSED. Follow-ups: a leaf symlinked to a directory deleted before the page slips the gate (benign); a FIFO named `*.clj` hangs the operation forever (pre-existing at 0914a37; `io/input-stream` blocks in open(2); `-type f` removes it). Round 7 launched.

## Opus verdict, verbatim

# streaming-ls-tree 3cedd44 (MEM-003) — round-SIX confirm-only re-review

Independent executed re-review of `bridge/streaming-ls-tree` at **3cedd44**, worktree
`/home/forge/tmp/sol/mem003r6-wt` (`git rev-parse HEAD` = `3cedd449ba86bd0772b0b9d3244fd5432e1c8eaf`,
tree clean, nothing committed / stashed / pushed). Every number below is from my own harness under
`/tmp/mem003r6-sol-fx`, `CLJ_SURGEON_STATE_ROOT` bound per run. Paired BEFORE figures are the same
harness against `/home/forge/tmp/sol/mem003r5-wt` (`0914a37`) on the **same fixture**, so before and
after are one measurement rather than a comparison against prose. The memory battery was NOT run.

**Verdict: NO-GO.**

My four round-five items: **3 CLOSED, 4 CLOSED, 6 CLOSED, 5 CLOSED-for-the-stated-case but its fix
introduced a functional REGRESSION on untampered input.** Commit `c86e365` refuses a manifest row
whose leaf resolves to a directory — and real discovery *does* produce such rows, because
`find-clj-files` passes **no `-type f`**. On a tree containing a directory named `src/mydir.clj`,
round five paged to completion and round six refuses the whole final page with a statement that is
false. That is precisely the "page-1/page-2 divergence introduced by the guard itself" that
`row-file`'s own docstring says the lexical leaf exists to prevent. Round five called the thing this
commit fixes a **nit**; the fix costs a working page.

---

## Part 1 — my four items, re-run

### Item 3 — the docs must NAME the source-file check-to-use window — **CLOSED**

Three texts changed and all three are now TRUE against my own measurement.

`read-path-memory-design.md:303`:

> **`:stale-result-cursor` is a boundary, not a read-time seal.** The digest check above runs ONCE
> per candidate, at page start; the encoder reopens each candidate's source file separately, after
> that check. A source file that changes in that window is not re-checked, so a page can carry fresh
> bytes of the correct, in-root, pinned file under a receipt that reports no staleness — measured at
> 15–19 of 400 page-2 reads under a deliberate live swap of an already-pinned source file … The path
> served is always the pinned path and it is always inside the scanned root: this check never lets a
> different file, or a file outside the root, stand in for the one that was pinned.

`read-path-memory-specs.md:79`, the new falsifier row, states the FALSE belief as the hypothesis
("`:stale-result-cursor` is a read-time seal, so a page's files hold exactly their pinned bytes for
as long as the page is being served") and the measurement as its falsifier — the correct shape for
that table. `specs.md:21` now reads "a page whose pinned file — **checked once, at page start** — no
longer held its recorded content **as of that check**", and the design table row at `:300` matches.

**My re-run** (`h/b_source_gap.clj`, 200-file corpus, 400 page-2 reads, a swapper rewriting the
already-pinned in-root `src/fixt/m007.clj` between its real bytes and `(ns evil.pwned)`; the state
root is never touched):

```
target: /tmp/mem003r6-sol-fx/r200/src/fixt/m007.clj
pinned h: 12fc24a406d2a94c6865f825c7aa02571010b237d82baef530813151ab9ac54d
TALLY over 400 page-2 reads under a live SOURCE-FILE content swap:
{"REFUSE:stale-result-cursor" 334,
 "SERVED-OTHER [fixt.m006 fixt.m008 fixt.m009 fixt.m010]" 43,
 "SERVED-correct" 8,
 "SERVED-OTHER [fixt.m006 evil.pwned fixt.m008 fixt.m009 fixt.m010]" 15}
```

**15 of 400** — inside the stated 15–19, and the served path is `src/fixt/m007.clj`, the pinned path,
inside the root. The wording no longer promises what the code does not deliver. **Closed.**

### Item 4 — the race witness must prove it was CONTENDED — **CLOSED**

`test/clj_surgeon/ls_tree_budget_test.clj:998`:

```clojure
(is (pos? refusals)
    (str "the storm must be contended: at least one page must have "
         "been refused, or this witness measures nothing — tally: " (pr-str tally)))
```

with `refusals (get tally "REFUSE:unknown-result-cursor" 0)` at `:987`.

**My re-run** (`h/b2_race.clj`, 200-file corpus, no interposition, a swapper renaming a substituted
rows file — row 6 carrying row 0's real path and real digest — into place under 400 page-2 reads):

```
rows total: 200
substituted row 6 -> {:i 6, :x 0, :p "src/fixt/m001.clj", :h "a85d3bb7377716bd…"}
TALLY over 400 page-2 reads under a live rows-file swap:
{"SERVED-correct" 184, "REFUSE:unknown-result-cursor" 216}
```

**216 refusals of 400, zero `SERVED-WRONG`.** The assertion is live rather than decorative: the full
suite is green (778/6322/0), which means `(pos? refusals)` was actually TRUE in a real CI run, so the
key string matches the tally the test itself builds. Residual nit, no action needed: keying on the
literal `"REFUSE:unknown-result-cursor"` means a future rename of that refusal makes this assertion
go RED rather than silently vacuous — which is the safe direction. **Closed.**

### Item 6 — `:empty-result-page` no longer gated on `over?` — **CLOSED**

`core.clj:974` is now `(if (zero? @advanced) …)`, matching `run-fresh-scan`'s copy at `core.clj:840`.

**My re-run** (`h/d_empty.clj`, 13-row corpus). First the shipped path, walked to exhaustion — the
change does not make a legitimately-final page refuse:

```
page1: [fixt.m01 fixt.m02 fixt.m03 fixt.m04 fixt.m05]  total= 13
page2 n=5 returned=5 remaining=3 next=yes
page3 n=3 returned=null remaining=null next=NONE
```

Then the encoder neutered (`alter-var-root #'core/stream-outlines!`), both pages:

```
MID page (offset 5, remaining 8 > ceiling 5, over? true):
{:error-type :empty-result-page,
 :error "the page at offset 5 encoded 0 of 5 pinned row(s); …",
 :limit {:kind :result-records, :requested 5, :observed 0, :offset 5}, …}

FINAL page (offset 10, remaining 3 <= ceiling 5, over? FALSE):
{:error-type :empty-result-page,
 :error "the page at offset 10 encoded 0 of 3 pinned row(s); …",
 :limit {:kind :result-records, :requested 3, :observed 0, :offset 10},
 :next_call {:op :ls-tree, :dir "…/r1", :max-results 5, :format :edn}}
```

Round five got a bare `[]` here. The final page now refuses, and `:next_call` carries **no `:cursor`**.
Pinned by `a-FINAL-page-that-encodes-nothing-refuses-instead-of-a-bare-empty-vector` (test:1099).
**Closed.**

### Item 5 — a leaf resolving to a DIRECTORY refuses — **CLOSED for the stated case; the fix REGRESSES an untampered path**

`ls_tree_snapshot.clj:613` — `(when-not (.isDirectory f) f)`.

**My re-run** (`h/a3_confine.clj`, every case re-folded and re-filed so the snapshot PASSES
verification). My leafdir row with `:h nil` — round five's exact reproduction:

```
=== c1 LEAF is symlink to a DIRECTORY, h=real dir-digest(nil) ===
  row6 = {:i 6, :x 0, :p "src/leafdir", :h nil}
  -> {:kind :refusal, :error-type :unconfined-manifest-row,
      :error "pinned manifest row \"src/leafdir\" is not inside the scanned root",
      :limit {:kind :manifest-row, :requested "src/leafdir"}}
```

Round five served this as a typed `:error` record inside a page. The three older escapes stay refused
(`../OUTSIDE/secret.clj`, the absolute row, `src/linkdir/secret.clj`, the sibling-prefix row), and the
deliberately-admitted symlinked FILE still pages (`c3` → `[fixt.m06 leaked.secret fixt.m08 fixt.m09
fixt.m10]`). The docstring's `.isDirectory`-not-`.isFile` promise (`:560-564`) **holds** — I checked it
directly rather than trusting it, deleting a pinned regular file between pin and page:

```
=== DELETED regular file ===
  leaf: exists= false  isDirectory= false  isFile= false
  RESULT: {:error-type :stale-result-cursor, :error "src/fixt/m07.clj changed since this cursor was issued",
           :limit {:kind :pinned-content, :file "src/fixt/m07.clj", :requested "deadbeefdeadbeef", :observed nil}}
```

**But the justification the commit rests on is false, and it costs a working page.** See Part 2.

---

## Part 2 — the blocker: `find-clj-files` passes no `-type f`

`row-file`'s docstring, `ls_tree_snapshot.clj:584`, justifies the new refusal:

> a LEAF naming a directory is likewise a row no scan can ever produce — **`find -type f` never lists
> a directory, symlinked or plain**

and `:592` generalises it:

> The rule the three bullets share: the guard refuses what discovery can NEVER produce (an absolute
> path, a `..` escape, a symlinked DIRECTORY component, **a leaf naming a directory**)

**Discovery does not run `find -type f`.** `core.clj:207-216`:

```clojure
(babashka.process/shell
  {:out :string :err :string :continue true}
  "find" (str dir)
  "-name" "*.clj" "-o" "-name" "*.cljs" "-o" "-name" "*.cljc")
```

No `-type` predicate at all. Measured on the real command:

```
$ find …/rdir/src -name "*.clj" -o -name "*.cljs" -o -name "*.cljc"
…/rdir/src/fixt/m01.clj … m12.clj
…/rdir/src/mydir.clj          <- a DIRECTORY
$ find …/rdir/src -type f \( -name "*.clj" … \) | wc -l
12
```

### The regression, paired, on an untampered tree

Fixture: an ordinary 12-file project plus a **directory** named `src/mydir.clj`. No tampering, no
`repin-row!`, no swapper. Discovery pins it honestly as row 12 with `:h nil`.

**BEFORE — `0914a37`, same fixture, same harness (`h/dirleaf.clj`):**
```
row-file on the dir row -> "/tmp/mem003r6-sol-fx/rdir/src/mydir.clj"
page1 : [fixt.m01 … fixt.m05] next= yes
page2 -> served, [fixt.m06 … fixt.m10], remaining 3
page3 -> {:kind :served, :n 2, :ns [fixt.m11 fixt.m12], :receipt nil, :next nil}
```

**AFTER — `3cedd44`, identical:**
```
row-file on the dir row -> ""
page1 : [fixt.m01 … fixt.m05] next= yes
page2 -> served, [fixt.m06 … fixt.m10], remaining 3
page3 -> {:kind :refusal, :error-type :unconfined-manifest-row,
          :error "pinned manifest row \"src/mydir.clj\" is not inside the scanned root",
          :limit {:kind :manifest-row, :requested "src/mydir.clj"}}
```

Three things are wrong at once:

1. **The refusal's statement is false.** `src/mydir.clj` *is* inside the scanned root. Discovery
   itself produced it. A confinement refusal that names an in-root path will send a reader hunting an
   attack that did not happen.
2. **Two innocent files are lost and the pagination is unfinishable.** `fixt.m11` and `fixt.m12` never
   arrive; rescanning rebuilds the same manifest and refuses again. There is no cursor a caller can
   construct that gets past row 12.
3. **It is exactly the divergence the docstring says the design avoids.** `ls_tree_snapshot.clj:573`:
   "resolving the LEAF would refuse on page 2 what page 1 encoded — a page-1/page-2 divergence
   introduced by the guard itself." An unbounded scan of the same tree still serves the row:

```
=== UNBOUNDED fresh scan, same tree ===
records: 13
  -> {:file "src/mydir.clj", :error "…/rdir/src/mydir.clj (Is a directory)"}
```

The fresh path serves 13 records; the pinned path refuses the page. That is the divergence, introduced
by this guard, on this branch.

**No test catches it.** `a-manifest-row-whose-LEAF-is-a-symlinked-DIRECTORY-is-refused-not-paged`
(test:1290) reaches the row through `repin-row!` — a tampered manifest. It never exercises a directory
row that *discovery* produced, which is the only case where the refusal is wrong.

### Fix guidance (why "just add `-type f`" is a trap)

Making the docstring true by adding `-type f` would stop discovery listing symlinked `.clj` FILES
(`-type f` is false for a symlink without `-L`), breaking
`a-symlinked-file-inside-the-root-pages-exactly-as-it-is-discovered` (test:1360) and the deliberate
trade at `ls_tree_snapshot.clj:566-578`. It needs `\( -type f -o -type l \)`, and note that
`core.clj:215`'s `-o` chain is **unparenthesized**, so a naively inserted `-type f` binds only to the
first `-name`. The cheaper repair is to drop the `.isDirectory` refusal and restore round-five
behaviour — one typed error record, one wasted page slot, honest arithmetic, nothing escaping —
which is what round five explicitly called a nit rather than a blocker.

---

## Part 3 — my two attacks on the `.isDirectory` gate

### (a) leaf = symlink to a directory DELETED between pin and page — **SERVED as an error record; not stale, not refused, not thrown**

```
=== LEAF symlink -> dir, target DELETED after pin ===
  row6 = {:i 6, :x 0, :p "src/gonedir", :h nil}
  leaf: exists= false  isDirectory= false  isFile= false
  row-file -> "…/r1/src/gonedir"
  RESULT: served, 4 outlines + 1 error record, :returned 5, :remaining 4, next cursor advances by 5
```

`.isDirectory` is false for a dangling symlink, so the gate does not fire; `content-digest` returns
`nil`, which equals the pinned `nil`, so staleness does not fire; the encoder opens it, fails, and
emits a typed error record naming an in-root path. **Nothing escapes and the arithmetic stays honest**
(`:returned 5` counts the four outlines plus the error record, and the cursor advances by 5). But it
shows the new gate closes a *case*, not the *class*: the wasted-page-slot behaviour round five
reported is still reachable, now through a dangling link rather than a live directory. Since it is
also a check-to-use gate, a target deleted between `row-file` and the encoder reaches the same place.
**Not a blocker — it is the round-five behaviour, which was already judged benign.**

### (b) leaf that is a FIFO or a socket — **socket benign; FIFO HANGS THE OPERATION INDEFINITELY**

Socket:
```
=== LEAF is a unix SOCKET ===
  leaf: exists= true  isDirectory= false  isFile= false
  RESULT: served, 4 outlines + 1 error record, :returned 5
```
Benign — `io/input-stream` throws, digest is `nil`, error record, honest arithmetic.

FIFO — **the serious one, and it needs no tampering at all.** Because `find-clj-files` has no `-type`
predicate (same root cause as Part 2), a named pipe called `src/pipe.clj` is DISCOVERED:

```
$ find …/rfifo/src -name "*.clj" -o -name "*.cljs" -o -name "*.cljc" | tail -1
…/rfifo/src/pipe.clj
$ timeout 5 cat …/rfifo/src/pipe.clj ; echo $?
124        # blocked
```

`content-digest` (`ls_tree_snapshot.clj:174-192`) opens it with `io/input-stream`, which blocks in
`open(2)` until a writer appears. A plain fresh scan of that tree produced **zero output** and had to
be SIGKILLed:

```
3cedd44: fresh scan of the FIFO tree, 100 s bound -> no output, killed (SIGTERM did not land; -k KILL did)
0914a37: same, 90 s bound                          -> no output, killed
```

An operation whose contract is a typed receipt returns nothing, forever, and does not respond to
SIGTERM. **This is PRE-EXISTING — it reproduces identically at `0914a37`, so round six neither
introduced nor worsened it** — and I report it as a follow-up, not as part of the NO-GO. It matters
here only because it is the *same missing predicate*: the branch's confinement doctrine is written
around "what `find -type f` can produce", and the flag is not there. One fix to `core.clj:215` makes
the docstring true, removes the hang, and removes the Part-2 regression at the same time.

---

## Gates — ran-lines verbatim, each once, JVM suites under `/home/forge/bin/suite-run`

```
$ /home/forge/bin/suite-run bb test/run_all.clj
Ran 778 tests containing 6322 assertions.
0 failures, 0 errors.
TESTFAST_EXIT=0

$ /home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
Ran 389 tests containing 3988 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0

$ swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0

$ bb bench/memory_battery/generate_tree.clj --self-test
generate_tree verification self-test: ok
generate_tree self-test: ok
$ bb -e "(require 'clj-surgeon.memory-battery-test …)"
Ran 24 tests containing 138 assertions.
0 failures, 0 errors.
BATTERY_EXIT=0
```

All four reproduce the builder's stated figures exactly (778/6322/0, 389/3988/0, oracle pass,
24/138/0). The memory battery itself was **not** run. Nothing committed, stashed or pushed; worktree
clean at `3cedd44`.

---

## Verdict

**NO-GO** for the mayor's merge queue — one code fix plus one witness away from GO.

1. **BLOCKER — `src/clj_surgeon/ls_tree_snapshot.clj:613` (`(when-not (.isDirectory f) f)`), justified
   by a premise `src/clj_surgeon/core.clj:215` does not implement.** Witness: on an untampered tree
   containing a directory `src/mydir.clj`, `0914a37` page 3 served `[fixt.m11 fixt.m12]` and `3cedd44`
   page 3 refuses `:unconfined-manifest-row "src/mydir.clj" is not inside the scanned root` — a false
   statement, two innocent files lost, pagination permanently unfinishable, while an unbounded scan of
   the same tree still serves all 13 records.
2. **BLOCKER (same fix) — `src/clj_surgeon/ls_tree_snapshot.clj:584` claims "`find -type f` never lists
   a directory"; `src/clj_surgeon/core.clj:215` passes no `-type` predicate.** Witness: the real
   discovery command lists `…/rdir/src/mydir.clj` (13 paths); adding `-type f` yields 12. Beware
   `core.clj:215`'s unparenthesized `-o` chain and `test/clj_surgeon/ls_tree_budget_test.clj:1360`,
   which requires symlinked FILES to keep being listed.
3. **BLOCKER (witness) — `test/clj_surgeon/ls_tree_budget_test.clj:1290` reaches the directory leaf only
   through `repin-row!`.** Witness: the suite is green at 778/6322/0 while the untampered case in (1)
   is broken; the new refusal needs a test on a tree where DISCOVERY produced the row.
4. **CLOSED — item 3, the source-file check-to-use window is named.**
   `docs/intent/read-path-memory/read-path-memory-design.md:303` and
   `docs/intent/read-path-memory/read-path-memory-specs.md:21,79`. Witness: my 400-read swap served
   `evil.pwned` under a clean receipt in **15 of 400**, inside the stated 15–19, always at the pinned
   in-root path — both passages now TRUE.
5. **CLOSED — item 4, the race witness proves contention.**
   `test/clj_surgeon/ls_tree_budget_test.clj:998`. Witness: my storm tallied
   `{"SERVED-correct" 184, "REFUSE:unknown-result-cursor" 216}` — **216 refusals**, zero wrong pages.
6. **CLOSED — item 6, `:empty-result-page` is symmetric.** `src/clj_surgeon/core.clj:974` now matches
   `:840`. Witness: a forced zero-encode on the FINAL page (offset 10) refuses `:empty-result-page`
   with no `:cursor` in `:next_call`, where round five returned a bare `[]`; the unforced walk still
   ends cleanly at `page3 n=3 next=NONE`.
7. **CLOSED — item 5's stated case.** `src/clj_surgeon/ls_tree_snapshot.clj:613`. Witness: my
   `src/leafdir` row with `:h nil` now refuses `:unconfined-manifest-row` naming the path (round five
   served it as an error record), while a DELETED regular file still reaches
   `:stale-result-cursor … :observed nil`, so the `.isDirectory`-not-`.isFile` promise at `:560-564`
   holds.
8. **Follow-up, not a blocker — the gate closes a case, not a class.**
   `src/clj_surgeon/ls_tree_snapshot.clj:613`. Witness: a leaf symlinked to a directory whose target is
   deleted before the page is served slips the gate (`isDirectory=false`), pages as a typed error
   record with `:returned 5` and an honest advance — round five's benign behaviour, still reachable.
9. **Follow-up, PRE-EXISTING (reproduces at `0914a37`) — a FIFO named `*.clj` hangs the operation
   forever.** `src/clj_surgeon/core.clj:215` discovers it; `src/clj_surgeon/ls_tree_snapshot.clj:180`
   (`io/input-stream`) blocks in `open(2)`. Witness: a fresh scan of a tree containing `src/pipe.clj`
   produced zero output at both `3cedd44` and `0914a37` and survived SIGTERM, requiring SIGKILL. The
   `-type` fix in (2) removes this too.
10. **Gates green and matching the builder:** 778/6322/0, 389/3988/0, oracle pass, 24/138/0. Memory
    battery not run. Nothing committed, stashed or pushed.
