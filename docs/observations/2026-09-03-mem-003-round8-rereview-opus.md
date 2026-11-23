# MEM-003 round 8 — Opus confirm of bridge/streaming-ls-tree at 95b0881 (2026-09-03T20:26Z)

Verdict: **GO — merge 95b0881.** Newline, trailing-newline, newline-only, CR, CRLF, 0x01, 250-byte names all discovered once and paged with parity; no phantom tokens; no other consumer assumed newline records; gates 785/6356/0, 389/3988/0, oracle, 24/138/0.

**ANDON (pre-existing on main, outside this diff, item 11):** `core/find-build-files` executes the caller-supplied `:dir` via `sh -c` — reproduced (`; touch PWNED` fires). Verified byte-identical on origin/main ad83378. Incident record inb-d27b79; fix branch bridge/andon-find-build-files-argv. Also pre-existing: a project directory whose name contains a newline is silently dropped (core.clj:179); `(System/exit 1)` inside the library op (inb-eca3b1).

## Opus verdict, verbatim

# streaming-ls-tree 95b0881 (MEM-003) — round-EIGHT independent executed re-check

Worktree `/home/forge/tmp/sol/mem003r8-wt`, `git rev-parse HEAD` =
`95b0881bc6d96b61c8ae6b7b2213991d02cbbdf5`, `git status --porcelain` empty, stash empty.
Nothing committed, stashed or pushed. Written 2026-09-03T20:25Z.

Every figure below is from my own harness under `/tmp/mem003r8-sol-fx` with
`snapshot/*state-root*` bound per run. The memory battery was NOT run. Ports 7888-7895
and 7906 were never contacted. `clojure -M:clj-surgeon/mcp-test` was invoked directly,
never `make mcp-test`. No process I did not start was signalled.

**Verdict: GO.** Round-seven item 7 is CLOSED under six independent attacks; all four
gates reproduce the builder's figures exactly. THREE items are OPEN — all three are
PRE-EXISTING on `origin/main`, in code this branch does not touch (`git diff
origin/main...95b0881 -- src/clj_surgeon/core.clj | grep -c find-build-files` = 0), so
holding this branch does not reduce exposure by one minute. One of them is
**andon-class and must be filed against main NOW, not queued behind this merge**: a
caller-supplied `:dir` reaches `sh -c` and executes. I ran it; my `touch` fired.

---

## Part 1 — item 7, re-run on MY round-seven tree

`src/clj_surgeon/core.clj:264,266` now passes `-print0` and splits on `\u0000`.
Measured at the shell on my `we<LF>ird.clj` tree (2 normal files + 1 newline-named):

    NEWLINE-SPLIT tokens: 4        <- the old parse, one file counted twice
    NUL-SPLIT   records: 3        <- the shipped parse

Through the operation, unbounded and paged at n=2:

    unbounded=3  files=["src/fixt/mod000.clj" "src/fixt/mod001.clj" "src/fixt/we\nird.clj"]
    paged=3 pages=[2 1]  parity(record-for-record)=true  dupes={}
    all-confined=true  all-exist-regular=true

Round seven threw `IllegalArgumentException: 'other' is different type of Path` out of
the unbounded scan before any receipt existed. It returns a records vector now, the
newline-named file is discovered ONCE under its whole real name, and the paged walk
equals the unbounded scan record for record. **CLOSED.**

## Part 2 — six new attacks on the NUL split

Every row: discovered exactly once, no duplicate, confined, resolves to a real regular
file, paged walk == unbounded scan record for record.

| attack | name | unbounded | paged | parity |
|---|---|---|---|---|
| trailing newline before ext | `trail<LF>.clj` | 3/3 | [2 1] | true |
| only a newline plus ext | `<LF>.clj` | 3/3 | [2 1] | true |
| carriage return | `cr<CR>x.clj` | 3/3 | [2 1] | true |
| CRLF (the `str/split-lines` regex) | `crlf<CR><LF>x.clj` | 3/3 | [2 1] | true |
| NUL-adjacent control char | `soh<0x01>x.clj` | 3/3 | [2 1] | true |
| 250-byte name | `z*246.clj` | 3/3 | [2 1] | true |
| all five in ONE tree | — | 8/8 | [3 3 2] | true |

The 0x01 is really in the filename, not a printing artifact —
`od -c` on the discovered leaf: `s o h 001 x . c l j` — and the record's `:file`
round-trips through the line-delimited EDN manifest (`ls_tree_snapshot.clj:493` writes
`pr-str` + `"\n"`, read back by `line-seq` at `:366`) because `pr-str` escapes `\n`
and `\r` and prints 0x01 raw, which `BufferedReader.readLine` does not split on.

**Trailing empty token / empty output**, called directly on `#'core/find-clj-files`:

    3-file tree   -> 3 candidates, any-blank=false      (no phantom "" after the last NUL)
    EMPTY dir     -> []  count=0                        (zero candidates, not one)
    newline-only  -> 1  ["/…/src/fixt/we\nird.clj"]

**Bonus, unclaimed by the commit:** the fix also dropped `str/trim`, so names with
leading/trailing whitespace now survive. `[" lead.clj" "trail .clj" "aaa.clj" "\ttab.clj"]`
-> 4 discovered, all resolving to real files. Under the old `(str/trim (:out result))`
the first and last records could lose an edge character.

## Part 3 — no other consumer assumed newline records

`grep -rn find-clj-files` over the tree: the only call sites are `core.clj:285`
(`discover-projects`, build-file path), `core.clj:296` (fallback recursive scan) and
`core.clj:780` (`discover-projects-grep`). All three do `(map str) sort vec` / `mapcat`
and nothing else — **no `str/split-lines` is applied to this output anywhere**, in
`src/`, `test/` or `bin/`. `rename.clj:32` is a different, identically-named private fn
that uses `file-seq`, never a shell. The returned seq shape is unchanged. **CLOSED.**

## Part 4 — gates, once each, under `suite-run`

| gate | command | result | claimed |
|---|---|---|---|
| test-fast | `suite-run bb test/run_all.clj` | **785 tests / 6356 assertions / 0 failures / 0 errors**, exit 0 | 785/6356/0 |
| mcp-test | `suite-run clojure -M:clj-surgeon/mcp-test` | **389 / 3988 / 0 / 0**, exit 0 | 389/3988/0 |
| oracle | `suite-run swipl -q -f test/mcp_operation_contract_oracle.pl` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]`, exit 0 | pass |
| self-test | `suite-run` on the `memory-battery-self-test` body | **24 / 138 / 0 / 0**, exit 0 | 24/138/0 |

Four for four, exact. Note the mcp-test run drops `MCP_JAVA_OPTS` (the brief's direct
invocation); it was green regardless.

---

## Part 5 — the three OPEN items (all pre-existing, none in this diff)

### OPEN-A — ANDON: caller-supplied `:dir` is executed by `sh -c`

`src/clj_surgeon/core.clj:174-178` builds a SHELL STRING with `format` and runs it
through `"sh" "-c"`. `dir` is the operation's own `:dir` argument. Isolated witness:

    (find-build-files "/tmp/mem003r8-sol-fx/H; touch /tmp/mem003r8-sol-fx/PWNED3 ; echo z")
    marker before: false
    marker after : true        <- my touch executed

and end-to-end through `core/run-ls-tree` with that `:dir`, the marker was created AND
the operation threw `IllegalArgumentException: 'other' is different type of Path` — the
exact untyped-throw class this round exists to defeat, still reachable one function up.
Byte-identical on `origin/main`. Fix is the same one this round already applied twice:
argv tokens, never a shell string (`-print0` + NUL split for the parse). **File it now,
against main; do not queue it behind this merge.**

### OPEN-B — a project directory whose NAME contains a newline is SILENTLY dropped

Same line, `src/clj_surgeon/core.clj:179` (`str/split-lines` on `find`'s output).
Mixed tree, one ordinary project + one whose directory name contains `\n`, scanned from
above:

    F: MIXED good + nl-dir   count=3 expect=6  files=["good/src/fixt/mod000.clj" …]

Three of six files, a COMPLETE-looking result, no refusal, no receipt saying anything
was lost. Silent under-reporting is worse in kind than the throw round eight fixed. It
also makes the round's own new EARS words — "reading the discovery command's output as
NUL-delimited records so that a candidate name containing a newline is counted once"
(`docs/intent/read-path-memory/read-path-memory-specs.md:21`) — true of the file scan and
NOT true of the project scan in the same operation. Either fix `find-build-files` or say
which discovery command the clause is about.

### OPEN-C — `(System/exit 1)` inside the library operation (round-seven observation, re-confirmed live)

`src/clj_surgeon/core.clj:886`, in `run-fresh-scan`: an empty scan `println`s and kills
the JVM. It killed my own harness mid-run when a newline-named project directory made a
scan look empty (case D) — cases E and F never ran until I re-invoked them in a separate
process. A library operation contracted to return a typed receipt must not call
`System/exit`.

---

## Verdict

**GO** — merge 95b0881. Item 7 CLOSED on my own trees under seven attack shapes, gates
four-for-four at the claimed figures, no regression anywhere in the diff. OPEN-A is
ANDON-class and belongs in a separate item against `main` filed today; OPEN-B and OPEN-C
are pre-existing and follow it. None of the three is a reason to hold this branch, and a
bare "GO" that lets OPEN-A ride quietly is the failure mode I am naming out loud.
