# Clojure-Native Edit Algebra Experiment

**Status:** Evidence earned; Stage A prototype next

## Product hypothesis

The current structural kernel is safe and expressive, but agents must translate
Clojure intent into an EDN vector embedded in shell quoting:

```bash
clj-surgeon :op :edit :file state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn
```

Clojure is already active in the model's context. Ordinary Clojure functions
may be a shorter and more reliable authoring surface for Clojure structure:

```clojure
(edit "state.clj"
  (-> (form 'transition)
      (match :finish)
      right
      (replace '(assoc state :status :complete))))
```

This experiment asks whether that native expression reduces translation,
quoting, help, and recovery work. It does not replace the guarded planner.

## Baseline to beat

With the installed edit-first skill, the existing CLI completed 12 of 12 exact
edits across `case`, outer `cond`, and `let` binding shapes.

- The structural plan was the first source-bearing command in 11 of 12 runs.
- The case and binding tasks reached the 3-command floor: skill read, plan,
  apply.
- The outer-`cond` task had a 5-command median because agents sometimes refined
  an already-correct plan or read the owner first.
- One agent generated a correct plan but replayed its diff with `apply_patch`
  instead of the verified executor.

The native surface must beat this baseline. Merely looking more like Clojure is
not enough.

## Architectural boundary

The experiment adds an authoring library, not an evaluation feature in the
CLI. Callers execute normal Clojure or Babashka code and require the library.
clj-surgeon does not accept an arbitrary expression string and call `eval`.

Combinators build the existing query data. Planning delegates to the existing
`:edit`/lens implementation. Applying delegates to the unchanged verified
executor.

```text
Clojure combinators
  -> existing EDN query vector
  -> existing singular planner
  -> existing versioned plan
  -> separate existing executor
  -> existing verified receipt
```

No new selector, replacement, address, diff, hash, replay, or write semantics
are permitted in Stage A.

## Stage A: pure query builders

Prototype only these functions:

```clojure
(form name)             ; => [[:form name]]
(match path pattern)    ; append [:find pattern]
(where path predicates) ; append [:where predicates]
(right path)            ; append :right
(left path)             ; append :left
(up path)               ; append :up
(down path)              ; append :down
(span path n)            ; append [:span n]
(partition-all path n)   ; append [:partition-all n]
(replace path form)      ; append [:replace form]
(replace-span path & forms) ; append [:replace-span ...]
```

All functions are pure. They accept and return persistent Clojure data. They
perform no parsing, file I/O, evaluation, matching, or mutation. Thread-first
composition must produce the byte-for-byte existing query vector.

Do not add macros. Functions are easier to test, inspect, compose, and call
programmatically. Symbols such as `right` are functions, so thread-first syntax
remains ordinary Clojure.

## Stage B: one planning call

Only after Stage A wins query-authoring probes, test:

```clojure
(edit "state.clj" query)
```

The call may create a generated plan artifact and return its explicit path,
diff, hashes, and exact apply form. It must never modify source. Generated plan
names must be collision-safe and must not hide durable state.

The returned value must make the review boundary impossible to miss:

```clojure
{:status :review-required
 :plan-file "/absolute/path/to/plan.edn"
 :diff "..."
 :apply-form '(apply-edit! "/absolute/path/to/plan.edn")}
```

Application remains a later evaluation or command. Do not add an option that
plans and applies in one call.

## Deferred transformations

The screenshot names replace, insert, delete, splice, wrap, unwrap, and move.
That is a vision, not the MVP contract.

Stage A exposes only existing replacement semantics. Each additional
transformation must earn its own behavior matrix and prove that it is a general
concrete-syntax operation:

- `insert` must define exact parent, position, and trivia ownership;
- `delete` must define attached-comment and separator behavior;
- `splice` must define container and arity boundaries;
- `wrap` and `unwrap` must preserve metadata and reader forms;
- `move` must reconcile nested addressing with dependency-aware top-level
  movement.

Do not ship a name whose refusal and preservation matrix is not yet exact.

## Pure test matrix

- every builder on an empty path;
- every builder after `form` and after a prior navigation step;
- symbols, keywords, lists, vectors, maps, sets, metadata, discards, and reader
  conditionals as literal patterns or replacements;
- thread-first expressions equal the documented EDN vectors;
- builder inputs remain unchanged;
- invalid path values and invalid numeric arguments refuse with stable data;
- `replace` and `replace-span` must be terminal in validated programs;
- no builder evaluates its pattern or replacement;
- no builder performs file I/O;
- current `case`, nested-`cond`, and binding benchmark queries round-trip
  exactly.

## Clean-agent probes

Give clean Codex the same three edits and one read-only inventory. Compare:

1. current CLI plus edit-first skill;
2. native API reference with no example for the specific syntax shape;
3. compact native API reference;
4. outcome-only prompt with both routes available.

Measure:

- exact query/program correctness before any file operation;
- number of repair attempts;
- characters and tokens in the authored expression;
- shell quoting or temporary-script work;
- source-bearing calls;
- full edit correctness and separate apply;
- plan replay versus manual diff reproduction;
- cumulative/uncached tokens, tool-output bytes, and wall time.

## Keep gate

Keep the native surface only if it does at least one of the following across two
or more syntax shapes:

- removes a command from the 3/5-command baselines;
- cuts authored selector/transformation size by at least 25%;
- removes a demonstrated quoting or query-grammar repair;
- prevents the manual-diff replay failure by returning a clearer review/apply
  value;
- is voluntarily chosen when both routes are documented neutrally.

It must retain 100% exactness, zero/many refusal, one plan/one edit, separate
consent, unchanged source during planning, atomic apply, complete-file parse,
and verified read-back hash.

If it only compiles into the same query with more setup or a temporary script,
record the negative result and keep the CLI algebra.
