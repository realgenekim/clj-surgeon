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

This whole-task replay must be paired with a second clock boundary for the editor-golf claim.
Once the model can state the complete mechanical decision, compare one compiled Surgeon
transaction with native materialization. The whole-task replay locates the crossover; the
decision-boundary replay tests the >=5x `think -> compile -> bang -> verify` goal. Do not dilute
one question with the other.

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

## Pilot cohort frozen on 2026-08-24

The first three cases were selected by change shape before their diffs were reviewed:

| Case | Parent → child | Stratum | Targets |
| --- | --- | --- | ---: |
| `cclsp-optional` | `cde6fc5b` → `6948f0eb` | small owner edit | 2 |
| `plain-nrepl` | `0dede868` → `28aad1cb` | native-positive source and test | 2 |
| `failure-atomic-commit` | `5d30900e` → `7af69092` | multi-form source/test/docs transaction | 3 |

The frozen alternates are `d0afc776`, `e93faa7b`, and `44bf0cb7`. Treatment results must not cause
case substitution.

Versioned capsules and blinded task prompts live under
`bench/counterfactual-replay/cases/`. The zero-model verifier proves commit ancestry, parent/child
tree identity, exact changed paths, line counts, target hashes, and Clojure parsing. The
materializer exports only a parent tree into an unrelated one-commit repository with no remote,
then writes the provenance receipt outside the caller workspace.

Local gates passed before Anvil dispatch:

- capsule verifier: three cases, seven targets;
- materializer: clean one-commit workspace, no remote, no task/capsule/oracle inside;
- existing-destination refusal: passed without altering the first workspace;
- `cclsp-optional` child: 604 tests, 5,222 assertions, zero failures/errors; and
- `failure-atomic-commit` child: 505 tests, 4,050 assertions, zero failures/errors.

The `plain-nrepl` child deliberately remains an Anvil gate: its historical verifier starts a JVM.
The local flight recorder was already yellow after the two parallel Babashka suites, so launching
another 512 MiB JVM locally would violate the load-safe test policy rather than strengthen the
experiment.

The first Anvil shakedown amended the `cclsp-optional` verifier before any result was admitted. Its
historical full suite has an unrelated `case*` incompatibility on the Anvil Babashka version, while
the changed `workspace-onboarding-test` namespace passes. The capsule now runs that focused
namespace and declares `.cpcache/` as generated output. Earlier receipts remain retained and
invalid; no score was overwritten.

### First admitted case

`cclsp-optional` produced three semantically correct, non-byte-identical implementations:

| Route | Complete-turn wall | Tool actions | MCP actions |
| --- | ---: | ---: | ---: |
| Native | **148.252 s** | 20 | 0 |
| Structural MCP | 306.631 s | 16 | 8 |
| Production choice | 280.199 s | 18 | 8 |

All arms changed exactly the two allowed files and passed the focused 17-test/94-assertion
verifier. Each changed the same production string; their regression assertions differed only in
expression style. Native won this native-positive shape decisively. MCP saved four actions at best
but performed five or six structural inspections, one semantic-provider failure in the production
arm, two structural mutations, and two foreground full-suite cycles.

This is evidence for route selection and a faster verification ladder, not evidence against the
structural editor. The next case is `failure-atomic-commit`: a 340-line source/test/docs change
where multi-owner transaction and proof density should have a fair chance to cross over. Its
authoritative verifier is the affected intent-transaction namespace; the task explicitly keeps the
unrelated historical full suite out of the foreground timing.

### Second admitted case

`failure-atomic-commit` established a crossover without approaching the final speed goal:

| Route | Complete-turn wall | Tool actions | MCP route |
| --- | ---: | ---: | --- |
| Production choice | **413.540 s** | 22 | seven inspections; native writes |
| Native | 487.903 s | 24 | none |
| Forced structural | 660.644 s | 24 | six inspections; four applies |

All three routes changed only allowed paths and passed the focused 19-test/145-assertion
verifier. Production was 15.2% faster than native and 37.4% faster than forced structural use.
The production caller selected a hybrid route: structural perception plus native
materialization. The forced structural caller paid two inspection refusals and two apply
refusals before succeeding.

This result proves that selective routing can cross over on a real multi-owner change. It does
not satisfy the editor-golf goal. The follow-on decision-boundary experiment will derive an exact
decision packet from the historical child without exposing its patch, start the clock after
architectural discovery, and permit one foreground verification cycle. Its acceptance gate is
>=5x complete materialization wall, exact semantic verification, zero failed mutations, and no
source reacquisition for already-addressed owners.
