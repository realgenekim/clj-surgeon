# Row 5 — E4 dual scoring under Astra's receipt-silent / trust-witness classifier

Runner: forge@anvil (Opus 5 main loop). No new caller arms, no Surgeon server calls, no
mutation of any arm tree. Read-only over retained E4 and E3 evidence. Temp under
`/var/tmp/forge/e4-dual/`.

## Headline

| slot | arm | legacy ANSWERABLE | receipt-silent | trust-witness | instrument-excluded | unresolved | judge agreement |
|---|---|---|---|---|---|---|---|
| 1 | Z1 | 2 | **1** | 1 | 0 | 0 | 2/2 |
| 2 | Z2 | 3 | 0 | 2 | 1 | 0 | 3/3 |
| 3 | Z3 | 3 | 0 | 3 | 0 | 0 | 3/3 |
| 4 | Z4 | 1 | 0 | 1 | 0 | 0 | 1/1 |
| 5 | Z5 | 3 | 0 | 3 | 0 | 0 | 3/3 |
| 6 | Z6 | 3 | 0 | 3 | 0 | 0 | 3/3 |
| **E4 total** | | **15** | **1** | **13** | **1** | **0** | **15/15 = 1.000** |

Accounting identity holds per Astra's rule: legacy ANSWERABLE (15) = receipt-silent (1) +
trust-witness (13) + instrument-excluded (1) + unresolved (0).

Legacy ANSWERABLE denominators reproduce the registered vector exactly: **2, 3, 3, 1, 3, 3**
(total 15), recomputed here by running the frozen `classify.py`
(`0365535e4990275f6ec5c929f73aa63a493b1e73ee566ab01aa2dad8abc93efb`) and `answerable.py`
(`df906f118010b7613df48826fb2a638b95378c828853ba6c35700229c7ad85a6`) unchanged — both hashes
verified against Astra's registration before use.

Per-slot receipt-silent denominators for a future E5 pairing: **1, 0, 0, 0, 0, 0**.

## The gate question, as arithmetic

*Is the E4 receipt-silent count already ≤ 0.50 of E3's legacy ANSWERABLE?*

- **Cohort totals:** E4 receipt-silent = 1. E3 legacy ANSWERABLE = 24 (per slot 2, 4, 5, 4, 4, 5).
  1 ÷ 24 = **0.0417**. 0.0417 ≤ 0.50 → **yes**.
- **Slotwise, paired to E3 slots:** 1/2, 0/4, 0/5, 0/4, 0/4, 0/5 = 0.500, 0, 0, 0, 0, 0.
  Ordered: 0, 0, 0, 0, 0, 0.500. Mean of the 3rd and 4th ordered values = **0.000**.
  0.000 ≤ 0.50 → **yes**.

That is the fact. It is not a verdict, and under Astra's own registration it cannot be read
as a retrospective E4 pass: E4's legacy ANSWERABLE result (0.675 median paired ratio against
a 0.50 gate) is unchanged, and the receipt-silent meter was never the registered E4 primary.

## The consequence for E5, also as arithmetic

The registered E5 coverage statistic is the median of six slotwise ratios
E5-receipt-silent ÷ E4-receipt-silent, with a zero-to-zero pair contributing 1 and a
positive-over-zero pair contributing +∞, no pseudocount and no excluded zeros.

The E4 denominators are 1, 0, 0, 0, 0, 0. Five of the six denominators are zero, so five of
the six ratios are **≥ 1 by construction** (1 if E5 also scores zero there, +∞ if it scores
anything). Whatever the first slot does, the sorted six-vector has at least five entries ≥ 1,
so the 3rd and 4th ordered values are both ≥ 1 and the median is **≥ 1.00**.

**The registered E5 coverage gate (median ≤ 0.50) cannot be passed on this baseline.** It is
not a hard question about E5's behaviour; it is a property of the denominators. The
conjunctive clause "no zero E4 slot develops a positive E5 receipt-silent count" is likewise
a five-slot no-regression test, not a halving test.

This is exactly the stop condition Astra wrote into the consultation: *"If it produces a
mostly zero receipt-silent baseline, stop: the answer is that another halving cohort lacks
room to teach us about coverage. Do not build an E5 battery just to obtain a green renamed
gate."* Five of six arms are zero.

## Astra's prediction, scored

| prediction | stated | observed | verdict |
|---|---|---|---|
| receipt-silent per arm, median | 0 | per-arm counts 0, 0, 0, 0, 0, 1 → median 0 | **correct** |
| receipt-silent per arm, range | 0–1 | observed range 0–1 | **correct** |
| feasibility warning ("mostly zero baseline → stop") | — | 5/6 arms zero | **borne out** |

## What the split actually says

Thirteen of the fifteen nominated actions are **covered re-derivation**: the caller went to
the tree for a proposition the receipt already answered. One is the receipt-decoding helper
that reads nothing but its own receipt. **One** action in the whole cohort establishes
something the receipt did not answer, and it is Astra's disclosed positive example: Z1's
compound `rm -f .clj-surgeon.edn && git status --short && sed …`, where the executed
`:workspace_status` predates the apparatus removal and therefore cannot attest the state the
caller asked about. A temporal gap, not a missing field.

Both judges converged on the same mechanism for the thirteen: E4's four added fact families
— `:ns_edits`, `:footprint`, `:lint_delta`, `:load_status` — supply the answers, with
`:ns_edits` doing most of the work (forbidden-edge absence, destination header, require and
import edits) and `:footprint` covering "which files differ". That is the substantive product
finding here: **the added fields did close the coverage gaps; what remains is re-derivation of
covered facts and one snapshot-freshness gap that no additional self-reported field can close.**

## E3 comparison split (same rules, same protocol)

E3's Y1–Y6 transcripts are present, so the same three-way split was dual-scored on E3's 24
legacy ANSWERABLE actions.

| slot | arm | legacy ANSWERABLE | receipt-silent floor | receipt-silent ceiling | trust-witness (agreed) | instrument-excluded | disputed |
|---|---|---|---|---|---|---|---|
| 1 | Y1 | 2 | 0 | 0 | 2 | 0 | 0 |
| 2 | Y2 | 4 | 0 | 1 | 3 | 0 | 1 |
| 3 | Y3 | 5 | 3 | 4 | 1 | 0 | 1 |
| 4 | Y4 | 4 | 1 | 1 | 3 | 0 | 0 |
| 5 | Y5 | 4 | 3 | 4 | 0 | 0 | 1 |
| 6 | Y6 | 5 | 0 | 1 | 4 | 0 | 1 |
| **E3 total** | | **24** | **7** | **11** | **13** | **0** | **4** |

Judge agreement on E3: **20/24 = 0.833**. Floor = both judges said receipt-silent; ceiling =
floor plus every disputed action where either judge said receipt-silent (per the tie rule
below).

So receipt-silent fell from **7–11 of 24** in E3 to **1 of 15** in E4. All four E3 disputes
have one root cause, and it is the same mechanism: whether the *alias bound* and the
*require/import edit actually made in each caller file* is covered. E3's receipt had no
`:ns_edits`; E4's does. This is a retrospective, non-preregistered comparison across cohorts
with different receipts — it is a mechanism observation, not a treatment estimate.

## Eligibility audit (Astra's step 1)

Fourteen of the fifteen nominated E4 actions obtain repository state independently of the
receipt: shell commands run inside the arm's own working tree using `git status`, `git diff`,
`git show`, `sed -n`, `grep` and `rg`. They are eligible for a causal label.

One is not: **Z2 action 10**,
`python3 …/Z2/slice.py comment_edits promotions unrequired_qualified_refs prose_mentions workspace_status source_retired next_call`.
The retained helper's source (`/var/tmp/forge/row5-adopt-4/Z2/slice.py`, included verbatim in
that packet) opens exactly one file — `…/Z2/e4-receipt.edn` — and prints bracket-matched
slices of receipt keys. It reads no repository state. Disposition **instrument-excluded**;
it remains inside legacy ANSWERABLE. Both judges reached this independently from the helper
source in the packet. This is the false positive Astra flagged in advance; it is documented
as an exception rather than forced into either causal bucket.

Zero E3 actions were instrument-excluded; no E3 arm wrote a receipt-slicing helper.

## Audit of substantive repository reads hidden outside the sampling frame

Gates nothing; published because Astra requires it. Actions bucketed OTHER or RECEIPT by the
frozen classifier that nevertheless touched the arm's own tree:

- **E4: 6** — one per arm, every one a `git status` fused into the same shell command as the
  apparatus removal and/or a `dogfood-stamp.sh` call, which is why the frozen classifier put
  them in OTHER. This confirms Astra's correction: all six retained E4 transcripts contain a
  postcommit git-status call, and the published "five-of-six FOOTPRINT-FALLBACK" figure is not
  a census of git-status use.
- **E3: 10** — the same pattern plus two `clj-kondo` baseline/candidate lint commands in Y4.

None of these were added to the fifteen (E4) or twenty-four (E3) nominated actions. The
sampling frame is unchanged.

## Blinding

Per Astra's rules. Reversible mapping kept in `/var/tmp/forge/e4-dual/BLIND-MAP.json` and
`BLIND-MAP-E3.json`, read by the runner only.

- Packets carry: the arm's complete receipt; the retained helper source where one exists;
  every action in the scored window in chronological order (nominated ones flagged); each
  nominated action's full command, full returned result and the caller's own commentary
  immediately before and after; and the caller's complete final report.
- Removed or replaced: cohort labels (`E2`/`E3`/`E4`), arm labels (`Z1`–`Z6`, `Y1`–`Y6`), slot
  numbers, wall times (all `HH:MM:SS` stamps and `(N s)` durations redacted, stamp table rows
  redacted), aggregate counts, prior verdicts, forecasts, and the gate paragraph. Path
  components replaced consistently within each packet (`cc-row5-Z4` → `proj-A`,
  `row5-adopt-4/Z4` → `work-A`, `e4-receipt.edn` → `split-receipt.edn`), preserving exact
  within-packet identity, command behaviour, receipt values, scope and chronology.
- Verified: `grep -ohE '\bZ[1-6]\b|\bE[234]\b|row5-adopt|cc-row5'` over all twelve packets and
  both shared task files returns nothing.
- Packet letters were assigned by a seeded shuffle (E4 seed 20260909 → A=Z4, B=Z5, C=Z3, D=Z2,
  E=Z6, F=Z1; E3 seed 31415926 → G=Y1, H=Y5, J=Y2, K=Y3, M=Y6, N=Y4) and presented in a second
  independent shuffle (E4 order E, C, A, F, D, B; E3 order K, N, M, J, H, G).
- The rendered task is identical across arms modulo the arm's own paths; it is supplied once
  per scoring pack as shared context.
- In the worked examples, arm names were relabelled to Greek letters (α…ζ) which do not
  correspond to packet letters, with that stated to the judges.

**Disclosed limit, as Astra required:** this is outcome and pairing blinding, not concealment
of treatment identity. E4 is calibration data, not a holdout. The three worked examples are
drawn from E4 itself; a judge can recognise those cases.

## Dual scoring protocol, and the deviations from Astra's registration

Two independent judges, each a fresh `claude -p` one-shot at the default model
(Claude Code 2.1.265), each given a byte-identical prompt: the classifier rule text and its
three worked examples, the shared rendered task, and the six blinded packets. Neither saw the
gate paragraph, the numeric statistic, Astra's forecasts, this brief's purpose, or the other
judge's labels. Neither was told a desired direction. Output format was constrained to one
JSON line per nominated action: id, label, one-line reason, and a separate motive class.

Deviations, stated plainly:

1. **Astra registered "a fresh Opus one-shot" plus "an independent Sol one-shot".** This run
   used two fresh `claude -p` default-model one-shots, per the operating brief. They are two
   samples of the same model and configuration, not two model families. **Independence here is
   weaker than the registration requires**, and a confirmatory E5 qualification would need the
   cross-family pair.
2. **Gene did not adjudicate.** Astra assigns disputes to Gene. No dispute was resolved by the
   runner; see the tie rule.
3. **The E3 pack was scored with the same rule text**, whose worked examples come from E4. That
   is a cross-cohort calibration leak into the E3 numbers and is why the E3 split is reported
   as a mechanism observation with a floor and a ceiling.
4. One aborted judge launch passed `--model sonnet` before the default-model requirement was
   applied. It was killed by PID at 22 s and produced zero bytes of output; no labels from it
   exist or were used.

## Tie rule — stated before it was applied

Recorded at `/var/tmp/forge/e4-dual/TIE-RULE.md`, written 2026-09-09T04:12:10Z. At that moment
the E4 reconciliation was complete with **zero disputes** (so no tie-break was ever applied to
E4), `judge2-E3.out` did not exist, and `judge1-E3.out` had landed in the same minute and had
not been read. The E3 numbers above are the first application of the rule.

> **R1.** No disagreement is silently resolved. Every disputed action is published as
> `unresolved-disputed` with both labels and both verbatim reasons; it counts neither as
> receipt-silent nor as trust-witness.
> **R2.** Receipt-silent counts are published as a range: floor = both judges said
> receipt-silent; ceiling = floor + every disputed action where either judge said
> receipt-silent. Astra's rule forbids defaulting to trust-witness and forbids scoring an
> unresolved case as a zero, so the ceiling is the number used for any coverage claim and the
> floor is reported beside it.
> **R3.** Where a judge's label contradicts one of Astra's three disclosed worked examples, the
> disclosed label governs — those are pre-registered applications of the rule text, not fresh
> judgments. *(Never fired: no judge contradicted a disclosed example.)*
> **R4.** The runner prepares evidence and does arithmetic. The runner does not resolve a label
> in either direction.

## Reconciliation

**E4: 15/15 = 1.000 agreement. Zero disputes.** Both judges labelled every action identically,
including the sole receipt-silent (Z1) and the sole instrument-excluded (Z2 slice.py). All
thirty labels, with both verbatim reasons per action, are in
`/var/tmp/forge/e4-dual/disputes-and-reasons-E4.md`; the machine-readable pair is
`reconciled-E4.json`.

**Read that 1.000 sceptically.** The only two non-trust-witness labels in E4 are precisely the
two cases Astra pre-labelled in the disclosed worked examples. On this pack the judges
reproduced the examples and called everything else trust-witness. Perfect agreement here is
evidence that the rule is *legible*, not that it is *robust*; the E3 pack, where no example
was pre-labelled, is the better robustness sample and it scored 0.833.

**E3: 20/24 = 0.833. Four disputes**, all in the same direction (judge 2 receipt-silent, judge 1
trust-witness) and all on the same proposition — whether caller-file *alias and require edits*
are covered by a receipt that has no `:ns_edits` field. Verbatim in
`/var/tmp/forge/e4-dual/disputes-E3.md`; reproduced here:


**Y2 / `J.6`** — judge 1 `trust-witness`, judge 2 `receipt-silent`

Command:

```
cd /home/forge/src/cc-row5-Y2 && grep -n 'defn-\? \(communicated?\|content-publishable?\|speaker-names\)' src/cfp_scheduler_killer/exports.clj; echo '--- exports requires calendar?'; grep -n 'exports.calendar' src/cfp_scheduler_killer/exports.clj || echo NONE; echo '--- callers requiring calendar:'; grep -rn 'exports\.calendar' src test bin dev 2>/dev/null | grep -v 'exports/calendar.clj:' | head -30
```

- judge 1 reason (verbatim): Promotions at 202/217/359 are :promotions; no exports.calendar require in exports.clj is :graph {:acyclic true}; the 9 caller files binding the destination namespace are exactly the 9 :static_sites_rewritten entries; caller says the receipt asserts acyclicity but 'I wanted the direct textual fact too'.
- judge 2 reason (verbatim): The `grep -rn exports\.calendar` part establishes that all 9 callers bind `:as calendar` (alias preference policy), which no receipt field states; covered parts: promotions (:promotions three forms), exports.clj not requiring calendar (:graph :acyclic true), 9 caller files rewritten (:static_sites_rewritten).

**Y3 / `K.9`** — judge 1 `trust-witness`, judge 2 `receipt-silent`

Command:

```
cd /home/forge/src/cc-row5-Y3 && for f in src/cfp_scheduler_killer/agent/commands.clj src/cfp_scheduler_killer/handlers/exports.clj src/cfp_scheduler_killer/handlers/public_cfp.clj src/cfp_scheduler_killer/handlers/public_widgets.clj src/cfp_scheduler_killer/inform.clj src/cfp_scheduler_killer/session_invites.clj test/cfp_scheduler_killer/comms_test.clj test/cfp_scheduler_killer/exports_test.clj test/cfp_scheduler_killer/schedule_test.clj; do echo "== $f"; grep -n 'exports\.calendar\|exports :as' $f | head -4; echo "   calendar/ sites: $(grep -o '\bcalendar/[a-z?!-]*' $f | wc -l)  exports/ sites: $(grep -o '\bexports/[a-z?!>-]*' $f | wc -l)"; done
```

- judge 1 reason (verbatim): Per-file calendar/ site counts 1,1,1,1,1,2,3,21,12 and the 9 caller files exactly match :static_sites_rewritten and :counts {:caller_files 9 :caller_sites 43}; require presence follows from those entries; caller states it wanted 'independent arithmetic on the tree rather than on the receipt's own table'.
- judge 2 reason (verbatim): Per-file site counts are covered exactly by :static_sites_rewritten (1/1/1/1/1/2/3/21/12 = 43), but the action also establishes the alias bound in each caller (`:as calendar`, preference policy) and that public_cfp/inform dropped the `exports` require; no receipt field states aliases or require edits.

**Y5 / `H.10`** — judge 1 `trust-witness`, judge 2 `receipt-silent`

Command:

```
cd /home/forge/src/cc-row5-Y5 && for f in src/cfp_scheduler_killer/session_invites.clj test/cfp_scheduler_killer/exports_test.clj src/cfp_scheduler_killer/inform.clj; do echo "=== $f"; git diff "$f" | grep "^[-+]" | grep -v "^[-+][-+][-+]"; done
```

- judge 1 reason (verbatim): Caller-site rewrites in session_invites (2), exports_test (21) and inform (1) are the matching :static_sites_rewritten counts, with no surviving exports/ reference per :stale_references :count 0; caller states it was a 'spot check of shape, not count'.
- judge 2 reason (verbatim): Caller diffs used as a 'spot check of shape': alias `calendar` per preference order and `exports` require retained/replaced per file, neither stated by any receipt field; covered part: site counts (:static_sites_rewritten session_invites 2, exports_test 21, inform 1).

**Y6 / `M.10`** — judge 1 `trust-witness`, judge 2 `receipt-silent`

Command:

```
cd /home/forge/src/cc-row5-Y6 && git diff src/cfp_scheduler_killer/session_invites.clj src/cfp_scheduler_killer/handlers/public_widgets.clj test/cfp_scheduler_killer/comms_test.clj
```

- judge 1 reason (verbatim): Rewrites in session_invites (2), public_widgets (1) and comms_test (3) are :static_sites_rewritten entries with exactly those counts; no stale exports/ reference survives per :stale_references :count 0 over 473 files; caller gives no reason for this read.
- judge 2 reason (verbatim): Full diffs of session_invites/public_widgets/comms_test establish the require insertion and alias binding (`:as calendar`, retained `exports` alias) in callers, which no receipt field states; covered part: site counts (:static_sites_rewritten 2/1/3).


## The motive annotation is the unreliable half

Astra required the caller's stated reason to be recorded separately and warned that
trust-witness "must mean covered re-derivation, not a psychological diagnosis". The data
supports the warning:

| pack | label agreement | motive-class agreement |
|---|---|---|
| E4 | 15/15 = 1.000 | 9/15 = 0.600 |
| E3 | 20/24 = 0.833 | 18/24 = 0.750 |

E4 motive tallies — judge 1: independent-witness 9, unstated 4, presentation 2. Judge 2:
independent-witness 3, other-stated 5, unstated 5, presentation 2. The two judges agree on
*whether the receipt covered it* far more readily than on *why the caller looked anyway*. The
motive field should be published as a disputed annotation, never used as a denominator.

## What this run does NOT establish

- Not a retrospective E4 pass. E4's legacy ANSWERABLE result stands at 0.675 against a failed
  0.50 gate.
- Not evidence that row 5 is friction-low.
- Not the independent dual-judge qualification a confirmatory E5 requires (same model family
  twice; no Gene adjudication).
- Not a fresh treatment estimate, a wall comparison, or a native-speed comparison. No arms ran.
- Not proof that a recorder or any further receipt field would retire the one remaining
  receipt-silent action; that action is a *snapshot freshness* gap, and a self-reported field
  written by the same issuer does not close it.

## Recommended next step

Do not launch six E5 caller arms against the registered coverage gate. Its median is pinned at
≥ 1.00 by five zero denominators before E5 runs a single action. Either amend the endpoint
before launch and disclose the amendment (Astra's own instruction: "If the E4 baseline exposes
an unhandled pattern, amend before launch and disclose that calibration"), or accept the
achieved coverage floor — **1 receipt-silent action across six arms, 13 of 15 covered
re-derivations** — as the finding and spend the arms on the trust-witness class instead, which
is where all the remaining work actually is.

## Artifacts

All under `/var/tmp/forge/e4-dual/` unless stated.

| file | sha256 |
|---|---|
| `judge-rules.md` | `62d3ac6586c61768fe8a0e2582a2efda0b5539adb01ec40dd9db9bbe25bcd128` |
| `judge-prompt.md` | `29379d615d733301904d689c2a95347c837dd2723ad10feb18a27bb8a2f146ac` |
| `judge-prompt-E3.md` | `8f9556cfbdb88e04fc5e0082cfcc4cafcd54bd947fbb63f733321cefdf34296f` |
| `judge1.out` | `5fac67a5aa9e28e3eebf655d2291326696a126706918504d94d2a8938dcc34c8` |
| `judge2.out` | `692cb3f1d398058441327223034e29fbfae0203f62ddef8a25f4b56b4816eada` |
| `judge1-E3.out` | `36538fa2e78191f7df0301326f73cd097faf88eadf44999cb07288cc7098bde7` |
| `judge2-E3.out` | `c85fc3d862c65e12ddedb40749d8c44f7f4c71225d9b60321987be761ddb760f` |
| `packet-A.md` | `9db41c032188d5c04f31673b43ede54b6dffb26b1925285467417c9405b82a1a` |
| `packet-B.md` | `fbfa0f8ad4e0038e2384ad139e44614baf1fab73ef9cf10c4dba405fd3a28778` |
| `packet-C.md` | `a12bd7302774025d6b5d596959c1bd1eaf46e35e2bce73c06e050ec9e8c49543` |
| `packet-D.md` | `d264459c3b11230a32acb8b54fe450948285250526712036a347d0c31429cfd5` |
| `packet-E.md` | `27527f53f0779a9eea8035eefd6c7ca977f074f66ef9b93b9140b67d9ca4953c` |
| `packet-F.md` | `db2471b4673afe71339cb1781773efdccbcd5b691e648c543e8999dcc9bc82bf` |
| `packet-G.md` | `9524ee41aa1e07ca67a6ff2f496930953c5af59fa2ebb1804286e7e39f3a54f0` |
| `packet-H.md` | `6cc28af08e6163e194c9a110c4b66ea6d354f5182f6cd3f73a145310b460ec7b` |
| `packet-J.md` | `29e28701130e51128f260ba080d7710ae92ec0e5dfff99dfff820b87dc22e483` |
| `packet-K.md` | `86bbcc71633f4ccaf4c6176bfd83535706c079d2a6ada587f463e39c218999eb` |
| `packet-M.md` | `17863dbc2158ccc10ef363098cbe3c68300982ab3af68ee84f423edae12b787b` |
| `packet-N.md` | `c7f3541332207b1b0a5a3684c10c1645b4f2084e6f786c2d9368e9977cadb7e9` |
| `shared-task.md` | `581ec6047203ee863c58d1c98f21bf5be03522f88406450431c85d4c4d4392bc` |
| `shared-task-E3.md` | `27edcb130c62fd422dcf5a458118b91df757520036b869ba04e695b1b23feabd` |
| `extract.py` | `5a3baa70ee7099d91eba381a685582963107740505b24624d4bc3839bfd1d0c5` |
| `build-packets.py` | `105b6f254bf40c7ba1b14fc34dba1d80183fcddfb479c1ef764aca267d606513` |
| `reconcile.py` | `cad411d39b8e4091a8df6c738ceab789d58df60916abb6e3ee3c74b6fe9f95b6` |


Also retained: `E4-actions.json`, `E3-actions.json` (extracted nominated actions with full
command, full result and commentary), `reconciled-E4.json`, `reconciled-E3.json`,
`disputes-and-reasons-E4.md`, `disputes-E3.md`, `BLIND-MAP.json`, `BLIND-MAP-E3.json`,
`TIE-RULE.md`, `HASHES.txt`, `presentation-order.txt`, `presentation-order-E3.txt`.

Source evidence read, unmodified: `/var/tmp/forge/row5-adopt-4/` (prereg, `E4-readout.json`,
`E3-answerable.json`, `transcripts.json`, `classify.py`, `answerable.py`, Z1–Z6 arm
directories), `/var/tmp/forge/row5-adopt-3/` (Y1–Y6), the six Z and six Y JSONL transcripts
named by each `transcripts.json`, and
`/var/tmp/forge/plan2/cellC/astra-e5-meter-report.md`.
