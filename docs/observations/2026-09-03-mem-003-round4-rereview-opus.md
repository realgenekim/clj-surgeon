# MEM-003 round 4 — Opus executed review of bridge/streaming-ls-tree at 281e13b (2026-09-03T12:04Z)

Verdict: **GO-WITH-FIX**, HELD for round 5. All six round-three items CLOSED on the reviewer's own re-runs. Two residuals, both state-root-write class: (1) verification and the slice read are two opens of one mutable file — 400 page-2 reads under a live rows swap: 223 refused, 88 correct, **89 SERVED-WRONG** with a valid cursor and full receipt; (2) a symlinked DIRECTORY component defeats the lexical confinement (row `src/linkdir/secret.clj` served `leaked.secret`); (3) specs/design promise `resolve` while the code delivers `lexical`; (4) docs name the race only in the harmless direction. Round 5 launched.

## Opus verdict, verbatim

# streaming-ls-tree 281e13b (MEM-003) — round-FOUR executed review

Independent executed review of `bridge/streaming-ls-tree` at **281e13b**, worktree
`/home/forge/tmp/sol/mem003r4-wt` (`git rev-parse --short HEAD` = `281e13b`). Every claim below
was re-run with my own harness (`bb --classpath src`, fixtures and state roots confined to
`/tmp/mem003r4-sol-fx`, `CLJ_SURGEON_STATE_ROOT` bound per run). Nothing was committed, stashed
or pushed. The memory battery was NOT run (exclusive lock, another lane).

**All six of my round-three FIX items are CLOSED, each against my own re-run of my own
round-three reproduction.** The single new finding is a residual of item 1: verification and the
slice read are **two separate opens of the same mutable file**, so the exact round-three wrong
page — `[m06 m01 m08 m09 m10]` — is still reachable, no longer by a persistent tamper but by
winning the window between them. I measured **89 wrong pages in 400 reads** on a real filesystem
with no interposition. Same access class as before (write access to the state root), which is why
this is GO-WITH-FIX and not NO-GO.

---

## Part 1 — my six round-three items, re-run

### Item 1 — the serve path trusts a snapshot it never verifies — **CLOSED**

[core.clj:861](src/clj_surgeon/core.clj#L861) now resolves with
`snapshot/verified-snapshot` ([ls_tree_snapshot.clj:330](src/clj_surgeon/ls_tree_snapshot.clj#L330)),
made public for exactly this. My round-three reproduction re-run verbatim
(`bb --classpath src /tmp/mem003r4-sol-fx/h/a1_serve_verify.clj`):

```
page1 -> :served [fixt.m01 fixt.m02 fixt.m03 fixt.m04 fixt.m05]
page2 BEFORE tamper: {:kind :served, :n 5,
 :ns [fixt.m06 fixt.m07 fixt.m08 fixt.m09 fixt.m10], ...}
row6 BEFORE: {:i 6, :x 0, :p "src/fixt/m07.clj", :h "b3273d36..."}
row6 AFTER : {:i 6, :x 0, :p "src/fixt/m01.clj", :h "e836c45f..."}
rows still re-fold to the id? false  (folded: 93fee0a0 ... n= 12 )
page2 AFTER tamper (unchanged cursor):
{:kind :refusal,
 :error-type :unknown-result-cursor,
 :error "no pinned manifest for this cursor under this root: ..."}
```

Round three served `[fixt.m06 fixt.m01 fixt.m08 fixt.m09 fixt.m10]` here. It now refuses, typed,
and `mod001`/`m01` is never encoded.

### Item 2 — the continuation receipt is asserted from arithmetic — **CLOSED**

Two mechanisms, and they are not equally load-bearing. `:returned` is now taken from the
encoder's emission count ([core.clj:759](src/clj_surgeon/core.clj#L759) `encode-page` takes a
`receipt-fn`; [core.clj:912](src/clj_surgeon/core.clj#L912) and
[core.clj:825](src/clj_surgeon/core.clj#L825) supply it), and a slice the manifest cannot supply
refuses at [core.clj:888](src/clj_surgeon/core.clj#L888).

My round-three reproductions, re-run (`h/a2_receipt.clj`):

```
=== A. round-3 repro: rows TRUNCATED 12 -> 7 ===
page2 (offset 5) on truncated rows:
{:kind :refusal, :error-type :unknown-result-cursor, ...}
=== B. round-3 repro: rows file ABSENT ===
page2 with rows file deleted:
{:kind :refusal, :error-type :unknown-result-cursor, ...}
```

Round three encoded 2 records under `:returned 5, :remaining 2` and 0 records under `:returned 5`
with a next cursor. Both are gone.

**Honest note on which half did the work.** `stream-outlines!`
([core.clj:520](src/clj_surgeon/core.clj#L520)) calls `consume!` exactly once per candidate —
refused files included, since both encoders' `emit!` append unconditionally
([core.clj:601](src/clj_surgeon/core.clj#L601), [core.clj:643](src/clj_surgeon/core.clj#L643)) —
so `encoded` is identically `(count rows)`, and `(count rows)` is identically `slice` once the
guard at :888 holds. **The measurement cannot disagree with the arithmetic it replaced.** The
refusal at :888 is the fix; the `receipt-fn` is a belt on top of braces. That is a fine thing to
ship — it removes the *class* — but the ratchet that would catch a regression is the one at :888.

### Items 3 + 4 — `..` and absolute manifest rows — **CLOSED**

One resolver now serves both the check and the read
([ls_tree_snapshot.clj:462](src/clj_surgeon/ls_tree_snapshot.clj#L462) `row-file`, used by
`stale-row` :504 and by `pinned-candidates` [core.clj:745](src/clj_surgeon/core.clj#L745)), with a
new typed refusal at [result_budget.clj:281](src/clj_surgeon/result_budget.clj#L281). My
round-three reproductions A and B, re-run inside a snapshot **re-folded and re-filed so it passes
verification** (`h/a3_confine.clj`):

```
=== a1 relative .. escape ===
  row6 path: "../OUTSIDE/secret.clj"
{:kind :refusal, :error-type :unconfined-manifest-row,
 :error "pinned manifest row \"../OUTSIDE/secret.clj\" is not inside the scanned root",
 :limit {:kind :manifest-row, :requested "../OUTSIDE/secret.clj"}}

=== a2 ABSOLUTE path ===
  row6 path: "/tmp/mem003r4-sol-fx/OUTSIDE/secret.clj"
{:kind :refusal, :error-type :unconfined-manifest-row,
 :error "pinned manifest row \"/tmp/.../OUTSIDE/secret.clj\" is not inside the scanned root", ...}
```

Round three encoded `leaked.secret` for A and threw `IllegalArgumentException` for B. Both are now
typed refusals that NAME the path. I also swept the classic edge cases (`h/f_edge.clj`) — the
sibling-prefix escape is correctly refused, so `Path/startsWith` is doing component-wise work and
not string-prefix work:

```
"../r1extra/secret.clj"                    -> ""      (refused)
""                                         -> ""      (refused)
"."                                        -> ""      (refused)
"src/fixt/../../../r1extra/secret.clj"     -> ""      (refused)
"src/./fixt/m01.clj"                       -> "/tmp/mem003r4-sol-fx/r1/src/fixt/m01.clj"
"src/fixt/m01.clj"                         -> "/tmp/mem003r4-sol-fx/r1/src/fixt/m01.clj"
non-string row :p 42 -> unconfined-row says: {:path 42}
nil row :p           -> unconfined-row says: {:path nil}
```

### Item 5 — `:unknown-result-cursor` documented a case it could not reach — **CLOSED**

The canonical root now seeds the manifest digest
([ls_tree_snapshot.clj:202](src/clj_surgeon/ls_tree_snapshot.clj#L202) `digest-header`,
`manifest-version` bumped to 2 at :187). My round-three twin reproduction, re-run
(`h/d_twin.clj`), on two trees `diff -r` proves byte-identical:

```
TWINS ARE BYTE-IDENTICAL
t1 digest: 222e4ac60c835c6143dbdca74534ee487850bd3da65e7815081b46ac199d5efa
t2 digest: 08f98c497b78be03516fa855d7883dc3d3c8e0186d2d678bc3a8812b38e869d1
same manifest digest? false

t1's cursor presented against t2:
{:kind :refusal, :error-type :unknown-result-cursor, ...}
```

Round three got `:invalid-result-cursor` — the forgery receipt about a token the server had
minted. The receipt is now true.

The 281e13b ratchet holds in the same run — a root spelled with `..` addresses and pages
identically:

```
root as given : /tmp/mem003r4-sol-fx/t1/src/..
digest via `..`: 222e4ac60c835c6143dbdca74534ee487850bd3da65e7815081b46ac199d5efa
identical to the plainly-spelled root's digest? true
t1's cursor served under the `..`-spelled root -> [fixt.m06 fixt.m07 fixt.m08 fixt.m09 fixt.m10]
```

### Item 6 — the determinism claim was unqualified — **CLOSED**

[ls_tree_snapshot.clj:52](src/clj_surgeon/ls_tree_snapshot.clj#L52) now reads "IDENTICALLY
**WITHIN ONE WARM SNAPSHOT STORE**", with sixteen lines under it explaining why the two halves of
the token have opposite requirements; the design doc carries the same qualification at
`read-path-memory-design.md:388`, one line under the table row that states the property. My
round-three witness, re-run over a 300-file tree, three warm processes and two cold stores:

```
bytes: 7455
warm sha: 4bfe3ba6f9d53a8f2e47 / 4bfe3ba6f9d53a8f2e47 / 4bfe3ba6f9d53a8f2e47
warm1==warm2==warm3 ?  IDENTICAL
cold1 vs cold2 diff:
403c403
<    ... :cursor a99b7d5e...:100:3ffc13eb3d8a6897430976e19be9e96fdbf7598c73655910bb1c479e6f67f392
>    ... :cursor a99b7d5e...:100:34f841f2207f5ab5635d1a265c7d539fddd028dd049f56246d21ea814c4594dd
diff line count: 2
snapshots in warm store: 2
```

Identical digest, different MAC, exactly one line apart. The doc now says precisely that.

---

## Part 2 — attacking round four

### (a) Lexical confinement vs a symlinked DIRECTORY component — **a real escape, tamper-only, and the requirement text overclaims**

**First, the builder's premise is accurate, and I re-measured it.** `find-clj-files`
([core.clj:208](src/clj_surgeon/core.clj#L208)) shells out to plain `find` with no `-L`. A root
containing BOTH a symlinked `.clj` file and a symlinked directory, scanned fresh
(`h/a5_discovery.clj`):

```
--- raw find (what discovery uses) ---
/tmp/mem003r4-sol-fx/r2/src/fixt/linkfile.clj      <- symlinked FILE, listed
/tmp/mem003r4-sol-fx/r2/src/fixt/m1.clj ... m6.clj
                                                    <- src/linkdir/* absent: find does NOT descend
FRESH SCAN:
{:kind :served, :n 7,
 :ns [leaked.secret fixt.m1 fixt.m2 fixt.m3 fixt.m4 fixt.m5 fixt.m6],
 :files ["src/fixt/linkfile.clj" "src/fixt/m1.clj" ... ]}
```

So the two halves of the builder's argument are both true: discovery **does** encode a symlinked
`.clj` file whose target is outside the root (a realpath guard would refuse on page 2 what page 1
encodes — the divergence the builder correctly refused to introduce), and discovery **does not**
descend a symlinked directory.

**But that second half is exactly the hole.** A row with a symlinked directory component is a
shape discovery can never produce — the same "corruption/tamper-only" class items 3 and 4 were
just fixed for — and the lexical guard passes it. Same harness, same re-folded-and-re-filed
snapshot that PASSES verification (`h/a3_confine.clj`):

```
=== a4 SYMLINKED DIR component: src/linkdir/secret.clj ===
  row6 path: "src/linkdir/secret.clj"
{:kind :served,
 :n 5,
 :ns [fixt.m06 leaked.secret fixt.m08 fixt.m09 fixt.m10],
 :files ["src/fixt/m06.clj" "src/linkdir/secret.clj" "src/fixt/m08.clj" ...]}
```

`src/linkdir -> /tmp/mem003r4-sol-fx/OUTSIDE`. **The serve path read and encoded a file outside
the root, under a valid cursor, with no refusal** — round three's item-3 outcome, reached through
a different spelling.

**Is it a pre-existing hole the LID should name, or a confinement failure this round owns?**
It is the second, on the branch's own words. The EARS requirement written this round
(`read-path-memory-specs.md:21`) says the operation shall refuse "**a manifest row that does not
resolve inside the scanned root**", and the design table (`read-path-memory-design.md:299`) says
"a row this page would serve **names a path outside the scanned root**". `src/linkdir/secret.clj`
satisfies both descriptions and is served. Only `row-file`'s own docstring
([ls_tree_snapshot.clj:462](src/clj_surgeon/ls_tree_snapshot.clj#L462)) says the boundary is
lexical, and the pinning witness
(`a-symlinked-file-inside-the-root-pages-exactly-as-it-is-discovered`, test:1099) pins the FILE
case, not the DIRECTORY case. **The requirement is falsified by a row the implementation accepts.**

The fix is cheap and does not reintroduce the divergence the builder avoided: **resolve the PARENT
and leave the final component lexical.** That is precisely the shape of what discovery can produce
— `find` lists a symlinked file but never descends a symlinked directory — so
`src/fixt/linkfile.clj` (parent `src/fixt`, real) still pages exactly as it scans, while
`src/linkdir/secret.clj` (parent resolves outside) refuses. One `.toRealPath()` on the parent,
and the existing symlink witness stays green.

### (b) TOCTOU between the verifying fold and the slice read — **the count direction is closed; the SUBSTITUTION direction is OPEN**

Interposing on `read-rows` ([ls_tree_snapshot.clj:449](src/clj_surgeon/ls_tree_snapshot.clj#L449))
after `verified-snapshot` has passed (`h/b_toctou.clj`):

```
sanity, untouched:
{:kind :served, :n 5, :ns [fixt.m06 fixt.m07 fixt.m08 fixt.m09 fixt.m10],
 :receipt {:limit 5, :offset 5, :returned 5, :total 12, :remaining 2, ...}, :next "...:10:a0d368cd..."}
--- b1: read-rows returns slice-1 (4 of 5) AFTER verification ---
{:kind :refusal, :error-type :unknown-result-cursor}
--- b1b: read-rows returns 1 of 5 ---
{:kind :refusal, :error-type :unknown-result-cursor}
--- b3: read-rows returns slice+1 (6 of 5) ---
{:kind :refusal, :error-type :unknown-result-cursor}
--- b2: exactly slice rows, row 1 substituted with a REAL (path,hash) pair ---
{:kind :served, :n 5, :ns [fixt.m06 fixt.m01 fixt.m08 fixt.m09 fixt.m10],
 :receipt {:limit 5, :offset 5, :returned 5, :total 12, :remaining 2, ...}}
```

The measured `:returned` and the guard at :888 do everything the builder claims — short slices and
long slices both refuse, and `:returned` always equals the record count beside it. **But the guard
counts rows; it does not identify them.** A slice of the right LENGTH whose rows are
self-consistent `(path, hash)` pairs is served, and it is the exact round-three wrong page.

That is not only an interposition artifact. **On a real filesystem, no `with-redefs`, no
`alter-var-root`** — a swapper thread renaming a substituted rows file (same 200 rows, row 6
carrying row 0's real path and real hash) into place while 400 page-2 reads run
(`h/b2_race.clj`, 200-file corpus):

```
rows total: 200
TALLY over 400 page-2 reads under a live rows-file swap:
{"REFUSE:unknown-result-cursor" 223,
 "SERVED-correct" 88,
 "SERVED-WRONG [fixt.m006 fixt.m001 fixt.m008 fixt.m009 fixt.m010]" 89}
```

**89 of 400 reads served a substituted candidate under a valid cursor with a full receipt.**

The cause is structural, and it is worth stating precisely because the branch's own docstrings
argue the opposite: `verified-snapshot` opens the rows file and streams it whole
([ls_tree_snapshot.clj:298](src/clj_surgeon/ls_tree_snapshot.clj#L298) `rows-digest`), then
`read-rows` ([:449](src/clj_surgeon/ls_tree_snapshot.clj#L449)) **opens it again**. Verification
and use are two observations of one mutable object, so the window is not a hairline — it is the
whole fold, and **the fold is O(N) in the manifest**, so the window GROWS with corpus size. At
N=10,000 it is the dominant cost of the page.

Neither the design doc nor the code names this. [core.clj:885](src/clj_surgeon/core.clj#L885) and
the witness at test:952 both describe the race in the "cannot supply the slice" (count) direction
only — "it is the only way a verified snapshot can hand the encoder fewer rows than the page
promised" is true, and *fewer* is not the dangerous direction; *different* is.

The structural fix is one pass: have `read-rows` fold the whole file while extracting the slice
and return `[rows digest count]`, and compare the digest to the cursor id there. One open, one
observation, no window. That also deletes the :888 special case, because a short file cannot
produce a matching fold.

### (c) A v1-minted cursor presented under manifest-version 2 — **typed refusal, no throw**

I laid down a genuine legacy snapshot: rows folded under the v1 header
(`"clj-surgeon/ls-tree-manifest/v1\n"`, no root line), filed under that v1 id, with a
`{:v 1 ...}` meta carrying a real secret, and minted a correctly-MAC'd cursor for it
(`h/c_v1.clj`):

```
v2 (current) id: 5872c03671a447018cca10dc3753d04eb67bee21709e138fdf4eb9f5f7486e6e
v1 (legacy) id : bc6aa621222c2930006e10071115aeb80afb5ac6b306fd675dce95b4be9321cd  same as v2? false
laid down v1 snapshot; files: (5872c036....edn 5872c036....rows bc6aa621....edn bc6aa621....rows)
v1 cursor: bc6aa621...:5:29c90e6247d5b10be6870bed9f645aaac955b3b863f878fd0819882eeb61626f
RESULT of presenting a v1-minted cursor under manifest-version 2:
{:kind :refusal, :error-type :unknown-result-cursor, :error "no pinned manifest for this cursor ..."}

sanity: does the CURRENT v2 cursor still serve?  -> [fixt.m06 fixt.m07 fixt.m08 fixt.m09 fixt.m10]
```

Refused twice over — `(= manifest-version (:v m))` at
[ls_tree_snapshot.clj:340](src/clj_surgeon/ls_tree_snapshot.clj#L340), and the v2-header refold
that can no longer reach a v1 id. No throw, no read, and the co-resident v2 snapshot is unaffected.
The migration story in `manifest-version`'s docstring ("every v1 snapshot becomes unaddressable at
once and ages out under the TTL") is exactly what happens.

### (d) Can a page's `:returned` and the next cursor's offset disagree? — **no, by construction**

Both are the same value. [core.clj:912](src/clj_surgeon/core.clj#L912) closes over one `encoded`
and uses it for `:returned` and for `(+ offset encoded)`; `:remaining` is
`(max 0 (- total (+ offset returned)))` at
[result_budget.clj:182](src/clj_surgeon/result_budget.clj#L182), derived from the same number.
There is no second arithmetic path left to drift. Confirmed in every served page above
(`:returned 5` / next offset `10` at offset 5; `:remaining 2` with `:total 12`), including the
b2 substitution page where the rows were wrong but the counting was not.

The one thing worth flagging is the reverse: because `over?` is computed from
`(- total offset)` and NOT from `encoded`, a page that encoded FEWER records than its slice would
emit a next cursor at `offset + encoded` — correct, never a skip — but a page that encoded ZERO
would emit a next cursor at its own offset and loop forever. That is unreachable today (the :888
guard plus one `emit!` per candidate), and it is unreachable only because of those two facts
together. Worth an assertion if :888 is ever refactored.

### (e) Is `:unconfined-manifest-row` in the published vocabulary? — **yes, in all four places**

Not a fifth name only the code knows:

- the receipt fn, [result_budget.clj:281](src/clj_surgeon/result_budget.clj#L281), with `:limit`,
  `:complete`, `:source-unchanged`, `:remedy`, `:next_call` in the same shape as its four siblings;
- the design doc's five-fact refusal table, `read-path-memory-design.md:299`;
- the EARS requirement, `read-path-memory-specs.md:21` ("a manifest row that does not resolve
  inside the scanned root");
- the falsifier table, `read-path-memory-specs.md:55`, with the `..` row, the absolute row, and the
  symlink case named;
- plus `run-pinned-page`'s docstring, [core.clj:834](src/clj_surgeon/core.clj#L834), which counts
  five refusals and explains why this one states a fact about the MANIFEST rather than the cursor.

There is no machine-readable error-type enum to add it to: `:ls-tree` is a CLI operation with no
MCP tool surface (`grep -rn ":ls-tree" src/` finds only `core.clj`, the receipt's `:next_call`
builder, and the battery runner's note), so the receipt map and these documents ARE the published
vocabulary. Consistent with the other four names. **The wording is the problem, not the coverage**
— see (a): the vocabulary entry promises `resolve`, and the implementation delivers `lexical`.

### Nit, not an item

`unconfined-row`'s docstring ([ls_tree_snapshot.clj:495](src/clj_surgeon/ls_tree_snapshot.clj#L495))
says the refusal "costs no read at all". The unconfined row itself is indeed never opened
(`row-file` returns nil before any `content-digest`), but `stale` is a sibling `let` binding at
[core.clj:881](src/clj_surgeon/core.clj#L881) and is evaluated regardless, so the confined rows
BEFORE the offending one are still digested. The security claim is right; the cost claim is not.

---

## Gates — ran-lines verbatim, each once, JVM suites under `/home/forge/bin/suite-run`

`/home/forge/bin/suite-run bb test/run_all.clj`:
```
Ran 772 tests containing 6300 assertions.
0 failures, 0 errors.
```

`/home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test`
(direct, never `make mcp-test`):
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

Battery self-test (`suite-run bb -e "(require 'clj-surgeon.memory-battery-test ...)"` and
`bb bench/memory_battery/generate_tree.clj --self-test`):
```
Ran 24 tests containing 138 assertions.
0 failures, 0 errors.
BATTERY_EXIT=0
generate_tree verification self-test: ok
generate_tree self-test: ok
GENTREE_EXIT=0
```

All four reproduce the builder's stated figures exactly (772/6300/0, 389/3988/0, oracle pass,
24/138/0). The memory battery itself was **not** run — exclusive lock, another lane.

---

## VERDICT: **GO-WITH-FIX** for the mayor's merge queue

Every one of my six round-three items is closed against my own re-run of my own reproduction, and
the branch is strictly better than 376d94a on every axis I tested. The two open items are
residuals of the same access class that made round three GO-WITH-FIX rather than NO-GO — write
access to `~/.local/state/clj-surgeon` — and neither is reachable from the tree, from an ordinary
caller, or from an honest concurrent scan. What keeps this from a clean GO is that item 1's CLASS
is narrowed rather than eliminated, and the branch's own documents say otherwise.

1. **FIX — verification and the slice read are TWO OPENS of one mutable file, and the exact
   round-three wrong page is still reachable through the window.**
   [ls_tree_snapshot.clj:298](src/clj_surgeon/ls_tree_snapshot.clj#L298) streams the rows file
   whole to check its address; [ls_tree_snapshot.clj:449](src/clj_surgeon/ls_tree_snapshot.clj#L449)
   then opens it again to take the slice. The guard at
   [core.clj:888](src/clj_surgeon/core.clj#L888) counts rows but does not identify them.
   Witness, real filesystem, no interposition, 400 page-2 reads under a live rows-file swap:
   `{"REFUSE:unknown-result-cursor" 223, "SERVED-correct" 88, "SERVED-WRONG [fixt.m006 fixt.m001
   fixt.m008 fixt.m009 fixt.m010]" 89}` — 89 pages with a substituted candidate, a valid cursor
   and a full receipt. The window is the whole fold, so it GROWS with N. Fix: fold while slicing
   in one open, return `[rows digest count]`, compare there — which also subsumes :888.

2. **FIX — lexical confinement is defeated by a symlinked DIRECTORY component; the serve path
   reads outside the root.** [ls_tree_snapshot.clj:462](src/clj_surgeon/ls_tree_snapshot.clj#L462)
   normalises lexically and deliberately does not resolve.
   Witness: row `src/linkdir/secret.clj` with `src/linkdir -> /tmp/.../OUTSIDE`, inside a snapshot
   re-folded and re-filed so it PASSES verification, served
   `:ns [fixt.m06 leaked.secret fixt.m08 fixt.m09 fixt.m10]`,
   `:files [... "src/linkdir/secret.clj" ...]`. Discovery can never produce that row (`find` with
   no `-L` does not descend a symlinked directory — measured), so it is the same tamper-only class
   items 3 and 4 were just fixed for. Fix that keeps the builder's correct concern intact: resolve
   the PARENT, leave the final component lexical — `src/fixt/linkfile.clj` still pages exactly as
   discovery encodes it, `src/linkdir/secret.clj` refuses, and
   `a-symlinked-file-inside-the-root-pages-exactly-as-it-is-discovered` stays green.

3. **FIX — the requirement and the refusal table promise `resolve`; the code delivers `lexical`.**
   `read-path-memory-specs.md:21` says the operation shall refuse "a manifest row that does not
   resolve inside the scanned root" and `read-path-memory-design.md:299` says "names a path outside
   the scanned root". The row in item 2 satisfies both descriptions and is served, so the EARS
   requirement is falsified by a row the implementation accepts. Take item 2's fix and the words
   become true; otherwise amend both documents to say LEXICAL and add a witness for the
   symlinked-directory row, so the gap is pinned rather than implied.

4. **FIX (documentation) — the fold/read race is named in the harmless direction only.**
   [core.clj:885](src/clj_surgeon/core.clj#L885) and the witness at
   `test/clj_surgeon/ls_tree_budget_test.clj:952` both describe it as "cannot supply the slice it
   promised", and the test comment calls the count case "the only way a verified snapshot can hand
   the encoder fewer rows than the page promised". *Fewer* is correct and is not the dangerous
   direction; *different* is, and nothing on the branch says so. Whatever is done about item 1,
   the serve-path integrity row in the specs must not read as though the substitution class is
   closed.

5. **NIT — `unconfined-row`'s "costs no read at all"**
   ([ls_tree_snapshot.clj:495](src/clj_surgeon/ls_tree_snapshot.clj#L495)). The offending row is
   never opened, but `stale` is a sibling `let` binding at
   [core.clj:881](src/clj_surgeon/core.clj#L881) and is evaluated regardless, so the confined rows
   before it are digested. Security claim right, cost claim wrong.

6. **PASS — item 1, the serve path verifies.**
   [core.clj:861](src/clj_surgeon/core.clj#L861) resolves with `verified-snapshot`.
   Witness: rows tampered so they no longer re-fold (`rows still re-fold to the id? false`), and
   the unchanged cursor now returns `{:kind :refusal, :error-type :unknown-result-cursor}` where
   round three served `[fixt.m06 fixt.m01 fixt.m08 fixt.m09 fixt.m10]`.

7. **PASS — item 2, `:returned` is measured and a short slice refuses.**
   [core.clj:759](src/clj_surgeon/core.clj#L759), [core.clj:888](src/clj_surgeon/core.clj#L888).
   Witness: truncated rows and an absent rows file both refuse `:unknown-result-cursor`; under
   interposition, slice−1, 1-of-5 and slice+1 all refuse. Noted honestly: `encoded` is identically
   `(count rows)` (one `emit!` per candidate), so :888 is the load-bearing half.

8. **PASS — items 3 and 4, one confined resolver at the check and the read.**
   [ls_tree_snapshot.clj:462](src/clj_surgeon/ls_tree_snapshot.clj#L462),
   [core.clj:745](src/clj_surgeon/core.clj#L745),
   [result_budget.clj:281](src/clj_surgeon/result_budget.clj#L281).
   Witness: `../OUTSIDE/secret.clj` and `/tmp/.../OUTSIDE/secret.clj` each refuse
   `:unconfined-manifest-row` NAMING the path, no throw, `leaked.secret` never encoded; the
   sibling-prefix escape `../r1extra/secret.clj` also refuses, so `Path/startsWith` is
   component-wise.

9. **PASS — item 5, the root is bound into the manifest address.**
   [ls_tree_snapshot.clj:202](src/clj_surgeon/ls_tree_snapshot.clj#L202),
   `manifest-version` 2 at :187.
   Witness: byte-identical twins now fold to `222e4ac6…` and `08f98c49…`, and t1's cursor against
   t2 refuses `:unknown-result-cursor` where round three said `:invalid-result-cursor`. The
   281e13b ratchet holds: a root spelled `…/src/..` gives the same digest and serves the same page.

10. **PASS — item 6, the determinism claim is narrowed and the narrowing is true.**
    [ls_tree_snapshot.clj:52](src/clj_surgeon/ls_tree_snapshot.clj#L52),
    `read-path-memory-design.md:388`.
    Witness: three warm processes sha `4bfe3ba6f9d53a8f2e47` ×3, IDENTICAL; two cold stores differ
    in exactly one line of 7,455 bytes, same digest `a99b7d5e…`, macs `3ffc13eb…` vs `34f841f2…`.

11. **PASS — a v1-minted cursor over a genuine v1 snapshot is a typed refusal, never a throw.**
    [ls_tree_snapshot.clj:340](src/clj_surgeon/ls_tree_snapshot.clj#L340).
    Witness: v1 id `bc6aa621…` laid down with real rows, real meta `{:v 1 …}` and a correct MAC →
    `:unknown-result-cursor`; the co-resident v2 cursor still serves
    `[fixt.m06 fixt.m07 fixt.m08 fixt.m09 fixt.m10]`.

12. **PASS — a page's `:returned` and the next cursor's offset cannot disagree.**
    [core.clj:912](src/clj_surgeon/core.clj#L912) closes over one `encoded` for both, and
    `:remaining` at [result_budget.clj:182](src/clj_surgeon/result_budget.clj#L182) derives from
    the same number. Only latent hazard: `over?` comes from `(- total offset)`, so a zero-record
    page would emit a next cursor at its own offset — unreachable today only because of :888 plus
    one `emit!` per candidate. Worth an assertion if :888 is refactored.

13. **PASS — `:unconfined-manifest-row` is published, not a code-only fifth name.**
    Receipt fn [result_budget.clj:281](src/clj_surgeon/result_budget.clj#L281) in the same shape as
    its four siblings; design refusal table `read-path-memory-design.md:299`; EARS requirement
    `read-path-memory-specs.md:21`; falsifier table `read-path-memory-specs.md:55`; and
    `run-pinned-page`'s docstring [core.clj:834](src/clj_surgeon/core.clj#L834). There is no
    machine-readable enum to add it to — `:ls-tree` has no MCP tool surface — so the receipt map
    and these documents ARE the vocabulary.

14. **PASS — gates reproduce the builder's figures exactly.** 772/6300/0, 389/3988/0, oracle pass,
    24/138/0, both generator self-tests ok. Battery not run (exclusive lock, another lane).
