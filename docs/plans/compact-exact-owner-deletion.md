# Compact exact-owner deletion

**Status:** Active  
**Issue:** `clj-surgeon-xey.1`  
**Supersedes no existing operation:** this is a compact entrance to the shipped
exact-owner deletion kernel described in `compiled-owner-deletion.md`.

## Outcome

When an agent already knows that named top-level owners are obsolete, one
`edit_clojure` request deletes them without a source preflight, repeated file
paths, marker forms, or the heavyweight transaction schema:

```json
{
  "workspace_root": "/workspace",
  "delete_owners": [
    {
      "file": "src/app/server.clj",
      "forms": ["legacy-a", "legacy-b", "legacy-c"]
    }
  ]
}
```

`edits`, `programs`, and `delete_owners` are independent optional arrays; at
least one must be non-empty. Every operation compiles against the same original
snapshot and commits through the existing failure-atomic writer.

## Judgment boundary

The caller decides which owners are obsolete. The editor does not discover dead
code, resolve callers, infer companion deletions, or choose architecture. It
only resolves exact named top-level owners, lowers them to guarded deletions,
proves the future files parse, commits atomically, reads back, and returns one
inverse receipt.

## Observable contract

- Each item has one confined project-relative Clojure file and one non-empty,
  duplicate-free `forms` array.
- A named owner must resolve exactly once and may not be the namespace form.
- Repeating an owner anywhere in the request refuses before write.
- All literal edits, computed programs, and owner deletions see the same frozen
  source. Any overlap refuses the complete request.
- Comments attached to a deleted owner follow the existing lossless whole-owner
  deletion policy; unrelated bytes and separators remain exact.
- Success reports grouped request count, deleted owner count, total concrete edit
  count, file count, hashes, and one undo receipt. It does not echo deleted source.
- Refusal reports the item/owner identity, `source_unchanged=true`, and a concise
  remedy.

## Behavior matrix

| Case | Result |
|---|---|
| one owner / one file | one atomic deletion |
| several owners / one file | one read, one commit, exact unrelated bytes |
| owners across files | one read per distinct file, all-or-none commit |
| delete plus literal/program edit | one frozen transaction when disjoint |
| absent, ambiguous, namespace, or duplicate owner | typed refusal, zero writes |
| path escape or unsupported extension | typed refusal, zero writes |
| overlap with another operation | typed refusal naming both operations |
| future parse or write/read-back failure | refuse or roll back all files |
| exact inverse on unchanged result | byte-identical restoration |

## Implementation seam

Keep the adapter thin:

1. Extend the canonical MCP schema and derived contract sets.
2. Purely lower grouped deletion items to the existing direct `changes`
   representation with `forms`, `delete=true`, and exact derived expectations.
3. Merge them with literal edits and compiled programs before the existing
   transaction compiler.
4. Reuse current source capture, address resolution, overlap checking, commit,
   read-back, telemetry, and receipt code unchanged.

Do not add a second deletion compiler or writer. Do not route the compact
gesture through semantic preparation.

## Test-first gates

1. Red pure contract/lowering tests for valid grouped deletion and every schema
   refusal above.
2. Public boundary regression using several real-program-derived owners and an
   attached comment; prove one read per file, terminal receipt, and exact undo.
3. Mixed edit/program/delete overlap and permutation tests.
4. Published tool-schema and clean-caller contract tests.
5. Format changed Clojure, run focused warm tests, then `make mcp-test` and the
   full repository suite at `-Xmx512m`.

## Counterfactual benchmark

Derive the first capsule from the August server extraction: after architecture
is decided, several exact owners in the original monolith are known obsolete.
Give both Sol/high arms the same file and owner names but no source bodies,
patch hunks, line numbers, or after hashes.

- Compact arm may use `edit_clojure` once and read only if it chooses.
- Native arm may batch reads and one `apply_patch`; it is not penalized for
  inconsequential final whitespace.
- Both pay the same task-required parser/focused-test gate.
- Score semantic correctness, consequential unrelated bytes, complete wall,
  first successful mutation, reads, actions, request/response size, and recovery.

Run one paired canary first. Expand to three alternating pairs only when both
routes are correct. A 2--5x claim requires replicated paired complete-wall
evidence; direct kernel time alone does not qualify.

## Non-goals

- no claim that one known literal replacement should use structural deletion;
- no automatic caller cleanup or namespace extraction;
- no formatter/test surcharge unique to the Surgeon arm;
- no production routing change before the real-program canary is correct.
