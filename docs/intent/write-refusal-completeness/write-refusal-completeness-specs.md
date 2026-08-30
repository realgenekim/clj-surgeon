---
parent: write-refusal-completeness-design
prefix: MCP-OP-WRITE-REFUSAL
status: 'ratified (Gene, 2026-08-30, verbatim: "Go")'
---

# Write-Side Refusal Completeness Specifications

These active IDs are stable and must not be reused if a requirement is
deleted. Gene ratified this design and registry on 2026-08-30 with `Go`.
Mayor separately authorized red-first product implementation with independent
SURGEON2 verification. Installation and MCP reload remain separately gated.

In every requirement, the version-1 inert continuation descriptor is the
closed, non-executable object defined by the parent design. It binds the
original entrance and refusal, closed family-specific subject, canonical
candidate-query SHA-256, ordering version, complete guards or an empty
pre-source guard map, next offset, 128-row page limit, and remaining count. If
the zero-row pre-finalization result and summary cannot fit in 32,640 bytes,
clj-surgeon shall use
the parent design's fixed fail-empty projection. That projection retains only
stable refusal identity and stage, `ok=false`, `source_unchanged=true`,
`mutation_attempted=false`, `write_authority=false`, numeric total and omission
counts, fixed limits, and
`write_refusal_evidence_omitted=output-budget`. It omits dynamic error text,
paths, IDs, names, maps, guards, descriptors, commands, and source. No
requirement changes the active finalizer: it shall add finite non-negative
`elapsed_ms`, and the byte meter shall measure the complete finalized MCP
result plus summary. Domain projection shall reserve 128 bytes for timing and
select the largest candidate prefix whose pre-finalization result and summary
use at most 32,640 bytes. Final measurement is an invariant check and shall not
change domain fields. Each family uses the stable stage registered in the
parent design. No
requirement authorizes the separately ratified read-only page operation needed
to consume a descriptor.

In the requirements below, `longest deterministic fitting prefix` is a defined
term for that largest prefix admitted by the 32,640-byte pre-finalization
budget. It does not depend on the later actual `elapsed_ms` value.

## Refusal families

- [x] **MCP-OP-WRITE-REFUSAL-001**: When `edit_clojure` or `apply_clojure_changes` refuses a generic scoped change with `expect-count-mismatch` after frozen compilation, clj-surgeon shall return expected and actual totals, complete per-file counts, form-scoped per-form counts only when form scope exists, and source-free resolved rows with closed `form`, `namespace`, or `root` scope identity. If more than 128 rows exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to a selector SHA-256 over the exact ordered file vector, scope, matcher, and expectation; it shall not reread source, invent an owner for root scope, select a match, or publish write or executable retry authority.

- [D] **MCP-OP-WRITE-REFUSAL-002**: When `edit_clojure` or `apply_clojure_changes` resolves a requested named, `defmethod`, or namespace owner zero or several times in frozen files, clj-surgeon shall return exact same-name status and the name-only owner or namespace vocabulary already available for every implicated file. If more than 128 rows exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to the resolution kind, requested owner, exact ordered file vector, and selector SHA-256; every row shall have `authority=false`, and clj-surgeon shall not choose an owner, widen scope, or publish an executable retry.

- [D] **MCP-OP-WRITE-REFUSAL-003**: When `edit_clojure` or the compact `edits` branch of `apply_clojure_changes` refuses `compact-location-unresolved` for an omitted location, clj-surgeon shall return diagnostics in the closed relation order `namespace-clause`, then `complete-named-owner`, including every failed closed predicate, observed count, and source-free candidate already derived. If more than 128 candidates exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to a selector SHA-256 over the compact edit fields that determine location proof; it shall not guess a location, lower a partial proof, or publish write or executable retry authority.

- [D] **MCP-OP-WRITE-REFUSAL-004**: When `transform_clojure`, or a computed program in `edit_clojure` or `apply_clojure_changes`, refuses a selection bound, expected match count, or comment-lossless commit gate after a bounded query completes, clj-surgeon shall return the source-free selected or offending rows already produced and shall identify exact comment-bearing selections without returning source. If more than 128 candidates exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to the program or change identity and query SHA-256; the descriptor shall contain no tool arguments, program body, replacement, verifier, `next_call`, executable retry, or write authority.

- [D] **MCP-OP-WRITE-REFUSAL-005**: When the extraction branch of `apply_clojure_changes` refuses `extraction-plan-refused` because requested forms are missing from frozen source, clj-surgeon shall return every missing requested name as structured data and the source-order name, owner-type, and line vocabulary of movable direct owners already computed. If more than 128 owners exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to the source file, requested names, and candidate-query SHA-256; it shall not select a substitute, change visibility or caller decisions, or publish write or executable retry authority.

- [D] **MCP-OP-WRITE-REFUSAL-006**: When a retained-basis compact edit in `apply_clojure_changes` refuses with `no-match` or `ambiguous-match` after `find-subforms`, clj-surgeon shall return the retained basis and site IDs and the source-free match candidates already produced in result order. If more than 128 candidates exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete retained frozen guards, and the version-1 inert continuation descriptor bound to the basis, site, and candidate-query SHA-256; it shall not refresh the basis, choose a subform, or publish a basis-backed or executable write retry.

- [D] **MCP-OP-WRITE-REFUSAL-007**: When `edit_clojure` or `apply_clojure_changes` refuses `binding-identity-ambiguous` or `comment-sensitive-binding`, clj-surgeon shall return the exact project-relative file, owner, binding, and source-free binder or comment-bearing candidates already known to the analyzer or compiler. If more than 128 candidates exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall return the longest deterministic fitting prefix, exact available, returned, and omitted counts, complete frozen guards, and the version-1 inert continuation descriptor bound to the binding request and candidate-query SHA-256; it shall not expose source, treat analyzer order as authority, choose a binder, or publish an executable retry.

- [D] **MCP-OP-WRITE-REFUSAL-008**: When `apply_clojure_changes` refuses `unknown-verification-profile`, clj-surgeon shall return the lexical configured profile-name vocabulary and no violation rows. When it refuses `invalid-exact-verification-profile`, an absent definition shall return only `profile-absent`, a non-map definition shall return only `definition-not-map`, and a map definition shall return every applicable failure in stable order from `definition-fields-not-exact`, `acceptance-not-exact-exit`, `timeout-missing-or-out-of-range`, `command-count-not-one`, `command-not-nonempty-string-vector`, `files-placeholder-present`, and `hot-or-cold-profile-present`. If more than 128 profile rows exist or the complete JSON MCP result would exceed 32,768 UTF-8 bytes, it shall omit top-level snapshot guards and return the longest deterministic fitting prefix, exact counts, and the version-1 inert continuation descriptor with internal `snapshot_guards={}`, requested profile, selector SHA-256, and source-free configuration-universe SHA-256; it shall preserve the pre-write refusal and return no command, argument, environment value, secret, fallback selection, executable retry, write authority, or verification authority.

## Falsifiers

| ID | Defensible opposite to test | Required witness families after ratification |
|---|---|---|
| `MCP-OP-WRITE-REFUSAL-001` | Aggregate and per-file counts are sufficient because callers can infer the responsible owners. | One owner; repeated match in one owner; matches across owners and files; zero-match scope; exact per-form counts; source-read and matcher spies remain unchanged. |
| `MCP-OP-WRITE-REFUSAL-002` | Same-name candidates alone are sufficient, and an empty same-name vector proves that no useful owner vocabulary exists. | Missing named owner; duplicate named owner; missing and duplicate `defmethod`; zero and several namespaces; multi-file scope; one-candidate authority remains false. |
| `MCP-OP-WRITE-REFUSAL-003` | A unique highest-ranked structural shape can safely become the omitted location. | Each failed injective rule; nested and external lookalikes; reader-conditional ambiguity; one displayed candidate; candidate permutation; no normalized edit or retry. |
| `MCP-OP-WRITE-REFUSAL-004` | Counts and a truncation flag are enough, or an oversize result may silently discard its bounded prefix. | Selection above the transform bound; expected-count mismatch; program and one-shot comment gates; exact offending rows; public-envelope edge; inert continuation contains no request or write fields. |
| `MCP-OP-WRITE-REFUSAL-005` | Missing names in an error string are sufficient, or the first movable owner can substitute for a missing form. | One and several missing forms; semantic rename outside lexical similarity; different owner types; line identity; no visibility, caller, or write decision. |
| `MCP-OP-WRITE-REFUSAL-006` | A retained site plus match count is enough to recover, or a retained basis may be refreshed automatically. | Zero, one, and several subforms; bounded candidate overflow; stale guard; exact retained site ID; no basis refresh or retry request. |
| `MCP-OP-WRITE-REFUSAL-007` | Analyzer order can select a binder, and naming the binding alone is enough for a comment-sensitive refusal. | Several binder identities; reordered candidates; nested destructuring; exact comment-bearing candidate; source omission; one-candidate authority remains false. |
| `MCP-OP-WRITE-REFUSAL-008` | A default verification profile or command hint is a safe recovery when admission fails. | Unknown profile among several configured names; malformed exact command, timeout, hot/cold, and field rules; secret-bearing configuration; no fallback or verifier authority. |

## Cross-family invariants

Every witness family must also prove that the existing refusal type remains
unchanged, preserve an existing stage when present, and otherwise publish the
registered family stage without changing outcome or authority. Candidate
ordering must remain deterministic but non-authoritative,
pre-write refusals leave source unchanged, and no in-scope refusal contains an
executable `next_call`, executable retry template, prepared request, selected
candidate, replacement text, verification command, or inherited write
authority.
