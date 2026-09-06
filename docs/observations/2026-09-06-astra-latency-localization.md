# Astra — retained latency localization
Read-only artifact analysis; no providers, cohorts, services, timing runs or existing Clojure source reads.

| Observation | Command wall | Localized timing | Arithmetic remainder |
|---|---:|---|---:|
| Productive M1 apply | 7.079935s | mission-apply event 1.636s | 5.443935s outside that event |
| Stale M2 refusal | 5.025183s | mission-apply refusal event 0.024s | 5.001183s outside that event |
| Raw T1 | 8.332298s | setup0.033982 + plan0.252 + apply2.519 | 5.527316s |
| Raw T2 | 7.330592s | setup0.033609 + plan0.251 + apply1.535 | 5.510983s |
| Raw T3 | 7.980176s | setup0.033722 + plan0.221 + apply2.228 | 5.497454s |
| Raw T4 | 7.279503s | setup0.033523 + plan0.230 + apply1.571 | 5.444980s |

M1's finer receipt interval is1.457790s, NOT its1.636s outer apply event. Within it, formatter reports0.467852s; winner gate0.022314s + witness0.015194s. Event verify47ms and source commit16ms are nested work, not extra terms to add to apply. Candidate2 was unparseable, then candidate0 passed; this was not an all-first-candidate success.
Raw receipt intervals range1.505676–2.486374s; formatter0.454240–1.016209s, winning proof command sums0.032646–0.043256s. Plan events0.221–0.252s are directly localized; raw proof work is small relative to unassigned command time. Parallel request durations must not be summed into wall or treated as a serial decomposition.

Evidence: M1/stale `apply-wall.json`, `stale-M2-wall.json`, `apply.edn`, `events-after-stale.txt`; raw `results.jsonl`, per-T `setup.json`, `usage-events.jsonl`. M1 event values are retained rounded monitor readings, not higher-resolution event source. Raw event durations are integer milliseconds. Arithmetic remainders inherit those precision limits.

What this supports: about5.0–5.5s of these commands is outside recorded apply, or outside setup+plan+apply for run. A stale refusal still pays ~5s without candidate generation/format/proof; those cannot explain most of its wall. The pattern makes launcher/runtime initialization a sensible hypothesis, not an assigned measurement.
What it does NOT locate: JVM startup versus class/namespace loading, launcher/slot overhead, ledger/config loading outside event boundaries, scheduling/GC, stdout serialization, process shutdown. M1 includes slot; raw has a Python/setup wrapper; M1/stale versus raw also use different actions/engines/load. Subtracting the stale command from M1 would not isolate generation overhead. The receipt and event intervals are not interchangeable, and event timestamps alone are not a complete critical-path trace.

Smallest next real-user observation: at the next already-needed propose/apply (no repeated mutation), retain outer start/end and a one-shot process sample/trace with monotonic markers for Java exec, entry to the CLI after namespace loading, plan/apply entry/exit, and final response flush/exit; correlate with the existing proof/format events. No persistent service and no provider retries. One real stale refusal can serve if no edit is needed. This distinguishes pre-handler loading from measured planner/proof and post-handler tail; splitting JVM bootstrap from namespace loading needs that additional boundary, not another opaque end-to-end timer.
Decision now: favor batching an already-decided run over avoidable separate JVM entrances, but do not promise a measured startup saving or optimize an unassigned component from these records alone.
