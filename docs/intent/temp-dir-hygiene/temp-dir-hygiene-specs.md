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
Makefile's `test-fast` and `mcp-test` targets are this leaf's
implementation-source witness, since the mechanism itself lives under
`test/` rather than `src/`.

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
