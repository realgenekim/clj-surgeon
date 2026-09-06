# Astra — existing-binding reuse, read-only D1 investigation
Subject: root 79ef9a9f6e5f3b8cb7b738d20f72f41e54347bb2; no calls executed, no source changed.
Verdict: deleting 18 bindings was unnecessary. An existing public exact-edit route can retain them.
The compact relation has a real, intentional add-only limitation; that is not a missing overall editing capability.

## Bound D1 evidence
Rollout: /home/forge/.codex/sessions/2026/09/06/rollout-2026-09-06T10-25-29-01a07640-6e21-7b50-a3bf-c4dcc16c6d65.jsonl
Bound SHA256: fca781bdceca5fdac25fa75a36f2afb312d8de2c30144952ac08d02029fbc947.
49/51: migration files were objects, not [file, rows] tuples; 55/56 corrected tuples but mismatched require file vector.
60/61: matched vectors then hit “Target namespace is already required.” This passed grammar and failed lowering.
76/77: symbol_migration alone refused missing require_change; 95/96: remove/add identical target alias refused.
83/84: alias_migration rejected verify="none"; 88/90 without it refused unsupported-binding-scope in article/latest.
106: native deletion of 18 bindings; 109 and 116: two successful compact commits; 119: native restoration.
These are bound actor calls, not inferred box events. No active arm was inspected.

## Was the exact error invisible?
No, for D1. Every relevant call used text(await tools...(...)), serializing the entire returned object.
Actual model-facing custom_tool_call_output lines 53/58/63/79/98 contain structuredContent.error verbatim.
They expose respectively tuple shape, ordered-vector mismatch, existing target, required pair, forbidden target removal.
The content text renderer only says generic scope/count correction; that is a genuine presentation weakness.
But making text include the structured error would not reveal information this actor lacked.
Its next requests already react to those exact errors. Do not attribute the workaround to invisible diagnostics.

## Existing public route (schema-valid, source-grounded; NOT executed)
Pass this object to apply_clojure_changes on a fresh seed with the forwarding helper already supplied:
```json
{"workspace_root":"/absolute/fresh-fixture","edits":[
 {"file":"src/maven/article.clj","within":{"form":"latest"},"from":"jdbc/execute!","to":"db/execute!","matches":1},
 {"file":"src/maven/article.clj","within":{"form":"exact-search"},"from":"jdbc/execute!","to":"db/execute!","matches":1},
 {"file":"src/maven/article.clj","within":{"form":"corpus-stats"},"from":"jdbc/execute!","to":"db/execute!","matches":1},
 {"file":"src/maven/article.clj","within":{"form":"candidate-rows"},"from":"jdbc/execute!","to":"db/execute!","matches":1},
 {"file":"src/maven/migrate.clj","within":{"form":"run"},"from":"jdbc/execute!","to":"db/execute!","matches":2}
]}
```
This is the six-site subset D1 tried at line76; replace the placeholder with a new scratch copy, never a completed cohort workspace.
For remaining existing-binding owners, batch the supplied owner/count rows using each file's existing target alias; do not assume one alias across the repository.
Preserve both require vectors unchanged. Exact jdbc/execute! subtrees do not replace jdbc/execute-one! or other jdbc uses.
This route asks the caller to supply exact sites; it does not claim alias_migration's whole-scope binding proof.
Only pool_relaxed needs a new require. An explicit namespace-scoped whole-clause from/to edit can add its libspec
before the original final libspec, retaining old closing-delimiter placement and matching the frozen textual witness.
That clause edit and all owner edits can share one transaction; no temporary invalid namespace and no require removals.
The snippet is a valid existing entrance, not an executed success prediction for the entire task or its proof profile.

## Source and witness anchors (paths relative to root worktree)
src/clj_surgeon/mcp_schema.clj:220–265: ordinary edits, named owner, exact from/to, positive matches.
test/clj_surgeon/mcp_tool_test.clj:131 and :644: public guarded/undoable exact edits and known multiplicity.
test/clj_surgeon/mcp_contract_test.clj:129–150: namespace-location compilation.
src/clj_surgeon/mcp_compact_relations.clj:283–315: paired relations and equal ordered file vectors/target alias.
Same file :431–496 compile-require-edit: unconditionally refuses existing target lib/alias before constructing require edit.
Same file :234–242: removal may not equal target alias; :500–535: combines generated and caller literal edits.
test/clj_surgeon/mcp_compact_relations_test.clj:439–468: rows lower to ordinary edits, coexist with literal edits.
Same test :508–530: target-already-present deliberately refuses; this is not an accidental schema mismatch.
docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:87, MCP-OP-EDIT-022 explicitly requires that refusal.
Public require-change-schema at mcp_schema.clj:385–404 explains add/pair/files but omits the existing-target prohibition.

## Smallest useful improvement, if approved separately
First improve route guidance: when exact target lib/alias already exists, recommend ordinary owner-scoped edits;
state add-only precondition in compact schema. Never recommend removing live bindings to satisfy admission.
If compact reuse is still worth extending, let Phase B recognize exactly one identical existing [lib :as alias]
with no remove request, preserve the entire require clause, and emit only the symbol edits for that file.
Continue refusing conflicting aliases/libs, duplicates, unsupported clauses, and removal requests in this reuse case.
This deliberately changes EDIT-022; adjust accounting to distinguish checked require files from emitted require edits,
and test mixed absent/present files, unchanged headers/other jdbc uses, stale snapshots, conflicts, and atomic rollback.
Do not make a no-op edit to evade transaction invariants. No new DSL or relaxed binding-discovery authority is needed.
