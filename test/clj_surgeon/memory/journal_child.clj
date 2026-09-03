(ns clj-surgeon.memory.journal-child
  "The same scenario as the frozen read, through the transaction journal.

   One file is resident at a time. The read set streams to the manifest, the
   pre-image bytes are pinned into the transaction's object store, the future
   bytes go to a staging file, and the tree hash is folded into a running digest
   rather than accumulated as a list of six hundred hashes - which would be the
   same defect at a smaller constant."
  (:require
   [clj-surgeon.memory.heap :as heap]
   [clj-surgeon.memory.scenario :as scenario]
   [clj-surgeon.scope-stream :as scope]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io])
  (:import
   (java.security MessageDigest))
  (:gen-class))

(defn- scope-walk
  [root]
  (fn []
    (sort (map #(.getCanonicalPath ^java.io.File %)
               (filter #(and (.isFile ^java.io.File %)
                             (.endsWith (.getName ^java.io.File %) ".clj"))
                       (file-seq (io/file root "src")))))))

(defn run
  [root state-home checkpoint-every]
  (journal/recover! root {:state-home state-home})
  (let [txn (journal/begin! root {:state-home state-home
                                  :scope-walk (scope-walk root)
                                  :max-journal-bytes (* 2 1024 1024 1024)
                                  :max-staged-files 5000})
        digest (MessageDigest/getInstance "SHA-256")
        planned (atom 0)
        refusals (atom [])
        summary
        (scope/stream-scope!
          root
          (fn [{:keys [path bytes sha256 source]}]
            (let [plan (scenario/plan-file path source)
                  recorded (journal/record-read!
                             txn {:path path :bytes bytes :sha256 sha256
                                  :mode "rw-r--r--"})
                  pinned (journal/pin! txn path)
                  staged (journal/stage! txn path (:result plan))]
              (doseq [step [recorded pinned staged]]
                (when-not (:ok step) (swap! refusals conj step)))
              (when (pos? @planned) (.update digest (.getBytes "\n" "UTF-8")))
              (.update digest (.getBytes ^String (:result-hash plan) "UTF-8"))
              (swap! planned inc)
              (when (zero? (mod @planned checkpoint-every))
                (heap/sample-retention!))
              nil))
          {:max-file-bytes (* 2 1024 1024)
           :max-aggregate-bytes (* 1024 1024 1024)
           :work-budget-bytes (* 192 1024 1024)})
        _ (journal/seal-read-set! txn)
        commit (journal/commit! txn)]
    (heap/sample-retention!)
    {:summary (dissoc summary :records)
     :commit commit
     :refusals @refusals
     :files @planned
     :tree-hash (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))}))

(defn -main
  [root state-home & args]
  (let [checkpoint (Integer/parseInt (or (first args) "50"))
        {:keys [result memory]} (heap/measure #(run root state-home checkpoint))]
    (heap/emit-receipt! {:arm :journal
                         :files (:files result)
                         :tree-hash (:tree-hash result)
                         :committed (get-in result [:commit :committed])
                         :files-written (get-in result [:commit :files-written])
                         :read-set-files (get-in result [:commit :read-set-files])
                         :reserved (merge (get-in result [:summary :reserved])
                                          (get-in result [:commit :reserved]))
                         :work (get-in result [:summary :work])
                         :refusals (:refusals result)
                         :commit-error (get-in result [:commit :error-type])
                         :memory memory})
    (shutdown-agents)
    (System/exit 0)))
