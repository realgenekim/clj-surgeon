# Clean Codex benchmark summary

Correctness is a gate. Efficiency medians include only correct runs. Token counts are the final cumulative usage reported by each Codex session.

| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | File changes | Source output | Skill read | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ops-registry-xray | matched-skill | post | 1 | 1 | 100% | 100% | 29,280ms | 61,155 | 8,931 | 1,102 | 3 | 0 | 2,956B | 100% | 0% | 100% | 0% | 0% | 100% | 0% | 0% | 0% | — |
| pair-view-edit | matched-skill | post | 1 | 1 | 100% | 100% | 31,973ms | 79,525 | 10,149 | 951 | 4 | 0 | 1,627B | 100% | 0% | 0% | 0% | 100% | 100% | 100% | 0% | 0% | 100% |
