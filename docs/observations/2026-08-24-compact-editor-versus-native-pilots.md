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

