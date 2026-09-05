# GO-WITH-FIX

1. **(A) Finding 11 — OPEN.** The named round-4 omissions are closed, and no
   matrix-required field is missing from any of the five production-mapped
   faces. Remaining permissiveness: refusal pins `committed=false` but does not
   require the field; all terminal faces require `source_file` but do not pin
   its production invariant `1`; `verification`, `closure`, `partition`, and
   `planned_partition` may be `{}` although their production mappers always
   emit their named subfields; and a terminal receipt may omit both
   `details_path` and `details_unavailable` although the public terminal path
   always publishes exactly one disposition.
2. **(B) Public pre-write refusals — CLOSED.** Both scope probes, an unadmitted
   profile, and the uninitialized server were invoked through the registered
   `:full` profile's `:tool-fn` and callback. Every `structuredContent` carried
   `operation="helper_extraction"`, `mutation_attempted=false`,
   `write_authority=false`, `source_unchanged=true`, `committed=false`, and
   explicit `next_call=nil`; each matched exactly the refusal branch; every
   fixture path and byte was unchanged.

Independent delta-only FENCE REVIEW round 5 of candidate
`60d6e0e267bddd3c9698512827c870fd2e24c218`.

Networknt JSON Schema Validator 2.0.0 under Draft 2020-12 accepted every
production-mapped face against exactly its named branch. Python-jsonschema
4.19.2's Draft 2020-12 validator produced no false green when removing declared required fields,
contradicting constants, removing required `restoration_read_back` /
`recovery_required` subkeys, or contradicting `manifest_in="details_path"`.
The same validator confirmed the remaining counterexamples: refusal without
`committed`, terminal `source_file=0`, empty nested objects named above, and a
committed face with neither detail-disposition field all still validate.

The directory-name request `scope.paths=["src","test"]` refused with the
planner's nonempty `files_outside_scope`, `admitted_roots=["src","test"]`, and
decision. The separate `scope.paths=["src/**"]` fixture reported exactly the
out-of-scope caller `test/acid/app/o01_test.clj` with the same evidence. The
unadmitted-profile and uninitialized calls traversed the same registered public
path and validated only as refusals.

Exactly two constrained JVMs were launched under
`/var/tmp/forge/helper-fence-fx` with `taskset -c 6-9 nice -n 10`; the first
stopped during reviewer validator setup, and the second completed all public
calls and checks before a reviewer-only filename oracle typo stopped the final
report print. No MCP server was started and no port was contacted. The fixture
directory was removed after this verdict.
