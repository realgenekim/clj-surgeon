---
name: clj-surgeon
description: >-
  Carries the canonical Clojure EDIT ROUTING table (policy revision 1, 2026-09-06) plus advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Production clj-surgeon routes (advanced router)

Optimize complete verified task time. The installed Babashka CLI is the
production entrance. Persistent MCP is a development-only, months-long
experiment; use it only when explicitly testing that service.

## Edit routing (policy revision 1, 2026-09-06)

This section is the CANONICAL routing text and the SOURCE of every copy. The
`CLJ-SURGEON ROUTING v:2` managed block points at it; `skills/safe-refactor`,
the repository `CLAUDE.md` and `AGENTS.md`, and
`docs/observations/2026-09-06-routing-prompt-surfaces.md` render its table
verbatim. Change it HERE first, then re-render the copies and re-run the parity
check (`bb bin/check-routing-parity.clj`). Installed skill mirrors follow from
this working tree through `make install-claude-skill` and
`make install-codex-skill`; the working tree is authoritative and the mirrors
are stable copies of it.

You do NOT have to load this skill before every edit. The managed block carries
the routing summary; a known literal change with sufficient context already in
hand pays no extra read boundary. Read this section when the route is not
already decided, and load the references below only for the advanced workflows
they cover.

### The table

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter) | Choose native or a deterministic Surgeon route by COMPLETE VERIFIED TASK COST. There is no executor-first rule in production. |
| Extraction to a new namespace; namespace rename; a require added or changed across namespaces; a surgical edit inside one known form | The earned deterministic Surgeon routes: `:extract!`, `:rename-ns!`, `require_change`, `within` plus `from`/`to`. Kept from the 2026-09-02 ruling: no native equivalent, or measured zero churn. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for the experimental executor on this build; not forbidden territory. |
| Under the mandated dogfood EXPERIMENT only, explicitly opted into, an eligible bounded mechanical edit | Try the `bin/mission` executor FIRST, then write one ledger line. Executor-first is the experiment's rule; it does not govern production routing. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

### Entrance commands, as they exist on this branch

`bin/mission help` runs on babashka in about 0.05 s. Read it before guessing.
`help`, `show`, `list`, `ready`, and `blocked` run on babashka. `propose`,
`plan`, `apply`, `run`, `resume`, `undo`, and `link` start a JVM.

Run a saved, complete spec in one JVM (plan and write together, no review step):

```bash
bin/mission run --workspace "$WS" --state-home "$H" --spec-file owner-forms.edn
```

Review before write:

```bash
bin/mission propose --spec-file spec.edn --workspace "$WS" --state-home "$H"
bin/mission show    M-1 --workspace "$WS" --state-home "$H"
bin/mission apply   M-1 --workspace "$WS" --state-home "$H"
bin/mission undo    M-1 --workspace "$WS" --state-home "$H"
```

`open` is an alias of `propose`. `run` supports the `owner_forms` verb only and
refuses an existing mission id. `apply` exits non-zero on a refusal or a failed
receipt. The spec schema — every required key, and why each one exists — is
[docs/mission-typist.md](../../docs/mission-typist.md).

No spec is written out here. `docs/mission-typist.md` is the schema of record;
build the spec from it against the real workspace, and never copy profile ids,
timings, or evidence strings out of a fixture.

`test/clj_surgeon/mission_typist_executor_test.clj` (`real-proof-commit-and-undo`)
is a synthetic integration witness — it redefines `request-candidates!`, so it
proves deterministic fixture plan, commit, and undo only; it is not measured
admission data, and neither its profiles nor its rate numbers describe live
provider behavior.

The gate and the acceptance profile must be independently authored and must run
DIFFERENT commands with different retained evidence; the planner refuses
identical proof commands (`:typist-identical-proof-commands`). Supply a profile
that actually proves the change. Never point a profile at a command that cannot
fail.

The mission executor is a PROTOTYPE dated 2026-09-05. Its kernel source commit
is not yet a git commit on any published branch. Say so when you cite it.

### The production entrance is the CLI

The installed Babashka CLI is the production entrance. Persistent MCP is
explicit development work; do not start an MCP server for an ordinary edit, and
do not state an unconditional preference for it. Call a semantic provider only
in a repository or profile that has opted in.

### A typed refusal

Refusals do not all share one spelling. Read whichever of these the receipt
carries, at whatever depth it carries it:

- `:error_type` as a STRING beginning `mission-`, in a CLI dossier — e.g.
  `"mission-not-ready"`, `"mission-workspace-required"`.
- `:error-type` as a KEYWORD, from the kernel — e.g.
  `:forms-protected-syntax` (the owner contains comments or other protected
  syntax; no mutation was attempted), `:forms-comment-lost`,
  `:forms-comment-moved`, `:forms-owner-mismatch`,
  `:typist-all-candidates-rejected`, `:typist-stale-plan`,
  `:typist-identical-proof-commands`, `:typist-invalid-undo-hash`.
- NESTED diagnostics: a refusal may arrive inside `:candidates`, `:proof`, or
  `:decision` rather than at the top level. `(:ok result)` false with no
  top-level code is still a refusal — walk the map before retrying.

A CLI dossier refusal has this shape:

```edn
{:ok false :operation "mission" :error_type "mission-<suffix>" :error "one sentence"
 :decision "what to do instead" :next_call nil}
```

1. Read the code and the `:error` and `:decision` sentences.
2. Retry only when new evidence or a concrete supported correction lifts the
   stated reason. Never retry an identical request after
   `:forms-protected-syntax`.
3. Otherwise finish the edit natively. That is a legitimate outcome, not a
   defeat, and it is recorded.
4. Record the provenance as actual or user-reported. Do not widen scope to make
   a refusal disappear.

### The tie-break

Complete verified task cost decides: orientation, refusals, retries, emission,
and proof. Not the existence of a receipt. A receipt must state its actual
guarantees, its unknowns, and whether its evidence is test or live.

### The dogfood ledger line

For the mandated experiment ONLY, record one line per Clojure edit in the
session work log:

```text
dogfood | <edit class> | <route> | <refusal type or -> | <wall seconds>
```

`route` is `executor`, `native-after-refusal`, or `native-ineligible`.

### The numbers, and exactly where each one comes from

Policy revision 1, dated 2026-09-06. No figure appears here without the
artifact it was read from.

| Meter | Result | Where it comes from | Qualifier |
|---|---|---|---|
| Median complete command wall, four prepared-change pairs | 3.05x | Astra forms cohort: `/var/tmp/forge/astra-forms-cohort-fx/summary.json` (written 2026-09-06 02:19:03Z); write-up `docs/observations/2026-09-06-astra-forms-cohort-result.md`, measurement stamped 2026-09-06T02:20:24Z, cohort engine `0a49f012` | Tool 3/4 verified against native Sol 4/4 verified. Latency only, bought at LOWER reliability. It is NOT an Astra-caller-versus-Sol-caller comparison. n=4; the native SD widened to 2SD 15.585s. |
| Codex `apply_patch` V4A payloads refused by a unified-diff-only gate | 69-75% of admit calls | `docs/observations/2026-09-02-resume-here-bridge-program.md`, UPDATE 15:41Z | Why a structured spec shape is used instead of a diff string. A different gate, not this executor. |
| The 2026-09-02 native-default ruling | ~2x wall and ~2x actions for a tool-mandated agent, no quality meter clearing the noise floor | `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`; 81 arm-runs, verified servers, two blind judges | Measured on the Sol caller, on the build before `13c12401` ("merge: helper_extraction: request-shape refusals carry :field, :decision and a runnable :example"), and on the MCP per-form editing grammar. |
| Executor-first in the pilot | not a measurement | this section's experiment row, and `docs/observations/2026-09-06-routing-prompt-surfaces.md` | MANDATED by the experiment, not chosen by any agent. Carries no adoption signal and no speedup claim. |

No 11x bench figure is carried. It was a single unreplicated harness reading
with no retained receipt naming its scope, and it has been removed.

The EXPERIMENTAL ENGINE is named separately from production routing: the
`bin/mission` `owner_forms` executor, PROTOTYPE dated 2026-09-05, cohort engine
`0a49f012`. Production routing in the table above does not depend on it and does
not change if it is withdrawn.

### Why this replaces the 2026-09-02 ruling

The 2026-09-02 ruling ("native is the default route; do not mention Surgeon in
agent prompts") was measured on the Sol caller, the pre-`13c12401` build, and
the MCP per-form editing grammar. Under those conditions the agent kept its
native read and patch loop and layered the tool on top, paying about 2x wall
and 2x actions with no quality meter clearing the noise floor.

That ruling's EXCEPTIONS survive unchanged and keep their row in the table:
`:extract!`, `:rename-ns!`, `require_change`, and `within` plus `from`/`to` were
the measured winners then and are the measured winners now.

The mission executor is a different mechanism: owners, intended forms, and a
proof profile in; a verified commit with a receipt and an undo out, or a typed
refusal before any write. A rule measured on the first mechanism does not
govern the second. It also does not license the second beyond what has been
measured, which is why the table keeps production routing on complete verified
task cost and confines executor-first to the opted-in experiment.

### Reassessment

Reassess at every Gene report. There is no automatic or silent expiry and no
policy flip without a decision.

Read `~/.clj-surgeon/events.jsonl` (env `CLJ_SURGEON_EVENTS_FILE`);
`make study-agent-events` counts the box-wide ledger. Each reassessment records:

1. The measured build.
2. The route taken, per edit class.
3. Any counterexample found.
4. The explicit decision delta, including "no change".

## Choose the cheapest authority

Route by the table above. Then pick the operation: `:ls`, `:cat`, or
`:match-form` for unknown structural owners; `:edit` for an exact nested
replacement; `:change!`, `:extract!`, `:mv-with-deps`, `:rename-ns!`, or
`:fix-declares!` for guarded cross-file work.
Surgeon earns its cost when one guarded operation replaces many owner reads and
writes. Historical favorable fan-out cohorts reached roughly 3–10x complete-task
speedup; tiny edits often favor native tools. Treat those figures as
workload-specific priors, never guarantees.
## Timing and safety

Count complete verified task time, including orientation, refusals, retries,
emission, and proof. Tool runtime alone is not end-to-end speed. Inspect the
EDN receipt, stop on `:error`, run focused tests or lint, and keep one coherent
operation per commit.
## Avoid shell quoting

For any nontrivial plan, put the structured request on stdin. This is the CLI
equivalent of MCP's structured arguments and avoids nested shell quoting:

```bash
clj-surgeon :op :change! :receipt-out ./change-receipt.edn :spec-file - <<'EDN'
{:changes [{:id :rename
            :in ["src/app.clj"]
            :forms [run]
            :find ":old"
            :do [:replace ":new"]
            :expect {:matches 1 :each-form 1}}]
 :expect {:changes 1 :edits 1 :files 1}}
EDN
```

`:find` and the replacement are source strings, even for a keyword literal.
`:change!` requires a writable `:receipt-out` path for its guarded undo receipt.
Use `:spec-file PATH` for a saved request. Attach stdin in the same shell
action; never invoke `:spec-file -` and wait for a later input stream.
Never run `clj-surgeon up` casually. It is development-only, edits workspace
agent configuration, starts local services, and requires an explicit guard:

```bash
clj-surgeon up /absolute/repository --force
```

## Syntax trip-wire

Every call is `:op <name>` plus key-value pairs; positional guesses produce
`Unknown op`. Known-good smoke test:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```
## References

Read [CLI fallback](references/cli-fallback.md) for full syntax and receipts,
[advanced CLI operations](references/advanced-operations.md) for extraction,
moves, renames, or CLJC, and [advanced MCP routes](references/mcp-advanced.md)
only for explicit development-service work. Do not reopen a reference already consumed.
