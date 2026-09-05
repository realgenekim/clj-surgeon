# Suite spike round one — full per-namespace timing table

Companion to `2026-09-04-suite-spike-round1.md`. One JVM, `clojure -M:clj-surgeon/mcp-test`,
production namespace order, 2026-09-04 17:36:27Z-17:48:28Z on Anvil (load 5.18 -> 5.21, 19-22
foreign JVMs). 716.7 s total, 865 tests, 0 failures. "sampled child procs" is the count of
DISTINCT child argv seen by a 40 ms `ProcessHandle/current .descendants` sampler while that
namespace ran (unresolvable entries dropped), so it is a lower bound on process launches.

| # | namespace | wall s | % | cum % | tests | asserts | sampled child procs |
|---|---|---:|---:|---:|---:|---:|---:|
| 1 | `clj-surgeon.reader-eval-fence-test` | 464.92 | 64.9% | 64.9% | 7 | 74 | 2 |
| 2 | `clj-surgeon.mcp-relation-census-launcher-test` | 63.71 | 8.9% | 73.8% | 5 | 186 | 3 |
| 3 | `clj-surgeon.mcp-alias-migration-test` | 59.89 | 8.4% | 82.1% | 126 | 3148 | 1 |
| 4 | `clj-surgeon.mcp-relation-census-test` | 36.21 | 5.1% | 87.2% | 82 | 2533 | 2 |
| 5 | `clj-surgeon.mcp-prepared-wire-test` | 18.57 | 2.6% | 89.8% | 3 | 27 | 7 |
| 6 | `clj-surgeon.txn-journal-test` | 16.97 | 2.4% | 92.1% | 80 | 545 | 1 |
| 7 | `clj-surgeon.mcp-hot-verify-test` | 10.02 | 1.4% | 93.5% | 4 | 19 | 0 |
| 8 | `clj-surgeon.admit-patch-test` | 6.48 | 0.9% | 94.4% | 114 | 2122 | 19 |
| 9 | `clj-surgeon.mcp-compact-relations-test` | 6.37 | 0.9% | 95.3% | 8 | 473 | 0 |
| 10 | `clj-surgeon.mcp-tool-test` | 6.03 | 0.8% | 96.2% | 48 | 490 | 0 |
| 11 | `clj-surgeon.outline-differential-test` | 5.67 | 0.8% | 97.0% | 5 | 17 | 0 |
| 12 | `clj-surgeon.mcp-process-test` | 3.59 | 0.5% | 97.5% | 19 | 70 | 9 |
| 13 | `clj-surgeon.core-discovery-test` | 3.01 | 0.4% | 97.9% | 7 | 35 | 1 |
| 14 | `clj-surgeon.mcp-http-server-test` | 1.28 | 0.2% | 98.1% | 13 | 158 | 0 |
| 15 | `clj-surgeon.mcp-operation-registry-test` | 0.87 | 0.1% | 98.2% | 1 | 51 | 0 |
| 16 | `clj-surgeon.mcp-cold-verify-test` | 0.59 | 0.1% | 98.3% | 7 | 50 | 5 |
| 17 | `clj-surgeon.scope-stream-test` | 0.45 | 0.1% | 98.3% | 15 | 78 | 0 |
| 18 | `clj-surgeon.mcp-inspect-tool-test` | 0.30 | 0.0% | 98.4% | 34 | 242 | 0 |
| 19 | `clj-surgeon.outline-memory-test` | 0.19 | 0.0% | 98.4% | 4 | 8 | 0 |
| 20 | `clj-surgeon.mcp-change-buffer-test` | 0.14 | 0.0% | 98.4% | 30 | 248 | 0 |
| 21 | `clj-surgeon.workspace-onboarding-test` | 0.13 | 0.0% | 98.4% | 17 | 95 | 0 |
| 22 | `clj-surgeon.census-pool-test` | 0.13 | 0.0% | 98.4% | 3 | 8 | 0 |
| 23 | `clj-surgeon.mcp-inspect-contract-test` | 0.13 | 0.0% | 98.5% | 20 | 261 | 0 |
| 24 | `clj-surgeon.mcp-compact-location-test` | 0.10 | 0.0% | 98.5% | 9 | 101 | 0 |
| 25 | `clj-surgeon.mcp-create-files-test` | 0.06 | 0.0% | 98.5% | 17 | 97 | 0 |
| 26 | `clj-surgeon.repository-hygiene-test` | 0.05 | 0.0% | 98.5% | 4 | 14 | 0 |
| 27 | `clj-surgeon.mcp-program-tool-test` | 0.05 | 0.0% | 98.5% | 8 | 54 | 0 |
| 28 | `clj-surgeon.mcp-intent-contract-test` | 0.04 | 0.0% | 98.5% | 11 | 23 | 0 |
| 29 | `clj-surgeon.mcp-prepared-request-test` | 0.04 | 0.0% | 98.5% | 13 | 169 | 0 |
| 30 | `clj-surgeon.mcp-extraction-test` | 0.04 | 0.0% | 98.5% | 7 | 60 | 0 |
| 31 | `clj-surgeon.mcp-contract-test` | 0.04 | 0.0% | 98.5% | 33 | 476 | 0 |
| 32 | `clj-surgeon.mcp-prepared-confirmation-test` | 0.03 | 0.0% | 98.5% | 19 | 92 | 0 |
| 33 | `clj-surgeon.mcp-relation-census-round20-test` | 0.03 | 0.0% | 98.5% | 3 | 21 | 0 |
| 34 | `clj-surgeon.mcp-write-refusal-test` | 0.02 | 0.0% | 98.5% | 6 | 54 | 0 |
| 35 | `clj-surgeon.mcp-combinable-transaction-test` | 0.02 | 0.0% | 98.5% | 16 | 62 | 0 |
| 36 | `clj-surgeon.mcp-extraction-plan-test` | 0.02 | 0.0% | 98.5% | 10 | 47 | 0 |
| 37 | `clj-surgeon.quoted-var-refs-test` | 0.02 | 0.0% | 98.5% | 5 | 36 | 0 |
| 38 | `clj-surgeon.mcp-server-test` | 0.01 | 0.0% | 98.5% | 6 | 83 | 0 |
| 39 | `clj-surgeon.mcp-schema-test` | 0.01 | 0.0% | 98.5% | 6 | 32 | 0 |
| 40 | `clj-surgeon.mcp-recovery-test` | 0.01 | 0.0% | 98.5% | 6 | 21 | 0 |
| 41 | `clj-surgeon.mcp-telemetry-test` | 0.01 | 0.0% | 98.5% | 6 | 24 | 0 |
| 42 | `clj-surgeon.mcp-workspace-test` | 0.01 | 0.0% | 98.5% | 5 | 34 | 0 |
| 43 | `clj-surgeon.mcp-compact-edit-test` | 0.00 | 0.0% | 98.5% | 3 | 24 | 0 |
| 44 | `clj-surgeon.mcp-compact-edit-fields-test` | 0.00 | 0.0% | 98.5% | 2 | 442 | 0 |
| 45 | `clj-surgeon.mcp-read-request-normalization-test` | 0.00 | 0.0% | 98.5% | 5 | 42 | 0 |
| 46 | `clj-surgeon.mcp-operation-async-test` | 0.00 | 0.0% | 98.5% | 3 | 22 | 0 |
| 47 | `clj-surgeon.mcp-operation-test` | 0.00 | 0.0% | 98.5% | 4 | 28 | 0 |
| 48 | `clj-surgeon.mcp-semantic-client-test` | 0.00 | 0.0% | 98.5% | 5 | 17 | 0 |
| 49 | `clj-surgeon.mcp-paths-test` | 0.00 | 0.0% | 98.5% | 1 | 10 | 0 |
