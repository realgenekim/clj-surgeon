# E-NSWEEP — the sweep, not the point: the literal-patch fraction crosses one half at N\* ≈ 22–23, and E-CEILING80's "bounded to N ≲ 40" was a restriction, not a boundary

*forge@anvil, 2026-09-04T02:24Z. Cohort E-NSWEEP is the experiment E-CEILING80 §13 said its
own result implied: **"a sweep, not a point."** Twelve native arm-runs at N ∈ {30, 40, 55},
serial and interleaved, pooled with eleven arms already on record (8 at N=21, 3 at N=80) for
a 23-arm fit. Pre-registration frozen before arm 1 at `/home/forge/tmp/arms/ensweep/
preregistration.md`, sha256 `3ea3c4eaecd71dbc89be20afb0df404df42e4660def30f5cdef466757fa734dc`,
receipt `receipts/prereg.sha256`, written with **zero arm directories and zero rollouts on
disk** (the receipt records both counts). Instruments frozen in `FROZEN.sha256`, all thirteen
still matching `sha256sum -c` after the last arm. Per-arm receipts under
`/home/forge/tmp/arms/ensweep/ensweep-P-N-N*-*/`.*

> **WALL AND LOAD ARE RECORDED UNCONDITIONALLY AND ARE DESCRIPTIVE ONLY. NO WALL CLAIM IS
> MADE ANYWHERE IN THIS DOCUMENT.** Eight of twelve walls ran with 1-minute load above 8 and
> are starred `†`; the star marks them uninterpretable, not excluded. Every finding below
> rests on the load-immune meters: the strategy label, emitted characters, and call counts.

> **NO TOOL CELL WAS RUN, BY DESIGN, AND NO TOOL-VERSUS-NATIVE CLAIM IS MADE HERE.** The T
> payload is flat in N and has been measured three times (E-REG k=1 465–593 chars per
> committing call; E-AFFORD cell mean 966; E-CEILING80 cell mean 1,301). Spending four of
> twelve arm slots re-measuring a constant would have cost a whole N cell. This document
> measures one caller's strategy choice as a function of one variable.

---

## 1. The headline

**The crossing is at N ≈ 22, not at 40.** The literal-patch fraction is 4/8 at N=21, then
**1/4, 2/4, 0/4** at N=30, 40, 55, and 0/3 at N=80. The pre-registered logistic fit over all
23 arms puts P(literal) = 0.5 at **N = 22.23**, so **N\* = 23**.

| N | n | literal | generated | stream-edit | literal fraction | source |
|---|---|---|---|---|---|---|
| **21** | 8 | 4 | 4 | 0 | **4/8 = 0.500** | pooled, reused |
| **30** | 4 | 1 | 3 | 0 | **1/4 = 0.250** | E-NSWEEP |
| **40** | 4 | 2 | 2 | 0 | **2/4 = 0.500** | E-NSWEEP |
| **55** | 4 | 0 | 4 | 0 | **0/4 = 0.000** | E-NSWEEP |
| **80** | 3 | 0 | 3 | 0 | **0/3 = 0.000** | pooled, reused |

**E-CEILING80's "the emission law is BOUNDED to N ≲ 40" is now measured, and it was too
generous by a factor of nearly two.** That restriction was a pre-registered honouring of a
triggered withdrawal condition, written when the only two points on the curve were 21 and 80.
With three points in between, the caller has already mostly stopped hand-typing patches by
**N = 30**, and by **N = 55** it has stopped entirely — 0 of 4, matching N=80's 0 of 3.

**The domain of the 449-chars-per-owner emission law is therefore N ≲ 21–23, not N ≲ 40.**
It is not a mid-size-fan-out law. It is a law about the size fan-out happened to be measured
at first.

---

## 2. The per-arm table

| N | arm | chars | chars/owner | patch calls | strategy | gate | wall s | returns | non-test | load start→end |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 21 | E-REG k1 N-1 | 8,594 | 409.2 | 1 | **literal-patch** | green | 364 | — | — | 10.38 → 13.90 |
| 21 | E-REG k1 N-2 | 1,929 | 91.9 | 1 | programmatic-generation | green | 88 | — | — | 6.00 → 6.86 |
| 21 | E-AFFORD Nd-1 | 8,977 | 427.5 | 1 | **literal-patch** | green | 125 | — | — | 5.63 → 8.23 |
| 21 | E-AFFORD Nd-2 | 1,869 | 89.0 | 1 | programmatic-generation | green | 87 | — | — | 6.46 → 5.02 |
| 21 | E-AFFORD Nd-3 | 10,090 | 480.5 | 1 | **literal-patch** | green | 144 | — | — | 9.87 → 9.03 |
| 21 | E-AFFORD Ns-1 | 2,822 | 134.4 | 1 | programmatic-generation | green | 120 | — | — | 7.81 → 6.54 |
| 21 | E-AFFORD Ns-2 | 10,090 | 480.5 | 1 | **literal-patch** | green | 133 | — | — | 5.02 → 6.17 |
| 21 | E-AFFORD Ns-3 | 3,604 | 171.6 | 2 | programmatic-generation | green | 96 | — | — | 8.17 → 7.97 |
| **30** | E-NSWEEP N30-1 | 3,552 | 118.4 | 2 | programmatic-generation | green | 113 | 4 | 8 | 5.18 → 5.52 |
| **30** | E-NSWEEP N30-2 | 3,515 | 117.2 | 3 | programmatic-generation | green | 173 | 4 | 9 | 5.78 → 7.09 |
| **30** | E-NSWEEP N30-3 | 1,937 | 64.6 | 2 | programmatic-generation | green | 173† | 4 | 8 | 14.59 → 9.66 |
| **30** | E-NSWEEP N30-4 | **12,764** | **425.5** | 1 | **literal-patch** | green | 210† | 4 | 4 | 9.35 → 9.92 |
| **40** | E-NSWEEP N40-1 | 1,415 | 35.4 | 1 | programmatic-generation | green | 87 | 5 | 5 | 5.06 → 4.82 |
| **40** | E-NSWEEP N40-2 | 2,426 | 60.6 | 2 | **literal-patch**\* | green | 191† | 5 | 6 | 7.09 → 8.25 |
| **40** | E-NSWEEP N40-3 | 2,073 | 51.8 | 2 | programmatic-generation | green | 223† | 5 | 6 | 9.66 → 6.42 |
| **40** | E-NSWEEP N40-4 | 2,966 | 74.2 | 2 | **literal-patch**\* | green | 274† | 5 | 9 | 9.92 → 5.06 |
| **55** | E-NSWEEP N55-1 | 861 | 15.7 | 1 | programmatic-generation | green | 198 | 3 | 7 | 4.82 → 5.78 |
| **55** | E-NSWEEP N55-2 | 765 | 13.9 | 1 | programmatic-generation | green | 135† | 5 | 5 | 8.25 → 14.59 |
| **55** | E-NSWEEP N55-3 | 1,377 | 25.0 | 2 | programmatic-generation | green | 236† | 5 | 7 | 6.42 → 9.35 |
| **55** | E-NSWEEP N55-4 | 1,423 | 25.9 | 2 | programmatic-generation | green | 164† | 6 | 8 | 5.06 → 9.23 |
| 80 | E-CEILING80 N-1 | 841 | 10.5 | 1 | programmatic-generation | green | 93 | — | — | 5.90 → 6.07 |
| 80 | E-CEILING80 N-2 | 722 | 9.0 | 1 | programmatic-generation | green | 61† | — | — | 6.45 → 10.08 |
| 80 | E-CEILING80 N-3 | 12,576 | 157.2 | 3 | programmatic-generation | green | 270† | — | — | 12.49 → 7.23 |

`†` = wall starred (1-minute load above 8 at either end). Wall for E-NSWEEP rows is the
**slot elapsed including lock wait**; the driver's own wall was 80–152 s in every one of the
twelve. `\*` = the two arms the frozen classifier labels literal and a post-hoc corrected rule
does not — see §4, which is the most important section in this document.

**12 of 12 E-NSWEEP arms passed the gate**: `rescore-FAN.sh <worktree> <N>` **6/6** against
`canonical-<N>`, **and** `diff -r worktree/src canonical-<N>/src` byte-identical, and churn
exactly `+120/−120`, `+160/−160`, `+220/−220` — the canonical churn at each N, to the line.
**No arm was excluded.**

---

## 3. The logistic fit and N\*

Pre-registered: an unweighted binary logistic regression of the per-arm literal indicator on
N, one observation per arm, over all 23 arms, by Newton–Raphson to |Δβ| < 1e-10.

    logit P(literal) = 1.523136 − 0.068525 · N

converged; **P(literal) = 0.5 at N = 22.23**, so **N\* = 23**.

| N | fitted P(literal) | raw fraction |
|---|---|---|
| 21 | 0.521 | 4/8 |
| 30 | 0.370 | 1/4 |
| 40 | 0.228 | 2/4 |
| 55 | 0.096 | 0/4 |
| 80 | 0.019 | 0/3 |

**The fit is an interpolation of the raw fractions, not a replacement for them, and it is
doing real smoothing work here**: the raw sequence 0.50, 0.25, 0.50, 0.00, 0.00 is not
monotone, and with n=4 per cell it could not be expected to be — a binomial proportion from
four trials has a 95% interval about ±0.45 wide, so 1/4 and 2/4 are not distinguishable.
**The fit's job is to say which way the whole 23-arm set leans, and it leans steeply
downward.** The N=40 bump is discussed next, because it turns out not to be a bump at all.

---

## 4. The classifier misfired at N=40 — reported in full, with the fix and both readings

**The frozen classifier's `literal-patch` rule is "the body carries ≥ 2 literal
`*** Update File:` markers".** Both N=40 arms it labels literal carry **exactly 2** markers.
Both are shell `for`-loops. Here is N40-2's labelled call, verbatim and abridged only in the
middle of its file lists:

```
const r = await tools.exec_command({"cmd":"{
  printf '%s\n' '*** Begin Patch'
  for f in src/acid/fanout/ns_003.clj src/acid/fanout/ns_005.clj … ns_088.clj; do
    printf '%s\n' "*** Update File: $f" '@@' \
      '-  (:require [acid.fanout.store2 :as store]' \
      '+  (:require [acid.fanout.store2 :as store2]'
  done
  for f in src/acid/fanout/ns_007.clj … ns_098.clj; do
    printf '%s\n' "*** Update File: $f" '@@' \
      '-            [acid.fanout.store2 :as store]' \
      '+            [acid.fanout.store2 :as store2]'
  done
  printf '%s\n' '*** End Patch'
} | apply_patch", …});
```

That is a **generator**, and it earns two markers only because the literal string
`"*** Update File: $f"` appears once inside each of two loops — one loop for the two
indentation shapes the `ns` forms come in. The classifier counted the *template*, not the
*owners*.

The tell was already in the numbers: the two arms emit 2,426 and 2,966 characters for 40
owners — **60.6 and 74.2 chars/owner**, against the literal stratum's measured 449.4 at N=21
and 425.5 at N=30. A hand-typed 40-owner patch in this family costs roughly 18,000
characters. These cost a seventh of that.

### Both readings, side by side

**Rule v1 (PRE-REGISTERED, frozen, PRIMARY):** ≥ 2 literal markers → literal-patch.
**Rule v2 (POST-HOC, clearly labelled, NOT substituted for v1):** also require markers ≥ N/2 —
the body must actually spell out most of the owners.

| N | v1 literal fraction | v2 literal fraction |
|---|---|---|
| 21 | 4/8 = 0.500 | 4/8 = 0.500 |
| 30 | 1/4 = 0.250 | 1/4 = 0.250 |
| **40** | **2/4 = 0.500** | **0/4 = 0.000** |
| 55 | 0/4 = 0.000 | 0/4 = 0.000 |
| 80 | 0/3 = 0.000 | 0/3 = 0.000 |
| **fit** | logit = 1.5231 − 0.06853·N, P=0.5 at **N=22.23**, **N\* = 23** | logit = 3.9288 − 0.18307·N, P=0.5 at **N=21.46**, **N\* = 22** |

**Exactly 2 of 23 arms reclassify, both at N=40, and the conclusion does not move: N\* goes
from 23 to 22.** Every other arm in the entire 23-arm set is unambiguous — its literal marker
count is either **0** or **exactly N**. There is no third regime and no borderline case: when
this caller types a patch it types every owner, and when it generates one it types the
template once. `receipts/classifier-sensitivity.txt` carries the per-arm reclassification.

**Under v2 the raw sequence is monotone: 0.50, 0.25, 0.00, 0.00, 0.00.** The N=40 bump was an
artefact of the meter, not of the caller. The primary result stated in §1 is the v1 result,
because v1 is what was frozen; the honest summary of both is **N\* ≈ 22–23**.

### The ratchet

`strategy.py`'s literal test must be **owner-relative, not absolute**: a body is a literal
patch when its literal `*** Update File:` count is a majority of the owners it must change,
and a body whose marker count is small while its patch covers many owners is generation
however many markers it has. The next cohort to touch this classifier should also add the
negative witness this cohort supplies for free — **N40-2's exact call text, asserted to
classify as programmatic-generation** — because a classifier that cannot fail on a case it
has already been wrong about is not a ratchet.

---

## 5. The five pre-registered predictions, scored

| # | prediction | p | verdict | measured |
|---|---|---|---|---|
| **P1** | literal fraction at N=30 **≥ 1/2** | 0.55 | **MISS** | **1/4 = 0.250** |
| **P2** | literal fraction at N=40 **≤ 1/2** | 0.82 | **HIT** | 2/4 = 0.500 (v2: 0/4) |
| **P3** | literal fraction at N=55 **≤ 1/4** | 0.70 | **HIT** | **0/4 = 0.000** |
| **P4** | **N\* ∈ [30, 45]** | 0.35 | **MISS** | **N\* = 23** (v2: 22) |
| **P5** | **no stream-edit** in the 12 arms | 0.92 | **HIT** | **0 of 12** |

3 hits, 2 misses. **Both misses are the same miss, and it is the finding**: the pre-registration
still half-believed E-CEILING80's N ≲ 40 restriction, so P1 expected N=30 to look like N=21 and
P4 expected the crossing somewhere in the thirties or low forties. Both were wrong in the same
direction — **the caller had already changed behaviour by N=30**, and the crossing sits barely
above the size every previous cohort in this family had run at.

P4 was written at p = 0.35, the lowest probability in the set, with the reason stated in the
pre-registration: *"the N=21 row is already exactly 4/8 = 0.50, which is on the ≤ 1/2 line, so
a fit that reproduces it can put N\* at or below 21 before the new cells get a vote."* That is
exactly what happened. **The pre-registration knew where its own prediction was weakest and
said so before the data arrived**, which is the only thing that makes a 0.35 worth writing down.

---

## 6. The pre-registered decisions, applied

**D1 — "if the literal fraction at N=30 is already ≤ 1/4, square 2's domain is restricted to
N ≤ 21 in this document."**
Measured: **1/4 = 0.250. TRIGGERED.**

> **Restriction, applied here as pre-registered: square 2's product claim — the one priced on
> "native emits ~450 characters per owner while the tool emits a constant few hundred" — is
> RESTRICTED TO N ≤ 21 and must not be quoted above it.** At N=30 three of four arms already
> generate, and the one that types is the last of its kind in this dataset: **no arm at N ≥ 40
> hand-types a patch under either classifier rule.** E-CEILING80's "N ≲ 40" is superseded by
> this document, which measured the interval it was guessing across.

**D2 — "if N=55 still shows literal ≥ 1/2, the restriction is LIFTED to N ≲ 60 and
E-CEILING80's W1 is re-read as the beginning of the slope."**
Measured: **0/4 = 0.000. NOT TRIGGERED.** The restriction is not lifted; it is tightened, by D1.

**D3 — "any arm failing the gate is reported and excluded, with its reason."**
**No arm failed.** 12 of 12 green, byte-identical, exact churn. Nothing was excluded.

---

## 7. Secondary meters — reported, never claimed

| N | n | mean chars | median chars | mean chars/owner | mean patch calls |
|---|---|---|---|---|---|
| 21 | 8 | 5,997 | 6,099 | 285.6 | 1.12 |
| 30 | 4 | 5,442 | 3,534 | 181.4 | 2.00 |
| 40 | 4 | 2,220 | 2,250 | 55.5 | 1.75 |
| 55 | 4 | 1,106 | 1,119 | 20.1 | 1.50 |
| 80 | 3 | 4,713 | 841 | 58.9 | 1.67 |

**Chars per owner falls monotonically from 285.6 at N=21 to 20.1 at N=55** — the 1/N signature
of an O(1) generated patch — and then rises at N=80 only because E-CEILING80's N-3 spent 12,576
characters on three attempts at a *program*, not on a patch. The mean at N=30 is likewise
carried by one arm: the median is 3,534 and the single literal arm is 12,764.

### Within the literal-patch stratum only (the §26 rule: pooling two modes describes neither)

| N | v1 n literal | mean chars | chars/owner | v2 n literal | chars/owner |
|---|---|---|---|---|---|
| 21 | 4 | 9,438 | **449.4** | 4 | **449.4** |
| 30 | 1 | 12,764 | **425.5** | 1 | **425.5** |
| 40 | 2 | 2,696 | 67.4 | **0 — NOT COMPUTABLE** | — |
| 55 | **0 — NOT COMPUTABLE** | — | — | 0 | — |
| 80 | **0 — NOT COMPUTABLE** | — | — | 0 | — |

**The per-owner constant is real, and it is flat where it exists: 449.4 at N=21 and 425.5 at
N=30, a 5% difference across a 43% increase in owners.** It is not a law that decays. It is a
law with a **domain**, and this table is the domain: it holds while the caller is typing, the
caller stops typing above ~N=30, and above that the constant has no stratum to be measured in.
The v1 figure of 67.4 at N=40 is the misfire of §4 and should not be read as the constant
bending — it is two generators in the wrong column.

**Not one stream edit — 0 of 12 here, and 0 of 34 arms now on record** across E-REG, E-AFFORD
(three of whose arms were handed `sed` and `perl` by name), E-CEILING80 and this cohort. Every
one of the twelve arms here routed its edit through `apply_patch`, including the ones that
built the patch body with a shell loop. **E-AFFORD's finding 3 now holds across a 3.8× range of
fan-out: `apply_patch` is not the affordance this caller reaches for, it is the affordance it
routes everything through.**

Re-emissions and first-call refusals: 7 of 12 arms emitted a second write after a failed one
(N30-1, N30-2 ×2, N30-3, N40-3, N55-3, N55-4). Reported, not claimed.

---

## 8. Validations run before arm 1, each with its receipt

A failure in any of these stopped the cohort. None failed.

| # | validation | receipt | result |
|---|---|---|---|
| 1 | three fixtures load and `bin/fan-test` green at base count | `receipts/fixture-load.txt` | `tests=30 assertions=210` / `tests=40 assertions=280` / `tests=55 assertions=385`, **0 failures, 0 errors** in all three |
| 2 | canonicals derived, and the gate itself green at each N | `receipts/gate-selftest.txt` | `rescore-FAN` **6/6** at all three N on canonical-applied worktrees; churn exactly 120/120, 160/160, 220/220; byte-identical; tree digests frozen |
| 3 | classifier reproduces the validation-set labels | `receipts/scorer-revalidation.txt` | **16 of 16 exact** — E-CEILING80's 3 generator arms, E-AFFORD's 3 literal arms, all 9 E-AFFORD labels, E-REG's k=1 pair, and E-CEILING80's 2 tool arms |
| 4 | `payload.py` reproduces E-AFFORD's nine published char counts | same | **all nine exact** (8977, 1869, 10090, 2822, 10090, 3604, 1214, 549, 1136) — and E-CEILING80's five (841, 722, 12576, 1094, 1508) |
| 5 | `secondaries.py` reproduces a published label | `receipts/secondaries-validation.txt` | reproduces *"both T arms refused the first call, `unknown-verification-profile`"* and N-3's 2 re-emissions |
| 6 | write fence proven, not asserted | `receipts/scorer-revalidation.txt` | 50,420 files in the three input roots, mtime+size digest identical before and after; **0** files created |
| 7 | prompts' shared text identical across all five | §9 and `FROZEN.sha256` | sha256 `853d9337…a09d25`, all five files exactly 2,784 bytes |
| 8 | pre-registration frozen with nothing on disk | `receipts/prereg.sha256` | **0** arm directories, **0** `rollout.jsonl` at freeze, 01:42:14Z |

### The gate's speed was checked, not assumed

Twelve gates returned in about nine seconds, which is the shape of a receipt that never ran.
**It was re-run rather than believed** (`receipts/gate-execution-proof.txt`): one recorded gate
re-executed from scratch in **0.306 s real**, producing output byte-identical to its recorded
log apart from the spelling of the worktree path. `rescore-FAN.sh` is a **babashka** script —
its load check and its test run are both `bb`, which starts in ~20 ms — so nine seconds for
twelve is consistent. *(`source-text-is-not-execution`: the thing to verify is a record of
execution, and the cheapest way to get one here was to execute it again and diff.)*

---

## 9. The apparatus

- **Fixtures.** `bb bench/fanout/gen-fanout.clj --n <N> --seed 7 --k 1` on `clj-surgeon-fanout`
  `bridge/fanout-fixtures-in-git` at **b62a501** — E-CEILING80's script, branch and commit,
  with only `--n` changed. Measured shape at every N: old-alias histogram `{"store" N}`,
  new-alias histogram `{"store2" N}`, **0 collisions**. Base shas
  `c227a811…` (30), `5868fe9d…` (40), `d3b7dd4b…` (55); canonical tree digests
  `4063a962…`, `437d25ca…`, `d4a782ba…`, 114 files each, all frozen before arm 1. The
  generator's target set is nested — the N=30 targets are a subset of N=40's are a subset of
  N=55's — so this is one task growing, not three unrelated tasks.
- **Canonicals DERIVED, never hand-written**: the generator's own post-state rendered with
  `post?=true`, exactly as E-REG derived `canonical-21` and E-CEILING80 `canonical-80`.
- **Prompts, proven byte-exact rather than asserted.** Each `ENSWEEP-N<N>.md` is
  E-AFFORD's **N-weak** prompt `EAFFORD-Nd.md` with **exactly one hunk** changed — the two
  owner-count lines — and **±0 bytes**: all five files (E-AFFORD's, E-CEILING80's and these
  three) are exactly 2,784 bytes, and the file with those two lines deleted has the same
  sha256 `853d93379d3a39aebd85a80c49678aaf42b29de6866a6b351274fdcd4ba09d25` in all five.
  §5 — the TOOLING section carrying the buried licence — is verbatim in all five. Each arm's
  attestation records the served prompt's sha256, and each matches `FROZEN.sha256`.
- **Scorers.** E-CEILING80's copies of E-AFFORD's `payload.py`, `strategy.py` and
  `secondaries.py`, changed in **exactly two lines each** (the write-fence root), so
  E-AFFORD's deviation-1 ratchet travels with them: a scorer refuses to write outside
  `/home/forge/tmp/arms/ensweep`.
- **`table.py`** is a **renderer and prediction scorer, not a meter**; every number it prints
  is computed by the three frozen instruments from `rollout.jsonl`.
- **No Surgeon server was started by this cohort at all**, and none was needed —
  native arms only. `COHORT_PORTS="7981 7982 7983"`, zero listeners on them at the end.
  **7888, 7894, 7895, 7906–7910 and 7941–7977 were never named and never contacted**; all
  twelve attestations record *"no mcp url configured; cohort ports (7981 7982 7983) show no
  listener"*, and all twelve score files record `via_verb=0`.
- **Serialisation.** Every arm ran under `flock /home/forge/tmp/arms/arm.lock`, sharing the box
  with E-HARNESS-2; the interleave 30→40→55 repeated four times means load drift is spread
  across the three cells rather than confounded with one. Gates ran post-hoc under
  `flock /home/forge/tmp/suite.lock`.
- **Timings.** Arm 1 at 01:45:09Z, arm 12 finished 02:21:37Z, gate 02:21:47Z.

---

## 10. Deviations, all of them

1. **No tool cell** — declared in the pre-registration with its evidence and its consequence:
   no tool-versus-native claim is made anywhere here.
2. **n = 4 per N.** ±0.45 at 95% on a four-trial proportion. The per-N fractions are coarse by
   construction and the 23-arm fit is what carries the trend. This is the resolution twelve
   arm slots buy.
3. **The frozen classifier mislabels two arms** (§4). Reported in full, with the verbatim call
   text, both readings, and the ratchet. **The frozen label remains primary**; the corrected
   rule is presented as a clearly-labelled post-hoc sensitivity and never substituted.
4. **`table.py` was rewritten after it was first written and before arm 1**, because its first
   version read `strategy.json` and `payload.json` off disk — and E-REG's arm directories have
   no `strategy.json` at all (the write fence correctly refuses to create one) while E-REG's
   `payload.json` predates the shared `classify()` and uses a different write-kind vocabulary.
   The bug produced `None` strategies and 0 patch calls for two pooled arms and would have made
   the N=21 row read 3/6 instead of the published 4/8. It was found by running the renderer on
   the pooled arms **before arm 1**, fixed to compute every meter in memory from
   `rollout.jsonl`, and the fixed file is the one in `FROZEN.sha256` — frozen at 01:45:02Z with
   **0 arm directories and 0 rollouts on disk**, seven seconds before arm 1 launched.
5. **The N=21 row pools two licence variants** (5 weak, 3 salient). Reported split as well as
   pooled: weak 3/5 = 0.600, salient 1/3 = 0.333. E-AFFORD's own finding — salience moves the
   cell mean by less than arms inside a cell differ — is the justification, and a reader who
   rejects it can undo the pooling from the table.
6. **Eight of twelve walls are starred** (load reached 14.59 with E-HARNESS-2 on the box). Load
   recorded at both ends of every arm. **No wall is claimed.**
7. **The fixture repos were `git init`-ed by this runner** (the generator emits a tree, not a
   repo, and `run-arm.sh` clones from a repo), authored `forge-anvil <forge-anvil@anvil>`.
8. **The end-of-cohort integrity digest over the input roots CHANGED, and it was chased down
   rather than reported as a pass or quietly dropped** (`receipts/final-integrity.txt`).
   Thirteen files differed, **all of them `.git/index` inside other cohorts' arm worktrees**,
   all written inside 150 ms at 02:11:05Z while E-HARNESS-2 was live on the box. **Zero**
   experiment-data files changed — computed over `*.json *.jsonl *.md *.txt *.log *.sha256
   *.edn *.out`, not asserted — and the worktrees' HEADs and dirty counts are unchanged.
   E-NSWEEP's scorers write only `*.json`, only under their own root, and never invoke git.
   **Ratchet: the fence digest must exclude `.git/` and hash evidence-file content**, so it
   reports the violation it exists for instead of ambient noise from a cohort running beside it.
9. **The publication target changed twice mid-cohort** — from `main` (the brief) to a seat
   branch to `MCP/main` — on Gene's standing instruction that nothing is published on `main`
   until there is a clear, tested, dogfooded winner. Recorded because the brief says `main` and
   this document is not on it.

---

## 11. One line of learning

**A pre-registered restriction that honours a triggered withdrawal condition is a promissory
note, not a measurement — and the interval it spans is exactly where the interesting thing
lives.** E-CEILING80 did the right thing: two points, 21 and 80, a broken law, and an honest
"bounded to N ≲ 40" written before anyone looked. Three points inside that interval cost twelve
arm-runs and thirty-six minutes, and moved the boundary from 40 to **22** — a factor of nearly
two, in the direction that shrinks the claim. **The restriction was not conservative; it was a
guess wearing a restriction's clothes, and only a sweep could tell the difference.** The
general form: when a document says "bounded to X" and the evidence is two points either side of
X, the number X was never measured, and the cost of measuring it is usually one cohort.

---

## 12. One caveat

**Twelve arms cannot resolve a probability curve, and the two numbers this document leans on
hardest are the two it can least afford to be wrong about.** N\* = 23 comes from a two-parameter
fit through five proportions, three of which are four-trial estimates whose 95% intervals are
nearly half the range they live in; the raw sequence is not even monotone under the frozen
classifier. And the N=21 anchor that most constrains the crossing is **pooled and reused**, from
two cohorts, under two licence variants, none of them run today on today's box. The honest
statement is a range — **the crossing is somewhere near the low-to-mid twenties, and it is
certainly below 40** — not a point estimate to two significant figures. Everything here is one
caller (`gpt-5.6-sol`, high reasoning effort), one harness with `apply_patch` as the offered
write verb, one task family, one k, one seed, and generated fixtures; a different caller may
have a different N\*, and a caller offered a different write verb may not have one at all.
The experiment this one implies is **narrower and deeper, not wider**: N ∈ {21, 25, 30} at
n ≥ 8, with the corrected owner-relative classifier and its negative witness in place, to find
out whether the crossing is a threshold the caller steps over or a slope it drifts down.
