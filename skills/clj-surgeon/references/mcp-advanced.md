# Advanced MCP routes

Read only the section required by the current task. Ordinary already-decided
edits belong in compact `edit_clojure`, not here.

## Gate an existing native patch (development MCP)

When this gate is the chosen development route, pass the exact native patch to
`admit_clojure_patch` with `mode=commit, verify=focused`. `verify=none` is for
an unverified preview; a commit with it refuses before writing. Do not use
`allow_partial` to work around missing proof. A separate preview is optional;
when you use one, copy its `expect_pre_sha256` into the subsequent commit.

Focused verification needs a real runner and coverage mapping. The repository
may declare `.clj-surgeon/focused-test.edn`; `:command`, `:timeout-ms` and
`:namespaces` override server configuration per key. Command argv must contain
literal `{snapshot}` and `{report}` arguments; `{namespaces}` expands into suite
names. The runner must test the candidate and write attributable results to that
report. The snapshot contains changed files, not a complete checkout: resolve
candidate files before unchanged dependencies and suites. The process starts
in the live workspace, so testing the current directory alone is insufficient.

An external passing command is not automatically an admission profile. If the
receipt says verification is incomplete, use its typed reason to repair the
actual profile or use native editing with the project's proof; do not claim the
gate verified or committed the change. Count profile setup and refusals in the
complete task time. There is no measured general speedup for this route.

## Semantic preparation

When exact sites are unknown, use `inspect_clojure` with
`mode=prepare-change`, one concise `intent`, and either a qualified `subject` or
an ordered `subjects` array. Its authority-labeled surface joins semantic
references with exact quoted-Var callers. Copy the returned `basis` and
`next_call`; fill each decision with exactly one keep, replacement, or owner
delete. Call `apply_clojure_changes` once. Do not repeat resolution, source
reads, selectors, hashes, counts, or site IDs.

For an unindexed known owner, prepare with project-relative `file` plus exact
top-level `form`. This proves exact source but does not claim caller
completeness. Preserve the caller's canonical `workspace_root`; never guess a
sibling repository path.

## Standalone computed preview

Use `transform_clojure` when one bounded pure SCI relation derives replacements
for known leaves and a preview is useful. Supply one path ending in `transform`,
exact `expect.matches`, and `expect.max_changed_characters`. Preview is the
default. Set `commit=true` only after the relation is decided. Narrow around
comments; use semantic preparation or a tested refactor operation when caller
completeness or namespace mechanics matter.

## Relation census

Use `relation_census` when the question is "which collection writes inside this
projection's fold arms are unguarded". It classifies every write inside a
`defmethod fold-event` arm as `:door` (routed through a known identity door),
`:set`, `:guarded` (a recognised guard on the written value's identity
dominates it), `:raw` (nothing recognised guards it), or `:unknown` with a
reason, and it publishes the calls inside arms it does not model — a write
behind one of those is not a site, so `raw 0` alone is not a clean bill of
health.

Omit `files` to census every file in the workspace that defines arms; pass
`files` for an exact list, `doors` to extend the default identity doors, and
`pool_size` for plan-phase parallelism (the answer never depends on it; the
effective pool never exceeds the box). The CLI equivalent is
`clj-surgeon :op :relation-census :dir . [:doors a,b] [:threads N]`.

It reads only, and it is the one tool that enumerates the workspace tree: point
it at the workspace you mean. It locates review work; it does not prove
idempotency and it is not an enforcement gate.

## Heavyweight transaction operations

Use `apply_clojure_changes` only for capabilities absent from compact editing:

- retained semantic-basis decisions;
- structured insertion or an owner operation absent from compact editing;
- namespace extraction with exact roots and any known caller decisions;
- `rename_binding` and comment-preserving `assoc_entry`; or
- formatter, linter, and test gates that must participate in rollback.

For a necessary direct heavyweight request, send one coherent `changes` array
with exact aggregate counts. A count mismatch, stale hash, overlap, parse error,
owner disagreement, or hot verification failure refuses or rolls back the
complete transaction. On cold pending verification, copy `next_call` once;
never replay or poll the original mutation.

For extraction, call `apply_clojure_changes` directly once the exact source,
destination, ordered owners, and require policy are known. Omit `public_forms`
to derive only mechanically required visibility from the same frozen snapshot.
Omitted caller arrays never prove that callers are absent: if the complete
workspace contains any candidate not explicitly changed or ignored, the call
refuses before writing and returns a completed snapshot-bound plan plus exact
caller-disposition unknowns. Fill that returned call and submit once; do not
restart with `plan-extraction` or source discovery.

## Hot process recovery

If tools are absent or point at the wrong repository, run
`clj-surgeon up [WORKSPACE]` once to join the shared process. A typed
`semantic-provider-warming` result is not a restart condition: preserve its
session, anchor, and `next_call`, wait, and retry once. For an invalid session or
false-green health, run `clj-surgeon recover [WORKSPACE]` once and follow its
terminal receipt. Never loop.

After a server schema change, run `make mcp-reload` in the source checkout.
Start a fresh agent session only when its cached tool schema blocks the new
call.
