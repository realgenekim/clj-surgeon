# Where the wall goes in REAL coding sessions (Opus reader, event timestamps only; scratch /var/tmp/forge/ethno-real-fx/; 21:1xZ)

Sources: S1 Astra's working Codex session 07:00–20:12Z (13.19 h, 1,182 tool calls, 431 inbound messages); S2–S4 the three largest Claude builder subagent transcripts of this seat (1.36 h / 1.89 h / 4.94 h). All four carry millisecond timestamps; nothing estimated.

| session | wall | largest bucket | second | mutation exec | reading exec | repair |
|---|---|---|---|---|---|---|
| S1 Astra | 47,488 s | blocked on delegated agents / inbound msgs 48.4% (22,973 s) | model generation across 1,182 calls 43.6% | 0.1% | 0.3% | 1.5% |
| S2 builder | 4,903 s | model generation across 845 micro-turns 71.8% | JVM verification 21.7% | ~0.1% | 0.5% | 1.2% |
| S3 builder | 6,791 s | JVM verification 59.0% (4,008 s; single suites 544/231/220/216 s) | generation 33.5% | ~0.1% | 2.2% | 0.6% |
| S4 builder | 17,798 s | idle awaiting the coordinator's next round 67.6% (12,024 s; rounds dispatched one at a time) | JVM verification 15.7% | ~0.1% | 0.4% | 0.1% |

Counts: JVM-launching commands 42 / 35 / 100 / 94 per session; repeated reads of the same file 428 / 28 / 140 / 123 (S1 read the 38-line coordination file 179 times ≈ 45 min of generation at its 14.8 s median gap); failures 54/9/5/8 with corrective retries 44/8/5/7. Tool exec medians 0.2 s / 0.0 s.

## Patterns
1. In every real session the largest bucket is a WAIT for something that is not the machine: coordination round trips (48%, 68%), model generation over many small turns (72%), or JVM suites (59%). Mutation execution is ≤ 0.1% in three of four sessions. The editing debate has been held over ~2% of the clock.
2. Round-trip COUNT, not round-trip cost, sets the bill: wall ≈ calls × per-turn model latency.
3. The JVM is the only real machine cost and is paid over and over; nothing warm is reused.
4. Re-reading the same file is endemic; nearly free in seconds, expensive in turns.
5. Repair is small and honest (0.1–1.5%); agents are not thrashing on their own errors.

## The one mechanism (reader's verdict)
Give every delegated builder a self-clocking work queue (the whole round list at spawn + a next-item file it re-reads itself) and make the coordinator's inbox event-driven (wake on a delegated agent's final answer) instead of polled/nudged. That alone removes S4's 12,024 s idle and S1's 22,973 s blocked: 34,997 s ≈ 9.7 h across two sessions — more than every JVM suite (7,889 s), every read (402 s) and every repair (851 s) in all four combined. Second order: a warm persistent test process kills most of the 7,889 s of verification; a cached per-repo brief removes hundreds of round trips from the generation bucket. Neither is worth doing before the wait is closed.

## Red-lines (Astra 21:06Z, accepted; the reader's percentages are DESCRIPTIVE, not proof of a mechanism)
- A call→result wait on a delegated agent is not automatically idle or waste: the delegated work may be the critical path.
- Inter-call gaps are not identifiable as model generation alone; model stalls/resumes, provider capacity events, and user-idle intervals (Gene asleep, coordinator asleep) need their own classification before any gap is attributed.
- 179 reads of one file do not establish 45 minutes of preventable time; the estimate multiplied a count by a median gap.
- Overlapping fleet intervals cannot be summed into a 9.7 h critical-path saving; the two sessions ran concurrently and each other's waits overlap.
- The work-queue / event-driven-wait hypothesis is worth TESTING (one builder run with a queue file vs one-round-at-a-time, idle measured with the classifications above); the percentages cannot prove it.
