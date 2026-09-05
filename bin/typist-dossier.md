# Mission dossier — scope-roots (FROZEN; identical bytes to every arm)

You are editing a small Clojure project. Make ONE bounded change and emit ONLY a
unified diff.

## Intended change

`fixture.scope/scope-roots` currently takes no arguments and always returns
`default-roots`. Make it accept an options map and honour `(:roots opts)`:

- `(scope-roots)` — unchanged, returns `default-roots`.
- `(scope-roots {:roots ["src" "test"]})` — returns `["src" "test"]`.
- `(scope-roots {:roots []})` and `(scope-roots nil)` — fall back to
  `default-roots`.
- The return value must be a vector.

`fixture.scope/expand` calls `(scope-roots)` with no arguments and must keep
working unchanged.

## Files and exact spans

### src/fixture/scope.clj (THE ONLY FILE YOU MAY CHANGE)

```clojure
(ns fixture.scope
  "Scope expansion for the helper-extraction fence."
  (:require [fixture.util :as util]))

(def default-roots
  "The roots this workspace admits when the caller names none."
  ["src"])

(defn scope-roots
  "The admitted roots for this scope expansion."
  []
  default-roots)

(defn expand
  "Every admitted root plus the caller's paths, normalised, in order."
  [paths]
  (vec (distinct (map util/normalize (concat (scope-roots) paths)))))
```

The span to replace is exactly the `scope-roots` defn above (lines 9-12 of the
file, counting the `(ns` form as line 1).

### src/fixture/util.clj (DO NOT CHANGE — shown for context only)

```clojure
(ns fixture.util
  "Path helpers shared by the scope expander. NOT part of this mission."
  (:require [clojure.string :as str]))

(defn normalize
  "Trim a path and drop any trailing slash."
  [p]
  (str/replace (str/trim p) #"/+$" ""))
```

### test/fixture/scope_test.clj (DO NOT CHANGE — this is the proof gate)

```clojure
(ns fixture.scope-test
  (:require [clojure.test :refer [deftest is testing]]
            [fixture.scope :as scope]))

(deftest default-roots-unchanged
  (is (= ["src"] (scope/scope-roots))))

(deftest roots-opt-is-honoured
  (testing "(:roots opts) replaces the default roots"
    (is (= ["src" "test"] (scope/scope-roots {:roots ["src" "test"]})))
    (is (= ["lib"] (scope/scope-roots {:roots ["lib"]})))))

(deftest empty-roots-falls-back
  (is (= ["src"] (scope/scope-roots {:roots []}))))

(deftest expand-still-normalises
  (is (= ["src" "test"] (scope/expand ["test/"]))))
```

## Proof gate

Your diff is applied with `git apply` and then this command must exit 0 in the
project root:

```
bb -cp src:test -e "(require 'clojure.test 'fixture.scope-test) (let [r (clojure.test/run-tests 'fixture.scope-test)] (System/exit (if (and (zero? (:fail r)) (zero? (:error r))) 0 1)))"
```

An independent behavioural check then confirms `scope-roots` honours
`(:roots opts)` and that every file other than `src/fixture/scope.clj` is
byte-identical to the preimage. Touching any other file fails the mission.

## THE RULE

Emit **ONLY** a unified diff against the paths shown above, using `a/` and `b/`
prefixes (the output of `git diff`). No prose, no explanation, no markdown
fences, no commentary before or after. Nothing but the diff.
