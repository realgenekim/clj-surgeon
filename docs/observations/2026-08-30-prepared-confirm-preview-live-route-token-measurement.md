# Prepared confirm/preview live-route token measurement

Date: 2026-08-30

Verdict: **GO to Gene's install decision, not an install or performance
claim.** Exact candidate `05f5a196` makes the public confirm/fill and preview
routes usable on real stdio JSON. On this one-owner prepared edit, the compact
arguments are 130 bytes / 64 `o200k_base` tokens instead of 288 bytes / 84
tokens: **158 bytes and 20 emitted proxy tokens removed**. That is a measured
23.8% argument-token reduction, materially smaller than the old projected
217-byte-to-30-byte shorthand.

## Exact subjects and route

- Control: `9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
  `6f9bc30316eb6417977c07c86caf8eb146dfbdb8` — the installed
  WRITE-REFUSAL-001 surface.
- Candidate: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`, tree
  `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`.
- Transport: two private 512 MiB MCP stdio processes, JSON-RPC 2.0 protocol
  `2024-11-05`; telemetry and nREPL off.
- Token proxy: local `tiktoken==0.9.0`, `o200k_base`.
- `environment_valid=true`, `route_adherent=true`, and every semantic gate is
  true. Shared runtime, registration, install, and reload were untouched.

## Relay-quotable result

| Surface | Control/full | Candidate/compact | Delta |
|---|---:|---:|---:|
| Full `tools/list` | 50,550 B / 11,402 T | 51,719 B / 11,663 T | **+1,169 B / +261 T input** |
| Exact `inspect_clojure` tool | 15,619 B / 3,673 T | 16,138 B / 3,798 T | **+519 B / +125 T input** |
| Exact `edit_clojure` tool | 7,800 B / 1,784 T | 8,450 B / 1,920 T | **+650 B / +136 T input** |
| Eligible inspect response | 2,313 B / 739 T | 2,544 B / 821 T | **+231 B / +82 T input** |
| Commit arguments | 288 B / 84 T | 130 B / 64 T | **-158 B / -20 T output** |
| Complete commit request | 380 B / 113 T | 222 B / 93 T | **-158 B / -20 T output** |

`T` means `o200k_base` proxy tokens, not provider billing tokens. The compact
call deletes 54.9% of argument bytes but only 23.8% of argument tokens. This is
another direct case where bytes are not a safe token proxy across formats.

The compact request committed the same exact future source as the full
prepared arguments. A second compact commit returned
`prepared-confirmation-consumed` with source unchanged. The served
confirmation remained session-bound, single-use, non-executable, and without
write authority.

## Preview price

The explicit preview request was 237 bytes / 98 tokens complete and 145 bytes
/ 69 tokens in caller-emitted arguments. Its complete response was 1,286 bytes
/ 454 input tokens. It returned one complete diff and exact hashes while the
source stayed byte-identical. Preview is therefore a measured optional review
cost, not a speed improvement; its value is zero-effect safety.

## No-cue compatibility

An ineligible inspect call used byte-identical 242-byte / 71-token requests.
Raw responses differed by 3 bytes / 1 token only because per-run timing text
differed. After excluding named timing fields and private arm roots, both
structured results were byte-identical at 1,127 bytes / 299 tokens, SHA-256
`7836d2388397bcf73daa0f0ba7a7181f0cdba96465945166304d8d1ba370cb29`.
No confirmation cue appeared.

An ordinary direct edit used byte-identical 219-byte / 72-token requests,
committed the same exact source in both arms, and produced byte-identical
normalized results at 414 bytes / 148 tokens, SHA-256
`7bc2c2b9daaca34bc36f17d6f54fcc9f24889d715f79017667fda0bb5a96e6b8`.
Raw responses retain elapsed, receipt, undo, UUID, and private-path variation.

## Price model and claim boundary

The +261 catalog tokens and +82 eligible-response tokens are caller input and
the catalog is cacheable. They are cheap relative to model-emitted output, but
not free. The measured paid-path benefit is 20 fewer caller-output proxy
tokens for this one-owner edit. The candidate commit response added only 7
bytes / 3 input tokens in this run.

Prior prepared-request cohorts measured fewer construction refusals and lower
recovery output. Those effects motivate installation but do not become a W1/W2
product claim from this screen. This zero-model measurement does not prove
routing lift, adoption, fewer turns, complete-wall improvement, or provider
billing cost. W2 preview is an explicit additional round trip and must never be
counted as deleted wall time.

## Invalid attempts retained

Attempt 1 passed a relative result path into servers launched from separate
worktrees, so the fixture root resolved incorrectly and the first eligible
read refused. Commit `59876942` fenced every caller path to an absolute path.

Attempt 2 passed every product route but correctly failed its no-cue scorer
because `inspection_elapsed_ms` remained in the normalized result. It was the
only difference. Commit `027b5027` added that named timing field to the frozen
exclusion and made the preview validity field explicitly boolean. The complete
experiment then reran with fresh processes and workspaces. No value from
either invalid directory is scored.

## Evidence

- Valid report SHA-256:
  `93a00fddb452b3d96e943d5208a2284504f95348fdad8770767d4c8ce9106537`
- Valid manifest SHA-256:
  `690f7afcec446e17ae351b94c04b009d5e477dbf4a5c1d6f5a84b4348aee23bc`
- Measurement script SHA-256:
  `f05270c851b1927b4e61dee4dff18a24b225138ce2927c296ffcfaa68db51bb3`
- Control/candidate `tools/list` SHA-256:
  `92d3409421c0ff046aeaf6f922395c302f3f18b644e8c6ef4612461733a19645`
  / `6cc0091b4050942cf5f7fe2f852e72d3f71c451854b19215961c74f533367f5c`
- Full/compact argument SHA-256:
  `4d5a76a49cde1fc5e94e85d21a53f1115101fbf0ec1132066cee1ae6ffbe725b`
  / `a0343adb578be5f3cb5f831698d1d5bc13b5f9284171d799edd728ec23c071f2`
- Preview response SHA-256:
  `fb59179aaad6081e311be1cd72db257aea1d3dc544cd847f71a447440f5cbc20`

All raw requests, responses, exact commands, stderr, fixtures, receipts,
normalized no-cue results, report JSON, and the manifest are retained under
`bench/results/2026-08-30-prepared-confirm-preview-live-route/`.

## Exact replay

From this branch, with the two exact clean detached worktrees:

```sh
/Users/genekim/anaconda3/bin/uv run \
  --no-project \
  --with tiktoken==0.9.0 \
  python dev/experiments/measure_prepared_actions_live_route.py \
  --control-worktree /private/tmp/clj-surgeon-prepared-control.xdX9b4 \
  --candidate-worktree /private/tmp/clj-surgeon-prepared-candidate.NqpX1e \
  --result-dir /private/tmp/clj-surgeon-prepared-actions-replay
```

The harness refuses a pre-existing output directory, dirty or wrong commit or
tree, semantic mismatch, preview mutation, replay success, source mismatch,
no-cue difference after the named exclusions, or any route that does not pass
through real MCP stdio JSON.

