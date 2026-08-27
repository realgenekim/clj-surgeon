# Plan: Earn Five-Times-Native Extraction

## Objective

Move the frozen 15-owner extraction from 37.871 seconds toward the
matched-correctness 5x gate of 24.456 seconds without changing the task,
correctness oracle, caller/model stratum, or safety boundary.

The stable starting point is tagged
`speed/extraction-3.23x-2026-08-26`:

| Route | Complete wall | Correctness | Geometry |
|---|---:|---|---|
| Surgeon | 37.871s | matched | one apply plus one exact lint |
| Native | 122.278s | matched | native implementation route |

An independent Surgeon run decomposed 37.500 seconds into 8.297 seconds of
server-authoritative extraction work and a 29.203-second non-kernel residual.
The retained `commands.tsv` value `1407` is lint output character count, not
1.407 seconds of lint wall; the exact verifier is still inside the unmeasured
residual. Reaching 5x requires another 13.415 seconds from the stable median
(13.044 seconds from the independent run). No single supported mechanism
credibly owns that full gap.

## Laws

1. Optimize complete verified task wall, not MCP execution time or adoption.
2. Freeze the task capsule, scorer, route, prompt information, and model stratum
   before comparing an option.
3. Begin with the smallest counterbalanced cohort that can kill an idea.
4. Combine only mechanisms that win independently and save disjoint time.
5. Preserve negative results and ugly runs. Never repair the past cohort.
6. Keep semantic correctness primary. Whitespace-only drift is secondary;
   comments, metadata, reader forms, directives, and unrelated source remain
   protected.
7. SURGEON1 owns product integration and publication. SURGEON2 owns bounded
   research receipts and cannot merge or publish into the production lane.
8. Use `(N * K * sigma) / t`: increase independent options and parallel trials,
   spend experiments where uncertainty is high, and lower verified cycle time.

## Experiment portfolio

### E1: Observable event and phase clocks

Add a transport-neutral JSONL byte-preserving timing tap to the clean Codex
harness. Record observer arrival time and line length in a separate raw clock
stream; classify allowlisted event phases offline. Preserve the original
`events.jsonl` byte-for-byte and distinguish observer time from MCP
server-authoritative `elapsed_ms`.

Acceptance:

- original event stream SHA is unchanged;
- Codex and observer exit statuses remain distinct;
- no prompt, command, result, source, or reasoning content enters timing data;
- median overhead is below 250ms and 1%;
- event/clock count and byte lengths pair exactly;
- a fresh extraction trace reconciles to complete wall within measurement
  tolerance and exposes at least one residual interval of three seconds.

Stop if Codex buffers events too coarsely, the tap perturbs the subject, or no
observable interval can select a meaningful next experiment.

First canary at the unchanged Sol/high route: **passed**. The 40.028-second
correct run exposed 30.012 seconds across observable model-controlled event
gaps, 7.917 seconds of apply wall (7.838 seconds server-authoritative), 0.236
seconds of exact verifier execution, and about 1.86 seconds of process/event
bookends. This promotes exact verifier-boundary fusion ahead of speculative
formatter or lint-process optimization. Establish a same-seat noise floor
before accepting a three-second product win.

First chord ablation: a bare “tool first” instruction caused a safe schema
refusal and a 55.397-second recovery. Supplying the exact nested call skeleton
then produced a correct one-shot 33.789-second route, 6.239 seconds below the
adjacent narrate-then-call canary despite a slower kernel. Treat this as an n=1
mechanism signal; run a small counterbalanced cohort before promotion.

Counterbalanced Anvil replication: **passed the small-cohort mechanism gate**.
dev-b normal→tool-first was 35.430s→28.230s; dev-c tool-first→normal was
27.667s→35.444s. All four runs were correct with one extraction and one exact
lint. Medians were 35.437s and 27.949s, a 7.489-second or 21.1% reduction. The
candidate is 4.38x faster than the retained 122.278-second native control and
3.493 seconds above the 5x gate. Encode the chord without weakening the
genuine-decision refusal route, then combine it only with an independently
earned verifier-boundary mechanism.

Tool-description productization: **stopped and reverted**. Under the ordinary
prompt, PRE was 36.521s median and POST was 35.115s; one paired seat improved
while the other regressed. Extra prose in the large tool description did not
reliably reproduce the first-item affordance. Preserve the prompt result, but
do not claim the transferable mechanism is implemented.

### E2: Hot or incremental complete extraction proof

Profile workspace enumeration, parse, owner/dependency closure, quoted-Var
scan, caller scan, render, formatting, and verification separately inside the
single frozen transaction. Test hash-fenced reuse only for a phase whose
measured contribution is at least three seconds.

Acceptance:

- exact snapshot ownership and complete caller evidence are unchanged;
- stale source still refuses before mutation;
- kernel falls from 8.297--11.121 seconds to at most four seconds, or complete
  wall improves by at least 15%;
- cold milestone tests still exercise the uncached path.

Stop if hash validation costs approximately recomputation, no reusable phase
owns three seconds, or any authority becomes index-dependent.

### E3: Exact repository verifier in the transaction

Shadow the exact existing command and acceptance semantics before changing the
route. Compare accepted/rejected fixtures, exit status, output, warning policy,
file scope, and rollback behavior. Only then test removing the separate model
boundary while preserving the same verifier.

Acceptance:

- four representative contracts are behaviorally identical;
- the frozen extraction remains correct and rollback remains atomic;
- complete wall improves by at least 15% or three seconds.

`verify=fast` is not a candidate: it already failed semantic equivalence.

### E4: Cheaper post-decision materializer

SURGEON2 owns this independent lane. Counterbalance Sol/high against a faster
model only after the complete structural decision is supplied. Keep kernel,
residual, complete wall, route actions, and scorer outcome separate.

Acceptance:

- every run preserves the one-apply plus exact-verifier geometry;
- every run passes the frozen scorer without retry;
- paired saving is at least three seconds;
- routing or model substitution does not reopen architectural judgment.

Initial ABBA screen: **keep as evidence; stop product work**. All four routes
were identical and correct. Terra/low saved 2.853 seconds at the median, but
model and reasoning changed together, and 31.842 seconds remains above the 5x
gate. Do not run a factorial unless E1 later identifies model materialization as
the dominant removable interval. It cannot contribute to the frozen Sol/high
5x claim.

### E5: Combine earned mechanisms

After E1 attributes the route, combine only independent winners from E2--E4.
Run at least two counterbalanced clean-context replicas. The milestone passes
only if every correct Surgeon replica is at most 24.456 seconds. Report
non-additive overlap rather than summing projections.

### E6: Generalize the win

Use remaining capacity on two low-cost studies:

- mechanically sample historical commits spanning extraction, move,
  caller/rename, namespace surgery, heterogeneous multi-file changes, and a
  native-positive small edit;
- replay five retained read-heavy turns using only information available in
  their original prompt, testing whether they compress to at most two
  structural reads.

Do not add a new public API before a retained task proves the need.

## Parallel allocation

| Lane | Owner | Work |
|---|---|---|
| Production | SURGEON1 | E1, then the E2 or E3 seam selected by clocks |
| Independent research | SURGEON2 | E4 materializer ablation and immutable receipt |
| Adversarial review | Fable and Codex Sol | Rank the same frozen portfolio; expose confounds and double counting |
| Anvil dev-b/dev-c | Fresh callers | Small counterbalanced cohorts after local interface and harness proof |

Use idle Anvil lanes rather than composer-wedged p32, p34, or p45. Start with
one replica per arm and grow only after the mechanism and scorer are green.

## Binding stops

Do not reopen these during this campaign without new causal evidence:

- fewer MCP schemas (15.6% slower despite 90.7% less catalog text);
- compact `plan_id` (missed byte and decision-time gates);
- mandatory public planning for mechanically complete decisions (~12.070s);
- generic `verify=fast` (different semantics);
- further narration suppression (3.7%, boundary remained);
- formatter as the missing 13 seconds;
- broad fuzzy ranking or Levenshtein as authority;
- universal SCI/editor catalogs or edit-count-only benchmarks;
- more native prompt golf on this frozen task.

## Required receipts

Each experiment records exact commits, fixture/scorer/prompt hashes, model and
reasoning, order, individual complete-wall and server times, route actions,
correctness, raw archive hashes, confounds, falsifier result, and the explicit
merge/continue/stop decision. Captain's Logs preserve both the mechanism and
what became cheaper next.
