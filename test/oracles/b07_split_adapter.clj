#!/usr/bin/env bb
;; B07 operator adapter: trusted proof configuration only, no discovery or emission.
(require '[clj-surgeon.namespace-split-io :as split]
         '[clojure.edn :as edn] '[clojure.java.io :as io])

(System/setProperty "java.io.tmpdir" (or (System/getenv "TMPDIR") "/var/tmp/forge"))
(let [[request-file output-dir & extra] *command-line-args*]
  (when (or (nil? request-file) (nil? output-dir) (seq extra))
    (binding [*out* *err*] (println "usage: bb -cp FROZEN/src b07_split_adapter.clj REQUEST.edn OUTPUT-DIRECTORY"))
    (System/exit 2))
  (let [artifact-dir (.getCanonicalFile (.getParentFile (io/file *file*)))
        config {:verification-profiles {"b07-cell-b"
                                        {:commands [["bash" (str (io/file artifact-dir "cell_b_oracle.sh"))]]
                                         :timeout-ms 1800000}}
                :receipt-dir output-dir}
        result (split/execute! config (edn/read-string (slurp request-file)))]
    (prn result)
    (System/exit (if (:ok result) 0 1))))
