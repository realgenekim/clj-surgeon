# Prepared Request Ratification Packet

Status: **Recovery-oriented leaf LLD and EARS registry ratified; red-first
implementation active.** Gene ratified the leaf on 2026-08-30 with
`"Wow!!! Love it! Go!"`. The earlier Option A HLD ratification (`"2 go"`) and
recovery authorization (`"Recovery go -- go"`) remain part of the decision
record. Installation and MCP reload remain unauthorized.

## Ratified decision

The completed success-only Option A leaf LLD and requirements
`MCP-OP-PREP-REQ-001..009` are active. An eligible successful inspect result
carries a non-executable prepared edit template with caller-owned replacement
holes.

This decision authorizes red-first implementation of the pure projector and
four narrow inspect-tool hooks specified by the leaf. It does not authorize a
prepared or corrected mutation in a refusal. The separate write-refusal leaf
owns source-free recovery evidence at its audited sites. A future refusal-side
correction remains a distinct Option C design with its own authority gate.

### Decision options

**A — Ratify the success-only LLD and `MCP-OP-PREP-REQ-001..009`
(recommended).** Why it might be right: the descriptor-plus-static-sentence
treatment reduced recovery work in a valid cohort without losing correctness
or read-only safety.
Cost: eligible read results may add up to 4,096 bytes, the projection and output
schema need permanent tests, and the recovery effect still needs a fresh
product-shaped cohort. Assumption: callers that already inspect can use a
guard-complete template more reliably than they reconstruct the edit request.

**B — Hold the prepared-request leaf.** Why it might be right: routing lost,
two of four treatment attempts completed through another mutation route, and
the recovery sample is small. Cost: the measured median reduction of about 18
seconds and 912 output tokens remains unavailable. Assumption: write-refusal
completeness or future caller improvements will remove the same recovery work
without adding a success-side descriptor.

Recommendation: choose A. The deciding argument is that the
descriptor-plus-static-sentence proxy treatment, not only the complete prompt
object, produced a measurable recovery signal. If that assumption fails in the
forward acceptance cohort, stop the leaf and keep B.

## Evidence boundary

- In the frozen Sweep-2 task, a complete guarded `edit_clojure` request in the
  user prompt moved Surgeon-first routing from 2/4 to 4/4. Exact before-state
  detail and a 600-line file-size increase moved routing in the wrong direction.
- The result has four attempts per cell. It proves a mechanism signal for one
  task, model, client, and prompt position. It does not prove that a tool result
  has the same salience or authority as user text.
- One external Clojure corpus contained 119 established `src/` or `test/`
  update opportunities among 266 native writes, or 44.74%. Structural mutation
  adoption in that corpus was 1.48%.
- Installed read normalization makes request IDs deterministic within one call
  and complete. Those IDs are evidence labels only. They do not grant selector,
  snapshot, or write authority.

The historical Sweep-2 causal bundle was **less request assembly plus more
route salience**. The product hypothesis in this packet is narrower: less
recovery after the same eligible inspect exposure. It does not use routing to
pass.

### Ratification reframe

The independent replication observed 10/10 Surgeon-first in both prepared and
unprepared arms. The fixture had no routing headroom, so the original +50
percentage-point routing effect did not replicate. The prepared arm did reduce
median recovery output by 47.4% (1,711.5 to 899.5 tokens) and reduced observed
construction refusals from six to zero.

The ratified value claim is therefore **assembly-error and recovery-output
reduction**. Routing lift is unsupported and excluded from this leaf's product
claim. The frozen proxy's failed primary gate remains closed. This reframe does
not rescore or weaken it.

### Recovery result and exact claim

The valid success-side proxy cohort failed its routing gate: control completed
Surgeon-first in 4/4 attempts and treatment in 2/4. The same cohort measured
30.0% lower median output, construction refusals falling from seven to four,
recovery actions falling from 20 to eight, and 25.3% lower median complete
wall. The sibling complete-request screen measured 47.4% lower median output
and six versus zero construction refusals.

The null-hole product shape did not eliminate all assembly errors. The exact
claim for this leaf is narrower: after an eligible successful inspect exposes
the descriptor, the descriptor may reduce later request-construction refusals
and observable recovery effort. The complete-request magnitude does not
transfer automatically to the null-hole descriptor. The leaf makes no routing,
adoption, universal one-shot, or general speed claim.

### Benefit in plain language

Actual: in the valid proxy cohort, the median completed task fell from 69.291
seconds to 51.729 seconds and emitted 912 fewer output tokens. Recovery actions
fell from 20 to eight across four attempts per arm. The sibling experiment
independently found the same direction with a 47.4% output reduction.

Potential: an agent that already inspected the intended forms can fill only
the replacement values instead of rebuilding file, owner, old source, and
count guards. That can prevent a malformed first mutation call or remove a
repair turn, especially for a fast or weak caller. The first-call and
Spark-class screens in the LLD test those possibilities. They are not current
claims.

## Non-negotiable authority boundary

An ordinary read knows selected old source and structural identity. It does
not know the intended replacement. Therefore:

- the server may prepare explicit file, named owner, old source, cardinality,
  and public request structure only when the read proved those facts;
- replacement values remain explicit caller-owned holes;
- the object is `prepared_request`, not executable `next_call`;
- the object has `executable=false` and `write_authority=false`;
- the read result retains `next_action=none` and never claims that the selected
  source should be edited;
- static coaching is conditional on an independent, task-authorized caller
  decision to edit the exact selections;
- no client may auto-consume or auto-execute the template;
- filling every hole makes `prepared_request.arguments` eligible for ordinary
  `edit_clojure` validation from zero replacement authority; and
- no request ID, position, handle, similarity score, or server guess may replace
  explicit subject identity.

This resolves the intent-supply paradox without pretending that the proposed
template is identical to the complete prompt-embedded request. Transfer must
be measured.

## Option A — Successful inspect result carries a prepared edit template

An eligible successful `inspect_clojure` result carries one visible
`prepared_request` for `edit_clojure`. The first slice prepares one through six
complete named whole forms from one terminal `forms` result for one file. Each
eligible form has exact source, exact hashes and anchor, exact cardinality, and
a unique named top-level owner. The template repeats the canonical workspace,
project-relative file, named owner, exact selected `from` source, and match
count. Each `to` value is a named null hole. The
concise result uses static server-owned conditional text: if the caller
independently decides that these exact selections are the intended mutation
subjects, fill the holes and submit `prepared_request.arguments`; otherwise,
ignore the template. The source values remain structured data; they are not
interpolated into instruction text.

Why it might be right: it places the intervention after a cheap structural
read and before the mutation choice. It adds no preparatory turn, makes the
Surgeon route visible, and pre-fills identity and guard fields. It also
uses the existing public edit contract rather than adding another executor.

Cost: eligible read results become larger, and only agents that choose the
read can see the template. Exact selections may be too rare. A template with
caller holes may not reproduce the effect of a complete unchanged-submit
request.

Load-bearing assumption: after the same eligible inspect exposure, a visible
guard-complete template with only replacement holes reduces later construction
refusals, recovery actions, or output while preserving exact correctness and
fallback behavior. Routing remains descriptive and cannot make this leaf pass.

## Option B — Dedicated `prepare_edit` operation

A separate non-mutating operation accepts explicit named edit subjects and
caller-owned replacement decisions. It returns one complete, schema-valid
`edit_clojure` request with guards expanded. This could be a mode on
`inspect_clojure`; it should not become another mutation tool or executor.

Why it might be right: the boundary is explicit and auditable. Ordinary reads
stay small. Because the caller supplies the edit decisions, the server can
return a complete request without inventing intent. This option most closely
matches the exact ready-to-submit object used in Sweep-2.

Cost: it creates a discovery problem. An agent that avoids `edit_clojure`
because request assembly looks costly must first choose a Surgeon-specific
preparation operation. It also adds a complete tool boundary. If the prepare
arguments already state every edit, much of the assembly work has already
occurred. If they state only high-level intent, the server becomes a semantic
planner, which violates the bookkeeping boundary.

Load-bearing assumption: agents will choose a low-commitment preparation
operation earlier than they choose mutation, and the returned complete request
will save more time than the added turn costs. This is the weakest assumption
in the option set. The option should remain a fallback experiment only if
Option A fails because its caller-owned holes leave too much assembly.

## Option C — Refusal carries a mechanically corrected request

When a request refuses for exactly one mechanically provable reason, the
refusal may carry one corrected guarded request. Examples are a closed field
alias or another correction that preserves every caller-owned subject,
replacement, guard, count, and scope. If correction authority is absent,
non-unique, stale, or accompanied by another failure, the refusal carries no
executable call and retains the current non-executable retry-template law.

Why it might be right: after a refusal, the server already has the caller's
mutation intent. It can correct request mechanics without inventing meaning.
The prepared correction can remove a recovery round and is highly salient at
the moment it is needed.

Cost: this surface activates only after the agent has already chosen Surgeon
and made a bad call. It cannot explain or improve first-choice adoption. It
also risks masking a poor request contract and can create replay or loop
hazards. Corrections must never relax a guard, widen a file or owner set, or
repair an authorization failure.

Load-bearing assumption: a material share of abandoned Surgeon calls follow a
single mechanically correctable refusal, and callers copy an exact correction
more often than they reconstruct it. If tested, the result must be reported as
refusal recovery, not as prepared-request adoption.

This option cannot override the existing non-executable template laws in
`MCP-OP-EDIT-010` or `MCP-OP-READ-CONT-001..002`. It requires a separate exact
correction-authority decision. It also cannot coexist with the proposed
write-refusal leaf at `6d558cb3`, whose invariant forbids corrected requests.
Ratifying Option C would require a separate decision that amends or supersedes
that invariant; the two designs cannot be composed concurrently as written.

## Recommendation and fallback

Ratify the success-only Option A leaf and its nine candidate requirements. The
deciding argument is that the same null-hole shape reduced recovery work in a
valid cohort while preserving correctness and read-only safety. It did not
improve routing, so routing remains outside the product claim.

Fallback: if the template cannot reduce repeat construction refusals or one
observable recovery action after product-shaped exposure, stop this leaf. Do
not add refusal-side correction, force inspection, or widen server judgment to
save it. Write-refusal completeness remains a separate sibling leaf.

## Historical routing gate — closed loss

The frozen proxy ran eight efficacy sessions and four read-only safety
sessions. Every fresh session remained in its assigned denominator, including
sessions that did not inspect, did not receive the template, refused, or used
another mutation route.

The registered routing gate required treatment Surgeon-first in at least 3/4
attempts and at least 25 percentage points above control. Treatment was 2/4;
control was 4/4. The gate failed and is closed. The recovery decision did not
rescore it. This packet therefore cannot claim routing lift, adoption lift, or
successful transfer of the prompt-embedded routing signal.

The completed LLD replaces no historical outcome. It uses only the separately
authorized recovery question and the forward-only acceptance gates in
`prepared-request-design.md`.

## Phase stops

1. Gene selected Option A and authorized the proxy experiment.
2. The valid proxy missed routing and produced aligned recovery evidence.
3. Gene authorized the recovery-oriented LLD phase.
4. Gene ratified the completed leaf LLD and EARS registry with
   `"Wow!!! Love it! Go!"`.
5. Direct red witnesses are active.
6. Product code may begin only after the exact intended red count is proved.
7. Installation, MCP reload, and any post-product cohort require separate
   authority after green product verification.

## Evidence

- `experiment/differential-routing-interview-20260829` at `a9afcd13`
- `experiment/external-corpus-shape-census` at `28ee81f4`
- `experiment/prepared-request-replication-20260830` at `6277e067`
- `experiment/prepared-request-proxy-screen-20260830` at `ab5759e5`
- externally reported sibling packet `docs/write-refusal-ratification-20260830`
  at `6d558cb3`, used only for scope coordination because that object is not
  present in this worktree
- installed read normalization at `c55de227`
- `docs/high-level-design.md`, especially **Compress a coherent read mission
  without guessing**
- `MCP-OP-READ-NORM-001..005`, `MCP-OP-READ-CONT-001..002`,
  `MCP-OP-READ-RETRY-001..002`, and `MCP-OP-EDIT-010`

## Captain's log

- **Option created:** turn exact read evidence into a visible edit template.
- **Proxy verdict, 2026-08-30:** the valid counterbalanced cohort failed the routing gate: control
  was 4/4 correct Surgeon-first and treatment was 2/4. Treatment nevertheless reduced median
  output 30.0%, construction refusals 7 to 4, and recovery actions 20 to 8 with correctness 4/4 in
  both arms. The routing result does not advance the leaf LLD. The aligned recovery signal goes to
  Gene as a separate recovery-oriented gate decision. Raw receipt:
  `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md`.
- **Recovery design authorized, 2026-08-30:** Gene said `"Recovery go -- go"`.
  The completed LLD keeps Option A success-only, makes the recovery claim
  narrower than error elimination, and composes with write-refusal completeness
  as a sibling artifact rather than a shared retry authority.
- **Leaf ratified, 2026-08-30:** Gene said `"Wow!!! Love it! Go!"`. The
  `MCP-OP-PREP-REQ-001..009` requirements and red-first implementation are
  active. Installation and MCP reload remain separately gated.
- **Counterfactual:** if the proxy's recovery signal came from fixture-specific
  behavior, the product descriptor will not reduce recovery actions or output
  after the same eligible inspect exposure.
- **Surprise:** the safest product object is necessarily less complete than the
  causal prompt object because the server does not own replacement meaning.
- **Falsifier:** either arm is not 4/4 exactly correct, treatment contact is not
  exact, treatment fails the registered recovery-action or output gate, or any
  read-only safety attempt mutates source.
- **Decision:** select the closest safe transfer experiment; keep implementation
  behind the result.
