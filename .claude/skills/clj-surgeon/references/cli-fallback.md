# CLI fallback

Load this reference only when the persistent MCP is unavailable, the operation
is not exposed through MCP, or the CLI itself is under test. Stop on a nonzero
exit or EDN `:error`. Use `clj-surgeon :op OP --help` when this reference does
not cover the operation.

## Smallest structural read

- Unknown top-level form: `clj-surgeon :op :ls :file FILE`.
- Known owner, containing line, or distinctive text: use `:cat` with exactly
  one of `:form`, `:forms`, `:line`, or `:contains`.
- Known owners across files: pipe one manifest to `:cat :spec-file -`.
- Unknown owner with a known EDN pattern: use `:match-form`. `:match` is not a
  regular expression.
- Related syntax or computed facts: use `:op :xray`.
- Exact nested edit: use `:op :edit`.

`:cat` never dumps a complete file. Quote names that contain shell syntax.
Use it instead of reconstructing a `sed` range. Do not run `:ls` solely as a
preflight when the owner or distinctive text is already known.
`_` matches exactly one subtree; `(loop _ _)` matches a two-argument loop.
There is no variadic wildcard.

For one coherent cross-file snapshot:

```bash
printf '%s\n' '{:reads [{:file "src/a.clj" :forms [a b]}] :expect {:file-count 1 :form-count 2}}' |
  clj-surgeon :op :cat :spec-file - :format :semantic
```

Never invoke `:spec-file -` and wait to provide input later.
`:format :semantic` omits comments and layout. Omit it when exact source is
required.

## Structural path primer

A path starts at `(form 'NAME)` or `(line N)`. Navigation skips whitespace and
comments:

- `right`: next structural sibling.
- `left`: previous structural sibling.
- `up`: structural parent.
- `down`: first structural child.
- `(match :href) right` selects the value paired with a map key.
- `span 2` selects adjacent structural peers.
- `partition-all 2` groups the remaining sibling run into pairs.
- `outermost` keeps selected nodes that have no selected ancestor. Use `up`
  before `outermost`.
- `initializer` selects a `def` right-hand side without evaluating it.

A `case` clause, `cond` branch, map entry, or binding pair is sibling syntax,
not a synthetic wrapper list. Use `:up :outermost`, not `:outermost :up`, when
nested matches must promote to disjoint owners.

## X-ray

Plain paths return exact source. End a literal path with `expect-count` when
cardinality matters. `analyze` receives one vector of ordinary Clojure data
and returns compact `:value` plus hash evidence. Use `tree-seq` only when the
input shape is unknown:

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (:events report)))))"
```

X-ray is capability-limited, not termination-proof. Keep analysis bounded. It
must never write source or a plan.

## One complete change transaction

When files, owners, exact targets, replacements, and counts are known, submit
one guarded transaction. Do not split one known plan into repeated edit calls.
Use `:spec-file -` like `kubectl apply -f -`:

```bash
clj-surgeon :op :change! :spec-file - :receipt-out /tmp/api-change.edn <<'EDN'
{:changes [{:id :body
            :in ["src/ui.clj"]
            :forms [shell reader]
            :find ":body"
            :do [:replace ":body.page"]
            :expect {:matches 2 :each-form 1}}]
 :expect {:changes 1 :edits 2 :files 1}}
EDN
```

Each named owner must resolve exactly once. Use `:each-form` or `:each-file`
when a total count could hide the wrong distribution. Count mismatch, overlap,
parse failure, or stale bytes refuses the entire transaction. Legacy exact
`:intents` remain accepted; never mix the two schemas.
The supported scoped operator is literal `[:replace SOURCE]`.

A successful receipt includes `:verified` read-back evidence and a reversible
inverse. Do not open the saved receipt. Undo only while all result hashes still
match:

```bash
clj-surgeon :op :undo-change! :receipt /tmp/api-change.edn
```

## Guarded single edit

Use `:expect` only with a literal replacement whose exact before-state is
known. Otherwise, create a plan, review the returned diff and hashes, and apply
it separately:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :expect '(assoc state :status :done)'

clj-surgeon :op :edit :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" \
  :plan-out plan.edn
clj-surgeon :op :replace-subform! :plan plan.edn
```

`transform` runs pure Clojure over selected syntax and stores only the concrete
replacement. Do not preflight whether plan paths exist. Literal replacements
preserve source spelling such as `#()`. Computed replacements use canonical printing.

Never chain plan generation and application. Do not edit the plan. To recover,
generate a new plan.
Do not reopen the plan file or reproduce it with a native
patch. A verified apply receipt is terminal read-back evidence for the
structural write.
