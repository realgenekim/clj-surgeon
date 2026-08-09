# Memory usage and process topology

This document records the memory investigation from 2026-08-09 on a 24 GiB
Apple Silicon Mac. Use it to identify Clojure processes, distinguish expected
services from stale processes, and select the next memory mitigation.

## Summary

The primary problem was process duplication and missing heap limits. It was not
one intrinsically large clj-surgeon process.

- The first sample contained 56 JVMs.
- Thirty-one JVMs were stale `itrev-mcp-server` stdio MCP processes.
- Fourteen long-lived JVMs were per-agent `clojure-mcp` assistants.
- The original clj-surgeon HTTP MCP had a 6 GiB ergonomic heap limit and a
  3.8 GiB physical footprint.
- A restart with `-Xms64m -Xmx2g` reduced the clj-surgeon footprint to
  approximately 408 MiB. Its used heap was approximately 131 MiB.
- That 408 MiB sample still used the unintended GraalVM CE 22.3/JDK 19
  runtime. A later launch selected OpenJDK HotSpot 26 through the operator's
  Java environment.
- A 1 GiB clj-surgeon heap limit did not reach readiness within the production
  startup window. The 2 GiB limit did reach readiness.
- After the requested cleanup, 20 long-lived JVMs remained.

RSS did not show the full pressure. macOS had compressed or paged out many JVM
pages. `ps` reported low RSS while `footprint` reported several hundred MiB per
idle JVM.

## Terms

Use these terms consistently:

- **RSS** is the process resident set at the sample time. RSS can fall when
  macOS compresses or pages out inactive memory.
- **Physical footprint** is the macOS process-accounting value from
  `footprint -p PID`. It includes compressed private memory and is more useful
  than RSS during memory pressure.
- **Used heap** is live or not-yet-collected Java heap data.
- **Committed heap** is heap capacity that the JVM obtained from the operating
  system. It can be much larger than the used heap.
- **Maximum heap** is an upper bound. It is not the process footprint limit.
  Thread stacks, class metadata, code, direct buffers, and native libraries can
  use memory outside the Java heap.

Do not treat a sum of process footprints as an exact machine total. Processes
can share mapped pages. Use the sum only to rank process families.

## Current long-lived JVMs

The post-cleanup sample contained 20 long-lived JVMs:

| Directory | Count | Purpose | Approximate footprint |
|---|---:|---|---:|
| `code-directory-cluster` | 7 | Code Director app, one nREPL, five agent MCPs | 2.62 GiB |
| `social-media-writer` | 4 | SMW app, one nREPL, two agent MCPs | 1.80 GiB |
| `clj-surgeon` | 3 | Shared HTTP MCP and two agent MCPs | 1.00 GiB |
| `mothership` | 2 | Mothership app and one agent MCP | 1.02 GiB |
| `sessionize-sched-killer` | 3 | Scheduler app, one nREPL, one agent MCP | 0.92 GiB |
| `reddit-scraper-fulcro/server2` | 1 | One agent MCP | 0.30 GiB |

The table contains mission-critical application processes. Do not terminate a
process only because its footprint is large. First identify the owner, command,
and working directory.

The first live runtime check after cleanup found 17 OpenJDK HotSpot 25 JVMs,
one OpenJDK HotSpot 26 JVM, and two GraalVM CE 22.3/JDK 19 JVMs. The two
GraalVM JVMs were the SMW and Mothership applications. After their Makefile
launch contracts were fixed and both apps were restarted, the distribution was
17 HotSpot 25, three HotSpot 26, and zero GraalVM JVMs. Existing processes do
not change runtimes when a version-manager symlink changes; restart and inspect
the live VM before declaring a migration complete.

## What separate JVM processes share

Using the same JDK in several processes does not create one shared JVM. Every
process still owns a separate Java heap, garbage collector, thread set, class
metadata area, JIT code cache, and application state. One process cannot use
another process's free heap capacity.

macOS can share identical read-only, file-backed pages for the Java executable,
native libraries, JAR files, and filesystem cache. HotSpot also enables base
Class Data Sharing by default on modern JDKs. The packaged CDS archive is
memory-mapped so read-only metadata for core classes can be shared across JVM
processes that use the same compatible archive. See the
[Oracle CDS guide](https://docs.oracle.com/en/java/javase/11/vm/class-data-sharing.html)
and [OpenJDK AppCDS design](https://openjdk.org/jeps/310).

These savings are useful but much smaller than eliminating duplicate
processes. AppCDS can archive additional application class metadata, but it
does not share mutable Clojure heaps, caches, indexes, or JIT state. Measure it
only after process lifecycle and heap limits are under control.

The highest-leverage topology is one multi-workspace service JVM with multiple
clients, where the application deliberately shares its heap, loaded classes,
and caches. That is the clj-surgeon HTTP MCP design. The tradeoff is a larger
failure and GC domain, so the shared process needs request isolation, bounded
caches, health checks, and an explicit heap policy.

## Why code-directory-cluster had five agent MCPs

The generic `clojure-mcp` integration uses stdio. Each client connection starts
one server process. The server process is a JVM.

The 2026-08-09 process tree contained these code-directory MCP processes:

| MCP PID | Parent Codex PID | Relationship |
|---:|---:|---|
| 2067 | 25100 | Direct child |
| 7277 | 25100 | Direct child |
| 9690 | 25100 | Direct child |
| 25271 | 25100 | Direct child |
| 41068 | 41043 | Direct child of a second Codex session |

Parentage proves that one Codex root owned four simultaneous MCP JVMs and a
second Codex root owned the fifth. Parentage does not prove whether the four
connections came from subagents, app-server connections, reconnects, or client
retries. The lifecycle requires a separate trace before assigning one cause.

These processes are not copies of the shared clj-surgeon HTTP MCP. The
clj-surgeon service uses one launchd-managed JVM on port 7888. Its workspace is
request data, so it does not require one JVM per repository or agent.

## Expected topology

The clj-surgeon stack must use this topology:

```text
Codex and Claude clients
          |
          v
clj-surgeon HTTP MCP :7888       one shared JVM
          |
          v
cclsp HTTP MCP :7890             one shared Bun process
          |
          v
clojure-lsp                      one lazy native child per active workspace
```

`clojure-lsp` is a native executable, not a JVM. Native compilation does not
guarantee a small steady-state footprint. Two observed `clojure-lsp` processes
used approximately 344 MiB and 683 MiB.

The generic `clojure-mcp` integration currently has a different topology:

```text
one agent or client connection
          |
          v
one stdio clojure-mcp JVM
```

Multiple clients therefore multiply the JVM baseline, loaded classes, heap,
and native libraries.

## Heap policy

The launch command for the shared clj-surgeon MCP must contain:

```text
-Xms64m -Xmx2g
```

The repository supplies these flags as `-J-Xms64m -J-Xmx2g` to the Clojure
CLI. The repository is Java-distribution and version-manager neutral. By
default, `make` selects `JAVA_HOME/bin/java` when `JAVA_HOME` is set and
otherwise selects `java` from its inherited `PATH`. An operator can set
`MCP_JAVA_HOME` or `MCP_JAVA_CMD` explicitly. The launch command passes that
selected home and executable to the Clojure CLI.

On the machine involved in this incident, SDKMAN owns Java selection. Run
`make mcp-start` from an environment initialized by SDKMAN, or pass the
already-selected generic Java home without encoding SDKMAN's location in this
repository:

```bash
MCP_JAVA_HOME="$JAVA_HOME" make mcp-start
```

Verify the live process instead of trusting the launch configuration:

```bash
pid=$(launchctl list | awk '$3=="com.realgenekim.clj-surgeon-mcp" {print $1}')
jcmd "$pid" VM.info | grep -m1 '# Java VM:'
jcmd "$pid" VM.flags
jcmd "$pid" GC.heap_info
```

The per-agent `clojure-mcp` JVMs had no explicit limit. On the 24 GiB Mac, the
JVM selected an approximately 6 GiB maximum heap and a 384 MiB initial heap.
Representative assistants used approximately 64-98 MiB of heap.

Test this initial policy for per-agent assistants:

```text
-Xms64m -Xmx512m
```

Apply the policy through the shared `:cli-assist` launch configuration. Do not
patch one repository at a time. Run representative semantic, formatting,
embedding, and large-file workloads before making the policy universal.

## Native Image decision

Do not make a GraalVM Native Image port the first mitigation.

The incident initially used GraalVM CE 22.3/JDK 19 as a normal JVM because
launchd resolved `/usr/bin/java` through the only macOS-registered JDK. The
replacement launch selected OpenJDK HotSpot 26 from the operator environment.
A Native Image build would use a different runtime from both JVM
configurations.

In this workload, HotSpot was not materially smaller than the GraalVM JVM. The
capped GraalVM sample had an approximately 408 MiB footprint. The capped
HotSpot sample was approximately 944 MiB before a full GC and 426 MiB after
one diagnostic `jcmd PID GC.run`; its committed heap fell from approximately
752 MiB to 200 MiB. This comparison is not a general runtime benchmark. It
shows that committed-but-free heap and collection timing can dominate an idle
snapshot. Do not use forced full GC as routine memory management without a
measured policy and latency test.

A Native Image can reduce cold startup and some runtime overhead. It does not
remove application indexes, retained caches, direct buffers, or duplicated
processes. Native Image also requires explicit heap policy. The maximum heap
does not bound all process memory.

The current clj-surgeon development service depends on dynamic behavior:

- embedded nREPL evaluation;
- runtime namespace reload;
- handler Var dereferencing;
- reflective library behavior;
- classpath resources.

Native Image static analysis may require reachability metadata for reflection,
resources, JNI, and dynamic proxies. See the
[GraalVM Native Image documentation](https://www.graalvm.org/jdk22/reference-manual/native-image/index.html).
See the
[GraalVM memory-management documentation](https://www.graalvm.org/latest/reference-manual/native-image/optimizations-and-performance/MemoryManagement/)
for heap and garbage-collector constraints.

Consider a separate native production profile only after these actions:

1. Bound every long-lived JVM heap.
2. Remove stale stdio MCP processes.
3. Replace per-client MCP JVMs with a shared service where practical.
4. Measure the remaining cold-start and steady-state cost.

The native profile should exclude nREPL and hot reload. Keep the JVM profile
for development. Validate the complete MCP contract, memory footprint, startup
time, request latency, and failure behavior before adopting the native profile.

## Java selection on macOS

Do not delete, replace, or symlink `/usr/bin/java`. It is an Apple-signed
launcher in a [System Integrity Protection](https://support.apple.com/en-us/102149)
area, not the GraalVM runtime. Its selected JDK depends on the launch-time
environment and the JDKs registered with macOS.

The GraalVM installation observed in this incident was a separate bundle:

```text
/Library/Java/JavaVirtualMachines/graalvm-ce-java19-22.3.0
```

It had no matching package receipt and appeared to have been copied into place
in October 2022. Do not remove that bundle while a live process uses it. First
make each service select Java through its supported operator environment,
restart during maintenance, and verify the actual VM with `jcmd PID VM.info`.
Then move the unused bundle out of `JavaVirtualMachines` for a reversible
quarantine before deleting it.

SDKMAN's `26.0.2-open` installation is a Java home, not a macOS `.jdk` bundle
with `Contents/Home` and `Contents/Info.plist`. It therefore does not appear in
`/usr/libexec/java_home -V` automatically. A user-level wrapper bundle can be
created under `~/Library/Java/JavaVirtualMachines`, but pointing bundle metadata
at SDKMAN's moving `current` symlink can leave the registered version metadata
stale after a switch. Prefer explicit `JAVA_HOME`/`JAVA_CMD` in Makefile-owned
service launches. Register a concrete SDKMAN candidate only when a third-party
GUI application requires macOS JDK discovery, and regenerate the wrapper when
the selected candidate changes.

## Inspection procedure

### List Java processes

```bash
pgrep -x java
ps -p "$(pgrep -x java | tr '\n' ',' | sed 's/,$//')" \
  -o pid=,ppid=,rss=,etime=,command=
```

The process list can change between the two commands. Repeat the inspection if
a PID exits during the sample.

### Find a process working directory

```bash
lsof -a -p PID -d cwd -Fn
```

The `n` record contains the working directory.

### Measure physical footprint

```bash
footprint -p PID
```

Use `ps ... rss` for active resident memory. Use `footprint` when memory
pressure causes compression or paging.

### Inspect a JVM heap

```bash
jcmd PID VM.flags
jcmd PID GC.heap_info
```

Do not run a forced garbage collection during reconnaissance. A forced
collection changes the state that the investigation is measuring.

### Inspect system pressure

```bash
memory_pressure
sysctl vm.swapusage
vm_stat
```

macOS does not necessarily release swap immediately after a process exits. A
reboot is the fastest reliable swap reset after important work is saved. Do not
delete swap files manually.

## Process termination procedure

> CAUTION: A working directory can contain a mission-critical application.
> Confirm the exact command and parent before sending a signal.

1. Inspect the process.

   ```bash
   ps -p PID -o pid=,ppid=,rss=,%cpu=,etime=,state=,command=
   lsof -a -p PID -d cwd -Fn
   ```

2. Send `TERM` to the exact PID.

   ```bash
   kill -TERM PID
   ```

3. Confirm that the process exited.

   ```bash
   kill -0 PID
   ```

   A nonzero result means that the PID is no longer live.

4. If the process ignores `TERM` after a bounded wait, send `KILL` to the exact
   verified PID.

   ```bash
   kill -KILL PID
   ```

5. Inspect the parent process and working directory for a replacement process.

Do not kill a broad process name, an unresolved process group, or every process
under a repository path.

## Incident record: 2026-08-09

The investigation performed these actions:

- Terminated 31 `itrev-mcp-server` JVMs. Twenty-five had PPID 1. Ten required
  `KILL` after they did not exit within five seconds of `TERM`.
- Created Beads issue `itrev-mcp-server-v38` for lifecycle and heap controls.
- Terminated a runaway Codex Node REPL kernel, PID 76227. It used approximately
  2.6-3.0 GiB and high CPU.
- Terminated old Codex session PID 41907 and its 18-process descendant tree.
  The tree used approximately 3.0 GiB and included three legacy
  `clojure-lsp` children.
- Terminated the video-publisher agent MCP, PID 11304.
- Terminated the old clj-surgeon-inspect agent MCP, PID 47464.
- Preserved the Code Director, SMW, and Mothership application and agent roots.
- Restarted the clj-surgeon HTTP MCP with `-Xms64m -Xmx2g` and verified health
  on port 7888.
- Found that the managed service still used GraalVM CE 22.3/JDK 19 through
  `/usr/bin/java`.
- Installed SDKMAN Java `26.0.2-open`, selected it in the operator environment,
  and relaunched the managed service with that selected HotSpot executable.
- Removed the repository's temporary SDKMAN/version-specific default. The
  shipped launcher now selects Java from the inherited `JAVA_HOME` or `PATH`
  and accepts explicit `MCP_JAVA_HOME` and `MCP_JAVA_CMD` overrides.
- Fixed SMW and Mothership Makefile/service launch contracts to capture the
  operator-selected generic Java home. Restarted both with their Makefile
  targets and verified HotSpot 26 with 512 MiB maximum heaps. No live GraalVM
  JVMs remained.

The investigation showed that Chrome was the immediate pressure trigger, but
the accumulated agent, JVM, native LSP, and Node footprints created the
conditions for that trigger.
