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

### 6. Integration tests: minimal, in try/finally, smoke-only

Integration tests exist to verify wiring, not logic. Keep them few (2-3) and
always clean up in `finally`:

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

;; Integration smoke tests (minimal I/O)
;;   — 2-3 tests max, verify wiring only
```

## When adding a new feature

1. Write the pure function first — takes data, returns data
2. Write unit tests with literal data
3. Write the I/O wrapper (private, thin)
4. Optionally add one integration smoke test
5. Run `make test`
