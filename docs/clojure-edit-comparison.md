# `clojure_edit` and `apply_clojure_changes`

The two tools solve related but different editing problems.

`clojure_edit` is a structural editor for one top-level form. It identifies a
form by file, form type, and identifier. It can replace that form or insert a
form before or after it. Its contract is flat and easy to call. It validates
and formats the supplied Clojure and returns a diff.

`apply_clojure_changes` is a structural transaction compiler. It accepts a
complete decision that can contain several exact nested replacements across
several named forms and files. It checks local and aggregate match counts
before writing. It then commits every file as one failure-atomic transaction,
parses and reads back the results, and returns a hash-fenced inverse receipt.

| Dimension | `clojure_edit` | `apply_clojure_changes` |
|---|---|---|
| Unit of work | One top-level form | One complete multi-edit decision |
| Address | File, form type, form identifier | Files, named owner forms, exact nested form |
| Operations | Replace form; insert before or after | Replace exact nested forms |
| Calls | One form per call | Many edits, forms, and files per call |
| Input | Five flat fields | Repeated scoped changes plus aggregate expectations |
| Validation | Parse, lint, and format | Counts, parse, read-back, hashes, and whole-transaction refusal |
| Preservation | Regenerates the supplied top-level form | Preserves every unrelated byte |
| Result | Diff for review | Verified receipt and inverse transaction |
| Best use | Rewrite or place one complete definition | Materialize a repeated or multi-file decision |

## Comparative advantage

`clojure_edit` should be easier for an agent to select for a single full-form
rewrite. Its address is simple, and the caller supplies one complete result.

`apply_clojure_changes` should win when the decision contains repetition,
relationships, several files, or safety constraints. The caller states its
judgment once. The transaction owns address resolution, count checks, write
ordering, rollback, read-back verification, and the inverse receipt. The
caller does not need to carry partial edit state between tool calls.

This boundary is intentional. `apply_clojure_changes` should not become a
weaker whole-form editor. `clojure_edit` should not be treated as a multi-file
transaction merely because an agent can call it repeatedly.

## Agent-facing lesson

The `clojure_edit` description makes a strong routing claim before it explains
mechanics. It tells the caller to prefer the tool over generic file editors.
It then explains the immediate payoff: stable structural addressing, fewer
text-matching failures, syntax validation, and fewer generated tokens.

The first `apply_clojure_changes` description led with restrictions and safety.
Clean Codex callers therefore continued to choose the familiar `apply_patch`,
even when the request supplied a complete six-edit decision. One caller found
the MCP tool only after a native patch failed.

The corrected pitch must lead with the comparative advantage:

> PREFER `apply_clojure_changes` over `apply_patch` for two or more supplied
> Clojure replacements or edits that span files. It avoids patch-context
> mismatches and compiles the complete decision into one verified call.

The safety contract still matters, but it follows the routing decision:

- every file and named owner is explicit;
- every before and replacement form is exact;
- every expected count is positive and checked before writing;
- any mismatch refuses the whole transaction;
- success includes parse, read-back, hashes, and an inverse receipt;
- `verification_complete=true` is terminal proof unless aggregate review was
  explicitly requested.

## Performance hypothesis

A single-form edit is not the target advantage. Native `apply_patch` and
`clojure_edit` are strong controls for that case.

The target is a coherent decision that would otherwise require several edits,
source rereads, or recovery from fragile text context. The keep gate remains
end-to-end and exact: on the frozen six-edit, two-file capsule,
`apply_clojure_changes` must preserve accepted bytes and beat the 45.777-second
native result by at least ten seconds. Server bootstrap is reported separately
because a persistent MCP server is shared across agent sessions.
