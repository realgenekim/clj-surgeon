# Commit Counterfactual Replay

## Outcome

Use real changes from repository history to compare editing routes without inventing toy tasks.
For each selected commit, its parent is the factual starting world and the child commit is a hidden
oracle. Fresh agents receive the original intent but not the child diff, then attempt the change in
separate counterfactual tool universes.

The experiment answers a narrower and more useful question than “which tool is fastest on a
one-line edit?”:

> Given the changes we actually needed in production, which editing route most reliably reaches
> the known-good result with the least agent effort, latency, and machine cost?

## Experimental worlds

Each case begins at the same parent commit in an isolated worktree.

| Arm | Available editing route | Purpose |
| --- | --- | --- |
| A — native | Normal bounded reads plus native patch/edit tools | Fearsome baseline; measures the platform's best ordinary workflow |
| B — structural | `inspect_clojure` plus `edit_clojure` or `apply_clojure_changes` | Measures structural addressability, guarded writes, and transactional verification |
| C — production choice | Normal installed skills and all production-eligible tools | Measures what a well-routed agent actually chooses and achieves |

An optional CLI-only structural arm can isolate MCP transport and schema ergonomics from the
underlying surgeon engine. It is diagnostic, not the primary product comparison.

## Replay capsule

Freeze every case before running any arm. A versioned capsule contains:

- repository identity and clean remote/fetch state;
- parent and child commit SHAs;
- an intent prompt reconstructed from the original issue, commit message, nearby planning docs,
  and bounded Claude/Codex history;
- provenance for every sentence in that prompt;
- permitted and protected paths;
- setup, formatter, linter, and test commands;
- dependency/cache assumptions;
- known generated files and excluded artifacts;
- time, action, token, CPU, and memory measurement rules; and
- hashes of the capsule and hidden oracle.

The child commit and its diff remain hidden from callers. A curator who can see the child writes
the intent prompt; a separate reviewer confirms that it describes the requested behavior without
leaking implementation details that were learned only from the answer.

## Case selection

Select commits before assigning tool routes. Do not select only changes that fit an existing
surgeon operation.

Start with three cases:

1. one small edit inside a known owner form;
2. one real multi-form, multi-file transaction with tests; and
3. one native-positive change where line-oriented editing should be especially competitive.

If the harness and scoring survive that batch, expand to 12 stratified cases and then 30. Later
cohorts should include:

- nested-form edits;
- insertions adjacent to named forms;
- coordinated source-and-test changes;
- refactors with renamed or moved forms;
- changes that should safely refuse stale assumptions;
- mixed Clojure/non-Clojure edits; and
- cases where structural tooling is inapplicable and production choice should route native.

Initially exclude merge commits, generated output, dependency-lock-only changes, mechanical
format sweeps, and commits whose intent cannot be reconstructed without exposing the solution.

## What counts as success

The child tree is an oracle, not necessarily the only valid textual answer. Score in layers:

1. required tests and behavioral assertions pass;
2. changed Clojure files parse, format, and lint;
3. no protected or unrelated files change;
4. the semantic change set matches the oracle's observable contract;
5. exact tree or normalized-diff agreement is reported as a stricter secondary measure; and
6. all mutations and refusals remain attributable from tool receipts.

Capture these costs separately:

- wall time to verified completion;
- model/tool actions and failed mutation attempts;
- tokens in and out;
- files and forms read;
- cold bootstrap versus hot persistent-service time;
- peak and steady-state RSS, JVM heap, CPU time, and host load; and
- cleanup cost and leaked processes after the arm ends.

Do not hide safe refusals inside a generic failure count. Report malformed-call refusals,
stale-guard refusals, partial writes, test failures, and wrong-but-valid edits separately.

## Fairness controls

- Use the same model, reasoning level, prompt, starting tree, time budget, and warmed dependency
  state for paired runs.
- Randomize arm order within a case so cache warmth and host load do not consistently favor one
  route.
- Run enough paired repetitions to expose variance and outliers; retain raw traces.
- Separate cold service startup from task latency, but report both because laptop architecture
  decisions care about total resource cost.
- Give every arm the minimum discovery it naturally needs. A no-read native edit is a useful
  structural-addressability probe, but it is not the fair native workflow baseline.
- Pin tool versions and installed skill text in the capsule.
- Prevent agents from reading Git history, reflogs, the child worktree, benchmark answers, or prior
  runs for the same case.

## Interpretation

No single aggregate should decide the architecture.

- Native can be the speed champion for small edits after one bounded read.
- Structural MCP can win when semantic location is known but physical layout is not, when a
  transaction spans several edits, or when guarded verification replaces repeated reads.
- Production choice can outperform either forced arm by routing each change to its natural tool.
- A route that saves 10 seconds but requires a permanently expensive JVM may still lose at the
  laptop/system level.

Report paired case-level results first, then medians and confidence intervals by stratum. A claim
that MCP “beats native” is earned only if it survives representative cases, symmetric routing,
paired repetitions, and total-cost accounting.

## Harness phases

### Phase 0 — capsule proof

- Curate three commits and reconstruct intent.
- Demonstrate that a blinded human can understand each prompt without seeing the child diff.
- Prove each parent worktree builds and each child satisfies its own verifier.

### Phase 1 — small paired batch

- Run A/B/C on all three cases with fresh Sol/high callers.
- Retain prompts, action traces, patches, receipts, timings, and host telemetry.
- Review every mismatch manually before changing prompts or tools.

### Phase 2 — representative batch

- Freeze 12 cases and their strata before execution.
- Repeat paired runs enough to estimate variance.
- Use failures to improve the product only after preserving the original result; rerun as a new
  tool version, never overwrite history.

### Phase 3 — historical portfolio

- Build a candidate inventory from commit history and durable issue/session provenance.
- Expand toward 30+ high-quality capsules rather than blindly replaying every commit.
- Publish raw data, exclusions, and route-specific results alongside the conclusions.

## Acceptance gates

The first 12-case experiment is credible when:

- every case has a reviewed, non-leaking intent capsule;
- all arms start from identical verified parent trees;
- scorers do not depend only on exact textual reproduction;
- route order is randomized and model/tool versions are pinned;
- task time and service/resource cost are both reported;
- every mutation attempt has an attributable outcome;
- the full experiment can be rerun from durable manifests; and
- conclusions name the strata where each route wins instead of claiming a universal champion.

## Immediate next actions

1. Inventory recent non-merge commits and nominate six candidates across the three initial strata.
2. Select three without regard to expected winner.
3. Write and review their replay capsules.
4. Add a harness that constructs isolated parent worktrees and keeps the child oracle inaccessible
   to callers.
5. Run the three-case Sol/high pilot on Anvil, review traces, then decide whether the 12-case batch
   is ready.
