# Clean Codex benchmark summary

Correctness is a gate. Token counts are the final cumulative usage reported by each Codex session.

| Task | Context | Version | n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | Source output | Skill read | show-form | Separate plan/apply |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| case-edit | compact-skill | post | 1 | 100% | 100% | 69,067ms | 196,010 | 21,930 | 2,340 | 13 | 9,792B | 100% | 100% | 100% |
| case-edit | compact-v2-skill | post | 1 | 100% | 100% | 44,152ms | 124,374 | 15,830 | 1,658 | 8 | 9,729B | 100% | 100% | 100% |
| case-edit | explicit-no-skill | post | 1 | 100% | 100% | 60,158ms | 161,355 | 20,555 | 2,061 | 11 | 10,091B | 0% | 100% | 100% |
| case-edit | explicit-no-skill | pre | 1 | 0% | 0% | 39,550ms | 118,642 | 21,106 | 1,433 | 6 | 25,433B | 0% | 0% | 0% |
| case-edit | matched-skill | post | 1 | 100% | 100% | 43,592ms | 136,604 | 19,868 | 1,523 | 9 | 9,473B | 100% | 100% | 100% |
| case-edit | matched-skill | pre | 1 | 100% | 100% | 57,517ms | 188,529 | 23,665 | 2,210 | 11 | 33,954B | 100% | 0% | 100% |
| case-edit | no-skill | post | 1 | 100% | 100% | 52,769ms | 131,889 | 17,201 | 1,964 | 6 | 9,069B | 0% | 0% | 0% |
| case-edit | no-skill | pre | 1 | 0% | 0% | 39,519ms | 91,091 | 10,707 | 1,329 | 4 | 7,999B | 0% | 0% | 0% |
| named-form | compact-skill | post | 1 | 100% | 100% | 52,149ms | 87,836 | 8,476 | 1,947 | 5 | 1,609B | 100% | 100% | — |
| named-form | compact-v2-skill | post | 1 | 100% | 100% | 33,094ms | 42,473 | 6,377 | 745 | 2 | 1,695B | 100% | 100% | — |
| named-form | explicit-no-skill | post | 1 | 100% | 100% | 22,587ms | 42,630 | 5,510 | 730 | 2 | 1,695B | 0% | 100% | — |
| named-form | explicit-no-skill | pre | 1 | 100% | 100% | 43,923ms | 87,526 | 8,166 | 1,287 | 5 | 1,576B | 0% | 0% | — |
| named-form | matched-skill | post | 1 | 100% | 100% | 24,751ms | 45,917 | 7,773 | 794 | 2 | 1,695B | 100% | 100% | — |
| named-form | matched-skill | pre | 1 | 100% | 100% | 26,581ms | 64,361 | 9,065 | 892 | 3 | 6,775B | 100% | 0% | — |
| named-form | no-skill | post | 1 | 100% | 100% | 21,253ms | 41,179 | 8,155 | 697 | 2 | 1,799B | 0% | 0% | — |
| named-form | no-skill | pre | 1 | 100% | 100% | 23,158ms | 41,380 | 5,284 | 690 | 2 | 1,799B | 0% | 0% | — |
| semantic-form | compact-skill | post | 1 | 100% | 100% | 53,401ms | 119,864 | 14,392 | 2,081 | 7 | 2,076B | 100% | 100% | — |
| semantic-form | compact-v2-skill | post | 1 | 100% | 100% | 34,759ms | 56,731 | 6,555 | 873 | 3 | 1,738B | 100% | 100% | — |
| semantic-form | explicit-no-skill | post | 1 | 100% | 100% | 24,974ms | 42,869 | 5,749 | 884 | 2 | 4,501B | 0% | 100% | — |
| semantic-form | explicit-no-skill | pre | 1 | 100% | 100% | 63,825ms | 107,550 | 17,182 | 2,617 | 6 | 3,503B | 0% | 0% | — |
| semantic-form | matched-skill | post | 1 | 100% | 100% | 27,180ms | 45,970 | 7,826 | 911 | 3 | 1,738B | 100% | 100% | — |
| semantic-form | matched-skill | pre | 1 | 100% | 100% | 36,356ms | 65,773 | 10,477 | 1,085 | 4 | 6,971B | 100% | 0% | — |
| semantic-form | no-skill | post | 1 | 100% | 100% | 28,370ms | 56,848 | 6,672 | 918 | 3 | 4,679B | 0% | 0% | — |
| semantic-form | no-skill | pre | 1 | 100% | 100% | 40,817ms | 70,908 | 7,676 | 1,308 | 4 | 4,045B | 0% | 0% | — |
| structural-find | compact-skill | post | 1 | 100% | 0% | 30,614ms | 72,734 | 12,574 | 809 | 4 | 538B | 100% | 0% | — |
| structural-find | compact-v2-skill | post | 1 | 100% | 100% | 26,376ms | 42,246 | 6,150 | 571 | 2 | 538B | 100% | 0% | — |
| structural-find | explicit-no-skill | post | 1 | 100% | 100% | 20,923ms | 57,584 | 10,480 | 651 | 3 | 538B | 0% | 0% | — |
| structural-find | explicit-no-skill | pre | 1 | 100% | 0% | 24,241ms | 57,146 | 5,946 | 729 | 3 | 538B | 0% | 0% | — |
| structural-find | matched-skill | post | 1 | 100% | 0% | 22,559ms | 45,600 | 7,456 | 631 | 2 | 538B | 100% | 0% | — |
| structural-find | matched-skill | pre | 1 | 100% | 100% | 26,597ms | 60,770 | 12,642 | 758 | 3 | 538B | 100% | 0% | — |
| structural-find | no-skill | post | 1 | 100% | 0% | 41,047ms | 86,850 | 9,538 | 1,053 | 5 | 538B | 0% | 0% | — |
| structural-find | no-skill | pre | 1 | 100% | 100% | 48,121ms | 85,230 | 12,014 | 1,748 | 5 | 1,233B | 0% | 0% | — |
