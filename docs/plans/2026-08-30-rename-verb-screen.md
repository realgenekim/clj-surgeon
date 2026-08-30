# Byte-efficient rename verb screen

**Status:** preregistered before the first evaluated model call  
**Date:** 2026-08-30  
**Product source:** `c55de2279826af5ed21c90981591479dd2e802b2`
(`origin/release/closed-relations-published`)  
**Branch:** `experiment/rename-verb-screen-20260830`  
**Harness:** `bench/run_rename_verb_screen.sh`

## Question and evidence class

When a clean subscription-backed Codex caller is offered the closed request

```json
{"op":"rename-symbol","from":"jitter-ms","to":"retry-jitter-ms"}
```

instead of today's complete `edit_clojure` edit set, does `gpt-5.6-sol` use the
verb, complete the exact rename, and materially reduce emitted request bytes?

This is a synthetic caller/interface screen. It can decide whether the compact
verb deserves another experiment; it cannot establish general rename safety or
production performance.

## Frozen fixture and common task

The synthetic fixture has two Clojure files. `jitter-ms` is defined once and
referenced once from seven other named top-level forms: four unqualified sites
in `src/bench/retry.clj` and three qualified sites in
`src/bench/worker.clj`. The frozen after tree renames the definition to
`retry-jitter-ms` and updates all seven references. Every site has an exact
owner, old form, new form, and match count of one.

Both arms receive byte-identical task prompts containing the complete file,
owner, old-form, new-form, and count decision. The prompt requires exactly one
call to the single available `edit_clojure` tool, prohibits source inspection,
shell commands, and file-change actions, and requires the terminal reply
`RENAME_OK`.

Calls are serial. Every run gets a fresh Codex home, fixture workspace, and
isolated 512 MB local MCP process on an ephemeral port. MCP bootstrap completes
before model wall starts and is recorded separately. Runs use ChatGPT
subscription auth, `--ephemeral`, `--ignore-rules`, reasoning effort `high`, a
180-second timeout, and no metered API key.

## Arms and proxy boundary

The only manipulated factor is the model-visible `edit_clojure` input grammar.

- **V — verb:** a closed schema with exactly required `op`, `from`, and `to`
  strings; `op` is the single enum value `rename-symbol`. An experiment-only
  handler validates the exact frozen tuple, expands it to the eight ordinary
  owner-scoped edits, and delegates unchanged to the published
  `edit_clojure` handler.
- **T — today's surface:** the published `edit_clojure` schema and handler. The
  caller must emit the complete eight-edit request supplied by the prompt.

The V adapter adds no discovery, model-code evaluation, product source change,
write authority, or alternate executor. Both arms finish through the same
published compact-location normalizer, transaction compiler, formatter,
atomic writer, read-back, and receipt path. The adapter and all instrumentation
live under `dev/experiments` or `bench`.

## Frozen schedule and sample

The Sol cohort is twelve fresh calls, six per arm, in this counterbalanced
order:

```text
V T  T V  T V  V T  V T  T V
```

Each arm appears first in three adjacent pairs. All attempts are retained.
There are no correctness-based rerolls, prompt repairs, or schema repairs after
the first evaluated call.

If the Sol cohort and receipts complete within the available subscription and
wall budget, run two additional V cells with the first reachable canonical
`gpt-5.3-codex-spark` identifier. Availability probes and failed aliases are
retained and are not evaluated rename trials.

## Registered outcomes

The primary outcome is total emitted mutation-request size per completed
rename, summed over every MCP call in that run:

1. exact UTF-8 bytes of each compact JSON `item.arguments` value preserved in
   Codex event order; and
2. the same request serialized bytes tokenized with pinned `tiktoken`
   `o200k_base`, reported explicitly as a tokenizer estimate rather than a
   model-billing token count.

The point prediction is a **90% median reduction** in both request bytes and
request-token estimate for V relative to T. The exact final
`turn.completed.usage.output_tokens` is retained separately as whole-turn
emission evidence; it is not mislabeled as request-only tokens.

Secondary outcomes are:

- exact final Clojure source manifest against the frozen after tree;
- all eight expected rename sites updated, with no old symbol remaining;
- one-shot rate: exact result, one successful MCP call, no failed MCP call,
  command, or file-change action;
- completed turn count and complete model wall;
- wrong-subject count, defined as any unexpected Clojure path or any mismatch
  against the frozen after manifest;
- V adoption: the first request is exactly the three-field verb tuple;
- V schema-fumble rate: any invalid tuple, failed MCP call, or extra mutation
  call before completion.

Exact correctness must be equal between arms and wrong-subject must be zero.
Incomplete, timed-out, refused, or inexact attempts stay in the denominator.

## Decision rule

Kill the verb if any of these occurs:

- the correctness rate is lower in V than T;
- any run changes the wrong subject; or
- the median exact request-byte reduction among completed renames is below
  50%.

Otherwise record the observed byte and token-estimate magnitude, adoption,
one-shot behavior, and schema-fumble rate. Passing this screen only earns a
next experiment; it does not authorize product code.

## Falsifiers and retained receipts

Before model calls, zero-model tests must reject unknown or missing verb
fields, prove the exact eight-edit lowering, prove V's schema is closed, prove
T uses the published schema, and mutate a copied fixture through the common
handler to the frozen after bytes.

Retain the product, experiment, and tree SHAs; Git status; Codex version and
model catalog; accepted model identifiers; plan, harness, proxy, scorer,
prompt, and fixture hashes; every raw Codex JSONL and stderr stream; exact
request JSON; MCP surface, stdout, stderr, readiness, bootstrap timing, and
telemetry; before, expected, and final source manifests; exit code; wall;
structured per-run and aggregate scores; tokenizer name/version; a SHA-256
manifest; a compressed raw archive; and one exact replay command.

