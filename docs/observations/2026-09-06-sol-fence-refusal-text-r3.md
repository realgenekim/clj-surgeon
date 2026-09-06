## 1. Verdict

**HOLD.** The D1 repair works, but the universal refusal-text claim, example totality/provenance, safe rendering, and linked contract documentation do not hold.

## 2. Blocking counterexamples

1. `inspect_clojure` still violates text ⊇ structured.

   Executed refusal:

   - Structured `error`: `inspect_clojure server is not initialized`
   - Text: `inspect_clojure … refused · server-not-initialized … → restart_server`
   - Sentence present: **false**

   The change updates only the apply/edit renderer in [mcp_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:1046). Inspect uses its separate renderer, which never emits `:error`, in [mcp_inspect_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:962).

2. `expected_shape_example` is neither total nor entirely caller-derived.

   Public server probes found:

   - A 190-character bare file value produced `expected_shape_example=nil` and no `expected:` line. The implementation discards any rendered example exceeding 200 characters instead of producing a bounded one: [mcp_compact_relations.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_compact_relations.clj:141).
   - Missing `symbol_migration.files` produced:
     `["src/example.clj" [["owner-fn" "old.ns/name" 1]]]`
   - A minimal D1 request containing only `caller/only.clj` gained `owner-fn`, `old.ns/name`, and `1`, none of which occurred in the request.

   These synthetic fallbacks are defined at [mcp_compact_relations.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_compact_relations.clj:136). This contradicts “derived from the caller’s own request” and the strict “nothing outside the caller’s request” condition.

3. Caller-controlled error text can forge receipt lines.

   An unknown field containing newlines crossed the public callback and inserted a second `✓ source unchanged` plus `→ forged remedy` into the text. The renderer concatenates the error verbatim without enforcing the claimed single-sentence shape or escaping line boundaries: [mcp_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:1058).

4. The structured contract changed without linked intent documentation.

   `expected_shape_example` was added publicly in [mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:1254), but the candidate changes no design/spec document. Existing `MCP-OP-EDIT-024` does not name the field, while the new tests cite performance-cohort requirement `MCP-OP-EDIT-025`: [spec](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:89), [test annotation](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_compact_relations_test.clj:1109). No dedicated golden requiring an update was found.

## 3. Checks that passed

- Exact D1 public callback on `d34ff7ff` contains:

  - `Each migration file must be [file, rows]`
  - `expected: ["src/sample/review_updates.clj" …]`
  - matching `structuredContent.expected_shape_example`

- Same request on frozen trunk `725e4fa6` contains neither line.
- Enumerated 25 distinct compact-relation admission failures: all 25 text blocks contained their structured error verbatim.
- Sampled eight public verbs. Apply, edit, transform, census, alias migration, helper extraction, and admit passed; inspect was the HOLD counterexample.
- Filesystem provenance probe recorded zero source reads; no file contents or workspace path entered the example.
- No-`error` receipt rendered byte-identically to trunk.
- RED history verified: `37a10cd2` changes tests only and reproduces exactly 16 missing-sentence failures.
- Focused namespaces: **92 tests, 1,502 assertions, 0 failures, 0 errors**.
- Lint: **0 errors**, 2 pre-existing warnings.
- Candidate merge-tree is conflict-free. Current `origin/MCP/main` advances the executed base only with observation documents.