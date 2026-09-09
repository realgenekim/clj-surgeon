NO-GO

# Sol fence review, round 12: preservation brief

- reviewed: sealed candidate `ed5708d79666cf65885b486843ee0645833b7a5f`
- branch tip accepted by the brief: `55374a284181a134062d194d7f40288d2a1c519c`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 11 `NO-GO` on the PB-FENCE-017 refusal residue
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-019 — R8 does not generate a specimen for every refusal kind

The candidate names eight refusal kinds in `file-refusal`, but R8's
`refusal-specimens` contains only three entries:

- `unparsable`
- `ns-count`
- `unelaborated-platform`

There is no generated R8 specimen for `ns-span`, `unplaced-definition`,
`unreported-owner`, `unreported-branch-def`, or `require-mismatch`. The executed
test makes the gap visible itself:

```text
R8  refusal specimens: 3
PASS — every table entry carries its falsifier, including a generated shadow witness.
```

Thus five branches of the new refusal boundary can be broken or misordered while
R8 remains green. This contradicts the round's explicit acceptance condition of
one generated specimen per refusal kind; the PASS headline overclaims the coverage.
The repair belongs in `bin/preservation-tables-test`, which the ship-fix contract
classifies as `HOLD reason=oracle-changed`. I therefore did not leave a
`GO-WITH-FIX` patch.

## Round-11 blocker retest

PB-FENCE-017's behavioral residue is repaired. The exact `two-ns` row reports:

```text
refused_files=1
refusal_kinds=["ns-count"]
reconcile_failures=2
ns_count_failures=2
dynamic_requires=0
undefined_alias_sites=0
base_owners=0
cand_owners=0
declares=0
reader_conditionals=0
```

The path `src/two.clj` appears only in the two base/candidate refusal rows. The
`cljc-unanalysed` row similarly reports one refused file of kind
`unelaborated-platform`, one reconciliation failure, and zero owners, bodies,
requires, aliases, declarations, no-Var forms, conditionals, sites, and forward
references. Its path appears only in its refusal row.

## Eleven probes and five controls

The full `bin/preservation-replay` exited 0. Each of the eight same-token,
different-resolution probes — `wrong-binding`, `are-binding`, `uppercase-alias`,
`kind-collision`, `referred-testing`, `refer-all-shadow`, `use-shadow`, and
`refer-all-external` — reduced Cell C relocated preservation from 99 to 98. A did
not call the attacked body preserved. `two-ns` and `cljc-unanalysed` refused the
whole path as above. `meta-declare` reported one declaration and no new forward
reference.

The five controls held:

- Clean Cell C: `refused_files=0`, 99/141 relocated bodies preserved, 32/64
  in-place bodies preserved; reconciliation, namespace-count, dynamic-require,
  undefined-alias, forward-reference, require-reorder, and require-sort counters
  are zero.
- `shapes`: no refusal, reconciliation, namespace-count, dynamic-require,
  undefined-alias, or forward-reference failure.
- `cljc-analysed`: no refusal, reconciliation, namespace-count, unanalysed
  platform, dynamic-require, undefined-alias, or forward-reference failure.
- `spliced-require-control`: no refusal, reconciliation, namespace-count,
  require reorder, sort break, dynamic require, undefined alias, or forward
  reference.
- `comment-require-control`: no refusal, reconciliation, namespace-count,
  require reorder, sort break, undefined alias, or forward reference; its four
  comment namespace usages remain the expected dynamic-review obligations.

## Manual R8 negative control

In an isolated archive of the sealed candidate under `/var/tmp/forge`, I added a
top-level output line containing `src/refused.clj` to the scratch copy of
`bin/preservation-brief`. `bin/preservation-tables-test` exited 1 and named the
expected R8 failures for `ns-count` and `unparsable`:

```text
R8 refused file leaked into another section — ns-count: `src/refused.clj` appears outside the refusal rows and the hard stop
R8 refused file leaked into another section — unparsable: `src/refused.clj` appears outside the refusal rows and the hard stop
```

The scratch-only mutation did not touch this repository.

## Other required checks

- Independence holds: the three implementation files import no
  `clj-surgeon.*` namespace and read no source under `src/`. Receipt parsing is
  confined to the receipt-comparison derivation and Section C; it supplies no
  Section A or B inventory.
- The replay initializes its scratch repository from `git archive` of the real
  Curtaincall CFP trees `d9205abc` and `65ad613b`, then plants defects on those
  snapshots. The principal replay is not a synthetic replacement.
- The Section-B-only promotion and require-sort defects are hoisted above A into
  the `HARD STOP`, immediately after “Do not consume the preservation figures
  below and stop” and “every one of them needs a human.” Their presentation is
  loud enough; they need not be relabelled as A failures.
- Section 9 honestly preregisters 21 specimens across C/T/N, gives every
  protected class two observations per arm, explicitly refuses to estimate a
  catch rate, and makes strict dominance the safety co-primary before any run.
- `95b0ff3f..55374a28` changes only `bin/preservation-brief` (+69/-29) and
  `bin/preservation-tables-test` (+64/-0). `git diff --check` is clean.
- `bin/preservation-tables-test` exits 0 on the sealed candidate, but its R8
  coverage claim is the blocker above.

## Decision

The whole-file refusal behavior closes PB-FENCE-017 and the requested probes and
controls pass. The candidate nevertheless does not satisfy its explicit R8
contract: five of eight refusal kinds have no generated witness while the table
test reports PASS. Do not ship this candidate.


> END RECEIPT (fence-run): worktree HEAD at review exit = ed5708d79666cf65885b486843ee0645833b7a5f = fenced sha.
