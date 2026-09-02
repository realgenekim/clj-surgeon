# From/to double repetition — zero-model measurement and design study

## Preregistration

Frozen at **2026-08-30T21:10:06Z** (2026-08-30 14:10:06 PDT), after reading the
complete prior study and before reading or computing any source-bearing telemetry row for
this study. The requested `designing-experiments` skill was unavailable, so these rules are
frozen directly in the study. Later corrections, if any, must be labeled deviations and must
not silently replace the registered result.

**Pre-outcome method correction, frozen before overlap computation.** A checksum-only schema
probe reproduced the prior study's 1,437-call population but showed that its 630,138-byte
canonical request checksum is obtained by *removing* top-level `workspace_root`, not by
replacing its value with `<workspace>` as the first registration draft said. Retaining the
key with `<workspace>` yields 636,183 bytes. The counter therefore removes that one field and
requires the published 630,138-byte checksum. This is a correction toward the prior method,
does not inspect an overlap outcome, and is the study's only preregistration deviation.

### Population and privacy contract

- Reuse the prior study's exact UTC window, **2026-08-22T00:00:00Z through
  2026-08-30T02:09:33.141926Z**, and the same local clj-surgeon MCP service telemetry. This is
  the strict seven-withdrawn-figures remeasurement population, not a new window.
- Admit every structured `apply_clojure_changes` request in that window, successful or
  refused, exactly as the prior local pass did. Exclude CLI transport, `edit_clojure` calls
  that never reached this telemetry service, and every other tool. The call-count checksum
  must reproduce **1,437 total calls / 1,242 reads / 195 writes** before results are accepted.
- Extract one pair for each object that contains string-valued `edits.from` and `edits.to`,
  and one pair for each object that contains string-valued `changes.find` and
  `changes.replace`. Do not infer pairs across objects. Report malformed, one-sided, or
  non-string candidates as exclusions rather than coercing them.
- Compute locally. No model call, request, embedding, or model-derived classification is
  permitted. No request content, result content, source, path, prose, project context, raw
  pair, or reversible fragment may leave the local pass. The durable receipt may contain
  only counts, byte totals, distributions, formulas, hashes, timestamps, and aggregate
  classifications.

### Units and counting rules

- Serialize requests canonically exactly as the prior pass: compact JSON and UTF-8, with only
  top-level `workspace_root` removed under the correction above. Text comparisons operate on the
  UTF-8 byte sequences of the decoded JSON string values. “Byte” always means one UTF-8 byte.
- For each pair, let `F` be the anchor (`from` or `find`) and `T` the transform (`to` or
  `replace`). Empty `T` contributes zero bytes and has an undefined ratio; count it, report
  it, and omit it only from ratio distributions. Empty `F` has zero overlap.
- **LCS coverage** is `len(LCS(F,T)) / len(T)`. Compute exact byte LCS length, not a fuzzy
  score. The aggregate LCS share is `sum LCS bytes / sum T bytes`, not the mean pair ratio.
- **6-gram coverage** uses the prior study's conservative greedy longest-match rule: index
  every six-byte gram in `F`; scan `T` left-to-right; at each byte choose the longest prefix
  beginning there that matches anywhere in `F`; count and advance over it only when its
  length is at least six, otherwise advance one byte. Source regions may be reused. The
  aggregate share is `sum covered bytes / sum T bytes`.
- The primary “source emitted twice” count is the conservative 6-gram-covered bytes in `T`:
  those bytes occur once in the paired anchor and again unchanged in the transform. LCS bytes
  are reported as an upper sensitivity bound, not substituted into the primary count.
- A write is one admitted telemetry request. Pair bytes and overlap bytes sum within a write.
  A write with no eligible pair remains in the write inventory but is omitted from pair-ratio
  distributions; its removable-pair bytes are zero in full-write totals.
- Distributions report `min/p25/median/p75/p90` using nearest-rank quantiles on sorted
  observations (`rank = ceil(p*n)`, with the minimum for `p=0`). Report pair-level ratios,
  pair-level byte counts, and write-level summed bytes. Corpus shares always use ratio of
  sums. Do not average percentages to obtain a corpus share.

### Expressibility and savings model

- A splice span is one contiguous non-equal region in a shortest insert/delete edit script.
  Among scripts that retain the maximum possible bytes (the LCS objective), choose one with
  the fewest non-equal regions; that region count is `k`. Identical pairs have `k=0`; all
  other pairs have `k>=1`. This secondary objective prevents the degenerate “replace the
  whole string once” answer from being called minimal.
- The hypothetical splice program keeps the anchor by reference and emits only each changed
  span plus per-operation grammar. Its **gross removable bytes** are the registered primary
  6-gram overlap bytes. This is deliberately conservative even when the LCS-retaining splice
  can avoid more bytes.
- Freeze the realistic per-operation grammar estimate to the prior corpus's measured
  **9.5% JSON-structure share**: after pair extraction, compute
  `H = 0.095 * (sum(F bytes + T bytes) / eligible pair count)`, rounded to the nearest byte.
  This turns the observed structure share into one fixed ceremony allowance per splice op
  without tuning it to the result. The three assumptions are optimistic `0.5H`, realistic
  `H`, and pessimistic `2H`, each rounded to the nearest byte.
- For each pair and assumption, projected net removable bytes are
  `max(0, six_gram_overlap_bytes - k * overhead_per_op)`. Clamping means an uneconomic pair
  stays in the existing grammar; negative “savings” may not subsidize another pair. Sum pair
  net within each write and corpus-wide. Report the number/share of pairs that remain
  economic, plus the net-byte distributions per pair and per write.
- Convert projected bytes to tokens only with the same explicit deterministic conversion in
  every table: **4 UTF-8 bytes per token**. Token results are labeled projections and rounded
  to the nearest whole token; byte results remain authoritative.
- The combination table must keep denominators honest. Reproduce the prior anchor class as
  **162,345 bytes = 28.5% of 570,355 leaf bytes = 25.8% of 630,138 full request bytes**.
  Combine that non-overlapping emitted-position class with each projected net splice saving,
  express the sum against the full **630,138-byte** request corpus, and compare the projected
  remaining bytes with the measured **105,177-byte / 16.7% DECIDED floor**. The 28.5% label
  must never be added directly to a full-request percentage.

### Decision rules and receipts

- The verified replacement for the Opus-era “75%” claim is the aggregate 6-gram share of
  `T`, accompanied by aggregate LCS share and the pair/write distributions. It is not a
  selected percentile and not rounded to the old claim.
- “Juice” means projected net removable bytes/tokens after splice grammar at all three frozen
  overhead assumptions. Measured overlap and `k` are observations; grammar savings,
  token-equivalents, and the combined addressable table are projections.
- Accept only a receipt that records: window; telemetry file inventory hashes; admitted call
  checksum; canonical-request-byte checksum; pair and exclusion counts by family; all
  numerator/denominator byte totals; quantile vectors and population sizes; overhead formula
  inputs; invariants; script SHA-256; and receipt SHA-256. Every headline in the completed
  study must cite the corresponding receipt key or a displayed arithmetic equation.

**Post-outcome mechanism correction; registered result preserved.** The registration assumed
the source-reusing 6-gram count would be conservative relative to one-to-one LCS and therefore
priced splices from 6-gram bytes. The result falsified that assumption: source reuse made
6-gram coverage larger than LCS. The receipt preserves the registered six-gram projections,
but this report does not call them splice-realizable. Literal double emission and the primary
design table use exact LCS; the registered six-gram result is reported separately as
copy-coverage sensitivity. No pair was removed and no threshold, quantile, `k`, or overhead
assumption changed.

---

## Verdict

There is juice. The dismissal is not supported.

The old “75%” cannot survive as one number because it conflated two different questions. The
verified replacements are:

- **70.5% literal one-to-one repetition**: exact LCS retains **143,747 of 203,852 transform
  bytes**. This is the count that can truthfully be called source bytes emitted once in the
  anchor and once again in the transform. `[receipt: overlap.lcs.overlap_bytes,
  overlap.lcs.to_bytes, overlap.lcs.share_of_to_percent]`
- **86.1% prior-style copy coverage**: the conservative six-byte threshold covers **175,528
  of 203,852 transform bytes** when an anchor region may be reused. This answers “could this
  transform byte be copied from its own anchor?” but is not a one-to-one splice budget.
  `[receipt: overlap.six_gram.overlap_bytes, overlap.six_gram.to_bytes,
  overlap.six_gram.share_of_to_percent]`

The preregistered replacement for the Opus claim was the second number, so the registered
answer is **86.1%, not 75%**. The literal double-emission answer is **70.5%**. The Opus-era
figure happens to lie between them, but it has no surviving receipt or declared counting rule
and therefore is withdrawn rather than “confirmed.”

The honest design prize is smaller than either percentage makes it sound. After charging a
realistic **105 bytes of grammar per retained-LCS splice op** and leaving uneconomic pairs on
the existing grammar, the projection removes **49,656 bytes / 12,414 tokens**, or **7.9% of
the complete 630,138-byte write corpus**. The median write saves **zero**; p75 saves **71
bytes / 18 projected tokens**, and p90 saves **635 bytes / 159 projected tokens**. Only **82
of 332 pairs** and **60 of 195 writes** are economic at that assumption. `[receipt:
expressibility.splice_lcs_savings_projections.realistic]`

So the correct lesson is neither “nothing here” nor “remove 75% of writes.” It is: **measured
overlap and `k` support a skewed, roughly eight-percent projected full-corpus splice prize;
combining it with anchor references raises the projected addressable share from 25.8% to
33.6%.** That is material enough to design and screen, but not universal enough to impose on
every edit.

## Receipt and population

The durable aggregate receipt is
[`2026-08-30-from-to-double-repetition-receipt.json`](evidence/2026-08-30-from-to-double-repetition-receipt.json).
Its payload SHA-256 is
`5c66a8b9934fcad07696db01dd3162327d4f5baa3221f572f4f3d4a846e5e4b1`; the committed JSON
file SHA-256 is
`e6c614df74c095d848f0a260ed865aadae28b8061b8c0343b2086a562b9f3b59`.
`[receipt: receipt_payload_sha256; local shasum receipt]`

- Window: **2026-08-22T00:00:00Z → 2026-08-30T02:09:33.141926Z**, or **2026-08-21
  17:00:00 PDT → 2026-08-29 19:09:33 PDT**. `[receipt: window]`
- Checksum: **1,437 calls = 1,242 `inspect_clojure` + 195
  `apply_clojure_changes`**, all 195 writes carrying full requests; canonical write bytes are
  exactly **630,138** after removing top-level `workspace_root`. `[receipt: population,
  canonical_requests.bytes, invariants]`
- Pair population: **332 eligible pairs in 150 writes**: **207 `from→to`** and **125
  `find→replace`**. **45 writes** have no eligible pair. **Eight** one-sided `changes`
  objects were excluded; none of the `edits` candidates were excluded. There were no empty
  anchors or transforms. `[receipt: population]`
- Pair payload: **161,488 anchor bytes + 203,852 transform bytes = 365,340 bytes**.
  `[receipt: pair_bytes]`
- The pass used **zero model calls**, RapidFuzz **3.14.5** for an exact LCS cross-check, an
  independent affine edit-run dynamic program, and **3,972 deterministic self-test cases**.
  All receipt invariants passed. Script SHA-256 is
  `684eccfce2f838926afa284308e96028a8557a9404c879a11c57cba80acc3d01`; helper-source
  SHA-256 is
  `b3e127c2ec76e75fb3edabfa643068aa1dd96e7073b62741f54b8468db61b53d`.
  `[receipt: method, invariants]`

Only aggregate evidence left the local pass. The receipt lists byte counts and SHA-256s for
the **six** contributing telemetry files, but no request, response, source, path, form name,
project, or reversible text fragment. `[receipt: telemetry_inventory]`

## 1. From/to overlap

All percentages below are coverage of each transform against its *own* paired anchor. Corpus
shares are ratios of byte sums; they are not averages of the pair percentages. Displayed
percentages round the receipt's six-decimal values to one decimal.

| metric | corpus overlap | corpus share of transform bytes | pair min / p25 / median / p75 / p90 | paired-write min / p25 / median / p75 / p90 |
|---|---:|---:|---:|---:|
| exact LCS | 143,747 B | **70.5%** | 0.0 / 49.5 / 70.3 / 86.9 / 95.7% | 9.1 / 50.4 / 71.4 / 82.6 / 93.6% |
| greedy 6-gram | 175,528 B | **86.1%** | 0.0 / 55.2 / 81.4 / 92.3 / 98.0% | 0.0 / 61.7 / 84.5 / 92.3 / 96.8% |

Receipts: `[receipt: overlap.lcs, overlap.six_gram]`. Pair distributions have `n=332`;
paired-write ratio distributions have `n=150`. The corresponding all-write overlap-byte
distributions, including the 45 zero-pair writes, are:

| metric | min / p25 / median / p75 / p90 bytes per write | writes with nonzero overlap |
|---|---:|---:|
| exact LCS | 0 / 3 / 123 / 549 / 1,820 | 150 / 195 |
| greedy 6-gram | 0 / 0 / 155 / 938 / 2,332 | 140 / 195 |

`[receipt: overlap.lcs.write_overlap_byte_quantiles_all_writes,
overlap.lcs.writes_with_nonzero_overlap,
overlap.six_gram.write_overlap_byte_quantiles_all_writes,
overlap.six_gram.writes_with_nonzero_overlap]`

The two grammar families are not alike:

| pair family | pairs | transform bytes | LCS bytes / share | 6-gram bytes / share |
|---|---:|---:|---:|---:|
| `edits.from→to` | 207 | 73,091 | 40,473 / **55.4%** | 56,267 / **77.0%** |
| `changes.find→replace` | 125 | 130,761 | 103,274 / **79.0%** | 119,261 / **91.2%** |
| **all pairs** | **332** | **203,852** | **143,747 / 70.5%** | **175,528 / 86.1%** |

`[receipt: overlap.family, overlap.lcs, overlap.six_gram]`

This split explains why a single remembered percentage is unsafe. Generic `find→replace`
is close to whole-anchor repetition; compact `from→to` is substantially less so under the
one-to-one measure.

## 2. The double-emission total

The literal receipt is **143,747 repeated byte units**. Each has one occurrence in its anchor
and one in its transform, so the paired occurrence traffic is **287,494 bytes = 2 ×
143,747**. The redundant second occurrence is **143,747 bytes**, equal to **22.8% of the
full 630,138-byte request corpus**. `[receipt: overlap.lcs.overlap_bytes,
overlap.lcs.share_of_full_request_percent, canonical_requests.bytes; displayed equation]`

The registered six-gram counter reports **175,528 transform-side byte occurrences** with an
anchor match, **86.1% of transform bytes** and **27.9% of the full request corpus**.
`[receipt: overlap.six_gram]` It is a valid copy-coverage count but
not a literal two-occurrence count: source reuse means one anchor region can cover several
transform regions. Pricing a one-to-one splice at 175,528 bytes would therefore overstate the
mechanism. This is why the design projection below uses the 143,747-byte LCS budget and keeps
the preregistered six-gram projection only as sensitivity evidence.

## 3. Expressibility: how many splices?

Among maximum-LCS programs, the minimal contiguous replacement-span count has this
distribution:

| population | min | p25 | median | p75 | p90 | maximum |
|---|---:|---:|---:|---:|---:|---:|
| 332 pairs | 0 | 1 | **2** | 5 | 28 | 254 |

`[receipt: expressibility.k_quantiles, expressibility.k_histogram]` Exactly **one** pair is
identical and has `k=0`; **147** pairs have `k=1`, **57** have `k=2`, and **28** have `k=3`.
The long tail is the design constraint: the median is easy, but p90 needs 28 operations and
the maximum needs 254. `[receipt: expressibility.identical_pairs_k_zero,
expressibility.k_histogram]`

The frozen overhead basis is:

```text
mean pair payload = 365,340 / 332 = 1,100.421687 bytes
realistic H        = round_half_up(9.5% × 1,100.421687) = 105 bytes/op
optimistic         = round_half_up(0.5 × 105)           = 53 bytes/op
pessimistic        = 2 × 105                            = 210 bytes/op
```

`[receipt: expressibility.overhead_basis,
expressibility.overhead_assumptions_bytes_per_op]`

### Net splice prize — projections

These are **projections**, not measured implementation savings. Gross is the one-to-one
143,747-byte LCS budget. Each pair pays `k × overhead`; a negative result clamps to zero and
keeps that pair on today's grammar.

| assumption | overhead / op | net bytes / projected tokens | full-request share | economic pairs | writes with net saving | per-write p75 / p90 bytes (tokens) |
|---|---:|---:|---:|---:|---:|---:|
| optimistic | 53 B | **62,487 B / 15,622 tok** | **9.9%** | 121 / 332 | 81 / 195 | 138 / 875 B (35 / 219 tok) |
| realistic | 105 B | **49,656 B / 12,414 tok** | **7.9%** | 82 / 332 | 60 / 195 | 71 / 635 B (18 / 159 tok) |
| pessimistic | 210 B | **31,062 B / 7,766 tok** | **4.9%** | 53 / 332 | 37 / 195 | 0 / 425 B (0 / 106 tok) |

`[receipt: expressibility.splice_lcs_savings_projections]` Every assumption has per-write
`min=0`, `p25=0`, and `median=0` across all 195 writes. Tokens use the registered **4 UTF-8
bytes/token** conversion and are projections; byte counts are authoritative. `[receipt:
expressibility.splice_lcs_savings_projections.*.write_net_byte_quantiles_all_writes,
expressibility.splice_lcs_savings_projections.*.write_net_token_quantiles_all_writes_projection_4_bytes_per_token]`

The preregistered, source-reusing 6-gram sensitivity produces **77,920 / 60,166 / 38,862
net bytes** under optimistic / realistic / pessimistic overhead. `[receipt:
expressibility.registered_six_gram_savings_projections]` Those are valid upper copy-coverage
projections, but they are not the honest splice number because `k` describes a one-to-one LCS
program. The table above is the design result.

The skew is the main reason this was easy to wave away. A universal grammar would pay for a
4,103-op tail (`430,815 / 105 = 4,103`) against only 143,747 gross bytes at the realistic
assumption. Selective routing turns that losing universal conversion into 49,656 positive
bytes by admitting only 82 economic pairs. `[receipt:
expressibility.splice_lcs_savings_projections.realistic.unclamped_grammar_penalty_bytes,
expressibility.splice_lcs_savings_projections.realistic.gross_overlap_bytes,
expressibility.splice_lcs_savings_projections.realistic.economic_pairs,
expressibility.splice_lcs_savings_projections.realistic.net_removable_bytes; displayed equation]`

## 4. Combined with anchor references

The prior anchor class is **162,345 bytes**: **28.5% of 570,355 leaf bytes**, but **25.8% of
the 630,138-byte full request corpus**. The table uses the full-request denominator throughout;
it never adds 28.5% directly to a full-request percentage. `[receipt:
combination_constants_from_prior_receipt]`

Anchor bytes and transform-overlap bytes are distinct emitted positions, so their projected
savings can be added without double-counting. Every splice row below is a **projection**.

| scenario | projected removable bytes | share of full requests | projected remaining bytes / share | remaining bytes above measured DECIDED floor |
|---|---:|---:|---:|---:|
| anchor reference only | 162,345 | 25.8% | 467,793 / 74.2% | 362,616 |
| + optimistic splice | **224,832** | **35.7%** | 405,306 / 64.3% | 300,129 |
| + realistic splice | **212,001** | **33.6%** | 418,137 / 66.4% | 312,960 |
| + pessimistic splice | **193,407** | **30.7%** | 436,731 / 69.3% | 331,554 |
| measured DECIDED floor | — | — | **105,177 / 16.7%** | 0 |

Receipts: `[receipt: combination_constants_from_prior_receipt,
expressibility.splice_lcs_savings_projections]`. The anchor-only row is the displayed
arithmetic `630,138 − 162,345 = 467,793` and `467,793 − 105,177 = 362,616`.

This table prevents a second overclaim. The realistic combined design addresses **33.6%** of
all write bytes, but it does not approach the **16.7%** floor: projected remaining emission is
**418,137 bytes / 66.4%**, still **312,960 bytes** above DECIDED and about **4.0× the floor**
(`418,137 / 105,177 = 3.9756`). `[receipt:
expressibility.splice_lcs_savings_projections.realistic,
combination_constants_from_prior_receipt; displayed equation]` Other copied and rule-generated
classes remain outside these two designs.

## What Gene's intuition got right

Gene's complaint identifies a real two-layer repetition:

1. The caller quotes old text as the anchor. That is the prior **162,345-byte** anchor-reference
   prize. `[receipt: combination_constants_from_prior_receipt.anchor_bytes]`
2. The caller then quotes much of that old text again inside the transform. The literal
   second-copy pool is **143,747 bytes**, and the prior-style copy-coverage pool is **175,528
   bytes**. `[receipt: overlap.lcs.overlap_bytes, overlap.six_gram.overlap_bytes]`

The reason to resist a universal solution is not absence of overlap. It is operation overhead
and the `k` tail. At the realistic assumption, three quarters of pairs are uneconomic
(`332 − 82 = 250`, or **75.3%**) and the median write saves zero. `[receipt:
population.eligible_pairs,
expressibility.splice_lcs_savings_projections.realistic.economic_pairs,
expressibility.splice_lcs_savings_projections.realistic.write_net_byte_quantiles_all_writes;
displayed equation]` The right design is therefore an opt-in or compiled representation chosen
only when the server can prove `overlap > kH`, not a replacement for readable `from/to`.

That is also why earlier work could rank other prizes ahead of this one without proving this
one empty. The prior study measured complete refusal-driven retry calls and post-decision
boundaries; this study measures a byte grammar inside only 150 of 195 writes. The prizes live
at different levels. This measurement says the inner-grammar prize is real and bounded.

## Design consequence and falsifiable next step

Do not ship “minimal splices” from this receipt. The acceptance ladder has not started: no
grammar is implemented, no mechanism is verified, no fresh caller has used it, and no
controlled efficiency gate has passed.

The smallest honest next screen is zero-model and source-preserving:

- reserialize only the **82 realistic-economic pairs** as explicit, readable splice ops;
- require byte-identical reconstructed `to`, identical frozen source and future hashes, and
  identical match counts;
- reject any pair whose actual serialized grammar exceeds its registered **105 B/op** budget;
- report actual rather than estimated structure bytes, then rerun the net table;
- proceed to a fresh caller only if the buildability screen preserves the projected sign.

The population and thresholds above come from `[receipt: population,
expressibility.splice_lcs_savings_projections.realistic,
expressibility.overhead_assumptions_bytes_per_op.realistic]`. The proposed screen itself is a
future experiment, not a result.

## Reproduction

The privacy-safe counter and exact minimal-run helper are committed as
[`from_to_double_repetition_measure.py`](../../dev/experiments/from_to_double_repetition_measure.py)
and [`from_to_min_runs.cpp`](../../dev/experiments/from_to_min_runs.cpp). With a private
`$TELEMETRY_ROOT` and RapidFuzz 3.14.5 installed in an isolated environment:

```bash
clang++ -O3 -std=c++17 -dynamiclib \
  dev/experiments/from_to_min_runs.cpp -o /tmp/libfromto_min_runs.dylib
python dev/experiments/from_to_double_repetition_measure.py \
  --telemetry-root "$TELEMETRY_ROOT" \
  --since 2026-08-22T00:00:00Z \
  --until 2026-08-30T02:09:33.141926Z \
  --helper-lib /tmp/libfromto_min_runs.dylib \
  --helper-source dev/experiments/from_to_min_runs.cpp \
  --output /tmp/fromto-receipt.json
```

Two complete passes produced byte-identical JSON and payload SHA-256
`5c66a8b9934fcad07696db01dd3162327d4f5baa3221f572f4f3d4a846e5e4b1`.
`[receipt: receipt_payload_sha256, method]`

## Limits

- One caller, one repository, one **8.09-day** window, and 195 MCP writes; refused writes are
  intentionally included because they consumed emission. The 8.09-day duration is the prior
  study's displayed window; exact bounds and write count are `[receipt: window, population]`.
- LCS is an exact one-to-one retention measure; 6-gram is a source-reusing copyability
  measure. Neither proves that a model would use a new grammar correctly.
- The **53 / 105 / 210 B/op** costs are frozen estimates derived from the old 9.5% structure
  share, not serialized candidate grammars. `[receipt: expressibility.overhead_basis,
  expressibility.overhead_assumptions_bytes_per_op]`
- The **4 bytes/token** conversion is a declared projection, not tokenizer output.
- No efficiency, latency, correctness, or adoption claim follows from byte arithmetic. This
  is the seven-withdrawn-figures measurement that decides whether a buildability screen is
  worth running; it is not that screen.
