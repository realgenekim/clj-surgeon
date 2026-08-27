# Plan: Syntax-First Var Surface Spike

## Outcome

Measure and implement the smallest structural relation that can answer ordinary
exact Var-surface questions without starting `clojure-lsp`. The spike must
reduce cold complete wall and process footprint without pretending that syntax
proves semantics it cannot prove.

## Authority boundary

Given a fully qualified subject `source.ns/owner` and one frozen map of source
files, the scanner may publish these relations as exact syntax evidence:

- the literal symbol `source.ns/owner`;
- `alias/owner` when the file's `ns` form maps `alias` to `source.ns`.

The scanner shall not grant semantic authority to:

- bare `owner` symbols, even in the source namespace or an explicit `:refer`;
- locals or destructuring bindings that happen to share the name;
- quoted, syntax-quoted, unevaluated, string, or comment content;
- macro-generated relationships, protocol implementations, classpath symbols,
  call hierarchy, or platform selection inside hard reader conditionals.

The exact named definition remains owned by the existing Surgeon form reader.
The new relation supplies qualified surface references only. It grants no write
authority.

## Vertical slice

1. Add a pure captured-source scanner with deterministic ordering, file hashes,
   zero-based ranges, relation names, and explicit `authority=true`.
2. Prove alias-qualified, fully qualified, quote/comment/string exclusion,
   lexical-shadow non-authority, parse refusal, and stable ordering.
3. Replay the three reconstructable retained Var-surface questions and compare
   exact locations, complete wall, cclsp initialization, process count, and
   physical footprint.
4. Keep semantic escalation for any request outside the authority boundary.

## Continue gate

- At least 60% of faithfully reconstructable retained questions are answered
  by exact syntax evidence.
- Every published exact location is correct.
- Cold complete wall is at least 30% lower than cclsp.
- The scanner starts no semantic worker.

## Stop gate

- A qualified relation can be confused with inert data or lexical binding.
- Fewer than half of the reconstructed questions are covered.
- The same route immediately needs cclsp for an unresolved semantic question.
- The implementation requires a second workspace or transaction engine.

## Next product boundary

If the spike passes, project this relation into one snapshot-bound
`resolve_var_surface` ladder:

```text
exact definition + qualified syntax surface
                 |
                 +-- complete for this request --> return without cclsp
                 |
                 +-- named semantic gap --------> consult memoized cclsp
                                                    or start one bounded worker
```

Memoization is a later ratchet. A cache key must include the subject, frozen
source hashes, project configuration, provider version, and platform/session
identity. Repeated subject text alone is only a cache candidate.
