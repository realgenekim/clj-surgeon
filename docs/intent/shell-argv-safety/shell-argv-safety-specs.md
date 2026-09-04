---
parent: shell-argv-safety-design
prefix: MCP-OP-SHELL-ARGV
status: "red-first; Andon pull inb-d27b79, 2026-09-03"
---

# External Command Invocation Safety Specifications

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

- [x] **MCP-OP-SHELL-ARGV-001**: When clj-surgeon runs an external command on behalf of project discovery, clj-surgeon shall invoke that command as an explicit argument vector in which every caller-supplied value occupies exactly one token, and shall never pass a command string built from caller-supplied data to a shell interpreter.

- [x] **MCP-OP-SHELL-ARGV-002**: When the `:ls-tree` entrance receives a `:dir` that does not resolve to an existing directory, clj-surgeon shall return the typed refusal `:workspace-root-not-a-directory` before project discovery starts.

- [x] **MCP-OP-SHELL-ARGV-003**: When clj-surgeon consumes the output of an external file-discovery command, clj-surgeon shall delimit that output with NUL so that a path containing a newline is returned as one intact path.

- [x] **MCP-OP-SHELL-ARGV-004**: When clj-surgeon reads a build file it discovered under a caller-named directory — `deps.edn`, `bb.edn` or `project.clj`, reached from `:ls-tree`'s `:dir` through project discovery — clj-surgeon shall read that file AS DATA with a reader that does not evaluate, shall never evaluate any form the file contains, and shall fall back to the default source paths when the file is not readable as data; and "as data" means a reader for which `*read-eval*` is not consulted, not a reader called with `*read-eval*` bound false, because a binding is a property of one call site and the requirement is a property of the reader.

- [x] **MCP-OP-SHELL-ARGV-005**: When any source under `src/` calls a reader that evaluates what it reads — `clojure.core/read-string` or `clojure.core/load-string`, whether unqualified, fully qualified, or reached through an alias of `clojure.core` — clj-surgeon shall fail its own test suite naming the file and the symbol, unless that site is enumerated in the witness's allow-list together with the reason it is safe; the allow-list's target is empty, and "the caller cannot name this file today" is not a reason, because it is a statement about the current call graph rather than about the reader.

- [x] **MCP-OP-SHELL-ARGV-006**: When clj-surgeon reads the source paths out of a build file it discovered under a caller-named directory — the `:paths` of a `deps.edn` or `bb.edn`, the `:source-paths` of a `project.clj` — clj-surgeon shall treat every entry as UNTRUSTED CALLER DATA and not as configuration it may follow. Each entry shall be validated as a string, and shall then be RESOLVED against the project root and FENCED against it, so that an entry which resolves outside that root is refused as a typed, counted refusal naming the entry AS THE CALLER SPELLED IT and never the tree it targeted, rather than walked and printed. A non-string entry shall be the same typed refusal and shall never reach `io/file`. The fence shall be applied to the RESOLVED path, because `..` and an absolute entry are the same escape reached by two spellings and a check on the spelling catches neither: `{:paths ["../outside"]}` and `{:paths ["/absolute/tree"]}` each directed `:op :ls-tree` to enumerate and print an arbitrary tree — 80 files, 2,499 forms, every namespace, every `require` and every `def` name with its line range — at exit 0, at both real launchers, from a directory whose only power the caller had was to write a file in it. Round twenty-three closed the READER half of this vector and left the CONFIGURATION half open: the same op, the same frame, the same premise that controlling a directory is enough, with the payload changed from code to data. The refusal shall be counted so that a tree whose build file names one escaping path and one legitimate one is not silently reported as complete, and the count shall be published on the receipt; "no Clojure files found under the directory you named" asserted over a walk that left that directory is a completeness claim about a tree that was never read.

## Misreadings these requirements exist to forbid

- "Escape or quote the directory before interpolating it." A filter is
  weakenable by the next refactor and was already the shape of the defect.
  MCP-OP-SHELL-ARGV-001 forbids the command string, not a character class.
- "Validating the directory at the entrance is enough." An entrance check
  leaves the shell in place for every future caller; MCP-OP-SHELL-ARGV-001 and
  -002 are separate promises for that reason.
- "A path cannot contain a newline, so line splitting is safe." It can. The
  observable symptom is a silently missing project, not an error.
- "Returning an empty result for a bad root is a refusal." It is not; an empty
  result is indistinguishable from an empty tree. MCP-OP-SHELL-ARGV-002 demands
  a typed `:error-type`.

## Falsifiers

The falsifier matrix, with the required result and the named witness for each
requirement, is in `shell-argv-safety-design.md`. Each falsifier is executable
and lives in `test/clj_surgeon/core_discovery_test.clj`.

## Rationale

Andon pull `inb-d27b79`, 2026-09-03: `find-build-files` interpolated the
caller-supplied `:dir` into an `sh -c` command string. Confirmed by direct
reproduction and end to end through `run-ls-tree`; both created a canary file.
The same call site dropped newline-bearing project directories through
`str/split-lines`.
