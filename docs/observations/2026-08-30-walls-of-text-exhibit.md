# Walls of text — direct write-payload exhibit

## Preregistration

Frozen at **2026-08-30T21:13:31Z** (2026-08-30 14:13:31 PDT), after reading the
prior aggregate study and the sibling lane's preregistration, but before reading or computing
any source-bearing telemetry row for this pass. Corrections must be labeled as deviations;
they may not silently replace these rules.

### Population and privacy contract

- Reuse the prior study's exact inclusive UTC window, **2026-08-22T00:00:00Z through
  2026-08-30T02:09:33.141926Z**, and the same local Codex activity and clj-surgeon service
  telemetry. This is a remeasurement, not a new sample.
- Admit every structured Codex `edit_clojure` and `apply_clojure_changes` payload whose call
  timestamp is inside the window, whether the call succeeded or was refused. Exclude CLI
  transport and all other tools. Deduplicate client representations by stable call identity;
  do not deduplicate repeated attempts that were genuinely emitted twice.
- Cross-check admitted `apply_clojure_changes` calls against the service-side checksum from
  the prior local pass: **195 writes** inside **1,437 calls / 1,242 reads / 195 writes**.
  Client-only `edit_clojure` calls are an additive population and must remain separately
  identified in every receipt.
- Compute locally with no model, embedding, or remote analysis call. No request or result
  content, source, path, prose, project context, raw fragment, or reversible text may leave
  the pass. The document and durable aggregate receipt may contain only sizes, exact token
  counts, hashes, counts, distributions, formulas, timestamps, field shapes, ranks, and
  overlap ratios.

### Counting rules

- A payload is the compact canonical JSON argument object (`ensure_ascii=false`, sorted keys,
  separators `,` and `:`), encoded as UTF-8, after replacing only a top-level
  `workspace_root` value with `<workspace>`. `total bytes` and total tokens cover that whole
  canonical object, including grammar and non-text fields.
- `from/find` is the sum of UTF-8 bytes and tokens of decoded string leaves whose exact field
  name is `from` or `find`. `to/replace/insert` is the corresponding sum for exact field names
  `to`, `replace`, `insert_after`, or `insert_before`. Missing fields contribute zero. No
  source text is normalized, joined, or inferred across edit/change objects.
- Token counts use Python `tiktoken` with encoding **`o200k_base`**. Payload totals tokenize
  the canonical JSON once. Field-family totals tokenize each decoded string leaf separately
  and sum the exact counts; no synthetic separator is added. The receipt records the package
  version and encoding identity.
- Decode seconds are a projection, not observed call wall: `UTF-8 bytes × 3.5237 ms / 1000`,
  using the prior study's measured write-arm slope and excluding its fixed 2,684 ms intercept.
  Field seconds use field bytes; total seconds use canonical payload bytes.
- One write's `from→to 6-gram overlap` is computed only within the same edit/change object.
  Index every six-byte gram in that object's `from`/`find`; scan each paired
  `to`/`replace`/`insert_after`/`insert_before` left-to-right; at each byte choose the longest
  prefix matching anywhere in the paired source; count and advance only for matches of at
  least six bytes, otherwise advance one byte. Source regions may be reused. Sum covered
  destination bytes across pairs and divide by that write's destination bytes. A write with
  no destination bytes reports overlap as undefined. Do not compare one object's destination
  with another object's source.
- The Top 20 rank by canonical total payload bytes descending, breaking ties by a
  privacy-safe SHA-256 of the canonical payload. Published identifiers are rank plus a
  12-hex prefix of that hash; timestamps, session identities, and paths remain local.
- Size distributions are per admitted payload and include zero-byte field-family values.
  Report nearest-rank `p50/p75/p90/p99/max` (`rank = ceil(p*n)`); bytes, exact token counts,
  and projected seconds are quantiled independently rather than converting a byte percentile
  into an alleged token percentile.
- The daily tax groups calls by UTC date of emission. Each row reports the sum of emitted
  `from/find + to/replace/insert` bytes and exact field-leaf tokens. Pure-decode minutes are
  `combined bytes × 3.5237 / 60,000`. The first eight dates are full UTC days; 2026-08-30 is
  partial through the registered upper bound. Corpus-average minutes/day divide by the exact
  window duration, while the displayed daily rows remain unnormalized observations.

### Registered deviation after the shape-only preflight

The frozen rule above assumed every counted field was string-valued. The first shape-only
preflight falsified that assumption: 72 of 76 `changes[].insert_after` values and 15 of 16
`changes[].insert_before` values are arrays. Before accepting any result, the pass was changed
to the prior study's exact role boundary: count string-valued `edits[].from` and `edits[].to`;
string-valued `changes[].find` and `changes[].replace`; and direct string values or direct
string array members under `changes[].insert_after` and `changes[].insert_before`. Exclude the
distinct structured `{forms: […]}` insertion shape and unrelated same-named fields such as an
extraction destination. This correction reproduces the prior field receipts exactly:
`changes.find = 117,184 B`, `changes.replace = 130,761 B`,
`changes.insert_after = 133,700 B`, `changes.insert_before = 26,338 B`,
`edits.from = 45,161 B`, and `edits.to = 73,091 B` on the 195-write service subset. All
token, overlap, distribution, Top-20, and daily calculations below use the corrected rule.

### Receipt acceptance

Accept only an aggregate receipt that records the window, privacy mode, source-inventory
hashes without paths, admitted and excluded counts by tool and representation, service-call
checksum, canonical-payload byte checksum, field byte and token totals, quantile vectors and
population sizes, every Top-20 row, daily vectors, overlap numerators and denominators,
invariants, analysis-script SHA-256, and receipt SHA-256. Every completed table or headline
must name its receipt key or show the arithmetic from named keys.

---

## The exhibit

**The walls are real.** The frozen corpus contains **247 structured writes**—195
`apply_clojure_changes` and 52 `edit_clojure`—whose canonical payloads total **729,057 bytes
/ 179,972 exact `o200k_base` tokens / 2,568.978 seconds (42.816 minutes) of projected
decode**. The `from/find` plus `to/replace/insert` fields alone are **603,327 bytes / 136,387
tokens / 35.432 minutes**. That field traffic is 82.8% of all canonical request bytes.
`[receipt: population.writes, population.by_tool, totals.canonical_payload_bytes,
totals.canonical_payload_tokens, totals.from_plus_to_bytes, totals.from_plus_to_tokens,
totals.from_plus_to_decode_minutes; displayed slope equation]`

The largest write is the visceral version: **write #1 is 29,598 B total; its destination
field is 14,840 B = 3,714 tokens = 52.292 seconds of pure decode, and 14,104 B (95.0%) of
that destination matched its own anchor.** Its 12,940-byte anchor costs another 45.597
seconds. The next two writes repeat exactly those anchor, destination, token, and overlap
volumes; only 7 canonical bytes separate the first and third payloads. `[receipt: top20[0:3]]`

The maximum destination wall is write #4: **15,960 B = 3,547 tokens = 56.238 seconds** in
the destination field alone, 75.8% matched to its own anchor. At the other extreme, write
#10 emits **10,651 destination bytes = 2,524 tokens = 37.531 seconds** with no paired anchor.
Both are expensive; one is mostly repetition, the other is insertion. `[receipt: top20[3],
top20[9]]`

## Top 20 payloads

Each triplet is **exact UTF-8 bytes / exact `o200k_base` tokens / projected decode seconds**.
Seconds use `bytes × 3.5237 ms / 1000`; token counts do not drive the time projection.
`apply` abbreviates `apply_clojure_changes`, and each identifier is the first 12 hex digits
of the privacy-safe canonical payload SHA-256. `[receipt: top20,
decode_projection.slope_ms_per_utf8_byte, tokenizer]`

| write | tool | total: B / tok / s | from/find: B / tok / s | to/replace/insert: B / tok / s | own 6-gram overlap |
|---:|---|---:|---:|---:|---:|
| #1 `1410ea4c5402` | `apply` | 29,598 / 7,903 / 104.294 | 12,940 / 3,349 / 45.597 | 14,840 / 3,714 / 52.292 | 14,104/14,840 = 95.0% |
| #2 `5a5d90602f4d` | `apply` | 29,592 / 7,902 / 104.273 | 12,940 / 3,349 / 45.597 | 14,840 / 3,714 / 52.292 | 14,104/14,840 = 95.0% |
| #3 `f453dbabbda2` | `apply` | 29,591 / 7,901 / 104.270 | 12,940 / 3,349 / 45.597 | 14,840 / 3,714 / 52.292 | 14,104/14,840 = 95.0% |
| #4 `e8cbc63a1b83` | `apply` | 28,537 / 6,768 / 100.556 | 10,759 / 2,315 / 37.911 | 15,960 / 3,547 / 56.238 | 12,090/15,960 = 75.8% |
| #5 `36d95f176d5f` | `apply` | 12,495 / 3,143 / 44.029 | 4,167 / 981 / 14.683 | 7,148 / 1,654 / 25.187 | 5,316/7,148 = 74.4% |
| #6 `1d038cd880bc` | `apply` | 11,631 / 2,574 / 40.984 | 4,158 / 727 / 14.652 | 6,443 / 1,457 / 22.703 | 1,857/6,443 = 28.8% |
| #7 `5cea2f5bcfcb` | `apply` | 11,631 / 2,575 / 40.984 | 4,158 / 727 / 14.652 | 6,444 / 1,457 / 22.707 | 1,857/6,444 = 28.8% |
| #8 `747dd2c62815` | `apply` | 11,627 / 2,573 / 40.970 | 4,158 / 727 / 14.652 | 6,444 / 1,457 / 22.707 | 1,857/6,444 = 28.8% |
| #9 `d29368f6d656` | `apply` | 11,627 / 2,574 / 40.970 | 4,158 / 727 / 14.652 | 6,443 / 1,457 / 22.703 | 1,857/6,443 = 28.8% |
| #10 `f28f442c4891` | `apply` | 11,392 / 2,874 / 40.142 | 0 / 0 / 0.000 | 10,651 / 2,524 / 37.531 | 0/10,651 = 0.0% |
| #11 `8756522402af` | `apply` | 11,019 / 2,392 / 38.828 | 1,907 / 439 / 6.720 | 8,115 / 1,595 / 28.595 | 2,617/8,115 = 32.2% |
| #12 `82d43204c2f5` | `apply` | 10,918 / 2,654 / 38.472 | 2,648 / 674 / 9.331 | 7,219 / 1,528 / 25.438 | 3,476/7,219 = 48.2% |
| #13 `1dfbe10effbf` | `apply` | 10,862 / 2,590 / 38.274 | 3,852 / 846 / 13.573 | 5,962 / 1,282 / 21.008 | 4,824/5,962 = 80.9% |
| #14 `94e37e581690` | `apply` | 10,846 / 2,585 / 38.218 | 3,852 / 846 / 13.573 | 5,946 / 1,277 / 20.952 | 4,825/5,946 = 81.1% |
| #15 `e85db71ac7c1` | `apply` | 10,035 / 1,853 / 35.360 | 3,975 / 726 / 14.007 | 5,720 / 908 / 20.156 | 5,537/5,720 = 96.8% |
| #16 `c3b237d6b4d7` | `apply` | 10,034 / 1,853 / 35.357 | 3,975 / 726 / 14.007 | 5,719 / 908 / 20.152 | 5,537/5,719 = 96.8% |
| #17 `354220a881ff` | `apply` | 8,955 / 2,148 / 31.555 | 433 / 99 / 1.526 | 539 / 126 / 1.899 | 474/539 = 87.9% |
| #18 `328f6c2eb70d` | `apply` | 8,935 / 2,143 / 31.484 | 433 / 99 / 1.526 | 7,511 / 1,664 / 26.467 | 474/7,511 = 6.3% |
| #19 `1a1582061f57` | `apply` | 8,934 / 2,142 / 31.481 | 433 / 99 / 1.526 | 7,509 / 1,664 / 26.459 | 474/7,509 = 6.3% |
| #20 `0b7374dbf4a0` | `apply` | 8,933 / 2,142 / 31.477 | 433 / 99 / 1.526 | 7,508 / 1,664 / 26.456 | 474/7,508 = 6.3% |

All Top-20 writes happen to be `apply_clojure_changes`; the 52 `edit_clojure` calls were
admitted and ranked but none crossed the twentieth-place cutoff of 8,933 canonical bytes.
This is a result, not a tool filter. `[receipt: top20, population.by_tool]`

## Field-size distributions

The seconds column is the cost. A p90 destination field takes **17.936 seconds** of decode;
p99 takes **52.292 seconds** and the maximum takes **56.238 seconds**, before the fixed
2.684-second call intercept. The p99 anchor independently costs **45.597 seconds**. Quantiles
are nearest-rank over all 247 writes, including zeros, and token quantiles were computed on
exact token counts rather than inferred from byte quantiles. `[receipt: distributions;
decode_projection.fixed_intercept_included]`

| field family | unit | p50 | p75 | p90 | p99 | max |
|---|---|---:|---:|---:|---:|---:|
| to/replace/insert | bytes | 575 | 2,036 | 5,090 | 14,840 | 15,960 |
| to/replace/insert | `o200k` tokens | 154 | 463 | 908 | 3,714 | 3,714 |
| to/replace/insert | decode seconds | 2.026 | 7.174 | 17.936 | 52.292 | 56.238 |
| from/find | bytes | 183 | 619 | 2,083 | 12,940 | 12,940 |
| from/find | `o200k` tokens | 51 | 149 | 455 | 3,349 | 3,349 |
| from/find | decode seconds | 0.645 | 2.181 | 7.340 | 45.597 | 45.597 |

`[receipt: distributions.to_replace_insert, distributions.from_find]`

## The daily retyping tax

Across the exact 8.089966920-day window, the model emitted **136,387 exact field tokens** and
spent a projected **35.432 minutes** decoding 603,327 `from+to` bytes. That is **16,858.783
tokens/day and 4.380 pure-decode minutes/day** on the exact-duration average. The busiest
calendar day paid **43,109 tokens and 10.305 minutes**. These minutes exclude the fixed call
intercept and therefore understate complete call cost. `[receipt: totals.from_plus_to_bytes,
totals.from_plus_to_tokens, totals.from_plus_to_decode_minutes, totals.exact_window_days,
totals.corpus_average_tokens_per_day, totals.corpus_average_decode_minutes_per_day, daily]`

| UTC date | writes (apply + edit) | from tok | to tok | combined B | combined tok | pure decode min |
|---|---:|---:|---:|---:|---:|---:|
| 2026-08-22 | 3 (3 + 0) | 3,285 | 5,137 | 37,837 | 8,422 | 2.222 |
| 2026-08-23 | 30 (30 + 0) | 14,472 | 28,637 | 175,477 | 43,109 | 10.305 |
| 2026-08-24 | 33 (33 + 0) | 4,005 | 11,753 | 68,012 | 15,758 | 3.994 |
| 2026-08-25 | 18 (18 + 0) | 5,273 | 11,967 | 82,046 | 17,240 | 4.818 |
| 2026-08-26 | 29 (20 + 9) | 2,614 | 5,244 | 35,830 | 7,858 | 2.104 |
| 2026-08-27 | 16 (11 + 5) | 1,743 | 2,610 | 19,016 | 4,353 | 1.117 |
| 2026-08-28 | 45 (38 + 7) | 5,406 | 12,803 | 88,866 | 18,209 | 5.219 |
| 2026-08-29 | 73 (42 + 31) | 6,635 | 14,803 | 96,243 | 21,438 | 5.652 |
| 2026-08-30 (partial through 02:09:33Z) | 0 (0 + 0) | 0 | 0 | 0 | 0 | 0.000 |

`[receipt: daily]`

## What matched its own anchor

Across the broad exhibit, own-anchor 6-gram coverage is **211,878 / 412,370 destination
bytes = 51.4%**. That aggregate deliberately includes insertion strings with no paired
`find`; they contribute destination bytes and zero own-anchor coverage rather than vanishing
from the denominator. Split by transport, `apply_clojure_changes` is **176,501 / 363,890 =
48.5%** and `edit_clojure` is **35,377 / 48,480 = 73.0%**. `[receipt:
totals.six_gram_overlap_bytes, totals.six_gram_overlap_denominator_bytes,
totals.six_gram_overlap_share, totals.by_tool]`

This aggregate must not be confused with the narrower sibling experiment. Restrict this pass
to the sibling lane's registered population—195 `apply_clojure_changes` writes and only 207
`edits.from→to` plus 125 `changes.find→replace` pairs—and it produces **332 pairs, 203,852
destination bytes, 175,528 covered bytes, 86.1056%**. The running
`experiment/fromto-overlap-study-20260830` lane independently reports exactly **332 / 203,852
/ 175,528 / 86.1056%** in its aggregate receipt. The figures reconcile exactly, byte for
byte; the apparent 86.1% versus 51.4% difference is entirely population scope. `[receipt:
sibling_compatible_apply_subset; sibling receipt: overlap.six_gram, payload SHA-256
5c66a8b9934fcad07696db01dd3162327d4f5baa3221f572f4f3d4a846e5e4b1]`

The bridge equation is explicit: adding insertion destinations changes the apply-only result
from **175,528 / 203,852** to **176,501 / 363,890**—160,038 more destination bytes but only
973 more own-anchor-covered bytes under the registered pairing rule. Adding the 52 compact
edits then yields the broad exhibit's **211,878 / 412,370**. `[receipt:
sibling_compatible_apply_subset, totals.by_tool, totals]`

## Receipt and verification

The durable aggregate receipt is
[`2026-08-30-walls-of-text-exhibit-receipt.json`](evidence/2026-08-30-walls-of-text-exhibit-receipt.json).
Its payload SHA-256 is
`0b19a8756edaff4aeeadbc341647b65449e25f241c1f1d39622c1358fa697c98`; the local analysis
script SHA-256 is
`54a3b3ac3512f35efd227c3be1bcc7d6d2d23dd60da259e09ad8cacce2e41875`. The pass used
`tiktoken 0.11.0`, encoding `o200k_base`, and zero model calls. The exact suffix-automaton
6-gram implementation matched a direct brute-force implementation on 2,005 deterministic
cases. `[receipt: receipt_sha256, analysis_script_sha256, tokenizer, zero_model_calls,
method]`

The input inventory is six service-telemetry files and four Codex activity files, retained
only as per-file SHA-256s and aggregate inventory hashes—never paths. The service checksum is
**1,437 calls = 1,242 reads + 195 applies**, and the applies reproduce the prior canonical
checksum of **630,138 bytes**. The prior activity receipt's logical hashes admit exactly 52
of 72 currently discoverable edit payloads; the other 20 are excluded to keep the historical
corpus frozen. All ten receipt invariants pass. `[receipt: source_inventory, population,
invariants]`

Nothing source-bearing left the local pass. Neither this exhibit nor its receipt contains a
request, result, path, source body, prose fragment, project name, session identity, or
timestamped payload row. Published write identifiers are truncated content hashes; all
reported measurements are sizes, exact token counts, counts, shapes, hashes, formulas, or
aggregate overlap. `[receipt: privacy_mode, source_inventory,
invariants.no_content_or_paths_in_receipt]`
