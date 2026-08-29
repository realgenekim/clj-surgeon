# Captain's log: positional mutation authority left the installed CLI

Date: 2026-08-29

Issue: `clj-surgeon-qf9`

## Outcome

The stable local CLI now refuses direct mutation when a positional selector
chooses the subject. Direct mutation continues to work when the caller names a
top-level owner.

The installed product is the minimal Option A candidate:

- commit `75585beeda63a4dcc9bb1e219d5721d89b93baa2`
- tree `b61fe3610643ddd23f1e7061879ac871b55e623a`
- installed source hash
  `dc65dfd56e2067704f389428f7ccbf5eec770b474bb9532f5b5268b815cf3d39`
- installed launcher SHA-256
  `d99ffc4e635bea3d5254786bc61768bd1f6df547b4389f2be0c6cc41f0c3ecdd`

Compared with the prior installed commit
`b8e52cb603c35471cab6d4f562161a1a588c3b20`, exactly two production
namespaces changed:

1. `src/clj_surgeon/core.clj`
2. `src/clj_surgeon/structural_lens.clj`

The edit-field aliases and all other in-flight product work were excluded.

## Gate and causal exception

The candidate full gate started at load averages
`17.47 / 126.37 / 303.25`.

- fast suite: 639 tests, 5,491 assertions, zero failures
- analyzer contract: 4 tests, 20 assertions, zero failures
- MCP suite: 269 tests, 2,284 assertions, two failures

Both MCP failures were in
`cold-clj-kondo-admission-timeout-is-unverified`:

- expected `:clj-kondo-admission-timeout`; observed
  `:clj-kondo-admission-unverified`
- expected `:admission-timeout`; observed `:delegated`

Publication stopped until the same MCP suite was run from an isolated, clean,
unmodified prior installed base. Base commit
`b8e52cb603c35471cab6d4f562161a1a588c3b20`, tree
`fe42cf35db2c743bd64351fab65f03f63686034e`, started at one-minute load
16.52 and produced the identical two failures. The focused base witness also
failed identically. The lower-level real
`admission-timeout-launches-no-second-analyzer` witness passed.

The base and candidate contain identical Git blobs for the failing test,
`mcp_cold_verify.clj`, and `mcp_process.clj`. The release authority therefore
classified the two failures as a pre-existing cold evidence-timing assertion,
not a qf9 regression, and declared them non-gating for this release. The
separate test defect remains recorded on `clj-surgeon-qf9`; it was not hidden,
deleted, retried into green, or used to change product behavior.

## Installed-artifact proof

The installation command was:

```sh
make -C /Users/genekim/src.local/clj-surgeon-qf9-cli-release install-cli
```

No `make install`, `make mcp-reload`, restart, or shared-port action occurred.

Before installation, the duplicate-content positional command exited zero and
silently changed `wrong` while leaving `intended` unchanged. The resulting
file SHA-256 was
`64461b749cba4c77c55addb31684320440b5786ac6a29373da77c232a7024a71`.

After installation, the same command against the original fixture exited one
and returned:

- `:error-type :positional-mutation-authority-refused`
- `:required-root [:form OWNER]`
- `:source-state :unchanged`
- `:source-unchanged true`
- a remedy that instructs the caller to start with `(form 'OWNER)`

The source SHA-256 remained byte-identical before and after:
`55cf78789dea785d06fd6fe6c45ffaafbeb9de4bcaa0aa40a8c43d1353af3a1f`.
Both `intended` and `wrong` retained `:old`.

The installed named-owner command then selected `(form 'intended)`, exited
zero, returned `:ok true` and `:mode :expect-guarded`, and changed only
`intended` from `:old` to `:new`. `wrong` retained `:old`. Its source and result
hashes were:

- source: `051ec41165c80772be3205a5653b70e9e38b34d48156ba20ba438d21d51c4ab1`
- result: `bc4d6250d00c94d14c79825765cdd4b200cfa119d70291ba0dc42b5f318db119`

## Rollback and runtime continuity

The durable rollback worktree is exact prior commit
`b8e52cb603c35471cab6d4f562161a1a588c3b20`. The rollback command is:

```sh
make -C /Users/genekim/src.local/clj-surgeon-cli-rollback-b8e52 install-cli
```

The command was exercised and the installed receipt returned to `b8e52cb`.
The qf9 install command was then exercised again, leaving the final installed
receipt at `75585be`.

The shared MCP listener remained PID 65458 on port 7888 before and after all
CLI pointer changes. Existing agents need no session restart for future CLI
calls because each invocation starts from the installed launcher. The live MCP
server remains unchanged until a separately authorized publication window.

