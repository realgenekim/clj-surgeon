# Anvil Sol/high clj-surgeon interface study — interim findings

**Status:** Wave 1 running; wave 2 durably queued; no efficacy conclusion yet

## Scope

This is an interim, privacy-safe record of the controlled Anvil study comparing
three routes on frozen repository fixtures:

- current clj-surgeon CLI plus its matched skill;
- persistent clj-surgeon MCP;
- native Codex tools with neither clj-surgeon CLI nor skill.

Every paid run uses `gpt-5.6-sol` with reasoning `high`. Raw prompts, model
events, and workspaces remain in seat-owned Anvil result directories. This
observation records structured scores, bounded failure evidence, and exact CWDs
only. It intentionally has no `agent-usage-window-end` marker because the study
is incomplete.

## Execution locations

| Seat | Wave-1 arm | Source CWD | Result root |
|---|---|---|---|
| `dev-a` | CLI | `/srv/fleet/dev-a/clj-surgeon-study-20260823-sol` | `/srv/fleet/dev-a/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-cli` |
| `dev-b` | CLI+MCP | `/srv/fleet/dev-b/clj-surgeon-study-20260823-sol` | `/srv/fleet/dev-b/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-cli-mcp` |
| `dev-c` | none | `/srv/fleet/dev-c/clj-surgeon-study-20260823-sol` | `/srv/fleet/dev-c/clj-surgeon-study-results/2026-08-23-sol-high-full8-wave1-none` |

The wave-2 coordinator is detached under PID 1 on `anvil-server`, with CWD
`/srv/fleet/gene/clj-surgeon-study-20260823-sol`. It waits for all 24 wave-1
terminal receipts, refuses if any arm has fewer than eight, then rotates the
arms and starts two replicates of four discriminating tasks.

## Invalid first wave: useful apparatus failure, no treatment evidence

The first 12-run attempt reported CLI+MCP 2/4, CLI 0/4, and none 0/4. Those
numbers are invalid. Anvil's nested Bubblewrap sandbox could not create its
loopback interface (`RTM_NEWADDR: Operation not permitted`). CLI and native
filesystem/process tools therefore failed before source access, while MCP ran
outside that failed boundary. The comparison favored MCP by construction.

The CLI arm also exposed an undeclared host prerequisite: the benchmark's PATH
isolation invokes `/bin/zsh`, which Anvil did not have. `zsh` was installed,
the failed receipts were retained, and no failed run was merged into corrected
results.

The harness now accepts an explicit `BENCH_SANDBOX_MODE`. Anvil fixture runs use
`danger-full-access` only inside disposable standalone task repositories. This
is an experiment accommodation, not a recommendation for production source.
The default local sandbox behavior is unchanged. Harness and fixture self-tests
passed before relaunch.

Corrected one-task proofs established that both previously blocked controls can
mutate and score:

| Arm | Correct | Wall | Shell calls | Input tokens |
|---|---:|---:|---:|---:|
| CLI | yes | 83.7s | 12 | 211,985 cumulative |
| none | yes | 46.7s | 2 | 77,641 cumulative |

## Corrected wave-1 evidence as of 2026-08-23 14:19 Pacific

These are partial counts, not final rates:

| Arm | Completed | Correct | Current early pattern |
|---|---:|---:|---|
| CLI | 2/8 | 2/2 | Correct, but slower and much more action/context-heavy than native on the supplied decision. |
| CLI+MCP | 2/8 | 1/2 | Extremely fast on the complete multi-file decision; exact nested edit scored false. |
| none | 5/8 | 4/5 | Fastest so far; only the exact nested edit scored false. |

On the common complete multi-file decision, corrected wall times were 82.4s
for CLI, 23.6s for CLI+MCP, and 38.0s for none. All three were correct. This is
the first result supporting a task-shape router: MCP may earn persistence on
complete coherent batch decisions even if native wins smaller changes. One
replicate on one fixed seat is not causal evidence; wave 2 rotates seats and
adds replication.

## The MCP exact-nested "failure" is two product defects

The MCP route for `pair-view-expect-edit` did not misunderstand the requested
change. Its bounded event sequence was:

1. A subject-based `prepare-change` failed because the isolated MCP server had
   no cclsp semantic provider.
2. Exact file plus owner `prepare-change` succeeded and returned one decision
   site with the complete named form.
3. The first apply was correctly refused because the caller nested `expect`
   inside a prepared-basis compact edit, an unsupported contract location.
4. The second apply matched once, committed `:done` to `:complete`, passed
   clj-kondo, read back the file, and returned
   `verification_complete=true` and `next_action="none"`.
5. Exact-byte scoring still failed because managed whole-file formatting
   removed one pre-existing trailing blank line outside the intended edit.

The semantic mutation was right; the route was not one-shot and the
preserve-unrelated-bytes contract was violated.

### Smallest falsifiable improvement

For a task that supplies file, owner, literal before value, literal after value,
and exact count:

- compile one direct `apply_clojure_changes` request without semantic discovery;
- place the count expectation at the direct-change contract level rather than
  inside a prepared-basis edit;
- rebase formatter output against the formatted baseline, or equivalently
  format only the changed structural region, so pre-existing unrelated
  formatter drift is not committed;
- return either one verified commit or one typed refusal, with no retry loop.

Acceptance is exact: the frozen nested fixture passes byte-for-byte in one MCP
call, with one match, the attached comment and audit payload preserved, no
unrelated EOF change, and `verification_complete=true`.

## Experiment 2: hone the routing hypothesis

Wave 2 is queued behind a strict eight-receipt wave-1 barrier. It uses two fresh
replicates of four task shapes:

- complete multi-file supplied decision;
- owner-known exact nested edit;
- smallest exact nested native-control edit;
- exploratory multi-file change.

The arm rotation is:

| Seat | Wave 1 | Wave 2 | Exact CWD |
|---|---|---|---|
| `dev-a` | CLI | none | `/srv/fleet/dev-a/clj-surgeon-study-20260823-sol` |
| `dev-b` | CLI+MCP | CLI | `/srv/fleet/dev-b/clj-surgeon-study-20260823-sol` |
| `dev-c` | none | CLI+MCP | `/srv/fleet/dev-c/clj-surgeon-study-20260823-sol` |

This 24-run second wave tests whether the early decision-shape pattern repeats
after moving each arm to a different account/seat. It does not yet test Terra;
model comparison begins only after the tooling instrument is stable.

## Current interpretation

The strongest live hypothesis is conditional routing, not universal MCP:

- native tools for obvious, narrow, low-risk edits;
- one compiled MCP transaction for complete multi-owner decisions;
- CLI as a fallback when MCP is unavailable or the operation is not exposed;
- nREPL for live semantic probes and fast validation, not as a substitute for
  exact source mutation receipts.

The hypothesis is rejected if MCP's multi-file advantage does not replicate,
if exact nested MCP remains non-one-shot after the contract/formatter fix, or
if its complete-turn and resident-process costs erase the correctness benefit.

## Church-organ hypothesis: point, operator, guarded chord

The desired interaction is an expert editor's algebra, not a transaction
protocol tutorial:

```text
location/selection + operator + payload
                       |
                       v
             guarded atomic transaction
```

An LLM should be able to submit this intuitive shape:

```json
{
  "edits": [{
    "within": {"file": "src/bench/pair_view.clj", "form": "route-event"},
    "from": ":done",
    "to": ":complete"
  }],
  "verify": "fast"
}
```

One match, atomicity, and byte preservation should be defaults. Several edits
form one atomic chord. The server may internally use direct changes, retained
bases, structural lenses, formatter staging, and verification profiles; none
of those route distinctions belong in the caller's motor plan.

### Ranked hypotheses

1. **Opaque structural point plus compare-and-swap is the missing primitive.**
   A structural read should return an action-ready anchor containing workspace,
   file, owner identity, structural location, and exact owner/subtree hash. At
   commit, re-read the latest file, re-resolve the location, and apply only if
   the guarded target is unchanged. Concurrent edits elsewhere in the file may
   survive; a changed target returns typed `stale-target` and no write.
2. **The current API leaks compiler modes.** Asking the model to choose direct
   versus basis routes, `forms` versus namespace-only `owner`, semantic versus
   exact-source preparation, and the legal location of `expect` creates
   protocol failures unrelated to the edit. One visible edit algebra should
   normalize intuitive requests into the existing kernels.
3. **`from` should be the guard.** Default expected matches to one. A prior
   action-ready anchor adds a stronger source hash, but callers should not
   manually supply aggregate counts, per-form counts, basis IDs, and site IDs
   for a simple change.
4. **Exact local edits must not depend on cclsp.** File plus form plus exact old
   subtree is sufficient location evidence. Semantic expansion is valuable
   only when the affected surface is genuinely unknown or crosses owners.
5. **A scalpel must not normalize the whole patient.** Whole-file formatting
   turned a correct one-token edit into unrelated EOF drift. Format the changed
   subtree in context, or subtract the formatter's baseline delta from the
   candidate delta. Repository-wide normalization is a separate explicit
   transaction.
6. **Tolerant compilation beats better prompting.** The model's rejected
   `edit.expect` placement was reasonable. The boundary can normalize that
   intuitive shape, or return one machine-ready corrected call. Skills and
   longer descriptions cannot make a needlessly brittle schema perfect.
7. **Receipts should be tiny and terminal.** Success should return changed
   targets, before/after hashes, match counts, verification, undo identity, and
   `next_action=none`. Failure should return one typed cause and, only when
   safe, one complete retry call. Large source echoes and prose remedies invite
   another model turn.
8. **One-shot is a caller property.** The server may inspect, lock, stage,
   format, lint, run laws, atomically write, and read back internally. These are
   not seven tool calls; they are the implementation of one keystroke.

### Product experiment

Add the simple `edits` surface to the existing `apply_clojure_changes` tool and
compile it into the current direct transaction kernel. Do not add another MCP
tool or mutation path. First acceptance battery:

- 10/10 clean-agent exact-nested edits complete in one MCP call;
- exact bytes pass, including the trailing-blank-line fixture;
- cclsp is unavailable and the local edit still succeeds;
- changing the guarded owner between selection and commit refuses with no byte
  change;
- changing an unrelated owner between selection and commit is preserved while
  the guarded edit succeeds;
- a four-edit chord commits or refuses as one unit;
- p90 complete turn is below 30 seconds with no schema retry.

Run this entire battery locally first. Add an interactive local dogfood pass
that explicitly judges whether the surface feels like point + operator +
payload rather than transaction-protocol construction. Then run the 10 clean
local agents. Only after both the mechanical battery and qualitative feel gate
pass may the exact admitted interface and skill hashes be copied to Anvil.

### First local self-host result

The smallest slice passed its decisive qualitative test on 2026-08-23. The
live server accepted one edit naming
`src/clj_surgeon/mcp_tool.clj`, owner `tool-description`, the exact old
string, and its replacement. No ID, basis, site, or count fields were needed.

Surgeon derived the change ID and all match/file counts, changed its own
description through the existing transaction kernel, read the bytes back,
passed focused lint, and emitted an inverse receipt. The formatter ran but
reported zero changed files. Replaying the identical request found zero
matches and refused with `source_unchanged=true`.

The server remained PID 48029 with CWD
`/Users/genekim/src.local/clj-surgeon`; live contract hashes advanced from
`386f140c` to `3465d1cc` and then `76b1b91f`, each synchronization
reporting `server-restart-required=false`. This proves both the ergonomic
route and the in-place feedback loop. It does not yet satisfy admission: the
successful call still took about 20 seconds, including about 14 seconds in a
formatter that changed nothing, and the exact trailing-blank-line fixture plus
10/10 clean local-agent gate remain pending.

Only after that local gate should instructions teach the surface as the default
or Anvil scale it. The standard is not that a sufficiently coached Sol agent
can eventually use the tool; it is that a fresh capable model finds the shortest
safe path obvious.

### Second local self-host result: exact known multiplicity

The first attempt to repair the new boundary test correctly refused: its old
configuration map appeared twice inside the named `deftest`, while the compact
gesture defaulted to one match. That was safe but not yet editor-like for the
intended replace-both operation.

The compact edit object now accepts optional positive `matches`, defaulting to
one. It still has no unbounded replace-all. The compiler binds `matches` to the
per-owner and per-file guards and derives the aggregate edit count. After an
in-place nREPL reload, the live contract hash advanced from `76b1b91f` to
`e71c3a67` without changing PID 48029 or CWD
`/Users/genekim/src.local/clj-surgeon`.

One live call then used `matches: 2` to change both nested configuration maps
inside clj-surgeon's own boundary test. It committed 2 edits in 1 file,
verified read-back, and returned terminal evidence in 2.1 seconds. Identical
replay refused with `expect-count-mismatch` and source unchanged. The focused
persistent-nREPL suite passed 48 tests and 498 assertions with zero failures or
errors, including durable one-match and exact-two-match commit, stale replay,
receipt, and undo coverage.

This materially widens the one-call region, but it does not yet admit the
interface for Anvil: the 10/10 fresh-agent gate and the real formatter-baseline
drift fixture remain pending. Selecting one among duplicate matches also still
needs the proposed opaque location handle; exact `matches` intentionally means
"all N guarded occurrences," not "the Nth occurrence."

## Later checkpoint: 2026-08-23 14:34 Pacific

Wave 1 was one terminal receipt from completion:

| Arm | Correct / completed | New evidence |
|---|---:|---|
| CLI | 7/8 | Highest corrected reliability so far; failed only the three-site semantic deletion, but paid 5–33 shell calls and 45.6–171.9s per task. |
| CLI+MCP | 5/7 | Very fast wins on supplied batch, exploratory, literal, and exact-small tasks; failed dependency movement after 750.1s and 1.71M cumulative input tokens. |
| none | 6/8 | Usually least ceremony and fastest; failed the exact nested preservation task and three-site semantic deletion. |

This weakens any early claim that native is globally sufficient. CLI currently
looks like the broadest reliable fallback; MCP looks bimodal—sometimes a
one-call step-function improvement, sometimes a refusal/retry sink. The product
goal is therefore to widen the one-call MCP region, beginning with the exact
nested direct-change and formatter-preservation defects above, while retaining
CLI as an escape hatch.
