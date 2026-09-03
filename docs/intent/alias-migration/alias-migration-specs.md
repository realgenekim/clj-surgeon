---
parent: alias-migration-design
prefix: MCP-OP-ALIAS
---

# #Alias Migration Specifications

Stable intent registry for the `alias_migration` leaf. IDs are never reused.
The status marker records whether current code and tests witness the
requirement.

# #Request and Registration

- [x] **MCP-OP-ALIAS-001**: When the clj-surgeon MCP server starts in the full tool profile, it shall advertise `alias_migration` as a public top-level tool whose declared outcome classes are exactly committed and typed refusal.
- [x] **MCP-OP-ALIAS-002**: When an `alias_migration` request is received, clj-surgeon shall accept exactly the closed fields `op`, `workspace_root`, `from`, `to`, `scope`, and `expect`, and shall refuse any other field before reading source.
- [x] **MCP-OP-ALIAS-003**: The `alias_migration` request shall carry no per-file, per-owner, or per-site table, so that its payload size is constant in the number of affected namespaces.

# #Discovery

- [x] **MCP-OP-ALIAS-004**: When `alias_migration` executes, clj-surgeon shall itself discover every namespace under `scope.paths` whose `ns` form directly requires `from.lib`, without the caller naming any file.
- [x] **MCP-OP-ALIAS-005**: When a requiring namespace is discovered, clj-surgeon shall itself discover every call site of `from.var` under every spelling that file makes legal: each `:as` or `:as-alias` alias, the fully qualified lib name, and the bare name when the file refers it.
- [x] **MCP-OP-ALIAS-006**: If no namespace under `scope.paths` requires `from.lib`, then clj-surgeon shall refuse with `alias-migration-empty-scope`, state the count found, and change no bytes.

# #Per-file Alias Policy

- [x] **MCP-OP-ALIAS-007**: When clj-surgeon chooses a file's alias, it shall select the first `to.alias_policy` entry that collides with nothing bound in that file's `ns` form, where the collision set is exactly the aliases introduced by `:as` and `:as-alias` together with the names introduced by `:refer`; a local binding, a function parameter, a destructured name, and a top-level definition name shall not be collisions, because the namespace part of a qualified symbol resolves through the namespace's alias map and cannot be shadowed lexically.
- [x] **MCP-OP-ALIAS-008**: If every `to.alias_policy` entry collides with an alias or referred name in one file's `ns` form, then clj-surgeon shall refuse with `alias-migration-alias-policy-exhausted`, name that file and the colliding names, and change no bytes.

# #Rewrite Closure

- [x] **MCP-OP-ALIAS-009**: When every use of `from.lib` in a file is a migrated site, clj-surgeon shall replace that file's `from.lib` libspec with `[to.lib :as <alias>]`; otherwise it shall add the new libspec alongside and leave the old require in place.
- [x] **MCP-OP-ALIAS-010**: When clj-surgeon rewrites a site, it shall write exactly `<alias>/<to.var>` and shall leave every other byte of the containing form unchanged, including comments, commas, indentation, and metadata.
- [x] **MCP-OP-ALIAS-011**: When clj-surgeon rewrites a file, it shall leave untouched a local binding of the same name, a string literal, a docstring, a comment, an `#_` discard, and every reader-conditional branch other than the file's own platform branch and `:default`.

# #Typed Refusals

- [x] **MCP-OP-ALIAS-012**: If the number of discovered requiring namespaces differs from `expect.files`, then clj-surgeon shall refuse with `alias-migration-expect-mismatch`, state the found and expected counts, and change no bytes.
- [x] **MCP-OP-ALIAS-013**: If `from.lib` or `from.var` is reachable only through a construct the tool cannot mechanically close — a prefix-list libspec, a `:use` clause, a runtime `require` or `alias`, a quoted or syntax-quoted occurrence, or a site in a non-selected reader-conditional branch of a `.cljc` file — then clj-surgeon shall refuse with `alias-migration-indirect-reference`, name the file and the form, and change no bytes.
- [x] **MCP-OP-ALIAS-014**: If a bare occurrence of `from.var` could resolve to two required namespaces, then clj-surgeon shall refuse with `alias-migration-ambiguous-ownership`, name both candidate vars, and change no bytes.
- [x] **MCP-OP-ALIAS-015**: When `alias_migration` refuses, the refusal shall carry an executable `next_call` that is a complete `alias_migration` request the caller may send verbatim.

# #Atomicity and Kernel Routing

- [x] **MCP-OP-ALIAS-016**: When `alias_migration` writes, it shall route the write through the same transaction kernel entrance the other public write tools use, as one failure-atomic transaction over every affected file.
- [x] **MCP-OP-ALIAS-017**: If any single file's compiled change fails at commit time, then no file in the transaction shall be modified on disk.
- [x] **MCP-OP-ALIAS-018**: When clj-surgeon compiles an `alias_migration` change, its `find` shall be the exact original bytes of one complete top-level form, so that source drift between discovery and commit refuses the whole transaction before any write.

# #Receipt

- [x] **MCP-OP-ALIAS-019**: When `alias_migration` commits, it shall return exactly one receipt whose length is constant in the number of affected namespaces, carrying the file count, the site count, the alias histogram, the collisions-resolved count, a kondo delta summary, and a focused-test result summary.
- [x] **MCP-OP-ALIAS-020**: The `alias_migration` receipt shall never contain a per-file list; per-file detail shall be written to a `details_path` inside the workspace's `.clj-surgeon` directory.

# #Lib-only Migration

- [x] **MCP-OP-ALIAS-021**: When `from.var` and `to.var` are both null, clj-surgeon shall rewrite every qualified use of every var of `from.lib` — through any alias, any fully qualified spelling, and, under the `alias-qualify` refer policy, any referred name — to the file's chosen alias qualifying the same var name; under the default `preserve-refer` policy it shall instead keep those names in a `:refer` vector against `to.lib`.
- [x] **MCP-OP-ALIAS-022**: When a lib-only migration commits, clj-surgeon shall also rename the namespace that defines `from.lib` to `to.lib`, create it at the path `to.lib` requires in the same transaction as every caller rewrite, and retire the superseded file reversibly; if any later step fails, it shall restore that file and roll the transaction back.
- [x] **MCP-OP-ALIAS-023**: If `to.lib` is already defined in scope while `from.lib` is still defined, or the path `to.lib` requires is already occupied, then clj-surgeon shall refuse with `alias-migration-target-lib-exists`, name both files, and change no bytes.
- [x] **MCP-OP-ALIAS-024**: If exactly one of `from.var` and `to.var` is null, then clj-surgeon shall refuse with `alias-migration-mixed-var-spec` and change no bytes.
- [x] **MCP-OP-ALIAS-025**: When clj-surgeon migrates a lib, it shall match namespaces and qualifiers by whole symbol identity, so that a prefix-sharing sibling namespace, a prefix-sharing namespace name, and a prefix-sharing qualified use are all left byte-identical.
- [x] **MCP-OP-ALIAS-026**: When a lib-only migration commits, its constant-size receipt shall carry `refer_sites` and a `lib_renamed` record naming the old lib, the new lib, the defining file, its new path, and where the superseded file was retired.

# #Server Adapter and Verification Policy

- [x] **MCP-OP-ALIAS-027**: When `alias_migration` is invoked through the MCP server for a routed `workspace_root`, clj-surgeon shall derive that workspace's receipt directory and resolve its lazy verification-profile accessors exactly as the direct dispatch does, so a request addressed to a workspace other than the server's own project directory neither fails nor silently adopts the server's configuration.
- [x] **MCP-OP-ALIAS-028**: Verification shall be opt-in: `alias_migration` shall run a transaction profile only when the request names one in `verify`, shall report `not-requested` otherwise, and shall refuse before writing when the named profile is not configured for that workspace.


# #Every Position a Qualified Symbol Can Occupy

- [x] **MCP-OP-ALIAS-029**: When clj-surgeon discovers sites, it shall treat a Var reference written as `#'alias/x` or `(var alias/x)` as a site, including names carrying earmuffs such as `alias/*clock*`.
- [x] **MCP-OP-ALIAS-030**: When a binding vector belongs to a form that rebinds Vars — `binding`, `with-redefs`, `with-bindings` — clj-surgeon shall treat every left-hand side as a reference and a migration site, and shall introduce no local from it; a `let`-family binding vector's left-hand sides shall remain binding forms that are never sites.
- [x] **MCP-OP-ALIAS-031**: When a qualified reference appears inside a syntax quote, clj-surgeon shall migrate it, because the reader resolves the alias; when one appears inside a plain quote, clj-surgeon shall refuse with `alias-migration-indirect-reference` and reason `quoted-reference`.
- [x] **MCP-OP-ALIAS-032**: If an auto-resolved keyword `::alias/k` reaches the migrating alias, then clj-surgeon shall refuse with `alias-migration-indirect-reference` and reason `auto-resolved-keyword`, because rewriting changes the keyword's value while leaving it breaks the read; a single-colon `:alias/k` shall never be treated as a site.
- [x] **MCP-OP-ALIAS-033**: When a qualified reference appears inside a metadata map, clj-surgeon shall migrate it, because metadata values are evaluated code; a string inside that map shall remain a string.
- [x] **MCP-OP-ALIAS-034**: When a lib-only migration completes, its receipt shall carry the count of files naming the old lib as a string literal, which the verb does not rewrite because such strings are assertions about the codebase or data rather than code references.
- [x] **MCP-OP-ALIAS-035**: When a lib-only migration runs, clj-surgeon shall treat a quoted fully-qualified symbol `'from.lib/x` as a site and rewrite it to `'to.lib/x`, including in a file that never requires the lib, because retiring the namespace otherwise breaks that reference lazily at call time with no compile error; a quoted alias-qualified symbol shall remain a typed refusal.

# #Repository Hygiene

- [x] **MCP-OP-ALIAS-036**: The clj-surgeon repository shall track no path under `.cpcache/` and shall carry a gitignore rule covering that directory, because a classpath cache records absolute paths belonging to the machine that produced it and is never portable.

# #Bounded, Confined Discovery

- [x] **MCP-OP-ALIAS-037**: When `alias_migration` expands `scope.paths`, the walk shall not follow a symlinked directory and shall be bounded in depth, so that a link cycle inside the project root terminates and a directory link pointing out of the root is never entered, before any confinement check runs.
- [x] **MCP-OP-ALIAS-038**: If `scope.paths` selects more files than the per-call file ceiling, then clj-surgeon shall refuse with `alias-migration-scope-too-large` before reading any source, naming the count found, the ceiling, and the request's `expect.files`.
- [x] **MCP-OP-ALIAS-039**: If any file in scope is larger than the per-file byte ceiling, then clj-surgeon shall refuse with `alias-migration-source-too-large` before that file is read, naming the file and the ceiling.
- [x] **MCP-OP-ALIAS-040**: When `expect.files` exceeds the number of files `scope.paths` selects, clj-surgeon shall still run discovery rather than refuse early, so the `alias-migration-expect-mismatch` refusal carries `found_files` and an executable `next_call` whose `expect.files` equals that found count; an over-declared expectation is the verb's self-correcting field idiom and shall cost one return, not a second request the caller must compose.
- [x] **MCP-OP-ALIAS-041**: When a lib-only migration retires the superseded defining file, clj-surgeon shall move the same canonical path the transaction's edits addressed; if that path is a symbolic link, it shall refuse with `alias-migration-retire-symlink-refused` before writing anything, and a restore shall put the file back at that same canonical path.
- [x] **MCP-OP-ALIAS-042**: The `alias_migration` receipt's `ok` and `committed` shall be the transaction kernel's own computed `committed`, its visible check-mark summary shall be rendered from those same values, and a kernel result that did not commit shall be published as a typed refusal rather than a receipt.
- [x] **MCP-OP-ALIAS-043**: If retiring the superseded defining file fails and the transaction is rolled back, then clj-surgeon shall delete that transaction's undo receipt, exactly as the verification-failure rollback does, so that no receipt survives describing an inverse that has already been applied.
- [x] **MCP-OP-ALIAS-044**: The `lib_renamed.retired_to` the `alias_migration` receipt publishes shall be a project-relative path, so the receipt names something the caller can act on and discloses no absolute path from the server's filesystem.
- [x] **MCP-OP-ALIAS-045**: When `alias_migration` writes its per-run detail document, clj-surgeon shall retain only the twenty most recent `.edn` documents in `.clj-surgeon/alias-migration/`, always including the one that run's receipt names, and shall leave the transactional `retired/` subtree untouched.
- [x] **MCP-OP-ALIAS-046**: If the total size of the files `scope.paths` selects exceeds the aggregate scope-byte ceiling, then clj-surgeon shall refuse with `alias-migration-scope-too-large-bytes` before reading any source, naming the total bytes, the file count, and the ceiling, because the per-call file ceiling and the per-file byte ceiling do not bound their product and a scope legal under both can still exhaust the heap.
- [x] **MCP-OP-ALIAS-047**: If an `alias_migration` call exhausts the server's heap, then clj-surgeon shall publish one typed `alias-migration-resource-exhausted` refusal at every entrance rather than an untyped throw, and its `source_unchanged` shall report whether the transaction kernel was entered rather than a hopeful literal.
- [x] **MCP-OP-ALIAS-048**: If any entry under `scope.paths` lies more path segments below the project root than the scope depth bound, then clj-surgeon shall refuse with `alias-migration-scope-too-deep`, naming that path, its depth, and the bound, and shall never truncate the walk instead, because a file dropped for depth leaves the found count and the verb's over-declare idiom would launder that omission into a commit.
- [x] **MCP-OP-ALIAS-049**: If the scope walk cannot read an entry that is not inside a pruned build or version-control directory, then clj-surgeon shall refuse with `alias-migration-scope-unreadable`, naming an exact count and a bounded list of those paths, and shall never continue past them, because a scope that silently shrank hands the caller a found count that omits what it could not see.
