# Captain's Log: Tool Names Enter the Real Arena

**Date:** 2026-08-28  
**Owner:** `clj-surgeon-x9d`  
**Candidate head:** `2f47ddc4cad9c8090b27105baea6d1c14c8ef009`  
**Status:** live, counterbalanced U/V/W/X full-edit tournament in progress

## Why this is a new experiment

The first catalog screen produced an implausibly strong legacy-name win. Its
adversarial audit proved that the public facade was not an authority boundary,
the exact client-visible catalog was absent, verifier provenance was invalid,
and multiple variables changed between arms. That verdict was withdrawn.

This cohort changes only the public extraction tool name:

| Catalog | Extraction name |
|---|---|
| U | `apply_clojure_changes` |
| V | `apply_clojure_extraction` |
| W | `extract_clojure` |
| X | `move_clojure_forms` |

Every other tool name, description, schema, annotation, output schema, order,
handler, prompt, fixture, model, reasoning effort, and verifier stays fixed.
Each fresh Codex home captures its actual app-server MCP registry before model
execution. The first model action remains the behavioral authority.

## The cheap screen evolved

A passive multiple-choice prompt was too cheap. With tool calls disabled, Sol
invented plausible names instead of using the real MCP router. It tested recall,
not tool selection.

The replacement is a one-call, zero-mutation canary. It supplies a complete
extraction against a deliberately absent source. A valid run must select the
extraction control first, make one MCP call, use no shell or file tools, and
stop after the pre-write refusal.

All four names passed that routing canary locally and on Anvil. Their one-sample
wall rankings reversed across hosts, so those times are classification evidence,
not a vocabulary verdict.

## Harness falsifiers earned before the tournament

### The configured allowlist was stale

The full-task harness launched a five-tool candidate server, then its old config
writer allowlisted only the four canonical production names. The independent
Codex registry preflight caught the missing `continue_clojure_plan` before the
model ran. Commit `a95b615` makes the allowlist derive from the exact runtime
role receipt while preserving the canonical four-tool default.

### Pure Clojure tests did not model the SDK boundary

The candidate admission membrane accepted Clojure persistent maps. The MCP SDK
actually passed `java.util.LinkedHashMap` and `ArrayList`. Every real request
therefore failed closed even though the pure tests were green. One retained U
trace shows the cost of this bug: five safe refusals, 16 shell commands, 291.450s,
and an unverified manual extraction.

Commit `2f47ddc` admits real SDK JSON containers, enforces map cardinality and
typed additional properties, and permanently proves that the exact
omission-based extraction request invokes the canonical handler once. Warm and
cold gates pass 24 tests and 283 assertions.

### Byte identity was an obsolete primary gate

The first corrected Anvil U run was semantically correct, atomically committed,
and exact-verifier clean in one call, but the temporary tournament steward
rejected it because `exact_correct=false`. That contradicted the benchmark law:
formatting-only presentation differences are secondary. The steward now gates
on meaning, parseability, transaction evidence, and exact verifier success.
Byte identity remains telemetry.

## Absolute timing anchors

| Route | Complete wall |
|---|---:|
| Best replicated fused route with terminal relay | 21.815s midpoint |
| Earlier fused one-call route with normal response | 27.471s midpoint |
| Earlier direct extraction | 37.871s |
| Public plan then apply | 49.941s |
| Correct native control | 122.278s |

The two valid corrected U observations before the complete mirrored cohort were
31.186s and 29.026s, midpoint 30.106s. That is 8.291s slower than the best
replicated Surgeon route and 92.172s faster than the native control.

## Live full-edit standings

Every cell uses Sol/high and the same 15-form Sessionize extraction. A valid
cell requires one successful extraction call, one atomic transaction, fused
exact verification, semantic correctness, no source discovery, and the expected
public extraction name as the first selected tool.

| Order | Catalog | First selected tool | Wall | Outcome |
|---:|---|---|---:|---|
| 1 | U | `apply_clojure_changes` | 29.026s | pass: one call, no reads or shell |
| 2 | V | `edit_clojure` | 52.484s | DNF: wrong first control; extraction recovered on call two |
| 3 | W | `extract_clojure` | 24.681s | pass: one call, no reads or shell |
| 4 | X | `move_clojure_forms` | 28.216s | pass: one call, no reads or shell |
| 5 | X | `move_clojure_forms` | 35.119s | pass: one call, no reads or shell |
| 6 | W | `extract_clojure` | 31.970s | pass: one call, no reads or shell |
| 7 | V | `apply_clojure_extraction` | 27.652s | pass: one call, no reads or shell |

The remaining mirrored order is U. A DNF is retained and the
tournament continues; one losing arm no longer stops collection of independent
options.

## Current interpretation

W currently leads the valid full-edit arms with a two-run midpoint of 28.326s
(24.681s, 31.970s). X follows at 31.668s (28.216s, 35.119s), an absolute gap of
3.342s. U has one valid 29.026s observation pending its mirror. W's midpoint is
6.511s slower than the best replicated Surgeon route and 93.952s faster than
native. V is now split: its mirror finished in one clean call at 27.652s, but
its first caller selected the compact editor first, received a safe refusal,
then recovered through `apply_clojure_extraction`. Its 1/2 one-shot rate keeps
it below every 2/2 arm regardless of the fast successful observation. These are real
observations, but no release verdict is valid until the mirrored calls complete.

The larger architectural win is already durable. A catalog variant is now an
edge projection over one semantic kernel, and its public schema is executable
authority. This makes vocabulary cheap to change, safe to falsify, and honest
to measure even if the production name ultimately remains unchanged.
