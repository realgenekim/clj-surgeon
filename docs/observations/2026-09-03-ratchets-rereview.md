# receipt-ratchets 49f6e12 — Sol executed re-review (round 2 → GO-WITH-FIX, 2 items; round 3 fix launched)

GO-WITH-FIX. Seven prior items are closed; dispatch presentation is only partial. One additional regression affects the active candidate-catalog entrance.

| # | Status | Executed result |
|---|---|---|
| 1 | CLOSED | Preorder spans at [intent_transaction.clj:1503](src/clj_surgeon/intent_transaction.clj:1503) distinguish two `(f _)` sites on one line: one edit produced `addressed=1`, `unaddressed=1`. Containment works both ways; an edit lacking preorder data conservatively reported the site unaddressed. |
| 2 | PARTIAL | The prescribed 60-arm/comment fixture passes the 2,048-character budget with no `;;` or newline, via [owner_hypotheses.clj:163](src/clj_surgeon/owner_hypotheses.clj:163). However, a valid multiline string dispatch remains multiline: witness returned `contains-newline=true`, `joined-line-count=2`, violating MCP-OP-DISPATCH-004. |
| 3 | CLOSED | Both `"MINIMAL"` and omission reach `invalid-require-policy` through [mcp_contract.clj:499](src/clj_surgeon/mcp_contract.clj:499) and [mcp_extraction.clj:107](src/clj_surgeon/mcp_extraction.clj:107), with `field`, `accepted`, and “required and is never defaulted.” |
| 4 | CLOSED | [outline.clj:150](src/clj_surgeon/outline.clj:150) skips discards and unwraps metadata. `#_skipped`, nested metadata, multiline values, and reader conditionals all round-tripped through the published owner form and selected the intended arm without throwing. |
| 5 | CLOSED | [mcp_schema.clj:491](src/clj_surgeon/mcp_schema.clj:491) rejects `{edits, expect_matched}` and extraction-plus-basis while accepting direct `{changes, expect, expect_matched}`. Production `edit_clojure` and `apply_clojure_changes` agree with their schemas for `changes`. |
| 6 | CLOSED | [mcp_inspect.clj:573](src/clj_surgeon/mcp_inspect.clj:573) inspects parsed data: `(f "a _ b")` had no note; `[a,_]` had the wildcard note. |
| 7 | CLOSED | [mcp_inspect_contract_test.clj:634](test/clj_surgeon/mcp_inspect_contract_test.clj:634) pins every minimal example to the live validator. Count-too-low and malformed-pattern public-tool tests prove byte-identical pre-write refusal at [mcp_tool_test.clj:2200](test/clj_surgeon/mcp_tool_test.clj:2200). |
| 8 | CLOSED | Singular wording, zero-match wording, one-time shape computation, unreadable-source classification, and project-relative `path-facts` are all corrected at [mcp_inspect_tool.clj:942](src/clj_surgeon/mcp_inspect_tool.clj:942), [mcp_tool.clj:947](src/clj_surgeon/mcp_tool.clj:947), [mcp_inspect.clj:107](src/clj_surgeon/mcp_inspect.clj:107), and [intent_transaction.clj:1596](src/clj_surgeon/intent_transaction.clj:1596). |

Contract-tightening audit:

- Production `edit_clojure` refusal is typed (`invalid-mcp-request`), names `unexpected_fields`, and proves `source_unchanged=true`, `mutation_attempted=false`, `write_authority=false` at [mcp_tool.clj:1199](src/clj_surgeon/mcp_tool.clj:1199). The same request was accepted and committed through `apply_clojure_changes`.
- No skill, production caller, or recorded JSON/JSONL request sends `changes` to production `edit_clojure`; repository guidance uses `edits` at [CLAUDE.md:277](CLAUDE.md:277).
- The active candidate catalog is the exception: [mcp_candidate_catalog.clj:394](dev/experiments/clj_surgeon/experiments/mcp_candidate_catalog.clj:394) advertises `changes` on its projected `edit_clojure` while retaining the compact handler. It is launched by [run_clean_codex.sh:1737](bench/run_clean_codex.sh:1737). Executed witness: `schema-ok=true`, handler refusal `unexpected=[changes expect]`.
- Invalid-policy fuzzing covered omission, uppercase, empty string, whitespace, integer, boolean, map, and vector. All refused through the named path; only exact `"minimal"` and `"copy-all"` passed.
- Inserted text is not rematched: inserting a new `(f 9)` left the frozen pre-image match count at one. An insertion’s existing anchor counts as addressed; edits without preorder addresses over-report unaddressed, never under-report.

Verification:

- Ratchet witnesses: 14 tests, 99 assertions, green.
- Seven affected namespaces: 200 tests, 1,780 assertions, green.
- Full MCP suite: 400 tests, 4,138 assertions; only the documented host-specific `/opt/homebrew/bin/clj-kondo` expectation failed.
- Worktree remains clean.

Verdict: **GO-WITH-FIX** before entering the mayor’s merge queue.

1. [owner_hypotheses.clj:163](src/clj_surgeon/owner_hypotheses.clj:163) — A dispatch string containing a literal newline survives presentation unchanged and breaks the promised one-line vocabulary; add reader-safe newline escaping plus a round-trip witness.
2. [mcp_candidate_catalog.clj:401](dev/experiments/clj_surgeon/experiments/mcp_candidate_catalog.clj:401) — Candidate `edit_clojure` advertises `changes` but now refuses them; either retain the apply handler for that widened candidate or remove the explicit-change branch and update its benchmark contract.