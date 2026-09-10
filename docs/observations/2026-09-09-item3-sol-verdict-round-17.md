NO-GO

# Sol fence review, round 17: preservation brief

- reviewed: sealed candidate `3beee531f8301633ca9d67b8a2238cf3041cf959`
- branch tip accepted by the brief: `10b77576307c6c1a7922baa82cc08a32d024ba2e`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 16 `NO-GO` on PB-FENCE-023
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-024 — the strict table parser accepts a blank required cell

PB-FENCE-023 is repaired for sequence identity: the Markdown/JSON comparison now
preserves order and multiplicity, and both the duplicated-row and reordered-row
mutations are rejected. The parser also rejects an escaped pipe, an embedded
newline, a missing final column, and a second reconciliation-table header.

It does not implement the repair's stated blank-row contract. At
`bin/preservation-tables-test:130-157`, `parse-refusal-table` validates only the
number of pipe-delimited cells. It does not validate that any of the four cells is
nonblank. Directly loading that function from the candidate and passing
`| base | src/a.clj | ns-count |  |` returned:

```clojure
{:rows [["base" "src/a.clj" "ns-count"]], :errors []}
```

This is an executable oracle escape, not only an imprecise diagnostic. I temporarily
changed the real `refusal-rows` producer at `bin/preservation-brief:889` to overwrite
only `:detail` with the empty string. The generated real `ns-count` table then had
two rows whose `why` cells were empty:

```text
| base | src/refused.clj | ns-count |  |
| candidate | src/refused.clj | ns-count |  |
```

The JSON still contained the correct ordered base/candidate `tree`, `path`, and
`kind` rows. The complete copied-by-mutation boundary was not needed: the unmodified
`bin/preservation-tables-test` run against that temporarily mutated real producer
exited 0 and printed PASS, including the 384-map property and all five behavioural
drift controls. This happens because both the JSON projection and the parsed
Markdown projection deliberately omit `why`; no assertion observes the blank cell.

A reconciliation row without its explanation hands the reviewer the very inference
work this table is meant to remove, and contradicts the candidate's explicit claim
that blank rows are reported errors. Repair must reject blank required cells (at
minimum `tree`, `file`, `refusal`, and `why`) and retain the blank-why mutation as a
behavioural drift control. That repair touches `bin/`, which the ship-fix contract
classifies as `HOLD reason=oracle-changed`; I therefore left no `GO-WITH-FIX` patch.
The temporary mutation was restored byte-for-byte before this verdict was written.

## Candidate checks completed

- The unmodified candidate's `bin/preservation-tables-test` exits 0. R1-R8 pass;
  R8 exercises seven real refusal specimens, the 384-map dispatch property, and all
  five required drift controls. The duplicate and reorder controls now fail their
  mutated properties, while the sealed bytes pass on revert.
- The requested sixteen replay probes were re-executed from the real-tree scratch
  repository: `local-shadow`, `symlink-escape`, `external-macro`, `are-binding`,
  `uppercase-alias`, `kind-collision`, `referred-testing`, `refer-all-shadow`,
  `use-shadow`, `refer-all-external`, `meta-owner`, `cljc-unanalysed`,
  `spliced-require-reorder`, `comment-require`, `two-ns`, and `meta-declare` all
  raised their expected targeted signal. The five replay controls (`clean`,
  `shapes`, `cljc-analysed`, `spliced-require-control`, and
  `comment-require-control`) remained silent for those targeted failure counters.
- Clean Cell C remains 141 relocated / 99 mechanically preserved bodies and 64
  in-place / 32 preserved bodies, with zero reconciliation failures, require
  reorders, undefined aliases, stale sites, and new forward references.
- The two B-only planted defects are impossible to overlook. `silent-promotion`
  puts `9 PRIVACY CHANGES` in the top hard-stop banner; `load-order-requires` puts
  `1 files whose :require list lost the sorted order` there. The banner precedes
  section A and explicitly says a section-B signal is not weaker.
- Independence remains intact: none of the three implementation files requires a
  `clj-surgeon.*` namespace. Receipt reading remains confined to the optional
  receipt-comparison state and section C; sections A and B derive from the two git
  trees. Replay construction still begins with `git archive` of real Curtaincall
  CFP trees `d9205abc` and `65ad613b`, not a synthetic Cell C.
- Section 9 remains honestly preregistered before any run: 21 specimens and 63
  observations are arithmetically specified, every protected class has two
  observations per arm, the text explicitly refuses a catch-rate estimate at that
  denominator, and the safety co-primary is strict dominance.
- `e7952518..10b77576` changes only `bin/preservation-tables-test` (+61/-8), and
  `git diff --check` is clean. The sealed candidate tree equals the branch-tip tree;
  the tip is twenty commits over the stated base.

## Decision

The ordered-vector repair closes PB-FENCE-023, but its strictness oracle still
accepts reconciliation rows whose required explanation is blank. The reviewer must
therefore manually confirm that every refusal row says why it was refused, so the
proof-burden boundary remains open.
