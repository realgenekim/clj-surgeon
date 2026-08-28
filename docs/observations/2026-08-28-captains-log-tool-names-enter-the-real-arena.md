# Captain's Log: Tool Names Enter the Real Arena

**Date:** 2026-08-28  
**Owner:** `clj-surgeon-x9d`  
**Candidate head:** `2f47ddc4cad9c8090b27105baea6d1c14c8ef009`  
**Status:** complete; retain `apply_clojure_changes` for the extraction route

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
| 8 | U | `apply_clojure_changes` | 28.191s | pass: one call, no reads or shell |

All mirrored cells have now run. A DNF is retained rather than converted into
a slow success; one losing arm did not stop collection of independent options.

## Current interpretation

W currently leads the valid full-edit arms with a two-run midpoint of 28.326s
(24.681s, 31.970s). X follows at 31.668s (28.216s, 35.119s), an absolute gap of
3.342s. U is the most stable arm: its midpoint is 28.609s (29.026s, 28.191s),
only 0.283s behind W, with a 0.835s range rather than W's 7.289s range. W's
midpoint is 6.511s slower than the best replicated Surgeon route and 93.952s
faster than native. V is split: its mirror finished in one clean call at 27.652s, but
its first caller selected the compact editor first, received a safe refusal,
then recovered through `apply_clojure_extraction`. Its 1/2 one-shot rate keeps
it below every 2/2 arm regardless of the fast successful observation. Raw
artifact recovery and phase-timing review remain before a release verdict.

## Completed N=2 ranking

Reliability is the first sort key; wall time ranks only valid one-shot cells.

| Rank | Catalog | One-shot | Valid walls | Midpoint | Gap to 21.815s best | Time saved vs 122.278s native |
|---:|---|---:|---|---:|---:|---:|
| 1 | W · `extract_clojure` | 2/2 | 24.681s, 31.970s | 28.326s | +6.511s | 93.952s |
| 2 | U · `apply_clojure_changes` | 2/2 | 29.026s, 28.191s | 28.609s | +6.794s | 93.670s |
| 3 | X · `move_clojure_forms` | 2/2 | 28.216s, 35.119s | 31.668s | +9.853s | 90.611s |
| DNF | V · `apply_clojure_extraction` | 1/2 | 27.652s; one 52.484s recovery | — | — | — |

Howard Cosell can call W the leader after two rounds. The commission cannot
yet call it the champion. W leads U by only 0.283s while its observed range is
7.289s. U's range is 0.835s. The measured separation is ordinary run noise
until a larger counterbalanced U/W cohort reproduces it.

The phase clocks strengthen that caution:

| Catalog | Initial materialization midpoint | Server midpoint | Receipt midpoint |
|---|---:|---:|---:|
| W | 22.943s | 1.962s | 2.550s |
| U | 22.479s | 2.316s | 2.955s |

W did not make the first call appear faster. Its initial model interval was
0.464s slower than U. Its small total lead came from 0.355s less server time
and 0.405s less receipt time, neither of which establishes a vocabulary-caused
selection advantage. The next experiment therefore compares only U and W with
more counterbalanced replicas; it does not promote W from this screen.

## U/W confirmation, live

The confirmation uses the serial counterbalanced order U, W, W, U, W, U, U,
W on the same exact candidate, model, fixture, scorer, and Anvil seat.

| Order | Catalog | Wall | Outcome |
|---:|---|---:|---|
| 1 | U | 31.637s | pass: one call, no reads or shell |
| 2 | W | 35.678s | pass: one call, no reads or shell |
| 3 | W | 29.926s | pass: one call, no reads or shell |
| 4 | U | 29.106s | pass: one call, no reads or shell |
| 5 | W | 35.607s | pass: one call, no reads or shell |
| 6 | U | 27.460s | pass: one call, no reads or shell |
| 7 | U | 30.190s | pass: one call, no reads or shell |
| 8 | W | 28.977s | pass: one call, no reads or shell |

The complete confirmation gives U a 29.648s median and W a 32.767s median; U
leads by 3.119s. Across pilot and confirmation, each arm now has six valid
one-shot observations. U's combined median is 29.066s and W's is 30.948s, so U
leads by 1.882s overall. W's 0.283s pilot lead did not reproduce. The clearer
extraction noun did not beat the established control on this task.

The confirmation clocks localize the loss:

| Catalog | Initial materialization median | MCP observer median | Server median | Receipt median | Reasoning-token median |
|---|---:|---:|---:|---:|---:|
| U | 23.660s | 2.503s | 2.462s | 2.738s | 345 |
| W | 26.317s | 2.097s | 2.059s | 3.283s | 439 |

W saved 0.406s at the MCP boundary, where the public projection should have
almost no causal performance effect. It lost 2.658s before the first tool call
and 0.544s after the receipt. The model also emitted 94 more reasoning tokens
at the median. The hoped-for semantic shortcut did not occur; the novel name
made Sol deliberate longer in this frozen catalog.

## Retained evidence and one harness defect

The complete result archives were copied back from Anvil and their SHA-256
hashes matched the remote artifacts:

- orders 1–2: `/tmp/clj-surgeon-catalog-results-2f47ddc-20260828T171739Z.tar.gz`,
  `9d058818214c2c35b501e400b3c5f33335d6b76f9add2a32233f3c2b692eca74`;
- orders 3–8: `/tmp/clj-surgeon-catalog-results-2f47ddc-20260828T172110Z.tar.gz`,
  `32626a113936f949b7749a0acf843c9875dc0549a9285788d1a1a15a3cb009f4`.
- U/W confirmation: `/tmp/clj-surgeon-catalog-confirm-results-2f47ddc-20260828T173212Z.tar.gz`,
  `e9bf58e36bdbf6a5950a2c5a88f916f8f8cf32d6a590506f114e04c5253b11e3`.

The second remote wrapper exited after producing every result because the
catalog test JVM created an untracked `.cpcache/` and the postflight asserted a
completely empty Git status. Source remained unchanged. This is a harness
cleanliness defect, not a failed model cell; future runs must isolate the
Clojure cache or explicitly exclude that generated directory while preserving
the source-dirt gate.

The larger architectural win is already durable. A catalog variant is now an
edge projection over one semantic kernel, and its public schema is executable
authority. This makes vocabulary cheap to change, safe to falsify, and honest
to measure even if the production name ultimately remains unchanged.

## Final verdict

Keep catalog U's `apply_clojure_changes` name for the extraction route. It was
6/6 semantically correct and one-shot, and its combined median was 1.882s
faster than the strongest challenger. `extract_clojure` was also 6/6 correct,
but the confirmation localized its loss before the first call rather than in
the structural kernel. `apply_clojure_extraction` failed one of two first-tool
routes. `move_clojure_forms` was correct but slower.

This is not evidence that familiar names are universally superior. It is
evidence that none of these three name-only extraction variants improved this
frozen Sol/high task. Do not rename the production tool from this portfolio.
Future catalog experiments should test a materially different hypothesis or a
different decision stratum, not rerun synonyms until noise yields a preferred
answer.

## Apex-predator rematch

Gene requested one fresh verification that the retained control remains on
top. A serial U, W, W, U rematch uses the same exact candidate, Sol/high,
fixture, scorer, and Anvil seat.

| Order | Catalog | Wall | Outcome |
|---:|---|---:|---|
| 1 | U | 34.392s | pass: one call, no reads or shell |
| 2 | W | 43.607s | pass: one call, no reads or shell |
| 3 | W | 31.844s | pass: one call, no reads or shell |

This first U observation is slower than its earlier distribution. The paired
W observations determine whether this is catalog behavior or current service
conditions; it is not compared to the older median in isolation. In the first
paired position, U leads W by 9.215s.
