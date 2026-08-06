# Captain's Log: the same trigger split the callers

<!-- agent-usage-window-end: 2026-08-06T16:28:00.284000Z -->

The previous study ended with a simple diagnosis: clj-surgeon was visible to
Claude, but a passive skill description never activated it. We changed the
description to an imperative:

> Invoke before using Read, Edit, grep, sed, or cat on existing Clojure,
> ClojureScript, or CLJC files.

The next twelve hours produced a clean replication and a surprising split.
Codex selected the skill in every relevant session and used the binary 127
times. Six Claude implementation agents saw the exact new description. None
loaded the skill or invoked the binary.

The wording changed. Claude's behavior did not. That moves the diagnosis one
layer deeper: metadata can advertise a workflow, but Claude still needs an
active repository rule or tool hook to interrupt its native Read/Edit loop.

The project and domain context remain anonymized. Counts come from a versioned
machine receipt that contains no transcript prose or workspace paths.

## Window and method

The exact observation window was:

| Boundary | UTC | Pacific |
|---|---|---|
| Start | 2026-08-06 04:20:51 | 2026-08-05 21:20:51 PDT |
| End | 2026-08-06 16:28:00.284 | 2026-08-06 09:28:00.284 PDT |

The start came from the machine marker in the prior completed study. The end
is the timestamp of the user request that initiated this review, so the study's
own collection work is excluded.

The collector found five Codex sessions and ten Claude transcripts with events
in the window. Three Codex sessions and six Claude transcripts performed
relevant Clojure work. The Claude population consisted of six implementation
agents linked to a continuing parent session; the parent coordinated them but
did not directly read or edit Clojure source in this window.

The providers expose different tool schemas. Native action counts are therefore
descriptive within a provider, not a direct cross-provider performance score.
Actual clj-surgeon command lines share one syntax and are directly comparable.

## Scoreboard

| Behavior | Codex | Claude |
|---|---:|---:|
| Relevant Clojure sessions | 3 | 6 |
| New imperative trigger visible | 3 / 3 | 6 / 6 |
| Skill loaded | 3 / 3 | **0 / 6** |
| Skill loads | 7 | 0 |
| clj-surgeon binary calls | **127** | **0** |
| Outer tool actions containing Surgeon | 76 | 0 |
| Native Clojure Read | not directly exposed | 48 |
| Native Clojure Edit | `apply_patch` counted separately | 85 |
| Native Clojure Write | not directly exposed | 6 |
| Native Clojure-related shell actions | 27 | 66 |
| Native Clojure `apply_patch` actions | 6 | n/a |
| Relevant tool output | 1,191,608 chars | 1,155,338 chars |

Codex produced 447,059 characters of output from the 76 outer actions that
contained clj-surgeon calls. This is not a source-savings measurement: those
actions include EDN metadata, plans, diffs, refusals, and batched invocations.
It does establish that adoption alone did not make the sessions small.

## Did adoption make Codex slower?

The field logs can bound direct cost, but they cannot supply the missing
counterfactual. Across all 76 Surgeon-bearing outer actions:

| Direct tool-wall measure | Observed |
|---|---:|
| Median | 645 ms |
| 90th percentile | 2.303 s |
| Maximum | 11.048 s |
| Cumulative | 92.497 s |

Six native `apply_patch` actions in the same sessions had a 116 ms median and
924 ms cumulative wall. The 5.6-times median difference is real, but it is not
an equivalent-work comparison. A Surgeon action can launch several binary
calls and performs parsing, selection, hashing, planning, reparsing, and
receipt generation; a native patch applies caller-supplied bytes.

Four completed Codex turns used Surgeon in this window:

| Turn wall | Surgeon calls | Outer Surgeon actions | Direct Surgeon wall | Share of turn |
|---:|---:|---:|---:|---:|
| 9.87 min | 1 | 1 | 0.344 s | 0.06% |
| 9.22 min | 14 | 6 | 34.526 s | 6.24% |
| 13.33 min | 39 | 31 | 23.070 s | 2.88% |
| 17.02 min | 21 | 17 | 16.574 s | 1.62% |

Together, those turns took 49.45 minutes. Direct Surgeon execution consumed
74.514 seconds, or 2.51%. The binary is therefore not the dominant wall-clock
cost in these field tasks.

That does **not** prove adoption made the tasks faster. Every outer action can
cause another model decision, tool round trip, context update, and recovery
choice. Thirty-one precise calls can cost far more in agent-loop latency than
their 23 seconds of subprocess wall. Field logs show where time went; they do
not show how long the same task would have taken with a native route.

The existing clean Codex benchmark supplies the causal evidence we do have:

| Matched task | Current Surgeon | Native control | Surgeon delta | Correctness gate |
|---|---:|---:|---:|---|
| Named-form read | 22.709 s | 23.949 s | 5.2% faster | 4 / 4 both |
| Semantic-form read | 28.384 s | 27.347 s | **3.8% slower** | 4 / 4 both |
| Structural search | 24.222 s | 40.627 s | 40.4% faster | 4 / 4 both |
| Guarded edit | 52.832 s | not comparable | — | Surgeon 4 / 4 exact; native 0 / 4 exact |

This is the honest answer: current Surgeon is not universally faster. It tied
or won on two read shapes, lost narrowly on one easy read, and won decisively
when syntax mattered. The edit control cannot earn a speed win because it
failed the exact-byte gate in every run.

### First real-feature replay: Surgeon was one-third slower

The first prompt-level replay used a landed mixed Clojure/JavaScript Explorer
feature. Both lanes started from the same parent commit, received the same
consolidated requirements, ran concurrently, and produced behaviorally correct
results. Both passed the neutral historical browser oracle: 36 tests out of
36.

| Measure | Surgeon | Native control | Surgeon delta |
|---|---:|---:|---:|
| Full task wall | 562 s | 425 s | **137 s slower (32.2%)** |
| Shell commands | 39 | 19 | 20 more (105%) |
| Input tokens | 3,123,216 | 1,726,420 | 81% more |
| Output tokens | 15,669 | 9,562 | 64% more |
| Tool-output characters | 193,611 | 708,653 | **73% fewer** |

This is a red result. A roughly one-third wall penalty is too large to dismiss
as a few seconds of CLI syntax discovery. Surgeon compressed raw tool output,
but the caller spent that advantage on more model/tool turns and a larger
context. Compact reads did not produce a compact task loop.

The verification scopes differed. The Surgeon lane chose the full Clojure
suite: 219 tests and 1,589 assertions. The native lane chose a focused Clojure
run: 73 tests and 1,015 assertions. Both ran direct browser behavior checks,
and both later passed the same 36-test browser oracle. This single replay is
therefore suggestive, not a final causal estimate. It still identifies the
product problem correctly: the default Surgeon workflow must reduce total
turns, not merely source bytes. The next replay should fix the verification
command in the prompt and keep the same correctness oracle.

### Second real-feature replay: smaller patch, larger time loss

The next landed Explorer prompt favored structural locality but not task speed.

| Measure | Surgeon | Native control | Surgeon delta |
|---|---:|---:|---:|
| Full task wall | 655 s | 414 s | **241 s slower (58.2%)** |
| Shell commands | 57 | 19 | 38 more (200%) |
| Input tokens | 3,376,706 | 2,029,504 | 66% more |
| Output tokens | 16,470 | 12,975 | 27% more |
| Tool-output characters | 159,128 | 1,216,226 | **87% fewer** |
| Changed lines, including new tests | 98 | 181 | **46% fewer** |

The smaller Surgeon patch was initially exciting. Structural reads helped the
caller reuse existing owners and edit a narrower surface. That did **not** make
it automatically better. Native also changed a second Explorer renderer and
replaced the existing substring traversal check with canonical root
containment. Those extra lines bought broader surface coverage and stronger
path safety. Patch size measures preservation surface; it is not a standalone
quality score. The correctness oracle prevented a premature quality claim. The landed browser suite accepted
the native `scrollTo` implementation and rejected Surgeon's alternative
`scrollBy` implementation, even though both requested smooth browser motion.
Both lanes exposed an absolute server path on hover instead of the landed
project-relative path. Native also omitted the landed visible-overflow hover
behavior. Neither lane reproduced the complete landed contract.

The speed result is still actionable. Surgeon's 57-command loop contained
three invalid line-as-form reads, a large `:ls` recovery, repeated namespace
header reads, two broad searches, twelve plan/edit/apply inspections, and ten
test-runner or test-environment probes. Direct tool execution was not the main
cost. Extra model/tool turns were.

The first hill-climb treatment should therefore change routing before adding
new syntax:

- cap Surgeon discovery reads at three for this bounded task;
- use native tools for JavaScript, tests, and broad multi-form rewrites;
- use one guarded `:edit` only for a known literal Clojure replacement;
- avoid computed plan/apply ceremony when a normal patch is clearer;
- provide the exact verification commands and dependencies to both lanes;
- test the observable hover/path contract, not one implementation primitive.

If this prompt-only treatment wins, move the rule into the installed skill. If
it still loses, add or hide a product operation only for a repeated command
class demonstrated by the trace.

### The first hill climb beat native on time-to-correct

The prompt-only treatment removed most Surgeon ceremony. It made the expected
relative-path and hover behavior explicit, supplied the two verification
routes, capped Surgeon source reads at three, and allowed native edits when a
computed plan/apply sequence would cost more turns.

| Correct-result measure | Surgeon | Native control | Surgeon delta |
|---|---:|---:|---:|
| Initial agent wall | 295 s | 255 s | 40 s slower initially |
| Required repair wall | 0 s | 228 s | 228 s avoided |
| **Time to correct working tree** | **295 s** | **483 s** | **38.9% faster** |
| Commands to correct tree | 14 | 25 | 44% fewer |
| Input tokens | 876,299 | 1,374,800 | 36% fewer |
| Output tokens | 7,486 | 11,977 | 37% fewer |
| Tool-output characters | 84,309 | 1,210,036 | **93% fewer** |
| Final changed lines | 130 | 201 | 35% fewer |

The native lane's initial 255-second result was not eligible: it stopped with
an unmatched delimiter in `views_test.clj`. A clean repair caller needed 228
more seconds, discovered additional behavioral failures, repaired them, and
then passed both Clojure and browser oracles. The Surgeon tree passed the same
external Clojure oracle and all 36 browser tests.

This establishes a win on the explicit replay contract, not universal design
superiority. The Surgeon result changed only the tree renderer exercised by the
prompt's tests and retained the existing raw-file traversal policy. Native
changed both tree and flat-list renderers and hardened raw-file containment.
Future prompts must state which Explorer surfaces and security properties are
required before patch size can support a design-quality judgment.

One Surgeon defect remains. Its final message claimed that the Clojure tests
passed even though the trace did not contain a successful post-repair rerun.
The external oracle proved the final tree correct, but the receipt was not
earned. Future replay prompts must require a machine-readable summary and zero
exit after the last edit. Time-to-correct and truthful verification are
separate gates.

The successful routing rules now belong in the repo skill: Surgeon is a lens,
not a quota; three structural reads is the default bounded-feature budget; use
one guarded literal edit when it saves a turn; use a normal patch for clearer
multi-form or computed changes; and do not invoke Surgeon after tests merely to
prove parsing.

The product rule remains:

> Correctness and preservation are gates. Among correct routes, compare full
> task-turn wall first—not binary wall—then calls, source output, and tokens.

If full structural routing is more than 10% slower across four or more matched
correct repetitions and supplies no measured safety or context benefit, remove
that routing rule. Adoption is an intermediate metric, never the keep gate.

### A good native reroll narrowed the win

The native repair result above was a bad draw, so the native lane was rerun
from the original parent with the same prompt and oracle. It completed a
correct tree in 314 seconds with nine commands. The treated Surgeon lane took
295 seconds. The defensible delta is therefore a 6% Surgeon win against a good
native draw, not only the 39% win against native time-to-repair.

| Fresh correct result | Surgeon | Native reroll | Surgeon delta |
|---|---:|---:|---:|
| Wall | 295 s | 314 s | 6% faster |
| Commands | 14 | 9 | 5 more |
| Input tokens | 876,299 | 995,174 | 12% fewer |
| Tool-output characters | 84,309 | 1,125,493 | 93% fewer |

This result is encouraging but close. Surgeon won by reducing context and
avoiding repair, not by issuing fewer commands. A second feature was needed to
test transfer.

### The first transfer failed and exposed dishonest vocabulary

On the other mixed Explorer feature, the revised skill still lost: 260 seconds
against 182 seconds native, or 43% slower. Both results were correct. The trace
showed why the routing did not transfer:

1. The caller guessed `:grep-form :pattern 'generated|explorer|shortcut|scroll'`.
2. The refusal recommended the same value under `:match`.
3. The structural matcher correctly found nothing because the value was a
   regex-shaped symbol, not a Clojure form pattern.
4. The caller then ran a broad native search that returned 413,097 characters.

The caller paid both discovery costs. The operation name had primed Unix regex
semantics, and the refusal reinforced the mistake. Two quick guesses were not
the problem; five seconds of syntax discovery would have been acceptable. The
problem was the unbounded recovery and duplicated context.

The product correction is now explicit:

- `:match-form` is the preferred structural name;
- `:grep-form` and `:find-subform` remain compatibility aliases only;
- `:match-form :match` accepts one EDN form pattern, with `_` matching exactly
  one subtree;
- a guessed `:pattern` containing regex alternation recommends a bounded `rg`
  command instead of feeding the value back into structural matching.

This keeps structural search honest. A future compact `:grep` may map text
matches directly to enclosing Clojure owners, but regex must not silently enter
the structural matcher and weaken its syntax-only guarantee.

### Guarded editing reached one source-bearing action

The next repeated cost was plan-file bookkeeping. The existing `:edit
:expect` contract already had the necessary safety: the caller declares the
exact before-state; the tool selects exactly one form, fences the source hash,
writes atomically, reparses the whole file, and verifies the read-back hash.
Requiring `:plan-out` for that route did not create another judgment boundary.

The surface now makes the artifact optional:

```text
:edit + :expect                 -> apply now, verified receipt
:edit + :expect + :plan-out     -> same, retain audit artifact
:edit without :expect           -> plan only; :plan-out required
:edit with computed transform   -> plan, review, separate apply
```

A clean Codex caller then received an unprimed instruction to make the known
literal edit safely with the fewest actions. It loaded the installed skill and
used exactly one source-bearing command:

```bash
clj-surgeon :op :edit :file src/bench/pair_view.clj \
  :expr "(-> (form 'route-event) (match :finish) right (match :status) right (match :done) (replace :complete))" \
  :expect :done
```

| Forward probe | Result |
|---|---:|
| Total actions | 2: skill read + edit |
| Source-bearing actions | 1 |
| Wall | 30 s |
| Input tokens | 45,375, including 38,144 cached |
| Output tokens | 781 |
| Surgeon help, outline, reads, plans, or separate applies | 0 |
| Exact diff | 1 line, `:done` to `:complete` |
| Receipt | atomic write, whole-file parse, matching read-back hash |

The attached comment, audit payload, and every unrelated byte survived. This
is the desired comparative advantage: native-patch action count with stronger
structural selection and verification. The 30-second result is also 43% below
the earlier 52.832-second four-run guarded-edit median, but the prompts and
product versions differ, so that percentage is suggestive rather than a
matched causal estimate.

The tool is not finished. Text discovery still leaves the structural surface,
and computed edits still earn their plan/review ceremony. But the common known
literal edit no longer pays that ceremony, and the clean caller found the route
without being told its command name.

## Codex adopted the whole read/edit loop

Codex did not merely mention the tool. All three relevant sessions loaded the
skill and invoked the binary:

| Operation | Calls |
|---|---:|
| `:cat` | 67 |
| `:edit` | 29 |
| `:ls` | 18 |
| `:grep-form` | 7 |
| `:replace-subform!` | 3 |
| `:help` | 2 |
| `:find-subform` | 1 |
| `:xray` | **0** |

The write route shows genuine product adoption. Twenty-one outer actions used
`:expect`, twenty-five named `:plan-out`, and three applied reviewed plans with
`:replace-subform!`. The high `:expect` count matters: most exact edits used the
new one-call guarded path rather than paying plan-review-apply ceremony for a
known before-state.

Codex also used structural vocabulary rather than only top-level form names:
23 actions used `match`, five used a physical line selector, one used `right`,
and one used `transform`. That is real use of the Clojure-native algebra.

## Codex raised a structural-compression question

One diagnostic session issued 66 binary calls in about an hour: 44 `:cat`, 14
`:ls`, two `:grep-form`, three edits, one apply, one `:find-subform`, and one
help call. Several outer actions launched four or five form reads together.
A later twelve-minute burst traversed many owners with alternating `:ls` and
`:cat` calls.

This is better than dumping complete large namespaces, but call counts alone do
not prove disadvantage. The task may genuinely require many forms. The pattern
does raise a concrete question: did precise reads reduce total task turns and
tokens, or did the caller reconstruct a broad namespace one form at a time?

The missing operation is conspicuous: zero X-rays across 127 calls. Some of the
work was runtime and ownership tracing where literal forms were appropriate.
The count does not prove that every batch should have been computed. It does
show that Codex never considered moving an inventory, aggregation, or
relationship calculation into the tool even while issuing dozens of reads.

The skill needs a stopping rule, not another reader:

> Do not shotgun `:ls` and `:cat` across a subsystem. After two or three form
> reads reveal a repeated inventory or relationship question, compute it in one
> bounded X-ray or return to a live semantic probe.

## Refusals did useful work, with one workflow lesson

Fifteen Surgeon-bearing actions returned structured refusal evidence. Parsed
types included:

| Refusal | Observed lesson |
|---|---|
| `:ambiguous-match` | Narrow the structural selector. |
| `:no-match` / `:form-not-found` | The caller's source assumption was wrong. |
| `:span-arity-mismatch` | A peer replacement would have changed structure. |
| `:invalid-edit-expression` / `:invalid-query` | The caller authored an invalid route. |
| `:missing-arguments` | Help or routing did not supply a complete call. |
| `:apply-failed` | A saved plan no longer matched current file bytes. |
| `:file-read-failed` | The selected file was unavailable at invocation time. |

The apply failure is the most reusable ethnographic finding. Planning multiple
independent edits against one file before applying the first leaves later plans
hash-stale. The refusal is correct; the caller workflow is wasteful. The skill
should say:

> For multiple edits to one file, plan and apply sequentially. Each successful
> apply invalidates other plans made from the prior file hash. Use one aggregate
> edit when the operations must be reviewed together.

Ten additional outer actions contained an unstructured nonzero or exception
signal without a parsed refusal type. Because one outer action can batch
several shell commands, this is a diagnostic count, not evidence of ten
independent binary defects. Future collector versions should resolve nested
receipts before grading those failures.

## Native patches did not disappear

The busiest Codex editing session made 60 Surgeon calls and five native
`apply_patch` actions that mentioned Clojure source. Three patches were mostly
additive, which is consistent with the intended boundary: adding forms,
comments, tests, or broad markup remains native-edit territory. One was a
two-line replacement and is a plausible structural fallback. The transcript
does not support labeling all five as failures.

This mixed route is healthy when chosen by shape:

```text
existing exact structure -> Surgeon
new forms or prose-heavy changes -> native patch
```

The product goal is not zero native tools. It is zero unnecessary source dumps,
zero model-computed bookkeeping, and guarded structural writes where the
relationship is already known.

## Claude replicated the activation failure

Claude's six relevant agents all received this exact listing:

> clj-surgeon: Invoke before using Read, Edit, grep, sed, or cat on existing
> Clojure, ClojureScript, or CLJC files.

Their combined behavior was:

| Native action | Calls |
|---|---:|
| Read | 48 |
| Edit | 85 |
| Shell referencing Clojure | 66 |
| Write | 6 |
| Skill load | **0** |
| clj-surgeon | **0** |

The description could not be more direct without becoming policy prose. The
result falsifies the hypothesis that stronger frontmatter alone would change
Claude's first action.

There was one independent improvement. Claude bounded more reads:

| Claude read behavior | Previous study | Current study |
|---|---:|---:|
| All native Clojure reads | 47 | 48 |
| Bounded reads | 9 | 18 |
| Unbounded reads | 38 | 30 |
| Bounded share | 19% | 37.5% |
| Read output | 442,423 chars | 596,701 chars |

The bounded share nearly doubled, but read output still rose by 35%. Different
tasks and file sizes prevent a direct performance conclusion. The defensible
claim is behavioral: Claude used offsets and limits more often, yet still paid
for 30 whole-file reads and never crossed into structural selection.

## Why the callers split

Both populations saw the same metadata. Their surrounding policy differed.

Codex sessions operated under mandatory skill-trigger instructions and
repository Clojure-routing rules. Claude saw a catalog entry but its target
repository guidance did not require the skill before native tools. Once a
Claude agent began with Read or Write, mechanically successful exact-string
Edit calls reinforced the old loop.

This is an inference from the transcripts and instruction surfaces, not a claim
about either model's internal reasoning. It predicts a falsifiable intervention:
put the routing rule in Claude's active repository instructions or a
PreToolUse warning, and measure the first source-bearing action again.

## The next experiment

Do not revise the skill description again. Hold it constant and compare four
fresh Claude repetitions per condition:

| Condition | Intervention |
|---|---|
| Metadata only | Current installed skill description. |
| Repository policy | Add the Read/Edit/grep/sed/cat rule to active `CLAUDE.md`. |
| Soft hook | Warn before native Read/Edit of existing Clojure source. |
| Control | Create a new Clojure namespace, where native Write should win. |

Measure skill loading, first source-bearing action, real Surgeon calls, bounded
and unbounded reads, source output, correctness, and wall time. Keep the task,
fixture, model, and expected bytes fixed.

For Codex, run a separate compression probe: give a subsystem inventory task
that can be solved either with many `:cat` calls or one X-ray. The gate is not
mere adoption. It is fewer source-bearing actions and lower output with equal
correctness.

### A small real-task replay, not a large factorial

The history and Git commits make a controlled experiment possible without
inventing synthetic work. Select three completed Codex turns whose final
messages name one landed commit. For each task, recover:

- the exact historical user prompt;
- the parent of the landed commit;
- the landed diff and relevant tests;
- the model and reasoning setting;
- the repository instructions active during the turn.

Replay each task once in two isolated temporary worktrees:

| Lane | Policy |
|---|---|
| Surgeon | Current installed skill and binary. |
| Native | Same prompt and instructions with only the Surgeon routing rule and executable removed. |

That is three tasks and six clean runs. Score exact diff or behavior, tests,
full `task_started` to `task_complete` wall, model/API time, direct tool wall,
reasoning turns, source-bearing calls, source output, and tokens. The historical
run remains field evidence; it is not a matched replicate because it carried
prior conversation context.

One run per lane is suggestive, not statistically decisive. Replicate only a
task whose result is close or contradictory. A large consistent difference
across three real tasks is enough to guide product work; an ambiguous task earns
three more repetitions. This preserves the negative-stage discipline without
turning every field observation into a research program.

## The study itself became a one-shot

This review repeated enough times to become product work. The repository now
owns a `study-agent-usage` skill and one command:

```bash
make study-agent-usage
```

The collector scans both history formats from the newest completed study
marker, emits a versioned JSON receipt, hashes session identities, excludes
transcript prose and workspace paths, and separates:

```text
skill visible -> activation trigger visible -> skill loaded -> binary invoked
```

Its self-test constructs both transcript formats and verifies marker discovery,
operation counting, native-action counting, and the privacy contract. The
first real run caught and fixed a Makefile defect that polluted JSON stdout
with the echoed command. The real receipt is now directly parseable.

## Verdict

The activation change worked for Codex and failed cleanly for Claude. That is a
valuable negative result, not a reason to add more words to the skill.

Codex now treats clj-surgeon as a normal Clojure read/edit surface. Its next
quality hill is restraint: fewer parallel `:cat` calls, deliberate X-ray use,
and sequential planning when several edits touch one file.

Claude's next hill is earlier. The skill must become active policy before the
first native source action. Until that happens, help text, aliases, X-ray
examples, and perfect refusal messages remain downstream of a boundary Claude
never crosses.
