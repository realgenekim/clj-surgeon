# Astra prototype validation

19 meaningful checks PASS in selftest.log: interval overlap union vs sum; declared
sequential dependency duration; unknown/open/self/cyclic dependencies; duplicate/
reversed/clock/task/end corruption; process end without close; real exit7; real
nonexistent-command terminal; concurrent append; external brackets; overwrite
refusal. Public begin/run/mark/end/report smoke also passed. No providers/JVMs.
The exact60s timeout was not exercised; escaped process groups are not contained.

External bracket code0 means the bracket CLOSED, not the enclosed edit succeeded.
Its interpretation requires the actual external action result and proof receipt.
Node spawn is observed subprocess launch, not separately measured OS fork/exec.
Report exposes observed spawn-to-close separately from the larger run bracket;
provider token/JVM-ready/service presence/fork-exec separation remain UNKNOWN.

The code is frozen while root hand-drives the real task; no changes were made to
trace.js after the first public smoke handoff. Tests do not alter the task ledger.
No measured task speedup, causal savings, or complete descendant census is claimed.
