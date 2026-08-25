# Captain's log: compact editor versus native pilots

Date: 2026-08-24

## Question

Can a fresh Sol/high caller apply a complete six-edit, two-file historical
decision faster with compact `edit_clojure` than with native `apply_patch`,
without sacrificing exactness or first-attempt safety?

The frozen `decision-batch-edit` capsule supplies both files, both owners, all
six before forms, all six replacements, and exact match counts. Correctness is
byte-exact against the accepted historical result.

## Pilot 1: installed skill versus native, one replicate

Result directory:
`/tmp/clj-surgeon-mcp-native-pilot-f87dacc`

| Route | Exact | Wall | Tool round trips | First mutation | Input tokens |
|---|---:|---:|---:|---|---:|
| MCP plus committed matched skill | yes | 48.465 s | 3 | succeeded | 90,570 |
| Native, no skill | yes | 41.884 s | 5 | refused, then succeeded | 77,481 |

Native won this pair by 6.581 seconds. MCP was 15.7% slower relative to
native; equivalently, native was 13.6% faster relative to MCP. One replicate is
a pilot, not a population estimate.

The mechanical editor was not the bottleneck. Full MCP telemetry reports
219.368 ms total for the single `edit_clojure` call: six guarded edits, two
files, one atomic commit, parse/readback, and receipt. That is 0.45% of the
48.465-second complete turn.

The routes behaved differently:

- MCP read the 81-line installed skill, then committed all six edits correctly
  on its first mutation. It made one post-mutation diff check despite terminal
  editor evidence.
- Native attempted a patch before reading. Context mismatch rejected that
  patch without changing either file. It then ran one bounded `rg`, applied a
  corrected two-file patch, and ran three post-mutation checks.
- MCP still lost despite fewer round trips and a successful first mutation. It
  consumed 13,089 more cumulative input tokens (+16.9%), including 1,825 more
  uncached input tokens and 150 more reasoning-output tokens.

## Interpretation

The compact transaction has demonstrated safety and action-count advantages,
but this pair does not demonstrate lower complete wall time. The leading
hypothesis is that skill loading, MCP catalog/schema context, tool selection,
and model deliberation dominate a sub-quarter-second editing engine. Fewer
actions do not automatically imply a faster turn when the route imposes more
model-side context and cognition.

This result also exposed a benchmark defect: the no-skill MCP route hint still
described an obsolete `old`/`new` schema. Commit `7214db9` replaces it with the
public compact contract (`edits` containing `file`, `within.form`, `from`,
`to`, and `matches`) and makes the native hint symmetrically request one
complete supplied patch.

## Next falsification

Run one fresh paired direct-route pilot on the same capsule:

- MCP with the corrected compact hint and no installed skill;
- native with the symmetric one-patch hint and no installed skill.

Both hints treat successful mutation as terminal proof and prohibit source
reads and post-mutation diffs. This isolates the public editing gesture from
the current long skill. If MCP approaches or beats native here, skill/context
design is the primary optimization target. If it remains materially slower,
measure the fixed MCP catalog and model tool-call tax next.

## Pilot 2: direct route, no skill, one mutation allowed

Result directory:
`/tmp/clj-surgeon-direct-route-pilot-4aac2d1`

| Route | Exact | Wall | Tool round trips | Engine time |
|---|---:|---:|---:|---:|
| MCP, compact hint, no skill | yes | 24.968 s | 1 | 166.150 ms |
| Native, one-patch hint, no skill | no | 16.292 s | 0 successful | not applicable |

The MCP caller used one public `edit_clojure` call, made all six changes on its
first mutation, and stopped on terminal evidence. Removing the installed skill
reduced MCP wall by 23.497 seconds (48.5%) relative to pilot 1. The trials are
independent single samples, so some of that difference can be run noise, but
the instruction/context effect is too large to ignore.

The native caller generated one coherent patch, but its guessed surrounding
whitespace did not match `app_shell.clj`. The patch refused without mutation.
The route hint prohibited the source read needed to recover, so the final state
was incorrect. Its 16.292-second wall is time to safe failure and is excluded
from the efficiency comparison.

This reveals a non-contrived structural advantage. The task supplied complete
Clojure forms, named owners, replacements, and counts, but did not supply the
files' indentation or patch context. Native `apply_patch` needs those exact
bytes and therefore must either read or guess. `edit_clojure` can consume the
supplied forms directly, ignore irrelevant formatting, scope duplicate forms
to their owners, and use exact counts as stale-source guards.

## Pilot 3 design

Keep the direct no-skill MCP route from pilot 2. Give native one bounded source
read followed by one patch, with successful mutation terminal for both lanes.
This makes both routes viable and measures the actual cost of native's required
context acquisition against structural no-read editing.

## Pilot 3: both routes viable, no skill

Result directory:
`/tmp/clj-surgeon-viable-routes-pilot-d41ca36`

| Route | Exact | Wall | Tool round trips | Input tokens |
|---|---:|---:|---:|---:|
| MCP, no read, one compact transaction | yes | 24.547 s | 1 | 45,234 |
| Native, one bounded read plus one patch | yes | 27.918 s | 2 | 45,466 |

MCP won the first both-correct pair by 3.371 seconds: 12.1% lower wall time
than native. Both callers stopped after successful mutation. MCP's direct route
was also stable across pilots 2 and 3: 24.968 and 24.547 seconds, a 421 ms
spread.

The input totals are essentially equal, which removes the large skill-context
confound from pilot 1. The observable route difference is one native `rg` call
returning 2,060 bytes before its patch versus one structural MCP call with no
source acquisition. This is the hypothesized crossover: when the decision
contains forms and owner scope but not formatting bytes, Surgeon can turn the
decision directly into a guarded mutation while native must first materialize
patch context.

This remains one both-correct pair. Run two more fresh pairs in alternating
AB/BA order before treating the 12.1% advantage as repeatable. The small-batch
gate is 3/3 exact per route, zero failed MCP mutations, and a lower paired
median for MCP. A 2--5x claim is explicitly not supported by these data.

## Storyboard: how compiled editing can beat native

The winning claim is not that Clojure parsing is faster than patch application.
Both engines are effectively instantaneous relative to model wall time. The
advantage is that Surgeon lets the caller transmit the decision it already has
without translating that decision into fragile file-layout bytes.

```text
                    NATIVE APPLY_PATCH

  decision already known
           |
           v
  need exact indentation and context
           |
           v
      read source                 model/tool boundary
           |
           v
   manufacture diff hunk          model reasoning
           |
           v
      apply_patch                 model/tool boundary
           |
      +----+-----+
      | mismatch |--------------> read -> rebuild -> retry
      +----+-----+
           v
          done


                   COMPILED EDIT_CLOJURE

  decision already known
           |
           v
  emit one structural transaction
  {owner, from, to, matches} x N
  + optional computed programs
           |
           v
  locate -> guard -> compile -> atomic commit
  -> parse -> read back -> receipt          166--219 ms
           |
           v
          done
```

Native `apply_patch` speaks in file layout. The model must know indentation and
enough surrounding bytes to anchor every hunk. Compact `edit_clojure` speaks in
the decision's vocabulary: named owner, old form, new form, and expected count.
The owner chooses the scope; the old form and count provide compare-and-swap
protection against stale source.

The editor-maestro mental model is one score and one chord:

```text
  app_shell / ide-shell
    :body       -> :body.ide-shell-page
    "/app.css"  -> "/command-center.css"

  source_reader / source-reader-shell
    argument vector -> add document-title
    title           -> computed title
    :body           -> :body.ide-shell-page
    tab label       -> title attribute + document-title

  ================= compiled as =================
  6 gestures | 2 owners | 2 files | 1 snapshot | 1 commit | 1 receipt
```

For computed work, a bounded SCI `program` is the keyboard macro: state one
relation once, declare its exact cardinality and changed-character budget, and
let Surgeon materialize every concrete guarded edit in the same transaction.

### Scenarios with a plausible decisive advantage

1. **Supplied forms, unknown formatting.** This is the demonstrated crossover.
   Surgeon can act immediately; native must read first or guess patch context.
2. **Several edits across forms or files.** One transaction replaces several
   reads, patches, and recovery decisions while adding atomicity.
3. **Duplicate syntax in different owners.** `within.form` selects the intended
   occurrence without requiring a large textual context hunk.
4. **Repeated computed changes.** One program can replace many generated patch
   hunks while preserving exact counts and a total churn budget.
5. **Concurrent or stale source.** Owner scope, old forms, counts, and a frozen
   snapshot refuse the complete transaction before mutation.
6. **Exact preservation outside the decision.** The compact editor changes only
   selected subtrees and leaves unrelated spelling, comments, and layout alone.

### Native-positive boundary

Native remains the strong default for one obvious line whose bytes are already
visible, prose and comments, new files, unsupported operations, and tasks where
source inspection is required for the decision anyway. The product should not
maximize Surgeon adoption. It should recognize when the model already possesses
a compiled structural decision and provide the shortest safe route to execute
it.

### Interface work required for a 2--5x result

The long installed skill erased the mechanical advantage in pilot 1. The path
to a larger speedup is therefore subtraction rather than a faster kernel:

- place the route choice in short always-loaded instructions instead of making
  every caller read a long skill;
- keep the compact public schema small and hide the heavyweight editor unless
  one of its unique semantic or rollback-gated operations is required;
- combine literal edits and computed programs in one transaction;
- treat commit/readback as terminal mutation evidence and apply the same
  proportional verification policy to native and Surgeon routes;
- return a compact receipt rather than expanded edit evidence; and
- teach one memorable rule: when the decision is already expressed as forms,
  compile it instead of rediscovering bytes.

The current result supports a repeatable-action hypothesis, not a 5x claim.
The editor kernel is already fast enough; model context, route selection, and
avoidable tool boundaries are now the principal optimization surface.

## Three-pair small-batch result

The two planned replications completed in alternating order. Result directories:

- `/tmp/clj-surgeon-viable-routes-pilot-d41ca36`
- `/tmp/clj-surgeon-viable-routes-rep2-04a7042`
- `/tmp/clj-surgeon-viable-routes-rep3-04a7042`

| Pair | First lane | MCP | Native with bounded read | Winner |
|---:|---|---:|---:|---|
| 1 | MCP | 24.547 s | 27.918 s | MCP by 3.371 s |
| 2 | Native | 23.142 s | 30.989 s | MCP by 7.847 s |
| 3 | MCP | 24.907 s | 28.812 s | MCP by 3.905 s |

Both routes were exact in 3/3 trials. MCP made one guarded transaction per
trial, with zero failed mutations and zero MCP failures. Native made one bounded
read and one successful patch per trial. MCP won all three paired walls despite
the AB/BA order change.

MCP median wall was 24.547 seconds. Native median wall was 28.812 seconds. The
independent-median difference was 4.265 seconds, or 14.8% lower wall for MCP.
The median paired saving was 3.905 seconds. This is a repeatable small-batch win
on one representative capsule, not evidence of a 2--5x speedup across a task
population.

The next safe-refactoring targets are now evidence-ranked:

1. shorten the 81-line automatically loaded skill while preserving route and
   refusal contracts through focused skill tests;
2. separate the compact editor catalog from the heavyweight semantic editor so
   simple callers do not pay choice/schema costs they do not need; and
3. replay real historical changes with repeated or heterogeneous decisions to
   find where one compiled relation eliminates more than one native round trip.

## Safe-refactored skill experiment

Commits `0e2878d`, `2e641fe`, and `c735e67` added a permanent skill contract,
moved conditional MCP operations to a deferred reference, and reduced the live
entrypoint from 86 lines to 39 with a 45-line build ratchet.

Result directory:
`/tmp/clj-surgeon-short-skill-pilot-c735e67`

| Route | Exact | Wall | Shell commands | Input tokens |
|---|---:|---:|---:|---:|
| Short matched skill + MCP | yes | 35.367 s | 1 skill read | 66,726 |
| Direct compact MCP hint | yes | 26.790 s | 0 | 45,279 |
| Native bounded read + patch | yes | 33.263 s | 1 source read | 46,008 |

Relative to the original 48.465-second matched-skill pilot, the shortened skill
recovered 13.098 seconds (27.0%) and removed the redundant post-mutation diff.
The refactor therefore changed behavior materially, not just wording.

It did not make the common route cheap enough. Reading the short skill still
added 8.577 seconds relative to the direct MCP lane and turned a 6.473-second
direct MCP win over native into a 2.104-second matched-skill loss. The common
exact-edit route should therefore live in always-loaded agent instructions and
the compact tool description. The invoked skill should be reserved for advanced
semantic preparation, CLI fallback, recovery, and uncommon operations.

This is a routing-boundary finding: progressive disclosure makes an invoked
skill cheaper, but the cheapest common skill invocation is no invocation.

## Tool-catalog subtraction did not buy latency

Commit `431749e` extracted a pure, tested catalog-profile seam without changing
the production default. The live contracts measured:

| Tool contract | Characters |
|---|---:|
| `inspect_clojure` | 7,177 |
| `apply_clojure_changes` | 17,751 |
| `edit_clojure` | 2,720 |
| `transform_clojure` | 1,529 |
| Full catalog | 29,177 |

An edit-only profile removes 90.7% of the full contract text. Three fresh
Sol/high trials nevertheless completed in 28.365, 29.115, and 27.870 seconds:
28.365 seconds median, all exact. The prior full-catalog direct-route median was
24.547 seconds. In this small sample the much smaller catalog was 15.6% slower,
while total model input remained approximately 45k tokens.

Result directory: `/tmp/clj-surgeon-edit-only-profile-431749e`

This falsifies catalog size as the missing 2--5x lever. The profile seam remains
useful for controlled experiments, but production should retain the full
catalog until representative evidence shows a routing or latency benefit. The
next search moves to historical task shapes where one compiled decision can
remove several model/tool boundaries, especially multi-owner deletion and
extraction after architecture is already decided.

## Exact-owner deletion crossed 2x in its historical calibration canary

Commits `f104c0b` through `bfcca4d` added a compact `delete_owners` gesture to
`edit_clojure`. One request names a file and an ordered set of exact top-level
owners. The adapter lowers that gesture into the existing failure-atomic
transaction kernel, so owner uniqueness, parse-before-write, read-back hashes,
and the inverse receipt remain authoritative without a source preflight read.

The first replay deliberately reconstructs the exact clean-caller experiment
from Codex session `019fecdc-fd65-7940-8345-61af94cced8b`: 17 contiguous
synthetic owners with attached comments between two keep sentinels. This is a
historical calibration replay, not production-code evidence. That distinction
is encoded in the capsule provenance.

Result directory:
`/tmp/clj-surgeon-historical-delete-canary2-20260824`

| Same Sol/high task boundary | Exact | Wall | Tool round trips | Tool output |
|---|---:|---:|---:|---:|
| Compact `edit_clojure` | yes | 23.053 s | 1 | 175 B |
| Native bounded read + `apply_patch` | yes | 47.416 s | 2 | 1,869 B |

The compact route was 24.363 seconds faster, or **2.06x end-to-end**. It made
one first-attempt mutation call, performed no source read, and received terminal
verification. Native correctly read the bounded owner block, then emitted one
successful patch. Compact also emitted 480 output tokens versus native's 1,533.

The first harness pass mislabeled both exact results as incorrect because a
legacy scorer inferred mutation tasks from a `*-edit` name suffix. The final
SHA in both arms already matched the capsule's expected SHA. Commit `d35081c`
removed that coupling: membership in the validated edit portfolio now defines
the scoring semantics. The corrected rerun is the table above.

This canary proves the proposed mechanism: when the complete owner set is the
decision, structural addressing removes the read needed to manufacture patch
context. It does not yet prove a representative 2x product advantage because
the source is synthetic and the deleted block is contiguous. The next gates
are replicated pairs and a production-derived extraction-shaped replay.

### Three-pair replication: stable advantage, not stable 2x

Result directory:
`/tmp/clj-surgeon-historical-delete-replicated-20260824`

| Pair | Compact | Native | Paired result |
|---:|---:|---:|---:|
| 1 | 60.758 s | 38.462 s | native 1.58x faster |
| 2 | 23.127 s | 30.155 s | compact 1.30x faster |
| 3 | 19.673 s | 39.622 s | compact 2.01x faster |

All six runs were exact and first-attempt successful. Compact used one tool
round trip with zero discovery reads in every trial. Native used one bounded
read and one patch in every trial. Compact median wall was 23.127 seconds;
native median wall was 38.462 seconds. The median result is therefore a
15.335-second saving, or **1.66x end-to-end**, not 2x.

The 60.758-second compact outlier completed with the same one-call route and
only 457 output tokens, which implicates model/service deliberation variance
rather than extra editor work. The result is encouraging because the route and
correctness are extremely stable, but it reinforces the correct claim boundary:
one lucky 2.06x canary is not a replicated 2x result. Production-derived task
shapes remain the decisive next test.

## A real extraction cleanup exposed and paid for a missing address

The next capsule derives from `sessionize-sched-killer` commit `7c71c9f7`,
which extracted development handlers without behavior changes. The public
fixture anonymizes function bodies but preserves the source-namespace cleanup
topology: one require rewrite, four route rewrites, four noncontiguous owner
deletions, and attached comments. The target namespace already exists, so this
capsule measures the post-move cleanup decision rather than file creation.

The first compact caller tried the two obvious namespace selectors,
`within.form=sample.server` and `within.form=ns`. Both refused before mutation
with `change-owner-mismatch`. Native completed exactly in 32.622 seconds. This
was useful failure: the compact address algebra exposed named Vars but not the
equally fundamental namespace form.

Commit `e10786d` added the obvious address:

```json
{"within":{"namespace":"sample.server"}}
```

Missing or mixed `form`/`namespace` locations refuse before compilation. A
behavior-level regression proves a namespace require edit, named-form edit, and
owner deletion commit and undo together. The complete 512 MiB MCP gate passed
195 tests and 1,615 assertions.

The next caller used the new address correctly on its first attempt and
committed all nine edits in 30.436 seconds. Its only mismatch was presentation:
the task supplied the replacement require as a one-line semantic form, so the
editor faithfully emitted it on one line while native learned the existing
multiline style from its required read. The harness had claimed exact
before/after source was supplied when only semantic form identity was supplied.

After the capsule supplied the exact multiline replacement source to both
arms, the exact-byte canary was:

Result directory:
`/tmp/clj-surgeon-dev-extraction-cleanup-canary3-20260825`

| Same Sol/high task boundary | Exact | Wall | Tool round trips |
|---|---:|---:|---:|
| Compact `edit_clojure` | yes | 26.261 s | 1 |
| Native full-file read + `apply_patch` | yes | 33.475 s | 2 |

Compact saved 7.214 seconds, or 21.5% wall, for a **1.27x** end-to-end win.
This is representative evidence that the mixed compact transaction helps on a
real extraction-cleanup shape. It is not a 2x result. A larger real extraction
is the next honest test of the hypothesized crossover: native patch generation
grows with moved/deleted source, while a supplied exact owner list remains
constant-size at the caller/tool boundary.

## The real extraction-scale canary crossed 2x

The higher-scale capsule derives from `sessionize-sched-killer` commit
`557684689060ec87ee16b4faaa6558fe9081e7c6`, which extracted public CFP handlers
and removed 441 lines from `server.clj`. The frozen public replay retains a
485-line source, 22 exact obsolete owners with attached comments, one namespace
require rewrite, seven route rewrites, and 30 total edits. Bodies are anonymized
to avoid publishing private product source, but owner count, cleanup scale, and
decision topology come from the real commit.

Result directory:
`/tmp/clj-surgeon-public-cfp-cleanup-canary-20260825`

| Same Sol/high task boundary | Exact | Wall | Tool round trips | Source/tool output | Input tokens | Output tokens |
|---|---:|---:|---:|---:|---:|---:|
| Compact `edit_clojure` | yes | 37.903 s | 1 | 175 B | 47,496 | 1,144 |
| Native read + `apply_patch` | yes | 83.794 s | 2 | 19,379 B | 131,950 | 3,157 |

Compact saved 45.891 seconds and completed **2.21x faster end-to-end**. Both
routes were exact and first-attempt successful. Compact made no discovery read;
it stated the 30-edit decision once and received one terminal atomic receipt.
Native read the 485-line file and then generated one large deletion patch.

This is the first production-derived task scale where the complete-wall result
crosses 2x. It supports the predicted complexity boundary:

```text
compact caller payload  ~= O(owner names + small literal rewrites)
native model work       ~= O(source read + changed source rendered as patch)
```

The server kernel is not the source of the speedup. The interface prevents
hundreds of lines from entering and leaving the autoregressive loop. The result
is still one canary; three alternating, exact-byte-gated pairs are required
before calling the 2x advantage replicated.

### Three-pair replication crossed 3x

Result directory:
`/tmp/clj-surgeon-public-cfp-cleanup-replicated-20260825`

| Pair | Compact | Native | Paired result |
|---:|---:|---:|---:|
| 1 | 36.995 s | 61.088 s | compact 1.65x faster |
| 2 | 37.687 s | 117.103 s | compact 3.11x faster |
| 3 | 35.139 s | 135.883 s | compact 3.87x faster |

All six final files matched the exact expected bytes. Compact won every pair
and was remarkably stable: its three runs spanned only 2.548 seconds. Compact
median wall was 36.995 seconds versus native's 117.103 seconds. The median
result is an 80.108-second saving, **68.4% lower wall time**, or **3.16x
end-to-end**.

The route difference remained the predicted one. Each compact caller sent the
complete 30-edit decision in one `edit_clojure` call, performed no source read,
and received a 175-byte terminal receipt. Native read the 485-line source and
then constructed a large patch. Median compact usage was 48,061 input tokens
and 1,149 output tokens; median native usage was 74,738 input tokens and 4,912
output tokens.

One native caller needed two guarded patch attempts after its first attempt
refused without mutation on a blank-line boundary mismatch. Codex did not emit
that refusal as a JSON `file_change` event; it appeared only in router stderr.
The original row therefore undercounted native failed mutation actions as zero.
The harness now counts this durable stderr evidence, and its self-test locks in
that behavior. The timing itself always included the failed attempt, so this
audit does not change the 3.16x wall-time result.

This is the first replicated evidence that the compact transaction can exceed
the 2--5x target's lower bound on a production-derived change shape. It is not
a claim that structural editing is universally 3x faster: the decision was
fully supplied, bodies were anonymized, and the capsule measures source cleanup
after extraction rather than discovery, new-file creation, or semantic design.
It does show a genuine product boundary where native patching pays O(source +
rendered deletion) model cost and compact structural intent avoids it.
