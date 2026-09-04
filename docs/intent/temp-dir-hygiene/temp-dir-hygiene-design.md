---
parent: high-level-design
prefix: MCP-OP-TMPHYG
status: "implemented 2026-09-04; round two after independent review; inb-9483a4"
---

# Test-Runner Temp-Directory Hygiene

## Context

Anvil's `/tmp` filled to 96% of its inodes from 82,210 leaked test-fixture
directories — 19,292 of them `clj-surgeon-change-buffer-*` from this repo's
`mcp_change_buffer_test.clj` — while bytes sat at 44%: the filesystem died on
inode exhaustion long before disk space would have warned anyone. Gene's
ruling, verbatim: *"/tmp is tmpfs, which uses RAM. You must use /var/tmp. Make
it impossible to make this mistake again."*

Two measured facts made the obvious fix (point `java.io.tmpdir` at
`/var/tmp/forge` and call it done) insufficient:

1. **bb (babashka's GraalVM native image) does not read `JAVA_TOOL_OPTIONS`
   at all.** `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge bb -e
   '(System/getProperty "java.io.tmpdir")'` still prints `/tmp`. Only a
   literal `-D` flag passed to `bb` itself changes it. `~/bin/suite-run`'s
   env alone does not protect `bb test/run_all.clj`.
2. **Neither bb nor a real `java` honors a runtime `System/setProperty
   "java.io.tmpdir"` for actual temp-file/dir creation**, even though
   `System/getProperty` immediately reflects the new value. Both
   `Files/createTempDirectory` and `File/createTempFile` kept writing to the
   process's ORIGINAL startup value after an in-process
   `System/setProperty` call, on both bb and a real JVM already launched
   with `JAVA_TOOL_OPTIONS` set. A first cut of the isolation mechanism
   called `System/setProperty` at runtime to give each suite run a private
   sub-directory; it silently did nothing, and the leak witness (which
   trusted `System/getProperty`) reported GREEN while real fixtures kept
   landing in the shared base directory (or, for bb, literally `/tmp`) — a
   false green from a witness watching the wrong directory.

Because of (2), the only way to isolate a run's temp-file creation into a
private, race-free directory is to set that directory as a literal `-D` flag
at a **process's own startup**. Because `/var/tmp/forge` is a shared,
seat-wide scratch area, a witness that snapshots and diffs that shared
directory around a run is also unsound: a first cut reported 67 "leaked"
entries after `bb test/run_all.clj`, all of them prefixed
`clj-surgeon-change-buffer-*` / `clj-surgeon-kondo-admission-*` — names that
namespace cannot even create (`mcp-change-buffer-test` is not required by
`run_all.clj`) — because a concurrent seat's own test run landed fixtures in
the same shared directory in the same second (`o2r4-review-fx`,
`gate3-review-fx`, `q5z15-review-fx` were interleaved in the same `ls`).

## Component boundary

```text
suite entry (test/run_all.clj or mcp_test_runner.clj -main)
        |
        v
  secure-tmpdir! (clj-surgeon.tmp-leak-support)
        |
        +--> base tmpfs? ──yes──> refuse: print + exit 97, run NOTHING
        |
        +--> not yet re-exec'd? ──yes──> spawn child with
        |         -Djava.io.tmpdir=<private per-run root>
        |         (bb: `bb -D... <script>`; JVM: nested `java -D... -cp
        |          <this classpath> clojure.main -m <main-ns>`)
        |         inherit stdio, wait, exit THIS process with the child's
        |         code -- never proceeds past this point
        |
        v (only reached by the re-exec'd child)
  run-tests over the isolated root
        |
        v
  report-and-sweep-leak!: diff root's contents against empty-at-start;
  print named leaked entries (exit code += 1); delete the root either way
```

The isolation directory is created fresh and PID/random-unique per run, so
concurrent suite runs on the same seat never share a directory and the
snapshot/diff inside `report-and-sweep-leak!` cannot see another process's
fixtures. The shutdown hook registered alongside the isolated root sweeps it
even if the process is killed by `SIGTERM` (an external `timeout`, Ctrl-C) —
not `SIGKILL`/OOM/crash, where nothing can run.

## Why marker presence is not enough here

`mcp-intent-contract/audit-contract` (the standing intent audit) checks that
an `@spec <ID>` annotation exists in some implementation source and some test
source — it cannot see whether a *new* leak was introduced by a future test
file that creates a temp directory and never deletes it. The structural
backstop is the runner itself: any test namespace under `test/run_all.clj` or
`test/clj_surgeon/mcp_test_runner.clj` that leaks now fails BOTH gates by
name, unconditionally, with no allowlist and no per-test opt-out.

## Falsifier table

Every witness below is EXECUTED behaviour. Round one's witnesses were a unit
test of the tmpfs predicate plus "every green suite run is the accepted-path
proof" — the accepted path is not the requirement, and no test anywhere drove
a runner to exit 97. The subprocess witnesses live in
`test/tmp_leak_ratchet_test.sh` (`make tmp-leak-ratchet-self-test`, inside
`mcp-test`), which drives `clj-surgeon.tmp-leak-probe` — a minimal real test
entry point — in child processes, because `secure-tmpdir!` re-execs its own
suite and calls `System/exit`.

| Intent | Falsifier — what would prove the promise broken | Required result | Witness |
|---|---|---|---|
| MCP-OP-TMPHYG-001 | Either original runner completes a run while its base is tmpfs-backed. | Refuse before running any test: exit 97, named message. | `tmp-leak-ratchet-self-test` 3a; `tmpfs-predicate-tells-ram-from-disk`. |
| MCP-OP-TMPHYG-002 | A test creates a file or directory under java.io.tmpdir and the suite still exits 0. | Non-zero exit, *err* names the leaked entries with a count. | `tmp-leak-ratchet-self-test` 5b; `with-temp-dir-cleans-up-on-throw` / `-on-success`. |
| MCP-OP-TMPHYG-003 | A runner runs when no mount source can determine the fstype, or when the base is `/tmp` / `/dev/shm` and `findmnt` is unavailable. | Exit 97 with a named message in BOTH cases; and the mounts-table fallback still answers when only `findmnt` is missing. | `tmp-leak-ratchet-self-test` 3a–3e (3b is the review's arm B; 3e proves the fallback is not dead code). |
| MCP-OP-TMPHYG-004 | A process that inherited the sentinel treats the shared base as its private root, and the sweep delete-trees it. | Exit 97; every planted foreign entry survives; `sweep-root!` refuses any name that is not `clj-surgeon-suite-*`. | `tmp-leak-ratchet-self-test` 4a, 4b. |
| MCP-OP-TMPHYG-005 | A subprocess of the run creates a temp dir outside the isolated root. | The subprocess's temp dir is inside the root, and is reported as a leak; the shared base is left empty. | `tmp-leak-ratchet-self-test` 5a–5c; `configure-environment-publishes-this-process-temp-directory`. |
| MCP-OP-TMPHYG-006 | The re-exec'd child runs at a different heap ceiling than the parent, or without the parent's argv. | child `maxMemory` == parent `maxMemory`; child argv == parent argv, on both the JVM and bb lanes. | `tmp-leak-ratchet-self-test` 6a–6c; `test/mcp_heap_config_test.sh`'s execution assertion. |
| MCP-OP-TMPHYG-007 | A SIGTERMed run leaves its isolated root behind; or the startup sweep deletes a live run's root or another tenant's entry. | Base empty after a SIGTERM; dead-pid stale root swept; live-pid root and foreign entries untouched. | `tmp-leak-ratchet-self-test` 7a, 7b. |
| MCP-OP-TMPHYG-008 | An unwritable base produces a stack trace instead of a refusal. | Exit 97 with a `tmp-refused:` line and no clojure `Execution error` banner. | `tmp-leak-ratchet-self-test` 8. |
| MCP-OP-TMPHYG-009 | Any of the five test entry points runs with a RAM-backed base. | All five exit 97 with a named message. | `tmp-leak-ratchet-self-test` 9 (drives all four `-m`-able runners; `run_all.clj` is covered by 3a's bb arm). |
| MCP-OP-TMPHYG-010 | A Makefile recipe or a `test/*.sh` gate names a `/tmp/<path>` write target. | The scan finds none. | `tmp-leak-ratchet-self-test` 10. |
| MCP-OP-TMPHYG-011 | A caller supplies a forged `CLJ_SURGEON_MOUNTS_FILE` claiming disk while `findmnt` cannot answer, and the run PROCEEDS. | Exit 97, `UNDETERMINABLE`: a seam-sourced non-tmpfs answer is `:unknown`; a seam-sourced `tmpfs` answer still refuses. | `tmp-leak-ratchet-self-test` 3g, 3h; `a-seam-sourced-fstype-can-never-prove-real-disk`. |

## Measured facts added in round two

1. **`slurp` is not the only thing that fails on procfs.** `slurp`,
   `(.readAllBytes (io/input-stream "/proc/mounts"))` and
   `(line-seq (io/reader "/proc/mounts"))` ALL throw
   `java.io.IOException: Invalid argument`, on bb and on a real JVM, because
   procfs reports `st_size = 0`. The review's suggested streaming-reader
   repair does not work. `java.nio.file.Files/lines` reads all 41 lines on
   both runtimes and is what `mounts-table-fstype` uses.
2. **`java.lang.management.ManagementFactory` cannot be named in code bb
   loads.** It is absent from bb's native image and sci rejects the symbol at
   ANALYSIS time, before any `try`/`catch` could run — so
   `parent-jvm-options` reads it through `Class/forName`.
3. **An undeterminable fstype is a REAL state, not a theoretical one**
   (it follows from 1), which is why `mount-fstype` is tri-state and
   `:unknown` refuses.

## One declared, deliberate exception

`src/clj_surgeon/mcp_process.clj` `extract-packaged-wrapper!` creates its
clj-kondo admission wrapper with `File/createTempFile` + `.deleteOnExit` and
no `finally`. That is CORRECT there: the wrapper must outlive the call that
extracted it. The consequence is declared rather than fixed — a long-running
or `kill -9`'d MCP server leaves one `clj-kondo-admission-*.py` behind per
start, now inside whatever `java.io.tmpdir` that server was launched with.
Every other `createTemp*` site in `src/` deletes in a `finally`.

## Deliberately out of scope in this leaf

- **Fixing every hard-coded `/tmp` string in `bench/*.sh`.** (The Makefile
  recipes that INVOKE those harnesses no longer hand them `/tmp` roots —
  MCP-OP-TMPHYG-010 — but the scripts' own internal defaults are untouched.) Those scripts are
  not required by `test/run_all.clj` or `mcp_test_runner.clj` and are not
  covered by this leaf's gates; `test/relation_causal_cohort_runner_test.sh`
  and `test/performance_regression_sentinel_runner_test.sh` (genuinely under
  `test/`) were fixed to `"${TMPDIR:-/tmp}/..."` as part of this leaf, but the
  `bench/` harnesses were left as a named follow-up.
- ~~**`test/mcp_heap_config_test.sh`'s `MCP_STATE_DIR='/tmp/...'`** is a
  `make -n` assertion, so it is not a leak.~~ CORRECTED in round two: true as
  far as it goes, and beside the point. That gate reading only recipe TEXT is
  exactly how the MCP suite came to run at the box default heap for a day
  while the gate stayed green. It now derives the path from `TMPDIR`
  (MCP-OP-TMPHYG-010) *and* asserts the heap ceiling by EXECUTION
  (MCP-OP-TMPHYG-006).
- **A process killed by `SIGKILL` or an OOM.** The shutdown hook that sweeps
  an isolated root only runs for terminations the JVM/Substrate VM gets to
  observe (a graceful `SIGTERM`, an ordinary `System/exit`); nothing can run
  after `SIGKILL`. The `finally`-based cleanups inside individual tests carry
  the same limit.
