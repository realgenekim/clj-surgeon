# Global compact-editor routing

**Status:** Implemented at `6ff11c9`; accepted on laptop and Anvil

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

## Acceptance record

The installer passed 4 focused tests with 38 assertions, including malformed
marker refusal, preflight-before-write, exact unmanaged-byte preservation, and
byte-idempotence. A clean temporary-home `make install` created both global
files; `make check-agent-routing` then passed with one block per file.

Laptop Codex and Claude passed the same check. Anvil dev-a, dev-b, and dev-c
each received the block plus CLI, Codex skill, and Claude skill receipts for
`6ff11c9`. Codex was already registered for the shared port-7888 MCP. The
acceptance audit found that Claude had no MCP registration, so the rollout also
added the shared HTTP server at user scope. `claude mcp list` reported
`Connected` on all three seats.

Fresh sessions now receive the compact route. Existing sessions can retain
cached instructions or tool schemas and require a new session.

The final constituent gate passed 609 Babashka tests with 5,235 assertions and
197 JVM MCP tests with 1,626 assertions at `-Xmx512m`. The laptop's first stdio
smoke attempt received no responses before its 120-second timeout while system
load was about 277. The unchanged smoke then passed on Anvil in 7.52 seconds
and locally in 55.53 seconds after load fell. The remaining usage, benchmark,
retention, and evidence self-tests passed. Anvil's Babashka 1.13 runner also
exposed an unrelated existing SCI compatibility error for `case*`; laptop
Babashka 1.12 passed that suite.
