---
parent: high-level-design
prefix: MCP-OP-TMPHYG
status: "implemented 2026-09-04; inb-9483a4"
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

| Intent | Falsifier — what would prove the promise broken | Required result | Witness |
|---|---|---|---|
| MCP-OP-TMPHYG-001 | Either runner (`bb test/run_all.clj`, `clojure -M:clj-surgeon/mcp-test`) completes a test run while `java.io.tmpdir`'s resolved base is tmpfs-backed. | The runner refuses before running any test (exit 97, named message); nothing is created under the tmpfs base. | `tmpfs-predicate-tells-ram-from-disk` (`clj_surgeon/tmp_leak_support_test.clj`); functionally, every green `~/bin/suite-run` invocation on this seat's `/var/tmp/forge` (ext4) IS the accepted-path proof. |
| MCP-OP-TMPHYG-002 | A test under either runner's namespace list creates a file or directory under `java.io.tmpdir` and the suite still exits 0. | The suite's exit code is non-zero, and *err* names the leaked entries (up to 5, with a count), whenever anything survives inside that run's isolated root after the suite finishes. | `with-temp-dir-cleans-up-on-throw`, `with-temp-dir-cleans-up-on-success` (`clj_surgeon/tmp_leak_support_test.clj`); functionally, the RED counts measured while fixing this leaf (`bb test/run_all.clj`: 1 leaked entry; `clojure -M:clj-surgeon/mcp-test`: 66 leaked entries, dominated by `clj-surgeon-change-buffer-*`) each went to 0 once the leaking test namespaces were fixed. |

## Deliberately out of scope in this leaf

- **Fixing every hard-coded `/tmp` string in `bench/*.sh`.** Those scripts are
  not required by `test/run_all.clj` or `mcp_test_runner.clj` and are not
  covered by this leaf's gates; `test/relation_causal_cohort_runner_test.sh`
  and `test/performance_regression_sentinel_runner_test.sh` (genuinely under
  `test/`) were fixed to `"${TMPDIR:-/tmp}/..."` as part of this leaf, but the
  `bench/` harnesses were left as a named follow-up.
- **`test/mcp_heap_config_test.sh`'s `MCP_STATE_DIR='/tmp/...'`** is a `make
  -n` (dry-run) assertion against the printed Makefile recipe text — it never
  launches a process or creates that directory, so it is not a leak.
- **A process killed by `SIGKILL` or an OOM.** The shutdown hook that sweeps
  an isolated root only runs for terminations the JVM/Substrate VM gets to
  observe (a graceful `SIGTERM`, an ordinary `System/exit`); nothing can run
  after `SIGKILL`. The `finally`-based cleanups inside individual tests carry
  the same limit.
