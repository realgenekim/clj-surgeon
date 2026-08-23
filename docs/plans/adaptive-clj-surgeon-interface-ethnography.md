# Adaptive clj-surgeon interface ethnography

**Status:** Sol/high full-eight-task wave 1 running on Anvil
**Motivating issue/incidents:** the 2026-08-23
[Clojure tooling memory and load architecture review](../observations/2026-08-23-clojure-tooling-memory-and-load-architecture-review.md),
the [bounded cclsp lifecycle](bounded-cclsp-workspace-lifecycle.md), and the
unresolved product question of whether clj-surgeon earns its resident cost and
agent ceremony.

## Outcome

Produce decision-grade evidence about whether agents should use clj-surgeon
through its CLI, through the persistent MCP plus CLI fallback, or not at all.
The study combines privacy-safe natural-history ethnography with matched,
randomized controlled tasks on Anvil. It measures complete agent work rather
than command startup alone and ends in an explicit keep, route, simplify,
subsume, or retire decision.

Execution is adaptive. Run one three-arm triplet first, then a batch of twelve
runs, then larger batches only when the preceding receipts prove that the
instrument, task corpus, model parity, isolation, and judge are trustworthy.

## Execution checkpoint

Done:

- documented the causal question, three tooling arms, task classes, meters,
  safety invariants, batch gates, and architecture decision rules;
- inventoried the three Anvil seats with exact tmux pane, CWD, account, model,
  context, and quota receipts;
- repaired the two composer wedges caused by sending a raw slash command
  through Agent Bridge and recorded the raw-tmux versus durable-message rule;
- established `gpt-5.6-sol` high parity on `dev-a` (`/home/dev-a`), `dev-b`
  (`/home/dev-b`), and `dev-c` (`/home/dev-c`) after Gene selected Sol first;
- pre-registered the separate paired Sol/high versus Terra/high extension.
- rejected the first 12-run Sol wave because Anvil's nested Bubblewrap could
  not initialize loopback for CLI/native tools; MCP bypassed that failed
  boundary, so its apparent 2/4 versus 0/4 advantage was confounded;
- added and self-tested an explicit sandbox-mode harness control, then proved
  one corrected run in each affected arm: CLI passed in 83.7 seconds and native
  passed in 46.7 seconds;
- launched a corrected detached 24-run wave across all eight frozen portfolio
  tasks, with one arm per Anvil seat and sequential runs within each seat.

Recommended next:

1. Let the detached Sol/high full-eight-task wave finish and retain every
   terminal receipt, including failures.
2. Reject any run that lacks the expected sandbox, model, CWD, task hash, or
   arm-isolation evidence.
3. Score the corrected wave by task and arm; do not combine it with the
   invalid Bubblewrap-confounded wave.
4. Rotate arms across seats for the next valid wave before making a causal
   tooling claim.
5. Replay the accepted paired task set under Terra/high only after the Sol
   instrument and scoring are stable.

## Decision the study must support

The study is not asking only which entrance is faster. It must determine:

1. Whether structural tooling improves semantic correctness and prevents
   wrong-file, wrong-form, malformed, or over-broad edits.
2. Whether any correctness benefit reduces complete task-turn wall, agent
   actions, context/output, and recovery loops enough to justify resident
   memory and architectural complexity.
3. Whether fresh agents naturally load and use the capability, or abandon it
   after refusal, quoting, plan, or discovery friction.
4. Which effects come from structural semantics, persistent process warmth,
   cclsp, nREPL, skill instructions, or the selected model.
5. Whether the right product is one universal entrance or a task-class router
   that deliberately chooses MCP, CLI, nREPL, or native tools.

## Current Anvil preflight

Read-only `/status` receipts on 2026-08-23 showed ample quota for the
calibration and twelve-run batch:

| Seat | tmux pane | Current CWD | Account | Model | Context left | General quota | Spark quota |
|---|---|---|---|---|---:|---:|---:|
| `dev-a` | `%32` | `/home/dev-a` | `genekkanban@gmail.com` Pro | `gpt-5.6-sol`, high | 92% | weekly 100% | weekly 99% |
| `dev-b` | `%34` | `/home/dev-b` | `genekkanban@gmail.com` Pro | `gpt-5.6-sol`, high | 87% | weekly 100% | weekly 75% |
| `dev-c` | `%45` | `/home/dev-c` | `tools@itrevolution.net` Pro | `gpt-5.6-sol`, high | 81% | weekly 100% | Spark weekly 98% |

The TUI warned that readings may be briefly stale, but no seat is near a
relevant limit. Reauthentication is therefore not justified before the first
batch. The initial model mismatch was first repaired at Terra/high, then all
three seats were deliberately moved to Sol/high when Gene selected Sol as the
first model. Exact TUI receipts showed Sol/high on all three seats before
dispatch. If that exact model becomes unavailable on one seat, stop rather
than mix models. The later Terra replay must prove the same Terra model,
reasoning, account plan, and quota before its first run.

The quota probe exposed an operating defect worth retaining in the protocol:
Agent Bridge messages are durable coordinator envelopes, not raw Codex TUI
keystrokes. Sending `/status` through `agent-bridge send` made workers interpret
it as a task and wedged two composers. Exact-pane
`agent-bridge recover-composer` repaired `%32` and `%34`; bounded raw tmux
keystrokes then opened `/status`. Future TUI slash commands use the raw tmux
lane. Assignments use Agent Bridge and durable files.

## Bitter-Lesson Boundary

The harness owns mechanical facts: arm availability, fixed commits, clocks,
process/resource meters, route capture, hidden tests, diff boundaries,
randomization, and blinded artifact labeling. It must not encode which tool is
architecturally superior or reward the tool for producing its preferred form
of receipt.

The agent retains architectural judgment. Correct refusal can beat unsafe
success. Native Write remains the expected control for new files and
prose-heavy work. The corpus deliberately includes tasks where clj-surgeon
should lose so the benchmark cannot succeed merely by being Surgeon-shaped.

## Public contract

### Experimental arms

| Arm | Available tools | Product question |
|---|---|---|
| `cli` | `~/bin/clj-surgeon`; no persistent Surgeon MCP or cclsp | Are structural semantics valuable without resident services? |
| `cli-mcp` | Current production skill; persistent MCP first, CLI fallback, bounded cclsp | Does the complete product justify persistence? |
| `none` | No Surgeon skill, binary, MCP, or cclsp; native reads, search, patches, formatter, and tests | Would agents perform as well without the product? |

nREPL availability is identical across arms in the mixed semantic stratum. A
separate product-bundle analysis may credit cclsp to `cli-mcp`, but the
structural causal comparison may not hide an nREPL or semantic-service mismatch
inside the interface label.

### Batch ladder

1. **Calibration: 3 runs.** One matched task, all three arms concurrently.
   This validates the apparatus; it is not an efficacy conclusion.
2. **First evidence batch: 12 runs.** Four matched tasks, three arms each,
   with arm-to-seat rotation.
3. **Expansion: 24-run batches.** Eight matched tasks per batch. Continue only
   while uncertainty can change an architectural decision. The provisional
   decision-grade target is about 72 controlled runs, not a quota-burning
   mandate.
4. **Model-suitability extension.** After the instrumentation and primary
   three-arm comparison are stable, replay a sealed paired subset under
   `gpt-5.6-sol` high and `gpt-5.6-terra` high. The first model calibration is
   six runs: one matched task x three tooling arms x two models. Expand in
   twelve- or twenty-four-run blocks only when the result can change a routing
   decision.
5. **Optional laboratory extension.** Other cold/warm factors begin only after
   the primary three-arm and model decisions are stable.

Each batch ends with a durable receipt, validity verdict, paired outcome table,
uncertainty assessment, and explicit `continue`, `repair-and-repeat`, or `stop`
decision. Superseded invalid runs never enter the aggregate.

### Model-suitability extension

The first tooling comparison holds the model fixed at Sol/high. It must not
mix the model factor into the interface result. Once that instrument is
trusted, the extension asks a separate practical question: does Sol/high make
complete work faster or safer often enough to justify using the largest model
by default, or should model choice route by task class?

Use the same accepted tooling arms, judge, meters, and task-class definitions.
Run fresh sessions at the same fixed commit with sealed task text. Pair exact
tasks where carryover is impossible; otherwise use pre-generated isomorphic
variants with blinded equivalence checks. No worker or model run may see a
prior solution, transcript, score, or judge feedback. Randomize model order
and rotate model x arm across seats so calendar time, account, and seat are not
mistaken for model effects.

Report the model comparison within each task class and tooling arm:

- hidden acceptance, semantic correctness, refusal quality, and safety;
- complete task-turn wall and time to first useful evidence;
- action count, tool-route changes, retries, and recovery loops;
- input, output, context, and observed quota consumption;
- cold and warm process cost, reported separately from model latency.

The extension begins only after a written primary-instrument validity verdict.
It can reuse retained tasks and harness code, but it produces a separate
receipt and decision so a model advantage cannot launder an inferior tooling
architecture, or vice versa.

## Safety invariants

- A new or changed model-facing interface must pass the local interface gate
  before any Anvil run exposes it. Anvil scales an accepted interaction; it is
  not where the team discovers whether the interaction feels right.
- All arms use the exact same model, reasoning, task text, base commit, time
  budget, formatter, tests, and external permissions within a batch.
- Every run uses a fresh disposable worktree. Before dispatch, the receipt
  proves its exact CWD, commit, clean state, arm manifest, and process identity.
- No run starts the Sessionize application, connects to its production
  database, deploys, pushes, sends messages, or performs other external writes.
- Agents cannot read another arm's transcript, worktree, result, hidden test,
  score, or judge output.
- The `none` arm receives an explicit experimental override permitting native
  Clojure inspection/editing; it is not silently asked to violate repository
  instructions.
- Raw transcripts remain local. Published results use privacy-safe route
  phases, neutral repository labels, bounded task descriptions, and aggregate
  metrics.
- A wrong-file, wrong-form, destructive, or out-of-scope mutation is a safety
  failure regardless of speed.
- Correct typed refusal is scored against task intent, not automatically as
  failure. Unsafe apparent completion cannot beat refusal.
- Slash commands such as `/status` and `/model` use bounded raw tmux keystrokes
  against the exact pane. Agent Bridge carries durable task envelopes only.
- Authentication is seat-local. Never read, copy, display, or transplant
  `auth.json`; reauthenticate only after a verified account/quota mismatch.

### Local interface gate

Every new model-facing schema, tool, mode, routing rule, or default advances in
this order:

1. Pure compiler and refusal tests exhaust the request-shape matrix.
2. A real local fixture proves exact bytes, stale-target refusal, rollback,
   formatting boundaries, verification, and the terminal receipt.
3. A local interactive dogfood pass answers the qualitative question: does the
   operation feel like one obvious editor gesture rather than protocol work?
4. Fresh local clean-agent trials prove one-shot discoverability without
   transcript coaching. For the surgical-edit surface, require 10/10 exact
   one-call successes before remote scaling.
5. The admission receipt records the source hash, tool schema hash, skill hash,
   model, reasoning, tests, fixture hashes, and local clean-agent results.
6. Only that admitted hash is copied to Anvil. Each seat verifies the same hash
   before its first paid run.

A local failure returns to interface design. It must not be converted into a
larger remote prompt experiment. Existing-interface baseline experiments may
continue, but they confer no admission on a proposed interface.

## Implementation shape

### Natural-history lane

Run `make study-agent-usage` once for the exact marker-bounded window. Its full
privacy-safe receipt is the counting authority for Codex versus Claude,
skill-visible versus skill-loaded versus invoked versus successful, structural
operations, native fallbacks, route phases, direct tool wall, complete task
wall, cclsp admissions, and LSP activity.

Historical evidence explains actual adoption and failure routes. It does not
establish causal performance because tasks, callers, models, and tool exposure
were not randomized. Inspect only receipt-named narrow transcript regions when
aggregates cannot explain a route; never publish private task prose or paths.

### Controlled-task lane

Build a sealed corpus from real prior tasks replayed at fixed historical
commits plus isomorphic seeded variants:

- large-file inspection and localization;
- narrow structural edit;
- multi-form extraction/refactor;
- damaged-source, ambiguity, and refusal/recovery;
- outside-in failure followed by nREPL probes and final acceptance;
- negative controls such as new files, prose, and simple mechanical edits.

The full target corpus contains four tasks in each of these six classes. The
three-run calibration uses one medium structural task with deterministic hidden
acceptance. The twelve-run batch uses four different classes; no arm sees the
same task in a prior conversation.

### Anvil rotation

Rotate arms across seats as a Latin square so account, seat, and filesystem
effects do not become tool effects:

| Wave | `dev-a` | `dev-b` | `dev-c` |
|---|---|---|---|
| 1 | `cli` | `cli-mcp` | `none` |
| 2 | `none` | `cli` | `cli-mcp` |
| 3 | `cli-mcp` | `none` | `cli` |

Before each wave, move every worker from its home CWD into the exact experiment
worktree CWD and prove that CWD in the dispatch receipt. Put the complete
assignment in a short durable file on Anvil; the bridge message contains only
the assignment ID, path, expected SHA-256, and response route. Preflight an
empty composer because the current refusal ledger contains prior
composer-wedged and dispatch-staging failures.

### Meters

Primary outcomes:

- hidden acceptance pass/fail and semantic correctness;
- wrong-file, wrong-form, malformed, or out-of-scope changes;
- correct refusal versus unsafe apparent success;
- unintended diff size and retained behavior.

Efficiency outcomes:

- complete task-turn wall, paired p50/p90, and time to first useful evidence;
- direct tool wall reported separately;
- action count, Surgeon calls, input/output/context volume;
- recovery loops, repeated discovery, and fallback transitions;
- cold-start and warm-service strata.

Ethnographic outcomes:

- `skill visible -> skill loaded -> binary invoked -> operation succeeded`;
- dominant privacy-safe route phases;
- when available tooling is ignored or abandoned;
- refusal and recovery behavior;
- CLI quoting/plan ceremony and MCP output amplification;
- outside-in test and nREPL inner-loop use;
- self-hosted versus fresh-caller behavior.

Resource outcomes:

- broker, JVM, and cclsp process counts and exact CWDs;
- Linux cgroup peak memory, PSS/RSS, CPU time, page faults, and child starts;
- worker initialization count, resident duration, lease, and reap receipts;
- separate macOS `footprint` validation for laptop relevance.

## Batch gates

### Three-run calibration gate

All must be true before the twelve-run batch:

- three seats show identical model and reasoning;
- three exact worktree CWDs and base commits are proven;
- each arm can use only its assigned tools;
- all clocks, tool routes, process/resource meters, diffs, and hidden tests are
  captured from outside the agent;
- no worker sees another arm's output;
- the blinded judge can score arm-neutral artifacts;
- no bridge, composer, auth, quota, or production-data incident occurred;
- the receipt can be regenerated from retained raw evidence.

Failure repairs the harness and repeats a new calibration task. It does not
advance by explaining away missing data.

### Twelve-run gate

After four matched tasks, inspect paired deltas and route evidence. Continue in
larger batches only if the confidence interval or task-class disagreement can
still change one of these decisions:

- keep persistent MCP as default;
- make CLI the default and MCP optional;
- route by task class;
- subsume cclsp or another daemon;
- retire clj-surgeon.

A dramatic, safety-consistent effect may justify stopping early. Ambiguous
results justify another sealed batch, not selective task replacement.

## Pre-registered architecture rules

- An arm with an additional destructive or wrong-form failure cannot win on
  speed.
- Keep MCP as default only when correctness is non-inferior and it improves
  complete task wall by approximately 15% or action count by approximately 20%
  in the classes it claims to help.
- Prefer CLI when it retains the safety/correctness benefit within about 10%
  of MCP task wall while materially reducing persistent memory.
- Retire Surgeon when `none` matches correctness and complete wall even on
  structural and recovery tasks.
- When task classes disagree, publish a routing policy instead of forcing one
  global winner. Native tooling is expected to win some negative controls.
- Report paired distributions and uncertainty, not one blended average that
  lets many trivial tasks erase a rare safety failure.

## Pre-registered model-routing rules

- Sol cannot win on wall time alone if it introduces an additional safety or
  semantic-correctness failure.
- Prefer Terra/high as the default when it is correctness- and safety-
  non-inferior and Sol does not materially improve complete task-turn wall or
  recovery behavior.
- Prefer Sol/high for a task class when it prevents a real failure, or when its
  additional reasoning reduces complete task-turn wall enough to offset its
  observed quota and latency cost. Direct generation speed is not sufficient;
  the comparison includes retries, tool misuse, and rework.
- When results differ by task class, publish an explicit router: for example,
  Terra for routine inspection and narrow edits, Sol for ambiguous recovery or
  architecture. Do not force one global winner.
- Treat "largest model for everything is fastest" as a testable hypothesis,
  not a premise. The final recommendation states where it is true, where it is
  false, and the evidence threshold that would reverse the route.

## Test plan

1. Hermetic harness tests prove arm manifests hide and expose only intended
   tools.
2. Fixture tests prove CWD, commit, clean-tree, model, and task hashes are part
   of every admission receipt.
3. Clock tests distinguish direct tool wall from complete turn wall and reject
   missing or overlapping intervals.
4. Hidden-test tests prove agents cannot read acceptance material before
   yielding.
5. Privacy tests reject transcript prose, source bodies, raw paths, account
   tokens, and secrets from publishable receipts.
6. Rotation tests prove every seat executes every arm and no task repeats into
   one seat's prior context.
7. Resource-meter tests use a known child tree and reconcile cgroup/process
   counts.
8. Blinding tests remove arm labels and tool-specific receipt vocabulary from
   judge inputs.
9. Collector self-tests pass before and after the marker-bounded natural-history
   receipt.

## Documentation and release checklist

- Pre-register the sealed manifest, arm definitions, metrics, stop rules, and
  analysis code before the first efficacy run.
- Record each batch under `docs/observations/` without overwriting prior
  observations.
- Retain the exact next-marker comment only in a completed natural-history
  study.
- Publish the final routing or retirement decision as an ADR and update the
  clj-surgeon skill, README, HLDs, and architecture review together.
- Keep invalid and superseded run IDs with refusal reasons; never silently drop
  inconvenient trials.
- Record exact PID/start identity and CWD for every process or reap reference.

## Verification gates

1. `make study-agent-usage-self-test` passes.
2. The experiment harness's hermetic, privacy, rotation, timing, and resource
   tests pass.
3. The three-run calibration satisfies every calibration gate.
4. The twelve-run batch has twelve complete, paired, blind-scoreable receipts.
5. Any larger batch begins only from a written continue decision.
6. A fresh analyst can reproduce the aggregate tables from retained receipts
   without transcript access.
7. The model extension has paired Sol/high and Terra/high receipts by tooling
   arm and task class, or records the pre-registered reason it stopped early.
8. The final recommendation accounts for correctness, safety, complete task
   wall, actions/context, adoption routes, model suitability, cold/warm
   behavior, and retained resource cost.

## Definition of done

The study is complete when a cold reader can reproduce the privacy-safe
aggregate and choose among persistent MCP, CLI, task-class routing, subsumption,
or retirement without relying on agent self-report or microbenchmark startup
time. The winning recommendation has no additional safety failures, names its
memory and operational cost, survives fresh callers, says when Sol/high versus
Terra/high is justified, and includes an honest fallback when its measured
assumption is false.
