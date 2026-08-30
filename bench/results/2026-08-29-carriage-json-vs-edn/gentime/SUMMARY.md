Model: gpt-5.6-sol | host: anvil-server | loadavg: 0.99 1.32 1.31 | payload: 48 lines / 1546 bytes of real Clojure

| Arm | n | parse fails | payload exact | median wall | MAD | decoded tok | out bytes |
|---|---|---|---|---|---|---|---|
| C | 9 | 0 | 0 | 3996.0 ms | 431.0 ms | 5.0 | 2.0 |
| J | 9 | 0 | 7 | 14226.0 ms | 419.0 ms | 590.0 | 1755.0 |
| E | 9 | 0 | 8 | 15144.0 ms | 642.0 ms | 650.0 | 1751.0 |

FLOOR: 3996.0 ms (MAD 431.0, n=9)

  J: 585.0 marginal tokens in 10230.0 ms -> 17.4872 ms/token (57.2 tok/s)
  E: 645.0 marginal tokens in 11148.0 ms -> 17.2837 ms/token (57.9 tok/s)

THE TWO RATIOS (EDN over JSON) — never collapse these:
  token count      : 1.1026
  generation time  : 1.0897
  ms per token     : 0.9884
  output bytes     : 0.9977
  difference resolved above noise: false

VERDICT: NOT RESOLVED AT THIS n. The J/E decode-time difference (918.0 ms) does not exceed the combined spread (2122.0 ms). Report as parity NOT ESTABLISHED rather than as parity, and note that token count ratio is 1.1026 while time ratio is 1.0897.

Confounds:
  - Both arms carry byte-identical Clojure; only the carriage differs. That is the intended isolation, but it also means this measures ONE payload, not a corpus. Content-dependent effects are not sampled.
  - Time is wall clock from process launch, so it includes network and the fixed per-turn floor. The floor is subtracted using a contemporaneous C arm, but server-side queueing is invisible and lands in every arm.
  - Token counts come from the provider usage report, which is the same source the decision uses for billing.
  - Reasoning tokens are included in decoded tokens because they are produced serially and cost time, but they are not part of the carriage and add variance unrelated to format.
  - One model, one reasoning effort, one hour. A different model could tokenize EDN quite differently.
  - This does not reproduce the corpus-wide token screen; it tests whether TIME tracks COUNT on a representative write. A divergence here invalidates the proxy, it does not by itself re-price the corpus.
