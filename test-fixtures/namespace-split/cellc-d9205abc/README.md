# Cell C namespace-split fixture, frozen 2026-11-28

Provenance. The exact witnessed contract behind the one automatically routed class, "exact-contract
namespace split": curtaincall-cfp commit `d9205abcc9b9f31d93bd4a76537b74fccd583ca6`,
`src/cfp_scheduler_killer/views.clj` (141 named owners into 20 absent destinations, 87 static sites in
the caller files). Until today the request and its receipts lived only under `/var/tmp` on Anvil, where
systemd's thirty-day cleaner reaped the alias-migration sentinel's evidence; this directory is the copy
that cannot be lost (fleet plan step 2; mayor's MORE-TONIGHT, 2026-11-28).

Files.

- `source/src/cfp_scheduler_killer/views.clj`, `exports.clj`, `server.clj`, `sinks.clj`: the bytes at
  `d9205abc` (`git show d9205abc:<path>` in `/home/forge/src/curtaincall-cfp`). The twenty `views/*.clj`
  destination files the receipts name do not exist at that commit; the split creates them.
- `D1-request.edn`: the frozen Cell C request as written 2026-09-07 (`/var/tmp/forge/plan2/cellC/`). The
  weekly sentinel sent a byte-identical request apart from `:workspace_root`; `:workspace_root` must be
  rebound to a fresh worktree of the source commit before use.
- `cc-split.json`: the same manifest in the MCP `namespace_split` shape; `cc-oracle.py`: the Cell C
  oracle used by the round runner.
- `sentinel-20261025-receipt.json`, `sentinel-20261025-split-receipt.edn`: the round receipt and the
  split receipt of the last sentinel run that completed (2026-10-25, trunk 454a2863). Read them together:
  the split itself verified (committed state, complete verification, oracles 4/4, 87 sites across five
  callers: `server.clj` 62 and four test files 16, 4, 2 and 3), while the ENCLOSING round recorded
  `verdict: RED`, `exit_code: 1` and a witness-runner exit of 97 with unknown test counts. The split
  passed; the round around it did not. The four test callers are not frozen here; `exports.clj` and
  `sinks.clj` are frozen because the request's scope names them, though the receipt rewrote neither.

Not here: the split's output tree. The receipts carry its hashes. Reproducing the result needs more than
this directory: a worktree of curtaincall-cfp at `d9205abc`, the verb from `archive/astra/namespace-split`
with `:workspace_root` rebound to that worktree, and the `split-unit` configuration the request's named
cold profile expects, which `d9205abc` itself does not carry. `cc-oracle.py` reads its inputs from the
old `/var/tmp/forge/plan2` paths and must be pointed at the new locations before it runs. The frozen bytes
here are the inputs and the receipts, not a push-button replay.
