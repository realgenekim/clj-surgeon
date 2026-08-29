# Positional Mutation Authority

## Context

`clj-surgeon` is a structural tool. A direct mutation must identify its
structural subject. A physical line, ordinal, index, or other positional
coordinate can identify a real subject without identifying the subject the
caller intended.

A retained duplicate-owner falsifier demonstrated the failure. A line-rooted
CLI `:edit` selected another real owner that contained the same guarded value.
The `:expect` comparison, atomic write, parse, and read-back checks all passed,
and the wrong owner changed. The failure was a category error in authority,
not a missing content guard.

## Boundary

The first slice governs the native CLI `:edit` entrance.

- Read-only lens queries keep `[:line N]`, navigation, spans, partitions, and
  positional result evidence.
- Plan-only `:edit` keeps the complete query language because it writes no
  source. Its saved plan binds the selected subject to exact source and result
  hashes and requires separate review and application.
- Direct `:expect`-guarded `:edit` requires its compiled query to start with
  `[:form NAME]`. The optional platform remains part of the named owner.
- A query with any other root refuses before source I/O or plan I/O. It does
  not attempt to infer an owner from a line or from unique content.
- `:replace-subform!` remains authorized only by a previously emitted,
  reviewed, hash-fenced plan. Internal addresses in that artifact are derived
  evidence, not caller-supplied subject selectors.

The rule is about authority, not vocabulary. Lines and indexes remain valid in
read results, diagnostics, receipts, and internal frozen-snapshot evidence.

## Refusal contract

The direct-write decoder returns a typed pre-write refusal:

```clojure
{:operation :edit
 :error-type :positional-mutation-authority-refused
 :source-unchanged true
 :source-state :unchanged
 :required-root [:form 'OWNER]
 :remedies
 {:named-owner
  {:instruction "Name the top-level owner with (form 'OWNER) before a direct :expect-guarded edit."}
  :plan-review
  {:instruction "Remove :expect, write the plan with :plan-out, review it, then apply it with :replace-subform!."}}}
```

The result includes the supplied query and its first step. It never claims the
line, content, or nearest owner is authoritative.

## Public entrance inventory

The permanent inventory separates caller input from output or derived
evidence:

| Entrance | Positional data | Mutation authority |
|---|---|---|
| CLI `:edit` with `:expect` | Query may contain positional navigation after a named root | Only an explicit first-step `[:form NAME]` grants direct-write subject authority. |
| CLI plan-only `:edit` and `:q` | Lines, navigation, spans, and partitions | No source write; a later apply consumes a reviewed hash-fenced plan. |
| CLI `:replace-subform!` | Derived addresses inside the plan | The exact plan, source hash, and result hash are authority; raw positions are not accepted. |
| Other CLI mutations | Counts and order can appear in evidence | Public subjects remain explicit file, form, namespace, or receipt identities. |
| MCP reads | Lines and ranges | Read only. |
| MCP mutations | Row indexes can appear in diagnostics | Public subjects remain explicit files and named owner/root scopes. |
| MCP compact relations | `file_index` and `row_index` in output | Diagnostic output only; not accepted as input authority. |

## Evidence and rationale

- [Positional subject authority audit](../../observations/2026-08-29-positional-subject-authority-audit.md)
- [Wrong in-range index falsifier](../../observations/2026-08-29-wrong-index-ended-emission-composition.md)

## Alternatives rejected

- **Validate only that the line resolves uniquely.** The wrong owner can be a
  unique valid resolution.
- **Trust `:expect`.** Duplicate source makes the wrong subject satisfy the
  same expectation.
- **Return a nearest owner and continue.** Similarity is a hint, never write
  authority.
- **Add an opt-in unsafe flag.** Request data cannot widen mutation authority.
- **Remove positional reads.** This over-purges useful evidence and does not
  improve write safety.

## Verification

The permanent tests must prove the duplicate-owner line request refuses before
source or plan I/O, the exact source remains byte-identical, the refusal names
the owner alternative, named-owner direct mutation still succeeds, positional
plan-only and read routes still work, and the public operation inventory
contains no other positional direct-mutation entrance.
