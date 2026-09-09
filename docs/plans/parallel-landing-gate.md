# Parallel landing gate (TEST-ISO-015)

## Intent and design

Gene's 2026-09-09 amendment requires one automatic gate, one shared coordinator,
complete namespace evidence, and a debugging entrance that cannot issue a landing
receipt. The unchanged d2c3aa80 baseline is recorded before implementation in
/var/tmp/forge/gate-lanes/baseline-table.md (510 s make test; separate audit 0.943 s).

Generalize battery_parallel_runner.clj in place. Battery selection, optional
per-var sharding and its ledger remain intact. Gate suites select fast+integration
from lane-manifest, the existing Babashka inventory from run_all.clj, and the two
already-required alias/artifact namespaces. Whole namespaces are process units;
fixtures and test-ns-hook are never split. All children retain the existing temp
root guard; JVM children retain home isolation and namespace probes. All fast JVM workers finish before any integration worker starts. Integration
can create checkout files that another process would observe; ordering only
within each worker is insufficient. Serial debugging keeps original manifest order. Fold JVM
isolation and budgets over the union, preserving BB's existing temp-leak contract.

The coordinator owns admit recovery -> battery freshness -> fast JVM wave ->
one shared pool of alias, JVM integration, Babashka and the sequential MCP shell
checks -> hygiene -> intent audit -> receipt. Make keeps existing public target
names. Width is the minimum of half nproc and the available memory allowance:
reserve 2048 MiB, charge 1536 MiB per worker (512 MiB heap plus native/child
reserve), additionally capped by namespace count. The fixed four-worker cap is
removed. The shell sequence occupies one slot in the same executor.
At least one lane is allowed only when the memory allowance can fund it; otherwise
refuse. Unknown memory refuses; NIO reads procfs directly because buffered slurp
fails on this JDK. Gate JVM options are fixed at Xms64m/Xmx512m. Battery options are unchanged.
Serial debugging is explicit and prints SERIAL/NOT-A-GATE. It writes diagnostic
child evidence under a separate directory and never publishes landing-gate.edn.

The final receipt binds the input source digest, tree-discovered namespace census,
selected and observed inventories, lane count, every child exit, namespace counters
and walls, zero skips, isolation/leak results, and each prerequisite stage exit.
A missing/duplicate/unexpected namespace, child result, or nonzero exit is red.
A changed input digest during the run is red. Freshness does not replace any
stage. Incomplete runs remove the previous receipt before beginning.

## Requirements and misreadings

TEST-ISO-015 is specified in the owning test-isolation leaf. Wrong readings to
exclude: CPU width is an operator flag; equal grand totals prove parity; a missing
child contributes zero tests; a serial diagnostic is landing proof; per-shard
budgets replace union budgets; BB needs JVM-only resource probes; historical walls
are authority for membership; passing a stale child file is current evidence.

## Behavior matrix and verification

Pure witnesses cover CPU/memory boundaries, every proper subset/duplicate/extra
of a small inventory, malformed and missing child receipts, nonzero child exits,
per-namespace parity drift and debug receipt refusal. Existing TEST-ISO-013 tests
retain packing, fixture/hook refusal, budget and named-skip witnesses. Run new
witnesses red before implementation, green afterward. Recompute count pins from
the tree; never guess census or intent totals.

Fence: one-lane and automatic-N controls for each runtime with per-namespace
counter comparator; named deliberately failing test and nonzero exit; three timed
locked full make test runs, audit, formatter, ~/bin/clj-kondo, existing caller
inspection and land dry path if available. Evidence lives under
/var/tmp/forge/gate-lanes. Report only at completion or 90 minutes to the exact
requested external report path. No push; commit as forge-anvil with Gene trailer.

## Round two: one shared pool (2026-09-09)

The requested second round removes the fixed four-worker cap. Width is
min(floor(nproc/2), floor((MemAvailableMiB-2048)/1536)), capped by the
number of namespace jobs (a single-CPU machine retains one funded worker).
Recovery and freshness still precede consumers. All fast JVM snapshots finish
before integration, alias, BB and shell work starts in the shared pool. The
MCP shell checks retain their existing internal sequence as one pool job;
that job spends one slot, so three suites never multiply the width budget.
Hygiene and intent audit run after all pool jobs. Each suite retains its own
namespace census, counters, budget fold and child receipts. The executor is
shared with component and battery execution; only gate scheduling changes.

The motivating TEST-ISO-007 failure was a whole-namespace wall oracle, not a
coordinator deadline. Its declared compact-relations override is 18,000 ms,
about twice the measured eight-worker 8,836 ms; exact-boundary tests preserve
refusal at 18,001 ms. CPU time would change the existing wall promise and omit
waits or delegated work. The fast-lane summed budget remains 60,000 ms.

The concurrency witness rendezvous requires alias, MCP and BB jobs to overlap
inside the same three-slot executor. It also observes the peak and completed
job identities. At the fence, compare all namespace counters against explicit
width-one debugging, repeat named-failure/lost-shard/unreadable-census faults,
and retain three consecutive locked full timings. Sample MemAvailable every
second and report its minimum alongside actual active jobs. Compare the
checkout-isolation alternative with the retained barrier before claiming its
cost. No installed caller edits and no push. The exact report path is
/var/tmp/forge/plan2/cellC/astra-gate-lanes-r2-report.md; interim at 60 minutes,
stop at 90 minutes from 04:47:25Z.

The first complete shared-pool trial took 173.191 s: 27.811 s fast wave,
125.436 s mixed pool, 8 peak jobs, and 17,987 MiB minimum MemAvailable.
A separate-checkout MCP probe with local fast-before-integration ordering took
62.898 s plus 3.063 s setup, versus 86.011 s for the shared-checkout barrier
control. Dropping local ordering caused six runtime-config mutation violations;
separate roots alone are insufficient. The measured MCP saving is 20.049 s.
The normal gate retains the shared-checkout barrier; the full mixed pool on
separate roots is unmeasured. Evidence and final repetitions are in the external
round-two report. Pool job exceptions drain siblings before returning failure.

TEST-ISO-015 cold-start ratchet: before any JVM shard starts, the coordinator
resolves the test-deps classpath once with bounded `clojure -Spath`. The first
cold-clone named-failure run also lost two children to `ClassNotFoundException:
clojure.main`; parallel CLI starts shared an initially absent .cpcache entry.
Classpath readiness is coordinator work, not a namespace budget exception.
A nonzero or empty classpath refuses before fan-out. BB-only jobs need no JVM
classpath preparation. The receipt records this preparation wall separately.
Witness: `battery-parallel-test/a-cold-checkout-prepares-the-worker-classpath`;
the real empty-cache named-failure gate must retain every namespace and show
only the deliberately failing assertion.
