# Battery launcher matrix

Gene's 2026-09-08 brief authorizes this branch-only TEST-ISO-014 change through
commit using linked-intent-testing. The measured serial loop costs 204–206 s
and bounds the prior 216 s eight-lane makespan.

Contract: expose the exact six runtime/build-file pairs as stable independent
deftests, preserving the launch body, assertions and cleanup. Prove pair set
and cardinality from the loaded cells against a frozen literal. Keep fixture
and test-ns-hook refusal, union verdicts, and summed budget semantics intact.
Measure each deftest rather than dividing an aggregate shard's wall.

Gates: fail-first coverage and grouped-wall witnesses; cold affected namespaces;
serialized `~/bin/clj-kondo`; exactly one locked `make test-battery` with default
eight lanes and prerequisites enabled. Only running updates the walls file.
Report measured walls, makespan against 216 s, counts, verdict and commit SHA
at `/var/tmp/forge/plan2/cellC/astra-battery-floor-report.md`, then stop without
pushing. Evidence belongs under `/var/tmp/forge/battery-floor/`.
