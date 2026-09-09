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

The coordinator owns admit recovery -> battery freshness -> alias/artifact ->
JVM plus shell checks -> Babashka -> hygiene -> intent audit -> receipt. Make
keeps existing public target names. Automatic width is bounded by half the
available processors (nproc), four lanes, and a conservative memory allowance:
reserve 2048 MiB, charge 1536 MiB per lane (512 MiB heap plus native/child reserve).
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
