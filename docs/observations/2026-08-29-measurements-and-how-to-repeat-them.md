# The 2026-08-29 measurements, and exactly how to repeat every one

Date: 2026-08-29
Seat: mayor@skiff
Purpose: **a reference, not a narrative.** Every number this program earned in 25 hours, with its
method, its sample size, the branch and commit that holds its evidence, and the command that
re-derives it. Companion logs carry the story: `...three-ways-to-name-the-wrong-thing.md`,
`...the-ledger-had-two-sides.md`, `...six-designs-died-and-the-tool-was-barely-used.md`.

## THE RULE THIS DAY EARNED — read before trusting anything below

**A single-source number is a hypothesis wearing a decimal point.**

Measured, not asserted: **every figure with n>1 and controls survived. Every single-source figure
died.** Five withdrawals in one day, all of them relayed as fact before they were checked. Section 3
lists them. Do not add a number to this file without a second derivation.

---

## 1. THE SURVIVING MEASUREMENTS

Each row: what, the number, how it was measured, and where the evidence lives.

### 1.1 Prefill/decode ratio — **1,284x**

**~14 microseconds to read a token; ~17.7 milliseconds to write one.**

- Method: 3 conditions, **n=9 each, interleaved C/A/B rotation**, with a tiny/tiny condition C
  subtracting the fixed floor.
  - A large prompt / 1-token output: median **6,954 ms** (MAD 441)
  - B 53-char prompt / 1,239-token output: median **25,781 ms** (MAD 264)
  - C floor: median **3,927 ms** (MAD 280)
  - `A-C` = 3,027 ms bought 219,544 input tokens -> **72,529 tok/s**
  - `B-C` = 21,854 ms bought 1,234 output tokens -> **56.5 tok/s**
- Controls that make it trustworthy: **linearity** across an 8-fold input range (67,299 / 50,700 /
  65,146 / 72,529 tok/s — flat, so not a large-prompt artifact); **network** at 163 ms for a 1 MB
  POST = 5.4% of the delta, so 1,284x is a **LOWER bound** (corrected: 76,657 tok/s); **caching
  verifiably excluded** (distinct payload SHAs, cached tokens never above 14,080 of 233,949).
- Floor decomposition: of the 3,927 ms, only **321 ms** is local process start.
- Environment: Anvil dev-a, 16 cores, load held 1.00-1.34 throughout, `gpt-5.6-sol`, reasoning
  `low`, codex-cli 0.147.0, **ChatGPT-subscription auth, no `OPENAI_API_KEY` in the profile**.
  Tokens counted from the **provider usage report**, never from characters.
- REPEAT IT:
  ```
  git checkout bench/prefill-decode-ratio
  bench/measure_prefill_decode_ratio.sh          # runs the cohort on dev-a
  bench/score_prefill_decode_ratio.clj           # folds; replays from committed TSV
  ```
  Evidence: `bench/results/2026-08-29-prefill-decode-ratio/`. Commits `8d3c6f6`, `6b58788`.
  **The fold replays from committed TSV — all five runs reproduce byte-identical verdicts from git
  alone, without rerunning anything.**

### 1.2 Emission rate — **3.5237 ms/byte** in production

- **R^2 = 0.9807, n=59, 76.3x byte spread**, intercept 2,684 ms. Fitted on production route
  telemetry, not the bench.
- **Supersedes the earlier ~6 ms/char bench figure.** Every magnitude computed before this rescales
  by **0.59**.
- **CRITICAL CAVEAT, learned the hard way: bytes are a PROXY for tokens, and the proxy BREAKS when
  the carriage format changes.** This is what killed the EDN proposal (1.6). Never price a
  format change in bytes.
- Evidence: commit `935cc0d`,
  `docs/observations/2026-08-29-write-side-emission-and-read-side-encoding-study.md`.

### 1.3 Copy is NOT cheaper than compose — **0.96x**

- n=9 interleaved, token counts matched within 3% and verified from the usage report.
  - B compose 1,234 tok, **56.4 tok/s**
  - D copy, unpredictable content, 1,228 tok, **54.0 tok/s**
  - E **copy the exact text B composes**, 1,198 tok, **54.0 tok/s**
- **Condition E is the design that makes this conclusive** — it holds content fixed and varies only
  whether the model had to construct it. **Supplying the answer did not speed up emitting it.**
  Predictability made no difference (D/E = 1.00).
- Later **extended from integer sequences to real Clojure** (1.6), closing the domain gap.
- Evidence: Appendix B, commit `dd36336` on `bench/prefill-decode-ratio`.

### 1.4 Transcription fidelity — **324/324 byte-exact**

- 24 adversarial identifiers, **six trap families**: underscore/hyphen, case-only, trailing `?`/`!`/
  `*`, deep discriminators, digit confusables, near-identical filenames. Order reshuffled per
  replicate. Three arms (reproduce-all, select-then-identifier, select-then-ordinal), 54-216 answers
  each, **including 130 answers whose target was one character from a sibling**.
- **Zero observed is not zero.** Rule of three at 95%: true error rate bounded below **1.39%**
  (reproduction) and **5.56%** (selection).
- **The finding that outranks the rate:** a mistyped identifier is usually GARBAGE (server refuses —
  safe); a wrong ordinal is almost always ANOTHER VALID CANDIDATE (server accepts — silent wrong
  subject). **Equal error rates, unequal risk.** Ordinals tied at 100% and are still the worse
  encoding.
- Evidence: Appendix B, `dd36336`.

### 1.5 Constrained decoding is LIVE on tool arguments

- Three probes, **5/5 unanimous each**: with `--output-schema` requiring an integer, an instruction
  to emit a string plus an extra key returned `{"value":0}` **5/5**; without a schema, invalid JSON
  permitted **5/5**; without a schema, invalid EDN permitted **5/5**.
- **Consequence:** production tool-call JSON is not *unlikely* to be malformed — it is **enforced to
  zero at the sampler.** A carriage living inside a JSON *string* cannot inherit that. Enforcement
  **prevents** at zero cost; a validator **detects** and charges a retry turn.
- Evidence: Appendix C, commit `93d9918`.

### 1.6 EDN carriage — measured and rejected

- Exact reviewer grammar, **no invented variants**; full outer `{clj: <EDN string>}` wrap tax
  included; file paths stayed strings. **Exact round-trip: reads 1242/1242, writes 195/195.**
  - writes 630,138 -> **599,069 bytes (+4.931% saving)** but 156,129 -> **158,297 tokens (1.389%
    GROWTH)**
  - reads 455,185 -> **465,680 bytes (2.306% GROWTH)**, 120,415 -> **135,169 tokens (12.253%
    GROWTH)**
  - Same sign under a second tokenizer (cl100k).
- Then measured on **generation time**, not just count: token ratio 1.103, time ratio 1.090,
  **ms/token 0.988.** Time tracks count; **no domain-coherence discount.**
- Fanout **inverted** the prediction: saving at 1-2 forms, **cost at 3-5 and 6+**.
- Evidence: commit `d0c11a59` on `experiment/emission-compression-screen`; Appendix C on
  `bench/prefill-decode-ratio`.

### 1.7 Name-derived mnemonic compression — **24.277%**, real but unusable

- Deterministic `~` + shortest unique literal prefix, exact equality only, against the frozen basis
  of 9 files / 37 owners. Exact round-trip decode **true**; label maps **injective**.
  - file-only 2,421 B (**-15.674%**), owner-only 2,624 B (**-8.603%**), combined 2,174 B
    (**-24.277%**)
  - Of 1,098 subject chars, **568 (51.73%) are qualified file paths**; zero owner values namespaced.
  - Collisions handled by deterministic lengthening: 3/9 files, 23/35 owners. **Zero identical owner
    names across files.**
- **Blocked on safety, not size.** See 2.2.
- Evidence: commit `0119cbe7`.

### 1.8 Adoption census — reproduced independently, twice

| | reviewer (121 sessions) | SURGEON1 independent |
|---|---|---|
| native write actions | 1,145 | **1,161** |
| surgeon mutation calls | 6 | **7** (4 accepted, 3 refused) |
| `apply_clojure_changes` | 0 | **0** |
| inspect calls | 400 | **443** |
| attempted write adoption | 0.52% | **0.60%** |
| accepted write adoption | — | **0.34%** |
| **addressable, src/test** | 6.1% | **6.0%** |

- **The 6% is NOT a self-hosting artifact:** the corpus is skewed (905/1,161 native actions are in
  clj-surgeon) but **65 of the 70 final src/test opportunities come from OTHER Clojure repos.**
  That is the falsification path, and it was run.
- **What did NOT reproduce:** the 3/6 = 50% failure rate (actual **3/7 = 42.9%**); the 8.7%
  middle rung (independent ceiling **13.3-14.0%**); and the claim that failures are universally the
  backslash-heavy class (**one of three refusals had only 3 backslashes**).
- Refusal cost is measured, not assumed: successful mutation followed **26.4 s, 13.8 s, 47.7 s**
  after each refusal.
- REPEAT IT:
  ```
  git checkout experiment/adoption-census-independent
  python3 dev/experiments/adoption_census_independent.py
  ```
  Byte-identical replay, privacy scan, and `study-agent-usage` self-test all pass.
  Fold: `bench/results/2026-08-29-adoption-census-independent/independent-fold.json`.
  Commit `286aa206`.

### 1.9 Ceremony attribution — **45.7%**

- 279 turns, **5,499** post-decision actions, median 12, mean 19.7, p90 41, max 147, **zero turns
  ending at their decision** — the COUNT reproduces exactly.
- Attribution is denominator-sensitive and all three are reported: **20.4%** of all post-decision
  items, **57.4%** of tool actions, **45.7%** including entrained reasoning. The third is binding and
  is measured: of 2,622 post-decision reasoning items, **1,347 (51.4%)** are immediately followed by
  a doctrine-attributable action.
- **Behavior does NOT track clause density:** the repo with **no** push clause has the **highest**
  attribution rate (60%) and 1.85 git actions/turn, versus 2.79 in a repo carrying the verbatim
  MANDATORY WORKFLOW block. **Deleting a clause would not delete the behavior.**
- Realizable scoping prize **~359 actions, ~10.2%**, whose largest component is **254 `git status`/
  `git diff` calls the repo's own instructions ALREADY FORBID** — a compliance gap, not a doctrine
  gap.
- Evidence: commit `7682abf` on `screen/ceremony-attribution`.

### 1.10 MCP catalog floor — **~40.75 ms fixed, NO byte slope**

- **14 counterbalanced blocks x 7 arms = 98/98** environment-valid, semantic-exact, route-adherent.
- Fixed local MCP effect **+40.75 ms**, 95% CI **+22.83..+68.28**.
- **Adding ~64 KiB at fixed one-tool shape: -0.50 ms, CI -55.46..+55.35. No slope.**
- Real full catalog 48,045 B vs inspect-only 13,154 B: **-143.5 ms, CI -847..+276. No win.**
- **Under-declaration silently hides tools — do not shrink or hide the catalog for speed.**
- Two failed attempts retained in the chart as **invalid losses**.
- Evidence: commit `87a53ec`.

### 1.11 Body fidelity — the tool does NOT corrupt

- **No-model HTTP MCP bisection:** JSON args decoded exactly; `apply_clojure_changes` committed
  **5/5 exact sensitive substrings** — backslash replacement, regex literal, literal `\n`, literal
  `\u` escapes, nested quotes — at **wire max backslash run 9**, `verification_complete=true`.
  Malformed source refused `invalid-intent-form` / `source_unchanged` / byte-identical.
- Model emission replay: **5/18 wrong-but-carriage-valid** (JSON 2/9, EDN 3/9), **only 1/18 also
  valid Clojure**; four of five meet the existing refusal.
- **Registered backslash-depth prediction FAILED and was reported as failed:** clean depths all
  **9**; corrupt JSON `[7,9]`, corrupt EDN `[5,5,5]`. **Deep escaping marks difficulty, not failure.**
- Exposure, exact: 195 writes / 630,138 canonical bytes; backslashes **43/195 (22.05%)**, regex
  literals **15/195 (7.69%)**, literal Unicode escapes **0**, union **45/195 (23.08%)**.
- Evidence: commit `2290192c` on `experiment/body-fidelity-audit`.

### 1.12 The promoted win, for reference

**Closed compact relations: 289.507 s -> 39.369 s = 7.35x.** Request bytes 6,509 -> 2,871. Live
canary 51 effects / 9 files in 1.64 s. **It won because it compressed shared STRUCTURE, and the
corpus repeats structure — 44 distinct subjects across 47 occurrences means it does NOT repeat
subjects.** That one premise explains six subsequent failures.

---

## 2. THE REFUSALS, AND THE LAW THEY ESTABLISH

**COMPRESS REPETITION, NEVER COMPRESS IDENTITY.** Error detection runs on redundancy; compression
removes redundancy; a maximally compressed subject reference is **by construction maximally
undetectable when wrong.** Measured twice: re-adding the redundancy costs almost exactly what
removing it saved.

**2.1 Positional line numbers** — cut payload 23.67%, **mutated the WRONG FILE with `ok=true`.
Purged, not gated.**

**2.2 Opaque mnemonic labels** — two owners containing identical `:old`, labels `mn/s01`/`mn/s02`,
intent stated for alpha, valid label for **beta** submitted. Result: `ok=true`,
`verification_complete=true`, **beta mutated, alpha untouched.** *"Observationally identical to
intentionally selecting the other owner."*

**2.3 Guarded labels** — fitted cost model **205 chars fixed + 35 per guard-char across 35 sites**.
A 128-bit (22-char) guard leaves **7 bytes of 982 saved = the guard consumes 99.3% of its own
prize.** 14/14 deletions admit no guard at all.

**2.4 Single-anchor splice** — **`after` is ALREADY a shipped alias for `to`**; the proposal inverted
it in the same edits vector, so `{:before anchor :after newtext}` **replaces the anchor and fails
OPEN** (the guard refuses only *incomplete* pairs). Separately: insertions are **28.22%** of edits,
replacements **68.46%** holding **83.2%** of from/to bytes; whitespace-drift screen returned **3.2%**
against a 5% kill; and `find`+`insert_after` **already ships in 941/4,319 ops**.

**2.5 Declared-intent compression** — **93.7% of requests net NEGATIVE** (178/190), median **-82 B**.
Flagship: gross mnemonic saving 170 B against a declaration lower bound of **1,796 B** = **net
-1,626 B, +56.6% growth.** The n>=2 break-even law holds, but declaring only repeated subjects
leaves 41 single-use subjects outside authority — **invalid by the complete-intended-set law.**

**2.6 Counts as a guard** — falsified concretely: alpha and beta both yield `{:files 1 :edits 1}`;
`counts_guard_accepts=true` while `declaration_guard_accepts=false`. **Counts bind cardinality, not
identity.**

**2.7 The standing law on server authority** — a deliberately wrong but *valid* replacement commits
cleanly. **The frozen basis proves the OLD bytes, not the INTENDED new ones.** No generic server
refusal exists without independent effect authority; **same-model hashes and escape-count heuristics
are not authority.**

---

## 3. THE WITHDRAWALS — all five, and what they share

| withdrawn | claimed | actual | why it died |
|---|---|---|---|
| candidate-list truncation | cap at 10 vs median 27 owners, 33.5 min | **never existed** | 137/137 refusals returned complete lists; 119/137 still triggered a redundant re-read |
| "191 derivable corrections" | 191 mechanically authoritative | **hint-only** | candidate vocabulary carries no authority |
| ceremony wall | **21.2 h**, 812x | **5.68-9.74 h**, 218x-373x | one turn at `coverage_ratio` 0.0046 supplied **54.2%**; 41.5M of its 41.7M ms were `unattributed-gap` |
| catalog tax | **+490 ms/turn** | **~40.75 ms**, no slope | single observation; 98-call counterbalanced cohort did not reproduce it |
| body corruption | **~17% in production** | **harness never called MCP** | `parse-ok` meant outer carriage only; the real path commits 5/5 exact |

**Every one had a single source. Every one was relayed as fact before being checked. Every
correction came from a seat re-deriving rather than trusting.**

---

## 4. HOW TO REPEAT THE WHOLE ANALYSIS

### 4.1 The paved road for agent telemetry

```
make study-agent-usage
make study-agent-usage AGENT_USAGE_ARGS='--since 2026-08-05T00:00:00Z --until 2026-08-06T00:00:00Z'
make study-agent-timeline RECEIPT=/tmp/receipt.json
make study-agent-usage-self-test
```
Read `skills/study-agent-usage/SKILL.md` FIRST. It defines the privacy contract, the receipt schema
(v6 carries argument byte counts, target SHAs, adjacent-read relations, and the post-Surgeon
boundary clock), and the adversarial-interpretation rules. **The complete JSON receipt is the
counting authority, not the printed aggregate.**

Telemetry payloads live at `~/.local/state/clj-surgeon/mcp/telemetry/*.jsonl` and **only when
`telemetry_mode` is `full`** — sessions running `metrics` retain no payloads. Check the mode before
concluding anything from an empty window.

### 4.2 Branches holding tonight's evidence

```
bench/prefill-decode-ratio               ratio, copy/compose, fidelity, carriage (Appendices A/B/C)
experiment/emission-compression-screen   EDN byte/token screen (X1/X2)
experiment/adoption-census-independent   independent census + fold + script
experiment/body-fidelity-audit           the audit that withdrew the 17%
screen/ceremony-attribution              doctrine attribution + terminal-signal screen
screen/splice-edit-grammar-zero-model    the three splice screens
docs/captains-logs-2026-08-29            all four narrative logs (this file included)
```

### 4.3 The procedure that produced the good numbers

1. **Predeclare validity before measuring** — `environment_valid`, `semantic_correct`, mutation/
   effect receipt verified, required verifier passed, **plus any route criterion stated in the
   hypothesis.** Report `route_adherent` **separately**; never overwrite one result field because
   another failed.
2. **Interleave and counterbalance.** N-R-R-N then R-N-N-R breaks position aliasing. n=9 per
   condition was sufficient for the ratio; **n=30 was NOT sufficient for a 2-point error-rate
   difference** — bound it and say so.
3. **Subtract a floor condition.** Condition C is why `A-C` and `B-C` mean anything.
4. **Add the control nobody asked for.** Linearity and network checks are what made the ratio
   trustworthy; condition E is what made copy-vs-compose conclusive.
5. **Register the prediction and its KILL CRITERION before running**, and **report a failed
   prediction as failed** rather than loosening the threshold afterward. Both happened tonight; both
   were more useful than a confirmation.
6. **Zero-model screen FIRST.** Count what exists before spending a cohort. Five ideas died for
   essentially nothing. A test that costs more than the maximum it can win is not worth running.
7. **Keep losses in the chart.** A benchmark that can no longer say *"don't use this here"* is a
   victory chart. Retained invalid attempts are part of the record.
8. **Report headroom, never consumption** — "84% remaining," never "16% used" — and name which seat
   each meter gates.
9. **Bound aggregate wall by coverage.** The ratchet this day earned: **emit per-turn
   `coverage_ratio` alongside any wall figure and refuse to aggregate below a stated threshold.**
   One field would have stopped an idle turn from becoming 54% of a headline.
10. **Verify before relaying. An agent's report is a hypothesis.** Five figures prove it.

### 4.4 Environment notes that matter for reproduction

- Cohorts run on **Anvil dev-a** (`root@100.66.152.23`), which has a separate quota from skiff.
  Skiff's meter is **shared by both surgeons**; report both.
- `gpt-5.6-sol` on **ChatGPT-subscription auth** — no `OPENAI_API_KEY` in the profile, so marginal
  cash cost is **$0**; the real currency is weekly quota.
- **Count TOKENS from the provider usage report, never characters**, and never assume bytes track
  tokens across a format change.
- Load matters: the ratio cohort held **1.00-1.34** throughout. A gate that failed at load 210+
  earlier in the program was re-measured at load 4-9 and failed identically — **check load before
  blaming it.**
