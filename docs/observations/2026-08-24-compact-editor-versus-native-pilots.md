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
