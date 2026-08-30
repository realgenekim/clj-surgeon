# Splice-by-reference adversarial replication — preregistration

Date frozen: 2026-08-30, before the first model call.

## Purpose and prior

This is the fresh-fixture adversarial replication earned by
`experiment/splice-reference-screen-20260830` at
`588893fce7c37929217967c9407bfa808f57a37e`. That screen measured 8/8 exact
tasks in both arms, 8/8 strict R adoption, zero wrong-subject attempts, and a
45.16% median mutation-token reduction. It expressly did not establish general
wrong-subject safety and named multiple similar labels per owner as the next
rung.

The attack catalog is frozen from `docs/acejump-postmortem-20260830` at
`a78c66092eceb00cd111015b9455992fa308126d`, including the mnemonic-label
falsifier recorded at `2d40036` and the positional wrong-owner audit. Those
falsifiers showed that a coherent wrong reference can mutate the wrong subject
with `ok=true` and `verification_complete=true`; snapshot and content guards
prove consistency, not caller intent. This replication therefore scores the
identity selected by the pointer independently of final bytes.

## Frozen fixture family

One fresh Clojure fixture contains four owners and exactly four labeled
candidate spans per owner. The 16 labels are the visually similar set `r01`
through `r16`, permuted globally so label order does not reveal file order or
the intended target. The intended target ordinals are 2, 4, 1, and 3.

The four confusion classes are:

1. same-name and near-name quoted forms, including both
   `(def alpha "alpha")` and `(def alpha "alphabet")`;
2. twin keyword pairs that differ only by order and a positional field;
3. `"alpha"` spans beside sibling strings for which it is a literal substring,
   including `"alphabet"` and `"alpha-prefix"`; and
4. four spans with the same literal, quoted form, and keyword pair, differing
   only in slot and position.

Every owner therefore has at least three plausible wrong twins. Each complete
replacement span is at least 100 UTF-8 bytes, unique inside its named owner,
and supported by today's exact `from`/`to` route. The task changes exactly one
candidate per owner from `:enabled? false` to `:enabled? true` and preserves all
other bytes.

## Frozen mechanism and arms

Product source remains unchanged relative to
`origin/release/closed-relations-published`. Every episode gets a fresh fixture
copy and fresh isolated product MCP process. Both arms expose only
`inspect_clojure` and `edit_clojure`.

- **Q — today's route:** ordinary inspect result and ordinary owner-scoped
  `edit_clojure`; the model re-quotes each exact old span.
- **R — splice-reference:** the same inspect result plus all 16 readable,
  snapshot-bound labels. A write may use `from_ref` plus `to`; the proxy resolves
  the label and lowers the exact old span into the unchanged product writer.

Every readable label carries label, candidate id, owner, ordinal, file and
owner snapshot hashes, UTF-8 offsets, anchor hash, complete source, preview,
and a 12-hex identity token. The token is not mutation authority. The optional
`ref_readback` field is an observable behavior signal: the prompt asks the
model to copy it only after checking the chosen label against the labeled read.
Omission remains valid and is scored as blind fire; a correct supplied token is
scored as verified read-back; mixtures are reported. Read-back behavior is not
a kill criterion and cannot rescue wrong identity.

## Predeclared adversarial identity scoring

For every R edit attempt, before product mutation:

1. infer the task-intended target identity from the unique requested `to`;
2. resolve `from_ref` server-side to full readable identity;
3. diff resolved candidate id, file, and owner against intended candidate id,
   file, and owner; and
4. retain and echo resolved identity, intended identity, identity-match result,
   and read-back status in the proxy receipt.

Any mismatch is `splice-reference-wrong-subject`, with
`source_unchanged=true` and `mutation_attempted=false`. It counts as one loud
wrong-subject occurrence even if the model later retries correctly and reaches
exact final bytes. Final-byte exactness, parse validity, product
`verification_complete=true`, and a plausible result never erase that event.
This is the pointer-to-identity law: a pointer receipt is incomplete without
the human-readable identity it resolved.

## Frozen cohort, caller, and counting

The Sol cohort order is:

`Q R R Q | R Q Q R | Q R R Q | R Q Q R`

There are exactly eight assigned attempts per arm. Failed attempts remain in
their slots. Every episode is a fresh ephemeral `codex exec` session on the
ChatGPT subscription route, with no API key, exact model `gpt-5.6-sol`,
reasoning `high`, read-only sandbox, fresh Codex home, fresh product process,
and fresh workspace.

After the Sol cohort, two descriptive bonus cells run in order `Q R` with exact
model `gpt-5.3-codex-spark` and the otherwise identical protocol. They test the
strict-grammar reliability thesis and are never pooled with the Sol cohort or
used to rescue its verdict.

The primary emitted unit is canonical JSON mutation-request arguments per
completed task, summed across all `edit_clojure` attempts including retries.
JSON is UTF-8, key-sorted, compact, and Unicode-preserving. Tokens use
`tiktoken==0.11.0`, explicit `o200k_base`. UTF-8 bytes and all-MCP request
tokens are co-reported. A valid completed task requires environment validity,
route adherence, exact expected bytes, and product
`verification_complete=true`.

Strict R adoption requires one four-edit request in which every edit uses
`from_ref` and none quotes `from`, `old`, or `before`. Mixed requests are
reported but do not count. Read-back behavior is classified per episode from
the independently validated optional identity tokens.

## Predictions and kill criteria

Predictions frozen before launch:

- exact completed tasks: 8/8 in each Sol arm;
- strict reference adoption: 7/8 R attempts;
- wrong-subject occurrences: 0;
- median mutation-token reduction: 38%, plausible range 30–46%;
- verified read-back behavior: at least 6/8 R episodes;
- Spark bonus: 2/2 exact and strict R reference use in its one R cell.

The replication is killed by any one of:

- any wrong-subject occurrence;
- R exactness below Q exactness;
- strict R adoption below 6/8; or
- median mutation-token reduction below 30%.

The measured reduction is also compared explicitly with the prior 45.16%.
Any regression is reported, including a surviving result between 30% and
45.16%. Secondaries and Spark cannot rescue a fired kill criterion. A null at
this sample size is not equivalence, and this synthetic replication cannot mint
a general product performance claim.

## Frozen evidence and replay

Every episode retains the prompt, raw Codex JSONL stream, stderr, exact command
identity, proxy duplex stream, proxy receipts, product telemetry, inverse
receipt, before/after workspace, score, wall time, model, and route identity.
The result root retains the frozen config, cohort and bonus summaries, protocol
and fixture SHAs, and a SHA-256 manifest over every retained file.

Replay to a new output root only:

```sh
PYTHONDONTWRITEBYTECODE=1 \
/private/tmp/splice-reference-screen-venv/bin/python -B \
  bench/run_splice_reference_screen.py --run \
  --output /private/tmp/splice-adversarial-replay \
  --auth-file /Users/genekim/.codex/auth.json \
  --expected-head <frozen-protocol-commit>
```
