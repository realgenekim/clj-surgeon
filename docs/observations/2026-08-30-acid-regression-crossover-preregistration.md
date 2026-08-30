# Acid regression gate and crossover ladder: frozen preregistration

Date: 2026-08-30

Status: frozen before model launch

## Governing doctrine and immutable product

This experiment executes the acid-test doctrine from `CLAUDE.md` at
`8539e7ea4ed9df8dc1e51b057ba58f9760e2a28e`: matched serial Anvil comparisons,
one exact product commit, identical task semantics, and the same semantic scorer
for both live arms.

The product under test is exactly
`c55de2279826af5ed21c90981591479dd2e802b2`. The benchmark harness commit and
fixture hashes are recorded before launch and are not product substitutions.

Every run uses the `gpt-5.6-sol` subscription route at high reasoning on Anvil
`dev-a`. Runs are serial. A capacity and identity fence immediately precedes
the first paid model call. Any resolved model other than `gpt-5.6-sol`, any
non-`dev-a` subject, any wrong task/fixture/product identity, or any changed
preregistered input stops the cohort.

## Part A: installed-normalization regression gate

Replay the canonical historical `sessionize-format-extraction` Surgeon route
twice. The route is the fused first-action `apply_clojure_changes` transaction
with the project-owned exact verifier and terminal relay. It moves 15 forms and
preserves 63 remaining caller occurrences. Do not repay the native arms; reuse
the README's retained matched-native receipts by doctrine.

Primary comparison: the two-run Surgeon median and range against the README
Sol/high high-water of 21.446 seconds. The retained Fable/high 14.325-second
result is reported as a caller-specific reference, not pooled with Sol. The
51-edit compact range of 55.763--61.354 seconds is a second historical
reference, but this gate does not claim to replay that decision unless the
exact one-shot relation harness is available unchanged.

Gate law: a median above 23.591 seconds, more than 10 percent slower than
21.446 seconds, is a candidate install regression and must be reported loudly.
Correctness, exact verification, route adherence, first-action application,
terminal relay, emitted output tokens, action count, and one-shot rate are all
reported. A wrong-subject count above zero invalidates the gate.

## Part B: crossover ladder

The fixture family is one prefix-stable realistic feature migration. It renames
`acid-crossover.flags/legacy-mode?` to `command-mode?`, updates all resolved
references, and updates associated `:legacy-mode` keywords and `"legacy-mode"`
wire literals. Historical comments and strings containing the old name are
decoys that must remain unchanged. The prompt supplies neither full source nor
the affected file/owner/site inventory; both arms discover it.

The four rungs are:

| Rung | Exact changes | Changed files | Added mechanical surface |
|---|---:|---:|---|
| 03 | 3 | 1 | definition, keyword, wire literal |
| 08 | 8 | 2 | first resolved-reference owner |
| 16 | 16 | 4 | two more dispersed reference owners |
| 32 | 32 | 6 | two high-repetition reference owners |

The Surgeon arm follows the repo's exploratory rule: one batched structural
discovery snapshot followed by one complete `apply_clojure_changes` mutation.
The native arm uses native source discovery plus `apply_patch`. Both use the
same hidden before/after capsule and the same semantic, exact-byte, source-set,
and route scorer.

Each rung has two live pairs (`n=2` per arm), serial and interleaved. The fixed
orders are:

```text
03  S N | N S
08  N S | S N
16  S N | N S
32  N S | S N
```

No failed or ugly position is retried or removed. Any invalid position remains
in the denominator. Nothing is tuned after the first model call.

## Predictions frozen before calls

The known anchors are native winning the two-file/three-change small class and
Surgeon winning the 51-edit/nine-file chord by 4.944x. This family uses one file
at the smallest rung, so only the mechanical-count direction transfers.

Preregistered directional predictions:

| Rung | Prediction |
|---|---|
| 03 | native wins clearly |
| 08 | native still wins, but by less |
| 16 | first likely Surgeon win; near the crossover |
| 32 | Surgeon wins clearly |

The falsifiable monotonicity expectation is that the per-rung ratio
`native median wall / Surgeon median wall` does not decrease as exact change
count rises. A direction reversal is reported, not smoothed away.

## Metrics, crossover estimator, and uncertainty

Per arm and rung report complete wall seconds, emitted output tokens, action
count, one-shot rate, semantic correctness, exact correctness, source-set
exactness, and route adherence. `Action count` is the harness's observed tool
round trips; discovery and mutation actions remain charged. `One-shot` means
one successful mutation attempt with zero failed mutation actions.

The primary crossover estimate is the interval between the largest measured
rung whose paired-arm median favors native and the smallest measured rung whose
paired-arm median favors Surgeon. If those rungs are adjacent, report that
change-count band. If no crossing occurs or the direction is nonmonotone,
report only the observed bound or disjoint ambiguity. With two observations per
arm per rung, do not claim a stable percentile or narrow confidence interval;
show both raw observations and call the estimate coarse.

`wrong-subject` counts a run that changes a history decoy, misses the declared
feature subject while mutating another subject, or changes any source outside
the accepted target set. The required result is zero across both parts.

## Retention and replay

Each arm retains `events.jsonl`, `event-clock.tsv`, prompt, stderr, target diff,
start/final hashes, terminal receipt, and the aggregate `runs.tsv`. After each
part, write a sorted SHA-256 manifest, archive the raw directory without
deleting it, hash the archive, copy the manifest and archive back to the control
machine, and verify both hashes. The observation committed after each part
records the remote path, archive hash, manifest hash, exact replay command,
harness commit, product commit, and scorer identity.

Part A replay shape:

```bash
BENCH_MODEL=gpt-5.6-sol BENCH_REASONING=high \
BENCH_POST_COMMIT=c55de2279826af5ed21c90981591479dd2e802b2 \
BENCH_RUN_MATRIX='mcp:mcp-extraction-fused-tool-first-no-skill' \
BENCH_TASKS=sessionize-format-extraction BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=2 BENCH_PARALLELISM=1 BENCH_RETENTION=local \
BENCH_SANDBOX_MODE=danger-full-access \
BENCH_MCP_JAVA_OPTS='-J-Xms64m -J-Xmx512m' \
BENCH_RESULT_DIR=/absolute/result/path make benchmark-edit-portfolio
```

Part B replay shape:

```bash
bash bench/run_anvil_acid_crossover_ladder.sh \
  /absolute/result/path \
  c55de2279826af5ed21c90981591479dd2e802b2
```
