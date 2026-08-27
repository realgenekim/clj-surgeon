# Captain's Log: the model showed its decision boundary

**Observed through:** 2026-08-26 16:52 PDT / 23:52 UTC  
**Stable installed release:** `66b8a606e44786bb8a835d7bcd79fc3da3c15afc`

## Mission

Make the time outside Surgeon visible, determine which work requires model
judgment, and move only repeated mechanical work behind snapshot-bound tool
contracts. Optimize complete verified task time, not Surgeon adoption or raw
server latency.

The bitter-lesson direction is not to make Surgeon guess more. Give a stronger
model complete evidence, an expressive but compact action surface, and exact
safety boundaries. Let deterministic code perform copying, serialization,
fencing, execution, and proof.

## What became visible

One nearby inspect-to-apply trace exposed the model boundary precisely:

```text
07:47:24.523  inspect result complete
      |
      +-- 4.119 s  service scheduling / context ingestion
      +-- 3.481 s  recorded reasoning phase
      |              183 reasoning tokens
      +-- 4.969 s  tool-call generation / serialization
      |              469 output tokens
      v
07:47:37.092  apply submitted
```

The model's visible reasoning summary was empty. The rollout still recorded
reasoning-token counts, output-token counts, cache use, and phase timestamps.
The request carried 168,175 input tokens, of which 166,912 were cached.

This does not expose private chain of thought. It is more useful operationally:
it separates model reasoning, model-visible serialization, service delay, tool
execution, and stopping behavior. A compact requested decision checkpoint can
make the unresolved variables visible without asking for prose reasoning.

The frozen extraction benchmark had a 51.322-second complete wall. Plan and
apply consumed 12.823 seconds of server time. The remaining 38.499 seconds are
not yet one proven "deliberation" bucket: the exact lint wall was not separately
reported, and service scheduling, prompt ingestion, model reasoning, argument
emission, receipt interpretation, and final response all share the remainder.

## Prompt intervention

The smallest proposed prompt change is a decision algebra, not "think harder"
and not a fixed call sequence:

```text
Extraction routing:
required = {source, destination, owners, visibility, callers, verifier}
unknown = required - supplied

If unknown is empty, perform the direct guarded extraction once and stop when
the commit receipt and supplied verifier succeed. Otherwise plan only for the
unknown fields. Do not rediscover, restate, or reread supplied fields.
```

The model may emit a bounded experimental checkpoint:

```json
{
  "unknown": [],
  "semantic_decisions": ["publicize not-blank"],
  "mechanical_work": ["copy snapshot hash", "encode callers"],
  "chosen_action": "apply frozen plan",
  "stop_when": ["atomic receipt", "exact verifier passes"]
}
```

This checkpoint is an experiment, not a permanent production requirement.
Requesting it changes the behavior being measured and costs output tokens. Cap
it at 50-80 tokens and measure its own wall and token cost separately.

## Concrete Surgeon response contract

Prompting can help the model recognize a complete decision. A compact result
shape removes the mechanical work after recognition. The transport-neutral
planner should be able to return:

```clojure
{:status :ready
 :plan-id "extract:sha256:..."
 :snapshot {:source "src/example/core.clj"
            :source-sha256 "..."
            :destination "src/example/moved.clj"
            :destination-state :absent}
 :decision {:owners ["alpha" "beta"]
            :publicize ["beta"]
            :require-policy :minimal
            :caller-changes 1
            :ignored-callers 0
            :affected-files 3}
 :evidence {:owners-resolved "2/2"
            :dependency-closure :complete
            :callers-returned 1
            :callers-omitted 0
            :ambiguities []
            :truncated false
            :authority :structural}
 :derivable #{:owner-source :caller-sites :expected-counts :ns-requires}
 :unknown []
 :verifiers [{:id :lint-exact :command ["clj-kondo" "--fail-level" "error"]}]
 :apply {:operation :apply-plan
         :plan-id "extract:sha256:..."
         :decisions {}}
 :evidence-ref "receipt:sha256:..."}
```

Contract laws:

- `:ready` is forbidden when evidence is truncated, omitted, stale, or
  unresolved.
- Owner names remain visible and reviewable; large bodies and candidate detail
  can live behind `:evidence-ref`.
- `:unknown` names exactly what still requires model judgment.
- `:derivable` names fields the model must not retranscribe.
- Similarity and structural candidates remain evidence, never write authority.
- The model authorizes mutation by accepting the hash-bound plan or supplying
  explicit changed judgments.
- Apply expands the plan server-side and refuses a changed snapshot.
- A supplied exact verifier remains exact. Surgeon must not substitute a
  generic verification profile; the rejected `verify=fast` experiment proved
  that such substitution changes semantics.

### MCP projection

`inspect_clojure` structured content should expose the complete decision packet.
Its text projection should remain short and source-free:

```text
ready · 2/2 owners · 1 exact caller · 0 ambiguities
unknown: none
next: apply plan extract:sha256:...
```

`apply_clojure_changes` should eventually accept `plan_id` plus only changed
decisions. A successful response should name the subject, snapshot/evidence
source, exact verifier result, rollback receipt, and
`required_next_actions=[]`. That makes terminal evidence structural rather
than a prose convention.

The MCP server cannot by itself hide unrelated Codex tools. Dynamic
`allowed_tools` is a caller/orchestrator capability. A benchmark may expose
only inspect, apply-plan, and the exact verifier during the extraction phase,
but this must be a separate factor from prompt and response-shape changes.

### CLI projection

The CLI must use the same transport-neutral decision compiler and return the
same EDN fields. Human output may render the short summary and an executable
command:

```text
clj-surgeon :op :apply-plan :plan-id extract:sha256:...
```

CLI and MCP parity covers owner vocabulary, hypotheses, ambiguity, snapshot,
unknown fields, plan identity, and verifier identity. Process exit semantics,
transport envelopes, and text formatting remain entrance-specific.

### Refusal projection

A correctable refusal should carry a hash-bound repair proposal or minimal
delta. It must never apply the repair automatically. Stale or ambiguous
evidence returns `:ready false`, names the failed owner/stage, exposes complete
bounded alternatives, and preserves `authority=false` for fuzzy hypotheses.

## Last 24 hours: owner-hypothesis evidence

Window: 2026-08-25 16:52 PDT through 2026-08-26 16:52 PDT.

The repository recorded 21 `batch-form-selection-failed` MCP events during the
window, but most predated the stable selector-recovery publication or belonged
to development replays. They are not 21 proven production Levenshtein wins.

The historical corpus contained 15 selector failures and eleven useful
patterns. Skeptical review retained only six strict one-to-one corrections for
acceptance. The broader eleven-case table remains exploratory.

| Corpus | Rank@1 | Rank@3 | Rank@10 | MRR |
|---|---:|---:|---:|---:|
| Normalized Levenshtein, broad 11 | 4/11 | 7/11 | 9/11 | 0.5009 |
| Normalized Levenshtein, strict 6 | 50.0% | 83.3% | 100% | 0.644 |

More complex token rankers improved ordering but did not improve strict
top-ten recall. The rejected broad ranker cost 780 lines. The accepted option
is normalized Levenshtein in a small transport-neutral implementation, top ten
per missing owner, always `authority=false`, backed by the complete bounded
owner vocabulary.

Two clean-context replays demonstrated the intended route:

- six-owner case: intended correction ranked first;
- 31-owner case: intended correction ranked seventh, proving that rank one is
  not authority;
- both corrected requests succeeded from refusal evidence alone;
- neither recovery required `rg`, outline, or `sed` discovery.

Mechanism timing was 2.35 seconds for the four-file two-call replay and 0.74
seconds for the 31-owner two-call replay. There was no matched native wall-time
arm. The defensible performance claim is 3-5 calls reduced to two, or 33-60%
fewer calls, not an end-to-end speedup ratio.

Stable shared publication completed at 2026-08-26 09:03 PDT. Since publication,
shared telemetry contains two tool calls and one selector refusal with
Levenshtein evidence. That refusal was the required release proof:

```text
requested: resolve-source-file
rank 1:   resolve-source-path
owners:   6 returned, 0 omitted
authority: false
service wall: 21.48 ms
```

An external installed-CLI proof returned the same six owners and rank-one
hypothesis. No organic post-publication hallucination has yet been observed,
so field benefit remains unmeasured. The live result proves publication and
parity, not fleet frequency or recovered task wall.

## Factorial experiment

Use the same frozen snapshot and precomputed semantics. Vary only prompt and
result representation:

| Arm | Prompt | Result shape |
|---|---|---|
| A | current | verbose manifest / `next_call` |
| B | decision algebra | verbose manifest / `next_call` |
| C | current | compact hash-bound `plan_id` packet |
| D | decision algebra | compact hash-bound `plan_id` packet |

Run fresh Sol/high sessions in counterbalanced order. A three-replicate learning
cohort per cell may reject weak options; promotion requires a larger paired
cohort.

Primary mediator:

```text
plan evidence arrival -> valid apply call start
```

Record reasoning tokens, visible output/tool-argument bytes, scheduling delay,
reasoning duration, serialization duration, complete verified task wall,
correctness, extra reads, refusals, verifier equivalence, and terminal stopping.

Current projections are hypotheses:

- prompt only: save 1-3 seconds;
- compact result only: save 5-8 seconds;
- combined: save 7-10 seconds at the pre-apply boundary;
- evidence-to-apply median target: at most three seconds;
- visible result and payload bytes target: at least 60% lower.

Any correctness regression, weakened verifier, false-complete result, stale
snapshot acceptance, automatic fuzzy selection, or added retry vetoes the
change regardless of speed.

## Decision

Instrument before optimizing the unexplained remainder. Test the lean generic
prompt, but expect the hash-bound result shape to be the stronger intervention.
The prompt helps Sol recognize what is unknown. The packet removes the work of
copying everything already known.

The next hill is not "show us every thought." It is:

```text
make the decision boundary observable
-> supply the evidence before the model asks
-> preserve only genuine model judgment
-> compile the rest into one guarded action
```
