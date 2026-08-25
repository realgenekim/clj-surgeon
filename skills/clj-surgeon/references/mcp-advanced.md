# Advanced MCP routes

Read only the section required by the current task. Ordinary already-decided
edits belong in compact `edit_clojure`, not here.

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

## Heavyweight transaction operations

Use `apply_clojure_changes` only for capabilities absent from compact editing:

- retained semantic-basis decisions;
- complete owner deletion or structured insertion;
- namespace extraction with explicit caller changes;
- `rename_binding` and comment-preserving `assoc_entry`; or
- formatter, linter, and test gates that must participate in rollback.

For a necessary direct heavyweight request, send one coherent `changes` array
with exact aggregate counts. A count mismatch, stale hash, overlap, parse error,
owner disagreement, or hot verification failure refuses or rolls back the
complete transaction. On cold pending verification, copy `next_call` once;
never replay or poll the original mutation.

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
