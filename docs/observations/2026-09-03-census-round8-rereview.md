# census-verb dae5d9c — Sol executed round-8 re-check: NO-GO (door leak CLOSED; validation still after workspace routing — a stat precedes the refusal; doors loses ordering to file-count and workspace refusals) — round 9 launched

# NO-GO

The round-seven defect is only partially closed. Invalid `doors` no longer leaks into `next_call`, but validation still occurs after filesystem-backed workspace routing, contradicting CENSUS-016/029.

1. **PARTIAL, blocking — round-seven item.** [mcp_relation_census.clj:804](src/clj_surgeon/mcp_relation_census.clj:804), [mcp_relation_census.clj:821](src/clj_surgeon/mcp_relation_census.clj:821) — wire `doors=[1]` returned `doors-not-strings`, index `0`, value `1`, and no `doors` in `next_call`; however, isolated tracing recorded `newfstatat("/tmp/census9-fx", …)` before the refusal, though neither source file was read.

2. **CLOSED — exact oversized repro.** [mcp_relation_census.clj:202](src/clj_surgeon/mcp_relation_census.clj:202) — `files=[huge.clj,small.clj] doors=[1] pool_size=1` produced the same typed refusal, not `source-too-large`; its continuation was `{tool, pool_size:8, workspace_root}` and passed the published schema.

3. **OPEN, blocking — clause ordering.** [mcp_relation_census.clj:178](src/clj_surgeon/mcp_relation_census.clj:178), [mcp_relation_census.clj:202](src/clj_surgeon/mcp_relation_census.clj:202) — malformed `doors` beats malformed `pool_size`, but 513 files beat it with `too-many-files`, and an invalid workspace beats it with `invalid-workspace-root`. Thus `doors` validation precedes neither `max_files` nor workspace routing.

4. **CLOSED for the current schema; fragile if extended.** [mcp_relation_census_test.clj:1368](test/clj_surgeon/mcp_relation_census_test.clj:1368), [mcp_relation_census.clj:51](src/clj_surgeon/mcp_relation_census.clj:51) — `schema-violations` covers every keyword presently published, and Python `jsonschema` 4.19.2 agreed on valid/invalid representative calls. It nevertheless false-accepts schema-invalid values if `additionalProperties` becomes a schema or `enum`, `pattern`, `minLength`, or `required` is introduced.

5. **CLOSED — battery witness.** [mcp_relation_census_test.clj:1591](test/clj_surgeon/mcp_relation_census_test.clj:1591) — focused rerun passed `1 test / 89 assertions / 0 failures / 0 errors`, traversing every emitted `next_call`, including the oversized-plus-bad-door shape.

6. **CLOSED — empty and whitespace doors.** [relation_census.clj:849](src/clj_surgeon/relation_census.clj:849) — both values are accepted by the structural JSON schema but semantically refused as `unknown-door-symbol` on MCP and `bb`; both replacement continuations conform to the schema.

7. **GREEN — gates.** [Makefile:175](Makefile:175) — direct MCP suite passed twice at `416/4559/0`; `test-fast` passed `716/6057/0`; operation oracle passed.

Recommended merge fix: perform schema-level type/bound validation before `workspace/resolve-request`, order `doors` before file-count and pool validation, and add instrumentation that fails on any routing/stat/read before this refusal. Port 7908 is stopped, `/tmp/census9-fx` was deleted, and the checkout is clean.