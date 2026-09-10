(ns clj-surgeon.jvm-error
  "`java.lang.Error` subclasses this tree catches, named in a way SCI can read.

   WHY THIS EXISTS. On 2026-09-10 the skiff's bb lane exited 1 before a single
   test ran, taking all 49 babashka namespaces with it:

       Type:     clojure.lang.ExceptionInfo
       Message:  Unable to resolve classname: StackOverflowError
       Location: src/clj_surgeon/core.clj:646:3
       Phase:    analysis

   babashka's SCI carries an ALLOWLIST of host classes, and `StackOverflowError`
   is not in babashka v1.12.209's. Neither is `OutOfMemoryError`. Measured on
   that exact binary, 2026-09-10:

       catching StackOverflowError            -> Unable to resolve classname
       catching java.lang.StackOverflowError  -> Unable to resolve classname
       catching OutOfMemoryError              -> Unable to resolve classname
       catching Error                         -> resolves
       catching Throwable                     -> resolves

   (Spelled that way deliberately: `clj-surgeon.jvm-error-test` scans this tree
   for the real code shapes, and a docstring that demonstrates one would be a
   scanner finding its own example.)

   So FULLY QUALIFYING THE NAME DOES NOT FIX IT: the class is absent from the
   map, not merely unqualified. It resolves on bb v1.13.219 and on every JVM,
   which is why this survived to a laptop -- the failure is a property of the
   READER, and it fires at ANALYSIS time, so a namespace that merely MENTIONS
   the class in a `catch` cannot be loaded at all, whether or not the code path
   ever runs.

   The repair that works on every runtime is to catch a class SCI does have and
   ask the caught value what it is. That is what these predicates are for, and
   they are deliberately string comparisons on `(class t)` rather than
   `instance?` checks: an instance check against StackOverflowError needs that
   same unresolvable classname and would reintroduce the defect in a new
   spelling. `clj-surgeon.jvm-error-test` forbids that spelling too.

   `clj-surgeon.jvm-error-test` scans this repository for every spelling that
   fails, so the next one is caught by a test rather than by a Mac.")

(defn- class-named?
  [thrown fully-qualified]
  (and (some? thrown) (= fully-qualified (.getName (class thrown)))))

(defn stack-overflow?
  "True when `thrown` IS a java.lang.StackOverflowError.

   Exact class, not a subtype test: nothing extends it, and an `instance?`
   check would need the classname SCI cannot resolve."
  [thrown]
  (class-named? thrown "java.lang.StackOverflowError"))

(defn out-of-memory?
  "True when `thrown` IS a java.lang.OutOfMemoryError."
  [thrown]
  (class-named? thrown "java.lang.OutOfMemoryError"))
