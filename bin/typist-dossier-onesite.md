# Mission dossier — one-site literal (FROZEN; identical bytes to every arm)

This is the ONE-SITE CONTROL. You are editing a small Clojure project. Make ONE
bounded change at ONE known site and emit ONLY a unified diff.

## Intended change

`fixture.util/max-path-segments` is currently `16`. Change that literal to `64`.

Nothing else changes. No signature changes, no new functions, no call-site
updates, no docstring rewording. One literal, one site.

## Files and exact spans

### src/fixture/util.clj (THE ONLY FILE YOU MAY CHANGE)

```clojure
(ns fixture.util
  "Path helpers shared by the scope expander."
  (:require [clojure.string :as str]))

(def max-path-segments
  "The most path segments this workspace will normalise in one call."
  16)

(defn normalize
  "Trim a path and drop any trailing slash."
  [p]
  (str/replace (str/trim p) #"/+$" ""))
```

The span to replace is exactly the line `  16)` — the value of the
`max-path-segments` def above (line 7 of the file, counting the `(ns` form as
line 1). `normalize` must be left byte-identical.

### src/fixture/scope.clj (DO NOT CHANGE — shown for context only)

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

### test/fixture/util_test.clj (DO NOT CHANGE — this is the proof gate)

```clojure
(ns fixture.util-test
  (:require [clojure.test :refer [deftest is]]
            [fixture.util :as util]))

(deftest max-path-segments-is-64
  (is (= 64 util/max-path-segments)))

(deftest normalize-unchanged
  (is (= "a/b" (util/normalize "  a/b//  "))))
```

## Proof gate

Your diff is applied with `git apply` and then this command must exit 0 in the
project root:

```
bb -cp src:test -e "(require 'clojure.test 'fixture.util-test) (let [r (clojure.test/run-tests 'fixture.util-test)] (System/exit (if (and (zero? (:fail r)) (zero? (:error r))) 0 1)))"
```

An independent behavioural check then confirms `max-path-segments` is the
integer `64`, that `normalize` still behaves, and that every file other than
`src/fixture/util.clj` is byte-identical to the preimage. Touching any other
file fails the mission.

## THE RULE

Emit **ONLY** a unified diff against the paths shown above, using `a/` and `b/`
prefixes (the output of `git diff`). No prose, no explanation, no markdown
fences, no commentary before or after. Nothing but the diff.
