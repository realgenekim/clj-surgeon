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

## Why NUL and not newline

`find`'s default `-print` terminates each path with a newline, and a POSIX
path may contain a newline. `str/split-lines` therefore turns one real path
into two fictional ones. The defect was invisible because the fictional paths
simply failed to parse and were dropped — a silent, correct-looking result.
`-print0` plus a NUL split removes the ambiguity at the source rather than
detecting it downstream.

## Falsifier table

| Intent | Falsifier — what would prove the promise broken | Required result | Witness |
|---|---|---|---|
| MCP-OP-SHELL-ARGV-001 | A `:dir` of `<real dir>; touch CANARY ; echo z` or `<real dir>$(touch CANARY)` causes `CANARY` to exist after discovery runs. | `CANARY` never exists; discovery returns no build files. | `hostile-dir-never-reaches-a-shell-from-find-build-files` |
| MCP-OP-SHELL-ARGV-002 | The public `:ls-tree` op accepts a non-directory `:dir`, reaches discovery, and reports an untyped error (or a stack trace) instead of a refusal. | `{:error-type :workspace-root-not-a-directory}` returned before discovery starts. | `ls-tree-entrance-refuses-a-non-directory-root-without-executing-it` |
| MCP-OP-SHELL-ARGV-003 | A project directory named `b\nad` is absent from `:ls-tree` output while an ordinary sibling project is present. | Both projects discovered; the newline is data inside one path. | `project-directories-containing-a-newline-are-discovered` |

## Deliberately out of scope in this leaf

- **Argument injection** (a value that `find`, `rg`, or `grep` would read as an
  *option* because it begins with `-`). This is a different failure class from
  shell injection: it cannot execute an arbitrary command, only mis-parse a
  flag. `find-build-files` is guarded by an existing-directory check, and
  `run-ls-tree` absolutizes before discovery, so the reachable surface is a
  relative directory literally named `-x`. Registered here as known and
  unratcheted rather than silently omitted.
- **`:grep` pattern handling** in `grep-tree`. It is already invoked as an
  argument vector, so it carries no shell-injection exposure; it retains the
  option-parsing exposure described above.
- **Any entrance outside project discovery.** The audit that produced this leaf
  found exactly one `sh -c` site in `src/`; if a second appears, it belongs
  under MCP-OP-SHELL-ARGV-001, not a new prefix.
