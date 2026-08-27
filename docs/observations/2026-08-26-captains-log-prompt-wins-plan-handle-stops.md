# Captain's log: the prompt won; the plan handle stopped

Date: 2026-08-26

Issue `clj-surgeon-90l` tested two independent ways to shorten recovery from a
hallucinated top-level owner:

```text
                      CURRENT RESULT       COMPACT PLAN_ID
                    +------------------+------------------+
  CURRENT PROMPT    | A: baseline      | C: shape only    |
                    +------------------+------------------+
  DECISION PROMPT   | B: prompt only   | D: both          |
                    +------------------+------------------+
```

The learning cohort used eight fresh `gpt-5.6-sol` sessions at high reasoning.
All eight runs were correct. Every run made exactly two MCP calls. No run wrote
source.

## Learning result

| Stratum and cell | Wall | Refusal-to-retry | Native search | Decision boundary |
|---|---:|---:|---:|---:|
| Easy rank 1 A | 30.543 s | 13.870 s | 1 | 3,230 bytes |
| Easy rank 1 B | 34.227 s | 9.857 s | 0 | 3,231 bytes |
| Easy rank 1 C | 46.295 s | 15.856 s | 1 | 1,632 bytes |
| Easy rank 1 D | 32.887 s | 9.860 s | 0 | 1,631 bytes |
| Hard rank 7 A | 32.817 s | 7.387 s | 0 | 7,868 bytes |
| Hard rank 7 B | 27.918 s | 7.088 s | 0 | 7,873 bytes |
| Hard rank 7 C | 38.291 s | 13.556 s | 0 | 3,745 bytes |
| Hard rank 7 D | 26.062 s | 7.129 s | 0 | 3,745 bytes |

The decision boundary is the first completed refusal result plus the second
MCP request. This is the model-visible surface that the experiment intended to
shrink. Aggregate result bytes were rejected because they also count the
identical successful source response after recovery.

The 162-byte decision prompt had the strongest causal signal:

- On rank 1, A to B cut deliberation by 4.013 seconds and removed the native
  search action.
- On rank 7, C to D cut deliberation by 6.427 seconds.
- The prompt main effect was -5.005 seconds on rank 1 and -3.363 seconds on
  rank 7.

The compact `plan_id` shape reduced boundary bytes but did not independently
reduce deliberation:

- Rank 1 fell from 3,230 to 1,631 bytes, a 49.5% reduction.
- Rank 7 fell from 7,868 to 3,745 bytes, a 52.4% reduction.
- On rank 7 with the decision prompt, B and D deliberation was effectively the
  same: 7.088 and 7.129 seconds.
- Shape-only cell C preserved the ambiguity. It still used one native search
  on rank 1 and had the slowest deliberation in both strata.

Wall time remained noisy because fresh-session startup and model execution
dominated the subsecond MCP calls. The factorial therefore uses the
refusal-to-retry interval for the primary mechanism claim.

## Stop decision

Do not run the 32-session release cohort and do not promote the compact handle.
The learning cohort falsified two frozen release gates:

- Refusal-to-retry median was 7.088 to 15.856 seconds; the gate was at most
  three seconds.
- Compact boundary reduction was 49.5% to 52.4%; the gate was at least 60%.

The stale-plan transaction seam has a permanent adversarial test. It refuses
after a corrected read when the current file hash differs from the retained
hash, reports both hashes, preserves the source, and retains the plan for an
explicit restart. The cold MCP classpath test passed with six assertions.

## What survived

The prompt treatment is the earned finding. Complete vocabulary is necessary
for a rank-7 recovery, but the model also needs explicit authority semantics:

> All listed owners are real snapshot evidence; ranking is non-authoritative,
> semantic selection among them is allowed, and the exact retry verifies the
> selection.

This converts “a suggestion is not authority” from an open-ended warning into
a bounded decision procedure. The tool still verifies the exact selection.
The model no longer needs to prove the candidate again with `rg`.

The compact handle remains useful experimental evidence, not product code. A
future handle must remove another 16% to 21% of its current boundary without
dropping the complete owner vocabulary or weakening snapshot authority.

## Relation to the earlier native-recovery comparison

The earlier 16-session selector benchmark compared complete owner evidence
with a sparse refusal followed by native `rg` or file reads:

| Stratum | Sparse refusal plus native recovery | Complete owner evidence | Effect |
|---|---:|---:|---:|
| Easy rank 1 | 22.785 s | 26.314 s | 15.5% slower |
| Hard rank 7 | 25.293 s | 18.527 s | 1.37x faster |
| Combined | 24.499 s | 23.924 s | 1.02x; nearly flat |

Levenshtein ranking was therefore not a universal speedup. Its durable value
was recovery completeness on hard misses: five native discovery actions became
zero. The new factorial explains part of the crossover. Evidence alone is not
enough; the response must tell the model how to use non-authoritative evidence
safely.

## Immutable evidence

- Harness head before measurement: `eb31930e1c2dcf38ba97509036c7cda589494001`
- Learning result directory:
  `/tmp/clj-surgeon-selector-factorial-learning-eb31930`
- Eight correct raw `events.jsonl` transcripts are retained under that
  directory.
- Corrected byte scorer and stale-plan test are the next receipt commit.
- Shared MCP ports `7888` and `7890` were not touched.
