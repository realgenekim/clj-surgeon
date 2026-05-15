# Field Extraction DSL for `.clj-surgeon.edn`

**Status:** design doc, pre-implementation
**Scope:** extends the existing `.clj-surgeon.edn` config (string → kind keyword)
to let projects describe how to extract structured fields (name, arglist,
endpoint route, EE namespace, etc.) from custom defining-form macros.

## Problem

The current `.clj-surgeon.edn` config maps a macro name to a kind:

```clojure
{:aliases {"defendpoint"   :defn
           "defenterprise" :defn
           "defsetting"    :def}}
```

That gets the form *recognized* — `:ls` lists it, `:deps` graphs it,
`:topo` sorts it. But it can't tell clj-surgeon how to find the form's
*fields*. Three concrete failure cases in Metabase:

1. **`defendpoint`** has no `name` slot — its shape is
   `(defendpoint METHOD URL DOCSTRING? [args] body)`.
   With `:kind :defn`, `extract-name` grabs the second child (`:get`),
   producing bogus form names that collide across endpoints. `:topo`
   reports false cycles. `:deps` graph is ambiguous.

2. **`defenterprise`** carries a useful piece of data — the EE-namespace
   symbol between docstring and arglist
   `(defenterprise NAME DOCSTRING NS [args] body)`. The current
   classification ignores it. Engineers reading `:ls` output don't see
   which OSS shims have EE implementations or where they live.

3. **`mu/defn`** with a meta-tagged arglist (`^String [k]`) drops `:args`
   from `:ls` because the walker looks for `:vector` AST nodes and meta
   wrappers tag as `:meta`. (Tracked separately as a pre-existing bug,
   but the DSL solves it for free by always unwrapping meta.)

## Goals

- Let a project's `.clj-surgeon.edn` describe how to find each named
  field of a custom macro.
- Stay pure EDN — config is data, not code. No `eval`, no SCI sandbox,
  no trust-boundary concerns.
- Cover the common case in ≤ 5 lines of config per macro.
- Compose to handle synthesized fields (e.g. an endpoint's name built
  from `METHOD` + `URL`).
- Fail loudly at config-load time, not silently at `:ls` time.

## Non-goals (deferred to a followup)

- Arbitrary user code in config (the **fn-as-data escape hatch**).
  Discussed at the end of this doc. Useful for macros whose layout
  doesn't fit a finite selector grammar — multi-arity bodies, conditional
  optional slots, etc. Not needed for the three Metabase macros.

## Design

### Config shape

```clojure
{:aliases
 {"defenterprise"
  {:kind   :defn
   :fields {:name         [:nth 1]
            :docstring    [:when-type :string [:nth 2]]
            :ee-namespace [:when-type :symbol [:right-of :docstring]]
            :arglist      [:find-first :vector]}}}}
```

Each value under `:fields` is a **selector expression** — a vector
starting with an operator keyword.

### Selector operators

| Operator                      | Result                                                                  |
|-------------------------------|-------------------------------------------------------------------------|
| `[:nth N]`                    | Direct child at index N (0 = the macro symbol itself, 1 = name slot).   |
| `[:find-first <type>]`        | First child matching `<type>` (after meta-unwrap).                      |
| `[:right-of <field-key>]`     | First non-trivial sibling immediately after another resolved field.     |
| `[:left-of <field-key>]`      | Mirror of `:right-of`.                                                  |
| `[:when-type <type> <sel>]`   | Run inner selector, return result only if its type matches. Else nil.   |
| `[:rest-after <field-key>]`   | All siblings after the named field. Returns a sequence.                 |
| `[:literal <value>]`          | A constant. For synthesized fields.                                     |
| `[:join <sep> <ref> <ref>…]`  | String-join previously-resolved field values, separated by `<sep>`.     |

`<type>` is one of:
`:symbol :string :keyword :vector :map :list :any`.

`<ref>` is a keyword naming another field in the same `:fields` map.

### Implicit meta-unwrap

Every selector that returns a node automatically unwraps `:meta` nodes.
That is, `^String [k]` is reported as the vector `[k]`, not as a
`:meta`-tagged thing. There is no `:unwrap-meta` op because no real
user wants meta as the captured value, and including the op everywhere
just adds noise. (Escape hatch: a future `:raw? true` field flag, only
shipped if someone actually needs it.)

### Field resolution order

Fields are resolved in a single pass per form. Some selectors reference
*other* fields (`:right-of :docstring`), so we topo-sort the fields by
their dependencies at config-load time. Cyclic references error.

Resolved fields are stored in a map keyed by the field name, both as
their zipper location (for anchored selectors to reference) and as
their stringified value (for `:join` and for emission in `:ls` output).

### Built-in defaults per `:kind`

Most macros only need a tiny override. So each `:kind` ships a default
`:fields` map, and the user's `:fields` is merged on top.

```clojure
;; built-in :defn default
{:name    [:nth 1]
 :arglist [:find-first :vector]}

;; built-in :def default
{:name [:nth 1]}
```

Defenterprise then becomes a 1-field override:

```clojure
{"defenterprise"
 {:kind :defn
  :fields {:ee-namespace
           [:when-type :symbol [:right-of :docstring]]}}}
```

### Worked example: `defendpoint`

Shape: `(api.macros/defendpoint METHOD URL DOCSTRING? [args] body)`.

```clojure
{"defendpoint"
 {:kind :defn
  :fields {:method  [:find-first :keyword]
           :path    [:right-of :method]
           :name    [:join " " :method :path]    ;; synth "GET /:id"
           :arglist [:find-first :vector]}}}
```

Topo sort: `:method` → `:path` → `:name` (depends on both)
and `:arglist` (no deps).

`:ls` output for `(api.macros/defendpoint :get "/:key" "doc" [k] body)`:

```clojure
{:type   api.macros/defendpoint
 :name   "GET /:key"
 :method :get
 :path   "/:key"
 :arglist "[k]"
 :line   N
 :end-line M}
```

The `:name` collision that breaks `:topo` today is gone: each
endpoint's synthesized name is unique.

### Failure modes

| Situation                                      | Result                                          |
|------------------------------------------------|-------------------------------------------------|
| Unknown selector op                            | Throw at config-load, name the bad op.          |
| Selector references unknown field key          | Throw at config-load, name the field.           |
| Cycle in field refs                            | Throw at config-load, list cycle members.       |
| Selector returns nil on a non-`:optional?` field | Form skipped from `:ls`; warning to stderr.   |
| Selector returns wrong type                    | Same as nil.                                    |
| Malformed EDN                                  | Throw at config-load with file path + parse error. |

Loud at load time, soft at runtime. Don't crash a `:topo` run because
one form in the file has a missing docstring.

## Why not just functions?

The fn-as-data alternative reads like:

```clojure
{"defenterprise"
 {:fields {:name (fn [zloc] (nth-child zloc 1))
           :arglist (fn [zloc] (first-vector zloc))}}}
```

Power-wise that's strictly more general. But:

- Config becomes code. Reading another team's `.clj-surgeon.edn` means
  reading their helper functions.
- Trust boundary: a `.clj-surgeon.edn` is checked into the repo, but
  CI tools (kondo, formatter, anything that loads our config) would
  now execute project code from the file. Real ecosystem footgun.
- Needs SCI or similar to safely sandbox the eval.
- Schema-checking, linting, IDE autocomplete — all harder when the
  language is "any Clojure expression."

The DSL covers the cases we actually have (three Metabase macros) and
the cases we can imagine (most defn-shaped macros with positional
extras). When that runs out, we add the escape hatch.

## Followup: the fn-as-data escape hatch

When we hit a macro whose layout the DSL can't express, we add a
single new field-value form: a list whose first element is a
recognized hook namespace. Example:

```clojure
{"weird-macro"
 {:kind :defn
  :fields {:name          [:nth 1]
           :arglist       [:find-first :vector]
           :weird-field   (clj-surgeon.hooks/find-weird-field)}}}
```

Implementation sketch:

1. Allow non-vector field values in `:fields`.
2. If the value is a list, treat it as a function reference. Resolve
   the symbol; call it with the form's zloc.
3. Run inside an SCI context with a curated set of zipper helpers
   exposed. No `eval`, no `System/exit`, no `slurp`.
4. The set of allowed hook namespaces is itself configurable, but
   defaults to `clj-surgeon.hooks` (a stdlib of common predicates and
   extractors).

This stays opt-in: projects that never hit the DSL ceiling never see
the hook layer. Projects that do can either contribute a hook to the
stdlib (most useful) or add a project-local hook.

## Migration

The existing `string → kind keyword` form remains valid:

```clojure
{:aliases {"defendpoint" :defn}}
```

…is equivalent to:

```clojure
{:aliases {"defendpoint" {:kind :defn}}}
```

…which inherits the built-in `:defn` defaults for `:name` and
`:arglist`. No breakage.

## Tests we'll want

- Each selector op in isolation, against synthetic forms.
- Topo sort of field refs (linear, branching, cyclic).
- Built-in defaults inheritance + per-field override.
- Failure modes: missing field, type mismatch, unknown op, cycle.
- End-to-end against real Metabase files (the three macros).

## Open questions

- **Optional fields.** Today `:when-type` returns nil on type mismatch.
  Do we need explicit `:optional? true` on the field to allow nil
  silently, vs. `:optional? false` (default) to warn? Probably yes —
  defendpoint's `:docstring` is genuinely optional, but `:method`
  missing is a bug.

- **Default emission policy.** Today `:ls` emits name + args only.
  Should the DSL automatically emit every resolved field? Probably yes,
  with an opt-out at the field level (e.g. `:emit? false`) for
  internal-only fields used by `:join`.

- **Cost of resolution.** Per-form selector eval should be O(form
  children) per field. Five fields × ten children = 50 zipper steps.
  Fine for `:ls`. Might want to memoize the parsed-selector tree at
  config-load.
