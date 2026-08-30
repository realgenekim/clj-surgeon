# External-corpus write-shape census

Date: 2026-08-30 PT

Window: `[2026-08-22T00:00:00Z, 2026-08-30T02:09:33.141926Z)`

Verdict: **Codex mutation adoption remains below 2%, insertion does not cross the reprice gate, but the 6% addressable ceiling does not generalize. Subject repetition remains unresolved at owner identity.**

## Executive result

I preregistered the eligibility rules and thresholds before counting at commit
`502f70a3c617af4ce44fd389be57448a260bec5f`. I then folded retained Codex
rollouts from the two non-Surgeon Clojure repository roots with the largest
eligible session populations. The report uses neutral labels and emits no
repository paths, session identifiers, commands, source, patches, prompts, or
tool arguments.

The two repositories are materially different:

| Measure | External A | External B |
|---|---:|---:|
| Active evidence files in window | 17 | 12 |
| Successful native writes confined to the repository | 63 | 266 |
| Existing-Clojure update actions | 0 | 156 |
| Successful public structural mutation calls | 0 | 4 |
| Structural mutation share | 0.00% | 1.48% |
| Final established `src/`/`test/` opportunity rung | 0 | 119 |
| Final rung as share of native writes | 0.00% | **44.74%** |

External A is an honest zero for the Clojure-shape questions: its retained work
changed files in a Clojure repository, but none of its 63 successful in-repo
native writes updated an existing `.clj`, `.cljc`, or `.cljs` file. External B
therefore supplies the only edit-shape denominator. This is one external shape
corpus, not a population estimate.

## Registered predictions and outcomes

| Prediction or law | External result | Verdict |
|---|---|---|
| Repeated subjects stay below 20% | File identity: 26/50, **52.0%**; owner-visible subset: 8/60, 13.3% | File prediction failed; owner verdict unresolved |
| Insertion stays below 40% | 32.19% of changed hunks; 40.55% of changed bytes | Count prediction passed; byte prediction missed by 0.55 points |
| Insertion reprices at 50% of bytes | 40.55% | No reprice; splice kill remains closed |
| Successful structural adoption stays below 5% | 4/(266+4), **1.48%** | Confirmed for Codex in External B |
| Final `src/`/`test/` rung stays below 15% | 119/266, **44.74%** | Failed decisively; the 6% ceiling must be repriced |

The edit-shape mass in External B is 351 changed hunks and 225,626 UTF-8
changed bytes:

| Shape | Hunks | Changed bytes |
|---|---:|---:|
| Insertion | 113 | 91,495 |
| Replacement | 232 | 133,100 |
| Deletion | 6 | 1,031 |

Forty-four empty navigation anchors were excluded from the hunk denominator.
They carried no addition or removal and are patch-navigation syntax, not edit
events.

## What this does to the five Grade-A stops

1. **Declared-intent compression: HOLD, not reopened and not reconfirmed.**
   Repository-relative file identity crosses the registered 30% reprice line:
   26 of 50 file subjects repeat within a task turn. But the preregistration
   deliberately treated file identity as an upper bound. Only 68 of 351 changed
   hunks expose a unique named top-level owner in retained patch context. In that
   19.4%-coverage subset, 8 of 60 owner subjects repeat, or 13.3%. The file-level
   signal invalidates the `<20%` prior; the missing owner provenance prevents an
   identity-safe reprice.

2. **Guarded labels: same HOLD.** The corpus shows repeated file carriage but
   does not prove repeated self-describing owner identity. It therefore does not
   rehabilitate opaque or positional labels, and it does not prove that full
   owner declarations would pay.

3. **Mnemonic sizing: remains closed on safety and unresolved on economics.**
   The external evidence does not create a new authority mechanism. Compressing
   repetition is still permitted; replacing identity with an unchecked
   reference is still prohibited.

4. **Single-anchor splice: confirmed closed under the registered gate.**
   Insertion is a minority by hunk count and 40.55% by changed-byte mass. The
   byte prediction narrowly failed, but the forward reprice threshold was 50%,
   and the corpus did not cross it.

5. **The write-side 94-second / 6% ceiling: reprice.** The 94-second figure
   remains a valid measurement of the original circular research corpus. It is
   not an external ceiling. External B leaves 119 established `src/`/`test/`
   actions after the same exclusions, or 44.74% of its in-repo native writes.
   External A leaves zero. The correct conclusion is heterogeneity by repository
   and work mix, not one global 6% opportunity rate.

The addressable ladder for External B is:

| Rung | Actions | Share of 266 native writes |
|---|---:|---:|
| All successful in-repo native writes | 266 | 100.0% |
| Updates to existing Clojure | 156 | 58.6% |
| After comment-only and small one-hunk exclusions | 127 | 47.7% |
| After same-mission-created exclusion | 119 | 44.7% |
| All targets in `src/` or `test/` | 119 | 44.7% |

This does **not** claim that Surgeon can or should absorb all 119 actions. It
does prove that the earlier 6% action ceiling was corpus-specific and cannot be
used to bound production-repository upside.

## Independent reconciliation

I reran the prior independent adoption-census lineage against a separately
filtered, privacy-safe receipt for External B. Its parser and population fold
were written before this experiment.

- Existing-Clojure actions reproduce exactly: `156` versus `156`.
- The final established `src/`/`test/` numerator reproduces exactly: `119`
  versus `119`.
- The legacy in-repo native denominator is `263`; the corrected fold is `266`,
  a difference of three actions or 1.13%. The legacy parser binds the first
  patch string to every `apply_patch` invocation in a multi-call cell. The new
  parser resolves each inline or most-recent assigned patch independently.
- The unconfined legacy total is not a repository denominator: 229 successful
  writes in the selected sessions targeted temporary or other workspaces and
  were excluded from the repository-specific fold.

Thus the load-bearing 119 numerator is independently exact, and the 44.74%
share is not sensitive to the three-action parser correction.

## Provider boundary

The bounded `study-agent-usage` receipt contains 43 Claude sessions and 114
Codex sessions overall, but its privacy-safe session projection intentionally
does not expose repository cwd. I did not rediscover Claude transcript formats
or infer repository identity from filenames. Claude is therefore **unmeasured**
for these two external roots, and no Claude count is combined with the Codex
denominator. The separate retained `16/16` Claude routing result is context, not
part of this census.

## Evidence and reproducibility

- Preregistered commit: `502f70a3c617af4ce44fd389be57448a260bec5f`
- Privacy-safe collector receipt SHA-256:
  `3ca4599657a666c37276e85d941a61750abe0904cb0ef647034cc05a3ad30600`
- Census report: [`bench/results/2026-08-29-external-corpus-shape-census/report.json`](../../bench/results/2026-08-29-external-corpus-shape-census/report.json)
  SHA-256 `bd7dd552e31b2e5fd068b6bfdcabec6b616376e6d58f1f9b9ea2b37be801e670`
- Reconciliation: [`bench/results/2026-08-29-external-corpus-shape-census/lineage-reconciliation.json`](../../bench/results/2026-08-29-external-corpus-shape-census/lineage-reconciliation.json)
  SHA-256 `c37cf569fa353383797b187edc610180a875c5798d1bbe9d03bc353be191f409`
- Fold: [`dev/experiments/external_corpus_shape_census.py`](../../dev/experiments/external_corpus_shape_census.py)
  SHA-256 `f496731864d07e182c9570eb81922459b0f479df916b9d205b1dc4cddd4a6636`
- Pure tests: [`dev/experiments/external_corpus_shape_census_test.py`](../../dev/experiments/external_corpus_shape_census_test.py), 8 tests green
  SHA-256 `80d8a47be9780cc904df11e1404b968b8da550d4cd68a094dc68a11a6d40b418`
- Prior lineage fold SHA-256:
  `a99a9ed0c1dd45e216b65e5400b181aabb572013ade704a51f41886c6eb71b90`

The collector enumerated only the documented
`~/.codex/sessions/2026/08/<day>/rollout-*.jsonl` layout. It did not scan the
home directory. No model call, product edit, install, reload, shared-port call,
or process mutation occurred.

## Decision

Do not reopen the six request-grammar designs as a portfolio. The single-anchor
splice remains below its forward gate, and identity-compressing labels remain
unearned. Do reopen the **strategic opportunity estimate**: future prioritizing
must stratify by repository and work mix instead of applying the circular
corpus's 6% ceiling globally.

The next earned measurement for declaration-first economics is not another
model cohort. It is a higher-provenance owner census that can identify the
remaining 80.6% of changed hunks without guessing. Until that identity exists,
the honest result is HOLD.
