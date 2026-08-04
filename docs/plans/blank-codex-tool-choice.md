# Blank Codex Tool-Choice Benchmark

**Status:** Ready to run

## Decision

Measure whether a clean Codex session voluntarily chooses clj-surgeon over
ordinary shell readers and line-oriented edits, and determine the least prompt
support needed for that choice to improve the outcome.

This is a choice experiment, not a compliance demonstration. A run is not
credited merely because its prompt names the tool. Correctness is the gate;
after that, compare calls, tokens, elapsed time, source bytes, and detours.

## Questions

1. Does an outcome-only agent discover clj-surgeon without being told?
2. Once told that both routes are acceptable, which route does it choose?
3. Is normal skill installation enough to produce correct one-shot behavior?
4. Does the agent require the exact operation name, `[:partition-all 2]`?
5. When the structural route wins, what does it save over the text/patch route?

## Primary frozen treatments

Every run receives the same task outcome. Only onboarding changes.

| Level | Context | Additional information |
|---:|---|---|
| 0 | `no-skill` | None. The CLI is on `PATH`, but no skill or tool hint is visible. |
| 1 | `aware-no-skill` | Adds only: “The `clj-surgeon` CLI is installed and available.” |

Level 1 is the direct answer to “which would blank Codex choose?” It does not
ask for the fastest route, describe the tool, or suggest an operation. That
would create demand characteristics rather than measure voluntary choice.

Run two follow-on studies and report them separately:

- `matched-skill`: the normal product surface, with the version-matched skill;
- `partition-hint-no-skill`, post only: says that `[:partition-all 2]` exists,
  without giving a complete query.

The skill study measures the integrated product and policy-guided behavior, not
blank voluntary choice. The hint study measures residual syntax coaxing.

## Frozen versions

- `0426efe`: structural lens and span operations, before `[:partition-all N]`.
- `e19c2d1`: fully integrated `[:partition-all N]`, help, README, and skill.

Never run the post-only operation hint against pre. That would measure failure
recovery, not feature performance.

## Frozen tasks

1. **Case inventory:** enumerate eight test/result pairs plus the optional
   default in `route-event`, preserving exact result source.
2. **Nested-cond inventory:** enumerate only the seven outer guard/result pairs
   beginning with `(nil? actor)` in `classify-request`; retain the nested `cond`
   as one result.
3. **Binding inventory:** enumerate the eight top-level name/initializer pairs
   in the binding vector inside `prepare-request`, excluding later uses.

These inventories test the bounded structural read across three distinct
shapes. Keep the original unknown-source nested-`cond` prompt as a separate
robustness probe because it also tests owner selection.

A separate edit-choice control uses the existing exact `case-edit` fixture. It
answers the broader line-patch-versus-reviewed-plan question without mixing a
different mutation capability into the partition benchmark.

## Repetitions and order

Run four independent sessions per task × context × version cell: 48 primary
sessions. Alternate pre/post order within each block. Each run receives an isolated
workspace, Codex home, shell startup, and version-specific CLI.

If a pilot reveals a harness or scoring defect, discard that result directory,
fix the harness, and restart from an empty directory. Never reinterpret a
failed scorer after seeing desirable results.

## Evidence

Record for every run:

- exact and normalized correctness;
- selected route and every command;
- clj-surgeon invocations;
- `:q`, `[:partition-all 2]`, help, and text-reader use;
- plan generation, separate apply, and post-write verification;
- shell calls and atomic commands;
- source and total tool-output bytes;
- cumulative, cached, uncached, output, and reasoning tokens;
- elapsed wall time;
- any manual sibling counting, offset reconstruction, or syntax recovery.

## Interpretation

Report adoption and performance separately.

- **Discovery gain:** Level 0 tool-choice rate.
- **Naming gain:** Level 1 minus Level 0 tool-choice rate.
- **Onboarding gain:** Level 2 correctness and one-shot rate versus Level 0.
- **Syntax-coaxing residue:** improvement from Level 2 to Level 3.
- **Execution gain:** within correct runs, structural-route medians versus
  text/patch-route medians for calls, tokens, bytes, and time.

Do not average incorrect runs into a speed claim. Do not call a run one-shot if
it read help, printed an owner form, retried a query, manually counted siblings,
or repaired a failed command.

## Success threshold

The tool earns “preferred ideal path” status only if:

1. normal skill installation yields correct behavior without task-specific
   syntax in at least nine of twelve runs across the three inventories;
2. the structural route removes at least one source-reading or reconstruction
   call from the observed text route;
3. correct structural runs do not increase median uncached input or source
   output enough to erase that call reduction;
4. the edit control preserves the non-writing plan, separate apply, and
   verification boundary;
5. failures produce bounded, actionable recovery rather than widened reads or
   unsafe writes.

If Level 1 chooses the old route and Level 2 succeeds, the feature is good but
not self-discovering: the skill is part of the product. If Level 3 is required,
the skill or help still hides the decisive operation. If even Level 3 fails,
the API has not earned further promotion.
