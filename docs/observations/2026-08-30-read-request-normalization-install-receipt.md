# Read-request normalization install receipt

Date: 2026-08-30 PT

State: installed and verified

## Installed subject

- product commit: `c55de2279826af5ed21c90981591479dd2e802b2`
- product tree: `565f009f0ff25fdedbc2fba5ad9ba5f55783e023`
- release branch: `release/read-normalization-published-20260830`
- stable tag: `stable-read-request-normalization-20260830`
- tag target: `c55de2279826af5ed21c90981591479dd2e802b2`

The prior published release head `7bd6da7` was an ancestor of the candidate.
The release was therefore a fast-forward of the existing published line, not
a rewritten cherry-pick. The live server's owning checkout was also
fast-forwarded to the same exact commit. Its unrelated pre-existing dirty
files were preserved; none overlapped the candidate delta.

## What changed

`inspect_clojure` now accepts two shorter but closed read shapes:

1. A complete forms request can omit `operation: forms`. File, owner names,
   and exact expected form count remain explicit.
2. A multi-request call can omit every call-local ID. The server assigns
   deterministic IDs such as `request-1` and `request-2`.

IDs must still be all supplied or all omitted. A mixed batch refuses before
source reading. This removes a whole invalid request class instead of guessing
what the caller intended.

The measured request savings occur in model output/decode: 4 proxy tokens for
one operation-less request and 12 for a two-request omitted-ID call. The
catalog grows by 317 input/prefill tokens. The separate prefill/decode probe
measured input as 1,284 times cheaper than output on the tested route, so the
catalog and argument tokens are not economically interchangeable. The
candidate is promising because it removes expensive output ceremony and
refusal opportunities while keeping subject identity explicit.

## Verification before publication

- `standard-clj` v0.24.0 formatted the six changed Clojure files with no diff.
- Focused normalization gate: 5 tests / 42 assertions / 0 failures / 0 errors.
- The release `make mcp-test` run completed 300 tests / 3,433 assertions with
  2 failures / 0 errors. Both failures were the disclosed load-sensitive
  `cold-clj-kondo-admission-timeout-is-unverified` assertions: the test saw
  `:delegated` / `:clj-kondo-admission-unverified` rather than its 250 ms
  timeout. This run is not labeled green. Independent quiet exact-candidate
  gates before the install were green, and the normalization namespace was
  green in both runs.

## Installed artifacts

`make install` completed from the exact product commit.

| Artifact | Installed evidence |
|---|---|
| stable CLI | commit `c55de227`; source hash `7a399bc4cac04c4e86d19e06334d9de535aede09d86bc0f8c743126e9bee3f20` |
| Codex skill | commit `c55de227`; source hash `cc4f6cc7d378947214d91b6e2260214c4b5061792c8b687a1e44afdec8679c59` |
| Claude skill | commit `c55de227`; source hash `cc4f6cc7d378947214d91b6e2260214c4b5061792c8b687a1e44afdec8679c59` |
| analyzer admission gate | installed by the repository's stable `make install` target |
| global routing | already current; 2 targets checked, 0 changed |

Receipts are at `~/bin/clj-surgeon.receipt.edn`,
`~/.codex/skills/clj-surgeon.receipt.edn`, and
`~/.claude/skills/clj-surgeon.receipt.edn`.

## Shared MCP publication

The shared MCP remained PID 65458; it was not restarted. Its CWD and relative
`src` classpath are `/Users/genekim/src.local/clj-surgeon`.

An initial reload from an isolated worktree synchronized but retained contract
hash `53b40e3f`; this correctly did not count as publication because the live
JVM's classpath still belonged to the original checkout. After safely
fast-forwarding that checkout to the exact product commit, `make mcp-reload`
reported:

```clojure
{:after-contract-hash "63069bc8"
 :before-contract-hash "53b40e3f"
 :upserted ["inspect_clojure"]
 :tool-count 4
 :status :synchronized
 :ok true
 :server-restart-required false}
```

This is the publication boundary: the contract hash changed and the live
registry upserted `inspect_clojure` without replacing the shared JVM.

## Installed-route proof

`dev/experiments/verify_installed_read_normalization.py` opened a new MCP HTTP
session against `http://127.0.0.1:7888/mcp`. It did not call the worktree
implementation directly. The exact HTTP requests, response bodies, response
headers, report, and manifest are retained at
`bench/results/2026-08-30-installed-read-normalization/`.

| Required proof | Live installed result |
|---|---|
| `tools/list` schema | 5 request variants; exactly 1 operation-less forms variant |
| installed input-schema SHA-256 | `4782eba0d8eebb930d04dacdc3b1e43e0cba6fac1fe7bdf6f0c4f6a63a2829c4` |
| operation-less forms call | success; `read_complete=true`; generated ID `request-1` |
| mixed supplied/omitted IDs | typed `mixed-request-ids` refusal |
| pre-read safety | `source_unchanged=true`, `read_started=false`, `read_complete=false` |
| source before/after SHA-256 | `797ea6f946b391e7572130b9d4c5d7b904d31f0ee5fc7418cfbd733ac04c412a`, unchanged |

- installed-route report SHA-256:
  `a1b9f34c408f8a1376250e4b4de5e324791fa17d6eb22c50b62fd92f02789a57`
- installed-route manifest SHA-256:
  `eb5600071b4ade3b79324507938450546621168a9a04fece8ff121e12d91434a`

The current Codex turn demonstrated the documented cache boundary: its old
model-visible schema refused a helper outline even after the server published
the new schema. Existing sessions can continue for behavior-only requests, but
a caller whose cached schema rejects the operation-less shape needs one new
agent session. The shared server must not be restarted for that client cache.
