# Retained Flat versus Closed-Relation Admission Audit

Date: 2026-08-29

Audit base: `e0596c98d9ed69479761622bae499bc715a97516`, tree
`5cbfb701f304c6a72582c0e29d7f8d70f8501555`.

Scope: retained evidence only. This audit launched no model, Anvil job,
analyzer, product process, MCP server, install, reload, or mutation.

## Verdict

No retained flat-versus-relation pair is admissible under the current identity
and causal laws. Therefore there is no valid block-one result, no block-two
result, and no pooled `T_emit` or `T_verified` promotion result.

The retained relation cohort still contains a useful descriptive signal. Its
flat midpoint was 65.841 seconds to emit the complete call and its relation
midpoint was 48.912 seconds, 25.7 percent lower. That is not a causal block:
the arms exposed different tool descriptions and schemas, the harness was
capture-only, no exact verifier ran, and flat correctness was established only
by a later product-equivalent replay through a different scorer path.

The closest exact production-flat observations are the two successful alias
treatment runs. Their `T_emit` midpoint was 53.346 seconds. Comparing that
midpoint with the relation midpoint gives only an 8.3 percent descriptive
reduction, below the current 20 percent pooled gate, and is additionally
invalid because candidate, prompt, surface, harness, scorer, and clock outcome
differ.

## Retained authorities

| Evidence | Immutable identity | Role |
|---|---|---|
| 00:28 alias cohort | `clj-surgeon-field-alias-ce05-20260829T071022Z.tar.gz`, SHA-256 `e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f` | Real mutating A-B-B-A field-alias cohort |
| Interrupted alias transfer | `.tar.gz.partial-9978840f`, SHA-256 also `e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f` | Byte-identical duplicate, retained but not independent authority |
| Relation preflight | `three-arm-request-shape-preflight-6328db5-20260829T083955Z.tar.gz`, SHA-256 `30b3bd5cacefc952d7d81f4a22e1ef03ac9578a646ecbdbeac0963e7dddcf545` | Zero-model public-surface and compiler preflight |
| Relation cohort | `6328db5-cohort-20260829T0851Z.tar.gz`, SHA-256 `1af9110d6bbdbe369cdcdf7feee0f70bac78b0f25717a24d937dfe603ecc9d2c` | Capture-only F-A-R-R-A-F model cohort |
| Initial relation interpretation | `d8a80f7c1f447f5c14d1d547ec0785b1abbf0f13`, tree `9d86c737e27e9ea68ffcc47cf16722e953bbbb16`, 02:02 PDT | Incorrectly scored F and A as incomplete |
| Corrected relation interpretation | `3a8f98ac8874a799de96af8195c5baf91c6dc5a1`, tree `7f567b6d8c5f6a8ed5320cae50404880685f1737`, 02:34 PDT | Documents product-equivalent replay and the surface confound; no new model run or archive |

The user's approximate 02:05 and 02:35 labels map to the initial relation
result/design sequence at `d8a80f7`/`61bce42` and the corrected scorer receipt
at `3a8f98a`. Both interpretations point to the same `6328db5` raw cohort.

## Frozen semantic future

All admissibility candidates target the same nine-file future in
`bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/capsule.edn`.
The capsule SHA-256 is
`7d985f4d30acdf871f615b174e0f6c37338539253e6591cf898f96c26f39d4b9`.
The canonical sorted map of file to after-hash has SHA-256
`06f069c095bd559fba8bf66c99443f89f873690a419bfb4b233dc2884e82eb3f`:

```text
src/sample/review_updates.clj          086fc1360d56dff9f0df08f6594337c902b1f1d5851c1c22ea3c3a3c59768675
src/sample/views/log.clj               949c957f5518d1ea453615d6051119b3fb13ec8328a71119d429113c4b15fc40
src/sample/views/people.clj            113c5d105c6915af1b4f779b78aa18a174c6a977d4e89a044fb35cddb7dde202
src/sample/views/review.clj            a68b7584e704db831d156d6650048f5dea8963cd84363aeb21228896b4ada1e8
test/sample/board_test.clj             a599f0d7c6bd9883401e709878b849b0322ac755d21cfddd9b945c9acb148559
test/sample/reviews_test.clj           7a87ba9fded86cde0aea5d9c9bc888408826a440493437875968479531a9da0b
test/sample/status_workflow_test.clj   9e7de3b37c53b5fe421b486a350d176d09e1046ebbf36295f1b1bdddb26e9db8
test/sample/views_test.clj             0e47210a2f3d1d48e684dc32685ce487dd987e1140fd75f5d72a508d883df30f
test/sample/voting_policy_test.clj     0bc8b306e00a3f0dfb77952937c0af0024b181dcfe8b934251a932f0b3d174ba
```

## 00:28 alias cohort

The cohort froze Sol/high, one task prompt with SHA-256
`924dac9fe64e302ca4c006675c2335b6050931b283ec01fa73c6ebe223895a2c`,
and one verifier script with SHA-256
`513a4659f1134d91f93e06f942ad7cab7d569bed06b7c1566acc36782c5852c0`.
Every run used a distinct temporary workspace and private MCP process.

The arms did not share product identity or surface:

- A: product `4904d4ea52c1e1330bc9f8a04c8a5e393af9a758`, tree
  `c47f1cce0b28696acebd29d0e66973621ea79e6e`, edit-item schema SHA-256
  `048368f7ac8fb887da201d0c679690ea62d2e94c2ae1d7f0f01aca718c550c0c`;
- B: product `ce05f6ee099ac029d96ecb6db6f5f225e4239b96`, tree
  `ecec5aebfa0f8adb6d76eeadaf3113ff8aeb7b3d`, edit-item schema SHA-256
  `3888cfe8a5fc61d149e997099fd62f2d4e6cc020ec6661252641d86ab6e1ab88`.

`T_emit` below is recomputed from the retained observer monotonic clock as
`turn.started` to the first `edit_clojure` item start. Process wall comes from
the same `phase-timing.edn`. All four runs emitted an agent message before the
first call, so none satisfies the current no-preamble geometry.

| Run | Arm | `T_emit` | Process wall | Calls | Exact outcome | Failure retained |
|---|---|---:|---:|---:|---|---|
| 01-A | canonical-only flat | 49.809 s | 93.504 s | 2 | Exact after retry | First `old/new` call refused safely |
| 02-B | alias-visible flat | 48.430 s | 59.277 s | 1 | Exact first call | None |
| 03-B | alias-visible flat | 58.262 s | 63.595 s | 1 | Exact first call | None |
| 04-A | canonical-only flat | 42.045 s | 87.800 s | 2 | Incomplete, source unchanged | `old/new`, then `before/after`, both refused safely |

The three successful outcomes share scorer-input SHA-256
`a9eee14ff011d36fd6da201fb6517fc76aac9d9286950eb009666182009ceb22`.
Run 04 has different scorer input because it did not reach the future. The B
runs emitted the ordinary 33-row flat representation. They are not relation
runs and cannot supply an R arm.

## 02:05 relation cohort and 02:35 correction

All six model runs used candidate
`6328db51557bc39ef1a0d40ca171a1ac9873005a`, tree
`7643441141abe042cb48e343c2707f3fa0649c4e`, Sol/high, task SHA-256
`789809060a52d647197cf1fb5ade2cc0a76992209a0223991c7a51179f44d8e1`,
and prompt SHA-256
`90c929528f687a3397c9ed212c888a3fcef0fdce6663e76f430538f8e3d7825a`.
Each run had a distinct project root, private port, PID, and Codex home under
`/srv/fleet/dev-a/clj-surgeon-three-arm-screen-results/6328db5-cohort-20260829T0851Z/`.

The surfaces were arm-specific, not identical:

| Arm | Advertised-surface SHA-256 | Description | Input schema | Top-level request vocabulary |
|---|---|---:|---:|---|
| F flat | `5277ea00c5d2ed25ab68d05d57ec914948fce75bb4bc80c8da576b527c1416ce` | 4,189 B | 4,203 B | `edits`, `delete_owners`, `programs`, `workspace_root` |
| A file groups | `774fc4a9f8988b49eddaf12b5f3c1a694346d5cc936c8d7c5e6bf2b4b5bbe74c` | 4,375 B | 6,524 B | F plus `file_groups` |
| R closed relations | `06b996fecf6ad9d08b60f26b50a2b8a5475890a5a6173dc837aa5243c119348d` | 4,715 B | 6,290 B | F plus `require_change`, `symbol_migration` |

The archive scorer generation is frozen by harness SHA-256
`f171c5d843f6c818461c53311fd34d7961aa8daa168a9da6909caa145f2fa66b`,
scorer SHA-256
`880feae92b5af28b6b40b6c1a32a9aba47fa427b4c1cca8f60f845545074be57`,
and observer SHA-256
`a3b8fc5b8259f14fb5ea0f306c24a013b78e9f8a1989adb1eee1a62c9160bc18`.
That scorer skipped production compact-location normalization. The 02:35
correction replayed raw calls through the public admission, normalizer, and
canonical compiler, but retained no new per-run replay artifact or scorer hash.
It is an immutable docs correction, not a new clock or model cohort.

| Run | Arm | First item | Payload | `T_emit` | Capture wall | Archive score | Corrected product-equivalent result |
|---|---|---|---:|---:|---:|---|---|
| 01 | F | Agent preamble | 6,470 B | 60.775 s | 63.000 s | `change-owner-mismatch` | Exact, 51 edits / 9 files / future equal |
| 02 | A | `edit_clojure` | 5,666 B | 75.125 s | 78.000 s | `change-owner-mismatch` | Exact, 51 edits / 9 files / future equal |
| 03 | R | `edit_clojure` | 2,715 B | 51.518 s | 54.000 s | Exact | Exact, 51 edits / 9 files / future equal |
| 04 | R | Agent preamble | 2,715 B | 46.307 s | 49.000 s | Exact | Exact, 51 edits / 9 files / future equal |
| 05 | A | `edit_clojure` | 5,918 B | 92.281 s | 96.000 s | Public-schema denial | Still refused; no future |
| 06 | F | Agent preamble | 6,470 B | 70.907 s | 74.000 s | `change-owner-mismatch` | Exact, 51 edits / 9 files / future equal |

No failure was dropped. The original F/A false negatives remain recorded, A05
remains a true failure, and the corrected F/A outcomes are labeled as post-hoc
replay rather than substituted into the original scorer generation.

## Admission decisions

### Alias B flat versus relation R

NO-GO. Both arms are exact and target the same future, but they differ in
candidate/tree, prompt bytes, tool surface, harness, scorer, execution mode,
and terminal clock. Alias B performed real mutation and external scoring;
relation R only captured arguments. The four retained calls cannot supply a
common `T_verified`. Every alias run also violates the current no-preamble
first-item law.

Descriptive arithmetic only:

```text
alias B flat T_emit midpoint       53.346 s
relation R T_emit midpoint         48.912 s
apparent reduction                  4.434 s / 8.3%

alias B process-wall midpoint      61.436 s
relation R capture-wall midpoint   51.500 s
apparent reduction                  9.936 s / 16.2%
```

The second comparison mixes verified mutation wall with capture-only wall and
must not be called `T_verified`.

### Relation-cohort F versus R

NO-GO. Candidate, model, prompt, task, future, isolation policy, and observer
generation match. The F-R-R-F subsequence also resembles block one. It is not
admissible because F and R exposed different descriptions and schemas; the
server never mutated or verified; three of four F/R runs had an agent preamble;
and F correctness comes from a later scorer path rather than the scorer bound
to the timed cohort.

Descriptive arithmetic only:

```text
F T_emit midpoint                  65.841 s
R T_emit midpoint                  48.912 s
apparent reduction                 16.929 s / 25.7%
```

This is the signal that earned a new experiment. It is not block-one evidence.

## Both-block and pooled law

The current law requires one immutable candidate and visible surface, exact
real mutation and verification, `N R R N` followed by `R N N R`, and R wins
for both `T_emit` and `T_verified` in each block plus at least 20 percent pooled
improvement for each metric.

The retained portfolio has:

- zero admissible N/R block-one runs;
- zero block-two runs;
- zero relation runs with `T_verified`;
- zero eight-run pooled cohorts; and
- no legal pooled effect to compute.

The correct disposition is to retain the 25.7 percent descriptive
materialization signal, retain the 8.3 percent cross-cohort counter-signal, and
run neither through the promotion formula. The approved future real-mutation
cohort remains the first experiment capable of satisfying `e0596c9`.
