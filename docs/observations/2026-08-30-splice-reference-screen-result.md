# Splice-by-reference screen: the model used the pointers and cut mutation emission 45%

Date: 2026-08-30

## Verdict

**The synthetic screen survives.** In the frozen 8/arm interleaved cohort,
`gpt-5.6-sol` used snapshot-bound span references in every R attempt and
reduced median mutation-request emission from **899 to 493 `o200k_base`
tokens, −45.16%**. Canonical UTF-8 request bytes fell from **2,844 to 1,567,
−44.90%**. Both arms completed exact expected bytes in 8/8 attempts. All 32 R
span resolutions echoed readable identity; wrong-subject and typed reference
refusals were both zero.

None of the three registered kill criteria fired:

- token reduction was 45.16%, above the 25% floor;
- wrong-subject was 0; and
- strict all-four reference use was 8/8, above the 4/8 floor.

This answers the prototype question positively for this fixture: the model can
point to old text instead of re-quoting it, and the saving appears in the raw
emitted tool arguments. It does **not** establish a product performance claim
or prove general wrong-subject safety. Under the experiment doctrine this is a
rung-2 screen; it earns a fresh-fixture replication, not implementation.

## Frozen protocol and provenance

The v3 cohort ran at protocol commit
`da139e1805a56bf36d2d7562b6486854ac67a4fa`, tree
`b2316a636a4e4048511b2f7acc1f7d316d15194d`, on unchanged product source from
`origin/release/closed-relations-published` commit
`c55de2279826af5ed21c90981591479dd2e802b2`.

- caller: `codex-cli 0.149.1`, ChatGPT subscription, no API key;
- model: exact `gpt-5.6-sol`, reasoning `high`;
- tokenizer: `tiktoken==0.11.0`, explicit `o200k_base`;
- fixture: four owner-scoped true replacements with 191–335 byte old spans;
- Q: ordinary read plus ordinary `edit_clojure` re-quotation;
- R: the same product read and edit handler behind an experiment-only proxy
  that labels returned spans, binds them to the product read's file SHA-256,
  resolves `from_ref`, and lowers it to ordinary `from` before validation;
- order: `Q R R Q | R Q Q R | Q R R Q | R Q Q R`;
- primary: median canonical mutation arguments per completed task, counting all
  edit retries.

The prompt, source, expected bytes, scorer, order, magnitude predictions, and
kill rules were committed before the first valid model episode. Product code
and product tests were never changed.

## Sub-ceiling pilot

The v3 Q→R pilot proved that the instrument could move before the cohort spent:

| arm | exact | route | mutation tokens | references | resolved identities | wrong-subject |
|---|---:|---|---:|---:|---:|---:|
| Q | yes | conventional | 1,761 | 0 | 0 | 0 |
| R | yes | strict reference | 509 | 4 | 4 | 0 |

The mechanically ideal requests were 847 tokens for Q and 509 for R, a 39.91%
available effect. The higher observed Q pilot count included all emitted
mutation attempts under the registered rule. Pilot data is not pooled with the
cohort.

## Cohort results

| measure | Q, ordinary quote | R, span reference | change |
|---|---:|---:|---:|
| attempts / completed exact | 8 / 8 | 8 / 8 | tied |
| median mutation tokens | 899 | 493 | **−45.16%** |
| median mutation UTF-8 bytes | 2,844 | 1,567 | **−44.90%** |
| median all-MCP request tokens | 1,011 | 562 | −44.41% |
| median MCP round trips | 3 | 2 | −1 |
| strict reference use | 0/8 | **8/8** | +8 |
| readable resolved identities | 0 | **32** | +32 |
| typed reference refusals | 0 | 0 | tied |
| wrong-subject | **0** | **0** | tied |

Per-task mutation tokens, in assigned order within each arm:

- Q: `899, 899, 899, 899, 899, 899, 1,933, 1,903`
- R: `509, 509, 493, 493, 493, 493, 493, 509`

The two large Q values are not removed: each contains an additional mutation
attempt, and the primary explicitly counts retries. They do not determine the
median. Every final source has the single expected SHA-256
`e72d4368180829b2c2f832a95201ed707620db0a14b0486663b5469fd5f80592`.

Wall time was not primary and is not a product result. Median cold episode wall
was 133.13 seconds for Q and 38.02 seconds for R, with Q spanning 38.82–237.69
seconds. The Q arm made extra reads in five episodes and a second edit in two;
R made one extra read in one episode. The richer labeled read may have reduced
uncertainty, but that is a post-result inference and cannot be claimed from
this screen.

## Registered predictions scored

| prediction | registered | measured | score |
|---|---:|---:|---|
| median mutation-token reduction | 40%, plausible 32–48% | **45.16%** | inside range |
| median mutation-byte reduction | 42%, plausible 35–50% | **44.90%** | inside range |
| exact completion | 8/8 each | **8/8 each** | confirmed |
| strict R reference use | 7/8 | **8/8** | exceeded |
| wrong-subject | 0 | **0** | confirmed |
| median MCP round trips | 2 each | **Q 3, R 2** | falsified for Q |

The turn secondary does not strengthen or rescue the primary; the primary
already passed. It is an unregistered direction that needs its own forward-only
gate if pursued.

Price-model line: the median saving is 406 emitted tokens per task. At the
preregistered approximately 56.5 output tokens/second, that is about **7.2
seconds of decode-equivalent emission**. It is a price fold, not observed
product speedup; input prefill is not priced as output.

## Safety result and boundary

Every label carried snapshot SHA, file, owner, byte offsets, anchor SHA and
preview. Before the unchanged product handler ran, the proxy required:

- a known label from the immediately preceding read;
- identical current file SHA;
- identical readable file and owner;
- exact-one current anchor cardinality; and
- the fixture's expected target-content identity.

Unknown, stale, ambiguous, or mismatched references use a loud typed refusal
with `source_unchanged=true` and `mutation_attempted=false`. The real stdio
zero-model test exercised the successful lowering and the stale and
wrong-target refusal paths. In the model cohort, no such refusal fired and no
wrong label was attempted.

That is strong evidence for this one-span-per-owner fixture, not proof of
general safety. The experiment proxy has a frozen manifest oracle for the
expected `to` content, which a generic product does not. A replication should
include multiple same-owner labels, reordered labels, and adversarially similar
spans while preserving the pointer-to-readable-identity receipt law. A valid
but semantically wrong pointer must remain impossible or loud before mutation.

## Two stopped pre-model pilots

The evidence tree retains two forward-only launcher failures rather than
rewriting history:

1. v1 passed a relative workspace to Codex after changing into that workspace;
   both slots exited in under 0.09 seconds with empty event streams.
2. v2 fixed the path, launched the proxy, then timed out the required MCP
   handshake at 120 seconds because the Codex-owned environment did not retain
   the shell's `JAVA_HOME`. Both tool receipts and event streams are empty.
3. v3 supplied the exact resolved JDK only to the isolated product child. No
   experimental content or decision rule changed.

V1 and v2 are scored `environment_valid=false`, excluded from the v3 pilot and
cohort, and retained with their own SHA manifests. They consumed no model/tool
events and cannot be mistaken for negative arm evidence.

## Evidence grade and limits

- one synthetic fixture, one model, one subscription seat, one repository;
- same task repeated, so this tests emission behavior under a frozen decision,
  not general task diversity;
- read annotation adds input context, which is cheap under the measured price
  asymmetry but not free in every serving architecture;
- no formal prospective power calculation was registered. The fixed 8/arm
  sample, median rule, magnitude prediction, and kill floors were registered,
  but they are not a substitute for a power analysis;
- a synthetic screen may kill but cannot mint a performance claim.

The correct next rung is a new preregistration with a fresh, adversarial
multi-label fixture at n≥10/arm. Only after that survives should a matched
serial Anvil comparison test a real historical decision at an exact product
commit.

## Receipts and replay

The v3 evidence root contains 220 files / 2.6 MiB: every prompt, raw Codex
JSONL stream, stderr, proxy duplex stream, proxy receipt, full product MCP
telemetry, inverse receipt, workspace bytes, episode score, summaries, and run
configuration. `shasum -a 256 -c manifest.sha256` verifies every retained file.

- v3 run config: `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/run-config.json`
- v3 pilot: `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot-summary.json`
- v3 cohort: `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort-summary.json`
- v3 manifest SHA-256:
  `be49c1463562fcc1a81a2210353dfe963ee2b5f08d16a5655eb894bdcc76c460`
- stopped v1 and v2 roots: `raw/` and `raw-v2/`

Replay to a new output root; never overwrite retained evidence:

```sh
PYTHONDONTWRITEBYTECODE=1 \
/private/tmp/splice-reference-screen-venv/bin/python -B \
  bench/run_splice_reference_screen.py --run \
  --output /private/tmp/splice-reference-screen-replay \
  --auth-file /Users/genekim/.codex/auth.json \
  --expected-head da139e1805a56bf36d2d7562b6486854ac67a4fa
```
