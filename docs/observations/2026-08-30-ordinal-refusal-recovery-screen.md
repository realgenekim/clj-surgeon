# Ordinal refusal recovery: complete vocabulary plus one confirmation hole

Date: 2026-08-30 PT  
Status: **prototype screen passes its registered mechanism gate**  
Boundary: **experiment-only evidence for a later ratification decision; not product code or authority**

## Result in one sentence

On a frozen 27-owner selection refusal, complete numbered vocabulary plus one
non-executable ordinal hole reduced mean recovery reads from **1.00 to 0.25**
(-75%) and produced index/hole-fill recovery in **7/8** treatment episodes,
with **16/16 exact outcomes and zero wrong-subject mutations**; it saved a
median **2.975 seconds** but did **not** save output tokens.

## Why this screen existed

The write/read evidence study counted 191 `batch-form-selection-failed`
refusals among 283 total. The server truncated 86.4% of their candidate lists
to ten despite a median available count of 27; only 16.8% showed the requested
name, and 98.4% were followed by a recovery read. Those reads returned 2.72 MB
redundantly. Refusal-to-retry pairs re-emitted 46.1% of all write bytes, 90%
mechanically derivable, while only 1/283 refusals carried a next call.

Sweep-1 at `c1e89d5d` established the vocabulary mechanism causally: complete
owner vocabulary produced 0/10 rereads against 10/10 with truncation. The
write-refusal packet at `6d558cb3` is pre-ratification and explicitly forbids
executable product refusal payloads. This screen asked a narrower experimental
question: if a proxy pairs complete vocabulary with one caller-owned ordinal
confirmation hole, will Sol use it, and what incremental cost disappears?

## Frozen experiment

- Caller: `gpt-5.6-sol`, reasoning `high`, fresh ephemeral Codex process and
  credential-only `CODEX_HOME` per episode.
- Surface: fresh local stdio MCP proxy per episode; no product process, shared
  port, install, reload, or product source change.
- Fixture: exactly 27 top-level Clojure owners. The intended singular
  operations dashboard is one-based ordinal 19. Every owner contains
  `:status :pending`, so an incorrect ordinal performs an observable
  wrong-subject mutation instead of failing harmlessly.
- Controlled refusal: the exact first request names near miss
  `render-dashbord` and returns `batch-form-selection-failed`.
- Control: first 10 names, `available_form_count=27`, `truncated=true`.
- Treatment: complete numbered 27-name list plus exactly one template with
  `executable=false`, `authority=false`, `write_authority=false`, and one
  `candidate_index` hole. A separate model-authored call revalidates refusal
  identity and source hash before applying the frozen edit.
- Pilot: `C T T C`, n=2/arm, never pooled. Main: fixed interleaved `C T T C`
  blocks, stopping at eight fully valid episodes per arm.

The proxy, refusal, launch, episode score, aggregate, and archive receipt all
state:

> EXPERIMENT SURFACE ONLY — not product code, not product authority, and not
> compliant evidence that product refusals may carry executable payloads.

## Preregistered predictions and observed values

| Outcome | Prediction | Observed | Score |
|---|---:|---:|---|
| Mean recovery reads, C | 0.75--1.25 | 1.00 (8/8 read once) | in range |
| Mean recovery reads, T | 0.00--0.25 | 0.25 (6/8 zero; 2/8 one) | boundary |
| Relative read reduction | 87.5%--100%; kill below 50% | **75.0%** | gate passes, magnitude misses |
| T index/hole-fill | at least 6/8; kill below half | **7/8** | passes |
| Median output-token reduction | 35%--60% | **-5.6%** (T used 45 more) | falsified |
| Median wall reduction | 25%--45% | **23.7%** (2.975 s) | just below range |
| Wrong subject | 0 | **0/16** | passes |
| Exact semantic result | at least 8/8 each arm | **8/8 each** | passes |

The pilot was genuinely sub-ceiling. All four episodes were valid and safe,
but both treatment episodes still reread; one then used the ordinal hole and
one retyped the request. The main cohort was not changed in response.

## Main cohort detail

| Arm | n | Recovery-read values | Recovery mode | Median output tokens | Median post-refusal emitted bytes | Median refusal-to-success |
|---|---:|---|---|---:|---:|---:|
| C | 8 | `1 1 1 1 1 1 1 1` | 8 retyped | 800 | 315.0 | 12,568.538 ms |
| T | 8 | `0 0 1 0 0 1 0 0` | 7 ordinal, 1 retyped | 845 | 297.5 | 9,593.921 ms |

The exact saved quantity on this surface is therefore **0.75 recovery reads
per treatment episode** and **2,974.617 ms at the median**. Treatment's
post-refusal emitted bytes fell only 17.5 bytes at the median (5.6%), while
Codex's authoritative whole-turn output meter rose 45 tokens (5.6%). The
ordinal removed a round trip; it did not remove reasoning or narration.

## Interpretation

This screen answers the prototype question **yes, with a qualification**.
Sol can recover by one index: it did so in seven of eight treatment episodes,
and six of those seven skipped the read entirely. The complete list plus hole
did not guarantee zero reads—two treatment episodes still inspected source,
including one that later used the index—and it did not save emitted tokens.

The result supports a turn-removal mechanism, not an emission-compression
mechanism. It also does not supersede `6d558cb3`: the product no-executable-
payload invariant remains untouched. The proxy's ordinal executor was useful
precisely because it made wrong-index risk measurable. Zero wrong subjects in
16 trials clears this prototype's mandatory safety stop, not the stronger
burden required for product authority.

## Receipts

- Base packet commit: `6d558cb3b5859cce6626fb67225c547483dc646f`
- Sweep-1 evidence commit: `c1e89d5d1b1f23d1655ef82f941a9d7be5624713`
- Frozen preregistration SHA-256:
  `76aa53efb4c02ca36f22f2aeb7e708629fd48aa6e1daff22566ff685c0cf5b60`
- Freeze receipt SHA-256:
  `62128da95763131cb36571a1e3b27c5bca1a61b78b7465fd6b154c567ef7fed6`
- Pilot receipt SHA-256:
  `1f29f029233af3953cee50a0f3fe8f1248b38c6c0860bb5c3d11de9d4374633d`
- Aggregate SHA-256:
  `845780b7f34cb2fab923ceac074750fd9177e447f652f5f34d32b3436afc38c7`
- Result receipt SHA-256:
  `77e9157935ea1948ce830620b11e972c7b310f0c62aca9719e29d5230bafcfc3`
- Retained artifact manifest SHA-256:
  `341d6c04d80adee4826ae36c68e1f296ba613ee39bd7685aca25adceefa9b492`
- Local archive SHA-256:
  `7104f1158b73550cc84fa1e69589a4bc8029a161297a374b2177fc010b86f512`
- Credential material in archive: `false`

The archive contains 137 retained artifacts, including every raw Codex JSONL
stream, stderr, proxy event log, workspace before/after evidence, launch
receipt, score, aggregate, and manifest. The targeted experiment does not
advance the agent-usage window marker.
