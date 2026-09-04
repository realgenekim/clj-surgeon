# Third landing: `bridge/integration-2026-09-03-studyops` — study-ops onto the MEM-005/MEM-003 branch

Opened 2026-09-04 by forge@anvil, from the second integration branch's tip.
Nothing merges to main from this seat, and nothing here was pushed anywhere
but this branch. Gene, verbatim, 2026-09-04: *"no one should be merging to
main, even mayor. People are using public repo, and I don't want to publish
anything on main until we have clear and decisive winner that is tested and
dogfooded for months."*

## Base, proven before any edit

```
git -C /home/forge/src/clj-surgeon fetch origin \
    bridge/integration-2026-09-03-mem003 bridge/study-ops-mcp
  origin/bridge/integration-2026-09-03-mem003 -> 0a38e3d8c7032776127f16a723429bc3d9afd024
  origin/bridge/study-ops-mcp                 -> a0b052061499daa2a4f729f285170f9e8981f2f2

~/bin/worktree-add /home/forge/src/clj-surgeon /home/forge/src/clj-surgeon-integ3 \
    bridge/integration-2026-09-03-studyops 0a38e3d8c7032776127f16a723429bc3d9afd024
  -> HEAD 0a38e3d8c7032776127f16a723429bc3d9afd024   (equal; worktree clean)

merge base of the two branches: a6e72c1262e5e621b5fce6b8fa16a69df46da779
```

Both refs are ancestors of the final tip (`git merge-base --is-ancestor`, both
true).

## Why this composition was hard, in one paragraph

`bridge/study-ops-mcp` moves project discovery and the whole `ls-tree`
implementation out of `clj-surgeon.core` into `clj-surgeon.study`, so the CLI
and the `inspect_clojure` read entrance share one kernel; O2 round two then
bounds the public MCP result at 32 KB and puts the rows in the text block.
The kernel it moved the code INTO was written against a main that predates
parser-admission (MEM-005), the bounded/streaming ls-tree (MEM-003) and the
andon shell-argv lane. Measured at the merge base, `study.clj` carried **zero**
of the six hardening markers:

```
a0b0520:src/clj_surgeon/study.clj
  StackOverflowError 0   parser_admission_refused 0   ls-tree-root-refusal 0
  -print0 0              find-start-token 0           scan_ms 0
HEAD(0a38e3d):src/clj_surgeon/core.clj
  StackOverflowError 4   parser_admission_refused 9   ls-tree-root-refusal 4
  -print0 5              find-start-token 4           scan_ms 5
```

That is the same refusal the first integrator recorded, unchanged by O2
round two. The composition below does NOT re-implement any of it: it moves
mem003's reviewed code into the shared kernel and keeps mem003's bounded CLI
encoder where MEM-003's memory-battery numbers were earned.

## The merge

```
git merge --no-ff a0b0520          (onto 0a38e3d)
merge commit: 83b20977832ecfff81f74b37637460e7bc3461e8
final tip:    <see the branch head; this document is the last commit on it>
```

Seven conflicted files, 1,560 conflict lines, 1,228 of them in `core.clj`.

| # | file | hunks | resolution |
|---|---|---|---|
| 1 | `src/clj_surgeon/core.clj` | 2 | **THEIRS** for the four file-scoped delegations (`run-deps`, `run-topo`, `run-closure`, `run-ls-deps` -> `study/*`); **OURS** for the entire ls-tree half, so the CLI keeps MEM-003's bounded, cursor-paginated encoder and the andon `ls-tree-root-refusal`. `skip-dirs` and the `babashka.fs` / `babashka.process` requires restored by hand: study-ops removed both and the retained block needs them. |
| 2 | `src/clj_surgeon/mcp_inspect.clj` (ns) | 1 | **UNION** of five requires. `node`/`parser` are live (`wildcard-pattern?`, MCP-OP-FIELD-003), and so are `walk`/`study`; each appears exactly once in the merged file, in code, not only in the require. |
| 3 | `src/clj_surgeon/mcp_inspect.clj` (`result-evidence`) | 1 | **THEIRS.** git had aligned O2's new row-returning `match` branch against the OLD `result-summary-line`'s string-returning one. Taking ours would have put a string inside a vector-of-rows `case`. |
| 4 | `src/clj_surgeon/mcp_inspect_tool.clj` (schema) | 1 | **UNION**: the DISPATCH-001 note and study's `limit`/`form` properties. |
| 5 | `src/clj_surgeon/mcp_inspect_tool.clj` (`inspect-summary`) | 2 | **UNION.** O2r2's single `refusal-text` renderer (item 4) is the frame; mem003's `defmethod-owner-lines`, `missing-field-lines` and `named-field-lines` ride in as `extra-lines`, and the FIELD-001 arrow is back in the `cond`. Taking THEIRS alone silently dropped four ratified refusal clauses that have named witnesses (`missing-fields-summary-names-the-field-and-the-minimal-shape`, `invalid-require-policy-summary-names-the-field-and-its-values`, and the two `owner is a multimethod` assertions). |
| 6 | `src/clj_surgeon/mcp_intent_contract.clj` | 1 | **OURS.** `spec-doc-paths` SCANS `docs/intent/<leaf>/<name>-specs.md`, so study-ops's new spec doc is picked up with no shared vector to conflict on — strictly better than the literal list it collided with, and it is why the registry needed no production edit. |
| 7 | `test/clj_surgeon/mcp_inspect_tool_test.clj` | 1 | **UNION** — two disjoint test blocks. |
| 8 | `test/run_all.clj` | 1 | **UNION** — both namespaces. |
| 9 | `docs/tech-tree.md` | 1 | **UNION**: mem003's two friction-ledger rows plus study-ops's updated ls-tree row (`OPEN` -> `BUILT, adoption unmeasured`). |

## What the resolution reproduced (the RED), and what fixed it

The resolved merge compiled and ran, and produced exactly six distinct
failures. Three were the composition's real defects; three were fixture drift
from lanes already on this branch. Nothing was skipped.

### The composition's real defects

**1. `test-format-ls-tree-edn` (ls_tree_test.clj:313) — mem003's own MEM-005
witness, RED.**

```
expected: (= 2 (count result))          actual: (not (= 2 1))
expected: (= {:resources {:bytes_scanned 0, :measured {:scan_ms 0.0}}}
             (:receipt (last result)))  actual: (not (= ... nil))
```

The merge repointed that witness at `study/format-ls-tree-edn`, which
published no receipt at all. **This is the witness the brief asked for and it
already existed** — mem003 wrote it, and composing study-ops made it fail. It
is now green, and the sabotage check below proves it is bound to the meter and
not to itself.

**2. `the-analyze-constructor-is-gated` (parser_admission_test.clj:444) — an
uncaught `ExceptionInfo` where a NAMED refusal belonged.** The delegations
dropped `named-plan-refusal`. Gating `clj-surgeon.analyze` (MEM-005) swapped an
uncatchable `StackOverflowError` for a typed `ExceptionInfo`; the four CLI
entrances are where a caller sees it, and the kernel deliberately throws.
Fixed by wrapping each delegation.

**3. `cli-ls-tree-refusal-bytes-match-the-frozen-golden` (study_test.clj:269) —
the empty-scan message published where the workspace lives.**

```
actual: (not (= "No Clojure files found under docs/intent/study-ops\n"
                "No Clojure files found under /home/forge/src/clj-surgeon-integ3/docs/intent/study-ops\n"))
"a refusal message must not publish where the workspace lives"
```

mem003's `no-clojure-files-message` named the canonical realpath;
MCP-OP-STUDY-026 requires the directory the CALLER asked for. Fixed at the one
call site; the parameter is now named `named`, not `abs`, so the next reader
cannot get it wrong by accident.

### Fixture drift, attributed by name

**4. `cli-ls-tree-edn-bytes-match-the-frozen-golden`.** MEM-005 publishes the
scan's wall clock in that receipt unconditionally, and **a wall-clock reading
can never be frozen in a byte-identity comparison.** This is the same
contradiction the second landing ruled on, arriving in a new channel. Resolved
the same way: the golden freezes the **hashed channel**, and the witness
projects both sides through `measured/hashed-channel` before comparing. Two
assertions were ADDED so the meter cannot be deleted to make the golden pass —
the same receipt must still publish a positive `scan_ms` and the exact
`bytes_scanned 6783`.

**5. `the-derived-spec-doc-set-matches-the-expected-set-exactly` and
`the-derived-audit-covers-exactly-the-registered-lane-intents`
(mcp_intent_contract_test.clj).** By design: that witness's own docstring says
*"a lane that adds an intent leaf adds one line here and one line to
`lanes-added-since-derivation` below — in the WITNESS, never in the production
registry."* study-ops adds `docs/intent/study-ops/study-ops-specs.md` and 43
ids. Both lines added; no production registry was touched.

**6. `the-public-tool-catalog-did-not-grow` and
`forms-text-carries-the-source-a-caller-asked-for` (mcp_study_test.clj).**
Attributable to lanes ALREADY on this branch, not to study-ops:
`alias_migration` is **bridge/q5z-alias-migration**'s tool, and
**bridge/parser-admission** moved `reader-cond?` in `analyze.clj` from 37-39 to
48-50. The clause study-ops owns — that IT added no public tool — is preserved;
the expected set names the fifth tool and its owner in a comment.

## The six holds, each with its evidence

### 1. MEM-005's `StackOverflowError` catch and the per-scan meter run through BOTH encoder families

`safe-outline` in `study.clj` is now the **union of two GO'd lanes, moved
verbatim from `core.clj`**: MEM-003's regular-file guard OUTSIDE (an `open(2)`
on a FIFO blocks and no `catch` can make a hang typed), MEM-005's
`StackOverflowError` catch INSIDE (an `Error` is not an `Exception`).

```
$ grep -n 'StackOverflowError' src/clj_surgeon/core.clj src/clj_surgeon/study.clj
src/clj_surgeon/study.clj:708:      (catch StackOverflowError _
src/clj_surgeon/core.clj:544:      (catch StackOverflowError _
```

The meter is threaded through `study/outline-take`, and it belongs to the
**cache, not the call** — a bounded MCP receipt GROWS by calling that function
repeatedly against one cache, so a per-call meter would have reported the last
increment as the whole scan's cost. mem003's streaming encoders keep theirs
untouched (`core/text-encoder` and `core/edn-encoder` both append
`bytes_scanned` plus the labelled measured line).

| encoder family | site | witness (green) |
|---|---|---|
| batch, shared kernel | `study/format-ls-tree-edn`, `study/format-ls-tree-text` | `test-format-ls-tree-edn` (ls_tree_test.clj:313) |
| streaming, CLI | `core/text-encoder`, `core/edn-encoder` | `clj-surgeon.measured-channel-test` (3 deftests), `clj-surgeon.ls-tree-budget-test` (88 `:max-results` drives through the paginated encoder) |
| MCP receipt | `ls-tree-bounded` -> `:resources` | `ls-tree-receipt-publishes-the-scan-cost-on-the-partitioned-channel` (new) |

**Sabotage check — the witness is bound to the meter, not to itself.** With the
one `binding [admission/*scan-meter* meter]` form removed from
`study/outline-take` and nothing else changed:

```
FAIL (ls-tree-receipt-publishes-the-scan-cost-on-the-partitioned-channel)  x6
FAIL (the-e6-cohort-shape-returns-rows-in-text-within-budget-and-metered)  x2
FAIL (study-ops-both-entrances-call-one-kernel)
```

Restored with `git checkout -- src/clj_surgeon/study.clj`; tree clean.

### 2. The measured partition reaches the MCP ls-tree receipt, text AND structured

`clj-surgeon.measured` is unchanged from the base (byte-identical). The MCP
`ls-tree` result now carries `:resources` as a **first-class field in every
format**, and `ls-tree-summary` renders both halves in the TEXT block — the
deterministic denominator plainly, the wall clock behind
`measured/text-measured-prefix`, because text has no keys and the partition has
to be visible in the bytes.

New witness `ls-tree-text-carries-the-scan-cost-the-receipt-carries` asserts
all four directions: the text carries `bytes_scanned`, the text carries the
measured prefix, `strip-measured-lines` LEAVES the denominator, and
`strip-measured-lines` TAKES the wall clock.

One defect this hold surfaced in my own first edit: appending the CLI's
trailing `:receipt` map to the MCP `files` array made it arrive at a caller as
**one more file with a null name** (caught by three existing study-ops
witnesses: `ls-tree-mode-returns-a-bounded-tree`, `ls-tree-ns-grep-filters-by-
path-not-content`, `a-source-path-through-a-symlink-is-a-named-skip`). Split
into `study/ls-tree-edn-rows` (rows, for the MCP) and `format-ls-tree-edn`
(rows + receipt, for the CLI).

### 3. The andon hardening survives — with one correction to the brief

```
$ grep -n '"-H"' src/clj_surgeon/core.clj
259:            args (concat ["find" "-H" (find-start-token dir)]
363:                     "find" "-H" (find-start-token dir)
$ grep -c -- '-print0' src/clj_surgeon/core.clj        -> 5
$ grep -c 'find-start-token' src/clj_surgeon/core.clj  -> 4
$ grep -c 'ls-tree-root-refusal' src/clj_surgeon/core.clj -> 4
$ grep -rn 'workspace-root-not-a-directory' test/
test/clj_surgeon/core_discovery_test.clj:107, :121   (the root-refusal witnesses)
```

**Correction, on the record: the brief's clause "`git grep -n 'System/exit' --
src` empty" is not satisfiable and was never true.** At the base `0a38e3d` that
grep returns **12 hits** across six namespaces, and at `a0b0520` it returns 7.
The andon requirement is narrower and IS met: study's kernel must not
`System/exit` on a bad root — it returns a typed `:workspace-root-not-a-directory`
refusal, and the exit code is the CLI's own contract, asserted by
`cli-ls-tree-refusal-bytes-match-the-frozen-golden` (`(is (= 1 (:exit result)))`).
**The merge added no `System/exit` site**, proven by set equality rather than
by counting:

```
$ diff <(git grep -h 'System/exit' HEAD~2 -- src | sed 's/^ *//' | sort) \
       <(git grep -h 'System/exit' HEAD   -- src | sed 's/^ *//' | sort)
(identical)
```

### 4. O2's 32 KB budget and evidence rows apply to the ls-tree output

`fit-public-result` / `max-public-result-bytes` (32,768) are unchanged from
`a0b0520` and now bound a receipt whose text block also carries the meter. The
over-budget path is a **typed** text abridgement that names `structuredContent`
(`! text abridged · %d of %d rows rendered · the complete receipt is in
structuredContent`, MCP-OP-STUDY-040), never a silent cut; the over-tree path
is `! bounded receipt · N files omitted · read_complete=false` with an
executable continuation.

`fit-public-result` measures through `inspect-summary`, so the budget is
enforced over the FINAL rendered text — the two new resources lines included,
not a pre-render estimate. Named green witness:
`ls-tree-public-result-is-bounded-by-the-declared-output-budget`
(mcp_study_test.clj:2023), which first asserts its own fixture actually
overshoots ("or the witness proves nothing") and then asserts the fitted
result is under budget, that the overshoot is `(or (false? (:ok fitted))
(pos-int? (:text_evidence_limit fitted)))`, and that the text contains both
`text abridged` and `structuredContent`. The E6 drive below exercises the
bounded-tree path at 32,383 of 32,768 bytes.

### 5. The intent registry

```
$ clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as c]) ..."
ok= true specs= 300 violations= 0        (base 0a38e3d reads 257 / 0)
```

**+43, and exactly `MCP-OP-STUDY-001` .. `MCP-OP-STUDY-043`** — the set the
derived-audit witness named as `added:` before its expected list was extended.
One top-level form, no duplicate ids, no production registry edit: the
registry is DERIVED by scanning `docs/intent/<leaf>/`, so the lane added a file
and touched no shared vector.

### 6. The E6 cohort's real shape, hand-driven end to end

A real MCP HTTP server on port **7960** (inside my allotted 7960-7962; started
by me, stopped by me, port confirmed down afterwards), real JSON-RPC
`initialize` -> `notifications/initialized` -> `tools/call`:

```
tools/call inspect_clojure {"mode":"ls-tree","dir":"src","grep":"defn","limit":16384}

PUBLIC RESULT BYTES: 32383      (budget 32768)
TEXT BLOCK CHARS:    15635
TEXT FILE ROWS:      7          (== structured `returned`)
structured: returned 7  file_count 77  project_count 2  truncated true  read_complete false
structured resources: {"bytes_scanned": 496440, "measured": {"scan_ms": 616.374}}
```

Text block, head and tail, verbatim:

```
inspect_clojure · ls-tree
  src · 2 projects · 7 of 77 files · 475.44 ms

── clj_surgeon (72 files; 7 shown)
...
clj_surgeon/agent_routing.clj  160 lines, 14 forms
clj_surgeon/alias_migration.clj  1241 lines, 65 forms
clj_surgeon/analyze.clj  609 lines, 28 forms
...
── total: 77 files; 7 shown, 70 omitted

! bounded receipt · 70 files omitted · read_complete=false
── resources: bytes_scanned 496440
── measured (not hashed): scan_ms 616.374
→ The receipt is already at the maximum limit; scan a subdirectory or add a grep pattern.
→ narrow_scope
```

Rows in the text block; inside the 32 KB budget; `bytes_scanned` on the hashed
side and `scan_ms` under `:measured`, in both channels. Pinned as
`the-e6-cohort-shape-returns-rows-in-text-within-budget-and-metered`.

## Gates — every ran-line executed on this branch's tip

| gate | result |
|---|---|
| `~/bin/suite-run bb test/run_all.clj` | `Ran 896 tests containing 7112 assertions.` / `0 failures, 0 errors.` (base 867/6999/0) |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 689 tests containing 8405 assertions.` / `0 failures, 0 errors.` (base 601/6326/0) |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| intent audit | `ok= true specs= 300 violations= 0` |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| `make memory-battery-self-test` | `generate_tree verification self-test: ok` / `generate_tree root-marker self-test: ok` / `generate_tree self-test: ok` / `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` — **flaky, read on** |
| `make memory-red-kernel` (flock) | `Ran 4 tests containing 25 assertions.` / `0 failures, 0 errors.` — `FLATNESS 60 {:xmx-mb 256.0, :heap-retained-peak-mb 14.44, :wall-ms 18617}`, `FLATNESS 600 {:xmx-mb 256.0, :heap-retained-peak-mb 14.84, :wall-ms 170712}` |
| CLI goldens `git diff 4480e3d..HEAD -- test-fixtures/` | 2 files; see below |
| `bash bench/anvil-arms/self-test.sh` | `anvil-arms self-test: 389 passed, 0 failed` |

### `memory-red` — the one non-deterministic gate, attributed

Four consecutive runs of the identical tree:

```
run 1  FAIL  giant 128m: admission scan under 50 ms   {:wall-ms 144, :scan-ms 62}
run 2  PASS  giant 128m: admission scan under 50 ms   {:wall-ms 125, :scan-ms 47}
run 3  FAIL  giant 128m: admission scan under 50 ms   {:wall-ms 108, :scan-ms 55}
run 4  PASS  giant 128m: admission scan under 50 ms   {:wall-ms 108, :scan-ms 13}
```

The other five assertions held on every run. **It is attributable to no branch
in this merge**, and the proof is a diff rather than a re-run: every file that
gate measures is byte-identical to the base `0a38e3d`.

```
$ git diff --stat 0a38e3d..HEAD -- bench/parser_admission/ \
      src/clj_surgeon/outline.clj src/clj_surgeon/parse_admission.clj
(empty)
```

It is a **50 ms wall-clock assertion on a shared 16-core box at load 8-14**,
with a 13-62 ms spread over identical bytes. It is NOT fixed here: fixing it
means amending a GO'd lane's threshold, which is not this seat's to amend —
the same reasoning the first integrator applied to the MEM-005/MEM-003
collision. Flagged for parser-admission's owner: **a wall-clock assertion at
this margin cannot distinguish a regression from a busy machine**, which is
the class the fleet already paid for once (the 250 ms gate at load 210).
Every run was under `flock /home/forge/tmp/suite.lock`.

### CLI goldens — what changed and why

```
$ git diff --stat 4480e3d..HEAD -- test-fixtures/
 test-fixtures/memory/mem_015_outline_fixture.clj          | 1067 ++++++++
 test-fixtures/study/ls-tree-existing-ops-edn.golden.txt   |    4 +-
```

The 1,067-line fixture is **bridge/read-path-memory's**, already on the
integration branch, and is an addition rather than a change. The one golden
this composition touched is the CLI EDN tree, and the whole delta is three
lines:

```
-   "[myapp.lib.jsutils :as jsu]"]}]
+   "[myapp.lib.jsutils :as jsu]"]}
+ {:receipt
+  {:resources {:bytes_scanned 6783, :measured {:scan_ms 0.0}}}}]
```

That is MEM-005's unconditional meter arriving in a channel study-ops froze
before it existed. Its `:scan_ms` is `0.0` **on purpose**: the witness compares
`measured/hashed-channel` projections, so the golden's wall clock is
deliberately not a number anyone should read, and two ADDED assertions stop
the meter from being deleted to make the golden pass. The **text** golden did
not change by a byte: the text encoders keep the quieter contract and append
resources only inside the refusal block.

## What is not established here

- **No adoption claim.** This branch makes the E6 shape work; it measures no
  cohort and no vs-native number. `docs/intent/study-ops/` still owes its E6
  free-choice cohort, and the tech-tree row says `BUILT, adoption unmeasured`.
- **The full `make memory-battery` was not run** — it is not in this landing's
  gate list, and the previous landing's INCOMPLETE verdict (four UNMEASURED
  reserved-peak lines, two pre-existing `held-scales-with-n` FAILs on
  `rename-ns-plan-full-match` and `workspace-sources-read-all`) is unchanged by
  anything here.
- **`memory-red`'s 50 ms assertion is not fixed**, only attributed. See above.
