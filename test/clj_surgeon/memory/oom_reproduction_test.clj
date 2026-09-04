(ns clj-surgeon.memory.oom-reproduction-test
  "RED: the frozen read runs out of heap, and this test PROVES the defect.

   Read the inversion carefully. This test PASSES while clj-surgeon is broken.
   It asserts that a child JVM at -Xmx256m, performing the frozen read exactly
   as the alias-migration verb performs it, dies of OutOfMemoryError on a scope
   whose every file is far below the per-file ceiling and whose file count is
   far below the file ceiling. It is the executable statement of the defect,
   not a regression guard; it goes green-side-up only in
   `clj-surgeon.memory.journal-green-test`, which runs the same scenario at the
   same heap ceiling through the transaction journal and completes.

   It is deliberately outside `make test-fast` and `make mcp-test`: it writes
   three hundred megabytes of fixture and spends minutes of wall. `make
   memory-red` runs it."
  (:require
   [clj-surgeon.memory.child :as child]
   [clj-surgeon.memory.fixture :as fixture]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(def scope-files 600)
(def scope-bytes-per-file (* 512 1024))
(def control-files 8)
(def child-xmx "256m")

(defn- temp-root
  [label]
  (let [dir (io/file (or (System/getenv "CLJ_SURGEON_MEMORY_TMP") "/home/forge/tmp")
                     (str "clj-surgeon-memory-" label "-" (System/currentTimeMillis)))]
    (.mkdirs dir)
    (.getPath dir)))

(deftest frozen-read-exhausts-the-heap-that-holds-its-scope
  (testing "600 files of 512 KiB, every one under the 2 MiB per-file ceiling and
            the 2000-file ceiling, is 300 MB the frozen read must hold at once"
    (let [root (temp-root "frozen")]
      (try
        (let [manifest (fixture/generate-scope!
                         root {:files scope-files
                               :bytes-per-file scope-bytes-per-file})
              arm (child/run-arm {:main-ns 'clj-surgeon.memory.frozen-read-child
                                  :xmx child-xmx
                                  :args [root]})]
          (println "RED scope:" manifest)
          (println "RED exit:" (:exit arm))
          (println "RED err:" (subs (:err arm) 0 (min 400 (count (:err arm)))))
          (println "RED out:" (subs (:out arm) 0 (min 400 (count (:out arm)))))
          (is (> (:bytes manifest) (* 256 1024 1024))
              "the scope must exceed the child heap for the defect to appear")
          (is (child/out-of-memory? arm)
              "the frozen read must die of OutOfMemoryError at -Xmx256m")
          (is (nil? (:receipt arm))
              "a child that OOMs emits no receipt"))
        (finally
          (fixture/delete-tree! root))))))

(deftest frozen-read-succeeds-when-the-scope-fits
  (testing "the positive control: the same arm, the same heap, a scope that fits"
    (let [root (temp-root "control")]
      (try
        (let [manifest (fixture/generate-scope!
                         root {:files control-files
                               :bytes-per-file scope-bytes-per-file})
              arm (child/run-arm {:main-ns 'clj-surgeon.memory.frozen-read-child
                                  :xmx child-xmx
                                  :args [root]})]
          (println "CONTROL scope:" manifest)
          (println "CONTROL receipt:" (:receipt arm))
          (is (= 0 (:exit arm))
              (str "the control arm must complete: " (:err arm)))
          (is (= control-files (:files (:receipt arm)))
              "the control arm reads its whole scope")
          (is (= control-files (:result-hash-count (:receipt arm)))
              "the control arm plans every file")
          (is (string? (:tree-hash (:receipt arm)))
              "the control arm publishes the reference tree hash"))
        (finally
          (fixture/delete-tree! root))))))
