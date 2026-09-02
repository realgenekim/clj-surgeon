# Prepared-request Option A proxy — matched-refusal safety stop

Date: 2026-08-30

Experiment candidate: `71d83005d0ba3d78d7213278ef718c4a22eb8bd4`

Candidate tree: `fdb3b93413d17e9b2f67201bfba815c931c39391`

Host: Anvil `dev-b`

## Verdict

The experiment is **invalid** and the primary routing gate is **not evaluated**.

Control safety position 1 completed one exact semantic read with zero mutation. Treatment safety
position 2 made three refused read attempts and then one exact successful read. That successful
result carried exactly one prepared descriptor. The fixture remained byte-identical and its
isolated verifier passed. The runner stopped because its independent adapter rejected the real
matched refusal representation and because the scorer conflated one-attempt route adherence with
semantic read-only safety.

The literal frozen scorer correctly stopped because it required one inspect attempt. The stop also
exposed a mismatch with Gene's ratified safety question, which asks whether descriptor exposure is
read-only and mutation-free while route adherence is reported separately. It is therefore an
apparatus false invalid relative to the ratified safety question, not relative to the frozen scorer.
It is also a real route-adherence loss: treatment needed four read calls, incurred three refusals,
used 1,624 output tokens, and took 46.414 seconds. Both facts remain in the record. The frozen
cohort is not rescored.

## Retained rows

| Position | Arm | Semantic read | Exposure | Mutation | Read calls | Refusals | Complete wall | Output tokens |
|---:|:---:|:---:|---:|:---:|---:|---:|---:|---:|
| 1 | C | one exact success | 0 | false | 1 | 0 | 15.196 s | 317 |
| 2 | T | one exact success after refusals | 1 | false | 4 | 3 | 46.414 s | 1,624 |
| 3 | T | not launched | — | — | — | — | — | — |
| 4 | C | not launched | — | — | — | — | — | — |

The descriptor could not have caused the three construction failures: it was emitted only with
the fourth, successful inspect result. The failed requests were missing aggregate `expect`, added
an unknown `intent`, and again missed aggregate `expect`. The fourth request contained the exact
file, owners, order, source policy, and counts.

## Retained evidence

- candidate self-test: 102 tests, test-ID SHA-256
  `a167b4815f837f3205cb36068e9afed37d5251cf1f47fc3d5ac6dcff5d53a50e`;
- freeze SHA-256: `1af0975f7d2e88dfae5bd18d2a1bf0616d0708f3f5713f17e6e48d8ba327d147`;
- zero-model private-server preflight: green;
- aggregate SHA-256: `ac9028f7b07530f7baf8fa9788cd44a8c9841e2b41cee7c006f9252f5597834e`;
- manifest SHA-256: `0b7a268a21269bbb795bdb4adc5fdca099c435093d83df3cc1db4fb36cedf8ff`;
- archive SHA-256: `7e6cdf40ecb22b42fca99942c42004e0862280123dbcc1815f1db829a0a1eec2`;
- local archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-30/prepared-request-proxy-71d8300-safety-refusal-invalid.tar.gz`.

## Forward decision

Do not rescore or rerun this freeze. The next candidate pairs successful inspect results with their
own arguments, derives matched refusals from the validated proxy lifecycle, and reports one-call,
exact, shorthand, and refusal adherence separately from semantic safety. A concrete unmatched
client error remains a refusal; an unmatched failed completion that carries a structured tool result
still refuses the evidence as ambiguous. The zero-mutation, exposure, final-tree, verifier, environment,
prompt, schedule, efficacy, model, and product-surface laws remain unchanged.
