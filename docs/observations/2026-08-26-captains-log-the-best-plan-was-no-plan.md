# Captain's Log: The Best Plan Was No Plan

**Date:** 2026-08-26  
**Branch:** `experiment/submission-row-counterfactual`  
**Candidate:** `543798a`  
**Task:** historical Sessionize 15-form extraction

## The finding

The extraction planner was working. That was the problem.

The frozen task had already supplied every material decision:

- the 4,594-line source namespace;
- the absent destination namespace;
- all 15 owners to move;
- dependency-minimal destination policy;
- the one private definition that must become public;
- the caller scope; and
- the exact affected-file verifier.

We nevertheless made the model ask Surgeon to rediscover those facts, read the
manifest, copy a hash-bound `next_call`, and then apply it. Two retained Sol/high
runs showed that the plan-to-apply model gap alone cost 10.287 seconds and 9.079
seconds. The MCP service itself accounted for only about 12.7--12.8 seconds of
the roughly 49.9-second complete route.

The winning change was therefore not a faster parser or JVM. It was deleting a
decision phase whose answer was already present in the task.

```text
Before

  supplied decision
        |
        v
  plan-extraction -------- 5.86s server
        |
        +----------------- 9--10s model copies an already-known decision
        v
  apply extraction ------- 6.85--6.96s server
        |
        v
  exact clj-kondo

  median complete wall: 49.9405s

After

  supplied decision
        |
        v
  apply extraction ------- 7.837s server, 15 edits / 2 files
        |
        v
  exact clj-kondo -------- 0.247s, 0 errors

  local complete wall: 34.354s
```

The local result was correct and meaning-preserving. It used one
`apply_clojure_changes` call and one shell verifier, with zero discovery calls,
zero refusals, zero failed mutations, zero MCP failures, and no post-decision
reads. Compared with the retained medians, this is 31.2% faster than the prior
MCP route and 3.60x faster than the unchanged native control.

## The losses that earned the answer

Three plausible shortcuts lost before the direct route won.

1. **Narration suppression was noise, not architecture.** A fresh Sol/high run
   completed correctly in 48.111 seconds, only 3.7% faster than the prior MCP
   median. Prompt golf could not remove the underlying plan boundary.
2. **Automatically attaching `verify: "fast"` changed semantics.** The frozen
   task explicitly permits warnings by running clj-kondo with
   `--fail-level error`. The built-in fast profile rejected three introduced
   unused-import warnings. Surgeon rolled the entire extraction back safely.
   Commit `306a6a9` was reverted by `ec304d7`; it is evidence, not a candidate.
3. **The first direct-route caller over-verified.** It added `verify: "full"`,
   launched a nonexistent `make test` target, and reread the cold job. The
   extraction itself was correct, but the route took 50.519 seconds and failed
   route adherence. The public instruction now says to omit `verify` when the
   task supplies its exact external verifier.

The safety mechanisms did their job in both losing mutation experiments: the
over-strict fast verification rolled back, while the asynchronous full check
left an explicit receipt and did not invite a blind retry.

## Product rule

Use `plan-extraction` when caller migration, moved-owner closure, visibility, or
destination policy is unknown. When the task already supplies the exact source,
destination, forms, visibility changes, and caller accounting, submit one direct
extraction transaction. The planner remains a safe fallback; it is no longer a
tax on complete decisions.

This rule is now encoded in the public MCP description, extraction schema text,
benchmark route, and a permanent public-boundary test that moves a private form
and publicizes it without a preflight plan (`85aaa3c`, refined by `543798a`).

The rollout also removed one stale global instruction. Codex previously said
every extraction must run CLI dependency and preview commands. That remains
correct for uncertain work, but it defeated this earned MCP fast path when the
complete extraction decision was supplied. Codex and Claude now share the
conditional law: direct atomic MCP extraction for complete decisions;
dependency/manifest planning for unknown closure, visibility, or callers.

## Ethnographic window

The bounded history/telemetry study covered
`2026-08-25T00:49:16Z` through `2026-08-26T15:16:56.988563Z`
(`2026-08-24 17:49:16` through `2026-08-26 08:16:56.988563` Pacific).
Receipt:
`/tmp/clj-surgeon-agent-usage-20260825T004916Z-20260826T151656988563Z.json`.

It observed 501 Surgeon calls across 38 Surgeon-using Codex turns and 221 MCP
service calls totaling 171.42 seconds of direct tool wall. That supports the
same conclusion as the frozen replay: complete-task latency is dominated by
decision fragmentation and model-managed phase transitions, not the median MCP
operation.

## Status

The counterbalanced Anvil gate passed at exact head `543798a` from tracked-clean
CWD
`/srv/fleet/dev-a/clj-surgeon-direct-extraction-543798a-20260826T154529Z`.
No arm was repaired or rerun.

| Order | Arm | Correct | Wall | Actions | Shell / MCP | Route-adherent |
|---|---|---:|---:|---:|---:|---:|
| MCP first | direct MCP | yes | 40.262 s | 2 | 1 / 1 | yes |
| MCP first | native | yes | 124.435 s | 5 | 4 / 0 | yes |
| native first | native | yes | 120.121 s | 6 | 5 / 0 | yes |
| native first | direct MCP | yes | 35.479 s | 2 | 1 / 1 | yes |

Both MCP arms made exactly one successful direct extraction call, zero
inspections, omitted `verify`, and ran the exact requested verifier once. They
had zero refusals, MCP failures, or failed mutations. Both preserved meaning;
the destination was byte-exact, while the source differed only in accepted
presentation.

A later forensic audit overturned the original `native 0/2` label. Both native
arms also faithfully materialized the supplied decision: all 15 form bodies and
comments survived, ownership and visibility were correct, callers resolved
through the destination, both files parsed, and exact `clj-kondo --fail-level
error` passed. The old generic scorer mistook harmless differences from one
historical after-state for meaning loss: focused `:refer` versus alias plus
refer, removal of unused imports, form order, banner-comment placement,
namespace wrapping, and omission of an unrelated namespace docstring. Those
are useful byte/style telemetry, but they are not task correctness.

Median direct MCP wall was 37.8705 seconds versus the prior two-call MCP median
of 49.9405 seconds: 12.070 seconds and 24.2% lower complete-task time. The new
native median was 122.278 seconds, only 1.1% different from the prior frozen
123.583-second median, so arm order did not manufacture the improvement. The
observed direct-MCP/native wall ratio is 3.23x. Because the forensic audit found
both routes 2/2 correct under the supplied-decision invariants, this is a
matched-correctness route result—not merely a ratio against failed attempts.

### Native comparison, in one view

| Route | Exact outcome | Median complete wall | Median tool actions | Compared with direct MCP |
|---|---:|---:|---:|---:|
| Direct extraction MCP | 2/2 correct | 37.871 s | 2 | baseline |
| Plan + apply MCP | 2/2 correct | 49.941 s | 3 | 12.070 s slower; 1.32x wall |
| Native control | 2/2 correct | 122.278 s | 5.5 | 84.408 s slower; 3.23x wall |

Against the unchanged native route, direct MCP used 3.5 fewer tool actions at
the median and 69.0% less wall time at equal observed task correctness. The
defensible conclusion is narrow but now genuinely comparative: on this
supplied multi-form extraction, Surgeon compiled the already-complete decision
into one mutation while native spent three to four additional actions on
discovery and source handling. This does not establish functional correctness
or general superiority; it establishes a 3.23x complete-route win on one frozen
historical decision under proportional mechanical invariants.

### Champion native did not close the gap

We then gave fresh Sol/high callers a native prompt designed by an explicit
native champion. It supplied the ownership and caller decisions, permitted one
coherent `apply_patch` or bounded generated script, required stale-source
assertions, prohibited whole-file reads and Surgeon exposure, and kept exact
historical bytes secondary. Both fresh runs received byte-identical prompt
`8f133e308ede3626d25a9d45e41e29e68dc2d98218dfde31a61d4981ef22f8a6`.

| Route | Correct | Median complete wall | Median actions | Relative to direct MCP |
|---|---:|---:|---:|---:|
| Direct extraction MCP | 2/2 | 37.871 s | 2 | baseline |
| Original native | 2/2 | 122.278 s | 5.5 | 3.23x slower |
| Champion native | 2/2 | 142.545 s | 5 | 3.76x slower |

Champion native was 16.6% slower than the original native median. More
importantly, the traces explain why prompt improvement did not change the
route. Each caller spent two actions discovering source spans, attempted one
large hand-compiled mutation that refused before writing, corrected that
script in a fourth action, and linted in a fifth. dev-b chose unavailable Ruby
and received exit 127; dev-c composed source transformations in an invalid
offset order and hit an assertion. Both recovered safely and produced correct
code, but the model still had to implement and debug a miniature structural
editor inside its shell command.

That is the moat on this task. Surgeon does not win because native cannot make
the change. It wins because a complete decision can invoke a tested structural
mechanism directly, replacing discovery plus bespoke mutation-program
construction and repair with one guarded transaction. The next experiment must
test where this advantage ends as decisions become incomplete; it should not
spend another cohort prompt-golfing the same complete decision.

### Captain's decision: make Surgeon faster

The native line is now closed. It served its purpose: native is a correct,
fearsome control, and Surgeon still won decisively. Further native prompt
optimization does not improve the product.

The first completeness-rung dogfood made the next Surgeon hill concrete. When
the exact 15 roots and destination were supplied but visibility and caller
proof were deliberately withheld, a fresh local Sol/high caller completed the
expected route exactly once:

```text
inspect_clojure(mode=plan-extraction)
  -> apply_clojure_changes(snapshot-bound next_call)
  -> exact clj-kondo
```

It was task-invariant correct and route-adherent in 51.624 seconds, with three
tool round trips, two successful MCP calls, and zero failures. That reproduces
the prior 49.941-second plan/apply median. Compared with direct extraction at
37.871 seconds, the public planning phase costs about 12 seconds and one model
interaction.

A deeper root-discovery rung took 163.572 seconds and stopped safely before
mutation after sending an unsupported `intent` field and an incorrect owner
set to `plan-extraction`. That is useful boundary evidence, not the next
optimization target: vague architectural intent still requires a real planning
decision.

The next product hill is narrower and higher leverage: **make planning an
internal compiler, not a mandatory public phase.** If omitted extraction facts
are mechanically provable against the frozen snapshot—target dependencies,
required visibility changes, and complete zero-candidate external caller
accounting—`apply_clojure_changes` should derive them and commit in the same
call. If any genuine caller decision remains, it must refuse before writing and
return the completed snapshot-bound plan. Models should pay another interaction
only for judgment, never for bookkeeping the kernel can prove.

Immutable result directories:

- `/srv/fleet/dev-a/clj-surgeon-study-results/20260826T154529Z-direct-extraction-543798a-dev-a-mcp-first`
- `/srv/fleet/dev-a/clj-surgeon-study-results/20260826T154529Z-direct-extraction-543798a-dev-a-native-first`
- `/srv/fleet/dev-b/clj-surgeon-study-results/20260827T002056Z-champion-native-8ab0c60-dev-b2`
- `/srv/fleet/dev-c/clj-surgeon-study-results/20260827T002039Z-champion-native-8ab0c60-dev-c2`
- `/tmp/clj-surgeon-hill2-plan-508dcd0`
- `/tmp/clj-surgeon-hill2-discover-508dcd0`

The candidate is recommended for integration. The earned principle is simple:
**do not ask a planner to rediscover a complete decision.**

## Stable publication receipt

Stable publication completed at exact head
`66b8a606e44786bb8a835d7bcd79fc3da3c15afc`:

- the installed CLI, Codex skill, and Claude skill all carry that source
  commit;
- the global Codex and Claude routing blocks match committed block
  `a27adb653893e5c601a268cd11c9f4445d82d632469eeb93f3a8c72dc164b560`;
- the complete gate passed with 613 core tests / 5,275 assertions and 232 MCP
  tests / 1,917 assertions, plus every ancillary gate and a clean diff check;
- the shared MCP hot-reloaded without restarting its process; and
- a real live refusal returned all 6/6 owners, ranked
  `resolve-source-path` first, marked the hypothesis `authority=false`, and
  completed in 24.12 ms. The existing MCP client session remained valid.

The first live proof had remained sparse even though registry synchronization
reported success. The reload manifest had omitted the newly shared
`show-form` and `owner-hypotheses` dependencies. Publication stopped; no blind
retry occurred. Commit `f5cc75c` added both dependencies and a permanent
`MCP-OP-READ-PARITY-001` witness. The corrected reload then passed the real
operation proof. A false green became a paved-road regression test before the
release window closed.
