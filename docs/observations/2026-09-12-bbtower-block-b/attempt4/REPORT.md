NO-GO

Build status: **blocked at step 1**. The required base checkout failed with
exit 128 because the sealed Landlock roots omit its Git registration path:

```text
Preparing worktree (detached HEAD eae1e432)
fatal: could not create directory of '/home/forge/src/clj-surgeon/.git/worktrees/base-checkout': Permission denied

```

Subject remains detached at `8217165b14a697df8446b164e4599065eea7943f`.
Manifest: `ce99e35ffc7e4d4492da8b0cf56df0e9d7e5cb7bfb1342577c07b39e58858597`.
Recorded UTC: 2026-09-12T06:33:04.854553+00:00.

The checkout destination is permitted, but `/home/forge/src/clj-surgeon/.git/worktrees/base-checkout`
is a sibling of the permitted owner registration, not beneath it. The roots
are constructed at `/home/forge/bin/lib/packet.py:166`.
[Admission evidence](admission-evidence.json) records the sealed roots and
confirms neither the checkout nor its registration was created. The
[creation receipt](base-checkout.json) retains exact argv, output and clocks.

1. Step 1: [slowdown.md](slowdown.md) records the blocker. All a/b/c sums,
   makespans and top deltas are unmeasured. [meter.tsv](meter.tsv) contains
   only the failed 15 ms Git registration, explicitly not a suite wall.
2. Step 2: measured-cause repair and test-fast remain owed. The fast ceiling
   remains 60,000 ms; no unsupported cause or speed fix was chosen.
3. Step 3: new bb ceiling, derivation and boundary witnesses remain owed.
   No step-2 sums exist. Fable's ratification remains external.
4. Step 4: probe refusal registry, completeness coverage, boolean false seams
   and encounter score remain owed in the frozen order. No placeholder
   registry was created to satisfy the apparatus's deliverable-existence check.
5. Step 5: feature-thread runtime measurements and integration remain owed.
   The integration ceiling stays 240,000 ms.
6. Step 6: [gate.md](gate.md) records zero builder prewarm attempts and no
   emitted census/budget lines. The registered battery check remains owed.

Only this attempt4 observation directory was written. No implementation,
classification, budget, instruction plate, shared install or Git commit
changed. No JVM, server, sub-agent or additional model was launched.

Gene or Fable must reseal the required registration access or authorize an
owned Git repository for the fixture. The one-suite lease interpretation is
accepted; this is a filesystem execution blocker. The frozen protocol was
not replaced with a different clone/registration strategy.

Least sure: slowdown cause, bb margin, and untested later prerequisites.
[report.edn](report.edn) contains all required vector fields, named obligation
owners and unblock conditions. Independent acceptance remains external.
