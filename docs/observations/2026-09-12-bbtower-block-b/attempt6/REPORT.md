NO-GO

Build status: **blocked at step 1 by an executed temp-isolation refusal**.
All eight base lanes exited 97; none of the required 61 namespaces ran.
The sealed `_JAVA_OPTIONS` overwrote private worker temp-directory flags, so
`secure-tmpdir!` refused `sentinel-mismatch`. This is not a 1 GiB heap stop.

Subject remains detached at `efdced9bd47dfb1265fbf9b48515ea8f89a42dc9`; base `eae1e43280635be9b4e317e176d29476fd358702` verified clean.
Manifest: `da0217373e110e79ca729d7624afc7ddb767671d89b86dad1f6ae27488a059af`. No implementation changes or commits.

Every JVM worker launched in this attempt ran at 1 GiB, not the Makefile’s 512 MiB, and under the packet temp directory, as imposed by the retained _JAVA_OPTIONS. Absolute sums under these settings are therefore not the landing gate’s numbers. The 60,000 ms and 240,000 ms verdicts inside this packet are provisional; the landing gate outside the packet (Fable’s ship path, seat environment) must re-establish them before any landing. Here no valid namespace sum or budget verdict was obtained: workers refused before testing.

1. Step 1: base failed-startup makespan **6127 ms**, complete launch **7440 ms**.
   Namespace sums and a/c deltas remain unknown. Arms b/c were not run after
   the baseline refused. [slowdown.md](slowdown.md) records the exact refusal,
   clocks, argv and the separately retained login-shell harness error.
2. Step 2: no measured cause; coordinator repair and test-fast remain owed.
3. Step 3: no measured serial bb sum; no new ceiling was invented.
4. Step 4: probe registry, completeness, boolean seams and encounter score
   remain owed in the frozen order.
5. Step 5: feature-thread comparison, classification and integration remain owed.
6. Step 6: zero prewarm attempts; [gate.md](gate.md) records that no prewarm
   budget/census lines exist. Battery and independent acceptance remain owed.

Fable or Gene must reseal a launch environment that lets each worker retain
its private startup temp directory, or authorize an equivalent protocol that
preserves the existing isolation oracle. The 1 GiB cap can remain. Matching
an environment that prevents tests from running cannot produce slowdown deltas.
The shared launcher repair remains outside this packet, as instructed.

Only attempt6 observations were written. The initial login-shell error was
corrected locally without changing frozen coordinator argv or `_JAVA_OPTIONS`;
its complete logs and receipt remain alongside the sealed-environment refusal.
No tests, budgets, classification, instruction plates or shared install changed.
Two coordinator attempts ran sequentially, with explicit heap limits; no servers,
sub-agents or additional models were launched.

Least sure: slowdown mechanism, bb ceiling and later gate prerequisites.
[report.edn](report.edn) contains all required vector fields, named owners and
unblock conditions. Independent acceptance remains external.
