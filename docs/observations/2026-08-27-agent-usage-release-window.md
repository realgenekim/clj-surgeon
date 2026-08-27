# Agent usage after the five-times-native hill climb

<!-- agent-usage-window-end: 2026-08-27T07:31:45.757004Z -->

**Study window (UTC):** 2026-08-27 02:24:00 through 2026-08-27
07:31:45.757004  
**Study window (Pacific):** 2026-08-26 19:24:00 through 2026-08-27
00:31:45.757004 PDT  
**Receipt:**
`/tmp/clj-surgeon-agent-usage-20260827T022400Z-20260827T073145757004Z.json`

## Question

After the frozen 15-owner extraction crossed the five-times-native gate, what
did agents actually spend time doing? Did the release remove the expensive
route boundaries, and what is the next smallest falsifiable improvement?

## Sampling and exclusions

The repository collector joined local Codex and Claude histories with
clj-surgeon MCP, cclsp, and clojure-lsp telemetry for the marker-bounded
window. Session keys are hashed. Transcript prose, workspace paths, and raw
service events are excluded.

This is an observational product-development window, not a randomized editor
comparison. It contains implementation, failure-injection tests, live release
proofs, and ordinary reads. The refusal totals therefore do not estimate a
production failure rate. Task-turn boundaries are reconstructed from provider
events; one long-running campaign can span many actions within one recognized
turn. Claude had no Clojure-relevant session in this short window, so the
provider comparison is coverage telemetry only.

The causal speed claim comes from the frozen counterbalanced extraction
benchmark, not this aggregate. That benchmark held the task, semantic scorer,
model, and native control fixed. Its promoted Surgeon median was 19.216
seconds versus 122.278 seconds for correct native, or 6.36x. Every promoted
arm remained below the 24.456-second five-times-native gate.

## Provider scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 8 | 2 |
| Clojure-relevant sessions | 5 | 0 |
| Recognized task turns | 25 | 0 |
| Surgeon calls | 71 | 0 |
| Native Clojure patches | 65 | 0 |
| Native Clojure shell/exec actions | 213 | 0 |
| Surgeon output characters | 585,743 | 0 |

The zero Claude column does not mean Claude rejected Surgeon or that Codex was
faster. No Claude session in the window entered the comparison population.

## Actual operations and fallbacks

The Codex transcript reported 69 `inspect_clojure` calls and two compact
`edit_clojure` calls. The server recorded 69 inspections and four public
`apply_clojure_changes` calls because the release proofs exercised public
normalization and transaction paths directly. Codex also performed 213 native
read-route actions and 65 native patches. This was a hybrid development route,
not a Surgeon-only route.

Inspection remained fragmented: 57 server batches had a median of one file
and one request, p90 four, and maximum five. The server returned 355,346 source
characters across 133 file reads. There were 125 form requests and 14 outline
requests. Broad discovery and release archaeology still produced many native
reads around the structural calls.

The server recorded 60 successful calls and 13 refusals. The transcript joined
31 refusal actions because several permanent tests deliberately generated
multiple typed failure witnesses. Examples include stale hashes, invalid
project profiles, failed verification, receipt publication failure, and undo
read-back failure. These are success-path safety tests, not evidence that
ordinary callers fail at those rates.

## Direct tool wall versus complete-turn wall

| Clock | Count | Median | p90 | Total |
|---|---:|---:|---:|---:|
| MCP server, all calls | 73 | 115 ms | 476 ms | 14.758 s |
| `inspect_clojure` server wall | 69 | 111 ms | 464 ms | 12.347 s |
| `apply_clojure_changes` server wall | 4 | 435 ms | 1.426 s | 2.411 s |
| Codex-visible Surgeon action wall | 71 | 224 ms | 582 ms | 19.906 s |
| Native `apply_patch` action wall | 65 | 692 ms | 1.327 s | 51.963 s |

Five recognized turns used Surgeon. Their median complete wall was 414.129
seconds, with 13 actions and seven route phases. Median Surgeon tool-wall share
was only 0.34%. The one native-patch/no-Surgeon turn is too small a cohort for
a comparison. The useful conclusion is narrower: in these complex development
turns, shaving another 100 milliseconds from an individual MCP call cannot
materially change complete verified task time. Route fragmentation, model
boundaries, verification boundaries, and native fallback dominate.

## Successes and failures in this window

The campaign deleted two model-managed phases on the frozen extraction:

1. an exact object skeleton made the mutation call the first emitted action;
2. repository-owned exact verification moved inside the atomic transaction;
3. a conditional terminal response let the caller relay completed evidence
   without spending another six seconds narrating the receipt.

The final counterbalanced product cohort moved from a 25.066-second PRE median
to 19.216 seconds POST, a 23.3% reduction. Both POST arms were semantically
correct and exact final-response relays. Compound-task sentinels proved that
the caller continued when required work remained. The live shared-server proof
then exercised both branches: compact edit omitted the relay, while a complete
verified extraction returned it exactly once. The existing MCP process and a
pre-existing Codex session survived the hot publication.

Important stops stayed stopped:

- a generic `verify=fast` changed verifier semantics and was rejected;
- a compact plan handle did not meet its boundary-reduction gate;
- a broad fuzzy ranker added machinery without authority;
- fewer tool schemas were slower in the small cohort;
- formatter optimization could not explain the model-controlled interval;
- a cheaper model saved only 2.853 seconds in a small screen and was not the
  target of this Sol/high campaign.

The initial absolute requirement for receipt interpretation below 3.000
seconds also missed by 154 milliseconds. It was not relabeled as a pass. After
independent review, the intent changed to a paired improvement requirement:
POST had to improve at least 30% versus contemporaneous PRE while every arm
still met the complete-wall gate. The measured paired reduction was 44.6%.

## Independent review and counterfactual limits

Codex Sol and Fable independently ranked observable item clocks,
verifier-boundary fusion, and kernel profiling as the best first experiments.
Fable challenged the missing noise floor, scorer pinning, cached-token
evidence, and small final cohort. Direct transcript inspection also falsified
Fable's initial suggestion that retained event streams already contained the
needed timestamps. This was a real adversarial review, not only a planned
portfolio row.

The release proves an existence result for one large, fully specified
extraction. It does not prove 6.36x across ordinary edits, exploratory work,
unknown-caller refactors, or another model. The current history also cannot
compare Codex with Claude, estimate production refusal incidence, or attribute
the 414-second median Surgeon turn to the server. The native control remains a
frozen matched-correctness counterfactual for the benchmark only.

## Product goal and next gate

The product goal remains complete verified task time, not Surgeon adoption.
The five-times-native gate is now met for the frozen 15-owner extraction. The
next product hill should target the observed read route: 69 structural reads,
213 native read phases, and median inspection batch width one.

The smallest falsifiable improvement is a compiled read-decision chord on a
mechanically sampled historical cohort. When exact files and owners are known,
compare the current route with one batched, snapshot-bound inspection that
returns guard-ready anchors for the next edit. Require:

- identical selected source evidence and final semantic outcome;
- no additional write authority from similarity or partial reads;
- no native discovery fallback on the mechanically resolvable stratum;
- at least 20% lower complete verified wall and at least 50% fewer read-route
  phases in a counterbalanced screen;
- safe, complete refusal when ownership is genuinely ambiguous.

If that screen fails, keep inspection as a bounded primitive and invest in
agent routing rather than a larger read compiler. If it passes, expand only
after a representative historical corpus shows the gain survives outside the
hero extraction.
