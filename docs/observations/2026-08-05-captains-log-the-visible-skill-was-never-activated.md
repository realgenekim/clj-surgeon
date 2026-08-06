# Captain's Log: the visible skill was never activated

<!-- agent-usage-window-end: 2026-08-06T04:20:51Z -->

A six-hour Claude Code history sample produced a clean negative result for
clj-surgeon adoption. Claude built a substantial local Clojure service, used
six implementation agents, and repeatedly read and edited growing namespaces.
The clj-surgeon skill was visible to every worker. Nobody invoked it.

This was not evidence that Claude tried clj-surgeon and rejected its API. It
was evidence that the skill never interrupted a familiar native-tool loop.
The next product problem is activation, not another alias.

The project and subject matter are anonymized here. The observations preserve
tool choices, timings, counts, and source shapes without retaining unrelated
domain context.

## Research question

The study asked:

> When Claude has clj-surgeon available during sustained real Clojure work,
> does it voluntarily use the structural read and edit surfaces? If not, what
> keeps the native workflow in place?

The question is narrower than "did the build succeed?" A productive session
can still reveal that a specialized tool failed to enter the workflow.

## Sample and method

The observation window was 2026-08-05 15:20:51 through 21:20:51 Pacific. The
history search found two main Claude sessions:

- one four-minute tail with no relevant Clojure work;
- one substantive session lasting about three and a half hours.

Session discovery excluded spawned-agent transcripts, as required by the
history-search procedure. After identifying the substantive parent session,
the behavioral analysis included its six linked implementation-agent
transcripts. All seven workers had Clojure source in scope.

The study counted native tool calls whose direct path ended in `.clj`, `.cljs`,
or `.cljc`. Bash calls were counted separately when their command referenced a
Clojure file. Character counts measure transcript payloads, not model tokens.
The token figure below is only a four-characters-per-token approximation.

## What Claude built

The session began with a small greenfield Clojure service and expanded it in
several committed phases. The work included parsers, indexes, queries, tool
definitions, a server, live reload, file watching, telemetry, embeddings, and
tests. Four major phases were delegated. One phase split two bounded namespaces
among additional agents.

The delegation discipline was strong. Prompts assigned explicit file
ownership, warned agents about concurrent work, and required formatting,
linting, tests, and acceptance evidence. This was not a confused or stalled
session. It was a productive session that stayed on the native path.

## The scoreboard

Across the parent and six implementation agents:

| Behavior | Calls | Tool input | Tool output |
|---|---:|---:|---:|
| Native `Read` on Clojure files | 47 | 4,072 chars | 442,423 chars |
| Native `Edit` on Clojure files | 128 | 203,068 chars | 21,905 chars |
| Native `Write` on Clojure files | 25 | 210,880 chars | 4,526 chars |
| Bash referencing Clojure files | 86 | 55,041 chars | 189,056 chars |
| clj-surgeon | **0** | 0 | 0 |

The direct source tools touched 25 distinct Clojure files. Their 200 calls were
not all avoidable: many writes created genuinely new namespaces and tests.

The read pattern is the important result:

| Read behavior | Count | Output |
|---|---:|---:|
| Unbounded whole-file reads | 38 | 413,090 chars |
| Bounded reads | 9 | 29,333 chars |
| All native reads | 47 | 442,423 chars |

Whole-file reads generated 93.4% of native read output. Thirteen full reads
targeted files whose final versions exceeded 500 lines. That wording matters:
some files grew during the session, so final size does not prove that every one
was already over 500 lines when read.

The native Read and Clojure-related Bash result channels emitted 631,479
characters, roughly 158,000 tokens at four characters per token. This is an
exposure estimate, not a claim that every byte could have been eliminated.
Test output, compiler output, and necessary source evidence are mixed into the
Bash total.

## The skill was present

The strongest causal evidence is the session's own skill listing. At the start
of the parent session it included:

> clj-surgeon: Inspect and modify Clojure, ClojureScript, and CLJC
> structurally. Use for compact reads, analysis, nested edits, navigation,
> dependencies, moves, and renames.

Each of the six spawned agents also received a skill listing containing
clj-surgeon. The substantive parent and all six agents made zero clj-surgeon
calls and zero skill invocations. The unrelated short session invoked only the
history-search skill.

The repository guidance produced during the build named the important source
files, test commands, live-reload procedure, and safety boundaries. It did not
name clj-surgeon or give a first-action rule for large Clojure files. Delegation
prompts likewise specified ownership and acceptance criteria but not structural
tool routing.

Availability therefore was necessary but insufficient. A passive catalog
entry lost to a deeply trained native-tool prior.

## The native loop reinforced itself

The observed workflow was internally coherent:

```text
create file with Write
  -> read the growing file
  -> copy exact old text into Edit
  -> see Edit succeed
  -> repeat
```

All 128 direct native Edit calls succeeded mechanically on their first tool
attempt. All 25 Write calls also succeeded. Mechanical success does not prove
semantic correctness, but it explains the behavior: no tool error forced
Claude to reconsider its route.

The edit payloads show where source context went:

| Exact `Edit` old-string shape | Count |
|---|---:|
| 80 characters or fewer | 22 |
| 200 characters or fewer | 61 |
| 1,000 characters or fewer | 116 |
| Began with a top-level `def...` form | 44 |

The median old string was 215 characters; the 90th percentile was 971; the
maximum was 2,836. Exact-string Edit was reliable because prior whole-file
reads supplied the bytes it needed. The hidden cost was acquiring and retaining
those bytes.

The Bash transcript also contained frequent `grep`, `cat`, `sed`, `head`, and
`tail` spellings. Those word counts are only heuristics because commands can
contain code and test scripts. They nevertheless agree with the direct-tool
evidence: Claude stayed inside familiar text and shell affordances.

## Where clj-surgeon would have fit

The most credible counterfactual is not "replace every native tool." It is a
small routing change for existing source:

```text
unknown form in a large file      -> :ls
known named form                  -> :cat
distinctive text, owner unknown   -> :cat :contains
structural occurrence             -> :grep-form
computed inventory or aggregate   -> :xray
exact structural replacement      -> :edit with :expect
new file or prose-heavy rewrite   -> native Write/Edit
```

Repeated full reads of 500- to 700-line namespaces were strong `:ls` and
`:cat` candidates. The 44 edits whose old strings began with a `def...` form
include plausible one-shot named-form edits. Registry inventories and counts
were plausible X-ray tasks. Broad cross-file discovery remained a good use of
`rg`; clj-surgeon should not absorb shell operations it does not improve.

The 413,090 characters returned by whole-file reads are an upper bound on the
opportunity, not a measured saving. Only a replay of the same tasks can tell us
how much source a correct structural route would expose.

## What this study does not establish

An adversarial reading prevents five overclaims:

1. **It does not show that clj-surgeon's API confused Claude.** Claude never
   invoked it.
2. **It does not show that native Edit was unreliable.** Every direct Edit
   call succeeded mechanically.
3. **It does not show that every native operation should move.** Greenfield
   file creation, comments, and broad multi-form changes remain native-tool
   territory.
4. **It does not establish exact token savings or latency.** Character counts
   are measured; token conversion and structural savings are estimates.
5. **It does not independently certify the completed service.** The transcript
   records formatting, linting, tests, and acceptance runs, but this study did
   not rerun that external repository's verification suite.

These limits strengthen the result that remains: the skill was visible to
seven Clojure workers and activated zero times.

## Why help and aliases cannot fix zero use

Improved refusal messages help after the first binary call. A smaller command
surface helps after the caller enters the tool. Neither can affect a session
that never invokes the skill or executable.

The evidence therefore argues against adding aliases in response to this
sample. The product's next constraint sits one layer earlier:

```text
skill visible -> skill selected -> route taught -> binary invoked -> API judged
                  ^
                  failure occurred here
```

## Recommended intervention

The skill description should name the competing behavior and the exact moment
to switch:

> Before using Read, Edit, grep, sed, or cat on an existing `.clj`, `.cljs`,
> or `.cljc` file, invoke this skill. For a file over 500 lines, begin with
> `clj-surgeon :op :ls`. Use native Write for a new file and native editing for
> unsupported prose- or comment-heavy changes.

Repository `CLAUDE.md` and `AGENTS.md` files should carry the same rule. Skill
listings are catalogs; repository instructions are active policy and propagate
into delegated work.

A non-blocking Claude pre-tool hook could reinforce the boundary by warning
before an unbounded Read of a large Clojure file. The first experiment should
warn rather than block. A hard prohibition would be premature because whole-
namespace review is sometimes legitimate.

## The next falsifiable experiment

Replay five anonymized task shapes from this session in clean Claude contexts:

| Task | Native route | Structural candidate | Primary measure |
|---|---|---|---|
| Find the owner of known text | search + full Read | `:cat :contains` | source output |
| Read one function in a 700-line file | full Read | `:cat :form` | source output |
| Inventory a large registry | Read + model counting | `:xray` | correctness |
| Replace one token in a named form | Read + Edit | `:edit :expect` | calls and receipt |
| Create a new namespace | Write | native Write | parity/control |

Run at least four independent repetitions per condition. Keep the task,
fixture, expected bytes, preservation gate, and model constant. Compare:

- correct final answer or bytes;
- voluntary skill activation;
- first source-bearing action;
- source-bearing calls and output;
- help and recovery calls;
- wall time;
- verified receipt or equivalent preservation evidence.

The activation intervention earns permanence if it causes voluntary structural
use on the four existing-source tasks without degrading the new-file control.
If activation rises and performance does not, then the API becomes the next
problem. If activation remains zero, more help text inside the binary is not a
credible response.

## Verdict

Claude delivered a large amount of correct-looking work quickly. Its native
editing loop was mechanically dependable. But it paid for that dependability
with repeated whole-file exposure and model-held source bytes, even though a
structural tool designed to avoid that cost was visible to every worker.

The result is less flattering and more useful than a forced-tool benchmark:
clj-surgeon has a capable read and edit surface, but its current skill listing
does not reliably cause adoption during real greenfield work. The next hill is
to make the first structural action obvious at the moment Claude is about to
read an existing Clojure file. Only after it crosses that hill can field usage
tell us whether the tool itself is perfect.
