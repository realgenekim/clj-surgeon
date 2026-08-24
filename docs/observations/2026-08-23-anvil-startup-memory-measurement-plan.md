# Anvil startup-memory measurement plan

**Status:** two isolated local canaries completed; Gene's reported 1 GiB OOM
was not reproduced by the minimal Linux profile

## Decision in one page

The shortest useful experiment is not a long heap-size sweep. It is one known
successful tier and one reported failing tier, with the same entry point and
instrumentation from process birth. That experiment ran on this Anvil seat:

| Tier | Result | Ready | Wall to ready | Peak RSS | Heap at ready | NMT committed after ready | GC before ready |
|---|---|---:|---:|---:|---:|---:|---:|
| 2 GiB | **Measured locally: success** | yes | 7 s | 595,208 KiB (581 MiB) | 208 MiB committed, 153 MiB used | 476 MiB | 41 collections |
| 1 GiB | **Measured locally: success** | yes | 7 s | 593,316 KiB (579 MiB) | 179 MiB committed, 48 MiB used | 446 MiB | 44 collections |
| 1.5 GiB | **Not measured here** | unknown | — | — | — | — | — |
| optimized 512 MiB | **Hypothetical target** | unknown | — | — | — | — | — |

The 2 GiB process was terminated five seconds after readiness; its complete
wall time was 12.16 s. The 1 GiB process was handled identically and completed
in 11.99 s. Exit status 143 in both receipts is the intentional `TERM`, not a
failure. No heap dump exists because neither process OOMed.

This result does **not** invalidate Gene's observation. It rejects only the
claim that `-Xmx` by itself explains the difference. This seat used Ubuntu
26.04, OpenJDK 21.0.11, source commit
`14beaf5f0aaac0ab54cae78748a548dc288ea888`, warm dependency caches, and the
HTTP benchmark profile with embedded nREPL disabled. Gene's observation came
from a different host and may include a different JDK, nREPL/CIDER
initialization, project configuration,
classpath, cache state, or service arguments. Those are now the variables to
match before changing the production cap.

The decisive next measurement is therefore a **profile-matched 2 GiB control
on Gene's host**, followed immediately by a 1.5 GiB run using the same JDK,
entry-point arguments, nREPL setting, project config, and cold/warm cache state.
Do not run another four-tier sweep until that pair explains the disagreement.

## What was and was not run

Both probe JVMs were disposable children of a foreground shell in this exact
CWD. They ran sequentially, used unique loopback ports, wrote only below the
evidence directory, and were gone before the next probe began. Host
`MemAvailable` was 24,722 MiB and 24,902 MiB respectively, above the 4 GiB
safety floor. No managed MCP, cclsp, or other seat process was touched.

The measured entry point was the repository's persistent HTTP MCP alias with
`:nrepl-port :none`. That is the smallest real server startup: it loads the
actual application, initializes all MCP tools, binds Jetty, writes the real
readiness file, and then blocks in the normal server join. It does not measure
the extra CIDER/nREPL startup path.

Evidence is in:

- `docs/observations/evidence/startup-memory/xmx-2048m/`
- `docs/observations/evidence/startup-memory/xmx-1024m/`
- `docs/observations/evidence/startup-memory/run-probe.sh`

The probe records the expanded command, wall time, process identity, 200 ms RSS
samples, `/usr/bin/time -v`, GC and safepoint unified logs, JFR from process
birth, NMT snapshots, heap snapshots, an `-all` class histogram, application
logs, telemetry, readiness, and OOM heap-dump configuration. Before retention,
the script removes JFR environment-variable, system-property, and host-process
events and retains only `startup-sanitized.jfr`.

## Exact reproduction

Run only from the canary CWD. The script takes a nonblocking single-probe lock
and refuses when another probe owns it, `MemAvailable` is below 4 GiB, or the
requested port is already listening.

```bash
cd /srv/fleet/dev-c/clj-surgeon-one-shot-canary-20260823

docs/observations/evidence/startup-memory/run-probe.sh \
  xmx-2048m 2048 17888 90

# Confirm the first JVM is gone and re-check the safety floor before proceeding.
free -h
ss -ltn 'sport = :17888'

docs/observations/evidence/startup-memory/run-probe.sh \
  xmx-1024m 1024 17889 90
```

The expanded application command, shown here with `$OUT`, `$PORT`, and `$XMX`
only to keep the line readable, is:

```bash
clojure \
  -J-Xms64m \
  -J-Xmx${XMX}m \
  -J-XX:NativeMemoryTracking=summary \
  -J-XX:+HeapDumpOnOutOfMemoryError \
  -J-XX:HeapDumpPath="$OUT" \
  -J-XX:StartFlightRecording=filename="$OUT/startup.jfr",settings=profile,dumponexit=true,maxsize=256m \
  -J-Xlog:gc\*,safepoint=debug:file="$OUT/gc-safepoint.log":time,uptime,level,tags:filecount=1 \
  -X:clj-surgeon/mcp \
  :project-dir '"/srv/fleet/dev-c/clj-surgeon-one-shot-canary-20260823"' \
  :port "$PORT" \
  :telemetry :full \
  :telemetry-dir "\"$OUT/telemetry\"" \
  :run-id '"startup-memory-probe"' \
  :nrepl-port :none \
  :ready-file "\"$OUT/ready.edn\"" \
  :log-file "\"$OUT/application.log\""
```

The literal, shell-escaped command for each completed run is preserved in its
`probe-metadata.txt`; use that file when exact bytes matter.

To reproduce the production/nREPL path on Gene's host, change only these
arguments and keep every other probe control identical:

```text
:nrepl-port 0
:port-file "<the same probe output directory>/nrepl-port"
```

Also record `java -version`, `clojure -Sdescribe`, the exact git SHA, the
effective project config hash, and whether Maven/git dependency caches were
cold. Those facts were not recorded in the original observation and are now
necessary comparison keys.

## Expected evidence and interpretation gates

### Gate A: genuine transient heap OOM

Accept “startup exceeds this heap tier” only when all of these agree:

1. no readiness file was written;
2. stderr or the GC log contains `OutOfMemoryError` or an allocation/evacuation
   failure;
3. `java_pid*.hprof` exists, or the log explains why dumping failed;
4. JFR and the final GC cycles show heap occupancy converging on the cap; and
5. after-GC occupancy is interpreted separately from pre-GC occupancy.

If after-GC occupancy stays well below the cap but allocation still fails,
inspect humongous allocation, evacuation failure, promotion failure, GC locker,
and native allocation evidence. Do not call all such failures “the live set is
too large.”

### Gate B: timeout without OOM

If the 90 s deadline fires with no OOM, the result is a startup timeout, not a
heap-cap failure. Use safepoint, thread, CPU, file, and socket evidence to find
the wait. Preserve `heap-deadline.txt`, `nmt-deadline.txt`, and the class
histogram, then terminate only the exact disposable PID.

### Gate C: failing tier reaches readiness

This is what happened locally at 1 GiB. The reported failure is not reproduced.
Compare JDK, OS, classpath, args, config, nREPL, cache warmth, and commit before
trying a smaller cap. A successful reduced tier is not permission to ship it:
startup must still be repeated under a representative cold cache and followed
by representative requests.

### Gate D: instrumentation changes the answer

JFR `profile`, NMT, class histograms, and heap dumps add overhead. The class
histogram is deliberately taken only after readiness and uses `-all` so it does
not request a full GC. Run one confirmation without the histogram and with JFR
`default` only after the instrumented pair locates the peak. If that control
changes pass/fail, report the observer effect rather than averaging it away.

## What the local evidence says

Startup is an allocation storm with a much smaller after-GC population.

- The 2 GiB run performed 42 collections in 12 s: 39 young and three old. The
  maximum recorded pre-GC heap was 154.6 MiB; that cycle retained 39.4 MiB.
- The 1 GiB run performed 44 collections: 41 young and three old. Its maximum
  recorded pre-GC heap was 141.0 MiB; the final startup cycle retained 47.4
  MiB.
- At the post-readiness snapshot, NMT reported 476 MiB committed for the 2 GiB
  process and 446 MiB for the 1 GiB process. Heap was only 208 MiB and 179 MiB
  of those totals. Metaspace, class space, GC structures, JIT code, compiler
  arenas, JFR tracing, thread stacks, symbols, and modules are outside the
  Java-heap used number.
- Peak RSS was effectively unchanged by halving `Xmx`: about 580 MiB in both
  runs. `Xmx` reserves an address-space ceiling; it neither precommits all of
  that space nor caps native memory.
- The 2 GiB `-all` histogram captured 39.1 MiB of byte arrays, 12.7 MiB of
  object arrays, 10.9 MiB of reflection `Method` objects, 4.9 MiB of Clojure
  `MapEntry`, and many transient sequences/maps. In the 1 GiB run, a young GC
  occurred immediately before the histogram, so the same categories were far
  smaller. The difference is collection timing, not evidence that the 2 GiB
  configuration retains eight times more application state.
- JFR's sampled allocation leaders include reflection method copies, byte-array
  copies, Clojure map/vector construction, lazy sequences, Clojure compiler
  analysis, ASM bytecode structures, stack traces, and locks. These point to
  class/namespace loading and tool construction, not one known application
  cache.
- The longest observed GC pause was 8.69 ms at 2 GiB and 10.1 ms at 1 GiB.
  Collection frequency increased slightly at the smaller cap, while readiness
  did not move in this sample.

The “heap at ready” values are momentary. Five seconds later, the 2 GiB heap
still contained about 153 MiB because no collection was needed; during shutdown
the next young GC reduced it to about 52 MiB. This is the precise mechanism by
which low settled RSS or a small after-GC live set can coexist with a dangerous
earlier peak.

## Startup allocation storyboard

Internal phase timing is **hypothetical** until markers are added. The 7 s
readiness point, GC sawteeth, heap sizes, RSS, NMT, and allocation samples below
are measured on this seat.

```text
time --->  0s                  2s                  4s                  6s   7s
           process birth       config + requires   tool construction   bind READY
           [ phase timing HYPOTHESIS; only process birth and READY are observed ]

allocation
rate       ▁▆████▇▆▇████▇▆████▇▆████▇▆████▇▆██▇▆▅▃▁
           JFR samples: reflection, arrays, maps/sequences, Compiler + ASM

used heap  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\       sawtooth is MEASURED
           ^ allocate quickly        ^ young GC repeatedly reclaims garbage
           2 GiB max before GC: 154.6 MiB -> 39.4 MiB after that collection
           1 GiB max before GC: 141.0 MiB -> 47.4 MiB after final startup GC

live-ish   8 MiB ---- 15 MiB ---- 23 MiB ---- 33 MiB ---- 44-48 MiB
heap       [after-GC occupancy; MEASURED, but includes survivors not proven live]

garbage    | 29 | 43 | 73 | 96 | 113 | 115 MiB awaiting the next collection
awaiting   [difference between selected pre/post-GC values; MEASURED]

native +   class metadata + JIT + GC structures + arenas + JFR + stacks
non-heap   ===================== grows toward 446-476 MiB committed =============

RSS        40 MiB ---> 300 MiB ---> 475 MiB ---> about 580 MiB at/after READY
           [MEASURED peak; includes resident heap and native/file-backed pages]

Xmx cap    1 GiB  --------------------------------------------------------------
           2 GiB  ------------------------------------------------------------------------
           [both well above this local profile's measured heap peak]
```

The failure story to test on Gene's host is:

```text
                maximum Java heap
                      |
                      v
  live objects + garbage not collected yet + evacuation/promotion reserve
  [==========] [=========================] [=======================]
  settled need       transient backlog            collector room
                      ^
                      allocation burst arrives faster than GC can create room

  If the sum crosses the usable heap boundary, allocation can OOM here.
  Later, on a successful larger heap, GC removes the backlog and the process
  settles small. The later low RSS does not retroactively erase the peak.

  Native memory sits beside this diagram, outside Xmx:
  metaspace + code cache + threads + GC bookkeeping + JFR + libraries + arenas
```

At 1.5 GiB, the current status is **Gene-reported likely OOM, not measured in
this canary**. At 512 MiB, the only honest status is **hypothetical optimized
path**. Reaching 512 MiB requires phase-attributed evidence and a reduction in
the causal allocation/retention site, not merely a smaller flag.

## Exact phase markers to add later

No production code was changed in this phase. Add one low-allocation JFR event
plus a single stderr line at each boundary below. Each event should contain a
monotonic timestamp, phase name, heap used/committed/max, non-heap used, loaded
class count, thread count, and process RSS when cheaply available. Use a fixed
enum or static strings so the markers do not become a new allocation source.

In `clj-surgeon.mcp-http-server/start` and `start-http-server!`, place markers:

1. `startup.begin`: immediately on entry to `start`, before
   `(start-http-server! opts)`.
2. `config.begin`: after normalizing `project-dir`, before
   `read-project-config`.
3. `config.end`: after `resolve-verification-profiles`.
4. `telemetry.begin` / `telemetry.end`: around `telemetry/start!`.
5. `nrepl.begin` / `nrepl.end` / `nrepl.skipped`: around
   `start-embedded-nrepl!`.
6. `tools.begin` / `tools.end`: immediately around `mcp-tool/init!`. This is
   the most important first split because all tools are constructed here.
7. `transport.begin` / `transport.end`: around transport construction,
   `McpServer/async`, specification configuration, `.build`, and
   `register-live-server!`.
8. `jetty.begin`: before creating/configuring `Server` and servlet context.
9. `bind.begin` / `bind.end`: immediately around `(.start jetty)`.
10. `ready.file`: immediately after `write-ready-file!`.
11. `startup.ready`: after the existing `:server.start` telemetry emit and
    before returning the running server map.

If `tools.begin` to `tools.end` owns the peak, add a second-level marker inside
`mcp-tool/init!` around config/materialization, handler registry construction,
schema generation, and formatter/verification setup. Do not instrument every
namespace require until the coarse split proves that it is necessary.

## Minimal decision sequence from here

1. Reproduce on Gene's host with the exact production profile at 2 GiB and
   1.5 GiB, in that order only if memory remains above 4 GiB between runs.
2. If 1.5 GiB OOMs, use its heap dump, last successful phase marker, final
   after-GC occupancy, and allocation sites to name one causal phase.
3. If 1.5 GiB succeeds, repeat the reported 1 GiB tier once with the exact same
   profile. A second success closes the old observation as environment/profile
   specific; a failure brackets the boundary.
4. Optimize only the named phase, then test 1 GiB. Test 512 MiB only after 1
   GiB is repeatably green under cold and warm startup plus representative
   requests.

The standalone visual companion is
`docs/observations/2026-08-23-anvil-startup-memory-storyboard.html`.
