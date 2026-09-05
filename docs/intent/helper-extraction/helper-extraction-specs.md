---
parent: helper-extraction-design
prefix: MCP-OP-HELPER
---

# #Selected-helper Closure Extraction Specifications

Stable intent registry for the `helper_extraction` leaf. IDs are never reused. `[ ]` = active gap
awaiting its witness; registration lands in the same change as the RED witnesses.

# #Request and Registration

- [ ] **MCP-OP-HELPER-001**: When the clj-surgeon MCP server starts in the full tool profile, it shall advertise `helper_extraction` as a public top-level tool whose input schema is the closed field set `op`, `workspace_root`, `from`, `helpers`, `to`, `scope`, `verification`, `expect`.
- [ ] **MCP-OP-HELPER-002**: The `helper_extraction` request shall carry no per-file, per-owner, or per-site table, and shall refuse any unknown field, so that its size is constant in the number of callers.
- [ ] **MCP-OP-HELPER-017**: `expect.caller_files` shall be optional; when absent the request is accepted, and when supplied it shall be a strict guard.

# #Owners and Dependencies

- [ ] **MCP-OP-HELPER-003**: When executing, clj-surgeon shall resolve each name in `helpers` to exactly one top-level owner in `from.file`, and shall refuse with `helper-extraction-ambiguous-owner` naming every owner found otherwise.
- [ ] **MCP-OP-HELPER-004**: If a selected owner references a retained private var of the source, then clj-surgeon shall refuse with `helper-extraction-private-dependency` naming the var.
- [ ] **MCP-OP-HELPER-019**: If a selected owner references a retained public var of the source, in head position, as a first-class value, or in an admitted def initializer, then clj-surgeon shall refuse with `helper-extraction-retained-dependency`, so that the destination never requires the source; universal absence of load cycles is not claimed, and a valid-original fixture that would create source→third→target→source shall refuse.
- [ ] **MCP-OP-HELPER-018**: If a selected owner's body contains a namespace-sensitive form (`::kw`, `::alias/kw`, syntax-quote, `*ns*`), then clj-surgeon shall refuse with `helper-extraction-namespace-sensitive-body` (explicit v1 refusal; faithful rewriting is a future extension with its own witness).

# #Discovery and Partition

- [ ] **MCP-OP-HELPER-005**: clj-surgeon shall discover every supported reference to a selected owner under all admitted roots (`src`, `test`, `.clj-surgeon.edn :source-roots`) under every spelling a file binds, byte-faithful to the filesystem it walks.
- [ ] **MCP-OP-HELPER-014**: Fully qualified uses of a selected owner, with or without a require of the source namespace, shall be discovered and rewritten as sites; for an admitted static caller with no require, clj-surgeon shall add one require of the destination so the rewritten symbol has a sound load path, and shall refuse with `helper-extraction-unsupported-binding` where load semantics cannot be established.
- [ ] **MCP-OP-HELPER-006**: clj-surgeon shall partition callers into `moved-only`, `mixed`, `qualified-only`, and `untouched`, shall retain every non-selected use unchanged, and shall never replace a whole library require in a mixed caller.
- [ ] **MCP-OP-HELPER-021**: If a supported reference to a selected owner exists outside `scope.paths`, then clj-surgeon shall refuse with `helper-extraction-caller-outside-scope`, so that definitions are never retired with a caller left behind.
- [ ] **MCP-OP-HELPER-015**: Source-local uses of a selected owner by retained source functions shall be lowered by the extraction machinery's own source rewrite against one immutable snapshot, and the source file shall be counted once in the footprint.
- [ ] **MCP-OP-HELPER-007**: When choosing a caller's alias, clj-surgeon shall select the first `alias_policy` entry colliding with nothing bound in that file, and shall refuse with `helper-extraction-alias-policy-exhausted` otherwise.
- [ ] **MCP-OP-HELPER-013**: If `expect.caller_files` is supplied and differs from the derived count of EXTERNAL caller files (the source is not a caller), then clj-surgeon shall refuse with `helper-extraction-expect-mismatch` reporting both counts under that definition.

- [ ] **MCP-OP-HELPER-023**: If a bare symbol could resolve to a selected owner through two required namespaces, then clj-surgeon shall refuse with `helper-extraction-ambiguous-reference` naming the file, symbol and candidates.
- [ ] **MCP-OP-HELPER-024**: If `to.lib` is already defined or its path is occupied, then clj-surgeon shall refuse with `helper-extraction-target-exists` naming the path.
- [ ] **MCP-OP-HELPER-025**: A request carrying any field outside the closed set shall refuse with `helper-extraction-unknown-field` listing `unknown_fields`.

# #Transaction and Proof

- [ ] **MCP-OP-HELPER-008**: The write shall be one transaction through the shared kernel entrance; on any refusal before staging no byte of any file changes.
- [ ] **MCP-OP-HELPER-011**: clj-surgeon shall validate that the named verification profile is synchronous, rollback-capable, and runnable before writing, and shall refuse with `helper-extraction-verification-preflight-unavailable` otherwise, with nothing staged.
- [ ] **MCP-OP-HELPER-020**: The terminal states `committed`, `verification-failed`, `verification-timeout`, and `rollback-failed` shall be distinct; after a handled failure every protected byte and mode shall be restored and the destination removed with `restored true`; a failed restoration shall report `source_unchanged false` with recovery-required evidence and shall never claim unchanged.
- [ ] **MCP-OP-HELPER-022**: The proof shall run in a fresh process, and the receipt shall report the executed profile and its typed checks (`structural_callers`, `helper_behaviors`, `compiled_callers`) and never an ambiguous coverage count, so that a warm namespace with stale vars cannot produce a false proof and an unexecuted check is never implied.

# #Receipt and Continuations

- [ ] **MCP-OP-HELPER-009**: The receipt shall contain counts and histograms only, never a file list — `caller_files` (external callers), `source_file`, `changed_files` (callers + source + destination), `sites`, `retained_sites`, the partition map and the alias histogram; per-caller detail shall be written to `details_path` in the kernel's local-state receipt directory, never inside the workspace, and the pure planner shall carry no `details_path` of its own.
- [ ] **MCP-OP-HELPER-010**: A refusal shall carry `next_call` only when a schema-valid, scope-preserving, non-identical continuation is mechanically known; otherwise it shall carry bounded evidence, the one unresolved decision, and `next_call null`.
- [ ] **MCP-OP-HELPER-016**: No refusal shall offer scope narrowing, caller exclusion, an invented alias or destination, or a weaker verification profile as a continuation.
- [ ] **MCP-OP-HELPER-012**: The receipt's `closure` field shall state the roots and the grammar over which closure is exact and shall state that dynamic references are not claimed.
