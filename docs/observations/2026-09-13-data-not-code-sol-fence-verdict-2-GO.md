GO

Reviewed sealed candidate `06dba69121b0fc5a62b01b65884c3f7d24699e71`.

- Envelope injection: CLI returned `:unknown-arguments`; MCP returned `:invalid-mcp-request / :unknown-fields`; the HTTP probe returned `:invalid-probe-request` when both forged `:destination-envelope` and outside `workspace_root` were supplied.
- Hard links: targeted boundary tests passed. The claim covers discoverable directory entries at admission time. Bind-mount aliases and links created after admission remain explicitly outside scope.
- Evidence, not attestation: a coordinated forged receipt plus consistently recomputed row was accepted (`:coordinated-forgery-accepted true`), matching `DATACODE-ROWS-001`, which explicitly disclaims cryptographic provenance.
- Merge preservation: probe authorization, closure, roots, and nesting witnesses passed: `5 tests / 600 assertions / 0 failures / 0 errors`. Parent-to-merge diffs showed only additions to the refusal registry and deftest census.
- Fixed-point repair: `bb test/diff_impact.clj --self-test` passed depths 1–8, every-edge cuts, cycles, multiple seeds, libspec shapes, and the exact prior finding. The formerly missed path is:
  `src/clj_surgeon/mcp_extraction.clj → mcp-contract → mcp-prepared-confirmation → mcp-prepared-wire-test`.
- Independent receipt parsing found 82 inventory entries and 82 successful results: `1,690 tests / 25,101 assertions`, with exactly `mcp-prepared-wire-test` added and none removed. That namespace passed `5 tests / 31 assertions`.
- Targeted envelope, sandbox, evidence, and sleep checks passed: JVM `7 tests / 119 assertions`; Babashka `2 tests / 48 assertions`.
- Parsed fast and prewarm receipts both report `:state :passed` and `:problems []`; prewarm is correctly marked `:landing? false`.
- No `make test` was run. `git diff HEAD --numstat` is empty; the pre-existing untracked reviewer log was preserved.

> END RECEIPT (fence-run): worktree HEAD at review exit = 06dba69121b0fc5a62b01b65884c3f7d24699e71 = fenced sha.
