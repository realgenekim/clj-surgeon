# Elaborator fallback model-variant battery: frozen preregistration

Frozen before this battery's first model call on 2026-08-30 PDT.

## Question and arms

Can a ChatGPT-account model provide a reliable elaborator-fill fallback that is faster than `gpt-5.6-sol`, and how does the best fallback compare with `gpt-5.3-codex-spark` after Spark's quota resets?

The immediate arms run serially in this fixed order:

1. `gpt-5.6-terra`
2. `gpt-5.6-luna`
3. `gpt-5.6-sol`

The clearly marked addendum arm is `gpt-5.3-codex-spark`, scheduled for 2026-08-31 00:45 PDT, after the user-observed reset at 00:42 PDT. All arms use reasoning effort `low`, a clean Codex CLI profile, ChatGPT subscription authentication, no tools, an exact model pin, and no provider model fallback.

User-supplied discovery facts are prior evidence, not battery trials: Terra, Luna, Sol, and Spark are supported; `terra-low`, `luna-high`, `codex-mini`, `sol-low`, and `5.5-terra` returned HTTP 400; Spark is quota-drained until 2026-08-31 00:42 PDT; and the user's crude 450-token E2E observation was Terra 9.4 s, Sol 12.5 s, Luna 14.0 s. This battery will not spend calls rediscovering those facts.

## Decode-rate endpoint

Each arm has five large-output trials and five matched one-word controls, interleaved `C, B` by replicate and run serially.

- B prompt: print integers 1 through 200, ascending, one per line, with no other text. This is expected to produce approximately 400 decoded tokens.
- C prompt: reply with the single word `ok` and no other text.
- Model output is extracted only from structured `agent_message` events. The transcript and echoed prompt are never searched for success text.
- B normalization accepts only decimal integers separated by commas and/or whitespace, then requires the parsed vector to equal exactly `[1 ... 200]`. Thus `1, 2` and `1\n2` normalize identically, while commentary, omissions, duplicates, and prompt echoes fail.
- C normalization trims Unicode whitespace and lowercases the isolated agent message, then requires exactly `ok`.
- Provider-reported `output_tokens + reasoning_output_tokens` is the decoded-token count.

For oracle-passing trials, report median, minimum, maximum, and MAD. The E2E rate is median B decoded tokens divided by median B wall time. The bootstrap-subtracted estimate is:

`(median B decoded tokens - median C decoded tokens) / (median B wall - median C wall)`.

Condition C is the measured Codex per-turn floor: process start, configuration/session setup, scheduling/queueing, request setup, teardown, and a one-word decode. The subtraction does not claim to isolate provider kernel time. The local time from process start to the structured `turn.started` event is also reported. A subtraction is marked resolved only when its positive wall-time delta exceeds `2 * (MAD(B) + MAD(C))`; otherwise the numerical estimate is shown as descriptive and labeled unresolved. Failed-oracle or missing-usage trials are excluded from rate aggregation and reported in the denominator.

## Guarded fill endpoint

Each arm gets the same six cases in `fill-cases.edn`, once each, through `/private/tmp/bang-rig/bang.sh`. Exactly one model daemon is warm. The rig is bounced with `bang-down.sh` and `BANG_MODEL=<exact model> bang-up.sh` between arms. The fixture is restored to its preregistered baseline and its SHA-256 checked before every arm. There are no retries.

The score is derived from structured rig receipts and a Clojure reader, never a grep of console text:

- `exact`: the isolated returned replacement parses as one Clojure form, its top-level owner equals the requested owner, and its normalized data form equals the case's expected form.
- `one-shot`: the rig applied the first and only response in its guarded transaction.
- `wrong-subject`: the returned top-level owner differs from the requested owner, or the action delta names a file other than the fixture. A wrong-subject trial scores zero exact regardless of other content.
- `schema-fumble`: the response is not exactly the rig's one-key JSON object with a string `replacement`.
- Timing: report model-generation (`spark_ms`, retained field name in the supplied rig) and total wall milliseconds for every bang, plus per-arm medians.

## Meter and failure rules

No failed trial is retried. Any usage-limit, rate-limit, or quota error is recorded as a meter fact and the remaining models continue. A failed warmup is an arm-level meter/failure fact, not a reason to probe alternate model spellings. Calls stay at the frozen counts.

## Verdict rule

The fallback ladder is lexicographic: exact count descending; one-shot count descending; wrong-subject count ascending; schema-fumble count ascending; then bootstrap-subtracted decode rate descending, with median model-generation fill wall as the final speed tiebreak.

`Reliable enough` is preregistered as at least 5/6 exact, zero wrong-subject, and zero schema fumbles. A model is honestly called `reliable enough but faster than Sol` only if it meets that threshold, is not worse than Sol on exact count, and has a higher resolved bootstrap-subtracted decode rate than Sol. If no model satisfies all conditions, the verdict says none. The Spark addendum reports reliability-count deltas, decode-rate ratio and percent delta, and median fill-generation wall ratio and percent delta between Spark and the best non-Spark fallback. Results are descriptive; n is intentionally small for quota courtesy.

## Frozen implementation identities

- Experiment base: `eb751875a877772e291305ac5b5239aceeb89bfa`
- Supplied rig target: `4ec9394c59805addef05076cf2c78c463b8ea6e6`
- Decode probe source: `bench/measure_prefill_decode_ratio.sh` at `8d3c6f685f1605844e03c6f851f78304e0c7bf41`, restricted to conditions B and C with `RATIO_REPLICATES=5`, `RATIO_COUNT_TO=200`, `RATIO_REASONING=low`, and `RATIO_PROFILE=clean`.
- The final report records SHA-256 identities for the rig server/client, prompts, case manifest, raw facts, and scorer.
