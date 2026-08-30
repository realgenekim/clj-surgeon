# Transform verbs cover the small edits; the escape hatch owns the bytes

## Verdict

Gene's hypothesis is directionally right and product-incomplete.

A small, closed, no-eval verb algebra exactly expresses **182 of 332 pairs (54.8%)**, but
those pairs occupy only **33,678 of 211,165 emitted `to` JSON-scalar bytes (16.0%)**. At the
write level, **60 of 150 eligible writes (40.0%)** fit in at most three closed verbs; the
other **90 eligible writes (60.0%)** need `replace-subform-freeform`. The escape hatch carries
**84.1% of target bytes**. It is not an edge case. It is the interface.

The byte result is harsher. Encoding every pair as its shortest discovered program costs
**211,084 bytes** against **211,165 actual emitted `to` bytes**: **81 bytes / 21 estimated
tokens saved corpus-wide** before any outer protocol cost. Add a modest 16 bytes per program
and the algebra loses **5,231 bytes / 1,307 estimated tokens**. Selective routing is better:
use verbs only for the 182 closed pairs and leave the 150 escapes on today's grammar. That
saves **9,081 bytes / 2,270 estimated tokens**, just **1.44% of all 630,138 request bytes**.

The registered verdict gate therefore lands at **qualified support by exactly its lower
boundary**—40.0% of eligible writes fit in three verbs—but the product warning fires twice:
the escape share is above 30%, and above 50% by eligible writes. The universal “programs,
not text” interface does not earn its byte cost from this corpus. A selective compiled path
does, weakly.

The sibling splice study wins the design fork. Its realistic selective projection saves
**49,656 bytes**, versus **9,081 measured bytes** for selectively routed verbs and **81 bytes**
for the universal verb-plus-escape encoding. Splices also win in both request families. The
right next buildability screen is an AST-addressed splice primitive with named verbs as
readable sugar, not a growing catalogue that tries to eliminate freeform replacement.

## Preregistration and receipts

The taxonomy, search limit, byte law, token estimate, privacy rule, and verdict gates were
frozen in commit `a8a9037` before any corpus payload was inspected or classified. See the
[preregistration](evidence/verb-algebra-census-20260830/preregistration.md). The requested
`designing-experiments` skill was unavailable, so its freeze-before-counting discipline was
applied directly and durably.

The aggregate [receipt](evidence/verb-algebra-census-20260830/receipt.json) has payload
SHA-256 `56fb0f0de9aad0ee9f478ead9f8c855c50f3d0ea5fafa7e78aab517f76436bd5`;
the committed JSON file SHA-256 is
`eba9e2b5a1b57a242c49f8b1263c3fb018f11be7898443d45cb74fbf783c877e`.
Two complete runs produced byte-identical receipts. `[receipt: receipt-payload-sha256;
local shasum and cmp]`

The sibling is
[`experiment/fromto-overlap-study-20260830` at `b51f802`](https://github.com/realgenekim/clj-surgeon/blob/experiment/fromto-overlap-study-20260830/docs/observations/2026-08-30-from-to-double-repetition-study.md).
Its receipt is the authority for LCS/splice totals. A local ordinal-preserving join reran its
committed exact minimal-run counter to split the registered 49,656-byte projection by the
same two pair families; its aggregate
[join receipt](evidence/verb-algebra-census-20260830/splice-family-join.json) has payload
SHA-256 `26951acb96a8f1afc5c29e708c017a05c8f2032402b17a518c9f8a3ac83d10c2`.
No payload or path left either pass.

## Population and method

This is the exact prior-study population, not a new sample:

- UTC **2026-08-22T00:00:00Z → 2026-08-30T02:09:33.141926Z**, inclusive;
- **1,437 service calls = 1,242 `inspect_clojure` + 195
  `apply_clojure_changes`**;
- all successful and refused MCP writes; no CLI writes and no client-only `edit_clojure`;
- **332 eligible pairs in 150 writes**: 207 `edits.from→to`, 125
  `changes.find→replace`; 45 writes have no literal pair;
- canonical requests total **630,138 bytes** after removing top-level `workspace_root`,
  exactly reproducing the earlier receipt.

`[receipt: window, population, canonical-requests, invariants]`

Both sides of every pair were parsed with rewrite-clj 1.2.50. The classifier converts the
parsed s-expression to a deterministic semantic tree, generates closed programs to depth
three, replays each candidate through a server-owned interpreter, and accepts it only when
the replay equals the parsed target. **Eight pairs failed semantic parsing** and **19 contain
comments**; all 27 conservatively route to freeform. Whitespace is treated as formatter-owned
rather than as a transform verb. One pair is semantically identical and needs zero verbs.

The search is exact over the candidates it generates: one-edit relations, specialized whole-
subtree relations, and recursively composed disjoint changes through three verbs. It is not
a proof of mathematical global minimality over every possible sequential same-path program.
That boundary was preregistered and is a limit, not silently promoted into a stronger claim.

The committed
[`transform_verb_census.clj`](../../dev/experiments/transform_verb_census.clj) contains no
corpus content. Eleven deterministic self-tests exercise ten closed verbs, including verbs
that do not appear in the corpus. Every corpus program replayed; all authority and depth
invariants passed. The script SHA-256 is
`9969a7be00ce726fcd240f30667a60c21bb2b109a9b24362b3a515dcf036ae9d`.
No model, embedding, evaluator, SCI callback, arbitrary function body, or model-authored code
ran inside classification. `[receipt: method, parsing, invariants]`

Only aggregates left the local pass. The durable receipt contains counts, byte totals,
distributions, and six contributing telemetry-file byte counts and hashes. It contains no
request, source, response, path, owner, literal, project, or reversible fragment.

## Expressibility

### Pair level

| maximum closed verbs | pairs | share of 332 |
|---|---:|---:|
| ≤1 | **166** | **50.0%** |
| ≤2 | **180** | **54.2%** |
| ≤3 | **182** | **54.8%** |
| escape hatch | **150** | **45.2%** |

`[receipt: pairs.closed-lte-*, pairs.escape-*]`

The median pair needs one operation. Only 16 additional pairs move from the one-verb to the
three-verb closure. More composition is not the main missing capability: the split is mostly
“one simple relation” versus “arbitrary replacement,” not a smooth ladder of longer programs.

### Write level

A write is within N only when it has at least one eligible pair and every pair in that write
fits within N closed verbs.

| maximum closed verbs | eligible writes (n=150) | all service writes (n=195) |
|---|---:|---:|
| ≤1 | **54 / 36.0%** | **54 / 27.7%** |
| ≤2 | **60 / 40.0%** | **60 / 30.8%** |
| ≤3 | **60 / 40.0%** | **60 / 30.8%** |
| any escape hatch | **90 / 60.0%** | **90 / 46.2%** |
| no eligible pair | — | **45 / 23.1%** |

`[receipt: writes; all-write percentages are displayed arithmetic]`

The requested headline is therefore **36.0% / 40.0% / 40.0% of eligible writes in at most
one / two / three verbs**. Against all 195 writes, including the 45 outside the literal-pair
question, the corresponding corpus coverage is **27.7% / 30.8% / 30.8%**.

## Which verbs actually mattered

| verb | operation occurrences | pairs using it | share of all pairs |
|---|---:|---:|---:|
| `replace-subform-freeform` | 150 | 150 | **45.2%** |
| `replace-value` | 97 | 91 | **27.4%** |
| `insert-child` | 54 | 52 | **15.7%** |
| `wrap-form` | 31 | 29 | **8.7%** |
| `remove-child` | 10 | 10 | **3.0%** |
| `replace-string` | 6 | 6 | 1.8% |
| `reorder` | 1 | 1 | 0.3% |
| `rename-symbol`, `thread`, `extract-binding`, `change-arglist` | 0 | 0 | 0.0% |

`[receipt: verbs]`

Including the escape hatch, the top five touch **327 of 332 pairs (98.5%)**. Excluding the
escape hatch, the five most common closed verbs—replace value, insert, wrap, remove, replace
string—cover **180 of 332 pairs (54.2%)**, or **180 of the 182 closed pairs (98.9%)**. The
elaborate semantic verbs did not rescue this corpus. The observed closed algebra is basically
four tree edits plus a small string operation.

That is useful product evidence. Adding more clever names is not supported by frequency;
the missing mass is large freeform subtrees, not `thread` or `extract-binding` hiding in the
tail.

## Bytes: the ceiling is byte-weighted, not pair-weighted

Program bytes include the full compact, sorted-key `{"ops":[…]}` envelope, unabbreviated
verb names, every `at` path, and all replay arguments. Actual `to` bytes are each original
string's canonical JSON scalar bytes, including quoting and escaping but excluding its field
key. Tokens are the registered `ceil(bytes/4)` estimate, not tokenizer observations.

| encoding | bytes | estimated tokens | net vs actual `to` |
|---|---:|---:|---:|
| actual emitted `to` scalars | **211,165** | 52,792 | — |
| all verb programs, full envelope | **211,084** | 52,771 | **+81 B / +21 tok** |
| programs plus 16 B outer cost each | 216,396 | 54,099 | **−5,231 B / −1,307 tok** |
| programs plus 32 B outer cost each | 221,708 | 55,427 | **−10,543 B / −2,635 tok** |

`[receipt: pairs, sensitivity]`

The pair count flatters the algebra. Closed programs cover 54.8% of pairs but only **33,678
target bytes**. Escape pairs carry **177,487 target bytes (84.1%)** and expand them to 186,487
program bytes. Thus the escape hatch is **45.2% by pair count, 60.0% by eligible write count,
and 84.1% by target bytes**. The last number is the honest ceiling on Gene's small-algebra
idea for this sample.

Selective routing avoids paying a freeform wrapper around content that is already freeform:

```text
closed target bytes  33,678
closed program bytes 24,597
selective saving       9,081 bytes = 2,270 estimated tokens
share of full writes   9,081 / 630,138 = 1.44%
```

Even with +16 bytes on each of 182 closed programs, the selective route remains positive at
**6,169 bytes / about 1,542 tokens**; at +32 it retains **3,257 bytes / about 815 tokens**.
Those two rows are displayed arithmetic sensitivity, not receipt fields.

## Verbs versus splices

The sibling study measures exact one-to-one repetition and projects selective splice savings
at **105 bytes of grammar per retained-LCS splice operation**. Its corpus-wide result is
**49,656 bytes / 12,414 projected tokens**, with only 82 of 332 pairs economic. This study
measures actual plausible verb JSON rather than assigning a fixed per-op estimate, so the
absolute comparison is not a controlled implementation race. It is sufficient to choose the
next design screen because the margins are large and point the same way in both request
families.

| pair/write class | pairs | closed in ≤3 verbs | verb net vs emitted JSON scalar | realistic selective splice net | winner |
|---|---:|---:|---:|---:|---|
| `edits.from→to` | 207 | **126 / 60.9%** | **−5,732 B** | **+4,412 B** | splice |
| `changes.find→replace` | 125 | **56 / 44.8%** | **+5,813 B** | **+45,244 B** | splice |
| all pairs, universal verbs | 332 | **182 / 54.8%** | **+81 B** | **+49,656 B** | splice |
| all pairs, selective verbs | 332 | **182 / 54.8%** | **+9,081 B** | **+49,656 B** | splice |

Verb figures are `[receipt: families, pairs]`. Splice totals reproduce sibling receipt
`expressibility.splice_lcs_savings_projections.realistic`; family splits are the local paired
join over the sibling's unchanged `k`, LCS, 105-byte rule, and extraction order.
`[join receipt: families, totals, invariants]`

The design lesson is not that verbs are useless. They give readable names to common atomic
tree edits and compress 182 small pairs. But splices handle arbitrary large deltas without
pretending that a payload is a semantic verb. The natural composition is therefore:

1. an AST-addressed, server-owned splice/replace primitive as the complete substrate;
2. the handful of frequent closed verbs as optional readable sugar or compiler targets;
3. no model code evaluation, callbacks, predicates, regex programs, or open-ended function
   arguments anywhere in the wire contract.

That preserves the best part of Gene's riff—manipulate the homoiconic expression, not editor
keystrokes—without making a 12-name taxonomy carry arbitrary program synthesis.

## Limits

- One caller, one repository, one 8.09-day window, and 195 service writes. Refused writes are
  included because their bytes were emitted.
- The unit is a literal pair. The 45 insertion/extraction/symbol-migration writes without a
  pair are inventoried but not converted into synthetic before/after trees.
- Semantic equality is rewrite-clj-derived and formatter-normalized. Comments fail closed to
  freeform; whitespace-only layout is server-owned. A byte-identical-source requirement would
  reduce closed coverage, not increase it.
- Bounded search can miss a shorter legal three-step same-path program. The measured 54.8%
  is therefore a lower bound for this exact taxonomy, while the 84.1% escape-byte share is a
  result of this registered classifier rather than a theorem about every possible classifier.
- Program bytes are real serialization of a plausible grammar, but no server implementation
  or fresh caller has used it. Splice savings still use the sibling's registered 105-byte
  estimate. The next screen must serialize and replay both candidates under one implemented
  envelope before making a latency or adoption claim.
- The four-bytes-per-token conversion is a projection. Byte totals are authoritative.

## Reproduction

From a clean checkout with the private telemetry root available:

```bash
clojure -M dev/experiments/transform_verb_census.clj \
  "$TELEMETRY_ROOT" \
  /tmp/transform-verb-census-receipt.json
```

The counter fails closed unless it reproduces 1,437 calls, 1,242 reads, 195 writes, 332
pairs, the 207/125 family split, and 630,138 canonical request bytes. Run it twice and compare
the resulting files; this study's two receipts were byte-identical.
