# Warm-executor economics screen: frozen preregistration

Status: **FROZEN BEFORE THE FIRST MODEL TURN**

Date: 2026-08-30

Branch: `experiment/warm-executor-screen-20260830`

Baseline: `origin/release/closed-relations-published` at
`b445a8c3595d70f6f05b6edccb9b1a924539a195`

Route: local Codex CLI using ChatGPT subscription authentication only

## Decision question

Does keeping one Spark executor process and conversation live make sequential
prepared edits economically attractive without loss of exactness, one-shot
behavior, or subject identity? GPT-5.6 Sol is the control.

The primary answer is not token price. It is complete local round-trip wall
time and exact guarded-edit reliability on the subscription route.

## Mechanism frozen before measurement

Local `codex-cli 0.149.1` exposes a persistent app-server protocol:

1. start one `codex app-server --listen stdio://` process;
2. send `initialize`, then `initialized`;
3. send `thread/start` once with the exact model, workspace, and sandbox;
4. send repeated `turn/start` requests against that same thread;
5. finish each observation at the matching `turn/completed` notification.

The protocol was established from the CLI's generated experimental JSON schema,
not from a model run. It emits `item/agentMessage/delta`, which provides an
observable first output token boundary. This is stronger warmth than
`codex exec resume`: the local Codex process and thread both remain live.

`codex exec resume` is a predeclared fallback only if the app-server refuses
repeated turns at runtime. A fallback result will be labeled as resumed context,
not persistent-process warmth. If neither mechanism works, the exact refusal is
the result; no fresh session will be relabeled as warm.

## Frozen route and isolation

- Models: `gpt-5.3-codex-spark` and `gpt-5.6-sol`.
- Reasoning effort: `low` for both.
- Parent `OPENAI_API_KEY` may exist, but it and provider/base-URL overrides are
  removed from every measured child process.
- Every isolated `CODEX_HOME` contains only a symlink to the exact existing
  ChatGPT `auth.json` plus the experiment's generated MCP config.
- `codex login status` under the stripped environment must say
  `Logged in using ChatGPT` or the experiment is killed before a turn.
- No direct Responses/API client is permitted.
- The MCP server is launched from this frozen worktree with `:tool-profile
  :edit`, `:nrepl-port :none`, a fresh telemetry directory, and an ephemeral
  port. Only `edit_clojure` is enabled in the Codex client config.
- The fixture is synthetic and outside the product source tree. The prohibited
  origin checkout is never used.
- Runs are serial. Model order alternates Spark/Sol then Sol/Spark by replicate
  or bang to spread provider-load drift.

## Measurement 1: cold-start decomposition

Five valid fresh-process trivial turns per model (`n=5`), with the exact prompt:

> Do not use tools. Reply with exactly `COLD_OK`.

Each observation launches a new app-server process and a new ephemeral thread.
The following client-observable phases are recorded with monotonic clocks:

- **process bootstrap**: process spawn to `initialize` response;
- **thread setup**: `thread/start` send to its response;
- **request to first token**: `turn/start` send to first
  `item/agentMessage/delta`;
- **decode tail**: first agent-message delta to `turn/completed`;
- **total E2E**: process spawn to `turn/completed`.

Provider authentication, queueing, request upload, prefix processing, model
materialization, reasoning, and first-token decode are not separately observable.
They remain explicitly bundled inside request-to-first-token. No report may
claim a finer decomposition.

Magnitude predictions:

- Spark median cold total E2E: 1.5-3.5 seconds.
- Sol median cold total E2E: 3.2-5.5 seconds.
- Median local process bootstrap: 0.10-0.60 seconds for each model.
- Most cold wall time will be in request-to-first-token, not local bootstrap.

The preregistered gross amortization band is computed per model as:

```text
fast-savings edits = ceil(median cold trivial E2E / 2.0 seconds)
slow-savings edits = ceil(median cold trivial E2E / 1.0 second)
```

It answers how many 1-2 second prepared-edit savings are needed to repay one
session start. A matched prepared-edit comparison below is reported separately.

## Measurement 2: warm dispatch latency

An unscored trivial turn first warms each persistent process and thread. The
same live process then receives ten guarded prepared edits (`n=10`, exceeding
the required eight). The primary warm latency is `turn/start` send through
`turn/completed` arrival.

For a work-matched cold comparator, five additional prepared edits per model
run in fresh app-server processes and ephemeral threads against fresh copies of
the same fixture. The reported number Gene asked for is:

```text
median fresh-process prepared bang E2E
versus
median persistent-process prepared bang round trip
```

Magnitude predictions:

- Spark persistent warmth saves 1.0-2.0 seconds versus its matched cold bang.
- Sol persistent warmth saves 0.5-1.5 seconds versus its matched cold bang.
- Spark's gross cold-start amortization is 1-4 prepared edits across the
  registered 1-2 second savings band; Sol's is 2-6 edits.

Economic success requires a median matched advantage of at least 1.0 second
for Spark. Sol is descriptive control, with 0.5 second as its registered
materiality threshold.

## Measurement 3: sustained-bang reliability

Each model gets one live process, one live thread, one fresh fixture, and ten
sequential prepared requests. Bang `i` supplies the complete guarded edit:

```text
file: src/warm_executor/fixture.clj
owner: slot-NN
from: :todo
to: :done-NN
matches: 1
```

No discovery is needed. The prompt requires exactly one `edit_clojure` call
and the terminal text `EXACT_OK`.

Per-bang scores are frozen as follows:

- **exact**: the tool receipt succeeds, its arguments match the supplied guard,
  and all workspace bytes equal the preregistered next state;
- **one-shot**: exactly one `edit_clojure` call is started and completed, with
  no command, file-change, or other tool call;
- **wrong-subject**: any non-target path changes, or the target file changes to
  bytes other than the exact next state. A clean refusal is inexact but is not
  wrong-subject.

Predictions:

- Spark: at least 9/10 exact, at least 8/10 one-shot, 0 wrong-subject.
- Sol: 10/10 exact, at least 9/10 one-shot, 0 wrong-subject.
- No quality drift: last-five exact count differs from first-five by at most
  one for either model.

Warm-executor reliability is rejected for any wrong-subject event. A drift
signal is registered if last-five exact is at least two below first-five.

## Validity and kill criteria

The harness stops before spending a model turn if any of these holds:

- stripped-environment auth is not ChatGPT subscription auth;
- a metered API environment variable reaches a measured child;
- the requested model is replaced or the thread response names another model;
- the repository HEAD/tree or fixture/harness hashes differ from the frozen
  preregistration commit;
- the MCP process is not launched from this worktree, fails readiness, or the
  client config enables anything except `edit_clojure`.

A cell is invalid unless it obtains five valid cold observations. Up to two
provider-error replacements are allowed after the fixed schedule; all failed
attempts remain in receipts. No latency outlier is discarded.

The warm stream is invalid if the process exits, the thread ID changes, or
fewer than eight prepared turns complete. Any wrong-subject event immediately
kills that model's stream and is retained. There are no corrective retries
inside a guarded edit.

The "winning pattern" requires all of the following:

1. Spark median matched warm advantage is at least 1.0 second;
2. Spark exactness is at least 9/10 and wrong-subject is 0;
3. Spark has no registered last-half drift signal;
4. gross Spark break-even is at most four edits at the conservative
   1-second-per-edit savings assumption.

Failing any gate is a negative result, not an invitation to tune the prompt.

## Receipts and analysis

The run retains protocol JSONL with monotonic arrival timestamps, stderr,
process commands with secret-free environment facts, MCP telemetry, fixture
hashes before and after every bang, model/thread/turn IDs, exact tool arguments,
and per-bang scores. The summary uses medians, min/max, first-five versus
last-five counts, matched cold-minus-warm deltas, and the registered
amortization formula. No post-hoc sample exclusion or magnitude threshold is
permitted.
