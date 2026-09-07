NO-GO for landing `01ad6719` on `MCP/main`.

### Blocking defects

1. **`next_call` is neither faithful nor safely serializable.**  
   Public execution keywordizes the request and removes `workspace_root` ([mcp_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_tool.clj:960)); the guard then adds a string-keyed `"expect"` ([mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:647)). Executed output therefore contained both `:expect` and `"expect"` and serialized as:

   ```json
   {"expect":{"changes":1,"edits":2,"files":1},
    "expect":{"changes":1,"edits":1,"files":1}}
   ```

   `workspace_root` was absent. JSON round-trip equality failed on both editor and direct routes.

2. **The published schema contradicts the new contract.**  
   [mcp_schema.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:526) still requires `expect` with `changes`, while [line 542](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:542) forbids it with editor gestures. Its route list also omits `create_files`. Executed schema admission showed:

   - `edits + expect`: denied
   - `edits` without expect: accepted
   - `changes + expect`: accepted
   - `changes` without expect: denied
   - create-only, with or without expect: denied

   Thus “guard on both routes” and “expect omitted = no guard” are not true at the MCP boundary.

3. **Create-only recovery is impossible.**  
   The implementation derives counts solely from lowered changes ([mcp_contract.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_contract.clj:952)), yielding zeroes for create-only requests. The probe refused before writing, but returned corrected zero counts; replay then refused `positive-integer`, consistent with the schema minimum of one ([mcp_schema.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:211)).

4. **Linked intent remains contradictory.**  
   The governing EARS requirements still mandate ignoring disagreement and describe aggregate expectations as bookkeeping ([mcp-operation-contract-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:62)). The branch changed code and tests without updating that mandatory intent chain.

### Requested probes

| Probe | Verdict | Result |
|---|---|---|
| (a) files match, edits mismatch | **NOT-REPRODUCED** | Both editor and direct routes returned `invalid-mcp-request / expect-mismatch`, named only `edits`, preserved bytes, and created no receipt or transaction entries. |
| (b) create-only + expect | **CONFIRMED-DEFECT** | Refuses against `{0,0,0}`, but its `next_call` cannot pass the positive-integer validator; the public schema rejects create-only entirely. |
| (c) `changes: 0` | **JUDGMENT** | Confirmed `positive-integer`. This aligns with the already-published minimum and no live caller dependency was found, but it is an undisclosed behavior change lacking a focused witness. |
| (d) `next_call` fidelity | **CONFIRMED-DEFECT** | Duplicate serialized `expect` keys, missing `workspace_root`, failed exact-shape round trip; editor replay shape is schema-denied. |
| (e) silently dropped declared field | **NOT-REPRODUCED** | Internal direct/editor matching, omitted, create, and compact-relation probes found refusal or preservation, not silent dropping. The schema-denial defects above are distinct and worse. |
| (f) old behavior load-bearing | **NOT-REPRODUCED** | No executable caller, routing plate, or installed skill relied on mismatched aggregate counts; plates generally omit aggregate `expect`. The normative EARS documents do still require the old behavior. |
| (g) text ⊇ structured | **NOT-REPRODUCED** | Visible text included `expect-mismatch`, the sentence, every mismatching field/value, corrected aggregate, and source-unchanged statement. |

### Gates

- `make mcp-test`: **PASS** — 729 tests, 8,986 assertions, 0 failures/errors.
- `~/bin/clj-kondo --lint <changed files>`: 0 errors, 1 warning, exit 2. The warning is an untouched, pre-existing unused binding at `lane_manifest_test.clj:211`; one informational finding was also pre-existing.
- Merge-tree against current `origin/MCP/main` (`847e8c9e`): clean.
- Worktree remains clean; temporary `/var/tmp` probes were removed.

I followed the repository-local clj-surgeon skill’s one-attempt fallback and `/var/tmp` isolation rules during the executed review.

> END RECEIPT (fence-run): worktree HEAD at review exit = 01ad6719f427617db358f823745453c671be263f = fenced sha.
