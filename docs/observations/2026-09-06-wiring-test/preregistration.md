# Wiring test (preregistered 2026-09-06T21:21:02.280370235Z, before any marker): queue file (all rounds at spawn) vs one-round-at-a-time dispatch
Arms: Q = one Sonnet subagent given all four rounds at spawn (self-clocking); R = one Sonnet subagent given round 1 at spawn, rounds 2–4 dispatched by the coordinator (this Fable seat) only after each DONE notification arrives — the coordinator's own dispatch latency IS the mechanism under test (it includes harness notification delivery + the coordinator being busy with other lanes).
Task: four trivial edits on a scratch copy of the maven seed (docstring/comment/new ns/def change), identical in both arms.
Measures (from markers.log timestamps written by the agents): per-arm wall ROUND 1 START → ALL DONE / ROUND 4 DONE; per-round work (START→DONE); per-gap wait (DONE n → START n+1). Hypothesis: R's inter-round waits dominate its wall (each ≥ the coordinator's notification-to-dispatch latency), Q's are sub-second; falsifier: R's summed waits < 2× Q's.
Caveat: n = 1 per arm; Sonnet actors; the coordinator is a busy Fable main loop, so R's waits measure THIS harness's dispatch path, not a general constant.
Dispatch log: Q launched 2026-09-06T21:21:02.280370235Z (all rounds); R round 1 launched 2026-09-06T21:21:02.280370235Z.
R round 2 dispatched 2026-09-06T21:21:14.204226048Z (R round 1 DONE 21:20:57.588Z)
R round 3 dispatched 2026-09-06T21:22:01.111103116Z (R round 2 DONE 21:21:15.519Z)
R round 4 dispatched 2026-09-06T21:22:13.298569021Z (R round 3 DONE 21:21:51.561Z)
