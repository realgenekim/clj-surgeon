(ns clj-surgeon.artifact-boundary-support
  "ONE answer to `is this receipt outside the workspace?`, for every witness.

   THE DEFECT. skiff, 2026-09-10, alias lane exit 39, three FAILs in
   `clj-surgeon.receipt-artifacts-boundary-test`: receipts published at
   `/private/var/tmp/forge/edit-clojure-receipts/...` while the assertion
   demanded a literal `/var/tmp/forge/` prefix. On darwin `/var/tmp` is a
   symlink to `/private/var/tmp`, and `receipt-artifacts/directory`
   canonicalizes -- it has to, because it must prove the receipt directory does
   not resolve INSIDE the workspace. The paths were right. The comparison
   canonicalized one side and not the other.

   Three witnesses in three namespaces had each written that comparison out
   longhand against a literal root, so the skiff found it three times and would
   have found it again in the next namespace to copy the line. They all call
   here now, and `receipt-artifacts-boundary-test` fails any test source that
   goes back to a literal."
  (:require
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn canonical
  "The path with every symlink resolved. `getCanonicalFile`, not `toRealPath`:
   it answers for a path that does not exist yet, so a boundary check does not
   depend on when it is asked."
  [path]
  (str (.getCanonicalFile (io/file (str path)))))

(defn verb-receipt-root
  "The canonical directory `verb` publishes under, from the root THIS RUN
   declared. Read from `receipt-artifacts/*artifact-root*` rather than from a
   literal: a witness that hardcodes where the writer publishes stops
   witnessing the writer the moment the writer moves."
  [verb]
  (canonical (io/file artifacts/*artifact-root* (str verb "-receipts"))))

(defn published-under-root?
  "Is `path` inside `verb`'s receipt root? Canonical on BOTH sides."
  [verb path]
  (str/starts-with? (canonical path)
                    (str (verb-receipt-root verb) java.io.File/separator)))
