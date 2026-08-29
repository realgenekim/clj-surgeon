# The action-emission clock fits inside the existing receipt compiler

Date: 2026-08-29

Verdict: **GO for one collector-only schema ratchet. NO-GO for history
recollection, transcript inspection, MCP changes, or a model cohort before the
ratchet passes its synthetic privacy matrix.**

## Scope and authority

This audit is based exactly on commit
`37b1d21d4a6dcea75bbaa8084bd2c89c8de70429`, tree
`59cdfdce25dc305fffa97f5bbfa4fc8b0036ffc3`, and
`docs/observations/2026-08-29-post-surgeon-boundary-decomposition.md`.

It did not recollect Codex or Claude history, advance an agent-usage marker,
read a transcript, inspect prompt or response prose, inspect source or
workspace paths, inspect hidden reasoning, launch a model, or touch a product
runtime. The existing synthetic self-test passed before this design was
written.

The exact current authorities are:

| Responsibility | Current file and pure seam |
|---|---|
| Parse one completed Codex item without prose | `skills/study-agent-usage/scripts/collect_agent_usage.py`, `completed_item_clock_sample` |
| Compile a Surgeon-complete to next-action boundary | same file, `compile_post_surgeon_boundaries` |
| Clip and union concurrent item intervals | same file, `merge_intervals`, `interval_coverage`, and `clipped_kind_coverage` |
| Build the public per-turn receipt | same file, `compile_event_clock` and `finalize_turn` |
| Parse bounded Codex `item_completed` events | same file, `analyze_codex_file` |
| Permanent synthetic privacy and clock witnesses | same file, `self_test` |
| Interpret the retained nine-second population | `dev/experiments/post_surgeon_boundary_decomposition.py`, `classify_boundary` and `summarize` |
| Public paved road and schema description | `skills/study-agent-usage/SKILL.md`; `.agents/skills/study-agent-usage` is a symlink to it |

At this base, the collector SHA-256 is
`6a83120a1b7fd8a11bc3e4fe53272125561cee73968b0a7ca210a249a7a91a5f`.
The derivative scorer SHA-256 is
`8e7851c64b390d7727ab3795cabd14dd9ea3079457c82d69546da76859dc1665`.

`bench/event_timing.clj` and the clean-agent benchmark scripts are not the
first seam. They measure purpose-built cohorts. The requested field evidence
already exists at the completed-item boundary used by the ethnography
collector; adding a second parser would create two clock laws.

## Why the seam is available

A completed structured MCP item already carries all of the private input
needed for the new evidence:

```text
item.arguments              item.result
       |                         |
       +-- safe scalars/hash ----+
                    |
completed_item_clock_sample
                    |
         compile_event_clock
                    |
     post_surgeon_boundaries
```

The compiler currently discards ordinary tool arguments and results after it
classifies the item. It retains source and target data only through bounded
counts or rehashed structural identities. The new ratchet should follow that
law: calculate safe evidence while the private value is in scope, then discard
the value before the sample enters `_clock_samples`.

No raw transcript format change is required. No server telemetry join is
required. No path needs to be canonicalized on disk. No hidden reasoning value
is read; only completed-item start and end milliseconds are used.

## Smallest pure design

Add three pure helpers beside `canonical_sha256`:

1. `canonical_json_bytes(value)` returns compact, key-sorted UTF-8 JSON bytes
   using the collector's existing canonicalization law.
2. `root_normalized_tool_arguments(arguments)` copies a top-level MCP argument
   object and replaces only its public `workspace_root` value with the literal
   `"<workspace>"`. It does not normalize nested fields, inspect strings, or
   resolve a filesystem path. This matches the existing benchmark convention
   and preserves every non-root decision byte.
3. `compile_mcp_size_evidence(item)` returns only integer byte counts and one
   domain-separated SHA-256. It never returns the argument, result, root, or
   canonical byte string.

The logical digest input should be:

```text
UTF-8("clj-surgeon.logical-tool-arguments.v1\0")
  ++ canonical_json_bytes(root_normalized_tool_arguments(arguments))
```

The domain separator prevents accidental reuse of an equal JSON digest for a
different evidence type. The digest proves equality, not secrecy. As with the
existing prompt, final-message, and structural-target digests, a party that
already knows a low-entropy candidate can test it. The receipt must not claim
that SHA-256 makes guessable input secret.

For a structured MCP action, add this optional nested evidence to the public
clock item:

```json
{
  "action_evidence": {
    "argument_canonical_bytes": 608,
    "logical_argument_sha256": "<64 lowercase hex>"
  }
}
```

For a clj-surgeon MCP action whose item contains a result, add
`result_canonical_bytes` to the same object. Count the compact canonical JSON
representation of the complete recorded result, not characters and not the
visible MCP summary. Do not add a result digest. If arguments or result are
absent, omit the corresponding field; absence must not become a synthetic
zero.

The first slice should deliberately omit CLI command and stdout evidence.
Structured MCP arguments are already parsed JSON with one canonical byte law.
CLI command/stdout byte counts would have different truncation and quoting
semantics and would make the scalar look comparable when it is not. The target
compact-relation experiment is MCP-to-MCP, so this omission does not block it.

Add one pure `compile_action_emission_evidence(source, endpoint, items,
boundary_start, boundary_end)` call inside
`compile_post_surgeon_boundaries`. It copies only the safe item evidence and
derives two clocks:

```json
{
  "action_emission": {
    "previous_surgeon_result_canonical_bytes": 8125,
    "next_argument_canonical_bytes": 608,
    "next_logical_argument_sha256": "<64 lowercase hex>",
    "last_reasoning_end_to_next_action_start_ms": 4270,
    "overlapping_background_wall_ms": 0
  }
}
```

Definitions are exact:

- `previous_surgeon_result_canonical_bytes` is copied from the completed
  source Surgeon MCP item.
- `next_*` is copied from the structured MCP endpoint. It is absent for an
  agent message, native file change, CLI, or malformed MCP item.
- `last_reasoning_end_to_next_action_start_ms` is endpoint start minus the
  latest completed `model-reasoning` item end that is after the Surgeon result
  and no later than endpoint start. If no such completed reasoning item
  exists, omit it. Do not label it thinking time or infer an item that the
  client did not record.
- `overlapping_background_wall_ms` is the union, clipped to the boundary, of
  recorded non-model actions already active across the boundary. Include
  collaboration, coordination, context compaction, shell, verification,
  semantic/native/Surgeon tools, and file changes. Exclude the source item,
  endpoint, model reasoning, model messages, human input, and unattributed
  gaps. This is a covariate and can overlap recorded reasoning; it is not a
  partition component and must not be added to the other clock segments.

The derivative scorer should continue to calculate its exclusive background
partition from public items. When `action_emission` exists, it should also
report the new direct emission interval and total overlapping-background
covariate. It must accept old receipts where the object is absent.

## Receipt compatibility

The ratchet should bump the producer identity from schema v5 to v6 because the
new digest and byte-count semantics are durable public evidence. The JSON
shape remains append-only:

- existing `event_clock.items` fields remain unchanged;
- existing `post_surgeon_boundaries` fields remain unchanged;
- `action_evidence` and `action_emission` are optional objects;
- v5 renderers and aggregate readers ignore the new objects;
- the updated derivative scorer accepts v5 by reporting the clock as
  unavailable, never as zero;
- the privacy block adds `tool_argument_content_emitted: false`,
  `tool_result_content_emitted: false`, and
  `tool_logical_arguments_hashed: true` without removing old claims.

Do not render the logical digest by default. The timeline may show argument
bytes, result bytes, emission wall, and background wall. Expose the digest only
in full JSON or an explicit equality-focused analysis, because its value is
comparison rather than human diagnosis.

## Smallest red/green matrix

All first-slice tests belong in the existing Python `self_test`; no temp source
repository or real history is required.

1. **Canonical bytes use UTF-8.** A synthetic argument containing a multibyte
   canary has the exact canonical byte count, not Python character count.
2. **Root spelling is not identity.** Two otherwise identical argument maps
   with different private `workspace_root` lengths have different raw byte
   counts and the same logical digest. Neither root occurs in serialized
   evidence.
3. **A decision byte changes identity.** Changing one non-root literal changes
   the logical digest.
4. **Private input never escapes.** Arguments contain path, source, prose,
   URL, account, request-id, and secret canaries; result contains source and
   diagnostic canaries. The serialized sample and final receipt contain none
   of them, only integer sizes and one 64-hex digest.
5. **No accidental raw source hash.** A 64-hex value inside an argument/result
   does not appear verbatim in evidence. This protects against confusing the
   logical digest with returned source-hash evidence.
6. **Result absence is not zero.** Missing result omits
   `result_canonical_bytes`; an explicit JSON null result has the canonical
   size of `null`.
7. **Emission wall uses the last completed reasoning item.** Two completed
   reasoning intervals select the later end. A reasoning item that overlaps
   endpoint start is not treated as completed-before-action.
8. **No reasoning is unknown.** A direct Surgeon-to-tool boundary omits the
   emission-wall field instead of writing zero.
9. **Background is unioned, not summed.** Two overlapping background items
   produce their clipped union. A background interval overlapping reasoning
   remains in this covariate; the existing exclusive decomposition remains
   smaller.
10. **Endpoint coverage is honest.** Structured MCP endpoints carry next
    argument evidence. Agent-message, native-patch, CLI, and malformed MCP
    endpoints do not.
11. **Old receipts remain readable.** The derivative scorer accepts a v5
    boundary without `action_emission` and reports unavailable fields.
12. **Final privacy scan remains global.** Existing fixture canaries plus all
    new argument/result canaries are absent from `json.dumps(receipt)` and the
    timeline renderer.

After those pure witnesses pass, rerun only
`make study-agent-usage-self-test`. Do not recollect history merely to prove
the implementation. A later, explicitly authorized study can create the first
v6 receipt under its normal marker law.

## Stop conditions

- **NO-GO** if implementation retains canonical bytes or normalized argument
  objects in `_clock_samples`; only the scalar/hash evidence may cross that
  boundary.
- **NO-GO** if root normalization walks arbitrary strings, resolves the
  filesystem, or removes non-root decisions.
- **NO-GO** if a missing completed reasoning item becomes zero emission wall.
- **NO-GO** if background wall is presented as model thinking or added to a
  supposedly exclusive partition.
- **NO-GO** if CLI and MCP byte counts are pooled without an explicit common
  wire law.
- **NO-GO** if the change requires history recollection, marker advancement,
  product code, MCP runtime, or a model token.

## Decision

**GO** for the collector-only v6 ratchet. The mechanism is already available
at one pure privacy boundary, and the permanent evidence can be entirely
synthetic. It will make the next compact-representation cohort answer the
causal question that the v5 receipt cannot: whether fewer logical argument
bytes actually shorten the last-reasoning-to-action interval.

**NO-GO** for claiming that the complete post-reasoning residual is action
emission. The new field measures a wall interval and correlates it with
argument size; it still includes client serialization, dispatch, scheduling,
and transport before the action-start event. A matched experiment is required
to attribute a speedup.
