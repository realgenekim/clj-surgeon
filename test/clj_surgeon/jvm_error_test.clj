(ns clj-surgeon.jvm-error-test
  "TEST-ISO-013 -- the babashka-reachable tree must not name a class SCI lacks.

   THE DEFECT. skiff, 2026-09-10, `make test`:

       Type:     clojure.lang.ExceptionInfo
       Message:  Unable to resolve classname: StackOverflowError
       Location: src/clj_surgeon/core.clj:646:3
       Phase:    analysis

   bb lane exit 1, all 49 babashka namespaces dead before a test ran. Not one
   of them names that class; they merely require a namespace that did. SCI
   resolves classnames at ANALYSIS time, so a class its allowlist lacks makes a
   namespace UNLOADABLE whether or not the code path is ever taken -- and
   `clj-surgeon.core` is required by everything.

   WHY A SCAN AND NOT A LOAD. The honest witness is `load the whole bb
   inventory under babashka v1.12.209`, and that is how the fix was verified:
   49 namespaces, 0 unloadable, on that exact binary. But the box running this
   suite has whatever babashka it has -- v1.13.219 here, which resolves the
   class and stays green straight over the defect. A witness that passes on the
   machine running it and fails on the machine that matters is the shape of the
   original bug, not a check on it. So the property is asserted where it is
   decidable on every box: the SPELLINGS are absent, and bb.edn declares the
   floor they were measured against.

   AND WHY THE SCAN IS SCOPED. The JVM has every one of these classes, and
   `mcp-admit-tool`, `admit-patch-test` and the census tests legitimately
   construct an `OutOfMemoryError` to drive a refusal. Forbidding the names
   repository-wide would be false, would be switched off within a week, and
   would tell an operator nothing about babashka. What matters is the CLOSURE
   of `test/run_all.clj` -- the namespaces babashka actually loads, computed
   here from their `ns` forms rather than assumed.

   This namespace runs in the BB lane on purpose: the runtime that cares is the
   one doing the checking."
  {:lane :bb}
  (:require
   [clj-surgeon.jvm-error :as jvm]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def forbidden
  "The code SHAPES that fail, not merely the class names.

   A classname resolves in more positions than `catch`: `instance?`, a
   constructor, and a type hint all send SCI to the same allowlist, and a
   scanner derived from the one spelling the skiff happened to print cannot see
   the others. Fully qualifying rescues none of them -- babashka v1.12.209 has
   no entry for either class at all, measured on that binary.

   Matching SHAPES rather than bare names is also what lets this repository go
   on explaining the defect in prose: `catch StackOverflowError` written in a
   sentence has no opening paren, and a sentence is where this belongs."
  [#"\(catch\s+(?:java\.lang\.)?(?:StackOverflowError|OutOfMemoryError)[\s)]"
   #"\(instance\?\s+(?:java\.lang\.)?(?:StackOverflowError|OutOfMemoryError)[\s)]"
   #"\((?:java\.lang\.)?(?:StackOverflowError|OutOfMemoryError)\.[\s)]"
   #"\^(?:java\.lang\.)?(?:StackOverflowError|OutOfMemoryError)[\s)]"])

(defn- ns-form
  "The first top-level form -- the `ns` form. Everything after the first `(` in
  column 0 that is not the file's first character belongs to another form."
  [source]
  (let [end (str/index-of source "\n(" 1)]
    (subs source 0 (or end (count source)))))

(defn ns->file
  "The file a namespace is read from, or nil when it has none on disk."
  [n]
  (let [stem (-> (str n) (str/replace "-" "_") (str/replace "." "/"))]
    (first (for [root ["src" "test"]
                 ext [".clj" ".cljc"]
                 :let [file (io/file root (str stem ext))]
                 :when (.exists file)]
             (str file)))))

(defn required-namespaces
  "The `clj-surgeon.*` namespaces an `ns` form requires. Read from the ns form
  ONLY, so a namespace merely NAMED in prose does not enlarge the closure."
  [source]
  (->> (re-seq #"clj-surgeon\.[a-zA-Z0-9.*+!_?<>=-]+" (ns-form source))
       (map symbol)
       distinct))

(defn babashka-closure
  "Every file babashka loads when it runs `test/run_all.clj`: the declared
  inventory plus everything it transitively requires."
  []
  (let [inventory (edn/read-string
                    (second (re-find #"(?s)\(def namespaces\s+'(\[.*?\])\)"
                                     (slurp "test/run_all.clj"))))]
    (loop [pending (vec inventory) seen #{} files {}]
      (if-let [n (first pending)]
        (if (seen n)
          (recur (rest pending) seen files)
          (if-let [file (ns->file n)]
            (let [source (slurp file)]
              (recur (into (vec (rest pending)) (required-namespaces source))
                     (conj seen n)
                     (assoc files n file)))
            (recur (rest pending) (conj seen n) files)))
        files))))

;; The scanner does not scan its own fixtures: `the-scanner-can-actually-see-
;; the-defect` below holds every forbidden shape as a string literal on
;; purpose, and a scanner that flagged its own test data would be turned off.
(def ^:private self 'clj-surgeon.jvm-error-test)

(deftest no-babashka-reachable-source-names-a-class-sci-lacks
  (let [closure (babashka-closure)
        offenders (vec (sort (for [[n file] closure
                                   :when (not= self n)
                                   :let [source (slurp file)]
                                   pattern forbidden
                                   :when (re-find pattern source)]
                               (str file " :: " (re-find pattern source)))))]
    (is (< 40 (count closure))
        "the closure must be the real inventory, not an empty scan reading green")
    (is (= [] offenders)
        (str "these name a class babashka v1.12.209's SCI cannot resolve in ANY "
             "spelling. SCI fails at ANALYSIS time, so one of these makes the "
             "namespace unloadable and takes every namespace that requires it "
             "with it -- 49 of them, on the skiff, before a test ran. Catch "
             "`Error` or `Throwable` and ask clj-surgeon.jvm-error what was "
             "caught."))))

(deftest the-closure-reaches-the-namespace-that-actually-broke
  ;; A scan of the wrong set is a scan that cannot fail. core.clj is the file
  ;; the skiff named, and it is reachable only transitively.
  (let [closure (babashka-closure)]
    (is (contains? closure 'clj-surgeon.core)
        "the namespace the skiff named, reachable only transitively")
    (is (contains? closure 'clj-surgeon.jvm-error)
        "the repair itself is loaded by babashka and is in scope")
    (is (not (contains? closure 'clj-surgeon.mcp-admit-tool))
        "JVM-only namespaces are out of scope, and legitimately construct these")))

(deftest the-scanner-can-actually-see-the-defect
  ;; An audit nobody has watched go red is an audit nobody should trust.
  (testing "every failing shape is caught"
    (doseq [shape ["(catch StackOverflowError _ nil)"
                   "(catch java.lang.StackOverflowError _ nil)"
                   "(catch OutOfMemoryError error (foo))"
                   "(catch java.lang.OutOfMemoryError _ nil)"
                   "(instance? StackOverflowError e)"
                   "(instance? java.lang.OutOfMemoryError e)"
                   "(StackOverflowError.)"
                   "(OutOfMemoryError. \"Java heap space\")"
                   "(defn f [^StackOverflowError e] e)"]]
      (is (some #(re-find % shape) forbidden) shape)))
  (testing "prose about the defect is not the defect"
    (doseq [prose [";; never `catch StackOverflowError`: see clj-surgeon.jvm-error"
                   "   an OutOfMemoryError is an Error, not an Exception"
                   "(= name \"java.lang.StackOverflowError\")"
                   "(catch Error error (if (jvm/stack-overflow? error) x (throw error)))"]]
      (is (not-any? #(re-find % prose) forbidden) prose))))

(deftest the-predicates-answer-a-real-overflow
  ;; A REAL overflow, because the constructor is itself an unresolvable
  ;; classname on babashka v1.12.209 -- a witness that builds the value it is
  ;; testing could not run on the runtime this exists for.
  (let [overflow (fn overflow [n] (inc (overflow n)))
        caught (try (overflow 0) ::none (catch Error error error))]
    (is (jvm/stack-overflow? caught))
    (is (not (jvm/out-of-memory? caught)))
    (is (not (jvm/stack-overflow? (ex-info "ordinary" {}))))
    (is (not (jvm/stack-overflow? nil)))
    (is (not (jvm/out-of-memory? nil)))))

(deftest bb-edn-names-the-floor-the-spellings-were-measured-against
  (is (re-find #":min-bb-version\s+\"1\.12\.209\"" (slurp "bb.edn"))
      "bb.edn declares the oldest babashka this tree is witnessed on")
  (is (str/includes? (slurp "bin/install-preflight") "min-bb-version")
      "the preflight reads that floor from bb.edn rather than repeating it"))
