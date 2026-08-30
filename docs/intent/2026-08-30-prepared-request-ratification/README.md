# Prepared Request Ratification Packet

Status: **Option A HLD ratified; leaf LLD, EARS requirements, red tests, and
product code deferred.** Gene ratified decision 2 on 2026-08-30 with `"2 go"`
inside `"1 go, 2 go, 3 go"`. The experiment-only proxy screen is separately
authorized. Installation and MCP reload remain unauthorized.

## Decision requested

Option A is selected: an eligible successful inspect result carries a
non-executable prepared edit template with caller-owned replacement holes.

This ratification selects the HLD direction and the contract for a non-product
proxy experiment. It does not ratify the leaf LLD or EARS registry and does not
authorize product implementation. The fallback remains Option C, limited to
recovery after a mechanically correctable refusal.

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

The causal bundle is **less request assembly plus more route salience**. The
packet does not claim that request bytes alone caused the routing change.

### Ratification reframe

The independent replication observed 10/10 Surgeon-first in both prepared and
unprepared arms. The fixture had no routing headroom, so the original +50
percentage-point routing effect did not replicate. The prepared arm did reduce
median recovery output by 47.4% (1,711.5 to 899.5 tokens) and reduced observed
construction refusals from six to zero.

The ratified value claim is therefore **assembly-error elimination and
recovery-output reduction**. Routing lift remains unresolved and secondary as
a product claim. The frozen proxy experiment keeps its original primary gate;
this reframe does not rescore or weaken it.

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
`prepared_request` for `edit_clojure`. The final LLD must decide whether the
first slice prepares one exact selection or an ordered batch. Each eligible
selection must be inside one uniquely named top-level owner, with exact source,
exact cardinality, and a completed read. The template repeats the
canonical workspace, project-relative file, named owner, exact selected
`from` source, and match count. Each `to` value is a named null hole. The
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

Load-bearing assumption: route choice remains open after inspection, and a
visible guard-complete template with only replacement holes is salient and
easy enough to increase successful Surgeon-first mutation. The experiment
must count sessions that never inspect or never see the template; conditioning
only on exposed sessions would create a false green.

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
correction-authority decision.

## Recommendation and fallback

Choose Option A for the first transfer experiment because it is the closest
safe test of partial transfer without charging an extra tool turn. Its null
replacement holes make it materially weaker than the complete causal object.
Do not approve implementation from the Sweep-2 result alone.

Fallback: if an exact eligible read cannot produce the public template without
guessing a subject or forcing whole-owner replacement, stop Option A and test
Option C as a narrower recovery feature. Do not widen the server's judgment to
save the adoption thesis.

## Falsifiable adoption commitment

For one frozen external-repository task family and one fixed caller/model
stratum, run eight fresh sessions in counterbalanced order: four control and
four prepared-result sessions. An experiment-only MCP proxy preserves the
production tool catalog and handlers and changes only the successful inspect
response projection. It adds the frozen null-hole candidate descriptor in
treatment and does not edit product files, mutate source, install, reload, or
publish a tool.
Prompts, task, source, and scoring remain identical. Inspection is not forced
or mentioned. Construction and execution of this proxy require separate
experiment authority after this HLD choice.

The option advances only if all of these conditions hold:

1. the prepared arm selects `edit_clojure` as the first mutation route in at
   least 3/4 attempts;
2. the prepared arm improves successful Surgeon-first routing by at least 25
   percentage points over control;
3. exact semantic correctness does not decrease;
4. refusal rate does not increase; and
5. four additional read-only safety attempts, two per arm, produce zero
   mutations.

Every fresh session remains in the primary denominator, including sessions
that never inspect, never receive the template, refuse, or choose native.
Exposure and conditional conversion are secondary diagnostics only. Complete
task wall, total observable client actions, and tool calls remain in the loss
chart, but they are not inferred from hidden reasoning and do not override the
primary routing gate.

Do not call a routing miss a pass. A miss means the prompt-embedded routing
signal did not transfer under this gate. If the registered recovery outcomes
still reproduce, the ratified HLD remains supported by its reframed value
claim, but the experiment does not advance the leaf LLD automatically. That
outcome requires a separate recovery-oriented LLD decision. It is not
permission to force inspection, add a route cue, or rescore only exposed
attempts.

## Phase stops

1. Gene selected Option A and separately authorized the non-product proxy
   experiment.
2. The proxy experiment runs before product code. A primary-gate pass returns
   a completed leaf LLD and EARS registry for separate ratification. A miss
   keeps them deferred unless Gene separately selects a recovery-oriented gate.
3. After a pass, the selected leaf LLD is completed and ratified separately.
4. The completed EARS registry and stable IDs are ratified separately.
5. Direct red witnesses activate the approved requirements.
6. Product code may begin only after the exact intended red count is proved.
7. Installation, MCP reload, and any post-product cohort require separate
   authority after green product verification.

## Evidence

- `experiment/differential-routing-interview-20260829` at `a9afcd13`
- `experiment/external-corpus-shape-census` at `28ee81f4`
- installed read normalization at `c55de227`
- `docs/high-level-design.md`, especially **Compress a coherent read mission
  without guessing**
- `MCP-OP-READ-NORM-001..005`, `MCP-OP-READ-CONT-001..002`,
  `MCP-OP-READ-RETRY-001..002`, and `MCP-OP-EDIT-010`

## Captain's log

- **Option created:** turn exact read evidence into a visible edit template.
- **Counterfactual:** if prompt authority or fixture-specific salience caused
  Sweep-2, the tool-result treatment will not move unforced routing.
- **Surprise:** the safest product object is necessarily less complete than the
  causal prompt object because the server does not own replacement meaning.
- **Falsifier:** less than +25 points, fewer than 3/4 prepared Surgeon-first
  attempts, any correctness loss, or any mutation in the read-only arm.
- **Decision:** select the closest safe transfer experiment; keep implementation
  behind the result.
