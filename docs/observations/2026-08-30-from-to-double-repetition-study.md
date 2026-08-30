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
