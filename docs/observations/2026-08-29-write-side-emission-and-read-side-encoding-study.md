# Write-side emission and read-side encoding — an evidence study

<!-- agent-usage-window-end: 2026-08-30T02:09:33.141926Z -->

A zero-model study of every clj-surgeon structural write in the window and the reads that
preceded it. The question was narrow: of the bytes a model must type into a mutation call,
how many were already handed to it, how many follow from a rule, and how many are genuine
judgment. No cohorts were run. No model was called. No quota was spent.

## Window and sampling

- **UTC:** 2026-08-22T00:00:00Z → 2026-08-30T02:09:33.141926Z (8.09 days)
- **Pacific:** 2026-08-21 17:00:00 PDT → 2026-08-29 19:09:33 PDT
- **`since` source:** explicit `--since` argument, not a marker.

Two receipts were collected. The paved-road default run (`make study-agent-usage`, marker
lower bound `2026-08-29T10:36:32.586284Z`) returned `status: ok` and exit 0, but contained
only **11 write actions** — too thin to answer a question about writes. The window was
therefore widened to eight days with an explicit `--since`; that run also returned
`status: ok` and exit 0 and **strictly contains** the default window, so the marker above is
the wide receipt's `next_marker` and advances the next default window without skipping
anything.

**Exclusions.** CLI-transport structural operations are excluded from every byte figure: the
schema places CLI arguments outside the byte law because their quoting and truncation
semantics are not comparable to structured MCP JSON. Eight of 128 write clock items were CLI
and are counted only in the inventory. `transform_clojure` does not appear anywhere in this
corpus and is reported as absent, not as zero-by-inference.

**Instrumentation note — a receipt gap, not a repair.** Schema v6 attaches
`structural_target_sha256`, cardinalities, and `target_relation` to `surgeon-read` actions
only. `surgeon-apply` actions carry `action_evidence` byte counts and hashes but no
structural target and no adjacent-read relation: `target_relation` was present on **0 of 67**
boundaries whose next action was a write. The receipt therefore cannot answer questions 1, 2,
3 or 5 by itself. Those four were answered by a local pass over the service's own telemetry,
described next. This is a genuine field gap in the receipt and is named as the smallest
falsifiable improvement at the end.

**Local pass, and why it is trustworthy.** The byte-level questions were computed locally from
the clj-surgeon MCP service telemetry for the identical window, and only aggregate statistics
left that pass. No request content, no result content, no source, no path, no prose, no
project context appears in this document. The local pass reconstructed **1,437 tool calls /
1,242 `inspect_clojure` / 195 `apply_clojure_changes`**, which equals the receipt's aggregate
for the same window exactly. The counting authority and the local pass agree to the call.

## Codex versus Claude scoreboard

| | Codex | Claude |
|---|---|---|
| sessions in window | 148 | 102 |
| clojure-relevant sessions | 114 | 43 |
| task turns with clocks | 684 | 0 |
| surgeon-using turns | 227 | 0 |
| clj-surgeon calls | 3,006 | 7 |
| surgeon-read actions | 2,141 | 2 |
| surgeon-apply actions | 114 | 1 |
| surgeon-plan actions | 15 | 2 |
| native-patch actions | 1,650 | — |
| native-read actions | 6,041 | 940 |
| verify actions | 1,567 | 55 |
| skill loads | 168 | 1 |

Claude is effectively a non-participant in the structural route this window: five of its seven
clj-surgeon calls were CLI `:edit`, one `:help`, one `inspect_clojure`. Every quantitative
result below is a Codex result. Do not generalize it to a caller that did not appear.

## Actual structural operations

Service telemetry, 7 server sessions, 1,437 MCP tool calls:

| tool | calls | ok | refused |
|---|---|---|---|
| `inspect_clojure` | 1,242 | 1,023 | 219 (17.6%) |
| `apply_clojure_changes` | 195 | 131 | 64 (32.8%) |
| `transform_clojure` | 0 | — | — |

Client-side clock items add 52 `edit_clojure` and 8 CLI write actions on routes that did not
reach this MCP service. Native `apply_patch` remained the dominant editing transport overall
(1,650 native-patch actions against 114 surgeon-apply): structural writing is still the
minority route, and nothing here should be read as adoption evidence.

Write size: min 89 bytes, p25 489, **median 1,564**, p75 3,726, p90 8,933, max 29,598.

---

## Q1 — What the model had to type into a write

195 MCP writes, **630,138 canonical request bytes** total.

| role | bytes | share |
|---|---|---|
| (a) subject — files, owner names, selectors, `find`/`from` anchors | 268,125 | **42.6%** |
| (c) new content — `replace`, `insert_after`, `insert_before`, `to` | 290,842 | **46.2%** |
| (b) operation — mode, verify, action | 255 | 0.0% |
| (b) guard — `expect` counts | 771 | 0.1% |
| (d) protocol — model-authored change `id` | 4,462 | 0.7% |
| other leaves | 5,900 | 0.9% |
| (d) JSON structure — keys, punctuation | 59,783 | 9.5% |

Field level, which is where the surprise is:

| field | bytes | share |
|---|---|---|
| `changes.insert_after` | 133,700 | 23.4% |
| `changes.replace` | 130,761 | 22.9% |
| `changes.find` | 117,184 | 20.5% |
| `edits.to` | 73,091 | 12.8% |
| `edits.from` | 45,161 | 7.9% |
| `changes.insert_before` | 26,338 | 4.6% |
| `changes.files` | 8,241 | 1.4% |
| `edits.file` | 7,626 | 1.3% |
| `changes.forms` | 5,923 | 1.0% |
| `edits.within.form` | 5,322 | 0.9% |
| `changes.id` | 4,448 | 0.8% |

**The identity locator is not the cost.** Every file path, owner name, and `within.form` in
the entire corpus totals **27,393 bytes — 4.3% of the write traffic.** The subject cost is
almost entirely **old text quoted back as an anchor**: `find` plus `edits.from` is **162,345
bytes, 28.5% of leaf bytes.** A design aimed at shortening how a subject is *named* is aimed
at 4.3% of the problem. A design aimed at not re-quoting text the server already holds is
aimed at 28.5%.

## Q2 — How much of it was already in hand

Copy coverage measured by 6-gram greedy longest-match against the preceding read result text.
Matches shorter than six characters count as novel, so every figure below is conservative.

| role | bytes | vs immediately preceding read | vs all mission reads | vs the previous (refused) write |
|---|---|---|---|---|
| subject | 268,125 | 43.2% | 49.7% | 57.5% |
| content | 290,842 | 31.2% | 39.5% | 69.7% |
| protocol | 4,462 | 16.1% | 22.9% | 58.2% |
| guard | 771 | 0.0% | 0.0% | 0.0% |
| operation | 255 | 0.0% | 0.0% | 0.0% |
| **all leaf** | **570,355** | **36.7%** | **44.2%** | **63.4%** |

Best-of the two upstream sources: **462,794 bytes = 81.1% of leaf bytes, 73.4% of the full
request.**

**Restricted to the population the prediction named** — writes whose file set is `exact` or
`same-files` against the immediately preceding read, n = 46, 197,480 bytes:

- copyable from the **immediately preceding read alone: 83.6% of leaf bytes, 76.9% of the full request**
- copyable from **mission reads: 89.2% of leaf, 82.0% of the full request**
- subject 91.0% / content 76.0% from the immediate read alone

Exact verbatim presence, a stricter test than coverage: **34.9%** of individual subject leaf
strings appear byte-identical somewhere in a preceding mission read (300 of 859, restricted to
the 101 writes that had any preceding read).

### The three-way split

Priority classification, each byte counted once: grammar and server-derivable guards first,
then upstream-copyable, then the remainder.

| | bytes | share |
|---|---|---|
| **RULE-GENERATED** — JSON structure, `expect` guards, operation enums, change ids | 65,271 | **10.4%** |
| **COPIED** — verbatim in a preceding read or in the call that was just refused | 459,690 | **73.0%** |
| **DECIDED** — authored, appears nowhere upstream | **105,177** | **16.7%** |

On the addressable population alone (n=46): RULE 8.8% / COPIED 82.3% / **DECIDED 8.9%**.

**Per write, DECIDED is small and heavily skewed:** min 0, p25 52, **median 181 bytes**, p75
468, p90 1,236, max 13,923, mean 539. **53.3% of writes contain under 200 bytes of genuine
judgment.** Five contain none at all.

This is the study's most important single number. Against the measured emission rate
(below), the entire eight-day corpus of structural writes contains **6.2 minutes of
irreducible authorship** inside **37.0 minutes of typing**.

## Q3 — What could not be copied

The 105,177 DECIDED bytes are 18.7% of subject bytes and 18.4% of content bytes — the two
roles are equally novel, which is itself a finding: the model is not merely re-typing old text
and inventing new text, it is inventing *within* both. The novel subject bytes are new anchor
text at sites the read did not return, and new owner names for forms that do not yet exist.
No read-side encoding can supply either. **DECIDED is the floor, and it is 16.7%.**

## Q4 — Decision boundaries per write

Receipt clock, same turn, counter reset at each write: **74 of 128 writes (57.8%) had zero
`surgeon-read` before them**; mean 1.77 reads per write. Local telemetry, mission bounded by
the previous write: **94 of 195 (48.2%) had zero preceding read**; mean 6.15, median 1;
1,199 reads consumed by 195 writes; the largest single chain was 129 reads before one write.

Boundary wall — the interval from the previous surgeon action to the next action start:

| transition | n | median | p90 | total |
|---|---|---|---|---|
| → `surgeon-read` | 913 | 7.3 s | 25.6 s | 175.3 min |
| → `surgeon-apply` | 67 | 18.7 s | 54.3 s | 29.7 min |
| read → read, same transport, prior call ok | 594 | 7.4 s | 29.3 s | **124.3 min** |
| after a **failed** surgeon call | 367 | 7.1 s | 20.4 s | 65.2 min |
| failed → recovery read | 225 | 6.6 s | 13.4 s | **33.5 min** |
| failed → retry write | 19 | 19.9 s | 54.3 s | 7.9 min |

Total boundary wall 561.6 min; measured model reasoning inside those boundaries 260.0 min.
**17.6% of all decision boundaries in this corpus exist only because the previous call was
refused.**

Ranked by removable whole-boundary wall: (1) read→read chains, 124.3 min across 594
boundaries; (2) recovery reads after refusal, 33.5 min across 225; (3) retry writes after
refusal, 7.9 min across 19.

## Q5 — Refusals, and whether the server already knew the answer

283 refusals. The distribution is dominated by one class.

| error type | n | carries `remedy` | carries `next_call` |
|---|---|---|---|
| `batch-form-selection-failed` | 191 | 0 | 0 |
| `invalid-intent-form` | 32 | 32 | 0 |
| `invalid-mcp-request` | 14 | 4 | 0 |
| `expect-count-mismatch` | 11 | 11 | 0 |
| `source-file-not-found` | 7 | 7 | 0 |
| `inspect-cardinality-mismatch` | 6 | 0 | 0 |
| `overlapping-intents` | 5 | 5 | 0 |
| `transaction-expectation-mismatch` | 5 | 5 | 0 |
| all others | 12 | 8 | 1 |

**Exactly one refusal in 283 carried a next call.**

The largest class is mechanically derivable *and the server proves it at refusal time*.
`batch-form-selection-failed` responses already carry a `form_candidates` list — the server
has parsed the file and knows every owner in it. But:

- **95.3%** carry candidates at all;
- **86.4% truncate the list** — median `available_form_count` is **27**, maximum **364**, and
  the shown list is hard-capped at **10**;
- the name the model actually asked for appears in the shown list **16.8%** of the time;
- `next_action` is `correct_request` on **all 191** — the server instructs a correction it is
  withholding the material for.

What the model does next is the cost. **98.4%** of these refusals are followed immediately by
another read. **88.5%** are followed by a *successful* read on the *same file* within seven
calls, median result **11,533 bytes**, **2.72 MB total** — the server re-serializing a file it
had already parsed. And **70.7%** of those recovery reads return the very name the refused
call asked for, meaning the answer was inside the server's own working set when it refused.

**Verdict on Q5: 191 of 283 refusals (67.5%) had a mechanically derivable correction at
refusal time, and the recovery turn is removable.** Cost of not carrying it: 225 recovery-read
boundaries, **33.5 minutes of boundary wall**, and 2.72 MB of redundant read results.

On the write side, 64 refusal→retry pairs re-emit **290,541 bytes = 46.1% of all write bytes
in the corpus**, of which **90.0% (261,491 bytes) belong to the shape-and-guard classes the
server can decide before the model types anything.**

---

## Registered predictions — scored

Counted before comparison. Raw numbers first, verdict second.

### 1. COPYABLE SHARE — **CONFIRMED**
Predicted ≥ 60%; demote below 40%; withdraw below 25%.
**Measured: 83.6% from the immediately preceding read, 89.2% from mission reads** (leaf bytes,
n=46, relation `exact` or `same-files`). As a share of the full canonical request including
JSON structure: 76.9% and 82.0%. Confirmed with wide margin, and conservatively measured —
6-gram floor, sub-6-character matches discarded.

### 2. ADDRESSABLE MARKET — **FALSIFIED**
Predicted ≥ 50%; niche below 30%.
**Measured: 23.6%** on the strict reading (relation to the last read since the previous write:
`exact-same-file-set` 19.0%, `same-files` 4.6%, `overlapping` 6.7%, `disjoint` 21.5%,
**no preceding read at all 48.2%**). **44.1%** on a widened reading that allows intervening
writes (`exact` 30.8% + `same-files` 13.3%). Both fall short; the strict reading falls under
the niche floor.

**The diagnosis matters more than the verdict.** The market is small because **nearly half of
all writes are not first attempts** — they follow another write, usually a refused one, with
no intervening read. Read-side annotation cannot reach a call whose upstream is a refusal.
That is not a reason to abandon the family; it relocates it. The refusal is the read the model
actually consulted.

### 3. REFUSAL-RETRY IDENTITY — **SPLIT: falsified strictly, confirmed on near-identity**
Predicted ≥ 50% equal or near-identical; below 30% means models genuinely re-decide.
- Identical canonical argument SHA: **1 of 64 = 1.6%** — falsified.
- Retry ≥95% byte-copied from the refused call: **57.8%**; ≥90%: 60.9%; ≥80%: 68.8%.
- **Median retry/refused byte overlap: 99.6%.** Same file set: 68.8%. Retry succeeded: 62.5%.

Models do **not** re-decide after a refusal. The typical retry is a byte-for-byte re-emission
of a call the server already received, with a surgical delta. Attaching corrections to
refusals is therefore right, not merely profitable — but attaching the *whole corrected call*
for the model to re-type saves nothing, because the cost is the typing.

### 4. REFUSAL FREQUENCY — **CONFIRMED, overwhelmingly**
Predicted ≥ 15%; demote below 5%.
**68.4% of missions contain at least one refusal** (80 of 117 runs, mission bounded by a
10-minute gap). **26.5%** contain at least one write refusal. Restricted to write-bearing
missions: **62.0% (31 of 50).** Four times the prediction.

### 5. THE FALSIFIER — **correlation CONFIRMED; rate FALSIFIED downward**
Predicted ~6 ms/char with R² > 0.9, given adequate spread.

| population | n | byte spread | slope | intercept | R² |
|---|---|---|---|---|---|
| next = `surgeon-apply` | 59 | 375 → 28,605 (**76.3×**) | **3.5237 ms/byte** | 2,684 ms | **0.9807** |
| next = `surgeon-apply`, 5% residual-trimmed | 55 | 375 → 11,696 (31.2×) | 3.5574 | 2,659 | 0.9593 |
| next = `surgeon-read` | 460 | 156 → 2,258 (14.5×) | 4.3968 | 1,048 | 0.2814 |
| next = `surgeon-read`, trimmed | 414 | 156 → 1,087 (7.0×) | 3.8170 | 1,160 | 0.6206 |
| pooled, next = any | 526 | 137 → 28,605 (208.8×) | 3.6592 | 1,506 | 0.9051 |

**The correlation is not flat. R² = 0.9807 on the write arm over a 76× byte spread, n = 59.**
The emission-time model transfers to the production harness. The *rate* does not: 3.5237
ms/byte is **59% of the predicted 6 ms/char**. Every magnitude in the program stands, rescaled
by 0.59.

The read arm's weak fit is a spread artifact, not a contradiction: read requests span 14.5×
against a 1,048 ms fixed floor, so the floor dominates. Trimming outliers lifts R² to 0.62 at
a slope of 3.82 — consistent with the write arm. **This is exactly the case the prediction
warned about: do not report a weak R² as a result when the spread is too narrow to fit.** The
write arm has the spread; the read arm does not.

The **2,684 ms fixed intercept** is a second finding. A write call costs about 2.7 seconds
before the first byte. That is the per-call tax any "fuse two calls into one" proposal
collects, independent of bytes.

---

## The count nobody asked for: ceremony after the last decision

279 task turns contained at least one structural write (surgeon-apply or native-patch). Counting
every action *after the last* such write in each turn:

- **5,499 post-decision actions.** Median **12 per turn**, mean 19.7, p90 41, max 147.
- **Zero turns of 279 ended at their last decision.** Not one.
- 2,560 post-decision `model-reasoning` items (median 6 per turn).
- Composition: model-reasoning 2,560 · shell 1,025 · model-message 561 · git 434 · native-read
  223 · coordination 197 · other-tool 178 · collaboration 146 · verify 63 · surgeon-read 37 ·
  live-probe 17 · skill-load 2.
- **Post-decision wall: median 68.7 s per turn, 1,273.4 minutes — 21.2 hours — across the window.**

Only 63 of those 5,499 actions are typed `verify`. The rest is shell, git, narration, and
re-reading. This is the largest single pool of deterministic ceremony in the corpus and it is
a pure turn-deletion prize: it sits *after* the semantic work is finished, so removing it
cannot cost correctness. It is also entirely outside the encoding question — no argument shape
touches it.

---

## Successful behavior, failures, counterfactual limits

Working well: the write route's guards fire loudly and early (32.8% of writes refused, none
silently), verification participates in the transaction, and refusals name a typed error class
in every case. The service telemetry and the receipt agree call-for-call, which means the
counting apparatus is sound.

Failing: **the server refuses with knowledge it does not hand over.** One `next_call` in 283
refusals; a candidate list truncated to 10 when the median file has 27 owners; 88.5% of
selection failures answered by a full re-read of a file the server had already parsed.

**Counterfactual limits, stated plainly.** These are observations of one caller (Codex) in one
repository over eight days, dominated by a single session that contributed 89 of 128 write
actions. Copy coverage is a 6-gram mechanical measure and is not proof that a model *could*
have copied rather than re-derived; it proves only that the bytes were available. The
emission-rate fit is correlational — bytes and difficulty are confounded, since larger calls
are plausibly harder calls. Nothing here has been tested against a controlled counterfactual,
and no proposal below has been implemented or gated.

## Progress against the acceptance gate

No efficiency claim is made or supported. On the repository's ladder — capability implemented →
mechanism verified → self-hosted → fresh caller succeeds → controlled efficiency gate passes —
this study is measurement upstream of the first rung. It identifies where a gate should be
pointed, and it disqualifies three candidate designs before any of them cost an implementation.

## The smallest falsifiable improvement

**Attach `structural_target_sha256`, file/selector cardinality, and `target_relation` to
`surgeon-apply` clock items, exactly as they are already attached to `surgeon-read`.** The
receipt currently cannot state the relation between a write and the read before it — it was
absent on all 67 write boundaries — which is why the central question of this study required a
local pass. One field triple makes the receipt self-sufficient for every question asked here.
It is falsifiable by rerunning this study from the receipt alone and reproducing 83.6% ± 2.

---

# Read-side encodings that set up minimal writes

Ranked by **measured** bytes and boundaries removed from this corpus. Every saving below was
computed by re-serializing all 195 real requests under the proposed grammar and canonicalizing
both, not by estimation.

**The governing law held throughout: compress repetition, never compress identity.** Every
proposal below either preserves subject bytes exactly or makes them *more* readable. Two
designs that would have compressed identity were considered and are refused at the end.

**First, the finding that reorders everything.** Request-shape code golf on writes is worth
almost nothing. All four shape optimizations applied together to all 195 writes save **26,803
bytes — 4.25% — 94.4 seconds of emission across eight days.** Meanwhile the refusal-driven
retry corpus alone is 290,541 bytes. **The prize is not the shape of the call. It is the calls
that should never have been typed twice.**

Second finding, and it corrects a premise: read *results* are free, but read *requests* are
not. The model typed **455,185 bytes into read requests** against **630,138 into writes** — a
**0.72 : 1 ratio**. Read results returned 11.68 MB at no typing cost, which is the 1,284×
asymmetry doing its work. But identity typed *into* a read costs the same 3.52 ms/byte as
identity typed into a write. **A lavish read result is free. A lavish read request is not.**

---

### 1. Untruncated refusal closure — the largest measured prize

**What the read result contains.** When `batch-form-selection-failed` fires, the refusal
returns **every** available owner name in full, not a list capped at 10, plus each name's
match count. Median 27 names at roughly 20 bytes each is about 540 bytes of result — free.
When exactly one available name is within edit distance 1 of the requested name, the refusal
additionally renders the corrected request in full and readable form.

**What the model then types.** For the disambiguation case, nothing — it re-sends the rendered
call. For the ordinary case it types the corrected read it would have typed anyway, median 272
bytes, but it types it **without a recovery round trip**.

**Measured saving.** 191 refusals of this class, 86.4% of them truncating a list the server
held. 225 failed→read boundaries, median 6.6 s, **33.5 minutes of boundary wall**; 183
recovery reads at median 11,533 bytes, **2.72 MB** of re-serialized result. At the measured
2,684 ms per-call fixed intercept alone, deleting 183 recovery calls removes **8.2 minutes**
before a single byte is counted.

**Why it does not compress identity.** It is the strict opposite: it *expands* identity from a
truncated 10 to the complete set of readable names. Nothing becomes an index, a handle, or a
label. A human reading the refusal sees more, not less.

**Zero-model screen.** For each `batch-form-selection-failed`, check whether the requested name
appears in the *full* available-form set but not in the shown 10. If it does, the truncation
alone caused the retry. Deterministic, replayable against stored telemetry, no model involved.
Falsified if fewer than 40% of these refusals are followed by a same-file recovery read;
measured 88.5%.

---

### 2. Server-owned guards — `expect` becomes a read-side fact

**What the read result contains.** Each returned form carries its exact match count for the
anchors present in it, bound to the snapshot hash the read already emits.

**What the model then types.** The `expect` block disappears from the write entirely; the
snapshot hash it already sends carries the staleness proof.

    {"changes":[{"at":"src/sample/views/review.clj#render-review",
                 "find":"…","replace":"…"}],
     "snapshot":"<sha from the read>"}

**Measured saving.** Dropping `expect` from all 195 real requests saves **12,502 bytes
(1.98%)** — small. But `expect-count-mismatch` caused **11 refusals whose retries re-emitted
13,533 bytes**, and `transaction-expectation-mismatch` five more at 16,643 bytes. Guard
failures are 0.1% of the bytes typed and 25% of the write refusals. Total attributable:
**42,678 bytes and 16 removed boundaries.**

**Why it does not compress identity.** `expect` is a count, not a name. Removing a number the
server computed better than the model guessed it removes nothing a reader needs to identify
the subject.

**Zero-model screen.** Replay every `expect-count-mismatch` refusal against the preceding
read's returned match count. If the server's count would have been correct in every case, the
guard was never the model's to supply. Deterministic.

---

### 3. Fan-out-aware locator closure — `path#owner`, on writes only

**What the read result contains.** Each returned form is rendered once in the exact byte shape
the write grammar accepts, as a parseable self-carrying string:

    "src/sample/views/review.clj#render-review"

**What the model then types.**

    {"changes":[{"at":"src/sample/views/review.clj#render-review",
                 "find":"…","replace":"…"}]}

instead of `{"files":["…"],"forms":["…"], …}`.

**Measured saving, and its measured limit.** Applied to all 195 writes: **8,109 bytes
(1.29%)**. Combined with server-assigned change ids (**6,192**, 0.98%) and server-owned
`expect` (**12,502**): **26,803 bytes, 4.25%, 94.4 seconds of emission over eight days.**
Hoisting a shared file to request level adds nothing once `at` exists.

**Applied to reads, the same encoding LOSES 91,372 bytes — minus 20.07%.** This is the most
useful negative result in the study and it falsifies format closure as a universal design.
The mechanism is fan-out, and it is measured: a write names **1.00 forms per change**
(217 of 218 changes name exactly one), while a read names **2.05 forms per file-scoped
sub-request**, maximum 62. Repeating a 37-byte path once per form is cheaper than stating it
once only when the fan-out is 1:1. **Format closure is a fan-out-dependent optimization, not a
format principle. Apply it to writes. Never apply it to reads.**

**Why it does not compress identity.** `path#owner` is strictly more readable than the
two-field form: the subject is a single human-legible string that carries its own meaning and
can be pasted into any other tool. It is a concatenation, not a reference into a table. A
reader who has never seen the read result can still tell exactly what is being mutated.

**Zero-model screen.** Re-serialize the stored corpus under both grammars and diff canonical
byte counts, split by fan-out. Already run: writes −8,109, reads +91,372. Any implementation
that does not reproduce those two numbers is wrong.

---

### 4. Read-request golf — bigger than everything on the write side

**What the read result contains.** Nothing new. This is a request-grammar change justified by
the 0.72 : 1 typed-byte ratio.

**What the model then types.** Read sub-request ids become server-assigned, and `operation`
is omitted when it is the default (`forms` in 1,873 of 2,326 sub-requests).

**Measured saving.** Dropping model-authored ids: **46,803 bytes (10.28%)**. Implying the
default operation: **37,460 bytes (8.23%)**. Together **84,263 bytes = 18.5% of read request
bytes = 297 seconds of emission** — **3.1× the entire write-side shape prize.** Every one of
the 2,326 read sub-requests in the corpus carried a model-authored id; not one of them named
anything in the codebase.

**Why it does not compress identity.** Neither field is identity. A change id is correlation
bookkeeping and the server can mint a *readable* one derived from the anchor
(`review.clj#render-review`), which is more legible than the model's own labels. A defaulted
operation is grammar. File paths and form names are untouched.

**Zero-model screen.** Re-serialize the 1,242 stored read requests without ids and without
default operations; confirm 84,263 bytes and confirm every request still resolves to the
identical structural target hash the receipt recorded.

---

### 5. Post-decision turn deletion — outside the encoding question, larger than all of it

**What it is.** 5,499 actions and **21.2 hours of wall** occur after the last structural
decision in a turn, across 279 turns, median 12 actions per turn, and **not one turn of 279
ended at its decision.** Only 63 of those actions are typed `verify`.

**Why it belongs in this list.** Every encoding above competes for a fraction of 37 minutes of
emission. This is 21.2 hours. It is deterministic, it is after the semantic work, and no
argument shape reaches it.

**Zero-model screen.** For each turn, classify the post-decision action sequence as
deterministic (fixed formatter, fixed linter, fixed test command, fixed commit) versus
responsive (an action whose arguments depend on a prior result). Count the deterministic
prefix. That count is deletable without a model, and the receipt already carries every field
needed to compute it.

---

## Two designs refused, on this corpus's evidence

**Opaque handles for mutation targets** — a short label, a positional index, or a numeric
reference returned by the read and quoted by the write. Refused. The measured prize is 4.3% of
write bytes, because identity locators are only 27,393 bytes of 630,138. **An encoding that
risks a valid-but-wrong subject with `ok: true` cannot be bought for 4.3%.** The arithmetic
alone disqualifies it, before the correctness argument is even reached.

**Server-rendered whole corrected calls for the model to re-send.** Refused as a *byte*
optimization, though it remains right as a *turn* optimization. Prediction 3 measured that the
median retry already re-emits 99.6% of the refused call. Handing the model a complete
corrected call it must type back changes nothing at 3.52 ms/byte. The saving only materializes
if the correction is applied without a full re-emission — which is proposal 1's disambiguation
case, and only there.

## What this study did not measure

Whether `find` and `edits.from` — the 162,345-byte anchor class, the single largest removable
category identified and the one no proposal above touches — could be replaced by a
form-scoped edit when the read has already proved the match is unique. The grammar supports
it (`edits.within.form` appears 190 times), the read already returns the match count, and the
subject would remain fully readable. Measuring it requires comparing each `find` string
against the form source the read returned, which this pass did not retain. **That is the next
measurement, and it is worth more than every encoding ranked above except the first.**
