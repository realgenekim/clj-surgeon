# Captain's Log: will the agent choose the scalpel?

The next question is not whether clj-surgeon *can* perform an exact edit. It
can. The more interesting product question is whether a clean coding agent,
given a fair choice, prefers it over the text tools it already knows.

That distinction matters. A benchmark that says “use clj-surgeon” measures
instruction following. A benchmark that hides the executable on `PATH`
measures accidental discovery. Neither tells us whether the interface has
become the agent's preferred instrument.

The clean experiment has three separate moments:

1. **Discovery:** give only the desired outcome. Does the agent find an
   unadvertised structural CLI?
2. **Choice:** add only “The clj-surgeon CLI is installed and available.” Does
   the agent voluntarily choose it over shell readers and line patches?
3. **Onboarding:** install the normal skill. Does the complete product teach a
   correct one-shot route without task-specific prompting?

We almost mixed those moments into one 48-cell factorial. An independent
critique caught the confounds. A version-matched skill changes policy and
initial context, not just capability. Naming `[:partition-all 2]` against a
pre-feature binary advertises an impossible operation. Asking the agent to
“choose the fastest route” telegraphs the desired answer. Those runs might be
interesting diagnostics, but they cannot establish voluntary preference.

The corrected primary study uses only outcome-only and neutral-awareness
prompts, four repetitions each, in isolated neutral repositories. The exact
same real-program-derived fixture is overlaid into pre and post environments.
The version-specific checkout contributes only its executable. Skills and
operation hints are separate follow-on studies and are labeled accordingly.

The write control is especially revealing. A familiar line edit can look
cheap because the command is short:

```text
search → print context → patch → print/diff/hash again
```

But the apparent single edit hides several proof obligations. Did the search
identify syntax rather than a comment or string? Was the intended peer chosen?
Did the patch touch the duplicate elsewhere? Did source change between read
and write? Is the resulting file parseable? Did verification read the same
bytes that were written?

The structural route makes those obligations one artifact:

```text
select + transform → reviewable hash-fenced plan → explicit apply → verified receipt
```

The plan is still non-writing. Application remains a later command because
human/model review is a real consent boundary, not latency to optimize away.
“One-shot editing” therefore means one expression states the complete
selection and transformation. It does not mean planning and mutation are
silently fused.

This exposes a product-language opportunity. `:q` is already both getter and
planned updater, but its name advertises query more strongly than edit.
`:replace-subform` advertises mechanics rather than intent. In the Unix-shaped
surface—`:ls`, `:cat`, `:grep-form`, `:q`—the obvious missing word is `:edit`.

The lean hypothesis is deliberately small:

> If `:edit` names the existing terminal lens-updater contract, a clean agent
> will discover the safe structural write path with less help and no new
> mutation semantics.

An MVP must not add arbitrary evaluation, fuzzy selection, multiple edits, or
an auto-apply shortcut. It should reuse the exact query grammar, plan schema,
hashes, diff, replay address, and verified executor already in production. A
read-only pipeline under `:edit` should refuse with a concise remedy to use
`:q`; zero and multiple targets should preserve their current structured
refusals. The apply command must stay visually and temporally separate.

The prototype earns permanence only through behavior:

- blank agents choose it after neutral awareness;
- correct edit plans take fewer source-bearing calls than the observed text
  route;
- agents do not need exact query syntax pasted into the prompt;
- plan review and apply remain separate;
- no benchmark gain comes from skipping verification;
- the name reduces help and recovery detours rather than merely adding another
  alias to memorize.

This is lean startup for an agent tool. The MVP is not a miniature product
roadmap. It is the cheapest truthful interface change that can falsify the
adoption hypothesis. The clean Codex transcript is the customer interview;
exact file bytes and receipts are the retention metric.

## Prototype checkpoint

The implementation remained as small as the hypothesis. `:edit` is a primary
write-category command with three required arguments: `:file`, the existing
lens `:query`, and `:plan-out`. It adds no query step and no plan field.
Successful node and span requests return the same plans as `:q`.

The façade adds four guards that a query-oriented entrypoint did not promise:

- a getter-only pipeline refuses and points reads back to `:q`;
- unknown consent-shaped flags such as `:force`, `:yes`, or `:apply` refuse
  instead of being ignored;
- the plan path may not canonically resolve to the source path;
- plan artifacts use the same fail-closed atomic writer as source application,
  so a failed plan cannot truncate the previous review artifact.

The focused prototype suite passes 6 tests and 51 assertions. The existing help
and lens suites also pass: 70 tests and 657 assertions. clj-kondo reports no
errors in the changed production and test namespaces. These facts establish
the safety contract, not product value. The name earns permanence only if the
neutral-awareness Codex study changes voluntary behavior.

## First voluntary-choice result: the noun did not win

The first valid study ran four independent repetitions in each stable/prototype
× hidden/aware cell. The only prompt difference in the aware condition was:
“The clj-surgeon CLI is installed and available.” No skill or operation name
was supplied.

| Context | Stable exact | Prototype exact | Stable median calls | Prototype median calls |
|---|---:|---:|---:|---:|
| Outcome only | 1/4 | 0/4 | 5 shell + 1 file change | 3 shell + 1 file change |
| Neutral awareness | 4/4 | 4/4 | 6 shell | 7 shell |

Awareness, not the new command, produced the large correctness gain. Seven of
eight hidden-tool agents used text reads plus a file-change patch and changed
an unrelated byte: the final blank line disappeared. All eight aware agents
used guarded structural plans and preserved every byte.

The prototype did not meet its adoption gate:

- `:edit` appeared in only 1 of 4 aware prototype runs;
- it was the first source-bearing operation in 0 of 4;
- that agent first used `rg`, then `:q`, then `:edit`;
- the other agents chose `:replace-subform` twice and `:q` once;
- prototype medians rose from 6 to 7 shell calls, 4.6 KB to 8.3 KB of output
  from source-bearing commands, 39.2 s to 46.0 s wall time, and 109.2k to
  111.6k cumulative input.

This is a clean negative result for “add the obvious noun.” The stable global
help already exposed two credible editing routes. Adding a third fragmented
choice and made the help scan larger. The lone `:edit` user proved that the
facade works safely, but not that it is preferred.

One fair iteration remains before removal. The global quick start taught `:q`
but did not show `:edit`, so agents who scanned only the orientation page could
not copy the new one-shot spelling. The v2 experiment adds exactly one global
quick-start example and one general rule: when a named form plus exact sibling
anchor and replacement are already supplied, the edit plan may be the first
source-bearing call. It does not alter the task prompt, skill, query grammar, or
planner. Four new neutral-awareness sessions will test whether this small
onboarding correction changes voluntary choice. If it does not, remove the
facade rather than adding more persuasion.

## v2: help taught adoption, but one action too late

Four new neutral-awareness sessions compared the first prototype with only the
global-help correction. Exactness remained 4/4 in both versions. The help
example moved eventual `:edit` use from 1/4 to 4/4, proving that a single copied
route was sufficient syntax teaching.

It did not make the route one-shot:

- first-source `:edit` remained 0/4;
- every v2 agent ran `rg` in the same first command as `clj-surgeon --help`;
- every agent then opened `edit --help` before planning;
- median source-bearing commands did not fall;
- median output from source-bearing commands increased from 4.7 KB to 13.2 KB;
- one agent spent 12 shell calls, 255k cumulative input tokens, and 106 seconds
  after trying to reconstruct Git-based verification in a neutral workspace.

The apparent paradox is temporal. Global help cannot teach the agent until the
agent chooses to call it. Clean agents hedged that latency by launching their
familiar `rg` inspection concurrently. A better global example cannot travel
backward in the event stream.

The next legitimate coaxing level is the installed skill. It is visible before
the first action and is the repository's normal onboarding surface. The v3
skill makes one distinction: `:q` reads; `:edit` plans when the supplied
relationship and replacement are already exact. If that does not make the
plan the first source-bearing command and remove help/text detours, the facade
has no remaining evidence-based iteration.
