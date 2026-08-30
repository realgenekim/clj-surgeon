# Preregistration: prepared guarded-request routing replication

Frozen before any measured Codex process. The freeze commit and SHA-256 manifest
are written by `run_experiment.py freeze`; the measured runner refuses any
post-freeze byte drift.

## Question and intervention

Does returning an exact, ready-to-submit guarded `edit_clojure` request beside
an otherwise ordinary successful `inspect_clojure` result increase Codex's
actual Surgeon-first mutation rate on a fresh multi-file Clojure edit?

- Caller: Codex CLI on Anvil `dev-a`, ChatGPT subscription authentication only.
- Model: exactly `gpt-5.6-sol`, reasoning `high`; no fallback or substitution.
- Arms: `U` (ordinary inspect result) and `P` (the same result plus the prepared
  request).
- Every task requires exactly one batched `inspect_clojure` call before any
  mutation. Native `apply_patch`/file change and production `edit_clojure` are
  both offered and executable.
- The prompt, model, reasoning, fixture, inspection request, tool schemas,
  native route, expected bytes, verifier, and limits are identical across arms.
- The sole model-visible arm difference is the appended text content in `P`:
  an exact JSON `edit_clojure` argument object with `workspace_root`, files,
  owner guards, old forms, replacements, and match counts prefilled. `U` emits
  the unmodified upstream result. The payload is not in the user prompt,
  system instructions, tool description, or schema.

## Fresh fixture and task

The synthetic repository is new for this replication and is not the sweep-2
fixture. It has three production namespaces and one test namespace. The task
coherently updates checkout resilience policy across two existing files:

1. `connect-timeout-ms`: 250 to 400;
2. `request-timeout-ms`: 900 to 1200;
3. rename public `retry-budget` to `resilience-budget` and update its qualified
   caller in the second file; and
4. change checkout profile text from `"legacy"` to `"resilient"` in three
   owner-scoped sites.

`src/acme/analytics_policy.clj` deliberately contains same-shaped distractor
values and names. It and every other unrelated byte must remain unchanged.
Both mutation routes are preflighted end to end against a disposable clone.

## Frozen cohort and schedule

Exactly 20 measured process starts, 10 per arm, in this interleaved order:

`U P P U / P U U P / U P P U / P U U P / U P P U`

The five four-run blocks alternate the two Williams sequences; the last block
restores equal arm counts. After a measured process starts, it is retained and
never replaced for invalidity, incorrectness, refusal, timeout, no mutation,
model error, or infrastructure failure. A prelaunch refusal before Codex starts
may be repaired without creating an attempt. The cohort is never expanded past
20 starts. After launch, prompts, payloads, schemas, fixture, schedule, scoring,
predictions, gates, and code are not tuned.

## Registered predictions with magnitudes

- Primary: `U` routes Surgeon-first in 4/10 (40%); `P` routes Surgeon-first in
  8/10 (80%); predicted `P-U` lift is **+40 percentage points**.
- Turns-to-success: among semantically correct attempts, `P` has a median of
  at least one fewer assistant action/tool event through the first successful
  mutation than `U`.
- Output: median CLI-reported `output_tokens` in `P` is at least **15% lower**
  than in `U`.
- Correctness: exact expected bytes and the independent verifier pass in
  **10/10 per arm**.
- Safety: wrong-subject is **0/20**.

The screen's observed 2/4 to 4/4 movement motivates the directional prediction,
but the magnitudes above deliberately regress toward zero for a new fixture and
a product-result rather than prompt-embedded treatment.

## Kill and safety criteria

- If the all-started-attempt `P-U` Surgeon-first difference is **below +20
  percentage points at n=10/arm**, the +50-point screen does not replicate.
- A difference of +20 points or more clears this replication gate; it does not
  establish a population effect or product readiness by itself.
- Any wrong-subject mutation, or fewer than 9/10 exact-and-verifier-green `P`
  attempts, fails the safety gate even if routing lifts.
- Turns and output-token predictions are secondary and cannot rescue a failed
  primary or safety gate.

## Frozen fields and scoring

Every started attempt records these distinct fields; one never overwrites
another:

- `process_started`, `process_exit_code`, `timed_out`;
- `requested_model`, `reasoning_effort`, `subscription_auth_preflight`, and
  `openai_api_key_absent`;
- `environment_valid`: process started and exited zero, the frozen proxy became
  ready, exact production tool-surface and server-instruction hashes match the
  preflight, one successful inspection occurred before mutation, and arm
  exposure matched assignment;
- `semantic_correct`: both target files equal their complete byte oracles and
  the independent Clojure verifier exits zero;
- `wrong_subject`: any final change outside the two target files, any byte
  change in the distractor/unrelated committed files, or any `edit_clojure`
  call naming an unallowed file;
- `primary_route`: first completed successful `edit_clojure` mutation is
  `surgeon_mcp`; first completed native `file_change` is `native`; otherwise
  `none`. Refused or incomplete calls do not count as actual mutation;
- `route_adherent`: environment-valid, semantic-correct, one successful inspect
  before mutation, exactly one mutation family, no wrong-subject, and no
  refusal or other write mechanism;
- `turns_to_success`: number of started assistant action items (`mcp_tool_call`,
  `file_change`, or `command_execution`) from turn start through the first
  completed successful mutation, only when the eventual result is semantically
  correct; and
- CLI usage fields: `input_tokens`, `cached_input_tokens`, `output_tokens`, and
  `reasoning_output_tokens` from `turn.completed`.

The primary headline keeps all ten started attempts per arm in the denominator.
Environment-valid and semantic-correct subsets are secondary only. Report each
rate as numerator/denominator with a 95% Wilson interval. Report `P-U` with a
95% Newcombe score interval. At n=10/arm, uncertainty is necessarily wide:
never claim equivalence or population resolution from a null or small result.

## Retention and delivery

Raw Codex JSONL, stderr, proxy JSONL (including upstream and emitted inspect
results), process receipts, diffs, final files, verifier output, per-run scores,
freeze/preflight receipts, aggregate, and replay commands are retained. The
deterministic archive and its manifest are SHA-256'd. Results are delivered on
`experiment/prepared-request-replication-20260830` with author
`sol <sol@skiff>` and trailer
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`. No force push, shared
install, or shared service reload is permitted.
