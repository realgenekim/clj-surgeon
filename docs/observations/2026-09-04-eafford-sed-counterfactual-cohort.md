# E-AFFORD — the salience gradient: naming every write path did not move the payload, strategy is bimodal *within* cell, and nothing streamed

*forge@anvil, 2026-09-04T01:08:49Z. Cohort E-AFFORD = Opus's **#1** in
`docs/observations/2026-09-03-brainfleet-hills.md` §21, selected in §23 as "the one
experiment that can still take the headline away", to be run "before either of us quotes
the number". Nine arm-runs, serial, interleaved. Pre-registration frozen before arm 1:
`/home/forge/tmp/arms/eafford/preregistration.md`, sha256
`be2ad7ceacefd165acfe520b00087cb77ab127443cee418a2a120da2737a4818`, receipt
`receipts/prereg.sha256` (v1 `5a53df34…`, superseded by Amendment 1 with zero arm
directories on disk). Per-arm receipts under `/home/forge/tmp/arms/eafford/eafford-*/`;
table `receipts/EAFFORD-table.md`. Fixtures and comparator read read-only from the E-REG
root, which was not modified.*

---

## 1. Pre-registration — verbatim, exactly as frozen before arm 1

*Frozen sha256 `be2ad7ceacefd165acfe520b00087cb77ab127443cee418a2a120da2737a4818` (receipt `/home/forge/tmp/arms/eafford/receipts/prereg.sha256`, written with zero arm directories on disk). Reproduced here byte-for-byte, including its own heading levels; nothing below it was edited after the first arm launched. **The measured results begin at section 2.***

# E-AFFORD — pre-registration (written and frozen BEFORE arm 1)

*forge@anvil. v1 frozen 2026-09-04T00:44:51Z, sha256
`5a53df34b95d7047e377a2fe97b97c0a02797db19a9a227bad487c7797dd689f`, receipt
`receipts/prereg-v1-superseded.sha256`. **Amendment 1 applied 2026-09-04T00:47:04Z, still before arm 1**
(no arm directory existed; the receipts directory is the witness) — this file is v2 and
supersedes v1. Runner root `/home/forge/tmp/arms/eafford`. Load at re-freeze: 5.63.
Cohort E-AFFORD = Opus's **#1** in `docs/observations/2026-09-03-brainfleet-hills.md` §21,
selected in §23 as the one experiment that can take tonight's E-REG headline away.*

## 1. The question

E-REG measured that 8 of 8 unprompted native runs hand-transcribed a full `apply_patch`
body — 7,377 to 16,531 characters — including the perfectly uniform k=1 cell that a single
`sed` would close, while the verb committed a byte-identical result in 485–1,260 characters.
Opus's own standing objection to that headline (§21 clause 1, "the load-bearing gap"):

> **No native arm was ever *offered* a scripted rewrite. Until E-AFFORD runs, "native does
> not use the regex even when it could" means "even when nobody mentioned it," and that is
> one prompt line away from collapsing.**

E-AFFORD is that one prompt line.

## 2. Amendment 1 (2026-09-04T00:47:04Z, before arm 1) — what changed and why

From the Sol cross-attack, accepted by the coordinator. Three changes, recorded here in
advance of any arm because a design changed after data is not a design:

1. **The licensing sentence was replaced.** v1's sentence (*"You may perform the rewrite
   with sed, perl, or a short script instead of a hand-typed patch; any correct method is
   acceptable"*) **names a preference** — "instead of a hand-typed patch" is a nudge, and a
   nudge measures compliance, not affordance. The replacement names `apply_patch` as an
   equal option and prefers nothing. The cell is renamed **N-salient** and is **never pooled**
   with N-default.
2. **Cell C (k=6, sed-licensed) is DROPPED and deferred.** In its place, a
   **contemporaneous tool cell T at k=1, n=3**. E-REG's T rows are historical context, not
   the comparator for a new ratio: a ratio whose numerator was measured tonight and whose
   denominator was measured on a different box-load hours earlier is not a measurement.
3. **The three cells are INTERLEAVED**, not run in blocks, so load drift is shared across
   cells rather than confounded with one of them.

## 3. The design (three cells, n=3, 9 arm-runs, serial, interleaved)

| cell | fixture | prompt | arm | n | what it is |
|---|---|---|---|---|---|
| **N-default** (`Nd`) | `fanout-k1` | `EAFFORD-Nd.md` | native | 3 | replication of E-REG's k=1 native cell |
| **N-salient** (`Ns`) | `fanout-k1` | `EAFFORD-Ns.md` | native | 3 | the counterfactual: every write path named, none preferred |
| **T** | `fanout-k1` | `EAFFORD-T.md` | tool | 3 | contemporaneous comparator, port 7947 |

Run order, fixed now: **Nd-1, Ns-1, T-1, Nd-2, Ns-2, T-2, Nd-3, Ns-3, T-3**, one at a time,
each under `flock /home/forge/tmp/arms/arm.lock`.

## 4. The intervention — the ONE added sentence, verbatim

Appended to §5 of the native prompt (`TOOLING`) and to nothing else:

> **Clarification: as in every native arm, you may use any available native write path, including apply_patch, sed, perl, or a generated patch. Choose whichever method you judge fastest and safest; none is required or preferred. Do not use clj-surgeon.**

Proven mechanically, not asserted:

| prompt | sha256 | provenance |
|---|---|---|
| `EAFFORD-Nd.md` | `9ab5267a77a2a02bb5bf4e4833d2bcbcb5055550f2830bdff092f316baa638f6` | **byte-identical** to the pinned `clj-surgeon-arms/bench/anvil-arms/prompts/E3-P-N.md` and to that file's committed `.sha256` at HEAD `89295d8` |
| `EAFFORD-Ns.md` | `bdb56497044870b58058197c612fe27c0c4890ef66f5471feb7224fee8d5423a` | Nd + that one line: unified diff is exactly `@@ -54,0 +55 @@`, **+249 bytes, no other hunk** |
| `EAFFORD-T.md` | `6062621cb9600df4b20f2c8763051c151cda1df65832fcd523f7098e4d4c6f08` | **byte-identical** to the pinned `E3-P-T.md` and its committed `.sha256` — E-REG's T prompt verbatim |

Everything else is held: same fixture trees (E-REG's own `fanout-k1`, read **read-only**,
never modified), same task text, same driver (`sol-yolo`, `gpt-5.6-sol`, reasoning effort
high), same churn band `67,101,67,101`, same gate.

**A material fact about N-default, declared here because it weakens the contrast and a
reader must not discover it in the results.** §5 of the unmodified prompt already ends
*"Use whatever route you judge fastest and safe, including a scripted edit if you believe
it is correct for this tree."* N-default is therefore **not** an unlicensed control; it is a
**soft, unnamed** license. N-salient makes the same permission **explicit and enumerated**.
If N-salient does not move, the honest reading is that *enumerating* the write paths did not
help an agent already told scripting was allowed — a weaker refutation of F2 than "native
ignores an offer it never had," and the report must say so.

## 5. The server for the T cell

This cohort's **own clone** of E-REG's build — `33a8236` ("merge origin/main 4be8566 into
q5z 0085db8"), the exact build E-REG's eight T arms ran — at
`/home/forge/tmp/arms/eafford/server-src`, so no JVM ever runs inside E-REG's checkout.
Started per T arm by `run-arm.sh` on an **explicit** `:port 7947`, `:telemetry :full` into
that arm's own directory, and stopped at the end of that arm by the pid this apparatus
recorded that it forked. `COHORT_PORTS=7947`. **7888, 7894, 7895 and 7906–7910 are never
named by this cohort and never contacted.** The server sha is read back from the running
process's `/proc/<pid>/cwd` on every T arm and must equal `33a8236`, or the arm is refused
before the driver launches. Native arms are refused if 7947 is listening at all.

## 6. Primary meter (load-immune) — two counts, one definition of "a write"

1. **Emitted write-payload characters per arm**, `payload.py`: the summed
   `custom_tool_call.input` length of every call that writes.
2. **A three-way strategy classifier per arm**, `strategy.py`, keyed on the emitted
   **REQUEST CONTENT**, never on call syntax (E-REG's apparatus lesson: three spellings of
   one call appeared across sixteen arms):
   - **literal-patch** — a patch body whose literal `*** Update File:` count is >= 2, i.e.
     the per-owner patch text is spelled out;
   - **programmatic-generation** — a patch body with <= 1 literal `*** Update File:` plus a
     generation construct (a file table + loop/map/template), or a read/replace/write script;
   - **stream-edit** — `sed`/`perl`/`awk` rewriting files in place or by redirect, with no
     literal patch body;
   - **tool-call** — an `alias_migration` request (the T cell; never mixed into a native mean).
   An arm's class is the class holding the **majority of emitted write characters**; a split
   with no majority prints `mixed(...)` with the char split, never a guess.

**Validation, run before arm 1 (a classifier that cannot reproduce the labels it audits is
not a meter).** Both scorers were run read-only over all 16 E-REG arms and reproduce
**every published character count, gap and rate exactly** (native 8,594 / 1,929 / 9,724 /
7,377 / 8,202 / 8,179 / 16,531 / 9,636; tool 1,013 / 485 / 1,019 / 1,260 / 1,067 / 912 /
1,075 / 580) **and every label E-REG's doc asserts**: 7 literal-patch, 1
programmatic-generation (`ereg-k1-N-2`, the compact JS table), **0 stream-edit**, 8
tool-call. The `stream-edit` branch — which no arm in the corpus has ever exercised, and
which is precisely the branch this cohort exists to catch — was hand-driven separately on
three spellings (`sed -i` in a shell loop, `perl -pi -e`, `awk -i inplace`) plus three
negatives (`sed -n` probe, `rg`, `bin/fan-test`): **8/8**.

**Secondary, reported and never claimed:** wall, chars/s, emission gap, returns, non-test
actions. `cut -d' ' -f1 /proc/loadavg` is recorded at each arm's start and end; **a wall
with either end above 8 is VOID**, starred in the table, and never used in any comparison.

## 7. Correctness gate

`rescore-FAN.sh <worktree> 21` **6/6** against `fanout-k1/canonical-21`, **plus**
`diff -r worktree/src canonical-21/src` byte-identical. An arm that fails the gate is
**reported with its reason and excluded from the payload mean**.

## 8. Pre-registered predictions

The **ratio** is defined explicitly, because §21's label and its value disagree:
**ratio = native emitted chars / tool emitted chars** (E-REG k=1: 5,262 / 749 = **7.0x**;
§21 writes this as "T/N" while quoting 7.0x, so the value governs). Under Amendment 1 the
denominator is **this cohort's own T cell**, not E-REG's.

| # | prediction | pass line |
|---|---|---|
| **P1** | N-default replicates E-REG's k=1 native cell | mean **5,000 +/- 3,000 chars** (2,000-8,000) **AND >=2 of 3 arms literal-patch** |
| **P2** | N-salient collapses | **median 1,100 chars AND >=2 of 3 arms under 2,000 chars** |
| **P3** | the k=1 ratio falls | N-salient mean / **contemporaneous T mean** lands in **1.0-1.8x** (from the N-default/T ratio measured in the same cohort, and from E-REG's 7.0x) |
| **P4** | the contemporaneous T cell reproduces E-REG's | T mean **749 +/- 300 chars** (449-1,049) |

*(v1's fourth prediction — cell C at k=6, 3,500 +/- 2,000 chars with ratio >= 3x — is
**withdrawn with the cell** under Amendment 1 and is not scored. It is deferred, not
answered.)*

## 9. Withdrawal condition (written before arm 1, both clauses)

1. **If N-salient emits >= 4,000 chars in >= 2 of 3 arms**, F2 is dead, the affordance flank
   is **closed**, and *"native does not reach for the regex even when it could"* stands as a
   behavioural law for this caller — **stop attacking it.**
2. **If the k=1 ratio falls below 1.5x**, square 2's headline is rewritten to *"irregular or
   large fan-out"* **before any report quotes the uniform cell**, and E-REG's k=1 row is
   annotated in place.

Both are evaluated and stated in the report whichever way they land.

## 10. Deviations already known at freeze time

1. **I overwrote E-REG's receipts during validation and restored them.** Running my scorers
   over E-REG's arm directories rewrote all 16 `payload.json` files (identical numbers, new
   `write_kinds` labels) and added 16 `strategy.json` files. **Restored**: the 16
   `payload.json` regenerated by E-REG's own untouched `payload.py` (labels back to
   `apply_patch`/`alias_migration`, counts unchanged), the added files deleted, verified 0
   remaining. **Ratchet applied the same hour**: both scorers now refuse to write outside
   `/home/forge/tmp/arms/eafford` — reading another cohort's root is the whole point of a
   comparator, writing into it never is. Proven: re-running both on an E-REG arm leaves its
   `payload.json` mtime unchanged and creates no file.
2. **My `payload.py` differs from E-REG's copy** in exactly one way: the char meter and the
   strategy classifier now share **one** definition of "a write" (`strategy.classify_call`).
   E-REG's copy detected `sed`/`perl` only; N-salient explicitly enumerates *"apply_patch,
   sed, perl, or a generated patch"*, and an `awk -i` or a JS read/replace/`writeFileSync`
   rewrite would have scored **zero emitted characters** — a silent zero on the primary
   meter, in exactly the cell this experiment is about. Re-validated against all 16 E-REG
   arms above.
3. **The brief said this cohort needs no Surgeon server and forbade starting one.**
   Amendment 1 supersedes that clause by adding the contemporaneous T cell, and constrains
   it: an explicit port above 7940 (7947), this cohort's own checkout, the E-REG build,
   stopped after each T arm.
4. **n = 3 per cell**, 9 arm-runs. No wall is claimed anywhere.
5. **N-default's soft pre-existing license**, section 4 above.

---

## 2. The table

| cell | run | emitted chars | chars/s | emission gap s | wall s | returns | non-test | strategy | gate | load start→end |
|---|---|---|---|---|---|---|---|---|---|---|
| N-weak | 1 | **8,977** | 141.5 | 63.426 | 125.0† | 4 | 7 | literal-patch | 6/6 + bytes | 5.63 → 8.23 |
| N-salient | 1 | **2,822** | 137.4 | 20.540 | 119.0 | 4 | 3 | programmatic-generation | 6/6 + bytes | 7.81 → 6.54 |
| T | 1 | **1,214** | 172.9 | 7.022 | 37.0 | 3 | 4 | tool-call | 6/6 + bytes | 6.42 → 6.93 |
| N-weak | 2 | **1,869** | 151.8 | 12.315 | 86.0 | 4 | 5 | programmatic-generation | 6/6 + bytes | 6.46 → 5.02 |
| N-salient | 2 | **10,090** | 141.8 | 71.161 | 133.0 | 4 | 6 | literal-patch | 6/6 + bytes | 5.02 → 6.17 |
| T | 2 | **549** | 165.2 | 3.323 | 32.0† | 3 | 3 | tool-call | 6/6 + bytes | 6.48 → 9.87 |
| N-weak | 3 | **10,090** | 142.1 | 70.991 | 143.0† | 4 | 5 | literal-patch | 6/6 + bytes | 9.87 → 9.03 |
| N-salient | 3 | **3,604** | 145.9 | 24.708 | 95.0† | 4 | 6 | programmatic-generation | 6/6 + bytes | 8.17 → 7.97 |
| T | 3 | **1,136** | 181.1 | 6.274 | 42.0 | 4 | 4 | tool-call | 6/6 + bytes | 7.97 → 7.20 |

`†` = wall VOID (1-minute load average above 8 at arm start or end). **5 of 9 walls are
valid; no wall is claimed anywhere in this document.** Rows are in RUN order, which is the
pre-registered interleave (Amendment 1): Nd-1, Ns-1, T-1, Nd-2, Ns-2, T-2, Nd-3, Ns-3, T-3.

### Cell means

| cell | n | mean chars | median chars | per-arm chars | mean chars/s | strategy classes | gate |
|---|---|---|---|---|---|---|---|
| **N-weak** (E-REG prompt verbatim) | 3 | **6,979** | 8,977 | 8,977 / 1,869 / 10,090 | 145.1 | literal-patch ×2, programmatic-generation ×1 | 3/3 green |
| **N-salient** (+ the licence sentence) | 3 | **5,505** | 3,604 | 2,822 / 10,090 / 3,604 | 141.7 | programmatic-generation ×2, literal-patch ×1 | 3/3 green |
| **T** (contemporaneous, port 7947) | 3 | **966** | 1,136 | 1,214 / 549 / 1,136 | 173.1 | tool-call ×3 | 3/3 green |

Ratios, native emitted chars ÷ tool emitted chars, **both terms measured in this cohort
within one 15-minute window**: N-weak **7.22×**, N-salient **5.70×**.

---

## 3. The four pre-registered predictions, scored

| # | prediction | pass line | verdict | measured |
|---|---|---|---|---|
| **P1** | N-weak replicates E-REG's k=1 native cell | mean 5,000 ± 3,000 (2,000–8,000) **AND** ≥2/3 literal-patch | **HIT** | mean **6,979**, inside the band; **2/3** literal-patch |
| **P2** | N-salient collapses | median 1,100 **AND** ≥2/3 arms under 2,000 | **MISS** | median **3,604** (3.3× the predicted median); **0/3** arms under 2,000 |
| **P3** | the k=1 ratio falls to 1.0–1.8× | N-salient mean ÷ contemporaneous T mean in 1.0–1.8× | **MISS** | **5.70×** — the ratio fell from 7.22× to 5.70×, a 21% move where a 4–7× move was predicted |
| **P4** | the contemporaneous T cell reproduces E-REG's | T mean 749 ± 300 (449–1,049) | **HIT** | mean **966**, inside the band; per-arm 1,214 / 549 / 1,136 vs E-REG's 1,013 / 485 |

*(v1's fourth prediction — cell C at k=6, 3,500 ± 2,000 with ratio ≥3× — was **withdrawn
with the cell** under Amendment 1 and is not scored. It is deferred, not answered.)*

## 4. The withdrawal conditions, applied

**Clause 1 — "if N-salient emits ≥ 4,000 chars in ≥ 2 of 3 arms, the salience flank is
closed."** Measured: **1 of 3** (2,822 / **10,090** / 3,604). **NOT TRIGGERED, by one arm.**

**Clause 2 — "if the k=1 ratio falls below 1.5×, rewrite square 2's headline to *irregular
or large fan-out*."** Measured **5.70×**. **NOT TRIGGERED.** Square 2's headline is **not**
rewritten and E-REG's k=1 row is **not** annotated.

**VERDICT, by the pre-registered wording: INCONCLUSIVE — and it must be reported as
inconclusive, not as a win.** The collapse P2 and P3 predicted did **not** happen; but the
cell also did not clear its own closure threshold. The cohort neither established that
salience collapses native's payload nor earned the right to say the flank is closed. That
is the same shape as E-REG's own pass line (a), and it is the honest reading.

---

## 5. What actually happened — five lines

**1. The cells barely differ; the ARMS differ enormously, and the split is inside each
cell.** N-weak 8,977 / 1,869 / 10,090. N-salient 2,822 / 10,090 / 3,604. The **difference
between the cell means is 1,473 characters. The spread inside N-weak alone is 8,221, and
inside N-salient 7,268** — the within-cell spread is roughly **five times** the between-cell
effect the experiment was built to detect. At n=3 the intervention is not merely
non-significant; it is smaller than the noise it would have to beat, and the interleaved
run order (Amendment 1) means it cannot be blamed on load drift: the two cells were run
alternately, minutes apart, on the same box.

**2. Strategy is BIMODAL and the two modes do not overlap — pooled over both cohorts, the
choice is a coin flip.** Across the **eight** k=1 native arms now on record (E-REG's 2 +
E-AFFORD's 6):

| strategy | n | emitted chars | mean |
|---|---|---|---|
| literal-patch | **4** | 8,594 / 8,977 / 10,090 / 10,090 | 9,438 |
| programmatic-generation | **4** | 1,869 / 1,929 / 2,822 / 3,604 | 2,556 |
| stream-edit | **0** | — | — |

The clusters are **disjoint** — the smallest literal patch (8,594) is larger than the
biggest generated one (3,604) — and the mean ratio between them is **3.69×**. **Strategy
explains nearly all of native's payload variance; the prompt cell explains almost none.**
The right model of this caller is not "native types 8,600 characters"; it is **"native
flips a coin between two stable strategies whose payloads differ by 3.7×, and the licence
wording does not weight the coin."**

**3. Not one of the nine arms performed a stream edit — including the three that were
handed `sed` and `perl` by name.** And this is not because the agent forgot sed exists:
**5 of the 6 native arms invoked `sed`, `perl` or `awk` at least once** — every single time
as a **reader** (`sed -n '1,12p' "$f"`, `awk 'NR==1,/^$/'`, `sed -n '1,/^)/p'`) to print
`ns` forms while surveying the tree. The one arm that used perl for the *edit*
(`eafford-Ns-N-1`, 2,822 chars) wrote a 40-line perl program that reads each file, applies
the alias policy, and **prints a patch to stdout — which it then fed to `apply_patch`.**
The agent will happily write a program to *generate* the edit; it will not let a stream
editor *make* the edit. **`apply_patch` is not the affordance native reaches for by
default — it is the affordance native routes everything through**, even when it has just
been told it need not.

**4. The tool cell reproduces, contemporaneously, and the tool's win survives the whole
intervention.** T mean **966** chars (1,214 / 549 / 1,136) against E-REG's 749 (1,013 /
485) — inside the pre-registered band, measured in the same window as the native arms it
is compared with. The ratio moved from **7.22× to 5.70×**: the most favourable native
result this program has ever produced, under the most explicit licence it has ever been
given, still emits **5.7 times** the tool's characters. **Even the generated-patch mode —
native's best strategy, 2,556 chars pooled — is 2.6× the tool.** Square 2's claim is not
in danger from this cohort; it is stronger than before, because the counterfactual was run
and it held. `unknown-verification-profile` refused the first call in **2 of 3** T arms
(E-REG: 6 of 8, mixed classes), each recovered in one round-trip — the first-call schema
tax is still there and is still a wall cost, not a character cost.

**5. E-REG's "8 of 8 hand-transcribed" is FALSE, and the correction is now measured twice.**
Amendment 2's fact, independently reproduced here by a classifier validated against E-REG's
own receipts: E-REG's native arms were **7 of 8 literal-patch and 1 of 8
programmatic-generation** (`ereg-k1-N-2`, 1,929 chars, a compact JS table of 21 filenames
looped into a patch body). **0 of 8 were sed/perl rewrites** — that half of the sentence
was right and is now confirmed at n=17 across both cohorts. Anyone quoting square 2 must
say **"7 of 8 hand-transcribed a full patch; the eighth generated one"**, not "8 of 8".

---

## 6. Amendments, both recorded before the arms they governed

**Amendment 1 — 2026-09-04T00:47:04Z, with ZERO arm directories on disk** (the receipt file
records the count). From the Sol cross-attack, accepted by the coordinator. Three changes,
all made before arm 1 and all re-frozen into pre-registration v2:

1. **The licensing sentence was replaced.** v1's sentence — *"You may perform the rewrite
   with sed, perl, or a short script instead of a hand-typed patch; any correct method is
   acceptable"* — **names a preference.** "Instead of a hand-typed patch" is a nudge, and a
   nudge measures compliance, not affordance. The replacement names `apply_patch` as an
   equal option and prefers nothing.
2. **Cell C (k=6, sed-licensed) was DROPPED and deferred**, replaced by a **contemporaneous
   T cell at k=1, n=3**. E-REG's T rows are historical context, not a comparator: a ratio
   whose numerator was measured tonight and whose denominator was measured hours earlier
   under a different load is not a measurement. This turned out to matter for the *reason*
   it was demanded and not the one anyone expected — the contemporaneous T mean (966) is
   **29% above** E-REG's (749), so the historical denominator would have overstated the
   ratio by that much in every line of this report.
3. **The three cells were INTERLEAVED**, not run in blocks, so load drift is shared across
   cells. With walls void in 4 of 9 arms and load ranging 5.02–9.87 across a 15-minute
   window, this is the clause that makes finding 1 defensible.

**Amendment 2 — arrived 2026-09-04 between 00:59:02Z and 00:59:14Z, i.e. with 7 of 9 arms
complete** (Nd-1, Ns-1, T-1, Nd-2, Ns-2, T-2, Nd-3 had run; Ns-3 and T-3 had not).
**Labels and interpretation only — no prompt byte, no prediction and no withdrawal
condition was changed**, which is why it could be applied mid-cohort at all. The fact, from
the Opus cross-attack against the frozen prompt: §5 of `E3-P-N.md`, which **every E-REG
native arm ran**, already licensed a scripted edit in its last clause —

> *"You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
> structural editing server available. Use whatever route you judge fastest and safe,
> including a scripted edit if you believe it is correct for this tree."*

So the control cell is **not unlicensed; it is WEAKLY licensed.** Relabelled
**N-default → N-weak**; N-salient keeps its name. **This cohort is therefore a SALIENCE
GRADIENT, not a counterfactual** — it varies how *prominent* the licence is, from the last
clause of §5 to a headline sentence appended to §5, and it never tested the absence of a
licence, because no such prompt has ever been run in this program. The withdrawal
condition's meaning changes with it: clause 1, had it fired, would have closed the
**salience** flank, not the affordance flank. The title and the learning line below say so.

*(The runner had independently declared the same soft-licence fact in pre-registration v1
§3 and v2 §4 as a known deviation, before any arm ran; Amendment 2 promoted it from a
caveat to the cohort's identity, which is the correct weight for it.)*

---

## 7. Correctness

**All 9 arms passed `rescore-FAN.sh <worktree> 21` 6/6 against `fanout-k1/canonical-21`,
AND were byte-identical to it** (`diff -r worktree/src canonical-21/src` clean). Every arm:
21 files changed, 0 extras, 0 missing; form-equality 21/21; 106/106 protected regions
intact; residue-and-alias 0 old-lib hits, 0 wrong-or-missing aliases, 0 shadowing; load
100/100 namespaces; `FAN-TEST tests=21 assertions=147 failures=0 errors=0`; churn +84/−84.
**No arm was excluded.** Every T arm: server sha `33a8236` read back from the running
process's `/proc/<pid>/cwd` and matching the expected sha, port 7947, healthz ok, exactly
one committing `alias_migration` call, **zero native `apply_patch` fallback**.

---

## 8. The apparatus, and what I changed

- **Fixtures and comparator: E-REG's, read-only.** `fanout-k1` (`gen-fanout.clj --n 21
  --seed 7 --k 1`, `{"store" 21}`, 0 collisions), its `canonical-21` and `manifest-21.edn`.
  Nothing under `/home/forge/tmp/arms/ereg` was modified — see deviation 1 for the one time
  it was, and how it was restored.
- **Prompts, proven byte-exact rather than asserted.** `EAFFORD-Nd.md` sha256
  `9ab5267a77a2a02bb5bf4e4833d2bcbcb5055550f2830bdff092f316baa638f6`, **identical to the
  pinned `E3-P-N.md` and to that file's committed `.sha256`** at `clj-surgeon-arms`
  `89295d8`. `EAFFORD-T.md` sha256 `6062621cb9600df4b20f2c8763051c151cda1df65832fcd523f7098e4d4c6f08`,
  identical to the pinned `E3-P-T.md`. `EAFFORD-Ns.md` sha256 `bdb56497044870b58058197c612fe27c0c4890ef66f5471feb7224fee8d5423a`
  — the unified diff Nd→Ns is **exactly `@@ -54,0 +55 @@` plus one line, +249 bytes, no
  other hunk.** The sentence, verbatim: *"Clarification: as in every native arm, you may use
  any available native write path, including apply_patch, sed, perl, or a generated patch.
  Choose whichever method you judge fastest and safest; none is required or preferred. Do
  not use clj-surgeon."*
- **Server.** This cohort's **own clone** of E-REG's build `33a8236`, at
  `/home/forge/tmp/arms/eafford/server-src`, so no JVM ran inside E-REG's checkout.
  Explicit `:port 7947`, started per T arm and stopped by the pid this apparatus recorded
  that it forked (`stop-server` printed the pid, start-ticks and boot-id each time; 7947 was
  verified free after every T arm). `COHORT_PORTS=7947`; native arms are refused if 7947 is
  listening at all. **7888, 7894, 7895 and 7906–7910 were never named by this cohort and
  never contacted.**
- **The strategy classifier (`strategy.py`), new, keyed on emitted REQUEST CONTENT.**
  literal-patch = a patch body whose literal `*** Update File:` count is ≥2; programmatic-
  generation = a patch body with ≤1 such literal plus a generation construct, or a
  read/replace/write script; stream-edit = `sed`/`perl`/`awk` rewriting in place or by
  redirect with no literal patch body; tool-call = an `alias_migration` request. An arm's
  class is the class holding the **majority of emitted write characters**; a split with no
  majority prints `mixed(...)` with the char split.
  - **Validated before arm 1 against all 16 E-REG arms**, read-only: it reproduces **every
    published character count, gap and rate exactly** and **every label E-REG's doc
    asserts** — 7 literal-patch, 1 programmatic-generation, 0 stream-edit, 8 tool-call.
  - **The `stream-edit` branch had never been exercised by any arm in the corpus** — and it
    is the branch this cohort exists to catch, so a silent miss there would have produced
    exactly the finding the experiment wanted. It was hand-driven separately on three
    spellings (`sed -i` in a shell loop, `perl -pi -e`, `awk -i inplace`) and three
    negatives (a `sed -n` read probe, `rg`, `bin/fan-test`): **8/8**. *(Hand-drive every
    mode you ship: the ladder is per mode, not per verb.)*
- **One definition of "a write", shared by both meters.** `payload.py` now delegates
  classification to `strategy.classify_call` — see deviation 2 for why, and the
  re-validation that keeps it honest.
- **Gate.** `gate-eafford.sh`, E-REG's post-hoc gate unchanged, under
  `flock /home/forge/tmp/suite.lock` so it never contends with a driver arm. Arms
  serialised with every other cohort on the box via `flock /home/forge/tmp/arms/arm.lock`.

---

## 9. Deviations, all of them

1. **I overwrote E-REG's receipts during validation, and restored them.** Running my
   scorers over E-REG's arm directories rewrote all 16 `payload.json` files (identical
   numbers, new `write_kinds` labels) and added 16 `strategy.json` files. **Restored the
   same minute**: the 16 `payload.json` regenerated by E-REG's own untouched `payload.py`
   (labels back to `apply_patch`/`alias_migration`, counts unchanged), the added files
   deleted, verified 0 remaining. **Ratchet applied before arm 1**: both scorers now refuse
   to write outside `/home/forge/tmp/arms/eafford`. Proven, not asserted — re-running both
   on an E-REG arm leaves its `payload.json` mtime unchanged and creates no file. *Reading
   another cohort's root is the whole point of a comparator; writing into it never is.*
2. **My `payload.py` differs from E-REG's copy in exactly one way**: the character meter and
   the strategy classifier share one definition of "a write". E-REG's copy detected
   `sed`/`perl` only; N-salient explicitly enumerates *"apply_patch, sed, perl, or a
   generated patch"*, and an `awk -i` or a JS read/replace/`writeFileSync` rewrite would
   have scored **zero emitted characters** — a silent zero on the primary meter, in exactly
   the cell the experiment is about. Re-validated against all 16 E-REG arms; every number
   reproduces.
3. **The brief said this cohort needs no Surgeon server and forbade starting one.**
   Amendment 1 superseded that clause by adding the contemporaneous T cell, and constrained
   it: explicit port above 7940 (7947), this cohort's own checkout, E-REG's build, stopped
   after each T arm.
4. **Cell C (k=6, licensed) was never run** — dropped by Amendment 1, deferred. The question
   *"does the licence survive collisions"* is unanswered, and the k=6 licensed cell should
   be the first thing added if this line is continued.
5. **Wall is void in 4 of 9 arms** (load reached 9.87 with other cohorts on the box). Load
   is recorded at both ends of every arm and voided walls are starred. **This is why the
   primary meter is emitted characters**; every headline above rests on the character counts
   and the strategy labels, which are load-immune. The wall figures are reported and are not
   claimed.
6. **n = 3 per cell.** Finding 1 is the reason this matters: the within-cell spread is ~5×
   the between-cell effect, so n=3 cannot resolve an intervention of this size, and this
   cohort's honest output about the intervention is a bound, not an estimate.
7. **The pre-registration was re-frozen once** (v1 → v2, Amendment 1) with zero arm
   directories on disk; both hashes and both receipts are kept, and v1 is retained as
   `receipts/prereg-v1-superseded.sha256`.

---

## 10. One line of learning

**A "counterfactual" that varies the SALIENCE of a permission the control already had is a
salience gradient, and it will measure the noise floor rather than the mechanism** — we
moved a licence from the last clause of §5 to a headline sentence, and the payload moved
1,473 characters between cell means while individual arms in the *same* cell differed by
8,221; the finding that survives is not about the prompt at all but about the caller, which
flips a coin between two disjoint strategies (literal patch ~9,400 chars, generated patch
~2,600 chars, 4/4 each across eight k=1 arms) and routes **both** of them through
`apply_patch`, never once letting a stream editor touch a file even while using `sed` five
arms out of six to *read* one.

## 11. One caveat

**The true zero rung was never run, and cannot be claimed from this cohort.** No native arm
in this program has ever seen a prompt that does *not* license a scripted edit, so
"native declines an offered regex" is measured only across the weak-to-prominent range of a
licence that is always present. Everything here is one caller (`gpt-5.6-sol`, high
reasoning effort), one harness (`apply_patch` as the offered write verb), one task family,
one N, and generated fixtures — and finding 3 suggests the *harness*, not the prompt, is the
variable worth attacking next: an arm whose only write path is `Bash`, with no `apply_patch`
at all, is the experiment this one implies.
