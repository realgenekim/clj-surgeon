# WRITE-REFUSAL-001 installed release

Date: 2026-08-30

Verdict: **installed and verified** for exact candidate
`9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
`6f9bc30316eb6417977c07c86caf8eb146dfbdb8`.

## Authority and release identity

Gene authorized the install with the verbatim decision `Go on all.` The
canonical branch `release/closed-relations-published` was fast-forwarded to
the exact candidate and pushed. The annotated tag
`stable-write-refusal-001-20260830` dereferences to that commit. Its tag object
is `df1184c87b0e8a77c9815ea6a5db35f8b5b457c1`.

The release train contains the already-published read normalization
`c55de227`, the prepared-request slice `b445a8c`, and WRITE-REFUSAL-001
`9af88fba`. It intentionally excludes the unrelated cold-clock test fix
`7dccc395`, README cargo `748bae3`, and doctrine/skill commits `8539e7e` and
`a4d7feb`. Those bytes were not part of the independently verified candidate.

`make install` produced these installed-path identities:

- CLI receipt SHA-256:
  `63ea06af63041398d81e97c84e0cd1c0e5087f6cff132d8cdff65cc6b767d14b`;
- installed CLI source commit: exact `9af88fba`;
- installed CLI source hash:
  `cb182e4c0aa44819442babc2e0ca4c9b99bf569b07ef8a9c095bbf12910e21b9`;
- Codex and Claude skill receipt SHA-256:
  `ef7f3675d0bd1094e8bb8c6024a91a7a0ddab231ed183fd030f7f5c245a51a10`;
- analyzer-gate receipt SHA-256:
  `6727c9f98ec7c18449e95c4d7dc22e6f2de7f580b53b3e6db6a8df91e4521029`.

`make mcp-reload` synchronized the contract without a server restart:
`server-restart-required=false`, tool count 4, contract hash `74450147` before
and after. The shared server PID was `65458` before and after the reload and
all installed-route proofs. `/healthz` returned
`{"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}`.

## Installed-route proofs

The proof used the live shared HTTP route at `http://127.0.0.1:7888/mcp` and
three disposable workspaces. It did not load candidate namespaces directly.
Raw JSON-RPC requests, raw responses, parsed responses, and the scored report
are retained under
`bench/results/2026-08-30-write-refusal-001-installed-route/`.

### Complete 27-owner refusal

One scoped edit named 27 owners, found one `:old` value in each owner, and
declared an expected count of 28. The installed route refused with
`expect-count-mismatch`, `source_unchanged=true`, and byte-identical source.
It returned exactly 27 of 27 inert rows, omitted none, did not truncate, and
returned no continuation. The structured result was 5,874 UTF-8 bytes. Raw
response SHA-256:
`ef48c81a41a5aa1a6cd3ce3b3fa8099edee11668a930602a70eaa8be5bc62d7e`.

### Bounded 129-owner refusal

One scoped edit named 129 owners and declared an expected count of 130. The
installed route refused before write, kept the source byte-identical, and
reported the exact universe: 129 available, 128 returned, one omitted. Its
version-1 continuation had `executable=false`, `authority=false`,
`write_authority=false`, `next_offset=128`, and `remaining_count=1`. The
structured result was 24,046 UTF-8 bytes, below the 32,768-byte public bound.
Raw response SHA-256:
`b56a6fd845964710b8c32f30e47a5129043b15c362ddc337d392cacfe03b6ba5`.

### Ordinary success no-cue parity

One ordinary one-owner edit committed and returned
`verification_complete=true`, with no `write_refusal_evidence`. After the
same named exclusions used by the pre-install live-route gate
(`workspace_root`, receipt identity, undo identity, and timing/receipt
metadata), its canonical result SHA-256 was
`b3fb1f3f997e57ba4d05d8791a62bd181b0ad634d0e25adfe4684369586b7f92`.
That exactly equals the pre-install control hash. The resulting source SHA-256
was `1a6fd57dbbe2675354c4371031cc45c2c4f81e839c621e9bd4ec2c9dd96aac09`.

## Replay

From this repository at the release receipt commit, with the installed shared
server still available:

```sh
clojure -Sdeps '{:paths ["bench"]}' -M \
  -m prove-installed-write-refusal-001 \
  http://127.0.0.1:7888/mcp \
  65458 \
  bench/results/2026-08-30-write-refusal-001-installed-route \
  /private/tmp/clj-surgeon-write-refusal-001-proof-workspaces

(cd bench/results/2026-08-30-write-refusal-001-installed-route \
  && shasum -a 256 -c MANIFEST.sha256)
```

The proof harness SHA-256 is
`37142907f356b5b0bc4b571d7aa0a37fea4212d9a63dd8222a50efcc52e6ef0f`.

## Claims boundary

The live price measurement showed no catalog or request cost on unaffected
paths. A realistic 27-row refusal adds 5,241 response bytes and 1,656
`o200k_base` tokens; these are caller-input costs. The write-family recovery
benefit remains **unmeasured**. The install publishes complete inert evidence
and a bounded inert continuation; it does not publish a measured turn, token,
or wall-time saving for this write family.
