# Turn-waste mining: preregistration and results

<!-- agent-usage-window-end: 2026-08-30T15:00:00Z -->

<!--
Preregistration status: frozen before collection or counting.
Protocol commit: 367470c5fb9aec129f716f36f41e64d332fd2599.
No agent-usage receipt had been generated for this study when these rules were
written.
-->

## Preregistered protocol

### Objective and frozen window

This is one zero-model, retrospective lane over retained local telemetry. It
tests three proposed ways to remove caller/tool boundaries rather than reduce
milliseconds inside a tool. The fixed observation window is the half-open UTC
interval **2026-08-24T00:00:00Z through 2026-08-30T15:00:00Z** (Pacific:
2026-08-23 17:00 through 2026-08-30 08:00). All three studies use that same
window and the repository's `study-agent-usage` collector. No model, subagent,
or transcript-summarization call may participate in collection or scoring.

The privacy-safe schema-v6 receipt is the counting authority. Raw Codex or
Claude history may be opened only after a receipt has named the evidence file,
only inside this exact window, and only for one shortlisted task container at
a time when route phases and clock identities cannot adjudicate intent. Notes
and publication must not retain prompt prose, commands, paths, URLs, account
names, source, or hidden reasoning. Repositories and domains are anonymized.
The receipt stays outside Git.

Here, a **turn** means one avoidable caller-to-tool decision boundary: a model
had to emit and await another action before it could proceed. It does not mean
a complete user message/assistant response pair. The measured waste total is
the count of avoidable boundaries plus the sum of their receipt-recorded
`boundary_ms`; missing clocks remain unknown rather than zero. The same
boundary can be assigned to only one study, in priority order Study 1, Study 2,
then Study 3, so the cross-study total is additive.

Shared exclusions are: synthetic/self-test/benchmark sessions; incomplete
task containers whose relevant action sequence crosses a window edge; retries
caused by transport failure, stale snapshots, or changed source; verification
reads required by an explicit task acceptance criterion; human interruptions;
and actions whose ordering is unclear. Exclusions are reported, not silently
discarded.

For structured MCP calls, packet size is the sum of retained canonical result
bytes plus **512 bytes per fused request** for response framing. A hypothetical
packet is useful at no more than **65,536 bytes**. "Regularly exceeds" or
"truncates often enough" means more than 20% of otherwise-eligible episodes or
batches exceed that bound. CLI calls without comparable canonical byte
evidence stay in incidence counts but are excluded from the packet-size rate.

Weekly firing rates are measured opportunities divided by the 159-hour window
and multiplied by 168. A build recommendation's weekly value is
`median avoidable turns per firing × measured firings per week`. Rankings use
that product, then smaller implementation scope as the tie-breaker. These are
mechanical counterfactual estimates, not clean-context causal results.

### Study 1 — one decision packet for semantic preparation

**Prediction:** 1–3 turns saved per eligible episode.

An episode is one task-local preparation/extraction/movement flow beginning at
the first successful semantic preparation or plan action and ending at the
first apply, native mutation, explicit abandonment, human input, or task end.
Qualifying starts are `inspect_clojure` preparation/planning requests and CLI
`:ls-deps`, `:ls-extract`, `:change`, `:extract`, or `:mv` plans. Repeated
requests in the same flow remain one episode.

Candidate episodes are ordered by start time. If there are 10–20, score all.
If there are more than 20, score 20 evenly across the ordered population using
positions `round(i × (N − 1) / 19)` for `i=0..19`; report the population size
and sampled positions. Fewer than 10 candidates makes this study underpowered
and therefore **KILL**.

For every later query, classify:

- **Mechanically predictable** when its fact category, target, and selector
  follow from the original user-stated operation intent and facts already
  named at episode start. Examples are dependency closure, destination
  namespace requirements, affected callers, visibility, source guards, and
  the exact next-call envelope for an already-requested extraction or move.
- **Genuinely new choice** when a later result caused the caller to choose new
  scope, destination, public/private policy, caller-migration policy,
  replacement semantics, or a newly discovered target that the original
  intent did not determine.
- **Unclear** when privacy-safe evidence plus the allowed bounded intent check
  cannot distinguish the two. Unclear counts conservatively as new choice.

An episode is one-packet eligible only when every later query before its
terminal action is mechanically predictable. Avoidable turns equal those
later queries. Packet bytes equal all eligible read results plus framing.

**PASS** unless any preregistered kill fires. **KILL** if genuinely-new/unclear
choices occur in more than 50% of scored episodes, if more than 20% of
otherwise one-packet-eligible episodes exceed 65,536 bytes, or if the study is
underpowered.

### Study 2 — batchable independent reads

**Prediction:** `N − 1` turns saved per batch of `N` reads.

A read sequence is a maximal task-local run of at least two successful,
read-only Surgeon calls on one transport. Model reasoning/messages and
coordination may occur between reads. A mutation, human input, failed/refused
read, native source read, semantic-provider call needed to form the next
selector, or task end closes the run. Preparation episodes assigned to Study 1
are excluded to prevent double counting.

A sequence is **independent** only when every request's operation, target, and
selector was available from the user intent or task state before the first
read; no request consumes a name, range, hash, source anchor, dependency,
cardinality, or refusal learned from an earlier read. Disjoint targets are
evidence for independence, not proof. Exact/same-file targets are evidence for
dependency, not proof. Unclear sequences count as dependent. An independent
sequence is batchable only if one frozen-snapshot request can express every
member without changing semantics. Avoidable turns are `N − 1`.

The independence rate denominator is every eligible read sequence in the
window, including dependent and unclear sequences. The oversize rate
denominator is otherwise-independent structured-MCP sequences with complete
byte evidence.

**PASS** unless either kill fires. **KILL** if fewer than 20% of eligible
sequences are independent, or if more than 20% of otherwise-independent
byte-measurable sequences exceed 65,536 bytes.

### Study 3 — schema-discovery friction

**Prediction:** 0.1–0.3 turns saved per eligible task.

An eligible task is a Codex task turn or Claude session in the window with at
least one Surgeon action, semantic-provider action, or native Clojure read or
mutation. Synthetic/self-test/benchmark tasks are excluded. A routing-friction
cluster is one or more adjacent actions containing any of:

1. a Surgeon `:help`/`--help`, tool capability/schema discovery, or equivalent
   catalog probe;
2. a refusal or unsupported-operation error attributable to choosing an
   operation that cannot express the task shape; or
3. a switch to a different Surgeon operation or from Surgeon to a native or
   semantic route within the next two externally visible actions, where the
   first route performed no task result used later.

Transport recovery, stale-source refusal, malformed payload correction,
ordinary plan-to-apply progression, and a fallback caused by absent capability
are not table-routable friction. A compact task-shape-to-operation table earns
the cluster only if the original task shape maps uniquely to one currently
supported public operation and would have prevented the entire detour. If two
operations remain reasonable without a judgment call, classify not uniquely
routable. Each earned cluster contributes its redundant calls as avoidable
turns; overlapping signals form one cluster and are not double-counted.

The routing-friction incidence denominator is all eligible tasks, not only
Surgeon-using tasks. The table-routable rate is also reported as a diagnostic,
but the preregistered kill uses observed friction incidence as requested.

**PASS** unless the kill fires. **KILL** if fewer than 10% of eligible tasks
show at least one routing-friction cluster. A PASS recommendation is limited to
the smallest table rows that uniquely route observed clusters; it may not
claim unsupported capabilities.

## Results

### Executive verdict

All three hypotheses are **KILL** under their preregistered gates. No PASS
exists, so the ranked build-recommendation set is empty.

| Study | Verdict | Gate result | Removable turns in window | Measured boundary wall |
|---|---:|---|---:|---:|
| One decision packet | **KILL** | 14/17 episodes (82.35%) needed a new or unclear choice; kill is >50% | 0 | 0 |
| Independent read batching | **KILL** | 13/290 sequences (4.48%) independent; kill is <20% | 15 | 174.646 s |
| Schema-discovery table | **KILL** | 31/455 tasks (6.81%) had routing friction; kill is <10% | 38 | 228.338 s + one Claude boundary unknown |
| **Deduplicated total** | — | Study priority applied | **53** | **402.984 s + one unknown** |

The raw opportunity normalizes to 56.00 removable boundaries/week, but it is
split across hypotheses that all failed their incidence or decision-authority
gate. That is a useful stop: the retained traces do not justify building any
of the three proposed general mechanisms.

### Receipt, population, and exclusions

The one fixed-window collection completed with status `ok` under
`clj-surgeon.agent-usage-ethnography.v6`. Its privacy receipt says session
keys, logical arguments, structural targets, and source hashes were hashed;
transcript prose, workspace paths, argument/result content, raw service events,
and source were not emitted. The collector's privacy self-test passed before
collection.

The receipt covered 741 Codex task turns across 172 Clojure-relevant sessions
and 40 Clojure-relevant Claude sessions. Service telemetry contained synthetic
test activity, so service-wide refusal counts were not used as task counts.
The task-level reducers excluded 31 controlled benchmark tasks and three
incomplete tasks from Study 3. Study 2 additionally excluded ten sequences
owned by Study 1 and one controlled benchmark sequence. Study 1 found 17
natural preparation-to-apply episodes, so its preregistered 10–20 rule selected
the complete population rather than a sample.

Collection and adjudication used only `make study-agent-usage`, the receipt's
read-chain renderer, Python standard-library reduction of privacy-safe fields,
and bounded inspection of receipt-named task containers. No model, subagent,
transcript summarizer, or private-project search was used. The raw receipt and
temporary reduction helper remain outside Git.

### Study 1 — one decision packet: KILL

There were 17 qualifying preparation-to-apply episodes. Twelve contained at
least one later query and together exposed 27 later queries. Those observed
follow-ups occupied 353.691 seconds of post-tool boundary wall, but none met
the preregistered rule for removable waste: every multi-query episode either
introduced selectors not fixed by the original intent, followed a refusal, or
mixed an unstructured CLI read whose dependency could not be proved. Unclear
counts as new choice by preregistration.

| Episode (privacy-safe locator) | Flow | Later queries | Packet bytes | Adjudication |
|---|---:|---:|---:|---|
| `864d48eff939:64–80` | 3 reads → change | 2 | 16,959 | new selectors + refusal |
| `9d80768e121d:79–99` | 8 reads → change | 7 | 35,026 | new selectors + refusal + CLI |
| `9d80768e121d:129–133` | 2 reads → change | 1 | 12,902 | new selectors + CLI |
| `9d80768e121d:249–258` | 2 reads → change | 1 | 13,881 | new selector + refusals |
| `9d80768e121d:419–423` | 2 reads → change | 1 | 7,306 | new selectors + refusal |
| `9d80768e121d:436–440` | 2 reads → change | 1 | 11,118 | new selectors |
| `9d80768e121d:676–696` | 5 reads → change | 4 | 13,773 | new selector + CLI |
| `04c15cadf6f1:9–25` | 4 reads → change | 3 | 42,615 | new selectors + refusals |
| `04c15cadf6f1:57–131` | 4 reads → change | 3 | 13,038 | new selectors |
| `8390ecb01ee6:179–191` | 2 reads → change | 1 | 12,791 | new selectors + CLI |
| `c9a5b038dba1:150–156` | 1 read → change | 0 | CLI bytes unavailable | unclear CLI intent |
| `b06b6aff6565:33–43` | 1 read → change | 0 | 14,976 | refusal in flow |
| `b06b6aff6565:793–795` | plan extraction → extraction | 0 | 2,396 | mechanically complete |
| `b06b6aff6565:1052–1054` | plan extraction → extraction | 0 | 5,523 | mechanically complete |
| `c08f422a9be4:358–368` | 3 CLI reads → apply | 2 | CLI bytes unavailable | unclear CLI dependencies |
| `8a8a476842d9:169–184` | 2 reads → change | 1 | 7,327 | new selector |
| `8a8a476842d9:209–211` | 1 read → change | 0 | 2,828 | mechanically complete |

Fourteen of 17 episodes were new/unclear (82.35%), far beyond the 50% kill
line. The three mechanically complete episodes had no later query to remove.
None exceeded the packet bound, but output size cannot rescue an interface
that would have to preempt choices still owned by the caller. Measured firing
rate was 17.96 episodes/week; measured removable value was 0 turns/week.

### Study 2 — independent reads: KILL

The reducer found 290 eligible maximal read sequences after priority and
exclusion rules. Only 13 were provably independent from the first action's
known task state: **4.48%**, versus the 20% keep gate. Those 13 sequences
contained 28 calls, so batching would remove 15 boundaries (`N − 1`) and
174.646 seconds of recorded boundary wall.

| Measure | Result |
|---|---:|
| Eligible read sequences | 290 |
| Provably independent | 13 (4.48%) |
| Dependent or unclear | 277 (95.52%) |
| Independent calls | 28 |
| Removable turns | 15 |
| Independent firings/week | 13.74 |
| Predicted turns saved/week | 15.85 |
| Median combined packet | 30,882 bytes |
| Maximum combined packet | 64,374 bytes |
| Packets above 65,536 bytes | 0/13 (0%) |

Output bounds passed, narrowly: the largest independent batch was 1,162 bytes
below the limit. Independence decisively failed. Most same-route chains reused
or refined a prior file, owner, selector, source result, or refusal; CLI chains
lacked comparable frozen-request evidence and therefore remained unclear.
Building a general batch surface for 4.48% incidence would optimize the
exception rather than the observed route.

### Study 3 — schema-discovery table: KILL

There were 455 eligible tasks: 415 Codex task turns and 40 Claude sessions.
Thirty-one tasks contained at least one routing-friction cluster, for **6.81%**
incidence. That misses the 10% keep gate and also falls below the prediction:
38 redundant calls / 455 tasks = **0.0835 turns/task**, versus the predicted
0.1–0.3.

| Friction signal | Signals |
|---|---:|
| Help/capability call | 13 |
| Unknown operation | 14 |
| Read format required a different operation/spec | 6 |
| Positional mutation authority refusal | 4 |
| Public schema denied | 1 |
| Unsupported arguments | 1 |
| Conflicting edit input exposed through help | 1 |

Overlapping signals collapsed to 38 clusters. A compact task-shape table would
have uniquely routed 25/38 (65.79%): chiefly obsolete/unknown read names to the
current exact-form read, and format-specific reads to the structural query
surface. It would not uniquely route the remaining 13. Those required a
capability change, left both native and structural mutation reasonable, or
were help probes without one determinate next operation.

The 38 clusters represent 38 removable boundaries and 228.338 seconds of
measured Codex boundary wall; the single Claude help boundary has no compatible
clock and remains unknown. That is 32.75 friction tasks/week and 40.15
turns/week, but only because the task volume is high. The preregistered product
question was per-task incidence, and 6.81% kills the schema-table build.

### Decision and counterfactual limit

No proposed build advances:

1. Do not build a speculative one-decision semantic packet from these traces;
   it would seize caller decisions in 82.35% of observed episodes.
2. Do not build a general batching mechanism from these traces; proven
   independence was only 4.48%.
3. Do not expand the schema with the proposed routing table; friction occurred
   in only 6.81% of eligible tasks.

The 53-boundary total is a deterministic trace counterfactual, not a causal
speedup claim. It assumes each removed action also removes its recorded
post-tool boundary and does not induce replacement reasoning, transport work,
or a larger later recovery. A future hill may be reopened only with a new
preregistered mechanism or a changed usage population, not by relaxing these
gates after seeing the data.
