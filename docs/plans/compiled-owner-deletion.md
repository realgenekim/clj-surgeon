# Compiled exact-owner deletion

**Status:** Implemented and verified
**Motivating issues/incidents:** `clj-surgeon-4uc`, `clj-surgeon-7xr`,
`clj-surgeon-ox7`; a field refactor had 17 proven obsolete owners but had to
replace them with harmless markers and remove the markers with a native patch.

## Outcome

An agent that already knows several exact named owners can delete them in one
MCP transaction. The agent supplies the architectural decision once. Surgeon
owns source capture, structural addresses, comment and separator boundaries,
combined future-state parsing, write ordering, rollback, verification, and the
inverse receipt. Exact-owner deletion never waits for the semantic provider.

The accepted public direction is one extensible transaction program:

```edn
{:workspace_root "/absolute/project"
 :intent "Remove the obsolete portal implementation"
 :program
 [{:id "portal-delete"
   :op "delete-owners"
   :owners [{:file "src/app/server.clj" :form "portal-drafts"}
            {:file "src/app/server.clj" :form "handle-portal"}]
   :expect {:owners 2}}]
 :expect {:instructions 1 :owners 2 :files 1}
 :verify "fast"}
```

The first implementation may expose the same semantics through the existing
`changes` representation, but the durable model is an ordered `program` of
explicit mechanical instructions. No third MCP tool is introduced.

## Bitter-Lesson Boundary

The model decides that the owners are obsolete. Surgeon does not infer dead
code, choose an architecture, or silently add callers or dependencies. It
only compiles an explicit ordered owner set into lossless deletions and proves
the declared counts. Resolved-reference proof remains a separate, explicit
instruction when the decision requires it.

Non-goals:

- no fuzzy owner matching;
- no inferred deletion from an empty replacement or marker name;
- no automatic caller deletion;
- no semantic-provider wait for exact `{file, form}` owners;
- no best-effort partial commit.

## Public Contract

`delete-owners` accepts an ordered, non-empty, unique vector of closed
`{file, form}` objects. Each file is project-relative and confined. Each form
must resolve to exactly one complete named top-level owner in the captured
snapshot.

Success returns a compact summary, `verification_complete=true`, exact changed
file and owner counts, result hashes, and one durable inverse receipt. It does
not print deleted source bodies.

Refusal returns one stable error, the instruction ID, owner index and owner
identity when applicable, `source_unchanged=true`, and one executable remedy.
Important refusals include missing, duplicate, or ambiguous owners; path
escape; stale source; comment/trivia boundaries that cannot be preserved;
future parse failure; count mismatch; and verification failure. Verification
failure rolls back every file.

## Safety Invariants

- Read each distinct canonical file exactly once during compilation.
- Address every owner in that one snapshot and bind application to its hash.
- Preserve unrelated bytes, attached comments, reader conditionals, namespace
  metadata, and neighboring separators exactly.
- Compile all deletions and parse every future file before the first write.
- Commit all files or none; verify read-back hashes after writing.
- Retain no basis after any preparation refusal.
- Never use semantic-provider availability as an admission requirement.
- Produce an exact inverse receipt whose apply is also hash-fenced.

## Implementation Shape

Build on `mcp-change-buffer`'s exact-owner capture and existing whole-site
deletion kernel. The multi-owner preparation path produces one basis, not one
public basis per owner, and caches source by canonical path. The transaction
compiler lowers `delete-owners` to the same concrete edit representation used
by current basis decisions. `apply_clojure_changes` remains the only write
tool.

Keep the compiler data-driven. Operation dispatch selects a small pure
compiler function; shared transaction code owns parsing, hashes, atomic writes,
verification, and receipts. Do not duplicate those mechanics in the MCP
adapter.

The live server publishes the changed schema with
`notifications/tools/list_changed`. New sessions receive it automatically.
Existing Codex sessions can retain model-visible schema text for the current
turn; document that client boundary honestly.

## Test Plan

1. Pure contract matrix for empty, malformed, duplicate, ambiguous, escaped,
   stale, and valid owner vectors, including mixed files and repeated files.
2. Field regression: 17 named owners in one large, real-program-derived
   namespace delete in one call with no markers and no semantic resolver call.
3. Comment matrix: leading attached comments, trailing comments, comment-only
   gaps, adjacent owners, reader conditionals, and final owner in a file.
4. Failure atomicity across two files, including parse and verification
   failures after compilation.
5. Read-count assertion proving one read per distinct file.
6. Receipt round-trip restoring byte-identical originals.
7. MCP adapter test proving the published schema, compact success summary, and
   structured refusal.
8. Clean-context agent comparison: given the 17-owner goal, the caller must
   choose one deletion transaction without custom coaching or marker cleanup.

No existing assertion may be removed or weakened.

## Documentation and Release Checklist

- Update MCP tool descriptions, `README.md`, and both installed agent skills.
- Present exact-owner deletion before marker replacement or native cleanup.
- Add one concise valid example and one refusal/remedy example.
- Run `make mcp-reload`; verify live `tools/list` and one real request.
- Run `make install` after CLI/skill changes.
- Record the before/after field route and clean-caller result in a Captain's
  Log.

## Verification Gates

- Standard Clojure Style on changed Clojure files.
- Focused contract, change-buffer, adapter, comment, atomicity, and receipt
  tests.
- `make mcp-test`, clj-kondo with zero errors/warnings, and the full suite.
- Live hot-server call against a dirty worktree fixture.
- Clean-context caller uses one transaction and produces zero marker forms and
  zero native source patches.

## Definition of Done

Given 17 exact obsolete owners across one or more files, a clean agent submits
one MCP call, Surgeon deletes all and only those owners without cclsp, verifies
the combined result, and returns one inverse receipt. Any ambiguity or drift
changes zero bytes and identifies one correctable owner. The previous
replace-with-markers plus native-cleanup route is unnecessary and no longer
recommended.

## Completion Evidence

- A live dirty-worktree fixture deleted two adjacent owners atomically; its
  inverse receipt restored the original bytes exactly.
- A clean Codex session received one formatted file and 17 exact owner names.
  It made one `apply_clojure_changes` call, used no structural read, semantic
  query, marker, or native patch, and succeeded on its first attempt.
- The clean transaction completed 17 edits in one file with `verify=fast`,
  read-back verification, and a durable inverse receipt in 1.216 seconds of
  direct tool time.
- The final MCP suite passed 136 tests and 1,102 assertions; the full repository
  suite passed 634 tests and 5,412 assertions. clj-kondo reported zero errors
  and zero warnings across the focused files.
