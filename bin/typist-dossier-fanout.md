# Mission dossier — fanout (FROZEN; identical bytes to every arm)

You are editing a small Clojure project. Make ONE bounded but MULTI-FILE change
and emit ONLY a unified diff.

## Intended change

Two things, together:

1. **Rename** the helper `fixture.util/normalize` to `fixture.util/normalize-path`
   — the definition and **every call site**, across `src/fixture/util.clj`,
   `src/fixture/scope.clj` and `src/fixture/paths.clj`. After the change the name
   `normalize` must not resolve as a var anywhere.

2. **Add a `:strict?` option**, threaded through **two** call paths.
   `normalize-path` gains a second arity taking an options map:

   - `(normalize-path p)` — unchanged behaviour: trim, then drop trailing slashes.
     `(normalize-path "  a//b//  ")` => `"a//b"`.
   - `(normalize-path p opts)` with `(:strict? opts)` truthy — additionally
     collapse every run of two or more inner slashes to one.
     `(normalize-path "  a//b//  " {:strict? true})` => `"a/b"`.
   - `(normalize-path p nil)` and `{:strict? false}` behave as the 1-arity.

   `fixture.scope/expand` gains a second arity `[paths opts]` and passes `opts`
   through to `normalize-path` for every path it normalises; `(expand paths)`
   keeps its current behaviour.

   `fixture.paths/join` gains a second arity `[segs opts]` and passes `opts`
   through to `normalize-path`; `(join segs)` keeps its current behaviour.

   `fixture.paths/relative-to`, `fixture.paths/clean-all` and
   `fixture.scope/admits?` are **renamed only** — they gain no option.

## TWO SPANS THAT MUST NOT BE RENAMED

`src/fixture/paths.clj` contains the text `normalize` twice in places that are
**not code references** and must stay **byte-identical**:

- the string literal that is the value of `op-name` — it stays `"normalize"`;
- the `Example:` line inside the `join` docstring — it stays
  `(normalize \"a/b/\")  ;=> \"a/b\"`.

A blind search-and-replace of `normalize` fails this mission.

## Files and exact spans

### src/fixture/util.clj (you may change this file)

```clojure
(ns fixture.util
  "Path helpers shared by the scope expander."
  (:require [clojure.string :as str]))

(defn normalize
  "Trim a path and drop any trailing slash."
  [p]
  (str/replace (str/trim p) #"/+$" ""))
```

### src/fixture/scope.clj (you may change this file)

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

(defn admits?
  "Does this scope admit the given path?"
  [p]
  (contains? (set (expand [])) (util/normalize p)))
```

### src/fixture/paths.clj (you may change this file)

```clojure
(ns fixture.paths
  "Path assembly built on the shared helpers."
  (:require [clojure.string :as str]
            [fixture.util :as util]))

(def op-name
  "The operation name reported in telemetry."
  "normalize")

(defn join
  "Join segments into one path.

  Example:
    (normalize \"a/b/\")  ;=> \"a/b\""
  [segs]
  (util/normalize (str/join "/" segs)))

(defn relative-to
  "Strip the root prefix from a path."
  [root p]
  (let [r (util/normalize root)
        n (util/normalize p)]
    (if (str/starts-with? n (str r "/"))
      (subs n (inc (count r)))
      n)))

(defn clean-all
  "Normalise every path in the collection."
  [paths]
  (mapv util/normalize paths))
```

### test/fixture/fanout_test.clj (DO NOT CHANGE — this is the proof gate)

```clojure
(ns fixture.fanout-test
  (:require [clojure.test :refer [deftest is testing]]
            [fixture.util :as util]
            [fixture.scope :as scope]
            [fixture.paths :as paths]))

(deftest normalize-path-renamed
  (is (= "a//b" (util/normalize-path "  a//b//  ")))
  (is (= "" (util/normalize-path "   ")))
  (is (nil? (resolve 'fixture.util/normalize))))

(deftest strict-collapses-inner-slashes
  (is (= "a/b" (util/normalize-path "  a//b//  " {:strict? true})))
  (is (= "a//b" (util/normalize-path "a//b" {:strict? false})))
  (is (= "a//b" (util/normalize-path "a//b" nil))))

(deftest expand-threads-strict
  (is (= ["src" "test"] (scope/expand ["test/"])))
  (is (= ["src" "a//b"] (scope/expand ["a//b/"])))
  (is (= ["src" "a/b"] (scope/expand ["a//b/"] {:strict? true}))))

(deftest admits-still-works
  (is (true? (scope/admits? "src/")))
  (is (false? (scope/admits? "lib"))))

(deftest join-threads-strict
  (is (= "a/b" (paths/join ["a" "b/"])))
  (is (= "a//b" (paths/join ["a" "" "b"])))
  (is (= "a/b" (paths/join ["a" "" "b"] {:strict? true}))))

(deftest relative-to-and-clean-all
  (is (= "a/b" (paths/relative-to "src/" "src/a/b")))
  (is (= "lib/x" (paths/relative-to "src" "lib/x")))
  (is (= ["a" "b"] (paths/clean-all ["a/" "b//"]))))

(deftest the-two-traps
  (testing "the string literal and the docstring example keep the OLD name"
    (is (= "normalize" paths/op-name))
    (is (re-find #"\(normalize " (:doc (meta #'paths/join))))))
```

## Proof gate

Your diff is applied with `git apply` and then this command must exit 0 in the
project root:

```
bb -cp src:test -e "(require 'clojure.test 'fixture.fanout-test) (let [r (clojure.test/run-tests 'fixture.fanout-test)] (System/exit (if (and (zero? (:fail r)) (zero? (:error r))) 0 1)))"
```

An independent behavioural check then confirms the end state directly — that
`normalize-path` resolves and `normalize` does not, that `:strict?` is honoured
on both `expand` and `join`, that both untouched spans still carry the old name —
and that every file other than the three `src/fixture/*.clj` files above is
byte-identical to the preimage. Touching `deps.edn`, `.clj-surgeon.edn` or the
test file fails the mission.

## THE RULE

Emit **ONLY** a unified diff against the paths shown above, using `a/` and `b/`
prefixes (the output of `git diff`). No prose, no explanation, no markdown
fences, no commentary before or after. Nothing but the diff.
