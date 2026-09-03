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

- [ ] **MCP-OP-SHELL-ARGV-003**: When clj-surgeon consumes the output of an external file-discovery command, clj-surgeon shall delimit that output with NUL so that a path containing a newline is returned as one intact path.

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
