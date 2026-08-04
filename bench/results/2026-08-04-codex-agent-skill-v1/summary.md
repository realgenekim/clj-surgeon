# Clean Codex benchmark summary

Correctness is a gate. Efficiency medians include only correct runs. Token counts are the final cumulative usage reported by each Codex session.

| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | File changes | Source output | Skill read | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| case-edit | matched-skill | post | 1 | 1 | 100% | 100% | 59,374ms | 98,762 | 14,282 | 1,698 | 8 | 0 | 17,568B | 100% | 0% | 0% | 0% | 100% | 100% | 0% | 0% | 0% | 100% |
| ops-registry-xray | matched-skill | post | 1 | 1 | 100% | 100% | 29,404ms | 60,942 | 12,814 | 1,048 | 3 | 0 | 2,835B | 100% | 0% | 100% | 0% | 0% | 100% | 0% | 0% | 0% | — |
