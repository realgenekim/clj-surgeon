---
name: clj-surgeon
description: >-
  Carries the canonical Clojure EDIT ROUTING table (measured 2026-09-06, build >= 13c12401) plus advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Production clj-surgeon routes (advanced router)

Optimize complete verified task time. The installed Babashka CLI is the
production entrance. Persistent MCP is a development-only, months-long
experiment; use it only when explicitly testing that service.

## Edit routing (measured 2026-09-06, build >= 13c12401)

This section is the CANONICAL routing text. The `CLJ-SURGEON ROUTING v:2`
managed block, `safe-refactor`, the repository `CLAUDE.md` and `AGENTS.md`, and
`docs/observations/2026-09-06-routing-prompt-surfaces.md` render this same
table. They do not paraphrase it. Change it here first.

### The table

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter, add a require across namespaces) **and** scope, proof profile, provider permission, and measured admission facts already fit | Try the `bin/mission` executor first. Do not invent a profile or a prior to force eligibility. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for this executor on this build; not forbidden territory. |
| Tonight's mandated dogfood experiment, eligible edit | Executor first, then one ledger line. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

### Entrance commands, as they exist on this branch

`bin/mission help` runs on babashka in about 0.05 s. Read it before guessing.
`help`, `show`, `list`, `ready`, and `blocked` run on babashka. `propose`,
`plan`, `apply`, `run`, `resume`, `undo`, and `link` start a JVM.

One JVM, plan and write together, no review step:

```bash
bin/mission run --spec-file - --state-home "$H" <<'EDN'
{:verb "owner_forms"
 :question "why this write is being made"
 :request {:op "owner_forms"
           :workspace_root "/abs/path"
           :scope {:paths ["src/**/*.clj"]}
           :verification {:profile "mission-proof"}}}
EDN
```

Review before write:

```bash
bin/mission propose --spec-file spec.edn --workspace "$WS" --state-home "$H"
bin/mission show    M-1 --workspace "$WS" --state-home "$H"
bin/mission apply   M-1 --workspace "$WS" --state-home "$H"
bin/mission undo    M-1 --workspace "$WS" --state-home "$H"
```

`open` is an alias of `propose`. `run` supports the `owner_forms` verb only.
`apply` exits non-zero on a refusal or a failed receipt. The proof profile is
read from `<workspace_root>/.clj-surgeon.edn`:

```edn
{:verification-profiles {"mission-proof" {:commands [["/bin/true"]]}}}
```

Supply a profile that actually proves the change. Do not point a profile at
`/bin/true` to make a mission eligible.

The mission executor is a PROTOTYPE dated 2026-09-05. Its kernel source commit
is not yet a git commit on any published branch. Say so when you cite it.

### The production entrance is the CLI

The installed Babashka CLI is the production entrance. Persistent MCP is
explicit development work; do not start an MCP server for an ordinary edit, and
do not state an unconditional preference for it. Call a semantic provider only
in a repository or profile that has opted in.

### A typed refusal

A refusal is EDN in the repository receipt shape:

```edn
{:ok false :operation "mission" :error_type "mission-<suffix>" :error "one sentence" :next_call nil}
```

1. Read `:error_type` and `:error`.
2. Retry only when new evidence or a concrete supported correction lifts the
   stated reason.
3. Otherwise finish the edit natively. That is a legitimate outcome, not a
   defeat, and it is recorded.
4. Record the provenance as actual or user-reported. Do not widen scope to make
   a refusal disappear.

### The tie-break

Complete verified task cost decides: orientation, refusals, retries, emission,
and proof. Not the existence of a receipt. A receipt must state its actual
guarantees, its unknowns, and whether its evidence is test or live.

### The dogfood ledger line

For the mandated experiment, record one line per Clojure edit in the session
work log:

```text
dogfood | <edit class> | <route> | <refusal type or -> | <wall seconds>
```

`route` is `executor`, `native-after-refusal`, or `native-ineligible`.

### The numbers, with their qualifiers

| Meter | Result | Qualifier |
|---|---|---|
| Executor adoption in the pilot | executor-first | MANDATED, not chosen. Not an adoption signal. |
| Terminal latency, Astra caller, complete CLI | 3.05x | Came with a reliability LOSS: 3/4 versus 4/4. |
| Bench harness wall | 11x | Single harness, not replicated. |
| Unified-diff route acceptance | 0 of 20 | Why the executor spec shape is used at all. |

The raw pilot is not a replicated speedup. Never render these bare.

### Why this replaces the 2026-09-02 ruling

The 2026-09-02 ruling ("native is the default route; do not mention Surgeon in
agent prompts") was measured on the Sol caller, the pre-`13c12401` build, and
the MCP per-form editing grammar. Under those conditions the agent kept its
native read and patch loop and layered the tool on top, paying about 2x wall
and 2x actions with no quality meter clearing the noise floor.

The mission executor is a different mechanism: owners, intended forms, and a
proof profile in; a verified commit with a receipt and an undo out, or a typed
refusal before any write. A rule measured on the first mechanism does not
govern the second. It also does not license the second beyond what has been
measured, which is why the table keeps native as the production default for
small literal changes.

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
clj-surgeon :op :change! :spec-file - <<'EDN'
{:changes [{:id :rename
            :in ["src/app.clj"]
            :forms [run]
            :find :old
            :do [:replace :new]
            :expect {:matches 1 :each-form 1}}]
 :expect {:changes 1 :edits 1 :files 1}}
EDN
```

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
