# Step 1: blocked before matched suites

Subject `617288424c62979308dca74f83db5e0102f6905c`; base checkout `eae1e43280635be9b4e317e176d29476fd358702` verified clean at
`target/base-checkout/`. Manifest `8eb28c1ea8260fd2d34b2d0b5a7f3381184b2f1f0a141468e1c537ab70d4cb42`.
Recorded UTC: 2026-09-12T06:39:34.055950+00:00.

The prior base-checkout and lease blockers are resolved. The one-suite lease
interpretation is accepted. No new clone or worktree registration was attempted.

The sealed environment injects `_JAVA_OPTIONS=-Xmx1024m
-Djava.io.tmpdir=<packet>/tmp` at `/home/forge/bin/lib/packet.py:162`.
A real JVM launched with `-Xmx512m` and a distinct private temp flag reports:

```text
   size_t MaxHeapSize                              = 1073741824                                {product} {command line}
    java.io.tmpdir = /var/tmp/forge/packets/ad7e0a55-9a1f-4363-b5b7-1a8f5899f1c6/tmp
```

The command, clocks and complete output are in
[jvm-effective-options.json](jvm-effective-options.json) and
[jvm-effective-options.log](jvm-effective-options.log). Both diagnostic JVMs
ran sequentially, exited zero, and had explicit maximum heaps. These are
launcher diagnostics, not namespace timing measurements.

The inherited `_JAVA_OPTIONS` wins over the worker argv. The child environment
in `test/clj_surgeon/tmp_leak_support.clj:285` does not replace it. The
re-exec's private `java.io.tmpdir` flag at line 185 therefore cannot take effect;
`secure-tmpdir!` compares the actual directory with the private sentinel at
line 349 and would refuse the mismatch before running tests. This last consequence
is source-derived; no suite refusal is claimed as executed evidence.

| Arm | Summed namespace walls | Coordinator makespan |
|---|---|---|
| (a) Base coordinator, original 61 | Unmeasured | Unmeasured |
| (b) Subject, bb children disabled, same 61 | Unmeasured | Unmeasured |
| (c) Subject as shipped | Unmeasured | Unmeasured |

The ten largest (a)/(c) deltas are unmeasured. The slowdown cause remains unknown;
concurrent bb/JVM jobs in the shipped plan are a candidate, not a measured cause.
No historical wall is presented as a fresh comparison.

Authority to unblock: Fable or Gene must approve a common process-local launch
environment or reseal it. The concrete requested correction is
`_JAVA_OPTIONS=-Xmx512m`, with no temp-directory override there, so each launcher
can supply its private startup path. It has not been applied. Revalidate effective
heap and private directory before arms a/b/c, retaining the same corrected
launch conditions across them. Do not change membership or lane budgets.
