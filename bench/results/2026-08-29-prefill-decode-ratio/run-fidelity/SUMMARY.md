Corpus: 24 adversarial identifiers | model: gpt-5.6-sol | host: anvil-server | loadavg: 2.47 1.60 1.28

| Arm | trials | route-adherent | answers | exact | exact rate | VALID-OTHER (dangerous) | garbage (safe) |
|---|---|---|---|---|---|---|---|
| F | 9/9 | 1.0 | 216 | 216 | 100.0% | 0 | 0 |
| S-echo | 9/9 | 1.0 | 54 | 54 | 100.0% | 0 | 0 |
| S-ord | 9/9 | 1.0 | 54 | 54 | 100.0% | 0 | 0 |

  F: 0 errors in 216 -> true error rate is only bounded BELOW 1.39% (95% upper bound, rule of three). Zero observed is not zero.
  S-echo: 0 errors in 54 -> true error rate is only bounded BELOW 5.56% (95% upper bound, rule of three). Zero observed is not zero.
  S-ord: 0 errors in 54 -> true error rate is only bounded BELOW 5.56% (95% upper bound, rule of three). Zero observed is not zero.

Error rate by characters separating the target from its nearest sibling:
  F: d=1 0/108  d=2 0/27  d=3 0/27  d=11 0/9  d=14 0/18  d=17 0/18  d=19 0/9
  S-echo: d=1 0/22  d=2 0/4  d=3 0/8  d=11 0/2  d=14 0/6  d=17 0/6  d=19 0/6

FAILURE CASES: none. Every answer in every arm was byte-exact.

DESIGN QUESTION: Indistinguishable on this corpus: both encodings were exact and neither produced a wrong-subject.

Confounds:
  - A 24-candidate block. Confusability grows with block size; this does not measure a 500-candidate catalogue.
  - One model at one reasoning effort. A cheaper model or a longer context could transcribe worse.
  - The selection arms use descriptions written by the same author as the corpus, so selection difficulty is controlled but not realistic.
  - An ordinal has almost no garbage failure mode: nearly every wrong number is still a valid number. Ordinal errors therefore land in the dangerous bucket by construction, and raw exactness cannot be compared across encodings without this classification.
  - Exactness here is byte equality after trimming. A downstream resolver that normalises case or separators would mask errors this test counts, and would introduce wrong-subject risk this test does not measure.
