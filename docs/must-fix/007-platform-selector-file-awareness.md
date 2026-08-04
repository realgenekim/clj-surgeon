# Make Platform Selection File-Aware or CLJC-Only

**Status:** Resolved
**Severity:** P1 correctness boundary

## Evidence

The pure structural query evaluator receives source text but not the file
extension. `source-index` consequently assigns ordinary top-level forms both
`:clj` and `:cljs`. This is correct for shared forms in CLJC, but a platform
selector applied to a plain `.clj` or `.cljs` source cannot distinguish the
file's actual platform.

Current user-facing guidance says the optional platform is for CLJC. The
runtime accepts it more broadly without an explicit refusal.

## Required Outcome

Either pass file-derived default platforms into query evaluation, or reject a
platform selector outside a known CLJC context. Preserve honest ambiguity for
duplicate branch-local CLJC definitions and exact selection for a requested
branch.

## Implemented Contract

Query evaluation now accepts file context and derives the ordinary-form
platform set from the source extension:

| extension | ordinary-form platforms |
|---|---|
| `.clj` | `:clj` |
| `.cljs` | `:cljs` |
| `.cljc` | `:clj`, `:cljs` |

Reader-conditional branches are intersected with that file platform set. A
cross-platform selector in a plain file therefore returns exact zero evidence,
including when the source text contains a reader conditional. A platform
selector without one of these extensions refuses with
`:platform-context-required` and lists the supported extensions. Unqualified
queries retain their existing ordered zero/one/many evidence contract.

Both literal and computed X-ray evaluation pass `:file` through to this pure
query contract. Lens reads and edit planning use the same evaluator, so the
selection semantics do not diverge by front door.

## Tests and Verification

- Table-driven tests cover `.clj`, `.cljs`, and `.cljc` ordinary forms.
- `#?` and `#?@` direct branch forms cover shared, missing, duplicate, and
  platform-specific cases.
- Unsupported or unknowable platform context fails closed rather than matching
  a plausible wrong form.
- Literal evidence and query order remain exact.

The permanent table-driven matrix is in
`test/clj_surgeon/platform_selector_test.clj`. It covers ordinary forms for all
three extensions; direct `#?` and spliced `#?@` forms; shared, missing,
duplicate, metadata-bearing, and platform-specific cases; plain-file
conditional intersection; literal and computed X-ray propagation; and the
CLI structured-EDN/nonzero-exit refusal boundary.

## Done When

Every accepted platform selector has file-aware semantics, and the tool never
labels a plain-CLJ form as a valid CLJS selection.
