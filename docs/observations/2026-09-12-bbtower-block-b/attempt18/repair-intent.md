# Attempt18 repair intent

Continue the approved attempt17 envelope work at 172a0685. The HLD links
Test-Runner Temp-Directory Hygiene; its design and MCP-OP-TMPHYG-002/005
require private per-run scratch and cleanup. This repair applies that
existing policy to fixture writers; it changes no operation contract.

When the gate runs within its three writable roots, fixture scratch shall
inherit TMPDIR (Python/shell) or the runner's java.io.tmpdir (Clojure).
Explicit fixture overrides retain their existing meaning. When a freshness
witness needs a Git checkout, its Git metadata shall also be owned scratch.
The fresh committed-HEAD bytes and before/after ignored-file comparison
remain the hygiene oracle.

Misreadings: adding shared filesystem roots to the envelope; skipping a
failing fixture for convenience; treating all home paths as writable;
allowing a linked worktree to register outside the envelope; considering a
passed write receipt equivalent to a passed gate.

Witnesses: the original whole-gate diagnostic; static-probes.py for the
actual baseline mirror and shell setup; unreached-red.log for the remaining
manifest commands; focused green checks and the complete restricted gate.
The helper's existing details-path publication witness must still commit
exactly once and publish outside its fixture workspace. Its root alone
changes. Formatter and paved lint precede the green gates. User approved
all policy repairs in attempt18, including whatever the diagnostic adds.

The wrapper differs from attempt17 only by the /dev/null WRITE_FILE rule
and reporting that exception. /home/forge/bin/lib/packet.py:78-82 grants the
same device-only exception. Three directory roots, UUID TMPDIR, JVM tmpdir,
_JAVA_OPTIONS removal, TRUNCATE and ABI >= 3 remain as built.
