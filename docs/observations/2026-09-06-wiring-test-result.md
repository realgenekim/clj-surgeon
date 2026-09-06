# Wiring test result — queue file (all rounds at spawn) vs one-round-at-a-time coordinator dispatch (21:20–21:22Z; preregistered before any marker; receipts in docs/observations/2026-09-06-wiring-test/)

| arm | wall (round 1 START → round 4 DONE) | work (Σ START→DONE) | inter-round waits | Σ waits |
|---|---|---|---|---|
| Q: one Sonnet subagent given all four rounds at spawn | 16.6 s | 8.2 s | 2.3 / 2.7 / 3.4 s | 8.4 s |
| R: one Sonnet subagent, rounds 2–4 dispatched by the coordinator on each DONE notification | 79.9 s | 9.8 s | 14.2 / 36.0 / 19.8 s | 70.1 s |
Ratio R/Q on wall 4.8x; Σ waits 8.3x. Hypothesis (R's summed waits ≥ 2× Q's) MET.

What the waits are: the harness's notification-to-dispatch path plus the coordinator (this Fable seat) being busy with other lanes between rounds — answering Gene and Astra — which is the real operating condition of a delegated builder tonight. Work time was equal within noise.
Limits: n = 1 per arm; trivial edits (no JVM, no tests); Sonnet actors; a single busy coordinator; the R waits are THIS harness's dispatch latency, not a constant. It is enough to say the mechanism is real and cheap to remove: hand a builder its whole list and a next-item file; wake the coordinator on final answers instead of polling.
Doctrine consequence (proposed, Gene's call): "one round at a time" dispatch to builders is a measured 5x tax on trivial rounds; the default becomes the queue file, with the coordinator reviewing at the end or at named checkpoints the builder can read itself.

## Control caveats (Astra 21:23Z, accepted)
- The withheld rounds were pre-authorized and independent, so arm R compares a prepared queue with ARTIFICIALLY withheld known work; a native coordinator can supply the same list at spawn. This result therefore measures the dispatch-path latency for work that could have been pre-listed — it does NOT show a win for rounds that genuinely depend on findings or review, where release timestamps would be identical and review time belongs in task wall.
- The measured quantity is ready-but-undispatched time (DONE n → START n+1), not "all inter-round time". Breakdown for R (ready = DONE; start = the builder's own START marker; my dispatch timestamps were recorded after the send returned, so they bound dispatch from above): ready→start 14.2 s / 36.0 s / 19.8 s. Q's ready→start 2.3 / 2.7 / 3.4 s is the builder's own turn latency with no coordinator in the path.
- Not recorded: overlap spans and presence of the coordinator (it was busy with two other lanes; presence unknown in the log). A repeat should stamp ready→dispatch→start→finish separately and the coordinator's own busy/idle state.
- The claim survives only in this form: when work can be pre-listed, one-at-a-time dispatch through a busy coordinator adds tens of seconds of ready-but-undispatched time per round on this harness; pre-listing removes it.
