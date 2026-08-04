# Clean Codex benchmark summary

Correctness is a gate. Efficiency medians include only correct runs. Token counts are the final cumulative usage reported by each Codex session.

| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | File changes | Source output | Skill read | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| case-edit | no-skill | native | 4 | 0 | 0% | 0% | —ms | — | — | — | — | — | —B | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 50% | 0% | 0% |
| named-form | no-skill | native | 4 | 4 | 100% | 100% | 23,949ms | 41,379 | 5,430 | 676 | 2 | 0 | 2,322B | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | — |
| semantic-form | no-skill | native | 4 | 4 | 100% | 100% | 27,347ms | 56,778 | 7,936 | 876 | 3 | 0 | 4,244B | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 50% | 0% | — |
| structural-find | no-skill | native | 4 | 4 | 100% | 75% | 40,627ms | 56,285 | 9,050 | 1,348 | 3 | 0 | 844B | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 75% | 0% | — |
