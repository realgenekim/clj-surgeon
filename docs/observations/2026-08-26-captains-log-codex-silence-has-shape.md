# Captain's Log: Codex Silence Has a Shape

Gene's recurring observation was precise: Surgeon would report a subsecond or
single-digit-second operation, but Codex would appear silent for 15, 20, or 30
seconds. What was it doing?

The retained `codex exec --json` stream could not answer. It preserved ten
ordered events but no arrival timestamps. A prior analysis also misread the
`commands.tsv` value `1407` as 1.407 seconds of lint; it was output character
count. That correction made measurement urgent.

## The reversible ratchet

We added a transport-neutral event tap to the clean benchmark harness. Its hot
path does not parse JSON. It forwards every input byte, flushes at complete
line boundaries, and records only sequence number, monotonic observer time,
UTC time, and line byte count. A separate offline summarizer joins clocks to
the unchanged raw stream and retains an allowlist of event type, item identity,
server/tool, status, exit code, and server-authoritative elapsed time.

Permanent gates prove:

- raw event bytes remain identical;
- prompts, commands, source, results, and reasoning text cannot leak into the
  timing summary;
- producer and observer exit statuses remain distinct;
- malformed JSON cannot interrupt the subject process;
- event count, sequence, and byte lengths match before a summary is trusted;
- raw clocks archive with the transcript while the structured summary remains;
- the existing harness and retention self-tests stay green.

## First correct Sol/high canary

The unchanged frozen 15-owner extraction completed correctly in 40.028 seconds
with one `apply_clojure_changes` and one exact lint.

| Observable interval | Wall |
|---|---:|
| Process start → first JSON event | 0.845s |
| Turn start → first agent message | 7.561s |
| First message → apply call emitted | 8.961s |
| Apply call start → completion | 7.917s |
| Surgeon server-authoritative apply time | 7.838s |
| Apply completion → next agent message | 4.270s |
| Message → verifier command emitted | 2.098s |
| Exact verifier command | **0.236s** |
| Verifier completion → final agent message | 7.122s |
| Final message → turn complete | 0.117s |
| Last JSON event → process exit | 0.893s |

The observer and server differed by only 79ms across the apply span. Codex did
not batch away the tool boundary, and the tap did not create a suspicious
multi-second perturbation.

The central result is the sum of five model-controlled gaps:

```text
initial decision output              7.561s
mutation call materialization        8.961s
mutation receipt interpretation      4.270s
verifier call materialization        2.098s
verification result interpretation   7.122s
                                      -------
                                     30.012s
```

This is externally observable event geometry, not private chain-of-thought.
We cannot name each second “reasoning”; it may include scheduling, cached prompt
ingestion, model generation, serialization, and transport. But we can now name
the removable boundaries.

## Decision

Exact lint computation is not the current bottleneck. The post-apply route
consumed 13.49 seconds around a 0.236-second command. This promotes one precise
hypothesis: execute the repository's identical exact verifier inside the
guarded transaction and return its terminal evidence, deleting the second
model-managed action without changing verification semantics.

Before claiming a speed win, establish same-seat variance and pin task, prompt,
scorer commit, model/reasoning, cached-token telemetry, service policy, and
route. Shadow-test verifier equivalence across four contracts first. Then use a
small paired cohort, and require a larger same-seat final cohort for the 5x
claim.

## Adversarial review

Codex Sol and Fable independently ranked observable clocks, verifier-boundary
fusion, and kernel profiling as the top three options. Fable initially argued
that retained transcripts might already carry timestamps; direct inspection
falsified that objection. Fable's stronger objections survive: the campaign
needs a noise floor, scorer pinning, cached-token evidence, and a final cohort
larger than two runs. Sol correctly warned that kernel work alone cannot own the
13.415-second stable gap.

The mystery that generated the experiment has become a map. Gene's “why did
Codex go silent?” observation now points to a concrete boundary worth deleting.

## First hill climb: make the correct chord obvious

The first attempt merely ordered Codex to emit the tool call first. It did—but
after 19.018 seconds it placed four extraction fields at the top level instead
of nesting them under `extraction`. Surgeon refused safely in 9.9ms with source
unchanged. Codex then recovered and produced correct source, but the route took
55.397 seconds and two mutation attempts. “Hurry” suppressed schema checking;
it did not make the change cheaper.

The next prompt supplied only the exact object skeleton, not source or a hidden
answer:

```text
{:workspace_root ...
 :extraction {:file ...
              :to ...
              :forms [...]
              :require_policy :minimal
              :public_forms [...]
              :caller_changes []
              :ignored_caller_files []}}
```

Codex filled the already-supplied values and emitted a correct MCP call as its
first item. The route was exact, one-shot, and completed in 33.789 seconds.

| Measure | Narrate-then-call canary | Skeleton tool-first canary | Delta |
|---|---:|---:|---:|
| Complete wall | 40.028s | 33.789s | **-6.239s (-15.6%)** |
| Turn start → mutation call | 16.522s over two phases | 11.829s one phase | -4.693s |
| Model-controlled event gaps | 30.012s | 22.659s | -7.353s |
| Apply observer wall | 7.917s | 9.435s | +1.518s |
| Exact verifier command | 0.236s | 0.214s | -0.022s |

The winning request was only 585 bytes. The earlier 8.961-second gap was not a
large-payload problem. The useful interface change was the chord's visible
shape: Codex no longer had to rediscover which fields belonged at which level.

This is one noisy pair, not a promotion result. Cached input also differed
(52,224 versus 35,072 tokens), and kernel wall moved against the candidate.
Run counterbalanced same-seat replicas before claiming the 6.239-second gain.
Still, it is the first direct evidence that “think, BANG” can remove a model
boundary without changing model, task, scorer, API semantics, or safety.

## Anvil replication: the chord survives both orders

Two fresh Sol/high Anvil seats ran the normal and skeleton tool-first prompts
in opposite order. All four runs were correct. Each used one extraction call,
one exact lint command, zero inspections, and zero refusals.

| Seat and order | Normal | Skeleton tool-first | Saving |
|---|---:|---:|---:|
| dev-b, normal → tool-first | 35.430s | 28.230s | 7.200s |
| dev-c, tool-first → normal | 35.444s | 27.667s | 7.777s |
| Median | **35.437s** | **27.949s** | **7.489s (21.1%)** |

The phase clocks locate the saving. Normal callers spent 16.10--16.66 seconds
producing an initial message and then materializing the mutation call.
Tool-first callers emitted the mutation as their first item after 10.55--12.55
seconds. Surgeon server time remained in the same 6.65--7.29-second range.
This is a route-compression win, not a faster extraction kernel.

Against the retained correct native control of 122.278 seconds, the replicated
tool-first median is **4.38x faster**. It is 3.493 seconds above the 24.456-second
5x gate. This is a strong promotion candidate, but not yet a general product
claim: the cohort contains two paired replicas of one frozen extraction.

Raw evidence is retained locally and on Anvil:

- `clj-surgeon-bench-archive/2026-08-27/tool-first-dev-b-ab.tar.gz`, SHA-256
  `e6d8c7b8280f2a368b88116f8168ed5dff31d20776d489799bfeca8526e16ae4`;
- `clj-surgeon-bench-archive/2026-08-27/tool-first-dev-c-ba.tar.gz`, SHA-256
  `d20d8c7dd2c2bdbc2a0c46d7abd3af7500cc5c5037e20079768f39977d46e123`.

Decision: keep the exact skeleton and first-item route. Do not generalize it as
“always suppress narration.” The mechanism is to make the complete valid
transaction shape cheaper to emit than a prose checkpoint, while leaving the
ordinary fail-closed planner available when a genuine decision is missing.

## Productization stop: prose is not the chord

The smallest apparent product change added direct-call and nesting guidance to
the always-loaded `apply_clojure_changes` description. The ordinary extraction
prompt remained unchanged. Two Anvil seats compared the description before and
after in opposite order:

| Seat | PRE description | POST description | Paired delta |
|---|---:|---:|---:|
| dev-b | 40.124s | 34.823s | -5.301s |
| dev-c | 32.918s | 35.406s | +2.488s |
| Median | **36.521s** | **35.115s** | **-1.407s (-3.9%)** |

All four runs were correct and used one Surgeon mutation plus one exact lint.
The crossed seat effect and sub-gate median do not earn the description change.
It was reverted. The stronger skeleton-prompt result remains valid evidence,
but the product needs an affordance closer to call construction than extra
sentences inside a large tool description.

The first attempt to run this cohort also exposed a harness hole: matrix cells
named `pre:mcp-*` and `post:mcp-*` were accepted even though only version `mcp`
starts a Surgeon server. Two callers received no Surgeon tool, entered shell
archaeology, and failed after 230--244 seconds. Those failures were not averaged
into the product cohort. A permanent schedule gate now rejects every MCP-prefixed
context without the MCP version before model launch.

Valid product-cohort archives:

- dev-b PRE `f1e770e0b40c38475917bc88a617061cd6b01e936da3bd84ef2a0d8b3c367e48`;
- dev-b POST `9c78f628ce267713acfcb33f16a4afd9afbf92ec095bc7b9e780e23a5b899be7`;
- dev-c PRE `dd32f41dce618d1bef4298b10afa087ea0ed5176b39681b49b6fee5826a47991`;
- dev-c POST `fc658d27a90bf7a102c217ed6655fbeedf55d8d068261ec3bded8340b01e03c6`.

If exact verifier fusion independently removes the second model-managed action,
the observed route geometry becomes capable of crossing 24.456 seconds. The
two options are now deliberately independent: SURGEON1 owns tool-first chord
ergonomics; SURGEON2 owns exact verifier equivalence and staged rollback.
