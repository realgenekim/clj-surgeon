# Captain's Log: The Model Typed Less; It Did Not Think Less

Date: 2026-08-29

Beads: `clj-surgeon-45j`, `clj-surgeon-45j.1`

Release: `19ab864889799b0028a5f7cb66c63b957ff7b973`

Stable tag: `stable-closed-relations-7.35x-native-20260829`

Decision: **PROMOTE. Closed relations earned the unchanged causal gate on
Anvil, survived exact verification, and are installed for the fleet.**

## The five-minute change became a 39-second change

The frozen job changes 51 exact things across nine Clojure files. Native tools
needed a retained midpoint of 289.507 seconds: nearly four minutes and fifty
seconds. The promoted closed-relation route completed in a 39.369-second
process-wall midpoint. That is 7.35 times faster.

The stricter causal comparison used two Surgeon requests against the same
candidate and the same public tool surface:

| Route | Complete verified midpoint | What the model had to state |
|---|---:|---|
| Normalized flat | 58.544 s | Every exact edit row |
| Closed relations | 38.647 s | The repeated relationships once |
| Reduction | **19.897 s / 33.99%** | Same 51 effects and nine files |

The native comparison tells us what a person experiences. The flat comparison
tells us what the new request language caused. We need both.

This changes which work an agent will attempt. A 51-edit cleanup is no longer
a long sequence of fragile text edits that an agent should avoid. It is one
explicit decision, one mutation call, one exact verifier, and one rollback
boundary.

## The mechanism surprised us

We expected the relation request to make the model think less. The retained
telemetry rejected that story. Median recorded reasoning tokens went **up** in
the faster arm.

What fell was visible output. The relation arm produced 51.0 percent fewer
visible output tokens. Across the four retained points, output-token count and
time to emit the call moved together with `R² = 0.99886`. `R²` is a fit score;
1.0 would be a perfect match in this small sample. The model predicted a
21.417-second reduction from output size. We observed 21.453 seconds.

For this frozen grammar, each authored request character cost about six
milliseconds of wall time. That is not a universal model constant. It is a
sharp local result: **the model typed less; it did not think less.**

The relation kept identity explicit while removing repetition. Instead of
restating the same file, owner, old symbol, new symbol, and match count across
dozens of rows, the request stated the relationship once. A pure compiler
expanded it into the same ordinary edit transaction. The existing engine still
owned path resolution, stale-source guards, overlap refusal, mutation,
read-back, verification, receipt, and rollback.

## Smaller was not enough

Several experiments stopped us from telling the easy but wrong story that
fewer bytes always make a tool faster.

An earlier treatment removed 63.7 percent of the visible tool description and
schema. It improved complete wall by only 5.2 percent and was 5.2 percent
slower before the first call. The model still had to make and emit the same
decision.

The file-groups request was also smaller than the flat request and slower. Its
emission midpoint was 83.703 seconds versus 65.841 seconds for flat rows. A new
grammar can cost more to understand than it saves in output.

The useful rule is narrower:

> Compress repeated expression inside a grammar the model can use correctly.
> Do not replace explicit identity with an opaque index, and do not assume that
> a smaller schema removes decision work.

That rule explains why unchecked file indexes were rejected even when they
compressed well. A wrong but valid index can select the wrong owner and then
pass every content, parse, read-back, and verification guard. Compression may
remove repetition. It may not remove subject identity.

## We found the reachable part of the clock

A separate 185-boundary study divided the time after a Surgeon result and
before the agent's next action:

| Observed segment | Share |
|---|---:|
| Recorded reasoning | 47.6% |
| Before the first recorded reasoning item | 19.7% |
| Between recorded reasoning items | 2.6% |
| After the last reasoning item | 22.5% |
| Overlapping background work | 7.6% |

The labels describe event clocks, not hidden thoughts. The post-reasoning
22.5 percent is consistent with constructing and emitting the next action.
Closed relations attacked that segment directly.

The rest is not automatically ours. In another experiment we handed the model
the complete 608-byte call. That saved about 2.2 seconds, but roughly 11
seconds still remained before the call. Scheduling, input processing,
inference, transport, and other service work can create a fixed floor that a
tool schema cannot remove.

This is why the 33.99 percent gain is large but not magical. We found one
expensive, reachable segment and made it smaller without moving its work into
another turn.

## The win is credible because we refused it twice

The performance number is one line. The method is the reusable result.

1. An early cohort produced an exciting reduction but failed its frozen
   admission law. We retained it and did not rescore it into a pass.
2. Adversarial review found a scorer weaker than its approved contract. We
   repaired the scorer before it could bless a cohort.
3. We denied raw payload size any promotion authority. Fewer bytes were a
   hypothesis, not a verdict.
4. A later row produced the correct future and exact verification but expressed
   the same disjoint effects in a different order. The scorer judged request
   order instead of proven effect identity. We stopped, designed the identity
   law, added overlap and permutation falsifiers, and started a new cohort.
5. The laptop reached load average 664 because five abandoned home-directory
   scans were still running. We did not benchmark through the storm. We found
   and removed the cause, then ran the acid test on quiet Anvil hardware.

Every stop was a chance to publish a dramatic number that would not survive
scrutiny. The refusals converted those chances into permanent tests.

## The acid test

The final candidate ran eight fresh Sol/high attempts on Anvil dev-a:

```text
Block 1: normalized  relation  relation  normalized
Block 2: relation    normalized normalized relation
```

This counterbalanced order makes each route run early, middle, and late. It
prevents one route from winning only because the service was faster during one
part of the experiment.

All eight attempts were correct, route-adherent, and exactly verified. Each
used one first-action `apply_clojure_changes` call. The thresholds never moved.

| Gate | Block 1 | Block 2 | Pooled |
|---|---:|---:|---:|
| Time to emit the call | 40.21% faster | 37.75% faster | **37.90% faster** |
| Complete verified time | 35.72% faster | 34.71% faster | **33.99% faster** |

The scorer returned `cohort-valid=true`, `block-2-authorized=true`, and
`promote=true`. We did not retry a losing position, omit an ugly row, or tune
the threshold after seeing the result.

## The product survived publication

The release combined the performance mechanism with the positional-mutation
safety repair. The milestone suites passed before publication. One
`make install` updated the fleet's CLI, analyzer gate, skills, and routing.

One synchronized MCP reload published the new contract without restarting the
server. PID `65458` remained live. A fresh installed-tool canary then compiled
and committed all 51 effects across nine files, ran the exact project verifier,
and returned complete receipt evidence in 1.635 seconds of server time. The
pre-existing MCP session remained usable afterward.

The 1.635-second canary is not a model-wall benchmark. It proves that the
installed transaction engine can execute the request safely and quickly once
the decision arrives.

## Where Surgeon should not win

The advantage has a task-shape crossover:

| Task size and shape | Retained outcome |
|---|---:|
| Six small edits | Near a tie |
| Seventeen owner deletions | About 1.66x faster |
| Thirty structural edits | About 3.17x to 5.80x faster |
| Fifty-one edits across nine files | About 4.94x to 7.35x faster |

If a visible patch fits in one read and one clear edit, native tools remain the
right choice. The fixed model/service floor can dominate small work. Below
roughly a dozen edits, do not reach for a relation merely because it exists.

Surgeon wins when ownership, repetition, atomicity, exact verification, and
rollback compound across many effects. It does not win by taxing every small
change with a large structural ceremony.

## Work-log ledger

- **Option created:** a closed relation that states one symbol migration and
  one require change while keeping every file, owner, symbol, and count
  explicit.
- **Reversible ratchet:** a pure facade lowers the relation to the existing
  transaction engine. The change added no second executor, cache, or mutation
  authority.
- **Evidence:** eight fresh counterbalanced Anvil attempts, 8/8 correct and
  route-adherent, both blocks above the unchanged gates, plus one installed
  51-effect canary.
- **Counterfactual:** normalized flat used the same candidate, model, task,
  public surface, verifier, and transaction engine. Only the request
  representation changed.
- **Surprise:** recorded reasoning tokens rose. Visible output fell, and output
  size predicted the emission-time delta almost exactly in this small cohort.
- **Falsifiers:** schema shrink, file groups, opaque indexes, order-sensitive
  scoring, dirty evidence, and high-load benchmarking all failed or stopped
  before promotion.
- **Decision:** promote closed relations for large, repeated structural
  decisions. Preserve native routing for small visible patches.
- **What becomes cheaper next:** test other explicit repetition-removing
  grammars, and use the new regression sentinel to detect any future loss of
  this 39-second route. Do not chase the roughly 11-second service floor with
  more schema ceremony.

## Evidence trail

- [The original HOLD and superseding acid test](2026-08-29-captains-log-closed-relations-earned-a-hold.md)
- [The valid-win protocol and mechanism correction](2026-08-29-captains-log-the-valid-win-path.md)
- [Why schema shrink alone failed](2026-08-29-captains-log-the-schema-shrank-but-the-decision-did-not.md)
- [The action-boundary decomposition](2026-08-29-post-surgeon-boundary-decomposition.md)
- [The 11-second fixed-floor experiment](2026-08-29-captains-log-two-seconds-were-call-construction-eleven-were-not.md)
- [The performance timeline](2026-08-28-performance-vs-native-timeline.md)

The durable lesson is not “make the JSON smaller.” It is: make the complete
decision cheaper to express, keep identity explicit, refuse evidence that does
not prove the claim, and let one verified transaction own the effects.
