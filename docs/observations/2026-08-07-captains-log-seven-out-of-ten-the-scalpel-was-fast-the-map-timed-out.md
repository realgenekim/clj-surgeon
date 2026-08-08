# Captain's Log: 7/10 — the scalpel was fast, the map timed out

## Verdict

For this Mothership investigation, the clj-surgeon MCP stack was **7/10 helpful**.

The structural read path was excellent. The semantic caller-proof path failed at
the point where it mattered most.

## What I was trying to learn

Mothership appears to contain two full-page implementations behind related URLs:
the long-lived `ide-shell` and the newer `source-reader-shell`. The practical
question was not merely which files mention those names. It was which routes and
callers still depend on each shell, and therefore what can be pruned without
silently breaking navigation, Edge-only `Cmd-L`, or the remaining
`app-doomed.js` and `app-purgatory.js` consumers.

That is exactly the kind of task where structural source plus resolved references
should beat text search.

## What worked

`clj-surgeon up /Users/genekim/src.local/mothership` was a clean paved road:

- both shared services were already ready;
- the workspace was loaded successfully;
- readiness named the exact cclsp health URL;
- no server restart or agent-session restart was required;
- the command returned in about 1.2 seconds.

Once the tools became visible, one `inspect_clojure` call read one coherent
snapshot containing eight named forms in four files:

- `ide-shell-handler` and `make-multi-routes`;
- `migrated-project-ids`, `legacy-deep-link-location`, and `page-handler`;
- `ide-shell` and `source-reader-shell`;
- `-main`.

It completed in **1,029 ms**, attached hashes, resolved every requested form, and
returned `read_complete=true`. The request replaced four separate source-reading
round trips and made the shell split concrete without dumping four namespaces.
This part felt like the intended product: fast, bounded, exact, and easy to trust.

## What failed

The next step asked cclsp for the resolved surfaces of four Vars:

- `app.views.ide-layout/ide-shell`;
- `app.views.ide-layout/source-reader-shell`;
- `app.source-reader/legacy-deep-link-location`;
- `app.source-reader/page-handler`.

All four returned only:

```text
Error: LSP request timeout: workspace/symbol (30000ms)
```

Although the caller launched the requests together, it waited roughly 102 seconds
before receiving the four failures. There was no definition, no reference list,
no partial surface, no reported recovery attempt, and no next action. The error
also named `workspace/symbol`, even though the user-level operation was
`resolve_var_surface`; that leaks an internal phase without explaining what the
caller should do.

This failure was consequential. Safe retirement depends on proving the caller
surface. The fast structural snapshot could explain the architecture, but it
could not authorize deletion. I had to stop short of claiming that either shell
or any legacy JavaScript file had zero consumers.

## Why the score is 7 rather than 5

The MCP entrance materially improved the investigation before it failed. Joining
the shared stack was immediate, tool discovery worked without restarting the
session, and the one-shot structural read was almost ideal. I left with a precise
map of the relevant owners and route decisions.

The failure was in a separate provider boundary, not in the structural reader or
transaction engine. A caller can still use the exact source snapshot and choose a
different semantic route.

## Why the score is 7 rather than 9

Resolved-reference proof was not optional for this task. The system spent about
100 times longer failing to produce the semantic map than it spent producing the
complete structural snapshot. Four identical terminal strings gave no evidence
that the documented exact-root recovery occurred and no way to distinguish a
cold index, a wedged child, an overloaded workspace, or a permanently unresolved
Var.

The product is therefore excellent at answering, "What do these known owners
contain?" It was not yet dependable at answering, "Who still calls them?" in the
same live session.

## The next experiment

Replay one `resolve_var_surface` request against Mothership and require one of two
bounded outcomes:

1. a complete definition-and-reference surface within 35 seconds; or
2. a typed refusal that includes the workspace root, requested Var, failed LSP
   phase, child PID/session, recovery attempted/result, elapsed time, any retained
   definition evidence, and one executable retry or fallback.

Then add a batched related-Var entrance so one architectural question can submit
the four shells and route owners as one semantic decision. The caller should not
pay four independent workspace-symbol searches—or receive four copies of the
same opaque timeout—to prove one retirement boundary.

The structural scalpel is already real. The semantic map must become equally
bounded and evidence-bearing before the combined MCP experience earns 9/10.

## Addendum: the map became bounded

The replay found two different defects, not one slow Mothership repository.

First, the original request threw away exact definition evidence and began with
fuzzy `workspace/symbol`. The structural reader already knew each file, owner,
range, and source hash. Named-form results now return that data as a directly
callable `source_anchor`. `resolve_var_surface` verifies the anchor, opens that
document, and asks for its symbols without workspace discovery.

Second, cclsp recovery sent `SIGTERM` and started the replacement immediately.
The old and new `clojure-lsp` processes could overlap. The replacement could
then fail to finish initialization, so the retry paid another timeout. Recovery
now waits for the old process to exit, escalates to `SIGKILL` after three
seconds, initializes the replacement, and reports the full lifecycle before it
retries.

Direct control measurements disproved the large-repository hypothesis. A fresh
Mothership `clojure-lsp` initialized in 3.111 seconds and returned the first
document symbols in 3.739 seconds. Four exact anchored surfaces then resolved
under one session in 2.539 seconds:

| Var | References |
|---|---:|
| `app.views.ide-layout/ide-shell` | 8 |
| `app.views.ide-layout/source-reader-shell` | 2 |
| `app.source-reader/legacy-deep-link-location` | 6 |
| `app.source-reader/page-handler` | 10 |

The first forced-wedge replay still took 32.452 seconds because one timeout did
two jobs: cold initialization and interactive request patience. Splitting those
budgets changed the shared defaults to 30 seconds for initialization and 10
seconds for an interactive request. The same deliberate wedge then recovered,
replayed, and returned the same eight-reference `ide-shell` surface in **12.457
seconds**. Roughly 20 seconds was wrapper policy, not useful `clojure-lsp` work.

The original four-call question now also has one entrance:
`resolve_var_surfaces` accepts up to four ordered exact anchors and returns each
complete surface or typed refusal. A successful recovery carries the old and
new sessions, old and new child PIDs, exit mode, and termination time. A failed
provider path returns before the MCP deadline with the failed LSP phase,
retained anchor, and executable next call.

The final live acceptance used that entrance, not four independent calls. It
resolved all four Mothership Vars in input order in 2.988 seconds, returned
reference counts 8, 2, 6, and 10, and bound every outcome to the same LSP
session.

This does not make semantic resolution infallible. It makes success provable
and failure bounded. The structural scalpel and semantic map now share the same
product rule: never make the caller perform recovery archaeology.

## Mothership follow-up: 8/10 — it found the seam and guarded the writes

The next Mothership session turned the architectural finding into a live
performance refactor. The source reader was returning 18,970,703 bytes of HTML,
including 7,085 explorer nodes and 3,458 rows in hidden Recent and Docs panels.
The task was to preserve selected-file reveal and directory navigation while
moving unopened subtrees behind server-rendered fragment boundaries.

For this work the combined clj-surgeon experience was **8/10 helpful**.

The strongest contribution was decision compression. Bounded structural reads
opened only the relevant forms in the 1,500-line view namespace and 3,000-line
route namespace. One `prepare-change` call then resolved the definition and
eight references of `tree-node` in 3.858 seconds. A second preparation proved
the complete Recent/Docs/sidebar surface in 4.157 seconds. Those surfaces made
it safe to change the hidden-panel contract without pretending that grep was a
caller proof.

The write path was also materially useful. One three-edit transaction inserted
the external explorer controller into `common-head`, `ide-shell`, and
`source-reader-shell`; it verified both changed files and returned one undo
receipt. A later exact test-form replacement also committed atomically with
read-back hashes. The transaction boundary was not ceremony: the worktree
already contained unrelated edits, so exact named-form scope prevented this
task from swallowing them.

Two refusals were valuable. A malformed replacement form was rejected before
source changed. A prepared `tree-node` replacement failed verification and was
rolled back byte-for-byte. The structured failure showed zero kondo errors and
four warnings, including pre-existing warnings elsewhere in the namespace.
That is much better than landing an unverified edit.

The remaining two points are product friction:

1. The prepared transaction treated the namespace's pre-existing kondo warning
   baseline as a fatal regression. It had no changed-warning delta, so a valid
   targeted edit could not use the preferred MCP write path. The first refusal
   summary said only `verification-failed`; the actionable command output was
   visible only after requesting the structured result on the retry. The tool
   protected the file, but it could not finish the job.
2. Top-level test insertion returned `unsupported-insertion-parent`. That forced
   a prose-style patch for a normal Clojure operation even though insertion is
   part of the advertised change language. Separately, a stale singular cclsp
   connector reported `invalid-mcp-session` while `clj-surgeon up` correctly
   reported both shared services healthy and no restart required. Later
   `prepare-change` calls worked, but the reconnect boundary was not one-shot.

The outcome validates the score. Clj-surgeon supplied the bounded reads, exact
reference surfaces, guarded multi-file changes, and safe rollback that made the
refactor trustworthy. Mothership's initial reader response fell from 18.97 MB
to 288 KB, a 98.5% reduction, and six concurrent real requests completed in
0.90–0.92 seconds. The tool did not single-handedly carry the implementation,
but it substantially reduced uncertainty and prevented collateral edits.

To earn 9/10 on the next field run, verification should compare new diagnostics
against the exact pre-change baseline, every refusal should expose its
actionable cause in the default summary, top-level sibling insertion should be
a supported transaction, and a healthy shared service should offer one
reliable session-reconnect path.
