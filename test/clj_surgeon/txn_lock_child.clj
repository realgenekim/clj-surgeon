(ns clj-surgeon.txn-lock-child
  "Hold a workspace's PUBLISH.lock from a SEPARATE process for a bounded time.

   Cross-process is the whole point. `FileChannel/lock` is a per-JVM view of an
   OS advisory lock: a second attempt from the test's own process throws
   `OverlappingFileLockException` rather than blocking, so a witness that
   another writer's lock is actually WAITED for cannot be written in one JVM."
  (:require
   [clojure.java.io :as io])
  (:import
   (java.nio.channels FileChannel)
   (java.nio.file OpenOption StandardOpenOption))
  (:gen-class))

(defn -main
  [transactions-dir hold-ms & _]
  (let [dir (io/file transactions-dir)
        _ (.mkdirs dir)
        file (io/file dir "PUBLISH.lock")]
    (with-open [channel (FileChannel/open
                          (.toPath file)
                          ^"[Ljava.nio.file.OpenOption;"
                          (into-array OpenOption [StandardOpenOption/CREATE
                                                  StandardOpenOption/WRITE]))]
      (let [lock (.lock channel)]
        (println "HELD")
        (flush)
        (Thread/sleep (Long/parseLong hold-ms))
        (.release lock))))
  (System/exit 0))
