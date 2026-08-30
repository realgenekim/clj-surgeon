# Complete owner vocabulary causal A/B

Date: 2026-08-29 PT  
Lane: SWEEP lane 1  
Status: **preregistered; no experiment episode has run**

## Question and source prior

Does a refusal that returns the complete owner vocabulary cause a fresh Sol
caller to skip a same-file recovery read before its next mutation attempt?

The retained observational source is commit `0ddfd1d` and
`docs/observations/2026-08-29-first-call-read-success-screen.md`. It counted
same-file rereads after 12 of 25 complete-list refusals (48.0%) and 134 of 166
truncated-list refusals (80.723%), an unadjusted 32.723 percentage-point split.
The source explicitly did not claim causality.

## Predictions frozen before launch

1. Arm C will reread in about 5 of 10 valid episodes; Arm T will reread in
   about 8 of 10. The predicted effect is a 30--35 percentage-point absolute
   reduction and a risk ratio near 0.60.
2. Arm C will require fewer recovery tool turns from the controlled refusal to
   the successful mutation. The median difference prediction is at least one
   tool turn.
3. Both arms will finish semantically correct in all valid episodes.
4. The wrong-subject rate will be exactly zero in both arms.

The primary mechanism is killed unless, among episodes valid on all three
declared dimensions, Arm C's reread rate is at least 30 percentage points lower
than Arm T's, Arm C's median recovery turns are no worse, and the wrong-subject
count is zero. Any wrong-subject mutation is reported loudly and independently
even if the primary metric passes. A passing N=10/arm result is a causal screen,
not a population-resolution claim.

## Resolution bound

The minimum cohort is 10 fully valid episodes per arm. Under independent
binomials with the observational priors `pC=0.48` and `pT=0.80723`, an exact
two-sided Fisher test at alpha 0.05 has only about 16.1% power. The corresponding
normal 95% half-width for the risk difference is about 39.5 percentage points.
This cohort cannot resolve a 30-point effect statistically. Failure to reject
zero will never be reported as equivalence; the report will show the effect
estimate, an unconditional exact or score interval, and this bound.

## Frozen experiment

### Caller and schedule

- Host/seat: Anvil `dev-a`, authenticated ChatGPT subscription route.
- Caller: fresh `codex exec --json --ephemeral` process per episode.
- Model: `gpt-5.6-sol`; reasoning effort `high`.
- Fixed interleaving: `C T T C` repeated five times. Invalid episodes remain in
  the chart; replacements continue in the same arm until each arm has 10
  episodes valid on all three dimensions.
- Every episode receives a fresh `CODEX_HOME`, workspace, fixture, and state.
  No install, shared MCP runtime, shared port, or live repository source is used.

### Real multi-owner fixture

Each workspace contains a parseable Clojure namespace at
`src/sample/views.clj` with 24 top-level `defn` owners. The frozen owner order is:

```text
render-home render-account render-profile render-settings render-help
render-login render-logout render-search render-results render-detail
render-cart render-checkout render-receipt render-history render-alerts
render-metrics render-admin render-dashboard render-activity render-feed
render-export render-import render-health render-version
```

Every owner returns a distinct literal map. Only `render-dashboard` contains
the target entry `:status :pending`; the required result is `:status :ready`.
The experiment controller records and fences the exact fixture, prompt, helper,
model, reasoning, schedule, and expected-output SHA-256 values before launch and
again before every episode.

### Controlled refusal and arms

The prompt requires the model to make its first mutation attempt, without a
source read, through the episode-local `./owner-edit dashboard` helper. That
declaration is deliberately invalid. The helper returns the same refusal in
both arms except for one field:

- **Arm C:** `available owners` contains all 24 names in source order.
- **Arm T:** `available owners` contains only the first 10 names and says that
  the list is truncated; this reproduces the historical shape and omits
  `render-dashboard`.

After refusal, the model may recover however it chooses, but `./owner-edit
OWNER` remains the only authorized mutation route. Supplying
`render-dashboard` performs the exact guarded replacement. Any other owner
refuses without writing. The helper cannot be modified.

The prompt, source, helper behavior, output ordering, and all result text are
identical across arms except the complete-versus-truncated owner vocabulary.

## Frozen scoring law

The controlled refusal is the completed first `./owner-edit dashboard` command.
The next mutation attempt is the next `./owner-edit` command or any direct file
mutation, whichever occurs first.

### Primary

`same_file_recovery_read=true` when, after the controlled refusal and before
the next mutation attempt, a completed tool action reads or searches
`src/sample/views.clj`. Reading the helper, listing directories, or reading
another file does not count. A command that both reads and mutates the fixture
counts as a read and a route violation.

### Secondary

`recovery_turns_to_success` is the number of completed model/tool boundaries
after the controlled refusal through the first successful fixture mutation,
inclusive. Report every value and arm medians; do not discard retries.

`wrong_subject=true` when any owner other than `render-dashboard` changes at
any point or when the final source differs from the exact expected bytes in any
other location. This safety outcome is never folded into the primary metric.

### Three independent validity dimensions

- `environment_valid`: exact model/reasoning and fenced inputs; recognized
  event stream; exit zero; exactly one controlled refusal; no timeout,
  transport failure, helper drift, or unknown actionable event type.
- `semantic_correct`: final fixture exactly equals the frozen expected bytes
  and the target mutation succeeded.
- `route_adherent`: no source read precedes the controlled refusal; all source
  mutations use the unchanged helper; first attempt is exactly `dashboard`;
  no direct patch or alternate writer is used.

All three booleans are shown separately for every episode. The primary
per-protocol estimate uses their conjunction. A second intention-to-treat chart
keeps every scheduled episode and every loss. Replacements never erase the
episode that caused them.

## Stop and retention law

After launch, no prompt, scorer, fixture, schedule, classification, or gate is
tuned. A failed or malformed position is retained and scored; only the minimum
valid-arm count may extend the fixed schedule. The whole cohort stops before
the next token if any fenced input or executable identity drifts.

The Anvil controller retains each raw JSONL event stream, stderr, prompt,
fixture before/after, helper log, per-episode score, manifest, and archive hash.
The final report records the remote path and copied-back archive hashes. It
does not advance the agent-usage window marker because this is a targeted
causal experiment rather than a history sweep.

