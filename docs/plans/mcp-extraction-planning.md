# MCP Extraction Planning

## Outcome

Expose the non-mutating extraction planner already used by the CLI through
`inspect_clojure`, then feed its exact evidence into the existing
failure-atomic `apply_clojure_changes` extraction executor. A coding agent that
has chosen a destination namespace and candidate roots should be able to plan
once, decide the semantic caller migrations, and execute once without dropping
to a process-starting CLI or reconstructing the migration manifest by hand.

The target route is:

```text
inspect_clojure mode=plan-extraction
    -> bounded migration manifest + frozen source hash + ready next_call
model decides caller_changes and explicit ignores
    -> apply_clojure_changes once
    -> terminal transaction receipt
```

## Field evidence

The historical Sessionize split at commit
`c4299615dcf6c37b4d929892fb3bb0b6b7a44322` moved a 4,594-line `views.clj`
into roughly twenty namespaces. Its 442-line plan used CLI dependency and
extraction previews before applying each move. The MCP executor can already
perform one failure-atomic extraction, but MCP callers cannot obtain the same
non-mutating plan. They must already know the exact moved forms, all structural
caller candidates, caller edits or ignores, and aggregate counts, or switch
interfaces.

The first representative dogfood case is one bounded extraction unit derived
from that campaign, not the complete 43-file historical commit. The initial
candidate is the 15-form `format` unit; `organizer-layout` is a later,
forward-reference-heavy stratum.

## Observable contract

`inspect_clojure` gains a mutually exclusive top-level mode:

```json
{
  "workspace_root": "/absolute/project",
  "mode": "plan-extraction",
  "file": "src/sample/views.clj",
  "to": "src/sample/views/format.clj",
  "forms": ["format-date", "format-time"],
  "require_policy": "minimal"
}
```

On success it returns:

- the source and destination namespace identities;
- the exact requested form set and count;
- dependency and require evidence from the existing pure planner;
- every structurally detected caller candidate and quoted-Var reference;
- exact returned, omitted, and truncated evidence counts;
- the frozen source hash used to build the plan;
- previews needed for human or model judgment, subject to the public result
  budget; and
- one `next_call` for `apply_clojure_changes` whose extraction payload already
  contains the mechanical fields and source hash. The caller fills only
  `caller_changes` and `ignored_caller_files`.

The plan never writes, never retains write authority, and never labels a
structural caller candidate as semantically complete. Similarity is not
authority. If evidence cannot fit the bounded public result, planning refuses
rather than silently omitting a caller.

The execution adapter accepts an optional planned `source_hash`. When present,
it must equal the exact source used by the executor's fresh workspace snapshot
or the transaction refuses before any write. A direct one-call extraction may
omit it because planning and commit already occur inside one captured request.

Aggregate extraction expectations are derived from the exact form list,
caller-change guards, and affected file set. Existing explicit `expect` remains
accepted and authoritative for compatibility; a mismatch still refuses.

## Architecture

1. Extract the existing project-wide Clojure source enumeration into one small
   shared module used by both inspection and mutation. It returns canonical
   paths and exact bytes in deterministic order.
2. Add a pure extraction-plan adapter over `extract/compile-plan`. It strips
   private future-source fields, attaches bounded evidence counts, and compiles
   the guarded next call.
3. Route `mode=plan-extraction` through `mcp_inspect_tool` without adding a
   fifth public MCP tool.
4. Add `source_hash` and derived expectations to the existing extraction
   contract and executor. Do not create a second extraction implementation.

The shared scanner, pure plan adapter, inspect transport, and execution guard
remain independently testable and cherry-pickable seams.

## Behavior matrix

| Case | Planning result | Write authority |
|---|---|---|
| Exact source, destination, and unique forms | Complete bounded manifest and guarded next call | None until apply succeeds |
| Missing or repeated form | Typed refusal with exact failed selector | None |
| Destination already exists or aliases conflict | Existing planner refusal | None |
| Caller candidates exist | Return all candidates; require change or explicit ignore at apply | None |
| No caller candidate exists | Report zero candidates without claiming semantic completeness | None |
| Quoted-Var reference exists | Return exact authority and location | None |
| Evidence exceeds public budget | Typed bounded refusal with counts | None |
| Source changes after planning | Apply refuses `source-hash-mismatch` before writing | None |
| Explicit expectations disagree with derived reality | Apply refuses before writing | None |
| Apply is interrupted and no receipt returns | Outcome is unverified, never safe-to-retry | Unknown until inspected |

## Non-goals

- Inferring the destination architecture or which roots should move.
- Guessing semantic caller rewrites.
- Combining the complete Sessionize namespace campaign into one mutation.
- Adding heuristic ranking, a retained adaptive recovery engine, or another
  workspace index.
- Making CLI and MCP transport output byte-identical.
- Weakening comment, metadata, reader-discard, lint-directive, rollback, or
  verification guarantees.

## Test layers

1. Pure plan-adapter tests with literal sources: complete manifest, stripped
   private fields, deterministic next call, explicit evidence counts, and
   bounded refusal.
2. Pure contract tests: omitted expectations derive correctly; explicit
   expectations remain compatible; stale source hash refuses.
3. Boundary test in a real-program-derived temporary workspace: plan, mutate
   source to prove staleness refusal, restore, fill caller decisions, apply,
   parse/read back every file, and inspect the terminal receipt.
4. Live hot-reload proof: reload the changed handler and schema without
   restarting port 7888, then run the exact public two-call route.
5. Milestone gates: formatter, focused tests, `make mcp-test`, `make runtests`,
   lint, heap/cclsp regression gates, and `git diff --check`.

## Benchmark gate

Freeze the prompt, fixture, model, and branch SHA. Run at least three fresh,
counterbalanced Sol/high pairs before scaling. Compare:

- native planning and extraction;
- current CLI-assisted extraction; and
- MCP plan plus MCP apply.

Report complete verified task wall, action count, route phases, source
characters returned, refusals, mutation attempts, semantic correctness,
meaning-bearing source preservation, and exact-byte presentation separately.
The improvement earns default routing only if it reduces complete task time or
risk on the real extraction stratum. Fast server execution alone is not a win.

## Implementation checkpoint: 2026-08-26

The first vertical slice is live on the experiment branch:

- `inspect_clojure mode=plan-extraction` calls the production pure planner;
- planning and execution share deterministic workspace source enumeration;
- public plans strip private future-source fields and carry exact evidence
  counts;
- `next_call` carries a frozen source hash and empty semantic decision arrays;
- apply derives aggregate counts while preserving explicit-count compatibility;
- stale source refuses before extraction compilation; and
- `make mcp-reload` publishes the schema without restarting port 7888.

The public two-call dogfood succeeded. Plan took 421.61 ms. Apply took
12,570.92 ms, of which 12,060.17 ms belonged to formatter startup. A generic
owner name also produced 29 structural candidates for one real caller. These
two findings define the immediate hill climb: reduce formatter fixed cost and
improve caller-candidate precision without truncating evidence or introducing
heuristic authority.
