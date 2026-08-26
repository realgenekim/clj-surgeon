# Captain's Log: The Extraction Plan Became a Next Call

## Entry

The compiled-edit advantage generalized again before extraction planning was
implemented. On the real single-file public-CFP cleanup, fresh Sol/high callers
completed the compact route in a median 30.418 seconds versus 176.346 seconds
for native editing: 5.80x faster. Every compact arm made one successful MCP
mutation, preserved meaning-bearing source, and had zero refusals, shell
fallbacks, or failed mutations.

That result changed the next question. Owner-level intent already wins when the
decision is supplied. Can Surgeon also make the planning of a real namespace
extraction cheap enough that a model reaches one compiled transaction instead
of reconstructing the migration manifest across CLI, reads, and patches?

## The missing seam was smaller than expected

MCP already had the failure-atomic extraction executor. CLI already had the
pure non-mutating planner. The gap was an adapter, not an engine:

```text
inspect_clojure plan-extraction
  -> exact form closure
  -> complete structural caller evidence
  -> frozen source hash
  -> ready-to-fill next_call

model supplies caller changes or explicit ignores

apply_clojure_changes
  -> compare source hash
  -> derive aggregate bookkeeping
  -> compile and commit once
  -> terminal reversible receipt
```

Planning and apply now enumerate source through one shared workspace boundary.
The read adapter calls the same `extract/compile-plan` used by execution, strips
private future bytes, reports exact evidence counts, and grants no write
authority. The apply adapter accepts the plan's SHA-256 source identity and
refuses stale source before compilation. Callers no longer repeat form, edit,
and file totals that the kernel can derive from stronger per-intent guards.

This is a high-option-value change: shared source enumeration, pure manifest
projection, inspect transport, and apply fencing are separate testable seams.
The executor remained singular.

## Live dogfood

The new handler and schema hot-reloaded into the existing MCP process; port
7888 did not restart. A real repository plan returned all three structural
callers and a guarded next call in 895.78 ms.

A disposable end-to-end extraction then completed as two public calls:

| Phase | Result | Server wall |
|---|---|---:|
| Plan | one moved owner, one exact caller candidate, complete evidence | 421.61 ms |
| Apply | owner moved, caller rewritten, three files read back, reversible receipt | 12,570.92 ms |
| Apply structural work excluding formatter | inferred from reported phase clocks | about 510.75 ms |

The apply succeeded without a caller-supplied aggregate `expect`. It moved the
owner, rewrote the fully qualified caller, formatted the three future files,
parsed and read them back, and returned `verification_complete=true` plus
receipt hash `b8781e5c9e87633fc438462473f95581a43a8c0fe481b8c54d0f1a1e132e5ed8`.
The disposable source was then removed.

The formatter consumed 12,060.17 ms—96% of apply wall. Structural planning and
mutation are now subsecond on this small case; formatter process startup is the
dominant server cost.

## The ugly evidence stayed visible

The first disposable owner was named `moved`. The existing structural caller
scan returned 29 candidates across the repository, only one relevant. This was
safe and complete, but filling 28 explicit ignores would destroy the ergonomic
win. Renaming the disposable owner to a unique field name reduced the candidate
set to exactly one and allowed the execution proof to proceed.

This is not a reason to hide or truncate candidates. It is evidence for the
next hill climb:

1. distinguish namespace-qualified references from same-spelled unqualified
   forms without weakening complete evidence;
2. keep quoted-Var and semantic evidence authority-labelled;
3. make exact false-positive reasons visible so the model can dismiss a group
   in one bounded decision; and
4. benchmark candidate quality on historical extraction roots, not synthetic
   unique names.

## Independent convergence

SURGEON2's isolated CLI/MCP parity audit reached the same design at commit
`f61a288`: compose the existing dependency and extraction compilers into one
read-only snapshot-bound manifest, keep semantic enrichment separate, and
reject a second extraction engine or CLI subprocess. Its projected route
compression is 75–80% for one root and 87.5–88.9% for three roots. Those are
projections, not measured wins; our next frozen cohort must admit complete wall,
bytes, authority, and correctness.

## Verification at this checkpoint

- warm focused loop: 93 tests, 851 assertions, zero failures;
- cold core milestone: 609 tests, 5,239 assertions, zero failures;
- cold MCP milestone: 223 tests, 1,834 assertions, zero failures;
- linked-intent Prolog oracle green;
- 512 MiB heap regression green;
- cclsp transient-health and launch-path regressions green;
- live plan and apply receipts terminal;
- no MCP server restart.

## Next hill

Freeze the 15-form historical `format` extraction from the Sessionize views
split. Measure supplied-plan execution separately from plan-plus-execute. The
first post-change cohort compares native, CLI-assisted, and MCP-manifest routes
with the same Sol/high model and correctness gates.

Before scaling, test whether formatter reuse or an already-running formatter
can remove the observed 12-second fixed cost without weakening rollback. Also
measure the real candidate precision of the 15 form roots. The product wins
only if complete verified task time falls; a beautiful manifest that creates a
large ignore ritual is not a win.

## The historical fixture found the missing visibility decision

The real 4,594-line `views.clj` fixture would not have compiled under the first
planner. Fourteen moved forms were byte-stable, but `not-blank` was `defn-` in
the monolith and `defn` in the historical destination. Twenty-four remaining
source owners call moved Vars; several call `not-blank`. Moving it privately
while rewriting those callers through the new namespace would create an
illegal cross-namespace private reference.

That is exactly the kind of decision the transaction must make visible. The
planner now reports the exact mechanically required public set and places it
in `next_call.extraction.public_forms`. Apply requires every such form, accepts
only selected private forms, and initially supports one lossless projection:
exact `defn-` to `defn`. Unsupported private metadata and custom macro forms
refuse. The visibility change, form movement, source require, internal caller
rewrites, formatting, parse, read-back, and receipt all remain one atomic
transaction.

The live public MCP route succeeded without restarting the server:

| Stage | Server-owned wall | Evidence |
| --- | ---: | --- |
| `inspect_clojure plan-extraction` | 6.116s | 15 forms, 24 remaining-source owners, exactly `public_forms=["not-blank"]` |
| `apply_clojure_changes` | 8.202s | 15 edits, 2 files, one atomic commit, terminal receipt |
| Total tool execution | 14.319s | two calls, zero refusals, zero failed mutations |

Every one of the 15 generated owner bodies is byte-identical to the actual
historical `views/format.clj`, including the intentional publicization of
`not-blank`. `clj-kondo` reported zero errors on the generated source and
target; warning-level source-header cleanup remains a separately visible
follow-up rather than being smuggled into this move.

The important claim is still pending the blinded native control. This receipt
proves feasibility and tool cost, not complete-task advantage. It also shifts
the measured bottleneck: formatter time fell to 1.141s in the public replay,
while planning still spent 6.112s scanning and compiling the large snapshot.
The next cohort must measure agent thinking, route fragmentation, and
correctness against the same frozen fixture.
