# Step 1: admission blocked before measurement

Subject: `d4736b46e016b8244b4db0eea0519ae62768a622`.
Base: `eae1e43280635be9b4e317e176d29476fd358702`.

The corrected 61-member premise is accepted. No fresh a/b/c arm was launched,
so summed namespace walls, makespans and the ten largest deltas are unknown.
No slowdown cause is inferred from the historical receipts alone.

The frozen packet permits one live JVM, explicitly including coordinators.
The base Makefile launches a JVM coordinator. Its worker command launches a
second JVM while the coordinator waits. Moreover, the worker calls
`secure-tmpdir!`, which starts a nested isolation JVM and waits on `@proc`.
This last behavior is present at both base and subject. With no reexec
sentinel in the launch environment, even one subject worker under a bb
coordinator needs at least two live JVMs. The base needs at least three.
These are source-derived lower bounds, not observed peak measurements.

`GATE_MEMAVAIL_MIB` can reduce worker width, but it does not remove waiting
JVM parents. `--debug-serial` also retains the child topology and marks the
run SERIAL/NOT-A-GATE. Running the base coordinator under bb and redesigning
isolation bootstrap would change the measurement protocol; neither is a
measurement of the frozen launch as given.

Evidence and numbered source excerpts: [admission-evidence.edn](admission-evidence.edn).
The historical base sum is mechanically re-folded there, explicitly labeled
historical. This is not arm a and is not paired with a fresh subject wall.

Stop authority: Fable, the packet predicate “more than one JVM at a time.”
Fable must authorize and reseal a sufficient peak allowance or a revised
matched launcher protocol that preserves isolation with one live JVM.
No throwaway measurement checkout was created, no source was changed, no
budget or classification was altered, and no new bb ceiling was invented.

Steps 2–3 remain unexecuted: there is no measured cause to fix and no step-2
bb sum from which to derive a ceiling.
