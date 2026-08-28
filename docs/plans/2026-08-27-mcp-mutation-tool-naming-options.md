# MCP Mutation Tool Naming Options

**Status:** Design options for `clj-surgeon-x9d`. No public API change.

**Scope:** `edit_clojure` and `apply_clojure_changes`

## Decision question

Which public names and request shapes let a fresh coding agent select the
correct Clojure mutation tool on its first call?

The current tools share an atomic mutation runtime, but they serve different
decision states:

- The compact editor receives a complete edit decision. The caller supplies
  exact old and new forms, a bounded computed relation, or exact owner names.
- The change compiler receives or derives a semantic change plan. It can use a
  prepared basis, compile an extraction, and run project verification that
  participates in rollback.

Both tools use guards, one frozen snapshot, atomic commit, read-back evidence,
and an inverse receipt. Therefore, names such as `safe`, `guarded`, `atomic`,
or `transaction` do not distinguish the tools.

## Protected facts

Any naming experiment must preserve these facts:

1. `edit_clojure` is the proven fast path for an already-decided batch.
2. `apply_clojure_changes` supports prepared decisions, extraction, and
   rollback-participating verification.
3. A direct extraction can compile and commit in one call. It does not require
   a separate public planning call.
4. A genuine unresolved decision must refuse before mutation.
5. Native patching remains appropriate for prose and a small arbitrary text
   edit.
6. A public rename must not weaken the measured Sol route.
7. The CLI has separate compatibility laws. MCP naming does not require a
   matching CLI command rename.

## Term map

| Preferred term | Meaning in this review |
|---|---|
| complete edit decision | The request contains every edit and mechanical guard needed to compile the mutation. |
| semantic change plan | Surgeon prepares or derives owners, sites, caller decisions, extraction facts, or transaction gates. |
| project verification | A repository-owned command that runs inside the transaction and can cause rollback. |
| compatibility alias | An old tool name that invokes the new canonical handler during migration. |

Avoid `simple`, `complex`, `light`, and `heavy` in public descriptions. Those
terms describe implementation cost, not a request contract.

## Scoring method

The scores below are design priors, not experimental results. A score of 5
means that the option appears clear or inexpensive. A score of 1 means that
the option appears ambiguous or expensive.

`Migration` scores ease of migration. A high score means low migration cost.
The `CLI` score measures conceptual clarity for a CLI user. It does not propose
renaming the existing CLI operations.

## Candidate portfolio

| ID | Public name or shape | Fresh Sol | Claude Opus or Fable | CLI | Migration | Main benefit | Main defect |
|---|---|---:|---:|---:|---:|---|---|
| A | Keep `edit_clojure` and `apply_clojure_changes` with disjoint schemas | 4 | 4 | 3 | 5 | Preserves the proven names and removes request overlap. | Both verbs remain generic. A caller must read the descriptions. |
| B | `edit_clojure` and `apply_clojure_plan` | 5 | 5 | 4 | 3 | Names the decision-state boundary directly. | The server can derive the plan in the same call, so `plan` must include compiler-derived plans. |
| C | `edit_clojure` and `refactor_clojure` | 5 | 5 | 5 | 3 | Matches common editor vocabulary and makes tool choice easy to explain. | Some verified changes are not refactors. The name can overstate semantic intent. |
| D | `apply_clojure_edits` and `apply_clojure_intent` | 4 | 4 | 4 | 2 | Uses parallel grammar and distinguishes supplied edits from semantic intent. | `intent` is abstract and can imply that the model may omit required decisions. |
| E | `commit_clojure_edits` and `execute_clojure_change` | 4 | 4 | 4 | 2 | Both names state that the tools mutate source. | `commit` and `execute` do not explain why two tools exist. |
| F | `edit_clojure` and `transact_clojure` | 4 | 4 | 3 | 3 | Signals that the second tool owns additional transaction gates. | The compact editor is also transactional, so the distinction is technically false. |
| G | `patch_clojure` and `refactor_clojure` | 5 | 5 | 5 | 2 | Uses a familiar local-change versus semantic-change pair. | `patch` understates computed programs, multi-file atomic edits, and owner deletion. |
| H | `apply_exact_clojure_edits` and `apply_prepared_clojure_change` | 5 | 5 | 4 | 2 | Makes the authority source explicit in both names. | The names are long. `exact` can be misread as byte-exact output rather than exact guards. |
| I | `edit_clojure` and `run_clojure_operation` | 3 | 3 | 3 | 3 | Leaves room for extraction and future semantic operations. | `operation` is vague and does not guide selection. |
| J | One `change_clojure` tool with `kind: exact`, `prepared`, or `extraction` | 5 | 5 | 4 | 1 | Removes the first tool-selection decision. | Creates a large tagged-union schema. Prior evidence found fewer public schemas slower, so this option reopens a stopped direction. |
| K | One `edit_clojure` tool with mutually exclusive `edits`, `basis`, or `extraction` branches | 4 | 4 | 4 | 1 | Preserves the established short name and one-shot routes. | Makes one generic editor own incompatible authority rules and a large schema. |
| L | `edit_clojure` for complete decisions. `apply_clojure_plan` for semantic plans and rollback-participating gates. | 5 | 5 | 5 | 2 | Aligns the public boundary with authority. It moves exact insertion, rename, and map updates to the compact route. | Requires schema movement and compatibility work before the naming benefit is real. |

## Top three options

### 1. Option L: `edit_clojure` and `apply_clojure_plan`

This option has the clearest executable rule:

```text
complete edit decision ----> edit_clojure
semantic plan or gate -----> apply_clojure_plan
```

The distinction depends on request authority, not perceived complexity.
Exact insertion, deletion, local rename, and map updates belong in
`edit_clojure` when the request contains the full decision. A prepared basis,
an extraction compiler, or project verification belongs in
`apply_clojure_plan`.

Proposed strict descriptions:

> `edit_clojure`: Commit one complete Clojure edit decision. Supply exact old
> and new forms, bounded computed programs, or exact owner deletions. Surgeon
> compiles all items against one frozen snapshot and commits them atomically.
> Use `apply_clojure_plan` when Surgeon must prepare or derive semantic change
> facts, or when project verification must participate in rollback.

> `apply_clojure_plan`: Compile and apply one semantic Clojure change plan. Use
> a prepared basis, an extraction request, or a change that requires project
> verification with rollback. Surgeon refuses unresolved decisions before
> mutation. Use `edit_clojure` when the request already contains every edit and
> guard.

Why it leads:

- The descriptions contain one reciprocal selection rule.
- The names do not claim that only the second tool is safe or atomic.
- The boundary can absorb current overlap instead of preserving accidental
  schema history.
- `plan` remains accurate when the server derives the plan internally, if the
  contract defines a plan as the compiler result rather than a prior public
  artifact.

### 2. Option A: keep the names and make the schemas disjoint

This is the lowest-risk option. It can earn most of the selection benefit
without changing public identifiers. Move every fully specified edit action to
`edit_clojure`. Reserve `apply_clojure_changes` for prepared or
compiler-derived semantic work and rollback-participating gates.

Why it remains a finalist:

- The current fast path and installed routing remain unchanged.
- Existing traces and clients need no name migration.
- The team can test the schema boundary before a rename adds another variable.
- If agents still select the wrong tool after the schemas are disjoint, the
  evidence can justify a later rename.

The defect is semantic debt. `apply_clojure_changes` still sounds like the
general mutation tool, although the intended contract is narrower.

### 3. Option C: `edit_clojure` and `refactor_clojure`

This pair gives fresh callers the strongest familiar analogy. Editors expose
direct edits and semantic refactors. Extraction, binding-aware rename, caller
changes, and verification-backed structural work fit the second term.

Why it remains a finalist:

- The names are short and easy to contrast.
- The distinction is likely legible before a caller reads the full schema.
- CLI users already understand edit versus refactor vocabulary.

The defect is scope accuracy. A transaction can use project verification
without being a refactor. This option must fail if clean-context callers avoid
`refactor_clojure` for such changes.

## Options to reject before experiments

- Reject F because both tools are transactions.
- Reject I because `operation` does not guide selection.
- Do not reopen J or K without new causal evidence. A single large tool schema
  previously lost performance, and tool-count reduction alone is not the goal.
- Treat G as a useful prompt label, not a preferred API. `patch` understates
  the compact editor's structural and computed capabilities.

## Executable clean-context evaluation

### Goal

Measure whether a candidate improves correct first-call routing without
regressing complete verified task time.

### Candidate catalogs

Test these catalogs first:

1. A: current names with disjoint strict descriptions and schemas.
2. L: `edit_clojure` and `apply_clojure_plan`.
3. C: `edit_clojure` and `refactor_clojure`.
4. M: `edit_clojure`, `extract_clojure`, `apply_clojure_plan`, and
   `transform_clojure` with split schemas.
5. N: M with `transform_clojure_with_clojure` to test the atomic-chord versus
   computed-solo language.
6. O: M with `apply_clojure_edits` and `run_clojure_transform` to test the
   edits-as-data versus program language.
7. P: effect-first verbs: `commit_clojure_edits`,
   `commit_clojure_extraction`, `preview_clojure_transform`, and
   `commit_clojure_transform`.
8. Q: dotted effect names such as `clojure.edit.commit` and
   `clojure.transform.preview`.
9. R: portable `_commit` names with human titles such as `edit_clojure!`.
10. S: portable `_bang` names with human titles that contain the real `!`.
11. T: action verbs: `write_clojure_edits`, `move_clojure_owners`,
    `preview_clojure_transform`, and `apply_clojure_transform`.

Use the same implementation commit, fixtures, output contract, and scorer for
all catalogs. Change only tool names, descriptions, and the public routing
projection. Do not install a candidate catalog on the shared MCP server.

MCP tool names permit ASCII letters, digits, underscore, hyphen, and dot.
They do not permit a literal `!`. Catalogs R and S therefore put the Clojure
bang in `annotations.title`, not in the canonical tool name.

### Effect annotation audit

The candidate projection uses these conservative annotations:

| Control | `readOnlyHint` | `destructiveHint` | `idempotentHint` | Reason |
|---|---:|---:|---:|---|
| Inspect or transform preview | true | false | true | The control does not write source. Repetition has no environmental effect. |
| Edit, extraction, retained plan, or transform commit | false | true | false | The control can replace or delete source. A refusal or rollback does not remove this capability. |

All projected controls use `openWorldHint=false`. They operate on the local
workspace, not an external entity. These fields are hints. They do not grant
authority and do not replace source guards.

The MCP specification defines `destructiveHint=false` as additive-only. A
source rewrite is not additive-only. The experiment therefore does not label
guarded or rollback-capable rewrites as non-destructive. It also leaves
mutations non-idempotent. That is the conservative choice for client retry
behavior.

Sources:

- https://modelcontextprotocol.io/specification/2025-11-25/schema#toolannotations
- https://modelcontextprotocol.io/specification/2025-11-25/server/tools#tool-names

### Name truth table

| Shape | Accurate signal | Truth problem |
|---|---|---|
| `_commit` | The operation requests a commit. | A safe refusal or successful rollback does not commit. |
| `_bang` plus a bang title | The operation may mutate. | The convention is clear to Clojure users but can be opaque to other callers. |
| Dotted names | Namespace and effect compose consistently. | Dots add visual ceremony without explaining the authority boundary. |
| Action verbs | `write`, `move`, `preview`, and `apply` state the intended effect. | The names are longer, and `apply_clojure_plan` remains valid only for a retained basis. |
| Title-only bang | A UI can show normal Clojure effect notation. | A caller that sees only the canonical name receives no bang signal. |

The isolated server proves that `tools/list` retains `annotations.title`.
Codex event traces identify calls by the canonical name. Whether Codex uses
the title during selection remains an experimental question. The current
Claude clean harness is CLI-only and cannot answer the title question.

### First-person LLM-user verdict

I want four controls that feel like editor chords:

```text
inspect ------------------------> show exact evidence
edit ---------------------------> apply one supplied atomic chord
extract ------------------------> move exact named owners
transform with Clojure ---------> compute one bounded solo
retained plan next call --------> continue only from inspect evidence
```

`edit_clojure` already feels fast. I can see all supplied literal,
structural, computed, and delete actions and send one batch. I would not
rename that proven control only to make the names symmetrical.

`transform_clojure_with_clojure` makes the computed solo explicit. I still
hesitate when `edit_clojure` accepts a programs-only request, because that
request overlaps the standalone transform. Reserve a standalone program for
transform. Permit an edit program only when it composes with another action.

`extract_clojure` should finish a mechanically complete extraction in one
call. A genuine unknown can refuse before write and return an exact retry.
Do not insert a public plan call into the complete route.

I should never freely select `apply_clojure_plan`. I should call it only from
an exact `inspect_clojure` next call with a retained basis. This constraint is
more important than the plan tool's name.

Catalog T is the clearest effect-signaling challenger. `move_clojure_owners`
and `preview_clojure_transform` tell me what the controls do. Catalog N is the
strongest minimal vocabulary because it preserves `edit_clojure` and names
the computed rule directly. Test N and T before `_commit`, dotted, or `_bang`
variants.

### Isolated harness commands

Run the no-model identity suite:

```bash
clojure -Sdeps '{:paths ["src" "dev/experiments"]}' \
  -M:clj-surgeon/mcp \
  -m clj-surgeon.experiments.mcp-candidate-catalog-test
```

Select a fresh Codex catalog with `BENCH_MCP_CANDIDATE_CATALOG`. For example:

```bash
BENCH_MCP_CANDIDATE_CATALOG=N \
BENCH_PRE_COMMIT=HEAD BENCH_POST_COMMIT=HEAD \
BENCH_RUN_MATRIX='mcp:mcp-extraction-tool-first-no-skill' \
BENCH_TASKS=sessionize-format-extraction \
BENCH_INCLUDE_COMPACT=false BENCH_REPLICATES=1 BENCH_PARALLELISM=1 \
BENCH_MODEL=gpt-5.6-sol BENCH_REASONING=high \
BENCH_RESULT_DIR=/tmp/clj-surgeon-catalog-N-sol \
bash bench/run_clean_codex.sh
```

Use T instead of N for the action-verb challenger. The harness starts an
isolated candidate-owned MCP server. It does not install or reload the shared
server.

### Frozen task strata

Use at least three tasks in each stratum:

| Stratum | Correct first action |
|---|---|
| Exact known nested or multi-file edits | Compact editor |
| Exact insertion, owner deletion, local rename, or map update | Compact editor after the schema boundary supports it |
| Prepared basis with filled decisions | Semantic-plan tool |
| Direct extraction with mechanically derivable facts | Extraction tool |
| Project verification that must roll back on failure | Semantic-plan tool |
| Prose or arbitrary non-structural text edit | Neither Surgeon mutation tool |

Include at least one deliberately small task in each Surgeon stratum. This
checks whether a name causes unnecessary escalation based on perceived task
size.

### Small screen

Run two fresh sessions per catalog, caller, and stratum in a counterbalanced
order. Use Sol/high, Claude Opus, and Claude Fable. Stop a candidate early if
it produces either result:

- a wrong mutation tool in more than one session;
- a Sol median complete wall that is more than 5 percent slower than the
  current catalog on the same tasks.

### Expanded cohort

For candidates that pass the screen, run eight fresh sessions per caller and
stratum. Preserve caller and model strata. Do not pool them.

Record:

1. first mutation tool;
2. first-call schema validity;
3. number of preflight reads;
4. refusal count before the successful mutation;
5. total tool actions;
6. semantic correctness;
7. complete verified task wall;
8. request and response bytes;
9. final response behavior.

The release gate is:

- no Sol correctness regression;
- no Sol p50 complete-wall regression greater than 5 percent;
- at least a 30 percent reduction in wrong first-tool selection or
  schema-invalid first calls;
- no Claude caller loses the current one-shot successful route;
- the prose control continues to select native editing.

### Causal interpretation

Names and descriptions earn credit only when the selected route changes. If a
candidate reduces thinking time but preserves every route, record the gain but
do not infer better tool understanding. If a schema move improves performance,
separate that result from the name comparison with a factorial arm.

### Alternate-universe boundary

A first-selection catalog is valid only when initialize instructions, every
advertised tool name and title, every tool description, and schema-facing
prose use one catalog lexicon. An unavailable legacy mutation name anywhere in
that pre-call surface invalidates the arm. Source archaeology that reads the
server, client, tool, benchmark, rule, or skill implementation to recover a
name also invalidates the arm and must be recorded explicitly.

This checkpoint does not project post-call results. Legacy operation fields,
remedies, next calls, and human summaries remain a separate complete-route
gate. No candidate can earn publication or a complete-task performance claim
until those responses are either projected consistently or proven harmless by
an isolated response wrapper and a clean-context route cohort.

## Orthogonality gate

A good catalog gives each control one job. A fresh caller must not need to
choose between two controls that accept the same intent.

Option M currently has one confirmed overlap and one missing seam:

| Pair | Classification | Falsifier |
|---|---|---|
| `edit_clojure` and `transform_clojure` | Defect | A standalone program must select transform. A computed program that must commit with literal actions or owner deletion must select edit. |
| `edit_clojure` and `extract_clojure` | Orthogonal | Exact namespace movement selects extract. A supplied edit batch selects edit. |
| `inspect_clojure` and `extract_clojure` | Defect risk | A mechanically complete extraction must call extract first and finish in one call. A genuine unknown may refuse with a completed frozen plan, but must not cause rediscovery. |
| `edit_clojure` and `apply_clojure_plan` | Authority boundary | A complete decision selects edit. Only an exact retained basis and filled decisions select plan. |
| `extract_clojure` and `apply_clojure_plan` | Missing continuation seam | Current extraction retries reuse the complete extraction request. The product has no executable extraction `plan_id` input. |

The first schema ratchet to test is:

```text
standalone computed rule ----------------> transform_clojure
computed rule + another source action ---> edit_clojure programs
```

Do not enforce this ratchet in production until a compatibility review. The
current editor permits a programs-only request.

The exact-verification boundary is a separate factorial. A complete edit might
need project verification to participate in rollback. Test whether
`edit_clojure` should accept the project-owned exact verifier. Do not force a
complete decision through a semantic-plan route only to obtain verification.

`apply_clojure_plan` is not a freely selected mutation tool. It is valid only
when `inspect_clojure` returned the retained basis and exact next call. A
fabricated or stale basis must refuse before mutation.

## Migration plan if a rename wins

1. Land the disjoint request boundary before the rename.
2. Add registry-derived tests for canonical names and compatibility aliases.
3. Publish the new canonical names in `tools/list`.
4. Keep old names as invocation-only aliases when the MCP dispatcher supports
   non-advertised aliases.
5. If hidden aliases are not possible, advertise aliases for one bounded
   release and measure whether the larger catalog harms routing.
6. Keep the old result operation names during the compatibility window, or add
   an explicit versioned result field. Do not change input and output names
   silently in one release.
7. Update installed Codex and Claude skills from the same immutable commit.
8. Hot reload once, then verify one old cached caller and one fresh caller.
9. Remove aliases only after retained telemetry shows no old-name calls in the
   declared observation window.

## Recommendation

Test the schema boundary before the rename. Use Option A as the control, then
compare Options L and C. The working preference is Option L because
`apply_clojure_plan` names the authority boundary more precisely than
`refactor_clojure`.

Do not ship a rename from design judgment alone. The current names already
support a proven fast path. A new name must improve first-call routing or
complete verified task time without reducing Sol performance.
