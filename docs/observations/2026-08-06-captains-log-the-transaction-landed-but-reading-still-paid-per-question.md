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
