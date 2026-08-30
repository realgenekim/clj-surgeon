# Turn-waste mining: preregistration and results

<!--
Preregistration status: frozen before collection or counting.
Protocol commit: filled after this file's preregistration-only commit.
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

Results intentionally omitted in the preregistration commit.
