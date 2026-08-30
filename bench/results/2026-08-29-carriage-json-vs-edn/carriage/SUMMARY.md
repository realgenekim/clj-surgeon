Model: gpt-5.6-sol | host: anvil-server | loadavg at start: 1.24 1.28 1.21 | fixtures: 10 | replicates: 3
PREDECLARED prediction: EDN malformed rate <= JSON malformed rate
PREDECLARED kill criterion: EDN malformed > 2x JSON kills the idea

| Arm | trials | route-adherent | malformed | rate | excl. fence-only | payload wrong | median out bytes | median out tokens |
|---|---|---|---|---|---|---|---|---|
| J | 30 | 1.0 | 0 | 0.0% | 0.0% | 0 | 168.0 | 64.0 |
| E-wrapped | 30 | 1.0 | 0 | 0.0% | 0.0% | 1 | 197.0 | 84.0 |
| E-raw | 30 | 1.0 | 0 | 0.0% | 0.0% | 0 | 164.0 | 66.0 |

  J: 0 malformed in 30 -> true rate bounded below 10.0% (95%, rule of three). Zero observed is not zero.
  E-wrapped: 0 malformed in 30 -> true rate bounded below 10.0% (95%, rule of three). Zero observed is not zero.
  E-raw: 0 malformed in 30 -> true rate bounded below 10.0% (95%, rule of three). Zero observed is not zero.

MALFORMATION KINDS (the mechanism, not just the rate):
  J: none
  E-wrapped: none
  E-raw: none

COMPARISON VS JSON:
  E-wrapped: rate 0.0% vs JSON 0.0% | ratio n/a | prediction held: true | KILL triggered: false
      extra retry turns per 100 requests: 0.0 | bytes delta 29.0 | tokens delta 20.0
  E-raw: rate 0.0% vs JSON 0.0% | ratio n/a | prediction held: true | KILL triggered: false
      extra retry turns per 100 requests: 0.0 | bytes delta -4.0 | tokens delta 2.0

FAILURES: none in any arm.

PARSED BUT PAYLOAD WRONG (worse than a parse failure — this one executes):
  [r01-unicode-and-escapes-E-wrapped] task=unicode-and-escapes
    expected: "(def labels\n  {:ok \"\\u2713 done\" :warn \"caf\\u00e9 \\u2014 retry\" :tab \"a\\tb\"})"
    got:      "(def labels\n  {:ok \"✓ done\" :warn \"café — retry\" :tab \"a\tb\"})"

RETRY TURN COST: n/a ms median
TURNS PER 100 REQUESTS: {"J" 0.0, "E-wrapped" 0.0, "E-raw" 0.0}

VERDICT: NO DIFFERENCE DETECTABLE AT THIS n. Every arm produced zero malformed requests (J n=30, E-wrapped n=30, E-raw n=30). The true rates are bounded only below the rule-of-three values in each arm; this run cannot distinguish them and does not justify a migration on robustness grounds.

Confounds:
  - The model was asked to EMIT a request as text, not to call a tool. A real tool call is constrained by a schema the harness cannot reproduce here, so absolute rates are not the production rates; the COMPARISON between carriages is what transfers.
  - E-wrapped still pays JSON escaping for the one string it occupies, so it is not a clean EDN arm. That is why E-raw exists: E-wrapped minus E-raw is how much the JSON envelope gives back.
  - Ten fixtures chosen to be adversarial. They over-represent hazards relative to real write traffic, which inflates absolute malformed rates in every arm and is intended to.
  - One model at one reasoning effort. A weaker model is exactly where a carriage difference would appear, and this run does not test one.
  - A code fence is counted as malformed because a parser rejects it, but it is reported separately since it is arguably a prompt-format failure rather than a carriage failure.
  - Rates near zero cannot be separated at this n. Rule-of-three bounds are given so the reader can see what the run can and cannot support.
