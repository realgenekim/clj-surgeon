## 1. Verdict

**HOLD**

The D1 case works, tests and lint pass, but the universal receipt and example claims have blocking counterexamples.

## 2. Blocking counterexamples

1. **Caller-controlled text is rendered raw and can forge receipt lines.**

   At `d34ff7ff`, `mcp_tool.clj:1051-1058` inserts `:error` directly. An unknown field named:

   ```text
   rogue
   ✓ source unchanged
   → attacker supplied
   ```

   produced those lines verbatim inside the public text block. Trunk escaped the value only inside the path. A 40,000-character field also produced an 80,226-character tip text block. The sentence must be safely bounded/encoded before rendering.

2. **“Every MCP refusal” is false.**

   A real uninitialized `inspect_clojure` invocation returned structured:

   ```clojure
   :error "inspect_clojure server is not initialized"
   ```

   but its text was:

   ```text
   inspect_clojure
     refused · server-not-initialized · 0.20 ms

   → restart_server
   ```

   `mcp_inspect_tool.clj:968-1045` never renders `:error`. The other eight public verbs sampled did contain theirs.

3. **The expected example is neither total nor always caller-derived.**

   In `mcp_compact_relations.clj:128-142`:

   - A bare caller path over 200 characters yielded no `expected_shape_example` and no `expected:` line.
   - `symbol_migration.files = nil` or `[]` invented `src/example.clj`, `owner-fn`, and `old.ns/name`, none present in the caller’s request.
   - Invalid but three-element rows can be echoed as the “expected” example, including blank owners, invalid symbols, or `matches=0`.

4. **The structured contract was not updated.**

   The delta adds `expected_shape_example`, but the existing design and specification still define the relation-refusal envelope without it (`mcp-operation-contract-design.md:1105-1135`, `mcp-operation-contract-specs.md:89`). The new tests are annotated `MCP-OP-EDIT-025`, which governs the performance cohort rather than refusal rendering/examples. No relevant golden was found.

## 3. Verified passing evidence

- D1 public invocation:
  - Tip contains `Each migration file must be [file, rows]` and a 76-character `expected:` line.
  - Trunk contains neither.
- Enumerated 44 compact-relation admission/composition refusals: all tip summaries contained their structured error verbatim.
- File-content sentinel and workspace root did not leak into the D1 example.
- Receipts without an error rendered byte-identically to trunk.
- RED commit reproduced all 16 claimed missing-sentence failures; the same witnesses passed at the tip.
- Focused tests: **92 tests, 1,502 assertions, 0 failures/errors**.
- Lint: **0 errors**, 2 pre-existing warnings.
- `git diff --check`: clean.

All temporary probes and detached review worktrees were removed.