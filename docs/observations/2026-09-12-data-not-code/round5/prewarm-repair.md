# First prewarm: operator worktree contamination

The first prewarm command exited 2 after 204170.225277 ms. Recovery passed;
alias and BB runtime pools passed. MCP failed isolation, not an assertion.
clj-surgeon.rename-alias-test executed 8 tests / 147 assertions, fail 0 / error 0,
but detected exactly two TEST-ISO-003 working-tree changes:

- docs/observations/2026-09-12-data-not-code/round5/focused.clj created;
- docs/tech-tree.md modified.

These were this agent's concurrent documentation writes, not test side effects.
The gate correctly refused them. Retained full pool receipts are in prewarm-red/.
No product code or test is changed to hide that result. The one repair is to
finish report/supporting documentation preparation before the final invocation
and perform no repository mutation until that invocation finishes. This is the
single allowed final prewarm retry; make test-fast is not repeated.
