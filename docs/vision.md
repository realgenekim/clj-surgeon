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

This is the Bitter Lesson boundary in API form. The kernel supplies general
navigation, exact addresses, concrete-syntax preservation, cardinality, and
safe replay. The model supplies the path and replacement. We do not encode a
special operation for every kind of peer edit, and we do not evaluate arbitrary
Clojure to obtain apparent generality.

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
