# Preregistration: multi-site `edit_clojure` versus native `apply_patch`

Frozen on 2026-08-30 before any measured model call. The preflight writes the exact UTC timestamp, input hashes, model-catalog hashes, fixture commit, and environment receipt. The preregistration commit is the immutable registration boundary.

## Question

On one byte-identical, single-file Clojure task, does one guarded `edit_clojure` call reduce model-emitted mutation payload tokens and complete-task wall time relative to one native multi-hunk `apply_patch` call?

This study tests one task class. It does not estimate performance across all Clojure edits.

## Fixture and requested effect

The synthetic repository contains a 137-line `src/acme/retry_policy.clj`. The task renames private Var `retry-delay-ms` to `backoff-delay-ms`, including exactly five references in five distinct `defn` owners. It also replaces one docstring in `policy-summary`.

The before file contains exactly six `retry-delay-ms` forms. The expected file contains exactly six `backoff-delay-ms` forms. The complete expected file is the byte oracle. Every episode starts at the same deterministic fixture Git commit and source SHA-256.

The complete source appears in the task prompt. No discovery tool is available or needed.

## Arms and isolation

The executor is Codex CLI 0.149.1 with exactly `gpt-5.6-sol`, reasoning effort `high`, and ChatGPT subscription authentication. The child environment removes `OPENAI_API_KEY` and all other `OPENAI_*` variables. It does not use a metered API route.

Each arm disables shell, web, image, app, plugin, planning, memory, and multi-agent tools. Both arms use the same system prompt, task prompt, fixture, model object, reasoning effort, approval mode, capture hook, and process timeout.

- Arm N exposes native `apply_patch` as the only mutation tool. It configures no MCP server.
- Arm S exposes `edit_clojure` as the only mutation tool. Its frozen model-catalog object differs from Arm N only at `apply_patch_tool_type`, which is `null` in Arm S. Codex connects to a fresh HTTP MCP server launched from this worktree. The server source, dependencies, and resources are byte-identical to `c55de2279826af5ed21c90981591479dd2e802b2`. The client enables only `edit_clojure`.

The S server starts and passes `/healthz` before the episode clock starts. JVM bootstrap time is not part of complete-task wall time. This matches an already-running interactive MCP service. Each episode gets a new server and workspace. No process, configuration, install, or reload is shared with the live clj-surgeon runtime.

Codex adds generic MCP resource helpers when an MCP server exists. These are non-mutation tools. Any use of them is retained and counted, but it does not satisfy route adherence.

## Compact Surgeon decision under test

The exact valid Surgeon request has two edits in one call:

1. Replace `retry-delay-ms` with `backoff-delay-ms` at root scope with `matches=6`.
2. Replace the exact old docstring inside `policy-summary` with `matches=1`.

The prompt does not include this request object. The model must construct it from the task and supplied source. Preflight submits the frozen oracle request directly to the isolated server and requires byte-exact output before any model call.

## Schedule and sample

Run exactly 12 fresh sessions in this fixed order:

`N S S N / S N N S / N S S N`

This gives `n=6` per arm. No episode is replaced. A timeout, refusal, invalid environment, wrong route, retry, or incorrect result remains in its scheduled cell.

## Primary measurements

The synchronous `PreToolUse` capture hook retains every local tool input before execution.

- For N, the mutation payload is the exact UTF-8 `tool_input.command` string supplied to `apply_patch`.
- For S, the mutation payload is the UTF-8 compact JSON serialization of the decoded `tool_input` object, preserving delivered key order and using no ASCII escaping.
- `payload_bytes` is the length of that UTF-8 payload.
- `payload_tokens` is the token count from `tiktoken` `o200k_base` version 0.9.0.

If a run emits more than one mutation call, sum bytes and tokens through the successful mutation. Retain per-call values. Do not count the final prose response in the primary payload metric. Provider-reported total output tokens are secondary.

`wall_seconds` starts immediately before `codex exec` launch and ends when that process exits. For S, the server is already healthy before the start. `turns_to_success` is the number of mutation call-and-result cycles through the successful mutation. `retries` is `turns_to_success - 1`. A run with no successful mutation has no `turns_to_success` value.

## Frozen validity fields

- `environment_valid`: the process starts and exits zero. The child has no `OPENAI_*` variable. ChatGPT login is active. The hook reports model `gpt-5.6-sol`. The fixture hashes match. The arm catalog has the registered `apply_patch_tool_type`. Arm S uses a healthy server whose source matches the registered base. The process does not time out.
- `semantic_correct`: the final target file is byte-identical to the expected file. No test or formatter can substitute for this oracle.
- `route_adherent`: N calls `apply_patch` and no other mutation tool. S calls `edit_clojure` and no other mutation tool. Only the target file changes relative to the fixture commit.

The confirmatory per-arm median includes episodes for which all three fields are true. The report must also show headline counts over all six scheduled episodes. No invalid episode is silently discarded or replaced.

## Kill criterion

Kill the confirmatory speed verdict if preflight fails, if the preregistered files drift after the preregistration commit, or if either arm has fewer than six episodes with all three validity fields true. Also kill if an episode exposes evidence of a second executable mutation family, a model fallback, API-key routing, the wrong server commit, or shared-runtime access. Retain and report all completed evidence even after a kill.

## Predictions registered before model execution

The directional prediction is `S < N` for both primary medians.

- Payload: S median will be at least 45% lower than N median. Point prediction: 55% lower, approximately 180 fewer `o200k_base` tokens.
- Wall time: S median will be at least 12% lower and at least 2.5 seconds faster. Point prediction: 20% lower and approximately 4 seconds faster.
- Calls: at least 5/6 valid S episodes and at least 5/6 valid N episodes will succeed in one mutation call.

The wall prediction uses two previously measured screens as magnitude checks, not as additive estimates: 3.5237 ms per emitted payload byte and 56.5 decoded tokens per second. The 1,284x input/output asymmetry predicts that the larger common prompt is not the primary latency driver.

## Analysis and decision rule

For each arm, report scheduled count, valid/correct/adherent count, median payload bytes, median payload tokens, median wall seconds, median turns to success, and total retries. Report `S-N` absolute deltas and `(S-N)/N` percent deltas for payload tokens and seconds.

Call the registered claim supported only if all six runs per arm are valid, correct, and adherent; both median directions favor S; and both minimum magnitude gates clear. If both directions favor S but either magnitude gate misses, call the claim directionally supported but smaller than predicted. If either primary median does not favor S, call the claim unsupported on this fixture.

Report every per-episode value. Use an exact label-permutation interval or test only as a secondary description. With `n=6` per arm, do not report a null result as equivalence. Report the 80%-power minimum detectable standardized effect under the stated equal-variance normal approximation. Discuss whether the result can survive changes in reference count, owner spacing, identifier length, docstring size, retry behavior, and server warmth.

## Receipts and replay

Commit the preregistration, harness, frozen prompts, fixtures, oracle request, generated catalogs, raw hook events, Codex JSONL streams, MCP telemetry, server logs, final files, scores, aggregate, environment receipt, SHA-256 manifest, and replay commands to branch `experiment/multisite-headtohead-20260830`.

Use author `sol <sol@skiff>` and trailer `Co-Authored-By: Gene Kim <genek@itrevolution.com>`. Push without force. Never read or modify `~/src.local/clj-surgeon`. Never install or reload shared tooling.
