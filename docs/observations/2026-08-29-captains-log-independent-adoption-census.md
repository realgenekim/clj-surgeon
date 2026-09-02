# Captain's Log: independent Surgeon adoption census

<!-- agent-usage-window-end: 2026-08-30T04:17:08.306112Z -->

Date: 2026-08-29 PT  
Window: `[2026-08-24T07:00:00Z, 2026-08-30T04:17:08.306112Z)`  
Verdict: **the strategic conclusion reproduces, but several headline counts and the 8.7% middle rung do not reproduce under one stated population rule.**

## Executive result

An independent raw-rollout fold found 118 Clojure-relevant Codex sessions that began inside the frozen window. They contain:

- 1,161 successful outer Codex actions that invoked native `apply_patch`;
- 7 direct `edit_clojure` calls: 4 accepted and 3 refused;
- 0 `apply_clojure_changes` calls;
- 443 `inspect_clojure` calls, or 441 outer actions containing an inspection.

This is 0.60% attempted write adoption and 0.34% accepted write adoption relative to native write actions. The reported 0.52% is therefore directionally correct. It is not a stable exact rate: it requires selecting 6 rather than the 7 direct edit calls and 1,145 rather than the 1,161 successful outer native actions.

The most important conservative result **does** reproduce exactly: 70 established `src/` or `test/` native actions remain after the structural exclusions. That is 6.0% of the independently counted native actions, nearly identical to the reported 6.1%. The practical 5–6% prize survives. The claimed 8.7% addressable ceiling does not: the independent rules leave 155–162 established-file actions, or 13.3–14.0%.

## Method and evidence

I did not read or run the first census implementation. I used the repository's `study-agent-usage` collector only to freeze a privacy-safe list of exact rollout evidence, then wrote a separate streaming fold over those receipt-named files.

- Collector receipt SHA-256: `b61a703eb42fc886530b4dfbcb11fa7ee84d58aec89e861121c14e9be910a5db`
- Independent fold: [`bench/results/2026-08-29-adoption-census-independent/independent-fold.json`](../../bench/results/2026-08-29-adoption-census-independent/independent-fold.json), SHA-256 `ffdef620d77cd63e009e9d2bd3c3d9cdc8f7d3aaf3de30d80627bb8c6d9d61bd`
- Fold source: [`dev/experiments/adoption_census_independent.py`](../../dev/experiments/adoption_census_independent.py), SHA-256 `a99a9ed0c1dd45e216b65e5400b181aabb572013ade704a51f41886c6eb71b90`
- The fold emits no paths, commands, source, prompts, tool arguments, or result content.
- No model call was made. Searches were limited to the 122 files named by the receipt; no home-directory scan occurred.

The population rule is explicit: use the **first** `session_meta` timestamp and retain sessions that began inside the half-open window. This matters. Four long-running sessions began before the window but remained active inside it. Including them changes the census to 2,750 successful outer native actions, 73 edits, 59 heavyweight applies, and 1,042 inspection actions. Those four sessions contain most of the self-hosting Surgeon work. An adoption rate without a session-start rule is therefore not interpretable.

The first collection attempt used a future end time and was discarded before analysis. The receipt above froze the actual collection instant.

## Headline count reconciliation

| Measure | Relayed census | Independent result | Reproduction verdict |
|---|---:|---:|---|
| Retained Clojure-relevant sessions | 121 | 122 overlapping; 118 began in window | Population differed |
| Native write actions | 1,145 | 1,161 successful outer actions | Close, not exact |
| `edit_clojure` calls | 6 | 7 | One unexplained successful call was excluded upstream |
| `apply_clojure_changes` calls | 0 | 0 | Exact |
| `inspect_clojure` calls | 400 | 443 calls / 441 outer actions | Close, not exact |
| Failed direct edits | 3 of 6 | 3 of 7 | Numerator exact; denominator selective |

No single time cutoff produces the relayed totals. In this frozen population, the sixth edit occurred at 10:45 PT, the 400th inspection at 18:45 PT, and the 1,145th successful native invocation at 20:07 PT. A second eligibility rule was applied in the original census but was not present in the relayed claim.

## The three live failures

The three refusals are real. All occurred in one subagent session, all returned `invalid-intent-form`, and all were followed by another mutation attempt:

| Refusal | Raw argument size | Backslash characters in argument source | Next successful mutation |
|---|---:|---:|---|
| 1 | 4,220 chars | 146 | `edit_clojure`, 26.4s later |
| 2 | 694 chars | 3 | `edit_clojure`, 13.8s later |
| 3 | 4,736 chars | 215 | native patch, 47.7s later |

Thus the felt-failure claim is earned: each refusal imposed a retry boundary. The stronger claim that all three were the same backslash-heavy string-literal failure is not earned. Two calls were escape-heavy; one contained only three backslashes. The first successful retry was almost equally escape-heavy. The supported statement is narrower: all three were malformed intent-form refusals, and complex string carriage was present in two of them but was neither necessary nor sufficient for failure.

Counting these as 3 of 6 requires excluding one additional successful direct edit. Under the explicit session-start population the honest rate is 3 of 7, or 42.9%. That is still an unacceptable point-of-use refusal rate, but it is not 50%.

### Subsequent body-fidelity bisection

SURGEON2 independently localized the broader escape-heavy corruption claim at immutable commit `2290192c7e83c9afd95a54c30768aa5f94afdb96`. Its retained 18-arm harness had emitted free-text requests; it never called MCP. Five of 18 replayed messages contained a wrong body, but four of those five were invalid Clojure and only one was both wrong and Clojure-valid.

A separate no-model probe then sent five escape-sensitive fixtures through real HTTP MCP. All five committed with exact readback, including regexes, nested quotes, literal escape sequences, and a maximum wire backslash depth of nine. Malformed Clojure refused before write and left source byte-identical. This establishes a sharper boundary:

- the three census refusals remain real point-of-use retry costs;
- model transcription of long bodies can fail before the tool call;
- no MCP transport, parser, or writer corruption was reproduced;
- backslash depth does not predict correctness;
- a valid but unintended replacement remains undetectable without an independent intent oracle.

Accordingly, do not use the census refusals to justify a parser/writer change. The earned design direction is to reduce model retranscription: compile transformations server-side or consume reviewed frozen replacement bytes. The body-fidelity raw archive is bound by SHA-256 `9c552a417fb67761cb338880f8ef8a9da7c16e326aaa7087282b919b09b0bc05`.

## Addressable ladder

The independent ladder uses one successful outer native action as the unit. An action is an existing-Clojure update when it updates at least one `.clj`, `.cljc`, or `.cljs` file. “Small” means one hunk and at most four added-plus-removed lines. “Same-session created” is tracked from earlier successful `Add File` actions. For a multi-file action, I report both conservative interpretations: all Clojure targets created versus any target created.

| Rung | Relayed | Independent | Share of 1,161 |
|---|---:|---:|---:|
| All successful native actions | 1,145 | 1,161 | 100.0% |
| Updates to existing Clojure | 444 | 463 | 39.9% |
| Comment-only | 23 | 8 | 0.7% |
| One hunk, at most 4 changed lines | 82 | 74 | 6.4% |
| After union of comment/small exclusions | 349 | 385 | 33.2% |
| Same-session-created targets | 249 | 223 all / 230 any | 19.2% / 19.8% |
| Established files remain | 100 | 162 all / 155 any | 14.0% / 13.4% |
| Established actions in `src/` or `test/` | 70 | 70 | 6.0% |

I agree with excluding comment-only work and tiny visible patches from a default Surgeon opportunity set. I do **not** agree that an action should disappear because any one target was created during the session; that throws away an established-file edit in the same atomic action. The all-target rule is the safer ceiling. Both independent variants remain materially above 100.

The same-session count is also a lower-confidence rung. The fold can prove files created by an earlier native `Add File`, but it deliberately does not parse arbitrary shell commands for file creation. That limitation can only increase the created-file deduction. It does not explain the comment-only mismatch or establish the exact reported 249.

## Circularity and the non-Surgeon falsifier

The corpus is heavily self-referential. Of the 1,161 successful outer native actions:

- 905 (78.0%) occurred in `clj-surgeon`;
- 96 (8.3%) occurred in other Clojure repositories;
- 160 (13.8%) occurred elsewhere.

The identical ladder on the 96 non-Surgeon Clojure actions is more encouraging than the aggregate skew:

- 93 updated existing Clojure;
- 78 remained after comment/small exclusions;
- 70 remained under the all-target established-file rule;
- 65 were in `src/` or `test/`.

In other words, 65 of the final 70 conservative opportunities—92.9%—came from non-Surgeon Clojure repositories. The research program over-represents self-hosting iteration in its gross activity, but the conservative 6% opportunity is not manufactured by clj-surgeon editing itself. The non-Surgeon sample is small, so this is **measured**, not a population estimate.

## Decision

Keep the strategic conclusion, revise the precision:

1. Write adoption is indeed near zero: approximately 0.3% accepted and 0.6% attempted in this population.
2. Read adoption is two orders of magnitude higher, but the exact “400” depends on an unstated eligibility rule.
3. The direct-edit point-of-use experience is bad: three real malformed-intent refusals caused retries. Call it 3 of 7 unless the excluded seventh call is named and justified.
4. The most defensible realizable prize remains about 6% of native actions. That figure reproduced exactly and is mostly supported by non-Surgeon Clojure repositories.
5. Do not publish 8.7% as an independently reproduced ceiling. The stated ladder is not arithmetically or definitionally reproducible; 13–14% is the independently derived established-file ceiling before the conservative `src/`/`test/` restriction.

The surprise is useful: the broad strategic reframe survives, while the strongest-sounding failure rate and the middle addressability rung were artifacts of an unstated population/eligibility choice. Making those rules explicit is now cheaper than debating the headline.
