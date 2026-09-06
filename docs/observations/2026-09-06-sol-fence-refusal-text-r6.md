## 1. Fence and apparatus

HOLD.

- Initial and final HEAD: `d6ffad413c2cee347e1cebf055b2902929197fb2`.
- All tip probes printed that SHA before and after execution.
- Trunk probes additionally recorded `6ca80334e735b533ae19c26bb8581672b23d2b60`.
- RED probe additionally recorded `37a10cd279760e658885f292c3f1996241389a98`.
- One malformed Unicode probe failed at compile time; its corrected rerun remained fenced at `d6ffad4`.
- Worktree remained clean and detached. Throwaway worktrees were removed.

## 2. Blocking counterexample

MCP-OP-EDIT-038 promises that every Unicode format mark is collapsed. A public `apply_clojure_changes` refusal containing supplementary-plane `U+E0001 LANGUAGE TAG` contradicts that contract:

```clojure
{:code-point "U+E0001"
 :unicode-type 16
 :expected-type 16
 :survives-structured true
 :survives-text true
 :text-contains-structured true}
```

The implementation iterates UTF-16 code units, so supplementary code points are split into surrogate characters before `Character/getType` runs. Neither surrogate is classified as `Character/FORMAT`. See [mcp_operation.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_operation.clj:70) and the violated requirement in [mcp-operation-contract-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:102).

Fix by iterating Unicode code points, then add at least one supplementary `Cf` public-entrance witness.

## 3. Required D1 differential

At tip `d6ffad4`, the real tool callback returned:

```text
apply_clojure_changes
  refused · invalid-compact-relation at ["symbol_migration" "files" 0] · 13.64 ms
  Each migration file must be [file, rows]
  expected: ["src/sample/review_updates.clj" [["describe-rating" "review/fmt-stars" 3]]]
```

`structuredContent.expected_shape_example` contained the identical 80-character example.

Against trunk `6ca80334`, the same request retained the structured error but the text contained neither the sentence nor an `expected:` line. Differential confirmed.

## 4. Refusal coverage and RED→GREEN evidence

- The original `37a10cd2` RED witness executed with exactly 16 failures: all 16 enumerated compact-relation admission refusals lacked their structured sentence.
- Its patch ID matches the rebased `f3e5102f` witness commit.
- At the tip, the three affected test namespaces passed: **104 tests, 1,586 assertions, 0 failures, 0 errors**.
- This includes all 16 compact-admission refusals and two refusal shapes for each of the nine advertised public verbs.
- No `text ⊇ structured.error` counterexample was found; the Unicode counterexample concerns the promised safe encoding.

## 5. Privacy and unchanged rendering

- The example privacy/no-source-read witnesses passed, including rejection of file contents, workspace paths, receipt paths, and request-external tokens. See [mcp_compact_relations_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/mcp_compact_relations_test.clj:1298).
- A receipt without `error` rendered byte-identically on tip and trunk: **228 bytes**, SHA-256 `c34043aeb221af3ee3592a27b056ad63f33e09c8fd15471259f04984f3fbe123`.

## 6. Lint, contract, and documentation

- `~/bin/clj-kondo`: **0 errors, 5 warnings**.
- MCP operation oracle passed.
- Contract design and EARS requirements were updated; no separate refusal-text golden exists.
- One documentation inconsistency remains: the design says file strings carrying control characters are skipped, while [valid-example-file](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_compact_relations.clj:112) accepts any nonblank string and canonicalizes it. The stale statement is at [mcp-operation-contract-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-design.md:1141).

**Verdict: HOLD** until supplementary Unicode format characters are safely encoded and the design prose agrees with the implemented canonicalization rule.

> END RECEIPT (fence-run): worktree HEAD at review exit = d6ffad413c2cee347e1cebf055b2902929197fb2 = fenced sha.
