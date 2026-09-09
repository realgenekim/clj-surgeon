# gate_slot: the split semaphore is now unrepresentable (abstract sockets)

Sol GATE-LANES-FENCE-001, rounds 2 and 3. R2 was repaired by refusing a replaced root;
Sol's R3 walked through it with an **independent** coordinator. That finding is correct and
general — every path-based lock has the hole — so the second round of this work removed the
pathname instead of patching it.

| item | value |
|---|---|
| worktree | `/home/forge/src/clj-surgeon-astra-consult`, branch `astra/gate-lanes` |
| base | `1c762f73bf49db62a1996b2e2afd8b76badb8f8f` |
| R2 commit (superseded) | `a966ca17b3255edb05c67ecdfddbeb8e037f5558` |
| **current commit** | **`2dfbc286fed98fb307d29437249fc2112ba53883`**, author `forge-anvil <forge-anvil@anvil>`, Gene + Fable trailers, **not pushed** |
| files | `test/gate_slot.py`, `test/oracles/test_gate_slot.py`, `test/clj_surgeon/battery_parallel_runner.clj` (reverted byte-identical to base) |
| full gate | `flock /home/forge/tmp/suite.lock make test` — **EXIT 0, wall 172 s** |

## The mechanism (rung 5: the bad state cannot be written down)

A slot is a name in the Linux **abstract** Unix-domain socket namespace,
`\0clj-surgeon-gate-slot-<i>`. `bind` succeeds → slot acquired. `EADDRINUSE` → taken.
`close`, exit, or SIGKILL → the kernel frees the name. Admission serialization is a bind on
`\0clj-surgeon-gate-admission`, held only across sample + count + acquire.

There is no directory, no inode and no path — nothing to unlink, nothing to recreate, and
nothing for two coordinators to disagree about. Deleted outright: `SLOT_ROOT`, `open_root`,
`hold_root`, `ROOT_ID_ENV`, `published_identity`, the dirfd holder process and all of its
environment plumbing. `battery_parallel_runner.clj` is byte-identical to base again — a
constant kernel name needs no coordinator to publish it, which is the clearest evidence that
the R2 design was carrying weight it should not have had to carry.

Unchanged: width from live `MemAvailable` on every acquire; holders above a shrunken width
still counted as occupied (probed to `MAX_SLOTS`); typed refusals for insufficient memory and
unknown memory.

## Sol's R3 attack, executed both ways

Two coordinators, width 1, first holds, root removed between them.

| | pre-fix (`a966ca17`, the R2 design) | post-fix (`2dfbc286`) |
|---|---|---|
| `coordinator_a_inode` | `2049:1083390` | — no root exists |
| `coordinator_b_inode` | `2049:1083391` | — no root exists |
| `different_roots` | **True** | **False** (no root attribute to have two of) |
| `first_held` | True | True |
| `second_admitted` | **True** | **False** (`refused`) |
| `derived_width` | 1 | 1 |
| `bound_violated` | **True** | **False** |

Post-fix, run against the production module with two processes sharing no file, no
descriptor and an environment of `PATH` alone:

```
{'coordinator_a': 'admitted', 'coordinator_b': 'refused',
 'root_attributes_that_could_differ': [], 'different_roots': False,
 'first_held': True, 'second_admitted': False, 'derived_width': 1,
 'bound_violated': False}
```

This is asserted as one dict literal in
`test_two_independent_coordinators_cannot_split_the_semaphore`, together with
`assertEqual([], roots)` — the R3 attack step now has no target, and the test says so
structurally rather than by narration.

## The four witnesses Sol asked for

| # | witness | result |
|---|---|---|
| 1 | R3 reproduction: two **independent** coordinators, width 1, "remove the root" is a no-op | `admitted`, `refused`; `bound_violated False` |
| 2 | three independent coordinators, width 2 | `['admitted', 'admitted', 'refused']` |
| 3 | holder SIGKILLed (and failed `exec`) frees its slot within one acquire attempt | green, both subtests — the next `try_acquire` succeeds immediately, no stale lock |
| 4 | nested coordinator JVMs in the real gate: peak workers ≤ derived width | receipt `:capacity {… :lanes 8 …}`, `:peak-worker-count 8` |

Plus the retained boundaries: capacity/typed refusals, live shrink and the high-slot budget
(`[3,3,3,1,1,2]` sample sequence), and exec inheritance — the slot survives `execvp` into
`sh`, which holds it, and exit releases it.

## Evidence

- Focused, **bare**: `python3 -B -W error::ResourceWarning -m unittest discover -s test/oracles
  -p test_gate_slot.py` → **Ran 7 tests, OK**, 0.28 s. (`-W error::ResourceWarning` so an
  unclosed slot socket is a failure, not a warning.)
- Focused, **under the gate**: the same target runs inside a gate worker as part of
  `mcp-test-checks` → `Ran 7 tests in 0.302 s`, OK. Every witness binds a
  `gate-slot-test-<uuid>` namespace, so it never touches the production names its own worker
  is holding.
- kondo: **n/a** — no Clojure changed; `battery_parallel_runner.clj` is byte-identical to
  `1c762f73` (`git diff 1c762f73 -- …` is empty).
- Full gate, one run, log `/var/tmp/forge/plan2/cellC/make-test.log`:

```
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 10647}
gate-stage: {:target "battery-fresh",                      :exit 0, :wall-ms  1859}
gate-stage: {:target "alias-migration-test",               :exit 0, :wall-ms 85720}
gate-stage: {:target "mcp-test",                           :exit 0, :wall-ms 90991}
gate-stage: {:target "test-bb",                            :exit 0, :wall-ms 71934}
gate-stage: {:target "repository-hygiene",                 :exit 0, :wall-ms  1879}
gate-stage: {:target "intent-audit",                       :exit 0, :wall-ms  1910}
EXIT=0 WALL_S=172
```

  Suite lines: `Ran 164 / 898 / 891` tests (3,495 + 11,197 + 7,888 assertions),
  **0 failures, 0 errors** in each. Receipt `target/landing-gate.edn`: `:state :passed`,
  `:landing? true`, `:problems []`, `:capacity {:cpus 16 :memory-mib 24106 :lanes 8 …}`,
  `:peak-worker-count 8`.
- Mechanism proof from the run itself: I deleted `/var/tmp/forge/gate-slots` before starting,
  and after a full eight-worker gate `ls -d /var/tmp/forge/gate-slots` is
  *No such file or directory* — the gate ran entirely on kernel names. `ss -xl | grep -c
  clj-surgeon-gate` is **0** afterwards: no name outlives its holder.

## Round-2 history, kept because it is the lesson

R2 (`a966ca17`, superseded) was itself caught red by its first full gate: I published the root
identity as a bare `dev:ino` read unconditionally from the environment, gate workers inherit
that variable, and this module's own tests admit against their own roots — three existing
witnesses failed with `slot root replaced` even though all nine passed bare. That was the
"ambient state is an invisible precondition" class. The oracle is now run **both** bare and
inside the gate as standing practice, which is how the current round was checked too.

The larger lesson is Sol's: R2 detected the bad state, R3 showed detection cannot be complete
at the pathname layer, and the fix that holds is the one where the state cannot be
constructed. The R2 defense-in-depth machinery is gone rather than kept alongside — keeping it
would have implied the pathname mechanism still had a role.

## Caveats and known scope

- The commit is local. **No push**, per the brief. Note for whoever lands this: the
  superseded R2 commit `a966ca17` IS on `origin/astra/gate-lanes` — it was pushed by the
  fence/ship apparatus when it sealed that candidate, not by me. `2dfbc286` is one ahead and
  unpushed, so the remote branch currently carries the design Sol rejected in R3.
- Abstract socket names are scoped to the **network namespace**, not to a user or a checkout.
  This is wider than the old `/var/tmp/forge/gate-slots` root, which was per-user: two
  different users running this gate on one box now share one admission budget. For a box-wide
  worker budget that is the intended semantics — but it is a real behavior change, and a
  containerized or netns-isolated runner would get its own independent budget.
- `hold_admission` spins on bind with a 2 ms sleep and a 60 s deadline
  (`gate-refused: admission is held too long`). A holder that dies releases admission in the
  kernel, so the deadline only bounds a live pathology, not a crash.
- `MAX_SLOTS = 64` bounds how far above the live width occupancy is counted. A width above 64
  would stop counting high holders; the derived width on this box is 8.
- I did not re-run Astra's retained 2,655-sample two-gate experiment (timebox). The
  `:peak-worker-count 8` against `:lanes 8` in this run is the fresh box-scale bound evidence.
