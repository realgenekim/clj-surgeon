# Read-request normalization live-route token measurement

Date: 2026-08-30 PT

Verdict: **measurement valid; semantic and route gates pass; the candidate is
not a net token win for an ordinary short-lived MCP connection.** The new call
shapes save 4 or 12 `o200k_base` proxy tokens per eligible request, but the
`inspect_clojure` catalog grows by 317 tokens. At MCP wire startup, that growth
requires 80 operation-less single calls or 27 omitted-ID two-request calls to
break even. This receipt measures only. It does not authorize or perform an
install.

## Exact subjects

- causal control: `b9db064a86c3919660a38f79ab5031dcf6d49f98`
  (`e5dd6183de87318bee8689a6129252c51d954e8a` tree)
- candidate: `c55de2279826af5ed21c90981591479dd2e802b2`
  (`565f009f0ff25fdedbc2fba5ad9ba5f55783e023` tree)
- installed stable reference: `19ab864889799b0028a5f7cb66c63b957ff7b973`
- control equivalence: the five relevant MCP surface files are byte-identical
  between the causal control and installed stable reference
- preregistration/harness: `5294ce9ba470d64dfae88c371bdac2b42f5cb93f`
- timing-fold repair: `78c553094578f6c1029ce3a561a3e0466c42c20b`

Both measured worktrees were clean before launch and remained clean after the
measurement.

## Route and validity

The harness launched two private processes through the real
`-X:clj-surgeon/mcp-stdio` entrance. Each used `-J-Xms64m -J-Xmx512m`, a
private fixture, private receipt and log paths, telemetry off, and no nREPL.
It did not register an MCP server, edit MCP configuration, bind a shared port,
reload a server, or touch the shared runtime.

| Gate | Result |
|---|---|
| `environment_valid` | true |
| `semantic_correct` | true; every explicit and shorthand result matched after removing timing only |
| `route_adherent` | true; actual MCP stdio JSON-RPC 2.0 |
| candidate mixed-ID refusal | `mixed-request-ids`, `source_unchanged=true`, `read_started=false` |
| source hash before/after | `b9fb65592299a497c43d1cba0e7938158d9897888ad6ca5ce82d71a18ed3fd21` / identical |

Both private servers were terminated after capture. Their recorded post-signal
exit status is 143. No measured server process remains.

## Token basis

All token counts use local `tiktoken 0.9.0`, encoding `o200k_base`, over the
exact compact UTF-8 JSON payload without the newline transport delimiter. This
is the same proxy basis as the emission-carriage corpus. It is a stable local
comparison, not provider billing authority.

An independent second fold over the retained files reproduced every headline
byte and token count below.

## Catalog cost

Negative savings are losses and remain visible.

| Serialized surface | Current bytes / tokens | Candidate bytes / tokens | Candidate delta |
|---|---:|---:|---:|
| complete `tools/list` response | 48,102 / 10,817 | 49,490 / 11,134 | **+1,388 / +317** (+2.89% / +2.93%) |
| complete `inspect_clojure` definition | 13,171 / 3,088 | 14,559 / 3,405 | **+1,388 / +317** (+10.54% / +10.27%) |
| `inspect_clojure` input schema | 6,481 / 1,641 | 7,869 / 1,958 | **+1,388 / +317** (+21.42% / +19.32%) |

The complete catalog growth is entirely attributable to the changed
`inspect_clojure` definition in this comparison.

## Call costs

The request columns are the complete live JSON-RPC envelopes. The arguments
columns isolate what a model must construct. Response deltas of a few bytes on
successful calls are timing serialization only; response token counts are
identical after semantic equality passed.

| Causal comparison | Current request B/T | Candidate request B/T | Request saving | Current args B/T | Candidate args B/T | Args saving | Current response B/T | Candidate response B/T |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| explicit single, same shape | 404 / 112 | 404 / 112 | 0 / 0 | 307 / 83 | 307 / 83 | 0 / 0 | 1,675 / 559 | 1,674 / 559 |
| operation-less single vs current explicit | 404 / 112 | 384 / 108 | **20 / 4** (4.95% / 3.57%) | 307 / 83 | 287 / 79 | **20 / 4** (6.51% / 4.82%) | 1,675 / 559 | 1,671 / 559 |
| explicit-ID multi, same shape | 507 / 143 | 507 / 143 | 0 / 0 | 410 / 114 | 410 / 114 | 0 / 0 | 2,496 / 877 | 2,497 / 877 |
| omitted-ID multi vs current explicit IDs | 507 / 143 | 473 / 131 | **34 / 12** (6.71% / 8.39%) | 410 / 114 | 376 / 102 | **34 / 12** (8.29% / 10.53%) | 2,496 / 877 | 2,497 / 877 |
| mixed IDs, same invalid request | 490 / 137 | 490 / 137 | 0 / 0 | 393 / 108 | 393 / 108 | 0 / 0 | 668 / 175 | 696 / 183 |

The candidate's mixed-ID response costs 28 bytes and 8 tokens more because it
returns the new typed refusal and pre-read safety evidence.

## What a caller pays

At the serialized MCP wire boundary:

- an unchanged explicit call saves nothing;
- an operation-less single read saves 4 request tokens;
- an omitted-ID two-request read saves 12 request tokens; and
- the catalog costs 317 additional tokens when listed.

For one catalog listing per MCP connection, the catalog delta alone amortizes
after 80 operation-less single calls (`ceil(317 / 4)`) or 27 omitted-ID
two-request calls (`ceil(317 / 12)`). A connection with one shorthand call is
therefore 313 or 305 proxy tokens larger before response accounting. This
measurement does not establish how often a particular agent client reinjects
the catalog into model context, so it does not convert that wire result into a
provider-billed total.

Token-only install reading: the shorthand is real and correct, but the live
route does not support a net token-reduction claim for ordinary sessions.
Correctness or usability value, if used to justify installation, must be stated
separately from performance.

## Retained evidence

Valid capture:

- `bench/results/2026-08-30-read-normalization-live-route/`
- report SHA-256:
  `d688af42fb7ec82470849299fbf1bc5bdf9ec31ace6b063e8f651ee4a0cc635a`
- manifest SHA-256:
  `27c988656c8cb5812f9017c659d7eadda87e3a2a211a5ca7c4de0dce6b989e7b`
- control schema SHA-256:
  `843fc55507ae96ad32b9d3463d300a50f245a318e6f784bfaa6fe0dadddcac91`
- candidate schema SHA-256:
  `4782eba0d8eebb930d04dacdc3b1e43e0cba6fac1fe7bdf6f0c4f6a63a2829c4`
- current explicit-single request SHA-256:
  `726ab29440f6e98737b2daf1b544988251ae0a47ec244fe4d8cd8441de1a5c95`
- candidate operation-less request SHA-256:
  `2f19944db943c348d2a26148ddf75d518ea8a6741b130dd409380e81529672be`
- current explicit-ID multi request SHA-256:
  `1bd27a9ecdcefd9bc1a4f685da6454160aeaae2e84aec5906919a2ca0425ced8`
- candidate omitted-ID multi request SHA-256:
  `44a47ac99c8da57cbe89d46068326ea6012a536e004ea3c03ec7f743772eed07`
- candidate mixed-ID refusal response SHA-256:
  `d552d5f83808c1e186b56042da93007090316eaf351d2b06eab6f728fb63c303`

The first live attempt is also retained at
`bench/results/2026-08-30-read-normalization-live-route-attempt1-invalid/`.
Every route and refusal gate passed, but the first scorer omitted
`inspection_elapsed_ms` from its timing-only normalization. It therefore
rejected semantically equal responses. The protocol had already specified
timing removal, so commit `78c5530` made that law executable before the fresh
whole measurement. No result from the invalid attempt is used above.

## Exact replay

From this measurement branch and a clean exact candidate worktree:

```sh
git worktree add --detach \
  /private/tmp/clj-surgeon-read-normalization-control \
  b9db064a86c3919660a38f79ab5031dcf6d49f98

/Users/genekim/anaconda3/bin/uv run \
  --no-project \
  --with tiktoken==0.9.0 \
  python dev/experiments/measure_read_normalization_live_route.py \
  --control-worktree /private/tmp/clj-surgeon-read-normalization-control \
  --candidate-worktree /private/tmp/clj-surgeon-lid-ratchet-20260829 \
  --result-dir /private/tmp/clj-surgeon-read-normalization-replay
```

The harness refuses an existing result directory, dirty or wrong head/tree
subjects, a control surface that differs from the installed reference,
semantic mismatch, unsafe mixed-ID handling, or any fixture mutation.
