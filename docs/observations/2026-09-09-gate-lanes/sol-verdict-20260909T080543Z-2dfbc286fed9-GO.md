GO

# Sol fence review, round 4: `astra/gate-lanes` at sealed candidate `a51dd39e955972ddac1d417d40e73e05f30637c1`

Reviewed the checked-out sealed merge of base
`e34e86da829f32a6735ebad1c8ee668342b0b63e` and tip
`2dfbc286fed98fb307d29437249fc2112ba53883`. The sealed candidate and tip have
the same tree, `ea189742e43eb8f0ba70e91cc372984b09d372b8`. I did not run
`make test`, as requested. No blocking finding remains.

## Repair verification

1. **GATE-LANES-FENCE-001-R3 is closed.** The production mechanism has no
   pathname, directory inode, published environment identity or coordinator
   root. A slot is a bound Linux abstract Unix socket and the admission mutex is
   another abstract socket. The ordered rerun of my round-3 attack used two
   independent processes with only `PATH` in their environments and no shared
   file or descriptor. It produced:

   ```text
   {'coordinator_a': 'admitted', 'coordinator_b': 'refused',
    'root_attributes_that_could_differ': [], 'first_held': True,
    'second_admitted': False, 'derived_width': 1,
    'bound_violated': False}
   ```

   `/var/tmp/forge/gate-slots` is absent. The focused oracle ran 7 tests in
   0.317 s with `ResourceWarning` promoted to an error and passed.

2. **Independent coordinator and lifetime witnesses pass.** The named width-two
   witness produced admitted/admitted/refused. The SIGKILL and failed-exec
   subtests both reclaimed the slot on the next acquisition attempt. Both
   focused tests passed. A separate 16-process probe modeling a second eight-job
   fast lane starting while the first eight holders were live admitted exactly
   8 and refused exactly 8 (`bound_violated False`). Admission serializes the
   live memory sample, occupied count and reservation across processes in the
   same network namespace.

3. **Full-gate evidence is bound to the repaired bytes.** The retained log
   `/var/tmp/forge/plan2/cellC/make-test.log` records source digest
   `88b43ae32e49c1d41f7346a9bb1cad5447fccfb13ca8eb19e0f61c5eeb594c79`;
   recomputing `source-digest` on the sealed candidate returned the same value.
   The log records `:peak-worker-count 8` at `:lanes 8`, exit 0, and 164/898/891
   tests with 0 failures and 0 errors. Its `:git-head`/`:git-tree` identify the
   pre-commit state because the repair was uncommitted during that run; the
   byte-inclusive source digest is the candidate binding. This receipt itself
   must not be consumed as evidence for the sealed Git tree.

4. **TEST-ISO-007 remains a measured bound.** The intent names the measured
   eight-worker 8,836 ms observation, selects 18,000 ms as approximately 2x
   margin, and retains the independent 60,000 ms fast-lane union ceiling. The
   direct boundary witness accepts 8,002 and 18,000 ms and refuses 18,001 ms;
   the focused TEST-ISO-007 and serial-authority witnesses passed 7 assertions,
   with no failure or error. Five retained corrected width-eight measurements
   were 7.325/8.379/8.358/8.869/8.852 s. This is not an unexplained
   red-suppressing number.

5. **The global isolation barrier is intact.** `run-gate-pool!` assigns only MCP
   fast jobs to phase 0. MCP integration, alias, BB and the MCP shell/oracle job
   are phase 1. The checkout experiment records six runtime-container
   violations without local ordering, zero with local ordering, and zero for
   the retained shared-root global barrier. The runner is byte-identical to
   `1c762f73` (identical Git blob `884728acd1069a15b4dcd1b3d3a7bc119178eb17`),
   so this repair did not disturb the barrier.

6. **Serial execution cannot authorize landing.** `make -n test-serial` always
   supplies `--debug-serial true` and prints `SERIAL/NOT-A-GATE`. The direct
   authority witness proves `landing-eligible?` false for debug serial even
   with no problems. `run-gate!` deletes the top-level output at entrance and
   writes it only for a passing non-debug run; no serial argument path was
   found that can mint a landing receipt.

7. **Make recipes preserve the gate.** No target present in the base Makefile is
   absent from the candidate. `make test` still delegates to `landing-gate`.
   `print-gate-stages` reports all seven required stages: recovery, freshness,
   alias, MCP, BB, hygiene and intent audit. The Makefile did not change in the
   round-3-to-round-4 delta, and both that delta and the base-to-candidate
   Makefile diff pass `git diff --check`. No target silently became a no-op or
   lost a required step.

8. **Landing-receipt consumption remains fail closed.** Installed ship/land
   hashes remain `88af948e...` / `8bd62ed8...`. Land independently checks the
   merged tree, landing authority, recipe hash, manifest hash, expected stage
   set and toolchain. A receipt from an earlier tree yields `tree-mismatch` and
   is rerun. For this branch's eventual own landing, the correct outcome is
   consumption only when the merged tree is byte-identical to the sealed tree
   and every recipe/manifest/stage/toolchain predicate matches; otherwise land
   must rerun. Proving which path the eventual landing takes was expressly not
   required.

## Delta audit and scope

`git diff a966ca17..2dfbc286` contains exactly:

```text
test/clj_surgeon/battery_parallel_runner.clj
test/gate_slot.py
test/oracles/test_gate_slot.py
```

The runner change is its restoration to the byte-identical `1c762f73` version;
the two Python files replace the path semaphore and its regressions. There are
no other round-3-to-round-4 changes. Abstract names are scoped to the network
namespace rather than the user, so two users in the same host network namespace
share the budget. That is the stated intended box-wide policy; isolated network
namespaces retain independent budgets.

The pre-existing untracked
`docs/observations/gate-lanes-fence.md.codex.log` was not modified. This verdict
document is the only intentional review change.


> END RECEIPT (fence-run): worktree HEAD at review exit = a51dd39e955972ddac1d417d40e73e05f30637c1 = fenced sha.
