# Independent confirmation audit of the 2026-08-31 speed claims

Date: 2026-08-31

## Verdict

The low-level performance story is real, but the receipts do not support every
headline exactly as stated. Four claims survive with bounded caveats, one
contains two separately confirmed measurements, and the nominated receipt for
the exact `or-bang` range does not support that range. The audit treats Git
commits, machine receipts, and raw trial rows as evidence; commit messages and
the conn's prose are not evidence by themselves.

| Claim | Verdict | Receipt-level finding |
|---|---|---|
| 1. `eligible-descriptor` refactor | **CONFIRMED with caveat** | One edit, one file, one successful `edit_clojure` transaction at 117.570125 ms server elapsed. The caller supplied both complete forms, but they total 4,784 B, not about 4.3 KB. |
| 2. `form-evidence?` confirm+fill refactor | **CONFIRMED with caveat** | The caller authored only the replacement; the `from` was server-supplied. The replacement was 2,263 B in the committed diff, or 2,264 B in the caller's newline-terminated heredoc. This provenance is local-transcript/local-receipt evidence, not a pushed machine receipt. |
| 3. `or-bang` 0.84--1.04 s and $0.00094/8 | **UNSUPPORTED as stated** | The cost is confirmed exactly at $0.00094325 across eight calls, but the nominated raw receipts span 0.554--4.831 s, not 0.84--1.04 s. |
| 4. Warm Spark bang and Spark decode rate | **CONFIRMED with caveat** | The warm prepared-edit median is 2.287578313 s over ten exact one-shot turns. The separately measured 152.9 tok/s is also reproducible, but it is an end-to-end five-trial statistic including reasoning tokens and process wall, not pure visible-token decode speed. |
| 5. W1 caller-token reduction | **CONFIRMED with caveat** | The per-arm medians are 967.5 versus 1,140 o200k MCP-argument tokens: `(967.5 - 1140) / 1140 = -15.1316%`. It is a small, single-task workflow-emission result, not billed model usage or a wall-time win. |
| 6. Differential verification 63/63, including Spark's 58-case hunt | **CONFIRMED with caveat** | The literal counts and 11.6894 s probe wall are recoverable. The 58 Spark cases were badly constructed and all reached the same early rejection, so 63/63 is weak equivalence evidence rather than broad adversarial coverage. |

## Audit basis

- `47d48390a673b68736593ed0b9e76395b08baf79` and
  `0ba2e465103f45796ebb2d573ada4f0c82d9e226` are on pushed branch
  `origin/refactor/eligibility-legible-checks-20260831`.
- `c390afb5090cb1fce56af840cbf8e550d46b32e8` is on
  `origin/experiment/fuel-table-completion-20260831`.
- `9b6c9708` is on `origin/experiment/warm-executor-screen-20260830`.
- `2a2915f6` is on
  `origin/experiment/elaborator-fallback-battery-20260831`.
- `d247b4dae4bd0a27aad70a7ad5e68a39bc44edfb` was fetched from pushed
  `muscle/experiment/w1-product-cohort-20260831`; it was not on `origin` at
  audit time.
- The requested Spark probe is local at
  `/private/tmp/bang-rig/state/probe-1788179508769-27891.json`, SHA-256
  `c0a1f6c8fb617fbc5098cf7935220fa8b5629cc58b8286258dec38fa56521271`.
  It is not itself a pushed receipt.

## 1. `eligible-descriptor`: fast server transaction, larger caller payload

**Verdict: CONFIRMED with caveat.**

The originating tool receipt records exactly one `edit_clojure` call with one
edit, one changed file, `verification_complete=true`, and
`elapsed_ms=117.570125`. Its receipt hash begins `d58a4a6a`; the read-back hash
matches the post-change file recorded at `47d48390`.

The caller tool input contains the complete `from` and `to` strings. Structural
reads of the frozen parent and candidate forms reproduce these UTF-8 sizes:

| Caller-authored field | Bytes |
|---|---:|
| `from` | 1,838 |
| `to` | 2,946 |
| Sum | **4,784 B** |

That is 4.784 decimal KB or 4.67 KiB. Calling it about 4.3 KB understates the
actual string bytes by 484 B, roughly 11% relative to 4,300 B. The important
clock qualification is equally strict: 117.570125 ms is server transaction
time, not the caller's complete think-and-type wall. The complete compact JSON
tool input was 5,063 B once paths, guards, keys, and punctuation are included.

The pushed commit retains the source diff and a prose claim about the timing;
the raw operation receipt is recoverable from the originating local Claude
transcript and the local undo-receipt store, not from the Git tree itself.

## 2. `form-evidence?`: server-owned `from`, caller-authored `to`

**Verdict: CONFIRMED with caveat.**

The commit diff at `0ba2e465` contains both sides because Git necessarily
records the old and new source. That does not mean the caller emitted both.
The originating caller input shows the actual route:

1. hold one MCP session;
2. inspect `form-evidence?` and receive a prepared-confirmation digest;
3. author one replacement heredoc;
4. send `confirm` plus `fill.arguments.edits[0].to` for preview;
5. send the same confirm+fill for commit.

No old form appears in that caller tool input. The local transaction receipt
materializes `intents[0].from` at 1,236 B and `intents[0].to` at 2,263 B, which
is direct evidence that the server supplied the old form. The caller-authored
heredoc is 2,264 B because it includes one terminal LF; the committed form is
2,263 B. Thus “about 2.1 KB” is directionally fair but low: 2.263 decimal KB,
or 2.21 KiB.

The caller authored the replacement once, but transmitted it twice over HTTP
because preview and commit were separate calls. The fast-path provenance is
therefore confirmed from the local tool-call structure and receipt, while the
pushed commit alone proves only the final diff. The complete hand-built
Bash/curl wrapper was 4,276 B, so “to-only” accurately describes authored
source content, not the total wrapper or total bytes sent on the wire.

## 3. `or-bang`: cost confirmed, claimed range absent

**Verdict: UNSUPPORTED as stated.**

At `c390afb5`,
`bench/model-variant-battery/results/oss-120b/score.json` records eight
scheduled/completed fills, 8/8 exact, 8/8 one-shot, and provider cost
`0.0009432499999999999`. Summing the eight raw `provider_cost_usd` fields gives
exactly **$0.00094325**. Every raw receipt names returned model
`openai/gpt-oss-120b`, provider `Cerebras`, and `committed=true`.

The same eight raw `timing_s.total` values are:

```text
4.831, 2.107, 0.976, 0.573, 0.554, 0.644, 0.623, 0.656 seconds
```

The full range is 0.554--4.831 s and the median is 0.650 s. Six of eight are
sub-second; after the first two startup/outlier calls, the range is
0.554--0.976 s. This is excellent narrow-path performance, but it is not the
claimed 0.84--1.04 s range. Because the claim is conjunctive and its exact
range is absent from the nominated receipt, the claim as stated is
unsupported even though its cost subclaim is confirmed.

## 4. Spark: 2.288 s warm bang and 152.9 tok/s

**Verdict: CONFIRMED with caveat.**

### Warm prepared edit

At `9b6c9708`, the pushed summary records
`warm_prepared_median_round_trip_ms=2287.578313`, `warm_prepared_n=10`, ten
exact one-shot outcomes, and zero wrong-subject outcomes. The ten raw
`turn_e2e_ms` values sort to:

```text
1829.146, 1931.967, 1963.988, 2000.974, 2282.691,
2292.465, 2611.913, 2680.361, 2736.473, 5249.682 ms
```

The middle-pair mean is 2,287.578313 ms. This is confirmed at receipt. It is a
ten-turn result on one persistent app-server and one persistent Spark thread,
after an unscored warm-up, for one exact-edit family on one host/account.

### End-to-end decode rate

At `2a2915f6`, Spark's B condition has five valid oracle-passing trials, median
decoded tokens 533, and median wall 3,486 ms. The committed scorer's number is
reproduced directly:

```text
533 / 3.486 = 152.897... tokens/s -> 152.9 tokens/s
```

Here “decoded tokens” means provider-reported output plus reasoning tokens,
and wall spans fresh ephemeral Codex process start through completion. It is
therefore an end-to-end throughput measurement, not pure kernel decode speed
or visible-token-only rate. The same receipt's 5,243.9 tok/s
bootstrap-subtracted number is explicitly unresolved and is not confirmed by
this audit.

## 5. W1 product cohort: 15.1% fewer caller argument tokens

**Verdict: CONFIRMED with caveat.**

At `d247b4da`, the committed episode table has eight C and eight O episodes.
The per-arm medians are 967.5 o200k tokens for prepared confirmation plus
preview and 1,140 for ordinary compact editing:

```text
(967.5 - 1140) / 1140 = -0.151315789... = -15.1%
```

The metric counts canonical-JSON MCP argument tokens over the complete
observed workflow, including recovery calls. It does not count total model
input/output or billed tokens. The result is also specifically a
median-of-arm comparison; the aggregate/mean reduction is different.

Both arms reached exact source in 8/8 episodes, but only 2/8 C episodes and
1/8 O episode followed the intended route without request-shape recovery. The
prepared arm was **1.180 s slower** at median complete wall, 46.635 s versus
45.455 s. This receipt confirms an emission win, not a caller-wall speed win,
and its external validity is limited to one task, one model, and n=8 per arm.

## 6. Differential verification: the arithmetic is real; the hunt is weak

**Verdict: CONFIRMED with caveat.**

The requested local probe records:

- successful result `{:tested 58, :disagreements []}`;
- `spark_ms=9339` for the successful iteration;
- `timing_ms.total=11689.4`, or 11.6894 s, for the full probe including one
  failed first iteration and predicate checks;
- final `verdict="success"`.

The local originating Claude transcript records five additional named direct
probes, each with `:agree true`, so the literal arithmetic is 58 + 5 = 63
agreements. Those five are transcript evidence, not a pushed Git receipt.

The decisive caveat is corpus construction. Spark's generated `base` placed
`:rows`, `:forms`, and `:snapshot_guards` at the top level while omitting
`:ok` and `:results`. Production `eligible-descriptor` obtains the row from
`(:results result)` and rejects immediately when `(:ok result)` is not true.
Consequently, all 58 supposedly varied adversarial maps exercised the same
early rejection and returned no disagreement. The five direct cases were also
malformed early-nil cases. Further, the probe's success predicate required
only at least 40 tests and a vector-valued `:disagreements`; it did not require
that vector to be empty. The actual result is empty, but the gate would have
accepted disagreements.

Thus **63/63 and 11.7 s are numerically confirmed**, but they do not establish
broad behavioral equivalence. A valid follow-up would start from a genuinely
near-eligible result map, mutate one production field at a time, and require
`(empty? (:disagreements ...))` in the success predicate.

## Honest assessment

Yes, the performance story is real at the boundaries actually measured:
guarded server edits can complete in about 0.1 s, a held Spark caller can
complete this prepared-edit family at a 2.288 s median, and six of eight
metered oss-120b loops were sub-second for less than one tenth of a cent total.
The strongest link is the warm Spark result because the pushed receipt retains
ten raw exact one-shot trials and the median recomputes exactly. The weakest
performance link is the claimed 0.84--1.04 s `or-bang` range because the
nominated receipt contains different timings; the weakest safety link is the
63/63 differential headline because its 58-case corpus was effectively one
early-rejection case repeated. Most importantly, the receipts prove fast
mechanisms, not yet a universal caller-wall win: the W1 product cohort saved
15.1% of emitted argument tokens while remaining 2.6% slower at median wall.
