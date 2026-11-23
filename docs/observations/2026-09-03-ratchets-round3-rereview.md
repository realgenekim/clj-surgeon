# receipt-ratchets fe7a1a1 — Sol executed round-3 re-check: GO-WITH-FIX (regex-literal newline; absorb main) — round 4 launched

GO-WITH-FIX. Both round-two defects are closed, but the broader DISPATCH-004 contract remains partial: raw-newline regex literals still produce multiline vocabulary entries.

Verification:

- Exact round-two probes: 2 tests, 10 assertions, green.
- `make test-fast`: 719 tests / 6,010 assertions; only five stale routing-document failures.
- MCP suite: 400 tests / 4,138 assertions; only the known host-specific clj-kondo-path failure.
- Candidate-catalog suite: 20 tests / 265 assertions; one pre-existing handler-identity failure.
- Benchmark prompt self-test passed.
- Worktree remains clean.

Merge-queue findings:

1. **PARTIAL — dispatch presentation.** [owner_hypotheses.clj:177](src/clj_surgeon/owner_hypotheses.clj:177) handles only `:multi-line`; the original string witness now yields `"a\\nb"`, one physical line, equal reader value, and exact selector matching, but `#"a<newline>b"` is a `:regex` leaf and still yields two physical lines. Add regex-safe newline escaping and a witness before merge.

2. **CLOSED — split-catalog admission.** [mcp_candidate_catalog.clj:412](dev/experiments/clj_surgeon/experiments/mcp_candidate_catalog.clj:412) now exposes only `workspace_root`, `edits`, `programs`, and `delete_owners`; the executed `changes+expect` request returned `:public-schema-denied`, `unexpected-fields=["changes" "expect"]`, `mutation-attempted=false`.

3. **CLOSED — string values.** [owner_hypotheses.clj:156](src/clj_surgeon/owner_hypotheses.clj:156) preserved reader values for multiline strings containing backslashes, quotes, `\u03bb`, and literal `\\u0041`; the Unicode escape’s spelling normalized to `λ`, but its value did not change.

4. **CLOSED except for regex — other node shapes.** [owner_hypotheses.clj:142](src/clj_surgeon/owner_hypotheses.clj:142) removed attached/internal comments and newlines; `\newline` remained a one-line character literal, reader-conditionals wrapping multiline strings became one line and round-tripped, and raw-newline keywords/symbols proved impossible because the reader tokenizes them as separate forms.

5. **CLOSED — teaching-text audit.** [run_clean_codex.sh:1906](bench/run_clean_codex.sh:1906) explicitly teaches `edits`/`delete_owners` for `edit_clojure`; [mcp-advanced.md:40](skills/clj-surgeon/references/mcp-advanced.md:40) teaches `changes` only for heavyweight `apply_clojure_changes`. No live bench prompt or skill text teaches `changes` on `edit_clojure`; remaining occurrences are negative tests, historical discussion, CLI `:change!`, or explicit apply-tool guidance.

6. **Independent, pre-existing — handler identity assertion.** [mcp_candidate_catalog_test.clj:201](dev/experiments/clj_surgeon/experiments/mcp_candidate_catalog_test.clj:201) expects the split `edit_clojure` to retain the broad apply handler, while the catalog intentionally derives it from the compact tool. Both facts already existed at `49f6e12`; current witness is `handle-apply-clojure-changes` versus `handle-edit-clojure`. No standard Make/merge gate reaches this namespace; only its standalone identity suite prescribed at [2026-08-27-mcp-mutation-tool-naming-options.md:286](docs/plans/2026-08-27-mcp-mutation-tool-naming-options.md:286) does.

7. **Baseline integration required.** [agent_routing_test.clj:106](test/clj_surgeon/agent_routing_test.clj:106) accounts for all five fast-suite failures, while the MCP failure is the known Homebrew-path assertion. This branch must absorb `origin/main`—where the routing-document and kondo fixes made baseline green—before the mayor reruns merge gates.