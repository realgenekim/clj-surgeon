# Warm-executor economics screen

Persistent mechanism: one live `codex app-server` process and one live thread per model, with repeated `turn/start` requests.

| Model | cold trivial E2E | cold prepared bang | warm prepared bang | cold-warm | amortization (2s..1s) | exact | one-shot | wrong-subject |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `gpt-5.3-codex-spark` | 3593.1 ms | 6773.3 ms | 2287.6 ms | 4485.8 ms | 2..4 edits | 10/10 | 10/10 | 0 |
| `gpt-5.6-sol` | 4283.1 ms | 10087.9 ms | 6349.9 ms | 3738.0 ms | 3..5 edits | 10/10 | 10/10 | 0 |

## Cold-start decomposition

- `gpt-5.3-codex-spark` medians: bootstrap 102.3 ms; thread setup 401.8 ms; request-to-first-token 2943.4 ms; decode tail 62.9 ms.
- `gpt-5.6-sol` medians: bootstrap 99.9 ms; thread setup 408.6 ms; request-to-first-token 3515.6 ms; decode tail 253.3 ms.

## Reliability drift

- `gpt-5.3-codex-spark`: first-five exact 5/5; last-five exact 5/5; drift signal `false`.
- `gpt-5.6-sol`: first-five exact 5/5; last-five exact 5/5; drift signal `false`.

Registered winning-pattern gate: **TRUE**.

Provider auth, queueing, model materialization, prefix processing, and first-token decode remain bundled in request-to-first-token; the local protocol does not expose a defensible finer split.
