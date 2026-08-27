# Captain's Log: The public plan moved inside the transaction

Date: 2026-08-26 Pacific time
Workstreams: SURGEON1 and SURGEON2
Related issue: `clj-surgeon-90l`

## Breakthrough

The proposed prompt by result-shape factorial was aimed at a real 9 to 10
second model boundary:

```text
public plan result -> model reads and retranscribes plan -> apply
```

The direct-extraction release then removed that boundary for complete
decisions. Running the original four cells on the fully supplied 15-owner task
would now optimize an obsolete route.

The remaining honest stratum has omitted facts that the kernel can prove
mechanically. The fastest safe design is to move planning inside the existing
apply transaction:

```text
CURRENT PUBLIC ROUTE

task with derivable omission
        |
        v
inspect plan-extraction -----> model boundary -----> apply -----> lint
        51.624 s complete in the withheld-visibility replay

PROPOSED INTERNAL ROUTE

task with derivable omission
        |
        v
apply
  |
  +-- freeze one snapshot
  +-- compile the complete extraction plan
  +-- classify unresolved fields
  |
  +-- no genuine decision remains ------> commit atomically -----> lint
  |
  `-- genuine decision remains ---------> refuse before write
                                          + completed frozen plan
```

This is not automatic guessing. It is compiler work performed against the same
snapshot that owns the transaction.

## Pure seam

The transport-neutral extraction compiler should return one of two states:

```clojure
{:status :ready
 :plan complete-plan
 :genuine-unknowns []}

{:status :needs-decision
 :plan completed-as-far-as-proven
 :genuine-unknowns [...]}
```

`apply_clojure_changes` may execute only `:ready`. It must return
`:needs-decision` before any write. The response contains the completed,
snapshot-bound plan so the model does not pay to rediscover mechanically proven
facts.

Kernel-derivable candidates include:

- target namespace dependencies;
- visibility required by proven remaining-source callers; and
- complete zero-candidate caller accounting when workspace enumeration is
  authoritative, untruncated, and snapshot-bound.

Similarity, partial caller enumeration, omitted results, and ambiguous caller
meaning remain non-authoritative.

## Measured prize

The frozen Sessionize extraction currently provides these bounds:

| Route | Correct | Median or observed complete wall | Actions |
|---|---:|---:|---:|
| Direct supplied-decision apply | 2/2 | 37.871 s | 2 |
| Public plan then apply | 2/2 | 49.941 s | 3 |
| Withheld visibility, public plan | 1/1 | 51.624 s | 3 |

If internal compilation recovers the direct route geometry, the available
measured prize is approximately 12.070 seconds or 24.2% of complete wall. That
is a hypothesis for the new implementation, not its result.

## Merge decision

SURGEON2 recommended that SURGEON1 merge no new SURGEON2 product commit into
this hill.

- Selector recovery at `c8d0e4c` is already integrated and does not shorten
  extraction execution.
- Experimental manifest compiler `6800670` overlaps the production
  `mcp_extraction` and `mcp_extraction_plan` algebra. Importing it would create
  a second plan representation.
- Audit commit `f61a288` and benchmark commits `5240761` and `9c05cb0` are
  evidence only.
- Formatter and warm nREPL work do not remove the model phase.

The low-overlap route is to call the current production extraction compiler
from apply and consume its plan directly. Keep one plan algebra, one snapshot
identity, one stale-source fence, and one execution path.

The recommendation was delivered to SURGEON1 as
`agentmsg-c65211018c61`.

## Acceptance battery

Reuse the existing extraction fixture and harness. Do not build a general
semantic scorer.

1. **Complete supplied decision.** One apply plus one exact lint. No regression
   from the 37.871-second reference route.
2. **Derivable omission.** Withhold one mechanically provable fact, such as the
   `not-blank` visibility requirement. One apply plus one exact lint. No public
   inspect phase.
3. **Genuine ambiguity.** Present a caller or visibility decision that
   structural evidence cannot resolve. The apply call refuses before write and
   returns the completed snapshot-bound plan with the exact remaining unknown.
4. **Stale source.** Change the frozen source after compilation. Execution
   refuses without writing.

The supplied and derivable strata must use the task's exact
`clj-kondo --fail-level error` verifier once. A generic verification profile
cannot replace it.

## Status of the factorial

Do not run the original four cells on the complete-decision task:

```text
                      CURRENT RESULT       COMPACT PLAN_ID
                    +------------------+------------------+
  CURRENT PROMPT    | obsolete tax     | obsolete tax     |
                    +------------------+------------------+
  DECISION PROMPT   | obsolete tax     | obsolete tax     |
                    +------------------+------------------+
```

Retain `clj-surgeon-90l` as an option. Reactivate it only for a task where a
public model decision remains after the internal compiler has exhausted all
mechanical derivation. The faster product hill is now removing the public phase,
not compressing its prose.

## Frozen contract after adversarial review

SURGEON1 owns implementation and release. SURGEON2 remains an isolated
exploratory and adversarial lane. The selected product contract is:

- this change applies to MCP extraction apply only;
- `file`, `to`, `forms`, and `require_policy` remain mandatory authority;
- omitted `public_forms` can be derived only from a complete, frozen,
  zero-unaccounted-candidate proof;
- omitted caller decision arrays never account for a discovered candidate;
- omitted aggregate `expect` is derived mechanically;
- a supplied `expect` remains authoritative and a mismatch refuses; and
- CLI extraction policy does not change in this merge.

The CLI and MCP should continue to share `extract/compile-plan` as the pure
planning kernel. They should not yet share the stricter MCP execution policy.
Current CLI extraction reports external callers for later review. MCP apply
requires each candidate to be changed or explicitly ignored. Combining those
policies would be a backward-incompatible change disguised as compiler reuse.

## Honest genuine-decision fixture

Add one external caller to the frozen Sessionize extraction:

```clojure
(ns cfp-scheduler-killer.report
  (:require [clojure.string :as format]
            [cfp-scheduler-killer.views :as views]))

(defn report-date [x]
  (format/trim (views/fmt-date x)))
```

The compiler can prove that extracted private owner `not-blank` must become
public. It cannot choose the external caller migration. The obvious `format`
alias is already occupied, and valid decisions include another alias, a
qualified use, or an explicit ignore policy. A unique candidate is evidence,
not rewrite authority.

```text
ORIGINAL FIXTURE
omit public_forms + zero external candidates
        |
        `-- prove visibility --> commit in one call

CALLER OVERLAY
omit public_forms + leave caller disposition empty
        |
        +-- prove visibility
        `-- caller decision unknown --> refuse before write
                                      + completed frozen plan
                                      + exact unknown
                                      + zero rediscovery reads
```

The refusal must not create a receipt, target file, target directory, formatter
action, or verifier action. It must freeze the caller file, name the exact
remaining decision, and withhold executable write authority until that decision
is supplied. If this fixture commits in one call, the compiler crossed from
mechanical proof into architectural guessing.

## Permanent parity witnesses

The smallest useful ratchets are:

1. `extract/compile-plan` and the MCP plan projection agree on one frozen
   snapshot after presentation and path normalization.
2. Omitted `expect` and the exact explicit `expect` compile to identical source
   and aggregate counts.
3. A wrong supplied `expect` refuses, and omitted caller decision vectors never
   close a discovered candidate.

Do not advertise a CLI `source_hash` guard until the CLI enforces it. A parsed
but ignored guard is worse than no advertised guard because it creates false
confidence.
