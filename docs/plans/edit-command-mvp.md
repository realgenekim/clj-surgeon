# `:edit` Command MVP

**Status:** v2 help adopted but was not first; version-matched skill study pending

## Hypothesis

The lens already expresses a complete singular structural edit, but `:q` does
not advertise that affordance and `:replace-subform` exposes a lower-level
matching mechanism. A canonical `:edit` planning command can improve clean-agent
discovery without creating a second mutation language.

## Proposed command

```bash
clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn

# Only after reviewing the returned plan and diff:
clj-surgeon :op :replace-subform! :plan plan.edn
```

The query grammar and returned plan are byte-for-byte the existing `:q`
terminal-updater contract. `:edit` is a write-category planner that never writes
source. It exists to name intent, specialize help, and reject getter-only
pipelines early.

All three arguments are required. `:plan-out` must not canonically alias the
source path, including through a symlink. Unknown arguments such as `:apply`,
`:yes`, or `:force` refuse instead of being ignored.

## Success

- A terminal `[:replace FORM]` returns the existing `:replace-subform` plan.
- A terminal `[:replace-span FORM ...]` returns the existing `:replace-span`
  plan.
- `:plan-out` writes the unchanged versioned plan artifact.
- A failed request leaves an existing plan artifact byte-identical.
- The plan applies through the existing `:replace-subform!` executor and emits
  the existing verified receipt.
- Source remains unchanged during planning.

## Refusals

| Input | Required result |
|---|---|
| Navigation-only query | `:error-type :edit-requires-transform`; remedy names `:q` for reading and the two terminal updater shapes |
| Missing `:query`, `:file`, or `:plan-out` | Existing `:missing-arguments` contract and nonzero CLI exit |
| `:plan-out` aliases `:file` | `:plan-overwrites-source`; no write |
| Unknown `:apply`, `:yes`, `:force`, or other argument | `:unsupported-arguments`; no write |
| Malformed or unsupported query | Existing `:invalid-query` data unchanged |
| Zero selected targets | Existing `:no-match` data unchanged |
| Multiple selected targets | Existing `:ambiguous-match` data unchanged |
| Invalid replacement form/arity | Existing replacement refusal unchanged |
| Stale or edited plan | Existing apply refusal; no write |

Do not translate existing failures into a new error vocabulary. The only new
refusal is using an edit-intent command without a terminal transformation.

## Non-goals

- no `eval`, SCI, or arbitrary Clojure transformer;
- no jq-like alternate selector language;
- no multiple edits or bulk application;
- no `:edit!` that combines selection, planning, and writing;
- no fuzzy, first, or best-match selection;
- no change to plan schema or executor;
- no claim that `case`, `cond`, maps, or bindings share semantics.

## Red test matrix

### Pure

- node replacement delegates to the existing lens plan exactly;
- span replacement delegates exactly and preserves internal trivia;
- navigation-only queries refuse with stable fields and remedies;
- malformed, zero, many, invalid-form, and span-arity failures pass through;
- planning leaves the source literal byte-identical;
- real-program-derived `:finish` case fixture yields one exact edit and a
  complete-file result that reparses.

### CLI/help

- global help lists `:edit` in planned writes;
- `:edit --help` says “plans; never writes” and shows plan then separate apply;
- missing args and getter-only use exit nonzero with EDN only;
- the documented command emits a saved plan;
- the saved plan applies only through the existing executor and returns full
  verification evidence;
- refused planning and refused apply preserve target bytes.

### Clean context

Give blank Codex the exact `case-edit` outcome in a neutral fixture:

1. outcome only;
2. only “The clj-surgeon CLI is installed and available”;
3. version-matched skill.

Compare the stable pre-prototype CLI with the prototype. Primary adoption is
whether the first source-bearing operation after neutral awareness uses
`:edit`. Correctness, separate plan/apply, receipt verification, calls, tokens,
source bytes, and wall time remain gates and secondary measures.

## Build/keep gate

Keep `:edit` only if clean agents choose it with less task-specific prompting
than `:q`/`:replace-subform`, while preserving the complete safety boundary.
If agents still require exact syntax, or the alias only creates another help
branch without reducing detours, remove it and improve the skill/help for the
existing lens instead.
