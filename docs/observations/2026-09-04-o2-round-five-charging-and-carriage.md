# O2 round five — every rendered byte is charged, and a short spelling is not evidence

2026-09-04 06:48Z · seat forge@anvil · branch `bridge/study-ops-mcp` · base `515e8109`

Round four was NO-GO on a regression it introduced. This is what the four items
cost and what they bought, measured on ONE fixed fixture at both revisions so
the comparison is not confounded by the sources changing underneath it
(`/var/tmp/forge/o2r5-fx/big`: a 140-form and a 30-form namespace; and
`/var/tmp/forge/o2r5-fx/proj`: one three-defn namespace with a require).

## The regression, before and after — a two-file `outline` batch

| | `515e8109` (round 4) | tip (round 5) |
|---|---|---|
| `content[0].text` | **151 chars** (the notice rung) | **12,473 chars** |
| published bytes | 19,994 | 32,755 |
| unspent budget | **12,774** | **13** |
| uncarried leaves | 1,055, undeclared | 785, declared `98 of 882 rendered` + pointers |
| `text_evidence_limit` | nil | 12,266 |

The cause was one unbudgeted line. `fact-section`'s `dropped:` named one
pointer per dropped leaf OUTSIDE the allowance, so it GREW as the allowance
shrank — 22,785 characters of text at allowance 0 against 13,423 at allowance
9,434. `fits?` was therefore not monotone, and `fit-public-result`'s bisection
probed the midpoint, missed, and recurred into the half that can never fit.

Both halves are fixed, because either alone leaves it reachable: `fact-block`
charges the whole section — lines, count header, and a `dropped:` line bounded
to eight pointers plus `(+N more)` — and `fit-public-result` scans measured
candidates (32 across the band, then a refinement pass) instead of bisecting.
The fit costs about 560 ms on the worst call we have, on the overshoot path
only. A witness pins monotonicity across the whole band, and another pins that
the fit publishes at least what a brute-force sweep finds.

## What carriage cost — small reads on the fixed fixture

| mode | `515e8109` | tip | delta |
|---|---|---|---|
| outline | 1,776 | 2,830 | +59% |
| deps | 1,553 | 2,248 | +45% |
| topo | 1,383 | 1,877 | +36% |
| ls-tree | 1,004 | 1,168 | +16% |

That is the price of section 4. Every COLLIDABLE leaf — every number, every
boolean, every spelling shorter than sixteen characters — now carries its own
`pointer=spelling` line, because a short spelling found in the text is not
evidence that the text carries THAT leaf. On the class-ratchet fixture, leaves
another value of the same type could replace with a byte-identical text went
from 8–15 per operation to 0, and leaves whose REMOVAL left the text unchanged
from 8–18 to 0. A threshold of eight rather than sixteen would have saved 3.5%
and left `operation` and every mode name invisible.

A second defect fell out of the removal audit: the status lines spelled
`read_complete=true` as a CONSTANT — which is exactly the label form the
carriage predicate looks for — so the leaf was reported carried by a string
that had never read it. All five now spell the receipt's own value.

## The envelope

`mcp-operation` declares `envelope-keys`, `envelope`, `finalized?` and
`request-elapsed-ms`; the budget gate and every text renderer ask it rather
than naming `:elapsed_ms`. The third of those was hiding behind the first two:
with the guard fixed, the fit accepted a nested `measured` block and then threw
inside `format-elapsed-ms` while rendering the very summary it was measuring.
A witness drives the MEM-003 five-field `measured` block and the top-level
clock through one over-budget receipt and through a gate-built refusal.

## Residuals, reported rather than folded in

- A DISTINCTIVE spelling is still carried by its own characters, so
  `results[0].file` can be replaced by `"clj"` in a decoy test and pass. Both
  new witnesses are scoped to collidable leaves and MCP-OP-STUDY-044 now says
  so.
- A receipt that spells one fact twice (`results[0].file` and
  `results[0].outline.file`) cannot make each copy independently removable.
- Small reads cost 16–59% more bytes than round four. Bytes are not tokens and
  token cost remains unmeasured.
- One child-process teardown flake in `mcp-prepared-wire-test`
  ("Stream closed" in `stop-child!`) appeared once on a loaded box and not in
  the runs before or after it.

## Gates at the tip

```
~/bin/suite-run bb test/run_all.clj                 731 tests / 6,023 assertions / 0 failures, 0 errors
~/bin/suite-run clojure -M:clj-surgeon/mcp-test     493 tests / 6,382 assertions / 0 failures, 0 errors
make mcp-operation-oracle                           pass
intent audit                                        {:ok true, :violations []}
git diff --exit-code 4480e3d..HEAD -- test-fixtures/  clean
```
