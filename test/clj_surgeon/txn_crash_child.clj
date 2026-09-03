(ns clj-surgeon.txn-crash-child
  "A transaction that is killed mid-flight so recovery has something to find.

   `Runtime.halt` is used rather than `System/exit` so no shutdown hook, no
   finally block and no buffered writer gets a chance to tidy up: the journal on
   disk is exactly what a power cut would leave."
  (:require
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io])
  (:gen-class))

(defn- source-files
  [root]
  (sort (map #(.getCanonicalPath ^java.io.File %)
             (filter #(.isFile ^java.io.File %)
                     (file-seq (io/file root "src"))))))

(defn -main
  [root state-home crash-point & _]
  (let [paths (source-files root)
        txn (journal/begin! root {:state-home state-home})]
    (doseq [path paths]
      (journal/record-read! txn path))
    (journal/seal-read-set! txn)
    (doseq [path paths]
      (journal/pin! txn path)
      (journal/stage! txn path (str (slurp path) ";; crashed\n")))
    (println "#CRASH-CHILD staged" (count paths) "files")
    (flush)
    (case crash-point
      "after-pin"
      (.halt (Runtime/getRuntime) 9)

      "between-renames"
      (journal/commit!
        txn {:after-publish (let [written (atom 0)]
                              (fn [_]
                                (when (>= (swap! written inc) 2)
                                  (.halt (Runtime/getRuntime) 9))))})

      (throw (ex-info "unknown crash point" {:crash-point crash-point})))
    (println "#CRASH-CHILD survived, which is a failed injection")
    (flush)
    (System/exit 0)))
