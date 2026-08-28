# Captain's Log: Nine seconds is the agent boundary, not Surgeon

## Correction to the first interpretation

The first event-clock report said that median Surgeon execution was about
0.243 seconds while the next externally visible agent action arrived about
9.1 seconds later. That was true, but incomplete. The natural question was
whether Surgeon caused the pause.

It did not.

Applying the same clock to native reads found essentially the same boundary:

| Completed tool | Boundaries | Tool p50 | Next-action p50 | Next-action p90 | Explicit reasoning p50 |
|---|---:|---:|---:|---:|---:|
| Surgeon read | 182 | 0.241s | 8.864s | 36.535s | 3.132s |
| Native read | 463 | 0.001s | 9.386s | 38.364s | 2.406s |
| Surgeon apply | 6 | 0.417s | 9.961s | 30.258s | 1.588s |
| Native patch | 295 | 0.001s | 7.310s | 24.122s | 0.655s |

These are all analyzable event-clock boundaries. Each begins when the tool item
finishes and ends when the next externally visible tool call, model message,
coordination event, collaboration event, or human input starts. Explicit
Reasoning items are measured inside that interval; unattributed time is not
relabeled as hidden thinking. Excluding human, collaboration, and coordination
endpoints moves the read medians only slightly: Surgeon 9.104 seconds and native
9.457 seconds.

The native item clocks have millisecond recorder granularity, and their task
mix differs from Surgeon's. The direct-tool columns are not a controlled speed
benchmark. The boundary comparison is the useful result: native and Surgeon
reads both return control to a model that commonly spends another nine seconds
deciding what to do.

## What happened next

The endpoint distribution is more revealing than the median:

| After a completed read | Surgeon read | Native read |
|---|---:|---:|
| Another read, either route | 102/182 (56.0%) | 200/463 (43.2%) |
| A mutation, either route | 24/182 (13.2%) | 59/463 (12.7%) |
| A model message | 23/182 (12.6%) | 94/463 (20.3%) |

The almost identical read-to-mutation rates are the sobering result. In this
observational window Surgeon did not generally make the model converge on an
edit more often than a native read. Surgeon reads were followed by another
read more often, although task selection is a major confound: agents tend to
choose Surgeon for structurally difficult work.

The same-route chains sharpen the opportunity:

- 75 Surgeon-read-to-Surgeon-read transitions had a 6.997-second median
  boundary and consumed 1,084 seconds in aggregate;
- 155 native-read-to-native-read transitions had an 8.124-second median
  boundary and consumed 2,258 seconds in aggregate;
- all 75 repeated Surgeon reads preserved route inertia: 51 MCP-to-MCP and 24
  CLI-to-CLI. The caller never switched transports between those paired reads.

Native patching showed a different geometry:

- 246 of 266 patches (92.5 percent) were followed by more tool work;
- 114 (42.9 percent) were followed by a read;
- 58 (21.8 percent) were followed by another mutation;
- only 20 (7.5 percent) were followed immediately by a model message.

That does not mean native patch failed. It means `apply_patch` is a cheap
incremental keystroke. Its strength is near-zero ceremony. Its weakness is
that a model can easily pay the next decision boundary many times while
assembling a multi-owner change.

The Surgeon-apply sample contains only six calls and cannot support a stable
observational comparison. Controlled extraction experiments remain the
stronger mutation evidence: one compiled Surgeon transaction plus terminal
response completed at about 21.8 seconds versus the frozen correct native
median of 122.278 seconds, approximately 5.6 times faster. That win came from
deleting planning, verification, and narration boundaries, not from making the
editor kernel faster.

## The product lesson

```text
native read   0.001s -> model boundary 9.457s -> next action
Surgeon read  0.239s -> model boundary 9.104s -> next action
                          ^
                          |
                    the dominant cost
```

For one obvious literal read, native is a fearsome competitor. Surgeon adds no
value by replacing `rg`, `sed`, or a visible file read one-for-one. A structural
tool earns its interaction only when it removes a later boundary by returning:

- the complete owner set for one decision;
- the exact relevant forms in one ordered snapshot;
- guard-ready anchors and hashes;
- mechanically derivable decisions already compiled;
- one executable next call when a genuine decision remains;
- a terminal mutation response when the operation finishes the task.

This changes the objective from "make inspect faster" to "make the next read
unnecessary." Saving 200 milliseconds inside Surgeon is almost irrelevant.
Deleting one median decision boundary saves about nine seconds. Deleting four
fragmented reads can plausibly save 35-40 seconds.

## Adversarial interpretation

SURGEON2 independently recomputed the receipt and reached the same verdict:
the 0.522-second read-boundary difference is not a credible route effect, while
the 75 same-route Surgeon chains are a credible optimization target.

The evidence does not yet prove that a compiled read mission will beat native:

1. Surgeon and native reads served different tasks.
2. A rich structural result can require more interpretation than a small text
   match.
3. Some repeated reads represent legitimate learning, not interface failure.
4. A giant response can move work from tool calls into model ingestion without
   improving complete task time.
5. The mutation sample is too small for a Surgeon-versus-native observational
   claim.
6. Forty-seven percent of both read populations came from one session, five of
   six Surgeon applies came from one session, and events within a session are
   correlated rather than independent.
7. Three Surgeon-read and seven native-read boundaries crossed a context
   compaction, and the event clock's endpoint law can leave git or live-probe
   work inside a boundary.

Therefore the next experiment must be matched and causal.

## Next falsifiable hill

Select retained tasks with two or more consecutive reads that serve one final
decision. Freeze task, model, source, and correctness scorer. Compare:

```text
control:   native or current Surgeon reads -> model decisions -> mutation
treatment: one compiled read mission       -> one model decision -> mutation
```

The treatment earns promotion only if it preserves semantic correctness while
reducing:

- complete task wall;
- read-call count;
- cumulative post-read boundary wall;
- source/output characters the model must interpret;
- fallback to native rediscovery.

The first target should be the observed Surgeon-read-to-Surgeon-read cohort,
not a synthetic large-file lookup. The threshold should be at least one entire
boundary removed and a 20 percent complete-wall improvement. The five-times
vision requires several boundaries to collapse into one chord.

## Receipt

- Frozen authority:
  `/tmp/clj-surgeon-agent-usage-24h-clock-20260827.json`
- Window: `2026-08-26T23:26:30Z` through `2026-08-27T23:26:30Z`
- Schema: `clj-surgeon.agent-usage-ethnography.v4`
- No new collection was performed and this note does not advance the study
  marker.
