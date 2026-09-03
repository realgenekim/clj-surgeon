# MEM-003 round 5 — Opus executed review of bridge/streaming-ls-tree at 0914a37 (2026-09-03T12:54Z)

Verdict: **GO-WITH-FIX**. Items 1–5 CLOSED on paired re-runs (storm 95/400 wrong → 0/800; symlinked dir refused; zero-advance page refuses; confinement refusal 4 reads → 0). Torn in-place write during the single open: 400/400 refuse. FIX before merge (minutes): (3) docs must name the SOURCE-file check-to-use gap (15–19/400 pages served fresh bytes of the correct in-root file under a clean receipt; pre-existing 14/400 at 281e13b; never escapes the root) — the pin is checked once at page start, not a read-time seal; (4) the race witness must assert `(pos? refusals)`. Follow-ups: (5) a row whose LEAF is a symlink to a directory passes confinement (typed error record, nothing escapes); (6) `:empty-result-page` is gated on `over?` at core.clj:962 so a final page cannot fire it (core.clj:840 is unconditional). Round 6 launched for all four.

## Opus verdict, verbatim

# streaming-ls-tree 0914a37 (MEM-003) — round-FIVE executed review

Independent executed review of `bridge/streaming-ls-tree` at **0914a37**, worktree
`/home/forge/tmp/sol/mem003r5-wt` (`git rev-parse --short HEAD` = `0914a37`). Every number below
is from my own harness under `/tmp/mem003r5-sol-fx`, `CLJ_SURGEON_STATE_ROOT` bound per run.
Where I quote a BEFORE figure I re-ran **the same harness against the round-four worktree**
`/home/forge/tmp/sol/mem003r4-wt` (`281e13b`) on **the same 200-file corpus**, so before and
after are one paired measurement rather than a comparison against my own report's prose. Nothing
was committed, stashed or pushed. The memory battery was NOT run (exclusive lock, another lane).

**Verdict: GO-WITH-FIX.** All five round-four items CLOSED on my own re-runs. Round five
introduces no defect that changes *what* a page serves. Four residuals, all doc- or
witness-strength: a pre-existing SOURCE-file check-to-use gap the design table's wording
overclaims about (measured 15–19 of 400, identical rate at 281e13b, so not a regression); a leaf
that names a directory slipping past a rule `row-file` states it enforces; a one-sided
`:empty-result-page` guard; and a race witness that can pass vacuously.

---

## Part 1 — my five round-four items, re-run

### Item 1 — verification and the slice are two opens of one mutable file — **CLOSED**

`fold-slice` ([ls_tree_snapshot.clj:305](src/clj_surgeon/ls_tree_snapshot.clj#L305)) folds the
digest, counts the rows and cuts `[offset, offset+limit)` inside ONE `with-open` at
[:340](src/clj_surgeon/ls_tree_snapshot.clj#L340); `verified-page`
([:376](src/clj_surgeon/ls_tree_snapshot.clj#L376)) compares `cursor-id` to the digest of exactly
those bytes; `run-pinned-page` calls it at [core.clj:886](src/clj_surgeon/core.clj#L886).

**My storm, same script, same corpus, both worktrees.** No `with-redefs`, no interposition: a
swapper thread renames a substituted rows file (same 200 rows; row 6 carries row 0's real path
and real digest) into place while 400 page-2 reads run.

BEFORE — `/home/forge/tmp/sol/mem003r4-wt` @ `281e13b`,
`bb --classpath src /tmp/mem003r5-sol-fx/h/b2_race.clj`:
```
rows total: 200
substituted row 6 -> {:i 6, :x 0, :p "src/fixt/m001.clj", :h "a85d3bb7377716bdf58e719fff6121c3258f24ab08cab6e8a44b380aa1c0df79"}
TALLY over 400 page-2 reads under a live rows-file swap:
{"REFUSE:unknown-result-cursor" 200,
 "SERVED-WRONG [fixt.m006 fixt.m001 fixt.m008 fixt.m009 fixt.m010]" 95,
 "SERVED-correct" 105}
```

AFTER — `mem003r5-wt` @ `0914a37`, identical script, run twice:
```
TALLY over 400 page-2 reads under a live rows-file swap:
{"REFUSE:unknown-result-cursor" 193, "SERVED-correct" 207}

TALLY over 400 page-2 reads under a live rows-file swap:
{"REFUSE:unknown-result-cursor" 192, "SERVED-correct" 208}
```

**95 of 400 wrong pages becomes 0 of 800.** The `SERVED-WRONG` key is absent from both after-runs,
and the refusal counts (193/192 vs 200) prove the swapper was still winning the file — the race
is contended and the page is refused, not merely never raced.

### Item 2 — a symlinked DIRECTORY component defeats the lexical boundary — **CLOSED**

`row-file` ([ls_tree_snapshot.clj:542](src/clj_surgeon/ls_tree_snapshot.clj#L542)) now resolves the
row's parent (or, via `deepest-existing` [:527](src/clj_surgeon/ls_tree_snapshot.clj#L527), its
deepest existing ancestor) with `real-path` and requires it under the real root; the leaf stays
lexical. My round-four reproduction re-run, every case laid down as a snapshot **re-folded and
re-filed so it PASSES verification** (`h/a3_confine.clj`, `src/linkdir -> /tmp/mem003r5-sol-fx/OUTSIDE`):

```
=== a4 SYMLINKED DIR component src/linkdir/secret.clj ===
  row6 = {:i 6, :x 0, :p "src/linkdir/secret.clj", :h "bf7b458c40b1000a81609a6b0503440d6d184d96123000e0dad695f432765aaf"}
  ->
{:kind :refusal,
 :error-type :unconfined-manifest-row,
 :error "pinned manifest row \"src/linkdir/secret.clj\" is not inside the scanned root",
 :limit {:kind :manifest-row, :requested "src/linkdir/secret.clj"}}
```

Round four served `[fixt.m06 leaked.secret fixt.m08 fixt.m09 fixt.m10]` here. The three older
escapes stay refused in the same run (`../OUTSIDE/secret.clj`, the absolute row, and the
sibling-prefix `../r1extra/secret.clj` — all `:unconfined-manifest-row`, each NAMING the path),
and the deliberately-admitted case still pages as discovery produces it:

```
=== c3 LEAF symlink to a FILE outside (discovery-consistent) ===
  -> {:kind :served, :n 5, :ns [fixt.m06 leaked.secret fixt.m08 fixt.m09 fixt.m10],
      :files ["src/fixt/m06.clj" "src/linkfile.clj" ...]}
```

That is the documented trade (`row-file` :561-578), not a regression: plain `find` lists a
symlinked `.clj` file, so refusing it on page 2 would be a page-1/page-2 divergence the guard
itself introduced.

### Item 3 — the requirement promised `resolve` where the code delivered `lexical` — **CLOSED**

The EARS text now says what the code does, in the same words:
`read-path-memory-specs.md:21` — "a manifest row **whose parent directory does not resolve inside
the scanned root**". The design table (`read-path-memory-design.md:300`) and `run-pinned-page`'s
docstring ([core.clj:864](src/clj_surgeon/core.clj#L864)) carry the same "parent directory
resolves outside" phrasing, and the falsifier table (`specs.md:78`) names both halves — the
symlinked FILE that must page and the symlinked DIRECTORY that must refuse. The requirement is no
longer falsified by a row the implementation accepts; my a4 row is now exactly the refusal the
text promises.

### Item 4 — the race was documented only in the harmless direction — **CLOSED**

Named in the unsafe direction in all three places a reader lands:
[core.clj:917-925](src/clj_surgeon/core.clj#L917) — "a short page under a full receipt lies about
how much was shown, never about WHAT was shown. **The dangerous direction is DIFFERENT rows of the
right length, which no count can see**"; `fold-slice`'s docstring
([:311-322](src/clj_surgeon/ls_tree_snapshot.clj#L311)) quotes the measurement and says the window
"is not a hairline — it is the whole fold, which is O(N) in the manifest, so it GROWS with the
corpus"; and `specs.md:78` puts it in the falsifier column ("400 page-2 reads under a LIVE
rows-file swap on a real filesystem, with no interposition, serving ZERO substituted pages, where
the two-open shape served 92 of 400"). Ratcheted by
`a-substituted-slice-is-never-served-under-a-live-rows-swap` (test:932) — a real 400-read race
with an atomic-move swapper, not a deterministic stand-in. (One caveat on that witness: item 9
below.)

### Item 5 — a page that encodes zero would mint a cursor at its own offset; and the nit, that the confinement refusal cost reads — **CLOSED (both)**

`budget/empty-page-refusal` ([result_budget.clj:311](src/clj_surgeon/result_budget.clj#L311)),
raised at [core.clj:962](src/clj_surgeon/core.clj#L962) and
[core.clj:840](src/clj_surgeon/core.clj#L840). Forced reachable by neutering the encoder
(`alter-var-root #'core/stream-outlines!`), `h/d_empty.clj`:

```
-- with the encoder forced to emit ZERO records --
MID page (offset 5, remaining 8 > ceiling 5, over? true):
{:error-type :empty-result-page,
 :error "the page at offset 5 encoded 0 of 5 pinned row(s); a page that advances by zero would repeat forever",
 :limit {:kind :result-records, :requested 5, :observed 0, :offset 5},
 :complete false, :source-unchanged true,
 :remedy "rescan from the start; this page cannot advance and its cursor would name its own offset",
 :next_call {:op :ls-tree, :dir "/tmp/mem003r5-sol-fx/r1", :max-results 5, :format :edn}}
```

The `:next_call` carries **no `:cursor` key** — the refusal really does decline to hand back a
token that cannot advance.

The nit — `unconfined-row` ([:595](src/clj_surgeon/ls_tree_snapshot.clj#L595)) claims "the refusal
costs no read at all", and `stale` is now a `delay` at
[core.clj:912](src/clj_surgeon/core.clj#L912) evaluated only in the `cond` at
[core.clj:935](src/clj_surgeon/core.clj#L935), after the confinement branch at
[core.clj:931](src/clj_surgeon/core.clj#L931). **Measured**, with the offending row placed LAST in
the slice so a sibling binding would digest four files first
(`h/nit5_reads.clj`, counting calls to `snapshot/content-digest`):

```
[AFTER  0914a37] result: :unconfined-manifest-row {:kind :manifest-row, :requested "../OUTSIDE/secret.clj"}
                 content-digest calls during the confinement refusal: 0

[BEFORE 281e13b] result: :unconfined-manifest-row {:kind :manifest-row, :requested "../OUTSIDE/secret.clj"}
                 content-digest calls during the confinement refusal: 4
   READ /tmp/mem003r5-sol-fx/r1/src/fixt/m06.clj
   READ /tmp/mem003r5-sol-fx/r1/src/fixt/m07.clj
   READ /tmp/mem003r5-sol-fx/r1/src/fixt/m08.clj
   READ /tmp/mem003r5-sol-fx/r1/src/fixt/m09.clj
```

4 reads to 0, my harness, both sides. Ratcheted by `an-unconfined-row-refusal-really-costs-no-read`
(test:1250).

### Carried forward — the two-cold-stores determinism check, re-run

Round four's item 6 qualification still holds exactly as documented
([ls_tree_snapshot.clj:52](src/clj_surgeon/ls_tree_snapshot.clj#L52),
`read-path-memory-design.md:388`). Three warm processes, two cold stores, 200-file corpus:

```
bytes: 7556
warm sha: a7974a315bb9b74d5b98 / a7974a315bb9b74d5b98 / a7974a315bb9b74d5b98
warm1==warm2==warm3 ?  IDENTICAL
cold1 vs cold2 diff:
403c403
<    ... :cursor 2ce0379cb8864f2311f055ad925eaeedc951aab32c73c0403a7df2b65bd092ed:100:c22ed15f147ee0e8da6414aa98803d60f0913f69777456e098eba530c01da4b5
---
>    ... :cursor 2ce0379cb8864f2311f055ad925eaeedc951aab32c73c0403a7df2b65bd092ed:100:179553d5ed10b3bf89eb38d40f27b1c06dee9872b14d71547938f2ef3045ceb3
diff line count: 2
snapshots in warm store: 1
```

Same manifest digest, one differing line, and that line is the MAC — the property the doc now
qualifies. Note `snapshots in warm store: 1`: reuse is working, three scans left one snapshot.

---

## Part 2 — attacking round five

### (a) A torn IN-PLACE write during the single open — **refusal, 400/400; no short page, no garbled page, no throw**

The builder's window claim is "one open, bytes as read — **not an atomic file read**", so the
question is what a reader observes when a writer truncates and rewrites the live rows file
underneath it, leaving a partial line at or near the slice boundary. A writer thread opens the
live file with `FileOutputStream(f, false)` and writes a byte PREFIX of the good content, cycling
ten cut lengths chosen to land mid-row across the page (`h/a_torn.clj`) — no rename, no atomicity:

```
rows bytes: 21890  rows: 200
TALLY over 400 page-2 reads under a TORN IN-PLACE rows write:
{"REFUSE:unknown-result-cursor" 400}
```

400 of 400 typed refusals. Nothing else appears in the tally: no `SERVED-OTHER`, no `THROW`. Two
mechanisms cover the space and neither can be dodged. A cut that lands mid-line makes
`edn/read-string` throw, or the row fails `(and (map? row) (= n (:i row)) (string? (:p row)))` at
[:344](src/clj_surgeon/ls_tree_snapshot.clj#L344), and `fold-slice`'s `catch Exception` returns
`nil` → `verified-page` nil → `:unknown-result-cursor`. A cut that lands exactly on a line
boundary yields a well-formed but SHORTER manifest, which folds to a different digest and fails
`(= cursor-id d)` at [:409](src/clj_surgeon/ls_tree_snapshot.clj#L409) — the short-file case the
docstring promises "cannot get that far, because a short file folds to a different digest". The
guard at [core.clj:926](src/clj_surgeon/core.clj#L926) never has to fire. **No finding.**

### (b) The SOURCE-file check-to-use gap — real, measured, PRE-EXISTING, no escape; but the design table's wording overclaims

The builder named this and did not claim it. It is real and I measured it. `stale-row`
([:610](src/clj_surgeon/ls_tree_snapshot.clj#L610)) digests each row's file; then
`pinned-candidates` ([core.clj:745](src/clj_surgeon/core.clj#L745)) hands those paths to the
encoder, which **reopens every one of them**. A swapper thread rewriting one in-root source file
between its original bytes and `(ns evil.pwned)`, 400 page-2 reads (`h/b_source_gap.clj`, 200-file
corpus; the state root is never touched):

```
[0914a37, run 1]                                  [0914a37, run 2]
{"REFUSE:stale-result-cursor" 329,                {"REFUSE:stale-result-cursor" 312,
 "SERVED-OTHER [... evil.pwned ...]" 19,           "SERVED-OTHER [... evil.pwned ...]" 15,
 "SERVED-OTHER [4 records, torn read]" 42,         "SERVED-OTHER [4 records, torn read]" 60,
 "SERVED-correct" 10}                              "SERVED-correct" 13}
```

**Is it reachable from the TREE rather than the state root?** Yes — that is the whole point, and
it is why it deserved asking. It needs only write access to the scanned source tree.

**Is it round three's wrong page in another coat, or a different, weaker class?** *Different, and
weaker.* State it precisely:

- Round three / round four substituted **WHICH FILE** the page serves. The attacker chose the
  path, and the path could name anything the resolver would accept — including, before item 2,
  a file outside the root. That is a confinement and identity failure.
- This substitutes **WHICH BYTES of the correct file**. The path served is exactly the pinned
  path, inside the root, and the bytes served are bytes the tree itself holds. Nothing crosses
  the boundary; my `evil.pwned` sits at `src/fixt/m007.clj` inside the root.
- The adversary gains nothing they did not already have. Anyone who can write `src/fixt/m007.clj`
  can leave that content there permanently, and an ordinary at-or-under-ceiling `ls-tree` — which
  pins nothing and checks nothing — reports it identically. A pinned page is therefore no weaker
  than the unbounded path it continues.
- **Not a regression.** Same harness at `281e13b`:
  `{"REFUSE:stale-result-cursor" 330, "SERVED-correct" 14, "[... evil.pwned ...]" 14, "[4 records]" 42}` —
  14/400 there, 15–19/400 here. Round five neither introduced it nor touched it, exactly as
  claimed.

**Where it belongs: MEM-003, as a named boundary — not an open defect, but the wording must
change.** Two texts read as a guarantee about the bytes a served page contains, and my run
falsifies that reading: `read-path-memory-design.md:300` — "a file this page must serve **no
longer holds its pinned content** → `:stale-result-cursor`" — and the EARS clause at
`specs.md:21`, "a page whose pinned file no longer holds its recorded content". A reader takes
away *if I got a page, its files held their pinned bytes*, and 15–19 of 400 pages did not. The
honest statement is that the pin is checked ONCE at page start and is not a read-time seal, which
is the same species of overclaim as round four's item 3 (`resolve` vs `lexical`) and deserves the
same treatment: one sentence, plus a witness pinning the boundary rather than the wish. This is a
doc fix, not a code fix — closing it in code would cost a second digest of every file per page and
still leave the window between the encoder's own read and its parse.

### (c) A row whose PARENT is inside the root but whose LEAF is itself a symlink to a directory — **admitted and PAGED as a typed error record; not refused, not thrown; nothing escapes**

Row `src/leafdir` with `src/leafdir -> /tmp/mem003r5-sol-fx/OUTSIDE`, inside a snapshot that passes
verification. The outcome depends entirely on the pinned `:h`, and both branches are benign:

```
=== c2 LEAF symlink to DIR, h="deadbeef" ===
  -> {:kind :refusal, :error-type :stale-result-cursor,
      :error "src/leafdir changed since this cursor was issued",
      :limit {:kind :pinned-content, :file "src/leafdir", :requested "deadbeef", :observed nil}}

=== c1 LEAF is symlink to a DIRECTORY, h = nil ===
  -> served, and the raw page is:
[{:ns fixt.m06, :file "src/fixt/m06.clj", ...}
 {:file "src/leafdir", :error "/tmp/mem003r5-sol-fx/r1/src/leafdir (Is a directory)"}
 {:ns fixt.m08, ...} {:ns fixt.m09, ...} {:ns fixt.m10, ...}
 {:receipt {:result_ceiling {:limit 5, :offset 5, :returned 5, :total 13, :remaining 3, ...}},
  :next_call {... :cursor "bd792f34...:10:44cc8604..."}}]
```

**The pinned-`nil` branch is the interesting one and it is still safe.** `content-digest` of a
directory returns `nil`, so a row pinning `:h nil` passes the staleness check; `row-file` admits it
because the parent `src` is real and inside; the encoder opens it, fails, and emits a typed error
record naming an **in-root** path. Nothing outside the root is read or named. The arithmetic stays
honest: `:returned 5` counts five emitted records (four outlines plus the error record — the same
accounting a fresh scan uses for an unreadable file), and the next cursor advances by 5, so no row
is skipped and no page repeats.

**The finding is doctrinal, not a leak.** `row-file`'s own stated rule
([:576-578](src/clj_surgeon/ls_tree_snapshot.clj#L576)) is "the guard refuses what discovery can
NEVER produce (an absolute path, a `..` escape, a symlinked DIRECTORY component) and defers to
discovery on what it can (a symlinked FILE)." A LEAF naming a directory — symlinked or plain — is
something `find -name '*.clj' -type f` can never produce, and the guard admits it. The rule as
written is not fully implemented. Cost of the gap: one wasted page slot and an error record. Cost
of the fix: refuse when the resolved leaf is a directory — which must NOT be spelled `.isFile`,
because a row whose file was legitimately deleted has to keep reaching `:stale-result-cursor`
rather than being falsely accused of escaping. **Nit, not a blocker.**

### (d) Can `:empty-result-page` fire on a legitimately empty final page and strand a caller? — **No. It is structurally unable to fire there.**

Two facts, both measured. First, no page is ever empty on the shipped path; a 13-row corpus walked
to exhaustion (`h/d_empty.clj`):

```
page1: [fixt.m01 fixt.m02 fixt.m03 fixt.m04 fixt.m05]  total= 13
page2 n=5 returned=5 remaining=3 next=yes
page3 n=3 returned=null remaining=null next=NONE
```

The final page carries three records, no receipt and no next cursor — a clean terminus, and
`remaining` fell 3 → done rather than stalling. Second, the guard at
[core.clj:962](src/clj_surgeon/core.clj#L962) is `(and over? (zero? @advanced))`, and `over?` is
`(> (- total offset) ceiling)` — **false on every final page by definition**. So the refusal cannot
be raised on a final page at all, and cannot strand a caller who had `remaining > 0` the page
before. Forcing the encoder to emit nothing and asking for the final page proves it:

```
FINAL page (offset 10, remaining 3 <= ceiling 5, over? FALSE):
[]
```

**The residual is the guard's one-sidedness, and it is worth a line.** On a final page the same
zero-encode condition returns a bare `[]` — no records, no receipt, no refusal — which a caller
reads as *complete result, nothing here*. That is precisely the failure mode
`:result-cursor-out-of-range` exists to prevent (`specs.md:76`: "rather than returning an empty
vector a caller reads as a complete result"). It is unreachable today for the two reasons
`empty-page-refusal`'s docstring already gives — the slice guard, and one `emit!` per candidate,
which my (c) run independently confirms fires even for a file that cannot be opened — but a
refactor breaking that invariant would be caught on mid pages and answer silently wrong on final
ones. The guard should be symmetric, or the asymmetry stated where a refactor reads it.
`run-fresh-scan`'s copy at [core.clj:840](src/clj_surgeon/core.clj#L840) is already unconditional
(`(if (zero? @advanced) ...)`), so the two sites disagree in shape.

### (e) Any remaining second-open path to the rows file — **none. `read-rows` is gone.**

```
$ grep -rn "read-rows" src/ test/ docs/ bench/
docs/observations/2026-09-03-mem-003-streaming-ls-tree.md:404:- `snapshot/read-rows` for the page's own slice, so a page does **no** discovery.
docs/observations/2026-09-03-mem-003-streaming-ls-tree.md:704:`read-rows` then **opened it again** to take the slice. Verification and use
docs/observations/2026-09-03-mem-003-streaming-ls-tree.md:727:and `read-rows` is **deleted** rather than left as an API a regression can
```

Zero hits in `src/` and `test/` — the API is deleted, not deprecated, so there is no shape a
regression can reach for. Every reader of the rows path in the namespace:

```
$ grep -n "rows-file\|io/reader\|line-seq\|slurp" src/clj_surgeon/ls_tree_snapshot.clj
155:(defn- rows-file ^File [root cursor-id] ...)
340:        (with-open [r (io/reader f)]        <- the ONLY open, inside fold-slice
351:                    (line-seq r))]
403:    (when-let [[slice d n] (fold-slice root (rows-file root cursor-id) offset limit)]
493:          (.delete (rows-file root cursor-id))    <- write path
494:          (.renameTo tmp-rows (rows-file root cursor-id))  <- write path
520:        (try (edn/read-string (slurp f)) ...)   <- read-meta, the .edn only
```

One `io/reader`, in `fold-slice`. `rows-digest` ([:355](src/clj_surgeon/ls_tree_snapshot.clj#L355))
and `verified-snapshot` ([:414](src/clj_surgeon/ls_tree_snapshot.clj#L414)) are both thin wrappers
over `fold-slice` with a zero-width slice, so even the reuse path has exactly one open. `read-meta`
slurps only the `.edn`. **Closed, and closed structurally rather than by convention.**

### Nit — the new race witness can pass vacuously

`a-substituted-slice-is-never-served-under-a-live-rows-swap` (test:932) asserts `(zero? wrong)`
and `(pos? (reduce + 0 (vals tally)))` — "the storm actually ran". Neither asserts the storm was
**contended**. A swapper that never won a single read (a slow CI box, a scheduler that starves the
future, a `move!` that starts throwing) passes both assertions while measuring nothing. My own
after-runs show the contention signal is available and free: 193 and 192 refusals out of 400.
`(is (pos? refusals))` turns it from a test that can silently stop testing into one that says so.
This is the round-four lesson about `:returned` — a measurement that cannot disagree with what it
replaced — in its test-side form.

---

## Gates — ran-lines verbatim, each once, JVM suites under `/home/forge/bin/suite-run`

`/home/forge/bin/suite-run bb test/run_all.clj`:
```
Ran 776 tests containing 6315 assertions.
0 failures, 0 errors.
```

`/home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test` (direct, never
`make mcp-test`):
```
Ran 389 tests containing 3988 assertions.
0 failures, 0 errors.
MCPTEST_EXIT=0
```

`swipl -q -f test/mcp_operation_contract_oracle.pl`:
```
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0
```

Battery self-test and `generate_tree` self-test:
```
Ran 24 tests containing 138 assertions.
0 failures, 0 errors.
{:test 24, :pass 138, :fail 0, :error 0, :type :summary}
BATTERY_EXIT=0
generate_tree verification self-test: ok
generate_tree self-test: ok
GENTREE_EXIT=0
```

All four reproduce the builder's stated figures exactly (776/6315/0, 389/3988/0, oracle pass,
24/138/0). The memory battery itself was **not** run — exclusive lock, another lane.

---

## Verdict

**GO-WITH-FIX** for the mayor's merge queue.

1. **Items 1–5 are CLOSED**, each on my own paired re-run against `281e13b` with the same script
   and the same corpus: the substitution storm 95/400 wrong → **0 of 800**; the symlinked-directory
   row served → `:unconfined-manifest-row`; the requirement text now says "parent directory
   resolves"; the race documented in the dangerous direction in code, docstring and falsifier
   table; the zero-advance page refused with no cursor; the confinement refusal **4 reads → 0**.
2. **No new defect changes what a page serves.** A torn in-place write during the single open
   refuses 400/400 with no short page, no garbled page and no throw.
3. **FIX before merge (docs only, minutes): the SOURCE-file check-to-use gap must be NAMED.**
   `read-path-memory-design.md:300` and `specs.md:21` read as a guarantee about the bytes a served
   page holds; 15–19 of 400 pages served content the pin did not cover. Say the pin is checked once
   at page start and is not a read-time seal. It is pre-existing (14/400 at `281e13b`), stays
   inside the root, and grants an attacker nothing an ordinary unpinned scan would not already
   report — which is exactly why the *wording*, not the code, is what has to change.
4. **FIX before merge (one assertion): make the race witness prove it was contended.** Add
   `(is (pos? refusals))` to test:932. As written it passes whether or not the swapper ever won.
5. **Follow-up, not a blocker: a row whose LEAF names a directory slips past the rule `row-file`
   states it enforces.** `src/leafdir` pinned with `:h nil` pages as a typed error record. Nothing
   escapes and the arithmetic stays honest, but discovery can never produce that row. Refuse a
   resolved leaf that is a directory — and do not spell it `.isFile`, or a legitimately deleted
   file loses its `:stale-result-cursor` and gets falsely accused of escaping.
6. **Follow-up, not a blocker: make the `:empty-result-page` guard symmetric.**
   [core.clj:962](src/clj_surgeon/core.clj#L962) is gated on `over?`, so it cannot fire on a final
   page — a forced zero-encode there returns a bare `[]` that reads as a complete result, the very
   thing `:result-cursor-out-of-range` exists to prevent. `run-fresh-scan`'s copy at
   [core.clj:840](src/clj_surgeon/core.clj#L840) is already unconditional; the two sites should
   agree.
7. **All four gates green and matching**: 776/6315/0, 389/3988/0, oracle pass, 24/138/0. Memory
   battery not run (exclusive lock, another lane). Nothing committed, stashed or pushed.
