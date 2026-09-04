---
parent: high-level-design
prefix: MCP-OP-SHELL-ARGV
status: "red-first implementation; Andon pull inb-d27b79, 2026-09-03"
---

# External Command Invocation Safety

## Context

clj-surgeon shells out during project discovery: `find` to locate build files
and Clojure sources, `rg`/`grep` to narrow a tree. Every one of those calls
carries caller data — the `:dir` of `:ls-tree`, the `workspace_root` of an MCP
request, a `:grep` pattern.

On 2026-09-03 a reviewer pulled the Andon cord on `clj-surgeon.core/find-build-files`.
It built its command with `format` and ran it through `sh -c`:

```clojure
cmd (format "find %s \\( %s \\) -prune -o \\( ... \\) -print" (str dir) prune-expr)
(babashka.process/shell {...} "sh" "-c" cmd)
```

`(find-build-files "/tmp/andon-fx/H; touch /tmp/andon-fx/PWNED ; echo z")`
created the marker file. So did the same string driven end to end through
`run-ls-tree`. The blast radius is every entrance that reaches project
discovery, so the promise registered here is not "escape the directory name"
— escaping is a filter that a future maintainer can weaken by accident. The
promise is **structural**: there is no shell.

## Component boundary

```text
caller value (:dir / workspace_root / :grep)
        |
        v
  entrance validation  ──> typed refusal (:workspace-root-not-a-directory)
        |
        v
  argument VECTOR  ──> babashka.process/shell, no shell interpreter
        |
        v
  NUL-delimited output ──> split on \0 ──> intact paths
```

Three separate guarantees, three separate requirements, three separate
witnesses. They are deliberately not folded together: an entrance check alone
would still leave a shell one refactor away, and an argv call alone would still
silently drop a path containing a newline.

## One authority, one answer: the gate and the executor

`existing-directory?` uses `Files.isDirectory`, which FOLLOWS symlinks, so a
symlinked workspace root passes the entrance gate. `find` under its `-P`
default does not descend a symlinked START POINT, so discovery returned zero
projects for a root the gate had just accepted — a composite state with two
disagreeing authorities, surfacing to the caller as "No Clojure files found"
for a tree that has files. Both `find` argvs now pass `-H`, which follows a
symlink given on the command line and only there: links found inside the tree
are still not followed, so the walk stays acyclic.

The same empty branch also carried a var shadow: `run-ls-tree` destructured its
`:format` argument into a local named `format`, shadowing `clojure.core/format`
for the whole body, so the "No Clojure files found" call invoked the caller's
`:format` VALUE as a function (`ArityException: Wrong number of args (3) passed
to: :edn`, or a nil-message NPE with no `:format`). The value is bound as
`output-format` now. The `(System/exit 1)` in that branch is a separate,
already-filed defect (inb-eca3b1) and is deliberately unchanged.

## Why NUL and not newline

`find`'s default `-print` terminates each path with a newline, and a POSIX
path may contain a newline. `str/split-lines` therefore turns one real path
into two fictional ones. The defect was invisible because the fictional paths
simply failed to parse and were dropped — a silent, correct-looking result.
`-print0` plus a NUL split removes the ambiguity at the source rather than
detecting it downstream.

## Why the intent audit is not the ratchet

`mcp-intent-contract/audit-contract` is a marker-PRESENCE audit: it checks that
an `@spec <ID>` annotation exists in some implementation source and some test
source. The 2026-09-03 adversarial review reintroduced a `format` + `sh -c`
discovery site in `src/` with every marker intact and the audit stayed
`OK= true, violations= []`. Marker presence cannot see a new shell. The
structural promise therefore carries its own source-level witness,
`no-source-file-hands-a-command-string-to-a-shell`, which parses every
`src/**.clj{,c,s}` with rewrite-clj and fails on an `"sh" "-c"`-shaped argv, a
`/bin/sh`-class program literal, or a process-spawning call whose program is
built by `format`/`str` at the call site. There is no allowlist; if a shell
ever becomes legitimate, the allowlist is added deliberately and reviewed.

## Falsifier table

| Intent | Falsifier — what would prove the promise broken | Required result | Witness |
|---|---|---|---|
| MCP-OP-SHELL-ARGV-001 | A `:dir` of `<real dir>; touch CANARY ; echo z` or `<real dir>$(touch CANARY)` causes `CANARY` to exist after discovery runs. **Or**: a NEW source file under `src/` hands a command string to a shell — an `"sh" "-c"` argv, or a process-spawning call whose program is built by `format`/`str` — and nothing fails. | `CANARY` never exists; discovery returns no build files. No source under `src/` invokes a shell interpreter. | `hostile-dir-never-reaches-a-shell-from-find-build-files`, `no-source-file-hands-a-command-string-to-a-shell` (with `the-source-scan-is-not-vacuous` as its own positive control) |
| MCP-OP-SHELL-ARGV-002 | The public `:ls-tree` op accepts a non-directory `:dir`, reaches discovery, and reports an untyped error (or a stack trace) instead of a refusal. **Or**: a root the entrance ACCEPTED reports an untyped error, or scans empty while its target scans full. | `{:error-type :workspace-root-not-a-directory}` returned before discovery starts; an accepted root either lists its projects or says in words what it did not find. | `ls-tree-entrance-refuses-a-non-directory-root-without-executing-it`, `an-empty-scan-names-what-it-searched-instead-of-throwing`, `a-symlinked-root-is-descended-just-like-its-target` |
| MCP-OP-SHELL-ARGV-003 | A project directory named `b\nad` is absent from `:ls-tree` output while an ordinary sibling project is present — on the full-scan path, or on the `:grep` fast path. | Both projects discovered on both paths; the newline is data inside one path. | `project-directories-containing-a-newline-are-discovered` |

## Deliberately out of scope in this leaf

- **Argument injection is closed for the leading-`-` class only.** A value that
  `find`, `rg`, or `grep` would read as an *option* because it begins with `-`
  is a different failure class from shell injection: it cannot execute an
  arbitrary command, only mis-parse a flag. That class is closed at every
  discovery site — `find-start-token` prefixes `./` to a relative start point
  that begins with `-`, and `grep-tree` passes the caller's pattern after `-e`
  and the directory after `--`.

  It is **not** closed for the rest of the class. `find`/`bfs` also treat `(`,
  `)`, `!`, and `,` as expression starts, and `find-start-token` only inspects
  a leading `-`: a relative directory literally named `(` yields `[]` from
  `find-build-files` (raw find: `bfs: error: Expected a ).`) while `./(` yields
  its `deps.edn` — a silently missing project, the same symptom class
  MCP-OP-SHELL-ARGV-003 exists to forbid. Not reachable through `run-ls-tree`,
  which absolutizes, but reachable through `rename/plan`'s `:root`
  (`src/clj_surgeon/rename.clj:118-121`). **Named follow-up**: `./`-prefix
  every relative start point, not only `-`-leading ones. No witness pins any of
  this yet.

- **Environment fact for anyone re-running these receipts.** `/usr/bin/find`
  on Anvil is **`bfs 4.1.1`**, not GNU findutils. The argv shape used here is
  compatible with both, but `-print0`, the parenthesised `-name` alternation,
  the `-H` start-point follow, and the `(`/`!` mis-parse above have been
  verified against bfs only.
- **Any entrance outside project discovery.** The audit that produced this leaf
  found exactly one `sh -c` site in `src/`; if a second appears, it belongs
  under MCP-OP-SHELL-ARGV-001, not a new prefix. Every other subprocess in
  `src/` already runs an argv vector through `ProcessBuilder`, and
  `worktree-lifecycle-io/validate-process-request` already refuses an argv
  containing `sh`, `bash`, or `-c` — a pre-existing ratchet of this same
  invariant, at a different site.
