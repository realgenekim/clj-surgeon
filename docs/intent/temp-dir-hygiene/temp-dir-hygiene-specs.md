---
parent: temp-dir-hygiene-design
prefix: MCP-OP-TMPHYG
status: "implemented 2026-09-04; inb-9483a4"
---

# Test-Runner Temp-Directory Hygiene Specifications

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

- [x] **MCP-OP-TMPHYG-001**: When `test/run_all.clj` or
  `test/clj_surgeon/mcp_test_runner.clj` starts, clj-surgeon shall resolve
  `java.io.tmpdir`'s base directory and refuse to run any test — exiting
  non-zero with a named message — whenever that base is tmpfs-backed (RAM),
  rather than silently writing test fixtures into RAM.

- [x] **MCP-OP-TMPHYG-002**: When either test runner completes a run,
  clj-surgeon shall have isolated that run's temp-file/temp-directory
  creation into a private, per-run root directory, and shall report a named,
  counted failure — and exit non-zero — whenever any entry created by that
  run survives inside that root after the run finishes.

- [x] **MCP-OP-TMPHYG-003**: When a test runner resolves its temp base,
  clj-surgeon shall refuse to run — exiting non-zero with a named message —
  unless that base is POSITIVELY PROVEN to be real-disk backed: a base that
  is, or is under, a path known to be RAM-backed by name (`/tmp`,
  `/dev/shm`), and a base whose filesystem type no mount source can
  determine, are both refusals.

- [x] **MCP-OP-TMPHYG-004**: When a test runner proceeds as the re-exec'd
  child, clj-surgeon shall refuse to run unless the re-exec sentinel names
  the exact private root that process was launched on, and shall refuse —
  with a named message — to delete any directory that is not one of its own
  `clj-surgeon-suite-*` per-run roots.

- [x] **MCP-OP-TMPHYG-006**: When a test runner re-executes itself to obtain
  an isolated temp root, clj-surgeon shall launch the child with the JVM
  options the parent itself was launched with — so a pinned heap ceiling is
  the ceiling the tests actually run under — and shall forward the runner's
  own command-line arguments to that child.

- [x] **MCP-OP-TMPHYG-005**: When a test runner isolates a run, clj-surgeon
  shall place every DESCENDANT PROCESS of that run inside the same isolated
  root — via `TMPDIR`/`TMP`/`TEMP`, since `-Djava.io.tmpdir` is a
  JVM-internal property no child process inherits — so a subprocess that
  picks its own temp location cannot write outside the root the leak witness
  watches.

- [x] **MCP-OP-TMPHYG-007**: When a test-runner process is terminated in a
  way the VM can observe — an external `timeout`'s SIGTERM, a Ctrl-C — before
  its run completes, clj-surgeon shall still delete that run's isolated root;
  and at the start of a run it shall delete isolated roots left under the
  base by runs whose owning process is dead and whose age exceeds the stale
  threshold, touching nothing whose name it did not itself create.

- [x] **MCP-OP-TMPHYG-008**: When a test runner cannot create its isolated
  root under the resolved base — an unwritable or otherwise unusable base —
  clj-surgeon shall refuse with the same named, exit-97 message as every
  other refusal, never a raw stack trace.

- [x] **MCP-OP-TMPHYG-009**: Every test entry point in this repository —
  `test/run_all.clj`, `test/clj_surgeon/mcp_test_runner.clj`,
  `test/analyzer_contract_test_runner.clj`,
  `test/clj_surgeon/memory/memory_test_runner.clj` and
  `src/clj_surgeon/memory_battery_runner.clj` — shall enforce this leaf's
  refusal and isolation before running anything.

- [x] **MCP-OP-TMPHYG-010**: No Makefile recipe, no `test/*.sh` gate and no
  `bench/*.sh` harness shall name a RAM write target — neither a hard-coded
  `/tmp/<path>` nor a `TMPDIR` fallback that names `/tmp`. Scratch roots derive
  from `TMPDIR` with a **real-disk** default (`/var/tmp`). `bench/*.sh` is in
  scope because `make test` runs four of those harnesses; the fallback shape is
  in scope because it takes `/tmp` in every shell that has not set `TMPDIR`.

- [x] **MCP-OP-TMPHYG-011**: When the mounts-table witness seam
  (`CLJ_SURGEON_MOUNTS_FILE`) supplies the filesystem type, clj-surgeon shall
  never treat that answer as positive proof of real disk: a seam-sourced
  `tmpfs` answer refuses as normal, and any other seam-sourced answer is
  `:unknown` — also a refusal. The seam exists only so a gate can execute the
  "no mount source can answer" branch; it can never turn a refusal into a run.

- [x] **MCP-OP-TMPHYG-012**: The Make layer shall not propagate a RAM-backed
  `TMPDIR` into the scratch root it hands to self-test recipes: with `TMPDIR`
  set to `/tmp` or `/dev/shm`, `SELF_TEST_TMP` shall resolve to a real-disk
  path; with `TMPDIR` unset it shall be `/var/tmp`; a real-disk `TMPDIR` shall
  be honoured unchanged. A refusal that lives only in the Clojure layer does
  not protect a recipe that never reaches Clojure.

- [x] **MCP-OP-TMPHYG-013**: `sweep-root!` shall report what it actually did:
  `true` only when the root is gone after the attempt, `false` when the delete
  failed (an undeletable root, a foreign owner) as well as when the name was
  refused. A receipt that names a subject it did not act on is `:unverified`,
  never success.

## Misreadings these requirements exist to forbid

- "Exporting `TMPDIR=/var/tmp/forge` before invoking bb is enough." bb
  (babashka's GraalVM native image) does not read `TMPDIR` or
  `JAVA_TOOL_OPTIONS` — measured 2026-09-04. Only a literal `-D` flag passed
  to `bb` itself, at its own process startup, changes where it creates temp
  files. MCP-OP-TMPHYG-001 and -002 are satisfied by `secure-tmpdir!`
  re-executing the suite as a child process with that flag, not by env vars
  alone.
- "Calling `System/setProperty \"java.io.tmpdir\"` at the top of `-main`
  redirects the run." It does not, on either bb or a real JVM — the JDK
  captures the property into an effectively-immutable holder at process
  bootstrap and never re-reads it. A witness that trusts
  `System/getProperty` after such a call is watching a value, not a
  location; the actual leak witness (MCP-OP-TMPHYG-002) instead asks whether
  the ISOLATED ROOT — set via a startup `-D` flag on a re-exec'd child — is
  empty.
- "Snapshotting `/var/tmp/forge` before and after the suite and diffing it is
  a sound leak witness." It is not, on a shared multi-tenant seat: a
  concurrent seat's own test run can land fixtures in the same window,
  producing a false leak report for a prefix the diffing suite cannot even
  create. MCP-OP-TMPHYG-002 isolates each run into a private, PID/random-
  unique sub-directory specifically so this cannot happen.
- "A `.deleteOnExit()`-registered temp file is clean." It is deferred to the
  JVM's own shutdown sequence, which runs AFTER the leak witness's own check
  — the witness will (correctly) flag it as present, and it must be swept
  immediately (a tracked `finally`, or `clj-surgeon.tmp-leak-support`'s
  `tracking-temp-dir-fixture`) instead.

## Falsifiers

The falsifier matrix, with the required result and the named witness for
each requirement, is in `temp-dir-hygiene-design.md`. The executable
witnesses live in `test/clj_surgeon/tmp_leak_support_test.clj`; the
mechanism under test (`secure-tmpdir!`, `report-and-sweep-leak!`,
`with-temp-dir`, `tracking-temp-dir-fixture`) lives in
`test/clj_surgeon/tmp_leak_support.clj` and is required by both
`test/run_all.clj` and `test/clj_surgeon/mcp_test_runner.clj` — the
Makefile's `test-bb` (the babashka corpus, `test/run_all.clj`) and
`mcp-test` (the JVM merge gate, the lane runner) targets are this leaf's
implementation-source witness, since the mechanism itself lives under
`test/` rather than `src/`. Both names are post-2026-09-04: `test-fast` used
to mean the babashka corpus and now means the JVM fast lane.

## Rationale

Andon-class finding `inb-9483a4`, 2026-09-04: Anvil's `/tmp` reached 96% of
its inodes from 82,210 leaked test-fixture directories (19,292
`clj-surgeon-change-buffer-*` from this repo's `mcp_change_buffer_test.clj`;
the remainder from an unrelated repo's `cfp-store-test*`) while bytes sat at
44% — an inode exhaustion invisible to a disk-space check. Fixing the
leaking tests alone (`mcp_change_buffer_test.clj`, `mcp_process_test.clj`,
`workspace_onboarding_test.clj`, `mcp_cold_verify_test.clj`,
`mcp_paths_test.clj`, `mcp_intent_contract_test.clj`,
`parser_admission_test.clj`, `worktree_lifecycle_io_test.clj`,
`failure_report_test.clj`, `recovery_test.clj`) closes today's instances;
this leaf's runner-level refusal and per-run isolation closes the CLASS —
any future leaking test fails both suite gates by name, with no allowlist.
