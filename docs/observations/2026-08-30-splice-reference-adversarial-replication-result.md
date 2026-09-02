# Splice-by-reference under attack: Sol survives, Spark finds the product stop

Date: 2026-08-30

## Verdict

**Overall product-trust verdict: KILL / HOLD. Do not promote splice-by-reference
to product from this evidence.**

The preregistered `gpt-5.6-sol` replication itself survives all four frozen
kill criteria. In the interleaved 8/arm cohort:

- Q and R were both 8/8 exact and route-adherent;
- R used four references in 8/8 episodes;
- all 32 submitted Sol references resolved to the task-intended candidate;
- wrong-subject was zero;
- all eight R episodes supplied and passed the observable read-back token; and
- median mutation emission fell from 650 to 444 `o200k_base` tokens,
  **−31.69%**.

That result is real, but it is only 1.69 percentage points above the 30% kill
floor and regresses **13.47 percentage points** from the foundation screen's
45.16% reduction. The new fixture priced the identity read-back signal and
smaller replacement spans honestly; the earlier headline does not replicate at
the same magnitude.

The requested `gpt-5.3-codex-spark` R bonus then produced the event this trust
screen was built to make loud. Spark chose the correct `r05` label and copied
its correct identity token, but paired it with explicit file
`src/splice_reference/fixture.clojure` instead of the intended
`src/splice_reference/fixture.clj`. The server-side intended-versus-resolved
identity diff returned `splice-reference-wrong-subject`,
`source_unchanged=true`, and `mutation_attempted=false`. Spark later recovered
to exact final bytes. The recovery does not erase the wrong-subject attempt.

The Spark bonus was preregistered as descriptive and is not pooled into the Sol
median. It therefore does not rewrite the formal Sol cohort verdict. It does
kill a broader cross-model trust or strict-grammar reliability claim: one of one
Spark R episodes produced one wrong-subject attempt. At this rung, the safe
product decision is hold.

## Frozen protocol and provenance

The cohort ran at protocol commit
`31c3ce02366fa530f8ef4c521e3fbaa5de9cdb1f`, tree
`c4afbe8e652f9aa05e3bfce2e3fa6e8d1a07f2cc`, against unchanged product
source and tests from
`c55de2279826af5ed21c90981591479dd2e802b2`.

- caller: Codex CLI on the ChatGPT subscription route; `OPENAI_API_KEY`
  removed;
- main model: exact `gpt-5.6-sol`, reasoning `high`;
- bonus model: exact `gpt-5.3-codex-spark`, reasoning `high`;
- tokenizer: `tiktoken==0.11.0`, explicit `o200k_base`;
- fixture: four owners, four confusable candidates per owner, labels `r01`
  through `r16` globally permuted, target ordinals 2/4/1/3;
- Sol order: `Q R R Q | R Q Q R | Q R R Q | R Q Q R`;
- Spark order: `Q R`, descriptive only;
- Q: today's exact owner-scoped `from`/`to` route;
- R: snapshot-bound `from_ref` lowered to the unchanged product writer after
  server-side intended-versus-resolved identity comparison;
- primary: canonical mutation arguments across every edit attempt, including
  retries and proxy refusals.

The fixture, task, proxy, order, models, scorer, predictions, and kill rules
were committed and pushed before the first model call. Three launcher checks
refused before result-root creation: two manually mistyped expanded protocol
SHAs and one moving product-ref mismatch. The forward-only immutable-product
amendment records these pre-model stops.

## Sol cohort result

| measure | Q, today's route | R, splice reference | change |
|---|---:|---:|---:|
| attempts / exact completed | 8 / 8 | 8 / 8 | tied |
| median mutation tokens | 650 | 444 | **−31.69%** |
| median mutation UTF-8 bytes | 2,438 | 1,590 | **−34.78%** |
| median all-MCP request tokens | 823.5 | 605 | −26.53% |
| median MCP round trips | 4 | 3.5 | −0.5 |
| strict reference adoption | 0/8 | **8/8** | +8 |
| intended/resolved identity matches | 0 | **32/32** | complete |
| verified read-back episodes | 0 | **8/8** | complete |
| blind or mixed R episodes | 0 | **0** | none |
| wrong-subject | 0 | **0** | tied |

Every Q mutation request was exactly 650 tokens / 2,438 bytes. Every R mutation
request was exactly 444 tokens / 1,590 bytes. There were no mutation retries or
typed reference refusals in the Sol cohort. Q needed 2–4 inspect calls before
its edit; R needed 2–4. The round-trip secondary therefore remains noisy and
does not strengthen the emission result.

Cold episode wall was not primary. Median wall was approximately 65.60 seconds
for Q and 49.02 seconds for R. That descriptive direction is not a product
speed claim.

## Frozen predictions scored

| prediction | registered | measured | score |
|---|---:|---:|---|
| exact completion | 8/8 each | **8/8 each** | confirmed |
| strict R adoption | 7/8 | **8/8** | exceeded |
| Sol wrong-subject | 0 | **0** | confirmed |
| mutation-token reduction | 38%, plausible 30–46% | **31.69%** | inside range, near floor |
| verified R read-back | at least 6/8 | **8/8** | exceeded |
| Spark exact | 2/2 | **2/2** | confirmed |
| Spark strict R use | 1/1 | **1/1** | confirmed |
| Spark wrong-subject | implicitly zero under any-wrong-subject trust rule | **1** | falsified |

The formal Sol kill table is all false:

| kill criterion | result |
|---|---|
| any Sol wrong-subject | false: 0 |
| R exactness below Q | false: 8 = 8 |
| strict R adoption below 6/8 | false: 8/8 |
| median mutation reduction below 30% | false: 31.69% |

The broader product-trust stop remains: the requested bonus contained one
wrong-subject attempt.

## What the read-back measure says

Sol did not fire blind. In all eight R episodes, every selected reference
carried the 12-hex identity token from the labeled read, and the server matched
all 32 tokens to the selected labels. This shows observable read-back behavior
under the explicit prompt. It does not prove that the model's internal intent
was correct; the independent candidate/file/owner comparison supplies that
proof for each submitted attempt.

Spark supplies the important counterexample. Its wrong request carried the
correct label and correct read-back token. Read-back proved that Spark checked
the pointer, but it did not prevent Spark from pairing the pointer with a wrong
explicit file. This is the same law as the earlier mnemonic and positional
falsifiers: one consistent component does not establish complete subject
identity. The server must compare the complete resolved identity immediately
before mutation.

## Spark bonus: exact recovery does not erase the miss

| measure | Spark Q | Spark R |
|---|---:|---:|
| final exact | yes | yes |
| mutation attempts | 2 | 2 |
| rescored mutation tokens | 1,272 | 857 |
| MCP round trips | 4 | 35 |
| references emitted | 0 | 8 across two attempts |
| wrong-subject | 0 | **1** |

Spark R made 33 inspect calls before its edit sequence. It eventually issued
one reference request with the wrong `.clojure` file identity, received the
loud refusal, and issued a corrected reference request that committed exact
bytes. The pair's emission arithmetic is descriptive only; the R cell is not a
correctness-clean performance observation.

This is not the feared silent wrong-twin mutation: the submitted label itself
was correct, and the identity firewall refused before product mutation. It is
still a registered wrong-subject because resolved file identity differed from
the request's explicit subject. The result validates the firewall and
falsifies reliance on Spark's strict grammar alone.

## Forward-only scoring correction

The first generated summaries counted only edit requests forwarded to the
product. That violated the preregistered rule for the one Spark request refused
inside the proxy. The raw refusal was retained, so the audit corrected the
scorer forward-only to include proxy-refused mutation attempts in emitted and
round-trip totals.

- Sol values and verdict are unchanged because Sol had zero proxy refusals.
- Spark R changes from the original undercounted 428 tokens / one edit to the
  correct 857 tokens / two edits and from 34 to 35 MCP round trips.
- Original `score.json`, cohort summary, Spark summary, and manifest remain in
  the evidence tree.
- Corrected files use `score-rescored.json` and `*-summary-rescored.json`.
- original manifest SHA-256:
  `1a8976de1bd00d8ee3e2966820de9a95aa044ebf0adef5b5199f2e45e34cb5dc`;
- complete rescored manifest SHA-256:
  `1e306fa2cbc8c3bc712efc31f005d12e8cf0cce2c23d80a437a4e7fd70e055e5`.

`shasum -a 256 -c manifest.sha256` verifies all 241 retained files. The tree is
3.6 MiB and includes raw Codex streams, prompts, stderr, proxy duplex streams,
proxy receipts, product telemetry, inverse receipts, exact workspaces, original
and rescored scores, summaries, config, and both manifest generations.

## Decision and next rung

Do not ship the experiment proxy or claim general splice-reference trust.
Retain the Sol result as evidence that full server-side identity resolution plus
read-back can survive a confusable 16-label fixture while saving 31.69% emitted
mutation tokens. Retain the Spark miss as the reason the product boundary must
remain fail-closed and model-independent.

A promotion attempt would need a new protocol in which the write carries one
canonical subject source of truth, not both a pointer and separately authored
file/owner fields that can disagree. The server must still echo full resolved
identity, and an adversarial multi-model cohort must produce zero
wrong-subject attempts. No product implementation is earned here.

## Evidence and replay

- preregistration:
  `docs/observations/2026-08-30-splice-reference-adversarial-replication-preregistration.md`
- retained root:
  `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/`
- authoritative Sol summary: `raw/cohort-summary-rescored.json`
- authoritative Spark summary: `raw/spark-bonus-summary-rescored.json`
- exact Spark refusal receipt: `raw/bonus/02-R/proxy-receipts.jsonl`

Replay requires a clean checkout at the frozen protocol commit and a new output
root:

```sh
PYTHONDONTWRITEBYTECODE=1 \
/private/tmp/splice-reference-screen-venv/bin/python -B \
  bench/run_splice_reference_screen.py --run \
  --output /private/tmp/splice-adversarial-replay \
  --auth-file /Users/genekim/.codex/auth.json \
  --expected-head 31c3ce02366fa530f8ef4c521e3fbaa5de9cdb1f
```
