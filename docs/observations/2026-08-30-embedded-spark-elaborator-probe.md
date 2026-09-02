# Embedded Spark elaborator feasibility probe

**Date:** 2026-08-30

**Host:** Skiff, macOS arm64

**Installed Codex:** `codex-cli 0.149.1`

**Repository basis:** `b445a8c3595d70f6f05b6edccb9b1a924539a195`

**Experiment branch:** `experiment/embedded-spark-probe-20260830`

## Verdict

Yes: the installed CLI has a viable non-terminal mode for a warm Spark
elaborator. The best candidate is a **dedicated, supervised**
`codex app-server --listen stdio://` child owned by the clj-surgeon MCP server.
It accepts JSONL-framed JSON-RPC-shaped messages, stays alive across turns,
loads the existing ChatGPT subscription auth, pins
`gpt-5.3-codex-spark`, supports an ephemeral thread, and constrains each final
message with `turn/start.outputSchema`.

The live probe pinned Spark without provider fallback and completed five turns
in one child. All five outputs parsed, all five were byte-identical, all five
matched the frozen expected `edit_clojure` arguments object, and no turn emitted
a command, file-change, MCP, dynamic-tool, web-search, or image-generation item.
Process start through initialized response took **90.284 ms**. Turn latency was
**1.240 / 2.012 / 9.064 seconds min / median / max**. The 9.064-second first
turn is a warning: a resident process removes CLI startup and handshake cost;
it does not prove a locally resident model or eliminate backend cold-tail
latency.

This is a feasibility pass, not production admission. One public-contract gap
remains: the installed `turn/start` schema can constrain output and the client
can detect tool items, but it does **not** expose a hard “disable all built-in
tools” switch. The five tool-free successes are positive evidence, not a hard
security boundary. Admission therefore requires process isolation plus a
failing adversarial test that proves a tool attempt cannot touch source or
escape the one-candidate budget.

## Scope and evidence discipline

This probe used no product code and contacted no shared clj-surgeon, cclsp, or
Codex daemon runtime. The harness started one fresh app-server child with:

- a temporary mode-0700 `CODEX_HOME`;
- a mode-0600 copy of the existing `auth.json`, deleted after shutdown;
- an empty `config.toml`, so configured MCP servers were absent;
- all API-key and access-token environment variables removed;
- an empty temporary workspace;
- a read-only sandbox, `approvalPolicy: "never"`, no dynamic tools, and an
  ephemeral thread.

The child itself reported `account.type = "chatgpt"`, plan `pro`, and requested
OpenAI auth. The installed catalog marks Spark `supported_in_api: false`, and
the model call succeeded with API-key variables scrubbed. This is direct
evidence that the probe used the existing ChatGPT subscription route, not a
metered API key. No auth-file contents appear in the receipts.

Primary evidence is:

- installed binary help, generated schemas, model catalog, and live protocol;
- the matching OpenAI source tag `rust-v0.149.1`;
- the [official Codex app-server documentation](https://developers.openai.com/codex/app-server),
  [CLI command reference](https://developers.openai.com/codex/cli/reference),
  and [deprecated MCP-server documentation](https://developers.openai.com/codex/mcp-server);
- raw files in
  [`2026-08-30-embedded-spark-elaborator-probe/`](2026-08-30-embedded-spark-elaborator-probe/).

The installed npm package itself ships only a top-level `README.md` plus the JS
launcher and platform binary. That README documents ChatGPT-plan login and
links to online docs; it does not include an app-server protocol guide. The
binary compensates by shipping `app-server generate-json-schema` and
`generate-ts`. The generated experimental V2 schema is hashed in the recon
receipt.

## Phase 1: installed protocol reconnaissance

### Mode inventory

| Installed mode | Protocol and framing | Session and model pinning | Auth and warmth | Output bounds | Finding |
|---|---|---|---|---|---|
| `codex app-server --listen stdio://` | Bidirectional JSON-RPC 2.0 shape with the `jsonrpc` header omitted; one JSON object per line. `initialize` then `initialized` is mandatory. | `thread/start.model` pins a thread; `turn/start.model` can override subsequent turns. `allowProviderModelFallback: false`, the `thread/start` response model, and `model/rerouted` events let the host enforce identity. Durable, resumed, forked, and ephemeral threads exist. | Reads normal Codex state from `$CODEX_HOME`; managed ChatGPT auth persists and refreshes there. One process/connection can serve many turns and retain loaded thread state. The actual model remains a remote service. | `turn/start.outputSchema` constrains the current final message. `turn/interrupt`, a client deadline, and a client byte ceiling can terminate work. There is no `max_output_tokens`, `max_output_bytes`, or hard `disable_builtin_tools` field in the installed thread/turn schema. | **Best candidate.** Use a dedicated stdio child, not a shared daemon. |
| `codex app-server --listen ws://…` or `unix://…` | Same messages; one JSON-RPC message per WebSocket text frame. Unix sockets use a WebSocket upgrade. OpenAI marks WebSocket transport experimental/unsupported; non-loopback listeners need explicit auth and TLS termination. | Same thread/turn model controls. | Same Codex home and process semantics. | Same turn controls; WebSocket ingress additionally has bounded queues and overload error `-32001`. | Useful for remote/full clients, unnecessary exposure for an in-process child. |
| `codex app-server daemon` + `proxy`; `codex remote-control` | The daemon owns a durable local app-server; `proxy` forwards stdio bytes to its Unix control socket. Remote control manages access to that daemon. | Same app-server threads and models. | Persistently managed and potentially shared with other clients. `daemon version` found no running daemon during this probe. | Same app-server controls. | Warm, but wrong isolation and ownership boundary for clj-surgeon. |
| `codex mcp-server` | Standard MCP JSON-RPC 2.0 over stdio. A live handshake negotiated MCP `2025-06-18` and advertised `tools.listChanged=true`. | `codex` starts a session and accepts `model`; `codex-reply` continues by `threadId` but exposes no per-reply model/output-schema override. | It uses Codex CLI configuration/auth and keeps the MCP process alive across turns. The official docs now mark it deprecated. | Tool result is only `{threadId, content}`. The exposed tool schemas have no structured-output schema or token/byte cap; a client timeout/process kill is the remaining bound. | Technically usable but strictly weaker and deprecated. |
| `codex exec-server` | Matching source describes exec-specific JSON-RPC over local WebSocket and Noise/protobuf relay frames remotely. Installed help lists WebSocket and stdio listen values, but direct local stdio invocation refused because `--remote` and `--environment-id` are required. | No Codex model session. It owns process IDs and filesystem/process RPCs. | ChatGPT, Agent Identity, or API-key auth applies only to remote environment registration. A connection owns spawned processes. | `process/read.maxBytes` bounds buffered subprocess output; concurrency is configurable. | Not an elaborator. It is an executor transport. |

`codex mcp` is a manager for external MCP registrations, not a server. There is
no installed `codex proto` subcommand: `codex help proto` exits 2 with
`unrecognized subcommand 'proto'`.

The exact installed app-server schema and matching tagged source agree on the
critical shape. Requests are `{method,id,params}`, successful responses are
`{id,result}`, and notifications omit `id`. This deliberately resembles
JSON-RPC 2.0 while omitting the version header on the wire. The official docs
also state that stdio is JSONL and that one connection is initialized once.

### Pinning is observable, not merely prompted

The installed catalog contains exactly `gpt-5.3-codex-spark`, lists low through
xhigh reasoning, and marks it unavailable through the ordinary API route. The
probe set `model` at `thread/start` and every `turn/start`, set
`allowProviderModelFallback: false`, required the thread-start response to echo
the exact model, and treated any `model/rerouted` event as disqualifying. The
response reported Spark and no reroute was emitted.

This is stronger than asking the model what it is. That distinction matters in
the Buster precedent: the local KiloClaw incident record documents a pinned
model timing out, silent fallback, and an agent incorrectly claiming it still
ran the requested model. Runtime metadata, not prose self-report, is the
authority.

### OpenClaw precedent

The local Buster operations runbook records `@openclaw/codex` as the app-server
harness that completed subscription-authenticated GPT-5.5 turns in production
on 2026-06-06. A separate incident record documents hundreds of app-server
session sidecars, persistent per-session model pins, token-expiry fallback, and
app-server connection-closure failures. This probe did not contact Buster; it
used those checked-in local records as precedent only:

- `/Users/genekim/src.local/kiloclaw/docs/buster-openclaw/README.md`
- `/Users/genekim/src.local/kiloclaw/docs/incident-2026-06-03-slack-gpt55-token-expiry-fallback-masquerade.md`

The precedent answers “can an app-server integration run continuously?” with
yes. It also supplies two production warnings: auth refresh and process
liveness must be first-class signals, and a configured model pin is not proof
of the model that actually answered.

## Phase 2: minimal live probe

### Frozen input and expected elaboration

The same 47-whitespace-token intent was submitted five times:

> Subject: `route-event` in `src/app/router.clj`, workspace `/repo`. From-anchor
> guard: `:done`. Expect guard: exactly one match within owner `route-event`.
> Decision: replace that form with `:complete`. Return only the complete
> `edit_clojure` arguments JSON object. Preserve the caller-authored path,
> owner, anchor, and cardinality exactly; invent no fields. Do not call tools.

Frozen expected value:

```json
{
  "workspace_root": "/repo",
  "edits": [{
    "file": "src/app/router.clj",
    "within": {"form": "route-event"},
    "from": ":done",
    "to": ":complete",
    "matches": 1
  }]
}
```

Intent SHA-256:
`086fb51532597b7885766a91faa2cea344ab405557e7499f2108b027707c5eab`.

Every response was the same 142-byte JSON value. Elaboration SHA-256:
`95128be522b1af0f7164079801c3334ab40772fc29447382768e0915207fc102`.

### Measurements

| Measure | Observed |
|---|---:|
| OS child spawn event | 4.196 ms |
| App-server `initialize` round trip | 87.748 ms |
| Process start through initialized response | 90.284 ms |
| Turn 1 | 9,063.961 ms |
| Turn 2 | 2,011.922 ms |
| Turn 3 | 1,424.817 ms |
| Turn 4 | 1,239.874 ms |
| Turn 5 | 2,268.510 ms |
| Median | 2,011.922 ms |
| Mean | 3,201.817 ms |
| Exact frozen match | 5/5 |
| JSON parse | 5/5 |
| Tool-free item stream | 5/5 |
| Model reroutes | 0 |
| Clean shutdown | stdin EOF, exit 0 |

The sample is deliberately small. The receipt includes an interpolated p95 for
mechanical completeness, but `n=5` is not enough to treat it as a stable tail
estimate. The useful result is the shape: sub-100-ms local startup, roughly
1.2–2.3-second subsequent turns, and a 9-second first-turn backend tail.

The Spark-specific rate-limit snapshot moved from **39% to 42%** in its
300-minute window and from **30% to 31%** in its 10,080-minute window during the
five calls. Those percentages are coarse and may include concurrent account
activity. `account/usage/read` returned `dailyUsageBuckets: null` and
`threadUsage: null`, so the installed service did not supply an exact
per-thread subscription charge for this run.

### Raw receipts

- [`probe-receipt.json`](2026-08-30-embedded-spark-elaborator-probe/probe-receipt.json)
  contains model/auth assertions, isolation controls, timing, output,
  intent/elaboration hashes, exactness verification, rate-limit snapshots, and
  shutdown status.
- [`app-server-transcript.jsonl`](2026-08-30-embedded-spark-elaborator-probe/app-server-transcript.jsonl)
  is the redacted bidirectional protocol transcript. Secret-bearing fields are
  replaced; no auth token is retained.
- [`installed-cli-recon.json`](2026-08-30-embedded-spark-elaborator-probe/installed-cli-recon.json)
  records installed binary/package hashes, commands, modes, schema hash, model
  catalog facts, and the local OpenClaw evidence paths.
- [`probe-app-server.mjs`](2026-08-30-embedded-spark-elaborator-probe/probe-app-server.mjs)
  is the reproducible harness. It creates and destroys its own temporary auth
  sandbox and workspace.

## Phase 3: trust-boundary design

### Fixed invariant

The feature is not “let Spark edit Clojure.” It is a compiler-like boundary:

```text
caller-authored subject + guards + decision
                    |
                    v
        one bounded Spark elaboration
                    |
                    v
     untrusted candidate arguments data
                    |
                    v
 server reconstructs/fences guards and validates schema
                    |
          reject or ordinary edit path
                    |
                    v
    existing edit_clojure enforcement + verification
```

The caller owns every authority-bearing field: workspace, file, owner,
from-anchor, exact match count, and decision. The elaborator fills the body of
one candidate. Its output has the same authority as arbitrary caller input:
none until the clj-surgeon server validates it.

The server must therefore:

1. Canonicalize and hash the caller intent before model contact.
2. Build an output schema from the operation shape, not from model output.
3. Accept exactly one final candidate and no tool/side-effect item.
4. Parse the candidate as data; reject unknown keys and non-canonical aliases.
5. Replace or compare every guard-bearing candidate field with the frozen
   caller-owned value. The elaborator may not weaken, omit, widen, or invent a
   guard.
6. Run the ordinary `edit_clojure` schema/compiler/refusal path unchanged.
7. Emit a receipt containing CLI/model/auth identity, intent hash,
   elaboration hash, validation result, and ordinary verification receipt.
8. On timeout, crash, auth failure, model absence/reroute, schema violation,
   tool item, or guard mismatch, produce no candidate and degrade to the
   existing ordinary caller-authored path.

There are no retries, reflection turns, repair prompts, tool calls, or agent
loops. One intent produces at most one candidate.

### Quota attribution and 24-hour metering

Quota belongs to the ChatGPT account represented by the child process's
`auth.json`. If the clj-surgeon server runs one child under the operator's Codex
home, every MCP caller spends **that operator account's** Spark allowance. The
MCP caller's identity does not automatically transfer through app-server. This
must be explicit in deployment policy and receipts; a multi-user shared server
must not silently pool one person's subscription.

The installed protocol offers three imperfect signals:

- `thread/tokenUsage/updated` supplies per-turn token counts;
- `account/rateLimits/read` supplies model-specific percentage windows and
  reset times;
- `account/usage/read` can supply daily buckets and an optional per-thread
  estimate, but both useful fields were `null` in this probe.

Therefore, meter the first 24 hours with a local append-only ledger keyed by a
non-secret auth-identity hash and containing timestamp, thread/turn id, model,
input/output token counts, latency, result class, intent hash, elaboration hash,
and pre/post rate-limit snapshots. Compute a rolling 24-hour call/token count
locally. Treat account percentage deltas as reconciliation evidence, not exact
per-call billing. Set a hard local call budget and circuit-open before the
subscription window is exhausted. Do not promise exact monetary or quota-unit
attribution until `account/usage/read.threadUsage` is non-null and validated
against the service window.

### Auth-material placement

Production must never accept auth bytes over MCP, store them in the repository,
log them, or put them in an API-key environment variable. Run the child as one
dedicated local service identity with a mode-0700 Codex home and mode-0600
`auth.json`. Scrub `OPENAI_API_KEY`, `CODEX_API_KEY`, and
`CODEX_ACCESS_TOKEN`. At startup, require both `codex login status` and
`account/read` to report ChatGPT managed auth.

The probe copied auth into a disposable private home so it could also supply an
empty config and avoid shared MCPs. That is appropriate for a one-off probe,
not automatically the best refresh strategy. A production child needs a
refresh-capable owner-controlled secret location or a narrowly mounted
per-service Codex home; otherwise atomic token refresh can update a disposable
copy while the authoritative credential goes stale. One process must map to one
auth identity, and the receipt must identify that mapping without exposing
email or token material.

### Lifecycle and restart policy

Use a directly supervised stdio child, not `app-server daemon`:

- Start lazily on the first eligible elaboration, initialize once, and verify
  account type plus Spark catalog presence.
- Keep the process warm, but start a **fresh ephemeral thread per intent** in
  production. The live probe reused one ephemeral thread to measure sequential
  warmth; production should avoid cross-intent context contamination.
- Pin Spark on thread start and turn start, disallow provider fallback, and
  reject any reroute notification.
- Apply a short turn deadline, a small accumulated-output byte cap, and a
  one-in-flight semaphore initially.
- On first process loss or protocol desynchronization, fail the request closed,
  discard the partial candidate, and open the ordinary path. Restart the child
  only for the next request with bounded exponential backoff. Never replay the
  failed intent automatically.
- Close stdin and wait for exit on MCP-server shutdown; escalate once to
  `SIGTERM`, then `SIGKILL` only after a bounded grace period.
- Pin the Codex CLI version and generated schema hash. Upgrade behind the same
  exactness/adversarial probe, not live in place.

### Model-volatility degrade

Model availability is a capability check, not a suggestion. Require Spark in
`model/list`, set `allowProviderModelFallback: false`, compare the
`thread/start` response model to the requested slug, and reject
`model/rerouted`. If Spark disappears, the subscription is ineligible, the CLI
schema changes, or structured output stops working, the elaborator is simply
unavailable. The system returns to the ordinary path; it does not silently
choose Terra, Sol, an API model, or another account.

### Three sharp failure modes and their falsifiers

| Failure mode | Why it is sharp | Falsifier required before admission |
|---|---|---|
| **Authority inversion:** a candidate changes `from`, `matches`, owner, file, workspace, or adds a second edit while remaining valid JSON. | This turns prose generation into write authority and can widen a surgical edit. | Generate adversarial and property-based intents with conflicting/injected guard text. Freeze the caller guard tuple independently. Every candidate difference, omission, alias, extra key, or extra edit must be rejected before `edit_clojure`; source bytes and verification state must remain unchanged. |
| **Hidden tool execution or context leakage:** Spark calls shell/MCP/file tools, reads service-user state, or loops after a failure. | The public schema does not hard-disable built-ins; prompt obedience is not a boundary. | Run a hostile corpus asking for shell reads, MCP calls, and iterative repair inside an OS sandbox with syscall/process audit. Admission requires zero completed tool actions, interruption on the first tool item, one turn only, empty product workspace visibility, and no candidate accepted from such a turn. Any observed tool side effect rejects the architecture. |
| **Failure amplification:** auth expiry, rate limit, child crash, malformed JSON, or backend tail causes automatic replays and a quota storm or duplicate candidate. | A “warm helper” can become a hidden retrying agent and consume the operator's subscription while delaying the ordinary path. | Kill the child mid-turn and inject 401/429, timeout, malformed output, and EOF cases. Each intent must cause at most one model turn, zero replay, zero partial candidate, one bounded failure receipt, and immediate ordinary-path availability. The next request may attempt one supervised restart only after backoff. |

## Recommendation and next gate

Keep the option alive and prototype the boundary as an **experimental,
default-off adapter** around a supervised stdio app-server child. The installed
CLI is not the blocker. The next gate is a no-product-write adversarial harness
that proves guard immutability, one-turn accounting, model/reroute enforcement,
and zero side effects under attempted tool use, crash, 401, 429, timeout, and
malformed output.

Do not embed the daemon, adopt the deprecated MCP façade, or add an API-key
fallback. If that adversarial gate cannot make “no tools” mechanical, retain
the current warm-seat/tmux path as the nearest safe fallback while keeping this
probe as evidence that app-server protocol and subscription-authenticated Spark
pinning themselves are viable.
