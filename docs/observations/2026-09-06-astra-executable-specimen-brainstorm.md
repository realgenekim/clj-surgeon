# Astra: preserve executable decisions across handoffs

2026-09-06, Gene's 20:57Z one-hour brainstorm. One archived-data hand-drive,
not a new actor cohort. Prototype and checks completed at 21:04:39Z.

The breakthrough I would pursue is removing repeated translation of the same
decision: discovery becomes an exact guarded selection; that selection becomes
a mutation; the actual request and proof become the handoff. Let the model supply
judgment once and let ordinary programs preserve the bookkeeping. This is a
systems hypothesis, not a measured 10x result.

## Eight possibilities, with their native competition

1. **Executable handoffs:** immutable request/selection values consumed by the
   next action, without asking the model to reconstruct owners and counts.
   Native scripts already preserve data; use existing plans before adding APIs.
2. **Proof dependency graph:** rerun invalidated checks using declared dependencies.
   Charge graph preparation; guessed changed-namespace coverage is inadequate.
3. **Native patch plus test continuation:** return actual assertions from an
   explicitly chosen repository command. Strong native callers already batch this.
4. **One structural relation across owners:** express one transformation over an
   identified set. Preserve comments and unrelated bytes; compete with a script.
5. **Tiny repository contract:** cache repeatedly needed test/dispatch facts with
   explicit invalidation. Compare against the same plain-text brief for native.
6. **Failure at the repair site:** inline expected/actual, candidate identity and
   mutation status. Fix dead report paths; do not claim surrounding minutes saved.
7. **Speculative declared proof:** start authorized checks while the model reviews;
   charge wasted runs and contention, and compare with native background checks.
8. **Executable examples:** publish the exact request that ran. Selected for this
   short prototype because we observed a concrete real-session failure tonight.

## Real-session witness

This is the working root session, not an experiment actor. A bounded timestamp
seek examined 172 records (19:56–20:15Z); local evidence is
`/var/tmp/forge/astra-brainstorm-fx/real-session-region.json` and
`real-session-case.md`. No broad collector or new provider run was launched.

Strict-3's published read request named two distinct files but expected three;
its execution evidence used three distinct files. Published fan-out entries also
differed from executed entries. Re-authoring the example destroyed request identity.

| Observed root event | UTC |
|---|---|
| Read strict-3 | 20:07:33.370 |
| Read retained requests | 20:07:56.221 |
| Write mismatch correction | 20:08:38.667 |
| Read strict-4 | 20:11:32.021 |
| Compare committed blocks to retained requests | 20:11:46.568 |
| Write GO | 20:12:10.172 |

Correction to GO took **211.505 seconds**. This includes scheduling, other copy
fixes, policy consistency and review; it is **not a measured causal saving**.

## Hand-drive and complete accounting

Two small scripts read the three actual retained request/result pairs, check
request equality, success and bounded cardinalities, and render original request
bytes plus artifact hashes. They validate the entire batch before emitting output.
No server call, mutation, provider, installation or replay occurs.

Five alternating-order pairs started a fresh process per invocation on the shared
box. Both arms did the same work. Runtime/file caches were not flushed. These
are subprocess component clocks, not isolated benchmarks or agent task clocks.

| Arm | Five walls, milliseconds | Median |
|---|---|---:|
| Native Node JSON script | 30.327, 28.500, 26.005, 27.354, 29.699 | 28.500 ms |
| Babashka Clojure-data script | 15.382, 12.878, 14.828, 13.245, 14.765 | 14.765 ms |

Babashka is **1.93x** faster here, an absolute **13.735 ms** difference. This
does not establish a Surgeon advantage: both are native scripts and the runtime
startup difference is immaterial beside review wall.

All ten positive runs succeeded and produced three byte-identical source examples.
Four deliberately bad cases per arm all refused with exit 2 and no partial output:
request/receipt drift, two-files/expect-three, failed receipt, receipt-count mismatch.
The count case updates the wrapper request too, so it tests cardinality rather
than merely failing identity first. Two initial positive smoke invocations preceded
the timed pairs; all succeeded. No failed attempt was discarded.

From the 20:58Z block acknowledgment to verified prototype receipt was about
6 minutes 40 seconds, including design, trace inspection, writing both scripts and
the harness. Reporting and independent review followed; the ledger records final
preparation-to-close wall separately. Actor/model token cost for this block is
unknown; no external typist/provider experiment was run.

Independent bounded review: GO for trusted archived fixtures. It correctly limits
the fan-out `edits == sum(matches)` interpretation to these all-one specimens.
Hashes identify supplied files, not independently authenticated execution. General
schema/size bounds are absent; BB hashes decoded text while Node hashes raw bytes
(the retained ASCII inputs agree). These are research scripts, not a production
validator, freshness check, proof of semantic correctness or permission to replay.

Scripts and raw clocks are preserved in
[the specimen directory](2026-09-06-astra-executable-specimen/).
The initial manifest pointed to retained local artifacts. The subsequent go90
keeper task checked those exact fixture files into the specimen directory and
made paths manifest-relative; see its README for offline reproduction. The
historical clocks remain unchanged. The raw request and receipt hashes appear in generated
`/var/tmp/forge/astra-brainstorm-fx/results/rendered.md`.

## Recommendation to Gene and Fable

Keep native as the default. Adopt the small convention that executed artifacts
supply examples and handoffs; it caught the actual mistake without a new tool API.
Do not market the 1.93x rendering ratio or the 211-second review interval as task
speedups. A fresh task comparison is still needed to measure prevented rework.

The larger Clojure bet remains promising where a relation over many forms replaces
an explicit edit list. That is where source-as-data can remove model bookkeeping.
JSON rendering itself gives Clojure no exclusive advantage. This follows the bitter
lesson by spending general model capability on decisions and ordinary computation
on preservation, rather than teaching another command taxonomy.

For the broader real-session study, distinguish critical-path delay from concurrent
agent execution, user absence, recovery stalls and intentional review. Inter-call
gaps do not identify thinking; repeated-read counts do not identify avoidable wall.
Do not sum overlapping fleet intervals into recoverable task hours. Work queues,
warm proof and tiny briefs are hypotheses worth comparing under those boundaries.

Direct method review of the broader study at `53353853` found concrete reasons
for caution: its regex counts `bb` as a JVM command; any instruction inside a
gap labels the entire gap idle; other gaps become generation by elimination;
call durations are summed without unioning overlaps. Its repair rule is the next
call after a heuristically detected failure. Those labels do not establish cold
start cost, avoidable waiting, model compute or the prevalence of actual repair.
These corrections were sent to Fable for the report itself. A useful next test
needs process lifecycle markers, overlapping interval accounting and a task
dependency path, not an infrastructure project justified by those percentages.
