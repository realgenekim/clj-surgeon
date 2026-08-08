# Binding-aware local rename

**Status:** implementation contract

## Field evidence

A production refactor had to keep the request key `:sort-by` while renaming
the local binding `sort-by` to `sort-field`. The safe result used explicit map
destructuring:

```clojure
{:keys [period]
 sort-field :sort-by
 :or {sort-field :score}}
```

The caller first changed nine destructuring forms by hand. It then used one
MCP Surgeon transaction to rename 38 exact symbol occurrences across nine
owners. The second step was atomic and verified. The first step was mechanical
state that the tool should have owned.

## One-shot contract

`apply_clojure_changes` gains one action on an ordinary change item:

```json
{
  "id": "rename-sort-binding",
  "files": ["src/app.clj"],
  "forms": ["feed-page", "table-page"],
  "rename_binding": {
    "from": "sort-by",
    "to": "sort-field",
    "preserve_external_key": true
  },
  "expect": {
    "matches": 12,
    "each_form": 1
  }
}
```

For this action, `matches` is the total number of binding and resolved local
usage occurrences. `each_form` is the required number of binding definitions
in each named owner. The aggregate `edits` count uses the same occurrence
count, even when the compiler represents several occurrences as one lossless
map replacement.

The compiler must:

1. analyze the exact in-memory source snapshot;
2. resolve one local binding with the requested name in every owner;
3. refuse if the destination name can capture or collide with another local;
4. preserve the external keyword for a `:keys` destructuring binding;
5. rename the corresponding `:or` key and only local usages with the same
   binding identity;
6. leave keywords, strings, comments, Vars, and shadowed bindings unchanged;
7. compile all owners and files before any write; and
8. use the existing atomic commit, receipt, undo, and read-back verification.

## Deliberate refusal boundary

The first version supports ordinary symbol bindings and `:keys`
destructuring. It refuses:

- more than one matching binding inside an owner;
- an existing destination local inside an owner;
- `:strs`, `:syms`, or unsupported destructuring syntax;
- a comment-bearing `:keys` vector whose comment attachment would move; and
- missing or inconsistent clj-kondo local analysis.

These are compiler diagnostics, not invitations to fall back to textual
replacement. Each refusal names the owner and leaves all source unchanged.

## Acceptance tests

- Rename a direct argument and only its resolved uses.
- Preserve `:sort-by` while producing `sort-field :sort-by`.
- Rename a matching `:or` default key.
- Leave a shadowed local and `clojure.core/sort-by` unchanged.
- Refuse ambiguous bindings, destination capture, unsupported destructuring,
  stale counts, and comment-sensitive destructuring.
- Rename several owners in one transaction and undo the result exactly.
- Prove the live MCP response has `verification_complete=true` and requires no
  source reread.

## Keep gate

Keep the action only if the production-shaped case becomes one accepted MCP
call with no native preparatory edit, no partial write on every adversarial
failure, and no weakening of the existing transaction suite.
