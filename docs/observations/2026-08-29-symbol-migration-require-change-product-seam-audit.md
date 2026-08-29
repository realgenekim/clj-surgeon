# Product seam audit: closed symbol migration plus require change

Date: 2026-08-29

Production audit base: `d8a80f7`

Durable owner: `clj-surgeon-45j`

Scope: read-only design audit. No product code, install, reload, shared port,
model, Anvil, or running process was changed.

## Recommendation

Build one compact-entrance relation compiler that accepts the proven pair
`symbol_migration` plus `require_change`, lowers it to ordinary compact edits,
and delegates the complete result to the existing transaction. Do not add an
executor, plan handle, cache, or second canonical representation.

For the first product slice, require the two relation fields together and
require their file sets to be identical. This restriction matches the accepted
9-file evidence and makes the implementation materially smaller:

1. source-blind symbol lowering emits ordinary exact form edits for every
   declared file;
2. those ordinary edits make the existing transaction capture all 9 files
   once;
3. one composed `prepare-spec` lowers `require_change` against that exact
   frozen source map, then runs existing compact-location normalization; and
4. the unchanged generic compiler validates, compiles, parses, commits,
   reads back, receipts, verifies, and rolls back the resulting transaction.

This permits the proven B shape—relations plus one bespoke `edits` row and one
`delete_owners` group—without making `require_change` an independent generic
namespace-edit language.

```text
MCP compact request
  symbol_migration + require_change + optional edits/delete_owners
        |
        +-- source-blind closed-shape validation
        +-- symbol relation -> ordinary compact form edits
        +-- existing edit-field compiler
        +-- existing editor gesture -> direct transaction adapter
        |
        +-- existing transaction captures the declared file set once
        |
        +-- composed prepare-spec
              +-- require relation -> exact namespace from/to edits
              +-- existing compact-location normalization
        |
        `-- unchanged canonical transaction compiler and effect path
```

## Evidence boundary

The retained F/A/B cohort at `d8a80f7` proves one product hypothesis, not a
speed multiple:

- F flat: 0/2 exact;
- A file groups: 0/2 exact;
- B closed relations: 2/2 exact, 2,715 bytes, 48.912-second prompt midpoint,
  51.500-second capture-only wall midpoint; and
- F and A supplied the complete 33 edit rows and 14 deletions, but addressed all
  nine namespace rows as named forms instead of the namespace owner; those
  complete semantic decisions therefore refused `change-owner-mismatch`.

The mechanism is decision and address legibility. Product work must preserve
four explicit classes—symbol migration, require delta, exceptional edits, and
owner deletions—while deriving the exact namespace address mechanically. The
incorrect controls forbid a causal performance claim.

## Existing production functions to reuse

| Existing function | Reuse |
|---|---|
| `mcp-compact-edit-fields/normalize-edits` | Preserve the already accepted `from/to`, `old/new`, and `before/after` field algebra for explicit and generated rows. |
| `mcp-contract/editor-gestures->direct-params` | Remain the single source-blind compact-to-direct adapter after relation expansion. Extend its input preparation; do not clone its count, owner, deletion, or aggregate logic. |
| `mcp-contract/validate-tool-params` | Own runtime closure of the relation pair even if an SDK drops a top-level JSON Schema authority envelope. |
| `mcp-contract/tool-params->transaction` | Remain the only JSON-shaped direct request to canonical transaction projection. |
| `mcp-compact-location/normalize-spec` | Normalize generated and explicit compact owner locations against the same captured source map after relation lowering. |
| `intent-transaction/execute-mcp-change!` | Retain the one capture, compile, commit, receipt, and rollback path. |
| `intent-transaction/compile-transaction` | Remain the only final semantic and cardinality authority; every generated edit must pass through it. |
| `mcp-tool/resolve-transaction-paths` | Retain root confinement for all relation-declared paths after the relation has lowered to ordinary compact edits. |

There is no existing production compiler for the accepted relation pair.
Three nearby capabilities are not substitutes:

- `mcp-compact-location/normalize-spec` proves an address for an already
  complete literal edit; it does not derive symbol or require edits.
- `extract-header/direct-libspecs` is a private extraction-header parser. It
  has useful parsing semantics but no add/remove relation or compact
  transaction authority.
- `cljc.require-ops/add-require` performs platform splitting and insertion,
  does not remove an exact libspec, throws rather than returning the MCP
  refusal contract, and has different comment/reader-conditional behavior.

Thus the seam is not a duplicate relation compiler. It has parser overlap with
`extract_header`. Do not widen this first slice into a parser refactor unless a
small characterization commit first proves identical lossless behavior.

## Proven experimental behavior to port

Port the observable laws—not the namespaces—of these pure experiment forms:

- `owner-aware-symbol-migration/migration-edit`;
- `owner-aware-symbol-migration/compile-manifest`;
- `three-arm-request-shape-screen/compile-one-require-change`;
- `three-arm-request-shape-screen/compile-require-change`; and
- `three-arm-request-shape-screen/expand-closed-relations`.

The product implementation should live in one new pure namespace, proposed as
`src/clj_surgeon/mcp_compact_relations.clj`. Its public boundary should return
only ordinary compact/direct data, bounded evidence, or a typed refusal.

## Authority and stale-source laws

### Source-blind admission

1. `symbol_migration` and `require_change` must either both be absent or both
   be present. Legacy fields cannot authorize a partial pair.
2. The first slice permits only `workspace_root`, the relation pair, optional
   exact `edits`, optional `delete_owners`, and the already supported verifier
   field on the surface that owns it. It excludes `programs`, `changes`,
   extraction, basis continuations, and any second relation spelling.
3. `symbol_migration.columns` is exactly
   `["owner" "from" "matches"]`; `target_rule` is exactly
   `"preserve-name"`; and `target_alias` is one valid nonblank Clojure alias.
4. Each migration file appears once. Each site is exactly one nonblank owner,
   one qualified source symbol, and one positive count. Duplicate
   file/owner/source rows refuse.
5. `require_change` declares one exact add `{lib, as}` and one row for every
   migration file. A removal, when present, is an explicit exact `{lib, as}`;
   it is never inferred from symbol use.
6. The ordered `require_change` file set must equal the migration file set in
   the first slice. A subset, superset, duplicate, or different path refuses
   before source read.

### Frozen-source lowering

1. Every file comes from the existing transaction's one captured source map.
   The relation compiler performs no I/O.
2. Each file has exactly one direct top-level namespace and exactly one direct
   `:require` clause. Missing, duplicate, nested, or reader-conditional
   ownership refuses.
3. The require clause is comment-free and contains only supported direct
   vector libspecs. Prefix lists, platform conditionals, unsupported options,
   or ambiguous syntax refuse in the first slice.
4. The added lib/alias is absent; the alias is not bound to another lib; and an
   explicit removal resolves exactly once. Zero or several removal matches
   refuse.
5. Require changes are emitted as complete exact namespace-clause `from/to`
   edits with `within.namespace=true` and `matches=1`.
6. Symbol rows emit exact `within.form` edits. `preserve-name` changes only the
   qualifier and preserves the parsed local symbol name. An unqualified or
   malformed source symbol refuses.
7. Generated relation rows, bespoke edits, and owner deletions must be
   disjoint. Duplicate canonical addresses or overlapping exact edits refuse
   the complete request.
8. Any refusal leaves every source unchanged, publishes write authority false,
   and provides no executable partial retry. Similarity and nearest-match
   ranking never grant authority.

### Final authority

The relation compiler grants no mutation authority. The ordinary transaction
compiler revalidates every generated owner, full `from` subtree, match count,
aggregate count, future parse, and source hash. Commit-time hash checks retain
stale-source protection. Exact verification, when explicitly selected, stays
inside the existing rollback boundary.

Successful receipts may add bounded `compact_relation_normalization` evidence:
relation names, declared/generated row counts, files, and emitted edit IDs. Do
not return source bodies or imply that normalization itself wrote anything.

## Exact overlap paths

Required product/design overlap:

- `docs/high-level-design.md`
- `docs/intent/mcp-operation-contract/mcp-operation-contract-design.md`
- `docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md`
- `src/clj_surgeon/mcp_schema.clj`
- `src/clj_surgeon/mcp_contract.clj`
- `src/clj_surgeon/mcp_compact_relations.clj` (new)
- `src/clj_surgeon/mcp_tool.clj`
- `test/clj_surgeon/mcp_compact_relations_test.clj` (new)
- `test/clj_surgeon/mcp_schema_test.clj`
- `test/clj_surgeon/mcp_contract_test.clj`
- `test/clj_surgeon/mcp_tool_test.clj`

No first-slice change is required in:

- `src/clj_surgeon/intent_transaction.clj`;
- `src/clj_surgeon/mcp_compact_location.clj`;
- `src/clj_surgeon/extract_header.clj`;
- `src/clj_surgeon/cljc/require_ops.clj`;
- CLI dispatch or help; or
- extraction, semantic provider, or basis-continuation code.

The production branch currently owns these hot files. Integration must remain
with SURGEON1; the audit offers no merge or publication authority.

## Shortest Linked-Intent chain

### HLD addition

Add one subsection after compact edit-field normalization: closed compact
relations are an MCP compact-entrance representation that names a complete
symbol migration and require delta, lowers against one frozen snapshot, and
delegates to the unchanged transaction. State that it does not infer sites,
removals, target rules, or caller semantics and does not change CLI/generic
direct/extraction behavior.

### LLD addition

Add one `Closed Compact Relations` section to the MCP operation-contract
design with:

- the pipeline shown above;
- the exact paired schema and first-slice file-set equality;
- source-blind versus frozen-source responsibilities;
- the complete refusal table;
- composition order with edit-field and location normalization;
- bounded success evidence; and
- the explicit no-second-executor/no-independent-require-language decision.

### EARS additions

Use the next six compact-edit requirements:

- **MCP-OP-EDIT-020 — paired admission:** When either relation field is
  supplied, the runtime contract shall require both complete closed shapes and
  shall refuse partial, unknown, legacy-masked, or disallowed mixed routes
  before source read.
- **MCP-OP-EDIT-021 — symbol lowering:** When a valid preserve-name migration
  is supplied, the compiler shall lower every declared file/owner/qualified
  symbol/count row exactly once to ordinary form-scoped compact edits without
  reading source.
- **MCP-OP-EDIT-022 — require lowering:** When the paired file sets are exact,
  the compiler shall lower the explicit add/removal decisions against the one
  frozen source map to complete namespace-clause edits and refuse every
  missing, duplicate, conditional, commented, colliding, already-present, or
  non-unique case.
- **MCP-OP-EDIT-023 — atomic composition:** When relation, bespoke edit, and
  owner deletion rows are disjoint, the compiler shall combine them into one
  ordinary transaction and derive exact counts; any overlap or sibling failure
  shall leave the complete request source-unchanged.
- **MCP-OP-EDIT-024 — delegated authority:** Successful lowering shall pass
  through the existing compact-location and generic transaction compilers,
  commit/read-back/receipt/verification path, and shall publish only bounded
  relation evidence; lowering alone grants no write authority.
- **MCP-OP-EDIT-025 — route isolation and corpus:** Permanent witnesses shall
  replay the accepted B corpus to the exact 51-match/9-file future and prove
  that flat compact edits, generic changes, programs, basis, extraction, CLI,
  and unsupported CLJC/reader-conditional paths do not invoke the relation
  compiler.

## Smallest red-test matrix

1. **`paired-relation-schema-is-closed`** — valid complete candidate-only and
   candidate-plus-bespoke/deletion requests pass; either partial field,
   unknown key, disallowed route, or legacy masking refuses pre-source.
2. **`preserve-name-relation-lowers-exact-rows`** — the retained 23 rows lower
   to 27 declared matches with exact files, owners, `from`, generated `to`, and
   counts; table-drive invalid columns/rule/alias/symbol/count and duplicate
   row/file refusals.
3. **`require-delta-lowers-from-one-frozen-map`** — the retained 9 additions
   and 3 removals lower to nine exact namespace edits; table-drive file-set
   mismatch, missing/duplicate namespace or require, comments, conditionals,
   unsupported entries, existing target, alias collision, and zero/multiple
   removal matches.
4. **`closed-relations-compose-to-frozen-future`** — relations plus the one
   bespoke edit and 14 deletions compile through the production contract to
   exactly 51 matches, 9 files, and all nine retained future hashes.
5. **`relation-overlap-refuses-atomically`** — duplicate generated rows,
   generated-versus-bespoke overlap, or any one stale owner/count refuses the
   complete batch with byte-identical sources and no partial evidence.
6. **`mcp-relation-mutation-is-undoable-and-exact-verifiable`** — one real
   isolated transaction commits the frozen fixture, reads back every file,
   returns a receipt, passes the task's exact verifier, and undoes to byte-exact
   originals; verifier failure rolls back.
7. **`stale-source-refuses-before-write`** — mutate one captured require or
   owner guard before commit and prove the existing hash/count authority
   refuses without retry.
8. **`nonrelation-routes-never-call-lowerer`** — throwing-spy witnesses for
   flat edits, generic changes, programs, basis, extraction, CLI, `.cljc`
   conditionals, and relation-absent requests.

Keep cases 1–5 and 8 pure. Use filesystem/process boundaries only for 6–7.

## What not to import

- Do not ship `file_groups`; it was 0/2 and did not expose the missing
  decisions.
- Do not ship the experimental MCP admission shim, capture server, registry
  observer, scorer, cohort timer, decision-coverage oracle, fixture constants,
  payload budgets, or model prompts.
- Do not copy `normalized-transaction`, `compile-request`, or another wrapper
  around `intent_transaction`; production already has the canonical path.
- Do not copy the experiment's indentation-dependent string splice for require
  removal/insertion. Use lossless nodes to emit the complete guarded clause or
  refuse unsupported layout.
- Do not accept arbitrary target rules, infer removal aliases, infer sites,
  auto-select owners, use fuzzy matching, or add a second `symbol_rewrites`
  spelling.
- Do not add a standalone `require_change` language in this slice. Requiring
  the paired file set is what avoids a new source-capture mechanism or a
  transaction-engine change.
- Do not broaden to reader-conditional or platform-specific CLJC requires;
  retain a typed pre-write refusal until independent evidence earns that
  semantic surface.
- Do not expose the relation through CLI, generic `changes`, programs,
  extraction, basis continuations, or another public tool.
- Do not claim the 48.912/51.500-second observation as a speedup. Only a real
  correct mutation cohort with exact verification can establish performance.

## Go/no-go

**GO** to HLD review for this paired, file-set-equal, one-transaction seam.

**NO-GO** to product implementation before HLD, LLD, and EARS approval; to
importing the experiment wholesale; to changing the transaction engine; or to
claiming a speed result from incorrect controls.
