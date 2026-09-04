# MEM-003 round 7 — Opus executed re-check of bridge/streaming-ls-tree at 6625b7d (2026-09-03T20:02Z)

Verdict: **GO-WITH-FIX**. Round-6 blockers 1–3 and follow-ups 8–9 CLOSED on the reviewer's own trees (13 → 12 discovered; paged = unbounded; dangling leaf refused while a deleted file stays stale; FIFO tree scans in 26 ms). Both builder refusals judged RIGHT with witnesses (NOFOLLOW would refuse on page 2 what page 1 served; dropping the leaf check turns substitutions into staleness receipts). THE FIX (pre-existing, one line): core.clj:255 splits `find` output on newlines — a filename containing a newline throws `IllegalArgumentException` out of the scan; use `-print0` and split on NUL. Observation outside the diff: the empty-scan branch calls `(System/exit 1)` inside the library operation (core.clj:874). Round 8 launched.

## Opus verdict, verbatim

# streaming-ls-tree 6625b7d (MEM-003) — round-SEVEN independent executed re-review

Worktree `/home/forge/tmp/sol/mem003r7-wt`, `git rev-parse HEAD` =
`6625b7dbfe463c79be061ba52d861c61012b0da6`, tree clean, stash empty, nothing committed /
stashed / pushed. Every figure below is from my own harness under `/tmp/mem003r7-sol-fx`
with `CLJ_SURGEON_STATE_ROOT` bound per run. The memory battery was NOT run. Ports
7888-7895 and 7906 were never contacted. Every FIFO probe ran under `timeout -s KILL`.

**Verdict: GO-WITH-FIX.**

All three of my round-six blockers are CLOSED against my own re-run, both follow-ups are
CLOSED, and both of the builder's refusals of the brief are RIGHT with a witness each.
Six new attacks found one item: a filename containing a NEWLINE produces a phantom
candidate that is neither a regular file nor a symlink to one — falsifying, in the
*shipped* text, the exact claim this round exists to make true — and throws an untyped
`IllegalArgumentException` out of an operation whose contract is a typed receipt. It is
PRE-EXISTING (identical code at `3cedd44`), so it does not undo the round; it is a one-line
fix in the same function the round already rewrote, and it should land before merge.

---

## Part 1 — my round-six items, re-run

### Blocker 1 + 2 — the missing `-type` predicate — **CLOSED**

`src/clj_surgeon/core.clj:246-253` now shells

```
find <dir> ( -name '*.clj' -o -name '*.cljs' -o -name '*.cljc' )
           ( -type f -o ( -type l -xtype f ) )
```

as argv tokens, both chains parenthesized. Measured at the shell on MY tree (12 `.clj`
files plus a DIRECTORY named `src/mydir.clj`):

```
shipped predicate : 12 paths, src/mydir.clj absent
old  predicate    : 13 paths
```

And through the operation, on that same untampered tree, `max-results 5`:

```
UNBOUNDED: 12 records [src/fixt/mod00.clj ... src/fixt/mod11.clj]
page1: SERVED [mod00 mod01 mod02 mod03 mod04]
page2: SERVED [mod05 mod06 mod07 mod08 mod09]
page3: SERVED [mod10 mod11]           <- round six REFUSED this page
DONE, records= 12    paged == unbounded, record for record: true
```

Round six refused page 3 with `:unconfined-manifest-row "src/mydir.clj" is not inside the
scanned root` and lost `mod10`/`mod11` permanently. The two innocent files are back and the
pagination finishes. **Closed.**

### Blocker 3 — a witness reaching the refusal through DISCOVERY, not `repin-row!` — **CLOSED**

`test/clj_surgeon/ls_tree_budget_test.clj:1418`
(`a-DIRECTORY-named-like-a-source-file-is-never-discovered-and-never-blocks-a-page`) builds
the directory with `fs/create-dirs` and walks three real pages — no `repin-row!` anywhere in
it. `:1458` (`a-DISCOVERED-row-whose-parent-becomes-a-symlink-out-of-the-root-refuses`)
tampers with the TREE, not the manifest, and asserts the bytes are unchanged so staleness
cannot be what refused. `:1490` is the parity witness (one tree carrying all four shapes,
walked to exhaustion, compared with the unbounded scan record for record). I re-derived the
same parity independently above. **Closed.**

### Follow-up 8 — the gate closed a case, not a class — **CLOSED**

`src/clj_surgeon/ls_tree_snapshot.clj:660-666`, `.isDirectory` replaced by

```clojure
(when-not (and (Files/exists resolved [NOFOLLOW_LINKS])
               (not (Files/isRegularFile resolved [])))   ; links FOLLOWED
  f)
```

My re-run — `src/fixt/zdang.clj -> outside/target.clj` DISCOVERED and pinned on page 1, then
repointed to a nonexistent target before page 2:

```
exists NOFOLLOW: true   regular-file?: false
page2 -> {:error-type :unconfined-manifest-row,
          :error "pinned manifest row \"src/fixt/zdang.clj\" does not resolve to a
                  source file inside the scanned root"}
```

Round six SERVED this as a typed error record inside the page, spending a slot. It refuses
now, and the tamper/deletion line is witnessed on BOTH sides — a legitimately deleted file
in the same position still reaches the right refusal:

```
page2 (m9.clj simply deleted) ->
  {:error-type :stale-result-cursor, :error "src/fixt/m9.clj changed since this cursor
   was issued", :limit {... :observed nil}}
```

**Closed.** The message change also matters: round six said "is not inside the scanned
root" of a path that WAS inside it. `result_budget` now says "does not resolve to a source
file inside the scanned root", which is true of both facts that reach this receipt.

### Follow-up 9 — a FIFO named `*.clj` hangs forever — **CLOSED, at three layers**

Discovery, at the shell on my FIFO tree (3 regular files + `src/fixt/pipe.clj`, a real
named pipe): the shipped predicate lists 3, `pipe.clj` absent, `find` exits 0 immediately.

Through the operation:

```
UNBOUNDED: 3 records [p0.clj p1.clj p2.clj]   elapsed 26 ms
paged n=2: page1 SERVED [p0 p1]; page2 SERVED [p2]; DONE
```

Round six produced NO output on this tree at all and survived SIGTERM.

The check-to-use window — a pinned REGULAR file replaced by a FIFO between pin and page,
which discovery cannot close — refuses rather than blocking:

```
page2 -> :unconfined-manifest-row "src/fixt/m7.clj"     2 ms
```

`ls_tree_snapshot.clj:198` (`content-digest`) and `core.clj:396` (`safe-outline`) both
pre-check `Files/isRegularFile` BEFORE the open, which is the only shape that works: a
blocking `open(2)` never throws, so `catch Exception` could never have made it typed.
**Closed.**

---

## Part 2 — the builder's two refusals of the brief

### Refusal 1 — NOFOLLOW on `isRegularFile` — **the builder is RIGHT**

Witness, my tree `rD`: `src/fixt/zlinked.clj -> /tmp/.../outside/target.clj`, a symlink to a
regular file whose target is OUTSIDE the root.

```
Files/isRegularFile(zlinked, NOFOLLOW_LINKS) -> false
unbounded scan: [k0.clj k1.clj k2.clj zlinked.clj]
zlinked record: {:file "src/fixt/zlinked.clj", :ns real.target, :form-count 1}
paged n=2:  p1 [k0 k1]   p2 [k2 zlinked]
```

Discovery admits it on purpose (`-type l -xtype f`), the unbounded scan outlines it, and the
paged walk serves it identically. Had the leaf check used NOFOLLOW on the regularity test it
would have returned `false` for exactly this row and refused on page 2 what page 1 served —
the page-1/page-2 divergence the whole design exists to avoid, and the same shape of
regression as round six's. The asymmetry (NOFOLLOW on EXISTENCE, follow on REGULARITY) is
not a compromise: it is the exact negation of the discovery predicate, and each half is
load-bearing in a different direction.

### Refusal 2 — dropping the leaf check — **the builder is RIGHT**

Round six's own "cheaper repair" was to drop the check and restore round-five behaviour. My
attacks show what that would cost. With the check in place, every non-regular leaf reached
through the check-to-use window refuses in 1-2 ms and names the row:

```
FIFO swapped in     -> :unconfined-manifest-row   2 ms
DIRECTORY swapped in-> :unconfined-manifest-row   (message now TRUE)
socket swapped in   -> :unconfined-manifest-row   2 ms
symlink LOOP a->b->a-> :unconfined-manifest-row   1 ms
symlink FILE -> DIR  -> :unconfined-manifest-row  (attack (a), below)
```

Without it, each of those becomes either a wasted page slot carrying a typed error record
(round five's behaviour) or a `:stale-result-cursor` that attributes a substitution to
byte drift. Neither is an escape, so this is a quality argument rather than a security one —
but the check is what makes "the paged walk equals the unbounded scan record for record"
hold as an invariant instead of a coincidence, and that invariant is the one whose absence
let the round-six regression ship. Closing the class is worth the two lines.

---

## Part 3 — six attacks on round seven

### (a) a symlink resolving to a FILE at discovery and to a DIRECTORY at page time — **REFUSED**

`src/fixt/zswap.clj -> outside/target.clj` discovered and pinned on page 1 (11 candidates),
then repointed to `outside/adir` before page 2:

```
page1: SERVED [m0..m5]  cursor=true
page2: {:error-type :unconfined-manifest-row,
        :error "pinned manifest row \"src/fixt/zswap.clj\" does not resolve to a source
                file inside the scanned root"}
```

`-xtype f` follows the link at DISCOVERY time and the leaf check follows it again at PAGE
time, so the two disagree exactly when the tree changed under them — which is the correct
outcome for a genuine tamper, refused rather than stale, in 2 ms, with the row named.

### (b) a symlink LOOP `a -> b -> a` named `*.clj` — **excluded, no hang, no throw**

```
find (shipped predicate) on the loop tree: 10 paths, a.clj and b.clj absent, exit 0
operation: UNBOUNDED 10 records; paged walk n=4 finishes clean
loop pinned by check-to-use swap -> :unconfined-manifest-row   1 ms
```

`-xtype f` resolves ELOOP to "not f"; `Files.isRegularFile` swallows the `IOException` and
returns false. Neither `find`, the pin, nor the page hangs or throws.

### (c) a filename containing a NEWLINE — **THE ONE ITEM. Untyped throw out of the operation.**

`find` prints one path per line and `core.clj:255` parses it with `str/split-lines`, so a
file named `we\nird.clj` becomes TWO candidates: an absolute path `.../src/fixt/we` and a
bare relative `ird.clj`.

```
find | wc -l  ->  12   for 11 actual files
operation     ->  java.lang.IllegalArgumentException: 'other' is different type of Path
                  at src/clj_surgeon/core.clj:565  (fs/relativize root-path (fs/path f))
```

The scan does not return a receipt at all — it throws, on the UNBOUNDED path, before any
page. Nothing escapes the root (the crash precedes row construction, and the phantom is a
relative fragment, not an out-of-root path), so this is availability and honesty, not
confinement. Two things make it worth fixing before merge rather than filing:

1. **It is the same failure class the falsifier table already cites as fixed.**
   `read-path-memory-specs.md:78` names, as a defeated defect, an absolute row that "threw
   `IllegalArgumentException: ... is not a relative path` out of an operation whose promise
   is a typed receipt." This is that sentence again, one line further up the pipeline.
2. **It falsifies the sentence this round was written to make true.** The new EARS clause
   (`specs.md:21`) says discovery "shall DISCOVER as candidates exactly the regular files
   and the symlinks that resolve to regular files"; `core.clj:210-213` says "A candidate is
   a REGULAR FILE, or a SYMLINK THAT RESOLVES TO ONE — nothing else." `ird.clj` is neither.
   Round six's blocker was precisely a docstring asserting a discovery property the code did
   not enforce; the new assertion is stronger and still has one hole.

**PRE-EXISTING**: `git show 3cedd44:src/clj_surgeon/core.clj` has the identical
`str/split-lines` parse and the identical `fs/relativize` at `:491`. Round seven did not
introduce it and did not worsen it — it merely made a claim that this case contradicts.

**Fix**, one line, in the function this round already rewrote: `-print0` and split on `\0`
(or, weaker, drop any produced line that is not `str/starts-with?` the scanned dir). Either
makes the EARS clause true as written.

**Leading `-` in the filename: survives.** `src/fixt/-lead.clj` is listed, outlined and
paged normally — the argv is a vector, never a shell string, and the path `find` prints is
absolute, so no token is ever read as an option.

```
UNBOUNDED: [-lead.clj m0..m9]  (11 records)
paged n=4: [-lead m0 m1 m2] / [m3..m6] / [m7 m8 m9]  DONE
```

### (d) a socket named `*.clj` — **not discovered; refused, not hung, when pinned**

```
find (shipped): sock.clj absent; operation serves the 10 real files
socket pinned by check-to-use swap -> :unconfined-manifest-row   2 ms
```

A device node was not creatable without root on this box; the socket exercises the same
`isRegularFile` branch, so the class is covered.

### (e) `find` on a root that is ITSELF a symlink — **consistent, but the op `System/exit`s**

```
find <symlink-to-project> ...     -> 0 paths     (find -P never descends the arg)
find <symlink>/src ...            -> 10 paths    (a symlink COMPONENT is fine)
row-file "<symlink-root>" "src/fixt/m0.clj" -> "<symlink-root>/src/fixt/m0.clj"  (accepted)
row-file "<real-root>"    "src/fixt/m0.clj" -> "<real-root>/src/fixt/m0.clj"     (accepted)
```

Confinement real-paths both the base and the anchor, so it accepts a symlinked root and a
real root identically — **consistent**. And since discovery returns nothing for such a root,
no snapshot is ever pinned there, so the two can never disagree. Pre-existing and unchanged
by this round. One observation worth a follow-up, not a blocker and outside the diff: the
empty-scan branch at `core.clj:874-875` prints a message and calls `(System/exit 1)` from
inside the library operation, so `ls-tree` on a symlinked root terminates the process
instead of returning a typed receipt.

### (f) is the discovery predicate stated in the EARS text exactly as shipped?

**Yes, at both levels, with the caveat in (c).** Quoting both:

Shipped, `src/clj_surgeon/core.clj:246-253`:

```
find <dir> ( -name '*.clj' -o -name '*.cljs' -o -name '*.cljc' )
           ( -type f -o ( -type l -xtype f ) )
```

EARS, `docs/intent/read-path-memory/read-path-memory-specs.md:21`:

> and it shall DISCOVER as candidates exactly the regular files and the symlinks that
> resolve to regular files whose names match `*.clj`, `*.cljs` or `*.cljc` — never a
> directory, a dangling symlink, a FIFO or a socket — and shall refuse by name, without
> opening it, a source that is not a regular file at read time.

The EARS states the semantics (correct for a requirement); the falsifier row
`MCP-OP-MEM-003 (discovery predicate)` at `specs.md:79` quotes the literal flags
`( -type f -o ( -type l -xtype f ) )` together with the 13-vs-12 measurement. `row-file`'s
docstring (`ls_tree_snapshot.clj:617-620`) quotes the command verbatim rather than
paraphrasing it, which is the correct remedy for round six's blocker. The only word that
does not hold is **"exactly"**, and only for the newline case in (c).

---

## Gates — each run once, JVM suites under `/home/forge/bin/suite-run`

```
$ /home/forge/bin/suite-run bb test/run_all.clj
Ran 784 tests containing 6352 assertions.
0 failures, 0 errors.                                   TESTFAST_EXIT=0

$ /home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
Ran 389 tests containing 3988 assertions.
0 failures, 0 errors.                                   MCPTEST_EXIT=0

$ swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
                                                        ORACLE_EXIT=0

$ /home/forge/bin/suite-run bb bench/memory_battery/generate_tree.clj --self-test
generate_tree verification self-test: ok
generate_tree self-test: ok
$ /home/forge/bin/suite-run bb -e "(require 'clj-surgeon.memory-battery-test ...)"
Ran 24 tests containing 138 assertions.
0 failures, 0 errors.                                   BATTERY_EXIT=0
```

All four reproduce the builder's stated figures exactly (784/6352/0, 389/3988/0, oracle
pass, 24/138/0). The memory battery itself was NOT run. Nothing committed, stashed or
pushed; worktree clean at `6625b7d`.

---

## Verdict

**GO-WITH-FIX** for the mayor's merge queue.

1. **CLOSED — round-six blockers 1 and 2, the missing `-type` predicate.**
   `src/clj_surgeon/core.clj:246-253`. Witness: on MY untampered 13-path tree (12 files plus
   a DIRECTORY `src/mydir.clj`) the shipped predicate lists 12 where the old listed 13, and
   the paged walk now serves `page3 [mod10 mod11]` and equals the unbounded scan record for
   record, where round six refused that page and lost both files.
2. **CLOSED — round-six blocker 3, a witness that reaches the refusal through DISCOVERY.**
   `test/clj_surgeon/ls_tree_budget_test.clj:1418`, `:1458`, `:1490`. Witness: `:1418` builds
   the directory with `fs/create-dirs` and walks three real pages with no `repin-row!`;
   `:1458` mutates the TREE with byte-identical content so staleness cannot be what refuses;
   I re-derived the same parity independently.
3. **CLOSED — round-six follow-up 8, the dangling-symlink leaf.**
   `src/clj_surgeon/ls_tree_snapshot.clj:660-666`. Witness: a discovered, pinned
   `src/fixt/zdang.clj` repointed to a nonexistent target refuses
   `:unconfined-manifest-row` naming the row, where round six spent a page slot on a typed
   error record — while a legitimately deleted file in the same position still reaches
   `:stale-result-cursor ... :observed nil`.
4. **CLOSED — round-six follow-up 9, the FIFO hang, at three layers.**
   `src/clj_surgeon/core.clj:246-253`, `src/clj_surgeon/ls_tree_snapshot.clj:198`,
   `src/clj_surgeon/core.clj:396`. Witness: my FIFO tree scans in 26 ms and pages to
   completion where round six produced no output and survived SIGTERM; a pinned regular file
   replaced by a FIFO before the page refuses `:unconfined-manifest-row` in 2 ms.
5. **The NOFOLLOW refusal is RIGHT.** `src/clj_surgeon/ls_tree_snapshot.clj:663-665`.
   Witness: `Files/isRegularFile(src/fixt/zlinked.clj, NOFOLLOW_LINKS)` is `false` for a
   symlink to a regular file that discovery admits, outlines as `real.target`, and pages
   identically at `p2 [k2.clj zlinked.clj]` — NOFOLLOW there would refuse on page 2 what
   page 1 served.
6. **Refusing to drop the leaf check is RIGHT.** `src/clj_surgeon/ls_tree_snapshot.clj:660`.
   Witness: FIFO, directory, socket, symlink-loop and file-to-directory swaps all refuse
   `:unconfined-manifest-row` in 1-2 ms naming the row; without the check each degrades to a
   wasted page slot or a staleness receipt that misattributes a substitution to byte drift,
   and paged-equals-unbounded stops being an invariant.
7. **THE FIX — `src/clj_surgeon/core.clj:255` parses `find` output with `str/split-lines`.**
   Witness: a file named `we\nird.clj` yields 12 lines for 11 files, and the scan throws
   `IllegalArgumentException: 'other' is different type of Path` at `core.clj:565` instead of
   returning a receipt — the same class `specs.md:78` already cites as defeated, and a
   counterexample to `specs.md:21` / `core.clj:210-213` ("exactly the regular files and the
   symlinks that resolve to regular files ... nothing else"). PRE-EXISTING and identical at
   `3cedd44`. One line: `-print0` and split on `\0`.
8. **Attacks (a), (b), (d) all behave.** `src/clj_surgeon/ls_tree_snapshot.clj:660-666`.
   Witness: symlink FILE→DIRECTORY between pin and page → `:unconfined-manifest-row` naming
   `src/fixt/zswap.clj`; symlink loop `a->b->a` excluded by `-xtype f` with `find` exit 0 and
   refused in 1 ms when pinned; a socket named `*.clj` not discovered and refused in 2 ms
   when pinned. A leading `-` in a filename survives — argv tokens, absolute paths.
9. **Attack (e) is consistent; one pre-existing observation, outside the diff.**
   `src/clj_surgeon/core.clj:874-875`. Witness: `find` on a root that IS a symlink lists 0
   paths (so no snapshot is ever pinned there and discovery/confinement cannot disagree),
   while `row-file` real-paths base and anchor and accepts symlinked and real roots
   identically — but the empty-scan branch calls `(System/exit 1)` from inside the library
   operation instead of returning a typed receipt.
10. **Gates green and matching the builder:** 784/6352/0, 389/3988/0, oracle pass, 24/138/0.
    Memory battery not run. Nothing committed, stashed or pushed; clean at `6625b7d`.
