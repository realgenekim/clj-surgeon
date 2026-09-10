NO-GO

# Sol fence review, round 13: preservation brief

- reviewed: sealed candidate `c614ed95e8b8d6bc85a89125b8a20def64012081`
- branch tip accepted by the brief: `77ee1e41de2b75cd1fc6045ce794067c76fa59e0`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 12 `NO-GO` on PB-FENCE-019
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-020 — the refusal enumeration is not the dispatch authority

`refusal-kinds` and `defensive-refusal-kinds` are exported by
`bin/preservation-brief`, but `file-refusal` does not read or dispatch through
either collection. It remains an independent `cond` containing eight literal
`:kind` strings. R8 compares only the exported triggerable enumeration with the
independently written `refusal-specimens` map. Thus those two tables agree today,
but a new valid-input branch can be added to the real refusal function without
appearing in either table, and R8 remains green.

I executed that exact negative control in an isolated copy under
`/var/tmp/forge/item3/r13-enum-drift`: I inserted a valid-input branch in
`file-refusal` for `(seq (:declares f))`, returning the new kind
`declaration-present`, and changed neither exported collection nor R8. The test
still exited 0 and printed:

```text
R8  refusal kinds: 7 | generated specimens: 7 | defensive (documented, not triggerable): 1
PASS — every table entry carries its falsifier, including a generated shadow witness.
```

The new branch is not hypothetical dead input: the separately executed
metadata-wrapped `declare` specimen is valid, reports `declares=1`, and reaches no
earlier refusal in the sealed candidate. The test therefore still permits exactly
the drift PB-FENCE-019 was meant to make impossible. The `file-refusal` docstring
also says every returned kind is in `refusal-kinds`, although its retained
`unplaced-definition` return is intentionally only in
`defensive-refusal-kinds`.

The repair must make one rule collection authoritative for the return surface and
ordering (including the defensive entry), or mechanically assert that every kind
returned by `file-refusal` is exactly the union of the triggerable and defensive
collections and that each triggerable entry has a generator. That repair touches
`bin/`, which the ship-fix contract classifies as `HOLD reason=oracle-changed`, so
I did not leave a `GO-WITH-FIX` patch.

## Candidate behavior that passed

`bin/preservation-tables-test` exits 0. Its seven generated specimens each reach
their expected refusal kind, every refused-file counter is zero, and the defensive
set carries one non-overlapping documented entry. The branch-definition repair is
effective: the analysed `defentity Gadget` specimen now reaches
`unreported-branch-def` rather than being hidden by unrelated `.cljc` definitions.

The twelve focused probes and five controls were re-executed against the sealed
bytes:

- All eight same-token/different-resolution probes (`wrong-binding`,
  `are-binding`, `uppercase-alias`, `kind-collision`, `referred-testing`,
  `refer-all-shadow`, `use-shadow`, `refer-all-external`) reduce relocated-body
  preservation from 99 to 98. None calls the attacked body preserved.
- `two-ns` reaches `ns-count`; `cljc-unanalysed` reaches
  `unelaborated-platform`; the metadata-wrapped declaration reports one declaration
  and no new forward reference.
- A valid file carrying both an unelaborated `:bb` branch and a parser/resolver
  require mismatch reports exactly one distinct refusal kind:
  `unelaborated-platform`. That is the documented `file-refusal` precedence; the
  file contributes no owners or downstream counters.
- I could not trigger `unplaced-definition` with a valid input. The valid shapes
  control covers metadata-prefixed definitions, a definition nested in a top-level
  `do`, declarations, protocols, interfaces, multimethods and no-Var forms; the
  analysed `.cljc` control covers definitions in both elaborated branches. Both
  report `reconcile_unlocated=0`. All seven generated refusal specimens also report
  zero unlocated definitions.
- Clean Cell C remains `99/141` relocated and `32/64` in-place, with zero refused
  files and zero reconciliation, require-order, undefined-alias and forward-reference
  failures. The `shapes`, analysed-CLJC, spliced-require and comment-require controls
  are likewise unrejected with their expected zero failure counters; the comment
  control retains its four expected outside-`ns` dynamic-review obligations.

## Other requested checks

- `55374a28..77ee1e41` changes only `bin/preservation-brief` (+37/-6) and
  `bin/preservation-tables-test` (+48/-14); aggregate diff is +85/-20 and
  `git diff --check` is clean.
- The three implementation files still import no `clj-surgeon.*` namespace.
  Receipt reading remains confined to the receipt-comparison derivation and Section
  C; Section A/B inventories do not consume it.
- Replay construction still begins with `git archive` of the real Curtaincall CFP
  trees `d9205abc` and `65ad613b`, then plants defects on those snapshots.
- Section B's promotion and require-order hard stops are unchanged and remain above
  Section A. The current delta does not weaken their prominence.
- Section 9 remains an honest preregistration: 21 specimens, two observations per
  protected class per arm, an explicit refusal to estimate catch rates at that
  denominator, and strict dominance as the safety co-primary before any run.

## Decision

The seven current refusal specimens and all requested behavioral probes pass, but
the claimed exhaustive ratchet is not connected to the function whose return
surface it claims to exhaust. A valid ninth refusal branch leaves R8 green. This is
the same proof-burden boundary as PB-FENCE-019, so the candidate must not ship.


> END RECEIPT (fence-run): worktree HEAD at review exit = c614ed95e8b8d6bc85a89125b8a20def64012081 = fenced sha.
