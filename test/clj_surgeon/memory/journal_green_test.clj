(ns clj-surgeon.memory.journal-green-test
  "GREEN: the scenario that killed the frozen read, through the journal.

   Same fixture, same -Xmx256m, same per-file work. The frozen read dies (see
   `clj-surgeon.memory.oom-reproduction-test`); the streaming reader plus the
   disk journal completes, commits six hundred files, and produces a tree the
   unbounded reference implementation agrees with byte for byte.

   The journal arm takes NO ceiling override. Every default admits this
   workload - including the journal quota, which is derived as twice the
   reader's aggregate ceiling precisely so that a scope the read path accepts
   is never one the journal refuses to stage. Before that derivation this arm
   had to raise the quota to 2 GiB to run, and `the defaults admit it` was a
   claim nothing tested.

   The pass lines are the hard ones. Sampled peak used heap is reported as a
   TREND, not gated: under default G1 at a small heap it measures how close
   allocation ran to the ceiling, not what the arm retains - an eight-file
   control that retained 12 MB peaked at 251 MB. The hard lines are no OOM,
   output parity, retained heap after a full collection, and the accountant's
   attributable reserved peak."
  (:require
   [clj-surgeon.memory.child :as child]
   [clj-surgeon.memory.fixture :as fixture]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

(def scope-files 600)
(def small-scope-files 60)
(def scope-bytes-per-file (* 512 1024))
(def child-xmx "256m")
(def reference-xmx "2g")
(def retained-headroom-mb 96.0)
(def flatness-headroom-mb 32.0)

(defn- temp-root
  [label]
  (let [dir (io/file (or (System/getenv "CLJ_SURGEON_MEMORY_TMP") "/home/forge/tmp")
                     (str "clj-surgeon-green-" label "-" (System/currentTimeMillis)))]
    (.mkdirs dir)
    (.getCanonicalPath dir)))

(defn- delete-tree!
  [root]
  (let [file (io/file root)]
    (when (.exists file)
      (doseq [child (reverse (file-seq file))]
        (Files/deleteIfExists (.toPath child))))))

(defn- tree-hash-on-disk
  "One digest over every file's current bytes, in path order.

   Computed in the parent from the tree itself, so it depends on neither arm."
  [root]
  (journal/sha256-string
    (str/join "\n" (map journal/sha256-file (fixture/scope-files root)))))

(defn- run-journal-arm!
  [root files]
  (let [state (temp-root (str "state-" files))]
    (try
      (let [manifest (fixture/generate-scope! root {:files files
                                                    :bytes-per-file scope-bytes-per-file})
            arm (child/run-arm {:main-ns 'clj-surgeon.memory.journal-child
                                :xmx child-xmx
                                :args [root state "50"]})]
        (assoc arm :manifest manifest))
      (finally (delete-tree! state)))))

(deftest the-journal-completes-the-scope-that-killed-the-frozen-read
  (let [root (temp-root "journal")]
    (try
      (let [manifest (fixture/generate-scope!
                       root {:files scope-files :bytes-per-file scope-bytes-per-file})
            reference (child/run-arm {:main-ns 'clj-surgeon.memory.frozen-read-child
                                      :xmx reference-xmx
                                      :args [root]})
            before (tree-hash-on-disk root)
            state (temp-root "journal-state")
            arm (child/run-arm {:main-ns 'clj-surgeon.memory.journal-child
                                :xmx child-xmx
                                :args [root state "50"]})
            after (tree-hash-on-disk root)
            receipt (:receipt arm)
            memory (:memory receipt)]
        (println "GREEN scope:" manifest)
        (println "GREEN reference receipt:" (:receipt reference))
        (println "GREEN journal receipt:" receipt)
        (println "GREEN err:" (subs (:err arm) 0 (min 600 (count (:err arm)))))

        (testing "the reference implementation completes at an unbounded heap"
          (is (= 0 (:exit reference)))
          (is (= scope-files (:files (:receipt reference)))))

        (testing "the journal arm completes at the heap that killed the frozen read"
          (is (= 0 (:exit arm)) (str "the journal arm must not OOM: " (:err arm)))
          (is (= 256.0 (:xmx-mb memory)))
          (is (true? (:committed receipt)))
          (is (= scope-files (:files-written receipt)))
          (is (= scope-files (:read-set-files receipt)))
          (is (empty? (:refusals receipt))))

        (testing "output parity with the unbounded reference"
          (is (not= before after) "the transaction actually rewrote the tree")
          (is (= (:tree-hash (:receipt reference)) (:tree-hash receipt))
              "the journal arm's streamed digest equals the frozen read's")
          (is (= after (:tree-hash receipt))
              "and the tree on disk is what both arms said it would be"))

        (testing "the hard memory lines"
          (is (< (:heap-retained-peak-mb memory)
                 (+ (:heap-used-start-mb memory) retained-headroom-mb))
              (str "retained heap " (:heap-retained-peak-mb memory)
                   " MB must stay within " retained-headroom-mb
                   " MB of the " (:heap-used-start-mb memory) " MB start"))
          (is (<= (get-in receipt [:reserved :heap-reserved-peak-bytes])
                  (get-in receipt [:reserved :work-budget-bytes]))
              "the attributable reserved peak stays inside the work budget"))

        (delete-tree! state))
      (finally (delete-tree! root)))))

(deftest retained-heap-does-not-grow-with-the-file-count
  (testing "ten times the files must not mean ten times the retention"
    (let [small (temp-root "flat-small")
          large (temp-root "flat-large")]
      (try
        (let [a (run-journal-arm! small small-scope-files)
              b (run-journal-arm! large scope-files)
              ma (get-in a [:receipt :memory])
              mb (get-in b [:receipt :memory])]
          (println "FLATNESS" small-scope-files ma)
          (println "FLATNESS" scope-files mb)
          (is (= 0 (:exit a)))
          (is (= 0 (:exit b)))
          (is (= small-scope-files (get-in a [:receipt :files-written])))
          (is (= scope-files (get-in b [:receipt :files-written])))
          (is (< (:heap-retained-peak-mb mb)
                 (+ (:heap-retained-peak-mb ma) flatness-headroom-mb))
              (str "retention at " scope-files " files (" (:heap-retained-peak-mb mb)
                   " MB) must stay within " flatness-headroom-mb " MB of retention at "
                   small-scope-files " files (" (:heap-retained-peak-mb ma) " MB)")))
        (finally (delete-tree! small) (delete-tree! large))))))
