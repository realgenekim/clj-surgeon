# Preregistration: ordinal refusal recovery screen

Status: predictions frozen before the first model launch. This is a
pre-ratification experiment surface only. It is evidence *for* Gene's later
write-refusal decision, never product code or implementation authority.

## Evidence and question

The retained study reports 191/283 refusals as
`batch-form-selection-failed`; 86.4% truncate a median 27-name candidate
universe to 10, only 16.8% show the requested name, and 98.4% are followed by
a recovery read. Those reads returned 2.72 MB redundantly. Refusal-to-retry
pairs re-emitted 46.1% of all write bytes, 90% mechanically derivable, while
only 1/283 refusals carried a next call. Sweep-1 `c1e89d5d` causally measured
0/10 recovery reads with complete vocabulary against 10/10 with truncation.

Question: after a deliberate selection refusal, does a complete numbered
27-owner vocabulary plus one non-executable corrected-request template with a
caller confirmation hole let `gpt-5.6-sol` recover by supplying one ordinal,
without a recovery read or full request re-emission?

## Frozen arms and isolation

- Fixture: one synthetic Clojure namespace with exactly 27 top-level `defn`
  owners. The target is the singular operations-dashboard owner at one-based
  ordinal 19. Every owner contains `:status :pending`, so a wrong ordinal can
  produce a real wrong-subject mutation and cannot hide behind a no-match.
- Controlled first attempt: `apply_clojure_changes` with the near-miss owner
  `render-dashbord`; no source read is allowed before it.
- Arm C: current-shaped refusal with `available_form_count=27`, the first ten
  owner names, and `truncated=true`.
- Arm T: the same refusal augmented with the complete numbered vocabulary and
  exactly one non-executable, no-authority experiment template. Its sole
  caller hole is `candidate_index`; filling it requires a separate public tool
  call that revalidates refusal identity and the frozen source hash.
- Catalog, prompt, fixture, tool schemas, mutation behavior, model, reasoning,
  and scoring are byte-identical across arms. Only the refusal projection
  differs.
- Every episode owns a fresh workspace, fresh credential-only `CODEX_HOME`,
  and fresh local stdio MCP proxy. No shared MCP port, install, reload, product
  source, or persistent server state is used.

The proxy and every refusal receipt say `experiment_only=true` and
`product_contract=false`. Product refusal invariant: no executable payload,
selected candidate, prepared mutation, or inherited write authority. This
proxy intentionally violates the *product surface shape* only inside the
isolated screen so that the proposed mechanism can be measured.

## Schedules and validity

The sub-ceiling pilot runs first in fixed order `C T T C` (n=2/arm). It cannot
pass the main gate and is never pooled. It releases the cohort only if all four
processes launch, produce the controlled refusal, finish semantically correct,
and make no wrong-subject mutation. Its purpose is apparatus and non-ceiling
validation, not effect estimation.

The main fixed schedule is `C T T C` repeated six times. Execution stops at
the first completed four-position block with at least eight fully valid
episodes per arm. Invalid launches remain in the chart; no slot is rewritten.
The maximum is 12 launches per arm. Model is `gpt-5.6-sol`, reasoning `high`,
ChatGPT subscription route, one ephemeral `codex exec --json` process per
episode, stdin closed, timeout 360 seconds.

Three dimensions are reported independently:

- `environment_valid`: model/effort frozen; process exits zero without timeout;
  one controlled refusal; recognized event stream; proxy completion.
- `semantic_correct`: final fixture equals the exact expected bytes.
- `route_adherent`: first completed MCP call is the exact near-miss write; no
  source or shell read precedes it; no native command writes the fixture.

`fully_valid` is their conjunction. The primary per-protocol estimate uses
fully valid episodes; an intention-to-treat chart retains every launch.

## Registered predictions and gates

Primary outcome is the number of completed `inspect_clojure` recovery reads
after the controlled refusal and before the first successful retry.

Predictions, frozen before launch:

1. Arm T mean recovery reads will be 0.00--0.25; Arm C mean will be
   0.75--1.25. The point prediction is T=0 and C=1, an 87.5%--100% reduction.
2. At least 6/8 fully valid T episodes will succeed by
   `index-hole-fill`; at most 2/8 will retype a corrected write request.
3. Median Codex output tokens (the authoritative turn meter, including the
   fixed forced first-call prefix) will be 35%--60% lower in T. Exact
   post-refusal model-emitted bytes are also retained because the Codex stream
   does not expose segment-level token usage.
4. Median refusal-to-success wall will be 25%--45% lower in T, an expected
   4--12 second saving on this local surface.
5. Semantic correctness will be 8/8 or better in each arm and wrong-subject
   mutations will be exactly zero.

The mechanism is killed if the per-protocol recovery-read reduction is below
50%, if fewer than half of valid T episodes use the ordinal hole, or if any
wrong-subject mutation occurs. Token and wall outcomes are descriptive and
cannot rescue the primary or safety gate. A passing n=8/arm result is a
prototype screen, not a population-resolution or product-ratification claim.

## Retention law

After freeze, no prompt, schema, fixture, schedule, prediction, scorer, or gate
may change. Drift stops before the next model token. Every raw JSONL stream,
stderr, proxy log, before/after hash, score, manifest, receipt, and archive hash
is retained. Credential homes are excluded and destroyed after each episode.
