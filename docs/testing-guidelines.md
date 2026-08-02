# Testing Guidelines: Functional Core, Imperative Shell

Every function in clj-surgeon should be testable without touching the filesystem.
If you can't write a unit test without creating temp files, the function is doing
I/O and logic in the same place — split it.

## The Pattern

```
BEFORE:  fn does I/O → logic → return
AFTER:   fn-pure takes data → returns data     (unit-tested with plain values)
         fn-io does I/O → calls fn-pure         (thin wrapper, integration-tested or skipped)
```

This is Rich Hickey's "functional core, imperative shell."

## Rules

### 1. Pure functions are public and take data, not file paths

```clojure
;; GOOD: takes data, returns data — testable with a literal
(defn source-paths-from-config [filename content]
  (case filename
    "deps.edn" (or (:paths content) ["src"])
    ...))

;; BAD: takes a file path, does I/O AND logic
(defn- extract-source-paths [build-file]
  (let [content (read-string (slurp (str build-file)))]
    (case (fs/file-name build-file) ...)))
```

The I/O wrapper is a thin private function that slurps and delegates:

```clojure
(defn- extract-source-paths [build-file]
  (source-paths-from-config (str (fs/file-name build-file))
                            (read-string (slurp (str build-file)))))
```

### 2. Test pure functions with plain data — no temp files

```clojure
;; GOOD: data in, data out
(deftest test-source-paths
  (is (= ["src"] (source-paths-from-config "deps.edn" {})))
  (is (= ["src" "test"] (source-paths-from-config "deps.edn" {:paths ["src" "test"]}))))

;; BAD: creating a temp file to test a one-line case expression
(deftest test-source-paths-via-disk
  (let [tmp (spit "/tmp/deps.edn" "{:paths [\"src\"]}")]
    (is (= ["src"] (extract-source-paths "/tmp/deps.edn")))))
```

### 3. Test formatters with synthetic data maps

Formatters take structured data and return strings. They're already pure.
Test them directly — don't run the whole pipeline to reach them.

```clojure
(deftest test-format-file-text
  (let [output (format-file-text
                {:ns 'my.app :lines 50 :form-count 2
                 :requires ["[clojure.string :as str]"]
                 :forms [{:type 'defn :name 'greet :args "[x]" :line 5 :end-line 10}]}
                "src/my/app.clj")]
    (is (str/includes? output "5-10: defn greet [x]"))))
```

### 4. Test rewrite-clj functions with z/of-string, not file I/O

```clojure
;; GOOD: parse from string, no disk
(deftest test-extract-ns-requires
  (let [zloc (z/of-string "(ns my.app (:require [clojure.string :as str]))")
        ns-zloc (-> zloc (z/find-value z/next 'ns) z/up)]
    (is (= ["[clojure.string :as str]"]
           (outline/extract-ns-requires ns-zloc)))))

;; BAD: spit to temp file, slurp back, parse — 3 I/O ops to test a pure function
(deftest test-extract-ns-requires-via-disk
  (let [tmp (spit "/tmp/test.clj" "(ns my.app (:require [clojure.string :as str]))")
        result (outline/outline "/tmp/test.clj")]
    (is (= ["[clojure.string :as str]"] (:requires result)))))
```

### 5. Test set/filter operations with literal sets

```clojure
;; GOOD: pure set intersection, no rg/grep
(deftest test-filter-by-hits
  (let [projects [{:name "a" :root "/r/a" :files ["/r/a/src/x.clj"]}
                  {:name "b" :root "/r/b" :files ["/r/b/src/y.clj"]}]
        hits #{"/r/a/deps.edn"}]
    (is (= 1 (count (filter-projects-by-hits projects hits))))
    (is (= "a" (:name (first (filter-projects-by-hits projects hits)))))))
```

### 6. Integration tests: minimal, high-value, and boundary-specific

Integration tests exist to verify wiring and real boundary behavior, not to
duplicate the pure behavior matrix through slow setup. Keep them few, give each
one a distinct contract, and always clean up in `finally`:

```clojure
(deftest test-integration-single-project
  (let [tmp-dir (str (fs/create-temp-dir {:prefix "test"}))]
    (try
      ;; setup: create real files
      ;; assert: end-to-end output looks right
      (finally
        (fs/delete-tree tmp-dir)))))
```

## Test organization

Each test file should have this structure:

```
;; Pure tests: function-name (data in, data out)
;;   — these are the majority, fast, no I/O

;; Integration and real-program regression tests (minimal I/O)
;;   — each test proves a boundary or field-failure contract that pure tests cannot
```

## The One-Shot Feature Standard

"One-shot" does not mean hoping one happy-path test is enough. It means loading
the repository's standards before implementation and making the first complete
change include every contract needed for review, use, and future maintenance.

Every non-trivial feature must have these layers:

1. **Observable contract:** exact inputs, successful output, refusal output,
   side effects, and invariants are written down before implementation.
2. **Pure behavior matrix:** every meaningful branch and interaction is tested
   with literals or parsed source strings. Happy paths alone are insufficient.
3. **Field-failure regression:** the motivating production failure is captured
   faithfully, including the precondition that made the original program valid.
4. **Real-program evidence:** at least one fixture derived from an actual
   program exercises realistic form shapes, metadata, comments, ordering, and
   dependency depth. Record where it came from and what was minimized.
5. **Boundary contracts:** filesystem, subprocess, CLI, exit status, atomicity,
   or external-tool behavior is tested only where the feature crosses that
   boundary.
6. **User-facing surface:** help, examples, README or skill instructions,
   structured EDN fields, aliases, and changelog are updated together.
7. **Completion gates:** formatting, targeted tests, full tests, lint/compile,
   and a documented end-to-end invocation all pass.

### Exhaustive means contract-exhaustive

Tests do not need to enumerate arbitrary syntax. They must enumerate the
feature's semantic dimensions and important intersections. Before coding, make
a table whose rows include:

- success, refusal, and no-op;
- every direction or mode;
- empty, singleton, direct, transitive, shared, and cyclic relationships;
- pre-existing valid exceptions such as declarations;
- ambiguity, missing targets, and unsupported syntax;
- dry-run versus execute;
- canonical operation versus aliases or convenience forms;
- preservation of comments, metadata, formatting, and unrelated forms;
- unchanged bytes on every refused mutation;
- stable EDN fields and process exit status.

If two dimensions interact in a way that could change the outcome, add the
intersection case. Prefer table-driven pure tests when many cases share the
same assertion shape.

### Real-program tests

Synthetic examples explain rules; real-program-derived fixtures expose the
shapes we forgot to imagine. Use both.

- Preserve enough surrounding structure to reproduce the failure honestly.
- Minimize noise, not the condition that made the source valid.
- Add a comment naming the incident, issue, or source snapshot.
- Never mutate the live program under test. Plan against strings or operate on
  a copied fixture/temp file.
- For source transformations, assert structural facts and parse the complete
  result. When the promised safety property is compilation, also cold-lint or
  compile the result; balanced parentheses are not sufficient.
- Keep a larger real source snapshot only when the size or interaction itself
  is the regression. Otherwise prefer a faithful minimized fixture plus one
  corpus/dogfood test over brittle assertions against a changing source file.

### Field failures become executable specifications

For every reported production failure:

1. Add a regression that fails for the reported reason on the old code.
2. Prove the starting fixture is valid; an invalid baseline cannot demonstrate
   that the operation introduced the breakage.
3. Assert the exact structured diagnostic or result, not only truthiness.
4. Assert refusal performs no write.
5. Assert the recommended remedy succeeds end to end when that remedy is part
   of the public contract.

### CLI and mutation contracts

For a new CLI operation, option, or alias, test:

- argument parsing and default injection;
- canonical dispatch and every documented alias;
- global help and operation help;
- machine-readable EDN on stdout with no stack trace noise;
- zero exit on success and nonzero exit on refusal/error;
- the exact command shown in documentation.

For an agent-facing write operation, also run a clean-context caller
simulation before declaring it complete:

- Give a fresh agent only the installed CLI help and a realistic user goal;
  withhold implementation source, plans, tests, and change history.
- Require it to state the exact preview, refusal branch, review fields, apply,
  and verification commands it would run.
- Treat any unsafe first command, contradictory write claim, guessed EDN field,
  or unclear exit behavior as a product defect. Fix help and add permanent
  assertions before rerunning the simulation.
- Put non-mutating commands first. If preview and apply are separate, name them
  unambiguously in both prose and structured output.

For a write operation, test:

- planning is pure;
- dry-run and execute use the same planned candidate;
- failed validation leaves the original bytes unchanged;
- successful output reparses completely;
- writes cannot partially apply a multi-form result;
- source staleness is rejected when the operation promises snapshot safety.

## Feature completion evidence

The final handoff for a feature reports:

- the contract implemented and explicit non-goals;
- the new test matrix and real-program regression fixture;
- targeted and full-suite results with test/assertion counts;
- formatter and linter/compile commands run;
- one documented CLI invocation and its observed EDN/exit behavior;
- any unsupported cases that fail closed.

## When adding a new feature

1. Read `CLAUDE.md`, `docs/vision.md`, this guide, and the applicable plan.
2. Write or refine the observable contract and behavior matrix.
3. Add the failing pure tests and faithful field-failure fixture first.
4. Write the public pure function — data in, data out.
5. Write the thin I/O or CLI wrapper.
6. Add the required boundary and real-program tests.
7. Update help, examples, skill/README, and changelog together.
8. Format changed Clojure files, run targeted tests, lint/compile transformed
   candidates, then run `make test`.
9. Report the completion evidence above; do not call the feature complete if a
   required layer is missing.
