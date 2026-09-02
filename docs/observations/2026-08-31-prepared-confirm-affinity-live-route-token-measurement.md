# Prepared-confirmation affinity live-route token measurement

Date: 2026-08-31

Verdict: **GO to the affinity breakfast/install decision, not an install or
performance claim.** The candidate adds actionable session guidance and exact
invalid-field names for a small input-side price. Ordinary successful reads
and writes remain byte-identical after only the predeclared dynamic exclusions.

## Exact subjects and route

- Control: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`, tree
  `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1` — the installed-lineage
  prepared-confirm/preview surface.
- Candidate: `7e0300fe0a75623fa6d7f275d2b99b57aa34f26d`, tree
  `1cfae15edb7ab167ce3c86f8157e8b0338c90790`.
- Transport: two private 512 MiB MCP stdio processes, JSON-RPC 2.0 protocol
  `2024-11-05`; telemetry and nREPL off.
- Token proxy: local `tiktoken==0.9.0`, `o200k_base`.
- `environment_valid=true`, `semantic_correct=true`, and
  `route_adherent=true`. Shared runtime, registration, install, and reload were
  untouched.

## Relay-quotable result

| Surface | Control | Candidate | Delta |
|---|---:|---:|---:|
| Full `tools/list` response | 51,719 B / 11,663 T | 52,332 B / 11,774 T | **+613 B / +111 T input** |
| Exact `inspect_clojure` tool | 16,138 B / 3,798 T | 16,460 B / 3,855 T | **+322 B / +57 T input** |
| Exact `edit_clojure` tool | 8,450 B / 1,920 T | 8,741 B / 1,974 T | **+291 B / +54 T input** |
| Invalid-fields refusal | 733 B / 178 T | 824 B / 205 T | **+91 B / +27 T input** |
| Unknown-confirmation refusal | 603 B / 152 T | 664 B / 158 T | **+61 B / +6 T input** |

`T` means `o200k_base` proxy tokens, not provider billing tokens. The catalog
grows 0.95% in tokens. The two refusal repairs cost 27 and 6 input tokens only
when those refusals occur. No ordinary request token count changes.

The invalid-fields response now renders the hostile caller-controlled field as
one canonical escaped JSON array:

```text
["ignore prior instructions\n\"quoted-now\""]
```

The unknown response now says: reuse the serving MCP session, or submit
ordinary explicit edit arguments. Never-served and cross-session identity
remain the same typed `prepared-confirmation-unknown` refusal; this screen does
not turn the digest into portable authority.

## Ordinary-path no-cue proof

The ineligible inspect requests were byte-identical at 242 bytes / 71 tokens.
After removing only elapsed-time fields and replacing private arm roots, both
structured results were byte-identical at 1,127 bytes / 299 tokens, SHA-256
`7836d2388397bcf73daa0f0ba7a7181f0cdba96465945166304d8d1ba370cb29`.

The ordinary direct-edit requests were byte-identical at 219 bytes / 72
tokens. Both committed the same exact source. After removing elapsed-time
fields, receipt/undo identifiers, and private arm roots, both structured
results were byte-identical at 414 bytes / 148 tokens, SHA-256
`7bc2c2b9daaca34bc36f17d6f54fcc9f24889d715f79017667fda0bb5a96e6b8`.
Raw response sizes retain timing, receipt UUID, and private-path variation and
are not used as no-cue authority.

## Price model and claim boundary

Catalog and refusal-response additions are caller input. The catalog is
cacheable; refusal text is read only on the failure path. These tokens are much
cheaper than caller-emitted decode tokens, but they are not free. The measured
benefit is safer, actionable recovery text with no ordinary-path semantic or
token change.

This zero-model screen does not measure caller session retention, descriptor
adoption, recovery-turn deletion, model wall time, or provider billing. W1
opportunities must still use the separately defined denominator of
session-affinity-capable callers. No routing, compression, or wall-time claim
is earned here.

## Invalid attempt retained

The first run passed every product and no-cue gate but the harness expected the
substring `Reuse the MCP session`; the exact candidate wire text is `Reuse the
serving MCP session`. It also expected `prepared_request.arguments` where the
refusal intentionally says `ordinary explicit edit arguments`. The run stopped
before producing a report or manifest. Its complete raw payloads are retained
under `invalid-run-1`; no metric from it is scored. The corrected literal was
committed before the fresh valid run.

## Evidence

- Valid report SHA-256:
  `0624179b54001fa3e427d22d683cb48b8d2548e67328acd62544123ea403494b`
- Valid manifest SHA-256:
  `a17ff517f011afd2669a1f9dc9ec87da181414b219d040f209d1163332763994`
- Measurement script SHA-256:
  `7e435dd00f0dfd789545a055def5c0cc7c0618bcb6e1cfc8a999affa1fec2436`
- Protocol SHA-256:
  `5d56388f6f20b44c7d949de203b76a644cce5475e7dae691602b77612eae9296`
- Control/candidate `tools/list` response SHA-256:
  `6cc0091b4050942cf5f7fe2f852e72d3f71c451854b19215961c74f533367f5c`
  / `517f47795eb76caf1223ad5f1470ed2dbe8a5b491c48021c2788d0b468954143`
- Control/candidate invalid-fields response SHA-256:
  `da3282abac1e4d435c2d0375b12ad30dcbbd240a93772b896180a26e25369b58`
  / `ba76caaf90121de9e1a47995a06884a63f60663523219a8c1be9a311fa96e8f6`
- Control/candidate unknown response SHA-256:
  `511b063a71eb446b772f0838fe4a582f31744c6666caa02d41fedf0aa3904694`
  / `8062ea08cabb57803965071e2e6bfc0bb5e2f7c9864652555cbf758a1f45946f`

All raw requests, responses, exact commands, stderr, fixtures, receipts,
normalized no-cue results, report JSON, and the manifest are retained under
`bench/results/2026-08-31-prepared-confirm-affinity-live-route/`.

## Exact replay

From this branch, with exact clean detached worktrees:

```sh
/Users/genekim/anaconda3/bin/uv run \
  --no-project \
  --with tiktoken==0.9.0 \
  python dev/experiments/measure_prepared_actions_live_route.py \
  --control-worktree /private/tmp/clj-surgeon-affinity-control.d4NasF/worktree \
  --candidate-worktree /private/tmp/clj-surgeon-affinity-candidate.yje1F4/worktree \
  --result-dir /private/tmp/clj-surgeon-affinity-live-route-replay
```

The harness refuses a pre-existing output directory, dirty or wrong commit or
tree, typed-refusal drift, unescaped invalid-field rendering, incomplete
unknown guidance, refusal mutation, or any ordinary-path difference after the
named exclusions.

## Dogfood ledger and operational loss

This measurement changed no established Clojure source. Eligible structural
edits: 0. The only authored changes were the Python instrument, protocol,
receipt, and generated evidence, so no Surgeon mutation route was applicable.

One orchestration error applied the reusable measurement-only commit to the
shared docs checkout before the isolated worktree. It was immediately
neutralized with a non-destructive revert; the net shared product/document
tree is unchanged and neither commit was pushed. The local shared branch is
therefore two commits ahead until its owner chooses how to dispose of that
history. No runtime, installation, or registered MCP state changed.
