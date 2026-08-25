# Captain's Log: The scalpel must earn every call

<!-- agent-usage-window-end: 2026-08-25T00:49:16Z -->

**Window:** 2026-08-24 07:00:00–2026-08-25 00:49:16 UTC;
2026-08-24 00:00:00–17:49:16 PDT

## Verdict

The next clj-surgeon breakthrough will not come from maximizing clj-surgeon
usage. It will come from making the model's already-formed decision compile
into one guarded burst, while abstaining wherever native tools are already the
shortest path.

Today's evidence shows three different frontiers:

1. A small supplied literal change was **1.89x faster with native editing**
   than the normal installed-skill route.
2. A real multi-owner failure-atomic implementation was **1.18x faster with a
   selective production route** than native editing. The winning route used
   Surgeon for structural perception and native tools for materialization.
3. Forcing that same task through structural tools was the slowest route. It
   incurred four safe refusals and finished 1.60x slower than the selective
   production route.

The product is not "a Clojure IDE that the model must use." The useful product
is a small structural camera plus an intent compiler:

```text
discover broadly with native search
  -> see exact structure once when structure removes uncertainty
  -> decide architecture in the model
  -> compile the complete mechanical decision once
  -> hot focused proof
  -> bounded cold proof outside the foreground loop
```

The acceptance bar for the final materialization phase should be at least 5x,
not 1.2x. A 1.2x full-task result merely proves that a crossover exists.

## Bounded daily receipt

`make study-agent-usage` collected one privacy-safe v3 receipt for the exact
window. It contains no transcript prose, workspace paths, source bodies, or
raw service events. `make study-agent-usage-self-test` passed after collection.

| Provider | Sessions | Clojure-relevant sessions | Task turns | Surgeon calls |
|---|---:|---:|---:|---:|
| Codex | 5 | 5 | 29 | 260 |
| Claude | 4 | 0 | 0 | 0 |

Claude supplied no comparable Clojure task in this window. Its row is a
coverage fact, not a performance comparison.

All five relevant Codex sessions could see the skill and all five loaded it.
Visibility and loading therefore did not guarantee a short route.

Codex made 260 clj-surgeon calls:

- 74 MCP calls: 66 `inspect_clojure`, eight `apply_clojure_changes`;
- 186 CLI calls, led by 118 `:cat` and 45 `:ls` operations;
- 81 native patch actions and 229 native-read phases in the same window.

The mixed route is not inherently wrong. The problem is duplicated perception
and repeated conversion between representations.

## The locally cheap, globally expensive trap

The shared MCP was hot and mechanically fast:

- 74 calls, 61 successful and 13 safely refused;
- 484 ms median direct tool wall;
- 112.601 seconds total direct MCP wall;
- 134 file reads and 304,210 returned source characters.

`inspect_clojure` alone consumed 95.874 seconds of direct service time. The
median was only 426 ms, but 66 individually reasonable inspections created a
large aggregate interaction tax before model deliberation was counted.

Only nine of 29 task turns used Surgeon. Six of those nine made no structural
apply at all. The largest turn made 103 Surgeon calls, crossed 27
Surgeon-read phases, 49 native-read phases, and 29 native-patch phases, and
spent 211.278 seconds inside Surgeon-bearing tool actions. It reached only one
Surgeon-apply phase.

That is not editor golf. It is repeated reacquisition.

A live scheduler-work screenshot made the pattern concrete. One good batch
returned 14 already-known forms across five files with exact owners, hashes,
and terminal completeness. The caller then requested an outline and another
three form bodies. The first batch was better than `grep defn`; the following
calls erased part of its advantage.

## When structural reading beats grep

`rg` or `grep` remains the best broad discovery tool. It cheaply answers
"where might this text or name occur?"

A structural read earns its call when it answers a materially stronger
question:

- return several complete, already-known owners across files in one batch;
- recover balanced multi-line source without guessing ranges;
- distinguish top-level ownership, `defmethod` dispatch, reader conditionals,
  comments, strings, and textual decoys;
- prove cardinality and completeness;
- return a hash-bound address that the following edit can reuse.

If the caller needs only definition names or candidate files, structural
reading is excess ceremony. If `read_complete=true` already answered the
decision, another outline or body read is turn amplification.

## The historical high-leverage archetype

The first real-use ethnography in
[`2026-03-28-first-real-use.md`](2026-03-28-first-real-use.md) recorded roughly
15 minutes, 20 Surgeon invocations, eight manual edits, and ten `bb` filters to
fix six forward declarations. The model understood the desired result, but it
had to rediscover callers, move dependency chains, delete declarations, and
recheck after every mutation. The report asked for one compound operation.

The later large-namespace refactor in
[`2026-08-10-captains-log-the-scalpel-was-fast-the-refactor-was-not-compiled.md`](2026-08-10-captains-log-the-scalpel-was-fast-the-refactor-was-not-compiled.md)
made 118 Surgeon calls and 31 native patches. Four LSP initializations consumed
about 480 seconds. Its conclusion was exact:

```text
think -> compile -> bang -> verified and reversible
```

The follow-up clean caller then deleted 17 supplied owners in one
`apply_clojure_changes` call with 1.216 seconds of direct tool time, no source
preflight, no semantic lookup, no marker forms, and no native cleanup.

That sequence demonstrates the mechanism for a 5x-class win, but the document
correctly did not call 1.216 seconds a matched whole-task speedup. The strongest
controlled full-turn results recorded so far are:

| Task | Faster route | Control | Speedup |
|---|---:|---:|---:|
| supplied two-file owner-scoped decision | 38.60 s | native 134.26 s | 3.48x |
| supplied six-edit/two-file decision | 27.976 s | native 68.932 s | 2.46x |
| corrected exact outgoing-call route | 20.049 s | failed route 84.799 s | 4.2x |

The missing experiment begins after architectural visualization is complete.
It must not charge the editor for the model's open-ended design phase.

## The second historical replay

The frozen `failure-atomic-commit` replay asks fresh Sol/high callers to
reconstruct a real failure-atomic source/test/documentation change. The child
commit remains hidden and all arms are scored by the same focused 19-test,
145-assertion verifier.

| Route | Semantic result | Wall | Actions | MCP route |
|---|---:|---:|---:|---|
| production choice | pass | **413.540 s** | 22 | seven inspections, no MCP write |
| native only | pass | 487.903 s | 24 | none |
| forced structural | pass | 660.644 s | 24 | six inspections, four applies |

All three touched only allowed paths. None reproduced the historical child
bytes exactly, which is expected for a semantic implementation task with
multiple valid designs.

The production route beat native by 74.363 seconds, or 15.2%. More important,
it beat forced structural by 247.104 seconds, or 37.4%. It used structural
perception and then selected native writes. The forced route suffered two
inspection refusals and two apply refusals before succeeding.

This is evidence for selective routing, not an argument to stop at 1.18x.

## Product thesis: three gestures, not a growing catalog

### 1. See

One batched structural read for known owners or relationships. The result must
be compact, complete, and reusable. It should return stable snapshot-bound
form references so later writes do not repeat file, owner, source, counts, and
hashes.

### 2. Bang

One guarded editor gesture when file, owner, old subtree, and replacement are
already known. This is `edit_clojure`: Emacs-style speed with the additional
guarantee that the old form has not changed underneath the caller.

### 3. Compile

One coherent transaction for an already-made architectural decision: several
owners, files, insertions, deletions, caller rewrites, or namespace movement.
The model supplies judgment. Surgeon owns balanced syntax, addressing,
cardinality, formatting, snapshot fences, failure atomicity, rollback,
read-back, and one verification cycle.

Everything else must earn its existence against native search, native patch,
hot nREPL, compiler, linter, and tests. Surgeon should not encode more
architectural judgment merely to increase adoption.

## MCP versus CLI

The kernel should have two transports, not two semantics.

MCP is preferable for a hot interactive caller when one typed request batches
multiple reads or edits, avoids shell quoting, or returns continuation state
that the next tool call consumes.

CLI is preferable for one simple read, cold or disconnected work, CI and
scripts, durable saved plans, piping, and human diagnosis. The CLI should
accept the same versioned request through stdin or a file so quoting is not an
artificial MCP advantage.

## Smallest falsifiable improvements

1. **Routing card, prompt only.** State that broad discovery and small supplied
   literal edits are native-positive; one complete structural batch is
   terminal; exact owners must not initialize cclsp; complete multi-owner
   decisions should use one transaction. Replay both the native-positive and
   multi-owner historical cases. The change passes only if it preserves the
   multi-owner win while routing the small case natively.
2. **Decision-boundary benchmark.** Freeze historical changes as decision
   packets containing exact owners, intended transformations, and semantic
   gates but no patch. Start the clock after the model has enough evidence to
   state the whole change. Compare one transaction with native materialization.
   The keep gate is at least 5x complete wall, exact semantics, zero failed
   mutations, and one foreground verification cycle.
3. **Terminal-read behavior.** Replay the five-file/14-form task with current
   and revised result language. A successful batch must cause zero subsequent
   source reacquisition for the supplied owners.
4. **Snapshot-bound handles.** Let `see` return opaque form references bound to
   workspace, file, owner, and source hash. Let `bang` reuse those references.
   Stale handles refuse before write. Compare payload size, first-call success,
   and reacquisition calls with literal owner/source payloads.
5. **Read transport crossover.** Extend the existing frozen inspect portfolio
   across 1, 3, and 10 known owners plus one multi-file batch. Measure cold CLI,
   repeated CLI, hot MCP, direct tool wall, complete caller wall, output bytes,
   and correctness. Promote MCP reads only above the measured crossover.
6. **Verification ladder.** Compare full-suite-before-and-after with focused
   failing test, one transaction, hot nREPL/focused green, and asynchronous
   bounded cold proof. The foreground route must remain truthful without
   paying two cold suites.

## Recommendation

Trim ambition aggressively around judgment and expand ambition around
interaction compression.

Do not try to beat `rg` at discovery or the model at architecture. Do make an
already-visualized Clojure change feel like a church-organ chord: one compact
decision, one mechanical burst, one immediate semantic proof, and a reversible
receipt.

The next implementation should be the prompt-only routing experiment followed
by the decision-boundary benchmark. If those cannot produce a replicated
5x-class materialization win, the transaction surface is still asking the
model to carry too much mechanics.
