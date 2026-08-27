# Captain's Log: Selector evidence has a crossover

Date: 2026-08-26 Pacific time
Issue: `clj-surgeon-wjz`
Next experiment: `clj-surgeon-90l`
Code Directory reminder: `cdc-976`

## Outcome

Complete owner vocabulary materially improved one difficult owner-recovery
mission. It did not improve the easy mission. The aggregate result was nearly
flat because the two effects canceled.

The feature is a conditional win, not a general wall-time win.

```text
EASY RANK 1

PRE   refusal with correct short hint -----> sometimes retry directly
POST  complete evidence -------------------> Sol often runs rg anyway

       PRE 22.785 s  <  POST 26.314 s

HARD RANK 7

PRE   wrong short hint -----> rg/read -----> exact retry
POST  all 31 owners -----------------------> exact retry

       POST 18.527 s  <  PRE 25.293 s
```

## Frozen experiment

The benchmark ran 16 fresh `gpt-5.6-sol` sessions at high reasoning effort:

- two frozen tasks;
- PRE and POST arms;
- four replicates per task and arm;
- counterbalanced route order;
- serial execution;
- identical prompts, source snapshot, shell access, and `inspect_clojure`
  exposure;
- read-only sandboxes and isolated MCP servers;
- no shared `:7888` restart or configuration change.

PRE used commit `d4f1adeeae1064ecde517b07535fa66eee768d1a`.
POST used the final focused mechanism at `c8d0e4c`. A separate live probe proved
that stable publication `66b8a606e44786bb8a835d7bcd79fc3da3c15afc`
returns the same six-owner and 31-owner evidence shapes.

Correctness was intentionally narrow. Each event trace had to prove:

1. the first call used the frozen wrong owner;
2. the second call used the frozen semantic target;
3. the second call returned `read_complete=true`;
4. the route used exactly two inspect calls;
5. the model emitted the same selected owner and successful receipt state.

Historical output bytes and response formatting were telemetry, not correctness
gates. All 16 sessions passed.

## Results

| Stratum | Arm | Correct | Median complete wall | Median refusal-to-retry | Native discovery | Median input tokens | Median result bytes |
|---|---|---:|---:|---:|---:|---:|---:|
| easy rank 1 | PRE | 4/4 | 22.785 s | 7.240 s | 2 calls | 73,211 | 4,592 |
| easy rank 1 | POST | 4/4 | 26.314 s | 11.759 s | 4 calls | 83,939 | 6,295 |
| hard rank 7 | PRE | 4/4 | 25.293 s | 10.151 s | 5 calls | 83,670 | 5,339 |
| hard rank 7 | POST | 4/4 | 18.527 s | 5.046 s | 0 calls | 67,667 | 11,186 |
| combined | PRE | 8/8 | 24.499 s | 9.670 s | 7 calls | 82,023 | 4,964 |
| combined | POST | 8/8 | 23.924 s | 8.696 s | 4 calls | 75,763 | 8,740 |

On the hard rank-7 case, POST was 1.37 times as fast as PRE. Complete wall time
fell 26.8%. Refusal-to-retry time fell 50.3%, from 10.151 to 5.046 seconds.
Native discovery fell from five calls to zero. Median input tokens fell 19.1%.
The paired median improvement was 6.555 seconds of complete wall and 5.299
seconds of decision time.

On the easy rank-1 case, POST was 15.5% slower. Refusal-to-retry time increased
62.4%. Native discovery increased from two calls to four. The paired median
cost was 2.598 seconds of complete wall and 4.217 seconds of decision time.

The combined median moved only from 24.499 to 23.924 seconds: 2.35% lower, or
1.02 times as fast. That aggregate is not the useful conclusion. The crossover
is the result.

## What happened

The PRE refusal already supplied the correct short hint for the easy typo:

```text
you may have meant resolve-source-path (hint only)
```

The POST refusal supplied the same rank-1 hypothesis, all six real owners, and
explicit `authority=false`. The benchmark prompt also said, "Do not guess."
Sol often interpreted that combination as a requirement to run `rg` before the
exact retry. More evidence increased caution instead of removing a call.

The hard case exposed the real option value. PRE showed eight lexical
candidates that did not contain the semantic target. Every PRE run needed
native discovery. POST showed all 31 real owners. The correct owner was rank 7,
but Sol found it from the complete vocabulary and retried without source
discovery in every replicate.

Levenshtein ranking was not the hard-case authority. It was a presentation
index into real snapshot evidence. The model supplied semantic judgment. The
second exact read supplied authority.

## Product decision

Keep complete owner vocabulary and non-authoritative ranking. They pay for
themselves when the likely answer is outside the old bounded hint list. Do not
claim that Levenshtein makes every owner recovery faster.

The next hill is the decision contract between the refusal and the model:

```text
Current implied rule
  hypothesis only + do not guess
       -> search for more evidence

Candidate explicit rule
  every listed name is a real owner from this snapshot
  ranking is not authority
  semantic selection is allowed
  the next exact read verifies the selection
       -> choose one exact owner and retry
```

This distinction preserves safety. It does not grant fuzzy authority. It tells
the model that exact retry is the verification step, so native search is not
required only to prove that a listed owner exists.

Issue `clj-surgeon-90l` owns the frozen 2 by 2 experiment:

```text
                      CURRENT RESULT       COMPACT PLAN_ID
                    +------------------+------------------+
  CURRENT PROMPT    | A: baseline      | C: shape only    |
                    +------------------+------------------+
  DECISION PROMPT   | B: prompt only   | D: both          |
                    +------------------+------------------+
```

The rank-1 loss and rank-7 win must remain separate strata. A prompt or compact
handle earns adoption only if it retains the hard-case win and removes the
easy-case verification detour.

## Evidence

- Harness commit: `52407618853a1a882646e803340661e4154a85f4`
- Structured receipt:
  `bench/results/2026-08-26-selector-recovery-release/receipt.edn`
- Structured runs:
  `bench/results/2026-08-26-selector-recovery-release/runs.tsv`
- Structured summary:
  `bench/results/2026-08-26-selector-recovery-release/summary.json`
- Raw archive SHA-256:
  `6621671d6d99a15035c7736d91baa4565638a7e2af2c29e875b21ae512133d51`
