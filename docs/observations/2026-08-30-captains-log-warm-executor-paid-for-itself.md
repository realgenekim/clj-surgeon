# Captain's Log: the warm executor paid for itself

Date: 2026-08-30

Branch: `experiment/warm-executor-screen-20260830`

Frozen preregistration: `d1ce1b2`

Recovery addendum: `2855471`

Registered replacement addendum: `2ee7f0f`

## Verdict

The registered winning pattern passed.

One live Spark app-server and one live Spark thread cut the median prepared-edit
round trip from **6.773 seconds cold to 2.288 seconds warm**, a **4.486-second
(66.2%) reduction**. The ten-turn warm stream was 10/10 exact, 10/10 one-shot,
and 0 wrong-subject. Its first five and last five were both 5/5 exact, so there
was no registered quality-drift signal.

Sol preserved the same perfect quality. Its median prepared edit fell from
**10.088 seconds cold to 6.350 seconds warm**, a **3.738-second (37.1%)
reduction**. Warm Spark was 64.0% lower latency than warm Sol on this fixture.

This is a screen on one local machine, one subscription account, one CLI
version, one exact-edit task family, and a short serial schedule. It earns the
executor pattern for the next integration experiment; it is not a universal
latency estimate.

## Mechanism actually found

`codex-cli 0.149.1` supports a genuinely persistent local route through its
stdio app-server protocol. The harness kept one process alive, initialized it
once, started one thread per model, warmed it with one unscored turn, and sent
ten sequential `turn/start` requests to that same thread. It did not use
`codex exec resume`.

Protocol receipts prove exactly one thread ID and ten distinct ordered turn IDs
per model. Each scored turn exposed exactly one action shape:
`["mcpToolCall"]`, and every call named `clj-surgeon/edit_clojure` with the
prepared owner, old value, new value, and exact match count.

This is the closest local realization of Gene's riff: the executor process,
tool catalog, and conversational thread stayed resident between bangs. It does
not prove that a dedicated model instance stayed materialized on provider
hardware.

## Measurement 1: cold-start decomposition

Five valid fresh-process trivial turns per model produced:

| Model | Total E2E median | Process bootstrap | Thread setup | Request to first token | Decode tail | Min-max E2E |
|---|---:|---:|---:|---:|---:|---:|
| Spark | 3.593s | 102ms | 402ms | 2.943s | 63ms | 3.447-4.138s |
| Sol | 4.283s | 100ms | 409ms | 3.516s | 253ms | 4.144-6.513s |

The prediction that remote first-token work would dominate local bootstrap was
correct. Spark's 3.593-second median narrowly exceeded the registered
1.5-3.5-second range; Sol landed inside its 3.2-5.5-second range. Bootstrap was
approximately 0.1 second, at the low edge of the predicted 0.10-0.60 seconds.

The protocol cannot isolate provider authentication, queueing, prompt upload,
prefix processing, model materialization, reasoning, or the first decoded
token. Those remain bundled in request-to-first-token. Calling the 2.943s or
3.516s span "model materialization" would overclaim the evidence.

The frozen 1-2-second-per-edit amortization curve is:

| Model | Gross cold trivial cost | Edits at 2s savings | Edits at 1s savings |
|---|---:|---:|---:|
| Spark | 3.593s | 2 | 4 |
| Sol | 4.283s | 3 | 5 |

Using the observed matched benefit rather than the preregistered hypothetical
1-2 seconds, one subsequent Spark bang repays the gross cold cost; Sol requires
two. That secondary calculation is descriptive, not the registered gate.

## Measurement 2: warm dispatch

| Model | Exact cold comparator | Persistent warm | Median benefit | Reduction | Cold min-max | Warm min-max |
|---|---:|---:|---:|---:|---:|---:|
| Spark | 6.773s | 2.288s | 4.486s | 66.2% | 5.684-7.688s | 1.829-5.250s |
| Sol | 10.088s | 6.350s | 3.738s | 37.1% | 9.223-11.522s | 5.126-7.931s |

Both benefits were substantially larger than predicted: Spark was registered
at 1.0-2.0 seconds and Sol at 0.5-1.5 seconds. The local process bootstrap and
thread setup explain only about half a second. The rest is a full-system warm
effect that may include tool-catalog reuse, provider prefix caching, thread
state reuse, and load. The study does not identify their individual causal
shares.

Spark's first scored warm bang took 5.250 seconds; its remaining nine ranged
from 1.829 to 2.736 seconds. That first-bang shape is consistent with additional
one-time warm work after the unscored text-only seed, but the schedule did not
randomize or isolate that mechanism.

## Measurement 3: sustained reliability

| Model | Exact | One-shot | Wrong-subject | First five | Last five | Drift signal |
|---|---:|---:|---:|---:|---:|---:|
| Spark | 10/10 | 10/10 | 0 | 5/5 | 5/5 | false |
| Sol | 10/10 | 10/10 | 0 | 5/5 | 5/5 | false |

Every intermediate source hash matched the preregistered prefix state. Every
final source contained `:done-01` through `:done-10` in the intended owners and
no other workspace path changed. Quality did not decay as either thread grew.

## Frozen recovery accounting

Attempt 1 is retained rather than erased. It produced ten valid cold-trivial
turns, then ten prepared calls that were refused before mutation because the
client requested interactive write approval while the thread correctly denied
interactive approvals. The mechanical recovery was frozen and pushed before
more turns. Those refusals are excluded from latency and reliability because
the tool never executed; all remain in `attempt-1-retained`.

In the fixed schedule, Spark cold-prepared replicate 2 saw no MCP catalog and
made no tool call. It changed no bytes and scored wrong-subject 0, but it was
transport-invalid. The preregistered replacement rule was frozen in an
addendum; Spark replicate 6 was exact and became the fifth valid comparator.
The final fold reports six Spark attempts, five exact observations, and one
retained invalid observation.

## Route proof and replay

- `meta.json` records `Logged in using ChatGPT` under the stripped child
  environment.
- The parent had `OPENAI_API_KEY`, but every measured child records an empty
  metered-environment set. No direct API client was used.
- Runtime `CODEX_HOME` directories and auth symlinks were removed before Git
  staging. A sensitive-token scan returned no hits.
- `protocol.jsonl` files retain monotonic send/arrival clocks and complete
  app-server notifications; MCP telemetry and exact after-states are retained.
- `finalization.json` binds the primary result, replacement rows, final fold,
  and script hashes.

Deterministic zero-model replay:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 bench/replay_warm_executor_screen.py \
  bench/results/2026-08-30-warm-executor-screen
```

Fresh model replay uses `bench/run_warm_executor_screen.py`; because provider
load is not controlled, it reproduces the protocol and scorer, not identical
latencies.

## Decision and next falsifier

Keep the persistent Spark executor pattern alive for prepared guarded edits.
The next experiment should measure idle-age decay: hold identical Spark
executors idle for 0, 1, 5, and 15 minutes before the next prepared bang. That
would determine whether the economic win requires constant traffic or survives
the real gaps between Gene's edits. A second useful falsifier is a mixed task
stream that alternates exact edits with unrelated reasoning, to test whether
topic/context accumulation changes the 10/10 result.
