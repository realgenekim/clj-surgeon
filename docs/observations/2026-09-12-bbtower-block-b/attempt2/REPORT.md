NO-GO

The build is **blocked at step 1** by the packet's explicit peak limit of one
JVM. The base runs a JVM coordinator plus JVM workers. Each JVM worker also
starts an isolation JVM and waits for it. The subject uses a bb coordinator,
but retains the worker's nested JVM. Limiting worker width to one therefore
does not satisfy the limit. This is source evidence, not an executed peak
measurement.

Subject remains detached at `d4736b46e016b8244b4db0eea0519ae62768a622`.
Manifest: `4bd681b0898afdb09881555c1be0398fb84c55964345672d79c32797adcf2936`.
Only this attempt2 observation directory was written. No implementation
changes, commits, JVM launches, servers, test runs or prewarm attempts.

1. Step 1: [slowdown.md](slowdown.md) records the blocker and unmeasured
   a/b/c sums, makespans and top deltas. [admission-evidence.edn](admission-evidence.edn)
   retains exact source excerpts and a separately labeled historical base
   receipt fold. No fresh performance claim is made.
2. Step 2: coordinator repair and the one test-fast run are owed; no measured
   cause has been established. The fast ceiling remains 60,000 ms.
3. Step 3: new bb ceiling and boundary witnesses are owed. No step-2 serial
   bb sum exists, so no proportional margin or ceiling is chosen. Fable's
   ratification remains external.
4. Step 4: probe refusal registry, completeness coverage, receipt boolean
   false seams and encounter score remain owed in the frozen task order.
5. Step 5: feature-thread bb/JVM walls, meter.tsv, measured classification
   and test-integration remain owed. The integration ceiling is 240,000 ms.
6. Step 6: [gate.md](gate.md) records zero attempts and zero emitted census
   or budget lines. The manifest's battery obligation is also owed.

The least certain point is the slowdown cause: no new evidence discriminates
contention, heap, worker width or startup. The complete battery descendant
peak is also unknown; the worker's two-JVM minimum is not a proposed allowance
for the whole gate.

The disagreement is between the required frozen launch topology and the
one-live-JVM contract. Fable must authorize and reseal either an allowance
covering all concurrent JVM parents and children, or a revised matched
launcher protocol preserving isolation within one JVM. Independent acceptance
remains external. The machine-readable [report.edn](report.edn) includes every
required field and named owners/unblock conditions.
