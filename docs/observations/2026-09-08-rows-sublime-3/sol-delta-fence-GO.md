GO

# Sol delta fence: `0956951b..6e8891e6`

Reviewed only the requested delta. No required fix was found.

## Ruling

- Ruling (a) is implemented. `admitted-tmpdir!` retains the strict `/var/tmp`
  policy and throws `background-gate-unsafe-tmpdir` with the required actionable
  `:next_call {:action "set-java-tmpdir-and-retry" :java_tmpdir
  "/var/tmp/<owned-temp-directory>"}`.
- The public `namespace_split` boundary promotes that repair into the receipt
  and removes the duplicate from `:evidence`. The background runner admission
  occurs before `capture!`, candidate analysis, artifact allocation, or
  `publish!`, so this refusal truthfully reports `mutation_attempted=false`.
- Both public instruction surfaces name `TMPDIR=/var/tmp/...` for Babashka and
  `-Djava.io.tmpdir=/var/tmp/...` for JVM/MCP startup.
- The three red-witness changes cover gate ex-data, public-boundary propagation
  before mutation, and both CLI help phrases. The MCP description carries the
  same operator guidance.
- `NS-SPLIT-059` is registered in the specifications and paper-cut registry;
  the non-MCP intent census correctly moves `237 -> 238`.
- The lane-manifest repair is accepted despite being beyond ruling (a).
  `0956951b` already contained four JVM witnesses omitted from the pins, and
  this delta adds one JVM witness. The per-namespace counts reconcile exactly,
  so `adopted 547 -> 552` and `total 1566 -> 1571` are the required repairs.
- `cli_dispatch_test.clj` is outside the paper-cut intent scanner, as disclosed.
  That does not orphan `NS-SPLIT-059`: its gate and public-boundary witnesses
  are both in scanned namespaces.

## Independent checks

- `git diff --check 0956951b..6e8891e6`: clean.
- Focused JVM run of `split-proof-gate-test`, `mcp-namespace-split-test`,
  `lane-manifest-test`, and `mcp-intent-contract-test`: 71 tests, 785
  assertions, 0 failures, 0 errors; 0 isolation violations.
- Focused Babashka run of
  `background-proof-help-names-temp-root-requirement`: 1 test, 2 assertions,
  0 failures, 0 errors.
- Per instruction, `make test` was not run. The builder's disclosed cold gates
  were considered supporting evidence, not independently repeated here.


> END RECEIPT (fence-run): worktree HEAD at review exit = 0fc7e37211504c72bd09d8de6c5bb6ce27fca9a2 = fenced sha.
