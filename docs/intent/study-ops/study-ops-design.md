---
prefix: MCP-OP-STUDY
issue: clj-surgeon-0me
status: implemented; awaiting ratification
---

# Study operations at the MCP read entrance

## The gap

`:ls-tree`, `:ls-deps`, `:deps`, `:topo`, and `:ls-extract` existed only in
`clj-surgeon.core`, the process-starting CLI. `docs/vision.md` names
"questions grep answers wrong" as one of the four squares we still compete on
and `:ls-tree` as its foundation, and the measured caller of Surgeon is the
MCP, not the CLI. A capability that the hot entrance cannot reach is a
capability agents do not have: the CLI as an MCP substitute is already a
closed, measured loser (a second layer, refuses 2.2x).

## The decision: `inspect_clojure`, not a new tool

`docs/plans/one-compiler-two-entrances.md` is binding here. Its Public
Contract says "Keep exactly `inspect_clojure` and `apply_clojure_changes` in
the Surgeon MCP", Safety Invariant 10 says "The number of public Surgeon MCP
tools remains two", and its Definition of Done requires that "no third Surgeon
MCP tool or parallel mutation runtime was introduced". A `study_clojure` tool
would be a new public tool for read-only work that the read tool already owns.
So the study operations enter through `inspect_clojure`.

`inspect_clojure` already has two input shapes, and the split between them is
not stylistic:

- **`requests`** — an ordered batch of *file-scoped* reads. Every item is keyed
  by one project-relative `file`; the batch declares `expect.files` as the
  number of distinct request files; `snapshot_guards` must cover every
  requested file; capture reads each canonical file once.
- **top-level `mode`** — a whole-project read that has no single subject file
  (`prepare-change`, `plan-extraction`).

Four of the five study operations are file-scoped (`deps`, `topo`, `ls-deps`,
`ls-extract` all take one source file) and become `requests` operations with no
change to any existing rule. `ls-tree` is *directory*-scoped: it has no `file`,
so making it a request item would require redefining `expect.files` (today a
positive integer counting distinct request file paths) and the
`snapshot_guards` completeness rule — a change to an existing verb's contract.
It therefore takes the shape the contract already reserves for a whole-project
read: a top-level `mode`, exactly like `plan-extraction`.

## One kernel, two entrances

Each study operation is a pure function in the new `clj-surgeon.study`
namespace, taking data and returning data:

```text
clj-surgeon.study/{deps,topo,ls-deps,ls-extract}  ; source string in, data out
clj-surgeon.study/ls-tree                         ; directory in, data out
```

- The CLI (`clj-surgeon.core/run-*`) is **kernel plus print**.
- The MCP (`inspect_clojure`) is **kernel plus receipt**.

Neither entrance holds a second implementation, so the two cannot drift. The
`…-both-entrances-call-one-kernel` witness asserts that identity directly on
the same bytes.

The `:ls-tree` discovery and formatting pipeline moved verbatim from
`core.clj` into `study.clj`; a byte-for-byte CLI golden over ten real
invocations proves the move changed no CLI output.

## Deliberately not exposed

`:mv`, `:rename-ns!`, and `:fix-declares!` are **write** operations and stay
CLI- and gate-only. They are absent from the MCP read entrance by design, not
by oversight. `inspect_clojure` is annotated `read-only true` and must remain
so; a write reachable from the read tool would make that annotation a lie.

## Receipts are the product

Every study receipt leads with state and is bounded:

- payload limited to 4096 JSON/text characters by default, `limit` up to 16384;
- `returned`, `omitted`, `truncated`, and `read_complete=false` when truncated,
  because a bounded receipt is not terminal evidence;
- an executable `next_call` only while raising `limit` can still advance. At
  the maximum limit the receipt says `narrow_scope` and serves no call: the
  narrower `dir`, `grep`, or `form` is a caller judgment, not a deterministic
  projection of proved facts, and serving back the call just made is a loop.
  Hand-driving the wire is what found this; the first implementation returned
  its own arguments;
- `elapsed_ms` from the shared operation clock;
- every refusal typed, with `next_call` and what would lift it.

`ls-tree` truncates at whole-file granularity and renders the kept files
through the same formatter the CLI uses, so a truncated tree is still a valid
tree, never a cut string.

## Confinement

`dir` is project-relative and resolved by `clj-surgeon.mcp-paths/resolve-directory-path`,
which mirrors `resolve-source-path` exactly: lexical rejection of absolute
paths, drive letters, NUL, and `..`; then real-path resolution that must still
start with the canonical root; then a directory check. It adds no new
confinement policy and relaxes nothing.

`ls-tree` is the first read path that runs subprocesses (`find`, `rg`) on
behalf of an MCP caller. `find-build-files` previously built a `sh -c` command
string with the directory interpolated into it; that is now an argv vector, so
no caller-influenced path reaches a shell. `grep` was already passed as an argv
element.

## Adversarial notes

- **"Just add a `study_clojure` tool."** Rejected: the architecture of record
  forbids a third public tool, and every study operation is a bounded read,
  which is precisely what `inspect_clojure` is for.
- **"Put `ls-tree` in `requests` with a `dir` field."** Rejected: it would
  force `expect.files` to become non-negative and would make the
  snapshot-guard completeness rule conditional — a contract change to an
  existing verb, paid by every existing caller, to avoid one new `oneOf`
  branch.
- **"Return the whole tree; agents can handle it."** Rejected with a
  measurement: `:ls-tree :dir .` over this repository is 221,018 characters.
  An unbounded receipt is a context bomb.
- **"`read_complete=true` because the batch resolved."** Rejected: a truncated
  receipt that claims terminal evidence is a false green, and a false green
  terminates investigation.
