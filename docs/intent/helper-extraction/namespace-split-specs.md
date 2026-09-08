# Whole namespace split compiler

Ratified scope: docs/plans/namespace-split.md and Gene's two-batch build brief.
One source snapshot, one final graph, one guarded publication and inverse.

- [x] **NS-SPLIT-001**: When given a complete seam mapping, the compiler shall assign every definition identity to exactly one destination.
- [x] **NS-SPLIT-002**: When references cross mapped seams, the compiler shall derive the final namespace edges from resolved reference identities with source witnesses.
- [x] **NS-SPLIT-003**: When a private definition is referenced across a seam, the compiler shall promote it only under promote-required or an explicitly authorized named set.
- [x] **NS-SPLIT-004**: When lowering resolved references, the compiler shall allocate aliases from each destination's policy without colliding with external bindings.
- [x] **NS-SPLIT-005**: When a forward reference remains within a destination, the compiler shall emit a declaration before its first use.
- [x] **NS-SPLIT-006**: When callers under bounded roots reference moved Vars, the compiler shall rewrite those references and their requires in the same future source set.
- [x] **NS-SPLIT-007**: When the final graph contains a cycle or a declared architectural violation, publication shall refuse before writing.
- [x] **NS-SPLIT-008**: When namespace identity, mapping, snapshot guards or promotion authorization is invalid, publication shall refuse before writing.
- [x] **NS-SPLIT-009**: When publishing a split, the shared extraction kernel shall commit source retirement, all destination creations and all caller edits as one failure-atomic file set.
- [x] **NS-SPLIT-010**: When publication or required verification fails, the shared guarded inverse shall restore the captured files or report recovery required.
- [x] **NS-SPLIT-011**: When analysis is requested, plan-only shall return the nonmutating projection of the same compiler, including unknown coverage and grouped blockers.
- [x] **NS-SPLIT-012**: When a split completes, its receipt shall report state, counts, destination libraries, promotions, final graph coverage and named checks with exits and durations.
- [x] **NS-SPLIT-013**: When invoked through MCP, namespace_split shall accept the closed input schema and return text containing the entire structured receipt.
- [x] **NS-SPLIT-014**: When invoked through CLI :split-ns!, the tool shall accept the same EDN request and plan-only flag with truthful refusal exit status.
- [x] **NS-SPLIT-015**: When capturing the compiler input, the boundary shall enforce finite confined roots and analyze the captured sources once.

Misreadings: twenty extraction calls constitute one compiler; source order proves
absence of forward references; textual mentions identify callers; a private crossing
authorizes any visibility change; old source require edges describe the final graph;
successful writes prove application behavior; a partial undo is a refusal without
mutation; a dry run may leave generated source files; test callers may be ignored.

Boundaries: aliases, first-class Vars, quoted Vars, locally shadowed symbols,
declarations, def metadata, external imports, external alias collisions, missing and
duplicate owners, cycles through callers, source-root mismatch, drift at publication,
proof failures and guarded undo. Unsupported reader/macro/dynamic cases are bounded
unknowns and refuse rather than imply universal static closure. State-space witnesses
exercise mapping and rollback alternatives; no retained second logical runtime.


Round 3 registered amendments (registry: namespace-split-papercuts.edn):

- [x] **NS-SPLIT-028**: When retiring a caller require, the compiler shall replace its exact position with the sorted destination block.
- [x] **NS-SPLIT-029**: While a live workspace nREPL is available, verification shall run the affected reload/test probe before cold commands and roll back on probe failure.
- [x] **NS-SPLIT-030**: When a profile selects warm proof, success shall report committed-probe-only with incomplete verification and all skipped cold commands pending.

- [x] **NS-SPLIT-032**: When a call head changes width, the compiler shall shift every subsequent line within its structural span by that delta at every nesting depth, except lines inside multiline string literals.
- [x] **NS-SPLIT-033**: When replacing the retired views require, the compiler shall splice only the sorted new block at its exact span, preserving other host bytes.

## B01: round-5 debt registration

These append-only promises register the behavior and existing witnesses in
14c0501f..1f0646e9. Earlier identifiers remain resolvable; 034 and 035 are the
current registry rulings for 032 and 033. This registration changes no compiler
behavior and does not claim a new fail-first experiment.

- [x] **NS-SPLIT-034**: When a changed call head has a width delta, the compiler shall shift every subsequent line owned by that call form by the head delta, with ownership determined by paren structure at every nesting depth and multiline string contents never moved.
- [x] **NS-SPLIT-035**: When replacing the retired views require in the pinned d9205abc forms/polish headers, the compiler shall replace only that span with the sorted destination block, preserving every other host byte.
- [x] **NS-SPLIT-036**: When the full MCP profile lists its public tools, it shall include namespace_split exactly once between helper_extraction and admit_clojure_patch.
