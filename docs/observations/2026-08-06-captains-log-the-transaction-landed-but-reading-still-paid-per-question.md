# Captain's Log: The Transaction Landed, but Reading Still Paid per Question

<!-- agent-usage-window-end: 2026-08-06T22:43:19Z -->

**Two-hour window:** 2026-08-06 20:43:19–22:43:19 UTC  
**Pacific:** 2026-08-06 13:43:19–15:43:19 PDT

**New-function window:** 2026-08-06 22:13:19–22:43:19 UTC  
**Pacific:** 2026-08-06 15:13:19–15:43:19 PDT

**Question:** Did callers adopt the intent transaction, stdin input, and
literal X-ray count guard? Did those features make real work faster?

## Sampling and exclusions

Two explicit `make study-agent-usage` receipts covered the nested windows. The
receipts hash session identities and omit prompts, source, commands, and
workspace paths. Their operation counts are the counting authority.

The two-hour sample contained eight Codex sessions, six of them
Clojure-relevant, and six Claude sessions, five of them Clojure-relevant. It
mixed product development, clean-context probes, one unrelated application
read, and one naturalistic local-performance diagnosis. Product-development
refusals are valid interface evidence but are not counted as field failures.

The route receipt could not determine whether the two newest transaction calls
used stdin, or why one caller made 34 read commands. Narrow transcript review
was therefore limited to two receipt-named Codex evidence files and the exact
task intervals. User goals were reduced to neutral task descriptions. No
private source, paths, commands, repositories, or domains are reproduced here.

## Two-hour scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 8 | 6 |
| Clojure-relevant sessions | 6 | 5 |
| Skill visible | 6 | 5 |
| Skill loaded in window | 4 | 1 |
| Sessions that invoked clj-surgeon | 4 | 4 |
| clj-surgeon calls | 71 | 11 |
| Outer Surgeon actions | 46 | 9 |
| Explicit refusals | 9 | 0 |
| Execution-error actions | 2 | 0 |
| Surgeon output captured | 246,066 chars | unavailable |
| Native patch actions | 24 | 0 |
| Native Clojure shell actions | 48 | 7 |
| Direct Surgeon wall | 134.913 s | unavailable |
| Median / p90 Surgeon action | 2.238 s / 6.567 s | unavailable |
| Direct native-patch wall | 98.559 s | unavailable |
| Median / p90 native patch | 3.442 s / 8.504 s | unavailable |

Claude's transcript format does not provide comparable result payload or wall
telemetry. Its zeroes in those fields mean “not observed,” not “free.” The
Claude skill-load count is also a lower bound: three clean probes invoked the
binary without a separately recorded skill read.

Actual operations show two distinct products in use:

| Operation | Codex | Claude | Total |
|---|---:|---:|---:|
| `:cat` | 40 | 1 | 41 |
| `:ls` | 15 | 0 | 15 |
| `:xray` | 6 | 0 | 6 |
| `:change!` | 7 | 4 | 11 |
| `:undo-change!` | 3 | 3 | 6 |
| `:match-form` | 0 | 3 | 3 |

The transaction was not merely documented. Codex and Claude both executed it,
and both exercised receipt-based undo. This is adoption evidence. Most of it
came from product development and clean probes, so it is not yet evidence of
habitual use in unrelated repositories.

## The clean paired probe remained competitive, not faster

Two simultaneous clean Codex callers took matched routes through a small edit
task. One used the intent transaction and undo. The other used native patch.

| Measure | Intent transaction | Native patch |
|---|---:|---:|
| Completed task wall | 108.160 s | 94.717 s |
| Write actions | 1 `:change!` + 1 undo | 1 patch |
| Direct write-tool wall | 1.886 s | 0.065 s |
| Native shell actions | 4 | 2 |

The transaction caller was 13.443 seconds, or 14.2%, slower in this one roll.
Only 1.821 seconds of the difference was direct write-tool execution. Model
variance, route construction, and verification dominate the remaining gap.
This sample is too small to infer a stable speed ratio. It does show that a
safe transaction can stay near native task wall, but it does not show a speed
advantage.

## The final 30 minutes

| Measure | Codex | Claude |
|---|---:|---:|
| Clojure-relevant sessions | 2 | 1 |
| Skill loaded inside subwindow | 2 | 0* |
| clj-surgeon calls | 45 | 1 |
| `:cat` / `:ls` | 30 / 11 | 1 / 0 |
| `:xray` | 2 | 0 |
| `:change!` | 2 | 0 |
| Surgeon output captured | 172,042 chars | unavailable |
| Direct Surgeon wall | 96.634 s | unavailable |
| Median / p90 Surgeon action | 2.274 s / 7.304 s | unavailable |
| Native patch actions | 6 | 0 |

\*The Claude session loaded the skill before the 30-minute cutoff, then made
one `:cat` call inside the subwindow. This is boundary censoring, not a
visibility failure.

### The write transaction worked, but stdin was not dogfooded

The implementation session made two successful `:change!` calls with explicit
count guards and receipt output. Both calls still created a temporary EDN spec
with a heredoc and passed its filename. Neither used `--spec-file -`.

That is the clearest new-feature finding. Stdin support was implemented and
installed, but this window contains no post-install naturalistic use. The
feature removes escaping and temporary-file ceremony in principle; the sample
does not yet show that callers choose it.

The same session also used literal `(expect-count n)` in X-ray work. One
invalid-cardinality result occurred during product-development verification,
then the guarded form was used successfully. This validates the refusal and
recovery path, not independent adoption.

### A post-window wrong guess was primed by our README

At 22:45:34 UTC, immediately after the receipt cutoff, one caller tried the
same unsupported named-form read spelling against four targets. The raw route
shows one outer tool action and one shared command template. This was one wrong
hypothesis fanned out four times, not four opportunities to learn from a prior
refusal.

The guess was not mysterious. The README's high-attention introduction used
that exact unsupported operation and selector as its example of executable
recovery. Text intended to advertise a good refusal primed the model to issue
the bad call. The runtime behavior remains correct: it refuses and returns an
executable `:cat :form` remedy. The guidance was wrong.

The remediation removes unsupported spellings from the README while retaining
the recovery contract. A permanent surface test now requires the README and
both repo skills to avoid the unsupported operation. Existing executable
remedy tests remain unchanged. The principle is:

```text
tolerate historical guesses at runtime; never teach them in caller guidance
```

### The naturalistic diagnosis used Surgeon as a precise but expensive reader

An unrelated application task asked why local page loads exceeded two seconds
and whether the request handler had an architectural problem. Its completed
diagnostic turn followed this route:

```text
skill-load -> 26 Surgeon read calls in 17 actions -> diagnosis
```

| Measure | Diagnostic turn |
|---|---:|
| Files considered | 9 |
| `:cat` calls | 17 in the completed turn; 25 in the session slice |
| `:ls` calls | 9 in the session slice |
| X-rays | 0 |
| Native patch actions | 0 |
| Surgeon output in completed turn | 113,993 chars |
| Direct Surgeon wall | 45.693 s |
| Complete turn wall | 409.751 s |
| Surgeon share of turn wall | 11.15% |

This was not failed adoption. The caller loaded the skill, stayed on structural
reads, avoided native source patching, and reached a diagnosis. It also paid
roughly one Babashka/process startup for every question. The 26 calls averaged
about 1.76 seconds of direct wall and produced enough source to make the
“compact lens” claim questionable for this task.

The missing capability is now obvious. Write intent can be compiled into one
transaction. Read intent still has to be executed as a sequence of independent
commands.

## A ten-read probe showed a 3.1× direct-tool opportunity

A quick self-hosting probe used the same ten named-form reads in two routes.
The sequential route launched the installed CLI ten times. The batch prototype
sent one EDN read manifest to one Babashka process, then called the existing
dispatcher ten times without changing selector behavior.

| Route | Run 1 | Run 2 | Run 3 | Median |
|---|---:|---:|---:|---:|
| Ten CLI processes | 5.70 s | 5.60 s | 5.53 s | 5.60 s |
| One process, ten reads | 1.80 s | 1.53 s | 1.87 s | 1.80 s |

The one-process prototype was 3.1× faster and removed 3.80 seconds, or 67.9%,
from direct wall. It still reread and reparsed the file for each request. A
real batch can snapshot each file once and execute all selectors against that
snapshot, so 1.80 seconds is not the lower bound.

A three-point startup probe separated the current costs:

| Probe | Median wall |
|---|---:|
| Bare Babashka | 0.03 s |
| Require `clj-surgeon.core` | 0.18 s |
| One installed named-form read | 0.30 s |

Babashka itself is not the problem. Loading Surgeon adds about 0.15 seconds,
and one complete read adds about 0.12 seconds more in this warm local probe.
The immediate win is one namespace load per declared read set. The next win is
one parse per distinct file. A daemon or MCP server is not justified by this
evidence; an ordinary stdin batch captures most of the available startup gain
without lifecycle state.

An instrumented pass inside the one-process prototype timed each existing
dispatcher call separately:

| In-process read measure | Wall |
|---|---:|
| Minimum | 58 ms |
| Median | 73 ms |
| Maximum | 114 ms |
| Ten-read total | 800 ms |

The existing dispatcher rereads and reparses the same file for each request.
These per-read costs confirm that work remains after process startup. A batch
snapshot cache can target it directly.

A Graal native image remains a valid later experiment, but not the first one.
Babashka has already removed JVM startup. Even a hypothetical image that erased
the measured 0.15-second Surgeon load from all ten separate invocations would
save about 1.5 seconds. The one-process prototype saved 3.8 seconds before
parse sharing. Native compilation also adds a build, portability, reflection,
and distribution surface. Revisit it only after batching and one-parse-per-file
profiling leave startup as a material fraction of task wall.

## Direct tool time did not dominate, but it became material

Across completed Codex turns in the two-hour receipt, direct Surgeon execution
was 134.913 seconds inside 3,035.005 seconds of aggregate task wall, or 4.4%.
Reducing startup cannot produce a fourfold end-to-end gain by itself.

In the naturalistic diagnosis, however, Surgeon consumed 11.15% of turn wall.
Removing 20 or more process starts could save tens of seconds while also
reducing action count and model bookkeeping. That is large enough to measure
without pretending it solves reasoning latency.

## Adversarial verdict

What is supported:

- The intent transaction and undo are executable by both callers.
- Explicit count guards are being used.
- The naturalistic caller preferred structural reads over raw file dumps.
- Guarded transaction overhead is small enough to remain near native task wall.

What is not supported:

- Stdin transaction input has not yet shown independent adoption.
- The transaction is not yet faster than native patch in a matched clean roll.
- Literal X-ray count syntax has not yet shown independent field adoption.
- `:cat` plus `:ls` is not a fast project-level exploration surface when a
  diagnosis spans many forms.
- Claude and Codex wall times cannot be compared from the current provider
  telemetry.

## Next falsifiable improvement: compile read intent too

Extend the stdin manifest model to read operations. One process should accept
an ordered set of guarded `:ls`, `:cat`, and `:xray` requests, execute them
against one consistent source snapshot, and return one ordered receipt. Each
request needs an explicit match or cardinality guard and a byte cap. The batch
must refuse loudly if any required read is ambiguous or truncated.

Replay the same local-performance diagnosis with a clean caller. Keep the
feature only if all of these gates pass:

| Gate | Current baseline | Target |
|---|---:|---:|
| Outer Surgeon read actions | 17 | 4 or fewer |
| Surgeon read calls/process starts | 26 | 2 or fewer |
| Direct Surgeon wall | 45.693 s | under 15 s |
| Surgeon output | 113,993 chars | under 60,000 chars |
| Diagnostic correctness | correct | no regression |
| Native unbounded Clojure reads | 0 | 0 |

The first implementation should be small enough to dogfood: stdin EDN in,
ordered read results out, no new selector language. If it cannot beat these
gates, do not add another public command. Improve startup, result compaction,
or the skill instead.

Start with the smallest same-file batch because it matches an observed caller
thought directly:

```bash
clj-surgeon :op :cat :file src/writer/routes.clj \
  :forms '[handle-sync-draft draft-conflict-response]'
```

`:form` and `:forms` must be mutually exclusive. `:forms` preserves requested
order, parses the file once, and returns an ordered vector of the existing
exact-source records under one file hash. If any requested name is missing or
ambiguous, the complete read refuses with compact per-name evidence and no
partial source. Reject duplicate requested names. This gives callers an
immediate one-call route for the common “read these neighboring owners” task.

The general stdin manifest is the second layer for cross-file or mixed
operations. It should accept `:cat :forms` as one member rather than inventing
another selector model.

### Same-file batch dogfood crossed its first speed gate

The first implementation added `:cat :forms '[a b c]'` directly to the
existing read operation. It does not loop over `show-file`. The file is read
once, the top-level record index is built once, and every requested name is
resolved against that snapshot. Pure tests instrument the index builder and
require exactly one call.

The first real call asked the working-tree binary to read `select-form`,
`select-named-forms`, and `show-file` from its own implementation. One result
returned all three exact sources in requested order under one file hash. A
second call inserted one absent name between two valid names; it refused the
complete batch and returned no source-bearing record.

Five local timing runs compared those same three owners:

| Route | Median wall | Output |
|---|---:|---:|
| Three separate CLI reads | 0.54 s | 8,884 bytes |
| One `:cat :forms` read | 0.20 s | 8,621 bytes |

The implemented batch was 2.7× faster, removed 63% of direct wall, and emitted
slightly fewer bytes. The source payload itself was unchanged; savings came
from one process, one file read, one parse, and one result envelope.

This microbenchmark is mechanism evidence, not the product keep gate. The clean
Mothership replay must save at least 10 seconds of direct tool wall and 10
seconds of complete task wall, preserve diagnostic correctness, reduce
source-bearing actions, and avoid increasing source output. A subsecond local
gain does not count as beating built-in tools.

Early dogfood found two defects before installation:

1. A new helper initially appeared before an existing dependency and failed at
   namespace analysis. Moving it behind the dependency restored the compile
   gate.
2. A failed canonical batch received a generic `:cat` remedy that repeated the
   same failed command. Recovery now recommends `:cat` only for an invented
   operation or historical selector spelling. A valid canonical `:cat`
   refusal never recommends itself.

Both failures now have permanent tests. This is the desired frontier loop:

```text
small batch -> use it on itself -> capture the surprise -> harden the contract
```

An adversarial output test added one more boundary: combined batch source over
65,536 characters refuses before returning any form record. Faster must not
mean faster context-window exhaustion.

### A bounded caller test found the real win boundary

The first clean Mothership replay was not a useful speed test. An open-ended
log diagnosis let the caller load broad architecture guidance, enumerate the
repository, inspect live processes, aggregate a changing log, and repeatedly
confirm its hypothesis. The first turn took 474.59 seconds. The complete
three-turn exchange took 632.47 seconds. Only two successful Surgeon source
reads occurred in the first turn. The task measured forensic stopping behavior,
not the named-form read surface.

A replacement task froze one repository commit and asked two bare Codex callers
the same deterministic question. Each caller had to explain one reader protocol
from named forms in `source_reader.clj`. The temporary native environment had no
personal skills or global Clojure instructions. The temporary Surgeon
environment added only the installed clj-surgeon skill. Both were read-only and
ephemeral.

The pilot looked like the desired product win:

| Measure | Native control | `:cat :forms` | Change |
|---|---:|---:|---:|
| Complete task wall | 73.64 s | 57.13 s | **−16.51 s (−22.4%)** |
| Source-bearing actions | 4 | 1 | −3 |
| Input tokens | 84,558 | 49,505 | −35,053 (−41.5%) |
| Source output | 20,171 chars | 9,052 chars | −11,119 (−55.1%) |

That task accidentally omitted two helper forms needed for the exact answer.
The native caller's broad ranges included one helper anyway, while the
structural caller correctly stayed inside the requested set. The pilot therefore
had unequal evidence and could not be the keep result.

The corrected task named every required helper and reversed run order:

| Measure | Native control | `:cat :forms` | Change |
|---|---:|---:|---:|
| Complete task wall | 57.61 s | 57.18 s | −0.43 s (noise) |
| Source-bearing actions | 3 | 1 | −2 |
| Total command actions | 3 | 2, including skill load | −1 |
| Input tokens | 63,146 | 50,227 | −12,919 (−20.5%) |
| Source output | 12,159 chars | 10,600 chars | −1,559 (−12.8%) |
| Answer correctness | complete | complete | tied |

The corrected replication did not cross the 5–10 second wall-time gate.
`:forms` reliably reduced source actions, source output, and input tokens. It
did not reliably beat a competent native caller that combined location search
and broad ranges into three actions. The 16.51-second pilot gain came from one
additional native range-repair turn.

This identifies the next large lever. The skill load consumes one action before
the structural read, and same-file batching can remove only a few native actions.
A cross-file read transaction can remove many more. A first-class tool surface
could also make the structural call directly, without a separate skill-read
action. Either experiment must beat the corrected native control by at least
five seconds; lower token use alone is not the product claim.

The proposed caller shape deliberately reuses the write-transaction model:

```clojure
{:reads
 [{:op :ls :file "src/service.clj"}
  {:op :cat :file "src/service.clj" :form 'handle-request}
  {:op :xray :file "src/state.clj"
   :expr "(-> (form 'load-state) (expect-count 1))"}]
 :limits {:source-chars 60000}}
```

The command should read this document from stdin and return one ordered EDN
receipt. The manifest is a scheduling envelope around existing operations, not
a new query language.

## Research instruments

The study used the repo-owned collector twice with explicit UTC bounds, `jq`
for privacy-safe receipt reduction, `rg --files` to locate only the two named
Codex evidence files, and bounded `jq` queries for task goals and feature
flags. The raw review was used only to distinguish temporary-file transaction
input from stdin, classify the 34-call session as architectural diagnosis, and
confirm that four post-window wrong calls came from one outer action. Three
repeated `/usr/bin/time` probes measured sequential reads, one-process reads,
and startup components. `make study-agent-usage-self-test` verifies the
collector and privacy contract after this document is written.

## Cross-file `:cat` made the complete read declarative

The next implementation kept the API small. It extended `:cat` instead of
adding a batch command:

```bash
clj-surgeon :op :cat :spec-file - <<'EDN'
{:reads [{:file "src/a.clj" :forms [start stop]}
         {:file "src/b.clj" :forms [route]}]
 :expect {:file-count 2 :form-count 3}
 :limits {:source-chars 65536}}
EDN
```

The transaction preserves file and form order, reads each distinct physical
file once, and returns one complete-file hash per file. It refuses before
returning partial source when the manifest has unknown keys, duplicate
canonical paths, incorrect file or form counts, invalid selectors, missing or
ambiguous forms, unreadable files, or excessive combined output.

Permanent tests cover the pure and CLI contracts. They instrument source I/O,
require exactly one read per file, test stdin and saved manifests, verify
ordering and hashes, reject path aliases, and search every refusal recursively
for leaked `:source` fields. The full repository gate passed 525 tests and
4,430 assertions with zero failures. The stable installer copied the CLI and
both agent skills after the test run.

The first self-hosting call read four implementation forms from
`show_form.clj` and `core.clj` in one command. A three-file Mothership call then
read nine exact owners and returned the expected 6,982 source characters in
0.7 seconds of direct tool wall. The mechanism worked.

## A cold nine-form task exposed the fixed acquisition cost

The first clean task asked for nine deterministic facts from nine named forms
in three frozen Mothership files. Logs, broad search, edits, tests, and Git were
out of scope. Both callers returned nine correct lines.

| Measure | Native control | Cross-file `:cat` | Change |
|---|---:|---:|---:|
| Complete task wall | 35.39 s | 55.61 s | **+20.22 s (+57.1%)** |
| Source actions | 1 | 1 | tied |
| Skill-load actions | 0 | 1 | +1 |
| Input tokens | 33,063 | 109,153 | +76,090 |
| Output tokens | 1,233 | 1,483 | +250 |
| Correct answers | 9 / 9 | 9 / 9 | tied |

Native Codex generated a Ruby balanced-form extractor and read all nine owners
in one source action. Surgeon also used one source action, but first read the
90-line skill. That extra tool round repeated the large agent context and
dominated the small task. The skill's byte count was not the main cost; the
additional deliberation round was.

This suggested a JVM-warmup model:

```text
total wall = fixed skill acquisition + repeated task cost + mistakes and rework
```

If Surgeon saved five seconds on every later task, a 20-second cold cost would
break even near task four. If it saved only one second, break-even would move
near task 20. A longer benchmark was necessary.

## A resumed-turn warmup attempt was invalid and was discarded

The first warmup design sent five sequential prompts to two resumed Codex
sessions. Stage 1 was valid. On resume, the CLI process used the benchmark
driver's working directory instead of the frozen Mothership directory. Both
callers correctly reported that the relative source paths were absent in stage
2. Native then returned the required nine refusal lines without reading source.
Surgeon later recovered by using absolute paths. Those later walls did not
measure the same work and are excluded.

The failure also corrected the warmup analogy. The agent policy requires a
skill read in every new user turn that triggers the skill. A sequence of five
resumed prompts therefore pays skill acquisition five times. Skill warmup can
be amortized across the multiple tool actions of one long user request, not
automatically across separate user turns.

## The valid five-times-larger task made the loss larger

The replacement benchmark put all five sections in one user request. It named
45 exact forms across the same three frozen files and required five headings
with nine ordered answers each. Both callers ran concurrently, read-only, with
the same prompt. The requested forms contained 58,569 source characters, below
the transaction's 65,536-character guard.

Both callers returned all five headings and all 45 numbered lines. Manual
comparison against the exact form sources found both answers substantively
correct. Surgeon was sometimes more explicit about branch conditions, but it
did not change the correctness verdict.

| Measure | Native control | Cross-file `:cat` | Change |
|---|---:|---:|---:|
| Complete task wall | 101.87 s | 145.33 s | **+43.46 s (+42.7%)** |
| Total shell actions | 2 | 5 | +3 |
| Successful source actions | 2 | 3 | +1 |
| Command output | 87,836 chars | 113,952 chars | +26,116 (+29.7%) |
| Input tokens | 87,716 | 206,270 | +118,554 (+135.2%) |
| Cached input tokens | 63,488 | 160,512 | +97,024 |
| Output tokens | 4,470 | 4,996 | +526 |
| Reasoning-output tokens | 245 | 808 | +563 |
| Correct ordered answers | 45 / 45 | 45 / 45 | tied |

There was no crossover. The longer task amplified the disadvantage.

### Why the transaction did not remain one call

The exact Surgeon route was:

```text
read the skill
    -> invoke `clj-surgeon :op :cat :spec-file -` without attached stdin
       -> command waited and was interrupted with exit 130
    -> pipe the complete 45-form manifest to `:cat`
       -> transaction succeeded
       -> result exceeded the agent transcript's useful visible window
    -> reread the forms hidden at the end of the transcript
    -> reread the final three hidden forms
    -> answer
```

The tool accepted the complete source payload, but its EDN result repeated the
operation, file, selector, platform, line, hash, and source-string envelope for
every record. Escaped newlines and quotes expanded the representation further.
The successful one-process result was therefore too large for the caller to
consume as one visible result. Transaction success at the CLI boundary did not
produce transaction success at the agent boundary.

Native also crossed its first output boundary. Its generated Python extractor
made one broad attempt, then one recovery read for forms hidden or missed near
the end. It still needed only two actions and emitted 26,116 fewer command
characters than Surgeon.

The failed bare-stdin call is a separate adoption defect. The skill taught the
shape but not the safest encoding for this tool environment. The agent invoked
the command and waited for interactive input instead of attaching the manifest
to the command. Its next attempt used `printf` successfully.

## Corrected product thesis

The unit of optimization is one model deliberation, not one process, file, or
selector. A useful transaction must fit through every boundary:

```text
model intent
    -> one tool action
    -> one bounded visible result
    -> enough evidence for the decision
```

Cross-file scheduling and one-snapshot semantics remain valuable. They did not
clear the product keep gate because the visible representation and cold tool
entrance erased those gains.

The next experiments are ordered by expected wall-clock impact:

1. Add an agent-compact result layout. Print each file hash once and each form
   as a short header plus raw source. Do not repeat result maps or EDN-escape
   complete source bodies. The 58,569-character payload must remain below the
   caller's transcript boundary without a recovery read.
2. Teach a noninteractive pipe as the canonical stdin spelling. The clean
   caller must attach the document on its first attempt and must never wait for
   interactive stdin.
3. Test a tiny preloaded route card or a minimal typed MCP entrance. The model
   should call the read transaction without a separate skill-loading action or
   shell escaping. Keep the tool surface to `inspect`, `change`, and `refactor`
   rather than exposing every CLI operation.
4. Repeat the exact 45-form task. Keep the compact route only if it returns all
   45 sources in one visible action, preserves both correctness and guards, and
   beats the 101.87-second native control by at least five seconds.
5. After the read gate passes, run one long mixed workload that inspects,
   computes, changes, refuses one stale expectation, and verifies. Measure the
   cumulative number of model deliberation rounds, not only shell calls.

The ambitious target remains three cognitive transactions:

```text
inspect -> decide -> change and verify
```

The present result is a real negative result, not a reason to weaken the tests
or lower the speed gate. Native `apply_patch` and generated shell programs are
fearsome competitors. clj-surgeon becomes irresistible only when its stronger
structural guarantees arrive with fewer model rounds and less visible output.

## The first remediation crossed the five-second keep gate

The benchmark failure immediately produced two product changes.

First, a bare `:spec-file -` no longer waits indefinitely for later input. When
no document is ready on stdin, the command refuses in about 0.36 seconds with
`:missing-spec-stdin` and an executable `printf | clj-surgeon` remedy. This is a
guard, not only documentation; the clean caller ignored the skill's explicit
warning and made the bare call again.

Second, cross-file `:cat` accepts `:format :semantic`. The default remains the
exact lexical-source EDN contract. Semantic format instead prints each file
hash once, a short name/range header per form, and one canonical Clojure value
per form. It omits comments and layout and may expand shorthand such as `#()`.
The tradeoff is explicit and appropriate for behavior and architecture reads.
Malformed semantic data or output above 65,536 characters refuses before any
partial semantic output is printed.

On the frozen 45-form payload:

| Representation | Output |
|---|---:|
| Exact-source EDN | 68,339 chars |
| First semantic rendering | 51,231 chars |
| Final short-header semantic rendering | 47,814 chars |
| Reduction from exact EDN | **20,525 chars (30.0%)** |

New adversarial tests prove requested order, file hashes, canonical shorthand
expansion, explicit comment/layout loss, unchanged default EDN, invalid-format
refusal, semantic parse refusal, semantic output limits, CLI stdin behavior,
and the fast missing-stdin remedy. The strengthened repository gate passed 528
tests and 4,478 assertions with zero failures before installation.

### The exact rerace reversed the result

The native control and the installed semantic Surgeon reran the identical
45-form prompt concurrently from fresh ephemeral sessions. Both returned all
five headings and 45 ordered answers. Manual comparison found both
substantively correct.

| Measure | Native v2 | Semantic Surgeon | Change |
|---|---:|---:|---:|
| Complete task wall | 108.39 s | **102.48 s** | **−5.91 s (−5.5%)** |
| Total shell actions | 2 | 4 | +2 |
| Successful source actions | 2 | 2 | tied |
| Command output | 88,308 chars | **63,766 chars** | −24,542 (−27.8%) |
| Input tokens | **74,458** | 115,513 | +41,055 (+55.1%) |
| Cached input tokens | **50,432** | 82,688 | +32,256 |
| Output tokens | 3,504 | 3,580 | +76 |
| Reasoning-output tokens | 258 | 260 | +2 |
| Correct ordered answers | 45 / 45 | 45 / 45 | tied |

The fixed Surgeon crossed the promised five-second wall-time gate. Relative to
its first 45-form run, it improved from 145.33 to 102.48 seconds, removed 50,186
command-output characters, and removed 90,757 input tokens.

The exact fixed route was:

```text
read skill
    -> incorrectly invoke bare stdin
       -> fast structured refusal instead of a hang
    -> pipe one 45-form semantic manifest
       -> 47,651 visible command characters
    -> reread five forms hidden from the middle of the transcript
    -> answer all 45 questions
```

Native again used two generated Ruby extraction commands. Surgeon matched its
two successful source actions and won complete wall despite paying both a skill
read and a fast refusal.

This is a keep result, not the final ideal. The 47,651-character semantic
result still crossed the caller's middle-truncation boundary, so the agent
reread five forms. The one-process transaction has not yet become one fully
visible agent transaction. Input tokens also remain 55% above native because
of the additional skill and refusal rounds.

The next frontier is now narrower:

1. A typed `inspect` entrance should attach the manifest without shell stdin
   ceremony and without a separate skill-loading action.
2. Large semantic reads need per-form projections or another transcript-native
   representation below approximately 40,000 visible characters.
3. The exact 45-form race remains the regression benchmark. A later version
   must preserve 45 / 45 correctness, use one successful visible source action,
   and improve on the 102.48-second Surgeon wall rather than comparing only
   with the older failure.

The lesson is not that generic text defeated structural editing. The general
caller generated a formidable temporary extractor. The structural product won
only after its input and presentation layers stopped sabotaging the engine.
