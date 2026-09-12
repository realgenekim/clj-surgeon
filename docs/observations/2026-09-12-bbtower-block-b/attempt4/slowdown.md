# Step 1: blocked before measurement

The required detached checkout could not be registered. The checkout path
`target/base-checkout/` is gitignored and inside the owner worktree, but Git
also needs a new sibling registration under the common repository's
`.git/worktrees/`. The sealed write roots admit only the existing owner
registration and object store; they do not admit the new registration.

Executed once, with hooks disabled for the fixture creation:

```text
git -c core.hooksPath=/var/empty worktree add --detach /home/forge/src/clj-surgeon-bbtower/target/base-checkout eae1e43280635be9b4e317e176d29476fd358702
Preparing worktree (detached HEAD eae1e432)
fatal: could not create directory of '/home/forge/src/clj-surgeon/.git/worktrees/base-checkout': Permission denied

```

Exit 128. No checkout or registration was created. See
[base-checkout.json](base-checkout.json) for the argv and measured creation
wall, [base-checkout.log](base-checkout.log) for verbatim output, and
[admission-evidence.json](admission-evidence.json) for actual sealed roots.
The 15 ms creation failure is not a namespace or suite measurement.

| Required measurement | Summed 61-member walls | Makespan |
|---|---|---|
| a: base coordinator at base SHA | Unmeasured | Unmeasured |
| b: subject with bb children disabled | Unmeasured | Unmeasured |
| c: subject as shipped | Unmeasured | Unmeasured |

The ten largest a/c namespace deltas remain unmeasured. No cause is named:
contention, heap, width and startup remain hypotheses. The historical 68,210 ms
fast failure is context only and is not an attempt4 measurement.

The one-suite JVM lease clarification is accepted. The failed step is Git
filesystem admission, not the attempt2 JVM-count disagreement. No JVM launched.
The packet freezes task order and argv; Gene or Fable must reseal a usable
registration path or authorize an owned Git repository for the fixture.
