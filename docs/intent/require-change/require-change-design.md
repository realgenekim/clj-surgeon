# Standalone whole-intent require change

The [HLD](../../high-level-design.md) assigns this operation one mechanical
decision over an explicit file set. `require_change` (MCP) and
`:require-change!` (CLI) share a pure token-splice compiler and a confined I/O
boundary. No symbol edit is accepted, required, inferred, or synthesized.

The closed request is `{:workspace_root STRING :add {:lib STRING
:alias_policy [STRING ...]} :files [{:file STRING :source_hash STRING?
:remove {:lib STRING :as STRING}?} ...] :expect {:files N :adds N :removes N}
:verification {:profile STRING} :plan_only BOOLEAN?}`. Optional `:op` must be
`"require_change"`. Optional `:layout` must be exactly
`{:order "stable-lib-symbol" :comments "attach-following" :blank_lines 0}`.
Names are plain unqualified symbols expressed as strings. Files are unique,
nonempty, relative `.clj` paths, at most 128; each is at most 2 MiB, total 16 MiB.

For each file, reuse an existing preferred alias bound to the target; otherwise
choose the first policy alias not bound to another lib. Duplicate target entries
or ambiguous alias bindings refuse. An existing target under a non-policy alias
refuses rather than adding a duplicate. Exhaustion names the file and every
occupied policy alias/lib. Removals are explicit exact lib/alias pairs and never
inferred from use; target removal is forbidden. Alias allocation observes the
original bindings, including any entry requested for removal.

Only one direct ns and one nonempty direct require clause are admitted. Entries
are direct `[lib]` or `[lib :as alias]`; unsupported reader conditionals, prefix
lists, refer and other options refuse. Comments outside entries are immutable.
Insert before the first inherited library sorting after the target, before its
attached contiguous preceding comment run. Preserve inherited order, indentation,
line endings, closing parentheses and all existing separators. A hanging first
entry cannot be displaced if doing so requires changing its line: refuse that
unrepresentable zero-churn layout. Appending after a final libspec that shares
closing parentheses similarly refuses if a whole-line insertion cannot preserve
their placement. Removal owns a complete standalone libspec line only; shared
closers, inline comments or attached comments on that removal refuse. These
boundaries expose a frozen-design conflict: `codex_app_server.clj` requires a
sorted append but shares its final entry's line with the clause closer. Exact
O4 whole-line preservation and O5 sorted insertion cannot both hold there.
The golden `capture_archive.clj` and `reducer_session.clj` targets share their opener lines, also making
literal whole-line seed stripping invalid. Row 3 remains unadmitted until its
design is explicitly revised; neither exception is silently normalized away.

The compiler returns changed sources, exact counts, ordered per-file alias
decisions with collision evidence and before/after hashes. Counts
mean changed files and actual added/removed entries; already satisfied inputs
require zero adds and zero touched files. Exact mismatches refuse before write.
Recomputing the authorized line splice validates every supplied candidate before commit.
Plan-only uses the same compiler and leaves source unchanged.

The boundary captures every explicit file once, checks optional frozen hashes,
confines paths and rejects symlinks. It passes one guarded candidate to the
existing failure-atomic extraction kernel, persists its integrity-checked undo,
runs the workspace's named synchronous verification profile, then guards the
candidate snapshot again. Failed checks invoke guarded undo; foreign bytes
produce recovery-required, never forced restoration. As with namespace split,
this is failure atomicity, not isolation from concurrent filesystem readers or
power-loss durability. Proof completeness is only the declared commands.

The bounded receipt reports state, touched files, counts vs expectations,
per-file chosen alias/reason/collisions, byte hashes, named checks/exits/durations,
verification completeness and durable detail/undo paths. Long per-file evidence
is retained in the details receipt rather than silently dropped. A committed
receipt must never be replayed blindly. CLI undo reuses `:undo-extract!`.

Tests link the [REQUIRE-CHANGE series](require-change-specs.md). The golden
header fixture comes from Marvin `9f9cf614`. Six unit seed headers delete the
target line; the two opener cases use exact parent `d170f3d5` headers, as documented
in the fixture README. This unit fixture does not amend the frozen full seed.
The independent papercut-style oracle also checks complete
files and all other paths against the isolated seed/golden. Its negative mutants
must reject lost/moved comments and unrelated bytes. O1 load and both suites and
O3 normalized golden lint remain necessary before any admission verdict.
