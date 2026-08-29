# Extraction-only tool surface: capture-screen protocol

Date: 2026-08-29

Status: **GO for one capture-only F–X–X–F model cohort after its client-surface
preflight; NO-GO for product integration or a mutation cohort.**

## Question

The frozen 15-owner extraction spends about 13.2 seconds before the first
`apply_clojure_changes` call. The previous literal-object treatment showed that
constructing the 608-byte logical argument costs about 2.2 seconds. Roughly 11
seconds remain in the fused model, service, prompt-ingestion, inference, and
transport interval.

Does the full union surface contribute materially to that interval? The
current tool asks the caller to ingest exact edits, basis-backed decisions,
heterogeneous changes, extraction, formatting, verification, rollback, cold
jobs, and terminal relay even when the task can only be an extraction.

This protocol compares that surface with one extraction-only projection. It
does not change the tool name, request object, kernel, prompt, peer tools, or
response.

## Why the hypothesis is plausible

The retained Codex registry receipt for the current control contains this
client-visible `apply_clojure_changes` surface:

| Surface component | Full control | Extraction-only | Reduction |
|---|---:|---:|---:|
| Description | 4,189 bytes | 424 bytes | 89.9% |
| Input schema | 18,640 bytes | 7,512 bytes | 59.7% |
| Complete tool projection | 23,096 bytes | 8,203 bytes | 64.5% |

The extraction-only schema is not new product machinery. It is the existing
closed `mcp-schema/extraction-schema`. The 424-byte description is the retained
strict extraction description from catalog U. The public name remains
`apply_clojure_changes`, which won the prior name-only tournament. Thus the
treatment removes unrelated ontology without paying a novel-name migration
cost.

Catalog U also provides useful prior evidence: the same familiar name and
strict extraction-only surface was 8/8 semantically correct and one-shot in
the earlier naming cohort. That experiment did not compare narrow against the
full union and cannot answer this question, but it makes a four-call
capture-only screen proportionate.

## Frozen evidence

The screen reuses the retained product-control identity from the literal-relay
experiment:

- candidate `51efa0591fc4083fa875a4754b7d56d88497b26b`;
- model `gpt-5.6-sol`, reasoning `high`;
- prompt 2,097 bytes, SHA-256
  `70ab6dc2ad07b04dc3930c277f8eac7185a19a5bb21c2a619c2f6bf0db6742a0`;
- root-normalized argument SHA-256
  `01d502300c9e6af22e22e69f5680a4ed767ecc7fa64e4c9bce1d91b78bdfba47`;
- control pre-first-call midpoint 13.238 seconds;
- control complete-wall midpoint 19.459 seconds.

The exact full-surface hashes come from the retained Codex registry, not from
an internal Clojure map. The treatment's advertised surface is frozen in
`dev/experiments/extraction_tool_surface_screen.edn`; a real Codex registry
preflight must reproduce it before any model token is spent.

## No-model result

The retained exact extraction call passed both the full public schema and the
closed extraction schema. Both surfaces pass the identical argument object to
the current runtime contract; the treatment performs no lowering or inference.
The normalized request contains:

- operation `extraction`;
- 15 ordered forms;
- one public form;
- zero caller changes;
- zero ignored caller files;
- `verify=exact`.

The real pure extraction compiler produced two future files and these ordered
SHA-256 values:

```text
6ed498052c8a30531047b1d1c9bd23c609bc32355403e8412b7cfda178a5f822
bdaf9cdc5b748b22563c575d8a8278c3634ef8b44d2b187f4e23374ca9e9c0f1
```

The closed schema admitted the exact retained call and refused every tested
counterexample before compilation: generic `edits`, an unknown top-level
field, an unknown extraction field, missing forms, an unsupported require
policy, and an unsupported verification profile.

## Minimal capture-only cohort

Run one serial Sol/high cohort in this order:

```text
F  X  X  F

F = exact current full apply_clojure_changes surface
X = same name and handler, extraction-only description and schema
```

Each run uses a fresh Codex home, fresh isolated server, read-only empty
workspace, and one capture file. Preserve the full production catalog and tool
order. Replace every handler with a no-effect capture handler. Between arms,
change only the `apply_clojure_changes` description and input schema.

The existing owner-aware infrastructure supplies the useful pieces:

- atomic argument capture without reading or writing source;
- app-server registry capture;
- advertised-versus-client-visible surface comparison;
- event-clock measurement;
- offline public-schema admission and product compiler scoring.

Do not reuse its task-specific scorer unchanged. The extraction scorer must
compile the captured request through the current runtime contract and pure
extraction compiler, then compare the exact logical argument hash, counts, and
future hashes above.

## Falsifiers before launch

The model cohort is NO-GO until permanent zero-token checks prove all of these:

1. The full arm equals the retained current description, client input schema,
   output schema, annotations, tool name, and tool order.
2. The candidate arm differs only in the named description and input schema.
3. The candidate client registry equals the advertised surface; an internal
   server map is not enough.
4. A generic edit, basis request, explicit-change request, unknown field,
   malformed extraction, or invalid `verify` value refuses before the capture
   can score correct.
5. The scorer requires the first external action to be
   `apply_clojure_changes`, with no earlier shell, native read, file change,
   tool call, or visible preamble.
6. The scorer requires one call, the exact root-normalized argument hash, 15
   forms, two future files, and both future hashes. It must reject a partial or
   reordered form list even when counts still equal 15.
7. Every peer tool uses the same no-effect handler. A wrong-tool call remains
   evidence and cannot touch source.
8. The exact F–X–X–F order, fresh-home identity, workspace identity, surface
   hash, prompt hash, model, and reasoning level are present for every row.
   Missing timing data is a failed run, never a dropped sample.
9. All four attempts count. A post-token failure consumes its position; do not
   retry or silently replace it.

## Predeclared promotion gate

This is a mechanism screen, not a product result. It promotes only to a small
verified mutation cohort when:

- both arms are 2/2 exact and first-call valid;
- X wins both paired positions;
- X lowers midpoint pre-first-call wall by at least 20%;
- X lowers complete capture wall by at least 10%;
- every run has one MCP call, zero shell/native/file actions, zero refusals,
  zero recoveries, the exact argument hash, and the exact compiler result.

The 20% gate is deliberate. A dedicated public operation adds catalog and
compatibility cost. A one-second or noise-scale improvement does not earn that
cost, even though the surface is much smaller.

If the screen passes, the next experiment is a small counterbalanced
real-handler cohort with fused exact verification and complete-wall scoring.
If it misses, retain the byte and correctness evidence and stop. Do not tune
synonyms, add a plan handle, or infer a router.

## Recommendation

**Run the four-call capture cohort.** The treatment removes 14,893
client-visible bytes from the active tool, preserves the incumbent name, and
already admits the exact production call. The mechanism is plausible and the
screen is cheap.

Do not claim that the remaining 11 seconds are schema parsing. The existing
clock cannot separate prompt ingestion, scheduling, inference, serialization,
or transport. This A/B can establish only whether the smaller surface changes
the observable pre-first-call boundary.
