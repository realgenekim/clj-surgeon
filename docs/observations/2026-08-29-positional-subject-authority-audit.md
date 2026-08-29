# Positional Subject Authority Audit

Date: 2026-08-29

Lane: SURGEON2, shipped-surface audit and deterministic local probe

Issue: `clj-surgeon-qf9` (P0)

## Result

The shipped CLI `:op :edit` accepted `(line N)` as the root of an
`:expect`-guarded direct write. Duplicate content proved that this can silently
mutate a different owner from the one the caller intended.

## Executable falsifier

The disposable source was:

```clojure
(ns demo.duplicate)

(defn intended [] :old)

(defn wrong [] :old)
```

The request was equivalent to:

```text
clj-surgeon :op :edit \
  :file duplicate.clj \
  :expr "(-> (line 5) (match :old) (replace :new))" \
  :expect :old
```

Before the P0 ratchet, the result was `:ok true`, `:mode :expect-guarded`, and
`verified`, while `intended` remained `:old` and `wrong` became `:new`. The
line selected a real owner and the selected subtree equaled `:expect`, so all
content, parse, atomic-write, and read-back guards truthfully verified the
wrong subject.

The original experiment witness and full inventory are immutable in commit
`5347ad82d69961ee712e038aabd6bfb9dd488ce3`.

## Authority inventory

| Entrance | Positional or opaque value | Result |
|---|---|---|
| CLI direct `:edit` with `(line N)` and `:expect` | Physical line selects owner | Unsafe before P0 ratchet. |
| CLI read or plan-only lens | Lines, navigation, spans, partitions | No direct write; retain. |
| CLI `:replace-subform!` | Derived plan addresses | Hash-fenced reviewed plan is authority; retain. |
| CLI `:change!`, extraction, move, rename, declare repair, undo | Order/counts may appear in evidence | Subjects are explicit files, forms, namespaces, or receipts. |
| MCP reads | Lines and ranges | Read only; retain. |
| MCP mutations | Row indexes in diagnostics | Subjects are explicit files and owner/root scopes. |
| MCP compact relations | `file_index` and `row_index` output | Diagnostic output only; retain. |

## Law

> Compress repetition. Never replace identity with an unchecked reference.

The repair is not another content guard. A direct mutation must start with a
caller-visible, self-describing named owner. Positional selectors can remain
useful for reads and for producing a reviewable hash-fenced plan.

No product namespace, installation, shared server, or existing process was
changed by this audit.
