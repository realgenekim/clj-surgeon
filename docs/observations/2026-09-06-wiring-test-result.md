# Wiring test result — queue file (all rounds at spawn) vs one-round-at-a-time coordinator dispatch (21:20–21:22Z; preregistered before any marker; receipts in docs/observations/2026-09-06-wiring-test/)

| arm | wall (round 1 START → round 4 DONE) | work (Σ START→DONE) | inter-round waits | Σ waits |
|---|---|---|---|---|
| Q: one Sonnet subagent given all four rounds at spawn | 16.6 s | 8.2 s | 2.3 / 2.7 / 3.4 s | 8.4 s |
| R: one Sonnet subagent, rounds 2–4 dispatched by the coordinator on each DONE notification | 79.9 s | 9.8 s | 14.2 / 36.0 / 19.8 s | 70.1 s |
Ratio R/Q on wall 4.8x; Σ waits 8.3x. Hypothesis (R's summed waits ≥ 2× Q's) MET.

What the waits are: the harness's notification-to-dispatch path plus the coordinator (this Fable seat) being busy with other lanes between rounds — answering Gene and Astra — which is the real operating condition of a delegated builder tonight. Work time was equal within noise.
Limits: n = 1 per arm; trivial edits (no JVM, no tests); Sonnet actors; a single busy coordinator; the R waits are THIS harness's dispatch latency, not a constant. It is enough to say the mechanism is real and cheap to remove: hand a builder its whole list and a next-item file; wake the coordinator on final answers instead of polling.
Doctrine consequence (proposed, Gene's call): "one round at a time" dispatch to builders is a measured 5x tax on trivial rounds; the default becomes the queue file, with the coordinator reviewing at the end or at named checkpoints the builder can read itself.
