# Global compact-editor routing

**Status:** Accepted implementation plan

## Outcome

Make the proven compact Clojure editing route an always-loaded instruction for
Codex and Claude on every managed laptop and Anvil coding seat. Keep one
canonical routing block in this repository. Install or update that block
without changing unrelated global instructions.

This work distributes an existing capability. It does not change editor
semantics or claim that every task should use clj-surgeon.

## Observable contract

The repository owns one Markdown block with versioned begin and end markers.
One installer accepts the Codex `AGENTS.md` path and Claude `CLAUDE.md` path.
For each target, it must:

1. create a missing file and its parent directory;
2. append the block when both markers are absent;
3. replace exactly one complete older block;
4. preserve every unrelated byte;
5. make a second installation byte-identical;
6. refuse duplicate, missing, reversed, or unbalanced markers without writing;
7. write atomically; and
8. report the target, previous state, changed state, and block hash as EDN.

`make install` installs the CLI, both advanced-only skills, and the global
routing block. `make check-agent-routing` verifies that both configured files
contain the exact current block once.

## Canonical routing content

The block must teach only the common decision boundary:

- batch known structural reads and use `include_source=false` only when source
  is not needed;
- use one `edit_clojure` transaction when the complete decision already names
  files, owners, old forms, replacements, counts, programs, or owner deletions;
- use `within.form` for named owners and `within.namespace` for the `ns` form;
- treat resolved owners, exact old forms, counts, and the frozen snapshot as
  stale-source guards;
- treat `verification_complete=true` as terminal mutation evidence;
- apply the same proportional formatter, linter, and test policy as native
  editing; and
- prefer native patching for small visible text edits, prose, new files, or
  unsupported operations.

The block must not require the advanced skill for an ordinary compact edit.
It must not promise that already-running model contexts reload instructions or
tool schemas.

## Behavior matrix

| Starting file | Required result |
|---|---|
| missing | create with exactly one block |
| empty | write exactly one block |
| unrelated text, no markers | preserve text and append one block |
| one older managed block | replace only that block |
| exact current block | no-op |
| begin without end | refuse, unchanged bytes |
| end without begin | refuse, unchanged bytes |
| duplicate markers | refuse, unchanged bytes |
| reversed markers | refuse, unchanged bytes |
| first target valid, second invalid | preflight both and write neither |

## Deployment and acceptance

1. Run pure and filesystem boundary tests locally.
2. Run `make install`; verify the laptop Codex and Claude files with
   `make check-agent-routing`.
3. Run the same commit's `make install` for Anvil dev-a, dev-b, and dev-c.
4. Verify each seat's exact managed block, MCP registration, CLI receipt, and
   Codex/Claude skill receipts.
5. Record that fresh agent sessions receive the new instructions. Record that
   an already-running session can retain cached instructions or MCP schemas and
   can require a new session.

## Completion gates

- Standard Clojure Style on changed Clojure files;
- focused routing and installation tests;
- installer shell or CLI smoke;
- complete `make test` at the 512 MiB MCP envelope;
- live laptop and three-seat Anvil acceptance; and
- updated explanatory documentation and Captain's Log.
