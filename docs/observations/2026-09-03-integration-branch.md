# Integration branch `bridge/integration-2026-09-03` — composition record

Opened 2026-09-03 22:14 UTC by forge@anvil, at the mayor's request: compose the remaining GO
lanes onto current main, produce ONE branch that is green with all of them
together, hand back one sha.

Base, proven before any edit:

```
git fetch origin main            -> 99394bfee6e72500d24808081ab8f81e43fe31e2
git worktree add /home/forge/src/clj-surgeon-integ -b bridge/integration-2026-09-03 99394bf
git -C /home/forge/src/clj-surgeon-integ rev-parse HEAD
                                 -> 99394bfee6e72500d24808081ab8f81e43fe31e2   (equal)
```

Nothing is pushed from here. Commits only; the seat pushes.

## STEP 0 — the ratchet: the spec-document registry is DERIVED, not listed

**The mayor's finding.** `src/clj_surgeon/mcp_intent_contract.clj` carried the
audited spec documents as a literal vector inside `audit-current-repository`
(7 entries, lines ~102-116). Every lane that added an intent leaf appended a line
to that one vector, so **every lane conflicted with every other lane by
construction** — a merge conflict manufactured by the registry's shape, not by
any disagreement about behaviour.

**The fix.** `spec-doc-paths` scans `docs/intent/<leaf>/<name>-specs.md` at
audit time and returns repo-relative paths sorted lexicographically. A new lane
now adds a FILE and touches no shared line.

Scan discipline, so the derivation stays honest:

- the name pattern is `.+-specs.md` exactly, so the `-specs.from-<source>--*.md`
  provenance variants that several leaves carry are NOT swept in;
- results are sorted, so the concatenated spec text is deterministic;
- an empty scan throws `:no-spec-docs-found` — a moved intent tree must not
  quietly become an empty, trivially-passing audit;
- `excluded-spec-docs` is a map of path -> one-line reason, and an entry naming a
  file that does not exist throws `:orphan-spec-doc-listing`;
- the exclusion map is an ARGUMENT (3-arity), not only a global, so a witness can
  drive the scan against a fixture root without the repository's own exclusions
  following it there.

### What the derivation newly includes, and what it cost

The scan finds 15 spec documents where the vector listed 7. Eight leaves the
vector never mentioned:

| newly scanned leaf | MCP-OP specs it contributes | audit effect |
|---|---|---|
| `2026-08-29-ratification/measurement-evidence-specs.md` | 0 (different ID prefix) | none |
| `2026-08-30-prepared-request-ratification/prepared-request-specs.md` | 9, identical IDs to `prepared-request/` | none (duplicate IDs collapse) |
| `operation-algebra` | 0 (different ID prefix) | none |
| `performance-regression-sentinel` | 0 (different ID prefix) | none |
| `sibling-pair-edit` | 0 | none |
| `worktree-lifecycle` | 0 (different ID prefix) | none |
| **`embedded-elaborator`** | **19 `MCP-OP-ELAB-*`, all active-gap** | **19 violations** |
| **`substantiation-telemetry`** | **19 `MCP-OP-SUBST-*`, all `[x]`** | **38 violations** |

**Both failures are reported here rather than silently dropped**, per the mayor's
instruction. Measured with the exclusion map empty:

```
DERIVED-COUNT 15
OK? false SPECS 225 VIOLATIONS 57
```

- `embedded-elaborator`: 19 `missing-test-witness`. Its red namespace
  `clj-surgeon.mcp-embedded-elaborator-test` is declared frozen-red
  (`docs/intent/embedded-elaborator/frozen-red-declaration.md`, 2026-08-30) and is
  **not in this tree**; `grep -r MCP-OP-ELAB src/ test/` returns nothing.
- `substantiation-telemetry`: 19 `missing-implementation-witness` + 19
  `missing-test-witness`. Its specs are marked `[x]` to record Gene's advance
  ratification ("Go on all!!!", 2026-08-30), not shipped code;
  `grep -r MCP-OP-SUBST src/ test/` returns nothing.

Both are pre-product intent leaves. Neither is a regression this branch caused,
and neither can be repaired tonight without writing the two product surfaces, so
both are excluded **by name, with a reason, and with a re-inclusion trigger**
recorded in `excluded-spec-docs`. With those two excluded:

```
DERIVED-COUNT 13
OK? true SPECS 187 VIOLATIONS 0
```

**187 is exactly the old literal vector's spec count** — deriving the list changed
WHICH FILES are scanned, not WHICH INTENTS are audited.

### Witnesses added (`test/clj_surgeon/mcp_intent_contract_test.clj`)

| witness | proves |
|---|---|
| `a-new-intent-leaf-is-picked-up-by-adding-only-a-file` | a temp `docs/intent/temp-lane/temp-lane-specs.md` is scanned; a sibling `-design.md` and a `-specs.from-docs--x.md` variant are not |
| `an-orphan-spec-doc-listing-fails-loudly` | an exclusion naming a path that does not exist throws `:orphan-spec-doc-listing` and names the path |
| `an-empty-intent-tree-fails-loudly` | an empty intent tree throws `:no-spec-docs-found` instead of auditing nothing |
| `every-spec-doc-exclusion-carries-a-named-reason` | the exclusion set is non-empty only for named reasons: each key exists on disk, each value is a substantive reason string |
| `the-derived-spec-doc-set-matches-the-expected-set-exactly` | the 13 derived paths are asserted literally, so any drift in `docs/intent` is visible |
| `the-derived-audit-covers-exactly-the-old-literal-vector-intents` | derived intent-ID set == the old 7-file vector's intent-ID set, count 187; a diff prints added/removed |

Ran line:

```
suite-run clojure -M:test -e "(require 'clj-surgeon.mcp-intent-contract-test) (clojure.test/run-tests 'clj-surgeon.mcp-intent-contract-test)"
  -> Ran 11 tests containing 24 assertions. 0 failures, 0 errors.

suite-run bb test/run_all.clj
  -> Ran 727 tests containing 6051 assertions. 0 failures, 0 errors.  (1m6s)
```

## STEP 1 — merges

Order as briefed. Each merge is `--no-ff`, each names its branch and its GO
verdict file, and the fast gate `suite-run bb test/run_all.clj` ran after every
one. Two lanes were REFUSED and are argued below.

| # | lane | sha merged | brief said | fast gate after |
|---|---|---|---|---|
| 0 | (ratchet) | — | — | 727/6051/0 |
| 1 | q5z-alias-migration | f51ceae | f51ceae | 760/6402/0 (after one fix) |
| 2 | read-path-memory | b7ef23d | b7ef23d | 760/6402/0 (after one fix) |
| 3 | parser-admission | 52c5d85 | 52c5d85 | 806/6688/0 |
| 4 | **study-ops-mcp** | **REFUSED** | 4480e3d | — |
| 5 | memory-battery | 5534e94 | 5534e94 | 811/6708/0 |
| 6 | **streaming-ls-tree** | **REFUSED** | 95b0881 | — |
| 7 | anvil-arms-apparatus | 89295d8 | 77e6237 (seat amended) | 811/6708/0 |
| 8 | txn-journal | 2df05b3 | 5a2d254 (seat amended) | 811/6708/0 |

Excluded before I started, by the mayor: rf2 965d49e.

### The two refusals

**bridge/study-ops-mcp — REFUSED. It silently reverts MEM-005, and its own
lane's O2 round does not touch that.**

The lane moves project discovery and the whole `ls-tree` implementation out of
`clj-surgeon.core` and into `clj-surgeon.study`, so that the CLI and the MCP read
entrance share one kernel. That is the right shape. But the kernel it moves the
code INTO was written against a main that predates two lanes already on this
branch, and the move takes their work with it:

| what disappears | where it lives on this branch | measured consequence |
|---|---|---|
| `safe-outline`'s `catch StackOverflowError` | core.clj, MCP-OP-MEM-005 | study's `safe-outline` catches `Exception` only. An `Error` is not an `Exception`, so ONE overflowing file kills the whole scan again — the exact defect MEM-005 shipped to close |
| the `:resources` / `parser_admission_refused` receipt | core.clj `format-ls-tree-edn`, `admission-refusals`, `scan-resources` | absent from study's encoders; 5 parser_admission_test witnesses error |
| `ls-tree-root-refusal` | core.clj, MCP-OP-SHELL-ARGV-002 (andon inb-d27b79) | study's `run-ls-tree` calls `System/exit 1` on a bad root, which **aborts the babashka test process** rather than returning a typed refusal |
| `find-build-files` `-H` / `-print0` / `find-start-token` | core.clj (andon) | study's copy is argv-safe but line-framed and `-P`-default |

The andon witness namespace does not even load: it resolves
`#'clj-surgeon.core/find-build-files`, which no longer exists.

I began the port — `existing-directory?`, `find-start-token`,
`nul-separated-paths`, a deterministic `prune-tokens`, `-H`/`-print0` on both
find sites, and repointing the andon witness — and stopped when the remaining
work was the whole MEM-005 surface (`safe-outline`, `admission-refusals`,
`scan-resources`, the text refusal block, the EDN receipt, and the root refusal)
re-implemented inside `study.clj`, unreviewed, at 05:00. **MEM-005 has a GO for
its implementation in `core.clj`. Writing a second implementation of it inside
`study.clj` tonight would ship an unreviewed version of a ratified lane**, which
is the thing the fence-review doctrine exists to prevent.

Checked again after the seat pushed O2 (`26e4810`, "ls-tree text rows, default
limit 8192, session.start telemetry"):

```
git diff --stat 4480e3d 26e4810
  docs/intent/study-ops/study-ops-specs.md, docs/observations/...o2..., 
  src/clj_surgeon/mcp_inspect_tool.clj, src/clj_surgeon/mcp_telemetry.clj,
  src/clj_surgeon/mcp_tool.clj, test/... — 7 files
```

**`src/clj_surgeon/study.clj` and `src/clj_surgeon/core.clj` are untouched by
O2**, and all four markers are still absent at the new tip:

```
26e4810:src/clj_surgeon/study.clj   StackOverflowError 0  parser_admission_refused 0
                                    ls-tree-root-refusal 0  -print0 0
```

So the refusal holds at 4480e3d and at 26e4810 alike. What the lane needs is one
round on a main that HAS parser-admission and the andon fix — the same ruling the
mayor already made for rf2.

**bridge/streaming-ls-tree — REFUSED. Two GO'd lanes carry contradictory
ratified requirements, and composing them means amending one lane's gate.**

The merge itself resolves cleanly and I did resolve it: `find-clj-files` takes
the union (andon's `-H`, `find-start-token`, NUL parse; MEM-003's
`( -type f -o ( -type l -xtype f ) )` predicate and parenthesised name chain),
`safe-outline` takes the union (MEM-003's regular-file guard wrapping MEM-005's
`StackOverflowError` catch), the andon `ls-tree-root-refusal` is kept and wired
into MEM-003's new `run-ls-tree` ahead of the ceiling check, and I threaded the
MEM-005 admission meter through MEM-003's streaming encoders so the streamed EDN
publishes the same `:resources` trailer the batch encoder does.

That last step is where it stops being a merge. **MEM-005 requires the timing
UNCONDITIONALLY; MEM-003 requires the result to be byte-identical across two
scans and to carry NO receipt when it is complete.** `scan_ms` is a wall-clock
reading. The two cannot both hold:

- MEM-005, `read-path-memory-specs.md`: *"the scan's own cost charged as
  `scan_ms` WITH its `bytes_scanned` denominator in the EDN receipt's
  `:resources` block UNCONDITIONALLY — a meter that only reports on the rare
  refusal branch is dark on the ~100% of scans a regression would appear in"*.
- MEM-003, same file: *"Two scans of an UNCHANGED tree ... producing
  byte-identical results in both the text and EDN encodings, cursor included"*,
  witnessed by `two-scans-of-an-unchanged-tree-are-byte-identical-and-pin-one-snapshot`;
  and `a-result-exactly-at-the-ceiling-is-complete` asserts `(nil? (receipt at))`.

Measured, with the composition applied — nine failures, every one of them this
one disagreement:

```
FAIL two-scans-of-an-unchanged-tree-are-byte-identical-and-pin-one-snapshot
     the two EDN results differ in exactly :scan_ms (2.888 vs 2.782)
FAIL a-result-exactly-at-the-ceiling-is-complete
     "a complete result carries no ceiling receipt"
     expected: (nil? (receipt at))
     actual:   (not (nil? {:receipt {:resources {:scan_ms 7.906, :bytes_scanned 674}}}))
FAIL under-the-ceiling-the-streamed-result-equals-the-batch-result  (x2)
FAIL a-parse-error-under-the-ceiling-still-reads-exactly-as-before
FAIL the-server-ceiling-binds-at-its-shipped-value-not-at-a-fixture-value  (x2)
FAIL the-continuation-cursor-pages-the-remainder-exactly-once
```

Note that MEM-003's own falsifier for the determinism row is the battery's
`nondeterministic:4` — four output hashes over five reps of one operation,
"differing in exactly the cursor line". A wall-clock field in the same result is
the same defect class arriving from the other lane.

**Two honest resolutions exist and both amend a ratified gate**, which is why I
am not choosing:

1. Narrow MEM-003's determinism and no-receipt rows so their subject is the
   CURSOR and the CEILING receipt (which is what their falsifiers are actually
   about), and mask the measured field the way this branch already masks it in
   the andon symlink witness. Cheap, and arguably what both rows meant.
2. Narrow MEM-005's "unconditionally" so `:resources` is published on the text
   encoding and on any scan that refused, and `bytes_scanned` (deterministic)
   always, with `scan_ms` excluded from a complete EDN result. Keeps MEM-003
   byte-exact, weakens the meter on exactly the scans MEM-005 says matter.

My own read, offered as a recommendation and not acted on: **(1)**. MEM-003's
requirement text says "cursor included" and its measured falsifier is the cursor
line; MEM-005's meter is the thing that would go dark under (2), and it was
argued for precisely because a dark meter is invisible. But it is a gate change
on a GO'd lane and it belongs to whoever owns that lane.

Everything else about the lane composed cleanly, so this is one decision, not a
round of work.

## Conflict table

Every conflict resolved on this branch, in merge order.

| # | merge | file : site | resolution | why |
|---|---|---|---|---|
| 1 | q5z | `src/clj_surgeon/mcp_intent_contract.clj` : `audit-current-repository` | OURS | their hunk appended `alias-migration` to the literal vector; the derived scan finds the file |
| 1 | q5z | `test/clj_surgeon/mcp_test_runner.clj` : ns require + `-main` | UNION | both inserted one ns at the same alphabetical slot: ours `core-discovery-test` (andon), theirs `mcp-alias-migration-test` |
| 1 | q5z | `src/clj_surgeon/intent_transaction.clj` : `change!` | UNION | main's `:expect-matched` and q5z's `:on-write-boundary`; dropping either makes that lane's own feature refuse as `:unknown-arguments` |
| 1 | q5z | `test/clj_surgeon/operation_algebra_test.clj` : effect inventory (fix commit 511814c) | CORRECT THE ORACLE | q5z rewrote the inventory to be owner-keyed; main added `matched-basis-evidence`, a callee of `execute-change-with-context!`. Added with the empty effect set its own docstring justifies ("Performs no I/O") |
| 2 | read-path | `src/clj_surgeon/mcp_intent_contract.clj` | OURS | same registry line |
| 2 | read-path | `src/clj_surgeon/outline.clj` : `top-level-form-records` | THEIRS | ours was the pre-MEM-015 inline builder this lane exists to replace; the three arities now delegate to `parse-and-build-records` so an outline parses ONCE |
| 2 | read-path | `src/clj_surgeon/outline.clj` : `form-records-from-walked` (fix 0a14fc6) | PORT MAIN IN | the new builder was written before main's `defmethod` dispatch (a28690e) and dropped `:dispatch` from every record |
| 2 | read-path | `test/clj_surgeon/outline_differential_test.clj` (fix 0a14fc6) | RE-FREEZE | the frozen twin was taken at 9f48694, before a28690e; left alone it reported a difference that was really the frozen side being stale |
| 3 | parser-admission | `src/clj_surgeon/mcp_intent_contract.clj` | OURS | same registry line |
| 3 | parser-admission | `Makefile` : `.PHONY` | UNION | ours `repository-hygiene(-self-test)`, theirs the five battery/red targets; a dropped `.PHONY` name is a silent no-op the day a file of that name exists |
| 3 | parser-admission | `docs/intent/read-path-memory/read-path-memory-specs.md` : MEM-015 / MEM-005 | OURS + ADD | ours is read-path round 2's EARS-scoped MEM-015; theirs is the pre-correction wording. MEM-005 added |
| 3 | parser-admission | `docs/observations/2026-09-03-captains-log-anvil-seat.md` | UNION, chronological | append-only lab notebook; the 06:22Z branch entry inserted before main's 06:25Z entry for the same round |
| 3 | parser-admission | `test/clj_surgeon/core_discovery_test.clj` : symlinked-root witness | MASK THE MEASURED FIELD | MEM-005 added a wall-clock `scan_ms`, so two runs of one scan are never byte-identical. The discovery claim is about what was found: `scan_ms` is masked and asserted separately, everything else compared byte for byte |
| 5 | memory-battery | — | none | the parser-admission merge had already absorbed this lane's Makefile targets |
| 7 | anvil-arms | `Makefile` : `.PHONY` | UNION | `anvil-arms-self-test` added |
| 8 | txn-journal | 7 battery files (add/add) | OURS | this branch carried an OLDER copy; the battery lane's is the reviewed one and has the attestation surface, the INCOMPLETE state, and the round-3 ruling that peak lines are a TREND. Theirs' five extra tests assert the SUPERSEDED gating |
| 8 | txn-journal | `Makefile` : `memory-red` | **RENAME, NOT MERGE** | **two different meters under one name.** Ours = the parser-admission red witness; theirs = the kernel's OOM witness. Their GO evidence quotes different numbers for the same word. `memory-red` stays the parser witness; `memory-red-kernel` is the kernel's, under the exclusive lock |
| 8 | txn-journal | `Makefile` : mcp-test tail, `MEMBAT_ENV`, `memory-battery`, `.PHONY` | UNION / OURS / OURS / UNION | both self-checks kept; the attestation env and the attest-gated battery are round 3's |
| 8 | txn-journal | `deps.edn` | THEIRS | additive `:clj-surgeon/memory-test` alias the kernel witness needs |
| 8 | txn-journal | `test/clj_surgeon/mcp_test_runner.clj` | UNION | `run_all` does NOT load `txn-journal-test`; the kernel's witnesses run only under mcp-test, so dropping either side removes a whole lane's gate |
| 8 | txn-journal | `src/clj_surgeon/memory_battery{,_runner}.clj` (fix 151e131) | PORT THEIRS' ONE FIX FORWARD | the battery lane's runner hard-coded `:heap-reserved-peak-mb nil`, so `reserved-check` could only ever say UNMEASURED. Sol's blocker 6 had been fixed on the kernel lane's superseded copy; `bytes->mb` + `reserved-peak-mb` ported |
| 8 | txn-journal | `Makefile` (fix 5cc1465) | REPAIR MY OWN DAMAGE | `memory-battery-self-test:` came out of the resolution COMMENTED — `make test` would have skipped the battery's gate in silence |

## What caught what

Worth recording, because in every case the composition was caught by a witness
and not by review:

- **a compile error**, not a silent pass, caught the battery/kernel accountant
  split: `No such var: battery/reserved-peak-mb` at `scope_stream_test.clj:279`,
  which is the witness whose own docstring names that hazard — *"if they
  disagree the battery reports UNMEASURED for ever and nothing fails"*.
- **the frozen differential** caught the dropped `defmethod` dispatch by naming
  the file: *"1 of 167 files outlined differently:
  test/clj_surgeon/analyzer_contract_test.clj"*. That is also the sabotage proof
  the lane claimed, delivered against a real divergence rather than an injected one.
- **the effect-inventory oracle** caught the q5z/main union at exactly one entry.
- **`make -n`** caught the commented Makefile target that `make` reported as
  "Nothing to be done".

## STEP 2 — gates at the final sha

Every ran-line below was executed on `aadbdbca636378b0b1878c863114f02b70507cef`.

| gate | result |
|---|---|
| `suite-run bb test/run_all.clj` | `Ran 814 tests containing 6724 assertions. 0 failures, 0 errors.` |
| `suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 597 tests containing 6305 assertions. 0 failures, 0 errors.` |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| `clj-surgeon.mcp-intent-contract-test` | `Ran 11 tests containing 23 assertions. 0 failures, 0 errors.` |
| `audit-current-repository` | `ok= true specs= 253 violations= 0 spec-docs= 17` |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| `make memory-battery-self-test` | `generate_tree root-marker self-test: ok` / `generate_tree self-test: ok` / `Ran 29 tests containing 158 assertions. 0 failures, 0 errors.` |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` |
| `make memory-red-kernel` | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` |
| `bash bench/anvil-arms/self-test.sh` | `anvil-arms self-test: 389 passed, 0 failed` |
| `clj-surgeon.core-discovery-test` (shell-safety) | `Ran 7 tests containing 35 assertions. 0 failures, 0 errors.` |
| `clj-surgeon.outline-test` + `outline-differential-test` (MEM-015) | `Ran 20 tests containing 68 assertions. 0 failures, 0 errors.` |
| `make memory-battery` (ONCE, under the exclusive lock) | **FAIL (INCOMPLETE) exit 1** — read on |

Note on `make memory-red`: its default `PARSER_RED_EXPECT=red` asserts the
PRE-FIX defect and correctly reports `0/3 assertions held (expect=red)` now that
MEM-005 is merged. `expect=green` is the post-fix assertion, and it holds 6/6.

Two brief instructions could not be honoured literally, both because the battery
lane's own guards refused them, which is the guards working:

- `MEMBAT_ROOT=/tmp/integ-fx/battery` → `REFUSED: MEMBAT_ROOT resolves outside
  /home/forge/tmp` (Sol hole 4). Used the tool's own documented escape,
  `MEMBAT_ALLOW_ANY_ROOT=1`, and kept the fixture where the brief put it.
- a pre-created root → `REFUSED: MEMBAT_ROOT exists without its marker`. Removed
  it so the battery could build a fresh, marked corpus.

### The battery result, and the one NEW red in it

```
verdict: FAIL (INCOMPLETE)   exit 1
```

**Three `held-scales-with-n` FAILs — all KNOWN, all by design.** The
memory-battery lane's GO says so in as many words: "main is RED under it BY
DESIGN".

```
FAIL held-scales-with-n {:op :cli-ls-tree, :profile :default, :observed 95.6, :limit 11.7, :small-n-observed 9.7, :slack-mb 2.0}
FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.8, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
```

The brief named two of these as pre-existing (`rename-ns-plan-full-match`,
`workspace-sources-read-all`). The third, `cli-ls-tree`, is equally pre-existing
and is quoted in the same GO ("ls-tree peak 274.8/418.3 MB vs 247.8 limit at
1k/10k"); MEM-003 is the lane that was going to close it, and MEM-003 is not in
this branch. Nine `TREND` lines and four `UNMEASURED` reserved-peak lines are
reported, never gated.

**And something else does fail, which was NOT known: twelve
`reference-mismatch` lines, on `cli-ls-tree` only, every profile, both phases.**

```
FAIL reference-mismatch {:op :cli-ls-tree, :n 100, :phase :fresh, :profile :default, :observed "50e81ef8…", :limit "a3ac4976…"}
FAIL reference-mismatch {:op :cli-ls-tree, :n 100, :phase :warm, :profile :default, :observed "nondeterministic:4", :limit "a3ac4976…"}
… the same pair for n=1000, n=10000, cljc, giant and nested …
```

`nondeterministic:4` means four distinct output hashes over five reps of one
operation on one unchanged corpus. The memory-battery lane's round 2 had output
parity GREEN on all six corpora, so this red is a product of the composition,
not of either lane alone. The reference was built minutes earlier on this exact
tree and attested — `attested to {:head-sha "aadbdbca…", :jvm "21.0.12"}` — so
this is the code disagreeing with ITSELF, not with a stale reference.

**Cause, measured rather than argued.** Two back-to-back `cli-ls-tree` EDN scans
of one unchanged tree:

```
equal? false
A receipt: {:receipt {:resources {:scan_ms 44.081, :bytes_scanned 111183}, …}}
B receipt: {:receipt {:resources {:scan_ms 23.054, :bytes_scanned 111183}, …}}
records equal (receipt dropped)? true
```

Every record is identical. The results differ in exactly one field: `scan_ms`, a
WALL-CLOCK reading that MCP-OP-MEM-005 requires the EDN receipt to publish
UNCONDITIONALLY, and that MCP-OP-MEM-011's output-parity line hashes.

**This is the SAME contradiction that made me refuse bridge/streaming-ls-tree**,
arriving from a third direction and already live on this branch. There it was
MEM-005 against MEM-003's byte-identical-result row; here it is MEM-005 against
MEM-011's reference-parity row. The subject is one field:

> a measured duration inside a result that other requirements hash.

Three ratified rows now depend on that field's treatment (MEM-005 unconditional;
MEM-003 byte-identical scans; MEM-011 output parity), and no arrangement
satisfies all three as written. **The fix belongs in one place and is one
decision**, which is why it is reported rather than taken here. My
recommendation, unchanged from the streaming-ls-tree section: keep the meter and
put the measured field where nothing hashes it — publish `scan_ms` on a
non-hashed channel (or exclude the measured key from the parity digest while
keeping `bytes_scanned`, which IS deterministic, inside it). That keeps MEM-005's
meter bright on ordinary scans, restores MEM-011 parity, and unblocks MEM-003's
merge in the same stroke.

Nothing else in the battery is new. The full run is preserved at
`/tmp/integ-fx/battery/receipts/20260903T235745.421675657Z-battery.edn`, with the
reference receipt beside it.
