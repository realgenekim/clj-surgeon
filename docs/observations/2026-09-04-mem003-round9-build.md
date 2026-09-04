# MEM-003 second landing — round nine build record (the COMPOSITION round)

*Written 2026-09-04T20:48:14Z by forge-anvil on `bridge/integration-2026-09-03-mem003`.
Subject: the fourteen composition failures round eight ATTRIBUTED but did not resolve, closed
under the seat's decision — OPTION (i), the census adapts to the measured contract.*

## Headline

**The composition is green.** `Ran 870 tests containing 13232 assertions. 0 failures, 0 errors.`
twice on a fresh clone at the tip, and `Ran 935 tests containing 7403 assertions.` on babashka.
Fourteen JVM failures and two babashka failures existed at `cb14686c` and none exist now.

| tree | JVM suite | babashka |
|---|---|---|
| the composition `cb14686c` | `Ran 870 tests containing 13082 assertions. 11 failures, 3 errors.` | `Ran 935 tests containing 7403 assertions. 2 failures, 0 errors.` |
| **the tip `57a409b8`** | **`Ran 870 tests containing 13232 assertions. 0 failures, 0 errors.`** | **`Ran 935 tests containing 7403 assertions. 0 failures, 0 errors.`** |

**Nothing under `test-fixtures/` changed.** `git diff --stat cb14686c..HEAD -- test-fixtures/`
is EMPTY. No census golden carries a receipt, so the partition move touched none of them, and no
fixture was edited to fit a result.

## Item 1 — the census composition (`5ed115e9`)

Option (i) applied as decided: the landed relation-census verb adapts to the measured contract.

- `relation-census/plan` and `mcp-relation-census/execute-in-context!` take their phase ticks
  through `measured/start` + `measured/elapsed-ms`. Neither is a raw clock read any more, and
  each phase figure is a TAGGED READING carrying its own provenance.
- `build-receipt` publishes `(measured/measured {:phases_elapsed_ms phases})` — built there
  rather than left to the finalizer's relocation, so `bound-receipt` measures the bytes the wire
  actually carries against its 4 KB budget.
- `census-output-schema` declares `measured` and REQUIRES it (MCP-OP-SCHEMA-001). The
  top-level `elapsed_ms` and `phases_elapsed_ms` properties are gone: they are shapes this tool
  no longer produces, and a schema promising the old wire is a second contract.
- `:phases_elapsed_ms` joins `measured/measured-field-names` and the shared
  `measured-output-schema`. The name is now in the invariant's vocabulary, so a future site
  publishing it top-level is RELOCATED by `partition-measured` rather than trusted.
- The CLI census entrance has no shared finalizer, so it builds the same partition itself. Left
  alone it would have published a `Reading` record on its wire under `:phases`.

MCP-OP-CENSUS-011, -013, -023 and -031 are amended to say the phases live inside `measured`.
**The intent is preserved and only the partition moves:** every phase that ran is still named with
its own numeric figure; -013 still requires it.

### THE CAUSE OF THE THREE "finite and non-negative" FIXTURE ERRORS

It was neither a phase that never started nor a fixture that skips a constructor. It was **one
line, and the refusal's own payload named it**:

```
ERROR in (every-continuation-either-entrance-emits-fits-the-byte-ceiling) (mcp_operation.clj:19)
  actual: clojure.lang.ExceptionInfo: MCP elapsed time must be finite and non-negative
  {:error-type :invalid-mcp-elapsed-time, :elapsed-ms nil}
```

`:elapsed-ms nil` is the whole diagnosis. The census `summary` renderer read
`(:elapsed_ms result)` as a **top-level** field. The measured finalizer relocates that field into
the `measured` block, so the lookup returned `nil` and `format-elapsed-ms` refused it — typed,
correctly — on a receipt that was otherwise complete. `mcp-operation/elapsed-ms` is the
partition-aware reader **every other tool's summary already uses**; the census, which landed while
the partition was landing on a branch, was the one that did not. The fix is at the reader, in
product code. The three fixtures are unchanged.

The fourth failure in that family is the same defect seen from the other side:

```
FAIL in (every-declared-refusal-shape-carries-no-field-over-the-ceiling)
the drives still cover every refusal the tool declares
  actual: ... #{nil ...}   -- :server-not-initialized missing, nil in its place
```

The throw escaped one refusal drive, so that drive's `:error_type` never reached the enumeration
and a `nil` took its place. One cause, four symptoms.

### The refusal-type enumeration, re-derived on the composed tree

**147, unmoved.** `refusal-kinds-in-source` is a derivation over source and this round added no
new `:error-type` constant (`:invalid-mcp-elapsed-time` already existed and is already pinned).
The branch's own witness re-derives and compares in both directions on every run and is green:
count 147, nothing extra in source, nothing missing from the pin. The pin's docstring — re-written
at the merge one round ago — needed no edit.

## Item 2 — the eight `reader_eval_fence_test` failures (`57a409b8`)

**The prose did not change. The refusal LINE was absent, and the defect is this branch's, not the
trunk's census landing.** Attributed by driving both real launchers by hand on one planted tree
(`deps.edn` carrying `{:paths ["../root-outside"]}`):

```
trunk a8c800a0 (bb launcher)          branch cb14686c (bb launcher)
── total: 0 files, 0 forms            ── total: 0 files, 0 forms
── source_paths_outside_project: 1 entry
   root  "../root-outside"  refused: it resolves outside the project root
```

`discover-projects`, `fenced-source-paths` and `source-path-refusals` were then probed on both
trees and returned **identical** values
(`{:paths [], :refused 1, :refused-entries ["\"../root-outside\""]}`), so the fence itself never
weakened and nothing outside the tree was read on either side. What changed is the ENCODER:
MEM-003 replaced `format-ls-tree-text` on the CLI path with the streaming `text-encoder`
(`src/clj_surgeon/core.clj:1116`), and that encoder carries only the refusal blocks that existed
when it was written. The trunk's MCP-OP-SHELL-ARGV-006 escaping-paths block landed afterwards, in
the batch formatter the streaming path no longer calls.

**What the witness now asserts, and why it was not weakened.** The failing assertion is
`(str/includes? out spelled)` at `test/clj_surgeon/reader_eval_fence_test.clj:200`: the refusal
must NAME the entry **as the caller spelled it**. That is the contract, not prose —
MCP-OP-SHELL-ARGV-006 requires a skip to be named and counted, and `source-path-refusals`'
docstring (`src/clj_surgeon/core.clj:855`) states why silence is worse here than elsewhere: a
build file naming only escaping paths yields a completeness claim over a walk that never happened.
**The sentence "refused: it resolves outside the project root" is asserted nowhere.** The witness
reads the caller's own spelling and separately asserts the resolved TARGET does not appear
(`reader_eval_fence_test.clj:205`). So the witness is untouched and the encoder is fixed.

The EDN streaming encoder carried the same hole and is fixed in the same commit: the batch
`format-ls-tree-edn` publishes `:source_paths_outside_project` in its trailing receipt and the
streaming one did not, so a machine reader of the bounded path saw a receipt that looked complete
over a tree half of which was fenced. Both blocks are conditional, so an ordinary scan's text and
receipt are byte-identical to what they were.

**Reported, not fixed:** `discover-projects-grep` (`core.clj:1283`) drops a project with no
files whatever it refused, where `discover-projects` deliberately KEEPS one that refused
something. Same class, one call site over, on the trunk's side of the merge, and no witness drives
it. Named here rather than changed inside this branch.

## Files changed, and why each

| file | why |
|---|---|
| `src/clj_surgeon/relation_census.clj` | phase ticks through `measured` |
| `src/clj_surgeon/mcp_relation_census.clj` | schema partition, receipt partition, summary reads `mcp-operation/elapsed-ms` |
| `src/clj_surgeon/measured.clj` | `:phases_elapsed_ms` joins the name vocabulary |
| `src/clj_surgeon/mcp_operation.clj` | `phases_elapsed_ms` declared in the shared measured schema |
| `src/clj_surgeon/core.clj` | CLI census builds its own partition; both streaming encoders carry the escaping-paths skip |
| `test/clj_surgeon/mcp_relation_census_test.clj` | assertions read the partition; schema witness pins the new shape AND the absence of the old |
| `docs/intent/relation-census/relation-census-specs.md` | CENSUS-011, -013, -023, -031 amended |
| `docs/intent/relation-census/relation-census-design.md` | the Phases and Receipt sections follow |
| `test-fixtures/` | **nothing** |

## Gates, on a FRESH `git clone` at the tip `57a409b8`

Cloned from the remote and fetched inside, so the refs are current;
`git status --porcelain` empty, HEAD verified. Fixtures lived only under
`/var/tmp/forge/mem003r9-fx`. No server was started; none of 7888 / 7890 / 7894 / 7895 was
contacted. Nothing was pushed to `main`; the trunk was never merged into.

| gate | result | load at start |
|---|---|---|
| JVM suite, run 1 | `Ran 870 tests containing 13232 assertions.` `0 failures, 0 errors.` | 11.89 |
| JVM suite, run 2 | `Ran 870 tests containing 13232 assertions.` `0 failures, 0 errors.` — identical | 9.41 |
| babashka suite | `Ran 935 tests containing 7403 assertions.` `0 failures, 0 errors.` | 8.64 |
| operation oracle | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | — |
| intent audit | `intent audit: :ok true :specs 410 :violations 0` | — |
| txn kernel warnings | `kernel warning check: 2 namespace(s), 0 warning(s)` | — |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` | 8.62 |
| tmp-leak ratchet | `tmp-leak ratchet witness passed` | — |
| admit-analyzer memory self-test | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` | — |
| battery self-test | `Ran 32 tests containing 171 assertions.` `0 failures, 0 errors.` | — |
| `make census-battery` | `:BATTERY-RESULT {:test 27, :pass 1336, :fail 0, :error 0}` — the census's own, still passing on the composed tree | — |

## The full memory battery, ONCE

Under `flock /home/forge/tmp/suite.lock`, a FRESH `MEMBAT_ROOT=/home/forge/tmp/membat-r9`,
reference built explicitly first, never `MEMBAT_ALLOW_ANY_ROOT`. Load at start: 10.23.

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.8, :limit 3.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5}
  … 9 TREND lines …
  UNMEASURED reserved-peak-over-budget ×4
receipt: /home/forge/tmp/membat-r9/receipts/20260904T210010.777638109Z-battery.edn
```

Parity read from the RECEIPT rather than the console:

```text
cells: 48
distinct :reference-mismatch: (nil)
reference-mismatch cells: 0
tool-errors: ()
:attestation :jvm "21.0.12" :head-sha "57a409b8b7b8b8804f8dd47408579c2ee27d290a"
```

**Exactly the state round eight recorded**, and exactly what the brief expected: `FAIL
(INCOMPLETE)` on MEM-001's lane only, 48 cells, **0 reference mismatches**, the same two
`held-scales-with-n`, four `UNMEASURED`. All four are MEM-001's lane, pre-existing at the base and
unchanged in kind by this round. The attestation names `57a409b8`, the item-2 commit; the only
commit after it is this docs file, so the battery attests the tip's code exactly.

## Merge with the trunk

`git merge-tree --write-tree HEAD origin/MCP/main` — **exit 0, clean**, checked against trunk
`b153f6aa` (the trunk moved three times during this round: `a8c800a0` at the merge, then
`4cb4cfd0`, `69c859c6`, `b153f6aa`; every move was a records/docs commit).
