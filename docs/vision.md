# clj-surgeon: Vision

## Purpose

clj-surgeon is a small structural kernel for Clojure agents. It exposes syntax,
dependencies, and exact edit mechanics as fast, deterministic operations while
leaving architectural judgment to the model and the human.

The division of labor is deliberate:

- The model decides what the code should mean and which change is appropriate.
- clj-surgeon finds, addresses, moves, and replaces syntax without guessing.
- The compiler, linter, and tests validate the resulting program.

This is the bitter lesson applied to developer tools: as models improve, the
durable investment is not a growing catalog of encoded refactoring opinions. It
is a small set of dependable primitives that give a smarter model inexpensive
perception and precise action.

## What We Proved

The original session proved that a Babashka CLI could outline a 2,768-line
namespace, map its dependency graph, identify removable declares, move forms
with their comments, infer extraction boundaries, and rename the project
structurally. The reason is Clojure's homoiconicity plus rewrite-clj: parsing,
format-preserving traversal, and printing are already available.

Production use subsequently established two distinct scales of value:

1. Top-level operations reduce huge namespaces to small, relevant form sets.
2. Structural lenses locate and replace exact nested syntax inside large forms.

On a 4,036-line namespace with 322 top-level forms, `:ls` immediately exposed
the relevant synchronization forms. Nested searches then proved that particular
state calls and Hiccup elements occurred exactly once. In later work, agents
used zero/one/many match counts as executable hypotheses about code shape and
used hash-bound plans to edit active dirty worktrees safely.

## Design Principles

### Bookkeeping, not judgment

A useful operation removes mechanics the model already knows it wants:

- locating exact form boundaries;
- computing and presenting dependency relationships;
- moving syntax while preserving formatting and comments;
- replaying one reviewed nested edit against an unchanged snapshot;
- updating namespace references mechanically.

The operation should not decide the desired architecture, infer intent, name a
new abstraction, or silently widen its scope.

### Search may be broad; mutation must be singular

`:find-subform` reports zero, one, or many matches because ambiguity is useful
information during discovery. `:replace-subform` refuses unless exactly one
complete pattern matches, and its replacement must also contain exactly one
complete form. There is no “best match” heuristic.

### Plans are durable interfaces

A structural edit plan is not console decoration. It is a versioned artifact
with stable fields:

- `:plan-version`
- `:operation`
- `:file`
- `:selector`
- `:source-hash`
- `:result-hash`
- `:edits`
- `:diff`
- `:provenance`

Provenance records the tool/version, operation, selector, and both snapshot
hashes. Application uses the recorded address; it does not rerun the selector.

### Fail closed

Errors are concise EDN and always produce a nonzero process exit status. A plan
is refused if its schema is unknown, its source changed, its addressed subtree
changed, its result hash differs, or its rewritten file cannot be parsed.

The write contract is literal: applying a lens plan uses an atomic filesystem
replacement. If the filesystem cannot provide it, clj-surgeon reports an error
and leaves the target unreplaced. It does not silently fall back to a weaker
write.

### Small composable primitives age well

`rg` remains the right tool for broad textual discovery. `:ls` is better when a
model needs a namespace's shape. `:find-subform` is better for repeated nested
Hiccup, handlers, rules, routes, and state transitions. The goal is not to
replace every tool; it is to make structural facts and exact actions cheap.

### One path should read and update

The most durable interface is jq-like: a composable path is both a getter and
an updater. In clj-surgeon, `:q` runs an EDN pipeline over rewrite-clj's concrete
syntax tree. For example,
`[[:form transition] [:find :finish] :right]` selects the value paired with a
`case` key. The same relationship works for a `cond` guard, map key, or binding
name. Ending the path with `[:replace FORM]` changes its role from read evidence
to a single hash-bound plan; it still never writes source. The reviewed plan is
applied separately with `:replace-subform!`.

The same path algebra includes structural slices. After selecting a node,
`[:span 2]` addresses it and its next semantic peer as one located object. A
terminal `[:replace-span FORM FORM]` replaces corresponding nodes with equal
arity, so comments and whitespace between them remain exact. This covers peer
pairs and flattened `#(...)` bodies without a macro-specific operation or a
synthetic wrapper node.

The enumeration counterpart is `[:partition-all 2]`. Starting at one located
node, it groups the complete following sibling suffix into consecutive
lossless spans. It retains a shorter final span instead of dropping or naming
it. The same mechanical step inventories `case` tests/results, `cond`
guards/results, map entries, bindings, and alternating function arguments. The
model supplies the starting node and interprets the neutral `:forms`. The
kernel supplies boundaries, source, cardinality, and addresses.

The generic `:outermost` stream filter closes the nested-owner case without
teaching the kernel macro semantics. After `[:find cond] :up`, it keeps every
current owner that has no current ancestor; disjoint outer owners remain
disjoint. Placement is part of the algebra: use `:up :outermost`, not
`:outermost :up`, because head symbols do not contain one another. When a first
outer guard is known, anchoring there remains the shorter expression.

The plan-only `:edit` front door can compile its capability-limited pure
Clojure `:expr` into the same path data. SCI exposes collection computation and the
structural builders, but not I/O, processes, namespaces, mutable references,
classes, or host interop. This gives the model Clojure for composing Clojure
edits without adding another selector, planner, plan schema, or executor.
Literal EDN paths remain available when they are shorter.

The native `transform` terminal is the stronger form of this idea. It gives a
pure function the exactly-one selected form as Clojure data. The function can
derive a new form with `mapv`, `assoc`, `update`, or other allowed operations.
Planning then discards the function and saves only the concrete replacement,
diff, and hashes. The durable artifact stays data, and the trusted executor
does not gain an interpreter.

The read-only surface is `:xray :expr`. A plain Clojure path such as
`(-> (form 'transition) (match :finish) right)` returns exact literal source
evidence. `(analyze path pure-function)` always passes an ordered selection
vector of ordinary Clojure data, including for zero or one match. Write one
terminating pure function over that a-priori contract; use collection predicates or
`tree-seq` inside the same expression instead of a separate representation
probe. `(expect-count path n)` refines
cardinality without changing that input representation and refuses before
analysis. Computed results return bounded EDN `:value` with compact addresses,
ranges, trace, cardinality, and hashes. Computation never replaces evidence.
X-ray never writes source or a plan. Former spellings remain compatibility
inputs but are not the primary surface. The general `initializer` path
operator selects a `def` right-hand side without evaluating it; semantic
interpretation remains with the model. Computed analysis normalizes a selected
value when that value itself is a map literal or `hash-map` / `array-map`
syntax. It does not recursively normalize nested constructor syntax or execute
source; evidence retains exact syntax.

The sandbox should feel like Clojure, not an accidental smaller dialect. Pure
`key`, `val`, and `for` are available. Macro expansion may use private loop and
chunk machinery, while direct `loop`, `recur`, I/O, mutation, classes,
processes, namespaces, and host interop remain refused. This prevents direct
unbounded loop forms without rejecting idiomatic comprehensions. It is a
capability boundary, not a proof of termination: callers must supply bounded
work, and a pure expression over an unbounded input can still fail to finish.

This is the Bitter Lesson boundary in API form. The kernel supplies general
navigation, exact addresses, concrete-syntax preservation, cardinality, and
safe replay. The model supplies the path and replacement. We do not encode a
special operation for every kind of peer edit. Pure Clojure analysis runs only
inside the bounded SCI capability sandbox; arbitrary host execution remains
outside the kernel.

## The Structural X-Ray Loop

clj-surgeon should be the preferred structural instrument for an agent that
must inspect and change Clojure. Compilers, linters, tests, and live REPLs
remain the semantic authorities.

```text
x-ray exact structure
  → view exact source and relationships
  → select one unambiguous target
  → plan one exact change
  → review diff and hashes
  → apply only to the unchanged snapshot
  → verify with parser, linter, compiler, tests, or live REPL
```

The ideal lens minimizes fallback to line-oriented reading when syntax already
provides a better address. Use `:show-form` instead of reconstructing a `sed`
range when a top-level name or containing line is known. When only distinctive
text is known, use literal `:show-form :contains` to select its enclosing form
without manufacturing a line number. Use `:grep-form` for file-wide structural
syntax; each match exposes reusable enclosing-form ownership for optional
`:inside` narrowing. Use
`:q :query '[[:form transition] [:find :finish] :right]' when structural
relationship—not textual containment—identifies the desired node. Add a
terminal `[:replace FORM]` to emit a plan and apply it later with
`:replace-subform!`. Keep `rg` for broad cross-file discovery and bounded text
reads for context that genuinely spans forms.

Perfection here means lossless perception, singular guarded action, and an
executable recovery path after refusal. It does not mean autonomous design or
an expanding catalog of inferred refactorings.

The native patch tool remains the wall-clock bar for one exact, supplied, unique
text edit. clj-surgeon should win when structure removes discovery, ambiguity,
manual computation, or unsafe replay. It should not maximize its own adoption.
When the structural route buys no measurable correctness, context, call, or
safety advantage, use the faster native patch.

## Shipped Kernel

| Operation | Role |
|---|---|
| `:ls` / `:outline` | Top-level form boundaries and signatures |
| `:show-form` | Exact top-level source by name, containing line, or literal text |
| `:ls-tree` | Cross-project structural map |
| `:deps`, `:ls-deps`, `:topo` | Dependency visibility |
| `:ls-extract` | Minimal mechanically extractable closure |
| `:declares` | Forward-declare audit |
| `:lens` / `:q` | Composable concrete-syntax getter and single-edit plan updater |
| `:xray` | Pure Clojure computation over selected values with compact hash-backed or full evidence |
| `:grep-form` / `:find-subform` | File-wide or scoped nested structural search |
| `:replace-subform` / `!` | Versioned, hash-bound single-subtree edit |
| `:mv` / `:mv-with-deps` | Guarded exact movement / explicit minimum dependency-expanded movement |
| `:fix-declares` / `!` | Mechanical declare cleanup |
| `:extract` / `!` | Planned namespace extraction |
| `:rename-ns` / `!` | Structural namespace rename |
| CLJC operations | Deterministic merge, split, require, and analysis |

## The Structural Lens Contract

The composable form uses one path for reading and planning:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right]'

clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
```

Navigation-only queries report zero, one, or many results and a per-step
cardinality trace. A terminal replacement requires exactly one result. Both
operate on syntax, preserve comments and whitespace outside the selected node,
and never write source.

Sibling slice planning uses the same boundary:

```bash
clj-surgeon :op :q :file src/state.clj \
  :query '[[:form transition] [:find :finish] [:span 2] [:replace-span :finish (assoc state :status :complete)]]' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

Planning:

```bash
clj-surgeon :op :replace-subform \
  :file src/views.clj \
  :inside render \
  :match '(post! "/api/items" _)' \
  :with '(items/actions surface)' \
  :plan-out plan.edn
```

The planner parses exactly one complete match and replacement form, requires
exactly one match, produces a unified diff, computes source and result hashes,
reparses the complete future file, and writes a versioned EDN plan.

Application:

```bash
clj-surgeon :op :replace-subform! :plan plan.edn
```

The applier validates plan version, source snapshot, recorded address, exact
before text, complete result parse, and result hash before attempting an atomic
replacement.

## DO NOT BUILD

These are not backlog items. They are intentionally excluded because they
encode judgment, duplicate a stronger general tool, or invite an unreliable
compound operation.

| Idea | Decision | Why |
|---|---|---|
| `:dead-code` | **DO NOT BUILD** | clj-kondo supplies evidence; a model can inspect reachability and runtime registration context. |
| unstructured cross-project `:find` replacement | **DO NOT BUILD** | `rg` is superior for broad discovery; `:q` and `:find-subform` should remain narrow lenses where syntax identity matters. |
| `:suggest-split` | **DO NOT BUILD** | Cluster boundaries, ownership, naming, and API design are architectural judgment. Give the model `:ls` and `:deps`. |
| semantic `:diff` | **DO NOT BUILD** | Git already produces the durable change artifact; a model can interpret it with surrounding context. Lens plans need only a real unified diff. |
| `:find-extractable-pure` auto-extraction | **DO NOT BUILD** | Recognizing `swap!` is easy; deciding parameters, nil semantics, invariants, names, and tests is the actual work. |
| `:refactor` / split-by-cluster | **DO NOT BUILD** | It compounds several judgment calls and makes the failure surface larger than the kernel. Compose primitives under model supervision. |
| macro expansion / semantic inference in lenses | **DO NOT BUILD** | Lenses promise syntax identity. Pretending to know expansion, scope, or intent would weaken that understandable contract. |
| fuzzy or best-match replacement | **DO NOT BUILD** | Ambiguity must be evidence, never an implicit mutation choice. Refine the selector instead. |
| multi-edit lens plans | **DO NOT BUILD** | One plan, one edit keeps review, provenance, replay, and failure atomic. Sequence plans when several edits are needed. |

The boundary is simple: improve the kernel when real use exposes a general
mechanical weakness. Do not grow it merely because an AST makes a clever
feature possible.
