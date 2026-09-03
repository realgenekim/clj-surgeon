# MEM-003 round 3 — Opus executed review of bridge/streaming-ls-tree at 376d94a (2026-09-03T11:17Z)

(Sol content filter refused the brief; Opus ran the same six-attack brief.) Verdict: **GO-WITH-FIX** — held out of the queue until round 4 closes items 1–6 (item 3 is a path-confinement hole reachable via a tampered rows file under the state root).

## Opus verdict, verbatim

# streaming-ls-tree 376d94a (MEM-003) — round-three executed review: GO-WITH-FIX

Independent executed review of `bridge/streaming-ls-tree` at `376d94a`, worktree
`/home/forge/tmp/sol/mem003r3-wt` (`git rev-parse HEAD` = `376d94aa26a6d701d03d2283e605f17470ffbeb7`).
Every attack in the brief was run with my own harness (bb, `--classpath src`, fixtures and state
roots confined to `/tmp/mem003r3-fx`). Nothing was committed, stashed or pushed.

**Both blockers from my round-two NO-GO are genuinely closed, with reproductions.** The cursor is
content-addressed, the MAC is unforgeable from published receipt material, and the concurrent-scan
caveat the builder declared is accurate. What remains open is one class: **the page-serving path
trusts the pinned snapshot without re-verifying it, and the continuation receipt is asserted from
arithmetic rather than measured from the page it describes.** Neither is reachable by an ordinary
caller, by any tree mutation, or by the race — they need write access to the local state root or a
damaged rows file. That is why this is GO-WITH-FIX and not NO-GO.

---

## Attack-by-attack

### (1) Forge a cursor from a receipt — **CLOSED**

`core.clj:822` checks the MAC against a per-snapshot secret that lives only inside the snapshot file
(`ls_tree_snapshot.clj:206`, secret minted at `ls_tree_snapshot.clj:357`). I built every MAC a
receipt holder can derive from published material — `sha256(id:off:)`, `sha256(id:off:id)`,
`sha256(id:off:nil)`, `sha256(id:off:total)`, `sha256(id:off:dir)`, `sha256(id||off)`,
`sha256(off:id)`, the id itself, all-zeros, and a replay of the legitimate MAC — across offsets
`0, 4, 5, 11, 12, 13, -1` and a 41-digit offset. **80 forged cursors, 80 typed
`:invalid-result-cursor` refusals, zero serves, zero throws.** The only cursor that served was the
genuine MAC at its own offset.

Reproduction: `bb --classpath src /tmp/mem003r3-fx/h/a1_forge.clj`

```
RECEIPT: manifest_digest= 2a3a746a... total= 12 offset= 0 returned= 5
cursor-id == published manifest_digest ? true
sanity: legit cursor serves -> SERVED n=5 first=fixt.m06
--- offset 5 ---
  sha256(id:off:<empty>)   -> REFUSE:invalid-result-cursor
  sha256(id:off:id)        -> REFUSE:invalid-result-cursor
  ...
  replay-legit-mac         -> SERVED n=5 first=fixt.m06
--- offset -1 ---   (all 10)  -> REFUSE:invalid-result-cursor
--- offset 999...9 (41 digits) (all 10) -> REFUSE:invalid-result-cursor
```

This closes round-two blocker 2 and round-two fix 3: the 41-digit offset returns a typed refusal
instead of `NumberFormatException` (`result_budget.clj:148`). `:result-cursor-out-of-range` is
reachable only with a genuine MAC and has a real witness at
`test/clj_surgeon/ls_tree_budget_test.clj:454`.

### (2) Stale under preserved stat — **CLOSED**

Byte swap with **identical size and identical mtime** (`touch -r` equivalent) on a page-2 file.

Reproduction: `bb --classpath src /tmp/mem003r3-fx/h/a2_stale.clj`

```
  size before/after = 51 / 51  EQUAL? true
  mtime before/after = 2026-09-03T11:09:38.874742506Z / ...874742506Z  EQUAL? true
  content changed? true
page2 AFTER byte-swap -> REFUSE:stale-result-cursor
  :error "src/fixt/m07.clj changed since this cursor was issued"
  :limit {:kind :pinned-content, :file "src/fixt/m07.clj", :requested 737f67de..., :observed 58bfb529...}
fresh-scan digest AFTER = d33b9824...   digest CHANGED? true
```

Round-two blocker 1 is closed: identity is content (`ls_tree_snapshot.clj:177-186`, size and mtime
deliberately absent from `row-identity`), the refusal names the path, and the manifest digest moves.

### (3) Reuse tamper — **PARTIALLY OPEN**

The half the brief specified is closed: a rows file that no longer proves its id is a **miss**, the
next scan **rebuilds**, and the old secret is **dead**.

Reproduction: `bb --classpath src /tmp/mem003r3-fx/h/a3_tamper.clj`

```
rows still re-fold to the id? false
(b) rescan digest = c51696ea...  same id? true
    secret(after rescan) = 7e3366cc...  CHANGED? true      (was f9986b36...)
    rows re-fold now? true
(c) OLD cursor AFTER rescan -> REFUSE:invalid-result-cursor
```

But the same run shows the gap. **Before any rescan, the existing valid cursor served the tampered
rows:**

```
(a) OLD cursor on TAMPERED rows -> SERVED n=5 first=fixt.m06
    served namespaces: [fixt.m06 fixt.m01 fixt.m08 fixt.m09 fixt.m10]
```

`fixt.m01` was substituted for `fixt.m07` and the caller got no signal. The cause is that
`run-pinned-page` resolves the snapshot with `read-meta` (`core.clj:817`) rather than with
`verified-snapshot` (`ls_tree_snapshot.clj:276`), so the content-address verification that guards
**reuse** does not guard **serve**. The namespace's own docstring states the invariant it breaks:
*"A file sitting under a content address is a CLAIM about its content"* (`ls_tree_snapshot.clj:47-50`).

### (4) The concurrent-pin race — **CLOSED; the builder's caveat is accurate**

Five trials of two concurrent `bb` processes scanning the same unpinned 2,000-file tree, then both
continuing with their own cursors.

Reproduction: the trial loop in this session (`a4_race.clj` + `a4_cont.clj`).

```
TRIAL 1,2,3,5: both cursors -> SERVED, offset 500, first=big.m0501 last=big.m1000  (identical)
TRIAL 4:       A -> REFUSE:invalid-result-cursor ; B -> SERVED, correct page
every trial:   exactly one snapshot on disk (one .edn + one .rows)
every trial:   both processes agree on digest c8aa7686..., total 2000, page-1 hash 1085848207
```

Then a tighter in-process storm — three writer threads forcing rebuilds against 600 reads
(`bb --classpath src /tmp/mem003r3-fx/h/a4c_threads.clj`):

```
READER TALLY: {"SERVED-correct" 5, "REFUSE:invalid-result-cursor" 595}
```

**Zero wrong pages, zero empty pages, zero throws.** The answer to the brief's sharper question — is
there a path where both serve pages from *different* secrets over the same rows? — is **no**, for
two independent reasons, and the design earns credit for both: only one meta file is addressable at
a time so at most one secret validates; and because rows are content-addressed, a mid-flight rename
swaps byte-identical rows. The rebuild also deletes the **meta first** (`ls_tree_snapshot.clj:359`
before `:360-361`), so the dangerous meta-present/rows-absent state is never produced by the race
itself.

### (5) Path confinement — **OPEN, two distinct failures**

Given a tampered rows file (the same access as attack 3), a row path escapes the scan root.

Reproduction A, relative `..`: `bb --classpath src /tmp/mem003r3-fx/h/a5_path.clj`

```
=== A. relative .. escape ===
  row path      : ../OUTSIDE-THE-ROOT/secret.clj
  RESULT: SERVED n=5 first=fixt.m06
  served ns   : [fixt.m06 leaked.secret fixt.m08 fixt.m09 fixt.m10]
  served files: ["src/fixt/m06.clj" "../OUTSIDE-THE-ROOT/secret.clj" ...]
  >>> CONFINEMENT ESCAPE: content from OUTSIDE the scan root was encoded <<<
```

Reproduction B, absolute path: `bb --classpath src /tmp/mem003r3-fx/h/a5b_abs.clj`

```
row path (ABSOLUTE) : /tmp/mem003r3-fx/conf2/OUTSIDE/secret.clj
fs/path abs resolves to : /tmp/mem003r3-fx/conf2/OUTSIDE/secret.clj
RESULT: !!! UNCAUGHT EXCEPTION out of run-ls-tree
        -> java.lang.IllegalArgumentException: ... is not a relative path
```

The mechanism behind B is a **resolver mismatch between the check and the read**: `stale-row`
resolves the row with `io/file` (`ls_tree_snapshot.clj:410`), which *refuses* an absolute child;
`pinned-candidates` resolves the same row with `fs/path` (`core.clj:750`), which *accepts* it and
escapes. The staleness check and the encoder can therefore be looking at different files. Today the
mismatch throws; wrapping the throw without unifying the resolvers would convert B into a silent
escape like A.

No legitimate scan produces such a row: `rel-path` (`core.clj:489`) relativizes discovered files
that are always under the scan root. These are corruption/tamper-only paths.

### (6) Determinism — **CLOSED as specified; one claim needs narrowing**

Three fresh processes, unchanged 2,000-file tree, warm state root:

```
  text: 36853 bytes | run1==run2: IDENTICAL | run1==run3: IDENTICAL
    sha: 3b41e71bd5366ee7 3b41e71bd5366ee7 3b41e71bd5366ee7
  edn:  86435 bytes | run1==run2: IDENTICAL | run1==run3: IDENTICAL
    sha: d30813bbcd9115a0 d30813bbcd9115a0 d30813bbcd9115a0
  snapshots in warm state root: 2 files
```

The `nondeterministic:4` regression is closed, and one tree state leaves one snapshot rather than
one per scan. But with a **cold** state root the same tree scans differently:

```
  fresh1==fresh2: DIFFER
< :cursor c8aa7686...:500:a7e47e451c08d58f...
> :cursor c8aa7686...:500:f9d97ec281e824aa...
```

The manifest digest is identical; the MAC differs because the secret is fresh. That is the correct
security trade — the secret must not be derivable from published material — but
`ls_tree_snapshot.clj:44` claims flatly that "an unchanged tree scan IDENTICALLY", which holds only
within one warm snapshot store. A battery run against a cleaned state root, or any scan after the
24 h TTL prune (`ls_tree_snapshot.clj:213`), will differ on that one line.

---

## Gates — ran-lines verbatim, all under `/home/forge/bin/suite-run`

`bb test/run_all.clj`:
```
Ran 764 tests containing 6260 assertions.
0 failures, 0 errors.
```

`clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test`:
```
Ran 389 tests containing 3988 assertions.
0 failures, 0 errors.
```

`swipl -q -f test/mcp_operation_contract_oracle.pl`:
```
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
```

Battery self-test (`bb -e "(require 'clj-surgeon.memory-battery-test ...)"` and
`bb bench/memory_battery/generate_tree.clj --self-test`):
```
Ran 24 tests containing 138 assertions.
0 failures, 0 errors.
generate_tree verification self-test: ok
generate_tree self-test: ok
```

All four reproduce the builder's stated figures exactly. The memory battery was **not** run
(exclusive lock held by another lane).

---

## VERDICT: **GO-WITH-FIX** for the mayor's merge queue

1. **FIX — the serve path trusts a snapshot it never verifies.**
   [core.clj:817](src/clj_surgeon/core.clj:817) calls `read-meta` where
   [ls_tree_snapshot.clj:276](src/clj_surgeon/ls_tree_snapshot.clj:276) `verified-snapshot` exists.
   Witness: rows tampered so they no longer re-fold to their id (`rows still re-fold to the id? false`),
   and the unchanged cursor still served `[fixt.m06 fixt.m01 fixt.m08 fixt.m09 fixt.m10]` — `m01`
   silently substituted for `m07`.

2. **FIX — the continuation receipt is asserted from arithmetic, never measured from the page.**
   [core.clj:833](src/clj_surgeon/core.clj:833) computes `returned (min ceiling remaining)` and
   [core.clj:853](src/clj_surgeon/core.clj:853) prints it unchanged (same at
   [core.clj:798](src/clj_surgeon/core.clj:798)).
   Witness: rows truncated 12→7, page at offset 5 encoded **2** records and the receipt claimed
   `:returned 5, :total 12, :remaining 2`; with the rows file absent it encoded **0** and still
   claimed `:returned 5` plus a next cursor — a complete-looking page that holds nothing.

3. **FIX — a `..` row path reads and encodes a file outside the scan root.**
   [core.clj:750](src/clj_surgeon/core.clj:750) resolves the pinned row with no confinement check.
   Witness: `served files: ["src/fixt/m06.clj" "../OUTSIDE-THE-ROOT/secret.clj" ...]` — namespace
   `leaked.secret` encoded from outside the root, no refusal.

4. **FIX — an absolute row path throws instead of refusing, because the check and the read use
   different resolvers.** [ls_tree_snapshot.clj:410](src/clj_surgeon/ls_tree_snapshot.clj:410) uses
   `io/file` (refuses absolute) while [core.clj:750](src/clj_surgeon/core.clj:750) uses `fs/path`
   (accepts it and escapes).
   Witness: `!!! UNCAUGHT EXCEPTION out of run-ls-tree -> java.lang.IllegalArgumentException: ... is
   not a relative path`, on a branch whose promise is a typed receipt and never a throw.

5. **FIX — `:unknown-result-cursor` documents a different-root case it cannot reach.**
   [result_budget.clj:228](src/clj_surgeon/result_budget.clj:228) and
   [ls_tree_snapshot.clj:29](src/clj_surgeon/ls_tree_snapshot.clj:29) both say a cursor from another
   root resolves to `:unknown-result-cursor`.
   Witness: two identical twin trees share a manifest digest (`same manifest digest? true`), so r1's
   cursor against r2 finds a meta and falls through to the MAC check — `REFUSE:invalid-result-cursor`,
   with the wrong remedy text.

6. **FIX — narrow the determinism claim to a warm snapshot store.**
   [ls_tree_snapshot.clj:44](src/clj_surgeon/ls_tree_snapshot.clj:44) claims an unchanged tree scans
   identically, without qualification.
   Witness: same tree, two cold state roots — identical digest `c8aa7686...`, MACs `a7e47e45...` vs
   `f9d97ec2...`, one line of 36,853 bytes different.

7. **PASS — cursor forgery from published receipt material is closed.**
   [core.clj:822](src/clj_surgeon/core.clj:822), [ls_tree_snapshot.clj:206](src/clj_surgeon/ls_tree_snapshot.clj:206).
   Witness: 80 derived MACs across 8 offsets, every one `:invalid-result-cursor`, no serve and no throw.

8. **PASS — content identity closes the stat-preserving byte swap (round-two blocker 1).**
   [ls_tree_snapshot.clj:177](src/clj_surgeon/ls_tree_snapshot.clj:177).
   Witness: size 51→51, mtime byte-identical, content changed → `:stale-result-cursor` naming
   `src/fixt/m07.clj`, and the fresh-scan digest moved.

9. **PASS — the concurrent-pin race behaves exactly as the builder declared.**
   [ls_tree_snapshot.clj:359](src/clj_surgeon/ls_tree_snapshot.clj:359) deletes the meta before the rows.
   Witness: 5 process trials + 600 in-process reads under a 3-thread rebuild storm — 
   `{"SERVED-correct" 5, "REFUSE:invalid-result-cursor" 595}`; zero wrong pages, zero empty pages,
   zero throws, one snapshot per tree state.

10. **PASS — output is byte-deterministic and one tree state pins one snapshot.**
    [ls_tree_snapshot.clj:350](src/clj_surgeon/ls_tree_snapshot.clj:350).
    Witness: three fresh processes, text sha `3b41e71bd5366ee7` ×3 and EDN sha `d30813bbcd9115a0` ×3,
    2 files in the state root.

**Why GO-WITH-FIX and not NO-GO.** The two round-two blockers were silent wrong results reachable by
an ordinary caller: a stat-preserving byte swap served as unchanged, and an unauthenticated offset
returning an empty vector that read as a complete result. Both are closed with executed witnesses,
and item 2 above is the vestigial tail of the second one — the same lying-receipt shape, now
reachable only through a damaged rows file. Every open item requires write access to
`~/.local/state/clj-surgeon` or filesystem damage; none is reachable from the tree, the caller, or
the race. Items 1 and 2 are each roughly one line and together convert items 3–4 from escapes into
refusals, so the cheapest merge-queue shape is: swap `read-meta` for `verified-snapshot` at
`core.clj:817`, take `:returned` from `(count rows)` rather than from `(min ceiling remaining)`, and
confine row paths at `core.clj:750` with the same resolver `stale-row` uses. Each deserves a witness
in `test/clj_surgeon/ls_tree_budget_test.clj` alongside the four refusal witnesses already there.
