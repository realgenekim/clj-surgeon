# Row 2 receipt artifact repair

Gene's 2026-09-08 brief authorizes this slice through implementation, replay,
verification and branch commit. The six frozen D candidates failed solely because
detail documents entered their staged path sets. Layout from N arms is out of scope.

HLD → alias-migration design → ALIAS-MIGRATION-001/002 → boundary witnesses →
shared artifact placement and post-write Git evidence. The spec leaf is discovered
by the repository intent audit; the accompanying EDN registry records misreadings.

All automatic receipt, detail and inverse publication uses
`/var/tmp/forge/<verb>-receipts/`, partitioned by workspace identity where needed.
Paths returned to callers are absolute. Artifact configuration cannot redirect a
verb's bookkeeping into its source tree. Source-adjacent atomic replacement files
are transient source transaction machinery, removed before the receipt; existing
user configuration is read-only. Cold verification snapshots are external fixtures.

After all writes, Git porcelain with all untracked files supplies the cleanliness
proof. A successful check publishes the sorted changed-file exception set. A failed
Git check or unrelated dirty path publishes incomplete cleanliness evidence rather
than asserting a clean tree or pretending a committed write did not happen.

Matrix: var migration, library retirement, undo, details retention; split,
require-change, helper extraction and general edits; clean Git, unrelated untracked
file, tracked unrelated change, non-Git root, names containing whitespace/Unicode,
rename records, external path aliases, artifact directories inside the workspace.

Gates: fail-first warm witness; warm focused green; frozen P1-D request rebound only
to a fresh worktree of row2/seed-repo; all five stage-2 oracles PASS, MINOR 0;
serialized lint; make test; intent audit; branch commit as forge-anvil with Gene's
trailer. Evidence and limitations go to the requested report. No push.

## Sol r10 landing repair

Gene authorizes the r10 findings through branch commit. The complete alias
battery must execute on every `make test` and `make landing-gate` invocation,
including the latter's `~/bin/land` caller. Keep its battery classification:
it launches child processes. Add a named target selecting the whole namespace,
and a default-lane witness resolving that target's actual namespace membership.

Amend MCP-OP-ALIAS-019/020/026/044/045 in place: migration summaries exclude per-file
edit payloads; measured Git exception paths may grow with the file set. Details,
inverse and retired-source paths are absolute external paths. Retention and
symlink/rollback witnesses must operate on the directory they mean to attack.
Legacy configured-path defenses remain commit-boundary contracts; the public
entrance overrides receipt configuration, as ALIAS-MIGRATION-001 requires.

Retain behavioral red evidence on 3d55fa34 for each other affected publication
verb, with the same probes green on the repaired build. A writer census or a
passing unrelated suite does not substitute for that evidence. Final gates are
the whole alias namespace, make test, intent audit, serialized lint, and a dry
merge against freshly fetched origin/MCP/main descending from 1ab53e0e.

The r10 source-independent publication battery contains 13 witnesses: helper,
compact edit, general changes, prepared apply, legacy extract, typist, standalone
require change, namespace split, legacy change, advisory lock, cold job publication,
admit focused report, and default transaction journal/preimage. The same file
loads on 3d55fa34 without the new artifact helper. Fixtures execute real writers;
observational wrappers leave operation arguments and results unchanged. Split
uses the same frozen analysis on both revisions. Six existing source/proof/undo
fixtures contribute their assertions to the reported runtime test total.

The installed /home/forge/bin/land was found to hardcode the old gate list, contrary
to the Makefile's stated contract. Its local gate list is repaired to `make test`,
which reaches landing-gate and both affected batteries. Its diff is retained with
the report; it is not invoked, and no push or actual merge is authorized here.
