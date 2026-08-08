# Structural Change Language

## Status

Accepted design. Exact scoped replacement and guarded comment-free sibling
insertion are implemented. Relational paths, captures, deletion, and computed
transforms remain evidence-gated proposals.

This design extends the shipped exact `:change` transaction. It does not
replace that contract. The purpose of the paper exercises is to find the
smallest language that lets a model materialize one complete edit plan without
translating the plan into repeated shell calls.

## Product hypothesis

A model usually decides a code change before it starts editing. The current
tool can force the model to serialize that decision as many independent
plan-and-apply operations.

A better interface accepts one transaction with five parts:

```text
scope -> selection -> operator -> cardinality -> commit receipt
```

This is the structural equivalent of how an expert editor user combines an
address, a motion, an operator, and repeat.

## Recognition test

A fresh model should understand this example without separate API prose:

```clojure
{:changes
 [{:in ["src/app/views/ide_layout.clj"]
   :forms [ide-shell source-reader-shell]
   :find ":body"
   :do [:replace ":body.ide-shell-page"]
   :expect {:matches 2 :each-form 1}}

  {:in ["src/app/views/ide_layout.clj"]
   :forms [source-reader-shell]
   :find "current-location"
   :do [:insert-left "document-title"]
   :expect {:matches 1}}

  {:in ["src/app/views/common.clj"]
   :forms [stylesheets]
   :find "(views/static ?asset)"
   :do [:replace "(assets/static ?asset)"]
   :expect {:matches 3}}]

 :expect {:changes 3 :edits 6 :files 2}}
```

The repeated record teaches the surface:

```text
:in       explicit file scope
:forms    explicit top-level owners
:find     structural selection
:do       one edit operator
:expect   exact mutation consent
```

## Model and kernel boundary

The model supplies:

- file scope;
- top-level ownership when known;
- target structure or structural path;
- edit operator and new source;
- exact per-change and aggregate expectations.

The kernel derives:

- canonical paths;
- one source snapshot per file;
- concrete syntax addresses;
- source and result hashes;
- overlap and stale-source checks;
- write order and rollback data;
- aggregate diff and inverse receipt.

The kernel does not infer architecture, widen scope, guess replacements, or
claim that parsing proves behavior. Repository formatters, linters, compilers,
tests, and live systems remain separate authorities.

## Proposed request grammar

The top-level document contains `:changes` and one aggregate `:expect` map.

Each change contains:

| Field | Contract |
|---|---|
| `:id` | Optional unique keyword for diagnostics. |
| `:in` | Non-empty vector of explicit `.clj`, `.cljs`, or `.cljc` paths. |
| `:forms` | Optional non-empty vector of owner names. Missing means the complete file scope. |
| `:find` | One exact source form or one capture pattern. |
| `:path` | Advanced structural path. Exactly one of `:find` and `:path` is allowed. |
| `:do` | One operator vector. |
| `:expect` | Exact match count and optional distribution guards. |

Use one `:forms` field for singleton and multiple owners. Do not add a
singular `:form` alias to the new language.

### Expectations

Every change requires `:expect {:matches N}` where `N` is a positive integer.

When a change names multiple owners, it can also require `:each-form N`. When
a change names multiple files, it can require `:each-file N`. These guards
catch a common false success: all expected matches occur in one scope member
while another intended member has none.

Distribution guards compose with the aggregate count. Do not infer a
distribution from the aggregate count alone.

The transaction requires exact `:changes`, `:edits`, and `:files` totals.

### Exact selection

`:find` is structural, not textual. The value is a source string containing
exactly one complete form. Whitespace can differ. Comments, metadata, reader
syntax, token spelling, and collection type remain significant.

The compiler searches only the declared files and owners. It resolves all
changes against the original snapshots. Inserted source cannot become a match
for a later change in the same transaction.

### Capture selection

A symbol that starts with `?` inside `:find` binds one structural subtree:

```clojure
{:find "(views/static ?asset)"
 :do [:replace "(assets/static ?asset)"]}
```

The replacement splices the exact captured source. It must not print the value
from parsed data. A repeated capture in one pattern must match losslessly equal
subtrees. Every capture used by the replacement must be bound by the pattern.

The first capture slice binds one subtree only. It does not bind sibling runs.
Use a smaller operator such as `:replace-head` when arbitrary call arguments
must remain untouched.

The legacy exact `:from` and `:to` intent remains the escape route for a real
source symbol whose name starts with `?`.

### Structural paths

`:path` reuses the shipped lens navigation algebra for selections that depend
on relationships:

```clojure
{:in ["src/view.clj"]
 :forms [link]
 :path [[:match :href] :right]
 :do [:replace "(route-for item)"]
 :expect {:matches 1}}
```

The language must not create a second meaning for `right`, `left`, `up`,
`down`, `span`, or `outermost`.

### Initial operators

| Operator | Meaning |
|---|---|
| `[:replace SOURCE]` | Replace each selected subtree with one exact source form or rendered capture template. |
| `[:replace-head SOURCE]` | Replace the head of each selected list and preserve all remaining bytes. |
| `[:insert-left SOURCE]` | Insert one structural sibling before each selected node. |
| `[:insert-right SOURCE]` | Insert one structural sibling after each selected node. |
| `[:delete]` | Remove the selected subtree under an explicit gap policy. |

The first implementation need not support every operator. All operators must
compile to the same concrete edit record and transaction protocol.

## Paper exercises

### 1. Replace one leaf in several owners

Goal: replace `:body` in two shell functions.

```clojure
{:in ["src/app/views/ide_layout.clj"]
 :forms [ide-shell source-reader-shell]
 :find ":body"
 :do [:replace ":body.ide-shell-page"]
 :expect {:matches 2 :each-form 1}}
```

Finding: total cardinality is insufficient. `:each-form 1` is necessary.

### 2. Replace the value of a known map key

Goal: replace the value to the right of `:href` without knowing the old value.

```clojure
{:in ["src/app/views/cards.clj"]
 :forms [post-card]
 :path [[:match :href] :right]
 :do [:replace "(post-url post)"]
 :expect {:matches 1}}
```

Finding: `:find` alone cannot express relational selection. The advanced
surface must reuse the existing lens path.

### 3. Insert one function argument

Goal: insert `document-title` before `current-location`.

```clojure
{:in ["src/app/views/ide_layout.clj"]
 :forms [source-reader-shell]
 :find "current-location"
 :do [:insert-left "document-title"]
 :expect {:matches 1}}
```

Finding: insertion needs a defined gap policy. A simple whitespace gap can
inherit the surrounding style. A gap containing comments is ambiguous and
must refuse until the caller selects a larger span or supplies exact gap
source.

### 4. Rename a call head with arbitrary arguments

Goal: replace `legacy/request` with `client/request` without rebuilding calls.

```clojure
{:in ["src/api/a.clj" "src/api/b.clj"]
 :find "(legacy/request ?request)"
 :do [:replace "(client/request ?request)"]
 :expect {:matches 2}}
```

This pattern handles one argument. It does not handle arbitrary arity.

The better general operation is smaller:

```clojure
{:in ["src/api/a.clj" "src/api/b.clj"]
 :find "legacy/request"
 :do [:replace "client/request"]
 :expect {:matches 7}}
```

Finding: select and replace the smallest stable node. Do not add a variadic
capture merely to replace a list head.

### 5. Preserve an anonymous function exactly

Goal: change the wrapper around `#(render-card %)` without expanding it to
`fn*`.

```clojure
{:in ["src/app/views/list.clj"]
 :forms [cards]
 :find "#(render-card %)"
 :do [:replace "(memoize #(render-card %))"]
 :expect {:matches 1}}
```

Finding: source strings and concrete-syntax insertion are mandatory. Parsed
value printing is not acceptable.

### 6. Wrap an expression while preserving unknown interior source

Goal: wrap an exact call shape while keeping its argument bytes.

```clojure
{:in ["src/service.clj"]
 :forms [load-record]
 :find "(fetch ?id)"
 :do [:replace "(trace (fetch ?id))"]
 :expect {:matches 1}}
```

Finding: one-subtree captures cover common wrappers. A separate `:wrap`
operator is optional until field evidence shows that it reduces errors.

### 7. Delete one map entry

Goal: remove `:debug` and its value from a map.

```clojure
{:in ["src/config.clj"]
 :forms [defaults]
 :path [[:match :debug] [:span 2]]
 :do [:delete]
 :expect {:matches 1}}
```

Finding: deletion acts on a selected span, not on a synthetic map-entry node.
The compiler must define which adjacent gap it removes. If comments occupy the
gap, the operation must refuse or require exact span source.

### 8. Replace one `case` branch result

Goal: change the result to the right of `:finish`.

```clojure
{:in ["src/state.clj"]
 :forms [transition]
 :path [[:match :finish] :right]
 :do [:replace "(assoc state :status :complete)"]
 :expect {:matches 1}}
```

Finding: the existing zipper vocabulary is already the correct motion
language. The transaction layer should compile it, not rename it.

### 9. Insert a top-level form after a known owner

Goal: add a helper after `parse-request`.

```clojure
{:in ["src/api.clj"]
 :forms [parse-request]
 :path [:self]
 :do [:insert-right "(defn request-id [request] (:request-id request))"]
 :expect {:matches 1}}
```

Finding: owner selection needs a `:self` path. Top-level insertion must preserve
the file's blank-line convention or require exact separator source.

### 10. Rename a local symbol inside one owner

Goal: rename `result` to `response` only inside `handle-request`.

```clojure
{:in ["src/api.clj"]
 :forms [handle-request]
 :find "result"
 :do [:replace "response"]
 :expect {:matches 4}}
```

Finding: this is a syntax operation, not resolved lexical rename. The model
owns the scope and count. A semantic rename requires a language server or
resolved-reference authority and must not masquerade as this operation.

### 11. Edit one CLJC platform branch

Goal: update only the ClojureScript definition of `platform-name`.

```clojure
{:in ["src/platform.cljc"]
 :forms [{:name platform-name :platform :cljs}]
 :find "\"browser\""
 :do [:replace "\"web\""]
 :expect {:matches 1}}
```

Finding: owner identity must carry platform when one `.cljc` file exposes the
same name on multiple platforms.

### 12. Change source next to a comment

Starting source:

```clojure
[project-id
 ;; The reader uses this value for conflict recovery.
 current-location]
```

Goal: insert `document-title` before `current-location`.

The compact insertion is ambiguous. Does the comment describe
`current-location` or the inserted name? The tool must not decide.

Acceptable remedies are:

- select and replace the complete vector with exact source;
- select a comment-free sibling gap;
- later supply an explicit comment-attachment policy if field evidence proves
  that such a policy is stable.

Finding: "preserve comments" is not sufficient. The tool must also avoid
changing which form a reader understands the comment to describe.

### 13. Apply a computed numeric transform

Goal: add 100 to every retry delay.

A pure function can compute the result, but the model has not declared exact
after-source. This remains a preview-and-review operation. It must not enter
the first one-shot mutation slice merely because the computation is pure.

Finding: one-shot consent comes from declared source and cardinality, not from
the determinism of executable code.

### 14. Move a branch or top-level form

A move changes two source gaps and can affect dependencies. The existing move
guards already define a stronger contract than a generic delete-plus-insert.

Finding: integrate guarded move plans with the transaction substrate later.
Do not rebuild move as two unrelated operators.

### 15. Add a namespace require

The namespace form has sorting, alias, reader-conditional, and duplicate
semantics. A raw insertion can produce parseable but poor source.

Finding: reuse the existing require operation or compile a typed require intent
into the transaction. Do not pretend a generic sibling insertion is equally
safe.

### 16. Apply two edits to one selected subtree

Goal: rename the head of `(fetch record-id)` and wrap the resulting call in
`trace`.

Two changes would select overlapping original source:

```clojure
{:find "fetch" :do [:replace "load"]}
{:find "(fetch record-id)" :do [:replace "(trace (load record-id))"]}
```

Finding: intent order must not determine the result. The transaction refuses
the overlap. The caller must consolidate the decision into one change:

```clojure
{:find "(fetch record-id)"
 :do [:replace "(trace (load record-id))"]}
```

### 17. Change a map key and its value independently

Goal: change `:timeout-ms` to `:timeout` and `5000` to `5s` in one entry.

The key and value are disjoint sibling nodes. Two changes can compile from the
same original map and apply from rightmost address to leftmost address.

Finding: sharing a parent is not overlap. The compiler must compare concrete
selected ranges, not reject all edits in one enclosing form.

### 18. Select a duplicated top-level owner name

Clojure source can contain two definitions with the same name even when that
source is undesirable. `:forms [handle]` is therefore not automatically a
unique address.

Finding: named owner selection must require exactly one owner per requested
file and platform. The refusal reports every candidate range. The caller can
use an exact file-wide structural selection when duplicate definitions are the
subject of the repair.

### 19. Add metadata to a definition

Goal: add `^:private` without reconstructing the complete `defn`.

A generic insertion near `defn` can put metadata on the symbol, the argument
vector, or the list. Each placement has different source and sometimes
different meaning.

Finding: metadata needs a typed operator with an explicit target. It does not
belong in the initial sibling-insertion slice.

### 20. Delete a top-level definition

Goal: remove an unused helper.

A syntax-only delete can strand callers or remove a dependency that another
form still uses.

Finding: top-level deletion must consult dependency evidence or require an
explicit dependency guard. Generic `[:delete]` is initially limited to nested
syntax whose enclosing owner remains present.

### 21. Edit an anonymous top-level form

Goal: change one expression in a top-level `(comment ...)` or registration
form that has no stable owner name.

`:forms` is optional. An exact `:find` can search the explicit file scope.
When several candidates exist, the positive count guard refuses ambiguity.

Finding: the language does not need line numbers for this case. Exact syntax
and an explicit file often form a stronger address.

### 22. Edit a string token

Goal: replace `"Waiting"` with `"Ready"` inside one owner.

```clojure
{:in ["src/status.clj"]
 :forms [status-label]
 :find "\"Waiting\""
 :do [:replace "\"Ready\""]
 :expect {:matches 1}}
```

Finding: a string is an ordinary structural token. This does not imply regular
expressions or substring replacement inside the string.

### 23. Update the same owner across several files

Goal: replace one option keyword in each of four adapters.

An aggregate `:matches 4` can succeed with two matches in one file and none in
another.

Finding: the expectation language also needs `:each-file`. Distribution guards
must compose: a change can require total, per-file, and per-form counts.

### 24. Select one multimethod implementation

Several `defmethod` forms share one var name and differ by dispatch value.
`:forms [render]` cannot identify one implementation.

Finding: owner identity is not always a name. A later owner selector can pair
the defining name with a dispatch form. Until then, use exact file-wide syntax
or a path that selects the desired `defmethod` form.

### 25. Replace a form with itself

Whitespace-only differences do not create a structural mutation under the
shipped lossless equality contract.

Finding: each compiled change must produce at least one lossless difference.
No-op changes refuse before aggregate edit counts are evaluated.

## Paper execution traces

These traces simulate the complete caller experience. They are design
artifacts, not test results.

### Trace A: two different edits in one owner

Starting source:

```clojure
(defn card [post]
  [:a {:href (:url post)}
   [:span.title (:name post)]])
```

One request:

```clojure
{:changes
 [{:id :card-link
   :in ["src/cards.clj"]
   :forms [card]
   :path [[:match :href] :right]
   :do [:replace "(post-url post)"]
   :expect {:matches 1}}

  {:id :card-title-class
   :in ["src/cards.clj"]
   :forms [card]
   :find ":span.title"
   :do [:replace ":span.card-title"]
   :expect {:matches 1}}]

 :expect {:changes 2 :edits 2 :files 1}}
```

Expected future source:

```clojure
(defn card [post]
  [:a {:href (post-url post)}
   [:span.card-title (:name post)]])
```

Expected compact result:

```clojure
{:ok true
 :operation :change!
 :change-count 2
 :match-count 2
 :changed-file-count 1
 :committed true
 :verified {:whole-files true}}
```

Study result: relational and exact selections can share one transaction. The
two selected ranges are disjoint even though they share one owner.

### Trace B: one small edit across several call shapes

Starting sources:

```clojure
;; src/a.clj
(legacy/request endpoint payload)

;; src/b.clj
(legacy/request endpoint
                ;; Keep the retry wrapper with the payload.
                (with-retry payload)
                {:timeout 5000})
```

One request:

```clojure
{:changes
 [{:id :request-client
   :in ["src/a.clj" "src/b.clj"]
   :find "legacy/request"
   :do [:replace "client/request"]
   :expect {:matches 2 :each-file 1}}]

 :expect {:changes 1 :edits 2 :files 2}}
```

Expected future sources:

```clojure
;; src/a.clj
(client/request endpoint payload)

;; src/b.clj
(client/request endpoint
                ;; Keep the retry wrapper with the payload.
                (with-retry payload)
                {:timeout 5000})
```

Study result: replacing the smallest stable node preserves arbitrary arity,
layout, nested forms, and the comment. A variadic capture would add machinery
and increase the source surface at risk.

### Trace C: refuse an ambiguous insertion

Starting source:

```clojure
[project-id
 ;; Used to recover the previous reader position.
 current-location]
```

Request:

```clojure
{:changes
 [{:id :reader-title-binding
   :in ["src/reader.clj"]
   :find "current-location"
   :do [:insert-left "document-title"]
   :expect {:matches 1}}]

 :expect {:changes 1 :edits 1 :files 1}}
```

Expected refusal:

```clojure
{:error-type :ambiguous-insertion-gap
 :change-id :reader-title-binding
 :file "src/reader.clj"
 :target "current-location"
 :remedy "Replace a larger exact span that declares comment placement."}
```

Expected side effects: no source write and no receipt write.

Study result: byte preservation alone is not enough. A tool can preserve every
comment byte and still change what the comment appears to describe.

## Design changes caused by the exercises

1. Use `:forms` only. Do not add a singular alias.
2. Add distribution guards such as `:each-form`; total counts alone can hide a
   missed owner.
3. Keep `:find` for the obvious exact structural case.
4. Add `:path` only for relational selections and reuse the existing lens
   vocabulary exactly.
5. Prefer the smallest node edit over variadic capture machinery.
6. Bind one subtree per capture in the first capture slice.
7. Refuse comment-bearing insertions and deletions until the caller supplies
   an unambiguous larger selection.
8. Keep computed transforms behind preview and review.
9. Reuse specialized dependency, move, require, rename, and CLJC operations.
   Do not weaken them into generic syntax edits.
10. Make safety and receipts defaults of `:change!`; do not repeat them in each
    manifest.
11. Add `:each-file` beside `:each-form`; aggregate counts do not prove scope
    distribution.
12. Define overlap by concrete source ranges. Disjoint siblings in one parent
    are valid; ancestor, descendant, and identical selections refuse.
13. Require unique owner resolution. A repeated definition name or multimethod
    family needs a stronger selector.
14. Limit generic deletion to nested syntax until dependency-aware top-level
    deletion has its own guard.
15. Refuse structural no-ops even when replacement source uses different
    whitespace.

## First implementation slice

The first slice should compile only these existing lens mechanics into one
transaction:

- explicit `:in` and `:forms` scope;
- exact `:find` selection;
- `[:replace SOURCE]` operator;
- exact match and `:each-form` guards;
- mixed use with shipped exact `:from` and `:to` intents;
- one aggregate diff, failure-atomic commit, and inverse receipt.

This slice is useful without capture or insertion. It can materialize several
different leaf and subtree replacements in one call. It also tests the main
hypothesis: transaction count, not replacement cleverness, is the current
bottleneck.

After that slice passes real dogfood, test in this order:

1. existing lens `:path` selection;
2. one-subtree capture templates;
3. comment-free sibling insertion; **implemented and live-proven**
4. deletion with an explicit gap contract;
5. integration of specialized move and dependency-aware intents.

## Refusal matrix

Every refusal returns EDN, exits nonzero, and writes no source or receipt.

| Condition | Required diagnostic |
|---|---|
| Unknown top-level or change key | Name the unknown key and allowed keys. |
| Empty or duplicate file scope | Identify the change and canonical duplicate. |
| Missing or ambiguous owner | Report file, owner, platform, and actual count. |
| Both `:find` and `:path` | Require exactly one selector. |
| Invalid source form | Name the field and parse failure. |
| Unsupported operator | Name the operator and list supported operators. |
| Missing or non-positive match guard | Refuse before source mutation. |
| Total match mismatch | Report expected and actual totals by file and owner. |
| `:each-form` or `:each-file` mismatch | Report each scope member that violated the guard. |
| Missing or duplicated named owner | Report all candidate ranges and require a stronger selector. |
| Unbound or inconsistent capture | Name the capture and occurrence. |
| Overlapping selections | Name both changes and their common file. |
| Ambiguous comment-bearing gap | Recommend exact enclosing replacement. |
| Top-level generic deletion | Recommend dependency-aware deletion or an explicit dependency guard. |
| Invalid future file | Name the file and parser failure. |
| Stale source before commit | Report expected and actual source hashes. |
| Handled write failure | Report rollback state and recovery evidence. |

## Non-goals

- natural-language interpretation;
- inferred file or owner scope;
- fuzzy matching;
- regular-expression source replacement;
- resolved lexical rename without a semantic authority;
- arbitrary verification command execution;
- automatic formatting of complete files;
- computed mutation without review;
- weaker replacements for existing move, dependency, require, rename, or CLJC
  contracts.

## Paper acceptance gate

Before implementation, give only the recognition example to clean Codex and
Claude contexts. Give each caller three new edit tasks:

1. one simple exact replacement;
2. one relational map or branch edit;
3. one task that must refuse because of a comment-bearing gap.

Keep the language only if both callers:

- produce one transaction without help;
- use exact scopes and positive guards;
- distinguish `:find` from `:path`;
- predict the refusal for the ambiguous insertion;
- do not invent line numbers, hashes, plan files, or an apply sequence.

Then compare the same edit workload with native patching. The feature must
reduce complete wall time by at least five seconds and must not weaken any
test, parse, refusal, or rollback gate.
