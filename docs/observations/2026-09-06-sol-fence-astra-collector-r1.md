## 1. Verdict — HOLD

One blocking documentation defect remains.

## 2. Blocking counterexample

The root-only diagnostic installs a global usage-window marker at [2026-09-06-astra-usage-after-mcp-resume.md:3](/home/forge/src/clj-surgeon-fence/docs/observations/2026-09-06-astra-usage-after-mcp-resume.md:3), despite explicitly sampling one Codex session and excluding Claude.

That contradicts the owning plan at [agent-usage-typed-results.md:18](/home/forge/src/clj-surgeon-fence/docs/plans/agent-usage-typed-results.md:18) and the retained review instruction not to advance the fleet marker. Execution confirmed the consequence: a default collection now selects `since=2026-09-06T08:45:00Z` from this file, skipping unsampled fleet history after the previous `04:44:02Z` marker.

Remove the active marker or otherwise prevent this narrow diagnostic from becoming the default fleet cutoff.

## 3. Collector verification

- `make study-agent-usage-self-test`: PASS.
- Fresh identical-bounds root-only collection: `status=ok`.
- After removing only volatile `generated_at` and symlink-derived `session_key`, its JSON exactly matched the corrected retained receipt.
- Reproduced: 3 inspect, 2 edit; 4 typed successes, 1 `invalid-intent-form` refusal.
- Reproduced D1–D4 bound rollouts:

  - D1: 9 attempted / 7 refused / 2 succeeded
  - D2: 9 / 7 / 2
  - D3: 9 / 7 / 2
  - D4: 10 / 7 / 3

For every rollout, attempted count equaled result-action count; the exec literal plus completed `McpToolCall` was not counted as two attempts.

## 4. Receipt and scope audit

All numerical claims in the corrected study reproduced, including route labels, 1.744-second outer wall, six service calls/1.089-second service wall, 1,998 source characters, 23 file reads, 50.58-minute clipped turn, and clock percentages/minutes.

The cold report carries the 09:52Z builder-overlap disclosure and clearly marks 3.02× as potentially contaminated. Cohort, capability-probe, mission-reconciliation, warm-transition, and Spark claims inspected were supported by their retained receipts.

The delta contains only Python and documentation, passes `git diff --check`, and contains no prohibited `.clj`, `Makefile`, `deps.edn`, or `bin/` change.