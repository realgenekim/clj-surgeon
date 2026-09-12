# Step 1: blocked before measurement

The resealed one-suite JVM lease clarification is accepted. Attempt 2's
peak-process interpretation is not carried forward as a blocker.

The required base checkout must be under `/var/tmp/forge/bbtower-fx`.
The active packet's Landlock write roots omit that directory. A direct
zero-JVM directory creation probe returned exactly:

```text
fixture-root mkdir: PermissionError(13, 'Permission denied')
```

Probe path: `/var/tmp/forge/bbtower-fx/attempt3-admission-3c45c1c3`.
No directory was created. The launched environment and the source that
constructs its write roots are retained in [admission-evidence.json](admission-evidence.json).
`/home/forge/bin/lib/packet.py:166` admits only the owner checkout, packet
client/temp directories, owner Git directory and common object store.

| Ordered measurement | Summed 61-member namespace wall | Makespan |
|---|---|---|
| (a) Base coordinator at eae1e43280635be9b4e317e176d29476fd358702 | Not measured | Not measured |
| (b) Subject coordinator, bb children disabled | Not measured | Not measured |
| (c) Subject coordinator as shipped | Not measured | Not measured |

Ten largest a-to-c deltas: not measured. Slowdown cause: unknown.
The historical 68,210 ms fast sum is not a fresh observation. Source inspection
shows runtime partitioning, but does not distinguish runtime cost, contention,
heap, width or startup as a measured cause.

Stop authority: Gene or registered owner Fable, under the packet's frozen
protocol rule. Unblock by authorizing a fixture beneath an admitted temp root
(with standalone Git metadata there), or resealing access to the prescribed
fixture and any Git metadata needed for its detached checkout. No path
substitution, checkout relocation or environment escape was attempted.
