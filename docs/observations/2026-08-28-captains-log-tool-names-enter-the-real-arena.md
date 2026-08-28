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
8/8 semantically correct and one-shot, and its combined median was 2.841s
faster than the strongest challenger. `extract_clojure` was also 8/8 correct,
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
| 4 | U | 28.329s | pass: one call, no reads or shell |

This first U observation is slower than its earlier distribution. The paired
W observations determine whether this is catalog behavior or current service
conditions; it is not compared to the older median in isolation. In the first
paired position, U leads W by 9.215s.

The complete rematch gives U a 31.361s midpoint and W a 37.726s midpoint. U
wins the fresh service window by 6.365s. Across all eight valid one-shot runs
per arm, U's cumulative median remains 29.066s while W's moves to 31.907s. The
control now leads by 2.841s. The retained name survives the requested
apex-predator retest.

The rematch again localizes the advantage before the first call:

| Catalog | Initial materialization midpoint | MCP observer midpoint | Server midpoint | Receipt midpoint |
|---|---:|---:|---:|---:|
| U | 25.686s | 2.462s | 2.422s | 2.355s |
| W | 32.598s | 2.017s | 1.980s | 2.250s |

W remained about 0.445s faster inside the MCP observer interval and 0.105s
faster after the receipt. U reached the first call 6.912s sooner. The retained
name's advantage is model decision materialization, not a faster structural
kernel.

The copied rematch archive is
`/tmp/clj-surgeon-catalog-apex-rematch-results-2f47ddc-20260828T174625Z.tar.gz`,
SHA-256
`3a99beea2aa60575f3eb6bc16031de3185be54a3bb2af86a86815ac68893cbca`;
the local hash matched the remote receipt.

## Brain Fleet interpretation: the mess may be at the right level

The practical product decision is stronger than the causal explanation.
Brain Fleet assigns roughly 80–85% confidence that retaining U is the correct
decision: no challenger earned migration cost, U remained 8/8, and W did not
beat it. Confidence that the public name itself caused the complete measured
advantage is only about 60–70% because order and caller-surface confounds remain.

The leading hypothesis is not that a messy monolith beats clean architecture.
Catalog U actually combines:

```text
familiar apply_clojure_changes retrieval key
  -> strict extraction-only schema
  -> separate continuation operation
  -> runtime admission before effects
  -> one unchanged semantic kernel
```

The name may look vague when the ontology classifies mechanisms. The frozen
task, however, authorizes a heterogeneous outcome: move 15 owners, create a
namespace, rewrite requires and callers, change one Var's visibility, format,
verify, and roll back on failure. `extract_clojure` names the headline
mechanism. `apply_clojure_changes` names the complete authorization envelope.
The evolved label may therefore have landed accidentally at the better
abstraction level.

A second plausible mechanism is learned action grammar. `apply_*_changes`
resembles the decisive mutation grammar of `apply_patch` and many change APIs.
Fresh Codex homes remove installed skills and session history; they do not
remove the model's broad lexical priors. `extract` is polysemous: it can mean a
read, data extraction, function extraction, or namespace movement. A capable
model may spend more effort reconciling that narrow name with a 7,384-character
schema that owns caller edits, visibility, verification, and rollback.

The raw evidence supports localization, not mind reading:

- after normalizing the temporary workspace path, all four apex-rematch call
  payloads had the same SHA-256;
- all four final responses were byte-identical;
- U and W had byte-identical descriptions, schemas, annotations, and peer
  controls after normalizing the extraction identifier;
- across all eight runs, initial-materialization medians were about 22.741s U
  and 26.238s W;
- reasoning-token medians were 345 U and 452.5 W;
- W's server boundary was about 0.477s faster.

Thus Surgeon did not make U faster. Sol reached the same mutation sooner.

## Adversarial limits on the story

The experiment is strong hill-climbing evidence, not a universal naming law:

1. The measured task explicitly requires the first available extraction
   control and supplies the exact object shape. This isolates routing friction
   well but is less naturalistic than an open-ended coding task.
2. The preflight app-server was separate from the measured `codex exec`. It
   proves the expected client surface existed, not the exact order presented
   to the measured model.
3. The retained helper sorted its projection. Raw app-server tool-name maps put
   U first alphabetically and W third. Primacy may be part of U's practical
   advantage. V was also first and still produced one DNF, so order is not a
   complete explanation.
4. Every turn also exposed 49 unrelated `codex_apps` tools. This matches a real
   rich Codex environment but adds tool-context and service variance.
5. U/W selection and the final rematch were adaptive rather than one frozen
   preregistered cohort. V and X have only two runs each.
6. An exact unblocked permutation of the eight U and eight W walls gives an
   observed 2.995s mean advantage but only about 0.094 one-sided and 0.188
   two-sided probability under label exchange. U was faster in 46 of 64
   cross-arm pairs. The direction is useful for retaining an incumbent, but
   the uncertainty interval still includes no intrinsic name effect.

The earned wording is: on this forced-extraction Sol/high task,
`apply_clojure_changes` remained 8/8 one-shot and was observed 2.841s faster at
the median than the strongest alternative. No alternative earned a rename.

## Highest-information next experiments

1. Disable unrelated app tools, retain the actual measured process's complete
   catalog and order, and run a randomized safe refusal canary.
2. Cross U/W names with earlier/later ordinal position. This separates lexical
   affordance from client primacy.
3. Cross U/W with a simple owner move and the full caller/visibility/verifier
   transaction. This tests whether U wins because it names the broader
   authorization envelope.
4. Keep U's name fixed and compare the old combined schema with the strict
   extraction/continuation split. This measures whether the facade's internal
   ontology helps independently of naming.
5. Keep U and test one shorter front-loaded description. The remaining large
   hill is the 23–26s before the first call, not the roughly 2s kernel.

Do not build a synonym zoo or infer a heuristic router. Tool names are retrieval
cues; schemas and admission authorize; the kernel proves effects. The facade's
greatest win is that it lets these hypotheses be changed and falsified cheaply
without moving the semantic machinery.

## Naming chapter closed

The experiment did its job. Keep `apply_clojure_changes`; retain the facade
that made alternative catalogs cheap; stop spending hill-climbing capacity on
synonyms, ordinal-position controls, or a production rename. Those controls
remain useful if a future interface claim specifically depends on vocabulary,
but they are not the next product hill.

The durable architectural result is more important than the winning label:

```text
public projection can change cheaply
  -> typed schema defines request authority
  -> admission refuses unsupported effects
  -> one transport-neutral kernel compiles and proves the transaction
```

This is a Kent Beck win even though the incumbent name won. We lowered the
cost of changing the interface, used that option to run a real tournament, and
learned that no migration currently pays for itself. Reversibility prevented
the experiment from becoming a speculative rename program.

## Next hill: compile the read decision, not another name

The highest-frequency measured waste is now the model boundary between related
structural reads. In the retained 24-hour clock:

- 75 Surgeon-read-to-Surgeon-read transitions consumed 1,084 seconds;
- a direct Surgeon operation had a median wall of about 0.243 seconds; and
- the next agent action arrived about 9.1 seconds later.

The likely prize is not making a 243 ms read into a 120 ms read. It is deleting
one or more nine-second decisions by returning the complete evidence needed for
one coherent judgment.

```text
today
  inspect A      0.243s
  model decides  ~9.1s
  inspect B      0.243s
  model decides  ~9.1s
  mutate

candidate
  compiled read mission: A + B + ownership + snapshot guards
  model decides once
  mutate
```

This is not permission to build a graph compiler. Generic batching and selector
continuation already missed their speed gates. First contact with the retained
v4 receipt found that it records operation, transport, wall, and the next
action, but not a privacy-safe target/snapshot identity or batch cardinality.
It can count the 75 boundaries but cannot yet prove that read B was knowable
when read A was issued. The first reversible ratchet is therefore to add those
fields to the collector and rerun one bounded window. The offline oracle then:

1. groups reads by action ordinal, structural target, and frozen snapshot;
2. classifies whether the second read was mechanically knowable at the first;
3. distinguishes hidden-result recovery, distrust/re-read, judgment-dependent
   investigation, and unrelated sequential work;
4. proceeds only if at least half are mechanically groupable; and
5. only then compares ordinary inspection with one operation-proof dossier.

The product keep gate is exact task correctness, one read, zero fallback, no
more than 1.25 times the unique evidence bytes, and at least 30 percent lower
complete wall. If fewer than half of the second reads were knowable up front,
stop. The event clock remains useful and the product stays unchanged.

This hill outranks further work on the 22--26 second first-call interval for
the naming fixture. That interval is real, but its removability is not yet
localized and its catalog contained substantial unrelated tool context. The
read-transition corpus gives us a repeated, measured decision boundary and a
zero-code falsifier. If the dossier gate fails, the next candidate is to keep
the winning mutation name and kernel fixed while shrinking only its exact
decision surface.

## First read-mission ratchet: identity without disclosure

Receipt schema v5 now attaches a privacy-safe action ordinal to every completed
clock item. Each `inspect_clojure` item also carries request batch cardinality,
a SHA-256 identity for its structural target after removing workspace and
bookkeeping fields, and a SHA-256 over returned source hashes. The original
path, target, owner names, request IDs, source, expectations, and source hashes
remain absent. The timeline renderer shows only twelve-character prefixes.

The exact prior 24-hour window was replayed without advancing the study marker.
It reproduced all 75 read-to-read transitions and their 1,084,371 ms cumulative
boundary wall. The new relation compiler found:

| Adjacent read relation | Pairs | Boundary wall | Median boundary |
|---|---:|---:|---:|
| Exact target | 2 | 13.382s | 7.582s |
| Same files, different target | 21 | 231.740s | 6.467s |
| Overlapping files | 16 | 176.129s | 8.894s |
| Disjoint files | 12 | 348.812s | 22.565s |
| CLI identity unavailable | 24 | 314.308s | 7.389s |

Thirty-nine of the 51 MCP pairs, or 76.5 percent, stayed on the same or an
overlapping file set. This clears the investigation gate, not the product gate:
file overlap does not prove that the second question was knowable before the
first answer. Only two pairs were exact repeats, so a response cache alone
cannot capture the prize. The likely product is a coherent decision surface,
not memoization.

The next bounded step is intent classification of those 39 related pairs. The
classifier must distinguish questions supplied by the original goal, questions
mechanically implied by the first request, choices created by the first result,
refusal recovery, and merely sequential investigation. Product work begins
only if at least 26 of the 51 MCP pairs are honestly groupable.

Retained replay:
`/tmp/clj-surgeon-agent-usage-24h-clock-20260827-v5.json`, SHA-256
`0cd2cf409df6819aed787c942ba0b194ade8c18e812ede71c497c3aeacc1a03d`.

## Broad read-mission gate: stop

The second oracle used only mechanically inspectable evidence. For each MCP
read-to-read pair, it asked whether the second target was an exact repeat,
already requested, or named as an exact source symbol in the first returned
source. A refusal retry was not counted: its correction still required model
judgment. An outline-to-forms transition was not counted: the owner choice was
made from the first result. A file overlap alone was never counted.

| Classification | Pairs | Groupable | Boundary wall |
|---|---:|---|---:|
| Exact repeat | 2 | yes | 13.382s |
| Already requested subset | 1 | yes | 5.864s |
| Exact source-linked follow-up | 7 | yes | 52.944s |
| Refusal recovery | 13 | no | 85.189s |
| Outline-driven selection | 11 | no | 120.133s |
| Judgment-dependent or unrelated | 17 | no | 492.551s |

Only 10 of 51 MCP pairs, 19.6 percent, passed. Their 72.190 seconds were 9.4
percent of MCP read-to-read boundary wall. The preregistered gate required at
least 26 groupable pairs. It missed decisively.

Do not build a general read-mission graph or operation-proof dossier from this
corpus. The model was usually doing real exploratory work: selecting owners
from an outline, correcting a safe refusal, or changing the next question after
reading source. Automatically returning more source would increase evidence
without deleting the judgment boundary.

Three narrower facts survive:

1. exact rereads are real but rare;
2. exact source-linked continuation exists but is a small stratum; and
3. refusal recovery is more frequent than redundant reads and already has a
   concrete one-shot compiler incident.

The next hill is therefore tolerant one-shot compilation (`clj-surgeon-tmr.6`),
not compiled read missions. Replay the retained three-change/two-file failure:
split several complete inserted forms when their syntax is independently
valid, derive redundant aggregate counts from compiled intents, and continue to
refuse the malformed extra parenthesis before any write. This attacks repeated
model boundaries while keeping the model's architectural judgment intact.
