# Clean Codex benchmark summary

Correctness is a gate. Efficiency medians include only correct runs. Token counts are the final cumulative usage reported by each Codex session.

| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | File changes | Source output | Skill read | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| case-edit | matched-skill | post | 4 | 4 | 100% | 100% | 52,832ms | 116,114 | 17,295 | 1,928 | 8 | 0 | 10,542B | 100% | 0% | 50% | 0% | 100% | 100% | 25% | 0% | 75% | 100% |
| case-edit | matched-skill | pre | 4 | 4 | 100% | 100% | 53,502ms | 176,294 | 24,784 | 1,763 | 8 | 0 | 37,443B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 100% |
| named-form | matched-skill | post | 4 | 4 | 100% | 100% | 22,709ms | 43,971 | 8,387 | 727 | 2 | 0 | 1,461B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 100% | — |
| named-form | matched-skill | pre | 4 | 4 | 100% | 100% | 28,792ms | 72,000 | 10,176 | 891 | 4 | 0 | 6,720B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | — |
| semantic-form | matched-skill | post | 4 | 4 | 100% | 100% | 28,384ms | 44,016 | 6,896 | 798 | 2 | 0 | 1,531B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 100% | — |
| semantic-form | matched-skill | pre | 4 | 4 | 100% | 100% | 39,444ms | 82,806 | 13,430 | 1,141 | 5 | 0 | 7,883B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | — |
| structural-find | matched-skill | post | 4 | 4 | 100% | 50% | 24,222ms | 43,672 | 6,645 | 665 | 2 | 0 | 596B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | — |
| structural-find | matched-skill | pre | 4 | 4 | 100% | 75% | 35,959ms | 76,213 | 13,401 | 887 | 4 | 0 | 663B | 100% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | 0% | — |
