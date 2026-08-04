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
