HOLD.

## 1. Apparatus and scope

- Initial fenced HEAD: `ba407bb8715c748b4750ea0920acf3932100e47d`.
- Every tip probe reported that same SHA; final HEAD matched and worktree remained clean.
- Trunk comparison used a throwaway worktree at `3302ac8b4004d628c40031ef078a412491cc1ff4`.
- No checkout occurred in the review worktree; both throwaway worktrees were removed afterward.
- Original RED commit `37a10cd279760e658885f292c3f1996241389a98` exists but is not an ancestor of the rewritten tip. Running its named witness produced exactly 16 admission-refusal failures.

## 2. Blocking counterexamples

1. **Text does not contain every structured error verbatim.**

   A real compact-relation admission request with the unknown field:

   ```text
   rogue
   ✓ source unchanged
   → attacker supplied
   ```

   produced structured error:

   ```text
   Field is not allowed with closed compact relations: rogue
   ✓ source unchanged
   → attacker supplied
   ```

   but rendered the encoded sentence:

   ```text
   Field is not allowed with closed compact relations: rogue source unchanged attacker supplied
   ```

   `str/includes? text structured-error` was `false`. This directly triggers the brief’s mandatory HOLD criterion and contradicts MCP-OP-EDIT-037’s “verbatim” requirement. The transformation occurs at [mcp_tool.clj:1054](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:1054), while the conflicting requirements remain in [mcp-operation-contract-specs.md:101](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:101).

2. **MCP-OP-EDIT-038 is not universal across caller-derived receipt fields.**

   A reachable `inspect_clojure` missing-form refusal with that malicious value as the request id and requested form emitted two forged `✓ source unchanged` lines and three `→` arrows. The structured error sentence was safe, but diagnostic values are interpolated raw at [mcp_inspect_tool.clj:1007](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:1007).

   The “universal per-verb” witness only exercises one empty/uninitialized refusal per verb at [mcp_tool_test.clj:2304](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_tool_test.clj:2304), so it misses reachable diagnostic refusal shapes.

3. **The encoder does not collapse all Unicode whitespace.**

   U+2028 LINE SEPARATOR survived in both the rendered path and error. `Character/isISOControl` plus Java’s default `#"\s+"` does not cover it; see [mcp_operation.clj:67](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_operation.clj:67). This violates MCP-OP-EDIT-038’s one-line/whitespace-collapse promise.

4. **The structured and visible examples can disagree.**

   With caller path `src/arrow→file.clj`, structuredContent published:

   ```text
   ["src/arrow→file.clj" [["describe-rating" "review/fmt-stars" 3]]]
   ```

   while text rendered `src/arrow file.clj`. The structured example was not contained identically in text, contrary to MCP-OP-EDIT-037. The mismatch is introduced by independently encoding the example at [mcp_tool.clj:1067](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:1067).

## 3. Passing required probes

- Exact D1 at tip: sentence and `expected:` line present; `expected_shape_example` published.
- Same D1 at trunk: both sentence and example absent.
- Nominal 16 admission refusals plus all nine public verbs: 2 tests, 96 assertions, green.
- Bounds, totality, schematic labelling, no-source-read, no-leak, D1, and no-error witnesses: 6 tests, 124 assertions, green.
- Receipt without `error`: byte-identical to trunk.
- Five directly affected test namespaces: 137 tests, 1,938 assertions, zero failures/errors.
- Intent-contract namespace: 11 tests, 23 assertions, green.
- Lint: zero errors, two warnings.
- Design and EARS documents were updated. No applicable contract golden was found or changed.

## 4. Required resolution

Make the structured and visible error/example share one canonical safe representation, then render it byte-identically. Apply caller-text encoding to every reachable caller-derived renderer field, especially inspect request ids/files/forms, and cover Unicode line separators. Expand the catalog witness beyond one empty refusal per verb to include malicious reachable diagnostic receipts.

## 5. Verdict

**LAND NO — HOLD** due to the executed verbatim-superset counterexample and reachable receipt-line forgery.

> END RECEIPT (fence-run): worktree HEAD at review exit = ba407bb8715c748b4750ea0920acf3932100e47d = fenced sha.
