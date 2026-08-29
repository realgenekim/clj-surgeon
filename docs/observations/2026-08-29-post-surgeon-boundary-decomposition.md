# The nine-second boundary was two clocks, not one thought

Date: 2026-08-29

Verdict: **NO-GO for a broad “compile the next action” product hill from this
population. GO for one privacy-safe action-emission clock and a matched compact
mutation screen.**

## Evidence and method

This is a derivative analysis of the existing privacy-safe receipt. It does not
advance the agent-usage marker and did not recollect provider history.

- Window: 2026-08-28 14:52:11.111450Z through
  2026-08-29 07:32:24.734205Z.
- Pacific window: 2026-08-28 07:52:11.111450 through
  2026-08-29 00:32:24.734205 PDT.
- Receipt:
  `/tmp/clj-surgeon-agent-usage-20260828T145211111450Z-20260829T073224734205Z.json`.
- Receipt SHA-256:
  `0d8918d5ae1ab7951d710b939be3e671bca934ce1985ae877dfed6f8017bc511`.
- Analysis:
  `dev/experiments/post_surgeon_boundary_decomposition.py`.
- Analysis SHA-256:
  `8e7851c64b390d7727ab3795cabd14dd9ea3079457c82d69546da76859dc1665`.
- Aggregate JSON SHA-256:
  `0e164383a5029c7448ff4232dbe1997002490b1db55e586e0a2c280eb62796b8`.

The analysis consumes only receipt fields: completed item offsets and wall,
action ordinals, kinds, operation names, statuses, and categorical endpoints.
It does not read prompt text, source, paths, arguments, hidden reasoning, or raw
service events. It reproduces the receipt authority exactly: 185 boundaries,
2,737.790 seconds total, 9.089-second median, and 29.959-second p90.

`model-reasoning` means one completed Reasoning item was recorded. It is not
hidden chain of thought. `unattributed` remains literal: the client assigned no
completed item to that interval. It can contain inference, scheduling, prompt
or result ingestion, serialization, transport, logging, or UI delay.

## The complete population

Across all 185 Surgeon-complete to next-action boundaries:

| Clock segment | Total | Share of boundary wall | Nonzero boundaries | Median |
|---|---:|---:|---:|---:|
| Recorded completed reasoning | 1,302.534s | 47.6% | 181 | 2.930s |
| Unattributed before first reasoning | 540.706s | 19.7% | 176 | 2.491s |
| Unattributed between reasoning items | 71.898s | 2.6% | 41 | 0ms |
| Unattributed after last reasoning | 615.315s | 22.5% | 176 | 2.354s |
| Unattributed with no reasoning item | 0.468s | less than 0.1% | 4 | 0ms |
| Exclusive overlapping background work | 206.869s | 7.6% | 11 | 0ms |

The totals partition the complete boundary wall. “Exclusive overlapping
background work” is recorded work such as an already-running shell command or
context compaction that overlaps the boundary but is not itself the next
action. It is not model thinking.

```text
all 2,737.790 seconds of observed boundary wall

  19.7% pre-reasoning residual
  47.6% recorded reasoning
   2.6% between-reasoning residual
  22.5% post-reasoning residual
   7.6% overlapping background work
  ─────
 100.0%
```

The result is stable after excluding all 11 boundaries with overlapping
background work. The remaining 174 boundaries have an 8.888-second median,
2.916-second recorded-reasoning median, 2.506-second pre-reasoning residual,
and 2.377-second post-reasoning residual.

## The endpoint changes the shape

These are independent medians, not additive synthetic timelines.

| Next externally visible action | N | Complete boundary | Recorded reasoning | Pre-reasoning residual | Post-reasoning residual |
|---|---:|---:|---:|---:|---:|
| Agent message | 15 | 14.583s | 9.578s | 2.491s | **0.010s** |
| Surgeon read | 68 | 7.332s | 1.404s | 2.420s | **2.481s** |
| Native read | 42 | 8.860s | 3.202s | 2.444s | **2.116s** |
| Surgeon mutation | 19 | 15.367s | 4.395s | 4.024s | **5.039s** |
| Native patch | 14 | 19.132s | 3.322s | 2.384s | **8.836s** |

An agent message is the clean negative control. The boundary stops when the
message starts, so message generation is not charged to it. Those 15 message
items took another 1.568 seconds median after the endpoint. The 10ms median
from last reasoning completion to message start shows that a multi-second
post-reasoning tail is not a universal scheduler tax.

Tool actions are different. Their post-reasoning tails rise from roughly two
seconds for reads, to five seconds for a Surgeon mutation, to almost nine
seconds for a native patch. The receipt has no argument-byte or argument-stream
clock, so this is evidence consistent with action construction, serialization,
or dispatch—not proof that all of the tail is token emission.

The read and mutation comparisons are observational, not matched experiments.
The task mix differs. Still, two useful clues survive:

1. Surgeon-read endpoints have lower recorded-reasoning wall than native-read
   endpoints, while their post-reasoning tails are similar.
2. Surgeon mutations have a materially shorter post-reasoning tail than native
   patches, even though the Surgeon mutation schema is richer.

Four failed-Surgeon to Surgeon-mutation retries sharpen the second clue but do
not establish a rate. Their recorded-reasoning median is only 1.156 seconds,
while their post-reasoning residual is 8.847 seconds. The decision may already
be known after a refusal while reconstructing or emitting the corrected call
remains expensive. All four occur in one session, and the privacy-safe receipt
does not retain argument identity, so no automatic-retry contract is earned.

## Which time can Surgeon reach?

| Segment | Reachability judgment | Evidence boundary |
|---|---|---|
| Recorded reasoning, 47.6% | Conditionally reachable | Terminal responses, exact executable continuations, and mechanically compiled facts can delete a decision. The later read-mission audit found too few first-call-knowable repeated missions for a broad compiler, so this is not a universal prize. |
| Pre-reasoning residual, 19.7% | Unknown and mostly service-owned until measured | It can include scheduling and result ingestion. The extraction-only surface screen removed 63.7% of client surface bytes but improved complete wall only 5.2%; catalog shrink did not earn this segment. |
| Between-reasoning residual, 2.6% | Low-priority | Median is zero and it is concentrated in a minority of boundaries. |
| Post-reasoning residual, 22.5% | Most promising contract-adjacent segment | The 10ms message control and endpoint gradient are consistent with action construction. Compact, injective request algebras can plausibly reduce it, but argument size is missing from this receipt. |
| Overlapping background work, 7.6% | Not a Surgeon-contract target | Recorded shell and compaction work was already active. Admission control or client scheduling owns this segment. |
| Agent-message generation | Already outside this boundary | A terminal relay can shorten narration after a terminal mutation, as prior experiments proved, but it does not explain the 9.089-second statistic. |

## Smallest falsifiable next experiment

First add one privacy-safe scalar clock to the experiment collector. For each
next tool action, retain:

- canonical argument byte count, but not argument content;
- a root-normalized logical argument SHA-256 for equality, not reconstruction;
- previous Surgeon result byte count;
- last completed reasoning end to action-start wall;
- overlapping-background wall.

Permanent synthetic witnesses must prove that argument text, source, paths,
and reasoning content never enter the receipt.

Then run one frozen, capture-only C-R-R-C comparison of the same complete
mutation decision. Control and relation arms must use the same model, prompt,
catalog, tool surface, scorer, and semantic transaction. The only changed
variable is the emitted request representation. Predeclare:

1. 2/2 exact first calls per arm;
2. byte-identical canonical transactions and future hashes;
3. zero refusal, recovery, shell, source read, or second action;
4. relation post-reasoning-to-action wall at least 20% lower in both blocks;
5. relation complete verified wall at least 20% lower when the later mutating
   gate is authorized.

This experiment directly tests the reachable 22.5% segment. It does not call
all unattributed time thinking, and it does not reopen the stopped broad
read-mission or catalog-compression hills.

## Decision

- **NO-GO:** infer that Surgeon can remove the median 9.089 seconds generally.
- **NO-GO:** label the 5.384-second unattributed median as hidden thinking.
- **NO-GO:** promote another next-call compiler from endpoint counts alone.
- **GO:** add the privacy-safe action-size/emission clock before spending the
  next compact-relation model cohort.

The finding is narrower and more useful than “the model thought for nine
seconds.” Almost half the wall has a recorded reasoning owner. Almost half does
not. Within the unattributed half, the action-shaped post-reasoning tail is the
one segment whose endpoint gradient gives Surgeon a concrete, falsifiable
product lever.
