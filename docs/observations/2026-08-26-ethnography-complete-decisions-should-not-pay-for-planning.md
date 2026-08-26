# Ethnography: Complete Decisions Should Not Pay for Planning

<!-- agent-usage-window-end: 2026-08-26T15:16:56.988563Z -->

**Study window (UTC):** 2026-08-25 00:49:16 through 2026-08-26
15:16:56.988563  
**Study window (Pacific):** 2026-08-24 17:49:16 through 2026-08-26
08:16:56.988563  
**Receipt:**
`/tmp/clj-surgeon-agent-usage-20260825T004916Z-20260826T151656988563Z.json`

## Question

What would make structural tooling purely net-positive for a native coding
agent: faster complete verified changes, not more clj-surgeon adoption?

The previous study found that Surgeon earned its keep on large supplied
mechanical decisions but often became a tax during discovery. This window asks
where the tax remains after compact editing and extraction became capable.

## Sampling and exclusions

The repository collector joined local Codex and Claude histories with
clj-surgeon MCP, cclsp, and clojure-lsp telemetry for the marker-bounded window.
It found 41 sessions for each provider, of which 32 Codex and 25 Claude sessions
were Clojure-relevant. Session identities are hashed; transcript prose,
workspace paths, and raw service events are excluded from the receipt.

This is observational telemetry, not a randomized comparison. Claude and Codex
worked on different task mixes. Some Codex traffic came from clj-surgeon product
development and benchmark/self-test work, so aggregate refusal counts are not a
production-user failure rate. Complete-turn comparisons are restricted to
recognized task turns, and the native-only cohort is small. The controlled
historical counterfactual later in this report supplies the causal test.

No internet research or remote transcript search was used for the aggregate.
The final bounded reconstruction used the collector receipt, two retained local
Codex event streams, their benchmark summaries, `jq`, `rg`, and the benchmark
verifier.

## Provider scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Clojure-relevant sessions | 32 | 25 |
| Surgeon calls | 501 | 0 |
| Surgeon tool actions | 398 | 0 |
| Native Clojure patch/edit actions | 255 `apply_patch` | 146 edits |
| Native Clojure shell actions | 678 | 392 |
| Bounded Clojure reads | 0 reported by collector | 63 |
| Unbounded Clojure reads | 0 | 5 |
| Surgeon output characters | 2,058,266 | 0 |
| Clojure read output characters | 0 | 314,647 |
| Skill-loaded relevant sessions | 20 | 0 |

The Claude result does not mean native editing was faster; the collector lacks
matched complete-task controls for Claude. It does show that mere skill
visibility did not produce Surgeon use: the activation trigger was visible in
14 relevant Claude sessions, but no session loaded or called the tool.

Codex used both instruments heavily. Its 501 calls included 194 MCP
`inspect_clojure`, 21 MCP `apply_clojure_changes`, nine MCP `edit_clojure`, and
older CLI-shaped reads such as 196 `:cat` and 74 `:ls` calls. It also made 255
native patches. The route was hybrid, not structurally exclusive.

## Service clock versus task clock

The MCP service recorded 221 calls:

| Operation | Calls | Median service wall | p90 | Total wall |
|---|---:|---:|---:|---:|
| `inspect_clojure` | 185 | 145 ms | 792 ms | 86.312 s |
| `apply_clojure_changes` | 36 | 1.571 s | 7.026 s | 85.108 s |
| **All MCP** | **221** | **157 ms** | **1.960 s** | **171.420 s** |

The applies were substantive: 16 were multi-edit, ten were multi-file, and the
largest transaction contained 51 edits across 12 files. Service execution is
not free, but it is much smaller than the minutes consumed by complete turns.

The recognized Codex task-turn cohort had this observational shape:

| Route observed | Turns | Median complete turn | Median actions | Median route phases |
|---|---:|---:|---:|---:|
| Surgeon used somewhere | 28 | 9.08 min | 11 | 6 |
| Native patch and no Surgeon | 4 | 7.48 min | 3 | 2 |

This is confounded by task difficulty. Its useful signal is route structure:
the Surgeon cohort contained 230 structural-read phases but only 16 structural
apply phases, and nine Surgeon-using turns eventually patched natively. The
common expensive route was a sequence of partial decisions:

```text
structural read -> another read -> native read -> decide -> native patch -> verify
```

The tool was often the fastest participant in a slow conversation.

## Failures and successful behavior

At the service boundary, 181 of 221 calls completed and 40 refused. The most
common recorded refusal was exact batch-form selection (25); smaller clusters
included invalid intent forms, count mismatches, missing files, overlap, and
one verification failure. At the caller-history boundary, Codex recorded 115
refusal actions across old CLI, MCP, deliberate safety tests, and product
development. These counts prove that safe refusal paths were exercised; they
do not estimate the probability that a normal user request fails.

The strongest successful behavior was the opposite of adaptive recovery: one
complete supplied decision became one transaction. That pattern produced the
retained 51-edit multi-file wins, the owner-deletion wins, and the historical
extraction result. The model did best when Surgeon received a compiled chord,
not when it was asked to conduct the whole investigation.

## Bounded reconstruction: the unnecessary extraction plan

Two retained Sol/high extractions of the same 15-form historical task were both
correct. Their plan server work was about 5.86 seconds, their apply server work
was about 6.9 seconds, and their complete walls were 51.322 and 48.559 seconds.
The event timestamps isolate 10.287 and 9.079 seconds between plan return and
apply start. Nothing changed in that interval except the model reading and
copying a manifest whose decisions the task had already supplied.

A narration-suppression prompt falsified the shallow hypothesis: 48.111 seconds,
only 3.7% better than the retained MCP median. An automatic `verify: fast`
experiment also lost because it strengthened the frozen task's verification
semantics; Surgeon safely rolled back. The first direct route over-verified with
`verify: full`, producing a correct extraction but an invalid 50.519-second
route.

The smallest falsifiable improvement was then:

> When source, destination, complete owner set, visibility changes, caller
> accounting, and verifier are supplied, skip `plan-extraction`. Submit one
> direct atomic extraction, then run the supplied verifier once.

Local Sol/high proof at `543798a` completed correctly in 34.354 seconds with one
MCP call and one shell verifier, zero discovery, zero refusals, and zero failed
mutations. That is 31.2% below the prior MCP median. The unchanged native median
is 123.5835 seconds, making the local direct result 3.60x faster.

## Progress against the product goal

The explicit product goal is complete verified task time, with structural tools
earning every interaction. This study advances it in three ways:

1. It rejects tool-adoption metrics: Claude's zero use is not itself a failure,
   and Codex's 501 calls are not themselves a success.
2. It isolates the dominant remaining cost as model-managed phase boundaries,
   not median MCP execution.
3. It turns a broad aspiration into a routing law: plan only unknown decisions;
   compile supplied decisions once.

The local acceptance gate passed. Two fresh counterbalanced Anvil MCP arms then
passed at exact head `543798a`: 40.262 and 35.479 seconds, both correct,
one-shot, and refusal-free. Both unchanged native controls remained incorrect at
124.435 and 120.121 seconds. Median MCP wall fell from 49.9405 to 37.8705
seconds (24.2%) while the native median changed by only 1.1%, so the improvement
survived arm-order reversal.

## Counterfactual limits

- The extraction task is large and unusually well specified; direct extraction
  should not be generalized to unknown callers or ambiguous visibility.
- Native results remain strategy-sensitive, and the prior native cohort was
  0/2 correct. The speed ratio is therefore not a matched-correctness latency
  estimate.
- The two Anvil replicates plus one local run establish feasibility and
  direction, not a low-variance latency distribution.
- MCP service wall omits caller/model latency and transport outside the server's
  clock; complete benchmark wall remains the product meter.

## Counterbalanced result

The final Anvil worktree was tracked-clean at
`/srv/fleet/dev-a/clj-surgeon-direct-extraction-543798a-20260826T154529Z`.
No arm was repaired or rerun.

| Order | MCP wall / result | Native wall / result |
|---|---:|---:|
| MCP first | 40.262 s / correct | 124.435 s / incorrect |
| native first | 35.479 s / correct | 120.121 s / incorrect |

Both MCP event streams contain exactly one `apply_clojure_changes` extraction,
zero `inspect_clojure` calls, no `verify` field, and one exact clj-kondo command.
Both destination files were byte-exact; both source files were parseable and
meaning-preserving with presentation-only differences. Both native prompts had
the same SHA-256 as the prior frozen controls, and both native outputs lost
meaning. This passes the routing and correctness gate and earns integration of
the direct-decision rule.

The next test should generalize the rule to a different extraction with one or
more real external caller decisions. It should determine where direct caller
accounting remains cheaper than `plan-extraction`, and where the planner begins
earning its call again.
