NO-GO

# Sol fence review, round 15: preservation brief

- reviewed: sealed candidate `7c24e0203b542672ec87e867a7fe7b39cbc64218`
- branch tip accepted by the brief: `b30aa9e7b363c54cfc416d3db565f5fabb5f1470`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 14 `NO-GO` on PB-FENCE-021
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-022 — the behavioural probe does not bind the index pipeline to the probed function

R8 establishes that the function called by `--refusal-probe` agrees with the
declarative `refusal-kinds` table for its generated file maps. It does not establish
that the index pipeline calls that function. The candidate has two independent call
sites: the probe calls `file-refusal` at `bin/preservation-brief:883`, while
`refused-files` calls it separately at line 1343. The prose saying they are the SAME
function is not an executable invariant.

I exercised the attack requested in the round-15 brief in an isolated copy at
`/var/tmp/forge/item3/r15-probe-seam`. I left `file-refusal`, `refusal-kinds`,
`predicate-for`, and the probe unchanged, added this second dispatch:

```clojure
(defn pipeline-refusal [f]
  (if (seq (:declares f))
    {:kind "declaration-present" :detail "probe/pipeline drift"}
    (file-refusal f)))
```

and changed only the index call site from `(file-refusal f)` to
`(pipeline-refusal f)`. The copied `bin/preservation-tables-test` still exited 0:
all R1-R8 checks passed, including the 384-map behavioural property and both drift
controls. The source-shape pre-filter also stayed silent: there remained exactly one
definition named `file-refusal` and one refusal-kind collection.

The divergence is observable in the real pipeline, not merely in source shape. On a
two-commit repository containing `(declare later)` and two ordinary owners, the
sealed brief reported `refused_files: 0`, `base_owners: 2`, `cand_owners: 2`, and one
in-place body needing review. The mutated copy exited 3 and reported
`refused_files: 1`, `refusal_kinds: ["declaration-present"]`, and zero owners in
both inventories. Meanwhile its `--refusal-probe` remained equal to the table for
all 384 generated maps. A rebinding local to the CLI entry can hide the same class
of divergence.

Repair must make the test exercise the dispatch used by `refused-files`, not only a
second CLI call site that currently spells the same Var. It also needs this exact
pipeline-redirection attack as a behavioural drift control. That repair touches
`bin/`, which the ship-fix contract classifies as `HOLD reason=oracle-changed`, so I
left no `GO-WITH-FIX` patch.

## Candidate checks completed

- The unmodified candidate's `bin/preservation-tables-test` exits 0. R1-R8 pass;
  R8 checks seven triggerable refusal kinds, one documented defensive kind, 384
  generated maps, the round-13 literal branch, and the round-14 second definition.
- The generator varies every path read by the current declarative predicates:
  `:analyzer-failed?`; reconciliation `:ns-count`, `:unanalysed-features`,
  `:ns-span-agrees?`, `:unlocated`, `:unreported`, and `:cond-gap`; and
  `:require-mismatch :ok?`. I found no current predicate field omitted from the
  Cartesian product. Random extra keys do not close PB-FENCE-022 because the
  production pipeline can call a different function.
- A bounded replay reran clean plus wrong-binding, silent-promotion,
  dropped-comment, omitted-owner, and load-order-form rows before I stopped the
  remaining cold rows once PB-FENCE-022 was reproduced within the review timebox.
  The table ratchet separately confirms all 20 required replay row names remain.
- `04110f80..b30aa9e7` changes only `bin/preservation-brief` (+46/-11) and
  `bin/preservation-tables-test` (+117/-31); `git diff --check` is clean. The tip is
  eighteen commits over `3ea3803e`.
- The three implementation files import no `clj-surgeon.*` namespace. Receipt
  consumption remains isolated to receipt comparison and section C; sections A/B
  use the independently derived state. Replay starts from `git archive` of the real
  Curtaincall CFP trees `d9205abc` and `65ad613b`, not a synthetic Cell C.
- Section B remains a loud hard stop above section A and exits 3 when non-clear, so
  the two B-only defect classes are not presented as weaker signals.
- Section 9 honestly preregisters 21 specimens and 63 observations, gives each
  protected class two observations per arm, explicitly refuses to estimate a catch
  rate at that denominator, and defines the safety co-primary as strict dominance
  before any run.

## Decision

The round-14 second-definition defect is caught, but the behavioural oracle can be
kept honest while the production pipeline is redirected to an unenumerated refusal
path. The reviewer still has to inspect the relationship between the probe and the
pipeline manually, so the requested proof-burden boundary remains open.


> END RECEIPT (fence-run): worktree HEAD at review exit = 7c24e0203b542672ec87e867a7fe7b39cbc64218 = fenced sha.
