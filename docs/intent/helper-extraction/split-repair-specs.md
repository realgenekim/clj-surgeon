# Extraction Andon repairs

- [x] **SPLIT-REPAIR-001**: When a workspace and its configured source roots are supplied, extraction shall derive the destination namespace relative to those anchored roots.
- [x] **SPLIT-REPAIR-002**: When a caller supplies a destination library inconsistent with its source-root-relative file, extraction shall refuse before writing.
- [x] **SPLIT-REPAIR-003**: When the helper planner supplies classified callers for the captured snapshot, the extraction kernel shall use that classification without manufacturing substring candidates.
- [x] **SPLIT-REPAIR-004**: When caller decisions are required, extraction shall return the candidate files and an executable planning continuation.

Misreadings: the first absolute /src/ identifies the project; a docstring word is a
Var reference; the lower layer may broaden an already classified caller set; a
remedy mentioning next_call suffices when that field is absent.
Boundaries: nested /src/ ancestors, nonstandard deps.edn roots, longer nested roots,
outside-root paths, authoritative lib disagreement, empty classified candidates,
and a real unresolved candidate. Rung e excludes mismatched identity before write;
literal examples independently witness the other promises.
