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

## The battlefield (Gene, 2026-09-02; measured on 81 attested arm-runs, two blind judges)

Grep plus a native patch is the competitor, and it is formidable for one reason: it costs the
agent nothing it was not already paying. One call to locate, one call to write, and the agent
trusts both without a second look. A structural tool that asks for one extra model return to
use it starts nine seconds and a context bump behind, and the agent knows it: told the tool is
available it layers it on top of its native loop at 2x wall; told it is optional it declines
it three times in three; ordered to substitute it complies on reads, escapes on writes, and
pays 2.2x. **The bar is therefore not "faster than grep". The bar is "removes a return grep
would have cost."** There are four squares where code-as-data can do that and text cannot,
and one square we withdraw from.

### Squares we compete on

1. **Verification after the agent's own patch (the gate; build first).** After every native
   patch the agent spends two or three returns to believe it: a re-read, a diff, a focused
   test. A gate that re-parses the patched file as forms, proves the untouched nodes
   byte-identical, names the owner delta, refuses form-level hazards with an executable
   next_call, and runs the focused suite returns one receipt worth those three returns, and it
   catches the classes the acceptance suite passed (a duplicate top-level definition shadowed
   by hoisting; a guard placed at the cheap top anchor instead of inside the branch). Taken on
   the route the agent already uses, its value does not depend on the agent choosing us.
2. **Fan-out: one intent across N owners, the tool discovering the owners.** Native did a
   21-owner change in one patch cell but read eleven namespaces first. An intent that finds its
   own owners from a Var or a predicate, splices the change without re-printing, and returns
   the diff plus a focused result in one receipt removes those reads and the hunk-writing. This
   is the only square where wall can go positive, and only here.
3. **Questions grep answers wrong.** Who calls this Var and with which arities; is this
   symbol a binding or a word inside a string or a comment; which branch of a reader
   conditional is live; what does this namespace require and expose. Grep returns candidates
   the agent then reads to reject. Code-as-data answers exactly, once. `:ls-tree` is the
   foundation of this square: a table of contents for a whole source tree (namespace, requires,
   every public form with its arglist and line span) in seconds, grep-filterable across many
   repos, which is the inspect-that-answers-the-question in its cheapest form. `:ls-deps`,
   `:topo` and `:mv` (move a form relative to another) sit on it.
4. **Proof before write.** Homoiconicity plus a warm JVM: a candidate change can be loaded and
   its named vars exercised before a byte lands on disk. A text tool cannot evaluate a patch.
   An agent that can ask "does this change do what I meant" and get a real answer in one
   return has something grep will never have.

### The square we withdraw from

A single edit at a known site in text the agent already holds. `apply_patch` is the floor
there and it is one cheap, atomic, batched call. The summer's per-form intent grammar lived on
this square, two thirds of every refusal it drew was the agent failing that grammar, and it is
the one square that cannot pay for itself. Measured winners that stay: `require_change`
(nine namespaces, zero churn), surgical `within` + `from`/`to`, `:extract!`, `:rename-ns!`,
`:fix-declares!`. Measured losers, closed: owner-kind-namespace insertion (re-prints the whole
file), per-form writes for fan-out, the CLI wrapper as an MCP substitute, prompt mandates.

### Constraints every design must satisfy

- **A call must remove a return the agent would otherwise make.** A tool that is faster per
  call but adds a round-trip is a loss; tool execution is 3 to 4 percent of wall and 87
  percent is model time between calls. Count returns, not milliseconds.
- **Sit on the agent's route.** Do not ask it to change route; it will not, and it is right.
- **Free-choice adoption is the acceptance test.** A feature the agent declines when the tool
  is optional has not shipped, whatever the benchmark says under a mandate.
- **Splice, never re-print.** A structural editor that reformats the untouched remainder of a
  form (hundreds of lines for a require insertion) defeats its own review-burden argument.
- **Every refusal carries a next_call the agent can execute unchanged**, or the tool performs
  the recovery and reports it. A refusal the agent cannot act on within its fields is a return
  with negative value.
- **Receipts discharge verification.** A verified receipt is terminal; if the agent still
  re-reads after it, the receipt has failed, not the agent.
- **The bitter-lesson boundary, with one added clause.** Invest in general perception,
  addressing, cardinality, preservation and replay; let the model supply interpretation and
  architecture; do not encode one refactoring opinion per incident; and the general capability
  must be on the route the model already takes, or the model routes around it.

### Cautions from the summer, so it is not repeated

Measure the free-choice baseline before building a feature. Measure the variance floor
before any comparison (nine identical runs spread 42 percent on wall and 0 to 4 on the
acceptance suite; every wall claim under 170 s and every quality score of the summer was
inside a floor nobody had measured). Attest the subject before measuring it: server identity
read from the server, prompt hash, worktree commit, per arm, or the receipt is blind. Type
the refusal ledger from day one; it settled what judge scores could not. Score the agent's
route (pre-edit, post-edit, layering), not the tool's features. Keep native as the positive
control in every cohort; a benchmark that never includes "do it without the tool" cannot
lose and so cannot learn. Keep the caller as a variable. Acceptance is a gate, not a score.

### The law of decisions, and the two-call shape (2026-09-02, evening; measured by hand at the meter)

The night's numbers, in one move: extracting nine forms with sixteen external and seven internal
call sites took native 141–152 s and 9–10 model returns to land; the rewiring verb landed the
same bytes in **1.3 s of tool time** and one call (`docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md`,
"what is possible"). The law that explains it: **an agent's cost is its count of decisions,
not its count of edits.** On that task there are two decisions, which forms go where and
whether to accept the verdict; everything between them is mechanical closure, and closure runs
at machine speed once a verb takes the whole intent and returns a verdict a cold reader can act
on. Three multipliers are true at once and must be quoted together: ~110× on the closure, ~4×
on the step (a return costs the same whether it is a call or a patch), ~1.15× on the whole task
until the gate absorbs the tail.

**The shape is two calls:** a verb that takes a complete intent and computes every consequence
(callers, requires, imports, visibility, verbatim moves), and a gate that takes the resulting
patch, verifies it against a snapshot with the repository's own coverage statement, and commits
or refuses with a remedy. Extract-with-rewire (`bridge/rf2-extract-rewire`) and
`admit_clojure_patch` (`bridge/admit-gate`) are the first two instances; `alias_migration`
(`bridge/q5z-alias-migration`) is the third and the one whose ratio should grow with the
codebase.

**The boundaries, which are part of the claim:**
- **The win exists only under mandate.** Free-choice adoption on 2026-09-02 was 0 of 10, the
  last with the exact one-call command named in the task's own terms. A tool's presence and name
  are not a path; a harness that routes the write through the verb is.
- **The receipt is the product.** A cold reader given only the receipt could not act on it twice:
  field names carried history (`remaining-source-callers` read as work to do), 347 KB of file text
  rode along, compile was unchecked. A receipt states the properties it guarantees, leads with
  state (`:applied`, target, header guarantees, callers rewired, `:callers-unresolved []`,
  `:compile {:checked true}`), is bounded (≤ 4 KB, no file contents), and every refusal carries
  what would lift it. The naive-reader probe (a fresh model, only the receipt, "what is your
  next call?") is the gate on this, and it is cheaper than any review.
- **The gate's own verification is a cost where there is nothing to remove.** Rung L control:
  gate ≈ 1.9× native on a two-minute hoist. The gate pays where native's tail is large (two JVM
  suites on the extraction task) and costs where it is small.
- **Wall and returns are two meters.** On suite-bound tasks the wall is the suites; stripping a
  third of the returns moved wall by 0.4 %. Report both, always.

**The target set is a catalogue, not a hunch:** every mechanical closure the tool can compute in
under two seconds that an agent would otherwise type or read, measured on real repos and ranked
by files-that-must-be-read × edit sites (`docs/closure-catalogue.md`). The slope experiment
(`docs/observations/2026-09-02-slope-spec-sl1.md`) draws the curve for the alias class; the
ladder that promotes a verb from hand-drive to battery is `docs/tweezer-loop.md`.

## What We Proved

The original session proved that a Babashka CLI could outline a 2,768-line
namespace, map its dependency graph, identify removable declares, move forms
with their comments, infer extraction boundaries, and rename the project
structurally. The reason is Clojure's homoiconicity plus rewrite-clj: parsing,
format-preserving traversal, and printing are already available.

Production use subsequently established two distinct scales of value:

1. Top-level operations reduce huge namespaces to small, relevant form sets.
2. Structural lenses locate and replace exact nested syntax inside large forms.

The current persistent MCP route also demonstrated complete-turn wall-clock
wins against matched native controls. The correctness-gated medians were 21.595
versus 26.749 seconds for one supplied nested edit, 27.976 versus 68.932 seconds
for six supplied edits, and 62.876 versus 81.730 seconds for the exploratory
version of that six-edit task. These results apply to a shared hot service and
the tested Clojure change strata. They do not apply to arbitrary text,
JavaScript, prose, or cold per-task startup.

The mechanism is interaction compression. Direct MCP read and write work was
subsecond. One ambiguous field caused a 9.7-millisecond refusal and a
20.46-second model recovery round. After the contract made the first call
unambiguous, the same six-edit task used one transaction and reduced median
wall by 59.4%.

The latest current-state synthesis is
[Last Night's Hill Climb](observations/2026-08-08-captains-log-last-nights-hill-climb.md).

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

One-edit plans remain the smallest review artifact. Change transactions add a
higher compilation layer for work that is already one coherent model decision.
The model supplies explicit files, optional named owner forms, exact structural
targets, literal replacements, and total or distribution assertions.
clj-surgeon compiles them into concrete per-file edits, validates their combined
future state, and emits one reversible transaction receipt. This removes
repeated bookkeeping without asking the kernel to infer intent.

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
model needs a namespace's shape. `:match-form` is better for repeated nested
Hiccup, handlers, rules, routes, and state transitions. The goal is not to
replace every tool; it is to make structural facts and exact actions cheap.

### One path should read and update

The most durable interface is jq-like: a composable path is both a getter and
an updater. In clj-surgeon, `:xray` reads a Clojure path and `:edit` uses the
same path with a terminal replacement. For example, `(-> (form 'transition)
(match :finish) right)` selects the value paired with a `case` key. The same
relationship works for a `cond` guard, map key, or binding name. A literal
terminal `replace` with `:expect` applies one declared edit and returns a
verified receipt. A computed replacement emits a hash-bound plan for separate
review and `:replace-subform!` application.

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

A literal terminal replacement has two compatible representations. Its
evaluated Clojure value defines the semantic change. Its raw `:expr` source
defines the spelling that the caller requested. The planner retains the raw
source only when SCI parses it to the same value. This preserves `#()`,
comments, metadata, commas, and layout without guessing how to rewrite an
arbitrary `fn*` form. Computed replacements remain data and use canonical
printing.

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

Paths have two explicit top-level roots. `(form 'NAME)` identifies semantic
ownership. `(line N)` identifies the one physical top-level owner whose source
range or contiguous attached comment contains N. The line root exists for
otherwise unnamed custom macro forms; it does not infer their names or select a
nested leaf. A following `match` or navigation step selects that leaf. Blank
gaps refuse with `:line-not-in-form`, and overlapping reader-conditional owners
refuse with `:ambiguous-form`. This keeps line numbers as bounded physical
locators, not guessed semantic identities.

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
provides a better address. Use `:cat` instead of reconstructing a `sed`
range when a top-level name or containing line is known. When only distinctive
text is known, use literal `:cat :contains` to select its enclosing form
without manufacturing a line number. Use `:match-form` for file-wide structural
syntax; each match exposes reusable enclosing-form ownership for optional
`:inside` narrowing. Use
`:xray :expr "(-> (form 'transition) (match :finish) right)"` when structural
relationship—not textual containment—identifies the desired node. Add a
terminal literal `replace` plus `:expect` to apply and verify a declared edit
in one call. Keep computed replacements behind a saved plan and separate
`:replace-subform!` apply. Keep `rg` for broad cross-file discovery and bounded
text reads for context that genuinely spans forms.

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
| `:ls` | Top-level form boundaries and signatures |
| `:cat` | Exact top-level source by name, containing line, or literal text |
| `:ls-tree` | Cross-project structural map |
| `:deps`, `:ls-deps`, `:topo` | Dependency visibility |
| `:ls-extract` | Minimal mechanically extractable closure |
| `:declares` | Forward-declare audit |
| `:edit` | Composable concrete-syntax single-edit updater |
| `:xray` | Pure Clojure computation over selected values with compact hash-backed or full evidence |
| `:match-form` | File-wide or scoped nested structural search |
| `:replace-subform` / `!` | Versioned, hash-bound single-subtree edit |
| `:mv` / `:mv-with-deps` | Guarded exact movement / explicit minimum dependency-expanded movement |
| `:fix-declares` / `!` | Mechanical declare cleanup |
| `:extract` / `!` | Planned namespace extraction |
| `:rename-ns` / `!` | Structural namespace rename |
| CLJC operations | Deterministic merge, split, require, and analysis |

## The Structural Lens Contract

The composable form uses one path for reading and planning:

```bash
clj-surgeon :op :xray :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right)"

clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
```

Navigation-only queries report zero, one, or many results and a per-step
cardinality trace. A terminal replacement requires exactly one result. Both
operate on syntax, preserve comments and whitespace outside the selected node,
and never write source.

Sibling slice planning uses the same boundary:

```bash
clj-surgeon :op :edit :file src/state.clj \
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
| unstructured cross-project `:find` replacement | **DO NOT BUILD** | `rg` is superior for broad discovery; `:xray`, `:edit`, and `:match-form` should remain narrow lenses where syntax identity matters. |
| `:suggest-split` | **DO NOT BUILD** | Cluster boundaries, ownership, naming, and API design are architectural judgment. Give the model `:ls` and `:deps`. |
| semantic `:diff` | **DO NOT BUILD** | Git already produces the durable change artifact; a model can interpret it with surrounding context. Lens plans need only a real unified diff. |
| `:find-extractable-pure` auto-extraction | **DO NOT BUILD** | Recognizing `swap!` is easy; deciding parameters, nil semantics, invariants, names, and tests is the actual work. |
| `:refactor` / split-by-cluster | **DO NOT BUILD** | It compounds several judgment calls and makes the failure surface larger than the kernel. Compose primitives under model supervision. |
| macro expansion / semantic inference in lenses | **DO NOT BUILD** | Lenses promise syntax identity. Pretending to know expansion, scope, or intent would weaken that understandable contract. |
| fuzzy or best-match replacement | **DO NOT BUILD** | Ambiguity must be evidence, never an implicit mutation choice. Refine the selector instead. |
## Intent transactions: the boundary beyond the microscope

Field use found a local optimum in the one-edit lens: it was precise and safe,
but a coherent change could require 23 plan/apply calls. That ceremony did not
add 23 independent design decisions. It repeatedly translated one model plan
into isolated mechanical edits and made native patching attractive again.

The durable answer is an intent compiler, not an autonomous refactoring oracle:

```text
model plan expressed as explicit EDN
  -> checked structural intents
  -> concrete hash-bound file edits
  -> combined future-state proof
  -> failure-atomic commit
  -> hash-fenced inverse receipt
```

The first compiler accepts both heterogeneous losslessly exact `:from` / `:to`
intents and scoped `:changes` over explicit file sets. A scoped change can name
top-level owner forms, match an exact subtree, and prove one match per owner or
file. One transaction can materialize many different edits from a
single model plan. `:change` previews that compiled transaction. `:change!`
commits it with handled-failure rollback and publishes a durable inverse
receipt. `:undo-change!` requires every forward result hash to remain exact.

Later transforms may add captures, insertion, deletion, movement, and
graph-aware caller mechanics. All must preserve the same boundary: the model
chooses meaning and scope; the kernel compiles and executes only declared
mechanics. See `docs/plans/intent-transactions.md` for the contract, atomicity
limits, and dogfood batches.

The boundary is simple: improve the kernel when real use exposes a general
mechanical weakness. Do not grow it merely because an AST makes a clever
feature possible.

## Typed native entrances

The CLI proved the kernel, but shell ceremony remained material. A model with
one complete six-edit decision still had to load guidance, encode EDN, cross a
process boundary, and interpret a CLI receipt. A typed persistent MCP entrance
removed that translation without changing the judgment boundary.

The first counterbalanced `apply_clojure_changes` experiment completed at a
24.530-second median versus 43.190 seconds for native. A later five-run MCP
matrix removed one ambiguous request shape. Its median was 27.976 seconds
versus 68.932 seconds for four valid native controls. Every MCP run used one
call, zero shell commands, zero source reads, and one terminal verified receipt.

Transport did not make tool choice automatic. When MCP was available but no
routing rule named it, Codex ignored it in four of four runs. One short project
`AGENTS.md` rule changed adoption to four of four while retaining a
27.432-second median. Capability metadata describes the tool. Repository
instructions state when that capability is preferred.

The same result suggests a read-side entrance. Repeated CLI reads pay one
process and agent boundary per question even when all questions are already
known. A separate `inspect_clojure` tool can batch ordered structural reads
against one in-memory snapshot. It must remain read-only and bounded. It must
not infer which question to ask next. The two-tool target is therefore:

```text
inspect_clojure          perception, read-only, batched
apply_clojure_changes    action, guarded transaction
```

These tools run through one shared multi-workspace stack. Requests carry the
canonical caller `workspace_root`. cclsp routes semantic work to lazy
workspace-specific clojure-lsp children and binds results to one LSP session,
source hashes, ranges, and owner evidence. clj-surgeon independently verifies
the source and retains the prepared basis. The model does not choose a
provider, reconstruct paths across repositories, or carry partial edit state.

The proof-carrying change buffer joins these tools without adding a third
surface. `inspect_clojure` can resolve one fully qualified Var through the hot
semantic provider, anchor its definition and references to lossless source,
and return the exact next basis-backed apply request. The model fills only
`keep` or `replace` holes. `apply_clojure_changes` reuses retained addresses and
hashes, then verifies and publishes the receipt. This is the first implemented
route where one coherent model decision remains one edit transaction even when
the exact sites were not known before inspection.

The first self-hosted prepare took 0.45 seconds. Changed-file lint and style
verification took 2.69 seconds. The earlier whole-MCP-suite profile took 45.65
seconds and was removed from the inner loop.

A later clean caller completed the intended route in exactly two MCP calls.
It took 31.00 seconds versus 54.13 seconds for the correct native control. The
42.7% reduction saved 23.13 seconds and produced a 1.75x speedup. This is one
paired probe, not a replicated median or a 3x result. It proves that complete
named owner forms and a basis-backed apply request can remove recovery and
manifest reconstruction from a real return-contract edit.

Batching is the intended source of a large read-side gain. A hot process alone
is not sufficient evidence. The representative read portfolio compares
complete task wall, calls, tokens, evidence bytes, and correctness against both
CLI and native controls.

The broader reader/explorer frontier—including snapshot-bound selection
handles, semantic zoom, structural history, future-state X-ray, impact
corridors, structural censuses, runtime/source joins, executable question
graphs, and durable watchpoints—is collected in
[The Code Reader/Explorer Frontier](code-reader-explorer-frontier.md). These
are experiment candidates, not accepted features; each must earn its
complexity against a strongest credible control.
