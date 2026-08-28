# Cross-Caller MCP Extraction Benchmark

## Objective

Preserve the established Codex Sol/high fast path while proving that Claude
Fable/high and Claude Opus/high can use the same one-shot
`apply_clojure_changes` transaction and complete the frozen extraction faster
than their own matched native controls.

This is an adapter and routing experiment. It does not authorize a change to
the public Surgeon transaction shape, Sol prompt, kernel, or verification
contract merely to accommodate Claude.

## Frozen Task

Use `sessionize-format-extraction` and its existing semantic scorer. The task
moves 15 named forms from the historical 4,594-line namespace into one new
namespace. The MCP arm uses the existing fused exact-verification transaction.
The native arm uses the existing native champion instructions.

## Caller Strata

Do not pool callers or models.

| Caller | Model request | Surgeon route | Native control |
|---|---|---|---|
| Codex | `gpt-5.6-sol`, high | Existing first-action fused context | Existing native champion context |
| Claude | `fable`, high | Optional `ToolSearch`, then one fused apply | Same task and native champion law |
| Claude | `opus`, high | Optional `ToolSearch`, then one fused apply | Same task and native champion law |

Claude may require one deferred-tool discovery action. Count its complete wall
and action cost. Do not force Sol through that discovery route.

## Pilot Design

1. Run a local Fable MCP canary against a fresh isolated 512 MiB MCP.
2. Run a local Opus MCP canary only after Fable proves configuration and route.
3. Ship the exact committed benchmark source to Anvil.
4. Run six serial pilot arms: one Surgeon and one native arm for each caller.
5. If all six arms are semantically correct, run a second batch in reverse
   order. Do not expand an incorrect or non-adherent arm.

Each Surgeon arm gets a fresh isolated MCP server and workspace. No arm uses or
restarts the shared `:7888` runtime. Run serially so model and analyzer load do
not confound complete wall time.

## Correctness and Route Gates

All arms must pass the existing semantic scorer. Exact bytes remain secondary.

The Codex Surgeon arm retains its existing route gate unchanged.

The Claude Surgeon arm must satisfy all of these conditions:

- record the resolved canonical model;
- use zero source readers, native editors, shell commands, or skill/CLI calls;
- use at most one `ToolSearch` that selects
  `mcp__clj-surgeon__apply_clojure_changes`;
- call `mcp__clj-surgeon__apply_clojure_changes` exactly once;
- make no other Surgeon call and have no failed tool call;
- receive `verification_complete=true`, exact-exit zero, and terminal receipt
  evidence;
- return exactly `Done — changes committed and exact verification completed.`

## Measurements

Record complete process wall, streamed action arrival clocks, resolved model,
tool count and order, MCP service wall and phase telemetry, semantic score,
verification outcome, final response, token use, and Claude cost telemetry.

Compute each speedup against the same caller and model's native arm. The
retained Codex native median of 122.278 seconds is context, not a matched Claude
baseline.

## Decision Law

- Keep any Claude-only harness or routing adapter that earns correct one-shot
  execution and a matched speed advantage.
- Do not change the Sol route unless a separate Sol cohort proves no regression.
- Do not weaken transaction, snapshot, rollback, or exact-verification safety.
- If direct routing works but natural routing does not, preserve the capability
  result and open a separate Claude discoverability hill.

## Outcome

The pilot passed without changing the Sol route. Matched Anvil speedups were
9.69x for Sol/high, 8.97x for Fable/high, and 6.37x for Opus/high. A later
Terra/high replication passed at 7.56x. Every promoted Surgeon route was one
compiled apply; Claude paid one visible deferred-tool discovery action.

The additive Spark/high screen produced a correct one-apply Surgeon result in
11.966 seconds, but its native control was semantically destructive, so no
matched Spark speedup is claimed. Natural-prompt adoption remains a separate
ethnographic question; it must not weaken the proven direct route.
