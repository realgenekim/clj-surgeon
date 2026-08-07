# Captain's Log: One Call Crossed the Double-Digit Gate

**Date:** 2026-08-07

**Question:** Can a typed, persistent MCP entrance turn one complete Clojure
editing decision into one verified transaction and beat native `apply_patch`
by at least ten seconds?

## Bottom line

Yes. The result replicated across four counterbalanced correct runs.

The frozen task supplied six exact nested replacements across two files. Each
assisted caller used one `apply_clojure_changes` MCP call. All four calls
produced exact accepted bytes, terminal read-back verification, and no shell
commands, source reads, native patches, diffs, or recovery actions.

The assisted MCP median was 24.530 seconds. The matched native median was
43.190 seconds. MCP was 18.660 seconds faster, a 43.2% reduction in complete
task wall time. It also beat the current CLI-and-skill median of 36.396 seconds
by 11.866 seconds, a 32.6% reduction.

The mechanism passed its performance gate. Metadata-only activation did not.
In four additional no-hint runs, Codex had the MCP tool but ignored it every
time and used native patching. Those runs had a 40.618-second median.

A one-sentence project `AGENTS.md` rule then changed adoption from 0 / 4 to
4 / 4. The rule lane remained one-shot and exact, with a 27.432-second median.
The activation mechanism can therefore be small and durable. It does not
require loading the complete skill.

## Replicated comparison

Correctness gated every efficiency median. Each lane contains four correct
runs. Lane order was reversed for half of the runs.

| Measure | Native control | Current CLI + skill | Assisted hot MCP |
|---|---:|---:|---:|
| Exact accepted bytes | 4 / 4 | 4 / 4 | 4 / 4 |
| Median wall | 43.190 s | 36.396 s | **24.530 s** |
| Median input | 74,872 | 64,842 | **44,020** |
| Median uncached input | 18,228 | 10,878 | **8,936** |
| Median output | 1,468 | 1,313 | **814** |
| Median shell calls | 4 | 3 | **0** |
| Median MCP calls | 0 | 0 | **1** |
| Median source output | 3,506 bytes | 1,758 bytes | **0 bytes** |
| Median MCP result | 0 bytes | 0 bytes | 546 bytes |
| Median post-decision reads | 3 | 2 | **0** |
| Failed mutations | 0 | 0 | 0 |
| One verified transaction | no | 4 / 4 | **4 / 4** |

The four assisted walls were 24.437, 33.649, 22.727, and 24.622 seconds. The
four native walls were 48.455, 37.168, 45.216, and 41.163 seconds. The slower
second assisted run did not erase the median advantage.

The first successful server needed 8.564 seconds to start and publish
readiness. Every benchmark recorded bootstrap separately and started the task
clock only after the health gate passed. The product design keeps one loopback
server hot across agent sessions, as an editor keeps its language runtime or
REPL hot. Restarting it for every edit would discard the architectural
advantage.

## The route changed

The native route was:

```text
understand the supplied decision
  -> attempt one multi-file patch
  -> inspect source after a context mismatch
  -> retry the patch
  -> inspect the aggregate diff
  -> answer
```

The successful MCP route was:

```text
understand the supplied decision
  -> call apply_clojure_changes once
  -> receive verification_complete=true
  -> answer
```

The model supplied judgment once. The transaction owned structural addresses,
cardinality checks, write ordering, failure atomicity, parsing, read-back
hashes, and the inverse receipt.

The successful receipt reported:

| Receipt field | Value |
|---|---:|
| Changes | 6 |
| Edits | 6 |
| Files | 2 |
| Committed | true |
| Verification complete | true |
| Direct transaction wall | 89.875 ms |
| Next action | none |

## The negative stages changed the product

The failures were useful because each isolated a different layer.

| Stage | Valid performance evidence? | What happened | Product change |
|---|---|---|---|
| Cold stdio entrance | no | The JVM became ready after Codex had already selected native tools. MCP was never called. | Make persistent Streamable HTTP the primary transport. Keep stdio as a fallback. |
| First hot HTTP call | no | Codex constructed the complete six-edit call, but unattended approval canceled it before execution. | Mark the server required, allow only the editing tool, and set its approval mode explicitly. |
| Voluntary hot runs | yes, as adoption evidence | Exact results took 45.395 s and 55.190 s, but Codex still chose native patching first. | Separate the performance ceiling from voluntary activation. Strengthen the LLM-facing pitch. |
| First assisted call | no | The tool was selected first, but the Java MCP SDK supplied `LinkedHashMap` and `ArrayList`; the contract accepted only Clojure persistent containers. | Normalize Java JSON containers recursively at the protocol boundary. Add a real valid HTTP mutation test. |
| Correct assisted call | yes | One MCP call succeeded on the first attempt with terminal verification. | Keep the mechanism; replicate and improve activation. |

The replication added one more decisive negative stage:

| Stage | Valid evidence? | Result | Meaning |
|---|---|---|---|
| Four counterbalanced assisted runs | yes | 4 / 4 correct; 24.530 s median | The double-digit gain replicated. |
| Four no-hint MCP-available runs | yes, for adoption | 0 / 4 MCP calls; 40.618 s median | Tool availability and metadata still did not overcome the native-patch habit. |
| Four current CLI-and-skill runs | yes | 4 / 4 correct; 36.396 s median | The typed MCP action beat the already-optimized CLI route by 11.866 s. |

## One project rule unlocked the route

The activation experiment added this rule to the isolated benchmark
workspace. The neutral task prompt did not mention MCP:

> For two or more exact Clojure replacements or edits spanning files, call
> `apply_clojure_changes` once before reading source or using `apply_patch`.
> Treat `verification_complete=true` as terminal mutation proof.

The harness allowed Codex to read this `AGENTS.md` only in the rule treatment.
It did not install a clj-surgeon skill or add a task-specific hint.

| Lane | Correct | MCP adopted | Median wall | Median input | Shell calls |
|---|---:|---:|---:|---:|---:|
| MCP available, no hint | 4 / 4 | 0 / 4 | 40.618 s | 74,865 | 2 |
| Current CLI + full skill | 4 / 4 | — | 36.396 s | 64,842 | 3 |
| MCP + one project rule | **4 / 4** | **4 / 4** | **27.432 s** | **44,215** | **0** |
| MCP + task hint | **4 / 4** | **4 / 4** | **24.530 s** | **44,020** | **0** |

The four rule-lane walls were 29.900, 23.099, 24.963, and 33.341 seconds.
Every run used one verified MCP transaction as its first mutation, performed no
source read, and emitted the same 546-byte receipt.

The rule lane was 15.758 seconds faster than native and 8.964 seconds faster
than the current CLI-and-skill lane. The task-hint lane remained 2.902 seconds
faster than the project-rule median, but both routes removed the same
mechanical work. More replicates would be required to attribute that small gap
to instruction placement rather than service variance.

This is the smallest successful activation surface tested so far. Tool metadata
describes capabilities. A project rule decides which capability is preferred
in this repository. That division matches Codex's instruction hierarchy and
keeps the MCP tool description from becoming an oversized policy document.

The invalid assisted call refused before source mutation in 2.334 ms and
reported `source_unchanged=true`. The refusal behavior worked even though the
adapter boundary was wrong.

## Dogfood exposed the next edge

The benchmark accounting itself became the first live MCP dogfood edit. One
call attempted five exact replacements in
`bench/summarize_clean_codex.clj`. The first attempt refused the complete
transaction because the caller selected a map without its embedded comment.
Lossless transaction identity retains comments, metadata, reader syntax, and
token spelling. Omitting the comment correctly produced zero matches and left
the file unchanged.

One narrow read exposed the mismatch. The corrected call included and
preserved the comment, then committed all five edits in one verified
transaction. The formatter and summary self-test passed afterward.

This was useful in two ways:

- it proved that real development can use the same MCP entrance that the
  benchmark measures;
- it showed that the refusal remedy should teach the lossless comment rule more
  directly.

The call arguments were visually large because the first write schema supports
exact replacement but not insertion or splice operations. That is a transcript
and token concern, not the source of the measured speedup. Batching one complete
decision remains the primary mechanism. A richer edit algebra should be added
only when repeated field evidence justifies it.

## The read-side hypothesis

Field use also exposed repeated structural reads across files. A read-only MCP
tool could keep the same batching advantage without mutation approval:

```text
inspect_clojure
  several files and structural questions
  -> one ordered snapshot
  -> bounded exact evidence and hashes
```

The intended server surface remains small:

```text
inspect_clojure          perception, read-only, batched
apply_clojure_changes    action, guarded transaction
```

The hypothesis is that a hot, batched read tool can approach a twofold
improvement on read-heavy tasks. Persistence alone is insufficient. The large
gain must come from replacing several caller/tool turns with one decision-sized
batch. The keep gate must compare complete task wall, correctness, calls,
tokens, and evidence bytes against both CLI and native controls.

## Separate the builder from the judge

The next experiment deliberately split into two concurrent lanes:

| Lane | Responsibility | Must not do |
|---|---|---|
| MCP implementation | Build `inspect_clojure`, its compact contract, safety checks, and focused tests. | Choose or rescore the performance tasks after seeing results. |
| Benchmark and evidence | Freeze representative tasks, run matched MCP, CLI, and native controls, verify exact answers, and report all valid runs. | Change the MCP implementation while a comparison is in flight. |

The implementation prompt requires one batched read call, ordered compact
results, source hashes, bounded evidence, and concise MCP output. The benchmark
portfolio remains independent of that implementation. It contains four common
read decisions: retrieve known forms across files, outline a large namespace,
find syntax among textual lookalikes, and compute an aggregate without dumping
source.

This separation protects the hill climb from a subtle failure mode: inventing
a task that merely flatters the latest API. The benchmark owner will first
record the exact server build, task capsule, prompt, and expected semantic
answer. Only then will fresh callers run the three lanes. Incorrect answers do
not contribute latency evidence.

The acceptance levels are intentionally asymmetric:

| Result | Interpretation |
|---|---|
| Less than 30% faster than the best correct control | Useful implementation evidence, but not enough to prefer the new route. |
| At least 30% faster with no correctness or evidence regression | Keep the read surface and continue hill climbing. |
| At least 2x faster on the representative portfolio | Strong evidence that one decision-sized read transaction changes the workflow. |

The write-side result remains the precedent, not a promised read-side result.
Its 43.2% gain came from collapsing a complete six-edit decision into one hot,
verified call. Compact arguments and receipts improve transcript legibility,
but batching is the mechanism that must earn the read-side win too.

## What `clojure_edit` taught us

The neighboring `clojure-mcp` project pitches `clojure_edit` more forcefully
than our first tool description. It tells the agent to prefer the structural
tool over generic file editors, then states the immediate benefits: stable
form addressing, fewer text-match failures, syntax validation, and fewer
generated tokens.

Our first description led with restrictions and guarantees. It explained when
the call was allowed, but it did not make the comparative advantage vivid
enough to overcome the model's strong `apply_patch` habit.

The MCP tool is now named `apply_clojure_changes`. Its route card leads with:

> PREFER this over `apply_patch` when a request supplies two or more exact
> Clojure replacements or spans files.

It then explains why: avoid fragile patch-context mismatches, apply the whole
decision in one call, and reduce reads and generated tokens. The complete
comparison is in [clojure-edit-comparison.md](../clojure-edit-comparison.md).

## Hot development is part of the product

The persistent server binds only to `127.0.0.1`, publishes `/healthz`, and can
start an embedded nREPL. A protocol-level regression test now proves all of the
following in one live JVM:

1. initialize one Streamable HTTP MCP session;
2. execute a valid structural edit through that session;
3. receive terminal verification and a durable receipt;
4. redefine the live handler Var through the embedded nREPL;
5. call the same MCP tool through the same session;
6. observe the new handler without reconnecting or restarting.

The MCP suite now has 23 tests and 267 assertions. The full Babashka suite had
550 tests and 4,806 assertions before the latest protocol regression was added;
the full suite must be rerun before installation.

## Evidence discipline

The server records three different clocks:

- daemon bootstrap to readiness;
- complete Codex task wall;
- direct MCP transaction wall.

They must not be blended. A persistent daemon amortizes bootstrap, but it does
not erase it. The task benchmark excludes startup only after a health and
readiness gate. Full local telemetry records the exact request and receipt for
ethnographic analysis. Metrics mode records shapes and timings without source
payloads. Benchmark raw events and telemetry remain in temporary result
directories unless a result is explicitly retained.

## What remains

1. Integrate the proven one-sentence routing rule without weakening the native
   home turf for one unique text edit.
2. Prove two fresh Codex sessions can reuse one already-hot server process.
3. Implement and benchmark the separate batched `inspect_clojure` read surface
   without changing the write benchmark contract.
4. Improve lossless-match remedies when an omitted comment causes zero matches.
5. Update the vision, accepted plan, README, help, and installed skills from the
   replicated evidence.
6. Run formatter, lint, focused protocol tests, and the complete repository
   suite before any release decision.

The central result is no longer hypothetical or singular. Once selected
correctly, the structural transaction keeps one coherent model decision as one
verified edit action and saves enough agent work to cut end-to-end task time by
43.2%. The remaining problem is not execution speed. It is making the correct
route obvious at the moment of choice.
