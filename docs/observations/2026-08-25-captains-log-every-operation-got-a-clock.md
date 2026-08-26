# Captain's Log: Every Operation Got a Clock

The visible request sounded tiny: display elapsed time on every MCP operation.
The implementation revealed that a clock is useful only when the word
“operation” has one enforceable meaning.

Before this work, each handler could choose when to measure, which branches to
decorate, and how to summarize the result. Ordinary inspection already showed
time, but prepared reads, asynchronous verification, exact edits, computed
transforms, and safe refusals did not share one law. Adding four local timers
would have preserved that ambiguity.

We instead built one public operation envelope. It executes the handler once,
measures server-owned time once, attaches the authoritative numeric
`elapsed_ms` before serialization or callbacks, and lets the handler render
its domain-specific summary from that same value. A canonical registry names
every outcome each public operation may produce. A new operation cannot enter
the registry without declaring and testing its outcomes.

Linked-Intent Development made the cross-cutting promise concrete. Twenty-three
active intent statements now have both implementation and test witnesses. The
retained Prolog oracle earned its keep by finding two distinctions the original
native contract blurred: pending verification was capable of satisfying a
completion-shaped witness, and failed verification had no separately stated
clock law. Those were model defects, not merely missing assertions.

The cold gate passed 213 tests and 1,751 assertions at a 512 MiB maximum heap.
Then the live server told the more persuasive story:

| Production call | Outcome | Server time |
|---|---:|---:|
| bounded `inspect_clojure` | exact form returned | 71.29 ms |
| malformed guarded edit | safe pre-write refusal | 71.34 ms |
| corrected guarded edit | atomic commit and read-back | 83.96 ms |

The refusal mattered as much as the success. Both carried the same timing law,
and the first attempt changed no source. The second attempt performed the whole
guarded mutation in one server interaction.

Dogfooding found one adjacent production defect. A test server could temporarily
replace the live registration, then erase the outer server during teardown; a
nested registration stack now restores it. The subsequent controlled restart
found a second race: launchd ownership could disappear before the old health
endpoint drained. Startup now requires health, readiness evidence, and launchd
ownership to agree, and refuses to launch a competing JVM until the old owner
and endpoint are both gone.

The breakthrough is not the timer itself. The timer forced the four MCP tools
to become one coherent instrument at their public boundary. That gives humans
honest performance evidence, gives agents uniform terminal evidence, and gives
future optimization work a stable place to measure whether structural tooling
actually beats native editing.
