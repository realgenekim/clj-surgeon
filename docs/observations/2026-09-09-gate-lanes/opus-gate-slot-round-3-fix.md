# gate_slot: a deleted slot root can no longer create a second semaphore

Sol GATE-LANES-FENCE-001-R2, repaired at the rung the brief named: a typed refusal, not
detection-after.

| item | value |
|---|---|
| worktree | `/home/forge/src/clj-surgeon-astra-consult`, branch `astra/gate-lanes` |
| base | `1c762f73bf49db62a1996b2e2afd8b76badb8f8f` |
| commit | **`a966ca17b3255edb05c67ecdfddbeb8e037f5558`**, author `forge-anvil <forge-anvil@anvil>`, Gene + Fable trailers, **not pushed** |
| files | `test/gate_slot.py`, `test/oracles/test_gate_slot.py`, `test/clj_surgeon/battery_parallel_runner.clj` |
| full gate | `flock /home/forge/tmp/suite.lock make test` — **EXIT 0, wall 171 s** |

## Sol's attack, executed both ways

Production `try_acquire`, live width 1, root removed while the first handle is held, own
root under `/var/tmp/forge`.

| | pre-fix (`git show HEAD~1:test/gate_slot.py`) | post-fix |
|---|---|---|
| `first_held` | True | True |
| `root_removed` | **False** — recreated under the live holder | **True** — still gone |
| `second_admitted` | **True** | **False** |
| `derived_width` | 1 | 1 |
| `bound_violated` | **True** | **False** |
| refusal | none | `gate-refused: slot root missing` |

The pre-fix row is my own executed reproduction of Sol's finding, not a quotation of it.
The post-fix row is asserted as one dict literal in
`test_removed_slot_root_refuses_and_never_splits_the_semaphore`, so the attack is now a
regression test rather than a review note.

## Two-coordinator witness

`test_two_coordinators_share_one_bound_over_one_root_inode`: two separate coordinator
processes plus a third caller, one coordinator-owned root, width 2 →
`['admitted', 'admitted', 'refused']`. Green.

The box-scale two-coordinator evidence is the full gate itself: `make test` runs nested
coordinator JVMs (`alias-migration-test`, `mcp-test`, `test-bb`) that each start their own
holder against the one box root and share its admission budget. The receipt records
`:peak-worker-count 8` against a derived width of 8 — no overshoot. I did **not** re-run
Astra's retained 2,655-sample two-gate experiment; that is outside the timebox and remains
the older evidence.

## The fix

1. **`try_acquire` never mkdirs.** A missing root is `gate-refused: slot root missing`; a
   root that is not the coordinator's inode is `gate-refused: slot root replaced`. Neither
   is ever repaired by recreating the directory under a live holder.
2. **Identity after the lock, not before.** Every open is relative to one `O_DIRECTORY` fd
   (openat), and both `admission.lock` and the taken slot are re-identified by
   `(st_dev, st_ino)` — `os.fstat(fd)` vs `os.stat(name, dir_fd=…)` — *after* the flock is
   held, plus the slot's device against the directory's. Mismatch releases and refuses.
3. **The coordinator owns the root for its lifetime.** `open_root` creates it exactly once;
   `gate_slot.py --hold-root` keeps that directory fd open until its stdin closes, so the
   kernel cannot recycle the inode number behind a worker's check. `-main` in
   `battery_parallel_runner.clj` starts the holder, publishes `<path>|<dev>:<ino>` as
   `GATE_SLOT_ROOT_ID` to every worker it spawns (all four process sites), and destroys the
   holder in a `finally`. A nested coordinator inherits the value and refuses to coordinate
   over a second inode.

## The defect the gate found in my own fix — and its ratchet

The first full `make test` was **red (EXIT 2)**: `shell-exit 2`, three pre-existing
witnesses failing with `gate-refused: slot root replaced`
(`test_killed_owner_and_failed_exec_release_slots[fail_exec=True]`,
`test_unknown_capacity_refuses_and_does_not_leak_admission_lock`, and my new two-coordinator
witness returning `['', '', '']`). Cause: I published the identity as a bare `dev:ino` and
read it from the environment unconditionally. Gate workers inherit that variable, and this
module's own tests admit against their own temporary roots — so every one of them was judged
against the box root and refused. All nine tests had passed when run bare; the ambient env
var was an invisible precondition, visible only inside the gate.

Repair: the published value carries the path (`<abspath>|<dev>:<ino>`) and `_expected_from_env`
returns it only for the root it names. Ratchet:
`test_published_identity_binds_to_its_own_root_path` asserts an unrelated root is still
admitted while the box identity sits in the environment, and the oracle is now run **both**
bare and with `GATE_SLOT_ROOT_ID` set (both 9/9 green) before any gate run.

## Evidence

- Focused: `python3 -B -m unittest discover -s test/oracles -p test_gate_slot.py` — **9 tests,
  OK**, 0.25 s. Same, with `GATE_SLOT_ROOT_ID="/var/tmp/forge/gate-slots|2049:5353195"` —
  **9 tests, OK**.
- `~/bin/clj-kondo --lint test/clj_surgeon/battery_parallel_runner.clj` — 0 errors, 0 warnings.
  (kondo n/a for the Python halves.)
- Full gate, one run, log `/var/tmp/forge/plan2/cellC/make-test.log`:

```
gate-stage: {:target "admit-transaction-recovery-battery", :exit 0, :wall-ms 10892}
gate-stage: {:target "battery-fresh",                      :exit 0, :wall-ms  1882}
gate-stage: {:target "alias-migration-test",               :exit 0, :wall-ms 87485}
gate-stage: {:target "mcp-test",                           :exit 0, :wall-ms 90158}
gate-stage: {:target "test-bb",                            :exit 0, :wall-ms 71921}
gate-stage: {:target "repository-hygiene",                 :exit 0, :wall-ms  1807}
gate-stage: {:target "intent-audit",                       :exit 0, :wall-ms  1942}
EXIT=0 WALL_S=171
```

  Suite lines: `Ran 164 / 898 / 891 tests` (3,495 + 11,197 + 7,888 assertions),
  **0 failures, 0 errors** in each; oracle blocks `Ran 9 … OK`, `Ran 4 … OK`, `Ran 3 … OK`.
  Receipt `target/landing-gate.edn`: `:state :passed`, `:landing? true`, `:problems []`,
  `:peak-worker-count 8`, `:git-head "1c762f73…"` (the digest is the base tree — the gate ran
  before the commit, which is the same working tree).

## Caveats

- The commit is local. **No push**, per the brief.
- `:git-head` in the receipt names the base sha because the gate ran on the working tree
  immediately before committing; the tree it measured is byte-identical to `a966ca17`.
- The residual inode-reuse window is closed only while the coordinator's holder is alive. A
  root removed after the coordinator exits and recreated before the next one starts is a new
  run with a new identity, which is correct; a holder killed mid-run makes the next worker
  refuse, which is the intended fail-closed direction.
- I did not re-run the retained 2,655-sample two-gate experiment (timebox).
