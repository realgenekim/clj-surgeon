# Prepared confirmation and preview: installed-route receipt

Date: 2026-08-31

Verdict: **installed and proved on the shared live route**.

Authority: Gene, relayed verbatim by MAYOR@SKIFF: "Go -- dogfood it and
use it to archive greatness".

## Published identity

- Canonical branch: `release/closed-relations-published`
- Exact commit: `05f5a1962e5a0c5aa0365c673994eca9024c1a44`
- Exact tree: `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`
- Tag: `stable-prepared-confirm-preview-20260831`
- Tag object: `e8466ba0a903e870195b234668d615d54da98e5e`
- Independent GO receipt: `7370c966b5c655fa7ad4549026dfcda4fac0fc81`

The canonical branch advanced by fast-forward from
`9af88fbae9ee720613599feaf8cf58432c5898bb`. No merge commit and no
unrelated release cargo were added.

## Installation

`make install` installed the stable CLI and agent skills from exact commit
`05f5a196`.

- CLI source SHA-256: `c23f5bf3a6a5b8346b5f2b1ac708d6b4c4b726ee27c2d9e75709e79d4ee4d97a`
- Skill source SHA-256: `cc4f6cc7d378947214d91b6e2260214c4b5061792c8b687a1e44afdec8679c59`
- Retained CLI receipt SHA-256: `3599d7b494e044718ea2d3cfd2afbb74178d44eca31309b143983655ac8b7687`
- Retained Codex skill receipt SHA-256: `2706690ffbe63e36c96b4da5d8021f50747e43fd99608b3d026ecae70404a143`

The live MCP process was PID `65458` before and after publication. It was
not restarted.

The first stock `make mcp-reload` returned a truthful no-op:
`before-contract-hash=74450147`, `after-contract-hash=74450147`, and no
upserted tools. The first live proof then refused because the installed
`edit_clojure` schema still lacked `confirm`. This attempt is excluded.

Root cause: the August 26 JVM has a relative `src` classpath anchored to its
original working directory. The main checkout had since been moved to an
unrelated docs branch, while the published release lived in an isolated
worktree. The ordinary reload therefore re-read old source.

The production-attached nREPL was independently bound to PID `65458` on port
`58942`. Seven changed namespaces were loaded by absolute path from the exact
published worktree, followed by `sync-tools!`. The repaired hot reload returned:

```text
before-contract-hash 74450147
after-contract-hash  8dc75168
upserted              [edit_clojure inspect_clojure]
server restart        false
```

This preserved the process and avoided modifying the dirty main checkout.

## Installed-route proof

The final proof used a fresh Streamable HTTP session at the shared installed
route, `http://127.0.0.1:7888/mcp`, and a disposable workspace. Response
session identifiers are retained only as SHA-256 hashes.

1. `tools/list` advertised `confirm` and `preview` on `edit_clojure`.
2. An eligible `inspect_clojure` forms read served one non-executable,
   no-authority prepared confirmation digest.
3. `{confirm, fill, preview:true}` returned operation
   `edit_clojure-preview`, exact old/new diff evidence, and
   `source_unchanged=true`; the source remained byte-identical.
4. `{confirm, fill}` committed the exact change with `committed=true` and
   `verification_complete=true`. Read-back SHA-256 was
   `cbe34fad1f94d691ed875e66e6df273bfa23c41279fed0fba050c44554b8763d`.
5. Replaying the consumed digest refused with
   `prepared-confirmation-consumed` and `source_unchanged=true`.

Proof report SHA-256:
`b1ea8d0abcf23da6c30232421629420558b4dd45b03018d972c321f93f06ce20`.
Proof program SHA-256:
`14c43553063755a99f24fe9a9e1cb323f2dfecd615d574be233238c6a8a3740d`.

Replay:

```sh
python3 dev/experiments/verify_installed_prepared_actions.py \
  --result-dir /private/tmp/clj-surgeon-prepared-actions-installed-proof
```

The exact serialized requests, response bodies, sanitized headers, report,
and install receipts are retained under
`bench/results/2026-08-31-prepared-confirm-preview-installed-route/`.

## Claim boundary

The measured product price remains the pre-install live-route result:

- one prepared commit removes 20 emitted `o200k_base` tokens, 23.8%;
- the catalog adds 261 cacheable input tokens.

There is no product wall-time claim, routing claim, or compression claim from
this installation.

